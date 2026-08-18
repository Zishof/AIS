package ais.action.master.payroll.util;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.payroll.ItemGaji;
import ais.database.model.payroll.ItemGajiPegawai;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.database.model.payroll.RencanaGajiPunyaPegawai;
import ais.database.model.payroll.RencanaItemGajiPegawai;
import ais.ui.util.MyJSONObject;

public class RencanaItemGajiPegawaiTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private Boolean tampilkanSemua;
	private RencanaGajiPunyaPegawai rencanaGajiPunyaPegawai;
	private ItemGajiPegawaiTreeModel itemGajiPegawaiTreeModel;
	private MyJSONObject jsonObject = null;

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public RencanaItemGajiPegawaiTreeModel(Boolean tampilkanSemua, RencanaGajiPunyaPegawai rencanaGajiPunyaPegawai) {
		super(null);
		this.rencanaGajiPunyaPegawai = rencanaGajiPunyaPegawai;
		this.tampilkanSemua = tampilkanSemua;

		itemGajiPegawaiTreeModel = new ItemGajiPegawaiTreeModel(tampilkanSemua,
				rencanaGajiPunyaPegawai.getPegawai().getFormatItemGaji(), rencanaGajiPunyaPegawai.getPegawai(),
				PembayaranGajiPunyaPegawai.ambilMulai(rencanaGajiPunyaPegawai.getRencanaGaji().getTahun(),
						Calendar.getInstance().get(Calendar.MONTH) - 1));

	}

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public RencanaItemGajiPegawaiTreeModel(RencanaItemGajiPegawai parentRencanaItemGajiPegawai, Boolean tampilkanSemua,
			RencanaGajiPunyaPegawai rencanaGajiPunyaPegawai, Pegawai pegawai) {
		super(parentRencanaItemGajiPegawai);
		this.rencanaGajiPunyaPegawai = rencanaGajiPunyaPegawai;
		this.tampilkanSemua = tampilkanSemua;
		itemGajiPegawaiTreeModel = new ItemGajiPegawaiTreeModel(tampilkanSemua,
				rencanaGajiPunyaPegawai.getPegawai().getFormatItemGaji(), rencanaGajiPunyaPegawai.getPegawai(),
				PembayaranGajiPunyaPegawai.ambilMulai(rencanaGajiPunyaPegawai.getRencanaGaji().getTahun(),
						Calendar.getInstance().get(Calendar.MONTH) - 1));
	}

	public List<RencanaItemGajiPegawai> getChildren(RencanaItemGajiPegawai parentRencanaItemGajiPegawai) {
		return getChildren(parentRencanaItemGajiPegawai, null);
	}

	@SuppressWarnings("unchecked")
	public List<RencanaItemGajiPegawai> getChildren(RencanaItemGajiPegawai parentRencanaItemGajiPegawai,
			Integer index) {
		Session session = HibernateUtil.currentSession();
		List<RencanaItemGajiPegawai> rencanaItemGajiPegawais = session.createCriteria(RencanaItemGajiPegawai.class)
				.add(Restrictions.eq("rencanaGajiPunyaPegawai", rencanaGajiPunyaPegawai))
				.setMaxResults(index == null ? 10000 : 1).setFirstResult(index == null ? 0 : index)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
				.add(parentRencanaItemGajiPegawai == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentRencanaItemGajiPegawai))
				.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama")).list();
		return rencanaItemGajiPegawais;
	}

	@SuppressWarnings({ "rawtypes" })
	public void populateData(List list, Date tanggal) {
		populateData(list, null, tanggal);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void populateData(List list, RencanaItemGajiPegawai parentRencanaItemGajiPegawai, Date tanggal) {
		List<RencanaItemGajiPegawai> parents = getChildren(parentRencanaItemGajiPegawai);
		for (RencanaItemGajiPegawai gajiPegawai : parents) {
			if (gajiPegawai.getTampilkanDiSlip()) {
				String tambahanDepan = "";
				Double hasil = null;
				if (!gajiPegawai.getSpace()) {
					if (getChildCount(gajiPegawai) > 0) {
						populateData(list, gajiPegawai, tanggal);
					}

					tambahanDepan = "";
					RencanaItemGajiPegawai parentPegawai = gajiPegawai.getParent();
					while (parentPegawai != null) {
						tambahanDepan += "      ";
						parentPegawai = parentPegawai.getParent();
					}

					hasil = gajiPegawai.getNilai();
				}
				Map map = new java.util.HashMap();
				map.put("item", tambahanDepan + "" + gajiPegawai.getNama());
				map.put("nilai", hasil);
				list.add(map);
			}
		}
	}

	public void generateAllChildren(RencanaItemGajiPegawai parentRencanaItemGajiPegawai,
			Set<RencanaItemGajiPegawai> rencanaItemGajiPegawais) {
		if (!isLeaf(parentRencanaItemGajiPegawai)) {
			List<RencanaItemGajiPegawai> kerjas = getChildren(parentRencanaItemGajiPegawai);
			for (RencanaItemGajiPegawai rencanaItemGajiPegawai : kerjas) {
				rencanaItemGajiPegawais.add(rencanaItemGajiPegawai);
				generateAllChildren(rencanaItemGajiPegawai, rencanaItemGajiPegawais);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		RencanaItemGajiPegawai parentRencanaItemGajiPegawai = (RencanaItemGajiPegawai) parent;
		List<RencanaItemGajiPegawai> rencanaItemGajiPegawais = getChildren(parentRencanaItemGajiPegawai, index);
		RencanaItemGajiPegawai rencanaItemGajiPegawai = rencanaItemGajiPegawais.size() > 0
				? rencanaItemGajiPegawais.get(0)
				: null;
		return rencanaItemGajiPegawai;
	}

	public int getChildCount(Object parent) {
		RencanaItemGajiPegawai parentRencanaItemGajiPegawai = (RencanaItemGajiPegawai) parent;
		Session session = HibernateUtil.currentSession();
		Integer count = ((Number) session.createCriteria(RencanaItemGajiPegawai.class)

				.add(Restrictions.eq("rencanaGajiPunyaPegawai", rencanaGajiPunyaPegawai))
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))

				.add(parentRencanaItemGajiPegawai == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentRencanaItemGajiPegawai))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		return count;
	}

	public void deleteChilds(Object parent) {
		RencanaItemGajiPegawai parentRencanaItemGajiPegawai = (RencanaItemGajiPegawai) parent;
		Session session = HibernateUtil.currentSession();
		List<RencanaItemGajiPegawai> rencanaItemGajiPegawais = getChildren(parentRencanaItemGajiPegawai);
		for (RencanaItemGajiPegawai rencanaItemGajiPegawai : rencanaItemGajiPegawais) {
			if (getChildCount(rencanaItemGajiPegawai) == 0) {
				session.delete(rencanaItemGajiPegawai);
			} else {
				deleteChilds(rencanaItemGajiPegawai);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(RencanaItemGajiPegawai rencanaItemGajiPegawai, RencanaItemGajiPegawai obj,
			List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (rencanaItemGajiPegawai.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session, (obj));
		} else {
			RencanaItemGajiPegawai parentRencanaItemGajiPegawai = (RencanaItemGajiPegawai) session
					.createCriteria(RencanaItemGajiPegawai.class)

					.add(Restrictions.eq("rencanaGajiPunyaPegawai", rencanaGajiPunyaPegawai))
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
					.add(Restrictions.idEq(rencanaItemGajiPegawai.getParent().getId())).uniqueResult();
			longs.add(parentRencanaItemGajiPegawai.getId());
			getParentCount(parentRencanaItemGajiPegawai, obj, longs);
		}

	}

	public void getParentSet(RencanaItemGajiPegawai rencanaItemGajiPegawai,
			List<RencanaItemGajiPegawai> rencanaItemGajiPegawais) {
		Session session = HibernateUtil.currentSession();
		if (rencanaItemGajiPegawai.getParent() != null) {
			RencanaItemGajiPegawai parentRencanaItemGajiPegawai = (RencanaItemGajiPegawai) session
					.createCriteria(RencanaItemGajiPegawai.class)

					.add(Restrictions.eq("rencanaGajiPunyaPegawai", rencanaGajiPunyaPegawai))
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
					.add(Restrictions.idEq(rencanaItemGajiPegawai.getParent().getId())).uniqueResult();
			if (parentRencanaItemGajiPegawai != null) {
				rencanaItemGajiPegawais.add(parentRencanaItemGajiPegawai);
				getParentSet(parentRencanaItemGajiPegawai, rencanaItemGajiPegawais);
			}
		}

	}

	public void getChildsSet(RencanaItemGajiPegawai rencanaItemGajiPegawai,
			Set<RencanaItemGajiPegawai> rencanaItemGajiPegawais) {
		List<RencanaItemGajiPegawai> childs = getChildren(rencanaItemGajiPegawai);
		for (RencanaItemGajiPegawai myRencanaItemGajiPegawai : childs) {
			rencanaItemGajiPegawais.add(myRencanaItemGajiPegawai);
			if (!isLeaf(myRencanaItemGajiPegawai)) {
				getChildsSet(myRencanaItemGajiPegawai, rencanaItemGajiPegawais);
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

	public void copyByFormat(RencanaItemGajiPegawai parent, RencanaItemGajiPegawai newParent,
			RencanaGajiPunyaPegawai toRencanaGajiPunyaPegawai) {
		Session session = HibernateUtil.currentSession();
		@SuppressWarnings("unchecked")
		List<RencanaItemGajiPegawai> rencanaItemGajiPegawais = session.createCriteria(RencanaItemGajiPegawai.class)

				.add(Restrictions.eq("rencanaGajiPunyaPegawai", rencanaGajiPunyaPegawai))
				.add(parent == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parent)).list();
		System.out.println("rencanaItemGajiPegawais = " + rencanaItemGajiPegawais.size());
		for (RencanaItemGajiPegawai rencanaItemGajiPegawai : rencanaItemGajiPegawais) {
			RencanaItemGajiPegawai newGaji = (RencanaItemGajiPegawai) rencanaItemGajiPegawai.clone();
			newGaji.setId(null);
			newGaji.setRencanaGajiPunyaPegawai(toRencanaGajiPunyaPegawai);
			newGaji.setParent(newParent);
			session.save(newGaji);
			if (getChildCount(rencanaItemGajiPegawai) > 0) {
				copyByFormat(rencanaItemGajiPegawai, newGaji, toRencanaGajiPunyaPegawai);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void reset(Date tanggal, Map<Long, String> formulasBaru, Integer tahun) throws Exception {
		Session session = HibernateUtil.currentNativeSession();

		try {
			session.createSQLQuery("delete from payroll.rencana_item_gaji_pegawai where rencana_gaji_punya_pegawai = "
					+ rencanaGajiPunyaPegawai.getId()).executeUpdate();
			jsonObject = new MyJSONObject(rencanaGajiPunyaPegawai.getKomponenGaji());

			Map<String, Double> mapTotal = new HashMap<String, Double>();

			for (Integer bulan = 1; bulan <= 12; bulan++) {

				Double biaya = null;

				Object[] biayaA = (Object[]) session.createCriteria(PembayaranGajiPunyaPegawai.class)
						.setProjection(Projections.projectionList().add(Projections.property("nilai"))
								.add(Projections.property("komponenGaji")))
						.add(Restrictions.eq("pegawai", rencanaGajiPunyaPegawai.getPegawai()))
						.createAlias("pembayaranGaji", "pembayaranGaji")
						.add(Restrictions.le("pembayaranGaji.bulan", bulan))
						.add(Restrictions.eq("pembayaranGaji.tahun", tahun))
						.add(Restrictions.isNotNull("pembayaranGaji.disetujuiOleh"))
						.addOrder(Order.desc("pembayaranGaji.bulan")).addOrder(Order.desc("id")).setMaxResults(1)
						.uniqueResult();

				if (biayaA != null && biayaA.length != 0) {
					biaya = (Double) biayaA[0];
					String komponenGaji = (String) biayaA[1];
					try {
						MyJSONObject myJSONObject = new MyJSONObject(komponenGaji);
						Iterator<String> iterator = myJSONObject.keys();
						while (iterator.hasNext()) {
							String key = iterator.next();
							Object val = myJSONObject.get(key);

							ItemGaji itemGaji = (ItemGaji) ConstantValues.ambil(ItemGaji.class.getName(),
									Long.parseLong(key));

							jsonObject.put(itemGaji.getKode() + "_" + bulan, val);

							Double h = mapTotal.get(itemGaji.getKode());
							if (h == null) {
								h = 0.0;
							}
							h += Double.parseDouble(val.toString());
							mapTotal.put(itemGaji.getKode(), h);

						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/RencanaItemGajiPegawaiTreeModel.java:306");
					}
				}

				if (biaya == null) {
					biaya = copyByItemGajiPegawai(null, null, rencanaGajiPunyaPegawai, tanggal, formulasBaru, bulan,
							tahun, session, mapTotal);
				}

				if (bulan.equals(1)) {
					rencanaGajiPunyaPegawai.setNilai1(biaya);
				} else if (bulan.equals(2)) {
					rencanaGajiPunyaPegawai.setNilai2(biaya);
				} else if (bulan.equals(3)) {
					rencanaGajiPunyaPegawai.setNilai3(biaya);
				} else if (bulan.equals(4)) {
					rencanaGajiPunyaPegawai.setNilai4(biaya);
				} else if (bulan.equals(5)) {
					rencanaGajiPunyaPegawai.setNilai5(biaya);
				} else if (bulan.equals(6)) {
					rencanaGajiPunyaPegawai.setNilai6(biaya);
				} else if (bulan.equals(7)) {
					rencanaGajiPunyaPegawai.setNilai7(biaya);
				} else if (bulan.equals(8)) {
					rencanaGajiPunyaPegawai.setNilai8(biaya);
				} else if (bulan.equals(9)) {
					rencanaGajiPunyaPegawai.setNilai9(biaya);
				} else if (bulan.equals(10)) {
					rencanaGajiPunyaPegawai.setNilai10(biaya);
				} else if (bulan.equals(11)) {
					rencanaGajiPunyaPegawai.setNilai11(biaya);
				} else if (bulan.equals(12)) {
					rencanaGajiPunyaPegawai.setNilai12(biaya);
				}
			}

			for (String kode : mapTotal.keySet()) {
				Double n = mapTotal.get(kode);
				jsonObject.put("RENC_TOT_" + kode, n);
			}

			rencanaGajiPunyaPegawai.setKomponenGaji(jsonObject.toString());

			session.getTransaction().begin();
			Common.refreshUpdate(session, rencanaGajiPunyaPegawai);
			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/RencanaItemGajiPegawaiTreeModel.java:353");
			// TODO: handle exception
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void checkExistingItemGaji(Date tanggal, Map<Long, String> formulasBaru, Integer bulan, Integer tahun)
			throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		Integer count = ((Number) session.createCriteria(RencanaItemGajiPegawai.class)
				.add(Restrictions.eq("rencanaGajiPunyaPegawai", rencanaGajiPunyaPegawai))
				.add(Restrictions.isNull("parent")).setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count.equals(0)) {
			session.createSQLQuery("delete from payroll.rencana_item_gaji_pegawai where rencana_gaji_punya_pegawai = "
					+ rencanaGajiPunyaPegawai.getId()).executeUpdate();
			copyByItemGajiPegawai(null, null, rencanaGajiPunyaPegawai, tanggal, formulasBaru, bulan, tahun, session,
					null);
		}
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	@SuppressWarnings("unchecked")
	public Double copyByItemGajiPegawai(ItemGajiPegawai parent, RencanaItemGajiPegawai newParent,
			RencanaGajiPunyaPegawai toRencanaGajiPunyaPegawai, Date tanggal, Map<Long, String> formulasBaru,
			Integer bulan, Integer tahun, Session session, Map<String, Double> mapTotal) throws Exception {

		List<ItemGajiPegawai> itemGajiPegawais = ConstantValues.simpleList(
				session.createCriteria(ItemGajiPegawai.class).addOrder(Order.asc("nomorUrut"))
						.add(Restrictions.eq("formatItemGaji",
								rencanaGajiPunyaPegawai.getPegawai().getFormatItemGaji()))
						.add(Restrictions.eq("pegawai", rencanaGajiPunyaPegawai.getPegawai()))
						.add(parent == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parent)),
				ItemGajiPegawai.class);
//		System.out.println("itemGajiPegawais = " + itemGajiPegawais.size());
		Double hasil = 0.0;

		for (ItemGajiPegawai itemGajiPegawai : itemGajiPegawais) {

			try {
				RencanaItemGajiPegawai newGaji = new RencanaItemGajiPegawai();
				newGaji.setId(null);
				newGaji.setFormatItemGaji(itemGajiPegawai.getFormatItemGaji());
				newGaji.setRencanaGajiPunyaPegawai(toRencanaGajiPunyaPegawai);
				newGaji.setParent(newParent);
				newGaji.setItemGajiPegawai(itemGajiPegawai);
				newGaji.setAkun(itemGajiPegawai.getItemGaji() == null ? null : itemGajiPegawai.getItemGaji().getAkun());
				newGaji.setAkunDebet(
						itemGajiPegawai.getItemGaji() == null ? null : itemGajiPegawai.getItemGaji().getAkunDebet());
				newGaji.setPegawai(rencanaGajiPunyaPegawai.getPegawai());
				newGaji.setBulan(bulan);
				newGaji.setTahun(tahun);
				if (formulasBaru != null && itemGajiPegawai.getItemGaji() != null
						&& formulasBaru.containsKey(itemGajiPegawai.getItemGaji().getId())) {
					String defaultFormula = formulasBaru.get(itemGajiPegawai.getItemGaji().getId());
					newGaji.setDefaultFormula(defaultFormula);
				}

				hasil = itemGajiPegawaiTreeModel.hitungItemGajiPegawai(newGaji.getKode(), newGaji.getDefaultFormula(),
						tanggal, bulan, tahun, null, null);
				if (itemGajiPegawai != null && itemGajiPegawai.getItemGaji() != null
						&& itemGajiPegawai.getItemGaji().getJadikan0JikaMinus() && hasil < 0.0) {
					hasil = 0.0;
				}
				newGaji.setNilai(hasil);

				if (jsonObject != null && itemGajiPegawai.getItemGaji() != null) {
					jsonObject.put(itemGajiPegawai.getItemGaji().getKode() + "_" + bulan, hasil);
				}

				if (mapTotal != null && itemGajiPegawai.getItemGaji() != null) {
					Double h = mapTotal.get(itemGajiPegawai.getItemGaji().getKode());
					if (h == null) {
						h = 0.0;
					}
					h += hasil;
					mapTotal.put(itemGajiPegawai.getItemGaji().getKode(), h);
				}

				session.getTransaction().begin();
				session.save(newGaji);
				session.getTransaction().commit();

				if (getItemGajiPegawaiChildCount(itemGajiPegawai) > 0) {
					copyByItemGajiPegawai(itemGajiPegawai, newGaji, toRencanaGajiPunyaPegawai, tanggal, formulasBaru,
							bulan, tahun, session, mapTotal);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/RencanaItemGajiPegawaiTreeModel.java:445");
			}
		}

		return hasil;
	}

	public int getItemGajiPegawaiChildCount(ItemGajiPegawai parentItemGajiPegawai) {
		Session session = HibernateUtil.currentSession();
		Integer count = ((Number) session.createCriteria(ItemGajiPegawai.class)

				.add(Restrictions.eq("pegawai", rencanaGajiPunyaPegawai.getPegawai()))
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))

				.add(parentItemGajiPegawai == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentItemGajiPegawai))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		return count;
	}

}