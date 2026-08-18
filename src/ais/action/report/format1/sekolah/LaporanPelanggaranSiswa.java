package ais.action.report.format1.sekolah;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.Hukuman;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Pelanggaran;
import ais.database.model.sekolah.PelanggaranSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanPelanggaranSiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private AmbilDataSiswaBanbox bandboxSiswa;
	private Center center;
	private Toolbar toolbar;

	public LaporanPelanggaranSiswa() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pelanggaran Siswa", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPelanggaranSiswa(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private MyDatebox tanggal;
	private MyDatebox sampai;

	private Combobox yayasan;

	private Combobox sekolah;

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

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(yayasan);
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(sekolah);
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_siswa")));
		row.appendChild(bandboxSiswa = new AmbilDataSiswaBanbox());
		bandboxSiswa.setWidth("90%");

		if (Common.getCurrentUser() != null && Common.getCurrentUser().getSiswa() != null) {
			Siswa siswa = Common.getCurrentUser().getSiswa();
			bandboxSiswa.setAttribute("siswa", siswa);
			bandboxSiswa.setAttribute("myValue", siswa);
			bandboxSiswa.setValue(siswa.getNim() + " - " + siswa.getNama());
			bandboxSiswa.setId("mhs_" + siswa.getId());
			bandboxSiswa.setDisabled(true);
		}

		bandboxSiswa.setEventListener(eventListener);

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
		}, "sekolah/kartu_pelanggaran", null, new EventListener() {

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

		Siswa siswa = (Siswa) this.bandboxSiswa.getAttribute("siswa");
		Date tanggal = this.tanggal.getValue();
		Date sampai = this.sampai.getValue();
		sampai.setDate(sampai.getDate() + 1);
		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();

		if (siswa != null) {
			siswa.putPhoto(parameters);
		}
		Yayasan yayasan = (Yayasan) (this.yayasan.getSelectedItem() == null ? null
				: this.yayasan.getSelectedItem().getValue());
		Sekolah sekolah = (Sekolah) (this.sekolah.getSelectedItem() == null ? null
				: this.sekolah.getSelectedItem().getValue());

		parameters.put("yayasan", yayasan == null ? "" : yayasan.getNama());
		parameters.put("sekolah", sekolah == null ? "" : sekolah.getNama());
		parameters.put("nama", siswa == null ? "" : (siswa.getNama()));
		parameters.put("siswa_id", siswa == null || siswa.getId() == null ? 1L : siswa.getId());
		parameters.put("tanggal", tanggal);
		parameters.put("mulai", tanggal);
		parameters.put("sampai", sampai);

		System.out.println("parameters => " + parameters);

		Session session = HibernateUtil.currentSession();
		List<PelanggaranSiswa> pelanggaranSiswas = session.createCriteria(PelanggaranSiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.isNotNull("siswa"))

				.add(siswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("siswa", siswa))
				.add(yayasan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("yayasan", yayasan))
				.add(sekolah == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("sekolah", sekolah))

				.add(Restrictions.between("waktu", tanggal, sampai)).addOrder(Order.asc("siswa.id"))
				.addOrder(Order.asc("waktu")).list();

		List<Map> maps = new ArrayList<Map>();
		for (PelanggaranSiswa pelanggaranSiswa : pelanggaranSiswas) {
			Map map = new HashMap();
			Common.insertProperty(PelanggaranSiswa.class, pelanggaranSiswa, map, "");

			Double point = 0.0;
			Set<Hukuman> hukumans = pelanggaranSiswa.getHukumans();
			for (Hukuman hukuman : hukumans) {
				point += hukuman.getPoin();
			}
			Double kredit = 0.0;
			Set<Pelanggaran> pelanggarans = pelanggaranSiswa.getPelanggarans();
			for (Pelanggaran pelanggaran : pelanggarans) {
				kredit += pelanggaran.getKredit();
			}

			KelasSiswa kelas = Siswa.ambilKelas(pelanggaranSiswa.getSiswa(), pelanggaranSiswa.getTa());
			Common.insertProperty(KelasSiswa.class, kelas, map, "kelas");
			map.put("kelas", kelas == null ? "" : kelas.getNama());
			map.put("point", point);
			map.put("kredit", kredit);
			maps.add(map);
		}
		parameters.put("maps", maps);
		return parameters;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map generateParameter(PelanggaranSiswa pelanggaranSiswa) throws Exception {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(pelanggaranSiswa.getWaktu());
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 2);

		Date tanggal = calendar.getTime();
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(pelanggaranSiswa.getWaktu());
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 2);
		Date sampai = calendar.getTime();

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();

		if (pelanggaranSiswa.getSiswa() != null) {
			pelanggaranSiswa.getSiswa().putPhoto(parameters);
		}
		Yayasan yayasan = pelanggaranSiswa.getYayasan();
		Sekolah sekolah = pelanggaranSiswa.getSekolah();

		parameters.put("yayasan", yayasan == null ? "" : yayasan.getNama());
		parameters.put("sekolah", sekolah == null ? "" : sekolah.getNama());
		parameters.put("nama", pelanggaranSiswa.getSiswa() == null ? "" : (pelanggaranSiswa.getSiswa().getNama()));
		parameters.put("siswa_id", pelanggaranSiswa.getSiswa() == null ? 1L : pelanggaranSiswa.getSiswa().getId());
		parameters.put("tanggal", tanggal);
		parameters.put("mulai", tanggal);
		parameters.put("sampai", sampai);

		System.out.println("parameters => " + parameters);

		Session session = HibernateUtil.currentSession();
		session.refresh(pelanggaranSiswa);

		List<Map> maps = new ArrayList<Map>();
		Map map = new HashMap();
		Common.insertProperty(PelanggaranSiswa.class, pelanggaranSiswa, map, "");

		Double point = 0.0;
		Set<Hukuman> hukumans = pelanggaranSiswa.getHukumans();
		for (Hukuman hukuman : hukumans) {
			point += hukuman.getPoin();
		}

		Double kredit = 0.0;
		Set<Pelanggaran> pelanggarans = pelanggaranSiswa.getPelanggarans();
		for (Pelanggaran pelanggaran : pelanggarans) {
			kredit += pelanggaran.getKredit();
		}

		KelasSiswa kelas = Siswa.ambilKelas(pelanggaranSiswa.getSiswa(), pelanggaranSiswa.getTa());
		Common.insertProperty(KelasSiswa.class, kelas, map, "kelas");
		map.put("kelas", kelas == null ? "" : kelas.getNama());
		map.put("point", point);
		map.put("kredit", kredit);
		maps.add(map);
		parameters.put("maps", maps);
		return parameters;
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "sekolah/kartu_pelanggaran",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Pelanggaran Siswa", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
