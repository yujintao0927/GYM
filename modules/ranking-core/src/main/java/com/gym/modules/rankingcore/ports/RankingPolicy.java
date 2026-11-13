package com.gym.modules.rankingcore.ports;
import java.time.ZoneId;
/** No default thresholds, period or small-population policy. Fixture provenance is explicit. */
public interface RankingPolicy  {
    enum Status  {
        CONFIGURED_FIXTURE,APPROVED_RUNTIME,UNAPPROVED
    }
    record Config(Status status,long thresholdMillis,ZoneId zone,int minimumParticipants) {
        public Config {
            if(status==null||thresholdMillis<0||zone==null||minimumParticipants<1)throw new IllegalArgumentException("Invalid explicit policy");
        }
    }
    enum TieMode  {
        COMPETITION,DENSE
    }
    Config get();
    default TieMode tieMode() {
        return null;
    }
    /** Window approval is independent from validity threshold approval. */
    default boolean approvesWindow(java.time.LocalDate from,java.time.LocalDate until) {
        return false;
    }
}
