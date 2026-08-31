package ais.common;

import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Vbox;

import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.kursus.KomponenDataProdukKursus;
import ais.database.model.sekolah.AbsenPiket;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JamPelajaran;
import ais.ui.util.MyLabelKecilBold;
// Import kelas UI ZK dan domain Anda diasumsikan sudah ada
// import org.zkoss.zul.*; 
// import ais.ui.util.*;

/**
 * Helper terfokus untuk jadwal display. Tipe ini membungkus satu variasi kecil dari alur yang
 * lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getHariByIndex()}, {@code
 * getJamByIndex()}); pelaporan/ekspor ({@code renderImageSafe()}); operasi domain lain ({@code
 * displayHariJamRuanganJadwalPelajaran()}, {@code displayHariJamRuanganJadwalPelajaran()}, {@code
 * displayHariJamRuanganJadwalPelajaranUmum()}, {@code displayGuruJadwalPelajaran()}, {@code
 * displayGuruJadwalPelajaran()}, {@code displayGuruJadwalPelajaranUmum()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 */
public class JadwalDisplayHelper {

	// ========================================================================
	// LOGIKA JADWAL (HARI, JAM, RUANGAN)
	// ========================================================================

	public static ais.ui.util.MyHtml displayHariJamRuanganJadwalPelajaran(JadwalPelajaran jadwal) {
		if (jadwal == null) {
			return new ais.ui.util.MyHtml("");
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:8px; color:black; font-weight: bolder;'>");

		// 1. Tampilkan Masa Jadwal (Jika ada)
		if (jadwal.getMasaJadwalPelajaran() != null) {
			String masa = jadwal.getMasaJadwalPelajaran().getNama();
			if (masa != null && !masa.trim().isEmpty()) {
				sb.append(" (").append(masa.trim()).append(") ");
			}
		}

		// 2. Loop Hari 1 sampai 12
		// Menggunakan loop agar tidak menulis ulang kode 12 kali
		boolean hasContent = false;
		for (int i = 1; i <= 12; i++) {
			String hari = getHariByIndex(jadwal, i);
			JamPelajaran jam = getJamByIndex(jadwal, i);

			if (hari != null && !hari.trim().isEmpty()) {
				// Tambahkan koma atau baris baru jika bukan item pertama
				if (hasContent) {
					// Logic asli: hari ke-1, 5, 8, 10, 12 menggunakan <br> di akhir waktu
					// sebelumnya?
					// Untuk penyederhanaan yang rapi, kita gunakan <br> jika sudah ada konten
					// sebelumnya
					// atau mengikuti pola koma.
					// Sesuai kode asli: pemisah antar jadwal adalah koma, tapi ada <br> spesifik.
					// Kita gunakan pendekatan standar: <br> antar slot agar rapi.
					sb.append(", ");
				}

				sb.append(hari).append(" ");

				if (jam != null) {
					String masuk = jam.getMulaiS() == null ? "" : jam.getMulaiS();
					String keluar = jam.getSampaiS() == null ? "" : jam.getSampaiS();
					sb.append(masuk).append(" ").append(keluar);
				}

				// Meniru logic <br> spesifik dari kode asli (indeks 2, 5, 8, 10, 12 diberi
				// break)
				if (i == 2 || i == 5 || i == 8 || i == 10 || i == 12) {
					sb.append("<br>");
					hasContent = false; // Reset flag agar tidak ada koma di awal baris baru
				} else {
					hasContent = true;
				}
			}
		}

		// 3. Ruangan
		if (jadwal.getRuang() != null && jadwal.getRuang().getKodeRuangan() != null) {
			// Pastikan ada spasi/break jika konten sebelumnya ada
			sb.append(" ").append(jadwal.getRuang().getKodeRuangan()).append("<br>");
		}

		// 4. Keterangan
		if (jadwal.getKeterangan() != null && !jadwal.getKeterangan().trim().isEmpty()) {
			sb.append(" / ").append(jadwal.getKeterangan().trim());
		}

		sb.append("</div>");
		return new ais.ui.util.MyHtml(sb.toString());
	}

	/**
	 * Helper untuk mengambil nama hari berdasarkan indeks (kompatibel Java 1.7)
	 * Menggantikan pemanggilan getHari(), getHari2(), dll secara manual.
	 */
	private static String getHariByIndex(JadwalPelajaran j, int index) {
		switch (index) {
		case 1:
			return j.getHari();
		case 2:
			return j.getHari2();
		case 3:
			return j.getHari3();
		case 4:
			return j.getHari4();
		case 5:
			return j.getHari5();
		case 6:
			return j.getHari6();
		case 7:
			return j.getHari7();
		case 8:
			return j.getHari8();
		case 9:
			return j.getHari9();
		case 10:
			return j.getHari10();
		case 11:
			return j.getHari11();
		case 12:
			return j.getHari12();
		default:
			return null;
		}
	}

	/**
	 * Helper untuk mengambil objek JamPelajaran berdasarkan indeks
	 */
	private static JamPelajaran getJamByIndex(JadwalPelajaran j, int index) {
		switch (index) {
		case 1:
			return j.getJamPelajaran();
		case 2:
			return j.getJamPelajaran2();
		case 3:
			return j.getJamPelajaran3();
		case 4:
			return j.getJamPelajaran4();
		case 5:
			return j.getJamPelajaran5();
		case 6:
			return j.getJamPelajaran6();
		case 7:
			return j.getJamPelajaran7();
		case 8:
			return j.getJamPelajaran8();
		case 9:
			return j.getJamPelajaran9();
		case 10:
			return j.getJamPelajaran10();
		case 11:
			return j.getJamPelajaran11();
		case 12:
			return j.getJamPelajaran12();
		default:
			return null;
		}
	}

	public static void displayHariJamRuanganJadwalPelajaran(Row row, JadwalPelajaran jadwalPelajaran) {
		displayHariJamRuanganJadwalPelajaranUmum(row, jadwalPelajaran);
	}

	public static void displayHariJamRuanganJadwalPelajaranUmum(Component row, JadwalPelajaran jadwalPelajaran) {
		if (jadwalPelajaran == null) {
			new Label().setParent(row);
		} else {
			row.appendChild(displayHariJamRuanganJadwalPelajaran(jadwalPelajaran));
		}
	}

	// ========================================================================
	// LOGIKA GURU & PEGAWAI
	// ========================================================================

	public static Hbox displayGuruJadwalPelajaran(Component row, JadwalPelajaran jadwal, Boolean tampilAsisten) {
		return displayGuruJadwalPelajaranUmum(row, jadwal, true, tampilAsisten, null);
	}

	public static Hbox displayGuruJadwalPelajaran(Component row, JadwalPelajaran jadwal, Boolean displayName,
			Boolean tampilAsisten) {
		return displayGuruJadwalPelajaranUmum(row, jadwal, displayName, tampilAsisten, null);
	}

	public static Hbox displayGuruJadwalPelajaranUmum(Component row, JadwalPelajaran jadwal, Boolean tampilAsisten) {
		return displayGuruJadwalPelajaranUmum(row, jadwal, true, tampilAsisten, null);
	}

	public static Hbox displayGuruJadwalPelajaranUmum(Component row, JadwalPelajaran jadwal, Boolean displayName,
			Boolean tampilAsisten) {
		return displayGuruJadwalPelajaranUmum(row, jadwal, displayName, tampilAsisten, null);
	}

	public static Hbox displayGuruJadwalPelajaranUmum(Component row, JadwalPelajaran jadwal, Boolean displayName,
			Boolean tampilAsisten, Guru guruTambahan) {
		int tampilPerRow = Common.isMobile() ? 2 : 6;
		return displayGuruJadwalPelajaranUmum(row, jadwal, displayName, tampilAsisten, guruTambahan, tampilPerRow);
	}

	public static Hbox displayGuruJadwalPelajaranUmum(Component row, JadwalPelajaran jadwal, Boolean displayName,
			Boolean tampilAsisten, Guru guruTambahan, int tampilPerRow) {

		if (jadwal == null) {
			return createEmptyHbox(row);
		}

		List<Guru> guruList = jadwal.populateGuruBuNama();

		// Setup container utama
		Vbox container = new Vbox();
		container.setParent(row);

		Hbox currentRow = new Hbox();
		currentRow.setParent(container);

		StringBuilder namaBuilder = new StringBuilder();
		int counter = 0;

		// Proses List Guru Utama
		for (Guru guru : guruList) {
			processItemDisplay(container, currentRow, guru, counter, tampilPerRow);
			appendName(namaBuilder, guru.getNama());

			// Update reference row jika baris baru dibuat dalam processItemDisplay
			if (counter > 0 && counter % tampilPerRow == 0) {
				// Karena ZK structure, kita perlu mengambil child terakhir dari container
				// untuk memastikan kita menambahkan ke row yang benar jika logic layout berubah
				// Tapi untuk simplifikasi di sini, method processItemDisplay menanganinya.
				currentRow = (Hbox) container.getLastChild();
			}
			counter++;
		}

		// Proses Guru Tambahan
		if (guruTambahan != null) {
			// Cek apakah perlu baris baru sebelum render guru tambahan
			if (counter > 0 && counter % tampilPerRow == 0) {
				currentRow = new Hbox();
				currentRow.setParent(container);
			}

			try {
				CommonMedia.tampilkanGambarKecil(guruTambahan).setParent(currentRow);
				appendName(namaBuilder, guruTambahan.getNama());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (displayName) {
			container.appendChild(new MyLabelKecilBold(namaBuilder.toString()));
		}

		// Return Hbox pertama (atau container, tergantung kebutuhan caller asli,
		// tapi method asli return Hbox, meskipun membuat Vbox parent.
		// Kita return currentRow atau row pertama untuk kompatibilitas).
		return (Hbox) container.getFirstChild();
	}

	// ========================================================================
	// LOGIKA PEGAWAI & ABSEN PIKET (Refactored Pattern)
	// ========================================================================

	public static Hbox displayPegawaiKomponenDataProdukKursus(Component row, KomponenDataProdukKursus data,
			Boolean tampilAsisten) {
		return displayPegawaiKomponenDataProdukKursusUmum(row, data, true, tampilAsisten, null);
	}

	public static Hbox displayPegawaiKomponenDataProdukKursus(Component row, KomponenDataProdukKursus data,
			Boolean displayName, Boolean tampilAsisten) {
		return displayPegawaiKomponenDataProdukKursusUmum(row, data, displayName, tampilAsisten, null);
	}

	public static Hbox displayPegawaiKomponenDataProdukKursusUmum(Component row, KomponenDataProdukKursus data,
			Boolean tampilAsisten) {
		return displayPegawaiKomponenDataProdukKursusUmum(row, data, true, tampilAsisten, null);
	}

	public static Hbox displayPegawaiKomponenDataProdukKursusUmum(Component row, KomponenDataProdukKursus data,
			Boolean displayName, Boolean tampilAsisten) {
		return displayPegawaiKomponenDataProdukKursusUmum(row, data, displayName, tampilAsisten, null);
	}

	public static Hbox displayPegawaiKomponenDataProdukKursusUmum(Component row, KomponenDataProdukKursus data,
			Boolean displayName, Boolean tampilAsisten, Pegawai pegawaiTambahan) {

		if (data == null)
			return createEmptyHbox(row);

		Map<String, Pegawai> map = data.populatePegawai();
		Vbox container = new Vbox();
		container.setParent(row);
		Hbox hbox = new Hbox();
		hbox.setParent(container);

		StringBuilder names = new StringBuilder();

		for (Pegawai p : map.values()) {
			renderImageSafe(p, hbox);
			appendName(names, p.getNama());
		}

		if (pegawaiTambahan != null) {
			renderImageSafe(pegawaiTambahan, hbox);
			appendName(names, pegawaiTambahan.getNama());
		}

		if (displayName) {
			container.appendChild(new MyLabelKecilBold(names.toString()));
		}
		return hbox;
	}

	public static Hbox displayGuruAbsenPiket(Component row, AbsenPiket absen, Boolean tampilAsisten) {
		return displayGuruAbsenPiket(row, absen, true, tampilAsisten, null);
	}

	public static Hbox displayGuruAbsenPiket(Component row, AbsenPiket absen, Boolean displayName,
			Boolean tampilAsisten) {
		return displayGuruAbsenPiket(row, absen, displayName, tampilAsisten, null);
	}

	public static Hbox displayGuruAbsenPiket(Component row, AbsenPiket absen, Boolean displayName,
			Boolean tampilAsisten, Guru guruTambahan) {

		if (absen == null)
			return createEmptyHbox(row);

		Map<String, Guru> map = absen.populateGuru();
		Vbox container = new Vbox();
		container.setParent(row);
		Hbox hbox = new Hbox();
		hbox.setParent(container);

		StringBuilder names = new StringBuilder();

		for (Guru g : map.values()) {
			renderImageSafe(g, hbox);
			appendName(names, g.getNama());
		}

		if (guruTambahan != null) {
			renderImageSafe(guruTambahan, hbox);
			appendName(names, guruTambahan.getNama());
		}

		if (displayName) {
			container.appendChild(new MyLabelKecilBold(names.toString()));
		}
		return hbox;
	}

	// ========================================================================
	// HELPER METHODS (Private)
	// ========================================================================

	private static Hbox createEmptyHbox(Component parent) {
		Hbox hbox = new Hbox();
		new Label().setParent(hbox);
		hbox.setParent(parent);
		return hbox;
	}

	// Menggabungkan logika rendering gambar dan error handling
	private static void renderImageSafe(GeneralValueObject personObj, Component parent) {
		try {
			CommonMedia.tampilkanGambarKecil(personObj).setParent(parent);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	// Mengatur layout grid (pindah baris jika melebihi batas)
	private static void processItemDisplay(Vbox container, Hbox currentHbox, GeneralValueObject personObj, int index,
			int perRow) {
		if (index > 0 && index % perRow == 0) {
			currentHbox = new Hbox();
			currentHbox.setParent(container);
		}
		renderImageSafe(personObj, currentHbox);
	}

	// Helper append nama dengan koma
	private static void appendName(StringBuilder sb, String name) {
		if (sb.length() > 0) {
			sb.append(", ");
		}
		sb.append(name);
	}
}