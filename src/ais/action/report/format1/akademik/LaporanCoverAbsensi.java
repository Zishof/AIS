package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.dao.DaoFactory;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class LaporanCoverAbsensi extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5809824888803449334L;
	private Combobox tahunAkademik;
	private Combobox semesterAbsensi;
	private Combobox perkuliahan;
	private Combobox fakultas;
	private Combobox prodi;
	private Center center;
	private Combobox program;
	private Textbox kelas;
	private Toolbar toolbar;

	class PerkuliahanEventListener implements EventListener {
		@Override
		public void onEvent(Event event) throws Exception {
			Common.clear(perkuliahan);
			perkuliahan.setSelectedItem(null);
			if (tahunAkademik.getSelectedItem() == null)
				return;
			if (semesterAbsensi.getSelectedItem() == null)
				return;
			if (fakultas.getSelectedItem() == null||fakultas.getSelectedItem().getValue() == null)
				return;
			if (prodi.getSelectedItem() == null)
				return;
			if (program.getSelectedItem() == null||program.getSelectedItem().getValue() == null)
				return;

			String myKelas = kelas.getValue();
			String myProgram = (String) (program.getSelectedItem() == null||program.getSelectedItem().getValue() == null ? ""
					: program.getSelectedItem().getLabel());

			List<Perkuliahan> items = DaoFactory
					.getInstance()
					.getPerkuliahanDao()
					.findByCriteria(
							Order.asc("waktuMulaiD"),
							myKelas.trim().equals("") ? Restrictions
									.sqlRestriction("1=1")
									: Restrictions.ilike("kelas", myKelas,
											MatchMode.ANYWHERE),

							myProgram.trim().equals("") ? Restrictions
									.sqlRestriction("1=1") : Restrictions
									.ilike("program", myProgram,
											MatchMode.ANYWHERE),

							Restrictions.eq("tahunAjaran", tahunAkademik
									.getSelectedItem().getValue()),
							Restrictions.eq("semester", semesterAbsensi
									.getSelectedItem().getValue()),
							CommonSearchFilterHelper.eqSelectedWithId("jurusan", prodi, false));

			if (items.size() == 0)
				return;
			for (Perkuliahan o : items) {
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel((o.getDosen1() == null ? "" : o.getDosen1()
						.getNama())
						+ " - "
						+ o.getMatakuliah().getNama()
						+ " (" + o.getId() + ")");
				comboitem.setValue(o);

				String deskripsi = "Smt: "
						+ (o.getSemester() + (o.getKelas() == null
								|| o.getKelas().equals("") ? "" : " "
								+ o.getKelas()))
						+ ", Ruang: "
						+ (o.getRuang() == null ? "" : o.getRuang()
								.getKodeRuangan())
						+ ", Hari: "
						+ o.getHari()
						+ ", Waktu: "
						+ o.getWaktuMulai()
						+ "-"
						+ o.getWaktuSelesai()
						+ ", Paralel: "
						+ (o.getMerupakan_paralel() == null
								|| !o.getMerupakan_paralel() ? "Bukan" : "Ya");

				comboitem.setDescription(deskripsi);

				perkuliahan.appendChild(comboitem);
			}

		}
	}

	private PerkuliahanEventListener perkuliahanEventListener = new PerkuliahanEventListener();

	public LaporanCoverAbsensi() {
		super();
		try {

			fakultas = new Combobox();
			prodi = new Combobox();
			Common.initFakultasDanJurusan(fakultas, prodi, null, null);

			program = Common.initPrograms(null);

			init();
			initPerkuliahan();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Cover Absensi", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanCoverAbsensi(String title, String border, boolean closable)
			throws Exception {
		super(title, border, closable);

		fakultas = new Combobox();
		prodi = new Combobox();
		program = Common.initPrograms(null);

		initPerkuliahan();
		init();
	}

	private void initPerkuliahan() {

		if (tahunAkademik != null) {
			System.out.println("tahun akademik");
			tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
			tahunAkademik
					.addEventListener("onChange", perkuliahanEventListener);
			for (int i = 1; i <= 21; i++) {
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(i + "");
				comboitem.setValue(i);
				semesterAbsensi.appendChild(comboitem);
			}
			Common.selectComboItem(semesterAbsensi, 1);
			semesterAbsensi.addEventListener("onChange",
					perkuliahanEventListener);
			prodi.addEventListener("onChange", perkuliahanEventListener);
			// dosen.addEventListener("onChange", eventListener);

		}

	}

	private void init() throws Exception {

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onLaporanCoverAbsensi(event);

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

		MyGrid grid = new MyGrid();grid.setWidth("100%");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(prodi);
		prodi.setWidth("90%");
		prodi.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterAbsensi = new Combobox());
		semesterAbsensi.setWidth("90%");
		semesterAbsensi.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");
		program.addEventListener("onChange", perkuliahanEventListener);
		program.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas = new Textbox());
		kelas.setWidth("90%");
		kelas.addEventListener("onChange", perkuliahanEventListener);
		kelas.addEventListener("onOK", perkuliahanEventListener);
		kelas.addEventListener("onChange", eventListener);
		kelas.addEventListener("onOK", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Perkuliahan"));
		row.appendChild(perkuliahan = new Combobox());
		perkuliahan.setWidth("90%");
		perkuliahan.addEventListener("onChange", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("Format Laporan"));
		// row.appendChild(reportType = CommonReport.generateReportType());
		// reportType.setWidth("90%");

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(
				new ParameterListener() {

					@SuppressWarnings({ "unchecked", "rawtypes" })
					@Override
					public Map<String, Serializable> generateParameters()
							throws Exception {
						Map parameters = generateParameter();
						return parameters;
					}
				}, "LaporanCoverAbsensi", null, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onLaporanCoverAbsensi(arg0);
					}
				}));

		onLaporanCoverAbsensi(null);

		try {
			perkuliahanEventListener.onEvent(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Cover Absensi", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		// if (this.perkuliahan.getSelectedItem() == null
		// && this.perkuliahan.getChildren().size() > 0) {
		// this.perkuliahan.setSelectedIndex(0);
		// }

		Perkuliahan perkuliahan = (Perkuliahan) (this.perkuliahan
				.getSelectedItem() == null ? null : this.perkuliahan
				.getSelectedItem().getValue());

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("perkuliahan",
				perkuliahan == null || perkuliahan.getId() == null ? -1 : perkuliahan.getId());

		return parameters;

	}

	@SuppressWarnings({ })
	public void onLaporanCoverAbsensi(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF,
					generateParameter(), "LaporanCoverAbsensi", ais.ui.util.WaktuUtil.getDate(),
					toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Cover Absensi", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
