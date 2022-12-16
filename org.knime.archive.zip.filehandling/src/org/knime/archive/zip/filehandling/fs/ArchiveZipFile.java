package org.knime.archive.zip.filehandling.fs;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.knime.filehandling.core.connections.base.UnixStylePathUtil;

/**
 * Wrapper class for a
 * {@link org.apache.commons.compress.archivers.zip.ZipFile}.<br>
 * Some methods of making a zip file (like fullpath in 7zip app) add extra
 * directories to the top of the tree that are not recognized as new
 * {@link org.apache.commons.compress.archivers.zip.ZipArchiveEntry}
 * objects.<br/>
 * Therefore, they have to be added extra.
 *
 * @author Dragan Keselj, KNIME GmbH
 */
class ArchiveZipFile extends ZipFile {

    /**
     * The file size.
     */
    private final long m_length;

    /**
     * Some zip files may contain extra directories on the top of the directory tree
     * which can not be seen as zip entries e.g.
     * {@link org.apache.commons.compress.archivers.zip.ZipArchiveEntry} objects.
     * Thus, they have to be created and added separately.
     */
    private final List<ZipArchiveEntry> m_topEntries = new ArrayList<>();

    ArchiveZipFile(final SeekableByteChannel channel) throws IOException {
        super(channel);
        m_length = channel.size();
        createTopEntries();
    }

    ArchiveZipFile(final SeekableByteChannel channel, final String encoding) throws IOException {
        super(channel, encoding);
        m_length = channel.size();
        createTopEntries();
    }

    private void createTopEntries() {
        final var entries = StreamSupport.stream(Spliterators
                .spliteratorUnknownSize(super.getEntriesInPhysicalOrder().asIterator(), Spliterator.ORDERED), false);
        final List<String> topEntryPath = entries //NOSONAR
                .map(e -> UnixStylePathUtil.getPathSplits(ArchiveZipFileSystem.SEPARATOR, e.getName())) //
                .min((p1, p2) -> Integer.compare(p1.size(), p2.size())) //
                .get();
        topEntryPath.remove(topEntryPath.size() - 1);
        final List<String> cumulativePathSegments = new ArrayList<>();
        for (final String pathSegment : topEntryPath) {
            cumulativePathSegments.add(pathSegment);
            final String entryPath = cumulativePathSegments.stream().collect( //
                    Collector.of(StringBuilder::new, //
                            (b, str) -> b.append(str).append(ArchiveZipFileSystem.SEPARATOR).append("/"), //
                            StringBuilder::append, //
                            StringBuilder::toString));
            m_topEntries.add(new ZipArchiveEntry(entryPath));
        }
    }

    private Enumeration<ZipArchiveEntry> withTopEntries(final Enumeration<ZipArchiveEntry> entries) {
        return new Enumeration<ZipArchiveEntry>() {
            private final Iterator<ZipArchiveEntry> m_i1 = m_topEntries.iterator();
            private final Iterator<ZipArchiveEntry> m_i2 = entries.asIterator();

            @Override
            public boolean hasMoreElements() {
                return m_i1.hasNext() || m_i2.hasNext();
            }

            @Override
            public ZipArchiveEntry nextElement() {
                if (m_i1.hasNext()) {
                    return m_i1.next();
                } else if (m_i2.hasNext()) {
                    return m_i2.next();
                }
                throw new NoSuchElementException();
            }
        };
    }

    /**
     * @return size of the zip file
     */
    public long getLength() {
        return m_length;
    }

    public List<ZipArchiveEntry> getTopEntries() {
        return m_topEntries;
    }

    public boolean hasTopEntries() {
        return !m_topEntries.isEmpty();
    }

    @Override
    public Enumeration<ZipArchiveEntry> getEntries() {
        return withTopEntries(super.getEntries());
    }

    @Override
    public Enumeration<ZipArchiveEntry> getEntriesInPhysicalOrder() {
        return withTopEntries(super.getEntriesInPhysicalOrder());
    }

    @Override
    public boolean canReadEntryData(final ZipArchiveEntry ze) {
        if (m_topEntries.contains(ze)) { //NOSONAR
            return true;
        }
        return super.canReadEntryData(ze);
    }
}
