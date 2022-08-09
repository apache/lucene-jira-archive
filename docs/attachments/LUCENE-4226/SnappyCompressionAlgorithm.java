import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.codecs.compressing.CompressionAlgorithm;
import org.apache.lucene.codecs.compressing.CompressionFormat;
import org.apache.lucene.codecs.compressing.Compressor;
import org.apache.lucene.codecs.compressing.Decompressor;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.xerial.snappy.Snappy;


public class SnappyCompressionAlgorithm extends CompressionAlgorithm {

    public static final SnappyCompressionAlgorithm INSTANCE = new SnappyCompressionAlgorithm();
    private static final int VERSION_CURRENT = 0;

    @Override
    public String getName() {
        return "Snappy";
    }

    @Override
    public CompressionFormat getCompressionFormat(DataInput in)
            throws IOException {
        final int version = in.readVInt();
        if (version != VERSION_CURRENT) throw new IllegalStateException();
        return new Format();
    }

    public static class Format extends CompressionFormat {

        @Override
        public CompressionAlgorithm getAlgorithm() {
            return INSTANCE;
        }

        @Override
        public void writeHeader(DataOutput out) throws IOException {
            out.writeVInt(VERSION_CURRENT);
        }

        @Override
        public Compressor newCompressor() {
            return new SnappyCompressor();
        }

        @Override
        public Decompressor newDecompressor() {
            return new SnappyDecompressor(new byte[0]);
        }
        
    }

    private static class SnappyCompressor extends Compressor {

        byte[] buf = new byte[0];

        @Override
        public void compress(BytesRef bytes, DataOutput out) throws IOException {
            final int maxCompressedlength = Snappy.maxCompressedLength(bytes.length);
            if (buf.length < maxCompressedlength) {
                buf = ArrayUtil.grow(buf, maxCompressedlength);
            }
            final int compressedLength = Snappy.rawCompress(bytes.bytes, bytes.offset, bytes.length, buf, 0);
            out.writeVInt(compressedLength);
            out.writeBytes(buf, compressedLength);
        }

    }

    private static class SnappyDecompressor extends Decompressor {

        byte[] buf;

        SnappyDecompressor(byte[] buf) {
            this.buf = buf;
        }

        @Override
        public void decompress(DataInput in, BytesRef bytes) throws IOException {
            bytes.offset = 0;

            final int compressedLength = in.readVInt();
            if (buf.length < compressedLength) {
                buf = ArrayUtil.grow(buf, compressedLength);
                assert buf.length >= compressedLength;
            }
            in.readBytes(buf, 0, compressedLength);

            bytes.length = Snappy.uncompressedLength(buf, 0, compressedLength);
            if (bytes.bytes.length < bytes.length) {
                bytes.bytes = ArrayUtil.grow(bytes.bytes, bytes.length);
                assert bytes.bytes.length >= bytes.length;
            }
            final int uncompressedLength = Snappy.uncompress(buf, 0, compressedLength, bytes.bytes, 0);
            assert bytes.length == uncompressedLength : "" + bytes.length + " " + uncompressedLength;
        }

        @Override
        public void skip(IndexInput in) throws IOException {
            final int compressedLength = in.readVInt();
            in.seek(in.getFilePointer() + compressedLength);
        }

        @Override
        public Decompressor clone() {
            return new SnappyDecompressor(new byte[buf.length]);
        }

    }

    public static void main(String[] args) throws IOException {
        byte[] bytes = new byte[14];
        byte[] toCompress = new byte[] {12, 44, 4, 5, 29, 9, 0, 2, 0, 66, 44, 91, 127, 3};
        final int compressedLength = Snappy.maxCompressedLength(toCompress.length);
        System.out.println("max: " + compressedLength);
        System.out.println("actual: " + Snappy.rawCompress(toCompress, 0, toCompress.length, bytes, 0));
        System.out.println(Arrays.toString(bytes));
        System.out.println(Arrays.toString(Snappy.uncompress(toCompress)));
        System.out.println("OK");
    }
}
