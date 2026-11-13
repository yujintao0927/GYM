package com.gym.modules.rankingcore.api;
import java.time.Instant;
public record Visit(String id,String subjectId,String storeId,String merchantId,Instant enteredAt,Instant leftAt,String source,String consumptionId,long version) {
}
