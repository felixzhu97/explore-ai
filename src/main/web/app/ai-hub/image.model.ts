// Image Generation Feature Models

export interface ImageSize {
  label: string;
  width: number;
  height: number;
}

export interface ImageGenerateParams {
  prompt: string;
  model?: string;
  quality?: string;
  width?: number;
  height?: number;
  n?: number;
}

export interface ImageGenerationApiResponse {
  imageUrl?: string | null;
  imageBase64?: string | null;
  model?: string;
  prompt?: string;
  revisedPrompt?: string | null;
  status: string;
}

export interface ImageGenerationResult {
  imageUrl?: string;
  imageBase64?: string;
  model?: string;
  prompt?: string;
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
