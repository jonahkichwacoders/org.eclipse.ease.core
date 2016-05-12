/*******************************************************************************
 * Copyright (c) 2016 Christian Pontesegger and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Christian Pontesegger - initial API and implementation
 *******************************************************************************/
package org.eclipse.ease.ui.scripts.keywordhandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ease.Logger;
import org.eclipse.ease.ui.scripts.Activator;
import org.eclipse.ease.ui.scripts.repository.IScript;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class ToolbarHandler implements EventHandler {
	/** Trace enablement for the script UI integration. */
	public static final boolean TRACE_UI_INTEGRATION = Activator.getDefault().isDebugging()
			&& "true".equalsIgnoreCase(Platform.getDebugOption(Activator.PLUGIN_ID + "/debug/UIIntegration"));

	private static final String VIEW_ATTRIBUTE_NAME = "name";

	public static class Location {

		public String fScheme;
		public String fViewID;
		public String fName = null;

		public Location(final String scheme, final String location) {
			fScheme = scheme;

			String[] tokens = location.split("\\|");

			// try to find a view with matching ID or matching title
			final IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.ui.views");
			for (final IConfigurationElement e : config) {
				if ("view".equals(e.getName())) {
					String id = e.getAttribute("id");
					if (id.equals(tokens[0])) {
						fViewID = id;
						break;
					}

					String name = e.getAttribute(VIEW_ATTRIBUTE_NAME);
					if (name.equals(tokens[0])) {
						fViewID = id;
						break;
					}
				}
			}

			if (fViewID != null) {
				if ((tokens.length >= 2) && (!tokens[1].isEmpty()))
					fName = tokens[1];
			}
		}

		public String getID() {
			return fScheme + ":" + fViewID;
		}
	}

	private class UIIntegrationJob extends UIJob {

		private UIIntegrationJob() {
			super("Update toolbar scripts");
		}

		@Override
		public IStatus runInUIThread(final IProgressMonitor monitor) {
			if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null) {
				// we might get called before the workbench is loaded.
				// in that case delay execution until the workbench is ready
				PlatformUI.getWorkbench().addWindowListener(new IWindowListener() {

					@Override
					public void windowOpened(final IWorkbenchWindow window) {
					}

					@Override
					public void windowDeactivated(final IWorkbenchWindow window) {
					}

					@Override
					public void windowClosed(final IWorkbenchWindow window) {
					}

					@Override
					public void windowActivated(final IWorkbenchWindow window) {
						PlatformUI.getWorkbench().removeWindowListener(this);
						schedule(300);
					}
				});

				return Status.CANCEL_STATUS;
			}

			List<Event> events;
			synchronized (ToolbarHandler.this) {
				events = fKeywordEvents;
				fKeywordEvents = new ArrayList<Event>();
			}

			for (Event event : events) {
				IScript script = (IScript) event.getProperty("script");
				String value = (String) event.getProperty("value");
				String oldValue = (String) event.getProperty("oldValue");

				if ((oldValue != null) && (!oldValue.isEmpty()))
					removeContribution(script, oldValue);

				if ((value != null) && (!value.isEmpty()))
					addContribution(script, value);
			}

			return Status.OK_STATUS;
		}
	}

	/**
	 * @param value
	 */
	public static Collection<Location> toLocations(final String value) {
		Collection<Location> locations = new HashSet<Location>();

		for (String part : value.split(";"))
			locations.add(new Location("toolbar", part));

		return locations;
	}

	/** Event process job. */
	private final Job fUpdateUIJob = new UIIntegrationJob();

	/** Event queue to be processed. */
	private List<Event> fKeywordEvents = new ArrayList<Event>();

	/** UI contribution factories. */
	private final Map<String, ScriptContributionFactory> fContributionFactories = new HashMap<String, ScriptContributionFactory>();

	@Override
	public void handleEvent(final Event event) {
		synchronized (this) {
			fKeywordEvents.add(event);
		}

		fUpdateUIJob.schedule(300);
	}

	/**
	 * Add a toolbar script contribution.
	 *
	 * @param script
	 *            script to add
	 * @param value
	 *            toolbar keyword value
	 */
	protected void addContribution(final IScript script, final String value) {

		// process each location
		for (Location location : toLocations(value)) {
			Logger.trace(Activator.PLUGIN_ID, TRACE_UI_INTEGRATION, Activator.PLUGIN_ID,
					"Adding script \"" + script.getName() + "\" to " + location.fScheme + ":" + location.fViewID);

			if (location.fViewID != null) {
				IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(location.fViewID);

				// update contribution factory
				getContributionFactory(location.getID()).addScript(script);

				if ((view instanceof ViewPart) && (view.getViewSite() != null)) {
					// the view is already rendered, contributions will not be
					// considered anymore so add item directly
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=452203 for
					// details

					getContributionFactory(location.getID()).setAffectedContribution(view.getViewSite().getActionBars().getToolBarManager());
					view.getViewSite().getActionBars().getToolBarManager().add(new ScriptContributionItem(script));
					view.getViewSite().getActionBars().updateActionBars();
				}
			}
		}
	}

	/**
	 * Remove a toolbar script contribution.
	 *
	 * @param script
	 *            script to remove
	 * @param value
	 *            toolbar keyword value
	 */
	protected void removeContribution(final IScript script, final String value) {
		// process each location
		for (Location location : toLocations(value)) {
			Logger.trace(Activator.PLUGIN_ID, TRACE_UI_INTEGRATION, Activator.PLUGIN_ID,
					"Removing script \"" + script.getName() + "\" from " + location.fScheme + ":" + location.fViewID);

			// update contribution
			getContributionFactory(location.getID()).removeScript(script);

			if (location.fViewID != null) {
				IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(location.fViewID);
				if ((view instanceof ViewPart) && (view.getViewSite() != null)) {
					// the view is already rendered, contributions will not be
					// considered anymore so remove item directly
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=452203 for
					// details
					view.getViewSite().getActionBars().getToolBarManager().remove(script.getLocation());
					view.getViewSite().getActionBars().updateActionBars();
				}
			}
		}
	}

	protected ScriptContributionFactory getContributionFactory(final String location) {
		if (!fContributionFactories.containsKey(location))
			fContributionFactories.put(location, new ScriptContributionFactory(location));

		return fContributionFactories.get(location);
	}
}