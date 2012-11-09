/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 * 
 * History
 *   Nov 9, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remotecredentials;

/**
 * 
 * @author Patrick Winter, University of Konstanz
 */
enum Protocol {

    /** SSH protocol. */
    SSH("SSH", 22, true, true),

    /** FTP protocol. */
    FTP("FTP", 21, false, false),

    /** HTTP protocol. */
    HTTP("HTTP", 80, false, false),

    /** HTTPS protocol. */
    HTTPS("HTTPS", 443, false, true);

    private String m_name;

    private int m_port;

    private boolean m_keyfilesupport;

    private boolean m_certificatesupport;

    private Protocol(final String name, final int port,
            final boolean keyfileSupport, final boolean certificateSupport) {
        m_name = name;
        m_port = port;
        m_keyfilesupport = keyfileSupport;
        m_certificatesupport = certificateSupport;
    }

    /**
     * @return the name
     */
    String getName() {
        return m_name;
    }

    /**
     * @return the port
     */
    int getPort() {
        return m_port;
    }

    /**
     * @return If this protocol has support for key files
     */
    boolean hasKeyfileSupport() {
        return m_keyfilesupport;
    }

    /**
     * @return If this protocol has support for certificates
     */
    boolean hasCertificateSupport() {
        return m_certificatesupport;
    }

    /**
     * Get the correspondent protocol to the name.
     * 
     * 
     * @param protocolName The name of the protocol
     * @return Protocol to the name
     */
    static Protocol getProtocol(final String protocolName) {
        Protocol protocol = null;
        if (protocolName.equals(SSH.getName())) {
            protocol = SSH;
        } else if (protocolName.equals(FTP.getName())) {
            protocol = FTP;
        } else if (protocolName.equals(HTTP.getName())) {
            protocol = HTTP;
        } else if (protocolName.equals(HTTPS.getName())) {
            protocol = HTTPS;
        }
        return protocol;
    }

    /**
     * @return Array with all protocol names
     */
    static String[] getAllProtocols() {
        return new String[]{SSH.getName(), FTP.getName(), HTTP.getName(),
                HTTPS.getName()};
    }

}
