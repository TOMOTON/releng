/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation, Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.buckminster.ide.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.buckminster.cmdline.Option;
import org.eclipse.buckminster.cmdline.OptionDescriptor;
import org.eclipse.buckminster.cmdline.OptionValueType;
import org.eclipse.buckminster.cmdline.SimpleErrorExitException;
import org.eclipse.buckminster.core.commands.WorkspaceCommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNatureDescriptor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class ToggleNature extends WorkspaceCommand {

	static private final OptionDescriptor OPTION_INCLUDE = new OptionDescriptor('I', "include", OptionValueType.REQUIRED);
	
	static private final OptionDescriptor OPTION_EXCLUDE = new OptionDescriptor('E', "exclude", OptionValueType.REQUIRED);
	
	protected Pattern[] includes;
	protected Pattern[] excludes;
	protected String natureId;


	public Pattern[] getIncludes() {
		return includes;
	}

	public void setIncludes(Pattern[] includes) {
		this.includes = includes;
	}

	public Pattern[] getExcludes() {
		return excludes;
	}

	public void setExcludes(Pattern[] excludes) {
		this.excludes = excludes;
	}

	public String getNatureId() {
		return natureId;
	}

	public void setNatureId(String natureId) {
		this.natureId = natureId;
	}

	@Override
	protected void getOptionDescriptors(List<OptionDescriptor> appendHere) throws Exception {
		super.getOptionDescriptors(appendHere);
		appendHere.add(OPTION_INCLUDE);
		appendHere.add(OPTION_EXCLUDE);		
	}

	@Override
	protected void handleOption(Option option) throws Exception {
		if (option.is(OPTION_INCLUDE)) {
			String[] parts = option.getValue().split(",");
			includes = new Pattern[parts.length];
			for(int i = 0; i < parts.length; i++) {
				includes[i] = Pattern.compile(asRegEx(parts[i]));
			}
		} else 
		if (option.is(OPTION_EXCLUDE)) {
			String[] parts = option.getValue().split(",");
			excludes = new Pattern[parts.length];
			for(int i = 0; i < parts.length; i++) {
				excludes[i] = Pattern.compile(asRegEx(parts[i]));
			}
		} else
			super.handleOption(option);
	}
	
	private String asRegEx(String pattern) {
		pattern = pattern.replaceAll("\\*", ".*");
		return "^" + pattern + "$";
	}

	@Override
	protected void handleUnparsed(String[] unparsed) throws Exception {
		if (unparsed.length > 1)
			throw new SimpleErrorExitException("Too many arguments"/*Messages.Too_many_arguments*/);
		if (unparsed.length < 1)
			throw new SimpleErrorExitException("Too few arguments"/*Messages.Too_few_arguments*/);
		setNatureId(unparsed[0]);
	}

	@Override
	protected int internalRun(IProgressMonitor monitor) throws Exception {
		if (natureId == null)
			throw new SimpleErrorExitException("Nature identifier argument missing.");
		monitor.beginTask("Toggling nature on projects...", 3);	
		IProjectNatureDescriptor[] descriptors = ResourcesPlugin.getWorkspace().getNatureDescriptors();
		monitor.worked(1); // work=2
		boolean known = false;
		for (IProjectNatureDescriptor descriptor: descriptors) {	
			if (descriptor.getNatureId().equals(natureId)) {
				known = true;
				break;
			}
		}
		if (!known) {
			throw new SimpleErrorExitException("Buckminster installation does not have nature '" + natureId + " 'registered!");
		}
		monitor.worked(1); // work=1
		List<IProject> projectList = new ArrayList<IProject>();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();		
		projectList.addAll(Arrays.asList(root.getProjects()));
		filterProjects(projectList);
		monitor.worked(1); // work=0
		for (IProject project: projectList) {
			toggleNature(project, natureId);
		}
		monitor.done();
		return 0;
	}
	
	private void filterProjects(List<IProject> IProjectList) {
		if (includes != null) {
			IProject[] IProjects = IProjectList.toArray(new IProject[IProjectList.size()]);
			for (IProject IProject: IProjects) {				
				boolean included = false;
				for (Pattern include: includes) {
					if(include.matcher(IProject.getName()).matches()) {
						included = true;
						break;
					}
				}
				if(!included) {
					IProjectList.remove(IProject);
				}
			}
		}
		if (excludes != null) {
			IProject[] IProjects = IProjectList.toArray(new IProject[IProjectList.size()]);
			for(IProject IProject: IProjects) {
				for(Pattern exclude: excludes) {
					if(exclude.matcher(IProject.getName()).matches()) {
						IProjectList.remove(IProject);
					}
				}
			}
		}
	}

	private void toggleNature(IProject project, String natureId) {
		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();
			for (int i = 0; i < natures.length; ++i) {
				if (natureId.equals(natures[i])) {
					// Remove the nature
					String[] newNatures = new String[natures.length - 1];
					System.arraycopy(natures, 0, newNatures, 0, i);
					System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
					description.setNatureIds(newNatures);
					project.setDescription(description, null);
					System.out.println(" - " + project.getName());
					return;
				}
			}
			// Add the nature
			String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = natureId;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
			System.out.println(" + " + project.getName());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
}
