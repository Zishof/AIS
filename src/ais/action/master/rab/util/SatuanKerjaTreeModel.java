package ais.action.master.rab.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Yayasan;

public class SatuanKerjaTreeModel extends AbstractTreeModel {

	private static final long serialVersionUID = -5115651721345571411L;
	private Boolean tampilkanSemua;
	private Yayasan yayasan;
	private String satuanKerjaPengguna;
	
	// OPTIMASI MEMORY: Simpan hasil split array agar tidak diproses berulang-ulang saat render UI
	private String[] kodeSatuanKerjas;

	public static Yayasan ambilYayasan() {
		return SekolahUtil.getYayasan();
	}

	private void initKodeSatuanKerja() {
		if (this.satuanKerjaPengguna != null && !this.satuanKerjaPengguna.trim().isEmpty()) {
			this.kodeSatuanKerjas = this.satuanKerjaPengguna.split(",");
		} else {
			this.kodeSatuanKerjas = new String[0];
		}
	}

	/**
	 * Constructor
	 */
	public SatuanKerjaTreeModel(Boolean tampilkanSemua) {
		super(null);
		this.tampilkanSemua = tampilkanSemua;
		Tbmuser tbmuser = Common.getCurrentUser();
		this.satuanKerjaPengguna = tbmuser == null || tbmuser.hakAkses() == null ? ""
				: tbmuser.hakAkses().getSatuanKerjas();
		this.yayasan = ambilYayasan();
		initKodeSatuanKerja();
	}

	/**
	 * Constructor
	 */
	public SatuanKerjaTreeModel(Boolean tampilkanSemua, String satuanKerjaPengguna) {
		super(null);
		this.tampilkanSemua = tampilkanSemua;
		this.satuanKerjaPengguna = satuanKerjaPengguna;
		this.yayasan = ambilYayasan();
		initKodeSatuanKerja();
	}

	/**
	 * Constructor
	 */
	public SatuanKerjaTreeModel(SatuanKerja parentSatuanKerja, Boolean tampilkanSemua) {
		super(parentSatuanKerja);
		this.yayasan = ambilYayasan();
		this.tampilkanSemua = tampilkanSemua;
		Tbmuser tbmuser = Common.getCurrentUser();
		this.satuanKerjaPengguna = tbmuser == null || tbmuser.hakAkses() == null ? ""
				: tbmuser.hakAkses().getSatuanKerjas();
		initKodeSatuanKerja();
	}

	/**
	 * Constructor
	 */
	public SatuanKerjaTreeModel(SatuanKerja parentSatuanKerja, String satuanKerjaPengguna, Boolean tampilkanSemua) {
		super(parentSatuanKerja);
		this.yayasan = ambilYayasan();
		this.tampilkanSemua = tampilkanSemua;
		this.satuanKerjaPengguna = satuanKerjaPengguna;
		initKodeSatuanKerja();
	}

	// ====================================================================================
	// HELPER METHOD (DRY Principle & Keamanan Session)
	// ====================================================================================

	/**
	 * Helper untuk menutup koneksi session secara aman
	 */
	private void closeSessionSafe(Session session) {
		if (session != null && session.isOpen()) {
			try {
				session.disconnect();
				session.close();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/SatuanKerjaTreeModel.java:107");
			}
		}
	}

	/**
	 * Helper agar kriteria Yayasan, Akses Pengguna, dan Default Item tidak ditulis berulang-ulang
	 */
	private void applyBaseRestrictions(Criteria criteria) {
		Criterion criterion = Restrictions.sqlRestriction(kodeSatuanKerjas.length == 0 ? "true" : "false");
		for (String s : kodeSatuanKerjas) {
			if (!s.trim().isEmpty()) {
				criterion = Restrictions.or(criterion, Restrictions.ilike("kode", s.trim(), MatchMode.EXACT));
			}
		}
		criteria.add(criterion);

		if (yayasan == null || yayasan.getId() == null) {
			criteria.add(Restrictions.isNull("yayasan"));
		} else {
			criteria.add(Restrictions.eq("yayasan", yayasan));
		}

		if (tampilkanSemua != null && tampilkanSemua) {
			criteria.add(Restrictions.sqlRestriction("1=1"));
		} else {
			criteria.add(Restrictions.eq("defaultItem", true));
		}
	}

	// ====================================================================================
	// METODE UTAMA PENGAMBILAN DATA
	// ====================================================================================

	public List<SatuanKerja> getChildren(SatuanKerja parentSatuanKerja) {
		return getChildren(parentSatuanKerja, null);
	}

	@SuppressWarnings("unchecked")
	public List<SatuanKerja> getChildren(SatuanKerja parentSatuanKerja, Integer index) {
		Session session = null;
		List<SatuanKerja> satuanKerjas = new ArrayList<SatuanKerja>();
		try {
			session = HibernateUtil.currentNativeSession();
			Criteria criteria = session.createCriteria(SatuanKerja.class);
			applyBaseRestrictions(criteria);

			if (parentSatuanKerja == null) {
				criteria.add(Restrictions.isNull("parent"));
			} else {
				criteria.add(Restrictions.eq("parent", parentSatuanKerja));
			}

			criteria.setMaxResults(index == null ? 10000 : 1);
			criteria.setFirstResult(index == null ? 0 : index);
			criteria.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")).addOrder(Order.desc("id"));

			satuanKerjas = ConstantValues.simpleList(criteria, SatuanKerja.class);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/SatuanKerjaTreeModel.java:166");
		} finally {
			closeSessionSafe(session);
		}
		return satuanKerjas;
	}

	public List<Long> getChildrenByIds(Long parentSatuanKerja) {
		return getChildrenByIds(parentSatuanKerja, null);
	}

	@SuppressWarnings("unchecked")
	public List<Long> getChildrenByIds(Long parentSatuanKerja, Integer index) {
		Session session = null;
		List<Long> satuanKerjas = new ArrayList<Long>();
		try {
			session = HibernateUtil.currentNativeSession();
			Criteria criteria = session.createCriteria(SatuanKerja.class);
			applyBaseRestrictions(criteria);

			if (parentSatuanKerja == null) {
				criteria.add(Restrictions.isNull("parent"));
			} else {
				criteria.add(Restrictions.eq("parent.id", parentSatuanKerja));
			}

			criteria.setProjection(Projections.property("id"));
			criteria.setMaxResults(index == null ? 10000 : 1);
			criteria.setFirstResult(index == null ? 0 : index);
			criteria.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")).addOrder(Order.desc("id"));

			satuanKerjas = criteria.list();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/SatuanKerjaTreeModel.java:199");
		} finally {
			closeSessionSafe(session);
		}
		return satuanKerjas;
	}

	public void generateAllChildren(SatuanKerja parentSatuanKerja, Set<SatuanKerja> satuanKerjas) {
		if (!isLeaf(parentSatuanKerja)) {
			List<SatuanKerja> kerjas = getChildren(parentSatuanKerja);
			for (SatuanKerja satuanKerja : kerjas) {
				satuanKerjas.add(satuanKerja);
				generateAllChildren(satuanKerja, satuanKerjas);
			}
		}
	}

	// ====================================================================================
	// METODE OVERRIDE TREEMODEL (ZKOSS)
	// ====================================================================================

	public Object getChild(Object parent, int index) {
		SatuanKerja parentSatuanKerja = (SatuanKerja) parent;
		List<SatuanKerja> satuanKerjas = getChildren(parentSatuanKerja, index);
		return satuanKerjas.size() > 0 ? satuanKerjas.get(0) : null;
	}

	public int getChildCount(Object parent) {
		SatuanKerja parentSatuanKerja = (SatuanKerja) parent;
		Session session = null;
		int count = 0;
		try {
			session = HibernateUtil.currentNativeSession();
			Criteria criteria = session.createCriteria(SatuanKerja.class);
			applyBaseRestrictions(criteria);

			if (parentSatuanKerja == null) {
				criteria.add(Restrictions.isNull("parent"));
			} else {
				criteria.add(Restrictions.eq("parent", parentSatuanKerja));
			}

			criteria.setProjection(Projections.rowCount());
			Number result = (Number) criteria.uniqueResult();
			if (result != null) count = result.intValue();
			
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/SatuanKerjaTreeModel.java:246");
		} finally {
			closeSessionSafe(session);
		}
		return count;
	}

	public int getChildCountByIds(Long parent) {
		Session session = null;
		int count = 0;
		try {
			session = HibernateUtil.currentNativeSession();
			Criteria criteria = session.createCriteria(SatuanKerja.class);
			applyBaseRestrictions(criteria);

			if (parent == null) {
				criteria.add(Restrictions.isNull("parent"));
			} else {
				criteria.add(Restrictions.eq("parent.id", parent));
			}

			criteria.setProjection(Projections.rowCount());
			Number result = (Number) criteria.uniqueResult();
			if (result != null) count = result.intValue();
			
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/SatuanKerjaTreeModel.java:272");
		} finally {
			closeSessionSafe(session);
		}
		return count;
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	// ====================================================================================
	// METODE PENGELOLAAN DATA / TRANSACTION (REKURSIF)
	// ====================================================================================

	public void deleteChilds(Object parent) {
		SatuanKerja parentSatuanKerja = (SatuanKerja) parent;
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			
			// Eksekusi hapus menggunakan session yang sama agar tidak tabrakan
			deleteChildsRecursive(parentSatuanKerja, session);
			
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/SatuanKerjaTreeModel.java:302");
		} finally {
			closeSessionSafe(session);
		}
	}

	/**
	 * PERBAIKAN LOGIKA: Rekursif penghapusan menggunakan 1 session aktif.
	 * Jika sebuah child memiliki child lagi, hapus bawahnya dulu baru hapus dirinya.
	 */
	@SuppressWarnings("unchecked")
	private void deleteChildsRecursive(SatuanKerja parentSatuanKerja, Session session) {
		Criteria criteria = session.createCriteria(SatuanKerja.class);
		applyBaseRestrictions(criteria);
		criteria.add(Restrictions.eq("parent", parentSatuanKerja));
		
		List<SatuanKerja> satuanKerjas = criteria.list();
		for (SatuanKerja satuanKerja : satuanKerjas) {
			// Periksa apakah dia masih punya anak
			Criteria checkChild = session.createCriteria(SatuanKerja.class)
					.add(Restrictions.eq("parent", satuanKerja))
					.setProjection(Projections.rowCount());
			
			Number count = (Number) checkChild.uniqueResult();
			if (count != null && count.intValue() > 0) {
				// Jika punya anak, bersihkan anaknya dulu
				deleteChildsRecursive(satuanKerja, session);
			}
			
			// Perbaikan krusial: Setelah dipastikan leaf (atau anaknya sudah rata), hapus dirinya sendiri
			session.delete(satuanKerja);
		}
	}

	public void getParentCount(SatuanKerja satuanKerja, SatuanKerja obj, List<Long> longs) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			
			if (satuanKerja.getParent() == null) {
				obj.setDeep(longs.size());
				Common.refreshUpdate(session, obj);
			} else {
				Criteria criteria = session.createCriteria(SatuanKerja.class);
				applyBaseRestrictions(criteria);
				criteria.add(Restrictions.idEq(satuanKerja.getParent().getId()));
				
				SatuanKerja parentSatuanKerja = (SatuanKerja) ConstantValues.simpleObject(criteria, SatuanKerja.class);
				if (parentSatuanKerja != null) {
					longs.add(parentSatuanKerja.getId());
					// Lepas ke rekursif, catatan: transaction sebaiknya ditangani secara eksternal jika rekursif panjang
					getParentCount(parentSatuanKerja, obj, longs);
				}
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/SatuanKerjaTreeModel.java:362");
		} finally {
			closeSessionSafe(session);
		}
	}

	public void getParentSet(SatuanKerja satuanKerja, List<SatuanKerja> satuanKerjas) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			if (satuanKerja.getParent() != null) {
				Criteria criteria = session.createCriteria(SatuanKerja.class);
				applyBaseRestrictions(criteria);
				criteria.add(Restrictions.idEq(satuanKerja.getParent().getId()));

				SatuanKerja parentSatuanKerja = (SatuanKerja) ConstantValues.simpleObject(criteria, SatuanKerja.class);
				if (parentSatuanKerja != null) {
					satuanKerjas.add(parentSatuanKerja);
					getParentSet(parentSatuanKerja, satuanKerjas);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/SatuanKerjaTreeModel.java:384");
		} finally {
			closeSessionSafe(session);
		}
	}

	public void generateChildsByIds(Long satuanKerja, Set<Long> childs) {
		List<Long> mychilds = getChildrenByIds(satuanKerja);
		if (mychilds.size() > 0) {
			for (Long mySatuanKerja : mychilds) {
				int count = getChildCountByIds(mySatuanKerja);
				if (count == 0) {
					childs.add(mySatuanKerja);
				} else {
					generateChildsByIds(mySatuanKerja, childs);
				}
			}
		} else {
			childs.add(satuanKerja);
		}
	}

	public void getChildsSet(SatuanKerja satuanKerja, Set<SatuanKerja> satuanKerjas) {
		List<SatuanKerja> childs = getChildren(satuanKerja);
		for (SatuanKerja mySatuanKerja : childs) {
			satuanKerjas.add(mySatuanKerja);
			if (!isLeaf(mySatuanKerja)) {
				getChildsSet(mySatuanKerja, satuanKerjas);
			}
		}
	}

	/**
	 * @since 5.0.6
	 * @see org.zkoss.zul.TreeModel#getIndexOfChild(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int getIndexOfChild(Object arg0, Object arg1) {
		return 0;
	}

}