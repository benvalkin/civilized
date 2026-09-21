package com.uncreated.civilized.ui.tabs;

/**
 * A screen (plain screen or menu screen) that shows tabs.
 **/
public interface ITabHost {

   ATab changeTab(int newTabIndex);

   /** Where the first tab button is drawn. The rest are spaced out below it. */
   int getFirstTabButtonX();

   int getFirstTabButtonY();
}
