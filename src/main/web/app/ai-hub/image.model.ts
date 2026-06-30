// Image Generation Feature Models

export interface ImageSize {
  label: string;
  width: number;
  height: number;
}

export interface ImageGenerateParams {
  prompt: string;
  negative_prompt?: string;
  width?: number;
  height?: number;
  num_images?: number;
}

export interface ImageGenerationRequest {
  prompt: string;
  negative_prompt?: string;
  width?: number;
  height?: number;
  num_inference_steps?: number;
  guidance_scale?: number;
  seed?: number;
  num_images?: number;
  style_preset?: string;
}

export interface ImageGenerationResult {
  images: string[];
  imageUrl?: string;
  seed?: number;
}

export interface ImageGenerationResponse {
  images: string[];
  seed: number;
  model: string;
  prompt: string;
  inference_steps: number;
  guidance_scale: number;
  width: number;
  height: number;
  processing_time_ms: number;
  metadata?: Record<string, unknown>;
}
