package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Order;
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

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.PrestasiPegawai;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class LaporanPrestasiPegawai extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private AmbilDataPegawaiBanbox bandboxPegawai;
	private Center center;
	private Toolbar toolbar;

	public LaporanPrestasiPegawai() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Prestasi Pegawai", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPrestasiPegawai(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private MyDatebox tanggal;

	private MyDatebox sampai;

	private void init() throws Exception {

		EventListener eventListener = new EventListener() {

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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_pegawai")));
		row.appendChild(bandboxPegawai = new AmbilDataPegawaiBanbox());
		bandboxPegawai.setWidth("90%");

		if (Common.getCurrentUser() != null && Common.getCurrentUser().getPegawai() != null) {
			Pegawai pegawai = Common.getCurrentUser().getPegawai();
			bandboxPegawai.setAttribute("pegawai", pegawai);
			bandboxPegawai.setAttribute("myValue", pegawai);
			bandboxPegawai.setValue(pegawai.getNim() + " - " + pegawai.getNama());
			bandboxPegawai.setId("mhs_" + pegawai.getId());
			bandboxPegawai.setDisabled(true);
		}

		bandboxPegawai.setEventListener(eventListener);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		tanggal = new MyDatebox(calendar.getTime());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(sampai);
		sampai.setWidth("90%");
		sampai.addEventListener("onChange", eventListener);
		sampai.setReadonly(true);

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

				Map parameters = generateParameter((Pegawai) bandboxPegawai.getAttribute("pegawai"), tanggal.getValue(),
						sampai.getValue());
				return parameters;
			}
		}, "Prestasi_Pegawai", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		onKHS(null);

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map generateParameter(Pegawai pegawai, Date tanggal, Date sampai) throws Exception {

		if (pegawai == null) {
			return null;
		}

		Map parameters = ais.common.HashMapGenerator.getRand();

		if (pegawai.getGuru() != null) {

			pegawai.getGuru().putPhoto(parameters);

		} else if (pegawai.getDosen() != null) {

			pegawai.getDosen().putPhoto(parameters);

		} else {

			pegawai.putPhoto(parameters);

		}

		List<PrestasiPegawai> prestasiPegawais = HibernateUtil.currentSession().createCriteria(PrestasiPegawai.class)
				.add(Restrictions.between("tanggal", tanggal, sampai))
				.add(pegawai == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("pegawai", pegawai))
				.addOrder(Order.asc("pegawai")).addOrder(Order.asc("tanggal")).list();

		List<Map> maps = new ArrayList<Map>();
		for (PrestasiPegawai prestasiPegawai : prestasiPegawais) {
			Map map = new HashMap();
			Common.insertProperty(PrestasiPegawai.class, prestasiPegawai, map, "", 2);
			maps.add(map);
		}
		parameters.put("maps", maps);
		parameters.put("jurusan",
				(pegawai == null || pegawai.getJurusan() == null ? "" : pegawai.getJurusan().getNama()));
		parameters.put("nama", (pegawai == null ? "" : pegawai.getNama()));
		parameters.put("pegawai_id", pegawai == null || pegawai.getId() == null ? -1L : pegawai.getId());
		parameters.put("tanggal", tanggal);
		parameters.put("sampai", sampai);

		parameters.put("tanggal.format", Common.dateFormat2.get().format(tanggal));
		parameters.put("sampai.format", Common.dateFormat2.get().format(sampai));

		System.out.println("parameters => " + parameters);

		return parameters;
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(
							Report.PDF, generateParameter((Pegawai) bandboxPegawai.getAttribute("pegawai"),
									tanggal.getValue(), sampai.getValue()),
							"Prestasi_Pegawai", ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Prestasi Pegawai", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
