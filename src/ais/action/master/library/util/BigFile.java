package ais.action.master.library.util;

import java.util.*;
import java.io.*;

/**
 * Pembungkus sederhana untuk membaca berkas teks besar baris demi baris tanpa memuat seluruh isi
 * berkas ke memori sekaligus, dengan antarmuka {@link Iterable} sehingga dapat dipakai langsung
 * dalam {@code for-each}. Setiap panggilan {@link Iterator#hasNext()} membaca satu baris baru dari
 * {@link BufferedReader}; iterator ini tidak mendukung banyak jalur baca bersamaan (satu
 * {@code BigFile} = satu posisi baca yang dibagi oleh iterator yang dihasilkan). Dipakai di modul
 * perpustakaan untuk memproses berkas data besar (mis. impor katalog) secara streaming.
 */
public class BigFile implements Iterable<String> {
	private BufferedReader _reader;

	/**
	 * Membuka {@code filePath} untuk dibaca baris demi baris.
	 *
	 * @param filePath path berkas teks yang akan dibaca
	 */
	public BigFile(String filePath) throws Exception {
		_reader = new BufferedReader(new FileReader(filePath));
	}

	/** Menutup reader berkas; kegagalan penutupan diabaikan (dicatat ke audit galat). */
	public void Close() {
		try {
			_reader.close();
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/library/util/BigFile.java:16");
		}
	}

	/** @return iterator baris yang membaca langsung dari reader berkas yang sama (bukan salinan independen). */
	public Iterator<String> iterator() {
		return new FileIterator();
	}

	/**
	 * Tipe implementasi bersarang {@link FileIterator} milik {@link BigFile}. Kelas ini memberi nama pada state
	 * atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link BigFile} dan dapat mengakses state kelas
	 * induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini merupakan detail
	 * implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String _currentLine}; operasi lokal:
	 * {@code hasNext()}, {@code next()}, {@code remove}(). Aturan bisnis bersama tetap berada pada kelas induk
	 * atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see BigFile
	 */
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