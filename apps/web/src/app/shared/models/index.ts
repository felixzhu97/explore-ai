// =============================================================================
// Shared Type Definitions - Unified Types for AI/Agent Services
// =============================================================================

// ==================== Chat Types ====================

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

export interface ChatMessageData {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: number | Date;
  toolCalls?: ToolCall[];
  isLoading?: boolean;
}

export interface ChatRequest {
  messages: ChatMessage[];
  session_id?: string;
  system_prompt?: string;
  temperature?: number;
  max_tokens?: number;
  provider?: string;
  model?: string;
}

export interface ChatResponse {
  text: string;
  response?: string;
  provider: string;
  model: string;
  session_id: string;
  usage?: Record<string, number>;
  finish_reason?: string;
}

// ==================== Agent Types ====================

export interface Agent {
  id: string;
  name: string;
  description: string;
  icon?: string;
  endpoint: string;
}

export interface AgentInfo {
  name: string;
  description: string;
  status?: 'online' | 'offline' | 'busy';
}

// ==================== Tool Types ====================

export type ToolCallStatus = 'pending' | 'running' | 'success' | 'error';

export interface ToolCall {
  id: string;
  name: string;
  input: Record<string, unknown>;
  output?: string;
  status: ToolCallStatus;
}

// ==================== Model/Provider Types ====================

export interface ModelInfo {
  name: string;
  provider: string;
  description?: string;
  max_tokens?: number;
}

export interface ProviderInfo {
  name: string;
  display_name: string;
  models: string[];
  status: 'available' | 'configured' | 'unavailable';
  supported_languages?: string[];
  features?: string[];
}

// ==================== RAG Types ====================

export interface RagQuery {
  query: string;
  session_id?: string;
  top_k?: number;
  temperature?: number;
  doc_ids?: string[];
}

export interface SourceDocument {
  text?: string;
  content?: string;
  score: number;
  metadata: Record<string, unknown>;
}

export interface RAGSource {
  documentId: string;
  documentName: string;
  content: string;
  similarity: number;
  pageNumber?: number;
}

export interface Document {
  id: string;
  name: string;
  size: number;
  uploadedAt: Date;
  title?: string;
  filename?: string;
}

// Unified document list response type
export interface DocumentListResponse {
  documents: DocumentListItem[];
}

export interface DocumentListItem {
  id?: string;
  doc_id?: string;
  title?: string;
  filename?: string;
  name?: string;
}

// ==================== Image Generation Types ====================

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

// ==================== Vision Types ====================

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

// ==================== TTS Types ====================

export interface TTSRequest {
  text: string;
  voice?: string;
  speed?: number;
  output_format?: string;
}

export interface TTSResult {
  audioUrl: string;
  duration?: number;
}

export interface Voice {
  id: string;
  name: string;
  language: string;
  language_name?: string;
  gender?: string;
  provider: string;
  is_default: boolean;
}

export interface SynthesizeRequest {
  text: string;
  voice?: string;
  language?: string;
  speed?: number;
  pitch?: number;
  output_format?: 'mp3' | 'wav' | 'ogg' | 'flac';
}

// ==================== Service Types ====================

export interface ServiceStatus {
  name: string;
  status: 'online' | 'offline' | 'error';
  latency?: number;
}

export interface HealthResponse {
  status: string;
  provider?: string;
  model?: string;
  version: string;
}
