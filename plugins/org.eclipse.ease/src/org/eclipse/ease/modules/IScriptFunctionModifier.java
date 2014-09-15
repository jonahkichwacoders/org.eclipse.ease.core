/*******************************************************************************
 * Copyright (c) 2013 Christian Pontesegger and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Christian Pontesegger - initial API and implementation
 *******************************************************************************/
package org.eclipse.ease.modules;

import java.lang.reflect.Method;

/**
 * Interface for modules that want to globally modify JavaScript code used to wrap Java method calls. An implementing module will affect only code generated by
 * the autoWrap mechanism of {@link AbstractJavaScriptModule}. Modules loaded before will not be affected. Modules loaded afterwards will be affected by code
 * insertions.
 */
public interface IScriptFunctionModifier {

	/** intermediate name of original method call return value. */
	String RESULT_NAME = "__result";

	/**
	 * Get code that shall be executed before actual method gets called. As multiple modules might want to inject code make sure to avoid interactions by using
	 * commonly used variable names.
	 * 
	 * @param method
	 *            method that will be called afterwards.
	 * 
	 * @return script code to be inserted before method call
	 */
	String getPreExecutionCode(Method method);

	/**
	 * Get code that shall be executed after actual method gets called. As multiple modules might want to inject code make sure to avoid interactions by using
	 * commonly used variable names.
	 * 
	 * @param method
	 *            method that will be called before.
	 * 
	 * @return script code to be inserted after method call
	 */
	String getPostExecutionCode(Method method);
}