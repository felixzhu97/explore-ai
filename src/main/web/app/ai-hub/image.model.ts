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

export interface ImageCatalogResponse {
  models: string[];
  sizes: string[];
  qualities: string[];
}

export function parseImageSizeLabel(label: string): ImageSize | null {
  const match = /^(\d+)x(\d+)$/.exec(label.trim());
  if (!match) {
    return null;
  }
  return {
    label,
    width: Number.parseInt(match[1], 10),
    height: Number.parseInt(match[2], 10),
  };
}

export const DEFAULT_IMAGE_SIZES: ImageSize[] = [
  { label: '512x512', width: 512, height: 512 },
  { label: '768x768', width: 768, height: 768 },
  { label: '1024x1024', width: 1024, height: 1024 },
];
