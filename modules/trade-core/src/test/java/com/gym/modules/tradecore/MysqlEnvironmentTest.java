package com.gym.modules.tradecore;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class MysqlEnvironmentTest {
 @Test void isolatedDatabaseMustHaveNoUnknownObjects() throws Exception {
  String url="jdbc:mysql://localhost:3306/gym_dev_test_biz002?allowPublicKeyRetrieval=true&useSSL=false";
  String user=System.getenv("GYM_DB_USERNAME"), password=System.getenv("GYM_DB_PASSWORD");
  assertThat(user).as("Explicit local database username required").isNotBlank();
  assertThat(password).as("Explicit local database secret required").isNotBlank();
  try(var server=DriverManager.getConnection("jdbc:mysql://localhost:3306/?allowPublicKeyRetrieval=true&useSSL=false",user,password);var st=server.createStatement()) {
   st.execute("CREATE DATABASE IF NOT EXISTS gym_dev_test_biz002 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
  }
  try(var c=DriverManager.getConnection(url,user,password)) {
   assertThat(c.getCatalog()).isEqualTo("gym_dev_test_biz002");
   List<String> tables=new ArrayList<>();
   try(var rs=c.getMetaData().getTables(c.getCatalog(),null,"%",new String[]{"TABLE"})) {while(rs.next())tables.add(rs.getString("TABLE_NAME"));}
   assertThat(tables).as("Unknown existing objects require coordinator review; no automatic cleanup").allMatch(t->t.equals("flyway_schema_history")||java.util.Set.of("trd_order","trd_product","trd_receipt","trd_money","trd_outbox","trd_reservation","trd_difference").contains(t));
   try(var st=c.createStatement(); var rs=st.executeQuery("SELECT VERSION()")){assertThat(rs.next()).isTrue();System.out.println("MYSQL_VERSION="+rs.getString(1));}
  }
 }
}
