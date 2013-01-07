package com.google.gwt.eclipse.core.nature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

import com.google.gwt.eclipse.core.internal.GWTActivator;

/**
 * Identifies a Java project as a GWT project.
 */
public class GWTNature implements IProjectNature {

	public static final String NATURE_ID = GWTActivator.PLUGIN_ID + ".gwtNature";

	private IProject project;
	
	@Override
	public void configure() throws CoreException {
		//? Intentionally left blank.
	}

	@Override
	public void deconfigure() throws CoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}

}