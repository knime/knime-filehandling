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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 */
package org.knime.base.filehandling.remote.files;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.util.FileUtil;
import org.knime.core.util.IRemoteFileUtilsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * {@link KnimeRemoteFile} for URIs with knime:// protocol. Currently, knime://knime.workflow and
 * knime://knime.mountpoint are supported.
 *
 * @author Christian Dietz, KNIME, Konstanz, Germany
 * @since 3.5
 */
public class KnimeRemoteFile extends RemoteFile<Connection> {

    KnimeRemoteFile(final URI uri) {
        super(uri, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean usesConnection() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Connection createConnection() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return "knime";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream() throws Exception {
        return FileUtil.openStreamWithTimeout(getURI().toURL());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream() throws Exception {
        return FileUtil.openOutputConnection(getURI().toURL(), "PUT").getOutputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() throws Exception {
        return makeRestCall((s) -> s.exists(getURI().toURL()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() throws Exception {
        return makeRestCall((s) -> s.isWorkflowGroup(getURI().toURL()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long lastModified() throws Exception {
        return exists() ? 0 : makeRestCall((s) -> s.lastModified(getURI().toURL()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RemoteFile<Connection>[] listFiles() throws Exception {
        final List<URL> urls = FileUtil.listFiles(getURI().toURL(), (s) -> true, false);
        @SuppressWarnings("unchecked")
        final RemoteFile<Connection>[] files = new RemoteFile[urls.size()];
        int i = 0;
        for (final URL url : urls) {
            files[++i] = new KnimeRemoteFile(url.toURI());
        }
        return files;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete() throws Exception {
        makeRestCall((s) -> {
            s.delete(getURI().toURL());
            return null;
        });
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() throws Exception {
        return makeRestCall((s) -> s.getSize(getURI().toURL()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean mkDir() throws Exception {
        makeRestCall((s) -> {
            s.mkDir(getURI().toURL());
            return null;
        });
        return true;
    }

    /**
     * @param function called on service
     * @throws Exception
     */
    private <O> O makeRestCall(final IRemoteFileUtilServiceFunction<O> func) throws Exception {
        final WorkflowContext workflowContext = getWorkflowContext();
        if (workflowContext.getRemoteRepositoryAddress().isPresent()
            && workflowContext.getServerAuthToken().isPresent()) {
            BundleContext ctx = FrameworkUtil.getBundle(IRemoteFileUtilsService.class).getBundleContext();
            ServiceReference<IRemoteFileUtilsService> ref = ctx.getServiceReference(IRemoteFileUtilsService.class);
            if (ref != null) {
                try {
                    return func.run(ctx.getService(ref));
                } finally {
                    ctx.ungetService(ref);
                }
            } else {
                throw new IllegalStateException(
                    "Unable to access KNIME REST service. No service registered. Most likely an Implementation error.");
            }
        } else {
            throw new IllegalStateException(
                "Unable to access KNIME REST service. Invalid context. Most likely an implemenation error.");
        }
    }

    // TODO handle illegal access
    private WorkflowContext getWorkflowContext() {
        return CheckUtils
            .checkArgumentNotNull(
                CheckUtils.checkArgumentNotNull(
                    CheckUtils
                        .checkArgumentNotNull(NodeContext.getContext(),
                            "No node context available, which is required for resolving knime-URLs")
                        .getWorkflowManager(),
                    "No workflow manager in node context, which is required for resolving knime-URLs").getContext(),
                "No workflow context available, which is required for resolving knime-URLs");
    }

    @FunctionalInterface
    private static interface IRemoteFileUtilServiceFunction<O> {
        O run(IRemoteFileUtilsService service) throws Exception;
    }
}
