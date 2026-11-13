package com.gym.identity.local;

import java.time.Clock;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import com.gym.identity.api.SessionFailure;

@Service @Profile("local-dev")
public class LocalAccountProvisioner {
 private final JdbcTemplate db;private final TransactionTemplate tx;private final LocalCredentialAuthenticator crypto;private final Clock clock;
 public LocalAccountProvisioner(JdbcTemplate db,TransactionTemplate tx,LocalCredentialAuthenticator crypto,Clock clock){this.db=db;this.tx=tx;this.crypto=crypto;this.clock=clock;}
 public String create(String login,char[] password){
  try{
   String normalized=crypto.login(login),hash=crypto.encode(password),id=UUID.randomUUID().toString();
   return tx.execute(status->{
    if(!db.queryForList("select account_id from iam_local_credential where login_normalized=?",normalized).isEmpty())throw new SessionFailure(409,"LOCAL_ACCOUNT_EXISTS");
    db.update("insert into iam_account(id,status,created_at) values(?,'ACTIVE',?)",id,clock.millis());
    db.update("insert into iam_local_credential(account_id,login_normalized,password_hash) values(?,?,?)",id,normalized,hash);return id;
   });
  }finally{Arrays.fill(password,'\0');}
 }
}
