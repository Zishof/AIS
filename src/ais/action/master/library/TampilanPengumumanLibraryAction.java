package ais.action.master.library;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.PengumumanAkademisAction;
import ais.action.master.TampilanPengumumanAkademisAction;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.DiskusiPengumumanAkademisDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DiskusiPengumumanAkademis;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranPengumumanAkademis;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk tampilan pengumuman library. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PengumumanAkademis pengumumanAkademis},
 * {@code MyDatebox searchmulai}, {@code MyDatebox searchsampai}, {@code Textbox judul}, {@code Textbox catatan},
 * {@code Textbox oleh}, {@code MyGrid grid}, {@code MyGrid gridKomentar}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()}, {@code init()}); pembacaan/pencarian
 * ({@code loadMenu()}, {@code loadData()}, {@code onSearchDefault()}, {@code loadDataAttachment()}, {@code
 * loadData()}, {@code getReadonly()}); mutasi data ({@code prosess()}, {@code onSave()}, {@code setReadonly()},
 * {@code setMenu()}); operasi domain lain ({@code displayDetailPertemuanFileContent()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class TampilanPengumumanLibraryAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2301873239699174688L;
	private PengumumanAkademis pengumumanAkademis;

	private MyDatebox searchmulai;
	private MyDatebox searchsampai;

	private Textbox judul;
	private Textbox catatan;
	private Textbox oleh;

	private MyGrid grid;
	private MyGrid gridKomentar;
	private MyGrid grids;

	private Boolean readonly = false;

	private DiskusiPengumumanAkademis diskusiPengumumanAkademis;
	private MyWindow addWindow;
	private Paging paging;
	private Rows rows;
	private Textbox cari;

	private Tabpanels tabpanels;
	private West menu;
	private Tabs tabs;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		if (searchmulai != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 12);
			searchmulai.setValue(calendar.getTime());
		}

		if (searchsampai != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 6);
			searchsampai.setValue(calendar.getTime());
		}

		loadMenu();
		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	private void loadMenu() {

		if (menu == null || tabpanels == null || tabs == null) {
			return;
		}

		Borderlayout subBorderlayout = new ais.ui.util.MyBorderlayout();
		subBorderlayout.setParent(menu);

		North subNorth = new North();
		subNorth.setParent(subBorderlayout);
		subNorth.setHeight("25px");
		subNorth.setBorder("none");

		Borderlayout subSubBorderlayout = new ais.ui.util.MyBorderlayout();
		subSubBorderlayout.setParent(subNorth);

		West subSubwest = new West();
		subSubwest.setParent(subSubBorderlayout);
		subSubwest.setWidth("80%");
		subSubwest.setBorder("none");

		cari = new Textbox();
		cari.setWidth("90%");
		cari.setParent(subSubwest);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.setWidth("90%");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(cari.getValue().trim());
			}
		});

		Center subsubcenter = new Center();
		subsubcenter.setParent(subSubBorderlayout);
		ais.ui.util.ZkCompat.setFlex(subsubcenter, true);
		button.setParent(subsubcenter);
		subsubcenter.setBorder("none");

		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(cari.getValue().trim());
			}
		});

		Center subcenter = new Center();
		subcenter.setParent(subBorderlayout);
		ais.ui.util.ZkCompat.setFlex(subcenter, true);
		subcenter.setBorder("none");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(subcenter);
		grid.setWidth("100%");
		grid.setHeight("100%");

		rows = new Rows();
		rows.setParent(grid);
		loadData(cari.getValue());

		String script = Common.getKonfigurasi("embeded_script_pada_halaman_perpustakaan",
				"<script id=\"cid0020000096856442526\" data-cfasync=\"false\" async src=\"http://st.chatango.com/js/gz/emb.js\" style=\"width: 100%;height: 100%;\">{\"handle\":\"zishof-ecampus\",\"arch\":\"js\",\"styles\":{\"a\":\"33ccff\",\"b\":100,\"c\":\"000000\",\"d\":\"000000\",\"k\":\"33ccff\",\"l\":\"33ccff\",\"m\":\"33ccff\",\"p\":\"10\",\"q\":\"33ccff\",\"r\":100,\"surl\":0,\"cnrs\":\"0.35\",\"fwtickm\":1}}</script>")
				.getNilai();
		if (!script.trim().isEmpty()) {
			South south = new South();
			south.setParent(subBorderlayout);
			south.setHeight("60%");
			south.appendChild(new ais.ui.util.MyHtml(script));
		}

	}

	@SuppressWarnings("unchecked")
	public void loadData(String keyword) {
		
		String currentLang = null;
		try {
			currentLang = (String) Sessions.getCurrent(true).getAttribute("current_lang");
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (currentLang == null) {
			currentLang = Tbmuser.INDONESIA;
		}

		Common.clear(rows);

		List<PengumumanAkademis> pengumumanAkademises = initCriteria(true).setMaxResults(50).list();

		for (final PengumumanAkademis pengumumanAkademis : pengumumanAkademises) {

			final MyFormRow row = new MyFormRow();row.setValign("top");
			row.setStyle("border:0px;background: transparent;font-size: xx-small;");

			row.setParent(rows);

			String text = "";
			if (currentLang.equals(Tbmuser.INDONESIA)) {
				text = pengumumanAkademis.getJudul();
			} else if (currentLang.equals(Tbmuser.ENGLISH)) {
				text = pengumumanAkademis.getJudulEn();
			}

			text = text.length() > 255 ? text.substring(0, 254) + ".." : text;

			final A toolbarbutton = new A(text);
			toolbarbutton.setStyle("font-size: xx-small;");

			row.appendChild(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Clients.scrollIntoView(row);
					prosess(pengumumanAkademis);
				}
			});

		}

	}

	@SuppressWarnings("unchecked")
	private void prosess(final PengumumanAkademis pengumumanAkademis) {
		List<Tabpanel> tabpanels = this.tabpanels.getChildren();
		synchronized (tabpanels) {
			for (Tabpanel myTabpanel : tabpanels) {

				if (myTabpanel.getAttribute("pengumumanAkademis") == null) {
					continue;
				}

				PengumumanAkademis myPengumumanAkademis = (PengumumanAkademis) myTabpanel
						.getAttribute("pengumumanAkademis");

				if (myPengumumanAkademis.getId().toString().equals(pengumumanAkademis.getId().toString())) {
					myTabpanel.getLinkedTab().setSelected(true);
					return;
				}

			}

			final MyTabConfig tab = new MyTabConfig(pengumumanAkademis.getJudul());
			final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			tab.setClosable(false);
			tab.setParent(tabs);

			tabpanel.setParent(this.tabpanels);
			tabpanel.setAttribute("pengumumanAkademis", pengumumanAkademis);

			Borderlayout subSubBorderlayout = new ais.ui.util.MyBorderlayout();
			subSubBorderlayout.setParent(tabpanel);

			Center subcenter = new Center();
			subcenter.setParent(subSubBorderlayout);
			ais.ui.util.ZkCompat.setFlex(subcenter, true);
			subcenter.setBorder("none");

			MyGrid grids = new MyGrid();
			grids.setMold("paging");
			grids.setParent(subcenter);
			grids.setSclass("fgrid");

			Columns columns = new Columns();

			columns.setParent(grids);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("0px");

			column = new MyColumnConfig();
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grids);

			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);

			final MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);
			displayDetailPertemuanFileContent(pengumumanAkademis, detail);

			final Vbox vbox = new Vbox();
			vbox.setHeight("100%");
			vbox.setWidth("100%");
			vbox.setParent(row);

			Tbmuser tbmuser = Common.getCurrentUser();
			PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			Sekolah sekolah = SekolahUtil.getSekolah();
			try {
				PengumumanAkademisAction.tampilPengumuman(rows, pengumumanAkademis, sekolah, tbmuser, selectedPerguruanTinggi,
						false, false, null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
			Vbox vbox2 = new Vbox();
			vbox2.setParent(vbox);
			PengumumanAkademisAction.tampilkanPolling(pengumumanAkademis, vbox2);

			tab.setSelected(true);

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Komentar", "/img/live-on.gif");
			toolbarbutton.setParent(vbox);
			toolbarbutton.setOrient("vertical");
			toolbarbutton.setVisible(pengumumanAkademis.getBolehDiberiKomentar());
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					init(new DiskusiPengumumanAkademis(), pengumumanAkademis, detail);
					addWindow.onModal();
				}
			});

		}

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link TampilanPengumumanLibraryAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TampilanPengumumanLibraryAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see TampilanPengumumanLibraryAction
	 */
	class TampilanPengumumanAkademisRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PengumumanAkademis pengumumanAkademis = (PengumumanAkademis) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setOpen(true);
			displayDetailPertemuanFileContent(pengumumanAkademis, detail);

			Vbox vbox = new Vbox();
			vbox.setHeight("100%");
			vbox.setWidth("100%");
			vbox.setParent(arg0);

			Tbmuser tbmuser = Common.getCurrentUser();
			PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

			Grid grid = new Grid();grid.setSclass("dgrid");
			grid.setSclass("fgrid");
			grid.setWidth("100%");
			grid.setParent(vbox);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);
			Sekolah sekolah = SekolahUtil.getSekolah();
			PengumumanAkademisAction.tampilPengumuman(rows, pengumumanAkademis, sekolah, tbmuser, selectedPerguruanTinggi,
					false, false, null);
			Vbox vbox2 = new Vbox();
			vbox2.setParent(vbox);
			PengumumanAkademisAction.tampilkanPolling(pengumumanAkademis, vbox2);

			if (pengumumanAkademis.getBolehDiberiKomentar()) {
				MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Komentar", "/img/live-on.gif");
				toolbarbutton.setParent(vbox);
				toolbarbutton.setOrient("vertical");
				toolbarbutton.setVisible(pengumumanAkademis.getBolehDiberiKomentar());
				toolbarbutton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						init(new DiskusiPengumumanAkademis(), pengumumanAkademis, detail);
						addWindow.onModal();
					}
				});
			}

		}
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		Criterion r = Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_PERPUSTAKAAN);

		Criteria criteria = session.createCriteria(PengumumanAkademis.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))).add(r)
				.add((searchmulai == null || searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.between("tanggal", searchmulai.getValue(), searchsampai.getValue())));
		if (order)
			criteria.addOrder(Order.desc("tanggal")).addOrder(Order.desc("id"));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		loadData(cari.getValue());

		Common.initPaging(initCriteria(false), paging);

		List<PengumumanAkademis> listPengumumanAkademis = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
				PengumumanAkademis.class);

		ListModel strset = new SimpleListModel(listPengumumanAkademis);

		grid.setRowRenderer(new TampilanPengumumanAkademisRenderer());
		grid.setModelCheckMobile(strset);

		grid.setVflex(true);

	}

	public void displayDetailPertemuanFileContent(final PengumumanAkademis pengumumanAkademis,
			final Component component) {
		this.pengumumanAkademis = pengumumanAkademis;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar file dan komentar terkait dengan Pengumuman ini"));

		grids = new MyGrid();
		grids.setMold("paging");
		grids.setPageSize(10);
		grids.setParent(groupbox);

		loadDataAttachment();

		if (pengumumanAkademis.getBolehDiberiKomentar()) {
			gridKomentar = new MyGrid();
			gridKomentar.setMold("paging");
			gridKomentar.setPageSize(10);
			gridKomentar.setParent(groupbox);

			Columns columns = new Columns();

			columns.setParent(gridKomentar);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Komentar");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Tanggal");
			column.setWidth("20%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Oleh");
			column.setWidth("25%");

			loadData(pengumumanAkademis);
		}

	}

	@SuppressWarnings("unchecked")
	public void loadDataAttachment() {
		Session session = HibernateUtil.currentSession();
		List<LampiranPengumumanAkademis> lampiranPengumumanAkademis = session
				.createCriteria(LampiranPengumumanAkademis.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("pengumumanAkademis", pengumumanAkademis)).setMaxResults(5).list();

		ListModel strset = new SimpleListModel(lampiranPengumumanAkademis);

		grids.setRowRenderer(new DetailLampiranPengumumanAkademisRenderer());
		grids.setModelCheckMobile(strset);

		grids.renderAll();
		grids.setOddRowSclass("non-odd");

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link TampilanPengumumanLibraryAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TampilanPengumumanLibraryAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see TampilanPengumumanLibraryAction
	 */
	class DetailLampiranPengumumanAkademisRenderer extends ais.ui.util.MyRowRenderer {

		public DetailLampiranPengumumanAkademisRenderer() {

		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final LampiranPengumumanAkademis lampiranPengumumanAkademis = (LampiranPengumumanAkademis) arg1;
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			vbox.setWidth("100%");
			CommonMedia.preview(lampiranPengumumanAkademis, vbox);
			new Label(Common.dateFormat.get().format(lampiranPengumumanAkademis.getUploadDate())).setParent(vbox);

			Hbox hbox = new Hbox();
			hbox.setParent(vbox);
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(
					"Download " + lampiranPengumumanAkademis.getNama(), lampiranPengumumanAkademis.iconDonwload());
			toolbarbutton.setParent(hbox);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					LampiranPengumumanAkademis content = (LampiranPengumumanAkademis) HibernateUtil.currentSession()
							.createCriteria(LampiranPengumumanAkademis.class)
							.add(Restrictions.idEq(lampiranPengumumanAkademis.getId())).setMaxResults(1).uniqueResult();

					Filedownload.save(CommonMedia.getFileFotoLangsungOld(content, false),
							lampiranPengumumanAkademis.getMimeType());
				}

			});

		}

	}

	private void init(DiskusiPengumumanAkademis diskusiPengumumanAkademis, final PengumumanAkademis pengumumanAkademis,
			final MyDetail detail) {
		this.diskusiPengumumanAkademis = diskusiPengumumanAkademis;
		this.pengumumanAkademis = pengumumanAkademis;
		addWindow = new MyWindow();
		addWindow.setParent(page.getFirstRoot());
		addWindow.setTitle("Komentar Pengumuman Akademis");
		addWindow.setHeight("300px");
		addWindow.setWidth(Common.isMobile() ? "100%" : "300px");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul"));
		row.appendChild(judul = new Textbox(
				diskusiPengumumanAkademis.getJudul() == null ? "" : diskusiPengumumanAkademis.getJudul()));
		judul.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
		catatan = new Textbox();
		catatan.setValue(diskusiPengumumanAkademis.getCatatan() == null ? "" : diskusiPengumumanAkademis.getCatatan());
		catatan.setRows(4);
		catatan.setWidth("90%");
		row.appendChild(catatan);

		String myoleh = "";

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Oleh"));
		row.appendChild(oleh = new Textbox(
				diskusiPengumumanAkademis.getOleh() == null ? myoleh : diskusiPengumumanAkademis.getOleh()));
		oleh.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				displayDetailPertemuanFileContent(pengumumanAkademis, detail);
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					loadData(TampilanPengumumanLibraryAction.this.diskusiPengumumanAkademis.getPengumumanAkademis());
					displayDetailPertemuanFileContent(pengumumanAkademis, detail);
					addWindow.detach();
				}
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (judul.getValue().trim().equals("")) {
			MyMessageboxConfig.show("judul harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (oleh.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		DiskusiPengumumanAkademisDao diskusiPengumumanAkademisDao = DaoFactory.getInstance()
				.getDiskusiPengumumanAkademisDao();
		if (diskusiPengumumanAkademis.getId() != null) {
			diskusiPengumumanAkademis = diskusiPengumumanAkademisDao.load(diskusiPengumumanAkademis.getId());
		}
		diskusiPengumumanAkademis.setTanggal(ais.ui.util.WaktuUtil.getDate());
		diskusiPengumumanAkademis.setJudul(judul.getValue());
		diskusiPengumumanAkademis.setOleh(oleh.getValue());
		diskusiPengumumanAkademis.setPengguna(oleh.getValue());
		diskusiPengumumanAkademis.setCatatan(catatan.getValue());
		diskusiPengumumanAkademis.setPengumumanAkademis(pengumumanAkademis);

		Common.refreshSaveOrUpdate(diskusiPengumumanAkademis);

		ais.action.master.helper.BroadcastHelper.kirimEmail(diskusiPengumumanAkademis);

		return true;
	}

	@SuppressWarnings("unchecked")
	public void loadData(PengumumanAkademis pengumumanAkademis) {
		Session session = HibernateUtil.currentSession();
		List<DiskusiPengumumanAkademis> diskusiPengumumanAkademis = session
				.createCriteria(DiskusiPengumumanAkademis.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("pengumumanAkademis", pengumumanAkademis)).list();

		ListModel strset = new SimpleListModel(diskusiPengumumanAkademis);
		gridKomentar.setRowRenderer(new DetailPengumumanRenderer());
		gridKomentar.setModelCheckMobile(strset);
		gridKomentar.renderAll();

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link TampilanPengumumanLibraryAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TampilanPengumumanLibraryAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see TampilanPengumumanLibraryAction
	 */
	class DetailPengumumanRenderer extends ais.ui.util.MyRowRenderer {

		public DetailPengumumanRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final DiskusiPengumumanAkademis diskusiPengumumanAkademis = (DiskusiPengumumanAkademis) data;

			new Label(diskusiPengumumanAkademis.getJudul() + " - " + diskusiPengumumanAkademis.getCatatan())
					.setParent(row);
			new Label(diskusiPengumumanAkademis.getTanggal() == null ? ""
					: Common.dateFormat3.get().format(diskusiPengumumanAkademis.getTanggal())).setParent(row);
			new Label(diskusiPengumumanAkademis.getPengguna()).setParent(row);

		}

	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public Boolean getReadonly() {
		return readonly;
	}

	public West getMenu() {
		return menu;
	}

	public void setMenu(West menu) {
		this.menu = menu;
	}

}
