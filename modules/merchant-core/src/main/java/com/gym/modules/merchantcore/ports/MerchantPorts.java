package com.gym.modules.merchantcore.ports;
import com.gym.modules.merchantcore.api.MerchantApi.Grant;
public final class MerchantPorts {
 private MerchantPorts(){}
 public enum Decision { ALLOW,DENY,UNAVAILABLE }
 public interface AuthorizationPort {Decision authorize(String actorId,String action,String merchantId,String storeId,String resourceOwnerId);}
 public record StoreSnapshot(String storeId,String merchantId,String status,long version){}
 public interface StoreDirectory {StoreSnapshot requireStore(String storeId);}
 public interface AccountDirectory {ActiveAccount requireActive(String accountId);}
 public record ActiveAccount(String accountId,long epoch){}
 public enum PolicyStatus { CONFIGURED_FIXTURE,APPROVED_RUNTIME,UNAPPROVED }
 public record PolicyMetadata(String id,long version,PolicyStatus status){}
 public interface GrantPolicy {default PolicyMetadata metadata(){return new PolicyMetadata("unapproved",0,PolicyStatus.UNAPPROVED);}Decision mayGrant(String actorId,Grant proposed);}
 public interface PlatformPermissionSource {Decision authorize(String actorId,String action);}
 public static AuthorizationPort denyAll(){return (a,b,c,d,e)->Decision.DENY;}
 public static GrantPolicy unapprovedGrantPolicy(){return new GrantPolicy(){public Decision mayGrant(String a,Grant g){return Decision.DENY;}};}
}



