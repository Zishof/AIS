package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.servlet.Api;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ChecklistHasilPenilaianUmum;
import ais.database.model.ChecklistPenilaianUmum;
import ais.database.model.Dosen;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.IsiAngketParameterUmum;
import ais.database.model.JadwalChecklistPenilaianUmum;
import ais.database.model.Mahasiswa;
import ais.database.model.OrangTua;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sisdes.Penduduk;

public class AngketUtilUmumApi {

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

				Long check = ais.common.CommonJSONUtil.ambilLong(request,"check_id");
				Integer nilai = request.getInt("nilai");
				String tahunAkademik = request.isNull("tahunAkademik") ? Common.getCurrentTahunAkademik()
						: request.getString("tahunAkademik");
				String keterangan = request.isNull("keterangan") ? "" : request.getString("keterangan");
				String semester = request.isNull("semester")
						? (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP)
						: request.getString("semester");

				ChecklistPenilaianUmum checklistPenilaianUmum = (ChecklistPenilaianUmum) ConstantValues
						.ambil(ChecklistPenilaianUmum.class.getName(), check, true);

				System.out.println("checklistPenilaianUmum " + checklistPenilaianUmum);

				try {

					Mahasiswa mahasiswa = tbmuser.getMahasiswa();
					Dosen dosen = tbmuser.ambilDosen();
					Guru guru = tbmuser.ambilGuru();
					Siswa siswa = tbmuser.getSiswa();
					Long pertemuanId = null;

					ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum = (ChecklistHasilPenilaianUmum) ConstantValues
							.simpleObject(session.createCriteria(ChecklistHasilPenilaianUmum.class)

									.add(Restrictions.isNull("tbmuserDinilai"))

									.add(pertemuanId == null ? Restrictions.isNull("pertemuanId")
											: Restrictions.eq("pertemuanId", pertemuanId))

									.add(mahasiswa == null || mahasiswa.getId() == null
											? Restrictions.isNull("mahasiswa")
											: Restrictions.eq("mahasiswa", mahasiswa))

									.add(siswa == null || siswa.getId() == null ? Restrictions.isNull("siswa")
											: Restrictions.eq("siswa", siswa))

									.add(dosen == null || dosen.getId() == null ? Restrictions.isNull("dosen")
											: Restrictions.eq("dosen", dosen))

									.add(guru == null || guru.getId() == null ? Restrictions.isNull("guru")
											: Restrictions.eq("guru", guru))

									.add(siswa != null || mahasiswa != null || tbmuser == null
											|| tbmuser.getUserId() == null ? Restrictions.isNull("tbmuser")
													: Restrictions.eq("tbmuser", tbmuser))

									.add(Restrictions.eq("checklistPenilaianUmum", checklistPenilaianUmum))
									.add(Restrictions.eq("semesterStr", semester))
									.add(Restrictions.eq("tahunAkademik", tahunAkademik)).setMaxResults(1),
									ChecklistHasilPenilaianUmum.class);

					System.out.println("checklistHasilPenilaianUmum " + checklistHasilPenilaianUmum);

					if (checklistHasilPenilaianUmum == null) {
						checklistHasilPenilaianUmum = new ChecklistHasilPenilaianUmum();
					}
					checklistHasilPenilaianUmum.setPertemuanId(pertemuanId);
					checklistHasilPenilaianUmum.setMahasiswa(mahasiswa);
					checklistHasilPenilaianUmum.setDosen(dosen);

					checklistHasilPenilaianUmum.setGuru(guru);
					checklistHasilPenilaianUmum.setSiswa(siswa);

					if (tbmuser.getSiswa() == null && tbmuser.getMahasiswa() == null && tbmuser.getUserId() != null
							&& !tbmuser.getUserId().trim().isEmpty()) {
						checklistHasilPenilaianUmum.setTbmuser(tbmuser);
					}
					checklistHasilPenilaianUmum.setChecklistPenilaianUmum(checklistPenilaianUmum);
					checklistHasilPenilaianUmum.setNilai(nilai);
					checklistHasilPenilaianUmum.setSemesterStr(semester);
					checklistHasilPenilaianUmum.setTahunAkademik(tahunAkademik);
					checklistHasilPenilaianUmum.setKeterangan(keterangan);

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, checklistHasilPenilaianUmum);
					session.getTransaction().commit();

					System.out.println("Simpan Penilaian " + checklistPenilaianUmum.getIsi() + ", nilai = "
							+ checklistHasilPenilaianUmum.getNilai());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/AngketUtilUmumApi.java:131");
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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/AngketUtilUmumApi.java:144");
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/AngketUtilUmumApi.java:150");
			}
		}
		return jsonObject;
	}

	public static JSONObject logout(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {

				Siswa siswa = tbmuser.getSiswa();
				Mahasiswa mahasiswa = tbmuser.getMahasiswa();
				Penduduk penduduk = tbmuser.getPenduduk();

				Session session = HibernateUtil.openSession();
				try {
				if (penduduk != null && penduduk.getId() != null) {
					session.refresh(penduduk);
					penduduk.setGcpToken(null);
					penduduk.setToken(null);

					session.getTransaction().begin();
					Common.refreshUpdate(session, penduduk);
					session.getTransaction().commit();
				} else if (siswa != null && siswa.getId() != null) {
					session.refresh(siswa);
					siswa.setGcpToken(null);
					siswa.setToken(null);

					session.getTransaction().begin();
					Common.refreshUpdate(session, siswa);
					session.getTransaction().commit();
				} else if (mahasiswa != null && mahasiswa.getId() != null) {
					session.refresh(mahasiswa);
					mahasiswa.setGcpToken(null);
					mahasiswa.setToken(null);

					session.getTransaction().begin();
					Common.refreshUpdate(session, mahasiswa);
					session.getTransaction().commit();
				} else {
					session.refresh(tbmuser);
					tbmuser.setGcpToken(null);
					tbmuser.setToken(null);

					session.getTransaction().begin();
					Common.refreshUpdate(session, tbmuser);
					session.getTransaction().commit();
				}

				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);

				String token = request.isNull("token") ? "------" : request.getString("token");
				Api.removeToken(token);

				// PENTING: ApiUtil.currentUser(JSONObject, HttpServletRequest) punya jalur
				// otentikasi KEDUA selain token -- ia cek session HTTP lebih dulu
				// (Common.getCurrentUser(req), baca atribut "mytbmuser"/"usersTemp"), dan
				// menaruh user ke session itu setiap kali token berhasil divalidasi. Bila
				// hanya token yang dihapus (di atas) tanpa membersihkan session ini, client
				// yang menyimpan cookie JSESSIONID (mis. Postman secara default) TETAP bisa
				// terotentikasi lewat session lama meski token sudah tidak valid -- persis
				// keluhan "sudah logout, tapi request lagi masih bisa". Bersihkan sesi HTTP
				// di sini juga supaya kedua jalur otentikasi benar-benar tertutup saat logout.
				try {
					if (req != null) {
						javax.servlet.http.HttpSession httpSession = req.getSession(false);
						if (httpSession != null) {
							httpSession.removeAttribute("mytbmuser");
							httpSession.removeAttribute("usersTemp");
							httpSession.invalidate();
						}
					}
				} catch (Exception eSess) { ais.common.ErrorAuditUtil.record(eSess, "auto-audit(empty-catch) src/ais/action/servlet/api/AngketUtilUmumApi.java:logout-invalidate-session");
				}

				jsonObject.put("status", "00");
				jsonObject.put("description", "Logout berhasil");

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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/AngketUtilUmumApi.java:224");
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

				String tahunAkademik = request.isNull("tahunAkademik") ? Common.getCurrentTahunAkademik()
						: request.getString("tahunAkademik");

				String semester = request.isNull("semester")
						? (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP)
						: request.getString("semester");

				Boolean refresh = request.isNull("refresh") ? false : request.getBoolean("refresh");

				Mahasiswa mahasiswa = tbmuser.getMahasiswa();
				StatusMahasiswa statusMahasiswa = mahasiswa == null ? null
						: ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();
				Dosen dosen = tbmuser.ambilDosen();
				Guru guru = tbmuser.ambilGuru();
				OrangTua orangTua = tbmuser.getOrangTua();
				Siswa siswa = tbmuser.getSiswa();
				Long pertemuanId = null;

				Criterion criterion = Restrictions.sqlRestriction("false");

				if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
						&& ConstantValues.tbmroleUmum != null && ConstantValues.tbmroleUmum.getRoleId() != null
						&& ConstantValues.tbmroleUmum.getRoleId().equals(tbmuser.hakAkses().getRoleId())) {
					criterion = Restrictions
							.sqlRestriction("  diperuntukkan='" + GrupChecklistPenilaianUmum.UNTUK_LINK_UMUM + "' ");
				} else if (mahasiswa != null && mahasiswa.getId() != null && statusMahasiswa != null
						&& statusMahasiswa.getNama() != null
						&& statusMahasiswa.getNama().toLowerCase().trim().contains("lulus")) {
					criterion = Restrictions.sqlRestriction(" ( (diperuntukkan='"
							+ GrupChecklistPenilaianUmum.UNTUK_ALUMNI + "' and (status_mahasiswa="
							+ statusMahasiswa.getId() + " or status_mahasiswa is null) and (mulai_angkatan<="
							+ mahasiswa.getTahunangkatan() + " or mulai_angkatan is null)  and (sampai_angkatan>="
							+ mahasiswa.getTahunangkatan() + " or sampai_angkatan is null) and (fakultas="
							+ mahasiswa.getJurusan().getFakultas().getId() + " or fakultas is null) and (jurusan="
							+ mahasiswa.getJurusan().getId() + " or jurusan is null) ))");
				} else if (mahasiswa != null && mahasiswa.getId() != null) {

					criterion = Restrictions.sqlRestriction(" ( (diperuntukkan='"
							+ GrupChecklistPenilaianUmum.UNTUK_MAHASISWA + "' and (status_mahasiswa="
							+ statusMahasiswa.getId() + " or status_mahasiswa is null) and (mulai_angkatan<="
							+ mahasiswa.getTahunangkatan() + " or mulai_angkatan is null)  and (sampai_angkatan>="
							+ mahasiswa.getTahunangkatan() + " or sampai_angkatan is null) and (fakultas="
							+ mahasiswa.getJurusan().getFakultas().getId() + " or fakultas is null) and (jurusan="
							+ mahasiswa.getJurusan().getId() + " or jurusan is null) )  or diperuntukkan='"
							+ GrupChecklistPenilaianUmum.UNTUK_UMUM + "') ");
				} else if (siswa != null && siswa.getId() != null) {

					criterion = Restrictions.sqlRestriction(
							" ( (diperuntukkan='" + GrupChecklistPenilaianUmum.UNTUK_SISWA + "' and (mulai_angkatan<="
									+ siswa.getTahunMasuk() + " or mulai_angkatan is null)  and (sampai_angkatan>="
									+ siswa.getTahunMasuk() + " or sampai_angkatan is null) and (yayasan="
									+ siswa.getSekolah().getYayasan().getId() + " or yayasan is null) and (sekolah="
									+ siswa.getSekolah().getId() + " or sekolah is null) )  or diperuntukkan='"
									+ GrupChecklistPenilaianUmum.UNTUK_UMUM + "') ");
				} else if (dosen != null && dosen.getId() != null) {
					criterion = Restrictions.sqlRestriction(" (diperuntukkan='" + GrupChecklistPenilaianUmum.UNTUK_DOSEN
							+ "' or diperuntukkan='" + GrupChecklistPenilaianUmum.UNTUK_UMUM + "') ");
				} else if (guru != null && guru.getId() != null) {
					criterion = Restrictions.sqlRestriction(" (diperuntukkan='" + GrupChecklistPenilaianUmum.UNTUK_GURU
							+ "' or diperuntukkan='" + GrupChecklistPenilaianUmum.UNTUK_UMUM + "') ");
				} else if (orangTua != null && orangTua.getId() != null) {
					criterion = Restrictions
							.sqlRestriction(" (diperuntukkan='" + GrupChecklistPenilaianUmum.UNTUK_ORANG_TUA
									+ "' or diperuntukkan='" + GrupChecklistPenilaianUmum.UNTUK_UMUM + "') ");
				} else if (tbmuser != null && tbmuser.getUserId() != null) {
					criterion = Restrictions.sqlRestriction(" (diperuntukkan='" + GrupChecklistPenilaianUmum.UNTUK_ADMIN
							+ "' or diperuntukkan='" + GrupChecklistPenilaianUmum.UNTUK_UMUM + "') ");
				}

				List<JadwalChecklistPenilaianUmum> jadwalChecklistPenilaianUmums = session
						.createCriteria(JadwalChecklistPenilaianUmum.class)
						.add(Restrictions.eq("tahunAkademik", tahunAkademik)).add(Restrictions.eq("semester", semester))
						.createCriteria("grupChecklistPenilaianUmum").add(criterion)
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))).list();

				Map<Long, List<ChecklistPenilaianUmum>> dataAngket = new HashMap<Long, List<ChecklistPenilaianUmum>>();
				for (JadwalChecklistPenilaianUmum jadwalChecklistPenilaianUmum : jadwalChecklistPenilaianUmums) {
					List<ChecklistPenilaianUmum> checklistPenilaianUmums = ConstantValues
							.simpleList(
									session.createCriteria(ChecklistPenilaianUmum.class).addOrder(Order.asc("isi"))
											.add(Restrictions.or(Restrictions.eq("aktif", true),
													Restrictions.isNull("aktif")))
											.add(Restrictions.eq("grupChecklistPenilaianUmum",
													jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianUmum())),
									ChecklistPenilaianUmum.class);

					dataAngket.put(jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianUmum().getId(),
							checklistPenilaianUmums);

					IsiAngketParameterUmum isiAngketParameterUmum = (IsiAngketParameterUmum) session
							.createCriteria(IsiAngketParameterUmum.class)
							.add(mahasiswa != null && mahasiswa.getId() != null
									? Restrictions.eq("mahasiswa", mahasiswa)
									: dosen != null && dosen.getId() != null ? Restrictions.eq("dosen", dosen)

											: siswa != null && siswa.getId() != null ? Restrictions.eq("siswa", siswa)
													: guru != null && guru.getId() != null
															? Restrictions.eq("guru", guru)

															: Restrictions.eq("tbmuser", tbmuser))
							.add(Restrictions.eq("jadwalChecklistPenilaianUmum", jadwalChecklistPenilaianUmum))
							.setMaxResults(1).uniqueResult();
					if (isiAngketParameterUmum == null) {
						isiAngketParameterUmum = new IsiAngketParameterUmum();
						isiAngketParameterUmum.setDosen(dosen);
						isiAngketParameterUmum.setMahasiswa(mahasiswa);
						isiAngketParameterUmum.setSiswa(siswa);
						isiAngketParameterUmum.setGuru(guru);
						isiAngketParameterUmum.setTbmuser(tbmuser);

						isiAngketParameterUmum.setJadwalChecklistPenilaianUmum(jadwalChecklistPenilaianUmum);

						session.getTransaction().begin();
						session.save(isiAngketParameterUmum);
						session.getTransaction().commit();
					}

					Integer jumlahChecklist = Integer.parseInt(
							Common.getKonfigurasi("jumlah_pilihan_checklist_penilaian_dosen_oleh_mahasiswa", "5")
									.getNilai().trim());
					try {
						jumlahChecklist = jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianUmum()
								.getAngketPenilaianUmum().getJumlahPilihan();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/AngketUtilUmumApi.java:367");

					}

					Collection<ChecklistHasilPenilaianUmum> checklistHasilPenilaianUmums = new ArrayList<ChecklistHasilPenilaianUmum>();
					if (mahasiswa != null) {
						checklistHasilPenilaianUmums = mahasiswa.ambilChecklistHasilPenilaianUmum(session, pertemuanId,
								null, refresh);
					} else if (siswa != null) {
						checklistHasilPenilaianUmums = siswa.ambilChecklistHasilPenilaianUmum(session, pertemuanId,
								null, refresh);
					} else if (dosen != null) {
						checklistHasilPenilaianUmums = dosen.ambilChecklistHasilPenilaianUmum(session, pertemuanId,
								null, refresh);
					} else if (guru != null) {
						checklistHasilPenilaianUmums = guru.ambilChecklistHasilPenilaianUmum(session, pertemuanId, null,
								refresh);
					} else if (tbmuser != null) {
						checklistHasilPenilaianUmums = tbmuser.ambilChecklistHasilPenilaianUmum(session, pertemuanId,
								null, refresh);
					}

					Map<Long, ChecklistHasilPenilaianUmum> maps = new HashMap<Long, ChecklistHasilPenilaianUmum>();
					for (ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum : checklistHasilPenilaianUmums) {
						if (checklistHasilPenilaianUmum.getTahunAkademik().equals(tahunAkademik)
								&& (pertemuanId == null || (checklistHasilPenilaianUmum.getPertemuanId() != null
										&& pertemuanId.equals(checklistHasilPenilaianUmum.getPertemuanId())))
								&& checklistHasilPenilaianUmum.getSemesterStr().equals(semester)) {
							maps.put(checklistHasilPenilaianUmum.getChecklistPenilaianUmum().getId(),
									checklistHasilPenilaianUmum);
						}
					}
					checklistHasilPenilaianUmums = null;

					System.out.println("maps -> " + maps);

					JSONObject jsonObjectSub = new JSONObject();
					jsonObjectSub.put("jumlahChecklist", jumlahChecklist);
					Common.insertProperty(GrupChecklistPenilaianUmum.class,
							jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianUmum(), jsonObjectSub, "grup", 1);

					JSONArray dataAngketSub = new JSONArray();

					for (ChecklistPenilaianUmum checklistPenilaianUmum : checklistPenilaianUmums) {

						JSONObject jsonObjectSubData = new JSONObject();

						Common.insertProperty(ChecklistPenilaianUmum.class, checklistPenilaianUmum, jsonObjectSubData,
								"angket", 1);

						dataAngketSub.put(jsonObjectSubData);

						ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum = maps
								.get(checklistPenilaianUmum.getId());

						Integer nilai = checklistHasilPenilaianUmum == null ? 0
								: checklistHasilPenilaianUmum.getNilai();

						JSONObject jsonObjectAngket = new JSONObject();
						jsonObjectAngket.put("keterangan",
								checklistHasilPenilaianUmum == null ? "" : checklistHasilPenilaianUmum.getKeterangan());
						jsonObjectAngket.put("nilai", nilai);

						jsonObjectSub.put("angket_" + checklistPenilaianUmum.getId(), jsonObjectAngket);

					}

					jsonObjectSub.put("dataAngketSub", dataAngketSub);

					arrayAngket.put(jsonObjectSub);
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/AngketUtilUmumApi.java:456");
			}
		}
		return jsonObject;
	}
}
