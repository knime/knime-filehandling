/*
 * ------------------------------------------------------------------------
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
package org.knime.ext.ssh;

import java.security.GeneralSecurityException;

import org.apache.sshd.common.util.security.SecurityUtils;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * SSH bundle activator.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class SshPlugin extends Plugin {

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final var cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            // make sure that bouncycastle bcprov from the target platform is found and
            // used. Other plugins may contain duplicates of bcprov, and those duplicates
            // might get loaded by accident by the default TCCL. This may result in ED25519
            // keys not being loadable (see AP-25617)
            if (SecurityUtils.getEdDSASupport().isEmpty()) {
                throw new Exception(//
                        "Apache MINA SSHD cannot load EdDSA support. Is Bouncycastle bcprov available in the target "
                                + "platform?");
            }
            SecurityUtils.getKeyFactory(SecurityUtils.ED25519);
        } catch (GeneralSecurityException e) {
            throw new Exception(//
                    "Failed to load key factory for ED25519 keys. Is Bouncycastle bcprov available"
                    + " in the target platform?", e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }
}
