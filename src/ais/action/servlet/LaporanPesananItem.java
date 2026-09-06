package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.action.report.Report;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.PesananAnggota;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

/**
 * Servlet pencetak laporan PDF gabungan untuk satu atau beberapa {@link PesananAnggota}
 * (pesanan item perpustakaan oleh anggota) sekaligus, lengkap dengan barcode Code128 dari
 * kode masing-masing pesanan. Berbeda dari {@link AmbilLaporanPerpustakaan} yang mencetak
 * satu transaksi per panggilan, servlet ini menerima daftar id dipisah koma dan menggabungkan
 * semuanya ke satu berkas PDF unduhan.
 *
 * <p><b>Keamanan:</b> endpoint ini tidak terdaftar eksplisit di
 * {@code applicationContext-security.xml}, sehingga tunduk pada aturan tangkapan-semua
 * {@code IS_AUTHENTICATED_ANONYMOUSLY} — dapat diakses tanpa login. Parameter {@code id}
 * pada {@link #process} diterima sebagai daftar primary key mentah tanpa pengecekan
 * kepemilikan/cabang, sehingga siapa pun yang mengetahui atau menebak id pesanan dapat
 * mengunduh laporannya. Pola gerbang otentikasi yang hilang pada servlet {@code Ambil*}/
 * {@code Laporan*} semacam ini sudah tercatat sebagai isu berulang di modul lain.</p>
 *
 * @see HttpServlet
 */
public class LaporanPesananItem extends HttpServlet {
	/** Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} servlet ini. */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor default tanpa argumen, hanya meneruskan ke {@link HttpServlet#HttpServlet()}.
	 * Tidak ada state khusus yang diinisialisasi di sini.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public LaporanPesananItem() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani GET dengan mendelegasikan ke {@link #process}; kegagalan apa pun ditelan dan
	 * hanya ditampilkan ke pengguna bila konteks saat ini adalah administrator, lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param request  request HTTP masuk; parameter {@code id} (daftar id dipisah koma)
	 *                 menentukan pesanan mana saja yang dicetak, lihat {@link #process}
	 * @param response response HTTP keluar; diisi berkas PDF unduhan oleh {@link #process}
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
	 * @param response response HTTP keluar; diisi berkas PDF unduhan oleh {@link #process}
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
	 * Membentuk satu berkas PDF gabungan berisi barcode dan ringkasan tiap
	 * {@link PesananAnggota} yang id-nya diminta, lalu menuliskannya sebagai unduhan
	 * ({@code Content-Disposition: attachment}) ke {@code response}.
	 *
	 * <p>Alur: (1) baca parameter {@code id}, pecah per koma, dan parse setiap bagian sebagai
	 * {@code Long} tanpa validasi lanjutan (nilai non-numerik akan melempar
	 * {@code NumberFormatException} yang berujung {@code Exception} ke pemanggil); (2) buka
	 * sesi Hibernate baru dan muat seluruh {@link PesananAnggota} yang id-nya termasuk dalam
	 * daftar tersebut; (3) untuk tiap pesanan, susun teks ringkasan (anggota, perpustakaan,
	 * ISBN/nama item, status, tanggal) dan pastikan berkas PNG barcode Code128 dari kode
	 * pesanan sudah ada di direktori {@code /report} pada konteks servlet (dibuat sekali saja
	 * bila belum ada — nama berkas memakai kode pesanan, BUKAN input bebas klien); (4) sesi
	 * selalu dibersihkan (clear/disconnect/close) di blok {@code finally}; (5) panggil
	 * {@link Report#generateFileReport} dengan template {@code "library/pesanan"} dan daftar
	 * peta hasil langkah (3); (6) tulis berkas PDF hasil ke {@code response} lewat aliran byte
	 * biasa dengan header {@code Content-Disposition} memaksa unduhan bernama
	 * {@code pesanan_anggota.pdf}.</p>
	 *
	 * @param request  request HTTP masuk; parameter {@code id} berisi daftar primary key
	 *                 {@link PesananAnggota} dipisah koma
	 * @param resp     response HTTP keluar; content-type diatur ke {@code application/pdf},
	 *                 badan berisi berkas PDF gabungan
	 * @throws Exception bila {@code id} kosong/null (menyebabkan {@code NullPointerException}
	 *                    pada {@code split}), bila salah satu bagian bukan angka valid, bila
	 *                    pembuatan barcode/laporan gagal, atau bila penulisan respons gagal
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void process(HttpServletRequest request, HttpServletResponse resp) throws Exception {
		String myid = request.getParameter("id");
		System.out.println("myid = " + myid);
		String[] strids = myid.split(",");
		List<Long> ids = new ArrayList<Long>();
		for (String strid : strids) {
			Long id = Long.parseLong(strid.trim());
			ids.add(id);
		}
		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			List<PesananAnggota> pesananAnggotas = session.createCriteria(PesananAnggota.class)
					.add(Restrictions.in("id", ids)).list();
			for (PesananAnggota pesananAnggota : pesananAnggotas) {

				if (pesananAnggota != null) {

					Map<String, Object> map = new java.util.HashMap<String, Object>();
					String code = pesananAnggota.getAnggota().toString()
							+ (pesananAnggota.getPerpustakaan() == null ? ""
									: "\n" + pesananAnggota.getPerpustakaan().getNama())
							+ "\n" + pesananAnggota.getItem().getIsbn() + " - " + pesananAnggota.getItem().getNama()
							+ "\nStatus : " + pesananAnggota.getStatus() + " - "
							+ Common.dateFormat5.get().format(pesananAnggota.getTanggal());
					map.put("code", code);

					@SuppressWarnings("deprecation")
					final File myfile = new File(
							request.getRealPath("/report") + "/barcode_" + pesananAnggota.getKode() + ".png");

					if (!myfile.exists()) {
						Barcode mybarcode = BarcodeFactory.createCode128(pesananAnggota.getKode());
						BarcodeImageHandler.savePNG(mybarcode, myfile);
					}

					map.put("barcode", myfile.getAbsolutePath());
					map.put("c_code", pesananAnggota.getKode());
					maps.add(map);

				}
			}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/LaporanPesananItem.java:113");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/LaporanPesananItem.java:114");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/LaporanPesananItem.java:115");}
			}
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("maps", maps);

		File file = Report.generateFileReport(Report.PDF, parameters, "library/pesanan",
				ais.ui.util.WaktuUtil.getDate(), Common.locale);

		resp.setContentType("application/pdf");
		resp.setHeader("Content-Disposition", "attachment; filename=\"pesanan_anggota.pdf\"");

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
