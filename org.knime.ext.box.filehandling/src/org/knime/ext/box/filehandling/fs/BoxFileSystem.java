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
 *   2023-02-14 (Alexander Bondaletov): created
 */
package org.knime.ext.box.filehandling.fs;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collections;

import org.knime.filehandling.core.connections.base.BaseFileSystem;
import org.knime.okhttp3.OkHttpProxyAuthenticator;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFolder;

import okhttp3.OkHttpClient.Builder;

/**
 * The Box {@link FileSystem}.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
public class BoxFileSystem extends BaseFileSystem<BoxPath> {

    /**
     * Character to use as path separator
     */
    public static final String SEPARATOR = "/";

    private final BoxAPIConnection m_api;

    /**
     * @param cacheTTL
     *            The time to live for cached elements in milliseconds.
     * @param config
     *            The file system configuration
     * @throws IOException
     */
    protected BoxFileSystem(final long cacheTTL, final BoxFSConnectionConfig config) throws IOException {
        super(new BoxFileSystemProvider(), cacheTTL, config.getWorkingDirectory(),
                BoxFSConnectionConfig.createFSLocationSpec());
        m_api = new BoxAPIConnection(config.getDeveloperToken()) {
            @Override
            protected Builder modifyHttpClientBuilder(final Builder httpClientBuilder) {
                httpClientBuilder.proxyAuthenticator(new OkHttpProxyAuthenticator());
                return super.modifyHttpClientBuilder(httpClientBuilder);
            }
        };
        m_api.setConnectTimeout(Math.toIntExact(config.getConnectionTimeout().toMillis()));
        m_api.setReadTimeout(Math.toIntExact(config.getReadTimeout().toMillis()));

        try {
            BoxFolder.getRootFolder(m_api).iterator().hasNext(); // NOSONAR just checking if we get an exception
        } catch (BoxAPIException e) {
            throw BoxUtils.toIOE(e, SEPARATOR);
        }
    }

    /**
     * @return the api
     */
    public BoxAPIConnection getApi() {
        return m_api;
    }

    @Override
    protected void prepareClose() throws IOException {
        // nothing to do
    }

    @Override
    public BoxPath getPath(final String first, final String... more) {
        return new BoxPath(this, first, more);
    }

    @Override
    public String getSeparator() {
        return SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singletonList(getPath(SEPARATOR));
    }
}
