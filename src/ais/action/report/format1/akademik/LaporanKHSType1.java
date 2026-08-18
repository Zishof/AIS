package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.hibernate.Session;
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
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.PenilaianMahasiswaHelper;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.JenjangProgramStudi;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Staff;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanKHSType1 extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;
	// private Combobox tahunAkademikUjianAkhirSemester;
	private Combobox semesterAbsensiUjian;
	private AmbilDataMahasiswaBanbox bandboxMahasiswa;
	private Center center;
	private Toolbar toolbar;
	private MyDatebox tanggal;

	public LaporanKHSType1() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan KHS Type1", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanKHSType1(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		// tahunAkademikUjianAkhirSemester = new Combobox();
		semesterAbsensiUjian = new Combobox();
		// if (tahunAkademikUjianAkhirSemester != null) {
		// tahunAkademikUjianAkhirSemester = Common
		// .generateTahunAjaran(tahunAkademikUjianAkhirSemester);
		for (int i = 1; i <= 21; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semesterAbsensiUjian.appendChild(comboitem);
		}
		Common.selectComboItem(semesterAbsensiUjian, 1);
		// }
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
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterAbsensiUjian);
		semesterAbsensiUjian.setWidth("90%");
		semesterAbsensiUjian.addEventListener("onChange", eventListener);

		row = new MyFormRow();
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
			// Mahasiswa sudah pasti (pengguna = mahasiswa) → Semester default = semester berjalannya.
			Common.selectComboItem(semesterAbsensiUjian, mahasiswa.currentSemester());
		}

		// Saat mahasiswa DIPILIH lewat banbox, Semester otomatis diisi = semester berjalan mahasiswa
		// tersebut (mahasiswa.currentSemester()); setelah itu tetap boleh diganti manual. Delegasikan
		// ke eventListener bersama agar perilaku lama (generate/preview) tetap sama.
		bandboxMahasiswa.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Mahasiswa mhsTerpilih = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
				if (mhsTerpilih != null) {
					Common.selectComboItem(semesterAbsensiUjian, mhsTerpilih.currentSemester());
				}
				eventListener.onEvent(arg0);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);

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

				// if (tahunAkademikUjianAkhirSemester.getSelectedItem()
				// ==
				// null) {
				// MyMessageboxConfig.show("Pilih salah satu tahun akademik",
				// "Peringatan", MyMessageboxConfig.OK,
				// MyMessageboxConfig.INFORMATION);
				// return null;
				// }
				if (semesterAbsensiUjian.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pilih ganjil atau genap", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
					MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Kartu_Hasil_Studi_type1", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		onKHS(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
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
		Integer semester = (Integer) semesterAbsensiUjian.getSelectedItem().getValue();
		if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa, semester)) {
			return null;
		}

		Session session = HibernateUtil.currentSession();
		Staff staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "prodi"))
				.setMaxResults(1).uniqueResult();

		JenjangProgramStudi jenjangProgramStudi = (JenjangProgramStudi) session
				.createCriteria(JenjangProgramStudi.class).add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
				.setMaxResults(1).uniqueResult();

		// String tahunAkademik = (String) tahunAkademikUjianAkhirSemester
		// .getSelectedItem().getValue();

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("semester", semester);
		// parameters.put("tahun_ajaran", tahunAkademik);
		parameters.put("pembantu_dekan", staffDekan == null ? "" : staffDekan.getNama());
		parameters.put("mahasiswa", mahasiswa.getId());
		// parameters.put("nip", staffDekan.getNip());
		parameters.put("tanggal", tanggal.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : tanggal.getValue());

		if (jenjangProgramStudi != null && jenjangProgramStudi.getNmKaPS() != null
				&& !jenjangProgramStudi.getNmKaPS().trim().equals("")) {
			parameters.put("kaprodi", jenjangProgramStudi == null ? "(                                          )"
					: jenjangProgramStudi.getNmKaPS());
			parameters.put("nip", jenjangProgramStudi == null ? "" : jenjangProgramStudi.getNidnKaPS());
		} else {
			Jurusan jurusan = mahasiswa.getJurusan();
			Dosen dosen = jurusan.getKaprodi();
			parameters.put("kaprodi", dosen == null ? "(                                          )" : dosen.getNama());
			parameters.put("nip", dosen == null ? "" : dosen.getCode());
		}

		Integer tahunAkademikMulai = Common.getTahunAkademik(semester, mahasiswa.getTahunangkatan(),
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

		String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

		parameters.put("bar", "2-" + tahunAkademik + "-" + semester + "-" + mahasiswa.getId());

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);
		parameters.put("nuptkosenpa", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNuptk());
		parameters.put("dosenpa", krsMahasiswa == null ? ""
				: krsMahasiswa.getDosenPa() == null ? "......................." : krsMahasiswa.getDosenPa().getNama());
		parameters.put("nipdosenpa",
				krsMahasiswa.getDosenPa() == null ? "......................."
						: (krsMahasiswa.getDosenPa().getCode().isEmpty() ? krsMahasiswa.getDosenPa().getNidn()
								: krsMahasiswa.getDosenPa().getCode()));

		Double ipmhs = krsMahasiswa.getIps();
		Double ipkmhs = krsMahasiswa.getIpk();

		Integer sksmhss = krsMahasiswa.getSksYangDiambil();
		Integer sksmhs = krsMahasiswa.getSksk();

		if (semester > 1) {
			Double iplast = Common.ipTerakhir(mahasiswa, semester);
			parameters.put("ip_sebelumnya", iplast);
		}
		parameters.put("ipk", ipkmhs);
		parameters.put("ips", ipmhs);
		parameters.put("sksk", sksmhs);
		parameters.put("sks", sksmhss);

		return parameters;
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Kartu_Hasil_Studi_type1",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan KHS Type1", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
