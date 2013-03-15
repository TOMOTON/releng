package org.eclipse.buckminster.pde.product.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.buckminster.cmdline.Option;
import org.eclipse.buckminster.cmdline.OptionDescriptor;
import org.eclipse.buckminster.cmdline.OptionValueType;
import org.eclipse.buckminster.cmdline.SimpleErrorExitException;
import org.eclipse.buckminster.core.commands.WorkspaceCommand;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.FeatureExportOperation;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public class FeatureExport extends WorkspaceCommand {

	static private final OptionDescriptor OPTION_SOURCE = new OptionDescriptor('S', "source", OptionValueType.REQUIRED);

	static private final OptionDescriptor OPTION_JAR = new OptionDescriptor('J', "jar", OptionValueType.NONE);
	
	static private final OptionDescriptor OPTION_METADATA = new OptionDescriptor('M', "metadata", OptionValueType.NONE);
	
	static private final OptionDescriptor OPTION_CYCLES = new OptionDescriptor('C', "cycles", OptionValueType.NONE);
	
	static private final OptionDescriptor OPTION_DESTINATION = new OptionDescriptor('D', "destination", OptionValueType.REQUIRED);

	static private final OptionDescriptor OPTION_QUALIFIER = new OptionDescriptor('Q', "qualifier", OptionValueType.REQUIRED);

	static private final OptionDescriptor OPTION_BUILD = new OptionDescriptor('B', "build", OptionValueType.NONE);
	
	static private final OptionDescriptor OPTION_CATEGORY =	new OptionDescriptor('G', "category", OptionValueType.REQUIRED);
	
	static private final OptionDescriptor OPTION_PLATFORM =	new OptionDescriptor('P', "platform", OptionValueType.REQUIRED);


	private FeatureExportInfo featureExportInfo;
	
	private ExportType exportSource = null;
	private boolean jarFormat = false;
	private boolean exportMetadata = false;
	private boolean allowBinaryCycles = false;
	private String destination;
	private String qualifier;
	private boolean build = false;
	private boolean archive = false;	
	private String[][] targets;
	private String category;
	private String[] featureIds;

	
	public ExportType getExportSource() {
		return exportSource;
	}

	public void setExportSource(ExportType exportSource) {
		this.exportSource = exportSource;
	}

	public boolean isJarFormat() {
		return jarFormat;
	}

	public void setJarFormat(boolean jarFormat) {
		this.jarFormat = jarFormat;
	}

	public boolean isExportMetadata() {
		return exportMetadata;
	}

	public void setExportMetadata(boolean exportMetadata) {
		this.exportMetadata = exportMetadata;
	}

	public boolean isAllowBinaryCycles() {
		return allowBinaryCycles;
	}

	public void setAllowBinaryCycles(boolean allowBinaryCycles) {
		this.allowBinaryCycles = allowBinaryCycles;
	}

	public String[] getFeatureIds() {
		return featureIds;
	}

	public void setFeatureIds(String[] featureIds) {
		this.featureIds = featureIds;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public String getQualifier() {
		return qualifier;
	}

	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}

	public boolean isBuild() {
		return build;
	}

	public void setBuild(boolean build) {
		this.build = build;
	}

	public boolean isArchive() {
		return archive;
	}

	public void setArchive(boolean archive) {
		this.archive = archive;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String[][] getTargets() {
		return targets;
	}

	public void setTargets(String[][] targets) {
		this.targets = targets;
	}

	@Override
	protected void getOptionDescriptors(List<OptionDescriptor> appendHere) throws Exception {	
		super.getOptionDescriptors(appendHere);
		appendHere.add(OPTION_SOURCE);
		appendHere.add(OPTION_JAR);		
		appendHere.add(OPTION_METADATA);			
		appendHere.add(OPTION_CYCLES);		
		appendHere.add(OPTION_QUALIFIER);
		appendHere.add(OPTION_BUILD);
		appendHere.add(OPTION_DESTINATION);		
		appendHere.add(OPTION_PLATFORM);
		appendHere.add(OPTION_CATEGORY);			
	}

	@Override
	protected void handleOption(Option option) throws Exception	{
		if(option.is(OPTION_SOURCE)) {
			setExportSource(ExportType.valueOf(option.getValue()));			
		} else
		if(option.is(OPTION_JAR)) {
			setJarFormat(true);			
		} else	
		if(option.is(OPTION_METADATA)) {
			setExportMetadata(true);		
		} else			
		if(option.is(OPTION_CATEGORY)) {
			setCategory(option.getValue());		
		} else			
		if(option.is(OPTION_CYCLES)) {
			setAllowBinaryCycles(true);		
		} else
		if(option.is(OPTION_QUALIFIER)) {
			setQualifier(option.getValue());		
		} else
		if(option.is(OPTION_BUILD)) {
			setBuild(true);		
		} else					
		if(option.is(OPTION_DESTINATION)) {
			setDestination(option.getValue());
			if(destination.endsWith(".zip")) {
				setArchive(true);
			}
		} else
		if(option.is(OPTION_PLATFORM)) {
			String[] platforms = option.getValue().split(",");
			targets = ListPlatforms.getTargets(platforms);
		} else
			super.handleOption(option);
	}
	
	@Override
	protected void handleUnparsed(String[] unparsed) throws Exception {
		setFeatureIds(unparsed);
	}
	
	@Override
	protected int internalRun(IProgressMonitor monitor) throws Exception {
		if(featureIds.length == 0)
			throw new SimpleErrorExitException("Missing feature identifier(s).");
		if(destination == null)
			throw new SimpleErrorExitException("Missing feature artifact destination, use --destination (-D) to a directory path or .zip file.");			
		monitor.beginTask(null, 3);
		monitor.subTask("Preparing product export");
		//-
		monitor.subTask("Checking product model");
		performPreliminaryChecks(monitor);
		monitor.worked(1);
		//-
		monitor.subTask("Building export info");
		buildExportInfo();		
		monitor.worked(1);
		//-
		try {
			monitor.subTask("Exporting...");		
			System.out.println("Exporting...");
			FeatureExportOperation operation = new FeatureExportOperation(featureExportInfo, "Exporting feature...");
			operation.setProgressGroup(monitor, 1);
			operation.setUser(true);
			operation.setRule(ResourcesPlugin.getWorkspace().getRoot());
			operation.schedule();			
			operation.join();			
			IStatus result = operation.getResult();
			if(!result.isOK()) {
				System.err.println("Operation resulted in " + result + '!');
				return 1;
			}			
		}
		finally {
			monitor.done();
		}
		return 0;		
	}
	
	protected void performPreliminaryChecks(IProgressMonitor monitor) throws SimpleErrorExitException {
		Set<String> featureIdSet = new HashSet<String>(Arrays.asList(getFeatureIds()));		
		IFeatureModel[] featureModels = selectFeatureModels(getFeatureIds());
		for(IFeatureModel featureModel: featureModels) {
			IResource resource = featureModel.getUnderlyingResource();
			String featureId = featureModel.getFeature().getId();
			System.out.println("Feature " + featureId + " came from " + resource.getFullPath().toPortableString());
			featureIdSet.remove(featureId);
		}		
		if(!featureIdSet.isEmpty()) {
			for(String featureId: featureIdSet) {
				System.err.println("Could not find feature " + featureId);
			}
			throw new SimpleErrorExitException("One ore more features could not be found, wrong id(s)?");
		}
	}

	private void buildExportInfo() throws SimpleErrorExitException {
		featureExportInfo = new FeatureExportInfo();
		featureExportInfo.toDirectory = !isArchive();
		featureExportInfo.exportSource = getExportSource() != null;
		featureExportInfo.exportSourceBundle = ExportType.BUNDLE == getExportSource();
		featureExportInfo.allowBinaryCycles = isAllowBinaryCycles();
		featureExportInfo.useJarFormat = isJarFormat();
		featureExportInfo.exportMetadata = isJarFormat() ? isExportMetadata() : false;
		featureExportInfo.categoryDefinition = getCategory();
		featureExportInfo.qualifier = getQualifier();
		featureExportInfo.useWorkspaceCompiledClasses = !isBuild();
		Path path = new Path(getDestination());
		if(!path.isValidPath(getDestination()))
			throw new SimpleErrorExitException("Invalid destination path!");
		featureExportInfo.destinationDirectory = isArchive() ? PathUtil.extractPath(path) : getDestination();
		//! System.err.println("m_info.destinationDirectory = '" + m_info.destinationDirectory + "'");
		featureExportInfo.zipFileName = isArchive() ? PathUtil.extractFileName(path) : null;
		featureExportInfo.targets = getTargets();
		featureExportInfo.items = selectFeatureModels(getFeatureIds());
	}
	
	private IFeatureModel[] selectFeatureModels(String[] featureIds) {
		ArrayList<IFeatureModel> list = new ArrayList<IFeatureModel>();
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		for (int i = 0; i < featureIds.length; i++) {
			IFeatureModel model = manager.findFeatureModel(featureIds[i]);
		if (model != null)
			list.add(model);
		}			
		return (IFeatureModel[]) list.toArray(new IFeatureModel[list.size()]);
	}
	
}
