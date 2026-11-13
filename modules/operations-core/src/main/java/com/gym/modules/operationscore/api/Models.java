package com.gym.modules.operationscore.api;
import java.time.Instant;
public final class Models {
 private Models(){}
 public record Context(String actorId,String requestId,String idempotencyKey){}
 public record Equipment(String id,String storeId,String name,String state,long version){}
 public record Event(String eventId,String type,int schemaVersion,String producer,String aggregateId,long version,Instant occurredAt,String merchantId,String storeId,String mode,String accountId,long amountMinor,boolean present,boolean reliable){}
 public record Metrics(long receivedMinor,long refundedMinor,long netMinor,long newMembers,long renewals,long totalMembers,long todayVisitors,long presentCount,boolean estimated,String mode){}
 public record Cursor(String actorId,String storeId,String filter,String after){}
 public record Message(String id,String kind,Instant createdAt){}
}
