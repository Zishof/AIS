package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.action.master.library.util.LibraryUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.KembaliPengadaanItem;
import ais.database.model.library.PeminjamanPengadaanItem;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

/**
 * Servlet pencetak laporan perpustakaan (bukti peminjaman, perpanjangan, dan pengembalian
 * item pengadaan) dalam format PDF atau gambar, lengkap dengan barcode Code128 dari kode
 * transaksi. Dipetakan sebagai endpoint tersendiri (bukan lewat dispatcher aksi ZK), sehingga
 * dapat ditaut langsung sebagai URL cetak/unduh dari halaman sirkulasi perpustakaan.
 *
 * <p><b>Keamanan:</b> endpoint ini tidak eksplisit terdaftar di
 * {@code applicationContext-security.xml} (berbeda dengan mis. {@code /AmbilLaporanMahasiswa}
 * yang diberi {@code IS_AUTHENTICATED_REMEMBERED}), sehingga tunduk pada aturan
 * tangkapan-semua {@code IS_AUTHENTICATED_ANONYMOUSLY} — dapat diakses tanpa login. Karena
 * {@code id} pada {@link #process} adalah primary key yang diterima mentah dari parameter
 * request tanpa pengecekan kepemilikan/cabang, siapa pun yang mengetahui atau menebak sebuah
 * id transaksi peminjaman/pengembalian dapat mengunduh laporannya (memuat ringkasan
 * peminjaman dari {@link LibraryUtil#tampilanSummaryPeminjaman}). Pola gerbang otentikasi
 * yang hilang pada servlet {@code Ambil*} semacam ini sudah tercatat sebagai isu berulang di
 * modul lain.</p>
 *
 * @see HttpServlet
 */
public class AmbilLaporanPerpustakaan extends HttpServlet {
	/** Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} servlet ini. */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor default tanpa argumen, hanya meneruskan ke {@link HttpServlet#HttpServlet()}.
	 * Tidak ada state khusus yang diinisialisasi di sini.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilLaporanPerpustakaan() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani GET dengan mendelegasikan ke {@link #process}; kegagalan apa pun ditelan dan
	 * hanya ditampilkan ke pengguna bila konteks saat ini adalah administrator, lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param request  request HTTP masuk; parameter {@code laporan}/{@code type}/{@code id}/
	 *                 {@code locale} menentukan laporan yang dicetak, lihat {@link #process}
	 * @param response response HTTP keluar; diisi berkas PDF atau gambar oleh {@link #process}
	 * @throws ServletException tidak pernah dilempar keluar karena {@link #process} dibungkus
	 *                          try/catch di sini; dipertahankan hanya karena tanda tangan
	 *                          {@link HttpServlet#doGet}
	 * @throws IOException      idem, ditelan oleh blok catch
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menangani POST dengan perilaku identik seperti {@link #doGet}: mendelegasikan ke
	 * {@link #process} dan menelan kegagalan lewat {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param request  request HTTP masuk; parameter sama seperti pada {@link #doGet}
	 * @param response response HTTP keluar; diisi berkas PDF atau gambar oleh {@link #process}
	 * @throws ServletException tidak pernah dilempar keluar, lihat catatan pada {@link #doGet}
	 * @throws IOException      idem
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membentuk dan menuliskan laporan perpustakaan (peminjaman/perpanjangan/pengembalian)
	 * sebagai PDF atau gambar langsung ke {@code response}, lengkap dengan barcode Code128
	 * dari kode transaksi yang bersangkutan.
	 *
	 * <p>Alur: (1) baca parameter {@code laporan} (nama laporan: {@code peminjaman},
	 * {@code perpanjangan}, atau {@code pengembalian}), {@code type} ({@code img} untuk
	 * gambar, selain itu PDF), {@code id} (primary key transaksi, di-{@code parseLong} tanpa
	 * validasi lanjutan), dan {@code locale}; (2) buka sesi Hibernate baru dan, sesuai nama
	 * laporan, muat {@link PeminjamanPengadaanItem} atau {@link KembaliPengadaanItem} dengan
	 * id tersebut, buat berkas PNG barcode Code128 dari kodenya ke direktori
	 * {@code /report} pada konteks servlet (nama berkas memakai kode transaksi, BUKAN input
	 * bebas klien), dan siapkan parameter laporan termasuk ringkasan peminjaman dari
	 * {@link LibraryUtil#tampilanSummaryPeminjaman}; (3) sesi selalu dibersihkan
	 * (clear/disconnect/close) di blok {@code finally}; (4) panggil
	 * {@link Report#generateFileImageReport} atau {@link Report#generateFileReport} dengan
	 * nama template {@code "library/" + namaLaporan} — bila {@code namaLaporan} tidak cocok
	 * salah satu dari ketiga nilai yang dikenali, langkah (2) tidak mengisi data apa pun namun
	 * generator laporan tetap dipanggil dengan nama template turunan parameter tersebut; (5)
	 * tulis berkas hasil (PDF atau JPEG) ke {@code response} lewat aliran byte biasa.</p>
	 *
	 * @param request  request HTTP masuk; parameter {@code laporan}, {@code type}, {@code id},
	 *                 dan {@code locale} menentukan jenis dan isi laporan
	 * @param resp     response HTTP keluar; content-type diatur ke {@code image/jpeg} atau
	 *                 {@code application/pdf} sesuai {@code type}, badan berisi berkas laporan
	 * @throws Exception bila {@code id} bukan angka valid, bila entity tidak ditemukan
	 *                    (menyebabkan {@code NullPointerException} saat mengakses kodenya),
	 *                    bila pembuatan barcode/laporan gagal, atau bila penulisan respons
	 *                    gagal
	 */
	private void process(HttpServletRequest request, HttpServletResponse resp) throws Exception {
		String namaLaporan = request.getParameter("laporan");
		String type = request.getParameter("type");
		Long id = Long.parseLong(request.getParameter("id"));
		String locale = request.getParameter("locale");

		String kodeBarcode = "";
		Map<String, Object> parameters = new HashMap<String, Object>();
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			if (namaLaporan.equalsIgnoreCase("peminjaman")) {
				final PeminjamanPengadaanItem peminjamanPengadaanItem = (PeminjamanPengadaanItem) session
						.createCriteria(PeminjamanPengadaanItem.class).add(Restrictions.idEq(id)).uniqueResult();
				final File myfilebarcode = new File(getServletContext().getRealPath("/report") + "/barcode_"
						+ peminjamanPengadaanItem.getKode() + ".png");
				Barcode mybarcode = BarcodeFactory.createCode128B(peminjamanPengadaanItem.getKode());
				BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
				kodeBarcode = myfilebarcode.getAbsolutePath();

				parameters.put("info", LibraryUtil.tampilanSummaryPeminjaman(
						peminjamanPengadaanItem.getKembaliPengadaanItem(), peminjamanPengadaanItem));

			} else if (namaLaporan.equalsIgnoreCase("perpanjangan")) {
				final PeminjamanPengadaanItem peminjamanPengadaanItem = (PeminjamanPengadaanItem) session
						.createCriteria(PeminjamanPengadaanItem.class).add(Restrictions.idEq(id)).uniqueResult();
				final File myfilebarcode = new File(getServletContext().getRealPath("/report") + "/barcode_"
						+ peminjamanPengadaanItem.getKode() + ".png");
				Barcode mybarcode = BarcodeFactory.createCode128B(peminjamanPengadaanItem.getKode());
				BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
				kodeBarcode = myfilebarcode.getAbsolutePath();

				parameters.put("info", LibraryUtil.tampilanSummaryPeminjaman(
						peminjamanPengadaanItem.getKembaliPengadaanItem(), peminjamanPengadaanItem));

			} else if (namaLaporan.equalsIgnoreCase("pengembalian")) {
				final KembaliPengadaanItem kembaliPengadaanItem = (KembaliPengadaanItem) session
						.createCriteria(KembaliPengadaanItem.class).add(Restrictions.idEq(id)).uniqueResult();
				final File myfilebarcode = new File(
						getServletContext().getRealPath("/report") + "/barcode_" + kembaliPengadaanItem.getKode() + ".png");
				Barcode mybarcode = BarcodeFactory.createCode128B(kembaliPengadaanItem.getKode());
				BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
				kodeBarcode = myfilebarcode.getAbsolutePath();

				parameters.put("info", LibraryUtil.tampilanSummaryPeminjaman(kembaliPengadaanItem,
						kembaliPengadaanItem.getPeminjamanPengadaanItem()));

			}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanPerpustakaan.java:118");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanPerpustakaan.java:119");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanPerpustakaan.java:120");}
			}
		}

		parameters.put("id", id);
		parameters.put("kode_barcode", kodeBarcode);

		File file = null;
		if (type.equals("img")) {
			file = Report.generateFileImageReport(Report.PDF, parameters, "library/" + namaLaporan,
					ais.ui.util.WaktuUtil.getDate(),
					locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
		} else {
			file = Report.generateFileReport(Report.PDF, parameters, "library/" + namaLaporan,
					ais.ui.util.WaktuUtil.getDate(),
					locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
		}

		if (type.equals("img")) {
			resp.setContentType("image/jpeg");
		} else {
			resp.setContentType("application/pdf");
		}

		ServletOutputStream out = resp.getOutputStream();
		FileInputStream in = new FileInputStream(file);
		int length = (int) file.length();

		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];

		while ((length = in.read(buffer)) != -1) {
			out.write(buffer, 0, length);
		}

		in.close();
		out.flush();

	}

}
