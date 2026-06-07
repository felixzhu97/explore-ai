// Shared type definitions - migrated from React models
export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: Date;
  isLoading?: boolean;
}

export interface Agent {
  id: string;
  name: string;
  description: string;
  icon?: string;
  endpoint: string;
}

export interface ImageAnalysisResult {
  caption?: string;
  objects?: DetectedObject[];
  text?: string;
}

export interface DetectedObject {
  class: string;
  confidence: number;
  bbox: [number, number, number, number]; // [x, y, width, height]
}

export interface Document {
  id: string;
  name: string;
  size: number;
  uploadedAt: Date;
}

export interface RAGSource {
  documentId: string;
  documentName: string;
  content: string;
  similarity: number;
  pageNumber?: number;
}

export interface ImageGenerationRequest {
  prompt: string;
  negativePrompt?: string;
  width: number;
  height: number;
}

export interface ImageGenerationResult {
  imageUrl: string;
  seed?: number;
}

export interface TTSRequest {
  text: string;
  voice: string;
  speed: number;
}

export interface TTSResult {
  audioUrl: string;
  duration?: number;
}

export interface ServiceStatus {
  name: string;
  status: 'online' | 'offline' | 'error';
  latency?: number;
}
