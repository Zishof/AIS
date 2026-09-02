package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import ais.action.master.resources.ELearningResource;
import ais.action.master.resources.PosResource;
import ais.action.master.resources.model.CommonID;
import ais.common.Common;
import ais.ui.util.WaktuUtil;

/**
 * Servlet publik (CORS diizinkan dari mana saja, {@code Access-Control-Allow-Origin: *}) untuk
 * mencatat kehadiran/absensi lewat request GET/POST tunggal, diautentikasi lewat header {@code p}
 * berisi password statis (bukan sesi login). Mendukung dua jalur: absensi mesin fingerprint/POS
 * (parameter {@code id_finger}+{@code waktu}, didelegasikan ke
 * {@link PosResource#absen(String, String, String)}) dan absensi online berbasis lokasi (parameter
 * {@code id}/{@code data}/{@code lat}/{@code lng}, didelegasikan ke
 * {@link ELearningResource#doAbsen}). Respons selalu JSON dengan kode {@code status} ("00" sukses,
 * "90"-"92" berbagai kegagalan).
 *
 * <p>
 * <b>Riwayat keamanan (DIPERBAIKI 2026-09-02)</b> — {@link #isValidPassword(HttpServletRequest)}
 * sebelumnya membaca konfigurasi {@code password_absen} lewat
 * {@link Common#getKonfigurasi(String, String)} dengan NILAI DEFAULT RAHASIA tertanam langsung di
 * kode sumber ({@code "4GUb3KPArA78B9AOmKj3pLivo49IEPfQDFHbeCLFpsAG6fgWQZ"}) — bila baris
 * konfigurasi {@code password_absen} belum pernah diisi/disimpan di database, string ini diam-diam
 * menjadi password API yang berlaku, tersebar ke siapa pun yang memiliki akses baca ke source code
 * (termasuk lewat riwayat kontrol versi). Default itu sudah dihapus (kini string kosong); endpoint
 * ini sekarang fail-closed — bila {@code password_absen} belum diisi eksplisit di database, TIDAK
 * ADA header {@code p} apa pun yang bisa lolos validasi (dibanding sebelumnya, saat baris
 * konfigurasi kosong secara diam-diam berarti "pakai password bawaan").
 * </p>
 * <p>
 * <b>TINDAK LANJUT DI LUAR PERUBAHAN KODE INI</b>: nilai yang sebelumnya tertanam sudah lama
 * berada di riwayat SVN dan WAJIB dianggap bocor — perlu dirotasi (ganti nilai
 * {@code password_absen} di database, lalu perbarui perangkat fingerprint/POS dan klien absensi
 * online yang mengirim header {@code p}) bila kredensial ini masih aktif dipakai di produksi.
 * </p>
 */
public class Absen extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Konstruktor default tanpa inisialisasi khusus. */
	public Absen() {
		super();
	}

	/**
	 * Titik masuk utama: menambahkan header CORS, memvalidasi password lewat header {@code p}
	 * ({@link #isValidPassword}), lalu memproses absensi fingerprint/POS (jika
	 * {@code id_finger}/{@code waktu} terisi) atau absensi online berbasis lokasi (jika tidak).
	 * Seluruh kegagalan ditangkap dan dikembalikan sebagai JSON {@code status=90} beserta pesan.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JSONObject jsonObject = new JSONObject();
		try {
			addCorsHeaders(response);

			if (!isValidPassword(request)) {
				jsonObject.put("status", "91");
				jsonObject.put("info", "Password salah");
				writeJson(response, jsonObject);
				return;
			}

			String id = safeTrim(request.getParameter("id"));
			String data = safeTrim(request.getParameter("data"));
			String lat = safeTrim(request.getParameter("lat"));
			String lng = safeTrim(request.getParameter("lng"));
			String idFinger = safeTrim(request.getParameter("id_finger"));
			String waktu = safeTrim(request.getParameter("waktu"));
			String state = safeTrim(request.getParameter("state"));

			if (idFinger.length() > 0 || waktu.length() > 0) {
				if (idFinger.length() == 0 || waktu.length() == 0) {
					jsonObject.put("status", "92");
					jsonObject.put("info", "Parameter id_finger dan waktu wajib diisi bersamaan");
					writeJson(response, jsonObject);
					return;
				}

				CommonID commonID = PosResource.absen(idFinger, waktu, state);
				if (commonID == null) {
					jsonObject.put("status", "90");
					jsonObject.put("info", "Absensi gagal diproses");
				} else {
					String info = commonID.getInfo4();
					jsonObject.put("status", info != null && info.toLowerCase().startsWith("gagal") ? "90" : "00");
					jsonObject.put("id", commonID.getInfo1());
					jsonObject.put("waktu", commonID.getInfo2());
					jsonObject.put("nama", commonID.getInfo3());
					jsonObject.put("info", commonID.getInfo4());
					jsonObject.put("mulai", commonID.getInfo5());
					jsonObject.put("selesai", commonID.getInfo6());
				}
			} else {
				CommonID hasil = ELearningResource.doAbsen(id, data, lat, lng,
						"Absensi online sukses pada " + Common.dateFormat5.get().format(WaktuUtil.getDate()));
				if (hasil == null) {
					jsonObject.put("status", "90");
					jsonObject.put("info", "Absensi online gagal diproses");
				} else {
					jsonObject.put("status", "00");
					jsonObject.put("id", hasil.getId());
					jsonObject.put("prodi", hasil.getInfo3());
					jsonObject.put("nama", hasil.getInfo1());
					jsonObject.put("foto", hasil.getInfo16());
					jsonObject.put("nim", hasil.getInfo2());
					jsonObject.put("status_absen", hasil.getInfo10());
					jsonObject.put("status", hasil.getInfo10() == null || hasil.getInfo10().trim().length() == 0 ? "00"
							: hasil.getInfo10());
				}
			}
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Absen.java:90");
			}
			try {
				jsonObject.put("status", "90");
				jsonObject.put("info", "Terjadi kesalahan internal: " + (e.getMessage() == null ? e.getClass().getName() : e.getMessage()));
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Absen.java:95");
			}
		}
		writeJson(response, jsonObject);
	}

	/**
	 * Memvalidasi header {@code p} terhadap konfigurasi {@code password_absen} (lihat riwayat
	 * keamanan pada javadoc kelas). Mengembalikan {@code false} bila header tidak ada/kosong,
	 * {@code password_absen} belum dikonfigurasi (fail-closed), atau nilainya tidak cocok
	 * (perbandingan case-insensitive).
	 */
	private boolean isValidPassword(HttpServletRequest request) {
		try {
			String headerPassword = request == null ? null : request.getHeader("p");
			if (headerPassword == null || headerPassword.trim().length() == 0) {
				return false;
			}
			String password = Common.getKonfigurasi("password_absen", "").getNilai();
			return password != null && password.trim().length() > 0
					&& password.trim().equalsIgnoreCase(headerPassword.trim());
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Absen.java:113");
			}
			return false;
		}
	}

	/** @return {@code value} setelah di-trim, atau string kosong bila {@code null}. */
	private static String safeTrim(String value) {
		return value == null ? "" : value.trim();
	}

	/** Menambahkan header CORS permisif (origin apa pun) ke {@code response} agar endpoint ini bisa dipanggil dari domain/aplikasi klien mana pun. */
	private void addCorsHeaders(HttpServletResponse response) {
		if (response == null) {
			return;
		}
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, p");
	}

	/** Menulis {@code jsonObject} (atau {@code "{}"} bila {@code null}) sebagai body respons {@code application/json}, lengkap dengan header CORS. */
	private void writeJson(HttpServletResponse response, JSONObject jsonObject) throws IOException {
		String body = jsonObject == null ? "{}" : jsonObject.toString();
		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		addCorsHeaders(response);
		PrintWriter writer = null;
		try {
			writer = response.getWriter();
			writer.write(body);
			writer.flush();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Absen.java:146");
				}
			}
		}
	}

	/** Menangani permintaan POST dengan perilaku identik {@link #doGet}. */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/** Menangani preflight CORS ({@code OPTIONS}) dengan hanya menambahkan header CORS, tanpa memproses absensi. */
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		addCorsHeaders(response);
	}
}
