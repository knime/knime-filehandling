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
 *   2020-07-28 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ssh.filehandling.fs;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.sshd.client.subsystem.sftp.SftpClient;
import org.apache.sshd.client.subsystem.sftp.SftpClient.DirEntry;

/**
 * Class to iterate through the files and folders in the path
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class SshPathIterator implements Iterator<SshPath> {
    private Iterator<SftpClient.DirEntry> m_dirEntryIterator;
    private final Filter<? super Path> m_filter;

    private SshPath m_next;
    private SshPath m_dir;

    /**
     * @param dir
     *            directory path.
     * @param iter
     *            directory iterator.
     * @param f
     *            filter.
     */
    public SshPathIterator(final SshPath dir, final Iterator<SftpClient.DirEntry> iter, final Filter<? super Path> f) {
        super();
        m_dirEntryIterator = iter;
        m_filter = f;
        m_dir = dir;

        goToNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_next != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SshPath next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        final SshPath p = m_next;
        goToNext();
        return p;
    }

    private void goToNext() {
        m_next = null;
        while (true) {
            if (!m_dirEntryIterator.hasNext()) {
                return;
            }

            final DirEntry entry = m_dirEntryIterator.next();
            String fileName = entry.getFilename();

            // ignore current and parent directory
            if (".".equals(fileName) || "..".equals(fileName)) {
                continue;
            }

            final SshPath path = createSshPath(fileName);
            try {
                if (m_filter == null || m_filter.accept(path)) {
                    m_next = path;
                    return;
                }
            } catch (final IOException e) {
                throw new DirectoryIteratorException(e);
            }
        }
    }

    private SshPath createSshPath(final String fileName) {
        return (SshPath) m_dir.resolve(fileName);
    }
}
