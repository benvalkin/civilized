package com.uncreated.civilized.ui.menu.building;

import java.util.List;

import com.uncreated.civilized.core.building.requirement.IBuildingRequirementResult;

/**
 * A screen or building menu tab that asked the server to check requirements, and shows the results once they arrive.
 */
public interface IRequirementsCheckListener {

   /**
    * @param requestId
    *           the id of the request this answers, which may be an old one the listener has since replaced
    */
   void receiveRequirementsChecked(int requestId, List<? extends IBuildingRequirementResult> results);
}
