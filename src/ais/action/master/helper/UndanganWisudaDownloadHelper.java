package ais.action.master.helper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.zkoss.zk.ui.Sessions;
import org.zkoss.zul.Filedownload;

import ais.common.Common;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.ui.util.MyMessageboxConfig;

/**
 * Mengunduh undangan wisuda personal yang disediakan panitia.
 *
 * Undangan di dalam arsip memiliki QR unik. Karena itu pencarian wajib cocok
 * sekaligus pada nama mahasiswa dan nomor kursi. Helper ini sengaja tidak
 * melakukan fallback berdasarkan nama saja agar undangan peserta lain tidak
 * pernah terkirim ketika data nomor kursi belum benar.
 */
public final class UndanganWisudaDownloadHelper {

	private static final String DIREKTORI_UNDANGAN = "/WEB-INF/undangan-wisuda";
	private static final String[] NAMA_ARSIP = new String[] {
			"SARJANA FARMASI-20260906T023939Z-1-001.zip",
			"DIPLOMA TIGA FARMASI-20260906T023929Z-1-001.zip" };
	private static final int MAKSIMUM_UKURAN_GAMBAR = 8 * 1024 * 1024;

	private UndanganWisudaDownloadHelper() {
	}

	/** Tombol undangan hanya boleh tampil setelah seluruh persetujuan lengkap. */
	public static boolean disetujuiSemua(PendaftaranWisuda pendaftaranWisuda) {
		return pendaftaranWisuda != null
				&& pendaftaranWisuda.getId() != null
				&& pendaftaranWisuda.getMahasiswa() != null
				&& Boolean.TRUE.equals(pendaftaranWisuda.getPersetujuanWisuda())
				&& disetujui(pendaftaranWisuda.getStatusPersetujuanKeuangan())
				&& disetujui(pendaftaranWisuda.getStatusPersetujuanAdministrasi())
				&& disetujui(pendaftaranWisuda.getStatusPersetujuanAdministrasiFakultas())
				&& disetujui(pendaftaranWisuda.getStatusPersetujuanPerpustakaan())
				&& disetujui(pendaftaranWisuda.getStatusPersetujuanPerpustakaanFakultas());
	}

	private static boolean disetujui(Integer status) {
		return Integer.valueOf(1).equals(status);
	}

	public static void download(PendaftaranWisuda pendaftaranWisuda) {
		if (!disetujuiSemua(pendaftaranWisuda)) {
			tampilkanPeringatan("Undangan belum dapat diunduh karena seluruh persetujuan pendaftaran wisuda belum selesai.");
			return;
		}

		Mahasiswa mahasiswa = pendaftaranWisuda.getMahasiswa();
		String noKursi = normalkanNomorKursi(pendaftaranWisuda.getNoKursi());
		if (noKursi == null) {
			tampilkanPeringatan("Undangan belum dapat ditemukan karena nomor kursi mahasiswa belum tersedia. Silakan lengkapi atau generate nomor kursi terlebih dahulu.");
			return;
		}

		String namaMahasiswa = mahasiswa.getNama();
		if (namaMahasiswa == null || namaMahasiswa.trim().length() == 0) {
			tampilkanPeringatan("Undangan belum dapat ditemukan karena nama mahasiswa belum tersedia.");
			return;
		}

		try {
			UndanganData undangan = cariUndangan(namaMahasiswa, noKursi);
			if (undangan == null) {
				tampilkanPeringatan("File undangan untuk " + namaMahasiswa + " dengan nomor kursi "
						+ noKursi + " belum ditemukan. Mohon hubungi panitia wisuda untuk memeriksa data undangan.");
				return;
			}
			Filedownload.save(undangan.data, "image/jpeg", undangan.namaFile);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			tampilkanPeringatan("Undangan belum dapat diunduh karena file undangan tidak dapat dibaca. Silakan coba kembali atau hubungi administrator sistem.");
		}
	}

	private static UndanganData cariUndangan(String namaMahasiswa, String noKursi) throws IOException {
		String realPath = Sessions.getCurrent().getWebApp().getRealPath(DIREKTORI_UNDANGAN);
		if (realPath == null) {
			throw new IOException("Direktori undangan wisuda tidak tersedia pada deployment aplikasi.");
		}

		String awalan = "UNDANGAN_" + normalkanNama(namaMahasiswa) + "_";
		String akhiran = "_" + noKursi;
		UndanganData hasil = null;

		for (int i = 0; i < NAMA_ARSIP.length; i++) {
			File fileArsip = new File(realPath, NAMA_ARSIP[i]);
			if (!fileArsip.isFile()) {
				continue;
			}

			ZipFile zipFile = null;
			try {
				zipFile = new ZipFile(fileArsip);
				Enumeration<? extends ZipEntry> entries = zipFile.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					if (entry.isDirectory()) {
						continue;
					}

					String namaFile = namaFileSaja(entry.getName());
					String namaTanpaEkstensi = hapusEkstensi(namaFile);
					String namaNormal = normalkanNama(namaTanpaEkstensi);
					if (!namaNormal.startsWith(awalan) || !namaNormal.endsWith(akhiran)) {
						continue;
					}

					if (hasil != null) {
						throw new IOException("Ditemukan lebih dari satu undangan dengan nama dan nomor kursi yang sama.");
					}
					hasil = new UndanganData(namaFile, bacaTerbatas(zipFile, entry));
				}
			} finally {
				if (zipFile != null) {
					zipFile.close();
				}
			}
		}
		return hasil;
	}

	private static byte[] bacaTerbatas(ZipFile zipFile, ZipEntry entry) throws IOException {
		long ukuran = entry.getSize();
		if (ukuran > MAKSIMUM_UKURAN_GAMBAR) {
			throw new IOException("Ukuran file undangan melebihi batas aman.");
		}

		InputStream input = null;
		ByteArrayOutputStream output = new ByteArrayOutputStream(
				ukuran > 0 && ukuran < MAKSIMUM_UKURAN_GAMBAR ? (int) ukuran : 32768);
		try {
			input = zipFile.getInputStream(entry);
			byte[] buffer = new byte[16384];
			int total = 0;
			int jumlah;
			while ((jumlah = input.read(buffer)) != -1) {
				total += jumlah;
				if (total > MAKSIMUM_UKURAN_GAMBAR) {
					throw new IOException("Ukuran file undangan melebihi batas aman.");
				}
				output.write(buffer, 0, jumlah);
			}
			return output.toByteArray();
		} finally {
			if (input != null) {
				input.close();
			}
			output.close();
		}
	}

	private static String normalkanNama(String nilai) {
		String hasil = Normalizer.normalize(nilai == null ? "" : nilai, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
				.toUpperCase(Locale.ENGLISH).replaceAll("[^A-Z0-9]+", "_");
		return hasil.replaceAll("^_+|_+$", "");
	}

	private static String normalkanNomorKursi(String nilai) {
		if (nilai == null) {
			return null;
		}
		String hasil = nilai.trim();
		if (!hasil.matches("[0-9]+")) {
			return null;
		}
		hasil = hasil.replaceFirst("^0+(?!$)", "");
		return hasil;
	}

	private static String namaFileSaja(String path) {
		int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		return slash < 0 ? path : path.substring(slash + 1);
	}

	private static String hapusEkstensi(String namaFile) {
		int titik = namaFile.lastIndexOf('.');
		return titik <= 0 ? namaFile : namaFile.substring(0, titik);
	}

	private static void tampilkanPeringatan(String pesan) {
		MyMessageboxConfig.show(pesan, "Undangan Wisuda", MyMessageboxConfig.OK,
				MyMessageboxConfig.EXCLAMATION);
	}

	private static final class UndanganData {
		private final String namaFile;
		private final byte[] data;

		private UndanganData(String namaFile, byte[] data) {
			this.namaFile = namaFile;
			this.data = data;
		}
	}
}
