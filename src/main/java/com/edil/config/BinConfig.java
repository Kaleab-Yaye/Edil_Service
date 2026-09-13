package com.edil.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

import java.util.concurrent.Executor;

@Configuration
public class BinConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

//    @Bean
//    public ObjectMapper retObjectMapper() {
//        return new ObjectMapper();
//    }


    @Bean(name ="endCampaignAndGeneratePdf")

    public Executor getEndCampaignAndGeneratePDFExecutor(){
      ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
      threadPoolTaskExecutor.setCorePoolSize(2);
      threadPoolTaskExecutor.setQueueCapacity(100);
      threadPoolTaskExecutor.setMaxPoolSize(2);
      threadPoolTaskExecutor.setThreadNamePrefix("EndCampaignGenPdf--");
      threadPoolTaskExecutor.initialize();

      return threadPoolTaskExecutor;

    }



}
