import "@supabase/functions-js/edge-runtime.d.ts"
import { createClient } from "jsr:@supabase/supabase-js@2"

const LIVEKIT_API_KEY = Deno.env.get("LIVEKIT_API_KEY") ?? ""
const LIVEKIT_API_SECRET = Deno.env.get("LIVEKIT_API_SECRET") ?? ""

// Sliding-window in-memory rate limiter
const rateLimitMap = new Map<string, number[]>()
const RATE_LIMIT_WINDOW_MS = 60_000   // 1 minute
const RATE_LIMIT_MAX_REQUESTS = 10    // 10 requests per minute per user

function isRateLimited(userId: string): boolean {
  const now = Date.now()
  const timestamps = (rateLimitMap.get(userId) ?? []).filter(t => now - t < RATE_LIMIT_WINDOW_MS)
  if (timestamps.length >= RATE_LIMIT_MAX_REQUESTS) return true
  timestamps.push(now)
  rateLimitMap.set(userId, timestamps)
  return false
}

async function buildLivekitToken(roomName: string, identity: string): Promise<string> {
  const encoder = new TextEncoder()

  const header = btoa(JSON.stringify({ alg: "HS256", typ: "JWT" }))
    .replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "")

  const now = Math.floor(Date.now() / 1000)
  const grants = { roomJoin: true, room: roomName, canPublish: true, canSubscribe: true }
  const payloadObj = {
    iss: LIVEKIT_API_KEY,
    sub: identity,
    iat: now,
    exp: now + 21600,
    nbf: now - 5,
    video: grants,
  }
  const payload = btoa(JSON.stringify(payloadObj))
    .replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "")

  const data = `${header}.${payload}`

  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode(LIVEKIT_API_SECRET),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  )
  const sigBuffer = await crypto.subtle.sign("HMAC", key, encoder.encode(data))
  const sig = btoa(String.fromCharCode(...new Uint8Array(sigBuffer)))
    .replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "")

  return `${data}.${sig}`
}

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 })
  }

  // Verify caller is authenticated
  const authHeader = req.headers.get("Authorization")
  if (!authHeader) {
    return new Response("Unauthorized", { status: 401 })
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL") ?? "",
    Deno.env.get("SUPABASE_ANON_KEY") ?? "",
    { global: { headers: { Authorization: authHeader } } },
  )

  const { data: { user }, error: authError } = await supabase.auth.getUser()
  if (authError || !user) {
    return new Response("Unauthorized", { status: 401 })
  }

  if (isRateLimited(user.id)) {
    return new Response("Too many requests", { status: 429 })
  }

  let roomName: string
  let identity: string
  try {
    const body = await req.json()
    roomName = body.room_name
    identity = body.identity
    if (!roomName || !identity) throw new Error("missing fields")
  } catch {
    return new Response("Bad request: room_name and identity are required", { status: 400 })
  }

  // Ensure identity matches the authenticated user to prevent token spoofing
  if (identity !== user.id) {
    return new Response("Forbidden: identity must match authenticated user", { status: 403 })
  }

  const token = await buildLivekitToken(roomName, identity)
  return new Response(JSON.stringify({ token }), {
    headers: { "Content-Type": "application/json" },
  })
})
