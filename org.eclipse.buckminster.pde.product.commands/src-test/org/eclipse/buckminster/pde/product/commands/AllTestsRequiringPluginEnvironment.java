/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation, Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.buckminster.pde.product.commands;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTestsRequiringPluginEnvironment {
	
	 public static Test suite() { 
         TestSuite suite = new TestSuite("Tests requiring Eclipse environment");
         suite.addTestSuite(TestOptionParsing.class);
         return suite; 
    }

}
