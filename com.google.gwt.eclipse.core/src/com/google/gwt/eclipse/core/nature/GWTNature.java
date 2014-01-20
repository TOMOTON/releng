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
package com.google.gwt.eclipse.core.nature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

import com.google.gwt.eclipse.core.internal.GWTActivator;

/**
 * Identifies a Java project as a GWT project.
 */
public class GWTNature implements IProjectNature {

	public static final String NATURE_ID = GWTActivator.PLUGIN_ID + ".gwtNature";

	private IProject project;
	
	@Override
	public void configure() throws CoreException {
		//? Intentionally left blank.
	}

	@Override
	public void deconfigure() throws CoreException {
		//? Intentionally left blank.
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}

}