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
 *   2020-10-03 (Vyacheslav Soldatov): created
 */

package org.knime.ext.ftp.filehandling.fs;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.knime.core.node.NodeLogger;

/**
 * Resource pool implementation.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 *
 */
public class ClientPool {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ClientPool.class);

    private final LinkedList<FtpClientResource> m_freeResources;

    private final Set<FtpClientResource> m_busyResources;

    private final FtpClientFactory m_clientFactory;

    private final FtpConnectionConfiguration m_configuration;

    private final AtomicBoolean m_isStarted = new AtomicBoolean();

    private List<ScheduledFuture<?>> m_scheduledTasks = new LinkedList<>();

    /**
     * @param cfg
     *            FTP connection configuration.
     */
    public ClientPool(final FtpConnectionConfiguration cfg) {
        this(cfg, new LinkedList<>(), new HashSet<>(), new FtpClientFactory(cfg));
    }

    /**
     * @param cfg
     *            FTP connection configuration.
     * @param freeResources
     *            free resources container.
     * @param busyResources
     *            busy resources container.
     * @param clientFactory
     *            client factory.
     */
    protected ClientPool(final FtpConnectionConfiguration cfg, //
            final LinkedList<FtpClientResource> freeResources,
            final Set<FtpClientResource> busyResources, final FtpClientFactory clientFactory) {
        super();
        this.m_freeResources = freeResources;
        this.m_busyResources = busyResources;

        m_configuration = cfg;
        m_clientFactory = clientFactory;
    }

    /**
     * Marks resource as busy and returns it.
     *
     * @return resource.
     * @throws IOException
     * @throws InterruptedException
     */
    public synchronized FtpClientResource take() throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        final Duration timeout = m_configuration.getConnectionTimeOut();

        while (true) {
            try {
                return takeImpl();
            } catch (ResourcesLimitExceedException ex) { // NOSONAR is correct if not available resources
                long waitTime = timeout.toMillis() - (System.currentTimeMillis() - startTime);
                if (waitTime > 0) {
                    wait(waitTime);
                } else {
                    throw new IOException("Wait of resource time out exceeded");
                }
            }
        }
    }

    private FtpClientResource takeImpl() throws ResourcesLimitExceedException {
        checkStarted();

        FtpClientResource resource = null;
        if (!m_freeResources.isEmpty()) {
            resource = m_freeResources.removeFirst();
        } else if (getNumResources() < m_configuration.getMaxConnectionPoolSize()) {
            try {
                resource = createResource(true);
            } catch (IOException ex) { // NOSONAR this exception is impossible because lazy initialization
                // nothing
            }
        }

        if (resource == null) {
            throw new ResourcesLimitExceedException();
        } else {
            m_busyResources.add(resource);
        }

        return resource;
    }

    private int getNumResources() {
        return m_busyResources.size() + m_freeResources.size();
    }

    private void checkStarted() {
        if (!m_isStarted.get()) {
            throw new IllegalStateException("Resource pool is not started");
        }
    }

    /**
     * Move resource as available again.
     * @param resource resource.
     */
    public synchronized void release(final FtpClientResource resource) {

        final boolean wasBusy = m_busyResources.remove(resource);
        if (wasBusy && m_isStarted.get()) {
            if (getNumResources() + 1 > m_configuration.getCoreConnectionPoolSize()) {
                // close resource immediately and not return it into pool
                resource.close();
            } else {
                addToFreeResources(resource);
            }
        }

        // notify resource consumers if any waits it
        notifyAll();
    }

    /**
     * Starts resource pool.
     *
     * @throws IOException
     */
    public synchronized void start() throws IOException {
        m_isStarted.set(true);

        IOException exc = null;
        final int len = Math.max(1, m_configuration.getMinConnectionPoolSize());

        for (int i = 0; i < len; i++) {
            try {
                addToFreeResources(createResource(false));
            } catch (IOException e) {
                exc = e;
                LOGGER.warn(String.format(
                        "Failed to create %d-th FTP session (%d sessions already opened). Please consider decreasing the maximum FTP sessions.",
                        i + 1, i), e);
                break;
            }
        }

        if (m_freeResources.isEmpty()) {
            LOGGER.error("Failed to initialize FTP client pool", exc);
            if (exc != null) {
                throw exc;
            }
        }

        // if and only if resource pool is initialized should
        // start resource idle time handler
        startTasks();
    }

    private void startTasks() {
        final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);

        // start and save scheduled tasks
        m_scheduledTasks.add(scheduler.scheduleAtFixedRate(this::processIdleResources, 3, 3, TimeUnit.SECONDS));

        // start connection keep alive task
        long keepAliveTimeOut = m_configuration.getConnectionTimeOut().toMillis() / 2;
        if (keepAliveTimeOut > 0) {
            m_scheduledTasks.add(scheduler.scheduleAtFixedRate(this::sendKeepAlive, keepAliveTimeOut, keepAliveTimeOut,
                    TimeUnit.MILLISECONDS));
        }
    }

    private void cancelTasks() {
        while (!m_scheduledTasks.isEmpty()) {
            m_scheduledTasks.remove(0).cancel(false);
        }
    }

    /**
     * Stops resource pool.
     */
    public synchronized void stop() {
        cancelTasks();

        // close resources
        List<FtpClientResource> toClose = new LinkedList<>(m_freeResources);
        toClose.addAll(m_busyResources);
        m_freeResources.clear();
        m_busyResources.clear();

        for (FtpClientResource res : toClose) {
            res.close();
        }

        // close session
        m_isStarted.set(false);

        // notify resource consumers if any waits it
        notifyAll();
    }

    /**
     * @param resource
     *            resource to release.
     */
    private void addToFreeResources(final FtpClientResource resource) {
        resource.setAsFreeOn(System.currentTimeMillis());
        m_freeResources.add(resource);
    }

    /**
     * Have protected modifier for make accessible in unit test.
     */
    protected synchronized void processIdleResources() {
        final List<FtpClientResource> toClose = new LinkedList<>();
        final long currentTime = System.currentTimeMillis();

        int numMoreThanMin = getNumResources() - m_configuration.getMinConnectionPoolSize();
        final Iterator<FtpClientResource> iter = m_freeResources.iterator();

        while (iter.hasNext() && numMoreThanMin > 0) {
            final FtpClientResource next = iter.next();

            // just if idle time expired, add resource to closing list.
            if (currentTime - next.getAsFreeOn() > m_configuration.getMaxIdleTime()) {
                toClose.add(next);
                iter.remove();

                numMoreThanMin--;
            }
        }

        for (FtpClientResource res : toClose) {
            res.close();
        }
    }

    private synchronized void sendKeepAlive() {
        final Iterator<FtpClientResource> iter = m_freeResources.iterator();

        while (iter.hasNext()) {
            final FtpClientResource next = iter.next();
            try {
                next.get().sendKeepAlive();
            } catch (IOException ex) {
                LOGGER.error("Keep alive request failed. Connection closed", ex);
                iter.remove();
                next.close();
            }
        }
    }

    /**
     * @return FTP client resource.
     * @throws IOException
     */
    private FtpClientResource createResource(final boolean lazyInitialize) throws IOException {
        if (lazyInitialize) {
            return new FtpClientResource(m_clientFactory);
        } else {
            return new FtpClientResource(m_clientFactory.createClient());
        }
    }
}
