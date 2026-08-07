package com.ai.automation.domain.model;

import java.util.Objects;

public record EmailMessage(String to, String subject, String textBody) {

    public EmailMessage {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(textBody, "textBody");
        if (to.isBlank()) {
            throw new IllegalArgumentException("to cannot be blank");
        }
        if (subject.isBlank()) {
            throw new IllegalArgumentException("subject cannot be blank");
        }
    }
}
