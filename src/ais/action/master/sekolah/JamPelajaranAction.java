package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

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
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.sekolah.JamPelajaran;
import ais.database.model.sekolah.JenisJadwalPelajaran;
import ais.database.model.sekolah.KelompokJamPelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk jam pelajaran. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchyayasan}, {@code Combobox
 * searchsekolah}, {@code Textbox nama}, {@code Combobox sekolah}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onMasa()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
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
public class JamPelajaranAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
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

	private JamPelajaran jamPelajaran;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Timebox mulai;
	private Timebox selesai;
	private Combobox jenisJadwalPelajaran;
	private Combobox kelompokJamPelajaran;

	private Tabpanel masa;
	private MyDoublebox jp;

	public void onMasa(Event event) {
		if (masa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(masa);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/kelompok_jam_pelajaran.zul");
			iframe.setParent(window);
		}
	}

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

		String[] contents = new String[] { "id", "nama", "sekolah", "mulaiS", "sampaiS", "jp", "jenisJadwalPelajaran",
				"kelompokJamPelajaran", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JamPelajaran.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link JamPelajaranAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link JamPelajaranAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see JamPelajaranAction
	 */
	class JamPelajaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JamPelajaran jamPelajaran = (JamPelajaran) arg1;

			RevisiHelper.createNewRevisi(JamPelajaran.class, jamPelajaran, jamPelajaran.getNama()).setParent(arg0);
			new Label(jamPelajaran.getSekolah() == null ? "" : jamPelajaran.getSekolah().getNama()).setParent(arg0);
			new Label(jamPelajaran.getMulai() == null ? "" : Common.timeFormat.get().format(jamPelajaran.getMulai()))
					.setParent(arg0);
			new Label(jamPelajaran.getSelesai() == null ? "" : Common.timeFormat.get().format(jamPelajaran.getSelesai()))
					.setParent(arg0);
			new Label(jamPelajaran.getJenisJadwalPelajaran() == null ? ""
					: jamPelajaran.getJenisJadwalPelajaran().getNama()).setParent(arg0);

			new Label(jamPelajaran.getKelompokJamPelajaran() == null ? ""
					: jamPelajaran.getKelompokJamPelajaran().getNama()).setParent(arg0);

			new Label(Common.numberFormat.get().format(jamPelajaran.getJp())).setParent(arg0);

			new Label(jamPelajaran.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jamPelajaran.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jamPelajaran.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jamPelajaran);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jamPelajaran, JamPelajaranAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JamPelajaran());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jamPelajaran = (JamPelajaran) obj;
		init(jamPelajaran);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final JamPelajaran jamPelajaran) throws Exception {
		this.jamPelajaran = jamPelajaran;
		addWindow.setTitle(jamPelajaran.getId() == null ? "Tambah Jam Pelajaran" : "Ubah Jam Pelajaran");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jam Pelajaran *"));
		row.appendChild(nama = new Textbox(jamPelajaran.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah JP *"));
		row.appendChild(jp = new MyDoublebox(jamPelajaran.getJp()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu *"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(mulai = new ais.ui.util.MyTimebox(jamPelajaran.getMulai()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		hbox.appendChild(selesai = new ais.ui.util.MyTimebox(jamPelajaran.getSelesai()));

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, jamPelajaran.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, jamPelajaran.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Jam Pelajaran *"));
		row.appendChild(jenisJadwalPelajaran = new Combobox());
		jenisJadwalPelajaran.setWidth("90%");
		jenisJadwalPelajaran.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok Jam Pelajaran"));
		row.appendChild(kelompokJamPelajaran = new Combobox());
		kelompokJamPelajaran.setWidth("90%");
		kelompokJamPelajaran.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);
				Common.insertCombo(jenisJadwalPelajaran, new String[] { "nama", "sekolah" }, JenisJadwalPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(jenisJadwalPelajaran, jamPelajaran.getJenisJadwalPelajaran());

				Common.insertCombo(kelompokJamPelajaran, new String[] { "nama", "sekolah" }, KelompokJamPelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(kelompokJamPelajaran, jamPelajaran.getKelompokJamPelajaran());
			}
		};

		sekolah.addEventListener("onChange", eventListener);
		Common.createDefaultTimer(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jamPelajaran.getKeterangan()));
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
			MyMessageboxConfig.show("Nama Jam Pelajaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (mulai.getValue() == null) {
			MyMessageboxConfig.show("Jam Mulai harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (selesai.getValue() == null) {
			MyMessageboxConfig.show("Jam Selesai harus diisi", "Peringatan", MyMessageboxConfig.OK,
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
		if (jenisJadwalPelajaran.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Jadwal harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jamPelajaran.getId() != null) {
			jamPelajaran = (JamPelajaran) session.load(JamPelajaran.class, jamPelajaran.getId());

		}

		jamPelajaran.setNama(nama.getValue());
		jamPelajaran.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		jamPelajaran.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		jamPelajaran.setKeterangan(keterangan.getValue());
		jamPelajaran.setMulai(mulai.getValue());
		jamPelajaran.setSelesai(selesai.getValue());
		jamPelajaran.setJenisJadwalPelajaran((JenisJadwalPelajaran) jenisJadwalPelajaran.getSelectedItem().getValue());
		jamPelajaran.setJp(jp.getValue());
		jamPelajaran
				.setKelompokJamPelajaran((KelompokJamPelajaran) (kelompokJamPelajaran.getSelectedItem() == null ? null
						: kelompokJamPelajaran.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, jamPelajaran);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JamPelajaran.class);

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

		List<JamPelajaran> jamPelajaran = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jamPelajaran);
		grid.setRowRenderer(new JamPelajaranRenderer());
		grid.setModelCheckMobile(strset);

	}

}
