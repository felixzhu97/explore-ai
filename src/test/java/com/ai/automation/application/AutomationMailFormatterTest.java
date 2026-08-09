package com.ai.automation.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AutomationMailFormatter")
class AutomationMailFormatterTest {

    private final AutomationMailFormatter formatter = new AutomationMailFormatter();

    @Test
    void shouldRenderMarkdownHeadingsAndListsWhenFormatHtml() {
        String markdown = """
                ## Thesis
                **Fact:** Databricks leads.
                ---
                ## Competitor / market signals
                - Item one
                - Item two with [link](https://example.com/x)
                """;

        AutomationMailFormatter.FormattedMail mail =
                formatter.format("Databricks简报", "Focus on GTM", markdown);

        assertThat(mail.htmlBody()).contains("<h2");
        assertThat(mail.htmlBody()).contains("Thesis");
        assertThat(mail.htmlBody()).contains("<strong>");
        assertThat(mail.htmlBody()).contains("<ul");
        assertThat(mail.htmlBody()).contains("https://example.com/x");
        assertThat(mail.htmlBody()).contains("Databricks简报");
        assertThat(mail.htmlBody()).contains("Focus on GTM");
        assertThat(mail.htmlBody()).doesNotContain("## Thesis");
    }

    @Test
    void shouldStripMarkdownMarkersWhenFormatPlainText() {
        String markdown = """
                ## Thesis
                **Fact:** Growing fast.
                - Signal A
                """;

        AutomationMailFormatter.FormattedMail mail =
                formatter.format("Daily", "Do research", markdown);

        assertThat(mail.textBody()).contains("Automation: Daily");
        assertThat(mail.textBody()).contains("Task:");
        assertThat(mail.textBody()).contains("Do research");
        assertThat(mail.textBody()).contains("Thesis");
        assertThat(mail.textBody()).contains("Fact: Growing fast.");
        assertThat(mail.textBody()).contains("• Signal A");
        assertThat(mail.textBody()).doesNotContain("## ");
        assertThat(mail.textBody()).doesNotContain("**");
    }

    @Test
    void shouldEscapeScheduleNameInHtmlShellWhenNameHasSpecialChars() {
        AutomationMailFormatter.FormattedMail mail =
                formatter.format("A <B> & \"C\"", "brief", "ok");

        assertThat(mail.htmlBody()).contains("A &lt;B&gt; &amp; &quot;C&quot;");
        assertThat(mail.htmlBody()).doesNotContain("A <B>");
    }
}
