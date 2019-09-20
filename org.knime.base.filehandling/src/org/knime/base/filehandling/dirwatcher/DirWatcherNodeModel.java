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
 *   12 Sep 2019 (Alexander): created
 */
package org.knime.base.filehandling.dirwatcher;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang.mutable.MutableLong;
import org.knime.base.node.io.listfiles.ListFiles.Filter;
import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopStartNodeTerminator;
import org.knime.core.util.FileUtil;

/**
 * Node model for the Directory Watcher Loop Start node.
 * @author Alexander Fillbrunn
 */
public class DirWatcherNodeModel extends NodeModel implements LoopStartNodeTerminator {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DirWatcherNodeModel.class);
    // The polling time
    private static final long WAIT_TIME_SEC = 3;
    // The time waited after a modification or creation to check whether the file changes further
    private static final long PENDING_PERIOD_MSEC = 1000L;

    private DirWatcherSettings m_settings = new DirWatcherSettings();

    /**
     * Creates a new instance of this {@code DirWatcherNodeModel}.
     */
    protected DirWatcherNodeModel() {
        super(0, 1);
    }

    private int m_iter = 0;
    private int m_numEvents = 0;

    private Instant m_startTime;
    // End time after which not to accept events anymore
    private Instant m_endTime;
    // End of node runtime, PENDING_PERIOD_MSEC longer than end time
    private Instant m_endRuntime;
    private boolean m_stop = false;
    private Path m_dir;

    // The file filter predicate
    private Predicate<String> m_predicate;
    // The queue for new events from the watcher thread
    private BlockingQueue<FileEvent> m_queue;
    private FileAlterationMonitor m_monitor;
    // Keeps track of currently changing files to avoid duplicate events
    private Set<String> m_pendingFiles = ConcurrentHashMap.newKeySet();

    private URL getFileURL() throws InvalidPathException, InvalidSettingsException, IOException, URISyntaxException {
        URL u = FileUtil.toURL(m_settings.getLocalPath());
        if ("file".equalsIgnoreCase(u.getProtocol())) {
            Path p = Paths.get(u.toURI());
            if (!Files.isDirectory(p)) {
                throw new InvalidSettingsException("\"" + m_settings.getLocalPath()
                + "\" does not exist or is not a directory");
            }
        } else if (!"knime".equalsIgnoreCase(u.getProtocol())) {
            throw new InvalidSettingsException("Watching of " + u.getProtocol() + " URLs is not supported.");
        } else {
            Path resolved = FileUtil.resolveToPath(u);
            if ((resolved != null) && !Files.isDirectory(resolved)) {
                throw new InvalidSettingsException("\"" + m_settings.getLocalPath()
                + "\" does not exist or is not a directory");
            }
        }
        return u;
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        if (m_iter == 0) {
            m_dir = Paths.get(getFileURL().toURI());
            m_predicate = createFilterPredicate();
            m_queue = new LinkedBlockingQueue<>();
            m_startTime = Instant.now();
            // End time is the last time an event is allowed to occur
            m_endTime = m_settings.isMaxDurationEnabled() ? m_startTime.plus(m_settings.getDuration()) : null;
            // Because of the delay we use to detect changing files, we need to wait a bit longer before we end the loop
            m_endRuntime = m_settings.isMaxDurationEnabled() ? m_endTime.plusMillis(PENDING_PERIOD_MSEC + 500) : null;
            FileAlterationObserver observer = new FileAlterationObserver(m_settings.getLocalPath());
            observer.addListener(new DirWatcherListener());
            m_monitor = new FileAlterationMonitor(WAIT_TIME_SEC, observer);
            m_monitor.start();
        }

        // Calculate how long we wait: either 3 seconds or less, if less time is left from the set max duration
        long waitSec = getWaitSeconds();
        // If configured time is up, we return an empty table
        if (waitSec <= 0) {
            m_stop = true;
            m_monitor.stop();
            return new BufferedDataTable[] {createTable(Collections.emptyList(), exec)};
        }
        // Poll for new entries
        FileEvent event = m_queue.poll(Math.min(WAIT_TIME_SEC, waitSec), TimeUnit.SECONDS);
        while (event == null) {
            exec.checkCanceled();
            waitSec = getWaitSeconds();
            if (waitSec <= 0) {
                m_stop = true;
                m_monitor.stop();
                return new BufferedDataTable[] {createTable(Collections.emptyList(), exec)};
            }
            event = m_queue.poll(Math.min(WAIT_TIME_SEC, waitSec), TimeUnit.SECONDS);
        }
        List<FileEvent> events = new ArrayList<>();
        events.add(event);
        m_queue.drainTo(events);

        BufferedDataTable outputTable = createTable(events, exec);
        m_iter++;

        double progress = 0.0;
        // Set progress and check whether another iteration should be done
        if (m_settings.isMaxDurationEnabled()) {
            long rest = Duration.between(Instant.now(), m_startTime.plus(m_settings.getDuration())).getSeconds();
            long total = Duration.between(m_startTime, m_startTime.plus(m_settings.getDuration())).getSeconds();
            progress = (double)rest / total;
        }
        if (m_settings.isMaxEventsEnabled()) {
            m_stop = m_stop || m_numEvents >= m_settings.getNumEvents();
            progress = Math.max(progress, (double)m_numEvents / m_settings.getNumEvents());
        }
        if (m_settings.isMaxLoopsEnabled()) {
            m_stop = m_stop || m_iter >= m_settings.getNumLoops();
            progress = Math.max(progress, (double)m_iter / m_settings.getNumLoops());
        }
        exec.setProgress(progress);

        if (m_stop) {
            m_monitor.stop();
        }
        return new BufferedDataTable[] {outputTable};
    }

    private BufferedDataTable createTable(final List<FileEvent> events, final ExecutionContext exec) {
        BufferedDataContainer dc = exec.createDataContainer(createOutputSpec());
        for (FileEvent e : events) {
            // Ignore events that occurred too late
            if (m_settings.isMaxDurationEnabled() && e.getTime().compareTo(m_endTime) > 0) {
                continue;
            }
            Path path = e.getPath();
            Path rel = m_dir.relativize(path);
            dc.addRowToTable(new DefaultRow(new RowKey(String.format("Row%d", m_numEvents)),
                new StringCell(e.getType().toString()), // Event type
                new StringCell(path.toString()),        // Absolute path
                new StringCell(rel.toString()),         // Relative path
                new IntCell(rel.getNameCount() - 1),    // Depth
                LocalDateTimeCellFactory.create(LocalDateTime.ofInstant(e.getTime(), ZoneId.systemDefault())) // Time
            ));
            m_numEvents++;
            // Check if we are done
            if (m_settings.isMaxEventsEnabled() && m_numEvents == m_settings.getNumEvents()) {
                break;
            }
        }
        dc.close();
        return dc.getTable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (m_settings.getLocalPath().trim().length() == 0) {
            throw new InvalidSettingsException("No directory selected");
        }
        try {
            getFileURL();
        } catch (Exception e) {
            throw new InvalidSettingsException(e);
        }
        if (m_settings.getDuration().getSeconds() == 0 && m_settings.isMaxDurationEnabled()) {
            throw new InvalidSettingsException("The duration must be greater or equal to one second.");
        }
        return new DataTableSpec[] {createOutputSpec()};
    }

    private long getWaitSeconds() {
        return m_settings.isMaxDurationEnabled()
            ? Duration.between(Instant.now(), m_endRuntime).getSeconds()
            : WAIT_TIME_SEC;
    }

    private static DataTableSpec createOutputSpec() {
        return new DataTableSpecCreator().addColumns(
            new DataColumnSpecCreator("eventType", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("absolutePath", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("relativePath", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("depth", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("time", LocalDateTimeCellFactory.TYPE).createSpec()
        ).createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // No-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // No-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        DirWatcherSettings s = new DirWatcherSettings();
        s.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        if (m_monitor != null) {
            try {
                m_monitor.stop();
            } catch (Exception e) {
                LOGGER.warn("Could not close directory monitor.", e);
            }
        }
        m_monitor = null;
        m_iter = 0;
        m_numEvents = 0;
        m_stop = false;
        m_startTime = null;
        m_dir = null;
        m_pendingFiles.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean terminateLoop() {
        return m_stop;
    }

    private class DirWatcherListener implements FileAlterationListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onStart(final FileAlterationObserver observer) {
         // No op
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onDirectoryCreate(final File directory) {
            if (m_settings.isDirCreate() && matchFilter(directory)) {
                m_queue.add(new FileEvent(Paths.get(directory.toURI()), EventType.DIRECTORY_CREATE));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onDirectoryChange(final File directory) {
            if (m_settings.isDirModify() && matchFilter(directory)) {
                m_queue.add(new FileEvent(Paths.get(directory.toURI()), EventType.DIRECTORY_CHANGE));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onDirectoryDelete(final File directory) {
            if (m_settings.isDirDelete() && matchFilter(directory)) {
                m_queue.add(new FileEvent(Paths.get(directory.toURI()), EventType.DIRECTORY_DELETE));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onFileCreate(final File file) {
            if (m_settings.isFileCreate() && matchFilter(file)) {
                monitorPending(file, EventType.FILE_CREATE);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onFileChange(final File file) {
            if (m_settings.isFileModify() && matchFilter(file) && !m_pendingFiles.contains(file.getAbsolutePath())) {
                monitorPending(file, EventType.FILE_CHANGE);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onFileDelete(final File file) {
            if (m_settings.isFileDelete() && matchFilter(file)) {
                m_queue.add(new FileEvent(Paths.get(file.toURI()), EventType.FILE_DELETE));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onStop(final FileAlterationObserver observer) {
            // No op
        }
    }

    private boolean matchFilter(final File file) {
        Path p = Paths.get(file.toURI());
        String rel = m_dir.relativize(p).toString();
        return m_predicate.test(rel);
    }

    private void monitorPending(final File file, final EventType type) {
        Path path = Paths.get(file.toURI());
        final MutableLong lastModified = new MutableLong(file.lastModified());
        m_pendingFiles.add(file.getAbsolutePath());
        final Timer t = new Timer();

        t.schedule(new TimerTask() {
            @Override
            public void run() {
                final long newLastModified = file.lastModified();
                if (m_stop) {
                    t.cancel();
                    return;
                }
                // File was deleted in the meantime or not modified
                if (newLastModified == 0L || newLastModified == lastModified.longValue()) {
                    m_queue.add(new FileEvent(path, type,
                        Instant.ofEpochMilli(lastModified.longValue())));
                    t.cancel();
                    m_pendingFiles.remove(file.getAbsolutePath());
                } else {
                    lastModified.setValue(newLastModified);
                }
            }
        }, PENDING_PERIOD_MSEC, PENDING_PERIOD_MSEC);
    }

    private Predicate<String> createFilterPredicate() {
        switch (m_settings.getFilterType()) {
            case None:
                return (str) -> true;
            case Extensions:
                final String[] extensions = m_settings.getFilter().split(";");
                if (m_settings.isCaseSensitive()) {
                    return (str) -> {
                        // check if one of the extensions matches
                        for (String ext : extensions) {
                            if (str.endsWith(ext)) {
                                return true;
                            }
                        }
                        return false;
                    };
                } else {
                    return (str) -> {
                        // case insensitive check on toLowerCase
                        String lowname = str.toLowerCase();
                        for (String ext : extensions) {
                            if (lowname.endsWith(ext.toLowerCase())) {
                                return true;
                            }
                        }
                        return false;
                    };
                }
            case RegExp:
                // no break;
            case Wildcards:
                String patternS;
                Pattern regExpPattern;
                if (m_settings.getFilterType().equals(Filter.Wildcards)) {
                    patternS = WildcardMatcher.wildcardToRegex(m_settings.getFilter());
                } else {
                    patternS = m_settings.getFilter();
                }
                if (m_settings.isCaseSensitive()) {
                    regExpPattern = Pattern.compile(patternS);
                } else {
                    regExpPattern =
                        Pattern.compile(patternS, Pattern.CASE_INSENSITIVE);
                }
                return (str) -> {
                    Matcher matcher = regExpPattern.matcher(FilenameUtils.getName(str));
                    return matcher.matches();
                };
            default:
                return (str) -> false;
            }
    }

    private enum EventType {
        DIRECTORY_CREATE,
        DIRECTORY_CHANGE,
        DIRECTORY_DELETE,
        FILE_CREATE,
        FILE_CHANGE,
        FILE_DELETE
    }

    private class FileEvent {

        private Path m_path;
        private EventType m_type;
        private Instant m_time;

        public FileEvent(final Path path, final EventType type) {
            this(path, type, Instant.now());
        }

        public FileEvent(final Path path, final EventType type, final Instant time) {
            m_path = path;
            m_type = type;
            m_time = time;
        }

        public Path getPath() {
            return m_path;
        }

        public EventType getType() {
            return m_type;
        }

        public Instant getTime() {
            return m_time;
        }
    }
}
