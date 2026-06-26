// Vision Feature Models

export interface ImageAnalysisResult {
  caption?: string;
  objects?: DetectedObject[];
  text?: string;
}

export interface DetectedObject {
  class: string;
  class_name?: string;
  confidence: number;
  bbox: [number, number, number, number]; // [x, y, width, height]
}

export interface VisionResult {
  caption?: string;
  detections?: Detection[];
  full_text?: string;
  processing_time_ms?: number;
}

export interface Detection {
  class_name: string;
  confidence: number;
  bbox: [number, number, number, number];
}
