package com.kramp.productinfo.domain.ports.exception;

public class UpstreamFailureException extends RuntimeException {

    private final String service;
    private final String reason;
    private final String details;

    public UpstreamFailureException(String service, String reason) {
        this(service, reason, null, null);
    }

    public UpstreamFailureException(String service, String reason, String details) {
        this(service, reason, details, null);
    }

    public UpstreamFailureException(String service, String reason, String details, Throwable cause) {
        super(buildMessage(service, reason, details), cause);
        this.service = service;
        this.reason = reason;
        this.details = details;
    }

    public String service() {
        return service;
    }

    public String reason() {
        return reason;
    }

    public String details() {
        return details;
    }

    private static String buildMessage(String service, String reason, String details) {
        if (details == null || details.isBlank()) {
            return service + " failed: " + reason;
        }
        return service + " failed: " + reason + " (" + details + ")";
    }
}
