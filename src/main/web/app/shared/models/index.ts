// Shared Type Definitions - Re-exports from feature-specific models
// Types used by core services or multiple features

// Chat types
export type { ChatMessage, ChatMessageData, ChatRequest, ChatResponse, ModelInfo, ProviderInfo } from '../../ai-hub/chat/chat.model';

// Agent types
export type { Agent, AgentInfo, ToolCall, ToolCallStatus, ChatMessageData as AgentChatMessageData } from '../../ai-infra/agent/agent.model';

// RAG types
export type { RagQuery, SourceDocument, Document, DocumentListResponse, DocumentListItem, RAGSource } from '../../rag/rag.model';

// Image types
export type { ImageGenerateParams, ImageGenerationRequest, ImageGenerationResult, ImageGenerationResponse } from '../../ai-hub/image/image.model';

// Vision types
export type { VisionResult, Detection, ImageAnalysisResult, DetectedObject } from '../../vision/vision.model';

// TTS types
export type { Voice, TTSRequest, TTSResult, SynthesizeRequest } from '../../ai-hub/tts/tts.model';

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
