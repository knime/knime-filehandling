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
 *   Oct 4, 2019 (benjamin): created
 */
package org.knime.ext.ssh.commandexecutor;

import static org.knime.ext.ssh.commandexecutor.SshCommandExecutorNodeSettings.FV_EXIT;
import static org.knime.ext.ssh.commandexecutor.SshCommandExecutorNodeSettings.FV_SOUT;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.common.util.io.output.NullOutputStream;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.ext.ssh.commandexecutor.SshCommandExecutorNodeSettings.ReturnCodePolicy;
import org.knime.ext.ssh.filehandling.fs.SshFileSystemProvider;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * SSH command executor node model.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // New Node UI is not yet API
final class SshCommandExecutorNodeModel extends WebUINodeModel<SshCommandExecutorNodeSettings> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SshCommandExecutorNodeModel.class);

    private static final int CTRL_C = 0x3; // = 'C' - 64, INTERRUPT

    private static final String INTERRUPT_WARNING = "Command may still be running or "
            + "have been only partially executed!";

    // commands

    /**
     * Template when using sh to execute. First format argument is the escaped
     * command and the second any file arguments.
     */
    static final String SH_COMMAND_TEMPLATE = "sh -c -- %s sh %s";

    private static final String SH_TEST_COMMAND = //
            "sh -c -- 'echo \"$4\" \"$3\" \".$2.\" .$1. $0' shtest '' '$HOME'\\''s' '$(echo)' '`echo`'";

    private static final String SH_TEST_COMMAND_OUTPUT = "`echo` $(echo) .$HOME's. .. shtest";

    // for cancelation
    private final ExecutorService m_executor = Executors.newSingleThreadExecutor();

    SshCommandExecutorNodeModel(final WebUINodeConfiguration config) {
        super(config, SshCommandExecutorNodeSettings.class);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs,
            final SshCommandExecutorNodeSettings settings) throws InvalidSettingsException {

        settings.validateOnConfigure(inSpecs[0]).ifPresent(this::setWarningMessage);

        return new PortObjectSpec[] { FlowVariablePortObjectSpec.INSTANCE };
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec,
            final SshCommandExecutorNodeSettings settings) throws Exception {

        settings.validateOnExecute(inData[0]).ifPresent(this::setWarningMessage);

        final var port = (FileSystemPortObject) inData[0];
        try (final var fscon = port.getFileSystemConnection().orElseThrow(); // NOSONAR checked in validate
                final var fs = fscon.getFileSystem()) {
            final var provider = (SshFileSystemProvider) fs.provider();
            prepareAndExecuteCommand(provider, fs, settings, exec);
        }

        return new PortObject[] { FlowVariablePortObject.INSTANCE };
    }

    private void prepareAndExecuteCommand(final SshFileSystemProvider provider, final FSFileSystem<?> fs,
            final SshCommandExecutorNodeSettings settings, final ExecutionContext exec) throws IOException {

        final var isPosixShell = testPosixShell(provider, settings, exec);
        if (settings.m_enforceSh && !isPosixShell) {
            throw new IOException("Shell is not POSIX compliant! " //
                    + "Please see node description about how to handle non-POSIX shells.");
        }

        exec.setMessage("Requesting shell session");
        provider.invokeWithExecChannel(//
                SshCommandUtil.getCommand(settings, fs, isPosixShell), //
                settings.getCommandEncoding(), //
                settings.getShellSessionTimeout(), //
                chan -> executeCommand(settings, exec, chan));
    }

    private Void executeCommand(final SshCommandExecutorNodeSettings settings, final ExecutionContext exec,
            final ChannelExec chan) throws IOException {
        try (final var stdin = new ByteArrayOutputStream();
                final var stdout = settings.m_exposeStdout ? new ByteArrayOutputStream()
                        : new NullOutputStream()) {
            chan.setOut(stdout);

            handleCommand(settings, exec, chan, "Executing command");

            exec.setMessage("Collecting results");
            handleExitCode(settings, chan, stdout);
            return null;
        }
    }

    private String executeTestCommand(final SshCommandExecutorNodeSettings settings, final ExecutionContext exec,
            final ChannelExec chan) throws IOException {
        try (final var stdin = new ByteArrayOutputStream();
                final var stdout = new ByteArrayOutputStream()) {
            chan.setOut(stdout);

            handleCommand(settings, exec, chan, "Running test command");

            if (chan.getExitStatus() != 0) {
                return null;
            }
            return stdout.toString(settings.getOutputEncoding());
        }
    }

    private void handleCommand(final SshCommandExecutorNodeSettings settings, final ExecutionContext exec,
            final ChannelExec chan, final String executionStartedMessage) throws IOException {

        // request a tty to be able to send CTRL-C
        // this combines stderr and stdout!
        // it also causes CRNL used for line endings
        chan.setupSensibleDefaultPty();
        chan.setUsePty(true);

        exec.setMessage("Opening connection");
        try {
            chan.open().verify(settings.getShellSessionTimeout());
        } catch (IOException ioe) {
            if (ExceptionUtil.getDeepestError(ioe) instanceof TimeoutException) {
                throw new IOException("Connecting timed out!", ioe);
            } else {
                throw new IOException("Could not open connection!", ioe);
            }
        }

        exec.setMessage(executionStartedMessage);
        var waitMask = Collections.<ClientChannelEvent>emptySet();
        // the ChannelExec method ignores interrupts which can be problematic when we
        // try to cancel the node or the command is hung
        // to be able to still cancel the wait we wrap the wait invocation into a Future
        // which will respond to interrupts
        try {
            final var future = m_executor.submit(() -> chan.waitFor(EnumSet.of(ClientChannelEvent.CLOSED),
                    null));
            waitMask = future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            tryCancelCommandSafely(chan);
            final var msg = "Execution was interrupted! " + INTERRUPT_WARNING;
            // the exception seems to get swallowed when execution is canceled,
            // so we can only log the problem
            LOGGER.warn(msg);
            throw new IOException(msg);
        } catch (ExecutionException ex) { // NOSONAR interested in content
            tryCancelCommandSafely(chan);
            throw new IOException("Error during execution!", ex.getCause());
        }

        if (waitMask.contains(ClientChannelEvent.TIMEOUT)) {
            tryCancelCommand(chan);
            throw new IOException("Execution timed out! " + INTERRUPT_WARNING);
        }
    }

    private boolean testPosixShell(final SshFileSystemProvider provider, final SshCommandExecutorNodeSettings settings,
            final ExecutionContext exec) throws IOException {

        exec.setMessage("Requesting shell session");
        final var testOutput = StringUtils.trim( //
                provider.invokeWithExecChannel(SH_TEST_COMMAND, settings.getCommandEncoding(),
                        settings.getShellSessionTimeout(), chan -> executeTestCommand(settings, exec, chan)));

        if (!SH_TEST_COMMAND_OUTPUT.equals(testOutput)) {
            LOGGER.debugWithFormat("Expected output “%s”, but got “%s”",
                    SH_TEST_COMMAND_OUTPUT, testOutput);
            return false;
        }

        return true;
    }

    private void handleExitCode(final SshCommandExecutorNodeSettings settings,
            final ChannelExec chan, final OutputStream stdout) throws IOException {
        final var exit = chan.getExitStatus();
        if (settings.m_policyReturnCode == ReturnCodePolicy.FAIL && (exit == null || exit != 0)) {
            String msg;
            if (exit == null) {
                msg = "Command did not return an exit code!";
            } else {
                msg = "Command returned non-zero exit code '" + exit + "'!";
            }
            if (settings.m_exposeStdout) {
                final var out = ((ByteArrayOutputStream) stdout).toString(settings.getOutputEncoding());
                LOGGER.error(msg + " Output:\n" + out);
                msg += " Please check the logs for output.";
            }
            throw new IOException(msg);
        } else if (exit != null) {
            pushFlowVariableInt(FV_EXIT, exit);
        }
        if (settings.m_exposeStdout) {
            pushFlowVariableString(FV_SOUT, ((ByteArrayOutputStream) stdout).toString(settings.getOutputEncoding()));
        }
    }

    @SuppressWarnings("resource")
    private static void tryCancelCommand(final ChannelExec chan) throws IOException {
        chan.getInvertedIn().write(CTRL_C);
        chan.getInvertedIn().flush();
    }

    private static void tryCancelCommandSafely(final ChannelExec chan) {
        try {
            tryCancelCommand(chan);
        } catch (IOException e) {
            LOGGER.debug("Could not cancel command!", e);
        }
    }

    @Override
    protected void reset() {
        // no-op
    }

    @Override
    protected void onDispose() {
        m_executor.shutdown();
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no-op
    }
}
