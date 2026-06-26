// RAG Feature Models

export interface RagQuery {
  query: string;
  session_id?: string;
  top_k?: number;
  temperature?: number;
  doc_ids?: string[];
}

export interface SourceDocument {
  text: string;
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
