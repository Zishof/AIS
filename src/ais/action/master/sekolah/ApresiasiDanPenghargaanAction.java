package ais.action.master.sekolah;


import ais.action.master.apresiasi.DasbordApresiasi;
import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.sekolah.Penghargaan;
import ais.database.model.sekolah.Apresiasi;
import ais.database.model.sekolah.ApresiasiDanPenghargaan;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk apresiasi dan penghargaan. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Tabpanel
 * tabDasbor}, {@code Paging paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox
 * searchyayasan}, {@code Combobox searchsekolah}, {@code Textbox nama}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onDasbor()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
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
public class ApresiasiDanPenghargaanAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Tabpanel tabDasbor;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Textbox nama;
	private Combobox sekolah;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private ApresiasiDanPenghargaan apresiasiDanPenghargaan;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Set<Apresiasi> selectedApresiasi;
	private Set<Penghargaan> selectedPenghargaan;

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

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

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

		String[] contents = new String[] { "id", "nama", "sekolah", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ApresiasiDanPenghargaan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
		onDasbor(null);
	}

	public void onDasbor(org.zkoss.zk.ui.event.Event event) {
		if (tabDasbor.getChildren().size() == 0) {
			DasbordApresiasi dasbord = new DasbordApresiasi(DasbordApresiasi.Lingkup.SEMUA);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Apresiasi & Penghargaan",
				"Ringkasan apresiasi dan penghargaan yang diberikan kepada seluruh sivitas.");
		}
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link ApresiasiDanPenghargaanAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link ApresiasiDanPenghargaanAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see ApresiasiDanPenghargaanAction
	 */
	class ApresiasiDanPenghargaanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ApresiasiDanPenghargaan apresiasiDanPenghargaan = (ApresiasiDanPenghargaan) arg1;

			RevisiHelper.createNewRevisi(ApresiasiDanPenghargaan.class, apresiasiDanPenghargaan,
					apresiasiDanPenghargaan.getNama()).setParent(arg0);
			new Label(
					apresiasiDanPenghargaan.getSekolah() == null ? "" : apresiasiDanPenghargaan.getSekolah().getNama())
					.setParent(arg0);
			new Label(apresiasiDanPenghargaan.getKeterangan()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			int i = 1;
			for (Apresiasi apresiasi : new TreeSet<Apresiasi>(apresiasiDanPenghargaan.getApresiasis())) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + apresiasi.getNama()));
				i++;
			}

			vbox = new Vbox();
			vbox.setParent(arg0);
			i = 1;
			for (Penghargaan penghargaan : new TreeSet<Penghargaan>(apresiasiDanPenghargaan.getPenghargaans())) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + penghargaan.getNama()));
				i++;
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(apresiasiDanPenghargaan.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					apresiasiDanPenghargaan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(apresiasiDanPenghargaan);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, apresiasiDanPenghargaan, ApresiasiDanPenghargaanAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new ApresiasiDanPenghargaan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		apresiasiDanPenghargaan = (ApresiasiDanPenghargaan) obj;
		init(apresiasiDanPenghargaan);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void init(ApresiasiDanPenghargaan apresiasiDanPenghargaan) {
		this.apresiasiDanPenghargaan = apresiasiDanPenghargaan;
		addWindow.setTitle(apresiasiDanPenghargaan.getId() == null ? "Tambah Jenis Apresiasi Dan Penghargaan" : "Ubah Jenis Apresiasi Dan Penghargaan");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Apresiasi Dan Penghargaan *"));
		row.appendChild(nama = new Textbox(apresiasiDanPenghargaan.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, apresiasiDanPenghargaan.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, apresiasiDanPenghargaan.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(apresiasiDanPenghargaan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("Apresiasi"));

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		List<Apresiasi> apresiasis = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(Apresiasi.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				Apresiasi.class);

		if (apresiasiDanPenghargaan.getId() != null) {
			HibernateUtil.currentSession().refresh(this.apresiasiDanPenghargaan);
		}
		selectedApresiasi = this.apresiasiDanPenghargaan.getApresiasis();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final Apresiasi apresiasi : apresiasis) {
			final Checkbox checkbox = new Checkbox(apresiasi.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(selectedApresiasi.contains(apresiasi));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedApresiasi.add(apresiasi);
					} else {
						selectedApresiasi.remove(apresiasi);
					}
				}
			});
		}

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		subGrid = new MyGrid();
		row.appendChild(subGrid);

		subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("Penghargaan"));

		subRows = new Rows();
		subRows.setParent(subGrid);

		subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		List<Penghargaan> penghargaans = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(Penghargaan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				Penghargaan.class);

		if (apresiasiDanPenghargaan.getId() != null) {
			HibernateUtil.currentSession().refresh(this.apresiasiDanPenghargaan);
		}
		selectedPenghargaan = this.apresiasiDanPenghargaan.getPenghargaans();

		vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final Penghargaan penghargaan : penghargaans) {
			final Checkbox checkbox = new Checkbox(penghargaan.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(selectedPenghargaan.contains(penghargaan));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedPenghargaan.add(penghargaan);
					} else {
						selectedPenghargaan.remove(penghargaan);
					}
				}
			});
		}

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
			MyMessageboxConfig.show("Jenis Apresiasi Dan Penghargaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Yayasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (apresiasiDanPenghargaan.getId() != null) {
			apresiasiDanPenghargaan = (ApresiasiDanPenghargaan) session.load(ApresiasiDanPenghargaan.class,
					apresiasiDanPenghargaan.getId());

		}

		apresiasiDanPenghargaan.setNama(nama.getValue());
		apresiasiDanPenghargaan.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		apresiasiDanPenghargaan.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		apresiasiDanPenghargaan.setKeterangan(keterangan.getValue());
		apresiasiDanPenghargaan.setApresiasis(selectedApresiasi);
		apresiasiDanPenghargaan.setPenghargaans(selectedPenghargaan);

		Common.refreshSaveOrUpdate(session, apresiasiDanPenghargaan);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ApresiasiDanPenghargaan.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ApresiasiDanPenghargaan> apresiasiDanPenghargaan = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(apresiasiDanPenghargaan);
		grid.setRowRenderer(new ApresiasiDanPenghargaanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
