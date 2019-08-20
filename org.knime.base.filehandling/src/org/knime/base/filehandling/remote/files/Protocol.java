/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Nov 9, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.files;

/**
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @since 2.11
 */
public class Protocol {

//    /** SSH protocol. */
//    SSH("ssh", 22, false, true, true, true, true, true),
//
//    /** FTP protocol. */
//    FTP("ftp", 21, true, false, false, true, true, false),
//
//    /** HTTP protocol. */
//    HTTP("http", 80, true, false, false, false, false, true),
//
//    /** HTTPS protocol. */
//    HTTPS("https", 443, true, false, false, false, false, true);

    private final String m_name;

    private final int m_port;

    private final boolean m_authNoneSupport;

    private final boolean m_keyFileSupport;

    private final boolean m_knownHostsSupport;

    private final boolean m_testSupport;

    private final boolean m_browseSupport;

    private final boolean m_userTimeoutSupport;

    private final boolean m_passwordSupport;

    private final boolean m_kerberosSupport;

    private final boolean m_tokenSupport;

    /**
     * Create a protocol.
     *
     *
     * @param name The name
     * @param port The default port
     * @param authNoneSupport If the authentication method none is supported
     * @param keyfileSupport If authentication via keyfile is supported
     * @param knownhostsSupport If use of known hosts is supported
     * @param testSupport If the testing of the connection is supported
     * @param browseSupport If this protocol supports browsing
     * @param userTimeoutSupport <code>true</code> if the user can change the connection's timeout, <code>false</code>
     *            otherwise
     * @param passwordSupport <code>true</code> if the protocol supports password based authentication
     * @param kerberorsSupport <code>true</code> if the protocol supports Kerberos based authentication
     */
    public Protocol(final String name, final int port, final boolean authNoneSupport, final boolean keyfileSupport,
            final boolean knownhostsSupport, final boolean testSupport, final boolean browseSupport,
            final boolean userTimeoutSupport, final boolean passwordSupport, final boolean kerberorsSupport) {
        this(name, port, authNoneSupport, keyfileSupport, knownhostsSupport, testSupport, browseSupport,
            userTimeoutSupport, passwordSupport, kerberorsSupport, false);
    }

    /**
     * Create a protocol.
     *
     *
     * @param name The name
     * @param port The default port
     * @param authNoneSupport If the authentication method none is supported
     * @param keyfileSupport If authentication via keyfile is supported
     * @param knownhostsSupport If use of known hosts is supported
     * @param testSupport If the testing of the connection is supported
     * @param browseSupport If this protocol supports browsing
     * @param userTimeoutSupport <code>true</code> if the user can change the connection's timeout, <code>false</code>
     *            otherwise
     * @param passwordSupport <code>true</code> if the protocol supports password based authentication
     * @param kerberorsSupport <code>true</code> if the protocol supports Kerberos based authentication
     * @param tokenSupport <code>true</code> if the protocol support token based authentication
     * @since 4.1
     */
    public Protocol(final String name, final int port, final boolean authNoneSupport, final boolean keyfileSupport,
                     final boolean knownhostsSupport, final boolean testSupport, final boolean browseSupport,
                     final boolean userTimeoutSupport, final boolean passwordSupport, final boolean kerberorsSupport,
                     final boolean tokenSupport) {
        m_name = name.toLowerCase();
        m_port = port;
        m_authNoneSupport = authNoneSupport;
        m_keyFileSupport = keyfileSupport;
        m_knownHostsSupport = knownhostsSupport;
        m_testSupport = testSupport;
        m_browseSupport = browseSupport;
        m_userTimeoutSupport = userTimeoutSupport;
        m_passwordSupport = passwordSupport;
        m_kerberosSupport = kerberorsSupport;
        m_tokenSupport = tokenSupport;
    }

    /**
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return m_port;
    }

    /**
     * @return If this protocol supports the authentication method none
     */
    public boolean hasAuthNoneSupport() {
        return m_authNoneSupport;
    }

    /**
     * @return If this protocol has support for key files
     */
    public boolean hasKeyfileSupport() {
        return m_keyFileSupport;
    }

    /**
     * @return If this protocol has support for known hosts
     */
    public boolean hasKnownhostsSupport() {
        return m_knownHostsSupport;
    }

    /**
     * @return If this protocol has support for testing the connection
     */
    public boolean hasTestSupport() {
        return m_testSupport;
    }

    /**
     * @return If this protocol supports browsing
     */
    public boolean hasBrowseSupport() {
        return m_browseSupport;
    }

    /**
     * Returns whether the user can adjust the connection timeout or not.
     *
     * @return <code>true</code> if the connection timeout can be adjusted, <code>false</code> otherwise
     */
    public boolean hasUserDefinedTimeoutSupport() {
        return m_userTimeoutSupport;
    }

    /**
     * @return <code>true</code> if the protocol supports password authentication
     */
    public boolean hasPasswordSupport() {
        return m_passwordSupport;
    }

    /**
     * @return <code>true</code> if the protocol supports Kerberos based authentication
     */
    public boolean hasKerberosSupport() {
        return m_kerberosSupport;
    }

    /**
     * @return <code>true</code> if the protocol supports token based authentication
     * @since 4.1
     */
    public boolean hasTokenSupport() {
        return m_tokenSupport;
    }
}
