package com.google.gwt.eclipse.core;

import java.io.File;

public class GWTSDK {

	private String id;

	private File path;

	public GWTSDK(String id, File path) {
		this.id = id;
		this.path = path;
	}

	public String getId() {
		return id;
	}

	public File getPath() {
		return path;
	}

	public String[] getLibraries() {
		return new String[] { "gwt-dev", "gwt-user", "validation-api-1.0.0.GA" };
	}

}
