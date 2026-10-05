package com.edil;

import com.edil.config.util.CampaignToSlotNumberAndSlotKeyToCampaignBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class EdilApplicationTests {

    @MockBean
    private CampaignToSlotNumberAndSlotKeyToCampaignBuilder startupBuilder;

    @Test
    void contextLoads() {
    }

}
