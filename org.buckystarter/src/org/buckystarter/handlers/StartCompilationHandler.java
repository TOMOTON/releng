package org.buckystarter.handlers;

import org.eclipse.buckminster.gwt.internal.GWTCompile;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class StartCompilationHandler extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public StartCompilationHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		
		System.out.println("#########################");
		
		Display.getDefault().syncExec(new Runnable() {
			  public void run() {
					GWTCompile compile = new GWTCompile();
					compile.setProjectName("com.google.test");
					compile.setModule("com.google.test.Test");
					
					try {
						compile.go();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			  }
			}); 
		
		MessageDialog.openInformation(
				window.getShell(),
				"Buckystarter",
				"GWT Compile at your command!");
		return null;
	}
}
