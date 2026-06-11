import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SERVICE_ROLE_JWT")!;
const FIREBASE_PROJECT_ID = Deno.env.get("FIREBASE_PROJECT_ID")!;
// Service account fields — paste individual fields instead of the full JSON file
const SA_CLIENT_EMAIL = Deno.env.get("SA_CLIENT_EMAIL")!;
const SA_PRIVATE_KEY = Deno.env.get("SA_PRIVATE_KEY")!; // with \n for newlines

const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

// ── FCM v1 OAuth2 via service account fields ───────────────────────────────

async function getAccessToken(): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const payload = {
    iss: SA_CLIENT_EMAIL,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    exp: now + 3600,
    iat: now,
  };

  const encode = (obj: unknown) =>
    btoa(JSON.stringify(obj))
      .replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");

  const signingInput = `${encode({ alg: "RS256", typ: "JWT" })}.${encode(payload)}`;

  const pemContents = SA_PRIVATE_KEY
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\\n/g, "")
    .replace(/\s/g, "");

  const keyBytes = Uint8Array.from(atob(pemContents), (c) => c.charCodeAt(0));
  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    keyBytes,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    new TextEncoder().encode(signingInput),
  );
  const sig = btoa(String.fromCharCode(...new Uint8Array(signature)))
    .replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");

  const jwt = `${signingInput}.${sig}`;

  const resp = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });
  const data = await resp.json();
  if (!resp.ok) throw new Error(`OAuth2 error: ${JSON.stringify(data)}`);
  return data.access_token;
}

// ── Send FCM v1 notification ───────────────────────────────────────────────

// Returns true if the token is stale and should be deleted from the DB.
async function sendFcm(
  token: string,
  title: string,
  body: string,
  data: Record<string, string>,
  accessToken: string,
): Promise<boolean> {
  const url = `https://fcm.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/messages:send`;
  const resp = await fetch(url, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      message: {
        token,
        data: { ...data, title, body },
        android: {
          priority: "high",
        },
      },
    }),
  });

  const respText = await resp.text();
  if (!resp.ok) {
    console.error(`FCM error [${resp.status}] for token ${token.slice(0, 20)}…`, respText);
    // 404 NOT_FOUND / UNREGISTERED → token is invalid; mark for deletion.
    const isStale = resp.status === 404 || resp.status === 410 ||
      respText.includes("NOT_FOUND") || respText.includes("UNREGISTERED");
    return isStale;
  }
  console.log(`FCM sent OK for token ${token.slice(0, 20)}…`);
  return false;
}

// ── Handler ────────────────────────────────────────────────────────────────

Deno.serve(async (req) => {
  try {
    const payload = await req.json();
    console.log("Payload received:", JSON.stringify(payload).slice(0, 200));
    const record = payload.record ?? payload;

    const conversationId: string = record.conversation_id;
    const senderId: string = record.sender_id;
    const content: string = record.content ?? "";
    const imageUrl: string | null = record.image_url ?? null;
    const gifUrl: string | null = record.gif_url ?? null;
    const stickerUrl: string | null = record.sticker_url ?? null;
    const audioUrl: string | null = record.audio_url ?? null;
    const callType: string | null = record.call_type ?? null;
    const callStatus: string | null = record.call_status ?? null;

    let notifBody = content;
    if (callType) {
      const callLabel = callType === "video" ? "Videollamada" : "Llamada de voz";
      if (callStatus === "missed") notifBody = `📵 ${callLabel} perdida`;
      else if (callStatus === "rejected") notifBody = `❌ ${callLabel} rechazada`;
      else notifBody = `📞 ${callLabel}`;
    } else if (imageUrl) notifBody = "📷 Foto";
    else if (gifUrl) notifBody = "GIF";
    else if (stickerUrl) notifBody = stickerUrl;
    else if (audioUrl) notifBody = "🎤 Audio";

    if (!notifBody.trim()) return new Response("No content", { status: 200 });

    const { data: senderProfile } = await supabase
      .from("profiles")
      .select("username, display_name")
      .eq("id", senderId)
      .single();
    const senderName =
      senderProfile?.username || senderProfile?.display_name || "Alguien";

    const { data: conv } = await supabase
      .from("conversations")
      .select("name, is_group")
      .eq("id", conversationId)
      .single();
    const isGroup = conv?.is_group ?? false;
    const notifTitle = isGroup ? (conv?.name ?? "Grupo") : senderName;
    const notifBodyFinal = isGroup ? `${senderName}: ${notifBody}` : notifBody;

    const { data: participants, error: participantsError } = await supabase
      .from("conversation_participants")
      .select("user_id")
      .eq("conversation_id", conversationId)
      .neq("user_id", senderId);

    if (participantsError) console.error("participants query error:", participantsError);

    if (!participants?.length) {
      console.log("No recipients for conversation", conversationId, "sender", senderId, "error:", participantsError?.message);
      return new Response("No recipients", { status: 200 });
    }

    const recipientIds = participants.map((p: { user_id: string }) => p.user_id);
    console.log("Recipients:", recipientIds);

    const { data: tokenRows } = await supabase
      .from("fcm_tokens")
      .select("token")
      .in("user_id", recipientIds);

    console.log("Tokens found:", tokenRows?.length ?? 0);
    if (!tokenRows?.length) return new Response("No tokens", { status: 200 });

    const accessToken = await getAccessToken();

    const results = await Promise.allSettled(
      tokenRows.map((row: { token: string }) =>
        sendFcm(row.token, notifTitle, notifBodyFinal, { conversation_id: conversationId }, accessToken)
          .then((isStale) => ({ token: row.token, isStale }))
      ),
    );

    // Delete stale/expired tokens so they are never retried.
    const staleTokens = results
      .filter((r): r is PromiseFulfilledResult<{ token: string; isStale: boolean }> =>
        r.status === "fulfilled" && r.value.isStale)
      .map((r) => r.value.token);

    if (staleTokens.length > 0) {
      console.log("Deleting stale tokens:", staleTokens.length);
      await supabase.from("fcm_tokens").delete().in("token", staleTokens);
    }

    return new Response("OK", { status: 200 });
  } catch (e) {
    console.error("send-fcm-notification error", e);
    return new Response(String(e), { status: 500 });
  }
});
