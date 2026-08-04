package com.zuzdog.dto;

import java.util.List;

// Wraps the feed candidates list so the JSON body is {"candidates": [...]}
// instead of a bare array - matches the documented API contract.
public class FeedResponse {

    private List<FeedCandidate> candidates;

    public FeedResponse(List<FeedCandidate> candidates) {
        this.candidates = candidates;
    }

    public List<FeedCandidate> getCandidates() { return candidates; }
    public void setCandidates(List<FeedCandidate> candidates) { this.candidates = candidates; }
}
