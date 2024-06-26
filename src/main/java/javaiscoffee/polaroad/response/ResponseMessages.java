package javaiscoffee.polaroad.response;

public enum ResponseMessages {
    SUCCESS("성공"),
    REGISTER_DUPLICATED("중복된 이메일이 존재합니다."),
    LOGIN_FAILED("로그인에 실패했습니다."),
    INPUT_ERROR("입력값이 잘못되었습니다."),
    REGISTER_FAILED("회원가입에 실패했습니다."),
    WISHNUMBER_OVER("최대 위시리스트 개수를 초과했습니다."),
    UNAUTHORIZED("유효하지 않은 토큰입니다."),
    SAVE_FAILED("저장에 실패했습니다."),
    DELETE_FAILED("삭제에 실패했습니다."),
    READ_FAILED("조회에 실패했습니다."),
    FORBIDDEN("권한이 없습니다."),
    GOOD_FAILED("자신이 작성한 게시글은 추천할 수 없습니다."),
    REVIEW_GOOD_FAILED("자신이 작성한 댓글은 추천할 수 없습니다."),
    BAD_REQUEST("잘못된 요청입니다."),
    NOT_FOUND("찾을 수 없음"),
    POST_NOT_FOUND("포스트를 찾을 수 없습니다."),
    MEMBER_NOT_FOUND("멤버를 찾을 수 없습니다."),
    SEND_FAILED("메세지를 전달할 수 없습니다."),
    SESSION_CLOSED("연결이 종료되어 있어 실패했습니다."),
    ERROR("서버 오류");
    // 기타 상태 코드

    private final String message;

    ResponseMessages(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}