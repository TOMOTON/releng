/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation, SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.pde.team;

import org.eclipse.core.resources.IProject;

public interface IVersionMetadataProvider {

	IVersionMetadata getVersionMetadata(IProject project);
	
	IVersionMetadata getVersionMetadataBlind(IProject project);
	
	String getTeamProviderID();
	
}
