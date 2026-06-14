import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { HttpClientTestingModule, HttpTestingController } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { McpService } from "./mcp.service";

describe("McpService", () => {
  let service: McpService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [McpService],
    });

    service = TestBed.inject(McpService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  describe("listTools", () => {
    it("should return tools from API", () => {
      const mockTools = [
        {
          name: "test_tool",
          description: "A test tool",
          inputSchema: { type: "object" },
          composite: false,
        },
      ];

      service.listTools().subscribe((tools) => {
        expect(tools).toEqual(mockTools);
      });

      const req = httpMock.expectOne("/api/mcp/.well-known/tools");
      req.flush(mockTools);
    });

    it("should handle empty tools array", () => {
      service.listTools().subscribe((tools) => {
        expect(tools).toEqual([]);
      });

      const req = httpMock.expectOne("/api/mcp/.well-known/tools");
      req.flush([]);
    });

    it("should include composite flag when present", () => {
      const mockTools = [
        {
          name: "composite_tool",
          description: "A composite tool",
          inputSchema: { type: "object" },
          composite: true,
        },
      ];

      service.listTools().subscribe((tools) => {
        expect(tools[0].composite).toBe(true);
      });

      const req = httpMock.expectOne("/api/mcp/.well-known/tools");
      req.flush(mockTools);
    });
  });

  describe("invokeTool", () => {
    it("should send JSON-RPC request to API", () => {
      const mockResult = {
        content: "Tool execution result",
        isError: false,
        structured: { result: "success" },
      };

      service.invokeTool("test_tool", { arg1: "value1" }).subscribe((result) => {
        expect(result).toEqual(mockResult);
      });

      const req = httpMock.expectOne("/api/mcp/messages");
      expect(req.request.method).toBe("POST");
      expect(req.request.body).toMatchObject({
        jsonrpc: "2.0",
        method: "tools/call",
        params: { name: "test_tool", arguments: { arg1: "value1" } },
      });
      req.flush(mockResult);
    });

    it("should handle error response", () => {
      const mockResult = {
        content: "Error occurred",
        isError: true,
      };

      service.invokeTool("error_tool", {}).subscribe((result) => {
        expect(result.isError).toBe(true);
      });

      const req = httpMock.expectOne("/api/mcp/messages");
      req.flush(mockResult);
    });

    it("should generate unique id for each request", () => {
      const mockResult = { content: "result", isError: false };

      service.invokeTool("tool1", {}).subscribe();
      service.invokeTool("tool2", {}).subscribe();

      const requests = httpMock.match("/api/mcp/messages");
      expect(requests.length).toBe(2);
      expect(requests[0].request.body.id).toBeTruthy();
      expect(requests[1].request.body.id).toBeTruthy();
      expect(requests[0].request.body.id).not.toBe(requests[1].request.body.id);
    });
  });
});
