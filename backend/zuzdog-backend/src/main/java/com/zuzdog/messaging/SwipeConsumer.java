package com.zuzdog.messaging;

import com.zuzdog.config.JmsConfig;
import com.zuzdog.dao.MatchDao;
import com.zuzdog.dao.SwipeDao;
import com.zuzdog.model.SwipeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SwipeConsumer {

    //log for debug 
    private static final Logger log = LoggerFactory.getLogger(SwipeConsumer.class);

    private final SwipeDao swipeDao;
    private final MatchDao matchDao;

    //constructor build a swipe consumer obj 
    public SwipeConsumer(SwipeDao swipeDao, MatchDao matchDao) {
        this.swipeDao = swipeDao;
        this.matchDao = matchDao;
    }


    // set this function to listen to swipe queue the function gets message and create obj
    @JmsListener(destination = JmsConfig.SWIPE_QUEUE, containerFactory = "jmsListenerContainerFactory")
    @Transactional
    public void onSwipeMessage(SwipeMessage message) {
        long senderId = message.getSenderId();
        long targetId = message.getTargetId();

        //if not up we just avoid 
        if (message.getAction() != SwipeAction.UP) {
            log.warn("SwipeConsumer received a non-UP message, ignoring: {}", message.getAction());
            return;
        }

        // check if the the other user already give us up to match 
        boolean ismatch = swipeDao.existsUpSwipe(targetId, senderId);

        //insert swap to db 
        swipeDao.insert(senderId, targetId, SwipeAction.UP);

        if (ismatch) {
            matchDao.insertMatch(senderId, targetId);
            log.info("Match created between {} and {}", senderId, targetId);
            
            
            // need to add after davies will finish the notify match function !!!!!
        }
    }
}