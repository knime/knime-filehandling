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
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMBApiException;

/**
 * Utility class for the SMB file system.
 *
 * @author Alexander Bondaletov
 */
final class SmbUtils {

    private SmbUtils() {
    }

    /**
     * Converts {@link SMBApiException} to {@link IOException}. Makes an attempt to
     * derive an appropriate sub-type of {@link IOException} from the status code.
     *
     *
     * @param ex
     *            The {@link SMBApiException} instance.
     * @param file
     *            A string identifying the file or {@code null} if not known.
     * @return The {@link IOException} instance.
     */
    public static IOException toIOE(final SMBApiException ex, final String file) {
        return toIOE(ex, file, null);
    }

    /**
     * Converts {@link SMBApiException} to {@link IOException}. Makes an attempt to
     * derive an appropriate sub-type of {@link IOException} from the status code.
     *
     *
     * @param ex
     *            The {@link SMBApiException} instance.
     * @param file
     *            A string identifying the file or {@code null} if not known.
     * @param other
     *            A string identifying the other file or {@code null} if not known.
     * @return The {@link IOException} instance.
     */
    public static IOException toIOE(final SMBApiException ex, final String file, final String other) {
        IOException result = null;

        switch (ex.getStatus()) {
        case STATUS_OBJECT_NAME_NOT_FOUND:
        case STATUS_OBJECT_PATH_NOT_FOUND:
        case STATUS_OBJECT_NAME_INVALID:
            result = new NoSuchFileException(file, other, ex.getMessage());
            break;
        case STATUS_ACCESS_DENIED:
        case STATUS_SHARING_VIOLATION:
        case STATUS_CANNOT_DELETE:
        case STATUS_FILE_ENCRYPTED:
            result = new AccessDeniedException(file, other, ex.getMessage());
            break;
        default:
            result = new IOException(getExceptionMessage(ex));
            break;
        }

        result.initCause(ex);
        return result;
    }

    private static String getExceptionMessage(final SMBApiException ex) {
        switch (ex.getStatus()) {
        case STATUS_BAD_NETWORK_NAME:
            return "Unable to connect to share/namespace, probably the name of the share/namespace is wrong.";
        case STATUS_LOGON_FAILURE:
            return "Authentication failed, probably the username and/or password is wrong.";
        case STATUS_ACCOUNT_DISABLED:
            return "Authentication failed, the account is disabled.";
        default:
            return ex.getMessage();
        }
    }

    /**
     * @param attributes
     *            The file attributes value.
     * @return Whether provided file attributes have 'FILE_ATTRIBUTE_DIRECTORY' set.
     */
    public static boolean isDirectory(final long attributes) {
        return checkFileAttribute(attributes, FileAttributes.FILE_ATTRIBUTE_DIRECTORY);
    }

    private static boolean checkFileAttribute(final long attributes, final FileAttributes flag) {
        return (attributes & flag.getValue()) != 0;
    }
}
