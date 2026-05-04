package ianlegaria.personalknowledgeengine.common.outbox;

import org.springframework.context.ApplicationEvent;

public class OutboxEventSavedEvent extends ApplicationEvent {
    public OutboxEventSavedEvent(Object source) {
        super(source);
    }
}
