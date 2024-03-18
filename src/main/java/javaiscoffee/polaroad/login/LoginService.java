package javaiscoffee.polaroad.login;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import javaiscoffee.polaroad.exception.UnAuthorizedException;
import javaiscoffee.polaroad.login.emailAuthentication.EmailCertificationRepository;
import javaiscoffee.polaroad.response.ResponseMessages;
import javaiscoffee.polaroad.exception.NotFoundException;
import javaiscoffee.polaroad.security.JwtTokenProvider;
import javaiscoffee.polaroad.member.*;
import javaiscoffee.polaroad.security.TokenDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import jakarta.servlet.http.Cookie;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LoginService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder bCryptPasswordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;
//    private final MailVerifyService mailVerifyService;
    private final EmailCertificationRepository emailCertificationRepository;

    /**
     * 1. 로그인 요청으로 들어온 memberId, password를 기반으로 Authentication 객체를 생성한다.
     * 2. authenticate() 메서드를 통해 요청된 Member에 대한 검증이 진행된다.
     * 3. 검증이 정상적으로 통과되었다면 인증된 Authentication 객체를 기반으로 JWT 토큰을 생성한다.
     */
    public TokenDto login(LoginDto loginDto, HttpServletResponse response) {
        log.info("로그인 검사 시작 loginDto={}",loginDto);
        Member member = memberRepository.findByEmail(loginDto.getEmail()).orElseThrow(() -> new NotFoundException(ResponseMessages.NOT_FOUND.getMessage()));
        if(member.getStatus() == MemberStatus.DELETED) throw new NotFoundException(ResponseMessages.NOT_FOUND.getMessage());
        // 1. Login ID/PW 를 기반으로 Authentication 객체 생성
        // 이때 authentication 는 인증 여부를 확인하는 authenticated 값이 false
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword());
        log.info("authenticationToken = {}",authenticationToken);
        // 2. 실제 검증 (사용자 비밀번호 체크)이 이루어지는 부분
        // authenticate 매서드가 실행될 때 CustomUserDetailsService 에서 만든 loadUserByUsername 메서드가 실행
        try {
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            log.info("Authentication successful, authentication = {}", authentication);

            return jwtTokenProvider.generateToken(authentication);
        } catch (Exception e) {
            log.error("로그인 실패: {}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public Member register(RegisterDto registerDto) {
        //이미 중복된 이메일이 존재
        if(memberRepository.findByEmail(registerDto.getEmail()).isPresent()) {
            log.info("중복 회원가입 실패 처리");
            return null;
        }

        //이메일 인증한 적이 없으면 예외처리
//        if (!mailVerifyService.isVerified(registerDto.getEmail(), registerDto.getCertificationNumber())) {
//            log.info("이메일 인증을 하지 않았습니다.");
//            return null;
//        }


        //중복이 없으면 회원가입 진행
        //현재 이메일 회원가입이라 socialLogin = null로 처리 oauth 추가시 변경해야 함
        Member newMember = new Member(registerDto.getEmail(), registerDto.getName(), registerDto.getNickname(), registerDto.getPassword(), "", 0, 0, 0, MemberRole.USER,null);
        newMember.hashPassword(bCryptPasswordEncoder);
//        log.info("save하려는 멤버 = {}", newMember);
        memberRepository.save(newMember);
        Optional<Member> savedMember = memberRepository.findByEmail(newMember.getEmail());
        if(savedMember.isPresent()) {
            log.info("회원가입 성공 = {}", savedMember.get());
//            emailCertificationRepository.removeEmailVerificationNumber(registerDto.getEmail());
            return savedMember.get();
        }
        return null;
    }

    /**
     * 카카오 로그인 및 회원가입
     * 가입 정보가 없으면 간편 회원가입 후 토큰 발행
     */
    @Transactional
    public TokenDto oauthLogin (Map<String, Object> userInfo) {
        String email = (String) userInfo.get("email");
        Optional<Member> findMember = memberRepository.findByEmail(email);
        Member member;
        // 가입 정보가 없으면 간편 회원가입 처리
        if(findMember.isEmpty()) {
            member = new Member(email, "폴라", (String) userInfo.get("nickname"),  String.valueOf(userInfo.get("id")));
            member.setProfileImage((String) userInfo.get("profile"));
            member.setSocialLogin((SocialLogin) userInfo.get("socialLogin"));
            member.hashPassword(bCryptPasswordEncoder);
            memberRepository.save(member);
        }

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email, String.valueOf(userInfo.get("id")));
        try {
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            log.info("Authentication successful, authentication = {}", authentication);

            return jwtTokenProvider.generateToken(authentication);
        } catch (Exception e) {
            log.error("로그인 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * access 토큰 재발급
     * isTemp = true이면 라이브코딩용 임시 토큰 발급이므로 1분짜리 토큰 발급
     * isTemp = false이면 일반적인 30분 토큰 발급
     */
    public TokenDto refresh(String refreshToken, HttpServletResponse response) {
        try {
            // refreshToken 유효성 검증
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                // 유효하지 않은 경우, 적절한 응답 반환
                throw new UnAuthorizedException("유효하지 않는 쿠키입니다.");
            }

            return jwtTokenProvider.generateNewAccessToken(refreshToken, 1000 * 60 * 30, response);

        } catch (JwtException | IllegalArgumentException e) {
            // 토큰 파싱 실패 또는 유효하지 않은 토큰으로 인한 예외 처리
            log.error("토큰 갱신 실패: {}", e.getMessage());
            throw new UnAuthorizedException(ResponseMessages.UNAUTHORIZED.getMessage());
        }
    }
}
