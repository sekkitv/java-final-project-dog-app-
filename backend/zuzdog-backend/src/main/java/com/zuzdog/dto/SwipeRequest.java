package com.zuzdog.dto;

import com.zuzdog.model.SwipeAction;
import jakarta.validation.constraints.NotNull;

// Body of POST /api/swipe. Jackson deserializes the "action" JSON string
// ("UP"/"DOWN") directly into the SwipeAction enum.
public record SwipeRequest(@NotNull Long targetId, @NotNull SwipeAction action) {
}
