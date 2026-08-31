package ais.action.master.kursus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.kursus.helper.AmbilDataPesertaKursusBanbox;
import ais.action.master.kursus.helper.KursusUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.file.LampiranLain;
import ais.database.model.kursus.KategoriProdukKursus;
import ais.database.model.kursus.KomponenProdukKursus;
import ais.database.model.kursus.PesertaKursus;
import ais.database.model.kursus.ProdukKursus;
import ais.database.model.kursus.TingkatKelasProdukKursus;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.MyLabelKecil;

/**
 * Controller/action ZK untuk produk kursus. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
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
public class ProdukKursusAction extends GenericAutowireComposer
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

	private ProdukKursus produkKursus;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private JSONArray hargaKomponens;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Combobox tingkatKelasProdukKursus;
	private Combobox kategoriProdukKursus;
	private AmbilDataPesertaKursusBanbox instrukturBanbox;
	private Combobox status;
	private Checkbox gratis;
	protected LampiranLain lainMahasiswa;

	private MyCkEditor deskripsi;

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

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "aktif", "satuanKerja",
				"tingkatKelasProdukKursus", "kategoriProdukKursus" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(ProdukKursus.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ProdukKursus.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class ProdukKursusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ProdukKursus produkKursus = (ProdukKursus) arg1;
			new Label(produkKursus.getKode()).setParent(arg0);
			Vbox a;
			(a = RevisiHelper.createNewRevisi(ProdukKursus.class, produkKursus, produkKursus.getNama()))
					.setParent(arg0);

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, produkKursus.getId(), "Gambar Produk Kursus", "Gambar",
					false, null, null, false, false, false, false);

			myvbox = new Vbox();
			myvbox.setParent(arg0);

			new Label(produkKursus.getTingkatKelasProdukKursus() == null ? ""
					: produkKursus.getTingkatKelasProdukKursus().getNama()).setParent(myvbox);

			new Label(produkKursus.getKategoriProdukKursus() == null ? ""
					: produkKursus.getKategoriProdukKursus().getNama()).setParent(myvbox);

			new MyLabelKecil("Instruktur: "
					+ (produkKursus.getInstruktur() == null ? "-" : produkKursus.getInstruktur().getNama()))
					.setParent(myvbox);

			final Label statusLabel = new MyLabelKecil("Status: " + produkKursus.getStatus());
			statusLabel.setParent(myvbox);

			if (edit && ProdukKursus.PENDING_REVIEW.equals(produkKursus.getStatus())) {
				Hbox approveHbox = new Hbox();
				approveHbox.setParent(myvbox);

				MyToolbarbuttonConfig approve = new MyToolbarbuttonConfig("Approve", "/img/ok.gif");
				approve.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						produkKursus.setStatus(ProdukKursus.PUBLISHED);
						Common.refreshSaveOrUpdate(produkKursus);
						onSearchDefault(null);
					}
				});
				approve.setParent(approveHbox);

				MyToolbarbuttonConfig reject = new MyToolbarbuttonConfig("Reject", "/img/cancel.gif");
				reject.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						produkKursus.setStatus(ProdukKursus.REJECTED);
						Common.refreshSaveOrUpdate(produkKursus);
						onSearchDefault(null);
					}
				});
				reject.setParent(approveHbox);
			}

			Grid grid = new Grid();grid.setSclass("dgrid");
			grid.setWidth("100%");
			grid.setParent(arg0);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig("Komponen");
			column.setParent(columns);

			column = new MyColumnConfig("Qty");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("10%");

			column = new MyColumnConfig("Harga");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("20%");

			column = new MyColumnConfig("Total");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("20%");

			Rows rows = new Rows();
			rows.setParent(grid);
			JSONArray array = new JSONArray(produkKursus.getHargaKomponens());
			Double hargaTotal = 0.0;
			for (int i = 0; i < array.length(); i++) {

				JSONObject jsonObject = array.getJSONObject(i);

				Long key = null;
				if (!jsonObject.isNull("key")) {
					key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
				}

				if (key != null) {

					KomponenProdukKursus komponenProdukKursusBiaya = (KomponenProdukKursus) (jsonObject
							.isNull("komponenProdukKursus") ? null
									: ConstantValues.ambil(KomponenProdukKursus.class.getName(),
											ais.common.CommonJSONUtil.ambilLong(jsonObject,"komponenProdukKursus")));
					Double hrg = jsonObject.isNull("harga") ? 0.0 : jsonObject.getDouble("harga");
					Double qty = jsonObject.isNull("qty") ? 1.0 : jsonObject.getDouble("qty");

					hargaTotal += (qty * hrg);

					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setParent(rows);

					row.appendChild(new MyLabelAgakKecil(
							komponenProdukKursusBiaya == null ? "" : komponenProdukKursusBiaya.getNama()));
					row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(qty)));
					row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(hrg)));
					row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(qty * hrg)));
				}
			}
			Foot foot = new Foot();
			foot.setParent(grid);

			Footer footer = new Footer("Total");
			foot.appendChild(footer);

			footer = new Footer("");
			foot.appendChild(footer);

			footer = new Footer("");
			foot.appendChild(footer);

			if (hargaTotal.intValue() != produkKursus.getHargaTotal().intValue()) {
				produkKursus.setHargaTotal(hargaTotal);
				Common.refreshSaveOrUpdate(produkKursus);
			}

			Footer footerTotal = new Footer(Common.numberFormat.get().format(produkKursus.getHargaTotal()));
			foot.appendChild(footerTotal);

			new MyLabelKecil(produkKursus.getKeterangan()).setParent(arg0);

			Vbox checkboxVbox = new Vbox();
			checkboxVbox.setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(produkKursus.getAktif());
			checkbox.setParent(checkboxVbox);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					produkKursus.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(produkKursus);
				}
			});

			final MyCheckboxConfig checkboxGratis = new MyCheckboxConfig("Gratis");
			checkboxGratis.setDisabled(!edit);
			checkboxGratis.setChecked(produkKursus.getGratis());
			checkboxGratis.setParent(checkboxVbox);
			checkboxGratis.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					produkKursus.setGratis(checkboxGratis.isChecked());
					Common.refreshSaveOrUpdate(produkKursus);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, produkKursus, ProdukKursusAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new ProdukKursus());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		produkKursus = (ProdukKursus) obj;
		init(produkKursus);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(ProdukKursus produkKursus) throws Exception {
		this.produkKursus = produkKursus;
		addWindow.setTitle(produkKursus.getId() == null ? "Tambah Produk Kursus" : "Ubah Produk Kursus");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabSoal = new MyTabConfig("Data Produk");
		tabSoal.setParent(tabs);

		MyTabConfig tabJawaban = new MyTabConfig("Deskripsi");
		tabJawaban.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(tabpanelUtama);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Produk *"));
		row.appendChild(kode = new Textbox(produkKursus.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Produk *"));
		row.appendChild(nama = new Textbox(produkKursus.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penjelasan Ringkas"));
		row.appendChild(keterangan = new Textbox(produkKursus.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tingkat *"));
		row.appendChild(tingkatKelasProdukKursus = new Combobox());
		tingkatKelasProdukKursus.setWidth("90%");
		tingkatKelasProdukKursus.setReadonly(true);
		Common.insertCombo(tingkatKelasProdukKursus, "nama", "keterangan", TingkatKelasProdukKursus.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(tingkatKelasProdukKursus, produkKursus.getTingkatKelasProdukKursus());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kategori *"));
		row.appendChild(kategoriProdukKursus = new Combobox());
		kategoriProdukKursus.setWidth("90%");
		kategoriProdukKursus.setReadonly(true);
		Common.insertCombo(kategoriProdukKursus, "nama", "keterangan", KategoriProdukKursus.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(kategoriProdukKursus, produkKursus.getKategoriProdukKursus());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(produkKursus.getSatuanKerja() == null ? "" : produkKursus.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", produkKursus.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Instruktur"));
		instrukturBanbox = new AmbilDataPesertaKursusBanbox();
		instrukturBanbox.setValue(produkKursus.getInstruktur() == null ? "" : produkKursus.getInstruktur().getNama());
		instrukturBanbox.setAttribute("pesertaKursus", produkKursus.getInstruktur());
		instrukturBanbox.setAttribute("myValue", produkKursus.getInstruktur());
		row.appendChild(instrukturBanbox);
		instrukturBanbox.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(status = new Combobox());
		status.setWidth("90%");
		status.setReadonly(true);
		for (String s : new String[] { ProdukKursus.DRAFT, ProdukKursus.PENDING_REVIEW, ProdukKursus.PUBLISHED,
				ProdukKursus.REJECTED }) {
			Comboitem item = status.appendItem(s);
			item.setValue(s);
			if (s.equals(produkKursus.getStatus())) {
				status.setSelectedItem(item);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gratis"));
		row.appendChild(gratis = new Checkbox());
		gratis.setChecked(produkKursus.getGratis());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Komponen Produk"));
		ais.ui.util.ZkCompat.setSpans(row, "2");

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		hargaKomponens = new JSONArray(produkKursus.getHargaKomponens());
		Row rowFormula = Common.tampilanScroll1(row);
		KursusUtil.reloadFormula(rowFormula, hargaKomponens);

		lainMahasiswa = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gambar / Icon"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, produkKursus.getId(), "Gambar Produk Kursus", "Gambar", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);
		tabpanelUtama.appendChild(deskripsi = new MyCkEditor());
		deskripsi.setValue(produkKursus.getDeskripsi());
		deskripsi.setHeight("100%");
		deskripsi.setWidth("100%");

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
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Produk harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Produk harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (tingkatKelasProdukKursus.getSelectedItem() == null
				|| tingkatKelasProdukKursus.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Tingkat Produk harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (kategoriProdukKursus.getSelectedItem() == null
				|| kategoriProdukKursus.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Kategori Produk harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Double d = 0.0;
		for (int i = 0; i < hargaKomponens.length(); i++) {
			JSONObject jsonObject = hargaKomponens.getJSONObject(i);
			Long key = null;
			if (!jsonObject.isNull("key")) {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
			}

			if (key != null) {
				Double hrg = jsonObject.isNull("harga") ? 0.0 : jsonObject.getDouble("harga");
				Double qty = jsonObject.isNull("qty") ? 1.0 : jsonObject.getDouble("qty");
				d += (hrg * qty);
			}
		}
		if (d.intValue() == 0) {
			MyMessageboxConfig.show("Nilai total komponen produk tidak boleh kosong", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (produkKursus.getId() != null) {
			produkKursus = (ProdukKursus) session.load(ProdukKursus.class, produkKursus.getId());

		}

		produkKursus.setKode(kode.getValue());
		produkKursus.setNama(nama.getValue());
		produkKursus.setKeterangan(keterangan.getValue());
		produkKursus.setHargaKomponens(hargaKomponens.toString());
		produkKursus.setHargaTotal(d);
		produkKursus.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		produkKursus.setTingkatKelasProdukKursus(
				(TingkatKelasProdukKursus) tingkatKelasProdukKursus.getSelectedItem().getValue());

		produkKursus.setKategoriProdukKursus((KategoriProdukKursus) kategoriProdukKursus.getSelectedItem().getValue());
		produkKursus.setDeskripsi(deskripsi.getValue());
		produkKursus.setInstruktur((PesertaKursus) instrukturBanbox.getAttribute("pesertaKursus"));
		produkKursus.setStatus(status.getSelectedItem() == null ? ProdukKursus.DRAFT
				: (String) status.getSelectedItem().getValue());
		produkKursus.setGratis(gratis.isChecked());
		Common.refreshSaveOrUpdate(session, produkKursus);

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(produkKursus.getId());

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
		Criteria criteria = session.createCriteria(ProdukKursus.class)

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas))))

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

		List<ProdukKursus> produkKursus = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(produkKursus);
		grid.setRowRenderer(new ProdukKursusRenderer());
		grid.setModelCheckMobile(strset);

	}

}
