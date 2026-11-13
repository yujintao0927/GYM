package com.gym.modules.rankingcore.domain;
public final class RankingRules  {
    public String qualify(Long thresholdMillis,long durationMillis) {
        if(durationMillis<0)throw new IllegalArgumentException("Invalid duration");
        if(thresholdMillis==null)return "PENDING_POLICY";
        if(thresholdMillis<0)throw new IllegalArgumentException("Invalid threshold");
        return durationMillis>=thresholdMillis?"VALID":"INVALID";
    }
}
