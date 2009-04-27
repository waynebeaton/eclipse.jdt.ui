/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 * 			(report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.progress.DeferredTreeContentManager;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.callhierarchy.CallerMethodWrapper;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodCall;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jdt.internal.corext.callhierarchy.RealCallers;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class CallHierarchyContentProvider implements ITreeContentProvider {
	private final static Object[] EMPTY_ARRAY= new Object[0];

    private DeferredTreeContentManager fManager;
    private CallHierarchyViewPart fPart;

    private class MethodWrapperRunnable implements IRunnableWithProgress {
        private MethodWrapper fMethodWrapper;
        private MethodWrapper[] fCalls= null;

        MethodWrapperRunnable(MethodWrapper methodWrapper) {
            fMethodWrapper= methodWrapper;
        }

        public void run(IProgressMonitor pm) {
        	fCalls= fMethodWrapper.getCalls(pm);
        }

        MethodWrapper[] getCalls() {
            if (fCalls != null) {
                return fCalls;
            }
            return new MethodWrapper[0];
        }
    }

    public CallHierarchyContentProvider(CallHierarchyViewPart part) {
        super();
        fPart= part;
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TreeRoot) {
			TreeRoot dummyRoot= (TreeRoot)parentElement;
			return dummyRoot.getRoots();

		} else if (parentElement instanceof RealCallers) {
			MethodWrapper parentWrapper= ((RealCallers)parentElement).getParent();
			RealCallers element= ((RealCallers)parentElement);
			if (fManager != null) {
				Object[] children= fManager.getChildren(new DeferredMethodWrapper(this, element));
				if (children != null)
					return children;
			}
			return fetchChildren(parentWrapper);

		} else if (parentElement instanceof MethodWrapper) {
			MethodWrapper methodWrapper= ((MethodWrapper)parentElement);

			if (shouldStopTraversion(methodWrapper)) {
				return EMPTY_ARRAY;
			} else {
				if (parentElement instanceof CallerMethodWrapper) {
					CallerMethodWrapper caller= (CallerMethodWrapper)parentElement;
					if (caller.getExpandWithConstructors()) {
						IType type= caller.getMember().getDeclaringType();
						try {
							if (type.isAnonymous()) {
								IMember anonymousClass= type;
								MethodCall anonymousConstructor= new MethodCall(anonymousClass);
								CallerMethodWrapper anonymousWrapper= (CallerMethodWrapper)caller.createMethodWrapper(anonymousConstructor);
								return new Object[] { anonymousWrapper, new RealCallers(methodWrapper, caller.getMethodCall()) };
							} else {
								IMember[] constructors= JavaElementUtil.getAllConstructors(type);
								if (constructors.length == 0) {
									constructors= new IType[] { type }; // type stands for the default constructor
								}
								Object children[]= new Object[constructors.length + 1];
								for (int j= 0; j < constructors.length; j++) {
									MethodCall constructor= new MethodCall(constructors[j]);
									CallerMethodWrapper constructorWrapper= (CallerMethodWrapper)caller.createMethodWrapper(constructor);
									children[j]= constructorWrapper;
								}
								children[constructors.length]= new RealCallers(methodWrapper, caller.getMethodCall());
								return children;
							}
						} catch (JavaModelException e) {
							JavaPlugin.log(e);
							return null;
						}

					}
				}
				if (fManager != null) {
					Object[] children= fManager.getChildren(new DeferredMethodWrapper(this, methodWrapper));
					if (children != null)
						return children;
				}
				return fetchChildren(methodWrapper);
			}
		}
        return EMPTY_ARRAY;
    }

    protected Object[] fetchChildren(final MethodWrapper methodWrapper) {
        IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
        MethodWrapperRunnable runnable= new MethodWrapperRunnable(methodWrapper);
        try {
            context.run(true, true, runnable);
        } catch (InvocationTargetException e) {
            ExceptionHandler.handle(e, CallHierarchyMessages.CallHierarchyContentProvider_searchError_title, CallHierarchyMessages.CallHierarchyContentProvider_searchError_message);
            return EMPTY_ARRAY;
        } catch (InterruptedException e) {
            Display.getDefault().asyncExec(new Runnable() {
            	public void run() {
            		CallHierarchyViewPart part= getViewPart();
            		if (part.getCallMode() == CallHierarchyViewPart.CALL_MODE_CALLERS) {
            			CallerMethodWrapper element= (CallerMethodWrapper)methodWrapper;
            			cancelSearchForElement(element);
            		}
            	}
            });
        }

        return runnable.getCalls();
    }


    /**
     * Refresh and collapses the given element or its parent on search cancel.
     * 
     * @param element the element on which search has been canceled and has to be collapsed
     * @since 3.5
     */
    protected void cancelSearchForElement(CallerMethodWrapper element) {
    	CallerMethodWrapper parent= (CallerMethodWrapper)element.getParent();
    	CallHierarchyViewer viewer= fPart.getViewer();
    	if (parent != null) {
    		viewer.refresh(parent);
    	} else {
    		IMember[] members= fPart.getInputElements();
    		viewer.setAutoExpandLevel(1); // set to 1 for root collapse on cancel
    		fPart.setInputElements(members);
    		viewer.setAutoExpandLevel(2); // set back to 2 for all other cases(bug 271446).
    	}
    	viewer.setExpandedState(element, false);
    }

	/**
     * Returns the call hierarchy view part.
	 * 
	 * @return the call hierarchy view part
	 * @since 3.5
	 */
    public CallHierarchyViewPart getViewPart() {    	
    	return fPart;
    }

	private boolean shouldStopTraversion(MethodWrapper methodWrapper) {
        return (methodWrapper.getLevel() > CallHierarchyUI.getDefault().getMaxCallDepth()) || methodWrapper.isRecursive();
    }

    /**
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     */
    public Object getParent(Object element) {
        if (element instanceof MethodWrapper) {
            return ((MethodWrapper) element).getParent();
        }

        return null;
    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {
        // Nothing to dispose
    }

    /**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		if (element == TreeRoot.EMPTY_ROOT || element == TreeTermination.SEARCH_CANCELED) {
			return false;
		}

		// Only certain members can have subelements, so there's no need to fool the
		// user into believing that there is more
		if (element instanceof MethodWrapper) {
			MethodWrapper methodWrapper= (MethodWrapper) element;
			if (! methodWrapper.canHaveChildren()) {
				return false;
			}
			if (shouldStopTraversion(methodWrapper)) {
				return false;
			}
			return true;
		} else if (element instanceof TreeRoot) {
			return true;
		} else if (element instanceof DeferredMethodWrapper) {
			// Err on the safe side by returning true even though
			// we don't know for sure that there are children.
			return true;
		}

		return false; // the "Update ..." placeholder has no children
	}

    /**
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
	 *      java.lang.Object, java.lang.Object)
	 */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    	if (oldInput instanceof TreeRoot) {
    		MethodWrapper[] roots = ((TreeRoot) oldInput).getRoots();
   			cancelJobs(roots);
    	}
        if (viewer instanceof AbstractTreeViewer) {
            fManager = new DeferredTreeContentManager((AbstractTreeViewer) viewer, fPart.getSite());
        }
    }

    /**
     * Cancel all current jobs.
     * @param wrappers the parents to cancel jobs for
     */
    void cancelJobs(MethodWrapper[] wrappers) {
        if (fManager != null && wrappers != null) {
        	for (int i= 0; i < wrappers.length; i++) {
				MethodWrapper wrapper= wrappers[i];
				fManager.cancel(wrapper);
			}
            if (fPart != null) {
                fPart.setCancelEnabled(false);
            }
        }
    }

    /**
     *
     */
    public void doneFetching() {
        if (fPart != null) {
            fPart.setCancelEnabled(false);
        }
    }

    /**
     *
     */
    public void startFetching() {
        if (fPart != null) {
            fPart.setCancelEnabled(true);
        }
    }
}