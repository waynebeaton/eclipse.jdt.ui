/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.Collection;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.StringAsserts;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeTypeRefactoring;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;

/**
 * @author rfuhrer, tip
 *
 */
public class ChangeTypeRefactoringTests extends RefactoringTest {
	private static final Class clazz= ChangeTypeRefactoringTests.class;
	private static final String REFACTORING_PATH= "ChangeTypeRefactoring/";
	
	private static final boolean BUG_84670= true;

	public ChangeTypeRefactoringTests(String name) {
		super(name);
	} 
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test test) {
		return new RefactoringTestSetup(test);
	}
	
	private String getSimpleTestFileName(boolean input) {
		String fileName= "A_" + getName() + (input ? "_in" : "_out") + ".java";

		return fileName;
	}
	
	private String getTestFileName(boolean positive, boolean input){
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();

		fileName += (positive ? "positive/": "negative/");
		fileName += getSimpleTestFileName(input);
		return fileName;
	}
	
	private ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean positive, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(input), getFileContents(getTestFileName(positive, input)));
	}
	
	private ICompilationUnit createAdditionalCU(String fileName, IPackageFragment pack) throws Exception {
		String fullName= TEST_PATH_PREFIX + getRefactoringPath() + "positive/" + fileName + ".java";
		return createCU(pack, fileName + ".java", getFileContents(fullName));
	}

	private ChangeTypeRefactoring helper1(int startLine, int startColumn, int endLine, int endColumn, String selectedTypeName)
		throws Exception {
		ICompilationUnit	cu= createCUfromTestFile(getPackageP(), true, true);
		ISourceRange		selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ChangeTypeRefactoring		ref= ChangeTypeRefactoring.create(cu, selection.getOffset(), selection.getLength(), 
												   selectedTypeName);
	
		// TODO Set parameters on your refactoring instance from arguments...
	
		RefactoringStatus	activationResult= ref.checkInitialConditions(new NullProgressMonitor());	
	
		assertTrue("activation was supposed to be successful:" + activationResult.toString(), activationResult.isOK());																
	
		RefactoringStatus	checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
	
		assertTrue("precondition was supposed to pass but was " + checkInputResult.toString(), checkInputResult.isOK());
	
		performChange(ref, false);	
		
		String newSource= cu.getSource();
	
		assertEqualLines(getName() + ": ", getFileContents(getTestFileName(true, false)), newSource);
	
		
		return ref;
	}

	private void failHelper1(int startLine, int startColumn, int endLine, int endColumn,
							 int expectedStatus, String selectedTypeName) throws Exception {
		ICompilationUnit	cu= createCUfromTestFile(getPackageP(), false, true);
		ISourceRange		selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ChangeTypeRefactoring	ref= ChangeTypeRefactoring.create(cu, selection.getOffset(), selection.getLength(), 
												   selectedTypeName);
		RefactoringStatus	result= performRefactoring(ref);

		assertNotNull("precondition was supposed to fail", result);
		assertEquals("status", expectedStatus, result.getSeverity());

		String	canonAfterSrcName= getTestFileName(false, false);

		assertEqualLines(getFileContents(canonAfterSrcName), cu.getSource());
	}	

	//--- TESTS
	public void testLocalVarName() throws Exception {
		System.out.println("running testLocalVarName()");
		Collection types= helper1(5, 19, 5, 24, "java.util.Map").getValidTypeNames();
		Assert.assertTrue(types.size() == 1);
		Assert.assertTrue(types.contains("java.util.Map"));
	}
	public void testLocalVarType() throws Exception {
		System.out.println("running testLocalVarType()");
		Collection types= helper1(5, 9, 5, 18, "java.util.Map").getValidTypeNames();
		Assert.assertTrue(types.size() == 1);
		Assert.assertTrue(types.contains("java.util.Map"));		
	}
	public void testLocalVarDecl() throws Exception {
		System.out.println("running testLocalVarDecl()");
		Collection types= helper1(8, 9, 8, 23, "java.util.Map").getValidTypeNames();
		Assert.assertTrue(types.size() == 1);
		Assert.assertTrue(types.contains("java.util.Map")); 		
	}
	public void testLocalSuperTypesOfArrayList() throws Exception {
		System.out.println("running testLocalSuperTypesOfArrayList()");
		Collection types= helper1(5, 19, 5, 23, "java.util.List").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
				"java.lang.Object", "java.lang.Cloneable", "java.lang.Iterable", 
				"java.io.Serializable", "java.util.Collection", "java.util.List",
				"java.util.AbstractList", "java.util.AbstractCollection", "java.util.RandomAccess" };
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	public void testParameterName() throws Exception {
		System.out.println("running testParameterName()");
		Collection types= helper1(4, 31, 4, 36, "java.util.Map").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.Map"));
		Assert.assertTrue(types.contains("java.util.Dictionary"));		
	}
	public void testParameterType() throws Exception {
		System.out.println("running testParameterType()");
		Collection types= helper1(4, 21, 4, 29, "java.util.Dictionary").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.Map"));
		Assert.assertTrue(types.contains("java.util.Dictionary"));				
	}
	public void testParameterDecl() throws Exception {
		System.out.println("running testParameterDecl()");
		Collection types= helper1(4, 21, 4, 36, "java.util.Map").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.Map"));
		Assert.assertTrue(types.contains("java.util.Dictionary"));				
	}
	public void testFieldName() throws Exception {
		System.out.println("running testFieldName()");
		Collection types= helper1(10, 29, 10, 33, "java.util.AbstractList").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.AbstractList"));
		Assert.assertTrue(types.contains("java.util.List"));
	}
	public void testFieldType() throws Exception {
		System.out.println("running testFieldType()");
		Collection types= helper1(10, 19, 10, 27, "java.util.AbstractList").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.AbstractList"));
		Assert.assertTrue(types.contains("java.util.List"));
	}
	public void testFieldDecl() throws Exception {
		System.out.println("running testFieldDecl()");
		Collection types= helper1(10, 19, 10, 32, "java.util.AbstractList").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.AbstractList"));
		Assert.assertTrue(types.contains("java.util.List"));
	}
	public void testFieldUseSubtypesOfList() throws Exception {
		System.out.println("running testFieldUseSubtypesOfList()");
		Collection types= helper1(5, 22, 5, 26, "java.util.List").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.AbstractList"));
		Assert.assertTrue(types.contains("java.util.List"));
	}
	public void testFieldDeclSubtypesOfList() throws Exception {
		System.out.println("running testFieldDeclSubtypesOfList()");
		Collection types= helper1(8, 12, 8, 25, "java.util.List").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.AbstractList"));
		Assert.assertTrue(types.contains("java.util.List"));		
	}
	public void testLocalVarUse() throws Exception {
		System.out.println("running testLocalVarUse()");
		Collection types= helper1(6, 22, 6, 26, "java.util.AbstractList").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.AbstractList"));
		Assert.assertTrue(types.contains("java.util.List"));
	}
	public void testReturnTypeWithCall() throws Exception {
		System.out.println("running testReturnTypeWithCall()");
		Collection types= helper1(4, 12, 4, 20, "java.util.AbstractList").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.AbstractList"));
		Assert.assertTrue(types.contains("java.util.List"));		
	}
	public void testParameterNameWithOverride() throws Exception {
		System.out.println("running testParameterNameWithOverride()");
		Collection types= helper1(5, 38, 5, 40, "java.util.Collection").getValidTypeNames();
		Assert.assertTrue(types.size() == 3);
		Assert.assertTrue(types.contains("java.util.AbstractCollection"));
//		Assert.assertTrue(types.contains("java.util.ArrayList"));
		Assert.assertTrue(types.contains("java.util.List"));
		Assert.assertTrue(types.contains("java.util.Collection"));		
	}
	public void testParameterTypeWithOverride() throws Exception {
		System.out.println("running testParameterTypeWithOverride()");
		Collection types= helper1(10, 25, 10, 36, "java.util.List").getValidTypeNames();
		Assert.assertTrue(types.size() == 3);
		Assert.assertTrue(types.contains("java.util.AbstractCollection"));
//		Assert.assertTrue(types.contains("java.util.ArrayList"));
		Assert.assertTrue(types.contains("java.util.List"));
		Assert.assertTrue(types.contains("java.util.Collection"));		
	}
	public void testParameterDeclWithOverride() throws Exception {
		System.out.println("running testParameterDeclWithOverride()");
		Collection types= helper1(10, 25, 10, 39, "java.util.AbstractCollection").getValidTypeNames();
		Assert.assertTrue(types.size() == 3);
		Assert.assertTrue(types.contains("java.util.AbstractCollection"));
		Assert.assertTrue(types.contains("java.util.List"));
		Assert.assertTrue(types.contains("java.util.Collection"));		
	}
	public void testLocalVarCast() throws Exception {
		System.out.println("running testLocalVarCast()");
		Collection types= helper1(7, 24, 7, 24, "java.util.List").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
				"java.lang.Object", "java.lang.Cloneable", "java.lang.Iterable", 
				"java.io.Serializable", "java.util.Collection", "java.util.List",
				"java.util.AbstractList", "java.util.AbstractCollection", "java.util.RandomAccess" };
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	public void testReturnType() throws Exception {
		System.out.println("running testReturnType()");
		createAdditionalCU("A_testReturnType2", getPackageP());
		Collection types= helper1(6, 12, 6, 15, "java.util.Collection").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= { "java.lang.Object", "java.lang.Iterable", "java.util.Collection" };
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}	
	public void testFieldWithAccess() throws Exception {
		System.out.println("running testFieldWithAccess()");
		createAdditionalCU("A_testFieldWithAccess2", getPackageP());
		Collection types= helper1(6, 12, 6, 21, "java.util.Collection").getValidTypeNames();
		Assert.assertTrue(types.size() == 1);
		Assert.assertTrue(types.contains("java.util.Collection"));
	}	
	public void testParameterTypeWithOverriding() throws Exception {
		System.out.println("running testParameterTypeWithOverriding()");
		createAdditionalCU("A_testParameterTypeWithOverriding2", getPackageP());
		Collection types= helper1(6, 21, 6, 24, "java.util.Collection").getValidTypeNames();
		Assert.assertTrue(types.size() == 1);
		Assert.assertTrue(types.contains("java.util.Collection"));
	}
	public void testMultiCU() throws Exception {
		System.out.println("running testMultiCU()");
		createAdditionalCU("A_testMultiCUInterface1", getPackageP());
		createAdditionalCU("A_testMultiCUInterface2", getPackageP());
		createAdditionalCU("A_testMultiCUClass1", getPackageP());
		createAdditionalCU("A_testMultiCUClass2", getPackageP());
		Collection types= helper1(6, 21, 6, 26, "java.util.Collection").getValidTypeNames();
		Assert.assertTrue(types.size() == 1);
		Assert.assertTrue(types.contains("java.util.Collection"));
	}
	public void testHashMap() throws Exception {
		System.out.println("running testHashMap()");
		Collection types= helper1(15, 17, 15, 19, "java.util.AbstractMap").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.AbstractMap"));
		Assert.assertTrue(types.contains("java.util.Map"));
	}
	public void testString() throws Exception {
		System.out.println("running testString()");
		Collection types= helper1(4, 9, 4, 14, "java.lang.Object").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
				"java.lang.Object", "java.lang.CharSequence", "java.lang.Comparable<java.lang.String>", "java.io.Serializable"
		};
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	public void testInterfaceTypes() throws Exception {
		System.out.println("running testInterfaceTypes()");
		Collection types= helper1(4, 11, 4, 11, "p.I").getValidTypeNames();
		Assert.assertTrue(types.size() == 3);
		Assert.assertTrue(types.contains("java.lang.Object"));
		Assert.assertTrue(types.contains("p.I"));
		Assert.assertTrue(types.contains("p.A"));
	}
	public void testImport() throws Exception {
		System.out.println("running testImport()");
		Collection types= helper1(11, 9, 11, 17, "java.util.List").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
				"java.lang.Object", "java.lang.Cloneable", "java.lang.Iterable", 
				"java.io.Serializable", "java.util.Collection", "java.util.List",
				"java.util.AbstractList", "java.util.AbstractCollection", "java.util.RandomAccess" };
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	public void testParametricTypeWithParametricSuperType() throws Exception {
		System.out.println("running testParametricTypeWithParametricSuperType()");
		Collection types= helper1(5, 22, 5, 22, "java.util.Collection<java.lang.String>").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
				"java.util.Collection<java.lang.String>", 
				"java.lang.Object", 
				"java.lang.Iterable<java.lang.String>"  };
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	public void testParametricTypeWithNonParametricSuperType() throws Exception {
		System.out.println("running testParametricTypeWithParametricSuperType()");
		Collection types= helper1(5, 22, 5, 22, "java.lang.Object").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
				"java.util.Collection<java.lang.String>", 
				"java.lang.Object", 
				"java.lang.Iterable<java.lang.String>"  };
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	public void testNonParametricTypeWithParametricSuperType() throws Exception {
		System.out.println("running testNonParametricTypeWithParametricSuperType()");
		Collection types= helper1(5, 16, 5, 16, "java.lang.Comparable<java.lang.String>").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
				//"java.lang.String", 
				"java.lang.Comparable<java.lang.String>",				
				"java.lang.CharSequence",
				"java.io.Serializable",
				"java.lang.Object"		
		};
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	public void testNestedParametricType() throws Exception {
		System.out.println("running testNestedParametricType()");
		Collection types= helper1(5, 32, 5, 32, "java.util.AbstractCollection<java.util.Vector<java.lang.String>>").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
				"java.util.AbstractCollection<java.util.Vector<java.lang.String>>",
				"java.util.Collection<java.util.Vector<java.lang.String>>",
				"java.util.RandomAccess",
				"java.lang.Cloneable",
				"java.lang.Object",
				"java.io.Serializable",
				"java.util.List<java.util.Vector<java.lang.String>>",
				"java.util.AbstractList<java.util.Vector<java.lang.String>>",
				"java.lang.Iterable<java.util.Vector<java.lang.String>>"

		};
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	public void testParametricHashtable() throws Exception {
		System.out.println("running testParametricHashtable()");
		Collection types= helper1(5, 9, 5, 36, "java.util.Map<java.lang.String,java.lang.Integer>").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
				"java.util.Map<java.lang.String,java.lang.Integer>",				
				"java.util.Dictionary<java.lang.String,java.lang.Integer>",
				"java.lang.Object",
				"java.lang.Cloneable",
				"java.io.Serializable"
		};
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	public void testNestedParametricHashtable() throws Exception {
		System.out.println("running testNestedParametricHashtable()");
		Collection types= helper1(6, 9, 6, 44, "java.util.Dictionary<java.lang.String,java.util.Vector<java.lang.Integer>>").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
				"java.util.Map<java.lang.String,java.util.Vector<java.lang.Integer>>",				
				"java.util.Dictionary<java.lang.String,java.util.Vector<java.lang.Integer>>",
				"java.lang.Object",
				"java.lang.Cloneable",
				"java.io.Serializable"
		};
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	public void testNestedRawParametricHashtable() throws Exception {
		System.out.println("running testNestedRawParametricHashtable()");
		Collection types= helper1(6, 9, 6, 36, "java.util.Dictionary<java.lang.String,java.util.Vector>").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
				"java.util.Map<java.lang.String,java.util.Vector>",				
				"java.util.Dictionary<java.lang.String,java.util.Vector>",
				"java.lang.Object",
				"java.lang.Cloneable",
				"java.io.Serializable"
		};
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	public void testReorderTypeParameters() throws Exception {
		System.out.println("running testReorderTypeParameters()");
		Collection types= helper1(6, 28, 6, 28, "p.A<java.lang.Integer,java.lang.String>").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
				"java.lang.Object",
				"p.A<java.lang.Integer,java.lang.String>"

		};
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	public void test4TypeParameters() throws Exception {
		System.out.println("running test4TypeParameters()");
		Collection types= helper1(3, 40, 3, 40, "p.I<java.lang.Double,java.lang.Float>").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
				"java.lang.Object",
				"p.I<java.lang.Double,java.lang.Float>",
				"p.J<java.lang.Float,java.lang.Double>",
				"p.I<java.lang.String,java.lang.Integer>"
		};
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	public void testRawComment() throws Exception {
		System.out.println("running testRawComment()");
		Collection types= helper1(5, 27, 5, 27, "java.util.Collection").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
				"java.util.Collection"
		};
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	public void testNonRawComment() throws Exception {
		System.out.println("running testNonRawComment()");
		Collection types= helper1(5, 31, 5, 31, "java.util.Collection<java.lang.String>").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
				"java.util.Collection<java.lang.String>"
		};
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	
	public void testUnrelatedTypeParameters() throws Exception {
		System.out.println("running testUnrelatedTypeParameters()");
		Collection types= helper1(3, 20, 3, 20, "p.E<java.lang.String>").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
				"p.F",
				"p.E<java.lang.String>",
				"java.lang.Object"

		};
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	public void testUnboundTypeParameter() throws Exception {
		System.out.println("running testUnboundTypeParameters()");
		Collection types= helper1(5, 17, 5, 20, "java.lang.Iterable<T>").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
			    "java.lang.Iterable<T>",
			    "java.util.Collection<T>",
				"java.lang.Object"

		};
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	public void testRawSubType() throws Exception {
		if (BUG_84670) {
			System.out.println("Disabled to due bug 84670");
			return;
		}
		System.out.println("running testRawSubType()");
		Collection types= helper1(7, 5, 7, 10, "java.lang.Comparable<java.lang.String>").getValidTypeNames();
		String[] actual= (String[]) types.toArray(new String[types.size()]);
		String[] expected= {
			    "java.lang.Comparable<java.lang.String>"

		};
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}
	
	// tests that are supposed to fail
	
	public void testInvalidSelection() throws Exception {
		System.out.println("running testInvalidSelection()");
		failHelper1(5, 23, 5, 37, 4, "java.lang.Object");
	}
	public void testBogusSelection() throws Exception {
		System.out.println("running testBogusSelection()");
		failHelper1(6, 23, 6, 35, 4, "java.lang.Object");
	}
	public void testMultiDeclaration() throws Exception {
		System.out.println("running testMultiDeclaration()");
		failHelper1(8, 22, 8, 26, 4, "java.util.List");		
	}
	public void testUpdateNotPossible() throws Exception {
		System.out.println("running testUpdateNotPossible()");
		failHelper1(5, 19, 5, 20, 4, "java.util.Hashtable");
	}
	public void testArray() throws Exception {
		System.out.println("running testArray()");
		failHelper1(5, 18, 5, 19, 4, "java.lang.Object");
	}
	public void testPrimitive() throws Exception {
		System.out.println("running testPrimitive()");
		failHelper1(5, 13, 5, 13, 4, "java.lang.Object");
	}
	public void testOverriddenBinaryMethod() throws Exception {
		System.out.println("running testOverriddenBinaryMethod()");
		failHelper1(3, 12, 3, 17, 4, "java.lang.Object");
	}
	public void testFieldOfLocalType() throws Exception {
		System.out.println("running testFieldOfLocalType()");
		failHelper1(5, 21, 5, 45, 4, "java.lang.Object");
	}
	public void testObjectReturnType() throws Exception {
		System.out.println("running testObjectReturnType()");
		failHelper1(3, 17, 3, 22, 4, "java.lang.Object");
	}
	public void testTypeParameter() throws Exception {
		System.out.println("running testTypeParameter()");
		failHelper1(3, 9, 3, 9, 4, "java.lang.Object");
	}
	public void testEnum() throws Exception {
		System.out.println("running testEnum()");
		failHelper1(9, 11, 9, 11, 4, "java.lang.Object");
	}
}
