/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation, SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.pde.team.egit.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;

public class Describe {

	/**
	 * Creates a new describe command which interacts with a single repository
	 * 
	 * @param repo
	 *            the {@link org.eclipse.jgit.lib.Repository} this command
	 *            should interact with
	 * @throws GitAPIException 
	 */
	public static TagMeta on(Repository repo) throws GitAPIException {
		return new Describe(repo).call();
	}

	private Repository repo;

	/**
	 * Creates a new describe command which interacts with a single repository
	 * 
	 * @param repo
	 *            the {@link org.eclipse.jgit.lib.Repository} this command
	 *            should interact with
	 */
	private Describe(Repository repo) {
		this.repo = repo;
	}

	public TagMeta call() throws GitAPIException {
		// get tags
		Map<ObjectId, String> tagObjectIdToName = findTagObjectIds(repo, false); // No lightweight tags.
		// get current commit
		RevCommit headCommit = findHeadObjectId(repo);
		if (isATag(headCommit, tagObjectIdToName)) {
			String tagName = tagObjectIdToName.get(headCommit);
			return new TagMeta(tagName, 0);
		}
		if (foundZeroTags(tagObjectIdToName)) {
			return null;
		}
		List<RevCommit> commits = findCommitsUntilSomeTag(repo, headCommit,	tagObjectIdToName);
		int distance = distanceBetween(repo, headCommit, commits.get(0));
		String tagName = tagObjectIdToName.get(commits.get(0));
		//? This is the distance to the nearest tag.
		return new TagMeta(tagName, distance);
	}

	private static boolean foundZeroTags(Map<ObjectId, String> tags) {
		return tags.isEmpty();
	}

	private static boolean isATag(ObjectId headCommit, Map<ObjectId, String> tagObjectIdToName) {
		return tagObjectIdToName.containsKey(headCommit);
	}

	private RevCommit findHeadObjectId(Repository repo)	throws RuntimeException {
		try {
			ObjectId headId = repo.resolve("HEAD");
			RevWalk walk = new RevWalk(repo);
			RevCommit headCommit = walk.lookupCommit(headId);
			walk.dispose();
			return headCommit;
		} catch (IOException ioe) {
			throw new RuntimeException("Unable to obtain HEAD commit!", ioe);
		}
	}

	private List<RevCommit> findCommitsUntilSomeTag(Repository repo, RevCommit head, Map<ObjectId, String> tagObjectIdToName) {
		RevWalk revWalk = new RevWalk(repo);
		try {
			revWalk.markStart(head);
			for (RevCommit commit : revWalk) {
				ObjectId objId = commit.getId();
				String lookup = tagObjectIdToName.get(objId);
				if (lookup != null) {
					return Collections.singletonList(commit);
				}
			}
			throw new RuntimeException("Did not find any commits until some tag.");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Calculates the distance (number of commits) between the given parent and
	 * child commits.
	 * 
	 * @return distance (number of commits) between the given commits
	 */
	private int distanceBetween(Repository repo, RevCommit child, RevCommit parent) {
		RevWalk revWalk = new RevWalk(repo);
		try {
			revWalk.markStart(child);
			Set<ObjectId> seena = new HashSet<ObjectId>();
			Set<ObjectId> seenb = new HashSet<ObjectId>();
			Queue<RevCommit> q = new LinkedList<RevCommit>();
			q.add(revWalk.parseCommit(child));
			int distance = 0;
			ObjectId parentId = parent.getId();
			while (q.size() > 0) {
				RevCommit commit = q.remove();
				ObjectId commitId = commit.getId();
				if (seena.contains(commitId)) {
					continue;
				}
				seena.add(commitId);
				if (parentId.equals(commitId)) {
					// don't consider commits that are included in this commit
					seeAllParents(revWalk, commit, seenb);
					// remove things we shouldn't have included
					for (ObjectId oid : seenb) {
						if (seena.contains(oid)) {
							distance--;
						}
					}
					seena.addAll(seenb);
					continue;
				}
				for (ObjectId oid : commit.getParents()) {
					if (!seena.contains(oid)) {
						q.add(revWalk.parseCommit(oid));
					}
				}
				distance++;
			}
			return distance;
		} catch (Exception e) {
			throw new RuntimeException(String.format("Unable to calculate distance between [%s] and [%s]",
					child, parent), e);
		} finally {
			revWalk.dispose();
		}
	}

	private static void seeAllParents(RevWalk revWalk,
			RevCommit child, Set<ObjectId> seen) throws IOException {
		Queue<RevCommit> q = new LinkedList<RevCommit>();
		q.add(child);
		while (q.size() > 0) {
			RevCommit commit = q.remove();
			for (ObjectId oid : commit.getParents()) {
				if (seen.contains(oid)) {
					continue;
				}
				seen.add(oid);
				q.add(revWalk.parseCommit(oid));
			}
		}
	}

	// git commit id -> its tag
	private Map<ObjectId, String> findTagObjectIds(Repository repo, boolean tagsFlag) {
		Map<ObjectId, String> commitIdsToTagNames = new HashMap<ObjectId, String>();
		RevWalk walk = new RevWalk(repo);
		try {
			walk.markStart(walk.parseCommit(repo.resolve("HEAD")));
			List<Ref> tagRefs = Git.wrap(repo).tagList().call();
			for (Ref tagRef : tagRefs) {
				walk.reset();
				String name = tagRef.getName();
				ObjectId resolvedCommitId = repo.resolve(name);
				// todo that's a bit of a hack...
				try {
					RevTag revTag = walk.parseTag(resolvedCommitId);
					ObjectId taggedCommitId = revTag.getObject().getId();
					commitIdsToTagNames.put(taggedCommitId, trimFullTagName(name));
				} catch (IncorrectObjectTypeException ex) {
					// it's an lightweight tag! (yeah, really)
					if (tagsFlag) {
						// --tags means "include lightweight tags"
						//! System.err.println("Including lightweight tag [%s]"	+ name);
						commitIdsToTagNames.put(resolvedCommitId, trimFullTagName(name));
					}
				} catch (Exception ignored) {}
			}
			return commitIdsToTagNames;
		} catch (Exception e) {
			System.err.println("Unable to locate any tags!");
			e.printStackTrace();
		} finally {
			walk.release();
		}
		return Collections.emptyMap();
	}

	private static String trimFullTagName(String tagName) {
		return tagName.replaceFirst("refs/tags/", "");
	}

}
