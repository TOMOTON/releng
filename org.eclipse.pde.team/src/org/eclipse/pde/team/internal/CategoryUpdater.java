/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation, SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.pde.team.internal;

import org.eclipse.core.internal.resources.ResourceStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.isite.ISite;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.site.WorkspaceSiteModel;
import org.eclipse.pde.team.IVersionUpdater;
import org.osgi.framework.Version;

@SuppressWarnings("restriction")
public class CategoryUpdater implements IVersionUpdater {
	
	@Override
	public void update(IFile categoryFile, Version version, IProgressMonitor monitor) throws CoreException {
		try {
			WorkspaceSiteModel model = new WorkspaceSiteModel(categoryFile);
			model.load();
			ISite site = model.getSite();
			FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
			for (ISiteFeature feature: site.getFeatures()) {
				String featureId = feature.getId();
				IFeatureModel featureModel = manager.findFeatureModel(featureId);
				if(featureModel != null) { //? Feature most likely target artifact.
					String pattern = feature.getVersion();
					feature.setVersion(version.toString());
					feature.setURL(feature.getURL().replace(pattern, version.toString()));
					System.out.println(" `- Versioning feature " + featureId + '.');					
				} else {
					System.out.println(" `- Warning! Ignoring unknown feature " + featureId + '!');
				}								
			}
			model.save();
		} catch (Exception e) {
			throw new CoreException(new ResourceStatus(Status.ERROR, categoryFile.getProjectRelativePath(), "Could not update category feature(s)!", e));
		}
	}

}
