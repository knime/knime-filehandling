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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.DefaultProvider;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.Credentials;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Advanced;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.CancelableActionHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.credentials.CredentialsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.credentials.base.CredentialCache;
import org.knime.credentials.base.oauth.api.nodesettings.AbstractTokenCacheKeyPersistor;
import org.knime.credentials.base.oauth.api.scribejava.CustomApi20;
import org.knime.credentials.base.oauth.api.scribejava.CustomOAuth2ServiceBuilder;

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

    private static final NodeLogger LOG = NodeLogger.getLogger(BoxAuthenticatorSettings.class);

    private static final String PARAM_BOX_SUBJECT_TYPE = "box_subject_type";
    private static final String PARAM_BOX_SUBJECT_ID = "box_subject_id";
    private static final String ENTERPRISE_SUBJECT_TYPE = "enterprise";
    private static final String DEFAULT_APP_ID = "ba33glhgadtrtp2hvl2pnulat70sm1xq";
    private static final String DEFAULT_APP_SECRET = "E2sOEHf94qFI69GubGE9fesAZFIoHlOJ"; // NOSONAR
    private static final String DEFAULT_REDIRECT_URL = "http://localhost:33749";

    private static final CustomApi20 BOX_API = new CustomApi20("https://api.box.com/oauth2/token", //
            "https://account.box.com/api/oauth2/authorize", //
            Verb.POST, //
            RequestBodyAuthenticationScheme.instance());

    @Widget(title = "Authentication type", //
            description = """
                    Authentication type to use. The following types are supported:
                    <ul>
                        <li>
                            <a href="https://developer.box.com/guides/authentication/oauth2/">
                                <b>User authentication</b>
                            </a>
                        </li>
                        <li>
                            <a href="https://developer.box.com/guides/authentication/client-credentials/">
                                <b>Server authentication (client credentials)</b>
                            </a>
                        </li>
                    </ul>
                    """)
    @ValueReference(AuthTypeRef.class)
    AuthType m_authType = AuthType.OAUTH;

    enum AuthType {
        @Label("User authentication")
        OAUTH,

        @Label("Server authentication (client credentials)")
        CLIENT_CREDENTIALS;
    }

    interface AuthTypeRef extends Reference<AuthType> {
    }

    static class AuthTypeIsOAuth implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(AuthTypeRef.class).isOneOf(AuthType.OAUTH);
        }
    }

    static class AuthTypeIsClientCreds implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(AuthTypeRef.class).isOneOf(AuthType.CLIENT_CREDENTIALS);
        }
    }

    @Section(title = "Client/App configuration")
    @Advanced
    interface ClientAppSection {
    }

    @Section(title = "Authentication")
    @Effect(predicate = AuthTypeIsOAuth.class, type = EffectType.SHOW)
    @After(ClientAppSection.class)
    interface AuthenticationSection {
    }

    @Widget(title = "Which client/app to use", //
            description = """
                    Whether to use the KNIME default app, or enter a custom one. The
                    KNIME default app is called "KNIME Analytics Platform".
                    """)
    @ValueSwitchWidget
    @Layout(ClientAppSection.class)
    @Effect(predicate = AuthTypeIsOAuth.class, type = EffectType.SHOW)
    @ValueReference(ClientSelectionRef.class)
    @Migration(ClientSelectionLegacyDefault.class)
    ClientSelection m_clientSelection = ClientSelection.DEFAULT;

    enum ClientSelection {
        DEFAULT, CUSTOM;
    }

    interface ClientSelectionRef extends Reference<ClientSelection> {
    }

    static class IsCustomSelection implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(ClientSelectionRef.class).isOneOf(ClientSelection.CUSTOM);
        }
    }

    static class IsCredsOrCustomSelection implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            final var typeOAuth = i.getPredicate(AuthTypeIsClientCreds.class);
            final var customSelection = i.getPredicate(IsCustomSelection.class);
            return or(typeOAuth, customSelection);
        }
    }

    static class ClientSelectionLegacyDefault implements DefaultProvider<ClientSelection> {

        @Override
        public ClientSelection getDefault() {
            return ClientSelection.CUSTOM;
        }
    }

    static class IsOAuthAndCustom implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            final var typeOAuth = i.getPredicate(AuthTypeIsOAuth.class);
            final var customSelection = i.getPredicate(IsCustomSelection.class);
            return and(typeOAuth, customSelection);
        }
    }

    @Widget(title = "Client/App configuration", //
            description = """
                    The app/client ID and secret of the custom Box app.
                    These fields can be found in the configuration settings of your custom Box app.
                    """)
    @CredentialsWidget(usernameLabel = "ID", passwordLabel = "Secret")
    @Layout(ClientAppSection.class)
    @Effect(predicate = IsCredsOrCustomSelection.class, type = EffectType.SHOW)
    @Migrate(loadDefaultIfAbsent = true)
    Credentials m_boxApp = new Credentials();

    @Widget(title = "Redirect URL (should be http://localhost:XXXXX)", description = """
            The redirect URL to be used at the end of the interactive login.
            Should be chosen as http://localhost:XXXXX where XXXXX is a random number in the 10000 - 65000 range
            to avoid conflicts. The redirect URL is part of the App configuration in Box.
            """)
    @Layout(ClientAppSection.class)
    @Effect(predicate = IsOAuthAndCustom.class, type = EffectType.SHOW)
    String m_redirectUrl = DEFAULT_REDIRECT_URL;

    @Widget(title = "Enterprise ID", description = """
            The Box Enterprise ID when authenticating as a
            <a href="https://developer.box.com/guides/getting-started/user-types/service-account/">
            service account</a>.
            """)
    @Layout(ClientAppSection.class)
    @Effect(predicate = AuthTypeIsClientCreds.class, type = EffectType.SHOW)
    String m_enterpriseId;

    @ButtonWidget(actionHandler = LoginActionHandler.class, //
            updateHandler = LoginUpdateHandler.class, //
            showTitleAndDescription = false)
    @Widget(title = "Login", //
            description = "Clicking on login opens a new browser window/tab which "
                    + "allows to interactively log into Box.")
    @Persistor(LoginCredentialRefPersistor.class)
    @Layout(AuthenticationSection.class)
    UUID m_loginCredentialRef;

    static final class LoginCredentialRefPersistor extends AbstractTokenCacheKeyPersistor {
        LoginCredentialRefPersistor() {
            super("loginCredentialRef");
        }
    }

    static class LoginActionHandler extends CancelableActionHandler<UUID, BoxAuthenticatorSettings> {

        @Override
        protected UUID invoke(final BoxAuthenticatorSettings settings, final DefaultNodeSettingsContext context)
                throws WidgetHandlerException {

            try {
                settings.validate();
            } catch (InvalidSettingsException e) { // NOSONAR
                throw new WidgetHandlerException(e.getMessage());
            }

            try {
                return CredentialCache.store(ScribeJavaHelper.fetchCredentialViaAuthCodeFlow(settings));
            } catch (Exception e) {
                LOG.debug("Interactive login failed: " + e.getMessage(), e);
                throw new WidgetHandlerException(e.getMessage());
            }
        }

        @Override
        protected String getButtonText(final States state) {
            return switch (state) {
            case READY -> "Login";
            case CANCEL -> "Cancel login";
            case DONE -> "Login again";
            default -> null;
            };
        }
    }

    void validate() throws InvalidSettingsException {
        if (m_clientSelection == ClientSelection.CUSTOM) {
            validateClientIdAndSecret();
            validateRedirectURL();
        } else if (m_authType == AuthType.CLIENT_CREDENTIALS) {
            validateClientIdAndSecret();
            CheckUtils.checkSetting(StringUtils.isNotEmpty(m_enterpriseId), "Enterprise ID is required");
        }
    }

    private void validateClientIdAndSecret() throws InvalidSettingsException {
        CheckUtils.checkSetting(StringUtils.isNotEmpty(m_boxApp.getUsername()), "Client/App ID is required");
        CheckUtils.checkSetting(StringUtils.isNotEmpty(m_boxApp.getPassword()), "Client/App secret is required");
    }

    private void validateRedirectURL() throws InvalidSettingsException {
        if (StringUtils.isBlank(m_redirectUrl)) {
            throw new InvalidSettingsException("Please specify the redirect URL");
        }

        try {
            var uri = new URI(m_redirectUrl);
            if (!Objects.equals(uri.getScheme(), "http") && !Objects.equals(uri.getScheme(), "https")) {
                throw new InvalidSettingsException("Redirect URL must start with http:// or https://.");
            }

            if (StringUtils.isBlank(uri.getHost())) {
                throw new InvalidSettingsException("Redirect URL must specify a host.");
            }
        } catch (URISyntaxException ex) {// NOSONAR
            throw new InvalidSettingsException("Please specify a valid redirect URL: " + ex.getMessage());
        }
    }

    static class LoginUpdateHandler extends CancelableActionHandler.UpdateHandler<UUID, BoxAuthenticatorSettings> {
    }

    /**
     * @return The redirect URL.
     */
    public String getRedirectURL() {
        if (m_clientSelection == ClientSelection.CUSTOM) {
            return m_redirectUrl;
        } else {
            return DEFAULT_REDIRECT_URL;
        }
    }

    /**
     * Creates {@link OAuth20Service} from the current settings.
     *
     * @return The {@link OAuth20Service} instance.
     * @throws InvalidSettingsException
     */
    OAuth20Service createService() {

        var username = DEFAULT_APP_ID;
        var password = DEFAULT_APP_SECRET; // NOSONAR

        if ((m_authType == AuthType.OAUTH && m_clientSelection == ClientSelection.CUSTOM)
                || m_authType == AuthType.CLIENT_CREDENTIALS) {
            username = m_boxApp.getUsername();
            password = m_boxApp.getPassword();
        }

        var builder = new CustomOAuth2ServiceBuilder(username)//
                .apiSecret(password);

        if (m_authType == AuthType.CLIENT_CREDENTIALS) {
            var subjectType = ENTERPRISE_SUBJECT_TYPE;
            var subjectId = m_enterpriseId;

            builder.additionalRequestBodyField(PARAM_BOX_SUBJECT_TYPE, subjectType);
            builder.additionalRequestBodyField(PARAM_BOX_SUBJECT_ID, subjectId);
        } else {
            builder.callback(getRedirectURL());
        }

        return builder.build(BOX_API);
    }
}
