package com.gym.modules.bookingcore;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import com.gym.modules.bookingcore.domain.BookingRules;
class BookingRulesTest {
 @Test void adjacentSlotsDoNotOverlapButIntersectionDoes() {
  var t=Instant.parse("2026-11-13T08:00:00Z");
  assertThat(BookingRules.overlaps(t,t.plusSeconds(60),t.plusSeconds(60),t.plusSeconds(120))).isFalse();
  assertThat(BookingRules.overlaps(t,t.plusSeconds(61),t.plusSeconds(60),t.plusSeconds(120))).isTrue();
 }
 @Test void undisclosedCancellationCannotCharge() {
  assertThat(BookingRules.cancellationCharge(100,false,true)).isZero();
  assertThat(BookingRules.cancellationCharge(100,true,false)).isZero();
  assertThat(BookingRules.cancellationCharge(100,true,true)).isEqualTo(100);
 }
 @Test void fulfilmentAndCancellationAreExclusive() {
  assertThat(BookingRules.transition("BOOKED","CHECK_IN")).isEqualTo("FULFILLED");
  assertThat(BookingRules.transition("BOOKED","CANCEL")).isEqualTo("CANCELLED");
  assertThatThrownBy(()->BookingRules.transition("CANCELLED","CHECK_IN")).isInstanceOf(IllegalStateException.class);
  assertThatThrownBy(()->BookingRules.transition("FULFILLED","CANCEL")).isInstanceOf(IllegalStateException.class);
 }
 @Test void invalidRangesAndNegativeFeesReject() {
  var t=Instant.EPOCH;
  assertThatThrownBy(()->BookingRules.overlaps(t,t,t,t.plusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->BookingRules.cancellationCharge(-1,true,true)).isInstanceOf(IllegalArgumentException.class);
 }
}
