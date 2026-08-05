package ais.action.master.rab.util;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.AcaraHasTransaksi;
import ais.database.model.akunting.Transaksi;
import ais.database.model.rab.Acara;
import ais.database.model.rab.JenisInformasiRab;
import ais.database.model.rab.Proyek;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Tugas;
import ais.database.model.rab.TugasPunyaPegawai;
import ais.database.model.rab.Workspace;
import ais.database.model.rab.WorkspacePunyaIndikator;
import ais.database.model.rab.WorkspacePunyaJenisParameter;
import ais.database.model.rab.WorkspacePunyaPegawai;
import ais.database.model.rab.WorkspacePunyaSasaran;
import ais.ui.util.MyMessageboxConfig;
import org.hibernate.EntityMode;

public class RabUtil {

	public static final Integer DEFAULT_REVISI = -1;
	public static final Integer DEFAULT_SATUAN_KERJA = 99999;

	public static JenisInformasiRab INFORMASI;
	public static JenisInformasiRab PENGUMUMAN;
	public static JenisInformasiRab PERINGATAN;

	static {
		Session session = HibernateUtil.getSessionFactory().openSession();

		try {
			PENGUMUMAN = (JenisInformasiRab) session.createCriteria(JenisInformasiRab.class)
					.add(Restrictions.eq("nama", "Pengumuman")).setMaxResults(1).uniqueResult();
			if (PENGUMUMAN == null) {
				PENGUMUMAN = new JenisInformasiRab();
				PENGUMUMAN.setKeterangan("Pengumuman");
				PENGUMUMAN.setNama("Pengumuman");
				session.getTransaction().begin();
				session.save(PENGUMUMAN);
				session.getTransaction().commit();
			}

			INFORMASI = (JenisInformasiRab) session.createCriteria(JenisInformasiRab.class)
					.add(Restrictions.eq("nama", "Informasi")).setMaxResults(1).uniqueResult();
			if (INFORMASI == null) {
				INFORMASI = new JenisInformasiRab();
				INFORMASI.setKeterangan("Informasi");
				INFORMASI.setNama("Informasi");
				session.getTransaction().begin();
				session.save(INFORMASI);
				session.getTransaction().commit();
			}

			PERINGATAN = (JenisInformasiRab) session.createCriteria(JenisInformasiRab.class)
					.add(Restrictions.eq("nama", "Peringatan")).setMaxResults(1).uniqueResult();
			if (PERINGATAN == null) {
				PERINGATAN = new JenisInformasiRab();
				PERINGATAN.setKeterangan("Peringatan");
				PERINGATAN.setNama("Peringatan");
				session.getTransaction().begin();
				session.save(PERINGATAN);
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/RabUtil.java:85");
		} finally {
			try {
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/util/RabUtil.java:89");
				// TODO: handle exception
			}
			try {
				session.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/util/RabUtil.java:94");
				// TODO: handle exception
			}
		}

	}

	public static void ubahSemuaStatus(Integer tahunWorkspace, Integer revisi, SatuanKerja satuanKerja,
			SumberDana sumberDana) {
		if (satuanKerja == null || sumberDana == null) {
			return;
		}
		Session session = HibernateUtil.currentSession();
		Integer maxrevisi = (Integer) session.createCriteria(Workspace.class)
				.add(Restrictions.or(Restrictions.eq("carryOver", true),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.eq("satuanKerja", satuanKerja))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.eq("tahunWorkspace", tahunWorkspace)).setProjection(Projections.max("revisi"))
				.uniqueResult();
		maxrevisi = maxrevisi == null ? 1 : maxrevisi;

		Integer count = ((Number) session.createSQLQuery("select count(*) from rab.workspace where parent_id = id;")
				.uniqueResult()).intValue();
		if (!count.equals(0)) {
			session.createSQLQuery("delete from rab.workspace where parent_id = id;").executeUpdate();
		}

		count = ((Number) session.createCriteria(Workspace.class)
				.add(Restrictions.or(Restrictions.eq("carryOver", true),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.eq("tahunWorkspace", tahunWorkspace))
				.add(Restrictions.sqlRestriction(
						"revisi = " + revisi + " and (aktif is null or aktif != " + (maxrevisi.equals(revisi)) + ")"))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		if (!count.equals(0)) {
			try {
				Session mySession = HibernateUtil.currentNativeSession();
				mySession.getTransaction().begin();
				String sql = "update rab.workspace set aktif = " + (maxrevisi.equals(revisi)) + " where satuan_kerja = "
						+ satuanKerja.getId() + (sumberDana == null ? "" : " and sumber_dana = " + sumberDana.getId())
						+ " and tahun_workspace = " + tahunWorkspace + " and revisi = " + revisi
						+ " and (aktif is null or aktif != " + (maxrevisi.equals(revisi)) + ");";
				// System.out.println("Ubah status sql = " + sql);
				mySession.createSQLQuery(sql).executeUpdate();

				sql = "update rab.workspace set aktif = false where satuan_kerja = " + satuanKerja.getId()
						+ (sumberDana == null ? "" : " and sumber_dana = " + sumberDana.getId())
						+ " and tahun_workspace = " + tahunWorkspace + " and revisi != -1 and revisi != " + maxrevisi
						+ " and (aktif is null or aktif = true);";
				// System.out.println("Ubah status sql = " + sql);
				mySession.createSQLQuery(sql).executeUpdate();

				sql = "update rab.workspace set aktif = true where revisi = -1 and (aktif is null or aktif = false);";
				mySession.createSQLQuery(sql).executeUpdate();

				sql = "update rab.workspace set revisi = 1 where id > 0 and revisi = -1;";
				mySession.createSQLQuery(sql).executeUpdate();
				mySession.getTransaction().commit();

				HibernateUtil.closeSession();
			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}

		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Serializable[] getDetailWorkspace(Class clazz, String alias, String[] properti, Workspace workspace) {
		Session session = HibernateUtil.currentSession();

		String isi = "";
		Integer size = 0;
		if (properti.length > 1) {

			ProjectionList projectionList = Projections.projectionList();
			for (String p : properti) {
				projectionList.add(Projections.property(p));
			}

			List<Object[]> strings = (session.createCriteria(clazz).createAlias(alias, "a1")
					.setProjection(projectionList).add(Restrictions.eq("workspace", workspace)).list());
			size = strings.size();
			for (Object[] s : strings) {
				String sub = "";
				for (Object c : s) {
					sub += "[" + (c == null ? "" : c) + "]";
				}

				isi += isi.equals("") ? sub : ", " + sub;
			}
		} else if (properti.length == 1) {

			List<String> strings = (session.createCriteria(clazz).createAlias(alias, "a1")
					.setProjection(Projections.property(properti[0])).add(Restrictions.eq("workspace", workspace))
					.list());
			size = strings.size();
			for (String s : strings) {
				s = "[" + s + "]";
				isi += isi.equals("") ? s : ", " + s;
			}
		}
		Serializable[] serializable = new Serializable[] { size, isi };
		return serializable;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Serializable[] getDetailTugas(Class clazz, String alias, String[] properti, Tugas tugas) {
		Session session = HibernateUtil.currentSession();

		String isi = "";
		Integer size = 0;
		if (properti.length > 1) {

			ProjectionList projectionList = Projections.projectionList();
			for (String p : properti) {
				projectionList.add(Projections.property(p));
			}

			List<Object[]> strings = (session.createCriteria(clazz).createAlias(alias, "a1")
					.setProjection(projectionList).add(Restrictions.eq("tugas", tugas)).list());
			size = strings.size();
			for (Object[] s : strings) {
				String sub = "";
				for (Object c : s) {
					sub += "[" + (c == null ? "" : c) + "]";
				}

				isi += isi.equals("") ? sub : ", " + sub;
			}
		} else if (properti.length == 1) {

			List<String> strings = (session.createCriteria(clazz).createAlias(alias, "a1")
					.setProjection(Projections.property(properti[0])).add(Restrictions.eq("tugas", tugas)).list());
			size = strings.size();
			for (String s : strings) {
				s = "[" + s + "]";
				isi += isi.equals("") ? s : ", " + s;
			}
		}
		Serializable[] serializable = new Serializable[] { size, isi };
		return serializable;
	}

	public static void setDefaultPegawai(AmbilDataPegawaiBanbox bandbox) {
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilPegawai() != null) {
			Pegawai pegawai = tbmuser.ambilPegawai();
			bandbox.setAttribute("pegawai", tbmuser.ambilPegawai());
			bandbox.setAttribute("myValue", tbmuser.ambilPegawai());
			bandbox.setValue(pegawai == null ? "" : (pegawai.getCode() + " - " + pegawai.getNama()));
			bandbox.setDisabled(true);
		} else if (tbmuser != null && tbmuser.ambilDosen() != null
				&& (tbmuser.hakAkses().getRoleName().toLowerCase().contains("dosen"))) {
			Pegawai pegawai = Pegawai.createDataPegawaiDariDosen(tbmuser.ambilDosen());
			bandbox.setAttribute("pegawai", pegawai);
			bandbox.setAttribute("myValue", pegawai);
			bandbox.setValue(pegawai == null ? "" : (pegawai.getCode() + " - " + pegawai.getNama()));
			bandbox.setDisabled(true);
		} else if (tbmuser != null && tbmuser.ambilGuru() != null
				&& (tbmuser.hakAkses().getRoleName().toLowerCase().contains("guru"))) {
			Pegawai pegawai = Pegawai.createDataPegawaiDariGuru(tbmuser.ambilGuru());
			bandbox.setAttribute("pegawai", pegawai);
			bandbox.setAttribute("myValue", pegawai);
			bandbox.setValue(pegawai == null ? "" : (pegawai.getCode() + " - " + pegawai.getNama()));
			bandbox.setDisabled(true);
		}
	}

	// public static void bersihkanSimpanTransaksi() {
	//
	// Session session = HibernateUtil.currentSession();
	//
	// Integer count = ((Number) session.createCriteria(Transaksi.class)
	// .add(Restrictions.eq("simpan", false))
	// .setProjection(Projections.rowCount()).uniqueResult())
	// .intValue();
	//
	// if (!count.equals(0)) {
	// session.createSQLQuery(
	// "delete from akunting.transaksi where simpan = false")
	// .executeUpdate();
	// }
	// }

//	public static GrupTransaksi checkSimpanWorkspace1(Transaksi transaksi, Workspace workspace) {
//		return checkSimpanWorkspace1(transaksi, workspace, null, null, 0);
//	}
//
//	public static GrupTransaksi checkSimpanWorkspace1(Transaksi transaksi, Workspace workspace,
//			SatuanKerja satuanKerja, SumberDana sumberDana, int coint) {
//		GrupTransaksi grupTransaksi = null;
//		try {
//			System.out.println(
//					"satuanKerja = " + satuanKerja + ", sumberDana = " + sumberDana + ",workspace = " + workspace);
//
//			Session mySession = HibernateUtil.currentSession();
//			if (satuanKerja != null && sumberDana != null && (workspace == null || workspace.getId() == null)) {
//				workspace = (Workspace) mySession.createCriteria(Workspace.class).add(Restrictions.or(Restrictions.eq("carryOver", true),Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
//						.add(Restrictions.eq("copyForm", transaksi.getId()))
//						.add(Restrictions.eq("satuanKerja", satuanKerja)).add(Restrictions.eq("sumberDana", sumberDana))
//						.add(Restrictions.eq("parentId", Long.MAX_VALUE))
//						.add(Restrictions.eq("tahunWorkspace", transaksi.getTahun())).setMaxResults(1).uniqueResult();
//
//				System.out.println("workspace = " + workspace);
//				if (workspace == null || !workspace.getAktif()) {
//					workspace = new Workspace();
//					workspace.setAkun(transaksi.getAkun());
//					workspace.setCopyForm(transaksi.getId());
//					workspace.setDeep(null);
//					workspace.setDurasi(0);
//					workspace.setHargaSatuan(0.0);
//					workspace.setHargaTotal(0.0);
//					workspace.setJenisWorkspace(null);
//					workspace.setJmlDipakai(0L);
//					workspace.setJmlWaktu(0.0);
//					workspace.setKeterangan("");
//					workspace.setKode("");
//					workspace.setLeaf(true);
//					workspace.setMerupakanHasilCopy(false);
//					workspace.setMulai(null);
//					workspace.setNama("");
//					workspace.setParentId(Long.MAX_VALUE);
//					workspace.setPersenKomplit(0.0);
//					workspace.setQty(0.0);
//					workspace.setRealisasiTotal(0.0);
//					workspace.setRevisi(-2);
//					workspace.setSatuan(null);
//					workspace.setSatuan1(null);
//					workspace.setSatuanKerja(satuanKerja);
//					workspace.setSatuanVolume("");
//					workspace.setSelesai(null);
//					workspace.setSumberDana(sumberDana);
//					workspace.setTahunWorkspace(transaksi.getTahun());
//					workspace.setUnitOrganisasi(null);
//					workspace.setVolume(0.0);
//					mySession.save(workspace);
//				}
//				System.out.println("workspace = " + workspace);
//
//				grupTransaksi = transaksi.getGrupTransaksi();
//
//				grupTransaksi.setWorkspace(workspace);
//				Common.refreshUpdate(mySession, grupTransaksi);
//			} else if (workspace == null || workspace.getId() == null || transaksi == null
//					|| transaksi.getId() == null) {
//				return null;
//			}
//
//			Integer count = ((Number) mySession.createCriteria(GrupTransaksi.class)
//					.add(Restrictions.eq("transaksi", transaksi)).add(Restrictions.eq("workspace", workspace))
//					.setProjection(Projections.rowCount()).setMaxResults(1).uniqueResult()).intValue();
//			if (count.equals(0)) {
//				grupTransaksi = new GrupTransaksi();
//				grupTransaksi.setWorkspace(workspace);
//				mySession.save(grupTransaksi);
//			}
//			// else {
//			// // initParents(grupTransaksi, workspace);
//			// mySession.update(mySession.merge(grupTransaksi));
//			// }
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/util/RabUtil.java:362");
//			Common.tampilErrorJikaAdmin(e); 
//			try {
//				// Session session = HibernateUtil.currentSession();
//				// session.saveOrUpdate(transaksi);
//				Thread.sleep(1000);
//			} catch (Exception e1) {
//				e1.printStackTrace();
//			}
//			if (coint < 3) {
//				checkSimpanWorkspace1(transaksi, workspace, satuanKerja, sumberDana, ++coint);
//			}
//		}
//		return grupTransaksi;
//	}

	// public static void initParents(
	// GrupTransaksi grupTransaksi, Workspace workspace) {
	// ClassMetadata classMetadata = HibernateUtil
	// .getClassMetadata(GrupTransaksi.class);
	// List<Workspace> workspaces = new ArrayList<Workspace>();
	// getParentSet(workspace, workspaces);
	// for (int i = 0; i < workspaces.size(); i++) {
	// classMetadata.setPropertyValue(grupTransaksi, "parent" + i, // workspaces.get(i), EntityMode.POJO);
	// }
	// }

	public static void getParentSet(Workspace workspace, List<Workspace> workspaces) {
		Session session = HibernateUtil.currentSession();
		if (workspace.getParentId() != null) {
			Workspace parentWorkspace = (Workspace) session.createCriteria(Workspace.class)
					.add(Restrictions.or(Restrictions.eq("carryOver", true),
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.idEq(workspace.getParentId())).uniqueResult();
			if (parentWorkspace != null) {
				workspaces.add(parentWorkspace);
				getParentSet(parentWorkspace, workspaces);
			}
		}

	}

	public static AcaraHasTransaksi checkSimpanAcara(Transaksi transaksi, Acara acara) {
		if (acara == null || acara.getId() == null) {
			return null;
		}
		Session session = HibernateUtil.currentSession();
		AcaraHasTransaksi acaraHasTransaksi = (AcaraHasTransaksi) session.createCriteria(AcaraHasTransaksi.class)
				.add(Restrictions.eq("transaksi", transaksi)).add(Restrictions.eq("acara", acara)).setMaxResults(1)
				.uniqueResult();
		if (acaraHasTransaksi == null) {
			acaraHasTransaksi = new AcaraHasTransaksi();
			acaraHasTransaksi.setTransaksi(transaksi);
			acaraHasTransaksi.setAcara(acara);
			session.save(acaraHasTransaksi);
		}
		return acaraHasTransaksi;
	}

//	@SuppressWarnings("unchecked")
//	public static void pindahkanRealisasiKeRevisiBaru(Integer tahun, final Integer oldRevisi,
//			final Integer newRevisi, SatuanKerja satuanKerja, SumberDana sumberDana) {
//		Session session = HibernateUtil.currentSession();
//		List<GrupTransaksi> grupTransaksis = session.createCriteria(GrupTransaksi.class)
//				.createAlias("workspace", "workspace", Criteria.INNER_JOIN)
//				.add(Restrictions.eq("workspace.satuanKerja", satuanKerja))
//				.add(Restrictions.eq("workspace.sumberDana", sumberDana))
//				.add(Restrictions.eq("workspace.tahunWorkspace", tahun))
//				.add(Restrictions.eq("workspace.revisi", oldRevisi)).list();
//
//		for (GrupTransaksi grupTransaksi : grupTransaksis) {
//
//			Transaksi transaksi = grupTransaksi.getTransaksi();
//			Workspace newWorkspace = (Workspace) session.createCriteria(Workspace.class).add(Restrictions.or(Restrictions.eq("carryOver", true),Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
//					.add(Restrictions.eq("copyForm", grupTransaksi.getWorkspace().getId())).setMaxResults(1)
//					.uniqueResult();
//			if (newWorkspace == null) {
//				continue;
//			}
//
//			GrupTransaksi newGrupTransaksi = (GrupTransaksi) session.createCriteria(GrupTransaksi.class)
//					.add(Restrictions.eq("transaksi", transaksi))
//					.createAlias("workspace", "workspace", Criteria.INNER_JOIN)
//					.add(Restrictions.eq("workspace.satuanKerja", satuanKerja))
//					.add(Restrictions.eq("workspace.sumberDana", sumberDana))
//					.add(Restrictions.eq("workspace.tahunWorkspace", tahun))
//					.add(Restrictions.eq("workspace.revisi", newRevisi)).setMaxResults(1).uniqueResult();
//
//			if (newGrupTransaksi == null) {
//				newGrupTransaksi = new GrupTransaksi();
//				newGrupTransaksi.setTransaksi(transaksi);
//				newGrupTransaksi.setWorkspace(newWorkspace);
//				// initParents(newGrupTransaksi, newWorkspace);
//				session.save(newGrupTransaksi);
//			} else {
//				// initParents(newGrupTransaksi, newWorkspace);
//				Common.refreshUpdate(session, (newGrupTransaksi));
//			}
//		}
//	}

	@SuppressWarnings("unchecked")
	public static void pindahkanAcaraKeRevisiBaru(Integer tahun, final Integer oldRevisi, final Integer newRevisi,
			SatuanKerja satuanKerja, SumberDana sumberDana) {
		Session session = HibernateUtil.currentSession();
		List<AcaraHasTransaksi> acaraHasTransaksis = session.createCriteria(AcaraHasTransaksi.class)
				.createAlias("acara", "acara", Criteria.INNER_JOIN)
				.createAlias("acara.workspace", "workspace", Criteria.INNER_JOIN)
				.add(Restrictions.eq("workspace.satuanKerja", satuanKerja))
				.add(Restrictions.eq("workspace.sumberDana", sumberDana))
				.add(Restrictions.eq("workspace.tahunWorkspace", tahun))
				.add(Restrictions.eq("workspace.revisi", newRevisi)).list();

		for (AcaraHasTransaksi acaraHasTransaksi : acaraHasTransaksis) {

			Transaksi transaksi = acaraHasTransaksi.getTransaksi();
			Acara newAcara = (Acara) session.createCriteria(Acara.class)
					.add(Restrictions.eq("copyForm", acaraHasTransaksi.getAcara().getId())).setMaxResults(1)
					.uniqueResult();

			AcaraHasTransaksi newAcaraHasTransaksi = (AcaraHasTransaksi) session.createCriteria(AcaraHasTransaksi.class)
					.add(Restrictions.eq("transaksi", transaksi)).createAlias("acara", "acara", Criteria.INNER_JOIN)
					.createAlias("acara.workspace", "workspace", Criteria.INNER_JOIN)
					.add(Restrictions.eq("workspace.satuanKerja", satuanKerja))
					.add(Restrictions.eq("workspace.sumberDana", sumberDana))
					.add(Restrictions.eq("workspace.tahunWorkspace", tahun))
					.add(Restrictions.eq("workspace.revisi", newRevisi)).setMaxResults(1).uniqueResult();

			if (newAcaraHasTransaksi == null) {
				newAcaraHasTransaksi = new AcaraHasTransaksi();
				newAcaraHasTransaksi.setTransaksi(transaksi);
				newAcaraHasTransaksi.setAcara(newAcara);
				session.save(newAcaraHasTransaksi);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void checkForChildsCopy(Workspace workspace, WorkspaceTreeModel workspaceTreeModel,
			Integer tahunTujuan, final Integer newRevisi, SatuanKerja satuanKerja, SumberDana sumberDana,
			SatuanKerja satuanKerjaTujuan, SumberDana sumberDanaTujuan) {

		if (workspace.getMerupakanHasilCopy() != null && workspace.getMerupakanHasilCopy()) {
			Session session = HibernateUtil.currentSession();
			Workspace copyFrom = (Workspace) session.createCriteria(Workspace.class)
					.add(Restrictions.or(Restrictions.eq("carryOver", true),
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.idEq(workspace.getCopyForm())).uniqueResult();

			int childCount = workspaceTreeModel.getChildCount(copyFrom);

			if (childCount != 0) {
				List<Workspace> workspaces = workspaceTreeModel.getChildren(copyFrom);

				for (Workspace myWorkspace : workspaces) {

					Workspace copyWorkspace = (Workspace) myWorkspace.clone();
					copyWorkspace.setDeep(null);
					copyWorkspace.setSatuanKerja(satuanKerjaTujuan);
					copyWorkspace.setSumberDana(sumberDanaTujuan);
					copyWorkspace.setCopyForm(myWorkspace.getId());
					copyWorkspace.setMerupakanHasilCopy(true);
					copyWorkspace.setParentId(workspace.getId());
					copyWorkspace.setTahunWorkspace(tahunTujuan);
					copyWorkspace.setRevisi(newRevisi);
					copyWorkspace.setId(null);
					session.save(copyWorkspace);

					List<WorkspacePunyaPegawai> workspacePunyaPegawais = session
							.createCriteria(WorkspacePunyaPegawai.class).add(Restrictions.eq("workspace", myWorkspace))
							.list();
					for (WorkspacePunyaPegawai workspacePunyaPegawai : workspacePunyaPegawais) {
						WorkspacePunyaPegawai copyWorkspacePunyaPegawai = (WorkspacePunyaPegawai) workspacePunyaPegawai
								.clone();
						copyWorkspacePunyaPegawai.setId(null);
						copyWorkspacePunyaPegawai.setWorkspace(copyWorkspace);
						session.save(copyWorkspacePunyaPegawai);
					}

					List<WorkspacePunyaSasaran> workspacePunyaSasarans = session
							.createCriteria(WorkspacePunyaSasaran.class).add(Restrictions.eq("workspace", myWorkspace))
							.list();
					for (WorkspacePunyaSasaran workspacePunyaSasaran : workspacePunyaSasarans) {
						WorkspacePunyaSasaran copyWorkspacePunyaSasaran = (WorkspacePunyaSasaran) workspacePunyaSasaran
								.clone();
						copyWorkspacePunyaSasaran.setId(null);
						copyWorkspacePunyaSasaran.setWorkspace(copyWorkspace);
						session.save(copyWorkspacePunyaSasaran);
					}

					List<WorkspacePunyaIndikator> workspacePunyaIndikators = session
							.createCriteria(WorkspacePunyaIndikator.class)
							.add(Restrictions.eq("workspace", myWorkspace)).list();
					for (WorkspacePunyaIndikator workspacePunyaIndikator : workspacePunyaIndikators) {
						WorkspacePunyaIndikator copyWorkspacePunyaIndikator = (WorkspacePunyaIndikator) workspacePunyaIndikator
								.clone();
						copyWorkspacePunyaIndikator.setId(null);
						copyWorkspacePunyaIndikator.setWorkspace(copyWorkspace);
						session.save(copyWorkspacePunyaIndikator);
					}

					List<WorkspacePunyaJenisParameter> workspacePunyaJenisParameters = session
							.createCriteria(WorkspacePunyaJenisParameter.class)
							.add(Restrictions.eq("workspace", myWorkspace)).list();
					for (WorkspacePunyaJenisParameter workspacePunyaJenisParameter : workspacePunyaJenisParameters) {
						WorkspacePunyaJenisParameter copyWorkspacePunyaJenisParameter = (WorkspacePunyaJenisParameter) workspacePunyaJenisParameter
								.clone();
						copyWorkspacePunyaJenisParameter.setId(null);
						copyWorkspacePunyaJenisParameter.setWorkspace(copyWorkspace);
						session.save(copyWorkspacePunyaJenisParameter);
					}

					checkForChildsCopy(copyWorkspace, workspaceTreeModel, tahunTujuan, newRevisi, satuanKerja,
							sumberDana, satuanKerjaTujuan, sumberDanaTujuan);
				}
			}
			workspace.setMerupakanHasilCopy(false);
			Common.refreshUpdate(session, (workspace));
		}
	}

	public static void createNewRevisi(final Integer tahun, final Integer oldRevisi, final Integer newRevisi,
			final Integer tahunLama, final SatuanKerja satuanKerja, final SumberDana sumberDana,
			final SatuanKerja satuanKerjaTujuan, final SumberDana sumberDanaTujuan, final EventListener eventListener) {
		try {

			Session session = HibernateUtil.currentSession();
			Integer count = ((Number) session.createCriteria(Workspace.class)
					.add(Restrictions.or(Restrictions.eq("carryOver", true),
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.eq("satuanKerja", satuanKerjaTujuan))
					.add(sumberDanaTujuan == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("sumberDana", sumberDanaTujuan))
					.add(Restrictions.eq("revisi", newRevisi)).setProjection(Projections.rowCount())
					.add(Restrictions.eq("tahunWorkspace", tahun)).uniqueResult()).intValue();

			if (!count.equals(0)) {
				MyMessageboxConfig.show("Perencanaan anggaran tahun " + tahun + " revisi ke " + newRevisi
						+ " sudah memiliki data, apakah anda ingin menimpa data yang sudah ada ?\n\n\nCatatan: Semua data anggaran tahun "
						+ tahun + " revisi ke " + newRevisi
						+ " akan terhapus dan diganti dengan hasil copy dari revisi tahun " + tahunLama + " revisi ke "
						+ oldRevisi, "Question", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {
										executeCopy(tahun, oldRevisi, newRevisi, tahunLama, satuanKerja, sumberDana,
												satuanKerjaTujuan, sumberDanaTujuan);
										eventListener.onEvent(null);
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig
												.show("Data ini tidak dapat dicopy .., error-nya adalah sbagai berikut:"
														+ e.getMessage());
									}

								}

							}
						});
			} else {
				executeCopy(tahun, oldRevisi, newRevisi, tahunLama, satuanKerja, sumberDana, satuanKerjaTujuan,
						sumberDanaTujuan);
				eventListener.onEvent(null);
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		ubahSemuaStatus(tahunLama, oldRevisi, satuanKerja, sumberDana);
	}

	@SuppressWarnings("unchecked")
	public static void executeCopy(Integer tahun, final Integer oldRevisi, final Integer newRevisi,
			Integer workspace_copy, SatuanKerja satuanKerja, SumberDana sumberDana, SatuanKerja satuanKerjaTujuan,
			SumberDana sumberDanaTujuan) {
		Session session = HibernateUtil.currentSession();

		Long parentId = WorkspaceTreeModel.checkForParent(workspace_copy, satuanKerja, oldRevisi);

		List<Workspace> willCopied = session.createCriteria(Workspace.class)
				.add(Restrictions.or(Restrictions.eq("carryOver", true),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.eq("satuanKerja", satuanKerja))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.eq("revisi", oldRevisi)).addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
				.add(Restrictions.eq("tahunWorkspace", workspace_copy)).add(Restrictions.eq("parentId", parentId))
				.list();

		// System.out.println("willCopied = " + willCopied.size()
		// + " workspace_copy = " + workspace_copy);

		session.createSQLQuery("delete from rab.workspace where tahun_workspace = " + tahun + " and revisi = "
				+ newRevisi + " and satuan_kerja = " + satuanKerjaTujuan.getId() + " and sumber_dana = "
				+ sumberDanaTujuan.getId()).executeUpdate();

		WorkspaceTreeModel willCopiedWorkspaceTreeModel = new WorkspaceTreeModel(workspace_copy, oldRevisi, satuanKerja,
				sumberDana);
		for (Workspace workspace : willCopied) {

			Long childParentId = WorkspaceTreeModel.checkForParent(tahun, satuanKerjaTujuan, newRevisi);

			Workspace newWorkspace = (Workspace) workspace.clone();
			newWorkspace.setDeep(null);
			newWorkspace.setSatuanKerja(satuanKerjaTujuan);
			newWorkspace.setSumberDana(sumberDanaTujuan);
			newWorkspace.setCopyForm(workspace.getId());
			newWorkspace.setMerupakanHasilCopy(true);
			newWorkspace.setRevisi(newRevisi);
			newWorkspace.setParentId(childParentId);

			newWorkspace.setTahunWorkspace(tahun);
			newWorkspace.setId(null);
			session.save(newWorkspace);

			List<WorkspacePunyaPegawai> workspacePunyaPegawais = session.createCriteria(WorkspacePunyaPegawai.class)
					.add(Restrictions.eq("workspace", workspace)).list();
			for (WorkspacePunyaPegawai workspacePunyaPegawai : workspacePunyaPegawais) {
				WorkspacePunyaPegawai copyWorkspacePunyaPegawai = (WorkspacePunyaPegawai) workspacePunyaPegawai.clone();
				copyWorkspacePunyaPegawai.setId(null);
				copyWorkspacePunyaPegawai.setWorkspace(newWorkspace);
				session.save(copyWorkspacePunyaPegawai);
			}

			List<WorkspacePunyaSasaran> workspacePunyaSasarans = session.createCriteria(WorkspacePunyaSasaran.class)
					.add(Restrictions.eq("workspace", workspace)).list();
			for (WorkspacePunyaSasaran workspacePunyaSasaran : workspacePunyaSasarans) {
				WorkspacePunyaSasaran copyWorkspacePunyaSasaran = (WorkspacePunyaSasaran) workspacePunyaSasaran.clone();
				copyWorkspacePunyaSasaran.setId(null);
				copyWorkspacePunyaSasaran.setWorkspace(newWorkspace);
				session.save(copyWorkspacePunyaSasaran);
			}

			List<WorkspacePunyaIndikator> workspacePunyaIndikators = session
					.createCriteria(WorkspacePunyaIndikator.class).add(Restrictions.eq("workspace", workspace)).list();
			for (WorkspacePunyaIndikator workspacePunyaIndikator : workspacePunyaIndikators) {
				WorkspacePunyaIndikator copyWorkspacePunyaIndikator = (WorkspacePunyaIndikator) workspacePunyaIndikator
						.clone();
				copyWorkspacePunyaIndikator.setId(null);
				copyWorkspacePunyaIndikator.setWorkspace(newWorkspace);
				session.save(copyWorkspacePunyaIndikator);
			}

			List<WorkspacePunyaJenisParameter> workspacePunyaJenisParameters = session
					.createCriteria(WorkspacePunyaJenisParameter.class).add(Restrictions.eq("workspace", workspace))
					.list();
			for (WorkspacePunyaJenisParameter workspacePunyaJenisParameter : workspacePunyaJenisParameters) {
				WorkspacePunyaJenisParameter copyWorkspacePunyaJenisParameter = (WorkspacePunyaJenisParameter) workspacePunyaJenisParameter
						.clone();
				copyWorkspacePunyaJenisParameter.setId(null);
				copyWorkspacePunyaJenisParameter.setWorkspace(newWorkspace);
				session.save(copyWorkspacePunyaJenisParameter);
			}

			RabUtil.checkForChildsCopy(newWorkspace, willCopiedWorkspaceTreeModel, tahun, newRevisi, satuanKerja,
					sumberDana, satuanKerjaTujuan, sumberDanaTujuan);
		}

	}

	@SuppressWarnings("unchecked")
	public static void checkForChildsCopy(Tugas tugas, TugasTreeModel tugasTreeModel, final Integer newRevisi,
			Proyek proyek, Proyek proyekTujuan) {

		if (tugas.getMerupakanHasilCopy() != null && tugas.getMerupakanHasilCopy()) {
			Session session = HibernateUtil.currentSession();
			Tugas copyFrom = (Tugas) session.createCriteria(Tugas.class).add(Restrictions.idEq(tugas.getCopyForm()))
					.uniqueResult();

			int childCount = tugasTreeModel.getChildCount(copyFrom);

			if (childCount != 0) {
				List<Tugas> tugass = tugasTreeModel.getChildren(copyFrom);

				for (Tugas myTugas : tugass) {

					Session mySession = HibernateUtil.currentNativeSession();

					Set<Tugas> tugs = ((Tugas) mySession.createCriteria(Tugas.class)
							.add(Restrictions.idEq(myTugas.getId())).uniqueResult()).getTugases();
					Set<Tugas> newTugases = new HashSet<Tugas>();
					for (Tugas t : tugs) {
						newTugases.add(new Tugas(t.getId()));
					}

					HibernateUtil.closeSession();

					Tugas copyTugas = (Tugas) myTugas.clone();
					copyTugas.setTugases(newTugases);
					copyTugas.setProyek(proyekTujuan);
					copyTugas.setCopyForm(myTugas.getId());
					copyTugas.setMerupakanHasilCopy(true);
					copyTugas.setParent(tugas);
					copyTugas.setRevisi(newRevisi);
					copyTugas.setId(null);
					session.save(copyTugas);

					List<TugasPunyaPegawai> tugasPunyaPegawais = session.createCriteria(TugasPunyaPegawai.class)
							.add(Restrictions.eq("tugas", myTugas)).list();
					for (TugasPunyaPegawai tugasPunyaPegawai : tugasPunyaPegawais) {
						TugasPunyaPegawai copyTugasPunyaPegawai = (TugasPunyaPegawai) tugasPunyaPegawai.clone();
						copyTugasPunyaPegawai.setId(null);
						copyTugasPunyaPegawai.setTugas(copyTugas);
						session.save(copyTugasPunyaPegawai);
					}

					checkForChildsCopy(copyTugas, tugasTreeModel, newRevisi, proyek, proyekTujuan);
				}
			}
			tugas.setMerupakanHasilCopy(false);
			Common.refreshUpdate(session, (tugas));
		}
	}

	public static void createNewRevisi(final Integer oldRevisi, final Integer newRevisi, final Proyek proyek,
			final Proyek proyekTujuan, final EventListener eventListener) {
		try {

			Session session = HibernateUtil.currentSession();
			Integer count = ((Number) session.createCriteria(Tugas.class).add(Restrictions.eq("proyek", proyekTujuan))
					.add(Restrictions.eq("revisi", newRevisi)).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();

			if (!count.equals(0)) {
				MyMessageboxConfig.show("Proyek tahun " + proyek.getNama() + " revisi ke " + newRevisi
						+ " sudah memiliki data, apakah anda ingin menimpa data yang sudah ada ?\n\n\nCatatan: Semua data proyek "
						+ proyek.getNama() + " revisi ke " + newRevisi + " akan terhapus dan diganti", "Question",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {
										executeCopy(oldRevisi, newRevisi, proyek, proyekTujuan);
										eventListener.onEvent(null);
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig
												.show("Data ini tidak dapat dicopy .., error-nya adalah sbagai berikut:"
														+ e.getMessage());
									}

								}

							}
						});
			} else {
				executeCopy(oldRevisi, newRevisi, proyek, proyekTujuan);
				eventListener.onEvent(null);
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static void executeCopy(final Integer oldRevisi, final Integer newRevisi, Proyek proyek,
			Proyek proyekTujuan) {
		Session session = HibernateUtil.currentSession();
		List<Tugas> willCopied = session.createCriteria(Tugas.class).add(Restrictions.eq("proyek", proyek))
				.add(Restrictions.eq("revisi", oldRevisi)).addOrder(Order.asc("nama"))
				.add(Restrictions.isNull("parent")).list();

		session.createSQLQuery(
				"delete from rab.tugas where revisi = " + newRevisi + " and proyek = " + proyekTujuan.getId())
				.executeUpdate();

		TugasTreeModel willCopiedTugasTreeModel = new TugasTreeModel(oldRevisi, proyek);
		for (Tugas tugas : willCopied) {

			Session mySession = HibernateUtil.currentNativeSession();

			Set<Tugas> tugs = ((Tugas) mySession.createCriteria(Tugas.class).add(Restrictions.idEq(tugas.getId()))
					.uniqueResult()).getTugases();

			HibernateUtil.closeSession();

			Set<Tugas> newTugases = new HashSet<Tugas>();
			for (Tugas t : tugs) {
				newTugases.add(new Tugas(t.getId()));
			}

			Tugas newTugas = (Tugas) tugas.clone();
			newTugas.setTugases(newTugases);
			newTugas.setProyek(proyekTujuan);
			newTugas.setCopyForm(tugas.getId());
			newTugas.setMerupakanHasilCopy(true);
			newTugas.setRevisi(newRevisi);
			newTugas.setParent(null);
			newTugas.setId(null);
			session.save(newTugas);

			List<TugasPunyaPegawai> tugasPunyaPegawais = session.createCriteria(TugasPunyaPegawai.class)
					.add(Restrictions.eq("tugas", tugas)).list();
			for (TugasPunyaPegawai tugasPunyaPegawai : tugasPunyaPegawais) {
				TugasPunyaPegawai copyTugasPunyaPegawai = (TugasPunyaPegawai) tugasPunyaPegawai.clone();
				copyTugasPunyaPegawai.setId(null);
				copyTugasPunyaPegawai.setTugas(newTugas);
				session.save(copyTugasPunyaPegawai);
			}

			RabUtil.checkForChildsCopy(newTugas, willCopiedTugasTreeModel, newRevisi, proyek, proyekTujuan);
		}

	}
	// @SuppressWarnings("unchecked")
	// public synchronized static void executeCopyPegawaiTugas(
	// final Integer newRevisi, Proyek proyek, Tugas parent) {
	// Session session = HibernateUtil.currentNativeSession();
	// List<Tugas> willCopied = session
	// .createCriteria(Tugas.class)
	// .add(Restrictions.eq("proyek", proyek))
	// .add(Restrictions.eq("revisi", newRevisi))
	// .addOrder(Order.asc("nama"))
	// .add(parent == null ? Restrictions.isNull("parent")
	// : Restrictions.eq("parent", parent)).list();
	//
	// HibernateUtil.closeSession();
	//
	// for (Tugas tugas : willCopied) {
	// if (tugas.getCopyForm() == null) {
	// continue;
	// }
	//
	// Session mySession = HibernateUtil.currentNativeSession();
	// Set<Pegawai> pegawais = ((Tugas) mySession
	// .createCriteria(Tugas.class)
	// .add(Restrictions.idEq(tugas.getCopyForm())).uniqueResult())
	// .getPegawais();
	//
	// Set<Pegawai> newPegawais = new HashSet<Pegawai>();
	// for (Pegawai pegawai : pegawais) {
	// newPegawais.add(new Pegawai(pegawai.getId()));
	// }
	//
	// my
	// HibernateUtil.closeSession();
	//
	// mySession = HibernateUtil.currentNativeSession();
	// tugas.setPegawais(newPegawais);
	// mySession.getTransaction().begin();
	// mySession.update(tugas);
	// mySession.getTransaction().commit();
	// my
	// HibernateUtil.closeSession();
	//
	// RabUtil.executeCopyPegawaiTugas(newRevisi, proyek, tugas);
	// }
	// }

}
