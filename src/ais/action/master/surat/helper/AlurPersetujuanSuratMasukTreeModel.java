package ais.action.master.surat.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.AlurPersetujuanSuratMasuk;

public class AlurPersetujuanSuratMasukTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private Boolean tampilkanSemua;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Checkbox searchaktif;
	private String tipe;

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public AlurPersetujuanSuratMasukTreeModel(Boolean tampilkanSemua, Combobox searchfakultas, Combobox searchjurusan,
			Combobox searchyayasan, Combobox searchsekolah, AmbilDataSatuanKerjaBanbox satuanKerja,
			Checkbox searchaktif, String tipe) {
		super(null);
		this.tipe = tipe;
		this.tampilkanSemua = tampilkanSemua;
		this.searchfakultas = searchfakultas;
		this.searchjurusan = searchjurusan;
		this.searchyayasan = searchyayasan;
		this.searchsekolah = searchsekolah;
		this.satuanKerja = satuanKerja;
		this.searchaktif = searchaktif;
		this.tampilkanSemua = tampilkanSemua;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
	}

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public AlurPersetujuanSuratMasukTreeModel(AlurPersetujuanSuratMasuk parentAlurPersetujuanSuratMasuk,
			Boolean tampilkanSemua, Combobox searchfakultas, Combobox searchjurusan, Combobox searchyayasan,
			Combobox searchsekolah, AmbilDataSatuanKerjaBanbox satuanKerja, Checkbox searchaktif, String tipe) {
		super(parentAlurPersetujuanSuratMasuk);
		this.tipe = tipe;
		this.searchfakultas = searchfakultas;
		this.searchjurusan = searchjurusan;
		this.searchyayasan = searchyayasan;
		this.searchsekolah = searchsekolah;
		this.satuanKerja = satuanKerja;
		this.searchaktif = searchaktif;
		this.tampilkanSemua = tampilkanSemua;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
	}

	public List<AlurPersetujuanSuratMasuk> getChildren(AlurPersetujuanSuratMasuk parentAlurPersetujuanSuratMasuk) {
		return getChildren(parentAlurPersetujuanSuratMasuk, null);
	}

	@SuppressWarnings("unchecked")
	public List<AlurPersetujuanSuratMasuk> getChildren(AlurPersetujuanSuratMasuk parentAlurPersetujuanSuratMasuk,
			Integer index) {
		Session session = HibernateUtil.currentSession();

		SatuanKerja parent = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		List<AlurPersetujuanSuratMasuk> alurPersetujuanSuratMasuks =

				ConstantValues.simpleList(session.createCriteria(AlurPersetujuanSuratMasuk.class)
						
						.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))

						.add(parentAlurPersetujuanSuratMasuk != null || searchaktif != null && searchaktif.isChecked()
								? Restrictions.or(Restrictions.isNull("defaultItem"),
										Restrictions.eq("defaultItem", true))
								: Restrictions.sqlRestriction("true"))

						.add(parentAlurPersetujuanSuratMasuk != null || satuanKerjas.size() == 0
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas)))

						.add(parentAlurPersetujuanSuratMasuk != null || searchjurusan.getSelectedItem() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("jurusan"),
												CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))
						.add(parentAlurPersetujuanSuratMasuk != null || searchfakultas.getSelectedItem() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("fakultas"),
												CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

						.add(parentAlurPersetujuanSuratMasuk != null || searchsekolah.getSelectedItem() == null
								|| searchsekolah.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("sekolah"),
												CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false)))

						.add(parentAlurPersetujuanSuratMasuk != null || searchyayasan.getSelectedItem() == null
								|| searchyayasan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("yayasan"),
												CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false)))

						.setMaxResults(index == null ? 10000 : 1).setFirstResult(index == null ? 0 : index)
						.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("defaultItem", true))
						.add(parentAlurPersetujuanSuratMasuk == null ? Restrictions.isNull("parent")
								: Restrictions.eq("parent", parentAlurPersetujuanSuratMasuk))
						.addOrder(Order.asc("nama")).addOrder(Order.desc("id")), AlurPersetujuanSuratMasuk.class);

		return alurPersetujuanSuratMasuks;
	}

	public void generateAllChildren(AlurPersetujuanSuratMasuk parentAlurPersetujuanSuratMasuk,
			Set<AlurPersetujuanSuratMasuk> alurPersetujuanSuratMasuks) {
		if (!isLeaf(parentAlurPersetujuanSuratMasuk)) {
			List<AlurPersetujuanSuratMasuk> kerjas = getChildren(parentAlurPersetujuanSuratMasuk);
			for (AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk : kerjas) {
				alurPersetujuanSuratMasuks.add(alurPersetujuanSuratMasuk);
				generateAllChildren(alurPersetujuanSuratMasuk, alurPersetujuanSuratMasuks);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		AlurPersetujuanSuratMasuk parentAlurPersetujuanSuratMasuk = (AlurPersetujuanSuratMasuk) parent;
		List<AlurPersetujuanSuratMasuk> alurPersetujuanSuratMasuks = getChildren(parentAlurPersetujuanSuratMasuk,
				index);
		AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk = alurPersetujuanSuratMasuks.size() > 0
				? alurPersetujuanSuratMasuks.get(0)
				: null;
		return alurPersetujuanSuratMasuk;
	}

	public int getChildCount(Object parent) {
		AlurPersetujuanSuratMasuk parentAlurPersetujuanSuratMasuk = (AlurPersetujuanSuratMasuk) parent;
		Session session = HibernateUtil.currentNativeSession();
		Integer count = ((Number) session.createCriteria(AlurPersetujuanSuratMasuk.class)
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("defaultItem", true))
				
				.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))

				.add(parentAlurPersetujuanSuratMasuk == null ? Restrictions.isNull("parent")
						: Restrictions.eq("parent", parentAlurPersetujuanSuratMasuk))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		HibernateUtil.closeSession();
		return count;
	}

	public void deleteChilds(Object parent) {
		AlurPersetujuanSuratMasuk parentAlurPersetujuanSuratMasuk = (AlurPersetujuanSuratMasuk) parent;
		Session session = HibernateUtil.currentSession();
		List<AlurPersetujuanSuratMasuk> alurPersetujuanSuratMasuks = getChildren(parentAlurPersetujuanSuratMasuk);
		for (AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk : alurPersetujuanSuratMasuks) {
			if (getChildCount(alurPersetujuanSuratMasuk) == 0) {
				session.delete(alurPersetujuanSuratMasuk);
			} else {
				deleteChilds(alurPersetujuanSuratMasuk);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk, AlurPersetujuanSuratMasuk obj,
			List<Long> longs) {
		Session session = HibernateUtil.currentSession();
		if (alurPersetujuanSuratMasuk.getParent() == null) {
			obj.setDeep(longs.size());
			Common.refreshUpdate(session, (obj));
		} else {
			AlurPersetujuanSuratMasuk parentAlurPersetujuanSuratMasuk = (AlurPersetujuanSuratMasuk) ConstantValues
					.simpleObject(
							session.createCriteria(AlurPersetujuanSuratMasuk.class)
							.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))
									.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
											: Restrictions.eq("defaultItem", true))
									.add(Restrictions.idEq(alurPersetujuanSuratMasuk.getParent().getId())),
							AlurPersetujuanSuratMasuk.class);
			longs.add(parentAlurPersetujuanSuratMasuk.getId());
			getParentCount(parentAlurPersetujuanSuratMasuk, obj, longs);
		}

	}

	public void getParentSet(AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk,
			List<AlurPersetujuanSuratMasuk> alurPersetujuanSuratMasuks) {
		Session session = HibernateUtil.currentSession();
		if (alurPersetujuanSuratMasuk.getParent() != null) {
			AlurPersetujuanSuratMasuk parentAlurPersetujuanSuratMasuk = (AlurPersetujuanSuratMasuk) ConstantValues
					.simpleObject(
							session.createCriteria(AlurPersetujuanSuratMasuk.class)
							.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))
									.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
											: Restrictions.eq("defaultItem", true))
									.add(Restrictions.idEq(alurPersetujuanSuratMasuk.getParent().getId())),
							AlurPersetujuanSuratMasuk.class);
			if (parentAlurPersetujuanSuratMasuk != null) {
				alurPersetujuanSuratMasuks.add(parentAlurPersetujuanSuratMasuk);
				getParentSet(parentAlurPersetujuanSuratMasuk, alurPersetujuanSuratMasuks);
			}
		}

	}

	public void getChildsSet(AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk,
			Set<AlurPersetujuanSuratMasuk> alurPersetujuanSuratMasuks) {
		List<AlurPersetujuanSuratMasuk> childs = getChildren(alurPersetujuanSuratMasuk);
		for (AlurPersetujuanSuratMasuk myAlurPersetujuanSuratMasuk : childs) {
			alurPersetujuanSuratMasuks.add(myAlurPersetujuanSuratMasuk);
			if (!isLeaf(myAlurPersetujuanSuratMasuk)) {
				getChildsSet(myAlurPersetujuanSuratMasuk, alurPersetujuanSuratMasuks);
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