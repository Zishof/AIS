package ais.action.master.sop;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyColorbox;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
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

import ais.action.master.GrupChecklistPenilaianUmumAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.sop.AktorSop;
import ais.database.model.sop.JenisSop;
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
 * Controller/action ZK untuk jenis sop. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Checkbox searchaktif}, {@code Textbox nama},
 * {@code Textbox keterangan}, {@code boolean edit}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari
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
public class JenisSopAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 *
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisSop jenisSop;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private MyColorbox warna;
	private Combobox diperuntukkan;
	private Textbox jenisPengguna;
	private Textbox usernamePengguna;
	private MyColorbox warnatext;
	private Combobox aktorSop;
	private Row a1;
	private Row a2;

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

		String[] contents = new String[] { "id", "kode", "nama", "warna", "diperuntukkan", "jenisPengguna",
				"usernamePengguna", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisSop.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisSop.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link JenisSopAction}. Kelas ini menerjemahkan satu item data menjadi
	 * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link JenisSopAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see JenisSopAction
	 */
	class JenisSopRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisSop jenisSop = (JenisSop) arg1;
			new Label(jenisSop.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(JenisSop.class, jenisSop, jenisSop.getNama()).setParent(arg0);

			Label l;
			(l = new Label(jenisSop.getWarna() + ";" + jenisSop.getWarnatext())).setParent(arg0);
			l.setStyle("background-color:" + jenisSop.getWarna() + ";color:" + jenisSop.getWarnatext() + ";");
			new Label(jenisSop.getDiperuntukkan()).setParent(arg0);
			new Label(jenisSop.getJenisPengguna().isEmpty() ? "Tidak ditentukan" : jenisSop.getJenisPengguna())
					.setParent(arg0);
			new Label(jenisSop.getUsernamePengguna().isEmpty() ? "Tidak ditentukan" : jenisSop.getUsernamePengguna())
					.setParent(arg0);
			new Label(jenisSop.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisSop.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisSop.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisSop);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisSop, JenisSopAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisSop());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisSop = (JenisSop) obj;
		init(jenisSop);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisSop jenisSop) throws Exception {
		this.jenisSop = jenisSop;
		addWindow.setTitle(jenisSop.getId() == null ? "Tambah Jenis SOP" : "Ubah Jenis SOP");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Jenis SOP *"));
		row.appendChild(kode = new Textbox(jenisSop.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis SOP *"));
		row.appendChild(nama = new Textbox(jenisSop.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan diperuntukkan oleh *"));
		row.appendChild(diperuntukkan = new Combobox());
		GrupChecklistPenilaianUmumAction.diperuntukkan(diperuntukkan);
		diperuntukkan.setWidth("90%");
		diperuntukkan.setReadonly(true);

		Common.selectComboItem(diperuntukkan, jenisSop.getDiperuntukkan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan oleh aktor"));
		row.appendChild(aktorSop = new Combobox());
		aktorSop.setWidth("90%");
		Common.insertComboDanSemua(aktorSop, new String[] { "kode", "nama" }, "keterangan", AktorSop.class,
				"=Aktor Kustom=", Restrictions.eq("aktif", true));
		Common.selectComboItem(true, aktorSop, jenisSop.getAktorSop());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Boleh diajukan oleh jenis pengguna"));
		row.appendChild(jenisPengguna = new Textbox(jenisSop.getJenisPengguna()));
		jenisPengguna.setWidth("90%");
		jenisPengguna.setRows(2);

		a1 = Common.initKeterangan(rows,
				"Jika lebih dari satu, pisahkan dengan tanda koma (,). Kosongkan apabila boleh diajukan oleh semua jenis pengguna");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Boleh diajukan oleh username pengguna"));
		row.appendChild(usernamePengguna = new Textbox(jenisSop.getUsernamePengguna()));
		usernamePengguna.setWidth("90%");
		usernamePengguna.setRows(2);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Username Pengguna",
				"/img/user_male_add.png");

		final MyFormRow rowAmbilPengguna = new MyFormRow();
		rowAmbilPengguna.setParent(rows);
		rowAmbilPengguna.appendChild(new ais.ui.util.MyLabelConfig(""));
		rowAmbilPengguna.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Tbmuser tbmuser : tbmusers) {
								usernamePengguna.setValue(usernamePengguna.getValue()
										+ (usernamePengguna.getValue().isEmpty() ? tbmuser.getUserId()
												: "," + tbmuser.getUserId()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		a2 = Common.initKeterangan(rows,
				"Jika lebih dari satu, pisahkan dengan tanda koma (,). Kosongkan apabila boleh diajukan oleh semua username pengguna");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AktorSop s = (AktorSop) (aktorSop.getSelectedItem() == null ? null
						: aktorSop.getSelectedItem().getValue());
				jenisPengguna.getParent().setVisible(s == null);
				usernamePengguna.getParent().setVisible(s == null);
				a1.setVisible(s == null);
				a2.setVisible(s == null);
				rowAmbilPengguna.setVisible(s == null);
			}

		};

		eventListener.onEvent(null);
		aktorSop.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Warna Dasar"));
		row.appendChild(warna = new MyColorbox());
		warna.setColor(jenisSop.getWarna());
		warna.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Warna Teks"));
		row.appendChild(warnatext = new MyColorbox());
		warnatext.setColor(jenisSop.getWarnatext());
		warnatext.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisSop.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Jenis SOP belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Kode Jenis SOP; (2) isikan kode yang unik dan sesuai ketentuan; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Jenis SOP belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Jenis SOP; (2) isikan nama jenis SOP yang sesuai; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (diperuntukkan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, kolom Diperuntukkan belum dipilih. Langkah yang dapat dilakukan: (1) klik pilihan Diperuntukkan; (2) pilih peruntukan yang sesuai dari daftar yang tersedia; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisSop.getId() != null) {
			jenisSop = (JenisSop) session.load(JenisSop.class, jenisSop.getId());

		}

		jenisSop.setKode(kode.getValue());
		jenisSop.setNama(nama.getValue());
		jenisSop.setKeterangan(keterangan.getValue());
		jenisSop.setWarna(warna.getColor());
		jenisSop.setWarnatext(warnatext.getColor());
		jenisSop.setDiperuntukkan((String) diperuntukkan.getSelectedItem().getValue());
		jenisSop.setJenisPengguna(jenisPengguna.getValue());
		jenisSop.setUsernamePengguna(usernamePengguna.getValue());
		jenisSop.setAktorSop(
				(AktorSop) (aktorSop.getSelectedItem() == null ? null : aktorSop.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, jenisSop);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisSop.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisSop> jenisSop = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisSop);
		grid.setRowRenderer(new JenisSopRenderer());
		grid.setModelCheckMobile(strset);

	}

}
