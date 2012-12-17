package org.eclipse.pde.team;

public class SimpleVersionMetadata implements VersionMetadata {

	private boolean baseline;
	
	private boolean mainline;
	
	private boolean mostRecent;
	
	private String rawVersion;
	
	@Override
	public boolean isBaseline() {
		return baseline;
	}

	@Override
	public boolean isMainline() {
		return mainline;
	}

	@Override
	public boolean isMostRecent() {
		return mostRecent;
	}

	@Override
	public String getRawVersion() {
		return rawVersion;
	}

	public SimpleVersionMetadata(String rawVersion, boolean mostRecent,	boolean mainline, boolean baseline) {
		this.rawVersion = rawVersion;
		this.mostRecent = mostRecent;
		this.mainline = mainline;
		this.baseline = baseline;
	}

}
