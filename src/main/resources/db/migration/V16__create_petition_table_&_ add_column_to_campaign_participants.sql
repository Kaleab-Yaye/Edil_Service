ALTER TABLE campaign_participants ADD column adder_admin_id UUID REFERENCES admin_profiles(id);

CREATE TABLE  petitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    petitioner_id UUID NOT NULL references user_profiles(id) ON DELETE CASCADE,
    resolver_admin_id  UUID NOT NULL references admin_profiles(id)  ON DELETE CASCADE,
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    status VARCHAR(25) NOT NULL DEFAULT 'UNRESOLVED',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP
)

