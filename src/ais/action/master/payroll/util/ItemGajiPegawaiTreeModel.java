package ais.action.master.payroll.util;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Period;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;
import org.zkoss.zul.Messagebox;

import ais.action.master.employ.helper.MasaKerjaUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.LogicalUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.Jabatan;
import ais.database.model.KehadiranDosenBulanan;
import ais.database.model.KehadiranPegawaiBulanan;
import ais.database.model.Konstanta;
import ais.database.model.ParameterTambahan;
import ais.database.model.Pegawai;
import ais.database.model.RekapUjianDosenBulanan;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.employ.GajiPokok;
import ais.database.model.employ.Insentif;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.Keluarga;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.employ.Makan;
import ais.database.model.employ.Transport;
import ais.database.model.kpi.PenilaianKpi;
import ais.database.model.payroll.AsuransiPegawai;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.GajiTabahan;
import ais.database.model.payroll.ItemGaji;
import ais.database.model.payroll.ItemGajiPegawai;
import ais.database.model.payroll.JenisTransaksiPegawai;
import ais.database.model.payroll.KodeTunjangan;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.database.model.payroll.RencanaGajiPunyaPegawai;
import ais.database.model.payroll.RencanaItemGajiPegawai;
import ais.database.model.payroll.TransaksiPegawai;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.ui.util.MyJSONObject;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.WaktuUtil;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 * Tipe khusus untuk item gaji pegawai tree model. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * AbstractTreeModel}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Boolean tampilkanSemua}, {@code
 * FormatItemGaji formatItemGaji}, {@code Pegawai pegawai}, {@code Collection kodeTunjangans}, {@code Collection
 * asuransiPegawais}, {@code GajiPokok gajiPokok}, {@code Insentif insentif}, {@code Map dataVar};
 * inisialisasi/lifecycle ({@code initModel()}); pembacaan/pencarian ({@code getChildren()}, {@code
 * getChildren()}, {@code getChild()}, {@code getChildCount()}, {@code getParentCount()}, {@code
 * getParentSet()}); validasi/perhitungan ({@code checkExistingItemGaji()}, {@code hitungItemGajiPegawai()},
 * {@code hitungItemGajiPegawai()}, {@code hitungItemGajiPegawai()}, {@code hitungUmurKeluarga()}, {@code
 * hitungItemGajiPegawai()}); mutasi data ({@code reset()}); penghapusan/pembatalan ({@code deleteChilds()});
 * operasi domain lain ({@code closeSession()}, {@code populateData()}, {@code populateData()}, {@code
 * generateAllChildren()}, {@code isLeaf()}, {@code copyByFormat()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see AbstractTreeModel
 */
public class ItemGajiPegawaiTreeModel extends AbstractTreeModel {

	private static final long serialVersionUID = -5115651721345571411L;
	private Boolean tampilkanSemua;
	private FormatItemGaji formatItemGaji;
	private Pegawai pegawai;
	@SuppressWarnings("rawtypes")
	private Collection kodeTunjangans = ConstantValues.ambilBerdasarClass(KodeTunjangan.class).values();
	@SuppressWarnings("rawtypes")
	private Collection asuransiPegawais = ConstantValues.ambilBerdasarClass(AsuransiPegawai.class).values();

	private GajiPokok gajiPokok = null;
	private Insentif insentif = null;
	private Map<String, Double> dataVar = new HashMap<String, Double>();
	// Kedalaman rekursi hitungItemGajiPegawai() saat ini (bukan akumulasi jumlah evaluasi
	// seumur instance) -- naik sebelum rekursi, turun di finally, agar ambang di bawah
	// membatasi rumus yang benar-benar sirkular tanpa memakan kuota rujukan silang biasa.
	private int kedalaman = 0;
	private List<KenaikanPangkat> kenaikanPangkats = null;
	private List<JabatanFungsional> jabatanFungsionals = null;
	private List<JabatanStruktural> jabatanStrukturals = null;
	private List<Jabatan> jabatans = null;
	private Date sekarangData = null;
	private Makan makan = null;
	private Transport transport = null;

	// In-memory cache to prevent N+1 Queries during deep recursive formula evaluations
	private List<Keluarga> cachedKeluargaAktif = null;

	public static final ThreadLocal<DecimalFormat> df = new ThreadLocal<DecimalFormat>() {
		@Override
		protected DecimalFormat initialValue() {
			return (DecimalFormat) NumberFormat.getNumberInstance(Common.locale);
		}
	};

	/**
	 * Constructor 1
	 */
	public ItemGajiPegawaiTreeModel(Boolean tampilkanSemua, FormatItemGaji formatItemGaji, Pegawai pegawai, Date waktu) {
		super(null);
		initModel(tampilkanSemua, formatItemGaji, pegawai, waktu);
	}

	/**
	 * Constructor 2
	 */
	public ItemGajiPegawaiTreeModel(ItemGajiPegawai parentItemGajiPegawai, Boolean tampilkanSemua,
			FormatItemGaji formatItemGaji, Pegawai pegawai, Date waktu) {
		super(parentItemGajiPegawai);
		initModel(tampilkanSemua, formatItemGaji, pegawai, waktu);
	}

	private void initModel(Boolean tampilkanSemua, FormatItemGaji formatItemGaji, Pegawai pegawai, Date waktu) {
		this.formatItemGaji = formatItemGaji;
		this.tampilkanSemua = tampilkanSemua;
		this.pegawai = pegawai;
		Date sekarang = waktu == null ? WaktuUtil.getDate() : waktu;
		sekarangData = waktu;
		gajiPokok = pegawai.ambilGajiPokok(sekarang);
		insentif = pegawai.ambilInsentif(sekarang);
		makan = pegawai.ambilMakan(sekarang);
		transport = pegawai.ambilTransport(sekarang);

		kenaikanPangkats = pegawai.ambilKenaikanPangkatData(sekarang);
		jabatanFungsionals = pegawai.ambilJabatanFungsionals(kenaikanPangkats);
		jabatanStrukturals = pegawai.ambilJabatanStrukturals(kenaikanPangkats);
		jabatans = pegawai.ambilJabatans(kenaikanPangkats);
	}

	/**
	 * Helper untuk menutup koneksi database secara aman (Java 1.6 style)
	 */
	private void closeSession(Session session) {
		if (session != null) {
			try {
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:141");
			}
		}
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:147");
		}
	}

	public List<ItemGajiPegawai> getChildren(ItemGajiPegawai parentItemGajiPegawai) {
		return getChildren(parentItemGajiPegawai, null);
	}

	@SuppressWarnings("unchecked")
	public List<ItemGajiPegawai> getChildren(ItemGajiPegawai parentItemGajiPegawai, Integer index) {
		Session session = null;
		List<ItemGajiPegawai> itemGajiPegawais = new ArrayList<ItemGajiPegawai>();
		try {
			session = HibernateUtil.currentNativeSession();
			itemGajiPegawais = ConstantValues.simpleList(session.createCriteria(ItemGajiPegawai.class)
					.add(Restrictions.eq("pegawai", pegawai)).add(Restrictions.eq("formatItemGaji", formatItemGaji))
					.createAlias("itemGaji", "itemGaji")
					.setMaxResults(index == null ? 10000 : 1).setFirstResult(index == null ? 0 : index)
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
					.add(parentItemGajiPegawai == null ? Restrictions.isNull("parent")
							: Restrictions.eq("parent", parentItemGajiPegawai))
					.addOrder(Order.asc("itemGaji.nomorUrut")).addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama")),
					ItemGajiPegawai.class);
		} finally {
			closeSession(session);
		}
		return itemGajiPegawais;
	}

	@SuppressWarnings({ "rawtypes" })
	public void populateData(List list, Map maps, Date tanggal, Integer bulan, Integer tahun, List<String> penghitungan)
			throws Exception {
		populateData(list, maps, null, tanggal, bulan, tahun, penghitungan);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void populateData(List list, Map maps, ItemGajiPegawai parentItemGajiPegawai, Date tanggal, Integer bulan,
			Integer tahun, List<String> penghitungan) throws Exception {
		List<ItemGajiPegawai> parents = getChildren(parentItemGajiPegawai);
		for (ItemGajiPegawai gajiPegawai : parents) {
			if (gajiPegawai.getTampilkanDiSlip()) {
				String tambahanDepan = "";
				Double hasil = null;
				if (!gajiPegawai.getSpace()) {
					if (getChildCount(gajiPegawai) > 0) {
						populateData(list, maps, gajiPegawai, tanggal, bulan, tahun, penghitungan);
					}
					hasil = hitungItemGajiPegawai(gajiPegawai.getKode(), gajiPegawai.getDefaultFormula(), tanggal,
							bulan, tahun, null, penghitungan);

					if (gajiPegawai != null && gajiPegawai.getItemGaji() != null
							&& gajiPegawai.getItemGaji().getJadikan0JikaMinus() && hasil != null && hasil < 0.0) {
						hasil = 0.0;
					}

					ItemGajiPegawai parentPegawai = gajiPegawai.getParent();
					while (parentPegawai != null) {
						tambahanDepan += "      ";
						parentPegawai = parentPegawai.getParent();
					}
				}

				maps.put(gajiPegawai.getKode() + "_nama", gajiPegawai.getNama());
				maps.put(gajiPegawai.getKode() + "_nilai", hasil);

				Map map = new java.util.HashMap();
				map.put("item", tambahanDepan + "" + gajiPegawai.getNama());
				map.put("nama", tambahanDepan + "" + gajiPegawai.getNama());
				map.put("nilai", hasil);
				list.add(map);
			}
		}
	}

	public void generateAllChildren(ItemGajiPegawai parentItemGajiPegawai, Set<ItemGajiPegawai> itemGajiPegawais) {
		if (!isLeaf(parentItemGajiPegawai)) {
			List<ItemGajiPegawai> kerjas = getChildren(parentItemGajiPegawai);
			for (ItemGajiPegawai itemGajiPegawai : kerjas) {
				itemGajiPegawais.add(itemGajiPegawai);
				generateAllChildren(itemGajiPegawai, itemGajiPegawais);
			}
		}
	}

	public Object getChild(Object parent, int index) {
		ItemGajiPegawai parentItemGajiPegawai = (ItemGajiPegawai) parent;
		List<ItemGajiPegawai> itemGajiPegawais = getChildren(parentItemGajiPegawai, index);
		return itemGajiPegawais.size() > 0 ? itemGajiPegawais.get(0) : null;
	}

	public int getChildCount(Object parent) {
		ItemGajiPegawai parentItemGajiPegawai = (ItemGajiPegawai) parent;
		Session session = null;
		Number count = null;
		try {
			session = HibernateUtil.currentNativeSession();
			count = ((Number) session.createCriteria(ItemGajiPegawai.class).add(Restrictions.eq("pegawai", pegawai))
					.add(Restrictions.eq("formatItemGaji", formatItemGaji))
					.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
					.add(parentItemGajiPegawai == null ? Restrictions.isNull("parent")
							: Restrictions.eq("parent", parentItemGajiPegawai))
					.setProjection(Projections.rowCount()).uniqueResult());
		} finally {
			closeSession(session);
		}
		return count == null ? 0 : count.intValue();
	}

	public void deleteChilds(Object parent) {
		ItemGajiPegawai parentItemGajiPegawai = (ItemGajiPegawai) parent;
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			List<ItemGajiPegawai> itemGajiPegawais = getChildren(parentItemGajiPegawai);
			for (ItemGajiPegawai itemGajiPegawai : itemGajiPegawais) {
				if (getChildCount(itemGajiPegawai) == 0) {
					session.delete(itemGajiPegawai);
				} else {
					deleteChilds(itemGajiPegawai);
				}
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:274");
		} finally {
			closeSession(session);
		}
	}

	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	public void getParentCount(ItemGajiPegawai itemGajiPegawai, List<Long> longs) {
		if (itemGajiPegawai.getParent() == null) {
			int size = longs.size();
			if (itemGajiPegawai.getDeep() == null || !itemGajiPegawai.getDeep().equals(size)) {
				itemGajiPegawai.setDeep(size);
				Common.refreshSaveOrUpdate(itemGajiPegawai);
			}
		} else {
			ItemGajiPegawai parentItemGajiPegawai = (ItemGajiPegawai) ConstantValues
					.ambil(ItemGajiPegawai.class.getName(), itemGajiPegawai.getParent().getId());
			if (parentItemGajiPegawai != null) {
				longs.add(itemGajiPegawai.getParent().getId());
				getParentCount(parentItemGajiPegawai, longs);
			}
		}
	}

	public void getParentSet(ItemGajiPegawai itemGajiPegawai, List<ItemGajiPegawai> itemGajiPegawais) {
		if (itemGajiPegawai.getParent() != null) {
			Session session = null;
			ItemGajiPegawai parentItemGajiPegawai = null;
			try {
				session = HibernateUtil.currentNativeSession();
				parentItemGajiPegawai = (ItemGajiPegawai) ConstantValues.simpleObject(
						session.createCriteria(ItemGajiPegawai.class).add(Restrictions.eq("pegawai", pegawai))
								.add(Restrictions.eq("formatItemGaji", formatItemGaji))
								.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
								.add(Restrictions.idEq(itemGajiPegawai.getParent().getId())),
						ItemGajiPegawai.class);
			} finally {
				closeSession(session);
			}

			if (parentItemGajiPegawai != null) {
				itemGajiPegawais.add(parentItemGajiPegawai);
				getParentSet(parentItemGajiPegawai, itemGajiPegawais);
			}
		}
	}

	public void getChildsSet(ItemGajiPegawai itemGajiPegawai, Set<ItemGajiPegawai> itemGajiPegawais) {
		List<ItemGajiPegawai> childs = getChildren(itemGajiPegawai);
		for (ItemGajiPegawai myItemGajiPegawai : childs) {
			itemGajiPegawais.add(myItemGajiPegawai);
			if (!isLeaf(myItemGajiPegawai)) {
				getChildsSet(myItemGajiPegawai, itemGajiPegawais);
			}
		}
	}

	@Override
	public int getIndexOfChild(Object arg0, Object arg1) {
		return 0;
	}

	@SuppressWarnings("unchecked")
	public void copyByFormat(ItemGajiPegawai parent, ItemGajiPegawai newParent, FormatItemGaji toFormatItemGaji) {
		Session session = null;
		List<ItemGajiPegawai> itemGajiPegawais = new ArrayList<ItemGajiPegawai>();
		try {
			session = HibernateUtil.currentNativeSession();
			itemGajiPegawais = ConstantValues.simpleList(
					session.createCriteria(ItemGajiPegawai.class).add(Restrictions.eq("pegawai", pegawai))
							.add(Restrictions.eq("formatItemGaji", formatItemGaji))
							.add(parent == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parent)),
					ItemGajiPegawai.class);
		} finally {
			closeSession(session);
		}

		for (ItemGajiPegawai itemGajiPegawai : itemGajiPegawais) {
			Session sessionSave = null;
			try {
				ItemGajiPegawai newGaji = (ItemGajiPegawai) itemGajiPegawai.clone();
				newGaji.setId(null);
				newGaji.setFormatItemGaji(toFormatItemGaji);
				newGaji.setParent(newParent);

				sessionSave = HibernateUtil.currentNativeSession();
				sessionSave.getTransaction().begin();
				sessionSave.save(newGaji);
				sessionSave.getTransaction().commit();

				if (getChildCount(itemGajiPegawai) > 0) {
					copyByFormat(itemGajiPegawai, newGaji, toFormatItemGaji);
				}
			} catch (Exception e) {
				if (sessionSave != null && sessionSave.getTransaction().isActive()) {
					sessionSave.getTransaction().rollback();
				}
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:374");
			} finally {
				closeSession(sessionSave);
			}
		}
	}

	public void reset() {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			int count = ((Number) session.createCriteria(RencanaItemGajiPegawai.class)
					.add(Restrictions.eq("pegawai", pegawai)).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();
			
			if (count == 0) {
				session.getTransaction().begin();
				session.createSQLQuery("delete from payroll.item_gaji_pegawai where pegawai = " + pegawai.getId()
						+ " and format_item_gaji = " + formatItemGaji.getId()).executeUpdate();
				session.flush();
				session.getTransaction().commit();
			}
			copyByItemGaji(null, null, formatItemGaji, session);
		} catch (Exception e) {
			if (session != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:401");
		} finally {
			closeSession(session);
		}
	}

	public void checkExistingItemGaji() {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Integer count = ((Number) session.createCriteria(ItemGajiPegawai.class)
					.add(Restrictions.eq("pegawai", pegawai))
					.add(Restrictions.eq("formatItemGaji", formatItemGaji)).add(Restrictions.isNull("parent"))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			
			if (count.equals(0)) {
				session.getTransaction().begin();
				session.createSQLQuery("delete from payroll.item_gaji_pegawai where pegawai = " + pegawai.getId()
						+ " and format_item_gaji = " + formatItemGaji.getId()).executeUpdate();
				session.getTransaction().commit();
				copyByItemGaji(null, null, formatItemGaji, session);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:424");
		} finally {
			closeSession(session);
		}
	}

	public void copyByItemGaji(ItemGaji parent, ItemGajiPegawai newParent, FormatItemGaji toFormatItemGaji,
			Session session) {

		@SuppressWarnings("unchecked")
		List<ItemGaji> itemGajis = ConstantValues.simpleList(
				session.createCriteria(ItemGaji.class).add(Restrictions.eq("formatItemGaji", formatItemGaji))
						.add(parent == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parent)),
				ItemGaji.class);
		
		for (ItemGaji itemGaji : itemGajis) {
			ItemGajiPegawai newGaji = (ItemGajiPegawai) session.createCriteria(ItemGajiPegawai.class)
					.add(Restrictions.eq("itemGaji", itemGaji)).add(Restrictions.eq("pegawai", pegawai))
					.setMaxResults(1).uniqueResult();

			if (newGaji == null) {
				newGaji = new ItemGajiPegawai();
				newGaji.setId(null);
				newGaji.setFormatItemGaji(toFormatItemGaji);
				newGaji.setParent(newParent);
				newGaji.setIkutiItemGaji(true);
				newGaji.setItemGaji(itemGaji);
				newGaji.setPegawai(pegawai);
				
				if (!session.getTransaction().isActive()) session.getTransaction().begin();
				session.save(newGaji);
				session.getTransaction().commit();
			}

			if (getItemGajiChildCount(itemGaji, session) > 0) {
				copyByItemGaji(itemGaji, newGaji, toFormatItemGaji, session);
			}
		}
	}

	public int getItemGajiChildCount(ItemGaji parentItemGaji, Session session) {
		Integer count = ((Number) session.createCriteria(ItemGaji.class)
				.add(Restrictions.eq("formatItemGaji", formatItemGaji))
				.add(tampilkanSemua ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("aktif", true))
				.add(parentItemGaji == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parentItemGaji))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		return count;
	}

	public Double hitungItemGajiPegawai(String kodeGaji, String formula, Date date, Integer bulan, Integer tahun,
			PembayaranGajiPunyaPegawai toPembayaranGajiPunyaPegawai, List<String> penghitungan) throws Exception {
		return hitungItemGajiPegawai(kodeGaji, formula, date, bulan, tahun, null, toPembayaranGajiPunyaPegawai, penghitungan);
	}

	@SuppressWarnings("unchecked")
	public Double hitungItemGajiPegawai(String kodeGaji, String formula, Date date, Integer bulan, Integer tahun,
			Map<String, String> formulasBaruBerdasarKode, PembayaranGajiPunyaPegawai toPembayaranGajiPunyaPegawai,
			List<String> penghitungan) throws Exception {

		if (formula == null || formula.trim().equals("")) {
			return 0.0;
		}

		Session sessionA = null;
		try {
			sessionA = HibernateUtil.currentNativeSession();
			List<PenilaianKpi> penilaianKpis = ConstantValues.simpleList(
					sessionA.createCriteria(PenilaianKpi.class).createAlias("pengajuanKpi", "pengajuanKpi")
							.add(Restrictions.isNotNull("pengajuanKpi.disetujuiOleh")).addOrder(Order.asc("id"))
							.add(Restrictions.eq("pegawai", pegawai)).add(Restrictions.gt("persen", 0.1)),
					PenilaianKpi.class);

			Date d = sekarangData != null ? sekarangData : date == null ? WaktuUtil.getDate() : date;

			List<GajiTabahan> gajiTabahansLagi = ConstantValues.simpleList(sessionA.createCriteria(GajiTabahan.class)
					.setProjection(Projections.property("id"))
					.add(Restrictions.or(Restrictions.ge("cabang", pegawai.getCabang()), Restrictions.isNull("cabang")))
					.add(Restrictions.or(Restrictions.ge("departemen", pegawai.getDepartemen()), Restrictions.isNull("departemen")))
					.add(Restrictions.or(Restrictions.ge("levelJabatan", pegawai.getLevelJabatan()), Restrictions.isNull("levelJabatan")))
					.add(Restrictions.or(Restrictions.isNull("pegawai"), Restrictions.eq("pegawai", pegawai)))
					.add(Restrictions.or(
							Restrictions.sqlRestriction("date('" + Common.databaseDateFormat.get().format(d) + "') between date(mulai) and date(sampai)"),
							Restrictions.and(Restrictions.le("mulai", d), Restrictions.or(Restrictions.ge("sampai", d), Restrictions.isNull("sampai")))))
					.addOrder(Order.desc("mulai")), GajiTabahan.class, false);

			formula = " ( " + formula + " ) ";

			return hitungItemGajiPegawai(kodeGaji, gajiTabahansLagi, penilaianKpis, formula, date, bulan, tahun,
					formulasBaruBerdasarKode, toPembayaranGajiPunyaPegawai, penghitungan);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:514");
		} finally {
			closeSession(sessionA);
		}
		return 0.0;
	}

	@SuppressWarnings("unchecked")
	private Double hitungItemGajiPegawai(String kodeGaji, List<GajiTabahan> gajiTabahans,
			List<PenilaianKpi> penilaianKpis, String formula, Date date, Integer bulan, Integer tahun,
			Map<String, String> formulasBaruBerdasarKode, PembayaranGajiPunyaPegawai toPembayaranGajiPunyaPegawai,
			List<String> penghitungan) throws Exception {

		HashMap<String, Double> hashMapTransaksi = new HashMap<String, Double>();
		HashMap<String, Double> hashMapFormulaTransaksi = new HashMap<String, Double>();
		Session sessionA = null;

		try {
			sessionA = HibernateUtil.currentNativeSession();
			List<JenisTransaksiPegawai> jenisTransaksiPegawais = ConstantValues.simpleList(
					sessionA.createCriteria(TransaksiPegawai.class).add(Restrictions.eq("pegawai", pegawai))
							.setProjection(Projections.groupProperty("jenisTransaksiPegawai.id"))
							.add(Restrictions.isNull("pembayaranGajiPunyaPegawai"))
							.add(Restrictions.or(Restrictions.lt("thn", tahun), Restrictions.and(
									Restrictions.eq("thn", tahun), Restrictions.le("bln", bulan)))),
					JenisTransaksiPegawai.class, false);

			for (JenisTransaksiPegawai jenisTransaksiPegawai : jenisTransaksiPegawais) {
				Number nilai = ((Number) sessionA.createCriteria(TransaksiPegawai.class)
						.add(Restrictions.eq("jenisTransaksiPegawai", jenisTransaksiPegawai))
						.setProjection(Projections.sum("nilai")).add(Restrictions.eq("pegawai", pegawai))
						.add(Restrictions.isNull("pembayaranGajiPunyaPegawai"))
						.add(Restrictions.or(Restrictions.lt("thn", tahun), Restrictions.and(
								Restrictions.eq("thn", tahun), Restrictions.le("bln", bulan)))).uniqueResult());

				nilai = nilai == null ? 0.0 : nilai.doubleValue();
				hashMapTransaksi.put(jenisTransaksiPegawai.getKode(), nilai.doubleValue());

				if (!jenisTransaksiPegawai.getFormula().isEmpty()) {
					Double n = hashMapFormulaTransaksi.get(jenisTransaksiPegawai.getFormula());
					n = n == null ? 0.0 : n;
					hashMapFormulaTransaksi.put(jenisTransaksiPegawai.getFormula(), nilai.doubleValue() + n);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:555");
		} finally {
			closeSession(sessionA);
		}

		Collection<Konstanta> konstantas = ConstantValues.ambilBerdasarClass(Konstanta.class).values();
		return hitungItemGajiPegawai(kodeGaji, formula, date, bulan, tahun, gajiTabahans, penilaianKpis,
				hashMapTransaksi, formulasBaruBerdasarKode, konstantas, hashMapFormulaTransaksi,
				toPembayaranGajiPunyaPegawai, penghitungan);
	}

	/**
	 * Helper method untuk mengambil List Keluarga aktif sekaligus masuk ke memory cache (mencegah N+1 Queries).
	 */
	@SuppressWarnings("unchecked")
	private List<Keluarga> getCachedKeluargaAktif(Session session) {
		if (cachedKeluargaAktif == null) {
			cachedKeluargaAktif = ConstantValues.simpleList(
					session.createCriteria(Keluarga.class)
							.add(Restrictions.eq("pegawai", pegawai))
							.add(Restrictions.eq("status", true)),
					Keluarga.class);
		}
		return cachedKeluargaAktif;
	}

	private int hitungUmurKeluarga(Date tglLahir, Date tglReferensi) {
		if (tglLahir == null || tglReferensi == null) return 0;
		Calendar a = Calendar.getInstance();
		a.setTime(tglLahir);
		Calendar b = Calendar.getInstance();
		b.setTime(tglReferensi);
		int diff = b.get(Calendar.YEAR) - a.get(Calendar.YEAR);
		if (a.get(Calendar.MONTH) > b.get(Calendar.MONTH) || 
			(a.get(Calendar.MONTH) == b.get(Calendar.MONTH) && a.get(Calendar.DATE) > b.get(Calendar.DATE))) {
			diff--;
		}
		return diff;
	}

	@SuppressWarnings("unchecked")
	private Double hitungItemGajiPegawai(String kodeGaji, String formula, Date date, Integer bulan, Integer tahun,
			List<GajiTabahan> gajiTabahans, List<PenilaianKpi> penilaianKpis, HashMap<String, Double> hashMapTransaksi,
			Map<String, String> formulasBaruBerdasarKode, Collection<Konstanta> konstantas,
			HashMap<String, Double> hashMapFormulaTransaksi, PembayaranGajiPunyaPegawai toPembayaranGajiPunyaPegawai,
			List<String> penghitungan) throws Exception {

		if (formula == null || formula.trim().equals("") || formula.replaceAll("[\\p{Punct}&&[^_-]]+", "").trim().isEmpty()) {
			return 0.0;
		}

		if (kedalaman > 25) {
			try {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, penghitungan formula untuk kode \"{V1}\" dihentikan karena kedalaman rekursi melebihi batas (25). Kemungkinan ada rujukan formula yang saling melingkar (sirkular), sehingga nilai kode ini dipaksa menjadi 0. Langkah yang dapat dilakukan: (1) Periksa kembali rumus untuk kode \"{V1}\" beserta kode-kode yang dirujuknya; (2) Pastikan tidak ada rujukan yang saling melingkar; (3) Perbaiki formula lalu ulangi proses penghitungan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, kodeGaji);
			} catch (Exception ea) { ais.common.ErrorAuditUtil.record(ea, "auto-audit(empty-catch) src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java: kedalaman rekursi terlampaui"); }
			if (penghitungan != null) {
				penghitungan.add(kodeGaji + " = 0 (DIHENTIKAN: kedalaman rekursi > 25, kemungkinan rumus sirkular)");
			}
			return 0.0;
		}

		kedalaman++;
		try {

		for (GajiTabahan gajiTabahan : gajiTabahans) {
			if (gajiTabahan != null && gajiTabahan.getKode() != null
					&& gajiTabahan.getKode().trim().equalsIgnoreCase(kodeGaji.trim())
					&& !gajiTabahan.getNama().trim().equals("")) {
				formula = gajiTabahan.getNama().trim();
				break;
			}
		}

		try {
			String formulaTemp = formula.replaceAll("\\(", " ").replaceAll("\\)", " ");
			return Double.parseDouble(formulaTemp.trim());
		} catch (NumberFormatException e) {
			// Skenario WAJAR (bukan bug): ini shortcut "coba parse formula sbg
			// angka literal dulu" -- kodeGaji/formula spt "GAPOK" bukan angka
			// literal, memang HARUS gagal di sini agar lanjut ke evaluasi
			// formula di bawah (baris 625 dst). JANGAN log via ErrorAuditUtil
			// (selalu tercatat ERROR walau ini alur normal yg diharapkan).
		}
		
		formula = ItemGajiPegawaiTreeModel.fixing(formula);

		if (hashMapFormulaTransaksi != null && !hashMapFormulaTransaksi.isEmpty()) {
			for (String key : hashMapFormulaTransaksi.keySet()) {
				Double d = hashMapFormulaTransaksi.get(key);
				if (StringUtils.contains(formula, " " + key + " ")) {
					formula = StringUtils.replace(formula, " " + key + " ", " " + d + " ");
				}
				if (kodeGaji != null && kodeGaji.equalsIgnoreCase(key)) {
					return d;
				}
			}
		}

		String g = "RENC_TOT_";
		if (formula.contains(" " + g + "")) {
			Session sessionA = null;
			try {
				sessionA = HibernateUtil.currentNativeSession();
				String komponenGaji = (String) sessionA.createCriteria(RencanaGajiPunyaPegawai.class)
						.setProjection(Projections.property("komponenGaji")).createAlias("rencanaGaji", "rencanaGaji")
						.add(Restrictions.eq("pegawai", pegawai)).add(Restrictions.eq("rencanaGaji.tahun", tahun))
						.setMaxResults(1).uniqueResult();
				
				if (komponenGaji != null) {
					MyJSONObject jsonObject = new MyJSONObject(komponenGaji);
					Iterator<String> iterator = jsonObject.keys();
					while (iterator.hasNext()) {
						String key = iterator.next();
						if (key.contains(g) && formula.contains(" " + key + " ")) {
							Double jml = Double.parseDouble(jsonObject.get(key).toString());
							formula = StringUtils.replace(formula, " " + key + " ", " " + angka(jml) + " ");
						}
					}
				}
			} finally {
				closeSession(sessionA);
			}
		}

		for (Konstanta konstanta : konstantas) {
			if (konstanta.getAktif() && konstanta.getKode() != null) {
				if (StringUtils.contains(formula, " " + konstanta.getKode() + " ")) {
					formula = StringUtils.replace(formula, " " + konstanta.getKode() + " ", " " + konstanta.getKeterangan() + " ");
				}
			}
		}

		List<CommonVO> vos = pegawai.ambilDataParameterTambahan();
		for (CommonVO konstanta : vos) {
			if (konstanta.getName1() != null && konstanta.getId() != null && Common.isNumber(konstanta.getName1())) {
				ParameterTambahan parameterTambahan = (ParameterTambahan) ConstantValues
						.ambil(ParameterTambahan.class.getName(), Long.parseLong(konstanta.getId()));
				if (parameterTambahan != null) {
					if (StringUtils.contains(formula, " " + parameterTambahan.getKode() + " ")) {
						formula = StringUtils.replace(formula, " " + parameterTambahan.getKode() + " ", " " + konstanta.getName1() + " ");
					}
				}
			}
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		Date dReferensi = sekarangData != null ? sekarangData : date == null ? ais.ui.util.WaktuUtil.getDate() : date;
		calendar.setTime(dReferensi);
		int month = calendar.get(Calendar.MONTH) + 1;
		int year = calendar.get(Calendar.YEAR);

		for (GajiTabahan gajiTabahan : gajiTabahans) {
			if (gajiTabahan != null && gajiTabahan.getKode() != null && !gajiTabahan.getKode().trim().equals("")
					&& !gajiTabahan.getNama().trim().equals("")) {
				if (formula.trim().contains(" " + gajiTabahan.getKode().trim() + " ")) {
					String tempFormula = ItemGajiPegawaiTreeModel.fixing(gajiTabahan.getNama().trim());
					for (GajiTabahan gajiTabahanLagi : gajiTabahans) {
						if (gajiTabahanLagi != null && gajiTabahanLagi.getKode() != null
								&& !gajiTabahanLagi.getKode().trim().equals("")
								&& !gajiTabahanLagi.getNama().trim().equals("")) {
							if (tempFormula.trim().contains(" " + gajiTabahanLagi.getKode().trim() + " ")) {
								Double hasil = hitungItemGajiPegawai(gajiTabahanLagi.getKode(),
										" ( " + gajiTabahanLagi.getNama() + " ) ", date, bulan, tahun, gajiTabahans,
										penilaianKpis, hashMapTransaksi, formulasBaruBerdasarKode, konstantas,
										hashMapFormulaTransaksi, toPembayaranGajiPunyaPegawai, penghitungan);
								tempFormula = StringUtils.replace(tempFormula, " " + gajiTabahanLagi.getKode().trim() + " ", angka(hasil));
							}
						}
					}
					tempFormula = ItemGajiPegawaiTreeModel.fixing(tempFormula);
					formula = StringUtils.replace(formula, " " + gajiTabahan.getKode().trim() + " ", " ( " + tempFormula + " ) ");
				}
			}
		}

		for (PenilaianKpi penilaianKpi : penilaianKpis) {
			if (penilaianKpi != null && penilaianKpi.getKode() != null && !penilaianKpi.getKode().trim().equals("")) {
				formula = StringUtils.replace(formula, " " + penilaianKpi.getKode().trim() + " ", " " + penilaianKpi.getPersen() + " ");
			}
		}

		for (String kode : hashMapTransaksi.keySet()) {
			Double value = hashMapTransaksi.get(kode);
			if (kode != null && value != null) {
				formula = StringUtils.replace(formula, " " + kode.trim() + " ", " ( " + value + " ) ");
			}
		}

		if (gajiPokok != null) {
			formula = StringUtils.replace(formula, " GAPOK ", " " + angka(gajiPokok.getGaji()) + " ");
			formula = StringUtils.replace(formula, " LAIN_LAIN ", " " + angka(gajiPokok.getLain()) + " ");
		}
		if (makan != null) {
			formula = StringUtils.replace(formula, " MAKAN ", " " + angka(makan.getMakan()) + " ");
		}
		if (transport != null) {
			formula = StringUtils.replace(formula, " TRANSPORT ", " " + angka(transport.getTransport()) + " ");
		}
		if (insentif != null) {
			formula = StringUtils.replace(formula, " INSENTIF ", " " + angka(insentif.getInsentif()) + " ");
		}

		if (pegawai != null && pegawai.getGuru() != null) {
			if (formula.contains(" JML_MATPEL ") || formula.contains(" JML_JP ") || formula.contains(" JML_JDW ")) {
				Session sessionA = null;
				try {
					sessionA = HibernateUtil.currentNativeSession();
					Criterion guruOrs = Restrictions.or(Restrictions.eq("guru12", pegawai.getGuru()),
						Restrictions.or(Restrictions.eq("guru11", pegawai.getGuru()),
						Restrictions.or(Restrictions.eq("guru10", pegawai.getGuru()),
						Restrictions.or(Restrictions.eq("guru9", pegawai.getGuru()),
						Restrictions.or(Restrictions.eq("guru8", pegawai.getGuru()),
						Restrictions.or(Restrictions.eq("guru7", pegawai.getGuru()),
						Restrictions.or(Restrictions.eq("guru6", pegawai.getGuru()),
						Restrictions.or(Restrictions.eq("guru5", pegawai.getGuru()),
						Restrictions.or(Restrictions.eq("guru4", pegawai.getGuru()),
						Restrictions.or(Restrictions.eq("guru3", pegawai.getGuru()),
						Restrictions.or(Restrictions.eq("guru", pegawai.getGuru()),
						Restrictions.eq("guru2", pegawai.getGuru()))))))))))));

					List<JadwalPelajaran> jadwalPelajarans = ConstantValues.simpleList(sessionA.createCriteria(JadwalPelajaran.class)
							.add(Restrictions.eq("tahunAjaran", Common.getCurrentTahunAkademik(pegawai.getGuru().getSekolah(), dReferensi)))
							.add(Restrictions.eq("semester", Common.isNowSemensterGanjil(pegawai.getGuru().getSekolah(), dReferensi) ? 1 : 2))
							.add(guruOrs), JadwalPelajaran.class);

					double jmlJp = 0.0;
					Set<Long> jmlMatpel = new HashSet<Long>();
					Long pGuruId = pegawai.getGuru().getId();
					for (JadwalPelajaran jp : jadwalPelajarans) {
						jmlMatpel.add(jp.getMatapelajaran().getId());
						if (jp.getJamPelajaran() != null && jp.getGuru() != null && jp.getGuru().getId().equals(pGuruId)) jmlJp += jp.getJamPelajaran().getJp();
						if (jp.getJamPelajaran2() != null && jp.getGuru2() != null && jp.getGuru2().getId().equals(pGuruId)) jmlJp += jp.getJamPelajaran2().getJp();
						if (jp.getJamPelajaran3() != null && jp.getGuru3() != null && jp.getGuru3().getId().equals(pGuruId)) jmlJp += jp.getJamPelajaran3().getJp();
						if (jp.getJamPelajaran4() != null && jp.getGuru4() != null && jp.getGuru4().getId().equals(pGuruId)) jmlJp += jp.getJamPelajaran4().getJp();
						if (jp.getJamPelajaran5() != null && jp.getGuru5() != null && jp.getGuru5().getId().equals(pGuruId)) jmlJp += jp.getJamPelajaran5().getJp();
						if (jp.getJamPelajaran6() != null && jp.getGuru6() != null && jp.getGuru6().getId().equals(pGuruId)) jmlJp += jp.getJamPelajaran6().getJp();
						if (jp.getJamPelajaran7() != null && jp.getGuru7() != null && jp.getGuru7().getId().equals(pGuruId)) jmlJp += jp.getJamPelajaran7().getJp();
						if (jp.getJamPelajaran8() != null && jp.getGuru8() != null && jp.getGuru8().getId().equals(pGuruId)) jmlJp += jp.getJamPelajaran8().getJp();
						if (jp.getJamPelajaran9() != null && jp.getGuru9() != null && jp.getGuru9().getId().equals(pGuruId)) jmlJp += jp.getJamPelajaran9().getJp();
						if (jp.getJamPelajaran10() != null && jp.getGuru10() != null && jp.getGuru10().getId().equals(pGuruId)) jmlJp += jp.getJamPelajaran10().getJp();
						if (jp.getJamPelajaran11() != null && jp.getGuru11() != null && jp.getGuru11().getId().equals(pGuruId)) jmlJp += jp.getJamPelajaran11().getJp();
						if (jp.getJamPelajaran12() != null && jp.getGuru12() != null && jp.getGuru12().getId().equals(pGuruId)) jmlJp += jp.getJamPelajaran12().getJp();
					}

					if (formula.contains(" JML_MATPEL ")) formula = StringUtils.replace(formula, " JML_MATPEL ", " " + angka(jmlMatpel.size()) + " ");
					if (formula.contains(" JML_JDW ")) formula = StringUtils.replace(formula, " JML_JDW ", " " + angka(jadwalPelajarans.size()) + " ");
					if (formula.contains(" JML_JP ")) formula = StringUtils.replace(formula, " JML_JP ", " " + angka(jmlJp) + " ");
				} finally {
					closeSession(sessionA);
				}
			}
		}

		if (pegawai != null && pegawai.getDosen() != null) {
			if (formula.contains(" JML_DSN_PERK ") || formula.contains(" HR_DSN_PERK ")
					|| formula.contains(" MK_DSN_PERK ") || formula.contains(" MK_SKS_DSN_PERK ")
					|| formula.contains(" HDR_DSN_PERK ") || formula.contains(" SKS_DSN_PERK ")
					|| formula.contains(" SKS_DSN_MASING_PERK ") || formula.contains(" SKS_TOTAL_DSN_PERK ")) {

				Session sessionA = null;
				try {
					sessionA = HibernateUtil.currentNativeSession();
					KehadiranDosenBulanan kehadiran = (KehadiranDosenBulanan) sessionA
							.createCriteria(KehadiranDosenBulanan.class).add(Restrictions.eq("bulan", bulan))
							.add(Restrictions.eq("dosen", pegawai.getDosen().getId()))
							.add(Restrictions.eq("tahun", tahun)).setMaxResults(1).uniqueResult();

					if (kehadiran != null) {
						if (toPembayaranGajiPunyaPegawai != null) {
							if ((toPembayaranGajiPunyaPegawai.getMulai() == null || !Common.dateFormat8.get().format(toPembayaranGajiPunyaPegawai.getMulai()).equals(Common.dateFormat8.get().format(kehadiran.getTanggalMulai())))
									|| (toPembayaranGajiPunyaPegawai.getSampai() == null || !Common.dateFormat8.get().format(toPembayaranGajiPunyaPegawai.getSampai()).equals(Common.dateFormat8.get().format(kehadiran.getTanggalSampai())))) {
								toPembayaranGajiPunyaPegawai.setMulai(kehadiran.getTanggalMulai());
								toPembayaranGajiPunyaPegawai.setSampai(kehadiran.getTanggalSampai());
								try {
									sessionA.getTransaction().begin();
									Common.refreshUpdate(sessionA, toPembayaranGajiPunyaPegawai);
									sessionA.getTransaction().commit();
								} catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:818"); }
							}
						}

						if (formula.contains(" JML_DSN_PERK ")) formula = StringUtils.replace(formula, " JML_DSN_PERK ", " " + angka(kehadiran.getJmlKelas()) + " ");
						if (formula.contains(" HR_DSN_PERK ")) formula = StringUtils.replace(formula, " HR_DSN_PERK ", " " + angka(kehadiran.getHr()) + " ");
						if (formula.contains(" HDR_DSN_PERK ")) formula = StringUtils.replace(formula, " HDR_DSN_PERK ", " " + angka(kehadiran.getMasuk()) + " ");
						if (formula.contains(" SKS_DSN_PERK ")) formula = StringUtils.replace(formula, " SKS_DSN_PERK ", " " + angka(kehadiran.getSkspecahan()) + " ");
						if (formula.contains(" MK_DSN_PERK ")) formula = StringUtils.replace(formula, " MK_DSN_PERK ", " " + angka(kehadiran.getJmlMk()) + " ");
						if (formula.contains(" MK_SKS_DSN_PERK ")) formula = StringUtils.replace(formula, " MK_SKS_DSN_PERK ", " " + angka(kehadiran.getSkspecahanmk()) + " ");
						if (formula.contains(" SKS_TOTAL_DSN_PERK ")) formula = StringUtils.replace(formula, " SKS_TOTAL_DSN_PERK ", " " + angka(kehadiran.getSksTotal()) + " ");
					} else {
						if (formula.contains(" JML_DSN_PERK ")) formula = StringUtils.replace(formula, " JML_DSN_PERK ", " 0 ");
						if (formula.contains(" HR_DSN_PERK ")) formula = StringUtils.replace(formula, " HR_DSN_PERK ", " 0 ");
						if (formula.contains(" HDR_DSN_PERK ")) formula = StringUtils.replace(formula, " HDR_DSN_PERK ", " 0 ");
						if (formula.contains(" SKS_DSN_PERK ")) formula = StringUtils.replace(formula, " SKS_DSN_PERK ", " 0 ");
						if (formula.contains(" MK_DSN_PERK ")) formula = StringUtils.replace(formula, " MK_DSN_PERK ", " 0 ");
						if (formula.contains(" MK_SKS_DSN_PERK ")) formula = StringUtils.replace(formula, " MK_SKS_DSN_PERK ", " 0 ");
						if (formula.contains(" SKS_TOTAL_DSN_PERK ")) formula = StringUtils.replace(formula, " SKS_TOTAL_DSN_PERK ", " 0 ");
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:839");
				} finally {
					closeSession(sessionA);
				}
			}
		}

		if (pegawai.getDosen() != null && (formula.contains(" V_SUM_UTS ") || formula.contains(" V_SUM_UAS ")
				|| formula.contains(" V_SUM_JML_DOSEN_UJIAN ") || formula.contains(" V_SUM_UTS_ESSAY ")
				|| formula.contains(" V_SUM_UTS_PG ") || formula.contains(" V_SUM_UAS_ESSAY ")
				|| formula.contains(" V_SUM_UAS_PG ") || formula.contains(" V_SUM_UTS_TUGAS ")
				|| formula.contains(" V_SUM_UAS_TUGAS ") || formula.contains(" V_SUM_UTS_PER_DOSEN ")
				|| formula.contains(" V_SUM_UAS_PER_DOSEN ") || formula.contains(" V_SUM_UTS_ESSAY_PER_DOSEN ")
				|| formula.contains(" V_SUM_UTS_PG_PER_DOSEN ") || formula.contains(" V_SUM_UAS_ESSAY_PER_DOSEN ")
				|| formula.contains(" V_SUM_UAS_PG_PER_DOSEN "))) {
			
			Session sessionA = null;
			try {
				sessionA = HibernateUtil.currentNativeSession();
				Object[] data = (Object[]) sessionA.createCriteria(RekapUjianDosenBulanan.class)
						.setProjection(Projections.projectionList()
								.add(Projections.sum("uts")).add(Projections.sum("uas"))
								.add(Projections.sum("utsUjianPg")).add(Projections.sum("utsUjianEssay"))
								.add(Projections.sum("uasUjianPg")).add(Projections.sum("uasUjianEssay"))
								.add(Projections.sum("utsTugas")).add(Projections.sum("uasTugas"))
								.add(Projections.sum("jmlDosen"))
								.add(Projections.sum("utsDibagiJmlDosen")).add(Projections.sum("uasDibagiJmlDosen"))
								.add(Projections.sum("utsUjianEssayJmlDosen")).add(Projections.sum("utsUjianPgJmlDosen"))
								.add(Projections.sum("uasUjianEssayJmlDosen")).add(Projections.sum("uasUjianPgJmlDosen"))
						).add(Restrictions.eq("bulan", bulan)).add(Restrictions.eq("dosen", pegawai.getDosen().getId()))
						.add(Restrictions.eq("tahun", tahun)).uniqueResult();

				if (data != null) {
					if (formula.contains(" V_SUM_UTS ")) formula = StringUtils.replace(formula, " V_SUM_UTS ", " " + angka(data[0] == null ? 0.0 : ((Number) data[0]).doubleValue()) + " ");
					if (formula.contains(" V_SUM_UAS ")) formula = StringUtils.replace(formula, " V_SUM_UAS ", " " + angka(data[1] == null ? 0.0 : ((Number) data[1]).doubleValue()) + " ");
					if (formula.contains(" V_SUM_UTS_PG ")) formula = StringUtils.replace(formula, " V_SUM_UTS_PG ", " " + angka(data[2] == null ? 0.0 : ((Number) data[2]).doubleValue()) + " ");
					if (formula.contains(" V_SUM_UTS_ESSAY ")) formula = StringUtils.replace(formula, " V_SUM_UTS_ESSAY ", " " + angka(data[3] == null ? 0.0 : ((Number) data[3]).doubleValue()) + " ");
					if (formula.contains(" V_SUM_UAS_PG ")) formula = StringUtils.replace(formula, " V_SUM_UAS_PG ", " " + angka(data[4] == null ? 0.0 : ((Number) data[4]).doubleValue()) + " ");
					if (formula.contains(" V_SUM_UAS_ESSAY ")) formula = StringUtils.replace(formula, " V_SUM_UAS_ESSAY ", " " + angka(data[5] == null ? 0.0 : ((Number) data[5]).doubleValue()) + " ");
					if (formula.contains(" V_SUM_UTS_TUGAS ")) formula = StringUtils.replace(formula, " V_SUM_UTS_TUGAS ", " " + angka(data[6] == null ? 0.0 : ((Number) data[6]).doubleValue()) + " ");
					if (formula.contains(" V_SUM_UAS_TUGAS ")) formula = StringUtils.replace(formula, " V_SUM_UAS_TUGAS ", " " + angka(data[7] == null ? 0.0 : ((Number) data[7]).doubleValue()) + " ");
					if (formula.contains(" V_SUM_JML_DOSEN_UJIAN ")) formula = StringUtils.replace(formula, " V_SUM_JML_DOSEN_UJIAN ", " " + angka(data[8] == null ? 0.0 : ((Number) data[8]).doubleValue()) + " ");
					if (formula.contains(" V_SUM_UTS_PER_DOSEN ")) formula = StringUtils.replace(formula, " V_SUM_UTS_PER_DOSEN ", " " + angka(data[9] == null ? 0.0 : ((Number) data[9]).doubleValue()) + " ");
					if (formula.contains(" V_SUM_UAS_PER_DOSEN ")) formula = StringUtils.replace(formula, " V_SUM_UAS_PER_DOSEN ", " " + angka(data[10] == null ? 0.0 : ((Number) data[10]).doubleValue()) + " ");
					if (formula.contains(" V_SUM_UTS_ESSAY_PER_DOSEN ")) formula = StringUtils.replace(formula, " V_SUM_UTS_ESSAY_PER_DOSEN ", " " + angka(data[11] == null ? 0.0 : ((Number) data[11]).doubleValue()) + " ");
					if (formula.contains(" V_SUM_UTS_PG_PER_DOSEN ")) formula = StringUtils.replace(formula, " V_SUM_UTS_PG_PER_DOSEN ", " " + angka(data[12] == null ? 0.0 : ((Number) data[12]).doubleValue()) + " ");
					if (formula.contains(" V_SUM_UAS_ESSAY_PER_DOSEN ")) formula = StringUtils.replace(formula, " V_SUM_UAS_ESSAY_PER_DOSEN ", " " + angka(data[13] == null ? 0.0 : ((Number) data[13]).doubleValue()) + " ");
					if (formula.contains(" V_SUM_UAS_PG_PER_DOSEN ")) formula = StringUtils.replace(formula, " V_SUM_UAS_PG_PER_DOSEN ", " " + angka(data[14] == null ? 0.0 : ((Number) data[14]).doubleValue()) + " ");
				} else {
					if (formula.contains(" V_SUM_UTS ")) formula = StringUtils.replace(formula, " V_SUM_UTS ", " 0 ");
					if (formula.contains(" V_SUM_UAS ")) formula = StringUtils.replace(formula, " V_SUM_UAS ", " 0 ");
					if (formula.contains(" V_SUM_UTS_ESSAY ")) formula = StringUtils.replace(formula, " V_SUM_UTS_ESSAY ", " 0 ");
					if (formula.contains(" V_SUM_UTS_PG ")) formula = StringUtils.replace(formula, " V_SUM_UTS_PG ", " 0 ");
					if (formula.contains(" V_SUM_UAS_ESSAY ")) formula = StringUtils.replace(formula, " V_SUM_UAS_ESSAY ", " 0 ");
					if (formula.contains(" V_SUM_UAS_PG ")) formula = StringUtils.replace(formula, " V_SUM_UAS_PG ", " 0 ");
					if (formula.contains(" V_SUM_UTS_TUGAS ")) formula = StringUtils.replace(formula, " V_SUM_UTS_TUGAS ", " 0 ");
					if (formula.contains(" V_SUM_UAS_TUGAS ")) formula = StringUtils.replace(formula, " V_SUM_UAS_TUGAS ", " 0 ");
					if (formula.contains(" V_SUM_JML_DOSEN_UJIAN ")) formula = StringUtils.replace(formula, " V_SUM_JML_DOSEN_UJIAN ", " 0 ");
					if (formula.contains(" V_SUM_UTS_PER_DOSEN ")) formula = StringUtils.replace(formula, " V_SUM_UTS_PER_DOSEN ", " 0 ");
					if (formula.contains(" V_SUM_UAS_PER_DOSEN ")) formula = StringUtils.replace(formula, " V_SUM_UAS_PER_DOSEN ", " 0 ");
					if (formula.contains(" V_SUM_UTS_ESSAY_PER_DOSEN ")) formula = StringUtils.replace(formula, " V_SUM_UTS_ESSAY_PER_DOSEN ", " 0 ");
					if (formula.contains(" V_SUM_UTS_PG_PER_DOSEN ")) formula = StringUtils.replace(formula, " V_SUM_UTS_PG_PER_DOSEN ", " 0 ");
					if (formula.contains(" V_SUM_UAS_ESSAY_PER_DOSEN ")) formula = StringUtils.replace(formula, " V_SUM_UAS_ESSAY_PER_DOSEN ", " 0 ");
					if (formula.contains(" V_SUM_UAS_PG_PER_DOSEN ")) formula = StringUtils.replace(formula, " V_SUM_UAS_PG_PER_DOSEN ", " 0 ");
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:905");
			} finally {
				closeSession(sessionA);
			}
		}

		if (formula.contains(" V_AKTIF ") || formula.contains(" V_HDR ") || formula.contains(" V_THDR ")
				|| formula.contains(" V_THHDR ") || formula.contains(" V_SKT ") || formula.contains(" V_IZIN ")
				|| formula.contains(" V_ALPA ") || formula.contains(" V_BLM ") || formula.contains(" V_CUTI ")
				|| formula.contains(" V_LEM ") || formula.contains(" V_MSK_LIBUR ") || formula.contains(" V_TPT ")
				|| formula.contains(" V_TERLAMBAT ") || formula.contains(" V_CEPAT ")) {
			Session sessionA = null;
			try {
				sessionA = HibernateUtil.currentNativeSession();
				KehadiranPegawaiBulanan khd = (KehadiranPegawaiBulanan) sessionA
						.createCriteria(KehadiranPegawaiBulanan.class).add(Restrictions.eq("bulan", bulan))
						.add(Restrictions.eq("pegawai.id", pegawai.getId())).add(Restrictions.eq("tahun", tahun))
						.setMaxResults(1).uniqueResult();
				
				if (khd != null) {
					if (formula.contains(" V_MSK_LIBUR ")) formula = StringUtils.replace(formula, " V_MSK_LIBUR ", " " + angka(khd.getMasukDihariLibur()) + " ");
					if (formula.contains(" V_TPT ")) formula = StringUtils.replace(formula, " V_TPT ", " " + angka(khd.getTepatWaktu()) + " ");
					if (formula.contains(" V_TERLAMBAT ")) formula = StringUtils.replace(formula, " V_TERLAMBAT ", " " + angka(khd.getTerlambat()) + " ");
					if (formula.contains(" V_CEPAT ")) formula = StringUtils.replace(formula, " V_CEPAT ", " " + angka(khd.getPulangcepat()) + " ");
					if (formula.contains(" V_AKTIF ")) formula = StringUtils.replace(formula, " V_AKTIF ", " " + angka(khd.getAktif()) + " ");
					if (formula.contains(" V_HDR ")) formula = StringUtils.replace(formula, " V_HDR ", " " + angka(khd.getMasuk()) + " ");
					if (formula.contains(" V_THDR ")) formula = StringUtils.replace(formula, " V_THDR ", " " + angka(khd.getTidakHadir()) + " ");
					if (formula.contains(" V_THHDR ")) formula = StringUtils.replace(formula, " V_THHDR ", " " + angka(khd.getTidakHadirTanpaHoliday()) + " ");
					if (formula.contains(" V_SKT ")) formula = StringUtils.replace(formula, " V_SKT ", " " + angka(khd.getSakit()) + " ");
					if (formula.contains(" V_IZIN ")) formula = StringUtils.replace(formula, " V_IZIN ", " " + angka(khd.getIzin()) + " ");
					if (formula.contains(" V_ALPA ")) formula = StringUtils.replace(formula, " V_ALPA ", " " + angka(khd.getAlpa()) + " ");
					if (formula.contains(" V_BLM ")) formula = StringUtils.replace(formula, " V_BLM ", " " + angka(khd.getBelum()) + " ");
					if (formula.contains(" V_CUTI ")) formula = StringUtils.replace(formula, " V_CUTI ", " " + angka(khd.getCuti()) + " ");
					if (formula.contains(" V_LEM ")) formula = StringUtils.replace(formula, " V_LEM ", " " + angka(khd.getLembur()) + " ");
				} else {
					if (formula.contains(" V_MSK_LIBUR ")) formula = StringUtils.replace(formula, " V_MSK_LIBUR ", " 0 ");
					if (formula.contains(" V_TPT ")) formula = StringUtils.replace(formula, " V_TPT ", " 0 ");
					if (formula.contains(" V_TERLAMBAT ")) formula = StringUtils.replace(formula, " V_TERLAMBAT ", " 0 ");
					if (formula.contains(" V_CEPAT ")) formula = StringUtils.replace(formula, " V_CEPAT ", " 0 ");
					if (formula.contains(" V_AKTIF ")) formula = StringUtils.replace(formula, " V_AKTIF ", " 0 ");
					if (formula.contains(" V_HDR ")) formula = StringUtils.replace(formula, " V_HDR ", " 0 ");
					if (formula.contains(" V_THHDR ")) formula = StringUtils.replace(formula, " V_THHDR ", " 0 ");
					if (formula.contains(" V_THDR ")) formula = StringUtils.replace(formula, " V_THDR ", " 0 ");
					if (formula.contains(" V_SKT ")) formula = StringUtils.replace(formula, " V_SKT ", " 0 ");
					if (formula.contains(" V_IZIN ")) formula = StringUtils.replace(formula, " V_IZIN ", " 0 ");
					if (formula.contains(" V_ALPA ")) formula = StringUtils.replace(formula, " V_ALPA ", " 0 ");
					if (formula.contains(" V_BLM ")) formula = StringUtils.replace(formula, " V_BLM ", " 0 ");
					if (formula.contains(" V_CUTI ")) formula = StringUtils.replace(formula, " V_CUTI ", " 0 ");
					if (formula.contains(" V_LEM ")) formula = StringUtils.replace(formula, " V_LEM ", " 0 ");
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:956");
			} finally {
				closeSession(sessionA);
			}
		}

		for (Object o : kodeTunjangans) {
			try {
				KodeTunjangan kodeTunjangan = (KodeTunjangan) o;
				if (kodeTunjangan != null && kodeTunjangan.getAktif()) {
					if (kodeTunjangan.getJenis().equals(KodeTunjangan.JABATAN_FUNGSIONAL)) {
						String k = kodeTunjangan.getJenis() + (kodeTunjangan.getKode().isEmpty() ? "" : "_" + kodeTunjangan.getKode());
						if (jabatanFungsionals != null && formula.contains(" " + k + " ")) {
							formula = StringUtils.replace(formula, " " + k + " ", " " + (JabatanFungsional.ambilTunjangans(sekarangData != null ? sekarangData : date, kodeTunjangan.getKode(), jabatanFungsionals)) + " ");
						}
					}
					if (kodeTunjangan.getJenis().equals(KodeTunjangan.JABATAN_STRUKTURAL)) {
						String k = kodeTunjangan.getJenis() + (kodeTunjangan.getKode().isEmpty() ? "" : "_" + kodeTunjangan.getKode());
						if (jabatanStrukturals != null && formula.contains(" " + k + " ")) {
							formula = StringUtils.replace(formula, " " + k + " ", " " + (JabatanStruktural.ambilTunjangans(sekarangData != null ? sekarangData : date, kodeTunjangan.getKode(), jabatanStrukturals)) + " ");
						}
					}
					if (kodeTunjangan.getJenis().equals(KodeTunjangan.JABATAN_LAIN)) {
						String k = kodeTunjangan.getJenis() + (kodeTunjangan.getKode().isEmpty() ? "" : "_" + kodeTunjangan.getKode());
						if (jabatans != null && formula.contains(" " + k + " ")) {
							formula = StringUtils.replace(formula, " " + k + " ", " " + (Jabatan.ambilTunjangans(sekarangData != null ? sekarangData : date, kodeTunjangan.getKode(), jabatans)) + " ");
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:986");
			}
		}

		// --- OPTIMIZATION: LOAD KELUARGA ONCE IN MEMORY IF ANY KEYWORD MATCHES ---
		if (pegawai != null && pegawai.getId() != null && (formula.contains("JUMLAH_") || formula.contains("ASURANSI_"))) {
			Session sessionA = null;
			try {
				sessionA = HibernateUtil.currentNativeSession();
				List<Keluarga> listKel = getCachedKeluargaAktif(sessionA);
				
				int jmlKeluargaInti = 0, jmlAnak = 0, jmlIstri = 0, jmlSuami = 0, jmlSaudara = 0, jmlOrtu = 0, jmlMertua = 0;
				for (Keluarga k : listKel) {
					if (Keluarga.SUAMI.equals(k.getHubungan()) || Keluarga.ISTRI.equals(k.getHubungan()) || Keluarga.ANAK.equals(k.getHubungan())) {
						jmlKeluargaInti++;
					}
					if (Keluarga.ANAK.equals(k.getHubungan())) jmlAnak++;
					else if (Keluarga.ISTRI.equals(k.getHubungan())) jmlIstri++;
					else if (Keluarga.SUAMI.equals(k.getHubungan())) jmlSuami++;
					else if (Keluarga.SAUDARA.equals(k.getHubungan())) jmlSaudara++;
					else if (Keluarga.ORANG_TUA.equals(k.getHubungan())) jmlOrtu++;
					else if (Keluarga.MERTUA.equals(k.getHubungan())) jmlMertua++;
				}

				if (formula.contains(" JUMLAH_KELUARGA ")) formula = StringUtils.replace(formula, " JUMLAH_KELUARGA ", " " + angka(jmlKeluargaInti) + " ");
				if (formula.contains(" JUMLAH_ANAK ")) formula = StringUtils.replace(formula, " JUMLAH_ANAK ", " " + angka(jmlAnak) + " ");
				if (formula.contains(" JUMLAH_ISTRI ")) formula = StringUtils.replace(formula, " JUMLAH_ISTRI ", " " + angka(jmlIstri) + " ");
				if (formula.contains(" JUMLAH_SUAMI ")) formula = StringUtils.replace(formula, " JUMLAH_SUAMI ", " " + angka(jmlSuami) + " ");
				if (formula.contains(" JUMLAH_SAUDARA ")) formula = StringUtils.replace(formula, " JUMLAH_SAUDARA ", " " + angka(jmlSaudara) + " ");
				if (formula.contains(" JUMLAH_ORANG_TUA ")) formula = StringUtils.replace(formula, " JUMLAH_ORANG_TUA ", " " + angka(jmlOrtu) + " ");
				if (formula.contains(" JUMLAH_MERTUA ")) formula = StringUtils.replace(formula, " JUMLAH_MERTUA ", " " + angka(jmlMertua) + " ");

				// Hitung Umur Anak - Tanpa query ulang 60x ke database
				if (formula.contains(" JUMLAH_ANAK_UMUR_")) {
					for (int umur = 0; umur < 30; umur++) {
						String key1 = " JUMLAH_ANAK_UMUR_" + umur + " ";
						String key2 = " JUMLAH_ANAK_UMUR_MAX_" + umur + " ";
						
						if (formula.contains(key1) || formula.contains(key2)) {
							int countAnakUmur = 0;
							int countAnakMaxUmur = 0;
							
							for (Keluarga k : listKel) {
								if (Keluarga.ANAK.equals(k.getHubungan()) && k.getTanggalLahir() != null) {
									int age = hitungUmurKeluarga(k.getTanggalLahir(), dReferensi);
									if (age == umur) countAnakUmur++;
									if (age <= umur) countAnakMaxUmur++;
								}
							}
							if (formula.contains(key1)) formula = StringUtils.replace(formula, key1, " " + angka(countAnakUmur) + " ");
							if (formula.contains(key2)) formula = StringUtils.replace(formula, key2, " " + angka(countAnakMaxUmur) + " ");
						}
					}
				}

				// Asuransi Pegawai Sendiri
				List<AsuransiPegawai> asuransiPegawaisPegawa = new ArrayList<AsuransiPegawai>();
				if (pegawai.getAsuransiPegawai1() != null) asuransiPegawaisPegawa.add(pegawai.getAsuransiPegawai1());
				if (pegawai.getAsuransiPegawai2() != null) asuransiPegawaisPegawa.add(pegawai.getAsuransiPegawai2());
				if (pegawai.getAsuransiPegawai3() != null) asuransiPegawaisPegawa.add(pegawai.getAsuransiPegawai3());
				if (pegawai.getAsuransiPegawai4() != null) asuransiPegawaisPegawa.add(pegawai.getAsuransiPegawai4());

				Map<String, Double> jumlahAsuransis = new HashMap<String, Double>();
				for (AsuransiPegawai asuransiPegawai : asuransiPegawaisPegawa) {
					String keyData = " TARIF_ASURANSI_" + asuransiPegawai.getKode() + " ";
					if (formula.contains(keyData)) {
						Double a = jumlahAsuransis.get(keyData);
						if (a == null) a = 0.0;
						a += asuransiPegawai.getTarif();
						jumlahAsuransis.put(keyData, a);
					}
				}
				for (String keyData : jumlahAsuransis.keySet()) {
					formula = StringUtils.replace(formula, keyData, " " + angka(jumlahAsuransis.get(keyData)) + " ");
				}

				// Asuransi dari Koleksi Asuransi Keluarga (dari Memory Cache `listKel`)
				for (Object a : asuransiPegawais) {
					AsuransiPegawai asu = (AsuransiPegawai) a;
					Long asuId = asu.getId();
					
					String keyKel = " JUMLAH_ASURANSI_KELUARGA_" + asu.getKode() + " ";
					String keyTarifKel = " JUMLAH_TARIF_ASURANSI_KELUARGA_" + asu.getKode() + " ";
					String keyAnakAsu = " JUMLAH_ANAK_ASURANSI_" + asu.getKode() + " ";
					String keyIstriAsu = " JUMLAH_ISTRI_ASURANSI_" + asu.getKode() + " ";
					String keySuamiAsu = " JUMLAH_SUAMI_ASURANSI_" + asu.getKode() + " ";
					
					int jmlAsuKel = 0, jmlAnakAsu = 0, jmlIstriAsu = 0, jmlSuamiAsu = 0;
					double sumTarifAsuKel = 0.0;
					
					if (formula.contains(keyKel) || formula.contains(keyTarifKel) || formula.contains(keyAnakAsu) 
						|| formula.contains(keyIstriAsu) || formula.contains(keySuamiAsu)) {
						
						for (Keluarga k : listKel) {
							if (k.getAsuransiPegawai1() != null && k.getAsuransiPegawai1().getId().equals(asuId)) {
								jmlAsuKel++;
								sumTarifAsuKel += (k.getPremiAsuransi1() != null ? k.getPremiAsuransi1() : 0.0);
								if (Keluarga.ANAK.equals(k.getHubungan())) jmlAnakAsu++;
								if (Keluarga.ISTRI.equals(k.getHubungan())) jmlIstriAsu++;
								if (Keluarga.SUAMI.equals(k.getHubungan())) jmlSuamiAsu++;
							}
						}
						
						if (formula.contains(keyKel)) formula = StringUtils.replace(formula, keyKel, " " + angka(jmlAsuKel) + " ");
						if (formula.contains(keyTarifKel)) formula = StringUtils.replace(formula, keyTarifKel, " " + angka(sumTarifAsuKel) + " ");
						if (formula.contains(keyAnakAsu)) formula = StringUtils.replace(formula, keyAnakAsu, " " + angka(jmlAnakAsu) + " ");
						if (formula.contains(keyIstriAsu)) formula = StringUtils.replace(formula, keyIstriAsu, " " + angka(jmlIstriAsu) + " ");
						if (formula.contains(keySuamiAsu)) formula = StringUtils.replace(formula, keySuamiAsu, " " + angka(jmlSuamiAsu) + " ");
					}
				}
			} finally {
				closeSession(sessionA);
			}
		}

		if (formula.contains(" PERSEN_INSENTIF ")) {
			Session sessionA = null;
			try {
				sessionA = HibernateUtil.currentNativeSession();
				PenilaianKpi penilaianKpiData = PenilaianKpi.hitungKpi(sessionA, pegawai, dReferensi);
				Double persen = penilaianKpiData == null ? pegawai.getPersenKpiDefault() : penilaianKpiData.getPersen();
				formula = StringUtils.replace(formula, " PERSEN_INSENTIF ", " " + angka(persen) + " ");
			} finally {
				closeSession(sessionA);
			}
		}

		if (formula.contains(" JP ")) formula = StringUtils.replace(formula, " JP ", " " + angka(pegawai.getJpDefault()) + " ");
		if (formula.contains(" TUNJANGAN_KINERJA_KHUSUS ")) formula = StringUtils.replace(formula, " TUNJANGAN_KINERJA_KHUSUS ", " " + angka(pegawai.getTunjanganKinerja()) + " ");
		if (formula.contains(" PTKP_PEGAWAI ") && pegawai.getPtkpPegawai() != null) formula = StringUtils.replace(formula, " PTKP_PEGAWAI ", " " + angka(pegawai.getPtkpPegawai().getTarif()) + " ");
		if (formula.contains(" ASURANSI_PEGAWAI_1 ") && pegawai.getAsuransiPegawai1() != null) formula = StringUtils.replace(formula, " ASURANSI_PEGAWAI_1 ", " " + angka(pegawai.getAsuransiPegawai1().getTarif()) + " ");
		if (formula.contains(" ASURANSI_PEGAWAI_2 ") && pegawai.getAsuransiPegawai2() != null) formula = StringUtils.replace(formula, " ASURANSI_PEGAWAI_2 ", " " + angka(pegawai.getAsuransiPegawai2().getTarif()) + " ");
		if (formula.contains(" ASURANSI_PEGAWAI_3 ") && pegawai.getAsuransiPegawai3() != null) formula = StringUtils.replace(formula, " ASURANSI_PEGAWAI_3 ", " " + angka(pegawai.getAsuransiPegawai3().getTarif()) + " ");
		if (formula.contains(" ASURANSI_PEGAWAI_4 ") && pegawai.getAsuransiPegawai4() != null) formula = StringUtils.replace(formula, " ASURANSI_PEGAWAI_4 ", " " + angka(pegawai.getAsuransiPegawai4().getTarif()) + " ");

		if (formula.contains(" MASA_KERJA_THN ")) formula = StringUtils.replace(formula, " MASA_KERJA_THN ", " " + pegawai.ambilMasaKerjaTahun() + " ");
		if (formula.contains(" MASA_KERJA_BLN ")) formula = StringUtils.replace(formula, " MASA_KERJA_BLN ", " " + pegawai.ambilMasaKerjaBulan() + " ");
		if (formula.contains(" PK_THN ")) formula = StringUtils.replace(formula, " PK_THN ", " " + pegawai.ambilMasaKerjaTahunPengalamanKerja() + " ");
		if (formula.contains(" PK_BLN ")) formula = StringUtils.replace(formula, " PK_BLN ", " " + pegawai.ambilMasaKerjaBulanPengalamanKerja() + " ");
		if (formula.contains(" HONOR_THN ")) formula = StringUtils.replace(formula, " HONOR_THN ", " " + angka(pegawai.ambilMasaKerjaTahunHonorer()) + " ");
		if (formula.contains(" HONOR_BLN ")) formula = StringUtils.replace(formula, " HONOR_BLN ", " " + angka(pegawai.ambilMasaKerjaBulanHonorer()) + " ");
		if (formula.contains(" ST_THN ")) formula = StringUtils.replace(formula, " ST_THN ", " " + angka(pegawai.ambilMasaKerjaTahunSemiTetap()) + " ");
		if (formula.contains(" ST_BLN ")) formula = StringUtils.replace(formula, " ST_BLN ", " " + angka(pegawai.ambilMasaKerjaBulanSemiTetap()) + " ");

		if (formula.contains(" MK ") || formula.contains(" MK_FIX ")) {
			Double masaKerjaTahun = MasaKerjaUtil.hitung(pegawai);
			if (formula.contains(" MK ")) formula = StringUtils.replace(formula, " MK ", " " + angka(masaKerjaTahun) + " ");
			if (formula.contains(" MK_FIX ")) formula = StringUtils.replace(formula, " MK_FIX ", " " + angka(masaKerjaTahun) + " ");
		}
		if (formula.contains(" MK_BUL_FIX ")) {
			Period period = MasaKerjaUtil.masaKerja(pegawai);
			formula = StringUtils.replace(formula, " MK_BUL_FIX ", " " + period.getYears() + "" + period.getMonths() + " ");
		}

		for (String kd : dataVar.keySet()) {
			if (kd != null && StringUtils.contains(formula, " " + kd + " ")) {
				formula = StringUtils.replace(formula, " " + kd + " ", " " + angka(dataVar.get(kd)) + " ");
			}
		}
		
		formula = ItemGajiPegawaiTreeModel.fixing(formula);

		if (penghitungan != null) {
			penghitungan.add(formula);
		}

		String formulaTemp = formula.replaceAll("\\(", " ").replaceAll("\\)", " ")
				.replaceAll("\\+", " ").replaceAll("\\-", " ").replaceAll("\\*", " ")
				.replaceAll("/", " ").replaceAll("%", " ");

		formulaTemp = StringUtils.replace(formulaTemp, "!=", " ");
		formulaTemp = StringUtils.replace(formulaTemp, ">=", " ");
		formulaTemp = StringUtils.replace(formulaTemp, "<=", " ");
		formulaTemp = StringUtils.replace(formulaTemp, ">", " ");
		formulaTemp = StringUtils.replace(formulaTemp, "<", " ");

		Map<String, Serializable> itemGajiPegawaiSet = new HashMap<String, Serializable>();
		String[] splits = formulaTemp.split(" ");

		boolean adaTerlambat = false, adaLembur = false, adaCepat = false, adaJam = false;
		Session sessionA = null;
		
		try {
			sessionA = HibernateUtil.currentNativeSession();
			for (String hasil : splits) {
				if (hasil != null && !hasil.trim().isEmpty() && !hasil.trim().equalsIgnoreCase("if")
						&& !hasil.trim().equalsIgnoreCase(",") && !hasil.trim().equalsIgnoreCase("=")
						&& !hasil.equalsIgnoreCase("roundup") && !hasil.equalsIgnoreCase("rounddown")
						&& !hasil.trim().equalsIgnoreCase("avg") && !hasil.trim().equalsIgnoreCase("sum")
						&& !hasil.trim().equalsIgnoreCase("upper")) {
					
					if (!Common.isNumber(hasil)) {
						String defaultFormula = formulasBaruBerdasarKode == null ? null : formulasBaruBerdasarKode.get(hasil.trim());
						if (defaultFormula == null) {
							ItemGajiPegawai gajiPegawai = (ItemGajiPegawai) ConstantValues.simpleObject(
									sessionA.createCriteria(ItemGajiPegawai.class).add(Restrictions.eq("pegawai", pegawai))
											.add(Restrictions.eq("kode", hasil.trim()))
											.add(Restrictions.eq("formatItemGaji", formatItemGaji))
											.addOrder(Order.desc("id")).setMaxResults(1),
									ItemGajiPegawai.class);
							defaultFormula = gajiPegawai == null ? null : gajiPegawai.getDefaultFormula();
						}
						itemGajiPegawaiSet.put(hasil, defaultFormula == null ? 0.0 : defaultFormula);
					}
				}

				if (hasil != null && hasil.trim().equals(ItemGaji.V_LEM)) adaLembur = true;
				if (hasil != null && hasil.trim().equals(ItemGaji.V_CEP)) adaCepat = true;
				if (hasil != null && hasil.trim().equals(ItemGaji.V_JAM)) adaJam = true;
				if (hasil != null && hasil.trim().equals(ItemGaji.V_TERL)) adaTerlambat = true;
			}

			if (adaLembur) {
				Number jumlahLemburMasuk = ((Number) sessionA.createCriteria(StatuskehadiranKaryawanHarian.class)
						.add(Restrictions.eq("pegawai", pegawai)).add(Restrictions.eq("bulan", month))
						.add(Restrictions.eq("tahun", year)).setProjection(Projections.sum("jumlahLemburMasuk")).uniqueResult());
				itemGajiPegawaiSet.put(ItemGaji.V_LEM, jumlahLemburMasuk == null ? 0.0 : jumlahLemburMasuk.doubleValue());
			}
			if (adaCepat) {
				Number jumlahCepatKeluar = ((Number) sessionA.createCriteria(StatuskehadiranKaryawanHarian.class)
						.add(Restrictions.eq("pegawai", pegawai)).add(Restrictions.eq("bulan", month))
						.add(Restrictions.eq("tahun", year)).setProjection(Projections.sum("jumlahCepatKeluar")).uniqueResult());
				itemGajiPegawaiSet.put(ItemGaji.V_CEP, jumlahCepatKeluar == null ? 0.0 : jumlahCepatKeluar.doubleValue());
			}
			if (adaJam) {
				Number jumlahJamMasuk = ((Number) sessionA.createCriteria(StatuskehadiranKaryawanHarian.class)
						.add(Restrictions.eq("pegawai", pegawai)).add(Restrictions.eq("bulan", month))
						.add(Restrictions.eq("tahun", year)).setProjection(Projections.sum("jumlahJamMasuk")).uniqueResult());
				itemGajiPegawaiSet.put(ItemGaji.V_JAM, jumlahJamMasuk == null ? 0.0 : jumlahJamMasuk.doubleValue());
			}
			if (adaTerlambat) {
				Number jumlahTerlambat = ((Number) sessionA.createCriteria(StatuskehadiranKaryawanHarian.class)
						.add(Restrictions.eq("pegawai", pegawai)).add(Restrictions.eq("bulan", month))
						.add(Restrictions.eq("tahun", year)).setProjection(Projections.sum("jumlahTerlambat")).uniqueResult());
				itemGajiPegawaiSet.put(ItemGaji.V_TERL, jumlahTerlambat == null ? 0.0 : jumlahTerlambat.doubleValue());
			}
		} finally {
			closeSession(sessionA);
		}

		double result = 0.0;
		try {
			Expression e = new ExpressionBuilder(formula).variables(itemGajiPegawaiSet.keySet())
					.functions(LogicalUtil.ALL_FUNCTION).operator(LogicalUtil.ALL_OPERATOR).build();
			
			for (String key : itemGajiPegawaiSet.keySet()) {
				try {
					if (!key.isEmpty() && !key.equalsIgnoreCase("if") && !key.equalsIgnoreCase("avg")
							&& !key.trim().equalsIgnoreCase(",") && !key.trim().equalsIgnoreCase("=")
							&& !key.equalsIgnoreCase("roundup") && !key.equalsIgnoreCase("rounddown")
							&& !key.equalsIgnoreCase("sum") && !key.equalsIgnoreCase("upper")) {
						
						Object object = itemGajiPegawaiSet.get(key);
						Double t = 0.0;
						if (object != null && object instanceof String) {
							String fomul = (String) object;
							t = fomul == null || fomul.trim().equals("") ? 0.0
									: hitungItemGajiPegawai(key, " ( " + fomul + " ) ", date, bulan, tahun,
											gajiTabahans, penilaianKpis, hashMapTransaksi, formulasBaruBerdasarKode,
											konstantas, hashMapFormulaTransaksi, toPembayaranGajiPunyaPegawai,
											penghitungan);
						} else if (object != null && object instanceof Double) {
							t = (Double) object;
						}
						e.setVariable(key, t);
					}
				} catch (Exception ee) {
					try {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, terjadi kesalahan pada saat penghitungan formula \"{V1}\". Rincian kesalahan: {V2}. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan formula tersebut; (2) Pastikan seluruh variabel yang digunakan telah terdefinisi dengan benar; (3) Perbaiki formula lalu ulangi proses penghitungan.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, formula, ee.getMessage());
					} catch (Exception ea) { ais.common.ErrorAuditUtil.record(ea, "auto-audit(empty-catch) src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:1257");}
					ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:1258");
				}
			}

			@SuppressWarnings("unused")
			boolean valid = false;
			try {
				valid = e.validate().isValid();
			} catch (Exception ee) {
				try {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, terjadi kesalahan pada saat validasi formula \"{V1}\". Rincian kesalahan: {V2}. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan formula tersebut; (2) Pastikan tanda kurung dan operator telah lengkap serta seimbang; (3) Perbaiki formula lalu ulangi proses validasi.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, formula, ee.getMessage());
				} catch (Exception ea) { ais.common.ErrorAuditUtil.record(ea, "auto-audit(empty-catch) src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:1271");}
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:1272");
			}

			result = e.evaluate();

		} catch (Exception ee) {
			try {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, terjadi kesalahan pada saat evaluasi formula \"{V1}\". Rincian kesalahan: {V2}. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan formula tersebut; (2) Pastikan seluruh variabel dan konstanta yang digunakan bernilai valid; (3) Perbaiki formula lalu ulangi proses evaluasi.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, formula, ee.getMessage());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:1282");}
			ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/payroll/util/ItemGajiPegawaiTreeModel.java:1283");
		}

		dataVar.put(kodeGaji, result);
		return result;

		} finally {
			kedalaman--;
		}
	}

	public static String angka(double hasil) {
		String string = df.get().format(hasil);
		string = StringUtils.replace(string, ".", "");
		string = StringUtils.replace(string, ",", ".");
		return hasil < 0.0 ? "(0 " + string + ")" : string;
	}

	public static String fixing(String formula) {
		formula = " " + formula + " ";
		formula = formula.replaceAll("\\(", " ( ").replaceAll("\\)", " ) ")
				.replaceAll("\\+", " + ").replaceAll("\\-", " - ")
				.replaceAll("\\*", " * ").replaceAll("/", " / ")
				.replaceAll(",", " , ").replaceAll("\n", " ").replaceAll("%", " % ");
		
		formula = StringUtils.replace(formula, "!=", " != ");
		formula = StringUtils.replace(formula, ">=", " >= ");
		formula = StringUtils.replace(formula, "<=", " <= ");
		formula = StringUtils.replace(formula, "<", " < ");
		formula = StringUtils.replace(formula, ">", " > ");
		formula = StringUtils.replace(formula, "< =", " <= ");
		formula = StringUtils.replace(formula, "> =", " >= ");
		formula = StringUtils.replace(formula, "if ", "if");
		formula = StringUtils.replace(formula, "avg ", "avg");
		formula = StringUtils.replace(formula, "sum ", "sum");

		for (int i = 0; i < 10; i++) {
			formula = formula.replaceAll("  ", " ");
		}
		return formula;
	}
}