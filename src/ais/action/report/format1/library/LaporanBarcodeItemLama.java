package ais.action.report.format1.library;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
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
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.library.helper.AmbilDataItemBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.BatchItemPunyaBarcode;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyWindow;

public class LaporanBarcodeItemLama extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private AmbilDataItemBanbox ambilDataItemBanbox;

	private Center center;
	private Toolbar toolbar;

	private Item item;

	private Combobox batch;

	private MyIntbox mulai;

	private MyIntbox sampai;

	public LaporanBarcodeItemLama() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Barcode Item Lama", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanBarcodeItemLama(Item item) {
		super();
		this.item = item;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Barcode Item Lama", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void init() throws Exception {

		final EventListener eventListener = new EventListener() {

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Item"));
		row.appendChild(ambilDataItemBanbox = new AmbilDataItemBanbox());
		ambilDataItemBanbox.setWidth("90%");

		EventListener myEventListener = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Item item = (Item) ambilDataItemBanbox.getAttribute("item");
				if (item != null) {
					Session session = HibernateUtil.currentSession();
					List<BatchItemPunyaBarcode> batchItemPunyaBarcodes = session
							.createCriteria(BatchItemPunyaBarcode.class).add(Restrictions.eq("item", item))
							.addOrder(Order.desc("id")).list();
					Common.insertComboItems(batch, new String[] { "kode", "perpustakaan" }, "tanggal",
							batchItemPunyaBarcodes);
				}

				eventListener.onEvent(arg0);
			}
		};

		ambilDataItemBanbox.setEventListener(myEventListener);
		if (item != null) {
			ambilDataItemBanbox.setAttribute("item", item);
			ambilDataItemBanbox.setValue(item.toString());
			ambilDataItemBanbox.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Batch"));
		row.appendChild(batch = new Combobox());
		batch.setWidth("90%");
		batch.addEventListener("onChange", eventListener);

		myEventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		row.appendChild(mulai = new MyIntbox(0));
		mulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		row.appendChild(sampai = new MyIntbox(10));
		sampai.setWidth("90%");

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (ambilDataItemBanbox.getAttribute("item") == null) {
					return null;
				}

				if (batch.getSelectedItem() == null) {
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "library/barcode_report_new", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (ambilDataItemBanbox.getAttribute("item") == null) {
			return null;
		}

		if (batch.getSelectedItem() == null) {
			return null;
		}

		final Map parameters = ais.common.HashMapGenerator.getRand();

		Session session = HibernateUtil.currentSession();
		List<ItemPunyaBarcode> itemPunyaBarcodes = session.createCriteria(ItemPunyaBarcode.class)
				.add(Restrictions.eq("item", ambilDataItemBanbox.getAttribute("item")))

				.add(Restrictions.eq("batchItemPunyaBarcode", batch.getSelectedItem().getValue()))

				.setFirstResult(mulai.getValue() == null ? 0 : mulai.getValue())
				.setMaxResults(sampai.getValue() == null ? 0 : sampai.getValue()).addOrder(Order.desc("id"))

				.list();

		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		for (ItemPunyaBarcode itemPunyaBarcode : itemPunyaBarcodes) {
			Map<String, Object> map = new java.util.HashMap<String, Object>();
			String code = (itemPunyaBarcode.getPerpustakaan() == null ? ""
					: "\n" + itemPunyaBarcode.getPerpustakaan().getNama()) + "\n" + itemPunyaBarcode.getItem().getIsbn()
					+ " - " + itemPunyaBarcode.getItem().getNama();
			map.put("code", code);

			map.put("c_code", itemPunyaBarcode.getBarcode());
			maps.add(map);
		}
		parameters.put("maps", maps);
		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "library/barcode_report_new",
					ais.ui.util.WaktuUtil.getDate(), null, toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Barcode Item Lama", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
