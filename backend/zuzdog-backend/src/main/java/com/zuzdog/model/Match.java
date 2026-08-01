package com.zuzdog.model;

import java.time.Instant;

//this file give us needed function for match logic mainly use in dao match put users in order and getter and setters

public class Match {

    private long user1Id;
    private long user2Id;
    private Instant matchDate;

    // Always call this before writing to or reading from the matches table.
    public static long[] pairInOrder(long userA, long userB) {
        if (userA == userB) {
            throw new IllegalArgumentException("A user cannot match with themselves");
        }

        if (userA < userB) {
            return new long[]{userA, userB};
        } else {
            return new long[]{userB, userA};
        }
    }

    public long getUser1Id() { return user1Id; }
    public void setUser1Id(long user1Id) { this.user1Id = user1Id; }

    public long getUser2Id() { return user2Id; }
    public void setUser2Id(long user2Id) { this.user2Id = user2Id; }

    public Instant getMatchDate() { return matchDate; }
    public void setMatchDate(Instant matchDate) { this.matchDate = matchDate; }
}
