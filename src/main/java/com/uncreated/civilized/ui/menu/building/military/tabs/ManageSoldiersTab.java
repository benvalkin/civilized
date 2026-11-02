package com.uncreated.civilized.ui.menu.building.military.tabs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.ui.components.IListViewBuilder;
import com.uncreated.civilized.ui.components.ScrollListView;
import com.uncreated.civilized.networking.packets.RequestSettlementLoadouts;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.menu.building.military.ILoadoutListener;
import com.uncreated.civilized.ui.menu.building.military.widgets.SoldierRowWidget;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ManageSoldiersTab extends ABuildingScreenTab implements ILoadoutListener {

   private static final int LIST_TOP = 15;
   /** Fits the soldier's name with their shift and loadout underneath. */
   private static final int ROW_HEIGHT = 22;
   private static final int BUTTON_WIDTH = 80;
   private static final int BUTTON_HEIGHT = 18;

   private final ITabHost tabHost;
   private final Button manageLoadoutsButton;
   private List<VillagerInfo> soldiers;
   private ScrollListView<VillagerInfo, SoldierRowWidget> soldierList;
   private int loadoutsRequestId;
   /** Empty until the server answers, so rows show no loadouts until then. */
   private Map<UUID, String> loadoutNames = new HashMap<>();

   public ManageSoldiersTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      super(tabHost, font, Component.translatable("menu.building.military.soldiers.tab.heading"), context);
      this.tabHost = tabHost;

      manageLoadoutsButton =
            Button.builder(
                  Component.translatable("menu.building.military.soldiers.button.manage_loadouts"),
                  button -> tabHost.changeTab(new ManageLoadoutsTab(tabHost, font, context)))
                  .pos(getRight() - BUTTON_WIDTH, getBottom() - BUTTON_HEIGHT)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();

      loadoutsRequestId = RequestSettlementLoadouts.nextRequestId();
      PacketDistributor
            .sendToServer(new RequestSettlementLoadouts(loadoutsRequestId, context.settlement().getSettlementId()));

      refresh();
   }

   @Override
   public void receiveListLoadouts(int requestId, List<Loadout> results) {
      if (requestId != loadoutsRequestId)
         return;

      loadoutNames = new HashMap<>();
      for (Loadout loadout : results)
         loadoutNames.put(loadout.loadoutId(), loadout.displayName());

      soldierList = createSoldierList();
   }

   private List<VillagerInfo> findSoldiers() {
      return ClientVillagerStore.INSTANCE.getCitizens(context.settlement().getSettlementId())
            .stream()
            .filter(villager -> villager.getOccupation().is(VillagerOccupations.SOLDIER))
            .sorted(Comparator.comparing(VillagerInfo::getFullName))
            .toList();
   }

   private ScrollListView<VillagerInfo, SoldierRowWidget> createSoldierList() {
      int listHeight = height - LIST_TOP - BUTTON_HEIGHT - 4;
      return new ScrollListView<>(getX(), getY() + LIST_TOP, width, listHeight, ROW_HEIGHT, new IListViewBuilder<>() {
         @Override
         public List<VillagerInfo> provideModelData() {
            return soldiers;
         }

         @Override
         public SoldierRowWidget buildElementWidgetFromModel(
               int elementIndex,
               VillagerInfo soldier,
               int elementX,
               int elementY,
               int elementWidth,
               int elementHeight,
               int elementSpacing) {
            return new SoldierRowWidget(
                  elementX,
                  elementY,
                  elementWidth,
                  elementHeight,
                  font,
                  soldier,
                  soldier.getLoadoutId() != null ? loadoutNames.get(soldier.getLoadoutId()) : null,
                  this::manage);
         }

         private void manage(VillagerInfo soldier) {
            tabHost.changeTab(new ManageSoldierTab(tabHost, font, context, soldier));
         }
      });
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);

      if (soldiers.isEmpty())
         graphics.drawWordWrap(
               font,
               Component.translatable("menu.building.military.soldiers.empty"),
               getX(),
               getY() + LIST_TOP,
               width,
               Colors.MENU_TEXT_DARK,
               false);
      else
         soldierList.render(graphics, mouseX, mouseY, partialTicks);

      manageLoadoutsButton.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      List<GuiEventListener> children = new ArrayList<>();
      children.add(manageLoadoutsButton);
      if (!soldiers.isEmpty()) {
         children.add(soldierList);
         children.addAll(soldierList.children());
      }
      return children;
   }

   @Override
   public void refresh() {
      soldiers = findSoldiers();
      soldierList = createSoldierList();
   }
}
