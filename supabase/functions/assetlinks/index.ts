// Serves /.well-known/assetlinks.json for Android App Links verification
// Deploy this behind a custom domain pointing to Supabase Edge Functions
Deno.serve((_req) => {
  const assetlinks = [{
    relation: ["delegate_permission/common.handle_all_urls"],
    target: {
      namespace: "android_app",
      package_name: "com.ajrpachon.chatapp",
      sha256_cert_fingerprints: [
        Deno.env.get("APP_SIGNING_CERT_SHA256") ?? "REPLACE_WITH_CERT_SHA256"
      ]
    }
  }]
  return new Response(JSON.stringify(assetlinks), {
    headers: { "Content-Type": "application/json" }
  })
})
