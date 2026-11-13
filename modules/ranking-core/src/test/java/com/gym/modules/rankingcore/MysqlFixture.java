package com.gym.modules.rankingcore;
import java.sql.*;
import com.gym.modules.rankingcore.persistence.Database;
final class MysqlFixture {
 static Database open(){
  if(!"jdbc:mysql://localhost:3306/gym_dev_test_biz006".equals(System.getenv("RNK_DB_URL")))throw new IllegalArgumentException("BIZ006 test URL required");
  try(var c=DriverManager.getConnection("jdbc:mysql://localhost:3306/",System.getenv("GYM_DB_USERNAME"),System.getenv("GYM_DB_PASSWORD"));var s=c.createStatement()){
   s.executeUpdate("CREATE DATABASE IF NOT EXISTS gym_dev_test_biz006 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
  }catch(SQLException e){throw new IllegalStateException("BIZ006 test database unavailable");}
  return Database.open(System.getenv("RNK_DB_URL"),System.getenv("GYM_DB_USERNAME"),System.getenv("GYM_DB_PASSWORD"));
 }
}
