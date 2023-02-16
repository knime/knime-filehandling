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
 *   2023-02-16 (Alexander Bondaletov): created
 */
package org.knime.ext.box.filehandling.fs;

import java.nio.file.attribute.FileTime;
import java.util.Date;

import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;

import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;

/**
 * {@link BaseFileAttributes} implementation for the Box file system
 *
 * @author Alexander Bondaletov, Redfield SE
 */
class BoxFileAttributes extends BaseFileAttributes {

    private final String m_itemId;

    /**
     * @param path
     *            The path.
     * @param info
     *            The box item info.
     */
    public BoxFileAttributes(final BoxPath path, final BoxItem.Info info) {
        super(info instanceof BoxFile.Info, //
                path, //
                toFileTime(info.getModifiedAt()), //
                toFileTime(info.getModifiedAt()), //
                toFileTime(info.getCreatedAt()), //
                info.getSize(), //
                false, //
                !(info instanceof BoxFile.Info || info instanceof BoxFolder.Info), //
                null);
        m_itemId = info.getID();
    }

    private static FileTime toFileTime(final Date date) {
        if (date == null) {
            return FileTime.fromMillis(0);
        } else {
            return FileTime.from(date.toInstant());
        }
    }

    /**
     * Creates attributes for the root directory.
     *
     * @param root
     *            The root path.
     */
    public BoxFileAttributes(final BoxPath root) {
        super(false, //
                root, //
                FileTime.fromMillis(0), //
                FileTime.fromMillis(0), //
                FileTime.fromMillis(0), //
                0, //
                false, //
                false, //
                null);
        m_itemId = null;
    }

    /**
     * @return the itemId
     */
    public String getItemId() {
        return m_itemId;
    }
}
