package ianlegaria.personalknowledgeengine.whatsapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
public class WhatsAppServiceImpl implements WhatsAppService {

    private final WebClient webClient;
    private final String instanceId;
    private final String token;

    public WhatsAppServiceImpl(
            WebClient.Builder builder,
            @Value("${green-api.base-url:https://api.green-api.com}") String baseUrl,
            @Value("${green-api.instance-id:}") String instanceId,
            @Value("${green-api.token:}") String token) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.instanceId = instanceId;
        this.token = token;
    }

    /**
     * @param chatId Green API chatId format: "521234567890@c.us"
     */
    public void sendMessage(String chatId, String text) {
        String path = "/waInstance" + instanceId + "/sendMessage/" + token;
        try {
            webClient.post()
                    .uri(path)
                    .bodyValue(Map.of("chatId", chatId, "message", text))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("WhatsApp message sent to {}", chatId);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to {}: {}", chatId, e.getMessage());
        }
    }
}
