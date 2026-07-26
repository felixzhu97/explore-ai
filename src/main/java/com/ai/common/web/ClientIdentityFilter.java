package com.ai.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Issues and resolves an anonymous browser client id via HttpOnly cookie.
 *
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html">OWASP Session Management</a>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@EnableConfigurationProperties(ClientIdentityProperties.class)
public class ClientIdentityFilter extends OncePerRequestFilter {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final ClientIdentityCookieFactory cookieFactory;

    public ClientIdentityFilter(ClientIdentityCookieFactory cookieFactory) {
        this.cookieFactory = cookieFactory;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String existing = readCookie(request);
        String clientId;
        if (existing != null) {
            clientId = existing;
        } else {
            clientId = cookieFactory.newClientId();
            response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.issue(clientId).toString());
        }
        request.setAttribute(ClientIdentity.REQUEST_ATTRIBUTE, clientId);
        filterChain.doFilter(request, response);
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String cookieName = cookieFactory.cookieName();
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value != null && UUID_PATTERN.matcher(value).matches()) {
                    return value;
                }
                return null;
            }
        }
        return null;
    }
}
