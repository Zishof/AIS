package ais.action.master.lkp.util;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.lkp.KegiatanTugasJabatan;
import ais.database.model.rab.SatuanKerja;

public class KegiatanTugasJabatanTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private Boolean tampilkanSemua;
	private SatuanKerja satuanKerja;

	private String periode = KegiatanTugasJabatan.BULANAN;
	private List<Tbmrole> tbmroles = null;

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public KegiatanTugasJabatanTreeModel(Boolean tampilkanSemua, String periode) {
		super(null);
		this.periode = periode;
		this.tampilkanSemua = tampilkanSemua;
	}

	public void setSatuanKerja(SatuanKerja satuanKerja, List<Tbmrole> tbmroles) throws Exception {
		this.satuanKerja = satuanKerja;
		this.tbmroles = tbmroles;
	}

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public KegiatanTugasJabatanTreeModel(KegiatanTugasJabatan indukKegiatanTugasJabatan, Boolean tampilkanSemua,
			String periode) {
		super(indukKegiatanTugasJabatan);
		this.periode = periode;
		this.tampilkanSemua = tampilkanSemua;
	}

	public List<KegiatanTugasJabatan> getChildren(KegiatanTugasJabatan indukKegiatanTugasJabatan) {
		return getChildren(indukKegiatanTugasJabatan, null);
	}

	@SuppressWarnings("unchecked")
	public List<KegiatanTugasJabatan> getChildren(KegiatanTugasJabatan indukKegiatanTugasJabatan, Integer index) {
		Session session = HibernateUtil.currentSession();
		Criterion criterion = satuanKerja == null ? Restrictions.sqlRestriction("false")
				: Restrictions.eq("satuanKerja", satuanKerja);
		List<KegiatanTugasJabatan> kegiatanTugasJabatans =

				ConstantValues.simpleList(session.createCriteria(KegiatanTugasJabatan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(periode.equals(KegiatanTugasJabatan.BULANAN)
								? Restrictions.or(Restrictions.isNull("periode"), Restrictions.eq("periode", periode))
								: Restrictions.eq("periode", periode))

						.add(Restrictions.or(criterion,
								tbmroles == null || tbmroles.isEmpty() ? Restrictions.sqlRestriction("false")
										: Restrictions.or(Restrictions.and(criterion, Restrictions.isNull("userRole")),
												Restrictions.in("userRole", tbmroles))))

						.setMaxResults(index == null ? 10000 : 1).setFirstResult(index == null ? 0 : index)
						.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
						.add(indukKegiatanTugasJabatan == null ? Restrictions.isNull("induk")
								: Restrictions.eq("induk", indukKegiatanTugasJabatan))
						.addOrder(Order.asc("nama")).addOrder(Order.desc("id")), KegiatanTugasJabatan.class);
		return kegiatanTugasJabatans;
	}

	public List<Long> getChildrenByIds(Long indukKegiatanTugasJabatan) {
		return getChildrenByIds(indukKegiatanTugasJabatan, null);
	}

	@SuppressWarnings("unchecked")
	public List<Long> getChildrenByIds(Long indukKegiatanTugasJabatan, Integer index) {
		Session session = HibernateUtil.currentSession();
		Criterion criterion = satuanKerja == null ? Restrictions.sqlRestriction("false")
				: Restrictions.eq("satuanKerja", satuanKerja);
		List<Long> kegiatanTugasJabatans = session.createCriteria(KegiatanTugasJabatan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(periode.equals(KegiatanTugasJabatan.BULANAN)
						? Restrictions.or(Restrictions.isNull("periode"), Restrictions.eq("periode", periode))
						: Restrictions.eq("periode", periode))

				.add(Restrictions.or(criterion,
						tbmroles == null || tbmroles.isEmpty() ? Restrictions.sqlRestriction("false")
								: Restrictions.or(Restrictions.and(criterion, Restrictions.isNull("userRole")),
										Restrictions.in("userRole", tbmroles))))

				.setProjection(Projections.property("id")).setMaxResults(index == null ? 10000 : 1)
				.setFirstResult(index == null ? 0 : index)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
				.add(indukKegiatanTugasJabatan == null ? Restrictions.isNull("induk")
						: Restrictions.eq("induk.id", indukKegiatanTugasJabatan))
				.addOrder(Order.asc("nama")).addOrder(Order.desc("id")).list();
		return kegiatanTugasJabatans;
	}

	public void generateAllChildren(KegiatanTugasJabatan indukKegiatanTugasJabatan,
			Set<KegiatanTugasJabatan> kegiatanTugasJabatans) {
		if (!isLeaf(indukKegiatanTugasJabatan)) {
			List<KegiatanTugasJabatan> kerjas = getChildren(indukKegiatanTugasJabatan);
			for (KegiatanTugasJabatan kegiatanTugasJabatan : kerjas) {
				kegiatanTugasJabatans.add(kegiatanTugasJabatan);
				generateAllChildren(kegiatanTugasJabatan, kegiatanTugasJabatans);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object induk, int index) {
		KegiatanTugasJabatan indukKegiatanTugasJabatan = (KegiatanTugasJabatan) induk;
		List<KegiatanTugasJabatan> kegiatanTugasJabatans = getChildren(indukKegiatanTugasJabatan, index);
		KegiatanTugasJabatan kegiatanTugasJabatan = kegiatanTugasJabatans.size() > 0 ? kegiatanTugasJabatans.get(0)
				: null;
		// System.out.println("index = " + index + ", kegiatanTugasJabatan = "
		// + kegiatanTugasJabatan);
		return kegiatanTugasJabatan;
	}

	public int getChildCount(Object induk) {
		KegiatanTugasJabatan indukKegiatanTugasJabatan = (KegiatanTugasJabatan) induk;
		Session session = HibernateUtil.currentNativeSession();
		Criterion criterion = satuanKerja == null ? Restrictions.sqlRestriction("false")
				: Restrictions.eq("satuanKerja", satuanKerja);
		Integer count = ((Number) session.createCriteria(KegiatanTugasJabatan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(periode.equals(KegiatanTugasJabatan.BULANAN)
						? Restrictions.or(Restrictions.isNull("periode"), Restrictions.eq("periode", periode))
						: Restrictions.eq("periode", periode))

				.add(Restrictions.or(criterion,
						tbmroles == null || tbmroles.isEmpty() ? Restrictions.sqlRestriction("false")
								: Restrictions.or(Restrictions.and(criterion, Restrictions.isNull("userRole")),
										Restrictions.in("userRole", tbmroles))))

				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))

				.add(indukKegiatanTugasJabatan == null ? Restrictions.isNull("induk")
						: Restrictions.eq("induk", indukKegiatanTugasJabatan))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}

		HibernateUtil.closeSession();
		return count;
	}

	public int getChildCountByIds(Long induk) {
		Session session = HibernateUtil.currentNativeSession();
		Criterion criterion = satuanKerja == null ? Restrictions.sqlRestriction("false")
				: Restrictions.eq("satuanKerja", satuanKerja);
		Integer count = ((Number) session.createCriteria(KegiatanTugasJabatan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(periode.equals(KegiatanTugasJabatan.BULANAN)
						? Restrictions.or(Restrictions.isNull("periode"), Restrictions.eq("periode", periode))
						: Restrictions.eq("periode", periode))

				.add(Restrictions.or(criterion,
						tbmroles == null || tbmroles.isEmpty() ? Restrictions.sqlRestriction("false")
								: Restrictions.or(Restrictions.and(criterion, Restrictions.isNull("userRole")),
										Restrictions.in("userRole", tbmroles))))

				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))

				.add(induk == null ? Restrictions.isNull("induk") : Restrictions.eq("induk.id", induk))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
		return count;
	}

	public void deleteChilds(Object induk) {
		KegiatanTugasJabatan indukKegiatanTugasJabatan = (KegiatanTugasJabatan) induk;
		Session session = HibernateUtil.currentSession();
		List<KegiatanTugasJabatan> kegiatanTugasJabatans = getChildren(indukKegiatanTugasJabatan);
		for (KegiatanTugasJabatan kegiatanTugasJabatan : kegiatanTugasJabatans) {
			if (getChildCount(kegiatanTugasJabatan) == 0) {
				session.delete(kegiatanTugasJabatan);
			} else {
				deleteChilds(kegiatanTugasJabatan);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getIndukCount(KegiatanTugasJabatan kegiatanTugasJabatan, KegiatanTugasJabatan obj, List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (kegiatanTugasJabatan.getInduk() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session, (obj));
		} else {
			Criterion criterion = satuanKerja == null ? Restrictions.sqlRestriction("false")
					: Restrictions.eq("satuanKerja", satuanKerja);
			KegiatanTugasJabatan indukKegiatanTugasJabatan = (KegiatanTugasJabatan)

			ConstantValues.simpleObject(

					session.createCriteria(KegiatanTugasJabatan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(periode.equals(KegiatanTugasJabatan.BULANAN) ? Restrictions
									.or(Restrictions.isNull("periode"), Restrictions.eq("periode", periode))
									: Restrictions.eq("periode", periode))

							.add(Restrictions.or(criterion, tbmroles == null || tbmroles.isEmpty()
									? Restrictions.sqlRestriction("false")
									: Restrictions.or(Restrictions.and(criterion, Restrictions.isNull("userRole")),
											Restrictions.in("userRole", tbmroles))))

							.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
							.add(Restrictions.idEq(kegiatanTugasJabatan.getInduk().getId())),
					KegiatanTugasJabatan.class);
			longs.add(indukKegiatanTugasJabatan.getId());
			getIndukCount(indukKegiatanTugasJabatan, obj, longs);
		}

	}

	public void getIndukSet(KegiatanTugasJabatan kegiatanTugasJabatan,
			List<KegiatanTugasJabatan> kegiatanTugasJabatans) {
		Session session = HibernateUtil.currentSession();
		if (kegiatanTugasJabatan.getInduk() != null) {
			Criterion criterion = satuanKerja == null ? Restrictions.sqlRestriction("false")
					: Restrictions.eq("satuanKerja", satuanKerja);
			KegiatanTugasJabatan indukKegiatanTugasJabatan = (KegiatanTugasJabatan) ConstantValues
					.simpleObject(
							session.createCriteria(KegiatanTugasJabatan.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

									.add(periode.equals(KegiatanTugasJabatan.BULANAN)
											? Restrictions.or(Restrictions.isNull("periode"),
													Restrictions.eq("periode", periode))
											: Restrictions.eq("periode", periode))

									.add(Restrictions.or(criterion, tbmroles == null || tbmroles.isEmpty()
											? Restrictions.sqlRestriction("false")
											: Restrictions.or(
													Restrictions.and(criterion, Restrictions.isNull("userRole")),
													Restrictions.in("userRole", tbmroles))))

									.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
											: Restrictions.eq("aktif", true))
									.add(Restrictions.idEq(kegiatanTugasJabatan.getInduk().getId())),
							KegiatanTugasJabatan.class);
			if (indukKegiatanTugasJabatan != null) {
				kegiatanTugasJabatans.add(indukKegiatanTugasJabatan);
				getIndukSet(indukKegiatanTugasJabatan, kegiatanTugasJabatans);
			}
		}

	}

	public void generateChildsByIds(Long kegiatanTugasJabatan, Set<Long> childs) {
		List<Long> mychilds = getChildrenByIds(kegiatanTugasJabatan);
		if (mychilds.size() > 0) {
			for (Long myKegiatanTugasJabatan : mychilds) {
				int count = getChildCountByIds(myKegiatanTugasJabatan);
				if (count == 0) {
					childs.add(myKegiatanTugasJabatan);
				} else {
					generateChildsByIds(myKegiatanTugasJabatan, childs);
				}
			}
		} else {
			childs.add(kegiatanTugasJabatan);
		}
	}

	public void getChildsSet(KegiatanTugasJabatan kegiatanTugasJabatan,
			Set<KegiatanTugasJabatan> kegiatanTugasJabatans) {
		List<KegiatanTugasJabatan> childs = getChildren(kegiatanTugasJabatan);
		for (KegiatanTugasJabatan myKegiatanTugasJabatan : childs) {
			kegiatanTugasJabatans.add(myKegiatanTugasJabatan);
			if (!isLeaf(myKegiatanTugasJabatan)) {
				getChildsSet(myKegiatanTugasJabatan, kegiatanTugasJabatans);
			}
		}
	}

	/**
	 * @since 5.0.6
	 * @see org.zkoss.zul.TreeModel#getIndexOfChild(java.lang.Object,
	 *      java.lang.Object)
	 */
	@Override
	public int getIndexOfChild(Object arg0, Object arg1) {
		return 0;
	}

}