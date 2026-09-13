create table campaign_participants_pdf (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       campaign_id UUID REFERENCES campaigns(id) ON DELETE CASCADE  NOT NULL UNIQUE,
       pdf_name VARCHAR(255) NOT NULL,
       pdf_size_in_bytes BIGINT NOT NULL,
       generated_at TIMESTAMP NOT NULL DEFAULT NOW()

)