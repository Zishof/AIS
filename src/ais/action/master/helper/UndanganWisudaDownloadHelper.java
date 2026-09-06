package ais.action.master.helper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.zkoss.zul.Filedownload;

import ais.action.report.Report;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Skripsi;
import ais.database.model.Wisuda;
import ais.ui.util.MyMessageboxConfig;

/**
 * Menyiapkan undangan wisuda personal melalui Jasper.
 *
 * <p>Tidak ada arsip gambar peserta di dalam WAR. QR dibentuk di memori untuk
 * setiap permintaan dan langsung disematkan ke PDF Jasper. Dengan demikian
 * ukuran deployment tetap kecil dan QR selalu mengikuti data pendaftaran.</p>
 *
 * <p>Semua jalur unduh (window ZK {@link GenerateUndanganWisudaWindow}, halaman
 * mahasiswa, dan endpoint native
 * {@code ais.common.newui.akademik.NewUiUndanganWisudaController}) memanggil
 * {@link #download(PendaftaranWisuda)} atau {@link #generatePdf(PendaftaranWisuda)}
 * di kelas ini, sehingga validasi kelayakan tidak dapat berbeda antar tampilan.
 * Kelas ini sendiri tidak melakukan pemeriksaan identitas/kepemilikan pemanggil
 * (mis. apakah pengguna yang login berhak melihat {@code pendaftaranWisuda}
 * tertentu) — tanggung jawab itu ada pada pemanggil (lihat kontrak
 * {@code root/report} pada controller native, yang mensyaratkan sesi login dan
 * {@code NewUiRouteGuard.isActionAuthorized}). Nama berkas unduhan disaring lewat
 * {@link #amanNamaFile(String)} sehingga tidak ada bagian path/nama berkas yang
 * berasal langsung dari input pengguna tanpa disaring (tidak ada path traversal
 * karena template Jasper selalu konstanta {@link #TEMPLATE}, bukan parameter).</p>
 */
public final class UndanganWisudaDownloadHelper {

	/** Nama template Jasper undangan wisuda, dipakai oleh {@link #generatePdf(PendaftaranWisuda)}. */
	public static final String TEMPLATE = "Undangan_Wisuda";

	/** Locale Indonesia dipakai untuk memformat tanggal acara wisuda dalam Bahasa Indonesia. */
	private static final Locale LOCALE_INDONESIA = new Locale("id", "ID");

	/** Kelas utilitas statis; tidak boleh diinstansiasi. */
	private UndanganWisudaDownloadHelper() {
	}

	/**
	 * Menentukan apakah tombol/menu undangan boleh ditampilkan — yaitu setelah
	 * seluruh persetujuan (keuangan, administrasi, administrasi fakultas,
	 * perpustakaan, dan perpustakaan fakultas) pada {@code pendaftaranWisuda}
	 * berstatus disetujui. Ini murni pemeriksaan kelengkapan alur bisnis, bukan
	 * pemeriksaan otorisasi siapa yang boleh memanggilnya.
	 *
	 * @param pendaftaranWisuda pendaftaran wisuda yang akan diperiksa; {@code null}
	 *        atau tanpa id/mahasiswa dianggap belum layak
	 * @return {@code true} bila seluruh prasyarat persetujuan sudah lengkap
	 */
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

	/**
	 * Menerjemahkan kode status persetujuan (kolom integer) menjadi boolean.
	 * Konvensi pada entitas {@link PendaftaranWisuda}: nilai {@code 1} berarti
	 * disetujui; {@code null} atau nilai lain (mis. 0=pending, 2=ditolak)
	 * dianggap belum disetujui.
	 *
	 * @param status nilai kolom status persetujuan; boleh {@code null}
	 * @return {@code true} hanya bila {@code status} bernilai {@code 1}
	 */
	private static boolean disetujui(Integer status) {
		return Integer.valueOf(1).equals(status);
	}

	/**
	 * Alur unduh untuk konteks ZK (window/daftar admin dan halaman mahasiswa):
	 * memvalidasi kelayakan, merender PDF, lalu mengirimkannya ke browser lewat
	 * {@link Filedownload#save(java.io.InputStream, String, String)}. Kegagalan
	 * validasi ({@link IllegalArgumentException}) ditampilkan sebagai peringatan
	 * ramah-pengguna; kegagalan tak terduga lain dicatat ke
	 * {@code ais.common.ErrorAuditUtil} (pencatatan audit sendiri tidak boleh
	 * menggagalkan/menutupi kegagalan proses utama) lalu ditampilkan sebagai
	 * pesan generik agar detail internal tidak bocor ke pengguna.
	 *
	 * @param pendaftaranWisuda pendaftaran wisuda yang undangannya akan diunduh
	 * @throws Exception hanya diteruskan bila {@link #tampilkanPeringatan(String)}
	 *         sendiri gagal (mis. sesi ZK sudah tidak valid)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void download(PendaftaranWisuda pendaftaranWisuda) throws Exception {
		try {
			validasi(pendaftaranWisuda);
			File pdf = generatePdf(pendaftaranWisuda);
			String nim = amanNamaFile(pendaftaranWisuda.getMahasiswa().getNim());
			Filedownload.save(new FileInputStream(pdf), "application/pdf",
					"undangan_yudisium_" + (nim.length() == 0 ? pendaftaranWisuda.getId() : nim) + ".pdf");
		} catch (IllegalArgumentException e) {
			tampilkanPeringatan(e.getMessage());
		} catch (Exception e) {
			try {
				ais.common.ErrorAuditUtil.record(e, "UndanganWisudaDownloadHelper.download");
			} catch (Exception diabaikan) {
				// Pelaporan audit tidak boleh menutupi kegagalan laporan utama.
			}
			tampilkanPeringatan("Undangan belum dapat dibuat. Silakan coba kembali atau hubungi administrator sistem jika kendala berulang.");
		}
	}

	/**
	 * Merender PDF undangan wisuda ke berkas sementara di server tanpa
	 * mengirimkannya ke klien. Dipakai pula oleh endpoint native (yang mengirim
	 * hasil sebagai base64 JSON, bukan lewat {@code Filedownload} ZK) agar seluruh
	 * jalur memakai validasi, template, dan pembuatan QR yang sama persis.
	 *
	 * @param pendaftaranWisuda pendaftaran wisuda yang akan dirender
	 * @return berkas PDF sementara hasil render Jasper; pemanggil bertanggung
	 *         jawab membaca/menghapusnya sesuai kebutuhan
	 * @throws IllegalArgumentException bila {@code pendaftaranWisuda} belum layak
	 *         (lihat {@link #validasi(PendaftaranWisuda)})
	 * @throws Exception diteruskan dari proses render Jasper atau pembuatan QR
	 */
	public static File generatePdf(PendaftaranWisuda pendaftaranWisuda) throws Exception {
		validasi(pendaftaranWisuda);
		return Report.generateFileReportSimple(Report.PDF, buatParameters(pendaftaranWisuda), TEMPLATE);
	}

	/**
	 * Menyusun seluruh parameter Jasper untuk template {@link #TEMPLATE}: data
	 * peserta, nomor kursi, tahun akademik, tanggal/waktu/tempat acara, nama dan
	 * jabatan penandatangan (dari master {@link PerguruanTinggi} dengan fallback
	 * ke {@link Konfigurasi}), serta gambar QR undangan.
	 *
	 * @param pendaftaranWisuda pendaftaran wisuda sumber data; dipanggil hanya
	 *        setelah lolos {@link #validasi(PendaftaranWisuda)} sehingga
	 *        mahasiswa dan nomor kursi dijamin tersedia
	 * @return map parameter siap dikirim ke {@code Report.generateFileReportSimple}
	 * @throws Exception diteruskan dari pembuatan QR ({@link #buatQr(PendaftaranWisuda)})
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Map buatParameters(PendaftaranWisuda pendaftaranWisuda) throws Exception {
		Mahasiswa mahasiswa = pendaftaranWisuda.getMahasiswa();
		String noKursi = teks(pendaftaranWisuda.getNoKursi());
		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id_pendaftaran_wisuda", pendaftaranWisuda.getId());
		parameters.put("nama_peserta", namaPeserta(mahasiswa));
		parameters.put("nomor_kursi", nomorKursiTampil(noKursi));
		parameters.put("tahun_akademik", tahunAkademik(pendaftaranWisuda));
		parameters.put("tanggal_acara", tanggalAcara(pendaftaranWisuda.getWisuda()));
		parameters.put("waktu_acara", konfigurasi("undangan_wisuda_waktu_acara", "08.00 WITA - selesai"));
		parameters.put("tempat_acara", konfigurasi("undangan_wisuda_tempat_acara", "Aula Lantai 6 STIKSAM"));
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		String namaPerguruanTinggi = perguruanTinggi == null ? "Perguruan Tinggi"
				: teks(perguruanTinggi.getNama());
		String namaPimpinan = perguruanTinggi == null ? "" : teks(perguruanTinggi.getRektor());
		parameters.put("nama_penandatangan", konfigurasi("undangan_wisuda_nama_penandatangan",
				namaPimpinan.length() == 0 ? "Pimpinan Perguruan Tinggi" : namaPimpinan));
		parameters.put("jabatan_penandatangan", konfigurasi("undangan_wisuda_jabatan_penandatangan",
				"Ketua " + namaPerguruanTinggi));
		parameters.put("qrcode_undangan", buatQr(pendaftaranWisuda));
		parameters.put("tidak_tampil_pilihan_export", Boolean.TRUE);
		return parameters;
	}

	/**
	 * Gerbang fail-closed sebelum PDF dirender: menolak (via
	 * {@link IllegalArgumentException}, ditangkap sebagai peringatan ramah-pengguna
	 * oleh {@link #download(PendaftaranWisuda)}) apabila persetujuan belum lengkap
	 * atau nomor kursi belum tersedia. Dipanggil dari kedua pintu masuk publik
	 * ({@link #download(PendaftaranWisuda)} dan {@link #generatePdf(PendaftaranWisuda)})
	 * sehingga tidak ada jalur yang dapat melewati pemeriksaan ini.
	 *
	 * @param pendaftaranWisuda pendaftaran wisuda yang akan diperiksa
	 * @throws IllegalArgumentException dengan pesan yang menjelaskan syarat mana
	 *         yang belum terpenuhi
	 */
	private static void validasi(PendaftaranWisuda pendaftaranWisuda) {
		if (!disetujuiSemua(pendaftaranWisuda)) {
			throw new IllegalArgumentException(
					"Undangan belum dapat diunduh karena seluruh persetujuan pendaftaran wisuda belum selesai.");
		}
		if (teks(pendaftaranWisuda.getNoKursi()).length() == 0) {
			throw new IllegalArgumentException(
					"Undangan belum dapat dibuat karena nomor kursi mahasiswa belum tersedia. Silakan generate nomor kursi terlebih dahulu.");
		}
	}

	/**
	 * Membuat gambar QR (320x320, hitam-putih) berisi payload identifikasi
	 * undangan (kode aplikasi, jenis dokumen, id pendaftaran, NIM, nomor
	 * registrasi, nomor kursi, dan status). Setiap komponen teks disaring lewat
	 * {@link #amanQr(String)} agar tidak dapat menyisipkan pemisah {@code |} atau
	 * baris baru yang merusak format payload saat dipindai ulang.
	 *
	 * @param pendaftaranWisuda sumber data payload QR
	 * @return gambar QR siap disematkan sebagai parameter Jasper
	 * @throws Exception diteruskan dari {@link QRCodeWriter#encode}
	 */
	private static BufferedImage buatQr(PendaftaranWisuda pendaftaranWisuda) throws Exception {
		Mahasiswa mahasiswa = pendaftaranWisuda.getMahasiswa();
		String payload = "ECAMPUS|UNDANGAN-YUDISIUM"
				+ "|PENDAFTARAN=" + pendaftaranWisuda.getId()
				+ "|NIM=" + amanQr(mahasiswa == null ? null : mahasiswa.getNim())
				+ "|REGISTRASI=" + amanQr(pendaftaranWisuda.getNoRegistrasiWisuda())
				+ "|KURSI=" + amanQr(pendaftaranWisuda.getNoKursi())
				+ "|STATUS=DISETUJUI";
		BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 320, 320);
		BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < matrix.getHeight(); y++) {
			for (int x = 0; x < matrix.getWidth(); x++) {
				image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
			}
		}
		return image;
	}

	/**
	 * Menyaring nilai teks agar aman disisipkan ke payload QR yang dipisah karakter
	 * {@code |}: mengganti {@code |} dengan {@code /} dan baris baru dengan spasi.
	 *
	 * @param nilai nilai mentah; boleh {@code null}
	 * @return teks yang sudah disaring, tidak pernah {@code null}
	 */
	private static String amanQr(String nilai) {
		return teks(nilai).replace('|', '/').replace('\n', ' ').replace('\r', ' ');
	}

	/**
	 * Membentuk nama peserta yang ditampilkan pada undangan: nama mahasiswa,
	 * ditambah singkatan gelar dari {@link Jurusan#getSingkatanGelar()} bila ada
	 * (dipisah koma), mis. {@code "Budi Santoso, S.Kom"}.
	 *
	 * @param mahasiswa mahasiswa peserta wisuda; boleh {@code null}
	 * @return nama peserta siap tampil, tidak pernah {@code null}
	 */
	private static String namaPeserta(Mahasiswa mahasiswa) {
		String nama = teks(mahasiswa == null ? null : mahasiswa.getNama());
		Jurusan jurusan = mahasiswa == null ? null : mahasiswa.getJurusan();
		String gelar = teks(jurusan == null ? null : jurusan.getSingkatanGelar());
		return gelar.length() == 0 ? nama : nama + ", " + gelar;
	}

	/**
	 * Memformat nomor kursi mentah menjadi tampilan 3 digit dengan nol di depan
	 * (mis. {@code "7"} menjadi {@code "007"}), setelah membuang nol di depan yang
	 * berlebih pada input. Nilai yang bukan murni digit (setelah pembuangan nol
	 * depan) dikembalikan apa adanya tanpa diformat ulang.
	 *
	 * @param nomorKursi nomor kursi mentah dari {@link PendaftaranWisuda#getNoKursi()},
	 *        tidak boleh {@code null} (pemanggil sudah memvalidasi via {@link #teks(String)})
	 * @return nomor kursi terformat untuk tampilan pada undangan
	 */
	private static String nomorKursiTampil(String nomorKursi) {
		String angka = nomorKursi.replaceFirst("^0+(?!$)", "");
		if (!angka.matches("[0-9]+")) {
			return nomorKursi;
		}
		while (angka.length() < 3) {
			angka = "0" + angka;
		}
		return angka;
	}

	/**
	 * Menentukan tahun akademik yang ditampilkan pada undangan: diambil dari
	 * skripsi mahasiswa bila tersedia, jika tidak jatuh ke tahun akademik berjalan
	 * ({@link Common#getCurrentTahunAkademik()}).
	 *
	 * @param pendaftaranWisuda pendaftaran wisuda sumber data skripsi
	 * @return tahun akademik untuk ditampilkan, tidak pernah {@code null}
	 */
	private static String tahunAkademik(PendaftaranWisuda pendaftaranWisuda) {
		Skripsi skripsi = pendaftaranWisuda.getSkripsi();
		String tahun = skripsi == null ? "" : teks(skripsi.getTahunAkademik());
		return tahun.length() == 0 ? Common.getCurrentTahunAkademik() : tahun;
	}

	/**
	 * Memformat tanggal acara wisuda dalam Bahasa Indonesia (mis.
	 * {@code "Sabtu, 12 September 2026"}) menggunakan {@link #LOCALE_INDONESIA}.
	 *
	 * @param wisuda entitas acara wisuda; boleh {@code null} atau belum
	 *        memiliki tanggal
	 * @return tanggal terformat, atau {@code "Belum ditentukan"} bila tanggal
	 *         acara belum diisi
	 */
	private static String tanggalAcara(Wisuda wisuda) {
		Date tanggal = wisuda == null ? null : wisuda.getTanggal();
		return tanggal == null ? "Belum ditentukan"
				: new SimpleDateFormat("EEEE, dd MMMM yyyy", LOCALE_INDONESIA).format(tanggal);
	}

	/**
	 * Mengambil nilai konfigurasi bertipe teks dari {@link Common#getKonfigurasi},
	 * jatuh ke {@code nilaiDefault} bila konfigurasi belum ada, kosong, atau
	 * pengambilannya gagal. Catatan: {@code Common#getKonfigurasi} sendiri
	 * bersifat auto-seed — pemanggilan pertama untuk {@code nama} yang belum ada
	 * akan menuliskan {@code nilaiDefault} sebagai baris konfigurasi baru ke DB.
	 *
	 * @param nama kunci konfigurasi
	 * @param nilaiDefault nilai jatuh bila konfigurasi tidak ditemukan/kosong/gagal
	 * @return nilai konfigurasi efektif, tidak pernah {@code null}
	 */
	private static String konfigurasi(String nama, String nilaiDefault) {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi(nama, nilaiDefault);
			String nilai = konfigurasi == null ? "" : teks(konfigurasi.getNilai());
			return nilai.length() == 0 ? nilaiDefault : nilai;
		} catch (Exception e) {
			return nilaiDefault;
		}
	}

	/**
	 * Menormalkan nilai teks: {@code null} menjadi string kosong, selain itu
	 * di-{@code trim()}. Dipakai di seluruh kelas ini untuk menghindari
	 * pengulangan pemeriksaan null.
	 *
	 * @param nilai nilai mentah; boleh {@code null}
	 * @return teks yang sudah dinormalkan, tidak pernah {@code null}
	 */
	private static String teks(String nilai) {
		return nilai == null ? "" : nilai.trim();
	}

	/**
	 * Menyaring nilai menjadi nama berkas yang aman: hanya huruf, angka, titik,
	 * garis bawah, dan tanda hubung yang dipertahankan; karakter lain (termasuk
	 * pemisah path {@code /} dan {@code \}, atau urutan {@code ..}) diganti garis
	 * bawah. Ini mencegah nama berkas hasil unduhan disisipi path traversal
	 * meskipun nilainya berasal dari data mahasiswa (NIM).
	 *
	 * @param nilai nilai mentah (biasanya NIM); boleh {@code null}
	 * @return nama berkas yang aman dipakai sebagai nama unduhan, tidak pernah {@code null}
	 */
	private static String amanNamaFile(String nilai) {
		return teks(nilai).replaceAll("[^A-Za-z0-9._-]+", "_");
	}

	/**
	 * Menampilkan pesan peringatan modal ke pengguna ZK yang sedang mengunduh
	 * undangan, dipakai baik untuk kegagalan validasi maupun kegagalan tak
	 * terduga pada {@link #download(PendaftaranWisuda)}.
	 *
	 * @param pesan isi pesan yang ditampilkan ke pengguna
	 * @throws Exception diteruskan dari {@link MyMessageboxConfig#show}
	 */
	private static void tampilkanPeringatan(String pesan) throws Exception {
		MyMessageboxConfig.show(pesan, "Undangan Wisuda", MyMessageboxConfig.OK,
				MyMessageboxConfig.EXCLAMATION);
	}
}
