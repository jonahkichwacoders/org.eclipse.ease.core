/*******************************************************************************
 * Copyright (c) 2015 Christian Pontesegger and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Martin Kloesch - initial API and implementation
 *******************************************************************************/
package org.eclipse.ease.ui.completion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.ease.completion.CompletionContext;
import org.eclipse.ease.completion.CompletionSource;
import org.eclipse.ease.completion.ICompletionContext;
import org.eclipse.ease.completion.ICompletionSource;
import org.eclipse.ease.completion.ICompletionSource.SourceType;
import org.eclipse.ease.modules.ModuleDefinition;
import org.eclipse.jface.fieldassist.ContentProposal;

/**
 * Base class for {@link ICompletionProvider} for modules.
 * 
 * Offers the actual context refinement and proposal calculation functionality.
 * 
 * Subclass must only override {@link #getModules()} to get the modules to be parsed.
 * 
 * @author Martin Kloesch
 *
 */
public abstract class ModuleCompletionBase extends AbstractCompletionProvider {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ease.ui.completion.ICompletionProvider#getProposals(org.eclipse.ease.modules.ICompletionContext)
	 */
	@Override
	public Collection<ICompletionSource> getProposals(final ICompletionContext context) {
		List<ICompletionSource> proposals = new ArrayList<ICompletionSource>();

		// Only add root level matches
		if (context != null && (context.getSourceStack() == null || context.getSourceStack().isEmpty())) {
			addMatches(context.getFilter(), proposals);
		}

		return proposals;
	}

	/**
	 * Adds all matching methods and members from all loaded modules to the given list of proposals.
	 * 
	 * @param toMatch
	 *            pattern to match methods and members against.
	 * @param proposals
	 *            List of {@link ContentProposal} to append matches to.
	 */
	protected void addMatches(String toMatch, final Collection<ICompletionSource> proposals) {
		// Show overloads only once
		Set<String> addedVariables = new HashSet<String>();

		for (ModuleDefinition definition : getModules()) {
			// add fields from modules
			for (Field field : definition.getFields()) {
				if ((field.getName().startsWith(toMatch)) && (toMatch.length() < field.getName().length())) {
					if (!addedVariables.contains(field.getName())) {
						addedVariables.add(field.getName());
						
						proposals.add(new CompletionSource(SourceType.MODULE_FIELD, field.getName(), definition.getModuleClass(), field));
					}
				}
			}

			// add methods from modules
			for (Method method : definition.getMethods()) {
				if ((method.getName().startsWith(toMatch)) && (toMatch.length() < method.getName().length())) {
					if (!addedVariables.contains(method.getName())) {
						addedVariables.add(method.getName());
						proposals.add(new CompletionSource(SourceType.MODULE_METHOD, method.getName(), definition.getModuleClass(), method));
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ease.ui.completion.IContextProvider#refineContext(org.eclipse.ease.modules.ICompletionContext)
	 */
	@Override
	public ICompletionContext refineContext(ICompletionContext context) {
		if (context == null || context.getSourceStack() == null || context.getSourceStack().isEmpty()) {
			return null;
		}

		ICompletionSource src = context.getSourceStack().get(0);
		if (src.getSourceType().equals(SourceType.METHOD) || src.getSourceType().equals(SourceType.LOCAL_FUNCTION)) {
			for (ModuleDefinition module : getModules()) {
				for (Method method : module.getMethods()) {
					if (method.getName().equals(src.getName())) {
						return createCompletionContext(module, method, context);
					}
				}
			}
		} else if (src.getSourceType().equals(SourceType.INSTANCE)) {
			for (ModuleDefinition module : getModules()) {
				for (Field field : module.getFields()) {
					if (field.getName().equals(src.getName())) {
						return createCompletionContext(module, field, context);
					}
				}
			}
		}

		return null;
	}

	/**
	 * Creates a refined completion context based on the given parameters.
	 * 
	 * @param module
	 *            The module the root element of the source stack belongs to.
	 * @param method
	 *            The actual method for the root element of the source stack.
	 * @param orig
	 *            The original {@link ICompletionContext} to parse further information from.
	 * @return new {@link ICompletionContext} if refined source stack could be calculated. <code>null</code> if new source stack cannot be calculated.
	 */
	protected ICompletionContext createCompletionContext(ModuleDefinition module, Method method, ICompletionContext orig) {
		List<ICompletionSource> newStack = orig.getSourceStack();
		newStack.set(0, new CompletionSource(SourceType.MODULE_METHOD, method.getName(), module.getModuleClass(), method));
		newStack = CompletionContext.refineSourceStack(newStack);
		if (newStack == null) {
			return null;
		} else {
			return new CompletionContext(orig.getInput(), orig.getFilter(), newStack);
		}
	}

	/**
	 * Creates a refined completion context based on the given parameters.
	 * 
	 * @param module
	 *            The module the root element of the source stack belongs to.
	 * @param field
	 *            The actual field for the root element of the source stack.
	 * @param orig
	 *            The original {@link ICompletionContext} to parse further information from.
	 * @return new {@link ICompletionContext} if refined source stack could be calculated. <code>null</code> if new source stack cannot be calculated.
	 */
	protected ICompletionContext createCompletionContext(ModuleDefinition module, Field field, ICompletionContext orig) {
		List<ICompletionSource> newStack = orig.getSourceStack();
		newStack.set(0, new CompletionSource(SourceType.MODULE_FIELD, field.getName(), module.getClass(), field));
		newStack = CompletionContext.refineSourceStack(newStack);
		if (newStack == null) {
			return null;
		} else {
			return new CompletionContext(orig.getInput(), orig.getFilter(), newStack);
		}
	}

	/**
	 * Getter method for available modules.
	 * 
	 * @return all modules to be parsed.
	 */
	protected abstract Collection<ModuleDefinition> getModules();
}