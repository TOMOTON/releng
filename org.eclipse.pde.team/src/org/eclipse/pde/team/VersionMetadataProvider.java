package org.eclipse.pde.team;

import org.eclipse.core.resources.IProject;

public interface VersionMetadataProvider {

	VersionMetadata getVersionMetadata(IProject project);
	
	String getTeamProviderID();
	
}
