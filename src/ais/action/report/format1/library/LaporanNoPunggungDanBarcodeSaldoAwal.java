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
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.SaldoAwal;
import ais.database.model.library.SaldoAwalDetail;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyWindow;

public class LaporanNoPunggungDanBarcodeSaldoAwal extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private SaldoAwal saldoAwal;

	private MyIntbox mulai;
	private MyIntbox sampai;
	private MyTextbox judul;

	private Item item = null;

	public LaporanNoPunggungDanBarcodeSaldoAwal(SaldoAwal saldoAwal) {
		super();
		this.saldoAwal = saldoAwal;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan No Punggung Dan Barcode Saldo Awal", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanNoPunggungDanBarcodeSaldoAwal(Item item) {
		super();
		this.item = item;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan No Punggung Dan Barcode Saldo Awal", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanNoPunggungDanBarcodeSaldoAwal(SaldoAwal saldoAwal, Item item) {
		super();
		this.item = item;
		this.saldoAwal = saldoAwal;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan No Punggung Dan Barcode Saldo Awal", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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

		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		row.appendChild(mulai = new MyIntbox(0));
		mulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		row.appendChild(sampai = new MyIntbox(10));
		sampai.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(item == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul/ISBN"));
		row.appendChild(judul = new MyTextbox());
		judul.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		MyButtonConfig button = new MyButtonConfig("Tampilkan Barcode");
		button.setParent(row);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(null);
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
		}, "library/ddc_per_item_barcode", null, new EventListener() {

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

		if (saldoAwal == null && item != null && item.getId() != null) {
			List<ItemPunyaBarcode> itemPunyaBarcodes = session.createCriteria(ItemPunyaBarcode.class)
					.createAlias("item", "item")
					.add(judul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("item.isbn10", judul.getValue().trim()),
									Restrictions.or(Restrictions.ilike("item.isbn", judul.getValue().trim()),
											Restrictions.ilike("item.nama", judul.getValue().trim(),
													MatchMode.ANYWHERE))))

					.setFirstResult(mulai.getValue() == null ? 0 : mulai.getValue())
					.setMaxResults(sampai.getValue() == null ? 0 : sampai.getValue()).addOrder(Order.desc("id"))
					.add(Restrictions.eq("item", item)).list();

			for (ItemPunyaBarcode itemPunyaBarcode : itemPunyaBarcodes) {
				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("barcode", itemPunyaBarcode.getBarcode());
				map.put("isbn", itemPunyaBarcode.getItem().getIsbn());
				map.put("indexke", itemPunyaBarcode.getIndexke());
				map.put("nama", itemPunyaBarcode.getItem().getNama());
				map.put("deweydecimalclass", itemPunyaBarcode.getItem().getDeweyDecimalClass());
				map.put("pengarangs", itemPunyaBarcode.getItem().getPengarangs());
				isiParameterPunggung(map, itemPunyaBarcode.getItem());
				maps.add(map);
			}

		} else {

			List<SaldoAwalDetail> saldoAwalDetails = session.createCriteria(SaldoAwalDetail.class)
					.createAlias("item", "item")
					.add(judul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("item.isbn10", judul.getValue().trim()),
									Restrictions.or(Restrictions.ilike("item.isbn", judul.getValue().trim()),
											Restrictions.ilike("item.nama", judul.getValue().trim(),
													MatchMode.ANYWHERE))))

					.setFirstResult(mulai.getValue() == null ? 0 : mulai.getValue())
					.setMaxResults(sampai.getValue() == null ? 0 : sampai.getValue()).addOrder(Order.desc("id"))

					.add(saldoAwal == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("saldoAwal", saldoAwal))

					.add(item == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("item", item))

					.list();
			for (SaldoAwalDetail saldoAwalDetail : saldoAwalDetails) {
				List<ItemPunyaBarcode> itemPunyaBarcodes = session.createCriteria(ItemPunyaBarcode.class)
						.add(Restrictions.eq("item", saldoAwalDetail.getItem()))
						.add(Restrictions.eq("batchItemPunyaBarcode", saldoAwalDetail.getBatchItemPunyaBarcode()))
						.list();

				for (ItemPunyaBarcode itemPunyaBarcode : itemPunyaBarcodes) {
					Map<String, Object> map = new java.util.HashMap<String, Object>();
					map.put("barcode", itemPunyaBarcode.getBarcode());
					map.put("isbn", saldoAwalDetail.getItem().getIsbn());
					map.put("indexke", itemPunyaBarcode.getIndexke());
					map.put("nama", saldoAwalDetail.getItem().getNama());
					map.put("deweydecimalclass", saldoAwalDetail.getItem().getDeweyDecimalClass());
					map.put("pengarangs", saldoAwalDetail.getItem().getPengarangs());
					isiParameterPunggung(map, saldoAwalDetail.getItem());
					maps.add(map);
				}
			}
		}
		parameters.put("maps", maps);
		if (saldoAwal != null) {
			parameters.put("perpustakaan", saldoAwal.getPerpustakaan().getNama());
		} else {
			parameters.put("perpustakaan", "Perpustakaan");
		}
		return parameters;
	}

	/**
	 * Mengisi 4 komponen LABEL PUNGGUNG BUKU ke map data JRXML: punggungklasifikasi (No Klasifikasi /
	 * kode koleksi di rak), punggungpengarang (3 huruf nama belakang pengarang, HURUF BESAR),
	 * punggungjudul (huruf pertama judul, huruf kecil), dan punggungedisi (edisi/copy, mis. "C.1").
	 * Bila field khusus di item KOSONG, dipakai nilai TURUNAN lama (deweyDecimalClass / potongan
	 * pengarangs & nama / edisi) supaya item lama tetap tercetak benar.
	 */
	private static void isiParameterPunggung(Map<String, Object> map, Item it) {
		if (it == null) {
			return;
		}
		boolean manual = Boolean.TRUE.equals(it.getPunggungManual());
		String klas;
		String peng;
		String jdl;
		String eds;
		if (manual) {
			// MANUAL (checkbox "Nomor Punggung Manual" dicentang): pakai field entry. Bila salah satu
			// field kosong, jatuh ke nilai otomatis agar label tak kosong.
			klas = trimAtauNull(it.getPunggungKlasifikasi());
			if (klas == null) {
				klas = autoKlasifikasi(it);
			}
			peng = trimAtauNull(it.getPunggungPengarang());
			if (peng == null) {
				peng = autoPengarang(it);
			}
			jdl = trimAtauNull(it.getPunggungJudul());
			if (jdl == null) {
				jdl = autoJudul(it);
			}
			eds = trimAtauNull(it.getPunggungEdisi());
			if (eds == null) {
				eds = autoEdisi(it);
			}
		} else {
			// OTOMATIS (mode lama, default): semua diturunkan dari data item (pola LaporanBarcodeItem).
			klas = autoKlasifikasi(it);
			peng = autoPengarang(it);
			jdl = autoJudul(it);
			eds = autoEdisi(it);
		}
		map.put("punggungklasifikasi", klas == null ? "" : klas);
		map.put("punggungpengarang", peng == null ? "" : peng);
		map.put("punggungjudul", jdl == null ? "" : jdl);
		map.put("punggungedisi", eds == null ? "" : eds);
	}

	/** No Klasifikasi otomatis = kode DDC/klasifikasi item (deweydecimalclass). */
	private static String autoKlasifikasi(Item it) {
		return it.getDeweyDecimalClass();
	}

	/**
	 * 3 huruf pengarang otomatis mengikuti pola {@code LaporanBarcodeItem}: ambil KATA TERAKHIR
	 * (nama belakang) dari daftar pengarang lalu 3 huruf pertamanya, HURUF BESAR.
	 */
	private static String autoPengarang(Item it) {
		String p = it.getPengarangs() == null ? "" : it.getPengarangs().trim();
		if (p.length() > 3) {
			String[] a = p.split(" ");
			String last = a[a.length - 1];
			return (last.length() > 3 ? last.substring(0, 3) : last).toUpperCase();
		}
		return p.toUpperCase();
	}

	/** Huruf pertama judul otomatis (huruf KECIL, sesuai contoh "s"). */
	private static String autoJudul(Item it) {
		String n = it.getNama() == null ? "" : it.getNama().trim();
		return (n.length() > 1 ? n.substring(0, 1) : n).toLowerCase();
	}

	/** Edisi/copy otomatis = field edisi item (bila ada). */
	private static String autoEdisi(Item it) {
		String e = trimAtauNull(it.getEdisi());
		return e == null ? "" : e;
	}

	private static String trimAtauNull(String s) {
		if (s == null) {
			return null;
		}
		s = s.trim();
		return s.isEmpty() ? null : s;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "library/ddc_per_item_barcode",
					ais.ui.util.WaktuUtil.getDate(), null, toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan No Punggung Dan Barcode Saldo Awal", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
