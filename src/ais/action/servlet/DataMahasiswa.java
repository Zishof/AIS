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
 * Servlet implementation class CheckISBN
 */
public class DataMahasiswa extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final String[] GETTER_ALAMAT_KTP = new String[] { "getAlamatKtp", "getAlamatKTP",
			"getAlamatSesuaiKtp", "getAlamatSesuaiKTP", "getAlamatKartuTandaPenduduk" };
	private static final String[] GETTER_KOTA_KTP = new String[] { "getKotaKtp", "getKotaKTP",
			"getKotaKabupatenKtp", "getKotaKabupatenKTP", "getKabupatenKotaKtp", "getKabupatenKotaKTP",
			"getKabupatenKtp", "getKabupatenKTP" };
	private static final String[] GETTER_PROPINSI_KTP = new String[] { "getPropinsiKtp", "getPropinsiKTP",
			"getProvinsiKtp", "getProvinsiKTP", "getPropinsiSesuaiKtp", "getPropinsiSesuaiKTP",
			"getProvinsiSesuaiKtp", "getProvinsiSesuaiKTP", "getPropinsiKartuTandaPenduduk",
			"getProvinsiKartuTandaPenduduk" };

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public DataMahasiswa() {
		super();

		// TODO Auto-generated constructor stub
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

	/**
	 * Dibutuhkan agar request browser/client yang menggunakan custom header
	 * password/api_data_mahasiswa_password dapat melewati preflight CORS.
	 */
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		setJsonHeader(response);
		response.setStatus(HttpServletResponse.SC_OK);
	}

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

	private void setJsonHeader(HttpServletResponse response) {
		//response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Type", "application/json; charset=UTF-8");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		response.addHeader("Access-Control-Allow-Headers", "password, api_data_mahasiswa_password, X-API-PASSWORD, X-API-KEY, Content-Type");
	}

	private void tulisResponse(HttpServletResponse response, JSONArray hasil) throws IOException {
		String body = hasil.toString();
		response.setHeader("length", body.length() + "");
		PrintWriter writer = response.getWriter();
		writer.write(body);
	}

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
