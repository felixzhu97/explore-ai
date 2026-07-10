// Vision Feature Models — aligned with backend vision DTOs (camelCase JSON)

export interface VisionResult {
  caption?: string;
  detections?: Detection[];
  fullText?: string;
  processingTimeMs?: number;
}

export interface Detection {
  className: string;
  confidence: number;
  bbox: [number, number, number, number];
}
