package com.gym.modules.operationscore.application;
import com.gym.modules.operationscore.api.*;
import com.gym.modules.operationscore.api.Models.*;
import com.gym.modules.operationscore.ports.Dependencies.*;
public final class Access {
 private final Authorization authorization; private final Stores stores;private final Accounts accounts;
 public Access(Authorization a,Stores s,Accounts c){authorization=a==null?(x,y,z,w,v)->Decision.DENY:a;stores=s;accounts=c;}
 public Store check(Context c,String action,String store,String owner){
  if(c==null||c.actorId()==null||c.actorId().isBlank())throw new Failure(401,"INVALID_SUBJECT");
  try{var account=accounts.requireActive(c.actorId());if(account==null||!c.actorId().equals(account.accountId()))throw new Failure(401,"INVALID_SUBJECT");
   Store scope=store==null?null:stores.requireStore(store);if(store!=null&&(scope==null||!store.equals(scope.storeId())))throw new Failure(404,"NOT_FOUND");
   require(authorization.authorize(c.actorId(),action,scope==null?null:scope.merchantId(),store,owner));return scope;
  }catch(Failure f){throw f;}catch(RuntimeException f){throw new Failure(503,"DEPENDENCY_UNAVAILABLE");}
 }
 public static void require(Decision d){if(d!=Decision.ALLOW)throw new Failure(d==Decision.DENY?403:503,d==Decision.DENY?"FORBIDDEN":"DEPENDENCY_UNAVAILABLE");}
}
