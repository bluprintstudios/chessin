import { DurableObject } from "cloudflare:workers";

export interface Env {
  AI: any;
}

// Required Durable Object class to satisfy wrangler.json bindings and exports
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
          "Access-Control-Allow-Headers": "Content-Type, Authorization",
        },
      });
    }

    const url = new URL(request.url);

    // 2. The streaming endpoint your Android app calls
    if (request.method === "POST" && url.pathname === "/api/coach/stream") {
      try {
        const authHeader = request.headers.get("Authorization");
        if (!authHeader || !authHeader.startsWith("Bearer ")) {
          return new Response("Unauthorized: Missing Firebase Token", { status: 401 });
        }

        const { fen, userMessage, moveHistory } = await request.json() as any;

        const systemPrompt = `You are an encouraging, expert chess coach. 
Analyze positions and moves conversationally. Explain concepts like pawn structure, king safety, center control, and opening plans in simple terms. Avoid just listing engine numbers—explain the 'why' behind moves.`;

        const prompt = `Current Position (FEN): ${fen || "Starting Position"}
Recent Moves: ${moveHistory || "None"}
User Question/Move: ${userMessage}`;

        // Call Cloudflare Workers AI with streaming enabled
        const stream = await env.AI.run("@cf/meta/llama-3.1-8b-instruct", {
          messages: [
            { role: "system", content: systemPrompt },
            { role: "user", content: prompt },
          ],
          stream: true,
        });

        return new Response(stream, {
          headers: {
            "Content-Type": "text/event-stream",
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