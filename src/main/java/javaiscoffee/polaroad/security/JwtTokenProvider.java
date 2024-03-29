package javaiscoffee.polaroad.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import javaiscoffee.polaroad.exception.NotFoundException;
import javaiscoffee.polaroad.member.JpaMemberRepository;
import javaiscoffee.polaroad.member.Member;
import javaiscoffee.polaroad.member.MemberStatus;
import javaiscoffee.polaroad.response.ResponseMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {
    private final Key key;
    private final JpaMemberRepository memberRepository;

    public JwtTokenProvider(@Value("${JWT_SECRET_KEY}")String secretKey, JpaMemberRepository memberRepository) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.memberRepository = memberRepository;
    }

    //유저 정보를 가지고 있는 AccessToken, RefreshToken을 생성하는 메서드
    public TokenDto generateToken(Authentication authentication) {
        //권한 가져오기
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        long now = (new Date().getTime());

        Object principal = authentication.getPrincipal();

        //Access Token 생성 30분
        Date accessTokenExpiresIn = new Date(now + (1000*60*30));
        Long memberId;
        String accessToken;

        if (principal instanceof CustomUserDetails) {
            memberId = ((CustomUserDetails)principal).getMemberId();
            accessToken = Jwts.builder()
                    .setSubject(authentication.getName())
                    .claim("auth", authorities)
                    .claim("memberId", memberId) // memberId 정보 추가
                    .setExpiration(accessTokenExpiresIn)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
        } else if (principal instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) principal;
            // memberId를 attributes에서 가져오기
            memberId = oAuth2User.getAttribute("memberId");
            log.info("토큰 생성 memberId = {}",memberId);
            accessToken = Jwts.builder()
                    .setSubject(oAuth2User.getAttribute("email"))
                    .claim("auth", authorities)
                    .claim("memberId", memberId) // memberId 정보 추가
                    .setExpiration(accessTokenExpiresIn)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
        } else {
            throw new IllegalArgumentException("Unsupported principal type");
        }

        //Refresh Token 생성 1주일
        String refreshToken = Jwts.builder()
                .claim("memberId", memberId)
                .setExpiration(new Date(now + (1000 * 60 * 60 * 24 * 7)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

//        // 쿠키에 액세스 토큰 저장
//        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
//        accessTokenCookie.setPath("/");
//        accessTokenCookie.setHttpOnly(true);
//        accessTokenCookie.setMaxAge(30 * 60); // 쿠키 유효 시간을 30분으로 설정
//        response.addCookie(accessTokenCookie);
//
//        // 쿠키에 리프레시 토큰 저장
//        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
//        refreshTokenCookie.setPath("/");
//        refreshTokenCookie.setHttpOnly(true);
//        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 쿠키 유효 시간을 1주일로 설정
//        response.addCookie(refreshTokenCookie);

        return TokenDto.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    //OAuth 멤버 정보로 토큰 발급
    public TokenDto generateToken(Authentication authentication, Long memberId, String email,HttpServletResponse response) {
        //권한 가져오기
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        long now = (new Date().getTime());

        Object principal = authentication.getPrincipal();

        //Access Token 생성 30분
        Date accessTokenExpiresIn = new Date(now + (1000 * 60 * 30));
        String accessToken;

        if (principal instanceof CustomUserDetails) {
            accessToken = Jwts.builder()
                    .setSubject(authentication.getName())
                    .claim("auth", authorities)
                    .claim("memberId", memberId) // memberId 정보 추가
                    .setExpiration(accessTokenExpiresIn)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
        } else if (principal instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) principal;
            // memberId를 attributes에서 가져오기
            log.info("토큰 생성 memberId = {}",memberId);
            accessToken = Jwts.builder()
                    .setSubject(email)
                    .claim("auth", authorities)
                    .claim("memberId", memberId) // memberId 정보 추가
                    .setExpiration(accessTokenExpiresIn)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
        } else {
            throw new IllegalArgumentException("Unsupported principal type");
        }

        //Refresh Token 생성 1주일
        String refreshToken = Jwts.builder()
                .claim("memberId", memberId)
                .setExpiration(new Date(now + (1000 * 60 * 60 * 24 * 7)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

//        //쿠키에 액세스 토큰 저장
//        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
//        accessTokenCookie.setPath("/");
//        accessTokenCookie.setHttpOnly(true);
//        accessTokenCookie.setMaxAge(30 * 60); // 쿠키 유효 시간을 30분으로 설정
//        response.addCookie(accessTokenCookie);

//        //쿠키에 리프레시 토큰 저장
//        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
//        refreshTokenCookie.setPath("/");
//        refreshTokenCookie.setHttpOnly(true);
//        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 쿠키 유효 시간을 1주일로 설정
//        response.addCookie(refreshTokenCookie);

        return TokenDto.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    //Access Token이 만료되어서 refresh Token을 받았을 때 새로운 access Token을 생성해서 반환
    public TokenDto generateNewAccessToken(String refreshToken, int expirationTime, HttpServletResponse response) {
        // refreshToken에서 클레임 추출
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();
        Long memberId = claims.get("memberId", Long.class);

        log.info("토큰 재발급하는 memberId = {}", memberId);

        Member member = memberRepository.findByMemberId(memberId).orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));
        if(member.getStatus().equals(MemberStatus.DELETED)) throw new NotFoundException(ResponseMessages.NOT_FOUND.getMessage());

        // 사용자 정보에서 권한 가져오기
        String authorities = member.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        // 새로운 AccessToken 생성
        long now = (new Date().getTime());
        Date accessTokenExpiresIn = new Date(now + (expirationTime)); // 30분 후 만료
        // 새로운 AccessToken 반환
        String accessToken = Jwts.builder()
                .setSubject(member.getUsername())
                .claim("auth", authorities)
                .claim("memberId", member.getMemberId())
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return TokenDto.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // JWT 토큰을 복호화하여 토큰에 들어있는 정보를 꺼내는 메서드
    public Authentication getAuthentication(String accessToken) {
        // 토큰 복호화
        Claims claims = parseClaims(accessToken);

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 클레임에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // 클레임에서 memberId 추출
        Long memberId = claims.get("memberId", Long.class); // 여기서는 claims에서 "memberId" 클레임을 Long 타입으로 추출합니다.

        // CustomUserDetails 객체를 만들어서 Authentication 리턴
        CustomUserDetails principal = new CustomUserDetails();
        principal.setUsername(claims.getSubject()); // 이메일(혹은 사용자명) 설정
        principal.setAuthorities(authorities); // 권한 설정
        principal.setMemberId(memberId); // memberId 설정

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    // 토큰 정보를 검증하는 메서드
    public boolean validateToken(String token) {
        log.debug("ValidateToken 메서드 JWT token 검증 : {}", token);
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
//            log.info("Invalid JWT Token", e);
            log.info("Invalid JWT Token");
        } catch (ExpiredJwtException e) {
//            log.info("Expired JWT Token", e);
            log.info("Expired JWT Token");
        } catch (UnsupportedJwtException e) {
//            log.info("Unsupported JWT Token", e);
            log.info("Unsupported JWT Token");
        } catch (IllegalArgumentException e) {
//            log.info("JWT claims string is empty.", e);
            log.info("JWT claims string is empty.");
        }
        return false;
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
