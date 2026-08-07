package com.ai.automation.domain.model;

import java.util.Objects;

public record EmailMessage(String to, String subject, String textBody, String htmlBody) {

    public EmailMessage(String to, String subject, String textBody) {
        this(to, subject, textBody, null);
    }

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
        if (htmlBody != null && htmlBody.isBlank()) {
            htmlBody = null;
        }
    }

    public boolean hasHtmlBody() {
        return htmlBody != null && !htmlBody.isBlank();
    }
}
