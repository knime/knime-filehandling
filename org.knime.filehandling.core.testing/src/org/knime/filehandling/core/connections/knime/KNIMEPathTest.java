package org.knime.filehandling.core.connections.knime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.knime.filehandling.core.connections.base.UnixStylePathUtil;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection;

public class KNIMEPathTest {
	
	private KNIMEFileSystemProvider m_fsProvider;
	private KNIMEFileSystem m_fileSystem;
	private String m_fsBaseLocation;
	private KNIMEConnection.Type m_knimeUrlType;
	private URI m_key;
	
	@Before
	public void setup() {
		m_fsProvider = KNIMEFileSystemProvider.getInstance();
		m_fsBaseLocation = "C:/";
		m_knimeUrlType = KNIMEConnection.Type.WORKFLOW_RELATIVE;
		m_key = URI.create("knime://knime.workflow/dummy/key/uri");
		m_fileSystem = new KNIMEFileSystem(m_fsProvider, m_fsBaseLocation, m_knimeUrlType, m_key);
	}
	
	@Test
	public void aPathsFileSystemCanBeRetrieved() throws MalformedURLException {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, "../../my-data/data.txt");
		assertTrue(knimePath.getFileSystem() == m_fileSystem);
	}
	
	@Test
	public void anNIMEPathIsAbsolute() throws MalformedURLException, URISyntaxException {
		KNIMEPath absoluteKnimePath = new KNIMEPath(m_fileSystem, "C:/my-data/data.txt");
		assertTrue(absoluteKnimePath.isAbsolute());
	}
	
	@Test
	public void aKNIMEPathHasNoRoot() throws MalformedURLException {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, "../../my-data/data.txt");

		assertEquals(null, knimePath.getRoot());
	}
	
	@Test
	public void aPathsFileNameCanBeRetreived() throws Exception {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, "../../my-data/data.txt");
		
		Path fileName = new KNIMEPath(m_fileSystem, "data.txt");
		assertTrue(knimePath.getFileName().equals(fileName));
		assertTrue(knimePath.getFileName() instanceof KNIMEPath);
	}
	
	@Test
	public void anyComponentNameCanBeRetrievedByItsIndex() throws Exception {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, "../../my-data/data.txt");
		
		Path firstComponent = new KNIMEPath(m_fileSystem, "..");
		Path secondComponent = new KNIMEPath(m_fileSystem, "..");
		Path thirdComponent = new KNIMEPath(m_fileSystem, "my-data");
		Path fileName = new KNIMEPath(m_fileSystem, "data.txt");
		
		assertEquals(firstComponent, knimePath.getName(0));
		assertEquals(secondComponent, knimePath.getName(1));
		assertEquals(thirdComponent, knimePath.getName(2));
		assertEquals(fileName, knimePath.getName(3));
		
		assertTrue(knimePath.getName(0) instanceof KNIMEPath);
		assertTrue(knimePath.getName(1) instanceof KNIMEPath);
		assertTrue(knimePath.getName(2) instanceof KNIMEPath);
		assertTrue(knimePath.getName(3) instanceof KNIMEPath);
	}
	
	@Test
	public void pathComponentCount() {
		KNIMEPath path = new KNIMEPath(m_fileSystem, "my-folder/hello/world/data.txt");
		assertEquals(4, path.getNameCount());

		KNIMEPath root = new KNIMEPath(m_fileSystem, "");
		assertEquals(0, root.getNameCount());
	}
	
	@Test
	public void aRootPathDoesNotHaveAParent() throws Exception {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, "");
		assertEquals(null, knimePath.getParent());
	}
	
	@Test
	public void aParentContainsAllComponentsBeforeTheFileName() {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, "my-folder/hello/world/data.txt");
		KNIMEPath parent = new KNIMEPath(m_fileSystem, "my-folder/hello/world/");
		assertEquals(parent, knimePath.getParent());
		assertTrue(knimePath instanceof KNIMEPath);
	}
	
	@Test
	public void aSubpathCanBeCreated() {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, "my-folder/hello/world/data.txt");
		KNIMEPath subpath = new KNIMEPath(m_fileSystem, "hello/world/");
		assertEquals(subpath, knimePath.subpath(1, 3));
		assertTrue(knimePath instanceof KNIMEPath);
	}
	
	@Test
	public void aSubpathEqualToTheWholePath() {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, "my-folder/hello/world/data.txt");
		KNIMEPath subpath = new KNIMEPath(m_fileSystem, "my-folder/hello/world/data.txt");
		assertEquals(subpath, knimePath.subpath(0, 4));
		assertTrue(knimePath instanceof KNIMEPath);
	}
	
	@Test
	public void aSubpathEqualToASingleElement() {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, "/my-folder/hello/world/data.txt");
		KNIMEPath subpath = new KNIMEPath(m_fileSystem, "data.txt");
		assertEquals(subpath, knimePath.subpath(3, 4));
		assertTrue(knimePath instanceof KNIMEPath);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void aSubpathStartingBelowTheRangeThrowsException() {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, "my-folder/hello/world/data.txt");

		knimePath.subpath(-1, 4);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void aSubpathStartingAboveTheRangeThrowsException() {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, "my-folder/hello/world/data.txt");
		
		knimePath.subpath(0, 5);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void aSubpathsEndIndexMustBeGreaterThanBeginIndex() {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, "my-folder/hello/world/data.txt");
		
		knimePath.subpath(2, 2);
	}
	
	@Test
	public void aPathStartsWithItself() {
		String stringPath = "my-folder/hello/world/data.txt";
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, stringPath);
		
		assertTrue(knimePath.startsWith(knimePath));
		assertTrue(knimePath.startsWith(stringPath));
	}
	
	@Test
	public void aPathStartsWithItsFirstComponent() {
		String stringPath = "my-folder/hello/world/data.txt";
		String stringRoot = "my-folder";
		KNIMEPath path = new KNIMEPath(m_fileSystem, stringPath);
		KNIMEPath root = new KNIMEPath(m_fileSystem, stringRoot);
		
		assertTrue(path.startsWith(root));
		assertTrue(path.startsWith(stringRoot));
	}
	
	@Test
	public void aPathStartsWithASubpathFromIndexZero() {
		String stringPath = "my-folder/hello/world/data.txt";
		String stringSubPath = "my-folder/hello";
		KNIMEPath path = new KNIMEPath(m_fileSystem, stringPath);
		KNIMEPath subPath = new KNIMEPath(m_fileSystem, stringSubPath);
		
		assertTrue(path.startsWith(subPath));
		assertTrue(path.startsWith(stringSubPath));
	}
	
	@Test
	public void aPathDoesNotStartWithAPathWithDifferentCompononents() {
		String stringPath = "my-folder/hello/world/data.txt";
		String otherStringPath = "other-folder/hello";
		KNIMEPath path = new KNIMEPath(m_fileSystem, stringPath);
		KNIMEPath otherPath = new KNIMEPath(m_fileSystem, otherStringPath);
		
		assertFalse(path.startsWith(otherPath));
		assertFalse(path.startsWith(otherStringPath));
	}
	
	@Test
	public void aPathDoesNotStartWithAPathFromADifferentFileSystem() {
		String stringPath = "my-folder/hello/world/data.txt";
		KNIMEPath path = new KNIMEPath(m_fileSystem, stringPath);
		KNIMEFileSystem otherFileSystem = new KNIMEFileSystem(null, null, null, null);
		KNIMEPath pathOnOtherFileSystem = new KNIMEPath(otherFileSystem, stringPath);
		
		assertFalse(path.startsWith(pathOnOtherFileSystem));
	}

	@Test
	public void aPathEndsWithItself() {
		String stringPath = "my-folder/hello/world/data.txt";
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, stringPath);
		
		assertTrue(knimePath.endsWith(knimePath));
		assertTrue(knimePath.endsWith(stringPath));
	}
	
	@Test
	public void aPathEndsWithItsFileName() {
		String stringPath = "my-folder/hello/world/data.txt";
		String stringFileName = "data.txt";
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, stringPath);
		KNIMEPath fileName = new KNIMEPath(m_fileSystem, stringFileName);
		
		assertTrue(knimePath.endsWith(fileName));
		assertTrue(knimePath.endsWith(stringFileName));
	}
	
	@Test
	public void aPathEndsWithItsLastComponents() {
		String stringPath = "my-folder/hello/world/data.txt";
		String stringLastComponents = "hello/world/data.txt";
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, stringPath);
		KNIMEPath lastComponents = new KNIMEPath(m_fileSystem, stringLastComponents);
		
		assertTrue(knimePath.endsWith(lastComponents));
		assertTrue(knimePath.endsWith(stringLastComponents));
	}
	
	@Test
	public void aPathDoesNotEndWithADifferentFileName() {
		String stringPath = "my-folder/hello/world/data.txt";
		String stringDifferentFileName = "world/other-data.txt";
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, stringPath);
		KNIMEPath differentFileNamePath = new KNIMEPath(m_fileSystem, stringDifferentFileName);
		
		assertFalse(knimePath.endsWith(differentFileNamePath));
		assertFalse(knimePath.endsWith(stringDifferentFileName));
	}
	
	@Test
	public void aPathDoesNotEndWithAPathFromADifferentFileSystem() {
		String stringPath = "my-folder/hello/world/data.txt";
		KNIMEPath path = new KNIMEPath(m_fileSystem, stringPath);
		KNIMEFileSystem otherFileSystem = new KNIMEFileSystem(null, null, null, null);
		KNIMEPath pathOnOtherFileSystem = new KNIMEPath(otherFileSystem, stringPath);
		
		assertFalse(path.endsWith(pathOnOtherFileSystem));
	}
	
	@Test
	public void normalize() {
		String stringPath = "my-folder/hello/../hello/world/data.txt";
		String stringNormalizedPath = "my-folder/hello/world/data.txt";
		KNIMEPath path = new KNIMEPath(m_fileSystem, stringPath);
		KNIMEPath normalizedPath = new KNIMEPath(m_fileSystem, stringNormalizedPath);
		
		assertEquals(normalizedPath, path.normalize());
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void pathsOnDifferentFileSystemCannotBeResolvedAgainstEachOther() {
		String stringPath = "my-folder/hello/world/data.txt";
		KNIMEPath path = new KNIMEPath(m_fileSystem, stringPath);
		KNIMEPath pathOnDifferentFS = new KNIMEPath(new KNIMEFileSystem(null, null, null, null), stringPath);
		
		path.resolve(pathOnDifferentFS);
	}
	
	@Test
	public void resolvingOtherPathAppendsIt() {
		String firstStringPath = "my-folder/hello/";
		KNIMEPath path = new KNIMEPath(m_fileSystem, firstStringPath);
		String otherStringPath = "world/data.txt";
		KNIMEPath otherPath = new KNIMEPath(m_fileSystem, otherStringPath);
		
		KNIMEPath expectedPath = new KNIMEPath(m_fileSystem, "my-folder/hello/world/data.txt");

		assertEquals(expectedPath, path.resolve(otherPath));
		assertEquals(expectedPath, path.resolve(otherStringPath));
	}
	
	@Test
	public void resolvingSibling() {
		String firstStringPath = "my-folder/some-data.csv";
		KNIMEPath path = new KNIMEPath(m_fileSystem, firstStringPath);
		String otherStringPath = "other-folder/data.txt";
		KNIMEPath otherPath = new KNIMEPath(m_fileSystem, otherStringPath);
		
		KNIMEPath expectedPath = new KNIMEPath(m_fileSystem, "my-folder/other-folder/data.txt");
		
		assertEquals(expectedPath, path.resolveSibling(otherPath));
		assertEquals(expectedPath, path.resolveSibling(otherStringPath));
	}
	
	@Test
	public void relativize() {
		KNIMEPath path = new KNIMEPath(m_fileSystem, "my-folder/");
		KNIMEPath otherPath = new KNIMEPath(m_fileSystem, "my-folder/other-folder/data.txt");
		
		KNIMEPath expectedPath = new KNIMEPath(m_fileSystem, "other-folder/data.txt");
		assertEquals(expectedPath, path.relativize(otherPath));
	}
	
	@Test
	public void urisAreCreatedWithTheFileSystemsConnectionType() {
		KNIMEFileSystem workflowRelativeFS = createFileSystemOfType(KNIMEConnection.Type.WORKFLOW_RELATIVE);
		KNIMEPath path = new KNIMEPath(workflowRelativeFS, "my-folder/");
		URI expectedURI = URI.create("knime://knime.workflow/my-folder");
		assertEquals(expectedURI, path.toUri());
		
		KNIMEFileSystem nodeRelativeFS = createFileSystemOfType(KNIMEConnection.Type.NODE_RELATIVE);
		path = new KNIMEPath(nodeRelativeFS, "my-folder/");
		expectedURI = URI.create("knime://knime.node/my-folder");
		assertEquals(expectedURI, path.toUri());
		
		KNIMEFileSystem mountPointRelativeFS = createFileSystemOfType(KNIMEConnection.Type.MOUNTPOINT_RELATIVE);
		path = new KNIMEPath(mountPointRelativeFS, "my-folder/");
		expectedURI = URI.create("knime://knime.mountpoint/my-folder");
		assertEquals(expectedURI, path.toUri());
	}
	
	@Test
	public void theAboslutePathIsResolvedAgainstTheFileSystemsBaseLocation() {
		KNIMEPath path = new KNIMEPath(m_fileSystem, "my-folder/");
		String expected = "C:/my-folder";
		assertEquals(expected, path.toAbsolutePath().toString());
	}
	
	@Test
	public void toRealPathReturnesAbsoluteAndNormalizedPath() throws IOException {
		String stringPath = "my-folder/hello/../hello/world/data.txt";
		KNIMEPath path = new KNIMEPath(m_fileSystem, stringPath);
		
		KNIMEPath expected = new KNIMEPath(m_fileSystem, "C:/my-folder/hello/world/data.txt");
		
		assertEquals(expected, path.toRealPath());
	}
	
	@Test
	public void toFileIsUnsupported() {
		KNIMEPath path = new KNIMEPath(m_fileSystem, "my-folder/hello/world/data.txt");
		File file = path.toFile();
		
		String unixStylePath = UnixStylePathUtil.asUnixStylePath(file.getPath());
		assertEquals("my-folder/hello/world/data.txt", unixStylePath);
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void watchKeysAreUnsupported() throws IOException {
		KNIMEPath path = new KNIMEPath(m_fileSystem, "my-folder/hello/world/data.txt");
		path.register(null);
	}
	
	@Test
	public void moreContainsNameSeperatedPath() {
		String first = "first";
		String second = "second/folder";
		String lastWindowsStyle = "windows\\data.txt";		
		
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, first, second, lastWindowsStyle);
		
		assertEquals(5, knimePath.getNameCount());
		assertEquals("first", knimePath.getName(0).toString());
		assertEquals("second", knimePath.getName(1).toString());
		assertEquals("folder", knimePath.getName(2).toString());
		assertEquals("windows", knimePath.getName(3).toString());
		assertEquals("data.txt", knimePath.getName(4).toString());
	}
	
	// TODO TU: make linux compatible :) 
	
	@Test
	public void absoluteLocalPath() {
		KNIMEPath absoluteLocalPath = new KNIMEPath(m_fileSystem, "C:/my-folder/hello/world/data.txt");
		
		Path localPath = absoluteLocalPath.toLocalPath();
		Path expected = Paths.get("C:/my-folder/hello/world/data.txt");
		
		assertFalse(localPath.equals(absoluteLocalPath));
		assertEquals(expected, localPath);
	}
	
	@Test
	public void relativeLocalPath() {
		KNIMEPath absoluteLocalPath = new KNIMEPath(m_fileSystem, "my-folder/hello/world/data.txt");
		
		Path localPath = absoluteLocalPath.toLocalPath();
		Path expected = Paths.get("C:/my-folder/hello/world/data.txt");
		
		assertFalse(localPath.equals(absoluteLocalPath));
		assertEquals(expected, localPath);
	}
	
	private KNIMEFileSystem createFileSystemOfType(KNIMEConnection.Type type) {
		return new KNIMEFileSystem(m_fsProvider, m_fsBaseLocation, type, m_key);
	}
	
}
