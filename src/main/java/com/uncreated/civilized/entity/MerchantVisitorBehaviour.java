package com.uncreated.civilized.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.trading.CurrencyStock;
import com.uncreated.civilized.core.trading.MerchantType;
import com.uncreated.civilized.core.trading.MerchantTypes;
import com.uncreated.civilized.core.trading.TradeItem;

import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

public class MerchantVisitorBehaviour extends VisitorBehaviour implements IMerchantBehaviour {

   private static final Logger LOGGER = LogUtils.getLogger();

   public static final String FIELD_MERCHANT_TYPE = "merchant_type";
   public static final String FIELD_TRADE_ITEMS = "trade_items";
   private static final String FIELD_TRADE_AVAILABLE_CURRENCY = "available_currency";

   @Getter
   private @Nullable ResourceKey<MerchantType> merchantType;

   /**
    * The amount of currency that this merchant has to pay the player when selling items.
    * <p>
    * Note: this object is deliberately passed by reference directly into the Trading Menu, so that when buying/selling
    * items, adding/subtracting to and from the merchant's available funds reflects here (server-side) and doesn't need
    * extra setting. This is why a wrapper object is used instead of a plain int.
    */
   @Getter
   private CurrencyStock availableCurrency = new CurrencyStock(0);
   @Getter
   private List<TradeItem> tradeItems = new ArrayList<>();

   private boolean stocked;

   public MerchantVisitorBehaviour(CivilizedVillager villager) {
      super(villager);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag compound) {
      super.addAdditionalSaveData(compound);

      if (!stocked)
         return;

      if (merchantType != null)
         compound.putString(FIELD_MERCHANT_TYPE, merchantType.location().toString());
      compound.putInt(FIELD_TRADE_AVAILABLE_CURRENCY, availableCurrency.availableCurrency());

      TradeItem.CODEC.listOf()
            .encodeStart(registryOps(), tradeItems)
            .resultOrPartial(error -> LOGGER.error("Couldn't save merchant's trades: {}", error))
            .ifPresent(tag -> compound.put(FIELD_TRADE_ITEMS, tag));
   }

   @Override
   public void readAdditionalSaveData(CompoundTag compound) {
      super.readAdditionalSaveData(compound);

      if (compound.contains(FIELD_MERCHANT_TYPE))
         merchantType =
               ResourceKey.create(
                     MerchantTypes.REGISTRY_KEY,
                     ResourceLocation.parse(compound.getString(FIELD_MERCHANT_TYPE)));

      if (compound.contains(FIELD_TRADE_ITEMS, Tag.TAG_LIST)) {
         stocked = true;
         tradeItems =
               TradeItem.CODEC.listOf()
                     .parse(registryOps(), compound.get(FIELD_TRADE_ITEMS))
                     .resultOrPartial(error -> LOGGER.error("Could not load merchant's trades: {}", error))
                     .map(trades -> (List<TradeItem>) new ArrayList<>(trades))
                     .orElseGet(ArrayList::new);
      }

      if (compound.contains(FIELD_TRADE_AVAILABLE_CURRENCY))
         availableCurrency = new CurrencyStock(compound.getInt(FIELD_TRADE_AVAILABLE_CURRENCY));
   }

   @Override
   public void serverTick(ServerLevel level, long gameTime) {
      super.serverTick(level, gameTime);
      if (villager.isRemoved())
         return;

      // done here rather than when the merchant spawns, so that merchants get stocked however they come about
      if (!stocked)
         stockWithNewTrades(level);
   }

   public void stockWithNewTrades(ServerLevel level) {
      stocked = true;

      Optional<Holder.Reference<MerchantType>> type =
            MerchantTypes.pickRandom(level.registryAccess(), villager.getArbitraryRandom());
      if (type.isEmpty()) {
         LOGGER.warn("No merchant types are loaded. {} has nothing to sell", villager);
         merchantType = null;
         availableCurrency = new CurrencyStock(0);
         tradeItems = new ArrayList<>();
         return;
      }

      stockWithNewTrades(type.get());
   }

   /** Replaces the merchant's goods with a new random selection from the given merchant type. */
   public void stockWithNewTrades(Holder.Reference<MerchantType> type) {
      stocked = true;
      merchantType = type.key();
      int startingCurrency = type.value().startingCurrency().sample(villager.getArbitraryRandom());
      availableCurrency = new CurrencyStock(startingCurrency);
      tradeItems = new ArrayList<>(type.value().rollTrades(villager.getArbitraryRandom()));
   }

   private RegistryOps<Tag> registryOps() {
      return villager.registryAccess().createSerializationContext(NbtOps.INSTANCE);
   }
}
