package com.example.sales_test_1.filter;

import com.example.sales_test_1.model.SessionInfo;
import com.example.sales_test_1.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private final AuthService authService;

    public AuthFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String token = request.getHeader("X-Auth-Token");
        if (token != null && !token.isBlank()) {
            SessionInfo session = authService.validateSession(token);
            if (session != null) {
                request.setAttribute("currentUser", session);
            }
        }

        String method = request.getMethod();
        String path = request.getRequestURI();

        // Public routes — pass through without auth check
        if (isPublicRoute(method, path)) {
            chain.doFilter(request, response);
            return;
        }

        // Everything else requires authentication
        SessionInfo user = (SessionInfo) request.getAttribute("currentUser");
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Authentication required\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicRoute(String method, String path) {
        // Auth endpoints
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) return true;
        // Public product browsing
        if ("GET".equals(method) && path.startsWith("/api/products")) return true;
        // Static frontend assets
        if (!path.startsWith("/api/")) return true;
        return false;
    }
}
