package ais.action.master.pkl;

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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.pkl.KomponenPenilaianPkl;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk komponen penilaian pkl. Tipe ini merupakan titik masuk UI yang
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
public class KomponenPenilaianPklAction extends GenericAutowireComposer
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

	private KomponenPenilaianPkl komponenPenilaianPkl;
	private MyToolbarbuttonConfig add;
	private MyIntbox nomorUrut;
	private Combobox parent;
	private MyDoublebox bobot;

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

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(KomponenPenilaianPkl.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			KomponenPenilaianPkl etosKerja = new KomponenPenilaianPkl(1);
			etosKerja.setNama("Etos Kerja");
			session.save(etosKerja);

			KomponenPenilaianPkl sikap = new KomponenPenilaianPkl(2);
			sikap.setNama("Sikap");
			session.save(sikap);

			KomponenPenilaianPkl hubunganSosial = new KomponenPenilaianPkl(3);
			hubunganSosial.setNama("Hubungan Sosial");
			session.save(hubunganSosial);

			KomponenPenilaianPkl komponenPenilaianPkl = new KomponenPenilaianPkl(1);
			komponenPenilaianPkl.setNama("Disiplin waktu");

			komponenPenilaianPkl.setParent(etosKerja);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(2);
			komponenPenilaianPkl.setNama("Kehadiran di tempat kerja");
			komponenPenilaianPkl.setParent(etosKerja);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(3);
			komponenPenilaianPkl.setNama("Kemauan kerja dan motivasi kerja");
			komponenPenilaianPkl.setParent(etosKerja);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(4);
			komponenPenilaianPkl.setNama("Ketepatanwaktu pelaksanaan tugas");
			komponenPenilaianPkl.setParent(etosKerja);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(5);
			komponenPenilaianPkl.setNama("Kualitas hasil kerja");
			komponenPenilaianPkl.setParent(etosKerja);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);
			komponenPenilaianPkl = new KomponenPenilaianPkl(6);
			komponenPenilaianPkl.setNama("Efektifitas dan efesiensi kerja");
			komponenPenilaianPkl.setParent(etosKerja);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);
			komponenPenilaianPkl = new KomponenPenilaianPkl(7);
			komponenPenilaianPkl.setNama("Tanggung jawab");
			komponenPenilaianPkl.setParent(etosKerja);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);
			komponenPenilaianPkl = new KomponenPenilaianPkl(8);
			komponenPenilaianPkl.setNama("Penggunaan prosedur kerja (SOP)");
			komponenPenilaianPkl.setParent(etosKerja);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(9);
			komponenPenilaianPkl.setNama("Penguasaan penggunaan alat berikut perlengkapannya");
			komponenPenilaianPkl.setParent(etosKerja);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(10);
			komponenPenilaianPkl.setNama("kemapuan berkomunikasi");
			komponenPenilaianPkl.setParent(etosKerja);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(1);
			komponenPenilaianPkl.setNama("Cara berpakaian");
			komponenPenilaianPkl.setParent(sikap);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(2);
			komponenPenilaianPkl.setNama("Prilaku");
			komponenPenilaianPkl.setParent(sikap);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(3);
			komponenPenilaianPkl.setNama("Sikap terhadap Pimpinan");
			komponenPenilaianPkl.setParent(sikap);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(4);
			komponenPenilaianPkl.setNama("Resposif terhadap tugas yang diberikan");
			komponenPenilaianPkl.setParent(sikap);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(5);
			komponenPenilaianPkl.setNama("Proaktif dalam melaksanakan tugas");
			komponenPenilaianPkl.setParent(sikap);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(6);
			komponenPenilaianPkl.setNama("Resfek terhadap intruksi kerja yang diberikan");
			komponenPenilaianPkl.setParent(sikap);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(1);
			komponenPenilaianPkl.setNama("Bagaimana Hubungan dengan pimpinan");
			komponenPenilaianPkl.setParent(hubunganSosial);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(2);
			komponenPenilaianPkl.setNama("Hubungan dengan karyawan");
			komponenPenilaianPkl.setParent(hubunganSosial);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(3);
			komponenPenilaianPkl.setNama("Hubungan dengan seluruh peserta PKL");
			komponenPenilaianPkl.setParent(hubunganSosial);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(4);
			komponenPenilaianPkl.setNama("Hubungan dengan masyarakat lingkunagan tempat kerja");
			komponenPenilaianPkl.setParent(hubunganSosial);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(5);
			komponenPenilaianPkl.setNama("Kepribadian yang baik yang menunjang kerja");
			komponenPenilaianPkl.setParent(hubunganSosial);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(6);
			komponenPenilaianPkl.setNama("Etika sopan santun dalam melaksanakan tugas");
			komponenPenilaianPkl.setParent(hubunganSosial);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

			komponenPenilaianPkl = new KomponenPenilaianPkl(7);
			komponenPenilaianPkl.setNama("Menepatkan diri secara profesional");
			komponenPenilaianPkl.setParent(hubunganSosial);
			komponenPenilaianPkl.setBobot(1.0);
			session.save(komponenPenilaianPkl);

		}

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

		String[] contents = new String[] { "id", "nama", "keterangan", "bobot", "nomorUrut", "parent" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KomponenPenilaianPkl.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class KomponenPenilaianPklRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KomponenPenilaianPkl komponenPenilaianPkl = (KomponenPenilaianPkl) arg1;

			RevisiHelper
					.createNewRevisi(KomponenPenilaianPkl.class, komponenPenilaianPkl, komponenPenilaianPkl.getNama())
					.setParent(arg0);
			new Label(komponenPenilaianPkl.getNomorUrut().toString()).setParent(arg0);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(komponenPenilaianPkl.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianPkl.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianPkl);
				}
			});
			new Label(komponenPenilaianPkl.getParent() == null ? "" : komponenPenilaianPkl.getParent().getNama())
					.setParent(arg0);
			new Label(Common.numberFormat.get().format(komponenPenilaianPkl.getBobot())).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			final MyCheckboxConfig dosen1 = new MyCheckboxConfig("Dsn1");
			dosen1.setChecked(komponenPenilaianPkl.getDosen1());
			dosen1.setParent(hbox);
			dosen1.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianPkl.setDosen1(dosen1.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianPkl);
				}
			});

			final MyCheckboxConfig dosen2 = new MyCheckboxConfig("Dsn2");
			dosen2.setChecked(komponenPenilaianPkl.getDosen2());
			dosen2.setParent(hbox);
			dosen2.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianPkl.setDosen2(dosen2.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianPkl);
				}
			});
			final MyCheckboxConfig dosen3 = new MyCheckboxConfig("Dsn3");
			dosen3.setChecked(komponenPenilaianPkl.getDosen3());
			dosen3.setParent(hbox);
			dosen3.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianPkl.setDosen3(dosen3.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianPkl);
				}
			});
			final MyCheckboxConfig dosen4 = new MyCheckboxConfig("Dsn4");
			dosen4.setChecked(komponenPenilaianPkl.getDosen4());
			dosen4.setParent(hbox);
			dosen4.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianPkl.setDosen4(dosen4.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianPkl);
				}
			});
			final MyCheckboxConfig dosen5 = new MyCheckboxConfig("Dsn5");
			dosen5.setChecked(komponenPenilaianPkl.getDosen5());
			dosen5.setParent(hbox);
			dosen5.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianPkl.setDosen5(dosen5.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianPkl);
				}
			});

			new Label(komponenPenilaianPkl.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, komponenPenilaianPkl, KomponenPenilaianPklAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KomponenPenilaianPkl());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		komponenPenilaianPkl = (KomponenPenilaianPkl) obj;
		init(komponenPenilaianPkl);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KomponenPenilaianPkl komponenPenilaianPkl) {
		this.komponenPenilaianPkl = komponenPenilaianPkl;
		addWindow.setTitle(komponenPenilaianPkl.getId() == null ? "Tambah Komponen Penilaian" : "Ubah Komponen Penilaian");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Komponen Penilaian"));
		row.appendChild(nama = new Textbox(komponenPenilaianPkl.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Urut"));
		row.appendChild(nomorUrut = new MyIntbox(komponenPenilaianPkl.getNomorUrut()));
		nomorUrut.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Induk"));
		row.appendChild(parent = new Combobox());
		parent.setWidth("90%");
		Common.insertComboDanSemua(parent, "nama", KomponenPenilaianPkl.class);
		Common.selectComboItem(parent, komponenPenilaianPkl.getParent());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bobot"));
		row.appendChild(bobot = new MyDoublebox(komponenPenilaianPkl.getBobot()));
		bobot.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(komponenPenilaianPkl.getKeterangan()));
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
			MyMessageboxConfig.show("Mohon maaf, Nama Komponen Penilaian belum diisi. Langkah yang dapat dilakukan: (1) Ketikkan nama komponen penilaian PKL pada kolom \"Nama Komponen Penilaian\"; (2) Pastikan nama tidak hanya berisi spasi; (3) Klik Simpan kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (komponenPenilaianPkl.getId() != null) {
			komponenPenilaianPkl = (KomponenPenilaianPkl) session.load(KomponenPenilaianPkl.class,
					komponenPenilaianPkl.getId());

		}

		komponenPenilaianPkl.setNama(nama.getValue());
		komponenPenilaianPkl.setNomorUrut(nomorUrut.getValue());
		komponenPenilaianPkl.setParent(
				(KomponenPenilaianPkl) (parent.getSelectedItem() == null ? null : parent.getSelectedItem().getValue()));
		komponenPenilaianPkl.setBobot(bobot.getValue());
		komponenPenilaianPkl.setKeterangan(keterangan.getValue());
		
		Common.refreshSaveOrUpdate(session, komponenPenilaianPkl);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KomponenPenilaianPkl.class).createAlias("parent", "parent",
				Criteria.LEFT_JOIN);

		if (order)
			criteria.addOrder(Order.asc("parent.nomorUrut")).addOrder(Order.asc("nomorUrut"))
					.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KomponenPenilaianPkl> komponenPenilaianPkl = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(komponenPenilaianPkl);
		grid.setRowRenderer(new KomponenPenilaianPklRenderer());
		grid.setModelCheckMobile(strset);

	}

}
