package com.example.FixItNow.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.FixItNow.dto.v1.AuthSessionV1;
import com.example.FixItNow.service.AuthV1Service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * After Google verifies the user, mint our own JWT + refresh token and redirect the
 * browser back to the frontend callback with the tokens in the URL fragment (so they
 * are not sent to any server in the Referer header).
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthV1Service authService;

    @Value("${app.oauth2.frontend-redirect-uri:http://localhost:3000/oauth/callback}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = (String) principal.getAttribute("email");
        String name = (String) principal.getAttribute("name");

        AuthSessionV1 session = authService.oauthLogin(email, name);

        String target = frontendRedirectUri
                + "#access_token=" + enc(session.getAccessToken())
                + "&refresh_token=" + enc(session.getRefreshToken());

        getRedirectStrategy().sendRedirect(request, response, target);
    }

    private String enc(String v) {
        return URLEncoder.encode(v != null ? v : "", StandardCharsets.UTF_8);
    }
}
