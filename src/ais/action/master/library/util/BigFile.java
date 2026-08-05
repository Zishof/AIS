package ais.action.master.library.util;

import java.util.*;
import java.io.*;

public class BigFile implements Iterable<String> {
	private BufferedReader _reader;

	public BigFile(String filePath) throws Exception {
		_reader = new BufferedReader(new FileReader(filePath));
	}

	public void Close() {
		try {
			_reader.close();
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/library/util/BigFile.java:16");
		}
	}

	public Iterator<String> iterator() {
		return new FileIterator();
	}

	private class FileIterator implements Iterator<String> {
		private String _currentLine;

		public boolean hasNext() {
			try {
				_currentLine = _reader.readLine();
			} catch (Exception ex) {
				_currentLine = null;
				ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/library/util/BigFile.java:32");
			}

			return _currentLine != null;
		}

		public String next() {
			return _currentLine;
		}

		public void remove() {
		}
	}
}