package com.edil.config.util;


import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class StoreCampaignToSlotHashMap {
    public static ConcurrentHashMap<UUID, AtomicInteger> campaignToSlotStore = new ConcurrentHashMap<>();
}
