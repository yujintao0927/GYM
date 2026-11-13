package com.gym.modules.supportcore.ports;
public final class SupportPorts {
 private SupportPorts() {}
 public enum Decision { ALLOW,DENY,UNAVAILABLE }
 public interface Authorization { Decision authorize(String actorId,String action,String merchantId,String storeId,String resourceOwnerId); }
 public interface Accounts { void requireActive(String accountId); }
 public record Store(String storeId,String merchantId,String status,long version) {}
 public interface Stores { Store requireStore(String storeId); }
 public record RefundFact(String reference,String ownerId,String storeId,String status,long version) {}
 public interface Trade { RefundFact refundStatus(String reference); }
 public interface ActionPolicy { boolean approved(String action); }
 public static Authorization denyAll() { return (a,b,c,d,e)->Decision.DENY; }
}
