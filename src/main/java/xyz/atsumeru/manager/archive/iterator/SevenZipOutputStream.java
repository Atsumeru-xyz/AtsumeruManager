package xyz.atsumeru.manager.archive.iterator;

import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZipException;

import java.io.ByteArrayInputStream;
import java.util.List;

public class SevenZipOutputStream implements ISequentialOutStream {
    private final List<ByteArrayInputStream> arrayInputStreams;

    public SevenZipOutputStream(List<ByteArrayInputStream> arrayInputStreams) {
        this.arrayInputStreams = arrayInputStreams;
    }

    @Override
    public int write(byte[] data) throws SevenZipException {
        arrayInputStreams.add(new ByteArrayInputStream(data));
        return data.length;
    }
}
