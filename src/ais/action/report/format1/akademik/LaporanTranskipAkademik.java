package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.PenilaianMahasiswaHelper;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Kurikulum;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Skripsi;
import ais.database.model.Staff;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

public class LaporanTranskipAkademik extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1550813616089440767L;
	private MyDatebox tanggal;

	private AmbilDataMahasiswaBanbox bandboxMahasiswa;
	private MyCheckboxConfig ambilDariKurikulum;

	private MyCheckboxConfig hitungUlang = new MyCheckboxConfig("Hitung Ulang IP/IPK");
	private Center center;
	private Toolbar toolbar;
	private String namaFile = "Transkrip_Akademik";
	private MyDatebox tanggalDicetak;
	private Mahasiswa mahasiswa = null;
	private Combobox semesterAbsensiUjian;

	public LaporanTranskipAkademik() {
		super();
		try {
			initTranskripAkademik();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Transkip Akademik", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanTranskipAkademik(Mahasiswa mahasiswa) {
		super();
		this.mahasiswa = mahasiswa;
		try {
			initTranskripAkademik();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Transkip Akademik", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanTranskipAkademik(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initTranskripAkademik();
		init();
	}

	private void initTranskripAkademik() throws Exception {
		tanggal = new MyDatebox();
		tanggal.setValue(ais.ui.util.WaktuUtil.getDate());

	}

	private void init() throws Exception {
		Tbmuser tbmuser = Common.getCurrentUser();
		if (mahasiswa == null) {

			if (tbmuser != null && tbmuser.getMahasiswa() != null) {
				if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(tbmuser.getMahasiswa())) {
					return;
				}
			}
		}

		Common.initDefaultJudisium();

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				onTranskrip(event);

			}
		};

		Tabpanel tabpanel1 = null;

		if (mahasiswa == null && tbmuser.getMahasiswa() == null
				&& Common.bolehKonfigurasi("tampilkan_semua_bentuk_transkrip")) {
			Tabbox tabbox = new Tabbox();
			tabbox.setParent(Common.tampilanScrollTabbox(this));
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			MyTabConfig tab1 = new MyTabConfig("Transkrip Akademik");
			tab1.setParent(tabs);

			MyTabConfig tab51 = new MyTabConfig("Preview Ijazah");
			tab51.setVisible(Common.bolehKonfigurasi("tampilkan_ijazah_ke_admin"));
			tab51.setParent(tabs);

			MyTabConfig tab2 = new MyTabConfig("Transkrip 2 Kolom");
			tab2.setParent(tabs);

			MyTabConfig tab3 = new MyTabConfig("Transkrip 2 Halaman");
			tab3.setParent(tabs);

			MyTabConfig tab4 = new MyTabConfig("Transkrip 4 Kolom");
			tab4.setParent(tabs);

			MyTabConfig tab5 = new MyTabConfig("Transkrip IPK");
			tab5.setParent(tabs);

			MyTabConfig tab6 = new MyTabConfig("IPK Per Prodi dan Angkatan");
			tab6.setVisible(Common.getCurrentUser() != null && Common.getCurrentUser().getMahasiswa() == null);
			tab6.setParent(tabs);

			MyTabConfig tab7 = new MyTabConfig("IPK berdasar Kelompok");
			tab7.setParent(tabs);

			MyTabConfig tab8 = new MyTabConfig("IPK berdasar Kelompok Per Prodi dan Angkatan");
			tab8.setVisible(Common.getCurrentUser() != null && Common.getCurrentUser().getMahasiswa() == null);
			tab8.setParent(tabs);

			MyTabConfig tab9 = new MyTabConfig("IPK berdasar Kelompok 1");
			tab9.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			tabpanel1 = new ais.ui.util.MyTabpanel();
			tabpanel1.setParent(tabpanels);

			final Tabpanel tabpanel51 = new ais.ui.util.MyTabpanel();
			tabpanel51.setParent(tabpanels);
			tabpanel51.setVisible(tab51.isVisible());
			tab51.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel51.getChildren().size() == 0) {
						LaporanIjazahAkademik laporanIjazahAkademik = new LaporanIjazahAkademik();
						laporanIjazahAkademik.setHeight("100%");
						laporanIjazahAkademik.setWidth("100%");
						laporanIjazahAkademik.setParent(tabpanel51);
					}
				}
			});

			final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
			tabpanel2.setParent(tabpanels);
			tab2.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel2.getChildren().size() == 0) {
						LaporanRekamanNilai2Kolom laporanRekamanNilai = new LaporanRekamanNilai2Kolom();
						laporanRekamanNilai.setHeight("100%");
						laporanRekamanNilai.setWidth("100%");
						laporanRekamanNilai.setParent(tabpanel2);
					}
				}
			});

			final Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
			tabpanel3.setParent(tabpanels);
			tab3.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel3.getChildren().size() == 0) {
						LaporanTranskipAkademikBeda laporanTranskipAkademikBeda = new LaporanTranskipAkademikBeda();
						laporanTranskipAkademikBeda.setHeight("100%");
						laporanTranskipAkademikBeda.setWidth("100%");
						laporanTranskipAkademikBeda.setParent(tabpanel3);
					}
				}
			});

			final Tabpanel tabpanel4 = new ais.ui.util.MyTabpanel();
			tabpanel4.setParent(tabpanels);
			tab4.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel4.getChildren().size() == 0) {
						LaporanTranskipAkademik4Kolom laporanTranskipAkademik4Kolom = new LaporanTranskipAkademik4Kolom();
						laporanTranskipAkademik4Kolom.setHeight("100%");
						laporanTranskipAkademik4Kolom.setWidth("100%");
						laporanTranskipAkademik4Kolom.setParent(tabpanel4);
					}
				}
			});

			final Tabpanel tabpanel5 = new ais.ui.util.MyTabpanel();
			tabpanel5.setParent(tabpanels);
			tab5.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel5.getChildren().size() == 0) {
						LaporanRekamanNilai laporanRekamanNilai = new LaporanRekamanNilai(mahasiswa);
						laporanRekamanNilai.setHeight("100%");
						laporanRekamanNilai.setWidth("100%");
						laporanRekamanNilai.setParent(tabpanel5);
					}
				}
			});

			final Tabpanel tabpanel6 = new ais.ui.util.MyTabpanel();
			tabpanel6.setParent(tabpanels);
			tab6.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel6.getChildren().size() == 0) {
						LaporanRekamanNilaiPerProdiDanAngkatan laporanRekamanNilai = new LaporanRekamanNilaiPerProdiDanAngkatan();
						laporanRekamanNilai.setHeight("100%");
						laporanRekamanNilai.setWidth("100%");
						laporanRekamanNilai.setParent(tabpanel6);
					}
				}
			});

			final Tabpanel tabpanel7 = new ais.ui.util.MyTabpanel();
			tabpanel7.setParent(tabpanels);
			tab7.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel7.getChildren().size() == 0) {
						LaporanRekamanNilaiKelompok laporanRekamanNilai = new LaporanRekamanNilaiKelompok();
						laporanRekamanNilai.setHeight("100%");
						laporanRekamanNilai.setWidth("100%");
						laporanRekamanNilai.setParent(tabpanel7);
					}
				}
			});

			final Tabpanel tabpanel8 = new ais.ui.util.MyTabpanel();
			tabpanel8.setParent(tabpanels);
			tab8.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel8.getChildren().size() == 0) {
						LaporanRekamanNilaiKelompokPerProdiDanAngkatan laporanRekamanNilai = new LaporanRekamanNilaiKelompokPerProdiDanAngkatan();
						laporanRekamanNilai.setHeight("100%");
						laporanRekamanNilai.setWidth("100%");
						laporanRekamanNilai.setParent(tabpanel8);
					}
				}
			});

			final Tabpanel tabpanel9 = new ais.ui.util.MyTabpanel();
			tabpanel9.setParent(tabpanels);
			tab9.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel9.getChildren().size() == 0) {
						LaporanRekamanNilaiKelompokType1 laporanRekamanNilai = new LaporanRekamanNilaiKelompokType1();
						laporanRekamanNilai.setHeight("100%");
						laporanRekamanNilai.setWidth("100%");
						laporanRekamanNilai.setParent(tabpanel9);
					}
				}
			});
		} else {
			if (tbmuser.getMahasiswa() != null
					&& Common.bolehKonfigurasi("tampilkan_semua_bentuk_beda_transkrip", Konfigurasi.TIDAK_AKTIF)) {
				namaFile = "Transkrip_Akademik_Mahasiswa";
			}

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(Common.tampilanScrollTabbox(this));
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			MyTabConfig tab1 = new MyTabConfig("Transkrip Akademik Mahasiswa");
			tab1.setParent(tabs);

			MyTabConfig tab2 = new MyTabConfig("Transkrip 2 Kolom");
			tab2.setVisible(Common.bolehKonfigurasi("tampilkan_transkrip_2_kolom_ke_mhs"));
			tab2.setParent(tabs);

			MyTabConfig tab51 = new MyTabConfig("Ijazah");
			tab51.setVisible(Common.bolehKonfigurasi("tampilkan_ijazah_ke_mhs"));
			tab51.setParent(tabs);

			MyTabConfig tab5 = new MyTabConfig("Transkrip IPK");
			tab5.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			tabpanel1 = new ais.ui.util.MyTabpanel();
			tabpanel1.setParent(tabpanels);

			final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
			tabpanel2.setParent(tabpanels);
			tab2.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel2.getChildren().size() == 0) {
						LaporanRekamanNilai2Kolom laporanRekamanNilai = new LaporanRekamanNilai2Kolom(mahasiswa);
						laporanRekamanNilai.setHeight("100%");
						laporanRekamanNilai.setWidth("100%");
						laporanRekamanNilai.setParent(tabpanel2);
					}
				}
			});

			final Tabpanel tabpanel51 = new ais.ui.util.MyTabpanel();
			tabpanel51.setParent(tabpanels);
			tab51.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel51.getChildren().size() == 0) {
						LaporanIjazahAkademik laporanIjazahAkademik = new LaporanIjazahAkademik(mahasiswa);
						laporanIjazahAkademik.setHeight("100%");
						laporanIjazahAkademik.setWidth("100%");
						laporanIjazahAkademik.setParent(tabpanel51);
					}
				}
			});

			final Tabpanel tabpanel5 = new ais.ui.util.MyTabpanel();
			tabpanel5.setParent(tabpanels);
			tab5.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel5.getChildren().size() == 0) {
						LaporanRekamanNilai laporanRekamanNilai = new LaporanRekamanNilai(mahasiswa);
						laporanRekamanNilai.setHeight("100%");
						laporanRekamanNilai.setWidth("100%");
						laporanRekamanNilai.setParent(tabpanel5);
					}
				}
			});
		}

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel1 == null ? this : tabpanel1);

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
		column.setWidth("45%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_mahasiswa")));
		row.appendChild(bandboxMahasiswa = new AmbilDataMahasiswaBanbox());
		bandboxMahasiswa.setWidth("90%");

		if (this.mahasiswa != null
				|| (Common.getCurrentUser() != null && Common.getCurrentUser().getMahasiswa() != null)) {
			Mahasiswa mahasiswa = this.mahasiswa != null ? this.mahasiswa : Common.getCurrentUser().getMahasiswa();
			bandboxMahasiswa.setAttribute("mahasiswa", mahasiswa);
			bandboxMahasiswa.setAttribute("myValue", mahasiswa);
			bandboxMahasiswa.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
			bandboxMahasiswa.setId("mhs_" + mahasiswa.getId());
			bandboxMahasiswa.setDisabled(true);
		}
		bandboxMahasiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
					return;
				}
				Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
				Common.selectComboItem(semesterAbsensiUjian, mahasiswa.currentSemester());

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onTranskrip(arg0);
					}
				});

			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hitungUlang);
		hitungUlang.addEventListener("onClick", eventListener);
		hitungUlang.setChecked(true);

		semesterAbsensiUjian = new Combobox();
		for (int i = 1; i <= 21; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semesterAbsensiUjian.appendChild(comboitem);
		}

		if (bandboxMahasiswa.getAttribute("mahasiswa") != null) {
			Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
			Common.selectComboItem(semesterAbsensiUjian, mahasiswa.currentSemester());
		} else {
			Common.selectComboItem(semesterAbsensiUjian, 1);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterAbsensiUjian);
		semesterAbsensiUjian.setWidth("90%");
		semesterAbsensiUjian.setReadonly(true);
		semesterAbsensiUjian.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Ditetapkan"));
		tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);
		tanggal.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Cetak"));
		tanggalDicetak = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggalDicetak);
		tanggalDicetak.setWidth("90%");
		tanggalDicetak.addEventListener("onChange", eventListener);
		tanggalDicetak.setDisabled(tbmuser == null || tbmuser.getMahasiswa() != null);
		tanggalDicetak.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(ambilDariKurikulum = new MyCheckboxConfig("Ambil berdasarkan kurikulum"));
		ambilDariKurikulum.addEventListener("onCheck", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
					MyMessageboxConfig.show("Mohon maaf, Mahasiswa belum dipilih. Langkah yang dapat dilakukan: (1) Ketik NIM atau nama mahasiswa pada kolom pencarian lalu pilih dari hasil yang muncul; (2) Pastikan data mahasiswa terdaftar di sistem; (3) Ulangi proses cetak transkrip akademik. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (tanggal.getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Tanggal cetak belum diisi. Langkah yang dapat dilakukan: (1) Isi kolom Tanggal dengan tanggal cetak yang valid; (2) Pastikan format tanggal sesuai ketentuan sistem; (3) Ulangi proses cetak transkrip akademik. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (semesterAbsensiUjian.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, Semester belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Semester dari daftar dropdown yang tersedia; (2) Pastikan data semester sudah dikonfigurasi; (3) Ulangi proses cetak transkrip akademik. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				Map parameters = generateParameter();
				return parameters;
			}
		}, namaFile, null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onTranskrip(arg0);

			}
		}));

		onTranskrip(null);

	}

	@SuppressWarnings({ "rawtypes" })
	public Map generateParameter() throws Exception {
		// if (tahunAkademikUjianAkhirSemester.getSelectedItem() == null) {
		// // MyMessageboxConfig.show("Pilih salah satu tahun akademik",
		// "Peringatan",
		// // 1,
		// // MyMessageboxConfig.INFORMATION);
		// return null;
		// }
		if (semesterAbsensiUjian.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih ganjil atau genap", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
			// MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");

		int semester = (Integer) semesterAbsensiUjian.getSelectedItem().getValue();

		return LaporanTranskipAkademik.generateParameter(mahasiswa, semester, hitungUlang.isChecked(),
				ambilDariKurikulum.isChecked(), tanggal.getValue(), tanggalDicetak.getValue());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map generateParameter(Mahasiswa mahasiswa, int semester, boolean hitungUang,
			boolean ambilDariKurikulum, Date tanggal, Date tanggalDicetak) throws Exception {
		Session session = null;
		boolean closeLocalSession = false;
		try {
			session = HibernateUtil.currentSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanTranskipAkademik.java:596");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Transkip Akademik", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		if (session == null || !session.isOpen()) {
			session = HibernateUtil.openSession();
			closeLocalSession = true;
		}
			try {
		if (mahasiswa != null && mahasiswa.getId() != null) {
			Mahasiswa m = (Mahasiswa) session.get(Mahasiswa.class, mahasiswa.getId());
			if (m != null) {
				mahasiswa = m;
			}
		}
		Staff staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.ilike("staff", "dekan"))
				.setMaxResults(1).add(Restrictions.eq("jurusan", mahasiswa.getJurusan())).uniqueResult();
		if (staffDekan == null) {
			staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.ilike("staff", "dekan"))
					.setMaxResults(1).add(Restrictions.eq("fakultas", mahasiswa.getJurusan().getFakultas()))
					.uniqueResult();
		}
		if (staffDekan == null) {
			staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.ilike("staff", "dekan"))
					.setMaxResults(1).uniqueResult();
		}

		Staff staffRektor = (Staff) session.createCriteria(Staff.class).add(Restrictions.ilike("staff", "rektor"))
				.setMaxResults(1).uniqueResult();

		if (staffRektor == null) {
			staffRektor = new Staff();
			staffRektor.setNama("Rektor");
			staffRektor.setNip("NIP Rektor");
			staffRektor.setStaff("rektor");
			session.save(staffRektor);
		}

		Staff pembatuRektor = (Staff) session.createCriteria(Staff.class)
				.add(Restrictions.ilike("staff", "Pembatu Rektor")).setMaxResults(1).uniqueResult();
		if (pembatuRektor == null) {
			pembatuRektor = new Staff();
			pembatuRektor.setNama("Pembatu Rektor");
			pembatuRektor.setNip("NIP Pembatu Rektor");
			pembatuRektor.setStaff("Pembatu Rektor");
			session.save(pembatuRektor);
		}

		Date date = tanggal;
		Integer angkatan = mahasiswa.getTahunangkatan();

		Kurikulum kurikulum = (Kurikulum) session.createCriteria(Kurikulum.class)
				.add(Restrictions.le("tahun", angkatan)).add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
				// .add(Restrictions.eq("program", mahasiswa.getProgram()))
				.addOrder(Order.desc("tahun")).setMaxResults(1).uniqueResult();
		if (kurikulum == null) {
			kurikulum = (Kurikulum) session.createCriteria(Kurikulum.class).add(Restrictions.le("tahun", angkatan))
					.add(Restrictions.eq("jurusan", mahasiswa.getJurusan())).addOrder(Order.desc("tahun"))
					.setMaxResults(1).uniqueResult();
		}
		if (kurikulum == null) {
			kurikulum = (Kurikulum) session.createCriteria(Kurikulum.class)
					// .add(Restrictions.le("tahun", angkatan))
					.add(Restrictions.eq("jurusan", mahasiswa.getJurusan())).addOrder(Order.desc("tahun"))
					.setMaxResults(1).uniqueResult();
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("tanggal", date);
		parameters.put("rektor", staffRektor == null ? "" : staffRektor.getNama());
		parameters.put("pembantu_rektor", pembatuRektor == null ? "" : pembatuRektor.getNama());
		parameters.put("pembantu_rektor_nip", pembatuRektor == null ? "" : pembatuRektor.getNip());

		parameters.put("dekan", staffDekan == null ? "" : staffDekan.getNama());
		parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? 1L : mahasiswa.getId());

		parameters.put("jurusan", mahasiswa.getJurusan().getNama());
		parameters.put("konsentrasi", mahasiswa.getKonsentrasi() == null ? "" : mahasiswa.getKonsentrasi().getNama());
		parameters.put("tanggal_dicetak", tanggalDicetak == null ? ais.ui.util.WaktuUtil.getDate() : tanggalDicetak);

		if (hitungUang) {
			mahasiswa.reInitDetailperkuliahan(session);
		}
		KrsMahasiswa.parameterData(mahasiswa, semester, hitungUang, parameters);

		mahasiswa.putPhotoLulus(parameters);

		parameters.put("ambilDariKurikulum", ambilDariKurikulum);
		if (ambilDariKurikulum) {
			parameters.put("subreport", "Transkrip_Akademik_subreport_k");
			parameters.put("kurikulum", mahasiswa == null ? 1L : kurikulum.getId());
			System.out.println("kurikulum : " + kurikulum.getNama());
		} else {
			parameters.put("subreport", "Transkrip_Akademik_subreport0");
		}

		List<Long> sebelumDisaring = mahasiswa.ambilDetailperkuliahan(semester);

		// Saring di DB-level: buang ID yang perkuliahan-nya null atau matakuliahnya null
		// (bisa lolos dari Hibernate cache jika proxy non-null tapi FK di DB null).
		// Tanpa filter ini muncul baris "null null" di transkrip.
		if (!sebelumDisaring.isEmpty()) {
			Session filterSesi = HibernateUtil.currentSession();
			List<Number> idsValidRaw = filterSesi.createSQLQuery(
					"SELECT a.id FROM detailperkuliahan a "
					+ "LEFT JOIN perkuliahan b ON (a.perkuliahan = b.id) "
					+ "LEFT JOIN matakuliah f ON (f.id = b.matakuliah OR a.matakuliah_konversi = f.id) "
					+ "WHERE a.id IN (:ids) AND f.id IS NOT NULL AND coalesce(trim(f.nama), '') <> ''")
					.setParameterList("ids", sebelumDisaring)
					.list();
			List<Long> idsValid = new ArrayList<Long>();
			for (Number n : idsValidRaw) {
				idsValid.add(n.longValue());
			}
			sebelumDisaring.retainAll(idsValid);
		}

		parameters.put("sebelumDisaring", sebelumDisaring.toArray());

		parameters.put("jumlah_mk_sebelum", sebelumDisaring.size());

		List<Long> detailsperkuliahans = new ArrayList<Long>();
		// Matakuliah yang saling ekivalen cukup tampil SATU kali di transkrip (nilai tertinggi).
		for (Long detailperkuliahan : ais.action.master.helper.EkivalenNilaiUtil
				.saringNilaiTertinggi(mahasiswa.saringBerdasarNilaiDan0(sebelumDisaring), mahasiswa.getNim())) {
			detailsperkuliahans.add(detailperkuliahan);
		}
		// MK SKS=0 yang SUDAH bernilai tetap TAMPIL di transkrip (nilai huruf "L") namun TIDAK ikut
		// menghitung IPK (IPK dihitung terpisah di KrsMahasiswa.parameterData). Opsional per konfigurasi.
		if (Common.bolehKonfigurasi("tampilkan_matakuliah_sks_0_bernilai_di_transkrip", Konfigurasi.TIDAK_AKTIF)) {
			for (Long idSks0 : mahasiswa.saringTampilkanSks0Bernilai(sebelumDisaring)) {
				if (!detailsperkuliahans.contains(idSks0)) {
					detailsperkuliahans.add(idSks0);
				}
			}
		}
		parameters.put("jumlah_mk", detailsperkuliahans.size());
		if (detailsperkuliahans.isEmpty()) {
			detailsperkuliahans.add(-1L);
		}
		parameters.put("detailsperkuliahans", detailsperkuliahans.toArray());

		Common.insertProperty(Mahasiswa.class, mahasiswa, parameters, "");
		if (mahasiswa.getJurusan() != null) {
			Common.insertProperty(Jurusan.class, mahasiswa.getJurusan(), parameters, "jur");
		}
		if (mahasiswa.getJurusan().getFakultas() != null) {
			Common.insertProperty(Fakultas.class, mahasiswa.getJurusan().getFakultas(), parameters, "fak");
		}
		if (mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null) {
			Common.insertProperty(PerguruanTinggi.class, mahasiswa.getJurusan().getFakultas().getPerguruanTinggi(),
					parameters, "pt");
		}

		/* Callee di atas (insertProperty/saring nilai) bisa menutup session di
		 * tengah method -> "Session is closed!" saat query Skripsi (kasus API
		 * /api transkrip). Pakai session terbuka bila masih hidup, atau ambil
		 * ulang thread-local TANPA mengubah variabel session agar logika
		 * closeLocalSession di bawah tetap benar. */
		Session sesiSkripsi = session != null && session.isOpen() ? session
				: ais.database.hibernate.HibernateUtil.currentNativeSession();
		Skripsi skripsi = (Skripsi) sesiSkripsi.createCriteria(Skripsi.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
		if (skripsi != null) {
			Common.insertProperty(Skripsi.class, skripsi, parameters, "skripsi",1,"mahasiswa");
		}

		String subReport = Common.ambilREAL_PATH_REPORT() + "/";
		System.out.println("subReport = " + subReport);
		parameters.put("SUBREPORT_DIR", subReport);
		StreamingHibernateUtil.getInstance().closeSession();
		return parameters;
		} finally {
			// Tutup session yang DIBUKA lokal (fallback openSession) di FINALLY -> tak bocor saat exception.
			if (closeLocalSession && session != null && session.isOpen()) {
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanTranskipAkademik.java:751");}
			}
		}
	}

	@SuppressWarnings({})
	public void onTranskrip(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), namaFile,
					ais.ui.util.WaktuUtil.getDate(), toolbar);

			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Transkip Akademik", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
