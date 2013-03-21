package org.eclipse.pde.team;

/**
 * Version meta-data in the spirit of http://en.wikipedia.org/wiki/Revision_control.
 * @author Dann Martens
 */
public class SimpleVersionMetadata implements IVersionMetadata {

	private String baseline;
	
	private String line;

	private boolean mainline;
	
	private boolean mostRecent;
	
	private String revisionReference;
	
	private String qualifier;
	
	@Override
	public boolean isBaseline() {
		return baseline != null;
	}

	@Override
	public String getLine() {
		return line;
	}
	
	@Override
	public boolean isMainline() {
		return mainline;
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
	public String getRevisionReference() {
		return revisionReference;
	}

	@Override
	public String getQualifier() {
		return qualifier;
	}
	
	public SimpleVersionMetadata(String revisionReference, boolean mostRecent, String line, boolean mainline, String baseline, String qualifier) {
		this.revisionReference = revisionReference;
		this.mostRecent = mostRecent;
		this.line = line;
		this.mainline = mainline;
		this.baseline = baseline;
		this.qualifier = qualifier;
	}

	@Override
	public String toString() {
		return "SimpleVersionMetadata{revisionReference=" + revisionReference
				+ ", mostRecent=" + mostRecent
				+ ", baseline=" + baseline
				+ ", line="	+ line
				+ ", isMainline="	+ mainline
			    + ", qualifier=" + qualifier +
			    "}";
	}

}
