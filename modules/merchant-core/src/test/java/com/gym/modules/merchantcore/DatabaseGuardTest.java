package com.gym.modules.merchantcore;
import com.gym.modules.merchantcore.persistence.MerchantDatabase;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DatabaseGuardTest {
 @Test void rejectsOtherSchemasBeforeOpeningConnection(){
  assertThrows(IllegalArgumentException.class,()->MerchantDatabase.validateUrl("jdbc:mysql://localhost:3306/gym_dev"));
  assertThrows(IllegalArgumentException.class,()->MerchantDatabase.validateUrl("jdbc:mysql://localhost:3306/gym_dev_test_biz002"));
  assertThrows(IllegalArgumentException.class,()->MerchantDatabase.validateUrl("jdbc:mysql://elsewhere:3306/gym_dev_test_biz001"));
  assertDoesNotThrow(()->MerchantDatabase.validateUrl("jdbc:mysql://localhost:3306/gym_dev_test_biz001"));
 }
}
