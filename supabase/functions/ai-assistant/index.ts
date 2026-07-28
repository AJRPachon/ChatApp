// Supabase Edge Function: ai-assistant
// Accepts { action: "summarize"|"suggest"|"freeform", messages?: string[], prompt?: string }
// Returns { result: string }
// TODO: replace mock responses with real LLM API calls (e.g. OpenAI, Anthropic)

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { action, messages, prompt } = await req.json();

    let result: string;

    switch (action) {
      case "summarize": {
        // `result` is inserted verbatim into the chat input on the client (InsertAiSuggestion),
        // so it must contain only the usable text — no "[Mock] ..." label describing it.
        result =
          "Los participantes discutieron varios temas. " +
          "Se mencionaron puntos importantes sobre el proyecto y se acordaron próximos pasos.";
        break;
      }
      case "suggest": {
        result = "Entendido, me parece bien. ¿Podemos coordinar los detalles más tarde?";
        break;
      }
      case "freeform": {
        result =
          "Esta es una respuesta de ejemplo del asistente de IA. " +
          "Cuando configures credenciales reales, aquí aparecerá la respuesta del modelo.";
        break;
      }
      default:
        return new Response(
          JSON.stringify({ error: `Unknown action: ${action}` }),
          { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
        );
    }

    return new Response(
      JSON.stringify({ result }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ error: String(err) }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  }
});
