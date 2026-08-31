package ais.action.master.payroll.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataPegawaiBanyak;
import ais.action.master.payroll.BayarGajiPegawaiAction;
import ais.action.master.payroll.PembayaranGajiAction;
import ais.action.master.payroll.util.PembayaranItemGajiPegawaiTreeModel;
import ais.action.report.format1.payroll.LaporanSlipGajiRealPegawaiPerOrang;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Bank;
import ais.database.model.Jabatan;
import ais.database.model.Pegawai;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Helper terfokus untuk pembayaran gaji punya pegawai. Tipe ini membungkus satu variasi kecil dari
 * alur yang lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan
 * implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PembayaranGaji pembayaranGaji}, {@code
 * Paging paging}, {@code MyGrid grid}, {@code boolean persetujuan}, {@code Collection pangkats}, {@code Date
 * sekarang}, {@code List gajiPunyaPegawais}, {@code MyTextbox nama}; inisialisasi/lifecycle ({@code initRow()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code loadData()}, {@code reloadData()}); validasi/perhitungan
 * ({@code hitungUlangSemuaPegawai()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class PembayaranGajiPunyaPegawaiHelper {

	private PembayaranGaji pembayaranGaji;
	private Paging paging;
	private MyGrid grid;
	private boolean persetujuan = false;
	@SuppressWarnings("rawtypes")
	Collection pangkats = ConstantValues.ambilBerdasarClass(KenaikanPangkat.class).values();

	public PembayaranGajiPunyaPegawaiHelper(MyGrid grid, boolean persetujuan) {
		super();
		this.grid = grid;
		this.persetujuan = persetujuan;
	}

	private Date sekarang = WaktuUtil.getDate();

	private List<PembayaranGajiPunyaPegawai> gajiPunyaPegawais = new ArrayList<PembayaranGajiPunyaPegawai>();

	public void initRow(final Row row, final PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai) throws Exception {

		if (!gajiPunyaPegawais.contains(pembayaranGajiPunyaPegawai)) {
			gajiPunyaPegawais.add(pembayaranGajiPunyaPegawai);
		}

		final Pegawai pegawai = pembayaranGajiPunyaPegawai.getPegawai();
		row.setValign("top");
		row.setAttribute("pembayaranGajiPunyaPegawai", pembayaranGajiPunyaPegawai);

		Vbox a;
		(a = RevisiHelper.createNewRevisi(PembayaranGajiPunyaPegawai.class, pembayaranGajiPunyaPegawai,
				pegawai.getNama())).setParent(row);

		new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(a);

		a = new Vbox();
		a.setParent(row);
		Bank bank = pegawai.ambilBank(pembayaranGajiPunyaPegawai.getFormatItemGaji());
		String noRek = pegawai.ambilNoRek(pembayaranGajiPunyaPegawai.getFormatItemGaji());
		new Label(bank == null ? "" : bank.getNama()).setParent(a);
		new Label(noRek).setParent(a);

		List<KenaikanPangkat> kenaikanPangkats = pegawai.ambilKenaikanPangkat(sekarang, pangkats);
		JabatanFungsional jabatanFungsional = pegawai.ambilJabatanFungsional(kenaikanPangkats);
		JabatanStruktural jabatanStruktural = pegawai.ambilJabatanStruktural(kenaikanPangkats);
		Jabatan jabatan = pegawai.ambilJabatan(kenaikanPangkats);

		new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">"
				+ (jabatanFungsional == null ? "" : jabatanFungsional.getNama() + "<br>")
				+ (jabatanStruktural == null ? "" : jabatanStruktural.getNama() + "<br>")
				+ (jabatan == null ? "" : jabatan.getNama()) + "</font>").setParent(row);
		kenaikanPangkats = null;

		Vbox vbox = new Vbox();
		vbox.setParent(row);

		if (pegawai.getTanggalMulaiPengalanKerja() != null) {
			new Label("Pengalaman Kerja : " + pegawai.ambilMasaKerjaTahunPengalamanKerja() + " thn, "
					+ pegawai.ambilMasaKerjaBulanPengalamanKerja() + " bln").setParent(vbox);
		}
		if (pegawai.getTanggalmasukHonorer() != null) {
			new Label("Honor : " + pegawai.ambilMasaKerjaTahunHonorer() + " thn, "
					+ pegawai.ambilMasaKerjaBulanHonorer() + " bln").setParent(vbox);
		}
		if (pegawai.getTanggalmasukSemiTetap() != null) {
			new Label("Semi Tetap : " + pegawai.ambilMasaKerjaTahunSemiTetap() + " thn, "
					+ pegawai.ambilMasaKerjaBulanSemiTetap() + " bln").setParent(vbox);
		}
		if (pegawai.getTanggalmasuk() != null) {
			new Label("Tetap : " + pegawai.ambilMasaKerjaTahun() + " thn, " + pegawai.ambilMasaKerjaBulan() + " bln")
					.setParent(vbox);
		}

		new Label(pembayaranGajiPunyaPegawai.getFormatItemGaji() == null ? ""
				: pembayaranGajiPunyaPegawai.getFormatItemGaji().getNama()).setParent(vbox);

		new Label(Common.numberFormat.get().format(pembayaranGajiPunyaPegawai.getNilai())).setParent(row);

		Date d = pembayaranGajiPunyaPegawai.getMulai();
		Date s = pembayaranGajiPunyaPegawai.getSampai();

		new Label(d == null || s == null ? "" : (Common.dateFormat1.get().format(d) + " s.d " + Common.dateFormat1.get().format(s)))
				.setParent(row);

		final MyTextbox keterangan = new MyTextbox(
				pembayaranGajiPunyaPegawai.getKeterangan() == null ? "" : pembayaranGajiPunyaPegawai.getKeterangan());
		keterangan.setWidth("90%");
		if (persetujuan) {
			new MyLabelKecil(pembayaranGajiPunyaPegawai.getKeterangan()).setParent(row);
		} else {
			keterangan.setParent(row);
		}
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				pembayaranGajiPunyaPegawai.setKeterangan(keterangan.getValue());

				if (pembayaranGaji.getId() != null) {
					Common.refreshSaveOrUpdate(pembayaranGajiPunyaPegawai);
				}
				row.setValign("top");
				row.setAttribute("pembayaranGajiPunyaPegawai", pembayaranGajiPunyaPegawai);
			}
		});

		if (pembayaranGaji.getId() != null) {
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			Toolbarbutton button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Cetak Slip Gaji Pegawai");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					LaporanSlipGajiRealPegawaiPerOrang laporan = new LaporanSlipGajiRealPegawaiPerOrang(pegawai,
							pembayaranGajiPunyaPegawai.getFormatItemGaji(),
							pembayaranGajiPunyaPegawai.getPembayaranGaji().getBulan(),
							pembayaranGajiPunyaPegawai.getPembayaranGaji().getTahun());
					laporan.setTitle("Cetak Slip Gaji Pegawai");
					laporan.setClosable(true);
					laporan.setHeight("97%");
					laporan.setWidth("97%");
					laporan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					laporan.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/process.svg");
			button.setTooltiptext("Hitung Ulang");
			button.setVisible(pembayaranGaji.getId() != null);
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					final Label label = Common.displayLoadBar(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							PembayaranGaji.hitungUlang(pembayaranGaji);

							loadData(null);
						}
					});

					final List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = initCriteria(true).list();

					new Thread(new Runnable() {

						@Override
						public void run() {

							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.MONTH, pembayaranGaji.getBulan() - 1);
							calendar.set(Calendar.YEAR, pembayaranGaji.getTahun());
							int size = pembayaranGajiPunyaPegawais.size();
							int index = 0;
							for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {

								index++;

								if (pembayaranGajiPunyaPegawai.getPegawai() != null
										&& pembayaranGajiPunyaPegawai.getFormatItemGaji() != null) {

									label.setValue("Memproses data rencana gaji "
											+ pembayaranGajiPunyaPegawai.getPegawai().getNama() + " ("
											+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

									PembayaranItemGajiPegawaiTreeModel pembayaranItemGajiPegawaiTreeModel = new PembayaranItemGajiPegawaiTreeModel(
											false, pembayaranGajiPunyaPegawai);
									try {
										pembayaranItemGajiPegawaiTreeModel.reset(calendar.getTime(), null,
												pembayaranGaji.getBulan(), pembayaranGaji.getTahun());
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/helper/PembayaranGajiPunyaPegawaiHelper.java:246");
									}
									pembayaranItemGajiPegawaiTreeModel.setLunas(calendar.getTime());
								}
							}
							pembayaranGajiPunyaPegawais.clear();
							label.setValue("");
						}
					}).start();

				}

			});
			aksiButtons.add(button);

			if (pembayaranGaji.getDisetujuiOleh() == null) {
				button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.setVisible(pembayaranGajiPunyaPegawai.getPostingHistory() == null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show(
								"Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diketahui bahwa data yang telah dihapus tidak dapat dikembalikan lagi.",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												gajiPunyaPegawais.remove(pembayaranGajiPunyaPegawai);

												Session session = HibernateUtil.currentSession();
												Common.refreshDelete(session, pembayaranGajiPunyaPegawai);
												session.flush();

												PembayaranGaji.hitungUlang(pembayaranGaji);

												loadData(null);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(MyMessageboxConfig.format(
														"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang masih terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) ulangi kembali proses penghapusan.",
														e.getMessage()));
											}

										}

									}
								});

					}

				});
				aksiButtons.add(button);

			}

			button = new MyToolbarbuttonConfig("", "/img/svg/money-bills.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Bayar Gaji");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					if (pegawai.getFormatItemGaji() == null) {

						MyMessageboxConfig.show(
								"Mohon maaf, format gaji untuk pegawai ini belum ditentukan sehingga proses tidak dapat dilanjutkan. Langkah yang dapat dilakukan: (1) buka menu Data Pegawai; (2) tentukan Format Item Gaji yang sesuai untuk pegawai ini; (3) simpan perubahan, lalu ulangi kembali proses ini.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					Common.displayWindow(
							"/pages/master/payroll/bayar_gaji_pegawai_data.zul?pembayaranGajiPunyaPegawai="
									+ pembayaranGajiPunyaPegawai.getId(),
							true, "95%", Common.isMobile() ? "100%" : "95%", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(null);
								}
							});

				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Bayar Gaji");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					if (pegawai.getFormatItemGaji() == null) {

						MyMessageboxConfig.show(
								"Mohon maaf, format gaji untuk pegawai ini belum ditentukan sehingga proses tidak dapat dilanjutkan. Langkah yang dapat dilakukan: (1) buka menu Data Pegawai; (2) tentukan Format Item Gaji yang sesuai untuk pegawai ini; (3) simpan perubahan, lalu ulangi kembali proses ini.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					HashMap<Long, String> formulasBaru = new HashMap<Long, String>();
					BayarGajiPegawaiAction.bayar(pegawai, pembayaranGajiPunyaPegawai.getPembayaranGaji().getBulan(),
							pembayaranGajiPunyaPegawai.getPembayaranGaji().getTahun(),
							pembayaranGajiPunyaPegawai.getPembayaranGaji().getCaraPembayaranGaji(),
							pembayaranGajiPunyaPegawai.getFormatItemGaji(),
							pembayaranGajiPunyaPegawai.getPembayaranGaji().getWaktuBayar(), formulasBaru, true);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(null);
						}
					});

				}

			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(row, 3, aksiButtons);
		} else {
			new Label().setParent(row);
		}
	}

	private MyTextbox nama;

	private Criteria initCriteria(boolean order) {

		Criterion critNama = Restrictions.sqlRestriction("false");
		if (!nama.getValue().trim().equals("")) {
			critNama = Restrictions.or(critNama,
					Restrictions.ilike("pegawai.mycode", nama.getValue().trim(), MatchMode.ANYWHERE));
			critNama = Restrictions.or(critNama,
					Restrictions.ilike("pegawai.code", nama.getValue().trim(), MatchMode.ANYWHERE));
			critNama = Restrictions.or(critNama,
					Restrictions.ilike("pegawai.nama", nama.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critNama = Restrictions.sqlRestriction("true");
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PembayaranGajiPunyaPegawai.class).createAlias("pegawai", "pegawai")
				.add(critNama)
				.add(pembayaranGaji == null || pembayaranGaji.getId() == null ? Restrictions.sqlRestriction("false")
						: Restrictions.eq("pembayaranGaji", pembayaranGaji));
		if (order)
			criteria.addOrder(Order.asc("pegawai.nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = pembayaranGaji == null
				|| pembayaranGaji.getId() == null
						? new ArrayList<PembayaranGajiPunyaPegawai>()
						: initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
								.setFirstResult(
										Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
								.list();

		Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
		rows.setParent(grid);
		Common.clear(rows);
		for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			try {
				initRow(row, pembayaranGajiPunyaPegawai);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/helper/PembayaranGajiPunyaPegawaiHelper.java:434");
			}
		}

	}

	public void reloadData(Combobox bulanByr, Intbox tahun) {

		Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
		rows.setParent(grid);
		Common.clear(rows);
		for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : gajiPunyaPegawais) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			try {

				pembayaranGajiPunyaPegawai.getPembayaranGaji()
						.setBulan((Integer) bulanByr.getSelectedItem().getValue());
				pembayaranGajiPunyaPegawai.getPembayaranGaji().setTahun(tahun.getValue());

				pembayaranGajiPunyaPegawai.setMulai(null);
				pembayaranGajiPunyaPegawai.setSampai(null);

				initRow(row, pembayaranGajiPunyaPegawai);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/helper/PembayaranGajiPunyaPegawaiHelper.java:460");
			}
		}

	}

	private List<Pegawai> pegawaisBaru = new ArrayList<Pegawai>();

	public Groupbox display(final PembayaranGaji pembayaranGaji, final Combobox bulanByr, final Intbox tahun) {
		this.pembayaranGaji = pembayaranGaji;
		boolean persetujuan = pembayaranGaji.getDisetujuiOleh() != null;
		Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();

		bulanByr.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pembayaranGaji.setBulan((Integer) bulanByr.getSelectedItem().getValue());
				pembayaranGaji.setTahun(tahun.getValue());
				reloadData(bulanByr, tahun);
			}
		});

		tahun.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pembayaranGaji.setBulan((Integer) bulanByr.getSelectedItem().getValue());
				pembayaranGaji.setTahun(tahun.getValue());
				reloadData(bulanByr, tahun);
			}
		});

		groupbox.appendChild(new MyCaptionStyled("Daftar Gaji Pegawai"));

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(groupbox);
		if (!persetujuan) {
			Toolbarbutton button = new MyToolbarbuttonConfig("Ambil Data Pegawai", "/img/add_item.png");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					Session session = HibernateUtil.currentSession();

					if (pembayaranGaji.getId() == null) {

						pembayaranGaji.setBulan((Integer) bulanByr.getSelectedItem().getValue());
						pembayaranGaji.setTahun(tahun.getValue());

						AmbilDataPegawaiBanyak ambilDataPegawaiBanyak = new AmbilDataPegawaiBanyak(pegawaisBaru);
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
								.appendChild(ambilDataPegawaiBanyak);
						ambilDataPegawaiBanyak.setEventListener(new EventListener() {

							@Override
							public void onEvent(final Event arg0) throws Exception {

								Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
								rows.setParent(grid);

								pegawaisBaru = (List<Pegawai>) arg0.getData();
								for (Pegawai pegawai : pegawaisBaru) {

									for (FormatItemGaji formatItemGaji : pegawai.ambilFormatItemGajis()) {
										PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai = new PembayaranGajiPunyaPegawai();
										pembayaranGajiPunyaPegawai.setPegawai(pegawai);
										pembayaranGajiPunyaPegawai.setFormatItemGaji(formatItemGaji);
										pembayaranGajiPunyaPegawai.setKeterangan("");
										pembayaranGajiPunyaPegawai.setPembayaranGaji(pembayaranGaji);

										Row row = new Row();
										row.setValign("top");
										row.setParent(rows);
										try {
											initRow(row, pembayaranGajiPunyaPegawai);
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/helper/PembayaranGajiPunyaPegawaiHelper.java:539");
										}
									}
								}

							}
						});
						ambilDataPegawaiBanyak.setWidth("90%");
						ambilDataPegawaiBanyak.setHeight("97%");
						ambilDataPegawaiBanyak.setVisible(true);
						ambilDataPegawaiBanyak.onModal();

					} else {

						List<Pegawai> pegawais = ConstantValues.simpleList(
								session.createCriteria(PembayaranGajiPunyaPegawai.class)
										.setProjection(Projections.groupProperty("pegawai.id"))
										.createAlias("pembayaranGaji", "pembayaranGaji")
										.add(Restrictions.eq("pembayaranGaji.bulan", pembayaranGaji.getBulan()))
										.add(Restrictions.eq("pembayaranGaji.tahun", pembayaranGaji.getTahun())),
								Pegawai.class, false);

						AmbilDataPegawaiBanyak ambilDataPegawaiBanyak = new AmbilDataPegawaiBanyak(pegawais,
								pembayaranGaji.getSatuanKerja());
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
								.appendChild(ambilDataPegawaiBanyak);
						ambilDataPegawaiBanyak.setEventListener(new EventListener() {

							@Override
							public void onEvent(final Event arg0) throws Exception {

								final Label label = Common.displayLoadBar(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										PembayaranGaji.hitungUlang(pembayaranGaji);

										loadData(null);
									}
								});

								new Thread(new Runnable() {

									@Override
									public void run() {
										try {

										Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
										calendar.set(Calendar.MONTH, pembayaranGaji.getBulan() - 1);
										calendar.set(Calendar.YEAR, pembayaranGaji.getTahun());

										List<Pegawai> pegawais = (List<Pegawai>) arg0.getData();
										int size = pegawais.size();
										int index = 0;
										for (Pegawai pegawai : pegawais) {
											index++;
											label.setValue("Memproses data rencana gaji " + pegawai.getNama() + " ("
													+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");
											for (FormatItemGaji formatItemGaji : pegawai.ambilFormatItemGajis()) {
												Session session = HibernateUtil.currentNativeSession();

												PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai = (PembayaranGajiPunyaPegawai) session
														.createCriteria(PembayaranGajiPunyaPegawai.class)
														.add(Restrictions.or(Restrictions.isNull("formatItemGaji"),
																Restrictions.eq("formatItemGaji", formatItemGaji)))
														.add(Restrictions.eq("pembayaranGaji", pembayaranGaji))
														.add(Restrictions.eq("pegawai", pegawai)).setMaxResults(1)
														.uniqueResult();

												if (pembayaranGajiPunyaPegawai == null) {
													pembayaranGajiPunyaPegawai = new PembayaranGajiPunyaPegawai();
													pembayaranGajiPunyaPegawai.setPegawai(pegawai);
													pembayaranGajiPunyaPegawai.setFormatItemGaji(formatItemGaji);
													pembayaranGajiPunyaPegawai.setKeterangan("");
													pembayaranGajiPunyaPegawai.setPembayaranGaji(pembayaranGaji);

													session.getTransaction().begin();
													session.save(pembayaranGajiPunyaPegawai);
													session.getTransaction().commit();

												}
												// session.disconnect();
												if (session.isOpen()) {session.disconnect();session.close();}
												HibernateUtil.closeSession();

												if (pembayaranGajiPunyaPegawai.getPegawai() != null
														&& formatItemGaji != null) {
													PembayaranItemGajiPegawaiTreeModel pembayaranItemGajiPegawaiTreeModel = new PembayaranItemGajiPegawaiTreeModel(
															false, pembayaranGajiPunyaPegawai);
													try {
														pembayaranItemGajiPegawaiTreeModel.reset(calendar.getTime(),
																null, pembayaranGaji.getBulan(),
																pembayaranGaji.getTahun());
													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/helper/PembayaranGajiPunyaPegawaiHelper.java:633");
													}
													pembayaranItemGajiPegawaiTreeModel.setLunas(calendar.getTime());
												}
											}

										}

										label.setValue("");
																			} finally {
											ais.database.hibernate.HibernateUtil.closeSession();
										}
									}
								}).start();

							}
						});
						ambilDataPegawaiBanyak.setWidth("90%");
						ambilDataPegawaiBanyak.setHeight("97%");
						ambilDataPegawaiBanyak.setVisible(true);
						ambilDataPegawaiBanyak.onModal();
					}
				}

			});
			button.setParent(toolbar);
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hitung Ulang", "/img/add_item.png");
		button.setVisible(pembayaranGaji.getId() != null);
		button.setAttribute("janganDisabled", true);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// HITUNG ULANG benar: recompute nilai TIAP pegawai (bukan sekadar menjumlah), lalu jumlahkan.
						hitungUlangSemuaPegawai(pembayaranGaji);

						loadData(null);
					}
				});

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {

						Criterion critNama = Restrictions.sqlRestriction("false");
						if (!nama.getValue().trim().equals("")) {
							critNama = Restrictions.or(critNama,
									Restrictions.ilike("pegawai.mycode", nama.getValue().trim(), MatchMode.ANYWHERE));
							critNama = Restrictions.or(critNama,
									Restrictions.ilike("pegawai.code", nama.getValue().trim(), MatchMode.ANYWHERE));
							critNama = Restrictions.or(critNama,
									Restrictions.ilike("pegawai.nama", nama.getValue().trim(), MatchMode.ANYWHERE));
						} else {
							critNama = Restrictions.sqlRestriction("true");
						}

						Session session = HibernateUtil.currentNativeSession();
						Criteria criteria = session.createCriteria(PembayaranGajiPunyaPegawai.class)
								.createAlias("pegawai", "pegawai").add(critNama)
								.add(Restrictions.eq("pembayaranGaji", pembayaranGaji));

						List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = criteria.list();

						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
						HibernateUtil.closeSession();

						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.set(Calendar.MONTH, pembayaranGaji.getBulan() - 1);
						calendar.set(Calendar.YEAR, pembayaranGaji.getTahun());
						int size = pembayaranGajiPunyaPegawais.size();
						int index = 0;
						for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {

							index++;

							if (pembayaranGajiPunyaPegawai.getPegawai() != null
									&& pembayaranGajiPunyaPegawai.getFormatItemGaji() != null) {

								label.setValue("Memproses data rencana gaji "
										+ pembayaranGajiPunyaPegawai.getPegawai().getNama() + " ("
										+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

								PembayaranItemGajiPegawaiTreeModel pembayaranItemGajiPegawaiTreeModel = new PembayaranItemGajiPegawaiTreeModel(
										false, pembayaranGajiPunyaPegawai);
								try {
									pembayaranItemGajiPegawaiTreeModel.reset(calendar.getTime(), null,
											pembayaranGaji.getBulan(), pembayaranGaji.getTahun());
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/helper/PembayaranGajiPunyaPegawaiHelper.java:732");
								}
								pembayaranItemGajiPegawaiTreeModel.setLunas(calendar.getTime());
							}
						}

						label.setValue("");
											} finally {
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
		button.setTooltiptext("Cetak");
		button.setVisible(pembayaranGaji.getId() != null);
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings({})
			@Override
			public void onEvent(Event event) throws Exception {
				PembayaranGajiAction.cetak(pembayaranGaji, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(null);
					}
				});
			}
		});
		button.setParent(toolbar);

		button.setAttribute("janganDisabled", true);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama:")));
		toolbar.appendChild(nama = new MyTextbox());
		nama.setWidth("80px");
		nama.setAttribute("janganDisabled", true);
		nama.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		Toolbarbutton search;
		toolbar.appendChild(search = new MyToolbarbuttonConfig("", "/img/svg/search.svg"));
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
		search.setAttribute("janganDisabled", true);
		if (!persetujuan) {
			Toolbarbutton delete;
			toolbar.appendChild(delete = new MyToolbarbuttonConfig("", "/img/svg/trash.svg"));
			delete.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus SELURUH data ini? Perlu diketahui bahwa seluruh data yang telah dihapus tidak dapat dikembalikan lagi.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();
											session.createSQLQuery(
													"delete from payroll.pembayaran_gaji_punya_pegawai where pembayaran_gaji = "
															+ pembayaranGaji.getId())
													.executeUpdate();

											PembayaranGaji.hitungUlang(pembayaranGaji);

											loadData(null);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(MyMessageboxConfig.format(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang masih terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) ulangi kembali proses penghapusan.",
													e.getMessage()));
										}

									}

								}
							});

				}
			});
		}
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Bank");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jabatan");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Masa Kerja");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setAlign("right");
		column.setWidth(pembayaranGaji.getId() != null ? "10%" : "0px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Tanggal Perhitungan");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ket.");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("12%");

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
		paging.setParent(groupbox);

		loadData(null);

		return groupbox;
	}

	/**
	 * Menghitung ULANG nilai gaji SETIAP pegawai pada satu {@link PembayaranGaji}, lalu
	 * menjumlahkannya — bukan sekadar menjumlah nilai lama.
	 *
	 * <p><b>Latar belakang masalah.</b> Tombol "Hitung Ulang" sebelumnya hanya memanggil
	 * {@link PembayaranGaji#hitungUlang(PembayaranGaji)} yang MENJUMLAHKAN
	 * ({@code Projections.sum("nilai")}) nilai per pegawai yang SUDAH TERSIMPAN. Padahal nilai
	 * per pegawai ({@code PembayaranGajiPunyaPegawai.nilai}) baru terisi ketika rincian dibuka
	 * (lewat {@link PembayaranItemGajiPegawaiTreeModel#reset}). Akibatnya pegawai yang rinciannya
	 * belum pernah dibuka bernilai 0, sehingga daftar &amp; totalnya 0 meski rincian menampilkan
	 * nominal yang benar.</p>
	 *
	 * <p><b>Cara kerja.</b> Untuk tiap pegawai pada pembayaran gaji ini dijalankan
	 * {@code reset(...)} (mengisi {@code nilai} sesuai rincian) lalu {@code setLunas(...)} —
	 * sama persis dengan yang terjadi saat membuka rincian — kemudian total keseluruhan
	 * dijumlahkan dengan {@code PembayaranGaji.hitungUlang(...)}. Kegagalan pada satu pegawai
	 * dicatat dan TIDAK menghentikan proses (tidak mematikan aplikasi).</p>
	 *
	 * @param pembayaranGaji pembayaran gaji yang seluruh pegawainya akan dihitung ulang
	 */
	@SuppressWarnings("unchecked")
	public static void hitungUlangSemuaPegawai(PembayaranGaji pembayaranGaji) {
		if (pembayaranGaji == null || pembayaranGaji.getId() == null) {
			return;
		}

		// Ambil daftar id pegawai (id saja, ringan) pada sesi tersendiri yang ditutup di finally.
		java.util.List<Long> ids = new java.util.ArrayList<Long>();
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			java.util.List<Long> hasil = session.createCriteria(PembayaranGajiPunyaPegawai.class)
					.setProjection(Projections.id()).add(Restrictions.eq("pembayaranGaji", pembayaranGaji)).list();
			for (java.util.Iterator<Long> it = hasil.iterator(); it.hasNext();) {
				Long id = it.next();
				if (id != null) {
					ids.add(id);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try {
					session.clear();
					session.disconnect();
					session.close();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/PembayaranGajiPunyaPegawaiHelper.java:944");
				}
			}
		}

		// Tanggal perhitungan mengikuti periode (bulan/tahun) pembayaran gaji, sama seperti
		// yang dipakai saat membuka rincian per pegawai.
		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, pembayaranGaji.getBulan() - 1);
		calendar.set(Calendar.YEAR, pembayaranGaji.getTahun());
		java.util.Date tanggal = calendar.getTime();

		for (int i = 0; i < ids.size(); i++) {
			Long id = ids.get(i);
			try {
				PembayaranGajiPunyaPegawai ppg = (PembayaranGajiPunyaPegawai) ais.database.model.GeneralValueObject
						.ambilData(PembayaranGajiPunyaPegawai.class, id.toString());
				if (ppg != null && ppg.getPegawai() != null && ppg.getFormatItemGaji() != null) {
					PembayaranItemGajiPegawaiTreeModel tm = new PembayaranItemGajiPegawaiTreeModel(false, ppg);
					tm.reset(tanggal, null, pembayaranGaji.getBulan(), pembayaranGaji.getTahun());
					tm.setLunas(tanggal);
				}
			} catch (Exception e) {
				// Jangan hentikan seluruh proses karena satu pegawai bermasalah; catat & lanjut.
				Common.tampilErrorJikaAdmin(e);
			}
		}

		// Terakhir jumlahkan total keseluruhan (kini tiap nilai per pegawai sudah terisi).
		PembayaranGaji.hitungUlang(pembayaranGaji);
	}

}
