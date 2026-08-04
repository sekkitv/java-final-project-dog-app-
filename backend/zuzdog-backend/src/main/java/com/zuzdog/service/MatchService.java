package com.zuzdog.service;

import com.zuzdog.dao.MatchDao;
import com.zuzdog.model.Match;
import org.springframework.stereotype.Service;

import java.util.List;

// Business logic layer for match retrieval.
// Thin wrapper around MatchDao - exists so MatchController never talks to the DAO directly.
@Service
public class MatchService {

    private final MatchDao matchDao;

    public MatchService(MatchDao matchDao) {
        this.matchDao = matchDao;
    }

    // All matches for a user, newest first.
    public List<Match> getMatchesForUser(long userId) {
        return matchDao.findAllForUser(userId);
    }

    // Check if two users are matched.
    public boolean isMatched(long userA, long userB) {
        return matchDao.existsBetween(userA, userB);
    }
}
