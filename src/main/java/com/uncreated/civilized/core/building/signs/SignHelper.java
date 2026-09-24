package com.uncreated.civilized.core.building.signs;

import static com.uncreated.civilized.ui.menu.building.worksite.tabs.ManageWorkersTab.MAX_ASSIGNED_WORKERS;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.settlement.SettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.villagerinfo.Gender;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerStore;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

public class SignHelper {

   public static final String MARKER_TAG = "#building";

   public static SignText createSpecialSignText() {
      Component[] components =
            { Component.literal(MARKER_TAG), Component.empty(), Component.empty(), Component.empty() };
      return new SignText(components, components, DyeColor.BLACK, false);
   }

   public static void serverTriggerBuildingSignUpdate(SignBlockEntity entity) {
      entity.setText(createSpecialSignText(), true);
   }

   public static boolean signTextHasSpecialTag(SignText signText) {

      int blankCount = 0;
      boolean specialTagFound = false;
      Component[] messages = signText.getMessages(false);
      for (int i = 0; i < messages.length; i++) {
         Component component = messages[i];

         if (component.getString().isBlank())
            blankCount++;
         if (component.getString().equals(MARKER_TAG))
            specialTagFound = true;
      }

      return specialTagFound && blankCount == 3;
   }

   public static boolean signIsBlank(SignText signText) {
      return Arrays.stream(signText.getMessages(false)).allMatch(c -> c.getString().isBlank());
   }


   public static SignText getBuildingSignText(
         Building building,
         VillagerStore villagerStore,
         SettlementsStore settlementsStore) {
      Component[] signTextComponents = getSignTextComponents(building, villagerStore, settlementsStore);
      return new SignText(signTextComponents, signTextComponents, DyeColor.BLACK, false);

   }

   private static Component[] getSignTextComponents(
         Building building,
         VillagerStore villagerStore,
         SettlementsStore settlementsStore) {
      if (building.getBuildingType().is(BuildingTypes.TOWN_HALL)) {
         Settlement settlement = settlementsStore.get(building.getSettlementId());
         Set<VillagerInfo> citizens = villagerStore.getCitizens(settlement.getSettlementId());
         return new Component[] { building.getBuildingType().shortName(),
               settlement.displayNameTranslation().withStyle(ChatFormatting.ITALIC),
               Component.translatable("menu.building.town_hall.population.count", citizens.size()), Component.empty() };
      } else if (building.getBuildingType().is(BuildingTypes.INN)) {
         List<VillagerInfo> visitors = BuildingUtil.getResidents(building, villagerStore);
         return new Component[] { building.getBuildingType().shortName(),
               Component.translatable("menu.building.inn.visitors.count", visitors.size()), Component.empty(),
               Component.empty() };
      } else if (building.getBuildingType().isResidence()) {
         List<VillagerInfo> residents = BuildingUtil.getResidents(building, villagerStore);

         Optional<VillagerInfo> owner = residents.stream().filter(v -> v.getGender() == Gender.MALE).findFirst();
         if (owner.isEmpty())
            owner = residents.stream().findFirst();

         String ownerName = owner.map(VillagerInfo::getLastName).orElse("");

         return new Component[] { building.getBuildingType().shortName(),
               Component.literal(ownerName).withStyle(ChatFormatting.ITALIC),
               Component.translatable("menu.building.residence.residents.count", residents.size()), Component.empty() };
      } else if (building.getBuildingType().isWorksite()) {
         List<VillagerInfo> workers = BuildingUtil.getAssignedWorkers(building, villagerStore);
         return new Component[] { building.getBuildingType().shortName(),
               Component.translatable("menu.building.worksite.workers.count", workers.size(), MAX_ASSIGNED_WORKERS),
               Component.empty(), Component.empty() };
      }
      return new Component[] { building.getBuildingType().translation(), Component.empty(), Component.empty(),
            Component.empty() };
   }
}
