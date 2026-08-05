package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Sessions;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Staff;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRuanganDosen extends MyWindow {

	// /**
	// * S
	// *
	// */
	private static final long serialVersionUID = 7505425767536099261L;
	private Combobox fakultas;
	private Combobox jurusan;
	private Textbox dosen;

	private Center center;
	private Toolbar toolbar;

	public LaporanRuanganDosen() {
		super();
		try {

			initDaftarHadirDosen();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Ruangan Dosen", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRuanganDosen(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initDaftarHadirDosen();
		init();
	}

	private void initDaftarHadirDosen() throws Exception {

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

	}

	private void init() throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(dosen = new Textbox());
		dosen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onDaftarHadirDosen(null);
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
		}, "Ruangan_Dosen", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onDaftarHadirDosen(arg0);

			}
		}));

		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Session session = HibernateUtil.currentSession();
		Staff staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "dekan"))
				.setMaxResults(1).uniqueResult();

		Staff staffPembantuDekan = (Staff) session.createCriteria(Staff.class)
				.add(Restrictions.eq("staff", "pembantu dekan bidang administrasi dan keuangan")).setMaxResults(1)
				.uniqueResult();

		final Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("nama_fakultas",
				fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? "Semua"
						: ((Fakultas) fakultas.getSelectedItem().getValue()).getNama());
		parameters.put("nama_jurusan",
				jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? "Semua"
						: ((Jurusan) jurusan.getSelectedItem().getValue()).getNama());

		parameters.put("fakultas",
				fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? -1L
						: ((Fakultas) fakultas.getSelectedItem().getValue()).getId());
		parameters.put("jurusan",
				jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? -1L
						: ((Jurusan) jurusan.getSelectedItem().getValue()).getId());
		parameters.put("dosen", dosen.getValue().trim());
		parameters.put("dekan", staffDekan == null ? "" : staffDekan.getNama());
		parameters.put("pembantu_dekan", staffPembantuDekan == null ? "" : staffPembantuDekan.getNama());

		return parameters;

	}

	@SuppressWarnings({})
	public void onDaftarHadirDosen(Event event) throws Exception {

		// Report.generatePDFReport(Report.PDF, parameters,
		// "Ruangan_Dosen",
		// ais.ui.util.WaktuUtil.getDate());

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Ruangan_Dosen",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Ruangan Dosen", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}
}
