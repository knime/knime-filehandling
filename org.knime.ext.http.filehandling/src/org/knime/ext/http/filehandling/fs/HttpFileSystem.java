/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   2020-11-18 (Bjoern Lohrmann): created
 */
package org.knime.ext.http.filehandling.fs;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.base.BaseFileStore;

/**
 * HTTP implementation of the {@link FileSystem}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class HttpFileSystem extends FSFileSystem<HttpPath> {

    /**
     * HTTP URI scheme.
     */
    public static final String FS_TYPE = "http";

    /**
     * Character to use as path separator
     */
    public static final String PATH_SEPARATOR = "/";

    private final HttpConnectionConfig m_config;

    private final HttpFileSystemProvider m_provider;

    private final HttpClient m_client;

    /**
     * @param cfg
     *            HTTP connection config.
     * @throws IOException
     */
    protected HttpFileSystem(final HttpConnectionConfig cfg) throws IOException {
        super(createUri(cfg), //
                createFSLocationSpec(cfg.getUrl()), //
                determineWorkingDirectory(cfg));
        m_config = cfg;
        m_provider = new HttpFileSystemProvider();
        m_provider.setFileSystem(this); // NOSONAR
        m_client = HttpClient.create(cfg);
    }

    @Override
    public HttpFileSystemProvider provider() {
        return m_provider;
    }

    HttpClient getClient() {
        return m_client;
    }

    /**
     * @param cfg
     *            connection configuration.
     * @return URI from configuration.
     * @throws URISyntaxException
     */
    private static URI createUri(final HttpConnectionConfig cfg) {
        final URI parsedUrl = URI.create(cfg.getUrl());
        return URI.create(String.format("%s://%s", FS_TYPE, parsedUrl.getHost()));
    }

    /**
     * @param url
     *            The base URL of the connection.
     * @return the {@link FSLocationSpec} for an HTTP file system.
     */
    public static DefaultFSLocationSpec createFSLocationSpec(final String url) {
        String resolvedHost = URI.create(url).getHost().toLowerCase(Locale.ENGLISH);
        try {
            resolvedHost = InetAddress.getByName(resolvedHost).getCanonicalHostName();
        } catch (UnknownHostException ex) { // NOSONAR is possible if host can't be resolved
        }

        return new DefaultFSLocationSpec(FSCategory.CONNECTED, HttpFileSystem.FS_TYPE + ":" + resolvedHost);
    }

    private static String determineWorkingDirectory(final HttpConnectionConfig cfg) {
        final URI url = URI.create(cfg.getUrl());
        return StringUtils.isEmpty(url.getPath()) ? HttpFileSystem.PATH_SEPARATOR : url.getRawPath();
    }

    @Override
    public HttpPath getPath(final String first, final String... more) {
        return new HttpPath(this, first, more);
    }

    @Override
    public String getSeparator() {
        return PATH_SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singletonList(getPath(PATH_SEPARATOR));
    }

    @Override
    protected void ensureClosedInternal() throws IOException {
        m_client.close();
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return Collections.singletonList(new BaseFileStore(getFileSystemBaseURI().getScheme(), "default_file_store"));
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        final Set<String> supportedViews = new HashSet<>();
        supportedViews.add("basic");
        return supportedViews;
    }

    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }

    String getBaseUrl() {
        return m_config.getUrl();
    }
}
