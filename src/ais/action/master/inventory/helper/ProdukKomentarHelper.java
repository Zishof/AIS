package ais.action.master.inventory.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.ProdukKomentar;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class ProdukKomentarHelper {

	private MyGrid gridKomentar;
	// private boolean add = false;
	// private boolean edit = false;
	private boolean delete = false;

	public ProdukKomentarHelper(MyGrid gridKomentar) {
		this.gridKomentar = gridKomentar;
		// add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		// edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Borderlayout initDetail(final Produk produk) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		org.zkoss.zul.North northInfo = new org.zkoss.zul.North();
		northInfo.setParent(borderlayout);
		northInfo.setBorder("none");
		buildInfoHtmlInventoryV1("Komentar Produk", "Daftar ini membantu pengelola melihat masukan, kontak, dan catatan pelanggan terhadap suatu produk. Informasi komentar dapat menjadi bahan evaluasi kualitas produk maupun layanan toko.").setParent(northInfo);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridKomentar);
		gridKomentar.setParent(center);
		gridKomentar.setWidth("100%");
		gridKomentar.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridKomentar);

		MyColumnConfig column = new MyColumnConfig("Komentar");
		column.setParent(columns);

		column = new MyColumnConfig("Oleh");
		column.setParent(columns);

		column = new MyColumnConfig("Email");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("20%");

		loadDataDetail(produk);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Produk produk) {

		List<ProdukKomentar> produkKomentars = produk == null || produk.getId() == null
				? new ArrayList<ProdukKomentar>()
				: HibernateUtil.currentSession().createCriteria(ProdukKomentar.class)
						.add(Restrictions.eq("produk", produk)).list();

		Rows rows = gridKomentar.getRows() == null ? new Rows() : gridKomentar.getRows();
		rows.setParent(gridKomentar);

		for (ProdukKomentar produkKomentar : produkKomentars) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, produkKomentar);
		}
	}

	public void initRow(final Row row, final ProdukKomentar produkKomentar) {
		row.setValign("top");row.setAttribute("produkKomentar", produkKomentar);

		new Label(produkKomentar.getNama()).setParent(row);
		new Label(produkKomentar.getKontak()).setParent(row);
		new Label(produkKomentar.getEmail()).setParent(row);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (produkKomentar.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(produkKomentar);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}


	private org.zkoss.zul.Html buildInfoHtmlInventoryV1(String judul, String deskripsi) {
		return new org.zkoss.zul.Html("<div style=\"padding:10px 12px;margin:4px 0;border-radius:12px;"
				+ "background:#f8fafc;border:1px solid #e2e8f0;color:#475569;font-size:11.5px;line-height:1.55;\">"
				+ "<b style=\"color:#0f172a;\">" + escapeHtmlInventoryV1(judul) + "</b><br/>"
				+ escapeHtmlInventoryV1(deskripsi) + "</div>");
	}

	private String escapeHtmlInventoryV1(String value) {
		if (value == null) {
			return "";
		}
		String s = value;
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\"", "&quot;");
		s = s.replace("'", "&#39;");
		return s;
	}

}
