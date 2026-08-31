package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.FormatTemplateSuratDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.FormatTemplateSurat;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.TemplateSurat;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyPanel;

/**
 * Helper ZK untuk mengelola daftar {@link FormatTemplateSurat} (variasi format/gaya cetak) milik
 * satu {@link TemplateSurat} (template dokumen/surat administrasi, mis. surat keterangan/
 * transkrip). Tiap format menentukan kombinasi jenis kegiatan pembayaran, item biaya, bahasa
 * (Indonesia/Inggris, dari {@link Common#locale}/{@link Common#localeEn}), dan biaya cetak.
 * Setiap baris grid dapat diperluas ({@link ais.ui.util.MyDetail}) untuk mengelola berkas JRXML
 * (template Jasper Report sesungguhnya) lewat {@link SuratJrxmlFileHelper}.
 *
 * <p>
 * Hak tambah/ubah/hapus mengikuti privilese {@link CommonPrivilages#CREATE}/{@code UPDATE}/
 * {@code DELETE} milik user login, dan seluruh aksi perubahan disembunyikan untuk user mahasiswa.
 * Penyimpanan dilakukan lewat {@link FormatTemplateSuratDao} (bukan langsung Hibernate Session).
 * </p>
 */
public class FormatTemplateSuratHelper implements DataLoader {

	private MyGrid grid;
	private TemplateSurat templateSurat;
	private boolean delete = false;
	private boolean add = false;
	private FormatTemplateSurat formatTemplateSurat;
	private MyWindow addWindow;
	private Textbox nama;
	private Textbox keterangan;
	private Combobox jenisBiaya;
	private Combobox itemBiaya;
	private Combobox bahasa;
	private boolean edit = false;
	private MyDoublebox biaya;

	/** Menentukan hak akses (hapus/ubah/tambah) dari privilese user login, dan menyiapkan combobox referensi (jenis biaya, item biaya, bahasa) yang dipakai ulang oleh dialog tambah/ubah. */
	public FormatTemplateSuratHelper() {
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);

		Common.insertCombo(jenisBiaya = new Combobox(), "namaKegiatan", JenisKegiatan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(itemBiaya = new Combobox(), "nama", ItemBiaya.class);

		bahasa = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Common.locale.getLanguage());
		comboitem.setValue(Common.locale.getLanguage());
		bahasa.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Common.localeEn.getLanguage());
		comboitem.setValue(Common.localeEn.getLanguage());
		bahasa.appendChild(comboitem);

	}

	/** Renderer baris format: bagian dapat-dibuka berisi pengelola berkas JRXML ({@link SuratJrxmlFileHelper}), diikuti nama (dengan riwayat revisi), jenis kegiatan, item biaya, bahasa, biaya cetak, keterangan, dan tombol edit/hapus (tersembunyi untuk mahasiswa/tanpa privilese). */
	class DetailTemplateSuratRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final FormatTemplateSurat formatTemplateSurat = (FormatTemplateSurat) data;

			final MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						SuratJrxmlFileHelper suratJrxmlFileHelper = new SuratJrxmlFileHelper(
								Common.getCurrentUser().getMahasiswa() == null && delete);

						suratJrxmlFileHelper.display(formatTemplateSurat, detail);
					}

				}
			});

			RevisiHelper.createNewRevisi(FormatTemplateSurat.class, formatTemplateSurat, formatTemplateSurat.getNama())
					.setParent(row);
			new Label(formatTemplateSurat.getJenisKegiatan() == null ? ""
					: formatTemplateSurat.getJenisKegiatan().getNamaKegiatan()).setParent(row);
			new Label(formatTemplateSurat.getItemBiaya() == null ? "" : formatTemplateSurat.getItemBiaya().getNama())
					.setParent(row);

			new Label(formatTemplateSurat.getBahasa()).setParent(row);
			new Label(formatTemplateSurat.getBiaya() == null ? ""
					: Common.numberFormat.get().format(formatTemplateSurat.getBiaya())).setParent(row);
			new Label(formatTemplateSurat.getKeterangan()).setParent(row);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setVisible(Common.getCurrentUser().getMahasiswa() == null && edit);
			button.setTooltiptext("Edit Data");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					init(formatTemplateSurat);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(Common.getCurrentUser().getMahasiswa() == null && delete);
			button.setTooltiptext("Hapus Data");
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
											FormatTemplateSuratDao templateSuratDao = DaoFactory.getInstance()
													.getFormatTemplateSuratDao();

											templateSuratDao.delete((formatTemplateSurat));

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}

	}

	/** Implementasi {@link DataLoader#loadData}: memuat ulang seluruh {@link FormatTemplateSurat} milik {@link #templateSurat} yang sedang ditampilkan. */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<FormatTemplateSurat> formatTemplateSurat = session.createCriteria(FormatTemplateSurat.class)
				.addOrder(Order.asc("id")).add(Restrictions.eq("templateSurat", templateSurat)).list();

		ListModel strset = new SimpleListModel(formatTemplateSurat);
		grid.setRowRenderer(new DetailTemplateSuratRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Titik masuk utama: membangun panel daftar format template surat untuk {@code templateSurat}
	 * — toolbar "Tambah File JRXML" (hanya tampil bila punya hak tambah dan bukan mahasiswa)
	 * diikuti grid berpaging.
	 *
	 * @param templateSurat template surat induk
	 * @param component     komponen induk (dibersihkan lebih dulu)
	 */
	public void display(final TemplateSurat templateSurat, final Component component) {
		this.templateSurat = templateSurat;
		Common.clear(component);

		MyPanel panel = new MyPanel();
		panel.setParent(component);
		panel.setWidth("100%");
		panel.setHeight("450px");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setVisible(Common.getCurrentUser().getMahasiswa() == null && add);
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah File JRXML", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onAdd(event);
			}

		});
		button.setParent(toolbar);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Pembayaran");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Item Biaya");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Bahasa");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Biaya");
		column.setAlign("right");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);

	}

	/** Membuka dialog tambah {@link FormatTemplateSurat} baru. */
	public void onAdd(Event event) throws Exception {
		init(new FormatTemplateSurat());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/** Membangun dialog modal tambah/ubah satu {@link FormatTemplateSurat} (nama, jenis biaya, item biaya, bahasa, biaya cetak, keterangan). */
	private void init(FormatTemplateSurat formatTemplateSurat) {
		addWindow = new MyWindow();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
		addWindow.setWidth("500px");
		addWindow.setHeight("400px");
		this.formatTemplateSurat = formatTemplateSurat;
		addWindow.setTitle(formatTemplateSurat.getId() == null ? "Tambah Format Template Surat" : "Ubah Format Template Surat");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Format Template Surat"));
		row.appendChild(nama = new Textbox(formatTemplateSurat.getNama() == null ? "" : formatTemplateSurat.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Biaya"));
		Common.selectComboItem(jenisBiaya, formatTemplateSurat.getJenisKegiatan());
		row.appendChild(jenisBiaya);
		jenisBiaya.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Item Biaya"));
		Common.selectComboItem(itemBiaya, formatTemplateSurat.getItemBiaya());
		row.appendChild(itemBiaya);
		itemBiaya.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bahasa"));
		Common.selectComboItem(bahasa, formatTemplateSurat.getBahasa());
		row.appendChild(bahasa);
		bahasa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Biaya Cetak"));
		row.appendChild(biaya = new MyDoublebox(formatTemplateSurat.getBiaya()));
		biaya.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				formatTemplateSurat.getKeterangan() == null ? "" : formatTemplateSurat.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(4);

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
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					loadData(null);
					addWindow.detach();
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	/**
	 * Memvalidasi (nama, jenis biaya, item biaya wajib diisi) dan menyimpan {@link
	 * FormatTemplateSurat} lewat {@link FormatTemplateSuratDao}.
	 *
	 * @return {@code true} bila berhasil disimpan; {@code false} bila validasi gagal (pesan sudah ditampilkan)
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, nama format template surat belum diisi. Langkah yang dapat dilakukan: (1) isi nama format template surat pada kolom yang tersedia; (2) pastikan nama tidak kosong; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisBiaya.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, jenis biaya belum dipilih. Langkah yang dapat dilakukan: (1) pilih jenis biaya dari daftar yang tersedia; (2) pastikan data jenis biaya sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (itemBiaya.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, item pembayaran belum dipilih. Langkah yang dapat dilakukan: (1) pilih item pembayaran dari daftar yang tersedia; (2) pastikan data item pembayaran tersedia; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		FormatTemplateSuratDao formatTemplateSuratDao = DaoFactory.getInstance().getFormatTemplateSuratDao();
		if (formatTemplateSurat.getId() != null) {
			formatTemplateSurat = formatTemplateSuratDao.load(formatTemplateSurat.getId());

		}

		formatTemplateSurat
				.setBahasa((String) (bahasa.getSelectedItem() == null ? null : bahasa.getSelectedItem().getValue()));
		formatTemplateSurat.setJenisKegiatan((JenisKegiatan) (jenisBiaya.getSelectedItem() == null ? null
				: jenisBiaya.getSelectedItem().getValue()));
		formatTemplateSurat.setItemBiaya(
				(ItemBiaya) (itemBiaya.getSelectedItem() == null ? null : itemBiaya.getSelectedItem().getValue()));

		formatTemplateSurat.setBiaya(biaya.getValue());
		formatTemplateSurat.setNama(nama.getValue());
		formatTemplateSurat.setKeterangan(keterangan.getValue());
		formatTemplateSurat.setTemplateSurat(templateSurat);

		if (formatTemplateSurat.getId() != null) {
			formatTemplateSuratDao.update(formatTemplateSurat);
		} else {
			formatTemplateSuratDao.save(formatTemplateSurat);
		}

		return true;
	}

}
