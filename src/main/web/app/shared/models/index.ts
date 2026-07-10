// Shared Type Definitions - Re-exports from feature-specific models

// Chat types
export type {
  ChatMessage,
  ChatMessageData,
  ChatStreamRequest,
  ModelInfo,
  ProviderInfo,
  SessionInfo,
  ToolCall,
} from '../../ai-hub/chat.model';

// RAG types
export type {
  RagQuery,
  SourceDocument,
  RagDocument,
  DocumentListResponse,
  DocumentListItem,
} from '../../rag/rag.model';

// Image types
export type {
  ImageGenerateParams,
  ImageGenerationApiResponse,
  ImageGenerationResult,
} from '../../ai-hub/image.model';

// Vision types
export type { VisionResult, Detection } from '../../vision/vision.model';

// TTS types
export type { Voice, TtsRequest } from '../../ai-hub/tts.model';

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
