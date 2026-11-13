package com.gym.modules.merchantcore.api;
import java.time.Instant;
import java.util.Set;
import com.gym.modules.merchantcore.domain.EmployeeGrant.Role;
public final class MerchantApi {
 private MerchantApi(){}
 public record StorePage(java.util.List<Store> items,String nextCursor){public StorePage{items=java.util.List.copyOf(items);}}
 public record Context(String actorId,String requestId,String key){}
 public record Result(String id,long version){}
 public record Merchant(String id,String applicant,String name,String evidenceRef,String state,long version){}
 public record Store(String id,String merchantId,String name,boolean deleted,Instant end,Instant graceEnd,long version){}
 public record Grant(String id,String merchantId,String accountId,Role role,Set<String> stores,boolean revoked,long version){public Grant{stores=Set.copyOf(stores);}}
}

