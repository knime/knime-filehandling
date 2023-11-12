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
 *   2023-06-08 (Alexander Bondaletov, Redfield SE): created
 */
package org.knime.ext.box.authenticator.node2;

import java.util.Optional;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.credentials.base.Credential;
import org.knime.credentials.base.CredentialPortObjectSpec;
import org.knime.credentials.base.CredentialRef;
import org.knime.credentials.base.node.AuthenticatorNodeModel;
import org.knime.credentials.base.oauth.api.AccessTokenCredential;
import org.knime.credentials.base.oauth.api.JWTCredential;
import org.knime.ext.box.authenticator.node2.BoxAuthenticatorSettings.AuthType;

/**
 * Node model of the Box Authenticator node. Perform authentication using auth
 * code grant or client credential grant. Produces port object containing
 * {@link AccessTokenCredential}.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
@SuppressWarnings("restriction")
public class BoxAuthenticatorNodeModel extends AuthenticatorNodeModel<BoxAuthenticatorSettings> {

    private static final String LOGIN_FIRST_ERROR = "Please use the configuration dialog to log in first.";

    /**
     * This references a {@link JWTCredential} or {@link AccessTokenCredential} that
     * was acquired interactively in the node dialog. It is disposed when the
     * workflow is closed, or when the authentication scheme is switched to
     * non-interactive. However, it is NOT disposed during reset().
     */
    private CredentialRef m_interactiveCredentialRef;

    /**
     * @param configuration
     *            The node configuration.
     */
    protected BoxAuthenticatorNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, BoxAuthenticatorSettings.class);
    }

    @Override
    protected void validateOnConfigure(final PortObjectSpec[] inSpecs, final BoxAuthenticatorSettings settings)
            throws InvalidSettingsException {

        settings.validate();

        if (settings.m_authType == AuthType.OAUTH) {
            m_interactiveCredentialRef = Optional.ofNullable(settings.m_loginCredentialRef)//
                    .map(CredentialRef::new)//
                    .orElseThrow(() -> new InvalidSettingsException(LOGIN_FIRST_ERROR));

            if (!m_interactiveCredentialRef.isPresent()) {
                throw new InvalidSettingsException(LOGIN_FIRST_ERROR);
            }
        } else {
            disposeInteractiveCredential();
        }
    }

    @Override
    protected final CredentialPortObjectSpec createSpecInConfigure(final PortObjectSpec[] inSpecs,
            final BoxAuthenticatorSettings modelSettings) {
        // Box issues string access token which are not JWTs
        return new CredentialPortObjectSpec(AccessTokenCredential.TYPE, null);
    }

    @Override
    protected Credential createCredential(final PortObject[] inObjects, final ExecutionContext exec,
            final BoxAuthenticatorSettings settings) throws Exception {

        return switch (settings.m_authType) {
        case OAUTH -> //
                m_interactiveCredentialRef.getCredential(Credential.class)//
                        .orElseThrow(() -> new InvalidSettingsException(LOGIN_FIRST_ERROR));
        case CLIENT_CREDENTIALS -> //
                ScribeJavaHelper.fetchCredentialViaClientCredentialsFlow(settings);
        default -> throw new IllegalArgumentException("Usupported auth type: " + settings.m_authType);
        };
    }

    private void disposeInteractiveCredential() {
        if (m_interactiveCredentialRef != null) {
            m_interactiveCredentialRef.dispose();
            m_interactiveCredentialRef = null;
        }
    }

    @Override
    protected void onDisposeInternal() {
        disposeInteractiveCredential();
    }
}
