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
 *   2020-10-02 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.fs;

import java.util.HashMap;
import java.util.Map;

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;

/**
 * Memory based user manager implementation {@link UserManager}
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
class InMemoryUserManager implements UserManager {
    private final Map<String, User> m_users = new HashMap<>();

    /**
     * Anonymous user name.
     */
    public static final String ANONYMOUS = "anonymous";
    /**
     * Big JUnit boss.
     */
    public static final String ADMIN = "bigJunitBoss";

    /**
     * {@inheritDoc}
     */
    @Override
    public User getUserByName(final String username) throws FtpException {
        return m_users.get(username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getAllUserNames() throws FtpException {
        return m_users.keySet().toArray(new String[m_users.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final String username) throws FtpException {
        m_users.remove(username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final User user) throws FtpException {
        m_users.put(user.getName(), user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean doesExist(final String username) throws FtpException {
        return m_users.containsKey(username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public User authenticate(final Authentication auth) throws AuthenticationFailedException {
        if (auth instanceof AnonymousAuthentication) {

            final User user = m_users.get(ANONYMOUS);
            if (user == null) {
                throw new AuthenticationFailedException(ANONYMOUS);
            }
            return user;
        } else if (auth instanceof UsernamePasswordAuthentication) {

            final UsernamePasswordAuthentication userPassword = (UsernamePasswordAuthentication) auth;
            final User user = m_users.get(userPassword.getUsername());
            if (user == null || !user.getPassword().endsWith(userPassword.getPassword())) {
                throw new AuthenticationFailedException("Incorrect user name or password");
            }
            return user;
        } else {
            throw new AuthenticationFailedException("Unexpected authentication type " + auth.getClass().getName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAdminName() throws FtpException {
        return ADMIN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAdmin(final String username) throws FtpException {
        return getAdminName().equals(username);
    }
}
