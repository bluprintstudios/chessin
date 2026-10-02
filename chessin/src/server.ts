import { DurableObject } from "cloudflare:workers";

export interface Env {
  AI: any;
}

// We leave this dummy class here so your existing wrangler.json doesn't crash during deployment
export class ChatAgent extends DurableObject {
  async fetch() {
    return new Response("Agent disabled");
  }
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    // 1. Handle CORS so your Android app can connect freely
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "POST, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type",
        },
      });
    }

    const url = new URL(request.url);

    // 2. The endpoint your Android app will call
    if (request.method === "POST" && url.pathname === "/api/coach") {
      try {
        const { fen, userMessage, moveHistory } = await request.json() as any;

        const systemPrompt = `You are an encouraging, expert chess coach.
Analyze positions and moves conversationally. Explain concepts like pawn structure, king safety, center control, and opening plans in simple terms. Avoid just listing engine numbers—explain the 'why' behind moves.`;

        const prompt = `Current Position (FEN): ${fen || "Starting Position"}
Recent Moves: ${moveHistory || "None"}
User Question/Move: ${userMessage}`;

        // Call Cloudflare Workers AI (using a free-tier model to avoid the 5035 error)
        const response = await env.AI.run("@cf/meta/llama-3.1-8b-instruct", {
          messages: [
            { role: "system", content: systemPrompt },
            { role: "user", content: prompt },
          ],
        });

        return new Response(JSON.stringify({ advice: response.response }), {
          headers: {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*",
          },
        });
      } catch (err: any) {
        return new Response(JSON.stringify({ error: err.message }), {
          status: 500,
          headers: { "Content-Type": "application/json" },
        });
      }
    }

    return new Response("Not Found", { status: 404 });
  },
};