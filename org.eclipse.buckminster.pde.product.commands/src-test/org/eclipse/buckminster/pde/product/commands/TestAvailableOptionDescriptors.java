package org.eclipse.buckminster.pde.product.commands;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class TestAvailableOptionDescriptors extends TestCase {
	
	public void testAllOptionDescriptorsAvailable() throws Exception {
		ProductExport export = new ProductExport();
		List optionDescriptors = new ArrayList();
		export.getOptionDescriptors(optionDescriptors);
		Field[] fields = ProductExport.class.getDeclaredFields();
		List<Field> optionsFoundInProductExportClass = new ArrayList<Field>();
		for (Field currentField: fields){
			if (currentField.getName().endsWith("_DESCRIPTOR")){
				optionsFoundInProductExportClass.add(currentField);
			}
		}
		assertEquals("The number of descriptors found does not match the number of fields found", optionsFoundInProductExportClass.size(), optionDescriptors.size());		
	}

}
