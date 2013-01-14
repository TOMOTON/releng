package org.eclipse.pde.team;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface IVersionUpdater {

	void update(IFile file, String version, IProgressMonitor monitor) throws CoreException;

}
