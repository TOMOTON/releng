package com.google.gwt.eclipse.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.Version;

import com.google.gwt.eclipse.core.internal.GWTActivator;

/**
 * Classpath container associated with an {@link Sdk}.
 * 
 * @param <T>
 *            type of {@link Sdk} associated with this classpath container
 */
public class GWTSDKClasspathContainer implements IClasspathContainer {

	public static final String CONTAINER_ID = "com.google.gwt.eclipse.core" + ".GWT_CONTAINER";

	public static final Path CONTAINER_PATH = new Path(CONTAINER_ID);

	public static GWTSDKClasspathContainer resolveClasspathContainer(IPath containerPath, IJavaProject javaProject, Version version) {
		TreeMap<Version, GWTSDK> sdkMap = GWTActivator.findAllGWTSDKs();
		GWTSDK lastSDK = sdkMap.get(version == null ? sdkMap.lastKey() : sdkMap.ceilingKey(version));
		String sdkId = lastSDK.getId();
		return new GWTSDKClasspathContainer(GWTSDKClasspathContainer.CONTAINER_PATH, sdkId, resolveEntries(lastSDK), "GWT SDK [" + sdkId + ']');
	}
	
	public static GWTSDKClasspathContainer resolveClasspathContainer(IPath containerPath, IJavaProject javaProject) {
		return resolveClasspathContainer(containerPath, javaProject, null);
	}

	private static IClasspathEntry[] resolveEntries(GWTSDK sdk) {
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
	
	public static IClasspathEntry[] jarToEntries(File jarFile) {
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
	
	private final IPath path;

	private final String sdkId;

	private final String description;

	private final IClasspathEntry[] classpathEntries;

	public GWTSDKClasspathContainer(IPath path, String sdkId, IClasspathEntry[] classpathEntries, String description) {
		assert (!path.isEmpty());
		assert (sdkId != null);
		assert (description != null);

		this.classpathEntries = classpathEntries;
		this.path = path;
		this.sdkId = sdkId;
		this.description = description;
	}

	/**
	 * Returns the set of {@link IClasspathEntry IClasspathEntries} associated
	 * with this container.
	 */
	public IClasspathEntry[] getClasspathEntries() {
		return classpathEntries;
	}

	public String getDescription() {
		return description;
	}

	public int getKind() {
		return IClasspathContainer.K_APPLICATION;
	}

	public IPath getPath() {
		return path;
	}

}
