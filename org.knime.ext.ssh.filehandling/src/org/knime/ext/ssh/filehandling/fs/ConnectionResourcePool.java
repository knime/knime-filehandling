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
 *   2020-08-01 (Vyacheslav Soldatov): created
 */

package org.knime.ext.ssh.filehandling.fs;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.subsystem.sftp.SftpClient;
import org.apache.sshd.client.subsystem.sftp.SftpClientFactory;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;

/**
 * This is the simple resource pool implementation. Main idea is for the most of
 * time to have just one free resource. Possible states is 0 free resources or 1
 * free resource. When the <code>take</code> method is called it is there free
 * resource it is returned immediately. Otherwise the resource is created and
 * registered as busy. When resource releasing and if the list of free resources
 * already contains one free resource, the releasing resource is just
 * immediately closed. Otherwise it is placed to free resources. It allows to
 * have one free resource during the most of time.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 *
 */
public class ConnectionResourcePool implements SessionListener {
    private final LinkedList<ConnectionResource> m_freeResources = new LinkedList<>();
    private final Set<ConnectionResource> m_busyResources = new HashSet<>();

    private final SftpSessionFactory m_sessionFactory;
    private ClientSession m_session;

    private final int m_maxResourcesLimit;
    private final long m_connectionTimeOut;

    /**
     * @param settings
     *            SSH connection settings.
     */
    public ConnectionResourcePool(final SshConnectionConfiguration settings) {
        super();
        m_maxResourcesLimit = settings.getMaxSftpSessionLimit();
        m_connectionTimeOut = settings.getConnectionTimeout();

        m_sessionFactory = new SftpSessionFactory(settings);
    }

    /**
     * Marks resource as busy and returns it.
     * @return resource.
     * @throws IOException
     */
    public synchronized ConnectionResource take() throws IOException {
        long startTime = System.currentTimeMillis();

        while (true) {
            try {
                return takeImpl();
            } catch (ResourcesLimitExceedException ex) {
                long waitTime = m_connectionTimeOut - (System.currentTimeMillis() - startTime);
                if (waitTime > 0) {
                    try {
                        wait(waitTime);
                    } catch (InterruptedException ex1) {
                        throw new IOException("Thread interrupted", ex1);
                    }
                } else {
                    throw new IOException("Wait of resource time out exceed");
                }
            }
        }
    }

    private ConnectionResource takeImpl() throws IOException, ResourcesLimitExceedException {
        checkStarted();
        makeSureSessionOpened();

        ConnectionResource resource = null;
        if (!m_freeResources.isEmpty()) {
            resource = m_freeResources.removeFirst();
            m_busyResources.add(resource);
        }

        if (resource == null) {
            if (getNumResourcesInUse() < m_maxResourcesLimit) {
                resource = createResource();
                m_busyResources.add(resource);
            } else {
                throw new ResourcesLimitExceedException();
            }
        }

        return resource;
    }

    private int getNumResourcesInUse() {
        return m_busyResources.size() + m_freeResources.size();
    }

    private void makeSureSessionOpened() throws IOException {
        if (!m_session.isOpen()) {
            sessionClosed(m_session);
        }
    }

    /**
     * @return SSH connection resource.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    private ConnectionResource createResource() throws IOException {
        final SftpClient client = SftpClientFactory.instance().createSftpClient(m_session);
        return new ConnectionResource(client);
    }

    private void checkStarted() {
        if (m_session == null) {
            throw new IllegalStateException("Resource pool is not started");
        }
    }

    /**
     * Move resource as available again.
     * @param resource resource.
     */
    public synchronized void release(final ConnectionResource resource) {
        if (m_busyResources.remove(resource) && m_session != null && !resource.isClosed()) {
            // should attempt to keep one alive client in pool
            if (!m_freeResources.isEmpty()) {
                resource.close();
            } else {
                m_freeResources.addFirst(resource);
            }
        }
        // notify resource consumers if any waits it
        notifyAll();
    }

    /**
     * Notify resource pool resource should be closed immediately.
     *
     * @param resource
     *            resource.
     */
    public synchronized void forceClose(final ConnectionResource resource) {
        m_busyResources.remove(resource);
        m_freeResources.remove(resource);
        close(resource);

        // notify resource consumers if any waits it
        notifyAll();
    }


    private void closeSession() {
        if (m_session != null) {
            ClientSession session = m_session;
            m_session = null;
            try {
                session.close();
            } catch (final Throwable ex) {
            }
        }
    }

    /**
     * Starts resource pool.
     *
     * @throws IOException
     */
    public synchronized void start() throws IOException {
        m_sessionFactory.init();
        m_session = m_sessionFactory.createSession();
    }

    /**
     * Stops resource pool.
     */
    public synchronized void stop() {
        // close resources
        List<ConnectionResource> toClose = new LinkedList<>(m_freeResources);
        toClose.addAll(m_busyResources);
        m_freeResources.clear();
        m_busyResources.clear();

        for (ConnectionResource res : toClose) {
            close(res);
        }

        // close session
        closeSession();

        // destroy session factory
        m_sessionFactory.destroy();

        // notify resource consumers if any waits it
        notifyAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionDisconnect(final Session session, final int reason, final String msg, final String language,
            final boolean initiator) {
        sessionClosed(session);
    }

    @Override
    public synchronized void sessionClosed(final Session session) {
        session.removeSessionListener(this);

        if (m_session != null && m_session == session) { // is not closed by API
            m_freeResources.clear();
            m_busyResources.clear();

            try {
                m_session = m_sessionFactory.createSession();
            } catch (IOException ex) {
            }

            // notify resource consumers if any waits it
            notifyAll();
        }
    }

    /**
     * @param resource resource to close
     */
    private static void close(final ConnectionResource resource) {
        try {
            resource.close();
        } catch (final Throwable e) {
        }
    }

    /**
     * @param resource
     *            corrupted resource.
     */
    public void handleCorrupted(final ConnectionResource resource) {
        forceClose(resource);
    }
}
