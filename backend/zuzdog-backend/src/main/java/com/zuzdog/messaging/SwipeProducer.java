package com.zuzdog.messaging;

import com.zuzdog.config.JmsConfig;
import com.zuzdog.model.SwipeAction;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SwipeProducer {

    private final JmsTemplate jmsTemplate;

    //constructor build a object wthat will get jmsconfig to connect jms 
    public SwipeProducer(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

   //create swipemessage object  then convert it and send the queue 
    public void publishUpSwipe(long senderId, long targetId) {
        SwipeMessage message = new SwipeMessage(senderId, targetId, SwipeAction.UP, Instant.now());
        jmsTemplate.convertAndSend(JmsConfig.SWIPE_QUEUE, message);
    }
}