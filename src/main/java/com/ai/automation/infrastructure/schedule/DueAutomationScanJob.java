package com.ai.automation.infrastructure.schedule;

import com.ai.automation.application.usecase.ExecuteDueAutomationsUseCase;
import com.ai.automation.application.AutomationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(AutomationProperties.class)
public class DueAutomationScanJob {

    private static final Logger log = LoggerFactory.getLogger(DueAutomationScanJob.class);

    private final ExecuteDueAutomationsUseCase executeDueAutomationsUseCase;
    private final AutomationProperties properties;

    public DueAutomationScanJob(
            ExecuteDueAutomationsUseCase executeDueAutomationsUseCase,
            AutomationProperties properties) {
        this.executeDueAutomationsUseCase = executeDueAutomationsUseCase;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.automation.scan-fixed-delay-ms:60000}")
    public void scan() {
        if (!properties.isScanEnabled()) {
            return;
        }
        int executed = executeDueAutomationsUseCase.executeDue();
        if (executed > 0) {
            log.info("Automation due scan executed={}", executed);
        }
    }
}
