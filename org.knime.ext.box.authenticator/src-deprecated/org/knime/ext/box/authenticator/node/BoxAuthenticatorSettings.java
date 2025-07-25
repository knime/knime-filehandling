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

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.CancelableActionHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.credentials.base.CredentialCache;
import org.knime.credentials.base.oauth.api.nodesettings.AbstractTokenCacheKeyPersistor;
import org.knime.credentials.base.oauth.api.scribejava.CustomApi20;
import org.knime.credentials.base.oauth.api.scribejava.CustomOAuth2ServiceBuilder;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.oauth2.clientauthentication.RequestBodyAuthenticationScheme;

/**
 * Settings class for the Box Authenticator node.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
@SuppressWarnings("restriction")
public class BoxAuthenticatorSettings implements NodeParameters {

    private static final NodeLogger LOG = NodeLogger.getLogger(BoxAuthenticatorSettings.class);

    private static final String PARAM_BOX_SUBJECT_TYPE = "box_subject_type";
    private static final String PARAM_BOX_SUBJECT_ID = "box_subject_id";
    private static final String ENTERPRISE_SUBJECT_TYPE = "enterprise";

    private static final CustomApi20 BOX_API = new CustomApi20("https://api.box.com/oauth2/token", //
            "https://account.box.com/api/oauth2/authorize", //
            Verb.POST, //
            RequestBodyAuthenticationScheme.instance());

    @Section(title = "Box App")
    interface BoxAppSection {
    }

    @Section(title = "Authentication method")
    @After(BoxAppSection.class)
    interface AuthenticationSection {
        interface TypeSwitcher {
        }

        @After(TypeSwitcher.class)
        interface Body {
        }
    }

    enum AuthType {
        @Label("OAuth 2")
        OAUTH,

        @Label("Client credentials")
        CLIENT_CREDENTIALS;
    }

    interface AuthTypeRef extends ParameterReference<AuthType> {
    }

    static class AuthTypeIsClientCreds implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(AuthTypeRef.class).isOneOf(AuthType.CLIENT_CREDENTIALS);
        }
    }

    @Layout(BoxAppSection.class)
    BoxAppSettings m_boxApp = new BoxAppSettings();

    @Widget(title = "Type", description = "Authentication method to use.")
    @Layout(AuthenticationSection.TypeSwitcher.class)
    @ValueReference(AuthTypeRef.class)
    @ValueSwitchWidget
    AuthType m_authType = AuthType.OAUTH;

    @Widget(title = "Enterprise ID", description = """
            The Box Enterprise ID when authenticating as a
            <a href="https://developer.box.com/guides/getting-started/user-types/service-account/">
            service account</a>.
            """)
    @Layout(AuthenticationSection.Body.class)
    @Effect(predicate = AuthTypeIsClientCreds.class, type = EffectType.SHOW)
    String m_enterpriseId;

    @Widget(title = "Redirect URL (should be http://localhost:XXXXX)", description = """
            The redirect URL to be used at the end of the interactive login. Should be chosen as http://localhost:XXXXX
            where XXXXX is a random number in the 10000 - 65000 range to avoid conflicts. The redirect URL is part of
            the App configuration in Box.
            """)
    @Layout(AuthenticationSection.Body.class)
    @Effect(predicate = AuthTypeIsClientCreds.class, type = EffectType.HIDE)
    String m_redirectUrl = "http://localhost:33749/";

    @ButtonWidget(actionHandler = LoginActionHandler.class, //
            updateHandler = LoginUpdateHandler.class, //
            showTitleAndDescription = false)
    @Widget(title = "Login", //
            description = "Clicking on login opens a new browser window/tab which "
                    + "allows to interactively log into Box.")
    @Persistor(TokenCacheKeyPersistor.class)
    @Layout(AuthenticationSection.Body.class)
    @Effect(predicate = AuthTypeIsClientCreds.class, type = EffectType.HIDE)
    UUID m_tokenCacheKey;

    static final class TokenCacheKeyPersistor extends AbstractTokenCacheKeyPersistor {
        TokenCacheKeyPersistor() {
            super("tokenCacheKey");
        }
    }

    static class LoginActionHandler extends CancelableActionHandler<UUID, BoxAuthenticatorSettings> {

        @Override
        protected UUID invoke(final BoxAuthenticatorSettings settings, final NodeParametersInput context)
                throws WidgetHandlerException {

            try {
                settings.validateOnExecute(((NodeParametersInputImpl) context).getCredentialsProvider().orElseThrow());
            } catch (InvalidSettingsException e) { // NOSONAR
                throw new WidgetHandlerException(e.getMessage());
            }

            final var credentialsProvider = ((NodeParametersInputImpl) context).getCredentialsProvider().orElseThrow();

            try {
                return CredentialCache
                        .store(ScribeJavaHelper.fetchCredentialViaAuthCodeFlow(credentialsProvider, settings));
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

    static class LoginUpdateHandler extends CancelableActionHandler.UpdateHandler<UUID, BoxAuthenticatorSettings> {
    }

    void validateOnConfigure(final CredentialsProvider credentialsProvider) throws InvalidSettingsException {
        validate();
        m_boxApp.validateOnConfigure(credentialsProvider);
    }

    void validateOnExecute(final CredentialsProvider credentialsProvider) throws InvalidSettingsException {
        validate();
        m_boxApp.validateOnExecute(credentialsProvider);

    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_authType == AuthType.CLIENT_CREDENTIALS) {
            CheckUtils.checkSetting(StringUtils.isNotEmpty(m_enterpriseId), "Enterprise ID is required");
        } else {
            CheckUtils.checkSetting(StringUtils.isNotEmpty(m_redirectUrl), "Redirect URL is required");
        }
    }

    /**
     * Creates {@link OAuth20Service} from the current settings.
     *
     * @return The {@link OAuth20Service} instance.
     */
    OAuth20Service createService(final CredentialsProvider credsProvider) {

        var builder = new CustomOAuth2ServiceBuilder(m_boxApp.login(credsProvider))//
                .apiSecret(m_boxApp.secret(credsProvider));

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
