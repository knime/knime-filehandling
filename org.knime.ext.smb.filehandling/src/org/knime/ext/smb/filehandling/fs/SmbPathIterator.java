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
 *   2021-03-08 (Alexander Bondaletov): created
 */
package org.knime.ext.smb.filehandling.fs;

import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.knime.filehandling.core.connections.base.BasePathIterator;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;

import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.smbj.share.DiskShare;

/**
 * Iterator to iterate through {@link SmbPath}.
 *
 * @author Alexander Bondaletov
 */
class SmbPathIterator extends BasePathIterator<SmbPath> {

    private static final Set<String> RESERVED_NAMES = new HashSet<>(Arrays.asList(".", ".."));

    /**
     * @param path
     *            path to iterate.
     * @param filter
     *            {@link Filter} instance.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    SmbPathIterator(final SmbPath path, final Filter<? super Path> filter) throws IOException {
        super(path, filter);

        DiskShare client = path.getFileSystem().getClient();
        try {
            Iterator<SmbPath> iterator = client.list(path.getSmbjPath()) //
                    .stream() //
                    .filter(SmbPathIterator::isRegularPath) //
                    .map(this::toPath) //
                    .iterator();

            setFirstPage(iterator); // NOSONAR standard pattern
        } catch (SMBApiException exb) {
            throw SmbUtils.toIOE(exb, path.toString());
        }
    }

    private static boolean isRegularPath(final FileIdBothDirectoryInformation fileInfo) {
        return !RESERVED_NAMES.contains(fileInfo.getFileName());
    }

    @SuppressWarnings("resource")
    private SmbPath toPath(final FileIdBothDirectoryInformation fileInfo) {
        SmbFileSystem fs = m_path.getFileSystem();
        SmbPath path = (SmbPath) m_path.resolve(fileInfo.getFileName());

        boolean isDirectory = SmbUtils.isDirectory(fileInfo.getFileAttributes());
        FileTime createdAt = FileTime.fromMillis(fileInfo.getChangeTime().toEpochMillis());
        FileTime modifiedAt = FileTime.fromMillis(fileInfo.getChangeTime().toEpochMillis());
        FileTime accessedAt = FileTime.fromMillis(fileInfo.getLastAccessTime().toEpochMillis());

        BaseFileAttributes attrs = new BaseFileAttributes(!isDirectory, path, modifiedAt, accessedAt, createdAt,
                fileInfo.getAllocationSize(), false, false, null);
        fs.addToAttributeCache(path, attrs);

        return path;
    }
}
