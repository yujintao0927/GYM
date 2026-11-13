package com.gym.identity.persistence;

import java.util.*;
import java.util.function.Supplier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import com.gym.identity.api.SessionFailure;

@Repository @Profile("local-dev")
public class SessionRepository {
 public final JdbcTemplate db; private final TransactionTemplate tx;
 public SessionRepository(JdbcTemplate db,TransactionTemplate tx){this.db=db;this.tx=tx;}
 public Map<String,Object> row(String sql,Object... args){var list=db.queryForList(sql,args);return list.isEmpty()?null:list.getFirst();}
 public long number(Map<String,Object> row,String field){return ((Number)row.get(field)).longValue();}
 // Expected rejections commit defensive writes (failure counters / reuse revocation).
 // Infrastructure errors propagate and roll back the entire transaction.
 public <T> T transaction(Supplier<T> action){
  Object outcome=tx.execute(status->{try{return action.get();}catch(SessionFailure failure){return failure;}});
  if(outcome instanceof SessionFailure failure)throw failure;
  @SuppressWarnings("unchecked") T value=(T)outcome;return value;
 }
}
