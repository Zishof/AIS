package ais.action.master.payroll.util;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
import ais.database.model.Pegawai;
import ais.database.model.payroll.ItemGajiPegawai;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.database.model.payroll.PembayaranItemGajiPegawai;
import ais.database.model.payroll.TransaksiPegawai;
import ais.ui.util.MyJSONObject;

public class PembayaranItemGajiPegawaiTreeModel extends AbstractTreeModel {

	private static final long serialVersionUID = -5115651721345571411L;
	private Boolean tampilkanSemua;
	private PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai;
	private ItemGajiPegawaiTreeModel itemGajiPegawaiTreeModel;
	private MyJSONObject jsonObject = null;

	/**
	 * Constructor
	 */
	public PembayaranItemGajiPegawaiTreeModel(Boolean tampilkanSemua,
			PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai) {
		super(null);
		this.pembayaranGajiPunyaPegawai = pembayaranGajiPunyaPegawai;
		this.tampilkanSemua = tampilkanSemua;
		itemGajiPegawaiTreeModel = new ItemGajiPegawaiTreeModel(tampilkanSemua,
				pembayaranGajiPunyaPegawai.getFormatItemGaji(), pembayaranGajiPunyaPegawai.getPegawai(),
				pembayaranGajiPunyaPegawai.getMulai());
	}

	/**
	 * Constructor
	 */
	public PembayaranItemGajiPegawaiTreeModel(PembayaranItemGajiPegawai parentPembayaranItemGajiPegawai,
			Boolean tampilkanSemua, PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai, Pegawai pegawai) {
		super(parentPembayaranItemGajiPegawai);
		this.pembayaranGajiPunyaPegawai = pembayaranGajiPunyaPegawai;
		this.tampilkanSemua = tampilkanSemua;
		itemGajiPegawaiTreeModel = new ItemGajiPegawaiTreeModel(tampilkanSemua,
				pembayaranGajiPunyaPegawai.getFormatItemGaji(), pembayaranGajiPunyaPegawai.getPegawai(),
				pembayaranGajiPunyaPegawai.getMulai());
	}

	public List<PembayaranItemGajiPegawai> getChildren(PembayaranItemGajiPegawai parent) {
		return getChildren(parent, null);
	}

	public List<PembayaranItemGajiPegawai> getChildren(PembayaranItemGajiPegawai parent, Integer index) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return getChildren(parent, index, session);
		} finally {
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	@SuppressWarnings("unchecked")
	private List<PembayaranItemGajiPegawai> getChildren(PembayaranItemGajiPegawai parent, Integer index, Session session) {
		List<PembayaranItemGajiPegawai> pembayaranItemGajiPegawais = ConstantValues.simpleList(
				session.createCriteria(PembayaranItemGajiPegawai.class)
						.add(Restrictions.eq("pembayaranGajiPunyaPegawai", pembayaranGajiPunyaPegawai))
						.setMaxResults(index == null ? 10000 : 1).setFirstResult(index == null ? 0 : index)
						.add(parent != null || tampilkanSemua ? Restrictions.sqlRestriction("true") : Restrictions.eq("aktif", true))
						.add(parent == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parent))
						.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama")),
				PembayaranItemGajiPegawai.class);
		return pembayaranItemGajiPegawais;
	}

	@SuppressWarnings({ "rawtypes" })
	public void populateData(List list, Map maps, Date tanggal) {
		populateData(list, maps, null, tanggal);
	}

	@SuppressWarnings({ "rawtypes" })
	public void populateData(List list, Map maps, PembayaranItemGajiPegawai parent, Date tanggal) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			populateDataRecursive(list, maps, parent, tanggal, session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:103");
		} finally {
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void populateDataRecursive(List list, Map maps, PembayaranItemGajiPegawai parent, Date tanggal, Session session) {
		List<PembayaranItemGajiPegawai> parents = getChildren(parent, null, session);
		for (PembayaranItemGajiPegawai gajiPegawai : parents) {
			if (gajiPegawai.getTampilkanDiSlip()) {
				String tambahanDepan = "";
				Double hasil = null;
				if (!gajiPegawai.getSpace()) {
					if (getChildCount(gajiPegawai, session) > 0) {
						populateDataRecursive(list, maps, gajiPegawai, tanggal, session);
					}

					StringBuilder sbTambahan = new StringBuilder();
					PembayaranItemGajiPegawai parentPegawai = gajiPegawai.getParent();
					while (parentPegawai != null) {
						sbTambahan.append("      ");
						parentPegawai = parentPegawai.getParent();
					}
					tambahanDepan = sbTambahan.toString();
					hasil = gajiPegawai.getNilai();
				}
				
				maps.put(gajiPegawai.getKode() + "_nama", gajiPegawai.getNama());
				maps.put(gajiPegawai.getKode() + "_nilai", hasil);
				maps.put(gajiPegawai.getKode() + "_mulai", gajiPegawai.getPembayaranGajiPunyaPegawai() == null
						|| gajiPegawai.getPembayaranGajiPunyaPegawai().getMulai() == null ? ""
								: Common.dateFormat1.get().format(gajiPegawai.getPembayaranGajiPunyaPegawai().getMulai()));
				maps.put(gajiPegawai.getKode() + "_sampai", gajiPegawai.getPembayaranGajiPunyaPegawai() == null
						|| gajiPegawai.getPembayaranGajiPunyaPegawai().getSampai() == null ? ""
								: Common.dateFormat1.get().format(gajiPegawai.getPembayaranGajiPunyaPegawai().getSampai()));
				
				Map map = new java.util.HashMap();
				map.put("item", tambahanDepan + "" + gajiPegawai.getNama());
				map.put("nilai", hasil);

				map.put(gajiPegawai.getKode() + "_mulai", gajiPegawai.getPembayaranGajiPunyaPegawai() == null
						|| gajiPegawai.getPembayaranGajiPunyaPegawai().getMulai() == null ? ""
								: Common.dateFormat1.get().format(gajiPegawai.getPembayaranGajiPunyaPegawai().getMulai()));
				map.put(gajiPegawai.getKode() + "_sampai", gajiPegawai.getPembayaranGajiPunyaPegawai() == null
						|| gajiPegawai.getPembayaranGajiPunyaPegawai().getSampai() == null ? ""
								: Common.dateFormat1.get().format(gajiPegawai.getPembayaranGajiPunyaPegawai().getSampai()));

				list.add(map);
			}
		}
	}

	public void generateAllChildren(PembayaranItemGajiPegawai parent, Set<PembayaranItemGajiPegawai> allItems) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			generateAllChildrenRecursive(parent, allItems, session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:166");
		} finally {
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}
	
	private void generateAllChildrenRecursive(PembayaranItemGajiPegawai parent, Set<PembayaranItemGajiPegawai> allItems, Session session) {
		if (getChildCount(parent, session) > 0) {
			List<PembayaranItemGajiPegawai> kerjas = getChildren(parent, null, session);
			for (PembayaranItemGajiPegawai item : kerjas) {
				allItems.add(item);
				generateAllChildrenRecursive(item, allItems, session);
			}
		}
	}

	public Object getChild(Object parent, int index) {
		PembayaranItemGajiPegawai parentObj = (PembayaranItemGajiPegawai) parent;
		List<PembayaranItemGajiPegawai> list = getChildren(parentObj, index);
		return list.size() > 0 ? list.get(0) : null;
	}

	public int getChildCount(Object parent) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return getChildCount((PembayaranItemGajiPegawai) parent, session);
		} finally {
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	private int getChildCount(PembayaranItemGajiPegawai parent, Session session) {
		Integer count = ((Number) session.createCriteria(PembayaranItemGajiPegawai.class)
				.add(Restrictions.eq("pembayaranGajiPunyaPegawai", pembayaranGajiPunyaPegawai))
				.add(parent != null || tampilkanSemua ? Restrictions.sqlRestriction("true") : Restrictions.eq("aktif", true))
				.add(parent == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parent))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		return count;
	}

	public void deleteChilds(Object parent) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			deleteChildsRecursive((PembayaranItemGajiPegawai) parent, session);
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session != null && session.getTransaction().isActive()) session.getTransaction().rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:224");
		} finally {
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	private void deleteChildsRecursive(PembayaranItemGajiPegawai parent, Session session) {
		List<PembayaranItemGajiPegawai> list = getChildren(parent, null, session);
		for (PembayaranItemGajiPegawai item : list) {
			if (getChildCount(item, session) == 0) {
				session.delete(item);
			} else {
				deleteChildsRecursive(item, session);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(PembayaranItemGajiPegawai objToCount, PembayaranItemGajiPegawai objToUpdate, List<Long> longs) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			getParentCountRecursive(objToCount, objToUpdate, longs, session);
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session != null && session.getTransaction().isActive()) session.getTransaction().rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:258");
		} finally {
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	private void getParentCountRecursive(PembayaranItemGajiPegawai objToCount, PembayaranItemGajiPegawai objToUpdate, List<Long> longs, Session session) {
		if (objToCount.getParent() == null) {
			objToUpdate.setDeep(longs.size());
			Common.refreshUpdate(session, objToUpdate);
		} else {
			PembayaranItemGajiPegawai parent = (PembayaranItemGajiPegawai) ConstantValues.simpleObject(
					session.createCriteria(PembayaranItemGajiPegawai.class)
							.add(Restrictions.eq("pembayaranGajiPunyaPegawai", pembayaranGajiPunyaPegawai))
							.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
							.add(Restrictions.idEq(objToCount.getParent().getId())),
					PembayaranItemGajiPegawai.class);
			longs.add(parent.getId());
			getParentCountRecursive(parent, objToUpdate, longs, session);
		}
	}

	public void getParentSet(PembayaranItemGajiPegawai obj, List<PembayaranItemGajiPegawai> list) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			getParentSetRecursive(obj, list, session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:290");
		} finally {
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	private void getParentSetRecursive(PembayaranItemGajiPegawai obj, List<PembayaranItemGajiPegawai> list, Session session) {
		if (obj.getParent() != null) {
			PembayaranItemGajiPegawai parent = (PembayaranItemGajiPegawai) ConstantValues.simpleObject(
					session.createCriteria(PembayaranItemGajiPegawai.class)
							.add(Restrictions.eq("pembayaranGajiPunyaPegawai", pembayaranGajiPunyaPegawai))
							.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
							.add(Restrictions.idEq(obj.getParent().getId())),
					PembayaranItemGajiPegawai.class);
			if (parent != null) {
				list.add(parent);
				getParentSetRecursive(parent, list, session);
			}
		}
	}

	public void getChildsSet(PembayaranItemGajiPegawai parent, Set<PembayaranItemGajiPegawai> set) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			getChildsSetRecursive(parent, set, session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:321");
		} finally {
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	private void getChildsSetRecursive(PembayaranItemGajiPegawai parent, Set<PembayaranItemGajiPegawai> set, Session session) {
		List<PembayaranItemGajiPegawai> childs = getChildren(parent, null, session);
		for (PembayaranItemGajiPegawai child : childs) {
			set.add(child);
			if (getChildCount(child, session) > 0) {
				getChildsSetRecursive(child, set, session);
			}
		}
	}

	@Override
	public int getIndexOfChild(Object arg0, Object arg1) {
		return 0;
	}

	public void copyByFormat(PembayaranItemGajiPegawai parent, PembayaranItemGajiPegawai newParent, PembayaranGajiPunyaPegawai target) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			copyByFormatRecursive(parent, newParent, target, session);
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session != null && session.getTransaction().isActive()) session.getTransaction().rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:355");
		} finally {
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	@SuppressWarnings("unchecked")
	private void copyByFormatRecursive(PembayaranItemGajiPegawai parent, PembayaranItemGajiPegawai newParent, PembayaranGajiPunyaPegawai target, Session session) {
		List<PembayaranItemGajiPegawai> list = ConstantValues.simpleList(
				session.createCriteria(PembayaranItemGajiPegawai.class)
						.add(Restrictions.eq("pembayaranGajiPunyaPegawai", pembayaranGajiPunyaPegawai))
						.add(parent == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parent)),
				PembayaranItemGajiPegawai.class);
		
		for (PembayaranItemGajiPegawai item : list) {
			PembayaranItemGajiPegawai newGaji = (PembayaranItemGajiPegawai) item.clone();
			newGaji.setId(null);
			newGaji.setPembayaranGajiPunyaPegawai(target);
			newGaji.setParent(newParent);
			session.save(newGaji);
			if (getChildCount(item, session) > 0) {
				copyByFormatRecursive(item, newGaji, target, session);
			}
		}
	}

	public void reset(Date tanggal, Map<Long, String> formulasBaru, Integer bulan, Integer tahun) throws Exception {
		Session session = null;
		try {
			// AKAR "Session is closed!" (reset:406): copyByItemGajiPegawaiRecursive memanggil
			// ItemGajiPegawaiTreeModel.hitungItemGajiPegawai() yang—pada formula kompleks—memakai
			// currentNativeSession() (THREAD-LOCAL yang SAMA) lalu MENUTUPNYA di finally
			// (closeSession → HibernateUtil.closeSession). Session milik reset() ikut tertutup →
			// save/commit berikutnya gagal. Solusi: pakai openSession() TERDEDIKASI (bukan
			// thread-local) sehingga kebal terhadap penutupan thread-local oleh formula engine.
			// Tutup sendiri di finally. Formula engine tetap memakai thread-local-nya sendiri.
			session = HibernateUtil.openSession();
			session.getTransaction().begin();
			session.createSQLQuery("delete from payroll.pembayaran_item_gaji_pegawai where pembayaran_gaji_punya_pegawai = "
					+ pembayaranGajiPunyaPegawai.getId()).executeUpdate();

			jsonObject = new MyJSONObject(pembayaranGajiPunyaPegawai.getKomponenGaji());

			// Hitung ulang dan simpan menggunakan sesi yang sama agar N+1 query terhindari
			Double hasil = copyByItemGajiPegawaiRecursive(null, null, pembayaranGajiPunyaPegawai, tanggal, formulasBaru, bulan, tahun, session);

			if (pembayaranGajiPunyaPegawai != null && (pembayaranGajiPunyaPegawai.getNilai().intValue() != hasil.intValue()
					|| (pembayaranGajiPunyaPegawai.getNilaiFinal() != null && pembayaranGajiPunyaPegawai.getNilaiFinal().intValue() != 0))) {
				pembayaranGajiPunyaPegawai.setNilai(hasil);
				pembayaranGajiPunyaPegawai.setKomponenGaji(jsonObject.toString());
				Common.refreshUpdate(session, pembayaranGajiPunyaPegawai);
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			// Defensif: jangan biarkan getTransaction() pada session yg mungkin sudah tertutup
			// melempar "Session is closed!" dan MENUTUPI exception asli.
			try {
				if (session != null && session.isOpen() && session.getTransaction() != null
						&& session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:420");
			}
			throw e;
		} finally {
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:425");}
				try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:426");}
			}
			// Bersihkan thread-local yang mungkin dibuka oleh formula engine di dalam recompute.
			HibernateUtil.closeSession();
		}
	}

	public void checkExistingItemGaji(Date tanggal, Map<Long, String> formulasBaru, Integer bulan, Integer tahun) throws Exception {
		Session session = null;
		try {
			// Akar sama dgn reset(): recompute memanggil formula engine yg menutup thread-local.
			// Pakai openSession() terdedikasi agar transaksi ini tidak ikut tertutup.
			session = HibernateUtil.openSession();
			Integer count = ((Number) session.createCriteria(PembayaranItemGajiPegawai.class)
					.add(Restrictions.eq("pembayaranGajiPunyaPegawai", pembayaranGajiPunyaPegawai))
					.add(Restrictions.isNull("parent")).setProjection(Projections.rowCount()).uniqueResult()).intValue();

			if (count.equals(0)) {
				session.getTransaction().begin();
				session.createSQLQuery("delete from payroll.pembayaran_item_gaji_pegawai where pembayaran_gaji_punya_pegawai = "
						+ pembayaranGajiPunyaPegawai.getId()).executeUpdate();
				copyByItemGajiPegawaiRecursive(null, null, pembayaranGajiPunyaPegawai, tanggal, formulasBaru, bulan, tahun, session);
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			try {
				if (session != null && session.isOpen() && session.getTransaction() != null
						&& session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:456");
			}
			throw e;
		} finally {
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:461");}
				try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:462");}
			}
			HibernateUtil.closeSession();
		}
	}

	public Double copyByItemGajiPegawai(ItemGajiPegawai parent, PembayaranItemGajiPegawai newParent,
			PembayaranGajiPunyaPegawai toPembayaranGajiPunyaPegawai, Date tanggal, Map<Long, String> formulasBaru,
			Integer bulan, Integer tahun) throws Exception {
		Session session = null;
		try {
			// Akar sama dgn reset(): recompute memanggil formula engine yg menutup thread-local.
			// Pakai openSession() terdedikasi agar transaksi ini tidak ikut tertutup.
			session = HibernateUtil.openSession();
			session.getTransaction().begin();
			Double hasil = copyByItemGajiPegawaiRecursive(parent, newParent, toPembayaranGajiPunyaPegawai, tanggal, formulasBaru, bulan, tahun, session);
			session.getTransaction().commit();
			return hasil;
		} catch (Exception e) {
			try {
				if (session != null && session.isOpen() && session.getTransaction() != null
						&& session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:486");
			}
			throw e;
		} finally {
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:491");}
				try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:492");}
			}
			HibernateUtil.closeSession();
		}
	}

	@SuppressWarnings("unchecked")
	private Double copyByItemGajiPegawaiRecursive(ItemGajiPegawai parent, PembayaranItemGajiPegawai newParent,
			PembayaranGajiPunyaPegawai target, Date tanggal, Map<Long, String> formulasBaru, Integer bulan, Integer tahun, Session session) throws Exception {
		
		List<ItemGajiPegawai> itemGajiPegawais = ConstantValues.simpleList(
				session.createCriteria(ItemGajiPegawai.class).setProjection(Projections.property("id"))
						.addOrder(Order.asc("nomorUrut"))
						.add(Restrictions.eq("formatItemGaji", pembayaranGajiPunyaPegawai.getFormatItemGaji()))
						.add(Restrictions.eq("pegawai", pembayaranGajiPunyaPegawai.getPegawai()))
						.add(parent == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parent)),
				ItemGajiPegawai.class, false);

		Double hasil = 0.0;
		for (ItemGajiPegawai itemGajiPegawai : itemGajiPegawais) {
			PembayaranItemGajiPegawai newGaji = new PembayaranItemGajiPegawai();
			newGaji.setId(null);
			newGaji.setFormatItemGaji(itemGajiPegawai.getFormatItemGaji());
			newGaji.setPembayaranGajiPunyaPegawai(target);
			newGaji.setParent(newParent);
			newGaji.setItemGajiPegawai(itemGajiPegawai);
			newGaji.setAkun(itemGajiPegawai.getItemGaji() == null ? null : itemGajiPegawai.getItemGaji().getAkun());
			newGaji.setAkunDebet(itemGajiPegawai.getItemGaji() == null ? null : itemGajiPegawai.getItemGaji().getAkunDebet());
			newGaji.setPegawai(pembayaranGajiPunyaPegawai.getPegawai());
			newGaji.setBulan(bulan);
			newGaji.setTahun(tahun);
			
			if (formulasBaru != null && itemGajiPegawai.getItemGaji() != null && formulasBaru.containsKey(itemGajiPegawai.getItemGaji().getId())) {
				newGaji.setDefaultFormula(formulasBaru.get(itemGajiPegawai.getItemGaji().getId()));
			}

			hasil = itemGajiPegawaiTreeModel.hitungItemGajiPegawai(newGaji.getKode(), newGaji.getDefaultFormula(), tanggal, bulan, tahun, target, null);
			if (itemGajiPegawai != null && itemGajiPegawai.getItemGaji() != null && itemGajiPegawai.getItemGaji().getJadikan0JikaMinus() && hasil != null && hasil < 0.0) {
				hasil = 0.0;
			}
			newGaji.setNilai(hasil);

			if (jsonObject != null && itemGajiPegawai.getItemGaji() != null && itemGajiPegawai.getItemGaji().getId() != null) {
				jsonObject.put(itemGajiPegawai.getItemGaji().getId().toString(), hasil);
			}

			if (itemGajiPegawai.getFinalGaji()) {
				if (target.getNilaiFinal() == null || (hasil != null && hasil > 0.1 && hasil.intValue() != target.getNilaiFinal().intValue())) {
					target.setNilaiFinal(hasil);
					Common.refreshSaveOrUpdate(session, target);
				}
			}

			session.save(newGaji);

			if (getItemGajiPegawaiChildCount(itemGajiPegawai, session) > 0) {
				copyByItemGajiPegawaiRecursive(itemGajiPegawai, newGaji, target, tanggal, formulasBaru, bulan, tahun, session);
			}
		}
		return hasil;
	}

	@SuppressWarnings("unchecked")
	public void setLunas(Date tanggal) {
		if (pembayaranGajiPunyaPegawai == null) return;

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal);
		int month = calendar.get(Calendar.MONTH) + 1;
		int year = calendar.get(Calendar.YEAR);

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();

			Criterion timeCriteria = Restrictions.or(
				Restrictions.lt("thn", year),
				Restrictions.and(Restrictions.eq("thn", year), Restrictions.le("bln", month))
			);

			List<TransaksiPegawai> jenisTransaksiPegawai = session.createCriteria(TransaksiPegawai.class)
					.add(Restrictions.isNull("pembayaranGajiPunyaPegawai"))
					.add(Restrictions.eq("pegawai", pembayaranGajiPunyaPegawai.getPegawai()))
					.add(timeCriteria)
					.list();
			
			for (TransaksiPegawai transaksiPegawai : jenisTransaksiPegawai) {
				transaksiPegawai.setPembayaranGajiPunyaPegawai(pembayaranGajiPunyaPegawai);
				session.update(transaksiPegawai);
			}
			
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session != null && session.getTransaction().isActive()) session.getTransaction().rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/PembayaranItemGajiPegawaiTreeModel.java:587");
		} finally {
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	public int getItemGajiPegawaiChildCount(ItemGajiPegawai parent) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return getItemGajiPegawaiChildCount(parent, session);
		} finally {
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	private int getItemGajiPegawaiChildCount(ItemGajiPegawai parent, Session session) {
		Integer count = ((Number) session.createCriteria(ItemGajiPegawai.class)
				.add(Restrictions.eq("pegawai", pembayaranGajiPunyaPegawai.getPegawai()))
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
				.add(parent == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parent))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		return count;
	}

}