package ais.action.master.kpi.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.LogicalUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konstanta;
import ais.database.model.ParameterTambahan;
import ais.database.model.kpi.FormatKpi;
import ais.database.model.kpi.FormatKpiDetail;
import ais.database.model.kpi.ItemKpi;
import ais.database.model.kpi.Kpi;
import ais.database.model.kpi.NilaiKpi;
import ais.database.model.kpi.PenilaianKpi;
import ais.ui.util.WaktuUtil;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class ItemKpiTreeModel extends AbstractTreeModel {
	private static boolean kodeVariabelValid(String kode) {
		return kode != null && kode.matches("[A-Za-z_][A-Za-z0-9_]*");
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private Boolean tampilkanSemua = false;
	private FormatKpiDetail formatKpiDetail;

	private Date sekarang = WaktuUtil.getDate();
	private ArrayList<NilaiKpi> nilaiKpis;

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public ItemKpiTreeModel(Boolean tampilkanSemua, FormatKpiDetail formatKpiDetail) {
		super(null);
		this.formatKpiDetail = formatKpiDetail;
		this.tampilkanSemua = tampilkanSemua;
	}

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public ItemKpiTreeModel(ItemKpi parentItemKpi, Boolean tampilkanSemua, FormatKpiDetail formatKpiDetail) {
		super(parentItemKpi);
		this.formatKpiDetail = formatKpiDetail;
		this.tampilkanSemua = tampilkanSemua;
	}

	public List<ItemKpi> getChildren(ItemKpi parentItemKpi) {
		return getChildren(parentItemKpi, null);
	}

	@SuppressWarnings("unchecked")
	public List<ItemKpi> getChildren(ItemKpi parentItemKpi, Integer index) {
		Session session = HibernateUtil.currentSession();
		List<ItemKpi> itemKpis =

				ConstantValues.simpleList(session.createCriteria(ItemKpi.class)
						.add(Restrictions.or(Restrictions.eq("formatKpiDetail", formatKpiDetail),
								Restrictions.and(Restrictions.eq("formatKpi", formatKpiDetail.getFormatKpi()),
										Restrictions.isNull("formatKpiDetail"))))
						.setMaxResults(index == null ? 10000 : 1).setFirstResult(index == null ? 0 : index)
						.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
						.add(parentItemKpi == null ? Restrictions.isNull("parent")
								: Restrictions.eq("parent", parentItemKpi))
						.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("kode")), ItemKpi.class);
		return itemKpis;
	}

	public void generateAllChildren(ItemKpi parentItemKpi, Set<ItemKpi> itemKpis) {
		if (!isLeaf(parentItemKpi)) {
			List<ItemKpi> kerjas = getChildren(parentItemKpi);
			for (ItemKpi itemKpi : kerjas) {
				itemKpis.add(itemKpi);
				generateAllChildren(itemKpi, itemKpis);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		ItemKpi parentItemKpi = (ItemKpi) parent;
		List<ItemKpi> itemKpis = getChildren(parentItemKpi, index);
		ItemKpi itemKpi = itemKpis.size() > 0 ? itemKpis.get(0) : null;
		// System.out.println("index = " + index + ", itemKpi = "
		// + itemKpi);
		return itemKpi;
	}

	public int getChildCount(Object parent) {
		ItemKpi parentItemKpi = (ItemKpi) parent;
		Session session = HibernateUtil.currentSession();
		Integer count = ((Number) session.createCriteria(ItemKpi.class)

				.add(Restrictions.or(Restrictions.eq("formatKpiDetail", formatKpiDetail),
						Restrictions.and(Restrictions.eq("formatKpi", formatKpiDetail.getFormatKpi()),
								Restrictions.isNull("formatKpiDetail"))))
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))

				.add(parentItemKpi == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parentItemKpi))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		return count;
	}

	public void deleteChilds(Object parent) {
		ItemKpi parentItemKpi = (ItemKpi) parent;
		Session session = HibernateUtil.currentSession();
		List<ItemKpi> itemKpis = getChildren(parentItemKpi);
		for (ItemKpi itemKpi : itemKpis) {
			if (getChildCount(itemKpi) == 0) {
				session.delete(itemKpi);
			} else {
				deleteChilds(itemKpi);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(ItemKpi itemKpi, List<Long> longs) {

		if (itemKpi.getParent() == null) {
			int size = longs.size();
			if (itemKpi.getDeep() == null || !itemKpi.getDeep().equals(size)) {
				itemKpi.setDeep(size);
				Common.refreshSaveOrUpdate(itemKpi);
			}
		} else {
			ItemKpi parentItemKpi = (ItemKpi) ConstantValues.ambil(ItemKpi.class.getName(),
					itemKpi.getParent().getId());
			longs.add(itemKpi.getParent().getId());
			getParentCount(parentItemKpi, longs);
		}

	}

	public void getParentSet(ItemKpi itemKpi, List<ItemKpi> itemKpis) {
		Session session = HibernateUtil.currentSession();
		if (itemKpi.getParent() != null) {
			ItemKpi parentItemKpi = (ItemKpi) ConstantValues.simpleObject(session.createCriteria(ItemKpi.class)

					.add(Restrictions.or(Restrictions.eq("formatKpiDetail", formatKpiDetail),
							Restrictions.and(Restrictions.eq("formatKpi", formatKpiDetail.getFormatKpi()),
									Restrictions.isNull("formatKpiDetail"))))
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
					.add(Restrictions.idEq(itemKpi.getParent().getId())), ItemKpi.class);
			if (parentItemKpi != null) {
				itemKpis.add(parentItemKpi);
				getParentSet(parentItemKpi, itemKpis);
			}
		}

	}

	public void getChildsSet(ItemKpi itemKpi, Set<ItemKpi> itemKpis) {
		List<ItemKpi> childs = getChildren(itemKpi);
		for (ItemKpi myItemKpi : childs) {
			itemKpis.add(myItemKpi);
			if (!isLeaf(myItemKpi)) {
				getChildsSet(myItemKpi, itemKpis);
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

	public void copyByFormat(ItemKpi parent, ItemKpi newParent, FormatKpi toFormatKpi,
			FormatKpiDetail newformatKpiDetail) {
		Session session = HibernateUtil.currentSession();
		@SuppressWarnings("unchecked")
		List<ItemKpi> itemKpis = ConstantValues.simpleList(
				session.createCriteria(ItemKpi.class)
						
				.add(Restrictions.or(Restrictions.eq("formatKpiDetail", formatKpiDetail),
								Restrictions.and(Restrictions.eq("formatKpi", formatKpiDetail.getFormatKpi()),
										Restrictions.isNull("formatKpiDetail"))))
						
						.add(parent == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parent)),
				ItemKpi.class);
		System.out.println("itemKpis = " + itemKpis.size());
		for (ItemKpi itemKpi : itemKpis) {
			ItemKpi newKpi = (ItemKpi) itemKpi.clone();
			newKpi.setId(null);
			newKpi.setFormatKpi(toFormatKpi);
			newKpi.setParent(newParent);
			newKpi.setFormatKpiDetail(formatKpiDetail);
			session.save(newKpi);
			if (getChildCount(itemKpi) > 0) {
				copyByFormat(itemKpi, newKpi, toFormatKpi, newformatKpiDetail);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public Double hitungItemKpi(ItemKpi parent, String formula, boolean refresh, List<String> penghitungan)
			throws Exception {

		if (formula == null || formula.trim().equals("")) {
			return 0.0;
		}

		ArrayList<ItemKpi> itemKpis = new ArrayList<ItemKpi>();
		for (Object o : ConstantValues.ambilBerdasarClass(ItemKpi.class).values()) {
			ItemKpi itemKpi = (ItemKpi) o;
			if (itemKpi != null && itemKpi.getFormatKpi() != null && ((itemKpi.getFormatKpiDetail() != null
					&& itemKpi.getFormatKpiDetail().getId().equals(formatKpiDetail.getId()))
					|| (itemKpi.getFormatKpiDetail() == null
							&& itemKpi.getFormatKpi().getId().equals(formatKpiDetail.getFormatKpi().getId())))) {
				itemKpis.add(itemKpi);
			}
		}

		formula = " ( " + formula + " ) ";
		Collection<Konstanta> konstantas = ConstantValues.ambilBerdasarClass(Konstanta.class).values();
		Double nilai = hitungItemKpi(parent, formula, konstantas, itemKpis, sekarang, refresh, 0, penghitungan);
		itemKpis = null;
		konstantas = null;
		return nilai;
	}

	public static Double hitungItemKpi(ItemKpi parent, String formula, Collection<Konstanta> konstantas,
			List<ItemKpi> itemKpis, Date sekarang, boolean refresh, int coba, List<String> penghitungan)
			throws Exception {
		FormatKpi formatKpi = parent == null ? null : parent.getFormatKpi();
		if (formula == null || formula.trim().equals("") || formatKpi == null || coba > 500) {
			return 0.0;
		}

		try {
			String formulaTemp = formula.replaceAll("\\(", " ");
			formulaTemp = formulaTemp.replaceAll("\\)", " ");
			Double nilaiFormula = Double.parseDouble(formulaTemp.trim());
			return nilaiFormula;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/ItemKpiTreeModel.java:264");

		}
		formula = " " + formula + " ";
		formula = formula.replaceAll("\\(", " ( ");
		formula = formula.replaceAll("\\)", " ) ");
		formula = formula.replaceAll("\\+", " + ");
		formula = formula.replaceAll("\\-", " - ");
		formula = formula.replaceAll("\\*", " * ");
		formula = formula.replaceAll("/", " / ");
		formula = formula.replaceAll("\n", " ");
		formula = formula.replaceAll("%", " % ");

		for (Konstanta konstanta : konstantas) {
			if (konstanta.getAktif() && konstanta.getKode() != null) {
				if (StringUtils.contains(formula, " " + konstanta.getKode() + " ")) {
					formula = org.apache.commons.lang3.StringUtils.replace(formula, " " + konstanta.getKode() + " ",
							" " + konstanta.getKeterangan() + " ");
				}
			}
		}

		if (parent.getAktif()) {
			if (StringUtils.contains(formula, " " + parent.getKode() + " ")) {
				formula = org.apache.commons.lang3.StringUtils.replace(formula, " " + parent.getKode() + " ",
						" " + parent.getVal() + " ");
			}
		}

		if (parent.getAktif()) {
			ParameterTambahan parameterTambahan = parent.getKpi().getSatuanKpi() == null ? null
					: parent.getKpi().getSatuanKpi().getParameterTambahan();
			if (parameterTambahan != null) {
				if (StringUtils.contains(formula, " " + parameterTambahan.getKode() + " ")) {
					formula = org.apache.commons.lang3.StringUtils.replace(formula,
							" " + parameterTambahan.getKode() + " ", " " + parent.getVal() + " ");
				}
			}
		}

		for (ItemKpi itemKpi : itemKpis) {
			Kpi kpi = null;
			try {
				kpi = itemKpi.getKpi();
			} catch (Exception e) {
				try {
					HibernateUtil.currentSession().refresh(itemKpi);
					kpi = itemKpi.getKpi();
				} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/ItemKpiTreeModel.java:312");
					// TODO: handle exception
				}
			}
			if (kpi != null) {
				if (kpi.getAktif() && kpi.getKode() != null) {
					if (StringUtils.contains(formula, " " + kpi.getKode() + " ")) {
						Double poin = KpiUtil.ambilPoint(kpi.getFormula(), sekarang, refresh);
						formula = org.apache.commons.lang3.StringUtils.replace(formula, " " + kpi.getKode() + " ",
								" " + poin.toString() + " ");
					}
				}
			}
		}

		for (ItemKpi itemKpi : itemKpis) {
			String kode = itemKpi.getKode();
			if (itemKpi.getAktif() && kode != null) {
				if (StringUtils.contains(formula, " " + kode + " ")) {
					String f = KpiUtil.ambilTarget(itemKpi.getFormula(), sekarang);
					Double hasil = refresh
							? hitungItemKpi(itemKpi, " ( " + f + " ) ", konstantas, itemKpis, sekarang, refresh, ++coba,
									penghitungan)
							: itemKpi.getTarget();
					formula = org.apache.commons.lang3.StringUtils.replace(formula, " " + kode + " ",
							" " + hasil.toString() + " ");
				}
			}
		}

		if (penghitungan != null) {
			penghitungan.add(formula);
		}

		String formulaTemp = formula.replaceAll("\\(", " ");
		formulaTemp = formulaTemp.replaceAll("\\)", " ");
		formulaTemp = formulaTemp.replaceAll("\\+", " ");
		formulaTemp = formulaTemp.replaceAll("\\-", " ");
		formulaTemp = formulaTemp.replaceAll("\\*", " ");
		formulaTemp = formulaTemp.replaceAll("/", " ");
		formulaTemp = formulaTemp.replaceAll("%", " ");

		for (int i = 0; i < 50; i++) {
			formulaTemp = formulaTemp.replaceAll("  ", " ");
		}

		Session session = HibernateUtil.currentSession();
		Map<String, ItemKpi> itemKpiSet = new HashMap<String, ItemKpi>();
		String[] splits = formulaTemp.split(" ");
		int nomorAlias = 0;
		for (String hasil : splits) {
			if (hasil != null && hasil.trim().length() > 0 && !Common.isNumber(hasil)) {
				ItemKpi itemKpi = (ItemKpi) ConstantValues
						.simpleObject(session.createCriteria(ItemKpi.class).add(Restrictions.eq("kode", hasil.trim()))
								.add(Restrictions.eq("formatKpi", formatKpi)).setMaxResults(1), ItemKpi.class);
				if (itemKpi != null) {
					String variabel = kodeVariabelValid(hasil) ? hasil : "KPI_VAR_" + nomorAlias++;
					if (!variabel.equals(hasil)) {
						formula = org.apache.commons.lang3.StringUtils.replace(formula,
								" " + hasil + " ", " " + variabel + " ");
					}
					itemKpiSet.put(variabel, itemKpi);
				}
			}
		}

		double result = 0.0;

		if (Common.isNumber(formula)) {
			result = Double.parseDouble(formula.trim());
		} else {

			try {
				Expression e = new ExpressionBuilder(formula).variables(itemKpiSet.keySet())
						.functions(LogicalUtil.ALL_FUNCTION).operator(LogicalUtil.ALL_OPERATOR).build();
				for (Map.Entry<String, ItemKpi> entri : itemKpiSet.entrySet()) {
					ItemKpi itemKpi = entri.getValue();
					try {
						Double t = itemKpi == null || itemKpi.getFormula() == null
								|| itemKpi.getFormula().trim().equals("")
										? 0.0
										: (refresh
												? hitungItemKpi(itemKpi, " ( " + itemKpi.getFormula() + " ) ",
														konstantas, itemKpis, sekarang, refresh, ++coba, penghitungan)
												: itemKpi.getTarget());
						e.setVariable(entri.getKey(), t == null ? 0.0 : t.doubleValue());
					} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/ItemKpiTreeModel.java:389");

					}
				}
				try {
					boolean valid = e.validate().isValid();
					if (coba == 0 && formula.length() > 50) {
						System.out.println("formula = " + formula + " valid => " + valid);
					}
					if (!valid) {
						List<String> d = e.validate().getErrors();
						if (!d.isEmpty()) {
							String ds = "";
							for (String dd : d) {
								ds += ds.isEmpty() ? dd : ".\n" + dd;
							}
							System.out.println("error = " + ds);
						}
					}
				} catch (Exception ee) {
					ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/kpi/helper/ItemKpiTreeModel.java:409");
				}
				result = e.evaluate();
				if (coba == 0 && formula.length() > 50) {
					System.out.println("result = " + result);
				}

			} catch (Exception ee) {
				System.out.println("error formula = " + formula);
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/kpi/helper/ItemKpiTreeModel.java:418");
			}
		}
		return result;

	}

	@SuppressWarnings("unchecked")
	public Double hitungNilaiKpi(NilaiKpi parent, String formula, PenilaianKpi penilaianKpi, boolean refresh,
			List<String> penghitungan) throws Exception {

		if (formula == null || formula.trim().equals("")) {
			return 0.0;
		}

		nilaiKpis = new ArrayList<NilaiKpi>();
		for (Object o : ConstantValues.ambilBerdasarClass(NilaiKpi.class).values()) {
			NilaiKpi nilaiKpi = (NilaiKpi) o;
			if (nilaiKpi != null && nilaiKpi.getPenilaianKpi() != null
					&& nilaiKpi.getPenilaianKpi().getId().equals(penilaianKpi.getId())) {
				nilaiKpis.add(nilaiKpi);
			}
		}

		formula = " ( " + formula + " ) ";
		Collection<Konstanta> konstantas = ConstantValues.ambilBerdasarClass(Konstanta.class).values();
		Double nilai = hitungNilaiKpi(parent, formula, penilaianKpi, konstantas, refresh, 0, penghitungan);
		nilaiKpis = null;
		konstantas = null;
		return nilai;
	}

	private Double hitungNilaiKpi(NilaiKpi parent, String formula, PenilaianKpi penilaianKpi,
			Collection<Konstanta> konstantas, boolean refresh, int coba, List<String> penghitungan) throws Exception {

		if (formula == null || formula.trim().equals("") || coba > 500) {
			return 0.0;
		}

		try {
			String formulaTemp = formula.replaceAll("\\(", " ");
			formulaTemp = formulaTemp.replaceAll("\\)", " ");
			Double nilaiFormula = Double.parseDouble(formulaTemp.trim());
			return nilaiFormula;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/ItemKpiTreeModel.java:462");

		}
		formula = " " + formula + " ";
		formula = formula.replaceAll("\\(", " ( ");
		formula = formula.replaceAll("\\)", " ) ");
		formula = formula.replaceAll("\\+", " + ");
		formula = formula.replaceAll("\\-", " - ");
		formula = formula.replaceAll("\\*", " * ");
		formula = formula.replaceAll("/", " / ");
		formula = formula.replaceAll("\n", " ");
		formula = formula.replaceAll("%", " % ");

		for (Konstanta konstanta : konstantas) {
			if (konstanta.getAktif() && konstanta.getKode() != null) {
				if (StringUtils.contains(formula, " " + konstanta.getKode() + " ")) {
					formula = org.apache.commons.lang3.StringUtils.replace(formula, " " + konstanta.getKode() + " ",
							" " + konstanta.getKeterangan() + " ");
				}
			}

		}

		if (parent.getItemKpi().getAktif()) {
			if (StringUtils.contains(formula, " " + parent.getKode() + " ")) {
				formula = org.apache.commons.lang3.StringUtils.replace(formula, " " + parent.getKode() + " ",
						" " + parent.getVal() + " ");
			}
		}

		if (parent.getItemKpi().getAktif()) {
			ParameterTambahan parameterTambahan = parent.getItemKpi().getKpi().getSatuanKpi() == null ? null
					: parent.getItemKpi().getKpi().getSatuanKpi().getParameterTambahan();
			if (parameterTambahan != null) {
				if (StringUtils.contains(formula, " " + parameterTambahan.getKode() + " ")) {
					formula = org.apache.commons.lang3.StringUtils.replace(formula,
							" " + parameterTambahan.getKode() + " ", " " + parent.getVal() + " ");
				}
			}
		}

		for (NilaiKpi nilaiKpi : nilaiKpis) {

			Kpi kpi = null;
			try {
				kpi = nilaiKpi.getItemKpi().getKpi();
			} catch (Exception e) {
				try {
					ItemKpi itemKpi = nilaiKpi.getItemKpi();
					HibernateUtil.currentSession().refresh(itemKpi);
					kpi = itemKpi.getKpi();
				} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/ItemKpiTreeModel.java:513");
					// TODO: handle exception
				}
			}
			if (kpi != null) {
				if (kpi.getAktif() && kpi.getKode() != null) {
					if (StringUtils.contains(formula, " " + kpi.getKode() + " ")) {
						Double poin = KpiUtil.ambilPoint(kpi.getFormula(), sekarang, refresh);
						formula = org.apache.commons.lang3.StringUtils.replace(formula, " " + kpi.getKode() + " ",
								" " + poin.toString() + " ");
					}
				}
			}
		}

		for (NilaiKpi nilaiKpi : nilaiKpis) {
			String kode = nilaiKpi.getKode();
			if (nilaiKpi.getItemKpi().getAktif() && kode != null) {
				if (StringUtils.contains(formula, " " + kode + " ")) {
					String f = KpiUtil.ambilTarget(nilaiKpi.getItemKpi().getFormula(), sekarang);
					Double hasil = refresh
							? hitungNilaiKpi(nilaiKpi, " ( " + f + " ) ", penilaianKpi, konstantas, refresh, ++coba,
									penghitungan)
							: nilaiKpi.getRealisasi();
					formula = org.apache.commons.lang3.StringUtils.replace(formula, " " + kode + " ",
							" " + hasil.toString() + " ");
				}
			}
		}

		if (penghitungan != null) {
			penghitungan.add(formula);
		}

		String formulaTemp = formula.replaceAll("\\(", " ");
		formulaTemp = formulaTemp.replaceAll("\\)", " ");
		formulaTemp = formulaTemp.replaceAll("\\+", " ");
		formulaTemp = formulaTemp.replaceAll("\\-", " ");
		formulaTemp = formulaTemp.replaceAll("\\*", " ");
		formulaTemp = formulaTemp.replaceAll("/", " ");
		formulaTemp = formulaTemp.replaceAll("%", " ");

		for (int i = 0; i < 50; i++) {
			formulaTemp = formulaTemp.replaceAll("  ", " ");
		}

		Session session = HibernateUtil.currentSession();
		Map<String, NilaiKpi> nilaiKpiSet = new HashMap<String, NilaiKpi>();
		String[] splits = formulaTemp.split(" ");
		int nomorAlias = 0;
		for (String hasil : splits) {
			if (hasil != null && hasil.trim().length() > 0 && !Common.isNumber(hasil)) {
				NilaiKpi nilaiKpi = (NilaiKpi) ConstantValues
						.simpleObject(
								session.createCriteria(NilaiKpi.class).add(Restrictions.eq("kode", hasil.trim()))
										.add(Restrictions.eq("penilaianKpi", penilaianKpi)).setMaxResults(1),
								NilaiKpi.class);
				if (nilaiKpi != null) {
					String variabel = kodeVariabelValid(hasil) ? hasil : "NILAI_VAR_" + nomorAlias++;
					if (!variabel.equals(hasil)) {
						formula = org.apache.commons.lang3.StringUtils.replace(formula,
								" " + hasil + " ", " " + variabel + " ");
					}
					nilaiKpiSet.put(variabel, nilaiKpi);
				}
			}
		}

		double result = 0.0;

		if (Common.isNumber(formula)) {
			result = Double.parseDouble(formula.trim());
		} else {

			try {
				Expression e = new ExpressionBuilder(formula).variables(nilaiKpiSet.keySet())
						.functions(LogicalUtil.ALL_FUNCTION).operator(LogicalUtil.ALL_OPERATOR).build();
				for (Map.Entry<String, NilaiKpi> entri : nilaiKpiSet.entrySet()) {
					NilaiKpi nilaiKpi = entri.getValue();
					try {
						Double t = nilaiKpi == null || nilaiKpi.getItemKpi() == null
								|| nilaiKpi.getItemKpi().getFormula() == null
								|| nilaiKpi.getItemKpi().getFormula().trim().equals("")
										? 0.0
										: refresh
												? hitungNilaiKpi(nilaiKpi,
														" ( " + nilaiKpi.getItemKpi().getFormula() + " ) ",
														penilaianKpi, konstantas, refresh, ++coba, penghitungan)
												: nilaiKpi.getRealisasi();
						e.setVariable(entri.getKey(), t == null ? 0.0 : t.doubleValue());
					} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/ItemKpiTreeModel.java:593");

					}
				}
				try {
					boolean valid = e.validate().isValid();
//					System.out.println("formula = " + formula + " valid => " + valid);
					if (!valid) {
						List<String> d = e.validate().getErrors();
						if (!d.isEmpty()) {
							String ds = "";
							for (String dd : d) {
								ds += ds.isEmpty() ? dd : ".\n" + dd;
							}
							System.out.println("error = " + ds);
						}
					}
				} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/ItemKpiTreeModel.java:610");
//					ee.printStackTrace();
				}
				result = e.evaluate();
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/ItemKpiTreeModel.java:614");
//				ee.printStackTrace();
			}
		}

		return result;

	}
}
