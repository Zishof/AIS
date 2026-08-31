package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Longbox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Menu;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk menu. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchmenu}, {@code Checkbox
 * searchaktif}, {@code boolean edit}, {@code boolean delete}; inisialisasi/lifecycle ({@code doBeforeCompose()},
 * {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); validasi/perhitungan ({@code checkId()}); mutasi data ({@code onSave()}); operasi domain
 * lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class MenuAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchmenu;
	private Checkbox searchaktif;

	private boolean edit = false;
	private boolean delete = false;

	private Menu menu;
	private MyToolbarbuttonConfig add;
	private Longbox id;
	private Longbox root;
	private Longbox child;
	private Textbox url;
	private Textbox label;
	private Textbox bigIcon;
	private MyCheckboxConfig aktif;
	private MyCheckboxConfig tampilDiPt;
	private MyCheckboxConfig tampilDiSekolah;
	private MyCheckboxConfig bukaHalamanBaru;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		if (!Common.getApakahAdmin()) {
			Common.goLogoff();
			return;
		}

		Common.initLaguage();

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "root", "child", "label", "url", "bigIcon", "nomorUrut", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Menu.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Menu.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link MenuAction}. Kelas ini menerjemahkan satu item data menjadi baris
	 * atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link MenuAction} dan dapat mengakses state kelas
	 * induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see MenuAction
	 */
	class MenuRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Menu menu = (Menu) arg1;
			new Label(menu.getId() == null ? "" : menu.getId().toString()).setParent(arg0);
			new Label(menu.getRoot() == null ? "" : menu.getRoot().toString()).setParent(arg0);
			new Label(menu.getChild() == null ? "" : menu.getChild().toString()).setParent(arg0);
			RevisiHelper.createNewRevisi(Menu.class, menu, menu.getLabel()).setParent(arg0);
			new Label(menu.getUrl()).setParent(arg0);
			new Label(menu.getBigIcon()).setParent(arg0);

			final Intbox nomorUrut = new Intbox(menu.getNomorUrut());
			nomorUrut.setParent(arg0);
			nomorUrut.setWidth("90%");
			nomorUrut.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					menu.setNomorUrut(nomorUrut.getValue());
					Common.refreshUpdate(menu);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(menu.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					menu.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(menu);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, menu, MenuAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Menu());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		menu = (Menu) obj;
		init(menu);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Menu menu) {
		this.menu = menu;
		addWindow.setTitle(menu.getId() == null ? "Tambah Menu" : "Ubah Menu");
		Common.clear(addWindow);
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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("ID Menu *"));
		row.appendChild(id = new Longbox(menu.getId()));
		id.setWidth("90%");
		id.setDisabled(menu.getId() != null);

		Common.initKeterangan(rows, "ID menu tidak boleh sama");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Root Menu *"));
		row.appendChild(root = new Longbox(menu.getRoot()));
		root.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Child Menu *"));
		row.appendChild(child = new Longbox(menu.getChild()));
		child.setWidth("90%");

		Common.initKeterangan(rows, "Child menu tidak boleh sama");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Label Menu *"));
		row.appendChild(label = new Textbox(menu.getLabel()));
		label.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("URL"));
		row.appendChild(url = new Textbox(menu.getUrl()));
		url.setWidth("90%");
		url.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Icon"));
		row.appendChild(bigIcon = new Textbox(menu.getBigIcon()));
		bigIcon.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(aktif = new MyCheckboxConfig());
		aktif.setChecked(menu.getAktif() == null || menu.getAktif());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampil Di Perguruan Tinggi"));
		row.appendChild(tampilDiPt = new MyCheckboxConfig());
		tampilDiPt.setChecked(menu.getTampilDiPt());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampil Di Sekolah"));
		row.appendChild(tampilDiSekolah = new MyCheckboxConfig());
		tampilDiSekolah.setChecked(menu.getTampilDiSekolah());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Buka Halaman Baru"));
		row.appendChild(bukaHalamanBaru = new MyCheckboxConfig());
		bukaHalamanBaru.setChecked(menu.getBukaHalamanBaru());

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
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {

		if (id.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data ID Menu",
					"Kolom ID Menu belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu ID Menu.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (menu.getId() == null && checkId()) {
			MyMessageboxConfig.show("ID Menu sudah ada", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (root.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Root Menu",
					"Kolom Root Menu belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Root Menu.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (child.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Child Menu",
					"Kolom Child Menu belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Child Menu.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (label.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Label Menu",
					"Kolom Label Menu belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Label Menu.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (menu.getId() == null && checkId()) {
			MyMessageboxConfig.show("ID Menu sudah ada", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (menu.getId() != null) {
			menu = (Menu) session.createCriteria(Menu.class).add(Restrictions.idEq(menu.getId())).uniqueResult();
		}

		boolean ubah = true;
		if (menu == null) {
			menu = new Menu();
			ubah = false;
		}

		menu.setId(id.getValue());
		menu.setLabel(label.getValue());
		menu.setRoot(root.getValue());
		menu.setChild(child.getValue());
		menu.setUrl(url.getValue());
		menu.setBigIcon(bigIcon.getValue());
		menu.setAktif(aktif.isChecked());
		menu.setTampilDiPt(tampilDiPt.isChecked());
		menu.setTampilDiSekolah(tampilDiSekolah.isChecked());
		menu.setBukaHalamanBaru(bukaHalamanBaru.isChecked());

		if (ubah) {
			Common.refreshSaveOrUpdate(session, menu);
		} else {
			session.save(menu);
			session.flush();
		}
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Menu.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("root")).addOrder(Order.asc("child"))
					.addOrder(Order.asc("label"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("label", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchmenu.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("url", searchmenu.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Menu> menu = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(menu);
		grid.setRowRenderer(new MenuRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkId() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Menu.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("id", id.getValue())).uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}
}
