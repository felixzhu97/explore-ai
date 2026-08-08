package com.ai.account.infrastructure.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * True when Google and/or GitHub OAuth is enabled with non-blank client credentials.
 */
public class AnyOAuthProviderReadyCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        return isReady(env, "app.oauth.google") || isReady(env, "app.oauth.github");
    }

    private static boolean isReady(Environment env, String prefix) {
        if (!env.getProperty(prefix + ".enabled", Boolean.class, false)) {
            return false;
        }
        return StringUtils.hasText(env.getProperty(prefix + ".client-id"))
                && StringUtils.hasText(env.getProperty(prefix + ".client-secret"));
    }
}
