import java.sql.*;
import org.flywaydb.core.Flyway;
public class VerifyFreshHealthMigration {
 public static void main(String[] args) throws Exception {
  String schema="gym_dev_test_coord_night001_20261116";
  String user=System.getenv("GYM_DB_USERNAME"),pass=System.getenv("GYM_DB_PASSWORD");
  try(var c=DriverManager.getConnection("jdbc:mysql://localhost:3306/",user,pass);var s=c.createStatement()){
   try(var rs=s.executeQuery("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='"+schema+"'")){rs.next();if(rs.getInt(1)!=0)throw new IllegalStateException("fresh schema already exists; no reuse or clean");}
   s.executeUpdate("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
  }
  var f=Flyway.configure().dataSource("jdbc:mysql://localhost:3306/"+schema,user,pass).locations("filesystem:"+args[0]).cleanDisabled(true).baselineOnMigrate(false).load();
  int first=f.migrate().migrationsExecuted;if(first!=2)throw new AssertionError("must migrate V1 and final V2");
  f.validate();if(f.migrate().migrationsExecuted!=0)throw new AssertionError("repeat migration changed");
  try(var c=DriverManager.getConnection("jdbc:mysql://localhost:3306/"+schema,user,pass);var s=c.createStatement();var rs=s.executeQuery("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='"+schema+"' AND table_name='hlt_record' AND data_type='decimal' AND extra LIKE '%STORED GENERATED%'")){rs.next();if(rs.getInt(1)!=6)throw new AssertionError("six generated decimals required");}
  for(var x:f.info().applied())System.out.println("VERSION="+x.getVersion()+" CHECKSUM="+x.getChecksum());
  System.out.println("JAVA="+System.getProperty("java.version")+" FRESH_MIGRATIONS=2 REPEAT=0 GENERATED_DECIMALS=6 SCHEMA="+schema);
 }
}
