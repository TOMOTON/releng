package org.eclipse.pde.team;


public interface IVersionMetadata {

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
	 * Gets the line, if other than 'main'.
	 * @return A <code>String</code>.
	 */
	public String getLine();

	/**
	 * Checks if this working copy is most recent. Also known as 'tip' or
	 * 'head'.
	 * @return A <code>boolean</code>flag.
	 */
	public boolean isMostRecent();
	
	/**
	 * Returns the system-specific reference version.
	 * @return A <code>String</code>.
	 */
	public String getRevisionReference();
	
	/**
	 * Returns
	 * @return A <code>String</code>.
	 */
	public String getQualifier();
		
}
