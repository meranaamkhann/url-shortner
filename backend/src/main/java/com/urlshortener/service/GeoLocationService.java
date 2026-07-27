package com.urlshortener.service;

import org.springframework.stereotype.Service;


@Service
public class GeoLocationService {

    public record GeoLocation(String countryCode, String city) {
    }

    public GeoLocation resolve(String ipAddress) {
        if (ipAddress == null || isPrivateOrLoopback(ipAddress)) {
            return new GeoLocation("XX", "Unknown");
        }
        // Placeholder: production code replaces this with a MaxMind DatabaseReader lookup.
        return new GeoLocation("XX", "Unknown");
    }

    private boolean isPrivateOrLoopback(String ip) {
        return ip.startsWith("10.") || ip.startsWith("192.168.") || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1");
    }
}
