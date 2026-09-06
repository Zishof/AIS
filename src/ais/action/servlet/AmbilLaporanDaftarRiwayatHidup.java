package ais.action.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Enumeration;
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
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.employ.GajiPokok;
import ais.ui.util.WaktuUtil;

/**
 * Servlet endpoint yang menghasilkan dan mengirimkan laporan "Daftar Riwayat Hidup" (CV/resume)
 * seorang {@link Pegawai}: memuat data pribadi, foto, serta gaji pokok terkini ({@link GajiPokok})
 * pegawai tersebut ke template laporan {@code employ/daftar_riwayat_hidup}, lalu mengembalikan
 * hasilnya dalam format HTML (ditampilkan langsung) atau berkas lain sesuai parameter
 * {@code type} (lihat {@link Report}).
 *
 * <p>
 * Identitas pegawai target ditentukan sepenuhnya dari parameter permintaan {@code email}
 * dan/atau {@code userId} — bukan dari sesi login pengguna yang sedang aktif. Bila
 * {@link Tbmuser} untuk email tersebut belum ada tetapi ditemukan {@link Pegawai} yang cocok,
 * servlet ini bahkan MEMBUAT baris {@link Tbmuser} baru sebagai efek samping dari permintaan
 * GET/POST biasa (auto-provisioning akun agar pegawai tersebut punya login sistem).
 * </p>
 *
 * <p>
 * <b>Catatan keamanan (dilaporkan, tidak diperbaiki sesuai instruksi tugas — pola broken access
 * control yang sudah berulang kali tercatat pada servlet {@code Ambil*} lain, mis.
 * {@code AmbilLaporanDaftarPegawai} dan {@code AmbilLaporanMahasiswa}):</b> tidak ada pengecekan
 * bahwa {@code email}/{@code userId} pada permintaan cocok dengan pengguna yang sedang login
 * (tidak ada pemanggilan {@code Common.getCurrentUser(...)} atau validasi sesi apa pun) — satu-
 * satunya gerbang adalah menolak nilai literal {@code "default@liferay.com"} atau parameter
 * {@code email} yang kosong. Siapa pun yang mengetahui/menebak alamat email seorang pegawai dapat
 * memanggil endpoint ini secara langsung dan mengunduh CV LENGKAP pegawai tersebut — termasuk
 * data pribadi dan {@link GajiPokok} (gaji pokok, data finansial sensitif) — tanpa otentikasi
 * maupun otorisasi apa pun selain mengetahui satu alamat email. Ini adalah kerentanan IDOR
 * (Insecure Direct Object Reference) yang mengekspos PII dan data gaji lintas-pengguna.
 * </p>
 */
public class AmbilLaporanDaftarRiwayatHidup extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Konstruktor baku servlet, tanpa inisialisasi tambahan. */
	public AmbilLaporanDaftarRiwayatHidup() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan GET dengan mendelegasikan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}; galat ditangani oleh
	 * {@link Common#tampilErrorJikaAdmin(Exception)} agar detail teknis hanya tampil untuk admin.
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
	 * Menangani permintaan POST dengan mendelegasikan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}; galat ditangani oleh
	 * {@link Common#tampilErrorJikaAdmin(Exception)} agar detail teknis hanya tampil untuk admin.
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
	 * Membangun dan mengirimkan laporan daftar riwayat hidup pegawai. Alur: (1) mencetak seluruh
	 * parameter permintaan ke {@link System#out} untuk keperluan debug; (2) menolak permintaan
	 * bila parameter {@code email} kosong atau bernilai literal {@code "default@liferay.com"}
	 * dengan menampilkan pesan HTML "harus login"; (3) mencari {@link Tbmuser} aktif berdasarkan
	 * {@code userId}/{@code email}, atau bila tidak ada mencari {@link Pegawai} aktif berdasarkan
	 * {@code email} lalu meng-auto-provisioning {@link Tbmuser} baru untuknya bila belum punya
	 * akun; (4) bila tidak ditemukan {@link Pegawai} yang terhubung, menampilkan pesan HTML "belum
	 * terhubung ke data pegawai"; (5) menyusun parameter laporan dari data {@link Pegawai}, foto
	 * (via {@link CommonMedia#loadPathFileFotoLangsung(Tbmuser)}), dan {@link GajiPokok} terkini
	 * (bila ada), lalu menghasilkan berkas laporan via
	 * {@link Report#generateFileReport(String, Map, String, java.util.Date, java.util.Locale)};
	 * dan (6) menuliskan hasilnya ke response — sebagai HTML inline bila {@code type} adalah
	 * {@link Report#HTML}, atau sebagai aliran biner ({@code application/&lt;type&gt;}) untuk
	 * format lain. Sesi Hibernate yang dibuka untuk pencarian pengguna selalu dibersihkan dan
	 * ditutup pada blok {@code finally}.
	 *
	 * @param request permintaan HTTP masuk, membawa parameter {@code type}, {@code email}, dan
	 *                {@code userId}
	 * @param response respons HTTP tempat laporan (HTML atau berkas biner) dituliskan
	 * @throws Exception bila terjadi galat query database, pembuatan laporan, atau I/O berkas
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		Enumeration<String> enumeration = request.getParameterNames();
		while (enumeration.hasMoreElements()) {
			String param = enumeration.nextElement();
			System.out.print("  " + param + " = " + request.getParameter(param));
		}

		String type = request.getParameter("type") == null ? Report.HTML : request.getParameter("type");

		String email = request.getParameter("email") == null ? "-11111111" : request.getParameter("email");
		String userId = request.getParameter("userId") == null ? "-11111111" : request.getParameter("userId");

		if (email.equals("default@liferay.com") || request.getParameter("email") == null) {
			response.setContentType("text/html;charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.println(
					"<font style=\"font-family: serif;font-size: x-large;color: blue;\">Anda harus login terlebih dahulu sebelum melihat daftar riwayat hidup pegawai.</font></body></html");
			out.close();
			return;
		}

		Session mySession = null;
		Tbmuser tbmuser = null;
		Pegawai pegawai = null;
		try {
			mySession = HibernateUtil.getSessionFactory().openSession();
			tbmuser = (Tbmuser) mySession.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(Restrictions.eq("userId", userId), Restrictions.eq("email", email)))
					.setMaxResults(1).uniqueResult();
			if (tbmuser == null || tbmuser.getUserId() == null) {
				pegawai = (Pegawai) mySession.createCriteria(Pegawai.class)
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
						.add(Restrictions.eq("email", email)).setMaxResults(1).uniqueResult();
				if (pegawai != null) {
					tbmuser = (Tbmuser) mySession.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("pegawai", pegawai))
							.setMaxResults(1).uniqueResult();
					if (tbmuser == null || tbmuser.getUserId() == null) {
						tbmuser = new Tbmuser();
						tbmuser.setPegawai(pegawai);
						tbmuser.setUserId(email);
						tbmuser.setUserNama(pegawai.getNama());
						tbmuser.setEmail(email);
						tbmuser.setIs_encripted(false);
						tbmuser.setRoot(true);
						tbmuser.setSatuanKerja(pegawai.getSatuanKerja());
						tbmuser.setUserRole(new Tbmrole(Tbmrole.PEGAWAI));
						tbmuser.setUserPassword(email);
						tbmuser.setUserShow(1);
						mySession.getTransaction().begin();
						mySession.save(tbmuser);
						mySession.getTransaction().commit();
					}
				}
			}

			if (pegawai == null) {
				pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
			}
		} finally {
			if (mySession != null) {
				try { mySession.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanDaftarRiwayatHidup.java:134");}
				try { mySession.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanDaftarRiwayatHidup.java:135");}
				try { mySession.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanDaftarRiwayatHidup.java:136");}
			}
		}

		if (pegawai == null) {
			response.setContentType("text/html;charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.println("<font style=\"font-family: serif;font-size: xx-large;color: red;\">Akun anda "
					+ (request.getParameter("email") == null ? "" : "(" + email + ")")
					+ " belum terhubung ke data pegawai. Harap segera menghubungi administrator untuk menghubungkan data anda ke data pegawai.</font></body></html");
			out.close();
			return;
		}

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		parameters.put("id", pegawai.getId());
		String myfile = CommonMedia.loadPathFileFotoLangsung(new Tbmuser(pegawai));
		parameters.put("foto", myfile);
		Common.insertProperty(Pegawai.class, pegawai, parameters, "", 2);
		GajiPokok gajiPokok = pegawai.ambilGajiPokok(WaktuUtil.getDate());
		if(gajiPokok != null) {
			Common.insertProperty(GajiPokok.class, gajiPokok, parameters, "gp", 1);
		}
		File file = Report.generateFileReport(type, parameters, "employ/daftar_riwayat_hidup",
				ais.ui.util.WaktuUtil.getDate(), Common.locale);

		if (type.equals(Report.HTML)) {
			String textAll = "";
			String text = "";
			Reader reader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(reader);
			try {
				while ((text = bufferedReader.readLine()) != "") {
					textAll += text;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanDaftarRiwayatHidup.java:171");
			}
			bufferedReader.close();
			reader.close();

			response.setContentType("text/html;charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.println(textAll);
			out.close();
			return;
		}

		response.setContentType("application/" + type);

		ServletOutputStream out = response.getOutputStream();
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
