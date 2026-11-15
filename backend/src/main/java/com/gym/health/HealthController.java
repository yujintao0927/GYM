package com.gym.health;
import java.time.Clock;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.gym.identity.api.SessionFailure;
@RestController @Profile("local-dev")
@RequestMapping("/v1/me")
public class HealthController {
 private final HealthService s;private final Clock clock;
 public HealthController(HealthService s,Clock clock){this.s=s;this.clock=clock;}
 private ResponseEntity<?> respond(HttpServletRequest req,int status,java.util.function.Function<String,Object> work){if(req.getHeader("Cookie")!=null)throw new SessionFailure(422,"AMBIGUOUS_AUTH");Object result=s.owned(req.getHeader("Authorization"),work);return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("data",result,"requestId",UUID.randomUUID().toString(),"mode","SIMULATED","serverTime",clock.instant().toString(),"contractVersion","0.1"));}
 private String cat(String path){return path.equals("body-measurements")?"MEASUREMENT":"RECORD";}
 @PostMapping({"/health-records","/body-measurements"}) public ResponseEntity<?> create(@RequestBody Map<String,Object> b,HttpServletRequest req){return respond(req,201,o->s.create(o,req.getServletPath().endsWith("body-measurements")?"MEASUREMENT":"RECORD",b,req.getHeader("Idempotency-Key")));}
 @GetMapping({"/health-records","/body-measurements"}) public ResponseEntity<?> list(@RequestParam Map<String,String> q,HttpServletRequest req){return respond(req,200,o->s.list(o,req.getServletPath().endsWith("body-measurements")?"MEASUREMENT":"RECORD",q));}
 @GetMapping({"/health-records/{id}","/body-measurements/{id}"}) public ResponseEntity<?> get(@PathVariable String id,HttpServletRequest req){return respond(req,200,o->s.get(o,req.getServletPath().contains("body-measurements")?"MEASUREMENT":"RECORD",id));}
 @PatchMapping({"/health-records/{id}","/body-measurements/{id}"}) public ResponseEntity<?> patch(@PathVariable String id,@RequestBody Map<String,Object> b,HttpServletRequest req){return respond(req,200,o->s.patch(o,req.getServletPath().contains("body-measurements")?"MEASUREMENT":"RECORD",id,b));}
 @DeleteMapping({"/health-records/{id}","/body-measurements/{id}"}) public ResponseEntity<?> delete(@PathVariable String id,@RequestParam Map<String,String> q,HttpServletRequest req){return respond(req,200,o->{if(!q.keySet().equals(Set.of("expectedVersion")))throw new SessionFailure(422,"INVALID_INPUT");return s.delete(o,req.getServletPath().contains("body-measurements")?"MEASUREMENT":"RECORD",id,s.expected(q.get("expectedVersion")));});}
 @GetMapping("/weight-goal") public ResponseEntity<?> goal(HttpServletRequest req){return respond(req,200,s::goal);}
 @PutMapping("/weight-goal") public ResponseEntity<?> putGoal(@RequestBody Map<String,Object> b,HttpServletRequest req){return respond(req,200,o->s.putGoal(o,b));}
 @DeleteMapping("/weight-goal") public ResponseEntity<?> deleteGoal(@RequestParam Map<String,String> q,HttpServletRequest req){return respond(req,200,o->{if(!q.keySet().equals(Set.of("expectedVersion")))throw new SessionFailure(422,"INVALID_INPUT");return s.deleteGoal(o,s.expected(q.get("expectedVersion")));});}
}
