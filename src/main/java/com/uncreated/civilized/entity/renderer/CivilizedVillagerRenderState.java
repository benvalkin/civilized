package com.uncreated.civilized.entity.renderer;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.villagerinfo.VillagerOccupation;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CivilizedVillagerRenderState extends HumanoidRenderState {

   public VillagerOccupation occupation;
   @Nullable
   public Component villagerName;
   public Component occupationName;
   public ResourceLocation skin;
   public ResourceLocation hair;
   public ResourceLocation clothing;
   public String debugBehavioursList;
   public boolean sleepingOnFloor;

   public CivilizedVillagerRenderState() {

   }
}
