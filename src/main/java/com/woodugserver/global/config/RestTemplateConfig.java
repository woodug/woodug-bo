package com.woodugserver.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(15_000);

        RestTemplate restTemplate = new RestTemplate(factory);

        // KBO API가 JSON을 text/plain으로 응답하므로 Jackson 컨버터에 추가
        restTemplate.getMessageConverters().stream()
                .filter(c -> c instanceof MappingJackson2HttpMessageConverter)
                .map(c -> (MappingJackson2HttpMessageConverter) c)
                .findFirst()
                .ifPresent(c -> c.setSupportedMediaTypes(
                        List.of(MediaType.APPLICATION_JSON,
                                MediaType.TEXT_PLAIN,
                                new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
                ));

        return restTemplate;
    }
}
