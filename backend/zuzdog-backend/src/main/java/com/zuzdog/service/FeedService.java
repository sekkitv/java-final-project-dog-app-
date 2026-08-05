package com.zuzdog.service;

import com.zuzdog.dao.FeedDao;
import com.zuzdog.dao.UserDao;
import com.zuzdog.dto.FeedCandidate;
import com.zuzdog.exception.ApiException;
import com.zuzdog.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

//  logic  for the discovery feed.
// Loads the viewer's own location/preferences from UserDao, then delegates
// the actual Haversine distance query to FeedDao.
@Service
public class FeedService {

    private static final int DEFAULT_LIMIT = 10;

    private final FeedDao feedDao;
    private final UserDao userDao;

    public FeedService(FeedDao feedDao, UserDao userDao) {
        this.feedDao = feedDao;
        this.userDao = userDao;
    }

    // Build a feed for the viewer using their own lat/lng/maxDistance from the users table.
    // Throws if the viewer has no location set (lat/lng are null) since distance can't be computed.
    // take the user from db check is location and takes from feed dao the list of possible 
    public List<FeedCandidate> getFeedForUser(long viewerId, int limit) {
        //
        User viewer = userDao.findById(viewerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (viewer.getLat() == null || viewer.getLng() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Set your location (lat/lng) before loading the feed");
        }
        // use limit to limit the number of results returned from the feed dao
        return feedDao.findFeed(viewerId, viewer.getLat(), viewer.getLng(),
                viewer.getMaxDistance(), limit); 
    }

    //  using a default result limit and call the overload function .
    public List<FeedCandidate> getFeedForUser(long viewerId) {
        return getFeedForUser(viewerId, DEFAULT_LIMIT);
    }
}
