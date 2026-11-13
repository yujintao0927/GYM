package com.gym.modules.operationscore.persistence;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.flywaydb.core.Flyway;
public final class Database {
 private Database(){}
 public static DataSource migrate(String url,String user,String password)throws Exception{
  if(url==null||!url.matches("jdbc:mysql://localhost:3306/gym_dev_test_biz004(?:\\?.*)?"))throw new IllegalArgumentException("ISOLATED_DATABASE_REQUIRED");
  var ds=new DriverManagerDataSource(url,user,password);
  var allowed=Set.of("ops_equipment","ops_history","ops_report","ops_receipt","ops_outbox","ops_inbox","ops_fact","ops_contact_lock","ops_message","flyway_schema_history");
  try(var c=ds.getConnection()){
   if(!"gym_dev_test_biz004".equals(c.getCatalog()))throw new IllegalStateException("DATABASE_MISMATCH");
   try(var r=c.getMetaData().getTables(c.getCatalog(),null,"%",new String[]{"TABLE"})){while(r.next())if(!allowed.contains(r.getString("TABLE_NAME")))throw new IllegalStateException("UNKNOWN_TABLE_REVIEW_REQUIRED");}
  }
  var f=Flyway.configure().dataSource(ds).locations("classpath:db/modules/operations-core").cleanDisabled(true).baselineOnMigrate(false).load();
  if(java.util.Arrays.stream(f.info().applied()).anyMatch(m->!"6".equals(m.getVersion().toString())))throw new IllegalStateException("UNKNOWN_MIGRATION");
  f.migrate();return ds;
 }
}
