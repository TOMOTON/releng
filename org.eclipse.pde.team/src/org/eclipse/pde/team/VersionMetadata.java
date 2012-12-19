package org.eclipse.pde.team;


public interface VersionMetadata {

	/**
	 * Checks if this unique line of development is baselined. Also known as 
	 * 'tag'.
	 * @return A <code>boolean</code>flag.
	 */
	public boolean isBaseline();
	
	/**
	 * Gets the baseline.
	 * @return A <code>String</code>.
	 */
	public String getBaseline();
	
	/**
	 * Checks if this unique line of development is main. Also known as 
	 * 'trunk' or 'master'.
	 * @return A <code>boolean</code>flag.
	 */
	public boolean isMainline();
	
	/**
	 * Gets the mainline.
	 * @return A <code>String</code>.
	 */
	public String getMainline();
	
	//public Version getMainLine(); ??? If this is a branch, from where?
	
	/**
	 * Checks if this working copy is most recent. Also known as 'tip' or
	 * 'head'.
	 * @return A <code>boolean</code>flag.
	 */
	public boolean isMostRecent();
	
	/**
	 * Returns the version as a raw version.
	 * @return A <code>String</code>.
	 */
	public String getRawVersion();
		
}
