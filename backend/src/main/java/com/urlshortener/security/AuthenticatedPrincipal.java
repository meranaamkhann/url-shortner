package com.urlshortener.security;

import java.util.UUID;

public record AuthenticatedPrincipal(UUID id, String username) {
}
