package ais.action.servlet.api;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.TampilanPengumumanAkademisAction;
import ais.action.servlet.Api;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BlokirMahasiswa;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.UserDevices;
import ais.database.model.WaProfile;
import ais.database.model.rab.Pejabat;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sisdes.Penduduk;

public class ApiUtil {

	public static Tbmuser currentUser(JSONObject request, HttpServletRequest req) {
		try {
			if (req != null) {
				try {
					Tbmuser tbmuser = Common.getCurrentUser(req);
					if (tbmuser != null && tbmuser.getUserId() != null) {
						return tbmuser;
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ApiUtil.java:58");
				}
			}
			String token = request.isNull("token") ? "------" : request.getString("token");
			Tbmuser tbmuser = currentUser(token);
			try {
				if (req != null && tbmuser != null && tbmuser.getUserId() != null) {
					req.getSession(true).setAttribute("mytbmuser", tbmuser);
					req.getSession(true).setAttribute("usersTemp", tbmuser);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ApiUtil.java:69");
			}
			return tbmuser;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ApiUtil.java:73");
		}
		return null;
	}

	public static Tbmuser currentUser(String token) {
		try {

			GeneralValueObject generalValueObject = (GeneralValueObject) ApiTokenManager.getTokenValue(token);
			if (generalValueObject instanceof Mahasiswa || generalValueObject instanceof Siswa
					|| generalValueObject instanceof Penduduk) {
				return new Tbmuser(generalValueObject);
			} else {
				return (Tbmuser) generalValueObject;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ApiUtil.java:89");

		}
		return null;
	}

	public static PerguruanTinggi ambilPerguruanTinggi(Tbmuser tbmuser, PerguruanTinggi selectedPerguruanTinggi) {
		try {
			if (tbmuser != null && tbmuser.getSiswa() != null && tbmuser.getSiswa().getSekolah() != null
					&& tbmuser.getSiswa().getSekolah().getPerguruanTinggi() != null) {
				selectedPerguruanTinggi = tbmuser.getSiswa().getSekolah().getPerguruanTinggi();
			} else if (tbmuser != null && tbmuser.getMahasiswa() != null && tbmuser.getMahasiswa().getJurusan() != null
					&& tbmuser.getMahasiswa().getJurusan().getFakultas().getPerguruanTinggi() != null) {
				selectedPerguruanTinggi = tbmuser.getMahasiswa().getJurusan().getFakultas().getPerguruanTinggi();
			} else if (tbmuser != null && tbmuser.getDosen() != null && tbmuser.getDosen().getJurusan() != null
					&& tbmuser.getDosen().getJurusan().getFakultas().getPerguruanTinggi() != null) {
				selectedPerguruanTinggi = tbmuser.getDosen().getJurusan().getFakultas().getPerguruanTinggi();
			} else if (tbmuser != null && tbmuser.getGuru() != null && tbmuser.getGuru().getSekolah() != null
					&& tbmuser.getGuru().getSekolah().getPerguruanTinggi() != null) {
				selectedPerguruanTinggi = tbmuser.getGuru().getSekolah().getPerguruanTinggi();
			} else if (tbmuser != null && tbmuser.ambilSekolah() != null
					&& tbmuser.ambilSekolah().getPerguruanTinggi() != null) {
				selectedPerguruanTinggi = tbmuser.ambilSekolah().getPerguruanTinggi();
			} else if (tbmuser != null && tbmuser.ambilJurusan() != null
					&& tbmuser.ambilJurusan().getFakultas().getPerguruanTinggi() != null) {
				selectedPerguruanTinggi = tbmuser.ambilJurusan().getFakultas().getPerguruanTinggi();
			} else if (tbmuser != null && tbmuser.ambilFakultas() != null
					&& tbmuser.ambilFakultas().getPerguruanTinggi() != null) {
				selectedPerguruanTinggi = tbmuser.ambilFakultas().getPerguruanTinggi();
			} else if (tbmuser != null && tbmuser.getPerguruanTinggi() != null) {
				selectedPerguruanTinggi = tbmuser.getPerguruanTinggi();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ApiUtil.java:122");
		}
		return selectedPerguruanTinggi;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject pengumuman(HttpServletRequest req, JSONObject request,
			PerguruanTinggi selectedPerguruanTinggi) {
		JSONObject jsonObject = new JSONObject();
		Session session = null;
		try {
			Tbmuser tbmuser = currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {

				selectedPerguruanTinggi = ApiUtil.ambilPerguruanTinggi(tbmuser, selectedPerguruanTinggi);

				Integer jumlahDataDalamSatuHalaman = request.isNull("jumlahDataDalamSatuHalaman") ? 5
						: Integer.parseInt(request.getString("jumlahDataDalamSatuHalaman").trim());
				Integer halaman = request.isNull("halaman") ? 0 : Integer.parseInt(request.getString("halaman").trim());

				/*
				 * Endpoint API tidak boleh memakai currentNativeSession(), karena pada request
				 * servlet session ThreadLocal bisa sudah ditutup oleh helper lain sebelum
				 * initCriteriaStatic(...) memanggil session.createCriteria(...).
				 */
				session = openApiSession();
				Criteria criteria = TampilanPengumumanAkademisAction.initCriteriaStatic(true, tbmuser,
						selectedPerguruanTinggi, null, session);

				jsonObject.put("status", "00");
				jsonObject.put("description", "Pengambilan data berhasil");

				List<PengumumanAkademis> listPengumumanAkademis = filterInstanceOfPengumumanAkademis(ConstantValues.simpleList(criteria
						.setMaxResults(jumlahDataDalamSatuHalaman).setFirstResult(halaman * jumlahDataDalamSatuHalaman),
						PengumumanAkademis.class));
				JSONArray data = new JSONArray();
				for (PengumumanAkademis pengumumanAkademis : listPengumumanAkademis) {
					JSONObject d = new JSONObject();
					d.put("judul", pengumumanAkademis.getJudul());
					d.put("isi", pengumumanAkademis.getCatatan());
					data.put(d);
				}
				jsonObject.put("data", data);
				listPengumumanAkademis = null;

				criteria = TampilanPengumumanAkademisAction.initCriteriaStatic(false, tbmuser, selectedPerguruanTinggi,
						null, session);
				Number total = (Number) criteria.setProjection(Projections.rowCount()).uniqueResult();
				jsonObject.put("size", total == null ? 0 : total.intValue());
			}

		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ApiUtil.java:182");
			}
		} finally {
			closeApiSessionQuietly(session);
			closeCurrentNativeSessionQuietly();
		}
		return jsonObject;
	}

	/**
	 * PERBAIKAN (ClassCastException Long->PengumumanAkademis): ConstantValues.simpleList(...)
	 * bisa mengembalikan List mentah yang isinya id (Long) alih-alih entity utuh apabila
	 * cache MemoryCacheUtil untuk kelas ini tidak konsisten. Menyaring elemen di sini
	 * (raw iteration, tanpa checkcast generic langsung) mencegah ClassCastException pada
	 * perulangan for-each di pemanggil (mis. method pengumuman(...) di atas).
	 */
	@SuppressWarnings("rawtypes")
	private static List<PengumumanAkademis> filterInstanceOfPengumumanAkademis(List rawList) {
		List<PengumumanAkademis> hasil = new ArrayList<PengumumanAkademis>();
		if (rawList == null) {
			return hasil;
		}
		for (Object o : rawList) {
			if (o instanceof PengumumanAkademis) {
				hasil.add((PengumumanAkademis) o);
			}
		}
		return hasil;
	}

	public static JSONObject konfigurasi(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			String nama = request.isNull("nama") ? "" : request.get("nama") + "";
			String nilai = request.isNull("nilai") ? "" : request.get("nilai") + "";

			Konfigurasi konfigurasi = Common.getKonfigurasi(nama, nilai);
			jsonObject.put("nama", konfigurasi.getNama());
			jsonObject.put("nilai", konfigurasi.getNilai());
			jsonObject.put("status", "00");
			jsonObject.put("description", "Ambil konfigurasi berhasil");
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ApiUtil.java:208");
			}
		}
		return jsonObject;
	}

	public static JSONObject code(JSONObject request, HttpServletRequest req, PerguruanTinggi selectedPerguruanTinggi) {
		JSONObject jsonObject = new JSONObject();
		Session session = null;
		try {

			String username = request.getString("username");
			String link = request.getString("link");
			String nama_pt = request.getString("nama_pt");
			String logo_pt = request.getString("logo_pt");
			String bg_pt = request.getString("bg_pt");
			String login_bg_pt = request.getString("login_bg_pt");
			String motto_pt = request.getString("motto_pt");
			String alamat_pt = request.getString("alamat_pt");
			String telp_pt = request.getString("telp_pt");
			String email_pt = request.getString("email_pt");
			String banner_pt = request.getString("banner_pt");

			session = openApiSession();

			UserDevices userDevices = (UserDevices) session.createCriteria(UserDevices.class)
					.add(Restrictions.eq("username", username)).setMaxResults(1).uniqueResult();
			if (userDevices == null) {
				userDevices = new UserDevices();
				userDevices.setImei(Common.getGeneratedBarCode(18));
			}
			userDevices.setUsername(username);
			userDevices.setPassword(link);

			session.getTransaction().begin();
			session.saveOrUpdate(userDevices);
			session.getTransaction().commit();

			userDevices.put(banner_pt, "banner_pt");
			userDevices.put(nama_pt, "nama_pt");
			userDevices.put(logo_pt, "logo_pt");
			userDevices.put(bg_pt, "bg_pt");
			userDevices.put(login_bg_pt, "login_bg_pt");
			userDevices.put(motto_pt, "motto_pt");
			userDevices.put(alamat_pt, "alamat_pt");
			userDevices.put(telp_pt, "telp_pt");
			userDevices.put(email_pt, "email_pt");

			jsonObject.put("status", "00");
			jsonObject.put("description", "simpan kode berhasil");


			jsonObject.put("code", userDevices.getImei());

		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ApiUtil.java:268");
			}
		} finally {
			closeApiSessionQuietly(session);
			closeCurrentNativeSessionQuietly();
		}
		return jsonObject;
	}

	public static JSONObject waProfile(JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		Session session = null;
		try {

			String kode = request.getString("kode");
			String keterangan = request.getString("keterangan");

			// "kode" adalah kunci unik & NOT NULL pada wa_profile. Bila null/kosong, insert PASTI
			// melanggar not-null constraint ("null value in column \"kode\" ... violates not-null").
			// Kembalikan respons gagal tanpa menyentuh DB (fungsi lama untuk kode valid tetap jalan).
			if (kode == null || kode.trim().isEmpty()) {
				jsonObject.put("status", "90");
				jsonObject.put("description", "Parameter 'kode' wajib diisi.");
				return jsonObject;
			}

			session = openApiSession();

			WaProfile waProfile = (WaProfile) session.createCriteria(WaProfile.class)
					.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
			if (waProfile == null) {
				// Insert baru. Request WhatsApp dapat tiba bersamaan dengan "kode" yang sama
				// sehingga dua proses sama-sama melihat data belum ada lalu sama-sama meng-insert
				// dan melanggar unique constraint "wa_profile_kode_key"
				// (ConstraintViolationException: duplicate key). Tangani secara idempoten:
				// bila insert gagal, ambil ulang record yang sudah dibuat proses lain lalu update.
				try {
					waProfile = new WaProfile();
					waProfile.setKode(kode);
					waProfile.setKeterangan(keterangan);
					session.getTransaction().begin();
					session.save(waProfile);
					session.getTransaction().commit();
				} catch (Exception insertEx) {
					rollbackApiTransactionQuietly(session);
					// Session Hibernate tidak boleh dipakai lagi setelah exception → buka sesi baru.
					closeApiSessionQuietly(session);
					session = openApiSession();
					WaProfile existing = (WaProfile) session.createCriteria(WaProfile.class)
							.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
					if (existing == null) {
						// Bukan kasus duplikat (record tetap tidak ada) → teruskan error aslinya.
						throw insertEx;
					}
					existing.setKode(kode);
					existing.setKeterangan(keterangan);
					session.getTransaction().begin();
					Common.refreshUpdate(session, existing);
					session.getTransaction().commit();
					waProfile = existing;
				}
			} else {
				waProfile.setKode(kode);
				waProfile.setKeterangan(keterangan);
				session.getTransaction().begin();
				Common.refreshUpdate(session, waProfile);
				session.getTransaction().commit();
			}

			jsonObject.put("status", "00");
			jsonObject.put("description", "simpan kode berhasil");
			jsonObject.put("code", waProfile.getKode());

		} catch (Exception e) {
			rollbackApiTransactionQuietly(session);
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ApiUtil.java:348");
			}
		} finally {
			// openApiSession() memakai openSession() → wajib ditutup (clear/disconnect/close)
			// di finally agar tidak bocor meskipun terjadi exception di tengah proses.
			closeApiSessionQuietly(session);
			closeCurrentNativeSessionQuietly();
		}
		return jsonObject;
	}

	@SuppressWarnings("deprecation")
	public static JSONObject ambilCode(JSONObject request, HttpServletRequest req,
			PerguruanTinggi selectedPerguruanTinggi) {
		JSONObject jsonObject = new JSONObject();
		Session session = null;
		try {

			String code = request.getString("code");
			if (code != null) {
				code = code.replace("\u0000", "").trim();
			}

			session = openApiSession();

			UserDevices userDevices = (UserDevices) session.createCriteria(UserDevices.class)
					.add(Restrictions.eq("imei", code)).setMaxResults(1).uniqueResult();
			if (userDevices != null) {

				String nama_pt = userDevices.retreive("nama_pt");

				jsonObject.put("username", userDevices.getUsername());
				jsonObject.put("link", userDevices.getPassword());
				jsonObject.put("nama_pt", nama_pt);
				jsonObject.put("logo_pt", userDevices.retreive("logo_pt"));
				jsonObject.put("login_bg_pt", userDevices.retreive("login_bg_pt"));
				jsonObject.put("banner_pt", userDevices.retreive("banner_pt"));
				jsonObject.put("bg_pt", userDevices.retreive("bg_pt"));
				jsonObject.put("motto_pt", userDevices.retreive("motto_pt"));
				jsonObject.put("alamat_pt", userDevices.retreive("alamat_pt"));
				jsonObject.put("telp_pt", userDevices.retreive("telp_pt"));
				jsonObject.put("email_pt", userDevices.retreive("email_pt"));

				jsonObject.put("status", "00");
				jsonObject.put("description", "ambil kode berhasil");

				String hasil = "";
				try {
					JSONObject loginInfo;
					if (Common.bolehKonfigurasi("ambil_code_local", Konfigurasi.TIDAK_AKTIF)) {
						request.put("username", userDevices.getUsername().split(";")[0]);
						loginInfo = loginInfo(request, req, selectedPerguruanTinggi);
						jsonObject.put("loginInfo", loginInfo);
					} else {
						JSONObject postData = new JSONObject();
						postData.put("username", userDevices.getUsername().split(";")[0]);
						postData.put("action", "loginInfo");

						String link = userDevices.getPassword();
//						System.out.println("Post link: " + link);
						PostMethod post = new PostMethod(link);
						try {
							StringRequestEntity requestEntity = new StringRequestEntity(postData.toString());
							post.setRequestEntity(requestEntity);
							post.setRequestHeader("Content-type", "application/json");
							HttpClient httpclient = new HttpClient();

							int responseStatus = httpclient.executeMethod(post);
							hasil = post.getResponseBodyAsString();
							if (responseStatus < 200 || responseStatus >= 300) {
								try {
									jsonObject.put("loginInfoStatus", responseStatus);
									jsonObject.put("loginInfoWarning", "Server tujuan mengembalikan HTTP " + responseStatus);
								} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:421");
								}
							}
						} catch (Exception e) {
							try {
								jsonObject.put("loginInfoWarning", "Gagal mengambil loginInfo: " + safeExceptionMessage(e));
							} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:427");
							}
						} finally {
							try {
								post.releaseConnection();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:432");
							}
						}

						loginInfo = parseRemoteJsonObject(hasil, jsonObject, "loginInfo", link);
						jsonObject.put("loginInfo", loginInfo);

					}

					if (!loginInfo.isNull("login_bg_pt")) {
						jsonObject.put("login_bg_pt", loginInfo.getString("login_bg_pt"));
					}
					if (!loginInfo.isNull("background_pt")) {
						jsonObject.put("background_pt", loginInfo.getString("background_pt"));
					}
					if (!loginInfo.isNull("logo_pt")) {
						jsonObject.put("logo_pt", loginInfo.getString("logo_pt"));
					}
					if (!loginInfo.isNull("banner_pt")) {
						jsonObject.put("banner_pt", loginInfo.getString("banner_pt"));
					}
					if (!loginInfo.isNull("motto_pt") && !loginInfo.getString("motto_pt").trim().isEmpty()) {
						jsonObject.put("motto_pt", loginInfo.getString("motto_pt"));
						jsonObject.put("pt.motto", loginInfo.getString("motto_pt"));
						jsonObject.put("siswa.sekolah.perguruanTinggi.motto", loginInfo.getString("motto_pt"));

					}
					if ((nama_pt == null || nama_pt.trim().isEmpty()) && !loginInfo.isNull("pt.nama")) {
						jsonObject.put("nama_pt", loginInfo.getString("pt.nama"));
					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ApiUtil.java:464");
				}

			} else {
				jsonObject.put("status", "91");
				jsonObject.put("description", "Ambil kode gagal, kode tidak ditemukan");
			}

		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ApiUtil.java:478");
			}
		} finally {
			closeApiSessionQuietly(session);
			closeCurrentNativeSessionQuietly();
		}
		return jsonObject;
	}

	public static JSONObject loginLangsung(JSONObject request, HttpServletRequest req,
			PerguruanTinggi selectedPerguruanTinggi) {
		JSONObject jsonObject = new JSONObject();
		Session session = null;
		try {

			String username = request.getString("username");
			String password = request.isNull("password") ? null : request.get("password") + "";

			session = openApiSession();

			Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.simpleObject(session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.add(password == null ? Restrictions.sqlRestriction("true")
							: password.trim().isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.eq("pass", Common.desEncrypter.get().encrypt(password)))

					.add(Restrictions.eq("nim", username)).setMaxResults(1), Mahasiswa.class);
			Siswa siswa = null;
			Tbmuser tbmuser = null;
			Penduduk penduduk = null;
			if (mahasiswa == null) {
				siswa = (Siswa) ConstantValues.simpleObject(session.createCriteria(Siswa.class)
						.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
						.add(Restrictions.isNotNull("sekolah")).add(Restrictions.eq("nomorIndukNasional", username))

						.add(password == null ? Restrictions.sqlRestriction("true")
								: password.trim().isEmpty() ? Restrictions.sqlRestriction("false")
										: Restrictions.eq("pass", Common.desEncrypter.get().encrypt(password)))

						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setMaxResults(1), Siswa.class);
			}

			if (siswa == null) {
				siswa = (Siswa) ConstantValues.simpleObject(session.createCriteria(Siswa.class)
						.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
						.add(Restrictions.isNotNull("sekolah")).add(Restrictions.eq("nomorInduk", username))

						.add(password == null ? Restrictions.sqlRestriction("true")
								: password.trim().isEmpty() ? Restrictions.sqlRestriction("false")
										: Restrictions.eq("pass", Common.desEncrypter.get().encrypt(password)))

						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setMaxResults(1), Siswa.class);
			}

			if (mahasiswa == null && siswa == null) {
				tbmuser = (Tbmuser) ConstantValues.simpleObject(session.createCriteria(Tbmuser.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("userId", username))

						.add(password == null ? Restrictions.sqlRestriction("true")
								: password.trim().isEmpty() ? Restrictions.sqlRestriction("false")
										: Restrictions.eq("userPassword", Common.desEncrypter.get().encrypt(password)))

						.setMaxResults(1), Tbmuser.class);
			}

			if (mahasiswa == null && siswa == null && tbmuser == null) {
				penduduk = (Penduduk) ConstantValues.simpleObject(session.createCriteria(Penduduk.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("kode", username))
						.add(password == null ? Restrictions.sqlRestriction("true")
								: password.trim().isEmpty() ? Restrictions.sqlRestriction("false")
										: Restrictions.eq("pass", Common.desEncrypter.get().encrypt(password)))
						.setMaxResults(1), Penduduk.class);
			}


			System.out.println(
					"loginLangsung -> " + mahasiswa + " " + siswa + " " + tbmuser + " " + selectedPerguruanTinggi);
			List<String> warning = new ArrayList<String>();
			if (mahasiswa != null && mahasiswa.getId() != null) {
				Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
				String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

				int currentSmt = Common.getSemester(tahunAngkatanMhs, semesterMulai,
						mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

				JenisKegiatan.apakahBoleh(mahasiswa, currentSmt, warning);

				try {

					for (Object o : ConstantValues.ambilBerdasarClass(BlokirMahasiswa.class).values()) {
						BlokirMahasiswa blokirMahasiswa = (BlokirMahasiswa) o;
						if (blokirMahasiswa.getAktif() && blokirMahasiswa.getLogin()
								&& blokirMahasiswa.getMahasiswa() != null && mahasiswa != null
								&& blokirMahasiswa.getMahasiswa().getId().equals(mahasiswa.getId())) {
							warning.add(blokirMahasiswa.getKeterangan());
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:581");
					// TODO: handle exception
				}
			}

			if (!warning.isEmpty()) {

				String s = "";
				for (String d : warning) {
					s += s.isEmpty() ? d : "\n\n\n" + s;
				}
				jsonObject.put("status", "91");
				jsonObject.put("description", s);
			}

			else if (mahasiswa != null || siswa != null || tbmuser != null || penduduk != null) {
				String link = Common.getRequestHostWithProtocol(req);
				jsonObject.put("username", username);
				jsonObject.put("link", link);
				jsonObject.put("nama_pt", selectedPerguruanTinggi.getNama());
				jsonObject.put("logo_pt", link + "/img/logo.png");
				jsonObject.put("login_bg_pt", link + "/img/bg.png");
				jsonObject.put("banner_pt", link + "/img/bg.png");
				jsonObject.put("bg_pt", link + "/img/bg.png");
				jsonObject.put("motto_pt", selectedPerguruanTinggi.getMotto());
				jsonObject.put("alamat_pt", selectedPerguruanTinggi.getAlamat1());
				jsonObject.put("telp_pt", selectedPerguruanTinggi.getTelepon());
				jsonObject.put("email_pt", selectedPerguruanTinggi.getEmail());

				jsonObject.put("status", "00");
				jsonObject.put("description", "ambil kode berhasil");

				System.out.println("jsonObject -> " + jsonObject);

				JSONObject loginInfo = new JSONObject();
				try {
					String gcpToken = request.isNull("gcpToken") ? null : request.getString("gcpToken");
					String email = request.isNull("email") ? null : request.getString("email");

					loginInfo = proseslogin(gcpToken, email, tbmuser, mahasiswa, siswa, penduduk,
							selectedPerguruanTinggi, req);

					jsonObject.put("username", username);
					jsonObject.put("loginInfo", loginInfo);

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ApiUtil.java:627");
				}

//				System.out.println("loginInfo -> " + loginInfo);

			} else {
				jsonObject.put("status", "91");
				jsonObject.put("description", "Ambil kode gagal, kode tidak ditemukan");
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ApiUtil.java:638");
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ApiUtil.java:644");
			}
		} finally {
			closeApiSessionQuietly(session);
			closeCurrentNativeSessionQuietly();
		}
		return jsonObject;
	}

	public static JSONObject loginInfo(JSONObject request, HttpServletRequest req,
			PerguruanTinggi selectedPerguruanTinggi) {
		JSONObject jsonObject = new JSONObject();
		Session session = null;
		try {

			String username = request.getString("username");
			String gcpToken = request.isNull("gcpToken") ? null : request.getString("gcpToken");
			String email = request.isNull("email") ? null : request.getString("email");

			System.out.println("gcpToken " + gcpToken);

			session = openApiSession();
			Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.simpleObject(session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("nim", username)).setMaxResults(1), Mahasiswa.class);
			Siswa siswa = null;
			Penduduk penduduk = null;
			Tbmuser tbmuser = null;
			if (mahasiswa == null) {
				siswa = (Siswa) ConstantValues.simpleObject(session.createCriteria(Siswa.class)
						.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
						.add(Restrictions.isNotNull("sekolah")).add(Restrictions.eq("nomorIndukNasional", username))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setMaxResults(1), Siswa.class);
			}

			if (mahasiswa == null && siswa == null) {
				tbmuser = (Tbmuser) ConstantValues.simpleObject(session.createCriteria(Tbmuser.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("userId", username)).setMaxResults(1), Tbmuser.class);

				if (tbmuser != null && tbmuser.getMahasiswa() != null) {
					mahasiswa = tbmuser.getMahasiswa();
				}
				if (tbmuser != null && tbmuser.getSiswa() != null) {
					siswa = tbmuser.getSiswa();
				}
			}

			if (mahasiswa == null && siswa == null && tbmuser == null) {
				penduduk = (Penduduk) ConstantValues.simpleObject(session.createCriteria(Penduduk.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("kode", username)).setMaxResults(1), Penduduk.class);
			}

			closeApiSessionQuietly(session);
			session = null;

			jsonObject = proseslogin(gcpToken, email, tbmuser, mahasiswa, siswa, penduduk, selectedPerguruanTinggi,
					req);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ApiUtil.java:706");
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ApiUtil.java:712");
			}
		} finally {
			closeApiSessionQuietly(session);
			closeCurrentNativeSessionQuietly();
		}
		return jsonObject;
	}

	public static JSONObject login(JSONObject request, HttpServletRequest req,
			PerguruanTinggi selectedPerguruanTinggi) {
		JSONObject jsonObject = new JSONObject();
		Session session = null;
		try {

			String username = request.getString("username");
			String password = request.getString("password");
			String gcpToken = request.isNull("gcpToken") ? null : request.getString("gcpToken");
			String email = request.isNull("email") ? null : request.getString("email");

			session = openApiSession();
			String mypassword = Common.desEncrypter.get().encrypt(password);
			Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.simpleObject(session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("nim", username)).add(Restrictions.eq("pass", mypassword)).setMaxResults(1),
					Mahasiswa.class);
			Siswa siswa = null;
			Tbmuser tbmuser = null;
			Penduduk penduduk = null;
			if (mahasiswa == null) {
				siswa = (Siswa) ConstantValues.simpleObject(session.createCriteria(Siswa.class)
						.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
						.add(Restrictions.isNotNull("sekolah")).add(Restrictions.eq("nomorInduk", username))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("pass", mypassword)).setMaxResults(1), Siswa.class);
			}

			if (mahasiswa == null && siswa == null) {
				tbmuser = (Tbmuser) ConstantValues
						.simpleObject(
								session.createCriteria(Tbmuser.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("userId", username))
										.add(Restrictions.eq("userPassword", mypassword)).setMaxResults(1),
								Tbmuser.class);
			}

			if (mahasiswa == null && siswa == null && tbmuser == null) {
				penduduk = (Penduduk) ConstantValues.simpleObject(session.createCriteria(Penduduk.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("kode", username)).add(Restrictions.eq("pass", mypassword))
						.setMaxResults(1), Penduduk.class);
			}

			closeApiSessionQuietly(session);
			session = null;

			jsonObject = proseslogin(gcpToken, email, tbmuser, mahasiswa, siswa, penduduk, selectedPerguruanTinggi,
					req);

		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ApiUtil.java:779");
			}
		} finally {
			closeApiSessionQuietly(session);
			closeCurrentNativeSessionQuietly();
		}
		return jsonObject;
	}

	/**
	 * Menyimpan pembaruan token/email login siswa dengan AMAN. Pembaruan token bersifat opsional;
	 * jika gagal (mis. data lama dengan {@code calonsiswa} ganda yang melanggar unique constraint
	 * {@code siswa_calonsiswa_key}), login TIDAK boleh ikut gagal. Maka: jika commit gagal,
	 * transaksi di-rollback, entity di-evict dari sesi (agar tidak mem-poison flush berikutnya), dan
	 * error hanya dicatat. Memakai sesi yang sedang berjalan; tidak menutup sesi di sini.
	 */
	private static void commitSiswaTokenAman(Session session, Siswa siswa) {
		try {
			session.getTransaction().begin();
			Common.refreshUpdate(session, siswa);
			session.getTransaction().commit();
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:805");
			}
			try {
				session.evict(siswa);
			} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:809");
			}
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static JSONObject proseslogin(String gcpToken, String email, Tbmuser tbmuser, Mahasiswa mahasiswa,
			Siswa siswa, Penduduk penduduk, PerguruanTinggi selectedPerguruanTinggi, HttpServletRequest req) {
		JSONObject jsonObject = new JSONObject();
		Session session = null;
		try {

			System.out.println("proseslogin");
			selectedPerguruanTinggi = ApiUtil.ambilPerguruanTinggi(tbmuser, selectedPerguruanTinggi);
			System.out.println("selectedPerguruanTinggi " + selectedPerguruanTinggi);

			session = openApiSession();
			String token = "";

			if (penduduk != null) {

				if (token != null && !token.trim().isEmpty()) {
					penduduk.setToken(token);
					if (gcpToken != null && !gcpToken.trim().isEmpty()) {
						penduduk.setGcpToken(gcpToken);
					}
					penduduk.appendEmail(email);
					session.getTransaction().begin();
					Common.refreshUpdate(session, penduduk);
					session.getTransaction().commit();
				} else if (penduduk.getToken() == null || penduduk.getToken().trim().isEmpty()) {
					token = Common.getGeneratedBarCode(30);
					penduduk.setToken(token);
					if (gcpToken != null && !gcpToken.trim().isEmpty()) {
						penduduk.setGcpToken(gcpToken);
					}
					penduduk.appendEmail(email);
					session.getTransaction().begin();
					Common.refreshUpdate(session, penduduk);
					session.getTransaction().commit();
				} else {
					token = penduduk.getToken();
				}

				jsonObject.put("wa_operator", penduduk.getNoTelp());

			} else if (siswa != null) {

				if (token != null && !token.trim().isEmpty()) {
					siswa.setToken(token);
					if (gcpToken != null && !gcpToken.trim().isEmpty()) {
						siswa.setGcpToken(gcpToken);
					}
					siswa.appendEmail(email);
					commitSiswaTokenAman(session, siswa);
				} else if (siswa.getToken() == null || siswa.getToken().trim().isEmpty()) {
					token = Common.getGeneratedBarCode(30);
					siswa.setToken(token);
					if (gcpToken != null && !gcpToken.trim().isEmpty()) {
						siswa.setGcpToken(gcpToken);
					}
					siswa.appendEmail(email);
					commitSiswaTokenAman(session, siswa);
				} else {
					token = siswa.getToken();
				}

				if (siswa.getSekolah() != null) {
					jsonObject.put("wa_operator", siswa.getSekolah().getWa());
				}

				if (siswa != null && siswa.getSekolah() != null && siswa.getSekolah().getPerguruanTinggi() != null) {
					selectedPerguruanTinggi = siswa.getSekolah().getPerguruanTinggi();
				}

			} else if (mahasiswa != null) {

				if (token != null && !token.trim().isEmpty()) {
					mahasiswa.setToken(token);
					if (gcpToken != null && !gcpToken.trim().isEmpty()) {
						mahasiswa.setGcpToken(gcpToken);
					}
					mahasiswa.appendEmail(email);
					session.getTransaction().begin();
					Common.refreshUpdate(session, mahasiswa);
					session.getTransaction().commit();
				} else if (mahasiswa.getToken() == null || mahasiswa.getToken().trim().isEmpty()) {
					token = Common.getGeneratedBarCode(30);
					mahasiswa.setToken(token);
					if (gcpToken != null && !gcpToken.trim().isEmpty()) {
						mahasiswa.setGcpToken(gcpToken);
					}
					mahasiswa.appendEmail(email);
					session.getTransaction().begin();
					Common.refreshUpdate(session, mahasiswa);
					session.getTransaction().commit();
				} else {
					token = mahasiswa.getToken();
				}

				if (mahasiswa.getJurusan() != null && !mahasiswa.getJurusan().getWa().isEmpty()) {
					jsonObject.put("wa_operator", mahasiswa.getJurusan().getWa());
				} else if (mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getFakultas() != null
						&& !mahasiswa.getJurusan().getFakultas().getWa().isEmpty()) {
					jsonObject.put("wa_operator", mahasiswa.getJurusan().getWa());
				} else if (mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getFakultas() != null
						&& mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null) {
					jsonObject.put("wa_operator", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getWa());
				}

				if (mahasiswa != null && mahasiswa.getJurusan() != null
						&& mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null) {
					selectedPerguruanTinggi = mahasiswa.getJurusan().getFakultas().getPerguruanTinggi();
				}

			} else if (tbmuser != null) {

				if (token != null && !token.trim().isEmpty()) {
					tbmuser.setToken(token);
					if (gcpToken != null && !gcpToken.trim().isEmpty()) {
						tbmuser.setGcpToken(gcpToken);
					}
					tbmuser.appendEmail(email);
					session.getTransaction().begin();
					Common.refreshUpdate(session, tbmuser);
					session.getTransaction().commit();
				} else if (tbmuser.getToken() == null || tbmuser.getToken().trim().isEmpty()) {
					token = Common.getGeneratedBarCode(30);
					tbmuser.setToken(token);
					if (gcpToken != null && !gcpToken.trim().isEmpty()) {
						tbmuser.setGcpToken(gcpToken);
					}
					tbmuser.appendEmail(email);

					System.out.println("1. update " + tbmuser);

					session.getTransaction().begin();
					Common.refreshUpdate(session, tbmuser);
					session.getTransaction().commit();

					System.out.println("2. update " + tbmuser);
				} else {
					token = tbmuser.getToken();
				}

				if (tbmuser.getDosen() != null && tbmuser.getDosen().getJurusan() != null) {
					if (tbmuser.getDosen().getJurusan() != null && !tbmuser.getDosen().getJurusan().getWa().isEmpty()) {
						jsonObject.put("wa_operator", tbmuser.getDosen().getJurusan().getWa());
					} else if (tbmuser.getDosen().getJurusan() != null
							&& tbmuser.getDosen().getJurusan().getFakultas() != null
							&& !tbmuser.getDosen().getJurusan().getFakultas().getWa().isEmpty()) {
						jsonObject.put("wa_operator", tbmuser.getDosen().getJurusan().getWa());
					} else if (tbmuser.getDosen().getJurusan() != null
							&& tbmuser.getDosen().getJurusan().getFakultas() != null
							&& tbmuser.getDosen().getJurusan().getFakultas().getPerguruanTinggi() != null) {
						jsonObject.put("wa_operator",
								tbmuser.getDosen().getJurusan().getFakultas().getPerguruanTinggi().getWa());
					}
				}

				else if (tbmuser.getGuru() != null && tbmuser.getGuru().getSekolah() != null) {
					jsonObject.put("wa_operator", tbmuser.getGuru().getSekolah().getWa());
				}

				else if (tbmuser != null && tbmuser.ambilJurusan() != null) {
					if (tbmuser.ambilJurusan() != null && !tbmuser.ambilJurusan().getWa().isEmpty()) {
						jsonObject.put("wa_operator", tbmuser.ambilJurusan().getWa());
					} else if (tbmuser.ambilJurusan() != null && tbmuser.ambilJurusan().getFakultas() != null
							&& !tbmuser.ambilJurusan().getFakultas().getWa().isEmpty()) {
						jsonObject.put("wa_operator", tbmuser.ambilJurusan().getWa());
					} else if (tbmuser.ambilJurusan() != null && tbmuser.ambilJurusan().getFakultas() != null
							&& tbmuser.ambilJurusan().getFakultas().getPerguruanTinggi() != null) {
						jsonObject.put("wa_operator",
								tbmuser.ambilJurusan().getFakultas().getPerguruanTinggi().getWa());
					}
				}

				else if (tbmuser != null && tbmuser.ambilFakultas() != null) {
					if (tbmuser.ambilFakultas() != null && !tbmuser.ambilFakultas().getWa().isEmpty()) {
						jsonObject.put("wa_operator", tbmuser.ambilFakultas().getWa());
					} else if (tbmuser.ambilFakultas() != null && tbmuser.ambilFakultas() != null
							&& tbmuser.ambilFakultas().getPerguruanTinggi() != null) {
						jsonObject.put("wa_operator", tbmuser.ambilFakultas().getPerguruanTinggi().getWa());
					}
				} else if (selectedPerguruanTinggi != null) {
					jsonObject.put("wa_operator", selectedPerguruanTinggi.getWa());
				}

			}

			List<String> warning = new ArrayList<String>();
			if (mahasiswa != null && mahasiswa.getId() != null) {
				Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
				String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

				int currentSmt = Common.getSemester(tahunAngkatanMhs, semesterMulai,
						mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

				JenisKegiatan.apakahBoleh(mahasiswa, currentSmt, warning);

			}

			try {
				for (Object o : ConstantValues.ambilBerdasarClass(BlokirMahasiswa.class).values()) {
					BlokirMahasiswa blokirMahasiswa = (BlokirMahasiswa) o;
					if (blokirMahasiswa.getAktif() && blokirMahasiswa.getLogin()
							&& blokirMahasiswa.getMahasiswa() != null && mahasiswa != null
							&& blokirMahasiswa.getMahasiswa().getId().equals(mahasiswa.getId())) {
						warning.add(blokirMahasiswa.getKeterangan());
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:1021");
				// TODO: handle exception
			}

			if (!warning.isEmpty()) {
				String s = "";
				for (String d : warning) {
					s += s.isEmpty() ? d : "\n\n\n" + s;
				}
				jsonObject.put("status", "98");
				jsonObject.put("description", s);
			} else if (tbmuser == null && mahasiswa == null && siswa == null && penduduk == null) {
				jsonObject.put("status", "98");
				jsonObject.put("description", "Pengguna tidak ditemukan");
			} else {
				jsonObject.put("gcpToken", gcpToken);
				jsonObject.put("token", token);
				jsonObject.put("status", "00");
				jsonObject.put("description", "Login berhasil");
				Common.insertProperty(PerguruanTinggi.class, selectedPerguruanTinggi, jsonObject, "pt", 1);
				String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
						.getPerguruanTinggiMedia(req, "background_perguruanTinggi_", selectedPerguruanTinggi);
				String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
						.getPerguruanTinggiMedia(req, "logo_perguruanTinggi_", selectedPerguruanTinggi);
				String banner_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
						.getPerguruanTinggiMedia(req, "banner_perguruanTinggi_", selectedPerguruanTinggi);
				String background_login_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
						.getPerguruanTinggiMedia(req, "background_login_perguruanTinggi_", selectedPerguruanTinggi);

				jsonObject.put("login_bg_pt", background_login_PerguruanTinggi);
				jsonObject.put("background_pt", background_PerguruanTinggi);
				jsonObject.put("logo_pt", logo_PerguruanTinggi);
				jsonObject.put("banner_pt", banner_PerguruanTinggi);
				jsonObject.put("motto_pt", selectedPerguruanTinggi == null ? "" : selectedPerguruanTinggi.getMotto());

				jsonObject.put("pt.motto", selectedPerguruanTinggi == null ? "" : selectedPerguruanTinggi.getMotto());
				jsonObject.put("siswa.sekolah.perguruanTinggi.motto",
						selectedPerguruanTinggi == null ? "" : selectedPerguruanTinggi.getMotto());

				if (mahasiswa != null) {

					if (mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getFakultas() != null
							&& mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null) {

						background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
								.getPerguruanTinggiMedia(req, "background_perguruanTinggi_",
										mahasiswa.getJurusan().getFakultas().getPerguruanTinggi());
						logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
								.getPerguruanTinggiMedia(req, "logo_perguruanTinggi_",
										mahasiswa.getJurusan().getFakultas().getPerguruanTinggi());
						banner_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
								.getPerguruanTinggiMedia(req, "banner_perguruanTinggi_",
										mahasiswa.getJurusan().getFakultas().getPerguruanTinggi());
						background_login_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
								.getPerguruanTinggiMedia(req, "background_login_perguruanTinggi_",
										mahasiswa.getJurusan().getFakultas().getPerguruanTinggi());

						jsonObject.put("login_bg_pt", background_login_PerguruanTinggi);
						jsonObject.put("background_pt", background_PerguruanTinggi);
						jsonObject.put("logo_pt", logo_PerguruanTinggi);
						jsonObject.put("banner_pt", banner_PerguruanTinggi);

						jsonObject.put("motto_pt",
								mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getMotto());
						jsonObject.put("pt.motto",
								mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getMotto());
						jsonObject.put("siswa.sekolah.perguruanTinggi.motto",
								mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getMotto());
					}

					if ((gcpToken != null && (mahasiswa.getGcpToken() == null || (mahasiswa.getGcpToken() != null
							&& !StringUtils.contains(mahasiswa.getGcpToken(), gcpToken)))) ||

							((email != null && !email.trim().isEmpty()) && (mahasiswa.getEmail() == null
									|| !StringUtils.contains(mahasiswa.getEmail(), email)))

					) {
						// FIX ConstraintViolationException tak tertangkap membatalkan seluruh respons
						// login: update email/gcpToken ini bisa gagal karena data TIDAK TERKAIT (mis.
						// constraint unik "nimkey" bentrok dari baris lain) -- kegagalan simpan token
						// perangkat/email ini BUKAN alasan valid untuk membatalkan login yang sudah
						// berhasil (status "00" sudah disiapkan di atas). Jangan biarkan exception ini
						// bocor & menggagalkan seluruh response JSON ke aplikasi mobile.
						try {
							mahasiswa.appendEmail(email);
							if (gcpToken != null && !gcpToken.trim().isEmpty()) {
								mahasiswa.setGcpToken(gcpToken);
							}
							session.getTransaction().begin();
							Common.refreshUpdate(session, mahasiswa);
							session.getTransaction().commit();
						} catch (Exception exToken) {
							if (session.getTransaction() != null && session.getTransaction().isActive()) {
								try { session.getTransaction().rollback(); } catch (Exception exRollback) { ais.common.ErrorAuditUtil.record(exRollback, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:proseslogin-rollback"); }
							}
							ais.common.ErrorAuditUtil.record(exToken,
									"auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:proseslogin - gagal simpan email/gcpToken, login tetap dilanjutkan");
						}
					}

					String code = Common.getRequestHostWithProtocol(req) + "/m?q="
							+ URLEncoder.encode(
									Common.desEncrypter.get()
											.encrypt(mahasiswa.getId() + "-Mahasiswa-abcdefghijklmnopqrstuvwxyzMobile"),
									"UTF-8");
					jsonObject.put("direct_login", code);

					Api.putToken(token, mahasiswa);

					String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa));
					jsonObject.put("foto", url);
					Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
					String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

					int currentSmt = Common.getSemester(tahunAngkatanMhs, semesterMulai,
							mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

					Common.insertProperty(Mahasiswa.class, mahasiswa, jsonObject, "mahasiswa", 5);
					KrsMahasiswa krsM;
					Common.insertProperty(KrsMahasiswa.class,
							krsM = Common.singkronkanKrsMahasiswa(mahasiswa, currentSmt, null, null), jsonObject, "krs",
							3);
					Common.insertProperty(HistoryStatusMahasiswa.class, ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsM), jsonObject,
							"status", 3);

				} else if (siswa != null) {

					if (siswa.getSekolah() != null) {

						background_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil.getSekolahMedia(req,
								"background_sekolah_", siswa.getSekolah());
						logo_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil.getSekolahMedia(req,
								"logo_sekolah_", siswa.getSekolah());
						banner_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil.getSekolahMedia(req,
								"banner_sekolah_", siswa.getSekolah());
						background_login_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil
								.getSekolahMedia(req, "background_login_sekolah_", siswa.getSekolah());

						jsonObject.put("login_bg_pt", background_login_PerguruanTinggi);
						jsonObject.put("background_pt", background_PerguruanTinggi);
						jsonObject.put("logo_pt", logo_PerguruanTinggi);
						jsonObject.put("banner_pt", banner_PerguruanTinggi);

						jsonObject.put("motto_pt", siswa.getSekolah().getMotto());
						jsonObject.put("pt.motto", siswa.getSekolah().getMotto());
						jsonObject.put("siswa.sekolah.perguruanTinggi.motto", siswa.getSekolah().getMotto());
					}

					if ((gcpToken != null && (siswa.getGcpToken() == null
							|| (siswa.getGcpToken() != null && !StringUtils.contains(siswa.getGcpToken(), gcpToken))))

							||

							((email != null && !email.trim().isEmpty()) && (siswa.getAlamatEmail() == null
									|| !StringUtils.contains(siswa.getAlamatEmail(), email)))

					) {
						siswa.appendEmail(email);
						if (gcpToken != null && !gcpToken.trim().isEmpty()) {
							siswa.setGcpToken(gcpToken);
						}
						commitSiswaTokenAman(session, siswa);
					}

					String code = Common.getRequestHostWithProtocol(req) + "/m?q=" + URLEncoder.encode(
							Common.desEncrypter.get().encrypt(siswa.getId() + "-Minisiswa-abcdefghijklmnopqrstuvwxyz-Mobile"),
							"UTF-8");
					jsonObject.put("direct_login", code);

					Api.putToken(token, siswa);

					String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa));
					jsonObject.put("foto", url);
					Common.insertProperty(Siswa.class, siswa, jsonObject, "siswa", 5);

					KelasSiswa kelasSiswa = siswa.getKelas();
					if (kelasSiswa != null) {
						Common.insertProperty(KelasSiswa.class, kelasSiswa, jsonObject, "kls", 1);
					}

				}

				else if (penduduk != null) {

					if ((gcpToken != null && (penduduk.getGcpToken() == null || (penduduk.getGcpToken() != null
							&& !StringUtils.contains(penduduk.getGcpToken(), gcpToken))))

							||

							((email != null && !email.trim().isEmpty()) && (penduduk.getAlamatEmail() == null
									|| !StringUtils.contains(penduduk.getAlamatEmail(), email)))

					) {
						penduduk.appendEmail(email);
						if (gcpToken != null && !gcpToken.trim().isEmpty()) {
							penduduk.setGcpToken(gcpToken);
						}
						session.getTransaction().begin();
						Common.refreshUpdate(session, penduduk);
						session.getTransaction().commit();
					}

					String code = Common.getRequestHostWithProtocol(req) + "/m?q="
							+ URLEncoder.encode(Common.desEncrypter.get().encrypt(
									penduduk.getId() + "-Penduduk-abcdefghijklmnopqrstuvwxyz-Mobile"), "UTF-8");
					jsonObject.put("direct_login", code);

					Api.putToken(token, penduduk);

					String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(penduduk));
					jsonObject.put("foto", url);
					Common.insertProperty(Penduduk.class, penduduk, jsonObject, "penduduk", 5);

				}

				else if (tbmuser != null) {

					if (tbmuser.getSekolah() != null) {
						background_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil.getSekolahMedia(req,
								"background_sekolah_", tbmuser.getSekolah());
						logo_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil.getSekolahMedia(req,
								"logo_sekolah_", tbmuser.getSekolah());
						banner_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil.getSekolahMedia(req,
								"banner_sekolah_", tbmuser.getSekolah());
						background_login_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil
								.getSekolahMedia(req, "background_login_sekolah_", tbmuser.getSekolah());

						jsonObject.put("login_bg_pt", background_login_PerguruanTinggi);
						jsonObject.put("background_pt", background_PerguruanTinggi);
						jsonObject.put("logo_pt", logo_PerguruanTinggi);
						jsonObject.put("banner_pt", banner_PerguruanTinggi);
						jsonObject.put("motto_pt", tbmuser.getSekolah().getMotto());
						jsonObject.put("pt.motto", tbmuser.getSekolah().getMotto());
						jsonObject.put("siswa.sekolah.perguruanTinggi.motto", tbmuser.getSekolah().getMotto());
					} else if (tbmuser.getPerguruanTinggi() != null) {

						background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
								.getPerguruanTinggiMedia(req, "background_perguruanTinggi_",
										tbmuser.getPerguruanTinggi());
						logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
								.getPerguruanTinggiMedia(req, "logo_perguruanTinggi_", tbmuser.getPerguruanTinggi());
						banner_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
								.getPerguruanTinggiMedia(req, "banner_perguruanTinggi_", tbmuser.getPerguruanTinggi());
						background_login_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
								.getPerguruanTinggiMedia(req, "background_login_perguruanTinggi_",
										tbmuser.getPerguruanTinggi());

						jsonObject.put("login_bg_pt", background_login_PerguruanTinggi);
						jsonObject.put("background_pt", background_PerguruanTinggi);
						jsonObject.put("logo_pt", logo_PerguruanTinggi);
						jsonObject.put("banner_pt", banner_PerguruanTinggi);
						jsonObject.put("motto_pt", tbmuser.getPerguruanTinggi().getMotto());
						jsonObject.put("pt.motto", tbmuser.getPerguruanTinggi().getMotto());
						jsonObject.put("siswa.sekolah.perguruanTinggi.motto", tbmuser.getPerguruanTinggi().getMotto());
					} else if (tbmuser.getYayasan() != null) {

						background_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil.getYayasanMedia(req,
								"background_yayasan_", tbmuser.getYayasan());
						logo_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil.getYayasanMedia(req,
								"logo_yayasan_", tbmuser.getYayasan());
						banner_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil.getYayasanMedia(req,
								"banner_yayasan_", tbmuser.getYayasan());
						background_login_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil
								.getYayasanMedia(req, "background_login_yayasan_", tbmuser.getYayasan());

						jsonObject.put("login_bg_pt", background_login_PerguruanTinggi);
						jsonObject.put("background_pt", background_PerguruanTinggi);
						jsonObject.put("logo_pt", logo_PerguruanTinggi);
						jsonObject.put("banner_pt", banner_PerguruanTinggi);
						jsonObject.put("motto_pt", tbmuser.getYayasan().getMotto());
						jsonObject.put("pt.motto", tbmuser.getYayasan().getMotto());
						jsonObject.put("siswa.sekolah.perguruanTinggi.motto", tbmuser.getYayasan().getMotto());
					}

					if ((gcpToken != null && (tbmuser.getGcpToken() == null || (tbmuser.getGcpToken() != null
							&& !StringUtils.contains(tbmuser.getGcpToken(), gcpToken))))

							||

							((email != null && !email.trim().isEmpty())
									&& (tbmuser.getEmail() == null || !StringUtils.contains(tbmuser.getEmail(), email)))

					) {
						tbmuser.appendEmail(email);
						if (gcpToken != null && !gcpToken.trim().isEmpty()) {
							tbmuser.setGcpToken(gcpToken);
						}

						System.out.println("1. update 1 " + tbmuser);

						session.getTransaction().begin();
						Common.refreshUpdate(session, tbmuser);
						session.getTransaction().commit();

						System.out.println("2. update 2 " + tbmuser);
					}

					String code = Common.getRequestHostWithProtocol(req) + "/m?q="
							+ URLEncoder.encode(Common.desEncrypter.get()
									.encrypt(tbmuser.getUserId() + "-user-abcdefghijklmnopqrstuvwxyz-Mobile"), "UTF-8");
					jsonObject.put("direct_login", code);

					System.out.println("1. code 1 " + code);

					Api.putToken(token, tbmuser);

					String url = CommonMedia.getUrlFotoPengguna(tbmuser);
					jsonObject.put("foto", url);

					System.out.println("1. foto 1 " + url);

					Common.insertProperty(Tbmuser.class, tbmuser, jsonObject, "user", 5);

					System.out.println("1. foto 2 " + url);

					if (tbmuser.getOrangTua() != null) {

						JSONArray anak = new JSONArray();
						jsonObject.put("anak", anak);

						JSONObject o = new JSONObject(tbmuser.getOrangTua().getAnak());
						Iterator<String> keys = o.keys();
						while (keys.hasNext()) {
							String key = keys.next();
							if (key.startsWith("siswa")) {
								siswa = (Siswa) ConstantValues.ambil(Siswa.class.getName(),
										ais.common.CommonJSONUtil.ambilLong(o, key));
								if (siswa != null) {
									JSONObject object = new JSONObject();

									String tokenSiswa = Common.getGeneratedBarCode(30);
									siswa.setToken(tokenSiswa);
									Common.insertProperty(Siswa.class, siswa, object, "siswa", 3);
									Api.putToken(siswa.getToken(), siswa);
									object.put("token", siswa.getToken());
									anak.put(object);
								}
							} else if (key.startsWith("mahasiswa")) {
								mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(),
										ais.common.CommonJSONUtil.ambilLong(o, key));
								if (mahasiswa != null) {
									JSONObject object = new JSONObject();
									String tokenSiswa = Common.getGeneratedBarCode(30);
									mahasiswa.setToken(tokenSiswa);

									Common.insertProperty(Mahasiswa.class, mahasiswa, object, "mahasiswa", 3);

									Api.putToken(mahasiswa.getToken(), siswa);
									object.put("token", mahasiswa.getToken());

									anak.put(object);
								}
							}
						}
					}

				}

				try {
					if (tbmuser != null && (tbmuser.getGuru() != null || tbmuser.getPegawai() != null
							|| tbmuser.getDosen() != null)) {

						System.out.println("1. pejabat 1 ");
						Pejabat pejabat = (Pejabat) ConstantValues.simpleObject(session.createCriteria(Pejabat.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

								.add(Restrictions.or(
										Restrictions.or(Restrictions.ilike("jenisPengguna",
												"," + tbmuser.hakAkses().getRoleId() + ",", MatchMode.ANYWHERE),
												Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
														MatchMode.ANYWHERE)),
										Restrictions.and(
												Restrictions.or(Restrictions.isNotNull("pegawai"),
														Restrictions.or(Restrictions.isNotNull("guru"),
																Restrictions.isNotNull("dosen"))),
												Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
														Restrictions.or(Restrictions.eq("dosen", tbmuser.getDosen()),
																Restrictions.eq("guru", tbmuser.getGuru())))))

								), Pejabat.class);
						System.out.println("1. pejabat 2 " + pejabat);
						if (pejabat != null) {
							jsonObject.put("pejabat.id", pejabat.getId());
							jsonObject.put("pejabat.nama", pejabat.getNama());
							jsonObject.put("pejabat.jenis",
									pejabat.getJenisJabatan() == null ? "" : pejabat.getJenisJabatan().getNama());
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ApiUtil.java:1396");
				}
			}

			if (tbmuser != null && tbmuser.getPegawai() != null) {
				Common.insertProperty(Pegawai.class, tbmuser.getPegawai(), jsonObject, "user.pegawai", 2);
			}


		} catch (Exception e) {
			rollbackApiTransactionQuietly(session);
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ApiUtil.java:1407");
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ApiUtil.java:1413");
			}
		} finally {
			closeApiSessionQuietly(session);
			closeCurrentNativeSessionQuietly();
		}

		try {
			System.out.println("logo_pt -> " + (jsonObject.isNull("logo_pt") ? "" : jsonObject.get("logo_pt") + ""));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ApiUtil.java:1424");
		}
		return jsonObject;
	}
	private static JSONObject parseRemoteJsonObject(String payload, JSONObject parent, String label, String source) {
		JSONObject result = new JSONObject();
		String text = safeTrim(payload);
		try {
			if (text.length() == 0) {
				result.put("status", "91");
				result.put("description", "Response " + safeTrim(label) + " kosong");
				putRemoteWarning(parent, label, "Response kosong dari " + safeTrim(source));
				return result;
			}

			if (text.startsWith("{")) {
				return new JSONObject(text);
			}

			if (text.startsWith("[")) {
				result.put("status", "91");
				result.put("description", "Response " + safeTrim(label) + " berupa array, bukan object");
				result.put("data", new JSONArray(text));
				putRemoteWarning(parent, label, "Response berupa array dari " + safeTrim(source));
				return result;
			}

			result.put("status", "91");
			result.put("description", "Response " + safeTrim(label) + " bukan JSON object");
			result.put("raw", abbreviate(text, 500));
			putRemoteWarning(parent, label, "Response bukan JSON object dari " + safeTrim(source));
			return result;
		} catch (Exception e) {
			try {
				result.put("status", "91");
				result.put("description", "Response " + safeTrim(label) + " tidak dapat dibaca sebagai JSON");
				result.put("raw", abbreviate(text, 500));
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:1461");
			}
			putRemoteWarning(parent, label, safeExceptionMessage(e));
		}
		return result;
	}

	private static void putRemoteWarning(JSONObject parent, String label, String message) {
		try {
			if (parent != null) {
				parent.put((safeTrim(label).length() == 0 ? "remote" : safeTrim(label)) + "Warning",
						abbreviate(safeTrim(message), 300));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:1474");
		}
	}

	private static String safeTrim(String value) {
		return value == null ? "" : value.trim();
	}

	private static String safeExceptionMessage(Exception e) {
		if (e == null) {
			return "";
		}
		String message = e.getMessage();
		if (message == null || message.trim().length() == 0) {
			message = e.getClass().getName();
		}
		return abbreviate(message, 300);
	}

	private static String abbreviate(String value, int max) {
		if (value == null) {
			return "";
		}
		if (max < 0) {
			max = 0;
		}
		if (value.length() <= max) {
			return value;
		}
		return value.substring(0, max) + "...";
	}

	private static Session openApiSession() {
		Session session = HibernateUtil.getSessionFactory().openSession();
		if (!isApiSessionUsable(session)) {
			closeApiSessionQuietly(session);
			throw new org.hibernate.SessionException("Session API tidak dapat dibuka atau sudah tertutup.");
		}
		return session;
	}

	private static boolean isApiSessionUsable(Session session) {
		try {
			return session != null && session.isOpen();
		} catch (Exception e) {
			return false;
		}
	}

	private static void rollbackApiTransactionQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.getTransaction() != null && !session.getTransaction().wasCommitted()
					&& !session.getTransaction().wasRolledBack()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:1532");
		}
	}

	private static void closeApiSessionQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				try {
					session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:1544");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:1548");
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:1552");
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:1555");
		}
	}

	private static void closeCurrentNativeSessionQuietly() {
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiUtil.java:1562");
		}
	}

}
