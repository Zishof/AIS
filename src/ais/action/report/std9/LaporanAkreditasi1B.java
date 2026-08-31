package ais.action.report.std9;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan akreditasi1 b. Kelas ini mengubah data domain menjadi
 * bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke
 * lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchFakultas}, {@code
 * Combobox searchJurusan}, {@code Center center}, {@code Toolbar toolbar}, {@code Map parameters};
 * inisialisasi/lifecycle ({@code init()}, {@code initData()}); pelaporan/ekspor ({@code onReport()}); operasi
 * domain lain ({@code generateParameter()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanAkreditasi1B extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Combobox searchFakultas;
	private Combobox searchJurusan;

	private Center center;
	private Toolbar toolbar;

	@SuppressWarnings("rawtypes")
	private Map parameters = null;

	public LaporanAkreditasi1B() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Akreditasi1 B", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAkreditasi1B(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() {

		searchFakultas = new Combobox();
		searchJurusan = new Combobox();
		Common.initFakultasDanJurusan(searchFakultas, searchJurusan, null, null);

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
		row.appendChild(searchFakultas);
		searchFakultas.setWidth("90%");
		searchFakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchJurusan);
		searchJurusan.setWidth("90%");
		searchJurusan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Button tampilkan;
		row.appendChild(tampilkan = new Button("Tampilkan Ulang"));
		tampilkan.addEventListener("onClick", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				return parameters;
			}
		}, "std9/1b", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		try {
			onReport(null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/std9/LaporanAkreditasi1B.java:159");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Akreditasi1 B", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter(Label label) throws Exception {

		Fakultas fakultas = (Fakultas) (searchFakultas.getSelectedItem() == null ? null
				: searchFakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchJurusan.getSelectedItem() == null ? null
				: searchJurusan.getSelectedItem().getValue());
		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("fakultas", fakultas == null ? "" : fakultas.getNama());
		parameters.put("jurusan", jurusan == null ? "" : jurusan.getNama());

		initData(label, parameters, fakultas, jurusan);

		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initData(final Label label, final Map parameters, final Fakultas fakultas, final Jurusan jurusan) {

		new Thread(new Runnable() {

			@Override
			public void run() {

				Session session = ais.action.report.Report.openNativeSession();

				List<Map> maps = new ArrayList<Map>();
				int size = Jurusan.SEMUA_STATUS.size();
				int index = 1;
				for (String s : Jurusan.SEMUA_STATUS) {
					label.setValue("Sedang mem-proses data " + s + " (" + ((index * 100) / size) + " %)");
					index++;
					try {
						String sql = "select\r\n"
								+ "sum(case when b.nama ilike 'S3' or b.nama ilike 'Strata 3' then 1 else 0 end) as s3,\r\n"
								+ "sum(case when b.nama ilike 'S2' or b.nama ilike 'Strata 2' then 1 else 0 end) as s2,\r\n"
								+ "sum(case when b.nama ilike 'S1' or b.nama ilike 'Strata 1' then 1 else 0 end) as s1,\r\n"
								+ "sum(case when b.nama ilike 'Sp-2' then 1 else 0 end) as sp2,\r\n"
								+ "sum(case when b.nama ilike 'Sp-1' then 1 else 0 end) as sp1,\r\n"
								+ "sum(case when b.nama ilike 'Profesi' then 1 else 0 end) as profesi,\r\n"
								+ "sum(case when b.nama ilike 'S-3T' then 1 else 0 end) as s3t,\r\n"
								+ "sum(case when b.nama ilike 'S-2T' then 1 else 0 end) as s2t,\r\n"
								+ "sum(case when b.nama ilike 'D4' then 1 else 0 end) as d4,\r\n"
								+ "sum(case when b.nama ilike 'D3' then 1 else 0 end) as d3,\r\n"
								+ "sum(case when b.nama ilike 'D2' then 1 else 0 end) as d2,\r\n"
								+ "sum(case when b.nama ilike 'D1' then 1 else 0 end) as d1\r\n" + "\r\n"
								+ "from jurusan a\r\n" + "inner join jenjang b on (a.jenjang=b.id)\r\n"
								+ "where (a.aktif or a.aktif is null)\r\n" + "and a.statusakreditasi='" + s + "' "
								+ (fakultas == null ? "" : " and a.fakultas = " + fakultas.getId())
								+ (jurusan == null ? "" : " and a.id = " + jurusan.getId());

						Object[] objs = (Object[]) session.createSQLQuery(sql).uniqueResult();

						Map map = new java.util.HashMap();
						map.put("status", s);
						map.put("S3",
								objs == null || objs[0] == null ? 0.0 : Double.parseDouble(objs[0].toString().trim()));
						map.put("S2",
								objs == null || objs[1] == null ? 0.0 : Double.parseDouble(objs[1].toString().trim()));
						map.put("S1",
								objs == null || objs[2] == null ? 0.0 : Double.parseDouble(objs[2].toString().trim()));
						map.put("SP2",
								objs == null || objs[3] == null ? 0.0 : Double.parseDouble(objs[3].toString().trim()));
						map.put("SP1",
								objs == null || objs[4] == null ? 0.0 : Double.parseDouble(objs[4].toString().trim()));
						map.put("Profesi",
								objs == null || objs[5] == null ? 0.0 : Double.parseDouble(objs[5].toString().trim()));
						map.put("S3T",
								objs == null || objs[6] == null ? 0.0 : Double.parseDouble(objs[6].toString().trim()));
						map.put("S2T",
								objs == null || objs[7] == null ? 0.0 : Double.parseDouble(objs[7].toString().trim()));
						map.put("D4",
								objs == null || objs[8] == null ? 0.0 : Double.parseDouble(objs[8].toString().trim()));
						map.put("D3",
								objs == null || objs[9] == null ? 0.0 : Double.parseDouble(objs[9].toString().trim()));
						map.put("D2", objs == null || objs[10] == null ? 0.0
								: Double.parseDouble(objs[10].toString().trim()));
						map.put("D1", objs == null || objs[11] == null ? 0.0
								: Double.parseDouble(objs[11].toString().trim()));
						maps.add(map);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/std9/LaporanAkreditasi1B.java:245");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Akreditasi1 B", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}
				}

				parameters.put("maps", maps);

				ais.action.report.Report.closeCurrentSessionQuietly();
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			}
		}).start();

	}

	@SuppressWarnings({ "rawtypes" })
	public void onReport(Event event) throws Exception {

		final Label label = new Label(ais.common.Common.getBahasaConfig("Sedang memproses data ...."));
		final Map parameters = generateParameter(label);

		ais.action.report.helper.LoadingReportUtil.showBusy(label);
		final Timer timer = new Timer(500);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ais.action.report.helper.LoadingReportUtil.showBusy(label);
				if (ais.action.report.helper.LoadingReportUtil.isError(label)) {

					ais.action.report.helper.LoadingReportUtil.clearBusy();
					ais.action.report.helper.LoadingReportUtil.stopAndDetach(timer);
				} else if (ais.action.report.helper.LoadingReportUtil.isSelesai(label)) {

					ais.action.report.helper.LoadingReportUtil.clearBusy();
					ais.action.report.helper.LoadingReportUtil.stopAndDetach(timer);

					File file = Report.generateFileReportWithProgress(Report.PDF, parameters, "std9/1b", ais.ui.util.WaktuUtil.getDate(),
							toolbar);
					CommonReport.tampilkanReportPDF(center, file);
				}

			}
		});
		timer.start();

	}

}
