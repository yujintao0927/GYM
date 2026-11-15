package com.gym.modules.bookingcore.ports;
import java.util.List;
import com.gym.modules.bookingcore.api.BookingFailure;
/** Adapters must join the caller's local transaction. Network adapters under locks are forbidden. */
public interface BookingPorts {
 enum PolicyStatus {CONFIGURED_FIXTURE,APPROVED_RUNTIME,UNAPPROVED}
 default PolicyStatus getPolicy(String policyId,long version){throw unavailable();}
 enum Decision {ALLOW,DENY,UNAVAILABLE}
 record Store(String storeId,String merchantId,String status,long version){}
 record Purchase(String orderId,String ownerId,String storeId,String productId,String kind,String policyId,long policyVersion,boolean disclosedBeforePayment,boolean accepted,long cancellationMinor){}
 record CancellationOrder(String bookingId,String orderId,String beneficiary,String policyId,long policyVersion,long amountMinor,String mode,String businessKey){}
 default String createOrder(CancellationOrder command){throw unavailable();}
 record Coach(String accountId,String storeId,String merchantId,String status,long version){}
 default Coach requireCoach(String coachId,String storeId){throw unavailable();}
 record Right(String sourceId,long version,String orderId,String productId,String coachId){}
 record RightsResult(String reference,long version){}
 default Decision authorize(String actor,String action,String merchant,String store,String owner){return Decision.DENY;}
 default void requireActive(String actor){throw unavailable();}
 default Store requireStore(String store){throw unavailable();}
 default Purchase requirePurchase(String order,String owner,String store,String session,String coach,String courseKind,String policy,long version){throw unavailable();}
 default List<Right> resolve(String owner,String store,String capability){throw unavailable();}
 default RightsResult reserve(String source,long quantity,long expectedVersion,String businessKey){throw unavailable();}
 default RightsResult consume(String reservation,long quantity,long expectedVersion,String businessKey){throw unavailable();}
 default RightsResult release(String reservation,long quantity,long expectedVersion,String businessKey){throw unavailable();}
 static BookingFailure unavailable(){return new BookingFailure(503,"DEPENDENCY_UNAVAILABLE");}
}
