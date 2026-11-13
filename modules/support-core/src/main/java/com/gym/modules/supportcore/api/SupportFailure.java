package com.gym.modules.supportcore.api;
public final class SupportFailure extends RuntimeException {
 public final int status;
 public SupportFailure(int status,String code) {super(code);this.status=status;}
}
