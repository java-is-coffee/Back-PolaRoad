package javaiscoffee.polaroad.login;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import javaiscoffee.polaroad.exception.NotFoundException;
import javaiscoffee.polaroad.response.ResponseMessages;
import javaiscoffee.polaroad.response.Status;
import javaiscoffee.polaroad.member.Member;
import javaiscoffee.polaroad.wrapper.RequestWrapperDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@Tag(name = "로그인 관련 API", description = "로그인에 관련된 API들 모음")
public class LoginController {
    private final LoginService loginService;

    @Operation(summary = "로그인 API", description = "로그인할 때 사용하는 API")
    @Parameter(name = "email", description = "## 사용자 이메일" +
            "\n- 이메일 형식이어야 함", required = true, example = "aaa@naver.com")
    @Parameter(name = "password", description = "## 사용자 비밀번호" +
            "\n- 8~20자 사이" +
            "\n- 소문자, 숫자, 특수문자 최소 1글자씩 있어야함", required = true, example = "Abcdefgh1!")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공했을 경우"),
            @ApiResponse(responseCode = "404", description = "로그인에 실패한 경우")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Validated @RequestBody RequestWrapperDto<LoginDto> requestDto, HttpServletResponse response) {
        LoginDto loginDto = requestDto.getData();
        log.info("로그인 요청 = {}",loginDto);
        boolean result = loginService.login(loginDto, response);
        //로그인 실패했을 경우 실패 Response 반환
        if(!result) throw new NotFoundException(ResponseMessages.LOGIN_FAILED.getMessage());

        return ResponseEntity.ok(ResponseMessages.SUCCESS.getMessage());
    }

    @Hidden
    @PostMapping("test")
    public ResponseEntity<?> test() {
        log.info("토큰 테스트");
        return ResponseEntity.ok("토큰 인증 성공");
    }

    @Operation(summary = "회원가입 API", description = "회원가입할 때 사용하는 API")
    @Parameter(name = "email", description = "## 사용자 이메일" +
            "\n- 이메일 형식이어야 함", required = true, example = "aaa@naver.com")
    @Parameter(name = "password", description = "## 사용자 비밀번호" +
            "\n- 8~20자 사이" +
            "\n- 소문자, 숫자, 특수문자 최소 1글자씩 있어야함", required = true, example = "Abcdefgh1!")
    @Parameter(name = "name", description = "## 사용자 이름", required = true, example = "박자바")
    @Parameter(name = "nickname", description = "## 사용자 닉네임", required = true, example = "자바커피")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공했을 경우"),
            @ApiResponse(responseCode = "400", description = "이메일이 중복되거나 입력값이 형식에 맞지 않아서 회원가입 실패한 경우")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Validated @RequestBody RequestWrapperDto<RegisterDto> requestDto) {
        RegisterDto registerDto = requestDto.getData();
        log.info("registerDto = {}", registerDto);
        Member registerdMember = loginService.register(registerDto);
        if(registerdMember ==null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Status(ResponseMessages.REGISTER_FAILED));
        }
        return ResponseEntity.ok(null);
    }
}