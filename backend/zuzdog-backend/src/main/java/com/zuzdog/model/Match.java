package com.zuzdog.model;

import java.time.Instant;

// mirrors one row of the matches table. The DB enforces user1_id < user2_id via a CHECK
// constraint, so canonicalPair() must be used any time we insert or look up a match —
// otherwise inserting (5,3) instead of (3,5) will violate the constraint.
public class Match {

    private long user1Id;
    private long user2Id;
    private Instant matchDate;

    // Always call this before writing to or reading from the matches table.
    public static long[] pairInOrder(long userA, long userB) {
        if (userA == userB) {
            throw new IllegalArgumentException("A user cannot match with themselves");
        }
        return userA < userB ? new long[]{userA, userB} : new long[]{userB, userA};
    }

    public long getUser1Id() { return user1Id; }
    public void setUser1Id(long user1Id) { this.user1Id = user1Id; }

    public long getUser2Id() { return user2Id; }
    public void setUser2Id(long user2Id) { this.user2Id = user2Id; }

    public Instant getMatchDate() { return matchDate; }
    public void setMatchDate(Instant matchDate) { this.matchDate = matchDate; }
}
