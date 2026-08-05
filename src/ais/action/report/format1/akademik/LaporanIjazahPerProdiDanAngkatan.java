package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Sessions;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataKelasBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoMahasiswaLulus;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanIjazahPerProdiDanAngkatan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Combobox fakultas;
	private Combobox jurusan;
	private AmbilDataKelasBanbox kelas;
	private Textbox nims;
	private Intbox angkatan;

	private Center center;
	private Toolbar toolbar;

	private MyDatebox tanggal;

	public LaporanIjazahPerProdiDanAngkatan() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Ijazah Per Prodi Dan Angkatan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanIjazahPerProdiDanAngkatan(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

		angkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));

	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

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
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas" + " *"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		// fakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Jurusan") + " *"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		// jurusan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan *"));
		row.appendChild(angkatan);
		angkatan.setWidth("90%");
		// angkatan.addEventListener("onChange", eventListener);
		// angkatan.addEventListener(Events.ON_OK, eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas = new AmbilDataKelasBanbox());
		kelas.setWidth("90%");
		kelas.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nims = new Textbox());
		nims.setWidth("90%");
		nims.setRows(5);

		Common.initKeterangan(rows,
				"Jika Ijazah ini khusus nim tertentu, masukkan nim yang pisah menggunakan tanda koma (,). Misal : 1209902323,1209902324,1209902325");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.setReadonly(true);

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
					MyMessageboxConfig.show("Pilih " + "Fakultas", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Pilih " + Common.getBahasaConfig("Jurusan"), "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (angkatan.getValue() == null) {
					MyMessageboxConfig.show("Pilih angkatan", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Ijazah_Per_Prodi", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		// onKHS(null);
		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		MyButtonConfig button = new MyButtonConfig("Tampilkan Laporan");
		button.setParent(row);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Pilih " + "Fakultas", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}

		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Pilih " + Common.getBahasaConfig("Jurusan"), "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}

		if (angkatan.getValue() == null) {
			MyMessageboxConfig.show("Pilih angkatan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}

		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());

		List<String> nims = new ArrayList<String>();
		for (String nim : this.nims.getValue().trim().split(",")) {
			if (!nim.trim().isEmpty()) {
				nims.add(nim.trim());
			}
		}

		String kel = kelas.getValue().trim();

		List<Mahasiswa> mahasiswas = HibernateUtil.currentSession().createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("jurusan", jurusan))
				.add(nims.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("nim", nims))
				.add(Restrictions.eq("tahunangkatan", angkatan.getValue()))
				.add(kel != null && !kel.trim().isEmpty() ? Restrictions.ilike("kelas", kel.trim(), MatchMode.EXACT)
						: Restrictions.sqlRestriction("true"))
				.list();

		if (nims.isEmpty()) {
			nims.add("-1");
		}

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("nims", nims.toArray());
		parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());
		parameters.put("tahunangkatan", angkatan.getValue() == null ? -1 : angkatan.getValue());
		parameters.put("tanggal", tanggal.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : tanggal.getValue());
		parameters.put("kaprodi", "(                                          )");
		parameters.put("nip", "");
		parameters.put("kelas", kelas.getValue().trim());

		String subReport = Common.ambilREAL_PATH_REPORT() + "/" + Common.getKonfigurasi("Report_Ijazah", "").getInfo1();

		File jasper = new File(subReport);
		File fileJasper = CommonReport.generateFileJasper(jasper.getName(), "Ijazah");

		subReport = fileJasper.getAbsolutePath();
		System.out.println("subReport = " + subReport);
		parameters.put("SUBREPORT_DIR_KHS", subReport);

		Session session = HibernateUtil.currentSession();
		for (Mahasiswa mahasiswa : mahasiswas) {
			BiodataMahasiswa biodataMahasiswa = (BiodataMahasiswa) session.createCriteria(BiodataMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();

			parameters.put("dekan_" + mahasiswa.getId(),
					mahasiswa.getJurusan().getFakultas().getDekan() == null ? "....................."
							: mahasiswa.getJurusan().getFakultas().getDekan().getNama());
			parameters.put("nip_dekan_" + mahasiswa.getId(),
					mahasiswa.getJurusan().getFakultas().getDekan() == null ? "....................."
							: mahasiswa.getJurusan().getFakultas().getDekan().getCode());
			parameters.put("nidn_dekan_" + mahasiswa.getId(),
					mahasiswa.getJurusan().getFakultas().getDekan() == null ? "....................."
							: mahasiswa.getJurusan().getFakultas().getDekan().getNidn());

			parameters.put("pudek1_" + mahasiswa.getId(),
					mahasiswa.getJurusan().getFakultas().getPudek1() == null ? "....................."
							: mahasiswa.getJurusan().getFakultas().getPudek1().getNama());
			parameters.put("nip_pudek1_" + mahasiswa.getId(),
					mahasiswa.getJurusan().getFakultas().getPudek1() == null ? "....................."
							: mahasiswa.getJurusan().getFakultas().getPudek1().getCode());
			parameters.put("nidn_pudek1_" + mahasiswa.getId(),
					mahasiswa.getJurusan().getFakultas().getPudek1() == null ? "....................."
							: mahasiswa.getJurusan().getFakultas().getPudek1().getNidn());

			parameters.put("mahasiswa_" + mahasiswa.getId(), mahasiswa == null || mahasiswa.getId() == null ? 1L : mahasiswa.getId());

			parameters.put("jurusan_" + mahasiswa.getId(), mahasiswa.getJurusan().getNama());
			parameters.put("konsentrasi_" + mahasiswa.getId(),
					mahasiswa.getKonsentrasi() == null ? "" : mahasiswa.getKonsentrasi().getNama());

			String nama = biodataMahasiswa == null || biodataMahasiswa.getNamaUntukIjazah() == null
					|| biodataMahasiswa.getNamaUntukIjazah().trim().isEmpty() ? mahasiswa.getNama()
							: biodataMahasiswa.getNamaUntukIjazah();

			parameters.put("akta_perguruan_tinggi_" + mahasiswa.getId(),
					mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNomorAkta());

			parameters.put("akreditasi_perguruan_tinggi_" + mahasiswa.getId(),
					mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAkreditasi());

			parameters.put("nama_" + mahasiswa.getId(), nama);

			parameters.put("nama_cap_" + mahasiswa.getId(), Common.capitailizeWord(nama));

			parameters.put("nama_asli_" + mahasiswa.getId(), mahasiswa.getNama());
			parameters.put("tempat_cap_" + mahasiswa.getId(), Common.capitailizeWord(mahasiswa.getTempatlahir()));
			parameters.put("tempat_" + mahasiswa.getId(), mahasiswa.getTempatlahir());
			parameters.put("tanggal_lahir_" + mahasiswa.getId(), mahasiswa.getTanggallahirManual());
			parameters.put("nim_" + mahasiswa.getId(), mahasiswa.getNim());
			parameters.put("jenjang_" + mahasiswa.getId(), mahasiswa.getJenjang().getKeterangan());
			parameters.put("jenjang_en_" + mahasiswa.getId(), mahasiswa.getJenjang().getKeteranganEn());
			parameters.put("tanggal_lulus_" + mahasiswa.getId(), mahasiswa.getTanggalLulus() == null ? "..........."
					: Common.dateFormat2.get().format(mahasiswa.getTanggalLulus()));
			parameters.put("tanggal_lulus_en_" + mahasiswa.getId(), mahasiswa.getTanggalLulus() == null ? "..........."
					: Common.dateFormat2En.get().format(mahasiswa.getTanggalLulus()));

			if (mahasiswa.getTanggalLulus() == null) {
				parameters.put("tanggal_satuan_lulus_" + mahasiswa.getId(), "..");
				parameters.put("bulan_satuan_lulus_" + mahasiswa.getId(), ".....");
				parameters.put("tahun_satuan_lulus_" + mahasiswa.getId(), "....");

				parameters.put("tanggal_satuan_lulus_en_" + mahasiswa.getId(), "..");
				parameters.put("bulan_satuan_lulus_en_" + mahasiswa.getId(), ".....");
				parameters.put("tahun_satuan_lulus_en_" + mahasiswa.getId(), "....");
			} else {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(mahasiswa.getTanggalLulus());
				int tanggal = calendar.get(Calendar.DATE);
				int tahun = calendar.get(Calendar.YEAR);

				parameters.put("tanggal_satuan_lulus_" + mahasiswa.getId(), tanggal);
				parameters.put("bulan_satuan_lulus_" + mahasiswa.getId(), mahasiswa.getTanggalLulus() == null ? ""
						: Common.monthFormat2.get().format(mahasiswa.getTanggalLulus()));
				parameters.put("tahun_satuan_lulus_" + mahasiswa.getId(), tahun);

				// parameters.put("tanggal_satuan_lulus_en_" +
				// mahasiswa.getId(), tanggal==1? );
				parameters.put("bulan_satuan_lulus_en_" + mahasiswa.getId(), mahasiswa.getTanggalLulus() == null ? ""
						: Common.monthFormat2En.get().format(mahasiswa.getTanggalLulus()));
				parameters.put("tahun_satuan_lulus_en_" + mahasiswa.getId(), tahun);
			}

			parameters.put("jurusan_" + mahasiswa.getId(), mahasiswa.getJurusan().getNama());
			parameters.put("jurusan_en_" + mahasiswa.getId(), mahasiswa.getJurusan().getNamaEn());
			parameters.put("sk_akreditasi_" + mahasiswa.getId(), mahasiswa.getJurusan().getNoSkAkreditasi());
			parameters.put("fakultas_" + mahasiswa.getId(), mahasiswa.getJurusan().getFakultas().getNama());
			parameters.put("jurusan_en_" + mahasiswa.getId(), mahasiswa.getJurusan().getNamaEn());
			parameters.put("gelar_" + mahasiswa.getId(), mahasiswa.getJurusan().getGelar());
			parameters.put("gelar_singkat_" + mahasiswa.getId(), mahasiswa.getJurusan().getSingkatanGelar());

			parameters.put("no_ijazah_1_" + mahasiswa.getId(), mahasiswa.getNoIjazah1());
			parameters.put("no_ijazah_2_" + mahasiswa.getId(), mahasiswa.getNoIjazah2());
			parameters.put("no_akta_1_" + mahasiswa.getId(), mahasiswa.getNoAkta1());
			parameters.put("no_akta_2_" + mahasiswa.getId(), mahasiswa.getNoAkta2());
			parameters.put("gelar_en_" + mahasiswa.getId(), mahasiswa.getJurusan().getGelarEn());
			parameters.put("gelar_en_singkat_" + mahasiswa.getId(), mahasiswa.getJurusan().getSingkatanGelarEn());

			if (mahasiswa.getTanggalYudisium() == null) {
				parameters.put("tanggal_judisium", ".......................");
				parameters.put("tanggal_judisium_angka", ".......................");

			} else {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(mahasiswa.getTanggalYudisium());
				int tanggal = calendar.get(Calendar.DATE);
				int bulan = calendar.get(Calendar.MONTH);
				int tahun = calendar.get(Calendar.YEAR);

				String tgl = IndonesianNumberToWords.convert((long) tanggal);
				String bln = Common.BULAN[bulan];
				String thn = IndonesianNumberToWords.convert((long) tahun);
				parameters.put("tanggal_judisium", tgl + " " + bln + " " + thn);
				parameters.put("tanggal_judisium_angka", mahasiswa.getTanggalYudisium());
			}

			if (mahasiswa.getTanggalWisuda() == null) {
				parameters.put("tanggal_wisuda", ".......................");
				parameters.put("tanggal_wisuda_angka", ".......................");

			} else {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(mahasiswa.getTanggalWisuda());
				int tanggal = calendar.get(Calendar.DATE);
				int bulan = calendar.get(Calendar.MONTH);
				int tahun = calendar.get(Calendar.YEAR);

				String tgl = IndonesianNumberToWords.convert((long) tanggal);
				String bln = Common.BULAN[bulan];
				String thn = IndonesianNumberToWords.convert((long) tahun);
				parameters.put("tanggal_wisuda", tgl + " " + bln + " " + thn);
				parameters.put("tanggal_wisuda_angka", mahasiswa.getTanggalWisuda());
			}

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
			parameters.put("sks_" + mahasiswa.getId(), krsMahasiswa.getSksk());
			parameters.put("sksk_" + mahasiswa.getId(), krsMahasiswa.getSksk());
			parameters.put("ipk_" + mahasiswa.getId(), krsMahasiswa.getIpk());

			Judisium judisium = Common.hitungJudisium(mahasiswa, krsMahasiswa);
			parameters.put("judisium_" + mahasiswa.getId(), judisium == null ? "" : judisium.getNama());
			parameters.put("judisium_en_" + mahasiswa.getId(), judisium == null ? "" : judisium.getNamaen());

			String myfile = CommonMedia.loadPathFileFotoLangsung(new Tbmuser(mahasiswa));
			parameters.put("foto_" + mahasiswa.getId(), myfile);

			parameters.put("foto_lulus_" + mahasiswa.getId(), myfile);

			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			try {
				FotoMahasiswaLulus fotoMahasiswaLulus = (FotoMahasiswaLulus) streamingSession
						.createCriteria(FotoMahasiswaLulus.class).add(Restrictions.eq("mahasiswa", mahasiswa.getId()))
						.setMaxResults(1).uniqueResult();
				parameters.put("foto_lulus_" + mahasiswa.getId(),
						fotoMahasiswaLulus == null ? myfile : fotoMahasiswaLulus.createLinkUri());
			} catch (Exception e) {
				parameters.put("foto_lulus_" + mahasiswa.getId(), myfile);
				Common.tampilErrorJikaAdmin(e);
				StreamingHibernateUtil.getInstance().rollbackTransaction();
			}
			StreamingHibernateUtil.getInstance().closeSession();

			String code = mahasiswa.getLinkValidasiEksternal().isEmpty() ? mahasiswa.getNim()
					: mahasiswa.getLinkValidasiEksternal();

			File myfilebarcode1 = new File(
					Common.ambilREAL_PATH_REPORT() + "/crcode_" + mahasiswa.getId() + ".png");

			BarcodeCommon.generateCRCode(code, myfilebarcode1);
			parameters.put("cr_code_" + mahasiswa.getId(), myfilebarcode1.getAbsolutePath());

			parameters.put("akreditasi_institusi_" + mahasiswa.getId(),
					mahasiswa.getJurusan().getFakultas() == null
							|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNoSkAkreditasi());
			parameters.put("akreditasi_prodi_" + mahasiswa.getId(),
					mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNoSkAkreditasi());

			parameters.put("nilai_akreditasi_institusi",
					mahasiswa.getJurusan().getFakultas() == null
							|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAkreditasi());

			parameters.put("peringkat_akreditasi_institusi", mahasiswa.getJurusan().getFakultas() == null
					|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
							: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getPeringkatAkreditasi());

			parameters.put("nilai_akreditasi_prodi",
					mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getAkreditasi());
			parameters.put("peringkat_akreditasi_prodi",
					mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getPeringkatAkreditasi());

			parameters.put("nik_" + mahasiswa.getId(),
					biodataMahasiswa == null ? "" : biodataMahasiswa.getNoIdentitas());
			parameters.put("noIdentitas_" + mahasiswa.getId(),
					biodataMahasiswa == null ? "" : biodataMahasiswa.getNoIdentitas());
			parameters.put("rektor_" + mahasiswa.getId(),
					mahasiswa.getJurusan().getFakultas() == null
							|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getRektor());
			parameters.put("tanggal_sk_" + mahasiswa.getId(), mahasiswa.getTanggalSkRektor() == null ? "..........."
					: Common.dateFormat2.get().format(mahasiswa.getTanggalSkRektor()));

		}

		subReport = Common.ambilREAL_PATH_REPORT() + "/";
		System.out.println("subReport = " + subReport);
		parameters.put("SUBREPORT_DIR", subReport);

		return parameters;
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Ijazah_Per_Prodi",
							ais.ui.util.WaktuUtil.getDate(), toolbar);
					CommonReport.tampilkanReportPDF(center, file);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Ijazah Per Prodi Dan Angkatan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
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
