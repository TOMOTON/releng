package org.eclipse.buckminster.pde.product.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.buckminster.cmdline.AbstractCommand;
import org.eclipse.buckminster.cmdline.Option;
import org.eclipse.buckminster.cmdline.OptionDescriptor;
import org.eclipse.buckminster.cmdline.OptionValueType;
import org.eclipse.buckminster.cmdline.SimpleErrorExitException;
import org.eclipse.buckminster.cmdline.UsageException;
import org.eclipse.buckminster.core.commands.WorkspaceCommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.ProductExportOperation;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.core.product.WorkspaceProductModel;
import org.osgi.framework.Version;

public class ProductExport extends WorkspaceCommand {

	static private final OptionDescriptor OPTION_SOURCE = new OptionDescriptor('S', "source", OptionValueType.NONE);

	static private final OptionDescriptor OPTION_CYCLES = new OptionDescriptor('C', "cycles", OptionValueType.OPTIONAL);
	
	static private final OptionDescriptor OPTION_DESTINATION = new OptionDescriptor('D', "destination", OptionValueType.REQUIRED);
	
	static private final OptionDescriptor OPTION_SYNCHRONIZE = new OptionDescriptor('Y', "synchronize", OptionValueType.NONE);
	
	static private final OptionDescriptor OPTION_ROOT = new OptionDescriptor('R', "root", OptionValueType.OPTIONAL);

	static private final OptionDescriptor OPTION_PLATFORM =	new OptionDescriptor('P', "platform", OptionValueType.OPTIONAL);
	
	private FeatureExportInfo featureExportInfo;
	private IProductModel productModel;
	
	private boolean exportSource = false;
	private String destination;
	private boolean archive = false;
	private boolean synchronize = false;
	private boolean allowBinaryCycles = false;
	private String root = "eclipse";		
	private String productFileName;
	private String[][] targets;
	
	public String getProductFileName() {
		return productFileName;
	}

	public void setProductFileName(String productFileName) {
		this.productFileName = productFileName;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public boolean isArchive() {
		return archive;
	}

	public void setArchive(boolean archive) {
		this.archive = archive;
	}

	@Override
	protected void getOptionDescriptors(List<OptionDescriptor> appendHere) throws Exception {	
		super.getOptionDescriptors(appendHere);
		appendHere.add(OPTION_SOURCE);
		appendHere.add(OPTION_CYCLES);		
		appendHere.add(OPTION_DESTINATION);
		appendHere.add(OPTION_SYNCHRONIZE);		
		appendHere.add(OPTION_ROOT);
		appendHere.add(OPTION_PLATFORM);
	}

	@Override
	protected void handleOption(Option option) throws Exception	{
		if(option.is(OPTION_SOURCE)) {
			exportSource = true;		
		} else
		if(option.is(OPTION_CYCLES)) {
			allowBinaryCycles = true;		
		} else			
		if(option.is(OPTION_DESTINATION)) {
			setDestination(option.getValue());
			if(destination.endsWith(".zip"))
					setArchive(true);
		} else
		if(option.is(OPTION_SYNCHRONIZE)) {
			synchronize = true;
		} else
		if(option.is(OPTION_ROOT)) {
			root = option.getValue(); 
		} else
		if(option.is(OPTION_PLATFORM)) {
			String[] platforms = option.getValue().split(",");
			targets = ListPlatforms.getTargets(platforms);
		} else
			super.handleOption(option);
	}
	
	@Override
	protected void handleUnparsed(String[] unparsed) throws Exception {
		int len = unparsed.length;
		if(len > 1)
			throw new UsageException("Too many arguments");
		if(len == 1)
			setProductFileName(unparsed[0]);
	}
	
	@Override
	protected int internalRun(IProgressMonitor monitor) throws Exception {
		if(productFileName == null)
			throw new SimpleErrorExitException("Missing product file name.");
		if(destination == null)
			throw new SimpleErrorExitException("Missing product artifact destination, use --destination (-D) to a directory path or .zip file.");			
		monitor.beginTask(null, 3);
		monitor.subTask("Preparing product export");
		//-
		monitor.subTask("Checking product model");
		System.err.println("performPreliminaryChecks");
		performPreliminaryChecks(monitor);
		monitor.worked(1);
		//-
		monitor.subTask("Building export info");
		System.err.println("buildExportInfo");
		buildExportInfo();		
		monitor.worked(1);
		//-
		monitor.subTask("Trimming root");
		if ("".equals(root.trim()))
			root = ".";
		try {
			System.err.println("Exporting");
			monitor.subTask("Performing actual export");
			ProductExportOperation operation = new ProductExportOperation(featureExportInfo, "Exporting product...", productModel.getProduct(), root);
			operation.setProgressGroup(monitor, 1);
			operation.setUser(true);
			operation.setRule(ResourcesPlugin.getWorkspace().getRoot());
			operation.schedule();			
			operation.join();
		}
		finally {
			monitor.done();
		}

		return 0;		
	}
	
	protected void performPreliminaryChecks(IProgressMonitor monitor) throws SimpleErrorExitException {		
		IFile productFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(productFileName));
		System.err.println("Product file is " + productFile);
		if(productFile != null)
			System.err.println(" inside " + productFile.getLocation().toOSString());
		productModel = new WorkspaceProductModel(productFile, false);
		try {
			productModel.load();
			if (!productModel.isLoaded()) {
				throw new SimpleErrorExitException("The product model could not be loaded, possibly corrupt?");
			}
		}
		catch (CoreException e) {
			throw new SimpleErrorExitException("The product model could not be loaded, possibly corrupt?");		
		}
//		if (m_synchronize) {
//			try {
//				monitor.subTask("Synchronizing product");
//				SynchronizationOperation operation = new SynchronizationOperation(m_productModel.getProduct(), null, product_file.getProject());
//				operation.run(new SubProgressMonitor(monitor, 1));
//			} catch (InvocationTargetException e) {
//				throw new SimpleErrorExitException("Product synchronization failed!\n" + e.getTargetException().getMessage());
//			} catch (InterruptedException e) {
//				throw new SimpleErrorExitException("Product synchronization interrupted!");
//			}
//		}		
	}

	private void buildExportInfo() throws SimpleErrorExitException {
		featureExportInfo = new FeatureExportInfo();
		featureExportInfo.toDirectory = !archive;
		featureExportInfo.exportSource = exportSource;
		//featureExportInfo.exportSourceBundles = exportSourceBundles;
		featureExportInfo.allowBinaryCycles = allowBinaryCycles;
		Path path = new Path(destination);
		if(!path.isValidPath(destination))
			throw new SimpleErrorExitException("Invalid destination path!");
		featureExportInfo.destinationDirectory = archive ? PathUtil.extractPath(path) : destination;
		//! System.err.println("m_info.destinationDirectory = '" + m_info.destinationDirectory + "'");
		featureExportInfo.zipFileName = archive ? PathUtil.extractFileName(path) : null;
		featureExportInfo.targets = targets;
		if (productModel.getProduct().useFeatures())
			featureExportInfo.items = getFeatureModels();
		else
			featureExportInfo.items = getPluginModels();
	}
	
		
//	private IFeatureModel[] getFeatureModels() {
//		ArrayList<IFeatureModel> list = new ArrayList<IFeatureModel>();
//		FeatureModelManager manager = PDECore.getDefault()
//				.getFeatureModelManager();
//		IProductFeature[] features = m_productModel.getProduct().getFeatures();
//		for (int i = 0; i < features.length; i++) {
//			IFeatureModel model = manager.findFeatureModel(features[i].getId(),
//					features[i].getVersion());
//			if (model != null)
//				list.add(model);
//		}
//		return (IFeatureModel[]) list.toArray(new IFeatureModel[list.size()]);
//	}
//
//	private BundleDescription[] getPluginModels() {
//		ArrayList<BundleDescription> list = new ArrayList<BundleDescription>();
//		//State state = TargetPlatform..getState();
//		IProductPlugin[] plugins = m_productModel.getProduct().getPlugins();
//		for (int i = 0; i < plugins.length; i++) {
//			BundleDescription bundle = state.getBundle(plugins[i].getId(), null);
//			if (bundle != null)
//				list.add(bundle);
//		}
//		return (BundleDescription[]) list.toArray(new BundleDescription[list.size()]);
//	}
	
	//--- new and improved
	
//	protected void scheduleExportJob() {
//		FeatureExportInfo info = new FeatureExportInfo();
//		info.toDirectory = fPage.doExportToDirectory();
//		info.exportSource = fPage.doExportSource();
//		info.exportSourceBundle = fPage.doExportSourceBundles();
//		info.allowBinaryCycles = fPage.doBinaryCycles();
//		info.exportMetadata = fPage.doExportMetadata();
//		info.destinationDirectory = fPage.getDestination();
//		info.zipFileName = fPage.getFileName();
//		if (fPage2 != null && fPage.doMultiPlatform())
//			info.targets = fPage2.getTargets();
//		if (fProductModel.getProduct().useFeatures())
//			info.items = getFeatureModels();
//		else
//			info.items = getPluginModels();
//
//		String rootDirectory = fPage.getRootDirectory();
//		if ("".equals(rootDirectory.trim())) //$NON-NLS-1$
//			rootDirectory = "."; //$NON-NLS-1$
//		ProductExportOperation job = new ProductExportOperation(info, PDEUIMessages.ProductExportJob_name, m_productModel.getProduct(), rootDirectory);
//		job.setUser(true);
//		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
//		job.schedule();
//		job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_FEATURE_OBJ);
//	}

	private IFeatureModel[] getFeatureModels() {
		ArrayList list = new ArrayList();
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		IProductFeature[] features = productModel.getProduct().getFeatures();
		for (int i = 0; i < features.length; i++) {
			IFeatureModel model = manager.findFeatureModel(features[i].getId(), features[i].getVersion());
			if (model != null)
				list.add(model);
		}
		return (IFeatureModel[]) list.toArray(new IFeatureModel[list.size()]);
	}

	private BundleDescription[] getPluginModels() {
		ArrayList list = new ArrayList();
		State state = TargetPlatformHelper.getState();
		IProductPlugin[] plugins = productModel.getProduct().getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			BundleDescription bundle = null;
			String v = plugins[i].getVersion();
			if (v != null && v.length() > 0) {
				bundle = state.getBundle(plugins[i].getId(), Version.parseVersion(v));
			}
			// if there's no version, just grab a bundle like before
			if (bundle == null)
				bundle = state.getBundle(plugins[i].getId(), null);
			if (bundle != null)
				list.add(bundle);
		}
		return (BundleDescription[]) list.toArray(new BundleDescription[list.size()]);
	}
	
}
