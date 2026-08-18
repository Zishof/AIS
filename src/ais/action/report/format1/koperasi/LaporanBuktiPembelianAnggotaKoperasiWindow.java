package ais.action.report.format1.koperasi;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;
import org.zkoss.zul.Window;

import ais.action.master.koperasi.helper.AmbilDataPembelianAnggotaKoperasiBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Pembelian;
import ais.database.model.koperasi.PembelianAnggotaKoperasi;

public class LaporanBuktiPembelianAnggotaKoperasiWindow extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private AmbilDataPembelianAnggotaKoperasiBanbox pembelianAnggotaKoperasi;

	private Center center;

	private PembelianAnggotaKoperasi mPembelianAnggotaKoperasi;

	private Toolbar toolbar;

	public LaporanBuktiPembelianAnggotaKoperasiWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/koperasi/LaporanBuktiPembelianAnggotaKoperasiWindow.java:61");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Bukti Pembelian Anggota Koperasi Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanBuktiPembelianAnggotaKoperasiWindow(PembelianAnggotaKoperasi mPembelianAnggotaKoperasi) {
		super();
		this.mPembelianAnggotaKoperasi = mPembelianAnggotaKoperasi;

		try {
			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/koperasi/LaporanBuktiPembelianAnggotaKoperasiWindow.java:72");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Bukti Pembelian Anggota Koperasi Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanBuktiPembelianAnggotaKoperasiWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() {

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);

		West north = new West();
		north.setVisible(mPembelianAnggotaKoperasi == null);
		north.setWidth("450px");
		north.setParent(borderlayout);

		Div div = new Div();
		div.setParent(north);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(div);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setWidth("150px");
		column.setParent(columns);
		column = new Column();

		column.setParent(columns);
		column = new Column();
		column.setWidth("150px");
		column.setParent(columns);
		column = new Column();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		boolean disabled = mPembelianAnggotaKoperasi != null;

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Transaksi")));
		row.appendChild(pembelianAnggotaKoperasi = new AmbilDataPembelianAnggotaKoperasiBanbox());
		pembelianAnggotaKoperasi.setWidth("90%");
		pembelianAnggotaKoperasi.setAttribute("pembelianAnggotaKoperasi", mPembelianAnggotaKoperasi);
		pembelianAnggotaKoperasi.setValue(mPembelianAnggotaKoperasi == null ? "" : mPembelianAnggotaKoperasi.getKode());
		pembelianAnggotaKoperasi.setDisabled(disabled);
		pembelianAnggotaKoperasi.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);
			}
		});

		South south = new South();
		south.setParent(borderlayout);
		south.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "koperasi/struk_pembelianAnggotaKoperasi"));

		onCetakStatusPasien(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		PembelianAnggotaKoperasi pembelianAnggotaKoperasi = (PembelianAnggotaKoperasi) this.pembelianAnggotaKoperasi
				.getAttribute("pembelianAnggotaKoperasi");
		if (pembelianAnggotaKoperasi == null) {
			return null;
		}

		Map parameters = new HashMap();
		Common.insertProperty(PembelianAnggotaKoperasi.class, pembelianAnggotaKoperasi, parameters, "", 2);
		parameters.put("id", pembelianAnggotaKoperasi == null || pembelianAnggotaKoperasi.getId() == null ? -1L : pembelianAnggotaKoperasi.getId());

		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public void onCetakStatusPasien(Event event) {

		try {
			Map parameters = generateParameter();

			File file = Report.generateFileReportWithProgress(Report.PDF, parameters, "koperasi/struk_pembelianAnggotaKoperasi",
					ais.ui.util.WaktuUtil.getDate(), toolbar);

			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/koperasi/LaporanBuktiPembelianAnggotaKoperasiWindow.java:182");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Bukti Pembelian Anggota Koperasi Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void dataPembelian(PembelianAnggotaKoperasi pembelianAnggotaKoperasi, Map parameters)
			throws Exception {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Pembelian.class);

		criteria.add(Restrictions.eq("pembelianAnggotaKoperasi", pembelianAnggotaKoperasi));

		List<Pembelian> pembelians = criteria.createAlias("produk", "produk")
				.addOrder(Order.asc("produk.jenisProduk.id")).addOrder(Order.asc("produk.nama")).list();
		List<Map> maps = new ArrayList<Map>();

		Double total = 0.0;
		Tbmuser tbmuserValidator = null;
		for (Pembelian pembelian : pembelians) {

			Double nominal = pembelian.getTotal();

			total += nominal;
			Map map = new HashMap();
			maps.add(map);
			Common.insertProperty(Pembelian.class, pembelian, map, "", 1);
			map.put("jenis_biaya_id", pembelian.getProduk().getJenisProduk().getId());
			map.put("jenis_biaya", pembelian.getProduk().getJenisProduk().getNama());

			map.put("tanggal", pembelian.getPembelianAnggotaKoperasi().getTanggalPembayaran());

			map.put("tanggal_jatuh_tempo", pembelian.getPembelianAnggotaKoperasi().getTanggalPembayaran());

			map.put("id_transaksi", pembelian.getPembelianAnggotaKoperasi().getId());

			String item = pembelian.getProduk().getNama();

			map.put("item_biaya", item);

			map.put("nominal", nominal);
			map.put("denda", 0.0);
			map.put("nilai", pembelian.getTotal());
			map.put("diskon", 0.0);

			map.put("cara", pembelian.getPembelianAnggotaKoperasi() == null ? ""
					: pembelian.getPembelianAnggotaKoperasi().getCaraPembayaranKoperasi().getNama());

			PembelianAnggotaKoperasi pembelianAnggotaKoperasiData = pembelian.getPembelianAnggotaKoperasi();

			map.put("tambahan_deposit", 0.0);
			map.put("validator", pembelianAnggotaKoperasiData == null ? "" : pembelian.getOleh());
			tbmuserValidator = pembelian == null ? null : pembelian.getTbmuser();

			map.put("bayarke", 1);
			map.put("dibayarsebayak", 1);

			map.put("nomor_induk", pembelian.getPembelianAnggotaKoperasi().getAnggotaKoperasi().getKodeIdentitas());
			map.put("nama_siswa", pembelian.getPembelianAnggotaKoperasi().getAnggotaKoperasi().getNama());
			map.put("koperasi_id", pembelian.getPembelianAnggotaKoperasi().getAnggotaKoperasi().getKoperasi().getId());

			map.put("kelas", "");
			map.put("asrama", "");

		}

		try {
			if (tbmuserValidator != null && tbmuserValidator.getUserId() != null) {
				Common.insertProperty(Tbmuser.class, tbmuserValidator, parameters, "validatorData", 2);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/koperasi/LaporanBuktiPembelianAnggotaKoperasiWindow.java:253");
			// TODO: handle exception
		}

		parameters.put("terbilang", IndonesianNumberToWords.convert(total.longValue()));
		parameters.put("maps", maps);

		pembelians.clear();
		pembelians = null;
	}
}
