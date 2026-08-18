package ais.action.servlet.api;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.helper.BroadcastHelper;
import ais.action.master.surat.SuratKeluarAction;
import ais.action.master.surat.helper.DasboardSurat;
import ais.action.master.surat.util.SuratUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FotoGambarSuratKeluar;
import ais.database.model.file.FotoGambarSuratMasuk;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Pejabat;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.surat.AlurPersetujuanSuratKeluar;
import ais.database.model.surat.AlurPersetujuanSuratKeluarStatus;
import ais.database.model.surat.AlurPersetujuanSuratMasuk;
import ais.database.model.surat.AlurPersetujuanSuratMasukStatus;
import ais.database.model.surat.KlasifikasiSuratKeluar;
import ais.database.model.surat.KlasifikasiSuratKeluarParemeter;
import ais.database.model.surat.KlasifikasiSuratKeluarParemeterValue;
import ais.database.model.surat.KlasifikasiSuratKeluarUntuk;
import ais.database.model.surat.SuratKeluar;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.WaktuUtil;

public class SuratApi {

	public static JSONObject disposisi_simpan_surat_masuk(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Session session = HibernateUtil.openSession();
				try {

				Long alurId = request.isNull("suratId") ? -1L : Long.parseLong(request.get("suratId") + "");

				SuratMasuk suratMasuk = (SuratMasuk) session.createCriteria(SuratMasuk.class)
						.add(Restrictions.idEq(alurId)).uniqueResult();

				if (suratMasuk != null && suratMasuk.getAlurPersetujuanSuratMasuk() != null) {

					JSONArray lanjut = new JSONArray();

					JenisJabatan jenisJabatan = suratMasuk.getAlurPersetujuanSuratMasuk().getJenisJabatan();
					List<Pejabat> pejabats = ConstantValues.simpleList(session.createCriteria(Pejabat.class)

							.add(Restrictions.or(
									Restrictions.or(
											Restrictions.ilike("jenisPengguna",
													"," + tbmuser.hakAkses().getRoleId() + ",", MatchMode.ANYWHERE),
											Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
													MatchMode.ANYWHERE)),
									Restrictions.and(
											Restrictions.or(Restrictions.isNotNull("pegawai"),
													Restrictions.or(Restrictions.isNotNull("guru"),
															Restrictions.isNotNull("dosen"))),
											Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
													Restrictions.or(Restrictions.eq("dosen", tbmuser.getDosen()),
															Restrictions.eq("guru", tbmuser.getGuru()))))))

							.add(Restrictions.eq("jenisJabatan", jenisJabatan))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							Pejabat.class);

					if (pejabats.isEmpty()) {
						pejabats = ConstantValues
								.simpleList(
										session.createCriteria(Pejabat.class)
												.add(Restrictions.eq("jenisJabatan", jenisJabatan)).add(Restrictions.or(
														Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
										Pejabat.class);
					}

					for (Pejabat pejabat : pejabats) {

						AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
								.createCriteria(AlurPersetujuanSuratMasukStatus.class)
								.add(Restrictions.isNotNull("kodeUnik"))
								.add(Restrictions.eq("alurPersetujuanSuratMasuk",
										suratMasuk.getAlurPersetujuanSuratMasuk()))
								.add(Restrictions.eq("suratMasuk", suratMasuk)).add(Restrictions.eq("pejabat", pejabat))
								.add(Restrictions.eq("jenisJabatan", jenisJabatan)).setMaxResults(1).uniqueResult();

						if (alurPersetujuanSuratMasukStatus == null) {
							String kodeUnik = AlurPersetujuanSuratMasukStatus.kodeUnik(pejabat, suratMasuk,
									pejabat.getJenisJabatan(), tbmuser, tbmuser == null ? null : tbmuser.getMahasiswa(),
									tbmuser == null ? null : tbmuser.getSiswa());
							if (kodeUnik != null) {
								alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
										.createCriteria(AlurPersetujuanSuratMasukStatus.class)
										.add(Restrictions.isNotNull("kodeUnik"))
										.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
							}
						}

						if (alurPersetujuanSuratMasukStatus == null) {
							alurPersetujuanSuratMasukStatus = new AlurPersetujuanSuratMasukStatus();

							alurPersetujuanSuratMasukStatus.setKonseptor(tbmuser);
							alurPersetujuanSuratMasukStatus
									.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
							alurPersetujuanSuratMasukStatus.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

							alurPersetujuanSuratMasukStatus
									.setMasihLanjut(suratMasuk.getAlurPersetujuanSuratMasuk() != null
											&& suratMasuk.getAlurPersetujuanSuratMasuk().getJenisJabatan() != null
											&& suratMasuk.getAlurPersetujuanSuratMasuk().getJenisJabatan().getId()
													.equals(jenisJabatan.getId()));

							alurPersetujuanSuratMasukStatus
									.setAlurPersetujuanSuratMasuk(suratMasuk.getAlurPersetujuanSuratMasuk());
							alurPersetujuanSuratMasukStatus.setSuratMasuk(suratMasuk);
							alurPersetujuanSuratMasukStatus.setJenisJabatan(jenisJabatan);
							alurPersetujuanSuratMasukStatus.setPejabat(pejabat);

							session.getTransaction().begin();
							session.save(alurPersetujuanSuratMasukStatus);
							session.getTransaction().commit();

						}

						JSONObject jsonObjectLanjut = new JSONObject();
						Common.insertProperty(AlurPersetujuanSuratMasukStatus.class, alurPersetujuanSuratMasukStatus,
								jsonObjectLanjut, "", 1);
						lanjut.put(jsonObjectLanjut);

						try {
							BroadcastHelper.kirimEmailSuratMasuk(suratMasuk, alurPersetujuanSuratMasukStatus, tbmuser);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/SuratApi.java:162");
						}
					}

					jsonObject.put("lanjut", lanjut);
					jsonObject.put("status", "00");
					jsonObject.put("description", "OK");

				} else {
					jsonObject.put("status", "90");
					jsonObject.put("description", "Disposisi surat tidak ditemukan");
				}

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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/SuratApi.java:187");
			}
		}
		return jsonObject;
	}

	public static JSONObject disposisi_simpan_surat_keluar(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Session session = HibernateUtil.openSession();
				try {

				Long alurId = request.isNull("suratId") ? -1L : Long.parseLong(request.get("suratId") + "");

				SuratKeluar suratKeluar = (SuratKeluar) session.createCriteria(SuratKeluar.class)
						.add(Restrictions.idEq(alurId)).uniqueResult();

				if (suratKeluar != null && suratKeluar.getAlurPersetujuanSuratKeluar() != null) {

					JSONArray lanjut = new JSONArray();

					JenisJabatan jenisJabatan = suratKeluar.getAlurPersetujuanSuratKeluar().getJenisJabatan();
					List<Pejabat> pejabats = ConstantValues.simpleList(session.createCriteria(Pejabat.class)

							.add(Restrictions.or(
									Restrictions.or(
											Restrictions.ilike("jenisPengguna",
													"," + tbmuser.hakAkses().getRoleId() + ",", MatchMode.ANYWHERE),
											Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
													MatchMode.ANYWHERE)),
									Restrictions.and(
											Restrictions.or(Restrictions.isNotNull("pegawai"),
													Restrictions.or(Restrictions.isNotNull("guru"),
															Restrictions.isNotNull("dosen"))),
											Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
													Restrictions.or(Restrictions.eq("dosen", tbmuser.getDosen()),
															Restrictions.eq("guru", tbmuser.getGuru()))))))

							.add(Restrictions.eq("jenisJabatan", jenisJabatan))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							Pejabat.class);

					if (pejabats.isEmpty()) {
						pejabats = ConstantValues
								.simpleList(
										session.createCriteria(Pejabat.class)
												.add(Restrictions.eq("jenisJabatan", jenisJabatan)).add(Restrictions.or(
														Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
										Pejabat.class);
					}

					for (Pejabat pejabat : pejabats) {

						AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) session
								.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
								.add(Restrictions.isNotNull("kodeUnik"))
								.add(Restrictions.eq("alurPersetujuanSuratKeluar",
										suratKeluar.getAlurPersetujuanSuratKeluar()))
								.add(Restrictions.eq("suratKeluar", suratKeluar))
								.add(Restrictions.eq("pejabat", pejabat))
								.add(Restrictions.eq("jenisJabatan", jenisJabatan)).setMaxResults(1).uniqueResult();

						if (alurPersetujuanSuratKeluarStatus == null) {
							String kodeUnik = AlurPersetujuanSuratKeluarStatus.kodeUnik(pejabat, suratKeluar,
									jenisJabatan, tbmuser, tbmuser == null ? null : tbmuser.getMahasiswa(),
									tbmuser == null ? null : tbmuser.getSiswa());
							if (kodeUnik != null) {
								alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) session
										.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
										.add(Restrictions.isNotNull("kodeUnik"))
										.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
							}
						}

						if (alurPersetujuanSuratKeluarStatus == null) {
							alurPersetujuanSuratKeluarStatus = new AlurPersetujuanSuratKeluarStatus();

							alurPersetujuanSuratKeluarStatus.setKonseptor(tbmuser);
							alurPersetujuanSuratKeluarStatus
									.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
							alurPersetujuanSuratKeluarStatus.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

							alurPersetujuanSuratKeluarStatus
									.setMasihLanjut(suratKeluar.getAlurPersetujuanSuratKeluar() != null
											&& suratKeluar.getAlurPersetujuanSuratKeluar().getJenisJabatan() != null
											&& suratKeluar.getAlurPersetujuanSuratKeluar().getJenisJabatan().getId()
													.equals(jenisJabatan.getId()));

							alurPersetujuanSuratKeluarStatus
									.setAlurPersetujuanSuratKeluar(suratKeluar.getAlurPersetujuanSuratKeluar());
							alurPersetujuanSuratKeluarStatus.setSuratKeluar(suratKeluar);
							alurPersetujuanSuratKeluarStatus.setJenisJabatan(jenisJabatan);
							alurPersetujuanSuratKeluarStatus.setPejabat(pejabat);

							session.getTransaction().begin();
							session.save(alurPersetujuanSuratKeluarStatus);
							session.getTransaction().commit();

						}

						JSONObject jsonObjectLanjut = new JSONObject();
						Common.insertProperty(AlurPersetujuanSuratKeluarStatus.class, alurPersetujuanSuratKeluarStatus,
								jsonObjectLanjut, "", 1);
						lanjut.put(jsonObjectLanjut);

						try {
							BroadcastHelper.kirimEmailSuratKeluar(suratKeluar, alurPersetujuanSuratKeluarStatus,
									tbmuser);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/SuratApi.java:301");
						}

					}

					jsonObject.put("lanjut", lanjut);
					jsonObject.put("status", "00");
					jsonObject.put("description", "OK");

				} else {
					jsonObject.put("status", "90");
					jsonObject.put("description", "Disposisi surat tidak ditemukan");
				}

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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/SuratApi.java:327");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject disposisi_surat_keluar(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Session session = HibernateUtil.openSession();
				try {

				Long alurId = request.isNull("alurId") ? -1L : Long.parseLong(request.get("alurId") + "");
				String keterangan = request.isNull("keterangan") ? "" : request.get("keterangan") + "";

				AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) session
						.createCriteria(AlurPersetujuanSuratKeluarStatus.class).add(Restrictions.isNotNull("kodeUnik"))
						.add(Restrictions.idEq(alurId)).uniqueResult();

				if (alurPersetujuanSuratKeluarStatus != null) {

					Pejabat pejabatcurrent = (Pejabat) ConstantValues.simpleObject(session.createCriteria(Pejabat.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

							.add(Restrictions.or(
									Restrictions.or(
											Restrictions.ilike("jenisPengguna",
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

					boolean disetujui = request.isNull("disetujui") ? false
							: Boolean.parseBoolean(request.get("disetujui") + "");
					boolean ditolak = request.isNull("ditolak") ? false
							: Boolean.parseBoolean(request.get("ditolak") + "");

					if (disetujui)
						alurPersetujuanSuratKeluarStatus.setWaktuPersetujuan(WaktuUtil.getDate());
					if (ditolak)
						alurPersetujuanSuratKeluarStatus.setWaktuDitolak(WaktuUtil.getDate());
					alurPersetujuanSuratKeluarStatus.setDisetujui(disetujui);
					alurPersetujuanSuratKeluarStatus.setDitolak(ditolak);

					if (pejabatcurrent != null)
						alurPersetujuanSuratKeluarStatus.setPejabat(pejabatcurrent);
					alurPersetujuanSuratKeluarStatus.setKeterangan(keterangan);

					session.getTransaction().begin();
					Common.refreshUpdate(session, alurPersetujuanSuratKeluarStatus);
					session.getTransaction().commit();

					List<AlurPersetujuanSuratKeluar> alurPersetujuanSuratKeluarsNext = ConstantValues.simpleList(
							session.createCriteria(AlurPersetujuanSuratKeluar.class)
									.add(Restrictions.isNotNull("jenisJabatan"))
									.add(Restrictions.eq("parent",
											alurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar())),
							AlurPersetujuanSuratKeluar.class);

					JSONArray lanjut = new JSONArray();

					for (AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar : alurPersetujuanSuratKeluarsNext) {
						JenisJabatan jenisJabatan = alurPersetujuanSuratKeluar.getJenisJabatan();

						List<Pejabat> pejabats = ConstantValues.simpleList(session.createCriteria(Pejabat.class)

								.add(Restrictions.or(
										Restrictions.or(
												Restrictions.ilike("jenisPengguna",
														"," + tbmuser.hakAkses().getRoleId() + ",", MatchMode.ANYWHERE),
												Restrictions.ilike(
														"usernamePengguna", "," + tbmuser.getUserId() + ",",
														MatchMode.ANYWHERE)),
										Restrictions.and(
												Restrictions.or(
														Restrictions.isNotNull("pegawai"),
														Restrictions.or(
																Restrictions.isNotNull("guru"),
																Restrictions.isNotNull("dosen"))),
												Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
														Restrictions.or(Restrictions.eq("dosen", tbmuser.getDosen()),
																Restrictions.eq("guru", tbmuser.getGuru()))))))

								.add(Restrictions.eq("jenisJabatan", jenisJabatan))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
								Pejabat.class);

						if (pejabats.isEmpty()) {
							pejabats = ConstantValues.simpleList(
									session.createCriteria(Pejabat.class)
											.add(Restrictions.eq("jenisJabatan", jenisJabatan)).add(Restrictions
													.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
									Pejabat.class);
						}
						for (Pejabat pejabat : pejabats) {

							AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatusLocal = (AlurPersetujuanSuratKeluarStatus) session
									.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
									.add(Restrictions.isNotNull("kodeUnik"))
									.add(Restrictions.eq("alurPersetujuanSuratKeluar", alurPersetujuanSuratKeluar))
									.add(Restrictions.eq("suratKeluar",
											alurPersetujuanSuratKeluarStatus.getSuratKeluar()))
									.add(Restrictions.eq("jenisJabatan", jenisJabatan))
									.add(Restrictions.eq("pejabat", pejabat)).setMaxResults(1).uniqueResult();

							if (alurPersetujuanSuratKeluarStatusLocal == null) {
								String kodeUnik = AlurPersetujuanSuratKeluarStatus.kodeUnik(pejabat,
										alurPersetujuanSuratKeluarStatus.getSuratKeluar(), jenisJabatan, tbmuser,
										tbmuser == null ? null : tbmuser.getMahasiswa(),
										tbmuser == null ? null : tbmuser.getSiswa());
								if (kodeUnik != null) {
									alurPersetujuanSuratKeluarStatusLocal = (AlurPersetujuanSuratKeluarStatus) session
											.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
											.add(Restrictions.isNotNull("kodeUnik"))
											.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
								}
							}

							if (alurPersetujuanSuratKeluarStatusLocal == null && !ditolak) {
								alurPersetujuanSuratKeluarStatusLocal = new AlurPersetujuanSuratKeluarStatus();

								alurPersetujuanSuratKeluarStatusLocal.setKonseptor(tbmuser);
								alurPersetujuanSuratKeluarStatusLocal
										.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
								alurPersetujuanSuratKeluarStatusLocal
										.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

								alurPersetujuanSuratKeluarStatusLocal
										.setAlurPersetujuanSuratKeluar(alurPersetujuanSuratKeluar);
								alurPersetujuanSuratKeluarStatusLocal
										.setSuratKeluar(alurPersetujuanSuratKeluarStatus.getSuratKeluar());
								alurPersetujuanSuratKeluarStatusLocal.setJenisJabatan(jenisJabatan);
								alurPersetujuanSuratKeluarStatusLocal.setPejabat(pejabat);

								alurPersetujuanSuratKeluarStatusLocal.setMasihLanjut(true);

								session.getTransaction().begin();
								session.save(alurPersetujuanSuratKeluarStatusLocal);
								session.getTransaction().commit();
							}

							if (disetujui) {

								JSONObject jsonObjectLanjut = new JSONObject();
								Common.insertProperty(AlurPersetujuanSuratKeluarStatus.class,
										alurPersetujuanSuratKeluarStatusLocal, jsonObjectLanjut, "", 1);
								lanjut.put(jsonObjectLanjut);

								try {
									BroadcastHelper.kirimEmailSuratKeluar(
											alurPersetujuanSuratKeluarStatusLocal.getSuratKeluar(),
											alurPersetujuanSuratKeluarStatusLocal, tbmuser);
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/SuratApi.java:494");
								}

							} else if (ditolak && alurPersetujuanSuratKeluarStatusLocal != null) {
								session.getTransaction().begin();
								session.delete(alurPersetujuanSuratKeluarStatusLocal);
								session.getTransaction().commit();
							}
						}

					}

					if (alurPersetujuanSuratKeluarStatus.getDitolak()) {
						session.getTransaction().begin();
						DasboardSurat.tolak(session, alurPersetujuanSuratKeluarStatus);
						session.getTransaction().commit();
					}

					JSONObject status = new JSONObject();
					Common.insertProperty(AlurPersetujuanSuratKeluarStatus.class, alurPersetujuanSuratKeluarStatus,
							status, "", 1);

					jsonObject.put("disposisi", status);
					jsonObject.put("lanjut", lanjut);
					jsonObject.put("status", "00");
					jsonObject.put("description", "OK");

				} else {
					jsonObject.put("status", "90");
					jsonObject.put("description", "Disposisi surat tidak ditemukan");
				}

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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/SuratApi.java:538");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject disposisi_surat_masuk(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Session session = HibernateUtil.openSession();
				try {

				Long alurId = request.isNull("alurId") ? -1L : Long.parseLong(request.get("alurId") + "");
				String keterangan = request.isNull("keterangan") ? "" : request.get("keterangan") + "";

				AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
						.createCriteria(AlurPersetujuanSuratMasukStatus.class).add(Restrictions.isNotNull("kodeUnik"))
						.add(Restrictions.idEq(alurId)).uniqueResult();

				if (alurPersetujuanSuratMasukStatus != null) {

					Pejabat pejabatcurrent = (Pejabat) ConstantValues.simpleObject(session.createCriteria(Pejabat.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

							.add(Restrictions.or(
									Restrictions.or(
											Restrictions.ilike("jenisPengguna",
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

					boolean disetujui = request.isNull("disetujui") ? false
							: Boolean.parseBoolean(request.get("disetujui") + "");
					boolean ditolak = request.isNull("ditolak") ? false
							: Boolean.parseBoolean(request.get("ditolak") + "");

					if (disetujui)
						alurPersetujuanSuratMasukStatus.setWaktuPersetujuan(WaktuUtil.getDate());
//					if (ditolak)
//						alurPersetujuanSuratMasukStatus.setWaktuDitolak(WaktuUtil.getDate());
					alurPersetujuanSuratMasukStatus.setDisetujui(disetujui);
					alurPersetujuanSuratMasukStatus.setDitolak(ditolak);

					if (pejabatcurrent != null)
						alurPersetujuanSuratMasukStatus.setPejabat(pejabatcurrent);
					alurPersetujuanSuratMasukStatus.setKeterangan(keterangan);

					session.getTransaction().begin();
					Common.refreshUpdate(session, alurPersetujuanSuratMasukStatus);
					session.getTransaction().commit();

					List<AlurPersetujuanSuratMasuk> alurPersetujuanSuratMasuksNext = ConstantValues.simpleList(
							session.createCriteria(AlurPersetujuanSuratMasuk.class)
									.add(Restrictions.isNotNull("jenisJabatan"))
									.add(Restrictions.eq("parent",
											alurPersetujuanSuratMasukStatus.getAlurPersetujuanSuratMasuk())),
							AlurPersetujuanSuratMasuk.class);

					JSONArray lanjut = new JSONArray();

					for (AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk : alurPersetujuanSuratMasuksNext) {
						JenisJabatan jenisJabatan = alurPersetujuanSuratMasuk.getJenisJabatan();

						List<Pejabat> pejabats = ConstantValues.simpleList(session.createCriteria(Pejabat.class)

								.add(Restrictions.or(
										Restrictions.or(
												Restrictions.ilike("jenisPengguna",
														"," + tbmuser.hakAkses().getRoleId() + ",", MatchMode.ANYWHERE),
												Restrictions.ilike(
														"usernamePengguna", "," + tbmuser.getUserId() + ",",
														MatchMode.ANYWHERE)),
										Restrictions.and(
												Restrictions.or(
														Restrictions.isNotNull("pegawai"),
														Restrictions.or(
																Restrictions.isNotNull("guru"),
																Restrictions.isNotNull("dosen"))),
												Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
														Restrictions.or(Restrictions.eq("dosen", tbmuser.getDosen()),
																Restrictions.eq("guru", tbmuser.getGuru()))))))

								.add(Restrictions.eq("jenisJabatan", jenisJabatan))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
								Pejabat.class);

						if (pejabats.isEmpty()) {
							pejabats =

									ConstantValues.simpleList(session.createCriteria(Pejabat.class)
											.add(Restrictions.eq("jenisJabatan", jenisJabatan)).add(Restrictions
													.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
											Pejabat.class);
						}
						for (Pejabat pejabat : pejabats) {

							AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatusLocal = (AlurPersetujuanSuratMasukStatus) session
									.createCriteria(AlurPersetujuanSuratMasukStatus.class)
									.add(Restrictions.isNotNull("kodeUnik"))
									.add(Restrictions.eq("alurPersetujuanSuratMasuk", alurPersetujuanSuratMasuk))
									.add(Restrictions.eq("suratMasuk", alurPersetujuanSuratMasukStatus.getSuratMasuk()))
									.add(Restrictions.eq("jenisJabatan", jenisJabatan))
									.add(Restrictions.eq("pejabat", pejabat)).setMaxResults(1).uniqueResult();

							if (alurPersetujuanSuratMasukStatusLocal == null) {
								String kodeUnik = AlurPersetujuanSuratMasukStatus.kodeUnik(pejabat,
										alurPersetujuanSuratMasukStatus.getSuratMasuk(), pejabat.getJenisJabatan(),
										tbmuser, tbmuser == null ? null : tbmuser.getMahasiswa(),
										tbmuser == null ? null : tbmuser.getSiswa());
								if (kodeUnik != null) {
									alurPersetujuanSuratMasukStatusLocal = (AlurPersetujuanSuratMasukStatus) session
											.createCriteria(AlurPersetujuanSuratMasukStatus.class)
											.add(Restrictions.isNotNull("kodeUnik"))
											.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
								}
							}

							if (alurPersetujuanSuratMasukStatusLocal == null && !ditolak) {
								alurPersetujuanSuratMasukStatusLocal = new AlurPersetujuanSuratMasukStatus();

								alurPersetujuanSuratMasukStatusLocal.setKonseptor(tbmuser);
								alurPersetujuanSuratMasukStatusLocal
										.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
								alurPersetujuanSuratMasukStatusLocal
										.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

								alurPersetujuanSuratMasukStatusLocal
										.setAlurPersetujuanSuratMasuk(alurPersetujuanSuratMasuk);
								alurPersetujuanSuratMasukStatusLocal
										.setSuratMasuk(alurPersetujuanSuratMasukStatus.getSuratMasuk());
								alurPersetujuanSuratMasukStatusLocal.setJenisJabatan(jenisJabatan);
								alurPersetujuanSuratMasukStatusLocal.setPejabat(pejabat);

								alurPersetujuanSuratMasukStatusLocal.setMasihLanjut(true);

								session.getTransaction().begin();
								session.save(alurPersetujuanSuratMasukStatusLocal);
								session.getTransaction().commit();
							}
							if (disetujui) {
								JSONObject jsonObjectLanjut = new JSONObject();
								Common.insertProperty(AlurPersetujuanSuratMasukStatus.class,
										alurPersetujuanSuratMasukStatusLocal, jsonObjectLanjut, "", 1);
								lanjut.put(jsonObjectLanjut);

								try {
									BroadcastHelper.kirimEmailSuratMasuk(
											alurPersetujuanSuratMasukStatusLocal.getSuratMasuk(),
											alurPersetujuanSuratMasukStatusLocal, tbmuser);
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/SuratApi.java:703");
								}

							} else if (ditolak && alurPersetujuanSuratMasukStatusLocal != null) {
								session.getTransaction().begin();
								session.delete(alurPersetujuanSuratMasukStatusLocal);
								session.getTransaction().commit();
							}
						}

					}

					if (alurPersetujuanSuratMasukStatus.getDitolak()) {
						session.getTransaction().begin();
						DasboardSurat.tolak(session, alurPersetujuanSuratMasukStatus);
						session.getTransaction().commit();
					}

					JSONObject status = new JSONObject();
					Common.insertProperty(AlurPersetujuanSuratMasukStatus.class, alurPersetujuanSuratMasukStatus,
							status, "", 1);

					jsonObject.put("disposisi", status);
					jsonObject.put("lanjut", lanjut);
					jsonObject.put("status", "00");
					jsonObject.put("description", "OK");

				} else {
					jsonObject.put("status", "90");
					jsonObject.put("description", "Disposisi surat tidak ditemukan");
				}

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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/SuratApi.java:747");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject klasifikasi(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Session session = HibernateUtil.openSession();
				try {

				String kode = request.isNull("kode") ? "" : request.getString("kode").trim();
				String nama = request.isNull("nama") ? "" : request.getString("nama").trim();
				String nomor = request.isNull("nomor") ? "" : request.getString("nomor").trim();

				Jurusan jurusan = tbmuser.getMahasiswa() != null ? tbmuser.getMahasiswa().getJurusan()
						: tbmuser.ambilJurusan();
				Fakultas fakultas = tbmuser.getMahasiswa() != null ? tbmuser.getMahasiswa().getJurusan().getFakultas()
						: tbmuser.ambilFakultas();

				Sekolah sekolah = tbmuser.ambilSekolah();
				Yayasan yayasan = tbmuser.ambilYayasan();

				Criterion criterion1 = Restrictions.or(
						fakultas == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("fakultas", fakultas),
						Restrictions.isNull("fakultas"));
				Criterion criterion2 = Restrictions.or(
						jurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", jurusan),
						Restrictions.isNull("jurusan"));

				Criterion criterion3 = Restrictions.or(
						sekolah == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("sekolah", sekolah),
						Restrictions.isNull("sekolah"));
				Criterion criterion4 = Restrictions.or(
						yayasan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("yayasan", yayasan),
						Restrictions.isNull("yayasan"));

				Criterion criterion = Restrictions.and(criterion1, criterion2);
				Criterion criterion11 = Restrictions.and(criterion3, criterion4);

				criterion = Restrictions.and(criterion, criterion11);

				KlasifikasiSuratKeluarUntuk umum = (KlasifikasiSuratKeluarUntuk) ConstantValues.simpleObject(
						session.createCriteria(KlasifikasiSuratKeluarUntuk.class)
								.add(Restrictions.ilike("nama", "Umum", MatchMode.EXACT)).setMaxResults(1),
						KlasifikasiSuratKeluarUntuk.class);
				if (tbmuser.getMahasiswa() != null) {
					List<KlasifikasiSuratKeluarUntuk> klasifikasiSuratKeluarUntuk = ConstantValues
							.simpleList(session.createCriteria(KlasifikasiSuratKeluarUntuk.class)
									.add(Restrictions.or(Restrictions.isNotNull("statusMahasiswa"), Restrictions.or(

											Restrictions.ilike("nama", "Mahasiswa", MatchMode.EXACT),
											Restrictions.isNotNull("statusAwalMahasiswa"))

									)), KlasifikasiSuratKeluarUntuk.class);
					criterion = Restrictions.and(criterion,
							klasifikasiSuratKeluarUntuk.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.in("klasifikasiSuratKeluarUntuk", klasifikasiSuratKeluarUntuk));
				}

				if (tbmuser.getSiswa() != null) {
					List<KlasifikasiSuratKeluarUntuk> klasifikasiSuratKeluarUntuk = ConstantValues.simpleList(
							session.createCriteria(KlasifikasiSuratKeluarUntuk.class)
									.add(Restrictions.ilike("nama", "Siswa", MatchMode.EXACT)),
							KlasifikasiSuratKeluarUntuk.class);
					criterion = Restrictions.and(criterion,
							klasifikasiSuratKeluarUntuk.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.in("klasifikasiSuratKeluarUntuk", klasifikasiSuratKeluarUntuk));
				}

				if (tbmuser.ambilDosen() != null) {
					KlasifikasiSuratKeluarUntuk klasifikasiSuratKeluarUntuk = (KlasifikasiSuratKeluarUntuk) ConstantValues
							.simpleObject(
									session.createCriteria(KlasifikasiSuratKeluarUntuk.class)
											.add(Restrictions.ilike("nama", "Dosen", MatchMode.EXACT)).setMaxResults(1),
									KlasifikasiSuratKeluarUntuk.class);

					criterion = Restrictions.and(criterion,
							Restrictions.or(Restrictions.eq("klasifikasiSuratKeluarUntuk", umum),
									Restrictions.eq("klasifikasiSuratKeluarUntuk", klasifikasiSuratKeluarUntuk)));
				}

				if (tbmuser.ambilGuru() != null) {
					KlasifikasiSuratKeluarUntuk klasifikasiSuratKeluarUntuk = (KlasifikasiSuratKeluarUntuk) ConstantValues
							.simpleObject(
									session.createCriteria(KlasifikasiSuratKeluarUntuk.class)
											.add(Restrictions.ilike("nama", "Guru", MatchMode.EXACT)).setMaxResults(1),
									KlasifikasiSuratKeluarUntuk.class);

					criterion = Restrictions.and(criterion,
							Restrictions.or(Restrictions.eq("klasifikasiSuratKeluarUntuk", umum),
									Restrictions.eq("klasifikasiSuratKeluarUntuk", klasifikasiSuratKeluarUntuk)));
				}

				criterion = Restrictions.and(criterion,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

				String sql = "(this_.kode_grup_pengguna is null or trim(this_.kode_grup_pengguna)='' or '"
						+ tbmuser.hakAkses().getRoleId() + "' = ANY(string_to_array(this_.kode_grup_pengguna,';')) )";
				criterion = Restrictions.and(criterion, Restrictions.sqlRestriction(sql));

				List<KlasifikasiSuratKeluar> klasifikasiSuratKeluars =

						ConstantValues.simpleList(session.createCriteria(KlasifikasiSuratKeluar.class)

								.createAlias("nomorSurat", "nomorSurat", Criteria.LEFT_JOIN).addOrder(Order.asc("nama"))
								.add(nomor.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
										: Restrictions.ilike("nomorSurat.contohFormat", nomor.trim(),
												MatchMode.ANYWHERE))
								.add(nama.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
										: Restrictions.ilike("nama", nama.trim(), MatchMode.ANYWHERE))
								.add(kode.trim().equals("") ? Restrictions.sqlRestriction("1=1")
										: Restrictions.ilike("kode", kode.trim(), MatchMode.ANYWHERE))

								.add(criterion).setMaxResults(Common.MAX_RESULT_500), KlasifikasiSuratKeluar.class);

				JSONArray data = new JSONArray();

				for (KlasifikasiSuratKeluar klasifikasiSuratKeluar : klasifikasiSuratKeluars) {
					JSONObject json = new JSONObject();

					Common.insertProperty(KlasifikasiSuratKeluar.class, klasifikasiSuratKeluar, json, "", 1);

					data.put(json);
				}

				jsonObject.put("data", data);
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/SuratApi.java:896");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject parameter(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Session session = HibernateUtil.openSession();
				try {

				SuratKeluar suratKeluar = (SuratKeluar) (request.isNull("id")
						|| !Common.isNumber(request.get("id").toString())
								? null
								: session.createCriteria(SuratKeluar.class)
										.add(Restrictions.idEq(Long.parseLong(request.get("id").toString())))
										.uniqueResult());

				Object id_klasifikasi = request.isNull("id_klasifikasi") ? null : request.get("id_klasifikasi");

				KlasifikasiSuratKeluar klasifikasiSuratKeluar = suratKeluar != null
						&& suratKeluar.getKlasifikasiSuratKeluar() != null
								? suratKeluar.getKlasifikasiSuratKeluar()
								: ((KlasifikasiSuratKeluar) (id_klasifikasi == null ? null
										: session.createCriteria(KlasifikasiSuratKeluar.class)
												.add(Restrictions.idEq(Long.parseLong(id_klasifikasi.toString())))
												.uniqueResult()));

				System.out.println(
						"id_klasifikasi -> " + id_klasifikasi + ", " + klasifikasiSuratKeluar + " " + suratKeluar);

				if (klasifikasiSuratKeluar != null) {
					List<KlasifikasiSuratKeluarParemeter> klasifikasiSuratKeluarParemeters = ConstantValues.simpleList(
							session.createCriteria(KlasifikasiSuratKeluarParemeter.class)
									.addOrder(Order.asc("nomorUrut"))
									.add(Restrictions.eq("klasifikasiSuratKeluar", klasifikasiSuratKeluar)),
							KlasifikasiSuratKeluarParemeter.class);

					JSONArray data = new JSONArray();

					for (KlasifikasiSuratKeluarParemeter klasifikasiSuratKeluarParemeter : klasifikasiSuratKeluarParemeters) {

						KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = null;
						if (suratKeluar != null && suratKeluar.getId() != null) {
							klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
									.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
									.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
											klasifikasiSuratKeluarParemeter))
									.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1).uniqueResult();
						}

						String val = klasifikasiSuratKeluarParemeterValue == null
								? klasifikasiSuratKeluarParemeter.getNilai()
								: klasifikasiSuratKeluarParemeterValue.getNama();

						JSONObject object = new JSONObject();
						object.put("key", klasifikasiSuratKeluarParemeter.getKey());
						object.put("nilai", val);
						object.put("tampil", klasifikasiSuratKeluarParemeter.getTampil());
						object.put("id", klasifikasiSuratKeluarParemeter.getId());
						object.put("type", klasifikasiSuratKeluarParemeter.getTipe());
						data.put(object);
					}

					jsonObject.put("data", data);
					jsonObject.put("status", "00");
					jsonObject.put("description", "OK");
				} else {
					jsonObject.put("status", "90");
					jsonObject.put("description", "Klasifikasi Surat Keluar tidak ditemukan");
				}

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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/SuratApi.java:987");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject updateParameter(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Session session = HibernateUtil.openSession();
				try {

				SuratKeluar suratKeluar = (SuratKeluar) (request.isNull("id")
						|| !Common.isNumber(request.get("id").toString())
								? null
								: session.createCriteria(SuratKeluar.class)
										.add(Restrictions.idEq(Long.parseLong(request.get("id").toString())))
										.uniqueResult());
				if (suratKeluar != null) {

					Object id_klasifikasi = request.isNull("id_klasifikasi") ? null : request.get("id_klasifikasi");

					KlasifikasiSuratKeluar klasifikasiSuratKeluar = suratKeluar != null
							&& suratKeluar.getKlasifikasiSuratKeluar() != null
									? suratKeluar.getKlasifikasiSuratKeluar()
									: ((KlasifikasiSuratKeluar) (id_klasifikasi == null ? null
											: session.createCriteria(KlasifikasiSuratKeluar.class)
													.add(Restrictions.idEq(Long.parseLong(id_klasifikasi.toString())))
													.uniqueResult()));

					System.out.println(
							"id_klasifikasi -> " + id_klasifikasi + ", " + klasifikasiSuratKeluar + " " + suratKeluar);

					if (klasifikasiSuratKeluar != null) {
						List<KlasifikasiSuratKeluarParemeter> klasifikasiSuratKeluarParemeters = ConstantValues
								.simpleList(
										session.createCriteria(KlasifikasiSuratKeluarParemeter.class)
												.addOrder(Order.asc("nomorUrut"))
												.add(Restrictions.eq("klasifikasiSuratKeluar", klasifikasiSuratKeluar)),
										KlasifikasiSuratKeluarParemeter.class);

						JSONArray data = new JSONArray();

						for (KlasifikasiSuratKeluarParemeter klasifikasiSuratKeluarParemeter : klasifikasiSuratKeluarParemeters) {
							KlasifikasiSuratKeluarParemeterValue klasifikasiSuratKeluarParemeterValue = null;
							if (suratKeluar != null && suratKeluar.getId() != null) {
								klasifikasiSuratKeluarParemeterValue = (KlasifikasiSuratKeluarParemeterValue) session
										.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
										.add(Restrictions.eq("klasifikasiSuratKeluarParemeter",
												klasifikasiSuratKeluarParemeter))
										.add(Restrictions.eq("suratKeluar", suratKeluar)).setMaxResults(1)
										.uniqueResult();
							}
							if (!request.isNull(klasifikasiSuratKeluarParemeter.getKey())) {

								if (klasifikasiSuratKeluarParemeterValue == null) {
									klasifikasiSuratKeluarParemeterValue = new KlasifikasiSuratKeluarParemeterValue();
									klasifikasiSuratKeluarParemeterValue
											.setKlasifikasiSuratKeluarParemeter(klasifikasiSuratKeluarParemeter);
									klasifikasiSuratKeluarParemeterValue.setSuratKeluar(suratKeluar);

								}

								klasifikasiSuratKeluarParemeterValue
										.setNama(request.get(klasifikasiSuratKeluarParemeter.getKey()) + "");

								session.getTransaction().begin();
								session.saveOrUpdate(klasifikasiSuratKeluarParemeterValue);
								session.getTransaction().commit();
							}

							String val = klasifikasiSuratKeluarParemeterValue == null
									? klasifikasiSuratKeluarParemeter.getNilai()
									: klasifikasiSuratKeluarParemeterValue.getNama();

							JSONObject object = new JSONObject();
							object.put("key", klasifikasiSuratKeluarParemeter.getKey());
							object.put("nilai", val);
							object.put("tampil", klasifikasiSuratKeluarParemeter.getTampil());
							object.put("id", klasifikasiSuratKeluarParemeter.getId());
							object.put("type", klasifikasiSuratKeluarParemeter.getTipe());
							data.put(object);
						}

						jsonObject.put("data", data);
						jsonObject.put("status", "00");
						jsonObject.put("description", "OK");
					} else {
						jsonObject.put("status", "90");
						jsonObject.put("description", "Klasifikasi Surat Keluar tidak ditemukan");
					}
				} else {
					jsonObject.put("status", "90");
					jsonObject.put("description", "Surat tidak ditemukan");
				}

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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/SuratApi.java:1101");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject info(HttpServletRequest req, JSONObject request) {

		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Session session = HibernateUtil.openSession();
				try {

				SuratKeluar suratKeluar = (SuratKeluar) (request.isNull("id")
						|| !Common.isNumber(request.get("id").toString())
								? null
								: session.createCriteria(SuratKeluar.class)
										.add(Restrictions.idEq(Long.parseLong(request.get("id").toString())))
										.uniqueResult());
				if (suratKeluar != null) {

					String html = "";
					List<AlurPersetujuanSuratKeluarStatus> alurPersetujuanSuratKeluarStatuss = session
							.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
							.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("suratKeluar", suratKeluar))
							.addOrder(Order.asc("id")).list();
					for (AlurPersetujuanSuratKeluarStatus myAlurPersetujuanSuratKeluarStatus : alurPersetujuanSuratKeluarStatuss) {
						if (myAlurPersetujuanSuratKeluarStatus.getJenisJabatan() == null
								&& myAlurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar() != null
								&& myAlurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar()
										.getJenisJabatan() != null) {
							html += "<li>" + myAlurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar() + " : "
									+ (myAlurPersetujuanSuratKeluarStatus.getDisetujui()
											? ("<font style=\"font-size: x-small;color:blue;font-weight: bolder;\">Sudah ditindak-lanjuti "
													+ (myAlurPersetujuanSuratKeluarStatus.getPejabat() == null
															|| myAlurPersetujuanSuratKeluarStatus.getPejabat()
																	.getPegawai() == null
																			? (myAlurPersetujuanSuratKeluarStatus
																					.getPejabat().getDosen() == null
																							? ""
																							: " " + myAlurPersetujuanSuratKeluarStatus
																									.getPejabat()
																									.getDosen()
																									.getNama())
																			: " " + myAlurPersetujuanSuratKeluarStatus
																					.getPejabat().getPegawai()
																					.getNama())
													+ (myAlurPersetujuanSuratKeluarStatus.getWaktuPersetujuan() == null
															? ""
															: " pada waktu " + Common.dateFormat3.get()
																	.format(myAlurPersetujuanSuratKeluarStatus
																			.getWaktuPersetujuan()))
													+ "</font>")
											: myAlurPersetujuanSuratKeluarStatus.getDitolak()
													? ("<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Ditolak "
															+ (myAlurPersetujuanSuratKeluarStatus.getPejabat() == null
																	|| myAlurPersetujuanSuratKeluarStatus.getPejabat()
																			.getPegawai() == null
																					? (myAlurPersetujuanSuratKeluarStatus
																							.getPejabat()
																							.getDosen() == null
																									? ""
																									: " " + myAlurPersetujuanSuratKeluarStatus
																											.getPejabat()
																											.getDosen()
																											.getNama())
																					: " " + myAlurPersetujuanSuratKeluarStatus
																							.getPejabat().getPegawai()
																							.getNama())
															+ (myAlurPersetujuanSuratKeluarStatus
																	.getWaktuDitolak() == null
																			? ""
																			: " pada waktu " + Common.dateFormat3.get()
																					.format(myAlurPersetujuanSuratKeluarStatus
																							.getWaktuDitolak()))
															+ "</font>")
													: "<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Belum ditindak lanjuti"
															+ (myAlurPersetujuanSuratKeluarStatus.getPejabat() == null
																	? ""
																	: " " + myAlurPersetujuanSuratKeluarStatus
																			.getPejabat().getNama())
															+ "</font>")
									+ "</li>";
						} else if (myAlurPersetujuanSuratKeluarStatus.getJenisJabatan() != null) {
							html += "<li>" + myAlurPersetujuanSuratKeluarStatus.getJenisJabatan().getNama() + " : "
									+ (myAlurPersetujuanSuratKeluarStatus.getDisetujui()
											? ("<font style=\"font-size: x-small;color:blue;font-weight: bolder;\">Sudah ditindak-lanjuti "
													+ (myAlurPersetujuanSuratKeluarStatus.getPejabat() == null
															|| myAlurPersetujuanSuratKeluarStatus.getPejabat()
																	.getPegawai() == null
																			? (myAlurPersetujuanSuratKeluarStatus
																					.getPejabat().getDosen() == null
																							? ""
																							: " " + myAlurPersetujuanSuratKeluarStatus
																									.getPejabat()
																									.getDosen()
																									.getNama())
																			: " " + myAlurPersetujuanSuratKeluarStatus
																					.getPejabat().getPegawai()
																					.getNama())
													+ (myAlurPersetujuanSuratKeluarStatus.getWaktuPersetujuan() == null
															? ""
															: " pada waktu " + Common.dateFormat3.get()
																	.format(myAlurPersetujuanSuratKeluarStatus
																			.getWaktuPersetujuan()))
													+ "</font>")
											: myAlurPersetujuanSuratKeluarStatus.getDitolak()
													? ("<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Ditolak "
															+ (myAlurPersetujuanSuratKeluarStatus.getPejabat() == null
																	|| myAlurPersetujuanSuratKeluarStatus.getPejabat()
																			.getPegawai() == null
																					? (myAlurPersetujuanSuratKeluarStatus
																							.getPejabat()
																							.getDosen() == null
																									? ""
																									: " " + myAlurPersetujuanSuratKeluarStatus
																											.getPejabat()
																											.getDosen()
																											.getNama())
																					: " " + myAlurPersetujuanSuratKeluarStatus
																							.getPejabat().getPegawai()
																							.getNama())
															+ (myAlurPersetujuanSuratKeluarStatus
																	.getWaktuDitolak() == null
																			? ""
																			: " pada waktu " + Common.dateFormat3.get()
																					.format(myAlurPersetujuanSuratKeluarStatus
																							.getWaktuDitolak()))
															+ "</font>")
													: "<font style=\"font-size: x-small;color:red;font-weight: bolder;\">Belum ditindak lanjuti"
															+ (myAlurPersetujuanSuratKeluarStatus.getPejabat() == null
																	? ""
																	: " " + myAlurPersetujuanSuratKeluarStatus
																			.getPejabat().getNama())
															+ "</font>")
									+ "</li>";
						}
					}

					JSONObject data = new JSONObject();
					Common.insertProperty(SuratKeluar.class, suratKeluar, data, "", 1);
					jsonObject.put("info", html);
					jsonObject.put("data", data);
					jsonObject.put("status", "00");
					jsonObject.put("description", "OK");
				} else {
					jsonObject.put("status", "90");
					jsonObject.put("description", "Surat tidak ditemukan");
				}

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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/SuratApi.java:1269");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static File cetakSurat(String idS, Tbmuser tbmuser) throws Exception {

		try {
			if (idS.startsWith("EE")) {
				idS = Common.desEncrypter.get().decrypt(idS.substring(2));
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/SuratApi.java:1283");
		}

		System.out.println("myid = " + idS);

		File file = null;
		Session session = HibernateUtil.openSession();
		try {
		try {
			SuratKeluar suratKeluar = (SuratKeluar) session.createCriteria(SuratKeluar.class)
					.add(Restrictions.idEq(Long.parseLong(idS))).uniqueResult();

			LampiranLain lainMahasiswa = LampiranLain.ambil(suratKeluar.getKlasifikasiSuratKeluar().getId(),
					LampiranLain.FILE_JRXML_LAYOUT_SURAT);

			if (lainMahasiswa != null && lainMahasiswa.getId() != null) {

				if (tbmuser == null || tbmuser.getUserId() == null) {
					tbmuser = suratKeluar.getKonseptor();
				}
				if (tbmuser == null && suratKeluar.getMahasiswa() != null) {
					tbmuser = new Tbmuser(suratKeluar.getMahasiswa());
				}
				if (tbmuser == null && suratKeluar.getSiswa() != null) {
					tbmuser = new Tbmuser(suratKeluar.getSiswa());
				}
				if (tbmuser == null && suratKeluar.getGuru() != null) {
					tbmuser = new Tbmuser(suratKeluar.getGuru());
				}
				if (tbmuser == null && suratKeluar.getDosen() != null) {
					tbmuser = new Tbmuser(suratKeluar.getDosen());
				}

				Map<String, Object> parameters = SuratUtil.ubahIsiSuratKeluar(suratKeluar, tbmuser, null);

				AlurPersetujuanSuratKeluarStatus disposisiTerakhir = (AlurPersetujuanSuratKeluarStatus) session
						.createCriteria(AlurPersetujuanSuratKeluarStatus.class).add(Restrictions.isNotNull("kodeUnik"))
						.add(Restrictions.isNotNull("suratKeluar")).add(Restrictions.eq("suratKeluar", suratKeluar))
						.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

				if (disposisiTerakhir != null) {

					File f = SuratKeluarAction.cetakDisposisi(parameters, disposisiTerakhir, false, tbmuser);
					PDFMergerUtility ut = new PDFMergerUtility();
					ut.addSource(f);

					file = new File(f.getParentFile().getAbsolutePath() + "/" + Common.getGeneratedBarCode() + ".pdf");

					for (int index = 1; index <= 15; index++) {
						try {
							LampiranLain lampiranLain = LampiranLain.ambil(
									suratKeluar.getKlasifikasiSuratKeluar().getId(),
									LampiranLain.FILE_JRXML_LAYOUT_SURAT + (index == 1 ? "" : "_" + index));
							if (lampiranLain != null && lampiranLain.getId() != null) {
								File f2 = Report.generateCompileFileReport(Report.PDF, parameters,
										lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());

								ut.addSource(f2);
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/SuratApi.java:1343");
						}
					}

					ut.setDestinationStream(new FileOutputStream(file));
					ut.mergeDocuments();

				} else {
					boolean ada = false;
					PDFMergerUtility ut = new PDFMergerUtility();
					for (int index = 1; index <= 15; index++) {
						try {
							LampiranLain lampiranLain = LampiranLain.ambil(
									suratKeluar.getKlasifikasiSuratKeluar().getId(),
									LampiranLain.FILE_JRXML_LAYOUT_SURAT + (index == 1 ? "" : "_" + index));
							if (lampiranLain != null && lampiranLain.getId() != null) {
								File f2 = Report.generateCompileFileReport(Report.PDF, parameters,
										lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());

								ut.addSource(f2);
								ada = true;
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/SuratApi.java:1366");
						}
					}

					if (ada) {
						file = new File(Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
						ut.setDestinationStream(new FileOutputStream(file));
						ut.mergeDocuments();
					}
				}
			}

			// session.disconnect();
			ApiHelperSupport.closeOpenedSession(session);

			session = StreamingHibernateUtil.getInstance().openSession();
			List<FotoGambarSuratKeluar> fotoGambarSuratKeluars = suratKeluar == null || suratKeluar.getId() == null
					? new ArrayList<FotoGambarSuratKeluar>()
					: session.createCriteria(FotoGambarSuratKeluar.class)
							.add(Restrictions.eq("suratKeluar", suratKeluar.getId())).addOrder(Order.desc("id")).list();
			if (!fotoGambarSuratKeluars.isEmpty()) {
				File fileBaru = new File(
						file.getParentFile().getAbsolutePath() + "/" + Common.getGeneratedBarCode() + ".pdf");
				PDFMergerUtility ut = new PDFMergerUtility();

				ut.addSource(file);

				for (FotoGambarSuratKeluar fotoGambarSuratKeluar : fotoGambarSuratKeluars) {
					File filea = fotoGambarSuratKeluar.ambilFile();
					if (filea != null && filea.exists() && filea.getName().toLowerCase().endsWith("pdf")) {
						ut.addSource(filea);
					}
				}
				ut.setDestinationStream(new FileOutputStream(fileBaru));
				ut.mergeDocuments();

				file = fileBaru;
			}
			StreamingHibernateUtil.getInstance().closeSession();

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/SuratApi.java:1407");
		}

		return file;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static JSONObject cetak(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Session session = HibernateUtil.openSession();
				try {

				SuratKeluar suratKeluar = (SuratKeluar) (request.isNull("id")
						|| !Common.isNumber(request.get("id").toString())
								? null
								: session.createCriteria(SuratKeluar.class)
										.add(Restrictions.idEq(Long.parseLong(request.get("id").toString())))
										.uniqueResult());
				if (suratKeluar != null) {

					if (tbmuser.getMahasiswa() != null)
						suratKeluar.setMahasiswa(tbmuser.getMahasiswa());

					if (tbmuser.getDosen() != null)
						suratKeluar.setDosen(tbmuser.getDosen());

					if (tbmuser.getGuru() != null)
						suratKeluar.setGuru(tbmuser.getGuru());

					if (tbmuser.getSiswa() != null)
						suratKeluar.setSiswa(tbmuser.getSiswa());

					if (tbmuser.getPegawai() != null)
						suratKeluar.setPegawai(tbmuser.getPegawai());

					Map<String, Object> parameters = SuratUtil.ubahIsiSuratKeluar(suratKeluar, tbmuser, null);

					LampiranLain lainMahasiswa = LampiranLain.ambil(suratKeluar.getKlasifikasiSuratKeluar().getId(),
							LampiranLain.FILE_JRXML_LAYOUT_SURAT);
					if (lainMahasiswa != null) {

						AlurPersetujuanSuratKeluarStatus disposisiTerakhir = (AlurPersetujuanSuratKeluarStatus) session
								.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
								.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.isNotNull("suratKeluar"))
								.add(Restrictions.eq("suratKeluar", suratKeluar)).addOrder(Order.desc("id"))
								.setMaxResults(1).uniqueResult();
						File file = null;
						if (disposisiTerakhir != null) {
							File f = SuratKeluarAction.cetakDisposisi(parameters, disposisiTerakhir, false, tbmuser);

							PDFMergerUtility ut = new PDFMergerUtility();
							ut.addSource(f);

							file = new File(
									f.getParentFile().getAbsolutePath() + "/" + Common.getGeneratedBarCode() + ".pdf");

							for (int index = 1; index <= 15; index++) {
								try {
									LampiranLain lampiranLain = LampiranLain.ambil(
											suratKeluar.getKlasifikasiSuratKeluar().getId(),
											LampiranLain.FILE_JRXML_LAYOUT_SURAT + (index == 1 ? "" : "_" + index));
									if (lampiranLain != null && lampiranLain.getId() != null) {
										File f2 = Report.generateCompileFileReport(Report.PDF, parameters,
												lainMahasiswa.ambilFile().getAbsolutePath(),
												ais.ui.util.WaktuUtil.getDate());

										ut.addSource(f2);
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/SuratApi.java:1484");
								}
							}

							ut.setDestinationStream(new FileOutputStream(file));
							ut.mergeDocuments();

						} else {

							boolean ada = false;
							PDFMergerUtility ut = new PDFMergerUtility();
							for (int index = 1; index <= 15; index++) {
								try {
									LampiranLain lampiranLain = LampiranLain.ambil(
											suratKeluar.getKlasifikasiSuratKeluar().getId(),
											LampiranLain.FILE_JRXML_LAYOUT_SURAT + (index == 1 ? "" : "_" + index));
									if (lampiranLain != null && lampiranLain.getId() != null) {
										File f2 = Report.generateCompileFileReport(Report.PDF, parameters,
												lainMahasiswa.ambilFile().getAbsolutePath(),
												ais.ui.util.WaktuUtil.getDate());

										ut.addSource(f2);
										ada = true;
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/SuratApi.java:1509");
								}
							}

							if (ada) {
								file = new File(
										Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
								ut.setDestinationStream(new FileOutputStream(file));
								ut.mergeDocuments();
							}
						}

						if (file == null) {
							jsonObject.put("status", "97");
							jsonObject.put("description", "File laporan tidak bisa di cetak");
						} else {
							String path = !Common.pakaiDirReportTergabung()
									? Common.CURRENT_URL + "/report/" + URLEncoder.encode(file.getName(), "UTF-8")
									: Common.CURRENT_URL + "/pdf?p="
											+ URLEncoder.encode(Common.desEncrypter.get().encrypt(file.getName()), "UTF-8");
							;
							jsonObject.put("url", path);
							jsonObject.put("status", "00");
							jsonObject.put("description", "OK");
						}

						jsonObject.put("status", "00");
						jsonObject.put("description", "OK");
					} else {
						jsonObject.put("status", "90");
						jsonObject.put("description", "Template Surat tidak ditemukan");
					}
				} else {
					jsonObject.put("status", "90");
					jsonObject.put("description", "Surat tidak ditemukan");
				}

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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/SuratApi.java:1558");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject lampiranSuratKeluar(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Session session = HibernateUtil.openSession();
				try {

				SuratKeluar suratKeluar = (SuratKeluar) (request.isNull("id")
						|| !Common.isNumber(request.get("id").toString())
								? null
								: session.createCriteria(SuratKeluar.class)
										.add(Restrictions.idEq(Long.parseLong(request.get("id").toString())))
										.uniqueResult());
				if (suratKeluar != null) {

					JSONArray jsonArray = new JSONArray();
					Session sessionFile = StreamingHibernateUtil.getInstance().openSession();
					try {
					List<FotoGambarSuratKeluar> fotoGambarSuratKeluars = suratKeluar == null
							|| suratKeluar.getId() == null
									? new ArrayList<FotoGambarSuratKeluar>()
									: sessionFile.createCriteria(FotoGambarSuratKeluar.class)
											.add(Restrictions.eq("suratKeluar", suratKeluar.getId()))
											.addOrder(Order.desc("id")).list();

					for (FotoGambarSuratKeluar fotoGambarSuratKeluar : fotoGambarSuratKeluars) {
						try {
							JSONObject fileJsonObject = new JSONObject();
							String nama = fotoGambarSuratKeluar.getNama();
							String url = "";
							if (fotoGambarSuratKeluar.getGdrive() != null
									&& !fotoGambarSuratKeluar.getGdrive().isEmpty()) {
								String contentVideo = fotoGambarSuratKeluar.getGdrive();
								url = "https://drive.google.com/file/d/" + contentVideo + "/preview";
							} else {
								File file = ((FileFoto) fotoGambarSuratKeluar).ambilFile();
								url = LampiranLain.ambilLinkLampiranLain(file);
							}
							fileJsonObject.put("id", fotoGambarSuratKeluar.getId());
							fileJsonObject.put("nama", nama);
							fileJsonObject.put("url", url);

							jsonArray.put(fileJsonObject);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/SuratApi.java:1613");
						}
					}
					try { sessionFile.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/SuratApi.java:1616");}
					sessionFile.disconnect();
					sessionFile.close();
				} finally {
					HibernateUtil.closeSessionQuietly(sessionFile);
				}
					StreamingHibernateUtil.getInstance().closeSession();
					jsonObject.put("data", jsonArray);
					jsonObject.put("status", "00");
					jsonObject.put("description", "OK");
				} else {

					jsonObject.put("status", "90");
					jsonObject.put("description", "Surat tidak ditemukan");

				}
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/SuratApi.java:1644");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject lampiranSuratMasuk(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Session session = HibernateUtil.openSession();
				try {

				SuratMasuk suratMasuk = (SuratMasuk) (request.isNull("id")
						|| !Common.isNumber(request.get("id").toString())
								? null
								: session.createCriteria(SuratMasuk.class)
										.add(Restrictions.idEq(Long.parseLong(request.get("id").toString())))
										.uniqueResult());
				if (suratMasuk != null) {

					JSONArray jsonArray = new JSONArray();
					Session sessionFile = StreamingHibernateUtil.getInstance().openSession();
					try {
					List<FotoGambarSuratMasuk> fotoGambarSuratMasuks = suratMasuk == null || suratMasuk.getId() == null
							? new ArrayList<FotoGambarSuratMasuk>()
							: sessionFile.createCriteria(FotoGambarSuratMasuk.class)
									.add(Restrictions.eq("suratMasuk", suratMasuk.getId())).addOrder(Order.desc("id"))
									.list();

					for (FotoGambarSuratMasuk fotoGambarSuratMasuk : fotoGambarSuratMasuks) {
						try {
							JSONObject fileJsonObject = new JSONObject();
							String nama = fotoGambarSuratMasuk.getNama();
							String url = "";
							if (fotoGambarSuratMasuk.getGdrive() != null
									&& !fotoGambarSuratMasuk.getGdrive().isEmpty()) {
								String contentVideo = fotoGambarSuratMasuk.getGdrive();
								url = "https://drive.google.com/file/d/" + contentVideo + "/preview";
							} else {
								File file = ((FileFoto) fotoGambarSuratMasuk).ambilFile();
								url = LampiranLain.ambilLinkLampiranLain(file);
							}
							fileJsonObject.put("id", fotoGambarSuratMasuk.getId());
							fileJsonObject.put("nama", nama);
							fileJsonObject.put("url", url);

							jsonArray.put(fileJsonObject);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/SuratApi.java:1698");
						}
					}
					try { sessionFile.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/SuratApi.java:1701");}
					sessionFile.disconnect();
					sessionFile.close();
				} finally {
					HibernateUtil.closeSessionQuietly(sessionFile);
				}
					StreamingHibernateUtil.getInstance().closeSession();
					jsonObject.put("data", jsonArray);
					jsonObject.put("status", "00");
					jsonObject.put("description", "OK");
				} else {

					jsonObject.put("status", "90");
					jsonObject.put("description", "Surat tidak ditemukan");

				}
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/SuratApi.java:1729");
			}
		}
		return jsonObject;
	}

}
