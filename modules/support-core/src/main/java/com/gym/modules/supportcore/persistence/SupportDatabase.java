package com.gym.modules.supportcore.persistence;
import javax.sql.DataSource;
import java.util.Set;
import org.flywaydb.core.Flyway;
public final class SupportDatabase {
 private SupportDatabase() {}
 public static void migrate(DataSource ds) throws Exception {
  try(var c=ds.getConnection()) {
   if(!"gym_dev_test_biz005".equals(c.getCatalog()) || !c.getMetaData().getURL().matches("jdbc:mysql://localhost:3306/gym_dev_test_biz005(?:\\?.*)?")) throw new IllegalStateException("DATABASE_NOT_ALLOWED");
   var allowed=Set.of("flyway_schema_history","sup_ticket","sup_entry","sup_receipt","sup_attachment","sup_outbox");
   try(var rs=c.getMetaData().getTables(c.getCatalog(),null,"%",new String[]{"TABLE"})) {while(rs.next())if(!allowed.contains(rs.getString("TABLE_NAME")))throw new IllegalStateException("UNKNOWN_DATABASE_OBJECT");}
  }
  var f=Flyway.configure().dataSource(ds).locations("classpath:db/modules/support-core").cleanDisabled(true).baselineOnMigrate(false).load();
  for(var m:f.info().applied())if(!"7".equals(m.getVersion().toString()))throw new IllegalStateException("UNKNOWN_MIGRATION");
  f.migrate();
 }
}
