package com.edil.dto.request;

import com.beust.jcommander.UnixStyleUsageFormatter;

import java.util.UUID;

public record EndCampaignByCreatorRequest(
        UUID campaignId
        ){

}
