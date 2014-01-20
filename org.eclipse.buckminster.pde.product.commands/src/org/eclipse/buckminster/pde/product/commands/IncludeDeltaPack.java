/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation, Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.buckminster.pde.product.commands;

import java.io.File;

import org.eclipse.buckminster.cmdline.SimpleErrorExitException;
import org.eclipse.buckminster.cmdline.UsageException;
import org.eclipse.buckminster.core.commands.WorkspaceCommand;
import org.eclipse.buckminster.pde.internal.PDETargetPlatform;
import org.eclipse.buckminster.runtime.Buckminster;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;


public class IncludeDeltaPack extends WorkspaceCommand {

	private File deltaPackPath;
	
	@Override
	protected int internalRun(IProgressMonitor monitor) throws Exception {
		if (deltaPackPath == null)
			throw new SimpleErrorExitException(org.eclipse.buckminster.core.Messages.Too_few_arguments);
		if(!deltaPackPath.exists())
			throw new SimpleErrorExitException("Path does not exist!");
		if(!deltaPackPath.isDirectory())
			throw new SimpleErrorExitException("Path is not a directory!");
		Buckminster bucky = Buckminster.getDefault();
		ITargetPlatformService service = bucky.getService(ITargetPlatformService.class);		
		ITargetHandle activeTarget = service.getWorkspaceTargetHandle();
		ITargetDefinition newTarget = service.newTarget();
		//? Copy.
		service.copyTargetDefinition(activeTarget.getTargetDefinition(), newTarget);
		//? Add Delta Pack directory path.
		ITargetLocation[] locations = newTarget.getTargetLocations();
		ITargetLocation[] newLocations = new ITargetLocation[locations.length + 1];
		System.arraycopy(locations, 0, newLocations, 0, locations.length);
		newLocations[locations.length] = service.newDirectoryLocation(deltaPackPath.getAbsolutePath());
		newTarget.setName(activeTarget.getTargetDefinition().getName() + " w/Delta");
		System.out.println("Added delta pack.");
		newTarget.setTargetLocations(newLocations);
		//? SHOULD WE RESOLVE???
		//newTarget.resolve(monitor);
		service.saveTargetDefinition(newTarget);
		PDETargetPlatform.setTargetActive(newTarget, monitor);
		return 0;
	}

	@Override
	protected void handleUnparsed(String[] unparsed) throws Exception {
		int len = unparsed.length;
		if(len > 1)
			throw new UsageException("Too many arguments");
		if(len == 1)
			setDeltaPackPath(unparsed[0]);
	}

	private void setDeltaPackPath(String path) {
		this.deltaPackPath = new File(path);		
	}

}
