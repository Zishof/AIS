package ais.action.report.format1.employ;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
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
import ais.database.model.employ.HukumanPegawai;
import ais.database.model.employ.PelanggaranPegawai;
import ais.database.model.employ.PendataanPelanggaranPegawai;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanPendataanPelanggaranPegawai extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private AmbilDataPegawaiBanbox bandboxPegawai;
	private Center center;
	private Toolbar toolbar;

	public LaporanPendataanPelanggaranPegawai() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pendataan Pelanggaran Pegawai", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPendataanPelanggaranPegawai(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private MyDatebox tanggal;
	private MyDatebox sampai;

	@SuppressWarnings("deprecation")
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
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

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 2);
		tanggal = new MyDatebox(calendar.getTime());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai"));
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

				Map parameters = generateParameter();
				return parameters;
			}
		}, "employ/kartu_pelanggaran_pegawai", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onKHS(null);
			}
		});
		print.setParent(hbox);

	}

	@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
	public Map generateParameter() throws Exception {

		Pegawai pegawai = (Pegawai) this.bandboxPegawai.getAttribute("pegawai");
		Date tanggal = this.tanggal.getValue();
		Date sampai = this.sampai.getValue();

		sampai.setDate(sampai.getDate() + 1);

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();

		if (pegawai != null) {
			pegawai.putPhoto(parameters);
		}

		parameters.put("nama", pegawai == null ? "" : (pegawai.getNama()));
		parameters.put("pegawai_id", pegawai == null || pegawai.getId() == null ? 1L : pegawai.getId());
		parameters.put("tanggal", tanggal);
		parameters.put("mulai", tanggal);
		parameters.put("sampai", sampai);

		System.out.println("parameters => " + parameters);

		Session session = HibernateUtil.currentSession();
		List<PendataanPelanggaranPegawai> pendataanPelanggaranPegawais = session
				.createCriteria(PendataanPelanggaranPegawai.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("pegawai"))

				.add(pegawai == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("pegawai", pegawai))

				.add(Restrictions.between("waktu", tanggal, sampai)).addOrder(Order.asc("pegawai.id"))
				.addOrder(Order.asc("waktu")).list();

		List<Map> maps = new ArrayList<Map>();
		for (PendataanPelanggaranPegawai pendataanPelanggaranPegawai : pendataanPelanggaranPegawais) {
			Map map = new HashMap();
			Common.insertProperty(PendataanPelanggaranPegawai.class, pendataanPelanggaranPegawai, map, "");

			Double point = 0.0;
			Set<HukumanPegawai> hukumans = pendataanPelanggaranPegawai.getHukumanPegawais();
			for (HukumanPegawai hukuman : hukumans) {
				point += hukuman.getPoint();
			}

			Double kredit = 0.0;
			Set<PelanggaranPegawai> pelanggarans = pendataanPelanggaranPegawai.getPelanggaranPegawais();
			for (PelanggaranPegawai pelanggaran : pelanggarans) {
				kredit += pelanggaran.getPoint();
			}

			map.put("point", point);
			map.put("kredit", kredit);
			maps.add(map);
		}
		parameters.put("maps", maps);
		return parameters;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map generateParameter(PendataanPelanggaranPegawai pendataanPelanggaranPegawai) throws Exception {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(pendataanPelanggaranPegawai.getWaktu());
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 2);

		Date tanggal = calendar.getTime();
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(pendataanPelanggaranPegawai.getWaktu());
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 2);
		Date sampai = calendar.getTime();

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();

		if (pendataanPelanggaranPegawai.getPegawai() != null) {
			pendataanPelanggaranPegawai.getPegawai().putPhoto(parameters);
		}

		parameters.put("nama", pendataanPelanggaranPegawai.getPegawai() == null ? ""
				: (pendataanPelanggaranPegawai.getPegawai().getNama()));
		parameters.put("pegawai_id", pendataanPelanggaranPegawai.getPegawai() == null ? 1L
				: pendataanPelanggaranPegawai.getPegawai().getId());
		parameters.put("tanggal", tanggal);
		parameters.put("mulai", tanggal);
		parameters.put("sampai", sampai);

		System.out.println("parameters => " + parameters);

		Session session = HibernateUtil.currentSession();
		session.refresh(pendataanPelanggaranPegawai);

		List<Map> maps = new ArrayList<Map>();
		Map map = new HashMap();
		Common.insertProperty(PendataanPelanggaranPegawai.class, pendataanPelanggaranPegawai, map, "");

		Double point = 0.0;
		Set<HukumanPegawai> hukumans = pendataanPelanggaranPegawai.getHukumanPegawais();
		for (HukumanPegawai hukuman : hukumans) {
			point += hukuman.getPoint();
		}

		Double kredit = 0.0;
		Set<PelanggaranPegawai> pelanggarans = pendataanPelanggaranPegawai.getPelanggaranPegawais();
		for (PelanggaranPegawai pelanggaran : pelanggarans) {
			kredit += pelanggaran.getPoint();
		}

		map.put("point", point);
		map.put("kredit", kredit);
		maps.add(map);
		parameters.put("maps", maps);
		return parameters;
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "employ/kartu_pelanggaran_pegawai",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Pendataan Pelanggaran Pegawai", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
