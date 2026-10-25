package com.uncreated.civilized.ui.menu.building.military.tabs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.entity.equipment.CarriedEquipmentSlot;
import com.uncreated.civilized.networking.packets.AssignLoadoutRequest;
import com.uncreated.civilized.networking.packets.AssignWorkerRequest;
import com.uncreated.civilized.networking.packets.RequestSettlementLoadouts;
import com.uncreated.civilized.networking.packets.RequestSoldierEquipment;
import com.uncreated.civilized.ui.components.widget.CarriedSlotIconWidget;
import com.uncreated.civilized.ui.components.widget.ItemDisplayWidget;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.menu.building.military.ILoadoutListener;
import com.uncreated.civilized.ui.menu.building.military.LoadoutSlotLayout;
import com.uncreated.civilized.ui.menu.building.military.widgets.LoadoutChoice;
import com.uncreated.civilized.ui.menu.building.military.widgets.LoadoutSelectorWidget;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class ManageSoldierTab extends ABuildingScreenTab implements ILoadoutListener {

   private static final int LABEL_Y = 12;
   private static final int CONTENT_Y = 24;
   private static final int SLOT_SPACING = 18;
   private static final int ROW_SPACING = 19;

   /** Where the settings start, to the right of the equipment grid. */
   private static final int SETTINGS_X = 160;
   private static final int SELECTOR_WIDTH = 126;

   private static final int BUTTON_WIDTH = 60;
   private static final int BUTTON_HEIGHT = 18;

   private final VillagerInfo soldier;
   private List<CarriedSlotIconWidget> equipmentIcons = new ArrayList<>();
   private final Button backButton;
   private final Button dismissButton;

   private int equipmentRequestId;
   /** Empty until the server answers. */
   private List<ItemDisplayWidget> equipmentItems = new ArrayList<>();
   private boolean equipmentLoading = true;
   /** The soldier is not loaded in a chunk (e.g. too far away) so the server cannot tell what they're carrying. */
   private boolean equipmentUnavailable;

   private int loadoutsRequestId;
   private final LoadoutSelectorWidget loadoutSelector;

   public ManageSoldierTab(ITabHost tabHost, Font font, BuildingScreenContext context, VillagerInfo soldier) {
      super(
            tabHost,
            font,
            Component.translatable("menu.building.military.soldier.tab.heading", soldier.getFullName()),
            context);
      this.soldier = soldier;

      backButton =
            Button.builder(
                  Component.translatable("gui.misc.button.back"),
                  button -> tabHost.changeTab(new ManageSoldiersTab(tabHost, font, context)))
                  .pos(getX(), getBottom() - BUTTON_HEIGHT)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();

      // a soldier is someone working at a military post, so dismissing them takes them off it
      dismissButton =
            Button.builder(
                  Component.translatable("menu.building.military.soldier.button.dismiss"),
                  button -> dismiss(tabHost, soldier))
                  .pos(getRight() - BUTTON_WIDTH, getBottom() - BUTTON_HEIGHT)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();

      equipmentRequestId = RequestSoldierEquipment.nextRequestId();
      PacketDistributor.sendToServer(new RequestSoldierEquipment(equipmentRequestId, soldier.getVillagerId()));

      loadoutSelector =
            new LoadoutSelectorWidget(
                  getX() + SETTINGS_X,
                  getY() + CONTENT_Y,
                  SELECTOR_WIDTH,
                  font,
                  Colors.MENU_TEXT_DARK,
                  true,
                  this::assignLoadout);
      loadoutsRequestId = RequestSettlementLoadouts.nextRequestId();
      PacketDistributor
            .sendToServer(new RequestSettlementLoadouts(loadoutsRequestId, context.settlement().getSettlementId()));
   }

   private void dismiss(ITabHost tabHost, VillagerInfo soldier) {
      PacketDistributor.sendToServer(new AssignWorkerRequest(soldier.getVillagerId(), Optional.empty()));
      tabHost.changeTab(new ManageSoldiersTab(tabHost, font, context));
   }

   public void receiveEquipment(int requestId, @Nullable List<ItemStack> carried) {
      if (requestId != equipmentRequestId)
         return;

      equipmentLoading = false;
      equipmentUnavailable = carried == null;
      if (carried == null) {
         equipmentItems = List.of();
         return;
      }

      equipmentIcons = new ArrayList<>();
      equipmentItems = new ArrayList<>();
      for (int i = 0; i < Loadout.SUPPORTED_SLOTS.size(); i++) {
         CarriedEquipmentSlot slot = Loadout.SUPPORTED_SLOTS.get(i);
         ItemStack itemStack = i < carried.size() ? carried.get(i) : ItemStack.EMPTY;
         int x = getX(i);
         int y = getY() + CONTENT_Y;
         equipmentItems.add(new ItemDisplayWidget(x, y, itemStack));
         equipmentIcons.add(
               new CarriedSlotIconWidget(
                     x,
                     y,
                     LoadoutSlotLayout.watermarkIconFor(slot),
                     LoadoutSlotLayout.nameOf(slot)));
      }
   }

   private int getX(int i) {
      return getX() + i * SLOT_SPACING;
   }

   @Override
   public void receiveListLoadouts(int requestId, List<Loadout> results) {
      if (requestId != loadoutsRequestId)
         return;

      loadoutSelector.setLoadouts(results, soldier.getLoadoutId());
   }

   private void assignLoadout(LoadoutChoice loadoutChoice) {
      PacketDistributor.sendToServer(
            new AssignLoadoutRequest(soldier.getVillagerId(), loadoutChoice.getLoadout().map(Loadout::loadoutId)));
   }

   @Override
   public void refresh() {
      loadoutSelector.showAssigned(soldier.getLoadoutId());
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);

      renderEquipment(graphics, mouseX, mouseY, partialTicks);
      renderLoadout(graphics, mouseX, mouseY, partialTicks);

      backButton.render(graphics, mouseX, mouseY, partialTicks);
      dismissButton.render(graphics, mouseX, mouseY, partialTicks);
   }

   private void renderEquipment(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      graphics.drawString(
            font,
            Component.translatable("menu.building.military.soldier.equipment"),
            getX(),
            getY() + LABEL_Y,
            Colors.MENU_TEXT_DARK,
            false);

      for (int i = 0; i < equipmentItems.size(); i++) {
         ItemDisplayWidget item = equipmentItems.get(i);
         if (!item.getDisplayItem().isEmpty())
            item.render(graphics, mouseX, mouseY, partialTicks);
         else {
            CarriedSlotIconWidget watermarkIcon = equipmentIcons.get(i);
            watermarkIcon.render(graphics, mouseX, mouseY, partialTicks);
         }
      }

      Component status = null;
      if (equipmentLoading)
         status = Component.translatable("gui.misc.loading");
      else if (equipmentUnavailable)
         status = Component.translatable("menu.building.military.soldier.equipment.unavailable");

      if (status != null)
         graphics.drawWordWrap(
               font,
               status,
               getX(),
               getY() + CONTENT_Y + ROW_SPACING * 2 + 4,
               SETTINGS_X - 10,
               Colors.MENU_TEXT_DARK,
               false);
   }

   private void renderLoadout(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      graphics.drawString(
            font,
            Component.translatable("menu.building.military.soldier.loadout"),
            getX() + SETTINGS_X,
            getY() + LABEL_Y,
            Colors.MENU_TEXT_DARK,
            false);

      loadoutSelector.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of(backButton, dismissButton, loadoutSelector);
   }
}
