package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import ais.action.ws.PembayaranAction;
import ais.action.ws.model.Response;
import ais.common.Common;
import ais.database.model.LogHostToHost;

/**
 * Komponen batas HTTP/servlet untuk payment. Tipe ini menerima input dari luar aplikasi,
 * meneruskannya ke layanan domain, lalu membentuk respons tanpa menduplikasi aturan bisnis.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * HttpServlet}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PembayaranAction action};
 * pembacaan/pencarian ({@code doGet()}); operasi domain lain ({@code doPost()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * <h4>Gerbang otentikasi/otorisasi (verifikasi 2026-09-07, ditambal 2026-09-07)</h4>
 * <p>{@link #doGet} MEMANG memiliki gerbang, dua lapis, bukan anonim seperti servlet
 * {@code Struk}/{@code AmbilLaporanDaftarPegawai} di paket yang sama:</p>
 * <ol>
 *   <li><b>Rahasia bersama {@code PassApp}</b> &mdash; badan JSON permintaan wajib memuat
 *       {@code PassApp} yang cocok dengan konfigurasi {@code BrivaPassApp}, diperiksa lewat
 *       {@link #cocokPassApp} (fail-closed, perbandingan waktu-konstan).</li>
 *   <li><b>Daftar putih IP pemanggil</b> &mdash; {@code action.pay(...)} meneruskan
 *       {@code request} ke {@code PembayaranUtil.getBankHost(HttpServletRequest)}, yang
 *       mencocokkan alamat IP pemanggil terhadap entitas {@code BankHost} tersimpan.</li>
 * </ol>
 * <p><b>Riwayat keamanan (DIPERBAIKI 2026-09-07):</b> nilai bawaan rahasia {@code BrivaPassApp}
 * sebelumnya berupa literal {@code "1234567890"} yang tertulis di kode sumber DAN ditampilkan
 * apa adanya pada layar konfigurasi admin ({@code KonfigurasiNewAction}) &mdash; bila operator
 * belum pernah mengubahnya, rahasia itu bukan lagi rahasia. Kini default-nya string kosong dan
 * {@link #cocokPassApp} menolak seluruh permintaan selama konfigurasi belum diisi. Lihat juga
 * {@code ais.action.ws.util.PembayaranUtil#getBankHost(HttpServletRequest)} untuk perbaikan
 * kepercayaan header proxy dan fallback wildcard {@code BankHost} pada gerbang IP.</p>
 *
 * @see HttpServlet
 */
public class Payment extends HttpServlet {
	/**
	 * Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai secara fungsional karena
	 * instance servlet tidak pernah diserialisasi oleh kontainer pada penyebaran AIS.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Layanan domain yang benar-benar memvalidasi rahasia {@code PassApp}, mencocokkan IP
	 * pemanggil terhadap {@code BankHost}, dan memposting pembayaran; lihat
	 * {@link ais.action.ws.PembayaranAction#pay}.
	 */
	private PembayaranAction action = new PembayaranAction();

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet. Tidak melakukan
	 * inisialisasi apa pun selain pembuatan {@link #action} pada deklarasi field.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public Payment() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani notifikasi pembayaran host-to-host dari bank (kanal BRI Briva): membaca badan
	 * permintaan sebagai JSON, memverifikasi {@code PassApp} (lihat bagian Keamanan pada
	 * dokumentasi kelas), lalu meneruskan data pembayaran ke {@link #action}
	 * ({@link ais.action.ws.PembayaranAction#pay}) dan menuliskan balasan JSON berisi status
	 * bill.
	 *
	 * <h4>Urutan kerja</h4>
	 * <ol>
	 *   <li>badan permintaan dibaca baris demi baris lalu diurai sebagai {@link JSONObject};</li>
	 *   <li>{@code PassApp} dicocokkan; gagal &rarr; {@code StatusBill=11}, tidak ada data lain
	 *       yang diproses;</li>
	 *   <li>berhasil &rarr; parameter {@code BrivaNum}, {@code TransaksiID},
	 *       {@code TransmisiDateTime}, {@code TerminalID}, {@code PaymentAmount}, {@code BankID}
	 *       dibaca; {@code BrivaNum} boleh dipotong dari depan sejumlah karakter yang ditentukan
	 *       konfigurasi {@code substrBriOnline} (bawaan 0, tanpa pemotongan);</li>
	 *   <li>{@link ais.database.model.LogHostToHost} diisi untuk audit lalu {@code action.pay(...)}
	 *       dipanggil; hasilnya ({@code Response}) diterjemahkan menjadi {@code StatusPayment}
	 *       ({@code ErrorDesc}/{@code ErrorCode}/{@code isError}) dan info tambahan (nama, prodi,
	 *       fakultas, semester).</li>
	 * </ol>
	 * <p>Balasan selalu berupa {@code application/json} dengan header
	 * {@code Access-Control-Allow-Origin: *} (CORS terbuka untuk semua origin), terlepas dari
	 * hasil verifikasi {@code PassApp}.</p>
	 *
	 * @param request  permintaan masuk; badan (bukan parameter form) berisi JSON notifikasi bank
	 * @param response balasan JSON status pembayaran
	 * @throws ServletException tidak pernah dilempar keluar method ini; seluruh kegagalan
	 *                          ditelan oleh blok {@code catch} internal
	 * @throws IOException      dapat dilempar bila penulisan balasan ({@code writer.write})
	 *                          gagal
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub

		JSONObject jsonObject = new JSONObject();

		try {
//			jsonObject.put("BillDetail", new JSONObject());
			jsonObject.put("Info1", "Payment");
			jsonObject.put("Info2", "");
			jsonObject.put("Info3", "");
			jsonObject.put("Info4", "");
			jsonObject.put("Info5", "");
//			jsonObject.put("StatusBill", "2");
//			jsonObject.put("Currency", "IDR");

			StringBuilder buffer = new StringBuilder();
			BufferedReader reader = request.getReader();
			String line;
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
			}
			String data = buffer.toString();
			JSONObject req = new JSONObject(data);

			System.out.println(req);

			String PassApp = req.getString("PassApp");
			if (!cocokPassApp(PassApp)) {
				jsonObject.put("StatusBill", "11");
			} else {

				String nim = req.getString("BrivaNum");
				String reffNumber = req.getString("TransaksiID");
				String tanggalBayar = req.getString("TransmisiDateTime");
				String jamBayar = req.getString("TransmisiDateTime");
				String userID = req.getString("TerminalID");
				String nominalTagihan = req.getString("PaymentAmount");
				String namaCabang = req.getString("BankID");
				
				
				

				String nama = ("=============================== PAY PAYMENT --> pay dengan NIM = " + nim
						+ ", reffNumber = " + reffNumber + ", tanggalBayar = " + tanggalBayar + ", userID = " + userID
						+ ", namaCabang = " + namaCabang + ", nominalTagihan = " + nominalTagihan);
				LogHostToHost logHostToHost = new LogHostToHost();
				logHostToHost.setInfo0(nim);
				logHostToHost.setInfo1(reffNumber);
				logHostToHost.setInfo2(tanggalBayar);
				logHostToHost.setInfo3(jamBayar);
				logHostToHost.setInfo4(userID);
				logHostToHost.setInfo5(namaCabang);
				logHostToHost.setInfo6(nominalTagihan);

				int substrBriOnline = 0;
				try {
					substrBriOnline = Integer.parseInt(Common.getKonfigurasi("substrBriOnline", "0").getNilai());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Payment.java:93");
					// TODO: handle exception
				}

				String n = substrBriOnline == 0 ? nim : nim.substring(substrBriOnline);

				Response resp = action.pay(n, reffNumber, tanggalBayar, jamBayar, userID, namaCabang, nominalTagihan,
						nama, logHostToHost, request);
				JSONObject StatusPayment = new JSONObject();
				StatusPayment.put("ErrorDesc", resp.getResponse_code().equals("03") ? "Bill Already paid"
						: resp.getResponse_code().equals("00") ? "Success" : "Fail");
				StatusPayment.put("ErrorCode", resp.getResponse_code());
				StatusPayment.put("isError", resp.getResponse_code().equals("00") ? "0" : "1");

//				if (resp.getResponse_code().equalsIgnoreCase(ConstantUtil.SUCCESS)) {
//					jsonObject.put("StatusBill", "0");
//				} else if (resp.getResponse_code().equalsIgnoreCase(ConstantUtil.BILLS_HAVE_BEEN_PAID)) {
//					jsonObject.put("StatusBill", "1");
//				} else if (resp.getResponse_code().equalsIgnoreCase(ConstantUtil.NIM_NOT_FOUND)
//						|| resp.getResponse_code().equalsIgnoreCase(ConstantUtil.BILLS_NOT_FOUND)) {
//					jsonObject.put("StatusBill", "2");
//				} else if (resp.getResponse_code().equalsIgnoreCase(ConstantUtil.BILLS_HAS_EXPIRED)) {
//					jsonObject.put("StatusBill", "3");
//				} else {
//					jsonObject.put("StatusBill", "9");
//				}
				jsonObject.put("StatusPayment", StatusPayment);
				jsonObject.put("Info1", resp.getNama());
				jsonObject.put("Info2", resp.getProdi());
				jsonObject.put("Info3", resp.getFakultas());
				jsonObject.put("Info4", "Semester " + resp.getSemester());
				jsonObject.put("Info5", resp.getInfo1());
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Payment.java:127");
		}
		String body = jsonObject.toString();
		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		PrintWriter writer = response.getWriter();

		writer.write(body);
	}

	/**
	 * Menangani permintaan HTTP POST dengan perilaku identik {@link #doGet} &mdash; notifikasi
	 * bank host-to-host dapat dikirim lewat metode HTTP apa pun karena keduanya diproses sama.
	 *
	 * @param request  permintaan masuk; badan berisi JSON notifikasi bank
	 * @param response balasan JSON status pembayaran
	 * @throws ServletException diteruskan apa adanya dari {@link #doGet}
	 * @throws IOException      diteruskan apa adanya dari {@link #doGet}
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

	/**
	 * Membandingkan {@code PassApp} yang dikirim pemanggil dengan konfigurasi
	 * {@code BrivaPassApp}, dipakai oleh {@link #doGet}. Mengikuti pola
	 * {@code Wa#cocokTokenVerifikasiWebhook}.
	 *
	 * <p><b>Fail-closed:</b> bila konfigurasi belum diisi (masih string kosong, nilai
	 * defaultnya), method ini selalu mengembalikan {@code false} &mdash; konfigurasi kosong
	 * tidak pernah dianggap cocok dengan {@code PassApp} kosong dari pemanggil. Perbandingan
	 * dilakukan dengan {@link MessageDigest#isEqual} atas byte UTF-8 supaya waktu eksekusinya
	 * tidak membocorkan panjang kecocokan awalan.</p>
	 *
	 * @param passApp nilai {@code PassApp} dari badan permintaan; boleh {@code null}
	 * @return {@code true} hanya bila konfigurasi terisi dan cocok persis dengan {@code passApp}
	 */
	private boolean cocokPassApp(String passApp) {
		String expected = Common.getKonfigurasi("BrivaPassApp", "").getNilai();
		expected = expected == null ? "" : expected.trim();
		if (expected.isEmpty() || passApp == null) {
			return false;
		}
		return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
				passApp.getBytes(StandardCharsets.UTF_8));
	}

}
