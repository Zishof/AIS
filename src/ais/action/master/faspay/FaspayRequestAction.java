package ais.action.master.faspay;

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

import ais.action.master.KegiatanTemporaryAction.DetailKegiatanTemporaryRenderer;
import ais.action.master.helper.RevisiHelper;
import ais.action.servlet.FasPayResponse;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.ItemBiaya;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Tbmuser;
import ais.database.model.faspay.FaspayRequest;
import ais.database.model.faspay.FaspayRequestDetail;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk faspay request. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Paging paging},
 * {@code Textbox searchtrxId}, {@code Textbox searchnim}, {@code Textbox searchKanal}, {@code Combobox
 * tahunAkademik}, {@code Combobox status}, {@code MyToolbarbuttonConfig find}; inisialisasi/lifecycle ({@code
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
public class FaspayRequestAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730217402400328L;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchtrxId;
	private Textbox searchnim;
	private Textbox searchKanal;
	private Combobox tahunAkademik;
	private Combobox status;

	private MyToolbarbuttonConfig find;

	private MyDatebox searchmulai;
	private MyDatebox searchsampai;

	private Tbmuser tbmuser = null;

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

		TreeMap<String, String> statses = new TreeMap<String, String>();
		statses.put("0", "Belum diproses");
		statses.put("1", "Dalam proses");
		statses.put("2", "Payment Sukses");
		statses.put("3", "Payment Gagal");
		statses.put("4", "Payment Reversal");
		statses.put("7", "Payment Expired");
		statses.put("8", "Payment Cancelled");
		statses.put("9", "Unknown");

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
		Common.generateTahunAjaranDanSemua(tahunAkademik);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Check Ulang Semua", "/img/svg/check2.svg");
		cetakSksDosen.setVisible(Common.bolehKonfigurasi("aktifkan_tombol_check_ulang_faspay"));
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
								List<FaspayRequest> perkuliahans = initCriteria(true).setMaxResults(1000).list();

								int i = 0;
								int size = perkuliahans.size();

								for (FaspayRequest faspayRequest : perkuliahans) {

									Session session = HibernateUtil.currentNativeSession();
									try {

										String strURL = (Common.getKonfigurasi("faspay_check_status_url",
												"http://faspaydev.mediaindonusa.com/pws/100004/183xx00010100000")
												.getNilai());

										if (!faspayRequest.getKegiatanTemporarys().isEmpty()) {
											faspayRequest.setHapusCicilanSebelumnya(true);
											faspayRequest.setCheckUlang(true);
											session.getTransaction().begin();
											Common.refreshUpdate(session, faspayRequest);
											session.getTransaction().commit();

											FaspayBackandProsess.check(strURL, faspayRequest, session);

										} else {

											Kegiatan kegiatan = FasPayResponse.createKegiatan(faspayRequest, session);

											List<CicilanPembayaran> cicilanPembayarans = session
													.createCriteria(CicilanPembayaran.class)
													.add(Restrictions.isNotNull("itemBiaya"))
													.add(Restrictions.eq("kegiatan", kegiatan))
													.addOrder(Order.asc("tanggal")).addOrder(Order.asc("ke")).list();

											for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
												int count = ((Number) session.createCriteria(FaspayRequestDetail.class)
														.add(Restrictions.eq("faspayRequest", faspayRequest))
														.add(Restrictions.eq("pengaturanPembayaranBulanan",
																cicilanPembayaran.getPengaturanPembayaranBulanan()))
														.setProjection(Projections.rowCount()).uniqueResult())
														.intValue();
												if (count == 0) {
													FaspayRequestDetail faspayRequestDetail = new FaspayRequestDetail();
													faspayRequestDetail.setFaspayRequest(faspayRequest);
													faspayRequestDetail.setPengaturanPembayaranBulanan(
															cicilanPembayaran.getPengaturanPembayaranBulanan());

													PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
															.getPengaturanPembayaranBulanan();
													ItemBiaya itemBiaya = cicilanPembayaran.getItemBiaya();

													faspayRequestDetail.setIdCicilan(cicilanPembayaran == null ? null
															: cicilanPembayaran.getId());
													faspayRequestDetail.setPengaturanPembayaranBulanan(
															pengaturanPembayaranBulanan);
													faspayRequestDetail.setItemBiaya(itemBiaya);
													faspayRequestDetail
															.setKeterangan(cicilanPembayaran.getKeterangan());
													faspayRequestDetail.setNilai(cicilanPembayaran.getNilai());
													faspayRequestDetail.setTanggal(cicilanPembayaran.getTanggal());
													faspayRequestDetail.setKe(0);

													faspayRequestDetail.setDenda(cicilanPembayaran == null
															|| cicilanPembayaran.getId() == null ? null
																	: cicilanPembayaran.getDenda());
													faspayRequestDetail.setNilaiAsli(cicilanPembayaran == null
															|| cicilanPembayaran.getId() == null ? null
																	: cicilanPembayaran.getNilaiAsli());

													session.getTransaction().begin();
													Common.refreshSaveOrUpdate(session, faspayRequestDetail);
													session.getTransaction().commit();
												}
											}

											session.refresh(faspayRequest);
											faspayRequest.setHapusCicilanSebelumnya(true);
											faspayRequest.setCheckUlang(true);
											session.getTransaction().begin();
											Common.refreshUpdate(session, faspayRequest);
											session.getTransaction().commit();

											FaspayBackandProsess.check(strURL, faspayRequest, session);

										}

										// session.disconnect();
										if (session.isOpen()) {session.disconnect();session.close();}
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}
									HibernateUtil.closeSession();
									if (label != null) {
										label.setValue("(" + (Common.numberFormat.get().format(i * 100.0 / size))
												+ " %) check ulang data  " + faspayRequest + " ..");
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

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "id", "nama", "url", "trxId", "merchant_id",
				"merchant", "response_code", "response_desc", "request", "response", "status", "kodeStatus",
				"mahasiswa", "biodataCalonMahasiswa", "jenisKegiatan", "jadwalPembayaran", "semester", "tahunAkademik",
				"keterangan", "pengurangan", "nilaiBiayaHarusDiBayars", "faspayResponse", "amount", "biayaAdministrasi",
				"payment_channel_name");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);
	}

	class FaspayRequestRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final FaspayRequest faspayRequest = (FaspayRequest) arg1;
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

						HibernateUtil.currentSession().refresh(faspayRequest);
						if (!faspayRequest.getKegiatanTemporarys().isEmpty()) {
							List<CicilanPembayaran> cicilanPembayarans = HibernateUtil.currentSession()
									.createCriteria(CicilanPembayaran.class)
									.add(Restrictions.in("kegiatanTemporary", faspayRequest.getKegiatanTemporarys()))
									.list();

							ListModel strset = new SimpleListModel(cicilanPembayarans);
							grid.setRowRenderer(new DetailKegiatanTemporaryRenderer());
							grid.setModelCheckMobile(strset);
						} else {

							List<FaspayRequestDetail> faspayRequestDetails = HibernateUtil.currentSession()
									.createCriteria(FaspayRequestDetail.class).add(Restrictions.isNull("idCicilan"))
									.add(Restrictions.eq("faspayRequest", faspayRequest)).list();

							for (FaspayRequestDetail faspayRequestDetail : faspayRequestDetails) {
								Row row = new Row();row.setValign("top");
								row.setParent(rows);
								RevisiHelper.createNewRevisi(FaspayRequestDetail.class, faspayRequestDetail,
										faspayRequestDetail.getKeterangan()).setParent(row);
								row.appendChild(new Label(Common.numberFormat.get().format(faspayRequestDetail.getNilai())));
							}
						}
					}
				}
			});

			RevisiHelper.createNewRevisi(FaspayRequest.class, faspayRequest, faspayRequest.getTrxId()).setParent(arg0);

			if (faspayRequest.getMahasiswa() != null) {
				new Label(faspayRequest.getMahasiswa().toString()).setParent(arg0);
			} else if (faspayRequest.getBiodataCalonMahasiswa() != null) {
				new Label(faspayRequest.getBiodataCalonMahasiswa().toString()).setParent(arg0);
			}
			try {
				new Label(faspayRequest.getTanggal_dirubah() == null ? ""
						: Common.dateFormat3.get().format(faspayRequest.getTanggal_dirubah())).setParent(arg0);
			} catch (Exception e) {
				new Label().setParent(arg0);
			}
			new Label(Common.numberFormat.get().format(faspayRequest.getAmount())).setParent(arg0);
			new Label(Common.numberFormat.get().format(faspayRequest.getBiayaAdministrasi())).setParent(arg0);
			new Label(
					faspayRequest.getJenisKegiatan() == null ? "" : faspayRequest.getJenisKegiatan().getNamaKegiatan())
					.setParent(arg0);
			new Label(faspayRequest.getPayment_channel_name()).setParent(arg0);
			new Label(faspayRequest.getTahunAkademik() + "-" + faspayRequest.getSemester()).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);

			new Label(faspayRequest.getStatus()).setParent(hbox);
			MyButtonConfig button = new MyButtonConfig("Cek Pembayaran");
			button.setParent(hbox);

			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.createDefaultTimer(new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event arg0) throws Exception {
							String strURL = (Common
									.getKonfigurasi("faspay_check_status_url",
											"http://faspaydev.mediaindonusa.com/pws/100004/183xx00010100000")
									.getNilai());
							Session session = HibernateUtil.currentNativeSession();

							session.refresh(faspayRequest);
							if (!faspayRequest.getKegiatanTemporarys().isEmpty()) {
								faspayRequest.setHapusCicilanSebelumnya(true);
								faspayRequest.setCheckUlang(true);
								session.getTransaction().begin();
								Common.refreshUpdate(session, faspayRequest);
								session.getTransaction().commit();

								JSONObject jsonObject = FaspayBackandProsess.check(strURL, faspayRequest, session);
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

								Kegiatan kegiatan = FasPayResponse.createKegiatan(faspayRequest, session);

								List<CicilanPembayaran> cicilanPembayarans = session
										.createCriteria(CicilanPembayaran.class)
										.add(Restrictions.isNotNull("itemBiaya"))
										.add(Restrictions.eq("kegiatan", kegiatan)).addOrder(Order.asc("tanggal"))
										.addOrder(Order.asc("ke")).list();

								for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
									int count = ((Number) session.createCriteria(FaspayRequestDetail.class)
											.add(Restrictions.eq("faspayRequest", faspayRequest))
											.add(Restrictions.eq("pengaturanPembayaranBulanan",
													cicilanPembayaran.getPengaturanPembayaranBulanan()))
											.setProjection(Projections.rowCount()).uniqueResult()).intValue();
									if (count == 0) {
										FaspayRequestDetail faspayRequestDetail = new FaspayRequestDetail();
										faspayRequestDetail.setFaspayRequest(faspayRequest);
										faspayRequestDetail.setPengaturanPembayaranBulanan(
												cicilanPembayaran.getPengaturanPembayaranBulanan());

										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
												.getPengaturanPembayaranBulanan();
										ItemBiaya itemBiaya = cicilanPembayaran.getItemBiaya();

										faspayRequestDetail.setIdCicilan(
												cicilanPembayaran == null ? null : cicilanPembayaran.getId());
										faspayRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
										faspayRequestDetail.setItemBiaya(itemBiaya);
										faspayRequestDetail.setKeterangan(cicilanPembayaran.getKeterangan());
										faspayRequestDetail.setNilai(cicilanPembayaran.getNilai());
										faspayRequestDetail.setTanggal(cicilanPembayaran.getTanggal());
										faspayRequestDetail.setKe(0);

										faspayRequestDetail.setDenda(
												cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
														: cicilanPembayaran.getDenda());
										faspayRequestDetail.setNilaiAsli(
												cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
														: cicilanPembayaran.getNilaiAsli());

										session.getTransaction().begin();
										Common.refreshSaveOrUpdate(session, faspayRequestDetail);
										session.getTransaction().commit();
									}
								}

								session.refresh(faspayRequest);
								faspayRequest.setHapusCicilanSebelumnya(true);
								faspayRequest.setCheckUlang(true);
								session.getTransaction().begin();
								Common.refreshUpdate(session, faspayRequest);
								session.getTransaction().commit();

								JSONObject jsonObject = FaspayBackandProsess.check(strURL, faspayRequest, session);
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

		}

	}

	public Criteria initCriteria(boolean order) {

		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(FaspayRequest.class)
				.add(mahasiswa != null ? Restrictions.eq("mahasiswa", mahasiswa) : Restrictions.sqlRestriction("true"))
				.createAlias("faspayResponse", "faspayResponse", Criteria.LEFT_JOIN)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa", Criteria.LEFT_JOIN);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("mahasiswa.nim", searchnim.getValue(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("biodataCalonMahasiswa.noRegistrasi", searchnim.getValue(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("biodataCalonMahasiswa.noUjian", searchnim.getValue(),
												MatchMode.ANYWHERE))))

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
						: Restrictions.ilike("trxId", searchtrxId.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchKanal.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("payment_channel_name", searchKanal.getValue().trim(),
								MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<FaspayRequest> faspayRequest = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(faspayRequest);
		grid.setRowRenderer(new FaspayRequestRenderer());
		grid.setModelCheckMobile(strset);

	}

}
