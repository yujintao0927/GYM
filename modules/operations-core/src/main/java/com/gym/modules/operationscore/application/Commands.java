package com.gym.modules.operationscore.application;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.function.Supplier;
import com.gym.modules.operationscore.api.*;
import com.gym.modules.operationscore.api.Models.*;
import com.gym.modules.operationscore.persistence.Repository;
final class Commands {
 static String hash(Object... values){try{var d=MessageDigest.getInstance("SHA-256");for(Object v:values){byte[] b=String.valueOf(v).getBytes(StandardCharsets.UTF_8);d.update(java.nio.ByteBuffer.allocate(4).putInt(b.length).array());d.update(b);}return HexFormat.of().formatHex(d.digest());}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 static String run(Repository r,Context c,String fingerprint,Supplier<String> work){
  if(c.idempotencyKey()==null||c.idempotencyKey().isBlank()||c.idempotencyKey().length()>200)throw new Failure(422,"INVALID_KEY");
  var old=r.row("select * from ops_receipt where actor_id=? and key_hash=?",c.actorId(),hash(c.idempotencyKey()));
  if(old!=null){if(!fingerprint.equals(old.get("fingerprint")))throw new Failure(409,"IDEMPOTENCY_CONFLICT");return "-".equals(old.get("result_id"))?null:(String)old.get("result_id");}
  String result=work.get();r.db.update("insert into ops_receipt values(?,?,?,?)",c.actorId(),hash(c.idempotencyKey()),fingerprint,result==null?"-":result);return result;
 }
 static void text(String s,int max){if(s==null||s.isBlank()||s.length()>max)throw new Failure(422,"INVALID_INPUT");}
}
