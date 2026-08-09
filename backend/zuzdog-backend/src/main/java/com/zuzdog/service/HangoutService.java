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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Business logic for hangouts. Three actions:
// list all hangouts with per-user stats (participantCount + isUserSignedUp)
// create a hangout
// sign the requesting user up for a hangout
// The service is the only layer that talks to UserDao; the DAOs only know SQL.
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

    // GET /api/hangouts  every hangout, newest first, enriched with the user`s signup state.
    public List<HangoutResponse> getAllHangouts(long userId) {
        return hangoutDao.findAll(userId).stream()
                .map(HangoutService::toResponse)
                .toList();
    }

    // POST /api/hangouts — create a new hangout on behalf of the authenticated user.
    // organizerName is looked up from the users row so the client cannot forge it.
    // orgnaizerId is the Id of the authinicated user
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

        // re-fetch to return the full row 
        Optional<Hangout> created = hangoutDao.findById(hangoutId, organizerId);
        if (created.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load created hangout");
        }
        return toResponse(created.get());
    }

    // POST /api/hangouts/{id}/signup — add the requesting user to the participant list.
    // Idempotent: signing up twice is a no-op. Returns the updated hangout so the caller
    // sees the new participantCount and isUserSignedUp=true.
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

    // map a Hangout model to the response DTO. activityType is flattened to its String name.
    // we need it so the controller can return JSON object with the right fields from the model.
    // we also choose what to return when some fields are null
    // HangoutRespone is taken from the DTO.
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

    // parse the activityType string from the request. null/blank defaults to MEETUP.
    // an unknown value is a 400 — simpler than silently picking a default for a typo.
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