package ais.action.master.bsi;

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
import org.zkoss.zul.Vbox;

import ais.action.master.KegiatanTemporaryAction.DetailKegiatanTemporaryRenderer;
import ais.action.master.helper.RevisiBsiRequestHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.servlet.Bsiresponse;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.ItemBiaya;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Tbmuser;
import ais.database.model.bsi.BsiRequest;
import ais.database.model.bsi.BsiRequestDetail;
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

/**
 * Controller/action ZK untuk bsi request. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Paging paging},
 * {@code Textbox searchtrxId}, {@code Textbox searchnim}, {@code Combobox tahunAkademik}, {@code Combobox
 * status}, {@code MyToolbarbuttonConfig find}, {@code MyDatebox searchmulai}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class BsiRequestAction extends GenericAutowireComposer implements DataCriteria {

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

	public static TreeMap<String, String> statses = new TreeMap<String, String>();

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
		// if (session.getAttribute("usersTemp") == null ||
		// !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
		// session.removeAttribute("usersTemp");
		// Common.goLogoff();
		// return;
		// }

		statses.put("105", "Duplicate Billing ID");
		statses.put("997", "System is temporarily offline");
		statses.put("998", "\"Content-Type\" header not defined as it should be");
		statses.put("999", "Internal Error");

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
			siswa = (Siswa) HibernateUtil.currentSession().createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.idEq(Long.parseLong(ExecutionsCtrl.getCurrent().getParameter("siswa"))))
					.uniqueResult();
		}

		if (ExecutionsCtrl.getCurrent().getParameter("calon_siswa") != null) {
			selectedCalonSiswa = (CalonSiswa) HibernateUtil.currentSession().createCriteria(CalonSiswa.class).add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
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
				"keterangan", "pengurangan", "nilaiBiayaHarusDiBayars", "bsiResponse", "amount", "biayaAdministrasi");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Check Ulang Semua", "/img/svg/check2.svg");
		cetakSksDosen.setVisible(Common.bolehKonfigurasi("aktifkan_tombol_check_ulang_bsi"));
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
								List<BsiRequest> perkuliahans = initCriteria(true).setMaxResults(1000).list();

								int i = 0;
								int size = perkuliahans.size();

								for (BsiRequest bsiRequest : perkuliahans) {

									Session session = HibernateUtil.currentNativeSession();
									try {

										Siswa siswa = bsiRequest.getSiswa();
										String ipClient = (Common.getKonfigurasi("bsi_ip_client", "").getNilai());
										if (!ipClient.trim().isEmpty()) {
											ipClient = ipClient + "/BsiForwarder";
										}
										String strURL = !ipClient.trim().isEmpty() ? ipClient
												: (siswa != null && siswa.getSekolah() != null
														&& !siswa.getSekolah().getBsiGatewayUrl().isEmpty()
																? siswa.getSekolah().getBsiGatewayUrl()
																: (Common
																		.getKonfigurasi("bsi_gateway_url",
																				"https://apibeta.bsi-ecollection.com/")
																		.getNilai()));

										boolean tambahkanMerchanId = Common.bolehKonfigurasi("tambahkan_merchan_id_di_bsi");
										if (!tambahkanMerchanId) {
											strURL = Common.getKonfigurasi("bsi_inquiry_gateway_url",
													"https://billing-bpi.maja.id/bsi/inquiry/").getNilai();
										}

										if (!bsiRequest.getKegiatanTemporarys().isEmpty()) {
											bsiRequest.setHapusCicilanSebelumnya(true);
											bsiRequest.setCheckUlang(true);
											session.getTransaction().begin();
											Common.refreshUpdate(session, bsiRequest);
											session.getTransaction().commit();

											BsiBackandProsess.check(strURL, bsiRequest, session);

										} else {
											if (bsiRequest.getSiswa() == null && bsiRequest.getCalonSiswa() == null) {
												Kegiatan kegiatan = Bsiresponse.createKegiatan(bsiRequest, session);

												List<CicilanPembayaran> cicilanPembayarans = session
														.createCriteria(CicilanPembayaran.class)
														.add(Restrictions.isNotNull("itemBiaya"))
														.add(Restrictions.eq("kegiatan", kegiatan))
														.addOrder(Order.asc("tanggal")).addOrder(Order.asc("ke"))
														.list();

												for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
													int count = ((Number) session.createCriteria(BsiRequestDetail.class)
															.add(Restrictions.eq("bsiRequest", bsiRequest))
															.add(Restrictions.eq("pengaturanPembayaranBulanan",
																	cicilanPembayaran.getPengaturanPembayaranBulanan()))
															.setProjection(Projections.rowCount()).uniqueResult())
															.intValue();
													if (count == 0) {
														BsiRequestDetail bsiRequestDetail = new BsiRequestDetail();
														bsiRequestDetail.setBsiRequest(bsiRequest);
														bsiRequestDetail.setPengaturanPembayaranBulanan(
																cicilanPembayaran.getPengaturanPembayaranBulanan());

														PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
																.getPengaturanPembayaranBulanan();
														ItemBiaya itemBiaya = cicilanPembayaran.getItemBiaya();

														bsiRequestDetail.setIdCicilan(cicilanPembayaran == null ? null
																: cicilanPembayaran.getId());
														bsiRequestDetail.setPengaturanPembayaranBulanan(
																pengaturanPembayaranBulanan);
														bsiRequestDetail.setItemBiaya(itemBiaya);
														bsiRequestDetail
																.setKeterangan(cicilanPembayaran.getKeterangan());
														bsiRequestDetail.setNilai(cicilanPembayaran.getNilai());
														bsiRequestDetail.setTanggal(cicilanPembayaran.getTanggal());
														bsiRequestDetail.setKe(0);

														bsiRequestDetail.setDenda(cicilanPembayaran == null
																|| cicilanPembayaran.getId() == null ? null
																		: cicilanPembayaran.getDenda());
														bsiRequestDetail.setNilaiAsli(cicilanPembayaran == null
																|| cicilanPembayaran.getId() == null ? null
																		: cicilanPembayaran.getNilaiAsli());

														session.getTransaction().begin();
														Common.refreshSaveOrUpdate(session, bsiRequestDetail);
														session.getTransaction().commit();
													}
												}
											}
											session.refresh(bsiRequest);
											bsiRequest.setHapusCicilanSebelumnya(true);
											bsiRequest.setCheckUlang(true);
											session.getTransaction().begin();
											Common.refreshUpdate(session, bsiRequest);
											session.getTransaction().commit();

											BsiBackandProsess.check(strURL, bsiRequest, session);

										}
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}
									HibernateUtil.closeSession();
									if (label != null) {
										label.setValue("(" + (Common.numberFormat.get().format(i * 100.0 / size))
												+ " %) check ulang data  " + bsiRequest + " ..");
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
				RevisiBsiRequestHelper revisiHelper = new RevisiBsiRequestHelper(new EventListener() {

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

	}

	class BsiRequestRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BsiRequest bsiRequest = (BsiRequest) arg1;
			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						Common.clear(detail);

						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						groupbox.setParent(detail);
						MyGrid grid = new MyGrid();
						grid.setParent(groupbox);

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

						HibernateUtil.currentSession().refresh(bsiRequest);
						if (!bsiRequest.getKegiatanTemporarys().isEmpty()) {
							List<CicilanPembayaran> cicilanPembayarans = HibernateUtil.currentSession()
									.createCriteria(CicilanPembayaran.class)
									.add(Restrictions.in("kegiatanTemporary", bsiRequest.getKegiatanTemporarys()))
									.list();

							ListModel strset = new SimpleListModel(cicilanPembayarans);
							grid.setRowRenderer(new DetailKegiatanTemporaryRenderer());
							grid.setModelCheckMobile(strset);
						} else {

							List<BsiRequestDetail> bsiRequestDetails = HibernateUtil.currentSession()
									.createCriteria(BsiRequestDetail.class).add(Restrictions.isNull("idCicilan"))
									.add(Restrictions.eq("bsiRequest", bsiRequest)).list();
							for (final BsiRequestDetail bsiRequestDetail : bsiRequestDetails) {
								Row row = new Row();row.setValign("top");
								row.setParent(rows);

								if (bsiRequestDetail.getTagihan() != null) {
									Tagihan tagihan = bsiRequestDetail.getTagihan();
									String desc = tagihan.getId() + "-" + tagihan.getItemBiayaSekolah().getNama()
											+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
													? " (ke " + tagihan.getBayarKe() + ")"
													: "")
											+ (tagihan.getBulan() == null ? "" : ", bulan " + tagihan.getBulan())
											+ (tagihan.getTahun() == null ? "" : ", tahun " + tagihan.getTahun())
											+ ", ";

									row.appendChild(new Label(desc));
								} else {
									row.appendChild(new ais.ui.util.MyLabelConfig(bsiRequestDetail.getKeterangan()));
								}
								row.appendChild(new ais.ui.util.MyLabelConfig(
										Common.numberFormat.get().format(bsiRequestDetail.getNilai())));
							}
						}
					}
				}
			});

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			RevisiHelper.createNewRevisi(BsiRequest.class, bsiRequest, bsiRequest.getTrxId()).setParent(vbox);

			new Label(bsiRequest.getVa()).setParent(vbox);
			if (bsiRequest.getMahasiswa() != null) {
				new Label(bsiRequest.getMahasiswa().toString()).setParent(arg0);
			} else if (bsiRequest.getBiodataCalonMahasiswa() != null) {
				new Label(bsiRequest.getBiodataCalonMahasiswa().toString()).setParent(arg0);
			} else if (bsiRequest.getSiswa() != null) {
				new Label(bsiRequest.getSiswa().getNomorInduk() + "-" + bsiRequest.getSiswa().getNama())
						.setParent(arg0);
			} else if (bsiRequest.getCalonSiswa() != null) {
				new Label(bsiRequest.getCalonSiswa().getNomorInduk() + "-" + bsiRequest.getCalonSiswa().getNama())
						.setParent(arg0);
			}
			new Label(bsiRequest.getTanggal_dirubah() == null ? ""
					: Common.dateFormat3.get().format(bsiRequest.getTanggal_dirubah())).setParent(arg0);
			new Label(Common.numberFormat.get().format(bsiRequest.getAmount())).setParent(arg0);
			new Label(Common.numberFormat.get().format(bsiRequest.getBiayaAdministrasi())).setParent(arg0);
			new Label(bsiRequest.getJenisKegiatan() == null ? bsiRequest.getKeterangan()
					: bsiRequest.getJenisKegiatan().getNamaKegiatan()).setParent(arg0);
			new Label((bsiRequest.getTahunAkademik() == null ? "" : bsiRequest.getTahunAkademik())
					+ (bsiRequest.getSemester() == null ? "" : "-" + bsiRequest.getSemester())).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);

			new Label(bsiRequest.getStatus()).setParent(hbox);

			// Kolom aksi rapi (pola MahasiswaAction): tombol dibungkus kebab popup (⋯) via
			// UIHelper.buatBarisAksi. Induknya hbox (bukan Row) karena sel ini juga memuat
			// label status yang harus tetap tampil di luar popup.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyButtonConfig button = new MyButtonConfig("Cek Pembayaran");
			aksiButtons.add(button);
			button.setVisible(Common.bolehKonfigurasi("tampilkan_check_ulang_pembayaran_via_bsi"));
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.createDefaultTimer(new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event arg0) throws Exception {

							Siswa siswa = bsiRequest.getSiswa();
							String ipClient = (Common.getKonfigurasi("bsi_ip_client", "").getNilai());
							if (!ipClient.trim().isEmpty()) {
								ipClient = ipClient + "/BsiForwarder";
							}
							String strURL = !ipClient.trim().isEmpty() ? ipClient
									: (siswa != null && siswa.getSekolah() != null
											&& !siswa.getSekolah().getBsiGatewayUrl().isEmpty()
													? siswa.getSekolah().getBsiGatewayUrl()
													: (Common
															.getKonfigurasi("bsi_gateway_url",
																	"https://apibeta.bsi-ecollection.com/")
															.getNilai()));

							boolean tambahkanMerchanId = Common.bolehKonfigurasi("tambahkan_merchan_id_di_bsi");
							if (!tambahkanMerchanId) {
								strURL = Common.getKonfigurasi("bsi_inquiry_gateway_url",
										"https://billing-bpi.maja.id/bsi/inquiry/").getNilai();
							}

							Session session = HibernateUtil.currentNativeSession();

							session.refresh(bsiRequest);
							if (!bsiRequest.getKegiatanTemporarys().isEmpty()) {
								bsiRequest.setHapusCicilanSebelumnya(true);
								bsiRequest.setCheckUlang(true);
								session.getTransaction().begin();
								Common.refreshUpdate(session, bsiRequest);
								session.getTransaction().commit();

								JSONObject jsonObject = BsiBackandProsess.check(strURL, bsiRequest, session);
								HibernateUtil.closeSession();

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
								if (bsiRequest.getSiswa() == null && bsiRequest.getCalonSiswa() == null) {
									Kegiatan kegiatan = Bsiresponse.createKegiatan(bsiRequest, session);

									List<CicilanPembayaran> cicilanPembayarans = session
											.createCriteria(CicilanPembayaran.class)
											.add(Restrictions.isNotNull("itemBiaya"))
											.add(Restrictions.eq("kegiatan", kegiatan)).addOrder(Order.asc("tanggal"))
											.addOrder(Order.asc("ke")).list();

									for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
										int count = ((Number) session.createCriteria(BsiRequestDetail.class)
												.add(Restrictions.eq("bsiRequest", bsiRequest))
												.add(Restrictions.eq("pengaturanPembayaranBulanan",
														cicilanPembayaran.getPengaturanPembayaranBulanan()))
												.setProjection(Projections.rowCount()).uniqueResult()).intValue();
										if (count == 0) {
											BsiRequestDetail bsiRequestDetail = new BsiRequestDetail();
											bsiRequestDetail.setBsiRequest(bsiRequest);
											bsiRequestDetail.setPengaturanPembayaranBulanan(
													cicilanPembayaran.getPengaturanPembayaranBulanan());

											PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
													.getPengaturanPembayaranBulanan();
											ItemBiaya itemBiaya = cicilanPembayaran.getItemBiaya();

											bsiRequestDetail.setIdCicilan(
													cicilanPembayaran == null ? null : cicilanPembayaran.getId());
											bsiRequestDetail
													.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
											bsiRequestDetail.setItemBiaya(itemBiaya);
											bsiRequestDetail.setKeterangan(cicilanPembayaran.getKeterangan());
											bsiRequestDetail.setNilai(cicilanPembayaran.getNilai());
											bsiRequestDetail.setTanggal(cicilanPembayaran.getTanggal());
											bsiRequestDetail.setKe(0);

											bsiRequestDetail.setDenda(
													cicilanPembayaran == null || cicilanPembayaran.getId() == null
															? null
															: cicilanPembayaran.getDenda());
											bsiRequestDetail.setNilaiAsli(
													cicilanPembayaran == null || cicilanPembayaran.getId() == null
															? null
															: cicilanPembayaran.getNilaiAsli());

											session.getTransaction().begin();
											Common.refreshSaveOrUpdate(session, bsiRequestDetail);
											session.getTransaction().commit();
										}
									}
								}
								session.refresh(bsiRequest);
								bsiRequest.setHapusCicilanSebelumnya(true);
								bsiRequest.setCheckUlang(true);
								session.getTransaction().begin();
								Common.refreshUpdate(session, bsiRequest);
								session.getTransaction().commit();

								JSONObject jsonObject = BsiBackandProsess.check(strURL, bsiRequest, session);
								HibernateUtil.closeSession();

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

			if (Common.getApakahAdmin() && !bsiRequest.getStatus().equalsIgnoreCase("Payment Sukses")) {
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

												HibernateUtil.currentSession().refresh(bsiRequest);
												Common.refreshDelete(bsiRequest);

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

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(BsiRequest.class)
				.add(mahasiswa != null ? Restrictions.eq("mahasiswa", mahasiswa) : Restrictions.sqlRestriction("true"))
				.add(siswa != null ? Restrictions.eq("siswa", siswa) : Restrictions.sqlRestriction("true"))
				.add(selectedCalonSiswa != null ? Restrictions.eq("calonSiswa", selectedCalonSiswa)
						: Restrictions.sqlRestriction("true"))

				.createAlias("bsiResponse", "bsiResponse", Criteria.LEFT_JOIN)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
				.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)
				.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa", Criteria.LEFT_JOIN)

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

		List<BsiRequest> bsiRequest = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(bsiRequest);
		grid.setRowRenderer(new BsiRequestRenderer());
		grid.setModelCheckMobile(strset);

	}

}
