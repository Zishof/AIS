package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Space;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanKegiatanKemahasiswaan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private AmbilDataMahasiswaBanbox bandboxMahasiswa;
	private Center center;
	private Toolbar toolbar;
	private Combobox semesterMulai;
	private Combobox semesterSampai;

	public LaporanKegiatanKemahasiswaan() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kegiatan Kemahasiswaan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanKegiatanKemahasiswaan(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private MyDatebox tanggal;

	private void init() throws Exception {

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onKHS(event);

			}
		};

		EventListener eventListenerMahasiswa = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				aturPilihanSemester((Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa"));
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
		column.setWidth("25%");
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

		if (Common.getCurrentUser() != null && Common.getCurrentUser().getMahasiswa() != null) {
			Mahasiswa mahasiswa = Common.getCurrentUser().getMahasiswa();
			bandboxMahasiswa.setAttribute("mahasiswa", mahasiswa);
			bandboxMahasiswa.setAttribute("myValue", mahasiswa);
			bandboxMahasiswa.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
			bandboxMahasiswa.setId("mhs_" + mahasiswa.getId());
			bandboxMahasiswa.setDisabled(true);
		}

		bandboxMahasiswa.setEventListener(eventListenerMahasiswa);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);
		tanggal.setReadonly(true);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// row = new MyFormRow();
		//		// row.setParent(rows);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
					MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (!validasiSemester(true)) {
					return null;
				}

				final Map parameters = generateParameter((Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa"),
						tanggal.getValue(), ambilSemester(semesterMulai, 1), ambilSemester(semesterSampai, 1));
				return parameters;
			}
		}, "Angka_Kredit_Kegiatan_Mahasiswa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		semesterMulai = buatPilihanSemester();
		semesterSampai = buatPilihanSemester();
		aturPilihanSemester((Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa"));
		semesterMulai.addEventListener("onChange", eventListener);
		semesterSampai.addEventListener("onChange", eventListener);
		toolbar.insertBefore(new Label("Semester Mulai"), toolbar.getFirstChild());
		toolbar.insertBefore(semesterMulai, toolbar.getFirstChild().getNextSibling());
		toolbar.insertBefore(new Space(), semesterMulai.getNextSibling());
		toolbar.insertBefore(new Label("Semester Sampai"), semesterMulai.getNextSibling().getNextSibling());
		toolbar.insertBefore(semesterSampai, semesterMulai.getNextSibling().getNextSibling().getNextSibling());
		toolbar.insertBefore(new Space(), semesterSampai.getNextSibling());

		onKHS(null);

	}

	private Combobox buatPilihanSemester() {
		Combobox combobox = new Combobox();
		combobox.setReadonly(true);
		combobox.setWidth("105px");
		return combobox;
	}

	private void aturPilihanSemester(Mahasiswa mahasiswa) {
		int semesterSaatIni = 1;
		if (mahasiswa != null && mahasiswa.getSemesterSaatIni() != null) {
			semesterSaatIni = Math.max(1, mahasiswa.getSemesterSaatIni());
		}
		int semesterMaksimum = Math.max(16, semesterSaatIni);
		isiPilihanSemester(semesterMulai, semesterMaksimum, 1);
		isiPilihanSemester(semesterSampai, semesterMaksimum, semesterSaatIni);
	}

	private void isiPilihanSemester(Combobox combobox, int semesterMaksimum, int semesterTerpilih) {
		combobox.getItems().clear();
		for (int semester = 1; semester <= semesterMaksimum; semester++) {
			Comboitem item = new Comboitem("Semester " + semester);
			item.setValue(Integer.valueOf(semester));
			combobox.appendChild(item);
			if (semester == semesterTerpilih) {
				combobox.setSelectedItem(item);
			}
		}
	}

	private int ambilSemester(Combobox combobox, int nilaiDefault) {
		if (combobox != null && combobox.getSelectedItem() != null
				&& combobox.getSelectedItem().getValue() instanceof Integer) {
			return ((Integer) combobox.getSelectedItem().getValue()).intValue();
		}
		return nilaiDefault;
	}

	private boolean validasiSemester(boolean tampilkanPesan) throws InterruptedException {
		if (ambilSemester(semesterMulai, 1) <= ambilSemester(semesterSampai, 1)) {
			return true;
		}
		if (tampilkanPesan) {
			MyMessageboxConfig.show("Semester Mulai tidak boleh lebih besar dari Semester Sampai.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}
		return false;
	}

	@SuppressWarnings({ "rawtypes" })
	public static Map generateParameter(Mahasiswa mahasiswa, Date tanggal) throws Exception {
		int semesterSaatIni = mahasiswa == null || mahasiswa.getSemesterSaatIni() == null ? 1
				: Math.max(1, mahasiswa.getSemesterSaatIni());
		return generateParameter(mahasiswa, tanggal, 1, semesterSaatIni);
	}

	@SuppressWarnings({ "rawtypes" })
	public static Map generateParameter(Mahasiswa mahasiswa, Date tanggal, Integer semesterMulai,
			Integer semesterSampai) throws Exception {

		if (mahasiswa == null) {
			return null;
		}

		Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();

		mahasiswa.putPhoto(parameters);

		parameters.put("jurusan", (mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()));
		parameters.put("nama", (mahasiswa.getNama()));
		parameters.put("mahasiswa_id", mahasiswa.getId());
		parameters.put("tanggal", tanggal);
		parameters.put("semester_mulai_filter", semesterMulai == null ? Integer.valueOf(1) : semesterMulai);
		parameters.put("semester_sampai_filter", semesterSampai == null ? Integer.valueOf(1) : semesterSampai);
		parameters.put("tahun_angkatan", mahasiswa.getTahunangkatan());
		Integer semesterAwal = mahasiswa.getPindahKeKampusIniMasukSemester();
		parameters.put("semester_awal_mahasiswa",
				semesterAwal == null || semesterAwal.intValue() <= 0 ? Integer.valueOf(1) : semesterAwal);
		parameters.put("jenis_semester_masuk", mahasiswa.getSemesterMulai());

		String code = mahasiswa.getNama() + "\n" + mahasiswa.getNim() + "\n" + mahasiswa.getJurusan().getNama() + "\n"
				+ Common.dateFormat5.get().format(WaktuUtil.getDate());

		File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + mahasiswa.getId() + ".png");

		BarcodeCommon.generateCRCode(code, myfilebarcode1);
		parameters.put("cr_code", myfilebarcode1.getAbsolutePath());
		parameters.put("qr_code", Common.desEncrypter.get().encrypt(Mahasiswa.class.getName() + ":" + mahasiswa.getId()));
		System.out.println("parameters => " + parameters);
		code = parameters.get("qr_code") + "";
		myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + Common.randLong() + ".png");
		BarcodeCommon.generateCRCode(code, myfilebarcode1);
		parameters.put("qr_code_img", myfilebarcode1.getAbsolutePath());
		return parameters;
		
		
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		try {
			if (!validasiSemester(true)) {
				return;
			}

			File file = Report.generateFileReportWithProgress(Report.PDF,
					generateParameter((Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa"), tanggal.getValue(),
							ambilSemester(semesterMulai, 1), ambilSemester(semesterSampai, 1)),
					"Angka_Kredit_Kegiatan_Mahasiswa", ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Kegiatan Kemahasiswaan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
