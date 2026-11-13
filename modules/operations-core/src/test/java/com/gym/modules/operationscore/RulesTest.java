package com.gym.modules.operationscore;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.gym.modules.operationscore.domain.Rules;
class RulesTest {
 @Test void repairMustPassThroughMaintenance(){assertTrue(Rules.transition("FAULT","MAINTENANCE"));assertTrue(Rules.transition("MAINTENANCE","AVAILABLE"));assertFalse(Rules.transition("FAULT","AVAILABLE"));assertFalse(Rules.transition("DISABLED","AVAILABLE"));}
 @Test void exactNetAndOverflow(){assertEquals(7000,Rules.net(10000,3000));assertThrows(ArithmeticException.class,()->Rules.net(Long.MIN_VALUE,1));}
}
