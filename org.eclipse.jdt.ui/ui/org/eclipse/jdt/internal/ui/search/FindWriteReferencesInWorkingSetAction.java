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
import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Finds field write accesses of the selected element in working sets.
 * The action is applicable for selections representing a Java field.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class FindWriteReferencesInWorkingSetAction extends FindReferencesInWorkingSetAction {

	/**
	 * Creates a new <code>FindWriteReferencesInWorkingSetAction</code>.
	 * The user will be prompted to select the working sets.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FindWriteReferencesInWorkingSetAction(IWorkbenchSite site) {
		super(site, SearchMessages.getString("Search.FindWriteReferencesInWorkingSetAction.label"), new Class[] {IField.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindWriteReferencesInWorkingSetAction.tooltip")); //$NON-NLS-1$
	}

	/**
	 * Creates a new <code>FindWriteReferencesInWorkingSetAction</code>.
	 * 
	 * @param site			the site providing context information for this action
	 * @param workingSets	the working sets to be used in the search
	 */
	public FindWriteReferencesInWorkingSetAction(IWorkbenchSite site, IWorkingSet[] workingSets) {
		super(site, workingSets, new Class[] {IField.class});
	}

	/**
	 * Creates a new <code>FindWriteReferencesInWorkingSetAction</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public FindWriteReferencesInWorkingSetAction(JavaEditor editor) {
		super(editor, SearchMessages.getString("Search.FindWriteReferencesInWorkingSetAction.label"), new Class[] {IField.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindWriteReferencesInWorkingSetAction.tooltip")); //$NON-NLS-1$
	}

	/**
	 * Creates a new <code>FindWriteReferencesInWorkingSetAction</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public FindWriteReferencesInWorkingSetAction(JavaEditor editor, IWorkingSet[] workingSets) {
		super(editor, workingSets, new Class[] {IField.class});
	}

	int getLimitTo() {
		return IJavaSearchConstants.WRITE_ACCESSES;
	}

	String getOperationUnavailableMessage() {
		return SearchMessages.getString("JavaElementAction.operationUnavailable.field"); //$NON-NLS-1$
	}
}

