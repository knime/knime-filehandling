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

import java.time.Duration;

import org.apache.commons.lang.StringUtils;
import org.knime.base.node.io.listfiles.ListFiles;
import org.knime.base.node.io.listfiles.ListFiles.Filter;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.StringHistory;

/**
 * Settings for the Directory Watcher Loop Start node.
 * @author Alexander Fillbrunn
 */
public class DirWatcherSettings {

    /** Config key for the local path string setting. */
    public static final String CFG_LOCAL_PATH = "localPath";

    private static final String CFG_NUM_EVENTS = "numEvents";
    private static final String CFG_NUM_LOOPS = "numLoops";
    private static final String CFG_DURATION = "duration";
    private static final String CFG_MAX_LOOPS_ENABLED = "maxLoopsEnabled";
    private static final String CFG_MAX_EVENTS_ENABLED = "maxEventsEnabled";
    private static final String CFG_MAX_DURATION_ENABLED = "maxDurationEnabled";
    private static final String CFG_FILE_CREATE = "fileCreate";
    private static final String CFG_FILE_MODIFY = "fileModify";
    private static final String CFG_FILE_DELETE = "fileDelete";
    private static final String CFG_DIR_CREATE = "dirCreate";
    private static final String CFG_DIR_MODIFY = "dirModify";
    private static final String CFG_DIR_DELETE = "dirDelete";
    private static final String CFG_FILTER = "filter";
    private static final String CFG_FILTER_TYPE = "filterType";
    private static final String CFG_CASE_SENSITIVE = "caseSensitive";

    /**
     * History id for the directory watcher's path field.
     */
    public static final String DIR_WATCHER_FILES_HISTORY_ID = "directoryWatcherHistory";

    /**
     * History id for the directory watcher's extension field.
     */
    public static final String DIR_WATCHER_EXT_HISTORY_ID = "directoryWatcherFilterHistory";

    private String m_localPath = "";
    private int m_numEvents = 1;
    private int m_numLoops = 10;
    private Duration m_duration = Duration.ofMinutes(10);
    private boolean m_fileCreate = true;
    private boolean m_fileModify = true;
    private boolean m_fileDelete = true;
    private boolean m_dirCreate = true;
    private boolean m_dirModify = true;
    private boolean m_dirDelete = true;
    private String m_filter = "";
    private ListFiles.Filter m_filterType = ListFiles.Filter.None;
    private boolean m_caseSensitive = false;

    private boolean m_maxEventsEnabled = false;
    private boolean m_maxLoopsEnabled = true;
    private boolean m_maxDurationEnabled = true;

    /**
     * @return a string for filtering the files handled by this node
     */
    public String getFilter() {
        return m_filter;
    }

    /**
     * @param filter a string for filtering the files handled by this node
     */
    public void setFilter(final String filter) {
        m_filter = filter;
    }

    /**
     * @return the type of filter, determining how {@link DirWatcherSettings#getFilter} is handled
     */
    public ListFiles.Filter getFilterType() {
        return m_filterType;
    }

    /**
     * @param filterType the type of filter, determining how {@link DirWatcherSettings#getFilter} is handled
     */
    public void setFilterType(final ListFiles.Filter filterType) {
        m_filterType = filterType;
    }

    /**
     * @return whether the given filter expression is handled case sensitive
     */
    public boolean isCaseSensitive() {
        return m_caseSensitive;
    }

    /**
     * @param caseSensitive whether the given filter expression is handled case sensitive
     */
    public void setCaseSensitive(final boolean caseSensitive) {
        m_caseSensitive = caseSensitive;
    }

    /**
     * @return whether the node should monitor file create events
     */
    public boolean isFileCreate() {
        return m_fileCreate;
    }

    /**
     * @param fileCreate whether the node should monitor file create events
     */
    public void setFileCreate(final boolean fileCreate) {
        m_fileCreate = fileCreate;
    }

    /**
     * @return whether the node should monitor file modify events
     */
    public boolean isFileModify() {
        return m_fileModify;
    }

    /**
     * @param fileModify whether the node should monitor file modify events
     */
    public void setFileModify(final boolean fileModify) {
        m_fileModify = fileModify;
    }

    /**
     * @return whether the node should monitor file delete events
     */
    public boolean isFileDelete() {
        return m_fileDelete;
    }

    /**
     * @param fileDelete whether the node should monitor file delete events
     */
    public void setFileDelete(final boolean fileDelete) {
        m_fileDelete = fileDelete;
    }

    /**
     * @return whether the node should monitor directory create events
     */
    public boolean isDirCreate() {
        return m_dirCreate;
    }

    /**
     * @param dirCreate whether the node should monitor directory create events
     */
    public void setDirCreate(final boolean dirCreate) {
        m_dirCreate = dirCreate;
    }

    /**
     * @return whether the node should monitor directory modify events
     */
    public boolean isDirModify() {
        return m_dirModify;
    }

    /**
     * @param dirModify whether the node should monitor directory modify events
     */
    public void setDirModify(final boolean dirModify) {
        m_dirModify = dirModify;
    }

    /**
     * @return whether the node should monitor directory delete events
     */
    public boolean isDirDelete() {
        return m_dirDelete;
    }

    /**
     * @param dirDelete whether the node should monitor directory delete events
     */
    public void setDirDelete(final boolean dirDelete) {
        m_dirDelete = dirDelete;
    }

    /**
     * @return whether the number of iterations is bounded by the processed events
     */
    public boolean isMaxEventsEnabled() {
        return m_maxEventsEnabled;
    }

    /**
     * @param maxEventsEnabled whether the number of iterations is bounded by the processed events
     */
    public void setMaxEventsEnabled(final boolean maxEventsEnabled) {
        m_maxEventsEnabled = maxEventsEnabled;
    }

    /**
     * @return whether the number of iterations is bounded
     */
    public boolean isMaxLoopsEnabled() {
        return m_maxLoopsEnabled;
    }

    /**
     * @param maxLoopsEnabled whether the number of iterations is bounded
     */
    public void setMaxLoopsEnabled(final boolean maxLoopsEnabled) {
        m_maxLoopsEnabled = maxLoopsEnabled;
    }

    /**
     * @return whether the number of iterations is bounded by a specified duration
     */
    public boolean isMaxDurationEnabled() {
        return m_maxDurationEnabled;
    }

    /**
     * @param maxDurationEnabled whether the number of iterations is bounded by a specified duration
     */
    public void setMaxDurationEnabled(final boolean maxDurationEnabled) {
        m_maxDurationEnabled = maxDurationEnabled;
    }

    /**
     * @return the number of loops to execute
     * if {@link DirWatcherSettings#isMaxLoopsEnabled} is {@code true}
     */
    public int getNumLoops() {
        return m_numLoops;
    }

    /**
     * @param numLoops the number of loops to execute before stopping
     * if {@link DirWatcherSettings#isMaxLoopsEnabled} is {@code true}
     */
    public void setNumLoops(final int numLoops) {
        m_numLoops = numLoops;
    }

    /**
     * @return the monitored local path
     */
    public String getLocalPath() {
        return m_localPath;
    }

    /**
     * @param localPath the monitored local path
     */
    public void setLocalPath(final String localPath) {
        m_localPath = localPath;
    }

    /**
     * @return the number of events to receive before stopping
     * if {@link DirWatcherSettings#isMaxEventsEnabled} is {@code true}
     */
    public int getNumEvents() {
        return m_numEvents;
    }

    /**
     * @param numChanges the number of events to receive before stopping
     * if {@link DirWatcherSettings#isMaxEventsEnabled} is {@code true}
     */
    public void setNumChanges(final int numChanges) {
        m_numEvents = numChanges;
    }

    /**
     * @return the time for which the node should monitor the specified directory
     * if {@link DirWatcherSettings#isMaxDurationEnabled} is {@code true}
     */
    public Duration getDuration() {
        return m_duration;
    }

    /**
     * @param duration the time for which the node should monitor the specified directory
     * if {@link DirWatcherSettings#isMaxDurationEnabled} is {@code true}
     */
    public void setDuration(final Duration duration) {
        m_duration = duration;
    }

    /**
     * Saves this settings object to node settings.
     * @param settings the node settings to save to
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString(CFG_LOCAL_PATH, m_localPath);
        settings.addInt(CFG_NUM_EVENTS, m_numEvents);
        settings.addInt(CFG_NUM_LOOPS, m_numLoops);
        settings.addString(CFG_DURATION, m_duration.toString());
        settings.addBoolean(CFG_FILE_CREATE, m_fileCreate);
        settings.addBoolean(CFG_FILE_DELETE, m_fileDelete);
        settings.addBoolean(CFG_FILE_MODIFY, m_fileModify);
        settings.addBoolean(CFG_DIR_CREATE, m_dirCreate);
        settings.addBoolean(CFG_DIR_DELETE, m_dirDelete);
        settings.addBoolean(CFG_DIR_MODIFY, m_dirModify);
        settings.addBoolean(CFG_MAX_EVENTS_ENABLED, m_maxEventsEnabled);
        settings.addBoolean(CFG_MAX_LOOPS_ENABLED, m_maxLoopsEnabled);
        settings.addBoolean(CFG_MAX_DURATION_ENABLED, m_maxDurationEnabled);

        settings.addString(CFG_FILTER, m_filter);
        settings.addString(CFG_FILTER_TYPE, m_filterType.toString());
        settings.addBoolean(CFG_CASE_SENSITIVE, m_caseSensitive);

        if (!StringUtils.isBlank(m_localPath)) {
            StringHistory h = StringHistory.getInstance(DIR_WATCHER_FILES_HISTORY_ID);
            h.add(m_localPath);
        }

        if (!StringUtils.isBlank(m_filter)) {
            StringHistory h = StringHistory.getInstance(DIR_WATCHER_EXT_HISTORY_ID);
            h.add(m_filter);
        }
    }

    /**
     * Loads settings from node settings.
     * @param settings the node settings to load from
     * @throws InvalidSettingsException if the settings cannot be loaded
     */
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_localPath = settings.getString(CFG_LOCAL_PATH);
        m_numEvents = settings.getInt(CFG_NUM_EVENTS);
        m_numLoops = settings.getInt(CFG_NUM_LOOPS);
        m_duration = Duration.parse(settings.getString(CFG_DURATION));
        m_fileCreate = settings.getBoolean(CFG_FILE_CREATE);
        m_fileDelete = settings.getBoolean(CFG_FILE_DELETE);
        m_fileModify = settings.getBoolean(CFG_FILE_MODIFY);
        m_dirCreate = settings.getBoolean(CFG_DIR_CREATE);
        m_dirDelete = settings.getBoolean(CFG_DIR_DELETE);
        m_dirModify = settings.getBoolean(CFG_DIR_MODIFY);
        m_maxEventsEnabled = settings.getBoolean(CFG_MAX_EVENTS_ENABLED);
        m_maxLoopsEnabled = settings.getBoolean(CFG_MAX_LOOPS_ENABLED);
        m_maxDurationEnabled = settings.getBoolean(CFG_MAX_DURATION_ENABLED);

        m_filter = settings.getString(CFG_FILTER);
        m_filterType = Filter.valueOf(settings.getString(CFG_FILTER_TYPE));
        m_caseSensitive = settings.getBoolean(CFG_CASE_SENSITIVE);
    }

    /**
     * Loads settings with defaults from node settings.
     * @param settings the node settings to load from
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_localPath = settings.getString(CFG_LOCAL_PATH, "");
        m_numEvents = settings.getInt(CFG_NUM_EVENTS, 1);
        m_numLoops = settings.getInt(CFG_NUM_LOOPS, 10);
        m_duration = Duration.parse(settings.getString(CFG_DURATION, Duration.ofMinutes(10).toString()));
        m_fileCreate = settings.getBoolean(CFG_FILE_CREATE, true);
        m_fileDelete = settings.getBoolean(CFG_FILE_DELETE, true);
        m_fileModify = settings.getBoolean(CFG_FILE_MODIFY, true);
        m_dirCreate = settings.getBoolean(CFG_DIR_CREATE, true);
        m_dirDelete = settings.getBoolean(CFG_DIR_DELETE, true);
        m_dirModify = settings.getBoolean(CFG_DIR_MODIFY, true);
        m_maxEventsEnabled = settings.getBoolean(CFG_MAX_EVENTS_ENABLED, false);
        m_maxLoopsEnabled = settings.getBoolean(CFG_MAX_LOOPS_ENABLED, true);
        m_maxDurationEnabled = settings.getBoolean(CFG_MAX_DURATION_ENABLED, true);

        m_filter = settings.getString(CFG_FILTER, "");
        m_filterType = Filter.valueOf(settings.getString(CFG_FILTER_TYPE, Filter.None.toString()));
        m_caseSensitive = settings.getBoolean(CFG_CASE_SENSITIVE, false);
    }

    /** @return the previous selected extension field strings. */
    static String[] getFilterHistory() {
        StringHistory h = StringHistory.getInstance(DIR_WATCHER_EXT_HISTORY_ID);
        return h.getHistory();
    }
}
