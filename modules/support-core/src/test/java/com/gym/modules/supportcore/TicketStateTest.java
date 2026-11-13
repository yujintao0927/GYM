package com.gym.modules.supportcore;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.gym.modules.supportcore.domain.TicketState;
class TicketStateTest {
 @Test void appealRequiresDisposition() { assertThrows(IllegalStateException.class,()->TicketState.OPEN.next("APPEAL")); }
 @Test void escalationAndDispositionPreserveAppealRoute() {
  assertEquals(TicketState.ESCALATED,TicketState.OPEN.next("ESCALATE"));
  assertEquals(TicketState.APPEALED,TicketState.ESCALATED.next("DISPOSE").next("APPEAL"));
 }
}
