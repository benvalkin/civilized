package com.uncreated.civilized.ui.menu.building.residence.artisan;

/**
 * Helps control where {@link EditProductionBillTab} draws its recipe slots.
 *
 * @param itemFilterButtonsX
 *           the first button that opens an item filter. The rest are stacked below it, one per recipe slot.
 **/
public record EditProductionBillLayout(int inputsX, int inputsY, int outputX, int outputY, int itemFilterButtonsX,
      int itemFilterButtonsY) {

   /** A 3x3 grid like the crafting table. Output slot is level with the middle row. */
   public static final EditProductionBillLayout CRAFTING = new EditProductionBillLayout(26, 0, 116, 18, 226, 56);

   /** A single input like the furnace. Output slot is in line with the input slot. */
   public static final EditProductionBillLayout COOKING = new EditProductionBillLayout(44, 18, 98, 18, 226, 56);
}
