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
 *   2020-12-01 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.ext.ssh.filehandling.node.auth;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelPassword;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.ext.ssh.filehandling.node.SshConnectorNodeFactory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.base.auth.AuthProviderSettings;
import org.knime.filehandling.core.connections.base.auth.AuthType;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class KeyFileAuthProviderSettings implements AuthProviderSettings {

    private static final String KEY_USER = "user";

    private static final String KEY_USE_PASSPHRASE = "use_passphrase";

    private static final String KEY_PASSPHRASE = "passphrase";

    /**
     * Settings key for the key file (must be public to derive flow variable model).
     */
    public static final String KEY_FILE = "file";

    private static final String SECRET_KEY = "ekerjvjhmzle,ptktysq";

    private final SettingsModelString m_keyUser;

    private SettingsModelBoolean m_useKeyPassphrase;

    private final SettingsModelPassword m_keyPassphrase;

    private SettingsModelReaderFileChooser m_keyFile;

    private final NodeCreationConfiguration m_nodeCreationConfig;

    private boolean m_enabled;

    /**
     * Creates a new instance.
     *
     * @param cfg
     */
    public KeyFileAuthProviderSettings(final NodeCreationConfiguration cfg) {
        m_keyUser = new SettingsModelString(KEY_USER, System.getProperty("user.name"));
        m_useKeyPassphrase = new SettingsModelBoolean(KEY_USE_PASSPHRASE, false);
        m_keyPassphrase = new SettingsModelPassword(KEY_PASSPHRASE, SECRET_KEY, "");
        m_keyFile = new SettingsModelReaderFileChooser(KEY_FILE,
                cfg.getPortConfig().orElseThrow(() -> new IllegalStateException("port creation config is absent")),
                SshConnectorNodeFactory.FS_CONNECT_GRP_ID, EnumConfig.create(FilterMode.FILE));

        m_useKeyPassphrase.addChangeListener(e -> m_keyPassphrase
                .setEnabled(m_useKeyPassphrase.getBooleanValue() && m_useKeyPassphrase.isEnabled()));

        m_nodeCreationConfig = cfg;
        m_enabled = true;
        updateEnabledness();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        m_enabled = enabled;
        updateEnabledness();
    }

    @Override
    public boolean isEnabled() {
        return m_enabled;
    }

    private void updateEnabledness() {
        m_keyUser.setEnabled(m_enabled);
        m_useKeyPassphrase.setEnabled(m_enabled);
        m_keyPassphrase.setEnabled(m_enabled && usePassphrase());
        m_keyFile.setEnabled(m_enabled);
    }

    /**
     * @return key file location.
     */
    public FSLocation getKeyFile() {
        return m_keyFile.getLocation();
    }

    @Override
    public AuthType getAuthType() {
        return SshAuth.KEY_FILE_AUTH_TYPE;
    }

    /**
     * @return key file location model.
     */
    public SettingsModelReaderFileChooser getKeyFileModel() {
        return m_keyFile;
    }

    /**
     * @return true if has key file.
     */
    public boolean hasKeyFile() {
        return !StringUtils.isBlank(m_keyFile.getLocation().getPath());
    }

    /**
     * @return the key file username model
     */
    public SettingsModelString getKeyUserModel() {
        return m_keyUser;
    }

    /**
     * @return model to determine whether to use a key passphrase or not.
     */
    public SettingsModelBoolean getUseKeyPassphraseModel() {
        return m_useKeyPassphrase;
    }

    /**
     * @return whether to use a key passphrase or not.
     */
    private boolean usePassphrase() {
        return m_useKeyPassphrase.getBooleanValue();
    }

    /**
     * @return key file password settings model.
     */
    public SettingsModelPassword getKeyPassphraseModel() {
        return m_keyPassphrase;
    }

    @Override
    public void configureInModel(final PortObjectSpec[] specs, final Consumer<StatusMessage> statusMessageConsumer,
            final CredentialsProvider credentialsProvider) throws InvalidSettingsException {

        if (isEnabled()) {
            m_keyFile.configureInModel(specs, statusMessageConsumer);
        }
    }

    private void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_keyUser.loadSettingsFrom(settings);
        m_useKeyPassphrase.loadSettingsFrom(settings);
        m_keyPassphrase.loadSettingsFrom(settings);

        updateEnabledness();
    }
    @Override
    public void loadSettingsForDialog(final NodeSettingsRO settings) throws NotConfigurableException {
        try {
            load(settings);
        } catch (InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage(), ex);
        }
        // m_keyFile must be loaded by dialog component
    }

    @Override
    public void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        load(settings);
        m_keyFile.loadSettingsFrom(settings);
    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_keyUser.validateSettings(settings);
        m_useKeyPassphrase.validateSettings(settings);
        m_keyPassphrase.validateSettings(settings);
        m_keyFile.validateSettings(settings);
    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (StringUtils.isBlank(m_keyUser.getStringValue())) {
            throw new InvalidSettingsException("Please provide a valid user name");
        }

        if (m_useKeyPassphrase.getBooleanValue() && m_keyPassphrase.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("Please enter a passphrase for the private key file");
        }

        if (!hasKeyFile()) {
            throw new InvalidSettingsException("Please select a valid key file location");
        }
    }


    /**
     * Saves settings to the given {@link NodeSettingsWO}.
     *
     * @param settings
     */
    private void save(final NodeSettingsWO settings) {
        if (!isEnabled()) {
            // don't save and persist credentials if they are not selected
            // see ticket AP-21749
            clear();
        } else if (!m_useKeyPassphrase.getBooleanValue()) {
            m_keyPassphrase.setStringValue("");
        }
        m_keyUser.saveSettingsTo(settings);
        m_useKeyPassphrase.saveSettingsTo(settings);
        m_keyPassphrase.saveSettingsTo(settings);
    }


    @Override
    public void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        save(settings);
        // m_keyFile must be saved by dialog component
    }

    @Override
    public void saveSettingsForModel(final NodeSettingsWO settings) {
        save(settings);
        m_keyFile.saveSettingsTo(settings);
    }

    @Override
    public void clear() {
        m_keyUser.setStringValue("");
        m_keyPassphrase.setStringValue("");
    }

    @Override
    public AuthProviderSettings createClone() {
        final var tempSettings = new NodeSettings("ignored");
        saveSettingsForModel(tempSettings);

        final var toReturn = new KeyFileAuthProviderSettings(m_nodeCreationConfig);
        try {
            toReturn.loadSettingsForModel(tempSettings);
        } catch (InvalidSettingsException ex) { // NOSONAR can never happen
            // won't happen
        }
        return toReturn;
    }
}
