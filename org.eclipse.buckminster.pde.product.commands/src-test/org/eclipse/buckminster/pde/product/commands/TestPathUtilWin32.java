//package org.eclipse.buckminster.pde.product.commands;
//
//import junit.framework.TestCase;
//
//import org.eclipse.core.runtime.Path;
//
//public class TestPathUtilWin32 extends TestCase {
//
//	public void testExtractPath() {
//		Path path = new Path("d:\\temp\\product.zip");
//		String extractedPath = PathUtil.extractPath(path);
//		assertEquals("The extracted path is not correct", "d:/temp",
//				extractedPath);
//	}
//
//	public void testExtractPathWithMultipleSegments() {
//		Path path = new Path("d:\\temp\\product-export\\today\\product.zip");
//		String extractedPath = PathUtil.extractPath(path);
//		assertEquals("The extracted path is not correct", "d:/temp/hsp-export/today",
//				extractedPath);
//	}
//
//	public void testExtractFileName() {
//		Path path = new Path("d:\\temp\\product.zip");
//		String extractedPath = PathUtil.extractFileName(path);
//		assertEquals("The extracted file name is not correct", "product.zip",
//				extractedPath);
//	}
//
//	public void testExtractFileNameWithMultipleSegments() {
//		Path path = new Path("d:\\temp\\product-export\\today\\product.zip");
//		String extractedPath = PathUtil.extractFileName(path);
//		assertEquals("The extracted file name is not correct", "product.zip",
//				extractedPath);
//	}
//
//	public void testExtractFileNameWithoutSegments() {
//		Path path = new Path("d:\\product.zip");
//		String extractedPath = PathUtil.extractFileName(path);
//		assertEquals("The extracted file name is not correct", "product.zip", extractedPath);
//	}
//
//}
