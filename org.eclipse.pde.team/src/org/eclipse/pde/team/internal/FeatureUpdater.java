package org.eclipse.pde.team.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.team.IVersionUpdater;
import org.osgi.framework.Version;

public class FeatureUpdater implements IVersionUpdater {
	
	@Override
	public void update(IFile featureFile, Version version, IProgressMonitor monitor) throws CoreException {
		
		//feature.setVersion("");
		//feature.getModel().getUnderlyingResource();
		
	}

}
