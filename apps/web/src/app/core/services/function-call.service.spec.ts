import { describe, it, expect, beforeEach, vi } from "vitest";
import { FunctionCallService, FunctionCallEvent } from "./function-call.service";

describe("FunctionCallService", () => {
  let service: FunctionCallService;

  beforeEach(() => {
    service = new FunctionCallService();
  });

  describe("chatStream", () => {
    it("should return an Observable", () => {
      const result = service.chatStream("test message");
      expect(result).toBeDefined();
    });

    it("should call fetch with correct parameters", async () => {
      const fetchSpy = vi.spyOn(global, "fetch").mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as any);

      const subscription = service.chatStream("test message").subscribe();

      await new Promise((resolve) => setTimeout(resolve, 50));

      expect(fetchSpy).toHaveBeenCalledWith(
        "/api/mcp/function-call/stream",
        expect.objectContaining({
          method: "POST",
          headers: { "Content-Type": "application/json" },
        })
      );

      subscription.unsubscribe();
    });

    it("should emit truncated event on HTTP error", async () => {
      vi.spyOn(global, "fetch").mockResolvedValue({
        ok: false,
        status: 500,
        statusText: "Internal Server Error",
      } as any);

      const events: FunctionCallEvent[] = [];
      const subscription = service.chatStream("test").subscribe({
        next: (event) => events.push(event),
      });

      await new Promise((resolve) => setTimeout(resolve, 50));

      expect(events.some((e) => e.type === "truncated")).toBe(true);
      subscription.unsubscribe();
    });

    it("should emit truncated event when no body", async () => {
      vi.spyOn(global, "fetch").mockResolvedValue({
        ok: true,
        body: null,
      } as any);

      const events: FunctionCallEvent[] = [];
      const subscription = service.chatStream("test").subscribe({
        next: (event) => events.push(event),
      });

      await new Promise((resolve) => setTimeout(resolve, 50));

      expect(events.some((e) => e.type === "truncated")).toBe(true);
      subscription.unsubscribe();
    });

    it("should include sessionId in request body when provided", async () => {
      const fetchSpy = vi.spyOn(global, "fetch").mockResolvedValue({
        ok: true,
        body: {
          getReader: () => ({
            read: vi.fn().mockResolvedValue({ done: true, value: new Uint8Array() }),
          }),
        },
      } as any);

      service.chatStream("test", "session-123").subscribe();

      await new Promise((resolve) => setTimeout(resolve, 50));

      expect(fetchSpy).toHaveBeenCalledWith(
        "/api/mcp/function-call/stream",
        expect.objectContaining({
          body: JSON.stringify({ message: "test", sessionId: "session-123" }),
        })
      );
    });
  });
});
