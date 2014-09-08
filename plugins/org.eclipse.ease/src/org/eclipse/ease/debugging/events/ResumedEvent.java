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

public class ResumedEvent extends AbstractEvent implements IDebuggerEvent {

	private final Thread mThread;

	private final int mType;

	public ResumedEvent(final Thread thread, final int type) {
		mThread = thread;
		mType = type;
	}

	public Thread getThread() {
		return mThread;
	}

	public int getType() {
		return mType;
	}
}
