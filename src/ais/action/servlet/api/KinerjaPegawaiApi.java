package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.lkp.KegiatanTugasJabatan;
import ais.database.model.lkp.RealisasiKerjaPegawai;
import ais.database.model.lkp.TargetKerjaPegawai;
import ais.database.model.rab.SatuanKerja;

public class KinerjaPegawaiApi {

	@SuppressWarnings("unchecked")
	public static JSONObject daftarTugasJabatan(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {

				if (tbmuser.getPegawai() == null) {
					jsonObject.put("status", "90");
					jsonObject.put("description", "Anda harus login sebagai pegawai");
				} else {
					Session session = HibernateUtil.openSession();
					try {

					String nama = request.isNull("cari") ? "" : request.get("cari") + "";
					List<Tbmrole> tbmroles = null;
					if (tbmuser.ambilPegawai() != null) {
						if (tbmroles == null || tbmroles.isEmpty()) {
							tbmroles = new ArrayList<Tbmrole>();
							tbmroles.add(tbmuser.hakAkses());
						}
					}

					SatuanKerja satuanKerja = tbmuser.ambilSatuanKerja();
					Criterion criterion = satuanKerja == null ? Restrictions.sqlRestriction("false")
							: Restrictions.eq("satuanKerja", satuanKerja);

					List<KegiatanTugasJabatan> kegiatanTugasJabatans = session
							.createCriteria(KegiatanTugasJabatan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.addOrder(Order.asc("nama"))
							.add(nama.trim().equals("") ? Restrictions.sqlRestriction("1=1")
									: Restrictions.ilike("nama", nama.trim(), MatchMode.ANYWHERE))
							.add(Restrictions.or(criterion, tbmroles == null || tbmroles.isEmpty()
									? Restrictions.sqlRestriction("false")
									: Restrictions.or(Restrictions.and(criterion, Restrictions.isNull("userRole")),
											Restrictions.in("userRole", tbmroles))))
							.setMaxResults(Common.MAX_RESULT_1000).list();

					JSONArray jsonArray = new JSONArray();
					for (KegiatanTugasJabatan kegiatanTugasJabatan : kegiatanTugasJabatans) {
						JSONObject jsonObjectKelas = new JSONObject();
						Common.insertProperty(KegiatanTugasJabatan.class, kegiatanTugasJabatan, jsonObjectKelas, "", 1);
						jsonArray.put(jsonObjectKelas);
					}

					jsonObject.put("data", jsonArray);
					jsonObject.put("status", "00");
					jsonObject.put("description", "OK");

					// session.disconnect();
					ApiHelperSupport.closeOpenedSession(session);
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/KinerjaPegawaiApi.java:97");
			}
		}
		return jsonObject;
	}

	public static JSONObject lanjutTugasJabatan(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				if (tbmuser.getPegawai() == null) {
					jsonObject.put("status", "90");
					jsonObject.put("description", "Anda harus login sebagai pegawai");
				} else {
					Session session = HibernateUtil.openSession();
					try {

					Integer tahun = request.isNull("tahun") ? Calendar.getInstance().get(Calendar.YEAR)
							: Integer.parseInt(request.get("tahun") + "");
					Integer bulan = request.isNull("bulan") ? Calendar.getInstance().get(Calendar.MONTH) + 1
							: Integer.parseInt(request.get("bulan") + "");
					Long tugasJabatan = request.isNull("tugasJabatan") ? null
							: Long.parseLong(request.get("tugasJabatan") + "");
					KegiatanTugasJabatan kegiatanTugasJabatan = tugasJabatan == null ? null
							: (KegiatanTugasJabatan) ConstantValues.ambil(KegiatanTugasJabatan.class.getName(),
									tugasJabatan);

					if (kegiatanTugasJabatan == null) {
						jsonObject.put("status", "90");
						jsonObject.put("description", "Tugas dan Jabatan harus dipilih");
					} else {

						TargetKerjaPegawai targetKerjaPegawai = ((TargetKerjaPegawai) session
								.createCriteria(TargetKerjaPegawai.class).add(Restrictions.eq("tahun", tahun))
								.add(Restrictions.eq("bulan", bulan))
								.add(Restrictions.eq("pegawai", tbmuser.getPegawai()))
								.add(Restrictions.eq("kegiatanTugasJabatan", kegiatanTugasJabatan)).uniqueResult());
						if (targetKerjaPegawai == null) {
							targetKerjaPegawai = new TargetKerjaPegawai();
							targetKerjaPegawai.setTahun(tahun);
							targetKerjaPegawai.setBulan(bulan);
							targetKerjaPegawai.setPegawai(tbmuser.getPegawai());
							targetKerjaPegawai.setKegiatanTugasJabatan(kegiatanTugasJabatan);
							session.getTransaction().begin();
							session.save(targetKerjaPegawai);
							session.getTransaction().commit();
						}
						JSONObject jsonObjectKelas = new JSONObject();
						Common.insertProperty(TargetKerjaPegawai.class, targetKerjaPegawai, jsonObjectKelas, "", 2,
								"pegawai");

						jsonObject.put("data", jsonObjectKelas);
						jsonObject.put("status", "00");
						jsonObject.put("description", "OK");
					}

					// session.disconnect();
					ApiHelperSupport.closeOpenedSession(session);
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/KinerjaPegawaiApi.java:170");
			}
		}
		return jsonObject;
	}

	public static JSONObject simpanTugasJabatan(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				if (tbmuser.getPegawai() == null) {
					jsonObject.put("status", "90");
					jsonObject.put("description", "Anda harus login sebagai pegawai");
				} else {
					Session session = HibernateUtil.openSession();
					try {
					List<String> warnings = new ArrayList<String>();
					RealisasiKerjaPegawai realisasiKerjaPegawai = (RealisasiKerjaPegawai) ElearningApiUtil
							.simpanData(RealisasiKerjaPegawai.class, session, request, tbmuser, warnings, req);
					JSONObject jsonObjectKelas = new JSONObject();
					Common.insertProperty(RealisasiKerjaPegawai.class, realisasiKerjaPegawai, jsonObjectKelas, "", 2,
							"pegawai");

					if (!warnings.isEmpty()) {
						String w = "";
						for (String ww : warnings) {
							w += w.isEmpty() ? ww : "\n" + ww;
						}
						jsonObject.put("status", "90");
						jsonObject.put("description", w);
					} else {
						jsonObject.put("data", jsonObjectKelas);
						jsonObject.put("status", "00");
						jsonObject.put("description", "OK");
					}

					// session.disconnect();
					ApiHelperSupport.closeOpenedSession(session);
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/KinerjaPegawaiApi.java:223");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject daftarCatatanTugasJabatan(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				if (tbmuser.getPegawai() == null) {
					jsonObject.put("status", "90");
					jsonObject.put("description", "Anda harus login sebagai pegawai");
				} else {
					Session session = HibernateUtil.openSession();
					try {

					String c = request.isNull("cari") ? "" : request.get("cari") + "";
					Integer max = request.isNull("max") ? 5 : Integer.parseInt(request.getString("max").trim());
					Integer halaman = request.isNull("halaman") ? 0
							: Integer.parseInt(request.getString("halaman").trim());

					Criteria criteria = session.createCriteria(RealisasiKerjaPegawai.class)
							.createAlias("targetKerjaPegawai", "targetKerjaPegawai")
							.add(tbmuser.getPegawai() == null
									|| (tbmuser.hakAkses() != null && tbmuser.hakAkses().getMelihatDataPegawaiLain())
											? Restrictions.sqlRestriction("true")
											: Restrictions.or(
													Restrictions.eq("targetKerjaPegawai.pegawai", tbmuser.getPegawai()),
													Restrictions.eq("pegawai", tbmuser.getPegawai())))
							.addOrder(Order.desc("id")).setMaxResults(max).setFirstResult(halaman * max);

					if (!c.trim().isEmpty()) {
						criteria.createAlias("targetKerjaPegawai.kegiatanTugasJabatan", "kegiatanTugasJabatan")
								.add(Restrictions.or(
										Restrictions.ilike("kegiatanTugasJabatan.nama", c.trim(), MatchMode.ANYWHERE),

										Restrictions.or(Restrictions.ilike("keterangan", c.trim(), MatchMode.ANYWHERE),
												Restrictions.ilike("catatan", c.trim(), MatchMode.ANYWHERE))));
					}

					List<RealisasiKerjaPegawai> realisasiKerjaPegawais = criteria.list();

					JSONArray jsonArray = new JSONArray();
					for (RealisasiKerjaPegawai realisasiKerjaPegawai : realisasiKerjaPegawais) {
						JSONObject jsonObjectKelas = new JSONObject();
						Common.insertProperty(RealisasiKerjaPegawai.class, realisasiKerjaPegawai, jsonObjectKelas, "",
								1, "pegawai");

						LampiranLain lampiranLain = LampiranLain.ambil(realisasiKerjaPegawai.getId(),
								RealisasiKerjaPegawai.class.getName());
						if (lampiranLain != null && lampiranLain.getId() != null) {
							jsonObjectKelas.put("lampiran", lampiranLain.createLinkUri(false));
						}

						jsonArray.put(jsonObjectKelas);
					}

					jsonObject.put("data", jsonArray);
					jsonObject.put("status", "00");
					jsonObject.put("description", "OK");

					// session.disconnect();
					ApiHelperSupport.closeOpenedSession(session);
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/KinerjaPegawaiApi.java:303");
			}
		}
		return jsonObject;
	}
}
