package org.eclipse.pde.team.internal;

import org.eclipse.core.internal.resources.ResourceStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.team.IVersionUpdater;
import org.osgi.framework.Version;

@SuppressWarnings("restriction")
public class FeatureUpdater implements IVersionUpdater {
	
	@Override
	public void update(IFile featureFile, Version version, IProgressMonitor monitor) throws CoreException {		
		try {
			WorkspaceFeatureModel model = new WorkspaceFeatureModel(featureFile);
			model.load();
			IFeature feature = model.getFeature();
			feature.setVersion(version.toString());
			model.save();
		} catch (Exception e) {
			throw new CoreException(new ResourceStatus(Status.ERROR, featureFile.getProjectRelativePath(), "Could not update feature!", e));
		}
	}

}
