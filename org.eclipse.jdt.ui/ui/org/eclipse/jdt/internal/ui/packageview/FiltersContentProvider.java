/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;
import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import java.util.ArrayList;import java.util.List;import java.util.Vector;

/**
 * The FiltersContentProvider provides the elements for use by the list dialog
 * for selecting the patterns to apply.
 */ 
class FiltersContentProvider implements IStructuredContentProvider {
	private static List fgDefinedFilters;
	private static List fgDefaultFilters;

	private JavaElementPatternFilter fJavaFilter; 
	
	/**
	 * Create a FiltersContentProvider using the selections from the suppliec
	 * resource filter.
	 */
	public FiltersContentProvider(JavaElementPatternFilter filter) {
		fJavaFilter= filter;
	}
	/**
	 * Disposes of this content provider.  
	 * This is called by the viewer when it is disposed.
	 */
	public void dispose() {}
	/**
	 * Returns the filters currently defined for the workbench. 
	 */
	public static List getDefinedFilters() {
		if (fgDefinedFilters == null) {
			readFilters();
		}
		return fgDefinedFilters;
	}
	/**
	 * Returns the filters which are enabled by default.
	 *
	 * @return a list of strings
	 */
	public static List getDefaultFilters() {
		if (fgDefaultFilters == null) {
			readFilters();
		}
		return fgDefaultFilters;
	}
	
	/* (non-Jaadoc)
	 * Method declared in IStructuredContentProvider.
	 */
	public Object[] getElements(Object inputElement) {
		return getDefinedFilters().toArray();
	}
	/**
	 * Return the initially selected values
	 * @return java.lang.String[]
	 */
	public String[] getInitialSelections() {
		return fJavaFilter.getPatterns();
	}
	/* (non-Javadoc)
	 * Method declared on IContentProvider.
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
	
	/**
 	 * Reads the filters currently defined for the workbench. 
 	 */
	private static void readFilters() {
		fgDefinedFilters = new ArrayList();
		fgDefaultFilters = new ArrayList();
		JavaPlugin plugin = JavaPlugin.getDefault();
		if (plugin != null) {
			IExtensionPoint extension = plugin.getDescriptor().getExtensionPoint(JavaElementPatternFilter.FILTERS_TAG);
			if (extension != null) {
				IExtension[] extensions =  extension.getExtensions();
				for(int i = 0; i < extensions.length; i++){
					IConfigurationElement [] configElements = extensions[i].getConfigurationElements();
					for(int j = 0; j < configElements.length; j++){
						String pattern = configElements[j].getAttribute("pattern");
						if (pattern != null)
							fgDefinedFilters.add(pattern);
						String selected = configElements[j].getAttribute("selected");
						if (selected != null && selected.equalsIgnoreCase("true"))
							fgDefaultFilters.add(pattern);
					}
				}
			}		
		}
	}
}
