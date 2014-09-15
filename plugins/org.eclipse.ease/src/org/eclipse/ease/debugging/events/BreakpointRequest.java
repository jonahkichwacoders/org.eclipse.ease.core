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
package org.eclipse.ease.debugging.events;

import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.ease.Script;

public class BreakpointRequest extends AbstractEvent implements IModelRequest {

	private final Script fScript;

	private final IBreakpoint fBreakpoint;

	public BreakpointRequest(final Script script, final IBreakpoint breakpoint) {
		fScript = script;
		fBreakpoint = breakpoint;
	}

	public Script getScript() {
		return fScript;
	}

	public IBreakpoint getBreakpoint() {
		return fBreakpoint;
	}
}