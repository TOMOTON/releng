package org.eclipse.buckminster.gwt.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.buckminster.cmdline.Option;
import org.eclipse.buckminster.cmdline.OptionDescriptor;
import org.eclipse.buckminster.cmdline.OptionValueType;
import org.eclipse.buckminster.cmdline.SimpleErrorExitException;
import org.eclipse.buckminster.core.commands.WorkspaceCommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.Version;

import com.google.gwt.eclipse.core.ClasspathUtilities;
import com.google.gwt.eclipse.core.GWTSDKClasspathContainer;
import com.google.gwt.eclipse.core.ProcessUtilities;
import com.google.gwt.eclipse.core.SimpleGWTCompileRunner;
import com.google.gwt.eclipse.core.SimpleGWTCompileSettings;


public class GWTCompile extends WorkspaceCommand {

	private static final String DEFAULT_WAR_DIRECTORY = "war";
	
	private enum OutputStyle {
		OBFUSCATED, PRETTY, DETAILED;
	}

	private static final OutputStyle DEFAULT_OUTPUT_STYLE = OutputStyle.OBFUSCATED;
	
	private enum LogLevel {
		ERROR, WARN, INFO, TRACE, DEBUG, SPAM, ALL;
	}
	
	private static final LogLevel DEFAULT_LOG_LEVEL = LogLevel.INFO;
	
	static private final OptionDescriptor OPTION_WAR = new OptionDescriptor(
			'W', "war", OptionValueType.REQUIRED);

	static private final OptionDescriptor OPTION_OUTPUT_STYLE = new OptionDescriptor(
			'O', "outputStyle", OptionValueType.REQUIRED);

	static private final OptionDescriptor OPTION_LOG_LEVEL = new OptionDescriptor(
			'L', "logLevel", OptionValueType.REQUIRED);

	static private final OptionDescriptor OPTION_STRICT = new OptionDescriptor(
			'S', "strict", OptionValueType.OPTIONAL);

	static private final OptionDescriptor OPTION_DEPLOY = new OptionDescriptor(
			'D', "deploy", OptionValueType.REQUIRED);

	static private final OptionDescriptor OPTION_EXTRA = new OptionDescriptor(
			'E', "extra", OptionValueType.REQUIRED);

	static private final OptionDescriptor OPTION_GWT = new OptionDescriptor(
			'G', "gwt", OptionValueType.REQUIRED);
	
	static private final OptionDescriptor OPTION_PROJECT = new OptionDescriptor(
			'P', "project", OptionValueType.REQUIRED);

	private final NullCompilerReceiver NULL_PROCESS_RECEIVER = new NullCompilerReceiver();
	
	private String projectName;
	
	private String warDirectory = DEFAULT_WAR_DIRECTORY;
	
	private OutputStyle outputStyle = DEFAULT_OUTPUT_STYLE;
	
	private LogLevel logLevel = DEFAULT_LOG_LEVEL;
	
	boolean strict = true;
	
	private String deployDir;

	private String extraDir;
	
	private Version gwtVersion;
	
	private String[] modules;
	
	public String getWarDirectory() {
		return warDirectory;
	}

	public void setWarDirectory(String warDirectory) {
		this.warDirectory = warDirectory;
	}
	
	public String getOutputStyle() {
		return outputStyle.toString();
	}

	public void setOutputStyle(String outputStyle) {
		this.outputStyle = OutputStyle.valueOf(outputStyle);
	}

	public String getLogLevel() {
		return logLevel.toString();
	}

	public void setLogLevel(String logLevel) {
		this.logLevel = LogLevel.valueOf(logLevel);
	}

	public boolean isStrict() {
		return strict;
	}

	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	public String getDeployDir() {
		return deployDir;
	}

	public void setDeployDir(String deployPath) {
		this.deployDir = deployPath;
	}

	public String getExtraDir() {
		return extraDir;
	}

	public void setExtraDir(String extraDir) {
		this.extraDir = extraDir;
	}
	
	public Version getGwtOSGiVersion() {
		return gwtVersion;
	}

	public void setGwtVersion(String gwtVersion) {
		this.gwtVersion = Version.parseVersion(gwtVersion);
	}

	public String[] getModules() {
		return modules;
	}

	public void setModules(String[] modules) {
		this.modules = modules;
	}

    public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	@Override
	protected void getOptionDescriptors(List<OptionDescriptor> appendHere) throws Exception {
		super.getOptionDescriptors(appendHere);
		appendHere.add(OPTION_WAR);
		appendHere.add(OPTION_OUTPUT_STYLE);
		appendHere.add(OPTION_LOG_LEVEL);
		appendHere.add(OPTION_STRICT);
		appendHere.add(OPTION_DEPLOY);
		appendHere.add(OPTION_EXTRA);
		appendHere.add(OPTION_GWT);			
		appendHere.add(OPTION_PROJECT);
	}

	@Override
	protected void handleOption(Option option) throws Exception {
		if (option.is(OPTION_WAR)) {
			setWarDirectory(option.getValue());
		} else if (option.is(OPTION_OUTPUT_STYLE)) {
			setOutputStyle(option.getValue());
		} else if (option.is(OPTION_LOG_LEVEL)) {
			setLogLevel(option.getValue());		
		} else if (option.is(OPTION_STRICT)) {
			boolean value = true;
			try {
				value = Boolean.parseBoolean(option.getValue());
			} catch (Exception ignore) {}
			setStrict(value);
		} else if (option.is(OPTION_DEPLOY)) {
			setDeployDir(option.getValue());	
		} else if (option.is(OPTION_EXTRA)) {
			setExtraDir(option.getValue());	
		} else if (option.is(OPTION_GWT)) {
			setGwtVersion(option.getValue());				
		} else if (option.is(OPTION_PROJECT)) {
			setProjectName(option.getValue());
		} else
			super.handleOption(option);
	}

	@Override
	protected void handleUnparsed(String[] unparsed) throws Exception {
		setModules(unparsed);
	}
	
	  /**
	   * Returns <code>true</code> if the given IJavaProject is not null, and
	   * <code>javaProject.exists()</code> is true.
	   */
	  public static boolean isJavaProjectNonNullAndExists(IJavaProject javaProject) {
	    return (javaProject != null && javaProject.exists());
	  }

	@Override
	protected int internalRun(IProgressMonitor monitor) throws Exception {
		if(projectName == null)
			throw new SimpleErrorExitException("Missing project name!");
		if(modules == null || modules.length == 0)
			throw new SimpleErrorExitException("At least one GWT module needs to be specified!");			
		System.err.println("(1)");
		int exitValue = 0;
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject(projectName);

		IJavaProject javaProject = JavaCore.create(project);
		if (!isJavaProjectNonNullAndExists(javaProject)) {
			return 1;
		}
		System.err.println("(2)");
		List<String> moduleList = new ArrayList<String>();
		moduleList.addAll(Arrays.asList(modules));
		SimpleGWTCompileSettings settings = new SimpleGWTCompileSettings(moduleList);
		settings.setOutputStyle(getOutputStyle());
		// 0 OBFUSCATED PRETTY DETAILED
		settings.setLogLevel(getLogLevel());
		// 0 ERROR WARN INFO TRACE DEBUG SPAM ALL
		String extraArgs = "";
		if(isStrict()) {
			extraArgs += "-strict ";
		}
		if(deployDir != null) {
			extraArgs += "-deploy " + deployDir + ' ';
		}
		if(extraDir != null) {
			extraArgs += "-extra " + extraDir + ' ';
		}		
		settings.setExtraArgs(extraArgs);
		if(gwtVersion != null) {
			IClasspathContainer classpathContainer =  GWTSDKClasspathContainer.resolveClasspathContainer(GWTSDKClasspathContainer.CONTAINER_PATH, javaProject, getGwtOSGiVersion());
			if(classpathContainer == null) {
				throw new SimpleErrorExitException("GWT SDK version '" + getGwtOSGiVersion() + "' could not be found!");	
			}
			System.out.println("Switching to GWT SDK version '" + getGwtOSGiVersion() + "'.");
		    JavaCore.setClasspathContainer(GWTSDKClasspathContainer.CONTAINER_PATH,
			        new IJavaProject[] { javaProject },
			        new IClasspathContainer[] { classpathContainer }, null);
		}
		try {
			System.err.println("(3)");
			SimpleGWTCompileRunner.compile(javaProject, new Path(warDirectory), settings, System.out, NULL_PROCESS_RECEIVER);
			System.err.println("(4)");
		} catch (Exception e) {
			System.err.println("(EXCEPTION)");
			e.printStackTrace();
			exitValue = 1;
		}
		return exitValue;
	}

	private class NullCompilerReceiver implements ProcessUtilities.IProcessReceiver {

		@Override
		public boolean hasDestroyedProcess() {
			return false;
		}

		@Override
		public void setProcess(Process process) {
			//? Intentionally left blank.			
		}
		
	}

	public void go() throws Exception {
		try {
			internalRun(getProgressProvider().getDefaultMonitor());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
