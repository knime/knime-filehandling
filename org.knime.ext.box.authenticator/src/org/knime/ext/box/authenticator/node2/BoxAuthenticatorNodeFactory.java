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

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.knime.credentials.base.CredentialPortObject;

/**
 * Node factory for the Box Authenticator node.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
@SuppressWarnings("restriction")
public class BoxAuthenticatorNodeFactory extends WebUINodeFactory<BoxAuthenticatorNodeModel> {
    private static final String FULL_DESCRIPTION = """
                    <p>This node authenticates against <a href="https://box.com/">Box</a>. The resulting credential
                    can then be used with the <a href="https://hub.knime.com/n/iICKTxfnHis42DpH">Box Connector node.</a>
                    The following authentication methods are supported:
                    <ul>

                    <li><a href="https://developer.box.com/guides/authentication/oauth2/">User authentication (OAuth
                    2.0)</a>
                    </li>

                    <li><a href="https://developer.box.com/guides/authentication/client-credentials/">
                        Server authentication (Client Credentials Grant)</a>
                    </li>
                    </ul>
                    We recommend to use User authentication for interactive workflow building and Server authentication
                    for <a href="https://docs.knime.com/latest/business_hub_deployments_guide/index.html">deployed workflows</a>
                    (e.g. schedules or data apps). For further details on which authentication method to select see the
                    <a href="https://developer.box.com/guides/authentication/select/">Box documentation.</a>
                    </p>

                    <p>
                    To use <i>User authentication</i>, you can either use the Default App or create and use
                    a Custom App. The Default App is provided by KNIME via the
                    <a href="https://cloud.app.box.com/integrations/knime_analytics_platform/i/arZJuPwEfy">Box Marketplace.</a>
                    To use the Default App simply select Default as client/app and, when asked, grant the
                    KNIME Analytics Platform access to Box.

                    To use a Custom App you need to first create and approve a Custom App in Box.
                    The steps are as follows:
                    <ul>

                    <li>Create a <a href="https://developer.box.com/guides/applications/custom-apps/">Custom App</a> in
                    Box. While creating the app choose OAuth 2.0 as the authentication method.
                    </li>

                    <li>
                    Configure the app as follows: Set <b>OAuth 2.0 Redirect URI = http://localhost</b>,
                    and <b>Application Scopes = Write all files and folders stored in Box.</b>
                    </li>

                    <li>Request approval of your app
                    <a href="https://developer.box.com/guides/authorization/custom-app-approval/"> as described here.
                    </a>
                    </li>

                    <li>As soon as a Box admin has approved your app it can be used in KNIME.</li>
                    </ul>
                    </p>

                    <p>
                    To use <i>Server authentication</i>, you need to first create and approve a Custom App in Box.
                    The steps are as follows:
                    <ul>

                    <li>Create a <a href="https://developer.box.com/guides/applications/custom-apps/">Custom App</a> in
                    Box. While creating the app choose Client Credentials Grant as the authentication method.
                    </li>

                    <li>
                    Configure the app as follows: Set <b>Access Level = App + Enterprise
                    Access</b> (to be able to access files/folders of your Box enterprise), and <b>Application Scopes =
                    Write all files and folders stored in Box.</b>
                    </li>

                    <li>Request approval of your app
                    <a href="https://developer.box.com/guides/authorization/custom-app-approval/"> as described here.
                    </a>
                    </li>

                    <li>As soon as a Box admin has approved your app it can be used in KNIME.</li>
                    </ul>
                    </p>
                    <p>
                    <b>Note:</b> If <i>User authentication</i> is selected, the node can currently only be executed in
                    KNIME Analytics Platform, i.e. execution on KNIME Hub, KNIME Server, or via Remote
                    Workflow Editor will fail. Authentication via <i>Server authentication</i> works in all cases.

                    If you have a KNIME Hub account you can centrally manage and share your Box secrets. For further
                    details see the
                    <a href="https://docs.knime.com/latest/business_hub_secrets_guide/index.html#secrets">KNIME Secrets User Guide.</a>
                    </p>
            """;

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder()//
            .name("Box Authenticator")//
            .icon("./oauth.png")//
            .shortDescription("Box Authenticator node.")//
            .fullDescription(FULL_DESCRIPTION) //
            .modelSettingsClass(BoxAuthenticatorSettings.class)//
            .addOutputPort("Credential", CredentialPortObject.TYPE, "Box credential (access token).")//
            .sinceVersion(5, 1, 0)//
            .build();

    /**
     * Creates new instance
     */
    public BoxAuthenticatorNodeFactory() {
        super(CONFIGURATION);
    }

    @Override
    public BoxAuthenticatorNodeModel createNodeModel() {
        return new BoxAuthenticatorNodeModel(CONFIGURATION);
    }

}
