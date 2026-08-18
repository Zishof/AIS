package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
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
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Judisium;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Staff;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanTranskipAkademikWisuda extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1550813616089440767L;
	private MyDatebox tanggal;

	private AmbilDataMahasiswaBanbox bandboxMahasiswa;
	private MyCheckboxConfig ambilDariKurikulum;
	private Center center;
	private Toolbar toolbar;

	public LaporanTranskipAkademikWisuda() {
		super();
		try {
			initTranskripAkademikWisuda();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Transkip Akademik Wisuda", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanTranskipAkademikWisuda(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initTranskripAkademikWisuda();
		init();
	}

	private void initTranskripAkademikWisuda() throws Exception {
		tanggal = new MyDatebox();
		tanggal.setValue(ais.ui.util.WaktuUtil.getDate());

	}

	private void init() throws Exception {

		Common.initDefaultJudisium();

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onTranskrip(event);

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

		MyFormRow row = new MyFormRow();row.setValign("top");
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
		bandboxMahasiswa.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);

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
					MyMessageboxConfig.show("Mohon maaf, Mahasiswa belum dipilih. Langkah yang dapat dilakukan: (1) Ketik NIM atau nama mahasiswa pada kolom pencarian lalu pilih dari hasil yang muncul; (2) Pastikan data mahasiswa terdaftar di sistem; (3) Ulangi proses cetak transkrip akademik wisuda. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (tanggal.getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Tanggal belum diisi. Langkah yang dapat dilakukan: (1) Isi kolom Tanggal dengan tanggal cetak yang valid; (2) Pastikan format tanggal sesuai ketentuan sistem; (3) Ulangi proses cetak transkrip akademik wisuda. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				Map parameters = generateParameter();
				return parameters;
			}
		}, "Transkrip_Akademik_Mahasiswa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onTranskrip(arg0);

			}
		}));

		onTranskrip(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
			// MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (tanggal.getValue() == null) {
			// MyMessageboxConfig.show("Tanggal atau genap", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");

		if (mahasiswa != null) {
			int semester = mahasiswa.currentSemester();
			if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(mahasiswa, semester)) {
				return null;
			}
		}

		Session session = HibernateUtil.currentSession();
		Staff staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "dekan"))
				.setMaxResults(1).uniqueResult();

		Staff staffRektor = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "rektor"))
				.setMaxResults(1).uniqueResult();

		Date date = tanggal.getValue();

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("tanggal", date);
		parameters.put("rektor", staffRektor == null ? "" : staffRektor.getNama());
		parameters.put("dekan", staffDekan == null ? "" : staffDekan.getNama());
		parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? 1L : mahasiswa.getId());
		parameters.put("jurusan", mahasiswa.getJurusan().getNama());
		parameters.put("konsentrasi", mahasiswa.getKonsentrasi() == null ? "" : mahasiswa.getKonsentrasi().getNama());

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, mahasiswa.currentSemester(), null, null,
				true);

		Judisium judisium = Common.hitungJudisium(mahasiswa, krsMahasiswa);
		parameters.put("judisium", judisium == null ? "" : judisium.getNama());
		parameters.put("judisium_en", judisium == null ? "" : judisium.getNamaen());
		parameters.put("dosen_pa", krsMahasiswa.getDosenPa()==null?"":krsMahasiswa.getDosenPa().getNama());
		parameters.put("dosen_nidn", krsMahasiswa.getDosenPa()==null?"":krsMahasiswa.getDosenPa().getNidn());
		parameters.put("dosen_code", krsMahasiswa.getDosenPa()==null?"":krsMahasiswa.getDosenPa().getCode());
		parameters.put("dosen_nip", krsMahasiswa.getDosenPa()==null?"":krsMahasiswa.getDosenPa().getMycode());
		parameters.put("sks", krsMahasiswa.getSksk());
		parameters.put("semester", krsMahasiswa.getSemester());
		parameters.put("sksk", krsMahasiswa.getSksk());
		parameters.put("ipk", krsMahasiswa.getIpk());parameters.put("ipk_ceil", Math.ceil(krsMahasiswa.getIpk()));parameters.put("ipk_floor", Math.floor(krsMahasiswa.getIpk()));parameters.put("ipk_round", Math.round(krsMahasiswa.getIpk()));
		parameters.put("ipk_terbilang",
				IndonesianNumberToWords.convert(Common.numberFormat2.get().format(krsMahasiswa.getIpk())));

		parameters.put("mutu", mahasiswa.hitungMutu());

		String sem = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), sem,
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
		System.out.println("Cetak Semester IPK, semester < " + semester + "(" + mahasiswa.getNim() + ")");
		parameters.put("semester", semester);

		mahasiswa.putPhotoLulus(parameters); 

		parameters.put("ambilDariKurikulum", ambilDariKurikulum.isChecked());

		String subReport = Common.ambilREAL_PATH_REPORT() + "/";
		System.out.println("subReport = " + subReport);
		parameters.put("SUBREPORT_DIR", subReport);
		StreamingHibernateUtil.getInstance().closeSession();
		return parameters;
	}

	@SuppressWarnings({})
	public void onTranskrip(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Transkrip_Akademik_Mahasiswa",
					ais.ui.util.WaktuUtil.getDate(), toolbar);

			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Transkip Akademik Wisuda", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
