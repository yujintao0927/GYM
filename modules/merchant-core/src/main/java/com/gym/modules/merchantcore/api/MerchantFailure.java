package com.gym.modules.merchantcore.api;
public final class MerchantFailure extends RuntimeException {
 private final int status;
 public MerchantFailure(int status,String code){super(code);this.status=status;}
 public int status(){return status;}
}
