package com.edil;

import com.edil.config.util.CampaignToSlotNumberAndSlotKeyToCampaignBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class EdilApplicationTests {

    @MockitoBean
    private CampaignToSlotNumberAndSlotKeyToCampaignBuilder startupBuilder;

    @Test
    void contextLoads() {
    }

}
