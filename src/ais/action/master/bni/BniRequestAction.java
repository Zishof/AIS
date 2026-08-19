package ais.action.master.bni;

import java.util.List;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Vlayout;

import ais.action.master.KegiatanTemporaryAction.DetailKegiatanTemporaryRenderer;
import ais.action.master.helper.RevisiBniRequestHelper;
import ais.action.master.helper.RevisiCicilanPembayaranTemporaryHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.servlet.Bniresponse;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Fakultas;
import ais.database.model.ItemBiaya;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Tbmuser;
import ais.database.model.bni.BniRequest;
import ais.database.model.bni.BniRequestDetail;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.action.master.helper.FilterLanjutHelper;

public class BniRequestAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730217402400328L;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchtrxId;
	private Textbox searchnim;
	private Combobox tahunAkademik;
	private Combobox status;

	private MyToolbarbuttonConfig find;

	private MyDatebox searchmulai;
	private MyDatebox searchsampai;

	private Tbmuser tbmuser = null;
	private Mahasiswa mahasiswa = null;
	private Siswa siswa = null;
	private Sekolah selectedSekolah = null;
	private Yayasan selectedYayasan = null;
	private CalonSiswa selectedCalonSiswa = null;
	private MyColumnConfig biayaAdminCol;
	public static TreeMap<String, String> statses = new TreeMap<String, String>();

	private Row hbFakultasLabel;
	private Row hbYayasan;
	private boolean pt = false;
	private boolean ya = false;

	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	static {
		statses.put("000", "Success");
		statses.put("001", "Incomplete/invalid Parameter(s)");
		statses.put("002", "IP address not allowed or wrong Client ID");
		statses.put("003", "Words not match");
		statses.put("004", "Service not found");
		statses.put("005", "Service not defined");
		statses.put("006", "Invalid VA Number");
		statses.put("007", "Amount not match");

		statses.put("008", "Technical Failure");
		statses.put("009", "Unexpected Error");
		statses.put("010", "Request Timeout");
		statses.put("011", "Billing type does not match billing amount");
		statses.put("012", "Invalid expiry date/time");
		statses.put("100", "Billing has been paid");
		statses.put("101", "Billing not found");
		statses.put("102", "VA Number is in use");
		statses.put("103", "Billing has been expired");
		statses.put("104", "Billing cancelled");
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
		biayaAdminCol.setVisible(Common.bolehKonfigurasi("tampilkan_biaya_admin_di_virtual_account"));

		statses.put("105", "Duplicate Billing ID");
		statses.put("997", "System is temporarily offline");
		statses.put("998", "\"Content-Type\" header not defined as it should be");
		statses.put("999", "Internal Error");

		try {
			pt = Common.bolehKonfigurasi("apakah_aktifkan_modul_perguruan_tinggi");
			ya = Common.bolehKonfigurasi("apakah_aktifkan_modul_sekolah", Konfigurasi.TIDAK_AKTIF);

			Sekolah sekolah = SekolahUtil.getSekolah();
			if (sekolah != null && sekolah.getId() != null) {
				pt = false;
				ya = true;
			}

			if (searchfakultas != null && searchjurusan != null && hbFakultasLabel != null && hbYayasan != null
					&& searchyayasan != null && searchsekolah != null) {
				Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
				Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

				hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1);
				hbYayasan.setVisible(ya);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/bni/BniRequestAction.java:165");
			// TODO: handle exception
		}

		MyComboitemConfig comboitem = new MyComboitemConfig("Semua Status");
		if (comboitem != null) { comboitem.setValue(null); }
		status.appendChild(comboitem);
		if (status != null) { status.setSelectedItem(comboitem); }

		for (String kode : statses.keySet()) {
			comboitem = new MyComboitemConfig(statses.get(kode));
			comboitem.setValue(kode);
			status.appendChild(comboitem);
		}

		if (status != null) { status.setReadonly(true); }

		tbmuser = Common.getCurrentUser();

		mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		siswa = tbmuser == null ? null : tbmuser.getSiswa();

		if (ExecutionsCtrl.getCurrent().getParameter("siswa") != null) {
			siswa = (Siswa) HibernateUtil.currentSession().createCriteria(Siswa.class)
					.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
					.add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.idEq(Long.parseLong(ExecutionsCtrl.getCurrent().getParameter("siswa"))))
					.uniqueResult();
		}

		if (ExecutionsCtrl.getCurrent().getParameter("calon_siswa") != null) {
			selectedCalonSiswa = (CalonSiswa) HibernateUtil.currentSession().createCriteria(CalonSiswa.class)
					.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
					.add(Restrictions.idEq(Long.parseLong(ExecutionsCtrl.getCurrent().getParameter("calon_siswa"))))
					.uniqueResult();
		}

		selectedSekolah = SekolahUtil.getSekolah();
		selectedYayasan = SekolahUtil.getYayasan();

		if (siswa != null) {
			selectedSekolah = siswa.getSekolah();
			selectedYayasan = siswa.getYayasan();
		}

		Common.generateTahunAjaranDanSemua(tahunAkademik);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "id", "nama", "url", "trxId", "merchant_id",
				"merchant", "response_code", "response_desc", "request", "response", "status", "kodeStatus",
				"mahasiswa", "biodataCalonMahasiswa", "jenisKegiatan", "jadwalPembayaran", "semester", "tahunAkademik",
				"keterangan", "pengurangan", "nilaiBiayaHarusDiBayars", "bniResponse", "amount", "biayaAdministrasi");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Check Ulang Semua", "/img/svg/check2.svg");
		cetakSksDosen.setVisible(Common.bolehKonfigurasi("aktifkan_tombol_check_ulang_bni"));
		Common.appendKeToolbar(cetakSksDosen, find, comp);
		cetakSksDosen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {

						final Label label = new Label(ais.common.Common.getBahasaConfig("Proses Check Ulang Semua"));

						new Thread(new Runnable() {

							@Override
							public void run() {
								try {
								List<BniRequest> perkuliahans = initCriteria(true).setMaxResults(1000).list();

								int i = 0;
								int size = perkuliahans.size();

								for (BniRequest bniRequest : perkuliahans) {

									Session session = HibernateUtil.currentNativeSession();
									try {

										Siswa siswa = bniRequest.getSiswa();
										String ipClient = (Common.getKonfigurasi("bni_ip_client", "").getNilai());
										if (!ipClient.trim().isEmpty()) {
											ipClient = ipClient + "/BniForwarder";
										}
										String strURL = !ipClient.trim().isEmpty() ? ipClient
												: (siswa != null && siswa.getSekolah() != null
														&& !siswa.getSekolah().getBniGatewayUrl().isEmpty()
																? siswa.getSekolah().getBniGatewayUrl()
																: (Common
																		.getKonfigurasi("bni_gateway_url",
																				"https://apibeta.bni-ecollection.com/")
																		.getNilai()));

										boolean tambahkanMerchanId = Common.bolehKonfigurasi("tambahkan_merchan_id_di_bni");
										if (!tambahkanMerchanId) {
											strURL = Common.getKonfigurasi("bni_inquiry_gateway_url",
													"https://billing-bpi.maja.id/bni/inquiry/").getNilai();
										}

										if (!bniRequest.getKegiatanTemporarys().isEmpty()) {
											bniRequest.setHapusCicilanSebelumnya(true);
											bniRequest.setCheckUlang(true);
											session.getTransaction().begin();
											Common.refreshUpdate(session, bniRequest);
											session.getTransaction().commit();

											BniBackandProsess.check(strURL, bniRequest, session, true);

										} else {
											if (bniRequest.getSiswa() == null && bniRequest.getCalonSiswa() == null) {
												Kegiatan kegiatan = Bniresponse.createKegiatan(bniRequest, session);

												List<CicilanPembayaran> cicilanPembayarans = session
														.createCriteria(CicilanPembayaran.class)
														.add(Restrictions.isNotNull("itemBiaya"))
														.add(Restrictions.eq("kegiatan", kegiatan))
														.addOrder(Order.asc("tanggal")).addOrder(Order.asc("ke"))
														.list();

												for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
													int count = ((Number) session.createCriteria(BniRequestDetail.class)
															.add(Restrictions.eq("bniRequest", bniRequest))
															.add(Restrictions.eq("pengaturanPembayaranBulanan",
																	cicilanPembayaran.getPengaturanPembayaranBulanan()))
															.setProjection(Projections.rowCount()).uniqueResult())
															.intValue();
													if (count == 0) {
														BniRequestDetail bniRequestDetail = new BniRequestDetail();
														bniRequestDetail.setBniRequest(bniRequest);
														bniRequestDetail.setPengaturanPembayaranBulanan(
																cicilanPembayaran.getPengaturanPembayaranBulanan());

														PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
																.getPengaturanPembayaranBulanan();
														ItemBiaya itemBiaya = cicilanPembayaran.getItemBiaya();

														bniRequestDetail.setIdCicilan(cicilanPembayaran == null ? null
																: cicilanPembayaran.getId());
														bniRequestDetail.setPengaturanPembayaranBulanan(
																pengaturanPembayaranBulanan);
														bniRequestDetail.setItemBiaya(itemBiaya);
														bniRequestDetail
																.setKeterangan(cicilanPembayaran.getKeterangan());
														bniRequestDetail.setNilai(cicilanPembayaran.getNilai());
														bniRequestDetail.setTanggal(cicilanPembayaran.getTanggal());
														bniRequestDetail.setKe(0);

														bniRequestDetail.setDenda(cicilanPembayaran == null
																|| cicilanPembayaran.getId() == null ? null
																		: cicilanPembayaran.getDenda());
														bniRequestDetail.setNilaiAsli(cicilanPembayaran == null
																|| cicilanPembayaran.getId() == null ? null
																		: cicilanPembayaran.getNilaiAsli());

														session.getTransaction().begin();
														Common.refreshSaveOrUpdate(session, bniRequestDetail);
														session.getTransaction().commit();
													}
												}
											}
											session.refresh(bniRequest);
											bniRequest.setHapusCicilanSebelumnya(true);
											bniRequest.setCheckUlang(true);
											session.getTransaction().begin();
											Common.refreshUpdate(session, bniRequest);
											session.getTransaction().commit();

											BniBackandProsess.check(strURL, bniRequest, session, true);

										}
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}
									if (label != null) {
										label.setValue("(" + (Common.numberFormat.get().format(i * 100.0 / size))
												+ " %) check ulang data  " + bniRequest + " ..");
									}
									i++;
								}
								label.setValue("");

															} finally {
									ais.database.hibernate.HibernateUtil.closeSession();
								}
							}
						}).start();

						final Timer timer = new Timer(500);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								// System.out.println("process = " +
								// label.getValue());
								Clients.showBusy(label.getValue());
								if (label.getValue().isEmpty()) {
									Clients.clearBusy();
									MyMessageboxConfig.show("Proses check ulang pembayaran telah selesai",
											"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									onSearchDefault(null);
									timer.detach();
								}

							}
						});
						timer.start();

					}
				});
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiBniRequestHelper revisiHelper = new RevisiBniRequestHelper(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});
					}
				});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();
			}

		});
		if (button != null) { button.setParent(find.getParent()); }

	        FilterLanjutHelper.setup(comp);
}

	class BniRequestRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BniRequest bniRequest = (BniRequest) arg1;
			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@SuppressWarnings("unchecked")
				private void reload() throws Exception {
					Common.clear(detail);

					Vlayout vlayout = new Vlayout();
					vlayout.setParent(detail);
					vlayout.setStyle("min-height: 200px;");

					Toolbar toolbar = new Toolbar();
					toolbar.setParent(vlayout);

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							RevisiCicilanPembayaranTemporaryHelper revisiHelper = new RevisiCicilanPembayaranTemporaryHelper(
									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													reload();
												}
											});
										}
									}, bniRequest.getKegiatanTemporarys());
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
							revisiHelper.setVisible(true);
							revisiHelper.onModal();

						}

					});
					button.setParent(toolbar);

					// Asumsi variabel bniRequest (header) sudah dideklarasikan sebelumnya
					// final BniRequest bniRequest = ...;

					MyToolbarbuttonConfig buttonRestoreDetail = new MyToolbarbuttonConfig("Restore Detail & Relasinya",
							"/img/svg/clock-history.svg");
					buttonRestoreDetail.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK,
							new org.zkoss.zk.ui.event.EventListener() {

								@Override
								public void onEvent(org.zkoss.zk.ui.event.Event event) throws Exception {

									if (bniRequest == null || bniRequest.getId() == null) {
										ais.ui.util.MyMessageboxConfig.show("Pilih header BNI Request terlebih dahulu.",
												"Peringatan", ais.ui.util.MyMessageboxConfig.OK,
												ais.ui.util.MyMessageboxConfig.EXCLAMATION);
										return;
									}

									ais.ui.util.MyMessageboxConfig.show(
											"Apakah Anda yakin ingin memulihkan semua BNI Request Detail untuk transaksi ini, beserta SELURUH data relasinya yang mungkin hilang?",
											"Konfirmasi Restore Terpusat",
											ais.ui.util.MyMessageboxConfig.OK | ais.ui.util.MyMessageboxConfig.CANCEL,
											ais.ui.util.MyMessageboxConfig.QUESTION,
											new org.zkoss.zk.ui.event.EventListener() {

												@Override
												public void onEvent(org.zkoss.zk.ui.event.Event event)
														throws Exception {
													int i = Integer.parseInt(event.getData().toString());
													if (i == ais.ui.util.MyMessageboxConfig.OK) {

														org.hibernate.Session session = null;
														org.hibernate.Transaction tx = null;

														try {
															// Fitur restore dari audit trail memerlukan Hibernate Envers 															// yang tidak aktif pada Hibernate 3.6. 															ais.ui.util.MyMessageboxConfig.show( 															    "Fitur ini tidak tersedia pada versi Hibernate ini.", 															    "Informasi", ais.ui.util.MyMessageboxConfig.OK, 															    ais.ui.util.MyMessageboxConfig.INFORMATION);

														} catch (Exception e) {
															if (tx != null && tx.isActive()) {
																tx.rollback();
															}
															ais.common.Common.tampilErrorJikaAdmin(e);
															ais.ui.util.MyMessageboxConfig.show(
																	"Data tidak dapat dipulihkan karena terjadi kesalahan sistem. Error: \n"
																			+ e.getMessage(),
																	"Gagal", ais.ui.util.MyMessageboxConfig.OK,
																	ais.ui.util.MyMessageboxConfig.ERROR);
														} finally {
															if (session != null && session.isOpen()) {
																try {
																	session.disconnect();
																	session.close();
																} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/bni/BniRequestAction.java:527");
																}
															}
														}
													}
												}
											});
								}
							});
					buttonRestoreDetail.setParent(toolbar);

					MyGrid grid = new MyGrid();
					grid.setParent(vlayout);

					Columns columns = new Columns();
					columns.setParent(grid);

					MyColumnConfig column = new MyColumnConfig("Keterangan");
					column.setParent(columns);
					column.setWidth("80%");

					column = new MyColumnConfig("Nominal");
					column.setParent(columns);
					column.setWidth("20%");

					Rows rows = new Rows();
					rows.setParent(grid);

					HibernateUtil.currentSession().refresh(bniRequest);
					if (!bniRequest.getKegiatanTemporarys().isEmpty()) {
						List<CicilanPembayaran> cicilanPembayarans = HibernateUtil.currentSession()
								.createCriteria(CicilanPembayaran.class)
								.add(Restrictions.in("kegiatanTemporary", bniRequest.getKegiatanTemporarys())).list();

						ListModel strset = new SimpleListModel(cicilanPembayarans);
						grid.setRowRenderer(new DetailKegiatanTemporaryRenderer());
						grid.setModelCheckMobile(strset);
					} else {

						List<BniRequestDetail> bniRequestDetails = HibernateUtil.currentSession()
								.createCriteria(BniRequestDetail.class).add(Restrictions.isNull("idCicilan"))
								.add(Restrictions.eq("bniRequest", bniRequest)).list();
						for (BniRequestDetail bniRequestDetail : bniRequestDetails) {
							Row row = new Row();
							row.setValign("top");
							row.setParent(rows);

							if (bniRequestDetail.getTagihan() != null) {
								Tagihan tagihan = bniRequestDetail.getTagihan();
								String desc = tagihan.getId() + "-" + tagihan.getItemBiayaSekolah().getNama()
										+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
												? " (ke " + tagihan.getBayarKe() + ")"
												: "")
										+ (tagihan.getBulan() == null ? "" : ", bulan " + tagihan.getBulan())
										+ (tagihan.getTahun() == null ? "" : ", tahun " + tagihan.getTahun()) + ", ";

								row.appendChild(new Label(desc));

							} else {
								String desc = bniRequestDetail.getId() + "-"
										+ (bniRequestDetail.getItemBiaya() == null ? ""
												: bniRequestDetail.getItemBiaya().getNama())
										+ (bniRequestDetail.getPengaturanPembayaranBulanan() == null ? ""
												: " bulan " + bniRequestDetail.getPengaturanPembayaranBulanan()
														.getRealBulan())

										+ ", ";
								row.appendChild(new Label(desc + bniRequestDetail.getKeterangan()));
							}

							RevisiHelper.createNewRevisi(BniRequestDetail.class, bniRequestDetail,
									Common.numberFormat.get().format(bniRequestDetail.getNilai())).setParent(row);

						}
					}
				}

				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						reload();
					}
				}
			});

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			RevisiHelper.createNewRevisi(BniRequest.class, bniRequest, bniRequest.getTrxId()).setParent(vbox);

			new Label(bniRequest.getVa()).setParent(vbox);
			if (bniRequest.getMahasiswa() != null) {
				new Label(bniRequest.getMahasiswa().toString()).setParent(arg0);
			} else if (bniRequest.getBiodataCalonMahasiswa() != null) {
				new Label(bniRequest.getBiodataCalonMahasiswa().toString()).setParent(arg0);
			} else if (bniRequest.getSiswa() != null) {
				new Label(bniRequest.getSiswa().getNomorInduk() + "-" + bniRequest.getSiswa().getNama())
						.setParent(arg0);
			} else if (bniRequest.getCalonSiswa() != null) {
				new Label(bniRequest.getCalonSiswa().getNomorInduk() + "-" + bniRequest.getCalonSiswa().getNama())
						.setParent(arg0);
			}
			try {
				new Label(bniRequest.getTanggal_dirubah() == null ? ""
						: Common.dateFormat3.get().format(bniRequest.getTanggal_dirubah())).setParent(arg0);
			} catch (Exception e) {
				new Label().setParent(arg0);
			}
			new Label(Common.numberFormat.get().format(bniRequest.getAmount())).setParent(arg0);
			new Label(Common.numberFormat.get().format(bniRequest.getBiayaAdministrasi())).setParent(arg0);
			new Label(bniRequest.getJenisKegiatan() == null ? bniRequest.getKeterangan()
					: bniRequest.getJenisKegiatan().getNamaKegiatan()).setParent(arg0);
			new Label((bniRequest.getTahunAkademik() == null ? "" : bniRequest.getTahunAkademik())
					+ (bniRequest.getSemester() == null ? "" : "-" + bniRequest.getSemester())).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);

			new Label(bniRequest.getStatus()).setParent(hbox);

			// Kolom aksi rapi (pola MahasiswaAction): tombol dibungkus kebab popup (⋯) via
			// UIHelper.buatBarisAksi. Kebab dipasang ke hbox (BUKAN ke arg0) karena sel ini
			// juga memuat label status — memindahkannya ke Row akan menambah sel baru.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyButtonConfig button = new MyButtonConfig("Cek Pembayaran");
			aksiButtons.add(button);
			button.setVisible(Common.bolehKonfigurasi("tampilkan_check_ulang_pembayaran_via_bni"));
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.createDefaultTimer(new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event arg0) throws Exception {

							Siswa siswa = bniRequest.getSiswa();
							String ipClient = (Common.getKonfigurasi("bni_ip_client", "").getNilai());
							if (!ipClient.trim().isEmpty()) {
								ipClient = ipClient + "/BniForwarder";
							}
							String strURL = !ipClient.trim().isEmpty() ? ipClient
									: (siswa != null && siswa.getSekolah() != null
											&& !siswa.getSekolah().getBniGatewayUrl().isEmpty()
													? siswa.getSekolah().getBniGatewayUrl()
													: (Common
															.getKonfigurasi("bni_gateway_url",
																	"https://apibeta.bni-ecollection.com/")
															.getNilai()));

							boolean tambahkanMerchanId = Common.bolehKonfigurasi("tambahkan_merchan_id_di_bni");
							if (!tambahkanMerchanId) {
								strURL = Common.getKonfigurasi("bni_inquiry_gateway_url",
										"https://billing-bpi.maja.id/bni/inquiry/").getNilai();
							}

							Session session = HibernateUtil.currentNativeSession();

							session.refresh(bniRequest);
							if (!bniRequest.getKegiatanTemporarys().isEmpty()) {
								bniRequest.setHapusCicilanSebelumnya(true);
								bniRequest.setCheckUlang(true);
//								session.getTransaction().begin();
//								Common.refreshUpdate(session, bniRequest);
//								session.getTransaction().commit();

								JSONObject jsonObject = BniBackandProsess.check(strURL, bniRequest, session, true);
								// session.disconnect();
								ais.common.Common.closeOpenedSession(session);

								if (Common.getApakahAdmin())
									MyMessageboxConfig.show(
											"Cek ulang telah dilakukan..\n\n\nInformasi lebih lanjut : \n"
													+ (jsonObject == null ? "" : jsonObject.toString()),
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
											new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});
							} else {
								if (bniRequest.getSiswa() == null && bniRequest.getCalonSiswa() == null) {
									Kegiatan kegiatan = Bniresponse.createKegiatan(bniRequest, session);

									List<CicilanPembayaran> cicilanPembayarans = session
											.createCriteria(CicilanPembayaran.class)
											.add(Restrictions.isNotNull("itemBiaya"))
											.add(Restrictions.eq("kegiatan", kegiatan)).addOrder(Order.asc("tanggal"))
											.addOrder(Order.asc("ke")).list();

									for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
										int count = ((Number) session.createCriteria(BniRequestDetail.class)
												.add(Restrictions.eq("bniRequest", bniRequest))
												.add(Restrictions.eq("pengaturanPembayaranBulanan",
														cicilanPembayaran.getPengaturanPembayaranBulanan()))
												.setProjection(Projections.rowCount()).uniqueResult()).intValue();
										if (count == 0) {
											BniRequestDetail bniRequestDetail = new BniRequestDetail();
											bniRequestDetail.setBniRequest(bniRequest);
											bniRequestDetail.setPengaturanPembayaranBulanan(
													cicilanPembayaran.getPengaturanPembayaranBulanan());

											PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
													.getPengaturanPembayaranBulanan();
											ItemBiaya itemBiaya = cicilanPembayaran.getItemBiaya();

											bniRequestDetail.setDetailBiaya(cicilanPembayaran.getDetailBiaya());
											bniRequestDetail.setIdCicilan(
													cicilanPembayaran == null ? null : cicilanPembayaran.getId());
											bniRequestDetail
													.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
											bniRequestDetail.setItemBiaya(itemBiaya);
											bniRequestDetail.setKeterangan(cicilanPembayaran.getKeterangan());
											bniRequestDetail.setNilai(cicilanPembayaran.getNilai());
											bniRequestDetail.setTanggal(cicilanPembayaran.getTanggal());
											bniRequestDetail.setKe(0);

											bniRequestDetail.setDenda(
													cicilanPembayaran == null || cicilanPembayaran.getId() == null
															? null
															: cicilanPembayaran.getDenda());
											bniRequestDetail.setNilaiAsli(
													cicilanPembayaran == null || cicilanPembayaran.getId() == null
															? null
															: cicilanPembayaran.getNilaiAsli());

											session.getTransaction().begin();
											Common.refreshSaveOrUpdate(session, bniRequestDetail);
											session.getTransaction().commit();
										}
									}
								}
//								session.refresh(bniRequest);
								bniRequest.setHapusCicilanSebelumnya(true);
								bniRequest.setCheckUlang(true);
//								session.getTransaction().begin();
//								Common.refreshUpdate(session, bniRequest);
//								session.getTransaction().commit();

								JSONObject jsonObject = BniBackandProsess.check(strURL, bniRequest, session, true);
								// session.disconnect();
								ais.common.Common.closeOpenedSession(session);
								if (Common.getApakahAdmin())
									MyMessageboxConfig.show(
											"Cek ulang telah dilakukan..\n\n\nInformasi lebih lanjut : \n"
													+ (jsonObject == null ? "" : jsonObject.toString()),
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
											new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});
							}
						}
					});
				}
			});

			if (Common.getApakahAdmin() && !bniRequest.getStatus().equalsIgnoreCase("Payment Sukses")) {
				button = new MyButtonConfig("Hapus", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												HibernateUtil.currentSession().refresh(bniRequest);
												Common.refreshDelete(bniRequest);

												onSearchDefault(event);
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(
														"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																+ e.getMessage());
											}

										}

									}
								});

					}
				});
				aksiButtons.add(button);
			}

			ais.ui.util.UIHelper.buatBarisAksi(hbox, 3, aksiButtons);
		}

	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {

		Sekolah sekolah = (Sekolah) (searchsekolah == null || searchsekolah.getSelectedItem() == null ? null
				: searchsekolah.getSelectedItem().getValue());
		Yayasan yayasan = (Yayasan) (searchsekolah == null || searchyayasan.getSelectedItem() == null ? null
				: searchyayasan.getSelectedItem().getValue());

		Jurusan jurusan = (Jurusan) (searchjurusan == null || searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());
		Fakultas fakultas = (Fakultas) (searchfakultas == null || searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());

		if (fakultas != null || jurusan != null) {
			sekolah = null;
			yayasan = null;
		}

		Session session = HibernateUtil.currentSession();

		List<Long> longsJurusans = fakultas != null && fakultas.getId() != null
				? session.createCriteria(Jurusan.class).add(Restrictions.eq("aktif", true))
						.setProjection(Projections.property("id")).add(Restrictions.eq("fakultas", fakultas)).list()
				: null;

		Criteria criteria = session.createCriteria(BniRequest.class)
				.add(mahasiswa != null ? Restrictions.eq("mahasiswa", mahasiswa) : Restrictions.sqlRestriction("true"))
				.add(siswa != null ? Restrictions.eq("siswa", siswa) : Restrictions.sqlRestriction("true"))
				.add(selectedCalonSiswa != null ? Restrictions.eq("calonSiswa", selectedCalonSiswa)
						: Restrictions.sqlRestriction("true"))

				.createAlias("bniResponse", "bniResponse", Criteria.LEFT_JOIN)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
				.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)
				.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa", Criteria.LEFT_JOIN)

				.add(sekolah == null || sekolah.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("siswa.sekolah", sekolah),
								Restrictions.eq("calonSiswa.sekolah", sekolah)))

				.add(jurusan == null || jurusan.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("mahasiswa.jurusan", jurusan),
								Restrictions.or(Restrictions.eq("biodataCalonMahasiswa.prodi1", jurusan),
										Restrictions.eq("biodataCalonMahasiswa.prodiLulus", jurusan))))

				.add(fakultas != null && fakultas.getId() != null && longsJurusans.isEmpty()
						? Restrictions.sqlRestriction("false")
						: longsJurusans == null || longsJurusans.isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.in("mahasiswa.jurusan.id", longsJurusans),
										Restrictions.or(
												Restrictions.in("biodataCalonMahasiswa.prodi1.id", longsJurusans),
												Restrictions.in("biodataCalonMahasiswa.prodiLulus.id", longsJurusans))))

				.add(yayasan == null || yayasan.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("siswa.yayasan", yayasan),
								Restrictions.eq("calonSiswa.yayasan", yayasan)))

				.add(selectedSekolah != null && selectedSekolah.getId() != null
						? Restrictions.or(Restrictions.eq("siswa.sekolah", selectedSekolah),
								Restrictions.eq("calonSiswa.sekolah", selectedSekolah))
						: Restrictions.sqlRestriction("true"))
				.add(selectedYayasan != null && selectedYayasan.getId() != null
						? Restrictions.or(Restrictions.eq("siswa.yayasan", selectedYayasan),
								Restrictions.eq("calonSiswa.yayasan", selectedYayasan))
						: Restrictions.sqlRestriction("true"));

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()));
		}
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakMahasiswa().isEmpty()) {
			criteria.add(Restrictions.in("mahasiswa.id", tbmuser.getOrangTua().ambilAnakMahasiswa()));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("biodataCalonMahasiswa.nama", searchnim.getValue(),
										MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("calonSiswa.noRegistrasi", searchnim.getValue(),
												MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("calonSiswa.namaSiswa", searchnim.getValue(),
														MatchMode.ANYWHERE),
												Restrictions.or(
														Restrictions.ilike("siswa.namaSiswa", searchnim.getValue(),
																MatchMode.ANYWHERE),
														Restrictions.or(
																Restrictions.ilike("siswa.nomorInduk",
																		searchnim.getValue(), MatchMode.ANYWHERE),
																Restrictions.or(Restrictions.ilike("mahasiswa.nama",
																		searchnim.getValue(), MatchMode.ANYWHERE),
																		Restrictions.or(
																				Restrictions.ilike("mahasiswa.nim",
																						searchnim.getValue(),
																						MatchMode.ANYWHERE),
																				Restrictions.or(Restrictions.ilike(
																						"biodataCalonMahasiswa.noRegistrasi",
																						searchnim.getValue(),
																						MatchMode.ANYWHERE),
																						Restrictions.ilike(
																								"biodataCalonMahasiswa.noUjian",
																								searchnim.getValue(),
																								MatchMode.ANYWHERE))))))))))

				.add((searchmulai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmulai.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("date(this_.tanggal_dirubah) >= date('"
								+ Common.databaseDateFormat.get().format(searchmulai.getValue()) + "')")))

				.add((searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsampai.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("date(this_.tanggal_dirubah) <= date('"
								+ Common.databaseDateFormat.get().format(searchsampai.getValue()) + "')")))

				.add(status.getSelectedItem() == null || status.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kodeStatus", status.getSelectedItem().getValue()))
				.add(tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", tahunAkademik.getSelectedItem().getValue().toString()))
				.add(searchtrxId.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("va", searchtrxId.getValue(), MatchMode.ANYWHERE),
								Restrictions.ilike("trxId", searchtrxId.getValue(), MatchMode.ANYWHERE)));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<BniRequest> bniRequest = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(bniRequest);
		grid.setRowRenderer(new BniRequestRenderer());
		grid.setModelCheckMobile(strset);

	}

}
