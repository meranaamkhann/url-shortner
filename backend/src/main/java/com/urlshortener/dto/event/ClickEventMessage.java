package com.urlshortener.dto.event;

import java.io.Serializable;
import java.time.Instant;

public record ClickEventMessage(
        String urlId,
        String shortCode,
        Instant clickedAt,
        String ipHash,
        String userAgent,
        String referrer,
        String countryCode,
        String city
) implements Serializable {
}
