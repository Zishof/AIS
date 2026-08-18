package ais.action.master.penelitiandanpengabdian;

import java.util.Calendar;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.DetailArtikelHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.model.PengumumanAkademis;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLType;

public class ArtikelAction extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private DetailArtikelHelper detailArtikelHelper = new DetailArtikelHelper(null);

	@SuppressWarnings("unused")
	private boolean edit = false;
	@SuppressWarnings("unused")
	private boolean delete = false;

	public ArtikelAction() throws Exception {
		super();
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		MyWindow addWindowPengajuan = new MyWindow();
		addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		detailArtikelHelper.displayPengajuan(false, null, PengumumanAkademis.UNTUK_UMUM, null, this, addWindowPengajuan,
				"100%");

	}

	public ArtikelAction(String arg0, String arg1, boolean arg2) throws Exception {
		super(arg0, arg1, arg2);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		MyWindow addWindowPengajuan = new MyWindow();
		addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		detailArtikelHelper.displayPengajuan(false, null, PengumumanAkademis.UNTUK_UMUM, null, this, addWindowPengajuan,
				"100%");

	}

	public static CSLItemData generateCSLItemData(Artikel itemData) {
		CSLItemDataBuilder builder = new CSLItemDataBuilder().type(CSLType.BOOK).title(itemData.getNama());
		if (itemData.getTbmuser() != null && itemData.getTbmuser().getDosen() != null) {
			String[] pp = itemData.getTbmuser().getDosen().getNama().split(" ", 1);
			String given = pp[0];
			String family = pp.length > 1 ? pp[1] : "";
			builder.author(given, family);
		}

		if (itemData.getTanggalPublikasi() != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(itemData.getTanggalPublikasi());
			builder.issued(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DATE));
		}

		if (itemData.getIssn() != null && !itemData.getIssn().trim().isEmpty()) {
			builder.ISSN(itemData.getIssn());
		}
		if (itemData.getAbstrak() != null && !itemData.getAbstrak().trim().isEmpty()) {
			builder.abstrct(itemData.getAbstrak());
		}
		if (itemData.getCopyrightHolder() != null && !itemData.getCopyrightHolder().trim().isEmpty()) {
			builder.publisher(itemData.getCopyrightHolder());
		}

		CSLItemData item = builder.build();
		return item;
	}

	public static void tampilkanKutipan(final Artikel artikel) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final MyWindow window = new MyWindow("Kutipan", "true", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("300px");
				window.setWidth("90%");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);
				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("15%");

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				CSLItemData item = ArtikelAction.generateCSLItemData(artikel);
				String bibl = CSL.makeAdhocBibliography("ieee", item).makeString();

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("IEEE"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				bibl = CSL.makeAdhocBibliography("acm-siggraph", item).makeString();

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("ACM"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				bibl = CSL.makeAdhocBibliography("apa", item).makeString();

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("APA"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				bibl = CSL.makeAdhocBibliography("chicago-author-date", item).makeString();

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Chicago"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				bibl = CSL.makeAdhocBibliography("council-of-science-editors", item).makeString();

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("CSE"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				bibl = CSL.makeAdhocBibliography("modern-language-association", item).makeString();

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("MLA"));
				row.appendChild(new ais.ui.util.MyHtml(bibl));

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);

				borderlayout.setParent(window);

				window.onModal();
			}
		});

	}

}
