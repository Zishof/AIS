package ais.action.report.format1.keuangan;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.JenisKegiatan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanRekapitulasiItemBiaya extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1662498263126327093L;
	private Combobox jenisKegiatan;
	private Combobox tahunAkademik;
	private Center center;
	private Toolbar toolbar;
	private MyDatebox mulai;
	private MyDatebox sampai;

	public LaporanRekapitulasiItemBiaya() {
		super();
		try {
			jenisKegiatan = new Combobox();
			Common.insertComboDanSemua(jenisKegiatan, "namaKegiatan", JenisKegiatan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			// Common.generateTahunAjaranDanSemua(tahunAkademik);
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi Item Biaya", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void init() {

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onRekap(event);

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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));
		row.appendChild(jenisKegiatan);
		jenisKegiatan.setWidth("90%");
		jenisKegiatan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		LaporanRekapitulasiItemBiaya.this.tahunAkademik = Common
				.generateTahunAjaran(LaporanRekapitulasiItemBiaya.this.tahunAkademik);
		row.appendChild(LaporanRekapitulasiItemBiaya.this.tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", eventListener);

		Calendar calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		row.appendChild(mulai = new MyDatebox(calendar.getTime()));
		mulai.setReadonly(true);
		mulai.addEventListener("onChange", eventListener);

		calendar = WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai"));
		row.appendChild(sampai = new MyDatebox(calendar.getTime()));
		sampai.setReadonly(true);
		sampai.addEventListener("onChange", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (tahunAkademik.getValue() == null) {
					MyMessageboxConfig.show("Tahun akademik harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				Map parameters = generateParameter();
				return parameters;
			}
		}, "Rekapitulasi_item_biaya", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRekap(arg0);
			}
		}));

		onRekap(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (tahunAkademik.getValue() == null) {
			return null;
		}

		final Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("mulai", mulai.getValue());
		parameters.put("sampai", sampai.getValue());
		parameters.put("periode",
				Common.dateFormat6.get().format(mulai.getValue()) + " s.d " + Common.dateFormat6.get().format(sampai.getValue()));
		parameters.put("mulai_s", Common.databaseDateFormat.get().format(mulai.getValue()));
		parameters.put("sampai_s", Common.databaseDateFormat.get().format(sampai.getValue()));

		parameters.put("tahun_akademik",
				tahunAkademik.getSelectedItem().getValue() == null ? "" : tahunAkademik.getSelectedItem().getValue());

		JenisKegiatan jenisKegiatanObj = null;
		if (jenisKegiatan.getSelectedItem() != null)
			jenisKegiatanObj = (JenisKegiatan) jenisKegiatan.getSelectedItem().getValue();

		parameters.put("item_biaya", jenisKegiatanObj == null || jenisKegiatanObj.getId() == null ? -1L : jenisKegiatanObj.getId());
		return parameters;
	}

	public void onRekap(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Rekapitulasi_item_biaya",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekapitulasi Item Biaya", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
