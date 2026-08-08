package com.zuzdog.model;

import java.time.Instant;

// mirrors one row of the hangout_participants junction table.
// (hangout_id, user_id) is the natural primary key — a user can only sign up once per hangout.
public class HangoutParticipant {

    private long hangoutId;
    private long userId;
    private Instant signedUpAt;

    public long getHangoutId() { return hangoutId; }
    public void setHangoutId(long hangoutId) { this.hangoutId = hangoutId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public Instant getSignedUpAt() { return signedUpAt; }
    public void setSignedUpAt(Instant signedUpAt) { this.signedUpAt = signedUpAt; }
}