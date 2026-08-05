package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
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

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyWindow;

public class LaporanRekamanNilaiPerProdiDanAngkatan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Combobox fakultas;
	private Combobox jurusan;
//	private Combobox semester;
	private Intbox angkatan;
	private MyTextbox kelas;
	private Center center;

	private Toolbar toolbar;

	private MyDatebox tanggal;

	public LaporanRekamanNilaiPerProdiDanAngkatan() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekaman Nilai Per Prodi Dan Angkatan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekamanNilaiPerProdiDanAngkatan(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

//		semester = new Combobox();
//		for (int i = 1; i <= 21; i++) {
//			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
//			comboitem.setLabel(i + "");
//			comboitem.setValue(i);
//			semester.appendChild(comboitem);
//		}
//		Common.selectComboItem(semester, 1);

		angkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));

	}

	private void init() throws Exception {

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onKHS(event);

			}
		};

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		// FIX tinggi-pasti: kelas ini SELALU dipakai sebagai sub-tab (di-embed) di
		// LaporanTranskipAkademik. Rantai height:100% tanpa leluhur ber-tinggi PASTI membuat
		// Borderlayout KOLAPS 0px → konten tab blank. Tinggi pasti; gulir ditangani MyTabpanel.
		borderlayout.setHeight("2000px");

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas *"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi  *"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.addEventListener("onChange", eventListener);

//		row = new MyFormRow();
//		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelConfig("Semester  *"));
//		row.appendChild(semester);
//		semester.setWidth("90%");
//		semester.addEventListener("onChange", eventListener);
//		semester.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan *"));
		row.appendChild(angkatan);
		angkatan.setWidth("90%");
		angkatan.addEventListener("onChange", eventListener);
		angkatan.addEventListener(Events.ON_OK, eventListener);

		kelas = new MyTextbox();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas);
		kelas.setWidth("90%");
		kelas.addEventListener("onChange", eventListener);

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

//				if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
//					MyMessageboxConfig.show("Pilih semester", "Peringatan", MyMessageboxConfig.OK,
//							MyMessageboxConfig.INFORMATION);
//					return null;
//				}

				if (angkatan.getValue() == null) {
					MyMessageboxConfig.show("Pilih angkatan", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Rekaman_Nilai_Per_Prodi_Angkatan", null, new EventListener() {

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
			// MyMessageboxConfig.show("Pilih " + "Fakultas",
			// "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return null;
		}

		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			// MyMessageboxConfig.show("Pilih " + Common.getBahasaConfig("Jurusan"),
			// "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return null;
		}

//		if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
//			// MyMessageboxConfig.show("Pilih semester", "Peringatan",
//			// MyMessageboxConfig.OK,
//			// MyMessageboxConfig.INFORMATION);
//			return null;
//		}

		if (angkatan.getValue() == null) {
			// MyMessageboxConfig.show("Pilih angkatan", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
				? null
				: jurusan.getSelectedItem().getValue());
		Fakultas myFakultas = (Fakultas) (fakultas.getSelectedItem() == null
				|| fakultas.getSelectedItem().getValue() == null ? null : fakultas.getSelectedItem().getValue());

		Integer ang = angkatan.getValue() == null ? -1 : angkatan.getValue();
		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("jurusan", myJurusan == null || myJurusan.getId() == null ? -1L : myJurusan.getId());
//		parameters.put("semester", semester.getSelectedItem().getValue());
		parameters.put("tahunangkatan", ang);
		parameters.put("tanggal", tanggal.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : tanggal.getValue());
		parameters.put("kaprodi", "(                                          )");
		parameters.put("nip", "");

		List<Map> maps = new ArrayList<Map>();
		String ta = Common.getCurrentTahunAkademik();
		List<Long> dataMhs = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.property("id"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		for (Long generalValueObjectid : dataMhs) {
			Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), generalValueObjectid);

			Skripsi skripsi = (Skripsi) HibernateUtil.currentSession().createCriteria(Skripsi.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.desc("id")).setMaxResults(1)
					.uniqueResult();
			if (skripsi != null) {
				Common.insertProperty(Skripsi.class, skripsi, parameters, "skripsi_" + mahasiswa.getId(), 1,
						"mahasiswa");
			}

			if (ang.equals(-1) || ang.equals(mahasiswa.getTahunangkatan())) {
				if (kelas.getValue().trim().isEmpty()
						|| kelas.getValue().trim().equalsIgnoreCase(mahasiswa.getKelas())) {

					if (myJurusan == null || (mahasiswa.getJurusan() != null
							&& mahasiswa.getJurusan().getId().equals(myJurusan.getId()))) {

						if (myFakultas == null || (mahasiswa.getJurusan() != null
								&& mahasiswa.getJurusan().getFakultas().getId().equals(myFakultas.getId()))) {

							Integer smtMha = Common.getSemester(mahasiswa.getTahunangkatan(), ta,
									!Common.isNowSemensterGanjil() ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
									mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

							Map map = new java.util.HashMap();
							map.put("mahasiswa", mahasiswa.getId());
							maps.add(map);

							System.out.println("mhs = " + mahasiswa);

							KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smtMha, null, null);
							parameters.put("sks_" + mahasiswa.getId(), krsMahasiswa.getSksk());
							parameters.put("semester_" + mahasiswa.getId(), krsMahasiswa.getSemester());
							parameters.put("sksk_" + mahasiswa.getId(), krsMahasiswa.getSksk());
							parameters.put("ipk_" + mahasiswa.getId(), krsMahasiswa.getIpk());
							parameters.put("ipk_terbilang_" + mahasiswa.getId(), IndonesianNumberToWords
									.convert(Common.numberFormat2.get().format(krsMahasiswa.getIpk())));
							parameters.put("ip_" + mahasiswa.getId(), krsMahasiswa.getIps());
							parameters.put("mutu_" + mahasiswa.getId(), mahasiswa.hitungMutu());
							Judisium judisium = Common.hitungJudisium(mahasiswa, krsMahasiswa);
							parameters.put("judisium_" + mahasiswa.getId(), judisium == null ? "" : judisium.getNama());
							parameters.put("judisium_en_" + mahasiswa.getId(),
									judisium == null ? "" : judisium.getNamaen());

							parameters.put("judul_" + mahasiswa.getId(), mahasiswa.getJudulSkripsi());

							List<Long> detailsperkuliahans = new ArrayList<Long>();
							for (Long detailperkuliahan : mahasiswa
									.saringBerdasarNilaiDan0(mahasiswa.ambilDetailperkuliahan())) {
								detailsperkuliahans.add(detailperkuliahan);
							}
							parameters.put("jumlah_mk_" + mahasiswa.getId(), detailsperkuliahans.size());
							if (detailsperkuliahans.isEmpty()) {
								detailsperkuliahans.add(-1L);
							}
							parameters.put("detailsperkuliahans_" + mahasiswa.getId(), detailsperkuliahans.toArray());

						}

					}

				}
			}
		}

		parameters.put("maps", maps);

		String namFile = "Rekaman_Nilai";

		String subReport = Common.ambilREAL_PATH_REPORT() + "/"
				+ (Common.getKonfigurasi("Report_" + namFile, "").getInfo1().isEmpty() ? namFile
						: Common.getKonfigurasi("Report_" + namFile, "").getInfo1());

		File jasper = new File(subReport);
		File fileJasper = CommonReport.generateFileJasper(jasper.getName(), namFile);

		subReport = fileJasper.getAbsolutePath();
		System.out.println("subReport = " + subReport);
		parameters.put("SUBREPORT_DIR_CUSTOM", subReport);

		return parameters;
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Rekaman_Nilai_Per_Prodi_Angkatan",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekaman Nilai Per Prodi Dan Angkatan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
