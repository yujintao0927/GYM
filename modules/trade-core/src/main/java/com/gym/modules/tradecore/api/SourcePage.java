package com.gym.modules.tradecore.api;
import java.util.List;
public record SourcePage(List<TradeView> items,String nextCursor){public SourcePage{items=List.copyOf(items);}}
