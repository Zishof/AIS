package ais.action.maintenance;

import java.net.URLEncoder;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Tab;
// NOTE: tab tambahan di dashboard-dashboard di bawah ini dibuat via
// ais.ui.util.MyTab (bukan org.zkoss.zul.Tab polos) - lihat MyTab utk guard
// "Exactly one selected tab is required: []" (race klik lama dari klien saat
// tabbox dashboard ini dibangun ulang tiap kali menu dibuka).
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.master.akunting.helper.DasboardAkuntansi;
import ais.action.master.akunting.helper.DasboardAkunting;
import ais.action.master.akunting.helper.DasboardKeuangan;
import ais.action.master.akunting.helper.DasboardPembayaranPerguruanTinggi;
import ais.action.master.akunting.helper.DasboardPembayaranSekolah;
import ais.action.master.asset.helper.DasboardAnalisisVendor;
import ais.action.master.asset.helper.DasboardPengadaan;
import ais.action.master.antarjemput.DasboardAntarJemput;
import ais.action.master.spmi.DasboardSPMI;
import ais.action.master.payroll.helper.DasborAnalisisPenggajian;
import ais.action.master.feeder.DasbordSinkronisasiNeoFeeder;
import ais.action.master.dashboard.admin.DasboardKepegawaian;
import ais.action.master.dashboard.admin.DashboardKegiatanKedosenan;
import ais.action.master.dashboard.admin.DashboardKegiatanKedosenanAdmin;
import ais.action.master.dashboard.admin.DashboardKegiatanKemahasiswaan;
import ais.action.master.dashboard.admin.DashboardKegiatanKesiswaan;
import ais.action.master.dashboard.admin.DashboardKegiatanKemahasiswaanAdmin;
import ais.action.master.dashboard.akunting.DasboardBukuBesar;
import ais.action.master.dashboard.akunting.DasboardNeracaLajur;
import ais.action.master.dashboard.employ.DasboardKinerja;
import ais.action.master.dashboard.helper.DashboardRekapAset;
import ais.action.master.dashboard.keuangan.DasboardPiutang;
import ais.action.master.dashboard.keuangan.DasboardPiutangRInci;
import ais.action.master.dashboard.keuangan.DasboardPiutangRInciExcel;
import ais.action.master.dashboard.keuangan.DasboardPiutangRinciSekolah;
import ais.action.master.dashboard.sekolah.DashboardKegiatanKesiswaanAdmin;
import ais.action.master.dashboard.utama.DashboardData;
import ais.action.master.dashboard.utama.DashboardDataSekolah;
import ais.action.master.dashboard.utama.DashboardPustaka;
import ais.action.master.repository.DasboardRepository;
import ais.action.master.sekolah.helper.ElearningSekolah;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.master.sop.helper.DasboardSop;
import ais.action.master.surat.helper.DasboardAlurSurat;
import ais.action.master.surat.helper.DasboardSurat;
import ais.common.Common;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.MyInclude;
import ais.ui.util.MyWindow;

final class MainDashboardEventHelper {

	private MainDashboardEventHelper() {
	}

	static void onPustaka(final MainAction action, Event event) throws Exception {
		if (action.tabPustaka != null && !action.tabPustaka.isInvalidated()) {
			action.selectExistingTab(action.tabPustaka);
			return;
		}
		DashboardPustaka a = new DashboardPustaka();
		a.setHeight("100%");
		a.setWidth("100%");
		action.tabPustaka = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Pustaka", "Pustaka",
				"/img/svg/books-thin.svg",
				action.createDashboardFrame("Pustaka",
						"Cari dan pantau koleksi pustaka agar pengguna lebih mudah menemukan bahan bacaan yang dibutuhkan.",
						a));
	}

	static void onWorkflow(final MainAction action, Event event) throws Exception {
		if (action.tabWorkflow != null && !action.tabWorkflow.isInvalidated()) {
			action.selectExistingTab(action.tabWorkflow);
			return;
		}
		DasboardSop dashboard = new DasboardSop(action.tabs, action.tabpanels, new MyWindow());
		action.tabWorkflow = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Workflow",
				"Workflow", "/img/svg/journal-arrow-up.svg",
				action.createDashboardFrame("Workflow",
						"Pantau alur pekerjaan, dokumen yang menunggu tindak lanjut, dan proses yang perlu segera diselesaikan.",
						dashboard));
	}

	static void onRepository(final MainAction action, Event event) throws Exception {
		if (action.tabRepository != null && !action.tabRepository.isInvalidated()) {
			action.selectExistingTab(action.tabRepository);
			return;
		}
		DasboardRepository dashboard = new DasboardRepository();
		dashboard.setHeight("100%");
		dashboard.setWidth("100%");
		action.tabRepository = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Repository",
				"Repository", "/img/svg/folder2.svg",
				action.createDashboardFrame("Dasbor Repository",
						"Pantau sinkronisasi, metadata, dokumen, DSpace, dan kesiapan indeks Turnitin repository.",
						dashboard));
	}

	static void onAntarJemput(final MainAction action, Event event) throws Exception {
		if (action.tabAntarJemput != null && !action.tabAntarJemput.isInvalidated()) {
			action.selectExistingTab(action.tabAntarJemput);
			return;
		}
		DasboardAntarJemput dashboard = new DasboardAntarJemput();
		dashboard.setHeight("100%");
		dashboard.setWidth("100%");
		action.tabAntarJemput = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Antar Jemput",
				"Antar Jemput", "/img/svg/calendar2.svg",
				action.createDashboardFrame("Dasboard Antar Jemput",
						"Pantau armada, rute, jadwal, peserta, scan gerbang, panggilan kelas, dan notifikasi.",
						dashboard));
	}

	static void onSpmi(final MainAction action, Event event) throws Exception {
		if (action.tabSpmi != null && !action.tabSpmi.isInvalidated()) {
			action.selectExistingTab(action.tabSpmi);
			return;
		}
		DasboardSPMI dashboard = new DasboardSPMI();
		dashboard.setHeight("100%");
		dashboard.setWidth("100%");
		action.tabSpmi = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "SPMI",
				"SPMI", "/img/svg/card-checklist-white.svg",
				action.createDashboardFrame("Dasbor SPMI",
						"Pantau hasil audit mutu internal, temuan, kepatuhan standar, dan tren kualitas kampus dalam satu halaman.",
						dashboard));
	}

	static void onKantin(final MainAction action, Event event) throws Exception {
		if (action.tabKantin != null && !action.tabKantin.isInvalidated()) {
			action.selectExistingTab(action.tabKantin);
			return;
		}
		MyInclude include = new MyInclude("/pages/master/koperasi/dashboard_kantin.zul");
		include.setHeight("100%");
		include.setWidth("100%");
		action.tabKantin = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Toko",
				"Toko", "/img/svg/basket3-white.svg",
				action.createDashboardFrame("Dasbor Kantin / Toko",
						"Ringkasan penjualan kantin: pemasukan, produk terlaris, jam ramai, dan pelanggan setia dalam satu halaman.",
						include));
	}

	static void onPos(final MainAction action, Event event) throws Exception {
		if (action.tabPos != null && !action.tabPos.isInvalidated()) {
			action.selectExistingTab(action.tabPos);
			return;
		}
		MyInclude include = new MyInclude("/pages/master/koperasi/pos_kantin.zul");
		include.setHeight("100%");
		include.setWidth("100%");
		action.tabPos = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "POS",
				"POS", "/img/svg/cash-register-white.svg",
				action.createDashboardFrame("Dasbor POS (Kasir)",
						"Layani pembeli dengan cepat: pilih produk, hitung otomatis diskon, cashback, dan kembalian, lalu cetak struk dalam satu layar.",
						include));
	}

	/**
	 * Membuka dasbor utama "Koperasi" yang mengumpulkan seluruh dasbor koperasi (Simpan Pinjam,
	 * Laporan, Pembagian SHU, Kantin) dalam satu halaman bertab. Tab yang sudah terbuka dipakai ulang
	 * agar tidak membuka duplikat.
	 */
	static void onKoperasi(final MainAction action, Event event) throws Exception {
		if (action.tabKoperasi != null && !action.tabKoperasi.isInvalidated()) {
			action.selectExistingTab(action.tabKoperasi);
			return;
		}
		MyInclude include = new MyInclude("/pages/master/koperasi/dashboard_koperasi.zul");
		include.setHeight("100%");
		include.setWidth("100%");
		action.tabKoperasi = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Koperasi",
				"Koperasi", "/img/svg/money-bills-white.svg",
				action.createDashboardFrame("Dasbor Koperasi",
						"Satu tempat untuk semua ringkasan koperasi: simpanan & pinjaman, laporan, pembagian SHU, dan penjualan kantin.",
						include));
	}

	/**
	 * Membuka dasbor utama <b>eMedic</b> (Rumah Sakit / Klinik) — dasbor SIRS komprehensif bergaya
	 * portal (kartu ringkasan, grafik donat/tren, tabel real-time, tombol Cetak PDF/Ekspor Excel) dari
	 * {@link ais.action.master.dashboard.sirs.DashboardSirsKomprehensif}. Komponen dipasang langsung
	 * sebagai isi tab (bukan lewat {@code MyInclude} halaman ber-{@code <?page>}) agar tab tampil rapi
	 * dengan label &amp; ikon yang benar. Tab yang sudah terbuka dipakai ulang agar tidak membuka duplikat.
	 */
	static void onEmedic(final MainAction action, Event event) throws Exception {
		if (action.tabEmedic != null && !action.tabEmedic.isInvalidated()) {
			action.selectExistingTab(action.tabEmedic);
			return;
		}
		ais.action.master.dashboard.sirs.DashboardSirsKomprehensif dashboard =
				new ais.action.master.dashboard.sirs.DashboardSirsKomprehensif();
		dashboard.setWidth("100%");
		// Ikon TAB memakai versi BERWARNA berukuran KECIL (emedic-icon.svg: width/height 18px) — bilah
		// tab berlatar terang, jadi ikon putih (hospital-white.svg, dipakai tombol menu berlatar hijau)
		// tak terlihat, dan ikon tanpa dimensi terbaca terlalu besar (tab tak mengecilkan img via CSS
		// seperti tombol menu). Nama file baru sekaligus mem-bypass cache ikon lama.
		action.tabEmedic = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "eMedic",
				"eMedic", "/img/svg/emedic-icon.svg",
				action.createDashboardFrame("Dasbor Utama Rumah Sakit / Klinik",
						"Ringkasan layanan rumah sakit/klinik dalam satu halaman: okupansi tempat tidur, "
						+ "tren pendaftaran & booking, pendapatan per unit, diagnosa, farmasi, dan transaksi.",
						dashboard));
		// Label tab dipaksa ulang setelah tab terbentuk. Saat kata "eMedic" pertama kali dipakai,
		// getBahasaConfig membuat label bahasa baru (tulis DB + tutup session) di tengah render
		// sehingga tab kadang tampil KOSONG. Pada titik ini label "eMedic" sudah ada di DB/cache,
		// jadi setLabel menghasilkan teks yang benar dan tab tidak lagi blank.
		if (action.tabEmedic != null) {
			try {
				action.tabEmedic.setLabel("eMedic");
				action.tabEmedic.setTooltiptext("Dasbor Utama Rumah Sakit / Klinik");
			} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/maintenance/MainDashboardEventHelper.java:215");
			}
		}
	}

	static void onGaji(final MainAction action, Event event) throws Exception {
		if (action.tabGaji != null && !action.tabGaji.isInvalidated()) {
			action.selectExistingTab(action.tabGaji);
			return;
		}
		DasborAnalisisPenggajian dashboard = new DasborAnalisisPenggajian();
		action.applyFullSize(dashboard);
		action.tabGaji = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Gaji",
				"Gaji", "/img/svg/money-bills.svg",
				action.createDashboardFrame("Dasbor Penggajian Utama",
						"Ringkasan penggajian, transaksi pegawai (tunjangan & potongan), serta cuti & izin dalam satu halaman.",
						dashboard));
	}

	static void onSinkronisasiFeeder(final MainAction action, Event event) throws Exception {
		if (action.tabSinkronisasiFeeder != null && !action.tabSinkronisasiFeeder.isInvalidated()) {
			action.selectExistingTab(action.tabSinkronisasiFeeder);
			return;
		}
		DasbordSinkronisasiNeoFeeder dashboard = new DasbordSinkronisasiNeoFeeder();
		dashboard.setHeight("100%");
		dashboard.setWidth("100%");
		action.tabSinkronisasiFeeder = Common.insertUrl(
				action.navigasi, action.menuService, action.iframe,
				"Neo Feeder", "Neo Feeder",
				"/img/svg/journal-arrow-up-white.svg",
				action.createDashboardFrame(
						"Sinkronisasi Neo Feeder PDDikti",
						"Pantau dan sinkronisasi data mahasiswa, dosen, nilai, matakuliah, " +
						"kurikulum, dan entitas lainnya dengan Neo Feeder PDDikti dalam satu halaman.",
						dashboard));
	}

	static void onSinkronisasiSister(final MainAction action, Event event) throws Exception {
		if (action.tabSinkronisasiSister != null && !action.tabSinkronisasiSister.isInvalidated()) {
			action.selectExistingTab(action.tabSinkronisasiSister);
			return;
		}
		ais.action.master.sister.DasbordSinkronisasiSister dashboard = new ais.action.master.sister.DasbordSinkronisasiSister();
		dashboard.setHeight("100%");
		dashboard.setWidth("100%");
		action.tabSinkronisasiSister = Common.insertUrl(
				action.navigasi, action.menuService, action.iframe,
				"Sister", "Sister",
				"/img/svg/journal-arrow-up-white.svg",
				action.createDashboardFrame(
						"Sinkronisasi Data SISTER",
						"Pantau dan sinkronisasi data referensi, dosen/SDM, dan Tridharma dari SISTER Kemdikbud (Dikti) dalam satu halaman.",
						dashboard));
	}

	static void onAdministrasi(final MainAction action, Event event) throws Exception {
		try {
			if (action.tabAdministrasi != null && !action.tabAdministrasi.isInvalidated()) {
				action.selectExistingTab(action.tabAdministrasi);
				return;
			}

			Tabs[] myTabs = new Tabs[1];
			Tabpanels[] myPanels = new Tabpanels[1];
			Tabbox tabbox = action.createStandardTabbox(myTabs, myPanels);

			String rHeight = action.dashboardHeight(25);
			String dashboardPanelHeight = action.dashboardHeight(25);

			DasboardSurat dashSurat = new DasboardSurat();
			dashSurat.setHeight("100%");
			dashSurat.setWidth("100%");

			Tabpanel panelDashboardSurat = action.createStandardTab(myTabs[0], myPanels[0], "Dashboard Persuratan",
					dashboardPanelHeight);
			action.applyAutoScrollableTabpanel(panelDashboardSurat, dashboardPanelHeight);
			panelDashboardSurat.appendChild(action.createDashboardFrame("Dashboard Persuratan",
					"Ringkasan surat masuk, surat keluar, dan pekerjaan persuratan yang masih perlu ditindaklanjuti.",
					dashSurat));

			Tab tabAlurPersetujuan = new ais.ui.util.MyTab("Alur Persetujuan");
			myTabs[0].appendChild(tabAlurPersetujuan);

			final Tabpanel panelAlurPersetujuan = new ais.ui.util.MyTabpanel();
			action.applyAutoScrollableTabpanel(panelAlurPersetujuan, rHeight);
			panelAlurPersetujuan.setParent(myPanels[0]);

			final EventListener loadAlurPersetujuanListener = new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (panelAlurPersetujuan.getChildren().isEmpty()) {
						DasboardAlurSurat l = new DasboardAlurSurat();
						l.setHeight("100%");
						l.setWidth("100%");
						action.createDashboardFrame("Alur Persetujuan",
								"Lihat posisi surat dan tahapan persetujuan agar pekerjaan yang tertahan bisa segera ditindaklanjuti.",
								l).setParent(panelAlurPersetujuan);
					}
				}
			};
			tabAlurPersetujuan.addEventListener("onClick", loadAlurPersetujuanListener);
			tabAlurPersetujuan.addEventListener("onSelect", loadAlurPersetujuanListener);

			if (myTabs[0] != null && myTabs[0].getChildren() != null && !myTabs[0].getChildren().isEmpty()) {
				((Tab) myTabs[0].getChildren().get(0)).setSelected(true);
			}
			tabbox.setSelectedIndex(0);

			action.tabAdministrasi = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Persuratan",
					"Administrasi", "/img/svg/journal-bookmark.svg", tabbox);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	static void onPengadaan(final MainAction action, Event event) throws Exception {
		if (action.tabPengadaan != null && !action.tabPengadaan.isInvalidated()) {
			action.selectExistingTab(action.tabPengadaan);
			return;
		}

		Tabs[] myTabs = new Tabs[1];
		Tabpanels[] myPanels = new Tabpanels[1];
		Tabbox tabbox = action.createStandardTabbox(myTabs, myPanels);

		String panelHeight = action.dashboardHeight(10);

		Tabpanel panelUtama = action.createStandardTab(myTabs[0], myPanels[0], "Proses Pengadaan", panelHeight);
		panelUtama.appendChild(action.createDashboardFrame("Proses Pengadaan",
				"Pantau pengajuan, proses pembelian, vendor, dan realisasi pengadaan dalam satu tampilan ringkas.",
				new DasboardPengadaan()));

		Tab tabInv = new ais.ui.util.MyTab("Rekap Inventaris");
		myTabs[0].appendChild(tabInv);

		final Tabpanel panelInv = new ais.ui.util.MyTabpanel();
		action.applyAutoScrollableTabpanel(panelInv, panelHeight);
		panelInv.setParent(myPanels[0]);

		tabInv.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelInv.getChildren().isEmpty()) {
					DashboardRekapAset laporan = new DashboardRekapAset();
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					action.createDashboardFrame("Rekap Inventaris",
							"Lihat jumlah, kondisi, dan sebaran aset agar inventaris lebih mudah diawasi.", laporan)
							.setParent(panelInv);
				}
			}
		});

		Tab tabAnalisisVendor = new ais.ui.util.MyTab("Analisis Vendor");
		myTabs[0].appendChild(tabAnalisisVendor);

		final Tabpanel panelAnalisisVendor = new ais.ui.util.MyTabpanel();
		action.applyAutoScrollableTabpanel(panelAnalisisVendor, panelHeight);
		panelAnalisisVendor.setParent(myPanels[0]);

		tabAnalisisVendor.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (panelAnalisisVendor.getChildren().isEmpty()) {
					DasboardAnalisisVendor dashboard = new DasboardAnalisisVendor();
					dashboard.setHeight("100%");
					dashboard.setWidth("100%");
					action.createDashboardFrame("Analisis Vendor",
							"Bandingkan aktivitas vendor agar pemilihan dan evaluasi rekanan menjadi lebih mudah.",
							dashboard).setParent(panelAnalisisVendor);
				}
			}
		});

		action.tabPengadaan = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Pengadaan",
				"Pengadaan", "/img/svg/boxes.svg", tabbox);
	}

	static void onKepegawaian(final MainAction action, Event event) throws Exception {
		if (action.tabKepegawaian != null && !action.tabKepegawaian.isInvalidated()) {
			action.selectExistingTab(action.tabKepegawaian);
			return;
		}
		DasboardKepegawaian dashboard = new DasboardKepegawaian();
		action.applyFullSize(dashboard);
		action.tabKepegawaian = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Kepegawaian",
				"Kepegawaian", "/img/svg/user-tie.svg",
				action.createDashboardFrame("Kepegawaian",
						"Lihat ringkasan data pegawai, aktivitas, dan kondisi penting yang perlu diperhatikan oleh pengelola.",
						dashboard));
	}

	static void onKeuangan(final MainAction action, Event event) throws Exception {
		if (action.tabKeuangan != null && !action.tabKeuangan.isInvalidated()) {
			action.selectExistingTab(action.tabKeuangan);
			return;
		}
		DasboardKeuangan dashboard = new DasboardKeuangan();
		action.tabKeuangan = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Keuangan",
				"Keuangan", "/img/svg/money-bills.svg",
				action.createDashboardFrame("Keuangan",
						"Pantau posisi keuangan, pembayaran, dan data penting agar keputusan bisa diambil lebih cepat.",
						dashboard));
	}

	static void onPembayaran(final MainAction action, Event event) throws Exception {
		try {
			if (action.tabPembayaran != null && !action.tabPembayaran.isInvalidated()) {
				action.selectExistingTab(action.tabPembayaran);
				return;
			}

			Tabs[] myTabs = new Tabs[1];
			Tabpanels[] myPanels = new Tabpanels[1];
			Tabbox tabbox = action.createStandardTabbox(myTabs, myPanels);

			final String rHeight = action.dashboardHeight(15);
			final String dashboardPanelHeight = action.dashboardHeight(15);

			final boolean modeGabungan = action.pt && action.ya;

			if (action.pt) {
				String labelDashPT = modeGabungan ? "Dashboard Pembayaran PT" : "Dashboard Pembayaran";
				String labelPiutangPT = modeGabungan ? "Kartu Piutang PT" : "Kartu Piutang";

				DasboardPembayaranPerguruanTinggi dashPT = new DasboardPembayaranPerguruanTinggi();
				dashPT.setHeight("100%");
				dashPT.setWidth("100%");

				Tabpanel panelDashPT = action.createStandardTab(myTabs[0], myPanels[0], labelDashPT,
						dashboardPanelHeight);
				action.applyAutoScrollableTabpanel(panelDashPT, dashboardPanelHeight);
				panelDashPT.appendChild(action.createDashboardFrame(labelDashPT,
						"Ringkasan pembayaran mahasiswa, penerimaan, dan kondisi transaksi terbaru.", dashPT));

				Tab tabBukuBesar = new ais.ui.util.MyTab(labelPiutangPT);
				myTabs[0].appendChild(tabBukuBesar);

				Tab tabBukuRinci = new ais.ui.util.MyTab("Kartu Piutang Rinci");
				myTabs[0].appendChild(tabBukuRinci);

				Tab tabBukuExcel = new ais.ui.util.MyTab("Kartu Piutang Rinci Excel");
				myTabs[0].appendChild(tabBukuExcel);

				final Tabpanel panelBuku = new ais.ui.util.MyTabpanel();
				action.applyAutoScrollableTabpanel(panelBuku, rHeight);
				panelBuku.setParent(myPanels[0]);

				final Tabpanel panelRinci = new ais.ui.util.MyTabpanel();
				action.applyAutoScrollableTabpanel(panelRinci, rHeight);
				panelRinci.setParent(myPanels[0]);

				final Tabpanel panelExcel = new ais.ui.util.MyTabpanel();
				action.applyAutoScrollableTabpanel(panelExcel, rHeight);
				panelExcel.setParent(myPanels[0]);

				final EventListener loadPiutangPTListener = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (panelBuku.getChildren().isEmpty()) {
							DasboardPiutang l = new DasboardPiutang();
							l.setHeight("100%");
							l.setWidth("100%");
							action.createDashboardFrame("Kartu Piutang PT",
									"Lihat posisi tagihan mahasiswa agar penagihan dan pemantauan pembayaran lebih jelas.",
									l).setParent(panelBuku);
						}
					}
				};
				tabBukuBesar.addEventListener("onClick", loadPiutangPTListener);
				tabBukuBesar.addEventListener("onSelect", loadPiutangPTListener);

				final EventListener loadPiutangRinciPTListener = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (panelRinci.getChildren().isEmpty()) {
							DasboardPiutangRInci l = new DasboardPiutangRInci();
							l.setHeight("100%");
							l.setWidth("100%");
							action.createDashboardFrame("Kartu Piutang Rinci",
									"Lihat rincian tagihan dan pembayaran per mahasiswa untuk pengecekan yang lebih teliti.",
									l).setParent(panelRinci);
						}
					}
				};
				tabBukuRinci.addEventListener("onClick", loadPiutangRinciPTListener);
				tabBukuRinci.addEventListener("onSelect", loadPiutangRinciPTListener);

				final EventListener loadPiutangExcelPTListener = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (panelExcel.getChildren().isEmpty()) {
							DasboardPiutangRInciExcel l = new DasboardPiutangRInciExcel();
							l.setHeight("100%");
							l.setWidth("100%");
							action.createDashboardFrame("Kartu Piutang Rinci Excel",
									"Tampilkan data piutang dalam tabel terlebih dahulu, lalu unduh Excel saat benar-benar dibutuhkan.",
									l).setParent(panelExcel);
						}
					}
				};
				tabBukuExcel.addEventListener("onClick", loadPiutangExcelPTListener);
				tabBukuExcel.addEventListener("onSelect", loadPiutangExcelPTListener);
			}

			if (action.ya) {
				String labelDashSekolah = modeGabungan ? "Dashboard Pembayaran Sekolah" : "Dashboard Pembayaran";
				String labelPiutangSekolah = modeGabungan ? "Kartu Piutang Sekolah" : "Kartu Piutang";

				if (modeGabungan) {
					Tab tabDashSekolah = new ais.ui.util.MyTab(labelDashSekolah);
					myTabs[0].appendChild(tabDashSekolah);

					final Tabpanel panelDashSekolah = new ais.ui.util.MyTabpanel();
					action.applyAutoScrollableTabpanel(panelDashSekolah, dashboardPanelHeight);
					panelDashSekolah.setParent(myPanels[0]);

					final EventListener loadDashboardSekolahListener = new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (panelDashSekolah.getChildren().isEmpty()) {
								DasboardPembayaranSekolah l = new DasboardPembayaranSekolah();
								l.setHeight("100%");
								l.setWidth("100%");
								action.createDashboardFrame("Dashboard Pembayaran Sekolah",
										"Ringkasan pembayaran siswa, tunggakan, dan penerimaan sekolah yang perlu dipantau.",
										l).setParent(panelDashSekolah);
							}
						}
					};

					tabDashSekolah.addEventListener("onClick", loadDashboardSekolahListener);
					tabDashSekolah.addEventListener("onSelect", loadDashboardSekolahListener);
				} else {
					DasboardPembayaranSekolah dashSekolah = new DasboardPembayaranSekolah();
					dashSekolah.setHeight("100%");
					dashSekolah.setWidth("100%");

					Tabpanel panelDashSekolahLangsung = action.createStandardTab(myTabs[0], myPanels[0],
							labelDashSekolah, dashboardPanelHeight);
					action.applyAutoScrollableTabpanel(panelDashSekolahLangsung, dashboardPanelHeight);
					panelDashSekolahLangsung.appendChild(dashSekolah);
				}

				Tab tabBukuBesarSekolah = new ais.ui.util.MyTab(labelPiutangSekolah);
				myTabs[0].appendChild(tabBukuBesarSekolah);

				final Tabpanel panelBukuSekolah = new ais.ui.util.MyTabpanel();
				action.applyAutoScrollableTabpanel(panelBukuSekolah, rHeight);
				panelBukuSekolah.setParent(myPanels[0]);

				final EventListener loadPiutangSekolahListener = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (panelBukuSekolah.getChildren().isEmpty()) {
							DasboardPiutangRinciSekolah l = new DasboardPiutangRinciSekolah();
							l.setHeight("100%");
							l.setWidth("100%");
							action.createDashboardFrame("Kartu Piutang Sekolah",
									"Lihat posisi tagihan siswa agar penagihan dan pemantauan pembayaran lebih jelas.",
									l).setParent(panelBukuSekolah);
						}
					}
				};

				tabBukuBesarSekolah.addEventListener("onClick", loadPiutangSekolahListener);
				tabBukuBesarSekolah.addEventListener("onSelect", loadPiutangSekolahListener);

				Tab tabLaporanRincianGrid = new ais.ui.util.MyTab("Dashboard Rincian Pembayaran");
				myTabs[0].appendChild(tabLaporanRincianGrid);

				final Tabpanel panelLaporanRincianGrid = new ais.ui.util.MyTabpanel();
				action.applyAutoScrollableTabpanel(panelLaporanRincianGrid, rHeight);
				panelLaporanRincianGrid.setParent(myPanels[0]);

				final EventListener loadLaporanRincianGridListener = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (panelLaporanRincianGrid.getChildren().isEmpty()) {
							ais.action.report.format1.sekolah.LaporanRincianPembayaranSiswaGrid l = new ais.action.report.format1.sekolah.LaporanRincianPembayaranSiswaGrid();
							l.setHeight("100%");
							l.setWidth("100%");
							action.createDashboardFrame("Dashboard Rincian Pembayaran",
									"Lihat rincian pembayaran siswa dalam tabel agar lebih mudah dicari sebelum diunduh.",
									l).setParent(panelLaporanRincianGrid);
						}
					}
				};

				tabLaporanRincianGrid.addEventListener("onClick", loadLaporanRincianGridListener);
				tabLaporanRincianGrid.addEventListener("onSelect", loadLaporanRincianGridListener);
			}

			if (myTabs[0] != null && myTabs[0].getChildren() != null && !myTabs[0].getChildren().isEmpty()) {
				Component firstTab = (Component) myTabs[0].getChildren().get(0);
				if (firstTab instanceof Tab) {
					((Tab) firstTab).setSelected(true);
				}
			}

			action.tabPembayaran = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Pembayaran",
					"Pembayaran", "/img/svg/payments.svg", tabbox);

		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
			Common.tampilErrorJikaAdmin(e);
		}
	}

	static void onKinerja(final MainAction action, Event event) throws Exception {
		if (action.tabKinerja != null && !action.tabKinerja.isInvalidated()) {
			action.selectExistingTab(action.tabKinerja);
			return;
		}
		DasboardKinerja dashboard = new DasboardKinerja();
		action.tabKinerja = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Kinerja", "Kinerja",
				"/img/svg/card-checklist.svg",
				action.createDashboardFrame("Kinerja",
						"Lihat capaian pekerjaan dan aktivitas utama agar evaluasi kinerja lebih mudah dipahami.",
						dashboard));
	}

	static void onAkunting(final MainAction action, Event event) throws Exception {
		if (action.tabAkunting != null && !action.tabAkunting.isInvalidated()) {
			action.selectExistingTab(action.tabAkunting);
			return;
		}

		Tabs[] myTabs = new Tabs[1];
		Tabpanels[] myPanels = new Tabpanels[1];
		Tabbox tabbox = action.createStandardTabbox(myTabs, myPanels);

		action.createStandardTab(myTabs[0], myPanels[0], "Dashboard Akuntansi", action.dashboardHeight(10))
				.appendChild(action.createDashboardFrame("Dashboard Akuntansi",
						"Lihat kondisi akuntansi, laporan keuangan, dan ringkasan transaksi penting dalam satu tempat.",
						new DasboardAkuntansi()));

		Tab tabLaporan = new ais.ui.util.MyTab("Laporan Keuangan");
		myTabs[0].appendChild(tabLaporan);
		final Tabpanel panelLaporan = new ais.ui.util.MyTabpanel();
		action.applyAutoScrollableTabpanel(panelLaporan, action.panelHeightOffset(30));
		panelLaporan.setParent(myPanels[0]);
		tabLaporan.addEventListener("onClick", new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				if (panelLaporan.getChildren().isEmpty()) {
					DasboardAkunting l = new DasboardAkunting();
					l.setHeight("100%");
					l.setWidth("100%");
					action.createDashboardFrame("Laporan Keuangan",
							"Lihat laporan keuangan utama untuk membantu pengecekan dan pengambilan keputusan.", l)
							.setParent(panelLaporan);
				}
			}
		});

		Tab tabBuku = new ais.ui.util.MyTab("Buku Besar");
		myTabs[0].appendChild(tabBuku);
		final Tabpanel panelBuku = new ais.ui.util.MyTabpanel();
		action.applyAutoScrollableTabpanel(panelBuku, action.panelHeightOffset(30));
		panelBuku.setParent(myPanels[0]);
		tabBuku.addEventListener("onClick", new EventListener() {
			public void onEvent(Event arg0) {
				if (panelBuku.getChildren().isEmpty()) {
					DasboardBukuBesar l = new DasboardBukuBesar();
					l.setHeight("100%");
					l.setWidth("100%");
					action.createDashboardFrame("Buku Besar",
							"Lihat pergerakan akun secara rinci agar transaksi lebih mudah diperiksa.", l)
							.setParent(panelBuku);
				}
			}
		});

		Tab tabNeraca = new ais.ui.util.MyTab("Neraca Lajur");
		myTabs[0].appendChild(tabNeraca);
		final Tabpanel panelNeraca = new ais.ui.util.MyTabpanel();
		action.applyAutoScrollableTabpanel(panelNeraca, action.panelHeightOffset(30));
		panelNeraca.setParent(myPanels[0]);
		tabNeraca.addEventListener("onClick", new EventListener() {
			public void onEvent(Event arg0) {
				if (panelNeraca.getChildren().isEmpty()) {
					DasboardNeracaLajur l = new DasboardNeracaLajur();
					l.setHeight("100%");
					l.setWidth("100%");
					action.createDashboardFrame("Neraca Lajur",
							"Lihat ringkasan saldo akun agar penyusunan laporan akhir lebih mudah dikontrol.", l)
							.setParent(panelNeraca);
				}
			}
		});

		action.tabAkunting = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Akuntansi",
				"Akuntansi", "/img/svg/file-earmark-text.svg", tabbox);
	}

	static void onDashboard(final MainAction action, Event event) throws Exception {
		if (action.tabDashboard != null && !action.tabDashboard.isInvalidated()) {
			action.selectExistingTab(action.tabDashboard);
			return;
		}

		if (action.tbmuser != null && (action.tbmuser.getMahasiswa() != null || action.tbmuser.ambilDosen() != null
				|| action.tbmuser.ambilGuru() != null || action.tbmuser.getSiswa() != null)) {
			MyWindow window = new MyWindow();
			ProfileAction.initProfile(action.tbmuser, window, null);
			action.tabDashboard = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Akademik",
					"Akademik", "/img/svg/dashboard-speed.svg", window);
		} else {
			String tinggi = action.dashboardHeight(10);
			Tabs[] myTabs = new Tabs[1];
			Tabpanels[] myPanels = new Tabpanels[1];
			Tabbox tabbox = action.createStandardTabbox(myTabs, myPanels);
			action.tabDashboard = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Akademik",
					"Akademik", "/img/svg/dashboard-speed.svg", tabbox);

			if (action.pt) {
				DashboardData a = new DashboardData();
				a.setHeight(tinggi);
				a.setWidth("100%");
				action.createStandardTab(myTabs[0], myPanels[0], "Akademik Perguruan Tinggi", tinggi)
						.appendChild(action.createDashboardFrame("Akademik Perguruan Tinggi",
								"Ringkasan aktivitas kampus, data akademik, dan informasi penting untuk pengelola perguruan tinggi.",
								a));
			}
			if (action.ya) {
				DashboardDataSekolah a = new DashboardDataSekolah();
				a.setHeight(tinggi);
				a.setWidth("100%");
				action.createStandardTab(myTabs[0], myPanels[0], "Akademik Sekolah", tinggi)
						.appendChild(action.createDashboardFrame("Akademik Sekolah",
								"Ringkasan aktivitas sekolah, data siswa, dan informasi penting untuk pengelola sekolah.",
								a));
			}
		}
	}

	static void onKegiatanDanPrestasi(final MainAction action, Event event) {
		if (action.tabPrestasi != null && !action.tabPrestasi.isInvalidated()) {
			action.selectExistingTab(action.tabPrestasi);
			return;
		}

		action.tbmuser = Common.getCurrentUser();
		if (action.tbmuser.ambilDosen() != null) {
			DashboardKegiatanKedosenan window = new DashboardKegiatanKedosenan(action.tbmuser.ambilDosen());
			window.setHeight("100%");
			window.setWidth("100%");
			action.tabPrestasi = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Prestasi",
					"Prestasi", "/img/svg/trophy.svg", action.createDashboardFrame("Prestasi Dosen",
							"Lihat kegiatan dan pencapaian dosen dalam satu tampilan yang mudah dipantau.", window));
		} else if (action.tbmuser.ambilGuru() != null) {
			action.tabPrestasi = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Prestasi",
					"/pages/master/prestasi_pegawai.zul", "/img/svg/trophy.svg", null);
		} else if (action.tbmuser.getMahasiswa() != null) {
			DashboardKegiatanKemahasiswaan window = new DashboardKegiatanKemahasiswaan(action.tbmuser.getMahasiswa());
			window.setHeight("100%");
			window.setWidth("100%");
			action.tabPrestasi = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Prestasi",
					"Prestasi", "/img/svg/trophy.svg",
					action.createDashboardFrame("Prestasi Mahasiswa",
							"Lihat kegiatan dan pencapaian peserta didik dalam satu tampilan yang mudah dipantau.",
							window));
		} else if (action.tbmuser.getSiswa() != null) {
			DashboardKegiatanKesiswaan windowSiswa = new DashboardKegiatanKesiswaan(action.tbmuser.getSiswa());
			windowSiswa.setHeight("100%");
			windowSiswa.setWidth("100%");
			action.tabPrestasi = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Prestasi",
					"Prestasi", "/img/svg/trophy.svg",
					action.createDashboardFrame("Prestasi Siswa",
							"Lihat kegiatan dan pencapaian peserta didik dalam satu tampilan yang mudah dipantau.",
							windowSiswa));
		} else {
			boolean[] ptYa = Common.chekPtAtauSekolah();
			action.pt = ptYa[0];
			action.ya = ptYa[1];

			String tinggi = action.mobile ? action.dashboardHeight(4)
					: (((int) (action.safeDesktopHeight() * 1.75)) + "px");
			Tabs[] myTabs = new Tabs[1];
			Tabpanels[] myPanels = new Tabpanels[1];
			Tabbox tabbox = action.createStandardTabbox(myTabs, myPanels);
			action.tabPrestasi = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Prestasi",
					"Prestasi", "/img/svg/trophy.svg", tabbox);

			if (action.pt) {
				Tabpanel p1 = action.createStandardTab(myTabs[0], myPanels[0], "Mahasiswa", tinggi);
				DashboardKegiatanKemahasiswaanAdmin w1 = new DashboardKegiatanKemahasiswaanAdmin("", "none", false);
				w1.setHeight("100%");
				w1.setWidth("100%");
				action.createDashboardFrame("Prestasi Mahasiswa",
						"Lihat aktivitas dan prestasi mahasiswa agar pembinaan lebih mudah diarahkan.", w1)
						.setParent(p1);

				Tabpanel p2 = action.createStandardTab(myTabs[0], myPanels[0], "Dosen", tinggi);
				DashboardKegiatanKedosenanAdmin w2 = new DashboardKegiatanKedosenanAdmin("", "none", false);
				w2.setHeight("100%");
				w2.setWidth("100%");
				action.createDashboardFrame("Prestasi Dosen",
						"Lihat aktivitas dan prestasi dosen agar evaluasi dan apresiasi lebih mudah dilakukan.", w2)
						.setParent(p2);
			} else if (action.ya) {
				Tabpanel p1 = action.createStandardTab(myTabs[0], myPanels[0], "Siswa", tinggi);
				DashboardKegiatanKesiswaanAdmin w1 = new DashboardKegiatanKesiswaanAdmin("", "none", false);
				w1.setHeight("100%");
				w1.setWidth("100%");
				action.createDashboardFrame("Prestasi Siswa",
						"Lihat aktivitas dan prestasi siswa agar pembinaan lebih mudah diarahkan.", w1).setParent(p1);
			}

			Tab tabPegawai = new ais.ui.util.MyTab("Pegawai");
			myTabs[0].appendChild(tabPegawai);
			final Tabpanel panelPegawai = new ais.ui.util.MyTabpanel();
			action.applyAutoScrollableTabpanel(panelPegawai, tinggi);
			panelPegawai.setParent(myPanels[0]);
			tabPegawai.addEventListener("onClick", new EventListener() {
				public void onEvent(Event arg0) {
					if (panelPegawai.getChildren().isEmpty()) {
						MyInclude include = new MyInclude("/pages/master/prestasi_pegawai.zul");
						action.createDashboardFrame("Prestasi Pegawai",
								"Lihat aktivitas dan prestasi pegawai agar evaluasi lebih mudah dilakukan.", include)
								.setParent(panelPegawai);
					}
				}
			});
		}
	}

	static void onPengumumanPerkuliahan(final MainAction action, Event event) throws Exception {
		try {
			if (action.tabLearning != null && !action.tabLearning.isInvalidated()) {
				action.selectExistingTab(action.tabLearning);
				return;
			}
			Sekolah sekolah1 = SekolahUtil.getSekolah();
			if (sekolah1 != null && sekolah1.getId() != null) {
				ElearningSekolah dashboard = new ElearningSekolah();
				action.applyFullSize(dashboard);
				action.tabLearning = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "e-Learning",
						"e-Learning", "/img/svg/book.svg", dashboard);
			} else {
				if (action.tbmuser == null) {
					action.tbmuser = Common.getCurrentUser();
				}
				String userId = action.tbmuser == null || action.tbmuser.getUserId() == null ? ""
						: action.tbmuser.getUserId();
				String url = "/common/mobile/e_learning.zul?user=" + URLEncoder.encode(userId, "UTF-8");
				MyInclude include = new MyInclude(url);
				action.applyFullSize(include);
				action.tabLearning = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "e-Learning",
						"e-Learning", "/img/svg/book.svg", include);
			}
		} catch (Exception e) {
			action.showError(e);
		}
	}

	static void onPengumuman(final MainAction action, Event event) throws Exception {
		if (action.tabPengumuman != null && !action.tabPengumuman.isInvalidated()) {
			action.tabPengumuman.setSelected(true);
			return;
		}
		action.tabPengumuman = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Pengumuman",
				"/pages/main/tampilan_pengumuman_akademis_mobile.zul", "/img/svg/comment-2-text-line.svg", null);
	}

	static void onPresensi(final MainAction action, Event event) throws Exception {
		if (action.tabPresensi != null && !action.tabPresensi.isInvalidated()) {
			action.selectExistingTab(action.tabPresensi);
			return;
		}
		action.tabPresensi = Common.insertUrl(action.navigasi, action.menuService, action.iframe, "Presensi",
				"/presensi.zul", "/img/svg/check-circled-outline.svg", null);
	}
}
