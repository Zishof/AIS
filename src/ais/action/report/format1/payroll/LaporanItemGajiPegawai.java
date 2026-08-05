package ais.action.report.format1.payroll;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.payroll.util.ItemGajiPegawaiTreeModel;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Pegawai;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.ItemGajiPegawai;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanItemGajiPegawai extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private FormatItemGaji formatGaji;
	private ItemGajiPegawaiTreeModel itemGajiPegawaiTreeModel;

	private Combobox bulan;

	private Combobox tahun;

	private Pegawai pegawai;

	public LaporanItemGajiPegawai(FormatItemGaji formatGaji, Pegawai pegawai) {
		super();
		try {
			this.formatGaji = formatGaji;
			this.pegawai = pegawai;
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Item Gaji Pegawai", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void init() throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		bulan = new Combobox();
		for (int i = 0; i < 12; i++) {
			Comboitem comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i + 1);
			bulan.appendChild(comboitem);
		}

		Common.selectComboItem(bulan, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1);
		bulan.setReadonly(true);

		tahun = new Combobox();
		Integer currTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = currTahun - 10; i < currTahun + 10; i++) {
			Comboitem comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
		}
		tahun.setReadonly(true);
		Common.selectComboItem(tahun, currTahun);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("300px");

		MyGrid grid = new MyGrid();
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setWidth("20%");
		column.setParent(columns);
		column = new Column();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Bulan")));
		row.appendChild(bulan);
		bulan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun")));
		row.appendChild(tahun);
		tahun.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				Map parameters = generateParameter();
				return parameters;
			}
		}, "payroll/ItemGajiPegawai", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings("rawtypes")
	private Map generateParameter() throws Exception {
		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		return generateParameter(null, maps);
	}

	private String getStrings(Integer deep) {
		String d = "";
		for (int i = 0; i < deep; i++) {
			d += "   ";
		}
		return d;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter(ItemGajiPegawai parent, List<Map<String, Object>> maps) throws Exception {
		itemGajiPegawaiTreeModel = new ItemGajiPegawaiTreeModel(true, formatGaji, pegawai,
				PembayaranGajiPunyaPegawai.ambilMulai((Integer) tahun.getSelectedItem().getValue(),
						((Integer) bulan.getSelectedItem().getValue()) - 1));
		List<ItemGajiPegawai> workspaces = itemGajiPegawaiTreeModel.getChildren(parent);
		Date waktu = WaktuUtil.getDate();

		for (ItemGajiPegawai itemGajiPegawai : workspaces) {

			List<Long> longs = new ArrayList<Long>();

			itemGajiPegawaiTreeModel.getParentCount(itemGajiPegawai, longs);

			Integer deep = longs.size();

			longs = null;

			Map<String, Object> map = new java.util.HashMap<String, Object>();
			map.put("workspace_id", itemGajiPegawai.getId());
			map.put("unique_id", itemGajiPegawai.getId());
			map.put("kode", itemGajiPegawai.getKode() == null ? "" : itemGajiPegawai.getKode());
			map.put("nama", getStrings(deep) + (itemGajiPegawai.getNama() == null ? "" : itemGajiPegawai.getNama()));

			String debet = itemGajiPegawai.getItemGaji().getAkunDebet() == null ? ""
					: itemGajiPegawai.getItemGaji().getAkunDebet().getKode() + "-"
							+ itemGajiPegawai.getItemGaji().getAkunDebet().getNama();
			map.put("debet", debet);
			String kredit = itemGajiPegawai.getItemGaji().getAkun() == null ? ""
					: itemGajiPegawai.getItemGaji().getAkun().getKode() + "-"
							+ itemGajiPegawai.getItemGaji().getAkun().getNama();
			map.put("kredit", kredit);
			Double hasil = itemGajiPegawaiTreeModel.hitungItemGajiPegawai(itemGajiPegawai.getKode(),
					itemGajiPegawai.getDefaultFormula(), waktu, (Integer) bulan.getSelectedItem().getValue(),
					(Integer) tahun.getSelectedItem().getValue(), null, null);
			map.put("hitungan", hasil);

			maps.add(map);

			if (!itemGajiPegawaiTreeModel.isLeaf(itemGajiPegawai)) {
				generateParameter(itemGajiPegawai, maps);
			}
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("target", formatGaji.getNama() + " " + pegawai.getNama());
		parameters.put("maps", maps);
		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "payroll/ItemGajiPegawai",
					ais.ui.util.WaktuUtil.getDate(), null, toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Item Gaji Pegawai", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
