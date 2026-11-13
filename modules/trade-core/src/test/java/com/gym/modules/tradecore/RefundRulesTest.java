package com.gym.modules.tradecore;
import com.gym.modules.tradecore.domain.RefundRules;
import java.math.BigInteger;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class RefundRulesTest {
 @Test void consumesAllUnmodified005VFixtures() throws Exception {
  String json=Files.readString(Path.of("../../docs/deliveries/TASK-005V/vectors.json"));
  var pattern=java.util.regex.Pattern.compile("\"paidMinor\": \"(\\d+)\",.*?\"totalUnits\": \"(\\d+)\",.*?\"remainingUnits\": \"(\\d+)\",.*?\"expectedRefundMinor\": \"(\\d+)\"",java.util.regex.Pattern.DOTALL);
  var m=pattern.matcher(json);int count=0;
  while(m.find()){assertThat(RefundRules.amount(new BigInteger(m.group(1)),new BigInteger(m.group(2)),new BigInteger(m.group(3)),BigInteger.ZERO)).isEqualTo(new BigInteger(m.group(4)));count++;}
  assertThat(count).isEqualTo(16);
 }
 @Test void rejectsInvalidAndAlreadyRefundedAmounts(){
  assertThatThrownBy(()->RefundRules.amount(BigInteger.TEN,BigInteger.ZERO,BigInteger.ONE,BigInteger.ZERO)).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->RefundRules.amount(BigInteger.TEN,BigInteger.ONE,BigInteger.TWO,BigInteger.ZERO)).isInstanceOf(IllegalArgumentException.class);
  assertThat(RefundRules.amount(BigInteger.TEN,BigInteger.TWO,BigInteger.ONE,BigInteger.valueOf(8))).isEqualTo(BigInteger.TWO);
 }
}
