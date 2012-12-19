package org.eclipse.pde.team;

public class SimpleVersionMetadata implements VersionMetadata {

	private String baseline;
	
	private String mainline;
	
	private boolean mostRecent;
	
	private String rawVersion;
	
	@Override
	public boolean isBaseline() {
		return baseline != null;
	}

	@Override
	public String getMainline() {
		return mainline;
	}
	
	@Override
	public boolean isMainline() {
		return mainline != null;
	}

	@Override
	public String getBaseline() {
		return baseline;
	}


	@Override
	public boolean isMostRecent() {
		return mostRecent;
	}

	@Override
	public String getRawVersion() {
		return rawVersion;
	}

	public SimpleVersionMetadata(String rawVersion, boolean mostRecent,	String mainline, String baseline) {
		this.rawVersion = rawVersion;
		this.mostRecent = mostRecent;
		this.mainline = mainline;
		this.baseline = baseline;
	}

	@Override
	public String toString() {
		return "SimpleVersionMetadata [baseline=" + baseline + ", mainline="
				+ mainline + ", mostRecent=" + mostRecent + ", rawVersion="
				+ rawVersion + "]";
	}

}
