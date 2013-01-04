package org.eclipse.buckminster.gwt.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.buckminster.cmdline.Option;
import org.eclipse.buckminster.cmdline.OptionDescriptor;
import org.eclipse.buckminster.cmdline.OptionValueType;
import org.eclipse.buckminster.cmdline.UsageException;
import org.eclipse.buckminster.core.commands.WorkspaceCommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.google.gwt.eclipse.core.ProcessUtilities;
import com.google.gwt.eclipse.core.SimpleGWTCompileRunner;
import com.google.gwt.eclipse.core.SimpleGWTCompileSettings;


public class GWTCompile extends WorkspaceCommand {

	private static final String DEFAULT_WAR_DIRECTORY = "war";
	
	static private final OptionDescriptor OPTION_WAR = new OptionDescriptor(
			'W', "war", OptionValueType.OPTIONAL);

	static private final OptionDescriptor OPTION_MODULE = new OptionDescriptor(
			'M', "module", OptionValueType.REQUIRED);

	private String projectName;
	
	private String warDirectory = DEFAULT_WAR_DIRECTORY;
	
	private String module;
	
	public String getWarDirectory() {
		return warDirectory;
	}

	public void setWarDirectory(String warDirectory) {
		this.warDirectory = warDirectory;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
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
		appendHere.add(OPTION_MODULE);
	}

	@Override
	protected void handleOption(Option option) throws Exception {

		if (option.is(OPTION_WAR)) {
			setWarDirectory(option.getValue());
		} else if (option.is(OPTION_MODULE)) {
			setModule(option.getValue());
		} else
			super.handleOption(option);
	}

	@Override
	protected void handleUnparsed(String[] unparsed) throws Exception {
		int len = unparsed.length;
		if (len > 1)
			throw new UsageException("Too many arguments");
		if (len == 1)
			setProjectName(unparsed[0]);
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
		System.err.println("(1)");
		int exitValue = 0;
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject(projectName);

		IJavaProject javaProject = JavaCore.create(project);
		if (!isJavaProjectNonNullAndExists(javaProject)) {
			return 1;
		}
		System.err.println("(2)");
		List<String> modules = new ArrayList<String>();
		modules.add(module);
		SimpleGWTCompileSettings settings = new SimpleGWTCompileSettings(modules);
		settings.setOutputStyle("OBFUSCATED");
		// 0 OBFUSCATED PRETTY DETAILED
		settings.setLogLevel("INFO");
		// 0 ERROR WARN INFO TRACE DEBUG SPAM ALL
		try {
			System.err.println("(3)");
			SimpleGWTCompileRunner.compile(javaProject, new Path(warDirectory), settings, System.out, new CompilerReceiver());
			System.err.println("(4)");
		} catch (Exception e) {
			System.err.println("(EXCEPTION)");
			e.printStackTrace();
			exitValue = 1;
		}
		return exitValue;
	}

	private class CompilerReceiver implements ProcessUtilities.IProcessReceiver {

		@Override
		public boolean hasDestroyedProcess() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void setProcess(Process process) {
			// TODO Auto-generated method stub
			
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
