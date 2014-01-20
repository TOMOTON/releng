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
package com.google.gwt.eclipse.core.internal;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.TreeMap;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

import com.google.gwt.eclipse.core.GWTSDK;

public class GWTActivator implements BundleActivator {
	
	public static final String PLUGIN_ID = "com.google.gwt.eclipse.core";
	
	private static final String SDK_BUNDLE_ID = "com.google.gwt.eclipse.sdkbundle";

	private static final String SDK_REGISTRANT_PROPERTIES = "SdkBundleRegistrant.properties";
	
	private static final String SDK_BUNDLE_PATH = "sdkBundlePath";
	
	private static final String SDK_TYPE = "sdkType";

	public static TreeMap<Version, GWTSDK> findAllGWTSDKs() {
		TreeMap<Version, GWTSDK> result = new TreeMap<Version, GWTSDK>();
		BundleContext context = FrameworkUtil.getBundle(GWTActivator.class).getBundleContext();
		for (Bundle bundle : context.getBundles()) {
			URL location = bundle.getEntry(SDK_REGISTRANT_PROPERTIES);
			InputStream in = null;
			try {
				if(location != null) {
					Properties properties = new Properties();
					in = location.openStream(); 
					properties.load(in);
					if("GWT".equals(properties.get(SDK_TYPE))) {
						String id = properties.getProperty(SDK_BUNDLE_PATH);
						File path = new File(FileLocator.toFileURL(location).toURI());
						File sdkPath = new File(path.getParentFile(), id);
						GWTSDK sdk = new GWTSDK(id, sdkPath);
						result.put(bundle.getVersion(), sdk);
					}
				}
			} catch (Exception e) {
				try { in.close(); } catch (Exception ignore) {}
			}
		} 
		return result;
	}
	
	@Override
	public void start(BundleContext context) throws Exception {
		//? Intentionally left blank.
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		//? Intentionally left blank.
	}

}
