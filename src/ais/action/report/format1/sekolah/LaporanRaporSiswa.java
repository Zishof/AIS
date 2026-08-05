package ais.action.report.format1.sekolah;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
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
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.helper.RevisiHelper;

import ais.action.master.sekolah.util.GrupPenilaianUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.FormulirKegiatanPeserta;
import ais.database.model.Konstanta;
import ais.database.model.ParameterTambahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.AbsenPiket;
import ais.database.model.sekolah.AbsenPiketDetail;
import ais.database.model.sekolah.ApresiasiSiswa;
import ais.database.model.sekolah.CatatanKelasSiswa;
import ais.database.model.sekolah.CatatanSiswa;
import ais.database.model.sekolah.DetailGrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.DetailGrupPenilaian;
import ais.database.model.sekolah.DetailJenisPenilaian;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Hukuman;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JenisCatatanKelasSiswa;
import ais.database.model.sekolah.JenisCatatanSiswa;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.JenisRaporSiswa;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.database.model.sekolah.KegiatanSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.KelompokMatapelajaran;
import ais.database.model.sekolah.KelompokParameterTambahanCatatanKelasSiswa;
import ais.database.model.sekolah.KelompokParameterTambahanCatatanSiswa;
import ais.database.model.sekolah.KurikulumPunyaMatapelajaran;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.NilaiHurufSekolah;
import ais.database.model.sekolah.ParameterTambahanCatatanKelasSiswa;
import ais.database.model.sekolah.ParameterTambahanCatatanSiswa;
import ais.database.model.sekolah.PelanggaranSiswa;
import ais.database.model.sekolah.Penghargaan;
import ais.database.model.sekolah.PrestasiSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

public class LaporanRaporSiswa extends MyWindow {

	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Paging paging = new Paging();
	private Textbox cari;
	private MyGrid grid;
	private TreeMap<Long, KelasSiswaPunyaSiswa> map = new TreeMap<Long, KelasSiswaPunyaSiswa>();
	private MyDatebox tanggal;
	private Combobox sekolah;
	private Combobox tahunAkademik;
	private Combobox searchsmt;
	private Combobox jenisRaporSiswa;
	private Combobox kelasCombo;
	private Tbmuser tbmuser;
	private MyCheckboxConfig tampilkanKelas;
	private Toolbar toolbar;

	// ====================================================================
	//  CACHE PERCEPAT GENERATE RAPOR (L1 / L2 / L3)
	//  Masalah: rata-rata KELAS & ANGKATAN serta total kelas dihitung ULANG
	//  per-SISWA, padahal nilainya SAMA untuk semua siswa sekelas → saat
	//  mencetak 1 kelas (mis. 30 siswa) hitungan berat diulang ~30x.
	//
	//  L1 = pemakaian ulang dalam satu siswa (beberapa item berbagi kunci).
	//  L2 = cache per-RUN cetak (ThreadLocal, dibagi SEMUA siswa dalam satu
	//       kali generate) → dihitung SEKALI per kelas. AMAN: bersih tiap run,
	//       tidak ada data basi. Inilah percepatan utama.
	//  L3 = lintas-request (DashboardCacheUtil L2/L3 in-memory+persisten),
	//       DIGERBANG konfigurasi "rapor_cache_rata_rata_lintas_request"
	//       (default MATI agar tidak ada angka basi lintas-request; nyalakan
	//       untuk cetak berulang super cepat — hanya meng-cache angka
	//       rata-rata REFERENSI, nilai pribadi siswa tetap dibaca segar).
	// ====================================================================
	private static final ThreadLocal<Map<String, Double>> RUN_CACHE_RATA = new ThreadLocal<Map<String, Double>>();
	private static final ThreadLocal<Map<String, double[]>> RUN_CACHE_TOTAL_KELAS = new ThreadLocal<Map<String, double[]>>();

	private static void mulaiRunCacheRapor() {
		RUN_CACHE_RATA.set(new HashMap<String, Double>());
		RUN_CACHE_TOTAL_KELAS.set(new HashMap<String, double[]>());
	}

	private static void akhiriRunCacheRapor() {
		RUN_CACHE_RATA.remove();
		RUN_CACHE_TOTAL_KELAS.remove();
	}

	private static Map<String, Double> runCacheRata() {
		Map<String, Double> m = RUN_CACHE_RATA.get();
		if (m == null) {
			m = new HashMap<String, Double>();
			RUN_CACHE_RATA.set(m);
		}
		return m;
	}

	private static Map<String, double[]> runCacheTotalKelas() {
		Map<String, double[]> m = RUN_CACHE_TOTAL_KELAS.get();
		if (m == null) {
			m = new HashMap<String, double[]>();
			RUN_CACHE_TOTAL_KELAS.set(m);
		}
		return m;
	}

	private static boolean cacheRaporLintasRequestAktif() {
		try {
			return ais.database.model.Konfigurasi.AKTIF.equals(Common.getKonfigurasi(
					"rapor_cache_rata_rata_lintas_request", ais.database.model.Konfigurasi.TIDAK_AKTIF).getNilai());
		} catch (Exception e) {
			return false;
		}
	}

	/** Ambil rata-rata dari L1/L2(run) lalu L3(lintas-request bila aktif). {@code null} bila belum ada. */
	private static Double ambilCacheRata(String key) {
		Map<String, Double> run = runCacheRata();
		Double v = run.get(key);
		if (v != null) {
			return v;
		}
		if (cacheRaporLintasRequestAktif()) {
			try {
				Object o = ais.common.DashboardCacheUtil.getL2("raporRata_" + key);
				if (o == null) {
					o = ais.common.DashboardCacheUtil.getL3("raporRata_" + key);
				}
				if (o instanceof Double) {
					run.put(key, (Double) o);
					return (Double) o;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:212");
				// abaikan — bila cache lintas-request bermasalah, hitung ulang (selalu benar)
			}
		}
		return null;
	}

	/** Simpan rata-rata ke L2(run) + L3(lintas-request bila aktif). */
	private static void simpanCacheRata(String key, Double val) {
		runCacheRata().put(key, val);
		if (cacheRaporLintasRequestAktif()) {
			try {
				ais.common.DashboardCacheUtil.putL2("raporRata_" + key, val);
				ais.common.DashboardCacheUtil.putL3("raporRata_" + key, val);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:226");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rapor Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
		}
	}

	/** Ambil [totalKelas, jumlahKelas] dari L2(run) lalu L3(bila aktif). {@code null} bila belum ada. */
	private static double[] ambilCacheTotalKelas(String key) {
		Map<String, double[]> run = runCacheTotalKelas();
		double[] v = run.get(key);
		if (v != null) {
			return v;
		}
		if (cacheRaporLintasRequestAktif()) {
			try {
				Object o = ais.common.DashboardCacheUtil.getL2("raporTotKls_" + key);
				if (o == null) {
					o = ais.common.DashboardCacheUtil.getL3("raporTotKls_" + key);
				}
				if (o instanceof double[]) {
					run.put(key, (double[]) o);
					return (double[]) o;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:248");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rapor Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
		}
		return null;
	}

	/** Simpan [totalKelas, jumlahKelas] ke L2(run) + L3(bila aktif). */
	private static void simpanCacheTotalKelas(String key, double[] val) {
		runCacheTotalKelas().put(key, val);
		if (cacheRaporLintasRequestAktif()) {
			try {
				ais.common.DashboardCacheUtil.putL2("raporTotKls_" + key, val);
				ais.common.DashboardCacheUtil.putL3("raporTotKls_" + key, val);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:261");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rapor Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
		}
	}

	public LaporanRaporSiswa() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rapor Siswa", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	// --- Helper Methods untuk Manajemen Session (Memory & Resource Optimization) ---
	private static Session bukaSession() {
		return ais.action.report.Report.openNativeSession();
	}

	private static void tutupSession(Session session) {
		if (session != null) {
			try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:282"); /* abaikan */ }
			try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:283"); /* abaikan */ }
			try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:284"); /* abaikan */ }
		}
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {
		Common.initPaging50(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		tbmuser = Common.getCurrentUser();

		Tabpanel tabpanel1 = null;
		if (tbmuser.getSiswa() == null && tbmuser.ambilGuru() == null) {
			Tabbox tabbox = new Tabbox();
			tabbox.setParent(Common.tampilanScrollTabbox(this));
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			MyTabConfig tab1 = new MyTabConfig("Rapor Siswa");
			tab1.setParent(tabs);

			MyTabConfig tab51 = new MyTabConfig("Jenis Rapor");
			tab51.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			tabpanel1 = new ais.ui.util.MyTabpanel();
			tabpanel1.setParent(tabpanels);

			final Tabpanel tabpanel51 = new ais.ui.util.MyTabpanel();
			tabpanel51.setParent(tabpanels);
			tab51.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel51.getChildren().size() == 0) {
						MyWindow window = new MyWindow("", "none", false);
						window.setHeight("100%");
						window.setWidth("100%");
						window.setParent(tabpanel51);
						MyInclude iframe = new MyInclude("/pages/master/sekolah/jenis_rapor_siswa.zul");
						iframe.setParent(window);
					}
				}
			});
		}

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel1 == null ? this : tabpanel1);
		borderlayout.setStyle("min-width:1540px;");

		West west = new West();
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("325px");

		final Center center1 = new Center();
		Borderlayout borderlayout1 = new ais.ui.util.MyBorderlayout();
		borderlayout1.setParent(west);

		North north = new North();
		north.setParent(borderlayout1);
		north.setHeight("420px");
		north.setBorder("none");

		MyGrid mygrid = new MyGrid();
		mygrid.setWidth("100%");
		mygrid.setParent(north);
		mygrid.setWidth("100%");
		mygrid.setHeight("100%");
		mygrid.setSclass("fgrid");

		Columns columns = new Columns();
		columns.setParent(mygrid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("60px");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(mygrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tgl : "));
		tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		tanggal.setFormat(Common.dateFormat1.get().toPattern());
		tanggal.setReadonly(true);

		Hbox hbox = new Hbox();
		hbox.setParent(row);
		hbox.appendChild(tanggal);

		MyButtonConfig button = new MyButtonConfig("Tampilkan Rapor");
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		});

		row = new MyFormRow();
		row.setVisible(tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Siswa : "));

		hbox = new Hbox();
		hbox.setParent(row);
		hbox.appendChild(sekolah = new Combobox());
		sekolah.setWidth("60px");
		Common.insertComboDanSemua(sekolah, "nama", Sekolah.class, Restrictions.sqlRestriction("true"));

		Sekolah s = SekolahUtil.getSekolah();
		if (s != null && s.getId() != null) {
			Common.selectComboItem(true, sekolah, s);
			sekolah.setDisabled(true);
		}

		cari = new Textbox();
		cari.setParent(hbox);
		cari.setTooltiptext("Cari nama / NIS / email / KELAS siswa (mis. VII-B), lalu tekan Enter atau klik Cari");
		cari.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		button = new MyButtonConfig("Cari");
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Rapor *"));
		row.appendChild(jenisRaporSiswa = new Combobox());
		jenisRaporSiswa.setWidth("90%");
		jenisRaporSiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("TA : "));
		tahunAkademik = new Combobox();
		Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				muatOpsiKelas();
				onSearchDefault(null);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Smt : "));
		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		comboitem.setValue(1);
		searchsmt = new Combobox();
		searchsmt.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		comboitem.setValue(2);
		searchsmt.appendChild(comboitem);
		searchsmt.setCols(2);

		Common.selectComboItem(searchsmt, Common.isNowSemensterGanjil() ? 1 : 2);
		searchsmt.setReadonly(true);
		row.appendChild(searchsmt);
		searchsmt.setWidth("90%");
		searchsmt.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		EventListener eventListenerJenis = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Tbmuser tbmuser = Common.getCurrentUser();
				Criterion criterion = (tbmuser.getSiswa() != null && tbmuser.getSiswa().getId() != null
						? Restrictions.or(Restrictions.isNull("tampilKeSiswa"), Restrictions.eq("tampilKeSiswa", true))
						: Restrictions.sqlRestriction("true"));

				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				Common.insertCombo(jenisRaporSiswa, new String[] { "nama", "kode" }, "keterangan",
						JenisRaporSiswa.class, Restrictions.and(criterion, Restrictions.and(
								s == null || s.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("sekolah", s),
								Restrictions.eq("aktif", true))));
				jenisRaporSiswa.setReadonly(true);
				if (jenisRaporSiswa.getChildren().size() > 0) {
					jenisRaporSiswa.setSelectedIndex(0);
				}
				if (jenisRaporSiswa.getChildren().size() == 1) {
					jenisRaporSiswa.setDisabled(true);
				}
			}
		};
		sekolah.addEventListener("onChange", eventListenerJenis);
		sekolah.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				muatOpsiKelas();
			}
		});
		Common.createDefaultTimer(eventListenerJenis);

		tampilkanKelas = new MyCheckboxConfig("Tampilkan hanya semua siswa di kelas yang dipilih");
		tampilkanKelas.setDisabled(true);

		row = new MyFormRow();
		row.setVisible(tbmuser.getSiswa() == null);
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Kelas : "));
		kelasCombo = new Combobox();
		kelasCombo.setWidth("90%");
		kelasCombo.setReadonly(true);
		row.appendChild(kelasCombo);

		EventListener kelasEventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				KelasSiswa kelasSiswa = kelasCombo.getSelectedItem() == null ? null
						: (KelasSiswa) kelasCombo.getSelectedItem().getValue();
				if (kelasSiswa != null) {
					if (tampilkanKelas.isChecked()) {
						Session session = null;
						try {
							session = bukaSession();
							List<KelasSiswaPunyaSiswa> kelasSiswaPunyaSiswaAtas = ConstantValues.simpleList(
									session.createCriteria(KelasSiswaPunyaSiswa.class)
											.add(Restrictions.eq("kelasSiswa", kelasSiswa))
											.createAlias("siswa", "siswa")
											.addOrder(Order.asc("nomorUrut"))
											.addOrder(Order.asc("siswa.nama")),
									KelasSiswaPunyaSiswa.class);

							for (KelasSiswaPunyaSiswa ksps : kelasSiswaPunyaSiswaAtas) {
								map.put(ksps.getId(), ksps);
							}
						} finally {
							tutupSession(session);
						}
					} else {
						map.clear();
					}
				}

				onSearchDefault(null);
				tampilkanKelas.setDisabled(kelasSiswa == null);
				Common.freeze(center1, tampilkanKelas.isChecked());
			}
		};

		kelasCombo.addEventListener("onChange", kelasEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(tampilkanKelas);
		tampilkanKelas.addEventListener("onClick", kelasEventListener);

		if (tbmuser.getSiswa() == null) {
			center1.setParent(borderlayout1);
			ais.ui.util.ZkCompat.setFlex(center1, true);

			South south1 = new South();
			south1.setParent(borderlayout1);
			south1.setHeight("40px");

			Vbox vbox = new Vbox();
			vbox.setParent(south1);

			paging.setParent(vbox);
			paging.setHeight("30px");

			grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center1);
			grid.setWidth("100%");
			grid.setHeight("100%");

			columns = new Columns();
			columns.setParent(grid);
			column = new MyColumnConfig();
			column.setWidth("45px");
			column.setParent(columns);

			column = new MyColumnConfig("Foto");
			column.setWidth("65px");
			column.setParent(columns);

			column = new MyColumnConfig("Kode");
			column.setParent(columns);

			column = new MyColumnConfig("Nama");
			column.setParent(columns);
		}

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		if (tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null) {
			String namaFile = "sekolah/report";
			north = new org.zkoss.zul.North();
			north.setParent(borderlayout);
			north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {
				@SuppressWarnings({ "unchecked" })
				@Override
				public Map<String, Serializable> generateParameters() throws Exception {
					JenisRaporSiswa j = (JenisRaporSiswa) (jenisRaporSiswa.getSelectedItem() == null ? null
							: jenisRaporSiswa.getSelectedItem().getValue());
					String ta = (String) (tahunAkademik.getSelectedItem() == null ? null
							: tahunAkademik.getSelectedItem().getValue());
					String smta = searchsmt.getValue();
					Integer smtas = (Integer) searchsmt.getSelectedItem().getValue();
					Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null
							: sekolah.getSelectedItem().getValue());
					Date tgl = tanggal.getValue();

					return LaporanRaporSiswa.generateParameter(map, j, tbmuser, ta, smtas, smta, s, tgl);
				}
			}, namaFile, null, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					onReport(arg0);
				}
			}, false));
		}

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				muatOpsiKelas();
				onSearchDefault(null);
			}
		});
	}

	private void muatOpsiKelas() {
		String ta = tahunAkademik.getSelectedItem() == null ? null
				: (String) tahunAkademik.getSelectedItem().getValue();
		Sekolah s = sekolah.getSelectedItem() == null ? null
				: (Sekolah) sekolah.getSelectedItem().getValue();
		Criterion cr = Restrictions.eq("aktif", true);
		if (ta != null) cr = Restrictions.and(cr, Restrictions.eq("tahunAjaran", ta));
		if (s != null && s.getId() != null) cr = Restrictions.and(cr, Restrictions.eq("sekolah", s));
		Common.insertComboDanSemua(kelasCombo, "nama", KelasSiswa.class, cr);
	}

	protected void onSearchDefault(Object object) {
		if (grid != null) {
			Session session = null;
			try {
				session = bukaSession();
				Common.initPaging50(initCriteria(session, false), paging);
				List<KelasSiswaPunyaSiswa> kelasSiswaPunyaSiswas = ConstantValues.simpleList(
						initCriteria(session, true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_50).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE_50 * (paging == null ? 0 : paging.getActivePage())),
						KelasSiswaPunyaSiswa.class);

				List<KelasSiswaPunyaSiswa> siswas = new ArrayList<KelasSiswaPunyaSiswa>();
				siswas.addAll(kelasSiswaPunyaSiswas);
				ListModel strset = new SimpleListModel(siswas);
				grid.setRowRenderer(new SiswaRenderer());
				grid.setModelCheckMobile(strset);
			} finally {
				tutupSession(session);
			}
		}
	}

	class SiswaRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa = (KelasSiswaPunyaSiswa) arg1;

			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setChecked(map.containsKey(kelasSiswaPunyaSiswa.getId()));
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						map.put(kelasSiswaPunyaSiswa.getId(), kelasSiswaPunyaSiswa);
					} else {
						map.remove(kelasSiswaPunyaSiswa.getId());
					}
				}
			});

			CommonMedia.tampilkanGambarKecil(kelasSiswaPunyaSiswa.getSiswa()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(kelasSiswaPunyaSiswa.getSiswa().getNomorInduk()).setParent(vbox);
			new Label(kelasSiswaPunyaSiswa.getKelasSiswa().getNama()).setParent(vbox);

			RevisiHelper.createNewRevisi(KelasSiswaPunyaSiswa.class, kelasSiswaPunyaSiswa.getSiswa(),
					kelasSiswaPunyaSiswa.getSiswa().getNama()).setParent(arg0);
		}
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(Session session, boolean order) {
		List<Long> ids = new ArrayList<Long>();
		Guru guru = null;
		Tbmuser currentUser = Common.getCurrentUser();
		guru = currentUser.ambilGuru();
		
		if (guru != null) {
			ids = session.createCriteria(JadwalPelajaran.class).add(Restrictions.isNotNull("kelas"))
					.setProjection(Projections.groupProperty("kelas.id"))
					.add(tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("tahunAjaran", tahunAkademik.getSelectedItem().getValue()))
					.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("semester", searchsmt.getSelectedItem().getValue()))
					.add(guru == null ? Restrictions.sqlRestriction("1=1") :
							Restrictions.or(Restrictions.eq("guru12", guru), Restrictions.or(
									Restrictions.eq("guru11", guru), Restrictions.or(
											Restrictions.eq("guru10", guru), Restrictions.or(
													Restrictions.eq("guru9", guru), Restrictions.or(
															Restrictions.eq("guru8", guru), Restrictions.or(
																	Restrictions.eq("guru7", guru), Restrictions.or(
																			Restrictions.eq("guru6", guru), Restrictions.or(
																					Restrictions.eq("guru5", guru), Restrictions.or(
																							Restrictions.eq("guru4", guru), Restrictions.or(
																									Restrictions.eq("guru3", guru), Restrictions.or(
																											Restrictions.eq("guru", guru),
																											Restrictions.eq("guru2", guru))))))))))))
					).list();
		}

		KelasSiswa kelasSiswa = kelasCombo == null || kelasCombo.getSelectedItem() == null ? null
				: (KelasSiswa) kelasCombo.getSelectedItem().getValue();
		List<Long> anak = currentUser != null && currentUser.getOrangTua() != null ? currentUser.getOrangTua().ambilAnakSiswa() : new ArrayList<Long>();

		Criterion criterion1 = Restrictions.ilike("siswa.nama", cari.getValue().trim(), MatchMode.ANYWHERE);
		criterion1 = Restrictions.or(criterion1, Restrictions.ilike("siswa.alamatEmail", cari.getValue().trim(), MatchMode.ANYWHERE));
		criterion1 = Restrictions.or(criterion1, Restrictions.ilike("siswa.nomorInduk", cari.getValue().trim(), MatchMode.ANYWHERE));
		// Cari juga berdasarkan NAMA KELAS (mis. "VII-B") supaya bisa memfilter siswa per kelas
		// langsung dari kotak pencarian, selain lewat pemilih "Kelas :". Alias "kelasSiswa" sudah
		// dibuat pada criteria di bawah sebelum criterion ini ditambahkan.
		criterion1 = Restrictions.or(criterion1, Restrictions.ilike("kelasSiswa.nama", cari.getValue().trim(), MatchMode.ANYWHERE));

		Criterion criterion = Restrictions.and(Restrictions.eq("aktif", true),
				kelasSiswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("kelasSiswa", kelasSiswa));
		criterion = Restrictions.and(criterion, Restrictions.eq("kelasSiswa.tahunAjaran", tahunAkademik.getSelectedItem().getValue()));
		criterion = Restrictions.and(criterion, Restrictions.eq("kelasSiswa.aktif", true));
		criterion = Restrictions.and(criterion, currentUser.getSiswa() == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("siswa", currentUser.getSiswa()));
		criterion = Restrictions.and(criterion, anak.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("siswa.id", anak));
		criterion = Restrictions.and(criterion, Restrictions.or(Restrictions.isNull("siswa.aktif"), Restrictions.eq("siswa.aktif", true)));
		criterion = Restrictions.and(criterion, sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true") : CommonSearchFilterHelper.eqSelectedWithId("siswa.sekolah", sekolah, false));
		criterion = Restrictions.and(criterion, cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : criterion1);

		Criteria criteria = session.createCriteria(KelasSiswaPunyaSiswa.class)
				.createAlias("siswa", "siswa")
				.createAlias("kelasSiswa", "kelasSiswa")
				.add(Restrictions.or(map.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", map.keySet()), criterion));

		criteria.add(Restrictions.or(
				guru != null ? Restrictions.eq("kelasSiswa.guruPembina", guru) : Restrictions.sqlRestriction("false"),
				guru != null && ids.isEmpty() ? Restrictions.sqlRestriction("false")
						: ids.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("kelasSiswa.id", ids)));

		if (currentUser != null && currentUser.getOrangTua() != null && !currentUser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			criteria.add(Restrictions.in("siswa.id", currentUser.getOrangTua().ambilAnakSiswa()));
		}

		if (order) criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.nama"));

		return criteria;
	}

	// Wrapper genarateKelompok untuk kompatibilitas panggilan eksternal jika ada.
	@SuppressWarnings({ "rawtypes" })
	public static void genarateKelompok(List list, KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa,
			KelompokMatapelajaran kelompokMatapelajaran, JenisRaporSiswa jenisRaporSiswa, boolean indukAda, Integer smt, Sekolah sekolah) throws Exception {
		Session session = null;
		try {
			session = bukaSession();
			genarateKelompokInternal(session, list, kelasSiswaPunyaSiswa, kelompokMatapelajaran, jenisRaporSiswa, indukAda, smt, sekolah);
		} finally {
			tutupSession(session);
		}
	}

	// Internal method yang menggunakan 1 session bersama (Sangat Optimal!)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void genarateKelompokInternal(Session session, List list, KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa,
			KelompokMatapelajaran kelompokMatapelajaran, JenisRaporSiswa jenisRaporSiswa, boolean indukAda, Integer smt, Sekolah sekolah) throws Exception {

		Siswa siswa = kelasSiswaPunyaSiswa.getSiswa();
		List<Long> longs = kelasSiswaPunyaSiswa == null ? new ArrayList<Long>() : kelasSiswaPunyaSiswa.ambilMk();
		List<Long> longsjenis = jenisRaporSiswa == null ? new ArrayList<Long>() : jenisRaporSiswa.ambilMk();
		// Cache rata-rata kelas/angkatan kini ditangani helper berlapis (L1/L2 per-RUN via
		// ThreadLocal + L3 lintas-request opsional). Map per-pemanggilan lama dihapus agar
		// hitungan dibagi antar SEMUA siswa dalam satu kali generate (bukan per siswa).

		JSONObject js = new JSONObject();
		try { js = new JSONObject(smt == 1 ? kelasSiswaPunyaSiswa.getKeterangan1() : kelasSiswaPunyaSiswa.getKeterangan2()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:808");}

		List<KurikulumPunyaMatapelajaran> kurikulumPunyaMatapelajarans = ConstantValues.simpleList(
				session.createCriteria(KurikulumPunyaMatapelajaran.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("kurikulumSekolah", kelasSiswaPunyaSiswa.getKelasSiswa().getKurikulumSekolah()))
						.createAlias("matapelajaran", "matapelajaran")
						.add(longs == null || longs.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.not(Restrictions.in("matapelajaran.id", longs)))
						.add(longsjenis == null || longsjenis.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.not(Restrictions.in("matapelajaran.id", longsjenis)))
						.createAlias("matapelajaran.kelompokMatapelajaran", "kelompokMatapelajaran")
						.add(indukAda ? Restrictions.eq("kelompokMatapelajaran.induk", kelompokMatapelajaran) : Restrictions.eq("matapelajaran.kelompokMatapelajaran", kelompokMatapelajaran))
						.addOrder(Order.asc("kelompokMatapelajaran.nama")).addOrder(Order.asc("matapelajaran.urutan")),
				KurikulumPunyaMatapelajaran.class);

		if (kurikulumPunyaMatapelajarans.isEmpty()) {
			List<KelompokMatapelajaran> kelompokMatapelajarans = ConstantValues.simpleList(
					session.createCriteria(KelompokMatapelajaran.class).addOrder(Order.asc("nomorUrut"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(sekolah == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("sekolah", sekolah))
							.add(Restrictions.eq("induk", kelompokMatapelajaran)), KelompokMatapelajaran.class);

			for (KelompokMatapelajaran kelompokMatapelajaranSub : kelompokMatapelajarans) {
				genarateKelompokInternal(session, list, kelasSiswaPunyaSiswa, kelompokMatapelajaranSub, jenisRaporSiswa, true, smt, sekolah);
			}

		} else {
			Map<Long, List<GrupPenilaian>> mapGrup = new HashMap<Long, List<GrupPenilaian>>();
			Map<Long, List<GrupKategoriItemPenilaianSiswa>> mapGrupKat = new HashMap<Long, List<GrupKategoriItemPenilaianSiswa>>();
			
			for (KurikulumPunyaMatapelajaran kpm : kurikulumPunyaMatapelajarans) {
				JenisPenilaian jenisPenilaian = kpm.getMatapelajaran().getJenisPenilaian();
				if (kpm != null && kpm.getKurikulumSekolah() != null && kpm.getKurikulumSekolah().getJenisPenilaian() != null) {
					jenisPenilaian = kpm.getKurikulumSekolah().getJenisPenilaian();
				}

				if (jenisPenilaian != null && jenisPenilaian.getId() != null && !mapGrup.containsKey(jenisPenilaian.getId())) {
					List<GrupPenilaian> grupPenilaians = ConstantValues.simpleList(
							session.createCriteria(DetailJenisPenilaian.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("jenisPenilaian", jenisPenilaian))
									.setProjection(Projections.groupProperty("grupPenilaian.id")),
							GrupPenilaian.class, false);

					mapGrup.put(jenisPenilaian.getId(), grupPenilaians);

					for (GrupPenilaian grupPenilaian : grupPenilaians) {
						if (!mapGrupKat.containsKey(grupPenilaian.getId())) {
							List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = ConstantValues.simpleList(
									session.createCriteria(DetailGrupPenilaian.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
											.add(Restrictions.isNotNull("grupKategoriItemPenilaianSiswa"))
											.setProjection(Projections.groupProperty("grupKategoriItemPenilaianSiswa.id"))
											.add(Restrictions.eq("grupPenilaian", grupPenilaian)),
									GrupKategoriItemPenilaianSiswa.class, false);
							mapGrupKat.put(grupPenilaian.getId(), grupKategoriItemPenilaianSiswas);
						}
					}
				}
			}

			for (KurikulumPunyaMatapelajaran kpm : kurikulumPunyaMatapelajarans) {
				Matapelajaran matapelajaran = kpm.getMatapelajaran();
				if (!longs.contains(matapelajaran.getId())) {
					Guru guru = (Guru) ConstantValues.simpleObject(
							session.createCriteria(JadwalPelajaran.class)
									.setProjection(Projections.property("guru.id"))
									.add(Restrictions.eq("semester", smt))
									.add(Restrictions.eq("kelas", kelasSiswaPunyaSiswa.getKelasSiswa()))
									.add(Restrictions.eq("matapelajaran", kpm.getMatapelajaran()))
									.setMaxResults(1),
							Guru.class, false);

					JenisPenilaian jenisPenilaian = kpm.getMatapelajaran().getJenisPenilaian();
					if (kpm != null && kpm.getKurikulumSekolah() != null && kpm.getKurikulumSekolah().getJenisPenilaian() != null) {
						jenisPenilaian = kpm.getKurikulumSekolah().getJenisPenilaian();
					}

					if (jenisPenilaian != null) {
						Map<String, Object> parameters = new HashMap<String, Object>();
						siapkanParemeter(parameters, kelasSiswaPunyaSiswa, smt, "");
						list.add(parameters);
						
						parameters.put("jenis", jenisPenilaian.getNama());
						parameters.put("ta", kelasSiswaPunyaSiswa.getKelasSiswa().getTahunAjaran());
						parameters.put("smt", smt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
						parameters.put("smt_angka", smt);
						parameters.put("smt_text", smt == 1 ? "Satu" : "Dua");

						if (guru != null) {
							Common.insertProperty(Guru.class, guru, parameters, "guru");
							parameters.put("url_foto_guru", CommonMedia.getUrlFotoPengguna(new Tbmuser(guru)));
						}
						parameters.put("kode_guru", guru == null ? "" : guru.getKode());
						parameters.put("nip_guru", guru == null ? "" : guru.getNip());
						parameters.put("nama_guru", guru == null ? "" : guru.getNama());
						parameters.put("sub_jenis", kpm.getMatapelajaran().getKelompokMatapelajaran().getNama());
						parameters.put("matapelajaran", kpm.getMatapelajaran().getNama());
						parameters.put("kode", kpm.getMatapelajaran().getKode());
						parameters.put("kkm", kpm.getMatapelajaran().getKkm());

						List<GrupPenilaian> grupPenilaians = mapGrup.get(jenisPenilaian.getId());
						Double totalSemua = 0.0, jumlahSemua = 0.0, grupMax = Double.MIN_VALUE, grupMin = Double.MAX_VALUE;
						Long grupIdMax = -1L, grupIdMin = -1L;
						String grupIdMaxKet = "", grupIdMinKet = "";
						Date sekarang = WaktuUtil.getDate();

						for (GrupPenilaian grupPenilaian : grupPenilaians) {
							String keyKet = kpm.getMatapelajaran().getId() + "_" + grupPenilaian.getId();
							String ket = js.isNull(keyKet) ? "" : js.getString(keyKet);
							parameters.put("keterangan_nilai_" + grupPenilaian.getId(), ket);

							List<GrupKategoriItemPenilaianSiswa> gKats = mapGrupKat.get(grupPenilaian.getId());
							Double total = 0.0, min = 0.0, totalKelas = 0.0, jumlahKelas = 0.0, max = 0.0;

							try {
								String formula = grupPenilaian.getFormula();
								String target = GrupPenilaianUtil.ambilTarget(formula, sekarang);
								total = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(target, kpm.getMatapelajaran(), grupPenilaian, smt, gKats);

								if ((jenisRaporSiswa != null && jenisRaporSiswa.getHitungRataRataKelas())) {
									// total & jumlah kelas SAMA untuk semua siswa sekelas → cache per
									// (grup, kelas, mapel, smt, target): hitung sekali per kelas.
									String keyTk = grupPenilaian.getId() + "_" + kelasSiswaPunyaSiswa.getKelasSiswa().getId()
											+ "_" + kpm.getMatapelajaran().getId() + "_" + smt + "_" + (target == null ? "" : target);
									double[] tk = ambilCacheTotalKelas(keyTk);
									if (tk != null) {
										totalKelas = tk[0];
										jumlahKelas = tk[1];
									} else {
										List<KelasSiswaPunyaSiswa> kspsList = ConstantValues.simpleList(
												session.createCriteria(KelasSiswaPunyaSiswa.class)
														.add(Restrictions.eq("kelasSiswa", kelasSiswaPunyaSiswa.getKelasSiswa())),
												KelasSiswaPunyaSiswa.class);

										for (KelasSiswaPunyaSiswa kspsLocal : kspsList) {
											totalKelas += kspsLocal.retreiveTotalNilaiTotal(target, kpm.getMatapelajaran(), grupPenilaian, smt, gKats);
											jumlahKelas += 1.0;
										}
										simpanCacheTotalKelas(keyTk, new double[] { totalKelas, jumlahKelas });
									}
								}

								min = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(GrupPenilaianUtil.ambilTargetMin(formula, sekarang), kpm.getMatapelajaran(), grupPenilaian, smt, gKats);
								max = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(GrupPenilaianUtil.ambilTargetMax(formula, sekarang), kpm.getMatapelajaran(), grupPenilaian, smt, gKats);

							} catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:953"); }

							totalSemua += total; jumlahSemua += 1.0;
							parameters.put("total_" + grupPenilaian.getId(), total);
							parameters.put("totalKelas_" + grupPenilaian.getId(), totalKelas);
							parameters.put("jumlahKelas_" + grupPenilaian.getId(), jumlahKelas);
							parameters.put("rataRataKelas_" + grupPenilaian.getId(), (jumlahKelas > 0 ? totalKelas / jumlahKelas : 0));
							parameters.put("min_" + grupPenilaian.getId(), min);
							parameters.put("max_" + grupPenilaian.getId(), max);

							if (grupMax < max) { grupMax = max; grupIdMax = grupPenilaian.getId(); grupIdMaxKet = ket; }
							if (grupMin > min) { grupMin = min; grupIdMin = grupPenilaian.getId(); grupIdMinKet = ket; }

							NilaiHurufSekolah nhs = NilaiHurufSekolah.getNilaiHurufSekolah(total, siswa.getTahunMasuk(), siswa.getSekolah(), siswa.getYayasan(),
									kelasSiswaPunyaSiswa.getKelasSiswa().getTahunAjaran(), smt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, grupPenilaian.getJenisNilaiHuruf());
							parameters.put("huruf_" + grupPenilaian.getId(), nhs == null ? "" : nhs.getNilaiHuruf());

							List maps = new ArrayList<Map<String, Object>>();
							JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(maps);

							for (GrupKategoriItemPenilaianSiswa gKat : gKats) {
								List<KategoriItemPenilaianSiswa> katsId = ConstantValues.simpleList(
										session.createCriteria(DetailGrupKategoriItemPenilaianSiswa.class)
												.add(Restrictions.eq("grupKategoriItemPenilaianSiswa", gKat))
												.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
												.setProjection(Projections.groupProperty("kategoriItemPenilaianSiswa.id")),
										KategoriItemPenilaianSiswa.class, false);

								List<JenisItemPenilaianSiswa> jenisItems = ConstantValues.simpleList(
										session.createCriteria(JenisItemPenilaianSiswa.class)
												.createAlias("kategoriItemPenilaianSiswa", "kategoriItemPenilaianSiswa")
												.addOrder(Order.asc("kategoriItemPenilaianSiswa.kode")).addOrder(Order.asc("nomorUrut"))
												.add(katsId.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("kategoriItemPenilaianSiswa", katsId))
												.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
										JenisItemPenilaianSiswa.class);

								Double totalLocal = 0.0;
								NilaiHurufSekolah nhsLocal = null;
								try {
									totalLocal = kelasSiswaPunyaSiswa.retreiveTotalNilai(jenisItems, GrupPenilaianUtil.ambilTarget(gKat.getFormula(), sekarang), kpm.getMatapelajaran(), grupPenilaian, gKat, smt, null);
									nhsLocal = NilaiHurufSekolah.getNilaiHurufSekolah(totalLocal, siswa.getTahunMasuk(), siswa.getSekolah(), siswa.getYayasan(), kelasSiswaPunyaSiswa.getKelasSiswa().getTahunAjaran(), smt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, grupPenilaian.getJenisNilaiHuruf());
								} catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:994"); }

								parameters.put("total_local_" + gKat.getId(), totalLocal);
								parameters.put("total_huruf_local_" + gKat.getId(), nhsLocal == null ? "" : nhsLocal.getNilaiHuruf());

								for (JenisItemPenilaianSiswa jItem : jenisItems) {
									String key = gKat.getKode() + "_" + jItem.getKode();

									if (jItem.getHitungRataRataKelas()) {
										String keyRata = "kls_" + jItem.getId() + "_" + kelasSiswaPunyaSiswa.getKelasSiswa().getId()
												+ "_" + kpm.getMatapelajaran().getId() + "_" + smt;
										Double n = ambilCacheRata(keyRata);
										if (n == null) {
											List<KelasSiswaPunyaSiswa> kspsList = ConstantValues.simpleList(
													session.createCriteria(KelasSiswaPunyaSiswa.class).add(Restrictions.eq("kelasSiswa", kelasSiswaPunyaSiswa.getKelasSiswa())),
													KelasSiswaPunyaSiswa.class);

											Double rataRataKelas = 0.0, jumlahRata = 0.0;
											for (KelasSiswaPunyaSiswa kspsLocal : kspsList) {
												String val = kspsLocal.retreiveDetailNilai(jItem, gKat, kpm.getMatapelajaran(), smt, null);
												val = (val == null || val.trim().isEmpty()) ? "0.0" : val;
												try {
													if (Common.isNumber(val)) {
														Double nilaiAngka = Double.parseDouble(val);
														if (nilaiAngka > 0.1) { rataRataKelas += nilaiAngka; jumlahRata += 1.0; }
													}
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:1020");}
											}
											n = jumlahRata.intValue() == 0 ? 0.0 : (rataRataKelas / jumlahRata);
											simpanCacheRata(keyRata, n);
										}
										parameters.put(key + "_rata_rata_kelas", n);
										parameters.put(key + "_rata_rata_kelas_text", Common.numberFormat.get().format(n));
									}

									if (jItem.getHitungRataRataAngkatan()) {
										String keyRata = "agk_" + jItem.getId() + "_" + kelasSiswaPunyaSiswa.getSiswa().getTahunMasuk()
												+ "_" + kelasSiswaPunyaSiswa.getSiswa().getSekolah().getId() + "_" + kpm.getMatapelajaran().getId() + "_" + smt;
										Double n = ambilCacheRata(keyRata);
										if (n == null) {
											List<KelasSiswaPunyaSiswa> kspsList = ConstantValues.simpleList(
													session.createCriteria(KelasSiswaPunyaSiswa.class).createAlias("siswa", "siswa")
															.add(Restrictions.eq("siswa.tahunMasuk", kelasSiswaPunyaSiswa.getSiswa().getTahunMasuk()))
															.add(Restrictions.eq("siswa.sekolah", kelasSiswaPunyaSiswa.getSiswa().getSekolah())),
													KelasSiswaPunyaSiswa.class);

											Double rataRataKelas = 0.0, jumlahRata = 0.0;
											for (KelasSiswaPunyaSiswa kspsLocal : kspsList) {
												String val = kspsLocal.retreiveDetailNilai(jItem, gKat, kpm.getMatapelajaran(), smt, null);
												val = (val == null || val.trim().isEmpty()) ? "0.0" : val;
												try {
													if (Common.isNumber(val)) {
														Double nilaiAngka = Double.parseDouble(val);
														if (nilaiAngka > 0.1) { rataRataKelas += nilaiAngka; jumlahRata += 1.0; }
													}
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:1049");}
											}
											n = jumlahRata.intValue() == 0 ? 0.0 : (rataRataKelas / jumlahRata);
											simpanCacheRata(keyRata, n);
										}
										parameters.put(key + "_rata_rata_angkatan", n);
										parameters.put(key + "_rata_rata_angkatan_text", Common.numberFormat.get().format(n));
									}

									String val = kelasSiswaPunyaSiswa.retreiveDetailNilai(jItem, gKat, kpm.getMatapelajaran(), smt, null);
									val = (val == null || val.trim().isEmpty()) ? "0.0" : val;
									Boolean sesuai = kelasSiswaPunyaSiswa.retreiveDetailVerify(jItem, gKat, kpm.getMatapelajaran(), smt);
									parameters.put(key + "_ok", sesuai);

									Double nilaiAngka = 0.0;
									try { if (Common.isNumber(val)) nilaiAngka = Double.parseDouble(val); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:1064");}

									parameters.put(key, val);
									parameters.put(key + "_angka", nilaiAngka);

									Map<String, Object> mapData = new HashMap<String, Object>();
									mapData.put("nilai_ok", sesuai);
									mapData.put("nilai", val);
									mapData.put("nilai_angka", nilaiAngka);
									mapData.put("nama", jItem.getNama());
									mapData.put("grup", gKat.getNama());
									mapData.put("kelas", kelasSiswaPunyaSiswa.getNama());
									maps.add(mapData);

									if (totalLocal.intValue() == min.intValue()) parameters.put(jItem.getKode() + "_min_" + grupPenilaian.getId(), val);
									if (totalLocal.intValue() == max.intValue()) parameters.put(jItem.getKode() + "_max_" + grupPenilaian.getId(), val);
								}

								if (kpm.getMatapelajaran().getKelompokMatapelajaran() != null) {
									genarateKelompokInternal(session, list, kelasSiswaPunyaSiswa, kpm.getMatapelajaran().getKelompokMatapelajaran(), jenisRaporSiswa, true, smt, sekolah);
								}
							}
							parameters.put("jr_maps_grup_nilai_" + grupPenilaian.getId(), dataSource);
							parameters.put("maps_grup_nilai_" + grupPenilaian.getId(), maps);
						}

						Double total = jumlahSemua > 0 ? totalSemua / jumlahSemua : 0.0;
						parameters.put("total", total);
						parameters.put("grupMax", grupMax);
						parameters.put("grupMin", grupMin);
						parameters.put("grupIdMax", grupIdMax);
						parameters.put("grupIdMin", grupIdMin);
						parameters.put("grupIdMinKet", grupIdMinKet);
						parameters.put("grupIdMaxKet", grupIdMaxKet);

						NilaiHurufSekolah nhs = NilaiHurufSekolah.getNilaiHurufSekolah(total, siswa.getTahunMasuk(), siswa.getSekolah(), siswa.getYayasan(),
								kelasSiswaPunyaSiswa.getKelasSiswa().getTahunAjaran(), smt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, null);
						parameters.put("huruf", nhs == null ? "" : nhs.getNilaiHuruf());
					}
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void masukkanPoin(Session session, Siswa siswa, String ta, Map parameters, List<KegiatanSiswa> kegiatanSiswas,
			List<PelanggaranSiswa> pelanggaranSiswas, List<ApresiasiSiswa> apresiasiSiswas) {

		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		Double totalPointKegiatan = 0.0;
		for (KegiatanSiswa kegiatan : kegiatanSiswas) {
			if (kegiatan.getSiswa() != null && siswa.getId().equals(kegiatan.getSiswa().getId())) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("id", kegiatan.getId());
				map.put("kelompok", kegiatan.getKelompokKegiatanSiswa().getNama());
				map.put("poin", kegiatan.getKelompokKegiatanSiswa().getPoin());
				map.put("keterangan", kegiatan.getKeterangan());
				map.put("tanggal", kegiatan.getWaktu());
				totalPointKegiatan += kegiatan.getKelompokKegiatanSiswa().getPoin();
				maps.add(map);
			}
		}
		parameters.put("totalPointKegiatan_" + siswa.getId(), totalPointKegiatan);
		parameters.put("kegiatanSiswas_" + siswa.getId(), maps);

		maps = new ArrayList<Map<String, Object>>();
		Double totalPointHukuman = 0.0;
		for (PelanggaranSiswa pelanggaran : pelanggaranSiswas) {
			if (pelanggaran.getSiswa() != null && siswa.getId().equals(pelanggaran.getSiswa().getId())) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("id", pelanggaran.getId());
				map.put("pelanggaranDanHukuman", pelanggaran.getPelanggaranDanHukuman().getNama());
				map.put("keterangan", pelanggaran.getKeterangan());
				map.put("tanggal", pelanggaran.getWaktu());

				StringBuilder hukumans = new StringBuilder();
				Double pointHukuman = 0.0;
				try {
					session.refresh(pelanggaran);
					for (Hukuman hukuman : pelanggaran.getHukumans()) {
						if (hukumans.length() > 0) hukumans.append(", ");
						hukumans.append(hukuman.getNama());
						map.put("hukuman_" + hukuman.getId(), hukuman.getNama());
						map.put("poin_" + hukuman.getId(), hukuman.getPoin());
						totalPointKegiatan += hukuman.getPoin();
						pointHukuman += hukuman.getPoin();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:1151");}
				map.put("hukumans", hukumans.toString());
				map.put("poin", pointHukuman);
				maps.add(map);
			}
		}
		parameters.put("totalPointHukuman_" + siswa.getId(), totalPointHukuman);
		parameters.put("pelanggaranSiswas_" + siswa.getId(), maps);

		maps = new ArrayList<Map<String, Object>>();
		Double totalPointPenghargaan = 0.0;
		for (ApresiasiSiswa apresiasi : apresiasiSiswas) {
			if (apresiasi.getSiswa() != null && siswa.getId().equals(apresiasi.getSiswa().getId())) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("id", apresiasi.getId());
				map.put("apresiasiDanPenghargaan", apresiasi.getApresiasiDanPenghargaan().getNama());
				map.put("keterangan", apresiasi.getKeterangan());
				map.put("tanggal", apresiasi.getWaktu());

				StringBuilder penghargaans = new StringBuilder();
				Double pointPenghargaan = 0.0;
				try {
					session.refresh(apresiasi);
					for (Penghargaan penghargaan : apresiasi.getPenghargaans()) {
						if (penghargaans.length() > 0) penghargaans.append(", ");
						penghargaans.append(penghargaan.getNama());
						map.put("penghargaan_" + penghargaan.getId(), penghargaan.getNama());
						map.put("poin_" + penghargaan.getId(), penghargaan.getPoin());
						totalPointKegiatan += penghargaan.getPoin();
						pointPenghargaan += penghargaan.getPoin();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:1182");}
				map.put("penghargaans", penghargaans.toString());
				map.put("poin", pointPenghargaan);
				maps.add(map);
			}
		}
		parameters.put("totalPointPenghargaan_" + siswa.getId(), totalPointPenghargaan);
		parameters.put("apresiasiSiswas_" + siswa.getId(), maps);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map generateParameter(Map<Long, KelasSiswaPunyaSiswa> mapsData, JenisRaporSiswa jenisRaporSiswa,
			Tbmuser tbmuser, String ta, Integer smtas, String smta, Sekolah sekolah, Date tanggal) throws Exception {
		
		Map<String, Object> parameters = ais.common.HashMapGenerator.getRand();
		// EKSPOR DINAMIS: bila jenis rapor yang dipilih punya jrxml layout terupload,
		// pakai sebagai template untuk SEMUA format ekspor (PDF/XLS/DOCX/PPTX) — selaras
		// dgn pratinjau (onReport). generateFileReportCore meng-compile jrxml dari key
		// "nama_laporan" sehingga menimpa template default "sekolah/report".
		if (jenisRaporSiswa != null && jenisRaporSiswa.getId() != null) {
			try {
				LampiranLain layoutDinamis = LampiranLain.ambil(jenisRaporSiswa.getId(),
						LampiranLain.FILE_JRXML_LAYOUT_JENIS_RAPOR);
				if (layoutDinamis != null && layoutDinamis.ambilFile() != null
						&& layoutDinamis.ambilFile().exists()) {
					parameters.put("nama_laporan", layoutDinamis.ambilFile().getAbsolutePath());
				}
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:1209");
				// best-effort: gagal resolve template dinamis -> pakai template default
			}
		}
		for (Object o : ConstantValues.ambilBerdasarClass(Konstanta.class).values()) {
			Konstanta konstanta = (Konstanta) o;
			if (konstanta != null) parameters.put(konstanta.getKode(), konstanta.getKeterangan());
		}
		HashMap<String, Map<String, Object>> ada = new HashMap<String, Map<String, Object>>();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		Session session = null;
		try {
			// Mulai cache per-RUN (L2) supaya rata-rata kelas/angkatan dihitung sekali per kelas
			// untuk SELURUH siswa dalam satu kali generate (bukan diulang per siswa).
			mulaiRunCacheRapor();
			session = bukaSession();

			if (tbmuser.getSiswa() != null) {
				List<KelasSiswaPunyaSiswa> kelasSiswaPunyaSiswaAtas = ConstantValues.simpleList(
						session.createCriteria(KelasSiswaPunyaSiswa.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.createAlias("kelasSiswa", "kelasSiswa")
								.add(Restrictions.eq("kelasSiswa.tahunAjaran", ta))
								.add(Restrictions.eq("kelasSiswa.aktif", true))
								.add(tbmuser.getSiswa() == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("siswa", tbmuser.getSiswa())),
						KelasSiswaPunyaSiswa.class);

				int size = kelasSiswaPunyaSiswaAtas.size();
				int count = 1;
				for (KelasSiswaPunyaSiswa ksps : kelasSiswaPunyaSiswaAtas) {
					mapsData.put(ksps.getId(), ksps);
					System.out.println("Proses raport " + ksps.getSiswa() + " (" + Common.numberFormat.get().format((count * 100.0) / size) + "%)");
					count++;
				}
			}

			if (jenisRaporSiswa != null && jenisRaporSiswa.getUntukSemuaSemester()) {
				List<Siswa> siswas = new ArrayList<Siswa>();
				for (KelasSiswaPunyaSiswa ksps : mapsData.values()) siswas.add(ksps.getSiswa());
				List<Long> longsjenis = jenisRaporSiswa.ambilMk();

				List<AbsenPiket> absenPikets = (jenisRaporSiswa != null && jenisRaporSiswa.getAmbilAbsenPiket()) ? session.createCriteria(AbsenPiket.class)
						.add(sekolah == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("sekolah", sekolah))
						.add(Restrictions.eq("tahunAjaran", ta)).add(Restrictions.eq("semester", smtas))
						.addOrder(Order.asc("tanggal")).addOrder(Order.asc("id")).list() : new ArrayList<AbsenPiket>();

				List<KegiatanSiswa> kegiatanSiswas = (jenisRaporSiswa != null && jenisRaporSiswa.getAmbilKegiatanSiswa()) ? session.createCriteria(KegiatanSiswa.class)
						.add(Restrictions.eq("ta", ta)).add(siswas.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("siswa", siswas))
						.addOrder(Order.asc("waktu")).addOrder(Order.asc("id")).list() : new ArrayList<KegiatanSiswa>();

				List<PelanggaranSiswa> pelanggaranSiswas = (jenisRaporSiswa != null && jenisRaporSiswa.getAmbilPelanggaranSiswa()) ? session.createCriteria(PelanggaranSiswa.class)
						.add(Restrictions.eq("ta", ta)).add(siswas.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("siswa", siswas))
						.addOrder(Order.asc("waktu")).addOrder(Order.asc("id")).list() : new ArrayList<PelanggaranSiswa>();

				List<ApresiasiSiswa> apresiasiSiswas = (jenisRaporSiswa != null && jenisRaporSiswa.getAmbilApresiasiSiswa()) ? session.createCriteria(ApresiasiSiswa.class)
						.add(Restrictions.eq("ta", ta)).add(siswas.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("siswa", siswas))
						.addOrder(Order.asc("waktu")).addOrder(Order.asc("id")).list() : new ArrayList<ApresiasiSiswa>();

				int size = mapsData.size();
				int count = 1;
				for (KelasSiswaPunyaSiswa k : mapsData.values()) {
					Siswa siswa = k.getSiswa();
					System.out.println("Proses raport " + siswa + " (" + Common.numberFormat.get().format((count * 100.0) / size) + "%)");
					count++;

					if ((jenisRaporSiswa != null && jenisRaporSiswa.getAmbilPelanggaranSiswa()) || (jenisRaporSiswa != null && jenisRaporSiswa.getAmbilApresiasiSiswa())) {
						masukkanPoin(session, siswa, k.getKelasSiswa().getTahunAjaran(), parameters, kegiatanSiswas, pelanggaranSiswas, apresiasiSiswas);
					}

					KelasSiswa kelasSiswaData = siswa.getKelas();
					int tingkat = kelasSiswaData == null ? 100 : kelasSiswaData.getTingkat();

					List<KelasSiswaPunyaSiswa> kspsList = ConstantValues.simpleList(
							session.createCriteria(KelasSiswaPunyaSiswa.class).createAlias("kelasSiswa", "kelasSiswa")
									.add(Restrictions.le("kelasSiswa.tingkat", tingkat)).addOrder(Order.asc("kelasSiswa.tingkat"))
									.addOrder(Order.asc("kelasSiswa.tahunAjaran")).add(Restrictions.eq("siswa", siswa)),
							KelasSiswaPunyaSiswa.class);

					for (KelasSiswaPunyaSiswa kspsLocal : kspsList) {
						List<Long> longs = kspsLocal.ambilMk();
						List<KurikulumPunyaMatapelajaran> kpmList = ConstantValues.simpleList(
								session.createCriteria(KurikulumPunyaMatapelajaran.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("kurikulumSekolah", kspsLocal.getKelasSiswa().getKurikulumSekolah()))
										.createAlias("matapelajaran", "matapelajaran")
										.add(longs == null || longs.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.not(Restrictions.in("matapelajaran.id", longs)))
										.add(longsjenis == null || longsjenis.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.not(Restrictions.in("matapelajaran.id", longsjenis)))
										.createAlias("matapelajaran.kelompokMatapelajaran", "kelompokMatapelajaran")
										.addOrder(Order.asc("kelompokMatapelajaran.nama")).addOrder(Order.asc("matapelajaran.urutan")),
								KurikulumPunyaMatapelajaran.class);

						Map<Long, List<GrupPenilaian>> mapGrup = new HashMap<Long, List<GrupPenilaian>>();
						Map<Long, List<GrupKategoriItemPenilaianSiswa>> mapGrupKat = new HashMap<Long, List<GrupKategoriItemPenilaianSiswa>>();
						
						for (KurikulumPunyaMatapelajaran kpm : kpmList) {
							JenisPenilaian jp = kpm.getMatapelajaran().getJenisPenilaian();
							if (kpm != null && kpm.getKurikulumSekolah() != null && kpm.getKurikulumSekolah().getJenisPenilaian() != null) {
								jp = kpm.getKurikulumSekolah().getJenisPenilaian();
							}
							if (!mapGrup.containsKey(jp.getId())) {
								List<GrupPenilaian> gpList = ConstantValues.simpleList(
										session.createCriteria(DetailJenisPenilaian.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
												.add(Restrictions.eq("jenisPenilaian", jp)).setProjection(Projections.groupProperty("grupPenilaian.id")),
										GrupPenilaian.class, false);
								mapGrup.put(jp.getId(), gpList);
								for (GrupPenilaian gp : gpList) {
									if (!mapGrupKat.containsKey(gp.getId())) {
										List<GrupKategoriItemPenilaianSiswa> gKatList = ConstantValues.simpleList(
												session.createCriteria(DetailGrupPenilaian.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
														.add(Restrictions.isNotNull("grupKategoriItemPenilaianSiswa")).setProjection(Projections.groupProperty("grupKategoriItemPenilaianSiswa.id"))
														.add(Restrictions.eq("grupPenilaian", gp)),
												GrupKategoriItemPenilaianSiswa.class, false);
										mapGrupKat.put(gp.getId(), gKatList);
									}
								}
							}
						}

						for (int smt : new Integer[] { 1, 2 }) {
							for (KurikulumPunyaMatapelajaran kpm : kpmList) {
								JenisPenilaian jp = kpm.getMatapelajaran().getJenisPenilaian();
								if (kpm != null && kpm.getKurikulumSekolah() != null && kpm.getKurikulumSekolah().getJenisPenilaian() != null) {
									jp = kpm.getKurikulumSekolah().getJenisPenilaian();
								}
								String prefix = kspsLocal.getKelasSiswa().getTingkat() + "." + smt + ".";
								Map<String, Object> map = ada.get(siswa.getId() + "_" + kpm.getMatapelajaran().getId());
								if (map == null) {
									map = new HashMap<String, Object>();
									ada.put(siswa.getId() + "_" + kpm.getMatapelajaran().getId(), map);
									list.add(map);
								}
								siapkanParemeter(map, kspsLocal, smt, "");
								map.put("jenis", jp.getNama());
								map.put(prefix + "jenis", jp.getNama());
								map.put("sub_jenis", kpm.getMatapelajaran().getKelompokMatapelajaran().getNama());
								map.put(prefix + "sub_jenis", kpm.getMatapelajaran().getKelompokMatapelajaran().getNama());
								map.put("singkatan", kpm.getMatapelajaran().getSingkatan());
								map.put("matapelajaran", kpm.getMatapelajaran().getNama());
								map.put("kode", kpm.getMatapelajaran().getKode());
								map.put("kkm", kpm.getMatapelajaran().getKkm());
								map.put(prefix + "matapelajaran", kpm.getMatapelajaran().getNama());
								map.put(prefix + "kode", kpm.getMatapelajaran().getKode());
								map.put(prefix + "kkm", kpm.getMatapelajaran().getKkm());

								List<GrupPenilaian> gpList = mapGrup.get(jp.getId());
								Double totalSemua = 0.0, jumlahSemua = 0.0;

								for (GrupPenilaian gp : gpList) {
									List<GrupKategoriItemPenilaianSiswa> gKatList = mapGrupKat.get(gp.getId());
									Date sekarang = WaktuUtil.getDate();
									Double totalGrup = 0.0, jumlahGrup = 0.0;
									String keyKet = kpm.getMatapelajaran().getId() + "_" + gp.getId() + "_" + kspsLocal.getKelasSiswa().getTahunAjaran() + "_" + smt;
									Double total = 0.0, min = 0.0, max = 0.0;

									try {
										String formula = gp.getFormula();
										total = kspsLocal.retreiveTotalNilaiTotal(GrupPenilaianUtil.ambilTarget(formula, sekarang), kpm.getMatapelajaran(), gp, smt, gKatList);
										min = kspsLocal.retreiveTotalNilaiTotal(GrupPenilaianUtil.ambilTargetMin(formula, sekarang), kpm.getMatapelajaran(), gp, smt, gKatList);
										max = kspsLocal.retreiveTotalNilaiTotal(GrupPenilaianUtil.ambilTargetMax(formula, sekarang), kpm.getMatapelajaran(), gp, smt, gKatList);
									} catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:1369"); }

									totalSemua += total; jumlahSemua += 1.0;
									totalGrup += total; jumlahGrup += 1.0;

									map.put(prefix + "total_" + keyKet, total);
									map.put(prefix + "min_" + keyKet, min);
									map.put(prefix + "max_" + keyKet, max);
									map.put("total_" + keyKet, total);
									map.put("min_" + keyKet, min);
									map.put("max_" + keyKet, max);

									NilaiHurufSekolah nhs = NilaiHurufSekolah.getNilaiHurufSekolah(total, siswa.getTahunMasuk(), siswa.getSekolah(), siswa.getYayasan(), kspsLocal.getKelasSiswa().getTahunAjaran(), smt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, gp.getJenisNilaiHuruf());
									map.put(prefix + "huruf_" + keyKet, nhs == null ? "" : nhs.getNilaiHuruf());
									map.put(prefix + "ta", kspsLocal.getKelasSiswa().getTahunAjaran());
									map.put(prefix + "totalGrup_" + gp.getId(), totalGrup);
									map.put(prefix + "jumlahGrup_" + gp.getId(), jumlahGrup);
									map.put(prefix + "rata_rata_Grup_" + gp.getId(), jumlahGrup > 0 ? totalGrup / jumlahGrup : 0);
									map.put("huruf_" + keyKet, nhs == null ? "" : nhs.getNilaiHuruf());
									map.put("huruf_" + gp.getId(), nhs == null ? "" : nhs.getNilaiHuruf());
									map.put("ta", kspsLocal.getKelasSiswa().getTahunAjaran());
									map.put("total_" + gp.getId(), totalGrup);
									map.put("jumlah_" + gp.getId(), jumlahGrup);
									map.put("rata_rata_" + gp.getId(), jumlahGrup > 0 ? totalGrup / jumlahGrup : 0);
									map.put("totalGrup_" + gp.getId(), totalGrup);
									map.put("jumlahGrup_" + gp.getId(), jumlahGrup);
									map.put("rata_rata_Grup_" + gp.getId(), jumlahGrup > 0 ? totalGrup / jumlahGrup : 0);
								}
								map.put(prefix + "totalSemua", totalSemua);
								map.put(prefix + "jumlahSemua", jumlahSemua);
								map.put(prefix + "rata_rata", jumlahSemua > 0 ? totalSemua / jumlahSemua : 0);
								map.put("totalSemua", totalSemua);
								map.put("jumlahSemua", jumlahSemua);
								map.put("rata_rata", jumlahSemua > 0 ? totalSemua / jumlahSemua : 0);
							}
						}
					}

					if ((jenisRaporSiswa != null && jenisRaporSiswa.getAmbilAbsenPiket())) {
						int hadir = 0, sakit = 0, izin = 0, alpa = 0, belum = 0;
						Set<String> tgls = new HashSet<String>();
						StringBuilder tglsSakit = new StringBuilder();
						StringBuilder tglsIzin = new StringBuilder();

						for (AbsenPiket ap : absenPikets) {
							if (k.getKelasSiswa() != null && ap.getKelas() != null && ap.getTanggal() != null && k.getKelasSiswa().getId().equals(ap.getKelas().getId())) {
								String tglD = Common.dateFormat83.get().format(ap.getTanggal());
								if (!tgls.contains(tglD)) {
									AbsenPiketDetail apd = AbsenPiketDetail.ambil(null, siswa, ap, ap.getKelas().getAbsensi(), 0);
									Statusabsensi status = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(), apd.retreiveAbsensiId(siswa.getId() + "_" + ap.getId()));
									if (status == null) status = ConstantValues.BELUM_ABSEN;

									if (ConstantValues.MASUK != null && status.getId().equals(ConstantValues.MASUK.getId())) hadir++;
									if (ConstantValues.SAKIT != null && status.getId().equals(ConstantValues.SAKIT.getId())) { sakit++; if (tglsSakit.length() > 0) tglsSakit.append(", "); tglsSakit.append(tglD); }
									if (ConstantValues.IZIN != null && status.getId().equals(ConstantValues.IZIN.getId())) { izin++; if (tglsIzin.length() > 0) tglsIzin.append(", "); tglsIzin.append(tglD); }
									if (ConstantValues.TIDAK_ADA_ALASAN != null && status.getId().equals(ConstantValues.TIDAK_ADA_ALASAN.getId())) alpa++;
									if (ConstantValues.BELUM_ABSEN != null && status.getId().equals(ConstantValues.BELUM_ABSEN.getId())) belum++;
									tgls.add(tglD);
								}
							}
						}
						parameters.put("hadir_" + siswa.getId() + "_" + k.getKelasSiswa().getTahunAjaran(), hadir);
						parameters.put("sakit_" + siswa.getId() + "_" + k.getKelasSiswa().getTahunAjaran(), sakit);
						parameters.put("izin_" + siswa.getId() + "_" + k.getKelasSiswa().getTahunAjaran(), izin);
						parameters.put("alpa_" + siswa.getId() + "_" + k.getKelasSiswa().getTahunAjaran(), alpa);
						parameters.put("belum_" + siswa.getId() + "_" + k.getKelasSiswa().getTahunAjaran(), belum);
						parameters.put("sakit_tgl_" + siswa.getId(), tglsSakit.toString());
						parameters.put("izin_tgl_" + siswa.getId(), tglsIzin.toString());
						parameters.put("hadir_" + siswa.getId(), hadir);
						parameters.put("sakit_" + siswa.getId(), sakit);
						parameters.put("izin_" + siswa.getId(), izin);
						parameters.put("alpa_" + siswa.getId(), alpa);
						parameters.put("belum_" + siswa.getId(), belum);
					}
				}
			} else {
				List<AbsenPiket> absenPikets = (jenisRaporSiswa != null && jenisRaporSiswa.getAmbilAbsenPiket()) ? session.createCriteria(AbsenPiket.class)
						.add(sekolah == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("sekolah", sekolah))
						.add(Restrictions.eq("tahunAjaran", ta)).add(Restrictions.eq("semester", smtas))
						.addOrder(Order.asc("tanggal")).addOrder(Order.asc("id")).list() : new ArrayList<AbsenPiket>();

				List<KelompokMatapelajaran> kelompokMatapelajarans = ConstantValues.simpleList(
						session.createCriteria(KelompokMatapelajaran.class).addOrder(Order.asc("nomorUrut"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(sekolah == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("sekolah", sekolah))
								.add(Restrictions.isNull("induk")), KelompokMatapelajaran.class);

				Map<Long, KelasSiswa> mapKelasSiswa = new HashMap<Long, KelasSiswa>();
				List<Siswa> siswas = new ArrayList<Siswa>();
				for (KelasSiswaPunyaSiswa ksps : mapsData.values()) {
					if (ksps.getKelasSiswa() != null && !mapKelasSiswa.containsKey(ksps.getKelasSiswa().getId())) {
						mapKelasSiswa.put(ksps.getKelasSiswa().getId(), ksps.getKelasSiswa());
					}
					siswas.add(ksps.getSiswa());
				}

				List<CatatanKelasSiswa> catatanKelasSiswas = (jenisRaporSiswa != null && jenisRaporSiswa.getAmbilCatatanSiswa()) ? session.createCriteria(CatatanKelasSiswa.class)
						.add(mapKelasSiswa.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("kelasSiswa", mapKelasSiswa.values()))
						.addOrder(Order.asc("nama")).list() : new ArrayList<CatatanKelasSiswa>();

				List<CatatanSiswa> catatanSiswas = (jenisRaporSiswa != null && jenisRaporSiswa.getAmbilCatatanSiswa()) ? session.createCriteria(CatatanSiswa.class)
						.add(siswas.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("siswa", siswas))
						.addOrder(Order.asc("nama")).add(Restrictions.eq("tahunAjaran", ta)).add(Restrictions.eq("semester", smtas)).list() : new ArrayList<CatatanSiswa>();

				List<FormulirKegiatanPeserta> formulirKegiatanPesertas = (jenisRaporSiswa != null && jenisRaporSiswa.getAmbilKegiatanSiswa()) ? session.createCriteria(FormulirKegiatanPeserta.class)
						.add(siswas.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("siswa", siswas))
						.createAlias("formulirKegiatan", "formulirKegiatan").addOrder(Order.asc("formulirKegiatan.nama"))
						.add(Restrictions.eq("formulirKegiatan.tahunAkademik", ta)).add(Restrictions.eq("formulirKegiatan.semester", smta))
						.add(Restrictions.eq("acc", true)).list() : new ArrayList<FormulirKegiatanPeserta>();

				List<PrestasiSiswa> prestasiSiswas = (jenisRaporSiswa != null && jenisRaporSiswa.getAmbilKegiatanSiswa()) ? session.createCriteria(PrestasiSiswa.class)
						.add(siswas.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("siswa", siswas))
						.add(Restrictions.eq("status", PrestasiSiswa.DISETUJUI)).addOrder(Order.asc("tanggal"))
						.add(Restrictions.eq("tahunAkademik", ta)).add(Restrictions.eq("jenisSemester", smta)).list() : new ArrayList<PrestasiSiswa>();

				List<KegiatanSiswa> kegiatanSiswas = (jenisRaporSiswa != null && jenisRaporSiswa.getAmbilKegiatanSiswa()) ? session.createCriteria(KegiatanSiswa.class)
						.add(Restrictions.eq("ta", ta)).add(siswas.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("siswa", siswas))
						.addOrder(Order.asc("waktu")).addOrder(Order.asc("id")).list() : new ArrayList<KegiatanSiswa>();

				List<PelanggaranSiswa> pelanggaranSiswas = (jenisRaporSiswa != null && jenisRaporSiswa.getAmbilPelanggaranSiswa()) ? session.createCriteria(PelanggaranSiswa.class)
						.add(Restrictions.eq("ta", ta)).add(siswas.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("siswa", siswas))
						.addOrder(Order.asc("waktu")).addOrder(Order.asc("id")).list() : new ArrayList<PelanggaranSiswa>();

				List<ApresiasiSiswa> apresiasiSiswas = (jenisRaporSiswa != null && jenisRaporSiswa.getAmbilPelanggaranSiswa()) ? session.createCriteria(ApresiasiSiswa.class)
						.add(Restrictions.eq("ta", ta)).add(siswas.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("siswa", siswas))
						.addOrder(Order.asc("waktu")).addOrder(Order.asc("id")).list() : new ArrayList<ApresiasiSiswa>();

				for (KelasSiswa ks : mapKelasSiswa.values()) catatanKelasSiswa(session, parameters, ks, catatanKelasSiswas);

				for (KelasSiswaPunyaSiswa ksps : mapsData.values()) {
					Siswa siswa = ksps.getSiswa();
					if ((jenisRaporSiswa != null && jenisRaporSiswa.getAmbilPelanggaranSiswa()) || (jenisRaporSiswa != null && jenisRaporSiswa.getAmbilApresiasiSiswa())) {
						masukkanPoin(session, siswa, ta, parameters, kegiatanSiswas, pelanggaranSiswas, apresiasiSiswas);
					}

					if ((jenisRaporSiswa != null && jenisRaporSiswa.getAmbilKegiatanSiswa())) {
						List<Map<String, Object>> mapEkstra = new ArrayList<Map<String, Object>>();
						for (FormulirKegiatanPeserta fkp : formulirKegiatanPesertas) {
							if (fkp.getSiswa() != null && siswa.getId().equals(fkp.getSiswa().getId())) {
								Map<String, Object> map = new HashMap<String, Object>();
								map.put("id", fkp.getId());
								map.put("kode", fkp.getKode());
								map.put("nilai", fkp.getNilai());
								map.put("keterangan", fkp.getKeterangan());
								map.put("id_formulirKegiatan", fkp.getFormulirKegiatan() == null ? -1L : fkp.getFormulirKegiatan().getId());
								map.put("nama_formulirKegiatan", fkp.getFormulirKegiatan() == null ? "" : fkp.getFormulirKegiatan().getNama());
								mapEkstra.add(map);
							}
						}
						parameters.put("ekstra_" + siswa.getId(), mapEkstra);

						List<Map<String, Object>> mapPrestasi = new ArrayList<Map<String, Object>>();
						for (PrestasiSiswa ps : prestasiSiswas) {
							if (ps.getSiswa() != null && siswa.getId().equals(ps.getSiswa().getId())) {
								Map<String, Object> map = new HashMap<String, Object>();
								map.put("id", ps.getId());
								map.put("nama", ps.getNama());
								map.put("kode", ps.getKode());
								map.put("jenis", ps.getCabangPrestasiSiswa() == null ? "" : ps.getCabangPrestasiSiswa().getNama());
								map.put("capaian", ps.getCapaian());
								map.put("keterangan", ps.getKeterangan());
								mapPrestasi.add(map);
							}
						}
						parameters.put("prestasi_" + siswa.getId(), mapPrestasi);
					}

					if ((jenisRaporSiswa != null && jenisRaporSiswa.getAmbilAbsenPiket())) {
						int hadir = 0, sakit = 0, izin = 0, alpa = 0, belum = 0;
						Set<String> tgls = new HashSet<String>();
						StringBuilder tglsSakit = new StringBuilder();
						StringBuilder tglsIzin = new StringBuilder();

						for (AbsenPiket ap : absenPikets) {
							if (ksps.getKelasSiswa() != null && ap.getKelas() != null && ap.getTanggal() != null && ksps.getKelasSiswa().getId().equals(ap.getKelas().getId())) {
								String tglD = Common.dateFormat83.get().format(ap.getTanggal());
								if (!tgls.contains(tglD)) {
									AbsenPiketDetail apd = AbsenPiketDetail.ambil(null, siswa, ap, ap.getKelas().getAbsensi(), 0);
									Statusabsensi status = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(), apd.retreiveAbsensiId(siswa.getId() + "_" + ap.getId()));
									if (status == null) status = ConstantValues.BELUM_ABSEN;

									if (ConstantValues.MASUK != null && status.getId().equals(ConstantValues.MASUK.getId())) hadir++;
									if (ConstantValues.SAKIT != null && status.getId().equals(ConstantValues.SAKIT.getId())) { sakit++; if (tglsSakit.length() > 0) tglsSakit.append(", "); tglsSakit.append(tglD); }
									if (ConstantValues.IZIN != null && status.getId().equals(ConstantValues.IZIN.getId())) { izin++; if (tglsIzin.length() > 0) tglsIzin.append(", "); tglsIzin.append(tglD); }
									if (ConstantValues.TIDAK_ADA_ALASAN != null && status.getId().equals(ConstantValues.TIDAK_ADA_ALASAN.getId())) alpa++;
									if (ConstantValues.BELUM_ABSEN != null && status.getId().equals(ConstantValues.BELUM_ABSEN.getId())) belum++;
									tgls.add(tglD);
								}
							}
						}
						parameters.put("hadir_" + siswa.getId() + "_" + ksps.getKelasSiswa().getTahunAjaran(), hadir);
						parameters.put("sakit_" + siswa.getId() + "_" + ksps.getKelasSiswa().getTahunAjaran(), sakit);
						parameters.put("izin_" + siswa.getId() + "_" + ksps.getKelasSiswa().getTahunAjaran(), izin);
						parameters.put("alpa_" + siswa.getId() + "_" + ksps.getKelasSiswa().getTahunAjaran(), alpa);
						parameters.put("belum_" + siswa.getId() + "_" + ksps.getKelasSiswa().getTahunAjaran(), belum);
						parameters.put("sakit_tgl_" + siswa.getId(), tglsSakit.toString());
						parameters.put("izin_tgl_" + siswa.getId(), tglsIzin.toString());
						parameters.put("hadir_" + siswa.getId(), hadir);
						parameters.put("sakit_" + siswa.getId(), sakit);
						parameters.put("izin_" + siswa.getId(), izin);
						parameters.put("alpa_" + siswa.getId(), alpa);
						parameters.put("belum_" + siswa.getId(), belum);
					}

					if ((jenisRaporSiswa != null && jenisRaporSiswa.getAmbilCatatanSiswa())) catatanSiswa(session, parameters, siswa, ta, smtas, catatanSiswas);

					for (KelompokMatapelajaran kmp : kelompokMatapelajarans) {
						genarateKelompokInternal(session, list, ksps, kmp, jenisRaporSiswa, true, smtas, sekolah);
						genarateKelompokInternal(session, list, ksps, kmp, jenisRaporSiswa, false, smtas, sekolah);
					}
				}
			}
		} finally {
			tutupSession(session);
			// Bersihkan cache per-RUN (L2) agar tidak tertinggal di thread pool / tidak basi.
			akhiriRunCacheRapor();
		}

		parameters.put("tanggal", tanggal);
		parameters.put("maps", list);
		parameters.put("ta", ta);
		parameters.put("smtas", smtas);
		parameters.put("smta", smta);

		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void catatanSiswa(Session session, Map parameters, Siswa siswa, String ta, Integer smtas, List<CatatanSiswa> catatanSiswas) {
		List<Map<String, Object>> mapsCatatan = new ArrayList<Map<String, Object>>();
		parameters.put("mapsCatatan_" + siswa.getId(), mapsCatatan);

		for (CatatanSiswa cs : catatanSiswas) {
			if (siswa.getId().equals(cs.getSiswa().getId())) {
				Map<String, Object> catat = new HashMap<String, Object>();
				catat.put("catatanSiswas_id", cs.getId());
				catat.put("catatanSiswas_tgl", cs.getWaktu());
				catat.put("catatanSiswas_keterangan", cs.getKeterangan());
				catat.put("catatanSiswas_jenis", cs.getJenisCatatanSiswa() == null ? "" : cs.getJenisCatatanSiswa().getNama());
				catat.put("catatanSiswas_guru", cs.getGuru() == null ? "" : cs.getGuru().getNama());
				mapsCatatan.add(catat);

				String jid = cs.getJenisCatatanSiswa() == null ? "" : cs.getJenisCatatanSiswa().getId().toString();
				parameters.put("catatanSiswas_id_" + siswa.getId() + "_" + jid, cs.getId());
				parameters.put("catatanSiswas_tgl_" + siswa.getId() + "_" + jid, cs.getWaktu());
				parameters.put("catatanSiswas_keterangan_" + siswa.getId() + "_" + jid, cs.getKeterangan());
				parameters.put("catatanSiswas_jenis_" + siswa.getId() + "_" + jid, cs.getJenisCatatanSiswa() == null ? "" : cs.getJenisCatatanSiswa().getNama());
				parameters.put("catatanSiswas_guru_" + siswa.getId() + "_" + jid, cs.getGuru() == null ? "" : cs.getGuru().getNama());

				parameters.put("catatanSiswas_id_" + siswa.getId(), cs.getId());
				parameters.put("catatanSiswas_tgl_" + siswa.getId(), cs.getWaktu());
				parameters.put("catatanSiswas_keterangan_" + siswa.getId(), cs.getKeterangan());
				parameters.put("catatanSiswas_jenis_" + siswa.getId(), cs.getJenisCatatanSiswa() == null ? "" : cs.getJenisCatatanSiswa().getNama());
				parameters.put("catatanSiswas_guru_" + siswa.getId(), cs.getGuru() == null ? "" : cs.getGuru().getNama());

				JenisCatatanSiswa j = cs.getJenisCatatanSiswa();
				if (j != null) {
					session.refresh(j);
					List<KelompokParameterTambahanCatatanSiswa> siswasCatatanSiswas = new ArrayList<KelompokParameterTambahanCatatanSiswa>(j.getKelompokParameterTambahanCatatanSiswas());

					for (KelompokParameterTambahanCatatanSiswa kptcs : siswasCatatanSiswas) {
						List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
								session.createCriteria(ParameterTambahanCatatanSiswa.class)
										.add(Restrictions.eq("kelompokParameterTambahanCatatanSiswa", kptcs))
										.createAlias("parameterTambahan", "parameterTambahan")
										.createAlias("kelompokParameterTambahanCatatanSiswa", "kelompokParameterTambahanCatatanSiswa")
										.add(Restrictions.eq("parameterTambahan.aktif", true))
										.add(Restrictions.eq("kelompokParameterTambahanCatatanSiswa.aktif", true))
										.setProjection(Projections.groupProperty("parameterTambahan.id")),
								ParameterTambahan.class, false);
						Collections.sort(parameterTambahans);

						for (ParameterTambahan pt : parameterTambahans) {
							String jenis = kptcs.getId() + "->" + pt.getId();
							String val = "";
							String[] spl = cs.getParameterTambahanInds().split("\n");
							for (String d : spl) {
								String[] value = d.split("<=>");
								if (value[0].trim().equalsIgnoreCase(jenis)) val = value.length > 1 ? value[1].trim() : "";
							}

							parameters.put("catatanSiswas_" + pt.getId() + "_" + siswa.getId() + "_" + jid, val);
							parameters.put("catatanSiswas_label_" + siswa.getId(), pt.getLabelInputan());
							catat.put("catatanSiswas_" + pt.getId(), val);
							catat.put("catatanSiswas_label", pt.getLabelInputan());
						}
					}
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void catatanKelasSiswa(Session session, Map parameters, KelasSiswa kelasSiswa, List<CatatanKelasSiswa> catatanKelasSiswas) {
		List<Map<String, Object>> mapsCatatan = new ArrayList<Map<String, Object>>();
		parameters.put("mapsCatatanKelasSiswa_" + kelasSiswa.getId(), mapsCatatan);

		for (CatatanKelasSiswa cks : catatanKelasSiswas) {
			if (kelasSiswa != null && cks.getKelasSiswa() != null && kelasSiswa.getId().equals(cks.getKelasSiswa().getId())) {
				Map<String, Object> catat = new HashMap<String, Object>();
				catat.put("catatanKelasSiswas_id", cks.getId());
				catat.put("catatanKelasSiswas_tgl", cks.getWaktu());
				catat.put("catatanKelasSiswas_keterangan", cks.getKeterangan());
				catat.put("catatanKelasSiswas_jenis", cks.getJenisCatatanKelasSiswa() == null ? "" : cks.getJenisCatatanKelasSiswa().getNama());
				mapsCatatan.add(catat);

				String jid = cks.getJenisCatatanKelasSiswa() == null ? "" : cks.getJenisCatatanKelasSiswa().getId().toString();
				parameters.put("catatanKelasSiswas_id_" + kelasSiswa.getId() + "_" + jid, cks.getId());
				parameters.put("catatanKelasSiswas_tgl_" + kelasSiswa.getId() + "_" + jid, cks.getWaktu());
				parameters.put("catatanKelasSiswas_keterangan_" + kelasSiswa.getId() + "_" + jid, cks.getKeterangan());
				parameters.put("catatanKelasSiswas_jenis_" + kelasSiswa.getId() + "_" + jid, cks.getJenisCatatanKelasSiswa() == null ? "" : cks.getJenisCatatanKelasSiswa().getNama());

				parameters.put("catatanKelasSiswas_id_" + kelasSiswa.getId(), cks.getId());
				parameters.put("catatanKelasSiswas_tgl_" + kelasSiswa.getId(), cks.getWaktu());
				parameters.put("catatanKelasSiswas_keterangan_" + kelasSiswa.getId(), cks.getKeterangan());
				parameters.put("catatanKelasSiswas_jenis_" + kelasSiswa.getId(), cks.getJenisCatatanKelasSiswa() == null ? "" : cks.getJenisCatatanKelasSiswa().getNama());

				JenisCatatanKelasSiswa j = cks.getJenisCatatanKelasSiswa();
				if (j != null) {
					session.refresh(j);
					List<KelompokParameterTambahanCatatanKelasSiswa> kptcksList = new ArrayList<KelompokParameterTambahanCatatanKelasSiswa>(j.getKelompokParameterTambahanCatatanKelasSiswas());

					for (KelompokParameterTambahanCatatanKelasSiswa kptcks : kptcksList) {
						List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
								session.createCriteria(ParameterTambahanCatatanKelasSiswa.class)
										.add(Restrictions.eq("kelompokParameterTambahanCatatanKelasSiswa", kptcks))
										.createAlias("parameterTambahan", "parameterTambahan")
										.createAlias("kelompokParameterTambahanCatatanKelasSiswa", "kelompokParameterTambahanCatatanKelasSiswa")
										.add(Restrictions.eq("parameterTambahan.aktif", true))
										.add(Restrictions.eq("kelompokParameterTambahanCatatanKelasSiswa.aktif", true))
										.setProjection(Projections.groupProperty("parameterTambahan.id")),
								ParameterTambahan.class, false);
						Collections.sort(parameterTambahans);

						for (ParameterTambahan pt : parameterTambahans) {
							String jenis = kptcks.getId() + "->" + pt.getId();
							String val = "";
							String[] spl = cks.getParameterTambahanInds().split("\n");
							for (String d : spl) {
								String[] value = d.split("<=>");
								if (value[0].trim().equalsIgnoreCase(jenis)) val = value.length > 1 ? value[1].trim() : "";
							}

							parameters.put("catatanKelasSiswas_" + pt.getId() + "_" + kelasSiswa.getId() + "_" + jid, val);
							parameters.put("catatanKelasSiswas_label_" + kelasSiswa.getId(), pt.getLabelInputan());
							catat.put("catatanKelasSiswas_" + pt.getId(), val);
							catat.put("catatanKelasSiswas_label", pt.getLabelInputan());
						}
					}
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map siapkanParemeter(Map parameters, KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa, Integer smt, String prefix) throws Exception {
		Siswa siswa = kelasSiswaPunyaSiswa.getSiswa();
		String kodeBarcode = "";
		try {
			File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/barcode_" + siswa.getNomorInduk() + ".png");
			Barcode mybarcode = BarcodeFactory.createCode128B(siswa.getNomorInduk());
			BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
			kodeBarcode = myfilebarcode.getAbsolutePath();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:1732");}

		try {
			Guru guruBk = kelasSiswaPunyaSiswa.getKelasSiswa().getGuruBk();
			if (guruBk != null) Common.insertProperty(Guru.class, guruBk, parameters, prefix + "bk");
		} catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:1737"); }

		try {
			Guru guruWali = kelasSiswaPunyaSiswa.getKelasSiswa().getGuruPembina();
			if (guruWali != null) Common.insertProperty(Guru.class, guruWali, parameters, prefix + "wali");
		} catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:1742"); }

		Integer semester_tingkat = ((kelasSiswaPunyaSiswa.getKelasSiswa().getTingkat() - 1) * 2) + smt;

		Common.insertProperty(Siswa.class, siswa, parameters, prefix + "siswa");
		parameters.put(prefix + "smt", smt);
		parameters.put(prefix + "semester_tingkat", semester_tingkat);
		parameters.put(prefix + "tingkat", kelasSiswaPunyaSiswa.getKelasSiswa().getTingkat());
		parameters.put(prefix + "sekolah.id", siswa.getSekolah().getId());
		parameters.put(prefix + "sekolah.nama", siswa.getSekolah().getNama());
		parameters.put(prefix + "yayasan.id", siswa.getSekolah().getNama());
		parameters.put(prefix + "yayasan.nama", siswa.getYayasan().getNama());

		parameters.put(prefix + "id", siswa.getId());
		parameters.put(prefix + "siswa", siswa.getId());
		parameters.put(prefix + "url_foto_siswa", CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa)));
		parameters.put(prefix + "kode_barcode", kodeBarcode);
		parameters.put(prefix + "penjurusan", siswa.getPenjurusanSekolah() == null ? "" : siswa.getPenjurusanSekolah().getNama());
		parameters.put(prefix + "warna", siswa.getSekolah().getYayasan().getWarna());

		parameters.put(prefix + "kelas_nama", kelasSiswaPunyaSiswa == null ? "" : kelasSiswaPunyaSiswa.getKelasSiswa().getNama());
		parameters.put(prefix + "kelas", kelasSiswaPunyaSiswa == null ? "" : kelasSiswaPunyaSiswa.getKelasSiswa().getId());
		parameters.put(prefix + "nipp", siswa.getNomorIndukSantri());
		parameters.put(prefix + "nisn", siswa.getNomorIndukNasional());
		parameters.put(prefix + "nis", siswa.getNomorInduk());
		parameters.put(prefix + "kode", siswa.getNomorInduk());
		parameters.put(prefix + "email", siswa.getAlamatEmail());
		parameters.put(prefix + "nama", siswa.getNama());
		parameters.put(prefix + "nama_ortu", siswa.getNamaAyah() == null || siswa.getNamaAyah().isEmpty() ? siswa.getNamaIbu() : siswa.getNamaAyah());
		parameters.put(prefix + "angkatan", siswa.getTahunMasuk());
		parameters.put(prefix + "alamat", siswa.getAlamatSiswa());
		parameters.put(prefix + "tempatLahir", siswa.getTempatLahir());
		parameters.put(prefix + "tanggalLahir", siswa.getTanggalLahir());
		parameters.put(prefix + "nama_kepala_sekolah", siswa.getSekolah().getNamaKepalaSekolah());
		parameters.put(prefix + "nip_kepala_sekolah", siswa.getSekolah().getNipKepalaSekolah());
		parameters.put(prefix + "nama_wali_kelas", kelasSiswaPunyaSiswa.getKelasSiswa() == null || kelasSiswaPunyaSiswa.getKelasSiswa().getGuruPembina() == null ? "" : kelasSiswaPunyaSiswa.getKelasSiswa().getGuruPembina().getNama());
		parameters.put(prefix + "nip_wali_kelas", kelasSiswaPunyaSiswa.getKelasSiswa() == null || kelasSiswaPunyaSiswa.getKelasSiswa().getGuruPembina() == null ? "" : kelasSiswaPunyaSiswa.getKelasSiswa().getGuruPembina().getNip());
		parameters.put(prefix + "kode_wali_kelas", kelasSiswaPunyaSiswa.getKelasSiswa() == null || kelasSiswaPunyaSiswa.getKelasSiswa().getGuruPembina() == null ? "" : kelasSiswaPunyaSiswa.getKelasSiswa().getGuruPembina().getKode());

		parameters.put(prefix + "sekolah", siswa.getSekolah().getNama());
		parameters.put(prefix + "jenjang", siswa.getSekolah().getJenisSekolah().getNama());
		parameters.put(prefix + "nama_yayasan", siswa.getSekolah().getYayasan().getNama());
		parameters.put(prefix + "nama_perguruan_tinggi", siswa.getSekolah().getYayasan().getNama());
		parameters.put(prefix + "alamat_yayasan", siswa.getSekolah().getYayasan().getAlamat());
		parameters.put(prefix + "ttl", siswa.getTempatLahir().toUpperCase() + " / " + (siswa.getTanggalLahir() == null ? "" : Common.dateFormat2.get().format(siswa.getTanggalLahir())));
		parameters.put(prefix + "telp", siswa.getTeleponSiswa());

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(siswa.getTanggalMasuk());
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 5);
		parameters.put(prefix + "tanggal_kadaluarsa", calendar.getTime());

		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(siswa.getTanggalMasuk());
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
		parameters.put(prefix + "tanggal_kadaluarsa_1", calendar.getTime());

		String myfile = CommonMedia.loadPathFileFotoLangsung(new Tbmuser(siswa));
		parameters.put(prefix + "foto", myfile);
		parameters.put(prefix + "foto_lulus", myfile);

		try {
			Map<String, Double[]> maps = NilaiHurufSekolah.getNilaiHurufSekolah(siswa.getTahunMasuk(), siswa.getSekolah(), siswa.getYayasan(), kelasSiswaPunyaSiswa.getKelasSiswa().getTahunAjaran(), null);
			StringBuilder nilaiHuruf = new StringBuilder();
			for (String key : maps.keySet()) {
				Double[] nilais = maps.get(key);
				String d = key + ":" + nilais[0] + ":" + nilais[1];
				if (nilaiHuruf.length() > 0) nilaiHuruf.append(";");
				nilaiHuruf.append(d);
				parameters.put(prefix + "nilai_huruf_" + key, nilais[0] + ";" + nilais[1]);
			}
			parameters.put(prefix + "nilaiHuruf", nilaiHuruf.toString());
		} catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sekolah/LaporanRaporSiswa.java:1814"); }

		return parameters;
	}

	public void onReport(Event event) {
		Common.createDefaultTimer(new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					Report.setPreviewPdfDefaultSekali(); // KHUSUS Rapor Siswa: pratinjau default PDF (bukan HTML)
						JenisRaporSiswa j = (JenisRaporSiswa) (jenisRaporSiswa.getSelectedItem() == null ? null : jenisRaporSiswa.getSelectedItem().getValue());
					String ta = (String) (tahunAkademik.getSelectedItem() == null ? null : tahunAkademik.getSelectedItem().getValue());
					String smta = searchsmt.getValue();
					Integer smtas = (Integer) searchsmt.getSelectedItem().getValue();
					Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
					Date tgl = tanggal.getValue();

					if (j != null) {
						LampiranLain lainMahasiswa = LampiranLain.ambil(j.getId(), LampiranLain.FILE_JRXML_LAYOUT_JENIS_RAPOR);
						if (lainMahasiswa == null) {
							MyMessageboxConfig.show("File laporan rapor siswa belum diupload", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						File file = Report.generateCompileFileReport(Report.PDF,
								LaporanRaporSiswa.generateParameter(map, j, tbmuser, ta, smtas, smta, s, tgl),
								lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
						CommonReport.tampilkanReportPDF(center, file);
					} else {
						String namaFile = "sekolah/report";
						File file = Report.generateFileReportWithProgress(Report.PDF,
								LaporanRaporSiswa.generateParameter(map, j, tbmuser, ta, smtas, smta, s, tgl), namaFile,
								ais.ui.util.WaktuUtil.getDate(), null, toolbar);
						CommonReport.tampilkanReportPDF(center, file);
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rapor Siswa", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
							new String[] {
								"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
								"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});
	}
}