package com.uncreated.civilized.ui.menu.villager.tabs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.ui.components.IListViewBuilder;
import com.uncreated.civilized.ui.components.ScrollListView;
import com.uncreated.civilized.ui.menu.villager.FamilyMemberRowWidget;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.TabCoords;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

/** Displays all the villager's living family members. Display order: current partner, parents, children, siblings. */
public class FamilyTab extends AVillagerPanelTab {

   private static final int HEADING_GAP = 6;

   private final CivilizedVillager villager;

   private List<FamilyMember> family = List.of();
   @Nullable
   private ScrollListView<FamilyMember, FamilyMemberRowWidget> familyList;

   public FamilyTab(TabCoords tabCoords, Font font, CivilizedVillager villager) {
      super(tabCoords, font);
      this.villager = villager;
   }

   @Override
   protected void init() {
      family = findFamily(villager.getInfo());
      familyList = createFamilyList();
   }

   private static List<FamilyMember> findFamily(VillagerInfo info) {
      ClientVillagerStore store = ClientVillagerStore.INSTANCE;
      List<FamilyMember> family = new ArrayList<>();

      if (info.getPartnerId() != null)
         store.find(info.getPartnerId())
               .ifPresent(partner -> family.add(new FamilyMember(FamilyMember.Relation.PARTNER, partner)));

      // a parent who died or left no longer has any villager info, so they aren't shown
      for (UUID parentId : info.getParentIds())
         store.find(parentId).ifPresent(parent -> family.add(new FamilyMember(FamilyMember.Relation.PARENT, parent)));

      addOldestFirst(family, FamilyMember.Relation.CHILD, store.findChildren(info.getVillagerId()));
      addOldestFirst(family, FamilyMember.Relation.SIBLING, store.findSiblings(info));
      return family;
   }

   private static void addOldestFirst(
         List<FamilyMember> family,
         FamilyMember.Relation relation,
         Collection<VillagerInfo> relatives) {
      List<VillagerInfo> sorted = new ArrayList<>(relatives);
      // relatives who weren't born in the world have no birthday, so they count as the oldest
      sorted.sort(Comparator.comparing(VillagerInfo::getBirthDay, Comparator.nullsFirst(Comparator.naturalOrder())));
      for (VillagerInfo relative : sorted)
         family.add(new FamilyMember(relation, relative));
   }

   private ScrollListView<FamilyMember, FamilyMemberRowWidget> createFamilyList() {
      int listTop = listTop();
      return new ScrollListView<>(
            getX() + PADDING,
            listTop,
            width - PADDING * 2,
            getBottom() - PADDING - listTop,
            FamilyMemberRowWidget.HEIGHT,
            new IListViewBuilder<>() {
               @Override
               public List<FamilyMember> provideModelData() {
                  return family;
               }

               @Override
               public FamilyMemberRowWidget buildElementWidgetFromModel(
                     int elementIndex,
                     FamilyMember member,
                     int elementX,
                     int elementY,
                     int elementWidth,
                     int elementHeight,
                     int elementSpacing) {
                  return new FamilyMemberRowWidget(elementX, elementY, elementWidth, font, member);
               }
            });
   }

   private int listTop() {
      return getY() + PADDING + font.lineHeight + HEADING_GAP;
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.drawString(
            font,
            Component.translatable("menu.villager.tab.family"),
            getX() + PADDING,
            getY() + PADDING,
            Colors.MENU_TEXT_VILLAGER_DIALOGUE);

      if (family.isEmpty())
         graphics.drawString(
               font,
               Component.translatable("menu.villager.family.empty", villager.getInfo().getFirstName()),
               getX() + PADDING,
               listTop(),
               Colors.TEXT_LIGHT_MUTED);
      else if (familyList != null)
         familyList.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      if (family.isEmpty() || familyList == null)
         return List.of();

      return List.of(familyList);
   }
}
