package org.eclipse.pde.team.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.pde.team.IVersionMetadata;
import org.eclipse.pde.team.IVersionMetadataProvider;
import org.eclipse.pde.team.IVersionUpdater;
import org.eclipse.team.core.RepositoryProvider;
import org.osgi.framework.Version;

public class VersionBuilder extends IncrementalProjectBuilder {
	
	private final IContentTypeManager contentTypeManager = Platform.getContentTypeManager();

	private class UpdateVersionDeltaVisitor implements IResourceDeltaVisitor {

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
					//? Nothing to do.
					break;
				case IResourceDelta.CHANGED:
					updateVersion(resource, monitor);
					break;
			}
			//? Continue visiting children.
			return true;
		}
	}

//	class XMLErrorHandler extends DefaultHandler {
//		
//		private IFile file;
//
//		public XMLErrorHandler(IFile file) {
//			this.file = file;
//		}
//
//		private void addMarker(SAXParseException e, int severity) {
//			VersionBuilder.this.addMarker(file, e.getMessage(), e.getLineNumber(), severity);
//		}
//
//		public void error(SAXParseException exception) throws SAXException {
//			addMarker(exception, IMarker.SEVERITY_ERROR);
//		}
//
//		public void fatalError(SAXParseException exception) throws SAXException {
//			addMarker(exception, IMarker.SEVERITY_ERROR);
//		}
//
//		public void warning(SAXParseException exception) throws SAXException {
//			addMarker(exception, IMarker.SEVERITY_WARNING);
//		}
//	}

	public static final String BUILDER_ID = "org.eclipse.pde.team.versionBuilder";

	//private static final String MARKER_TYPE = "org.eclipse.pde.team.xmlProblem";
	
	private static final String PROVIDER_EXTENSION_POINT_ID = "org.eclipse.pde.team.versionMetadataProvider";

	private static final String UPDATER_EXTENSION_POINT_ID = "org.eclipse.pde.team.resourceVersionUpdater";
	
	private ConcurrentHashMap<String, IVersionMetadataProvider> providerMap = new ConcurrentHashMap<String, IVersionMetadataProvider>();
	
	private ConcurrentHashMap<String, IVersionUpdater> updaterMap = new ConcurrentHashMap<String, IVersionUpdater>();
	
	public VersionBuilder() {
		super();
		try {
			IConfigurationElement[] elements = Platform.getExtensionRegistry(). getConfigurationElementsFor(PROVIDER_EXTENSION_POINT_ID);
			for(IConfigurationElement element: elements) {
				if("provider".equals(element.getName())) {
					IVersionMetadataProvider provider;
					try {
						provider = (IVersionMetadataProvider) element.createExecutableExtension("class");							
					} catch (Exception e) {
						throw new IllegalArgumentException("Extension does not implement IVersionMetadataProvider!", e);
					}
					if(providerMap.putIfAbsent(provider.getTeamProviderID(), provider) != null) {
						throw new IllegalStateException("Extension for team provider '" + provider.getTeamProviderID() + " alread registered!");
					}		
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			IConfigurationElement[] elements = Platform.getExtensionRegistry(). getConfigurationElementsFor(UPDATER_EXTENSION_POINT_ID);
			for(IConfigurationElement element: elements) {
				if("updater".equals(element.getName())) {
					IVersionUpdater updater;
					try {
						updater = (IVersionUpdater) element.createExecutableExtension("class");							
					} catch (Exception e) {
						throw new IllegalArgumentException("Extension does not implement IVersionUpdater!", e);
					}
					String contentTypeId = element.getAttribute("contentTypeId");
					if(updaterMap.putIfAbsent(contentTypeId, updater) != null) {
						throw new IllegalStateException("Updater for content type id " + contentTypeId + " already registered!");
					}					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

//	private void addMarker(IFile file, String message, int lineNumber, int severity) {
//		try {
//			IMarker marker = file.createMarker(MARKER_TYPE);
//			marker.setAttribute(IMarker.MESSAGE, message);
//			marker.setAttribute(IMarker.SEVERITY, severity);
//			if (lineNumber == -1) {
//				lineNumber = 1;
//			}
//			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
//		} catch (CoreException e) {}
//	}

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

//	private void deleteMarkers(IFile file) {
//		try {
//			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
//		} catch (CoreException ce) {
//		}
//	}

	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
		getProject().accept(new IResourceVisitor() {
			@Override
			public boolean visit(IResource resource) throws CoreException {
				//! System.err.println(" IN " + resource.getName());
				updateVersion(resource, monitor);
				return true;
			}
		});
	}
	
	private void updateVersion(IResource resource, IProgressMonitor monitor) throws CoreException {
		if(resource instanceof IFile) {
			for(IContentType type: contentTypeManager.findContentTypesFor(resource.getName())) {
				IVersionUpdater updater = updaterMap.get(type.getId());
				if(updater != null) {
					IFile file = (IFile) resource;
					Version version = deriveVersion(file);
					boolean blind = false;
					if(version == null) { //? Attempt blind resolution.
						version = deriveVersionBlind(file);
						blind = true;
					}
					if(version != null) {
						System.out.println("Updating " + file.getFullPath().toOSString() + " to version " + version + (blind ? " (blind)" : ""));
						updater.update(file, version, monitor);
					}
				} 
			}
		}
	}

	private Version deriveVersion(IFile file) {
		Version result = null;
		RepositoryProvider repositoryProvider = RepositoryProvider.getProvider(getProject());
		if(repositoryProvider != null) {
			result = deriveVersion(repositoryProvider.getID(), false);
		}
		
		return result;
	}

	private Version deriveVersionBlind(IFile file) {
		Version result = null;
		for(String providerId: providerMap.keySet()) {
			result = deriveVersion(providerId, true);
			if(result != null) {
				break;
			}
		}
		return result;
	}
	
	private Version deriveVersion(String providerId, boolean blind) {
		Version result = null;
		IVersionMetadataProvider metadataProvider = providerMap.get(providerId);
		if(metadataProvider != null) {
			IVersionMetadata versionMetadata = blind ? metadataProvider.getVersionMetadataBlind(getProject()) : metadataProvider.getVersionMetadata(getProject());
			if(versionMetadata != null) {
				if(versionMetadata.isBaseline()) {
					result = parseVersion(versionMetadata.getBaseline());
					if(!versionMetadata.isMostRecent()) { //? Append qualifier.
						String qualifier = "".equals(result.getQualifier()) ? "" : "-";
						qualifier += versionMetadata.getRawVersion();
						result = new Version(result.getMajor(), result.getMinor(), result.getMicro(), qualifier);
					} else { //? Fallback to 'qualifier' keyword: let PDE replace it.
						result = new Version(result.getMajor(), result.getMinor(), result.getMicro(), "qualifier");
					}
				} else {
					result = parseVersion(versionMetadata.getMainline());
					if(Version.emptyVersion.equals(result)) { //? This is not a prefix1.0.0.qualfier mainline.
						String qualifier = versionMetadata.getMainline() + '-' + versionMetadata.getRawVersion();
						result = new Version(result.getMajor(), result.getMinor(), result.getMicro(), qualifier);
					}
				}				
			}
		}
		return result;
	}
	
	private Version parseVersion(String string) {
		Version result = null;
		Pattern pattern = Pattern.compile("^\\D*(\\d*)\\.(\\d*)\\.(\\d*)(?:\\.(.*))?");
		Matcher matcher = pattern.matcher(string);
		if(matcher.matches()) {
			int major = Integer.parseInt(matcher.group(1));
			int minor = Integer.parseInt(matcher.group(2));
			int micro = Integer.parseInt	(matcher.group(3));
			String qualifier = matcher.group(4);
			result = new Version(major, minor, micro, qualifier);
		} else {
			result = Version.emptyVersion;
		}
		return result;
	}
	
	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		delta.accept(new UpdateVersionDeltaVisitor(monitor));
	}
	
}
