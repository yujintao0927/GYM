package com.gym.modules.merchantcore;
import com.gym.modules.merchantcore.domain.ServiceTerm;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ServiceTermTest {
 @Test void explicitEndpointsKeepStoresIndependent() {
  var end=Instant.parse("2026-11-13T00:00:00Z"); var grace=end.plusSeconds(123);
  var a=new ServiceTerm(end,grace); var b=new ServiceTerm(grace,grace.plusSeconds(123));
  assertEquals(ServiceTerm.State.NORMAL,a.at(end.minusNanos(1)));
  assertEquals(ServiceTerm.State.GRACE,a.at(end));
  assertEquals(ServiceTerm.State.GRACE,a.at(grace.minusNanos(1)));
  assertEquals(ServiceTerm.State.DISABLED,a.at(grace));
  assertEquals(ServiceTerm.State.GRACE,b.at(grace));
 }
 @Test void reversedAndMissingEndpointsRejected() {
  var t=Instant.EPOCH;
  assertThrows(IllegalArgumentException.class,()->new ServiceTerm(t,t.minusSeconds(1)));
  assertThrows(IllegalArgumentException.class,()->new ServiceTerm(null,t));
 }
}
