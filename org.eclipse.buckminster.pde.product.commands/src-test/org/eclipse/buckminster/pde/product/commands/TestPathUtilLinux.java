package org.eclipse.buckminster.pde.product.commands;

import org.eclipse.core.runtime.Path;

import junit.framework.TestCase;

public class TestPathUtilLinux extends TestCase {
	
	public void testExtractPath() {
		Path path = new Path("/tmp/product.zip");
		String extractedPath = PathUtil.extractPath(path);
		assertEquals("The extracted path is not correct", "/tmp",
				extractedPath);
	}

	public void testExtractPathWithMultipleSegments() {
		Path path = new Path("/tmp/product-export/today/product.zip");
		String extractedPath = PathUtil.extractPath(path);
		assertEquals("The extracted path is not correct", "/tmp/hsp-export/today",
				extractedPath);
	}

	public void testExtractFileName() {
		Path path = new Path("/tmp/product.zip");
		String extractedPath = PathUtil.extractFileName(path);
		assertEquals("The extracted file name is not correct", "product.zip",
				extractedPath);
	}

	public void testExtractFileNameWithMultipleSegments() {
		Path path = new Path("/tmp/product-export/today/product.zip");
		String extractedPath = PathUtil.extractFileName(path);
		assertEquals("The extracted file name is not correct", "product.zip",
				extractedPath);
	}

	public void testExtractFileNameWithoutSegments() {
		Path path = new Path("/product.zip");
		String extractedPath = PathUtil.extractFileName(path);
		assertEquals("The extracted file name is not correct", "product.zip",
				extractedPath);
	}

}
