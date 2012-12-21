package org.eclipse.pde.team.internal;

import java.text.MessageFormat;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			String EXAMPLE_TEST = "Little Red ridinghood went to town!";
			String pattern = "(\\w)(\\s+)([\\.,])";
			System.out.println(EXAMPLE_TEST.replaceAll(pattern, "$1$3"));
			 MessageFormat form = new MessageFormat(
				     "The disk \"{main}\" contains {0} {2} file(s).");
			 System.err.println(form.getFormatsByArgumentIndex().length);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
