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
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.product.WorkspaceProductModel;
import org.eclipse.pde.team.IVersionUpdater;
import org.osgi.framework.Version;

@SuppressWarnings("restriction")
public class ProductUpdater implements IVersionUpdater {

	@Override
	public void update(IFile productFile, Version version, IProgressMonitor monitor) throws CoreException {
		try {
			WorkspaceProductModel model = new WorkspaceProductModel(productFile, true);
			model.load();
			IProduct product = model.getProduct();
			product.setVersion(version.toString());
			model.save();
		} catch (Exception e) {
			throw new CoreException(new ResourceStatus(Status.ERROR, productFile.getProjectRelativePath(), "Could not update product!", e));
		}
	}

}
