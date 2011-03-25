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
