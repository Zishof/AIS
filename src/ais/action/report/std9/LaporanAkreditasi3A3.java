package ais.action.report.std9;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.Type;
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
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class LaporanAkreditasi3A3 extends MyWindow {

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

	public LaporanAkreditasi3A3() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Akreditasi3 A3", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAkreditasi3A3(String title, String border, boolean closable) throws Exception {
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
		}, "std9/3a3", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		try {
			onReport(null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/std9/LaporanAkreditasi3A3.java:165");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Akreditasi3 A3", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

			@SuppressWarnings("deprecation")
			@Override
			public void run() {

				Session session = ais.action.report.Report.openNativeSession();

				List<Map> maps = new ArrayList<Map>();

				List<Jurusan> jurusans = session.createCriteria(Jurusan.class).add(jurusan == null
						? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan.id", jurusan.getId()))

						.add(fakultas == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("fakultas", fakultas))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("fakultas")).list();
				int size = jurusans.size();
				int index = 1;

				for (Jurusan jurusan : jurusans) {

					label.setValue("Sedang mem-proses data " + jurusan + " (" + ((index * 100) / size) + " %)");
					index++;

					try {

						String sql = "count(id) as t2," + "sum(case when this_.sertifikasi then 1 else 0 end) as t1";

						Object[] n6 = (Object[]) session.createCriteria(Dosen.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("jurusan", jurusan))

								.setProjection(
										Projections.projectionList()
												.add(Projections.sqlProjection(sql, new String[] { "t2", "t1" },
														new Type[] { org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE })))
								.uniqueResult();

						Map map = new java.util.HashMap();
						map.put("unitPengelola", jurusan.getFakultas().getNama() + " / " + jurusan.getNama());

						map.put("jumlahDosen",
								n6 == null || n6[0] == null ? 0.0 : Double.parseDouble(n6[0].toString().trim()));
						map.put("jumlahDosenBersertifikat",
								n6 == null || n6[1] == null ? 0.0 : Double.parseDouble(n6[1].toString().trim()));

						maps.add(map);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/std9/LaporanAkreditasi3A3.java:238");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Akreditasi3 A3", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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

					File file = Report.generateFileReportWithProgress(Report.PDF, parameters, "std9/3a3", ais.ui.util.WaktuUtil.getDate(),
							toolbar);
					CommonReport.tampilkanReportPDF(center, file);
				}

			}
		});
		timer.start();

	}

}
