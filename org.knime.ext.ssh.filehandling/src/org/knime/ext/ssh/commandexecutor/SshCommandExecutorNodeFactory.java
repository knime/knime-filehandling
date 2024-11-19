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
 *   2024-06-04 (jloescher): created
 */
package org.knime.ext.ssh.commandexecutor;

import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * SSH command executor node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // New Node UI is not yet API
public final class SshCommandExecutorNodeFactory extends WebUINodeFactory<SshCommandExecutorNodeModel> {

    private static final String FULL_DESCRIPTION = "" //
            + "Executes a non-interactive shell command using SSH.<br/>" //
            + "<p>"//
            + "Depending on the shell of the remote machine, it is usually possible to provide "
            + "a full command line, including multiple commands and shell directives."
            + "The node makes the exit code of the command(s) available as a flow variable. If enabled, "
            + "the standard out and standard error are similarly captured. "//
            + "Placeholders can be used to insert input and output files or folders into the "
            + "command string if so desired."//
            + "</p>" //
            + "<p>" //
            + "<u><b>Important Considerations:</b></u>" //
            + "<ul>"
            + "  <li>By default the node tests for a POSIX compliant <code>sh</code> shell and refuses execution "
            + "      if none is found. This is done to so that any input and output file/folder paths can be "//
            + "      securely passed to the command. "//
            + "      In case this enforcement is disabled with the setting in the advanced “Security” settings "
            + "      and no compliant shell is found, then <b>paths are directly inserted into the command string and "
            + "      special characters in them are <i>NOT</i> escaped</b> because the target shell and its quoting "
            + "      and escaping mechanism is unknown. Thus, the paths have to be manually escaped and quoted in the "
            + "      command string to prevent syntax errors and <b><u>command injection</u></b>."
            + "      The “Forbidden Characters” option provides a way to reject paths if they contain certain "
            + "      characters, which is especially important if the paths are provided via flow variable (<u>which "
            + "      is <b><i>NOT</i></b> recommended</u>). More information can be found in the relevant option "
            + "      descriptions." //
            + "  </li>" //
            + "  <li>Upon cancellation, the node sends a <code>CTRL-C</code> to the target shell and then terminates. "
            + "      Not that a remote command may continue to execute beyond that. "
            + "      This may happen either because the current program ignores the signal, or because the shell "
            + "      jumped to the next specified command. In POSIX this can be avoided "
            + "      by using conditional (<code>&amp;&amp;</code>) instead of sequential (<code>;</code>)"
            + "      execution. In either case these commands may have to be canceled manually or with subsequent SSH "
            + "      External Tool nodes." //
            + "  </li>" //
            + "  <li>Each command execution requests a shell session from  the SSH server. These sessions are shared"
            + "      with SFTP sessions from the preceding SSH Connector. The number of available "
            + "      shell sessions can be increased in the advanced tab of the SSH Connector." //
            + "  </li>" //
            + "</ul>"//
            + "</p>";

    private static final String INPUT_PORT = "SSH File System Connection";
    private static final String INPUT_PORT_DESC = "" //
            + "An SSH File System Connection whose existing SSH connection will be used to "
            + "execute the command. The file system is also browsable to select input and output files or folders.";

    private static final String OUTPUT_PORT = "Output Flow Variables";
    private static final String OUTPUT_PORT_DESC = "" //
            + "The node exports the following flow variables:" //
            + "<ul>"//
            + "  <li><code>" + SshCommandExecutorNodeSettings.FV_EXIT + "</code> (if enabled): "
            + "      contains the exit code of the command(s) (integer)</li>" //
            + "  <li><code>" + SshCommandExecutorNodeSettings.FV_SOUT + "</code> (if enabled): "
            + "      contains the complete standard and error output (string)</li>" //
            + "</ul>";

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
            .name("SSH Command Executor")//
            .icon("./commandExecutor.png").shortDescription("Execute command(s) on a remote machine using SSH")//
        .fullDescription(FULL_DESCRIPTION)//
            .modelSettingsClass(SshCommandExecutorNodeSettings.class)//
            .nodeType(NodeType.Other)//
            .addInputPort(INPUT_PORT, FileSystemPortObject.TYPE, INPUT_PORT_DESC)//
            .addOutputPort(OUTPUT_PORT, FlowVariablePortObject.TYPE_OPTIONAL, OUTPUT_PORT_DESC)//
            .sinceVersion(5, 4, 0).build();

    /**
     * Creates a new instance.
     */
    public SshCommandExecutorNodeFactory() {
        super(CONFIG);
    }

    @Override
    public SshCommandExecutorNodeModel createNodeModel() {
        return new SshCommandExecutorNodeModel(CONFIG);
    }
}
