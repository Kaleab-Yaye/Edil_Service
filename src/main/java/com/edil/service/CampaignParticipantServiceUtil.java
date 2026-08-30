package com.edil.service;


import com.edil.dto.internal.CbePayload;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class CampaignParticipantServiceUtil {

    private final RestClient restClient;

    CampaignParticipantServiceUtil(RestClient restClient){
        this.restClient = restClient;
    }


    public CbePayload fetchCbePayload(String rawUri) {

        //Stat-request header, that will be subject to a lot of change possibley
        final String x_app_id = "d1292e42-7400-49de-a2d3-9731caa4c819";
        final String x_app_version = "0a01980b-9859-1369-8198-59f403820000";
        final String user_agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36 Edg/152.0.0.0";
        final String Origin = "https://mbreciept.cbe.com.et";
        final String Referer = "https://mbreciept.cbe.com.et/";
        //End

        // the real end point :: "https://mb.cbe.com.et/api/v1/transactions/public/transaction-detail/v2-";
        String cbeJasonPayloadEndPoint = "http://localhost:3005/api/v1/transactions/public/transaction-detail/v2-";

        String patternTobeMatched = "^https://mbreciept\\.cbe\\.com\\.et/v2-([a-zA-Z0-9]+)$";
        Pattern pattern = Pattern.compile(patternTobeMatched);
        Matcher matcher = pattern.matcher(rawUri);

        if (!matcher.matches()) {
            return null;
        }

        String uniqueIdOnLink = matcher.group(1); //

        String requestTobeMadeLink = cbeJasonPayloadEndPoint + uniqueIdOnLink;


        return restClient.get()
                .uri(requestTobeMadeLink)
                .accept(MediaType.APPLICATION_JSON)// Tells server we want JSON
                .header("x-app-version", x_app_version)
                .header("x-app-id", x_app_id)
                .header("user-agent", user_agent
                )
                .header("Origin", Origin)
                .header("Referer", Referer)
                .retrieve()
                .body(CbePayload.class);

    }



}

