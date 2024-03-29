package org.eclipse.pde.team.internal;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.internal.resources.ResourceStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.team.IVersionUpdater;
import org.osgi.framework.Version;

@SuppressWarnings("restriction")
public class ManifestUpdater implements IVersionUpdater {

	@Override
	public void update(IFile manifestFile, Version version, IProgressMonitor monitor) throws CoreException {			
		if(version == null)
			throw new NullPointerException("Version cannot be undefined!");
		InputStream in = manifestFile.getContents();
		ByteArrayOutputStream content;
		try {
			String charset = manifestFile.getCharset();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
			byte[] separator = System.getProperty("line.separator").getBytes(charset);
			content = new ByteArrayOutputStream();
			String line;
			while((line = reader.readLine()) != null) {
				if(line.startsWith("Bundle-Version")) {
					content.write("Bundle-Version: ".getBytes(charset));
					content.write(version.toString().getBytes(charset));						
				} else {
					content.write(line.getBytes(charset));	
				}
				content.write(separator);
			}
		} catch(IOException ioe) {
			throw new CoreException(new ResourceStatus(Status.ERROR, manifestFile.getProjectRelativePath(), "Could not update MANIFEST.MF!", ioe));
		} finally {
			try { in.close(); } catch (Exception ignore) {}
		}
		manifestFile.setContents(new ByteArrayInputStream(content.toByteArray()), true, false, monitor);
	}
	
}
