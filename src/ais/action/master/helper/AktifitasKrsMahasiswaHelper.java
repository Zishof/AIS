package ais.action.master.helper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.helper.util.PenilaianUtil;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanTranskipAkademik;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.classroom.ClassRoomUtil;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Helper tampilan "Detail Bimbingan Akademik" untuk satu {@link KrsMahasiswa} (relasi
 * mahasiswa-semester-dosen PA), dirender sebagai jendela bertab ({@link Tabbox}) berisi:
 * <ol>
 * <li><b>"Agenda Bimbingan Akademik"</b> — timeline {@link Pertemuan} konsultasi dengan
 * dosen PA (dibangun eager, tab pertama), lengkap dengan toolbar aksi
 * ({@link #initAgendaKrsMahasiswa}), video conference, absensi, dan diskusi/komentar
 * per pertemuan;</li>
 * <li><b>"Rencana Studi"</b> — KRS mahasiswa, dirender lazy saat tab diklik lewat
 * {@link KrsHelper} (bila pengguna adalah mahasiswa sendiri) atau
 * {@link StudiMahasiswaHelper} (bila pengguna staf/admin, dengan hak hapus nilai
 * langsung dikontrol lewat kombinasi konfigurasi dan role admin);</li>
 * <li><b>"Cetak Agenda Konsultasi"</b> dan <b>"Cetak Transkrip"</b> — laporan PDF,
 * dirender lazy lewat {@link #initCetak}.</li>
 * </ol>
 *
 * <p>
 * Karena ZK 5.5 tidak selalu memicu {@code onClick} tab saat berpindah tab (hanya
 * {@code onSelect} pada {@link Tabbox}), sebuah listener {@code ON_SELECT} di
 * {@link #initCetak} secara manual meneruskan event {@code onClick} ke tab yang baru
 * dipilih (kecuali tab pertama yang sudah dibangun eager) agar konten lazy tetap
 * termuat. Isi tab "Agenda Bimbingan Akademik" dibungkus try/catch tersendiri agar satu
 * kegagalan (mis. data pertemuan korup) tidak menggagalkan pembangunan tab-tab
 * lainnya — begitu pula setiap {@link Pertemuan} individual dalam timeline dilindungi
 * try/catch per-item agar satu pertemuan bermasalah tidak menyembunyikan pertemuan lain.
 * </p>
 */
public class AktifitasKrsMahasiswaHelper {

	/** Helper penjadwalan konsultasi PA, dipakai tombol "Agenda Konsultasi dengan Dosen PA" pada toolbar. */
	protected PenjadwalanKrsMahasiswaHelper penjadwalanHelper = new PenjadwalanKrsMahasiswaHelper();

	/** Data mahasiswa milik pengguna yang sedang login (bila pengguna adalah mahasiswa), dipakai untuk kontrol tampilan/akses pada beberapa bagian timeline. */
	private Mahasiswa userMahasiswa = null;

	/** Membuat helper dan mengambil data mahasiswa pengguna yang sedang login (bila ada) ke {@link #userMahasiswa}. */
	public AktifitasKrsMahasiswaHelper() {
		userMahasiswa = Common.getCurrentUser().getMahasiswa();
	}

	/**
	 * Membangun toolbar aksi untuk satu {@code krsMahasiswa}: "Agenda Konsultasi dengan
	 * Dosen PA" (buka {@link PenjadwalanKrsMahasiswaHelper}), "Absensi" (cetak laporan
	 * absensi via {@link CommonReportHelper#onLaporanAbsensi}), kalender
	 * ({@link AktifitasPerkuliahanHelper#tampilCalender}), kelas virtual
	 * ({@link ClassRoomUtil#createButton}), recovery pertemuan
	 * ({@link RecoveryPertemuanHelper#button}), dan "Refresh".
	 *
	 * @param krsMahasiswa relasi KRS mahasiswa-semester-dosen PA terkait
	 * @param dataLoader   callback pemuatan ulang data setelah aksi toolbar dijalankan
	 * @return toolbar berisi tombol-tombol aksi
	 */
	public Toolbar initAgendaKrsMahasiswa(final KrsMahasiswa krsMahasiswa, final DataLoader dataLoader) {

		Toolbar hbox = new Toolbar();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Agenda Konsultasi dengan Dosen PA",
				"/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				penjadwalanHelper.display(krsMahasiswa, dataLoader);
			}

		});

		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(krsMahasiswa, true);
			}

		});
		button.setParent(hbox);

		AktifitasPerkuliahanHelper.tampilCalender(hbox, dataLoader, krsMahasiswa);

		ClassRoomUtil.createButton(krsMahasiswa, dataLoader).setParent(hbox);
		RecoveryPertemuanHelper.button(krsMahasiswa, new EventListener() {
			
			@Override
			public void onEvent(Event arg0) throws Exception {
				refreshData = true;
				krsMahasiswa.belum();
				dataLoader.loadData(null);
			}
		}).setParent(hbox);
		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(hbox);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				refreshData = true;
				krsMahasiswa.belum();
				dataLoader.loadData(null);
			}
		});

		return hbox;
	}

	/** Ditandai {@code true} oleh tombol Refresh/Recovery pada toolbar agar {@link #initDetail} memaksa muat ulang cache pertemuan sekali pada pembangunan berikutnya. */
	private boolean refreshData = false;

	/** Seperti {@link #initDetail(KrsMahasiswa, DataLoader, Div)} dengan {@code dataLoader} default yang memuat ulang dirinya sendiri. */
	public void initDetail(KrsMahasiswa krsMahasiswa, final Div groupbox) throws Exception {
		initDetail(krsMahasiswa, null, groupbox);
	}

	/**
	 * Membangun tampilan bertab lengkap untuk {@code krsMahasiswa} ke dalam
	 * {@code groupbox}: tab "Agenda Bimbingan Akademik" (timeline pertemuan, dibangun
	 * eager), "Rencana Studi", "Cetak Agenda Konsultasi", dan "Cetak Transkrip"
	 * (ketiganya dimuat lazy saat tab dipilih). Setelah seluruh tab terpasang,
	 * {@link ais.ui.util.MyButtonTabbox#gantiTabboxNative} mengganti {@link Tabbox}
	 * native dengan varian berbasis tombol (workaround ZK 5) dan langsung memilih tab
	 * pertama.
	 *
	 * @param krsMahasiswa relasi KRS mahasiswa-semester-dosen PA yang ditampilkan
	 * @param mydataLoader callback pemuatan ulang; bila {@code null}, dibuat default yang memanggil ulang {@link #initDetail(KrsMahasiswa, Div)}
	 * @param groupbox     kontainer ZK yang akan diisi ulang (dibersihkan lebih dulu)
	 * @throws Exception diteruskan dari operasi tampilan/akses Hibernate di luar blok try/catch internal tab agenda
	 */
	public void initDetail(final KrsMahasiswa krsMahasiswa, final DataLoader mydataLoader, final Div groupbox)
			throws Exception {

		final DataLoader dataLoader = mydataLoader == null ? new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(krsMahasiswa, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} : mydataLoader;

		groupbox.setStyle("border: none;");
		Common.clear(groupbox);

		final Tabbox tabbox = new Tabbox();
		tabbox.setSclass("ais-aktifitas-tabbox");
		tabbox.setParent(groupbox);
		tabbox.setWidth("100%");
		tabbox.setHeight("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab = new MyTabConfig("Agenda Bimbingan Akademik");
		tab.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);
		tabpanel.setStyle("min-height: 2000px;");

		// Seluruh isi tab "Agenda Bimbingan Akademik" dibungkus try/catch: sebelumnya
		// satu exception di sini (mis. data pertemuan korup) membatalkan seluruh method,
		// sehingga tab "Rencana Studi" / "Cetak Agenda Konsultasi" / "Cetak Transkrip"
		// TIDAK PERNAH dibuat sama sekali -> tombol-tombol tidak tampil. Karena mahasiswa
		// bukan admin, error ini juga senyap (Common.tampilErrorJikaAdmin), jadi gagal
		// tanpa jejak. Dengan try/catch di sini, tab-tab lain tetap dibangun walau agenda gagal.
		try {
			TreeMap<String, Long> pertemuans = krsMahasiswa.ambilPertemuan(refreshData);
			System.out.println("pertemuans -> " + pertemuans);
			refreshData = false;
			if (pertemuans == null) {
				pertemuans = new TreeMap<String, Long>();
			}
			groupbox.setStyle("height: " + (Math.max(pertemuans.size(), 1) * 6000) + "px;");
			ais.ui.util.MyDiv myGroupbox = new ais.ui.util.MyDiv();
			myGroupbox.setStyle("min-height: 500px;");
			myGroupbox.setParent(tabpanel);
			myGroupbox.appendChild(initAgendaKrsMahasiswa(krsMahasiswa, new DataLoader() {

				@Override
				public void loadData(Object value) {
					try {
						initDetail(krsMahasiswa, groupbox);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

				}
			}));

			groupbox.setStyle("height:6000px;");

			MyGrid grid = new MyGrid();
			grid.setSclass("fgrid");
			grid.setWidth("100%");
			grid.setParent(myGroupbox);
			grid.setWidth("100%");
			grid.setHeight("100%");
			grid.setStyle("height: 6000px;");
			grid.setSclass("fgrid");
			grid.setMold("paging");
			grid.setPageSize(1);
			grid.getPagingChild().setMold("os");
			grid.setPagingPosition("top");

			Rows rows = new Rows();
			rows.setParent(grid);

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
			Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
			calendar1.set(Calendar.DATE, calendar1.get(Calendar.DATE) + 6);
			Tbmuser tbmuser = Common.getCurrentUser();

			boolean urut = false;
			try {
				String pil = tbmuser.retreive("urutkan_diskusi_berdasarkan_terlama");
				urut = (pil == null || pil.trim().isEmpty() ? false : Boolean.parseBoolean(pil));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasKrsMahasiswaHelper.java:216");
				// TODO: handle exception
			}
			boolean mobile = Common.isMobile();
			int selected = 0;
			Date sekarang = WaktuUtil.getDate();
			for (Long pertemuanid : pertemuans.values()) {
				/* Data indeks pertemuan lama dapat menyimpan nilai null. Jangan memanggil
				 * toString() pada nilai tersebut; lewati hanya entri indeks yang rusak agar
				 * pertemuan lain pada agenda tetap dapat ditampilkan. */
				if (pertemuanid == null) {
					continue;
				}
				// Idem: satu pertemuan bermasalah tidak boleh menggagalkan pertemuan lain
				// ataupun tab-tab sesudahnya.
				try {
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
				if (pertemuan != null) {
					Row rowUtama = new Row();
					rowUtama.setParent(rows);
					rowUtama.setValign("top");

					if (pertemuan.getTanggal() != null && pertemuan.getTanggal().before(sekarang)) {
						selected++;
					}

					String tgl = pertemuan.getTanggal() == null ? "-"
							: Common.dateFormat11.get().format(pertemuan.getTanggal()) + " "
									+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
											: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai());

					Groupbox pertemuanBox = new ais.ui.util.MyGroupboxStyled();
					pertemuanBox.setWidth(mobile ? "93%" : "95%");
					rowUtama.appendChild(pertemuanBox);
					MyCaptionStyled c;
					pertemuanBox.appendChild(
							c = new MyCaptionStyled("Pertemuan ke-" + pertemuan.getPertemuanKe() + ", " + tgl));
					c.setStyle("font-size:12px;font-weight: bolder;text-decoration: none;color:"
							+ pertemuan.warna().split(",")[0] + ";border: 1px solid " + pertemuan.warna().split(",")[0]
							+ ";\r\n" + "  padding: 5px;" + "  background-color: rgba(169,169,169,0.4);"
							+ "  border-radius: 5px 15px;");

					Vbox a = RevisiHelper.createNewRevisi(Pertemuan.class, pertemuan,
							pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getStatusPertemuan().getNama());

					Vbox vbox = new Vbox();
					a.setParent(vbox);
					vbox.setParent(pertemuanBox);

					DashboardTimelinePertemuan.displayCatatan(vbox, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							dataLoader.loadData(null);
						}
					}, pertemuan, tbmuser, mobile);

					Component aa = DashboardTimelinePertemuan.createVideoConrefrence(pertemuan, vbox, false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							dataLoader.loadData(null);
						}
					});

					Component bb = AbsensiHelper.createTombolAbsen(pertemuan, true, dataLoader);

					AktifitasPerkuliahanHelper.createKeterangan(pertemuan, userMahasiswa, null, dataLoader, aa, bb,
							DashboardTimelinePertemuan.createScanFoto(tbmuser, pertemuan)).setParent(vbox);

					List<Dosen> dosens = new ArrayList<Dosen>();
					dosens.add(krsMahasiswa.getDosenPa());
					AbsensiHelper.createStatusKehadiran(dosens, pertemuan);

					pertemuan.masukkanData("akses");
					if (Common.bolehKonfigurasi("komentar_tampil_di_halaman_utama_elearning")) {
						Vbox vbox2 = new Vbox();
						vbox2.setParent(vbox);
						if (!pertemuan.udah()) {
							Session session = HibernateUtil.currentSession();
							pertemuan.reInitPertemuanPunyaDiskusi(session);
						}

						TreeSet<Long> pertemuanPunyaDiskusisa = pertemuan.ambilPertemuanPunyaDiskusiTotal(urut);
						DashboardTimelinePertemuan.loadKomentarDetail(null, "42px", pertemuanPunyaDiskusisa, pertemuan,
								vbox2, "background-color: rgba(255,255,255,0.5);", 0, 10, false, null);
					}

				}
				} catch (Exception ePertemuan) {
					ais.common.ErrorAuditUtil.record(ePertemuan,
							"auto-audit(per-pertemuan) src/ais/action/master/helper/AktifitasKrsMahasiswaHelper.java:222 pertemuanid=" + pertemuanid);
				}
			}

			try {
				grid.getPagingChild().setActivePage(selected);
			} catch (Exception e) {
				try {
					grid.getPagingChild().setActivePage(selected - 1);
				} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasKrsMahasiswaHelper.java:303");
					// TODO: handle exception
				}
			}
		} catch (Exception eAgendaTab) {
			ais.common.ErrorAuditUtil.record(eAgendaTab,
					"auto-audit(agenda-tab) src/ais/action/master/helper/AktifitasKrsMahasiswaHelper.java:168 gagal membangun tab Agenda Bimbingan Akademik, lanjut ke tab lain");
		}

		final MyTabConfig tabPenilaian = new MyTabConfig("Rencana Studi");
		tabPenilaian.setParent(tabs);

		final Tabpanel tabpanelPenilaian = new ais.ui.util.MyTabpanel();
		tabpanelPenilaian.setParent(tabpanels);
		tabpanelPenilaian.setHeight("5500px");
		tabPenilaian.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPenilaian.getChildren().size() == 0) {

					Tbmuser tbmuser = Common.getCurrentUser();

					if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
						boolean adminLainBoleh = false;
						String admLain = Common.getKonfigurasi(
								"admin_lain_bisa_menghapus_langsung_data_nilai_mahasiswa_di_menu_krs", "").getNilai();
						String[] aa = admLain.split(";");
						for (String a : aa) {
							try {
								adminLainBoleh = a.trim().equalsIgnoreCase(tbmuser.hakAkses().getRoleId());
								if (adminLainBoleh) {
									break;
								}
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);

							}
						}

						StudiMahasiswaHelper studiMahasiswaHelper = new StudiMahasiswaHelper(
								krsMahasiswa.getSemesterPendek(), false,
								adminLainBoleh || (Common.bolehKonfigurasi("admin_bisa_menghapus_langsung_data_nilai_mahasiswa_di_menu_krs", Konfigurasi.TIDAK_AKTIF) && Common.getApakahAdmin()),
								false, true);
						Html komentarshtml = new ais.ui.util.MyHtml("");
						Html html = new ais.ui.util.MyHtml("");
						Label ip = new Label();
						A sks = new A();
						sks.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								PenilaianUtil.downloadSemuaKRS(krsMahasiswa.getSkskS(), krsMahasiswa.getMahasiswa());
							}
						});

						MyLabelAgakKecil catatan = new MyLabelAgakKecil();
						MyLabelAgakKecil catatanKhs = new MyLabelAgakKecil();

						studiMahasiswaHelper.display(krsMahasiswa.getMahasiswa(), krsMahasiswa.getTahunAkademik(),
								krsMahasiswa.getSemester(), krsMahasiswa.getTahapan(), tabpanelPenilaian, html,
								komentarshtml, ip, sks, catatan, catatanKhs);

					} else if (tbmuser != null && tbmuser.getMahasiswa() != null) {
						KrsHelper krsHelper = new KrsHelper(krsMahasiswa.getSemesterPendek(), false);
						Html komentarshtml = new ais.ui.util.MyHtml("");
						Html html = new ais.ui.util.MyHtml("");
						krsHelper.display(true, krsMahasiswa.getMahasiswa(), krsMahasiswa.getTahunAkademik(),
								krsMahasiswa.getSemester(), krsMahasiswa.getTahapan(), tabpanelPenilaian, html,
								komentarshtml, false);
					}
				}
			}
		});

		AktifitasKrsMahasiswaHelper aktifitasKrsMahasiswaHelper = new AktifitasKrsMahasiswaHelper();
		aktifitasKrsMahasiswaHelper.initCetak(tabbox, krsMahasiswa);

		// Jalur modal detail PA sebelumnya masih memakai Tabbox native. Pada ZK 5,
		// memilih tab laporan tidak selalu memicu onClick sehingga panel cetak tampak
		// kosong. Konversi dilakukan setelah seluruh tab dan pemuat lazy terpasang;
		// MyButtonTabbox akan meneruskan klik ke handler lama lalu memindahkan hasilnya
		// ke panel yang benar-benar terlihat. Tab pertama langsung aktif dan terisi.
		ais.ui.util.MyButtonTabbox tombolTabbox = ais.ui.util.MyButtonTabbox.gantiTabboxNative(tabbox,
				new int[] { 1 });
		if (tombolTabbox != null) {
			// Pastikan tab paling depan langsung aktif dan event pemuat kontennya
			// dijalankan ketika popup dibuka, tanpa menunggu klik pengguna.
			tombolTabbox.pilihPertama();
		}

	}

	/** Toolbar ekspor pada tab "Cetak Agenda Konsultasi", dibuat oleh {@link CommonReport#exportReport}. */
	private Toolbar toolbar;

	/**
	 * Menambahkan dua tab lazy ke {@code tabbox}: "Cetak Agenda Konsultasi" (laporan PDF
	 * {@code lembar_konsultasi}, dibangun ulang setiap tab diklik lewat
	 * {@link Report#generateFileReport}/{@link CommonReport#tampilkanReportPDF}) dan
	 * "Cetak Transkrip" ({@link LaporanTranskipAkademik}, hanya dibangun sekali —
	 * idempoten terhadap klik berulang — dan diblokir bila pengguna mahasiswa tidak
	 * berhak melihat nilai semester tersebut lewat
	 * {@link PenilaianMahasiswaHelper#checkBolehLihatNilai}). Juga memasang listener
	 * {@code ON_SELECT} pada {@code tabbox} yang meneruskan event {@code onClick} ke tab
	 * yang baru dipilih (kecuali tab pertama), sebagai workaround ZK 5.5 agar konten
	 * lazy tab-tab ini benar-benar termuat saat berpindah tab.
	 *
	 * @param tabbox       tabbox induk (biasanya dari {@link #initDetail}) tempat tab baru ditambahkan
	 * @param krsMahasiswa relasi KRS mahasiswa-semester-dosen PA yang datanya dilaporkan
	 */
	public void initCetak(final Tabbox tabbox, final KrsMahasiswa krsMahasiswa) {
		final MyTabConfig tabMonitor = new MyTabConfig("Cetak Agenda Konsultasi");
		tabMonitor.setParent(tabbox.getTabs());
		final Tabpanel tabpanelMonitor = new ais.ui.util.MyTabpanel();
		tabpanelMonitor.setHeight("2000px");
		tabpanelMonitor.setParent(tabbox.getTabpanels());
		tabMonitor.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				// Idempoten: sudah terbangun (mis. oleh pemicu eager pasca-attach) -> jangan
				// generate PDF ulang saat tab diklik/di-select lagi.
				if (tabpanelMonitor.getChildren().size() > 0) {
					return;
				}
				Common.clear(tabpanelMonitor);

				// Panel diberi tinggi pasti (min-height 2000px) agar isi tidak kolaps saat dirender.
				tabpanelMonitor.setStyle("min-height: 2000px;");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(tabpanelMonitor);
				// ZK 5.5: Borderlayout WAJIB punya tinggi PASTI agar region (Center/North) muncul; tanpa
				// ini tingginya 0 → konsultasi/PDF & toolbar tak tampil (panel tampak kosong).
				borderlayout.setWidth("100%");
				borderlayout.setHeight("2000px");

				final Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Map parameters = ais.common.HashMapGenerator.getRand();
						Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs", 2, "mahasiswa");
						Common.insertProperty(Mahasiswa.class, krsMahasiswa.getMahasiswa(), parameters, "", 2);
						Common.insertProperty(Fakultas.class,
								krsMahasiswa.getMahasiswa().getJurusan() == null ? null
										: krsMahasiswa.getMahasiswa().getJurusan().getFakultas(),
								parameters, "fak");
						parameters.put("perkuliahan", krsMahasiswa == null || krsMahasiswa.getId() == null ? -1L : krsMahasiswa.getId());

						File file = Report.generateFileReport(Report.PDF, parameters, "lembar_konsultasi",
								ais.ui.util.WaktuUtil.getDate(), toolbar);
						CommonReport.tampilkanReportPDF(center, file);
					}
				};

				North south = new North();
				south.setParent(borderlayout);
				south.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

					@SuppressWarnings({})
					@Override
					public Map<String, Serializable> generateParameters() throws Exception {
						Map parameters = ais.common.HashMapGenerator.getRand();
						Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs", 2, "mahasiswa");
						Common.insertProperty(Mahasiswa.class, krsMahasiswa.getMahasiswa(), parameters, "", 2);
						Common.insertProperty(Fakultas.class,
								krsMahasiswa.getMahasiswa().getJurusan() == null ? null
										: krsMahasiswa.getMahasiswa().getJurusan().getFakultas(),
								parameters, "fak");
						parameters.put("perkuliahan", krsMahasiswa == null || krsMahasiswa.getId() == null ? -1L : krsMahasiswa.getId());
						return parameters;
					}
				}, "lembar_konsultasi", null, eventListener));

				eventListener.onEvent(null);
			}
		});

		final MyTabConfig tabMonitorTranskrip = new MyTabConfig("Cetak Transkrip");
		tabMonitorTranskrip.setParent(tabbox.getTabs());
		final Tabpanel tabpanelTranskrip = new ais.ui.util.MyTabpanel();
		tabpanelTranskrip.setHeight("2000px");
		tabpanelTranskrip.setParent(tabbox.getTabpanels());
		tabMonitorTranskrip.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {

				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getMahasiswa() != null && !PenilaianMahasiswaHelper
						.checkBolehLihatNilai(krsMahasiswa.getMahasiswa(), krsMahasiswa.getSemester())) {
					return;
				}

				// Idempoten: hindari bangun-ulang saat tab di-select/klik lagi.
				if (tabpanelTranskrip.getChildren().size() > 0) {
					return;
				}
				tabpanelTranskrip.setStyle("min-height: 2000px;");
				Common.clear(tabpanelTranskrip);
				LaporanTranskipAkademik laporanTranskipAkademik = new LaporanTranskipAkademik(
						krsMahasiswa.getMahasiswa());
				// Tinggi PASTI (bukan 100%) agar tidak kolaps 0px di dalam tabpanel min-height.
				laporanTranskipAkademik.setHeight("2000px");
				laporanTranskipAkademik.setWidth("100%");
				laporanTranskipAkademik.setParent(tabpanelTranskrip);
			}
		});

		// FIX konten sub-tab kosong (ZK 5.5): klik tab memicu event ON_SELECT pada Tabbox, BUKAN
		// onClick tiap Tab, sehingga pemuatan lazy (Rencana Studi / Cetak Agenda Konsultasi / Cetak
		// Transkrip) tidak jalan → panel kosong. Re-dispatch onClick ke tab yang SEDANG dipilih. Tab
		// pertama ("Agenda Bimbingan Akademik") kontennya sudah dibangun eager (tanpa onClick),
		// dikecualikan agar tidak membangun ulang / loop.
		tabbox.addEventListener(org.zkoss.zk.ui.event.Events.ON_SELECT, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				org.zkoss.zul.Tab dipilih = tabbox.getSelectedTab();
				org.zkoss.zk.ui.Component tabPertamaAgenda = (tabbox.getTabs() == null) ? null
						: tabbox.getTabs().getFirstChild();
				if (dipilih != null && dipilih != tabPertamaAgenda) {
					org.zkoss.zk.ui.event.Events.sendEvent(new Event("onClick", dipilih));
				}
			}
		});

		// gantiTabboxNative DIPINDAH ke StudiMahasiswaHelper (pemanggil) agar dieksekusi
		// SETELAH konten Tab-1 (Rencana Studi) selesai dibangun ke dalam tabpanelUtama.
		// Bila dipanggil di sini, pindahkanIsiPanel dipanggil saat tabpanelUtama masih
		// kosong → isi Tab-1 tertinggal di Tabpanel tersembunyi → harus klik dulu.
	}
}
