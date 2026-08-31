package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
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
import ais.action.master.sekolah.helper.DiskonSiswaPunyaSiswaHelper;
import ais.action.master.sekolah.helper.DiskonSiswaSyncHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonDashboardHtmlHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.sekolah.DiskonSiswa;
import ais.database.model.sekolah.DiskonSiswaItemBiaya;
import ais.database.model.sekolah.DiskonSiswaPunyaSiswa;
import ais.database.model.sekolah.ItemBiayaSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk diskon siswa. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Html dashboardHtml}, {@code Html progressHtml}, {@code Textbox
 * searchnama}, {@code Textbox searchsiswa}, {@code Combobox searchyayasan}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code loadItemBiaya()}, {@code onSearchDefault()}, {@code refreshDashboard()}); mutasi
 * data ({@code onSave()}); operasi domain lain ({@code onAdd()}, {@code sinkronkanTagihanSesuaiFilter()}, {@code
 * countDashboard()}, {@code showProgress()}, {@code hideProgress()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
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
public class DiskonSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Html dashboardHtml;
	private Html progressHtml;

	private Textbox searchnama;
	private Textbox searchsiswa;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Checkbox searchaktif;

	private Combobox tahunAjaran;
	private Combobox sekolah;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private DiskonSiswa diskonSiswa;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private List<Checkbox> selectedItemBiayaSekolah;
	private Row rowItemBiaya;
	// PERMINTAAN: pencarian nama + paging 10/halaman utk checklist item biaya di bawah, sama
	// seperti perbaikan di SetingBiayaAction.java/PengaturanBiayaAction.java. PENTING: onSave()
	// di sini melakukan DELETE MENYELURUH lalu re-insert HANYA dari selectedItemBiayaSekolah
	// ([DiskonSiswaAction.java] "delete from sekolah.diskon_siswa_item_biaya where
	// diskon_siswa=..." diikuti loop re-insert) -- kalau selectedItemBiayaSekolah hanya berisi
	// checkbox halaman yg sedang tampil, SEMUA item diskon di halaman LAIN akan ikut terhapus
	// permanen saat Simpan. Karena itu status per-item (checked/nilai) WAJIB dijaga di Map
	// stabil ini, TIDAK di-reset saat loadItemBiaya() dipanggil ulang krn pindah
	// halaman/pencarian -- hanya direset saat combo "Sekolah" berganti.
	private Textbox cariItemBiayaSekolah;
	private Paging pagingItemBiayaSekolah;
	private int halamanItemBiayaSekolah = 0;
	private Map<Long, DiskonSiswaItemBiaya> objekItemBiayaSekolahPerId = new HashMap<Long, DiskonSiswaItemBiaya>();
	private Map<Long, Boolean> checkedItemBiayaSekolahPerId = new HashMap<Long, Boolean>();
	private Map<Long, Checkbox> checkboxItemBiayaSekolahPerId = new LinkedHashMap<Long, Checkbox>();
	private Textbox nama;
	private MyCheckboxConfig menggunkanPersen;
	private Combobox jenis;
	private MyDatebox diskonMulai;
	private MyDatebox diskonSampai;

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
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "nama", "sekolah", "tahunAjaran", "jenis", "aktif", "memotongTagihan",
				"diskonMulai", "diskonSampai", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, DiskonSiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		String[] contentsDetail = new String[] { "id", "calonSiswa.noRegistrasi||siswa.nomorInduk",
				"calonSiswa.nama||siswa.nama", "diskonSiswa.nama", "diskonSiswa.tahunAjaran", "diskonSiswa.jenis",
				"keterangan", "setujui" };

		cetakToolbarbutton = Common.cetakDataCustomButton(DiskonSiswaPunyaSiswa.class, new DataCriteria() {

			@SuppressWarnings("unchecked")
			@Override
			public Object initCriteria(boolean order) {
				List<Long> diskonSiswas = DiskonSiswaAction.this.initCriteria(true)
						.setProjection(Projections.property("id")).list();
				Session session = HibernateUtil.currentSession();

				return session.createCriteria(DiskonSiswaPunyaSiswa.class)
						.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)
						.createAlias("siswa", "siswa", Criteria.LEFT_JOIN).addOrder(Order.asc("calonSiswa.nama"))
						.addOrder(Order.asc("siswa.nama"))
						.add(diskonSiswas.isEmpty() ? Restrictions.sqlRestriction("false")
								: Restrictions.in("diskonSiswa.id", diskonSiswas));
			}
		}, "Download Diskon Calon Siswa", "/img/print.png", contentsDetail);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Singkronkan Tagihan", "/img/new.gif");
		if (button != null) { button.setTooltiptext("Sinkronkan ulang nilai diskon ke tagihan sesuai filter yang sedang tampil."); }
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						sinkronkanTagihanSesuaiFilter();
						onSearchDefault(null);
					}
				});
			}

		});
		if (button != null) { button.setParent(add.getParent()); }

		onSearchDefault(null);

	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link DiskonSiswaAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DiskonSiswaAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code DiskonSiswaPunyaSiswaHelper
	 * diskonSiswaPunyaSiswaHelper}; operasi lokal: {@code render}(). Aturan bisnis bersama tetap berada pada kelas
	 * induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see DiskonSiswaAction
	 */
	class DiskonSiswaRenderer extends ais.ui.util.MyRowRenderer {

		private DiskonSiswaPunyaSiswaHelper diskonSiswaPunyaSiswaHelper = new DiskonSiswaPunyaSiswaHelper();

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DiskonSiswa diskonSiswa = (DiskonSiswa) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (detail.getChildren().isEmpty() && detail.isOpen()) {

						diskonSiswaPunyaSiswaHelper.display(diskonSiswa, detail, addWindow);

					}

				}

			});

			RevisiHelper.createNewRevisi(DiskonSiswa.class, diskonSiswa, diskonSiswa.getNama()).setParent(arg0);
			new Label(diskonSiswa.getTahunAjaran() == null || diskonSiswa.getTahunAjaran().trim().isEmpty() ? "Semua"
					: diskonSiswa.getTahunAjaran()).setParent(arg0);
			new Label(diskonSiswa.getSekolah() == null ? "" : diskonSiswa.getSekolah().getNama()).setParent(arg0);

			new Label(diskonSiswa.getJenis() == null ? "" : diskonSiswa.getJenis()).setParent(arg0);
			new Label((diskonSiswa.getDiskonMulai() == null ? ""
					: Common.dateFormat1.get().format(diskonSiswa.getDiskonMulai())) + " sd "
					+ (diskonSiswa.getDiskonSampai() == null ? ""
							: Common.dateFormat1.get().format(diskonSiswa.getDiskonSampai())))
					.setParent(arg0);

			Session session = HibernateUtil.currentSession();
			List<DiskonSiswaItemBiaya> selectedItemBiaya = session.createCriteria(DiskonSiswaItemBiaya.class)
					.createAlias("itemBiayaSekolah", "itemBiayaSekolah")
					.add(Restrictions.or(Restrictions.isNull("itemBiayaSekolah.aktif"),
							Restrictions.eq("itemBiayaSekolah.aktif", true)))
					.add(Restrictions.eq("diskonSiswa", diskonSiswa)).list();
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			int i = 1;
			for (DiskonSiswaItemBiaya itemBiaya : selectedItemBiaya) {
				vbox.appendChild(
						new MyLabelKecil(
								i + ". " + itemBiaya.getItemBiayaSekolah().getNama()
										+ (itemBiaya.getDefaultBiaya() > 0.1 ? " (Diskon : "
												+ Common.numberFormat.get().format(itemBiaya.getDefaultBiaya())
												+ (diskonSiswa.getMenggunkanPersen() ? "%" : "") + ")" : "")));
				i++;
			}
			selectedItemBiaya = null;

			new Label(diskonSiswa.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(diskonSiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					diskonSiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(diskonSiswa);
				}
			});

			final MyCheckboxConfig memotongTagihan = new MyCheckboxConfig("Memotong Tagihan");
			memotongTagihan.setDisabled(!edit);
			memotongTagihan.setChecked(diskonSiswa.getMemotongTagihan());
			memotongTagihan.setParent(arg0);
			memotongTagihan.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					diskonSiswa.setMemotongTagihan(memotongTagihan.isChecked());
					Common.refreshSaveOrUpdate(diskonSiswa);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, diskonSiswa, DiskonSiswaAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new DiskonSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		diskonSiswa = (DiskonSiswa) obj;

		init(diskonSiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "deprecation" })
	private void init(final DiskonSiswa diskonSiswa) throws Exception {
		this.diskonSiswa = diskonSiswa;
		addWindow.setTitle(diskonSiswa.getId() == null ? "Tambah Diskon Siswa" : "Ubah Diskon Siswa");
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

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		Sekolah selectedSekolah = SekolahUtil.getSekolah();
		if (selectedSekolah != null && selectedSekolah.getId() != null) {
			diskonSiswa.setYayasan(selectedSekolah.getYayasan());
			diskonSiswa.setSekolah(selectedSekolah);
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Diskon"));
		row.appendChild(nama = new Textbox(diskonSiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, diskonSiswa.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, diskonSiswa.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran"));
		Common.selectComboItem(true, tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
				diskonSiswa.getTahunAjaran());
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
		row.appendChild(jenis = new Combobox());
		for (String s : DiskonSiswa.JENIS) {
			MyComboitemConfig comboitemConfig = new MyComboitemConfig(s);
			comboitemConfig.setValue(s);
			jenis.appendChild(comboitemConfig);
		}
		MyComboitemConfig comboitemConfig = new MyComboitemConfig("Tanpa Jenis Diskon");
		comboitemConfig.setValue(null);
		jenis.appendChild(comboitemConfig);
		Common.selectComboItem(jenis, diskonSiswa.getJenis());
		jenis.setWidth("90%");
		jenis.setReadonly(true);

		if (diskonSiswa.getId() != null) {
			try {
				HibernateUtil.currentSession().refresh(this.diskonSiswa);
			} catch (org.hibernate.UnresolvableObjectException uoe) { ais.common.ErrorAuditUtil.record(uoe, "auto-audit(empty-catch) src/ais/action/master/sekolah/DiskonSiswaAction.java:390");
				// Baris DiskonSiswa sudah dihapus sesi lain; lanjut pakai data di memori.
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Persen"));
		menggunkanPersen = new MyCheckboxConfig("Menggunakan penghitungan persen");
		row.appendChild(menggunkanPersen);
		menggunkanPersen.setChecked(diskonSiswa.getMenggunkanPersen());

		// PERMINTAAN: pencarian nama item biaya + paging 10/halaman (lihat javadoc field
		// cariItemBiayaSekolah di atas). Baris pencarian ini baris 2-kolom NORMAL (label|isian)
		// -- JANGAN pakai setSpans("2") di sini (hanya boleh 1 anak, pernah bikin widget tak
		// tampil sama sekali di kasus serupa pada SetingBiayaAction.java).
		MyFormRow rowCariItemBiayaSekolah = new MyFormRow();
		rowCariItemBiayaSekolah.setParent(rows);
		rowCariItemBiayaSekolah.appendChild(new ais.ui.util.MyLabelConfig("Cari Item Biaya"));
		cariItemBiayaSekolah = new Textbox();
		cariItemBiayaSekolah.setWidth("90%");
		rowCariItemBiayaSekolah.appendChild(cariItemBiayaSekolah);
		cariItemBiayaSekolah.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				halamanItemBiayaSekolah = 0;
				loadItemBiaya();
			}
		});

		// Paging di baris tersendiri ber-setSpans("2") -- di baris ini HANYA satu anak
		// (widget Paging itu sendiri), jadi aman dipakai spans penuh.
		MyFormRow rowPagingItemBiayaSekolah = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowPagingItemBiayaSekolah, "2");
		rowPagingItemBiayaSekolah.setParent(rows);
		pagingItemBiayaSekolah = new Paging();
		pagingItemBiayaSekolah.setPageSize(10);
		pagingItemBiayaSekolah.setParent(rowPagingItemBiayaSekolah);
		pagingItemBiayaSekolah.addEventListener("onPaging", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				halamanItemBiayaSekolah = pagingItemBiayaSekolah.getActivePage();
				loadItemBiaya();
			}
		});

		rowItemBiaya = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowItemBiaya, "2");
		rowItemBiaya.setStyle("border:0px;background: transparent;");
		rowItemBiaya.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diskon Berlaku"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(diskonMulai = new MyDatebox(diskonSiswa.getDiskonMulai()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		hbox.appendChild(diskonSampai = new MyDatebox(diskonSiswa.getDiskonSampai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(diskonSiswa.getKeterangan()));
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

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				System.out.println(s);

				// Sekolah berganti -> daftar item biaya-nya berganti total, jadi cache
				// per-item (checked/nilai/checkbox) dan status pencarian+halaman WAJIB
				// direset. JANGAN reset ini di dalam loadItemBiaya() sendiri -- method itu
				// juga dipanggil ulang oleh kotak pencarian & paging, yang justru HARUS
				// mempertahankan cache ini.
				objekItemBiayaSekolahPerId.clear();
				checkedItemBiayaSekolahPerId.clear();
				checkboxItemBiayaSekolahPerId.clear();
				halamanItemBiayaSekolah = 0;
				if (cariItemBiayaSekolah != null) {
					cariItemBiayaSekolah.setValue("");
				}
				loadItemBiaya();
			}
		};
		sekolah.addEventListener("onChange", eventListener);
		Common.createDefaultTimer(eventListener);
	}

	@SuppressWarnings("unchecked")
	private void loadItemBiaya() {
		Common.clear(rowItemBiaya);

		MyGrid vboxSkala = new MyGrid();
		vboxSkala.setParent(rowItemBiaya);

		Columns columns = new Columns();
		columns.setParent(vboxSkala);

		MyColumnConfig column = new MyColumnConfig("Item Biaya");
		column.setParent(columns);
		column.setWidth("80%");

		column = new MyColumnConfig("Diskon");
		column.setParent(columns);

		Rows rowsSkala = new Rows();
		rowsSkala.setParent(vboxSkala);

		Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
		Session session = HibernateUtil.currentSession();
		List<ItemBiayaSekolah> itemBiayaSekolahSemua = ConstantValues.simpleList(
				session.createCriteria(ItemBiayaSekolah.class).addOrder(Order.asc("id"))
						.add(s == null ? Restrictions.sqlRestriction("false") : Restrictions.eq("sekolah", s))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				ItemBiayaSekolah.class);

		// PERMINTAAN: pencarian nama + paging 10/halaman -- lihat javadoc field
		// cariItemBiayaSekolah/objekItemBiayaSekolahPerId di atas soal kenapa status per-item
		// (checked/nilai) HARUS di-cache di Map stabil (bukan dibangun ulang dari nol tiap
		// loadItemBiaya() dipanggil), supaya onSave() (yg DELETE MENYELURUH lalu re-insert
		// dari selectedItemBiayaSekolah) tidak diam-diam menghapus item diskon di halaman lain.
		String kataCariItemBiayaSekolah = cariItemBiayaSekolah == null || cariItemBiayaSekolah.getValue() == null ? ""
				: cariItemBiayaSekolah.getValue().trim().toLowerCase();
		List<ItemBiayaSekolah> itemBiayaSekolahCocok = new ArrayList<ItemBiayaSekolah>();
		for (ItemBiayaSekolah ib : itemBiayaSekolahSemua) {
			if (kataCariItemBiayaSekolah.isEmpty()
					|| (ib.getNama() != null && ib.getNama().toLowerCase().contains(kataCariItemBiayaSekolah))) {
				itemBiayaSekolahCocok.add(ib);
			}
		}
		int totalHalamanItemBiayaSekolah = Math.max(1, (int) Math.ceil(itemBiayaSekolahCocok.size() / 10.0));
		if (halamanItemBiayaSekolah >= totalHalamanItemBiayaSekolah) {
			halamanItemBiayaSekolah = totalHalamanItemBiayaSekolah - 1;
		}
		if (halamanItemBiayaSekolah < 0) {
			halamanItemBiayaSekolah = 0;
		}
		if (pagingItemBiayaSekolah != null) {
			pagingItemBiayaSekolah.setTotalSize(itemBiayaSekolahCocok.size());
			pagingItemBiayaSekolah.setActivePage(halamanItemBiayaSekolah);
		}
		int mulaiItemBiayaSekolah = halamanItemBiayaSekolah * 10;
		int akhirItemBiayaSekolah = Math.min(mulaiItemBiayaSekolah + 10, itemBiayaSekolahCocok.size());
		List<ItemBiayaSekolah> itemBiayaSekolahs = mulaiItemBiayaSekolah >= akhirItemBiayaSekolah
				? new ArrayList<ItemBiayaSekolah>()
				: itemBiayaSekolahCocok.subList(mulaiItemBiayaSekolah, akhirItemBiayaSekolah);

		for (final ItemBiayaSekolah itemBiayaSekolah : itemBiayaSekolahs) {

			MyFormRow rowSkala = new MyFormRow();
			rowSkala.setStyle("border:0px;background: transparent;");
			rowSkala.setParent(rowsSkala);

			final DiskonSiswaItemBiaya diskonSiswaItemBiaya;
			final boolean checkedAwal;
			if (objekItemBiayaSekolahPerId.containsKey(itemBiayaSekolah.getId())) {
				// Item ini sudah pernah dirender sebelumnya dlm sesi dialog ini (mis. user
				// pindah halaman lalu balik lagi) -- PAKAI objek & status TERSIMPAN yg sama,
				// JANGAN query ulang dari database (supaya perubahan user tetap ada).
				diskonSiswaItemBiaya = objekItemBiayaSekolahPerId.get(itemBiayaSekolah.getId());
				checkedAwal = Boolean.TRUE.equals(checkedItemBiayaSekolahPerId.get(itemBiayaSekolah.getId()));
			} else {
				DiskonSiswaItemBiaya diskonSiswaItemBiayatemp = (DiskonSiswaItemBiaya) (diskonSiswa == null
						|| diskonSiswa.getId() == null
								? null
								: session.createCriteria(DiskonSiswaItemBiaya.class)
										.createAlias("itemBiayaSekolah", "itemBiayaSekolah")
										.add(Restrictions.or(Restrictions.isNull("itemBiayaSekolah.aktif"),
												Restrictions.eq("itemBiayaSekolah.aktif", true)))
										.add(Restrictions.eq("diskonSiswa", diskonSiswa)).setMaxResults(1)
										.add(Restrictions.eq("itemBiayaSekolah", itemBiayaSekolah)).uniqueResult());
				if (diskonSiswaItemBiayatemp == null) {
					diskonSiswaItemBiaya = new DiskonSiswaItemBiaya();
					diskonSiswaItemBiaya.setItemBiayaSekolah(itemBiayaSekolah);
				} else {
					diskonSiswaItemBiaya = diskonSiswaItemBiayatemp;
				}
				checkedAwal = diskonSiswaItemBiaya.getId() != null;
				objekItemBiayaSekolahPerId.put(itemBiayaSekolah.getId(), diskonSiswaItemBiaya);
				checkedItemBiayaSekolahPerId.put(itemBiayaSekolah.getId(), checkedAwal);
			}

			final MyDoublebox defaultDiskon = new MyDoublebox(diskonSiswaItemBiaya.getDefaultBiaya());

			final Checkbox checkbox = new Checkbox(itemBiayaSekolah.getNama());
			checkbox.setAttribute("diskonSiswaItemBiaya", diskonSiswaItemBiaya);
			checkbox.setParent(rowSkala);
			checkbox.setChecked(checkedAwal);
			checkboxItemBiayaSekolahPerId.put(itemBiayaSekolah.getId(), checkbox);

			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// PENTING utk paging aman: status centang harus disimpan ke Map stabil di
					// sini, bukan cuma hidup di widget checkbox yg akan dibuang saat pindah
					// halaman.
					checkedItemBiayaSekolahPerId.put(itemBiayaSekolah.getId(), checkbox.isChecked());
					defaultDiskon.setDisabled(!checkbox.isChecked());
				}
			});

			defaultDiskon.setParent(rowSkala);
			defaultDiskon.setWidth("90%");
			defaultDiskon.setDisabled(!checkbox.isChecked());
			defaultDiskon.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (checkbox.isChecked()) {
						diskonSiswaItemBiaya.setDefaultBiaya(defaultDiskon.getValue());
					}
				}
			});
		}

		// selectedItemBiayaSekolah dipakai onSave() -- dibangun dari cache
		// checkboxItemBiayaSekolahPerId (mencakup SEMUA item yg pernah dirender di sesi dialog
		// ini, lintas halaman, bukan cuma yg lagi tampil di halaman ini) supaya perubahan user
		// di halaman lain tidak hilang/terhapus saat "Simpan" diklik.
		selectedItemBiayaSekolah = new ArrayList<Checkbox>(checkboxItemBiayaSekolahPerId.values());
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Nama diskon harus diisi", "Peringatan", MyMessageboxConfig.OK,
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

		if (tahunAjaran.getValue() == null) {
			MyMessageboxConfig.show("Tahun ajaran masuk harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (diskonSiswa.getId() != null) {
			diskonSiswa = (DiskonSiswa) session.load(DiskonSiswa.class, diskonSiswa.getId());

		}
		diskonSiswa.setNama(nama.getValue());
		diskonSiswa.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		diskonSiswa.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		diskonSiswa.setKeterangan(keterangan.getValue());
		diskonSiswa.setTahunAjaran(
				(String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue()));

		diskonSiswa.setJenis((String) (jenis.getSelectedItem() == null ? null : jenis.getSelectedItem().getValue()));
		diskonSiswa.setDiskonMulai(diskonMulai.getValue());
		diskonSiswa.setDiskonSampai(diskonSampai.getValue());

		String diskonItem = "";
		for (Checkbox checkbox : selectedItemBiayaSekolah) {
			if (checkbox.isChecked()) {
				DiskonSiswaItemBiaya diskonSiswaItemBiaya = (DiskonSiswaItemBiaya) checkbox
						.getAttribute("diskonSiswaItemBiaya");
				diskonItem += diskonItem.isEmpty() ? diskonSiswaItemBiaya.getItemBiayaSekolah().getId().toString()
						: "," + diskonSiswaItemBiaya.getItemBiayaSekolah().getId();
			}
		}
		diskonSiswa.setItemBiaya(diskonItem);

		diskonSiswa.setMenggunkanPersen(menggunkanPersen.isChecked());
		Common.refreshSaveOrUpdate(session, diskonSiswa);

		session.createSQLQuery("delete from sekolah.diskon_siswa_item_biaya where diskon_siswa=" + diskonSiswa.getId())
				.executeUpdate();
		for (Checkbox checkbox : selectedItemBiayaSekolah) {
			if (checkbox.isChecked()) {
				DiskonSiswaItemBiaya diskonSiswaItemBiaya = (DiskonSiswaItemBiaya) checkbox
						.getAttribute("diskonSiswaItemBiaya");
				diskonSiswaItemBiaya.setDiskonSiswa(diskonSiswa);
				session.save(diskonSiswaItemBiaya);
			}
		}
		session.flush();

		return true;
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> idsiswas = new ArrayList<Long>();
		if (!searchsiswa.getValue().trim().isEmpty()) {

			idsiswas = session.createCriteria(DiskonSiswaPunyaSiswa.class).add(Restrictions.isNotNull("diskonSiswa"))
					.setProjection(Projections.groupProperty("diskonSiswa.id"))
					.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
					.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)

					.add(Restrictions.or(
							Restrictions.or(
									Restrictions.ilike("calonSiswa.nomorIndukNasional", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE),

									Restrictions.or(
											Restrictions.ilike("calonSiswa.namaSiswa", searchsiswa.getValue().trim(),
													MatchMode.ANYWHERE),
											Restrictions.ilike("calonSiswa.nomorInduk", searchsiswa.getValue().trim(),
													MatchMode.ANYWHERE))),

							Restrictions.or(
									Restrictions.ilike("siswa.nomorIndukNasional", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("siswa.nomorIndukSantri", searchsiswa.getValue().trim(),
													MatchMode.ANYWHERE),

											Restrictions.or(
													Restrictions.ilike("siswa.namaSiswa", searchsiswa.getValue().trim(),
															MatchMode.ANYWHERE),
													Restrictions.ilike("siswa.nomorInduk",
															searchsiswa.getValue().trim(), MatchMode.ANYWHERE))))))

					.list();

		}

		System.out.println("idsiswas -> " + idsiswas);

		Criteria criteria = session.createCriteria(DiskonSiswa.class)

				.add(!searchsiswa.getValue().trim().isEmpty() && idsiswas.isEmpty()
						? Restrictions.sqlRestriction("false")
						: idsiswas.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("id", idsiswas))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.desc("tahunAjaran")).addOrder(Order.asc("nama"));

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
		try {
			showProgress(10, "Membaca filter", "Daftar diskon sedang disiapkan.");
			refreshDashboard();

			showProgress(45, "Menghitung data", "Jumlah data diskon sesuai filter sedang dihitung.");
			Common.initPaging(initCriteria(false), paging);

			showProgress(75, "Mengambil daftar", "Data diskon halaman aktif sedang dimuat.");
			List<DiskonSiswa> diskonSiswa = ConstantValues
					.simpleList(
							initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
									Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
							DiskonSiswa.class);
			ListModel strset = new SimpleListModel(diskonSiswa);

			showProgress(95, "Menampilkan data", "Tabel diskon sedang disusun.");
			grid.setRowRenderer(new DiskonSiswaRenderer());
			grid.setModelCheckMobile(strset);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			hideProgress();
		}
	}

	@SuppressWarnings("unchecked")
	private void sinkronkanTagihanSesuaiFilter() {
		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Tagihan Diskon Siswa");
		try {
			showProgress(10, "Sinkronisasi diskon", "Membaca data diskon sesuai filter.");
			List<Long> diskonIds = initCriteria(true).setProjection(Projections.property("id")).list();
			showProgress(45, "Sinkronisasi tagihan", "Menghitung ulang tagihan siswa penerima diskon.");
			int jumlah = DiskonSiswaSyncHelper.sinkronkanBanyak(diskonIds, false, laporan);
			showProgress(100, "Sinkronisasi selesai", "Data tagihan yang diperbarui: " + jumlah + ".");
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			laporan.tambahCatatan("Proses sinkronisasi terhenti total (di luar per-diskon): "
					+ ais.common.LaporanUpload.detailTeknisException(e));
		} finally {
			hideProgress();
			laporan.selesaikan(null);
		}
	}

	private void refreshDashboard() {
		if (dashboardHtml == null) {
			return;
		}
		try {
			long total = countDashboard(null);
			long aktif = countDashboard(Boolean.TRUE);
			long nonAktif = total - aktif;
			long item = 0L;
			long penerima = 0L;

			try {
				Session session = HibernateUtil.currentSession();
				Object data = session.createCriteria(DiskonSiswaItemBiaya.class).setProjection(Projections.rowCount())
						.uniqueResult();
				item = data instanceof Number ? ((Number) data).longValue() : 0L;
				data = session.createCriteria(DiskonSiswaPunyaSiswa.class).setProjection(Projections.rowCount())
						.uniqueResult();
				penerima = data instanceof Number ? ((Number) data).longValue() : 0L;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			String[] cards = new String[] {
					CommonDashboardHtmlHelper.metricCard("Total Diskon",
							Common.numberFormat1.get().format(Long.valueOf(total)),
							"Jumlah aturan diskon yang sesuai filter."),
					CommonDashboardHtmlHelper.metricCard("Diskon Aktif",
							Common.numberFormat1.get().format(Long.valueOf(aktif)),
							"Aturan yang sedang dapat digunakan."),
					CommonDashboardHtmlHelper.metricCard("Tidak Aktif",
							Common.numberFormat1.get().format(Long.valueOf(nonAktif)),
							"Aturan yang disimpan tetapi tidak digunakan."),
					CommonDashboardHtmlHelper.metricCard("Penerima",
							Common.numberFormat1.get().format(Long.valueOf(penerima)),
							"Jumlah siswa/calon siswa penerima diskon.") };
			dashboardHtml.setContent(CommonDashboardHtmlHelper.page("Manajemen Diskon Siswa",
					"Atur diskon, item biaya yang dipotong, masa berlaku, dan penerima diskon dalam satu tempat.",
					CommonDashboardHtmlHelper.cards(cards)
							+ CommonDashboardHtmlHelper.descriptionBlock("Item biaya terkait: "
									+ Common.numberFormat1.get().format(Long.valueOf(item))
									+ " data. Gunakan pencarian untuk mempersempit daftar sebelum sinkronisasi.")));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private long countDashboard(Boolean aktifOnly) {
		try {
			Criteria criteria = initCriteria(false);
			if (Boolean.TRUE.equals(aktifOnly)) {
				criteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			}
			criteria.setProjection(Projections.rowCount());
			Object value = criteria.uniqueResult();
			return value instanceof Number ? ((Number) value).longValue() : 0L;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 0L;
		}
	}

	private void showProgress(int percent, String title, String detail) {
		if (progressHtml == null) {
			return;
		}
		progressHtml.setVisible(true);
		progressHtml.setContent(CommonDashboardHtmlHelper.progressBar(percent, title, detail));
	}

	private void hideProgress() {
		try {
			if (progressHtml != null) {
				progressHtml.setContent("");
				progressHtml.setVisible(false);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/DiskonSiswaAction.java:810");
		}
	}

}
