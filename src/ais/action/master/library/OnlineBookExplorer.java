package ais.action.master.library;

import java.io.File;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

import ais.action.master.library.helper.OnlineBookTreeModel;
import ais.common.Common;
import ais.database.model.Konfigurasi;

public class OnlineBookExplorer extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7995108882641193063L;

	private Iframe iframe;
	private Tree tree;

	private OnlineBookTreeModel onlineBookTreeModel;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
			org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		init();
	}

	private void init() {

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol();
		treecol.setWidth("70px");
		treecol.setParent(treecols);

		treecol = new Treecol();
		treecol.setParent(treecols);

		treecols.setParent(tree);

		Konfigurasi konfigurasi = Common.getKonfigurasi("ONLINE_BOOK_DIR",
				Konfigurasi.AKTIF, "/opt/books", "", "");

		final File rootDir = new File(
				konfigurasi.getInfo1() == null ? "/opt/books"
						: konfigurasi.getInfo1());
		tree.setModel(onlineBookTreeModel = new OnlineBookTreeModel(rootDir));
		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {
			@Override
			public void render(Treeitem treeitem, Object arg1) throws Exception {
				Common.clear(treeitem);
				final File file = (File) arg1;
				final Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				if (!onlineBookTreeModel.isLeaf(file)) {
					treeitem.setImage("/img/books-icon_42.png");
					Treecell treecell = new Treecell(file.getName());
					treecell.setTooltiptext(file.getName());
					treecell.setParent(treerow);
				} else {
					treeitem.setImage("/img/address_book.png");
					Treecell treecell = new Treecell();
					treecell.setTooltiptext(file.getName());

					A a = new A(file.getName());
					a.setHref("#");
					treecell.appendChild(a);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Konfigurasi konfigurasi = Common
									.getKonfigurasi(
											"ONLINE_BOOK_ROOT_URL",
											Konfigurasi.AKTIF,
											"http://prajna.balitbang.kominfo.go.id/books",
											"", "");
							String urlLink = konfigurasi.getInfo1() == null ? "http://prajna.balitbang.kominfo.go.id/books"
									: konfigurasi.getInfo1();
							File temp = file;
							String subLink = "index.html";
							for (int i = 0; i < 100; i++) {
								subLink = (temp.getName() + "/" + subLink);
								temp = temp.getParentFile();
								if (temp.getName().equals(rootDir.getName())) {
									break;
								}
							}
							urlLink = urlLink + "/" + subLink;
							System.out.println("urlLink = " + urlLink);
							iframe.setSrc(urlLink);
						}
					});
					treecell.setParent(treerow);
				}
			}
		});
	}
}
