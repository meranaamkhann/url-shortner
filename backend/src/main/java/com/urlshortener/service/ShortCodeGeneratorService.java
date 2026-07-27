package com.urlshortener.service;

import com.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
public class ShortCodeGeneratorService {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final int codeLength;
    private final UrlRepository urlRepository;

    public ShortCodeGeneratorService(@Value("${app.short-code.length:7}") int codeLength, UrlRepository urlRepository) {
        this.codeLength = codeLength;
        this.urlRepository = urlRepository;
    }

    public String generateRandomCode() {
        StringBuilder sb = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public String generateUniqueCode() {
        final int maxAttempts = 5;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String candidate = generateRandomCode();
            if (!urlRepository.existsByShortCodeAndDomainIsNullAndDeletedAtIsNull(candidate)) {
                return candidate;
            }
            log.warn("Short code collision on attempt {} (candidate={}) — regenerating.", attempt, candidate);
        }
        throw new IllegalStateException(
                "Failed to generate a unique short code after " + maxAttempts + " attempts. Consider increasing app.short-code.length.");
    }
}
