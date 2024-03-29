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
 *   2022-04-27 (Dragan Keselj): created
 */
package org.knime.archive.zip.filehandling.fs;

import java.io.Closeable;
import java.io.IOException;

import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.meta.base.BaseFSConnectionConfig;

/**
 * Configuration for the {@link ArchiveZipFSConnection}.
 *
 * @author Dragan Keselj, KNIME GmbH
 */
public class ArchiveZipFSConnectionConfig extends BaseFSConnectionConfig implements Closeable {

    private FSPath m_zipFilePath;

    private boolean m_useDefaultEncoding;

    private String m_encoding;

    private Closeable m_closeable;

    /**
     * Constructor.
     *
     * @param workingDir
     */
    public ArchiveZipFSConnectionConfig(final String workingDir) {
        super(workingDir);
    }

    /**
     * @return FSPath of the zip file.
     */
    public FSPath getZipFilePath() {
        return m_zipFilePath;
    }

    /**
     * @param zipFilePath
     */
    public void setZipFilePath(final FSPath zipFilePath) {
        m_zipFilePath = zipFilePath;
    }

    /**
     * @return true, if the default encoding shall be used, false otherwise.
     */
    public boolean isUseDefaultEncoding() {
        return m_useDefaultEncoding;
    }

    /**
     * @param useDefaultEncoding whether the default encoding shall be used, or not.
     */
    public void setUseDefaultEncoding(final boolean useDefaultEncoding) {
        m_useDefaultEncoding = useDefaultEncoding;
    }

    /**
     * Returns the encoding used on unpacking the zip archive.
     *
     * @return encoding
     */
    public String getEncoding() {
        return m_encoding;
    }

    /**
     * Sets encoding used on unpacking the zip file.
     *
     * @param encoding
     */
    public void setEncoding(final String encoding) {
        m_encoding = encoding;
    }

    /**
     * Sets {@code Closeable} resource needed in this cfg object
     * and it has to be closed when this object is no longer in use.
     *
     * @param resource
     */
    public void setCloseable(final Closeable resource) {
        this.m_closeable = resource;
    }

    /**
     * Generates a {@link FSLocationSpec} for the current ArchiveZip file system
     * configuration.
     *
     * @return the {@link FSLocationSpec} for the current ArchiveZip file system
     *         configuration.
     */
    public static DefaultFSLocationSpec createFSLocationSpec() {
        return new DefaultFSLocationSpec(FSCategory.CONNECTED, ArchiveZipFSDescriptorProvider.FS_TYPE.getTypeId());
    }

    @Override
    public void close() throws IOException {
        if (m_closeable != null) {
            m_closeable.close();
        }
    }
}
