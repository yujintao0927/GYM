package com.gym.modules.operationscore.api;
public final class Failure extends RuntimeException {
 public final int status;
 public Failure(int status,String code){super(code);this.status=status;}
}
