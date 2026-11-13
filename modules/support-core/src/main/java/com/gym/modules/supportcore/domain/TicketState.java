package com.gym.modules.supportcore.domain;
public enum TicketState {
 OPEN, ASSIGNED, ESCALATED, DISPOSED, APPEALED;
 public TicketState next(String action) {
  return switch(action) {
   case "ASSIGN" -> { if(this==DISPOSED) throw new IllegalStateException("INVALID_STATE"); yield ASSIGNED; }
   case "REPLY", "EVIDENCE", "PROPOSE" -> this;
   case "ESCALATE" -> { if(this==DISPOSED) throw new IllegalStateException("INVALID_STATE"); yield ESCALATED; }
   case "DISPOSE" -> { if(this==DISPOSED) throw new IllegalStateException("INVALID_STATE"); yield DISPOSED; }
   case "APPEAL" -> { if(this!=DISPOSED) throw new IllegalStateException("INVALID_STATE"); yield APPEALED; }
   default -> throw new IllegalArgumentException("INVALID_ACTION");
  };
 }
}
