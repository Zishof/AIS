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

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.action.master.sekolah.util.PembayaranSiswaUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.WaktuUtil;

/**
 * Servlet publik yang menghasilkan berkas PDF "struk pembayaran" (bukti bayar siswa/calon
 * siswa) dari {@link PembayaranSiswa} yang ditunjuk parameter {@code id}, lalu menuliskannya
 * langsung sebagai unduhan {@code application/pdf} pada respons.
 *
 * <h4>Keamanan &mdash; STATUS TERKINI (diverifikasi ulang dari kode berjalan, 2026-09-07)</h4>
 * <p>Endpoint ini <b>MASIH sepenuhnya anonim</b>: {@link #doGet}/{@link #doPost} memanggil
 * {@link #process} langsung tanpa pemeriksaan sesi/login apa pun &mdash; tidak ada pembacaan
 * {@link javax.servlet.http.HttpSession}, tidak ada pemanggilan {@code Common.getCurrentUser()},
 * dan tidak ada {@code intercept-url} khusus untuk {@code /Struk} pada
 * {@code applicationContext-security.xml}. Ini konsisten dengan catatan lama yang menyebut
 * servlet ini sebagai "endpoint anonim ke-5" pada klaster {@code task_493423ef}, bersama
 * beberapa servlet {@code AmbilLaporan*} lain yang juga belum digerbangi.</p>
 * <p>Parameter {@code id} diterima dalam DUA bentuk yang SAMA-SAMA berfungsi (lihat
 * {@link #process}):</p>
 * <ul>
 *   <li>{@code id=<angka>} &mdash; dipakai LANGSUNG sebagai primary key {@code PembayaranSiswa}
 *       lewat {@code Restrictions.idEq(Long.parseLong(myid))}. Primary key ini sekuensial
 *       (auto-increment basis data), sehingga seluruh riwayat pembayaran siswa/calon siswa
 *       di sistem dapat DIENUMERASI hanya dengan menaik-turunkan angka pada URL &mdash; id
 *       MASIH mudah ditebak.</li>
 *   <li>{@code id=EE<token>} &mdash; token didekripsi lebih dulu lewat
 *       {@code Common.desEncrypter.get().decrypt(...)}; namun kegagalan dekripsi ditelan
 *       (blok {@code catch} kosong) dan {@code myid} MENTAH tetap dipakai apa adanya pada
 *       baris berikutnya. Jalur terenkripsi ini TIDAK menutup jalur angka mentah di atas;
 *       keduanya tetap berfungsi berdampingan.</li>
 * </ul>
 * <p>Payload PDF yang dihasilkan memuat PII lengkap milik {@link CalonSiswa} atau
 * {@link Siswa} pemilik pembayaran (lewat {@code Common.insertProperty}, yang menyalin
 * SELURUH properti bean orang tersebut ke parameter laporan) DITAMBAH rincian finansial
 * lengkap dari {@code PembayaranSiswaUtil.dataPembayaran(...)} &mdash; kombinasi PII plus
 * data finansial lengkap yang sama persis dengan pola yang sudah dicatat pada
 * {@code task_493423ef}.</p>
 * <p><b>Kesimpulan verifikasi:</b> ketiga karakteristik yang dicatat sebelumnya &mdash;
 * (1) endpoint anonim, (2) id sekuensial/mudah ditebak, (3) payload PII+finansial lengkap
 * &mdash; SEMUANYA masih berlaku pada revisi kode saat ini. Tidak ditemukan perubahan yang
 * mengubah status ini sejak terakhir dicatat; dokumentasi ini adalah konfirmasi ulang, bukan
 * temuan baru.</p>
 *
 * @see HttpServlet
 * @see PembayaranSiswa
 */
public class Struk extends HttpServlet {
	/**
	 * Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai secara fungsional karena
	 * instance servlet tidak pernah diserialisasi oleh kontainer pada penyebaran AIS.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet. Tidak melakukan
	 * inisialisasi apa pun; seluruh state diambil per-permintaan di {@link #process}.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public Struk() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan meneruskannya ke {@link #process}. Tidak ada
	 * gerbang otentikasi/otorisasi di sini maupun di {@link #process} &mdash; lihat bagian
	 * Keamanan pada dokumentasi kelas.
	 *
	 * @param request  permintaan masuk berisi parameter {@code id}
	 * @param response balasan yang akan diisi bita berkas PDF struk pembayaran
	 * @throws ServletException tidak pernah dilempar keluar method ini; kegagalan
	 *                          {@link #process} ditelan {@link Common#tampilErrorJikaAdmin(Exception)}
	 * @throws IOException      tidak pernah dilempar keluar method ini, dengan alasan yang sama
	 * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse)
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
	 * Menangani permintaan HTTP POST dengan perilaku identik {@link #doGet}, termasuk tidak
	 * adanya gerbang otentikasi/otorisasi.
	 *
	 * @param request  permintaan masuk berisi parameter {@code id}
	 * @param response balasan yang akan diisi bita berkas PDF struk pembayaran
	 * @throws ServletException tidak pernah dilempar keluar method ini
	 * @throws IOException      tidak pernah dilempar keluar method ini
	 * @see HttpServlet#doPost(HttpServletRequest, HttpServletResponse)
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
	 * Inti servlet: menerjemahkan parameter {@code id} menjadi sebuah {@link PembayaranSiswa},
	 * membangun parameter laporan PDF-nya (PII pemilik pembayaran + rincian finansial), lalu
	 * menyalin berkas PDF yang dihasilkan ke {@code resp} sebagai unduhan.
	 *
	 * <h4>Urutan kerja</h4>
	 * <ol>
	 *   <li>parameter {@code id} dibaca; bila berawalan {@code "EE"}, sisanya didekripsi lewat
	 *       {@code Common.desEncrypter.get().decrypt(...)} &mdash; kegagalan pada langkah ini
	 *       ditelan (dicatat lewat {@code ErrorAuditUtil.record}) dan nilai mentah tetap
	 *       dipakai;</li>
	 *   <li>{@code myid} diurai sebagai {@code Long} dan dipakai LANGSUNG sebagai primary key
	 *       pencarian {@link PembayaranSiswa} (tanpa penyaring kepemilikan/sesi apa pun);</li>
	 *   <li>bila {@code pembayaranSiswa} ditemukan, parameter laporan diisi dari propertinya,
	 *       dari {@link CalonSiswa}/{@link Siswa} pemiliknya (seluruh properti bean, lewat
	 *       {@code Common.insertProperty}), dan dari {@code PembayaranSiswaUtil.dataPembayaran};</li>
	 *   <li>laporan {@code sekolah/struk_pembayaran} dirender ke PDF lewat
	 *       {@link Report#generateFileReport} lalu disalin ke {@code resp} dengan penyangga
	 *       1&nbsp;KiB.</li>
	 * </ol>
	 * <p>Tidak ada gerbang otentikasi/otorisasi pada method ini &mdash; lihat bagian Keamanan
	 * pada dokumentasi kelas untuk rincian dan konfirmasi status terkini.</p>
	 * <p>Bila {@code pembayaranSiswa} bernilai {@code null} (id tidak ditemukan/tidak valid),
	 * pemanggilan {@code pembayaranSiswa.getCalonSiswa()} pada langkah pengisian PII akan
	 * melempar {@link NullPointerException} yang menembus ke {@link #doGet}/{@link #doPost}
	 * dan ditelan di sana; permintaan dengan id tak dikenal akan gagal senyap tanpa berkas.</p>
	 *
	 * @param request permintaan masuk berisi parameter {@code id}
	 * @param resp    balasan yang akan diisi bita berkas PDF struk pembayaran
	 * @throws Exception bila parsing id, pencarian data, atau pembangunan laporan gagal
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void process(HttpServletRequest request, HttpServletResponse resp) throws Exception {
		String myid = request.getParameter("id");

		try {
			if (myid.startsWith("EE")) {
				myid = Common.desEncrypter.get().decrypt(myid.substring(2));
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Struk.java:77");
		}

		System.out.println("myid = " + myid);

		PembayaranSiswa pembayaranSiswa = null;
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			pembayaranSiswa = (PembayaranSiswa) session.createCriteria(PembayaranSiswa.class)
					.add(Restrictions.idEq(Long.parseLong(myid))).uniqueResult();
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Struk.java:90");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Struk.java:91");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Struk.java:92");}
			}
		}

		Map parameters = new HashMap();
		parameters.put("id_pembayaran", pembayaranSiswa == null || pembayaranSiswa.getId() == null ? -1L : pembayaranSiswa.getId());
		parameters.put("id_sekolah", pembayaranSiswa == null ? -1L : pembayaranSiswa.getSekolah().getId());
		parameters.put("id_bri", -1L);
		parameters.put("id_bni", -1L);
		parameters.put("id_bsi", -1L);
		parameters.put("id_va", -1L);

		String kode_transaksi = "0000000000000000000000" + (pembayaranSiswa == null ? "" : pembayaranSiswa.getId());
		kode_transaksi = StringUtils.substring(kode_transaksi, kode_transaksi.length() - 8);
		parameters.put("kode_transaksi", kode_transaksi);

		parameters.put("waktu_cetak", pembayaranSiswa == null ? Common.dateFormat1.get().format(WaktuUtil.getDate())
				: Common.dateFormat1.get().format(pembayaranSiswa.getTanggal()));

		if (pembayaranSiswa.getCalonSiswa() != null) {
			Common.insertProperty(CalonSiswa.class, pembayaranSiswa.getCalonSiswa(), parameters, "");
		} else if (pembayaranSiswa.getSiswa() != null) {
			Common.insertProperty(Siswa.class, pembayaranSiswa.getSiswa(), parameters, "");
		}

		PembayaranSiswaUtil.dataPembayaran(pembayaranSiswa, null, null, null, null, parameters);

		File file = Report.generateFileReport(Report.PDF, parameters, "sekolah/struk_pembayaran",
				ais.ui.util.WaktuUtil.getDate(), Common.locale);

		resp.setContentType("application/pdf");
		resp.setHeader("Content-Disposition", "attachment; filename=\"struk_pembayaran.pdf\"");

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
