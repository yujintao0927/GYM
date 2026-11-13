package com.gym.modules.merchantcore.persistence;
import java.util.Set;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
public final class MerchantDatabase {
 private MerchantDatabase(){}
 public static void validateUrl(String url) {
  if(url==null || !Set.of("jdbc:mysql://localhost:3306/gym_dev_test_biz001","jdbc:mysql://localhost:3306/gym_dev_test_biz001?createDatabaseIfNotExist=true").contains(url))
   throw new IllegalArgumentException("EXPLICIT_BIZ001_DATABASE_REQUIRED");
 }
 public static Flyway migrate(DataSource ds) throws java.sql.SQLException {
  var known=Set.of("flyway_schema_history","mch_mutex","mch_merchant","mch_store","mch_grant","mch_grant_store","mch_receipt","mch_audit","mch_outbox");
  try(var c=ds.getConnection()) {
   validateUrl(c.getMetaData().getURL());
   if(!"gym_dev_test_biz001".equals(c.getCatalog()))throw new IllegalArgumentException("DATABASE_MISMATCH");
   try(var r=c.getMetaData().getTables(c.getCatalog(),null,"%",new String[]{"TABLE"})){
    while(r.next())if(!known.contains(r.getString("TABLE_NAME")))throw new IllegalStateException("UNKNOWN_DATABASE_OBJECT_REVIEW_REQUIRED");
   }
  }
  var f=Flyway.configure().dataSource(ds).locations("classpath:db/modules/merchant-core").cleanDisabled(true).baselineOnMigrate(false).load();
  for(var migration:f.info().applied()) if(!migration.getVersion().toString().equals("3")) throw new IllegalStateException("UNKNOWN_MIGRATION_REVIEW_REQUIRED");
  f.migrate(); return f;
 }
}
