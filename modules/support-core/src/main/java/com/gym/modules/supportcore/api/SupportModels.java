package com.gym.modules.supportcore.api;
public final class SupportModels {
 private SupportModels() {}
 public record Context(String actorId,String requestId,String idempotencyKey) {}
 public record Open(String storeId,String kind,String text,String refundReference) {}
 public record Change(String ticketId,long expectedVersion,String action,String text,String assigneeId) {}
 public record Ticket(String id,String ownerId,String merchantId,String storeId,String kind,String state,String assigneeId,long version) {}
 public record Entry(long sequence,String action,String actorId,String text,String requestId) {}
 public record Attachment(String id,String ticketId,String mediaType,long size) {}
}
