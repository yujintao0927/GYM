package com.gym.identity.api;

public final class SessionFailure extends RuntimeException {
 public final int status; public final String code; public final long retryAfter;
 public SessionFailure(int status,String code){this(status,code,0);}
 public SessionFailure(int status,String code,long retryAfter){super(code);this.status=status;this.code=code;this.retryAfter=retryAfter;}
}
