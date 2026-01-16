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
 * ------------------------------------------------------------------------
 */

package org.knime.ext.box.filehandling.node;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithCustomFileSystem;
import org.knime.credentials.base.CredentialPortObject;
import org.knime.credentials.base.oauth.api.AccessTokenCredential;
import org.knime.ext.box.filehandling.fs.BoxFSConnection;
import org.knime.ext.box.filehandling.fs.BoxFSConnectionConfig;
import org.knime.ext.box.filehandling.fs.BoxFileSystem;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;

/**
 * Node parameters for Box Connector.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class BoxConnectorNodeParameters implements NodeParameters {

    private static final int DEFAULT_TIMEOUT = 60;

    @Advanced
    @Section(title = "Timeouts")
    interface TimeoutsSection {
    }

    @Widget(title = "Working directory", description = """
            Specify the working directory of the resulting file system connection.
            The working directory must be specified as an absolute path.
            A working directory allows downstream nodes to access files/folders using relative paths,
            i.e. paths that do not have a leading slash. The default working directory is "/".""")
    @FileSelectionWidget(SingleFileSelectionMode.FOLDER)
    @WithCustomFileSystem(connectionProvider = BoxFileSystemConnectionProvider.class)
    @ValueReference(WorkingDirectoryRef.class)
    String m_workingDirectory = BoxFileSystem.SEPARATOR;

    static final class WorkingDirectoryRef implements ParameterReference<String> {
    }

    @Widget(title = "Connection timeout (seconds)", description = """
            Timeout in seconds to establish a connection,
            or 0 for an infinite timeout.""")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Layout(TimeoutsSection.class)
    @ValueReference(ConnectionTimeoutRef.class)
    int m_connectionTimeout = DEFAULT_TIMEOUT;

    static final class ConnectionTimeoutRef implements ParameterReference<Integer> {
    }

    @Widget(title = "Read timeout (seconds)", description = """
            Timeout in seconds to read data from an established connection,
            or 0 for an infinite timeout.""")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Layout(TimeoutsSection.class)
    @ValueReference(ReadTimeoutRef.class)
    int m_readTimeout = DEFAULT_TIMEOUT;

    static final class ReadTimeoutRef implements ParameterReference<Integer> {
    }

    public void validateInConfigure() throws InvalidSettingsException {
        CheckUtils.checkSetting(
                m_workingDirectory != null && !m_workingDirectory.isEmpty()
                        && m_workingDirectory.startsWith(BoxFileSystem.SEPARATOR),
                "Working directory must be set to an absolute path.");
    }

    BoxFSConnectionConfig createFSConnectionConfig(final String accessToken) {
        var config = new BoxFSConnectionConfig(m_workingDirectory);

        config.setConnectionTimeout(Duration.ofSeconds(m_connectionTimeout));
        config.setReadTimeout(Duration.ofSeconds(m_readTimeout));

        config.setAccessToken(accessToken);

        return config;
    }

    /**
     * Provides a {@link FSConnectionProvider} based on the Box connection settings
     * and the credential from the input port.
     */
    static final class BoxFileSystemConnectionProvider implements StateProvider<FSConnectionProvider> {

        private static final String NO_CREDENTIAL = "Credential is not available. "
                + "Please connect and re-execute the authenticator node.";

        private Supplier<Integer> m_connectionTimeoutSupplier;
        private Supplier<Integer> m_readTimeoutSupplier;
        private Supplier<String> m_workingDirectory;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_connectionTimeoutSupplier = initializer.computeFromValueSupplier(ConnectionTimeoutRef.class);
            m_readTimeoutSupplier = initializer.computeFromValueSupplier(ReadTimeoutRef.class);
            m_workingDirectory = initializer.computeFromValueSupplier(WorkingDirectoryRef.class);
            initializer.computeAfterOpenDialog();
        }

        @Override
        public FSConnectionProvider computeState(final NodeParametersInput parametersInput) {
            return () -> createConnection((CredentialPortObject) parametersInput.getInPortObject(0)
                    .orElseThrow(() -> new InvalidSettingsException(NO_CREDENTIAL)));
        }

        private FSConnection createConnection(final CredentialPortObject credentialPortObject)
                throws InvalidSettingsException, IOException {
            final var params = new BoxConnectorNodeParameters();
            params.m_workingDirectory = m_workingDirectory.get();
            if (params.m_workingDirectory == null || params.m_workingDirectory.isBlank()) {
                params.m_workingDirectory = BoxFileSystem.SEPARATOR;
            }
            params.m_connectionTimeout = m_connectionTimeoutSupplier.get();
            params.m_readTimeout = m_readTimeoutSupplier.get();
            params.validateInConfigure();

            final var accessToken = credentialPortObject.getCredential(AccessTokenCredential.class)
                    .orElseThrow(() -> new InvalidSettingsException(NO_CREDENTIAL));

            return new BoxFSConnection(params.createFSConnectionConfig(accessToken.getAccessToken()));
        }
    }

}
