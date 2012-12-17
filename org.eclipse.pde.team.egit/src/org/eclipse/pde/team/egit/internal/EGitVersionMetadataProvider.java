package org.eclipse.pde.team.egit.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.pde.team.VersionMetadata;
import org.eclipse.pde.team.VersionMetadataProvider;
import org.eclipse.team.core.RepositoryProvider;

public class EGitVersionMetadataProvider implements VersionMetadataProvider {
	
	@Override
	public VersionMetadata getVersionMetadata(IProject project) {
		VersionMetadata result = null;
		RepositoryProvider provider = RepositoryProvider.getProvider(project);
		if (provider != null || provider instanceof GitProvider) {
			GitProvider gitProvider = (GitProvider) provider;
			String gitDir = gitProvider.getData().getRepositoryMapping(project).getGitDir();
			File repositoryDir = new File(gitDir);
			if(repositoryDir.isAbsolute()) {
				throw new UnsupportedOperationException();
			} else {
				repositoryDir = new File(project.getLocation().toFile(), gitDir);
				//repositoryDir = new File("C:\\Users\\dannmartens\\git\\snapform\\.git");
			}
			System.err.println("GIT DIR " + repositoryDir.getAbsolutePath());
	        try {	        	
	        	FileRepositoryBuilder builder = new FileRepositoryBuilder();
	        	Repository repository = builder.setGitDir(repositoryDir)
	        	  .readEnvironment() // scan environment GIT_* variables
	        	  .findGitDir() // scan up the file system tree
	        	  .build();
				//Repository repository = new FileRepository(gitDir);

	        	System.err.println(repository.getAllRefs().keySet().size());
				System.err.println("BRANCH: " + repository.getBranch());
				System.err.println("FULL BRANCH: " + repository.getFullBranch());
				System.err.println("ORIG HEAD: " + repository.readOrigHead());
				System.err.println("WHATAMI: " + whatami(repository));	
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		return result;
	}
	
	private static Map<ObjectId, String> collectTags(Repository repository) {
		Map<ObjectId, String> map = new HashMap<ObjectId, String>();
		Map<String, Ref> refs = repository.getTags();
		for (Map.Entry<String, Ref> tag : refs.entrySet()) {
			ObjectId tagcommit = tag.getValue().getObjectId();
			map.put(tagcommit, tag.getKey());
		}
		return map;
	}
	
	private String whatami(Repository repository) {
		String result = null;
		try {
			Map<String, Ref> allRefs = repository.getAllRefs();
			for(String name: allRefs.keySet()) {
				System.err.println("REF " + name + " -> " + allRefs.get(name));
			}
			ObjectId head = repository.resolve(Constants.HEAD);
			if(head != null || !ObjectId.zeroId().equals(head)) {
				Map<String, Ref> tagMap = repository.getTags();
				System.err.println("OK " + tagMap.size());
				for(String name: tagMap.keySet()) {
					System.err.println("NAME " + name + " : " + tagMap.get(name));
				}
			}
			result = head.getName();
			
		} catch (AmbiguousObjectException aoe) {
			aoe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return result;
	}
	

	@Override
	public String getTeamProviderID() {
		return "org.eclipse.egit.core.GitProvider";
	}

}
