package com.gym.modules.merchantcore.persistence;
import com.gym.modules.merchantcore.api.*;
import com.gym.modules.merchantcore.api.MerchantApi.*;
import com.gym.modules.merchantcore.domain.EmployeeGrant.Role;
import java.time.Instant; import java.util.*; import java.util.function.Supplier; import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
/** Internal adapter. Do not expose this class as a cross-module write port. */
public final class MerchantRepository {
 private final JdbcTemplate db; private final TransactionTemplate tx;
 public MerchantRepository(DataSource ds){db=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));tx.setTimeout(10);}
 public <T>T transaction(Supplier<T> work){try{return tx.execute(s->{db.queryForObject("SELECT id FROM mch_mutex WHERE id=1 FOR UPDATE",Integer.class);return work.get();});}catch(org.springframework.dao.CannotAcquireLockException e){throw new MerchantFailure(409,"CONCURRENT_CONFLICT");}catch(org.springframework.dao.DataAccessException | org.springframework.transaction.TransactionException e){throw new MerchantFailure(503,"PERSISTENCE_UNAVAILABLE");}}
 public int update(String sql,Object...args){return db.update(sql,args);}
 public List<Map<String,Object>> rows(String sql,Object...args){return db.queryForList(sql,args);}
 public Map<String,Object> row(String sql,Object...args){var rows=rows(sql,args);if(rows.size()!=1)throw new MerchantFailure(404,"RESOURCE_NOT_FOUND");return rows.getFirst();}
 public Merchant merchant(String id){var r=row("SELECT * FROM mch_merchant WHERE id=?",id);return new Merchant(id,(String)r.get("applicant"),(String)r.get("name"),(String)r.get("evidence_ref"),(String)r.get("review_state"),n(r,"version"));}
 public Store store(String id){var s=storeIncludingDeleted(id);if(s.deleted())throw new MerchantFailure(404,"RESOURCE_NOT_FOUND");return s;}
 public Store storeIncludingDeleted(String id){var r=row("SELECT * FROM mch_store WHERE id=?",id);return new Store(id,(String)r.get("merchant_id"),(String)r.get("name"),(Boolean)r.get("deleted"),time(r,"end_at"),time(r,"grace_end"),n(r,"version"));}
 public Grant grant(String id){var r=row("SELECT * FROM mch_grant WHERE id=?",id);var stores=new TreeSet<String>(); for(var s:rows("SELECT store_id FROM mch_grant_store WHERE grant_id=?",id))stores.add((String)s.get("store_id"));return new Grant(id,(String)r.get("merchant_id"),(String)r.get("account_id"),Role.valueOf((String)r.get("role_name")),stores,((Boolean)r.get("revoked")),n(r,"version"));}
 public static long n(Map<String,Object> r,String key){return ((Number)r.get(key)).longValue();}
 private static Instant time(Map<String,Object> r,String key){return r.get(key)==null?null:Instant.parse((String)r.get(key));}
}
