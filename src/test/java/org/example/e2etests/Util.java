package org.example.e2etests;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public final class Util {

    private Util() {
    }

    public static HttpHeaders createHeaders(String jwtSecret) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Access-Token", jwtSecret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
