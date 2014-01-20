/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.google.gwt.eclipse.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class GWTRuntimeContainerInitializer extends	ClasspathContainerInitializer {

	@Override
	public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
	    GWTSDKClasspathContainer classpathContainer = GWTSDKClasspathContainer.resolveClasspathContainer(containerPath, javaProject);
	    // Container will be set to null if it could not be resolved which will
	    // result in a classpath error for the project.
	    JavaCore.setClasspathContainer(containerPath,
	        new IJavaProject[] { javaProject },
	        new IClasspathContainer[] { classpathContainer }, null);
	}

	@Override
	public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
		return true;
	}

	@Override
	public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project, IClasspathContainer containerSuggestion)	throws CoreException {
		throw new UnsupportedOperationException();
	}

}
