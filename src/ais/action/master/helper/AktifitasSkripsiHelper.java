package ais.action.master.helper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.LogLoginAction;
import ais.action.master.SkripsiAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.classroom.ClassRoomUtil;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Helper tampilan detail lengkap aktivitas bimbingan skripsi/tugas akhir untuk satu
 * {@link Skripsi}, menyerupai timeline e-learning perkuliahan tetapi untuk konteks bimbingan
 * skripsi. {@link #initDetail} merakit tab-tab (via {@link MyButtonTabbox}): "Agenda Revisi
 * Skripsi atau Tugas Akhir" (timeline pertemuan bimbingan dengan diskusi, absensi, video
 * conference, status kehadiran dosen), "Referensi" (referensi diajukan, buku, bahan ajar,
 * artikel — dimuat lazy), "Penilaian" (hanya setelah sidang disetujui), "Laporan" (cetak lembar
 * konsultasi revisi PDF), dan "Pengajuan Wisuda" (hanya setelah sidang disetujui).
 *
 * <p>
 * Toolbar aksi utama ({@link #initAgendaSkripsi}) hanya menampilkan tombol Agenda
 * Sidang/Revisi, ambil jadwal, ekspor DSpace, kalender, dan integrasi ruang kelas virtual
 * ketika {@link Skripsi#getSetujuiSidang()} bernilai {@code true} — sebelum sidang disetujui,
 * hanya tombol Absensi, Recovery Pertemuan, dan Refresh yang tampil.
 * </p>
 *
 * <p>
 * Bila skripsi sudah punya tanggal sidang tetapi belum punya satu pun {@link Pertemuan}
 * terkait, {@link #initDetail} otomatis membuat satu pertemuan "Sidang atau persiapan sidang"
 * pada tanggal sidang tersebut sebelum menampilkan timeline (self-healing data agar tanggal
 * sidang selalu punya representasi agenda).
 * </p>
 */
public class AktifitasSkripsiHelper {

	protected PenjadwalanSkripsiHelper penjadwalanHelper = new PenjadwalanSkripsiHelper();

	private Mahasiswa userMahasiswa = null;

	/** Menyimpan referensi {@link Mahasiswa} pengguna saat ini (bila pengguna login adalah mahasiswa), dipakai untuk personalisasi tampilan timeline. */
	public AktifitasSkripsiHelper() {
		try {
			Tbmuser tbmuser = Common.getCurrentUser();
			userMahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasSkripsiHelper.java:69");
			// TODO: handle exception
		}
	}

	/**
	 * Membangun toolbar aksi agenda skripsi: Agenda Sidang dan Revisi (tampil hanya bila
	 * {@code skripsi.getSetujuiSidang()}), Absensi, dan — hanya setelah sidang disetujui —
	 * tombol ambil jadwal, ekspor DSpace, kalender, serta tombol integrasi ruang kelas virtual
	 * ({@link ClassRoomUtil}). Tombol Recovery Pertemuan dan Refresh selalu tampil.
	 *
	 * @param skripsi    skripsi yang agendanya dikelola
	 * @param dataLoader callback disegarkan setelah aksi pada toolbar mengubah data
	 * @return toolbar siap ditempelkan ke parent ZK
	 */
	public Toolbar initAgendaSkripsi(final Skripsi skripsi, final DataLoader dataLoader) {

		Toolbar hbox = new Toolbar();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Agenda Sidang dan Revisi", "/img/jadwal.png");
		button.setVisible(skripsi.getSetujuiSidang());
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				penjadwalanHelper.display(skripsi, dataLoader);
			}

		});

		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(skripsi, true);
			}

		});
		button.setParent(hbox);

		// PenjadwalanHelper.tampilTombol(hbox, null, null, null, null,
		// skripsi);

		if (skripsi.getSetujuiSidang()) {
			PenjadwalanHelper.tampilTombolAmbil(hbox, null, null, null, null, skripsi, null, null, dataLoader);

			DspaceHelper.tampilkanButtonExportDiPertemuan(hbox, null, null, null, null, skripsi, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					dataLoader.loadData(arg0);
					LogLoginAction.tampilDpsaceLog();
				}
			});

			AktifitasPerkuliahanHelper.tampilCalender(hbox, dataLoader, skripsi);

			ClassRoomUtil.createButton(skripsi, dataLoader).setParent(hbox);
		}
		RecoveryPertemuanHelper.button(skripsi, new EventListener() {
			
			@Override
			public void onEvent(Event arg0) throws Exception {
				skripsi.belum();
				dataLoader.loadData(null);
			}
		}).setParent(hbox);
		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(hbox);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				skripsi.belum();
				dataLoader.loadData(null);
			}
		});

		return hbox;
	}

	/** Varian {@link #initDetail(Skripsi, DataLoader, MyDiv)} tanpa {@link DataLoader} eksplisit — memakai callback default yang cukup memanggil ulang {@code initDetail} untuk menyegarkan tampilan. */
	public void initDetail(final Skripsi skripsi, final MyDiv groupbox) throws Exception {
		initDetail(skripsi, null, groupbox);
	}

	/**
	 * Merakit seluruh tampilan detail aktivitas skripsi ke dalam {@code groupbox}: tab Agenda
	 * (timeline pertemuan bimbingan berpaging satu-per-halaman, otomatis membuka halaman
	 * pertemuan terkini/lampau pertama), Referensi, Penilaian, Laporan, dan Pengajuan Wisuda.
	 * Bila belum ada pertemuan tetapi tanggal sidang sudah ditentukan, membuat satu pertemuan
	 * "sidang/persiapan sidang" otomatis lalu memuat ulang tampilan (lihat javadoc kelas).
	 * Urutan tampil pertemuan mengikuti preferensi pengguna
	 * {@code urutkan_diskusi_berdasarkan_terlama}.
	 *
	 * @param skripsi     skripsi yang detailnya ditampilkan
	 * @param mydataLoader callback penyegaran custom; bila {@code null}, dibuatkan callback
	 *                     default yang memanggil ulang method ini
	 * @param groupbox    kontainer yang akan diisi (isi sebelumnya dibersihkan)
	 */
	public void initDetail(final Skripsi skripsi, final DataLoader mydataLoader, final MyDiv groupbox)
			throws Exception {

		final DataLoader dataLoader = mydataLoader == null ? new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(skripsi, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} : mydataLoader;

		groupbox.setStyle("border:none;height:72vh;min-height:520px;overflow-y:auto;overflow-x:hidden;");
		Common.clear(groupbox);

		final MyButtonTabbox tabbox = MyButtonTabbox.buat(groupbox, "3000px", new int[] { 1 });

		Div tabpanel = tabbox.tambahTab(1, "Agenda Revisi Skripsi atau Tugas Akhir");
		tabpanel.setStyle("min-height:500px;overflow:auto;");

		ais.ui.util.MyDiv myGroupbox = new ais.ui.util.MyDiv();
		myGroupbox.setStyle("min-height: 500px;");
		myGroupbox.setParent(tabpanel);
		myGroupbox.appendChild(initAgendaSkripsi(skripsi, new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(skripsi, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		}));

		TreeMap<String, Long> pertemuans = skripsi.ambilPertemuan();
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && pertemuans.isEmpty() && skripsi.getTanggalSidang() != null) {

			Pertemuan pertemuan = new Pertemuan();

			pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);
			pertemuan.setTanggal(skripsi.getTanggalSidang() == null ? WaktuUtil.getDate() : skripsi.getTanggalSidang());
			pertemuan.setSkripsi(skripsi);
			pertemuan.setTopik("Sidang atau persiapan sidang dengan judul \"" + skripsi.getJudul() + "\"");
			pertemuan.setWaktuMulai(
					skripsi.getTanggalSidang() == null ? "" : Common.timeFormat2.get().format(skripsi.getTanggalSidang()));
			pertemuan.setWaktuSelesai(
					skripsi.getTanggalSidang() == null ? "" : Common.timeFormat2.get().format(skripsi.getTanggalSidang()));

			pertemuan.setSkripsi(skripsi);

			try {
				Session session = HibernateUtil.currentSession();
				session.save(pertemuan);
				session.flush();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasSkripsiHelper.java:252");
			}
			pertemuans.put(Common.dateFormat8.get().format(pertemuan.getTanggal()) + "_" + pertemuan.getId(),
					pertemuan.getId());

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					skripsi.belum();
					initDetail(skripsi, dataLoader, groupbox);
				}
			});

			return;
		}
		myGroupbox.setStyle("height:2000px;");

		MyGrid grid = new MyGrid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(myGroupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("height: 2000px;");
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

		boolean urut = false;
		try {
			String pil = tbmuser.retreive("urutkan_diskusi_berdasarkan_terlama");
			urut = (pil == null || pil.trim().isEmpty() ? false : Boolean.parseBoolean(pil));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasSkripsiHelper.java:295");
			// TODO: handle exception
		}

		int selected = 0;
		Date sekarang = WaktuUtil.getDate();

		if (skripsi.getSetujuiSidang()) {
			boolean mobile = Common.isMobile();
			for (Long pertemuanid : pertemuans.values()) {
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
				if (pertemuan != null) {
					final Row rowUtama = new Row();
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
					vbox.setParent(pertemuanBox);
					a.setParent(vbox);
					new Label(pertemuan.getTopik()).setParent(vbox);

					DashboardTimelinePertemuan.displayCatatan(vbox, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							dataLoader.loadData(null);
						}
					}, pertemuan, tbmuser, mobile);

					Component buttonVicon = DashboardTimelinePertemuan.createVideoConrefrence(pertemuan, null, false,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									dataLoader.loadData(null);
								}
							});
					Component buttonAbsen = AbsensiHelper.createTombolAbsen(pertemuan, true, dataLoader);

					AktifitasPerkuliahanHelper.createKeterangan(pertemuan, userMahasiswa, null, dataLoader, buttonVicon,
							buttonAbsen, DashboardTimelinePertemuan.createScanFoto(tbmuser, pertemuan)).setParent(vbox);

					if (pertemuan.getSkripsi() != null) {
						List<CommonVO> dataDosen = pertemuan.getSkripsi().dataDosen(true);
						AbsensiHelper.createStatusKehadiranData(dataDosen, pertemuan).setParent(pertemuanBox);
					}

					DashboardTimelinePertemuan.tampilOnline(pertemuan, pertemuanBox, tbmuser, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							dataLoader.loadData(null);
						}
					});

					pertemuan.masukkanData("akses");
					if (Common.bolehKonfigurasi("komentar_tampil_di_halaman_utama_elearning")) {
						Vbox vbox2 = new Vbox();
						vbox2.setParent(pertemuanBox);
						if (!pertemuan.udah()) {
							Session session = HibernateUtil.currentSession();
							pertemuan.reInitPertemuanPunyaDiskusi(session);
						}
						TreeSet<Long> pertemuanPunyaDiskusisa = pertemuan.ambilPertemuanPunyaDiskusiTotal(urut);
						DashboardTimelinePertemuan.loadKomentarDetail(null, "42px", pertemuanPunyaDiskusisa, pertemuan,
								vbox2, "background-color: rgba(255,255,255,0.5);", 0, 10, false, null);
					}
				}
			}

		}

		try {
			grid.getPagingChild().setActivePage(selected);
		} catch (Exception e) {
			try {
				grid.getPagingChild().setActivePage(selected - 1);
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasSkripsiHelper.java:393");
				// TODO: handle exception
			}
		}

		final Div tabpanelReferensi = tabbox.tambahTabLazy(2, "Referensi", new MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panelReferensi) throws Exception {
				if (panelReferensi.getChildren().size() == 0) {

					final MyButtonTabbox tabboxReferensi = MyButtonTabbox.buat(panelReferensi, "100%",
							new int[] { 1 });

					Div tabpanelReferensiDiajukan = tabboxReferensi.tambahTab(1, "Referensi");
					setPanelDetailTinggi(tabpanelReferensiDiajukan);

					Grid gridref = SkripsiAction.initReferensi(skripsi, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					});
					gridref.setParent(Common.tampilanScroll(tabpanelReferensiDiajukan));

					Div panelReferensiBuku = tabboxReferensi.tambahTab(2, "Ref. Buku");
					setPanelDetailTinggi(panelReferensiBuku);

					DataPunyaItemHelper dataPunyaItemHelper = new DataPunyaItemHelper();
					dataPunyaItemHelper.display(skripsi, null, null, null, null, panelReferensiBuku);

					final Div tabpanelBukuAjar = tabboxReferensi.tambahTabLazy(3, "Ref. Bahan Ajar",
							new MyButtonTabbox.PemuatTab() {
								@Override
								public void muat(Div panel) throws Exception {
									if (panel.getChildren().size() == 0) {

										Div panelBukuAjar = new Div();
										setPanelDetailTinggi(panelBukuAjar);
										panelBukuAjar.setParent(panel);
										DataPunyaBukuAjarHelper dataPunyaBukuAjarHelper = new DataPunyaBukuAjarHelper();
										dataPunyaBukuAjarHelper.display(skripsi, null, null, null, null,
												panelBukuAjar);
									}
								}
							});
					setPanelDetailTinggi(tabpanelBukuAjar);

					final Div tabpanelArtikel = tabboxReferensi.tambahTabLazy(4, "Ref. Artikel",
							new MyButtonTabbox.PemuatTab() {
								@Override
								public void muat(Div panel) throws Exception {
									if (panel.getChildren().size() == 0) {

										DataPunyaArtikelHelper dataPunyaArtikelHelper = new DataPunyaArtikelHelper();
										dataPunyaArtikelHelper.display(skripsi, null, null, null, null, null, null,
												panel);
									}
								}
							});
					setPanelDetailTinggi(tabpanelArtikel);
					tabboxReferensi.pilihPertama();
				}
			}
		});

		final Div tabpanelPenilaian = tabbox.tambahTabLazy(3, "Penilaian", new MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panel) throws Exception {
				if (panel.getChildren().size() == 0) {

					PenilaianSkripsiHelper penilaianSkripsiHelper = new PenilaianSkripsiHelper();
					penilaianSkripsiHelper.display(skripsi, panel, null);
				}
			}
		});
		setPanelDetailTinggi(tabpanelPenilaian);
		tabbox.setVisiblePanel(tabpanelPenilaian, skripsi.getSetujuiSidang());

		initCetak(tabbox, skripsi);

		final Div tabpanelPengajuanWisuda = tabbox.tambahTabLazy(5, "Pengajuan Wisuda",
				new MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						if (panel.getChildren().size() == 0) {

							Borderlayout borderlayout = new Borderlayout();
							borderlayout.setParent(panel);
							borderlayout.setStyle("min-height:360px");
							Center center = new Center();
							center.setParent(borderlayout);
							center.appendChild(new MyInclude("/pages/master/pendaftaran_wisuda_mahasiswa.zul?mahasiswa="
									+ skripsi.getMahasiswa().getId()));
						}
					}
				});
		setPanelDetailTinggi(tabpanelPengajuanWisuda);
		tabbox.setVisiblePanel(tabpanelPengajuanWisuda, skripsi.getSetujuiSidang());
		tabbox.pilihPertama();

	}

	private Toolbar toolbar;

	/**
	 * Menambahkan tab "Laporan" (dimuat lazy) yang menampilkan pratinjau PDF laporan "lembar
	 * konsultasi revisi" untuk {@code skripsi}, lengkap dengan toolbar ekspor
	 * ({@link CommonReport#exportReport}). Parameter laporan disusun dari id skripsi, rentang
	 * tanggal bimbingan (awal/akhir), dan seluruh properti entitas {@link Skripsi} (lewat
	 * {@link Common#insertProperty}).
	 */
	public void initCetak(MyButtonTabbox tabbox, final Skripsi skripsi) {
		final Div tabpanelMonitor = tabbox.tambahTabLazy(4, "Laporan", new MyButtonTabbox.PemuatTab() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void muat(Div panel) throws Exception {
				Common.clear(panel);
				final Map parameters = ais.common.HashMapGenerator.getRand();

				parameters.put("perkuliahan", skripsi == null || skripsi.getId() == null ? -1L : skripsi.getId());

				parameters.put("awal", skripsi == null || skripsi.getAwalBimbingan() == null ? ""
						: Common.dateFormat2.get().format(skripsi.getAwalBimbingan()));

				parameters.put("akhir", skripsi == null || skripsi.getAkhirBimbingan() == null ? ""
						: Common.dateFormat2.get().format(skripsi.getAkhirBimbingan()));

				Common.insertProperty(Skripsi.class, skripsi, parameters, "", 2);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(panel);
				borderlayout.setHeight("700px");

				final Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						File file = Report.generateFileReport(Report.PDF, parameters, "lembar_konsultasi_revisi",
								ais.ui.util.WaktuUtil.getDate(), toolbar);
						CommonReport.tampilkanReportPDF(center, file);
					}
				};

				North north = new North();
				north.setParent(borderlayout);
				north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

					@SuppressWarnings({})
					@Override
					public Map<String, Serializable> generateParameters() throws Exception {
						return parameters;
					}
				}, "lembar_konsultasi_revisi", null, eventListener));

				eventListener.onEvent(null);
			}
		});
		setPanelDetailTinggi(tabpanelMonitor);
	}

	/** Mengatur tinggi minimum 3000px + scroll vertikal pada panel tab (bila komponen HTML), agar konten timeline yang panjang tetap dapat digulir dengan wajar dalam tabbox. */
	private void setPanelDetailTinggi(Component tabpanel) {
		if (tabpanel == null) {
			return;
		}
		if (tabpanel instanceof HtmlBasedComponent) {
			HtmlBasedComponent html = (HtmlBasedComponent) tabpanel;
			html.setHeight("3000px");
			html.setStyle("height:3000px;min-height:3000px;overflow-y:auto;overflow-x:hidden;");
		}
	}

}
