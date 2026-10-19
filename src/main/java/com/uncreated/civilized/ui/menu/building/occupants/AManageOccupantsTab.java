package com.uncreated.civilized.ui.menu.building.occupants;

import java.util.ArrayList;
import java.util.List;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.ui.components.IListViewBuilder;
import com.uncreated.civilized.ui.components.ScrollListView;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

public abstract class AManageOccupantsTab extends ABuildingScreenTab {

   private static final int SUB_HEADING_Y = 15;
   private static final int LIST_TOP = 30;

   protected final ITabHost tabHost;
   protected final Building building;

   private List<VillagerInfo> occupants = List.of();
   private ScrollListView<VillagerInfo, OccupantRowWidget> occupantList;

   protected AManageOccupantsTab(ITabHost tabHost, Font font, Component heading, BuildingScreenContext context) {
      super(tabHost, font, heading, context);
      this.tabHost = tabHost;
      this.building = context.building();
   }

   protected abstract List<VillagerInfo> provideOccupantsModelData();

   protected abstract OccupantRowWidget buildElementWidgetFromModel(VillagerInfo occupant, int x, int y, int width);

   protected abstract Component getSubHeading(List<VillagerInfo> occupants);

   protected abstract Component getEmptyOccupantsMessage();


   @Override
   public void refresh() {
      occupants = provideOccupantsModelData();
      occupantList = createOccupantList();
   }

   private ScrollListView<VillagerInfo, OccupantRowWidget> createOccupantList() {
      return new ScrollListView<>(
            getX(),
            getY() + LIST_TOP,
            width,
            height - LIST_TOP,
            OccupantRowWidget.HEIGHT,
            new IListViewBuilder<>() {
               @Override
               public List<VillagerInfo> provideModelData() {
                  return occupants;
               }

               @Override
               public OccupantRowWidget buildElementWidgetFromModel(
                     int elementIndex,
                     VillagerInfo occupant,
                     int elementX,
                     int elementY,
                     int elementWidth,
                     int elementHeight,
                     int elementSpacing) {
                  return AManageOccupantsTab.this
                        .buildElementWidgetFromModel(occupant, elementX, elementY, elementWidth);
               }
            });
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);

      graphics.drawString(
            font,
            getSubHeading(occupants),
            getX(),
            getY() + SUB_HEADING_Y,
            Colors.MENU_TEXT_DARK,
            false);

      if (occupants.isEmpty())
         graphics.drawWordWrap(
               font,
               getEmptyOccupantsMessage(),
               getX(),
               getY() + LIST_TOP,
               width,
               Colors.BUILDING_DARK,
               false);
      else
         occupantList.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      List<GuiEventListener> children = new ArrayList<>();
      if (!occupants.isEmpty()) {
         children.add(occupantList);
         children.addAll(occupantList.children());
      }
      return children;
   }
}
