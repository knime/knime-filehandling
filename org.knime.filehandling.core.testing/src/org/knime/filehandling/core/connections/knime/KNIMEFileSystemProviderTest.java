package org.knime.filehandling.core.connections.knime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Test;

public class KNIMEFileSystemProviderTest {
	
	@Test
	public void createNewFileSystemFromKNIMEUri() throws IOException, URISyntaxException {
		KNIMEFileSystemProvider provider = new KNIMEFileSystemProvider();
		
		Map<String, ?> env = null;
		URI knimeURI = URI.create("knime://knime.workflow/data/test.txt");
		FileSystem fileSystem = provider.newFileSystem(knimeURI, env);
		
		assertTrue(fileSystem != null);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void createNewFileSystemFromNonKNIMEUriThrowsException() throws IOException, URISyntaxException {
		KNIMEFileSystemProvider provider = new KNIMEFileSystemProvider();
		
		Map<String, ?> env = null;
		URI knimeURI = URI.create("strange-protocol://knime.workflow/data/test.txt");
		provider.newFileSystem(knimeURI, env);
	}
	
	@Test(expected = FileSystemAlreadyExistsException.class)
	public void creatingNewFileSystemForAlreadyExistingFileSystemThrowsException() throws IOException {
		KNIMEFileSystemProvider provider = new KNIMEFileSystemProvider();
		
		Map<String, ?> env = null;
		URI knimeURI = URI.create("knime://knime.workflow/data/test.txt");
		provider.newFileSystem(knimeURI, env);
		provider.newFileSystem(knimeURI, env);
	}
	
	@Test
	public void eacMountpoointGetsItsOwnFileSystem() throws IOException {
		KNIMEFileSystemProvider provider = new KNIMEFileSystemProvider();
		
		Map<String, ?> env = null;
		URI mountPoint1 = URI.create("knime://mount-point-1/data/test.txt");
		URI mountPoint2 = URI.create("knime://mount-point-2/different-data/test.txt");
		FileSystem mountPoint1FS = provider.newFileSystem(mountPoint1, env);
		FileSystem mountPoint2FS = provider.newFileSystem(mountPoint2, env);
		
		assertTrue(mountPoint1FS != mountPoint2FS);
	}
	
	@Test(expected = FileSystemAlreadyExistsException.class)
	public void mountPointAndWorkflowRelativeURIShareFS() throws IOException {
		KNIMEFileSystemProvider provider = new KNIMEFileSystemProvider();
		
		Map<String, ?> env = null;
		URI mountPointRelativeURI = URI.create("knime://knime.mountpoint/data/test.txt");
		URI workflowRelativeURI = URI.create("knime://knime.workflow/data/test.txt");
		provider.newFileSystem(mountPointRelativeURI, env);
		provider.newFileSystem(workflowRelativeURI, env);
	}
	
	@Test(expected = FileSystemAlreadyExistsException.class)
	public void mountPointAndNodeRelativeURIShareFS() throws IOException {
		KNIMEFileSystemProvider provider = new KNIMEFileSystemProvider();
		
		Map<String, ?> env = null;
		URI mountPointRelativeURI = URI.create("knime://knime.mountpoint/data/test.txt");
		URI nodeRelativeURI = URI.create("knime://knime.node/data/test.txt");
		provider.newFileSystem(mountPointRelativeURI, env);
		provider.newFileSystem(nodeRelativeURI, env);
	}
	
	@Test(expected = FileSystemAlreadyExistsException.class)
	public void nodeAndWorkflowRelativeURIShareFS() throws IOException {
		KNIMEFileSystemProvider provider = new KNIMEFileSystemProvider();
		
		Map<String, ?> env = null;
		URI nodeRelativeURI = URI.create("knime://knime.node/data/test.txt");
		URI workflowRelativeURI = URI.create("knime://knime.workflow/data/test.txt");
		provider.newFileSystem(nodeRelativeURI, env);
		provider.newFileSystem(workflowRelativeURI, env);
	}
	
	@Test
	public void aCreatedFileSystemCanBeRetrieved() throws IOException {
		KNIMEFileSystemProvider provider = new KNIMEFileSystemProvider();
		
		Map<String, ?> env = null;
		URI knimeURI = URI.create("knime://knime.workflow/data/test.txt");
		FileSystem fileSystem = provider.newFileSystem(knimeURI, env);
		
		assertEquals(fileSystem, provider.getFileSystem(knimeURI));
	}
	
	@Test
	public void path() throws IOException {
		@SuppressWarnings("unused") // workflowURI present to illustrate the big picture
		URI workflowURI = URI.create("file:///C:/my-workflows/workflow");
		URI dataURI = URI.create("file:///C:/my-data/data.txt");
		URI workflowRelativeDataURI = URI.create("knime://knime.workflow/../../my-data/data.txt");
		
		KNIMEFileSystemProvider provider = new KNIMEFileSystemProvider();
		
		Map<String, ?> env = null;
		provider.newFileSystem(workflowRelativeDataURI, env);
		
		Path resolvedPath = provider.getPath(workflowRelativeDataURI);
		Path dataPath = Paths.get(dataURI);
//		assertEquals(dataPath, resolvedPath); Fix with equals method in KNIMEPath!
		assertTrue(resolvedPath instanceof KNIMEPath);
	}
	
	/*
	 * TODO TU: more testing on the path!!
	 * 
	 *  What happens when the relative path does not exist?
	 */
	
	@Test
	public void seekableChannel() {
		
		
		
		
		
	}
	
}
