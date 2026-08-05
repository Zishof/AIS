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

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanAbsensiMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox tahunAkademik;
	private Combobox semester;
	private AmbilDataDosenBanbox dosen;

	private Center center;

	private Toolbar toolbar;

	public LaporanAbsensiMahasiswa() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Absensi Mahasiswa", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAbsensiMahasiswa(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		semester = new Combobox();
		for (int i = 1; i <= 21; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semester.appendChild(comboitem);
		}
		Common.selectComboItem(semester, 1);

	}

	private void init() throws Exception {

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onKHS(event);

			}
		};

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
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setWidth("90%");
		dosen.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semester);
		semester.setWidth("90%");
		semester.addEventListener("onChange", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Fakultas belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Fakultas dari daftar dropdown; (2) Pastikan data Fakultas sudah dimuat di sistem; (3) Ulangi proses cetak laporan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return null;
				}
				if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Jurusan/Program Studi belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Jurusan dari dropdown setelah memilih Fakultas; (2) Pastikan data Jurusan tersedia di sistem; (3) Ulangi proses cetak laporan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return null;
				}

				if (tahunAkademik.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, Tahun Akademik belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Tahun Akademik dari daftar dropdown; (2) Pastikan data Tahun Akademik sudah dikonfigurasi di sistem; (3) Ulangi proses cetak laporan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return null;
				}
				if (dosen.getAttribute("dosen") == null) {
					MyMessageboxConfig.show("Mohon maaf, Dosen belum dipilih. Langkah yang dapat dilakukan: (1) Ketik nama atau NIP dosen pada kolom pencarian lalu pilih dari hasil yang muncul; (2) Pastikan data dosen terdaftar di sistem; (3) Ulangi proses cetak laporan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return null;
				}

				if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Semester belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Semester (Ganjil/Genap) dari daftar dropdown; (2) Pastikan data Semester tersedia sesuai Tahun Akademik yang dipilih; (3) Ulangi proses cetak laporan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Laporan_Absensi_Mahasiswa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		onKHS(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
			return null;
		}
		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			return null;
		}

		if (tahunAkademik.getSelectedItem() == null) {
			return null;
		}
		if (dosen.getAttribute("dosen") == null) {
			return null;
		}
		if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
			return null;
		}

		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());

		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());

		Dosen dosen = (Dosen) this.dosen.getAttribute("dosen");

		String tahunAkademik = (String) this.tahunAkademik.getSelectedItem().getValue();
		Integer semester = (Integer) this.semester.getSelectedItem().getValue();

		final Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("dosen", dosen == null || dosen.getId() == null ? -1L : dosen.getId());
		parameters.put("tahun_akademik", tahunAkademik);
		parameters.put("semester", semester);
		parameters.put("fakultas", fakultas == null || fakultas.getId() == null ? -1L : fakultas.getId());
		parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());

		parameters.put("kaprodi", "(                                          )");
		parameters.put("nip", "");

		return parameters;
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Laporan_Absensi_Mahasiswa",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Absensi Mahasiswa", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
