package ais.action.master.rab.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.action.master.akunting.JenisUangMukaAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.Transaksi;
import ais.database.model.rab.PenggunaanAnggaran;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;
import ais.database.model.rab.WorkspacePunyaPegawai;

public class WorkspaceTreeModel extends AbstractTreeModel { 

	private static final long serialVersionUID = -5115651721345571411L;
	private Integer tahunWorkspace;
	private Integer revisi = 1;
	private Set<SatuanKerja> satuanKerjas;
	private SumberDana sumberDana;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private boolean termasukYgNonAktif = false;

	public WorkspaceTreeModel(Integer tahunWorkspace, Integer revisi, SatuanKerja satuanKerja, SumberDana sumberDana) {
		this(tahunWorkspace, revisi, satuanKerja, sumberDana, false);
	}

	public WorkspaceTreeModel(Integer tahunWorkspace, Integer revisi, SatuanKerja satuanKerja, SumberDana sumberDana,
			boolean termasukYgNonAktif) {
		super(new Workspace(checkForParent(tahunWorkspace, satuanKerja, revisi, termasukYgNonAktif), tahunWorkspace));
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(satuanKerja, false);
		this.termasukYgNonAktif = termasukYgNonAktif;
		this.tahunWorkspace = tahunWorkspace;
		this.revisi = revisi;
		this.satuanKerjas = new HashSet<SatuanKerja>();
		this.satuanKerjas.add(satuanKerja);
		satuanKerjaTreeModel.generateAllChildren(satuanKerja, satuanKerjas);
		this.sumberDana = sumberDana;
		getCheckForLeafNulll();
	}

	/**
	 * Helper method untuk memastikan penutupan session di blok finally dengan aman
	 */
	private static void closeSession(Session session) {
		try {
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/WorkspaceTreeModel.java:72");
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/WorkspaceTreeModel.java:77");
			}
		}
	}

	public static void copy(Workspace workspace, SatuanKerja satuanKerja, SumberDana sumberDana, Integer selectedTahun,
			Integer maxrevisi) {
		maxrevisi = maxrevisi == null ? 1 : maxrevisi;
		Session session = null;

		try {
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();

			Workspace copyworkspace = (Workspace) workspace.clone();
			copyworkspace.setId(null);
			copyworkspace.setRevisi(maxrevisi);
			copyworkspace.setTahunWorkspace(selectedTahun);
			copyworkspace.setSumberDana(sumberDana);
			copyworkspace.setSatuanKerja(satuanKerja);
			copyworkspace.setParentId(0L);

			session.save(copyworkspace);
			session.flush();

			copyChild(session, copyworkspace, workspace, maxrevisi, satuanKerja, sumberDana, selectedTahun);
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session != null && session.getTransaction().isActive())
				session.getTransaction().rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/WorkspaceTreeModel.java:107");
		} finally {
			closeSession(session);
		}
	}

	public static void copyChild(Session session, Workspace copyworkspace, Workspace dari, Integer maxrevisi,
			SatuanKerja satuanKerja, SumberDana sumberDana, Integer selectedTahun) {

		List<Workspace> myparents = ConstantValues.simpleList(
				session.createCriteria(Workspace.class).add(Restrictions.eq("parentId", dari.getId())),
				Workspace.class);

		for (Workspace workspaceData : myparents) {
			Workspace copyworkspaceBaru = (Workspace) workspaceData.clone();
			copyworkspaceBaru.setId(null);
			copyworkspaceBaru.setRevisi(maxrevisi);
			copyworkspaceBaru.setTahunWorkspace(selectedTahun);
			copyworkspaceBaru.setSumberDana(sumberDana);
			copyworkspaceBaru.setSatuanKerja(satuanKerja);
			copyworkspaceBaru.setParentId(copyworkspace.getId());

			session.save(copyworkspaceBaru);
			session.flush();

			copyChild(session, copyworkspaceBaru, workspaceData, maxrevisi, satuanKerja, sumberDana, selectedTahun);
		}
	}

	private static void checkRootSatuanKerja(Integer tahunWorkspace, SatuanKerja satuanKerja,
			boolean termasukYgNonAktif) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			List<Workspace> myparents = ConstantValues.simpleList(session.createCriteria(Workspace.class)
					.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.eq("carryOver", true),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.eq("satuanKerja", satuanKerja)).add(Restrictions.eq("parentId", 0L))
					.add(tahunWorkspace == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("tahunWorkspace", tahunWorkspace)),
					Workspace.class);

			if (!myparents.isEmpty()) {
				session.getTransaction().begin();
				for (Workspace myparent : myparents) {
					if (myparent != null) {
						String s = (satuanKerja.getId() + "").length() >= "2000000001".length()
								? (satuanKerja.getId() + "").substring((satuanKerja.getId() + "").length() - 5)
								: (satuanKerja.getId() + "");
						myparent.setParentId(-(Long.MAX_VALUE
								- (Long.parseLong("" + RabUtil.DEFAULT_SATUAN_KERJA + tahunWorkspace + "" + s))));
						session.update(myparent);
					}
				}
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			if (session != null && session.getTransaction().isActive())
				session.getTransaction().rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/WorkspaceTreeModel.java:167");
		} finally {
			closeSession(session);
		}
	}

	// Metode utama untuk merekap hierarki (Dioptimasi dengan Injeksi Session)
	public static void ubahRealisasiParents(Workspace workspace, WorkspaceTreeModel workspaceTreeModel,
			Session session) {
		if (workspace == null || session == null) {
			return;
		}

		try {
			Workspace currentWorkspace = workspace;
			java.util.Date currentDate = ais.ui.util.WaktuUtil.getDate(); // Panggil sekali di luar loop

			// Iteratif mencari atasan
			while (currentWorkspace != null) {

				Double oldRealisasi = currentWorkspace.getRealisasiProses();
				if (oldRealisasi == null)
					oldRealisasi = 0.0D;

				Double realisasi = 0.0D;

				// Gunakan metode Overload yang menerima Session agar tidak buang-buang koneksi
				boolean isChild = (workspaceTreeModel.getChildCountById(currentWorkspace.getId(), session) == 0);

				if (isChild) {
					Double saldo = JenisUangMukaAction.hitungSaldoDalamProses(currentWorkspace, currentDate, false,
							session);
					realisasi = (saldo != null) ? saldo : 0.0D;
				} else {
					Double calcRealisasi = workspaceTreeModel.getRealisasiProses(currentWorkspace, session);
					realisasi = (calcRealisasi != null) ? calcRealisasi : 0.0D;
				}

				// PERBAIKAN AKURASI: Gunakan equals ketimbang intValue()
				if (!oldRealisasi.equals(realisasi)) {
					currentWorkspace.setRealisasiProses(realisasi);
					// Simpan langsung menggunakan session aktif
					Common.refreshUpdate(session, currentWorkspace);
				}

				Workspace parentWorkspace = null;
				Long parentId = currentWorkspace.getParentId();

				if (parentId != null) {
					parentWorkspace = (Workspace) ConstantValues.simpleObject(
							session.createCriteria(Workspace.class)
									.add(Restrictions.or(Restrictions.eq("carryOver", true),
											Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true))))
									.add(Restrictions.idEq(parentId)),
							Workspace.class);
				}

				currentWorkspace = parentWorkspace;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/WorkspaceTreeModel.java:228");
		}
	}

	// Metode Overload perhitungan child (Tanpa buka/tutup Session sendiri)
	public int getChildCountById(Long parent, Session session) {
		if (parent == null || session == null)
			return 0;
		return anakTerurutTampil(parent, session).size();
	}

	// Metode Overload kalkulasi proses (Tanpa buka/tutup Session sendiri)
	public Double getRealisasiProses(Object parent, Session session) {
		if (parent == null || session == null)
			return 0.0D;

		Workspace myparentWorkspace = (Workspace) parent;
		Workspace parentWorkspace = (Workspace) session.createCriteria(Workspace.class)
				.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("carryOver", true),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.idEq(myparentWorkspace.getId())).uniqueResult();

		if (parentWorkspace != null) {
			if (getChildCountById(parentWorkspace.getId(), session) != 0 || (parentWorkspace.getRevisi() != null
					&& parentWorkspace.getRevisi().equals(ais.action.master.rab.util.RabUtil.DEFAULT_REVISI))) {
				Number count = ((Number) session.createCriteria(Workspace.class)
						.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("carryOver", true),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
						.add(Restrictions.ne("id", parentWorkspace.getId()))
						.add(Restrictions.eq("parentId", parentWorkspace.getId()))
						.setProjection(Projections.sum("realisasiProses")).uniqueResult());
				return count == null ? 0.0 : count.doubleValue();
			} else {
				return parentWorkspace.getRealisasiProses() == null ? 0.0D : parentWorkspace.getRealisasiProses();
			}
		}
		return 0.0D;
	}

	public void ubahHargaTotalParentss(Workspace workspace) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			ubahHargaTotalParentssRecursive(workspace, session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/WorkspaceTreeModel.java:275");
		} finally {
			closeSession(session);
		}
	}

	private void ubahHargaTotalParentssRecursive(Workspace workspace, Session session) {
		Object[] totals = getHargaTotals(workspace, session);

		Double bulan1 = (totals[0] == null ? 0.0 : (Number) totals[0]).doubleValue();
		Double bulan2 = (totals[1] == null ? 0.0 : (Number) totals[1]).doubleValue();
		Double bulan3 = (totals[2] == null ? 0.0 : (Number) totals[2]).doubleValue();
		Double bulan4 = (totals[3] == null ? 0.0 : (Number) totals[3]).doubleValue();
		Double bulan5 = (totals[4] == null ? 0.0 : (Number) totals[4]).doubleValue();
		Double bulan6 = (totals[5] == null ? 0.0 : (Number) totals[5]).doubleValue();
		Double bulan7 = (totals[6] == null ? 0.0 : (Number) totals[6]).doubleValue();
		Double bulan8 = (totals[7] == null ? 0.0 : (Number) totals[7]).doubleValue();
		Double bulan9 = (totals[8] == null ? 0.0 : (Number) totals[8]).doubleValue();
		Double bulan10 = (totals[9] == null ? 0.0 : (Number) totals[9]).doubleValue();
		Double bulan11 = (totals[10] == null ? 0.0 : (Number) totals[10]).doubleValue();
		Double bulan12 = (totals[11] == null ? 0.0 : (Number) totals[11]).doubleValue();

		Double total = bulan1 + bulan2 + bulan3 + bulan4 + bulan5 + bulan6 + bulan7 + bulan8 + bulan9 + bulan10
				+ bulan11 + bulan12;

		if (workspace.getBulan1().intValue() != bulan1.intValue()
				|| workspace.getBulan2().intValue() != bulan2.intValue()
				|| workspace.getBulan3().intValue() != bulan3.intValue()
				|| workspace.getBulan4().intValue() != bulan4.intValue()
				|| workspace.getBulan5().intValue() != bulan5.intValue()
				|| workspace.getBulan6().intValue() != bulan6.intValue()
				|| workspace.getBulan7().intValue() != bulan7.intValue()
				|| workspace.getBulan8().intValue() != bulan8.intValue()
				|| workspace.getBulan9().intValue() != bulan9.intValue()
				|| workspace.getBulan10().intValue() != bulan10.intValue()
				|| workspace.getBulan11().intValue() != bulan11.intValue()
				|| workspace.getBulan12().intValue() != bulan12.intValue()) {

			workspace.setHargaTotal(total);
			workspace.setBulan1(bulan1);
			workspace.setBulan2(bulan2);
			workspace.setBulan3(bulan3);
			workspace.setBulan4(bulan4);
			workspace.setBulan5(bulan5);
			workspace.setBulan6(bulan6);
			workspace.setBulan7(bulan7);
			workspace.setBulan8(bulan8);
			workspace.setBulan9(bulan9);
			workspace.setBulan10(bulan10);
			workspace.setBulan11(bulan11);
			workspace.setBulan12(bulan12);

			session.getTransaction().begin();
			Common.refreshUpdate(session, workspace);
			session.getTransaction().commit();
		}

		Workspace parentWorkspace = (Workspace) ConstantValues.simpleObject(session.createCriteria(Workspace.class)
				.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("carryOver", true),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.idEq(workspace.getParentId())), Workspace.class);

		if (parentWorkspace != null) {
			ubahHargaTotalParentssRecursive(parentWorkspace, session);
		}
	}

	public void ubahHargaTotalParents(Workspace workspace) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			ubahHargaTotalParentsRecursive(workspace, session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/WorkspaceTreeModel.java:349");
		} finally {
			closeSession(session);
		}
	}

	private void ubahHargaTotalParentsRecursive(Workspace workspace, Session session) {
		Double total = getHargaTotal(workspace, session);
		if (workspace.getHargaTotal() == null || !workspace.getHargaTotal().equals(total)) {
			workspace.setHargaTotal(total);
			session.getTransaction().begin();
			Common.refreshUpdate(session, (workspace));
			session.getTransaction().commit();
		}

		Workspace parentWorkspace = (Workspace) ConstantValues.simpleObject(session.createCriteria(Workspace.class)
				.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.idEq(workspace.getParentId())), Workspace.class);

		if (parentWorkspace != null) {
			ubahHargaTotalParentsRecursive(parentWorkspace, session);
		}
	}

	public void ubahMulaiParents(Workspace workspace) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			ubahMulaiParentsRecursive(workspace, session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/WorkspaceTreeModel.java:380");
		} finally {
			closeSession(session);
		}
	}

	private void ubahMulaiParentsRecursive(Workspace workspace, Session session) {
		Date mulai = getMinimalMulai(workspace, session);
		workspace.setMulai(mulai);

		session.getTransaction().begin();
		Common.refreshUpdate(session, (workspace));
		session.getTransaction().commit();

		Workspace parentWorkspace = (Workspace) ConstantValues.simpleObject(session.createCriteria(Workspace.class)
				.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("carryOver", true),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.idEq(workspace.getParentId())), Workspace.class);

		if (parentWorkspace != null) {
			ubahMulaiParentsRecursive(parentWorkspace, session);
		}
	}

	public void ubahSelesaiParents(Workspace workspace) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			ubahSelesaiParentsRecursive(workspace, session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/WorkspaceTreeModel.java:411");
		} finally {
			closeSession(session);
		}
	}

	private void ubahSelesaiParentsRecursive(Workspace workspace, Session session) {
		Date selesai = getMaksimalSelesai(workspace, session);
		workspace.setSelesai(selesai);

		session.getTransaction().begin();
		Common.refreshUpdate(session, (workspace));
		session.getTransaction().commit();

		Workspace parentWorkspace = (Workspace) ConstantValues.simpleObject(session.createCriteria(Workspace.class)
				.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("carryOver", true),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.idEq(workspace.getParentId())), Workspace.class);

		if (parentWorkspace != null) {
			ubahSelesaiParentsRecursive(parentWorkspace, session);
		}
	}

	public void ubahKomplitParents(Workspace workspace) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			ubahKomplitParentsRecursive(workspace, session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/WorkspaceTreeModel.java:442");
		} finally {
			closeSession(session);
		}
	}

	private void ubahKomplitParentsRecursive(Workspace workspace, Session session) {
		Double persenKomplit = getPersenKomplit(workspace, session);
		workspace.setPersenKomplit(persenKomplit);

		session.getTransaction().begin();
		Common.refreshUpdate(session, (workspace));
		session.getTransaction().commit();

		Workspace parentWorkspace = (Workspace) ConstantValues.simpleObject(session.createCriteria(Workspace.class)
				.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("carryOver", true),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.idEq(workspace.getParentId())), Workspace.class);

		if (parentWorkspace != null) {
			ubahKomplitParentsRecursive(parentWorkspace, session);
		}
	}

	public void ubahDurasiParents(Workspace workspace) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			ubahDurasiParentsRecursive(workspace, session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/WorkspaceTreeModel.java:473");
		} finally {
			closeSession(session);
		}
	}

	private void ubahDurasiParentsRecursive(Workspace workspace, Session session) {
		Integer durasi = getDurasi(workspace, session);
		workspace.setDurasi(durasi);

		session.getTransaction().begin();
		Common.refreshUpdate(session, (workspace));
		session.getTransaction().commit();

		Workspace parentWorkspace = (Workspace) ConstantValues.simpleObject(session.createCriteria(Workspace.class)
				.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("carryOver", true),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.idEq(workspace.getParentId())), Workspace.class);

		if (parentWorkspace != null) {
			ubahDurasiParentsRecursive(parentWorkspace, session);
		}
	}

	public void ubahPegawaisParents(Workspace workspace) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			ubahPegawaisParentsRecursive(workspace, session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/WorkspaceTreeModel.java:504");
		} finally {
			closeSession(session);
		}
	}

	private void ubahPegawaisParentsRecursive(Workspace workspace, Session session) {
		List<WorkspacePunyaPegawai> workspacePunyaPegawais = getPagawais(workspace, session);

		session.getTransaction().begin();
		session.createSQLQuery("delete from rab.workspace_punya_pegawai where workspace = :workspaceId")
				.setLong("workspaceId", workspace.getId()).executeUpdate();

		for (WorkspacePunyaPegawai workspacePunyaPegawai : workspacePunyaPegawais) {
			WorkspacePunyaPegawai copyWorkspacePunyaPegawai = (WorkspacePunyaPegawai) workspacePunyaPegawai.clone();
			copyWorkspacePunyaPegawai.setId(null);
			copyWorkspacePunyaPegawai.setWorkspace(workspace);
			session.save(copyWorkspacePunyaPegawai);
		}
		session.getTransaction().commit();
		session.flush();

		Workspace parentWorkspace = (Workspace) ConstantValues.simpleObject(session.createCriteria(Workspace.class)
				.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("carryOver", true),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.idEq(workspace.getParentId())), Workspace.class);

		if (parentWorkspace != null) {
			ubahPegawaisParentsRecursive(parentWorkspace, session);
		}
	}

	private static Workspace createNewWorkspace(Integer tahunWorkspace, SatuanKerja satuanKerja) {
		Workspace workspace = new Workspace();
		workspace.setAkun(null);
		workspace.setCopyForm(null);
		workspace.setDeep(null);
		workspace.setDurasi(0);
		workspace.setHargaSatuan(0.0);
		workspace.setHargaTotal(0.0);
		workspace.setJenisWorkspace(null);
		workspace.setJmlDipakai(0L);
		workspace.setJmlWaktu(0.0);
		workspace.setKeterangan("");
		workspace.setKode(satuanKerja.getKode());
		workspace.setLeaf(false);
		workspace.setMerupakanHasilCopy(false);
		workspace.setMulai(null);
		workspace.setNama(satuanKerja.getNama());

		String sParent = satuanKerja.getParent() == null ? ""
				: (satuanKerja.getParent().getId() + "").length() >= "2000000001".length()
						? (satuanKerja.getParent().getId() + "")
								.substring((satuanKerja.getParent().getId() + "").length() - 5)
						: (satuanKerja.getParent().getId() + "");

		workspace.setParentId(satuanKerja.getParent() == null ? -Long.MAX_VALUE
				: -(Long.MAX_VALUE
						- (Long.parseLong((RabUtil.DEFAULT_SATUAN_KERJA) + "" + tahunWorkspace + "" + sParent))));
		workspace.setPersenKomplit(0.0);
		workspace.setQty(0.0);
		workspace.setRealisasiTotal(0.0);
		workspace.setRevisi(RabUtil.DEFAULT_REVISI);
		workspace.setSatuan(null);
		workspace.setSatuan1(null);
		workspace.setSatuanKerja(satuanKerja);
		workspace.setSatuanVolume("");
		workspace.setSelesai(null);
		workspace.setSumberDana(null);
		workspace.setTahunWorkspace(tahunWorkspace);
		workspace.setUnitOrganisasi(null);
		workspace.setVolume(0.0);

		String s = (satuanKerja.getId() + "").length() >= "2000000001".length()
				? (satuanKerja.getId() + "").substring((satuanKerja.getId() + "").length() - 5)
				: (satuanKerja.getId() + "");
		final Long mustid = -(Long.MAX_VALUE
				- (Long.parseLong((RabUtil.DEFAULT_SATUAN_KERJA) + "" + tahunWorkspace + "" + s)));

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();

			// Cek apakah workspace dengan mustid sudah ada (bisa terjadi saat concurrent call)
			Workspace existing = (Workspace) session.get(Workspace.class, mustid);
			if (existing != null) {
				return existing;
			}

			session.getTransaction().begin();
			session.save(workspace);

			String sql = "update rab.workspace set id = " + mustid + " where id = " + workspace.getId();
			session.createSQLQuery(sql).executeUpdate();
			session.getTransaction().commit();

			workspace = (Workspace) ConstantValues.simpleObject(
					session.createCriteria(Workspace.class).add(Restrictions.idEq(mustid)), Workspace.class);

		} catch (org.hibernate.exception.ConstraintViolationException cve) {
			// workspace dengan mustid sudah dibuat oleh proses lain — ambil yang sudah ada
			if (session != null && session.getTransaction().isActive()) {
				try { session.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/rab/util/WorkspaceTreeModel.java:607");}
			}
			try {
				session.clear();
				workspace = (Workspace) session.get(Workspace.class, mustid);
			} catch (Exception ex) {
				workspace = null;
			}
		} catch (Exception e) {
			if (session != null && session.getTransaction().isActive())
				session.getTransaction().rollback();
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSession(session);
		}

		return workspace;
	}

	public static void main(String[] argv) {
		System.out.println("max = " + Long.MAX_VALUE);
		Long.parseLong("9999920122000000001");
	}

	public static Long checkForParent(Integer tahunWorkspace, SatuanKerja satuanKerja, Integer revisi) {
		return checkForParent(tahunWorkspace, satuanKerja, revisi, false);
	}

	public static Long checkForParent(Integer tahunWorkspace, SatuanKerja satuanKerja, Integer revisi,
			boolean termasukYgNonAktif) {
		checkRootSatuanKerja(tahunWorkspace, satuanKerja, termasukYgNonAktif);
		SatuanKerjaTreeModel satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		Long parent = 0L;

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Number myparent = (Number) session.createCriteria(Workspace.class)
					.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("satuanKerja", satuanKerja))
					.add(Restrictions.eq("revisi", RabUtil.DEFAULT_REVISI))
					.add(tahunWorkspace == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("tahunWorkspace", tahunWorkspace))
					.setProjection(Projections.min("parentId")).uniqueResult();
			parent = myparent == null ? null : myparent.longValue();

			if (parent == null) {
				parent = createNewWorkspace(tahunWorkspace, satuanKerja).getParentId();
			}

			List<SatuanKerja> satuanKerjas = satuanKerjaTreeModel.getChildren(satuanKerja);
			for (SatuanKerja mySatuanKerja : satuanKerjas) {
				checkForParent(tahunWorkspace, mySatuanKerja, revisi, termasukYgNonAktif);
			}

			/*
			 * PENTING: native session ThreadLocal yang dipegang `session` SUDAH ditutup di titik ini.
			 * Baik createNewWorkspace(...) di atas maupun rekursi checkForParent(...) untuk tiap anak
			 * memakai session ThreadLocal yang SAMA (HibernateUtil.currentNativeSession()) lalu
			 * menutupnya di blok finally masing-masing (closeSession). Akibatnya query kedua di bawah
			 * memicu "org.hibernate.SessionException: Session is closed!". Ambil ulang session yang
			 * usable sebelum query kedua; closeSession(session) di finally akan menutup session baru ini.
			 */
			session = HibernateUtil.currentNativeSession();

			myparent = (Number) session.createCriteria(Workspace.class)
					.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("satuanKerja", satuanKerja)).add(Restrictions.eq("revisi", revisi))
					.add(tahunWorkspace == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("tahunWorkspace", tahunWorkspace))
					.setProjection(Projections.min("parentId")).uniqueResult();
			parent = myparent == null ? null : myparent.longValue();

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/WorkspaceTreeModel.java:683");
		} finally {
			closeSession(session);
		}

		return parent == null ? 0L : parent;
	}

	@SuppressWarnings("unchecked")
	public void getCheckForLeafNulll() {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			List<Workspace> count = (session.createCriteria(Workspace.class)
					.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.eq("carryOver", true),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
							Restrictions.in("satuanKerja", satuanKerjas)))
					.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("sumberDana", sumberDana))
					.add(Restrictions.eq("revisi", revisi))
					.add(tahunWorkspace == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("tahunWorkspace", tahunWorkspace))
					.add(Restrictions.isNull("leaf")).list());

			if (!count.isEmpty()) {
				session.getTransaction().begin();
				for (Workspace workspace : count) {
					workspace.setLeaf(getChildCount(workspace, session) == 0);
					Common.refreshUpdate(session, (workspace));
				}
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			if (session != null && session.getTransaction().isActive())
				session.getTransaction().rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/WorkspaceTreeModel.java:720");
		} finally {
			closeSession(session);
		}
	}

	public List<Workspace> getChildren(Workspace parentWorkspace) {
		return getChildren(parentWorkspace, null);
	}

	/**
	 * Ambil anak (children) untuk DITAMPILKAN di pohon: terfilter aktif/carry-over, di-scope
	 * sumber dana bila dipilih, lalu DI-DEDUP per (kode + nama) supaya satu item TAMPIL SEKALI
	 * walau di DB ada banyak baris (mis. satu baris per sumber dana saat melihat "Semua").
	 */
	private List<Workspace> anakTerurutTampil(Long parentId, Session session) {
		if (parentId == null || session == null) {
			return new ArrayList<Workspace>();
		}
		List<Workspace> raw = ConstantValues.simpleList(session.createCriteria(Workspace.class)
				.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("carryOver", true),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.eq("parentId", parentId)).add(Restrictions.ne("id", parentId))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("sumberDana", sumberDana))
				.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")).addOrder(Order.desc("id")), Workspace.class);
		if (raw == null || raw.isEmpty()) {
			return new ArrayList<Workspace>();
		}
		java.util.LinkedHashMap<String, Workspace> unik = new java.util.LinkedHashMap<String, Workspace>();
		for (Workspace w : raw) {
			if (w == null) {
				continue;
			}
			String kunci = (w.getKode() == null ? "" : w.getKode().trim()) + "||"
					+ (w.getNama() == null ? "" : w.getNama().trim());
			if (!unik.containsKey(kunci)) {
				unik.put(kunci, w);
			}
		}
		return new ArrayList<Workspace>(unik.values());
	}
	public Workspace getSatuChildren(Workspace parentWorkspace, Integer index) {
		if (parentWorkspace == null || parentWorkspace.getId() == null) {
			return null;
		}
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			List<Workspace> anak = anakTerurutTampil(parentWorkspace.getId(), session);
			int i = index == null ? 0 : index;
			return (i >= 0 && i < anak.size()) ? anak.get(i) : null;
		} finally {
			closeSession(session);
		}
	}

	public List<Workspace> getChildren(Workspace parentWorkspace, Integer index) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return getChildren(parentWorkspace, index, session);
		} finally {
			closeSession(session);
		}
	}

	private List<Workspace> getChildren(Workspace parentWorkspace, Integer index, Session session) {
		List<Workspace> anak = anakTerurutTampil(parentWorkspace.getId(), session);
		if (index == null) {
			return anak;
		}
		return (index >= 0 && index < anak.size()) ? new ArrayList<Workspace>(anak.subList(index, index + 1))
				: new ArrayList<Workspace>();
	}

	public List<Long> getChildrenIds(Long parentWorkspace) {
		return getChildrenIds(parentWorkspace, null);
	}

	@SuppressWarnings("unchecked")
	public List<Long> getChildrenIds(Long parentWorkspace, Integer index) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			List<Workspace> anak = anakTerurutTampil(parentWorkspace, session);
			List<Long> ids = new ArrayList<Long>();
			if (index == null) {
				for (Workspace w : anak) {
					ids.add(w.getId());
				}
			} else if (index >= 0 && index < anak.size()) {
				ids.add(anak.get(index).getId());
			}
			return ids;
		} finally {
			closeSession(session);
		}
	}

	public boolean sudahDigunakanTransaksi(Workspace workspace) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Number number = (Number) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.eq("workspace", workspace)).setProjection(Projections.rowCount()).uniqueResult();
			return number.intValue() != 0;
		} finally {
			closeSession(session);
		}
	}

	public boolean sudahDigunakanTransaksi(Integer tahunWorkspace, Integer revisi, SatuanKerja satuanKerja,
			SumberDana sumberDana) {
		return sudahDigunakanTransaksi(null, tahunWorkspace, revisi, satuanKerja, sumberDana);
	}

	public boolean sudahDigunakanTransaksi(Workspace workspace, Integer tahunWorkspace, Integer revisi,
			SatuanKerja satuanKerja, SumberDana sumberDana) {
		Set<Workspace> workspaces = null;
		if (workspace != null && workspace.getId() != null) {
			workspaces = new HashSet<Workspace>();
			generateChilds(workspace, workspaces);
		}

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Number number = (Number) session.createCriteria(GrupTransaksi.class)
					.add(workspaces != null ? Restrictions.in("workspace", workspaces)
							: Restrictions.sqlRestriction("1=1"))
					.createAlias("workspace", "workspace")
					.add(Restrictions.eq("workspace.tahunWorkspace", tahunWorkspace))
					.add(Restrictions.eq("workspace.revisi", revisi))
					.add(Restrictions.eq("workspace.satuanKerja", satuanKerja))
					.add(sumberDana == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("workspace.sumberDana", sumberDana))
					.setProjection(Projections.rowCount()).uniqueResult();
			return number.intValue() != 0;
		} finally {
			closeSession(session);
		}
	}

	@Override
	public Object getChild(Object parent, int index) {
		Workspace parentWorkspace = (Workspace) parent;
		return getSatuChildren(parentWorkspace, index);
	}

	public int getChildCountById(Long parent) {
		if (parent == null)
			return 0;
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return anakTerurutTampil(parent, session).size();
		} finally {
			closeSession(session);
		}
	}

	public int getChildCount(Object parent) {
		Workspace parentWorkspace = (Workspace) parent;
		if (parentWorkspace == null || parentWorkspace.getId() == null) {
			return 0;
		}
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return getChildCount(parentWorkspace, session);
		} finally {
			closeSession(session);
		}
	}

	private int getChildCount(Workspace parentWorkspace, Session session) {
		if (parentWorkspace == null || parentWorkspace.getId() == null)
			return 0;
		return anakTerurutTampil(parentWorkspace.getId(), session).size();
	}

	public void deleteChilds(Object parent) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			deleteChildsRecursive((Workspace) parent, session);
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session != null && session.getTransaction().isActive())
				session.getTransaction().rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/WorkspaceTreeModel.java:912");
		} finally {
			closeSession(session);
		}
	}

	private void deleteChildsRecursive(Workspace parentWorkspace, Session session) {
		List<Workspace> workspaces = getChildren(parentWorkspace, null, session);
		for (Workspace workspace : workspaces) {
			if (getChildCount(workspace, session) == 0) {
				session.createSQLQuery(
						"delete from rab.workspace_punya_indikator where workspace = " + workspace.getId())
						.executeUpdate();
				session.createSQLQuery(
						"delete from rab.workspace_punya_jenis_parameter where workspace = " + workspace.getId())
						.executeUpdate();
				session.createSQLQuery("delete from rab.workspace_punya_pegawai where workspace = " + workspace.getId())
						.executeUpdate();
				session.createSQLQuery("delete from rab.workspace_punya_sasaran where workspace = " + workspace.getId())
						.executeUpdate();
				session.delete(workspace);
			} else {
				deleteChildsRecursive(workspace, session);
			}
		}
		session.delete(parentWorkspace);
	}

	public void deleteChildsWithNativeSession(Object parent) {
		deleteChilds(parent); // Logika dipusatkan agar menggunakan recursive shared session
	}

	public Double getHargaTotal(Object parent) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return getHargaTotal((Workspace) parent, session);
		} finally {
			closeSession(session);
		}
	}

	private Double getHargaTotal(Workspace parentWorkspace, Session session) {
		if (getChildCount(parentWorkspace, session) != 0
				|| parentWorkspace.getRevisi().equals(RabUtil.DEFAULT_REVISI)) {
			Number count = ((Number) session.createCriteria(Workspace.class)
					.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.eq("carryOver", true),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.eq("parentId", parentWorkspace.getId()))
					.add(Restrictions.ne("id", parentWorkspace.getId())).setProjection(Projections.sum("hargaTotal"))
					.uniqueResult());
			return count == null ? 0.0 : count.doubleValue();
		} else {
			return (parentWorkspace.getQty() == null ? 0.0 : parentWorkspace.getQty())
					* (parentWorkspace.getJmlWaktu() == null ? 0.0 : parentWorkspace.getJmlWaktu())
					* (parentWorkspace.getHargaSatuan() == null ? 0.0 : parentWorkspace.getHargaSatuan());
		}
	}

	public Object[] getHargaTotals(Object parent) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return getHargaTotals((Workspace) parent, session);
		} finally {
			closeSession(session);
		}
	}

	private Object[] getHargaTotals(Workspace parentWorkspace, Session session) {
		if (getChildCount(parentWorkspace, session) != 0
				|| parentWorkspace.getRevisi().equals(RabUtil.DEFAULT_REVISI)) {
			Object[] count = ((Object[]) session.createCriteria(Workspace.class)
					.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.eq("carryOver", true),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.ne("id", parentWorkspace.getId()))
					.add(Restrictions.eq("parentId", parentWorkspace.getId()))
					.setProjection(Projections.projectionList().add(Projections.sum("bulan1"))
							.add(Projections.sum("bulan2")).add(Projections.sum("bulan3"))
							.add(Projections.sum("bulan4")).add(Projections.sum("bulan5"))
							.add(Projections.sum("bulan6")).add(Projections.sum("bulan7"))
							.add(Projections.sum("bulan8")).add(Projections.sum("bulan9"))
							.add(Projections.sum("bulan10")).add(Projections.sum("bulan11"))
							.add(Projections.sum("bulan12")).add(Projections.sum("hargaTotal")))
					.uniqueResult());
			return count;
		} else {
			return new Object[] { parentWorkspace.getBulan1(), parentWorkspace.getBulan2(), parentWorkspace.getBulan3(),
					parentWorkspace.getBulan4(), parentWorkspace.getBulan5(), parentWorkspace.getBulan6(),
					parentWorkspace.getBulan7(), parentWorkspace.getBulan8(), parentWorkspace.getBulan9(),
					parentWorkspace.getBulan10(), parentWorkspace.getBulan11(), parentWorkspace.getBulan12(),
					parentWorkspace.getHargaTotal() };
		}
	}

	public Date getMaksimalSelesai(Object parent) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return getMaksimalSelesai((Workspace) parent, session);
		} finally {
			closeSession(session);
		}
	}

	private Date getMaksimalSelesai(Workspace parentWorkspace, Session session) {
		if (getChildCount(parentWorkspace, session) != 0
				|| parentWorkspace.getRevisi().equals(RabUtil.DEFAULT_REVISI)) {
			return ((Date) session.createCriteria(Workspace.class)
					.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.eq("carryOver", true),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.ne("id", parentWorkspace.getId()))
					.add(Restrictions.eq("parentId", parentWorkspace.getId())).setProjection(Projections.max("selesai"))
					.uniqueResult());
		} else {
			return parentWorkspace.getSelesai();
		}
	}

	public Date getMinimalMulai(Object parent) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return getMinimalMulai((Workspace) parent, session);
		} finally {
			closeSession(session);
		}
	}

	private Date getMinimalMulai(Workspace parentWorkspace, Session session) {
		if (getChildCount(parentWorkspace, session) != 0
				|| parentWorkspace.getRevisi().equals(RabUtil.DEFAULT_REVISI)) {
			return ((Date) session.createCriteria(Workspace.class)
					.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.eq("carryOver", true),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.ne("id", parentWorkspace.getId()))
					.add(Restrictions.eq("parentId", parentWorkspace.getId())).setProjection(Projections.min("mulai"))
					.uniqueResult());
		} else {
			return parentWorkspace.getMulai();
		}
	}

	public Integer getDurasi(Object parent) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return getDurasi((Workspace) parent, session);
		} finally {
			closeSession(session);
		}
	}

	private Integer getDurasi(Workspace parent, Session session) {
		Date mulai = getMinimalMulai(parent, session);
		Date selesai = getMaksimalSelesai(parent, session);
		if (mulai != null && selesai != null) {
			return (int) ((selesai.getTime() - mulai.getTime()) / (1000 * 60 * 60 * 24));
		}
		return 0;
	}

	public List<WorkspacePunyaPegawai> getPagawais(Object parent) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return getPagawais((Workspace) parent, session);
		} finally {
			closeSession(session);
		}
	}

	@SuppressWarnings("unchecked")
	private List<WorkspacePunyaPegawai> getPagawais(Workspace parentWorkspace, Session session) {
		if (getChildCount(parentWorkspace, session) != 0
				|| parentWorkspace.getRevisi().equals(RabUtil.DEFAULT_REVISI)) {
			return session.createCriteria(WorkspacePunyaPegawai.class).createCriteria("workspace")
					.add(Restrictions.ne("id", parentWorkspace.getId()))
					.add(Restrictions.eq("parentId", parentWorkspace.getId())).addOrder(Order.asc("kode"))
					.addOrder(Order.asc("nama")).addOrder(Order.desc("id")).list();
		} else {
			return session.createCriteria(WorkspacePunyaPegawai.class)
					.add(Restrictions.eq("workspace", parentWorkspace)).list();
		}
	}

	public Double getRealisasiSemuaTotal(Object parent) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return getRealisasiSemuaTotalRecursive((Workspace) parent, session);
		} finally {
			closeSession(session);
		}
	}

	private Double getRealisasiSemuaTotalRecursive(Workspace parentWorkspace, Session session) {
		Double count = 0.0;
		List<Workspace> workspaces = getChildren(parentWorkspace, null, session);
		for (Workspace workspace : workspaces) {
			if (getChildCount(workspace, session) == 0) {
				count += getRealisasi(workspace, session);
			} else {
				count += getRealisasiSemuaTotalRecursive(workspace, session);
			}
		}
		return count == null ? 0.0 : count;
	}

	public Double getRealisasiTotal(Object parent) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Workspace myparentWorkspace = (Workspace) parent;
			Workspace parentWorkspace = (Workspace) session.createCriteria(Workspace.class)
					.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(myparentWorkspace.getId())).uniqueResult();

			if (getChildCount(parentWorkspace, session) != 0
					|| parentWorkspace.getRevisi().equals(RabUtil.DEFAULT_REVISI)) {
				Number count = ((Number) session.createCriteria(Workspace.class)
						.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("carryOver", true),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
						.add(Restrictions.ne("id", parentWorkspace.getId()))
						.add(Restrictions.eq("parentId", parentWorkspace.getId()))
						.setProjection(Projections.sum("realisasiTotal")).uniqueResult());
				return count == null ? 0.0 : count.doubleValue();
			} else {
				return (parentWorkspace.getRealisasiProses());
			}
		} finally {
			closeSession(session);
		}
	}

	public Double getRealisasiProses(Object parent) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Workspace myparentWorkspace = (Workspace) parent;
			Workspace parentWorkspace = (Workspace) session.createCriteria(Workspace.class)
					.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.eq("carryOver", true),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.idEq(myparentWorkspace.getId())).uniqueResult();

			if (getChildCount(parentWorkspace, session) != 0
					|| parentWorkspace.getRevisi().equals(RabUtil.DEFAULT_REVISI)) {
				Number count = ((Number) session.createCriteria(Workspace.class)
						.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("carryOver", true),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
						.add(Restrictions.ne("id", parentWorkspace.getId()))
						.add(Restrictions.eq("parentId", parentWorkspace.getId()))
						.setProjection(Projections.sum("realisasiProses")).uniqueResult());
				return count == null ? 0.0 : count.doubleValue();
			} else {
				return (parentWorkspace.getRealisasiProses());
			}
		} finally {
			closeSession(session);
		}
	}

	public void generateChilds(Workspace workspace, Set<Workspace> childs) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			generateChildsRecursive(workspace, childs, session);
		} finally {
			closeSession(session);
		}
	}

	private void generateChildsRecursive(Workspace workspace, Set<Workspace> childs, Session session) {
		childs.add(workspace);
		List<Workspace> mychilds = getChildren(workspace, null, session);
		for (Workspace myWorkspace : mychilds) {
			childs.add(myWorkspace);
			if (getChildCount(myWorkspace, session) > 0) {
				generateChildsRecursive(myWorkspace, childs, session);
			}
		}
	}

	public void generateChildsByIds(Long workspace, Set<Long> childs) {
		List<Long> mychilds = getChildrenIds(workspace);
		if (mychilds.size() > 0) {
			for (Long myWorkspace : mychilds) {
				int count = getChildCountById(myWorkspace);
				if (count <= 0) {
					childs.add(myWorkspace);
				} else {
					generateChildsByIds(myWorkspace, childs);
				}
			}
		} else {
			childs.add(workspace);
		}
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> getHargaTotalRealisasiTiapBulan(Set<Long> childs, Integer tahun) {
		String sql = "select sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('"
				+ tahun + "-01-01') and date(b.tanggal_transaksi) < date('" + (tahun + 1)
				+ "-01-01') then b.kredit else 0 end) as total, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-01-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-02-01') then b.kredit else 0 end) as harga_total_1, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-02-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-03-01') then b.kredit else 0 end) as harga_total_2, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-03-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-04-01') then b.kredit else 0 end) as harga_total_3, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-04-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-05-01') then b.kredit else 0 end) as harga_total_4, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-05-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-06-01') then b.kredit else 0 end) as harga_total_5, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-06-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-07-01') then b.kredit else 0 end) as harga_total_6, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-07-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-08-01') then b.kredit else 0 end) as harga_total_7, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-08-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-09-01') then b.kredit else 0 end) as harga_total_8, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-09-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-10-01') then b.kredit else 0 end) as harga_total_9, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-10-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-11-01') then b.kredit else 0 end) as harga_total_10, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-11-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-12-01') then b.kredit else 0 end) as harga_total_11, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-12-01') and date(b.tanggal_transaksi) < date('" + (tahun + 1)
				+ "-01-01') then b.kredit else 0 end) as harga_total_12 \n"
				+ "from akunting.workspace_has_transaksi a \n"
				+ "inner join akunting.transaksi b on (a.transaksi = b.id) \n"
				+ "inner join rab.workspace c on (a.workspace = c.id) \n" + "where c.id in ("
				+ (childs.size() == 0 ? "-1" : childs.toString().replaceAll("\\[", "").replaceAll("\\]", "")) + ");";

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return session.createSQLQuery(sql).list();
		} finally {
			closeSession(session);
		}
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> getHargaTotalPerencanaanTiapBulan(Set<Long> childs, Integer tahun) {
		String sql = "select \nsum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-01-01') and date(a.mulai) < date('" + (tahun + 1)
				+ "-01-01') then a.harga_total else 0 end) as total, \n"
				+ "sum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-01-01') and date(a.mulai) < date('" + tahun
				+ "-02-01') then a.harga_total else 0 end) as harga_total_1, \n"
				+ "sum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-02-01') and date(a.mulai) < date('" + tahun
				+ "-03-01') then a.harga_total else 0 end) as harga_total_2, \n"
				+ "sum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-03-01') and date(a.mulai) < date('" + tahun
				+ "-04-01') then a.harga_total else 0 end) as harga_total_3, \n"
				+ "sum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-04-01') and date(a.mulai) < date('" + tahun
				+ "-05-01') then a.harga_total else 0 end) as harga_total_4, \n"
				+ "sum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-05-01') and date(a.mulai) < date('" + tahun
				+ "-06-01') then a.harga_total else 0 end) as harga_total_5, \n"
				+ "sum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-06-01') and date(a.mulai) < date('" + tahun
				+ "-07-01') then a.harga_total else 0 end) as harga_total_6, \n"
				+ "sum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-07-01') and date(a.mulai) < date('" + tahun
				+ "-08-01') then a.harga_total else 0 end) as harga_total_7, \n"
				+ "sum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-08-01') and date(a.mulai) < date('" + tahun
				+ "-09-01') then a.harga_total else 0 end) as harga_total_8, \n"
				+ "sum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-09-01') and date(a.mulai) < date('" + tahun
				+ "-10-01') then a.harga_total else 0 end) as harga_total_9, \n"
				+ "sum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-10-01') and date(a.mulai) < date('" + tahun
				+ "-11-01') then a.harga_total else 0 end) as harga_total_10, \n"
				+ "sum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-11-01') and date(a.mulai) < date('" + tahun
				+ "-12-01') then a.harga_total else 0 end) as harga_total_11, \n"
				+ "sum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-12-01') and date(a.mulai) < date('" + (tahun + 1)
				+ "-01-01') then a.harga_total else 0 end) as harga_total_12 \n" + "from rab.workspace a \n"
				+ "where a.id in ("
				+ (childs.size() == 0 ? "-1" : childs.toString().replaceAll("\\[", "").replaceAll("\\]", "")) + ");";

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return session.createSQLQuery(sql).list();
		} finally {
			closeSession(session);
		}
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> getHargaTotalRealisasiTriWulan(Set<Long> childs, Integer tahun) {
		String sql = "select sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('"
				+ tahun + "-01-01') and date(b.tanggal_transaksi) < date('" + (tahun + 1)
				+ "-01-01') then b.kredit else 0 end) as total, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-01-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-04-01') then b.kredit else 0 end) as harga_total_1, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-04-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-07-01') then b.kredit else 0 end) as harga_total_2, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-07-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-10-01') then b.kredit else 0 end) as harga_total_3, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-10-01') and date(b.tanggal_transaksi) < date('" + (tahun + 1)
				+ "-01-01') then b.kredit else 0 end) as harga_total_4, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-01-01') and date(b.tanggal_transaksi) < date('" + (tahun + 1)
				+ "-01-01') then c.volume else 0 end) as total_volume, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-01-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-04-01') then c.volume else 0 end) as volume_total_1, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-04-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-07-01') then c.volume else 0 end) as volume_total_2, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-07-01') and date(b.tanggal_transaksi) < date('" + tahun
				+ "-10-01') then c.volume else 0 end) as volume_total_3, \n"
				+ "sum(case when b.tanggal_transaksi is not null and date(b.tanggal_transaksi) >= date('" + tahun
				+ "-10-01') and date(b.tanggal_transaksi) < date('" + (tahun + 1)
				+ "-01-01') then c.volume else 0 end) as volume_total_4 \n"
				+ "from akunting.workspace_has_transaksi a \n"
				+ "inner join akunting.transaksi b on (a.transaksi = b.id) \n"
				+ "inner join rab.workspace c on (a.workspace = c.id) \n" + "where c.id in ("
				+ (childs.size() == 0 ? "-1" : childs.toString().replaceAll("\\[", "").replaceAll("\\]", "")) + ");";

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return session.createSQLQuery(sql).list();
		} finally {
			closeSession(session);
		}
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> getHargaTotalPerencanaanTriWulan(Set<Long> childs, Integer tahun) {
		String sql = "select \nsum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-01-01') and date(a.mulai) < date('" + (tahun + 1)
				+ "-01-01') then a.harga_total else 0 end) as total, \n"
				+ "sum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-01-01') and date(a.mulai) < date('" + tahun
				+ "-04-01') then a.harga_total else 0 end) as harga_total_1, \n"
				+ "sum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-04-01') and date(a.mulai) < date('" + tahun
				+ "-07-01') then a.harga_total else 0 end) as harga_total_2, \n"
				+ "sum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-07-01') and date(a.mulai) < date('" + tahun
				+ "-10-01') then a.harga_total else 0 end) as harga_total_3, \n"
				+ "sum(case when a.mulai is not null and date(a.mulai) >= date('" + tahun
				+ "-10-01') and date(a.mulai) < date('" + (tahun + 1)
				+ "-01-01') then a.harga_total else 0 end) as harga_total_4 \n" + "from rab.workspace a \n"
				+ "where a.id in ("
				+ (childs.size() == 0 ? "-1" : childs.toString().replaceAll("\\[", "").replaceAll("\\]", "")) + ");";

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return session.createSQLQuery(sql).list();
		} finally {
			closeSession(session);
		}
	}

	public Double getRealisasi(Workspace workspace) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return getRealisasi(workspace, session);
		} finally {
			closeSession(session);
		}
	}

	private Double getRealisasi(Workspace workspace, Session session) {
		Set<Workspace> workspaces = new HashSet<Workspace>();
		generateChildsRecursive(workspace, workspaces, session);

		Criterion criterion = Restrictions.sqlRestriction("false");
		for (Workspace w : workspaces) {
			criterion = Restrictions.or(criterion,
					Restrictions.ilike("grupTransaksi.angarans", "," + w.getId() + ",", MatchMode.ANYWHERE));
		}

		Number count = ((Number) session.createCriteria(Transaksi.class).createAlias("grupTransaksi", "grupTransaksi")
				.add(Restrictions.or(workspaces.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("grupTransaksi.workspace", workspaces), criterion))
				.setProjection(Projections.sum("kredit")).uniqueResult());
		return count == null ? 0.0 : count.doubleValue();
	}

	public static Double getRealisasi(Integer tahun, Collection<SatuanKerja> satuanKerjas, SumberDana sumberDana,
			Integer revisi, Collection<Akun> akuns, Akun akun) {
		if (akun.getDebetCredit() == null) {
			return 0.0;
		}

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Number count = ((Number) session.createCriteria(Transaksi.class)
					.createAlias("grupTransaksi", "grupTransaksi").createAlias("grupTransaksi.workspace", "workspace")
					.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("workspace.sumberDana", sumberDana))
					.add(Restrictions.eq("workspace.tahunWorkspace", tahun))
					.add(Restrictions.eq("workspace.revisi", revisi))
					.add(Restrictions.in("workspace.satuanKerja", satuanKerjas)).add(Restrictions.in("akun", akuns))
					.setProjection(akun.getDebetCredit().equals(Akun.DEBET) ? Projections.sum("kredit")
							: Projections.sum("debet"))
					.uniqueResult());
			return count == null ? 0.0 : count.doubleValue();
		} finally {
			closeSession(session);
		}
	}

	public Double getRealisasi(Workspace workspace, Date sampai) {
		Set<Workspace> workspaces = new HashSet<Workspace>();
		generateChilds(workspace, workspaces);

		Criterion criterion = Restrictions.sqlRestriction("false");
		for (Workspace workspace2 : workspaces) {
			criterion = Restrictions.or(criterion,
					Restrictions.ilike("grupTransaksi.angarans", "," + workspace2.getId() + ",", MatchMode.ANYWHERE));
		}

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Number count = ((Number) session.createCriteria(Transaksi.class)
					.createAlias("grupTransaksi", "grupTransaksi")
					.add(Restrictions.lt("grupTransaksi.tanggalTransaksi", sampai))
					.add(Restrictions.or(workspaces.isEmpty() ? Restrictions.sqlRestriction("false")
							: Restrictions.in("grupTransaksi.workspace", workspaces), criterion))
					.setProjection(Projections.sum("debet")).uniqueResult());
			return count == null ? 0.0 : count.doubleValue();
		} finally {
			closeSession(session);
		}
	}

	public Double getRealisasi(Workspace workspace, Date mulai, Date sampai) {
		Set<Workspace> workspaces = new HashSet<Workspace>();
		generateChilds(workspace, workspaces);

		Criterion criterion = Restrictions.sqlRestriction("false");
		for (Workspace workspace2 : workspaces) {
			criterion = Restrictions.or(criterion,
					Restrictions.ilike("grupTransaksi.angarans", "," + workspace2.getId() + ",", MatchMode.ANYWHERE));
		}

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Number count = ((Number) session.createCriteria(Transaksi.class)
					.createAlias("grupTransaksi", "grupTransaksi")
					.add(Restrictions.lt("grupTransaksi.tanggalTransaksi", sampai))
					.add(Restrictions.gt("grupTransaksi.tanggalTransaksi", mulai))
					.add(Restrictions.or(workspaces.isEmpty() ? Restrictions.sqlRestriction("false")
							: Restrictions.in("grupTransaksi.workspace", workspaces), criterion))
					.setProjection(Projections.sum("kredit")).uniqueResult());
			return count == null ? 0.0 : count.doubleValue();
		} finally {
			closeSession(session);
		}
	}

	public static Integer getJumlahJurnal(Workspace workspace) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			int count = ((Number) session.createCriteria(PenggunaanAnggaran.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("workspace", workspace)).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();
			return count;
		} finally {
			closeSession(session);
		}
	}

	@Override
	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(Long root, Long workspaceId, Long parentId, List<Long> longs) {
		if (root != null && parentId != null && root.equals(parentId)) {
			return;
		}
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			getParentCountRecursive(root, workspaceId, parentId, longs, session);
		} finally {
			closeSession(session);
		}
	}

	private void getParentCountRecursive(Long root, Long workspaceId, Long parentId, List<Long> longs,
			Session session) {
		if (root != null && parentId != null && root.equals(parentId))
			return;

		ProjectionList projectionList = Projections.projectionList();
		projectionList.add(Projections.property("id"));
		projectionList.add(Projections.property("parentId"));
		Object[] parentWorkspace = (Object[]) session.createCriteria(Workspace.class)
				.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("carryOver", true),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.idEq(parentId)).setProjection(projectionList).uniqueResult();
		if (parentWorkspace != null && parentWorkspace.length != 0) {
			Long id = (Long) parentWorkspace[0];
			Long parent = (Long) parentWorkspace[1];
			longs.add(id);
			getParentCountRecursive(root, id, parent, longs, session);
		}
	}

	public void getParentSet(Workspace workspace, TreeSet<Workspace> longs) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			getParentSetRecursive(workspace, longs, session);
		} finally {
			closeSession(session);
		}
	}

	private void getParentSetRecursive(Workspace workspace, TreeSet<Workspace> longs, Session session) {
		Long parentId = WorkspaceTreeModel.checkForParent(workspace.getTahunWorkspace(), workspace.getSatuanKerja(),
				workspace.getRevisi(), termasukYgNonAktif);
		if (workspace.getParentId() == null || workspace.getParentId().equals(parentId)) {
			longs.add(workspace);
		} else {
			Workspace parentWorkspace = (Workspace) session.createCriteria(Workspace.class)
					.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.eq("carryOver", true),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.idEq(workspace.getParentId())).uniqueResult();
			longs.add(parentWorkspace);
			getParentSetRecursive(parentWorkspace, longs, session);
		}
	}

	public void getParentSetName(Long parentId, Long workspace, List<String> longs) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			getParentSetNameRecursive(parentId, workspace, longs, session);
		} finally {
			closeSession(session);
		}
	}

	private void getParentSetNameRecursive(Long parentId, Long workspace, List<String> longs, Session session) {
		ProjectionList projectionList = Projections.projectionList();
		projectionList.add(Projections.property("id"));
		projectionList.add(Projections.property("nama"));
		projectionList.add(Projections.property("parentId"));
		Object[] parentWorkspace = (Object[]) session.createCriteria(Workspace.class)
				.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("carryOver", true),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.idEq(workspace)).setProjection(projectionList).uniqueResult();
		if (parentWorkspace != null && parentWorkspace.length != 0) {
			String name = (String) parentWorkspace[1];
			Long parent = (Long) parentWorkspace[2];
			longs.add(name);
			getParentSetNameRecursive(parentId, parent, longs, session);
		}
	}

	public void getChildDeepSet(Long parent, List<Long> longs) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			getChildDeepSetRecursive(parent, longs, session);
		} finally {
			closeSession(session);
		}
	}

	@SuppressWarnings("unchecked")
	private void getChildDeepSetRecursive(Long parent, List<Long> longs, Session session) {
		List<Long> values = session.createCriteria(Workspace.class)
				.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("carryOver", true),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.eq("parentId", parent)).setProjection(Projections.property("id")).list();
		if (values.size() > 0) {
			longs.add(values.get(0));
			getChildDeepSetRecursive(values.get(0), longs, session);
		}
	}

	@Override
	public int getIndexOfChild(Object arg0, Object arg1) {
		return 0;
	}

	public Integer getTahunWorkspace() {
		return tahunWorkspace;
	}

	@SuppressWarnings({ "unchecked", "unused" })
	public WorkspacePunyaPegawai checkBentrok(Workspace workspace, Date mulai, Date selesai) {
		if (true) {
			return null; // Mempertahankan logic bawaan original (meski dead-code)
		}

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			List<WorkspacePunyaPegawai> workspacePunyaPegawais = session.createCriteria(WorkspacePunyaPegawai.class)
					.add(Restrictions.eq("workspace", workspace)).list();

			for (WorkspacePunyaPegawai workspacePunyaPegawai : workspacePunyaPegawais) {
				Pegawai pegawai = workspacePunyaPegawai.getPegawai();
				if (workspace == null || pegawai == null)
					continue;

				WorkspacePunyaPegawai count = ((WorkspacePunyaPegawai) session
						.createCriteria(WorkspacePunyaPegawai.class).add(Restrictions.eq("pegawai", pegawai))
						.add(workspacePunyaPegawai.getId() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.ne("id", workspacePunyaPegawai.getId()))
						.createCriteria("workspace").add(Restrictions.eq("leaf", true))
						.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.in("satuanKerja", satuanKerjas)))
						.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("sumberDana", sumberDana))
						.add(Restrictions.eq("revisi", revisi)).add(
								tahunWorkspace == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("tahunWorkspace", tahunWorkspace))
						.add(Restrictions.or(
								mulai == null ? Restrictions.sqlRestriction("1!=1")
										: Restrictions.sqlRestriction("date('" + Common.databaseDateFormat.get().format(mulai)
												+ "') between date(mulai) and date(selesai)"),
								selesai == null ? Restrictions.sqlRestriction("1!=1")
										: Restrictions
												.sqlRestriction("date('" + Common.databaseDateFormat.get().format(selesai)
														+ "') between date(mulai) and date(selesai)")))
						.setMaxResults(1).uniqueResult());
				if (count != null) {
					return count;
				}
			}
		} finally {
			closeSession(session);
		}
		return null;
	}

	public WorkspacePunyaPegawai checkBentrok(WorkspacePunyaPegawai workspacePunyaPegawai) {
		Workspace workspace = workspacePunyaPegawai.getWorkspace();
		Pegawai pegawai = workspacePunyaPegawai.getPegawai();
		if (workspace == null || pegawai == null) {
			return null;
		}
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			WorkspacePunyaPegawai count = ((WorkspacePunyaPegawai) session.createCriteria(WorkspacePunyaPegawai.class)
					.add(Restrictions.eq("pegawai", pegawai))
					.add(workspacePunyaPegawai.getId() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ne("id", workspacePunyaPegawai.getId()))
					.createCriteria("workspace").add(Restrictions.eq("leaf", true))
					.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
							Restrictions.in("satuanKerja", satuanKerjas)))
					.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("sumberDana", sumberDana))
					.add(Restrictions.eq("revisi", revisi)).add(
							tahunWorkspace == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("tahunWorkspace", tahunWorkspace))
					.add(Restrictions.or(
							workspace.getMulai() == null ? Restrictions.sqlRestriction("1!=1")
									: Restrictions.sqlRestriction(
											"date('" + Common.databaseDateFormat.get().format(workspace.getMulai())
													+ "') between date(mulai) and date(selesai)"),
							workspace.getSelesai() == null ? Restrictions.sqlRestriction("1!=1")
									: Restrictions.sqlRestriction(
											"date('" + Common.databaseDateFormat.get().format(workspace.getSelesai())
													+ "') between date(mulai) and date(selesai)")))
					.setMaxResults(1).uniqueResult());
			return count;
		} finally {
			closeSession(session);
		}
	}

	public Double getPersenKomplit(Object parent) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return getPersenKomplit((Workspace) parent, session);
		} finally {
			closeSession(session);
		}
	}

	private Double getPersenKomplit(Workspace parentWorkspace, Session session) {
		if (getChildCount(parentWorkspace, session) != 0
				|| parentWorkspace.getRevisi().equals(RabUtil.DEFAULT_REVISI)) {
			Double mulai = ((Double) session.createCriteria(Workspace.class)
					.add(termasukYgNonAktif ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.eq("carryOver", true),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
					.add(Restrictions.eq("parentId", parentWorkspace.getId()))
					.setProjection(Projections.avg("persenKomplit")).uniqueResult());
			return mulai == null ? 0.0 : mulai;
		} else {
			return parentWorkspace.getPersenKomplit() == null ? 0.0 : parentWorkspace.getPersenKomplit();
		}
	}

	public Workspace getJenisWorkspace(TreeSet<Workspace> workspaces, String contains) {
		Workspace myworkspace = null;
		for (Workspace workspace : workspaces) {
			if (workspace.getJenisWorkspace() != null && workspace.getJenisWorkspace().getNama() != null
					&& workspace.getJenisWorkspace().getNama().toLowerCase().contains(contains.toLowerCase())) {
				myworkspace = workspace;
			}
		}
		return myworkspace;
	}

	public Set<SatuanKerja> getSatuanKerjas() {
		return satuanKerjas;
	}

	public SatuanKerjaTreeModel getSatuanKerjaTreeModel() {
		return satuanKerjaTreeModel;
	}
}