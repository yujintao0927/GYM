package com.gym.modules.bookingcore;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.gym.modules.bookingcore.persistence.BookingDatabase;
class DatabaseGuardTest {
 @Test void rejectsEveryNonTaskDatabaseBeforeConnecting() {
  for(var u:new String[]{"jdbc:mysql://localhost:3306/gym_dev","jdbc:mysql://localhost:3306/gym_dev_test_biz003?allowMultiQueries=true","jdbc:mysql://remote:3306/gym_dev_test_biz003", "jdbc:mysql://localhost:3306/gym_dev_test_003b"})
   assertThatThrownBy(()->BookingDatabase.validateUrl(u)).isInstanceOf(IllegalArgumentException.class);
  assertThatCode(()->BookingDatabase.validateUrl("jdbc:mysql://localhost:3306/gym_dev_test_biz003")).doesNotThrowAnyException();
 }
}
