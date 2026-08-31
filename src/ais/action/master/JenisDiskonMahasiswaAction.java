package ais.action.master;

import java.math.BigDecimal;
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
import org.zkoss.zul.Decimalbox;
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
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisDiskonMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.StatusAwalMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk jenis diskon mahasiswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Checkbox searchaktif}, {@code Textbox nama},
 * {@code Textbox keterangan}, {@code boolean edit}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}, {@code
 * keteranganFilterMahasiswa()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
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
public class JenisDiskonMahasiswaAction extends GenericAutowireComposer
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

	private JenisDiskonMahasiswa jenisDiskonMahasiswa;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private MyDoublebox diskon;
	private MyCheckboxConfig berupaPersen;
	private Combobox itemBiaya;
	private Decimalbox semesterMulai;
	private Decimalbox semesterSampai;
	private Combobox itemBiaya2;
	private Combobox itemBiaya3;
	private Combobox itemBiaya4;
	private Combobox itemBiaya5;
	private MyDatebox tanggalMulaiBerlaku;
	private MyDatebox tanggalSampaiBerlaku;
	private MyCheckboxConfig berlakuUntukSemuaMahasiswa;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox program;
	private Combobox statusAwalMahasiswa;

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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "aktif", "diskon", "itemBiaya",
				"berupaPersen", "semesterMulai", "semesterSampai", "tanggalMulaiBerlaku", "tanggalSampaiBerlaku",
				"berlakuUntukSemuaMahasiswa", "fakultas", "jurusan", "program", "statusAwalMahasiswa" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisDiskonMahasiswa.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisDiskonMahasiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class JenisDiskonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisDiskonMahasiswa jenisDiskonMahasiswa = (JenisDiskonMahasiswa) arg1;
			new Label(jenisDiskonMahasiswa.getKode()).setParent(arg0);
			RevisiHelper
					.createNewRevisi(JenisDiskonMahasiswa.class, jenisDiskonMahasiswa, jenisDiskonMahasiswa.getNama())
					.setParent(arg0);
			new Label(Common.numberFormat.get().format(jenisDiskonMahasiswa.getDiskon())).setParent(arg0);
			new Label(jenisDiskonMahasiswa.getBerupaPersen() ? "Ya" : "Tidak").setParent(arg0);

			String item = "";
			for (ItemBiaya itemBiaya : jenisDiskonMahasiswa.ambilItemBiayas()) {
				item += item.isEmpty() ? itemBiaya.getNama() : ", " + itemBiaya.getNama();
			}

			if (item.trim().isEmpty()) {
				item = "Tidak ditentukan";
			}

			new Label(item).setParent(arg0);

			new Label((jenisDiskonMahasiswa.getSemesterMulai() == null ? "" : jenisDiskonMahasiswa.getSemesterMulai())
					+ " sd " + (jenisDiskonMahasiswa.getSemesterSampai() == null ? ""
							: jenisDiskonMahasiswa.getSemesterSampai()))
					.setParent(arg0);
			new Label((jenisDiskonMahasiswa.getTanggalMulaiBerlaku() == null ? ""
					: Common.dateFormat1.get().format(jenisDiskonMahasiswa.getTanggalMulaiBerlaku())) + " sd "
					+ (jenisDiskonMahasiswa.getTanggalSampaiBerlaku() == null ? ""
							: Common.dateFormat1.get().format(jenisDiskonMahasiswa.getTanggalSampaiBerlaku())))
					.setParent(arg0);
			new Label(keteranganFilterMahasiswa(jenisDiskonMahasiswa)).setParent(arg0);

			new Label(jenisDiskonMahasiswa.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisDiskonMahasiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisDiskonMahasiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisDiskonMahasiswa);
					// Promo global di-cache 60 detik di mesin tagihan — kosongkan agar
					// pengaktifan/penonaktifan langsung terasa pada perhitungan tagihan.
					JenisDiskonMahasiswa.bersihkanCachePromoGlobal();
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisDiskonMahasiswa, JenisDiskonMahasiswaAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisDiskonMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisDiskonMahasiswa = (JenisDiskonMahasiswa) obj;
		init(jenisDiskonMahasiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisDiskonMahasiswa jenisDiskonMahasiswa) {
		this.jenisDiskonMahasiswa = jenisDiskonMahasiswa;
		addWindow.setTitle(jenisDiskonMahasiswa.getId() == null ? "Tambah Jenis Diskon" : "Ubah Jenis Diskon");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Jenis Diskon"));
		row.appendChild(kode = new Textbox(jenisDiskonMahasiswa.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Diskon"));
		row.appendChild(nama = new Textbox(jenisDiskonMahasiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Diskon"));
		row.appendChild(diskon = new MyDoublebox(jenisDiskonMahasiswa.getDiskon()));
		diskon.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(berupaPersen = new MyCheckboxConfig("Berupa persen"));
		berupaPersen.setChecked(jenisDiskonMahasiswa.getBerupaPersen());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Default Item Biaya I"));
		row.appendChild(itemBiaya = new Combobox());
		Common.insertComboDanSemua(itemBiaya, new String[] { "nama" }, "deskripsi", ItemBiaya.class,
				"=Item Biaya Ditentukan Nanti=", Restrictions.eq("aktif", true));
		Common.selectComboItem(itemBiaya, jenisDiskonMahasiswa.getItemBiaya());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Default Item Biaya II"));
		row.appendChild(itemBiaya2 = new Combobox());
		Common.insertComboDanSemua(itemBiaya2, new String[] { "nama" }, "deskripsi", ItemBiaya.class,
				"=Item Biaya Ditentukan Nanti=", Restrictions.eq("aktif", true));
		Common.selectComboItem(itemBiaya2, jenisDiskonMahasiswa.getItemBiaya2());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Default Item Biaya III"));
		row.appendChild(itemBiaya3 = new Combobox());
		Common.insertComboDanSemua(itemBiaya3, new String[] { "nama" }, "deskripsi", ItemBiaya.class,
				"=Item Biaya Ditentukan Nanti=", Restrictions.eq("aktif", true));
		Common.selectComboItem(itemBiaya3, jenisDiskonMahasiswa.getItemBiaya3());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Default Item Biaya IV"));
		row.appendChild(itemBiaya4 = new Combobox());
		Common.insertComboDanSemua(itemBiaya4, new String[] { "nama" }, "deskripsi", ItemBiaya.class,
				"=Item Biaya Ditentukan Nanti=", Restrictions.eq("aktif", true));
		Common.selectComboItem(itemBiaya4, jenisDiskonMahasiswa.getItemBiaya4());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Default Item Biaya V"));
		row.appendChild(itemBiaya5 = new Combobox());
		Common.insertComboDanSemua(itemBiaya5, new String[] { "nama" }, "deskripsi", ItemBiaya.class,
				"=Item Biaya Ditentukan Nanti=", Restrictions.eq("aktif", true));
		Common.selectComboItem(itemBiaya5, jenisDiskonMahasiswa.getItemBiaya5());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester Default"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		semesterMulai = new Decimalbox(jenisDiskonMahasiswa.getSemesterMulai() == null ? null
				: new BigDecimal(jenisDiskonMahasiswa.getSemesterMulai()));
		semesterMulai.setCols(3);
		semesterSampai = new Decimalbox(jenisDiskonMahasiswa.getSemesterSampai() == null ? null
				: new BigDecimal(jenisDiskonMahasiswa.getSemesterSampai()));
		semesterSampai.setCols(3);
		hbox.appendChild(semesterMulai);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		hbox.appendChild(semesterSampai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai Berlaku"));
		row.appendChild(tanggalMulaiBerlaku = new MyDatebox(jenisDiskonMahasiswa.getTanggalMulaiBerlaku()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai Berlaku"));
		row.appendChild(tanggalSampaiBerlaku = new MyDatebox(jenisDiskonMahasiswa.getTanggalSampaiBerlaku()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(berlakuUntukSemuaMahasiswa = new MyCheckboxConfig("Berlaku Untuk Semua Mahasiswa"));
		berlakuUntukSemuaMahasiswa.setChecked(jenisDiskonMahasiswa.getBerlakuUntukSemuaMahasiswa());

		final MyFormRow rowFakultas = new MyFormRow();
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);
		rowFakultas.appendChild(fakultas);
		fakultas.setReadonly(true);
		fakultas.setWidth("90%");
		Common.selectComboItem(fakultas, jenisDiskonMahasiswa.getFakultas());

		final MyFormRow rowJurusan = new MyFormRow();
		rowJurusan.setParent(rows);
		rowJurusan.appendChild(new ais.ui.util.MyLabelConfig("Jurusan"));
		rowJurusan.appendChild(jurusan);
		jurusan.setReadonly(true);
		jurusan.setWidth("90%");
		Common.selectComboItem(jurusan, jenisDiskonMahasiswa.getJurusan());

		final MyFormRow rowProgram = new MyFormRow();
		rowProgram.setParent(rows);
		rowProgram.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		program = Common.initPrograms(program);
		rowProgram.appendChild(program);
		program.setReadonly(true);
		program.setWidth("90%");
		Common.selectComboItem(program, jenisDiskonMahasiswa.getProgram());

		final MyFormRow rowStatusAwal = new MyFormRow();
		rowStatusAwal.setParent(rows);
		rowStatusAwal.appendChild(new ais.ui.util.MyLabelConfig("Status Awal"));
		Common.insertComboDanSemua(statusAwalMahasiswa = new Combobox(), "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(statusAwalMahasiswa, jenisDiskonMahasiswa.getStatusAwalMahasiswa());
		rowStatusAwal.appendChild(statusAwalMahasiswa);
		statusAwalMahasiswa.setReadonly(true);
		statusAwalMahasiswa.setWidth("90%");

		EventListener tampilkanFilterMahasiswa = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				boolean tampil = berlakuUntukSemuaMahasiswa.isChecked();
				rowFakultas.setVisible(tampil);
				rowJurusan.setVisible(tampil);
				rowProgram.setVisible(tampil);
				rowStatusAwal.setVisible(tampil);
			}
		};
		berlakuUntukSemuaMahasiswa.addEventListener("onCheck", tampilkanFilterMahasiswa);
		try {
			tampilkanFilterMahasiswa.onEvent(null);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "JenisDiskonMahasiswaAction.toggleFilterMahasiswa");
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisDiskonMahasiswa.getKeterangan()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Diskon Mahasiswa",
					"Kolom Nama Jenis Diskon belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data "
							+ "dapat disimpan.",
					new String[] {
							"Isi terlebih dahulu kolom Nama Jenis Diskon.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		/* VALIDASI 21-08-2026 -- mencegah promo yang diam-diam tidak berefek.
		 *
		 * Mesin tagihan (JenisDiskonMahasiswa.cocokUntukTagihanGlobal) baru mengenakan promo
		 * global bila item biaya baris tagihan termasuk daftar Default Item Biaya I..V. Bila
		 * daftar itu kosong, promo dilewati TANPA pesan apa pun, dan pengguna menyimpulkan
		 * diskonnya rusak padahal datanya yang belum lengkap. Hal yang sama berlaku untuk
		 * nilai potongan nol dan rentang tanggal terbalik. */
		if (berlakuUntukSemuaMahasiswa.isChecked()) {
			boolean adaItemBiaya = itemBiaya.getSelectedItem() != null || itemBiaya2.getSelectedItem() != null
					|| itemBiaya3.getSelectedItem() != null || itemBiaya4.getSelectedItem() != null
					|| itemBiaya5.getSelectedItem() != null;
			if (!adaItemBiaya) {
				PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Diskon Mahasiswa",
						"Diskon ini Bapak/Ibu tandai \"Berlaku Untuk Semua Mahasiswa\", tetapi belum ada satu pun "
								+ "Default Item Biaya yang dipilih. Tanpa item biaya, potongan tidak akan pernah "
								+ "dikenakan pada tagihan mana pun.",
						new String[] {
								"Pilih sekurang-kurangnya satu Default Item Biaya (I sampai V) yang hendak dipotong.",
								"Ulangi proses penyimpanan setelah item biaya tersebut terisi."
						});
				return false;
			}
			if (diskon.getValue() == null || diskon.getValue().doubleValue() <= 0.0) {
				PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Diskon Mahasiswa",
						"Nilai potongan masih kosong atau nol, padahal diskon ini ditandai berlaku untuk semua "
								+ "mahasiswa. Promo dengan potongan nol tidak mengubah tagihan sama sekali.",
						new String[] {
								"Isi kolom Diskon dengan nilai lebih besar dari nol.",
								"Pastikan pilihan persen atau nominal sudah sesuai maksud Bapak/Ibu."
						});
				return false;
			}
		}
		if (tanggalMulaiBerlaku.getValue() != null && tanggalSampaiBerlaku.getValue() != null
				&& tanggalMulaiBerlaku.getValue().after(tanggalSampaiBerlaku.getValue())) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Diskon Mahasiswa",
					"Tanggal Mulai Berlaku lebih akhir daripada Tanggal Sampai Berlaku, sehingga rentang "
							+ "waktunya kosong dan diskon tidak akan pernah aktif.",
					new String[] {
							"Perbaiki salah satu tanggal sehingga Mulai tidak melewati Sampai.",
							"Kosongkan kedua tanggal bila diskon dimaksudkan berlaku tanpa batas waktu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisDiskonMahasiswa.getId() != null) {
			jenisDiskonMahasiswa = (JenisDiskonMahasiswa) session.load(JenisDiskonMahasiswa.class,
					jenisDiskonMahasiswa.getId());

		}

		jenisDiskonMahasiswa.setKode(kode.getValue());
		jenisDiskonMahasiswa.setNama(nama.getValue());
		jenisDiskonMahasiswa.setDiskon(diskon.getValue());
		jenisDiskonMahasiswa.setBerupaPersen(berupaPersen.isChecked());
		jenisDiskonMahasiswa.setItemBiaya(
				(ItemBiaya) (itemBiaya.getSelectedItem() == null ? null : itemBiaya.getSelectedItem().getValue()));

		jenisDiskonMahasiswa.setItemBiaya2(
				(ItemBiaya) (itemBiaya2.getSelectedItem() == null ? null : itemBiaya2.getSelectedItem().getValue()));

		jenisDiskonMahasiswa.setItemBiaya3(
				(ItemBiaya) (itemBiaya3.getSelectedItem() == null ? null : itemBiaya3.getSelectedItem().getValue()));

		jenisDiskonMahasiswa.setItemBiaya4(
				(ItemBiaya) (itemBiaya4.getSelectedItem() == null ? null : itemBiaya4.getSelectedItem().getValue()));

		jenisDiskonMahasiswa.setItemBiaya5(
				(ItemBiaya) (itemBiaya5.getSelectedItem() == null ? null : itemBiaya5.getSelectedItem().getValue()));

		jenisDiskonMahasiswa.setKeterangan(keterangan.getValue());

		jenisDiskonMahasiswa
				.setSemesterMulai(semesterMulai.getValue() == null ? null : semesterMulai.getValue().intValue());
		jenisDiskonMahasiswa
				.setSemesterSampai(semesterSampai.getValue() == null ? null : semesterSampai.getValue().intValue());
		jenisDiskonMahasiswa.setTanggalMulaiBerlaku(tanggalMulaiBerlaku.getValue());
		jenisDiskonMahasiswa.setTanggalSampaiBerlaku(tanggalSampaiBerlaku.getValue());
		jenisDiskonMahasiswa.setBerlakuUntukSemuaMahasiswa(berlakuUntukSemuaMahasiswa.isChecked());
		if (berlakuUntukSemuaMahasiswa.isChecked()) {
			jenisDiskonMahasiswa.setFakultas(
					(Fakultas) (fakultas.getSelectedItem() == null ? null : fakultas.getSelectedItem().getValue()));
			jenisDiskonMahasiswa.setJurusan(
					(Jurusan) (jurusan.getSelectedItem() == null ? null : jurusan.getSelectedItem().getValue()));
			jenisDiskonMahasiswa.setProgram(
					(String) (program.getSelectedItem() == null ? null : program.getSelectedItem().getValue()));
			jenisDiskonMahasiswa.setStatusAwalMahasiswa((StatusAwalMahasiswa) (statusAwalMahasiswa
					.getSelectedItem() == null ? null : statusAwalMahasiswa.getSelectedItem().getValue()));
		} else {
			jenisDiskonMahasiswa.setFakultas(null);
			jenisDiskonMahasiswa.setJurusan(null);
			jenisDiskonMahasiswa.setProgram(null);
			jenisDiskonMahasiswa.setStatusAwalMahasiswa(null);
		}

		Common.refreshSaveOrUpdate(session, jenisDiskonMahasiswa);
		// Promo global ("Berlaku Untuk Semua Mahasiswa") di-cache 60 detik di mesin tagihan —
		// kosongkan agar perubahan nilai/tanggal/filter langsung berlaku pada tagihan.
		JenisDiskonMahasiswa.bersihkanCachePromoGlobal();

		return true;
	}

	private String keteranganFilterMahasiswa(JenisDiskonMahasiswa jenisDiskonMahasiswa) {
		if (!jenisDiskonMahasiswa.getBerlakuUntukSemuaMahasiswa()) {
			return "";
		}
		String keterangan = "Semua mahasiswa";
		if (jenisDiskonMahasiswa.getFakultas() != null) {
			keterangan += ", Fakultas: " + jenisDiskonMahasiswa.getFakultas().getNama();
		}
		if (jenisDiskonMahasiswa.getJurusan() != null) {
			keterangan += ", Jurusan: " + jenisDiskonMahasiswa.getJurusan().getNama();
		}
		if (jenisDiskonMahasiswa.getProgram() != null && !jenisDiskonMahasiswa.getProgram().trim().isEmpty()) {
			keterangan += ", Program: " + jenisDiskonMahasiswa.getProgram();
		}
		if (jenisDiskonMahasiswa.getStatusAwalMahasiswa() != null) {
			keterangan += ", Status Awal: " + jenisDiskonMahasiswa.getStatusAwalMahasiswa().getNama();
		}
		return keterangan;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisDiskonMahasiswa.class)
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

		List<JenisDiskonMahasiswa> jenisDiskonMahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisDiskonMahasiswa);
		grid.setRowRenderer(new JenisDiskonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
