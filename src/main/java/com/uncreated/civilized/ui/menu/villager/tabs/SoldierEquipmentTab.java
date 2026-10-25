package com.uncreated.civilized.ui.menu.villager.tabs;

import java.util.List;

import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.networking.packets.AssignLoadoutRequest;
import com.uncreated.civilized.networking.packets.RequestSettlementLoadouts;
import com.uncreated.civilized.ui.menu.building.military.ILoadoutListener;
import com.uncreated.civilized.ui.menu.building.military.widgets.LoadoutChoice;
import com.uncreated.civilized.ui.menu.building.military.widgets.LoadoutSelectorWidget;
import com.uncreated.civilized.ui.menu.villager.VillagerMenu;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.TabCoords;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;


public class SoldierEquipmentTab extends AVillagerPanelTab implements ILoadoutListener {

   private static final int LOADOUT_Y = 40;
   private static final int LABEL_GAP = 4;

   private final CivilizedVillager villager;
   private final int columnX;
   private final LoadoutSelectorWidget loadoutSelector;
   private final int loadoutsRequestId;

   public SoldierEquipmentTab(TabCoords tabCoords, Font font, CivilizedVillager villager) {
      super(tabCoords, font);
      this.villager = villager;
      // the same column the screen centres the slots in
      this.columnX = getX() + (width - VillagerMenu.SLOT_COLUMN_WIDTH) / 2;

      int selectorX = columnX + font.width(loadoutLabel()) + LABEL_GAP;
      loadoutSelector =
            new LoadoutSelectorWidget(
                  selectorX,
                  getY() + LOADOUT_Y,
                  columnX + VillagerMenu.SLOT_COLUMN_WIDTH - selectorX,
                  font,
                  Colors.TEXT_LIGHT_MUTED,
                  false,
                  this::assignLoadout);

      loadoutsRequestId = RequestSettlementLoadouts.nextRequestId();
      PacketDistributor
            .sendToServer(new RequestSettlementLoadouts(loadoutsRequestId, villager.getInfo().getSettlementId()));
   }

   private static Component loadoutLabel() {
      return Component.translatable("menu.building.military.soldier.loadout");
   }

   @Override
   public boolean showsPlayerInventory() {
      return true;
   }

   @Override
   public void receiveListLoadouts(int requestId, List<Loadout> results) {
      if (requestId != loadoutsRequestId)
         return;

      loadoutSelector.setLoadouts(results, villager.getInfo().getLoadoutId());
   }

   private void assignLoadout(LoadoutChoice loadout) {
      PacketDistributor.sendToServer(
            new AssignLoadoutRequest(villager.getInfo().getVillagerId(), loadout.getLoadout().map(Loadout::loadoutId)));
   }

   @Override
   public void refresh() {
      loadoutSelector.showAssigned(villager.getInfo().getLoadoutId());
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.drawString(
            font,
            Component.translatable("menu.villager.tab.equipment"),
            columnX,
            getY() + PADDING,
            Colors.MENU_TEXT_VILLAGER_DIALOGUE);

      int labelY = getY() + LOADOUT_Y + (LoadoutSelectorWidget.SELECTOR_HEIGHT - font.lineHeight) / 2 + 1;
      graphics.drawString(font, loadoutLabel(), columnX, labelY, Colors.TEXT_LIGHT_MUTED);
      loadoutSelector.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of(loadoutSelector);
   }
}
