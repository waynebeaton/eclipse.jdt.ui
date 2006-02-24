/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarimport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.filesystem.URIUtil;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptor;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.ProblemsLabelDecorator;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.filters.EmptyPackageFilter;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerUtil;
import org.eclipse.jdt.internal.ui.refactoring.binary.StubRefactoringHistoryWizard;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

/**
 * Jar import wizard page.
 * 
 * @since 3.2
 */
public final class JarImportWizardPage extends WizardPage {

	/** The jar import wizard page name */
	private static final String PAGE_NAME= "JarImportWizardPage"; //$NON-NLS-1$

	/** Is the wizard part of an import wizard? */
	private final boolean fImportWizard;

	/** The jar package data */
	private final JarImportData fJarImportData;

	/** The location text field */
	private Text fLocationField= null;

	/** The java model viewer */
	private TreeViewer fTreeViewer= null;

	/**
	 * Creates a new jar import wizard page.
	 * 
	 * @param data
	 *            the jar import data
	 * @param wizard
	 *            <code>true</code> if the wizard is part of an import wizard,
	 *            <code>false</code> otherwise
	 */
	public JarImportWizardPage(final JarImportData data, final boolean wizard) {
		super(PAGE_NAME);
		Assert.isNotNull(data);
		fJarImportData= data;
		fImportWizard= wizard;
		if (fImportWizard) {
			setTitle(JarImportMessages.JarImportWizardPage_page_title);
			setDescription(JarImportMessages.JarImportWizardPage_page_description);
		} else {
			setTitle(JarImportMessages.JarImportWizardPage_page_replace_title);
			setDescription(JarImportMessages.JarImportWizardPage_page_replace_description);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void createControl(final Composite parent) {
		initializeDialogUnits(parent);
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		createLocationGroup(composite);
		if (fImportWizard)
			createInputGroup(composite);
		createRenameGroup(composite);
		setPageComplete(false);
		if (fImportWizard && !fTreeViewer.getControl().isEnabled())
			setErrorMessage(JarImportMessages.JarImportWizardPage_no_jar_files);
		setControl(composite);
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.JARIMPORT_WIZARD_PAGE);
	}

	/**
	 * Creates a new grid data.
	 * 
	 * @param flag
	 *            the flags to use
	 * @param hspan
	 *            the horizontal span
	 * @param indent
	 *            the indent
	 * @return the grid data
	 */
	protected GridData createGridData(final int flag, final int hspan, final int indent) {
		final GridData data= new GridData(flag);
		data.horizontalIndent= indent;
		data.horizontalSpan= hspan;
		return data;
	}

	/**
	 * Creates the input group.
	 * 
	 * @param parent
	 *            the parent control
	 */
	protected void createInputGroup(final Composite parent) {
		Assert.isNotNull(parent);
		new Label(parent, SWT.NONE);
		final Label label= new Label(parent, SWT.NONE);
		label.setText(JarImportMessages.JarImportWizardPage_import_message);
		final StandardJavaElementContentProvider contentProvider= new StandardJavaElementContentProvider() {

			public Object[] getChildren(Object element) {
				if ((element instanceof IJavaProject) || (element instanceof IJavaModel))
					return super.getChildren(element);
				return new Object[0];
			}

			protected Object[] getJavaProjects(final IJavaModel model) throws JavaModelException {
				final Set set= new HashSet();
				final IJavaProject[] projects= model.getJavaProjects();
				for (int index= 0; index < projects.length; index++) {
					if (JarImportWizard.isValidJavaProject(projects[index])) {
						final Object[] roots= getPackageFragmentRoots(projects[index]);
						if (roots.length > 0)
							set.add(projects[index]);
					}
				}
				return set.toArray();
			}

			protected Object[] getPackageFragmentRoots(final IJavaProject project) throws JavaModelException {
				final Set set= new HashSet();
				final IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
				for (int offset= 0; offset < roots.length; offset++) {
					if (JarImportWizard.isValidClassPathEntry(roots[offset].getRawClasspathEntry()))
						set.add(roots[offset]);
				}
				return set.toArray();
			}

			public boolean hasChildren(final Object element) {
				return (element instanceof IJavaProject) || (element instanceof IJavaModel);
			}
		};

		final DecoratingLabelProvider labelProvider= new DecoratingLabelProvider(new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_BASICS | JavaElementLabelProvider.SHOW_OVERLAY_ICONS | JavaElementLabelProvider.SHOW_SMALL_ICONS), new ProblemsLabelDecorator());
		fTreeViewer= new TreeViewer(parent, SWT.SINGLE | SWT.BORDER);
		fTreeViewer.getTree().setLayoutData(createGridData(GridData.FILL_BOTH, 6, 0));
		fTreeViewer.setLabelProvider(labelProvider);
		fTreeViewer.setContentProvider(contentProvider);
		fTreeViewer.addFilter(new EmptyPackageFilter());
		fTreeViewer.setSorter(new JavaElementSorter());
		fTreeViewer.setAutoExpandLevel(2);
		fTreeViewer.setInput(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()));
		final IPackageFragmentRoot root= fJarImportData.getPackageFragmentRoot();
		if (root != null) {
			fTreeViewer.setSelection(new StructuredSelection(new Object[] { root}), true);
			fTreeViewer.expandToLevel(root, 1);
		}
		fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(final SelectionChangedEvent event) {
				handleInputChanged();
			}
		});
		if (contentProvider.getChildren(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot())).length == 0) {
			fTreeViewer.getControl().setEnabled(false);
			label.setEnabled(false);
		}
	}

	/**
	 * Creates the location group.
	 * 
	 * @param parent
	 *            the parent control
	 */
	protected void createLocationGroup(final Composite parent) {
		Assert.isNotNull(parent);
		new Label(parent, SWT.NONE).setText(JarImportMessages.JarImportWizardPage_import_label);
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 6, 0));
		composite.setLayout(new GridLayout(3, false));
		final Label label= new Label(composite, SWT.NONE);
		label.setText(JarImportMessages.JarImportWizardPage_location_label);
		label.setLayoutData(createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 1, 0));
		fLocationField= new Text(composite, SWT.SINGLE | SWT.BORDER);
		fLocationField.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fLocationField.addModifyListener(new ModifyListener() {

			public final void modifyText(final ModifyEvent event) {
				handleInputChanged();
			}
		});
		fLocationField.setFocus();
		final Button button= new Button(composite, SWT.PUSH);
		button.setText(JarImportMessages.JarImportWizardPage_browse_button_label);
		button.setLayoutData(createGridData(GridData.HORIZONTAL_ALIGN_FILL, 1, 0));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {

			public final void widgetSelected(final SelectionEvent event) {
				handleBrowseButtonSelected();
			}
		});
	}

	/**
	 * Creates the rename group.
	 * 
	 * @param parent
	 *            the parent control
	 */
	protected void createRenameGroup(final Composite parent) {
		Assert.isNotNull(parent);
		final Button button= new Button(parent, SWT.CHECK);
		button.setText(JarImportMessages.JarImportWizardPage_replace_jar_file);
		button.setSelection(!fJarImportData.isRenameJarFile());
		button.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent event) {
				fJarImportData.setRenameJarFile(!button.getSelection());
			}
		});
		if (fImportWizard && !fTreeViewer.getControl().isEnabled())
			button.setEnabled(false);
		if (!fImportWizard) {
			final GridData data= new GridData();
			data.horizontalIndent= IDialogConstants.HORIZONTAL_MARGIN;
			button.setLayoutData(data);
		}
	}

	/**
	 * Handles the browse button selected event.
	 */
	protected void handleBrowseButtonSelected() {
		final FileDialog file= new FileDialog(getShell(), SWT.OPEN);
		file.setText(JarImportMessages.JarImportWizardPage_browse_caption);
		file.setFilterNames(new String[] { "*.jar", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		file.setFilterExtensions(new String[] { "*.jar", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		final String path= file.open();
		if (path != null)
			fLocationField.setText(path);
	}

	/**
	 * Handles the input changed event.
	 */
	protected void handleInputChanged() {
		fJarImportData.setRefactoringHistory(null);
		fJarImportData.setRefactoringFileLocation(null);
		setErrorMessage(null);
		setPageComplete(true);
		handleJarFileChanged();
		if (isPageComplete())
			handlePackageFragmentRootChanged();
		if (fImportWizard && !fTreeViewer.getControl().isEnabled())
			setErrorMessage(JarImportMessages.JarImportWizardPage_no_jar_files);
		getContainer().updateButtons();
	}

	/**
	 * Handles the jar file changed event.
	 */
	protected void handleJarFileChanged() {
		if (fLocationField != null) {
			final String path= fLocationField.getText();
			if ("".equals(path)) { //$NON-NLS-1$
				setErrorMessage(JarImportMessages.JarImportWizardPage_empty_location);
				setPageComplete(false);
				return;
			} else {
				final File file= new File(path);
				if (!file.exists()) {
					setErrorMessage(JarImportMessages.JarImportWizardPage_invalid_location);
					setPageComplete(false);
					return;
				}
				ZipFile zip= null;
				try {
					zip= new ZipFile(file, ZipFile.OPEN_READ);
				} catch (IOException exception) {
					setErrorMessage(JarImportMessages.JarImportWizardPage_invalid_location);
					setPageComplete(false);
					return;
				}
				fJarImportData.setRefactoringFileLocation(URIUtil.toURI(path));
				ZipEntry entry= zip.getEntry(JarPackagerUtil.getRefactoringsEntry());
				if (entry == null) {
					setMessage(JarImportMessages.JarImportWizardPage_no_refactorings, INFORMATION);
					setPageComplete(true);
					return;
				}
				handleTimeStampChanged();
				if (fJarImportData.getExistingTimeStamp() > entry.getTime()) {
					setMessage(JarImportMessages.JarImportWizardPage_version_warning, WARNING);
					setPageComplete(true);
					return;
				}
				InputStream stream= null;
				try {
					stream= zip.getInputStream(entry);
					fJarImportData.setRefactoringHistory(RefactoringCore.getHistoryService().readRefactoringHistory(stream, JavaRefactoringDescriptor.JAR_IMPORTABLE));
				} catch (IOException exception) {
					setErrorMessage(JarImportMessages.JarImportWizardPage_no_refactorings);
					setPageComplete(false);
					return;
				} catch (CoreException exception) {
					JavaPlugin.log(exception);
					setErrorMessage(JarImportMessages.JarImportWizardPage_no_refactorings);
					setPageComplete(false);
					return;
				} finally {
					if (stream != null) {
						try {
							stream.close();
						} catch (IOException exception) {
							// Do nothing
						}
					}
				}
			}
		}
	}

	/**
	 * Handles the package fragment root changed event.
	 */
	protected void handlePackageFragmentRootChanged() {
		if (fTreeViewer != null) {
			final IStructuredSelection selection= (IStructuredSelection) fTreeViewer.getSelection();
			final Object[] elements= selection.toArray();
			if (elements.length != 1) {
				setErrorMessage(JarImportMessages.JarImportWizardPage_select_single_jar);
				setPageComplete(false);
				return;
			} else {
				final Object element= elements[0];
				if (element instanceof IPackageFragmentRoot)
					fJarImportData.setPackageFragmentRoot((IPackageFragmentRoot) element);
				else if (element instanceof IPackageFragment) {
					fJarImportData.setPackageFragmentRoot((IPackageFragmentRoot) ((IJavaElement) element).getParent());
				} else {
					setErrorMessage(JarImportMessages.JarImportWizardPage_select_single_jar);
					setPageComplete(false);
				}
			}
		}
	}

	/**
	 * Handles the time stamp changed event.
	 */
	protected void handleTimeStampChanged() {
		final IPackageFragmentRoot root= fJarImportData.getPackageFragmentRoot();
		if (root != null) {
			try {
				final URI uri= StubRefactoringHistoryWizard.getLocationURI(root.getRawClasspathEntry());
				if (uri != null) {
					final File file= new File(uri);
					if (file.exists()) {
						ZipFile zip= null;
						try {
							zip= new ZipFile(file, ZipFile.OPEN_READ);
							ZipEntry entry= zip.getEntry(JarPackagerUtil.getRefactoringsEntry());
							if (entry != null) {
								fJarImportData.setExistingTimeStamp(entry.getTime());
							}
						} catch (IOException exception) {
							// Just leave it
						}
					}
				}
			} catch (CoreException exception) {
				JavaPlugin.log(exception);
			}
		}
	}
}
