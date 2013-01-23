package org.eclipse.pde.team;

import org.eclipse.core.resources.IProject;

public interface IVersionMetadataProvider {

	IVersionMetadata getVersionMetadata(IProject project);
	
	IVersionMetadata getVersionMetadataBlind(IProject project);
	
	String getTeamProviderID();
	
}
