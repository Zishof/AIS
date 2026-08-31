package ais.action.master.payroll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiNotDefaultBanbox;
import ais.action.master.payroll.util.ItemGajiPegawaiTreeModel;
import ais.action.master.payroll.util.PembayaranItemGajiPegawaiTreeModel;
import ais.action.report.format1.payroll.LaporanSlipGajiRealPegawaiPerOrang;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.akunting.StandingInstruction;
import ais.database.model.payroll.CaraPembayaranGaji;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.GajiTabahan;
import ais.database.model.payroll.ItemGajiPegawai;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.database.model.payroll.PembayaranItemGajiPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk bayar gaji pegawai. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code AmbilDataPegawaiNotDefaultBanbox
 * searchPegawai}, {@code Tree tree}, {@code MyCheckboxConfig tampilkanRumus}, {@code MyButtonConfig save},
 * {@code Row rowButtonSave}, {@code Combobox bulan}, {@code Intbox tahun}, {@code MyDatebox tanggalBayar};
 * inisialisasi/lifecycle ({@code doAfterCompose()}, {@code initTree()}); pembacaan/pencarian ({@code
 * onReloadTree()}, {@code doLoad()}, {@code reloadTreeitem()}, {@code reloadTreeitem()}, {@code reloadTotal()});
 * operasi domain lain ({@code bayar()}, {@code openChilds()}, {@code closeChilds()}, {@code
 * indexOfContainedPunctuation()}, {@code hasSomeChilds()}). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class BayarGajiPegawaiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private AmbilDataPegawaiNotDefaultBanbox searchPegawai;
	private Tree tree;
	private MyCheckboxConfig tampilkanRumus;
	private MyButtonConfig save;
	private Row rowButtonSave;
	private Combobox bulan;
	private Intbox tahun;
	private MyDatebox tanggalBayar;
	private Combobox caraBayar;
	private Combobox pilihFormat;
	private MyLabelBolder periode;
	private Map<Long, String> formulasBaru;
	private Map<String, String> formulasBaruBerdasarKode;
	private Map<Long, Treecell> treecellHasils = new HashMap<Long, Treecell>();

	private Date mulai = null;
	private Date sampai = null;

	private PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai = null;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		for (int i = 0; i < 12; i++) {
			Comboitem comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i);
			bulan.appendChild(comboitem);
		}

		Common.selectComboItem(bulan, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH));
		if (bulan != null) { bulan.setReadonly(true); }

		if (tahun != null) { tahun.setValue(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)); }
		if (tanggalBayar != null) { tanggalBayar.setValue(ais.ui.util.WaktuUtil.getDate()); }
		if (tanggalBayar != null) { tanggalBayar.setReadonly(true); }

		if (execution.getParameter("pembayaranGajiPunyaPegawai") != null
				&& Common.isNumber(execution.getParameter("pembayaranGajiPunyaPegawai"))) {
			pembayaranGajiPunyaPegawai = (PembayaranGajiPunyaPegawai) HibernateUtil.currentSession()
					.createCriteria(PembayaranGajiPunyaPegawai.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("pembayaranGajiPunyaPegawai"))))
					.uniqueResult();

			if (pembayaranGajiPunyaPegawai != null) {
				Common.selectComboItem(bulan, pembayaranGajiPunyaPegawai.getPembayaranGaji().getBulan() - 1);
				bulan.setDisabled(true);

				tahun.setValue(pembayaranGajiPunyaPegawai.getPembayaranGaji().getTahun());
				tahun.setDisabled(true);

				Common.selectComboItem(true, caraBayar,
						pembayaranGajiPunyaPegawai.getPembayaranGaji().getCaraPembayaranGaji());
				caraBayar.setDisabled(true);

				tanggalBayar.setValue(pembayaranGajiPunyaPegawai.getTanggalBayar());
				tanggalBayar.setDisabled(true);

				searchPegawai.setValue(pembayaranGajiPunyaPegawai.getPegawai() == null ? ""
						: pembayaranGajiPunyaPegawai.getPegawai().getNama());

				searchPegawai.setAttribute("pegawai", pembayaranGajiPunyaPegawai.getPegawai());
				searchPegawai.setAttribute("myValue", pembayaranGajiPunyaPegawai.getPegawai());
				searchPegawai.setDisabled(true);

				Common.clear(pilihFormat);
				Common.insertComboItems(pilihFormat, "nama",
						pembayaranGajiPunyaPegawai.getPegawai().ambilFormatItemGajis());
				pilihFormat.setReadonly(true);
				Common.selectComboItem(true, pilihFormat, pembayaranGajiPunyaPegawai.getFormatItemGaji());
				pilihFormat.setDisabled(true);
				pilihFormat.setDisabled(pilihFormat.getChildren().size() == 1);

			}
		}

		searchPegawai.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Pegawai pegawai = (Pegawai) searchPegawai.getAttribute("pegawai");
				if (pegawai != null) {

					Common.clear(pilihFormat);
					Common.insertComboItems(pilihFormat, "nama", pegawai.ambilFormatItemGajis());
					pilihFormat.setReadonly(true);
					if (pilihFormat.getSelectedItem() == null && !pilihFormat.getChildren().isEmpty()) {
						pilihFormat.setSelectedIndex(0);
					}
					pilihFormat.setDisabled(pilihFormat.getChildren().size() == 1);

					SatuanKerja satuanKerja = pegawai.getSatuanKerja();

					CaraPembayaranGaji caraPembayaranGajiDefault = (CaraPembayaranGaji) HibernateUtil
							.currentSession().createCriteria(CaraPembayaranGaji.class)
							.add(Restrictions.eq("defaultPembayaran", true)).add(
									Restrictions.and(
											satuanKerja == null ? Restrictions.sqlRestriction("true")
													: Restrictions.or(Restrictions.isNull("satuanKerja"),
															Restrictions.eq("satuanKerja", satuanKerja)),
											Restrictions.and(Restrictions.isNotNull("akun"),
													Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))))
							.setMaxResults(1).uniqueResult();

					Common.insertCombo(caraBayar, "nama", "akun", CaraPembayaranGaji.class, Restrictions.and(
							satuanKerja == null ? Restrictions.sqlRestriction("true")
									: Restrictions.or(Restrictions.isNull("satuanKerja"),
											Restrictions.eq("satuanKerja", satuanKerja)),
							Restrictions.and(Restrictions.isNotNull("akun"),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));
					Common.selectComboItem(true, caraBayar, caraPembayaranGajiDefault);
					caraBayar.setReadonly(true);

					final Integer bln = ((Integer) bulan.getSelectedItem().getValue()) + 1;
					final Integer ta = tahun.getValue();
					if (ta != null && bln != null) {

						FormatItemGaji formatItemGaji = (FormatItemGaji) (pilihFormat.getSelectedItem() == null ? null
								: pilihFormat.getSelectedItem().getValue());
						if (formatItemGaji == null) {
							MyMessageboxConfig.show("Mohon maaf, format gaji pegawai belum ditentukan. Langkah yang dapat dilakukan: (1) pilih format gaji pegawai terlebih dahulu pada pilihan Format; (2) ulangi proses.", "Peringatan", 1,
									MyMessageboxConfig.EXCLAMATION);
							return;
						}

						Session session = HibernateUtil.currentSession();
						PembayaranGaji pembayaranGaji = (PembayaranGaji) session.createCriteria(PembayaranGaji.class)
								.add(Restrictions.eq("bulan", bln)).add(Restrictions.eq("tahun", ta)).setMaxResults(1)
								.uniqueResult();
						if (pembayaranGaji != null) {

							if (pembayaranGaji.getCaraPembayaranGaji() != null) {
								Common.selectComboItem(caraBayar, pembayaranGaji.getCaraPembayaranGaji());
							}

							pembayaranGajiPunyaPegawai = (PembayaranGajiPunyaPegawai) HibernateUtil.currentSession()
									.createCriteria(PembayaranGajiPunyaPegawai.class)
									.add(Restrictions.or(Restrictions.isNull("formatItemGaji"),
											Restrictions.eq("formatItemGaji", formatItemGaji)))
									.add(Restrictions.eq("pembayaranGaji", pembayaranGaji))
									.add(Restrictions.eq("pegawai", pegawai)).setMaxResults(1).uniqueResult();

							if (pembayaranGajiPunyaPegawai != null) {
								tanggalBayar.setValue(pembayaranGajiPunyaPegawai.getTanggalBayar());
							}
						}

					}

				}

				onReloadTree(arg0);
			}
		});

		pilihFormat.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				FormatItemGaji formatItemGaji = (FormatItemGaji) (pilihFormat.getSelectedItem() == null ? null
						: pilihFormat.getSelectedItem().getValue());
				Pegawai pegawai = (Pegawai) searchPegawai.getAttribute("pegawai");
				if (formatItemGaji == null && pegawai != null) {
					MyMessageboxConfig.show("Mohon maaf, format gaji pegawai belum ditentukan. Langkah yang dapat dilakukan: (1) pilih format gaji pegawai terlebih dahulu pada pilihan Format; (2) ulangi proses.", "Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				SatuanKerja satuanKerja = pegawai.getSatuanKerja();

				CaraPembayaranGaji caraPembayaranGajiDefault = (CaraPembayaranGaji) HibernateUtil.currentSession()
						.createCriteria(CaraPembayaranGaji.class).add(Restrictions.eq("defaultPembayaran", true))
						.add(Restrictions.and(
								satuanKerja == null ? Restrictions.sqlRestriction("true")
										: Restrictions.or(Restrictions.isNull("satuanKerja"),
												Restrictions.eq("satuanKerja", satuanKerja)),
								Restrictions.and(Restrictions.isNotNull("akun"),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))))
						.setMaxResults(1).uniqueResult();

				Common.insertCombo(caraBayar, "nama", "akun", CaraPembayaranGaji.class, Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.and(Restrictions.isNotNull("akun"),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));
				Common.selectComboItem(true, caraBayar, caraPembayaranGajiDefault);
				caraBayar.setReadonly(true);

				final Integer bln = ((Integer) bulan.getSelectedItem().getValue()) + 1;
				final Integer ta = tahun.getValue();
				if (ta != null && bln != null) {

					Session session = HibernateUtil.currentSession();
					PembayaranGaji pembayaranGaji = (PembayaranGaji) session.createCriteria(PembayaranGaji.class)
							.add(Restrictions.eq("bulan", bln)).add(Restrictions.eq("tahun", ta)).setMaxResults(1)
							.uniqueResult();
					if (pembayaranGaji != null) {

						if (pembayaranGaji.getCaraPembayaranGaji() != null) {
							Common.selectComboItem(caraBayar, pembayaranGaji.getCaraPembayaranGaji());
						}

						pembayaranGajiPunyaPegawai = (PembayaranGajiPunyaPegawai) HibernateUtil.currentSession()
								.createCriteria(PembayaranGajiPunyaPegawai.class)
								.add(Restrictions.or(Restrictions.isNull("formatItemGaji"),
										Restrictions.eq("formatItemGaji", formatItemGaji)))
								.add(Restrictions.eq("pembayaranGaji", pembayaranGaji))
								.add(Restrictions.eq("pegawai", pegawai)).setMaxResults(1).uniqueResult();

						if (pembayaranGajiPunyaPegawai != null) {
							tanggalBayar.setValue(pembayaranGajiPunyaPegawai.getTanggalBayar());
						}
					}

				}

				onReloadTree(arg0);
			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTree(null);
			}
		});

		save.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				CaraPembayaranGaji caraPembayaranGaji = (CaraPembayaranGaji) (caraBayar.getSelectedItem() == null ? null
						: caraBayar.getSelectedItem().getValue());
				if (caraPembayaranGaji == null) {
					MyMessageboxConfig.show("Mohon maaf, cara pembayaran belum dipilih. Langkah yang dapat dilakukan: (1) pilih salah satu cara pembayaran; (2) ulangi penyimpanan.", "Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				Pegawai pegawai = (Pegawai) searchPegawai.getAttribute("pegawai");
				if (pegawai == null) {
					MyMessageboxConfig.show("Mohon maaf, pegawai belum dipilih. Langkah yang dapat dilakukan: (1) pilih salah satu pegawai; (2) ulangi penyimpanan.", "Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				FormatItemGaji formatItemGaji = (FormatItemGaji) (pilihFormat.getSelectedItem() == null ? null
						: pilihFormat.getSelectedItem().getValue());
				if (formatItemGaji == null) {
					MyMessageboxConfig.show("Mohon maaf, format gaji pegawai belum ditentukan. Langkah yang dapat dilakukan: (1) pilih format gaji pegawai terlebih dahulu pada pilihan Format; (2) ulangi proses.", "Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Integer bln = ((Integer) bulan.getSelectedItem().getValue()) + 1;
				Integer ta = tahun.getValue();
				if (ta != null && bln != null) {
					BayarGajiPegawaiAction.bayar(pegawai, bln, ta, caraPembayaranGaji, formatItemGaji,
							tanggalBayar.getValue(), formulasBaru, true);
				}
			}
		});
	}

	public static void bayar(final Pegawai pegawai, final Integer bln, final Integer ta,
			final CaraPembayaranGaji caraPembayaranGaji, final FormatItemGaji formatItemGaji, final Date tanggalBayar,
			Map<Long, String> formulasBaru, boolean cetak) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		PembayaranGaji pembayaranGaji = (PembayaranGaji) ConstantValues.simpleObject(
				session.createCriteria(PembayaranGaji.class).add(Restrictions.eq("bulan", bln))
						.add(Restrictions.eq("tahun", ta))
						.add(Restrictions.eq("caraPembayaranGaji", caraPembayaranGaji)).setMaxResults(1),
				PembayaranGaji.class);

		if (pembayaranGaji == null) {
			pembayaranGaji = new PembayaranGaji();
			pembayaranGaji.setBulan(bln);
			pembayaranGaji.setTahun(ta);
			pembayaranGaji.setWaktuBayar(tanggalBayar);
			pembayaranGaji.setCaraPembayaranGaji(caraPembayaranGaji);
			pembayaranGaji.setKeterangan("Pembayaran gaji per bulan " + bln + " tahun " + ta + " dengan cara "
					+ caraPembayaranGaji.getNama());

			session.getTransaction().begin();
			session.save(pembayaranGaji);
			session.getTransaction().commit();
		}

		StandingInstruction.simpanPembayaranGajiPunyaPegawai(pembayaranGaji);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, bln);
		calendar.set(Calendar.YEAR, ta);

		PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai = (PembayaranGajiPunyaPegawai) session
				.createCriteria(PembayaranGajiPunyaPegawai.class).add(Restrictions.eq("pembayaranGaji", pembayaranGaji))
				.add(Restrictions.or(Restrictions.isNull("formatItemGaji"),
						Restrictions.eq("formatItemGaji", formatItemGaji)))
				.add(Restrictions.eq("pegawai", pegawai)).setMaxResults(1).uniqueResult();

		if (pembayaranGajiPunyaPegawai == null) {
			pembayaranGajiPunyaPegawai = new PembayaranGajiPunyaPegawai();
			pembayaranGajiPunyaPegawai.setFormatItemGaji(formatItemGaji);
			pembayaranGajiPunyaPegawai.setPegawai(pegawai);
			pembayaranGajiPunyaPegawai.setKeterangan("");
			pembayaranGajiPunyaPegawai.setPembayaranGaji(pembayaranGaji);
			pembayaranGajiPunyaPegawai.setTanggalBayar(tanggalBayar);

			session.getTransaction().begin();
			session.save(pembayaranGajiPunyaPegawai);
			session.getTransaction().commit();
		}

		if (pembayaranGajiPunyaPegawai.getId() != null && pembayaranGajiPunyaPegawai.getFormatItemGaji() == null) {
			pembayaranGajiPunyaPegawai.setFormatItemGaji(formatItemGaji);
			session.getTransaction().begin();
			session.update(pembayaranGajiPunyaPegawai);
			session.getTransaction().commit();
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		PembayaranItemGajiPegawaiTreeModel pembayaranItemGajiPegawaiTreeModel = new PembayaranItemGajiPegawaiTreeModel(
				false, pembayaranGajiPunyaPegawai);
		pembayaranItemGajiPegawaiTreeModel.reset(calendar.getTime(), formulasBaru, bln, ta);
		pembayaranItemGajiPegawaiTreeModel.setLunas(calendar.getTime());

		PembayaranGaji.hitungUlang(pembayaranGaji);

		if (cetak) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					LaporanSlipGajiRealPegawaiPerOrang laporan = new LaporanSlipGajiRealPegawaiPerOrang(pegawai,
							formatItemGaji, bln, ta);
					laporan.setTitle("Cetak Slip Gaji Pegawai");
					laporan.setClosable(true);
					laporan.setHeight("97%");
					laporan.setWidth("97%");
					laporan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					laporan.onModal();
				}
			});
		}
	}

	private void initTree(Tree tree) throws Exception {

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Item Gaji");
		treecol.setParent(treecols);
		if (tampilkanRumus.isChecked()) {
			treecol = new Treecol("Formula");
			treecol.setWidth("30%");
			treecol.setParent(treecols);

			treecol = new Treecol("Perhitungan");
			treecol.setWidth("30%");
			treecol.setParent(treecols);
		}

		treecol = new Treecol("Hasil Perhitungan");
		treecol.setWidth("15%");
		treecol.setParent(treecols);

		treecols.setParent(tree);
	}

	@SuppressWarnings("unchecked")
	public void onReloadTree(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			private void load(FormatItemGaji formatItemGaji, Pegawai pegawai, Tree tree) {

				Integer bln = ((Integer) bulan.getSelectedItem().getValue()) + 1;
				Integer ta = tahun.getValue();

				Session session = HibernateUtil.currentSession();
				List<Object[]> dataFormula = session.createCriteria(PembayaranItemGajiPegawai.class)
						.add(Restrictions.eq("formatItemGaji", formatItemGaji))
						.add(Restrictions.isNotNull("itemGajiPegawai")).add(Restrictions.isNotNull("defaultFormula"))
						.add(Restrictions.ne("defaultFormula", "")).createAlias("itemGajiPegawai", "itemGajiPegawai")
						.add(Restrictions.isNotNull("itemGajiPegawai.itemGaji"))
						.setProjection(
								Projections.projectionList().add(Projections.property("itemGajiPegawai.itemGaji.id"))
										.add(Projections.property("defaultFormula"))
										.add(Projections.property("itemGajiPegawai.kode")))

						.add(Restrictions.eq("pegawai", pegawai))
						.createAlias("pembayaranGajiPunyaPegawai", "pembayaranGajiPunyaPegawai")
						.add(Restrictions.gt("pembayaranGajiPunyaPegawai.nilai", 0.1))
						.createAlias("pembayaranGajiPunyaPegawai.pembayaranGaji", "pembayaranGaji")
						.add(Restrictions.eq("pembayaranGaji.bulan", bln))
						.add(Restrictions.eq("pembayaranGaji.tahun", ta)).list();

				if (dataFormula.isEmpty()) {
					doLoad(formatItemGaji, pegawai, tree);
				} else {
					for (Object[] d : dataFormula) {
						try {
							Number id = (Number) d[0];
							String formula = (String) d[1];
							String kode = (String) d[2];
							formulasBaru.put(id.longValue(), formula);
							if (kode != null) {
								formulasBaruBerdasarKode.put(kode, formula);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/BayarGajiPegawaiAction.java:488");
							// TODO: handle exception
						}
					}
					doLoad(formatItemGaji, pegawai, tree);

				}
			}

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (rowButtonSave != null) {
					rowButtonSave.setVisible(false);
				}
				Common.clear(tree);
				initTree(tree);

				Pegawai pegawai = (Pegawai) searchPegawai.getAttribute("pegawai");
				if (pegawai == null) {
					return;
				}
				FormatItemGaji formatItemGaji = (FormatItemGaji) (pilihFormat.getSelectedItem() == null ? null
						: pilihFormat.getSelectedItem().getValue());
				if (formatItemGaji == null) {
					return;
				}

				Integer bln = ((Integer) bulan.getSelectedItem().getValue()) + 1;
				Integer ta = tahun.getValue();

				mulai = PembayaranGajiPunyaPegawai.ambilMulai(ta, bln - 1 + (Integer.parseInt(
						Common.getKonfigurasi("plus_minus_penambahan_bulan_penggajian", "0").getNilai().trim())));
				sampai = PembayaranGajiPunyaPegawai.ambilSampai(ta, bln - 0 + (Integer.parseInt(
						Common.getKonfigurasi("plus_minus_penambahan_bulan_penggajian", "0").getNilai().trim())));

				periode.setValue(Common.dateFormat11.get().format(mulai) + " sd " + Common.dateFormat11.get().format(sampai));

				formulasBaru = new HashMap<Long, String>();
				formulasBaruBerdasarKode = new HashMap<String, String>();

				load(formatItemGaji, pegawai, tree);

			}
		});
	}

	private void doLoad(FormatItemGaji formatItemGaji, Pegawai pegawai, Tree tree) {
		if (rowButtonSave != null) {
			rowButtonSave.setVisible(true);
		}

		final ItemGajiPegawaiTreeModel itemGajiPegawaiTreeModel = new ItemGajiPegawaiTreeModel(true, formatItemGaji,
				pegawai, mulai);

		itemGajiPegawaiTreeModel.checkExistingItemGaji();

		tree.setModel(itemGajiPegawaiTreeModel);
		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				ItemGajiPegawai itemGajiPegawai = (ItemGajiPegawai) arg1;

				try {
					Common.clear(treeitem);
					Treerow treerow = new Treerow();
					treerow.setParent(treeitem);

					hasSomeChilds(treerow, itemGajiPegawai, itemGajiPegawaiTreeModel);

					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					Hbox toolbar = new Hbox();

					Toolbarbutton button = new MyToolbarbuttonConfig("", "/img/svg/refresh-cw.svg");
					button.setTooltiptext("Refresh");
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							reloadTreeitem(treeitem, true, false);
						}
					});
					button.setParent(toolbar);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});
	}

	private void reloadTreeitem(final Treeitem treeitem, final Boolean reloadTotal, final Boolean loadParent) {
		reloadTreeitem(treeitem, reloadTotal, loadParent, null);
	}

	private void reloadTreeitem(final Treeitem treeitem, final Boolean reloadTotal, final Boolean loadParent,
			final EventListener eventListener) {
		final Treeitem treeitemParent = loadParent ? treeitem.getParentItem() : treeitem;
		if (treeitemParent == null) {
			try {
				onReloadTree(null);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		} else {
			treeitemParent.unload();
			final Timer timer = new Timer(300);
			timer.setParent(page.getFirstRoot());
			timer.addEventListener("onTimer", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event arg0) throws Exception {
					treeitemParent.setOpen(true);
					treeitem.setOpen(true);
					if (reloadTotal) {
						reloadTotal();
					}
					if (eventListener != null) {
						eventListener.onEvent(null);
					}
					timer.detach();
				}

			});

			timer.start();
		}
	}

	private void reloadTotal() {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings({ "unchecked" })
	public void openChilds(final Treeitem treeitemParent, int max, int index) {
		if (max > index) {
			treeitemParent.setOpen(true);
			List<Treeitem> treeitems = treeitemParent.getChildren();
			for (Object object : treeitems) {

				if (object instanceof Treechildren) {
					Treechildren treechildren = (Treechildren) object;
					List<Treeitem> mytreeitems = treechildren.getChildren();
					for (Treeitem treeitem : mytreeitems) {
						openChilds(treeitem, max, (++index));
					}

				}
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	public void closeChilds(final Treeitem treeitemParent) {
		treeitemParent.setOpen(false);
		List<Treeitem> treeitems = treeitemParent.getChildren();
		for (Object object : treeitems) {

			if (object instanceof Treechildren) {
				Treechildren treechildren = (Treechildren) object;
				List<Treeitem> mytreeitems = treechildren.getChildren();
				for (Treeitem treeitem : mytreeitems) {
					closeChilds(treeitem);
				}

			}
		}
	}

	List<Character> punctuation = Arrays.asList('\'', '.', ':', ',', ')', '(', ']', '[', '+', '-', '*', '-', '/');

	public int indexOfContainedPunctuation(String input) {
		if (punctuation.contains(input.charAt(0))) {
			return 0;
		} else if (punctuation.contains(input.charAt(input.length() - 1))) {
			return input.length() - 1;
		}
		return -1;
	}

	private void hasSomeChilds(Treerow treerow, final ItemGajiPegawai itemGajiPegawai,
			final ItemGajiPegawaiTreeModel itemGajiPegawaiTreeModel) throws Exception {

		if (itemGajiPegawai == null) {
			treerow.setVisible(false);
			return;
		}

		Treecell treecell = new Treecell(itemGajiPegawai.toString());
		treecell.setTooltiptext(itemGajiPegawai.toString());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);
		final Date date = tanggalBayar.getValue();
		final Integer bln = ((Integer) bulan.getSelectedItem().getValue()) + 1;
		final Integer ta = tahun.getValue();

		final Treecell treecellPengitungan = new Treecell();

		List<String> penghitungan = new ArrayList<String>();
		Double hasil = itemGajiPegawaiTreeModel.hitungItemGajiPegawai(itemGajiPegawai.getKode(),
				itemGajiPegawai.getDefaultFormula(), date, bln, ta, pembayaranGajiPunyaPegawai, penghitungan);

		Common.clear(treecellPengitungan);
		Vbox vbox = new Vbox();
		for (String s : penghitungan) {
			vbox.appendChild(new MyLabelAgakKecil(s));
		}
		treecellPengitungan.appendChild(vbox);

		if (itemGajiPegawai != null && itemGajiPegawai.getItemGaji() != null
				&& itemGajiPegawai.getItemGaji().getJadikan0JikaMinus() && hasil < 0.0) {
			hasil = 0.0;
		}

		if (pembayaranGajiPunyaPegawai != null && pembayaranGajiPunyaPegawai.getId() != null
				&& itemGajiPegawai.getFinalGaji()) {

//			System.out.println("1. final itemGajiPegawai -> " + itemGajiPegawai + ", hasil -> " + hasil);

			if (pembayaranGajiPunyaPegawai.getNilaiFinal() == null || (hasil != null && hasil > 0.1
					&& hasil.intValue() != pembayaranGajiPunyaPegawai.getNilaiFinal().intValue())) {

//				System.out.println("2. final itemGajiPegawai -> " + itemGajiPegawai + ", hasil -> " + hasil);

				pembayaranGajiPunyaPegawai.setNilaiFinal(hasil);
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, pembayaranGajiPunyaPegawai);
				session.getTransaction().commit();
				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}
				HibernateUtil.closeSession();
			}
		}

		final Treecell treecellHasil = new Treecell(Common.numberFormat.get().format(hasil));
		treecellHasil.setAttribute("itemGajiPegawai", itemGajiPegawai);

		if (!itemGajiPegawai.getNilaiVariableBisaDiubah() || itemGajiPegawai.getDefaultFormula().isEmpty()
				|| itemGajiPegawai.getDefaultFormula() == null) {

			if (tampilkanRumus.isChecked()) {
				treecell = new Treecell(itemGajiPegawai.getDefaultFormula());
				treecell.setTooltiptext(itemGajiPegawai.getDefaultFormula());
				treecell.setStyle("font-size:xx-small;text-align: left;");
				treecell.setParent(treerow);
			}
		} else {

			Session session = HibernateUtil.currentSession();

			if (tampilkanRumus.isChecked()) {
				treecell = new Treecell();
				treecell.setTooltiptext(itemGajiPegawai.getDefaultFormula());
				treecell.setStyle("font-size:x-small;text-align: left;");
				treecell.setParent(treerow);
			}
			Hbox hbox = new Hbox();
			String formulaTersimpan = formulasBaru.get(itemGajiPegawai.getItemGaji().getId());

			List<String> susunans = new ArrayList<String>();
			if (formulaTersimpan != null && !formulaTersimpan.trim().isEmpty()) {
				for (String s : formulaTersimpan.trim().split(" ")) {
					susunans.add(s);
				}

				penghitungan = new ArrayList<String>();
				hasil = itemGajiPegawaiTreeModel.hitungItemGajiPegawai(itemGajiPegawai.getKode(), formulaTersimpan,
						date, bln, ta, pembayaranGajiPunyaPegawai, penghitungan);
				Common.clear(treecellPengitungan);
				vbox = new Vbox();
				for (String s : penghitungan) {
					vbox.appendChild(new MyLabelAgakKecil(s));
				}
				treecellPengitungan.appendChild(vbox);
				treecellHasil.setLabel(Common.numberFormat.get().format(hasil));
			}
			int index = 0;
			for (final String s : itemGajiPegawai.getDefaultFormula().trim().split(" ")) {
				String nilaiTersimpan = susunans.size() > index ? susunans.get(index) : null;

				index++;

				if (Common.isNumber(s) || indexOfContainedPunctuation(s) != -1) {
					hbox.appendChild(new MyLabelAgakKecil(s));
				} else {

					GajiTabahan gajiTabahans = (GajiTabahan) ConstantValues.simpleObject(session
							.createCriteria(GajiTabahan.class)

							.add(Restrictions.or(Restrictions.isNull("pegawai"),
									Restrictions.eq("pegawai", itemGajiPegawai.getPegawai())))

							.add(Restrictions.or(Restrictions.ge("cabang", itemGajiPegawai.getPegawai().getCabang()),
									Restrictions.isNull("cabang")))
							.add(Restrictions.or(
									Restrictions.ge("departemen", itemGajiPegawai.getPegawai().getDepartemen()),
									Restrictions.isNull("departemen")))
							.add(Restrictions.or(
									Restrictions.ge("levelJabatan", itemGajiPegawai.getPegawai().getLevelJabatan()),
									Restrictions.isNull("levelJabatan")))

							.add(Restrictions.ilike("kode", s, MatchMode.EXACT)).add(Restrictions.le("mulai", date))
							.add(Restrictions.or(Restrictions.ge("sampai", date), Restrictions.isNull("sampai")))
							.addOrder(Order.desc("mulai")).setMaxResults(1), GajiTabahan.class);

					Double sumNilai = null;
					if (nilaiTersimpan != null && Common.isNumber(nilaiTersimpan)) {
						sumNilai = Double.parseDouble(nilaiTersimpan.trim());
					} else if (gajiTabahans != null && gajiTabahans.getNama() != null
							&& !gajiTabahans.getNama().trim().isEmpty()
							&& !Common.isNumber(gajiTabahans.getNama().trim())) {
						penghitungan = new ArrayList<String>();
						sumNilai = itemGajiPegawaiTreeModel.hitungItemGajiPegawai(gajiTabahans.getKode(),
								gajiTabahans.getNama().trim(), date, bln, ta, pembayaranGajiPunyaPegawai, penghitungan);
						Common.clear(treecellPengitungan);
						vbox = new Vbox();
						for (String ss : penghitungan) {
							vbox.appendChild(new MyLabelAgakKecil(ss));
						}
						treecellPengitungan.appendChild(vbox);
						treecellHasil.setLabel(Common.numberFormat.get().format(hasil));
					} else if (gajiTabahans != null && gajiTabahans.getNama() != null
							&& Common.isNumber(gajiTabahans.getNama().trim())) {
						sumNilai = Double.parseDouble(gajiTabahans.getNama().trim());
					}

					MyLabelKecil lbl = new MyLabelKecil(s);
					lbl.setStyle("color:blue;font-size:x-small;text-align: left;");
					hbox.appendChild(lbl);
					final MyDoublebox nilai = new MyDoublebox(sumNilai);
					nilai.setCols(8);
					nilai.setParent(hbox);
					nilai.setStyle("font-size:xx-small;text-align: right;");
					nilai.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							String kode = itemGajiPegawai.getKode();
							Double value = nilai.getValue();
							String formulaTersimpanLokal = formulasBaruBerdasarKode.get(kode);

							List<String> asli = new ArrayList<String>();
							for (String sLokal : itemGajiPegawai.getDefaultFormula().trim().split(" ")) {
								asli.add(sLokal);
							}
							Map<String, String> susunansLokal = new HashMap<String, String>();
							if (formulaTersimpanLokal != null && !formulaTersimpanLokal.trim().isEmpty()) {
								Integer i = 0;
								for (String sAja : formulaTersimpanLokal.trim().split(" ")) {
									try {
										if (s.equals(asli.get(i))) {
											sAja = value + "";
										}
										susunansLokal.put(asli.get(i), sAja);
									} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									i++;
								}
							}

							String formula = "";

							for (String sLokal : itemGajiPegawai.getDefaultFormula().trim().split(" ")) {
								if (susunansLokal.containsKey(sLokal)) {
									formula += formula.isEmpty() ? susunansLokal.get(sLokal) + ""
											: " " + susunansLokal.get(sLokal);
								} else {
									formula += formula.isEmpty() ? sLokal : " " + sLokal;
								}
							}

							if (value != null) {
								formula = org.apache.commons.lang3.StringUtils.replace(formula, s.trim(), value + "");
							}
							formulasBaru.put(itemGajiPegawai.getItemGaji().getId(), formula);
							formulasBaruBerdasarKode.put(kode, formula);

							List<String> penghitungan = new ArrayList<String>();
							Double hasil = itemGajiPegawaiTreeModel.hitungItemGajiPegawai(itemGajiPegawai.getKode(),
									formula, date, bln, ta, formulasBaruBerdasarKode, pembayaranGajiPunyaPegawai,
									penghitungan);
							Common.clear(treecellPengitungan);
							Vbox vbox = new Vbox();
							for (String ss : penghitungan) {
								vbox.appendChild(new MyLabelAgakKecil(ss));
							}
							treecellPengitungan.appendChild(vbox);

							if (itemGajiPegawai != null && itemGajiPegawai.getItemGaji() != null
									&& itemGajiPegawai.getItemGaji().getJadikan0JikaMinus() && hasil < 0.0) {
								hasil = 0.0;
							}

							treecellHasil.setLabel(Common.numberFormat.get().format(hasil));

							Common.createDefaultTimerNoBusy(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									for (Treecell tc : treecellHasils.values()) {
										ItemGajiPegawai itemGajiPegawai = (ItemGajiPegawai) tc
												.getAttribute("itemGajiPegawai");
										String kode = itemGajiPegawai.getKode();
										String formula = formulasBaruBerdasarKode.get(kode);

										if (formula == null) {
											formula = itemGajiPegawai.getDefaultFormula();
										}
										List<String> penghitungan = new ArrayList<String>();
										Double hasil = itemGajiPegawaiTreeModel.hitungItemGajiPegawai(kode, formula,
												date, bln, ta, formulasBaruBerdasarKode, pembayaranGajiPunyaPegawai,
												penghitungan);
										Common.clear(treecellPengitungan);
										Vbox vbox = new Vbox();
										for (String ss : penghitungan) {
											vbox.appendChild(new MyLabelAgakKecil(ss));
										}
										treecellPengitungan.appendChild(vbox);
										tc.setLabel(Common.numberFormat.get().format(hasil));
										tc.setTooltiptext(Common.numberFormat.get().format(hasil));
									}

								}
							}, "", false, 500);
						}
					});
				}
			}
			hbox.setParent(treecell);
		}

		if (tampilkanRumus.isChecked()) {
			treecellPengitungan.setStyle("font-size:small;text-align: right;");
			treecellPengitungan.setParent(treerow);
		}

		treecellHasil.setTooltiptext(Common.numberFormat.get().format(hasil));
		treecellHasil.setStyle("font-size:small;text-align: right;");
		treecellHasil.setParent(treerow);
		this.treecellHasils.put(itemGajiPegawai.getId(), treecellHasil);
	}

}
