/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation, Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.buckminster.ide.commands;

import org.eclipse.buckminster.core.commands.WorkspaceCommand;
import org.eclipse.core.resources.IProjectNatureDescriptor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;

public class ListNatures extends WorkspaceCommand {

	@Override
	protected int internalRun(IProgressMonitor monitor) throws Exception {
		IProjectNatureDescriptor[] descriptors = ResourcesPlugin.getWorkspace().getNatureDescriptors();
		for (IProjectNatureDescriptor descriptor: descriptors) {	
			System.out.println(descriptor.getNatureId());
		}
		if (descriptors.length == 0)
			System.out.println("<<NONE>>");
		return 0;
	}

}
