package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Kegiatan;

/**
 * Servlet publik yang menghasilkan berkas PDF "struk pembayaran" dari {@link Kegiatan}
 * (bukti bayar kegiatan/pesanan mahasiswa) yang ditunjuk parameter {@code id}, lalu
 * menuliskannya langsung sebagai unduhan {@code application/pdf} pada respons. "M" pada nama
 * kelas menandakan varian mobile/alternatif dari {@link Struk} (yang bekerja pada
 * {@link ais.database.model.sekolah.PembayaranSiswa}, bukan {@link Kegiatan}).
 *
 * <h4>Keamanan &mdash; PERBANDINGAN DENGAN {@link Struk} (diverifikasi dari kode berjalan,
 * 2026-09-07)</h4>
 * <p><b>Sama seperti {@link Struk}:</b> endpoint ini sepenuhnya ANONIM &mdash;
 * {@link #doGet}/{@link #doPost} memanggil {@link #process} langsung tanpa pemeriksaan
 * sesi/login apa pun, dan tidak ada {@code intercept-url} khusus untuk {@code /StrukM} pada
 * {@code applicationContext-security.xml} (jatuh ke katalog {@code /**} ber-akses
 * {@code IS_AUTHENTICATED_ANONYMOUSLY}). Parameter {@code id} juga menerima dua bentuk yang
 * sama: angka mentah (primary key {@link Kegiatan} sekuensial, dipakai langsung lewat
 * {@code Restrictions.idEq}, sehingga tetap dapat DIENUMERASI) atau {@code id=EE<token>}
 * (didekripsi lewat {@code Common.desEncrypter}, dengan kegagalan dekripsi yang ditelan sama
 * seperti {@link Struk}). Berkas PDF yang dihasilkan ({@code CommonReportHelper
 * .cetakBuktipembayaranMahasiswa}) juga berpotensi memuat PII/rincian finansial pemilik
 * pembayaran, sama seperti {@link Struk}.</p>
 * <p><b>Berbeda dari {@link Struk}:</b> method {@link #process} pada kelas ini sudah
 * DIPERKUAT terhadap {@link NumberFormatException} &mdash; parameter {@code id} diurai lewat
 * {@link #parseLong(String)} yang mengembalikan {@code null} (bukan melempar exception) bila
 * kosong/bukan angka, dan servlet membalas {@link HttpServletResponse#SC_BAD_REQUEST} secara
 * eksplisit untuk id tidak valid serta {@link HttpServletResponse#SC_NOT_FOUND} bila
 * {@link Kegiatan} tidak ditemukan &mdash; {@link Struk} sebaliknya masih memanggil
 * {@code Long.parseLong(myid)} mentah yang dapat melempar exception tak tertangani ke
 * pemanggil. Perbaikan ini HANYA menutup celah crash/robustness, BUKAN celah otentikasi atau
 * enumerasi id; status anonim dan id sekuensial di atas TETAP berlaku sama seperti
 * {@link Struk}.</p>
 *
 * @see Struk
 * @see Kegiatan
 */
public class StrukM extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet; tidak melakukan inisialisasi
	 * khusus.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public StrukM() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan ke {@link #process}. Tidak ada
	 * gerbang otentikasi/otorisasi di sini maupun di {@link #process} &mdash; lihat bagian
	 * Keamanan pada dokumentasi kelas.
	 *
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
	 * Menangani permintaan HTTP POST dengan perilaku identik {@link #doGet}, termasuk tidak
	 * adanya gerbang otentikasi/otorisasi.
	 *
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
	 * Inti servlet: menerjemahkan parameter {@code id} menjadi sebuah {@link Kegiatan},
	 * membangun PDF bukti pembayarannya lewat
	 * {@code CommonReportHelper.cetakBuktipembayaranMahasiswa}, lalu menyalin berkas PDF yang
	 * dihasilkan ke {@code resp} sebagai unduhan.
	 * <p>Urutan kerja: (1) parameter {@code id} dibaca; bila berawalan {@code "EE"}, sisanya
	 * didekripsi lewat {@code Common.desEncrypter.get().decrypt(...)} &mdash; kegagalan pada
	 * langkah ini ditelan (dicatat lewat {@code ErrorAuditUtil.record}) dan nilai mentah tetap
	 * dipakai; (2) {@code myid} diurai secara aman lewat {@link #parseLong(String)}, membalas
	 * {@link HttpServletResponse#SC_BAD_REQUEST} bila bukan angka valid; (3) {@link Kegiatan}
	 * dicari lewat primary key tersebut TANPA penyaring kepemilikan/sesi apa pun, membalas
	 * {@link HttpServletResponse#SC_NOT_FOUND} bila tidak ditemukan; (4) PDF dirender dan
	 * disalin ke {@code resp} dengan penyangga 1&nbsp;KiB.</p>
	 * <p>Tidak ada gerbang otentikasi/otorisasi pada method ini &mdash; lihat bagian Keamanan
	 * pada dokumentasi kelas untuk perbandingan dengan {@link Struk}.</p>
	 *
	 * @param request permintaan masuk berisi parameter {@code id}
	 * @param resp    balasan yang akan diisi bita berkas PDF struk pembayaran, atau status
	 *                {@code 400}/{@code 404} bila id tidak valid/tidak ditemukan
	 * @throws Exception bila pencarian data atau pembangunan laporan gagal
	 */
	@SuppressWarnings({ })
	private void process(HttpServletRequest request, HttpServletResponse resp) throws Exception {
		String myid = request.getParameter("id");

		try {
			if (myid.startsWith("EE")) {
				myid = Common.desEncrypter.get().decrypt(myid.substring(2));
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/StrukM.java:70");
		}

		System.out.println("myid = " + myid);

		// FIX NumberFormatException "For input string: \"\"": parameter "id" boleh kosong/hilang
		// (mis. tautan struk lama/rusak), atau dekripsi "EE..." di atas gagal diam-diam sehingga
		// myid tersisa bukan angka. JANGAN Long.parseLong mentah -- guard dulu (pola sama dgn
		// Document.java#parseLong / #downloadDocument) dan balas 400, jangan sampai servlet crash.
		Long idKegiatan = parseLong(myid);
		if (idKegiatan == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID struk tidak valid.");
			return;
		}

		Kegiatan pembayaranSiswa = null;
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			pembayaranSiswa = (Kegiatan) session.createCriteria(Kegiatan.class)
					.add(Restrictions.idEq(idKegiatan)).uniqueResult();
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/StrukM.java:83");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/StrukM.java:84");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/StrukM.java:85");}
			}
		}
		if (pembayaranSiswa == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Data struk tidak ditemukan.");
			return;
		}

		File file = CommonReportHelper.cetakBuktipembayaranMahasiswa(pembayaranSiswa, false);

		resp.setContentType("application/pdf");
		resp.setHeader("Content-Disposition", "attachment; filename=\"struk_pembayaran.pdf\"");

		ServletOutputStream out = resp.getOutputStream();
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			int length;
			byte[] buffer = new byte[1024];
			while ((length = in.read(buffer)) != -1) {
				out.write(buffer, 0, length);
			}
			out.flush();
		} finally {
			if (in != null) {
				try { in.close(); } catch (Exception ignored) { }
			}
		}
	}

	/**
	 * Parse aman: null (bukan exception) bila value kosong/tidak berupa angka.
	 *
	 * @param value teks yang akan diurai, boleh {@code null}/kosong
	 * @return {@link Long} hasil parsing, atau {@code null} bila {@code value} kosong atau
	 *         bukan representasi angka yang valid (tidak pernah melempar exception)
	 */
	private static Long parseLong(String value) {
		try {
			return value == null || value.trim().length() == 0 ? null : Long.valueOf(value.trim());
		} catch (Exception e) {
			return null;
		}
	}

}
