package ais.action.report.format1.keuangan;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import ais.ui.util.MyGrid;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanTagihanMahasiswa extends MyWindow {

	private Center center;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Combobox tahunAkademik;
	private Combobox semester;
	private MyTextbox kelas;
	private Combobox fakultas;
	private Combobox jurusan;

	private Toolbar toolbar;

	public LaporanTagihanMahasiswa() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Tagihan Mahasiswa", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanTagihanMahasiswa(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	private void init() {

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		semester = new Combobox();
		kelas = new MyTextbox();

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

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas *"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi *"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(tahunAkademik = new Combobox());
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(semester);
		semester.setWidth("90%");

		for (Integer i = 1; i <= 20; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			semester.appendChild(comboitem);
		}

		semester.setSelectedIndex(0);
		semester.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas *"));
		row.appendChild(kelas);
		kelas.setWidth("90%");
		
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan Item Tagihan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				
			}
		});
		print.setParent(row);

//		row = new MyFormRow();
////		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelConfig(""));
//		 print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
//		print.addEventListener("onClick", new EventListener() {
//			@Override
//			public void onEvent(Event event) throws Exception {
//				onCetak(event);
//			}
//		});
//		print.setParent(row);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "laporan_tagihan", null, new EventListener() {

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
				? null : jurusan.getSelectedItem().getValue());
		Fakultas myFakultas = (Fakultas) (fakultas.getSelectedItem() == null
				|| fakultas.getSelectedItem().getValue() == null ? null : fakultas.getSelectedItem().getValue());

		if (myFakultas == null) {
			return null;
		}
		if (myJurusan == null) {
			return null;
		}
		if (kelas.getValue().trim().isEmpty()) {
			return null;
		}

		parameters.put("kelas", kelas.getValue().trim());
		parameters.put("fakultas", myFakultas == null || myFakultas.getId() == null ? -1L : myFakultas.getId());

		parameters.put("jurusan", myJurusan == null || myJurusan.getId() == null ? -1L : myJurusan.getId());
		parameters.put("fakultas_nama", myFakultas == null ? "" : myFakultas.getNama());
		parameters.put("jurusan_nama", myJurusan == null ? "" : myJurusan.getNama());
		parameters.put("tahun_akademik",
				tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null ? "-1"
						: tahunAkademik.getSelectedItem().getValue());
		parameters.put("semester", semester.getSelectedItem().getValue());

		String namFile = "Surat_Tagihan";

		String subReport = Common.ambilREAL_PATH_REPORT() + "/"
				+ (Common.getKonfigurasi("Report_" + namFile, "").getInfo1().isEmpty() ? namFile
						: Common.getKonfigurasi("Report_" + namFile, "").getInfo1());

		File jasper = new File(subReport);
		File fileJasper = CommonReport.generateFileJasper(jasper.getName(), namFile);

		subReport = fileJasper.getAbsolutePath();
		System.out.println("subReport = " + subReport);
		parameters.put("SUBREPORT_DIR_UJIAN", subReport);
		Integer semesterKe = (Integer) semester.getSelectedItem().getValue();

		Integer tahunAngkatan = Common.getTahunAngkatan(semesterKe,
				semesterKe % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
				tahunAkademik.getSelectedItem().getValue().toString());

		Session session = HibernateUtil.currentSession();
		List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.createAlias("jurusan", "jurusan")
				.add(Restrictions.eq("tahunangkatan", tahunAngkatan))
				.add(Restrictions.eq("jurusan.fakultas", myFakultas))
				.add(Restrictions.eq("jurusan", myJurusan))
				.add(Restrictions.ilike("kelas", kelas.getValue().trim(), MatchMode.ANYWHERE))
				.list();
		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
		

		return parameters;
	}

	@SuppressWarnings({})
	public void onCetak(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "laporan_tagihan", ais.ui.util.WaktuUtil.getDate(),
					toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Tagihan Mahasiswa", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
