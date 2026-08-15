package com.zuzdog.service;

import com.zuzdog.dao.MatchDao;
import com.zuzdog.dao.UserDao;
import com.zuzdog.dto.MatchSummaryResponse;
import com.zuzdog.model.Match;
import com.zuzdog.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

//  logic  for match retrieval.

@Service
public class MatchService {

    private final MatchDao matchDao;
    private final UserDao userDao;

    //mame constructor for MatchService 
    public MatchService(MatchDao matchDao, UserDao userDao) {
        this.matchDao = matchDao;
        this.userDao = userDao;
    }

    // All matches for a user, newest first 
    public List<Match> getMatchesForUser(long userId) {
        return matchDao.findAllForUser(userId);
    }

   // 
    public List<MatchSummaryResponse> getMatchSummariesForUser(long userId) {
        List<Match> matches = getMatchesForUser(userId); // get all matches from db 
        List<MatchSummaryResponse> result = new ArrayList<>();
        for (Match match : matches) {
            result.add(toSummary(match, userId));
        }
        return result;
    }

    // Check if two users are is matched
    public boolean isMatched(long userA, long userB) {
        return matchDao.existsBetween(userA, userB);
    }

    // get a match and return a photo and deatiles of the other user
    private MatchSummaryResponse toSummary(Match match, long viewerId) {
        long otherUserId;
        if (match.getUser1Id() == viewerId) {
            otherUserId = match.getUser2Id();
        } else {
            otherUserId = match.getUser1Id();
        }
        User other = userDao.findById(otherUserId).orElse(null);
        String username = other != null ? other.getUsername() : "Unknown user";
        String photoUrl = other != null ? other.getPhotoUrl() : null;
        return new MatchSummaryResponse(otherUserId, username, photoUrl, match.getMatchDate());
    }
}
