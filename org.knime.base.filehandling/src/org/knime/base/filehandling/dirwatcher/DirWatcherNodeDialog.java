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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.base.node.io.listfiles.ListFiles;
import org.knime.base.node.io.listfiles.ListFiles.Filter;
import org.knime.base.util.WildcardMatcher;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ConvenientComboBoxRenderer;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * Dialog for the Directory Watcher Loop Start node.
 * @author Alexander Fillbrunn
 */
public class DirWatcherNodeDialog extends NodeDialogPane {

    private DirWatcherSettings m_settings = new DirWatcherSettings();

    private FilesHistoryPanel m_path;
    private JCheckBox m_createFileCB = new JCheckBox("Creations");
    private JCheckBox m_deleteFileCB = new JCheckBox("Deletions");
    private JCheckBox m_modifyFileCB = new JCheckBox("Modifications");
    private JCheckBox m_createDirCB = new JCheckBox("Creations");
    private JCheckBox m_deleteDirCB = new JCheckBox("Deletions");
    private JCheckBox m_modifyDirCB = new JCheckBox("Modifications");

    private JCheckBox m_durationCB = new JCheckBox("Duration");
    private JCheckBox m_numEventsCB = new JCheckBox("Number of Events");
    private JCheckBox m_numLoopsCB = new JCheckBox("Number of Loops");
    private JSpinner m_durationHours = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    private JSpinner m_durationMinutes = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    private JSpinner m_durationSeconds = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    private JSpinner m_numEvents = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    private JSpinner m_numLoops = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));

    private JComboBox<String> m_filterField;
    private JCheckBox m_caseSensitive;
    private JRadioButton m_filterALLRadio;
    private JRadioButton m_filterExtensionsRadio;
    private JRadioButton m_filterRegExpRadio;
    private JRadioButton m_filterWildCardsRadio;

    /**
     * Creates a new default instance of {@code DirWatcherNodeDialog}.
     */
    public DirWatcherNodeDialog() {
        // Spinner sizes
        setSpinnerColumns(m_durationHours, 3);
        setSpinnerColumns(m_durationMinutes, 3);
        setSpinnerColumns(m_durationSeconds, 3);
        setSpinnerColumns(m_numEvents, 3);
        setSpinnerColumns(m_numLoops, 3);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        panel.add(createPathPanel(), gbc);
        gbc.gridy++;
        panel.add(createEndCriterionPanel(), gbc);
        gbc.gridy++;
        panel.add(createEventsPanel(), gbc);
        gbc.gridy++;
        panel.add(createFilterPanel(), gbc);

        addTab("Standard Settings", panel);
    }

    private JPanel createPathPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Selected Directory"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        FlowVariableModel pathModel = createFlowVariableModel(DirWatcherSettings.CFG_LOCAL_PATH, Type.STRING);
        m_path = new FilesHistoryPanel(pathModel, DirWatcherSettings.DIR_WATCHER_FILES_HISTORY_ID,
                    LocationValidation.DirectoryInput, new String[]{});
        m_path.setSelectMode(JFileChooser.DIRECTORIES_ONLY);
        m_path.setAllowRemoteURLs(false);
        panel.add(m_path, gbc);
        return panel;
    }

    /**
     * @return
     */
    private JPanel createEventsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Handled Events"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        panel.add(new JLabel("Files:"), gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        panel.add(m_createFileCB, gbc);
        gbc.gridx++;
        panel.add(m_modifyFileCB, gbc);
        gbc.gridx++;
        panel.add(m_deleteFileCB, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        panel.add(new JLabel("Directories:"), gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        panel.add(m_createDirCB, gbc);
        gbc.gridx++;
        panel.add(m_modifyDirCB, gbc);
        gbc.gridx++;
        panel.add(m_deleteDirCB, gbc);

        return panel;
    }

    private JPanel createEndCriterionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("End Criterion"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;

        JPanel durationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        durationPanel.add(m_durationCB);
        durationPanel.add(m_durationHours);
        durationPanel.add(new JLabel("h"));
        durationPanel.add(m_durationMinutes);
        durationPanel.add(new JLabel("m"));
        durationPanel.add(m_durationSeconds);
        durationPanel.add(new JLabel("s"));
        m_durationCB.addActionListener(e -> updateInputs());
        panel.add(durationPanel, gbc);
        gbc.gridy++;

        JPanel numEventsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        numEventsPanel.add(m_numEventsCB);
        numEventsPanel.add(m_numEvents);
        m_numEventsCB.addActionListener(e -> updateInputs());
        panel.add(numEventsPanel, gbc);
        gbc.gridy++;

        JPanel numLoopsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        numLoopsPanel.add(m_numLoopsCB);
        numLoopsPanel.add(m_numLoops);
        m_numLoopsCB.addActionListener(e -> updateInputs());
        panel.add(numLoopsPanel, gbc);
        gbc.gridy++;

        return panel;
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("File Filter"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;

        int buttonHeight = new JButton("Browse...").getPreferredSize().height;

        createFiltersModels();
        m_filterField = new JComboBox<String>();
        m_filterField.setEditable(true);
        m_filterField.setRenderer(new ConvenientComboBoxRenderer());
        m_filterField.setMinimumSize(new Dimension(250, buttonHeight));
        m_filterField.setPreferredSize(new Dimension(250, buttonHeight));

        panel.add(m_filterField, gbc);

        m_caseSensitive = new JCheckBox("case sensitive");

        JPanel filterBox = new JPanel(new GridLayout(2, 3));
        filterBox.add(m_filterALLRadio);
        filterBox.add(m_filterExtensionsRadio);
        filterBox.add(m_caseSensitive);
        filterBox.add(m_filterRegExpRadio);
        filterBox.add(m_filterWildCardsRadio);

        gbc.gridy++;
        panel.add(filterBox, gbc);

        return panel;
    }

    /** creates the filter radio buttons. */
    private void createFiltersModels() {
        m_filterALLRadio = new JRadioButton();
        m_filterALLRadio.setText("none");
        m_filterALLRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                m_filterField.setEnabled(false);
            }
        });

        m_filterExtensionsRadio = new JRadioButton();
        m_filterExtensionsRadio.setText("file extension(s)");
        m_filterExtensionsRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                m_filterField.setEnabled(true);
            }
        });

        m_filterRegExpRadio = new JRadioButton();
        m_filterRegExpRadio.setText("regular expression");
        m_filterRegExpRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                m_filterField.setEnabled(true);
            }
        });

        m_filterWildCardsRadio = new JRadioButton();
        m_filterWildCardsRadio.setText("wildcard pattern");
        m_filterWildCardsRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                m_filterField.setEnabled(true);
            }
        });

        ButtonGroup group = new ButtonGroup();
        group.add(m_filterALLRadio);
        group.add(m_filterExtensionsRadio);
        group.add(m_filterRegExpRadio);
        group.add(m_filterWildCardsRadio);
    }

    private void updateInputs() {
        m_numEvents.setEnabled(m_numEventsCB.isSelected());
        boolean durEnabled = m_durationCB.isSelected();
        m_durationHours.setEnabled(durEnabled);
        m_durationMinutes.setEnabled(durEnabled);
        m_durationSeconds.setEnabled(durEnabled);
        m_numLoops.setEnabled(m_numLoopsCB.isSelected());
    }

    private int getDurationSeconds() {
        Integer h = (Integer)m_durationHours.getValue();
        Integer m = (Integer)m_durationMinutes.getValue();
        Integer s = (Integer)m_durationSeconds.getValue();
        return h * 3600 + m * 60 + s;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        if (m_durationCB.isSelected()) {
            m_settings.setDuration(Duration.ofSeconds(getDurationSeconds()));
        }
        if (m_numEventsCB.isSelected()) {
            m_settings.setNumChanges((Integer)m_numEvents.getValue());
        }
        if (m_numLoopsCB.isSelected()) {
            m_settings.setNumLoops((Integer)m_numLoops.getValue());
        }
        m_settings.setMaxDurationEnabled(m_durationCB.isSelected());
        m_settings.setMaxEventsEnabled(m_numEventsCB.isSelected());
        m_settings.setMaxLoopsEnabled(m_numLoopsCB.isSelected());

        m_settings.setLocalPath(m_path.getSelectedFile());
        m_settings.setFileCreate(m_createFileCB.isSelected());
        m_settings.setFileDelete(m_deleteFileCB.isSelected());
        m_settings.setFileModify(m_modifyFileCB.isSelected());
        m_settings.setDirCreate(m_createDirCB.isSelected());
        m_settings.setDirDelete(m_deleteDirCB.isSelected());
        m_settings.setDirModify(m_modifyDirCB.isSelected());

        m_settings.setCaseSensitive(m_caseSensitive.isSelected());
        String filterStr = m_filterField.getEditor().getItem().toString();
        m_settings.setFilter(filterStr);

        // save the selected radio-Button
        ListFiles.Filter filter;
        if (m_filterALLRadio.isSelected()) {
            filter = Filter.None;
        } else if (m_filterExtensionsRadio.isSelected()) {
            filter = Filter.Extensions;
        } else if (m_filterRegExpRadio.isSelected()) {
            if (filterStr.trim().isEmpty()) {
                throw new InvalidSettingsException("Enter valid regular expressin pattern");
            }
            try {
                String pattern = filterStr;
                Pattern.compile(pattern);
            } catch (PatternSyntaxException pse) {
                throw new InvalidSettingsException("Error in pattern: ('" + pse.getMessage(), pse);
            }
            filter = Filter.RegExp;
        } else if (m_filterWildCardsRadio.isSelected()) {
            if ((filterStr).length() <= 0) {
                throw new InvalidSettingsException("Enter valid wildcard pattern");
            }
            try {
                String pattern = filterStr;
                pattern = WildcardMatcher.wildcardToRegex(pattern);
                Pattern.compile(pattern);
            } catch (PatternSyntaxException pse) {
                throw new InvalidSettingsException("Error in pattern: '" + pse.getMessage(), pse);
            }
            filter = Filter.Wildcards;
        } else { // one button must be selected though
            filter = Filter.None;
        }
        m_settings.setFilterType(filter);
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        String[] history = DirWatcherSettings.getFilterHistory();
        m_filterField.removeAllItems();
        for (String str : history) {
            m_filterField.addItem(str);
        }

        m_durationCB.setSelected(m_settings.isMaxDurationEnabled());
        m_numEventsCB.setSelected(m_settings.isMaxEventsEnabled());
        m_numLoopsCB.setSelected(m_settings.isMaxLoopsEnabled());
        m_durationHours.setValue(m_settings.getDuration().getSeconds() / 3600);
        m_durationMinutes.setValue((m_settings.getDuration().getSeconds() % 3600) / 60);
        m_durationSeconds.setValue((m_settings.getDuration().getSeconds() % 3600) % 60);
        m_numEvents.setValue(m_settings.getNumEvents());
        m_numLoops.setValue(m_settings.getNumLoops());
        m_createFileCB.setSelected(m_settings.isFileCreate());
        m_deleteFileCB.setSelected(m_settings.isFileDelete());
        m_modifyFileCB.setSelected(m_settings.isFileModify());
        m_createDirCB.setSelected(m_settings.isDirCreate());
        m_deleteDirCB.setSelected(m_settings.isDirDelete());
        m_modifyDirCB.setSelected(m_settings.isDirModify());
        m_path.setSelectedFile(m_settings.getLocalPath());

        m_caseSensitive.setSelected(m_settings.isCaseSensitive());
        String ext = m_settings.getFilter();
        m_filterField.getEditor().setItem(ext == null ? "" : ext);
        switch (m_settings.getFilterType()) {
            case Extensions:
                m_filterExtensionsRadio.doClick(); // trigger event
                break;
            case RegExp:
                m_filterRegExpRadio.doClick();
                break;
            case Wildcards:
                m_filterWildCardsRadio.doClick();
                break;
            default:
                m_filterALLRadio.doClick();
        }

        updateInputs();
    }

    private static void setSpinnerColumns(final JSpinner spinner, final int cols) {
        Component spinnerEditor = spinner.getEditor();
        JFormattedTextField jftf = ((JSpinner.DefaultEditor) spinnerEditor).getTextField();
        jftf.setColumns(cols);
    }
}
