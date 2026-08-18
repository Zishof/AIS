package ais.action.report.format1.library;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.master.library.util.LibraryUtil;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.ItemPunyaPengarang;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyWindow;

public class LaporanBarcodeItem extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private Paging paging = new Paging();
	private Textbox cari;

	private MyGrid grid;

	Map<Long, Item> map = new java.util.HashMap<Long, Item>();

	private AmbilDataPerpustakaanBanbox perpustakaan;

	private MyTextbox barcode;

	private MyCheckboxConfig khususbarcode;

	private Perpustakaan p;

	public LaporanBarcodeItem() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Barcode Item", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void init() throws Exception {

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Cetak Punggung Buku");
		tab1.setParent(tabs);

		MyTabConfig tab12 = new MyTabConfig("Cetak Barcode Buku");
		tab12.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		final Tabpanel tabpanel12 = new ais.ui.util.MyTabpanel();
		tabpanel12.setParent(tabpanels);
		tab12.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel12.getChildren().size() == 0) {
					LaporanBarcodeSajaItem laporanKHS = new LaporanBarcodeSajaItem();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel12);
				}
			}
		});

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel1);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("450px");

		Row rowUtama1 = Common.tampilanScroll1(west);

		MyGrid mygrid = new MyGrid();// grid.setOddRowSclass("non-odd");
		mygrid.setSclass("fgrid");
		mygrid.setWidth("100%");
		mygrid.setParent(rowUtama1);
		mygrid.setWidth("100%");
		mygrid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(mygrid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("60px");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(mygrid);

		Row row = new Row();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Barcode: "));

		Hbox hbox = new Hbox();
		hbox.setParent(row);
		barcode = new MyTextbox();
		barcode.setCols(18);
		barcode.setRows(2);
		barcode.setParent(hbox);

		Common.initKeterangan(rows, "Pisahkan dengan spasi jika lebih dari satu barcode");

		MyButtonConfig button = new MyButtonConfig("Cetak Barcode");
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		});

		row = new Row();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig(""));
		row.appendChild(khususbarcode = new MyCheckboxConfig("Cetak khusus barcode yg tertera"));
		khususbarcode.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		});

		row = new Row();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Buku: "));

		hbox = new Hbox();
		hbox.setParent(row);

		hbox.appendChild(perpustakaan = new AmbilDataPerpustakaanBanbox());
		perpustakaan.setWidth("60px");

		cari = new Textbox();
		cari.setParent(hbox);
		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		button = new MyButtonConfig("Cari");
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Row r = new Row();
		r.setParent(rowUtama1.getParent());

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setSclass("dgrid");
		grid.setParent(r);
		grid.setWidth("100%");
		grid.setHeight("100%");

		columns = new Columns();
		columns.setParent(grid);
		column = new MyColumnConfig();
		column.setWidth("45px");
		column.setParent(columns);

		column = new MyColumnConfig("Foto");
		column.setWidth("65px");
		column.setParent(columns);

		column = new MyColumnConfig("ISBN/ISSN");
		column.setParent(columns);

		column = new MyColumnConfig("Judul");
		column.setParent(columns);

		column = new MyColumnConfig("Qty");
		column.setWidth("75px");
		column.setParent(columns);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North south = new North();
		south.setParent(borderlayout);
		south.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

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

		onSearchDefault(null);

		r = new Row();
		r.setParent(rowUtama1.getParent());

		paging.setParent(r);
		paging.setHeight("30px");
	}

	@SuppressWarnings("unchecked")
	protected void onSearchDefault(Object object) {
		Common.initPaging(initCriteria(false), paging);
		List<Item> item = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		List<Item> items = new ArrayList<Item>();
		items.addAll(map.values());
		items.addAll(item);
		ListModel strset = new SimpleListModel(items);
		grid.setRowRenderer(new ItemRenderer());
		grid.setModelCheckMobile(strset);
	}

	class ItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Item item = (Item) arg1;

			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setChecked(map.keySet().contains(item.getId()));
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						map.put(item.getId(), item);
					} else {
						map.remove(item.getId());
					}
				}
			});

			Image image = LibraryUtil.generateImage(item);
			image.setWidth("100%");
			image.setParent(arg0);

			new Label((item.getIsbn().isEmpty() ? item.getIsbn10() : item.getIsbn()) + " " + item.getIssn())
					.setParent(arg0);

			RevisiHelper.createNewRevisi(Item.class, item, item.getNama()).setParent(arg0);

			Html html = new ais.ui.util.MyHtml("load..");
			html.setParent(arg0);
			html.setContent(LibraryUtil.tersediaDi(item));

		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = null;
		if (!barcode.getValue().trim().isEmpty()) {

			String bbb = barcode.getValue().trim();
			bbb = org.apache.commons.lang3.StringUtils.replace(bbb, "\n", " ");
			String[] barcodes = bbb.split(" ");
			List<String> b = new ArrayList<String>();
			for (String bb : barcodes) {
				if (!bb.trim().isEmpty()) {
					b.add(bb.trim());
				}
			}

			Criterion criterion = Restrictions.ilike("item.nama", cari.getValue().trim(), MatchMode.ANYWHERE);
			criterion = Restrictions.or(criterion,
					Restrictions.ilike("item.isbn10", cari.getValue().trim(), MatchMode.ANYWHERE));
			criterion = Restrictions.or(criterion,
					Restrictions.ilike("item.isbn", cari.getValue().trim(), MatchMode.ANYWHERE));
			criterion = Restrictions.or(criterion,
					Restrictions.ilike("item.issn", cari.getValue().trim(), MatchMode.ANYWHERE));

			criteria = session.createCriteria(ItemPunyaBarcode.class).setProjection(Projections.groupProperty("item"))
					.add(b.size() > 1 ? Restrictions.in("barcode", b)
							: Restrictions.ilike("barcode", barcode.getValue().trim(), MatchMode.ANYWHERE))
					.createAlias("item", "item")
					.add(Restrictions.or(Restrictions.isNull("item.aktif"), Restrictions.eq("item.aktif", true)))
					.add(map.isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.not(Restrictions.in("item.id", map.keySet())))
					.add(cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : criterion);

		} else {

			criteria = session.createCriteria(Item.class)
					.add(Restrictions.sqlRestriction("this_.id in (select item from library.item_punya_barcode group by item)"))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

			if (order)
				criteria.addOrder(Order.asc("nama"));

			Criterion criterion = Restrictions.ilike("nama", cari.getValue().trim(), MatchMode.ANYWHERE);
			criterion = Restrictions.or(criterion,
					Restrictions.ilike("isbn10", cari.getValue().trim(), MatchMode.ANYWHERE));
			criterion = Restrictions.or(criterion,
					Restrictions.ilike("isbn", cari.getValue().trim(), MatchMode.ANYWHERE));
			criterion = Restrictions.or(criterion,
					Restrictions.ilike("issn", cari.getValue().trim(), MatchMode.ANYWHERE));

			criteria.add(map.isEmpty() ? Restrictions.sqlRestriction("true")
					: Restrictions.not(Restrictions.in("id", map.keySet())))
					.add(cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : criterion);
		}
		return criteria;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		p = (Perpustakaan) perpustakaan.getAttribute("perpustakaan");
//		if (p == null) {
//			return null;
//		}
		List list = new ArrayList();
		if (khususbarcode.isChecked()) {
			if (barcode.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Barcode harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return null;
			}

			String bbb = barcode.getValue().trim();
			bbb = org.apache.commons.lang3.StringUtils.replace(bbb, "\n", " ");
			String[] barcodes = bbb.split(" ");
			List<String> b = new ArrayList<String>();
			for (String bb : barcodes) {
				if (!bb.trim().isEmpty()) {
					b.add(bb.trim());
				}
			}

			Session session = HibernateUtil.currentSession();
			List<ItemPunyaBarcode> items = session.createCriteria(ItemPunyaBarcode.class)
					.add(Restrictions.in("barcode", b)).list();
			for (ItemPunyaBarcode itemPunyaBarcode : items) {
				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("barcode", itemPunyaBarcode.getBarcode());
				map.put("isbn", itemPunyaBarcode.getItem().getIsbn());
				map.put("nama", itemPunyaBarcode.getItem().getNama());
				map.put("deweydecimalclass", itemPunyaBarcode.getItem().getDeweyDecimalClass());
				map.put("pengarangs", itemPunyaBarcode.getItem().getPengarangs());
				map.put("perpustakaan", itemPunyaBarcode.getPerpustakaan().getNama());

				String code = (itemPunyaBarcode.getPerpustakaan() == null ? ""
						: "\n" + itemPunyaBarcode.getPerpustakaan().getNama()) + "\n"
						+ itemPunyaBarcode.getItem().getIsbn() + " - " + itemPunyaBarcode.getItem().getNama();
				map.put("code", code);

				map.put("c_code", itemPunyaBarcode.getBarcode());

				Item item = itemPunyaBarcode.getItem();
				if (item != null && item.getPengarangs().isEmpty()) {

					session.refresh(item);
					List<String> strings = session.createCriteria(ItemPunyaPengarang.class)
							.createAlias("pengarang", "pengarang").setProjection(Projections.property("pengarang.nama"))
							.add(Restrictions.eq("item", item)).list();
					String pengarangs = strings.toString().replaceAll("\\[", "").replaceAll("\\]", "");
					System.out.println("pengarangs=> " + pengarangs);

					item.setPengarangs(pengarangs.trim().equals("") ? "None" : pengarangs);

					session.update(item);
					session.flush();
				}

				try {
					if (item.getPengarangs().length() > 3) {
						String[] a = item.getPengarangs().split(" ");
						map.put("pengarang_3_huruf", a[a.length - 1].substring(0, 3));
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanBarcodeItem.java:477");
					// TODO: handle exception
				}
				map.put("judul_1_huruf", item.getNama().length() > 1 ? item.getNama().substring(0, 1) : "");

				Common.insertProperty(Item.class, item, map, "data");

				if (p != null) {
					p = itemPunyaBarcode.getPerpustakaan();
				}
				list.add(map);
			}
			items = null;
		} else {

			for (Item item : map.values()) {
				siapkanParemeter(item, list, p);
			}
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("perpustakaan", p == null ? "" : p.getNama());
		parameters.put("maps", list);

		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void siapkanParemeter(Item item, List list, Perpustakaan perpustakaan) throws Exception {
		Session session = HibernateUtil.currentSession();
		List<ItemPunyaBarcode> itemPunyaBarcodes = session.createCriteria(ItemPunyaBarcode.class)
				.add(perpustakaan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("perpustakaan", perpustakaan))
				.add(Restrictions.eq("item", item)).addOrder(Order.desc("id")).list();

		for (ItemPunyaBarcode itemPunyaBarcode : itemPunyaBarcodes) {
			if (itemPunyaBarcode.getBarcode() != null && !itemPunyaBarcode.getBarcode().trim().isEmpty()) {
				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("barcode", itemPunyaBarcode.getBarcode());
				map.put("isbn", itemPunyaBarcode.getItem().getIsbn());
				map.put("nama", itemPunyaBarcode.getItem().getNama());
				map.put("deweydecimalclass", itemPunyaBarcode.getItem().getDeweyDecimalClass());
				map.put("pengarangs", itemPunyaBarcode.getItem().getPengarangs());
				map.put("perpustakaan", itemPunyaBarcode.getPerpustakaan().getNama());

				String code = (itemPunyaBarcode.getPerpustakaan() == null ? ""
						: "\n" + itemPunyaBarcode.getPerpustakaan().getNama()) + "\n"
						+ itemPunyaBarcode.getItem().getIsbn() + " - " + itemPunyaBarcode.getItem().getNama();
				map.put("code", code);

				map.put("c_code", itemPunyaBarcode.getBarcode());

				if (p != null) {
					p = itemPunyaBarcode.getPerpustakaan();
				}

				item = itemPunyaBarcode.getItem();
				if (item != null && item.getPengarangs().isEmpty()) {

					session.refresh(item);
					List<String> strings = session.createCriteria(ItemPunyaPengarang.class)
							.createAlias("pengarang", "pengarang").setProjection(Projections.property("pengarang.nama"))
							.add(Restrictions.eq("item", item)).list();
					String pengarangs = strings.toString().replaceAll("\\[", "").replaceAll("\\]", "");
					System.out.println("pengarangs=> " + pengarangs);

					item.setPengarangs(pengarangs.trim().equals("") ? "None" : pengarangs);

					session.update(item);
					session.flush();
				}

				try {
					if (item.getPengarangs().length() > 3) {
						String[] a = item.getPengarangs().split(" ");
						map.put("pengarang_3_huruf", a[a.length - 1].substring(0, 3));
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanBarcodeItem.java:554");
					// TODO: handle exception
				}
				map.put("judul_1_huruf", item.getNama().length() > 1 ? item.getNama().substring(0, 1) : "");

				Common.insertProperty(Item.class, item, map, "data");

				list.add(map);
			}
		}
		itemPunyaBarcodes = null;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					String namaFile = "library/ddc_per_item_barcode";

					p = null;

					File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), namaFile,
							ais.ui.util.WaktuUtil.getDate(), null, toolbar);
					CommonReport.tampilkanReportPDF(center, file);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Barcode Item", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
							new String[] {
								"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
								"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});

	}

}
