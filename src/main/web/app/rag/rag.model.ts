// RAG Feature Models — aligned with backend DTOs (camelCase JSON)

/** POST /api/rag/chat/stream */
export interface RagQuery {
  question: string;
  sessionId?: string;
  topK?: number;
  temperature?: number;
  documentIds?: string[];
}

/** Matches SourceDocumentResponse / SSE sources event */
export interface SourceDocument {
  id: string;
  content: string;
  score: number;
  metadata: Record<string, unknown>;
}

/** GET /api/rag/documents — DocumentSummaryResponse */
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
