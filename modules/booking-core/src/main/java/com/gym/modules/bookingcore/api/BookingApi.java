package com.gym.modules.bookingcore.api;
import java.time.Instant;
public final class BookingApi {
 private BookingApi(){}
 public record SessionView(String id,String kind,int capacity,int remaining,long version,String coachId,Instant start,Instant end,String policyId,long policyVersion){}
 public record SessionPage(java.util.List<SessionView> items,String nextCursor){public SessionPage{items=java.util.List.copyOf(items);}}
 public record Context(String actorId,String requestId,String key,String mode){}
 public record Schedule(String storeId,String coachId,String kind,Instant start,Instant end,int capacity,String policyId,long policyVersion){}
 public record Book(String sessionId,String sourceId,long expectedRightsVersion,String orderId){}
 public record View(String id,String sessionId,String ownerId,String state,long version,String reconciliation){}
}
