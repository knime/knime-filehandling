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
 *   2020-10-06 (soldatov): created
 */
package org.knime.ext.ftp.filehandling.fs;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 * Supported FTP client features.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public final class FtpClientFeatures {
    /**
     * MLST command is used for request the file attributes by command channel.
     */
    private boolean m_mListSupported;
    /**
     * MDTM command is used for request the file last modified time.
     */
    private boolean m_mDtmSupported;

    private FtpClientFeatures() {
    }

    /**
     * @param client
     *            FTP client.
     * @return features supported by given client.
     * @throws IOException
     */
    public static FtpClientFeatures autodetect(final FTPClient client) throws IOException {
        final FtpClientFeatures features = new FtpClientFeatures();
        // request supported features
        client.feat();

        if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
            // not positive response, possible FEAT command is not supported
            return features;
        }

        for (String feature : client.getReplyStrings()) {
            final String upperCaseFeature = feature.toUpperCase();
            if (upperCaseFeature.contains("MLST")) {
                features.m_mListSupported = true;
            } else if (upperCaseFeature.contains("MDTM")) {
                features.m_mDtmSupported = true;
            } else {
                // possible check other features
            }
        }
        return features;
    }

    /**
     * @return true if MLST command supported by server.
     */
    public boolean ismListSupported() {
        return m_mListSupported;
    }

    /**
     * @return true if MDTM command supported by server
     */
    public boolean ismDtmSupported() {
        return m_mDtmSupported;
    }
}
