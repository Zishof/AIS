package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.AngketUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BlokirMahasiswa;
import ais.database.model.ChecklistBaruPenilaianDosenOlehMahasiswa;
import ais.database.model.ChecklistPenilaianDosen;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GrupChecklistPenilaianDosen;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.ChecklistBaruPenilaianGuruOlehSiswa;
import ais.database.model.sekolah.ChecklistPenilaianGuru;
import ais.database.model.sekolah.GrupChecklistPenilaianGuru;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;

public class AngketUtilApi {

	@SuppressWarnings("unchecked")
	public static JSONObject checkAngket(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Session session = HibernateUtil.openSession();
				try {
				List<String> alasans = new ArrayList<String>();
				List<String> warning = new ArrayList<String>();
				if (tbmuser.getMahasiswa() != null) {

					Mahasiswa mahasiswa = tbmuser.getMahasiswa();

					alasans = session.createCriteria(BlokirMahasiswa.class).add(Restrictions.isNotNull("keterangan"))
							.add(Restrictions.ne("keterangan", "")).setProjection(Projections.property("keterangan"))
							.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("login", true)).list();

					if (alasans.isEmpty()) {

						List<JenisKegiatan> pembayaranSyaratLogin = new ArrayList<JenisKegiatan>();
						if (ais.common.CommonHelperClass.jenisKegiatansAktif == null) {
							Common.reloadJenisKegiatans();
						}
						for (JenisKegiatan d : ais.common.CommonHelperClass.jenisKegiatansAktif) {
							if (d.getDigunakanSyaratLogin()) {
								pembayaranSyaratLogin.add(d);
							}
						}

						int tahunAngkatanMhs = mahasiswa.getTahunangkatan();

						String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

						int currentSmt = Common.getSemester(tahunAngkatanMhs, semesterMulai,
								mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

						if (!pembayaranSyaratLogin.isEmpty()) {

							for (JenisKegiatan syarat : pembayaranSyaratLogin) {

								if (syarat.getBayarHanyaSmtSaatIni() || syarat.getBayarHanyaSmtSaatIniDanSebelumnya()
										|| syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi()
										|| syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi3()
										|| syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi4()

										|| syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi5()
										|| syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi6()
										|| syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi7()
										|| syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi8()) {
									if (syarat.getBayarHanyaSmtSaatIni()) {
										PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa
												.ambilCuti(currentSmt, null, false);
										if (pendaftaranCutiMahasiswa == null
												|| !pendaftaranCutiMahasiswa.getPersetujuan()) {
											if (!syarat.bolehMasuk(mahasiswa, currentSmt, 0, warning)) {

											}
										}
									}
									if (syarat.getBayarHanyaSmtSaatIniDanSebelumnya() && currentSmt > 1) {
										PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa
												.ambilCuti(currentSmt - 1, null, false);
										if (pendaftaranCutiMahasiswa == null
												|| !pendaftaranCutiMahasiswa.getPersetujuan()) {
											if (!syarat.bolehMasuk(mahasiswa, currentSmt - 1, 1, warning)) {

											}
										}
									}
									if (syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi() && currentSmt > 2) {
										PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa
												.ambilCuti(currentSmt - 2, null, false);
										if (pendaftaranCutiMahasiswa == null
												|| !pendaftaranCutiMahasiswa.getPersetujuan()) {
											if (!syarat.bolehMasuk(mahasiswa, currentSmt - 2, 2, warning)) {

											}
										}
									}
									if (syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi3() && currentSmt > 3) {
										PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa
												.ambilCuti(currentSmt - 3, null, false);
										if (pendaftaranCutiMahasiswa == null
												|| !pendaftaranCutiMahasiswa.getPersetujuan()) {
											if (!syarat.bolehMasuk(mahasiswa, currentSmt - 3, 3, warning)) {

											}
										}
									}
									if (syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi4() && currentSmt > 4) {
										PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa
												.ambilCuti(currentSmt - 4, null, false);
										if (pendaftaranCutiMahasiswa == null
												|| !pendaftaranCutiMahasiswa.getPersetujuan()) {
											if (!syarat.bolehMasuk(mahasiswa, currentSmt - 4, 4, warning)) {

											}
										}
									}

									if (syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi5() && currentSmt > 5) {
										PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa
												.ambilCuti(currentSmt - 5, null, false);
										if (pendaftaranCutiMahasiswa == null
												|| !pendaftaranCutiMahasiswa.getPersetujuan()) {
											if (!syarat.bolehMasuk(mahasiswa, currentSmt - 5, 5, warning)) {

											}
										}
									}

									if (syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi6() && currentSmt > 6) {
										PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa
												.ambilCuti(currentSmt - 6, null, false);
										if (pendaftaranCutiMahasiswa == null
												|| !pendaftaranCutiMahasiswa.getPersetujuan()) {
											if (!syarat.bolehMasuk(mahasiswa, currentSmt - 6, 6, warning)) {

											}
										}
									}

									if (syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi7() && currentSmt > 7) {
										PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa
												.ambilCuti(currentSmt - 7, null, false);
										if (pendaftaranCutiMahasiswa == null
												|| !pendaftaranCutiMahasiswa.getPersetujuan()) {
											if (!syarat.bolehMasuk(mahasiswa, currentSmt - 7, 7, warning)) {

											}
										}
									}

									if (syarat.getBayarHanyaSmtSaatIniDanSebelumnyalagi8() && currentSmt > 8) {
										PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa
												.ambilCuti(currentSmt - 8, null, false);
										if (pendaftaranCutiMahasiswa == null
												|| !pendaftaranCutiMahasiswa.getPersetujuan()) {
											if (!syarat.bolehMasuk(mahasiswa, currentSmt - 8, 8, warning)) {

											}
										}
									}

								} else {
									for (int smtMulai = syarat.getMinSmt(); smtMulai <= currentSmt; smtMulai++) {
										PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa
												.ambilCuti(smtMulai, null, false);
										if (pendaftaranCutiMahasiswa == null
												|| !pendaftaranCutiMahasiswa.getPersetujuan()) {
											if (!syarat.bolehMasuk(mahasiswa, smtMulai, 0, warning)) {

											}
										}
									}
								}
							}

						}

						pembayaranSyaratLogin = null;

						String jenis = currentSmt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
						Konfigurasi konfigurasi = Common.checkKonfigurasiDenganKalenderAkademik(session,
								"checklist_penilaian_dosen_semester_berlangsung", Common.getCurrentTahunAkademik(),
								jenis, mahasiswa.getSemesterMulai(), mahasiswa.getJurusan().getFakultas(),
								mahasiswa.getJurusan(), mahasiswa.getProgram());
						boolean angketBerlangsung = konfigurasi != null
								&& konfigurasi.getNilai().equals(Konfigurasi.AKTIF);

						if (angketBerlangsung) {
							Integer sp = null;
							if (AngketUtil.checkStatusChecklist(mahasiswa, currentSmt, sp)) {

							}
						}
					}
				}

				alasans.addAll(warning);

				jsonObject.put("alasans_tidak_boleh_login", alasans);
				jsonObject.put("status", "00");
				jsonObject.put("description", "OK");

				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);
				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/AngketUtilApi.java:249");
			}
		}
		return jsonObject;
	}

	private static Map<Long, Map<String, ChecklistBaruPenilaianDosenOlehMahasiswa>> maps = new HashMap<Long, Map<String, ChecklistBaruPenilaianDosenOlehMahasiswa>>();

	private static Map<Long, Map<String, ChecklistBaruPenilaianGuruOlehSiswa>> maps1 = new HashMap<Long, Map<String, ChecklistBaruPenilaianGuruOlehSiswa>>();

	public static JSONObject simpanAngket(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Session session = HibernateUtil.openSession();
				try {

				if (tbmuser.getMahasiswa() != null) {
					Mahasiswa mahasiswa = tbmuser.getMahasiswa();
					Long perkuliahan = request.isNull("perkuliahan_id") ? null : ais.common.CommonJSONUtil.ambilLong(request,"perkuliahan_id");
					Long dosen = request.isNull("dosen_id") ? null : ais.common.CommonJSONUtil.ambilLong(request,"dosen_id");
					Long check = ais.common.CommonJSONUtil.ambilLong(request,"check_id");
					Integer nilai = request.getInt("nilai");
					String keterangan = request.isNull("keterangan") ? "" : request.getString("keterangan");
					String masukan = request.isNull("masukan") ? null : request.getString("masukan");

					if (perkuliahan != null && dosen != null) {

						Perkuliahan perkuliahanData = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
								perkuliahan, true);
						Dosen dosenData = (Dosen) ConstantValues.ambil(Dosen.class.getName(), dosen, true);

						ChecklistPenilaianDosen checklistPenilaianDosen = (ChecklistPenilaianDosen) ConstantValues
								.ambil(ChecklistPenilaianDosen.class.getName(), check, true);

						Map<String, ChecklistBaruPenilaianDosenOlehMahasiswa> mapsKey = maps.get(mahasiswa.getId());
						if (mapsKey == null) {
							mapsKey = mahasiswa.byKey(session, true);
							maps.put(mahasiswa.getId(), mapsKey);
						}
						String kodeUnik = mahasiswa.getId() + "_" + perkuliahan + "_" + dosen;
						ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa = mapsKey
								.get(kodeUnik);
						if (checklistBaruPenilaianDosenOlehMahasiswa == null) {
							checklistBaruPenilaianDosenOlehMahasiswa = new ChecklistBaruPenilaianDosenOlehMahasiswa();
						}

						checklistBaruPenilaianDosenOlehMahasiswa.setValue(nilai, mahasiswa, dosenData, perkuliahanData,
								checklistPenilaianDosen, keterangan);
						checklistBaruPenilaianDosenOlehMahasiswa.setMasukan(masukan);

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, checklistBaruPenilaianDosenOlehMahasiswa);
						session.getTransaction().commit();

						mapsKey.put(kodeUnik, checklistBaruPenilaianDosenOlehMahasiswa);

						jsonObject.put("data", checklistBaruPenilaianDosenOlehMahasiswa);
					}

				}

				else if (tbmuser.getSiswa() != null) {
					Siswa siswa = tbmuser.getSiswa();
					Long jadwalPelajaran = request.isNull("jadwalPelajaran_id") ? null
							: ais.common.CommonJSONUtil.ambilLong(request,"jadwalPelajaran_id");
					Long guru = request.isNull("guru_id") ? null : ais.common.CommonJSONUtil.ambilLong(request,"guru_id");
					Long check = ais.common.CommonJSONUtil.ambilLong(request,"check_id");
					Integer nilai = request.getInt("nilai");
					String keterangan = request.isNull("keterangan") ? "" : request.getString("keterangan");
					String masukan = request.isNull("masukan") ? null : request.getString("masukan");

					if (jadwalPelajaran != null && guru != null) {

						JadwalPelajaran jadwalPelajaranData = (JadwalPelajaran) ConstantValues
								.ambil(JadwalPelajaran.class.getName(), jadwalPelajaran, true);
						Guru guruData = (Guru) ConstantValues.ambil(Guru.class.getName(), guru, true);

						ChecklistPenilaianGuru checklistPenilaianGuru = (ChecklistPenilaianGuru) ConstantValues
								.ambil(ChecklistPenilaianGuru.class.getName(), check, true);

						Map<String, ChecklistBaruPenilaianGuruOlehSiswa> mapsKey = maps1.get(siswa.getId());
						if (mapsKey == null) {
							mapsKey = siswa.byKey(session, true);
							maps1.put(siswa.getId(), mapsKey);
						}
						String kodeUnik = siswa.getId() + "_" + jadwalPelajaran + "_" + guru;
						ChecklistBaruPenilaianGuruOlehSiswa checklistBaruPenilaianGuruOlehSiswa = mapsKey.get(kodeUnik);
						if (checklistBaruPenilaianGuruOlehSiswa == null) {
							checklistBaruPenilaianGuruOlehSiswa = new ChecklistBaruPenilaianGuruOlehSiswa();
						}

						checklistBaruPenilaianGuruOlehSiswa.setValue(nilai, siswa, guruData, jadwalPelajaranData,
								checklistPenilaianGuru, keterangan);
						checklistBaruPenilaianGuruOlehSiswa.setMasukan(masukan);

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, checklistBaruPenilaianGuruOlehSiswa);
						session.getTransaction().commit();

						mapsKey.put(kodeUnik, checklistBaruPenilaianGuruOlehSiswa);

						jsonObject.put("data", checklistBaruPenilaianGuruOlehSiswa);
					}

				}

				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);
				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			}

			jsonObject.put("status", "00");
			jsonObject.put("description", "Simpan data berhasil");
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/AngketUtilApi.java:375");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject daftarAngket(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Session session = HibernateUtil.openSession();
				try {
				JSONArray jsonArray = new JSONArray();
				JSONArray arrayAngket = new JSONArray();

				if (tbmuser.getMahasiswa() != null) {
					Mahasiswa mahasiswa = tbmuser.getMahasiswa();
					Fakultas fakultas = mahasiswa.getJurusan().getFakultas();
					Jurusan jurusan = mahasiswa.getJurusan();
					String program = mahasiswa.getProgram();
					String angkatan = mahasiswa.getTahunangkatan() == null ? ""
							: mahasiswa.getTahunangkatan().toString();

					List<GrupChecklistPenilaianDosen> grupChecklistPenilaianDosens = ConstantValues.simpleList(
							session.createCriteria(GrupChecklistPenilaianDosen.class)

									.createAlias("angketPenilaianDosen", "angketPenilaianDosen", Criteria.LEFT_JOIN)

									.add(Restrictions.or(Restrictions.isNull("angketPenilaianDosen.untukMahasiswa"),
											Restrictions.eq("angketPenilaianDosen.untukMahasiswa", true)))

									.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.fakultas", fakultas),
											Restrictions.isNull("angketPenilaianDosen.fakultas")))

									.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.jurusan", jurusan),
											Restrictions.isNull("angketPenilaianDosen.jurusan")))

									.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.program", ""),
											Restrictions.or(Restrictions.eq("angketPenilaianDosen.program", program),
													Restrictions.isNull("angketPenilaianDosen.program"))))

									.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.angkatan", ""),
											Restrictions.or(
													Restrictions.ilike("angketPenilaianDosen.angkatan", angkatan),
													Restrictions.isNull("angketPenilaianDosen.angkatan"))))

									.addOrder(Order.asc("angketPenilaianDosen.kode")).addOrder(Order.asc("isi"))
									.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))),
							GrupChecklistPenilaianDosen.class);

					Map<Long, List<ChecklistPenilaianDosen>> dataAngket = new HashMap<Long, List<ChecklistPenilaianDosen>>();
					for (GrupChecklistPenilaianDosen g : grupChecklistPenilaianDosens) {
						List<ChecklistPenilaianDosen> checklistPenilaianDosens = ConstantValues.simpleList(
								session.createCriteria(ChecklistPenilaianDosen.class).addOrder(Order.asc("isi"))
										.add(Restrictions.or(Restrictions.eq("aktif", true),
												Restrictions.isNull("aktif")))
										.add(Restrictions.eq("grupChecklistPenilaianDosen", g)),
								ChecklistPenilaianDosen.class);

						dataAngket.put(g.getId(), checklistPenilaianDosens);

						Integer jumlahChecklist = Integer.parseInt(
								Common.getKonfigurasi("jumlah_pilihan_checklist_penilaian_dosen_oleh_mahasiswa", "5")
										.getNilai().trim());
						try {
							jumlahChecklist = g.getAngketPenilaianDosen().getJumlahPilihan();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/AngketUtilApi.java:446");

						}

						JSONObject jsonObjectSub = new JSONObject();
						jsonObjectSub.put("jumlahChecklist", jumlahChecklist);
						Common.insertProperty(GrupChecklistPenilaianDosen.class, g, jsonObjectSub, "grup", 1);

						JSONArray dataAngketSub = new JSONArray();

						for (ChecklistPenilaianDosen checklistPenilaianDosen : checklistPenilaianDosens) {

							JSONObject jsonObjectSubData = new JSONObject();

							Common.insertProperty(ChecklistPenilaianDosen.class, checklistPenilaianDosen,
									jsonObjectSubData, "angket", 1);

							dataAngketSub.put(jsonObjectSubData);
						}

						jsonObjectSub.put("dataAngketSub", dataAngketSub);

						arrayAngket.put(jsonObjectSub);
					}

					Integer semester = request.isNull("semester") ? tbmuser.getMahasiswa().currentSemester()
							: request.getInt("semester");
					Boolean refresh = request.isNull("refresh") ? false : request.getBoolean("refresh");

					Map<String, ChecklistBaruPenilaianDosenOlehMahasiswa> mapsKey = mahasiswa.byKey(session, refresh);

					List<Long> perkuliahans = mahasiswa.ambilPerkuliahanDanParalel(semester, null);

					for (Long p : perkuliahans) {
						Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(), p);

						List<Dosen> dosens = perkuliahan.populateDosenBuNama();
						for (Dosen dosen : dosens) {
							JSONObject jsonObjectSub = new JSONObject();
							Common.insertProperty(Perkuliahan.class, perkuliahan, jsonObjectSub, "perkuliahan", 1);
							Common.insertProperty(Dosen.class, dosen, jsonObjectSub, "dosen", 1);

							Integer jumlahChecklist = ((Number) session.createCriteria(ChecklistPenilaianDosen.class)
									.createAlias("grupChecklistPenilaianDosen", "grupChecklistPenilaianDosen")

									.createAlias("grupChecklistPenilaianDosen.angketPenilaianDosen",
											"angketPenilaianDosen")

									.add(Restrictions.or(Restrictions.isNull("angketPenilaianDosen.untukMahasiswa"),
											Restrictions.eq("angketPenilaianDosen.untukMahasiswa", true)))

									.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.fakultas", fakultas),
											Restrictions.isNull("angketPenilaianDosen.fakultas")))

									.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.jurusan", jurusan),
											Restrictions.isNull("angketPenilaianDosen.jurusan")))

									.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.program", ""),
											Restrictions.or(Restrictions.eq("angketPenilaianDosen.program", program),
													Restrictions.isNull("angketPenilaianDosen.program"))))

									.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.angkatan", ""),
											Restrictions.or(
													Restrictions.ilike("angketPenilaianDosen.angkatan", angkatan),
													Restrictions.isNull("angketPenilaianDosen.angkatan"))))

									.add(Restrictions.or(Restrictions.eq("grupChecklistPenilaianDosen.aktif", true),
											Restrictions.isNull("grupChecklistPenilaianDosen.aktif")))
									.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
									.setProjection(Projections.rowCount()).uniqueResult()).intValue();

							String kodeUnik = mahasiswa.getId() + "_" + perkuliahan.getId() + "_" + dosen.getId();
							ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa = mapsKey
									.get(kodeUnik);
							Integer jumlahSaved = checklistBaruPenilaianDosenOlehMahasiswa == null ? 0
									: checklistBaruPenilaianDosenOlehMahasiswa.count();

							if (jumlahSaved > jumlahChecklist) {
								jumlahChecklist = jumlahSaved;
							}
							jsonObjectSub.put("jumlahChecklist", jumlahChecklist);
							jsonObjectSub.put("jumlahSaved", jumlahSaved);
							jsonObjectSub.put("labelSudahTerisi",
									(jumlahChecklist.equals(jumlahSaved) ? "Telah diisi" : "Belum terisi") + " - "
											+ (jumlahSaved + " dari " + jumlahChecklist + " telah terisi"));

							if (checklistBaruPenilaianDosenOlehMahasiswa != null) {
								Common.insertProperty(ChecklistBaruPenilaianDosenOlehMahasiswa.class,
										checklistBaruPenilaianDosenOlehMahasiswa, jsonObjectSub, "angket", 1,
										"keterangan", "dosen");
							}

							for (GrupChecklistPenilaianDosen g : grupChecklistPenilaianDosens) {
								List<ChecklistPenilaianDosen> checklistPenilaianDosens = dataAngket.get(g.getId());
								for (ChecklistPenilaianDosen c : checklistPenilaianDosens) {
									Integer checklistPenilaianDosenOlehMahasiswa = checklistBaruPenilaianDosenOlehMahasiswa == null
											? 0
											: checklistBaruPenilaianDosenOlehMahasiswa.getValue(c);

									JSONObject jsonObjectAngket = new JSONObject();
									jsonObjectAngket.put("keterangan",
											checklistBaruPenilaianDosenOlehMahasiswa == null ? ""
													: checklistBaruPenilaianDosenOlehMahasiswa.getKeteranganValue(c));
									jsonObjectAngket.put("nilai", checklistPenilaianDosenOlehMahasiswa);

									jsonObjectSub.put("angket_" + c.getId(), jsonObjectAngket);
								}

							}

							jsonArray.put(jsonObjectSub);
						}
					}

				}

				else if (tbmuser.getSiswa() != null) {
					Siswa siswa = tbmuser.getSiswa();
					Yayasan yayasan = siswa.getSekolah().getYayasan();
					Sekolah sekolah = siswa.getSekolah();
					String program = siswa.getProgram();
					String angkatan = siswa.getTahunMasuk() == null ? "" : siswa.getTahunMasuk().toString();

					List<GrupChecklistPenilaianGuru> grupChecklistPenilaianGurus = ConstantValues.simpleList(
							session.createCriteria(GrupChecklistPenilaianGuru.class)

									.createAlias("angketPenilaianGuru", "angketPenilaianGuru", Criteria.LEFT_JOIN)

									.add(Restrictions.or(Restrictions.isNull("angketPenilaianGuru.untukSiswa"),
											Restrictions.eq("angketPenilaianGuru.untukSiswa", true)))

									.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.yayasan", yayasan),
											Restrictions.isNull("angketPenilaianGuru.yayasan")))

									.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.sekolah", sekolah),
											Restrictions.isNull("angketPenilaianGuru.sekolah")))

									.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.program", ""),
											Restrictions.or(Restrictions.eq("angketPenilaianGuru.program", program),
													Restrictions.isNull("angketPenilaianGuru.program"))))

									.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.angkatan", ""),
											Restrictions.or(
													Restrictions.ilike("angketPenilaianGuru.angkatan", angkatan),
													Restrictions.isNull("angketPenilaianGuru.angkatan"))))

									.addOrder(Order.asc("angketPenilaianGuru.kode")).addOrder(Order.asc("isi"))
									.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))),
							GrupChecklistPenilaianGuru.class);

					Map<Long, List<ChecklistPenilaianGuru>> dataAngket = new HashMap<Long, List<ChecklistPenilaianGuru>>();
					for (GrupChecklistPenilaianGuru g : grupChecklistPenilaianGurus) {
						List<ChecklistPenilaianGuru> checklistPenilaianGurus = ConstantValues.simpleList(
								session.createCriteria(ChecklistPenilaianGuru.class).addOrder(Order.asc("isi"))
										.add(Restrictions.or(Restrictions.eq("aktif", true),
												Restrictions.isNull("aktif")))
										.add(Restrictions.eq("grupChecklistPenilaianGuru", g)),
								ChecklistPenilaianGuru.class);

						dataAngket.put(g.getId(), checklistPenilaianGurus);

						Integer jumlahChecklist = Integer.parseInt(
								Common.getKonfigurasi("jumlah_pilihan_checklist_penilaian_guru_oleh_siswa", "5")
										.getNilai().trim());
						try {
							jumlahChecklist = g.getAngketPenilaianGuru().getJumlahPilihan();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/AngketUtilApi.java:612");

						}

						JSONObject jsonObjectSub = new JSONObject();
						jsonObjectSub.put("jumlahChecklist", jumlahChecklist);
						Common.insertProperty(GrupChecklistPenilaianGuru.class, g, jsonObjectSub, "grup", 1);

						JSONArray dataAngketSub = new JSONArray();

						for (ChecklistPenilaianGuru checklistPenilaianGuru : checklistPenilaianGurus) {

							JSONObject jsonObjectSubData = new JSONObject();

							Common.insertProperty(ChecklistPenilaianGuru.class, checklistPenilaianGuru,
									jsonObjectSubData, "angket", 1);

							dataAngketSub.put(jsonObjectSubData);
						}

						jsonObjectSub.put("dataAngketSub", dataAngketSub);

						arrayAngket.put(jsonObjectSub);
					}

					Integer semester = request.isNull("semester") ? 1 : request.getInt("semester");
					String ta = request.isNull("ta") ? Common.getCurrentTahunAkademik() : request.getString("ta");
					Boolean refresh = request.isNull("refresh") ? false : request.getBoolean("refresh");

					Map<String, ChecklistBaruPenilaianGuruOlehSiswa> mapsKey = siswa.byKey(session, refresh);

					String sql = "this_.kelas_id in (select kelas_id from sekolah.kelas_punya_siswa where siswa_id="
							+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";
					Criterion criterionKls = Restrictions.sqlRestriction(sql);

					sql = "this_.kelas_les_siswa in (select kelas_id from sekolah.kelas_les_punya_siswa where siswa_id="
							+ siswa.getId() + " and kelas_id is not null and aktif=true group by kelas_id)";
					Criterion criterionLes = Restrictions.sqlRestriction(sql);

					Criterion criterion = Restrictions.or(criterionKls, criterionLes);

					List<Long> jadwalPelajarans = session.createCriteria(JadwalPelajaran.class).add(criterion)
							.add(Restrictions.eq("semester", semester)).add(Restrictions.eq("tahunAjaran", ta))
							.setProjection(Projections.property("id")).list();

					for (Long p : jadwalPelajarans) {
						JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) ConstantValues
								.ambil(JadwalPelajaran.class.getName(), p);

						List<Guru> gurus = jadwalPelajaran.populateGuruBuNama();
						for (Guru guru : gurus) {
							JSONObject jsonObjectSub = new JSONObject();
							Common.insertProperty(JadwalPelajaran.class, jadwalPelajaran, jsonObjectSub,
									"jadwalPelajaran", 1);
							Common.insertProperty(Guru.class, guru, jsonObjectSub, "guru", 1);

							Integer jumlahChecklist = ((Number) session.createCriteria(ChecklistPenilaianGuru.class)
									.createAlias("grupChecklistPenilaianGuru", "grupChecklistPenilaianGuru")

									.createAlias("grupChecklistPenilaianGuru.angketPenilaianGuru",
											"angketPenilaianGuru")

									.add(Restrictions.or(Restrictions.isNull("angketPenilaianGuru.untukSiswa"),
											Restrictions.eq("angketPenilaianGuru.untukSiswa", true)))

									.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.yayasan", yayasan),
											Restrictions.isNull("angketPenilaianGuru.yayasan")))

									.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.sekolah", sekolah),
											Restrictions.isNull("angketPenilaianGuru.sekolah")))

									.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.program", ""),
											Restrictions.or(Restrictions.eq("angketPenilaianGuru.program", program),
													Restrictions.isNull("angketPenilaianGuru.program"))))

									.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.angkatan", ""),
											Restrictions.or(
													Restrictions.ilike("angketPenilaianGuru.angkatan", angkatan),
													Restrictions.isNull("angketPenilaianGuru.angkatan"))))

									.add(Restrictions.or(Restrictions.eq("grupChecklistPenilaianGuru.aktif", true),
											Restrictions.isNull("grupChecklistPenilaianGuru.aktif")))
									.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
									.setProjection(Projections.rowCount()).uniqueResult()).intValue();

							String kodeUnik = siswa.getId() + "_" + jadwalPelajaran.getId() + "_" + guru.getId();
							ChecklistBaruPenilaianGuruOlehSiswa checklistBaruPenilaianGuruOlehSiswa = mapsKey
									.get(kodeUnik);
							Integer jumlahSaved = checklistBaruPenilaianGuruOlehSiswa == null ? 0
									: checklistBaruPenilaianGuruOlehSiswa.count();

							if (jumlahSaved > jumlahChecklist) {
								jumlahChecklist = jumlahSaved;
							}
							jsonObjectSub.put("jumlahChecklist", jumlahChecklist);
							jsonObjectSub.put("jumlahSaved", jumlahSaved);
							jsonObjectSub.put("labelSudahTerisi",
									(jumlahChecklist.equals(jumlahSaved) ? "Telah diisi" : "Belum terisi") + " - "
											+ (jumlahSaved + " dari " + jumlahChecklist + " telah terisi"));

							if (checklistBaruPenilaianGuruOlehSiswa != null) {
								Common.insertProperty(ChecklistBaruPenilaianGuruOlehSiswa.class,
										checklistBaruPenilaianGuruOlehSiswa, jsonObjectSub, "angket", 1, "keterangan",
										"jadwalPelajaran", "guru");
							}

							for (GrupChecklistPenilaianGuru g : grupChecklistPenilaianGurus) {
								List<ChecklistPenilaianGuru> checklistPenilaianGurus = dataAngket.get(g.getId());
								for (ChecklistPenilaianGuru c : checklistPenilaianGurus) {
									Integer checklistPenilaianGuruOlehSiswa = checklistBaruPenilaianGuruOlehSiswa == null
											? 0
											: checklistBaruPenilaianGuruOlehSiswa.getValue(c);

									JSONObject jsonObjectAngket = new JSONObject();
									jsonObjectAngket.put("keterangan", checklistBaruPenilaianGuruOlehSiswa == null ? ""
											: checklistBaruPenilaianGuruOlehSiswa.getKeteranganValue(c));
									jsonObjectAngket.put("nilai", checklistPenilaianGuruOlehSiswa);

									jsonObjectSub.put("angket_" + c.getId(), jsonObjectAngket);
								}

							}

							jsonArray.put(jsonObjectSub);
						}
					}

				}

				jsonObject.put("angket", arrayAngket);
				jsonObject.put("data", jsonArray);
				jsonObject.put("status", "00");
				jsonObject.put("description", "Ambil data berhasil");

				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);
				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/AngketUtilApi.java:758");
			}
		}
		return jsonObject;
	}
}
