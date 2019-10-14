package org.knime.filehandling.core.connections.knime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.spi.FileSystemProvider;

import org.junit.Test;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection;

public class KNIMEFileSystemProviderTest {
	
	@Test
	public void createFileSystemWithURI() throws IOException {
		FileSystemProvider provider = new KNIMEFileSystemProvider();
		
		URI fsURI = URI.create("knime://knime.workflow/C:/path/to/base");
		FileSystem createdFS = provider.newFileSystem(fsURI, null);
		
		assertTrue(createdFS instanceof KNIMEFileSystem);
		KNIMEFileSystem knimeFS = (KNIMEFileSystem) createdFS;
		assertEquals("C:/path/to/base", knimeFS.getBase());
		assertEquals(KNIMEConnection.Type.WORKFLOW_RELATIVE, knimeFS.getKNIMEConnectionType());
	}
	
	@Test(expected = FileSystemAlreadyExistsException.class)
	public void tryingToRecreateAlreadyExistingFileSystemThrowsException() throws IOException {
		FileSystemProvider provider = new KNIMEFileSystemProvider();
		
		URI fsURI = URI.create("knime://knime.workflow/C:/path/to/base");
		provider.newFileSystem(fsURI, null);
		provider.newFileSystem(fsURI, null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void aFileSystemMustBeCreatedFromAUriWithAbsolutePath() throws Exception {
		FileSystemProvider provider = new KNIMEFileSystemProvider();
		
		URI uri = URI.create("knime://knime.workflow/../path/to/somewhere");
		
		provider.newFileSystem(uri, null);
	}
	
}
