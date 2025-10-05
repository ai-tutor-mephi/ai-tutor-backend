package com.VLmb.ai_tutor_backend.client.config;

import com.VLmb.ai_tutor_backend.client.RagRestClient;
import com.VLmb.ai_tutor_backend.client.RagRestClientImpl;
import com.VLmb.ai_tutor_backend.exception.UpstreamClientException;
import com.VLmb.ai_tutor_backend.exception.UpstreamServerException;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagRestClientConfig {

    @Bean
    RestClient ragRestClient(RagProperties ragProperties) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(ragProperties.connectTimeout()))
                .setResponseTimeout(Timeout.of(ragProperties.responseTimeout()))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        var auth = new RagAuthInterceptor(ragProperties);
        var logging = new LoggingInterceptor(ragProperties.logPayloads());

        return RestClient.builder()
                .baseUrl(ragProperties.baseUrl())
                .requestFactory(factory)
                .defaultHeaders(headers -> headers.setAccept(List.of(MediaType.APPLICATION_JSON)))
                .requestInterceptor(logging)
                .requestInterceptor(auth)
                .defaultStatusHandler(
                        HttpStatusCode::is4xxClientError,
                        (request, response) -> {
                            String body = safeBody(response);
                            throw new UpstreamClientException(response.getStatusCode(), body);
                        })
                .defaultStatusHandler(
                        HttpStatusCode::is5xxServerError,
                        (request, response) -> {
                            String body = safeBody(response);
                            throw new UpstreamServerException(response.getStatusCode(), body);
                        })
                .build();
    }

    @Bean
    RagRestClient ragClientApi(RestClient ragRestClient) {
        return new RagRestClientImpl(ragRestClient);
    }

    private static String safeBody(org.springframework.http.client.ClientHttpResponse response) {
        try {
            return StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "<unreadable>";
        }
    }

}
