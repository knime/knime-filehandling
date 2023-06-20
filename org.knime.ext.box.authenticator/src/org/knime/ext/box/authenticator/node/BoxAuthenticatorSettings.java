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

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.VariableType.CredentialsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Not;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.credentials.base.oauth.api.scribejava.AuthCodeFlow;
import org.knime.credentials.base.oauth.api.scribejava.CustomApi20;
import org.knime.credentials.base.oauth.api.scribejava.CustomOAuth2ServiceBuilder;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.oauth2.clientauthentication.RequestBodyAuthenticationScheme;

/**
 * Settings class for the Box Authenticator node.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
@SuppressWarnings("restriction")
public class BoxAuthenticatorSettings implements DefaultNodeSettings {

    private static final String PARAM_BOX_SUBJECT_TYPE = "box_subject_type";
    private static final String PARAM_BOX_SUBJECT_ID = "box_subject_id";
    private static final String ENTERPRISE_SUBJECT_TYPE = "enterprise";

    private static final CustomApi20 BOX_API = new CustomApi20("https://api.box.com/oauth2/token", //
            "https://account.box.com/api/oauth2/authorize", //
            Verb.POST, //
            RequestBodyAuthenticationScheme.instance());

    /**
     * A {@link ChoicesProvider} yielding choices for credential flow variables.
     */
    static final class CredentialFlowVarChoicesProvider implements ChoicesProvider {
        @Override
        public String[] choices(final SettingsCreationContext context) {
            return context.getAvailableInputFlowVariables(CredentialsType.INSTANCE).keySet().toArray(String[]::new);
        }
    }

    @Widget(title = "Client ID and Secret", description = "")
    @ChoicesWidget(choices = CredentialFlowVarChoicesProvider.class, showNoneColumn = false)
    @Layout(BoxAppSection.class)
    String m_clientCredentialsFlowVariable = "";

    @Widget(title = "Type", description = "Authentication method to use.")
    @Layout(AuthenticationSection.class)
    @Signal(condition = AuthTypeIsClientCreds.class)
    @ValueSwitchWidget
    AuthType m_authType = AuthType.OAUTH;

    @Widget(title = "Enterprise ID", description = """
            The Box Enterprise ID when authenticating as a <a href="https://developer.box.com/guides/getting-started/user-types/service-account/">
            service account</a>.
            """)
    @Layout(AuthenticationSection.class)
    @Effect(signals = { AuthTypeIsClientCreds.class }, type = EffectType.SHOW)
    String m_enterpriseId;

    @Widget(title = "Redirect URL (should be http://localhost:XXXXX)", description = """
            The redirect URL to be used at the end of the interactive login. Should be chosen as http://localhost:XXXXX
            where XXXXX is a random number in the 10000 - 65000 range to avoid conflicts. The redirect URL is part of the
            App configuration in Box.
            """)
    @Layout(AuthenticationSection.class)
    @Effect(signals = { AuthTypeIsClientCreds.class }, type = EffectType.SHOW, operation = Not.class)
    String m_redirectUrl = "http://localhost:33749/";

    @Section(title = "Box App")
    interface BoxAppSection {
    }

    @Section(title = "Authentication method")
    @After(BoxAppSection.class)
    interface AuthenticationSection {
    }

    static class AuthTypeIsClientCreds extends OneOfEnumCondition<AuthType> {
        @Override
        public AuthType[] oneOf() {
            return new AuthType[] { AuthType.CLIENT_CREDENTIALS };
        }
    }

    enum AuthType {
        @Label("OAuth 2")
        OAUTH,

        @Label("Client credentials")
        CLIENT_CREDENTIALS;
    }

    /**
     * Performs interactive login. The method will be called by the login button in
     * the dialog.
     *
     * @param settings
     *            The node settings.
     * @return the {@link OAuth2AccessToken} if the login was successful.
     * @throws Exception
     *             if the login failed for some reason.
     */
    static OAuth2AccessToken doLogin(final CredentialsProvider credsProvider, final BoxAuthenticatorSettings settings)
            throws Exception {
        try (var service = settings.createService(credsProvider)) {
            return new AuthCodeFlow(service, URI.create(settings.m_redirectUrl))//
                    .login(null);
        }
    }

    /**
     * Creates {@link OAuth20Service} from the current settings.
     *
     * @return The {@link OAuth20Service} instance.
     */
    OAuth20Service createService(final CredentialsProvider credsProvider) {
        var creds = credsProvider.get(m_clientCredentialsFlowVariable);
        var clientId = creds.getLogin();
        var clientSecret = creds.getPassword();

        var builder = new CustomOAuth2ServiceBuilder(clientId);
        if (!StringUtils.isEmpty(clientSecret)) {
            builder.apiSecret(clientSecret);
        }

        if (m_authType == AuthType.CLIENT_CREDENTIALS) {
            var subjectType = ENTERPRISE_SUBJECT_TYPE;
            var subjectId = m_enterpriseId;

            builder.additionalRequestBodyField(PARAM_BOX_SUBJECT_TYPE, subjectType);
            builder.additionalRequestBodyField(PARAM_BOX_SUBJECT_ID, subjectId);
        } else {
            builder.callback(m_redirectUrl);
        }

        return builder.build(BOX_API);
    }
}
