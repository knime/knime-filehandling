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
 *  This program is distributed in the hope that it will be useful, but.
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.AfterAllOf;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.setting.fileselection.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.widget.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.ext.ssh.commandexecutor.SshCommandExecutorNodeSettings.IsCustomCommandEncodingSelected.CommandEncodingRef;
import org.knime.ext.ssh.commandexecutor.SshCommandExecutorNodeSettings.IsCustomDangerousCharactersDefined.CustomForbiddenCharactersDefinedRef;
import org.knime.ext.ssh.commandexecutor.SshCommandExecutorNodeSettings.IsCustomOutputEncodingSelected.OutputEncodingRef;
import org.knime.ext.ssh.commandexecutor.SshCommandExecutorNodeSettings.IsEnforceShDisabled.EnforceShRef;
import org.knime.ext.ssh.commandexecutor.SshCommandExecutorNodeSettings.IsInputPathSelected.InputPathRef;
import org.knime.ext.ssh.commandexecutor.SshCommandExecutorNodeSettings.IsOutputPathSelected.OutputPathRef;
import org.knime.ext.ssh.commandexecutor.SshCommandExecutorNodeSettings.IsPathFilterUsed.PolicyForbiddenPathsRef;
import org.knime.ext.ssh.filehandling.fs.SshFileSystem;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;

/**
 * Settings of the SSH command executor node managing all configurations
 * required for the node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // New Node UI is not yet API
final class SshCommandExecutorNodeSettings implements DefaultNodeSettings {

    static final String INPUT_FILE_PLACEHOLDER = "%%inputFile%%";
    static final String OUTPUT_FILE_PLACEHOLDER = "%%outputFile%%";
    static final String DEFAULT_FORBIDDEN_CHAR_SET = "<\";$`&'|>\\";
    private static final String ESCAPED_FORBIDDEN_CHAR_SET = "&lt;\";$`&amp;'|&gt;\\";

    static final String FV_EXIT = "ssh_command_exit";

    private static final String ISO_LINK = "https://docs.oracle.com/en/"
            + "java/javase/17/docs/api/java.base/java/lang/Character.html#isISOControl(int)";

    @Section(title = "Remote Command")
    interface RemoteCommandSection {
    }

    @Section(title = "Input")
    @After(RemoteCommandSection.class)
    interface InputSection {
    }

    @Section(title = "Output")
    @After(InputSection.class)
    interface OutputSection {
    }

    @Section(title = "Security", advanced = true)
    @After(OutputSection.class)
    interface SecuritySection {
    }

    @Section(title = "Timeouts", advanced = true)
    @AfterAllOf(value = { @After(OutputSection.class), @After(SecuritySection.class) })
    interface TimeoutsSection {

    }

    @Widget(title = "Command", description = "Specifies the remote command to run.<br/>"
            + "The special strings “<code>" + INPUT_FILE_PLACEHOLDER + "</code>” and " //
            + "“<code>" + OUTPUT_FILE_PLACEHOLDER + "</code>” will be replaced by the "
            + "paths specified below if desired. <b>Placeholders must be quoted using the "
            + "target shell's quoting mechanism</b> to prevent unexpected syntax errors or "
            + "code injection. Normally the node enforces a POSIX compliant <code>sh</code> shell and "
            + "provides the paths as the first and second argument respectively, thus placeholders must be quoted "
            + "using double quotes (<code>\"\"</code>). The same is true for Windows's cmd.exe. In the case POSIX "
            + "compliance could not be determined and enforcement is disabled, the file/folder paths will be "
            + "directly inserted into the command string and <b> are <i>NOT</i> escaped!</b> If the target "
            + "shell is partially POSIX compliant (like Windows PowerShell or fish), single quotes "
            + "(<code>''</code>) should be used to quote the directly inserted file/folder paths. "
            + "Even if not POSIX compliant, depending on the advanced “Forbidden Characters” option, a path may "
            + "be rejected before execution if it contains dangerous characters.<br/>" //
            + "All paths will be made <i>absolute</i> before they are used to work from "
            + "every working directory. The command will <i>not</i> use the working directory specified "
            + "in the SSH Connector Node and instead usually uses the remote user's user folder "
            + "as its working directory.<br/>"
            + "Most target shells support providing a shell script instead of a single command. "
            + "Thus, for example, multiple commands can be separated with “<code>&amp;&amp;</code>” on "
            + "POSIX shells and Windows's cmd.exe.")
    @TextInputWidget(placeholderProvider = CmdPlaceholderProvider.class)
    @Layout(RemoteCommandSection.class)
    String m_command = "";

    @Widget(title = "Command encoding", //
            description = "String encoding in which to send the command to the remote machine. This setting depends on "
                    + "the SSH server implementation on the remote machine. On modern machines, the server will "
                    + "likely expect UTF-8, even on Windows.", //
            advanced = true)
    @Layout(RemoteCommandSection.class)
    @ValueReference(CommandEncodingRef.class)
    Encoding m_commandEncoding = Encoding.UTF_8;

    @Widget(title = "Custom command encoding", //
            description = "Name of a custom character encoding known to the JVM.", //
            advanced = true)
    @Effect(predicate = IsCustomCommandEncodingSelected.class, type = EffectType.SHOW)
    @Layout(RemoteCommandSection.class)
    String m_customCommandEncoding = "";

    @Widget(title = "If the remote command exits with non-zero status", //
            description = "What to do if the remote command returns a non-zero (or no) exit code.", //
            advanced = true)
    @Layout(RemoteCommandSection.class)
    @ValueSwitchWidget
    ReturnCodePolicy m_policyReturnCode = ReturnCodePolicy.FAIL;

    @Widget(title = "Output Encoding", //
            description = "String encoding in which to expect any response output from the remote machine. This "
                    + "setting depends on the PTY implementation on the remote machine. On modern machines, the "
                    + "output will likely be in UTF-8, even on Windows.", //
            advanced = true)
    @Layout(RemoteCommandSection.class)
    @ValueReference(OutputEncodingRef.class)
    Encoding m_outputEncoding = Encoding.UTF_8;

    @Widget(title = "Custom output encoding", //
            description = "Name of a custom character encoding known to the JVM.", //
            advanced = true)
    @Effect(predicate = IsCustomOutputEncodingSelected.class, type = EffectType.SHOW)
    @Layout(RemoteCommandSection.class)
    String m_customOutputEncoding = "";

    @Widget(title = "Use input file or folder", //
            description = "Whether to replace “<code>" + INPUT_FILE_PLACEHOLDER + "</code>” in the command with the "
                    + "file or folder specified below.")
    @ValueReference(InputPathRef.class)
    @Layout(InputSection.class)
    boolean m_useInputPath;

    @Widget(title = "File/Folder", //
            description = "Specifies a path to an input file or folder which will be used in the external "
                    + "command.<br/>" //
                    + "<b>When this file is specified by a flow variable, special care has to be taken "
                    + "to avoid code injection!</b> The advanced “Security” option can be used "
                    + "to reject paths with dangerous characters. However, in general relying on flow variables is "
                    + "<b><i>NOT</i></b> recommended, and instead using a " //
                    + "<a href=\"https://hub.knime.com/n/44-o-1aGfQ_mRTaI\">Transfer Files</a> node before this node "
                    + "to ensure that the input file is at the required location is more robust and secure.")
    @Effect(predicate = IsInputPathSelected.class, type = EffectType.SHOW)
    @FileReaderWidget
    @Layout(InputSection.class)
    FileSelection m_inputPath = new FileSelection();

    @Widget(title = "If input file/folder does not exist before execution", //
            description = "What to do if the specified file or folder does not exist before execution.", //
            advanced = true)
    @Effect(predicate = IsInputPathSelected.class, type = EffectType.SHOW)
    @ValueSwitchWidget
    @Layout(InputSection.class)
    ErrorPolicy m_policyInputPathMissing = ErrorPolicy.FAIL;

    @Widget(title = "Use output file or folder", //
            description = "Whether to replace “<code>" + OUTPUT_FILE_PLACEHOLDER + "</code>” in the command with the "
                    + "file or folder specified below.")
    @ValueReference(OutputPathRef.class)
    @Layout(OutputSection.class)
    boolean m_useOutputPath;

    @Widget(title = "File/Folder", //
            description = "Specifies a path to an output file or folder which will be used in the external" //
                    + " command.<br/>"
                    + "<b>When this file is specified by a flow variable, special care has to be taken "
                    + "to avoid code injection!</b> The advanced “Security” option can be used "
                    + "to reject paths with dangerous characters. However, in general relying on flow variables is "
                    + "<b><i>NOT</i></b> recommended, and instead using a "
                    + "<a href=\"https://hub.knime.com/n/44-o-1aGfQ_mRTaI\">Transfer Files Node</a> after this node to "
                    + "ensure that the output file is moved to the desired location is more robust and secure.")
    @Effect(predicate = IsOutputPathSelected.class, type = EffectType.SHOW)
    @FileReaderWidget
    @Layout(OutputSection.class)
    FileSelection m_outputPath = new FileSelection();

    @Widget(title = "If output file/folder already exists", //
            description = "What to do if the specified file or folder already exists before execution. This can be "
                    + "used to avoid accidental overwrite of existing output files.")
    @Effect(predicate = IsOutputPathSelected.class, type = EffectType.SHOW)
    @ValueSwitchWidget
    @Layout(OutputSection.class)
    ErrorPolicy m_policyOutputPathExists = ErrorPolicy.IGNORE;

    @Widget(title = "Enforce POSIX compliant shell (recommended)", //
            description = "Whether to fail execution if the node could not detect a POSIX compliant <code>sh</code> "
                    + "shell.<br/>" //
                    + "Using a POSIX shell allows the node to pass the input and output files or folders as "
                    + "arguments/variables instead of inserting them into the command string. This increases security "
                    + "greatly as the paths only have to be quoted, and no special characters have to be escaped.<br/>"
                    + "This option should only be disabled if no POSIX shell is available. In that case the “forbidden "
                    + "character set” will still be checked as a fallback. It is recommended to <b><i>NOT</i></b> "
                    + "control input and output files or folders with flow variables if this option is disabled! (See "
                    + "respective option description.)")
    @ValueReference(EnforceShRef.class)
    @Layout(SecuritySection.class)
    boolean m_enforceSh = true;

    @Widget(title = "Use DOS-style paths if no POSIX shell is detected", //
            description = "Whether to convert absolute file/folder paths to their DOS-style equivalent by removing the "
                    + "leading slash and replacing slashes with backslashes. If the command is running on a Windows or "
                    + "DOS shell, a path is invalid otherwise.<br/>" //
                    + "DOS-style paths are only used if no POSIX compliant shell is detected, even if this option is "
                    + "enabled.")
    @Effect(predicate = IsEnforceShDisabled.class, type = EffectType.SHOW)
    @Layout(SecuritySection.class)
    boolean m_useDOSPaths;

    @Widget(title = "If file/folder paths contain forbidden characters", //
            description = "What to do if a specified absolute path of file or folder contains a character "
                    + "from the set below.")
    @ValueSwitchWidget
    @ValueReference(PolicyForbiddenPathsRef.class)
    @Layout(SecuritySection.class)
    ErrorPolicy m_policyForbiddenCharacters = ErrorPolicy.FAIL;

    @Widget(title = "Forbidden character set to use", //
            description = "Which character set to use for dangerous characters.")
    @ValueSwitchWidget
    @ValueReference(CustomForbiddenCharactersDefinedRef.class)
    @Effect(predicate = IsPathFilterUsed.class, type = EffectType.SHOW)
    @Layout(SecuritySection.class)
    ForbiddenCharacterSet m_forbiddenCharacterSet = ForbiddenCharacterSet.DEFAULT;

    @Widget(title = "Forbid control characters", //
            description = "Whether to add all <a href=\"" + ISO_LINK + "\">ISO control characters</a> like new lines "
                    + "and tabs to the custom character set defined below.")
    @Effect(predicate = IsCustomDangerousCharactersDefined.class, type = EffectType.SHOW)
    @Layout(SecuritySection.class)
    boolean m_customForbiddenCharactersIncludeControl = true;

    @Widget(title = "Forbidden characters", //
            description = "A custom list of characters to use. These characters are used verbatim. That means trailing "
                    + "white space will not be removed and escape sequences for special characters like "
                    + "<code>\\n</code> are not supported.")
    @Effect(predicate = IsCustomDangerousCharactersDefined.class, type = EffectType.SHOW)
    @Layout(SecuritySection.class)
    String m_customForbiddenCharacters = DEFAULT_FORBIDDEN_CHAR_SET;

    @Widget(title = "Shell session timeout (seconds)", //
            description = "The timeout to open a shell session using the SSH connection. " //
                    + "A value of “0” means no timeout. Upon frequent timeouts, it may help to increase the number of "
                    + "maximum concurrent shell sessions in the SSH Connector, while decreasing the number of SFTP "
                    + "sessions.")
    @Layout(TimeoutsSection.class)
    @NumberInputWidget(validation = IsNonNegativeValidation.class)
    int m_shellSessionTimeout = 30;

    Charset getOutputEncoding() throws IllegalCharsetNameException {
        return Optional.ofNullable(m_outputEncoding.m_charset) //
                .orElseGet(() -> Charset.forName(m_customOutputEncoding.trim()));
    }

    Charset getCommandEncoding() throws IllegalCharsetNameException {
        return Optional.ofNullable(m_commandEncoding.m_charset) //
                .orElseGet(() -> Charset.forName(m_customCommandEncoding.trim()));
    }

    Duration getShellSessionTimeout() {
        if (m_shellSessionTimeout > 0) {
            return Duration.ofSeconds(m_shellSessionTimeout);
        } else {
            return null;
        }
    }

    Optional<String> validateOnConfigure(final PortObjectSpec spec) throws InvalidSettingsException {
        return validate(spec, false);
    }

    Optional<String> validateOnExecute(final PortObject port) throws InvalidSettingsException {
        return validate(port.getSpec(), true);
    }

    private Optional<String> validate(final PortObjectSpec spec, final boolean isExecute)
            throws InvalidSettingsException {
        if (!(spec instanceof FileSystemPortObjectSpec)
                || !((FileSystemPortObjectSpec) spec).getFSType().equals(SshFileSystem.FS_TYPE)) {
            throw new InvalidSettingsException("Please attach an SSH Connector node as input");
        }

        basicChecks();

        final var fsSpec = (FileSystemPortObjectSpec) spec;
        if (fsSpec.getFileSystemConnection().isPresent() || (m_useInputPath || m_useOutputPath)) {
            try (final var fscon = fsSpec.getFileSystemConnection().get(); // NOSONAR checked above
                    final var fs = fscon.getFileSystem()) {
                return checkPaths(fs, isExecute);
            } catch (IOException ex) { // connection and fs should be opened by connector
                throw new InvalidSettingsException("Could not check paths", ex);
            }
        } else if (isExecute) {
            throw new InvalidSettingsException("No connection available. Execute the SSH Connector first.");
        }
        return Optional.of("No connection available. Execute the SSH Connector first.");
    }

    private void basicChecks() throws InvalidSettingsException {
        if (StringUtils.isBlank(m_command)) {
            throw new InvalidSettingsException("Please specify a remote command");
        }

        CheckUtils.checkSetting(m_shellSessionTimeout >= 0, "Please specify a non-negative shell session timeout");

        if (m_useInputPath && m_inputPath.getFSLocation() != null
                && m_inputPath.getFSLocation().getPath().isBlank()) {
            throw new InvalidSettingsException("Please specify a input file or folder, or disable it");
        }

        if (m_useOutputPath && m_outputPath.getFSLocation() != null
                && m_outputPath.getFSLocation().getPath().isBlank()) {
            throw new InvalidSettingsException("Please specify an output file or folder, or disable it");
        }

        checkEncoding(m_outputEncoding, m_customOutputEncoding, "output");
        checkEncoding(m_commandEncoding, m_customCommandEncoding, "command");
    }

    private static void checkEncoding(final Encoding encoding, final String custom, final String name)
            throws InvalidSettingsException {
        if (encoding.m_charset == null) {
            if (custom.isBlank()) {
                throw new InvalidSettingsException("Please provide a custom " + name + " encoding");
            }
            try {
                Charset.forName(custom.trim());
            } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
                throw new InvalidSettingsException(
                        "Unknown " + name + " encoding, please specify a known custom " + name + " encoding.", e);
            }
        }
    }

    private Optional<String> checkPaths(final FSFileSystem<?> fs, final boolean checkExistence)
            throws InvalidSettingsException {
        var msg = "";
        var maxPolicy = ErrorPolicy.IGNORE;
        // test input path
        if (m_useInputPath) {
            final var result = checkInputPath(fs, checkExistence);
            maxPolicy = result.getSecond();
            msg = result.getFirst();
        }
        // test output path
        if (m_useOutputPath) {
            final var result = checkOutputPath(fs, checkExistence);
            if (result.getSecond().moreSevere(maxPolicy)) {
                maxPolicy = result.getSecond();
                msg = result.getFirst();
            }
        }

        if (maxPolicy == ErrorPolicy.FAIL) {
            throw new InvalidSettingsException(msg);
        }
        checkPlaceholdersUsed();
        if (maxPolicy == ErrorPolicy.WARN) {
            return Optional.of(msg);
        } else {
            return Optional.empty();
        }
    }

    private Pair<String, ErrorPolicy> checkInputPath(final FSFileSystem<?> fs, final boolean checkExistence) {
        var msg = "";
        var maxPolicy = ErrorPolicy.IGNORE;
        final var path = fs.getPath(m_inputPath.getFSLocation()).normalize().toAbsolutePath();
        final var dangerous = checkForbiddenCharacters(path.toString());
        if (dangerous.isPresent()) {
            maxPolicy = m_policyForbiddenCharacters;
            msg = String.format("Input path “%s” contains forbidden character '%s'", path, dangerous.get());
        }
        if (checkExistence && m_policyInputPathMissing.moreSevere(maxPolicy) && !Files.exists(path)) {
            maxPolicy = m_policyInputPathMissing;
            msg = "Input path does not exist";
        }

        return Pair.create(msg, maxPolicy);
    }

    private Pair<String, ErrorPolicy> checkOutputPath(final FSFileSystem<?> fs, final boolean checkExistence) {
        var msg = "";
        var maxPolicy = ErrorPolicy.IGNORE;
        final var path = fs.getPath(m_outputPath.getFSLocation()).normalize().toAbsolutePath();
        final var dangerous = checkForbiddenCharacters(path.toString());
        if (dangerous.isPresent()) {
            maxPolicy = m_policyForbiddenCharacters;
            msg = String.format("Output path “%s” contains forbidden character '%s'", path, dangerous.get());
        }

        if (checkExistence && m_policyOutputPathExists.moreSevere(maxPolicy) && Files.exists(path)) {
            maxPolicy = m_policyOutputPathExists;
            if (m_policyOutputPathExists == ErrorPolicy.FAIL) {
                msg = "Output path exists and node will not execute due to user settings";
            } else {
                msg = "Output path exists";
            }
        }

        return Pair.create(msg, maxPolicy);
    }

    private Optional<String> checkForbiddenCharacters(final String path) {
        if (m_policyForbiddenCharacters == ErrorPolicy.IGNORE) {
            return Optional.empty();
        }

        final IntPredicate filter;
        if (m_forbiddenCharacterSet == ForbiddenCharacterSet.DEFAULT) {
            filter = i -> DEFAULT_FORBIDDEN_CHAR_SET.indexOf(i) != -1 || Character.isISOControl(i);
        } else if (m_customForbiddenCharactersIncludeControl) {
            filter = i -> m_customForbiddenCharacters.indexOf(i) != -1 || Character.isISOControl(i);
        } else {
            filter = i -> m_customForbiddenCharacters.indexOf(i) != -1;
        }

        return path.codePoints().filter(filter) //
                .mapToObj(i -> Character.isISOControl(i) ? Character.getName(i) : Character.toString(i)).findFirst();
    }

    private void checkPlaceholdersUsed() throws InvalidSettingsException {
        // this is likely a user error and may result in an unintended command being
        // executed (e.g. if the placeholder has a typo) thus, we error
        if (m_useInputPath && !m_command.contains(INPUT_FILE_PLACEHOLDER)) {
            throw new InvalidSettingsException("Input file/folder is enabled but the " + INPUT_FILE_PLACEHOLDER
                    + " placeholder is not in the command. Please insert the placeholder or disable the option.");
        } else if (m_useOutputPath && !m_command.contains(OUTPUT_FILE_PLACEHOLDER)) {
            throw new InvalidSettingsException("Output file/folder is enabled but the " + OUTPUT_FILE_PLACEHOLDER
                    + " placeholder is not in the command. Please insert the placeholder or disable the option.");
        }
    }

    enum ReturnCodePolicy {

        @Label(value = "Fail", //
                description = "Node execution will fail.")
        FAIL,

        @Label(value = "Report", //
                description = "The return value will be made available as a flow variable called " //
                    + "<code>" + FV_EXIT + "</code>.")
        REPORT;

    }

    enum Encoding {

        @Label(value = "UTF-8", //
                description = "Standard encoding used on most Linux machines and OpenSSH "
                        + "(Eight-bit UCS Transformation Format).")
        UTF_8(StandardCharsets.UTF_8),

        @Label(value = "UTF-16LE", //
                description = "May be used by some Windows machines "
                        + "(Sixteen-bit UCS Transformation Format, little-endian byte order).")
        UTF_16LE(StandardCharsets.UTF_16LE),

        @Label(value = "Other", //
                description = "Allows to specify a custom valid charset name supported by the Java "
                        + "Virtual Machine.")
        CUSTOM(null);

        private Charset m_charset;

        Encoding(final Charset charset) {
            this.m_charset = charset;
        }

    }

    enum ForbiddenCharacterSet {

        @Label(value = "Default", //
                description = "Forbids characters that can be used to start a new command or do "
                        + "command substitution in a POSIX shell or Windows's cmd.exe (namely " //
                        + "<code>" + ESCAPED_FORBIDDEN_CHAR_SET + "</code>) as well as all " //
                        + "<a href=\"" + ISO_LINK + "\">ISO control characters</a>.")
        DEFAULT,

        @Label(value = "Custom", //
                description = "Allows to define a custom character set to disallow.")
        CUSTOM;

    }

    enum ErrorPolicy {

        @Label(value = "Fail", description = "Fail node execution.")
        FAIL,

        @Label(value = "Warn", description = "Set a node warning message.")
        WARN,


        @Label(value = "Ignore", description = "Do nothing.")
        IGNORE;

        boolean moreSevere(final ErrorPolicy other) {
            return this.compareTo(other) < 0;
        }
    }

    static final class CmdPlaceholderProvider implements StateProvider<String> {

        private Supplier<Boolean> m_inputFile;

        private Supplier<Boolean> m_outputFile;

        private Supplier<Boolean> m_enforceSh;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_inputFile = initializer.computeFromValueSupplier(InputPathRef.class);
            m_outputFile = initializer.computeFromValueSupplier(OutputPathRef.class);
            m_enforceSh = initializer.computeFromValueSupplier(EnforceShRef.class);
        }

        @Override
        public String computeState(final DefaultNodeSettingsContext context) {
            final boolean in = m_inputFile.get();
            final boolean out = m_outputFile.get();
            // provide some usage examples (and best practices)
            if (!in && !out) {
                return "ps | sort -d; exit 0";
            } else if (in && !out) {
                return "ls -a " + escape(INPUT_FILE_PLACEHOLDER) + " | while read F; do ./filter.sh \"$F\"; done";
            } else if (!in) {
                return "who | grep -v \"$USER\" | diff - " + escape(OUTPUT_FILE_PLACEHOLDER);
            } else {
                return "sed -e 's/in/out/g' " + escape(INPUT_FILE_PLACEHOLDER) + //
                        " > " + escape(OUTPUT_FILE_PLACEHOLDER);
            }
        }

        private String escape(final String path) {
            final boolean softEscape = m_enforceSh.get();
            if (softEscape) {
                return '"' + path + '"';
            } else {
                return "'" + path + "'";
            }
        }
    }

    static final class IsCustomOutputEncodingSelected implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(OutputEncodingRef.class).isOneOf(Encoding.CUSTOM);
        }

        static final class OutputEncodingRef implements Reference<Encoding> {
        }
    }

    static final class IsCustomCommandEncodingSelected implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(CommandEncodingRef.class).isOneOf(Encoding.CUSTOM);
        }

        static final class CommandEncodingRef implements Reference<Encoding> {
        }
    }

    static final class IsInputPathSelected implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getBoolean(InputPathRef.class).isTrue();
        }

        static final class InputPathRef implements Reference<Boolean> {
        }
    }

    static final class IsOutputPathSelected implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getBoolean(OutputPathRef.class).isTrue();
        }

        static final class OutputPathRef implements Reference<Boolean> {
        }
    }

    static final class IsEnforceShDisabled implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getBoolean(EnforceShRef.class).isFalse();
        }

        static final class EnforceShRef implements Reference<Boolean> {
        }
    }

    static final class IsPathFilterUsed implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(PolicyForbiddenPathsRef.class).isOneOf(ErrorPolicy.FAIL, ErrorPolicy.WARN);
        }

        static final class PolicyForbiddenPathsRef implements Reference<ErrorPolicy> {
        }
    }


    static final class IsCustomDangerousCharactersDefined implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getPredicate(IsPathFilterUsed.class)
                    .and(i.getEnum(CustomForbiddenCharactersDefinedRef.class).isOneOf(ForbiddenCharacterSet.CUSTOM));
        }

        static final class CustomForbiddenCharactersDefinedRef implements Reference<ForbiddenCharacterSet> {
        }
    }

}
