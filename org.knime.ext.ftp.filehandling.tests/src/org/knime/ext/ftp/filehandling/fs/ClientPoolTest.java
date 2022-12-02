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
 *   2020-10-20 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.knime.filehandling.core.connections.meta.base.BaseFSConnectionConfig.BrowserRelativizationBehavior;

/**
 * Unit tests for {@link ClientPool}
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class ClientPoolTest extends ClientPool {
    private FtpFSConnectionConfig m_config;
    private LinkedList<FtpClientResource> m_freeResources;
    private Set<FtpClientResource> m_busyResources;
    private final MockFtpClientFactory m_clientFactory;

    /**
     * Default constructor.
     */
    public ClientPoolTest() {
        this(new FtpFSConnectionConfig("/", BrowserRelativizationBehavior.ABSOLUTE), new LinkedList<>(),
                new HashSet<>(), new MockFtpClientFactory());
    }

    private ClientPoolTest(final FtpFSConnectionConfig cfg, final LinkedList<FtpClientResource> freeResources,
            final Set<FtpClientResource> busyResources, final MockFtpClientFactory clientFactory) {
        super(cfg, freeResources, busyResources, clientFactory);
        m_config = cfg;
        m_freeResources = freeResources;
        m_busyResources = busyResources;
        m_clientFactory = clientFactory;
    }

    /**
     * Tests minimum pool size configuration settings.
     *
     * @throws IOException
     */
    @Test
    public void testMinPoolSize() throws IOException {
        final int minPoolSize = 10;

        setPoolSizes(minPoolSize, minPoolSize + 10, minPoolSize + 20);
        // set idle time to zero.
        m_config.setMaxIdleTime(0);

        // add client resources
        final long releaseTime = System.currentTimeMillis() - 1;
        for (int i = 0; i < minPoolSize + 1; i++) {
            addFreeResource(new FtpClientResource(m_clientFactory.createClient()), releaseTime);
        }

        // process idle resources
        processIdleResources();

        // make sure the resource pool is not less then minimum
        assertEquals(minPoolSize, m_freeResources.size());
        assertEquals(0, m_busyResources.size());
    }

    /**
     * Tests minimum pool size configuration settings.
     *
     * @throws IOException
     */
    @Test
    public void testMinPoolSizeOnStart() throws IOException {
        final int minPoolSize = 10;

        setPoolSizes(minPoolSize, minPoolSize + 10, minPoolSize + 20);
        // set any very big idle time
        m_config.setMaxIdleTime(1000000l);
        start();

        // make sure the resource pool is not less then minimum
        assertEquals(minPoolSize, m_freeResources.size());
    }

    /**
     * Tests core pool size configuration settings.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testCorePoolSize() throws IOException, InterruptedException {
        final int minPoolSize = 10;

        setPoolSizes(minPoolSize, minPoolSize + 10, minPoolSize + 15);
        m_config.setMaxIdleTime(1000000000l);
        start();

        // move all free resources to busy
        while (!m_freeResources.isEmpty()) {
            m_busyResources.add(m_freeResources.remove());
        }

        take();
        // test this resource is additionally created
        assertEquals(minPoolSize + 1, m_freeResources.size() + m_busyResources.size());
    }

    /**
     * Test just one resource initialized when pool started.
     *
     * @throws IOException
     */
    @Test
    public void testInitialPoolSizeIsMoreThenZero() throws IOException {
        setPoolSizes(0, 100, 100);
        // set any big max idle time
        m_config.setMaxIdleTime(1000000000l);

        start();
        assertEquals(1, m_freeResources.size());
    }

    /**
     * Tests processIdleResources method
     *
     * @throws IOException
     */
    @Test
    public void testProcessIdleResources() throws IOException {
        setPoolSizes(0, 1, 2);
        final long maxIdleTime = 60000l;
        m_config.setMaxIdleTime(maxIdleTime);

        final long releaseTime = System.currentTimeMillis();

        addFreeResource(new FtpClientResource(m_clientFactory.createClient()), releaseTime);
        addFreeResource(new FtpClientResource(m_clientFactory.createClient()), releaseTime);

        processIdleResources();
        assertEquals(2, m_freeResources.size());

        // shift released time of one resource
        m_freeResources.get(0).setAsFreeOn(releaseTime - maxIdleTime - 1l);

        processIdleResources();
        assertEquals(1, m_freeResources.size());
    }

    /**
     * This test uses 2 seconds time out therefore uncomment it just when really
     * need to test the resource time out and comment then again.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    // @Test
    public void testProcessIdleResourcesByScheduledTask() throws IOException, InterruptedException {
        setPoolSizes(0, 1, 2);
        final long maxIdleTime = 3000l;
        m_config.setMaxIdleTime(maxIdleTime);

        final long releaseTime = System.currentTimeMillis();

        addFreeResource(new FtpClientResource(m_clientFactory.createClient()), releaseTime);
        start();

        Thread.sleep(2 * maxIdleTime + 1); // NOSONAR this method will disabled after local test
        assertEquals(0, m_freeResources.size());
    }

    /**
     * This test uses 2 seconds time out therefore uncomment it just when really
     * need to test the resource time out and comment then again.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    // @Test(expected = IOException.class)
    public void testWaitResourceTimeOut() throws IOException, InterruptedException {
        setPoolSizes(0, 1, 2);
        m_config.setConnectionTimeOut(Duration.ofSeconds(2));
        start();

        m_freeResources.clear();
        m_busyResources.add(new FtpClientResource(m_clientFactory.createClient()));
        m_busyResources.add(new FtpClientResource(m_clientFactory.createClient()));

        take();
    }

    /**
     * Tests resource is closed immediately when released if the number of resource
     * in use is more then core size.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    public void testResourceClosedImmediatellyIfOutOfCorePoolSize() throws IOException, InterruptedException {
        final int corePoolSize = 10;

        setPoolSizes(corePoolSize - 5, corePoolSize, corePoolSize + 5);
        m_config.setMaxIdleTime(1000000000l);
        start();

        // add client resources
        final long releaseTime = System.currentTimeMillis() - 1;
        final int numResources = corePoolSize + 1;
        while (m_freeResources.size() < numResources) {
            addFreeResource(new FtpClientResource(m_clientFactory.createClient()), releaseTime);
        }

        // move two resources to busy for pure experiment
        m_busyResources.add(m_freeResources.removeFirst());
        m_busyResources.add(m_freeResources.removeFirst());

        FtpClientResource resource = take();
        // process idle resources
        resource.get(); // turn lazy initialization
        release(resource);

        // test resource is closed
        assertTrue(((MockFtpClient) resource.get()).isClosed());

        // make sure the resource pool is not less then minimum
        assertEquals(numResources - 1, m_freeResources.size() + m_busyResources.size());
    }

    /**
     * Test exception thrown because the 'take' method is call but pool is not
     * started
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test(expected = IllegalStateException.class)
    public void testCheckStartedNotStarted() throws IOException, InterruptedException {
        setPoolSizes(1, 2, 3);

        m_freeResources.add(new FtpClientResource(m_clientFactory.createClient()));
        m_freeResources.add(new FtpClientResource(m_clientFactory.createClient()));

        take();
    }

    /**
     * Test exception thrown because the 'take' method is call but pool stopped
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test(expected = IllegalStateException.class)
    public void testCheckStartedStopped() throws IOException, InterruptedException {
        setPoolSizes(1, 2, 3);
        start();
        stop();

        take();
    }

    private void setPoolSizes(final int minPoolSize, final int corePoolSize, final int maxPoolSize) {
        m_config.setMinConnectionPoolSize(minPoolSize);
        m_config.setCoreConnectionPoolSize(corePoolSize);
        m_config.setMaxConnectionPoolSize(maxPoolSize);
    }

    private void addFreeResource(final FtpClientResource resource, final long releaseTime) {
        resource.setAsFreeOn(releaseTime);
        m_freeResources.add(resource);
    }

    /**
     * Destroys the test.
     */
    @After
    public void destroyTest() {
        super.stop();
    }
}
