package org.eclipse.buckminster.pde.product.commands;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.buckminster.cmdline.Option;

public class TestOptionParsing extends TestCase {

	/**
	 * This test uses a patched version of ParseResult. This class is inside
	 * "org.eclipse.buckminster.cmdline.parser", but is not being exported by
	 * Buckminster in the new Buckminster release
	 */
	public void testParseWithShortOptionNames() throws Exception {
//		ProductExport export = new ProductExport();
//		List optionDescriptors = new ArrayList();
//		export.getOptionDescriptors(optionDescriptors);
//		ParseResult parseResult = ParseResult.parse(new String[] { "-Y",
//				"-Dd:\\temp\\hsp-export", "-Rhsp", "-Pwin32:win32/x86" },
//				optionDescriptors);
//		Option[] options = parseResult.getOptions();
//		for (Option currentOption : options) {
//			export.handleOption(currentOption);
//		}
//		assertEquals("The synchronize flag was not parsed correctly", true,
//				export.synchronize);
//		assertEquals("The -D flag was not parsed correctly",
//				"d:\\temp\\bms-export", export.destination);
//		assertEquals("The -R flag was not parsed correctly", "bms",
//				export.root);
//		String[][] resultTarget = new String[][] { { "win32", "win32", "x86",
//				"" } };
//		for (int i = 0; i < 4; i++) {
//			assertEquals("The -P flag was not parsed correctly",
//					resultTarget[0][i], export.targets[0][i]);
//		}
	}
}
