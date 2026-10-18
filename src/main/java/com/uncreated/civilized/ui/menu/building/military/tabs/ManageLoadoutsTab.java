package com.uncreated.civilized.ui.menu.building.military.tabs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import com.uncreated.civilized.networking.packets.RequestSettlementLoadouts;
import com.uncreated.civilized.ui.components.IListViewBuilder;
import com.uncreated.civilized.ui.components.ScrollListView;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.menu.building.military.IListLoadoutsListener;
import com.uncreated.civilized.ui.menu.building.military.widgets.LoadoutRowWidget;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ManageLoadoutsTab extends ABuildingScreenTab implements IListLoadoutsListener {

   private static final int BUTTON_WIDTH = 80;
   private static final int BUTTON_HEIGHT = 18;
   private static final int LIST_TOP = 15;

   private final ITabHost tabHost;
   private final Button backButton;
   private final Button newLoadoutButton;

   @Nullable
   private List<Loadout> results;
   @Nullable
   private ScrollListView<Loadout, LoadoutRowWidget> loadoutList;

   public ManageLoadoutsTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      super(tabHost, font, Component.translatable("menu.building.military.loadouts.tab.heading"), context);
      this.tabHost = tabHost;

      backButton =
            Button.builder(
                  Component.translatable("gui.misc.button.back"),
                  button -> tabHost.changeTab(new ManageSoldiersTab(tabHost, font, context)))
                  .pos(getX(), getBottom() - BUTTON_HEIGHT)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();

      newLoadoutButton =
            Button.builder(
                  Component.translatable("menu.building.military.loadouts.button.new"),
                  button -> tabHost.changeTab(new EditLoadoutTab(tabHost, font, context)))
                  .pos(getRight() - BUTTON_WIDTH, getBottom() - BUTTON_HEIGHT)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();

      PacketDistributor.sendToServer(
            new RequestSettlementLoadouts(
                  RequestSettlementLoadouts.nextRequestId(),
                  context.settlement().getSettlementId()));
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);

      Component status = null;
      if (results == null)
         status = Component.translatable("gui.misc.loading");
      else if (results.isEmpty())
         status = Component.translatable("menu.building.military.loadouts.empty");

      if (status != null)
         graphics.drawWordWrap(font, status, getX(), getY() + LIST_TOP, width, Colors.MENU_TEXT_DARK, false);
      else if (loadoutList != null)
         loadoutList.render(graphics, mouseX, mouseY, partialTicks);

      backButton.render(graphics, mouseX, mouseY, partialTicks);
      newLoadoutButton.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      List<GuiEventListener> children = new ArrayList<>(List.of(backButton, newLoadoutButton));
      if (loadoutList != null) {
         children.add(loadoutList);
         children.addAll(loadoutList.children());
      }
      return children;
   }

   @Override
   public void receiveListLoadouts(int requestId, List<Loadout> results) {
      this.results = results.stream().sorted(Comparator.comparing(Loadout::displayName)).toList();
      this.loadoutList = createLoadoutList(this.results);
   }

   private ScrollListView<Loadout, LoadoutRowWidget> createLoadoutList(List<Loadout> loadouts) {
      int listHeight = height - LIST_TOP - BUTTON_HEIGHT - 4;
      return new ScrollListView<>(
            getX(),
            getY() + LIST_TOP,
            width,
            listHeight,
            LoadoutRowWidget.HEIGHT,
            new IListViewBuilder<>() {
               @Override
               public List<Loadout> provideModelData() {
                  return loadouts;
               }

               @Override
               public LoadoutRowWidget buildElementWidgetFromModel(
                     int elementIndex,
                     Loadout loadout,
                     int elementX,
                     int elementY,
                     int elementWidth,
                     int elementHeight,
                     int elementSpacing) {
                  return new LoadoutRowWidget(
                        elementX,
                        elementY,
                        elementWidth,
                        LoadoutRowWidget.HEIGHT,
                        font,
                        loadout,
                        l -> tabHost.changeTab(new EditLoadoutTab(tabHost, font, context, l)));
               }
            });
   }
}
