package com.gym.modules.bookingcore.domain;
import java.time.Instant;
import java.util.Objects;
/** Pure rules; does not authorize a caller or write money/rights. */
public final class BookingRules {
 private BookingRules() {}
 public static boolean overlaps(Instant start, Instant end, Instant otherStart, Instant otherEnd) {
  Objects.requireNonNull(start); Objects.requireNonNull(end);
  Objects.requireNonNull(otherStart); Objects.requireNonNull(otherEnd);
  if (!start.isBefore(end) || !otherStart.isBefore(otherEnd)) throw new IllegalArgumentException("INVALID_INTERVAL");
  return start.isBefore(otherEnd) && otherStart.isBefore(end);
 }
 public static long cancellationCharge(long acceptedFeeMinor, boolean disclosedBeforePayment, boolean accepted) {
  if(acceptedFeeMinor<0) throw new IllegalArgumentException("INVALID_AMOUNT");
  return disclosedBeforePayment && accepted ? acceptedFeeMinor : 0;
 }
 public static String transition(String state,String command) {
  if(!"BOOKED".equals(state)) throw new IllegalStateException("BOOKING_TERMINAL");
  return switch(command) {
   case "CHECK_IN" -> "FULFILLED";
   case "CANCEL" -> "CANCELLED";
   default -> throw new IllegalArgumentException("INVALID_COMMAND");
  };
 }
}
