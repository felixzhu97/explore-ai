-- This migration is only for existing databases that were created with the old 1536 dimension
-- For new databases, V1 already creates the correct 1024 dimension

DO $$
BEGIN
    -- Check if the column exists and has wrong dimension
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'document_chunks' AND column_name = 'embedding'
    ) THEN
        -- Check current dimension by trying to cast
        BEGIN
            ALTER TABLE document_chunks DROP COLUMN IF EXISTS embedding;
            ALTER TABLE document_chunks ADD COLUMN embedding vector(1024);
            
            DROP INDEX IF EXISTS idx_document_chunks_embedding;
            CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding ON document_chunks USING ivfflat (embedding vector_cosine_ops);
        END;
    END IF;
END $$;
