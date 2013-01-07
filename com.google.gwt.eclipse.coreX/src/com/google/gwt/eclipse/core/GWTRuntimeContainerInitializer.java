package com.google.gwt.eclipse.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.Version;

import com.google.gwt.eclipse.core.internal.GWTActivator;

public class GWTRuntimeContainerInitializer extends	ClasspathContainerInitializer {

	public static final String CONTAINER_ID = "com.google.gwt.eclipse.core" + ".GWT_CONTAINER";

	public static final Path CONTAINER_PATH = new Path(CONTAINER_ID);
	
	@Override
	public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
		System.err.println("CONTAINER PATH " + containerPath);
	    GWTSDKClasspathContainer classpathContainer = resolveClasspathContainer(containerPath, javaProject);
	    // Container will be set to null if it could not be resolved which will
	    // result in a classpath error for the project.
	    JavaCore.setClasspathContainer(containerPath,
	        new IJavaProject[] { javaProject },
	        new IClasspathContainer[] { classpathContainer }, null);
	}
	
	private GWTSDKClasspathContainer resolveClasspathContainer(IPath containerPath, IJavaProject javaProject) {
		TreeMap<Version, GWTSDK> sdkMap = GWTActivator.findAllGWTSDKs();
		GWTSDK lastSDK = sdkMap.get(sdkMap.lastKey());
		String sdkId = lastSDK.getId();
		return new GWTSDKClasspathContainer(CONTAINER_PATH, sdkId, resolveEntries(lastSDK), "GWT " + sdkId);
	}

	private IClasspathEntry[] resolveEntries(GWTSDK sdk) {
		String[] jarFiles = sdk.getLibraries();
		List<IClasspathEntry> resultList = new ArrayList<IClasspathEntry>();
		for(int i = 0; i < jarFiles.length; i++) {
			IClasspathEntry[] entries = jarToEntries(new File(sdk.getPath(), jarFiles[i]));
			resultList.add(entries[0]);
			if(entries[1] != null)
				resultList.add(entries[1]);
		}
		return resultList.toArray(new IClasspathEntry[resultList.size()]);
	}
	
	private IClasspathEntry[] jarToEntries(File jarFile) {
		IClasspathEntry[] result = new IClasspathEntry[2];
		System.err.println("jarFile should be at: " + jarFile.getAbsolutePath());
		System.err.println("PATH jarFile should be at: " + new Path(jarFile.getAbsolutePath()).isAbsolute());
		Path path = new Path(jarFile.getAbsolutePath() + ".jar");
		Path sourcePath = new Path(jarFile.getAbsolutePath() + "-sources.jar");
		if(sourcePath.toFile().exists()) {
			result[0] = JavaCore.newLibraryEntry(path, sourcePath, new Path("/"));
			result[1] = JavaCore.newLibraryEntry(sourcePath, null, null);			
		} else {
			result[0] = JavaCore.newLibraryEntry(path, null, null);		
		}
		return result;
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
