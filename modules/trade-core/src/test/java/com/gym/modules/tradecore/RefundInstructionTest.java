package com.gym.modules.tradecore;
import com.gym.modules.tradecore.domain.RefundInstruction;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class RefundInstructionTest {
 @Test void unknownCannotBeResentAndLateSuccessIsTerminal() {
  var i=RefundInstruction.approve("original",100);
  i=i.dispatch();i=i.observe(RefundInstruction.State.UNKNOWN,100,"CNY","SIMULATED");
  var unknown=i;
  assertThatThrownBy(unknown::dispatch).hasMessage("QUERY_OR_MANUAL_REVIEW_REQUIRED");
  i=i.escalate();i=i.observe(RefundInstruction.State.SUCCEEDED,100,"CNY","SIMULATED");
  assertThat(i.state()).isEqualTo(RefundInstruction.State.SUCCEEDED);
  assertThat(i.instructionId()).isEqualTo("original");
  assertThat(i.observe(RefundInstruction.State.FAILED_CONFIRMED,100,"CNY","SIMULATED").state()).isEqualTo(RefundInstruction.State.SUCCEEDED);
  assertThatThrownBy(i::dispatch).hasMessage("QUERY_OR_MANUAL_REVIEW_REQUIRED");
 }
 @Test void mismatchedFactCannotChangeInstruction() {
  var i=RefundInstruction.approve("original",100).dispatch();
  assertThatThrownBy(()->i.observe(RefundInstruction.State.SUCCEEDED,101,"CNY","SIMULATED")).hasMessage("CALLBACK_MISMATCH");
  assertThatThrownBy(()->i.observe(RefundInstruction.State.SUCCEEDED,100,"CNY","LIVE_VERIFIED")).hasMessage("CALLBACK_MISMATCH");
  assertThat(i.state()).isEqualTo(RefundInstruction.State.SUBMITTING);
 }
 @Test void failedRetryMustExplicitlyProveSameIdCapability() {
  var i=RefundInstruction.approve("original",100).dispatch().observe(RefundInstruction.State.FAILED_CONFIRMED,100,"CNY","SIMULATED");
  assertThatThrownBy(()->i.retry(false)).hasMessage("SAFE_RETRY_UNAVAILABLE");
  assertThat(i.retry(true).instructionId()).isEqualTo("original");
 }
}
