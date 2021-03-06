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
package org.eclipse.ease.lang.javascript.rhino.debugger;

import org.eclipse.ease.IScriptEngine;
import org.eclipse.ease.Script;
import org.eclipse.ease.debugging.events.IDebugEvent;

public class LineNumberDebugger extends RhinoDebugger {

	public LineNumberDebugger(IScriptEngine engine) {
		super(engine, false);
	}

	@Override
	protected void fireDispatchEvent(final IDebugEvent event) {
		// nothing to do
	}

	@Override
	protected boolean isTrackedScript(Script script) {
		// we do not want to check for breakpoints, etc
		return false;
	}
}
