package com.uncreated.civilized.core.building.requirement;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A requirement result the server worked out, sent to the client to display. Display text ({@code description} and
 * {@code tooltip}) are computed on the server and sent to the client to display in a GUI.
 */
public record RequirementResultData(boolean satisfied, Component description, @Nullable Component tooltip,
      boolean hideIfSatisfied) implements IBuildingRequirementResult {

   public static final StreamCodec<RegistryFriendlyByteBuf, RequirementResultData> STREAM_CODEC =
         StreamCodec.composite(
               ByteBufCodecs.BOOL,
               RequirementResultData::satisfied,
               ComponentSerialization.TRUSTED_STREAM_CODEC,
               RequirementResultData::description,
               ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC,
               result -> Optional.ofNullable(result.tooltip()),
               ByteBufCodecs.BOOL,
               RequirementResultData::hideIfSatisfied,
               (satisfied, description, tooltip, hideIfSatisfied) -> new RequirementResultData(
                     satisfied,
                     description,
                     tooltip.orElse(null),
                     hideIfSatisfied));

   public static RequirementResultData from(IBuildingRequirementResult result) {
      return new RequirementResultData(
            result.isSatisfied(),
            result.getDescription(),
            result.getTooltipDescription(),
            result.hideIfSatisfied());
   }

   @Override
   public boolean isSatisfied() {
      return satisfied;
   }

   @Override
   public Component getDescription() {
      return description;
   }

   @Override
   public @Nullable Component getTooltipDescription() {
      return tooltip;
   }
}
