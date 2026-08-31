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
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.maintenance.MainAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataPegawaiBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPayroll;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.Tbmuser;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.payroll.DetailJenisShiftPegawai;
import ais.database.model.payroll.JenisShiftPegawai;
import ais.database.model.payroll.JenisShiftPunyaPegawai;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk jenis shift punya pegawai. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code JenisShiftPegawai jenisShiftPegawai},
 * {@code Paging paging}, {@code MyGrid grid}, {@code MyTextbox kode}, {@code MyTextbox nama};
 * inisialisasi/lifecycle ({@code initCriteria()}); pembacaan/pencarian ({@code loadData()}, {@code
 * onSearchDefault()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyDetail
 */
public class JenisShiftPunyaPegawaiAction extends MyDetail implements DataCriteria, DataSearchDefault {

	/**
	 *  
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private JenisShiftPegawai jenisShiftPegawai;
	private Paging paging;
	private MyGrid grid;

	public JenisShiftPunyaPegawaiAction(JenisShiftPegawai jenisShiftPegawai) {
		super();
		this.jenisShiftPegawai = jenisShiftPegawai;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(JenisShiftPunyaPegawaiAction.this);
				if (isOpen()) {
					display();
				}
			}
		});

	}

	class JenisShiftPunyaPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		Date sekarang = WaktuUtil.getDate();
		@SuppressWarnings("rawtypes")
		Collection pangkats = ConstantValues.ambilBerdasarClass(KenaikanPangkat.class).values();

		public JenisShiftPunyaPegawaiRenderer() {

		}

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object data) throws Exception {
			final JenisShiftPunyaPegawai jenisShiftPunyaPegawai = (JenisShiftPunyaPegawai) data;
			Pegawai pegawai = jenisShiftPunyaPegawai.getPegawai();
			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);
			Vbox a;
			(a = RevisiHelper.createNewRevisi(Pegawai.class, pegawai, pegawai.getNama())).setParent(arg0);

			new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(a);

			new Label(pegawai.getTipePegawai() == null ? "" : pegawai.getTipePegawai().getNama()).setParent(arg0);

			RevisiHelper
					.createNewRevisi(JenisShiftPunyaPegawai.class, jenisShiftPunyaPegawai,
							pegawai.getStatusPegawai() == null ? "" : pegawai.getStatusPegawai().getNama())
					.setParent(arg0);

			final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Abaikan Jarak");
			checkboxConfig.setParent(arg0);
			checkboxConfig.setChecked(jenisShiftPunyaPegawai.getAbaikanJarak());
			checkboxConfig.addEventListener(Events.ON_CHECK, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					jenisShiftPunyaPegawai.setAbaikanJarak(checkboxConfig.isChecked());
					Common.refreshUpdate(session, (jenisShiftPunyaPegawai));
				}
			});

			Session session = HibernateUtil.currentSession();
			List<DetailJenisShiftPegawai> detailJenisShiftPegawaiTemps = ConstantValues
					.simpleList(
							session.createCriteria(DetailJenisShiftPegawai.class).addOrder(Order.asc("ke"))
									.add(Restrictions.eq("jenisShiftPegawai", jenisShiftPegawai)),
							DetailJenisShiftPegawai.class);
			final Combobox detailJenisShiftPegawai = new Combobox();
			Comboitem comboitem = new Comboitem();
			comboitem.setLabel("Tidak Ditentukan");
			comboitem.setValue(null);
			detailJenisShiftPegawai.appendChild(comboitem);
			for (DetailJenisShiftPegawai jenisShiftPegawai : detailJenisShiftPegawaiTemps) {
				comboitem = new Comboitem();
				comboitem.setLabel(jenisShiftPegawai.getNama());
				comboitem.setValue(jenisShiftPegawai);
				detailJenisShiftPegawai.appendChild(comboitem);
			}
			Common.selectComboItem(detailJenisShiftPegawai, jenisShiftPunyaPegawai.getDetailJenisShiftPegawai());
			detailJenisShiftPegawai.setReadonly(true);
			detailJenisShiftPegawai.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					jenisShiftPunyaPegawai.setDetailJenisShiftPegawai(
							(DetailJenisShiftPegawai) (detailJenisShiftPegawai.getSelectedItem() == null ? null
									: detailJenisShiftPegawai.getSelectedItem().getValue()));
					Common.refreshUpdate(session, (jenisShiftPunyaPegawai));
				}
			});
			detailJenisShiftPegawai.setWidth("90%");
			detailJenisShiftPegawai.setParent(arg0);

			final MyTextbox keterangan = new MyTextbox(
					jenisShiftPunyaPegawai.getKeterangan() == null ? "" : jenisShiftPunyaPegawai.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setParent(arg0);

			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					jenisShiftPunyaPegawai.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (jenisShiftPunyaPegawai));
				}
			});

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
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

											Common.refreshDelete((jenisShiftPunyaPegawai));

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
			button.setParent(toolbar);
			toolbar.setParent(arg0);

		}
	}

	private MyTextbox kode;
	private MyTextbox nama;

	public Criteria initCriteria(boolean order) {

		Criterion critKode = Restrictions.sqlRestriction("false");
		if (!kode.getValue().trim().equals("")) {
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
		Criteria criteria = session.createCriteria(JenisShiftPunyaPegawai.class)

				.createAlias("pegawai", "pegawai").add(critKode).add(critNama)

				.add(Restrictions.eq("jenisShiftPegawai", jenisShiftPegawai));
		if (order)
			criteria.addOrder(Order.asc("pegawai.nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisShiftPunyaPegawai> jenisShiftPunyaPegawais = jenisShiftPegawai == null
				|| jenisShiftPegawai.getId() == null
						? new ArrayList<JenisShiftPunyaPegawai>()
						: initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
								.setFirstResult(
										Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
								.list();

		ListModel strset = new SimpleListModel(jenisShiftPunyaPegawais);
		grid.setRowRenderer(new JenisShiftPunyaPegawaiRenderer());
		grid.setModelCheckMobile(strset);
		grid.renderAll();
	}

	private void display() {

		Integer desktopHeight = 700;
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getUserId() != null) {
			desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
		}

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar Pegawai dan Simulasi Shift"));
		groupbox.setStyle("min-height:" + (desktopHeight * 1.2) + "px");
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		Toolbarbutton button = new MyToolbarbuttonConfig("Ambil Data Pegawai", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Pegawai> pegawais = ConstantValues.simpleList(session.createCriteria(JenisShiftPunyaPegawai.class)
						.add(Restrictions.isNull("pegawai")).setProjection(Projections.groupProperty("pegawai.id"))
						.add(Restrictions.eq("jenisShiftPegawai", jenisShiftPegawai)), Pegawai.class, false);

				AmbilDataPegawaiBanyak ambilDataPegawaiBanyak = new AmbilDataPegawaiBanyak(pegawais);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataPegawaiBanyak);
				ambilDataPegawaiBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Pegawai> pegawais = (List<Pegawai>) arg0.getData();
						Session session = HibernateUtil.currentSession();
						for (Pegawai pegawai : pegawais) {
							JenisShiftPunyaPegawai jenisShiftPunyaPegawai = new JenisShiftPunyaPegawai();
							jenisShiftPunyaPegawai.setPegawai(pegawai);
							jenisShiftPunyaPegawai.setKeterangan("");
							jenisShiftPunyaPegawai.setJenisShiftPegawai(jenisShiftPegawai);
							session.save(jenisShiftPunyaPegawai);
						}

						loadData(null);
					}
				});
				ambilDataPegawaiBanyak.setWidth("90%");
				ambilDataPegawaiBanyak.setHeight("97%");
				ambilDataPegawaiBanyak.setVisible(true);
				ambilDataPegawaiBanyak.onModal();
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
				loadData(arg0);
			}
		});

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
												"delete from payroll.jenis_shift_punya_pegawai where jenis_shift_pegawai = "
														+ jenisShiftPegawai.getId())
												.executeUpdate();

										loadData(event);
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

		MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Singkronkan", "/img/svg/check2.svg");
		toolbar.appendChild(cetakSksDosen);
		cetakSksDosen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {

						final Label label = new Label(ais.common.Common.getBahasaConfig("Proses singkronisasi shift"));
						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Jenis Shift Pegawai");

						new Thread(new Runnable() {

							@Override
							public void run() {
								try {
								List<JenisShiftPunyaPegawai> jenisShiftPunyaPegawais = ConstantValues
										.simpleList(initCriteria(true), JenisShiftPunyaPegawai.class);

								int i = 0;
								int size = jenisShiftPunyaPegawais.size();
								for (JenisShiftPunyaPegawai jenisShiftPunyaPegawai : jenisShiftPunyaPegawais) {

									String kunciPegawai = jenisShiftPunyaPegawai.getPegawai() == null
											? "id=" + jenisShiftPunyaPegawai.getId()
											: String.valueOf(jenisShiftPunyaPegawai.getPegawai());
									try {
									if (jenisShiftPunyaPegawai.getPegawai() != null) {
										if (label != null) {
											label.setValue("(" + (Common.numberFormat.get().format(i * 100.0 / size))
													+ " %) sinkronisasi data shift "
													+ jenisShiftPunyaPegawai.getPegawai().getNama() + " ..");
										}

										if (jenisShiftPunyaPegawai.getJenisShiftPegawai() != null
												&& jenisShiftPunyaPegawai.getJenisShiftPegawai()
														.getBerlakuMulai() != null) {
											Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
											calendar.setTime(
													jenisShiftPunyaPegawai.getJenisShiftPegawai().getBerlakuMulai());

											Calendar s = ais.ui.util.WaktuUtil.getCalendar();
											s.setTime(jenisShiftPunyaPegawai.getJenisShiftPegawai()
													.getBerlakuSampai() == null ? WaktuUtil.getDate()
															: jenisShiftPunyaPegawai.getJenisShiftPegawai()
																	.getBerlakuSampai());
											s.set(Calendar.DATE, s.get(Calendar.DATE) + 1);
											while (calendar.getTime().before(s.getTime())) {
												Session session = HibernateUtil.currentNativeSession();
												try {
													Date tanggal = calendar.getTime();
													calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);

													StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = CommonPayroll
															.getDefaultStatuskehadiranKaryawanHarian(tanggal,
																	jenisShiftPunyaPegawai.getPegawai(), null, null, "",
																	"", session, true);
													session.refresh(statuskehadiranKaryawanHarian);
													String hari = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];
													DetailJenisShiftPegawai jenis = CommonPayroll
															.getDetailJenisShiftPegawai(
																	jenisShiftPunyaPegawai.getPegawai(), null, null,
																	statuskehadiranKaryawanHarian
																			.ambilMasukjam() == null
																					? tanggal
																					: statuskehadiranKaryawanHarian
																							.ambilMasukjam(),
																	statuskehadiranKaryawanHarian.getTanggal(), hari,
																	statuskehadiranKaryawanHarian
																			.getLiburNasional() != null);

													statuskehadiranKaryawanHarian.setDetailJenisShiftPegawai(jenis);

													session.getTransaction().begin();
													Common.refreshSaveOrUpdate(session, statuskehadiranKaryawanHarian);
													session.getTransaction().commit();

													CommonPayroll.simpanDetail(session, statuskehadiranKaryawanHarian,
															true);

													// session.disconnect();
													if (session.isOpen()) {
														session.disconnect();
														session.close();
													}

												} catch (Exception e) {
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/detail/JenisShiftPunyaPegawaiAction.java:498");
												}
												HibernateUtil.closeSession();
											}
										}
									}
									laporan.catatBerhasil(i, kunciPegawai, "Sinkronisasi berhasil");
									} catch (Exception ePegawai) {
										Common.tampilErrorJikaAdmin(ePegawai);
										laporan.catatGagalDetail(i, kunciPegawai, ePegawai);
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

								Clients.showBusy(label.getValue());
								if (label.getValue().isEmpty()) {
									Clients.clearBusy();
									laporan.selesaikan(null);
									timer.detach();
								}

							}
						});
						timer.start();

					}
				});
			}
		});

		String[] contents = new String[] { "id", "pegawai", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisShiftPunyaPegawai.class, this, contents);
		toolbar.appendChild(cetakToolbarbutton);

		HashMap<String, Object> nilai = new HashMap<String, Object>();
		nilai.put("jenisShiftPegawai", jenisShiftPegawai);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisShiftPunyaPegawai.class, null, null, nilai,
				contents);
		toolbar.appendChild(upload);

		Tabbox tabbox = new Tabbox();
		tabbox.setStyle("min-height:" + desktopHeight + "px");
		tabbox.setParent(groupbox);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");
		tabbox.setStyle("min-height:" + (desktopHeight * 0.9) + "px");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Data Pegawai");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Simulasi Shift");
		tab2.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight(desktopHeight + "px");

		grid = new MyGrid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(tabpanel1);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jabatan");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Status");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Abaikan Jarak");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Shift");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ket.");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
		paging.setParent(groupbox);

		loadData(null);

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight(desktopHeight + "px");

		MyGrid grid = new MyGrid();
		grid.setMold("paging");
		grid.setPageSize(35);
		grid.setParent(tabpanel1);

		columns = new Columns();
		columns.setParent(grid);

		column = new Column();
		column.setParent(columns);
		column.setLabel("Hari / Tanggal");
		column.setWidth("30%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama Shift");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama Waktu");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Mulai");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Sampai");
		column.setWidth("10%");

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(jenisShiftPegawai.getBerlakuMulai());
		int jumlahHari = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

		Rows rows = new Rows();
		rows.setParent(grid);
		Session session = HibernateUtil.currentSession();
		for (int i = 1; i <= jumlahHari; i++) {

			List<DetailJenisShiftPegawai> detailJenisShiftPegawais = jenisShiftPegawai
					.getJumlahHariSamaDenganJumlahShift() ? null
							: CommonPayroll.shiftRotasiHari(session, calendar.getTime(), jenisShiftPegawai);
			if (detailJenisShiftPegawais == null) {
				detailJenisShiftPegawais = new ArrayList<DetailJenisShiftPegawai>();
				String hari = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];
				DetailJenisShiftPegawai detailJenisShiftPegawai = CommonPayroll.shiftDetail(session, calendar.getTime(),
						null, hari, false, null, jenisShiftPegawai);
				detailJenisShiftPegawais.add(detailJenisShiftPegawai);
			}

			for (DetailJenisShiftPegawai detailJenisShiftPegawai : detailJenisShiftPegawais) {
				Row row = new Row();
				row.setValign("top");
				row.setParent(rows);

				row.appendChild(new Label(Common.dateFormat6.get().format(calendar.getTime())));
				row.appendChild(new Label(detailJenisShiftPegawai == null ? "" : detailJenisShiftPegawai.getNama()));
				row.appendChild(new Label(detailJenisShiftPegawai == null ? ""
						: detailJenisShiftPegawai.getKhususBuatHariLibur() ? "Libur"
								: detailJenisShiftPegawai == null || detailJenisShiftPegawai.getWaktuShift() == null
										? ""
										: detailJenisShiftPegawai.getWaktuShift().getNama()));
				row.appendChild(new Label(
						detailJenisShiftPegawai == null || detailJenisShiftPegawai.getKhususBuatHariLibur() ? ""
								: Common.timeFormat.get().format(detailJenisShiftPegawai.getMulai())));
				row.appendChild(new Label(
						detailJenisShiftPegawai == null || detailJenisShiftPegawai.getKhususBuatHariLibur() ? ""
								: Common.timeFormat.get().format(detailJenisShiftPegawai.getSampai())));
			}

			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);

		}
	}

	@Override
	public void onSearchDefault(Event event) {
		loadData(null);
	}

}
