package ais.action.report.format1.library;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import ais.ui.util.MyButtonConfig;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;
import ais.ui.util.MyWindow;

import ais.action.master.library.helper.AmbilDataItemBanyakBerdasarkanStok;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.library.Item;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyDatebox;

public class LaporanTrackingStokItem extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private AmbilDataPerpustakaanBanbox perpustakaan;
	private MyDatebox mulai;
	private MyDatebox sampai;

	private Center center;
	private Toolbar toolbar;

	private ArrayList<Item> selectedItems;

	private Perpustakaan myperpustakaan;

	private Item item;

	public LaporanTrackingStokItem() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Tracking Stok Item", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanTrackingStokItem(Perpustakaan perpustakaan, Item item) {
		super();
		myperpustakaan = perpustakaan;
		this.item = item;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Tracking Stok Item", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("380px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("40%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan = new AmbilDataPerpustakaanBanbox());
		perpustakaan.setWidth("90%");

		if (myperpustakaan != null) {
			perpustakaan.setAttribute("perpustakaan", myperpustakaan);
			perpustakaan.setValue(myperpustakaan.toString());
			perpustakaan.setDisabled(true);
		}

		if (item == null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Item"));
			MyButtonConfig ambilItem;
			row.appendChild(ambilItem = new MyButtonConfig("Ambil data item"));
			ambilItem.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Perpustakaan myperpustakaan = (Perpustakaan) perpustakaan.getAttribute("perpustakaan");

					if (myperpustakaan == null) {
						MyMessageboxConfig.show("Pilih salah satu perpustakaan", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
						return;
					}

					AmbilDataItemBanyakBerdasarkanStok ambilDataItemBanyak = new AmbilDataItemBanyakBerdasarkanStok(
							new ArrayList<Item>(), myperpustakaan, false);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
					ambilDataItemBanyak.setEventListener(new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event event) throws Exception {
							selectedItems = (ArrayList<Item>) event.getData();
							onReport(event);
						}
					});
					ambilDataItemBanyak.setWidth("570px");
					ambilDataItemBanyak.setHeight("97%");
					ambilDataItemBanyak.setVisible(true);
					ambilDataItemBanyak.setPosition("left");
					ambilDataItemBanyak.onModal();
				}
			});
		}
		// ambilItem.setWidth("90%");

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		row.appendChild(mulai = new MyDatebox(calendar.getTime()));
		mulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		row.appendChild(sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		sampai.setWidth("90%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onReport(event);
			}
		};

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		MyButtonConfig button = new MyButtonConfig("Tampilkan Laporan");
		button.setParent(row);
		button.addEventListener("onClick", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (perpustakaan.getAttribute("perpustakaan") == null) {
					return null;
				}
				if (selectedItems == null) {
					return null;
				}
				Map parameters = generateParameter();
				return parameters;
			}
		}, "library/tracking_stok_item", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		if (item != null) {
			onReport(null);
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		if (perpustakaan.getAttribute("perpustakaan") == null) {
			return null;
		}
		if (selectedItems == null) {
			selectedItems = new ArrayList<Item>();
		}

		if (item != null) {
			selectedItems.add(item);
		}

		List<Long> myitems = new ArrayList<Long>();
		if (selectedItems == null || selectedItems.size() == 0) {
			myitems.add(-1234567899999999999L);
		} else {
			for (Item item : selectedItems) {
				myitems.add(item.getId());
			}
		}

		Date mulai = this.mulai.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : this.mulai.getValue();
		Date sampai = this.sampai.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : this.sampai.getValue();

		final Perpustakaan perpustakaan = (Perpustakaan) this.perpustakaan.getAttribute("perpustakaan");
		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("perpustakaan", perpustakaan.getId());
		parameters.put("items", myitems);
		parameters.put("mulai", Common.databaseDateFormat.get().format(mulai));
		parameters.put("sampai", Common.databaseDateFormat.get().format(sampai));
		return parameters;
	}

	@SuppressWarnings({ "unchecked" })
	public void onReport(Event event) {

		try {
			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "library/tracking_stok_item",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Tracking Stok Item", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
