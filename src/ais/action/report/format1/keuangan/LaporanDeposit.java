package ais.action.report.format1.keuangan;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataCalonMahasiswaBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataCalonSiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class LaporanDeposit extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;

	private Toolbar toolbar;
	private Intbox angkatanMulai;
	private Intbox angkatanSampai;
	private MyDatebox mulai;

	private MyDatebox sampai;

	private AmbilDataMahasiswaBanbox mahasiswa;

	private AmbilDataCalonMahasiswaBanbox calonMahasiswa;

	private AmbilDataSiswaBanbox siswa;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private AmbilDataCalonSiswaBanbox calonSiswa;

	private boolean pt;

	private boolean ya;

	public LaporanDeposit() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Deposit", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanDeposit(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() {

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onReport(event);

			}
		};

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("250px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("40%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		MyFormRow row = new MyFormRow();
		row.setVisible(pt);
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("(atau) Calon Mahasiswa"));
		row.appendChild(calonMahasiswa = new AmbilDataCalonMahasiswaBanbox());
		calonMahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Siswa"));
		row.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("(atau) Calon Siswa *"));
		row.appendChild(calonSiswa = new AmbilDataCalonSiswaBanbox());
		calonSiswa.setWidth("90%");

		EventListener a = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				mahasiswa.getParent().setVisible(false);
				calonMahasiswa.getParent().setVisible(false);

				siswa.getParent().setVisible(false);
				calonSiswa.getParent().setVisible(false);

				if (calonMahasiswa.getAttribute("calonMahasiswa") != null) {
					calonMahasiswa.getParent().setVisible(true);
				} else if (mahasiswa.getAttribute("mahasiswa") != null) {
					mahasiswa.getParent().setVisible(true);
				} else if (siswa.getAttribute("siswa") != null) {
					siswa.getParent().setVisible(true);
				} else if (calonSiswa.getAttribute("calonSiswa") != null) {
					calonSiswa.getParent().setVisible(true);
				} else {
					mahasiswa.getParent().setVisible(pt);
					calonMahasiswa.getParent().setVisible(pt);

					siswa.getParent().setVisible(ya);
					calonSiswa.getParent().setVisible(ya);
				}

			}
		};

		mahasiswa.setEventListener(a);
		calonMahasiswa.setEventListener(a);
		siswa.setEventListener(a);
		calonSiswa.setEventListener(a);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Periode Mulai"));
		row.appendChild(mulai = new MyDatebox());
		mulai.setFormat(Common.dateFormat1.get().toPattern());
		mulai.setReadonly(true);
		// mulai.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Periode Sampai"));
		row.appendChild(sampai = new MyDatebox());
		sampai.setFormat(Common.dateFormat1.get().toPattern());
		sampai.setReadonly(true);
		// sampai.addEventListener("onChange", eventListener);

		int tahun = Calendar.getInstance().get(Calendar.YEAR);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan Mulai"));
		row.appendChild(angkatanMulai = new Intbox(tahun - 5));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan Sampai"));
		row.appendChild(angkatanSampai = new Intbox(tahun));

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas = new Combobox());
		searchfakultas.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jurusan"));
		row.appendChild(searchjurusan = new Combobox());
		searchjurusan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(searchyayasan = new Combobox());
		searchyayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah = new Combobox());
		searchsekolah.setWidth("90%");

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, false, false);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Button tampilkan;
		row.appendChild(tampilkan = new Button("Tampilkan"));
		tampilkan.addEventListener("onClick", eventListener);

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
		}, "deposit", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			onReport(null);
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Sekolah sekolah = (Sekolah) (searchsekolah == null || searchsekolah.getSelectedItem() == null ? null
				: searchsekolah.getSelectedItem().getValue());
		Yayasan yayasan = (Yayasan) (searchsekolah == null || searchyayasan.getSelectedItem() == null ? null
				: searchyayasan.getSelectedItem().getValue());

		Jurusan jurusan = (Jurusan) (searchjurusan == null || searchjurusan.getSelectedItem() == null ? null
				: searchjurusan.getSelectedItem().getValue());
		Fakultas fakultas = (Fakultas) (searchfakultas == null || searchfakultas.getSelectedItem() == null ? null
				: searchfakultas.getSelectedItem().getValue());

		if (fakultas != null || jurusan != null || !ya) {
			sekolah = null;
			yayasan = null;
		}

		Date tglMulai = mulai.getValue();
		Date tglSampai = sampai.getValue();

		Mahasiswa mahasiswa = (Mahasiswa) this.mahasiswa.getAttribute("mahasiswa");
		BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) this.calonMahasiswa
				.getAttribute("calonMahasiswa");
		Siswa siswa = (Siswa) this.siswa.getAttribute("siswa");
		CalonSiswa calonSiswa = (CalonSiswa) this.calonSiswa.getAttribute("calonSiswa");

		Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("sekolah", sekolah == null || sekolah.getId() == null ? -1L : sekolah.getId());
		parameters.put("yayasan", yayasan == null || yayasan.getId() == null ? -1L : yayasan.getId());
		parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());
		parameters.put("fakultas", fakultas == null || fakultas.getId() == null ? -1L : fakultas.getId());

		parameters.put("calon_mahasiswa", biodataCalonMahasiswa == null || biodataCalonMahasiswa.getId() == null ? -1L : biodataCalonMahasiswa.getId());
		parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? -1L : mahasiswa.getId());

		parameters.put("siswa", siswa == null || siswa.getId() == null ? -1L : siswa.getId());
		parameters.put("calon_siswa", calonSiswa == null || calonSiswa.getId() == null ? -1L : calonSiswa.getId());

		parameters.put("mulai", tglMulai == null ? "" : Common.databaseDateFormat.get().format(tglMulai));
		parameters.put("sampai", tglSampai == null ? "" : Common.databaseDateFormat.get().format(tglSampai));
		parameters.put("mulai_1", tglMulai == null ? "" : Common.dateFormat5.get().format(tglMulai));
		parameters.put("sampai_1", tglSampai == null ? "" : Common.dateFormat5.get().format(tglSampai));

		parameters.put("angkatan_mulai", angkatanMulai.getValue() == null ? 0 : angkatanMulai.getValue());
		parameters.put("angkatan_sampai", angkatanSampai.getValue() == null ? 2100 : angkatanSampai.getValue());

		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "deposit",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Deposit", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
