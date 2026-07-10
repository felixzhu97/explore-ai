// Image Generation Feature Models — aligned with /api/images/generate

export interface ImageSize {
  label: string;
  width: number;
  height: number;
}

/** POST /api/images/generate */
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
