package org.eclipse.buckminster.ide.commands;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.buckminster.cmdline.Option;
import org.eclipse.buckminster.cmdline.OptionDescriptor;
import org.eclipse.buckminster.cmdline.OptionValueType;
import org.eclipse.buckminster.cmdline.SimpleErrorExitException;
import org.eclipse.buckminster.core.commands.WorkspaceCommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;

public class ImportProjects extends WorkspaceCommand {

	/**
	 * The name of the folder containing metadata information for the workspace.
	 */
	public static final String METADATA_FOLDER = ".metadata"; //$NON-NLS-1$

	static private final OptionDescriptor OPTION_COPY =	new OptionDescriptor('C', "copy", OptionValueType.NONE);

	static private final OptionDescriptor OPTION_OVERWRITE = new OptionDescriptor('O', "overwrite", OptionValueType.NONE);

	static private final OptionDescriptor OPTION_INCLUDE = new OptionDescriptor('I', "include", OptionValueType.REQUIRED);
	
	static private final OptionDescriptor OPTION_EXCLUDE = new OptionDescriptor('E', "exclude", OptionValueType.REQUIRED);
	
	protected String sourceDirectory;
	protected boolean copy = false;
	protected boolean overwrite = false;
	protected Pattern[] includes;
	protected Pattern[] excludes;
	
	public String getSourceDirectory() {
		return sourceDirectory;
	}

	public void setSourceDirectory(String sourceDirectory) {
		this.sourceDirectory = sourceDirectory;
	}

	public boolean isCopy() {
		return copy;
	}

	public void setCopy(boolean copy) {
		this.copy = copy;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

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

	@Override
	protected void getOptionDescriptors(List<OptionDescriptor> appendHere) throws Exception {
		super.getOptionDescriptors(appendHere);
		appendHere.add(OPTION_COPY);
		appendHere.add(OPTION_OVERWRITE);
		appendHere.add(OPTION_INCLUDE);
		appendHere.add(OPTION_EXCLUDE);		
	}

	@Override
	protected void handleOption(Option option) throws Exception {
		if (option.is(OPTION_COPY))
			setCopy(true);
		else
		if (option.is(OPTION_OVERWRITE))
			setOverwrite(true);
		else
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
		setSourceDirectory(unparsed[0]);
	}

	@Override
	protected int internalRun(IProgressMonitor monitor) throws Exception {
		if(sourceDirectory == null)
			throw new SimpleErrorExitException("Source directory argument missing.");
		monitor.beginTask("Importing projects...", 2);		
		File directory = new File(sourceDirectory);	
		if(!directory.isDirectory())
			throw new SimpleErrorExitException("Path '" + sourceDirectory + "' is not a directory.");		
		// Enumerate and import.
		List<ProjectRecord> projectRecordList = enumerateProjectsRecordsFrom(directory, new SubProgressMonitor(monitor, 1));
		monitor.worked(1); // work = 1
		filterProjects(projectRecordList);
		createProjects(projectRecordList, new SubProgressMonitor(monitor, projectRecordList.size()));
		if(copy) {
			System.err.println("Copy not implemented.");
		}
		monitor.worked(1); // work = 0
		outReport(projectRecordList);
		monitor.done();		
		return 0;
	}
	
	private void filterProjects(List<ProjectRecord> projectRecordList) {
		if(includes != null) {
			ProjectRecord[] projectRecords = projectRecordList.toArray(new ProjectRecord[projectRecordList.size()]);
			for(ProjectRecord projectRecord: projectRecords) {				
				boolean included = false;
				for(Pattern include: includes) {
					if(include.matcher(projectRecord.getProjectName()).matches()) {
						included = true;
						break;
					}
				}
				if(!included) {
					projectRecordList.remove(projectRecord);
				}
			}
		}
		if(excludes != null) {
			ProjectRecord[] projectRecords = projectRecordList.toArray(new ProjectRecord[projectRecordList.size()]);
			for(ProjectRecord projectRecord: projectRecords) {
				for(Pattern exclude: excludes) {
					if(exclude.matcher(projectRecord.getProjectName()).matches()) {
						projectRecordList.remove(projectRecord);
					}
				}
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			List<ProjectRecord> projectRecordList = new ArrayList<ProjectRecord>();
			ProjectRecord projectRecord1 = new ProjectRecord("ch.ringler.a.b.c");
			ProjectRecord projectRecord2 = new ProjectRecord("ch.ringler.x.z");
			ProjectRecord projectRecord3 = new ProjectRecord("com.riag");
			projectRecordList.add(projectRecord1);
			projectRecordList.add(projectRecord2);
			projectRecordList.add(projectRecord3);
			ImportProjects instance = new ImportProjects();
			instance.setIncludes(new Pattern[] { Pattern.compile(instance.asRegEx("ch.ringler.*")) });
			instance.setExcludes(new Pattern[] { Pattern.compile(instance.asRegEx("*.x.z")) });
			instance.filterProjects(projectRecordList);
			System.err.println("LEFT " + projectRecordList.size());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void outReport(List<ProjectRecord> projectRecordList) {
		System.out.println("Imported projects from '" + sourceDirectory + "':");
		String[] names = new String[projectRecordList.size()];
		int index = 0;
		int maxLength = 0;
		for(ProjectRecord record: projectRecordList) {			
			names[index] = record.getProjectName();
			int currentLength = names[index].length(); 
			if(currentLength > maxLength)
				maxLength = currentLength;
			index++;
		}
		maxLength += 4;
		char[] spaces = new char[maxLength];
		Arrays.fill(spaces, ' ' );
		index = 0;
		for(ProjectRecord record: projectRecordList) {
			System.out.print(record.projectName);
			System.out.print(new String(spaces, 0, maxLength - record.projectName.length()));
			switch(record.state) {
				case IGNORED:
					System.out.println("IGNORED");
					break;
				case IMPORTED:
					System.out.println("IMPORTED");
					break;
				case OVERWRITTEN:
					System.out.println("OVERWRITTEN");
					break;
				default:
					System.out.println("UNKNOWN");										
			}
		}
	}
	
	private List<ProjectRecord> enumerateProjectsRecordsFrom(File directory, IProgressMonitor monitor) {
		List<ProjectRecord> result = new ArrayList<ProjectRecord>();
		Collection<File> files = new ArrayList<File>();
		if (collectProjectFilesFromDirectory(files, directory, null, monitor)) {
			Iterator<File> filesIterator = files.iterator();
			while (filesIterator.hasNext()) {
				File file = (File) filesIterator.next();
				result.add(new ProjectRecord(file));			
			}
			return result;
		} else
			return Collections.EMPTY_LIST;
	}
	
	private void createProjects(List<ProjectRecord> projectRecordList, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		monitor.beginTask("Creating projects...", projectRecordList.size());
		for(ProjectRecord record: projectRecordList) {
			createProject(record, new SubProgressMonitor(monitor, 1));		
			monitor.worked(1); // work = 0;
		}
		monitor.done();
	}
	
	/**
	 * Create the project described in record. If it is successful return true.
	 * 
	 * @param record
	 * @return boolean <code>true</code> if successful
	 * @throws InterruptedException
	 */
	private boolean createProject(final ProjectRecord record, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		String projectName = record.getProjectName();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject(projectName);
		if (record.description == null) {
			// Error case.
			record.description = workspace.newProjectDescription(projectName);
			IPath locationPath = new Path(record.projectSystemFile.getAbsolutePath());
			// If it is under the root use the default location.
			if (Platform.getLocation().isPrefixOf(locationPath)) {
				record.description.setLocation(null);
			} else {
				record.description.setLocation(locationPath);
			}
		} else {
			record.description.setName(projectName);
		}
		// import from file system
//		File importSource = null;
//		if (copy) {
//			// import project from location copying files - use default project
//			// location for this workspace
//			URI locationURI = record.description.getLocationURI();
//			// if location is null, project already exists in this location or
//			// some error condition occured.
//			if (locationURI != null) {
//				importSource = new File(locationURI);
//				IProjectDescription newDescription = workspace.newProjectDescription(projectName);
//				newDescription.setBuildSpec(record.description.getBuildSpec());
//				newDescription.setComment(record.description.getComment());
//				newDescription.setDynamicReferences(record.description.getDynamicReferences());
//				newDescription.setNatureIds(record.description.getNatureIds());
//				newDescription.setReferencedProjects(record.description.getReferencedProjects());
//				record.description = newDescription;
//			}
//		}
		try {
			//monitor.subTask("Creating project...", 100);
			if(project.exists()) {
				if(overwrite) {
					project.delete(true, new SubProgressMonitor(monitor, 30));
					project.create(record.description, new SubProgressMonitor(monitor, 30));
					record.setState(ImportState.OVERWRITTEN);					
				} else {
					record.setState(ImportState.IGNORED);
				}
			} else {
				project.create(record.description, new SubProgressMonitor(monitor, 30));
				record.setState(ImportState.IMPORTED);
			}
			project.open(IResource.NONE, new SubProgressMonitor(monitor, 70));
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			//monitor.done();
		}
		// import operation to import project files if copy checkbox is selected
//		if (copy && importSource != null) {
//			List filesToImport = FileSystemStructureProvider.INSTANCE.getChildren(importSource);
//			ImportOperation operation = new ImportOperation(project.getFullPath(), importSource, FileSystemStructureProvider.INSTANCE, this, filesToImport);
//			operation.setContext(getShell());
//			operation.setOverwriteResources(true); // need to overwrite
//			// .project, .classpath
//			// files
//			operation.setCreateContainerStructure(false);
//			operation.run(monitor);
//		}
		return true;
	}

	
	private boolean collectProjectFilesFromDirectory(Collection<File> files, File directory, Set<String> directoriesVisited, IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			return false;
		}
		monitor.subTask("Collecting .project files from " + directory.getPath());
		File[] contents = directory.listFiles();
		if (contents == null)
			return false;
		// Initialize recursion guard for recursive symbolic links
		if (directoriesVisited == null) {
			directoriesVisited = new HashSet<String>();
			try {
				directoriesVisited.add(directory.getCanonicalPath());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		// first look for project description files
		final String dotProject = IProjectDescription.DESCRIPTION_FILE_NAME;
		for (int i = 0; i < contents.length; i++) {
			File file = contents[i];
			if (file.isFile() && file.getName().equals(dotProject)) {
				files.add(file);
				// don't search sub-directories since we can't have nested
				// projects
				return true;
			}
		}
		// no project description found, so recurse into sub-directories
		for (int i = 0; i < contents.length; i++) {
			if (contents[i].isDirectory()) {
				if (!contents[i].getName().equals(METADATA_FOLDER)) {
					try {
						String canonicalPath = contents[i].getCanonicalPath();
						if (!directoriesVisited.add(canonicalPath)) {
							// already been here --> do not recurse
							continue;
						}
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
					collectProjectFilesFromDirectory(files, contents[i], directoriesVisited, monitor);
				}
			}
		}
		return true;
	}
	
	private enum ImportState {
		NEW, IMPORTED, OVERWRITTEN, IGNORED;
	}
	
	private static class ProjectRecord {
		
		private File projectSystemFile;

		private String projectName;

		private ImportState state = ImportState.NEW;
		
		private IProjectDescription description;
		
		ProjectRecord(String projectName) {
			this.projectName = projectName;
		}

		/**
		 * Create a record for a project based on the info in the file.
		 * 
		 * @param file
		 */
		ProjectRecord(File file) {
			projectSystemFile = file;
			setProjectName();
		}

		/**
		 * Set the name of the project based on the projectFile.
		 */
		private void setProjectName() {
			try {
				// If we don't have the project name try again
				if (projectName == null) {
					IPath path = new Path(projectSystemFile.getPath());
					// if the file is in the default location, use the directory
					// name as the project name
					if (isDefaultLocation(path)) {
						projectName = path.segment(path.segmentCount() - 2);
						description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
					} else {
						description = ResourcesPlugin.getWorkspace().loadProjectDescription(path);
						projectName = description.getName();
					}
				}
			} catch (CoreException e) {
				// no good couldn't get the name
			}
		}

		/**
		 * Returns whether the given project description file path is in the
		 * default location for a project
		 * 
		 * @param path
		 * 		The path to examine
		 * @return Whether the given path is the default location for a project
		 */
		private boolean isDefaultLocation(IPath path) {
			// The project description file must at least be within the project,
			// which is within the workspace location
			if (path.segmentCount() < 2)
				return false;
			return path.removeLastSegments(2).toFile().equals(Platform.getLocation().toFile());
		}

		/**
		 * Get the name of the project
		 * 
		 * @return String
		 */
		public String getProjectName() {
			return projectName;
		}

		public ImportState getState() {
			return state;
		}

		public void setState(ImportState state) {
			this.state = state;
		}
		
	}

}
