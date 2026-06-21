// Shared Type Definitions - Re-exports from feature-specific models
// Types used by core services or multiple features

// Chat types
export type { ChatMessage, ChatMessageData, ChatRequest, ChatResponse, ModelInfo, ProviderInfo } from '../features/ai/chat/chat.model';

// Agent types
export type { Agent, AgentInfo, ToolCall, ToolCallStatus, ChatMessageData as AgentChatMessageData } from '../features/agents/models/agent.model';

// RAG types
export type { RagQuery, SourceDocument, Document, DocumentListResponse, DocumentListItem, RAGSource } from '../features/ai/rag/rag.model';

// Image types
export type { ImageGenerateParams, ImageGenerationRequest, ImageGenerationResult, ImageGenerationResponse } from '../features/ai/image/image.model';

// Vision types
export type { VisionResult, Detection, ImageAnalysisResult, DetectedObject } from '../features/ai/vision/vision.model';

// TTS types
export type { Voice, TTSRequest, TTSResult, SynthesizeRequest } from '../features/ai/tts/tts.model';

// Service types (generic, kept here)
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
