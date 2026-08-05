package ais.action.report.helper.asset;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

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

import ais.action.master.asset.LokasiAction;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Ruang;
import ais.database.model.asset.JenisAsset;
import ais.database.model.asset.KelompokAsset;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.PemilikAsset;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class LaporanPenyusutan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Combobox pemilikAsset;
	private Combobox lokasi;
	private AmbilDataRuangBanbox ruang;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private Combobox jenisAsset;
	private Combobox kelompokAsset;

	private Center center;
	private Toolbar toolbar;

	public LaporanPenyusutan() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Penyusutan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pemilik"));
		row.appendChild(pemilikAsset = new Combobox());
		Common.insertComboDanSemua(pemilikAsset, new String[] { "nama", "id" }, "keterangan", PemilikAsset.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		pemilikAsset.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi"));
		row.appendChild(lokasi = new Combobox());
		Common.insertComboDanSemua(lokasi, new String[] { "nama" }, "alamat", Lokasi.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		lokasi.setWidth("90%");
		LokasiAction.kunciLokasi(lokasi);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang"));
		row.appendChild(ruang = new AmbilDataRuangBanbox());
		ruang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
		row.appendChild(jenisAsset = new Combobox());
		Common.insertComboDanSemua(jenisAsset, new String[] { "nama" }, JenisAsset.class);
		jenisAsset.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok"));
		row.appendChild(kelompokAsset = new Combobox());
		Common.insertComboDanSemua(kelompokAsset, new String[] { "nama" }, KelompokAsset.class);
		kelompokAsset.setWidth("90%");

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengadaan Mulai"));
		row.appendChild(mulai = new MyDatebox(calendar.getTime()));
		mulai.setReadonly(true);
		mulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengadaan Sampai"));
		row.appendChild(sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		sampai.setReadonly(true);
		sampai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		MyButtonConfig button;
		row.appendChild(button = new MyButtonConfig("Tampilkan Laporan"));
		button.addEventListener("onClick", new EventListener() {

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
		}, "asset/penyusutan", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		PemilikAsset myPemilikAsset = (PemilikAsset) (pemilikAsset.getSelectedItem() == null || pemilikAsset.getSelectedItem().getValue() == null ? null
				: pemilikAsset.getSelectedItem().getValue());
		Lokasi myLokasi = (Lokasi) (lokasi.getSelectedItem() == null || lokasi.getSelectedItem().getValue() == null ? null : lokasi.getSelectedItem().getValue());

		JenisAsset myJenisAsset = (JenisAsset) (jenisAsset.getSelectedItem() == null || jenisAsset.getSelectedItem().getValue() == null ? null
				: jenisAsset.getSelectedItem().getValue());
		KelompokAsset myKelompokAsset = (KelompokAsset) (kelompokAsset.getSelectedItem() == null || kelompokAsset.getSelectedItem().getValue() == null ? null
				: kelompokAsset.getSelectedItem().getValue());

		Ruang myRuang = (Ruang) ruang.getAttribute("ruang");
		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("pemilikAsset", myPemilikAsset == null || myPemilikAsset.getId() == null ? -1L : myPemilikAsset.getId());
		parameters.put("lokasi", myLokasi == null || myLokasi.getId() == null ? -1L : myLokasi.getId());
		parameters.put("ruang", myRuang == null || myRuang.getId() == null ? -1L : myRuang.getId());
		parameters.put("jenis", myJenisAsset == null || myJenisAsset.getId() == null ? -1L : myJenisAsset.getId());
		parameters.put("kelompok", myKelompokAsset == null || myKelompokAsset.getId() == null ? -1L : myKelompokAsset.getId());
		parameters.put("mulai", mulai.getValue());
		parameters.put("sampai", sampai.getValue());
		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "asset/penyusutan", ais.ui.util.WaktuUtil.getDate(),
					toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Penyusutan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
