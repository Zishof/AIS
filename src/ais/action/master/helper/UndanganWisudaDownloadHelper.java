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
 * Tidak ada arsip gambar peserta di dalam WAR. QR dibentuk di memori untuk
 * setiap permintaan dan langsung disematkan ke PDF Jasper. Dengan demikian
 * ukuran deployment tetap kecil dan QR selalu mengikuti data pendaftaran.
 */
public final class UndanganWisudaDownloadHelper {

	public static final String TEMPLATE = "Undangan_Wisuda";
	private static final Locale LOCALE_INDONESIA = new Locale("id", "ID");

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

	/** Dipakai pula oleh endpoint native agar seluruh jalur memakai template dan QR yang sama. */
	public static File generatePdf(PendaftaranWisuda pendaftaranWisuda) throws Exception {
		validasi(pendaftaranWisuda);
		return Report.generateFileReportSimple(Report.PDF, buatParameters(pendaftaranWisuda), TEMPLATE);
	}

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

	private static String amanQr(String nilai) {
		return teks(nilai).replace('|', '/').replace('\n', ' ').replace('\r', ' ');
	}

	private static String namaPeserta(Mahasiswa mahasiswa) {
		String nama = teks(mahasiswa == null ? null : mahasiswa.getNama());
		Jurusan jurusan = mahasiswa == null ? null : mahasiswa.getJurusan();
		String gelar = teks(jurusan == null ? null : jurusan.getSingkatanGelar());
		return gelar.length() == 0 ? nama : nama + ", " + gelar;
	}

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

	private static String tahunAkademik(PendaftaranWisuda pendaftaranWisuda) {
		Skripsi skripsi = pendaftaranWisuda.getSkripsi();
		String tahun = skripsi == null ? "" : teks(skripsi.getTahunAkademik());
		return tahun.length() == 0 ? Common.getCurrentTahunAkademik() : tahun;
	}

	private static String tanggalAcara(Wisuda wisuda) {
		Date tanggal = wisuda == null ? null : wisuda.getTanggal();
		return tanggal == null ? "Belum ditentukan"
				: new SimpleDateFormat("EEEE, dd MMMM yyyy", LOCALE_INDONESIA).format(tanggal);
	}

	private static String konfigurasi(String nama, String nilaiDefault) {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi(nama, nilaiDefault);
			String nilai = konfigurasi == null ? "" : teks(konfigurasi.getNilai());
			return nilai.length() == 0 ? nilaiDefault : nilai;
		} catch (Exception e) {
			return nilaiDefault;
		}
	}

	private static String teks(String nilai) {
		return nilai == null ? "" : nilai.trim();
	}

	private static String amanNamaFile(String nilai) {
		return teks(nilai).replaceAll("[^A-Za-z0-9._-]+", "_");
	}

	private static void tampilkanPeringatan(String pesan) throws Exception {
		MyMessageboxConfig.show(pesan, "Undangan Wisuda", MyMessageboxConfig.OK,
				MyMessageboxConfig.EXCLAMATION);
	}
}
