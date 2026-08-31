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
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
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
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.LogLoginAction;
import ais.action.master.MahasiswaRequestTugasAkhirAction;
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
import ais.database.model.FormatNilaiProposalSkripsi;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Pertemuan;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.TemplateFormatBimbingan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Helper composer ZK untuk halaman detail aktivitas bimbingan tugas akhir/skripsi
 * ({@link MahasiswaRequestTugasAkhir}) — analog {@code AktifitasPerkuliahanHelper} tapi untuk
 * konteks tugas akhir. Membangun UI bertab ({@code MyButtonTabbox}) berisi: (1) tab "Agenda
 * Bimbingan" — daftar {@link Pertemuan} bimbingan berpaging (1 pertemuan per halaman, otomatis
 * diposisikan ke pertemuan terakhir yang tanggalnya sudah lewat) lengkap dengan absensi, video
 * konferensi, unggah catatan/scan, dan (bila diaktifkan) diskusi komentar per pertemuan; (2) tab
 * "Referensi" (buku, bahan ajar, artikel) dimuat lazy; (3) untuk status aktif/seminar/mengulang/
 * lulus: tab "Penilaian", tab "Laporan" (cetak lembar konsultasi bimbingan PDF), dan — bila format
 * nilai proposal mensyaratkan sidang — tab "Sidang" untuk mengajukan/mengelola {@link Skripsi}.
 *
 * <p>
 * {@link #initDetail} memiliki efek samping penting: bila mahasiswa belum punya satu pun
 * {@link Pertemuan} bimbingan padahal tanggal awal bimbingan sudah diset, satu pertemuan awal
 * otomatis dibuat (topik "Seminar skripsi atau tugas akhir dengan judul ..."); dan bila format
 * nilai proposal punya {@link TemplateFormatBimbingan} yang belum punya pertemuan padanan, setiap
 * template yang hilang otomatis dibuatkan pertemuan baru (tanggal dihitung dari
 * {@code setelahHari} template terhadap pertemuan pertama). Setelah pertemuan-pertemuan itu
 * dibuat, seluruh tampilan dimuat ulang lewat timer default agar data konsisten.
 * </p>
 */
public class AktifitasTugasAkhirHelper {

	protected PenjadwalanTugasAkhirHelper penjadwalanHelper = new PenjadwalanTugasAkhirHelper();

	private Mahasiswa userMahasiswa = null;

	/** Membuat helper dan mengambil {@link Mahasiswa} milik user yang sedang login (bila ada), dipakai untuk kontrol tampilan yang berbeda antara dosen dan mahasiswa. */
	public AktifitasTugasAkhirHelper() {
		try {
			Tbmuser tbmuser = Common.getCurrentUser();
			userMahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasTugasAkhirHelper.java:75");
			// TODO: handle exception
		}
	}

	/**
	 * Membangun toolbar aksi untuk satu {@link MahasiswaRequestTugasAkhir}: tombol "Agenda
	 * Bimbingan" (buka penjadwalan, tampil hanya untuk status aktif/seminar/mengulang/lulus),
	 * "Absensi" (cetak laporan absensi), tombol ambil jadwal ({@code PenjadwalanHelper}), ekspor
	 * DSpace, kalender, Google Classroom, pemulihan pertemuan, dan Refresh.
	 *
	 * @param mahasiswaRequestTugasAkhir konteks bimbingan tugas akhir
	 * @param dataLoader                 callback penyegar tampilan setelah aksi tombol
	 * @return toolbar berisi seluruh tombol aksi
	 */
	public Toolbar initAgendaMahasiswaRequestTugasAkhir(final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			final DataLoader dataLoader) {

		Toolbar hbox = new Toolbar();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Agenda Bimbingan", "/img/jadwal.png");
		button.setVisible(mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
				|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
				|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.MENGULANG_STATUS)
				|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.LULUS_STATUS));

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				penjadwalanHelper.display(mahasiswaRequestTugasAkhir, dataLoader);
			}

		});

		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(mahasiswaRequestTugasAkhir, true);
			}

		});
		button.setParent(hbox);

		// PenjadwalanHelper.tampilTombol(hbox, null, null, null,
		// mahasiswaRequestTugasAkhir, null);

		PenjadwalanHelper.tampilTombolAmbil(hbox, null, null, null, mahasiswaRequestTugasAkhir, null, null, null,
				dataLoader);

		DspaceHelper.tampilkanButtonExportDiPertemuan(hbox, null, null, null, mahasiswaRequestTugasAkhir, null,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dataLoader.loadData(arg0);
						LogLoginAction.tampilDpsaceLog();
					}
				});

		AktifitasPerkuliahanHelper.tampilCalender(hbox, dataLoader, mahasiswaRequestTugasAkhir);

		ClassRoomUtil.createButton(mahasiswaRequestTugasAkhir, dataLoader).setParent(hbox);
		RecoveryPertemuanHelper.button(mahasiswaRequestTugasAkhir, new EventListener() {
			
			@Override
			public void onEvent(Event arg0) throws Exception {
				mahasiswaRequestTugasAkhir.belum();
				dataLoader.loadData(null);
			}
		}).setParent(hbox);
		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(hbox);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				mahasiswaRequestTugasAkhir.belum();
				dataLoader.loadData(null);
			}
		});

		return hbox;
	}

	/** Seperti {@link #initDetail(MahasiswaRequestTugasAkhir, DataLoader, Div)} dengan {@code dataLoader} default yang memuat ulang dirinya sendiri. */
	public void initDetail(final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir, final Div groupbox)
			throws Exception {
		initDetail(mahasiswaRequestTugasAkhir, null, groupbox);
	}

	/**
	 * Membangun seluruh UI bertab detail bimbingan tugas akhir ke dalam {@code groupbox}. Lihat
	 * javadoc kelas untuk penjelasan lengkap tab yang dibangun dan efek samping pembuatan
	 * {@link Pertemuan} otomatis. Untuk status aktif/seminar/mengulang/lulus, method ini juga
	 * memuat daftar pertemuan bimbingan ke grid berpaging (1 per halaman) dengan halaman aktif
	 * otomatis diposisikan ke pertemuan terakhir yang sudah lewat.
	 *
	 * @param mahasiswaRequestTugasAkhir konteks bimbingan tugas akhir yang detailnya ditampilkan
	 * @param mydataLoader               callback penyegar tampilan; bila {@code null}, dibuat
	 *                                    callback default yang memanggil ulang method ini
	 * @param groupbox                   komponen ZK tujuan tampilan (dibersihkan lebih dulu)
	 */
	@SuppressWarnings("unchecked")
	public void initDetail(final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir, final DataLoader mydataLoader,
			final Div groupbox) throws Exception {

		final DataLoader dataLoader = mydataLoader == null ? new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(mahasiswaRequestTugasAkhir, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		} : mydataLoader;

		groupbox.setStyle("border: none;");
		Common.clear(groupbox);

		final MyButtonTabbox buttonTabbox = MyButtonTabbox.buat(groupbox, "1250px", new int[] { 1 });
		buttonTabbox.setTombolMembungkus(true);
		final Div tabpanel = buttonTabbox.tambahTab(1, "Agenda Bimbingan", "/img/jadwal.png");
		tabpanel.setStyle("overflow:auto;min-height:1100px;box-sizing:border-box;");

		ais.ui.util.MyDiv myGroupbox = new ais.ui.util.MyDiv();
		myGroupbox.setStyle("min-height: 500px;");
		myGroupbox.setParent(tabpanel);
		myGroupbox.appendChild(initAgendaMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir, new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					initDetail(mahasiswaRequestTugasAkhir, groupbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		}));

		if ((mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
				|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
				|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.MENGULANG_STATUS)
				|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.LULUS_STATUS))) {

			TreeMap<String, Long> pertemuans = mahasiswaRequestTugasAkhir.ambilPertemuan();
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && pertemuans.isEmpty()
					&& mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan() != null) {

				Pertemuan pertemuan = new Pertemuan();
				pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);
				pertemuan.setTanggal(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan());
				pertemuan.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
				pertemuan.setTopik("Seminar skripsi atau tugas akhir dengan judul \""
						+ (mahasiswaRequestTugasAkhir.getJudul().isEmpty() ? mahasiswaRequestTugasAkhir.getJudul1()
								: mahasiswaRequestTugasAkhir.getJudul())
						+ "\"");
				pertemuan.setWaktuMulai("07.00");
				pertemuan.setWaktuSelesai("17.00");

				try {
					Session session = HibernateUtil.currentSession();
					session.save(pertemuan);
					session.flush();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasTugasAkhirHelper.java:249");
				}

				if (pertemuan.getId() != null) {
					pertemuans.put(Common.dateFormat8.get().format(pertemuan.getTanggal()) + "_" + pertemuan.getId(),
							pertemuan.getId());

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							mahasiswaRequestTugasAkhir.belum();
							initDetail(mahasiswaRequestTugasAkhir, dataLoader, groupbox);
						}
					});

					return;
				}
			}

			FormatNilaiProposalSkripsi formatNilaiProposalSkripsi = mahasiswaRequestTugasAkhir
					.getFormatNilaiProposalSkripsi();
			if (formatNilaiProposalSkripsi != null && formatNilaiProposalSkripsi.getId() != null && !pertemuans.isEmpty()) {

				Pertemuan pertemuanAwal = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
						(pertemuans.values().iterator().next() == null ? "" : pertemuans.values().iterator().next().toString()));

				Session session = HibernateUtil.currentSession();
				List<TemplateFormatBimbingan> templates = session.createCriteria(TemplateFormatBimbingan.class)
						.addOrder(Order.asc("setelahHari"))
						.add(Restrictions.eq("formatNilaiProposalSkripsi", formatNilaiProposalSkripsi)).list();
				if (templates != null && templates.size() > 0) {
					boolean belumAdaSemua = false;
					for (TemplateFormatBimbingan templateFormatBimbingan : templates) {
						boolean ada = false;
						for (Long pertemuanid : pertemuans.values()) {
							if (pertemuanid == null) {
								continue;
							}
							Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
									pertemuanid.toString());
							if (pertemuan != null) {
								if (pertemuan.getKurikulumPunyaMatakuliahDetail() != null && pertemuan
										.getKurikulumPunyaMatakuliahDetail().equals(templateFormatBimbingan.getId())) {
									ada = true;
									break;
								}
							}
						}
						if (!ada) {
							belumAdaSemua = true;

							Calendar calendar = WaktuUtil.getCalendar();
							calendar.setTime(pertemuanAwal.getTanggal());
							calendar.set(Calendar.DATE,
									calendar.get(Calendar.DATE) + templateFormatBimbingan.getSetelahHari());

							Pertemuan pertemuan = new Pertemuan();
							pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);
							pertemuan.setTanggal(calendar.getTime());
							pertemuan.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
							pertemuan.setTopik(templateFormatBimbingan.getNama());
							pertemuan.setWaktuMulai("07.00");
							pertemuan.setWaktuSelesai("17.00");
							pertemuan.setKurikulumPunyaMatakuliahDetail(templateFormatBimbingan.getId());

							try {

								session.save(pertemuan);
								session.flush();
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasTugasAkhirHelper.java:315");
							}

							if (pertemuan.getId() != null) {
								pertemuans.put(Common.dateFormat8.get().format(pertemuan.getTanggal()) + "_"
										+ pertemuan.getId(), pertemuan.getId());
							}
						}
					}

					if (belumAdaSemua) {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								mahasiswaRequestTugasAkhir.belum();
								initDetail(mahasiswaRequestTugasAkhir, dataLoader, groupbox);
							}
						});
					}
				}
			}

			myGroupbox.setStyle("height:6000px;");

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

			boolean urut = false;
			try {
				String pil = tbmuser.retreive("urutkan_diskusi_berdasarkan_terlama");
				urut = (pil == null || pil.trim().isEmpty() ? false : Boolean.parseBoolean(pil));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasTugasAkhirHelper.java:363");
				// TODO: handle exception
			}
			boolean mobile = Common.isMobile();
			int selected = 0;
			Date sekarang = WaktuUtil.getDate();
			for (Long pertemuanid : pertemuans.values()) {
				if (pertemuanid == null) {
					continue;
				}
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
					a.appendChild(new Label(
							pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getStatusPertemuan().getNama()));

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

					Component aa = DashboardTimelinePertemuan.createVideoConrefrence(pertemuan, null, false,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									dataLoader.loadData(null);
								}
							});

					Component bb = AbsensiHelper.createTombolAbsen(pertemuan, true, dataLoader);

					AktifitasPerkuliahanHelper.createKeterangan(pertemuan, userMahasiswa, null, dataLoader, aa, bb,
							DashboardTimelinePertemuan.createScanFoto(tbmuser, pertemuan)).setParent(vbox);

					AbsensiHelper.createStatusKehadiran(mahasiswaRequestTugasAkhir.populateDosen().values(), pertemuan)
							.setParent(pertemuanBox);

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

			try {
				grid.getPagingChild().setActivePage(selected);
			} catch (Exception e) {
				try {
					grid.getPagingChild().setActivePage(selected - 1);
				} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/helper/AktifitasTugasAkhirHelper.java:449");
					// TODO: handle exception
				}
			}
		}

		final Div panelReferensi = buttonTabbox.tambahTabLazy(2, "Referensi", "/img/svg/books-thin.svg",
				new MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(Div panelReferensiUtama) throws Exception {
				panelReferensiUtama.setStyle("overflow:auto;min-height:1100px;");
				final MyButtonTabbox referensiTabs = MyButtonTabbox.buat(panelReferensiUtama, "1100px",
						new int[] { 1 });

				Div panelReferensiDiajukan = referensiTabs.tambahTab(1, "Referensi");
				Grid gridref = MahasiswaRequestTugasAkhirAction.initReferensi(mahasiswaRequestTugasAkhir,
						new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
							}
						});
				gridref.setParent(Common.tampilanScroll(panelReferensiDiajukan));

				referensiTabs.tambahTabLazy(2, "Ref. Buku", new MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						DataPunyaItemHelper helper = new DataPunyaItemHelper();
						helper.display(null, mahasiswaRequestTugasAkhir, null, null, null, panel);
					}
				});
				referensiTabs.tambahTabLazy(3, "Ref. Bahan Ajar", new MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						DataPunyaBukuAjarHelper helper = new DataPunyaBukuAjarHelper();
						helper.display(null, mahasiswaRequestTugasAkhir, null, null, null, panel);
					}
				});
				referensiTabs.tambahTabLazy(4, "Ref. Artikel", new MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						DataPunyaArtikelHelper helper = new DataPunyaArtikelHelper();
						helper.display(null, mahasiswaRequestTugasAkhir, null, null, null, null, null, panel);
					}
				});
				referensiTabs.pilih(1);
			}
		});
		setPanelDetailTinggi(panelReferensi);

		if (mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
				|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
				|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.MENGULANG_STATUS)
				|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.LULUS_STATUS)) {
			final Div panelPenilaian = buttonTabbox.tambahTabLazy(3, "Penilaian", "/img/svg/journal-check.svg",
					new MyButtonTabbox.PemuatTab() {
				@Override
				public void muat(Div panel) throws Exception {
					setPanelDetailTinggi(panel);
					PenilaianProposalSkripsiHelper helper = new PenilaianProposalSkripsiHelper();
					helper.display(mahasiswaRequestTugasAkhir, panel);
				}
			});
			setPanelDetailTinggi(panelPenilaian);

			initCetak(buttonTabbox, 4, mahasiswaRequestTugasAkhir);

			if (mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() != null
					&& mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getTerdapatSidangSetelahSelesai()) {
				buttonTabbox.tambahTabLazy(5, "Sidang", "/img/svg/journal-check.svg",
						new MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						panel.setStyle("overflow:auto;min-height:1100px;");
						initSidang(mahasiswaRequestTugasAkhir, panel, groupbox);
					}
				});
			}
		}
		buttonTabbox.pilih(1);
	}

	/** Mengatur tinggi tetap 1100px (dengan scroll vertikal) pada panel tab, bila komponennya berupa {@link org.zkoss.zk.ui.HtmlBasedComponent}. */
	private void setPanelDetailTinggi(Component panel) {
		if (panel instanceof org.zkoss.zk.ui.HtmlBasedComponent) {
			org.zkoss.zk.ui.HtmlBasedComponent html = (org.zkoss.zk.ui.HtmlBasedComponent) panel;
			html.setHeight("1100px");
			html.setStyle("height:1100px;min-height:1100px;overflow-y:auto;overflow-x:hidden;");
		}
	}

	/**
	 * Mengisi tab Sidang: bila {@link Skripsi} untuk bimbingan ini belum ada, tampilkan tombol
	 * "Ajukan Sidang" yang membuat entitas {@link Skripsi} baru (judul dan pembimbing 1-3 diisi
	 * dari data bimbingan) via {@code SkripsiAction.onAddExternal}, lalu memuat ulang tab setelah
	 * tersimpan; bila {@link Skripsi} sudah ada, tampilkan langsung komponen
	 * {@code SkripsiAction.initComponenAddExternal}-nya (form sidang) di tab, dan menyimpan
	 * referensi {@code SkripsiAction} pada atribut {@code groupbox} untuk dipakai pemanggil.
	 *
	 * @param mahasiswaRequestTugasAkhir konteks bimbingan tugas akhir
	 * @param tabpanelSidang             panel tab Sidang yang akan diisi
	 * @param groupbox                   komponen induk, dipakai untuk menyimpan atribut {@code skripsiAction}
	 */
	private void initSidang(final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir, final Component tabpanelSidang,
			final Component groupbox) throws Exception {
		Session session = HibernateUtil.currentSession();
		Skripsi skripsi = (Skripsi) session.createCriteria(Skripsi.class)
				.add(Restrictions.eq("mahasiswaRequestTugasAkhir", mahasiswaRequestTugasAkhir)).setMaxResults(1)
				.addOrder(Order.desc("id")).uniqueResult();

		if (skripsi == null) {
			MyButtonConfig myButtonConfig = new MyButtonConfig("Ajukan Sidang", "/img/add_item.png");
			myButtonConfig.setParent(tabpanelSidang);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Skripsi skripsi = new Skripsi();
					skripsi.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
					skripsi.setJudul(mahasiswaRequestTugasAkhir.getJudul());
					skripsi.setPembimbing(mahasiswaRequestTugasAkhir.getDosen1());
					skripsi.setKetuaSidang(mahasiswaRequestTugasAkhir.getDosen2());
					skripsi.setPembimbing3(mahasiswaRequestTugasAkhir.getDosen3());
					skripsi.setAwalBimbingan(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan());
					skripsi.setAkhirBimbingan(mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan());
					SkripsiAction.onAddExternal(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Common.clear(tabpanelSidang);
									initSidang(mahasiswaRequestTugasAkhir, tabpanelSidang, groupbox);
								}
							});

							try {

								Skripsi skripsi = (Skripsi) arg0.getData();
								if (skripsi.getMahasiswaRequestTugasAkhir() == null) {
									skripsi.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
									Common.refreshUpdate(skripsi);
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AktifitasTugasAkhirHelper.java:640");
							}
						}
					}, skripsi, mahasiswaRequestTugasAkhir.getMahasiswa());
				}
			});
		} else {
			MyWindow myWindow = SkripsiAction.initComponenAddExternal(skripsi,
					mahasiswaRequestTugasAkhir.getMahasiswa());
			myWindow.setTitle("");
			myWindow.setClosable(false);
			myWindow.setBorder("none");
			tabpanelSidang.appendChild(myWindow);
			SkripsiAction skripsiAction = (SkripsiAction) myWindow.getAttribute("skripsiAction");
			groupbox.setAttribute("skripsiAction", skripsiAction);
		}
	}

	private Toolbar toolbar;

	/**
	 * Menambahkan tab "Laporan" (dimuat lazy) berisi pratinjau PDF laporan
	 * {@code lembar_konsultasi_bimbingan} untuk {@code mahasiswaRequestTugasAkhir}, lengkap dengan
	 * toolbar ekspor ({@code CommonReport.exportReport}). Parameter laporan disusun dari id
	 * bimbingan, tanggal awal/akhir bimbingan, dan properti entitas {@link MahasiswaRequestTugasAkhir}
	 * lainnya via {@link Common#insertProperty}.
	 *
	 * @param tabbox                      tabbox tujuan penambahan tab
	 * @param index                       indeks/urutan tab
	 * @param mahasiswaRequestTugasAkhir  konteks bimbingan tugas akhir yang laporannya dicetak
	 */
	public void initCetak(MyButtonTabbox tabbox, int index,
			final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir) {
		tabbox.tambahTabLazy(index, "Laporan", "/img/svg/file-report.svg",
				new MyButtonTabbox.PemuatTab() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void muat(final Div tabpanelMonitor) throws Exception {
				tabpanelMonitor.setStyle("overflow:auto;min-height:700px;");
				final Map parameters = ais.common.HashMapGenerator.getRand();

				parameters.put("perkuliahan",
						mahasiswaRequestTugasAkhir == null || mahasiswaRequestTugasAkhir.getId() == null ? -1L : mahasiswaRequestTugasAkhir.getId());

				parameters.put("awal", mahasiswaRequestTugasAkhir == null
						|| mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan() == null ? ""
								: Common.dateFormat2.get().format(mahasiswaRequestTugasAkhir.getTanggalAwalBimbingan()));

				parameters.put("akhir", mahasiswaRequestTugasAkhir == null
						|| mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan() == null ? ""
								: Common.dateFormat2.get().format(mahasiswaRequestTugasAkhir.getTanggalAkhirBimbingan()));

				Common.insertProperty(MahasiswaRequestTugasAkhir.class, mahasiswaRequestTugasAkhir, parameters, "", 2);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(tabpanelMonitor);
				borderlayout.setHeight("700px");

				final Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						File file = Report.generateFileReport(Report.PDF, parameters, "lembar_konsultasi_bimbingan",
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
				}, "lembar_konsultasi_bimbingan", null, eventListener));

				eventListener.onEvent(null);
			}
		});
	}

}
