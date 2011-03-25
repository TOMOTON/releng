package org.eclipse.buckminster.pde.product.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.buckminster.cmdline.UsageException;
import org.eclipse.buckminster.core.commands.WorkspaceCommand;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public class ListPlatforms extends WorkspaceCommand {

	@Override
	protected int internalRun(IProgressMonitor monitor) throws Exception {
		Configuration[] configurations = getConfigurations();
		if (configurations != null) {
			for (Configuration configuration : configurations)
				System.out.println(configuration);
		} else {
			System.out.println("Cross-platform export not supported!");
			System.out.println("");
			System.out.println("Install the RCP delta pack first, to enable feature 'org.eclipse.platform.launchers'.");
		}
		return 0;
	}

	@Override
	protected void handleUnparsed(String[] unparsed) throws Exception {
		int len = unparsed.length;
		if (len > 0)
			throw new UsageException("No arguments required");
	}

	private static Configuration[] getConfigurations() {
		if (PDECore.getDefault() == null)
			new PDECore();
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel model = manager.getDeltaPackFeature();
		if (model != null)
			return getListElements(model);
		else
			return null;
	}
	
	private static Configuration[] getListElements(IFeatureModel model) {
		ArrayList<Configuration> list = new ArrayList<Configuration>();
		if (model != null) {
			File bin = new File(model.getInstallLocation(), "bin"); //$NON-NLS-1$
			if (bin.exists() && bin.isDirectory()) {
				File[] children = bin.listFiles();
				for (int i = 0; i < children.length; i++) {
					if (children[i].isDirectory())
						getWS(list, children[i]);
				}
			}
		}
		return (Configuration[]) list.toArray(new Configuration[list.size()]);
	}

	private static void getWS(ArrayList<Configuration> list, File file) {
		File[] children = file.listFiles();
		for (int i = 0; i < children.length; i++) {
			if (children[i].isDirectory())
				getOS(list, children[i], file.getName());
		}
	}

	private static void getOS(ArrayList<Configuration> list, File file, String ws) {
		File[] children = file.listFiles();
		for (int i = 0; i < children.length; i++) {
			if (children[i].isDirectory()
					&& !"CVS".equalsIgnoreCase(children[i].getName())) { //$NON-NLS-1$
				Configuration config = new Configuration();
				config.ws = ws;
				config.os = file.getName();
				config.arch = children[i].getName();
				list.add(config);
			}
		}
	}

	private static Configuration[] getConfigurations(String[] platforms) {
		Configuration configurations[] = getConfigurations();
		HashMap<String, Configuration> configurationMap = new LinkedHashMap<String, Configuration>();
		for (Configuration configuration : configurations) {
			configurationMap.put(configuration.toString(), configuration);
		}
		List<Configuration> resultList = new ArrayList<Configuration>();
		for (String platform : platforms) {
			if (configurationMap.containsKey(platform))
				resultList.add(configurationMap.get(platform));
		}
		return resultList.toArray(new Configuration[resultList.size()]);
	}

	/**
	 * 
	 * @return
	 */
	protected static String[][] getTargets(String[] platforms) {
		Configuration[] configurations = getConfigurations(platforms);
		String[][] targets = new String[configurations.length][4];
		for (int i = 0; i < configurations.length; i++) {
			Configuration config = configurations[i];
			String[] combo = new String[4];
			combo[0] = config.os;
			combo[1] = config.ws;
			combo[2] = config.arch;
			combo[3] = ""; //$NON-NLS-1$
			targets[i] = combo;
		}
		return targets;
	}

	private static class Configuration {

		String os;

		String ws;

		String arch;

		public String toString() {
			return os + ":" + ws + "/" + arch; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		
	}

}
