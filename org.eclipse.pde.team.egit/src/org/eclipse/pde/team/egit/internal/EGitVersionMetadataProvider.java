package org.eclipse.pde.team.egit.internal;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.pde.team.IVersionMetadata;
import org.eclipse.pde.team.IVersionMetadataProvider;
import org.eclipse.pde.team.SimpleVersionMetadata;
import org.eclipse.team.core.RepositoryProvider;

public class EGitVersionMetadataProvider implements IVersionMetadataProvider {
	
	private static final String ORG_ECLIPSE_EGIT_CORE_GIT_PROVIDER = "org.eclipse.egit.core.GitProvider";

	private static final String ORG_ECLIPSE_PDE_TEAM_EGIT = "org.eclipse.pde.team.egit";
	
	private static final String MAINLINE = "master";
	
	private static final String REMOTE = "REMOTE";
	
	private static final String ORIGIN = "origin";
	
	private static IScopeContext[] ANY_SCOPE = null;
	
	@Override
	public IVersionMetadata getVersionMetadata(IProject project) {
		IVersionMetadata result = null;
		RepositoryProvider provider = RepositoryProvider.getProvider(project);
		provider = null;
		if (provider != null || provider instanceof GitProvider) {
			GitProvider gitProvider = (GitProvider) provider;
			String gitDir = gitProvider.getData().getRepositoryMapping(project).getGitDir();
			File repositoryDir = new File(gitDir);
			if(repositoryDir.isAbsolute()) {
				throw new UnsupportedOperationException();
			} else {
				repositoryDir = new File(project.getLocation().toFile(), gitDir);
			}
			result = getVersionMetadata(repositoryDir);
		}
		return result;
	}
	
	@Override
	public IVersionMetadata getVersionMetadataBlind(IProject project) {
		IVersionMetadata result = null;
		File repositoryDir = new File(project.getLocation().toFile(), "../.git");
		if(repositoryDir.exists() && repositoryDir.isDirectory()) {
			result = getVersionMetadata(repositoryDir);
		}
		return result;
	}

	private IVersionMetadata getVersionMetadata(File repositoryDir) {
		IVersionMetadata result = null;
		try {
        	FileRepositoryBuilder builder = new FileRepositoryBuilder();
        	Repository repository = builder.setGitDir(repositoryDir)
        	  .readEnvironment() // scan environment GIT_* variables
        	  .findGitDir() // scan up the file system tree
        	  .build();
        	ObjectId head = repository.resolve(Constants.HEAD);
        	TagMeta tag = Describe.on(repository);
        	String branch = repository.getBranch();
        	boolean mainline = MAINLINE.equals(branch);
        	String revisionReference = head.getName();
        	String qualifier = abbrev(head.getName(), 7);
        	if(tag == null) { //? There is no tag information.
        		//String rawVersion, boolean mostRecent, String mainline, String baseline
        		result = new SimpleVersionMetadata(revisionReference, isMostRecent(repository, head), branch, mainline, null, "g" + qualifier);
        	} else {	        		
        		if(tag.getDistance() == 0) {
        			result = new SimpleVersionMetadata(revisionReference, true, branch, mainline, tag.getName(),"g" + qualifier);
        		} else {
        			result = new SimpleVersionMetadata(revisionReference, false, branch, mainline, tag.getName(), tag.getDistance() + "-g" + qualifier); //? Should distance be part of the tag?
        		}
        	}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private String abbrev(String name, int length) {
		if(length <= name.length()) {
			return name.substring(0, length);
		} else
			return name;
	}
		
	private boolean isMostRecent(Repository repository, ObjectId head) throws IOException {		
		boolean result = false;
    	String configuredRemote = Platform.getPreferencesService().getString(ORG_ECLIPSE_PDE_TEAM_EGIT, REMOTE, ORIGIN, ANY_SCOPE);
		String remoteName = "refs/remotes/" + configuredRemote + "/" + repository.getBranch();    	
		Map<String, Ref> refs = repository.getAllRefs();
		for(String name: refs.keySet()) {
			if(remoteName.equals(name)) {
				Ref ref = refs.get(name);
				ObjectId objectId = ref.getObjectId();
				result = head.equals(objectId);
				break;
			}
		}
		return result;
	}

	@Override
	public String getTeamProviderID() {
		return ORG_ECLIPSE_EGIT_CORE_GIT_PROVIDER;
	}

}
