package ais.action.master.library;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.library.helper.TampilanHasilScanPerHalamanWindow;
import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaPengarang;
import ais.database.model.library.JenisItem;
import ais.database.model.library.TipeAnggota;
import ais.database.model.library.TipeItem;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KatalogOnlineAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	protected MyWindow addWindow;
	protected Paging paging;
	protected MyGrid grid;

	protected Textbox searchbahasa;
	protected Textbox searchisbn;
	protected Textbox searchissn;
	protected Textbox searchnama;
	protected Textbox searchtema;
	protected Textbox searchedisi;
	protected Textbox searchpengarang;
	protected Textbox searchcatatan;
	protected Textbox searchpenerbit;
	protected Combobox searchjenisItem;
	protected Combobox searchtipeItem;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		// Themes.setTheme(execution, "sapphire");

		Common.initLaguage();

		Session session = HibernateUtil.currentSession();
		Integer count = ((Number) session.createCriteria(Item.class)
				.add(Restrictions.or(Restrictions.isNull("pengarangs"),
						Restrictions.sqlRestriction("trim(pengarangs) = ''")))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (!count.equals(0)) {
			List<Item> items = session.createCriteria(Item.class).add(Restrictions.or(Restrictions.isNull("pengarangs"),
					Restrictions.sqlRestriction("trim(pengarangs) = ''"))).list();
			for (Item item : items) {
				List<String> strings = session.createCriteria(ItemPunyaPengarang.class)
						.createAlias("pengarang", "pengarang").setProjection(Projections.property("pengarang.nama"))
						.add(Restrictions.eq("item", item)).list();
				String pengarangs = strings.toString().replaceAll("\\[", "").replaceAll("\\]", "");
				item.setPengarangs(pengarangs.trim().equals("") ? "None" : pengarangs);
				Common.refreshUpdate(session, (item));
			}
		}

		@SuppressWarnings("unused")
		TipeAnggota tipeAnggota = LibraryUtil.UMUM;
		Common.insertCombo(searchtipeItem, "nama", TipeItem.class, "font-size: xx-large;");
		Common.insertCombo(searchjenisItem, "nama", JenisItem.class, "font-size: xx-large;");

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

	}

	class ItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Item item = (Item) arg1;
			arg0.setValign("top");

			if (item.getImageUrl() == null || item.getImageUrl().trim().equals("")) {
				Image image = new Image(CommonMedia.getMediaItem(item.getId(), 168, 168, true));
				image.setWidth("168px");
				image.setParent(arg0);
			} else {
				Image image = new Image(item.getImageUrl());
				image.setWidth("168px");
				image.setParent(arg0);
			}

			String penerbits = "";
			penerbits += item.getPenerbit() == null ? "" : item.getPenerbit().getNama() + "<br>";
			penerbits += item.getPenerbit2() == null ? "" : item.getPenerbit2().getNama() + "<br>";
			penerbits += item.getPenerbit3() == null ? "" : item.getPenerbit3().getNama() + "<br>";
			penerbits += item.getPenerbit4() == null ? "" : item.getPenerbit4().getNama() + "<br>";
			penerbits += item.getPenerbit5() == null ? "" : item.getPenerbit5().getNama() + "<br>";

			String label = "<ol>" + "<li>Judul : " + (item.getNama()) + "</li><li>ISBN 10 : "
					+ (item.getIsbn10() == null ? "" : item.getIsbn10()) + "</li><li>ISBN 13 : "
					+ (item.getIsbn() == null ? "" : item.getIsbn()) + "</li><li>ISSN : "
					+ (item.getIssn() == null ? "" : item.getIssn()) + "</li><li>Call Number (DDC) : "
					+ (item.getDdcItem() == null ? "" : item.getDdcItem()) + "</li><li>Call Number (UDC) : "
					+ (item.getUdcItem() == null ? "" : item.getUdcItem()) + "</li><li>Jenis : "
					+ (item.getJenisItem() == null ? "" : item.getJenisItem().getNama())

					+ "</li><li>Penerbit : " + (penerbits)

					+ "</li><li>Kategori : " + (item.getKategories())

					+ "</li><li>Bahasa : " + (item.getBahasa())

					+ "</li><li>Pengarang : " + (item.getPengarangs())

					+ "</li><li>Penaklikan : " + (item.getPenaklikan())

					+ "</li><li>Tahun : " + (item.getTahun())

					+ "</li><li>Tersedia di : " + (LibraryUtil.tersediaDi(item)) + "</ol>";

			new ais.ui.util.MyHtml(label).setParent(arg0);

			label = "<ol>" + "<li>Ringkasan Indonesia : " + (item.getAbstrak()) + "</li><li>Summary English : "
					+ (item.getAbstrakEn()) + "</ol>";

			new ais.ui.util.MyHtml(label).setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/Google-icon-big.png");
			button.setTooltiptext("Lihat Isi Buku via Google");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					LibraryUtil.laporanHTML(item.getIsbn(), item.getNama());
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/Books-icon-big.png");
			button.setTooltiptext("Lihat Isi Buku");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						TampilanHasilScanPerHalamanWindow halamanWindow = new TampilanHasilScanPerHalamanWindow(
								"Isi Buku", "none", true);
						page.getFirstRoot().appendChild(halamanWindow);
						halamanWindow.init(item);
						halamanWindow.onModal();
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}

			});
			button.setParent(toolbar);

			toolbar.setParent(arg0);
		}

	}

	protected Criteria initCriteria(boolean order) {

		Criterion pen = Restrictions.sqlRestriction("false");
		if (!searchpenerbit.getValue().trim().equals("")) {
			pen = Restrictions.or(pen,
					Restrictions.ilike("penerbit.nama", searchpenerbit.getValue().trim(), MatchMode.ANYWHERE));
			pen = Restrictions.or(pen,
					Restrictions.ilike("penerbit2.nama", searchpenerbit.getValue().trim(), MatchMode.ANYWHERE));
			pen = Restrictions.or(pen,
					Restrictions.ilike("penerbit3.nama", searchpenerbit.getValue().trim(), MatchMode.ANYWHERE));
			pen = Restrictions.or(pen,
					Restrictions.ilike("penerbit4.nama", searchpenerbit.getValue().trim(), MatchMode.ANYWHERE));
			pen = Restrictions.or(pen,
					Restrictions.ilike("penerbit5.nama", searchpenerbit.getValue().trim(), MatchMode.ANYWHERE));
		}

		Criterion catat = Restrictions.sqlRestriction("false");
		if (!searchcatatan.getValue().trim().equals("")) {
			catat = Restrictions.or(catat,
					Restrictions.ilike("abstrak", searchcatatan.getValue().trim(), MatchMode.ANYWHERE));
			catat = Restrictions.or(catat,
					Restrictions.ilike("catatan", searchcatatan.getValue().trim(), MatchMode.ANYWHERE));
			catat = Restrictions.or(catat,
					Restrictions.ilike("abstrakEn", searchcatatan.getValue().trim(), MatchMode.ANYWHERE));

			catat = Restrictions.or(catat,
					Restrictions.ilike("kewords", searchcatatan.getValue().trim(), MatchMode.ANYWHERE));
			catat = Restrictions.or(catat,
					Restrictions.ilike("kewordsEn", searchcatatan.getValue().trim(), MatchMode.ANYWHERE));
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Item.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.createAlias("penerbit", "penerbit", Criteria.LEFT_JOIN)
				.createAlias("penerbit2", "penerbit2", Criteria.LEFT_JOIN)
				.createAlias("penerbit3", "penerbit3", Criteria.LEFT_JOIN)
				.createAlias("penerbit4", "penerbit4", Criteria.LEFT_JOIN)
				.createAlias("penerbit5", "penerbit5", Criteria.LEFT_JOIN)
				.add(Restrictions.isNull("defaultSatuanKerja"))
				.add(searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchbahasa.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("bahasa", searchbahasa.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchisbn.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("isbn", searchisbn.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("isbn10", searchisbn.getValue().trim(), MatchMode.ANYWHERE)))
				.add(searchissn.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("issn", searchissn.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchedisi.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("edisi", searchedisi.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchpengarang.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("pengarangs", searchpengarang.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchcatatan.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1") : catat)
				.add(searchpenerbit.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1") : pen)
				.add(searchjenisItem.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisItem", searchjenisItem.getSelectedItem().getValue()))
				.add(searchtipeItem.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tipeItem", searchtipeItem.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnama == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<Item> item = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(item);
		grid.setRowRenderer(new ItemRenderer());
		grid.setModelCheckMobile(strset);

	}

}
