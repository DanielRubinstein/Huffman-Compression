/**
 *	Basic bit OutputStream utility based on the java.nio
 *	package.  Uses multiple buffers to move optimized chunks
 *	at a time.  Allows for writing up to 32 bits at a time.
 *
 *	@contributor Owen Astrachan
 *	@author Brian Lavallee
 *	@date 10 April 2016
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

public class BitOutputStream extends OutputStream {
	
	private static final int BYTE_SIZE = 8;
	private static final int INT_SIZE = 32;
	private static final int BUFFER_SIZE = 8192;
	
	private static final long[] bitMask = { 0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff, 0x1ff, 0x3ff, 0x7ff,
			0xfff, 0x1fff, 0x3fff, 0x7fff, 0xffff, 0x1ffff, 0x3ffff, 0x7ffff, 0xfffff, 0x1fffff, 0x3fffff, 0x7fffff,
			0xffffff, 0x1ffffff, 0x3ffffff, 0x7ffffff, 0xfffffff, 0x1fffffff, 0x3fffffff, 0x7fffffff, 0xffffffff,
			0x1ffffffffl, 0x3ffffffffl, 0x7ffffffffl, 0xfffffffffl, 0x1fffffffffl, 0x3fffffffffl, 0x7fffffffffl,
			0xffffffffffl, 0x1ffffffffffl, 0x3ffffffffffl, 0x7ffffffffffl, 0xfffffffffffl, 0x1fffffffffffl,
			0x3fffffffffffl, 0x7fffffffffffl, 0xffffffffffffl, 0x1ffffffffffffl, 0x3ffffffffffffl, 0x7ffffffffffffl,
			0xfffffffffffffl, 0x1fffffffffffffl, 0x3fffffffffffffl, 0x7fffffffffffffl, 0xffffffffffffffl,
			0x1ffffffffffffffl, 0x3ffffffffffffffl, 0x7ffffffffffffffl, 0xfffffffffffffffl, 0x1fffffffffffffffl,
			0x3fffffffffffffffl, 0x7fffffffffffffffl, 0xffffffffffffffffl };
	
	private OutputStream source;
	private int bitsWritten, available;
	private long bitBuffer;
	private ByteBuffer buffer;
	private WritableByteChannel output;
	
	public BitOutputStream(String filePath) {
		this(new File(filePath));
	}
	
	public BitOutputStream(File fileSource) {
		try {
			initialize(new FileOutputStream(fileSource));
		}
		catch (FileNotFoundException fnf) {
			throw new RuntimeException(fnf);
		}
	}
	
	public BitOutputStream(OutputStream out) {
		initialize(out);
	}
	
	public void initialize(OutputStream out) {
		source = out;
		bitsWritten = 0;
		available = 64;
		bitBuffer = 0;
		output = Channels.newChannel(source);
		buffer = ByteBuffer.allocate(BUFFER_SIZE);
	}
	
	public int bitsWritten() {
		return bitsWritten;
	}
	
	public void flush() {
		emptyBitBufferExact();
		emptyBuffer();
	}
	
	public void close() {
		try {
			flush();
			output.close();
			source.close();
		}
		catch (IOException io) {
			throw new RuntimeException(io);
		}
		
	}
	
	public void write(int value) {
		writeBits(BYTE_SIZE, value);
	}
	
	public void writeBits(int numBits, int value) {
		if (numBits > INT_SIZE || numBits < 1) {
			throw new RuntimeException("Illegal argument: numBits must be on [1, 32]");
		}
		
		bitsWritten += numBits;
		
		if (numBits > available) {
			bitBuffer |= Integer.toUnsignedLong(value) >>> (numBits - available);
			value &= bitMask[numBits - available];
			numBits -= available;
			emptyBitBuffer();
		}
		
		bitBuffer |= Integer.toUnsignedLong(value) << (available - numBits);
		available -= numBits;
	}
	
	private void emptyBitBuffer() {
		if (!buffer.hasRemaining()) {
			emptyBuffer();
		}
		
		buffer.putLong(bitBuffer);
		bitBuffer = 0;
		available = 64;
	}
	
	private void emptyBitBufferExact() {
		if (!buffer.hasRemaining()) {
			emptyBuffer();
		}
		
		while (available < 64) {
			buffer.put((byte) (bitBuffer >>> 56));
			bitBuffer <<= 8;
			available += 8;
		}
	}
	
	private void emptyBuffer() {
		try {
			buffer.flip();
			output.write(buffer);
			buffer.clear();
		}
		catch (IOException io) {
			throw new RuntimeException(io);
		}
	}
}