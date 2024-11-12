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
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.exception.SshChannelOpenException;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.knime.core.node.NodeLogger;

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

    private static final NodeLogger LOG = NodeLogger.getLogger(ConnectionResourcePool.class);

    private final LinkedList<ConnectionResource> m_freeResources = new LinkedList<>();
    private final Set<ConnectionResource> m_busyResources = new HashSet<>();

    private final Set<ChannelExec> m_currentExecChannels = new HashSet<>();

    private final SftpSessionFactory m_sessionFactory;
    private ClientSession m_session;

    private final int m_maxResourcesLimit;
    private final int m_maxExecChannelLimit;
    private final Duration m_connectionTimeOut;

    /**
     * @param settings
     *            SSH connection settings.
     */
    public ConnectionResourcePool(final SshFSConnectionConfig settings) {
        super();
        m_maxResourcesLimit = settings.getMaxSftpSessionLimit();
        m_maxExecChannelLimit = settings.getMaxExecChannelLimit();
        m_connectionTimeOut = settings.getConnectionTimeout();

        m_sessionFactory = new SftpSessionFactory(settings);
    }

    /**
     * Marks resource as busy and returns it.
     * @return resource.
     * @throws IOException
     */
    public synchronized ConnectionResource take() throws IOException {
        return take(this::takeImpl, m_connectionTimeOut, "Wait of resource time out exceed");
    }

    /**
     * Try to create a new execution channel within a given timeout but do not open
     * it.
     *
     * @param command
     *            the command to execute
     * @param encoding
     *            the encoding in which to send the command
     * @param timeOut
     *            the maximum time out to wait for the resource to be created and
     *            the connection to be established. A duration of {@code null} means
     *            no time out
     * @return the newly created execution channel
     * @throws IOException
     */
    public synchronized ChannelExec takeExecChannel(final String command, final Charset encoding,
            final Duration timeOut) throws IOException {
        return take(() -> takeExecChannelImpl(command, encoding), timeOut,
                "Waiting for shell session timed out. "
                        + "Please consider decreasing the maximum SFTP sessions or increasing the maximum "
                        + "shell sessions in the SSH Connector.");
    }

    private synchronized <R> R take(final TakeSupplier<R> supplier, final Duration timeout, final String timeoutMessage)
            throws IOException {
        if (timeout == null || timeout.isZero()) {
            return takeWithoutTimeout(supplier);
        } else {
            return takeWithTimeout(supplier, timeout.toMillis(), timeoutMessage);
        }
    }

    private synchronized <R> R takeWithoutTimeout(final TakeSupplier<R> supplier) throws IOException {
        while (true) {
            try {
                return supplier.take();
            } catch (ResourcesLimitExceedException ignored) { // NOSONAR used for waiting
                try {
                    wait();
                } catch (InterruptedException iex) { // NOSONAR
                    throw new IOException("Thread interrupted", iex);
                }
            }
        }
    }

    private synchronized <R> R takeWithTimeout(final TakeSupplier<R> supplier, final long millis,
            final String timeoutMessage) throws IOException {
        long startTime = System.currentTimeMillis();

        while (true) {
            try {
                return supplier.take();
            } catch (ResourcesLimitExceedException ignored) { // NOSONAR used for waiting
                long waitTime = millis - (System.currentTimeMillis() - startTime);
                if (waitTime > 0) {
                    try {
                        wait(waitTime);
                    } catch (InterruptedException iex) { // NOSONAR
                        Thread.currentThread().interrupt();
                        throw new IOException("Thread interrupted", iex);
                    }
                } else {
                    throw new IOException(timeoutMessage);
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
            throw new ResourcesLimitExceedException();
        }

        return resource;
    }

    private ChannelExec takeExecChannelImpl(final String command, final Charset encoding)
            throws IOException, ResourcesLimitExceedException {
        checkStarted();
        makeSureSessionOpened();

        if (m_currentExecChannels.size() < m_maxExecChannelLimit) {
            final var newChan = createExecChannel(command, encoding);
            m_currentExecChannels.add(newChan);
            return newChan;
        }

        throw new ResourcesLimitExceedException();
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

    /**
     * @return SSH execution channel resource.
     * @throws IOException
     */
    private ChannelExec createExecChannel(final String command, final Charset encoding) throws IOException {
        return m_session.createExecChannel(command, encoding, null, null);
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
        m_busyResources.remove(resource);
        if (m_session != null && !resource.isClosed()) {
            m_freeResources.add(resource);
        }

        // notify resource consumers if any waits it
        notifyAll();
    }

    /**
     * Remove a channel from the open channel list and closes it
     *
     * @param channel
     *            the channel.
     */
    public synchronized void release(final ChannelExec channel) {
        m_currentExecChannels.remove(channel);
        close(channel);

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

        for (int i = 0; i < m_maxResourcesLimit; i++) {
            try {
                m_freeResources.add(createResource());
            } catch (SshChannelOpenException e) {
                LOG.warn(String.format(
                        "Failed to create %d-th SFTP session (%d sessions already opened). Please consider decreasing the maximum SFTP sessions.",
                        i + 1, i), e);
                break;
            }
        }
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

        // close execution channels
        List<ChannelExec> channelsToClose = new LinkedList<>(m_currentExecChannels);
        m_currentExecChannels.clear();

        for (var chan : channelsToClose) {
            close(chan);
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
            m_currentExecChannels.clear();

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
     * @param channel
     *            channel to close
     */
    private static void close(final ChannelExec channel) {
        try {
            channel.close();
        } catch (final Throwable e) { // NOSONAR
        }
    }

    @FunctionalInterface
    private interface TakeSupplier<R> {
        R take() throws IOException, ResourcesLimitExceedException;
    }
}
