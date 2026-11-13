package com.gym.modules.rankingcore.ports;
import java.sql.Connection;
import java.time.Instant;
import com.gym.modules.rankingcore.api.Failure;
/** Adapters must join the supplied connection; no network calls while locks are held. */
public interface Dependencies  {
    enum Decision  {
        ALLOW,DENY,UNAVAILABLE
    }
    record Store(String id,String merchantId,long version) {
    }
    record Right(String id,long version) {
    }
    default String username(String account) {
        throw new Failure(503,"ACCOUNT_UNAVAILABLE");
    }
    default void active(String account) {
        throw new Failure(503,"ACCOUNT_UNAVAILABLE");
    }
    default Store store(String id) {
        throw new Failure(503,"STORE_UNAVAILABLE");
    }
    default Decision authorize(String actor,String action,Store store,String owner) {
        return Decision.DENY;
    }
    default void requireSimulatedDevice(String owner,String device) {
        throw new Failure(503,"DEVICE_BINDING_UNAVAILABLE");
    }
    default Right right(String owner,String store,String capability) {
        throw new Failure(503,"RIGHTS_UNAVAILABLE");
    }
    /** Resolve ownership/store/hold, activate if approved, consume exactly one under businessKey. */
    default String consume(Connection transaction,Right right,String owner,String store,String businessKey,Instant at) {
        throw new Failure(503,"RIGHTS_UNAVAILABLE");
    }
}
