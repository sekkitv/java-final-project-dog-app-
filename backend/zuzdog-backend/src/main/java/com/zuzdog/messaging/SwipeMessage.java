package com.zuzdog.messaging;

import com.zuzdog.model.SwipeAction;

import java.io.Serializable;
import java.time.Instant;

// This is the json payload for SWIPE_QUEUE.
//serializable  pack the object so it will fit the tcp transfter
public class SwipeMessage implements Serializable {

    private long senderId;
    private long targetId;
    private SwipeAction action;
    private Instant enqueuedAt; //when the action entered to the queue added for debug mainly 

    //empty constructor 
    public SwipeMessage() {}

    //constructor for swipemessage keep the id of each side action and the time to qeue 
    public SwipeMessage(long senderId, long targetId, SwipeAction action, Instant enqueuedAt) {
        this.senderId = senderId;
        this.targetId = targetId;
        this.action = action;
        this.enqueuedAt = enqueuedAt;
    }

    // getters and setters for the object 

    public long getSenderId() { return senderId; }
    public void setSenderId(long senderId) { this.senderId = senderId; }

    public long getTargetId() { return targetId; }
    public void setTargetId(long targetId) { this.targetId = targetId; }

    public SwipeAction getAction() { return action; }
    public void setAction(SwipeAction action) { this.action = action; }

    public Instant getEnqueuedAt() { return enqueuedAt; }
    public void setEnqueuedAt(Instant enqueuedAt) { this.enqueuedAt = enqueuedAt; }
}