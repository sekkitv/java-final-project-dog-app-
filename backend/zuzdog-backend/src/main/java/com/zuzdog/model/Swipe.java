package com.zuzdog.model;

import java.time.Instant;

// mirrors one row of the swipes table: (sender_id, target_id) is the primary key.
public class Swipe {

    private long senderId;
    private long targetId;
    private SwipeAction action;
    private Instant swipedAt;

    public long getSenderId() { return senderId; }
    public void setSenderId(long senderId) { this.senderId = senderId; }

    public long getTargetId() { return targetId; }
    public void setTargetId(long targetId) { this.targetId = targetId; }

    public SwipeAction getAction() { return action; }
    public void setAction(SwipeAction action) { this.action = action; }

    public Instant getSwipedAt() { return swipedAt; }
    public void setSwipedAt(Instant swipedAt) { this.swipedAt = swipedAt; }
}
