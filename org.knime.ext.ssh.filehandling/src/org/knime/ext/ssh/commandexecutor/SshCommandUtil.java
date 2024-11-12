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
 *   2024-11-11 (loescher): created
 */
package org.knime.ext.ssh.commandexecutor;

import static org.knime.ext.ssh.commandexecutor.SshCommandExecutorNodeSettings.INPUT_FILE_PLACEHOLDER;
import static org.knime.ext.ssh.commandexecutor.SshCommandExecutorNodeSettings.OUTPUT_FILE_PLACEHOLDER;

import java.nio.file.Path;

import org.knime.filehandling.core.connections.FSFileSystem;

/**
 * Utility methods of the SSH command executor node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class SshCommandUtil {

    private SshCommandUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String getCommand(final SshCommandExecutorNodeSettings settings, final FSFileSystem<?> fs,
            final boolean useSh) {
        var result = settings.m_command;
        if (useSh) {
            var args = "";
            if (settings.m_useInputPath) {
                result = result.replace(INPUT_FILE_PLACEHOLDER, "$1");
                args += " " + getPathString(settings, fs.getPath(settings.m_inputPath.getFSLocation()), useSh);
            } else {
                args += " ''";
            }
            if (settings.m_useOutputPath) {
                result = result.replace(OUTPUT_FILE_PLACEHOLDER, "$2");
                args += " " + getPathString(settings, fs.getPath(settings.m_outputPath.getFSLocation()), useSh);
            } else {
                args += " ''";
            }

            result = String.format(SshCommandExecutorNodeModel.SH_COMMAND_TEMPLATE, escapeStringSh(result), args);

        } else {
            if (settings.m_useInputPath) {
                result = result.replace(INPUT_FILE_PLACEHOLDER,
                        getPathString(settings, fs.getPath(settings.m_inputPath.getFSLocation()), useSh));
            }
            if (settings.m_useOutputPath) {
                result = result.replace(OUTPUT_FILE_PLACEHOLDER,
                        getPathString(settings, fs.getPath(settings.m_outputPath.getFSLocation()), useSh));
            }
        }
        return result;
    }

    static String escapeStringSh(final String str) {
        return "'" + str.replace("'", "'\\''") + "'";
    }

    private static String getPathString(final SshCommandExecutorNodeSettings settings, final Path path,
            final boolean useSh) {
        var result = path.normalize().toAbsolutePath().toString();
        if (useSh) {
            return escapeStringSh(result);
        }

        if (!settings.m_useDOSPaths) {
            return result;
        }

        // remove leading slash and replace path separators
        return result.substring(1).replace('/', '\\');
    }

}
