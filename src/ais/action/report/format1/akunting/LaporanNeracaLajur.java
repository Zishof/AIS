package ais.action.report.format1.akunting;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
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
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.akunting.util.AkunTreeModel;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanNeracaLajur extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Combobox tahun;
	private Combobox bulan;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataAkunBanbox akun;
	private Combobox level;

	private Center center;
	private Toolbar toolbar;

	public LaporanNeracaLajur() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Neraca Lajur", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanNeracaLajur(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() throws Exception {

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		row.appendChild(tahun = new Combobox());
		int year = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = year + 5; i > (year - 20); i--) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
			if (i == year) {
				tahun.setSelectedItem(comboitem);
			}
		}
		tahun.setWidth("90%");
		tahun.setReadonly(true);
		// tahun.// addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bulan"));
		row.appendChild(bulan = new Combobox());

		int month = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH);
		int i = 1;
		for (String bln : Common.BULAN) {
			MyComboitemConfig comboitem2 = new MyComboitemConfig(bln);
			comboitem2.setValue(i);
			bulan.appendChild(comboitem2);
			i++;
		}
		bulan.setSelectedIndex(month);
		bulan.setWidth("90%");
		bulan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setWidth("90%");
		// satuanKerja.// setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun"));
		row.appendChild(akun = new AmbilDataAkunBanbox(true));
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Level"));
		row.appendChild(level = new Combobox());
		int defaultLevel = 0;
		for (i = 0; i < 20; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			level.appendChild(comboitem);
			if (i == defaultLevel) {
				level.setSelectedItem(comboitem);
			}
		}
		level.setWidth("90%");
		level.setReadonly(true);
		// level.// addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
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

				if (tahun.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pilih salah satu tahun", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "akunting/neraca_lajur", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (tahun.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu tahun", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			// MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan",
			// MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return null;
		}
		if (level.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu level", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}

		Integer tahun1 = (Integer) this.tahun.getSelectedItem().getValue();
		final SatuanKerja satuanKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");
		Akun akun1 = (Akun) this.akun.getAttribute("akun");

		SatuanKerjaTreeModel satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		satuanKerjas.add(satuanKerja);
		satuanKerjaTreeModel.getChildsSet(satuanKerja, satuanKerjas);

		Session session = HibernateUtil.currentSession();
		List<Akun> akuns = session.createCriteria(Akun.class).addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
				.add(Restrictions.eq("tahunAkun", tahun1)).list();

		AkunTreeModel akunTreeModel = new AkunTreeModel(akun1);

		Integer level = (Integer) this.level.getSelectedItem().getValue();
		if (level > 0) {
			Iterator<Akun> it = akuns.iterator();
			List<Akun> deletedAkuns = new ArrayList<Akun>();
			while (it.hasNext()) {
				Akun akun = it.next();
				List<Long> longs = new ArrayList<Long>();
				akunTreeModel.getChildDeepSet(akun.getId(), longs);
				if (level > longs.size()) {
					deletedAkuns.add(akun);
				}
			}
			akuns.removeAll(deletedAkuns);
		}

		// Long parentId = AkunTreeModel.checkForParent(tahun1, satuanKerja,
		// akuntingReportHelper.getMaxrevisi());

		final Akun selectedAkun = (Akun) akun.getAttribute("akun");
		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		for (final Akun akun : akuns) {
			if ((selectedAkun != null && selectedAkun.getId().equals(akun.getId())) || (selectedAkun == null)) {

				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("akun_id", akun.getId());
				map.put("unique_id", akun.getId());
				map.put("kode", akun.getKode() == null ? "" : akun.getKode());
				map.put("nama", akun.getNama() == null ? "" : akun.getNama());

				// map.put("harga_total", akun.getHargaTotal().equals(0.0) ?
				// null : akun.getHargaTotal());


				// for (Object[] myNumbers : numbers) {
				// map.put("total", myNumbers[0] == null ? 0L : ((Number)
				// myNumbers[0]).doubleValue());
				// map.put("harga_total_1", myNumbers[1] == null ? 0L :
				// ((Number) myNumbers[1]).doubleValue());
				// map.put("harga_total_2", myNumbers[2] == null ? 0L :
				// ((Number) myNumbers[2]).doubleValue());
				// map.put("harga_total_3", myNumbers[3] == null ? 0L :
				// ((Number) myNumbers[3]).doubleValue());
				// map.put("harga_total_4", myNumbers[4] == null ? 0L :
				// ((Number) myNumbers[4]).doubleValue());
				// map.put("harga_total_5", myNumbers[5] == null ? 0L :
				// ((Number) myNumbers[5]).doubleValue());
				// map.put("harga_total_6", myNumbers[6] == null ? 0L :
				// ((Number) myNumbers[6]).doubleValue());
				// map.put("harga_total_7", myNumbers[7] == null ? 0L :
				// ((Number) myNumbers[7]).doubleValue());
				// map.put("harga_total_8", myNumbers[8] == null ? 0L :
				// ((Number) myNumbers[8]).doubleValue());
				// map.put("harga_total_9", myNumbers[9] == null ? 0L :
				// ((Number) myNumbers[9]).doubleValue());
				// map.put("harga_total_10", myNumbers[10] == null ? 0L :
				// ((Number) myNumbers[10]).doubleValue());
				// map.put("harga_total_11", myNumbers[11] == null ? 0L :
				// ((Number) myNumbers[11]).doubleValue());
				// map.put("harga_total_12", myNumbers[12] == null ? 0L :
				// ((Number) myNumbers[12]).doubleValue());
				// }
				//
				// maps.add(map);
				// akuntingReportHelper.generateRencanaTiapBulanAnggaran(akun.getParentId(),
				// akunTreeModel, akun.getId(), akuns,
				// tahun1, maps);
			}
		}

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("satuan_kerja", satuanKerja.getNama());
		parameters.put("tahun", tahun1);
		parameters.put("maps", maps);
		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "akunting/neraca_lajur", ais.ui.util.WaktuUtil.getDate(),
					toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Neraca Lajur", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
