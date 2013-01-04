package com.google.gwt.eclipse.core;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Represents a GWT runtime and provides a URLClassLoader that can be used to
 * load the gwt-user and gwt-dev classes.
 * 
 * TODO: Move this and subtypes into the sdk package.
 */
public class GWTRuntime {
	
	  public static final String GWT_DEV_NO_PLATFORM_JAR = "gwt-dev.jar";

	  public static final String GWT_USER_JAR = "gwt-user.jar";

	public static GWTRuntime findSdkFor(IJavaProject javaProject) {
		return new GWTRuntime();
	}

	public File getDevJar() throws CoreException {
		// TODO Auto-generated method stub
		return new File("C:\\eclipse-3\\plugins\\com.google.gwt.eclipse.sdkbundle_2.5.0.v201212122042-rel-r42\\gwt-2.5.0\\gwt-dev.jar");
	}

}

