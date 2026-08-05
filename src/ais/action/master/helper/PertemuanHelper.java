package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.North;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.sekolah.helper.AbsensiSiswaHelper;
import ais.action.master.sekolah.helper.PertemuanPunyaUjianSiswaHelper;
import ais.common.AIGenerator;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PertemuanHelper {

	private DataLoader dataLoader;
	public MyWindow window = null;
	public boolean tampilSelesai = true;
	private Pertemuan pertemuan;

	private VideoPertemuanHelper videoPertemuanHelper;
	private AudioPertemuanHelper audioPertemuanHelper;
	private FilePerkuliahanHelper filePerkuliahanHelper;

	private PertemuanPunyaUjianHelper pertemuanPunyaUjianHelper;
	private PertemuanPunyaUjianSiswaHelper pertemuanPunyaUjianSiswaHelper;
	private PertemuanPunyaDiskusiHelper pertemuanPunyaDiskusiHelper;
	private PertemuanPunyaHasilHelper pertemuanPunyaHasilHelper;
	private TugasMandiriHelper tugasMandiriHelper;
	private AbsensiHelper absensiHelper;
	private AbsensiSiswaHelper absensiSiswaHelper;

	private Mahasiswa mahasiswa;

	private Textbox catatan;
	private int index = 0;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private ais.ui.util.MyButtonTabbox btnTab;
	private TugasPertemuan selectedTugasPertemuan = null;
	private TugasKelompok selectedTugasKelompok = null;

	public Long selectedDiskusi = null;
	private PertemuanFileContent selectedPertemuanFileContent = null;
	private AudioPertemuan selectedAudioPertemuan = null;
	private VideoPertemuan selectedVideoPertemuan = null;
	private Tbmuser tbmuser;

	public PertemuanHelper() {
		this(Common.getCurrentUser());
	}

	public PertemuanHelper(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
		if (tbmuser != null) {
			mahasiswa = tbmuser.getMahasiswa();
			biodataCalonMahasiswa = tbmuser.getBiodataCalonMahasiswa();
		}
		tugasMandiriHelper = new TugasMandiriHelper(mahasiswa, biodataCalonMahasiswa);
		filePerkuliahanHelper = new FilePerkuliahanHelper(mahasiswa, biodataCalonMahasiswa);
		// FIX NPE: tbmuser bisa null di konstruktor ini (parameter tbmuser diteruskan apa adanya,
		// tidak selalu berasal dari Common.getCurrentUser() yang sudah dicek pemanggilnya).
		videoPertemuanHelper = new VideoPertemuanHelper(mahasiswa == null && biodataCalonMahasiswa == null
				&& (tbmuser == null || (tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null)), false);
		audioPertemuanHelper = new AudioPertemuanHelper(mahasiswa == null && biodataCalonMahasiswa == null
				&& (tbmuser == null || (tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null)), false);
		absensiHelper = new AbsensiHelper(mahasiswa, biodataCalonMahasiswa);
		pertemuanPunyaUjianHelper = new PertemuanPunyaUjianHelper(mahasiswa, biodataCalonMahasiswa);

		pertemuanPunyaUjianSiswaHelper = new PertemuanPunyaUjianSiswaHelper(tbmuser.getSiswa(),
				tbmuser.getCalonSiswa());

		absensiSiswaHelper = new AbsensiSiswaHelper(tbmuser.getSiswa(), tbmuser.getCalonSiswa());

		pertemuanPunyaDiskusiHelper = new PertemuanPunyaDiskusiHelper(mahasiswa,
				tbmuser == null ? null : tbmuser.ambilDosen(), biodataCalonMahasiswa,
				tbmuser == null ? null : tbmuser.getSiswa(), tbmuser == null ? null : tbmuser.getCalonSiswa());
		pertemuanPunyaHasilHelper = new PertemuanPunyaHasilHelper();
	}

	public PertemuanHelper(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa, boolean tampilSelesai) {
		this(mahasiswa, biodataCalonMahasiswa);
		this.tampilSelesai = tampilSelesai;
	}

	public PertemuanHelper(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		Tbmuser tbmuser = Common.getCurrentUser();
		this.tbmuser = tbmuser;
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			mahasiswa = tbmuser.getMahasiswa();
		}
		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			biodataCalonMahasiswa = tbmuser.getBiodataCalonMahasiswa();
		}
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		tugasMandiriHelper = new TugasMandiriHelper(mahasiswa, biodataCalonMahasiswa);
		filePerkuliahanHelper = new FilePerkuliahanHelper(mahasiswa, biodataCalonMahasiswa);
		// FIX NPE: tbmuser bisa null (mis. akses anonim/edge-case sesi) -- konstruktor lain di
		// bawah (absensiSiswaHelper dst.) sudah menjaga tbmuser null, tapi 2 baris ini belum.
		videoPertemuanHelper = new VideoPertemuanHelper(mahasiswa == null && biodataCalonMahasiswa == null
				&& (tbmuser == null || (tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null)), false);
		audioPertemuanHelper = new AudioPertemuanHelper(mahasiswa == null && biodataCalonMahasiswa == null
				&& (tbmuser == null || (tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null)), false);
		absensiHelper = new AbsensiHelper(mahasiswa, biodataCalonMahasiswa);
		absensiSiswaHelper = new AbsensiSiswaHelper(tbmuser == null ? null : tbmuser.getSiswa(),
				tbmuser == null ? null : tbmuser.getCalonSiswa());
		pertemuanPunyaUjianHelper = new PertemuanPunyaUjianHelper(mahasiswa, biodataCalonMahasiswa);
		pertemuanPunyaUjianSiswaHelper = new PertemuanPunyaUjianSiswaHelper(tbmuser == null ? null : tbmuser.getSiswa(),
				tbmuser == null ? null : tbmuser.getCalonSiswa());

		pertemuanPunyaDiskusiHelper = new PertemuanPunyaDiskusiHelper(mahasiswa,
				tbmuser == null ? null : tbmuser.ambilDosen(), biodataCalonMahasiswa,
				tbmuser == null ? null : tbmuser.getSiswa(), tbmuser == null ? null : tbmuser.getCalonSiswa());
		pertemuanPunyaHasilHelper = new PertemuanPunyaHasilHelper();
	}

	private void initTugas(final Component tabpanelFileTugasPertemuan, boolean tampilInfo) throws Exception {
		initTugas(tabpanelFileTugasPertemuan, false, tampilInfo);
	}

	private void initTugas(final Component tabpanelFileTugasPertemuan, final boolean selectTerakhir,
			final boolean tampilInfo) throws Exception {
		if (tabpanelFileTugasPertemuan != null) {
			Common.clear(tabpanelFileTugasPertemuan);
		}
		Borderlayout borderlayout1 = new Borderlayout();
		borderlayout1.setParent(tabpanelFileTugasPertemuan);

		Center center1 = new Center();
		center1.setParent(borderlayout1);
		ais.ui.util.ZkCompat.setFlex(center1, true);

		Tabbox tabbox1 = new Tabbox();
		tabbox1.setParent(center1);

		final Tabs tabs1 = new Tabs();
		tabs1.setParent(tabbox1);

		Collection<Tugas> tugases = pertemuan.ambilTugasTotalSemua().values();

		final Tabpanels tabpanels1 = new Tabpanels();
		tabpanels1.setParent(tabbox1);

		boolean buka = false;
		if (!tugases.isEmpty() && pertemuan.getJudultugas().trim().isEmpty()) {
			buka = true;
//			System.out.println("Tugas utama tidak ditampilkan karena sudah ada sub tugas");
		} else {
			final Tab tabTugas1 = new Tab(pertemuan.getJudultugas().trim().isEmpty() ? "(Tidak ada tugas)"
					: pertemuan.getJudultugas().length() > 30 ? pertemuan.getJudultugas().substring(0, 30) + "..."
							: pertemuan.getJudultugas(),
					"/img/Status-mail-task-icon.png");

			tabTugas1.setParent(tabs1);

			Tabpanel tabpanelUtama1 = new ais.ui.util.MyTabpanel();
			tabpanelUtama1.setParent(tabpanels1);

			tugasMandiriHelper.createTugas(pertemuan, tabpanelUtama1, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					initTugas(tabpanelFileTugasPertemuan, selectTerakhir);
				}
			}, tampilInfo);
		}

		Tab tabTugas = null;
		Tabpanel tabpanelUtamaU = null;
		Tugas tugasPertemuanU = null;
		for (final Tugas tugasPertemuan : tugases) {
			if (tugasPertemuan != null && tugasPertemuan.getId() != null) {
				tugasPertemuanU = tugasPertemuan;

				if (tugasPertemuan instanceof TugasPertemuan) {
					((TugasPertemuan) tugasPertemuan).setPertemuan(pertemuan.getId());
				} else if (tugasPertemuan instanceof TugasKelompok) {
					((TugasKelompok) tugasPertemuan).setPertemuan(pertemuan.getId());
				}

				String icon = "Status-mail-task-icon.png";
				if (tugasPertemuan instanceof TugasKelompok) {
					icon = "Healthcare-Groups-icon.png";
				}

				tabTugas = new Tab(tugasPertemuan.getJudultugas().trim().isEmpty() ? "(Tidak ada tugas)"
						: tugasPertemuan.getJudultugas().length() > 30
								? tugasPertemuan.getJudultugas().substring(0, 30) + "..."
								: tugasPertemuan.getJudultugas(),
						"/img/" + icon);
				tabTugas.setVisible(!tugasPertemuan.getJudultugas().trim().isEmpty());
				tabTugas.setParent(tabs1);
				final Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
				tabpanelUtama.setParent(tabpanels1);
				tabpanelUtamaU = tabpanelUtama;
				if (selectedTugasPertemuan != null && selectedTugasPertemuan.getId() != null
						&& selectedTugasPertemuan.getId().equals(tugasPertemuan.getId())) {

					TugasMandiriHelper tugasMandiriHelper = new TugasMandiriHelper(mahasiswa, biodataCalonMahasiswa);
					tugasMandiriHelper.createTugas(tugasPertemuan, tabpanelUtama, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							initTugas(tabpanelFileTugasPertemuan, selectTerakhir);
						}
					}, tampilInfo);
					tabTugas.setSelected(true);
				} else if (selectedTugasKelompok != null && selectedTugasKelompok.getId() != null
						&& selectedTugasKelompok.getId().equals(tugasPertemuan.getId())) {
					TugasKelompokHelper tugasKelompokHelper = new TugasKelompokHelper(mahasiswa, biodataCalonMahasiswa);
					tugasKelompokHelper.tampilanTugas(selectedTugasKelompok, tabpanelUtama, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							initTugas(tabpanelFileTugasPertemuan, selectTerakhir);
						}
					});
					tabTugas.setSelected(true);
				} else {
					EventListener eventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabpanelUtama.getChildren().isEmpty()) {

								if (tugasPertemuan instanceof TugasPertemuan) {
									TugasMandiriHelper tugasMandiriHelper = new TugasMandiriHelper(mahasiswa,
											biodataCalonMahasiswa);
									tugasMandiriHelper.createTugas(tugasPertemuan, tabpanelUtama, new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											initTugas(tabpanelFileTugasPertemuan, selectTerakhir);
										}
									}, tampilInfo);
								} else if (tugasPertemuan instanceof TugasKelompok) {
									TugasKelompokHelper tugasKelompokHelper = new TugasKelompokHelper(mahasiswa,
											biodataCalonMahasiswa);
									tugasKelompokHelper.tampilanTugas((TugasKelompok) tugasPertemuan, tabpanelUtama,
											new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													initTugas(tabpanelFileTugasPertemuan, selectTerakhir);
												}
											});
								}
							}
						}
					};

					tabTugas.addEventListener("onClick", eventListener);

					if (buka) {
						buka = false;
						Common.createDefaultTimer(eventListener);
					}
				}
			}
		}
		tugases = null;

		if (selectedTugasPertemuan == null) {
			if (tabTugas != null && selectTerakhir) {
				tabTugas.setSelected(true);
				TugasMandiriHelper tugasMandiriHelper = new TugasMandiriHelper(mahasiswa, biodataCalonMahasiswa);
				tugasMandiriHelper.createTugas(tugasPertemuanU, tabpanelUtamaU, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initTugas(tabpanelFileTugasPertemuan, true);
					}
				}, tampilInfo);
			}
		}
		if (selectedTugasKelompok == null) {
			if (tabTugas != null && selectTerakhir) {
				tabTugas.setSelected(true);
				TugasKelompokHelper tugasKelompokHelper = new TugasKelompokHelper(mahasiswa, biodataCalonMahasiswa);
				tugasKelompokHelper.tampilanTugas((TugasKelompok) tugasPertemuanU, tabpanelUtamaU, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initTugas(tabpanelFileTugasPertemuan, true);
					}
				});
			}
		}

		final Tab tabTugasBaru = new Tab("Tugas Individu Baru", "/img/add_item.png");
		tabTugasBaru.setVisible(mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
				&& tbmuser.getSiswa() == null);
		tabTugasBaru.setParent(tabs1);
		tabTugasBaru.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final TugasPertemuan tugasPertemuan = new TugasPertemuan();
				tugasPertemuan.setPertemuan(pertemuan.getId());
				tugasPertemuan.setFormatNilai(pertemuan.getFormatNilai());
				tugasPertemuan.setSyaratMengumpulkanTugas(pertemuan.getSyaratMengumpulkanTugas());
				tugasPertemuan.setProsentase(pertemuan.getProsentase());
				tugasPertemuan.setJudultugas("Tugas individu pertemuan ke " + pertemuan.getPertemuanKe());
				Session session = HibernateUtil.currentSession();
				session.save(tugasPertemuan);
				session.flush();

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Session session = HibernateUtil.currentSession();
						pertemuan.reInitTugasPertemuan(session);

						selectedTugasKelompok = null;
						selectedTugasPertemuan = tugasPertemuan;
						initTugas(tabpanelFileTugasPertemuan, false);
					}
				});
			}
		});

		if (pertemuan != null && (pertemuan.getPerkuliahan() != null || pertemuan.getJadwalPelajaran() != null
				|| pertemuan.getKelompokKkn() != null || pertemuan.getKelompokPkl() != null)) {
			final Tab tabTugasKelompokBaru = new Tab("Tugas Kelompok Baru", "/img/add_item.png");
			tabTugasKelompokBaru.setVisible(mahasiswa == null && biodataCalonMahasiswa == null
					&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null);
			tabTugasKelompokBaru.setParent(tabs1);
			tabTugasKelompokBaru.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					final TugasKelompok tugasKelompok = new TugasKelompok();
					tugasKelompok.setPertemuan(pertemuan.getId());
					tugasKelompok.setFormatNilai(pertemuan.getFormatNilai());
					tugasKelompok.setSyaratMengumpulkanTugas(pertemuan.getSyaratMengumpulkanTugas());
					tugasKelompok.setProsentase(pertemuan.getProsentase());
					tugasKelompok.setJudultugas("Tugas kelompok pertemuan ke " + pertemuan.getPertemuanKe());
					Session session = HibernateUtil.currentSession();
					session.save(tugasKelompok);
					session.flush();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Session session = HibernateUtil.currentSession();
							pertemuan.reInitTugasKelompok(session);

							selectedTugasPertemuan = null;
							selectedTugasKelompok = tugasKelompok;
							initTugas(tabpanelFileTugasPertemuan, false);
						}
					});
				}
			});
		}
	}

	private void tampilanDesktop(org.zkoss.zk.ui.Component container) throws Exception {
		TreeMap<Long, TugasPertemuan> tugasPertemuansa = pertemuan.ambilTugasPertemuanTotal();
		final int jumlahTugas = (pertemuan.getJudultugas().isEmpty() ? 0 : 1) + tugasPertemuansa.size();
		tugasPertemuansa = null;

		// Tinggi: 100vh dikurangi South + window chrome (~70px).
		// Info pertemuan hanya ditampilkan pada tab Dasbor; tab lain langsung menampilkan
		// kontennya agar panel ringkasan tidak berulang di setiap tab.
		btnTab = ais.ui.util.MyButtonTabbox.buat(container, "calc(100vh - 70px)",
				new int[] { index });

		// Tab 9: Dasbor — didaftarkan PERTAMA agar tampil paling kiri.
		// Berisi info VoPembelajaran + ringkasan data semua tab.
		btnTab.tambahTabLazy(9, "Dasbor", "/img/svg/dashboard-chart.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						initDasbor(panel);
					}
				});

		// Tab 0: Kehadiran
		btnTab.tambahTabLazy(0, "Kehadiran", "/img/svg/person-check.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						if (pertemuan.getJadwalPelajaran() != null || pertemuan.getJadwalUjianPSB() != null
								|| (pertemuan.getFormulirKegiatan() != null
										&& pertemuan.getFormulirKegiatan().getSekolah() != null)) {
							absensiSiswaHelper.mainInit(pertemuan, panel, false);
						} else {
							absensiHelper.mainInit(pertemuan, panel, false);
						}
					}
				});

		// Tab 1: Pembelajaran (catatan/materi belajar)
		btnTab.tambahTabLazy(1, "Pembelajaran", "/img/svg/book.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						panel.appendChild(initCatatan(false));
					}
				});

		// Tab 2: Materi (file konten pertemuan)
		btnTab.tambahTabLazy(2,
				"Materi (" + pertemuan.ambilJumlahPertemuanFileContent() + ")",
				"/img/svg/folder-open-thin.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						filePerkuliahanHelper.createFile(pertemuan, null, null, null, panel,
								selectedPertemuanFileContent);
					}
				});

		// Tab 3: Tugas
		btnTab.tambahTabLazy(3, "Tugas (" + jumlahTugas + ")", "/img/svg/card-checklist.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						initTugas(panel, false);
					}
				});

		// Tab 4: Audio
		btnTab.tambahTabLazy(4,
				"Audio (" + pertemuan.ambilJumlahAudioPertemuan() + ")",
				"/img/svg/file-audio-thin.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						audioPertemuanHelper.display(pertemuan, null, null, panel, selectedAudioPertemuan);
					}
				});

		// Tab 5: Video
		btnTab.tambahTabLazy(5,
				"Video (" + pertemuan.ambilJumlahVideoPertemuan() + ")",
				"/img/svg/camera-video.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						videoPertemuanHelper.display(pertemuan, null, null, panel, selectedVideoPertemuan);
					}
				});

		// Tab 6: Ujian
		btnTab.tambahTabLazy(6,
				"Ujian (" + pertemuan.ambilJumlahPertemuanPunyaUjian() + ")",
				"/img/svg/pencil-square.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						if (pertemuan.getJadwalPelajaran() != null || pertemuan.getJadwalUjianPSB() != null) {
							pertemuanPunyaUjianSiswaHelper.display(pertemuan, panel);
						} else {
							pertemuanPunyaUjianHelper.display(pertemuan, panel);
						}
					}
				});

		// Tab 7: Diskusi
		btnTab.tambahTabLazy(7,
				"Diskusi (" + pertemuan.ambilJumlahPertemuanPunyaDiskusi() + ")",
				"/img/svg/comment-2-text-line.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						pertemuanPunyaDiskusiHelper.display(pertemuan, panel, false, selectedDiskusi);
					}
				});

		// Tab 8: Hasil, Evaluasi, Kusioner
		btnTab.tambahTabLazy(8, "Hasil, Evaluasi, Kusioner", "/img/svg/chart-line.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					@Override
					public void muat(Div panel) throws Exception {
						pertemuanPunyaHasilHelper.display(pertemuan, panel);
					}
				});

		// Paksa tab yang sesuai terpilih SETELAH semua tab terdaftar.
		// MyButtonTabbox.buat() menerima tabAktif[] sebagai hint, tapi
		// buatTombolDanPanel() meng-override-nya ke tab pertama jika target
		// belum ada di panelMap saat tab pertama ditambahkan.
		btnTab.pilih(index);
	}

	private void addInfoPertemuan(Div panel) throws Exception {
		Div infoWrap = new Div();
		infoWrap.setStyle("border-bottom:1px solid #dee2e6;padding:4px 8px 8px;margin-bottom:8px;");
		infoWrap.appendChild(DashboardTimelinePertemuan.displayInfoPertemuan(pertemuan));
		infoWrap.setParent(panel);
	}

	private void initDasbor(Div panel) throws Exception {
		addInfoPertemuan(panel);

		// === Kumpulkan data ===

		// Total peserta kelas
		int totalMahasiswa = 0;
		int totalSiswa = 0;
		int totalDosen = 0;
		int totalGuru = 0;
		try { totalMahasiswa = pertemuan.ambilMahasiswa().size(); } catch (Exception ignored) {}
		try {
			if (pertemuan.getJadwalPelajaran() != null)
				totalSiswa = pertemuan.ambilSiswa().size();
		} catch (Exception ignored) {}
		try { totalDosen = pertemuan.ambilDosen().size(); } catch (Exception ignored) {}
		try { totalGuru = pertemuan.ambilGuru().size(); } catch (Exception ignored) {}
		int totalPeserta = totalMahasiswa + totalSiswa;

		java.util.Map<String,Integer> statusMahasiswa = null;
		java.util.Map<String,Integer> statusSiswa = null;
		java.util.Map<String,Integer> statusMahasiswaSiswa = null;
		java.util.Map<String,Integer> statusDosen = null;
		java.util.Map<String,Integer> statusGuru = null;
		try { statusMahasiswa = hitungStatusDenganSuffix("mahasiswa"); } catch (Exception ignored) {}
		try { statusSiswa = hitungStatusDenganSuffix("siswa"); } catch (Exception ignored) {}
		statusMahasiswaSiswa = gabungStatus(statusMahasiswa, statusSiswa);
		try { statusDosen = pertemuan.hitungStatusDosen(); } catch (Exception ignored) {}
		try { statusGuru = pertemuan.hitungStatusGuru(); } catch (Exception ignored) {}

		if (totalDosen == 0 && statusDosen != null) {
			for (Integer v : statusDosen.values()) {
				totalDosen += v == null ? 0 : v.intValue();
			}
		}
		if (totalGuru == 0 && statusGuru != null) {
			for (Integer v : statusGuru.values()) {
				totalGuru += v == null ? 0 : v.intValue();
			}
		}

		// Kehadiran: M=Masuk, S=Sakit, I=Izin, A=Alpa
		int cMasuk = 0, cSakit = 0, cIzin = 0, cAlpa = 0;
		try {
			java.util.Map<String,Integer> sm = statusMahasiswaSiswa;
			if (sm != null) {
				for (java.util.Map.Entry<String,Integer> en : sm.entrySet()) {
					int v = en.getValue() == null ? 0 : en.getValue();
					if ("M".equals(en.getKey()))      cMasuk  += v;
					else if ("S".equals(en.getKey())) cSakit  += v;
					else if ("I".equals(en.getKey())) cIzin   += v;
					else if ("A".equals(en.getKey())) cAlpa   += v;
				}
			}
		} catch (Exception ignored) {}
		int cBelumAbsen = Math.max(0, totalPeserta - cMasuk - cSakit - cIzin - cAlpa);

		// Tugas: per-TugasPertemuan kumpulkan nama + jumlah terkumpul
		TreeMap<Long,TugasPertemuan> tugasMap = null;
		try { tugasMap = pertemuan.ambilTugasPertemuanTotal(); } catch (Exception ignored) {}
		List<String>  tugasNama      = new ArrayList<String>();
		List<Integer> tugasTerkumpul = new ArrayList<Integer>();
		List<Integer> tugasBelum     = new ArrayList<Integer>();
		if (tugasMap != null) {
			int idx = 0;
			for (java.util.Map.Entry<Long,TugasPertemuan> en : tugasMap.entrySet()) {
				idx++;
				TugasPertemuan tp = en.getValue();
				String judul = null;
				try { judul = tp.getJudultugas(); } catch (Exception ignored) {}
				if (judul == null || judul.trim().isEmpty()) judul = "Tugas " + idx;
				if (judul.length() > 30) judul = judul.substring(0, 27) + "...";
				int kumpul = 0;
				try { kumpul = tp.ambilJumlahTugasFileContent(); } catch (Exception ignored) {}
				tugasNama.add(judul);
				tugasTerkumpul.add(kumpul);
				tugasBelum.add(Math.max(0, totalPeserta - kumpul));
			}
		}
		int totalTerkumpul = 0;
		for (int v : tugasTerkumpul) totalTerkumpul += v;
		int totalBelumTerkumpul = 0;
		for (int v : tugasBelum) totalBelumTerkumpul += v;
		int jTugas = (pertemuan.getJudultugas() != null && !pertemuan.getJudultugas().isEmpty() ? 1 : 0)
				   + (tugasMap != null ? tugasMap.size() : 0);

		// Ujian: per-PertemuanPunyaUjian ikut vs belum
		List<String>  ujianNama = new ArrayList<String>();
		List<Integer> ujianIkut = new ArrayList<Integer>();
		List<Integer> ujianBelum = new ArrayList<Integer>();
		try {
			TreeMap<Long,PertemuanPunyaUjian> ujianMap =
					pertemuan.ambilPertemuanPunyaUjianTotal(null);
			if (ujianMap != null) {
				int idx = 0;
				for (java.util.Map.Entry<Long,PertemuanPunyaUjian> en : ujianMap.entrySet()) {
					idx++;
					PertemuanPunyaUjian ppu = en.getValue();
					String nama = null;
					try { nama = ppu.getNama(); } catch (Exception ignored) {}
					if (nama == null || nama.trim().isEmpty()) nama = "Ujian " + idx;
					if (nama.length() > 30) nama = nama.substring(0, 27) + "...";
					int sudah = 0;
					try { sudah = ppu.ambilJumlahHasilUjianMahasiswaTelahIkut(false); } catch (Exception ignored) {}
					ujianNama.add(nama);
					ujianIkut.add(sudah);
					ujianBelum.add(Math.max(0, totalPeserta - sudah));
				}
			}
		} catch (Exception ignored) {}
		int jUjian = ujianNama.size();
		int totalMengerjakanUjian = 0;
		for (int v : ujianIkut) totalMengerjakanUjian += v;
		int totalBelumMengerjakanUjian = 0;
		for (int v : ujianBelum) totalBelumMengerjakanUjian += v;

		// Konten
		int cMateri = 0, cAudio = 0, cVideo = 0, cDiskusi = 0;
		try { cMateri  = pertemuan.ambilJumlahPertemuanFileContent(); }   catch (Exception ignored) {}
		try { cAudio   = pertemuan.ambilJumlahAudioPertemuan(); }         catch (Exception ignored) {}
		try { cVideo   = pertemuan.ambilJumlahVideoPertemuan(); }         catch (Exception ignored) {}
		try {
			Number nd = pertemuan.ambilJumlahPertemuanPunyaDiskusi();
			if (nd != null) cDiskusi = nd.intValue();
		} catch (Exception ignored) {}

		// === KPI Cards — individual clickable per kategori ===
		org.zkoss.zul.Groupbox gbKpi = new org.zkoss.zul.Groupbox();
		gbKpi.setMold("3d");
		new org.zkoss.zul.Caption("Ringkasan Cepat").setParent(gbKpi);
		gbKpi.setStyle("margin:4px 8px 8px;");
		Div kpiRow = new Div();
		kpiRow.setStyle("display:flex;flex-wrap:wrap;");
		kpiRow.setParent(gbKpi);
		addKpiCard(kpiRow, "Total Peserta", String.valueOf(totalPeserta), "terdaftar",        "#1877f2", 0);
		addKpiCard(kpiRow, "Hadir",         String.valueOf(cMasuk),       "dari " + totalPeserta, "#42b72a", 0);
		addKpiCard(kpiRow, "Belum Absen",   String.valueOf(cBelumAbsen),  "perlu absen",       "#f7b928", 0);
		addKpiCard(kpiRow, "Tugas",         String.valueOf(jTugas),       "tugas aktif",       "#e4496b", 3);
		addKpiCard(kpiRow, "Ujian",         String.valueOf(jUjian),       "kuis/ujian",        "#8b5cf6", 6);
		addKpiCard(kpiRow, "Konten",        String.valueOf(cMateri + cAudio + cVideo), "materi+audio+video", "#06b6d4", 2);
		gbKpi.setParent(panel);

		// === Kehadiran — akses cepat sesuai jenis peserta/pengajar ===
		org.zkoss.zul.Groupbox gbHadir = buatGbClickable("Kehadiran ↗", 0);
		if (totalPeserta > 0) {
			Grid gHadir = new Grid();
			gHadir.setWidth("100%");
			gHadir.setSclass("ais-form-grid");
			Rows rHadir = new Rows();
			rHadir.setParent(gHadir);
			if (totalMahasiswa > 0) {
				tambahBarisClickable(rHadir, "Kehadiran Mahasiswa",
						ringkasStatus(statusMahasiswa, totalMahasiswa), 0);
			}
			if (totalDosen > 0) {
				tambahBarisClickable(rHadir, "Kehadiran Dosen",
						ringkasStatus(statusDosen, totalDosen), 0);
			}
			if (totalSiswa > 0) {
				tambahBarisClickable(rHadir, "Kehadiran Siswa",
						ringkasStatus(statusSiswa, totalSiswa), 0);
			}
			if (totalGuru > 0) {
				tambahBarisClickable(rHadir, "Kehadiran Guru",
						ringkasStatus(statusGuru, totalGuru), 0);
			}
			gHadir.setParent(gbHadir);
		} else {
			new Label("Belum ada data peserta.").setParent(gbHadir);
		}
		gbHadir.setParent(panel);

		// === Tugas — stacked bar, klik → tab Tugas ===
		if (!tugasNama.isEmpty()) {
			org.zkoss.zul.Groupbox gbTugas = buatGbClickable("Pengumpulan Tugas ↗", 3);
			String[] catsTugas = tugasNama.toArray(new String[0]);
			double[][] valsTugas = new double[catsTugas.length][2];
			for (int i = 0; i < catsTugas.length; i++) {
				valsTugas[i][0] = tugasTerkumpul.get(i);
				valsTugas[i][1] = tugasBelum.get(i);
			}
			new org.zkoss.zul.Html(HtmlChartHelper.stackedBar(
				null, null, catsTugas,
				new String[]{"Sudah Kumpul","Belum Kumpul"},
				valsTugas, new String[]{"#42b72a","#e4e6eb"}
			)).setParent(gbTugas);
			gbTugas.setParent(panel);
		}

		// === Ujian — stacked bar, klik → tab Ujian ===
		if (!ujianNama.isEmpty()) {
			org.zkoss.zul.Groupbox gbUjian = buatGbClickable("Partisipasi Ujian / Kuis ↗", 6);
			String[] catsUjian = ujianNama.toArray(new String[0]);
			double[][] valsUjian = new double[catsUjian.length][2];
			for (int i = 0; i < catsUjian.length; i++) {
				valsUjian[i][0] = ujianIkut.get(i);
				valsUjian[i][1] = ujianBelum.get(i);
			}
			new org.zkoss.zul.Html(HtmlChartHelper.stackedBar(
				null, null, catsUjian,
				new String[]{"Sudah Ikut","Belum Ikut"},
				valsUjian, new String[]{"#8b5cf6","#e4e6eb"}
			)).setParent(gbUjian);
			gbUjian.setParent(panel);
		}

		// === Radar — kelengkapan, klik → tab Kehadiran sebagai starting point ===
		{
			org.zkoss.zul.Groupbox gbRadar = buatGbClickable("Kelengkapan Pertemuan ↗", 0);
			double rKehadiran = totalPeserta > 0 ? Math.min(10.0, (cMasuk * 10.0) / totalPeserta) : 0;
			new org.zkoss.zul.Html(HtmlChartHelper.radar(
				null,
				"Skor kelengkapan setiap dimensi (skala 0–10). Klik untuk navigasi.",
				new String[]{"Kehadiran","Materi","Tugas","Audio","Video","Ujian","Diskusi"},
				new String[]{"Pertemuan ini"},
				new double[][]{{
					rKehadiran,
					Math.min(10.0, cMateri  * 2.0),
					Math.min(10.0, jTugas   * 3.0),
					Math.min(10.0, cAudio   * 2.0),
					Math.min(10.0, cVideo   * 2.0),
					Math.min(10.0, jUjian   * 3.0),
					Math.min(10.0, cDiskusi * 3.0)
				}},
				new String[]{"#1877f2"}, 10.0
			)).setParent(gbRadar);
			gbRadar.setParent(panel);
		}

		// === Konten — bar horizontal, klik → tab Materi ===
		if (cMateri + cAudio + cVideo > 0) {
			org.zkoss.zul.Groupbox gbKonten = buatGbClickable("Konten Pertemuan ↗", 2);
			new org.zkoss.zul.Html(HtmlChartHelper.barHorizontal(
				null, null,
				new String[]{"Materi (berkas)","Audio","Video","Diskusi"},
				new double[]{cMateri, cAudio, cVideo, cDiskusi},
				"#06b6d4"
			)).setParent(gbKonten);
			gbKonten.setParent(panel);
		}

		// === Informasi Pembelajaran, klik → tab Pembelajaran ===
		ais.database.model.VOPembelajaran vo = pertemuan.ambilVOPembelajaran();
		if (vo != null) {
			org.zkoss.zul.Groupbox gbVo = buatGbClickable("Informasi Pembelajaran ↗", 1);
			gbVo.setStyle("margin:4px 8px 0;cursor:pointer;");
			Grid gVo = new Grid();
			gVo.setWidth("100%");
			gVo.setSclass("ais-form-grid");
			Rows rVo = new Rows();
			rVo.setParent(gVo);
			try {
				String info = vo.infoSimple();
				if (info != null && !info.isEmpty()) tambahBarisLabel(rVo, "Kelas / Mata Kuliah", info);
			} catch (Exception ignored) {}
			String kode = vo.getCourse();
			if (kode != null && !kode.isEmpty()) tambahBarisLabel(rVo, "Kode", kode);
			tambahBarisLabel(rVo, "Total Pertemuan Kelas", vo.ambilJumlahPertemuan() + " pertemuan");
			gVo.setParent(gbVo);
			gbVo.setParent(panel);
		}

		// === Navigasi Tab — semua tab sebagai baris clickable ===
		org.zkoss.zul.Groupbox gbNav = new org.zkoss.zul.Groupbox();
		gbNav.setMold("3d");
		new org.zkoss.zul.Caption("Navigasi Tab").setParent(gbNav);
		gbNav.setStyle("margin:8px;");
		Grid gNav = new Grid();
		gNav.setWidth("100%");
		gNav.setSclass("ais-form-grid");
		Rows rNav = new Rows();
		rNav.setParent(gNav);
		tambahBarisClickable(rNav, "Kehadiran",
				cMasuk + " hadir · " + cSakit + " sakit · " + cIzin + " izin · " + cAlpa + " alpa · " + cBelumAbsen + " belum absen", 0);
		tambahBarisClickable(rNav, "Pembelajaran", "Bahan kajian & catatan", 1);
		tambahBarisClickable(rNav, "Materi",   cMateri + " berkas tersedia", 2);
		tambahBarisClickable(rNav, "Tugas",    jTugas  + " tugas · " + totalTerkumpul
				+ " mengumpulkan · " + totalBelumTerkumpul + " belum mengumpulkan", 3);
		tambahBarisClickable(rNav, "Audio",    cAudio  + " audio", 4);
		tambahBarisClickable(rNav, "Video",    cVideo  + " video", 5);
		tambahBarisClickable(rNav, "Ujian",    jUjian  + " ujian · " + totalMengerjakanUjian
				+ " mengerjakan · " + totalBelumMengerjakanUjian + " belum mengerjakan", 6);
		tambahBarisClickable(rNav, "Diskusi",  cDiskusi + " diskusi", 7);
		tambahBarisClickable(rNav, "Hasil, Evaluasi, Kuesioner", "lihat evaluasi & kuesioner", 8);
		gNav.setParent(gbNav);
		gbNav.setParent(panel);
	}

	private java.util.Map<String,Integer> hitungStatusDenganSuffix(String suffix) {
		java.util.Map<String,Integer> jumlah = new java.util.HashMap<String,Integer>();
		if (pertemuan == null || suffix == null) {
			return jumlah;
		}
		String absensi = pertemuan.getAbsensi();
		if (absensi == null || absensi.trim().isEmpty()) {
			return jumlah;
		}
		String suffixLower = suffix.toLowerCase();
		String[] nilais = absensi.split(";");
		for (String nn : nilais) {
			try {
				if (nn != null && nn.toLowerCase().endsWith(suffixLower)) {
					String[] s = nn.split(",");
					if (s.length > 2 && s[2] != null && !s[2].equalsIgnoreCase("-")) {
						Integer lama = jumlah.get(s[2]);
						jumlah.put(s[2], Integer.valueOf(lama == null ? 1 : lama.intValue() + 1));
					}
				}
			} catch (Exception ignored) {}
		}
		return jumlah;
	}

	private java.util.Map<String,Integer> gabungStatus(java.util.Map<String,Integer> a, java.util.Map<String,Integer> b) {
		java.util.Map<String,Integer> hasil = new java.util.HashMap<String,Integer>();
		tambahStatus(hasil, a);
		tambahStatus(hasil, b);
		return hasil;
	}

	private void tambahStatus(java.util.Map<String,Integer> hasil, java.util.Map<String,Integer> sumber) {
		if (hasil == null || sumber == null) {
			return;
		}
		for (java.util.Map.Entry<String,Integer> en : sumber.entrySet()) {
			if (en.getKey() != null) {
				Integer lama = hasil.get(en.getKey());
				int nilai = en.getValue() == null ? 0 : en.getValue().intValue();
				hasil.put(en.getKey(), Integer.valueOf(lama == null ? nilai : lama.intValue() + nilai));
			}
		}
	}

	private String ringkasStatus(java.util.Map<String,Integer> statusMap, int total) {
		int hadir = nilaiStatus(statusMap, "M");
		int sakit = nilaiStatus(statusMap, "S");
		int izin = nilaiStatus(statusMap, "I");
		int alpa = nilaiStatus(statusMap, "A");
		int belum = Math.max(0, total - hadir - sakit - izin - alpa);
		return hadir + " hadir · " + sakit + " sakit · " + izin + " izin · " + alpa + " alpa · "
				+ belum + " belum absen";
	}

	private int nilaiStatus(java.util.Map<String,Integer> statusMap, String kode) {
		if (statusMap == null || kode == null) {
			return 0;
		}
		Integer nilai = statusMap.get(kode);
		return nilai == null ? 0 : nilai.intValue();
	}

	private org.zkoss.zul.Groupbox buatGbClickable(String judul, final int tabIdx) {
		org.zkoss.zul.Groupbox gb = new org.zkoss.zul.Groupbox();
		gb.setMold("3d");
		new org.zkoss.zul.Caption(judul).setParent(gb);
		gb.setStyle("margin:8px;cursor:pointer;");
		gb.setTooltiptext("Klik untuk membuka tab");
		gb.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				btnTab.pilih(tabIdx);
			}
		});
		return gb;
	}

	private void addKpiCard(Div parent, String label, String nilai, String subtitle,
			String warna, final int tabIdx) {
		Div wrap = new Div();
		wrap.setStyle("cursor:pointer;flex:1 1 0;min-width:110px;");
		wrap.setTooltiptext("→ Tab " + label);
		wrap.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				btnTab.pilih(tabIdx);
			}
		});
		new org.zkoss.zul.Html(HtmlChartHelper.kpiCards(
			new String[]{label}, new String[]{nilai},
			new String[]{subtitle}, null, null, new String[]{warna}
		)).setParent(wrap);
		wrap.setParent(parent);
	}

	private void tambahBarisLabel(Rows rows, String label, String nilai) {
		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		Label lbl = new Label(label);
		lbl.setStyle("font-weight:bold;color:#555;white-space:nowrap;padding-right:8px;");
		lbl.setParent(row);
		new Label(nilai).setParent(row);
	}

	private void tambahBarisClickable(Rows rows, String label, String nilai, final int tabIndex) {
		MyFormRow row = new MyFormRow();
		row.setStyle("cursor:pointer;");
		row.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				btnTab.pilih(tabIndex);
			}
		});
		row.setParent(rows);
		Label lbl = new Label(label);
		lbl.setStyle("font-weight:bold;color:#0070c0;white-space:nowrap;padding-right:8px;text-decoration:underline dotted;");
		lbl.setParent(row);
		new Label(nilai).setParent(row);
	}

	private void tampilanMobile(Center div) throws Exception {
		System.out.println("index = " + index);
		if (index == 0) {
			if (pertemuan.getJadwalPelajaran() != null || pertemuan.getJadwalUjianPSB() != null
					|| (pertemuan.getFormulirKegiatan() != null
							&& pertemuan.getFormulirKegiatan().getSekolah() != null)) {
				absensiSiswaHelper.mainInit(pertemuan, div, true);
			} else {
				absensiHelper.mainInit(pertemuan, div, true);
			}
		} else if (index == 1) {
			div.appendChild(initCatatan(true));
		} else if (index == 2) {
			filePerkuliahanHelper.createFile(pertemuan, null, null, null, Common.tampilanScroll1(div),
					selectedPertemuanFileContent);
		} else if (index == 3) {
			initTugas(div, true);
		} else if (index == 4) {
			audioPertemuanHelper.display(pertemuan, null, null, Common.tampilanScroll1(div), selectedAudioPertemuan);
		} else if (index == 5) {
			videoPertemuanHelper.display(pertemuan, null, null, Common.tampilanScroll1(div), selectedVideoPertemuan);
		} else if (index == 6) {

			if (pertemuan.getJadwalPelajaran() != null || pertemuan.getJadwalUjianPSB() != null) {
				pertemuanPunyaUjianSiswaHelper.display(pertemuan, Common.tampilanScroll1(div));
			} else {
				pertemuanPunyaUjianHelper.display(pertemuan, Common.tampilanScroll1(div));
			}

		} else if (index == 7) {
			pertemuanPunyaDiskusiHelper.display(pertemuan, div, true, selectedDiskusi);
		}
	}

	public void init() throws Exception {
		Borderlayout borderlayout = new MyBorderlayout(true);
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setBorder("none");

		if (Common.isMobile()) {
			tampilanMobile(center);
		} else {
			// Tab buttons di paling atas; info pertemuan menyusul di bawah
			// tab buttons (via tambahHeaderKonten) sehingga navigasi antar-tab
			// selalu mudah dijangkau tanpa diblokir info card.
			tampilanDesktop(center);
		}

		South south = new South();
		south.setVisible(tampilSelesai);
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				dataLoader.loadData(null);
				window.detach();
			}
		});
		cancel.setParent(toolbar);

	}

	@SuppressWarnings("deprecation")
	private Borderlayout initCatatan(boolean tampilInfo) throws Exception {
		if (pertemuan != null) {
			pertemuan.masukkanData("melihat_catatan");
		}
		Borderlayout myBorderlayout = new ais.ui.util.MyBorderlayout();

		Center myCenter = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter, true);
		myCenter.setParent(myBorderlayout);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(myCenter);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();

		rows.setParent(grid);

		if (mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
				&& tbmuser.getSiswa() == null) {

			if (tampilInfo) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(DashboardTimelinePertemuan.displayInfoPertemuan(pertemuan));

			}

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setValign("top");
			row.setParent(rows);

			new MyLabelBoldConfig("Catatan : ").setParent(row);

			row = new MyFormRow();
			row.setParent(rows);

			catatan = new Textbox();
			catatan.setValue(pertemuan.getCatatan());
			catatan.setRows(pertemuan.getPerkuliahan() == null ? 15 : 5);
			catatan.setWidth("90%");
			catatan.setParent(row);

			catatan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.refresh(pertemuan);
					pertemuan.setCatatan(catatan.getValue());
					Common.refreshSaveOrUpdate(pertemuan);
				}
			});

			row = new MyFormRow();
			row.setParent(rows);

			new MyLabelAgakKecil(
					"*) Catatan juga bisa berisi link atau URL yang mengarah ke website, audio, video, atau file tertentu")
					.setParent(row);

			row = new MyFormRow();
			row.setParent(rows);

			Hbox hbox = new Hbox();
			hbox.setParent(row);

			Hbox hbox1 = new Hbox();
			hbox1.setParent(hbox);
			LampiranLain.createDownloadUploadFileLain(hbox1, pertemuan.getId(), LampiranLain.CATATAN_PERKULIAHAN,
					"Catatan", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, true, null, false, false);

			hbox1 = new Hbox();
			hbox1.setParent(hbox);
			LampiranLain.createDownloadUploadFileLain(hbox1, pertemuan.getId(), LampiranLain.LHP,
					pertemuan.getPerkuliahan() == null ? "Laporan Hasil" : LampiranLain.LHP, false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, true, null, false, false);

			if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
					&& tbmuser.getBiodataCalonMahasiswa() == null) {
				MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Generate Catatan",
						"/img/svg/gear.svg");
				toolbarbutton.setParent(hbox);

				String d = "";
				for (Dosen dosen : pertemuan.ambilDosen()) {
					d += d.isEmpty() ? dosen.getNama() : " dan " + dosen.getNama();
				}

				String p = "";
				for (Mahasiswa mahasiswa : pertemuan.ambilMahasiswa()) {
					p += p.isEmpty() ? mahasiswa.getNama() : " dan " + mahasiswa.getNama();
				}

				String tanya = "Buatkan catatan dan berita acara untuk " + pertemuan.info() + ", hari dan tanggal : "
						+ Common.dateFormat6.get().format(pertemuan.getTanggal()) + ", jam waktu mulai "
						+ pertemuan.getWaktuMulai() + " sampai " + pertemuan.getWaktuSelesai() + ", Ruangan : "
						+ (pertemuan.getRuang() == null ? "Belum ditentukan" : pertemuan.getRuang().getNama())
						+ ", Gedung : "
						+ (pertemuan.getRuang() == null || pertemuan.getRuang().getGedung() == null ? ""
								: pertemuan.getRuang().getGedung().getNama())
						+ ", Pengajar : " + d + ", Topik : " + pertemuan.getTopik() + ", Peserta : " + p;

				String tanyaAkhiran = "";
				String tanyaMengajar = " apa saja";
				if (pertemuan.getPerkuliahan() != null && pertemuan.getPerkuliahan().getMatakuliah() != null) {
					tanyaMengajar = " matakuliah " + pertemuan.getPerkuliahan().getMatakuliah().getNama();
					tanyaAkhiran = " pada matakuliah \"" + pertemuan.getPerkuliahan().getMatakuliah().getNama() + "\"";
				} else if (pertemuan.getJadwalPelajaran() != null
						&& pertemuan.getJadwalPelajaran().getMatapelajaran() != null) {
					tanyaMengajar = " matapelajaran " + pertemuan.getJadwalPelajaran().getMatapelajaran().getNama();
					tanyaAkhiran = " pada matapelajaran \""
							+ pertemuan.getJadwalPelajaran().getMatapelajaran().getNama() + "\"";
				}
				toolbarbutton.addEventListener("onClick", AIGenerator.generateApa("Generate Catatan",
						"Informasikan tentang pertemuan kali ini", tanya, false, tanyaAkhiran,
						Common.getKonfigurasi("llama_system_catatan", "Kamu adalah Pengajar atau Dosen atau Guru ")
								.getNilai().trim(),
						null, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								catatan.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
										.replaceAll("\n", "<br>"));

								Common.refresh(pertemuan);
								pertemuan.setCatatan(catatan.getValue());
								Common.refreshSaveOrUpdate(pertemuan);

							}
						}, tanyaMengajar, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								catatan.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
										.replaceAll("\n", "<br>"));
							}
						}));

			}

			// Tombol Catatan (Ubah / Upload Catatan / Generate Catatan) dibuat RATA KIRI.
			rataKiriToolbarLampiran(hbox);

			if (pertemuan.getPerkuliahan() != null) {

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Indikator Capaian : ").setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				final Textbox indikator = new Textbox(pertemuan.getIndikator());
				indikator.setRows(3);
				indikator.setWidth("90%");
				indikator.setParent(row);

				indikator.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.refresh(pertemuan);
						pertemuan.setIndikator(indikator.getValue());
						Common.refreshSaveOrUpdate(pertemuan);
					}
				});

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelAgakKecil("*) Contoh: Mahasiswa mampu menjelaskan dan mendiskusikan ....").setParent(row);

				if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.getSiswa() == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
						&& tbmuser.getBiodataCalonMahasiswa() == null) {

					row = new MyFormRow();
					row.setParent(rows);

					MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Generate Indikator Capaian",
							"/img/svg/gear.svg");
					toolbarbutton.setParent(row);

					String tanya = "Buatkan Indikator Capaian untuk \"" + pertemuan.info() + "\"";

					String tanyaAkhiran = "";
					String tanyaMengajar = " apa saja";
					if (pertemuan.getPerkuliahan() != null && pertemuan.getPerkuliahan().getMatakuliah() != null) {
						tanyaMengajar = " matakuliah " + pertemuan.getPerkuliahan().getMatakuliah().getNama();
						tanyaAkhiran = " pada matakuliah \"" + pertemuan.getPerkuliahan().getMatakuliah().getNama()
								+ "\"";
					} else if (pertemuan.getJadwalPelajaran() != null
							&& pertemuan.getJadwalPelajaran().getMatapelajaran() != null) {
						tanyaMengajar = " matapelajaran " + pertemuan.getJadwalPelajaran().getMatapelajaran().getNama();
						tanyaAkhiran = " pada matapelajaran \""
								+ pertemuan.getJadwalPelajaran().getMatapelajaran().getNama() + "\"";
					}
					toolbarbutton.addEventListener("onClick", AIGenerator.generateApa("Generate Indikator",
							"Indikator tentang apa ?", tanya, true, tanyaAkhiran,
							Common.getKonfigurasi("llama_system_catatan", "Kamu adalah Pengajar atau Dosen atau Guru ")
									.getNilai().trim(),
							null, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									indikator.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
											.replaceAll("\n", "<br>"));

									Common.refresh(pertemuan);
									pertemuan.setIndikator(indikator.getValue());
									Common.refreshSaveOrUpdate(pertemuan);

								}
							}, tanyaMengajar, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									indikator.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
											.replaceAll("\n", "<br>"));
								}
							}));

				}

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Metode Pembelajaran : ").setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				final Textbox metodePembelajaran = new Textbox(pertemuan.getMetodePembelajaran());
				metodePembelajaran.setRows(3);
				metodePembelajaran.setWidth("90%");
				metodePembelajaran.setParent(row);

				metodePembelajaran.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.refresh(pertemuan);
						pertemuan.setMetodePembelajaran(metodePembelajaran.getValue());
						Common.refreshSaveOrUpdate(pertemuan);
					}
				});

				if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.getSiswa() == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
						&& tbmuser.getBiodataCalonMahasiswa() == null) {

					row = new MyFormRow();
					row.setParent(rows);

					MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Generate Metode Pembelajaran",
							"/img/svg/gear.svg");
					toolbarbutton.setParent(row);

					String tanya = "Buatkan Metode Pembelajaran untuk \"" + pertemuan.info() + "\"";

					String tanyaAkhiran = "";
					String tanyaMengajar = " apa saja";
					if (pertemuan.getPerkuliahan() != null && pertemuan.getPerkuliahan().getMatakuliah() != null) {
						tanyaMengajar = " matakuliah " + pertemuan.getPerkuliahan().getMatakuliah().getNama();
						tanyaAkhiran = " pada matakuliah \"" + pertemuan.getPerkuliahan().getMatakuliah().getNama()
								+ "\"";
					} else if (pertemuan.getJadwalPelajaran() != null
							&& pertemuan.getJadwalPelajaran().getMatapelajaran() != null) {
						tanyaMengajar = " matapelajaran " + pertemuan.getJadwalPelajaran().getMatapelajaran().getNama();
						tanyaAkhiran = " pada matapelajaran \""
								+ pertemuan.getJadwalPelajaran().getMatapelajaran().getNama() + "\"";
					}
					toolbarbutton.addEventListener("onClick", AIGenerator.generateApa("Generate Metode Pembelajaran",
							"Metode Pembelajaran tentang apa ?", tanya, true, tanyaAkhiran,
							Common.getKonfigurasi("llama_system_catatan", "Kamu adalah Pengajar atau Dosen atau Guru ")
									.getNilai().trim(),
							null, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									metodePembelajaran.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
											.replaceAll("\n", "<br>"));

									Common.refresh(pertemuan);
									pertemuan.setMetodePembelajaran(metodePembelajaran.getValue());
									Common.refreshSaveOrUpdate(pertemuan);

								}
							}, tanyaMengajar, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									metodePembelajaran.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
											.replaceAll("\n", "<br>"));
								}
							}));

				}

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Pengalaman Belajar : ").setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				final Textbox pengalamanBelajar = new Textbox(pertemuan.getPengalamanBelajar());
				pengalamanBelajar.setRows(3);
				pengalamanBelajar.setWidth("90%");
				pengalamanBelajar.setParent(row);

				pengalamanBelajar.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.refresh(pertemuan);
						pertemuan.setPengalamanBelajar(pengalamanBelajar.getValue());
						Common.refreshSaveOrUpdate(pertemuan);
					}
				});

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelAgakKecil("*) Contoh: Menyimak, Mengamati, Mendiskusikan, dan Menjawab soal").setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Waktu Pembelajaran : ").setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				final Textbox waktupembelajaran = new Textbox(pertemuan.getWaktupembelajaran());
				waktupembelajaran.setRows(1);
				waktupembelajaran.setWidth("90%");
				waktupembelajaran.setParent(row);

				waktupembelajaran.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.refresh(pertemuan);
						pertemuan.setWaktupembelajaran(waktupembelajaran.getValue());
						Common.refreshSaveOrUpdate(pertemuan);
					}
				});

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelAgakKecil("*) Contoh: 2 x 50 menit").setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Tugas dan Penilaian : ").setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				final Textbox tugasDanPenilaian = new Textbox(pertemuan.getTugasDanPenilaian());
				tugasDanPenilaian.setRows(3);
				tugasDanPenilaian.setWidth("90%");
				tugasDanPenilaian.setParent(row);

				tugasDanPenilaian.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.refresh(pertemuan);
						pertemuan.setTugasDanPenilaian(tugasDanPenilaian.getValue());
						Common.refreshSaveOrUpdate(pertemuan);
					}
				});

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelAgakKecil(
						"*) Contoh: Ketepatan menjelaskan...., Ketepatan menyebutkan..., dan lain sebagainya")
						.setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

			}

		} else {

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);

			if (tampilInfo) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setValign("top");
				row.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(row, "2");
				row.appendChild(DashboardTimelinePertemuan.displayInfoPertemuan(pertemuan));

			}

			List<String> urls = Common.getUrls(pertemuan.getCatatan());
			String catat = pertemuan.getCatatan();
			catat = catat.replaceAll("\n", "<br>");
			for (String url : urls) {
				catat = org.apache.commons.lang3.StringUtils.replace(catat, url,
						"<a href='" + url + "' target='_blank'>" + url + "</a>");
			}

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setValign("top");
			row.setParent(rows);

			new MyLabelBoldConfig("Catatan").setParent(row);
			if (pertemuan.getPerkuliahan() != null) {
				new ais.ui.util.MyHtmlIframe(catat).setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Kemampuan akhir pembelajaran").setParent(row);
				new Label(pertemuan.getTopik()).setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Kriteria,Indikator&Bobot penilaian").setParent(row);
				new Label(pertemuan.getIndikator()).setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Metode Pembelajaran").setParent(row);
				new Label(pertemuan.getMetodePembelajaran()).setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Pengalaman Belajar").setParent(row);
				new Label(pertemuan.getPengalamanBelajar()).setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Waktu Pembelajaran").setParent(row);
				new Label(pertemuan.getWaktupembelajaran()).setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Tugas dan Penilaian").setParent(row);
				new Label(pertemuan.getTugasDanPenilaian()).setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

			} else {
				if (biodataCalonMahasiswa != null) {
					new ais.ui.util.MyHtmlIframe(catat).setParent(row);
				} else {
					catatan = new Textbox();
					catatan.setValue(pertemuan.getCatatan());
					catatan.setRows(15);
					catatan.setWidth("90%");
					catatan.setParent(row);

					catatan.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.refresh(pertemuan);
							pertemuan.setCatatan(catatan.getValue());
							Common.refreshSaveOrUpdate(pertemuan);
						}
					});

					row = new MyFormRow();
					row.setParent(rows);

					new MyLabelAgakKecil(
							"*) Catatan juga bisa berisi link atau URL yang mengarah ke website, audio, video, atau file tertentu")
							.setParent(row);

					if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
							&& tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
							&& tbmuser.getCalonSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null) {

						row = new MyFormRow();
						row.setParent(rows);

						MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Generate Catatan",
								"/img/svg/gear.svg");
						toolbarbutton.setParent(row);

						String d = "";
						for (Dosen dosen : pertemuan.ambilDosen()) {
							d += d.isEmpty() ? dosen.getNama() : " dan " + dosen.getNama();
						}

						String p = "";
						for (Mahasiswa mahasiswa : pertemuan.ambilMahasiswa()) {
							p += p.isEmpty() ? mahasiswa.getNama() : " dan " + mahasiswa.getNama();
						}

						String tanya = "Buatkan catatan dan berita acara untuk " + pertemuan.info()
								+ ", hari dan tanggal : " + Common.dateFormat6.get().format(pertemuan.getTanggal())
								+ ", jam waktu mulai " + pertemuan.getWaktuMulai() + " sampai "
								+ pertemuan.getWaktuSelesai() + ", Ruangan : "
								+ (pertemuan.getRuang() == null ? "Belum ditentukan" : pertemuan.getRuang().getNama())
								+ ", Gedung : "
								+ (pertemuan.getRuang() == null || pertemuan.getRuang().getGedung() == null ? ""
										: pertemuan.getRuang().getGedung().getNama())
								+ ", Pengajar : " + d + ", Topik : " + pertemuan.getTopik() + ", Peserta : " + p;

						String tanyaAkhiran = "";
						String tanyaMengajar = " apa saja";
						if (pertemuan.getPerkuliahan() != null && pertemuan.getPerkuliahan().getMatakuliah() != null) {
							tanyaMengajar = " matakuliah " + pertemuan.getPerkuliahan().getMatakuliah().getNama();
							tanyaAkhiran = " pada matakuliah \"" + pertemuan.getPerkuliahan().getMatakuliah().getNama()
									+ "\"";
						} else if (pertemuan.getJadwalPelajaran() != null
								&& pertemuan.getJadwalPelajaran().getMatapelajaran() != null) {
							tanyaMengajar = " matapelajaran "
									+ pertemuan.getJadwalPelajaran().getMatapelajaran().getNama();
							tanyaAkhiran = " pada matapelajaran \""
									+ pertemuan.getJadwalPelajaran().getMatapelajaran().getNama() + "\"";
						}
						toolbarbutton.addEventListener("onClick",
								AIGenerator.generateApa("Generate Catatan", "Informasikan tentang pertemuan kali ini",
										tanya, false, tanyaAkhiran,
										Common.getKonfigurasi("llama_system_catatan",
												"Kamu adalah Pengajar atau Dosen atau Guru ").getNilai().trim(),
										null, new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												catatan.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
														.replaceAll("\n", "<br>"));

												Common.refresh(pertemuan);
												pertemuan.setCatatan(catatan.getValue());
												Common.refreshSaveOrUpdate(pertemuan);

											}
										}, tanyaMengajar, new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												catatan.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
														.replaceAll("\n", "<br>"));
											}
										}));

					}
				}

			}

			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			Hbox hbox = new Hbox();
			hbox.setParent(row);

			Hbox hbox1 = new Hbox();
			hbox1.setParent(hbox);
			LampiranLain.createDownloadUploadFileLain(hbox1, pertemuan.getId(), LampiranLain.CATATAN_PERKULIAHAN,
					"Catatan", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, false, null, false, false);

			hbox1 = new Hbox();
			hbox1.setParent(hbox);
			LampiranLain.createDownloadUploadFileLain(hbox1, pertemuan.getId(), LampiranLain.LHP,
					pertemuan.getPerkuliahan() == null ? "Laporan Hasil" : LampiranLain.LHP, false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, false, null, false, false);

			// Tombol Catatan dibuat RATA KIRI (samakan dengan tampilan dosen/admin).
			rataKiriToolbarLampiran(hbox);

		}

		return myBorderlayout;
	}

	/**
	 * Membuat deretan tombol lampiran (hasil {@code createDownloadUploadFileLain}) menjadi
	 * <b>rata kiri</b>. Utilitas bersama {@code FileFotoLain.createDownloadUpload} membungkus
	 * tombol dalam Vbox/Hbox ber-{@code hflex="1"} (melebar penuh); akibatnya, bila tombolnya
	 * lebih dari satu — mis. bar Catatan: Ubah, Upload Catatan, Generate Catatan — ZK menyebar
	 * tombol ke sisi kiri, tengah, dan kanan (tampak "rata kanan-kiri"). Metode ini menelusuri
	 * seluruh komponen di bawah {@code akar} lalu MENGHAPUS {@code hflex} sehingga tiap wadah
	 * kembali selebar isinya dan tombol menempel rapat di kiri.
	 *
	 * <p>Perubahan hanya menyentuh properti tata letak ({@code hflex}); tidak mengubah data,
	 * event, maupun perilaku komponen berbagi-pakai lain — jadi aman dan bersifat lokal untuk
	 * halaman ini (tidak ikut mengubah halaman lain yang memakai createDownloadUpload).
	 *
	 * @param akar wadah terluar bar tombol (mis. {@code hbox} pada bagian Catatan).
	 */
	private static void rataKiriToolbarLampiran(org.zkoss.zk.ui.Component akar) {
		if (akar == null) {
			return;
		}
		try {
			if (akar instanceof org.zkoss.zk.ui.HtmlBasedComponent) {
				((org.zkoss.zk.ui.HtmlBasedComponent) akar).setHflex(null);
			}
			java.util.List<?> anak = akar.getChildren();
			for (int i = 0; i < anak.size(); i++) {
				Object c = anak.get(i);
				if (c instanceof org.zkoss.zk.ui.Component) {
					rataKiriToolbarLampiran((org.zkoss.zk.ui.Component) c);
				}
			}
		} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanHelper.java:1416");
		}
	}

	public void display(final Pertemuan pertemuan, final DataLoader dataLoader, final int index) throws Exception {
		display(pertemuan, dataLoader, index, null, null, null, null, null);
	}

	public void display(final Pertemuan pertemuan, final DataLoader dataLoader, final int index,
			PertemuanFileContent pertemuanFileContent) throws Exception {
		display(pertemuan, dataLoader, index, null, null, pertemuanFileContent, null, null);
	}

	public void display(final Pertemuan pertemuan, final DataLoader dataLoader, final int index,
			AudioPertemuan audioPertemuan) throws Exception {
		display(pertemuan, dataLoader, index, null, null, null, audioPertemuan, null);
	}

	public void display(final Pertemuan pertemuan, final DataLoader dataLoader, final int index,
			VideoPertemuan videoPertemuan) throws Exception {
		display(pertemuan, dataLoader, index, null, null, null, null, videoPertemuan);
	}

	public void display(final Pertemuan pertemuan, final DataLoader dataLoader, final int index,
			TugasPertemuan selectedTugasPertemuan, TugasKelompok selectedTugasKelompok,
			PertemuanFileContent pertemuanFileContent, AudioPertemuan audioPertemuan, VideoPertemuan videoPertemuan)
			throws Exception {
		this.selectedTugasPertemuan = selectedTugasPertemuan;
		this.selectedTugasKelompok = selectedTugasKelompok;
		this.selectedPertemuanFileContent = pertemuanFileContent;
		this.selectedAudioPertemuan = audioPertemuan;
		this.selectedVideoPertemuan = videoPertemuan;

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PertemuanHelper.this.index = index;

				if (window != null) {

					Common.clear(window);

				}

				if (window == null) {
					window = new MyWindow();
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
					window.setWidth("99%");
					window.setHeight("99%");
				}

				PertemuanHelper.this.dataLoader = dataLoader;

				PertemuanHelper.this.pertemuan = pertemuan;
				init();
				PertemuanHelper.this.window.setVisible(true);

				PertemuanHelper.this.window.onModal();
			}
		});
	}

}
