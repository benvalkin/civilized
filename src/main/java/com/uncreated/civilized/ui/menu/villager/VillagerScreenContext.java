package com.uncreated.civilized.ui.menu.villager;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.recruitment.VisitorHospitality;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.VisitorBehaviour;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

@Accessors(fluent = true)
@Getter
public class VillagerScreenContext {

   public static final StreamCodec<RegistryFriendlyByteBuf, VillagerScreenContext> STREAM_CODEC =
         StreamCodec.composite(
               NeoForgeStreamCodecs.enumCodec(InteractionHand.class),
               VillagerScreenContext::interactionHand,
               VisitorHospitality.NULLABLE_STREAM_CODEC,
               VillagerScreenContext::hospitality,
               VillagerScreenContext::new);
   private final InteractionHand interactionHand;
   /** Visitor hospitality of the interacted villager, if they're a visitor (null otherwise). */
   private final @Nullable VisitorHospitality hospitality;

   public VillagerScreenContext(InteractionHand interactionHand, @Nullable VisitorHospitality hospitality) {
      this.interactionHand = interactionHand;
      this.hospitality = hospitality;
   }

   public VillagerScreenContext(InteractionHand interactionHand, CivilizedVillager villager) {
      this.interactionHand = interactionHand;
      this.hospitality = VisitorBehaviour.tryGet(villager).map(VisitorBehaviour::getHospitality).orElse(null);
   }
}
