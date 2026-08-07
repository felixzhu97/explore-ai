package com.ai.automation.domain.repository;

import com.ai.automation.domain.model.EmailMessage;

public interface EmailGateway {

    void send(EmailMessage message);
}
