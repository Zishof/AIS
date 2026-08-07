package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.helper.virtualaccount.BankaltimtaraResponseUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.WaktuUtil;

/**
 * Servlet implementation class CheckISBN
 */
public class Bankaltimtara extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Bankaltimtara() {
		super();
	}

	/**
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

	public static JSONObject checkPakaiqris(final VirtualAccountBank virtualAccountBankReadOnly) throws Exception {
		System.out.println("Request body: ");

		String strURL = (Common.getKonfigurasi("bankaltimtara_gateway_qris_url_autentication",
				"https://api-dev.bankaltimtara.co.id:8084/api/user/auth").getNilai());

		String user = Common.getKonfigurasi("bankaltimtara_qris_username", "qrisdev").getNilai();
		String pwd = Common.getKonfigurasi("bankaltimtara_qris_password", "PB@|1Kp@paN19112021").getNilai();

		JSONObject login = new JSONObject();
		login.put("username", user);
		login.put("password", pwd);

		System.out.println(login + "");

		String[] command = { "curl", "--silent", "--show-error", "--location", strURL, "--header",
				"Content-type: application/json", "--data-raw", login.toString() };

		ProcessBuilder process = new ProcessBuilder(command);
		process.redirectErrorStream(true);
		Process p;
		p = process.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringBuilder builder = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
		}
		String hasil = builder.toString();
		System.out.println(hasil);

		JSONObject jsonObject2 = null;
		try {
			JSONObject jSONObject = BankaltimtaraResponseUtil.parseJson(hasil, "autentikasi QRIS");
			String token = BankaltimtaraResponseUtil.ambilStringWajib(jSONObject, "token", "autentikasi QRIS");

			strURL = (Common.getKonfigurasi("bankaltimtara_gateway_qris_url_check_status",
					"https://api-dev.bankaltimtara.co.id:8084/api/qrismpm/transaction/status").getNilai());

			String inst_id = Common.getKonfigurasi("bankaltimtara_inst_id_qris", "211028001").getNilai();

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("kd_tagihan", virtualAccountBankReadOnly.getKode());
			jsonObject.put("institusi", inst_id);
			String postData = jsonObject.toString();

			System.out.println(postData + "");

			command = new String[] { "curl", "--silent", "--show-error", "--location", strURL, "--header",
					"Content-type: application/json", "--header", "Authorization: Bearer " + token, "--data",
					postData };

			process = new ProcessBuilder(command);
			process.redirectErrorStream(true);

			p = process.start();
			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			builder = new StringBuilder();
			line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			hasil = builder.toString();
			System.out.println(hasil);

			jsonObject2 = BankaltimtaraResponseUtil.parseJson(hasil, "cek status QRIS");
			BankaltimtaraResponseUtil.pastikanSukses(jsonObject2, "cek status QRIS");
		} catch (Exception e) {
			throw e;
		}

		System.out.println("hasil -> " + hasil);

		try {

			if (jsonObject2 != null) {
				Double paid = Double.parseDouble((jsonObject2.get("bruto_amount") + "").trim());
				// Bandingkan ke totalBiaya() (total+biaya admin), BUKAN getTotal() saja -- VA/QRIS
				// dibuat & ditagihkan sebesar totalBiaya() (lihat DownloadTagihanMahasiswaBankBankaltimtara
				// & pengecekan H2H masuk di file ini yg SUDAH benar pakai totalBiaya()). Bila
				// bankaltimtara_biaya_administrasi != 0, bandingkan ke getTotal() saja TAK PERNAH cocok
				// -> VA yg sudah lunas di bank tak pernah ditandai lunas di sini ("Cek Ulang" diam gagal).
				if (paid.intValue() == virtualAccountBankReadOnly.totalBiaya()) {
					if (VirtualAccountBank.isSudahTerbayar(virtualAccountBankReadOnly)) {
						return jsonObject2;
					}
					Session session = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						VirtualAccountBank.bayarVa(virtualAccountBankReadOnly, WaktuUtil.getDate(), hasil, session);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Bankaltimtara.java:161");
					} finally {
						if (session != null) {
							try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:164");}
							try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:165");}
							try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:166");}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Bankaltimtara.java:172");
		}
		return jsonObject2;
	}

	/**
	 * Validasi minimal sebelum {@code new JSONObject(hasil)}: pastikan respons tidak
	 * null/kosong dan diawali karakter '{' (setelah di-trim). Endpoint bank kadang membalas
	 * string kosong atau halaman HTML error (mis. gateway timeout/404) alih-alih JSON, yang
	 * kalau langsung di-parse akan melempar {@link org.json.JSONException} mentah ("A
	 * JSONObject text must begin with '{'...") sampai ke dialog admin. Di sini exception
	 * diganti dengan pesan Indonesia yang jelas agar admin tahu akar masalahnya (respons bank
	 * tidak valid), bukan pesan teknis JSON.
	 *
	 * @param hasil   string respons mentah dari bank yang akan diparse sebagai JSON.
	 * @param konteks label singkat sumber respons (mis. "login"/"cek status VA") untuk pesan error.
	 * @throws Exception bila respons tidak valid sebagai JSON object.
	 */
	private static void validasiResponJsonBankaltimtara(String hasil, String konteks) throws Exception {
		String trimmed = hasil == null ? "" : hasil.trim();
		if (trimmed.isEmpty() || trimmed.charAt(0) != '{') {
			throw new Exception("Gagal memeriksa status VA Bankaltimtara (" + konteks
					+ "): respons dari bank tidak valid (kosong atau bukan format JSON yang dikenali). Respons: "
					+ BankaltimtaraResponseUtil.ringkas(trimmed) + ". "
					+ "Kemungkinan gateway bank sedang bermasalah/berubah. Silakan coba lagi beberapa saat, "
					+ "atau hubungi administrator bila terus berulang.");
		}
	}

	public static JSONObject checkPakaiva(final VirtualAccountBank virtualAccountBankReadOnly) throws Exception {
		String strURL = (Common.getKonfigurasi("bankaltimtara_gateway_url_autentication",
				"https://api-dev.bankaltimtara.co.id:8300/api/user/auth").getNilai());

		String linkPost = Common.getKonfigurasi("url_status_va_bankaltimtara",
				"https://api-dev.bankaltimtara.co.id:8081/api-service/api/va/paid/nova").getNilai().trim();

		String user = Common.getKonfigurasi("bankaltimtara_username", "ubtva1").getNilai();
		String pwd = Common.getKonfigurasi("bankaltimtara_password", "12345678").getNilai();

		JSONObject login = new JSONObject();
		login.put("username", user);
		login.put("password", pwd);

		System.out.println(login + "");

		String[] command = { "curl", "--location", strURL, "--header", "Content-type: application/json", "--data-raw",
				login.toString() };

		ProcessBuilder process = new ProcessBuilder(command);
		Process p;
		p = process.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringBuilder builder = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
		}
		String hasil = builder.toString();
		System.out.println(hasil);

		validasiResponJsonBankaltimtara(hasil, "login/autentikasi");
		JSONObject jSONObject = new JSONObject(hasil);

		String token = jSONObject.getString("token");

		String post = linkPost + "/" + virtualAccountBankReadOnly.getKode();

		String[] commandPost = { "curl", "--location", post, "--header", "Authorization: Bearer " + token };

		System.out.println("linkPost -> " + post);
		System.out.println("token -> " + token);

		JSONObject jsonObject2 = null;
		Exception errorLive = null;

		try {

			process = new ProcessBuilder(commandPost);

			p = process.start();
			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			builder = new StringBuilder();
			line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			hasil = builder.toString();

			System.out.println("hasil -> " + hasil);

			jsonObject2 = prosesHasilCekVaBankaltimtara(virtualAccountBankReadOnly, hasil, false);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Bankaltimtara.java:269");
			errorLive = e;
		}

		if (errorLive != null) {
			// FALLBACK: pengecekan LIVE ke gateway Bankaltimtara gagal (mis. endpoint "nova" sedang
			// bermasalah/berubah dan membalas HTML 404 alih-alih JSON, sehingga new JSONObject(hasil)
			// gagal parse). Daripada berhenti dengan hasil kosong, olah ULANG notifikasi TERAKHIR yang
			// sudah tersimpan di kolom notif (hasil pengecekan/notifikasi bank sebelumnya yang valid),
			// SEOLAH-OLAH baru saja diterima — tapi diproses langsung secara lokal, tanpa panggilan
			// jaringan baru ke bank. Ini murni fallback pemulihan data yang SUDAH ADA; bila tidak ada
			// notifikasi tersimpan sama sekali, error LIVE asli tetap dilempar apa adanya agar admin
			// melihat akar masalah sebenarnya (mis. endpoint bank yang berubah/error).
			String notifTersimpan = virtualAccountBankReadOnly.getNotif();
			if (notifTersimpan != null && !notifTersimpan.trim().isEmpty()) {
				try {
					jsonObject2 = prosesHasilCekVaBankaltimtara(virtualAccountBankReadOnly, notifTersimpan, true);
				} catch (Exception eFallback) {
					ais.common.ErrorAuditUtil.record(eFallback,
							"auto-audit src/ais/action/servlet/Bankaltimtara.java:checkPakaiva fallback notif tersimpan gagal juga");
					throw errorLive;
				}
			} else {
				throw errorLive;
			}
		}

		return jsonObject2;
	}

	/**
	 * Olah satu string hasil pengecekan status VA Bankaltimtara (baik dari pengecekan LIVE ke
	 * gateway, maupun dari notifikasi yang sebelumnya tersimpan di kolom {@code notif}): cocokkan
	 * nominal, lalu tandai lunas via {@link VirtualAccountBank#bayarVa} bila belum.
	 *
	 * <p>Format LIVE membungkus nominal di {@code data.amount}; format notifikasi tersimpan (hasil
	 * webhook H2H sebelumnya, mis. {@code {"inst_id":...,"number":...,"amount":...,"reff":...,"date":...}})
	 * menaruh {@code amount} langsung di root — keduanya ditangani di sini.</p>
	 *
	 * @param dariNotifTersimpan true bila {@code hasil} berasal dari fallback notif tersimpan (bukan
	 *                           pengecekan live baru) — ditandai di JSON balikan agar admin tahu sumber datanya.
	 */
	private static JSONObject prosesHasilCekVaBankaltimtara(VirtualAccountBank virtualAccountBankReadOnly,
			String hasil, boolean dariNotifTersimpan) throws Exception {
		validasiResponJsonBankaltimtara(hasil, dariNotifTersimpan ? "notifikasi tersimpan" : "cek status VA");
		JSONObject jsonObject2 = new JSONObject(hasil);

		JSONObject object = ambilDataPembayaranBankaltimtara(jsonObject2);
		if (object == null) {
			throw new Exception("Respons cek status Bankaltimtara tidak memuat transaksi dengan field amount: "
					+ BankaltimtaraResponseUtil.ringkas(hasil) + ", VA id=" + virtualAccountBankReadOnly.getId());
		}

		Double paid = parseNominalBankaltimtara(object.get("amount"));
				// Sama seperti checkPakaiqris: bandingkan ke totalBiaya() (total+biaya admin), BUKAN
				// getTotal() saja -- konsisten dgn pengecekan H2H masuk di file ini (baris ~502) yg
				// SUDAH pakai totalBiaya(). Bug lama di sini: getTotal() saja membuat VA yg sudah lunas
				// (dgn biaya admin > 0) tak pernah cocok -> "Cek Ulang" diam-diam gagal update status.
		if (paid.intValue() == virtualAccountBankReadOnly.totalBiaya()) {
			if (VirtualAccountBank.isSudahTerbayar(virtualAccountBankReadOnly)) {
				return tandaiSumberData(jsonObject2, dariNotifTersimpan);
			}
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				VirtualAccountBank.bayarVa(virtualAccountBankReadOnly, WaktuUtil.getDate(), hasil, session);
			} finally {
				if (session != null) {
					try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:257");}
					try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:258");}
					try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:259");}
				}
			}
		} else {
					// Nominal dari bank TIDAK cocok totalBiaya() -- sebelumnya diam saja (VA tetap
					// tampak belum lunas tanpa keterangan). Catat spy "sudah bayar tapi status tak
					// berubah" bisa ditelusuri: idnya VA, nominal versi bank vs versi sistem.
					ais.common.ErrorAuditUtil.record(
							new Exception("Nominal cek Bankaltimtara tidak cocok: dibayar=" + paid.intValue()
									+ ", totalBiaya() sistem=" + virtualAccountBankReadOnly.totalBiaya()
									+ ", VA id=" + virtualAccountBankReadOnly.getId()),
					"Bankaltimtara.prosesHasilCekVaBankaltimtara: nominal tidak cocok, status VA tidak diperbarui");
			throw new Exception("Nominal pembayaran Bankaltimtara " + paid.intValue()
					+ " tidak sama dengan tagihan sistem " + virtualAccountBankReadOnly.totalBiaya()
					+ " untuk VA " + virtualAccountBankReadOnly.getKode() + ".");
		}

		return tandaiSumberData(jsonObject2, dariNotifTersimpan);
	}

	private static JSONObject ambilDataPembayaranBankaltimtara(JSONObject response) throws Exception {
		if (!response.isNull("data")) {
			Object data = response.get("data");
			if (data instanceof JSONObject) {
				JSONObject object = (JSONObject) data;
				if (!object.isNull("amount")) {
					return object;
				}
			} else if (data instanceof JSONArray) {
				JSONArray daftar = (JSONArray) data;
				for (int i = daftar.length() - 1; i >= 0; i--) {
					Object item = daftar.get(i);
					if (item instanceof JSONObject && !((JSONObject) item).isNull("amount")) {
						return (JSONObject) item;
					}
				}
			}
		}
		return response.isNull("amount") ? null : response;
	}

	private static Double parseNominalBankaltimtara(Object nominal) throws Exception {
		String nilai = nominal == null ? "" : nominal.toString().trim().replace(",", "");
		if (nilai.isEmpty()) {
			throw new Exception("Nominal pembayaran Bankaltimtara kosong.");
		}
		try {
			return Double.parseDouble(nilai);
		} catch (NumberFormatException e) {
			throw new Exception("Nominal pembayaran Bankaltimtara tidak dapat dibaca: " + nilai, e);
		}
	}

	private static JSONObject tandaiSumberData(JSONObject jsonObject2, boolean dariNotifTersimpan) {
		try {
			if (dariNotifTersimpan) {
				jsonObject2.put("_sumberData", "notifikasi_tersimpan_fallback");
				jsonObject2.put("_keterangan", "Pengecekan LIVE ke gateway Bankaltimtara gagal (lihat log admin "
						+ "untuk detail); hasil di atas diproses ULANG dari notifikasi TERAKHIR yang tersimpan, "
						+ "BUKAN hasil pengecekan live saat ini.");
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:tandaiSumberData");
		}
		return jsonObject2;
	}

	/**
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

	@SuppressWarnings("unchecked")
	public static JSONObject doProcess(double nominalP, String tanggalP, String va, String bank, BankHost bankHost,
			HttpServletRequest request, String data, boolean chekLagi, boolean inquery, boolean reversal, boolean chek)
			throws Exception {

		JSONObject response = new JSONObject(data);
		response.put("errorCode", "00");
		response.put("statusDescription", "Success");

		JSONArray billDetails = new JSONArray();

		if (inquery) {
			response.put("billDetails", billDetails);
		}

		{ // TANPA syarat bankHost: request bank apa pun WAJIB tercatat ke log H2H (lihat finally)
			String nim = "";
			String nama = "";
			JSONArray rincian = new JSONArray();
			// Jejak stack trace bila inquiry/pembayaran error; disimpan ke kolom log H2H.
			String h2hStackTrace = null;

			VirtualAccountBank virtualAccountBankNtt = null;
			Session session = null;
			try {

				virtualAccountBankNtt = VirtualAccountBank.ambilVa(va, nominalP, bankHost);

				if (virtualAccountBankNtt != null && virtualAccountBankNtt.getKadaluarsa() != null) {
					System.out.println(
							"Kadaluara " + Common.databaseDateFormat.get().format(virtualAccountBankNtt.getKadaluarsa()));
				}
				Date tanggal = ais.ui.util.WaktuUtil.getDate();
				// VALIDASI DEFENSIF: bank kadang mengirim trx_date kosong/tak lengkap
				// (callback duplikat/retry/health-check) - jangan langsung parse mentah.
				boolean tanggalTidakValid = false;
				if (tanggalP == null || tanggalP.trim().isEmpty()) {
					tanggalTidakValid = true;
					ais.common.ErrorAuditUtil.record(
							new Exception("trx_date kosong/tidak dikirim oleh bank pada callback Bankaltimtara"),
							"Bankaltimtara.doProcess: trx_date kosong, callback ditolak sebelum parse tanggal (baris ~321)");
				} else {
					try {
						tanggal = Common.databaseDateFormat1.get().parse(tanggalP);
					} catch (Exception e) {
						tanggalTidakValid = true;
						ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:322");
					}
				}
				Date msk = virtualAccountBankNtt == null ? null : virtualAccountBankNtt.getKadaluarsaWaktu();
				if (Double.isNaN(nominalP)) {
					// VALIDASI DEFENSIF: amount kosong/tak valid dari bank (lihat sentinel di doProses)
					// - tolak callback dengan respons error yang jelas, jangan lempar exception mentah.
					response.put("errorCode", "07");
					response.put("statusDescription", "Format nominal transaksi tidak valid");
				} else if (tanggalTidakValid) {
					response.put("errorCode", "06");
					response.put("statusDescription", "Format tanggal transaksi tidak valid");
				} else if (!chekLagi && virtualAccountBankNtt != null && (tanggal != null && msk != null
						&& Double.parseDouble(Common.dateFormat84.get().format(msk)) < Double
								.parseDouble(Common.dateFormat84.get().format(tanggal)))) {
					response.put("errorCode", "03");
					response.put("statusDescription", "Tagihan kadaluarsa");
				} else if (virtualAccountBankNtt == null) {
					response.put("errorCode", "01");
					response.put("statusDescription", "Nomor VA salah");
				}
				
				

//				else if (!reversal && !chek && virtualAccountBankNtt != null
//						&& virtualAccountBankNtt.getKadaluarsa().before(WaktuUtil.getDate())) {
//
//					response.put("errorCode", "02");
//					response.put("statusDescription", "Tagihan tidak tersedia");
//
//				} else {

				else if (reversal && virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1
						&& virtualAccountBankNtt.getKegiatan() == null
						&& virtualAccountBankNtt.getPembayaran() == null) {
					response = new JSONObject();
					response.put("errorCode", "05");
					response.put("statusDescription", "Reversal gagal");
				} else

//				if (!reversal && !chek && virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1
//						&& ((virtualAccountBankNtt.getKegiatan() != null || virtualAccountBankNtt.getPembayaran() != null)
//								|| virtualAccountBankNtt.getPembayaran() != null)) {
//					response.put("errorCode", "03");
//					response.put("statusDescription", "Tagihan sudah terbayar");
//				}
//
//				else 
					
					
					
					

				if (VirtualAccountBank.isSudahTerbayarUntukPayment(virtualAccountBankNtt, inquery, reversal, chek)) {
					response.put("errorCode", "03");
					response.put("statusDescription", "Tagihan sudah terbayar");
				}

				else if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1) {

					if (nominalP < 0.0) {
						nominalP = virtualAccountBankNtt.getTotal() + virtualAccountBankNtt.getBiayaAdmin();
					}

					String TahunID = "";
					try {
						TahunID = virtualAccountBankNtt.getTahunAkademik().split("/")[0];
						response.put("TahunID", TahunID);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:382");
						// TODO: handle exception
					}

					session = HibernateUtil.getSessionFactory().openSession();
					{

						Double nominal = nominalP;

						if (!inquery && nominal.intValue() != virtualAccountBankNtt.totalBiaya()) {
							response.put("errorCode", "04");
							response.put("statusDescription", "Nominal Tagihan tidak sesuai");
						} else {

							if (inquery) {

								response.put("billAmount", virtualAccountBankNtt.getTotal().intValue()
										+ virtualAccountBankNtt.getBiayaAdmin().intValue());

								if (virtualAccountBankNtt.getBiayaAdmin() > 0.1) {
									JSONObject bill = new JSONObject();
									bill.put("billID", "0010");
									bill.put("billName", "Biaya admin");
									bill.put("billNameID", "0");
									bill.put("billAmount", virtualAccountBankNtt.getBiayaAdmin().intValue());

									bill.put("billAmountBayar", 0);
									bill.put("billAmountPay", 0);
									bill.put("TahunID", TahunID);

									billDetails.put(bill);

								}
							} else {
								response.put("amount", virtualAccountBankNtt.getTotal().intValue()
										+ virtualAccountBankNtt.getBiayaAdmin().intValue());
							}

							

							if (virtualAccountBankNtt.getSiswa() != null
									|| virtualAccountBankNtt.getCalonSiswa() != null) {

								Sekolah sekolah = null;
								if (virtualAccountBankNtt.getSiswa() != null) {
									sekolah = virtualAccountBankNtt.getSiswa().getSekolah();
									nim = virtualAccountBankNtt.getSiswa().getNomorInduk();
									nama = virtualAccountBankNtt.getSiswa().getNama();

								} else if (virtualAccountBankNtt.getCalonSiswa() != null) {
									sekolah = virtualAccountBankNtt.getCalonSiswa().getSekolah();
									nim = virtualAccountBankNtt.getCalonSiswa().getNomorInduk();
									nama = virtualAccountBankNtt.getCalonSiswa().getNama();

								}

								response.put("Nama", nama);
								response.put("FormID", nim);
								response.put("ProdiNama", sekolah == null ? "" : sekolah.getNama());

								Map<String, List<Tagihan>> map = VirtualAccountBank.bayarSiswa(virtualAccountBankNtt,
										session, tanggal, bank, inquery, data, false);

								for (List<Tagihan> tagihans : map.values()) {

									for (Tagihan tagihan : tagihans) {

										if (inquery) {
											JSONObject bill = new JSONObject();

											bill.put("billID", tagihan.getItemBiayaSekolah().getKode());
											bill.put("billName", tagihan.getItemBiayaSekolah().getNama());
											bill.put("billNameID", tagihan.getItemBiayaSekolah().getId() + "");

											bill.put("billAmount", tagihan.getNominal().intValue());

											bill.put("billAmountBayar", 0);
											bill.put("billAmountPay", 0);
											bill.put("TahunID", TahunID);

											billDetails.put(bill);

											if (tagihan.getDenda() > 0.1) {
												bill = new JSONObject();
												bill.put("billID", "D" + tagihan.getItemBiayaSekolah().getKode());
												bill.put("billName",
														"Denda " + tagihan.getItemBiayaSekolah().getNama());
												bill.put("billNameID", "0" + tagihan.getItemBiayaSekolah().getId());
												bill.put("billAmount", tagihan.getDenda().intValue());

												bill.put("billAmountBayar", 0);
												bill.put("billAmountPay", 0);
												bill.put("TahunID", TahunID);

												billDetails.put(bill);
											}

										}
									}

								}

							} else {

								JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();
								Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
								BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt
										.getBiodataCalonMahasiswa();

								Integer semester = virtualAccountBankNtt.getSemester();

								nim = mahasiswa == null ? biodataCalonMahasiswa.getNoRegistrasi() : mahasiswa.getNim();
								nama = mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama();
								Jurusan jurusan = mahasiswa == null ? null : mahasiswa.getJurusan();
								if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getProdiLulus() != null) {
									jurusan = biodataCalonMahasiswa.getProdiLulus();
								} else if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getProdi1() != null) {
									jurusan = biodataCalonMahasiswa.getProdi1();
								}

								response.put("Nama", nama);
								response.put("FormID", nim);
								response.put("Gelombang",
										biodataCalonMahasiswa == null
												|| biodataCalonMahasiswa.getGelombangPendaftaran() == null ? ""
														: biodataCalonMahasiswa.getGelombangPendaftaran().getNama());
								response.put("ProdiNama", jurusan == null ? "" : jurusan.getNama());
								response.put("FakultasNama", jurusan == null ? "" : jurusan.getFakultas().getNama());

								Kegiatan kegiatan = (Kegiatan) (virtualAccountBankNtt.getKegiatan() == null ? null
										: session.createCriteria(Kegiatan.class)
												.add(Restrictions.idEq(virtualAccountBankNtt.getKegiatan()))
												.uniqueResult());

								if (kegiatan == null || kegiatan.getId() == null) {

									kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class)

											.addOrder(Order.asc("id"))

											.add(biodataCalonMahasiswa != null
													? Restrictions.eq("calonMahasiswa", biodataCalonMahasiswa)
													: Restrictions.eq("mahasiswa", mahasiswa))
											.add(Restrictions.eq("jenisKegiatan",
													virtualAccountBankNtt.getJenisKegiatan()))
											.add(Restrictions.eq("semster", semester))

											.setMaxResults(1).uniqueResult();
								}

								if (kegiatan == null || kegiatan.getId() == null) {
									kegiatan = new Kegiatan();
								}

								if (reversal) {

									kegiatan.setKodeUnikLain(virtualAccountBankNtt.getKodeUnikLain());
									kegiatan.setJadwalPembayaran(virtualAccountBankNtt.getJadwalPembayaran());
									kegiatan.setNama(nama);
									kegiatan.setUploadVirtualAccount(null);
									kegiatan.setAmount(virtualAccountBankNtt.getTotal());
									kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
									kegiatan.setMahasiswa(mahasiswa);
									kegiatan.setTahunAkademik(virtualAccountBankNtt.getTahunAkademik());
									kegiatan.setSemster(semester);
									kegiatan.setJenisKegiatan(jenisKegiatan);
									kegiatan.setTanggal(tanggal);
									kegiatan.setValidated(1);
									kegiatan.setValidator(bank);

									session.getTransaction().begin();
									Common.refreshSaveOrUpdate(session, kegiatan);
									session.getTransaction().commit();

									virtualAccountBankNtt.setKegiatan(null);

									List<Long> detailBiayasId = new ArrayList<Long>();
									for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(), ",")) {
										try {
											detailBiayasId.add(Long.parseLong(id.trim()));
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:562");

										}
									}

									Collection<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class)
											.add(detailBiayasId.isEmpty() ? Restrictions.sqlRestriction("false")
													: Restrictions.in("id", detailBiayasId))
											.list();

									Double nilaiBiayaHarusDiBayars = 0.0;
									for (DetailBiaya detailBiaya : detailBiayas) {

										DetailKegiatan detailKegiatan = kegiatan.ambilSatuDetailKegiatan(detailBiaya,
												session);
										if (detailKegiatan == null) {
											detailKegiatan = new DetailKegiatan();
										}

										Double biaya = detailBiaya.hitungTotalKegiatan(kegiatan, session);

										detailKegiatan.setBiaya(biaya);

										detailKegiatan.setDetailBiaya(detailBiaya);
										detailKegiatan.setKeterangan(detailBiaya.getKeterangan());
										detailKegiatan.setKegiatan(kegiatan);

										nilaiBiayaHarusDiBayars += biaya;

									}

									session.getTransaction().begin();
									session.createSQLQuery("delete from cicilan_pembayaran where ref_va = "
											+ virtualAccountBankNtt.getId()).executeUpdate();
									session.getTransaction().commit();

									Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session,
											kegiatan);
									Double jumlah = d[0];
									Double denda = d[1];
									kegiatan.setDenda(denda.doubleValue());
									kegiatan.setAmountTerhutang(
											nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));

									kegiatan.setAmount(jumlah.doubleValue() > 0.1 ? jumlah.doubleValue()
											: virtualAccountBankNtt.getTotal());
									kegiatan.setValidator(bank);

									session.getTransaction().begin();
									Common.refreshUpdate(session, kegiatan);
									session.getTransaction().commit();

									VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, null, data, bank);
								} else {

									kegiatan.setKodeUnikLain(virtualAccountBankNtt.getKodeUnikLain());
									kegiatan.setJadwalPembayaran(virtualAccountBankNtt.getJadwalPembayaran());
									kegiatan.setNama(nama);
									kegiatan.setUploadVirtualAccount(null);
									kegiatan.setAmount(virtualAccountBankNtt.getTotal());
									kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
									kegiatan.setMahasiswa(mahasiswa);
									kegiatan.setTahunAkademik(virtualAccountBankNtt.getTahunAkademik());
									kegiatan.setSemster(semester);
									kegiatan.setJenisKegiatan(jenisKegiatan);
									kegiatan.setTanggal(tanggal);
									kegiatan.setValidated(1);
									kegiatan.setValidator(bank);

									session.getTransaction().begin();
									Common.refreshSaveOrUpdate(session, kegiatan);
									session.getTransaction().commit();

									List<Long> detailBiayasId = new ArrayList<Long>();
									for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(), ",")) {
										try {
											detailBiayasId.add(Long.parseLong(id.trim()));
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:639");

										}
									}

									Collection<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class)
											.add(detailBiayasId.isEmpty() ? Restrictions.sqlRestriction("false")
													: Restrictions.in("id", detailBiayasId))
											.list();

									Double nilaiBiayaHarusDiBayars = 0.0;
									for (DetailBiaya detailBiaya : detailBiayas) {
										Double biaya = detailBiaya.hitungTotalKegiatan(kegiatan, session);

										nilaiBiayaHarusDiBayars += biaya;

									}

									Double[] totalCicilan = kegiatan.hitungTotalDanDendaFromCicilan();
									Double total = totalCicilan[0];
									Double totalTagihan = kegiatan.getAmount() + kegiatan.getAmountTerhutang();
									System.out.println(
											"cicilanPembayaran total -> " + total + " totalTagihan " + totalTagihan);

									if (virtualAccountBankNtt.getCicilan() != null
											&& !virtualAccountBankNtt.getCicilan().isEmpty()) {
										for (String idPemBul : StringUtils.split(virtualAccountBankNtt.getCicilan(),
												",")) {
											if (Common.isNumber(idPemBul)) {
												PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
														.createCriteria(PengaturanPembayaranBulanan.class)
														.add(Restrictions.idEq(Long.parseLong(idPemBul)))
														.uniqueResult();

												if (pengaturanPembayaranBulanan != null) {
													String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
															+ virtualAccountBankNtt.getId();

													ItemBiaya itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya()
															.getItemBiaya();

													Double subtotal = 0.0;
													try {
														String[] spl = idPemBul.split("-");
														subtotal = Double.parseDouble(spl[spl.length - 1]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:684");
													}

													if (itemBiaya.getPenghitungan()
															.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
														subtotal = 0.0 - subtotal;
													}

													if (inquery) {
														JSONObject bill = new JSONObject();

														bill.put("billID", itemBiaya.getKode());
														bill.put("billName", itemBiaya.getNama());
														bill.put("billNameID", itemBiaya.getId() + "");

														bill.put("billAmount", subtotal.intValue());

														bill.put("billAmountBayar", 0);
														bill.put("billAmountPay", 0);
														bill.put("TahunID", TahunID);

														billDetails.put(bill);

													} else {

														CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																.createCriteria(CicilanPembayaran.class)
																.add(Restrictions.eq("ref", ref)).setMaxResults(1)
																.uniqueResult();

														if (cicilanPembayaran == null) {
															cicilanPembayaran = new CicilanPembayaran(
																	pengaturanPembayaranBulanan.getDetailBiaya());
														}
														cicilanPembayaran.setRef(ref);
														cicilanPembayaran.setValidator(bank);
														cicilanPembayaran.setKegiatan(kegiatan);
														cicilanPembayaran.setItemBiaya(pengaturanPembayaranBulanan
																.getDetailBiaya().getItemBiaya());
														cicilanPembayaran.setPengaturanPembayaranBulanan(
																pengaturanPembayaranBulanan);
														cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());
														cicilanPembayaran.setNilai(subtotal);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														cicilanPembayaran.setTanggal(tanggal);
														cicilanPembayaran.setJenisPembayaran(bankHost == null
																|| bankHost.getJenisPembayaran() == null
																		? ConstantValues.TUNAI
																		: bankHost.getJenisPembayaran());
														cicilanPembayaran.setDenda(0.0);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());

														session.getTransaction().begin();
														if (cicilanPembayaran.getId() == null)
															session.save(cicilanPembayaran);
														else
															Common.refreshUpdate(session, cicilanPembayaran);
														session.getTransaction().commit();

														JSONObject jsonObjectRinci = new JSONObject();
														jsonObjectRinci.put("nama", pengaturanPembayaranBulanan
																.getDetailBiaya().getItemBiaya().getNama());
														jsonObjectRinci.put("bulan",
																pengaturanPembayaranBulanan.getNamaBulan());
														jsonObjectRinci.put("nominal", pengaturanPembayaranBulanan
																.ambilNominalModifikasi(mahasiswa, semester));

														rincian.put(jsonObjectRinci);

													}
												}
											} else if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
												PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
														.createCriteria(PengaturanPembayaranBulanan.class)
														.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1])))
														.uniqueResult();

												if (pengaturanPembayaranBulanan != null) {
													String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
															+ virtualAccountBankNtt.getId();

													ItemBiaya itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya()
															.getItemBiaya();
													Double subtotal = 0.0;
													try {
														String[] spl = idPemBul.split("-");
														subtotal = Double.parseDouble(spl[spl.length - 1]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:771");
													}

													if (itemBiaya.getPenghitungan()
															.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
														subtotal = 0.0 - subtotal;
													}

													if (inquery) {
														JSONObject bill = new JSONObject();

														bill.put("billID", itemBiaya.getKode());
														bill.put("billName", itemBiaya.getNama());
														bill.put("billNameID", itemBiaya.getId() + "");

														bill.put("billAmount", subtotal.intValue());

														bill.put("billAmountBayar", 0);
														bill.put("billAmountPay", 0);
														bill.put("TahunID", TahunID);

														billDetails.put(bill);

													} else {
														CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																.createCriteria(CicilanPembayaran.class)
																.add(Restrictions.eq("ref", ref)).setMaxResults(1)
																.uniqueResult();

														if (cicilanPembayaran == null) {
															cicilanPembayaran = new CicilanPembayaran(
																	pengaturanPembayaranBulanan.getDetailBiaya());
														}
														cicilanPembayaran.setRef(ref);
														cicilanPembayaran.setValidator(bank);
														cicilanPembayaran.setKegiatan(kegiatan);
														cicilanPembayaran.setItemBiaya(itemBiaya);
														cicilanPembayaran.setPengaturanPembayaranBulanan(
																pengaturanPembayaranBulanan);
														cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());

														cicilanPembayaran.setNilai(subtotal);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														cicilanPembayaran.setTanggal(tanggal);
														cicilanPembayaran.setJenisPembayaran(bankHost == null
																|| bankHost.getJenisPembayaran() == null
																		? ConstantValues.TUNAI
																		: bankHost.getJenisPembayaran());
														cicilanPembayaran.setDenda(0.0);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														session.getTransaction().begin();
														if (cicilanPembayaran.getId() == null)
															session.save(cicilanPembayaran);
														else
															Common.refreshUpdate(session, cicilanPembayaran);
														session.getTransaction().commit();

														JSONObject jsonObjectRinci = new JSONObject();
														jsonObjectRinci.put("nama", pengaturanPembayaranBulanan
																.getDetailBiaya().getItemBiaya().getNama());
														jsonObjectRinci.put("bulan",
																pengaturanPembayaranBulanan.getNamaBulan());
														jsonObjectRinci.put("nominal", subtotal);

														rincian.put(jsonObjectRinci);
													}
												}

											} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
												ItemBiaya itemBiaya = (ItemBiaya) ConstantValues
														.simpleObject(
																session.createCriteria(ItemBiaya.class)
																		.add(Restrictions.idEq(Long
																				.parseLong(idPemBul.split("-")[1]))),
																ItemBiaya.class);

												if (itemBiaya != null) {
													String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
															+ virtualAccountBankNtt.getId();
													Double subtotal = 0.0;
													try {
														String[] spl = idPemBul.split("-");
														subtotal = Double.parseDouble(spl[2]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:854");
													}

													Long detailBiayaId = null;
													try {
														String[] spl = idPemBul.split("-");
														detailBiayaId = Long.parseLong(spl[4]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:861");
													}

													if (itemBiaya.getPenghitungan()
															.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
														subtotal = 0.0 - subtotal;
													}

													if (inquery) {
														JSONObject bill = new JSONObject();

														bill.put("billID", itemBiaya.getKode());
														bill.put("billName", itemBiaya.getNama());
														bill.put("billNameID", itemBiaya.getId() + "");

														bill.put("billAmount", subtotal.intValue());

														bill.put("billAmountBayar", 0);
														bill.put("billAmountPay", 0);
														bill.put("TahunID", TahunID);

													} else {

														CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																.createCriteria(CicilanPembayaran.class)
																.add(Restrictions.eq("ref", ref)).setMaxResults(1)
																.uniqueResult();

														if (cicilanPembayaran == null) {
															cicilanPembayaran = new CicilanPembayaran(
																	DetailBiaya.muatRefAman(session, detailBiayaId));
														}
														cicilanPembayaran.setDetailBiaya(DetailBiaya.muatRefAman(session, detailBiayaId));
														cicilanPembayaran.setRef(ref);
														cicilanPembayaran.setValidator(bank);
														cicilanPembayaran.setKegiatan(kegiatan);
														cicilanPembayaran.setItemBiaya(itemBiaya);
														cicilanPembayaran.setPengaturanPembayaranBulanan(null);
														cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());

														cicilanPembayaran.setNilai(subtotal);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														cicilanPembayaran.setTanggal(tanggal);
														cicilanPembayaran.setJenisPembayaran(bankHost == null
																|| bankHost.getJenisPembayaran() == null
																		? ConstantValues.TUNAI
																		: bankHost.getJenisPembayaran());
														cicilanPembayaran.setDenda(0.0);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														session.getTransaction().begin();
														if (cicilanPembayaran.getId() == null)
															session.save(cicilanPembayaran);
														else
															Common.refreshUpdate(session, cicilanPembayaran);
														session.getTransaction().commit();

														JSONObject jsonObjectRinci = new JSONObject();
														jsonObjectRinci.put("nama", itemBiaya.getNama());
														jsonObjectRinci.put("bulan", "");
														jsonObjectRinci.put("nominal", subtotal);

														rincian.put(jsonObjectRinci);
													}

												}
											} else if (idPemBul != null && idPemBul.startsWith("Keranjang-")) {
												// Pembayaran Keranjang Belanja (multi jenis / KegiatanTemporary): konversi draf
												// menjadi Kegiatan+Cicilan nyata — pemroses terpusat yang sama dengan Esmartlink.
												ais.action.ws.util.PembayaranGatewayHelper.prosesSatuTokenKeranjang(session, idPemBul,
														virtualAccountBankNtt, inquery, bank, bankHost, tanggal, data, null);
											}
										}
									}

									response.put("numBill", billDetails.length());

									if (reversal) {
										response = new JSONObject();
										response.put("errorCode", "00");
										response.put("statusDescription", "Success");
									}

									else if (inquery) {

									} else {

										response.put("UserLogin", "");
										response.put("Password", "");

										Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session,
												kegiatan);
										Double jumlah = d[0];
										Double denda = d[1];
										kegiatan.setDenda(denda.doubleValue());
										kegiatan.setAmountTerhutang(
												nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));

										kegiatan.setAmount(jumlah.doubleValue() > 0.1 ? jumlah.doubleValue()
												: virtualAccountBankNtt.getTotal());
										kegiatan.setValidator(bank);

										session.getTransaction().begin();
										Common.refreshUpdate(session, kegiatan);
										session.getTransaction().commit();

										VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan, data,
												bank);

									}

								}
							}

							response = new JSONObject();
							response.put("code", "00");
							response.put("message", "Success");
						}
					}

				}
//				}
			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();

				if (reversal) {
					response = new JSONObject();
					response.put("errorCode", "05");
					response.put("statusDescription", "Reversal gagal");
				} else {

					response.put("errorCode", "91");
					response.put("statusDescription", "Link Down");
				}
				h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
				Common.tampilErrorJikaAdmin(e);
			} finally {
				// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception
				// (helper: session terdedikasi + commit + retry, tak pernah gagal).
				if (session != null) {
					try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:1000");}
					try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:1001");}
					try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Bankaltimtara.java:1002");}
				}
				ais.action.ws.util.PembayaranGatewayHelper.catatLogHostToHost(request, bankHost, data, nim, va, nama,
						response.toString(), nominalP, rincian.toString(), h2hStackTrace);
			}
		}
		return response;
	}

	public static String doProses(String data, HttpServletRequest request, BankHost bankHost, String bank, boolean chek)
			throws Exception {
		JSONObject req = data == null ? null : new JSONObject(data);

		String va = req == null || req.isNull("number") ? (request == null ? "" : request.getParameter("number"))
				: req.getString("number");

		double nominalP = 0.0;
		if (req != null && !req.isNull("amount")) {
			// VALIDASI DEFENSIF: bank kadang mengirim amount kosong ("")
			// (callback duplikat/retry/health-check) - jangan langsung parseDouble mentah.
			// Sentinel Double.NaN dipakai supaya tak bentrok dengan nominalP<0.0 (arti lain: pakai total tagihan).
			String amountStr = req.get("amount") + "";
			if (amountStr.trim().isEmpty()) {
				nominalP = Double.NaN;
				System.out.println("Callback Bankaltimtara ditolak: amount kosong.");
			} else {
				try {
					nominalP = Double.parseDouble(amountStr);
				} catch (Exception e) {
					nominalP = Double.NaN;
					System.out.println("Callback Bankaltimtara ditolak: amount tidak valid. " + e.getMessage());
				}
			}
		}

		if (req != null && !req.isNull("kd_tagihan")) {
			va = req.get("kd_tagihan") + "";
			nominalP = -1.0;
		}

		boolean reversal = false;
		if (request != null && request.getRequestURI().endsWith("BankaltimtaraReversal")) {
			reversal = true;
		}

		String tanggalP = null;
		if (req != null && !req.isNull("trx_date")) {
			tanggalP = req.get("trx_date") + "";
		}
		if (Double.isNaN(nominalP) || tanggalP == null || tanggalP.trim().length() == 0) {
			System.out.println("Callback Bankaltimtara ditolak: amount/trx_date wajib belum lengkap.");
			return errorDb(false, data);
		}

		String body;
		try {
			// 05, request tidak diizinkan
			body = Bankaltimtara
					.doProcess(nominalP, tanggalP, va, bank, bankHost, request, data, true, false, reversal, chek)
					.toString();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Bankaltimtara.java:1047");
			body = errorDb(false, data);

		}

		if (Common.bolehKonfigurasi("bankaltimtara_va_sleep", Konfigurasi.TIDAK_AKTIF)) {
			Thread.sleep(3 * 1000);
			body = timeoutDb(req.isNull("amount"), data);
		}

		System.out.println("response->" + body);

		return body;
	}

	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Read from request

		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String data = buffer.toString();

		String querystring = request.getQueryString();
		System.out.println("==> VA data => " + data);
		System.out.println("==> VA querystring => " + querystring);

		String bank = "BMS";
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), bank);

		String body = doProses(data, request, bankHost, bank, false);

		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();
		writer.write(body);
		writer.flush();

	}

	private static String timeoutDb(boolean inquery, String data) throws Exception {

		JSONObject jsonObjectResponse = new JSONObject(data);
		jsonObjectResponse.put("errorCode", "68");
		jsonObjectResponse.put("statusDescription", "Connection Timeout");
		return jsonObjectResponse.toString();
	}

	private static String errorDb(boolean inquery, String data) throws Exception {

		JSONObject jsonObjectResponse = new JSONObject(data);
		jsonObjectResponse.put("errorCode", "91");
		jsonObjectResponse.put("statusDescription", "Link Down");
		return jsonObjectResponse.toString();
	}
}
