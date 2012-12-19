package org.eclipse.pde.team.egit.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.pde.team.SimpleVersionMetadata;
import org.eclipse.pde.team.VersionMetadata;
import org.eclipse.pde.team.VersionMetadataProvider;
import org.eclipse.team.core.RepositoryProvider;

public class EGitVersionMetadataProvider implements VersionMetadataProvider {
	
	private static final String ORG_ECLIPSE_PDE_TEAM = "org.eclipse.pde.team";

	private static final String ORG_ECLIPSE_PDE_TEAM_EGIT = "org.eclipse.pde.team.egit";
	
	private static final String MAINLINE = "MAINLINE";
	
	private static final String REMOTE = "REMOTE";
	
	private static final String MASTER = "master";
	
	private static final String ORIGIN = "origin";
	
	private static IScopeContext[] ANY_SCOPE = null;
	
	private class TagMeta {
		
		private String name;
		
		private int distance;

		public TagMeta(String name, int distance) {
			this.name = name;
			this.distance = distance;
		}

		public String getName() {
			return name;
		}

		public int getDistance() {
			return distance;
		}
		
	}
	
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
				repositoryDir = new File("D:\\GitHub\\lib-gwt-svg\\.git");
			}
			try {
	        	FileRepositoryBuilder builder = new FileRepositoryBuilder();
	        	Repository repository = builder.setGitDir(repositoryDir)
	        	  .readEnvironment() // scan environment GIT_* variables
	        	  .findGitDir() // scan up the file system tree
	        	  .build();
	        	String configuredMainline = Platform.getPreferencesService().getString(ORG_ECLIPSE_PDE_TEAM, MAINLINE, MASTER, ANY_SCOPE);
	        	String configuredRemote = Platform.getPreferencesService().getString(ORG_ECLIPSE_PDE_TEAM_EGIT, REMOTE, ORIGIN, ANY_SCOPE);
	        	ObjectId head = repository.resolve(Constants.HEAD);
	        	TagMeta tag = describe(repository, head);
	        	if(tag == null) { //? We're not on a tag.
	        		String remoteName = "refs/remotes/" + configuredRemote + "/" + configuredMainline;
	        		result = new SimpleVersionMetadata(repository.getBranch() + "-g" + head.getName(), isMostRecent(repository, head, remoteName), repository.getBranch(), null);
	        	} else {	        		
	        		result = new SimpleVersionMetadata(tag.getName() + '-' + tag.getDistance() + "-g" + head.getName(), tag.getDistance() == 0, null, tag.getName());
	        	}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	private boolean isMostRecent(Repository repository, ObjectId head, String remoteName) {
		boolean result = false;
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
	
	private TagMeta describe(Repository repositiory, ObjectId oid) throws NoHeadException, JGitInternalException {
		//int abbreviationLength = 0;
		RevWalk w = new RevWalk(repositiory);
		Map<RevCommit, List<String>> tagLookup = new HashMap<RevCommit, List<String>>();
		try {
			RevFlag f = w.newFlag("wanted");
			for (Ref tag : repositiory.getTags().values()) {
				// Tags can point to non-commits - skip those
				if (w.parseCommit(tag.getObjectId()).getType() != Constants.OBJ_COMMIT)
					continue;
				RevCommit rc = w.parseCommit(tag.getObjectId());
				rc.add(f);
				String fullTagName = tag.getName();
				String[] tagParts = fullTagName.split("/");
				String tagName = tagParts[Array.getLength(tagParts) - 1];
				if (tagLookup.containsKey(rc)) {
					tagLookup.get(rc).add(tagName);
				} else {
					List<String> l = new ArrayList<String>();
					l.add(tagName);
					tagLookup.put(rc, l);
				}
			}
			RevCommit start = w.parseCommit(oid);
			RevCommit candidate = null;
			int candidateDistance = 0;
			w.markStart(start);
			w.setRevFilter(RevFilter.ALL);
			w.sort(RevSort.TOPO);
			RevCommit r = null;
			while ((r = w.next()) != null) {
				if (r.has(f)) {
					candidate = r;
					w.markUninteresting(w.parseCommit(r));
				}
				++candidateDistance;
			}
			if (candidate == null) {
				// not found
				return null;
			}
			// Determine tag name - if there happens to be more than one tag at
			// the same commit, use the one with the most recent date. This is
			// what cgit does.
			int age = 0;
			String tagName = null;
			for (Map.Entry<String, Ref> e : repositiory.getTags().entrySet()) {
				ObjectId thisOid = w.parseCommit(e.getValue().getObjectId());
				ObjectId candidateOid = candidate.getId();
				if (thisOid.equals(candidateOid)) {
					if (w.parseCommit(thisOid).getCommitTime() > age) {
						age = w.parseCommit(thisOid).getCommitTime();
						tagName = e.getKey();
					}
				}
			}
			return new TagMeta(tagName, candidateDistance - 1);
//			if (candidateDistance == 1 || abbreviationLength == 0) {
//				return tagName;
//			}
//			return tagName
//					+ "-"
//					+ Integer.toString(candidateDistance - 1)
//					+ "-"
//					+ "g"
//					+ repositiory.getObjectDatabase().newReader().abbreviate(oid, abbreviationLength).name();
		} catch (Exception e) {
			throw new JGitInternalException("Describe from "+ oid.toString() +" failed!", e);
		} finally {
			w.release();
		}
	}

	@Override
	public String getTeamProviderID() {
		return "org.eclipse.egit.core.GitProvider";
	}

}
