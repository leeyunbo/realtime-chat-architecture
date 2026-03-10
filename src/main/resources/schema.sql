-- Drop PostgreSQL FTS artifacts (migrated to Elasticsearch)
DROP TRIGGER IF EXISTS trg_messages_content_tsv ON messages;;
DROP FUNCTION IF EXISTS messages_content_tsv_trigger();;
DROP INDEX IF EXISTS idx_messages_content_tsv;;
ALTER TABLE messages DROP COLUMN IF EXISTS content_tsv;;
