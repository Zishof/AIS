package ais.action.master.kursus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import ais.ui.util.MyDetail;
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
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.AktifitasKomponenDataProdukKursusHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataUjianBandbox;
import ais.action.master.library.helper.AmbilDataItemBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.Ujian;
import ais.database.model.file.LampiranLain;
import ais.database.model.kursus.KomponenDataProdukKursus;
import ais.database.model.kursus.KomponenProdukKursus;
import ais.database.model.library.Item;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk komponen data produk kursus. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Checkbox searchaktif}, {@code
 * AmbilDataSatuanKerjaBanbox searchparent}, {@code Textbox nama}, {@code Textbox keterangan};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()});
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
public class KomponenDataProdukKursusAction extends GenericAutowireComposer
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

	private AmbilDataSatuanKerjaBanbox searchparent;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KomponenDataProdukKursus komponenDataProdukKursus;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private String komponenProdukKursus;
	protected LampiranLain lainMahasiswa;
	private Row rowFile;
	private AmbilDataItemBanbox buku;
	private Row rowBuku;
	private Row rowUjian;
	private AmbilDataUjianBandbox ujian;
	private MyCheckboxConfig hargaIkutDefault;
	private Row rowHarga;
	private MyDoublebox harga;
	private MyIntbox jumlahPertemuan;
	private MyDatebox mulai;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Timebox durasi;
	private AmbilDataPegawaiBanbox tutor1;
	private AmbilDataPegawaiBanbox tutor2;
	private AmbilDataPegawaiBanbox tutor3;
	private AmbilDataPegawaiBanbox tutor4;
	private AmbilDataPegawaiBanbox tutor5;

	public KomponenDataProdukKursusAction(String komponenProdukKursus) {
		this.komponenProdukKursus = komponenProdukKursus;
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

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		String[] contents = new String[] { "id", "kode", "nama", "komponenProdukKursus", "keterangan", "buku", "ujian",
				"hargaIkutDefault", "harga", "mulai", "jumlahPertemuan", "statusPertemuan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KomponenDataProdukKursus.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KomponenDataProdukKursus.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	private static AktifitasKomponenDataProdukKursusHelper aktifitasKomponenDataProdukKursusHelper = new AktifitasKomponenDataProdukKursusHelper();

	/**
	 * Renderer lokal untuk layar/komponen {@link KomponenDataProdukKursusAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KomponenDataProdukKursusAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KomponenDataProdukKursusAction
	 */
	class KomponenDataProdukKursusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KomponenDataProdukKursus komponenDataProdukKursus = (KomponenDataProdukKursus) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			if (komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_TATAP_MUKA)
					|| komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_JARAK_JAUH)
					|| komponenProdukKursus.equals(KomponenProdukKursus.EKSTRA_KURIKULER)) {

				EventListener detailEventListener = new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.clear(detail);
						if (detail.isOpen()) {
							aktifitasKomponenDataProdukKursusHelper.initDetail(komponenDataProdukKursus, detail,
									!komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_JARAK_JAUH));
						}
					}
				};
				detail.addEventListener("onOpen", detailEventListener);
			} else {
				detail.setOpen(true);
				Hbox hbox = new Hbox();
				hbox.setParent(detail);
				LampiranLain.createDownloadUploadFileLain(hbox, komponenDataProdukKursus.getId(),
						KomponenDataProdukKursus.class.getName(), komponenProdukKursus, false, null, null, false, false,
						false, false);
			}

			new Label(komponenDataProdukKursus.getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(KomponenDataProdukKursus.class, komponenDataProdukKursus,
					komponenDataProdukKursus.getNama()).setParent(arg0);

			new Label(komponenDataProdukKursus.getHargaIkutDefault() ? "Default"
					: Common.numberFormat.get().format(komponenDataProdukKursus.getHarga())).setParent(arg0);

			new Label(Common.numberFormat.get().format(komponenDataProdukKursus.getJumlahPertemuan())).setParent(arg0);
			new Label(Common.dateFormat1.get().format(komponenDataProdukKursus.getMulai())).setParent(arg0);

			new Label(komponenDataProdukKursus.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(komponenDataProdukKursus.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenDataProdukKursus.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(komponenDataProdukKursus);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, komponenDataProdukKursus, KomponenDataProdukKursusAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KomponenDataProdukKursus());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		komponenDataProdukKursus = (KomponenDataProdukKursus) obj;
		init(komponenDataProdukKursus);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KomponenDataProdukKursus komponenDataProdukKursus) throws Exception {
		this.komponenDataProdukKursus = komponenDataProdukKursus;
		addWindow.setTitle("Pendataan Komponen Produk \"" + komponenProdukKursus + "\"");
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

		rowBuku = new MyFormRow();
		rowBuku.setVisible(komponenProdukKursus.equals(KomponenProdukKursus.EBOOK)
				|| komponenProdukKursus.equals(KomponenProdukKursus.BUKU));

		rowUjian = new MyFormRow();
		rowUjian.setVisible(komponenProdukKursus.equals(KomponenProdukKursus.UJIAN)
				|| komponenProdukKursus.equals(KomponenProdukKursus.LATIHAN_SOAL));

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setVisible(!rowBuku.isVisible() && !rowUjian.isVisible());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode " + komponenProdukKursus));
		row.appendChild(kode = new Textbox(komponenDataProdukKursus.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(!rowBuku.isVisible() && !rowUjian.isVisible());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama " + komponenProdukKursus + " *"));
		row.appendChild(nama = new Textbox(komponenDataProdukKursus.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_TATAP_MUKA)
				|| komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_JARAK_JAUH)
				|| komponenProdukKursus.equals(KomponenProdukKursus.EKSTRA_KURIKULER));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah pertemuan *"));
		row.appendChild(jumlahPertemuan = new MyIntbox(komponenDataProdukKursus.getJumlahPertemuan()));

		row = new MyFormRow();
		row.setVisible(komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_TATAP_MUKA)
				|| komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_JARAK_JAUH)
				|| komponenProdukKursus.equals(KomponenProdukKursus.EKSTRA_KURIKULER));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai Pertemuan *"));
		row.appendChild(mulai = new MyDatebox(komponenDataProdukKursus.getMulai()));

		row = new MyFormRow();
		row.setVisible(komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_TATAP_MUKA)
				|| komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_JARAK_JAUH)
				|| komponenProdukKursus.equals(KomponenProdukKursus.EKSTRA_KURIKULER));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tutor I"));
		row.appendChild(tutor1 = new AmbilDataPegawaiBanbox(false));
		tutor1.setValue(
				komponenDataProdukKursus.getTutor1() == null ? "" : komponenDataProdukKursus.getTutor1().getNama());
		tutor1.setAttribute("pegawai", komponenDataProdukKursus.getTutor1());
		tutor1.setAttribute("myValue", komponenDataProdukKursus.getTutor1());
		tutor1.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_TATAP_MUKA)
				|| komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_JARAK_JAUH)
				|| komponenProdukKursus.equals(KomponenProdukKursus.EKSTRA_KURIKULER));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tutor II"));
		row.appendChild(tutor2 = new AmbilDataPegawaiBanbox(false));
		tutor2.setValue(
				komponenDataProdukKursus.getTutor2() == null ? "" : komponenDataProdukKursus.getTutor2().getNama());
		tutor2.setAttribute("pegawai", komponenDataProdukKursus.getTutor2());
		tutor2.setAttribute("myValue", komponenDataProdukKursus.getTutor2());
		tutor2.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_TATAP_MUKA)
				|| komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_JARAK_JAUH)
				|| komponenProdukKursus.equals(KomponenProdukKursus.EKSTRA_KURIKULER));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tutor III"));
		row.appendChild(tutor3 = new AmbilDataPegawaiBanbox(false));
		tutor3.setValue(
				komponenDataProdukKursus.getTutor3() == null ? "" : komponenDataProdukKursus.getTutor3().getNama());
		tutor3.setAttribute("pegawai", komponenDataProdukKursus.getTutor3());
		tutor3.setAttribute("myValue", komponenDataProdukKursus.getTutor3());
		tutor3.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_TATAP_MUKA)
				|| komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_JARAK_JAUH)
				|| komponenProdukKursus.equals(KomponenProdukKursus.EKSTRA_KURIKULER));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tutor IV"));
		row.appendChild(tutor4 = new AmbilDataPegawaiBanbox(false));
		tutor4.setValue(
				komponenDataProdukKursus.getTutor4() == null ? "" : komponenDataProdukKursus.getTutor4().getNama());
		tutor4.setAttribute("pegawai", komponenDataProdukKursus.getTutor4());
		tutor4.setAttribute("myValue", komponenDataProdukKursus.getTutor4());
		tutor4.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_TATAP_MUKA)
				|| komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_JARAK_JAUH)
				|| komponenProdukKursus.equals(KomponenProdukKursus.EKSTRA_KURIKULER));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tutor V"));
		row.appendChild(tutor5 = new AmbilDataPegawaiBanbox(false));
		tutor5.setValue(
				komponenDataProdukKursus.getTutor5() == null ? "" : komponenDataProdukKursus.getTutor5().getNama());
		tutor5.setAttribute("pegawai", komponenDataProdukKursus.getTutor5());
		tutor5.setAttribute("myValue", komponenDataProdukKursus.getTutor5());
		tutor5.setReadonly(true);

		rowBuku.setParent(rows);
		rowBuku.appendChild(new ais.ui.util.MyLabelConfig(komponenProdukKursus + " *"));
		rowBuku.appendChild(buku = new AmbilDataItemBanbox());
		buku.setAttribute("item", komponenDataProdukKursus.getBuku());
		buku.setValue(komponenDataProdukKursus.getBuku() == null ? "" : komponenDataProdukKursus.getBuku().getNama());
		buku.setWidth("90%");

		rowUjian.setParent(rows);
		rowUjian.appendChild(new ais.ui.util.MyLabelConfig(komponenProdukKursus + " *"));
		rowUjian.appendChild(ujian = new AmbilDataUjianBandbox());
		ujian.setAttribute("ujian", komponenDataProdukKursus.getUjian());
		ujian.setValue(
				komponenDataProdukKursus.getUjian() == null ? "" : komponenDataProdukKursus.getUjian().getNama());
		ujian.setWidth("90%");

		hargaIkutDefault = new MyCheckboxConfig("Ya");
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Harga ikut default"));
		row.appendChild(hargaIkutDefault);
		hargaIkutDefault.setChecked(komponenDataProdukKursus.getHargaIkutDefault());

		rowHarga = new MyFormRow();
		rowHarga.setParent(rows);
		rowHarga.appendChild(new ais.ui.util.MyLabelConfig("Harga"));
		harga = new MyDoublebox(komponenDataProdukKursus.getHarga());
		rowHarga.appendChild(harga);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				rowHarga.setVisible(!hargaIkutDefault.isChecked());
			}

		};

		hargaIkutDefault.addEventListener("onClick", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(komponenDataProdukKursus.getSatuanKerja() == null ? ""
				: komponenDataProdukKursus.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", komponenDataProdukKursus.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(komponenDataProdukKursus.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		lainMahasiswa = null;
		rowFile = new MyFormRow();
		rowFile.setVisible(komponenProdukKursus.equals(KomponenProdukKursus.EBOOK)
				|| komponenProdukKursus.equals(KomponenProdukKursus.VIDEO));
		rowFile.setParent(rows);
		rowFile.appendChild(new ais.ui.util.MyLabelConfig("File / Link *"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, komponenDataProdukKursus.getId(),
				KomponenDataProdukKursus.class.getName(), komponenProdukKursus, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(rowFile);

		row = new MyFormRow();
		row.setVisible(komponenProdukKursus.equals(KomponenProdukKursus.VIDEO));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Durasi Video"));
		row.appendChild(durasi = new ais.ui.util.MyTimebox(komponenDataProdukKursus.getDurasi()));

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

		if (rowBuku.isVisible()) {
			if (buku.getAttribute("item") == null) {
				MyMessageboxConfig.show(komponenDataProdukKursus + " harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
		} else if (rowUjian.isVisible()) {
			if (ujian.getAttribute("ujian") == null) {
				MyMessageboxConfig.show(komponenDataProdukKursus + " harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
		} else {
			if (nama.getValue().trim().equals("")) {
				MyMessageboxConfig.show("Nama harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		if (komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_TATAP_MUKA)
				|| komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_JARAK_JAUH)
				|| komponenProdukKursus.equals(KomponenProdukKursus.EKSTRA_KURIKULER)) {
			if (jumlahPertemuan.getValue() == null) {
				MyMessageboxConfig.show("Jumlah pertemuan harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
			if (mulai.getValue() == null) {
				MyMessageboxConfig.show("Tanggal mulai pertemuan harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		try {

			if (rowFile.isVisible()) {
				if (komponenDataProdukKursus != null && komponenDataProdukKursus.getId() != null) {
					Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

					int jumlah = ((Number) streamingSession.createCriteria(LampiranLain.class)
							.setProjection(Projections.rowCount())
							.add(Restrictions.eq("ref", komponenDataProdukKursus.getId()))
							.add(Restrictions.eq("jenis", KomponenDataProdukKursus.class.getName())).uniqueResult())
							.intValue();

					StreamingHibernateUtil.getInstance().closeSession();

					if (jumlah == 0) {
						MyMessageboxConfig.show("File / link " + komponenProdukKursus + " harus di-upload",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return false;
					}
				} else {
					if (lainMahasiswa == null) {
						MyMessageboxConfig.show("File / link " + komponenProdukKursus + " harus di-upload",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return false;
					}
				}
			}
		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/kursus/KomponenDataProdukKursusAction.java:572");
		}

		Session session = HibernateUtil.currentSession();
		if (komponenDataProdukKursus.getId() != null) {
			komponenDataProdukKursus = (KomponenDataProdukKursus) session.load(KomponenDataProdukKursus.class,
					komponenDataProdukKursus.getId());

		}
		komponenDataProdukKursus.setJumlahPertemuan(jumlahPertemuan.getValue());
		komponenDataProdukKursus.setMulai(mulai.getValue());
		komponenDataProdukKursus.setBuku((Item) buku.getAttribute("item"));
		komponenDataProdukKursus.setUjian((Ujian) ujian.getAttribute("ujian"));
		komponenDataProdukKursus.setKode(kode.getValue());
		komponenDataProdukKursus.setNama(nama.getValue());
		komponenDataProdukKursus.setKeterangan(keterangan.getValue());
		komponenDataProdukKursus.setKomponenProdukKursus(komponenProdukKursus);
		komponenDataProdukKursus.setHarga(harga.getValue());
		komponenDataProdukKursus.setHargaIkutDefault(hargaIkutDefault.isChecked());
		komponenDataProdukKursus.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		komponenDataProdukKursus.setDurasi(durasi.getValue());
		komponenDataProdukKursus.setTutor1((Pegawai) tutor1.getAttribute("pegawai"));
		komponenDataProdukKursus.setTutor2((Pegawai) tutor2.getAttribute("pegawai"));
		komponenDataProdukKursus.setTutor3((Pegawai) tutor3.getAttribute("pegawai"));
		komponenDataProdukKursus.setTutor4((Pegawai) tutor4.getAttribute("pegawai"));
		komponenDataProdukKursus.setTutor5((Pegawai) tutor5.getAttribute("pegawai"));

		if (komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_TATAP_MUKA)
				|| komponenProdukKursus.equals(KomponenProdukKursus.EKSTRA_KURIKULER)) {
			komponenDataProdukKursus.setStatusPertemuan(ConstantValues.TATAP_MUKA);
		} else if (komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_JARAK_JAUH)) {
			komponenDataProdukKursus.setStatusPertemuan(ConstantValues.DARING);
		} else {
			komponenDataProdukKursus.setStatusPertemuan(null);
		}

		Common.refreshSaveOrUpdate(session, komponenDataProdukKursus);

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(komponenDataProdukKursus.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KomponenDataProdukKursus.class)

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas))))

				.add(Restrictions.eq("komponenProdukKursus", komponenProdukKursus))
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KomponenDataProdukKursus> komponenDataProdukKursus = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(komponenDataProdukKursus);
		grid.setRowRenderer(new KomponenDataProdukKursusRenderer());
		grid.setModelCheckMobile(strset);

	}

}
