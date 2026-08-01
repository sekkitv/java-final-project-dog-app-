package com.zuzdog.model;

// UP = the user liked the profile, DOWN = the user passed on it.
// stored in the DB as a VARCHAR ('UP' / 'DOWN') via CHECK constraint on the swipes table.
public enum SwipeAction {
    UP,
    DOWN
}
