/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation, SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.pde.team;

public interface IVersionMetadata {

	/**
	 * Checks if this unique line of development is baselined. Also known as 
	 * 'tag'.
	 * @return A <code>boolean</code>flag.
	 */
	public boolean isBaseline();
	
	/**
	 * Gets the baseline.
	 * @return A <code>String</code>.
	 */
	public String getBaseline();
	
	/**
	 * Checks if this unique line of development is main. Also known as 
	 * 'trunk' or 'master'.
	 * @return A <code>boolean</code>flag.
	 */
	public boolean isMainline();
	
	/**
	 * Gets the line, if other than 'main'.
	 * @return A <code>String</code>.
	 */
	public String getLine();

	/**
	 * Checks if this working copy is most recent. Also known as 'tip' or
	 * 'head'.
	 * @return A <code>boolean</code>flag.
	 */
	public boolean isMostRecent();
	
	/**
	 * Returns the system-specific reference version.
	 * @return A <code>String</code>.
	 */
	public String getRevisionReference();
	
	/**
	 * Returns
	 * @return A <code>String</code>.
	 */
	public String getQualifier();
		
}
