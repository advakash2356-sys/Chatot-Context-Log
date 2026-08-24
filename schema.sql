-- ContextLog Database Schema Migration Script
-- Enable pgvector extension for AI embeddings
CREATE EXTENSION IF NOT EXISTS vector;

-- Enable pgcrypto for UUID generation
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. MATTERS TABLE
CREATE TABLE IF NOT EXISTS matters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    client_name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for matter lookups by code
CREATE INDEX IF NOT EXISTS idx_matters_code ON matters(code);

-- 2. CONTEXT_NOTES TABLE
CREATE TABLE IF NOT EXISTS context_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    matter_id UUID REFERENCES matters(id) ON DELETE SET NULL,
    raw_transcript TEXT NOT NULL,
    clean_text TEXT NOT NULL,
    entry_type TEXT NOT NULL CHECK (entry_type IN ('LOG', 'REMINDER', 'LEGAL_MATTER', 'DECISION_PAUSE', 'RAG_QUESTION')),
    depth_level INT NOT NULL DEFAULT 1 CHECK (depth_level BETWEEN 1 AND 5),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    two_hour_block_start TIMESTAMPTZ NOT NULL,
    scheduled_datetime TIMESTAMPTZ,
    google_event_id TEXT,
    fts_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('english', coalesce(clean_text, '') || ' ' || coalesce(raw_transcript, ''))
    ) STORED
);

-- Indices for performance and filtering
CREATE INDEX IF NOT EXISTS idx_context_notes_matter ON context_notes(matter_id);
CREATE INDEX IF NOT EXISTS idx_context_notes_recorded ON context_notes(recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_context_notes_block ON context_notes(two_hour_block_start DESC);
CREATE INDEX IF NOT EXISTS idx_context_notes_fts ON context_notes USING GIN(fts_vector);

-- Trigger Function: Automatic 2-hour window block calculation
-- Formula: floor(epoch / 7200) * 7200
CREATE OR REPLACE FUNCTION set_two_hour_block()
RETURNS TRIGGER AS $$
BEGIN
    NEW.two_hour_block_start := to_timestamp(floor(extract(epoch from NEW.recorded_at) / 7200) * 7200);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger execution on INSERT or UPDATE of recorded_at
DROP TRIGGER IF EXISTS trigger_set_two_hour_block ON context_notes;
CREATE TRIGGER trigger_set_two_hour_block
BEFORE INSERT OR UPDATE OF recorded_at ON context_notes
FOR EACH ROW
EXECUTE FUNCTION set_two_hour_block();

-- 3. DOCUMENTS TABLE
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    matter_id UUID REFERENCES matters(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. DOCUMENT_CHUNKS TABLE with Gemini Embedding Vector (768 dimensions for text-embedding-004)
CREATE TABLE IF NOT EXISTS document_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    page_number INT DEFAULT 1,
    embedding vector(768),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Vector Index for fast cosine similarity search
CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding 
ON document_chunks USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- RPC Function: Vector Similarity Search for RAG Questions
CREATE OR REPLACE FUNCTION match_document_chunks(
    query_embedding vector(768),
    match_threshold FLOAT DEFAULT 0.5,
    match_count INT DEFAULT 5
)
RETURNS TABLE (
    id UUID,
    document_id UUID,
    title TEXT,
    content TEXT,
    page_number INT,
    similarity FLOAT
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        dc.id,
        dc.document_id,
        dc.title,
        dc.content,
        dc.page_number,
        1 - (dc.embedding <=> query_embedding) AS similarity
    FROM document_chunks dc
    WHERE 1 - (dc.embedding <=> query_embedding) > match_threshold
    ORDER BY dc.embedding <=> query_embedding
    LIMIT match_count;
END;
$$;
