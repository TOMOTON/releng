package org.eclipse.pde.team.egit.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

public class MainlineInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		System.err.println("###### MAINLINE INITIALIZER ######");
	}

}
