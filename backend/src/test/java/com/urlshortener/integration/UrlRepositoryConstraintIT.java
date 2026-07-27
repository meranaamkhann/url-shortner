package com.urlshortener.integration;

import com.urlshortener.domain.entity.Url;
import com.urlshortener.domain.enums.UrlStatus;
import com.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;


class UrlRepositoryConstraintIT extends AbstractIntegrationTest {

    @Autowired
    private UrlRepository urlRepository;

    @Test
    void duplicateShortCodeInDefaultDomain_violatesUniqueConstraint() {
        urlRepository.saveAndFlush(buildUrl("dupCode1"));

        assertThatThrownBy(() -> urlRepository.saveAndFlush(buildUrl("dupCode1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Url buildUrl(String shortCode) {
        return Url.builder()
                .shortCode(shortCode)
                .longUrl("https://example.com/" + shortCode)
                .longUrlHash("hash-" + shortCode)
                .status(UrlStatus.ACTIVE)
                .build();
    }
}
