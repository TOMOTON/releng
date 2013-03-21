package org.eclipse.pde.team.egit.internal;

public class TagMeta {
	
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