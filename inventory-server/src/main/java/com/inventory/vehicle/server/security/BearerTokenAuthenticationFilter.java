package com.inventory.vehicle.server.security;

import com.inventory.vehicle.server.auth.SessionTokenService;
import com.inventory.vehicle.server.auth.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SessionTokenService sessionTokenService;
    private final UserRepository userRepository;

    public BearerTokenAuthenticationFilter(SessionTokenService sessionTokenService, UserRepository userRepository) {
        this.sessionTokenService = sessionTokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            sessionTokenService.findUserId(token)
                    .flatMap(userRepository::findById)
                    .filter(user -> user.isActive())
                    .ifPresent(user -> {
                        var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
                        var authentication = new UsernamePasswordAuthenticationToken(user, null, java.util.List.of(authority));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        filterChain.doFilter(request, response);
    }
}
