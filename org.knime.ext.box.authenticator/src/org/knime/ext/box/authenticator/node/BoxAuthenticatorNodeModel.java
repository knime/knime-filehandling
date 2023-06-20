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
package org.knime.ext.box.authenticator.node;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.credentials.base.Credential;
import org.knime.credentials.base.CredentialPortObjectSpec;
import org.knime.credentials.base.node.AuthenticatorNodeModel;
import org.knime.credentials.base.oauth.api.AccessTokenCredential;
import org.knime.credentials.base.oauth.api.scribejava.ClientCredentialsFlow;
import org.knime.credentials.base.oauth.api.scribejava.CredentialFactory;
import org.knime.ext.box.authenticator.node.BoxAuthenticatorSettings.AuthType;

import com.github.scribejava.core.model.OAuth2AccessToken;

/**
 * Node model of the Box Authenticator node. Perform authentication using auth
 * code grant or client credential grant. Produces port object containing
 * {@link AccessTokenCredential}.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
@SuppressWarnings("restriction")
public class BoxAuthenticatorNodeModel extends AuthenticatorNodeModel<BoxAuthenticatorSettings> {

    /**
     * @param configuration
     *            The node configuration.
     */
    protected BoxAuthenticatorNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, BoxAuthenticatorSettings.class);
    }

    @Override
    protected void validate(final PortObjectSpec[] inSpecs, final BoxAuthenticatorSettings settings)
            throws InvalidSettingsException {

        if (StringUtils.isEmpty(settings.m_clientCredentialsFlowVariable)) {
            throw new InvalidSettingsException("Flow variable with client ID and secret is missing");
        }

        if (!getCredentialsProvider().listNames().contains(settings.m_clientCredentialsFlowVariable)) {
            throw new InvalidSettingsException(String.format("Cannot find chosen credentials flow variable '%s'",
                    settings.m_clientCredentialsFlowVariable));
        }

        if (settings.m_authType == AuthType.CLIENT_CREDENTIALS) {
            if (StringUtils.isEmpty(settings.m_enterpriseId)) {
                throw new InvalidSettingsException("Enterprise ID is required");
            }
        }
    }

    @Override
    protected final CredentialPortObjectSpec createSpecInConfigure(final PortObjectSpec[] inSpecs,
            final BoxAuthenticatorSettings modelSettings) {
        // Box issues string access token which are not JWTs
        return new CredentialPortObjectSpec(AccessTokenCredential.TYPE);
    }

    @Override
    protected Credential createCredential(final PortObject[] inObjects, final ExecutionContext exec,
            final BoxAuthenticatorSettings settings) throws Exception {

        var scribeJavaToken = fetchAccessToken(settings);
        return CredentialFactory.fromScribeToken(scribeJavaToken,
                () -> settings.createService(getCredentialsProvider()));
    }

    private OAuth2AccessToken fetchAccessToken(final BoxAuthenticatorSettings settings) throws Exception {

        switch (settings.m_authType) {
        case CLIENT_CREDENTIALS:
            try (var service = settings.createService(getCredentialsProvider())) {
                return new ClientCredentialsFlow(service).login(null);
            }
        case OAUTH:
            return BoxAuthenticatorSettings.doLogin(getCredentialsProvider(), settings);
        default:
            throw new IllegalArgumentException("Usupported auth type: " + settings.m_authType);
        }
    }
}
