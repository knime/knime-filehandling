package org.knime.filehandling.core.connections.knime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.util.FileUtil;

public class KNIMEPathTest {
	
	private MockKNIMEUrlHandler m_mockUrlHandler;
	private KNIMEFileSystem m_fileSystem;
	
	@Before
	public void setup() {
		m_mockUrlHandler = new MockKNIMEUrlHandler();
		m_fileSystem = new KNIMEFileSystem(null);
	}
	
	@Test
	public void aPathsFileSystemCanBeRetrieved() throws MalformedURLException {
		@SuppressWarnings("unused") // workflowURI present to illustrate the big picture
		URI workflowURI = URI.create("file:///C:/my-workflows/workflow");
		URL dataURL = URI.create("file:///C:/my-data/data.txt").toURL();
		String workflowRelativePath = "knime://knime.workflow/../../my-data/data.txt";
		URL workflowRelativeURL = FileUtil.toURL(workflowRelativePath);
		
		m_mockUrlHandler.mockResolve(workflowRelativeURL, dataURL);
		
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, workflowRelativeURL, m_mockUrlHandler);
		
		assertTrue(knimePath.getFileSystem() == m_fileSystem);
	}
	
	@Test
	public void aResolvedKNIMEUrlIsAbsolute() throws MalformedURLException, URISyntaxException {
		@SuppressWarnings("unused") // workflowURI present to illustrate the big picture
		URI workflowURI = URI.create("file:///C:/my-workflows/workflow");
		URL dataURL = URI.create("file:///C:/my-data/data.txt").toURL();
		String workflowRelativePath = "knime://knime.workflow/../../my-data/data.txt";
		URL workflowRelativeURL = FileUtil.toURL(workflowRelativePath);

		m_mockUrlHandler.mockResolve(workflowRelativeURL, dataURL);
		
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, workflowRelativeURL, m_mockUrlHandler);
		
		assertTrue(knimePath.isAbsolute());
	}
	
	@Test
	public void anAbsoluteNonKNIMEUrlIsAbsolute() throws MalformedURLException {
		URL dataURL = URI.create("file:///C:/my-data/data.txt").toURL();
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, dataURL, m_mockUrlHandler);
		assertTrue(knimePath.isAbsolute());
	}
	
	@Test
	public void aResolvedKNIMEUrlHasARoot() throws MalformedURLException {
		@SuppressWarnings("unused") // workflowURI present to illustrate the big picture
		URI workflowURI = URI.create("file:///C:/my-workflows/workflow");
		URL dataURL = URI.create("file:///C:/my-data/data.txt").toURL();
		String workflowRelativePath = "knime://knime.workflow/../../my-data/data.txt";
		URL workflowRelativeURL = FileUtil.toURL(workflowRelativePath);
		
		m_mockUrlHandler.mockResolve(workflowRelativeURL, dataURL);

		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, workflowRelativeURL, m_mockUrlHandler);
		
		Path root = new KNIMEPath(m_fileSystem, Paths.get("C:/"));

		assertTrue(knimePath.getRoot().equals(root));
		assertTrue(knimePath.getRoot() instanceof KNIMEPath);
	}
	
	@Test
	public void aPathsFileNameCanBeRetreived() throws Exception {
		@SuppressWarnings("unused") // workflowURI present to illustrate the big picture
		URI workflowURI = URI.create("file:///C:/my-workflows/workflow");
		URL dataURL = URI.create("file:///C:/my-data/data.txt").toURL();
		String workflowRelativePath = "knime://knime.workflow/../../my-data/data.txt";
		URL workflowRelativeURL = FileUtil.toURL(workflowRelativePath);
		
		m_mockUrlHandler.mockResolve(workflowRelativeURL, dataURL);
		
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, workflowRelativeURL, m_mockUrlHandler);
		
		Path fileName = new KNIMEPath(m_fileSystem, Paths.get("data.txt"));
		assertTrue(knimePath.getFileName().equals(fileName));
		assertTrue(knimePath.getFileName() instanceof KNIMEPath);
	}
	
	@Test
	public void anyComponentNameCanBeRetrievedByItsIndex() throws Exception {
		@SuppressWarnings("unused") // workflowURI present to illustrate the big picture
		URI workflowURI = URI.create("file:///C:/my-workflows/workflow");
		URL dataURL = URI.create("file:///C:/my-data/data.txt").toURL();
		String workflowRelativePath = "knime://knime.workflow/../../my-data/data.txt";
		URL workflowRelativeURL = FileUtil.toURL(workflowRelativePath);
		
		m_mockUrlHandler.mockResolve(workflowRelativeURL, dataURL);
		
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, workflowRelativeURL, m_mockUrlHandler);
		
		Path firstComponent = new KNIMEPath(m_fileSystem, Paths.get("my-data"));
		Path fileName = new KNIMEPath(m_fileSystem, Paths.get("data.txt"));
		
		assertEquals(firstComponent, knimePath.getName(0));
		assertEquals(fileName, knimePath.getName(1));
		
		assertTrue(knimePath.getName(0) instanceof KNIMEPath);
		assertTrue(knimePath.getName(1) instanceof KNIMEPath);
	}
	
	@Test
	public void pathComponentCount() {
		String stringPath = "C:/my-folder/hello/world/data.txt";
		KNIMEPath path = new KNIMEPath(m_fileSystem, Paths.get(stringPath));

		assertEquals(4, path.getNameCount());

		String stringRoot = "C:/";
		KNIMEPath root = new KNIMEPath(m_fileSystem, Paths.get(stringRoot));

		assertEquals(0, root.getNameCount());
	}
	
	@Test
	public void aRootPathDoesNotHaveAParent() throws Exception {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, Paths.get("C:/"));
		assertEquals(null, knimePath.getParent());
		assertTrue(knimePath instanceof KNIMEPath);
	}
	
	@Test
	public void aParentContainsAllComponentsAfterTheRootAndBeforeTheFileName() {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, Paths.get("C:/my-folder/hello/world/data.txt"));
		KNIMEPath parent = new KNIMEPath(m_fileSystem, Paths.get("my-folder/hello/world/"));
		assertEquals(parent, knimePath.getParent());
		assertTrue(knimePath instanceof KNIMEPath);
	}
	
	@Test
	public void aSubpathCanBeCreated() {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, Paths.get("C:/my-folder/hello/world/data.txt"));
		KNIMEPath subpath = new KNIMEPath(m_fileSystem, Paths.get("hello/world/"));
		assertEquals(subpath, knimePath.subpath(1, 3));
		assertTrue(knimePath instanceof KNIMEPath);
	}
	
	@Test
	public void aSubpathEqualToTheWholePath() {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, Paths.get("C:/my-folder/hello/world/data.txt"));
		KNIMEPath subpath = new KNIMEPath(m_fileSystem, Paths.get("my-folder/hello/world/data.txt"));
		assertEquals(subpath, knimePath.subpath(0, 4));
		assertTrue(knimePath instanceof KNIMEPath);
	}
	
	@Test
	public void aSubpathEqualToASingleElement() {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, Paths.get("C:/my-folder/hello/world/data.txt"));
		KNIMEPath subpath = new KNIMEPath(m_fileSystem, Paths.get("data.txt"));
		assertEquals(subpath, knimePath.subpath(3, 4));
		assertTrue(knimePath instanceof KNIMEPath);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void aSubpathStartingBelowTheRangeThrowsException() {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, Paths.get("C:/my-folder/hello/world/data.txt"));

		knimePath.subpath(-1, 4);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void aSubpathStartingAboveTheRangeThrowsException() {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, Paths.get("C:/my-folder/hello/world/data.txt"));
		
		knimePath.subpath(0, 5);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void aSubpathsEndIndexMustBeGreaterThanBeginIndex() {
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, Paths.get("C:/my-folder/hello/world/data.txt"));
		
		knimePath.subpath(2, 2);
	}
	
	@Test
	public void aPathStartsWithItself() {
		String stringPath = "C:/my-folder/hello/world/data.txt";
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, Paths.get(stringPath));
		
		assertTrue(knimePath.startsWith(knimePath));
		assertTrue(knimePath.startsWith(stringPath));
	}
	
	@Test
	public void aPathStartsWithItsRoot() {
		String stringPath = "C:/my-folder/hello/world/data.txt";
		String stringRoot = "C:/";
		KNIMEPath path = new KNIMEPath(m_fileSystem, Paths.get(stringPath));
		KNIMEPath root = new KNIMEPath(m_fileSystem, Paths.get(stringRoot));
		
		assertTrue(path.startsWith(root));
		assertTrue(path.startsWith(stringRoot));
	}
	
	@Test
	public void aPathStartsWithASubpathFromIndexZero() {
		String stringPath = "C:/my-folder/hello/world/data.txt";
		String stringSubPath = "C:/my-folder/hello";
		KNIMEPath path = new KNIMEPath(m_fileSystem, Paths.get(stringPath));
		KNIMEPath subPath = new KNIMEPath(m_fileSystem, Paths.get(stringSubPath));
		
		assertTrue(path.startsWith(subPath));
		assertTrue(path.startsWith(stringSubPath));
	}
	
	@Test
	public void aPathDoesNotStartWithAPathWithDifferentCompononents() {
		String stringPath = "C:/my-folder/hello/world/data.txt";
		String otherStringPath = "C:/other-folder/hello";
		KNIMEPath path = new KNIMEPath(m_fileSystem, Paths.get(stringPath));
		KNIMEPath otherPath = new KNIMEPath(m_fileSystem, Paths.get(otherStringPath));
		
		assertFalse(path.startsWith(otherPath));
		assertFalse(path.startsWith(otherStringPath));
	}
	
	@Test
	public void aPathDoesNotStartWithAPathFromADifferentFileSystem() {
		String stringPath = "C:/my-folder/hello/world/data.txt";
		KNIMEPath path = new KNIMEPath(m_fileSystem, Paths.get(stringPath));
		KNIMEFileSystem otherFileSystem = new KNIMEFileSystem(null);
		KNIMEPath pathOnOtherFileSystem = new KNIMEPath(otherFileSystem, Paths.get(stringPath));
		
		assertFalse(path.startsWith(pathOnOtherFileSystem));
	}

	@Test
	public void aPathEndsWithItself() {
		String stringPath = "C:/my-folder/hello/world/data.txt";
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, Paths.get(stringPath));
		
		assertTrue(knimePath.endsWith(knimePath));
		assertTrue(knimePath.endsWith(stringPath));
	}
	
	@Test
	public void aPathEndsWithItsFileName() {
		String stringPath = "C:/my-folder/hello/world/data.txt";
		String stringFileName = "data.txt";
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, Paths.get(stringPath));
		KNIMEPath fileName = new KNIMEPath(m_fileSystem, Paths.get(stringFileName));
		
		assertTrue(knimePath.endsWith(fileName));
		assertTrue(knimePath.endsWith(stringFileName));
	}
	
	@Test
	public void aPathEndsWithItsLastComponents() {
		String stringPath = "C:/my-folder/hello/world/data.txt";
		String stringLastComponents = "hello/world/data.txt";
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, Paths.get(stringPath));
		KNIMEPath lastComponents = new KNIMEPath(m_fileSystem, Paths.get(stringLastComponents));
		
		assertTrue(knimePath.endsWith(lastComponents));
		assertTrue(knimePath.endsWith(stringLastComponents));
	}
	
	@Test
	public void aPathDoesNotEndWithADifferentFileName() {
		String stringPath = "C:/my-folder/hello/world/data.txt";
		String stringDifferentFileName = "world/other-data.txt";
		KNIMEPath knimePath = new KNIMEPath(m_fileSystem, Paths.get(stringPath));
		KNIMEPath differentFileNamePath = new KNIMEPath(m_fileSystem, Paths.get(stringDifferentFileName));
		
		assertFalse(knimePath.endsWith(differentFileNamePath));
		assertFalse(knimePath.endsWith(stringDifferentFileName));
	}
	
	@Test
	public void aPathDoesNotEndWithAPathFromADifferentFileSystem() {
		String stringPath = "C:/my-folder/hello/world/data.txt";
		KNIMEPath path = new KNIMEPath(m_fileSystem, Paths.get(stringPath));
		KNIMEFileSystem otherFileSystem = new KNIMEFileSystem(null);
		KNIMEPath pathOnOtherFileSystem = new KNIMEPath(otherFileSystem, Paths.get(stringPath));
		
		assertFalse(path.endsWith(pathOnOtherFileSystem));
	}
	
	@Test
	public void normalize() {
		String stringPath = "C:/my-folder/hello/../hello/world/data.txt";
		String stringNormalizedPath = "C:/my-folder/hello/world/data.txt";
		KNIMEPath path = new KNIMEPath(m_fileSystem, Paths.get(stringPath));
		KNIMEPath normalizedPath = new KNIMEPath(m_fileSystem, Paths.get(stringNormalizedPath));
		
		assertEquals(normalizedPath, path.normalize());
	}
	
	@Test
	public void resolve() {
		
		
		Path path = Paths.get("C:/my-folder/hello/lol.txt");
		Path path2 = Paths.get("helloooo/blah");

		
		Path resolve = path.resolve(path2);
		
		System.out.println(resolve);
		System.out.println(path.getRoot());
		
		
	}
	
	
}
