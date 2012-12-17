//package org.eclipse.pde.team.internal;
//
//import java.util.concurrent.ConcurrentHashMap;
//
//import org.eclipse.core.runtime.IConfigurationElement;
//import org.eclipse.core.runtime.Platform;
//import org.eclipse.pde.team.VersionMetadataProvider;
//import org.osgi.framework.BundleActivator;
//import org.osgi.framework.BundleContext;
//
//public class VersionActivator implements BundleActivator {
//
//	private static final String EXTENSION_POINT_ID = "org.eclipse.pde.team.versionMetadataProvider";
//	
//	private ConcurrentHashMap<String, VersionMetadataProvider> providerMap = new ConcurrentHashMap<String, VersionMetadataProvider>();
//	
//	@Override
//	public void start(BundleContext context) throws Exception {
//		try {
//			IConfigurationElement[] elements = Platform.getExtensionRegistry(). getConfigurationElementsFor(EXTENSION_POINT_ID);
//			for(IConfigurationElement element: elements) {
//				System.err.println("ELEMENT: " + element.getName());
//				if("provider".equals(element.getName())) {
//					String className = element.getAttribute("class");
//					System.err.println("CLASS: " + className);
//					VersionMetadataProvider provider;
//					try {
//						provider = (VersionMetadataProvider) element.createExecutableExtension("class");							
//					} catch (Exception e) {
//						throw new IllegalArgumentException("Extension does not implement VersionMetadataProvider!", e);
//					}
//					if(providerMap.putIfAbsent(provider.getTeamProviderID(), provider) != null) {
//						throw new IllegalStateException("Extension for team provider '" + provider.getTeamProviderID() + " alread registered!");
//					}		
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	@Override
//	public void stop(BundleContext context) throws Exception {
//
//	}
//
//}
