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
	    SdkClasspathContainer sdkClasspathContainer = null;
	    final T sdk = resolveSdkFromContainerPath(containerPath, javaProject);
	    if (sdk != null) {
	      String description = getDescription(containerPath, javaProject);
	      sdkClasspathContainer = createClasspathContainer(containerPath, sdk,
	          description, javaProject);
	    }

	    // Container will be set to null if it could not be resolved which will
	    // result in a classpath error for the project.
	    JavaCore.setClasspathContainer(containerPath,
	        new IJavaProject[] {javaProject},
	        new IClasspathContainer[] { sdkClasspathContainer }, null);
	}

//	  @Override
//	  public String getDescription(IPath containerPath, IJavaProject project) {
//	    return "GWT " + super.getDescription(containerPath, project);
//	  }
//
//	  @Override
//	  protected SdkClasspathContainer createClasspathContainer(
//	      IPath containerPath, GWTRuntime sdk, String description,
//	      IJavaProject javaProject) {
//	    return new GWTRuntimeContainer(containerPath, sdk,
//	        sdk.getClasspathEntries(), description);
//	  }
//
//	  @Override
//	  protected String getContainerId() {
//	    return GWTRuntimeContainer.CONTAINER_ID;
//	  }
//
//	  @Override
//	  protected SdkManager<GWTRuntime> getSdkManager() {
//	    return GWTPreferences.getSdkManager();
//	  }
//
//	  @Override
//	  protected SdkClasspathContainer<GWTRuntime> updateClasspathContainer(
//	      IPath containerPath, GWTRuntime sdk, String description,
//	      IJavaProject project, IClasspathContainer containerSuggestion) {
//
//	    // TODO: Persist the changes to the container
//
//	    return new GWTRuntimeContainer(containerPath, sdk,
//	        containerSuggestion.getClasspathEntries(), description);
//	  }

}
