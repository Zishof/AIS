package ais.action.report.format1.library;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import ais.ui.util.MyButtonConfig;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;
import ais.ui.util.MyWindow;

import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.library.Item;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyDatebox;

public class LaporanStokItem extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private AmbilDataPerpustakaanBanbox perpustakaan;
	private MyDatebox mulai;

	private Center center;
	private Toolbar toolbar;

	private Perpustakaan myperpustakaan;

	private Item item;

	private MyDatebox sampai;

	public LaporanStokItem() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Stok Item", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanStokItem(Perpustakaan perpustakaan, Item item) {
		super();
		myperpustakaan = perpustakaan;
		this.item = item;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Stok Item", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
		west.setWidth("350px");

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

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 3);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai Tanggal"));
		row.appendChild(mulai = new MyDatebox(calendar.getTime()));
		mulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai Tanggal"));
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
				Map parameters = generateParameter();
				return parameters;
			}
		}, "library/stok_item", null, new EventListener() {

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
		final Perpustakaan perpustakaan = (Perpustakaan) this.perpustakaan.getAttribute("perpustakaan");
		return generateParameterJsp(perpustakaan.getId(), item == null ? null : item.getId(), this.mulai.getValue(),
				this.sampai.getValue());
	}

	// ============================================================
	// Versi REUSABLE (dipakai bersama ZK + JSP). Param-gen identik.
	// ============================================================

	/** Key template jrxml laporan ini. */
	public static String namaTemplateLaporanJsp() {
		return "library/stok_item";
	}

	/** Param-gen inti tanpa dependensi UI (dipanggil ZK & JSP). */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map generateParameterJsp(Long perpustakaanId, Long itemId, Date mulai, Date sampai) throws Exception {
		if (mulai == null) {
			mulai = ais.ui.util.WaktuUtil.getDate();
		}
		if (sampai == null) {
			sampai = ais.ui.util.WaktuUtil.getDate();
		}
		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("perpustakaan", perpustakaanId);
		parameters.put("item", itemId == null ? -1L : itemId);
		parameters.put("mulai", Common.databaseDateFormat.get().format(mulai));
		parameters.put("sampai", Common.databaseDateFormat.get().format(sampai));
		return parameters;
	}

	/** Adapter dari request JSP -> param-gen inti (dipantulkan oleh JalankanLaporanJsp). */
	@SuppressWarnings({ "rawtypes" })
	public static Map generateParameterDariRequestJsp(javax.servlet.http.HttpServletRequest req) throws Exception {
		Long perpustakaanId = ais.action.report.helper.LaporanJspUtil.parseLong(req.getParameter("perpustakaan"));
		if (perpustakaanId == null) {
			throw new Exception("Perpustakaan wajib dipilih.");
		}
		Long itemId = ais.action.report.helper.LaporanJspUtil.parseLong(req.getParameter("item"));
		Date mulai = ais.action.report.helper.LaporanJspUtil.parseTanggal(req.getParameter("mulai"));
		Date sampai = ais.action.report.helper.LaporanJspUtil.parseTanggal(req.getParameter("sampai"));
		return generateParameterJsp(perpustakaanId, itemId, mulai, sampai);
	}

	@SuppressWarnings({ "unchecked" })
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "library/stok_item", ais.ui.util.WaktuUtil.getDate(),
					toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Stok Item", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
