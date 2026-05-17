/**
 * Image Generation API Client
 * 
 * HTTP client for the Media Generation Service (port 3456).
 * Provides type-safe interface for text-to-image generation.
 */

import { z } from 'zod';

const ImageGenerationRequestSchema = z.object({
  prompt: z.string().min(1).max(1000),
  negative_prompt: z.string().max(500).optional(),
  width: z.number().int().min(256).max(1024).optional(),
  height: z.number().int().min(256).max(1024).optional(),
  num_inference_steps: z.number().int().min(1).max(100).optional(),
  guidance_scale: z.number().min(1.0).max(20.0).optional(),
  seed: z.number().int().min(0).optional(),
  num_images: z.number().int().min(1).max(4).optional(),
});

const ImageGenerationResponseSchema = z.object({
  images: z.array(z.string()),
  seed: z.number(),
  model: z.string(),
  prompt: z.string(),
  width: z.number(),
  height: z.number(),
  num_inference_steps: z.number(),
  guidance_scale: z.number(),
  processing_time_ms: z.number(),
});

const HealthResponseSchema = z.object({
  status: z.literal('ok'),
  model: z.string(),
  device: z.string(),
  cuda_available: z.boolean(),
  mps_available: z.boolean(),
  cuda_device_count: z.number(),
  pipeline_loaded: z.boolean(),
});

export type ImageGenerationRequest = z.infer<typeof ImageGenerationRequestSchema>;
export type ImageGenerationResponse = z.infer<typeof ImageGenerationResponseSchema>;
export type HealthResponse = z.infer<typeof HealthResponseSchema>;

const DEFAULT_BASE_URL = 'http://localhost:3456';
const DEFAULT_TIMEOUT = 300_000;

export class ImageGenerationClient {
  private baseUrl: string;
  private timeout: number;

  constructor(baseUrl: string = DEFAULT_BASE_URL, timeout: number = DEFAULT_TIMEOUT) {
    this.baseUrl = baseUrl.replace(/\/$/, '');
    this.timeout = timeout;
  }

  private async request<T>(
    method: 'GET' | 'POST',
    endpoint: string,
    body?: unknown
  ): Promise<T> {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), this.timeout);

    try {
      const response = await fetch(`${this.baseUrl}${endpoint}`, {
        method,
        headers: {
          'Content-Type': 'application/json',
        },
        body: body ? JSON.stringify(body) : undefined,
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        const error = await response.json().catch(() => ({ detail: response.statusText }));
        throw new Error(error.detail || `HTTP ${response.status}`);
      }

      return response.json() as Promise<T>;
    } catch (error) {
      clearTimeout(timeoutId);
      if (error instanceof Error && error.name === 'AbortError') {
        throw new Error('Request timeout');
      }
      throw error;
    }
  }

  async health(): Promise<HealthResponse> {
    return this.request<HealthResponse>('GET', '/health');
  }

  async generate(params: Partial<ImageGenerationRequest> & { prompt: string }): Promise<ImageGenerationResponse> {
    const validated = ImageGenerationRequestSchema.parse(params);
    return this.request<ImageGenerationResponse>('POST', '/image/generate', validated);
  }

  async clearCache(): Promise<{ status: string; message: string }> {
    return this.request<{ status: string; message: string }>('POST', '/cache/clear');
  }

  base64ToDataUrl(base64: string, mimeType = 'image/png'): string {
    return `data:${mimeType};base64,${base64}`;
  }
}

export const imageGenClient = new ImageGenerationClient();

export default ImageGenerationClient;
