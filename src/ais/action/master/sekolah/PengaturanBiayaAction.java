package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
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
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataKelasSiswaSiswaBanyak;
import ais.action.master.sekolah.helper.DetailTagihanCalonSiswaHelper;
import ais.action.master.sekolah.helper.DetailTagihanSiswaHelper;
import ais.action.master.sekolah.helper.TagihanUtil;
import ais.action.master.sekolah.helper.TagihanUtilCalonSiswa;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.DataRecoveryHelper;
import ais.common.ProgressListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.sekolah.AsramaSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.ItemBiayaSekolah;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.PaketPsb;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.PengaturanBiayaItemBiaya;
import ais.database.model.sekolah.PenjurusanSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.StatusAwalSiswa;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

public class PengaturanBiayaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;
	private Textbox searchsiswa;
	private Combobox searchPenjurusan;
	private Combobox searchSatusAwal;
	private Combobox searchta;
	private Textbox searchkelas;
	private Decimalbox searchTahunAngkatan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox tahunAjaran;
	private Combobox sekolah;
	private Textbox keterangan;

	private Combobox bulanMulai;
	private Combobox bulanSampai;

	private boolean edit = false;
	private boolean approve = false;
	private boolean delete = false;

	private PengaturanBiaya pengaturanBiaya;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Combobox jenisBiayaSekolah;
	private Intbox tahunAngkatan;
	private List<Checkbox> selectedItemBiayaSekolah;
	private MyCheckboxConfig gunakanBiayaDefault;
	private Row rowItemBiaya;
	// PERMINTAAN: pencarian nama + paging 10/halaman utk checklist item biaya di bawah
	// (dulu satu daftar panjang semua ItemBiayaSekolah aktif milik sekolah terpilih, tanpa
	// pencarian/paging -- sama seperti pola yang sudah diperbaiki di SetingBiayaAction.java).
	// Field (bukan variabel lokal) krn dipakai bersama antara initMain() (yg membuat widget-
	// nya sekali) dan loadItemBiaya() (method terpisah yg dipanggil ulang tiap kombo
	// "Sekolah" berubah -- keduanya butuh state yg sama, persis alasan rowItemBiaya sendiri
	// juga sudah jadi field, bukan variabel lokal).
	private Textbox cariItemBiayaSekolah;
	private Paging pagingItemBiayaSekolah;
	private int halamanItemBiayaSekolah = 0;
	// PENTING (supaya paging AMAN, tidak diam-diam menghapus/melupakan perubahan user):
	// onSave() ((sekitar) baris "for (Checkbox checkbox : selectedItemBiayaSekolah)") hanya
	// memproses checkbox yang ADA di selectedItemBiayaSekolah saat "Simpan" diklik -- checkbox
	// yang TIDAK tercentang akan langsung DIHAPUS dari database. Sebelum paging ditambahkan,
	// SEMUA item selalu ada di satu halaman sehingga ini aman. Dengan paging, kalau
	// loadItemBiaya() membangun ulang objek/checkbox dari NOL setiap pindah halaman, status
	// centang & isian nominal yang diubah user di halaman lain akan HILANG saat "Simpan".
	// Makanya status per-item disimpan di 2 Map STABIL ini (key = ItemBiayaSekolah id),
	// TIDAK di-reset saat loadItemBiaya() dipanggil ulang krn pindah halaman/pencarian --
	// hanya di-reset saat combo "Sekolah" berganti (lihat eventListener "sekolah" di
	// initMain, sebab itu berarti daftar itemnya memang berganti total).
	private Map<Long, PengaturanBiayaItemBiaya> objekItemBiayaSekolahPerId = new HashMap<Long, PengaturanBiayaItemBiaya>();
	private Map<Long, Boolean> checkedItemBiayaSekolahPerId = new HashMap<Long, Boolean>();
	private Map<Long, Checkbox> checkboxItemBiayaSekolahPerId = new LinkedHashMap<Long, Checkbox>();
//	private MyCheckboxConfig bolehMemilihRincianPembayaran;
	private MyDatebox tanggalTagihan;

	private MyCheckboxConfig terdapatDenda;
	private MyIntbox tanggalDeadlineDenda;
	private MyDatebox deadlineTagihan;
	private MyCheckboxConfig dendaMengunakanPersen;
	private MyDoublebox denda;
	private MyCheckboxConfig tanggalTagihanMengikutiDefault;
	private Combobox penjurusanSekolah;

	private MyDatebox tanggalTagihanBulan1;
	private MyDatebox tanggalTagihanBulan2;
	private MyDatebox tanggalTagihanBulan3;
	private MyDatebox tanggalTagihanBulan4;
	private MyDatebox tanggalTagihanBulan5;
	private MyDatebox tanggalTagihanBulan6;
	private MyDatebox tanggalTagihanBulan7;
	private MyDatebox tanggalTagihanBulan8;
	private MyDatebox tanggalTagihanBulan9;
	private MyDatebox tanggalTagihanBulan10;
	private MyDatebox tanggalTagihanBulan11;
	private MyDatebox tanggalTagihanBulan12;
	private MyCheckboxConfig tanggalTagihanMengikutiBulanBerjalan;
	private MyCheckboxConfig khususBuatSiswaTertentu;
	private Combobox kelasSiswa;
	private Combobox kelasLesSiswa;
	private Combobox statusAwalSiswa;
	private MyCheckboxConfig terdapatBulanYangTidakAdaTagihannya;
	private Textbox bulanYangTidakAdaTagihannya;
	private Combobox gelombangPendaftaran;
	private Combobox paket;
	private MyCheckboxConfig aktifkanNotifikasi;
	private MyDatebox waktuNotifikasi;
	private Textbox templateNotifikasi;
	private MyDatebox batasWaktuPembayaran;
	private MyCheckboxConfig tampilanSemuaKelas;
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

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		tbmuser = Common.getCurrentUser();

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.generateTahunAjaranDanSemua(searchta);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "jenisBiayaSekolah", "tahunAngkatan", "kelasSiswa", "kelasLesSiswa",
				"gelombangPendaftaranPsb", "paketPsb", "statusAwalSiswa", "sekolah", "tahunAjaran",
				"gunakanBiayaDefault", "bulanMulai", "bulanSampai", "tanggalTagihanMengikutiDefault", "tanggalTagihan",
				"terdapatDenda", "tanggalDeadlineDenda", "deadlineTagihan", "dendaMengunakanPersen", "denda",
				"keterangan", "tanggalTagihanMengikutiBulanBerjalan", "tanggalTagihanBulan1", "tanggalTagihanBulan2",
				"tanggalTagihanBulan3", "tanggalTagihanBulan4", "tanggalTagihanBulan5", "tanggalTagihanBulan6",
				"tanggalTagihanBulan7", "tanggalTagihanBulan8", "tanggalTagihanBulan9", "tanggalTagihanBulan10",
				"tanggalTagihanBulan11", "tanggalTagihanBulan12", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PengaturanBiaya.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		if (upload.isVisible()) {
			
			
			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Singkronkan Semua Tagihan",
					"/img/Configure.png");
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(final Event arg0) throws Exception {

					final List<Long> pengaturans = initCriteria(true).setProjection(Projections.property("id")).list();

					if (pengaturans == null || pengaturans.isEmpty()) {
						MyMessageboxConfig.show("Tidak terdapat data pengaturan biaya yang dapat disinkronkan.", "Informasi", 
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					final int totalData = pengaturans.size();
					final String namaSiswaFilter = searchnama == null ? "" : searchnama.getValue();

					final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Semua Tagihan Pengaturan Biaya");

					// 1. Gunakan AtomicInteger agar thread-safe saat diakses oleh banyak thread bersamaan
					final java.util.concurrent.atomic.AtomicInteger completedCount = new java.util.concurrent.atomic.AtomicInteger(0);
					final java.util.concurrent.atomic.AtomicInteger barisLaporan = new java.util.concurrent.atomic.AtomicInteger(0);

					// 2. Buat UI Progress Bar (Window Modal)
					final org.zkoss.zul.Window progressWin = new org.zkoss.zul.Window("Proses Sinkronisasi Tagihan...", "normal", true);
					progressWin.setWidth("450px");
					progressWin.setPosition("center");
					progressWin.setClosable(false);
					progressWin.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()); // Tempelkan pada root terdekat

					org.zkoss.zul.Vbox vbox = new org.zkoss.zul.Vbox();
					vbox.setParent(progressWin);
					vbox.setWidth("100%");
					vbox.setAlign("center");
					vbox.setStyle("padding: 20px;");

					final org.zkoss.zul.Progressmeter pm = new org.zkoss.zul.Progressmeter();
					pm.setValue(0);
					pm.setWidth("100%");
					pm.setParent(vbox);

					final org.zkoss.zul.Label lblProgress = new org.zkoss.zul.Label("Memproses 0 dari " + totalData + " data (0%). Sisa: " + totalData);
					lblProgress.setParent(vbox);
					lblProgress.setStyle("margin-top: 10px; font-weight: bold;");

					// Label dummy (tidak di-attach ke UI) untuk menampung lemparan argumen ke method doSinkronkan
					// guna mencegah cross-thread UI Exception.
					final org.zkoss.zul.Label dummyLabel = new org.zkoss.zul.Label();

					// 3. Eksekusi Background Task (Maksimal 50 Thread secara paralel)
					int threadCount = Math.min(50, totalData);
					final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);

					for (final Long id : pengaturans) {
						executor.submit(new Runnable() {
							@Override
							public void run() {
								String kunci = "PengaturanBiaya id=" + id;
								try {
									PengaturanBiaya pengaturanBiaya = (PengaturanBiaya) ConstantValues
											.ambil(PengaturanBiaya.class.getName(), id);
									kunci = String.valueOf(pengaturanBiaya);

									if (pengaturanBiaya.getJenisBiayaSekolah().getGunakanCalonSiswa()) {
										TagihanUtilCalonSiswa.doSinkronkanTagihanCalonSiswa(pengaturanBiaya, dummyLabel, searchnama, true);
									} else {
										TagihanUtil.doSinkronkanTagihanSiswa(pengaturanBiaya, null, null, dummyLabel,
												namaSiswaFilter, true);
									}
									laporan.catatBerhasil(barisLaporan.getAndIncrement(), kunci, "Sinkronisasi berhasil");
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e); // Tampilkan di console agar tidak merusak flow Thread
									laporan.catatGagalDetail(barisLaporan.getAndIncrement(), kunci, e);
								} finally {
									// Tambahkan counter setiap kali ada 1 proses selesai, baik sukses maupun error
									completedCount.incrementAndGet();
								}
							}
						});
					}
					
					// Tutup penerimaan task baru
					executor.shutdown();

					// 4. UI Timer: Melakukan polling secara aman pada UI Thread untuk memperbarui progress bar
					final org.zkoss.zul.Timer timer = new org.zkoss.zul.Timer();
					timer.setDelay(300); // Polling setiap 300 ms
					timer.setRepeats(true);
					timer.setParent(progressWin);
					timer.addEventListener("onTimer", new EventListener() {
						@Override
						public void onEvent(Event eventTimer) throws Exception {
							int current = completedCount.get();
							int percent = (int) ((current * 100.0) / totalData);
							int remaining = totalData - current;

							pm.setValue(percent);
							lblProgress.setValue("Memproses " + current + " dari " + totalData + " data (" + percent + "%). Sisa: " + remaining);

							// Jika semua tugas sudah selesai
							if (current >= totalData) {
								timer.stop();
								progressWin.detach(); // Tutup window progress bar

								laporan.selesaikan(new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										// Refresh halaman atau panggil method default Anda
										onSearchDefault(arg0);
									}
								});
							}
						}
					});

					// Tampilkan Jendela Progress secara Modal (menahan interaksi user di belakangnya)
					progressWin.doModal();
				}

			});
			buttonTagihan.setParent(add.getParent());
			

			MyToolbarbuttonConfig buttonRecovery = new MyToolbarbuttonConfig("Recovery", "/img/Configure.png");
			buttonRecovery.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(final Event eventOri) throws Exception {

					// 1. Tampilkan Dialog Konfirmasi
					MyMessageboxConfig.show(
							"Apakah Anda yakin ingin melakukan Recovery data dari tabel audit? Tindakan ini akan memulihkan data yang hilang.",
							"Konfirmasi Recovery", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int response = Integer.parseInt(event.getData().toString());
									if (response == MyMessageboxConfig.OK) {

										final List<PengaturanBiaya> pengaturanBiayas = initCriteria(true).list();

										// OPTIMASI 1: Cegah eksekusi jika data kosong
										if (pengaturanBiayas == null || pengaturanBiayas.isEmpty()) {
											MyMessageboxConfig.show("Tidak terdapat data yang dapat diproses.", "Informasi",
													MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											return;
										}

										final List<String> warnings = java.util.Collections
												.synchronizedList(new ArrayList<String>());

										final org.zkoss.zk.ui.Desktop desktop = org.zkoss.zk.ui.Executions.getCurrent()
												.getDesktop();

										// OPTIMASI 2: Catat status awal ServerPush
										final boolean serverPushAlreadyEnabled = desktop.isServerPushEnabled();
										if (!serverPushAlreadyEnabled) {
											desktop.enableServerPush(true);
										}
										final String namaSiswa = searchsiswa.getValue().trim();
										final Label label = Common.displayLoadBar(new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												Common.createDefaultTimer(new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {

														if (!warnings.isEmpty()) {
															StringBuilder sb = new StringBuilder();
															synchronized (warnings) {
																for (String w : warnings) {
																	sb.append(w).append("\n");
																}
															}
															MyMessageboxConfig.show(sb.toString(), "Peringatan",
																	MyMessageboxConfig.OK,
																	MyMessageboxConfig.INFORMATION);
														}

														// OPTIMASI 3: Hanya refresh tampilan di sini (UI Thread)
														onSearchDefault(eventOri);
													}
												});
											}
										});

										new Thread(new Runnable() {
											@Override
											public void run() {
												try {
													// A. Eksekusi Query Recovery Berat
													DataRecoveryHelper.restoreDeletedDataFromAudit(pengaturanBiayas,
															namaSiswa, warnings, new ProgressListener() {
																@Override
																public void onProgress(final int percent,
																		final String message) {
																	try {
																		org.zkoss.zk.ui.Executions.schedule(desktop,
																				new EventListener() {
																					@Override
																					public void onEvent(Event event)
																							throws Exception {
																						if (label != null) {
																							label.setValue("Loading... "
																									+ percent + "% ("
																									+ message + ")");
																						}
																					}
																				}, null);
																	} catch (Exception e) {
																		ais.common.Common.tampilErrorJikaAdmin(e);
																	}
																}
															});

													// OPTIMASI 4: Pindahkan proses Reload Tagihan ke Background Thread!
													// Ini akan mencegah browser pengguna hang/freeze.
													int total = pengaturanBiayas.size();
													int count = 0;
													for (PengaturanBiaya pb : pengaturanBiayas) {
														count++;
														final int currentPercent = (int) (((double) count / total)
																* 100);

														// Update progress UI
														try {
															org.zkoss.zk.ui.Executions.schedule(desktop,
																	new EventListener() {
																		@Override
																		public void onEvent(Event event)
																				throws Exception {
																			if (label != null) {
																				label.setValue("Reloading Tagihan... "
																						+ currentPercent + "%");
																			}
																		}
																	}, null);
														} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

														// Eksekusi fungsi database di background
														PengaturanBiaya.reloadTagihan(pb, true);
													}

												} catch (Exception e) {
													warnings.add("Terjadi kesalahan saat mengeksekusi thread: "
															+ e.getMessage());
													ais.common.Common.tampilErrorJikaAdmin(e);
												} finally {
													try {
														org.zkoss.zk.ui.Executions.schedule(desktop,
																new EventListener() {
																	@Override
																	public void onEvent(Event event) throws Exception {
																		if (label != null) {
																			label.setValue(""); // Trigger penutupan
																								// loading bar
																		}

																		// OPTIMASI 5: Matikan ServerPush jika
																		// sebelumnya mati (Menghemat RAM Server)
																		if (!serverPushAlreadyEnabled
																				&& desktop.isServerPushEnabled()) {
																			desktop.enableServerPush(false);
																		}
																	}
																}, null);
													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}
												}
											}
										}).start();
									}
								}
							});
				}

			});
			buttonRecovery.setParent(add.getParent()); // Asumsi 'add' ada di scope Anda
		}

		EventListener eventListenerSekolah = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (searchPenjurusan != null) {
					Common.clear(searchPenjurusan);
					Sekolah s = (Sekolah) (searchsekolah.getSelectedItem() == null ? null
							: searchsekolah.getSelectedItem().getValue());
					System.out.println("s => " + s);

					searchPenjurusan.setReadonly(true);

					if (s != null && s.getId() != null) {
						try {
							HibernateUtil.currentSession().refresh(s);
							Set<PenjurusanSekolah> selectedPenjurusanSekolah = s.getPenjurusanSekolahs();
							for (PenjurusanSekolah o : selectedPenjurusanSekolah) {
								if (o.getAktif()) {
									Comboitem comboitem = new Comboitem();
									comboitem.setLabel(o.getNama());
									comboitem.setDescription(o.getKeterangan());
									comboitem.setValue(o);
									searchPenjurusan.appendChild(comboitem);
								}
							}

							Comboitem comboitem = new Comboitem();
							comboitem.setLabel("Semua Penjurusan");
							comboitem.setValue(null);
							searchPenjurusan.appendChild(comboitem);
							searchPenjurusan.setSelectedItem(comboitem);

							comboitem = new Comboitem();
							comboitem.setLabel("Belum Ditentukan Penjurusan");
							comboitem.setValue(new PenjurusanSekolah());
							searchPenjurusan.appendChild(comboitem);

							comboitem = new Comboitem();
							comboitem.setLabel("Sudah Ditentukan Penjurusan");
							comboitem.setValue(new PenjurusanSekolah(-1L, ""));
							searchPenjurusan.appendChild(comboitem);
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
					}
				}

			}
		};

		Common.insertComboDanSemua(searchSatusAwal, "nama", StatusAwalSiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (searchSatusAwal != null) { searchSatusAwal.setReadonly(true); }

		searchsekolah.addEventListener("onChange", eventListenerSekolah);
		Common.createDefaultTimer(eventListenerSekolah);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class PengaturanBiayaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PengaturanBiaya pengaturanBiaya = (PengaturanBiaya) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (detail.getChildren().isEmpty() && detail.isOpen()) {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (pengaturanBiaya.getJenisBiayaSekolah().getGunakanCalonSiswa()) {
									new DetailTagihanCalonSiswaHelper(null, edit, approve).display(pengaturanBiaya,
											detail);
								} else {
									new DetailTagihanSiswaHelper(null, edit, approve).display(pengaturanBiaya, detail);
								}
							}
						});
					}

				}

			});

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PengaturanBiaya.class, pengaturanBiaya,
					pengaturanBiaya.getJenisBiayaSekolah().getNama())).setParent(arg0);

			if (pengaturanBiaya.getGelombangPendaftaranPsb() != null) {
				new Label(pengaturanBiaya.getGelombangPendaftaranPsb().getNama()).setParent(a);
			}
			if (pengaturanBiaya.getPaketPsb() != null) {
				new Label(pengaturanBiaya.getPaketPsb().getNama()).setParent(a);
			}

			if (pengaturanBiaya.getAsramaSiswa() != null) {
				new Label(pengaturanBiaya.getAsramaSiswa().getNama()).setParent(a);
			}

			if (pengaturanBiaya.getKelasLesSiswa() != null) {
				new Label(pengaturanBiaya.getKelasLesSiswa().getNama()).setParent(a);
			} else {

				new Label(
						pengaturanBiaya
								.getKelasSiswa() != null
										? pengaturanBiaya.getKelasSiswa().getNama()
										: (pengaturanBiaya
												.getKhususBuatSiswaTertentu()
														? ""
														: (pengaturanBiaya.getTahunAngkatan().equals(0) ? ""
																: pengaturanBiaya.getTahunAngkatan()) + ""))
						.setParent(a);
				new Label((pengaturanBiaya.getTahunAjaran() == null || pengaturanBiaya.getTahunAjaran().trim().isEmpty()
						? ""
						: pengaturanBiaya.getTahunAjaran())
						+ (" " + (pengaturanBiaya.getStatusAwalSiswa() == null ? ""
								: pengaturanBiaya.getStatusAwalSiswa().getNama())))
						.setParent(a);
			}

			if (pengaturanBiaya.getKelasSiswa() == null && !pengaturanBiaya.getKelasBanyak().trim().isEmpty()) {
				new Label(pengaturanBiaya.getKelasBanyak()).setParent(a);
			}

			a = new Vbox();
			a.setParent(arg0);
			new Label(pengaturanBiaya.getSekolah() == null ? "" : pengaturanBiaya.getSekolah().getNama()).setParent(a);

			new Label(pengaturanBiaya.getPenjurusanSekolah() == null ? ""
					: pengaturanBiaya.getPenjurusanSekolah().getNama()).setParent(a);

//			new Label(pengaturanBiaya.getBolehMemilihRincianPembayaran() ? "Ya" : "Tidak").setParent(arg0);
			Session session = HibernateUtil.currentSession();
			List<PengaturanBiayaItemBiaya> selectedItemBiaya = ConstantValues
					.simpleList(
							session.createCriteria(PengaturanBiayaItemBiaya.class)
									.createAlias("itemBiayaSekolah", "itemBiayaSekolah")
									.add(Restrictions.or(Restrictions.isNull("itemBiayaSekolah.aktif"),
											Restrictions.eq("itemBiayaSekolah.aktif", true)))
									.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya)),
							PengaturanBiayaItemBiaya.class);
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			int i = 1;
			for (PengaturanBiayaItemBiaya itemBiaya : selectedItemBiaya) {
				vbox.appendChild(new MyLabelKecil(i + ". " + itemBiaya.getItemBiayaSekolah().getNama()
						+ (itemBiaya.getDefaultBiaya() > 0.1
								? " (Default : " + Common.numberFormat.get().format(itemBiaya.getDefaultBiaya()) + ")"
								: "")));
				i++;
			}
			selectedItemBiaya = null;

			int count = ((Number) session.createCriteria(Tagihan.class)
					.add(Restrictions.isNotNull("pembayaranSiswaDetail")).createAlias("nominalBiaya", "nominalBiaya")
					.add(Restrictions.eq("nominalBiaya.pengaturanBiaya", pengaturanBiaya))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			boolean buttonDelete = delete;
			if (Common.bolehKonfigurasi("pembayaran_siswa_yang_sudah_dibayar_tidak_bisa_dihapus")) {

				int countLagi = ((Number) session.createCriteria(GrupTransaksi.class).createAlias("tagihan", "tagihan")
						.createAlias("tagihan.nominalBiaya", "nominalBiaya")
						.add(Restrictions.eq("nominalBiaya.pengaturanBiaya", pengaturanBiaya))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();
				buttonDelete = delete && count == 0 && countLagi == 0;
			}

			new Label(pengaturanBiaya.getKeterangan() + "(jml pemb. " + Common.numberFormat.get().format(count) + ")")
					.setParent(arg0);

			vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(pengaturanBiaya.getBulanMulai() == null ? "" : pengaturanBiaya.getBulanMulai().toString())
					.setParent(vbox);
			new Label(pengaturanBiaya.getBulanSampai() == null ? ""
					: " sd " + pengaturanBiaya.getBulanSampai().toString()).setParent(vbox);

			new Label(!pengaturanBiaya.getTerdapatDenda() ? "Tidak ada"
					: (Common.numberFormat.get().format(pengaturanBiaya.getDenda())
							+ (pengaturanBiaya.getDendaMengunakanPersen() ? "%" : "")))
					.setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit && pengaturanBiaya.getKunci() == null);
			checkbox.setChecked(pengaturanBiaya.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pengaturanBiaya.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(pengaturanBiaya);
				}
			});

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dikumpulkan lalu dibungkus
			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			if (pengaturanBiaya.getKunci() == null) {
				Hbox tempCrud = Common.copyEditDeleteButtons(edit, buttonDelete, pengaturanBiaya,
						PengaturanBiayaAction.this);
				aksiButtons.addAll(ais.ui.util.UIHelper.ambilItemAksi(tempCrud));
			}

			if (pengaturanBiaya.getAktifkanNotifikasi()) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/check2-circle.svg");
				button.setTooltiptext("Kirimkan ulang notifikasi sekarang");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						try {
							Calendar cal = WaktuUtil.getCalendar();
							int bln = cal.get(Calendar.MONTH);
							int thn = cal.get(Calendar.YEAR);

							pengaturanBiaya.kirimTemplate(thn, bln);

							MyMessageboxConfig.show("Notifikasi telah berhasil dikirimkan.", "Informasi", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							ais.common.Common.tampilErrorJikaAdmin(e);
						}

					}

				});
				aksiButtons.add(button);
			}

			// Tombol Kunci / Buka Kunci via container sementara lalu dilipat ke daftar aksi.
			// tampilkanKunci membungkus tombolnya dalam Hbox dalam, jadi anak Box diratakan.
			Hbox tempKunci = new Hbox();
			PengaturanBiayaAction.tampilkanKunci(tempKunci, pengaturanBiaya, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			}, tbmuser);
			for (Object anakKunci : new java.util.ArrayList<Object>(tempKunci.getChildren())) {
				org.zkoss.zk.ui.Component compKunci = (org.zkoss.zk.ui.Component) anakKunci;
				if (compKunci instanceof org.zkoss.zul.Box) {
					aksiButtons.addAll(new java.util.ArrayList<org.zkoss.zk.ui.Component>(
							((org.zkoss.zul.Box) compKunci).getChildren()));
				} else {
					aksiButtons.add(compKunci);
				}
			}

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PengaturanBiaya());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pengaturanBiaya = (PengaturanBiaya) obj;

		init(pengaturanBiaya);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private Rows rowsSyarat;

	private Set<Long> idsSyarat = new HashSet<Long>();

	private EventListener ubahSyarat = new EventListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void onEvent(Event arg0) throws Exception {
			Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
			String ta = (String) tahunAjaran.getSelectedItem().getValue();

			Integer t = Integer.parseInt(StringUtils.split(ta, "/")[0]) - 1;
			String taMinSatu = t + "/" + (t + 1);

			Session session = HibernateUtil.currentSession();
			List<PengaturanBiaya> pengaturanBiayas = ConstantValues.simpleList(
					session.createCriteria(PengaturanBiaya.class).add(Restrictions.and(
							tampilanSemuaKelas.isChecked()
									? Restrictions.or(Restrictions.eq("tahunAjaran", ta),
											Restrictions.eq("tahunAjaran", taMinSatu))
									: Restrictions.eq("tahunAjaran", ta),
							Restrictions.and(
									Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))))
							.addOrder(Order.desc("id")),
					PengaturanBiaya.class);

			for (String syarat : pengaturanBiaya.getWajibDibayarSebelumnya().split(",")) {
				if (!syarat.trim().isEmpty()) {
					idsSyarat.add(Long.parseLong(syarat.trim()));
				}
			}

			Common.clear(rowsSyarat);

			for (final PengaturanBiaya pengaturanBiaya : pengaturanBiayas) {
				MyFormRow row = new MyFormRow();
				row.setParent(rowsSyarat);

				final Checkbox checkbox = new Checkbox(
						pengaturanBiaya.getJenisBiayaSekolah().getNama() + " " + pengaturanBiaya.getTahunAjaran() + " "
								+ (pengaturanBiaya.getKelasSiswa() == null ? ""
										: " kelas " + pengaturanBiaya.getKelasSiswa().getNama())
								+ (pengaturanBiaya.getTahunAngkatan().equals(0) ? ""
										: (pengaturanBiaya.getTahunAngkatan() > 1900
												? " tahun masuk " + pengaturanBiaya.getTahunAngkatan()
												: "")));
				checkbox.setChecked(idsSyarat.contains(pengaturanBiaya.getId()));

				row.appendChild(checkbox);
				checkbox.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (checkbox.isChecked()) {
							idsSyarat.add(pengaturanBiaya.getId());
						} else {
							idsSyarat.remove(pengaturanBiaya.getId());
						}
					}
				});
			}

		}
	};
	private MyCheckboxConfig otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion;
	private MyIntbox jumlahHariPenagihanBerikutnya;
	private MyDatebox tagihanKadaluarsa;
	private Combobox asramaSiswa;
	private MyCheckboxConfig tanpaAsrama;
	private Textbox bulanYangTidakAdaDendanya;
	private Textbox kelasBanyak;

	private void initSyarat(final PengaturanBiaya pengaturanBiaya, Tabpanel tabpanel) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);

		rowsSyarat = new Rows();
		rowsSyarat.setParent(grid);
	}

	private void initNotif(final PengaturanBiaya pengaturanBiaya, Tabpanel tabpanel) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(aktifkanNotifikasi = new MyCheckboxConfig(
				"Terdapat notifikasi/pemberitahuan tagihan ke orang tua / wali murid"));
		aktifkanNotifikasi.setChecked(pengaturanBiaya.getAktifkanNotifikasi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Tanggal, jam, dan menit kirim notifikasi/pemberitahuan tagihan"));
		row.appendChild(waktuNotifikasi = new MyDatebox(pengaturanBiaya.getWaktuNotifikasi()));
		waktuNotifikasi.setFormat(Common.dateFormat.get().toPattern());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Batas tanggal / paling lambat pembayaran tagihan"));
		row.appendChild(batasWaktuPembayaran = new MyDatebox(pengaturanBiaya.getBatasWaktuPembayaran()));

		Common.initKeterangan(rows,
				"Jika jenis pembayaran bulanan, maka notifikasi/pemberitahuan tagihan akan dikirimkan tiap tanggal dan waktu yang sesuai di tiap-tiap bulan-nya.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Template notifikasi/pemberitahuan tagihan"));
		row.appendChild(templateNotifikasi = new Textbox(pengaturanBiaya.getTemplateNotifikasi()));
		templateNotifikasi.setWidth("90%");
		templateNotifikasi.setRows(10);

		EventListener eventListeneraktifkanNotifikasi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				templateNotifikasi.setDisabled(!aktifkanNotifikasi.isChecked());
				waktuNotifikasi.setDisabled(!aktifkanNotifikasi.isChecked());
				batasWaktuPembayaran.setDisabled(!aktifkanNotifikasi.isChecked());
			}
		};

		aktifkanNotifikasi.addEventListener("onClick", eventListeneraktifkanNotifikasi);
		eventListeneraktifkanNotifikasi.onEvent(null);
	}

	@SuppressWarnings("deprecation")
	private void initMain(final PengaturanBiaya pengaturanBiaya, Tabpanel tabpanel) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		Sekolah selectedSekolah = SekolahUtil.getSekolah();
		if (selectedSekolah != null && selectedSekolah.getId() != null) {
			pengaturanBiaya.setYayasan(selectedSekolah.getYayasan());
			pengaturanBiaya.setSekolah(selectedSekolah);
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, pengaturanBiaya.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);

		// selectComboItem(yayasan,...) TIDAK memicu onChange, jadi daftar sekolah harus
		// dimuat eksplisit untuk yayasan terpilih agar combo Sekolah tidak kosong.
		ais.database.model.sekolah.Yayasan yayasanTerpilih = (yayasan.getSelectedItem() != null
				&& yayasan.getSelectedItem().getValue() instanceof ais.database.model.sekolah.Yayasan)
						? (ais.database.model.sekolah.Yayasan) yayasan.getSelectedItem().getValue()
						: pengaturanBiaya.getYayasan();
		Common.muatSekolahMilikYayasan(sekolah, yayasanTerpilih);

		Common.pilihSekolah(sekolah, pengaturanBiaya.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penjurusan"));
		row.appendChild(penjurusanSekolah = new Combobox());
		penjurusanSekolah.setWidth("90%");
		penjurusanSekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran / Biaya *"));
		row.appendChild(jenisBiayaSekolah = new Combobox());
		Common.insertCombo(jenisBiayaSekolah, new String[] { "kode", "nama", "sekolah", "periode" },
				JenisBiayaSekolah.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(true, jenisBiayaSekolah, pengaturanBiaya.getJenisBiayaSekolah());
		jenisBiayaSekolah.setWidth("90%");
		jenisBiayaSekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal *"));
		row.appendChild(statusAwalSiswa = new Combobox());
		Common.insertComboDanSemua(statusAwalSiswa, "nama", StatusAwalSiswa.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(statusAwalSiswa, pengaturanBiaya.getStatusAwalSiswa());
		statusAwalSiswa.setWidth("90%");
		statusAwalSiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
		Common.selectComboItem(true, tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
				pengaturanBiaya.getTahunAjaran());
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");

		tahunAngkatan = pengaturanBiaya.getTahunAngkatan().equals(0) ? new Intbox()
				: new Intbox(pengaturanBiaya.getTahunAngkatan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelasSiswa = new Combobox());
		kelasSiswa.setWidth("90%");
		kelasSiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Kelas (banyak kelas) (contoh: A,B,C arti-nya berlaku untuk kelas A, B, dan C)"));
		row.appendChild(kelasBanyak = new Textbox(pengaturanBiaya.getKelasBanyak()));
		kelasBanyak.setWidth("90%");
		kelasBanyak.setRows(2);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Kelas", "/img/user_male_add.png");

		final MyFormRow rowAmbilPengguna = new MyFormRow();
		rowAmbilPengguna.setParent(rows);
		rowAmbilPengguna.appendChild(new ais.ui.util.MyLabelConfig(""));
		rowAmbilPengguna.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataKelasSiswaSiswaBanyak ambil = new AmbilDataKelasSiswaSiswaBanyak(new ArrayList<KelasSiswa>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						List<KelasSiswa> kelasSiswas = (List<KelasSiswa>) arg0.getData();
						if (kelasSiswas != null && kelasSiswas.size() != 0) {
							for (KelasSiswa kelasSiswa : kelasSiswas) {
								kelasBanyak.setValue(kelasBanyak.getValue()
										+ (kelasBanyak.getValue().isEmpty() ? kelasSiswa.getNama()
												: "," + kelasSiswa.getNama()));
							}
						}

						JenisBiayaSekolah jenisBiayaSekolah = (JenisBiayaSekolah) (PengaturanBiayaAction.this.jenisBiayaSekolah
								.getSelectedItem() == null ? null
										: PengaturanBiayaAction.this.jenisBiayaSekolah.getSelectedItem().getValue());

						if (tahunAngkatan != null && tahunAngkatan.getParent() != null)
							tahunAngkatan.getParent()
									.setVisible((jenisBiayaSekolah == null || !jenisBiayaSekolah.getGunakanLes())
											&& kelasBanyak.getValue().trim().isEmpty()
											&& (kelasSiswa.getSelectedItem() == null
													|| kelasSiswa.getSelectedItem().getValue() == null));
					}
				});
				ambil.setWidth("950px");
				ambil.setHeight("400px");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas Les / Kursus"));
		row.appendChild(kelasLesSiswa = new Combobox());
		kelasLesSiswa.setWidth("90%");
		kelasLesSiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Asrama"));
		row.appendChild(asramaSiswa = new Combobox());
		asramaSiswa.setWidth("90%");
		asramaSiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tanpaAsrama = new MyCheckboxConfig("Hanya buat siswa yang tidak tinggal di dalam asrama"));
		tanpaAsrama.setChecked(pengaturanBiaya.getTanpaAsrama());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilanSemuaKelas = new MyCheckboxConfig("Tampilkan juga kelas di TA sebelumnya"));
		tampilanSemuaKelas.setChecked(pengaturanBiaya.getTampilanSemuaKelas());

		EventListener asramaEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				asramaSiswa.getParent().setVisible(!tanpaAsrama.isChecked());
			}
		};
		asramaEventListener.onEvent(null);
		tanpaAsrama.addEventListener("onClick", asramaEventListener);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang Pendaftaran *"));
		gelombangPendaftaran = new Combobox();
		row.appendChild(gelombangPendaftaran);
		gelombangPendaftaran.setWidth("90%");
		gelombangPendaftaran.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Paket *"));
		paket = new Combobox();
		row.appendChild(paket);
		paket.setWidth("90%");
		paket.setReadonly(true);

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				String ta = (String) tahunAjaran.getSelectedItem().getValue();

				Integer t = Integer.parseInt(StringUtils.split(ta, "/")[0]) - 1;
				String taMinSatu = t + "/" + (t + 1);

				System.out.println(s);

				if (jenisBiayaSekolah != null) {
					Common.insertCombo(jenisBiayaSekolah, new String[] { "kode", "nama", "sekolah", "periode" },
							JenisBiayaSekolah.class,
							Restrictions.and(
									Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
					Common.selectComboItem(jenisBiayaSekolah, pengaturanBiaya.getJenisBiayaSekolah());
				}

				if (kelasSiswa != null) {
					Common.insertComboDanSemua(kelasSiswa, new String[] { "nama", "tahunAjaran" }, "keterangan",
							KelasSiswa.class, "Semua Kelas",
							Restrictions.and(
									tampilanSemuaKelas.isChecked()
											? Restrictions.or(Restrictions.eq("tahunAjaran", ta),
													Restrictions.eq("tahunAjaran", taMinSatu))
											: Restrictions.eq("tahunAjaran", ta),
									Restrictions.and(
											Restrictions.or(Restrictions.isNull("sekolah"),
													Restrictions.eq("sekolah", s)),
											Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))));
					Common.selectComboItem(true, kelasSiswa, pengaturanBiaya.getKelasSiswa());
				}

				if (kelasBanyak != null) {
					kelasBanyak.getParent().setVisible(
							kelasSiswa.getSelectedItem() == null || kelasSiswa.getSelectedItem().getValue() == null);
					rowAmbilPengguna.setVisible(
							kelasSiswa.getSelectedItem() == null || kelasSiswa.getSelectedItem().getValue() == null);
				}

				if (asramaSiswa != null) {
					// Saat method ini dipanggil dari timer inisialisasi (lihat createDefaultTimer),
					// kombo "sekolah" bisa belum memiliki pilihan sehingga s == null. Pemanggilan
					// s.getYayasan() pada kondisi itu menyebabkan NullPointerException.
					// Hitung yayasan secara null-safe dan sesuaikan filter agar fungsi tetap jalan.
					Object yayasanFilter = (s == null ? null : s.getYayasan());
					Common.insertComboDanSemua(asramaSiswa, new String[] { "nama" }, "keterangan", AsramaSiswa.class,
							"Semua Asrama",
							Restrictions.and(
									yayasanFilter == null ? Restrictions.isNull("yayasan")
											: Restrictions.or(Restrictions.isNull("yayasan"),
													Restrictions.eq("yayasan", yayasanFilter)),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
					Common.selectComboItem(asramaSiswa, pengaturanBiaya.getAsramaSiswa());
				}

				if (kelasLesSiswa != null) {
					Common.insertCombo(kelasLesSiswa, new String[] { "nama" }, "keterangan", KelasLesSiswa.class,
							Restrictions.and(
									Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
					Common.selectComboItem(kelasLesSiswa, pengaturanBiaya.getKelasLesSiswa());
				}

				// Sekolah berganti -> daftar item biaya-nya memang berganti total, jadi cache
				// per-item (checked/nilai/checkbox) dan status pencarian+halaman WAJIB direset.
				// JANGAN reset ini di dalam loadItemBiaya() sendiri -- method itu juga dipanggil
				// ulang oleh kotak pencarian & paging, yang justru HARUS mempertahankan cache ini.
				objekItemBiayaSekolahPerId.clear();
				checkedItemBiayaSekolahPerId.clear();
				checkboxItemBiayaSekolahPerId.clear();
				halamanItemBiayaSekolah = 0;
				if (cariItemBiayaSekolah != null) {
					cariItemBiayaSekolah.setValue("");
				}
				loadItemBiaya(pengaturanBiaya);

				Common.createDefaultTimer(ubahSyarat);
			}
		};
		sekolah.addEventListener("onChange", eventListener);

		EventListener eventListenerKelas = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				String ta = (String) tahunAjaran.getSelectedItem().getValue();
				Integer t = Integer.parseInt(StringUtils.split(ta, "/")[0]) - 1;
				String taMinSatu = t + "/" + (t + 1);
				System.out.println(s);

				if (kelasSiswa != null) {
					Common.insertComboDanSemua(kelasSiswa, new String[] { "nama", "tahunAjaran" }, "keterangan",
							KelasSiswa.class, "Semua Kelas",
							Restrictions.and(
									tampilanSemuaKelas.isChecked()
											? Restrictions.or(Restrictions.eq("tahunAjaran", ta),
													Restrictions.eq("tahunAjaran", taMinSatu))
											: Restrictions.eq("tahunAjaran", ta),
									Restrictions.and(
											Restrictions.or(Restrictions.isNull("sekolah"),
													Restrictions.eq("sekolah", s)),
											Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))));
					Common.selectComboItem(true, kelasSiswa, pengaturanBiaya.getKelasSiswa());
				}

				if (kelasBanyak != null) {
					kelasBanyak.getParent().setVisible(
							kelasSiswa.getSelectedItem() == null || kelasSiswa.getSelectedItem().getValue() == null);
					rowAmbilPengguna.setVisible(
							kelasSiswa.getSelectedItem() == null || kelasSiswa.getSelectedItem().getValue() == null);
				}

				Common.createDefaultTimer(ubahSyarat);

			}
		};

		tampilanSemuaKelas.addEventListener("onClick", eventListenerKelas);
		tahunAjaran.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				String ta = (String) tahunAjaran.getSelectedItem().getValue();

				Common.insertComboDanSemua(kelasSiswa, new String[] { "nama", "tahunAjaran" }, "keterangan",
						KelasSiswa.class, "Semua Kelas",
						Restrictions.and(
								tampilanSemuaKelas.isChecked() ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("tahunAjaran", ta),
								Restrictions.and(
										Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
										Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))));
				Common.selectComboItem(kelasSiswa, pengaturanBiaya.getKelasSiswa());
				Common.createDefaultTimer(ubahSyarat);

				if (kelasBanyak != null) {
					kelasBanyak.getParent().setVisible(
							kelasSiswa.getSelectedItem() == null || kelasSiswa.getSelectedItem().getValue() == null);
					rowAmbilPengguna.setVisible(
							kelasSiswa.getSelectedItem() == null || kelasSiswa.getSelectedItem().getValue() == null);
				}
			}
		});

		if (kelasBanyak != null) {
			kelasBanyak.getParent().setVisible(
					kelasSiswa.getSelectedItem() == null || kelasSiswa.getSelectedItem().getValue() == null);
			rowAmbilPengguna.setVisible(
					kelasSiswa.getSelectedItem() == null || kelasSiswa.getSelectedItem().getValue() == null);

			kelasSiswa.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelasBanyak.getParent().setVisible(
							kelasSiswa.getSelectedItem() == null || kelasSiswa.getSelectedItem().getValue() == null);
					rowAmbilPengguna.setVisible(
							kelasSiswa.getSelectedItem() == null || kelasSiswa.getSelectedItem().getValue() == null);
				}
			});
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan / Masuk"));
		row.appendChild(tahunAngkatan);

		final EventListener mulaiDanSampai = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(PengaturanBiayaAction.this.bulanMulai);
				Common.clear(PengaturanBiayaAction.this.bulanSampai);
				PengaturanBiayaAction.this.bulanMulai.setSelectedItem(null);
				PengaturanBiayaAction.this.bulanSampai.setSelectedItem(null);
				JenisBiayaSekolah jenisBiayaSekolah = (JenisBiayaSekolah) (PengaturanBiayaAction.this.jenisBiayaSekolah
						.getSelectedItem() == null
						|| PengaturanBiayaAction.this.jenisBiayaSekolah.getSelectedItem().getValue() == null
								? pengaturanBiaya.getJenisBiayaSekolah()
								: PengaturanBiayaAction.this.jenisBiayaSekolah.getSelectedItem().getValue());

				if (jenisBiayaSekolah != null) {

					if (gelombangPendaftaran != null && gelombangPendaftaran.getParent() != null)
						gelombangPendaftaran.getParent().setVisible(jenisBiayaSekolah.getGelombangTertentu());

					if (paket != null && paket.getParent() != null)
						paket.getParent().setVisible(jenisBiayaSekolah.getPaketTertentu());

					if (kelasSiswa != null && kelasSiswa.getParent() != null)
						kelasSiswa.getParent().setVisible(!jenisBiayaSekolah.getGunakanLes());

					if (tahunAngkatan != null && tahunAngkatan.getParent() != null)
						tahunAngkatan.getParent().setVisible(!jenisBiayaSekolah.getGunakanLes()
								&& kelasBanyak.getValue().trim().isEmpty() && (kelasSiswa.getSelectedItem() == null
										|| kelasSiswa.getSelectedItem().getValue() == null));

					if (statusAwalSiswa != null && statusAwalSiswa.getParent() != null)
						statusAwalSiswa.getParent().setVisible(!jenisBiayaSekolah.getGunakanLes());

					if (kelasLesSiswa != null && kelasLesSiswa.getParent() != null)
						kelasLesSiswa.getParent().setVisible(jenisBiayaSekolah.getGunakanLes());
				}

				if (jenisBiayaSekolah != null) {
					if (bulanMulai != null && bulanMulai.getParent() != null)
						bulanMulai.getParent().setVisible(jenisBiayaSekolah.getPeriode().equalsIgnoreCase("Bulanan")
								|| jenisBiayaSekolah.getPeriode().equalsIgnoreCase("Harian"));

					if (bulanSampai != null && bulanSampai.getParent() != null)
						bulanSampai.getParent().setVisible(jenisBiayaSekolah.getPeriode().equalsIgnoreCase("Bulanan")
								|| jenisBiayaSekolah.getPeriode().equalsIgnoreCase("Harian"));

					if (tanggalTagihanMengikutiDefault != null && tanggalTagihanMengikutiDefault.getParent() != null)
						tanggalTagihanMengikutiDefault.getParent().setVisible(!bulanMulai.getParent().isVisible());

					if (tanggalTagihan != null && tanggalTagihan.getParent() != null)
						tanggalTagihan.getParent().setVisible(!bulanMulai.getParent().isVisible());
				}

				String ta = (String) (tahunAjaran.getSelectedItem() == null
						|| tahunAjaran.getSelectedItem().getValue() == null ? null
								: tahunAjaran.getSelectedItem().getValue());
				Integer tahunMasuk = ta == null ? null : Integer.parseInt(ta.split("/")[0]);

				if (jenisBiayaSekolah != null && tahunMasuk != null) {
					int mulai = 8;
					try {
						mulai = Integer.parseInt(Common.getKonfigurasi("bulan_mulai_tagihan", "8").getNilai());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/PengaturanBiayaAction.java:1393");
						// TODO: handle exception
					}

					if (jenisBiayaSekolah.getMulaiDitagihDiBulan() != null) {
						mulai = jenisBiayaSekolah.getMulaiDitagihDiBulan();
					}

					Calendar cal = ais.ui.util.WaktuUtil.getCalendar();

					Integer thn = ta == null ? cal.get(Calendar.YEAR) : Integer.parseInt(ta.split("/")[1]);
					Integer thnMulai = Integer.parseInt(ta.split("/")[0]);

					final int bulanTahunAkhir = PembayaranSiswa.convert(thn + 1, mulai);

					cal.set(Calendar.DATE, 1);
					cal.set(Calendar.MONTH, mulai - 1);
					cal.set(Calendar.YEAR, thnMulai);

					Integer pembayaranTerakhir = 0;
					while (bulanTahunAkhir > pembayaranTerakhir) {
						int tahunCurrent = cal.get(Calendar.YEAR);
						int bulanCurrent = cal.get(Calendar.MONTH);
						int bulanCurrentPlus = bulanCurrent + 1;
						pembayaranTerakhir = PembayaranSiswa.convert(tahunCurrent, bulanCurrentPlus);
						int tahun = Integer.parseInt((pembayaranTerakhir + "").substring(0, 4));
						int bulan = Integer.parseInt((pembayaranTerakhir + "").substring(4));
						if (bulan > 12 || bulan < 1) {
							continue;
						}

						Comboitem comboitem = new Comboitem(tahun + "-" + bulan);
						comboitem.setValue(pembayaranTerakhir);
						PengaturanBiayaAction.this.bulanMulai.appendChild(comboitem);

						comboitem = new Comboitem(tahun + "-" + bulan);
						comboitem.setValue(pembayaranTerakhir);
						PengaturanBiayaAction.this.bulanSampai.appendChild(comboitem);

						cal.add(Calendar.MONTH, 1);
					}

				}

				Common.selectComboItem(true, PengaturanBiayaAction.this.bulanMulai, pengaturanBiaya.getBulanMulai());
				Common.selectComboItem(true, PengaturanBiayaAction.this.bulanSampai, pengaturanBiaya.getBulanSampai());
			}
		};

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tagihan Mulai Di Bulan (khusus bulanan)"));
		row.appendChild(bulanMulai = new Combobox());
		bulanMulai.setWidth("90%");
		bulanMulai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tagihan Selesai Di Bulan (khusus bulanan)"));
		row.appendChild(bulanSampai = new Combobox());
		bulanSampai.setWidth("90%");
		bulanSampai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tanggalTagihanMengikutiBulanBerjalan = new MyCheckboxConfig(
				"Tanggal Tagihan Bulanan mengikuti bulan berjalan"));
		tanggalTagihanMengikutiBulanBerjalan.setChecked(pengaturanBiaya.getTanggalTagihanMengikutiBulanBerjalan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(terdapatBulanYangTidakAdaTagihannya = new MyCheckboxConfig(
				"Terdapat Bulan Tertentu Yang Tidak Ada Tagihannya"));
		terdapatBulanYangTidakAdaTagihannya.setChecked(pengaturanBiaya.getTerdapatBulanYangTidakAdaTagihannya());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Bulan Yang Tidak Ada Tagihannya (contoh: {KODE_ITEM_BIAYA}:1,{KODE_ITEM_BIAYA}:2,{KODE_ITEM_BIAYA}:3)"));
		row.appendChild(bulanYangTidakAdaTagihannya = new Textbox(pengaturanBiaya.getBulanYangTidakAdaTagihannya()));
		bulanYangTidakAdaTagihannya.setWidth("90%");
		bulanYangTidakAdaTagihannya.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan Bulan Juli"));
		row.appendChild(tanggalTagihanBulan7 = new MyDatebox(pengaturanBiaya.getTanggalTagihanBulan7()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan Bulan Agustus"));
		row.appendChild(tanggalTagihanBulan8 = new MyDatebox(pengaturanBiaya.getTanggalTagihanBulan8()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan Bulan September"));
		row.appendChild(tanggalTagihanBulan9 = new MyDatebox(pengaturanBiaya.getTanggalTagihanBulan9()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan Bulan Oktober"));
		row.appendChild(tanggalTagihanBulan10 = new MyDatebox(pengaturanBiaya.getTanggalTagihanBulan10()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan Bulan Nopember"));
		row.appendChild(tanggalTagihanBulan11 = new MyDatebox(pengaturanBiaya.getTanggalTagihanBulan11()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan Bulan Desember"));
		row.appendChild(tanggalTagihanBulan12 = new MyDatebox(pengaturanBiaya.getTanggalTagihanBulan12()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan Bulan Januari"));
		row.appendChild(tanggalTagihanBulan1 = new MyDatebox(pengaturanBiaya.getTanggalTagihanBulan1()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan Bulan Pebruari"));
		row.appendChild(tanggalTagihanBulan2 = new MyDatebox(pengaturanBiaya.getTanggalTagihanBulan2()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan Bulan Maret"));
		row.appendChild(tanggalTagihanBulan3 = new MyDatebox(pengaturanBiaya.getTanggalTagihanBulan3()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan Bulan April"));
		row.appendChild(tanggalTagihanBulan4 = new MyDatebox(pengaturanBiaya.getTanggalTagihanBulan4()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan Bulan Mei"));
		row.appendChild(tanggalTagihanBulan5 = new MyDatebox(pengaturanBiaya.getTanggalTagihanBulan5()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan Bulan Juni"));
		row.appendChild(tanggalTagihanBulan6 = new MyDatebox(pengaturanBiaya.getTanggalTagihanBulan6()));

		EventListener tanggalTagihanMengikutiBulanBerjalanListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				pengaturanBiaya
						.setTanggalTagihanMengikutiBulanBerjalan(tanggalTagihanMengikutiBulanBerjalan.isChecked());
				pengaturanBiaya.setTerdapatBulanYangTidakAdaTagihannya(terdapatBulanYangTidakAdaTagihannya.isChecked());

				tanggalTagihanBulan1.setValue(pengaturanBiaya.getTanggalTagihanBulan1());
				tanggalTagihanBulan2.setValue(pengaturanBiaya.getTanggalTagihanBulan2());
				tanggalTagihanBulan3.setValue(pengaturanBiaya.getTanggalTagihanBulan3());
				tanggalTagihanBulan4.setValue(pengaturanBiaya.getTanggalTagihanBulan4());
				tanggalTagihanBulan5.setValue(pengaturanBiaya.getTanggalTagihanBulan5());
				tanggalTagihanBulan6.setValue(pengaturanBiaya.getTanggalTagihanBulan6());
				tanggalTagihanBulan7.setValue(pengaturanBiaya.getTanggalTagihanBulan7());
				tanggalTagihanBulan8.setValue(pengaturanBiaya.getTanggalTagihanBulan8());
				tanggalTagihanBulan9.setValue(pengaturanBiaya.getTanggalTagihanBulan9());
				tanggalTagihanBulan10.setValue(pengaturanBiaya.getTanggalTagihanBulan10());
				tanggalTagihanBulan11.setValue(pengaturanBiaya.getTanggalTagihanBulan11());
				tanggalTagihanBulan12.setValue(pengaturanBiaya.getTanggalTagihanBulan12());

				tanggalTagihanBulan1.getParent().setVisible(!tanggalTagihanMengikutiBulanBerjalan.isChecked());
				tanggalTagihanBulan2.getParent().setVisible(!tanggalTagihanMengikutiBulanBerjalan.isChecked());
				tanggalTagihanBulan3.getParent().setVisible(!tanggalTagihanMengikutiBulanBerjalan.isChecked());
				tanggalTagihanBulan4.getParent().setVisible(!tanggalTagihanMengikutiBulanBerjalan.isChecked());
				tanggalTagihanBulan5.getParent().setVisible(!tanggalTagihanMengikutiBulanBerjalan.isChecked());
				tanggalTagihanBulan6.getParent().setVisible(!tanggalTagihanMengikutiBulanBerjalan.isChecked());
				tanggalTagihanBulan7.getParent().setVisible(!tanggalTagihanMengikutiBulanBerjalan.isChecked());
				tanggalTagihanBulan8.getParent().setVisible(!tanggalTagihanMengikutiBulanBerjalan.isChecked());
				tanggalTagihanBulan9.getParent().setVisible(!tanggalTagihanMengikutiBulanBerjalan.isChecked());
				tanggalTagihanBulan10.getParent().setVisible(!tanggalTagihanMengikutiBulanBerjalan.isChecked());
				tanggalTagihanBulan11.getParent().setVisible(!tanggalTagihanMengikutiBulanBerjalan.isChecked());
				tanggalTagihanBulan12.getParent().setVisible(!tanggalTagihanMengikutiBulanBerjalan.isChecked());

				bulanYangTidakAdaTagihannya.getParent().setVisible(terdapatBulanYangTidakAdaTagihannya.isChecked());

			}
		};

		tanggalTagihanMengikutiBulanBerjalan.addEventListener("onClick", tanggalTagihanMengikutiBulanBerjalanListener);
		terdapatBulanYangTidakAdaTagihannya.addEventListener("onClick", tanggalTagihanMengikutiBulanBerjalanListener);

		tanggalTagihanMengikutiBulanBerjalanListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan Default *"));
		row.appendChild(tanggalTagihan = new MyDatebox(pengaturanBiaya.getTanggalTagihan()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan Kadaluarsa"));
		row.appendChild(tagihanKadaluarsa = new MyDatebox(pengaturanBiaya.getTagihanKadaluarsa()));
		tagihanKadaluarsa.setReadonly(false);
		Common.initKeterangan(rows, "Kosongkan tanggal kadaluarsa jika tidak ada batas waktu kadaluarsa-nya");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				tanggalTagihanMengikutiDefault = new MyCheckboxConfig("Tanggal Denda Mengikuti Tanggal Default"));
		tanggalTagihanMengikutiDefault.setChecked(pengaturanBiaya.getTanggalTagihanMengikutiDefault());

		jenisBiayaSekolah.addEventListener("onChange", mulaiDanSampai);
		tahunAngkatan.addEventListener("onChange", mulaiDanSampai);
		tahunAjaran.addEventListener("onChange", mulaiDanSampai);
		kelasBanyak.addEventListener("onChange", mulaiDanSampai);
		kelasSiswa.addEventListener("onChange", mulaiDanSampai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(terdapatDenda = new MyCheckboxConfig("Terdapat Denda"));
		terdapatDenda.setChecked(pengaturanBiaya.getTerdapatDenda());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tiap Tanggal Deadline Denda"));
		row.appendChild(deadlineTagihan = new MyDatebox(pengaturanBiaya.getDeadlineTagihan()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Bulan Yang Tidak Ada Denda-nya (contoh: 202502,202503,202511 arti-nya tidak ada denda di bulan 2,3 dan 11 di tahun 2025)"));
		row.appendChild(bulanYangTidakAdaDendanya = new Textbox(pengaturanBiaya.getBulanYangTidakAdaDendanya()));
		bulanYangTidakAdaDendanya.setWidth("90%");
		bulanYangTidakAdaDendanya.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Deadline Denda"));
		row.appendChild(tanggalDeadlineDenda = new MyIntbox(pengaturanBiaya.getTanggalDeadlineDenda()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(dendaMengunakanPersen = new MyCheckboxConfig("Denda Menggunakan Persen"));
		dendaMengunakanPersen.setChecked(pengaturanBiaya.getDendaMengunakanPersen());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(khususBuatSiswaTertentu = new MyCheckboxConfig("Khusus buat siswa tertentu"));
		khususBuatSiswaTertentu.setChecked(pengaturanBiaya.getKhususBuatSiswaTertentu());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Denda"));
		row.appendChild(denda = new MyDoublebox(pengaturanBiaya.getDenda()));

		EventListener eventListenerDenda = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				JenisBiayaSekolah jenisBiayaSekolah = (JenisBiayaSekolah) (PengaturanBiayaAction.this.jenisBiayaSekolah
						.getSelectedItem() == null ? null
								: PengaturanBiayaAction.this.jenisBiayaSekolah.getSelectedItem().getValue());

				deadlineTagihan.getParent().setVisible(terdapatDenda.isChecked() && jenisBiayaSekolah != null
						&& !jenisBiayaSekolah.getPeriode().equalsIgnoreCase("Bulanan"));
				tanggalDeadlineDenda.getParent().setVisible(terdapatDenda.isChecked() && jenisBiayaSekolah != null
						&& jenisBiayaSekolah.getPeriode().equalsIgnoreCase("Bulanan"));

				bulanYangTidakAdaTagihannya.getParent().setVisible(terdapatDenda.isChecked()
						&& jenisBiayaSekolah != null && jenisBiayaSekolah.getPeriode().equalsIgnoreCase("Bulanan"));

				dendaMengunakanPersen.getParent().setVisible(terdapatDenda.isChecked());
				denda.getParent().setVisible(terdapatDenda.isChecked());
			}
		};

		terdapatDenda.addEventListener("onClick", eventListenerDenda);
		jenisBiayaSekolah.addEventListener("onChange", eventListenerDenda);
		eventListenerDenda.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion = new MyCheckboxConfig(
				"Otomatis Tertagih Jika Lebih Dari Sekian Waktu Atau Subscribtion"));
		otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion
				.setChecked(pengaturanBiaya.getOtomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Hari Penagihan Berikutnya"));
		row.appendChild(
				jumlahHariPenagihanBerikutnya = new MyIntbox(pengaturanBiaya.getJumlahHariPenagihanBerikutnya()));

		EventListener otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtionEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				JenisBiayaSekolah jenisBiayaSekolah = (JenisBiayaSekolah) (PengaturanBiayaAction.this.jenisBiayaSekolah
						.getSelectedItem() == null ? null
								: PengaturanBiayaAction.this.jenisBiayaSekolah.getSelectedItem().getValue());

				if (bulanMulai != null && bulanMulai.getParent() != null)
					bulanMulai.getParent()
							.setVisible(!otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion.isChecked()
									&& (jenisBiayaSekolah != null
											&& (jenisBiayaSekolah.getPeriode().equalsIgnoreCase("Bulanan")
													|| jenisBiayaSekolah.getPeriode().equalsIgnoreCase("Harian"))));

				if (bulanSampai != null && bulanSampai.getParent() != null)
					bulanSampai.getParent()
							.setVisible(!otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion.isChecked()
									&& (jenisBiayaSekolah != null
											&& (jenisBiayaSekolah.getPeriode().equalsIgnoreCase("Bulanan")
													|| jenisBiayaSekolah.getPeriode().equalsIgnoreCase("Harian"))));

				if (tanggalTagihanMengikutiDefault != null && tanggalTagihanMengikutiDefault.getParent() != null)
					tanggalTagihanMengikutiDefault.getParent()
							.setVisible(!otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion.isChecked()
									&& !bulanMulai.getParent().isVisible());

				if (tanggalTagihan != null && tanggalTagihan.getParent() != null)
					tanggalTagihan.getParent()
							.setVisible(!otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion.isChecked()
									&& !bulanMulai.getParent().isVisible());

				jumlahHariPenagihanBerikutnya.getParent()
						.setVisible(otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion.isChecked());
			}
		};

		otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion.addEventListener("onClick",
				otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtionEventListener);
		otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtionEventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(gunakanBiayaDefault = new MyCheckboxConfig("Gunakan Nilai Tagihan Default"));
		gunakanBiayaDefault.setChecked(pengaturanBiaya.getGunakanBiayaDefault());

		if (pengaturanBiaya.getId() != null) {
			HibernateUtil.currentSession().refresh(this.pengaturanBiaya);
		}

		// PERMINTAAN: pencarian nama item biaya + paging 10/halaman (lihat javadoc field
		// cariItemBiayaSekolah di atas). Baris pencarian ini dibuat sbg baris 2-kolom NORMAL
		// (label|isian) -- JANGAN pakai setSpans("2") di sini, karena baris ber-setSpans("2")
		// hanya boleh berisi SATU anak; pernah bikin widget tak tampil sama sekali saat
		// dicoba dgn 2 anak (Label+Textbox) di kasus serupa pada SetingBiayaAction.java.
		MyFormRow rowCariItemBiayaSekolah = new MyFormRow();
		rowCariItemBiayaSekolah.setParent(rows);
		rowCariItemBiayaSekolah.appendChild(new ais.ui.util.MyLabelConfig("Cari Item Biaya"));
		cariItemBiayaSekolah = new Textbox();
		cariItemBiayaSekolah.setWidth("90%");
		rowCariItemBiayaSekolah.appendChild(cariItemBiayaSekolah);
		cariItemBiayaSekolah.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				halamanItemBiayaSekolah = 0;
				loadItemBiaya(pengaturanBiaya);
			}
		});

		// Paging di baris tersendiri ber-setSpans("2") -- di baris ini HANYA satu anak
		// (widget Paging itu sendiri), jadi aman dipakai spans penuh.
		MyFormRow rowPagingItemBiayaSekolah = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowPagingItemBiayaSekolah, "2");
		rowPagingItemBiayaSekolah.setParent(rows);
		pagingItemBiayaSekolah = new Paging();
		pagingItemBiayaSekolah.setPageSize(10);
		pagingItemBiayaSekolah.setParent(rowPagingItemBiayaSekolah);
		pagingItemBiayaSekolah.addEventListener("onPaging", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				halamanItemBiayaSekolah = pagingItemBiayaSekolah.getActivePage();
				loadItemBiaya(pengaturanBiaya);
			}
		});

		rowItemBiaya = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowItemBiaya, "2");
		rowItemBiaya.setStyle("border:0px;background: transparent;");
		rowItemBiaya.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(pengaturanBiaya.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		EventListener eventListenerSekolah = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				penjurusanSekolah.getParent().setVisible(false);
				Common.clear(penjurusanSekolah);
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);

				if (s != null) {
					HibernateUtil.currentSession().refresh(s);
					Set<PenjurusanSekolah> selectedPenjurusanSekolah = s.getPenjurusanSekolahs();
					for (PenjurusanSekolah o : selectedPenjurusanSekolah) {
						if (o.getAktif()) {
							Comboitem comboitem = new Comboitem();
							comboitem.setLabel(o.getNama());
							comboitem.setDescription(o.getKeterangan());
							comboitem.setValue(o);
							penjurusanSekolah.appendChild(comboitem);
						}
					}

					Comboitem comboitem = new Comboitem();
					comboitem.setLabel("== Semua Penjurusan ==");
					comboitem.setDescription("== Semua Penjurusan ==");
					comboitem.setValue(null);
					penjurusanSekolah.appendChild(comboitem);

					penjurusanSekolah.getParent().setVisible(!selectedPenjurusanSekolah.isEmpty());
					Common.selectComboItem(penjurusanSekolah,
							PengaturanBiayaAction.this.pengaturanBiaya.getPenjurusanSekolah());

					String ta = (String) (tahunAjaran.getSelectedItem() == null
							|| tahunAjaran.getSelectedItem().getValue() == null ? null
									: tahunAjaran.getSelectedItem().getValue());
					Common.insertComboDanSemua(gelombangPendaftaran, new String[] { "nama", "tahunAjaran" },
							"informasi", GelombangPendaftaranPsb.class, "Pilih Gelombang Pendaftaran",
							Restrictions.and(Restrictions.eq("tahunAjaran", ta), Restrictions.and(
									Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
									Restrictions.eq("aktif", true))));
					Common.selectComboItem(true, gelombangPendaftaran,
							PengaturanBiayaAction.this.pengaturanBiaya.getGelombangPendaftaranPsb());

					Common.insertComboDanSemua(paket, new String[] { "nama" }, "keterangan", PaketPsb.class,
							"Pilih Paket",
							Restrictions.and(
									Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
									Restrictions.eq("aktif", true)));
					Common.selectComboItem(true, paket, PengaturanBiayaAction.this.pengaturanBiaya.getPaketPsb());

				}

			}
		};

		sekolah.addEventListener("onChange", eventListenerSekolah);
		tahunAjaran.addEventListener("onChange", eventListenerSekolah);
		Common.createDefaultTimer(eventListenerSekolah);

		final EventListener eventListener2 = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (tahunAngkatan != null && tahunAngkatan.getParent() != null) {
					KelasSiswa a = (KelasSiswa) (kelasSiswa.getSelectedItem() == null ? null
							: kelasSiswa.getSelectedItem().getValue());
					tahunAngkatan.getParent().setVisible(a == null && !khususBuatSiswaTertentu.isChecked());
				}
			}
		};

		khususBuatSiswaTertentu.addEventListener("onClick", eventListener2);
		kelasSiswa.addEventListener("onChange", eventListener2);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.createDefaultTimer(mulaiDanSampai);
				Common.createDefaultTimer(eventListener2);

				eventListener.onEvent(arg0);

				if (templateNotifikasi.getValue().trim().isEmpty()) {
					templateNotifikasi.setValue(pengaturanBiaya.refreshTemplate(selectedItemBiayaSekolah));
				}

				try {
					if (pengaturanBiaya.getId() != null) {
						Session session = HibernateUtil.currentSession();
						int count = ((Number) session.createCriteria(Tagihan.class)
								.add(Restrictions.isNotNull("pembayaranSiswaDetail"))
								.createAlias("nominalBiaya", "nominalBiaya")
								.add(Restrictions.eq("nominalBiaya.pengaturanBiaya", pengaturanBiaya))
								.setProjection(Projections.rowCount()).uniqueResult()).intValue();
						if (count > 0) {
							Common.freezeGanti(yayasan, sekolah, tahunAjaran, jenisBiayaSekolah);

						}

					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/PengaturanBiayaAction.java:1856");
					// TODO: handle exception
				}
			}
		});
	}

	@SuppressWarnings({})
	private void init(final PengaturanBiaya pengaturanBiaya) throws Exception {
		this.pengaturanBiaya = pengaturanBiaya;
		addWindow.setTitle(pengaturanBiaya.getId() == null ? "Tambah Tagihan Siswa" : "Ubah Tagihan Siswa");
		Common.clear(addWindow);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabBiaya = new MyTabConfig("Biaya");
		tabBiaya.setParent(tabs);

		MyTabConfig tabSyarat = new MyTabConfig("Syarat Bayar");
		tabSyarat.setParent(tabs);

		MyTabConfig tabNotifikasi = new MyTabConfig("Notifikasi Tagihan");
		tabNotifikasi.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelBiaya = new ais.ui.util.MyTabpanel();
		tabpanelBiaya.setParent(tabpanels);

		Tabpanel tabpanelSyarat = new ais.ui.util.MyTabpanel();
		tabpanelSyarat.setParent(tabpanels);

		Tabpanel tabpanelNotifikasi = new ais.ui.util.MyTabpanel();
		tabpanelNotifikasi.setParent(tabpanels);

		initNotif(pengaturanBiaya, tabpanelNotifikasi);

		initSyarat(pengaturanBiaya, tabpanelSyarat);

		initMain(pengaturanBiaya, tabpanelBiaya);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings("unchecked")
	private void loadItemBiaya(final PengaturanBiaya pengaturanBiaya) {
		Common.clear(rowItemBiaya);

		MyGrid vboxSkala = new MyGrid();
		vboxSkala.setParent(rowItemBiaya);

		Columns columns = new Columns();
		columns.setParent(vboxSkala);

		MyColumnConfig column = new MyColumnConfig("Kode/Nama Item Biaya");
		column.setParent(columns);
		column.setWidth("25%");

		column = new MyColumnConfig("Nominal Tagihan");
		column.setParent(columns);

		column = new MyColumnConfig("Minimal");
		column.setParent(columns);

		column = new MyColumnConfig("Maksimal");
		column.setParent(columns);

		column = new MyColumnConfig("Diskon Lunas 1x");
		column.setParent(columns);

		Rows rowsSkala = new Rows();
		rowsSkala.setParent(vboxSkala);

		Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
		Session session = HibernateUtil.currentSession();
		List<ItemBiayaSekolah> itemBiayaSekolahSemua = ConstantValues.simpleList(
				session.createCriteria(ItemBiayaSekolah.class).addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
						.add(s == null ? Restrictions.sqlRestriction("false") : Restrictions.eq("sekolah", s))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				ItemBiayaSekolah.class);

		// PERMINTAAN: pencarian nama/kode + paging 10/halaman -- lihat javadoc field
		// cariItemBiayaSekolah/objekItemBiayaSekolahPerId di atas soal kenapa status per-item
		// (checked/nilai) HARUS di-cache di Map stabil, bukan sekadar List<Checkbox> yang
		// dibangun ulang dari nol setiap kali method ini dipanggil (paging/pencarian akan
		// diam-diam "melupakan" perubahan user di halaman lain kalau tidak di-cache).
		String kataCariItemBiayaSekolah = cariItemBiayaSekolah == null || cariItemBiayaSekolah.getValue() == null ? ""
				: cariItemBiayaSekolah.getValue().trim().toLowerCase();
		List<ItemBiayaSekolah> itemBiayaSekolahCocok = new ArrayList<ItemBiayaSekolah>();
		for (ItemBiayaSekolah ib : itemBiayaSekolahSemua) {
			if (kataCariItemBiayaSekolah.isEmpty()
					|| (ib.getNama() != null && ib.getNama().toLowerCase().contains(kataCariItemBiayaSekolah))
					|| (ib.getKode() != null && ib.getKode().toLowerCase().contains(kataCariItemBiayaSekolah))) {
				itemBiayaSekolahCocok.add(ib);
			}
		}
		int totalHalamanItemBiayaSekolah = Math.max(1, (int) Math.ceil(itemBiayaSekolahCocok.size() / 10.0));
		if (halamanItemBiayaSekolah >= totalHalamanItemBiayaSekolah) {
			halamanItemBiayaSekolah = totalHalamanItemBiayaSekolah - 1;
		}
		if (halamanItemBiayaSekolah < 0) {
			halamanItemBiayaSekolah = 0;
		}
		if (pagingItemBiayaSekolah != null) {
			pagingItemBiayaSekolah.setTotalSize(itemBiayaSekolahCocok.size());
			pagingItemBiayaSekolah.setActivePage(halamanItemBiayaSekolah);
		}
		int mulaiItemBiayaSekolah = halamanItemBiayaSekolah * 10;
		int akhirItemBiayaSekolah = Math.min(mulaiItemBiayaSekolah + 10, itemBiayaSekolahCocok.size());
		List<ItemBiayaSekolah> itemBiayaSekolahs = mulaiItemBiayaSekolah >= akhirItemBiayaSekolah
				? new ArrayList<ItemBiayaSekolah>()
				: itemBiayaSekolahCocok.subList(mulaiItemBiayaSekolah, akhirItemBiayaSekolah);

		for (final ItemBiayaSekolah itemBiayaSekolah : itemBiayaSekolahs) {

			MyFormRow rowSkala = new MyFormRow();
			rowSkala.setStyle("border:0px;background: transparent;");
			rowSkala.setParent(rowsSkala);

			final PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya;
			final boolean checkedAwal;
			if (objekItemBiayaSekolahPerId.containsKey(itemBiayaSekolah.getId())) {
				// Item ini sudah pernah dirender sebelumnya dlm sesi dialog ini (mis. user
				// pindah halaman lalu balik lagi) -- PAKAI objek & status TERSIMPAN yg sama,
				// JANGAN query ulang dari database (supaya perubahan user tetap ada).
				pengaturanBiayaItemBiaya = objekItemBiayaSekolahPerId.get(itemBiayaSekolah.getId());
				checkedAwal = Boolean.TRUE.equals(checkedItemBiayaSekolahPerId.get(itemBiayaSekolah.getId()));
			} else {
				final PengaturanBiayaItemBiaya pengaturanBiayaItemBiayatemp;
				PengaturanBiayaItemBiaya pengaturanBiayaItemBiayatempCopy = null;
				if (pengaturanBiaya != null && pengaturanBiaya.getId() == null
						&& pengaturanBiaya.getCopyDari() != null) {
					pengaturanBiayaItemBiayatempCopy = (PengaturanBiayaItemBiaya) (pengaturanBiaya == null
							|| pengaturanBiaya.getId() == null
									? null
									: ConstantValues.simpleObject(
											session.createCriteria(PengaturanBiayaItemBiaya.class)
													.createAlias("itemBiayaSekolah", "itemBiayaSekolah")
													.add(Restrictions.or(Restrictions.isNull("itemBiayaSekolah.aktif"),
															Restrictions.eq("itemBiayaSekolah.aktif", true)))
													.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya.getCopyDari()))
													.setMaxResults(1)
													.add(Restrictions.eq("itemBiayaSekolah", itemBiayaSekolah)),
											PengaturanBiayaItemBiaya.class));

					System.out.println("pengaturanBiayaItemBiayatempCopy -> " + pengaturanBiayaItemBiayatempCopy);

					if (pengaturanBiayaItemBiayatempCopy != null) {
						pengaturanBiayaItemBiayatemp = new PengaturanBiayaItemBiaya();
						pengaturanBiayaItemBiayatemp.setId(null);
						pengaturanBiayaItemBiayatemp.setItemBiayaSekolah(itemBiayaSekolah);
						pengaturanBiayaItemBiayatemp.setPengaturanBiaya(pengaturanBiaya);
						pengaturanBiayaItemBiayatemp.setDefaultBiaya(pengaturanBiayaItemBiayatempCopy.getDefaultBiaya());
						pengaturanBiayaItemBiayatemp.setDiskonBiaya(pengaturanBiayaItemBiayatempCopy.getDiskonBiaya());
						pengaturanBiayaItemBiayatemp.setMinimalBiaya(pengaturanBiayaItemBiayatempCopy.getMinimalBiaya());
						pengaturanBiayaItemBiayatemp.setMaksimalBiaya(pengaturanBiayaItemBiayatempCopy.getMaksimalBiaya());
					} else {
						pengaturanBiayaItemBiayatemp = null;
					}

				} else {
					pengaturanBiayaItemBiayatemp = (PengaturanBiayaItemBiaya) (pengaturanBiaya == null
							|| pengaturanBiaya.getId() == null
									? null
									: ConstantValues.simpleObject(
											session.createCriteria(PengaturanBiayaItemBiaya.class)
													.createAlias("itemBiayaSekolah", "itemBiayaSekolah")
													.add(Restrictions.or(Restrictions.isNull("itemBiayaSekolah.aktif"),
															Restrictions.eq("itemBiayaSekolah.aktif", true)))
													.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya))
													.setMaxResults(1)
													.add(Restrictions.eq("itemBiayaSekolah", itemBiayaSekolah)),
											PengaturanBiayaItemBiaya.class));
				}

				if (pengaturanBiayaItemBiayatemp == null) {
					pengaturanBiayaItemBiaya = new PengaturanBiayaItemBiaya();
					pengaturanBiayaItemBiaya.setItemBiayaSekolah(itemBiayaSekolah);
					pengaturanBiayaItemBiaya.setPengaturanBiaya(pengaturanBiaya);
				} else {
					pengaturanBiayaItemBiaya = pengaturanBiayaItemBiayatemp;
				}

				checkedAwal = (pengaturanBiayaItemBiaya.getId() != null) || pengaturanBiayaItemBiayatempCopy != null;
				objekItemBiayaSekolahPerId.put(itemBiayaSekolah.getId(), pengaturanBiayaItemBiaya);
				checkedItemBiayaSekolahPerId.put(itemBiayaSekolah.getId(), checkedAwal);
			}

			final MyDoublebox defaultTagihan = new MyDoublebox(pengaturanBiayaItemBiaya.getDefaultBiaya());

			final MyDoublebox minimalBiaya = new MyDoublebox(pengaturanBiayaItemBiaya.getMinimalBiaya());
			final MyDoublebox maksimalBiaya = new MyDoublebox(pengaturanBiayaItemBiaya.getMaksimalBiaya());
			final MyDoublebox diskonBiaya = new MyDoublebox(pengaturanBiayaItemBiaya.getDiskonBiaya());

			final Checkbox checkbox = new Checkbox(itemBiayaSekolah.getKode() + " " + itemBiayaSekolah.getNama());
			checkbox.setAttribute("pengaturanBiayaItemBiaya", pengaturanBiayaItemBiaya);
			checkbox.setParent(rowSkala);
			checkbox.setChecked(checkedAwal);
			checkboxItemBiayaSekolahPerId.put(itemBiayaSekolah.getId(), checkbox);
			gunakanBiayaDefault.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					defaultTagihan.setDisabled(!gunakanBiayaDefault.isChecked() || !checkbox.isChecked());
					minimalBiaya.setDisabled(!gunakanBiayaDefault.isChecked() || !checkbox.isChecked());
					maksimalBiaya.setDisabled(!gunakanBiayaDefault.isChecked() || !checkbox.isChecked());
				}
			});

			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// PENTING utk paging aman: status centang harus disimpan ke Map stabil di
					// sini, bukan cuma hidup di widget checkbox yg akan dibuang saat pindah
					// halaman.
					checkedItemBiayaSekolahPerId.put(itemBiayaSekolah.getId(), checkbox.isChecked());

					defaultTagihan.setDisabled(!gunakanBiayaDefault.isChecked() || !checkbox.isChecked());
					minimalBiaya.setDisabled(!gunakanBiayaDefault.isChecked() || !checkbox.isChecked());
					maksimalBiaya.setDisabled(!gunakanBiayaDefault.isChecked() || !checkbox.isChecked());

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							templateNotifikasi.setValue(pengaturanBiaya.refreshTemplate(selectedItemBiayaSekolah));
						}
					});
				}
			});

			defaultTagihan.setParent(rowSkala);
			defaultTagihan.setWidth("90%");
			defaultTagihan.setDisabled(!gunakanBiayaDefault.isChecked() || !checkbox.isChecked());
			defaultTagihan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (checkbox.isChecked()) {
						pengaturanBiayaItemBiaya.setDefaultBiaya(defaultTagihan.getValue());
					}
					checkbox.setAttribute("pengaturanBiayaItemBiaya", pengaturanBiayaItemBiaya);
				}
			});

			minimalBiaya.setParent(rowSkala);
			minimalBiaya.setWidth("90%");
			minimalBiaya.setDisabled(!gunakanBiayaDefault.isChecked() || !checkbox.isChecked());
			minimalBiaya.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (checkbox.isChecked()) {
						pengaturanBiayaItemBiaya.setMinimalBiaya(minimalBiaya.getValue());
					}
					checkbox.setAttribute("pengaturanBiayaItemBiaya", pengaturanBiayaItemBiaya);
				}
			});

			maksimalBiaya.setParent(rowSkala);
			maksimalBiaya.setWidth("90%");
			maksimalBiaya.setDisabled(!gunakanBiayaDefault.isChecked() || !checkbox.isChecked());
			maksimalBiaya.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (checkbox.isChecked()) {
						pengaturanBiayaItemBiaya.setMaksimalBiaya(maksimalBiaya.getValue());
					}
					checkbox.setAttribute("pengaturanBiayaItemBiaya", pengaturanBiayaItemBiaya);
				}
			});

			diskonBiaya.setParent(rowSkala);
			diskonBiaya.setWidth("90%");
			diskonBiaya.setDisabled(!checkbox.isChecked());
			diskonBiaya.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (checkbox.isChecked()) {
						pengaturanBiayaItemBiaya.setDiskonBiaya(diskonBiaya.getValue());
					}
					checkbox.setAttribute("pengaturanBiayaItemBiaya", pengaturanBiayaItemBiaya);
				}
			});

		}

		// selectedItemBiayaSekolah dipakai onSave()/refreshTemplate() -- dibangun dari cache
		// checkboxItemBiayaSekolahPerId (mencakup SEMUA item yg pernah dirender di sesi dialog
		// ini, lintas halaman, bukan cuma yg lagi tampil di halaman ini) supaya perubahan user
		// di halaman lain tidak hilang saat "Simpan" diklik.
		selectedItemBiayaSekolah = new ArrayList<Checkbox>(checkboxItemBiayaSekolahPerId.values());
	}

	public boolean onSave(Event event) throws Exception {

		if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Yayasan belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Yayasan; (2) pilih yayasan yang sesuai; (3) ulangi proses setelah yayasan dipilih.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sekolah belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Sekolah; (2) pilih sekolah yang sesuai; (3) ulangi proses setelah sekolah dipilih.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisBiayaSekolah.getSelectedItem() == null || jenisBiayaSekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Nama Jenis Pembayaran / Biaya belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Jenis Pembayaran / Biaya; (2) pilih jenis pembayaran atau biaya yang sesuai; (3) ulangi proses setelah pilihan terisi.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (gelombangPendaftaran != null && gelombangPendaftaran.getParent() != null
				&& gelombangPendaftaran.getParent().isVisible() && (gelombangPendaftaran.getSelectedItem() == null
						|| gelombangPendaftaran.getSelectedItem().getValue() == null)) {
			MyMessageboxConfig.show("Gelombang belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Gelombang; (2) pilih gelombang yang sesuai; (3) ulangi proses setelah gelombang dipilih.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (paket != null && paket.getParent() != null && paket.getParent().isVisible()
				&& (paket.getSelectedItem() == null || paket.getSelectedItem().getValue() == null)) {
			MyMessageboxConfig.show("Paket belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Paket; (2) pilih paket yang sesuai; (3) ulangi proses setelah paket dipilih.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (aktifkanNotifikasi.isChecked() && waktuNotifikasi.getValue() == null) {
			MyMessageboxConfig.show("Tanggal dan waktu pengiriman notifikasi belum diisi. Langkah yang dapat dilakukan: (1) isi tanggal dan waktu pengiriman notifikasi; (2) pastikan waktunya sesuai dengan jadwal yang diinginkan; (3) ulangi penyimpanan setelah data terisi.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (aktifkanNotifikasi.isChecked() && batasWaktuPembayaran.getValue() == null) {
			MyMessageboxConfig.show("Batas tanggal atau tanggal paling lambat pembayaran belum diisi. Langkah yang dapat dilakukan: (1) isi batas tanggal pembayaran; (2) pastikan tanggalnya sesuai ketentuan; (3) ulangi penyimpanan setelah data terisi.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

//		KelasSiswa a = (KelasSiswa) (kelasSiswa.getSelectedItem() == null ? null
//				: kelasSiswa.getSelectedItem().getValue());
//		if (a == null) {
//			if (tahunAngkatan.getValue() == null) {
//				MyMessageboxConfig.show("Tahun Angkatan / tahun masuk harus diisi", "Peringatan", MyMessageboxConfig.OK,
//						MyMessageboxConfig.INFORMATION);
//				return false;
//			}
//		}
		if (tahunAjaran.getValue() == null) {
			MyMessageboxConfig.show("Tahun ajaran masuk belum diisi. Langkah yang dapat dilakukan: (1) isi atau pilih tahun ajaran masuk; (2) pastikan tahun ajaran sesuai; (3) ulangi penyimpanan setelah data terisi.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		JenisBiayaSekolah j = (JenisBiayaSekolah) jenisBiayaSekolah.getSelectedItem().getValue();
		if (j.getGunakanLes()) {
			if (kelasLesSiswa.getSelectedItem() == null || kelasLesSiswa.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Kelas les / kursus belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar pilihan Kelas les / kursus; (2) pilih kelas les atau kursus yang sesuai; (3) ulangi proses setelah pilihan terisi.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}

		}

		Session session = HibernateUtil.currentSession();
		if (pengaturanBiaya.getId() != null) {
			pengaturanBiaya = (PengaturanBiaya) session.load(PengaturanBiaya.class, pengaturanBiaya.getId());

		}
		pengaturanBiaya.setKelasBanyak(kelasBanyak.getValue());
		pengaturanBiaya.setBulanYangTidakAdaDendanya(bulanYangTidakAdaDendanya.getValue());
		pengaturanBiaya.setKelasLesSiswa((KelasLesSiswa) (kelasLesSiswa.getSelectedItem() == null ? null
				: kelasLesSiswa.getSelectedItem().getValue()));
		pengaturanBiaya.setJenisBiayaSekolah(j);
		pengaturanBiaya.setTahunAngkatan(tahunAngkatan.getValue());
		pengaturanBiaya.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		pengaturanBiaya.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		pengaturanBiaya.setKeterangan(keterangan.getValue());
		pengaturanBiaya.setGunakanBiayaDefault(gunakanBiayaDefault.isChecked());
		pengaturanBiaya.setTahunAjaran(
				(String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue()));
		pengaturanBiaya.setBulanMulai(
				(Integer) (bulanMulai.getSelectedItem() == null ? null : bulanMulai.getSelectedItem().getValue()));
		pengaturanBiaya.setBulanSampai(
				(Integer) (bulanSampai.getSelectedItem() == null ? null : bulanSampai.getSelectedItem().getValue()));
		pengaturanBiaya.setTanggalTagihan(tanggalTagihan.getValue());

		pengaturanBiaya.setTerdapatDenda(terdapatDenda.isChecked());
		pengaturanBiaya.setTanggalDeadlineDenda(tanggalDeadlineDenda.getValue());
		pengaturanBiaya.setDeadlineTagihan(deadlineTagihan.getValue());
		pengaturanBiaya.setDendaMengunakanPersen(dendaMengunakanPersen.isChecked());
		pengaturanBiaya.setDenda(denda.getValue());
		pengaturanBiaya.setTanggalTagihanMengikutiDefault(tanggalTagihanMengikutiDefault.isChecked());
		pengaturanBiaya.setPenjurusanSekolah((PenjurusanSekolah) (penjurusanSekolah.getSelectedItem() == null ? null
				: penjurusanSekolah.getSelectedItem().getValue()));

		pengaturanBiaya.setTanggalTagihanMengikutiBulanBerjalan(tanggalTagihanMengikutiBulanBerjalan.isChecked());
		pengaturanBiaya.setTerdapatBulanYangTidakAdaTagihannya(terdapatBulanYangTidakAdaTagihannya.isChecked());

		pengaturanBiaya.setTanggalTagihanBulan1(tanggalTagihanBulan1.getValue());
		pengaturanBiaya.setTanggalTagihanBulan2(tanggalTagihanBulan2.getValue());
		pengaturanBiaya.setTanggalTagihanBulan3(tanggalTagihanBulan3.getValue());
		pengaturanBiaya.setTanggalTagihanBulan4(tanggalTagihanBulan4.getValue());
		pengaturanBiaya.setTanggalTagihanBulan5(tanggalTagihanBulan5.getValue());
		pengaturanBiaya.setTanggalTagihanBulan6(tanggalTagihanBulan6.getValue());
		pengaturanBiaya.setTanggalTagihanBulan7(tanggalTagihanBulan7.getValue());
		pengaturanBiaya.setTanggalTagihanBulan8(tanggalTagihanBulan8.getValue());
		pengaturanBiaya.setTanggalTagihanBulan9(tanggalTagihanBulan9.getValue());
		pengaturanBiaya.setTanggalTagihanBulan10(tanggalTagihanBulan10.getValue());
		pengaturanBiaya.setTanggalTagihanBulan11(tanggalTagihanBulan11.getValue());
		pengaturanBiaya.setTanggalTagihanBulan12(tanggalTagihanBulan12.getValue());

		pengaturanBiaya.setBulanYangTidakAdaTagihannya(bulanYangTidakAdaTagihannya.getValue().trim());

		pengaturanBiaya.setKhususBuatSiswaTertentu(khususBuatSiswaTertentu.isChecked());

		pengaturanBiaya.setStatusAwalSiswa((StatusAwalSiswa) (statusAwalSiswa.getSelectedItem() == null ? null
				: statusAwalSiswa.getSelectedItem().getValue()));

		pengaturanBiaya.setKelasSiswa(
				(KelasSiswa) (kelasSiswa.getSelectedItem() == null ? null : kelasSiswa.getSelectedItem().getValue()));

		pengaturanBiaya.setTanpaAsrama(tanpaAsrama.isChecked());

		pengaturanBiaya.setAsramaSiswa(tanpaAsrama.isChecked() ? null
				: (AsramaSiswa) (asramaSiswa.getSelectedItem() == null ? null
						: asramaSiswa.getSelectedItem().getValue()));

		pengaturanBiaya.setGelombangPendaftaranPsb(
				gelombangPendaftaran == null || gelombangPendaftaran.getSelectedItem() == null ? null
						: (GelombangPendaftaranPsb) gelombangPendaftaran.getSelectedItem().getValue());

		pengaturanBiaya.setPaketPsb(paket == null || paket.getSelectedItem() == null ? null
				: (PaketPsb) paket.getSelectedItem().getValue());

		pengaturanBiaya.setAktifkanNotifikasi(aktifkanNotifikasi.isChecked());
		pengaturanBiaya.setWaktuNotifikasi(waktuNotifikasi.getValue());
		pengaturanBiaya.setTemplateNotifikasi(templateNotifikasi.getValue());
		pengaturanBiaya.setBatasWaktuPembayaran(batasWaktuPembayaran.getValue());
		pengaturanBiaya.setTampilanSemuaKelas(tampilanSemuaKelas.isChecked());

		pengaturanBiaya.setOtomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion(
				otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion.isChecked());
		pengaturanBiaya.setJumlahHariPenagihanBerikutnya(jumlahHariPenagihanBerikutnya.getValue());

		pengaturanBiaya.setOtomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion(
				otomatisTertagihJikaLebihDariSekianWaktuAtauSubscribtion.isChecked());

		pengaturanBiaya.setTagihanKadaluarsa(tagihanKadaluarsa.getValue());

		String s = "";
		for (Long id : idsSyarat) {
			s += s.isEmpty() ? id + "" : "," + id;
		}
		pengaturanBiaya.setWajibDibayarSebelumnya(s);

		Common.refreshSaveOrUpdate(session, pengaturanBiaya);

		for (Checkbox checkbox : selectedItemBiayaSekolah) {
			PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya = (PengaturanBiayaItemBiaya) checkbox
					.getAttribute("pengaturanBiayaItemBiaya");
			if (checkbox.isChecked()) {

				pengaturanBiayaItemBiaya.setPengaturanBiaya(pengaturanBiaya);
				session.saveOrUpdate(pengaturanBiayaItemBiaya);
				session.flush();
			} else {
				try {
					session.createSQLQuery("delete from sekolah.pengaturan_biaya_item_biaya where pengaturan_biaya_id="
							+ pengaturanBiaya.getId() + " and item_biaya_sekolah_id="
							+ pengaturanBiayaItemBiaya.getItemBiayaSekolah().getId() + " ").executeUpdate();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/PengaturanBiayaAction.java:2320");
					// TODO: handle exception
				}
			}
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PengaturanBiaya.reInit();
			}
		});

		return true;
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> idsiswas = new ArrayList<Long>();
		if (!searchsiswa.getValue().trim().isEmpty()) {

			idsiswas = session.createCriteria(Tagihan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.isNotNull("pengaturanBiaya"))
					.setProjection(Projections.groupProperty("pengaturanBiaya.id"))
					.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
					.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)

					.add(Restrictions.or(
							Restrictions.or(
									Restrictions.ilike("calonSiswa.nomorIndukNasional", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE),

									Restrictions.or(
											Restrictions.ilike("calonSiswa.namaSiswa", searchsiswa.getValue().trim(),
													MatchMode.ANYWHERE),
											Restrictions.ilike("calonSiswa.nomorInduk", searchsiswa.getValue().trim(),
													MatchMode.ANYWHERE))),

							Restrictions.or(
									Restrictions.ilike("siswa.nomorIndukNasional", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("siswa.nomorIndukSantri", searchsiswa.getValue().trim(),
													MatchMode.ANYWHERE),

											Restrictions.or(
													Restrictions.ilike("siswa.namaSiswa", searchsiswa.getValue().trim(),
															MatchMode.ANYWHERE),
													Restrictions.ilike("siswa.nomorInduk",
															searchsiswa.getValue().trim(), MatchMode.ANYWHERE))))))

					.list();

		}

		System.out.println("idsiswas -> " + idsiswas);

		PenjurusanSekolah penjurusanSekolah = (PenjurusanSekolah) (searchPenjurusan == null
				|| searchPenjurusan.getSelectedItem() == null ? null : searchPenjurusan.getSelectedItem().getValue());

		Criteria criteria = session.createCriteria(PengaturanBiaya.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						|| searchta.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchta.getSelectedItem().getValue()))

				.add(penjurusanSekolah == null ? Restrictions.sqlRestriction("true")
						: penjurusanSekolah.getId() == null ? Restrictions.isNull("penjurusanSekolah")
								: penjurusanSekolah.getId().equals(-1L) ? Restrictions.isNotNull("penjurusanSekolah")
										: Restrictions.eq("penjurusanSekolah", penjurusanSekolah))

				.add(!searchsiswa.getValue().trim().isEmpty() && idsiswas.isEmpty()
						? Restrictions.sqlRestriction("false")
						: idsiswas.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("id", idsiswas));

		if (!searchnama.getValue().trim().isEmpty()) {
			criteria.createAlias("jenisBiayaSekolah", "jenisBiayaSekolah");
		}
		if (!searchkelas.getValue().trim().isEmpty()) {
			criteria.createAlias("kelasSiswa", "kelasSiswa");
		}

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("jenisBiayaSekolah.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchSatusAwal.getSelectedItem() == null || searchSatusAwal.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("statusAwalSiswa", searchSatusAwal.getSelectedItem().getValue()))

				.add(searchkelas.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") :

						Restrictions.or(
								Restrictions.ilike("kelasSiswa.nama", searchkelas.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("kelasBanyak", searchkelas.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchTahunAngkatan.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunAngkatan", searchTahunAngkatan.getValue().intValue()))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PengaturanBiaya> pengaturanBiaya = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pengaturanBiaya);
		grid.setRowRenderer(new PengaturanBiayaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public static void tampilkanKunci(Hbox vbox1, final PengaturanBiaya pengaturanBiaya, final EventListener refrsh,
			final Tbmuser tbmuser) {
		if (pengaturanBiaya != null && pengaturanBiaya.getId() != null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null) {
			final Toolbarbutton bukaKunciDetail = new ais.ui.util.MyToolbarbuttonConfig(
					pengaturanBiaya.getKunci() == null ? "" : pengaturanBiaya.getKunci().getUserNama(),
					"/img/svg/unlock.svg");
			final Toolbarbutton kunciDetail = new ais.ui.util.MyToolbarbuttonConfig(
					pengaturanBiaya.getKunci() == null ? "" : pengaturanBiaya.getKunci().getUserNama(),
					"/img/svg/lock.svg");

			Hbox vbox = new Hbox();
			vbox.setParent(vbox1);

			bukaKunciDetail.setParent(vbox);
			kunciDetail.setParent(vbox);

			bukaKunciDetail.setStyle("font-size:6px;");
			kunciDetail.setStyle("font-size:6px;");

			kunciDetail.setTooltiptext("Klik untuk meng-kunci tagihan ini");

			if (pengaturanBiaya.getKunci() != null) {
				bukaKunciDetail.setTooltiptext(
						"Dikunci oleh " + pengaturanBiaya.getKunci().getUserId() + ", klik untuk membuka kunci");
			} else {
				bukaKunciDetail.setTooltiptext("klik untuk membuka kunci");
			}

			kunciDetail.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(final Event event1) throws Exception {

					MyMessageboxConfig.show("Apakah Anda yakin ingin mengunci tagihan ini? Setelah dikunci, tagihan tidak dapat diubah sampai kuncinya dibuka kembali.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										pengaturanBiaya.setKunci(tbmuser);
										Common.refreshUpdate(pengaturanBiaya);

										refrsh.onEvent(event1);

									}

								}
							});
				}
			});
			kunciDetail.setVisible(pengaturanBiaya.getKunci() == null);

			kunciDetail.setOrient("vertical");

			bukaKunciDetail.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(final Event event1) throws Exception {

					MyMessageboxConfig.show("Apakah Anda yakin ingin membuka kunci tagihan ini? Setelah kuncinya dibuka, tagihan dapat diubah kembali.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										pengaturanBiaya.setKunci(null);
										Common.refreshUpdate(pengaturanBiaya);

										refrsh.onEvent(event1);

									}
								}
							});
				}
			});
			bukaKunciDetail.setVisible(pengaturanBiaya.getKunci() != null);
			if (pengaturanBiaya.getKunci() != null) {
				bukaKunciDetail.setTooltiptext("Dikunci oleh " + pengaturanBiaya.getKunci().getUserId());
			}

			bukaKunciDetail.setOrient("vertical");
			kunciDetail.setOrient("vertical");

			bukaKunciDetail.setVisible(pengaturanBiaya.getKunci() != null);
			bukaKunciDetail.setDisabled(tbmuser == null || pengaturanBiaya.getKunci() == null
					|| !pengaturanBiaya.getKunci().getUserId().equals(tbmuser.getUserId()));

			kunciDetail.setVisible(pengaturanBiaya.getKunci() == null);
			if (Common.getApakahAdmin()) {
				kunciDetail.setDisabled(false);
			}

		}
	}
}
