package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanSKSDosenPerMatakuliah extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	// Untuk Laporan SksDosen
	private Combobox sksDosenFakultas;
	private Combobox sksDosenJurusan;
	private Combobox sksDosenSemester;
	private Combobox tahunAkademik;
	private AmbilDataDosenBanbox dosen;
	private Vbox dosenPilihan;

	private AmbilDataMasaPerkuliahanBanbox masaPerkuliahan;
	private MyCheckboxConfig semesterPendek;
	private MyCheckboxConfig ekstrakurikuler;

	//
	private Center center;
	private Toolbar toolbar;

	public LaporanSKSDosenPerMatakuliah() {
		super();
		try {
			initSksDosen();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan SKS Dosen Per Matakuliah", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanSKSDosenPerMatakuliah(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initSksDosen();
		init();
	}

	private void initSksDosen() throws Exception {
		// sksDosenFakultas = new Combobox();
		sksDosenSemester = new Combobox();
		tahunAkademik = new Combobox();

		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);

		sksDosenFakultas = new Combobox();
		sksDosenJurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(sksDosenFakultas, sksDosenJurusan, null, null);

		sksDosenSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		sksDosenSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		sksDosenSemester.setValue(Perkuliahan.GANJIL);
		sksDosenSemester.appendChild(comboitem);
		sksDosenSemester.setReadonly(true);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		sksDosenSemester.appendChild(comboitem);

		Common.selectComboItem(sksDosenSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

	}

	private void init() {

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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(sksDosenFakultas);
		sksDosenFakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(sksDosenJurusan);
		sksDosenJurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(sksDosenSemester);
		sksDosenSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Perkuliahan"));
		row.appendChild(masaPerkuliahan = new AmbilDataMasaPerkuliahanBanbox());
		masaPerkuliahan.setWidth("90%");
		sksDosenJurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (sksDosenJurusan.getSelectedItem() != null) {
					masaPerkuliahan.setJurusanSelected((Jurusan) sksDosenJurusan.getSelectedItem().getValue());
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(this.semesterPendek = new MyCheckboxConfig("Semester Pendek"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(this.ekstrakurikuler = new MyCheckboxConfig("Ekstrakurikuler"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSksDosen(null);
			}
		});
		print.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		dosenPilihan = new Vbox();
		row.appendChild(dosenPilihan);
		dosenPilihan.setWidth("90%");
		for (int i = 1; i <= 10; i++) {
			MyCheckboxConfig checkbox = new MyCheckboxConfig("Dosen " + i);
			checkbox.setValue("dosen" + i);
			dosenPilihan.appendChild(checkbox);
		}

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
		}, "sks_dosen_per_matakuliah", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSksDosen(arg0);

			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Fakultas fakultas = (Fakultas) (sksDosenFakultas.getSelectedItem() == null ? null
				: sksDosenFakultas.getSelectedItem().getValue());

		Jurusan jurusan = (Jurusan) (sksDosenJurusan.getSelectedItem() == null ? null
				: sksDosenJurusan.getSelectedItem().getValue());

		String semester = (String) (sksDosenSemester.getSelectedItem() == null
				|| sksDosenSemester.getSelectedItem().getValue() == null ? "Semua"
						: sksDosenSemester.getSelectedItem().getValue());
		String tahun = (String) (tahunAkademik.getSelectedItem() == null
				|| tahunAkademik.getSelectedItem().getValue() == null ? "Semua"
						: tahunAkademik.getSelectedItem().getValue());

		Dosen dosen = (Dosen) this.dosen.getAttribute("dosen");

		Map parameters = ais.common.HashMapGenerator.getRand();
		MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) this.masaPerkuliahan.getAttribute("masaPerkuliahan");

		parameters.put("ekstrakurikuler", ekstrakurikuler.isChecked() ? Perkuliahan.EKSTRA : -1L);
		parameters.put("semester_pendek", semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : -1L);
		parameters.put("masa_perkuliahan", masaPerkuliahan == null || masaPerkuliahan.getId() == null ? -1L : masaPerkuliahan.getId());
		parameters.put("fakultas", fakultas == null || fakultas.getId() == null ? -1L : fakultas.getId());
		parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());
		parameters.put("semester", semester);
		parameters.put("tahun_akademik", tahun);
		parameters.put("dosen", dosen == null || dosen.getId() == null ? -1L : dosen.getId());

		return parameters;

	}

	@SuppressWarnings({})
	public void onSksDosen(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "sks_dosen_per_matakuliah",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan SKS Dosen Per Matakuliah", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
