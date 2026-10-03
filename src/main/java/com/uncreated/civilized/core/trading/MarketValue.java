package com.uncreated.civilized.core.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.ExtraCodecs;

public record MarketValue(int value, int per) {

   public static final float BUY_PRICE_MULTIPLIER = 2.33f;

   public static final Codec<MarketValue> CODEC =
         RecordCodecBuilder.create(
               instance -> instance
                     .group(
                           ExtraCodecs.POSITIVE_INT.fieldOf("value").forGetter(MarketValue::value),
                           ExtraCodecs.POSITIVE_INT.optionalFieldOf("per", 1).forGetter(MarketValue::per))
                     .apply(instance, MarketValue::new));

   public MarketValue {
      if (value < 1 || per < 1)
         throw new IllegalArgumentException("A market value has to be positive, not " + value + " per " + per);
   }

   public static MarketValue of(int value) {
      return new MarketValue(value, 1);
   }

   public static MarketValue of(int value, int per) {
      return new MarketValue(value, per);
   }
}
