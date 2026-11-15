package com.gym.health;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.gym.identity.api.SessionFailure;
import com.gym.identity.session.SessionService;
import static org.assertj.core.api.Assertions.*;
class HealthValidationTest {
 @Test void identityReaderRequiresActiveTransactionBeforeTouchingDependencies(){
  var auth=new SessionService(null,null,Clock.systemUTC());
  assertThatThrownBy(()->auth.authenticatedHealthActor("Bearer synthetic")).isInstanceOf(IllegalStateException.class).hasMessage("Transaction required");
 }
 @Test void callerCannotAddOwnershipOrRoleFields(){
  for(String extra:List.of("accountId","ownerId","role","merchantId","storeId")) {
   var body=new HashMap<String,Object>();body.put("payload",Map.of());body.put(extra,"forged");
   assertThatThrownBy(()->HealthService.fields(body,Set.of("payload"),Set.of("payload"))).isInstanceOf(SessionFailure.class);
  }
 }
 @Test void requiredFieldCannotBeOmitted(){assertThatThrownBy(()->HealthService.fields(Map.of(),Set.of("payload"),Set.of("payload"))).isInstanceOf(SessionFailure.class);}
}
