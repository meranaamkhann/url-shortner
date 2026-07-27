package com.urlshortener.controller;

import com.urlshortener.dto.request.UrlAccessRequest;
import com.urlshortener.exception.InvalidCredentialsException;
import com.urlshortener.exception.LinkPasswordRequiredException;
import com.urlshortener.service.impl.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequiredArgsConstructor
@Tag(name = "Redirect", description = "Public short-link resolution (the hot path)")
public class RedirectController {

    private final UrlService urlService;

    @GetMapping("/r/{shortCode}")
    @Operation(summary = "Resolve a short code and redirect to its destination URL")
    public ResponseEntity<?> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        try {
            String longUrl = urlService.resolveAndTrack(shortCode, request);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, longUrl)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .build();
        } catch (LinkPasswordRequiredException ex) {
            String accept = request.getHeader(HttpHeaders.ACCEPT);
            if (accept != null && accept.contains(MediaType.TEXT_HTML_VALUE)) {
                return passwordPromptResponse(shortCode, false);
            }
            throw ex;
        }
    }

    @PostMapping(value = "/r/{shortCode}/access", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Unlock a password-protected short link and redirect on success (API/JSON clients)")
    public ResponseEntity<Void> redirectWithPassword(@PathVariable String shortCode,
                                                       @Valid @RequestBody UrlAccessRequest request,
                                                       HttpServletRequest httpRequest) {
        String longUrl = urlService.resolveWithPassword(shortCode, request.password(), httpRequest);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, longUrl)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    @PostMapping(value = "/r/{shortCode}/access", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "Unlock a password-protected short link and redirect on success (browser form submit)")
    public ResponseEntity<?> redirectWithPasswordForm(@PathVariable String shortCode,
                                                        @RequestParam String password,
                                                        HttpServletRequest httpRequest) {
        try {
            String longUrl = urlService.resolveWithPassword(shortCode, password, httpRequest);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, longUrl)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .build();
        } catch (InvalidCredentialsException ex) {
            return passwordPromptResponse(shortCode, true);
        }
    }

    private ResponseEntity<String> passwordPromptResponse(String shortCode, boolean wrongPassword) {
        String safeCode = HtmlUtils.htmlEscape(shortCode);
        String error = wrongPassword
                ? "<p style=\"color:#D8695A;margin:0 0 16px;font-size:14px;\">That password didn't work — try again.</p>"
                : "";
        String html = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<title>Password required — RDRCT</title>"
                + "<style>"
                + "body{background:#0B0A08;color:#F3EFE7;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Inter,sans-serif;"
                + "min-height:100vh;display:flex;align-items:center;justify-content:center;margin:0;padding:24px;}"
                + ".card{background:#181513;border:1px solid rgba(255,250,240,0.09);border-radius:16px;padding:36px;max-width:380px;width:100%;}"
                + "h1{font-size:19px;font-weight:600;margin:0 0 8px;}"
                + "p.sub{color:#A39B8C;font-size:14px;margin:0 0 24px;}"
                + "input{width:100%;background:#1F1B18;border:1px solid rgba(255,250,240,0.18);border-radius:9px;"
                + "color:#F3EFE7;padding:12px 14px;font-size:15px;box-sizing:border-box;margin-bottom:16px;}"
                + "input:focus{outline:none;border-color:#E8B54A;}"
                + "button{width:100%;background:#E8B54A;color:#0B0A08;border:none;border-radius:9px;padding:12px;"
                + "font-size:15px;font-weight:600;cursor:pointer;}"
                + "button:hover{background:#F5CD73;}"
                + "</style></head><body>"
                + "<div class=\"card\"><h1>This link is password-protected</h1>"
                + "<p class=\"sub\">Enter the password to continue to the destination.</p>"
                + error
                + "<form method=\"post\" action=\"/r/" + safeCode + "/access\">"
                + "<input type=\"password\" name=\"password\" autofocus required autocomplete=\"current-password\">"
                + "<button type=\"submit\">Continue</button>"
                + "</form></div></body></html>";
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(html);
    }
}