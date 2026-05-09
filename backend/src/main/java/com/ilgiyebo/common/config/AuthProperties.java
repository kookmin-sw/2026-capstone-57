package com.ilgiyebo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private List<String> allowedDomains = List.of();
    private long verificationExpiryMinutes = 10;
    private Map<String, String> universityDomainMap = new HashMap<>();

    public List<String> getAllowedDomains() { return allowedDomains; }
    public void setAllowedDomains(List<String> allowedDomains) { this.allowedDomains = allowedDomains; }
    public long getVerificationExpiryMinutes() { return verificationExpiryMinutes; }
    public void setVerificationExpiryMinutes(long verificationExpiryMinutes) { this.verificationExpiryMinutes = verificationExpiryMinutes; }
    public Map<String, String> getUniversityDomainMap() { return universityDomainMap; }
    public void setUniversityDomainMap(Map<String, String> universityDomainMap) { this.universityDomainMap = universityDomainMap; }
}
