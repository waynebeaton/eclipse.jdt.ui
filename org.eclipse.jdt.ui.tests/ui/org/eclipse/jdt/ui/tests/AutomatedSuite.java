/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.ui.tests.core.AddUnimplementedMethodsTest;
import org.eclipse.jdt.ui.tests.core.AllTypesCacheTest;
import org.eclipse.jdt.ui.tests.core.ImportOrganizeTest;
import org.eclipse.jdt.ui.tests.core.JavaModelUtilTest;
import org.eclipse.jdt.ui.tests.core.TextBufferTest;
import org.eclipse.jdt.ui.tests.core.TypeHierarchyTest;
import org.eclipse.jdt.ui.tests.core.TypeInfoTest;


/**
 * Test all areas of the UI.
 */
public class AutomatedSuite extends TestSuite {

	/**
	 * Returns the suite.  This is required to
	 * use the JUnit Launcher.
	 */
	public static Test suite() {
		return new AutomatedSuite();
	}

	/**
	 * Construct the test suite.
	 */
	public AutomatedSuite() {
		addTest(TypeInfoTest.suite());
		addTest(AddUnimplementedMethodsTest.suite());
		addTest(ImportOrganizeTest.suite());
		addTest(JavaModelUtilTest.suite());
		addTest(TextBufferTest.suite());
		addTest(TypeHierarchyTest.suite());
		
//disable - fails on motif		
//		addTest(AllTypesCacheTest.suite());
	}
	
	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), AutomatedSuite.class, args);
	}		


}

