package org.knime.filehandling.core.connections.knime;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MockKNIMEUrlHandler implements KNIMEUrlHandler {
	
	private Map<URI, URI> m_mockedResolvedURIs = new HashMap<>();
	private Map<URL, URL> m_mockedResolvedURLs = new HashMap<>();

	public void mockResolve(URI knimeRelativeURI, URI resolvedURI) {
		m_mockedResolvedURIs.put(knimeRelativeURI, resolvedURI);
	}
	
	public void mockResolve(URL knimeRelativeURL, URL resolvedURL) {
		m_mockedResolvedURLs.put(knimeRelativeURL, resolvedURL);
	}
	
	@Override
	public URI resolveKNIMEURI(URI uri) {
		return m_mockedResolvedURIs.get(uri);
	}
	
	@Override
	public URL resolveKNIMEURL(URL url) {
		return m_mockedResolvedURLs.get(url);
	}
	
}
