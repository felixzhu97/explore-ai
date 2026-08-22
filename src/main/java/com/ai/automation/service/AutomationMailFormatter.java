package com.ai.automation.service;

import com.ai.automation.domain.model.AutomationRun;
import java.util.Objects;
import java.util.regex.Pattern;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

/** Builds structured plain-text + HTML bodies for automation result emails. */
@Component
public class AutomationMailFormatter {

  private static final Pattern HEADING = Pattern.compile("(?m)^(#{1,6})\\s+(.+)$");
  private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
  private static final Pattern ITALIC =
      Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)");
  private static final Pattern LINK = Pattern.compile("\\[(.*?)]\\((https?://[^)\\s]+)\\)");
  private static final Pattern HORIZONTAL_RULE = Pattern.compile("(?m)^\\s*---\\s*$");
  private static final Pattern BULLET = Pattern.compile("(?m)^\\s*[-*]\\s+");

  private final Parser markdownParser = Parser.builder().build();
  private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().escapeHtml(true).build();

  /** Documentation. */
  public FormattedMail format(String scheduleName, String brief, String resultMarkdown) {
    Objects.requireNonNull(scheduleName, "scheduleName");
    String safeBrief = brief == null ? "" : brief.trim();
    String result = truncate(resultMarkdown == null ? "" : resultMarkdown);

    String textBody = buildText(scheduleName, safeBrief, result);
    String htmlBody = buildHtml(scheduleName, safeBrief, result);
    return new FormattedMail(textBody, htmlBody);
  }

  /** Documentation. */
  public record FormattedMail(String textBody, String htmlBody) {}

  private String buildText(String scheduleName, String brief, String result) {
    StringBuilder sb = new StringBuilder();
    sb.append("Automation: ").append(scheduleName).append("\n\n");
    sb.append("Task:\n").append(brief.isBlank() ? "—" : brief).append("\n\n");
    sb.append("Result:\n");
    sb.append(toReadablePlain(result));
    return sb.toString().trim() + "\n";
  }

  private String buildHtml(String scheduleName, String brief, String result) {
    String resultHtml = renderMarkdown(result);
    return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="utf-8"></head>
                <body style="margin:0;padding:0;background:#f5f5f7;">
                <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',\
                Helvetica,Arial,sans-serif;\
                color:#1d1d1f;line-height:1.55;max-width:680px;margin:24px auto;padding:24px 28px;\
                background:#ffffff;border:1px solid #e5e5ea;border-radius:12px;">
                  <p style="margin:0 0 8px;font-size:12px;letter-spacing:0.04em;\
                text-transform:uppercase;\
                color:#86868b;">ExploreAI Automation</p>
                  <h1 style="margin:0 0 16px;font-size:22px;font-weight:600;\
                line-height:1.3;">%s</h1>
                  <p style="margin:0 0 4px;font-size:13px;color:#86868b;">Task</p>
                  <p style="margin:0 0 20px;font-size:14px;white-space:pre-wrap;">%s</p>
                  <hr style="border:none;border-top:1px solid #e5e5ea;margin:0 0 20px;">
                  <div style="font-size:15px;">
                    %s
                  </div>
                </div>
                </body>
                </html>
                """
        .formatted(escapeHtml(scheduleName), escapeHtml(brief.isBlank() ? "—" : brief), resultHtml);
  }

  private String renderMarkdown(String markdown) {
    if (markdown == null || markdown.isBlank()) {
      return "<p style=\"color:#86868b;\">—</p>";
    }
    Node document = markdownParser.parse(markdown);
    String rendered = htmlRenderer.render(document);
    return rendered
        .replace("<h1>", "<h1 style=\"font-size:20px;margin:1.2em 0 0.5em;\">")
        .replace("<h2>", "<h2 style=\"font-size:17px;margin:1.2em 0 0.45em;\">")
        .replace("<h3>", "<h3 style=\"font-size:15px;margin:1em 0 0.4em;\">")
        .replace("<p>", "<p style=\"margin:0 0 0.85em;\">")
        .replace("<ul>", "<ul style=\"margin:0 0 0.85em;padding-left:1.25em;\">")
        .replace("<ol>", "<ol style=\"margin:0 0 0.85em;padding-left:1.25em;\">")
        .replace("<li>", "<li style=\"margin:0.2em 0;\">")
        .replace(
            "<hr />", "<hr style=\"border:none;border-top:1px solid #e5e5ea;margin:1.2em 0;\">")
        .replace("<hr>", "<hr style=\"border:none;border-top:1px solid #e5e5ea;margin:1.2em 0;\">")
        .replace("<a ", "<a style=\"color:#0071e3;\" ");
  }

  static String toReadablePlain(String markdown) {
    if (markdown == null || markdown.isBlank()) {
      return "—";
    }
    String text = markdown;
    text = LINK.matcher(text).replaceAll("$1 ($2)");
    text = HEADING.matcher(text).replaceAll("\n$2\n");
    text = BOLD.matcher(text).replaceAll("$1");
    text = ITALIC.matcher(text).replaceAll("$1");
    text = HORIZONTAL_RULE.matcher(text).replaceAll("\n");
    text = BULLET.matcher(text).replaceAll("• ");
    text = text.replaceAll("(?m)^\\s*\\d+\\.\\s+", "  ");
    text = text.replaceAll("\\n{3,}", "\n\n");
    return text.trim();
  }

  private static String truncate(String value) {
    if (value.length() <= AutomationRun.MAX_RESULT_EXCERPT) {
      return value;
    }
    return value.substring(0, AutomationRun.MAX_RESULT_EXCERPT);
  }

  private static String escapeHtml(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
