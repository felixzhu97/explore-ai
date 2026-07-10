// RAG Feature Models — aligned with backend DTOs (camelCase JSON)

/** POST /api/rag/chat/stream */
export interface RagQuery {
  query: string;
  sessionId?: string;
  topK?: number;
  temperature?: number;
  docIds?: string[];
  images?: string[];
}

/** Matches SourceDocumentDto / SSE sources event */
export interface SourceDocument {
  text: string;
  score: number;
  metadata: Record<string, unknown>;
}

/** GET /api/rag/documents — DocumentSummaryDto */
export interface DocumentListItem {
  id: string;
  title: string;
  status?: string;
  createdAt?: string;
  chunkCount?: number;
}

export interface DocumentListResponse {
  documents: DocumentListItem[];
}

/** UI-facing document in RAG feature */
export interface RagDocument {
  id: string;
  title: string;
}
