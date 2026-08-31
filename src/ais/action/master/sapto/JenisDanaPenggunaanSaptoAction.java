package ais.action.master.sapto;

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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
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
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.sapto.JenisDanaPenggunaanSapto;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk jenis dana penggunaan sapto. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox nama}, {@code Textbox keterangan},
 * {@code boolean edit}, {@code boolean delete}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); validasi/perhitungan ({@code checkNamaJenisDanaPenggunaanSapto()}); mutasi data ({@code
 * onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
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
public class JenisDanaPenggunaanSaptoAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisDanaPenggunaanSapto jenisDanaPenggunaanSapto;
	private MyToolbarbuttonConfig add;
	private Combobox jenisPenggunaan;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
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

		String[] contents = new String[] { "id", "nama", "jenisPenggunaan", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisDanaPenggunaanSapto.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link JenisDanaPenggunaanSaptoAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link JenisDanaPenggunaanSaptoAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see JenisDanaPenggunaanSaptoAction
	 */
	class JenisDanaPenggunaanSaptoRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisDanaPenggunaanSapto jenisDanaPenggunaanSapto = (JenisDanaPenggunaanSapto) arg1;

			RevisiHelper.createNewRevisi(JenisDanaPenggunaanSapto.class, jenisDanaPenggunaanSapto,
					jenisDanaPenggunaanSapto.getNama()).setParent(arg0);
			new Label(jenisDanaPenggunaanSapto.getJenisPenggunaan()).setParent(arg0);
			new Label(jenisDanaPenggunaanSapto.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, jenisDanaPenggunaanSapto, JenisDanaPenggunaanSaptoAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisDanaPenggunaanSapto());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisDanaPenggunaanSapto = (JenisDanaPenggunaanSapto) obj;
		init(jenisDanaPenggunaanSapto);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisDanaPenggunaanSapto jenisDanaPenggunaanSapto) {
		this.jenisDanaPenggunaanSapto = jenisDanaPenggunaanSapto;
		addWindow.setTitle(jenisDanaPenggunaanSapto.getId() == null ? "Tambah Jenis Dana Penggunaan" : "Ubah Jenis Dana Penggunaan");
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
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Penggunaan Dana *"));
		row.appendChild(nama = new Textbox(jenisDanaPenggunaanSapto.getNama()));
		nama.setWidth("90%");
		
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Penggunaan *"));
		row.appendChild(jenisPenggunaan = new Combobox()); 
		MyComboitemConfig comboitemConfig = new MyComboitemConfig(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENDIDIKAN);
		comboitemConfig.setValue(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENDIDIKAN); 
		jenisPenggunaan.appendChild(comboitemConfig);
		
		comboitemConfig = new MyComboitemConfig(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENELITIAN);
		comboitemConfig.setValue(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENELITIAN); 
		jenisPenggunaan.appendChild(comboitemConfig);
		
		comboitemConfig = new MyComboitemConfig(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENGABDIAN);
		comboitemConfig.setValue(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_PENGABDIAN); 
		jenisPenggunaan.appendChild(comboitemConfig);
		
		comboitemConfig = new MyComboitemConfig(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_INVESTASI_PRASARANA);
		comboitemConfig.setValue(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_INVESTASI_PRASARANA); 
		jenisPenggunaan.appendChild(comboitemConfig);
		
		comboitemConfig = new MyComboitemConfig(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_INVESTASI_SARANA);
		comboitemConfig.setValue(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_INVESTASI_SARANA); 
		jenisPenggunaan.appendChild(comboitemConfig);
		
		comboitemConfig = new MyComboitemConfig(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_INVESTASI_SDM);
		comboitemConfig.setValue(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_INVESTASI_SDM); 
		jenisPenggunaan.appendChild(comboitemConfig);
		
		comboitemConfig = new MyComboitemConfig(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_LAIN_LAIN);
		comboitemConfig.setValue(JenisDanaPenggunaanSapto.JENIS_PENGGUNAAN_LAIN_LAIN); 
		jenisPenggunaan.appendChild(comboitemConfig);
		
		jenisPenggunaan.setWidth("90%");
		jenisPenggunaan.setReadonly(true);
		Common.selectComboItem(jenisPenggunaan, jenisDanaPenggunaanSapto.getJenisPenggunaan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisDanaPenggunaanSapto.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Jenis Penggunaan Dana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisPenggunaan.getSelectedItem()==null) {
			MyMessageboxConfig.show("Jenis Penggunaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		

		boolean i = checkNamaJenisDanaPenggunaanSapto();
		if (i) {
			MyMessageboxConfig.show("Nama Jenis Penggunaan Dana sudah ada di database", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisDanaPenggunaanSapto.getId() != null) {
			jenisDanaPenggunaanSapto = (JenisDanaPenggunaanSapto) session.load(JenisDanaPenggunaanSapto.class,
					jenisDanaPenggunaanSapto.getId());

		}

		jenisDanaPenggunaanSapto.setNama(nama.getValue());
		jenisDanaPenggunaanSapto.setJenisPenggunaan((String) jenisPenggunaan.getSelectedItem().getValue()); 
		jenisDanaPenggunaanSapto.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisDanaPenggunaanSapto);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisDanaPenggunaanSapto.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisDanaPenggunaanSapto> jenisDanaPenggunaanSapto = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisDanaPenggunaanSapto);
		grid.setRowRenderer(new JenisDanaPenggunaanSaptoRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaJenisDanaPenggunaanSapto() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisDanaPenggunaanSapto.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.jenisDanaPenggunaanSapto.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenisDanaPenggunaanSapto.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
