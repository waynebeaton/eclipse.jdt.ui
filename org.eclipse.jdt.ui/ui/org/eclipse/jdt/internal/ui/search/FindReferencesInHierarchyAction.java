/*
 * Copyright (c) 2000, 2002 IBM Corp. and others..
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Finds references of the selected element in its hierarchy.
 * The action is applicable for selections representing a Java element.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class FindReferencesInHierarchyAction extends FindReferencesAction {

	/**
	 * Creates a new <code>FindReferencesInHierarchyAction</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FindReferencesInHierarchyAction(IWorkbenchSite site) {
		super(site, SearchMessages.getString("Search.FindHierarchyReferencesAction.label"), new Class[] {IType.class, IMethod.class, IField.class}); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindHierarchyReferencesAction.tooltip")); //$NON-NLS-1$
	}

	/**
	 * Creates a new <code>FindReferencesInHierarchyAction</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public FindReferencesInHierarchyAction(JavaEditor editor) {
		super(editor, SearchMessages.getString("Search.FindHierarchyReferencesAction.label"), new Class[] {IType.class, IMethod.class, IField.class}); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindHierarchyReferencesAction.tooltip")); //$NON-NLS-1$
	}

	FindReferencesInHierarchyAction(IWorkbenchSite site, String label, Class[] validTypes) {
		super(site, label, validTypes);
	}

	FindReferencesInHierarchyAction(JavaEditor editor, String label, Class[] validTypes) {
		super(editor, label, validTypes);
	}

	IJavaSearchScope getScope(IType type) throws JavaModelException {
		if (type == null)
			return super.getScope(type);
		return SearchEngine.createHierarchyScope(type);
	}
	
	String getScopeDescription(IType type) {
		String typeName= ""; //$NON-NLS-1$
		if (type != null)
			typeName= type.getElementName();
		return SearchMessages.getFormattedString("HierarchyScope", new String[] {typeName}); //$NON-NLS-1$
	}
}