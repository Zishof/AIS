package ais.action.master.employ;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import ais.ui.util.MyCkEditor;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
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
import ais.database.dao.DaoFactory;
import ais.database.dao.employ.KonfigurasiSKDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.employ.JenisKegiatanEmploy;
import ais.database.model.employ.KonfigurasiSK;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk konfigurasi sk. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox nama}, {@code Combobox jenisField},
 * {@code Combobox jenisKegiatanEmploy}, {@code MyCkEditor isi}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class KonfigurasiSKAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private Textbox nama;
	private Combobox jenisField;
	private Combobox jenisKegiatanEmploy;
	private MyCkEditor isi;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KonfigurasiSK konfigurasiSK;
	private MyToolbarbuttonConfig add;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.insertCombo(jenisKegiatanEmploy, "nama", JenisKegiatanEmploy.class);

		jenisField = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(KonfigurasiSK.MENGINGAT);
		if (comboitem != null) { comboitem.setValue(KonfigurasiSK.MENGINGAT); }
		jenisField.appendChild(comboitem);
		comboitem = new MyComboitemConfig(KonfigurasiSK.MENIMBANG);
		if (comboitem != null) { comboitem.setValue(KonfigurasiSK.MENIMBANG); }
		jenisField.appendChild(comboitem);
		comboitem = new MyComboitemConfig(KonfigurasiSK.MEMPERHATIKAN);
		if (comboitem != null) { comboitem.setValue(KonfigurasiSK.MEMPERHATIKAN); }
		jenisField.appendChild(comboitem);

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
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link KonfigurasiSKAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KonfigurasiSKAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KonfigurasiSKAction
	 */
	class KonfigurasiSKRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KonfigurasiSK konfigurasiSK = (KonfigurasiSK) arg1;

			RevisiHelper.createNewRevisi(KonfigurasiSK.class, konfigurasiSK, konfigurasiSK.getIsi()).setParent(arg0);
			new Label(konfigurasiSK.getJenisField()).setParent(arg0);
			new Label(konfigurasiSK.getJenisKegiatanEmploy() == null ? ""
					: konfigurasiSK.getJenisKegiatanEmploy().getNama()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(konfigurasiSK);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											KonfigurasiSKDao konfigurasiSKDao = DaoFactory.getInstance()
													.getKonfigurasiSKDao();
											konfigurasiSKDao.delete((konfigurasiSK));
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);

			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new KonfigurasiSK());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KonfigurasiSK konfigurasiSK) {
		this.konfigurasiSK = konfigurasiSK;
		addWindow.setTitle(konfigurasiSK.getId() == null ? "Tambah Konfigurasi SK" : "Ubah Konfigurasi SK");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(konfigurasiSK.getNama() == null ? "" : konfigurasiSK.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Field"));
		row.appendChild(jenisField);
		jenisField.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kegiata"));
		row.appendChild(jenisKegiatanEmploy = new Combobox());
		Common.insertCombo(jenisKegiatanEmploy, "nama", JenisKegiatanEmploy.class);
		Common.selectComboItem(jenisKegiatanEmploy,
				konfigurasiSK.getJenisKegiatanEmploy() == null ? "" : konfigurasiSK.getJenisKegiatanEmploy());
		jenisKegiatanEmploy.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Isi"));
		row.appendChild(isi = new MyCkEditor());
		isi.setValue(konfigurasiSK.getIsi());
		isi.setWidth("90%");
		isi.setHeight("250px");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(konfigurasiSK.getKeterangan() == null ? "" : konfigurasiSK.getKeterangan()));
		keterangan.setWidth("90%");

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

		if (nama.getValue().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama pada form konfigurasi SK; (2) pastikan nama tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (jenisField.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Field belum dipilih. Langkah yang dapat dilakukan: (1) pilih Jenis Field dari dropdown pada form; (2) pastikan pilihan tersedia dan sesuai; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (jenisKegiatanEmploy.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Kegiatan belum dipilih. Langkah yang dapat dilakukan: (1) pilih Jenis Kegiatan dari dropdown pada form; (2) pastikan data jenis kegiatan sudah tersedia di master; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		KonfigurasiSKDao konfigurasiSKDao = DaoFactory.getInstance().getKonfigurasiSKDao();
		if (konfigurasiSK.getId() != null) {
			konfigurasiSK = konfigurasiSKDao.load(konfigurasiSK.getId());

		}
		konfigurasiSK.setNama(nama.getValue());
		konfigurasiSK.setJenisField(jenisField.getSelectedItem().getValue().toString());
		konfigurasiSK.setJenisKegiatanEmploy((JenisKegiatanEmploy) (jenisKegiatanEmploy.getSelectedItem() == null ? null
				: jenisKegiatanEmploy.getSelectedItem().getValue()));
		konfigurasiSK.setIsi(isi.getValue());
		konfigurasiSK.setKeterangan(keterangan.getValue());

		if (konfigurasiSK.getId() != null) {
			konfigurasiSKDao.update(konfigurasiSK);
		} else {
			konfigurasiSKDao.save(konfigurasiSK);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KonfigurasiSK.class);
		if (order)
			criteria.addOrder(Order.asc("id"));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging(initCriteria(false), paging);

		List<KonfigurasiSK> konfigurasiSK = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(konfigurasiSK);
		grid.setRowRenderer(new KonfigurasiSKRenderer());
		grid.setModelCheckMobile(strset);

	}
}