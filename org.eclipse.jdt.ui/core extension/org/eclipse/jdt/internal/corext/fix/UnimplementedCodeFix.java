/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class UnimplementedCodeFix extends CompilationUnitRewriteOperationsFix {

	public static final class MakeTypeAbstractOperation extends CompilationUnitRewriteOperation {

		private final TypeDeclaration fTypeDeclaration;

		public MakeTypeAbstractOperation(TypeDeclaration typeDeclaration) {
			fTypeDeclaration= typeDeclaration;
		}

		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel linkedProposalPositions) throws CoreException {
			AST ast= cuRewrite.getAST();
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			Modifier newModifier= ast.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD);
			TextEditGroup textEditGroup= createTextEditGroup(CorrectionMessages.UnimplementedCodeFix_TextEditGroup_label, cuRewrite);
			rewrite.getListRewrite(fTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY).insertLast(newModifier, textEditGroup);

			LinkedProposalPositionGroup group= new LinkedProposalPositionGroup("modifier"); //$NON-NLS-1$
			group.addPosition(rewrite.track(newModifier), !linkedProposalPositions.hasLinkedPositions());
			linkedProposalPositions.addPositionGroup(group);
		}
	}

	public static IFix createCleanUp(CompilationUnit root, boolean addMissingMethod, boolean makeTypeAbstract, IProblemLocation[] problems) {
		Assert.isLegal(!addMissingMethod || !makeTypeAbstract);
		if (!addMissingMethod && !makeTypeAbstract)
			return null;

		if (problems.length == 0)
			return null;

		ArrayList operations= new ArrayList();

		for (int i= 0; i < problems.length; i++) {
			IProblemLocation problem= problems[i];
			if (addMissingMethod) {
				ASTNode typeNode= getSelectedTypeNode(root, problem);
				if (typeNode != null && !isTypeBindingNull(typeNode)) {
					operations.add(new AddUnimplementedMethodsOperation(typeNode, problem.getProblemId()));
				}
			} else {
				ASTNode typeNode= getSelectedTypeNode(root, problem);
				if (typeNode instanceof TypeDeclaration) {
					operations.add(new MakeTypeAbstractOperation(((TypeDeclaration) typeNode)));
				}
			}
		}

		if (operations.size() == 0)
			return null;

		String label;
		if (addMissingMethod) {
			label= CorrectionMessages.UnimplementedMethodsCorrectionProposal_description;
		} else {
			label= CorrectionMessages.UnimplementedCodeFix_MakeAbstractFix_label;
		}
		return new UnimplementedCodeFix(label, root, (CompilationUnitRewriteOperation[]) operations.toArray(new CompilationUnitRewriteOperation[operations.size()]));
	}

	public static UnimplementedCodeFix createAddUnimplementedMethodsFix(CompilationUnit root, IProblemLocation problem) {
		ASTNode typeNode= getSelectedTypeNode(root, problem);
		if (typeNode == null)
			return null;

		if (isTypeBindingNull(typeNode))
			return null;

		AddUnimplementedMethodsOperation operation= new AddUnimplementedMethodsOperation(typeNode, problem.getProblemId());
		return new UnimplementedCodeFix(CorrectionMessages.UnimplementedMethodsCorrectionProposal_description, root, new CompilationUnitRewriteOperation[] { operation });
	}

	public static UnimplementedCodeFix createMakeTypeAbstractFix(CompilationUnit root, IProblemLocation problem) {
		ASTNode typeNode= getSelectedTypeNode(root, problem);
		if (!(typeNode instanceof TypeDeclaration))
			return null;

		TypeDeclaration typeDeclaration= (TypeDeclaration) typeNode;
		MakeTypeAbstractOperation operation= new MakeTypeAbstractOperation(typeDeclaration);

		String label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_addabstract_description, typeDeclaration.getName().getIdentifier());
		return new UnimplementedCodeFix(label, root, new CompilationUnitRewriteOperation[] { operation });
	}

	public static ASTNode getSelectedTypeNode(CompilationUnit root, IProblemLocation problem) {
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (selectedNode == null)
			return null;

		if (selectedNode.getNodeType() == ASTNode.ANONYMOUS_CLASS_DECLARATION) { // bug 200016
			selectedNode= selectedNode.getParent();
		}

		if (selectedNode.getNodeType() == ASTNode.SIMPLE_NAME && selectedNode.getParent() instanceof AbstractTypeDeclaration) {
			return selectedNode.getParent();
		} else if (selectedNode.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
			return ((ClassInstanceCreation) selectedNode).getAnonymousClassDeclaration();
		} else if (selectedNode.getNodeType() == ASTNode.ENUM_CONSTANT_DECLARATION) {
			return ((EnumConstantDeclaration) selectedNode).getAnonymousClassDeclaration();
		} else if (selectedNode.getNodeType() == ASTNode.METHOD_DECLARATION && problem.getProblemId() == IProblem.EnumAbstractMethodMustBeImplemented) {
			EnumDeclaration enumDecl= (EnumDeclaration) selectedNode.getParent(); // bug 200026
			if (!enumDecl.enumConstants().isEmpty()) {
				return enumDecl;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private static boolean isTypeBindingNull(ASTNode typeNode) {
		if (typeNode instanceof AbstractTypeDeclaration) {
			AbstractTypeDeclaration abstractTypeDeclaration= (AbstractTypeDeclaration) typeNode;
			if (abstractTypeDeclaration.resolveBinding() == null)
				return true;

			return false;
		} else if (typeNode instanceof AnonymousClassDeclaration) {
			AnonymousClassDeclaration anonymousClassDeclaration= (AnonymousClassDeclaration) typeNode;
			if (anonymousClassDeclaration.resolveBinding() == null)
				return true;

			return false;
		} else if (typeNode instanceof EnumDeclaration) {
			EnumDeclaration enumDeclaration= (EnumDeclaration) typeNode;
			if (enumDeclaration.resolveBinding() == null)
				return true;

			return false;
		} else {
			return true;
		}
	}

	public UnimplementedCodeFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}