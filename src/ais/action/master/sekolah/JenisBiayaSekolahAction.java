package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.math.BigDecimal;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
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
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.KanalPembayaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk jenis biaya sekolah. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchyayasan}, {@code Combobox
 * searchsekolah}, {@code Checkbox searchaktif}, {@code Textbox nama}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class JenisBiayaSekolahAction extends GenericAutowireComposer
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
	private Checkbox searchaktif;

	private Textbox nama;
	private Combobox sekolah;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisBiayaSekolah jenisBiayaSekolah;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Textbox kode;
	private Combobox periode;
	private Decimalbox untukBulan;
	private Decimalbox untukTahun;
	private Decimalbox mulaiDitagihDiBulan;
//	private MyCheckboxConfig tagihanPerSiswaBerbeda;
	private MyCheckboxConfig gunakanCalonSiswa;
	private MyCheckboxConfig bolehAngsurBerapapun;
	private Checkbox gunakanLes;
	private MyCheckboxConfig gelombangTertentu;
	private MyCheckboxConfig paketTertentu;
	private MyCheckboxConfig pilihanItemBiayaTerakumulasiBulanan;
	private Combobox kanalPembayaran;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		org.zkoss.zul.Div mainContainer = (org.zkoss.zul.Div) comp.getFellow("mainContainer");
		int[] tabAktif = {0};
		ais.ui.util.MyButtonTabbox btabs = ais.ui.util.MyButtonTabbox.buat(mainContainer, "100%", tabAktif);

		// Tab 0: Jenis Pembayaran (inline)
		org.zkoss.zul.Div panel0 = btabs.tambahTab(0, "Jenis Pembayaran");
		ais.ui.util.MyButtonTabbox.muatZulEager(panel0,
			"/WEB-INF/z/x/y/pages/master/sekolah/jenis_biaya_sekolah_tab_0.zul");

		super.doAfterCompose(comp);

		// Tab 1: Kanal Pembayaran (lazy/include)
		final String src1 = "/WEB-INF/z/x/y/pages/master/sekolah/kanal_pembayaran.zul";
		btabs.tambahTabLazy(1, "Kanal Pembayaran", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(org.zkoss.zul.Div panel) throws Exception {
				ais.ui.util.MyButtonTabbox.muatZul(panel, src1);
			}
		});

		btabs.pulihkanSeleksi(2);

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

		String[] contents = new String[] { "id", "nama", "periode", "untukBulan", "untukTahun", "mulaiDitagihDiBulan",
				"sekolah", "keterangan", "aktif", "tagihanPerSiswaBerbeda", "gunakanCalonSiswa" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisBiayaSekolah.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link JenisBiayaSekolahAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link JenisBiayaSekolahAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see JenisBiayaSekolahAction
	 */
	class JenisBiayaSekolahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisBiayaSekolah jenisBiayaSekolah = (JenisBiayaSekolah) arg1;

			new Label(jenisBiayaSekolah.getKode()).setParent(arg0);
			Vbox a;
			(a = RevisiHelper.createNewRevisi(JenisBiayaSekolah.class, jenisBiayaSekolah, jenisBiayaSekolah.getNama()))
					.setParent(arg0);

			if (jenisBiayaSekolah.getKanalPembayaran() != null) {
				new Label(jenisBiayaSekolah.getKanalPembayaran().getNama()).setParent(a);
			}

			new Label(jenisBiayaSekolah.getPeriode()).setParent(arg0);

			new Label(
					jenisBiayaSekolah.getUntukBulan() == null ? "Semua" : jenisBiayaSekolah.getUntukBulan().toString())
					.setParent(arg0);
			new Label(
					jenisBiayaSekolah.getUntukTahun() == null ? "Semua" : jenisBiayaSekolah.getUntukTahun().toString())
					.setParent(arg0);

			new Label(jenisBiayaSekolah.getMulaiDitagihDiBulan() == null ? "Default"
					: jenisBiayaSekolah.getMulaiDitagihDiBulan().toString()).setParent(arg0);

			new Label(jenisBiayaSekolah.getGunakanCalonSiswa() ? "Ya" : "Tidak").setParent(arg0);
//			new Label(jenisBiayaSekolah.getTagihanPerSiswaBerbeda() ? "Ya" : "Tidak").setParent(arg0);

			new Label(jenisBiayaSekolah.getSekolah() == null ? "" : jenisBiayaSekolah.getSekolah().getNama())
					.setParent(arg0);

			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(Tagihan.class)
					.createAlias("pembayaranSiswaDetail", "pembayaranSiswaDetail")
					.createAlias("pembayaranSiswaDetail.pembayaranSiswa", "pembayaranSiswa")
					.add(Restrictions.eq("pembayaranSiswa.jenisBiayaSekolah", jenisBiayaSekolah))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			boolean buttonDelete = delete;
			if (Common.bolehKonfigurasi("pembayaran_siswa_yang_sudah_dibayar_tidak_bisa_dihapus")) {
				buttonDelete = delete && count == 0;
			}

			new Label(jenisBiayaSekolah.getKeterangan() + "(jml pemb. " + Common.numberFormat.get().format(count) + ")")
					.setParent(arg0);

			new Label(jenisBiayaSekolah.getBolehAngsurBerapapun() ? "Ya" : "Tidak").setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisBiayaSekolah.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisBiayaSekolah.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisBiayaSekolah);
				}
			});

			Common.copyEditDeleteButtons(edit, buttonDelete, jenisBiayaSekolah, JenisBiayaSekolahAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisBiayaSekolah());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisBiayaSekolah = (JenisBiayaSekolah) obj;
		init(jenisBiayaSekolah);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final JenisBiayaSekolah jenisBiayaSekolah) throws Exception {
		this.jenisBiayaSekolah = jenisBiayaSekolah;
		addWindow.setTitle(jenisBiayaSekolah.getId() == null ? "Tambah Jenis Biaya" : "Ubah Jenis Biaya");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Jenis Biaya *"));
		row.appendChild(kode = new Textbox(jenisBiayaSekolah.getKode()));
		kode.setWidth("90%");
		kode.setMaxlength(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Biaya *"));
		row.appendChild(nama = new Textbox(jenisBiayaSekolah.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Periode *"));
		row.appendChild(periode = new Combobox());
		for (String s : new String[] { "Bulanan", "Harian", "Insidentil" }) {
			Comboitem comboitem = new Comboitem(s);
			comboitem.setValue(s);
			periode.appendChild(comboitem);
		}

		Common.selectComboItem(periode, jenisBiayaSekolah.getPeriode());
		periode.setWidth("90%");
		periode.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Untuk Bulan"));
		row.appendChild(untukBulan = new Decimalbox(
				jenisBiayaSekolah.getUntukBulan() == null ? null : new BigDecimal(jenisBiayaSekolah.getUntukBulan())));

		final Row a = Common.initKeterangan(rows, "* Kosongkan bulan jika berlaku untuk semua bulan");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Untuk Tahun"));
		row.appendChild(untukTahun = new Decimalbox(
				jenisBiayaSekolah.getUntukTahun() == null ? null : new BigDecimal(jenisBiayaSekolah.getUntukTahun())));

		final Row b = Common.initKeterangan(rows, "* Kosongkan tahun jika berlaku untuk semua tahun");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ditagih di mulai Bulan"));
		row.appendChild(mulaiDitagihDiBulan = new Decimalbox(jenisBiayaSekolah.getMulaiDitagihDiBulan() == null ? null
				: new BigDecimal(jenisBiayaSekolah.getMulaiDitagihDiBulan())));

		final Row c = Common.initKeterangan(rows,
				"* Kosongkan bulan jika menggunakan default atau jika bukan menggunakan tagihan bulanan");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(gunakanCalonSiswa = new MyCheckboxConfig(
				"Pembayaran ini untuk Calon Siswa, jika tidak dipilih untuk Siswa"));
		gunakanCalonSiswa.setChecked(jenisBiayaSekolah.getGunakanCalonSiswa());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(gunakanLes = new MyCheckboxConfig("Pembayaran ini untuk kelas les / kursus / private"));
		gunakanLes.setChecked(jenisBiayaSekolah.getGunakanLes());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(gelombangTertentu = new MyCheckboxConfig("Pembayaran ini khusus untuk gelombang tertentu"));
		gelombangTertentu.setChecked(jenisBiayaSekolah.getGelombangTertentu());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(paketTertentu = new MyCheckboxConfig("Pembayaran ini khusus untuk paket tertentu"));
		paketTertentu.setChecked(jenisBiayaSekolah.getPaketTertentu());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(bolehAngsurBerapapun = new MyCheckboxConfig("Boleh Angsur Berapapun Diterima"));
		bolehAngsurBerapapun.setChecked(jenisBiayaSekolah.getBolehAngsurBerapapun());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(pilihanItemBiayaTerakumulasiBulanan = new MyCheckboxConfig(
				"Pilihan Item Biaya Terakumulasi Pembayaran Bulanan"));
		pilihanItemBiayaTerakumulasiBulanan.setChecked(jenisBiayaSekolah.getPilihanItemBiayaTerakumulasiBulanan());

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, jenisBiayaSekolah.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, jenisBiayaSekolah.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisBiayaSekolah.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				String p = (String) (periode.getSelectedItem() == null ? "Insidentil"
						: periode.getSelectedItem().getValue());

				if (p.equalsIgnoreCase("Bulanan")) {

					a.setVisible(false);
					b.setVisible(false);
					c.setVisible(true);

					untukTahun.getParent().setVisible(false);
					untukBulan.getParent().setVisible(false);

					bolehAngsurBerapapun.getParent().setVisible(false);
					mulaiDitagihDiBulan.getParent().setVisible(true);
				} else {

					a.setVisible(true);
					b.setVisible(true);
					c.setVisible(false);

					untukTahun.getParent().setVisible(true);
					untukBulan.getParent().setVisible(true);

					bolehAngsurBerapapun.getParent().setVisible(true);
					mulaiDitagihDiBulan.getParent().setVisible(false);
				}

			}
		};

		eventListener.onEvent(null);
		periode.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kanal Pembayaran"));
		row.appendChild(kanalPembayaran = new Combobox());
		kanalPembayaran.setWidth("90%");
		kanalPembayaran.setReadonly(true);

		EventListener eventListenerKanal = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				Common.insertComboDanSemua(kanalPembayaran, new String[] { "nama" }, "keterangan",
						KanalPembayaran.class, "Ikuti Kanal Pembayaran Default",
						Restrictions.and(
								Restrictions.or(Restrictions.isNull("sekolah"),
										s == null || s.getId() == null ? Restrictions.sqlRestriction("false")
												: Restrictions.eq("sekolah.id", s.getId())),
								Restrictions.eq("aktif", true)));
				Common.selectComboItem(kanalPembayaran, jenisBiayaSekolah.getKanalPembayaran());
			}
		};
		sekolah.addEventListener("onChange", eventListenerKanal);
		Common.createDefaultTimer(eventListenerKanal);

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

		if (jenisBiayaSekolah.getId() != null) {
			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(Tagihan.class)
					.add(Restrictions.isNotNull("pembayaranSiswaDetail"))
					.createAlias("pengaturanBiaya", "pengaturanBiaya")
					.add(Restrictions.eq("pengaturanBiaya.jenisBiayaSekolah", jenisBiayaSekolah))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (count > 0) {
				Common.freezeGanti(yayasan, sekolah, gunakanCalonSiswa);
			}

		}

	}

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Jenis Biaya harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Jenis Biaya harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (periode.getSelectedItem() == null) {
			MyMessageboxConfig.show("Periode harus diisi", "Peringatan", MyMessageboxConfig.OK,
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
		if (jenisBiayaSekolah.getId() != null) {
			jenisBiayaSekolah = (JenisBiayaSekolah) session.load(JenisBiayaSekolah.class, jenisBiayaSekolah.getId());

		}

		jenisBiayaSekolah.setNama(nama.getValue());
		jenisBiayaSekolah.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		jenisBiayaSekolah.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		jenisBiayaSekolah.setPeriode((String) periode.getSelectedItem().getValue());

		jenisBiayaSekolah.setKanalPembayaran((KanalPembayaran) (kanalPembayaran.getSelectedItem() == null ? null
				: kanalPembayaran.getSelectedItem().getValue()));

		jenisBiayaSekolah.setKeterangan(keterangan.getValue());
		jenisBiayaSekolah.setKode(kode.getValue());
		jenisBiayaSekolah.setUntukBulan(untukBulan.getValue() == null ? null : untukBulan.getValue().intValue());
		jenisBiayaSekolah.setUntukTahun(untukTahun.getValue() == null ? null : untukTahun.getValue().intValue());
		jenisBiayaSekolah
				.setMulaiDitagihDiBulan(mulaiDitagihDiBulan == null || mulaiDitagihDiBulan.getValue() == null ? null
						: mulaiDitagihDiBulan.getValue().intValue());
		jenisBiayaSekolah.setGunakanCalonSiswa(gunakanCalonSiswa.isChecked());
		jenisBiayaSekolah.setBolehAngsurBerapapun(bolehAngsurBerapapun.isChecked());
		jenisBiayaSekolah.setGunakanLes(gunakanLes.isChecked());
		jenisBiayaSekolah.setGelombangTertentu(gelombangTertentu.isChecked());
		jenisBiayaSekolah.setPilihanItemBiayaTerakumulasiBulanan(pilihanItemBiayaTerakumulasiBulanan.isChecked());
		jenisBiayaSekolah.setPaketTertentu(paketTertentu.isChecked());

		Common.refreshSaveOrUpdate(session, jenisBiayaSekolah);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisBiayaSekolah.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
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

		List<JenisBiayaSekolah> jenisBiayaSekolah = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisBiayaSekolah);
		grid.setRowRenderer(new JenisBiayaSekolahRenderer());
		grid.setModelCheckMobile(strset);

	}

}
