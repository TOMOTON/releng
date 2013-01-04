package com.google.gwt.eclipse.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * A GWT library containing the gwt-user and gwt-dev classes.
 * 
 * TODO: Move this and subtypes into the sdk package.
 */
public class GWTRuntimeContainer extends SdkClasspathContainer {
	
  public static final String CONTAINER_ID = "com.google.gwt.eclipse.core" + ".GWT_CONTAINER";
  
  public static final Path CONTAINER_PATH = new Path(CONTAINER_ID);

  public static IPath getDefaultRuntimePath() {
    // Default runtime just has one segment path with container ID
    return CONTAINER_PATH;
  }

  public static boolean isPathForGWTRuntimeContainer(IPath path) {
    return CONTAINER_PATH.isPrefixOf(path);
  }

  public GWTRuntimeContainer(IPath path, GWTRuntime runtime, IClasspathEntry[] classpathEntries, String description) {
    super(path, runtime, classpathEntries, description);
  }

  @Override
  public IClasspathEntry[] getClasspathEntries() {
    IClasspathEntry[] superEntries = super.getClasspathEntries();
    List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();

    for (IClasspathEntry superEntry : superEntries) {
      entries.add(superEntry);
    }
    return entries.toArray(new IClasspathEntry[entries.size()]);
  }
}
