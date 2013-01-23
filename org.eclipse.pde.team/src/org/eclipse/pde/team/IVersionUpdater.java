package org.eclipse.pde.team;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.osgi.framework.Version;

public interface IVersionUpdater {

	void update(IFile file, Version version, IProgressMonitor monitor) throws CoreException;

}
