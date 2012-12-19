package org.eclipse.pde.team.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.team.VersionMetadata;
import org.eclipse.pde.team.VersionMetadataProvider;
import org.eclipse.team.core.RepositoryProvider;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class VersionBuilder extends IncrementalProjectBuilder {

	class UpdateVersionDeltaVisitor implements IResourceDeltaVisitor {

		private IProgressMonitor monitor;
		
		public UpdateVersionDeltaVisitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}

		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
				case IResourceDelta.ADDED:
					updateVersion(resource, monitor);
					break;
				case IResourceDelta.REMOVED:
					// handle removed resource
					break;
				case IResourceDelta.CHANGED:
					updateVersion(resource, monitor);
					break;
			}
			//? Continue visiting children.
			return true;
		}
	}

	class XMLErrorHandler extends DefaultHandler {
		
		private IFile file;

		public XMLErrorHandler(IFile file) {
			this.file = file;
		}

		private void addMarker(SAXParseException e, int severity) {
			VersionBuilder.this.addMarker(file, e.getMessage(), e.getLineNumber(), severity);
		}

		public void error(SAXParseException exception) throws SAXException {
			addMarker(exception, IMarker.SEVERITY_ERROR);
		}

		public void fatalError(SAXParseException exception) throws SAXException {
			addMarker(exception, IMarker.SEVERITY_ERROR);
		}

		public void warning(SAXParseException exception) throws SAXException {
			addMarker(exception, IMarker.SEVERITY_WARNING);
		}
	}

	public static final String BUILDER_ID = "org.eclipse.pde.team.versionBuilder";

	private static final String MARKER_TYPE = "org.eclipse.pde.team.xmlProblem";
	
	private static final String EXTENSION_POINT_ID = "org.eclipse.pde.team.versionMetadataProvider";

	private ConcurrentHashMap<String, VersionMetadataProvider> providerMap = new ConcurrentHashMap<String, VersionMetadataProvider>();
	
	public VersionBuilder() {
		super();
		try {
			IConfigurationElement[] elements = Platform.getExtensionRegistry(). getConfigurationElementsFor(EXTENSION_POINT_ID);
			for(IConfigurationElement element: elements) {
				if("provider".equals(element.getName())) {
					String className = element.getAttribute("class");
					VersionMetadataProvider provider;
					try {
						provider = (VersionMetadataProvider) element.createExecutableExtension("class");							
					} catch (Exception e) {
						throw new IllegalArgumentException("Extension does not implement VersionMetadataProvider!", e);
					}
					if(providerMap.putIfAbsent(provider.getTeamProviderID(), provider) != null) {
						throw new IllegalStateException("Extension for team provider '" + provider.getTeamProviderID() + " alread registered!");
					}		
				}
			}
		} catch (Exception ignore) {
			
		}
	}

	private void addMarker(IFile file, String message, int lineNumber, int severity) {
		try {
			IMarker marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber == -1) {
				lineNumber = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		} catch (CoreException e) {}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {				
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	void updateVersion(IResource resource, IProgressMonitor monitor) throws CoreException {
		if (resource instanceof IFile && resource.getName().equals("MANIFEST.MF")) {
			IFile file = (IFile) resource;
			deleteMarkers(file);			
			RepositoryProvider repositoryProvider = RepositoryProvider.getProvider(getProject());
			VersionMetadataProvider metadataProvider = providerMap.get(repositoryProvider.getID());
			VersionMetadata versionMetadata = metadataProvider.getVersionMetadata(getProject());
			System.out.println("VERSION " + versionMetadata);
			ManifestUpdater updater = new ManifestUpdater(PDEProject.getManifest(getProject()));
			if(versionMetadata.isBaseline()) {
				updater.update(versionMetadata.getBaseline(), monitor);
			}			

//			XMLErrorHandler reporter = new XMLErrorHandler(file);
//			try {
//				getParser().parse(file.getContents(), reporter);
//			} catch (Exception e1) {
//			}
		}
	}

	private void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
		}
	}

	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
		updateVersion(PDEProject.getManifest(getProject()), monitor);
	}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		delta.accept(new UpdateVersionDeltaVisitor(monitor));
	}
	
}
