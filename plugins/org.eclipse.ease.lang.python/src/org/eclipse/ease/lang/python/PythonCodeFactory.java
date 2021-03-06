/*******************************************************************************
 * Copyright (c) 2013, 2016 Atos and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Arthur Daussy - initial implementation
 *     Christian Pontesegger - simplified to use base class
 *******************************************************************************/
package org.eclipse.ease.lang.python;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ease.Logger;
import org.eclipse.ease.modules.AbstractCodeFactory;
import org.eclipse.ease.modules.IEnvironment;
import org.eclipse.ease.modules.IScriptFunctionModifier;
import org.eclipse.ease.modules.ModuleHelper;

public class PythonCodeFactory extends AbstractCodeFactory {

	public static List<String> RESERVED_KEYWORDS = new ArrayList<String>();

	static {
		RESERVED_KEYWORDS.add("and");
		RESERVED_KEYWORDS.add("as");
		RESERVED_KEYWORDS.add("assert");
		RESERVED_KEYWORDS.add("break");
		RESERVED_KEYWORDS.add("class");
		RESERVED_KEYWORDS.add("continue");
		RESERVED_KEYWORDS.add("def");
		RESERVED_KEYWORDS.add("del");
		RESERVED_KEYWORDS.add("elif");
		RESERVED_KEYWORDS.add("else");
		RESERVED_KEYWORDS.add("except");
		RESERVED_KEYWORDS.add("exec");
		RESERVED_KEYWORDS.add("finally");
		RESERVED_KEYWORDS.add("for");
		RESERVED_KEYWORDS.add("from");
		RESERVED_KEYWORDS.add("global");
		RESERVED_KEYWORDS.add("if");
		RESERVED_KEYWORDS.add("import");
		RESERVED_KEYWORDS.add("in");
		RESERVED_KEYWORDS.add("is");
		RESERVED_KEYWORDS.add("lambda");
		RESERVED_KEYWORDS.add("not");
		RESERVED_KEYWORDS.add("or");
		RESERVED_KEYWORDS.add("pass");
		RESERVED_KEYWORDS.add("print");
		RESERVED_KEYWORDS.add("raise");
		RESERVED_KEYWORDS.add("return");
		RESERVED_KEYWORDS.add("try");
		RESERVED_KEYWORDS.add("while");
		RESERVED_KEYWORDS.add("with");
		RESERVED_KEYWORDS.add("yield");
	}

	@Override
	public String createFunctionWrapper(final IEnvironment environment, final String moduleVariable, final Method method) {

		final StringBuilder pythonCode = new StringBuilder();

		// parse parameters
		final List<Parameter> parameters = ModuleHelper.getParameters(method);

		// build parameter string
		final StringBuilder methodSignature = new StringBuilder();
		final StringBuilder methodCall = new StringBuilder();
		for (final Parameter parameter : parameters) {
			methodSignature.append(", ").append(parameter.getName());
			methodCall.append(", ").append(parameter.getName());
			if (parameter.isOptional())
				methodSignature.append(" = ").append(getDefaultValue(parameter));
		}

		if (methodSignature.length() > 2) {
			methodSignature.delete(0, 2);
			methodCall.delete(0, 2);
		}

		final StringBuilder body = new StringBuilder();

		// insert hooked pre execution code
		body.append(getPreExecutionCode(environment, method));

		// insert method call
		body.append('\t').append(IScriptFunctionModifier.RESULT_NAME).append(" = ");
		// use getattr to get around reserved names, e.g.:
		// __result = getattr(__environmentModule, "print")(args)
		// instead of the illegal:
		// __result = __environmentModule.print(args)
		body.append("getattr(").append(moduleVariable).append(", \"").append(method.getName()).append("\")");
		body.append('(');
		body.append(methodCall);
		body.append(")\n");

		// insert hooked post execution code
		body.append(getPostExecutionCode(environment, method));

		// insert return statement
		body.append("\treturn ").append(IScriptFunctionModifier.RESULT_NAME).append('\n');

		// build function declarations
		for (final String name : getMethodNames(method)) {
			String cleanName = name;
			if (!isValidMethodName(cleanName)) {
				// Try once to make a clean name, e.g. turn print into print_
				cleanName += "_";
				if (!isValidMethodName(cleanName)) {
					Logger.error(Activator.PLUGIN_ID,
							"The method name \"" + name + "\" from the module \"" + moduleVariable + "\" can not be wrapped because it's name is reserved");
				}
			}

			if (!cleanName.isEmpty()) {
				pythonCode.append("def ").append(cleanName).append('(').append(methodSignature).append("):\n");
				pythonCode.append(body);
				pythonCode.append('\n');
			}
		}

		return pythonCode.toString();
	}

	@Override
	public String getSaveVariableName(final String variableName) {
		return PythonHelper.getSaveName(variableName);
	}

	@Override
	public String classInstantiation(final Class<?> clazz, final String[] parameters) {
		final StringBuilder code = new StringBuilder();
		code.append(clazz.getCanonicalName());
		code.append('(');

		if (parameters != null) {
			for (final String parameter : parameters) {
				code.append(parameter);
				code.append(", ");
			}
			if (parameters.length > 0)
				code.delete(code.length() - 2, code.length());
		}

		code.append(')');

		return code.toString();
	}

	public boolean isValidMethodName(final String methodName) {
		return PythonHelper.isSaveName(methodName) && !RESERVED_KEYWORDS.contains(methodName);
	}

	@Override
	public String createFinalFieldWrapper(final IEnvironment environment, final String moduleVariable, final Field field) {
		return PythonHelper.getSaveName(field.getName()) + " = " + moduleVariable + '.' + field.getName() + '\n';
	}

	@Override
	protected String getNullString() {
		return "None";
	}

	@Override
	protected String getTrueString() {
		return "True";
	}

	@Override
	protected String getFalseString() {
		return "False";
	}

	@Override
	protected String getSingleLineCommentToken() {
		return "# ";
	}

	@Override
	protected String getMultiLineCommentStartToken() {
		return "\"\"\"";
	}

	@Override
	protected String getMultiLineCommentEndToken() {
		return "\"\"\"";
	}
}
