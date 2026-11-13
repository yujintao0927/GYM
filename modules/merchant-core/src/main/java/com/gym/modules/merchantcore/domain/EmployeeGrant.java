package com.gym.modules.merchantcore.domain;
import java.util.Set;
public record EmployeeGrant(String accountId,String merchantId,Role role,Set<String> stores,boolean revoked,long version) {
 public EmployeeGrant {
  if(accountId==null || accountId.isBlank() || merchantId==null || merchantId.isBlank() || role==null || stores==null || stores.isEmpty() || version<0)
   throw new IllegalArgumentException("INVALID_GRANT");
  stores=Set.copyOf(stores);
  if(stores.stream().anyMatch(String::isBlank))throw new IllegalArgumentException("INVALID_GRANT");
 }
 public enum Role {OWNER, MANAGER, FRONT_DESK, COACH}
 public boolean includes(String storeId){return !revoked && stores.contains(storeId);}
 public EmployeeGrant revoke(){return new EmployeeGrant(accountId,merchantId,role,stores,true,Math.addExact(version,1));}
}
