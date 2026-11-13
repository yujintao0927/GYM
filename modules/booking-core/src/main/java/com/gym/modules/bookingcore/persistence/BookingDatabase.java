package com.gym.modules.bookingcore.persistence;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
/** Explicit test-schema adapter. Does not auto-register with the host application. */
public final class BookingDatabase {
 private BookingDatabase() {}
 public static void validateUrl(String url) {
  if(!"jdbc:mysql://localhost:3306/gym_dev_test_biz003".equals(url))
   throw new IllegalArgumentException("ISOLATED_DATABASE_REQUIRED");
 }
 public static DataSource connect(String url,String username,String password) {
  validateUrl(url);
  return new DriverManagerDataSource(url,username,password);
 }
}
