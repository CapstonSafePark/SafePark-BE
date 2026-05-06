package com.safepark.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    // ⭐ 이 메서드 추가!
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") || path.startsWith("/api/parking-lots/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        System.out.println("\n=== JWT 필터 동작 확인 ===");
        System.out.println("요청 URI: " + request.getRequestURI());

        try {
            String token = getJwtFromRequest(request);
            System.out.println("1. 헤더에서 추출한 토큰: " + (token != null ? token.substring(0, 15) + "..." : "null"));

            if (token != null) {
                boolean isValid = jwtTokenProvider.validateToken(token);
                System.out.println("2. 토큰 유효성 검사 통과 여부: " + isValid);

                if (isValid) {
                    String username = jwtTokenProvider.getUsernameFromToken(token);
                    System.out.println("3. 토큰에서 찾은 유저 아이디: " + username);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    System.out.println("4. DB 유저 조회 성공: " + userDetails.getUsername());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("5. ✅ SecurityContext 인증 저장 완료!");
                } else {
                    System.out.println("❌ 에러: 토큰이 유효하지 않습니다. (만료되었거나 서명 불일치)");
                }
            } else {
                System.out.println("❌ 에러: 요청 헤더에 토큰이 존재하지 않거나 형식이 틀렸습니다.");
            }
        } catch (Exception e) {
            System.out.println("❌ JWT 필터 내부 에러 발생: " + e.getMessage());
            e.printStackTrace(); // 상세 에러 내용 출력
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}