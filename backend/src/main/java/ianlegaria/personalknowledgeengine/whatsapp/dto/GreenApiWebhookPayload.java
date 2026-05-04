package ianlegaria.personalknowledgeengine.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GreenApiWebhookPayload {

    private String typeWebhook;
    private Long timestamp;
    private String idMessage;
    private SenderData senderData;
    private MessageData messageData;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SenderData {
        private String chatId;
        private String senderName;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageData {
        private String typeMessage;
        private TextMessageData textMessageData;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextMessageData {
        private String textMessage;
    }
}
