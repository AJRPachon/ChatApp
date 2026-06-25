import "@supabase/functions-js/edge-runtime.d.ts"
import { createClient } from "jsr:@supabase/supabase-js@2"

const PACKAGE_NAME = "com.ajrpachon.chatapp"

// GOOGLE_SERVICE_ACCOUNT_JSON must be set via `supabase secrets set`
// It is the JSON key file of a Google Cloud service account with the
// "Android Device Verification > Play Integrity API" role enabled.
const SERVICE_ACCOUNT_JSON = Deno.env.get("GOOGLE_SERVICE_ACCOUNT_JSON") ?? ""

async function getAccessToken(): Promise<string> {
  const sa = JSON.parse(SERVICE_ACCOUNT_JSON)
  const now = Math.floor(Date.now() / 1000)
  const header = btoa(JSON.stringify({ alg: "RS256", typ: "JWT" }))
    .replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "")
  const payload = btoa(JSON.stringify({
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/playintegrity",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  })).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "")

  const data = `${header}.${payload}`
  const keyPem = sa.private_key.replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\n/g, "")
  const keyBytes = Uint8Array.from(atob(keyPem), (c) => c.charCodeAt(0))
  const key = await crypto.subtle.importKey(
    "pkcs8", keyBytes.buffer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false, ["sign"],
  )
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(data))
  const sigB64 = btoa(String.fromCharCode(...new Uint8Array(sig)))
    .replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "")
  const jwt = `${data}.${sigB64}`

  const tokenRes = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
  })
  const tokenJson = await tokenRes.json()
  return tokenJson.access_token
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("Method not allowed", { status: 405 })

  const authHeader = req.headers.get("Authorization")
  if (!authHeader) return new Response("Unauthorized", { status: 401 })

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL") ?? "",
    Deno.env.get("SUPABASE_ANON_KEY") ?? "",
    { global: { headers: { Authorization: authHeader } } },
  )
  const { data: { user }, error } = await supabase.auth.getUser()
  if (error || !user) return new Response("Unauthorized", { status: 401 })

  let token: string, nonce: string
  try {
    const body = await req.json()
    token = body.token
    nonce = body.nonce
    if (!token || !nonce) throw new Error("missing fields")
  } catch {
    return new Response("Bad request", { status: 400 })
  }

  try {
    const accessToken = await getAccessToken()
    const verifyRes = await fetch(
      `https://playintegrity.googleapis.com/v1/${PACKAGE_NAME}:decodeIntegrityToken`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ integrity_token: token }),
      },
    )
    const verdict = await verifyRes.json()
    const tokenPayload = verdict.tokenPayloadExternal

    const appVerdict = tokenPayload?.appIntegrity?.appRecognitionVerdict ?? ""
    const deviceVerdicts: string[] = tokenPayload?.deviceIntegrity?.deviceRecognitionVerdict ?? []
    const licensingVerdict = tokenPayload?.accountDetails?.appLicensingVerdict ?? ""
    const requestNonce = tokenPayload?.requestDetails?.nonce ?? ""

    // Verify nonce matches to bind the token to this specific request
    const passed =
      requestNonce === nonce &&
      appVerdict === "PLAY_RECOGNIZED" &&
      deviceVerdicts.includes("MEETS_DEVICE_INTEGRITY") &&
      licensingVerdict === "LICENSED"

    const reason = !passed
      ? `app=${appVerdict} device=${deviceVerdicts.join(",")} licensing=${licensingVerdict} nonceMatch=${requestNonce === nonce}`
      : undefined

    return new Response(JSON.stringify({ passed, reason }), {
      headers: { "Content-Type": "application/json" },
    })
  } catch (e) {
    return new Response(JSON.stringify({ passed: false, reason: `server_error: ${e.message}` }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    })
  }
})
