package com.gym.modules.rankingcore.ports;
/** Resolves a current authorized location reference. Never returns or persists coordinates. */
public interface RegionResolver  {
    record Region(String province,String city,String district) {
        public Region {
            for(String s:new String[] {
                province,city,district
            }
            )if(s==null||!s.matches("[A-Za-z0-9_-]{1,24}"))throw new IllegalArgumentException("Invalid region code");
        }
    }
    Region resolve(String actor,String authorizationReference);
}
