package com.example.sales_test_1.filter;

import com.example.sales_test_1.model.SessionInfo;
import com.example.sales_test_1.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private final AuthService authService;

    public AuthFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String method = request.getMethod();
        String path   = request.getRequestURI();
        String token  = request.getHeader("X-Auth-Token");

        log.debug("--> {} {} | token={}", method, path, token != null ? token.substring(0, 8) + "…" : "none");

        if (token != null && !token.isBlank()) {
            SessionInfo session = authService.validateSession(token);
            if (session != null) {
                request.setAttribute("currentUser", session);
                log.debug("Authenticated: {} [{}] role={}", session.getName(), session.getEmail(), session.getRole());
            } else {
                log.warn("Invalid/expired token on {} {}", method, path);
            }
        }

        if (isPublicRoute(method, path)) {
            chain.doFilter(request, response);
            return;
        }

        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null) {
            log.warn("Unauthorized access blocked: {} {} | ip={}", method, path, request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Authentication required\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicRoute(String method, String path) {
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) return true;
        if ("GET".equals(method) && path.startsWith("/api/products")) return true;
        if (!path.startsWith("/api/")) return true;
        return false;
    }
}
