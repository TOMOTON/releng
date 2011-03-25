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
