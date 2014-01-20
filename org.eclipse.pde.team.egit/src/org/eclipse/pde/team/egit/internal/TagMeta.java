/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation, SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.pde.team.egit.internal;

public class TagMeta {
	
	private String name;
	
	private int distance;
	
	public TagMeta(String name, int distance) {
		this.name = name;
		this.distance = distance;
	}

	public String getName() {
		return name;
	}

	public int getDistance() {
		return distance;
	}
	
}