package ais.common;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Filedownload;

import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.WaktuUtil;

/**
 * Laporan hasil <b>proses unggah</b> (Excel maupun berkas foto/dokumen massal) yang dapat
 * dipakai ulang oleh SEMUA layar unggah.
 *
 * <p><b>Masalah yang diselesaikan.</b> Sebelumnya hampir seluruh penangan unggah memakai pola
 * yang sama: baris Excel yang tidak cocok dengan data (mis. NIM tidak ditemukan) <i>dilewati
 * diam-diam</i> tanpa pesan apa pun, lalu di akhir proses tetap muncul kotak
 * "Upload ... berhasil dilakukan." tanpa syarat. Akibatnya pengguna melihat notifikasi berhasil
 * padahal tidak ada satu baris pun yang tersimpan, dan tidak punya cara untuk tahu baris mana
 * yang bermasalah maupun sebabnya.</p>
 *
 * <p><b>Cara pakai.</b> Buat satu objek sebelum perulangan baris, catat hasil TIAP baris, lalu
 * panggil {@link #selesaikan(EventListener)} dari thread event ZK (mis. di dalam listener Timer
 * yang selama ini dipakai untuk menutup indikator sibuk):</p>
 *
 * <pre>
 * final LaporanUpload laporan = new LaporanUpload("Upload Data Mahasiswa Lulus");
 * ...
 * for (int i = 1; i &lt; rowCount; i++) {
 *     String nim = Common.getSheetContentAsString(sheet, 0, i);
 *     try {
 *         Mahasiswa m = cariMahasiswa(nim);
 *         if (m == null) {
 *             laporan.catatDilewati(i, nim, "NIM tidak ditemukan pada data mahasiswa");
 *             continue;
 *         }
 *         simpan(m);
 *         laporan.catatBerhasil(i, nim, m.getNama());
 *     } catch (Exception e) {
 *         laporan.catatGagal(i, nim, e);
 *     }
 * }
 * // di thread event ZK:
 * laporan.selesaikan(eventListener);
 * </pre>
 *
 * <p>{@link #selesaikan(EventListener)} menulis berkas teks berisi rincian per baris, MENGUNDUHNYA
 * otomatis ke peramban, lalu menampilkan kotak pesan berisi jumlah berhasil/gagal/dilewati.
 * Ikonnya menyesuaikan: informasi bila semua berhasil, peringatan bila ada yang tidak tersimpan.</p>
 *
 * <p><b>Thread-safe.</b> Pencatatan biasanya dilakukan dari thread pekerja sementara pembacaan
 * ringkasan dilakukan dari thread event ZK, sehingga seluruh method disinkronkan.</p>
 *
 * <p>Kompatibel Java 1.6/1.7 dan ZK 5.5: tanpa lambda, tanpa diamond operator.</p>
 */
public class LaporanUpload {

	/** Status baris: tersimpan. */
	public static final String BERHASIL = "BERHASIL";
	/** Status baris: tidak tersimpan karena galat teknis. */
	public static final String GAGAL = "GAGAL";
	/** Status baris: sengaja tidak diproses (mis. data acuan tak ditemukan / baris kosong). */
	public static final String DILEWATI = "DILEWATI";

	private final String judul;
	private final List<String> rincian = new ArrayList<String>();
	private final List<String> catatanTambahan = new ArrayList<String>();
	private final java.util.Date waktuMulai;

	private String namaBerkasSumber = "";
	private int berhasil;
	private int gagal;
	private int dilewati;

	public LaporanUpload(String judul) {
		this.judul = judul == null || judul.trim().isEmpty() ? "Upload" : judul.trim();
		this.waktuMulai = WaktuUtil.getDate();
	}

	/** Nama berkas Excel yang diunggah, dicantumkan di kepala laporan. */
	public synchronized void setNamaBerkasSumber(String namaBerkasSumber) {
		this.namaBerkasSumber = namaBerkasSumber == null ? "" : namaBerkasSumber;
	}

	// ------------------------------------------------------------------ pencatatan

	/**
	 * @param nomorBaris nomor baris pada perulangan (indeks POI, berbasis 0). Ditampilkan +1 agar
	 *                   sesuai nomor baris yang dilihat pengguna di Excel.
	 * @param kunci      penanda baris bagi pengguna (NIM/NIP/kode); boleh kosong.
	 */
	public synchronized void catatBerhasil(int nomorBaris, String kunci, String keterangan) {
		berhasil++;
		tambah(nomorBaris, kunci, BERHASIL, keterangan);
	}

	public synchronized void catatGagal(int nomorBaris, String kunci, String sebab) {
		gagal++;
		tambah(nomorBaris, kunci, GAGAL, sebab);
	}

	/**
	 * Varian yang mengambil pesan dari exception. Selain pesan exception terluar, baris laporan
	 * kini SELALU menyertakan PENYEBAB AKAR (cause paling dalam — mis. pesan asli PostgreSQL)
	 * dan SARAN SOLUSI otomatis berdasarkan pola kegagalan yang umum, sehingga pengguna tahu
	 * apa yang harus dilakukan agar upload berikutnya berhasil.
	 */
	public synchronized void catatGagal(int nomorBaris, String kunci, Throwable t) {
		String sebab;
		if (t == null) {
			sebab = "(tanpa keterangan)";
		} else if (t.getMessage() == null || t.getMessage().trim().isEmpty()) {
			sebab = t.getClass().getSimpleName();
		} else {
			sebab = t.getClass().getSimpleName() + ": " + t.getMessage().trim();
		}
		if (t != null) {
			Throwable akar = akarPenyebab(t);
			if (akar != null && akar != t && akar.getMessage() != null && !akar.getMessage().trim().isEmpty()
					&& (t.getMessage() == null || !t.getMessage().contains(akar.getMessage().trim()))) {
				sebab += " | Penyebab akar: " + akar.getClass().getSimpleName() + ": "
						+ ringkas(akar.getMessage().trim(), 220);
			}
			String solusi = saranSolusi(t);
			if (!solusi.isEmpty()) {
				sebab += " | " + solusi;
			}
		}
		catatGagal(nomorBaris, kunci, sebab);
	}

	/** Potong teks panjang agar baris laporan tetap terbaca. */
	private static String ringkas(String s, int maks) {
		if (s == null) {
			return "";
		}
		String satuBaris = s.replace('\r', ' ').replace('\n', ' ');
		return satuBaris.length() <= maks ? satuBaris : satuBaris.substring(0, maks) + "...";
	}

	/** Telusuri rantai cause sampai yang paling dalam (maks 8 tingkat). */
	private static Throwable akarPenyebab(Throwable t) {
		Throwable a = t;
		int guard = 0;
		while (a != null && a.getCause() != null && a.getCause() != a && guard++ < 8) {
			a = a.getCause();
		}
		return a;
	}

	/**
	 * SARAN SOLUSI otomatis berdasarkan pola exception yang paling sering terjadi di proses
	 * upload Excel. Mengembalikan string kosong bila polanya tidak dikenali (laporan tetap
	 * menampilkan pesan exception + penyebab akar).
	 */
	public static String saranSolusi(Throwable t) {
		if (t == null) {
			return "";
		}
		String semua = "";
		Throwable a = t;
		int guard = 0;
		while (a != null && guard++ < 8) {
			semua += a.getClass().getName().toLowerCase() + " "
					+ (a.getMessage() == null ? "" : a.getMessage().toLowerCase()) + " ";
			Throwable next = a.getCause();
			if (next == a) {
				break;
			}
			a = next;
		}
		if (semua.contains("session is closed")) {
			return "Solusi: kendala internal aplikasi (koneksi database tertutup di tengah proses) -- "
					+ "sudah diperbaiki pada versi aplikasi terbaru; minta Administrator melakukan "
					+ "update/redeploy aplikasi, lalu ulangi upload file yang sama.";
		}
		if (semua.contains("value too long") || semua.contains("terlalu panjang")) {
			return "Solusi: isi salah satu kolom pada baris ini MELEBIHI batas panjang kolom database -- "
					+ "persingkat isi sel yang panjang (lihat pesan akar utk tipe kolomnya), lalu upload ulang "
					+ "baris ini.";
		}
		if (semua.contains("null id in") && semua.contains("flush")) {
			return "Solusi: baris ini gagal sebagai EFEK dari baris gagal SEBELUMNYA (session tercemar). "
					+ "Perbaiki baris gagal pertama pada laporan ini, lalu upload ulang -- baris yang sudah "
					+ "berhasil aman diikutkan lagi (tidak dobel).";
		}
		if (semua.contains("lock timeout") || semua.contains("could not obtain lock") || semua.contains("55p03")) {
			return "Solusi: data baris ini sedang dikunci/diproses pengguna lain -- tunggu 1-2 menit lalu "
					+ "upload ulang baris yang gagal.";
		}
		if (semua.contains("duplicate key") || semua.contains("unique constraint")) {
			return "Solusi: data yang sama sudah ada di sistem (bentrok data unik) -- periksa duplikat pada "
					+ "baris ini (nomor/kode yang harus unik), perbaiki di Excel, lalu upload ulang.";
		}
		if (semua.contains("numberformatexception") || semua.contains("for input string")) {
			return "Solusi: ada kolom ANGKA pada baris ini yang berisi teks/format salah -- pastikan sel "
					+ "angka hanya berisi angka (tanpa huruf/spasi/karakter lain), lalu upload ulang.";
		}
		if (semua.contains("unparseable date") || semua.contains("parseexception")) {
			return "Solusi: format TANGGAL pada baris ini salah -- gunakan format dd-MM-yyyy (mis. "
					+ "27-07-2026) atau format sel Date di Excel, lalu upload ulang.";
		}
		if (semua.contains("transientobject") || semua.contains("unsaved transient")) {
			return "Solusi: kendala internal aplikasi (referensi data belum tersimpan) -- minta Administrator "
					+ "update aplikasi ke versi terbaru, lalu ulangi upload.";
		}
		if (semua.contains("permission denied")) {
			return "Solusi: user database aplikasi tidak punya hak akses objek yang dibutuhkan -- hubungi "
					+ "Administrator/DBA (pesan akar menyebut schema/tabel yang ditolak).";
		}
		if (semua.contains("could not open connection") || semua.contains("connections could not be acquired")
				|| semua.contains("connection has been closed") || semua.contains("i/o error")) {
			return "Solusi: gangguan koneksi database (sibuk/putus) -- tunggu beberapa saat lalu ulangi "
					+ "seluruh proses upload.";
		}
		return "";
	}

	public synchronized void catatDilewati(int nomorBaris, String kunci, String sebab) {
		dilewati++;
		tambah(nomorBaris, kunci, DILEWATI, sebab);
	}

	/**
	 * Varian {@link #catatGagal(int, String, Throwable)} yang JUGA mencatat rincian TEKNIS
	 * lengkap ke seksi "CATATAN TEKNIS TAMBAHAN" di akhir laporan (lihat
	 * {@link #detailTeknisException(Throwable)}) — kelas exception, pesan, rantai
	 * {@code cause}, titik BERHENTI tepatnya di kode aplikasi sendiri (class.method
	 * berikut nama berkas &amp; nomor baris), dan langkah umum mengatasinya — TANPA merusak
	 * perataan kolom pada baris ringkas per-item. Dipakai proses yang butuh forensik
	 * lengkap per baris (bukan cuma pesan singkat), mis. sinkronisasi massal calon
	 * mahasiswa dengan pembayaran.
	 */
	public synchronized void catatGagalDetail(int nomorBaris, String kunci, Throwable t) {
		catatGagal(nomorBaris, kunci, t);
		if (t != null) {
			tambahCatatan("Baris " + (nomorBaris + 1) + " ("
					+ (kunci == null || kunci.trim().isEmpty() ? "-" : kunci.trim()) + "):\n"
					+ detailTeknisException(t));
		}
	}

	/**
	 * Susun rincian TEKNIS dari sebuah {@link Throwable}: kelas + pesan + rantai
	 * {@code cause} (maks 6 tingkat), masing-masing disertai titik BERHENTI paling relevan
	 * — frame PERTAMA di stack trace yang berasal dari kode aplikasi sendiri (package
	 * {@code ais.}), berupa {@code NamaClass.namaMethod (NamaBerkas.java:baris)} — supaya
	 * developer/admin langsung tahu class mana & baris kode berapa yang menyebabkan gagal,
	 * bukan cuma pesan generik. Ditutup dengan langkah umum mengatasi.
	 */
	public static String detailTeknisException(Throwable t) {
		if (t == null) {
			return "(tanpa keterangan)";
		}
		StringBuilder sb = new StringBuilder();
		Throwable saatIni = t;
		Throwable sebelumnya = null;
		int guard = 0;
		while (saatIni != null && saatIni != sebelumnya && guard++ < 6) {
			sb.append(guard > 1 ? "  disebabkan oleh: " : "  Exception   : ");
			sb.append(saatIni.getClass().getName());
			if (saatIni.getMessage() != null && !saatIni.getMessage().trim().isEmpty()) {
				sb.append(": ").append(saatIni.getMessage().trim());
			}
			sb.append("\n");

			StackTraceElement[] jejak = saatIni.getStackTrace();
			StackTraceElement titik = null;
			for (int i = 0; jejak != null && i < jejak.length; i++) {
				if (jejak[i].getClassName().startsWith("ais.")) {
					titik = jejak[i];
					break;
				}
			}
			if (titik == null && jejak != null && jejak.length > 0) {
				titik = jejak[0];
			}
			if (titik != null) {
				sb.append("    berhenti di: ").append(titik.getClassName()).append(".")
						.append(titik.getMethodName()).append(" (").append(titik.getFileName()).append(":")
						.append(titik.getLineNumber()).append(")\n");
			}

			sebelumnya = saatIni;
			saatIni = saatIni.getCause();
		}
		sb.append("  Langkah mengatasi: (1) catat/screenshot detail teknis di atas (kelas & baris kode); "
				+ "(2) bila berkaitan dengan data yang belum lengkap (mis. Setting Biaya/Jadwal Pembayaran/Jurusan "
				+ "belum diisi utk baris ini), lengkapi data tsb lalu ulangi proses HANYA utk baris ini; "
				+ "(3) bila error database/koneksi (timeout, connection closed, dsb.), tunggu beberapa saat lalu "
				+ "ulangi seluruh proses; (4) bila masih gagal, sampaikan detail teknis di atas ke tim developer.");
		return sb.toString();
	}

	private void tambah(int nomorBaris, String kunci, String status, String keterangan) {
		StringBuilder sb = new StringBuilder();
		sb.append("Baris ").append(nomorBaris + 1);
		while (sb.length() < 12) {
			sb.append(' ');
		}
		sb.append("| ");
		String k = kunci == null || kunci.trim().isEmpty() ? "-" : kunci.trim();
		sb.append(k);
		for (int i = k.length(); i < 22; i++) {
			sb.append(' ');
		}
		sb.append("| ").append(status);
		for (int i = status.length(); i < 9; i++) {
			sb.append(' ');
		}
		sb.append("| ").append(keterangan == null || keterangan.trim().isEmpty() ? "-" : keterangan.trim());
		rincian.add(sb.toString());
	}

	// ------------------------------------------------------------------ ringkasan

	/**
	 * Catatan bebas yang ikut dicetak di akhir laporan TANPA memengaruhi hitungan
	 * berhasil/gagal/dilewati. Dipakai mempertahankan keterangan teknis yang dulu ditulis ke
	 * berkas error terpisah (mis. isi baris mentah saat gagal simpan), supaya informasinya tidak
	 * hilang setelah berkas error digabung ke laporan ini.
	 */
	public synchronized void tambahCatatan(String catatan) {
		if (catatan != null && !catatan.trim().isEmpty()) {
			catatanTambahan.add(catatan.trim());
		}
	}

	public synchronized int getBerhasil() {
		return berhasil;
	}

	public synchronized int getGagal() {
		return gagal;
	}

	public synchronized int getDilewati() {
		return dilewati;
	}

	public synchronized int getTotal() {
		return berhasil + gagal + dilewati;
	}

	/** true bila ada baris yang TIDAK tersimpan (gagal atau dilewati). */
	public synchronized boolean adaMasalah() {
		return gagal > 0 || dilewati > 0;
	}

	/** Kalimat ringkas untuk kotak pesan. */
	public synchronized String ringkasan() {
		if (getTotal() == 0) {
			return "TIDAK ADA data yang diproses. Pastikan berkas Excel berisi data pada lembar pertama "
					+ "dan formatnya sesuai berkas contoh (gunakan tombol Download untuk mengambil format).";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Berhasil disimpan : ").append(berhasil).append(" baris\n");
		if (gagal > 0) {
			sb.append("Gagal             : ").append(gagal).append(" baris\n");
		}
		if (dilewati > 0) {
			sb.append("Dilewati          : ").append(dilewati).append(" baris\n");
		}
		sb.append("Total diproses    : ").append(getTotal()).append(" baris");
		return sb.toString();
	}

	// ------------------------------------------------------------------ berkas & penyelesaian

	/** Menyusun isi berkas laporan. */
	public synchronized String susunIsiBerkas() {
		SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		String garis = "------------------------------------------------------------------------";
		StringBuilder sb = new StringBuilder();
		sb.append("LAPORAN UPLOAD EXCEL\n");
		sb.append(garis).append("\n");
		sb.append("Judul        : ").append(judul).append("\n");
		if (!namaBerkasSumber.isEmpty()) {
			sb.append("Sumber       : ").append(namaBerkasSumber).append("\n");
		}
		sb.append("Mulai        : ").append(fmt.format(waktuMulai)).append("\n");
		sb.append("Selesai      : ").append(fmt.format(WaktuUtil.getDate())).append("\n");
		sb.append(garis).append("\n");
		sb.append("RINGKASAN\n");
		sb.append("  Berhasil disimpan : ").append(berhasil).append("\n");
		sb.append("  Gagal             : ").append(gagal).append("\n");
		sb.append("  Dilewati          : ").append(dilewati).append("\n");
		sb.append("  Total diproses    : ").append(getTotal()).append("\n");
		sb.append(garis).append("\n");
		sb.append("RINCIAN PER BARIS\n");
		sb.append(garis).append("\n");
		if (rincian.isEmpty()) {
			sb.append("(tidak ada baris data yang terbaca pada lembar pertama berkas Excel)\n");
		} else {
			for (int i = 0; i < rincian.size(); i++) {
				sb.append(rincian.get(i)).append("\n");
			}
		}
		sb.append(garis).append("\n");
		if (!catatanTambahan.isEmpty()) {
			sb.append("CATATAN TEKNIS TAMBAHAN\n");
			sb.append(garis).append("\n");
			for (int i = 0; i < catatanTambahan.size(); i++) {
				sb.append(catatanTambahan.get(i)).append("\n");
			}
			sb.append(garis).append("\n");
		}
		sb.append("Keterangan status:\n");
		sb.append("  BERHASIL = baris tersimpan ke basis data.\n");
		sb.append("  GAGAL    = baris tidak tersimpan karena galat; lihat kolom terakhir.\n");
		sb.append("  DILEWATI = baris sengaja tidak diproses, mis. data acuan tidak ditemukan.\n");
		return sb.toString();
	}

	/** Menulis laporan ke berkas teks pada folder tmp aplikasi. */
	public synchronized File tulisBerkas() throws Exception {
		String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(WaktuUtil.getDate());
		String nama = judul.replaceAll("[^A-Za-z0-9]+", "_");
		if (nama.length() == 0) {
			nama = "Upload";
		}
		File file = new File(Common.REAL_PATH + "/tmp/Laporan_" + nama + "_" + ts + ".txt");
		if (file.getParentFile() != null && !file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		Writer w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
		try {
			w.write(susunIsiBerkas());
		} finally {
			try {
				w.close();
			} catch (Exception e) {
				ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/LaporanUpload.java:tulisBerkas");
			}
		}
		return file;
	}

	/**
	 * Menutup proses unggah: menulis berkas laporan, mengunduhnya otomatis, lalu menampilkan
	 * kotak pesan berisi jumlah berhasil/gagal/dilewati.
	 *
	 * <p>WAJIB dipanggil dari thread event ZK (bukan thread pekerja), karena memakai
	 * {@link Filedownload} dan messagebox. Bila penulisan/pengunduhan berkas gagal, kotak pesan
	 * TETAP tampil supaya pengguna tidak kehilangan informasi jumlahnya.</p>
	 *
	 * @param setelahTutup listener yang dijalankan setelah pengguna menekan OK (boleh null);
	 *                     biasanya dipakai untuk memuat ulang grid.
	 */
	public void selesaikan(EventListener setelahTutup) {
		boolean terunduh = false;
		try {
			File file = tulisBerkas();
			// JARING PENGAMAN (independen dari fix pemanggilan lintas-thread di caller,
			// mis. DosenPembimbingAkademikAction): tulisBerkas() bisa saja mengembalikan
			// null/berkas kosong bila penulisan gagal di tengah tanpa melempar exception
			// (mis. proses lain menghapus folder tmp). Filedownload.save melempar NPE
			// bila file null. Cek eksplisit di sini mencegah NPE dan tetap menampilkan
			// kotak pesan ringkasan (lihat blok di bawah) walau unduhan gagal.
			if (file != null && file.exists() && file.length() > 0) {
				Filedownload.save(file, "text/plain");
				terunduh = true;
			}
		} catch (Exception e) {
			ErrorAuditUtil.record(e, "gagal-unduh-laporan src/ais/common/LaporanUpload.java:selesaikan");
		}

		String pesan = (adaMasalah() || getTotal() == 0 ? "Upload selesai, TETAPI ada data yang tidak tersimpan.\n\n"
				: "Upload selesai.\n\n") + ringkasan();
		if (terunduh) {
			pesan = pesan + "\n\nRincian per baris otomatis diunduh sebagai berkas teks.";
		} else {
			pesan = pesan + "\n\n(Berkas rincian gagal diunduh; hubungi Administrator bila diperlukan.)";
		}

		try {
			MyMessageboxConfig.show(pesan, "Hasil Upload", MyMessageboxConfig.OK,
					adaMasalah() || getTotal() == 0 ? MyMessageboxConfig.EXCLAMATION
							: MyMessageboxConfig.INFORMATION,
					setelahTutup);
		} catch (InterruptedException e) {
			// Thread event ZK diinterupsi saat menampilkan kotak pesan (mis. desktop ditutup).
			Thread.currentThread().interrupt();
			ErrorAuditUtil.record(e, "messagebox-interrupted src/ais/common/LaporanUpload.java:selesaikan");
		}
	}
}
