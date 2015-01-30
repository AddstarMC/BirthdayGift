package au.com.addstar.birthdaygift;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.google.common.io.Closeables;

public class Messages {
	public static String Join = "";
	public static String Announce = "";
	public static String Claim = "";

	public static void load(File file) throws IOException {
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			load(in);
		} finally {
			Closeables.closeQuietly(in);
		}
	}

	public static void load(InputStream stream) throws IOException {
		Properties values = new Properties();
		values.load(stream);

		Join = values.getProperty("join", "");
		Announce = values.getProperty("announcement", "");
		Claim = values.getProperty("claim", "");
	}
}
