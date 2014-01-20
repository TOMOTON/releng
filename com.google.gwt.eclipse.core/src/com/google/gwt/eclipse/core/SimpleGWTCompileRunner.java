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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import com.google.gwt.eclipse.core.internal.GWTActivator;


/**
 * Performs a GWT compilation on a project.
 */
public class SimpleGWTCompileRunner {
	
  private static final Pattern EXTRACT_QUOTED_ARGS_PATTERN = Pattern.compile("^([\"'])(.*)([\"'])$");

  private static final Pattern SPLIT_QUOTED_ARGS_PATTERN = Pattern.compile("[^\\s\"]+|\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"");

  /**
   * @param processReceiver optional, receives the process after it is started
   */
  public static void compile(IJavaProject javaProject, IPath warLocation,
      SimpleGWTCompileSettings settings, OutputStream consoleOutputStream,
      ProcessUtilities.IProcessReceiver processReceiver) throws IOException,
      InterruptedException, CoreException, OperationCanceledException {
    IProject project = javaProject.getProject();
    if (settings.getEntryPointModules().isEmpty()) {
      // Nothing to compile, so just return.
      return;
    }
	List<String> commandLine = computeCompilerCommandLine(javaProject, warLocation, settings);
	if("DEBUG".equals(settings.getLogLevel())) {
		dumpCommandLine(commandLine);
	}
    int processStatus = ProcessUtilities.launchProcessAndWaitFor(
        commandLine,
        project.getLocation().toFile(), consoleOutputStream, processReceiver);
    
    /*
     * Do a refresh on the war folder if it's in the workspace. This ensures
     * that Eclipse sees the generated artifacts from the GWT compile, and
     * doesn't complain about stale resources during subsequent file searches.
     */
    if (warLocation != null) {
      for (IContainer warFolder : ResourcesPlugin.getWorkspace().getRoot().findContainersForLocation(
          warLocation)) {
        warFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
      }
    }

    if (processStatus != 0) {
      if (processReceiver != null && processReceiver.hasDestroyedProcess()) {
        PrintWriter printWriter = new PrintWriter(consoleOutputStream);
        printWriter.println("GWT compilation terminated by the user.");
        printWriter.flush();
        throw new OperationCanceledException();
      } else {
        throw new CoreException(new Status(IStatus.ERROR, GWTActivator.PLUGIN_ID, "GWT compilation failed!"));
      }
    }
  }
  
  private static void dumpCommandLine(List<String> commandLine) {
		System.out.print("Invoking GWT compiler with \"");
		char spacer = 0;
		for(String string: commandLine) {
			if(spacer == 0) {
				spacer = ' ';				
			} else {
				System.out.print(spacer);
			}
			System.out.print(string);
		}
		
		System.out.println("\"");
  }

  public static String computeTaskName(IProject project) {
    return project.getName() + " - GWT Compile";
  }

  /**
   * Computes a GWT compiler-tailored list of classpath entries for the given
   * Java project.
   */
  /*
   * This is package-scoped so it can be JUnit tested.
   */
  static List<IRuntimeClasspathEntry> computeClasspath(IJavaProject javaProject)
      throws CoreException {
    // Get the unresolved runtime classpath
    IRuntimeClasspathEntry[] unresolvedRuntimeClasspath = JavaRuntime.computeUnresolvedRuntimeClasspath(javaProject);
    List<IRuntimeClasspathEntry> resolvedRuntimeClasspath = new ArrayList<IRuntimeClasspathEntry>();

    for (IRuntimeClasspathEntry unresolvedClasspathEntry : unresolvedRuntimeClasspath) {
      if (JavaRuntime.isVMInstallReference(unresolvedClasspathEntry)) {
        continue;
      }

      // Add resolved entries for this unresolved entry
      resolvedRuntimeClasspath.addAll(Arrays.asList(JavaRuntime.resolveRuntimeClasspathEntry(
          unresolvedClasspathEntry, javaProject)));
    }

    /*
     * Prepend the resolved classpath with the source entries (parallels the
     * launch config's ordering of entries)
     */
    try {
      resolvedRuntimeClasspath.addAll(
          0,
          ClasspathUtilities.getGWTSourceFolderPathsFromProjectAndDependencies(
              javaProject, false));
    } catch (CoreException e) {
      throw new CoreException(new Status(IStatus.ERROR, GWTActivator.PLUGIN_ID,
          e.getLocalizedMessage(), e));
    }

    return resolvedRuntimeClasspath;
  }

  private static String computeCompilerClassName(IJavaProject javaProject) throws JavaModelException {
    return "com.google.gwt.dev.Compiler";
  }

  /**
   * Computes the command line arguments required to invoke the GWT compiler for
   * this project.
   */
  private static List<String> computeCompilerCommandLine(
      IJavaProject javaProject, IPath warLocation, SimpleGWTCompileSettings settings)
      throws CoreException {
    List<String> commandLine = new ArrayList<String>();
    // add the fully qualified path to java
    String javaExecutable = ProcessUtilities.computeJavaExecutableFullyQualifiedPath(javaProject);
    commandLine.add(javaExecutable);

//    commandLine.addAll(GWTLaunchConfiguration.computeCompileDynamicVMArgsAsList(
//        javaProject, false));

    commandLine.addAll(splitArgs(VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(
        settings.getVmArgs())));

    // add the classpath
    commandLine.add("-cp");
    commandLine.add(ClasspathUtilities.flattenToClasspathString(computeClasspath(javaProject)));

    // add the GWT compiler class name
    commandLine.add(computeCompilerClassName(javaProject));

    // add the GWT compiler options
    commandLine.addAll(computeCompilerOptions(warLocation, settings));

    // add the startup modules
    commandLine.addAll(settings.getEntryPointModules());

    return commandLine;
  }

  private static List<String> computeCompilerOptions(IPath warLocation,
      SimpleGWTCompileSettings settings) {
    List<String> options = new ArrayList<String>();

    String logLevel = settings.getLogLevel();
    if (logLevel != null && logLevel.trim().length() > 0) {
      options.add("-logLevel");
      options.add(logLevel);
    }

    String outputStyle = settings.getOutputStyle();
    if (outputStyle != null && outputStyle.trim().length() > 0) {
      options.add("-style");
      options.add(outputStyle);
    }

    if (warLocation != null) {
      options.add("-war");
      options.add(warLocation.toOSString());
    }

    options.addAll(splitArgs(settings.getExtraArgs()));

    return options;
  }

  private static List<String> splitArgs(String args) {
    List<String> options = new ArrayList<String>();

    if (args != null && args.trim().length() > 0) {
      Matcher matcher = SPLIT_QUOTED_ARGS_PATTERN.matcher(args);
      while (matcher.find()) {
        // Strip leading and trailing quotes from the arg
        String arg = matcher.group();
        Matcher qmatcher = EXTRACT_QUOTED_ARGS_PATTERN.matcher(arg);
        if (qmatcher.matches()) {
          options.add(qmatcher.group(2));
        } else {
          options.add(arg);
        }
      }
    }

    return options;
  }
  
}
