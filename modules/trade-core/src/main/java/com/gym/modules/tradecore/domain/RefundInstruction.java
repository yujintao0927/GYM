package com.gym.modules.tradecore.domain;
/** Immutable instruction facts. Caller must persist transitions under the aggregate lock. */
public record RefundInstruction(String instructionId, long amountMinor, State state) {
 public enum State {APPROVED_NOT_SENT,SUBMITTING,PROCESSING,FAILED_CONFIRMED,UNKNOWN,MANUAL_REVIEW,SUCCEEDED}
 public RefundInstruction {
  if(instructionId==null||instructionId.isBlank()||amountMinor<0||state==null)throw new IllegalArgumentException("INVALID_INSTRUCTION");
 }
 public static RefundInstruction approve(String id,long amount){return new RefundInstruction(id,amount,State.APPROVED_NOT_SENT);}
 public RefundInstruction dispatch(){
  if(state!=State.APPROVED_NOT_SENT)throw new IllegalStateException("QUERY_OR_MANUAL_REVIEW_REQUIRED");
  return with(State.SUBMITTING);
 }
 public RefundInstruction retry(boolean verifiedSameIdRetry){
  if(state!=State.FAILED_CONFIRMED&&state!=State.MANUAL_REVIEW)throw new IllegalStateException("QUERY_OR_MANUAL_REVIEW_REQUIRED");
  // A manual case preserving UNKNOWN cannot use this shortcut: only FAILED_CONFIRMED is eligible.
  if(!verifiedSameIdRetry||state!=State.FAILED_CONFIRMED)throw new IllegalStateException("SAFE_RETRY_UNAVAILABLE");
  return with(State.SUBMITTING);
 }
 public RefundInstruction escalate(){
  if(state==State.SUCCEEDED||state==State.APPROVED_NOT_SENT)throw new IllegalStateException("INVALID_TRANSITION");
  return with(State.MANUAL_REVIEW);
 }
 public RefundInstruction observe(State fact,long amount,String currency,String mode){
  if(amount!=amountMinor||!"CNY".equals(currency)||!"SIMULATED".equals(mode))throw new IllegalArgumentException("CALLBACK_MISMATCH");
  if(fact!=State.PROCESSING&&fact!=State.FAILED_CONFIRMED&&fact!=State.UNKNOWN&&fact!=State.SUCCEEDED)throw new IllegalArgumentException("INVALID_CHANNEL_FACT");
  if(state==State.APPROVED_NOT_SENT)throw new IllegalStateException("UNSENT_CALLBACK_QUARANTINE_REQUIRED");
  if(state==State.SUCCEEDED)return this;
  // Unknown/failed facts never release hold; late trusted success may converge from any sent state.
  return with(fact);
 }
 private RefundInstruction with(State next){return new RefundInstruction(instructionId,amountMinor,next);}
}
