package com.zuzdog.model;

import java.time.Instant;

// mirrors one row of the hangouts table plus two computed fields the API returns:
//   participantCount  -> how many users signed up (COUNT over hangout_participants)
//   isUserSignedUp   -> whether the requesting user is among the participants
// both computed fields are filled in by HangoutDao, not stored on the row.
public class Hangout {

    private long hangoutId;
    private long organizerUserId;
    private String title;
    private String description;
    private String organizerName;   
    private double latitude;
    private double longitude;
    private Instant eventTime;      // nullable, an "always-open spot" has no event time
    private HangoutActivityType activityType;
    private Instant createdAt;
    private Integer participantCount; // computed by the DAO (LEFT JOIN + COUNT)
    private boolean isUserSignedUp;   // computed by the DAO for the requesting user

    public long getHangoutId() { return hangoutId; }
    public void setHangoutId(long hangoutId) { this.hangoutId = hangoutId; }

    public long getOrganizerUserId() { return organizerUserId; }
    public void setOrganizerUserId(long organizerUserId) { this.organizerUserId = organizerUserId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }

    public HangoutActivityType getActivityType() { return activityType; }
    public void setActivityType(HangoutActivityType activityType) { this.activityType = activityType; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Integer getParticipantCount() { return participantCount; }
    public void setParticipantCount(Integer participantCount) { this.participantCount = participantCount; }

    public boolean isUserSignedUp() { return isUserSignedUp; }
    public void setUserSignedUp(boolean isUserSignedUp) { this.isUserSignedUp = isUserSignedUp; }
}