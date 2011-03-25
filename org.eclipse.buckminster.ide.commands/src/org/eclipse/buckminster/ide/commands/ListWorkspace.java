package org.eclipse.buckminster.ide.commands;

import java.util.Arrays;

import org.eclipse.buckminster.core.commands.WorkspaceCommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;

public class ListWorkspace extends WorkspaceCommand {

	@Override
	protected int internalRun(IProgressMonitor monitor) throws Exception {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();		
		IProject[] projects = root.getProjects();
		String[] names = new String[projects.length + 1];
		int index = 0;
		names[index] = "/ (Workspace)";
		int maxLength = names[index].length();
		for(IProject project: projects) {			
			names[++index] = project.getName();
			int currentLength = names[index].length(); 
			if(currentLength > maxLength)
				maxLength = currentLength;
		}
		maxLength += 4;
		char[] spaces = new char[maxLength];
		Arrays.fill(spaces, ' ' );
		index = -1;
		for(String name: names) {
			System.out.print(name);
			System.out.print(new String(spaces, 0, maxLength - name.length()));
			if(index == -1) {
				System.out.println(root.getLocation().toOSString());
			} else {
				System.out.println(projects[index].getLocation().toOSString());
			}
			index++;
		}
		if(index == 0)
			System.out.println("<<EMPTY>>");
		return 0;
	}

}
