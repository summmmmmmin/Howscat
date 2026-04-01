package com.example.howscat;

/**
 * 인증/인가 관련 실패를 표현하기 위한 예외.
 * (기존 UserService가 참조하지만 프로젝트에 소스가 없어 발생한 컴파일 에러 해결용)
 */
public class AuthException extends Exception {

    public AuthException() {
        super();
    }

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthException(Throwable cause) {
        super(cause);
    }
}

