package com.gym.modules.operationscore.ports;
import java.time.*;
public final class Dependencies {
 private Dependencies(){}
 public enum Decision {ALLOW,DENY,UNAVAILABLE}
 public enum Approval {APPROVED_RUNTIME,CONFIGURED_FIXTURE,UNAPPROVED}
 public record Store(String storeId,String merchantId,String status,long version){}
 public record Account(String accountId,long epoch){}
 public interface Authorization {Decision authorize(String actorId,String action,String merchantId,String storeId,String resourceOwnerId);}
 public interface Stores {Store requireStore(String storeId);}
 public interface Accounts {Account requireActive(String accountId);}
 public interface Membership {Decision resolve(String accountId,String storeId,String capability);}
 /** Private references only; adapter must validate ownership and return no public URL. */
 public interface Images {Decision authorize(String actorId,String storeId,String imageRef);}
 public record ContactFact(String accountId,String storeId,long version,Instant expiresAt,Instant lastVisit,boolean consent,boolean unsubscribed){}
 public interface Facts {ContactFact require(String accountId,String storeId);}
 public record Policy(String id,long version,Approval approval,Duration expiryLead,Duration silence,Duration frequency){}
 public interface Policies {Policy get(String policyId,long version);}
 public record MetricPolicy(String id,long version,Approval approval){}
 public interface MetricPolicies {MetricPolicy get(String storeId);}
 public static Authorization deny(){return (a,b,c,d,e)->Decision.DENY;}
}
