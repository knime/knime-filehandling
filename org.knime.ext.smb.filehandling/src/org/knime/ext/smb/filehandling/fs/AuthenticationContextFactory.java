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
 *   2021-06-01 (bjoern): created
 */
package org.knime.ext.smb.filehandling.fs;

import java.io.IOException;
import java.security.AccessController;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.filehandling.core.connections.base.auth.StandardAuthTypes;
import org.knime.kerberos.api.KerberosDelegationProvider;
import org.knime.kerberos.api.KerberosProvider;

import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.auth.GSSAuthenticationContext;

final class AuthenticationContextFactory {

    private AuthenticationContextFactory() {
    }

    static AuthenticationContext create(final SmbFSConnectionConfig config, final ExecutionContext exec)
            throws IOException {

        if (config.getAuthType() == StandardAuthTypes.USER_PASSWORD) {
            return createUsernamePasswordAuthContext(config.getUser(), config.getPassword());
        } else if (config.getAuthType() == SmbFSConnectionConfig.KERBEROS_AUTH_TYPE) {
            return createKerberosAuthContext(exec);
        } else if (config.getAuthType() == SmbFSConnectionConfig.GUEST_AUTH_TYPE) {
            return AuthenticationContext.guest();
        } else if (config.getAuthType() == StandardAuthTypes.ANONYMOUS) {
            return AuthenticationContext.anonymous();
        } else {
            throw new IllegalArgumentException("Unsupported authentication type: " + config.getAuthType().getText());
        }
    }

    private static AuthenticationContext createUsernamePasswordAuthContext(final String user, final String password) {
        if (isValidSAMAccountName(user)) {
            return createAuthContextforSAMAccountName(user, password);
        } else if (isValidPrincipal(user)) {
            return createAuthContextforPrincipal(user, password);
        } else {
            return new AuthenticationContext(user, password.toCharArray(), null);
        }
    }

    private static boolean isValidSAMAccountName(final String user) {
        final int separatorIdx = user.indexOf('\\');
        return separatorIdx > 0 && separatorIdx < user.length() - 1;
    }

    private static AuthenticationContext createAuthContextforSAMAccountName(final String samAccountName,
            final String password) {
        final int separatorIdx = samAccountName.indexOf('\\');

        final String domain = samAccountName.substring(0, separatorIdx);
        final String user = samAccountName.substring(separatorIdx + 1);

        return new AuthenticationContext(user, password.toCharArray(), domain);
    }

    private static boolean isValidPrincipal(final String user) {
        final int separatorIdx = user.indexOf('@');
        return separatorIdx > 0 && separatorIdx < user.length() - 1;
    }

    private static AuthenticationContext createAuthContextforPrincipal(final String principal, final String password) {
        final int separatorIdx = principal.indexOf('@');

        final String user = principal.substring(0, separatorIdx);
        final String domain = principal.substring(separatorIdx + 1);

        return new AuthenticationContext(user, password.toCharArray(), domain);
    }

    private static AuthenticationContext createKerberosAuthContext(final ExecutionMonitor exec) throws IOException {
        KerberosProvider.ensureInitialized();

        try {
            return KerberosDelegationProvider
                    .doWithConstrainedDelegationBlocking(cred -> {
                        final Subject subject = Subject.getSubject(AccessController.getContext());
                        final KerberosPrincipal krbPrincipal =  subject//
                                .getPrincipals(KerberosPrincipal.class) //
                                .iterator() //
                                .next();

                        return new GSSAuthenticationContext(krbPrincipal.getName(), krbPrincipal.getRealm(), subject,
                                cred);
                    }, exec);

        } catch (Exception ex) {
            // rethrow as IOE because in the file system constructor we are only allowed to
            // throw IOE
            throw new IOException(ex.getMessage(), ex);
        }
    }
}
