package org.knime.archive.zip.filehandling.node;

import java.io.FileInputStream;
import java.io.IOException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

@SuppressWarnings("restriction")
final class ArchiveZipConnectorNodeParametersTest extends DefaultNodeSettingsSnapshotTest {

    ArchiveZipConnectorNodeParametersTest() {
        super(getConfig());
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
