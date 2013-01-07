package com.google.gwt.eclipse.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class GWTRuntimeContainerInitializer extends	ClasspathContainerInitializer {

	@Override
	public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
		System.err.println("CONTAINER PATH " + containerPath);
	    GWTSDKClasspathContainer classpathContainer = GWTSDKClasspathContainer.resolveClasspathContainer(containerPath, javaProject);
	    // Container will be set to null if it could not be resolved which will
	    // result in a classpath error for the project.
	    JavaCore.setClasspathContainer(containerPath,
	        new IJavaProject[] { javaProject },
	        new IClasspathContainer[] { classpathContainer }, null);
	}

	@Override
	public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
		return true;
	}

	@Override
	public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project, IClasspathContainer containerSuggestion)	throws CoreException {
		throw new UnsupportedOperationException();
	}

}
