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
 * ------------------------------------------------------------------------
 */

package org.knime.archive.zip.filehandling.node;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Snapshot tests for ZIP Archive Connector.
 *
 * @author Halil Yerlikaya, KNIME GmbH, Berlin, Germany
 * @author AI Migration Pipeline v1.2
 */

@SuppressWarnings("restriction")
final class ArchiveZipConnectorNodeParametersTest extends DefaultNodeSettingsSnapshotTest {

    private MockedStatic<Charset> m_mockedCharset;

    ArchiveZipConnectorNodeParametersTest() {
        super(getConfig());
    }

    @BeforeEach
    void setDefaults() {
        m_mockedCharset = Mockito.mockStatic(Charset.class, Mockito.CALLS_REAL_METHODS);
        m_mockedCharset.when(Charset::defaultCharset).thenReturn(StandardCharsets.UTF_8);
    }

    @AfterEach
    void resetDefaults() {
        m_mockedCharset.close();
    }

    private static SnapshotTestConfiguration getConfig() {
        return SnapshotTestConfiguration.builder() //
            .withInputPortObjectSpecs(createInputPortSpecs())
            .testJsonFormsForModel(ArchiveZipConnectorNodeParameters.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings()) //
            .testNodeSettingsStructure(() -> readSettings()) //
            .build();
    }

    private static ArchiveZipConnectorNodeParameters readSettings() {
        try {
            var path = getSnapshotPath(ArchiveZipConnectorNodeParameters.class).getParent().resolve("node_settings")
                .resolve("ArchiveZipConnectorNodeParameters.xml");
            try (var fis = new FileInputStream(path.toFile())) {
                var nodeSettings = NodeSettings.loadFromXML(fis);
                // The ArchiveZipConnectorNodeParameters is effectively the model-side logic + dialog
                return NodeParametersUtil.loadSettings(nodeSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()),
                    ArchiveZipConnectorNodeParameters.class);
            }
        } catch (IOException | InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
    }

    private static PortObjectSpec[] createInputPortSpecs() {
        return new PortObjectSpec[]{};
    }

}
