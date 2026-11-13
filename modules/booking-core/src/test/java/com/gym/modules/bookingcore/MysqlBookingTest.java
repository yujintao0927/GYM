package com.gym.modules.bookingcore;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import java.sql.*;
import static org.assertj.core.api.Assertions.*;
class MysqlBookingTest {
 static final String URL="jdbc:mysql://localhost:3306/gym_dev_test_biz003";
 @Test void migrationIsExplicitRepeatableAndPersistent() throws Exception {
  var user=System.getenv("GYM_DB_USERNAME"); var password=System.getenv("GYM_DB_PASSWORD");
  assertThat(user).as("explicit local database credentials required").isNotBlank();
  try(var c=DriverManager.getConnection(URL+"?createDatabaseIfNotExist=true",user,password)) {
   try(var r=c.getMetaData().getTables("gym_dev_test_biz003",null,"%",new String[]{"TABLE"})) {
    while(r.next()) {
     String table=r.getString("TABLE_NAME");
     assertThat(table).as("Unknown table must be investigated before migration").isIn("bkg_sessions","bkg_bookings","bkg_coach_locks","bkg_receipts","bkg_outbox","flyway_schema_history");
    }
   }
  }
  var f=Flyway.configure().dataSource(URL,user,password).locations("classpath:db/modules/booking-core").cleanDisabled(true).load();
  f.migrate(); assertThat(f.info().applied()).hasSize(1);
  assertThat(f.migrate().migrationsExecuted).isZero();
  try(var c=DriverManager.getConnection(URL,user,password);var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM bkg_sessions")) {
   assertThat(r.next()).isTrue(); assertThat(r.getLong(1)).isGreaterThanOrEqualTo(0);
  }  try(var c=DriverManager.getConnection(URL,user,password);var statement=c.createStatement()) {
   try(var charset=statement.executeQuery("SELECT DEFAULT_CHARACTER_SET_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME=DATABASE()")) {
    assertThat(charset.next()).isTrue();assertThat(charset.getString(1)).isEqualTo("utf8mb4");
   }
   try(var migration=statement.executeQuery("SELECT checksum FROM flyway_schema_history WHERE version='5' AND success=1")) {
    assertThat(migration.next()).isTrue();System.out.println("BOOKING_V5_FLYWAY_CHECKSUM="+migration.getInt(1));
   }
  }
 }
}
