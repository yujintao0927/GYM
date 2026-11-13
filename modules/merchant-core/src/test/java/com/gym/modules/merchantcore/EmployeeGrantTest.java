package com.gym.modules.merchantcore;
import com.gym.modules.merchantcore.domain.EmployeeGrant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EmployeeGrantTest {
 @Test void explicitStoreSetIsImmutableAndNewStoresNeverExpandIt() {
  var stores=new HashSet<>(Set.of("s1"));
  var grant=new EmployeeGrant("staff","merchant",EmployeeGrant.Role.COACH,stores,false,1);
  stores.add("s2"); assertTrue(grant.includes("s1")); assertFalse(grant.includes("s2"));
  assertThrows(UnsupportedOperationException.class,()->grant.stores().add("s3"));
 }
 @Test void revokeImmediatelyDeniesWithoutMembershipDependency() {
  var grant=new EmployeeGrant("staff","merchant",EmployeeGrant.Role.FRONT_DESK,Set.of("s1"),false,1);
  var revoked=grant.revoke(); assertFalse(revoked.includes("s1")); assertEquals(2,revoked.version());
  assertTrue(grant.includes("s1"));
 }
 @Test void emptyScopeAndInvalidVersionRejected() {
  assertThrows(IllegalArgumentException.class,()->new EmployeeGrant("staff","m",EmployeeGrant.Role.OWNER,Set.of(),false,1));
  assertThrows(IllegalArgumentException.class,()->new EmployeeGrant("staff","m",EmployeeGrant.Role.OWNER,Set.of("s"),false,-1));
 }
}
