package com.zuzdog.service;

import com.zuzdog.dao.MatchDao;
import com.zuzdog.dao.SwipeDao;
import com.zuzdog.messaging.SwipeProducer;
import com.zuzdog.model.SwipeAction;
import org.springframework.stereotype.Service;

//  logic  for swipe operations.
// Coordinates DAOs and async messaging to implement swipe/match logic.
// UP swipes go async via JMS queue; DOWN swipes are persisted immediately.
@Service
public class SwipeService {

    private final SwipeDao swipeDao;
    private final MatchDao matchDao;
    private final SwipeProducer swipeProducer;

    public SwipeService(SwipeDao swipeDao, MatchDao matchDao, SwipeProducer swipeProducer) {
        this.swipeDao = swipeDao;
        this.matchDao = matchDao;
        this.swipeProducer = swipeProducer;
    }

    // Process a swipe action (UP or DOWN) from userId to targetId.
    public void processSwipe(long userId, long targetId, SwipeAction action) {
        if (action == SwipeAction.UP) {
            swipeProducer.publishUpSwipe(userId, targetId);
        } else {
            swipeDao.insert(userId, targetId, action);
        }
    }

    // Check if user already swiped (UP or DOWN) on target.
    public boolean hasUserSwiped(long userId, long targetId) {
        return swipeDao.hasViewed(userId, targetId);
    }

    // Check if a match exists between two users.
    public boolean isMatched(long userId, long otherId) {
        return matchDao.existsBetween(userId, otherId);
    }
}
