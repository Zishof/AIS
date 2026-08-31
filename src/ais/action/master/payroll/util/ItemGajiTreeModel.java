package ais.action.master.payroll.util;

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
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.GajiTabahan;
import ais.database.model.payroll.ItemGaji;
import ais.ui.util.WaktuUtil;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 * Tipe khusus untuk item gaji tree model. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * AbstractTreeModel}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Boolean tampilkanSemua}, {@code
 * FormatItemGaji formatItemGaji}, {@code Date sekarang}, {@code Map dataVar}; pembacaan/pencarian ({@code
 * getChildren()}, {@code getChildren()}, {@code getChild()}, {@code getChildCount()}, {@code getParentCount()},
 * {@code getParentSet()}); validasi/perhitungan ({@code hitungItemGaji()}, {@code hitungItemGaji()});
 * penghapusan/pembatalan ({@code deleteChilds()}); operasi domain lain ({@code generateAllChildren()}, {@code
 * isLeaf()}, {@code copyByFormat()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see AbstractTreeModel
 */
public class ItemGajiTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5115651721345571411L;
	private Boolean tampilkanSemua;
	private FormatItemGaji formatItemGaji;
	private Date sekarang = WaktuUtil.getDate();
	private Map<String, Double> dataVar = new HashMap<String, Double>();

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public ItemGajiTreeModel(Boolean tampilkanSemua, FormatItemGaji formatItemGaji) {
		super(null);
		this.formatItemGaji = formatItemGaji;
		this.tampilkanSemua = tampilkanSemua;
	}

	/**
	 * Constructor
	 * 
	 * @param tree the list is contained all data of nodes.
	 */
	public ItemGajiTreeModel(ItemGaji parentItemGaji, Boolean tampilkanSemua, FormatItemGaji formatItemGaji) {
		super(parentItemGaji);
		this.formatItemGaji = formatItemGaji;
		this.tampilkanSemua = tampilkanSemua;
	}

	public List<ItemGaji> getChildren(ItemGaji parentItemGaji) {
		return getChildren(parentItemGaji, null);
	}

	@SuppressWarnings("unchecked")
	public List<ItemGaji> getChildren(ItemGaji parentItemGaji, Integer index) {
		Session session = HibernateUtil.currentSession();
		List<ItemGaji> itemGajis =

				ConstantValues.simpleList(

						session.createCriteria(ItemGaji.class)

								.add(Restrictions.eq("formatItemGaji", formatItemGaji))
								.setMaxResults(index == null ? 10000 : 1).setFirstResult(index == null ? 0 : index)
								.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("aktif", true))
								.add(parentItemGaji == null ? Restrictions.isNull("parent")
										: Restrictions.eq("parent", parentItemGaji))
								.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama")),
						ItemGaji.class);
		return itemGajis;
	}

	public void generateAllChildren(ItemGaji parentItemGaji, Set<ItemGaji> itemGajis) {
		if (!isLeaf(parentItemGaji)) {
			List<ItemGaji> kerjas = getChildren(parentItemGaji);
			for (ItemGaji itemGaji : kerjas) {
				itemGajis.add(itemGaji);
				generateAllChildren(itemGaji, itemGajis);
			}
		}
	}

	// TreeModel //
	public Object getChild(Object parent, int index) {
		ItemGaji parentItemGaji = (ItemGaji) parent;
		List<ItemGaji> itemGajis = getChildren(parentItemGaji, index);
		ItemGaji itemGaji = itemGajis.size() > 0 ? itemGajis.get(0) : null;
		// System.out.println("index = " + index + ", itemGaji = "
		// + itemGaji);
		return itemGaji;
	}

	public int getChildCount(Object parent) {
		ItemGaji parentItemGaji = (ItemGaji) parent;
		Session session = HibernateUtil.currentSession();
		Integer count = ((Number) session.createCriteria(ItemGaji.class)

				.add(Restrictions.eq("formatItemGaji", formatItemGaji))
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))

				.add(parentItemGaji == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parentItemGaji))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		return count;
	}

	public void deleteChilds(Object parent) {
		ItemGaji parentItemGaji = (ItemGaji) parent;
		Session session = HibernateUtil.currentSession();
		List<ItemGaji> itemGajis = getChildren(parentItemGaji);
		for (ItemGaji itemGaji : itemGajis) {
			if (getChildCount(itemGaji) == 0) {
				session.delete(itemGaji);
			} else {
				deleteChilds(itemGaji);
			}
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(ItemGaji itemGaji, List<Long> longs) {
		if (itemGaji.getParent() == null) {
			int size = longs.size();
			if (itemGaji.getDeep() == null || !itemGaji.getDeep().equals(size)) {
				itemGaji.setDeep(size);
				Common.refreshSaveOrUpdate(itemGaji);
			}
		} else {
			ItemGaji parentItemGaji = (ItemGaji) ConstantValues.ambil(ItemGaji.class.getName(),
					itemGaji.getParent().getId());
			longs.add(itemGaji.getParent().getId());
			getParentCount(parentItemGaji, longs);
		}

	}

	public void getParentSet(ItemGaji itemGaji, List<ItemGaji> itemGajis) {
		Session session = HibernateUtil.currentSession();
		if (itemGaji.getParent() != null) {
			ItemGaji parentItemGaji = (ItemGaji) ConstantValues.simpleObject(session.createCriteria(ItemGaji.class)

					.add(Restrictions.eq("formatItemGaji", formatItemGaji))
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
					.add(Restrictions.idEq(itemGaji.getParent().getId())), ItemGaji.class);
			if (parentItemGaji != null) {
				itemGajis.add(parentItemGaji);
				getParentSet(parentItemGaji, itemGajis);
			}
		}

	}

	public void getChildsSet(ItemGaji itemGaji, Set<ItemGaji> itemGajis) {
		List<ItemGaji> childs = getChildren(itemGaji);
		for (ItemGaji myItemGaji : childs) {
			itemGajis.add(myItemGaji);
			if (!isLeaf(myItemGaji)) {
				getChildsSet(myItemGaji, itemGajis);
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

	public void copyByFormat(ItemGaji parent, ItemGaji newParent, FormatItemGaji toFormatItemGaji) {
		Session session = HibernateUtil.currentSession();
		@SuppressWarnings("unchecked")
		List<ItemGaji> itemGajis = ConstantValues.simpleList(
				session.createCriteria(ItemGaji.class).add(Restrictions.eq("formatItemGaji", formatItemGaji))
						.add(parent == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parent)),
				ItemGaji.class);
		System.out.println("itemGajis = " + itemGajis.size());
		for (ItemGaji itemGaji : itemGajis) {
			ItemGaji newGaji = (ItemGaji) itemGaji.clone();
			newGaji.setId(null);
			newGaji.setFormatItemGaji(toFormatItemGaji);
			newGaji.setParent(newParent);
			session.save(newGaji);
			if (getChildCount(itemGaji) > 0) {
				copyByFormat(itemGaji, newGaji, toFormatItemGaji);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public Double hitungItemGaji(ItemGaji parent) {

		if (parent.getDefaultFormula() == null || parent.getDefaultFormula().trim().equals("")) {
			return 0.0;
		}

		Session session = HibernateUtil.currentSession();
		List<GajiTabahan> gajiTabahansLagi = ConstantValues.simpleList(session.createCriteria(GajiTabahan.class)

				.add(Restrictions.or(Restrictions.ge("cabang", formatItemGaji.getCabang()),
						Restrictions.isNull("cabang")))
				.add(Restrictions.or(Restrictions.ge("departemen", formatItemGaji.getDepartemen()),
						Restrictions.isNull("departemen")))
				.add(Restrictions.or(Restrictions.ge("levelJabatan", formatItemGaji.getLevelJabatan()),
						Restrictions.isNull("levelJabatan")))
				
				.add(Restrictions.isNull("pegawai"))

				.setProjection(Projections.property("id")).add(Restrictions.le("mulai", sekarang))
				.add(Restrictions.or(Restrictions.ge("sampai", sekarang), Restrictions.isNull("sampai"))),
				GajiTabahan.class, false);

		String formula = parent == null ? null : " ( " + parent.getDefaultFormula() + " ) ";
		Collection<Konstanta> konstantas = ConstantValues.ambilBerdasarClass(Konstanta.class).values();
		return hitungItemGaji(parent, formula, gajiTabahansLagi, konstantas, 0);
	}

	private Double hitungItemGaji(ItemGaji parent, String formula, List<GajiTabahan> gajiTabahans,
			Collection<Konstanta> konstantas, int coba) {

		FormatItemGaji formatItemGaji = parent == null ? null : parent.getFormatItemGaji();
		if (formula == null || formula.trim().equals("") || formatItemGaji == null || coba > 25) {
			return 0.0;
		}

		try {
			String formulaTemp = formula.replaceAll("\\(", " ");
			formulaTemp = formulaTemp.replaceAll("\\)", " ");
			Double nilaiFormula = Double.parseDouble(formulaTemp.trim());
			return nilaiFormula;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/util/ItemGajiTreeModel.java:250");

		}

		formula = " " + formula + " ";
		formula = formula.replaceAll("\\(", " ( ");
		formula = formula.replaceAll("\\)", " ) ");
		formula = formula.replaceAll("\\+", " + ");
		formula = formula.replaceAll("\\-", " - ");
		formula = formula.replaceAll("\\*", " * ");
		formula = formula.replaceAll("/", " / ");
		formula = formula.replaceAll("\n", " ");formula = formula.replaceAll("%", " % ");

		for (Konstanta konstanta : konstantas) {
			if (konstanta.getAktif() && konstanta.getKode() != null) {
				if (StringUtils.contains(formula, " " + konstanta.getKode() + " ")) {
					formula = org.apache.commons.lang3.StringUtils.replace(formula, " " + konstanta.getKode() + " ",
							" " + konstanta.getKeterangan() + " ");
				}
			}

		}

		for (GajiTabahan gajiTabahan : gajiTabahans) {
			if (gajiTabahan != null && gajiTabahan.getKode() != null && !gajiTabahan.getKode().trim().equals("")
					&& !gajiTabahan.getNama().trim().equals("")) {
				formula = org.apache.commons.lang3.StringUtils.replace(formula, " " + gajiTabahan.getKode().trim() + " ",
						" ( " + gajiTabahan.getNama().trim() + " ) ");
			}
		}

		formula = " " + formula + " ";
		formula = formula.replaceAll("\\(", " ( ");
		formula = formula.replaceAll("\\)", " ) ");
		formula = formula.replaceAll("\\+", " + ");
		formula = formula.replaceAll("\\-", " - ");
		formula = formula.replaceAll("\\*", " * ");
		formula = formula.replaceAll("/", " / ");
		formula = formula.replaceAll(",", " , ");
		formula = formula.replaceAll("\n", " ");formula = formula.replaceAll("%", " % ");
		formula = formula.replaceAll("\n", " ");
		formula = org.apache.commons.lang3.StringUtils.replace(formula, "!=", " != ");
		formula = org.apache.commons.lang3.StringUtils.replace(formula, ">=", " >= ");
		formula = org.apache.commons.lang3.StringUtils.replace(formula, "<=", " <= ");
		formula = org.apache.commons.lang3.StringUtils.replace(formula, "<", " < ");
		formula = org.apache.commons.lang3.StringUtils.replace(formula, ">", " > ");

		formula = org.apache.commons.lang3.StringUtils.replace(formula, "< =", " <= ");
		formula = org.apache.commons.lang3.StringUtils.replace(formula, "> =", " >= ");

		for (String kd : dataVar.keySet()) {
			if (kd != null) {
				if (StringUtils.contains(formula, " " + kd + " ")) {
					formula = org.apache.commons.lang3.StringUtils.replace(formula, " " + kd + " ", " " + dataVar.get(kd) + " ");
				}
			}
		}

		String formulaTemp = formula.replaceAll("\\(", " ");
		formulaTemp = formulaTemp.replaceAll("\\)", " ");
		formulaTemp = formulaTemp.replaceAll("\\+", " ");
		formulaTemp = formulaTemp.replaceAll("\\-", " ");
		formulaTemp = formulaTemp.replaceAll("\\*", " ");
		formulaTemp = formulaTemp.replaceAll("/", " ");
		formulaTemp = formulaTemp.replaceAll("%", " ");
		formulaTemp = org.apache.commons.lang3.StringUtils.replace(formulaTemp, "!=", " ");
		formulaTemp = org.apache.commons.lang3.StringUtils.replace(formulaTemp, ">=", " ");
		formulaTemp = org.apache.commons.lang3.StringUtils.replace(formulaTemp, "<=", " ");
		formulaTemp = org.apache.commons.lang3.StringUtils.replace(formulaTemp, ">", " ");
		formulaTemp = org.apache.commons.lang3.StringUtils.replace(formulaTemp, "<", " ");

		for (int i = 0; i < 50; i++) {
			formulaTemp = formulaTemp.replaceAll("  ", " ");
		}

		Session session = HibernateUtil.currentSession();
		Map<String, ItemGaji> itemGajiSet = new HashMap<String, ItemGaji>();
		String[] splits = formulaTemp.split(" ");
		for (String hasil : splits) {
			if (hasil != null && !hasil.trim().isEmpty() && !hasil.trim().equalsIgnoreCase("if")
					&& !hasil.trim().equalsIgnoreCase("avg") && !hasil.equalsIgnoreCase("sum")
					&& !hasil.equalsIgnoreCase("roundup") && !hasil.equalsIgnoreCase("rounddown")
					&& !hasil.trim().equalsIgnoreCase("upper")) {
				if (!Common.isNumber(hasil)) {
					ItemGaji itemGaji = (ItemGaji) ConstantValues.simpleObject(
							session.createCriteria(ItemGaji.class).add(Restrictions.eq("kode", hasil.trim()))
									.add(Restrictions.eq("formatItemGaji", formatItemGaji)).setMaxResults(1),
							ItemGaji.class);
					itemGajiSet.put(hasil, itemGaji);
				}
			}
		}

		double result = 0.0;

		try {
			Expression e = new ExpressionBuilder(formula).variables(itemGajiSet.keySet())
					.functions(LogicalUtil.ALL_FUNCTION)
					.operator(LogicalUtil.ALL_OPERATOR).build();
			for (ItemGaji itemGaji : itemGajiSet.values()) {
				try {
					String key = itemGaji.getKode();
					if (!key.isEmpty() && !key.equalsIgnoreCase("if") && !key.equalsIgnoreCase("avg")
							&& !key.equalsIgnoreCase("roundup") && !key.equalsIgnoreCase("rounddown")
							&& !key.equalsIgnoreCase("sum") && !key.equalsIgnoreCase("upper")) {
						Double t = itemGaji == null || itemGaji.getDefaultFormula().trim().isEmpty() ? 0.0
								: hitungItemGaji(itemGaji, " ( " + itemGaji.getDefaultFormula() + " ) ", gajiTabahans,
										konstantas, ++coba);
						e.setVariable(key, t);
					}
				} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/payroll/util/ItemGajiTreeModel.java:360");

				}
			}
			try {
				boolean valid = e.validate().isValid();
//				System.out.println("formula = " + formula + " valid => " + valid);
				if (!valid) {
					List<String> d = e.validate().getErrors();
					if (!d.isEmpty()) {
						String ds = "";
						for (String dd : d) {
							ds += ds.isEmpty() ? dd : ".\n" + dd;
						}
//						System.out.println("error = " + ds);
					}
				}
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/payroll/util/ItemGajiTreeModel.java:378");
			}
			result = e.evaluate();
		} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/payroll/util/ItemGajiTreeModel.java:381");

		}

		dataVar.put(formatItemGaji.getKode(), result);

		return result;

	}
}