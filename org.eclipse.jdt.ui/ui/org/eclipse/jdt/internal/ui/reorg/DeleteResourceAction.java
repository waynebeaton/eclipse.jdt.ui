/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.MultiStatus;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIException;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jdt.internal.corext.refactoring.reorg.*;

/** 
 * Action for deleting elements in a delete target.
 */
class DeleteResourceAction extends ReorgAction {
	private boolean fDeleteProjectContent;

	public DeleteResourceAction(ISelectionProvider provider) {
		super(ReorgMessages.getString("deleteAction.label"), provider); //$NON-NLS-1$
		setDescription(ReorgMessages.getString("deleteAction.description")); //$NON-NLS-1$
	}

	/**
	 * The user has invoked this action
	 */
	public void run() {
		DeleteRefactoring refactoring= new DeleteRefactoring(getStructuredSelection().toList());
		
		fDeleteProjectContent= false;
		if (!confirmDelete(getStructuredSelection()))
			return;

		if (hasReadOnlyResources() && !isOkToDeleteReadOnly()) 
			return;
		refactoring.setDeleteProjectContents(fDeleteProjectContent);
		try{
			MultiStatus status= ReorgAction.perform(refactoring);
			if (!status.isOK()) {
				JavaUIException t= new JavaUIException(status);
				ExceptionHandler.handle(t, "Delete", "Unexpected exception. See log for details.");
			}
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, "Delete", "Unexpected exception. See log for details.");
			return;
		}	
	}
	
	private static boolean isOkToDeleteReadOnly(){
			String msg= ReorgMessages.getString("deleteAction.confirmReadOnly"); //$NON-NLS-1$
			String title= ReorgMessages.getString("deleteAction.checkDeletion"); //$NON-NLS-1$
			return MessageDialog.openQuestion(
					JavaPlugin.getActiveWorkbenchShell(),
					title,
					msg);
	}
	
	private boolean hasReadOnlyResources(){
		for (Iterator iter= getStructuredSelection().iterator(); iter.hasNext();){	
			if (ReorgUtils.shouldConfirmReadOnly(iter.next()))
				return true;
		}
		return false;
	}

	/* non java-doc
	 * @see IRefactoringAction#canOperateOn(IStructuredSelection)
	 */
	public boolean canOperateOn(IStructuredSelection selection) {
		return ReorgAction.canActivate(new DeleteRefactoring(selection.toList()));
	}
	
	private boolean confirmDelete(IStructuredSelection selection) {
		List projects= getProjects(selection);
		if (projects.isEmpty())
			return confirmNonProjects();
	 	else
			return confirmProjets(projects);
	}
	
	private boolean confirmNonProjects() {	
		String title= ReorgMessages.getString("deleteAction.confirm.title"); //$NON-NLS-1$
		String label= ReorgMessages.getString("deleteAction.confirm.message"); //$NON-NLS-1$
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		return MessageDialog.openConfirm(parent, title, label);
	}
	
	private boolean confirmProjets(List projects) {
		MessageDialog dialog =
			new MessageDialog(JavaPlugin.getActiveWorkbenchShell(), 
				ReorgMessages.getString("deleteAction.deleteContents.title"),  //$NON-NLS-1$
				null, // accept the default window icon
				createDialogMessage(projects),
				MessageDialog.QUESTION,
				new String[] {
					IDialogConstants.YES_LABEL,
					IDialogConstants.NO_LABEL,
					IDialogConstants.CANCEL_LABEL },
				0); // yes is the default
				
		int code = dialog.open();
		switch (code) {
			case 0 : // YES
				fDeleteProjectContent = true;
				return true;
			case 1 : // NO
				fDeleteProjectContent = false;
				return true;
			default : // CANCEL and close dialog
				return false;
		}
	}
	
	private static String createDialogMessage(List projects){
		if (projects.size() == 1) {
			IProject project = (IProject)projects.get(0);
			return ReorgMessages.getFormattedString("deleteAction.confirmDelete1Project.message",  //$NON-NLS-1$
																				new String[] { project.getName(), project.getLocation().toOSString()});
		} else {
			return ReorgMessages.getFormattedString("deleteAction.confirmDeleteNProjects.message", //$NON-NLS-1$
																			new String[] {String.valueOf(projects.size()) });
		}
	}
	
	private static List getProjects(IStructuredSelection selection) {
		List result= new ArrayList(selection.size());
		for(Iterator iter= selection.iterator(); iter.hasNext(); ) {
			Object element= iter.next();
			if (element instanceof IJavaProject) {
				try {
					result.add(((IJavaProject)element).getUnderlyingResource());
				} catch (JavaModelException e) {
					if (!e.isDoesNotExist()) {
						//do not show error dialogs in a loop
						JavaPlugin.log(e);
					}
				}
			}
		}
		return result;
	}	
}
