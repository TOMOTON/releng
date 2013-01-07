package com.google.gwt.eclipse.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 * Classpath container associated with an {@link Sdk}.
 * 
 * @param <T>
 *            type of {@link Sdk} associated with this classpath container
 */
public class GWTSDKClasspathContainer implements IClasspathContainer {

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
