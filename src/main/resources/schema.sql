-- tsvector 컬럼 추가 (PostgreSQL 9.6+ IF NOT EXISTS 지원)
ALTER TABLE messages ADD COLUMN IF NOT EXISTS content_tsv tsvector;;

-- GIN 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_messages_content_tsv ON messages USING GIN (content_tsv);;

-- 트리거 함수: INSERT/UPDATE 시 tsvector 자동 갱신
CREATE OR REPLACE FUNCTION messages_content_tsv_trigger() RETURNS trigger AS $$
BEGIN
    NEW.content_tsv := to_tsvector('simple', COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;;

-- 트리거 등록 (DROP IF EXISTS → CREATE로 idempotent)
DROP TRIGGER IF EXISTS trg_messages_content_tsv ON messages;;
CREATE TRIGGER trg_messages_content_tsv
    BEFORE INSERT OR UPDATE OF content ON messages
    FOR EACH ROW
    EXECUTE FUNCTION messages_content_tsv_trigger();;

-- 기존 데이터 백필 (content_tsv가 NULL인 행만)
UPDATE messages SET content_tsv = to_tsvector('simple', COALESCE(content, ''))
WHERE content_tsv IS NULL;;
