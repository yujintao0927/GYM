package com.gym.modules.tradecore.ports;
/** In-process ports only: adapters must not call a network while aggregate locks are held. */
public interface TradePorts {
 enum Decision {ALLOW,DENY,UNAVAILABLE}
 enum Approval {CONFIGURED_FIXTURE,APPROVED_RUNTIME,UNAPPROVED}
 record Account(String accountId,long epoch){}
 record Store(String storeId,String merchantId,String status,long version){}
 enum Purpose {CARD_PRODUCT,PERSONAL_PRICE,COACHING_PRICE,NORMALIZED_COUNT,SIMULATED_ACTIVATION}
 record Policy(String policyId,String version,Approval approval,java.util.Set<Purpose> purposes){
  public Policy{purposes=purposes==null?java.util.Set.of():java.util.Set.copyOf(purposes);}
  public Policy(String id,String version,Approval approval){this(id,version,approval,java.util.Set.of());}
 }
 default boolean supportsSameInstructionRetry(){return false;}
 default Decision authorize(String actor,String action,String merchant,String store,String owner){return Decision.DENY;}
 default Account requireActive(String accountId){throw new IllegalStateException("DEPENDENCY_UNAVAILABLE");}
 default Store requireStore(String storeId){throw new IllegalStateException("DEPENDENCY_UNAVAILABLE");}
 default Policy policy(String id,String version){return new Policy(id,version,Approval.UNAPPROVED);}
}
