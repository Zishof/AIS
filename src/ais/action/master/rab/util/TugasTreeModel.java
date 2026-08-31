package ais.action.master.rab.util;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.rab.Proyek;
import ais.database.model.rab.Tugas;
import ais.database.model.rab.TugasPunyaPegawai;

/**
 * Tipe khusus untuk tugas tree model. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * AbstractTreeModel}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Integer revisi}, {@code Proyek proyek};
 * pembacaan/pencarian ({@code getCheckForLeafNulll()}, {@code getChildren()}, {@code getChild()}, {@code
 * getChildCount()}, {@code getMaksimalSelesai()}, {@code getMinimalMulai()}); validasi/perhitungan ({@code
 * checkBentrok()}, {@code checkBentrok()}); penghapusan/pembatalan ({@code deleteChilds()}); operasi domain lain
 * ({@code isLeaf()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see AbstractTreeModel
 */
public class TugasTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private Integer revisi = 1;
	private Proyek proyek;

	/**
	 * Constructor
	 * 
	 * @param tree
	 *            the list is contained all data of nodes.
	 */
	public TugasTreeModel(Integer revisi, Proyek proyek) {
		super(null);
		this.revisi = revisi;
		this.proyek = proyek;
		getCheckForLeafNulll();
	}

	@SuppressWarnings("unchecked")
	public void getCheckForLeafNulll() {
		Session session = HibernateUtil.currentNativeSession();
		List<Tugas> count = (session.createCriteria(Tugas.class)
				.add(Restrictions.eq("proyek", proyek))
				.add(Restrictions.eq("revisi", revisi))
				.add(Restrictions.isNull("leaf")).list());
		session.getTransaction().begin();
		for (Tugas tugas : count) {
			tugas.setLeaf(getChildCount(tugas) == 0);
			Common.refreshUpdate(session,(tugas));
		}
		session.getTransaction().commit();

		HibernateUtil.closeSession();
	}

	@SuppressWarnings("unchecked")
	public List<Tugas> getChildren(Tugas parentTugas) {
		Session session = HibernateUtil.currentSession();
		List<Tugas> tugass = session
				.createCriteria(Tugas.class)
				.add(Restrictions.eq("proyek", proyek))
				.add(Restrictions.eq("revisi", revisi))
				.add(parentTugas == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentTugas))
				.addOrder(Order.asc("mulai")).addOrder(Order.asc("nama"))
				.addOrder(Order.desc("id")).list();
		return tugass;
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		Tugas parentTugas = (Tugas) parent;

		List<Tugas> tugass = getChildren(parentTugas);

		Tugas tugas = null;

		try {
			if (tugass.size() < index) {
				tugas = null;
			} else {
				tugas = tugass.get(index);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/rab/util/TugasTreeModel.java:86");
		}

		return tugas;
	}

	public int getChildCount(Object parent) {
		Tugas parentTugas = (Tugas) parent;
		Session session = HibernateUtil.currentNativeSession();
		Integer count = ((Number) session
				.createCriteria(Tugas.class)
				.add(Restrictions.eq("proyek", proyek))
				.add(Restrictions.eq("revisi", revisi))
				.add(parentTugas == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentTugas))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		HibernateUtil.closeSession();
		return count;
	}

	@SuppressWarnings("unchecked")
	public TugasPunyaPegawai checkBentrok(Tugas tugas, Date mulai, Date selesai) {
		List<TugasPunyaPegawai> tugasPunyaPegawais = HibernateUtil
				.currentSession().createCriteria(TugasPunyaPegawai.class)
				.add(Restrictions.eq("tugas", tugas)).list();
		Session session = HibernateUtil.currentNativeSession();
		for (TugasPunyaPegawai tugasPunyaPegawai : tugasPunyaPegawais) {
			Pegawai pegawai = tugasPunyaPegawai.getPegawai();
			if (tugas == null || pegawai == null) {
				continue;
			}

			TugasPunyaPegawai count = ((TugasPunyaPegawai) session
					.createCriteria(TugasPunyaPegawai.class)
					.add(Restrictions.eq("pegawai", pegawai))

					.add(tugasPunyaPegawai.getId() == null ? Restrictions
							.sqlRestriction("1=1") : Restrictions.ne("id",
							tugasPunyaPegawai.getId()))

					.createCriteria("tugas")
					.add(Restrictions.eq("leaf", true))
					.add(Restrictions.eq("proyek", proyek))
					.add(Restrictions.eq("revisi", revisi))
					.add(Restrictions.or(
							mulai == null ? Restrictions.sqlRestriction("1!=1")
									: Restrictions
											.sqlRestriction("date('"
													+ Common.databaseDateFormat.get()
															.format(mulai)
													+ "') between date(mulai) and date(selesai)"),
							selesai == null ? Restrictions
									.sqlRestriction("1!=1")
									: Restrictions.sqlRestriction("date('"
											+ Common.databaseDateFormat.get()
													.format(selesai)
											+ "') between date(mulai) and date(selesai)")))

					.setMaxResults(1).uniqueResult());
			if (count != null) {

				HibernateUtil.closeSession();
				return count;
			}

		}

		HibernateUtil.closeSession();
		return null;
	}

	public TugasPunyaPegawai checkBentrok(TugasPunyaPegawai tugasPunyaPegawai) {
		Tugas tugas = tugasPunyaPegawai.getTugas();
		Pegawai pegawai = tugasPunyaPegawai.getPegawai();
		if (tugas == null || pegawai == null) {
			return null;
		}
		Session session = HibernateUtil.currentNativeSession();
		TugasPunyaPegawai count = ((TugasPunyaPegawai) session
				.createCriteria(TugasPunyaPegawai.class)
				.add(Restrictions.eq("pegawai", pegawai))

				.add(tugasPunyaPegawai.getId() == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ne("id",
						tugasPunyaPegawai.getId()))

				.createCriteria("tugas")
				.add(Restrictions.eq("leaf", true))
				.add(Restrictions.eq("proyek", proyek))
				.add(Restrictions.eq("revisi", revisi))
				.add(Restrictions.or(
						tugas.getMulai() == null ? Restrictions
								.sqlRestriction("1!=1")
								: Restrictions
										.sqlRestriction("date('"
												+ Common.databaseDateFormat.get()
														.format(tugas
																.getMulai())
												+ "') between date(mulai) and date(selesai)"),
						tugas.getSelesai() == null ? Restrictions
								.sqlRestriction("1!=1")
								: Restrictions.sqlRestriction("date('"
										+ Common.databaseDateFormat.get()
												.format(tugas.getSelesai())
										+ "') between date(mulai) and date(selesai)")))

				.setMaxResults(1).uniqueResult());

		HibernateUtil.closeSession();
		return count;
	}

	public void deleteChilds(Object parent) {
		Tugas parentTugas = (Tugas) parent;
		Session session = HibernateUtil.currentSession();
		List<Tugas> tugass = getChildren(parentTugas);
		for (Tugas tugas : tugass) {
			if (getChildCount(tugas) == 0) {
				session.delete(tugas);
			} else {
				deleteChilds(tugas);
			}
		}
	}

	public Date getMaksimalSelesai(Object parent) {
		Tugas parentTugas = (Tugas) parent;
		if (getChildCount(parentTugas) != 0) {
			Session session = HibernateUtil.currentNativeSession();
			Date selesai = ((Date) session
					.createCriteria(Tugas.class)
					.add(Restrictions.eq("proyek", proyek))
					.add(Restrictions.eq("revisi", revisi))
					.add(parentTugas == null ? Restrictions.isNull("parent")
							: Restrictions.eq("parent", parentTugas))
					.setProjection(Projections.max("selesai")).uniqueResult());

			HibernateUtil.closeSession();

			// System.out.println("parentTugas = " + parentTugas.getNama()
			// + " selesai = " + selesai);
			return selesai;
		} else {
			return parentTugas.getSelesai();
		}
	}

	public Date getMinimalMulai(Object parent) {
		Tugas parentTugas = (Tugas) parent;
		if (getChildCount(parentTugas) != 0) {
			Session session = HibernateUtil.currentNativeSession();
			Date mulai = ((Date) session
					.createCriteria(Tugas.class)
					.add(Restrictions.eq("proyek", proyek))
					.add(Restrictions.eq("revisi", revisi))
					.add(parentTugas == null ? Restrictions.isNull("parent")
							: Restrictions.eq("parent", parentTugas))
					.setProjection(Projections.min("mulai")).uniqueResult());

			HibernateUtil.closeSession();
			return mulai;
		} else {
			return parentTugas.getMulai();
		}
	}

	public Double getPersenKomplit(Object parent) {
		Tugas parentTugas = (Tugas) parent;
		if (getChildCount(parentTugas) != 0) {
			Session session = HibernateUtil.currentNativeSession();
			Double mulai = ((Double) session
					.createCriteria(Tugas.class)
					.add(Restrictions.eq("proyek", proyek))
					.add(Restrictions.eq("revisi", revisi))
					.add(parentTugas == null ? Restrictions.isNull("parent")
							: Restrictions.eq("parent", parentTugas))
					.setProjection(Projections.avg("persenKomplit"))
					.uniqueResult());

			HibernateUtil.closeSession();
			return mulai;
		} else {
			return parentTugas.getPersenKomplit();
		}
	}

	public Integer getDurasi(Object parent) {
		Date mulai = getMinimalMulai(parent);
		Date selesai = getMaksimalSelesai(parent);
		if (mulai != null && selesai != null) {
			int durasi = (int) ((selesai.getTime() - mulai.getTime()) / (1000 * 60 * 60 * 24));
			return durasi;
		} else {
			return 0;
		}
	}

	@SuppressWarnings("unchecked")
	public List<TugasPunyaPegawai> getPagawais(Object parent) {
		Tugas parentTugas = (Tugas) parent;
		if (getChildCount(parentTugas) != 0) {
			Session session = HibernateUtil.currentNativeSession();
			List<TugasPunyaPegawai> tugasPunyaPegawais = session
					.createCriteria(TugasPunyaPegawai.class)
					.createCriteria("tugas")
					.add(Restrictions.eq("proyek", proyek))
					.add(Restrictions.eq("revisi", revisi))
					.add(parentTugas == null ? Restrictions.isNull("parent")
							: Restrictions.eq("parent", parentTugas))
					.addOrder(Order.asc("mulai")).addOrder(Order.asc("nama"))
					.list();

			HibernateUtil.closeSession();
			return tugasPunyaPegawais;
		} else {
			Session session = HibernateUtil.currentNativeSession();
			List<TugasPunyaPegawai> tugasPunyaPegawais = session
					.createCriteria(TugasPunyaPegawai.class)
					.add(Restrictions.eq("tugas", parentTugas)).list();

			HibernateUtil.closeSession();
			return tugasPunyaPegawais;
		}
	}

	public Double getTotalBiaya(Object parent) {
		Tugas parentTugas = (Tugas) parent;
		if (getChildCount(parentTugas) != 0) {
			Session session = HibernateUtil.currentNativeSession();
			Double biaya = (Double) session
					.createCriteria(TugasPunyaPegawai.class)
					.setProjection(Projections.sum("anggaran"))
					.createCriteria("tugas")
					.add(Restrictions.eq("proyek", proyek))
					.add(Restrictions.eq("revisi", revisi))
					.add(parentTugas == null ? Restrictions.isNull("parent")
							: Restrictions.eq("parent", parentTugas))
					.uniqueResult();

			HibernateUtil.closeSession();
			return biaya == null ? 0.0 : biaya;
		} else {
			Session session = HibernateUtil.currentNativeSession();
			Double biaya = (Double) session
					.createCriteria(TugasPunyaPegawai.class)
					.setProjection(Projections.sum("anggaran"))
					.add(Restrictions.eq("tugas", parentTugas)).uniqueResult();

			HibernateUtil.closeSession();
			return biaya == null ? 0.0 : biaya;
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(Tugas tugas, Tugas obj, List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (tugas.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session,(obj));
		} else {
			Tugas parentTugas = (Tugas) session.createCriteria(Tugas.class)
					.add(Restrictions.idEq(tugas.getParent().getId()))
					.uniqueResult();
			longs.add(parentTugas.getId());
			getParentCount(parentTugas, obj, longs);
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