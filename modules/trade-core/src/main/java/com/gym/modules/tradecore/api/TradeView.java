package com.gym.modules.tradecore.api;
public record TradeView(String id,String owner,String store,String kind,String state,long remaining,long reserved,long version,long refundMinor,String refundState,String instructionId,String mode){}
