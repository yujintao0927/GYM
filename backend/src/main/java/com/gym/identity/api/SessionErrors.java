package com.gym.identity.api;

import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes=SessionController.class) @Profile("local-dev")
public class SessionErrors {
 @ExceptionHandler(SessionFailure.class) public ResponseEntity<?> failure(SessionFailure error){
  var response=ResponseEntity.status(error.status).cacheControl(CacheControl.noStore());if(error.retryAfter>0)response.header("Retry-After",Long.toString(error.retryAfter));
  return response.body(Map.of("code",error.code,"message","Request rejected","requestId",UUID.randomUUID().toString(),"retryable",error.status==429,"details",Map.of()));
 }
 @ExceptionHandler(Exception.class) public ResponseEntity<?> unexpected(Exception error){

  int code=error instanceof org.springframework.http.converter.HttpMessageNotReadableException?422:503;
  return failure(new SessionFailure(code,code==422?"INVALID_INPUT":"ADAPTER_UNAVAILABLE"));
 }
}
