package com.zuzdog.dto;

//functions to take care and combine dog for a user 
// the dto will help the back and the front to talk with each other by objects 
public class FeedCandidate {

    private long userId;
    private String username;
    private String description;
    private String photoUrl;
    private double distanceKm;

    private Long dogId;
    private String dogName;
    private String breed;
    private Integer dogAge;
    private String dogPhotoUrl;

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public Long getDogId() { return dogId; }
    public void setDogId(Long dogId) { this.dogId = dogId; }

    public String getDogName() { return dogName; }
    public void setDogName(String dogName) { this.dogName = dogName; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public Integer getDogAge() { return dogAge; }
    public void setDogAge(Integer dogAge) { this.dogAge = dogAge; }

    public String getDogPhotoUrl() { return dogPhotoUrl; }
    public void setDogPhotoUrl(String dogPhotoUrl) { this.dogPhotoUrl = dogPhotoUrl; }
}
