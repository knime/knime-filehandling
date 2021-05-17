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
 *   2021-03-06 (Alexander Bondaletov): created
 */
package org.knime.ext.smb.filehandling.node;

import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.ext.smb.filehandling.fs.SmbFSConnection;
import org.knime.ext.smb.filehandling.fs.SmbFileSystem;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.kerberos.api.KerberosCallback;
import org.knime.kerberos.api.KerberosProvider;

import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.auth.GSSAuthenticationContext;

/**
 * Samba connector node.
 *
 * @author Alexander Bondaletov
 */
public class SmbConnectorNodeModel extends NodeModel {
    private static final String FILE_SYSTEM_NAME = "SMB";

    private String m_fsId;
    private SmbFSConnection m_fsConnection;

    /**
     * Creates new instance.
     */
    protected SmbConnectorNodeModel() {
        super(new PortType[] {}, new PortType[] { FileSystemPortObject.TYPE });
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        String host = System.getProperty("smb.host");
        String share = System.getProperty("smb.share");

        m_fsId = FSConnectionRegistry.getInstance().getKey();
        return new PortObjectSpec[] { createSpec(host, share) };
    }

    private FileSystemPortObjectSpec createSpec(final String host, final String share) {
        return new FileSystemPortObjectSpec(FILE_SYSTEM_NAME, m_fsId,
                SmbFileSystem.createFSLocationSpec(host, share));
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        String host = System.getProperty("smb.host");
        String share = System.getProperty("smb.share");

        m_fsConnection = new SmbFSConnection(SmbFileSystem.PATH_SEPARATOR, host, share, createAuthContext(exec));
        FSConnectionRegistry.getInstance().register(m_fsId, m_fsConnection);

        return new PortObject[] { new FileSystemPortObject(createSpec(host, share)) };
    }

    private static AuthenticationContext createAuthContext(final ExecutionMonitor exec) throws Exception {
        boolean useKerberos = Boolean.valueOf(System.getProperty("smb.kerberos", "false"));
        if (useKerberos) {
            return createKerberosAuthContext(exec);
        } else {
            return createUsernamePasswordAuthContext();
        }
    }

    private static AuthenticationContext createUsernamePasswordAuthContext() {
        String username = System.getProperty("smb.username");
        String password = System.getProperty("smb.password");

        return new AuthenticationContext(username, password.toCharArray(), "");
    }

    private static AuthenticationContext createKerberosAuthContext(final ExecutionMonitor exec) throws Exception {
        KerberosProvider.ensureInitialized();
        return KerberosProvider.doWithKerberosAuthBlocking(new SmbKerberosCallback(), exec);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // TODO Auto-generated method stub

    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // TODO Auto-generated method stub

    }

    @Override
    protected void reset() {
        // TODO Auto-generated method stub

    }

    private static class SmbKerberosCallback implements KerberosCallback<GSSAuthenticationContext> {

        private static final String SPNEGO_OID = "1.3.6.1.5.5.2";
        private static final String KERBEROS5_OID = "1.2.840.113554.1.2.2";

        @Override
        public GSSAuthenticationContext doAuthenticated() throws Exception {
            Subject subject = Subject.getSubject(AccessController.getContext());

            KerberosPrincipal krbPrincipal = subject.getPrincipals(KerberosPrincipal.class).iterator().next();

            Oid spnego = new Oid(SPNEGO_OID);
            Oid kerberos5 = new Oid(KERBEROS5_OID);

            final GSSManager manager = GSSManager.getInstance();

            final GSSName name = manager.createName(krbPrincipal.toString(), GSSName.NT_USER_NAME);
            Set<Oid> mechs = new HashSet<>(Arrays.asList(manager.getMechsForName(name.getStringNameType())));
            final Oid mech;
            if (mechs.contains(kerberos5)) {
                mech = kerberos5;
            } else if (mechs.contains(spnego)) {
                mech = spnego;
            } else {
                throw new IllegalArgumentException("No mechanism found");
            }

            GSSCredential creds = manager.createCredential(name, GSSCredential.DEFAULT_LIFETIME, mech,
                            GSSCredential.INITIATE_ONLY);

            return new GSSAuthenticationContext(krbPrincipal.getName(), krbPrincipal.getRealm(), subject, creds);
        }

    }
}
