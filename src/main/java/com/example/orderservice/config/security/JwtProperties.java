package com.example.orderservice.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private String customerIdClaim = "sub";
    private String rolesClaim      = "roles";
}