package ais.action.master.payroll.detail;

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
import ais.ui.util.MyDetail;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataPegawaiBanyak;
import ais.action.master.payroll.BayarGajiPegawaiAction;
import ais.action.master.payroll.util.PembayaranItemGajiPegawaiTreeModel;
import ais.action.report.format1.payroll.LaporanSlipGajiRealPegawaiPerOrang;
import ais.common.Common;
import ais.common.CommonMedia;
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

public class PembayaranGajiPunyaPegawaiAction extends MyDetail {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private PembayaranGaji pembayaranGaji;
	private Paging paging;
	private MyGrid grid;

	private boolean persetujuan = false;

	public PembayaranGajiPunyaPegawaiAction(PembayaranGaji pembayaranGaji, boolean persetujuan) {
		super();
		this.persetujuan = persetujuan;
		this.pembayaranGaji = pembayaranGaji;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PembayaranGajiPunyaPegawaiAction.this);
				if (isOpen()) {
					display();
				}
			}
		});

	}

	class PembayaranGajiPunyaPegawaiRenderer extends ais.ui.util.MyRowRenderer {
		Date sekarang = WaktuUtil.getDate();
		@SuppressWarnings("rawtypes")
		Collection pangkats = ConstantValues.ambilBerdasarClass(KenaikanPangkat.class).values();

		public PembayaranGajiPunyaPegawaiRenderer() {

		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			final PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai = (PembayaranGajiPunyaPegawai) data;
			final Pegawai pegawai = pembayaranGajiPunyaPegawai.getPegawai();

			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);
			Vbox a;
			(a = RevisiHelper.createNewRevisi(PembayaranGajiPunyaPegawai.class, pembayaranGajiPunyaPegawai,
					pegawai.getNama())).setParent(arg0);

			new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(a);
			
			a = new Vbox();
			a.setParent(arg0);
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
					+ (jabatan == null ? "" : jabatan.getNama()) + "</font>").setParent(arg0);
			kenaikanPangkats = null;

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

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
				new Label(
						"Tetap : " + pegawai.ambilMasaKerjaTahun() + " thn, " + pegawai.ambilMasaKerjaBulan() + " bln")
						.setParent(vbox);
			}

			new Label(pembayaranGajiPunyaPegawai.getFormatItemGaji() == null ? ""
					: pembayaranGajiPunyaPegawai.getFormatItemGaji().getNama()).setParent(vbox);

			new Label(Common.numberFormat.get().format(pembayaranGajiPunyaPegawai.getNilai())).setParent(arg0);

			Date d = pembayaranGajiPunyaPegawai.getMulai();
			Date s = pembayaranGajiPunyaPegawai.getSampai();

			new Label(d == null || s == null ? ""
					: (Common.dateFormat1.get().format(d) + " s.d " + Common.dateFormat1.get().format(s))).setParent(arg0);

			final MyTextbox keterangan = new MyTextbox(pembayaranGajiPunyaPegawai.getKeterangan() == null ? ""
					: pembayaranGajiPunyaPegawai.getKeterangan());
			keterangan.setWidth("90%");
			if (persetujuan) {
				new MyLabelKecil(pembayaranGajiPunyaPegawai.getKeterangan()).setParent(arg0);
			} else {
				keterangan.setParent(arg0);
			}
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					pembayaranGajiPunyaPegawai.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (pembayaranGajiPunyaPegawai));
				}
			});

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dikumpulkan lalu dibungkus
			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
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

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(pembayaranGajiPunyaPegawai.getPostingHistory() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan. Tekan OK untuk melanjutkan penghapusan, atau Batal untuk membatalkan.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Session session = HibernateUtil.currentSession();
											Common.refreshDelete(session, pembayaranGajiPunyaPegawai);
											session.flush();

											PembayaranGaji.hitungUlang(pembayaranGaji);

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(MyMessageboxConfig.format(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan terlebih dahulu data lain yang berelasi; (2) periksa kembali keterkaitan data; (3) hubungi administrator apabila kendala masih berlanjut.",
													e.getMessage()));
										}

									}

								}
							});

				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/money-bills.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Bayar Gaji");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					if (pegawai.getFormatItemGaji() == null) {

						MyMessageboxConfig.show("Mohon maaf, format gaji pegawai ini belum ditentukan sehingga proses tidak dapat dilanjutkan. Langkah yang dapat dilakukan: (1) tentukan Format Item Gaji untuk pegawai bersangkutan terlebih dahulu melalui menu Item Gaji Pegawai; (2) pastikan format telah tersimpan; (3) ulangi kembali proses ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
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

						MyMessageboxConfig.show("Mohon maaf, format gaji pegawai ini belum ditentukan sehingga proses tidak dapat dilanjutkan. Langkah yang dapat dilakukan: (1) tentukan Format Item Gaji untuk pegawai bersangkutan terlebih dahulu melalui menu Item Gaji Pegawai; (2) pastikan format telah tersimpan; (3) ulangi kembali proses ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
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

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}
	}

	private MyTextbox kode;
	private MyTextbox nama;

	private Criteria initCriteria(boolean order) {

		Criterion critKode = Restrictions.sqlRestriction("false");
		if (!kode.getValue().trim().equals("")) {
			critKode = Restrictions.or(critKode,
					Restrictions.ilike("pegawai.mycode", kode.getValue().trim(), MatchMode.ANYWHERE));
			critKode = Restrictions.or(critKode,
					Restrictions.ilike("pegawai.code", kode.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critKode = Restrictions.sqlRestriction("true");
		}

		Criterion critNama = Restrictions.sqlRestriction("false");
		if (!nama.getValue().trim().equals("")) {
			critNama = Restrictions.or(critNama,
					Restrictions.ilike("pegawai.nama", nama.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critNama = Restrictions.sqlRestriction("true");
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PembayaranGajiPunyaPegawai.class).createAlias("pegawai", "pegawai")
				.add(critKode).add(critNama)

				.add(Restrictions.eq("pembayaranGaji", pembayaranGaji));
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

		ListModel strset = new SimpleListModel(pembayaranGajiPunyaPegawais);
		grid.setRowRenderer(new PembayaranGajiPunyaPegawaiRenderer());
		grid.setModelCheckMobile(strset);
		grid.renderAll();
	}

	private void display() {
		boolean persetujuan = pembayaranGaji.getDisetujuiOleh() != null;
		Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar Pegawai"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		if (persetujuan) {
			Toolbarbutton button = new MyToolbarbuttonConfig("Ambil Data Pegawai", "/img/add_item.png");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					Session session = HibernateUtil.currentSession();

					List<Pegawai> pegawais = session.createCriteria(PembayaranGajiPunyaPegawai.class)
							.setProjection(Projections.groupProperty("pegawai"))
							.createAlias("pembayaranGaji", "pembayaranGaji")
							.add(Restrictions.eq("pembayaranGaji.bulan", pembayaranGaji.getBulan()))
							.add(Restrictions.eq("pembayaranGaji.tahun", pembayaranGaji.getTahun())).list();

					AmbilDataPegawaiBanyak ambilDataPegawaiBanyak = new AmbilDataPegawaiBanyak(pegawais,
							pembayaranGaji.getSatuanKerja());
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataPegawaiBanyak);
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

											if (pembayaranGajiPunyaPegawai.getId() != null
													&& pembayaranGajiPunyaPegawai.getFormatItemGaji() == null) {
												pembayaranGajiPunyaPegawai.setFormatItemGaji(formatItemGaji);
												session.getTransaction().begin();
												session.update(pembayaranGajiPunyaPegawai);
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
													pembayaranItemGajiPegawaiTreeModel.reset(calendar.getTime(), null,
															pembayaranGaji.getBulan(), pembayaranGaji.getTahun());
												} catch (Exception e) {
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/detail/PembayaranGajiPunyaPegawaiAction.java:484");
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

			});
			button.setParent(toolbar);
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hitung Ulang", "/img/add_item.png");
		button.setVisible(pembayaranGaji.getId() != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// HITUNG ULANG benar: recompute nilai TIAP pegawai (bukan sekadar menjumlah), lalu jumlahkan.
						ais.action.master.payroll.helper.PembayaranGajiPunyaPegawaiHelper
								.hitungUlangSemuaPegawai(pembayaranGaji);

						loadData(null);
					}
				});

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {

						Criterion critKode = Restrictions.sqlRestriction("false");
						if (!kode.getValue().trim().equals("")) {
							critKode = Restrictions.or(critKode,
									Restrictions.ilike("pegawai.mycode", kode.getValue().trim(), MatchMode.ANYWHERE));
							critKode = Restrictions.or(critKode,
									Restrictions.ilike("pegawai.code", kode.getValue().trim(), MatchMode.ANYWHERE));
						} else {
							critKode = Restrictions.sqlRestriction("true");
						}

						Criterion critNama = Restrictions.sqlRestriction("false");
						if (!nama.getValue().trim().equals("")) {
							critNama = Restrictions.or(critNama,
									Restrictions.ilike("pegawai.nama", nama.getValue().trim(), MatchMode.ANYWHERE));
						} else {
							critNama = Restrictions.sqlRestriction("true");
						}

						Session session = HibernateUtil.currentNativeSession();
						// FIX: properti bersarang "pegawai.nama"/"pegawai.mycode"/"pegawai.code" pada
						// filter ilike TIDAK bisa di-resolve tanpa alias asosiasi → QueryException
						// "could not resolve property: pegawai.nama". Tambahkan createAlias("pegawai")
						// (konsisten dgn createCriteria lain di kelas ini). Baris ber-pegawai null
						// memang di-skip di loop bawah, jadi inner-join aman.
						List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = session
								.createCriteria(PembayaranGajiPunyaPegawai.class).createAlias("pegawai", "pegawai")
								.add(critKode).add(critNama)
								.add(Restrictions.eq("pembayaranGaji", pembayaranGaji)).list();
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
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/detail/PembayaranGajiPunyaPegawaiAction.java:590");
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

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode:")));
		toolbar.appendChild(kode = new MyTextbox());
		kode.setWidth("80px");
		kode.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama:")));
		toolbar.appendChild(nama = new MyTextbox());
		nama.setWidth("80px");
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
				PembayaranGaji.hitungUlang(pembayaranGaji);
				loadData(arg0);
			}
		});
		if (persetujuan) {
			Toolbarbutton delete;
			toolbar.appendChild(delete = new MyToolbarbuttonConfig("", "/img/svg/trash.svg"));
			delete.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus seluruh data ini? Data yang telah dihapus tidak dapat dikembalikan. Tekan OK untuk melanjutkan penghapusan, atau Batal untuk membatalkan.",
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
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan terlebih dahulu data lain yang berelasi; (2) periksa kembali keterkaitan data; (3) hubungi administrator apabila kendala masih berlanjut.",
													e.getMessage()));
										}

									}

								}
							});

				}
			});
		}
		grid = new MyGrid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new Column();
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
		column.setWidth("10%");

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
		column.setWidth("10%");

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
		paging.setParent(groupbox);

		loadData(null);
	}

}
