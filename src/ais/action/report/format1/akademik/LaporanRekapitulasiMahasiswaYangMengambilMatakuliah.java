package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class LaporanRekapitulasiMahasiswaYangMengambilMatakuliah extends MyWindow {

	private static final long serialVersionUID = 4766478176972379068L;
	private Combobox tahunAkademikUjianAkhirSemester;
	private Combobox genapGanjilUjianAkhirSemester;
	private Combobox kurikulumFakultas;
	private Combobox kurikulumJurusan;
	private Combobox program;

	private Combobox tahunAkademik;
	// private Textbox kodeMatakuliah;
	// private Textbox namaMatakuliah;
	private Intbox tahunAngkatan;
	// private Combobox status;
	private Center center;
	private Toolbar toolbar;

	private Combobox kelamin;
	private AmbilDataMatakuliahBanbox matakuliah;

	public LaporanRekapitulasiMahasiswaYangMengambilMatakuliah() {
		super();
		try {

			kurikulumFakultas = new Combobox();
			kurikulumJurusan = new Combobox();

			// status = new Combobox();
			//
			// Common.insertCombo(status, new String[] { "nama", "kodeEpsbed" },
			// StatusMahasiswa.class);

			Common.insertCombo(kurikulumFakultas, new String[] { "nama", "kode" }, Fakultas.class,
					Restrictions.eq("aktif", true));

			class SearchFakultasEventListener implements EventListener {

				@Override
				public void onEvent(Event event) throws Exception {
					// TODO Auto-generated method stub
					Common.clear(kurikulumJurusan);
					kurikulumJurusan.setSelectedItem(null);
					if (kurikulumFakultas.getSelectedItem() == null) {
						return;
					}
					Common.insertCombo(kurikulumJurusan, "nama", Jurusan.class,
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
							CommonSearchFilterHelper.eqSelectedWithId("fakultas", kurikulumFakultas, false));
				}

			}

			kurikulumFakultas.addEventListener("onChange", new SearchFakultasEventListener());

			// Apabila user berwenang hanya di fakultas tertentu, maka user
			// hanya
			// boleh mengakses data fakultas atau jurusan tertentu

			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser.ambilFakultas() != null) {
				Common.selectComboItem(kurikulumFakultas, tbmuser.ambilFakultas());
				Common.clear(kurikulumJurusan);
				Common.insertCombo(kurikulumJurusan, "nama", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
				kurikulumFakultas.setDisabled(true);
			} else {
				kurikulumFakultas.setDisabled(false);
			}

			if (tbmuser.ambilJurusan() != null) {
				Common.selectComboItem(kurikulumJurusan, tbmuser.ambilJurusan());
				kurikulumJurusan.setDisabled(true);
			} else {
				kurikulumJurusan.setDisabled(false);
			}

			initJadwalPerkuliahan();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi Mahasiswa Yang Mengambil Matakuliah", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanRekapitulasiMahasiswaYangMengambilMatakuliah(String title, String border, boolean closable)
			throws Exception {
		super(title, border, closable);

		kurikulumFakultas = new Combobox();
		kurikulumJurusan = new Combobox();
		// if (kurikulumFakultas != null) {
		// Common.insertCombo(kurikulumFakultas, new String[]{"nama", "kode"},
		// Fakultas.class);
		// Common.insertCombo(kurikulumJurusan, "nama", Jurusan.class,
		// Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif",
		// true)));
		// kurikulumFakultas.setSelectedIndex(0);
		// kurikulumJurusan.setSelectedIndex(0);
		// kurikulumJurusan.setDisabled(false);
		// kurikulumFakultas.setDisabled(false);
		// }

		Common.insertCombo(kurikulumFakultas, new String[] { "nama", "kode" }, Fakultas.class,
				Restrictions.eq("aktif", true));

		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(kurikulumJurusan);
				kurikulumJurusan.setSelectedItem(null);
				if (kurikulumFakultas.getSelectedItem() == null) {
					return;
				}
				Common.insertCombo(kurikulumJurusan, "nama", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", kurikulumFakultas, false));
			}

		}

		kurikulumFakultas.addEventListener("onChange", new SearchFakultasEventListener());

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(kurikulumFakultas, tbmuser.ambilFakultas());
			Common.clear(kurikulumJurusan);
			Common.insertCombo(kurikulumJurusan, "nama", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			kurikulumFakultas.setDisabled(true);
		} else {
			kurikulumFakultas.setDisabled(false);
		}

		if (tbmuser.ambilJurusan() != null) {
			Common.selectComboItem(kurikulumJurusan, tbmuser.ambilJurusan());
			kurikulumJurusan.setDisabled(true);
		} else {
			kurikulumJurusan.setDisabled(false);
		}

		initJadwalPerkuliahan();
		init();
	}

	private void initJadwalPerkuliahan() throws Exception {
		tahunAkademikUjianAkhirSemester = new Combobox();
		tahunAkademikUjianAkhirSemester = Common.generateTahunAjaran(tahunAkademikUjianAkhirSemester);

		genapGanjilUjianAkhirSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		genapGanjilUjianAkhirSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		genapGanjilUjianAkhirSemester.appendChild(comboitem);

	}

	private void init() throws Exception {

		program = Common.initPrograms(null);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(genapGanjilUjianAkhirSemester);
		genapGanjilUjianAkhirSemester.setWidth("90%");
		genapGanjilUjianAkhirSemester.setReadonly(true);
		Common.selectComboItem(genapGanjilUjianAkhirSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
		row.appendChild(matakuliah = new AmbilDataMatakuliahBanbox());
		matakuliah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(kurikulumFakultas);
		kurikulumFakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(kurikulumJurusan);
		kurikulumJurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunAngkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)));
		tahunAngkatan.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		// row.appendChild(status);
		// status.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kelamin"));
		row.appendChild(kelamin = new Combobox());
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Laki-laki");
		comboitem.setValue("Laki-laki");
		kelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Perempuan");
		comboitem.setValue("Perempuan");
		kelamin.appendChild(comboitem);
		kelamin.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		MyButtonConfig tombol;
		row.appendChild(tombol = new MyButtonConfig("Lihat Laporan"));
		ais.action.master.dashboard.admin.RekapMahasiswaViewHelper.pasangTombolRingkasan(row, center, tahunAkademik, null, null, null);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporanPerkuliahan(arg0);
			}
		};
		tombol.addEventListener("onClick", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Rekap_Data_Mahasiswa_Matakuliah", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporanPerkuliahan(arg0);
			}
		}));

		// onLaporanPerkuliahan(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Map generateParameter() throws Exception {

		// if (kurikulumFakultas.getSelectedItem() == null) {
		// MyMessageboxConfig.show("Fakultas" + " harus
		// diisi",
		// "Peringatan", MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return null;
		// }
		// if (kurikulumJurusan.getSelectedItem() == null) {
		// MyMessageboxConfig.show(Common.getBahasaConfig("Jurusan") + " harus
		// diisi",
		// "Peringatan", MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return null;
		// }
		// if (tahunAngkatan.getValue() == null) {
		// MyMessageboxConfig.show("Tahun Angkatan harus diisi", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return null;
		// }

		Matakuliah myMatakuliah = (Matakuliah) matakuliah.getAttribute("matakuliah");

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("tahunAkademik", tahunAkademik.getSelectedItem().getValue());
		parameters.put("fakultas", kurikulumFakultas.getSelectedItem() == null ? -1L
				: ((Fakultas) kurikulumFakultas.getSelectedItem().getValue()).getId());
		parameters.put("jurusan", kurikulumJurusan.getSelectedItem() == null ? -1L
				: ((Jurusan) kurikulumJurusan.getSelectedItem().getValue()).getId());

		parameters.put("jenisSemester", genapGanjilUjianAkhirSemester.getSelectedItem().getValue());

		parameters.put("kodeMatakuliah", "");
		parameters.put("namaMatakuliah", "");
		parameters.put("matakuliah", myMatakuliah == null || myMatakuliah.getId() == null ? -1L : myMatakuliah.getId());

		parameters.put("tahunangkatan", tahunAngkatan.getValue() == null ? -1 : tahunAngkatan.getValue());
		// parameters.put("status", status.getSelectedItem() ==
		// null||status.getSelectedItem().getValue()==null ? -1L
		// : ((StatusMahasiswa) status.getSelectedItem().getValue()).getId());

		parameters.put("program",
				program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? "-1"
						: program.getSelectedItem().getValue());

		parameters.put("kelamin", kelamin.getSelectedItem() == null ? "-1" : kelamin.getSelectedItem().getValue());

		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public void onLaporanPerkuliahan(Event event) throws Exception {
		final Map parameters = generateParameter();
		if (parameters == null) {
			return;
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					File file = Report.generateFileReportWithProgress(Report.PDF, parameters, "Rekap_Data_Mahasiswa_Matakuliah",
							ais.ui.util.WaktuUtil.getDate(), toolbar);
					CommonReport.tampilkanReportPDF(center, file);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekapitulasi Mahasiswa Yang Mengambil Matakuliah", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
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
