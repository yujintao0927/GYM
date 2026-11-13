package com.gym.identity.persistence;

import java.time.Clock;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods=false)
@Profile("local-dev")
public class LocalIdentityConfiguration {
 @Bean DataSource identityDataSource(@Value("${gym.db.url}") String url,
  @Value("${gym.db.username}") String user,@Value("${gym.db.password}") String password,
  @Value("${gym.db.schema}") String schema) throws Exception {
  if(!schema.equals("gym_dev")&&!schema.equals("gym_dev_test_003b"))throw new IllegalStateException("Unapproved identity schema");
  if(!url.matches("jdbc:mysql://localhost:3306/"+schema+"(?:\\?.*)?"))throw new IllegalStateException("Explicit local schema URL required");
  var ds=new DriverManagerDataSource(url,user,password);
  try(var c=ds.getConnection()){
   if(!schema.equals(c.getCatalog()))throw new IllegalStateException("Schema mismatch");
   try(var r=c.getMetaData().getTables(schema,null,"%",new String[]{"TABLE"})){
    while(r.next())if(!java.util.Set.of("flyway_schema_history","iam_account","iam_local_credential","iam_session","iam_refresh_family","iam_refresh_node","iam_login_receipt","iam_auth_failure").contains(r.getString("TABLE_NAME")))throw new IllegalStateException("Unknown table in identity schema; coordinator review required");
   }
  }
  Flyway.configure().dataSource(ds).cleanDisabled(true).baselineOnMigrate(false).load().migrate();
  return ds;
 }
 @Bean JdbcTemplate identityJdbc(DataSource ds){return new JdbcTemplate(ds);}
 @Bean TransactionTemplate identityTransactions(DataSource ds){return new TransactionTemplate(new DataSourceTransactionManager(ds));}
 @Bean Clock identityClock(){return Clock.systemUTC();}
}
