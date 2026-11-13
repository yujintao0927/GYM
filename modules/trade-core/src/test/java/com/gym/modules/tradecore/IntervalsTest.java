package com.gym.modules.tradecore;
import com.gym.modules.tradecore.domain.LossIntervals;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class IntervalsTest {
 LossIntervals.Range r(long a,long b){return new LossIntervals.Range(Instant.ofEpochSecond(a),Instant.ofEpochSecond(b));}
 @Test void overlapWithHoldAndCompensatedSegmentsIsNeverCountedTwice(){
  var loss=LossIntervals.uncompensated(r(10,100),List.of(r(0,30),r(20,60),r(50,110)),List.of(r(15,25),r(40,50)));
  assertThat(loss).containsExactly(r(10,15),r(25,40),r(50,100));
  assertThat(LossIntervals.seconds(loss)).isEqualTo(70);
  assertThat(LossIntervals.uncompensated(r(10,100),loss,loss)).isEmpty();
 }
 @Test void emptyBoundaryAndReversedRange(){
  assertThat(LossIntervals.uncompensated(r(0,10),List.of(r(10,20)),List.of())).isEmpty();
  assertThatThrownBy(()->r(10,0)).isInstanceOf(IllegalArgumentException.class);
 }
}
