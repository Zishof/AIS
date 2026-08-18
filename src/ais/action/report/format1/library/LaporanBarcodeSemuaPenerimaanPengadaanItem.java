package ais.action.report.format1.library;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.PenerimaanPengadaanItemDetail;
import ais.ui.util.MyWindow;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

public class LaporanBarcodeSemuaPenerimaanPengadaanItem extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;



	public LaporanBarcodeSemuaPenerimaanPengadaanItem() {
		super();
		
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Barcode Semua Penerimaan Pengadaan Item", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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

		final Map parameters = ais.common.HashMapGenerator.getRand();
		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		Session session = HibernateUtil.currentSession();
		List<PenerimaanPengadaanItemDetail> penerimaanPengadaanItemDetails = session.createCriteria(PenerimaanPengadaanItemDetail.class)
				.createAlias("penerimaanPengadaanItem", "penerimaanPengadaanItem")
				.add(Restrictions.isNotNull("penerimaanPengadaanItem.disetujuiOleh"))
				.add(Restrictions.isNotNull("penerimaanPengadaanItem.tanggalPersetujuan"))
				.addOrder(Order.desc("id")).list();
		for (PenerimaanPengadaanItemDetail penerimaanPengadaanItemDetail : penerimaanPengadaanItemDetails) {
			List<ItemPunyaBarcode> itemPunyaBarcodes = session.createCriteria(ItemPunyaBarcode.class)
					.add(Restrictions.eq("item", penerimaanPengadaanItemDetail.getItem()))
					.add(Restrictions.eq("batchItemPunyaBarcode", penerimaanPengadaanItemDetail.getBatchItemPunyaBarcode())).list();

			for (ItemPunyaBarcode itemPunyaBarcode : itemPunyaBarcodes) {
				Map<String, Object> map = new java.util.HashMap<String, Object>();
				String code = (itemPunyaBarcode.getPerpustakaan() == null ? ""
						: "\n" + itemPunyaBarcode.getPerpustakaan().getNama()) + "\n"
						+ itemPunyaBarcode.getItem().getIsbn() + " - " + itemPunyaBarcode.getItem().getNama();
				map.put("code", code);

				final File myfile = new File(Common.ambilREAL_PATH_REPORT() + "/barcode_"
						+ itemPunyaBarcode.getBarcode() + ".png");

				if (!myfile.exists()) {
					Barcode mybarcode = BarcodeFactory.createCode128(itemPunyaBarcode.getBarcode());
					BarcodeImageHandler.savePNG(mybarcode, myfile);
				}

				map.put("barcode", myfile.getAbsolutePath());
				map.put("c_code", itemPunyaBarcode.getBarcode());
				maps.add(map);
			}
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
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Barcode Semua Penerimaan Pengadaan Item", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
