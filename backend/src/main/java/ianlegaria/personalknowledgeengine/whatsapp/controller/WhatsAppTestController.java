package ianlegaria.personalknowledgeengine.whatsapp.controller;

import ianlegaria.personalknowledgeengine.whatsapp.service.FlashcardReminderScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
@Profile("!prod")
public class WhatsAppTestController {

    private final FlashcardReminderScheduler scheduler;

    @PostMapping("/test-reminder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void triggerReminder() {
        scheduler.sendDueFlashcards();
    }
}
