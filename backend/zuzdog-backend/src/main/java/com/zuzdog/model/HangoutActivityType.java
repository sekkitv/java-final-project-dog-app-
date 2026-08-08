package com.zuzdog.model;

// The kind of hangout event shown on the map.
// stored in the DB as a VARCHAR ('MEETUP' / 'DOG_FRIENDLY_BUSINESS' / 'WATER_SPOT' / 'POOP_BAGS_SPOT')
// via a CHECK constraint on the hangouts table. Read/write is done with
// HangoutActivityType.valueOf(...) and .name(), matching how SwipeAction is handled.
public enum HangoutActivityType {
    MEETUP,
    DOG_FRIENDLY_BUSINESS,
    WATER_SPOT,
    POOP_BAGS_SPOT
}