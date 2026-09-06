package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.LampiranLain;

/**
 * Servlet penyaji berkas {@link LampiranLain} berdasarkan pasangan parameter polos
 * {@code ref} dan {@code jenis} pada <i>query string</i>.
 *
 * <p>Terdaftar di {@code web.xml} dengan alamat {@code /AmbilLampiranLain}. Berbeda dari
 * {@code AmbilLampiran} (yang menerima token terenkripsi {@code d} maupun parameter
 * {@code usingId} untuk mencocokkan langsung ke primary key), kelas ini hanya memanggil
 * bentuk aman {@link LampiranLain#ambil(Long, String)} &mdash; setara
 * {@code usingId = false} &mdash; sehingga penyaring {@code jenis} selalu aktif dan
 * tebakan {@code ref} sembarang tidak otomatis mengembalikan lampiran jenis lain
 * (lih. temuan {@code task_b82b25d2}, yang <b>tidak berlaku di sini</b> karena
 * overload {@code usingId} tidak pernah dipakai oleh kelas ini).</p>
 *
 * <h4>Cakupan otentikasi</h4>
 * <p>Alamat ini tidak punya aturan {@code intercept-url} sendiri di
 * {@code applicationContext-security.xml}, tetapi tercakup oleh pola berawalan-sama
 * {@code /AmbilLampiran**} yang bernilai {@code IS_AUTHENTICATED_REMEMBERED} &mdash;
 * jadi menuntut sesi masuk, berbeda dari alias publik {@code /al} milik
 * {@code AmbilLampiran}.</p>
 *
 * <h4>PERINGATAN KEAMANAN &mdash; tanpa pemeriksaan kepemilikan</h4>
 * <p>Sekalipun penyaring {@code jenis} aktif, {@link #process} maupun {@link #loadFile}
 * tidak pernah membandingkan pengguna yang masuk dengan pemilik baris {@code ref} yang
 * diminta. Siapa pun yang sudah masuk (peran apa pun) dapat menebak nilai {@code ref}
 * berurutan bersama {@code jenis} yang diketahui/ditebak dan mengunduh lampiran milik
 * pengguna lain, persis seperti peringatan yang sudah tercatat pada
 * {@link LampiranLain#ambil(Long, String)}.</p>
 *
 * @see LampiranLain
 */
public class AmbilLampiranLain extends HttpServlet {
	/**
	 * Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai secara fungsional karena
	 * instance servlet tidak pernah diserialisasi oleh kontainer pada penyebaran AIS.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet. Tidak melakukan
	 * inisialisasi apa pun.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilLampiranLain() {
		super();
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan seluruh pekerjaan ke
	 * {@link #process}.
	 *
	 * @param request  permintaan masuk, memuat parameter {@code ref} dan {@code jenis}
	 * @param response balasan yang akan diisi bita berkas lampiran atau berkas pengganti
	 * @throws ServletException tidak pernah dilempar keluar &mdash; {@link #process}
	 *                          menelan seluruh pengecualiannya sendiri
	 * @throws IOException      tidak pernah dilempar keluar, dengan alasan yang sama
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	/**
	 * Menangani permintaan HTTP POST dengan perilaku identik {@link #doGet}, seluruhnya
	 * didelegasikan ke {@link #process}.
	 *
	 * @param request  permintaan masuk, memuat parameter {@code ref} dan {@code jenis}
	 * @param response balasan yang akan diisi bita berkas lampiran atau berkas pengganti
	 * @throws ServletException tidak pernah dilempar keluar &mdash; lih. {@link #doGet}
	 * @throws IOException      tidak pernah dilempar keluar, dengan alasan yang sama
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	/**
	 * Mengoordinasikan pengambilan lokasi berkas lewat {@link #loadFile} lalu menuliskan
	 * isinya bita demi bita ke {@code resp}.
	 *
	 * <p>Sesi Hibernate {@code streamingSession} dibuka hanya untuk keperluan
	 * {@link #loadFile} (query {@link LampiranLain#ambil(Long, String)}) dan langsung
	 * dibersihkan &mdash; {@code clear()}, {@code disconnect()}, {@code close()} &mdash;
	 * begitu jalur berkas didapat, sebelum penyaliman bita dimulai; dengan begitu koneksi
	 * database tidak tertahan selama proses I/O berkas yang bisa berlangsung lama.
	 * Seluruh pengecualian dari kedua tahap (pencarian lampiran maupun penyaliman berkas)
	 * ditelan lewat {@link Common#tampilErrorJikaAdmin(Exception)}, sehingga kegagalan
	 * tidak pernah tampak ke peramban sebagai kode status 5xx.</p>
	 *
	 * @param request permintaan masuk, diteruskan apa adanya ke {@link #loadFile}
	 * @param resp    balasan yang akan diisi header dan bita berkas
	 */
	private void process(HttpServletRequest request, HttpServletResponse resp) {

		Session streamingSession = null;
		String filename = "";
		try {
			streamingSession = StreamingHibernateUtil.getInstance().openSession();
			filename = loadFile(request, resp, streamingSession).getAbsolutePath();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (streamingSession != null) {
				try { streamingSession.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiranLain.java:63");}
				try { streamingSession.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiranLain.java:64");}
				try { streamingSession.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiranLain.java:65");}
			}
		}

		try {

			// Set content size
			File file = new File(filename);
			resp.setContentLength((int) file.length());
			// String headerKey = "Content-Disposition";
			// String headerValue = String.format("attachment; filename=\"%s\"",
			// filename);
			// resp.setHeader(headerKey, headerValue);

			// Open the file and output streams
			FileInputStream in = new FileInputStream(file);

			OutputStream out = resp.getOutputStream();

			// Copy the contents of the file to the output stream
			byte[] buf = new byte[1024];
			int count = 0;
			while ((count = in.read(buf)) >= 0) {
				out.write(buf, 0, count);
			}
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			Common.tampilErrorJikaAdmin(e);
		} catch (IOException e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	/**
	 * Menentukan berkas yang harus disajikan: berkas lampiran bila ditemukan lewat
	 * {@code ref}/{@code jenis}, atau PDF alur pendaftaran sebagai berkas pengganti.
	 *
	 * <p>Direktori {@code media/} di bawah induk direktori webapp dipastikan ada
	 * ({@code mkdirs()} bila belum) meskipun tidak dipakai langsung oleh metode ini
	 * &mdash; sisa dari pola bersama servlet-servlet penyaji berkas lain di paket ini.
	 * Bila parameter {@code ref} maupun {@code jenis} ada, keduanya diteruskan ke
	 * {@link LampiranLain#ambil(Long, String)}; hasilnya dipakai hanya bila baris
	 * ditemukan <b>dan</b> {@link LampiranLain#ambilFile()}-nya tidak {@code null}.
	 * Tipe konten balasan diisi dari kolom {@code keterangan} baris lampiran, yang pada
	 * baris-baris ini menyimpan nilai MIME type, bukan keterangan bebas seperti pada
	 * kebanyakan entitas lain.</p>
	 *
	 * <p>{@code Long.parseLong(request.getParameter("ref"))} melempar
	 * {@code NumberFormatException} bila parameter {@code ref} tidak dikirim atau bukan
	 * angka; pengecualian itu merambat ke {@link #process}, yang menelannya lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)} sehingga berkas pengganti yang
	 * sudah disiapkan sebagai {@code file} lokal tidak sempat dikembalikan.</p>
	 *
	 * @param request          permintaan masuk; parameter {@code ref} (angka, wajib agar
	 *                         lampiran ditemukan) dan {@code jenis} (penanda jenis
	 *                         lampiran) dibaca darinya
	 * @param resp             balasan yang tipe kontennya (awalnya {@code application/pdf})
	 *                         diubah menjadi MIME type lampiran bila lampiran ditemukan
	 * @param streamingSession sesi Hibernate yang dipakai {@link LampiranLain#ambil}
	 *                         untuk kueri; siklus hidupnya dikendalikan pemanggil
	 * @return berkas PDF alur pendaftaran bila lampiran tidak ditemukan, atau berkas
	 *         lampiran itu sendiri bila ditemukan
	 * @throws Exception meneruskan apa adanya setiap pengecualian dari
	 *                    {@link LampiranLain#ambil(Long, String)} maupun dari penguraian
	 *                    parameter {@code ref}
	 */
	private File loadFile(HttpServletRequest request, HttpServletResponse resp, Session streamingSession)
			throws Exception {

		ServletContext sc = getServletContext();
		String path = new File(sc.getRealPath("/")).getParentFile().getAbsolutePath() + "/media/";
		File mediaDic = new File(path);
		if (!mediaDic.exists()) {
			mediaDic.mkdirs();
		}

		File file = new File(sc.getRealPath("/help/alur_pendaftaran.pdf"));
		resp.setContentType("application/pdf");
		Long ref = Long.parseLong(request.getParameter("ref"));
		String jenis = request.getParameter("jenis");

		if (ref != null && jenis != null) {

			LampiranLain lainMahasiswa = LampiranLain.ambil(ref, jenis);

			if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
				file = lainMahasiswa.ambilFile();
				resp.setContentType(lainMahasiswa.getKeterangan());
			}

		}

		return file;

	}

}
