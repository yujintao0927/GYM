package com.gym.modules.merchantcore.domain;
import java.time.Instant;
/** Calendar normalization belongs to an approved caller policy, never to this value. */
public record ServiceTerm(Instant end, Instant graceEnd) {
 public ServiceTerm {
  if(end==null || graceEnd==null || graceEnd.isBefore(end))
   throw new IllegalArgumentException("INVALID_SERVICE_ENDPOINTS");
 }
 public enum State { NORMAL, GRACE, DISABLED }
 public State at(Instant now) {
  if(now==null) throw new IllegalArgumentException("INVALID_TIME");
  return now.isBefore(end)?State.NORMAL:now.isBefore(graceEnd)?State.GRACE:State.DISABLED;
 }
}
