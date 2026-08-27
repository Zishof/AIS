package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.dashboard.helper.DashboardRekapMahasiswa;
import ais.action.master.helper.HistoryStatusMahasiswaUtil;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapJumlahMahasiswa extends MyWindow {

	private Center center;

	/**
	 *
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox program;

	private Toolbar toolbar;
	private Combobox tahunAjaran;
	private Combobox ganjilGenap;
	@SuppressWarnings("rawtypes")
	private ArrayList<Map> maps;
	private Html html;

	private Combobox statusMahasiswaAwal;

	private ArrayList<Mahasiswa> mahasiswasTanpaJenisKelamin = null;

	private Decimalbox tahunAngkatan;
	private Decimalbox tahunAngkatanSd;

	public LaporanRekapJumlahMahasiswa() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Jumlah Mahasiswa", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapJumlahMahasiswa(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

		final Tabbox tabbox = new Tabbox();
		// Tinggi PASTI 2000px DIREKAT ke window (bukan height:100% via tampilanScrollTabbox). Saat
		// window ini DIPASANG sebagai sub-tab di dalam tabpanel window pembungkus
		// (LaporanRekapJumlahMhsFakWindow), rantai height:100% (tampilanScrollTabbox → Borderlayout
		// 100% → Center flex → Tabbox 100%) tidak punya leluhur ber-tinggi PASTI sehingga kolaps ke
		// 0px dan konten tab TIDAK tampil. Pola tinggi-pasti ini sama dengan sibling
		// LaporanRekapJumlahMahasiswaAngkatan (Borderlayout setHeight("2000px") direkat ke window)
		// yang kontennya SUDAH tampil. Scroll ditangani oleh window pembungkus (Common.tampilanScroll)
		// + MyTabpanel (min-height) di lapis luar.
		tabbox.setParent(this);
		tabbox.setHeight("2000px");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Rekap");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Rekap Angkatan");
		tab2.setParent(tabs);

		MyTabConfig tab211 = new MyTabConfig("Rekap Program");
		tab211.setParent(tabs);

		MyTabConfig tab10 = new MyTabConfig("Rasio Mhs/Dosen");
		tab10.setParent(tabs);

		MyTabConfig tab20 = new MyTabConfig("Rekap per Pilihan");
		tab20.setParent(tabs);

//		MyTabConfig tab21 = new MyTabConfig("Rekap per semester dan " + Common.getBahasaConfig("Jurusan"));
//		tab21.setParent(tabs);
//
//		MyTabConfig tab22 = new MyTabConfig("Rekap per semester dan " + Common.getBahasaConfig("Fakultas"));
//		tab22.setParent(tabs);

		// MyTabConfig tab23 = new MyTabConfig("Rekap per jenis pendidikan
		// sebelumnya");
		// tab23.setParent(tabs);
		//
		// MyTabConfig tab24 = new MyTabConfig("Rekap per tempat tinggal");
		// tab24.setParent(tabs);
		//
		// MyTabConfig tab25 = new MyTabConfig("Rekap per alat transportasi");
		// tab25.setParent(tabs);

		MyTabConfig tab3 = new MyTabConfig("Data Mahasiswa");
		tab3.setParent(tabs);

		MyTabConfig tab311 = new MyTabConfig("Status Mahasiswa");
		tab311.setParent(tabs);

		MyTabConfig tab31 = new MyTabConfig("Per Matakuliah");
		tab31.setParent(tabs);

		MyTabConfig tab4 = new MyTabConfig("Data Alumni");
		tab4.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().size() == 0) {
					LaporanRekapJumlahMahasiswaAngkatan laporan = new LaporanRekapJumlahMahasiswaAngkatan();
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(tabpanel2);
				}
			}
		});

		final Tabpanel tabpanel211 = new ais.ui.util.MyTabpanel();
		tabpanel211.setParent(tabpanels);
		tab211.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel211.getChildren().size() == 0) {
					LaporanRekapJumlahMahasiswaProgram laporan = new LaporanRekapJumlahMahasiswaProgram();
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(tabpanel211);
				}
			}
		});

		final Tabpanel tabpanel10 = new ais.ui.util.MyTabpanel();
		tabpanel10.setParent(tabpanels);
		tab10.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel10.getChildren().size() == 0) {
					LaporanRasioJumlahMahasiswaDanDosen laporan = new LaporanRasioJumlahMahasiswaDanDosen();
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(tabpanel10);
				}
			}
		});

		final Tabpanel tabpanel20 = new ais.ui.util.MyTabpanel();
		tabpanel20.setParent(tabpanels);
		tab20.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel20.getChildren().size() == 0) {
					DashboardRekapMahasiswa laporan = new DashboardRekapMahasiswa();
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(tabpanel20);
				}
			}
		});

//		final Tabpanel tabpanel21 = new ais.ui.util.MyTabpanel();
//		tabpanel21.setParent(tabpanels);
//		tab21.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel21.getChildren().size() == 0) {
//					LaporanRekapJumlahMahasiswaProdi laporan = new LaporanRekapJumlahMahasiswaProdi();
//					laporan.setHeight("100%");
//					laporan.setWidth("100%");
//					laporan.setParent(tabpanel21);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel22 = new ais.ui.util.MyTabpanel();
//		tabpanel22.setParent(tabpanels);
//		tab22.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel22.getChildren().size() == 0) {
//					LaporanRekapJumlahMahasiswaFakultas laporan = new LaporanRekapJumlahMahasiswaFakultas();
//					laporan.setHeight("100%");
//					laporan.setWidth("100%");
//					laporan.setParent(tabpanel22);
//				}
//			}
//		});

		// final Tabpanel tabpanel23 = new ais.ui.util.MyTabpanel();
		// tabpanel23.setParent(tabpanels);
		// tab23.addEventListener("onClick", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// if (tabpanel23.getChildren().size() == 0) {
		// LaporanRekapJumlahMahasiswaPerJenisSekolah laporan = new
		// LaporanRekapJumlahMahasiswaPerJenisSekolah();
		// laporan.setHeight("100%");
		// laporan.setWidth("100%");
		// laporan.setParent(tabpanel23);
		// }
		// }
		// });
		//
		// final Tabpanel tabpanel24 = new ais.ui.util.MyTabpanel();
		// tabpanel24.setParent(tabpanels);
		// tab24.addEventListener("onClick", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// if (tabpanel24.getChildren().size() == 0) {
		// LaporanRekapJumlahMahasiswaPerTempatTinggalSaatKuliah laporan = new
		// LaporanRekapJumlahMahasiswaPerTempatTinggalSaatKuliah();
		// laporan.setHeight("100%");
		// laporan.setWidth("100%");
		// laporan.setParent(tabpanel24);
		// }
		// }
		// });
		//
		// final Tabpanel tabpanel25 = new ais.ui.util.MyTabpanel();
		// tabpanel25.setParent(tabpanels);
		// tab25.addEventListener("onClick", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// if (tabpanel25.getChildren().size() == 0) {
		// LaporanRekapJumlahMahasiswaPerTransportasi laporan = new
		// LaporanRekapJumlahMahasiswaPerTransportasi();
		// laporan.setHeight("100%");
		// laporan.setWidth("100%");
		// laporan.setParent(tabpanel25);
		// }
		// }
		// });

		final Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
		tabpanel3.setParent(tabpanels);
		tab3.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel3.getChildren().size() == 0) {
					LaporanRekapitulasiMahasiswa laporan = new LaporanRekapitulasiMahasiswa();
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(tabpanel3);
				}
			}
		});

		final Tabpanel tabpanel311 = new ais.ui.util.MyTabpanel();
		tabpanel311.setParent(tabpanels);
		tab311.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel311.getChildren().size() == 0) {
					LaporanRekapitulasiStatusMahasiswa laporan = new LaporanRekapitulasiStatusMahasiswa();
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(tabpanel311);
				}
			}
		});

		final Tabpanel tabpanel31 = new ais.ui.util.MyTabpanel();
		tabpanel31.setParent(tabpanels);
		tab31.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel31.getChildren().size() == 0) {
					LaporanRekapitulasiMahasiswaYangMengambilMatakuliah laporan = new LaporanRekapitulasiMahasiswaYangMengambilMatakuliah();
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(tabpanel31);
				}
			}
		});

		final Tabpanel tabpanel4 = new ais.ui.util.MyTabpanel();
		tabpanel4.setParent(tabpanels);
		tab4.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel4.getChildren().size() == 0) {
					LaporanRekapitulasiAlumniJurusan laporan = new LaporanRekapitulasiAlumniJurusan();
					laporan.setHeight("100%");
					laporan.setWidth("100%");
					laporan.setParent(tabpanel4);
				}
			}
		});

		// FIX konten sub-tab kosong (ZK 5.5): klik tab memicu event ON_SELECT pada Tabbox, bukan
		// onClick tiap Tab. Re-dispatch onClick ke tab yang SEDANG dipilih agar konten lazy-nya
		// termuat. Tab pertama ("Rekap") kontennya dibangun eager di bawah (tanpa onClick) → aman.
		tabbox.addEventListener(org.zkoss.zk.ui.event.Events.ON_SELECT, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				org.zkoss.zul.Tab dipilih = tabbox.getSelectedTab();
				if (dipilih != null) {
					org.zkoss.zk.ui.event.Events.sendEvent(new Event("onClick", dipilih));
				}
			}
		});

		HistoryStatusMahasiswaUtil.initDataStatusMahasiswa();

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		program = Common.initPrograms(null);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		// Tinggi PASTI 2000px agar Borderlayout (West "Menu" + Center laporan) tidak kolaps ke 0px
		// saat berada di dalam tabpanel ber-min-height (rantai height:100% tidak menghasilkan tinggi
		// pasti) — sehingga konten tab benar-benar tampil.
		borderlayout.setHeight("2000px");
		borderlayout.setParent(tabpanel1);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal"));
		row.appendChild(statusMahasiswaAwal = new Combobox());
		statusMahasiswaAwal.setWidth("90%");
		Common.insertComboDanSemua(statusMahasiswaAwal, "nama", StatusAwalMahasiswa.class,
				Restrictions.eq("aktif", true));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(tahunAngkatan = new Decimalbox());
		tahunAngkatan.setCols(4);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		hbox.appendChild(tahunAngkatanSd = new Decimalbox());
		tahunAngkatanSd.setCols(4);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");
		tahunAjaran.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		ganjilGenap = new Combobox();
		row.appendChild(ganjilGenap);
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		ganjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		ganjilGenap.appendChild(comboitem);
		Common.selectComboItem(ganjilGenap, Common.getSemesterString());
		ganjilGenap.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		// Semua tombol diletakkan dalam SATU sel (Hbox) yang span 2 kolom -> bar tombol rapi &
		// sejajar (sebelumnya 4 sel pada grid 2 kolom -> meluap & posisi tak rata).
		org.zkoss.zul.Hbox barisTombol = new org.zkoss.zul.Hbox();
		barisTombol.setSpacing("6px");
		row.appendChild(barisTombol);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		MyButtonConfig tombol;
		barisTombol.appendChild(tombol = new MyButtonConfig("Lihat Laporan"));

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onCetak(event);

			}
		};
		tombol.addEventListener("onClick", eventListener);

			// ── Ringkasan (cepat, dari cache memori) + Hitung Ulang (recompute dari DB) ──
			final MyButtonConfig tombolRingkasan = new MyButtonConfig("Ringkasan");
			tombolRingkasan.setTooltiptext("Tampilan ringkas & cepat (dari memori): kartu + grafik.");
			barisTombol.appendChild(tombolRingkasan);
			final MyButtonConfig tombolHitungUlang = new MyButtonConfig("Hitung Ulang");
			tombolHitungUlang.setTooltiptext("Hitung ulang dari database bila data berubah.");
			barisTombol.appendChild(tombolHitungUlang);

			final EventListener renderRingkasan = new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					final boolean recompute = ev != null && Boolean.TRUE.equals(ev.getData());
					final String ta = tahunAjaran.getSelectedItem() == null ? Common.getCurrentTahunAkademik()
							: (String) tahunAjaran.getSelectedItem().getValue();
					final String sem = ganjilGenap.getSelectedItem() == null ? Common.getSemesterString()
							: (String) ganjilGenap.getSelectedItem().getValue();
					Fakultas mf = (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null)
							? null : (Fakultas) fakultas.getSelectedItem().getValue();
					Jurusan mj = (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null)
							? null : (Jurusan) jurusan.getSelectedItem().getValue();
					final Long fakId = mf == null ? null : mf.getId();
					final Long jurId = mj == null ? null : mj.getId();
					// Build di LATAR + progress bar (fase + persen); tidak memblokir UI.
					ais.action.master.dashboard.admin.RekapMahasiswaViewHelper.renderDenganProgress(center, ta, sem,
							fakId, jurId, recompute);
				}
			};
			tombolRingkasan.addEventListener("onClick", renderRingkasan);
			tombolHitungUlang.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					renderRingkasan.onEvent(new Event("onClick", null, Boolean.TRUE));
				}
			});

		row = new MyFormRow();
		row.setParent(rows);
		html = new ais.ui.util.MyHtml();
		row.appendChild(html);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		North north = new North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "rekap_jumlah_mahasiswa_semua", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);

			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Map parameters = ais.common.HashMapGenerator.getRand();
		Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
				? null
				: jurusan.getSelectedItem().getValue());
		Fakultas myFakultas = (Fakultas) (fakultas.getSelectedItem() == null
				|| fakultas.getSelectedItem().getValue() == null ? null : fakultas.getSelectedItem().getValue());

		parameters.put("fakultas", myFakultas == null || myFakultas.getId() == null ? -1L : myFakultas.getId());
		parameters.put("jurusan", myJurusan == null || myJurusan.getId() == null ? -1L : myJurusan.getId());

		parameters.put("program",
				program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? "-1"
						: program.getSelectedItem().getValue());
		parameters.put("tahunAkademik", tahunAjaran.getSelectedItem() == null ? Common.getCurrentTahunAkademik()
				: tahunAjaran.getSelectedItem().getValue());
		parameters.put("semester", ganjilGenap.getSelectedItem() == null ? Common.getSemesterString()
				: ganjilGenap.getSelectedItem().getValue());

		parameters.put("statusMahasiswaAwal",
				statusMahasiswaAwal.getSelectedItem() == null
						|| statusMahasiswaAwal.getSelectedItem().getValue() == null ? -1
								: ((StatusAwalMahasiswa) statusMahasiswaAwal.getSelectedItem().getValue()).getId());

		parameters.put("tahunangkatan", tahunAngkatan.getValue() == null ? -1 : tahunAngkatan.getValue().intValue());
		parameters.put("tahunangkatan_sd",
				tahunAngkatanSd.getValue() == null ? -1 : tahunAngkatanSd.getValue().intValue());

		if (maps != null) {
			parameters.put("maps", maps);
		}

		return parameters;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void generateDataDanImageAlbum(Label label) {

		List<String> jenisKelaminsValid = new ArrayList<String>();
		jenisKelaminsValid.add("Laki-laki");
		jenisKelaminsValid.add("Perempuan");
		mahasiswasTanpaJenisKelamin = new ArrayList<Mahasiswa>();
		List<Long> dataMhs = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
				.setProjection(Projections.property("id"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		TreeMap<String, Map<String, Object>> dataHasil = new TreeMap<String, Map<String, Object>>();

		String tahunAkademik = (String) (tahunAjaran.getSelectedItem() == null ? Common.getCurrentTahunAkademik()
				: tahunAjaran.getSelectedItem().getValue());

		String semestera = (String) (ganjilGenap.getSelectedItem() == null ? Common.getSemesterString()
				: ganjilGenap.getSelectedItem().getValue());

		String programA = (String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
				? null
				: program.getSelectedItem().getValue());

		Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
				? null
				: jurusan.getSelectedItem().getValue());
		Fakultas myFakultas = (Fakultas) (fakultas.getSelectedItem() == null
				|| fakultas.getSelectedItem().getValue() == null ? null : fakultas.getSelectedItem().getValue());

		StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) (statusMahasiswaAwal.getSelectedItem() == null
				|| statusMahasiswaAwal.getSelectedItem().getValue() == null ? null
						: statusMahasiswaAwal.getSelectedItem().getValue());

		Integer tahunangkatan = tahunAngkatan.getValue() == null ? null : tahunAngkatan.getValue().intValue();
		Integer tahunangkatanSd = tahunAngkatanSd.getValue() == null ? null : tahunAngkatanSd.getValue().intValue();

		int size = dataMhs.size();

		maps = new ArrayList<Map>();
		int index = 0;

		for (Long generalValueObjectid : dataMhs) {
			Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), generalValueObjectid);
			try {

				label.setValue("Memproses data " + mahasiswa + " (" + Common.numberFormat.get().format((index * 100.0) / size)
						+ "%)");
				index++;

				if (myFakultas == null || myFakultas.getId().equals(mahasiswa.getJurusan().getFakultas().getId())) {

					if (myJurusan == null || myJurusan.getId().equals(mahasiswa.getJurusan().getId())) {

						if (programA == null || programA.equals(mahasiswa.getProgram())) {

							if (statusAwalMahasiswa == null
									|| statusAwalMahasiswa.getId().equals(mahasiswa.getStatusAwalMahasiswa().getId())) {

								if (tahunangkatan == null || tahunangkatan <= mahasiswa.getTahunangkatan()) {

									if (tahunangkatanSd == null || tahunangkatanSd >= mahasiswa.getTahunangkatan()) {

										if (mahasiswa.getKelamin() == null
												|| !jenisKelaminsValid.contains(mahasiswa.getKelamin())) {
											mahasiswasTanpaJenisKelamin.add(mahasiswa);
										} else if (mahasiswa.getJurusan() != null) {
											String key = mahasiswa.getJurusan().getFakultas().getId() + "_"
													+ mahasiswa.getJurusan().getId();
											Map<String, Object> map = dataHasil.get(key);
											if (map == null) {
												map = new HashMap<String, Object>();
												dataHasil.put(key, map);
											}
											map.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
											map.put("jurusan", mahasiswa.getJurusan().getNama());

											Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(),
													tahunAkademik, semestera,
													mahasiswa.getPindahKeKampusIniMasukSemester(),
													mahasiswa.getSemesterMulai());

											HistoryStatusMahasiswa historyStatusMahasiswa = Common
													.currentStatus(mahasiswa, tahunAkademik, smt);
											if (historyStatusMahasiswa == null
													|| historyStatusMahasiswa.getStatusMahasiswa() == null) {
												continue;
											}

											Integer aktif_lk = (Integer) map.get("aktif_lk");
											if (aktif_lk == null) {
												aktif_lk = 0;
											}
											Integer aktif_pr = (Integer) map.get("aktif_pr");
											if (aktif_pr == null) {
												aktif_pr = 0;
											}
											Integer aktif_total = (Integer) map.get("aktif_total");
											if (aktif_total == null) {
												aktif_total = 0;
											}

											if (mahasiswa.getKelamin().equalsIgnoreCase("Laki-laki")
													&& historyStatusMahasiswa.getStatusMahasiswa().getId()
															.equals(ConstantValues.AKTIF.getId())) {
												aktif_lk++;
												aktif_total++;
											} else if (mahasiswa.getKelamin().equalsIgnoreCase("Perempuan")
													&& historyStatusMahasiswa.getStatusMahasiswa().getId()
															.equals(ConstantValues.AKTIF.getId())) {
												aktif_pr++;
												aktif_total++;
											}

											map.put("aktif_lk", aktif_lk);
											map.put("aktif_pr", aktif_pr);
											map.put("aktif_total", aktif_total);

											Integer cuti_lk = (Integer) map.get("cuti_lk");
											if (cuti_lk == null) {
												cuti_lk = 0;
											}
											Integer cuti_pr = (Integer) map.get("cuti_pr");
											if (cuti_pr == null) {
												cuti_pr = 0;
											}
											Integer cuti_total = (Integer) map.get("cuti_total");
											if (cuti_total == null) {
												cuti_total = 0;
											}

											if (mahasiswa.getKelamin().equalsIgnoreCase("Laki-laki")
													&& historyStatusMahasiswa.getStatusMahasiswa().getId()
															.equals(ConstantValues.CUTI.getId())) {
												cuti_lk++;
												cuti_total++;
											} else if (mahasiswa.getKelamin().equalsIgnoreCase("Perempuan")
													&& historyStatusMahasiswa.getStatusMahasiswa().getId()
															.equals(ConstantValues.CUTI.getId())) {
												cuti_pr++;
												cuti_total++;
											}

											map.put("cuti_lk", cuti_lk);
											map.put("cuti_pr", cuti_pr);
											map.put("cuti_total", cuti_total);

											Integer lulus_lk = (Integer) map.get("lulus_lk");
											if (lulus_lk == null) {
												lulus_lk = 0;
											}
											Integer lulus_pr = (Integer) map.get("lulus_pr");
											if (lulus_pr == null) {
												lulus_pr = 0;
											}
											Integer lulus_total = (Integer) map.get("lulus_total");
											if (lulus_total == null) {
												lulus_total = 0;
											}

											if (mahasiswa.getKelamin().equalsIgnoreCase("Laki-laki")
													&& historyStatusMahasiswa.getStatusMahasiswa().getId()
															.equals(ConstantValues.LULUS.getId())) {
												lulus_lk++;
												lulus_total++;
											} else if (mahasiswa.getKelamin().equalsIgnoreCase("Perempuan")
													&& historyStatusMahasiswa.getStatusMahasiswa().getId()
															.equals(ConstantValues.LULUS.getId())) {
												lulus_pr++;
												lulus_total++;
											}

											map.put("lulus_lk", lulus_lk);
											map.put("lulus_pr", lulus_pr);
											map.put("lulus_total", lulus_total);

											Integer do_lk = (Integer) map.get("do_lk");
											if (do_lk == null) {
												do_lk = 0;
											}
											Integer do_pr = (Integer) map.get("do_pr");
											if (do_pr == null) {
												do_pr = 0;
											}
											Integer do_total = (Integer) map.get("do_total");
											if (do_total == null) {
												do_total = 0;
											}

											if (mahasiswa.getKelamin().equalsIgnoreCase("Laki-laki")
													&& (historyStatusMahasiswa.getStatusMahasiswa().getId()
															.equals(ConstantValues.KELUAR.getId())
															|| historyStatusMahasiswa.getStatusMahasiswa().getId()
																	.equals(ConstantValues.DROP_OUT.getId()))) {
												do_lk++;
												do_total++;
											} else if (mahasiswa.getKelamin().equalsIgnoreCase("Perempuan")
													&& (historyStatusMahasiswa.getStatusMahasiswa().getId()
															.equals(ConstantValues.KELUAR.getId())
															|| historyStatusMahasiswa.getStatusMahasiswa().getId()
																	.equals(ConstantValues.DROP_OUT.getId()))) {
												do_pr++;
												do_total++;
											}

											map.put("do_lk", do_lk);
											map.put("do_pr", do_pr);
											map.put("do_total", do_total);

											Integer ta_lk = (Integer) map.get("ta_lk");
											if (ta_lk == null) {
												ta_lk = 0;
											}
											Integer ta_pr = (Integer) map.get("ta_pr");
											if (ta_pr == null) {
												ta_pr = 0;
											}
											Integer ta_total = (Integer) map.get("ta_total");
											if (ta_total == null) {
												ta_total = 0;
											}

											if (mahasiswa.getKelamin().equalsIgnoreCase("Laki-laki")
													&& historyStatusMahasiswa.getStatusMahasiswa().getId()
															.equals(ConstantValues.TIDAK_AKTIF.getId())) {
												ta_lk++;
												ta_total++;
											} else if (mahasiswa.getKelamin().equalsIgnoreCase("Perempuan")
													&& historyStatusMahasiswa.getStatusMahasiswa().getId()
															.equals(ConstantValues.TIDAK_AKTIF.getId())) {
												ta_pr++;
												ta_total++;
											}

											map.put("ta_lk", ta_lk);
											map.put("ta_pr", ta_pr);
											map.put("ta_total", ta_total);
										}
									}
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapJumlahMahasiswa.java:842");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Jumlah Mahasiswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
		}

		maps = new ArrayList<Map>();
		for (Map map : dataHasil.values()) {
			maps.add(map);
		}
		ais.action.report.helper.LoadingReportUtil.selesai(label);
	}

	@SuppressWarnings({})
	public void onCetak(Event event) throws Exception {

		generateParameter();

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {

				if (mahasiswasTanpaJenisKelamin != null && !mahasiswasTanpaJenisKelamin.isEmpty()) {

					String content = "<p style='color:red'>Peringatan !!. Terdapat "
							+ mahasiswasTanpaJenisKelamin.size()
							+ " data mahasiswa yang belum benar data jenis kelamin-nya, data yang benar adalah \"Laki-laki\" atau \"Perempuan\", selain dua data tsb dianggap salah, sehingga bisa menyebabkan data tidak valid.<br>Harap segera perbaiki data antara lain :</p>";

					content += "<table>";
					for (Mahasiswa mahasiswa : mahasiswasTanpaJenisKelamin) {
						content += "<tr><td>" + mahasiswa.getNim() + "</td><td>" + mahasiswa.getNama() + "</td><td>"
								+ mahasiswa.getKelamin() + "</td></tr>";
					}
					content += "</table>";

					html.setContent(content);
				}

				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "rekap_jumlah_mahasiswa_semua",
						ais.ui.util.WaktuUtil.getDate(), null, toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					generateDataDanImageAlbum(label);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapJumlahMahasiswa.java:893");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekap Jumlah Mahasiswa", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}).start();

	}

}
