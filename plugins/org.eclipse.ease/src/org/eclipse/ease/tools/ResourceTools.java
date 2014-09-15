/*******************************************************************************
 * Copyright (c) 2014 Christian Pontesegger and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Christian Pontesegger - initial API and implementation
 *******************************************************************************/
package org.eclipse.ease.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.ease.service.IScriptService;
import org.eclipse.ease.service.ScriptType;
import org.eclipse.ease.urlhandler.WorkspaceURLConnection;
import org.eclipse.ui.PlatformUI;

public final class ResourceTools {

	private static final String PROJECT_SCHEME = "project";

	/**
	 * @deprecated
	 */
	@Deprecated
	private ResourceTools() {
	}

	/**
	 * Resolve a file from a given input location. Tries to resolve absolute and relative files within the workspace or file system. Relative files will be
	 * resolved against a provided parent location.
	 *
	 * @param location
	 *            file location to be resolved
	 * @param parent
	 *            location of parent resource
	 * @param exists
	 *            return file only if it exists
	 * @return {@link IFile}, {@link File} or <code>null</code>
	 */
	public static Object resolveFile(final Object location, final Object parent, final boolean exists) {
		if (location == null)
			return null;

		Object parentObject = parent;
		if ((parent != null) && (!(parent instanceof IResource)) && (!(parent instanceof File))) {
			parentObject = resolveFile(parent, null, true);
			if (parentObject == null)
				parentObject = resolveFolder(parent, null, true);
		}

		Object candidate = resolveAbsolute(location, parentObject, false);

		if ((candidate == null) && (parentObject != null))
			candidate = resolveRelativeFile(location, parentObject, exists);

		if (candidate instanceof IFile)
			return ((((IFile) candidate).exists()) || (!exists)) ? candidate : null;

		if ((candidate instanceof File) && ((((File) candidate).isFile()) || (!exists)))
			return ((((File) candidate).exists()) || (!exists)) ? candidate : null;

		// giving up
		return null;
	}

	/**
	 * Resolve a folder from a given input location. Tries to resolve absolute and relative folders within the workspace or file system. Relative folders will
	 * be resolved against a provided parent location.
	 *
	 * @param location
	 *            folder location to be resolved
	 * @param parent
	 *            location of parent resource
	 * @param exists
	 *            return folder only if it exists
	 * @return {@link IContainer}, {@link File} or <code>null</code>
	 */
	public static Object resolveFolder(final Object location, final Object parent, final boolean exists) {
		if (location == null)
			return null;

		Object parentObject = parent;
		if ((parent != null) && (!(parent instanceof IResource)) && (!(parent instanceof File))) {
			parentObject = resolveFile(parent, null, true);
			if (parentObject == null)
				parentObject = resolveFolder(parent, null, true);
		}

		Object candidate = resolveAbsolute(location, parentObject, true);

		if ((candidate == null) && (parentObject != null))
			candidate = resolveRelativeFolder(location, parentObject, exists);

		if (candidate instanceof IContainer)
			return ((((IContainer) candidate).exists()) || (!exists)) ? candidate : null;

		if ((candidate instanceof File) && ((((File) candidate).isDirectory()) || (!exists)))
			return ((((File) candidate).exists()) || (!exists)) ? candidate : null;

		// giving up
		return null;
	}

	private static Object resolveAbsolute(Object location, final Object parent, final boolean isFolder) {
		if (location instanceof IFile)
			return location;

		if (location instanceof String) {
			// try to convert to an URI
			try {
				location = URI.create((String) location);
			} catch (IllegalArgumentException e) {
				// throw on invalid URIs, ignore and continue with location as-is
			}
		}

		if (location instanceof URI) {
			// resolve file:// URIs
			try {
				location = new File((URI) location);
			} catch (Exception e) {
				// URI scheme is not "file"
			}
		}

		if (location instanceof File)
			return location;

		// nothing of the previous, try to resolve
		String reference = location.toString();

		if (reference.startsWith(PROJECT_SCHEME)) {
			// project relative link
			if (parent instanceof IResource) {
				if (isFolder)
					return ((IResource) parent).getProject().getFolder(new Path(reference.substring(PROJECT_SCHEME.length() + 3)));
				else
					return ((IResource) parent).getProject().getFile(new Path(reference.substring(PROJECT_SCHEME.length() + 3)));
			}

		} else if (reference.startsWith(WorkspaceURLConnection.SCHEME)) {
			// workspace absolute link
			if (isFolder) {
				Path path = new Path(reference.substring(WorkspaceURLConnection.SCHEME.length() + 3));
				if (path.segmentCount() > 1)
					return ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
				else
					return ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));

			} else
				return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(reference.substring(WorkspaceURLConnection.SCHEME.length() + 3)));

		} else {
			// maybe this is an absolute path within the file system
			File systemFile = new File(reference);
			if (systemFile.isAbsolute())
				return systemFile;
		}

		return null;

	}

	private static Object resolveRelativeFile(final Object location, final Object parent, final boolean exists) {
		String reference = location.toString();

		if (parent instanceof IResource) {
			// resolve a relative path in the workspace
			IFile relativeFile = null;
			if (parent instanceof IContainer)
				relativeFile = ((IContainer) parent).getFile(new Path(reference));
			else if (parent instanceof IResource)
				relativeFile = ((IResource) parent).getParent().getFile(new Path(reference));

			if ((relativeFile.exists()) || (!exists))
				return relativeFile;

		} else if (parent instanceof File) {
			// resolve a relative path in the file system
			File systemFile = null;
			if (((File) parent).isDirectory())
				systemFile = new File(((File) parent).getAbsolutePath() + File.separator + reference);

			else
				systemFile = new File(((File) parent).getParentFile().getAbsolutePath() + File.separator + reference);

			if (((systemFile.exists()) && (systemFile.isFile())) || (!exists))
				return systemFile;
		}

		// giving up
		return null;
	}

	private static Object resolveRelativeFolder(final Object location, final Object parent, final boolean exists) {
		String reference = location.toString();

		if (parent instanceof IResource) {
			// resolve a relative path in the workspace
			IContainer relativeFolder = null;

			if (parent instanceof IContainer)
				relativeFolder = ((IContainer) parent).getFolder(new Path(reference));
			else if (parent instanceof IResource)
				relativeFolder = ((IResource) parent).getParent().getFolder(new Path(reference));

			if ((relativeFolder.exists()) || (!exists))
				return relativeFolder;

		} else if (parent instanceof File) {
			// resolve a relative path in the file system
			File systemFolder = null;
			if (((File) parent).isDirectory())
				systemFolder = new File(((File) parent).getAbsolutePath() + File.separator + reference);

			else
				systemFolder = new File(((File) parent).getParentFile().getAbsolutePath() + File.separator + reference);

			if (((systemFolder.exists()) && (systemFolder.isDirectory())) || (!exists))
				return systemFolder;
		}

		// giving up
		return null;
	}

	public static String toLocation(final IResource resource) {
		return WorkspaceURLConnection.SCHEME + ":/" + resource.getFullPath().toPortableString();
	}

	public static ScriptType getScriptType(final IFile file) {
		// resolve by content type
		final IScriptService scriptService = (IScriptService) PlatformUI.getWorkbench().getService(IScriptService.class);
		try {
			IContentDescription contentDescription = file.getContentDescription();
			if (contentDescription != null)
				return scriptService.getScriptType(contentDescription.getContentType());

		} catch (CoreException e) {
		}

		// did not work, resolve by extension
		return scriptService.getScriptType(file.getFileExtension());
	}

	public static ScriptType getScriptType(final File file) {
		// resolve by extension
		return getScriptType(file.getName());
	}

	public static ScriptType getScriptType(final String location) {
		// resolve by extension
		if (location.contains(".")) {
			String extension = location.substring(location.lastIndexOf('.') + 1);

			final IScriptService scriptService = (IScriptService) PlatformUI.getWorkbench().getService(IScriptService.class);
			return scriptService.getScriptType(extension);
		}

		return null;
	}

	/**
	 * Verifies that a readable source (file/stream) exists at location.
	 *
	 * @param location
	 *            location to verify
	 * @return <code>true</code> when location is readable
	 */
	public static boolean exists(final String location) {
		if (resolveFile(location, null, true) != null)
			return true;

		// not a file, maybe an URI?
		try {
			URI uri = URI.create(location);
			InputStream stream = uri.toURL().openStream();
			if (stream != null) {
				stream.close();
				return true;
			}
		} catch (Exception e) {
			// cannot open / read from stream
		}

		return false;
	}

	public static Object getResource(final String location) {
		Object file = resolveFile(location, null, true);
		if (file != null)
			return file;

		// not a file, maybe a folder?
		file = resolveFolder(location, null, true);
		if (file != null)
			return file;

		// not a folder, maybe an URI?
		try {
			return URI.create(location);
		} catch (Exception e) {
			// cannot create URI
		}

		return null;
	}

	public static InputStream getInputStream(final String location) {
		try {
			Object resource = getResource(location);
			if (resource instanceof IFile)
				return ((IFile) resource).getContents();

			if (resource instanceof File)
				return new FileInputStream((File) resource);

			if (resource instanceof URI)
				return ((URI) resource).toURL().openStream();
		} catch (Exception e) {
			// cannot open stream
		}

		return null;
	}

	/**
	 * Converts an {@link IPath} representing a local file system path to a {@link URI}.
	 *
	 * @param path
	 *            The path to convert
	 * @return The URI representing the provided path
	 */
	public static URI toURI(final IPath path) {
		// source from org.eclipse.core.filesystem.URIUtil (Indigo version)
		if (path == null)
			return null;
		if (path.isAbsolute())
			return toURI(path.toFile().getAbsolutePath());
		try {
			// try to preserve the path as a relative path
			return new URI(escapeColons(path.toString()));
		} catch (URISyntaxException e) {
			return toURI(path.toFile().getAbsolutePath());
		}
	}

	/**
	 * Converts a String representing a local file system path to a {@link URI}. For example, this method can be used to create a URI from the output of
	 * {@link File#getAbsolutePath()}.
	 *
	 * @param pathString
	 *            The path string to convert
	 * @return The URI representing the provided path string
	 */
	public static URI toURI(String pathString) {
		// source from org.eclipse.core.filesystem.URIUtil (Indigo version)
		if (File.separatorChar != '/')
			pathString = pathString.replace(File.separatorChar, '/');
		final int length = pathString.length();
		StringBuffer pathBuf = new StringBuffer(length + 1);
		// There must be a leading slash in a hierarchical URI
		if ((length > 0) && (pathString.charAt(0) != '/'))
			pathBuf.append('/');
		// additional double-slash for UNC paths to distinguish from host separator
		if (pathString.startsWith("//")) //$NON-NLS-1$
			pathBuf.append('/').append('/');
		pathBuf.append(pathString);
		try {
			return new URI(EFS.SCHEME_FILE, null, pathBuf.toString(), null);
		} catch (URISyntaxException e) {
			// try java.io implementation
			return new File(pathString).toURI();
		}
	}

	/**
	 * Replaces any colon characters in the provided string with their equivalent URI escape sequence.
	 */
	private static String escapeColons(final String string) {
		// source from org.eclipse.core.filesystem.URIUtil (Indigo version)
		final String COLON_STRING = "%3A"; //$NON-NLS-1$
		if (string.indexOf(':') == -1)
			return string;
		int length = string.length();
		StringBuffer result = new StringBuffer(length);
		for (int i = 0; i < length; i++) {
			char c = string.charAt(i);
			if (c == ':')
				result.append(COLON_STRING);
			else
				result.append(c);
		}
		return result.toString();
	}

	/**
	 * Convert a location to a path in the workspace.
	 *
	 * @param location
	 *            location to convert (workspace://...)
	 * @return
	 */
	public static IPath toPath(final String location) {
		Object resource = resolveAbsolute(location, null, true);
		if (resource == null)
			resource = resolveAbsolute(location, null, false);

		return (resource instanceof IResource) ? ((IResource) resource).getFullPath() : null;
	}
}