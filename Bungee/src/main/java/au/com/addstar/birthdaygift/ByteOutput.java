package au.com.addstar.birthdaygift;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

public class ByteOutput implements DataOutput {
	private ByteArrayOutputStream stream;
	private DataOutputStream out;
	
	public ByteOutput() {
		stream = new ByteArrayOutputStream();
		out = new DataOutputStream(stream);
	}
	
	public ByteOutput(int size) {
		stream = new ByteArrayOutputStream(size);
		out = new DataOutputStream(stream);
	}
	
	@Override
	public void write(int b) {
		try {
			out.write(b);
		} catch (IOException e) {
			// Shouldnt happen
		}
	}

	@Override
	public void write(byte[] b) {
		try {
			out.write(b);
		} catch (IOException e) {
			// Shouldnt happen
		}
	}

	@Override
	public void write(byte[] b, int off, int len) {
		try {
			out.write(b, off, len);
		} catch (IOException e) {
			// Shouldnt happen
		}
	}

	@Override
	public void writeBoolean(boolean v) {
		try {
			out.writeBoolean(v);
		} catch (IOException e) {
			// Shouldnt happen
		}
	}

	@Override
	public void writeByte(int v) {
		try {
			out.writeByte(v);
		} catch (IOException e) {
			// Shouldnt happen
		}
	}

	@Override
	public void writeBytes(String s) {
		try {
			out.writeBytes(s);
		} catch (IOException e) {
			// Shouldnt happen
		}
	}

	@Override
	public void writeChar(int v) {
		try {
			out.writeChar(v);
		} catch (IOException e) {
			// Shouldnt happen
		}
	}

	@Override
	public void writeChars(String s) {
		try {
			out.writeChars(s);
		} catch (IOException e) {
			// Shouldnt happen
		}
	}

	@Override
	public void writeDouble(double v) {
		try {
			out.writeDouble(v);
		} catch (IOException e) {
			// Shouldnt happen
		}
	}

	@Override
	public void writeFloat(float v) {
		try {
			out.writeFloat(v);
		} catch (IOException e) {
			// Shouldnt happen
		}
	}

	@Override
	public void writeInt(int v) {
		try {
			out.writeInt(v);
		} catch (IOException e) {
			// Shouldnt happen
		}
	}

	@Override
	public void writeLong(long v) {
		try {
			out.writeLong(v);
		} catch (IOException e) {
			// Shouldnt happen
		}
	}

	@Override
	public void writeShort(int v) {
		try {
			out.writeShort(v);
		} catch (IOException e) {
			// Shouldnt happen
		}
	}

	@Override
	public void writeUTF(String str) {
		try {
			out.writeUTF(str);
		} catch (IOException e) {
			// Shouldnt happen
		}
	}
	
	public byte[] toBytes() {
		return stream.toByteArray();
	}

}
