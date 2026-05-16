"""
Document metadata persistence store using SQLite.
Supports document tracking, incremental updates, and indexing history.
"""
import sqlite3
import json
from typing import Optional, Any
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from loguru import logger
from ..config import get_settings


@dataclass
class DocumentRecord:
    """Document record"""
    doc_id: str
    title: str
    source: str
    filename: Optional[str] = None
    file_size: Optional[int] = None
    mime_type: Optional[str] = None
    chunk_count: int = 0
    chunk_size: int = 500
    chunk_overlap: int = 50
    status: str = "pending"  # pending, indexing, completed, failed
    error_message: Optional[str] = None
    created_at: Optional[str] = None
    updated_at: Optional[str] = None
    indexed_at: Optional[str] = None
    version: int = 1
    metadata_json: Optional[str] = None


class DocumentMetadataStore:
    """
    Document metadata persistence store.

    Uses SQLite for storing:
    - Document basic info (title, source, size, etc.)
    - Indexing status and history
    - Chunking configuration
    - Version info (supports incremental update tracking)
    """

    def __init__(self, db_path: Optional[str] = None):
        settings = get_settings()
        self.db_path = db_path or settings.DOCUMENT_DB_PATH
        self._ensure_db_dir()
        self._init_db()

    def _ensure_db_dir(self) -> None:
        """Ensure database directory exists"""
        db_dir = Path(self.db_path).parent
        db_dir.mkdir(parents=True, exist_ok=True)

    def _get_connection(self) -> sqlite3.Connection:
        """Get database connection"""
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        return conn

    def _init_db(self) -> None:
        """Initialize database tables"""
        with self._get_connection() as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS documents (
                    doc_id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    source TEXT NOT NULL,
                    filename TEXT,
                    file_size INTEGER,
                    mime_type TEXT,
                    chunk_count INTEGER DEFAULT 0,
                    chunk_size INTEGER DEFAULT 500,
                    chunk_overlap INTEGER DEFAULT 50,
                    status TEXT DEFAULT 'pending',
                    error_message TEXT,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    indexed_at TEXT,
                    version INTEGER DEFAULT 1,
                    metadata_json TEXT,
                    UNIQUE(doc_id)
                )
            """)

            conn.execute("""
                CREATE TABLE IF NOT EXISTS document_chunks (
                    chunk_id TEXT PRIMARY KEY,
                    doc_id TEXT NOT NULL,
                    text_preview TEXT NOT NULL,
                    token_count INTEGER,
                    vector_id TEXT,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (doc_id) REFERENCES documents(doc_id) ON DELETE CASCADE
                )
            """)

            conn.execute("""
                CREATE TABLE IF NOT EXISTS indexing_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    doc_id TEXT NOT NULL,
                    action TEXT NOT NULL,
                    chunk_count INTEGER,
                    status TEXT NOT NULL,
                    error_message TEXT,
                    started_at TEXT NOT NULL,
                    completed_at TEXT,
                    duration_ms INTEGER,
                    FOREIGN KEY (doc_id) REFERENCES documents(doc_id) ON DELETE CASCADE
                )
            """)

            conn.execute("""
                CREATE INDEX IF NOT EXISTS idx_documents_status
                ON documents(status)
            """)
            conn.execute("""
                CREATE INDEX IF NOT EXISTS idx_documents_source
                ON documents(source)
            """)
            conn.execute("""
                CREATE INDEX IF NOT EXISTS idx_chunks_doc_id
                ON document_chunks(doc_id)
            """)
            conn.execute("""
                CREATE INDEX IF NOT EXISTS idx_history_doc_id
                ON indexing_history(doc_id)
            """)

            conn.commit()
            logger.info(f"Document metadata database initialized at {self.db_path}")

    def add_document(self, record: DocumentRecord) -> bool:
        """Add a document record"""
        now = datetime.now().isoformat()
        try:
            with self._get_connection() as conn:
                conn.execute("""
                    INSERT OR REPLACE INTO documents
                    (doc_id, title, source, filename, file_size, mime_type,
                     chunk_count, chunk_size, chunk_overlap, status, error_message,
                     created_at, updated_at, indexed_at, version, metadata_json)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (
                    record.doc_id,
                    record.title,
                    record.source,
                    record.filename,
                    record.file_size,
                    record.mime_type,
                    record.chunk_count,
                    record.chunk_size,
                    record.chunk_overlap,
                    record.status,
                    record.error_message,
                    now,
                    now,
                    record.indexed_at,
                    record.version,
                    record.metadata_json,
                ))
                conn.commit()
                logger.info(f"Added document record: {record.doc_id}")
                return True
        except Exception as e:
            logger.error(f"Failed to add document: {e}")
            return False

    def update_document(
        self,
        doc_id: str,
        status: Optional[str] = None,
        chunk_count: Optional[int] = None,
        error_message: Optional[str] = None,
        indexed_at: Optional[str] = None,
    ) -> bool:
        """Update document status"""
        now = datetime.now().isoformat()
        try:
            with self._get_connection() as conn:
                updates = ["updated_at = ?"]
                params = [now]

                if status:
                    updates.append("status = ?")
                    params.append(status)
                if chunk_count is not None:
                    updates.append("chunk_count = ?")
                    params.append(chunk_count)
                if error_message is not None:
                    updates.append("error_message = ?")
                    params.append(error_message)
                if indexed_at:
                    updates.append("indexed_at = ?")
                    params.append(indexed_at)

                params.append(doc_id)
                conn.execute(
                    f"UPDATE documents SET {', '.join(updates)} WHERE doc_id = ?",
                    params,
                )
                conn.commit()
                return True
        except Exception as e:
            logger.error(f"Failed to update document {doc_id}: {e}")
            return False

    def get_document(self, doc_id: str) -> Optional[DocumentRecord]:
        """Get a document record"""
        try:
            with self._get_connection() as conn:
                row = conn.execute(
                    "SELECT * FROM documents WHERE doc_id = ?", (doc_id,)
                ).fetchone()
                if row:
                    return DocumentRecord(**dict(row))
                return None
        except Exception as e:
            logger.error(f"Failed to get document {doc_id}: {e}")
            return None

    def list_documents(
        self,
        status: Optional[str] = None,
        limit: int = 100,
        offset: int = 0,
    ) -> list[DocumentRecord]:
        """List documents"""
        try:
            with self._get_connection() as conn:
                query = "SELECT * FROM documents"
                params = []
                if status:
                    query += " WHERE status = ?"
                    params.append(status)
                query += " ORDER BY updated_at DESC LIMIT ? OFFSET ?"
                params.extend([limit, offset])

                rows = conn.execute(query, params).fetchall()
                return [DocumentRecord(**dict(row)) for row in rows]
        except Exception as e:
            logger.error(f"Failed to list documents: {e}")
            return []

    def delete_document(self, doc_id: str) -> bool:
        """Delete a document record"""
        try:
            with self._get_connection() as conn:
                conn.execute("DELETE FROM documents WHERE doc_id = ?", (doc_id,))
                conn.commit()
                logger.info(f"Deleted document record: {doc_id}")
                return True
        except Exception as e:
            logger.error(f"Failed to delete document {doc_id}: {e}")
            return False

    def add_indexing_history(
        self,
        doc_id: str,
        action: str,
        chunk_count: int = 0,
        status: str = "completed",
        error_message: Optional[str] = None,
    ) -> bool:
        """Record indexing history"""
        now = datetime.now().isoformat()
        try:
            with self._get_connection() as conn:
                conn.execute("""
                    INSERT INTO indexing_history
                    (doc_id, action, chunk_count, status, error_message, started_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                """, (doc_id, action, chunk_count, status, error_message, now))
                conn.commit()
                return True
        except Exception as e:
            logger.error(f"Failed to add indexing history: {e}")
            return False

    def complete_indexing_history(
        self,
        doc_id: str,
        status: str = "completed",
        error_message: Optional[str] = None,
    ) -> bool:
        """Complete indexing history record"""
        now = datetime.now().isoformat()
        try:
            with self._get_connection() as conn:
                row = conn.execute("""
                    SELECT id, started_at FROM indexing_history
                    WHERE doc_id = ? AND completed_at IS NULL
                    ORDER BY started_at DESC LIMIT 1
                """, (doc_id,)).fetchone()

                if row:
                    started = datetime.fromisoformat(row["started_at"])
                    duration_ms = int((datetime.now() - started).total_seconds() * 1000)
                    conn.execute("""
                        UPDATE indexing_history
                        SET completed_at = ?, status = ?, error_message = ?, duration_ms = ?
                        WHERE id = ?
                    """, (now, status, error_message, duration_ms, row["id"]))
                    conn.commit()
                return True
        except Exception as e:
            logger.error(f"Failed to complete indexing history: {e}")
            return False

    def get_stats(self) -> dict:
        """Get statistics"""
        try:
            with self._get_connection() as conn:
                total = conn.execute("SELECT COUNT(*) as count FROM documents").fetchone()["count"]
                by_status = {}
                for row in conn.execute(
                    "SELECT status, COUNT(*) as count FROM documents GROUP BY status"
                ).fetchall():
                    by_status[row["status"]] = row["count"]

                total_chunks = conn.execute(
                    "SELECT SUM(chunk_count) as total FROM documents"
                ).fetchone()["total"] or 0

                return {
                    "total_documents": total,
                    "by_status": by_status,
                    "total_chunks": total_chunks,
                }
        except Exception as e:
            logger.error(f"Failed to get stats: {e}")
            return {}


_document_store: Optional[DocumentMetadataStore] = None


def get_document_store() -> DocumentMetadataStore:
    """Get global document store instance"""
    global _document_store
    if _document_store is None:
        _document_store = DocumentMetadataStore()
    return _document_store


def reset_document_store() -> None:
    """Reset global document store"""
    global _document_store
    _document_store = None
