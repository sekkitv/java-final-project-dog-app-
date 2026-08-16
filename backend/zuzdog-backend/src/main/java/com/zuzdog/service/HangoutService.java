package com.zuzdog.service;

import com.zuzdog.dao.HangoutDao;
import com.zuzdog.dao.HangoutParticipantDao;
import com.zuzdog.dao.UserDao;
import com.zuzdog.dto.CreateHangoutRequest;
import com.zuzdog.dto.HangoutResponse;
import com.zuzdog.exception.ApiException;
import com.zuzdog.model.Hangout;
import com.zuzdog.model.HangoutActivityType;
import com.zuzdog.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

//  logic for hangouts Three main things :
// list all hangouts with per-user stats (participantCount + isUserSignedUp)
// create a hangout
// sign user up for a hangout
@Service
public class HangoutService {

    private final HangoutDao hangoutDao;
    private final HangoutParticipantDao participantDao;
    private final UserDao userDao;
    private final NotificationService notificationService;

    public HangoutService(HangoutDao hangoutDao,
                           HangoutParticipantDao participantDao,
                           UserDao userDao,
                           NotificationService notificationService) {
        this.hangoutDao = hangoutDao;
        this.participantDao = participantDao;
        this.userDao = userDao;
        this.notificationService = notificationService;
    }

    // sweeps expired hangouts off the map every hour. same pattern as
    // SessionService.purgeExpired() (needs @EnableScheduling on ZuzdogApplication).
    @Scheduled(fixedDelay = 3_600_000L)
    public void purgeExpired() {
        hangoutDao.deleteExpired();
    }

    // GET /api/hangouts  every hangout so we could put on map
    public List<HangoutResponse> getAllHangouts(long userId) {
        return hangoutDao.findAll(userId).stream()
                .map(HangoutService::toResponse)
                .toList();
    }

    // POST /api/hangouts — create a new hangout 
   
    public HangoutResponse createHangout(long organizerId, CreateHangoutRequest request) {
        Optional<User> organizer = userDao.findById(organizerId);
        if (organizer.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        }
        String organizerName = organizer.get().getUsername();

        HangoutActivityType activityType = parseActivityType(request.activityType());

        //insert into hangouts table and get generated id
        long hangoutId = hangoutDao.insert(
                organizerId,
                organizerName,
                request.title(),
                request.description(),
                request.latitude(),
                request.longitude(),
                request.eventTime(),
                activityType);

        // the organizer is automatically signed up for their own hangout
        participantDao.add(hangoutId, organizerId);

        // re-fetch to return the full row
        Optional<Hangout> created = hangoutDao.findById(hangoutId, organizerId);
        if (created.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load created hangout");
        }
        return toResponse(created.get());
    }

    // POST /api/hangouts/{id}/signup — add the user to the participant list 

    public HangoutResponse signup(long hangoutId, long userId) {
        Optional<Hangout> existing = hangoutDao.findById(hangoutId, userId);
        if (existing.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Hangout not found");
        }
        // participantDao.add uses ON CONFLICT (hangout_id, user_id) DO NOTHING, so it returns
        // 1 on a real first signup and 0 on a repeat signup (idiot proof)
        int added = participantDao.add(hangoutId, userId);
        Optional<Hangout> updated = hangoutDao.findById(hangoutId, userId);
        Hangout result = updated.orElse(existing.get());
        // if added == 1, we add a new participant and notify the organizer and other participantes, if added == 0 we do nothing.
        if (added == 1) {
            long organizerId = result.getOrganizerUserId();
            List<Long> participantIds = participantDao.findParticipantUserIds(hangoutId);
            notificationService.notifyHangoutJoin(hangoutId, userId, organizerId, participantIds);
        }
        return toResponse(result);
    }

    // DELETE /api/hangouts/{id}/signup — remove the requesting user from the list
    public HangoutResponse cancelSignup(long hangoutId, long userId) {
        // make sure the hangout itself exists before touching signups
        Optional<Hangout> existing = hangoutDao.findById(hangoutId, userId);
        if (existing.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Hangout not found");
        }
        // deletes the (hangoutId, userId) row if it exists; 0 rows affected if the
        
        participantDao.remove(hangoutId, userId);
        
        Optional<Hangout> updated = hangoutDao.findById(hangoutId, userId);
        Hangout result = updated.orElse(existing.get());
        return toResponse(result);
    }

    // GET /api/hangouts/mine — every hangout the requesting user is signed up for not all hangouts 
    public List<HangoutResponse> getUserHangouts(long userId) {
        // same shape as getAllHangouts() but  filtered DAO query
        
        return hangoutDao.findSignedUpByUser(userId).stream()
                .map(HangoutService::toResponse)
                .toList();
    }

    //hangout repsonse is a dto that we send to the frontend, it has all the needefeild of hangout
    private static HangoutResponse toResponse(Hangout h) {
        return new HangoutResponse(
                h.getHangoutId(),
                h.getOrganizerUserId(),
                h.getTitle(),
                h.getDescription() == null ? "" : h.getDescription(),
                h.getOrganizerName(),
                h.getLatitude(),
                h.getLongitude(),
                h.getEventTime(),
                h.getActivityType() == null ? null : h.getActivityType().name(),
                h.getCreatedAt(),
                h.getParticipantCount() == null ? 0 : h.getParticipantCount(),
                h.isUserSignedUp());
    }

    // check if things/strings are ok 
    private static HangoutActivityType parseActivityType(String s) {
        if (s == null || s.isBlank()) {
            return HangoutActivityType.MEETUP;
        }
        try {
            return HangoutActivityType.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid activityType: " + s);
        }
    }
}