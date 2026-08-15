package ais.action.master.sekolah;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.DepositSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class PostingDepositSiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730217402400328L;
	private MyGrid grid;

	private Paging paging;
	private Textbox searchsiswa;
	private Combobox searchyayasan;
	private MyCheckboxConfig searchtampil;

	private Combobox searchsekolah;
	private Combobox searchjenis;
	private Decimalbox searchtahunMasuk;

	private boolean edit = false;

	private MyToolbarbuttonConfig sent;
	public boolean adminLain;

	private MyDatebox tglMulai;
	private MyDatebox tglSampai;

	private Tabpanel tabpanelPostingBiayaAdmin;

	public void onBiayaAdmin(Event event) {
		if (tabpanelPostingBiayaAdmin.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelPostingBiayaAdmin);
			MyInclude iframe = new MyInclude("/pages/master/posting_biaya_administrasi_pembayaran_siswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tabpanelPostingBiayaPaymentGateway;
	private Tbmuser tbmuser;

	public void onBiayaPaymentGateway(Event event) {
		if (tabpanelPostingBiayaPaymentGateway.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabpanelPostingBiayaPaymentGateway);
			MyInclude iframe = new MyInclude("/pages/master/posting_biaya_payment_gateway_pembayaran_siswa.zul");
			iframe.setParent(window);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tbmuser = Common.getCurrentUser();

		if (tglMulai != null) tglMulai.setValue(ais.ui.util.WaktuUtil.getDate());
		if (tglSampai != null) tglSampai.setValue(ais.ui.util.WaktuUtil.getDate());

		adminLain = Common.getApakahAdmin();

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		if (sent != null) { sent.setVisible(edit); }

		Tbmuser tbmuser = Common.getCurrentUser();

		Sekolah curr = tbmuser == null ? null : tbmuser.ambilSekolah();

		Common.insertComboDanSemua(searchjenis, new String[] { "nama", "sekolah" }, "keterangan", AkunPembayaranSiswa.class,

				Restrictions.and(
						curr == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", curr)),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

		);

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		sudahPostingDasbor = ais.action.master.helper.PostingJurnalHelper.ambilParameterSudahPosting();
		ais.action.master.helper.PostingJurnalHelper.terapkanParameterTanggal(tglMulai, tglSampai);

		loadDataDenganProgressPosting(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDenganProgressPosting(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi pembayaran ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<DepositSiswa> depositSiswas = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (DepositSiswa depositSiswa : depositSiswas) {
								depositSiswa.setPostingHistory(null);
								Common.refreshSaveOrUpdate(depositSiswa);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where deposit_siswa="
												+ depositSiswa.getId() + " and closing is null")
										.executeUpdate();
							}
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDenganProgressPosting(null);
							}
						});
					}
				});

	}

	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Pembayaran Siswa");
		addWindow.setWidth("800px");
		addWindow.setHeight("300px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid mygrid = new MyGrid();
		mygrid.setWidth("100%");
		mygrid.setParent(center);
		mygrid.setWidth("100%");
		mygrid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(mygrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("100px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");

		Rows rows = new Rows();
		rows.setParent(mygrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal / Waktu"));
		final MyDatebox tanggal;
		row.appendChild(tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diposting oleh"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				Common.getCurrentUser().ambilPegawai() == null ? Common.getCurrentUser().getUserId()
						: Common.getCurrentUser().ambilPegawai().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		final MyTextbox keterangan;
		row.appendChild(keterangan = new MyTextbox());
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final Date tgl = tanggal.getValue();
				if (tgl == null) {
					MyMessageboxConfig.show("Tanggal harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (keterangan.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Keterangan harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi pembayaran ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final Tbmuser tbmuser = Common.getCurrentUser();

									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											MyMessageboxConfig.show("Posting transaksi siswa berhasil dilakukan",
													"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
													new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															onSearchDefault(arg0);
														}
													});

											addWindow.detach();
										}
									});

									new Thread(new Runnable() {

										@Override
										public void run() {
											try {

											Session session = HibernateUtil.currentNativeSession();

											PostingHistory postingHistory = new PostingHistory(
													PostingHistory.JENIS_MAHASISWA);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<DepositSiswa> depositSiswas = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (DepositSiswa depositSiswa : depositSiswas) {
												if (depositSiswa != null
														&& depositSiswa.getAkunPembayaranSiswa() != null
														&& depositSiswa.getAkunPembayaranSiswa()
																.getAkunDeposit() != null
														&& depositSiswa.getAkunPembayaranSiswa().getAkun() != null) {

													Akun akunDebet = depositSiswa.getAkunPembayaranSiswa().getAkun();
													Akun akunKredit = depositSiswa.getAkunPembayaranSiswa()
															.getAkunDeposit();
													if (akunDebet != null && akunKredit != null) {
														Boolean apakahUangMasuk = true;
														String ket = "Deposit "
																+ ((depositSiswa.getSiswa() == null
																		? depositSiswa.getCalonSiswa()
																		: depositSiswa.getSiswa()))
																+ " via "
																+ depositSiswa.getAkunPembayaranSiswa().getNama()
																+ " - " + depositSiswa.getKeterangan();

														label.setValue(ket + " ("
																+ Common.numberFormat.get()
																		.format(rowIndex * 100.0 / depositSiswas.size())
																+ " %)");

														Double nilai = depositSiswa.getNominal();
														try {

															Akun akunDenda = null;
															Akun akunPiutangDenda = null;
															Double denda = 0.0;

															session.getTransaction().begin();
															if (nilai > 0.1) {
																CommonAkunting.saveTransaksi(akunDebet, akunKredit,
																		akunDenda, akunPiutangDenda, postingHistory,
																		apakahUangMasuk, ket,
																		depositSiswa.getPembayaranSiswa().getTanggal(),
																		nilai, denda, depositSiswa,
																		depositSiswa.getSekolah().getSatuanKerja(),
																		session);
															} else {
																CommonAkunting.saveTransaksi(akunKredit, akunDebet,
																		akunDenda, akunPiutangDenda, postingHistory,
																		apakahUangMasuk, ket,
																		depositSiswa.getPembayaranSiswa().getTanggal(),
																		nilai, denda, depositSiswa,
																		depositSiswa.getSekolah().getSatuanKerja(),
																		session);
															}
															session.getTransaction().commit();
														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
														}

														depositSiswa.setPostingHistory(postingHistory);
														session.getTransaction().begin();
														session.update(depositSiswa);
														session.getTransaction().commit();
													}

												}
												rowIndex++;
											}

											label.setValue("");
											HibernateUtil.closeSession();
																					} finally {
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

								}

							}
						});
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.onModal();
	}

	class DepositSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DepositSiswa depositSiswa = (DepositSiswa) arg1;

			if (depositSiswa.getSiswa() != null) {

				CommonMedia.tampilkanGambarKecil(depositSiswa.getSiswa()).setParent(arg0);

				new Label(depositSiswa.getSiswa() == null ? "" : depositSiswa.getSiswa().getNomorInduk())
						.setParent(arg0);

				RevisiHelper
						.createNewRevisi(DepositSiswa.class, depositSiswa,
								depositSiswa.getSiswa() == null ? "" : depositSiswa.getSiswa().getNama())
						.setParent(arg0);

				new Label(depositSiswa.getAkunPembayaranSiswa().getNama()).setParent(arg0);

				new Label(
						depositSiswa.getNominal() == null ? "0" : Common.numberFormat.get().format(depositSiswa.getNominal()))
						.setParent(arg0);

				// new Label(depositSiswa.getAmount() == null ? "0" :
				// Common.numberFormat.get().format(depositSiswa.getAmount()))
				// .setParent(arg0);
				new Label(depositSiswa.getSiswa() == null || depositSiswa.getSiswa().getSekolah() == null ? ""
						: depositSiswa.getSiswa().getSekolah().getNama()).setParent(arg0);
				new Label(depositSiswa.getSiswa() == null || depositSiswa.getSiswa().getSekolah() == null
						|| depositSiswa.getSiswa().getSekolah().getYayasan() == null ? ""
								: depositSiswa.getSiswa().getSekolah().getYayasan().getNama())
						.setParent(arg0);
			} else if (depositSiswa.getCalonSiswa() != null) {
				CommonMedia.tampilkanGambarKecil(depositSiswa.getCalonSiswa()).setParent(arg0);

				new Label(depositSiswa.getCalonSiswa() == null ? "" : depositSiswa.getCalonSiswa().getNoRegistrasi())
						.setParent(arg0);
				RevisiHelper
						.createNewRevisi(DepositSiswa.class, depositSiswa,
								depositSiswa.getCalonSiswa() == null ? "" : depositSiswa.getCalonSiswa().getNama())
						.setParent(arg0);

				new Label(depositSiswa.getAkunPembayaranSiswa().getNama()).setParent(arg0);

				new Label(
						depositSiswa.getNominal() == null ? "0" : Common.numberFormat.get().format(depositSiswa.getNominal()))
						.setParent(arg0);

				// new Label(depositSiswa.getAmount() == null ? "0" :
				// Common.numberFormat.get().format(depositSiswa.getAmount()))
				// .setParent(arg0);
				new Label(depositSiswa.getCalonSiswa() == null || depositSiswa.getCalonSiswa().getSekolah() == null ? ""
						: depositSiswa.getCalonSiswa().getSekolah().getNama()).setParent(arg0);
				new Label(depositSiswa.getCalonSiswa() == null || depositSiswa.getCalonSiswa().getSekolah() == null
						|| depositSiswa.getCalonSiswa().getSekolah().getYayasan() == null ? ""
								: depositSiswa.getCalonSiswa().getSekolah().getYayasan().getNama())
						.setParent(arg0);
			}

			new Label(Common.dateFormat3.get().format(depositSiswa.getWaktu())).setParent(arg0);

			Akun akunDebet = depositSiswa.getAkunPembayaranSiswa().getAkun();
			Akun akunKredit = depositSiswa.getAkunPembayaranSiswa().getAkunDeposit();
			if (akunDebet != null && akunKredit != null) {
				Double nilai = depositSiswa.getNominal();

				GrupTransaksi.tampilkanJurnal(akunDebet, nilai, akunKredit, nilai).setParent(arg0);
			} else {
				// Debet & kredit sama-sama dari Akun Pembayaran Siswa yang sama (Akun & Akun Deposit).
				String namaAkunPembayaran = depositSiswa.getAkunPembayaranSiswa() == null ? "yang dipakai"
						: "\"" + depositSiswa.getAkunPembayaranSiswa().getNama() + "\"";
				ais.action.master.helper.AnalisisPemetaanAkunHelper.tampilkanInvalid(arg0, CommonAkunting.jelaskanTransaksiTidakValid(akunDebet, akunKredit,
						CommonAkunting.langkahLengkapiKolomAkun("Akun Pembayaran Siswa",
								"Akun Pembayaran Siswa " + namaAkunPembayaran, "Akun"),
						CommonAkunting.langkahLengkapiKolomAkun("Akun Pembayaran Siswa",
								"Akun Pembayaran Siswa " + namaAkunPembayaran, "Akun Deposit")));
			}

			new Label(depositSiswa.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: depositSiswa.getPostingHistory().toString()).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			Double nilai = depositSiswa.getNominal() - (depositSiswa.getPembayaranSiswa() == null ? 0.0
					: depositSiswa.getPembayaranSiswa().getNominal());

			if (akunDebet != null && akunKredit != null && nilai > 0.1) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && depositSiswa.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								depositSiswa.setPostingHistory(null);
								Common.refreshSaveOrUpdate(depositSiswa);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where deposit_siswa="
												+ depositSiswa.getId() + " and closing is null")
										.executeUpdate();

								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										loadDataDenganProgressPosting(null);
									}
								});
							}
						});

					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("", "/img/svg/check2-circle.svg");
				button.setTooltiptext("Posting Data");
				button.setVisible(edit && depositSiswa.getPostingHistory() == null && tbmuser != null
						&& depositSiswa.getSekolah().getSatuanKerja() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_MAHASISWA);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Akun akunDebet = depositSiswa.getAkunPembayaranSiswa().getAkun();
								Akun akunKredit = depositSiswa.getAkunPembayaranSiswa().getAkunDeposit();
								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;
									String ket = "Deposit "
											+ ((depositSiswa.getSiswa() == null ? depositSiswa.getCalonSiswa()
													: depositSiswa.getSiswa()))
											+ " via " + depositSiswa.getAkunPembayaranSiswa().getNama() + " - "
											+ depositSiswa.getKeterangan();

									Double nilai = depositSiswa.getNominal()
											- (depositSiswa.getPembayaranSiswa() == null ? 0.0
													: depositSiswa.getPembayaranSiswa().getNominal());

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									if (nilai > 0.1) {
										CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket, depositSiswa.getTanggalBayar(),
												nilai, denda, depositSiswa, depositSiswa.getSekolah().getSatuanKerja(),
												session);
									} else {
										CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket, depositSiswa.getTanggalBayar(),
												nilai, denda, depositSiswa, depositSiswa.getSekolah().getSatuanKerja(),
												session);
									}

									depositSiswa.setPostingHistory(postingHistory);
									session.update(depositSiswa);
								}

								loadDataDenganProgressPosting(null);
							}
						});

					}

				});
				button.setParent(toolbar);
			}

		}
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Yayasan yayasan = (Yayasan) (searchyayasan.getSelectedItem() == null
				|| searchyayasan.getSelectedItem().getValue() == null
				|| searchyayasan.getSelectedItem().getValue() == null ? null
						: searchyayasan.getSelectedItem().getValue());
		Sekolah sekolah = (Sekolah) (searchsekolah.getSelectedItem() == null
				|| searchsekolah.getSelectedItem().getValue() == null
				|| searchsekolah.getSelectedItem().getValue() == null ? null
						: searchsekolah.getSelectedItem().getValue());

		Criteria criteria = session.createCriteria(DepositSiswa.class).add(ais.action.master.helper.PostingJurnalHelper.restriksiPosting("postingHistory", sudahPostingDasbor))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(tanggal_bayar) between date('" + Common.databaseDateFormat.get().format(tglMulai.getValue())
								+ "') and  date('" + Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(!searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

				.add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("akunPembayaranSiswa", searchjenis.getSelectedItem().getValue()));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
				.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)

				.add(searchtahunMasuk.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("siswa.tahunMasuk", searchtahunMasuk.getValue().intValue()),
								Restrictions.eq("calonSiswa.tahunMasuk", searchtahunMasuk.getValue().intValue())))

				.add(sekolah == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("siswa.sekolah", sekolah),
								Restrictions.eq("calonSiswa.sekolah", sekolah)))

				.add(yayasan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("siswa.yayasan", yayasan),
								Restrictions.eq("calonSiswa.yayasan", yayasan)))

				.add(searchsiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.or(
								Restrictions.ilike("siswa.nama", searchsiswa.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("calonSiswa.nama", searchsiswa.getValue().trim(),
										MatchMode.ANYWHERE)),
								Restrictions.or(
										Restrictions.or(
												Restrictions.or(
														Restrictions.ilike("siswa.nomorInduk",
																searchsiswa.getValue().trim(), MatchMode.ANYWHERE),
														Restrictions.ilike("calonSiswa.nomorInduk",
																searchsiswa.getValue().trim(), MatchMode.ANYWHERE)),
												Restrictions.ilike("calonSiswa.noRegistrasi",
														searchsiswa.getValue().trim(), MatchMode.ANYWHERE)),
										Restrictions.ilike("calonSiswa.noUjian", searchsiswa.getValue().trim()))));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DepositSiswa> depositSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(depositSiswa);
		grid.setRowRenderer(new DepositSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}


	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/* Status posting kiriman dasbor draft jurnal (null = dibuka dari menu). */
	private Boolean sudahPostingDasbor = null;

	private boolean postingJurnalLoadingAktif = false;
	private boolean postingJurnalReloadTertunda = false;

	private void loadDataDenganProgressPosting(final org.zkoss.zk.ui.event.Event event) {
		if (postingJurnalLoadingAktif) {
			postingJurnalReloadTertunda = true;
			ais.ui.util.PostingJurnalLoadingUtil.show("Memuat Ulang Data Posting Jurnal",
					"Permintaan reload baru diterima. Data akan dimuat ulang setelah proses yang berjalan selesai.", 12);
			return;
		}
		postingJurnalLoadingAktif = true;
		postingJurnalReloadTertunda = false;
		ais.ui.util.PostingJurnalLoadingUtil.show("Memuat Data Posting Jurnal",
				"Menyiapkan filter dan tabel data jurnal.", 7);
		Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event timerEvent) throws Exception {
				try {
					ais.ui.util.PostingJurnalLoadingUtil.update("Mengambil Data Posting Jurnal",
							"Mencari data sesuai tanggal, status posting, dan filter halaman.", 48);
					onSearchDefaultTanpaProgress(event);
					ais.ui.util.PostingJurnalLoadingUtil.update("Merapikan Tampilan",
							"Menyusun tabel, paging, status posting, dan preview jurnal.", 92);
				} finally {
					boolean reloadLagi = postingJurnalReloadTertunda;
					postingJurnalReloadTertunda = false;
					postingJurnalLoadingAktif = false;
					if (reloadLagi) {
						ais.ui.util.PostingJurnalLoadingUtil.update("Memuat Ulang Data Posting Jurnal",
								"Filter atau halaman berubah saat data sedang diproses. Data akan dimuat ulang sekarang.", 96);
						Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
							@Override
							public void onEvent(org.zkoss.zk.ui.event.Event ulangEvent) throws Exception {
								loadDataDenganProgressPosting(event);
							}
						});
					} else {
						ais.ui.util.PostingJurnalLoadingUtil.complete("Data Posting Jurnal Siap",
								"Tabel sudah selesai dimuat dan siap digunakan.", 100);
					}
				}
			}
		});
	}

}
