package org.eclipse.pde.team.egit.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.pde.team.IVersionMetadata;
import org.eclipse.pde.team.IVersionMetadataProvider;
import org.eclipse.pde.team.SimpleVersionMetadata;
import org.eclipse.team.core.RepositoryProvider;

public class EGitVersionMetadataProvider implements IVersionMetadataProvider {
	
	private static final String ORG_ECLIPSE_PDE_TEAM = "org.eclipse.pde.team";

	private static final String ORG_ECLIPSE_PDE_TEAM_EGIT = "org.eclipse.pde.team.egit";
	
	private static final String MAINLINE = "MAINLINE";
	
	private static final String REMOTE = "REMOTE";
	
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
        	String configuredRemote = Platform.getPreferencesService().getString(ORG_ECLIPSE_PDE_TEAM_EGIT, REMOTE, ORIGIN, ANY_SCOPE);
        	ObjectId head = repository.resolve(Constants.HEAD);
        	TagMeta tag = describeTag(repository, head, 7);
        	if(tag == null) { //? We're not on a tag.
        		String remoteName = "refs/remotes/" + configuredRemote + "/" + repository.getBranch();
        		result = new SimpleVersionMetadata("g" + head.getName(), isMostRecent(repository, head, remoteName), repository.getBranch(), null);
        	} else {	        		
        		if(tag.getDistance() == 0) {
        			result = new SimpleVersionMetadata("g" + head.getName(), true, null, tag.getName());
        		} else {
        			result = new SimpleVersionMetadata(tag.getDistance() + "-g" + head.getName(), false, null, tag.getName()); //? Should distance be part of the tag?
        		}
        	}
		} catch (Exception e) {
			e.printStackTrace();
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
	
	private TagMeta describeTag(Repository repository, ObjectId objectId, int shalength) throws NoHeadException, JGitInternalException {
        RevWalk walk = null;
        RevCommit start = null;
        try {
            walk = new RevWalk(repository);
            //start = walk.parseCommit(repository.resolve(objectId.getName())); //HEAD
            start = walk.parseCommit(objectId);
            walk.markStart(start);
        } catch (IOException e) {
            throw new RuntimeException("Could not find target", e);
        }
        final Map<ObjectId, String> tags = new HashMap<ObjectId,String>();
        for (Map.Entry<String, Ref> tag : repository.getTags().entrySet()) {
            try {
                RevTag r = walk.parseTag(tag.getValue().getObjectId());
                ObjectId taggedCommit = r.getObject().getId();
                tags.put(taggedCommit, tag.getKey());
            } catch (IOException ignore) {}
        }
        // No tags found. 
        if (tags.isEmpty()) {
            return null;
        }
        final List<RevCommit> taggedParents = taggedParentCommits(walk, start, tags);
        RevCommit best = null;
        int bestDistance = 0;
        for (RevCommit commit : taggedParents) {
            int distance = distanceBetween(start, commit);
            if (best == null || (distance < bestDistance)) {
                best = commit;
                bestDistance = distance;
            }
        }
        if(best != null) {
            return new TagMeta(tags.get(best.getId()), bestDistance);
        } else {
        	return null;
        }
    }
	
    /**
     * This does something. I think it gets every possible parent tag this
     * commit has, then later we look for which is closest and use that as
     * the tag to describe. Or something like that.
     *
     * @param walk
     * @param child
     * @param tagmap
     * @return
     * @throws RuntimeException
     */
    private List<RevCommit> taggedParentCommits(final RevWalk walk, final RevCommit child, final Map<ObjectId, String> tagmap) throws RuntimeException {
        final Queue<RevCommit> q = new LinkedList<RevCommit>();
        q.add(child);
        final List<RevCommit> taggedcommits = new LinkedList<RevCommit>();
        final Set<ObjectId> seen = new HashSet<ObjectId>();

        while (q.size() > 0) {
            final RevCommit commit = q.remove();
            if (tagmap.containsKey(commit.getId())) {
                taggedcommits.add(commit);
                // don't consider commits that are farther away than this tag
                continue;
            }
            for (RevCommit p : commit.getParents()) {
                if (!seen.contains(p.getId())) {
                    seen.add(p.getId());
                    try {
                        q.add(walk.parseCommit(p.getId()));
                    } catch (IOException e) {
                        throw new RuntimeException("Parent not found", e);
                    }
                }
            }
        }
        return taggedcommits;
    }

    /**
     * Calculate the distance between 2 given commits, parent and child.
     *
     * @param child Commit to calculate distance to (The latest commit)
     * @param parent Commit to calculate distance from (The last tag)
     * @return Numeric value between the 2 commits.
     */
    private int distanceBetween(final RevCommit child, final RevCommit parent) {
        final Set<ObjectId> seen = new HashSet<ObjectId>();
        final Queue<RevCommit> q1 = new LinkedList<RevCommit>();
        final Queue<RevCommit> q2 = new LinkedList<RevCommit>();

        q1.add(child);
        int distance = 1;
        while ((q1.size() > 0) || (q2.size() > 0)) {
            if (q1.size() == 0) {
                distance++;
                q1.addAll(q2);
                q2.clear();
            }
            final RevCommit commit = q1.remove();
            if (commit.getParents() == null) {
                return 0;
            } else {
                for (RevCommit p : commit.getParents()) {
                    if (p.getId().equals(parent.getId())) {
                        return distance;
                    }
                    if (!seen.contains(p.getId())) {
                        q2.add(p);
                    }
                }
            }
            seen.add(commit.getId());
        }
        return distance;
    }
	
	@Override
	public String getTeamProviderID() {
		return "org.eclipse.egit.core.GitProvider";
	}

}
