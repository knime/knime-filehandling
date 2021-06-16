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
 *   2021-03-05 (Alexander Bondaletov): created
 */
package org.knime.ext.smb.filehandling.fs;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.ext.smb.filehandling.fs.SmbFSConnectionConfig.ConnectionMode;
import org.knime.filehandling.core.connections.base.BaseFileSystem;

import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.SmbConfig.Builder;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.share.DiskShare;

/**
 * SMB implementation of the {@link FileSystem} interface.
 *
 * @author Alexander Bondaletov
 */
public class SmbFileSystem extends BaseFileSystem<SmbPath> {

    private static final NodeLogger LOG = NodeLogger.getLogger(SmbFileSystem.class);

    /**
     * Character to use as path separator
     */
    public static final String SEPARATOR = "\\";

    private final SMBClient m_client;

    private final DiskShare m_share;

    /**
     * Constructor.
     *
     * @param cacheTTL
     *            The time to live for cached elements in milliseconds.
     * @param config
     *            The file system configuration
     * @param exec
     *            An optional {@link ExecutionMonitor} to be able to cancel Kerberos
     *            authentication. May be null.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    protected SmbFileSystem(final long cacheTTL, final SmbFSConnectionConfig config, final ExecutionMonitor exec)
            throws IOException {
        super(new SmbFileSystemProvider(), //
                cacheTTL, //
                config.getWorkingDirectory(), //
                config.createFSLocationSpec());

        Builder builder = SmbConfig.builder() //
                .withMultiProtocolNegotiate(true) //
                .withDfsEnabled(true) //
                .withTimeout(config.getTimeout().toSeconds(), TimeUnit.SECONDS);

        final AuthenticationContext authContext = AuthenticationContextFactory.create(config, exec);
        final boolean usingKerberos = config.getAuthType() == SmbFSConnectionConfig.KERBEROS_AUTH_TYPE;

        m_client = new SMBClient(builder.build());

        try {
            if (config.getConnectionMode() == ConnectionMode.DOMAIN) {
                final String host = canonicalizeIfNecessary(config.getDomainName(), usingKerberos);
                m_share = (DiskShare) m_client.connect(host) //
                        .authenticate(authContext) //
                        .connectShare(config.getDomainNamespace());
            } else {
                final String host = canonicalizeIfNecessary(config.getFileserverHost(), usingKerberos);
                m_share = (DiskShare) m_client.connect(host, config.getFileserverPort()) //
                        .authenticate(authContext) //
                        .connectShare(config.getFileserverShare());
            }
        } catch (SMBApiException ex) {
            throw SmbUtils.toIOE(ex, SEPARATOR);
        }
    }

    private static final Pattern IPV4_PATTERN = Pattern.compile("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}");

    private static final Pattern IPV6_PATTERN = Pattern.compile("(?:[A-F0-9]{0,4}:){7}[A-F0-9]{0,4}");

    private static String canonicalizeIfNecessary(final String hostOrDomain, final boolean usingKerberos) {

        final String uppercaseHostOrDomain = hostOrDomain.trim().toUpperCase(Locale.US);

        if (!usingKerberos) {
            return uppercaseHostOrDomain;
        }

        try {
            // when mode=FILESERVER, this is meant to resolve host -> host.mydomain.com
            // when mode=DOMAIN, then smbj needs to connect to the domain controller which
            // can be obtained by resolving the domain to a canonical hostname.
            final String canonicalized = InetAddress.getByName(uppercaseHostOrDomain).getCanonicalHostName();

            // if the canonical hostname is an IP address we may be making things worse, so
            // fall back to user-provided value
            if (canonicalized.equals(uppercaseHostOrDomain) //
                    || IPV4_PATTERN.matcher(canonicalized).matches() //
                    || IPV6_PATTERN.matcher(canonicalized).matches()) {
                return uppercaseHostOrDomain;
            } else {
                LOG.debugWithFormat("Making SMB connection to canonicalized host/domain %s (instead of %s)",
                        canonicalized, uppercaseHostOrDomain);
                return canonicalized;
            }

        } catch (UnknownHostException | SecurityException e) { // NOSONAR if canonicalization not possible, then
                                                               // fallback to user-provided value
            return uppercaseHostOrDomain;
        }
    }

    /**
     * @return The share client
     */
    public DiskShare getClient() {
        return m_share;
    }

    @Override
    protected void prepareClose() throws IOException {
        m_client.close();
    }

    @Override
    public SmbPath getPath(final String first, final String... more) {
        return new SmbPath(this, first, more);
    }

    @Override
    public String getSeparator() {
        return SmbFileSystem.SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singletonList(getPath(SmbFileSystem.SEPARATOR));
    }
}
