package ais.action.master.surat.helper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.OnSaveListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.file.LampiranLain;
import ais.database.model.surat.KlasifikasiSuratKeluar;
import ais.database.model.surat.KlasifikasiSuratKeluarParemeter;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper UI (implementasi {@link DataCriteria}/{@link DataSearchDefault}) untuk kelola daftar
 * parameter mail-merge ({@link KlasifikasiSuratKeluarParemeter}) sebuah
 * {@link KlasifikasiSuratKeluar} (template surat keluar): setiap parameter punya nama/key, nomor
 * urut tampil, dan salah satu dari sembilan tipe nilai (String/Integer/Double/Date/COMBO/
 * GAMBAR/TEXT/DATA/DAFTAR_MAHASISWA/DAFTAR_SISWA/DAFTAR_PENGGUNA) yang menentukan editor nilai
 * default yang muncul: textbox biasa, unggah gambar, editor kaya (CKEditor) untuk TEXT, atau
 * editor tabel 100 baris x 15 kolom (nilai disimpan terserialisasi dengan pemisah
 * {@code "||"} antarbaris dan {@code "<->"} antarkolom) untuk DATA. Bila
 * {@code klasifikasiSuratKeluar.getCopyDari()} terisi, daftar parameter yang ditampilkan/dibaca
 * diambil dari template sumber salinan tersebut, bukan template ini sendiri. Sebagian besar
 * perubahan kolom pada baris (nomor urut, tampil, nilai, pilihan, tipe) langsung tersimpan ke
 * basis data. Visibilitas tambah/ubah/hapus mengikuti hak akses
 * {@link CommonPrivilages#CREATE}/{@link CommonPrivilages#UPDATE}/{@link CommonPrivilages#DELETE}.
 */
public class KlasifikasiSuratKeluarParameterHelper implements DataCriteria, DataSearchDefault {

	private MyGrid gridParemeter;
	private boolean add = false;
	private boolean delete = false;
	private boolean edit = false;
	private KlasifikasiSuratKeluar klasifikasiSuratKeluar;

	/** @param gridParemeter grid yang akan diisi/dikelola helper ini */
	public KlasifikasiSuratKeluarParameterHelper(MyGrid gridParemeter) {
		this.gridParemeter = gridParemeter;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
	}

	/**
	 * Menampilkan jendela modal isi/edit nama dan keterangan satu parameter. Menyimpan
	 * ({@code key} disamakan dengan {@code nama}) hanya bila
	 * {@code klasifikasiSuratKeluarParemeter.getKlasifikasiSuratKeluar()} sudah punya id; baris
	 * grid yang bersangkutan dibangun ulang lewat {@link #initRow} ({@code rowLama} dipakai
	 * ulang bila mengedit baris yang sudah ada, atau baris baru dibuat untuk parameter baru).
	 *
	 * @param klasifikasiSuratKeluarParemeter parameter yang diedit, atau instans baru untuk parameter baru
	 * @param rowLama                         baris grid yang sedang diedit, atau {@code null} untuk baris baru
	 */
	private void onAdd(final KlasifikasiSuratKeluarParemeter klasifikasiSuratKeluarParemeter, final Row rowLama)
			throws Exception {

		final MyWindow window = new MyWindow("Klasifikasi Surat Keluar", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("240px");
		window.setWidth("550px");

		final Textbox nama = new Textbox(klasifikasiSuratKeluarParemeter.getNama());
		final Textbox keterangan = new Textbox(klasifikasiSuratKeluarParemeter.getKeterangan());

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama / Key Parameter"));
		row.appendChild(nama);
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Label Keterangan"));
		row.appendChild(keterangan);
		keterangan.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("Nilai bisa diubah"));
		// final Checkbox bisaDiubah;
		// row.appendChild(bisaDiubah = new Checkbox());
		// bisaDiubah.setChecked(klasifikasiSuratKeluarParemeter.getBisaDiubah());

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
				window.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (nama.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Mohon maaf, Nama Parameter Klasifikasi Surat Keluar belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Parameter; (2) isikan nama parameter secara lengkap; (3) klik tombol Simpan kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Rows rows = gridParemeter.getRows() == null ? new Rows() : gridParemeter.getRows();
				rows.setParent(gridParemeter);

				klasifikasiSuratKeluarParemeter.setNama(nama.getValue().trim());
				klasifikasiSuratKeluarParemeter.setKey(nama.getValue().trim());
				klasifikasiSuratKeluarParemeter.setKeterangan(keterangan.getValue().trim());
				// klasifikasiSuratKeluarParemeter.setBisaDiubah(bisaDiubah.isChecked());

				if (klasifikasiSuratKeluarParemeter.getKlasifikasiSuratKeluar().getId() != null) {
					Common.refreshSaveOrUpdate(klasifikasiSuratKeluarParemeter);
				}

				Row row = rowLama == null ? new MyFormRow() : rowLama;
				row.setParent(rows);
				Common.clear(row);
				try {
					initRow(row, klasifikasiSuratKeluarParemeter);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				window.detach();
			}
		});
		save.setParent(toolbar);

		window.onModal();
	}

	/**
	 * Menyusun tata letak (toolbar tambah/cetak/unggah/refresh + grid parameter dengan kolom
	 * Parameter/Key/No.Urut/Tampil/Nilai Default/Tipe/Hapus) dan langsung memuat data parameter
	 * yang sudah tersimpan. Tombol tambah lebih dulu memanggil {@code onSaveListener.onSave}
	 * untuk memastikan template surat induk sudah tersimpan.
	 *
	 * @param klasifikasiSuratKeluar template surat keluar yang parameternya dikelola
	 * @param onSaveListener         callback penyimpanan template induk, dipanggil sebelum dialog tambah parameter dibuka
	 * @return komponen tata letak siap pakai untuk ditempelkan ke jendela detail
	 */
	public Borderlayout initDetail(final KlasifikasiSuratKeluar klasifikasiSuratKeluar,
			final OnSaveListener onSaveListener) throws Exception {

		this.klasifikasiSuratKeluar = klasifikasiSuratKeluar;

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Paremeter", "/img/new.gif");
		add.setVisible(KlasifikasiSuratKeluarParameterHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSaveListener.onSave(event)) {
					KlasifikasiSuratKeluarParemeter klasifikasiSuratKeluarParemeter = new KlasifikasiSuratKeluarParemeter();
					klasifikasiSuratKeluarParemeter.setKlasifikasiSuratKeluar(klasifikasiSuratKeluar);
					onAdd(klasifikasiSuratKeluarParemeter, null);
				}
			}
		});

		String[] contents = new String[] { "id", "nama", "key", "nilai", "tipe", "tampil", "nomorUrut", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KlasifikasiSuratKeluarParemeter.class, this,
				contents);
		toolbar.appendChild(cetakToolbarbutton);

		HashMap<String, Object> nilai = null;
		Criterion idCrit = null;
		if (klasifikasiSuratKeluar != null) {
			idCrit = Restrictions.eq("klasifikasiSuratKeluar", klasifikasiSuratKeluar);
			nilai = new HashMap<String, Object>();
			nilai.put("klasifikasiSuratKeluar", klasifikasiSuratKeluar);
		}

		MyToolbarbuttonConfig upload = Common.uploadData(this, KlasifikasiSuratKeluarParemeter.class, null, idCrit,
				nilai, contents);
		upload.setVisible(add.isVisible());
		toolbar.appendChild(upload);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail();
			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridParemeter);
		gridParemeter.setParent(center);
		gridParemeter.setWidth("100%");
		gridParemeter.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridParemeter);

		MyColumnConfig column = new MyColumnConfig("Paremeter");
		column.setParent(columns);

		column = new MyColumnConfig("Key");
		column.setParent(columns);

		column = new MyColumnConfig("No. Urut");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Tampil");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Nilai Default");
		column.setParent(columns);

		column = new MyColumnConfig("Tipe");
		column.setParent(columns);

//		column = new MyColumnConfig("Nilai bisa diubah");
//		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("10%");

		loadDataDetail();

		return borderlayout;
	}

	/** Memuat baris {@link KlasifikasiSuratKeluarParemeter} milik {@link #klasifikasiSuratKeluar} (atau {@code copyDari}-nya bila diisi) ke dalam grid, diurutkan lewat {@link #initCriteria(boolean)}. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail() {

		List<KlasifikasiSuratKeluarParemeter> klasifikasiSuratKeluarParemeters = klasifikasiSuratKeluar == null
				|| klasifikasiSuratKeluar.getId() == null ? new ArrayList<KlasifikasiSuratKeluarParemeter>() :

						ConstantValues.simpleList(initCriteria(true), KlasifikasiSuratKeluarParemeter.class);

		if (klasifikasiSuratKeluar.getCopyDari() != null) {
			klasifikasiSuratKeluarParemeters = ConstantValues.simpleList(initCriteria(true),
					KlasifikasiSuratKeluarParemeter.class);
		}

		Rows rows = gridParemeter.getRows() == null ? new Rows() : gridParemeter.getRows();
		Common.clear(rows);
		rows.setParent(gridParemeter);

		for (KlasifikasiSuratKeluarParemeter klasifikasiSuratKeluarParemeter : klasifikasiSuratKeluarParemeters) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			try {
				initRow(row, klasifikasiSuratKeluarParemeter);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	/**
	 * Mengisi satu baris grid dengan keterangan+key, nomor urut, checkbox tampil, editor nilai
	 * (bentuknya mengikuti tipe parameter — textbox nilai default, unggah gambar, tombol buka
	 * editor kaya untuk TEXT, atau tombol buka editor tabel 100x15 untuk DATA), textbox pilihan
	 * (untuk tipe COMBO), kombo tipe (mengganti visibilitas editor nilai saat diubah), dan tombol
	 * ubah/hapus. Sebagian besar perubahan langsung tersimpan ke basis data bila baris sudah
	 * tersimpan.
	 *
	 * @param row                              baris grid yang diisi
	 * @param klasifikasiSuratKeluarParemeter  data parameter untuk baris ini
	 */
	public void initRow(final Row row, final KlasifikasiSuratKeluarParemeter klasifikasiSuratKeluarParemeter)
			throws Exception {
		row.setValign("top");
		row.setAttribute("klasifikasiSuratKeluarParemeter", klasifikasiSuratKeluarParemeter);
		final Combobox tipe = new Combobox();

		new Label(klasifikasiSuratKeluarParemeter.getKeterangan()).setParent(row);
		new Label(klasifikasiSuratKeluarParemeter.getKey()).setParent(row);

		final Intbox nomorUrut = new Intbox(klasifikasiSuratKeluarParemeter.getNomorUrut());
		nomorUrut.setWidth("90%");
		nomorUrut.setParent(row);
		nomorUrut.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				klasifikasiSuratKeluarParemeter.setNomorUrut(nomorUrut.getValue());
				row.setValign("top");
				row.setAttribute("klasifikasiSuratKeluarParemeter", klasifikasiSuratKeluarParemeter);
				if (klasifikasiSuratKeluarParemeter.getId() != null) {
					Session session = HibernateUtil.currentSession();
					session.update(klasifikasiSuratKeluarParemeter);
				}
			}
		});

		final MyCheckboxConfig tampil = new MyCheckboxConfig("Tampil");
		tampil.setChecked(klasifikasiSuratKeluarParemeter.getTampil());
		tampil.setParent(row);
		tampil.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				klasifikasiSuratKeluarParemeter.setTampil(tampil.isChecked());
				row.setValign("top");
				row.setAttribute("klasifikasiSuratKeluarParemeter", klasifikasiSuratKeluarParemeter);
				if (klasifikasiSuratKeluarParemeter.getId() != null) {
					Session session = HibernateUtil.currentSession();
					session.update(klasifikasiSuratKeluarParemeter);
				}
			}
		});

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		final Textbox nilai = new Textbox(klasifikasiSuratKeluarParemeter.getNilai());
		nilai.setWidth("90%");
		nilai.setParent(hbox);
		nilai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				klasifikasiSuratKeluarParemeter.setNilai(nilai.getValue().trim());
				row.setValign("top");
				row.setAttribute("klasifikasiSuratKeluarParemeter", klasifikasiSuratKeluarParemeter);
				if (klasifikasiSuratKeluarParemeter.getId() != null) {
					Session session = HibernateUtil.currentSession();
					session.update(klasifikasiSuratKeluarParemeter);
				}
			}
		});

		final Textbox pilihan = new Textbox(klasifikasiSuratKeluarParemeter.getPilihan());
		pilihan.setWidth("90%");
		pilihan.setRows(2);
		pilihan.setParent(hbox);
		pilihan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				klasifikasiSuratKeluarParemeter.setPilihan(pilihan.getValue().trim());
				row.setValign("top");
				row.setAttribute("klasifikasiSuratKeluarParemeter", klasifikasiSuratKeluarParemeter);
				if (klasifikasiSuratKeluarParemeter.getId() != null) {
					Session session = HibernateUtil.currentSession();
					session.update(klasifikasiSuratKeluarParemeter);
				}
			}
		});

		final Hbox gambarHbox = new Hbox();
		gambarHbox.setParent(hbox);
		LampiranLain.createDownloadUploadFileLain(gambarHbox, klasifikasiSuratKeluarParemeter.getId(),
				KlasifikasiSuratKeluarParemeter.class.getName(), "Gambar", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahasiswa = (LampiranLain) arg0.getData();
						klasifikasiSuratKeluarParemeter.setNilai(lainMahasiswa.ambilFile().getAbsolutePath());
						row.setValign("top");
						row.setAttribute("klasifikasiSuratKeluarParemeter", klasifikasiSuratKeluarParemeter);
						if (klasifikasiSuratKeluarParemeter.getId() != null) {
							Session session = HibernateUtil.currentSession();
							session.update(klasifikasiSuratKeluarParemeter);
						}
					}
				});
		gambarHbox.setParent(hbox);

//		hbox = new Hbox();
//		hbox.setParent(row);

		final MyButtonConfig tombol = new MyButtonConfig("Ubah Nilai Default");
		tombol.setWidth("90%");
		tombol.setParent(hbox);
		tombol.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Nilai Default", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("440px");
				window.setWidth("850px");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				final MyCkEditor nilai = new MyCkEditor();
				nilai.setValue(klasifikasiSuratKeluarParemeter.getNilai());
				nilai.setParent(center);

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
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						if (nilai.getValue().trim().equals("")) {
							MyMessageboxConfig.show("Mohon maaf, Nilai Parameter belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nilai Parameter; (2) isikan nilai parameter yang sesuai; (3) klik tombol Simpan kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
							return;
						}

						klasifikasiSuratKeluarParemeter.setNilai(nilai.getValue().trim());
						row.setValign("top");
						row.setAttribute("klasifikasiSuratKeluarParemeter", klasifikasiSuratKeluarParemeter);
						if (klasifikasiSuratKeluarParemeter.getId() != null) {
							Session session = HibernateUtil.currentSession();
							session.update(klasifikasiSuratKeluarParemeter);
						}

						window.detach();
					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});

		final MyButtonConfig tombolData = new MyButtonConfig("Ubah Nilai Default");
		tombolData.setWidth("90%");
		tombolData.setParent(hbox);
		tombolData.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Nilai Default", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("440px");
				window.setWidth("90%");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				String nilai = klasifikasiSuratKeluarParemeter.getNilai();
				String[] rrr = new String[100];
				if (nilai != null && !nilai.trim().isEmpty()) {
					String[] s = StringUtils.split(nilai, "||");
					for (int i = 0; i < s.length; i++) {
						try {
							rrr[i] = s[i];
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/helper/KlasifikasiSuratKeluarParameterHelper.java:504");

						}
					}
				}

				MyGrid grid = new MyGrid();
				grid.setParent(center);
				final Rows rows = new Rows();
				rows.setParent(grid);
				for (int r = 0; r < 100; r++) {
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					String nil = rrr[r];
					String[] val = nil == null || nil.trim().isEmpty() ? new String[15] : StringUtils.split(nil, "<->");
					for (int col = 0; col < 15; col++) {
						String v = val.length > col ? val[col] : "";
						Textbox textbox = new Textbox(v);
						textbox.setWidth("90%");
						textbox.setParent(row);
					}
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
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						String hasil = "";
						for (Object o : rows.getChildren()) {
							if (o instanceof Row) {
								Row row = (Row) o;
								String ss = "";
								for (Object c : row.getChildren()) {
									if (c instanceof Textbox) {
										Textbox d = (Textbox) c;
										ss += ss.isEmpty() ? d.getValue() : "<->" + d.getValue();
									}
								}
								hasil += hasil.isEmpty() ? ss : "||" + ss;
							}
						}

						klasifikasiSuratKeluarParemeter.setNilai(hasil);
						row.setValign("top");
						row.setAttribute("klasifikasiSuratKeluarParemeter", klasifikasiSuratKeluarParemeter);
						if (klasifikasiSuratKeluarParemeter.getId() != null) {
							Session session = HibernateUtil.currentSession();
							session.update(klasifikasiSuratKeluarParemeter);
						}

						window.detach();
					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});

		tipe.setWidth("90%");
		tipe.setParent(row);
		MyComboitemConfig comboitem = new MyComboitemConfig(String.class.getSimpleName());
		comboitem.setValue(String.class.getName());
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Integer.class.getSimpleName());
		comboitem.setValue(Integer.class.getName());
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Double.class.getSimpleName());
		comboitem.setValue(Double.class.getName());
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Date.class.getSimpleName());
		comboitem.setValue(Date.class.getName());
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(KlasifikasiSuratKeluarParemeter.COMBO);
		comboitem.setValue(KlasifikasiSuratKeluarParemeter.COMBO);
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(KlasifikasiSuratKeluarParemeter.GAMBAR);
		comboitem.setValue(KlasifikasiSuratKeluarParemeter.GAMBAR);
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(KlasifikasiSuratKeluarParemeter.TEXT);
		comboitem.setValue(KlasifikasiSuratKeluarParemeter.TEXT);
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(KlasifikasiSuratKeluarParemeter.DATA);
		comboitem.setValue(KlasifikasiSuratKeluarParemeter.DATA);
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(KlasifikasiSuratKeluarParemeter.DAFTAR_MAHASISWA);
		comboitem.setValue(KlasifikasiSuratKeluarParemeter.DAFTAR_MAHASISWA);
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(KlasifikasiSuratKeluarParemeter.DAFTAR_SISWA);
		comboitem.setValue(KlasifikasiSuratKeluarParemeter.DAFTAR_SISWA);
		tipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(KlasifikasiSuratKeluarParemeter.DAFTAR_PENGGUNA);
		comboitem.setValue(KlasifikasiSuratKeluarParemeter.DAFTAR_PENGGUNA);
		tipe.appendChild(comboitem);

		tipe.setReadonly(true);

		Common.selectComboItem(tipe, klasifikasiSuratKeluarParemeter.getTipe());

		tipe.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				klasifikasiSuratKeluarParemeter
						.setTipe((String) (tipe.getSelectedItem() == null ? null : tipe.getSelectedItem().getValue()));
				row.setValign("top");
				row.setAttribute("klasifikasiSuratKeluarParemeter", klasifikasiSuratKeluarParemeter);
				if (klasifikasiSuratKeluarParemeter.getId() != null) {
					Session session = HibernateUtil.currentSession();
					session.update(klasifikasiSuratKeluarParemeter);
				}

				nilai.setValue(klasifikasiSuratKeluarParemeter.getNilai());

				nilai.setVisible(!tipe.getValue().equalsIgnoreCase(KlasifikasiSuratKeluarParemeter.GAMBAR)
						&& !tipe.getValue().equalsIgnoreCase(KlasifikasiSuratKeluarParemeter.TEXT)
						&& !tipe.getValue().equalsIgnoreCase(KlasifikasiSuratKeluarParemeter.DATA));
				gambarHbox.setVisible(tipe.getValue().equalsIgnoreCase(KlasifikasiSuratKeluarParemeter.GAMBAR));
				tombol.setVisible(tipe.getValue().equalsIgnoreCase(KlasifikasiSuratKeluarParemeter.TEXT));
				tombolData.setVisible(tipe.getValue().equalsIgnoreCase(KlasifikasiSuratKeluarParemeter.DATA));

				pilihan.setVisible(tipe.getValue().equalsIgnoreCase(KlasifikasiSuratKeluarParemeter.COMBO));
			}
		});

		nilai.setVisible(!tipe.getValue().equalsIgnoreCase(KlasifikasiSuratKeluarParemeter.GAMBAR)
				&& !tipe.getValue().equalsIgnoreCase(KlasifikasiSuratKeluarParemeter.TEXT)
				&& !tipe.getValue().equalsIgnoreCase(KlasifikasiSuratKeluarParemeter.DATA));
		gambarHbox.setVisible(tipe.getValue().equalsIgnoreCase(KlasifikasiSuratKeluarParemeter.GAMBAR));
		tombol.setVisible(tipe.getValue().equalsIgnoreCase(KlasifikasiSuratKeluarParemeter.TEXT));
		tombolData.setVisible(tipe.getValue().equalsIgnoreCase(KlasifikasiSuratKeluarParemeter.DATA));

		pilihan.setVisible(tipe.getValue().equalsIgnoreCase(KlasifikasiSuratKeluarParemeter.COMBO));

		hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
		button.setTooltiptext("Edit Data");
		button.setVisible(edit);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onAdd(klasifikasiSuratKeluarParemeter, row);
			}
		});

		button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (klasifikasiSuratKeluarParemeter.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(klasifikasiSuratKeluarParemeter);
									}
									row.setVisible(false);
									row.detach();
								}

							}
						});

			}
		});
	}

	/** @return kriteria pencarian {@link KlasifikasiSuratKeluarParemeter} milik {@link #klasifikasiSuratKeluar} (atau {@code copyDari}-nya bila diisi), diurutkan menaik berdasarkan nomor urut lalu menurun berdasarkan id. */
	@Override
	public Criteria initCriteria(boolean order) {
		// TODO Auto-generated method stub
		return HibernateUtil.currentSession().createCriteria(KlasifikasiSuratKeluarParemeter.class)
				.addOrder(Order.asc("nomorUrut")).addOrder(Order.desc("id"))
				.add(klasifikasiSuratKeluar.getCopyDari() == null
						? Restrictions.eq("klasifikasiSuratKeluar", klasifikasiSuratKeluar)
						: Restrictions.eq("klasifikasiSuratKeluar", klasifikasiSuratKeluar.getCopyDari()));
	}

	/** Alias {@link #loadDataDetail()} untuk memenuhi kontrak {@link DataSearchDefault}. */
	@Override
	public void onSearchDefault(Event event) {
		loadDataDetail();
	}

}
