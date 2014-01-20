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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

import com.google.gwt.eclipse.core.internal.GWTActivator;

/**
 * Helper methods for creating Java processes.
 */
@SuppressWarnings("restriction")
public class ProcessUtilities {

  /**
   * Receives {@link Process} instances.
   * 
   * The caller does not guarantee any thread-safety.
   */
  public interface IProcessReceiver {

    /**
     * @return true if the implementor has destroyed the process
     */
    boolean hasDestroyedProcess();

    void setProcess(Process process);
  }

  /**
   * The list of potential filenames for the javac executable.
   */
  private static final String[] CANDIDATE_JAVAC_FILES = {"javac", "javac.exe"};

  /**
   * Computes the fully qualified path to the javac executable that located in
   * the JRE/JDK used by this project.
   * 
   * @param javaProject
   * @return the path to the JRE/JDK javac used on this project
   * @throws CoreException
   */
  public static String computeJavaCompilerExecutableFullyQualifiedPath(
      IJavaProject javaProject) throws CoreException {
    return computeJavaCompilerExecutablePathFromJavaExecutablePath(computeJavaExecutableFullyQualifiedPath(javaProject));
  }

  /**
   * Computes the fully qualified path to the java executable for the JRE/JDK
   * used by this project.
   * 
   * @param javaProject
   * @return the path to the JRE/JDK java (executable) used on this project
   * @throws CoreException
   */
  public static String computeJavaExecutableFullyQualifiedPath(
      IJavaProject javaProject) throws CoreException {
    IVMInstall projectVMInstall = JavaRuntime.getVMInstall(javaProject);

    if (projectVMInstall == null) {
      throw new CoreException(
          new Status(
              Status.ERROR,
              GWTActivator.PLUGIN_ID,
              "Unable to locate the JVM for project "
                  + javaProject.getElementName()
                  + ". Please verify that you have a project-level JVM installed by inspecting your project's build path."));
    }

    return getJavaExecutableForVMInstall(projectVMInstall);
  }

  /**
   * Return the path to the java executable for Eclipse's JVM.
   * 
   * @return the fully qualified path to the java executable
   * @throws CoreException if Eclipse's JVM could not be detected
   */
  public static String findJavaExecutableForEclipse() throws CoreException {

    String javaHomeProp = System.getProperty("java.home");
    File javaHomeDir = null;

    if (javaHomeProp != null) {
      javaHomeDir = new File(javaHomeProp);
    }

    if (javaHomeDir == null || !javaHomeDir.exists()) {
      throw new CoreException(
          new Status(
              Status.ERROR,
              GWTActivator.PLUGIN_ID,
              "Cannot read the java.home property - unable detect the JVM that Eclipse is running on."));
    }

    File javaExecutable = StandardVMType.findJavaExecutable(javaHomeDir);

    if (javaExecutable == null || !javaExecutable.exists()) {
      throw new CoreException(
          new Status(
              Status.ERROR,
              GWTActivator.PLUGIN_ID,
              "Unable to find a java executable for the JVM that Eclipse is running on (located at "
                  + javaHomeDir.getAbsolutePath()
                  + "). Please verify that this JVM is installed properly."));
    }

    return javaExecutable.getAbsolutePath();
  }
  
  /**
   * Launch the process specified in the commands and wait for it to terminate.
   * 
   * @param commands commands to pass to the {@link ProcessBuilder}
   * @param workingDir directory to use as the working directory
   * @param additionalPaths list of additional directories to be appended to the
   *          PATH environmental variable
   * @param outputStream output stream to receive process output
   * @param processReceiver optional, will be given the process right after it
   *          is started
   * @return process exit code
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  public static int launchProcessAndWaitFor(List<String> commands,
      File workingDir, final List<String> additionalPaths,
      final OutputStream outputStream, IProcessReceiver processReceiver)
      throws InterruptedException, IOException {
    ProcessBuilder pb = new ProcessBuilder(commands);
    pb.directory(workingDir);
    pb.redirectErrorStream(true);
    moveClasspathArgToEnvironmentVariable(commands, pb);

    // if given a non-null, non-empty list of paths, then append to the PATH
    // environmental variable
    if (additionalPaths != null && additionalPaths.size() >= 1) {
      StringBuilder newPathEnvVar = new StringBuilder(pb.environment().get(
          "PATH"));
      // for each additional path, add it- if it isn't already in the path list
      for (String path : additionalPaths) {
        // if this additional path isn't already in the new path environment
        // variable
        String[] existingPaths = newPathEnvVar.toString().split(
            java.io.File.pathSeparatorChar + "");
        boolean pathAlreadyInPATH = false;
        for (String existingPath : existingPaths) {
          if (path.equals(existingPath)) {
            pathAlreadyInPATH = true;
          }
        }
        if (!pathAlreadyInPATH) {
          // append the new path onto newPathEnvVar
          newPathEnvVar.append(java.io.File.pathSeparatorChar);
          newPathEnvVar.append(path);
        }
      }
      // finally, set the evn variable on the process builder
      pb.environment().put("PATH", newPathEnvVar.toString());
    }

    Process process = null;
    Thread t = null;
    int processExitCode = -1;

    try {
      process = pb.start();
      if (processReceiver != null) {
        processReceiver.setProcess(process);
      }

      // Local class to read the output of the process and write it to the
      // output stream
      class OutputPump implements Runnable {

        Process process;

        OutputPump(Process p) {
          process = p;
        }

        public void run() {
          BufferedReader processReader = new BufferedReader(
              new InputStreamReader(process.getInputStream()));
          PrintWriter outputWriter = new PrintWriter(outputStream, true);
          try {
            String line = null;
            while ((line = processReader.readLine()) != null) {
              outputWriter.println(line);
            }
          } catch (IOException e) {
            // The "Stream closed" exception is common when we destroy the
            // process (e.g. when the user requests to terminate a GWT compile)
            if (!e.getMessage().contains("Stream closed")) {
              e.printStackTrace();
            }
          }
        }
      }
      t = new Thread(new OutputPump(process), "Process Output Pump");
      t.start();

      // Wait for process to complete
      processExitCode = process.waitFor();
    } catch (InterruptedException ie) {
      /*
       * If the thread that called this method is interrupted while waiting for
       * process.waitFor to return, ensure that the process is terminated and
       * that its file handles have been cleaned up.
       */
      cleanupProcess(process);

      // Rethrow the original exception
      throw ie;
    } finally {
      if (t != null) {
        // Wait for this thread to complete before returning from the method.
        try {
          t.join();
          // Close all of the process' streams, and destroy the process
          cleanupProcess(process);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    return processExitCode;
  }

  /**
   * Launch the process specified in the commands and wait for it to terminate.
   * 
   * @param commands commands to pass to the {@link ProcessBuilder}
   * @param workingDir directory to use as the working directory
   * @param outputStream output stream to receive process output
   * @param processReceiver optional, will be given the process right after it
   *          is started
   * @return process exit code
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  public static int launchProcessAndWaitFor(List<String> commands,
      File workingDir, final OutputStream outputStream,
      IProcessReceiver processReceiver) throws InterruptedException,
      IOException {
    return launchProcessAndWaitFor(commands, workingDir, null, outputStream,
        processReceiver);
  }

  /**
   * Closes the process' input stream, output stream, and error stream, and
   * finally destroys the process by calling <code>destroy()</code>.
   * 
   * @param p the process to cleanup
   */
  private static void cleanupProcess(Process p) {
    if (p == null) {
      return;
    }

    try {
      if (p.getInputStream() != null) {
        p.getInputStream().close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      if (p.getOutputStream() != null) {
        p.getOutputStream().close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      if (p.getErrorStream() != null) {
        p.getErrorStream().close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    p.destroy();
  }

  private static String computeJavaCompilerExecutablePathFromJavaExecutablePath(
      String fullyQualifiedJavaExecutableSystemPath) {

    assert (fullyQualifiedJavaExecutableSystemPath != null);

    IPath javaExecutablePath = new Path(fullyQualifiedJavaExecutableSystemPath);
    File javaExecutableFile = javaExecutablePath.toFile();
    File javaExecutableDir = javaExecutableFile.getParentFile();

    File javaCompilerExecutableFile = findJavaCompilerExecutableInDir(javaExecutableDir);

    if (javaCompilerExecutableFile != null) {
      return javaCompilerExecutableFile.getAbsolutePath();
    }

    /*
     * If we didn't find a javac executable as a peer of the java executable,
     * then the java executable was located in <jdk home>/jre/bin.
     */
    if (javaExecutablePath.segmentCount() < 3) {
      return null;
    }

    File jdkHomeBinDir = javaExecutablePath.removeLastSegments(3).append("bin").toFile();
    javaCompilerExecutableFile = findJavaCompilerExecutableInDir(jdkHomeBinDir);

    if (javaCompilerExecutableFile != null) {
      return javaCompilerExecutableFile.getAbsolutePath();
    }

    return null;
  }

  private static File findJavaCompilerExecutableInDir(File dir) {
    if (dir == null || !dir.exists() || !dir.isDirectory()) {
      return null;
    }

    for (String candidateJavaCompilerFileName : CANDIDATE_JAVAC_FILES) {
      File javaCompilerFile = new File(dir, candidateJavaCompilerFileName);

      if (javaCompilerFile.exists()) {
        return javaCompilerFile;
      }
    }

    return null;
  }

  private static String getJavaExecutableForVMInstall(IVMInstall vmInstall)
      throws CoreException {

    assert (vmInstall != null);

    File vmInstallLocation = vmInstall.getInstallLocation();

    if (vmInstallLocation == null) {
      throw new CoreException(
          new Status(
              Status.ERROR,
              GWTActivator.PLUGIN_ID,
              "Unable to determine the path for the JVM "
                  + vmInstall.getName()
                  + ". Please verify that this JVM is installed properly by inspecting your project's build path."));
    }

    File javaExecutable = StandardVMType.findJavaExecutable(vmInstallLocation);

    if (javaExecutable == null || !javaExecutable.exists()) {
      throw new CoreException(
          new Status(
              Status.ERROR,
              GWTActivator.PLUGIN_ID,
              "Unable to find a java executable for the JVM   "
                  + vmInstall.getName()
                  + " located at "
                  + vmInstallLocation.getAbsolutePath()
                  + ". Please verify that this JVM is installed properly by inspecting your project's build path."));
    }

    return javaExecutable.getAbsolutePath();
  }

  /*
   * Put classpath argument in an environment variable so we don't overflow the
   * process command-line buffer on Windows.
   */
  private static void moveClasspathArgToEnvironmentVariable(
      List<String> commandArgs, ProcessBuilder pb) {
    int cpFlagIndex = commandArgs.indexOf("-cp");
    if (cpFlagIndex == -1) {
      cpFlagIndex = commandArgs.indexOf("-classpath");
    }

    if (cpFlagIndex > -1 && cpFlagIndex < commandArgs.size() - 1) {
      Map<String, String> env = pb.environment();
      String classpath = commandArgs.get(cpFlagIndex + 1);
      env.put("CLASSPATH", classpath);

      commandArgs.remove(cpFlagIndex);
      commandArgs.remove(cpFlagIndex);
    }
  }
}
