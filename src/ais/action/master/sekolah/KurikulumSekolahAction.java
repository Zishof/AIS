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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.KurikulumPunyaMatapelajaran;
import ais.database.model.sekolah.KurikulumSekolah;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk kurikulum sekolah. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchyayasan}, {@code Combobox
 * searchsekolah}, {@code Textbox nama}, {@code Combobox sekolah}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code ubahKurikulum()}, {@code onSave()});
 * operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
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
public class KurikulumSekolahAction extends GenericAutowireComposer
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

	private KurikulumSekolah kurikulumSekolah;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	
	public static String[] contents = new String[] { "id", "nama", "sekolah", "jenisPenilaian", "keterangan", "aktif" };

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

		
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KurikulumSekolah.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link KurikulumSekolahAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KurikulumSekolahAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KurikulumSekolahAction
	 */
	class KurikulumSekolahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KurikulumSekolah kurikulumSekolah = (KurikulumSekolah) arg1;

			RevisiHelper.createNewRevisi(KurikulumSekolah.class, kurikulumSekolah, kurikulumSekolah.getNama())
					.setParent(arg0);
			new Label(kurikulumSekolah.getJenisPenilaian() == null ? "Ikuti jenis penilaian matapelajaran"
					: kurikulumSekolah.getJenisPenilaian().getNama()).setParent(arg0);
			new Label(kurikulumSekolah.getSekolah() == null ? "" : kurikulumSekolah.getSekolah().getNama())
					.setParent(arg0);
			new Label(kurikulumSekolah.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kurikulumSekolah.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kurikulumSekolah.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kurikulumSekolah);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, kurikulumSekolah, KurikulumSekolahAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KurikulumSekolah());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kurikulumSekolah = (KurikulumSekolah) obj;
		init(kurikulumSekolah);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private Grid myGrid;
	private Combobox jenisPenilaian;

	@SuppressWarnings({ "deprecation" })
	private void init(final KurikulumSekolah kurikulumSekolah) {
		this.kurikulumSekolah = kurikulumSekolah;
		addWindow.setTitle(kurikulumSekolah.getId() == null ? "Tambah Kurikulum Sekolah" : "Ubah Kurikulum Sekolah");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kurikulum Sekolah *"));
		row.appendChild(nama = new Textbox(kurikulumSekolah.getNama()));
		nama.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, kurikulumSekolah.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, kurikulumSekolah.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Penilaian *"));
		row.appendChild(jenisPenilaian = new Combobox());
		jenisPenilaian.setWidth("90%");
		jenisPenilaian.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);
				Common.insertComboDanSemua(jenisPenilaian, new String[] { "jenis", "sekolah" }, "yayasan",
						JenisPenilaian.class, "==Ikuti jenis penilaian matapelajaran==",
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(jenisPenilaian, kurikulumSekolah.getJenisPenilaian());

			}
		};

		sekolah.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kurikulumSekolah.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		final Rows rowdata = (Rows) Common.tampilanScroll1(row).getParent();

		try {
			ubahKurikulum(rowdata);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		sekolah.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ubahKurikulum(rowdata);
			}
		});

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

		Common.createDefaultTimer(eventListener);
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void ubahKurikulum(Rows rows) throws Exception {

		Common.clear(rows);

		Sekolah sekolah = (Sekolah) (this.sekolah.getSelectedItem() == null ? null
				: this.sekolah.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		List<Matapelajaran> matapelajarans = ConstantValues
				.simpleList(
						session.createCriteria(Matapelajaran.class).add(Restrictions.eq("sekolah", sekolah))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("urutan")),
						Matapelajaran.class);
		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("Matapelajaran"));

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		myGrid = new Grid();
		myGrid.setParent(row);
		Rows myrows = new Rows();
		myrows.setParent(myGrid);

		Columns columns = new Columns();
		columns.setParent(myGrid);

		MyColumnConfig column = new MyColumnConfig("Nama Matapelajaran");
		column.setParent(columns);

		column = new MyColumnConfig("Jumlah Jam");
		column.setParent(columns);
		column.setWidth("30%");

		for (Matapelajaran matapelajaran : matapelajarans) {

			final MyFormRow rowData1 = new MyFormRow();
			rowData1.setParent(myrows);

			KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran = null;
			if (kurikulumSekolah.getId() != null) {
				kurikulumPunyaMatapelajaran = (KurikulumPunyaMatapelajaran) session
						.createCriteria(KurikulumPunyaMatapelajaran.class)
						.add(Restrictions.eq("kurikulumSekolah", kurikulumSekolah))
						.add(Restrictions.eq("matapelajaran", matapelajaran)).setMaxResults(1).uniqueResult();
			}
			if (kurikulumPunyaMatapelajaran == null) {
				kurikulumPunyaMatapelajaran = new KurikulumPunyaMatapelajaran();
			}
			kurikulumPunyaMatapelajaran.setKurikulumSekolah(kurikulumSekolah);
			kurikulumPunyaMatapelajaran.setMatapelajaran(matapelajaran);
			rowData1.setAttribute("kurikulumPunyaMatapelajaran", kurikulumPunyaMatapelajaran);

			final Checkbox checkbox = new Checkbox(matapelajaran.getNama());
			final MyDoublebox jumlahJamPelajaran = new MyDoublebox(kurikulumPunyaMatapelajaran.getJumlahJamPelajaran());
			jumlahJamPelajaran.setWidth("95%");

			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran = (KurikulumPunyaMatapelajaran) rowData1
							.getAttribute("kurikulumPunyaMatapelajaran");
					kurikulumPunyaMatapelajaran.setAktif(checkbox.isChecked());

					jumlahJamPelajaran.setDisabled(!checkbox.isChecked());

					if (kurikulumPunyaMatapelajaran.getId() != null) {
						Common.refreshUpdate(kurikulumPunyaMatapelajaran);
					}

				}
			});
			checkbox.setChecked(kurikulumPunyaMatapelajaran.getAktif());
			jumlahJamPelajaran.setDisabled(!checkbox.isChecked());

			jumlahJamPelajaran.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran = (KurikulumPunyaMatapelajaran) rowData1
							.getAttribute("kurikulumPunyaMatapelajaran");
					kurikulumPunyaMatapelajaran.setJumlahJamPelajaran(jumlahJamPelajaran.getValue());

					if (kurikulumPunyaMatapelajaran.getId() != null) {
						Common.refreshUpdate(kurikulumPunyaMatapelajaran);
					}

				}
			});

			rowData1.appendChild(checkbox);
			rowData1.appendChild(jumlahJamPelajaran);
		}
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Kurikulum Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
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
		if (kurikulumSekolah.getId() != null) {
			kurikulumSekolah = (KurikulumSekolah) session.load(KurikulumSekolah.class, kurikulumSekolah.getId());

		}

		kurikulumSekolah.setNama(nama.getValue());
		kurikulumSekolah.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		kurikulumSekolah.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		kurikulumSekolah.setJenisPenilaian((JenisPenilaian) (jenisPenilaian.getSelectedItem() == null ? null
				: jenisPenilaian.getSelectedItem().getValue()));
		kurikulumSekolah.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, kurikulumSekolah);

		List<Row> rows = myGrid.getRows().getChildren();
		for (Row row : rows) {
			KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran = (KurikulumPunyaMatapelajaran) row
					.getAttribute("kurikulumPunyaMatapelajaran");
			kurikulumPunyaMatapelajaran.setKurikulumSekolah(kurikulumSekolah);

			Common.refreshSaveOrUpdate(session, kurikulumPunyaMatapelajaran);

		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KurikulumSekolah.class);

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

		List<KurikulumSekolah> kurikulumSekolah = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kurikulumSekolah);
		grid.setRowRenderer(new KurikulumSekolahRenderer());
		grid.setModelCheckMobile(strset);

	}

}
