package org.eclipse.pde.team.ui.internal;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.pde.team.internal.VersionActivator;

public class TeamPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public TeamPreferencePage() {
		super(GRID);
	}

	public void createFieldEditors() {
//		addField(new DirectoryFieldEditor("PATH", "&Directory preference:",
//				getFieldEditorParent()));
//		addField(new BooleanFieldEditor("BOOLEAN_VALUE",
//				"&An example of a boolean preference", getFieldEditorParent()));
//
//		addField(new RadioGroupFieldEditor("CHOICE",
//				"An example of a multiple-choice preference", 1,
//				new String[][] { { "&Choice 1", "choice1" },
//						{ "C&hoice 2", "choice2" } }, getFieldEditorParent()));
		addField(new StringFieldEditor("MAINLINE", "&Mainline",	getFieldEditorParent()));
//		addField(new StringFieldEditor("MySTRING2", "A &text preference:",
//				getFieldEditorParent()));
	}
	
	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(VersionActivator.getDefault().getPreferenceStore());
		// setDescription("A demonstration of a preference page implementation");
	}
	
	private void initializeDefaults() {
		String mainline = Platform.getPreferencesService().getString("org.eclipse.pde.team", "MAINLAINE", "N/A", null);
	}
	
}
