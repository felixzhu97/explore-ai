"""
Session persistence store for chat history and session configuration.
Uses SQLite for storage with support for session search and export.
"""
import sqlite3
import json
from typing import Optional, Any
from dataclasses import dataclass, asdict
from datetime import datetime
from pathlib import Path
from loguru import logger
from ..config import get_settings


@dataclass
class ChatMessage:
    """Chat message"""
    role: str  # user, assistant
    content: str
    timestamp: str
    sources: Optional[str] = None  # JSON serialized sources


@dataclass
class ChatSession:
    """Chat session"""
    session_id: str
    title: str = "New Chat"
    created_at: Optional[str] = None
    updated_at: Optional[str] = None
    message_count: int = 0
    metadata_json: Optional[str] = None


class SessionStore:
    """
    Session persistence store.

    Uses SQLite for storing:
    - Session basic information
    - Chat message history
    - Session configuration and metadata
    """

    def __init__(self, db_path: Optional[str] = None):
        settings = get_settings()
        self.db_path = db_path or settings.SESSION_DB_PATH
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
                CREATE TABLE IF NOT EXISTS sessions (
                    session_id TEXT PRIMARY KEY,
                    title TEXT NOT NULL DEFAULT 'New Chat',
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    message_count INTEGER DEFAULT 0,
                    metadata_json TEXT
                )
            """)

            conn.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id TEXT NOT NULL,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    sources TEXT,
                    FOREIGN KEY (session_id) REFERENCES sessions(session_id) ON DELETE CASCADE
                )
            """)

            conn.execute("""
                CREATE INDEX IF NOT EXISTS idx_messages_session_id
                ON messages(session_id)
            """)
            conn.execute("""
                CREATE INDEX IF NOT EXISTS idx_messages_timestamp
                ON messages(timestamp)
            """)

            conn.commit()
            logger.info(f"Session database initialized at {self.db_path}")

    def create_session(self, session_id: str, title: str = "New Chat") -> bool:
        """Create a new session"""
        now = datetime.now().isoformat()
        try:
            with self._get_connection() as conn:
                conn.execute("""
                    INSERT INTO sessions (session_id, title, created_at, updated_at, message_count)
                    VALUES (?, ?, ?, ?, 0)
                """, (session_id, title, now, now))
                conn.commit()
                logger.info(f"Created session: {session_id}")
                return True
        except Exception as e:
            logger.error(f"Failed to create session: {e}")
            return False

    def get_session(self, session_id: str) -> Optional[ChatSession]:
        """Get a session by ID"""
        try:
            with self._get_connection() as conn:
                row = conn.execute(
                    "SELECT * FROM sessions WHERE session_id = ?", (session_id,)
                ).fetchone()
                if row:
                    return ChatSession(**dict(row))
                return None
        except Exception as e:
            logger.error(f"Failed to get session: {e}")
            return None

    def update_session(
        self,
        session_id: str,
        title: Optional[str] = None,
        message_count: Optional[int] = None,
    ) -> bool:
        """Update a session"""
        now = datetime.now().isoformat()
        try:
            with self._get_connection() as conn:
                updates = ["updated_at = ?"]
                params = [now]

                if title:
                    updates.append("title = ?")
                    params.append(title)
                if message_count is not None:
                    updates.append("message_count = ?")
                    params.append(message_count)

                params.append(session_id)
                conn.execute(
                    f"UPDATE sessions SET {', '.join(updates)} WHERE session_id = ?",
                    params,
                )
                conn.commit()
                return True
        except Exception as e:
            logger.error(f"Failed to update session: {e}")
            return False

    def delete_session(self, session_id: str) -> bool:
        """Delete a session and its messages"""
        try:
            with self._get_connection() as conn:
                conn.execute("DELETE FROM messages WHERE session_id = ?", (session_id,))
                conn.execute("DELETE FROM sessions WHERE session_id = ?", (session_id,))
                conn.commit()
                logger.info(f"Deleted session: {session_id}")
                return True
        except Exception as e:
            logger.error(f"Failed to delete session: {e}")
            return False

    def list_sessions(self, limit: int = 50, offset: int = 0) -> list[ChatSession]:
        """List recent sessions"""
        try:
            with self._get_connection() as conn:
                rows = conn.execute("""
                    SELECT * FROM sessions
                    ORDER BY updated_at DESC
                    LIMIT ? OFFSET ?
                """, (limit, offset)).fetchall()
                return [ChatSession(**dict(row)) for row in rows]
        except Exception as e:
            logger.error(f"Failed to list sessions: {e}")
            return []

    def add_message(
        self,
        session_id: str,
        role: str,
        content: str,
        sources: Optional[list] = None,
    ) -> Optional[int]:
        """Add a message to a session"""
        now = datetime.now().isoformat()
        try:
            with self._get_connection() as conn:
                cursor = conn.execute("""
                    INSERT INTO messages (session_id, role, content, timestamp, sources)
                    VALUES (?, ?, ?, ?, ?)
                """, (
                    session_id,
                    role,
                    content,
                    now,
                    json.dumps(sources) if sources else None,
                ))

                conn.execute("""
                    UPDATE sessions
                    SET updated_at = ?, message_count = message_count + 1
                    WHERE session_id = ?
                """, (now, session_id))

                conn.commit()
                return cursor.lastrowid
        except Exception as e:
            logger.error(f"Failed to add message: {e}")
            return None

    def get_messages(
        self,
        session_id: str,
        limit: int = 100,
        offset: int = 0,
    ) -> list[ChatMessage]:
        """Get messages for a session"""
        try:
            with self._get_connection() as conn:
                rows = conn.execute("""
                    SELECT * FROM messages
                    WHERE session_id = ?
                    ORDER BY timestamp ASC
                    LIMIT ? OFFSET ?
                """, (session_id, limit, offset)).fetchall()
                return [ChatMessage(**dict(row)) for row in rows]
        except Exception as e:
            logger.error(f"Failed to get messages: {e}")
            return []

    def search_sessions(self, keyword: str) -> list[ChatSession]:
        """Search sessions by title and message content"""
        try:
            with self._get_connection() as conn:
                rows = conn.execute("""
                    SELECT DISTINCT s.* FROM sessions s
                    LEFT JOIN messages m ON s.session_id = m.session_id
                    WHERE s.title LIKE ? OR m.content LIKE ?
                    ORDER BY s.updated_at DESC
                    LIMIT 50
                """, (f"%{keyword}%", f"%{keyword}%")).fetchall()
                return [ChatSession(**dict(row)) for row in rows]
        except Exception as e:
            logger.error(f"Failed to search sessions: {e}")
            return []

    def export_session(self, session_id: str) -> Optional[dict]:
        """Export session as JSON"""
        session = self.get_session(session_id)
        if not session:
            return None

        messages = self.get_messages(session_id)
        return {
            "session": asdict(session),
            "messages": [asdict(m) for m in messages],
            "exported_at": datetime.now().isoformat(),
        }

    def clear_old_sessions(self, days: int = 30) -> int:
        """Clear sessions older than specified days"""
        cutoff = datetime.now() - datetime.timedelta(days=days)
        try:
            with self._get_connection() as conn:
                cursor = conn.execute("""
                    DELETE FROM sessions WHERE updated_at < ?
                """, (cutoff.isoformat(),))
                conn.commit()
                count = cursor.rowcount
                logger.info(f"Cleared {count} old sessions")
                return count
        except Exception as e:
            logger.error(f"Failed to clear old sessions: {e}")
            return 0

    def get_stats(self) -> dict:
        """Get statistics"""
        try:
            with self._get_connection() as conn:
                total_sessions = conn.execute(
                    "SELECT COUNT(*) FROM sessions"
                ).fetchone()[0]
                total_messages = conn.execute(
                    "SELECT COUNT(*) FROM messages"
                ).fetchone()[0]
                return {
                    "total_sessions": total_sessions,
                    "total_messages": total_messages,
                }
        except Exception as e:
            logger.error(f"Failed to get stats: {e}")
            return {}


_session_store: Optional[SessionStore] = None


def get_session_store() -> SessionStore:
    """Get global session store instance"""
    global _session_store
    if _session_store is None:
        _session_store = SessionStore()
    return _session_store


def reset_session_store() -> None:
    """Reset global session store"""
    global _session_store
    _session_store = None
