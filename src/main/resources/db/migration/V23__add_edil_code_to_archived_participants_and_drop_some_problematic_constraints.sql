ALTER TABLE archived_Campaign_participant ADD column edil_code VARCHAR(255);
ALTER TABLE archived_Campaign_participant ADD CONSTRAINT  edil_code_and_campaign_id_unique_constraint UNIQUE (edil_code, campaign_id);
ALTER TABLE campaign_participants DROP CONSTRAINT  campaign_participants_edil_code_key;
ALTER TABLE campaign_participant ADD CONSTRAINT campaign_participants_edil_code_key_and_account_id_unique UNIQUE (edil_code, campaign_id);
