package org.knime.archive.zip.filehandling.fs;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 * Wrapper class for a
 * {@link org.apache.commons.compress.archivers.zip.ZipFile}.<br>
 *
 * @author Dragan Keselj, KNIME GmbH
 */
class ArchiveZipFile extends ZipFile {

    /**
     * The file size.
     */
    private final long m_length;

    ArchiveZipFile(final SeekableByteChannel channel) throws IOException {
        super(channel);
        m_length = channel.size();
    }

    ArchiveZipFile(final SeekableByteChannel channel, final String encoding) throws IOException {
        super(channel, encoding);
        m_length = channel.size();
    }

    /**
     * @return size of the zip file
     */
    public long getLength() {
        return m_length;
    }
}
