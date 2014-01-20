/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation, Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.buckminster.pde.product.commands;

import org.eclipse.core.runtime.Path;

public class PathUtil {

	public static String extractPath(Path path) {
		if(path.segmentCount() > 1)
			return path.removeLastSegments(1).toString();
		else
			return ".";
	}

	public static String extractFileName(Path path) {
		if(path.segmentCount() > 0)
			return path.lastSegment();
		return path.toString();
	}


}
