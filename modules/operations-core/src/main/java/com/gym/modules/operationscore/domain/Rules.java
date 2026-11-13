package com.gym.modules.operationscore.domain;
import java.util.Set;
public final class Rules {
 private Rules(){}
 public static boolean transition(String from,String to){return switch(from){case "AVAILABLE" -> Set.of("FAULT","DISABLED").contains(to);case "FAULT" -> Set.of("MAINTENANCE","DISABLED").contains(to);case "MAINTENANCE" -> Set.of("AVAILABLE","DISABLED").contains(to);default -> false;};}
 public static long net(long received,long refunded){return Math.subtractExact(received,refunded);}
}
