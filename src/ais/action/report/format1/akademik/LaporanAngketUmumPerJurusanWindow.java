package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

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
import org.zkoss.zul.West;

import ais.action.master.GrupChecklistPenilaianUmumAction;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class LaporanAngketUmumPerJurusanWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4766478176972379068L;
	private Combobox tahunAkademik;
	private Combobox semesterAbsensi;
	private Combobox fakultas;
	private Combobox jurusan;

	private Toolbar toolbar;
	private Center center;
	private Combobox searchdiperuntukkan;

	public LaporanAngketUmumPerJurusanWindow() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Angket Umum Per Jurusan Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanAngketUmumPerJurusanWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	private void init() {
		setHeight("100%");
		setWidth("100%");
		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onLaporanAngketDosenPerDosen(event);
			}
		};

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

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
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.setReadonly(true);
		fakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);
		jurusan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaran(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", eventListener);
		tahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterAbsensi = new Combobox());
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setWidth("90%");
		semesterAbsensi.addEventListener("onChange", eventListener);
		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		semesterAbsensi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengguna"));
		row.appendChild(searchdiperuntukkan = new Combobox());
		GrupChecklistPenilaianUmumAction.diperuntukkan(searchdiperuntukkan);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		searchdiperuntukkan.appendChild(comboitem);
		searchdiperuntukkan.setWidth("90%");
		searchdiperuntukkan.addEventListener("onChange", eventListener);
		searchdiperuntukkan.setReadonly(true);

		searchdiperuntukkan.setSelectedItem(comboitem);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "rekap_angket_umum_per_jurusan", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporanAngketDosenPerDosen(arg0);
			}
		}));

		try {
			onLaporanAngketDosenPerDosen(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Umum Per Jurusan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());

		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());

		String genapGanjil = (String) (semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? "Semua"
						: semesterAbsensi.getSelectedItem().getValue());

		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? "Semua"
						: this.tahunAkademik.getSelectedItem().getValue());

		Map parameters = new java.util.HashMap();

		parameters.put("genapGanjil", genapGanjil == null ? "Semua" : genapGanjil);

		parameters.put("tahun_akademik", tahunAkademik == null ? "Semua" : tahunAkademik);

		String diperuntukkan = (String) (searchdiperuntukkan.getSelectedItem() == null
				|| searchdiperuntukkan.getSelectedItem().getValue() == null ? "Semua"
						: searchdiperuntukkan.getSelectedItem().getValue());
		parameters.put("diperuntukkan", diperuntukkan == null ? "Semua" : diperuntukkan);

		parameters.put("fakultas", fakultas == null || fakultas.getId() == null ? -1L : fakultas.getId());
		parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());
		parameters.put("jurusan_nama", jurusan == null ? "" : jurusan.getNama());
		parameters.put("fakultas_nama", fakultas == null ? "" : fakultas.getNama());

		return parameters;

	}

	@SuppressWarnings({})
	public void onLaporanAngketDosenPerDosen(Event event) throws Exception {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "rekap_angket_umum_per_jurusan",
						ais.ui.util.WaktuUtil.getDate(), toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

	}

}
