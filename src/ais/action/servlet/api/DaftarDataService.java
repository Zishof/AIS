package ais.action.servlet.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.PertemuanPunyaDiskusi;
import ais.database.model.Program;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoAdmin;
import ais.database.model.file.FotoBiodataCalonMahasiswa;
import ais.database.model.file.FotoCalonSiswa;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.FotoGuru;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.FotoPegawai;
import ais.database.model.file.FotoSiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.inventory.Pedagang;
import ais.database.model.inventory.Produk;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;
// Pastikan import class model dan utilitas lain sesuai package project Anda

public class DaftarDataService { // Sesuaikan nama class dengan file asli

	public static JSONObject daftar(HttpServletRequest req, JSONObject request) {
		return daftar(req, request, true);
	}

	public static JSONObject daftar(HttpServletRequest req, JSONObject request, boolean pakaiToken) {
		JSONObject jsonObject = new JSONObject();
		try {
			if (!pakaiToken) {
				jsonObject = proses(req, request, null);
			} else {
				Tbmuser tbmuser = ApiUtil.currentUser(request, req);
				if (tbmuser == null || tbmuser.getUserId() == null) {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Token tidak sesuai");
				} else {
					jsonObject = proses(req, request, tbmuser);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/DaftarDataService.java:77");
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (JSONException ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/DaftarDataService.java:83");
			}
		}
		return jsonObject;
	}

	public static JSONObject load(HttpServletRequest req, JSONObject request) {
		return load(req, request, true);
	}

	public static JSONObject load(HttpServletRequest req, JSONObject request, boolean pakaiToken) {
		JSONObject jsonObject = new JSONObject();
		try {
			if (!pakaiToken) {
				jsonObject = load(req, request, null);
			} else {
				Tbmuser tbmuser = ApiUtil.currentUser(request, req);
				if (tbmuser == null || tbmuser.getUserId() == null) {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Token tidak sesuai");
				} else {
					jsonObject = load(req, request, tbmuser);
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (JSONException ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/DaftarDataService.java:113");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("rawtypes")
	private static Criteria createCriteriaProses(JSONObject request, Class clazz, Session session, Tbmuser tbmuser)
			throws Exception {

		Criteria c = session.createCriteria(clazz);

		// 1. Refactoring Alias (Looping 1-10)
		for (int i = 1; i <= 10; i++) {
			String key = "alias" + i;
			if (!request.isNull(key)) {
				String aliasVal = request.getString(key).trim();
				if (!aliasVal.isEmpty()) {
					String[] extensionRemoved = aliasVal.split("\\.");
					c.createAlias(aliasVal, extensionRemoved[extensionRemoved.length - 1], Criteria.LEFT_JOIN);
				}
			}
		}

		// 2. Persiapkan Filter Sekolah (jika ada)

		String filterSekolahReplacement = null;
		String idSekolahGuru = null;

		if (tbmuser != null && tbmuser.getGuru() != null) {
			Set<Long> sekolahs = new HashSet<Long>();
			Guru guru = tbmuser.getGuru();
			if (guru.getSekolah() != null)
				sekolahs.add(guru.getSekolah().getId());
			if (guru.getSekolah1() != null)
				sekolahs.add(guru.getSekolah1().getId());
			if (guru.getSekolah2() != null)
				sekolahs.add(guru.getSekolah2().getId());
			if (guru.getSekolah3() != null)
				sekolahs.add(guru.getSekolah3().getId());

			if (sekolahs.size() > 1) {
				StringBuilder inBuilder = new StringBuilder();
				for (Long s : sekolahs) {
					if (inBuilder.length() > 0)
						inBuilder.append(",");
					inBuilder.append(s);
				}

				if (guru.getSekolah() != null) {
					idSekolahGuru = String.valueOf(guru.getSekolah().getId());
					filterSekolahReplacement = " sekolah_id in (" + inBuilder.toString() + ") ";
				}
			}
		}

		// 3. Refactoring Where Clauses (Looping 1-10)
		for (int i = 1; i <= 10; i++) {
			String key = "where" + i;
			if (!request.isNull(key)) {
				String val = request.getString(key).trim();
				if (!val.isEmpty()) {
					// Terapkan filter sekolah jika perlu
					if (filterSekolahReplacement != null && idSekolahGuru != null) {
						// Pola replace yang sama seperti kode asli
						val = StringUtils.replace(val, "sekolah_id=" + idSekolahGuru, filterSekolahReplacement);
						val = StringUtils.replace(val, "sekolah_id = " + idSekolahGuru, filterSekolahReplacement);
						val = StringUtils.replace(val, "sekolah_id= " + idSekolahGuru, filterSekolahReplacement);
						val = StringUtils.replace(val, "sekolah_id =" + idSekolahGuru, filterSekolahReplacement);
					}
					// Fix 'andthis_' typo logic
					c.add(Restrictions.sqlRestriction(val.replaceAll("andthis_", " and this_")));
				}
			}
		}

		// Khusus KRS Mahasiswa
		if (request.isNull("semesterPendek")
				&& "ais.database.model.KrsMahasiswa".equalsIgnoreCase(request.getString("class"))) {
			c.add(Restrictions.isNull("semesterPendek"));
		}

		return c;
	}

	public static JSONArray sql(JSONObject jsonObject) throws Exception {
		String sql = jsonObject.getString("sql");
		List<Map<String, Object>> list = Common.ambilSqlMap(sql);
		JSONArray jsonArray = new JSONArray();
		for (Map<String, Object> s : list) {
			JSONObject object = new JSONObject();
			for (String key : s.keySet()) {
				Object val = s.get(key);
				formatAndPutValue(object, key, val, null); // Helper method
			}

			jsonArray.put(object);
		}

		return jsonArray;
	}

	@SuppressWarnings({ "rawtypes" })
	public static JSONObject load(HttpServletRequest req, JSONObject request, Tbmuser tbmuser) throws Exception {
		JSONObject jsonObject = new JSONObject();
		String clazzName = request.getString("class").trim();
		
		int deep = 1;
		try {
			deep = request.isNull("deep") ? 1 : Integer.parseInt(request.get("deep").toString().trim());
		}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/DaftarDataService.java:223");
			// TODO: handle exception
		}
		
		Class clazz = Class.forName(clazzName);
		JSONObject data = new JSONObject();
		GeneralValueObject generalValueObject = GeneralValueObject.ambilData(clazz, request.get("id") + "", true);
		Common.insertProperty(clazz, generalValueObject, data, "", deep);
		jsonObject.put("data", data);
		jsonObject.put("status", "00");
		jsonObject.put("description", "Data sukses di load");
		return jsonObject;

	}

	@SuppressWarnings({ "rawtypes" })
	public static JSONObject proses(HttpServletRequest req, JSONObject request, Tbmuser tbmuser) throws Exception {
		JSONObject jsonObject = new JSONObject();

		// Parsing parameter dasar dengan nilai default
		Integer max = request.isNull("max") ? 500 : Integer.parseInt((request.get("max") + "").trim());
		Integer halaman = request.isNull("halaman") ? 0 : Integer.parseInt((request.get("halaman") + "").trim());
		Boolean count = !request.isNull("count") && Boolean.parseBoolean((request.get("count") + "").trim());
		int deep = request.isNull("deep") ? 6 : Integer.parseInt((request.get("deep") + "").trim());

		String clazzName = request.getString("class").trim();
		Class clazz;
		try {
			clazz = Class.forName(clazzName);
		} catch (ClassNotFoundException cnfe) {
			// KE-13: nama class dari klien API tidak dikenal (mis. "ais.database.model.employ.SatuanKerja"
			// yang tak ada — yang benar "ais.database.model.rab.SatuanKerja" atau
			// "ais.database.model.employ.SatuanKerjaEmploy"). Balas JSON error rapi (status 01), JANGAN
			// lempar exception yang membanjiri ErrorAuditFilter dengan HTTP 500. Session belum dibuka -> tak bocor.
			jsonObject.put("status", "01");
			jsonObject.put("description", "Error : class tidak dikenal -> " + clazzName);
			jsonObject.put("data", new JSONArray());
			jsonObject.put("count", 0);
			return jsonObject;
		}

		// Menentukan Session
		boolean isStreaming = clazzName.startsWith("ais.database.model.file.")
				|| clazzName.startsWith("ais.database.model.streaming.");

		Session session = isStreaming ? StreamingHibernateUtil.getInstance().openSession()
				: HibernateUtil.getSessionFactory().openSession();

		Transaction transaction = null;
		try {
			transaction = session.beginTransaction();
			// Hitung Data (Jika diminta)
			Number countData = 0;
			if (count) {
				Criteria cCount = createCriteriaProses(request, clazz, session, tbmuser);
				countData = (Number) cCount.setProjection(Projections.rowCount()).uniqueResult();
			}

			// Ambil Data Utama
			Criteria c = createCriteriaProses(request, clazz, session, tbmuser);
			c.setMaxResults(max).setFirstResult(halaman * max);

			// 1. Refactoring Sorting (Looping 1-10)
			for (int i = 1; i <= 10; i++) {
				String orderKey = "order" + i;
				String sortKey = "sort" + i;

				String order = request.isNull(orderKey) ? (i == 1 ? "asc" : "") : request.getString(orderKey).trim();
				String sort = request.isNull(sortKey) ? "id" : request.getString(sortKey).trim();

				if (!order.isEmpty()) {
					c.addOrder("asc".equalsIgnoreCase(order) ? Order.asc(sort) : Order.desc(sort));
				}
			}

			// Setup Projection
			String projection = request.isNull("projection") ? "" : request.getString("projection").trim();
			boolean adaProjection = !projection.isEmpty();

			if (adaProjection) {
				ProjectionList projectionList = Projections.projectionList();
				boolean terdapatId = false;
				List<String> projFields = new ArrayList<String>();

				// Cek field yang diminta
				for (String s : projection.split(";")) {
					if (!s.trim().isEmpty() && !s.toLowerCase().startsWith("file_")) {

						if (clazzName.equals(Tbmuser.class.getName()) && s.equalsIgnoreCase("id")) {
							s = "userId";
						} else if (clazzName.equals(Tbmrole.class.getName()) && s.equalsIgnoreCase("id")) {
							s = "roleId";
						} else if (clazzName.equals(Program.class.getName()) && s.equalsIgnoreCase("id")) {
							s = "nama";
						}

						projectionList.add(Projections.property(s));
						projFields.add(s);
						if (s.equalsIgnoreCase("id"))
							terdapatId = true;
						if (clazzName.equals(Tbmuser.class.getName()) && s.equalsIgnoreCase("userId"))
							terdapatId = true;
						if (clazzName.equals(Tbmrole.class.getName()) && s.equalsIgnoreCase("roleId"))
							terdapatId = true;
						if (clazzName.equals(Program.class.getName()) && s.equalsIgnoreCase("nama"))
							terdapatId = true;
					}
				}

				// Tambahkan ID wajib jika belum ada
				if (!terdapatId) {
					if (clazzName.equals(Tbmuser.class.getName())) {
						projectionList.add(Projections.property("userId"));
						projection += ";userId";
					} else if (clazzName.equals(Tbmrole.class.getName())) {
						projectionList.add(Projections.property("roleId"));
						projection += ";roleId";
					} else if (clazzName.equals(Program.class.getName())) {
						projectionList.add(Projections.property("nama"));
						projection += ";nama";
					} else {
						projectionList.add(Projections.property("id"));
						projection += ";id";
					}
				}
				c.setProjection(projectionList);
			}

			List resultList = !adaProjection ? ConstantValues.simpleList(c, clazz) : c.list();
			JSONArray jsonArray = new JSONArray();

			if (resultList != null) {
				for (Object o : resultList) {
					JSONObject rowObj = new JSONObject();

					if (o instanceof GeneralValueObject) {
						// Handler untuk Object utuh (bukan proyeksi)
						GeneralValueObject da = (GeneralValueObject) o;
						Common.insertProperty(clazz, da, rowObj, "", deep);

						if (da instanceof FileFotoLain) {
							rowObj.put("url", ((FileFotoLain) da).createLinkUri());
						}
						if (da instanceof PertemuanPunyaDiskusi) {
							LampiranLain lampiranLain = LampiranLain.ambil(da.getId(), LampiranLain.DISKUSI);
							if (lampiranLain != null) {
								rowObj.put("link_url", lampiranLain.createLinkUri());
							}
						}
					} else {
						// Handler untuk Proyeksi (Object[] atau Single Object)
						String[] projections = projection.split(";");
						// Normalisasi ke array object biar seragam pemrosesannya
						Object[] objects = (o instanceof Object[]) ? (Object[]) o : new Object[] { o };

						int index = 0;
						for (String s : projections) {
							if (s.trim().isEmpty() || s.toLowerCase().startsWith("file_"))
								continue;
							if (index >= objects.length)
								break;

							Object val = objects[index];
							formatAndPutValue(rowObj, s, val, request); // Helper method

							// Logika Foto yang disederhanakan
							if (val != null && (s.equalsIgnoreCase("id") || s.equalsIgnoreCase("userId"))) {
								prosesFoto(rowObj, clazzName, s, val.toString());
							}

							if (val != null
									&& (clazzName.equals(Tbmuser.class.getName()) && s.equalsIgnoreCase("userId"))) {
								rowObj.put("id", s);
							} else if (val != null
									&& (clazzName.equals(Tbmrole.class.getName()) && s.equalsIgnoreCase("roleId"))) {
								rowObj.put("id", s);
							} else if (val != null
									&& (clazzName.equals(Program.class.getName()) && s.equalsIgnoreCase("nama"))) {
								rowObj.put("id", s);
							}
							index++;
						}
					}
					jsonArray.put(rowObj);
				}
			}

			jsonObject.put("data", jsonArray);
			if (count) {
				jsonObject.put("count", countData.intValue());
			}
			jsonObject.put("status", "00");
			jsonObject.put("description", resultList != null && resultList.size() > 0 ? "Pengambilan data berhasil"
					: "Pengambilan data berhasil, namun data tidak ditemukan");

			// 2. Commit transaksi jika eksekusi berhasil
			transaction.commit();
		} catch (Exception e) {
			// 3. WAJIB ROLLBACK jika terjadi error
			if (transaction != null && transaction.isActive()) {
				try {
					transaction.rollback();
					System.err.println("Transaction di-rollback untuk mencegah status 'aborted' pada koneksi.");
				} catch (Exception rollbackEx) {
					System.err.println("Gagal melakukan rollback: " + rollbackEx.getMessage());
				}
			}

			// Cetak error di log console
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/DaftarDataService.java:432");

			// 4. MENCARI PESAN ERROR SQL ASLI (ROOT CAUSE)
			String errorMessage = e.getMessage();
			Throwable cause = e.getCause();
			// Looping untuk menggali sampai ke error terdalam (biasanya PSQLException)
			while (cause != null) {
				errorMessage = cause.getMessage();
				cause = cause.getCause();
			}

			// 5. MEMASUKKAN ERROR KE DALAM JSON RESPONSE
			try {
				jsonObject.put("status", "01");
				jsonObject.put("description", "Error : sql -> " + errorMessage);
				
				// Optional: Kosongkan data agar response rapi jika terjadi error
				jsonObject.put("data", new JSONArray()); 
				jsonObject.put("count", 0);
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/servlet/api/DaftarDataService.java:451");
			}

		} finally {
			// Pastikan session selalu tertutup
			try {
				try { session.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/DaftarDataService.java:457");}
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/DaftarDataService.java:459");
				// Ignore close error
			}
			// Pastikan session selalu tertutup
			try {
				session.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/DaftarDataService.java:465");
				// Ignore close error
			}

			// Handle Streaming/Native specific closing logic if needed
			if (isStreaming) {
				StreamingHibernateUtil.getInstance().closeSession();
			}
		}

		return jsonObject;
	}

	// --- Helper Methods ---

	/**
	 * Memformat nilai database menjadi format JSON yang sesuai (Date, Boolean,
	 * Object link).
	 */
	private static void formatAndPutValue(JSONObject json, String key, Object val, JSONObject request)
			throws JSONException {
		String strVal = "";
		String type = "";

		if (val != null) {
			type = val.getClass().getName().split("_")[0];
			if (val instanceof Boolean) {
				strVal = ((Boolean) val) ? "true" : "false";
			} else if (val instanceof Date || val instanceof java.sql.Date) {
				strVal = Common.dateFormatInput.get().format(val);
				json.put(key + "_tgl", Common.databaseDateFormat.get().format(val));
				json.put(key + "_time", Common.timeFormat.get().format(val));
				json.put(key + "_format", Common.dateFormat5.get().format(val));
				json.put(key + "_iso", Common.dateFormatInput.get().format(val));
			} else if (val instanceof GeneralValueObject) {
				GeneralValueObject gvo = (GeneralValueObject) val;
				strVal = gvo.getNama();
				json.put(key + "_id", (gvo.getId() == null) ? "-1" : gvo.getId().toString());
				json.put(key + "_nama", (gvo.getNama() == null) ? "" : gvo.getNama());
			} else if (val instanceof Tbmuser) {
				strVal = val.toString();
				json.put(key + "_id", ((Tbmuser) val).getUserId());
				prosesFoto(json, Tbmuser.class.getName(), key, val.toString());
			} else if (val instanceof Mahasiswa) {
				strVal = val.toString();
				json.put(key + "_id", ((Mahasiswa) val).getId());
				prosesFoto(json, Mahasiswa.class.getName(), key, val.toString());
			} else if (val instanceof Siswa) {
				strVal = val.toString();
				json.put(key + "_id", ((Siswa) val).getId());
				prosesFoto(json, Siswa.class.getName(), key, val.toString());
			} else if (val instanceof Guru) {
				strVal = val.toString();
				json.put(key + "_id", ((Guru) val).getId());
				prosesFoto(json, Guru.class.getName(), key, val.toString());
			} else if (val instanceof Dosen) {
				strVal = val.toString();
				json.put(key + "_id", ((Dosen) val).getId());
				prosesFoto(json, Dosen.class.getName(), key, val.toString());
			} else if (val instanceof Pegawai) {
				strVal = val.toString();
				json.put(key + "_id", ((Pegawai) val).getId());
				prosesFoto(json, Pegawai.class.getName(), key, val.toString());
			} else if (val instanceof Tbmrole) {
				strVal = val.toString();
				json.put(key + "_id", ((Tbmrole) val).getRoleId());
			} else {
				strVal = val.toString();
			}
		}

		json.put(key, strVal);
		json.put(key + "_type", type);

		// Decryption logic
		try {
			if (request != null && !request.isNull("decript") && key.toLowerCase().contains("pass")) {
				json.put(key + "_decript", Common.desEncrypter.get().decrypt(strVal));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/DaftarDataService.java:544");
			// Ignore decryption error
		}
	}

	// Menggunakan Map<String, Class<?>> agar tidak perlu Class.forName() berulang
	// kali
	public static final Map<String, Class<?>> MAP_ENTITY_TO_FOTO = new HashMap<String, Class<?>>();

	static {
		// Registrasi mapping Entity -> Class Foto
		MAP_ENTITY_TO_FOTO.put(Tbmuser.class.getName(), FotoAdmin.class);
		MAP_ENTITY_TO_FOTO.put(Mahasiswa.class.getName(), FotoMahasiswa.class);
		MAP_ENTITY_TO_FOTO.put(Siswa.class.getName(), FotoSiswa.class);
		MAP_ENTITY_TO_FOTO.put(Dosen.class.getName(), FotoDosen.class);
		MAP_ENTITY_TO_FOTO.put(Guru.class.getName(), FotoGuru.class);
		MAP_ENTITY_TO_FOTO.put(Pegawai.class.getName(), FotoPegawai.class);
		MAP_ENTITY_TO_FOTO.put(BiodataCalonMahasiswa.class.getName(), FotoBiodataCalonMahasiswa.class);
		MAP_ENTITY_TO_FOTO.put(CalonSiswa.class.getName(), FotoCalonSiswa.class);
		MAP_ENTITY_TO_FOTO.put(Pedagang.class.getName(), FotoAdmin.class);
		MAP_ENTITY_TO_FOTO.put(Produk.class.getName(), LampiranLain.class);
	}

	/**
	 * Logika sentralisasi pengambilan foto. Menggunakan Map Lookup dan Reflection
	 * untuk menghindari if-else masif.
	 */
	private static void prosesFoto(JSONObject json, String clazzName, String fieldName, String idVal) {
//		System.out.println("clazzName: " + clazzName + ", fieldName: " + fieldName + ", idVal: " + idVal);
		// 1. Validasi Input Dasar
		if (json == null || clazzName == null || fieldName == null || idVal == null) {
			return;
		}

		// 2. Cek apakah Class ini terdaftar memiliki foto
		Class<?> fotoClass = MAP_ENTITY_TO_FOTO.get(clazzName);
//		System.out.println("fotoClass: " + fotoClass);
		if (fotoClass == null) {
			return; // Skip jika class tidak dikenal
		}

		try {
			// 3. Validasi Field Name & Penentuan Tipe ID
			// Khusus Tbmuser field harus "userId" dan ID-nya String.
			// Selain itu field harus "id" dan ID-nya Long.
			boolean isTbmUser = clazzName.equals(Tbmuser.class.getName());

			if (isTbmUser) {
				if (!fieldName.equalsIgnoreCase("userId"))
					return;

				Tbmuser tbmuserDa = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), idVal);
				if (tbmuserDa != null && tbmuserDa.getDosen() != null) {
					isTbmUser = false;
					fotoClass = MAP_ENTITY_TO_FOTO.get(Dosen.class.getName());
					idVal = tbmuserDa.getDosen().getId().toString();
				} else if (tbmuserDa != null && tbmuserDa.getGuru() != null) {
					isTbmUser = false;
					fotoClass = MAP_ENTITY_TO_FOTO.get(Guru.class.getName());
					idVal = tbmuserDa.getGuru().getId().toString();
				} else if (tbmuserDa != null && tbmuserDa.getPegawai() != null) {
					isTbmUser = false;
					fotoClass = MAP_ENTITY_TO_FOTO.get(Pegawai.class.getName());
					idVal = tbmuserDa.getPegawai().getId().toString();
				}
			} else {
				if (!fieldName.equalsIgnoreCase("id"))
					return;
			}

			// 4. Ambil 'DEFAULT_JENIS' secara dinamis menggunakan Reflection
			// Ini mengasumsikan semua Class Foto memiliki public static final String
			// DEFAULT_JENIS
			String jenis = (String) fotoClass.getField("DEFAULT_JENIS").get(null);

			if (clazzName.equalsIgnoreCase(Pedagang.class.getName())) {
				isTbmUser = true;
				Pedagang pedagang = (Pedagang) ConstantValues.ambil(Pedagang.class.getName(), idVal);
				if (pedagang != null && pedagang.getUserid() != null) {
					idVal = pedagang.getUserid();
				}
			} else if (clazzName.equalsIgnoreCase(Produk.class.getName())) {
				isTbmUser = false;
				jenis = Produk.class.getName();
			}

			// 5. Parsing ID
			Serializable idObj = idVal;
			if (!isTbmUser) {
				// Parse ke Long untuk entity selain Tbmuser
				// Trim untuk menghindari error spasi tak terlihat
				idObj = Long.valueOf(idVal.trim());
			}

			// 6. Eksekusi Pengambilan Data
			// Note: Pastikan casting/generic method 'ambil' sesuai dengan library anda
			FileFotoLain fileFotoLain = FileFotoLain.ambil(idObj, jenis, fotoClass);
//			System.out.println("fileFotoLain: " + fileFotoLain + ", jenis -> " + jenis);
			if (fileFotoLain != null) {
				String small = FileFotoLain.ambilLinkLampiranLain(fileFotoLain, false, false, fotoClass, true, false,
						true);
				String big = FileFotoLain.ambilLinkLampiranLain(fileFotoLain, false, false, fotoClass, true, false,
						false);

				json.put("foto_kecil", small);
				json.put("foto_besar", big);
				json.put("foto_clazz", fotoClass.getName());
				json.put("foto_jenis", jenis);
			} else {
				String link = Common.getRequestHostWithProtocol() + "/img/"
						+ FileFotoLain.iconNggakAda(fotoClass.getName());
				json.put("foto_kecil", link);
				json.put("foto_besar", link);
				json.put("foto_clazz", fotoClass.getName());
				json.put("foto_jenis", jenis);
			}

		} catch (NumberFormatException e) {
			// Handle jika idVal bukan angka valid untuk Long
			System.err.println("Error parsing ID Foto: " + idVal);
		} catch (Exception e) {
			// Catch global untuk JSONException, Reflection, dll agar tidak mematikan flow
			// utama
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/DaftarDataService.java:667");
		}
	}
}