package com.gym.identity.api;

import java.time.Clock;
import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.gym.identity.session.SessionService;

@RestController @Profile("local-dev")
public class SessionController {
 private final SessionService service;private final Clock clock;
 public SessionController(SessionService service,Clock clock){this.service=service;this.clock=clock;}
 private String text(Map<String,Object> body,String key){Object v=body.get(key);if(!(v instanceof String s)||s.length()>512)throw new SessionFailure(422,"INVALID_INPUT");return s;}
 private void fields(Map<String,Object> body,String... fields){if(!body.keySet().equals(Set.of(fields)))throw new SessionFailure(422,"INVALID_INPUT");}
 private void transport(HttpServletRequest req){if(req.getHeader("Cookie")!=null)throw new SessionFailure(422,"AMBIGUOUS_AUTH");}
 private ResponseEntity<?> ok(int status,Object data){return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("data",data,"requestId",UUID.randomUUID().toString(),"mode","SIMULATED","serverTime",clock.instant().toString(),"contractVersion","0.1"));}
 @PostMapping("/v1/sessions") public ResponseEntity<?> login(@RequestBody Map<String,Object> body,@RequestHeader(value="Idempotency-Key",required=false)String key,HttpServletRequest req){
  transport(req);fields(body,"credential","entry","clientKind");
  if(!(body.get("credential") instanceof Map<?,?> raw))throw new SessionFailure(422,"INVALID_INPUT");
  Map<String,Object> credential=new HashMap<>();raw.forEach((k,v)->credential.put(String.valueOf(k),v));fields(credential,"kind","login","proof");
  if(!"LOCAL_PASSWORD".equals(text(credential,"kind")))throw new SessionFailure(422,"INVALID_INPUT");
  return ok(201,service.login(text(credential,"login"),text(credential,"proof"),text(body,"entry"),text(body,"clientKind"),key,req.getRemoteAddr()));
 }
 @PostMapping("/v1/sessions/refresh") public ResponseEntity<?> refresh(@RequestBody Map<String,Object> body,@RequestHeader(value="Idempotency-Key",required=false)String key,HttpServletRequest req){
  transport(req);fields(body,"refreshToken","generation");Object value=body.get("generation");if(!(value instanceof Integer||value instanceof Long))throw new SessionFailure(422,"INVALID_INPUT");
  return ok(201,service.refresh(text(body,"refreshToken"),((Number)value).longValue(),key,req.getRemoteAddr()));
 }
 @GetMapping("/v1/me/contexts")public ResponseEntity<?> contexts(@RequestHeader(value="Authorization",required=false)String bearer,HttpServletRequest req){transport(req);return ok(200,service.contexts(bearer));}
 @DeleteMapping("/v1/sessions/{id}")public ResponseEntity<?> logout(@PathVariable String id,@RequestHeader(value="Authorization",required=false)String bearer,HttpServletRequest req){transport(req);return ok(200,service.logout(bearer,id));}
}
