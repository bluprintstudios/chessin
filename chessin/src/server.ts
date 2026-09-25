import { createWorkersAI } from "workers-ai-provider";
import { callable, routeAgentRequest, type Schedule } from "agents";
import { getSchedulePrompt, scheduleSchema } from "agents/schedule";
import { AIChatAgent, type OnChatMessageOptions } from "@cloudflare/ai-chat";
import {
  convertToModelMessages,
  pruneMessages,
  stepCountIs,
  streamText,
  tool
} from "ai";
import { z } from "zod";

export class ChatAgent extends AIChatAgent<Env> {
  maxPersistedMessages = 100;
  chatRecovery = true;

  waitForMcpConnections = true;

  onStart() {
    this.mcp.configureOAuthCallback({
      customHandler: (result) => {
        if (result.authSuccess) {
          return new Response("<script>window.close();</script>", {
            headers: { "content-type": "text/html" },
            status: 200
          });
        }
        return new Response(
          `Authentication Failed: ${result.authError || "Unknown error"}`,
          { headers: { "content-type": "text/plain" }, status: 400 }
        );
      }
    });
  }

  @callable()
  async addServer(name: string, url: string) {
    return await this.addMcpServer(name, url);
  }

  @callable()
  async removeServer(serverId: string) {
    await this.removeMcpServer(serverId);
  }

  async onChatMessage(_onFinish: unknown, options?: OnChatMessageOptions) {
    const mcpTools = this.mcp.getAITools();
    const workersai = createWorkersAI({ binding: this.env.AI });

    const result = streamText({
      model: workersai("@cf/moonshotai/kimi-k2.7-code", {
        sessionAffinity: this.sessionAffinity
      }),
      system: `You are a helpful assistant that can understand images. You can check the weather, get the user's timezone, run calculations, and schedule tasks. When users share images, describe what you see and answer questions about them.

${getSchedulePrompt({ date: new Date() })}

If the user asks to schedule a task, use the schedule tool to schedule the task.`,
      messages: pruneMessages({
        messages: await convertToModelMessages(this.messages),
        toolCalls: "before-last-2-messages",
        reasoning: "before-last-message"
      }),
      tools: {
        ...mcpTools,

        getWeather: tool({
          description: "Get the current weather for a city",
          inputSchema: z.object({
            city: z.string().describe("City name")
          }),
          execute: async ({ city }) => {
            const conditions = ["sunny", "cloudy", "rainy", "snowy"];
            const temp = Math.floor(Math.random() * 30) + 5;
            return {
              city,
              temperature: temp,
              condition:
                conditions[Math.floor(Math.random() * conditions.length)],
              unit: "celsius"
            };
          }
        }),

        getUserTimezone: tool({
          description:
            "Get the user's timezone from their browser. Use this when you need to know the user's local time.",
          inputSchema: z.object({})
        }),

        calculate: tool({
          description:
            "Perform a math calculation with two numbers. Requires user approval for large numbers.",
          inputSchema: z.object({
            a: z.number().describe("First number"),
            b: z.number().describe("Second number"),
            operator: z
              .enum(["+", "-", "*", "/", "%"])
              .describe("Arithmetic operator")
          }),
          needsApproval: async ({ a, b }) =>
            Math.abs(a) > 1000 || Math.abs(b) > 1000,
          execute: async ({ a, b, operator }) => {
            const ops: Record<string, (x: number, y: number) => number> = {
              "+": (x, y) => x + y,
              "-": (x, y) => x - y,
              "*": (x, y) => x * y,
              "/": (x, y) => x / y,
              "%": (x, y) => x % y
            };
            if (operator === "/" && b === 0) {
              return { error: "Division by zero" };
            }
            return {
              expression: `${a} ${operator} ${b}`,
              result: ops[operator](a, b)
            };
          }
        }),

        scheduleTask: tool({
          description:
            "Schedule a task to be executed at a later time. Use this when the user asks to be reminded or wants something done later.",
          inputSchema: scheduleSchema,
          execute: async ({ when, description }) => {
            if (when.type === "no-schedule") {
              return "Not a valid schedule input";
            }
            const input =
              when.type === "scheduled"
                ? when.date
                : when.type === "delayed"
                  ? when.delayInSeconds
                  : when.type === "cron"
                    ? when.cron
                    : null;
            if (!input) return "Invalid schedule type";
            try {
              this.schedule(input, "executeTask", description, {
                idempotent: true
              });
              return `Task scheduled: "${description}" (${when.type}: ${input})`;
            } catch (error) {
              return `Error scheduling task: ${error}`;
            }
          }
        }),

        getScheduledTasks: tool({
          description: "List all tasks that have been scheduled",
          inputSchema: z.object({}),
          execute: async () => {
            const tasks = this.getSchedules();
            return tasks.length > 0 ? tasks : "No scheduled tasks found.";
          }
        }),

        cancelScheduledTask: tool({
          description: "Cancel a scheduled task by its ID",
          inputSchema: z.object({
            taskId: z.string().describe("The ID of the task to cancel")
          }),
          execute: async ({ taskId }) => {
            try {
              this.cancelSchedule(taskId);
              return `Task ${taskId} cancelled.`;
            } catch (error) {
              return `Error cancelling task: ${error}`;
            }
          }
        })
      },
      stopWhen: stepCountIs(20),
      abortSignal: options?.abortSignal
    });

    return result.toUIMessageStreamResponse();
  }

  async executeTask(description: string, _task: Schedule<string>) {
    console.log(`Executing scheduled task: ${description}`);
    this.broadcast(
      JSON.stringify({
        type: "scheduled-task",
        description,
        timestamp: new Date().toISOString()
      })
    );
  }
}

export default {
  async fetch(request: Request, env: Env) {
    const url = new URL(request.url);

    // AI Coach SSE Streaming Endpoint for Mobile App
    if (url.pathname === "/api/coach/stream" && request.method === "POST") {
      try {
        const body = (await request.json()) as {
          context?: {
            sanMove?: string;
            classificationTier?: string;
            evalDeltaCp?: number;
            fenBefore?: string;
            fenAfter?: string;
            precedingHistorySan?: string[];
            alternativeLines?: any[];
          };
          userQuestion?: string;
        };

        const context = body.context;
        const userQuestion = body.userQuestion || "Can you explain this move?";

        const systemPrompt = `You are a Grandmaster Chess Coach providing actionable, concise instructional insights to a student.
Position & Move Context:
- Played Move: ${context?.sanMove || "unknown"}
- Classification: ${context?.classificationTier || "NORMAL"}
- Evaluation Delta (centipawns): ${context?.evalDeltaCp ?? "0"}
- FEN Before: ${context?.fenBefore || "unknown"}
- FEN After: ${context?.fenAfter || "unknown"}
- Preceding Move History: ${context?.precedingHistorySan?.join(" ") || "None"}
${context?.alternativeLines?.length ? `- Alternative PV Lines: ${JSON.stringify(context.alternativeLines)}` : ""}

Instructional Rules:
1. Explain WHY the played move is good or bad in clear tactical/positional terms.
2. If it was a blunder/mistake, highlight the tactical oversight or missed opportunity.
3. Keep responses concise (3-4 sentences max) and direct. Do not write generic fluff.`;

        const openAiKey = (env as any).OPENAI_API_KEY;

        if (openAiKey) {
          const openAiRes = await fetch("https://api.openai.com/v1/chat/completions", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${openAiKey}`
            },
            body: JSON.stringify({
              model: "gpt-4o-mini",
              stream: true,
              messages: [
                { role: "system", content: systemPrompt },
                { role: "user", content: userQuestion }
              ]
            })
          });

          if (!openAiRes.ok || !openAiRes.body) {
            return new Response(`LLM Provider error: ${openAiRes.statusText}`, { status: 502 });
          }

          const { readable, writable } = new TransformStream();
          const writer = writable.getWriter();
          const reader = openAiRes.body.getReader();
          const decoder = new TextDecoder();
          const encoder = new TextEncoder();

          (async () => {
            let buffer = "";
            try {
              while (true) {
                const { done, value } = await reader.read();
                if (done) break;
                buffer += decoder.decode(value, { stream: true });
                const lines = buffer.split("\n");
                buffer = lines.pop() || "";

                for (const line of lines) {
                  const trimmed = line.trim();
                  if (trimmed.startsWith("data: ")) {
                    const dataStr = trimmed.slice(6);
                    if (dataStr === "[DONE]") continue;
                    try {
                      const parsed = JSON.parse(dataStr);
                      const text = parsed.choices?.[0]?.delta?.content;
                      if (text) {
                        await writer.write(encoder.encode(`data: ${text}\n\n`));
                      }
                    } catch {}
                  }
                }
              }
              await writer.write(encoder.encode(`data: [DONE]\n\n`));
            } catch {
              await writer.write(encoder.encode(`data: [ERROR] Stream interrupted\n\n`));
            } finally {
              await writer.close();
            }
          })();

          return new Response(readable, {
            headers: {
              "Content-Type": "text/event-stream",
              "Cache-Control": "no-cache",
              Connection: "keep-alive",
              "Access-Control-Allow-Origin": "*"
            }
          });
        } else {
          // Fallback: simulated SSE stream if OPENAI_API_KEY secret is not yet set
          const { readable, writable } = new TransformStream();
          const writer = writable.getWriter();
          const encoder = new TextEncoder();

          (async () => {
            const simulatedText = `Playing ${context?.sanMove || "this move"} was classified as ${context?.classificationTier || "GOOD"}. It controls key squares and maintains a solid position.`;
            const words = simulatedText.split(" ");
            for (const word of words) {
              await writer.write(encoder.encode(`data: ${word} \n\n`));
              await new Promise((r) => setTimeout(r, 60));
            }
            await writer.write(encoder.encode(`data: [DONE]\n\n`));
            await writer.close();
          })();

          return new Response(readable, {
            headers: {
              "Content-Type": "text/event-stream",
              "Cache-Control": "no-cache",
              Connection: "keep-alive",
              "Access-Control-Allow-Origin": "*"
            }
          });
        }
      } catch (err: any) {
        return new Response(`Bad Request: ${err?.message || err}`, { status: 400 });
      }
    }

    return (
      (await routeAgentRequest(request, env)) ||
      new Response("Not found", { status: 404 })
    );
  }
} satisfies ExportedHandler<Env>;
