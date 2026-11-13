package com.gym.modules.tradecore.persistence;
import javax.sql.DataSource;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.flywaydb.core.Flyway;
public final class TradeDatabase {
 private final DataSource ds;private final JdbcTemplate jdbc;private final TransactionTemplate tx;
 public TradeDatabase(String url,String user,String password){
  if(url==null||!url.matches("jdbc:mysql://localhost:3306/gym_dev_test_biz002(?:\\?.*)?"))throw new IllegalStateException("ISOLATED_DATABASE_REQUIRED");
  ds=new DriverManagerDataSource(url,user,password);jdbc=new JdbcTemplate(ds);
  tx=new TransactionTemplate(new DataSourceTransactionManager(ds));tx.setTimeout(10);
 }
 public DataSource dataSource(){return ds;}
 public JdbcTemplate jdbc(){return jdbc;}
 public TransactionTemplate transactions(){return tx;}
 public int migrate(){
  Set<String> known=Set.of("flyway_schema_history","trd_product","trd_order","trd_receipt","trd_money","trd_outbox","trd_reservation","trd_difference");
  try(var c=ds.getConnection()){
   if(!"gym_dev_test_biz002".equals(c.getCatalog()))throw new IllegalStateException("ISOLATED_DATABASE_REQUIRED");
   try(var rs=c.getMetaData().getTables(c.getCatalog(),null,"%",new String[]{"TABLE"})){while(rs.next())if(!known.contains(rs.getString("TABLE_NAME")))throw new IllegalStateException("UNKNOWN_DATABASE_OBJECT_REVIEW_REQUIRED");}
  }catch(java.sql.SQLException ex){throw new IllegalStateException("DATABASE_UNAVAILABLE");}
  var flyway=Flyway.configure().dataSource(ds).locations("classpath:db/modules/trade-core").cleanDisabled(true).baselineOnMigrate(false).validateOnMigrate(true).load();
  if(Arrays.stream(flyway.info().applied()).anyMatch(x->!"4".equals(x.getVersion().getVersion())))throw new IllegalStateException("UNKNOWN_MIGRATION_REVIEW_REQUIRED");
  return flyway.migrate().migrationsExecuted;
 }
}
