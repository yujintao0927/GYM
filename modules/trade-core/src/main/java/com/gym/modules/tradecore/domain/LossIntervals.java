package com.gym.modules.tradecore.domain;
import java.time.*;
import java.util.*;
/** Half-open ranges: union loss (including hold), intersect availability, subtract already allocated loss. */
public final class LossIntervals {
 private LossIntervals(){}
 public record Range(Instant start,Instant end){public Range{if(start==null||end==null||!start.isBefore(end))throw new IllegalArgumentException("INVALID_INTERVAL");}}
 private static List<Range> union(List<Range> input){
  var sorted=new ArrayList<>(input);sorted.sort(Comparator.comparing(Range::start));var result=new ArrayList<Range>();
  for(var r:sorted){if(result.isEmpty()||result.getLast().end().isBefore(r.start()))result.add(r);
   else{var p=result.removeLast();result.add(new Range(p.start(),p.end().isAfter(r.end())?p.end():r.end()));}}
  return result;
 }
 public static List<Range> uncompensated(Range available,List<Range> loss,List<Range> allocated){
  List<Range> pieces=new ArrayList<>();
  for(var r:union(loss)){var start=r.start().isAfter(available.start())?r.start():available.start();var end=r.end().isBefore(available.end())?r.end():available.end();if(start.isBefore(end))pieces.add(new Range(start,end));}
  for(var covered:union(allocated)){
   List<Range> next=new ArrayList<>();
   for(var p:pieces){
    if(!p.start().isBefore(covered.end())||!covered.start().isBefore(p.end())){next.add(p);continue;}
    if(p.start().isBefore(covered.start()))next.add(new Range(p.start(),covered.start()));
    if(covered.end().isBefore(p.end()))next.add(new Range(covered.end(),p.end()));
   }pieces=next;
  }return List.copyOf(pieces);
 }
 public static long seconds(List<Range> ranges){long sum=0;for(var r:union(ranges))sum=Math.addExact(sum,Duration.between(r.start(),r.end()).getSeconds());return sum;}
}
