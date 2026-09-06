package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;

/**
 * Servlet API JSON publik ("data_mahasiswa") yang mengembalikan daftar {@link Mahasiswa}
 * beserta biodata dan status kemahasiswaannya, dipetakan ke URL {@code /DataMahasiswa} (lihat
 * {@code web.xml}), dipakai untuk integrasi pihak ketiga (mis. sistem PDDikti/lapor eksternal).
 *
 * <h4>Keamanan &mdash; gerbang password OPSIONAL, TANPA cakupan institusi (diverifikasi dari
 * kode berjalan, 2026-09-07)</h4>
 * <p>{@link #process} membaca password dari header HTTP (lewat
 * {@link #ambilPasswordDariHeader(HttpServletRequest)}: {@code password},
 * {@code api_data_mahasiswa_password}, {@code X-API-PASSWORD}, atau {@code X-API-KEY}) dan
 * membandingkannya dengan konfigurasi {@code api_data_mahasiswa_password} &mdash; TETAPI hanya
 * bila nilai konfigurasi tersebut TIDAK KOSONG. Bila konfigurasi belum diisi (nilai default
 * pada instalasi baru), permintaan TIDAK diblokir sama sekali: siapa pun dapat memanggil
 * endpoint ini tanpa kredensial apa pun. Bahkan ketika password dikonfigurasi dan diperiksa
 * dengan benar, query {@link Mahasiswa} pada {@link #process} TIDAK memiliki penyaring
 * institusi/perguruan tinggi/prodi apa pun (hanya filter opsional {@code nim}/{@code nama}
 * dengan {@code MatchMode.ANYWHERE}) &mdash; sehingga API ini, begitu diberi kredensial yang
 * benar (atau tanpa kredensial sama sekali bila belum dikonfigurasi), membocorkan PII
 * (nama, tanggal lahir, jenis kelamin, email, HP, agama, status pernikahan, alamat domisili,
 * alamat KTP) SELURUH mahasiswa LINTAS SEMUA perguruan tinggi pada instalasi, bukan hanya milik
 * pemanggil. Pola ini konsisten dengan kelas masalah cakupan tenant/satker yang berulang di
 * seluruh basis kode ini (lihat servlet/action serupa yang sudah diperbaiki dengan gerbang
 * cakupan satker/prodi eksplisit); dicatat di sini sebagai FAKTA arsitektur endpoint ini, bukan
 * sebagai temuan baru yang belum pernah tercatat pada kelas masalah tersebut.</p>
 */
public class DataMahasiswa extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Daftar nama method getter alamat KTP yang dicoba berurutan lewat refleksi pada
	 * {@link #ambilStringDariGetter(Object, String[])}, menampung variasi ejaan
	 * ("Ktp"/"KTP") nama field pada model {@link BiodataMahasiswa}/{@link Mahasiswa} yang
	 * berbeda-beda.
	 */
	private static final String[] GETTER_ALAMAT_KTP = new String[] { "getAlamatKtp", "getAlamatKTP",
			"getAlamatSesuaiKtp", "getAlamatSesuaiKTP", "getAlamatKartuTandaPenduduk" };
	/**
	 * Daftar nama method getter kota/kabupaten KTP yang dicoba berurutan lewat refleksi,
	 * menampung variasi ejaan ("Kota"/"Kabupaten", "Ktp"/"KTP") nama field pada model yang
	 * berbeda-beda.
	 */
	private static final String[] GETTER_KOTA_KTP = new String[] { "getKotaKtp", "getKotaKTP",
			"getKotaKabupatenKtp", "getKotaKabupatenKTP", "getKabupatenKotaKtp", "getKabupatenKotaKTP",
			"getKabupatenKtp", "getKabupatenKTP" };
	/**
	 * Daftar nama method getter provinsi/propinsi KTP yang dicoba berurutan lewat refleksi,
	 * menampung variasi ejaan ("Propinsi"/"Provinsi", "Ktp"/"KTP") nama field pada model yang
	 * berbeda-beda.
	 */
	private static final String[] GETTER_PROPINSI_KTP = new String[] { "getPropinsiKtp", "getPropinsiKTP",
			"getProvinsiKtp", "getProvinsiKTP", "getPropinsiSesuaiKtp", "getPropinsiSesuaiKTP",
			"getProvinsiSesuaiKtp", "getProvinsiSesuaiKTP", "getPropinsiKartuTandaPenduduk",
			"getProvinsiKartuTandaPenduduk" };

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet; tidak melakukan inisialisasi
	 * khusus.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public DataMahasiswa() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan ke {@link #process}; kegagalan
	 * ditangkap dan hanya ditampilkan ke administrator lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}, tidak dilempar ke kontainer.
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
	 * Menangani permintaan HTTP POST dengan perilaku identik {@link #doGet}.
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
	 * Menangani permintaan HTTP OPTIONS (preflight CORS): dibutuhkan agar request
	 * browser/client yang menggunakan custom header {@code password}/
	 * {@code api_data_mahasiswa_password} dapat melewati preflight CORS. Menyetel header
	 * JSON+CORS yang sama seperti respons sesungguhnya lewat {@link #setJsonHeader} lalu
	 * membalas {@link HttpServletResponse#SC_OK} tanpa memproses data apa pun.
	 *
	 * @param request  permintaan preflight masuk (tidak dipakai)
	 * @param response balasan yang diisi header CORS dan status 200
	 */
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		setJsonHeader(response);
		response.setStatus(HttpServletResponse.SC_OK);
	}

	/**
	 * Inti servlet API: memeriksa password opsional (lihat bagian Keamanan pada dokumentasi
	 * kelas), lalu mengembalikan daftar {@link Mahasiswa} (dipaginasi lewat {@code start}/
	 * {@code max}, disaring opsional lewat {@code nim}/{@code nama}) sebagai JSON, masing-masing
	 * digabung dengan biodata, status kemahasiswaan terkini, dan alamat KTP (dicoba lewat
	 * berbagai nama getter pada {@link #ambilStringDariGetter(Object, String[])}).
	 * <p>TIDAK ADA penyaring institusi/perguruan tinggi pada query &mdash; lihat bagian
	 * Keamanan pada dokumentasi kelas.</p>
	 *
	 * @param request  permintaan masuk; parameter {@code nim}, {@code nama}, {@code start},
	 *                 {@code max} bersifat opsional, dan header password diperiksa lewat
	 *                 {@link #ambilPasswordDariHeader(HttpServletRequest)}
	 * @param response balasan yang diisi JSON array hasil (atau JSON error dengan status
	 *                 {@link HttpServletResponse#SC_UNAUTHORIZED} bila password salah)
	 * @throws Exception bila akses basis data gagal
	 */
	@SuppressWarnings({ })
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		setJsonHeader(response);
		JSONArray hasil = new JSONArray();

		String nim = request.getParameter("nim");
		String nama = request.getParameter("nama");
		String start = request.getParameter("start");
		String max = request.getParameter("max");

		// Password API sekarang dibaca dari HTTP Header, bukan parameter URL/body.
		// Header utama yang dipakai: password.
		// Header api_data_mahasiswa_password ikut didukung agar namanya sama dengan key konfigurasi.
		String password = ambilPasswordDariHeader(request);

		try {
			String passwordD = Common.getKonfigurasi("api_data_mahasiswa_password", "").getNilai().trim();
			if (!passwordD.isEmpty()) {
				if (password == null || !password.trim().equalsIgnoreCase(passwordD)) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("error", "Password salah");
					hasil.put(jsonObject);
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					tulisResponse(response, hasil);
					hasil = null;
					return;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/DataMahasiswa.java:117");
			// Jika konfigurasi belum tersedia, API tetap mengikuti perilaku lama: tidak memblokir request.
		}

		Integer startD = 0;
		try {
			startD = Integer.parseInt(start.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/DataMahasiswa.java:124");
			// TODO: handle exception
		}
		Integer maxD = 10;
		try {
			maxD = Integer.parseInt(max.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/DataMahasiswa.java:130");
			// TODO: handle exception
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			List<Mahasiswa> mahasiswas = ConstantValues
					.simpleList(session.createCriteria(Mahasiswa.class).setFirstResult(startD).setMaxResults(maxD)
							.add(nim == null || nim.trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.ilike("nim", nim, MatchMode.ANYWHERE))
							.add(nama == null || nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.ilike("nama", nama, MatchMode.ANYWHERE))
							.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim")), Mahasiswa.class);

			for (Mahasiswa mahasiswa : mahasiswas) {
				try {
					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
					BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
					HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa);

					JSONObject jsonObject = new JSONObject();
					jsonObject.put("nama", mahasiswa.getNama());
					jsonObject.put("nim", mahasiswa.getNim());
					jsonObject.put("tanggalLahir", mahasiswa.getTanggallahir() == null ? ""
							: Common.dateFormat1.get().format(mahasiswa.getTanggallahir()));
					jsonObject.put("jenisKelamin", mahasiswa.getKelamin());

					// Tambahan field yang dibutuhkan oleh integrasi API.
					jsonObject.put("email", mahasiswa.getEmail() == null ? "" : mahasiswa.getEmail());
					jsonObject.put("hp", mahasiswa.getTelp() == null ? "" : mahasiswa.getTelp());

					jsonObject.put("agama", mahasiswa.getAgama() == null ? "" : mahasiswa.getAgama().getNama());

					String statusNikah = "";
					if (biodataMahasiswa != null && biodataMahasiswa.getStatusNikah() != null) {
						statusNikah = biodataMahasiswa.getStatusNikah().equals(0) ? "Belum Nikah"
								: biodataMahasiswa.getStatusNikah().equals(1) ? "Nikah"
										: biodataMahasiswa.getStatusNikah().equals(2) ? "Janda" : "Duda";
					}

					jsonObject.put("statusPernikahan", statusNikah);
					jsonObject.put("statusKemahasiswaan",
							historyStatusMahasiswa == null || historyStatusMahasiswa.getStatusMahasiswa() == null ? ""
									: historyStatusMahasiswa.getStatusMahasiswa().getNama());

					jsonObject.put("namaPerguruanTinggi",
							mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null
									|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
											: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama());

					jsonObject.put("tanggalMasukKuliah", mahasiswa.getTanggalMasuk() == null ? ""
							: Common.dateFormat1.get().format(mahasiswa.getTanggalMasuk()));

					jsonObject.put("kodePerguruanTinggi",
							mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null
									|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
											: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi()
													.getKodePerguruanTinggi());
					jsonObject.put("statusPerguruanTinggi",
							mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null
									|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
											: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getStatus());
					jsonObject.put("namaProgramStudi",
							mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());
					jsonObject.put("jenjang", mahasiswa.getJenjang() == null ? "" : mahasiswa.getJenjang().getNama());
					jsonObject.put("semester", krsMahasiswa == null ? 0 : krsMahasiswa.getSemester());

					jsonObject.put("ipk", krsMahasiswa == null ? 0.0 : krsMahasiswa.getIpk());
					jsonObject.put("sks", krsMahasiswa == null ? 0 : krsMahasiswa.getSksk());
					jsonObject.put("provinsiDanKotaKabupatenPerguruanTinggi", mahasiswa.getJurusan() == null
							|| mahasiswa.getJurusan().getFakultas() == null
							|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getKota() + " "
											+ mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getPropinsi());

					jsonObject.put("alamatDomisili", mahasiswa.getAlamat());
					jsonObject.put("provinsiDanKotaKabupatenDomisili",
							biodataMahasiswa == null ? ""
									: (biodataMahasiswa.getKota() == null ? "" : biodataMahasiswa.getKota().getNama()) + " "
											+ (biodataMahasiswa.getPropinsi() == null ? ""
													: biodataMahasiswa.getPropinsi().getNama()));

					String alamatKtp = ambilStringDariGetter(biodataMahasiswa, GETTER_ALAMAT_KTP);
					if (alamatKtp.trim().isEmpty()) {
						alamatKtp = ambilStringDariGetter(mahasiswa, GETTER_ALAMAT_KTP);
					}

					String kotaKtp = ambilStringDariGetter(biodataMahasiswa, GETTER_KOTA_KTP);
					if (kotaKtp.trim().isEmpty()) {
						kotaKtp = ambilStringDariGetter(mahasiswa, GETTER_KOTA_KTP);
					}

					String propinsiKtp = ambilStringDariGetter(biodataMahasiswa, GETTER_PROPINSI_KTP);
					if (propinsiKtp.trim().isEmpty()) {
						propinsiKtp = ambilStringDariGetter(mahasiswa, GETTER_PROPINSI_KTP);
					}

					jsonObject.put("alamatKtp", alamatKtp);
					jsonObject.put("provinsiDanKotaKabupatenKtp", gabungDenganSpasi(kotaKtp, propinsiKtp));

					hasil.put(jsonObject);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/DataMahasiswa.java:233");
					// TODO: handle exception
				}
			}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DataMahasiswa.java:239");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DataMahasiswa.java:240");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DataMahasiswa.java:241");}
			}
		}

		tulisResponse(response, hasil);
		hasil = null;
	}

	/**
	 * Membaca password API dari header HTTP, mencoba berurutan header {@code password},
	 * {@code api_data_mahasiswa_password}, {@code X-API-PASSWORD}, lalu {@code X-API-KEY}
	 * (nama header pertama yang berisi nilai non-kosong dipakai). Password tidak lagi dibaca
	 * dari parameter URL/body sejak perubahan ini.
	 *
	 * @param request permintaan masuk
	 * @return nilai header password pertama yang ditemukan, atau {@code null} bila tidak ada
	 *         satu pun header tersebut yang berisi nilai
	 */
	private String ambilPasswordDariHeader(HttpServletRequest request) {
		String password = request.getHeader("password");
		if (password == null || password.trim().isEmpty()) {
			password = request.getHeader("api_data_mahasiswa_password");
		}
		if (password == null || password.trim().isEmpty()) {
			password = request.getHeader("X-API-PASSWORD");
		}
		if (password == null || password.trim().isEmpty()) {
			password = request.getHeader("X-API-KEY");
		}
		return password;
	}

	/**
	 * Menyetel header respons standar API ini: tipe konten JSON UTF-8 dan header CORS
	 * permisif ({@code Access-Control-Allow-Origin: *}, metode GET/POST/OPTIONS, serta daftar
	 * header kustom yang diizinkan termasuk varian password).
	 *
	 * @param response balasan yang akan diisi header
	 */
	private void setJsonHeader(HttpServletResponse response) {
		//response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Type", "application/json; charset=UTF-8");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		response.addHeader("Access-Control-Allow-Headers", "password, api_data_mahasiswa_password, X-API-PASSWORD, X-API-KEY, Content-Type");
	}

	/**
	 * Menuliskan {@code hasil} sebagai teks JSON ke {@code response}, sekaligus menyetel
	 * header non-standar {@code length} berisi panjang teks JSON.
	 *
	 * @param response balasan yang akan diisi body JSON
	 * @param hasil    array JSON yang akan dituliskan
	 * @throws IOException bila penulisan ke response gagal
	 */
	private void tulisResponse(HttpServletResponse response, JSONArray hasil) throws IOException {
		String body = hasil.toString();
		response.setHeader("length", body.length() + "");
		PrintWriter writer = response.getWriter();
		writer.write(body);
	}

	/**
	 * Mencoba memanggil, berurutan, setiap nama getter pada {@code getterNames} lewat refleksi
	 * pada {@code object}, mengembalikan hasil non-kosong PERTAMA yang ditemukan
	 * (dikonversi ke {@link String} lewat {@link #objectToString(Object)}). Dipakai untuk
	 * menoleransi variasi nama field alamat/kota/provinsi KTP antar model
	 * {@link BiodataMahasiswa}/{@link Mahasiswa} yang berbeda-beda. Getter yang tidak ada atau
	 * gagal dipanggil pada {@code object} dilewati (dicoba getter berikutnya), bukan
	 * melempar exception.
	 *
	 * @param object      objek sasaran refleksi, boleh {@code null} (langsung mengembalikan
	 *                    string kosong)
	 * @param getterNames daftar nama method getter tanpa argumen yang dicoba berurutan
	 * @return nilai string non-kosong pertama yang ditemukan, atau string kosong bila tidak ada
	 *         satu pun getter yang berhasil mengembalikan nilai non-kosong
	 */
	private String ambilStringDariGetter(Object object, String[] getterNames) {
		if (object == null || getterNames == null) {
			return "";
		}
		for (int i = 0; i < getterNames.length; i++) {
			try {
				Method method = object.getClass().getMethod(getterNames[i], new Class[0]);
				Object value = method.invoke(object, new Object[0]);
				String hasil = objectToString(value);
				if (hasil != null && !hasil.trim().isEmpty()) {
					return hasil;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/DataMahasiswa.java:290");
				// Coba getter berikutnya. Dibuat fleksibel karena nama field KTP bisa berbeda di model.
			}
		}
		return "";
	}

	/**
	 * Mengonversi sebuah nilai hasil refleksi menjadi {@link String} yang cocok ditampilkan:
	 * {@code null} menjadi string kosong, {@link String} dikembalikan apa adanya, dan objek
	 * lain dicoba dipanggil method {@code getNama()}-nya (pola umum model referensi AIS,
	 * mis. kota/provinsi/agama) &mdash; jatuh kembali ke {@link Object#toString()} bila
	 * {@code getNama()} tidak ada/gagal dipanggil.
	 *
	 * @param value nilai yang akan dikonversi, boleh {@code null}
	 * @return representasi string dari {@code value}, tidak pernah {@code null}
	 */
	private String objectToString(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof String) {
			return (String) value;
		}
		try {
			Method method = value.getClass().getMethod("getNama", new Class[0]);
			Object nama = method.invoke(value, new Object[0]);
			return nama == null ? "" : nama.toString();
		} catch (Exception e) {
			return value.toString();
		}
	}

	/**
	 * Menggabungkan dua teks (masing-masing di-{@code trim} lebih dulu) dengan satu spasi di
	 * antaranya, melewati bagian yang kosong (mengembalikan hanya bagian yang tidak kosong
	 * tanpa spasi ganda/depan/belakang).
	 *
	 * @param kiri  teks pertama, boleh {@code null}/kosong
	 * @param kanan teks kedua, boleh {@code null}/kosong
	 * @return gabungan kedua teks yang tidak kosong dipisah satu spasi; string kosong bila
	 *         keduanya kosong
	 */
	private String gabungDenganSpasi(String kiri, String kanan) {
		String a = kiri == null ? "" : kiri.trim();
		String b = kanan == null ? "" : kanan.trim();
		if (a.isEmpty()) {
			return b;
		}
		if (b.isEmpty()) {
			return a;
		}
		return a + " " + b;
	}
}
