package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataKelasBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Kelas;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusPertemuan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanKehadiranAsisten extends MyWindow {

	private Center center;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Combobox tahunAkademik;
	private Combobox genapGanjil;
	private Combobox fakultas;
	private Combobox jurusan;

	private Toolbar toolbar;

	private Textbox matakuliah;
	private AmbilDataKelasBanbox kelas;
	private AmbilDataMasaPerkuliahanBanbox masaPerkuliahan;
	private Textbox asisten;
	private MyCheckboxConfig semesterPendek;
	private MyCheckboxConfig ekstrakurikuler;
	private MyCheckboxConfig hanyaYgStatusMasuk;

	private MyDatebox mulai;
	private MyDatebox sampai;

	private AmbilDataDosenBanbox dosen;

	private AmbilDataMatakuliahBanbox matkul;

	private ArrayList<MyCheckboxConfig> listJenisPertemuan;

	public LaporanKehadiranAsisten() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kehadiran Asisten", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanKehadiranAsisten(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	private void init() {

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		genapGanjil = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		genapGanjil.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		genapGanjil.appendChild(comboitem);
		genapGanjil.setReadonly(true);

		Common.selectComboItem(genapGanjil, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		genapGanjil.setReadonly(true);

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

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(genapGanjil);
		genapGanjil.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(this.kelas = new AmbilDataKelasBanbox());
		kelas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(matkul = new AmbilDataMatakuliahBanbox());
		matkul.setCols(6);
		hbox.appendChild(new Label(""));
		hbox.appendChild(this.matakuliah = new Textbox());
		matakuliah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(this.dosen = new AmbilDataDosenBanbox());
		dosen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Asisten"));
		row.appendChild(this.asisten = new Textbox());
		asisten.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(mulai = new MyDatebox());
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		hbox.appendChild(sampai = new MyDatebox());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Perkuliahan"));
		row.appendChild(masaPerkuliahan = new AmbilDataMasaPerkuliahanBanbox());
		masaPerkuliahan.setWidth("90%");
		jurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (jurusan.getSelectedItem() != null) {
					masaPerkuliahan.setJurusanSelected((Jurusan) jurusan.getSelectedItem().getValue());
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
		row.appendChild(this.hanyaYgStatusMasuk = new MyCheckboxConfig("Tampilkan hanya yang hadir"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pertemuan"));
		row.appendChild(new ais.ui.util.MyLabelConfig());

		listJenisPertemuan = new ArrayList<MyCheckboxConfig>();
		for (Object o : ConstantValues.ambilBerdasarClass(StatusPertemuan.class).values()) {
			StatusPertemuan statusPertemuan = (StatusPertemuan) o;
			if (statusPertemuan.getAktif()) {
				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig());
				MyCheckboxConfig checkbox = new MyCheckboxConfig(statusPertemuan.getNama());
				checkbox.setChecked(true);
				checkbox.setAttribute("statusPertemuan", statusPertemuan);
				row.appendChild(checkbox);
				listJenisPertemuan.add(checkbox);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onCetak(null);
			}
		});
		print.setParent(row);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "LaporanAbsensiAsisten", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);

			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		final Map parameters = ais.common.HashMapGenerator.getRand();
		Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
				? null
				: jurusan.getSelectedItem().getValue());
		Fakultas myFakultas = (Fakultas) (fakultas.getSelectedItem() == null
				|| fakultas.getSelectedItem().getValue() == null ? null : fakultas.getSelectedItem().getValue());
		MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) this.masaPerkuliahan.getAttribute("masaPerkuliahan");

		parameters.put("tanggal", (mulai.getValue() == null ? "" : Common.dateFormat4.get().format(mulai.getValue()))
				+ (sampai.getValue() == null ? "" : " s.d " + Common.dateFormat4.get().format(sampai.getValue())));
		parameters.put("ekstrakurikuler", ekstrakurikuler.isChecked() ? Perkuliahan.EKSTRA : -1L);
		parameters.put("semester_pendek", semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : -1L);
		parameters.put("masa_perkuliahan", masaPerkuliahan == null || masaPerkuliahan.getId() == null ? -1L : masaPerkuliahan.getId());
		parameters.put("fakultas", myFakultas == null || myFakultas.getId() == null ? -1L : myFakultas.getId());
		parameters.put("jurusan", myJurusan == null || myJurusan.getId() == null ? -1L : myJurusan.getId());
		parameters.put("tahun_akademik",
				tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null ? "-1"
						: tahunAkademik.getSelectedItem().getValue());
		parameters.put("semester",
				genapGanjil.getSelectedItem() == null || genapGanjil.getSelectedItem().getValue() == null ? "-1"
						: genapGanjil.getSelectedItem().getValue());

		parameters.put("kelas",
				kelas.getAttribute("kelas") == null ? "-1" : ((Kelas) kelas.getAttribute("kelas")).getNama());
		List<Long> statusPertemuans = new ArrayList<Long>();
		for (MyCheckboxConfig config : listJenisPertemuan) {
			if (config.isChecked()) {
				StatusPertemuan statusPertemuan = (StatusPertemuan) config.getAttribute("statusPertemuan");
				statusPertemuans.add(statusPertemuan.getId());
			}
		}
		List<Map<String, Serializable>> maps = CommonReportHelper.generateParameterMapAbsensiRinciAsisten(myFakultas,
				myJurusan, kelas.getAttribute("kelas") == null ? null : ((Kelas) kelas.getAttribute("kelas")).getNama(),
				masaPerkuliahan, tahunAkademik.getSelectedItem().getValue().toString(),
				genapGanjil.getSelectedItem() == null || genapGanjil.getSelectedItem().getValue() == null ? null
						: genapGanjil.getSelectedItem().getValue().toString(),
				semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : null,
				ekstrakurikuler.isChecked() ? Perkuliahan.EKSTRA : null, (Dosen) dosen.getAttribute("dosen"),
				asisten.getValue().trim(), matakuliah.getValue().trim(), (Matakuliah) matkul.getAttribute("matakuliah"),
				mulai.getValue(), sampai.getValue(), hanyaYgStatusMasuk.isChecked(), statusPertemuans);
		parameters.put("maps", maps);

		return parameters;
	}

	@SuppressWarnings({})
	public void onCetak(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "LaporanAbsensiAsisten",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Kehadiran Asisten", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
