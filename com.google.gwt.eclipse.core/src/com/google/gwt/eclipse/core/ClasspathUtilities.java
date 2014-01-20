/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.google.gwt.eclipse.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import com.google.gwt.eclipse.core.internal.GWTActivator;

@SuppressWarnings("restriction")
public final class ClasspathUtilities {
	
  /**
   * Returns the {@link IClasspathEntry#CPE_CONTAINER} entry with the specified
   * container ID or <code>null</code> if one could not be found.
   * 
   * @param classpathEntries array of classpath entries
   * @param containerId container ID
   * @return {@link IClasspathEntry#CPE_CONTAINER} entry with the specified
   *         container ID or <code>null</code> if one could not be found
   */
  public static IClasspathEntry findClasspathEntryContainer(IClasspathEntry[] classpathEntries, String containerId) {
    int index = indexOfClasspathEntryContainer(classpathEntries, containerId);
    if (index >= 0) {
      return classpathEntries[index];
    }
    return null;
  }

  /**
   * Returns a String (in the format of the JVM classpath argument) which
   * contains the given classpath entries.
   * 
   * @param classpathEntries array of runtime classpath entries
   * @return flattened String of the given classpath entries in the format
   *         suitable for passing as a JVM argument
   */
  public static String flattenToClasspathString(List<IRuntimeClasspathEntry> classpathEntries) {
    StringBuilder sb = new StringBuilder();
    boolean needsSeparator = false;
    for (IRuntimeClasspathEntry r : classpathEntries) {
      if (needsSeparator) {
        sb.append(File.pathSeparatorChar);
      }
      needsSeparator = true;
      sb.append(r.getLocation());
    }

    return sb.toString();
  }

  /**
   * Returns the first index of the specified
   * {@link IClasspathEntry#CPE_CONTAINER} entry with the specified container ID
   * or -1 if one could not be found.
   * 
   * @param classpathEntries array of classpath entries
   * @param containerId container ID
   * @return index of the specified {@link IClasspathEntry#CPE_CONTAINER} entry
   *         with the specified container ID or -1
   */
  public static int indexOfClasspathEntryContainer(IClasspathEntry[] classpathEntries, String containerId) {
    for (int i = 0; i < classpathEntries.length; ++i) {
      IClasspathEntry classpathEntry = classpathEntries[i];
      if (classpathEntry.getEntryKind() != IClasspathEntry.CPE_CONTAINER) {
        // Skip anything that is not a container
        continue;
      }

      IPath containerPath = classpathEntry.getPath();
      if (containerPath.segmentCount() > 0
          && containerPath.segment(0).equals(containerId)) {
        return i;
      }
    }

    return -1;
  }
  
  /**
   * Returns the GWT-applicable source folder paths from a project (note: this
   * will not traverse into the project's dependencies, for this behavior, see
   * {@link #getGWTSourceFolderPathsFromProjectAndDependencies(IJavaProject, boolean)}
   * ).
   * 
   * @param javaProject Reference to the project
   * @param sourceEntries The list to be filled with the entries corresponding
   *          to the source folder paths
   * @param includeTestSourceEntries Whether to include the entries for test
   *          source
   * @throws SdkException
   */
  private static void fillGWTSourceFolderPathsFromProject(IJavaProject javaProject, Collection<? super IRuntimeClasspathEntry> sourceEntries, boolean includeTestSourceEntries) throws CoreException {
    
    assert (javaProject != null);

    if (isGWTRuntimeProject(javaProject)) {
      // TODO: Do we still need to handle this here since Sdk's report their
      // own runtime classpath entries?
      sourceEntries.addAll(getGWTRuntimeProjectSourceEntries(
          javaProject, includeTestSourceEntries));
    } else {
      try {
        for (IClasspathEntry curClasspathEntry : javaProject.getRawClasspath()) {
          if (curClasspathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
            IPath sourcePath = curClasspathEntry.getPath();
            // If including tests, include all source, or if not including tests, ensure
            // it is not a test path
            if (includeTestSourceEntries || !isTestPath(sourcePath)) {
              sourceEntries.add(JavaRuntime.newArchiveRuntimeClasspathEntry(sourcePath));
            }
          }
        }
        IFolder folder = javaProject.getProject().getFolder("super");
        if (folder.exists()) {
          sourceEntries.add(JavaRuntime.newArchiveRuntimeClasspathEntry(folder.getFullPath()));
        }
      } catch (JavaModelException jme) {
        System.err.println("Unable to retrieve raw classpath for project "
                + javaProject.getProject().getName());
        jme.printStackTrace();
      }
    }
  }
  
  /**
   * Returns the GWT-applicable source folder paths from a project and all of
   * its transitively required projects.
   * @throws SdkException 
   * @throws JavaModelException 
   * 
   * @see #fillGWTSourceFolderPathsFromProject(IJavaProject, Collection, boolean)
   */
  public static List<IRuntimeClasspathEntry> getGWTSourceFolderPathsFromProjectAndDependencies(IJavaProject javaProject, boolean includeTestSourceEntries) throws JavaModelException, CoreException {
    List<IRuntimeClasspathEntry> sourceEntries =
        new ArrayList<IRuntimeClasspathEntry>();
    for (IJavaProject curJavaProject :
        getTransitivelyRequiredProjects(javaProject)) {
      fillGWTSourceFolderPathsFromProject(curJavaProject, sourceEntries,
          includeTestSourceEntries);
    }
    return sourceEntries;
  }

  public static boolean isTestPath(IPath path) {
    return "test".equals(path.lastSegment())
        || "javatests".equals(path.lastSegment());
  }

  /**
   * Gets the transitive closure of required Java projects for the given Java
   * project (including the project itself). The ordering is depth-first
   * preordered (i.e. a Java project is earlier than its dependencies).
   * 
   * @param javaProject The Java project whose dependencies will be returned
   * @return An ordered set of the Java project and its transitively required
   *         projects
   * @throws JavaModelException
   */
  public static List<IJavaProject> getTransitivelyRequiredProjects(
      IJavaProject javaProject) throws JavaModelException {
    List<IJavaProject> requiredProjects = new ArrayList<IJavaProject>();
    fillTransitivelyRequiredProjects(javaProject, requiredProjects);
    return requiredProjects;
  }
  
  private static void fillTransitivelyRequiredProjects(IJavaProject javaProject,
	      List<? super IJavaProject> requiredProjects)
	      throws JavaModelException {
	    
	    if (requiredProjects.contains(javaProject)) {
	      return;
	    }
	    
	    requiredProjects.add(javaProject);
	    
	    for (String projectName : javaProject.getRequiredProjectNames()) {
	      IJavaProject curJavaProject = findJavaProject(
	          projectName);
	      if (curJavaProject == null) {
	        continue;
	      }
	      
	      fillTransitivelyRequiredProjects(curJavaProject, requiredProjects);
	    }
	  }

  /**
   * a Finds a Java project with the given name.
   * 
   * @param projectName The name of the Java project
   * @return The Java project, or null if it cannot be found or if it does not
   *         have the Java nature
   */
  public static IJavaProject findJavaProject(String projectName) {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = workspaceRoot.getProject(projectName);
    
    if (!project.exists()) {
      return null;
    }
    
    try {
      if (!hasNature(project, JavaCore.NATURE_ID)) {
        return null;
      }
    } catch (CoreException e) {
      // Thrown if the project doesn't exist or is not open
      return null;
    }
    
    return JavaCore.create(project);
  }
  
  /**
   * Returns <code>true</code> if the project is accessible and has the
   * specified nature ID.
   * 
   * @param project
   * @param natureId
   * 
   * @return <code>true</code> if the project is accessible and has the
   *         specified nature ID
   * @throws CoreException
   */
  public static boolean hasNature(IProject project, String natureId)
      throws CoreException {
    return project.isAccessible() && project.hasNature(natureId);
  }

  /**
   * FIXME - Were it not for the super source stuff, we would need this method.
   * Can't we provide a way for users to state which folders are super-source,
   * etc?
   */
  public static List<IRuntimeClasspathEntry> getGWTRuntimeProjectSourceEntries(
      IJavaProject project, boolean includeTestSourceEntries)
      throws CoreException {

    assert (isGWTRuntimeProject(project) && project.exists());

    String projectName = project.getProject().getName();
    List<IRuntimeClasspathEntry> sourceEntries = new ArrayList<IRuntimeClasspathEntry>();

    IClasspathEntry[] gwtUserJavaProjClasspathEntries = null;

    try {
      gwtUserJavaProjClasspathEntries = project.getRawClasspath();
    } catch (JavaModelException e) {
      throw new CoreException(new Status(Status.ERROR, GWTActivator.PLUGIN_ID, "Cannot extract raw classpath from " + projectName
          + " project."));
    }

    Set<IPath> absoluteSuperSourcePaths = new HashSet<IPath>();

    for (IClasspathEntry curClasspathEntry : gwtUserJavaProjClasspathEntries) {
      if (curClasspathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
        IPath sourcePath = curClasspathEntry.getPath();

        if (isJavadocPath(sourcePath)) {
          // Ignore javadoc paths.
          continue;
        }

        if (isTestPath(sourcePath)
            && !includeTestSourceEntries) {
          // Ignore test paths, unless it is specified explicitly that we should
          // include them.
          continue;
        }

        sourceEntries.add(JavaRuntime.newArchiveRuntimeClasspathEntry(sourcePath));

        // Figure out the location of the super source path.

        IPath absoluteSuperSourcePath = sourcePath.removeLastSegments(1).append(
            SUPER_SOURCE_FOLDER_NAME);
        IPath relativeSuperSourcePath = absoluteSuperSourcePath.removeFirstSegments(1);

        if (absoluteSuperSourcePaths.contains(absoluteSuperSourcePath)) {
          // I've already included this path.
          continue;
        }

        if (project.getProject().getFolder(relativeSuperSourcePath).exists()) {
          /*
           * We've found the super source path, and we've not added it already.
           * The existence test uses a relative path, but the creation of a
           * runtime classpath entry requires an absolute path.
           */
          sourceEntries.add(JavaRuntime.newArchiveRuntimeClasspathEntry(absoluteSuperSourcePath));
          absoluteSuperSourcePaths.add(absoluteSuperSourcePath);
        }

        IPath absoluteTestSuperSourcePath = sourcePath.removeLastSegments(1).append(
            TEST_SUPER_SOURCE_FOLDER_NAME);
        IPath relativeTestSuperSourcePath = absoluteTestSuperSourcePath.removeFirstSegments(1);
        if (absoluteSuperSourcePaths.contains(absoluteTestSuperSourcePath)) {
          // I've already included this path.
          continue;
        }

        if (includeTestSourceEntries
            && project.getProject().getFolder(relativeTestSuperSourcePath).exists()) {
          /*
           * We've found the super source path, and we've not added it already.
           * The existence test uses a relative path, but the creation of a
           * runtime classpath entry requires an absolute path.
           */
          sourceEntries.add(JavaRuntime.newArchiveRuntimeClasspathEntry(absoluteTestSuperSourcePath));
          absoluteSuperSourcePaths.add(absoluteTestSuperSourcePath);
        }
      }
    }

    if (absoluteSuperSourcePaths.isEmpty()) {
      System.err.println("There were no super source folders found for the project '" + project.getProject().getName() + "'");
    }

    return sourceEntries;
  }
  
  private static final String JAVADOC_SOURCE_FOLDER_NAME = "javadoc";
  
  private static boolean isJavadocPath(IPath path) {
	    return (JAVADOC_SOURCE_FOLDER_NAME.equals(path.lastSegment()));
	  }

  
  private static final String SUPER_SOURCE_FOLDER_NAME = "super";

  private static final String TEST_SUPER_SOURCE_FOLDER_NAME = "test-super";

  static final String GWT_DEV_FALLBACK_PROJECT_NAME = "gwt-dev";
  
  private static final String GWT_USER_PROJECT_NAME = "gwt-user";
  
  public static boolean isGWTRuntimeProject(IJavaProject project) {
	    String projectName = project.getProject().getName();
	    return (GWT_USER_PROJECT_NAME.equals(projectName)
	        || projectName.equals(GWT_DEV_FALLBACK_PROJECT_NAME));
	  }
  
  
  private ClasspathUtilities() {
  }
  
}
