package com.gym.modules.rankingcore;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.gym.modules.rankingcore.domain.RankingRules;
class RankingRulesTest {
 @Test void noPolicyCannotQualify(){assertEquals("PENDING_POLICY",new RankingRules().qualify(null,100));}
 @Test void explicitFixtureThresholdIsInclusive(){assertEquals("VALID",new RankingRules().qualify(100L,100));assertEquals("INVALID",new RankingRules().qualify(100L,99));}
 @Test void negativeDurationRejected(){assertThrows(IllegalArgumentException.class,()->new RankingRules().qualify(null,-1));}
}
