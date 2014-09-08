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
package org.eclipse.ease.debugging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.ease.Script;

public class ScriptDebugStackFrame extends ScriptDebugElement implements IStackFrame {

	private final ScriptDebugThread fThread;

	private final IScriptDebugFrame fDebugFrame;

	private final List<ScriptDebugVariable> fVariables = new ArrayList<ScriptDebugVariable>();

	private boolean fDirty = true;

	public ScriptDebugStackFrame(final ScriptDebugThread thread, final IScriptDebugFrame debugFrame) {
		super(thread.getDebugTarget());
		fThread = thread;

		fDebugFrame = debugFrame;
	}

	@Override
	public IThread getThread() {
		return fThread;
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
		if(fDirty) {
			// TODO do not clear old variables, try to update them
			fVariables.clear();

			Map<String, Object> variables = getDebugFrame().getVariables();
			for(String name : variables.keySet()) {
				if(variables.get(name) != null) {
					ScriptDebugVariable variable = new ScriptDebugVariable(this, name, variables.get(name));
					fVariables.add(variable);
				}
			}

			fDirty = false;
		}

		return fVariables.toArray(new IVariable[fVariables.size()]);
	}

	@Override
	public boolean hasVariables() throws DebugException {
		return getVariables().length > 0;
	}

	@Override
	public int getLineNumber() throws DebugException {
		return getDebugFrame().getLineNumber();
	}

	@Override
	public int getCharStart() throws DebugException {
		return -1;
	}

	@Override
	public int getCharEnd() throws DebugException {
		return -1;
	}

	@Override
	public String getName() throws DebugException {
		return getScript().toString();
	}

	@Override
	public IRegisterGroup[] getRegisterGroups() throws DebugException {
		return new IRegisterGroup[0];
	}

	@Override
	public boolean hasRegisterGroups() throws DebugException {
		return false;
	}

	// TODO eventually move next three methods to base class
	@Override
	public boolean isTerminated() {
		return getThread().isTerminated();
	}

	@Override
	public boolean isSuspended() {
		return getThread().isSuspended();
	}

	@Override
	public boolean isStepping() {
		return getThread().isStepping();
	}

	public Script getScript() {
		return getDebugFrame().getScript();
	}

	public IScriptDebugFrame getDebugFrame() {
		return fDebugFrame;
	}

	public void setDirty() {
		fDirty = true;
	}
}
