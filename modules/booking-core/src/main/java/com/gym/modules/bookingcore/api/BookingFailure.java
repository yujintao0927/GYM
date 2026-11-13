package com.gym.modules.bookingcore.api;
public final class BookingFailure extends RuntimeException {
 private final int status; private final String code;
 public BookingFailure(int status,String code){super(code);this.status=status;this.code=code;}
 public int status(){return status;} public String code(){return code;}
}
