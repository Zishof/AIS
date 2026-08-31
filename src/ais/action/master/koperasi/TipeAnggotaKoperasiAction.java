package ais.action.master.koperasi;

import java.util.List;
import java.util.LinkedHashMap;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Columns;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.koperasi.TipeAnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.inventory.Toko;
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
 * Controller/action ZK untuk tipe anggota koperasi. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchkode}, {@code Checkbox
 * searchaktif}, {@code Textbox nama}, {@code Textbox keterangan}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); validasi/perhitungan ({@code
 * checkNamaTipeAnggotaKoperasi()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * ringkasanLimit()}, {@code ringkasanVerifikasi()}, {@code onAdd()}, {@code angka()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class TipeAnggotaKoperasiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox keterangan;
	private MyCheckboxConfig wajibHp;
	private MyCheckboxConfig wajibEmail;
	private Decimalbox maksimalBolehUtang;
	private Decimalbox maksimalHarian;
	private Decimalbox maksimalMingguan;
	private Decimalbox maksimalBulanan;
	private MyCheckboxConfig wajibPin;
	private MyCheckboxConfig wajibWajah;
	private MyCheckboxConfig wajibFingerprint;
	private MyCheckboxConfig kunciCaraBayar;
	private Combobox caraBayarDefault;
	private Map<Checkbox, CaraPembayaranKoperasi> pilihanCaraBayar = new LinkedHashMap<Checkbox, CaraPembayaranKoperasi>();
	private Map<Checkbox, CaraPembayaranKoperasi> pilihanCaraBayarWajibPin = new LinkedHashMap<Checkbox, CaraPembayaranKoperasi>();
	private MyCheckboxConfig berlakuSemuaToko;
	private Map<Checkbox, Toko> pilihanToko = new LinkedHashMap<Checkbox, Toko>();
	/**
	 * true bila admin sudah menyentuh checkbox "Wajib No. HP" secara manual pada sesi
	 * form ini; dipakai agar default per nama (lihat {@link TipeAnggotaKoperasi#defaultWajibHp(String)})
	 * berhenti mengikuti ketikan kolom Nama begitu admin menentukan pilihannya sendiri.
	 */
	private boolean wajibHpDiubahManual = false;

	private boolean edit = false;
	private boolean delete = false;

	private TipeAnggotaKoperasi tipeAnggotaKoperasi;
	private MyToolbarbuttonConfig add;
	private Textbox kode;

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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(TipeAnggotaKoperasi.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, TipeAnggotaKoperasi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class TipeAnggotaKoperasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final TipeAnggotaKoperasi tipeAnggotaKoperasi = (TipeAnggotaKoperasi) arg1;
			new Label(tipeAnggotaKoperasi.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(TipeAnggotaKoperasi.class, tipeAnggotaKoperasi, tipeAnggotaKoperasi.getNama())
					.setParent(arg0);
			new Label(tipeAnggotaKoperasi.getKeterangan()).setParent(arg0);
			new Label(ringkasanLimit(tipeAnggotaKoperasi)).setParent(arg0);
			new Label(ringkasanVerifikasi(tipeAnggotaKoperasi)).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(tipeAnggotaKoperasi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					tipeAnggotaKoperasi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(tipeAnggotaKoperasi);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, tipeAnggotaKoperasi, TipeAnggotaKoperasiAction.this)
					.setParent(arg0);

		}

	}

	private String ringkasanLimit(TipeAnggotaKoperasi tipe) {
		java.util.List<String> bagian = new java.util.ArrayList<String>();
		if (tipe.getMaksimalTransaksiHarian() > 0) bagian.add("H: " + Common.numberFormat.get().format(tipe.getMaksimalTransaksiHarian()));
		if (tipe.getMaksimalTransaksiMingguan() > 0) bagian.add("M: " + Common.numberFormat.get().format(tipe.getMaksimalTransaksiMingguan()));
		if (tipe.getMaksimalTransaksiBulanan() > 0) bagian.add("B: " + Common.numberFormat.get().format(tipe.getMaksimalTransaksiBulanan()));
		return bagian.isEmpty() ? "Tanpa batas" : bagian.toString().replace("[", "").replace("]", "");
	}

	private String ringkasanVerifikasi(TipeAnggotaKoperasi tipe) {
		java.util.List<String> bagian = new java.util.ArrayList<String>();
		if (Boolean.TRUE.equals(tipe.getWajibPin())) bagian.add("PIN");
		if (Boolean.TRUE.equals(tipe.getWajibVerifikasiBiometricWajah())) bagian.add("Wajah");
		if (Boolean.TRUE.equals(tipe.getWajibVerifikasiBiometricFingerprint())) bagian.add("Fingerprint");
		return bagian.isEmpty() ? "-" : bagian.toString().replace("[", "").replace("]", "");
	}

	public void onAdd(Event event) throws Exception {
		init(new TipeAnggotaKoperasi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		tipeAnggotaKoperasi = (TipeAnggotaKoperasi) obj;
		init(tipeAnggotaKoperasi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(TipeAnggotaKoperasi tipeAnggotaKoperasi) {
		this.tipeAnggotaKoperasi = tipeAnggotaKoperasi;
		addWindow.setTitle(tipeAnggotaKoperasi.getId() == null ? "Tambah Jenis Identitas Koperasi" : "Ubah Jenis Identitas Koperasi");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Jenis Identitas"));
		row.appendChild(kode = new Textbox(tipeAnggotaKoperasi.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Identitas *"));
		row.appendChild(nama = new Textbox(tipeAnggotaKoperasi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(tipeAnggotaKoperasi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Maksimal Boleh Utang"));
		row.appendChild(maksimalBolehUtang = angka(tipeAnggotaKoperasi.getMaksimalBolehUtang()));

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Batas Transaksi"));
		Vbox batas = new Vbox();
		batas.appendChild(new Label("Harian (0 = tanpa batas)"));
		batas.appendChild(maksimalHarian = angka(tipeAnggotaKoperasi.getMaksimalTransaksiHarian()));
		batas.appendChild(new Label("Mingguan (0 = tanpa batas)"));
		batas.appendChild(maksimalMingguan = angka(tipeAnggotaKoperasi.getMaksimalTransaksiMingguan()));
		batas.appendChild(new Label("Bulanan (0 = tanpa batas)"));
		batas.appendChild(maksimalBulanan = angka(tipeAnggotaKoperasi.getMaksimalTransaksiBulanan()));
		row.appendChild(batas);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cakupan Toko"));
		final Vbox cakupanToko = new Vbox();
		berlakuSemuaToko = new MyCheckboxConfig("Berlaku ke semua toko");
		berlakuSemuaToko.setChecked(Boolean.TRUE.equals(tipeAnggotaKoperasi.getBerlakuSemuaToko()));
		cakupanToko.appendChild(berlakuSemuaToko);
		cakupanToko.appendChild(new Label("Jika tidak aktif, pilih satu atau beberapa toko berikut."));
		pilihanToko.clear();
		String csvToko = tipeAnggotaKoperasi.getDaftarToko();
		@SuppressWarnings("unchecked")
		List<Toko> semuaToko = HibernateUtil.currentSession().createCriteria(Toko.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama")).list();
		for (Toko toko : semuaToko) {
			Checkbox cbToko = new Checkbox(toko.getNama());
			cbToko.setChecked(csvToko.contains("," + toko.getId() + ","));
			cbToko.setDisabled(berlakuSemuaToko.isChecked());
			cakupanToko.appendChild(cbToko);
			pilihanToko.put(cbToko, toko);
		}
		berlakuSemuaToko.addEventListener("onCheck", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				for (Checkbox pilihan : pilihanToko.keySet()) {
					pilihan.setDisabled(berlakuSemuaToko.isChecked());
				}
			}
		});
		row.appendChild(cakupanToko);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cara Bayar"));
		Vbox bayar = new Vbox();
		pilihanCaraBayar.clear();
		caraBayarDefault = new Combobox();
		caraBayarDefault.setReadonly(true);
		Comboitem kosong = new Comboitem("-- Tidak ada default --");
		kosong.setValue(null);
		caraBayarDefault.appendChild(kosong);
		String csvCara = tipeAnggotaKoperasi.getDaftarCaraPembayaranYangBolehDiPilih();
		@SuppressWarnings("unchecked")
		List<CaraPembayaranKoperasi> semuaCara = HibernateUtil.currentSession().createCriteria(CaraPembayaranKoperasi.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama")).list();
		for (CaraPembayaranKoperasi cara : semuaCara) {
			Checkbox cb = new Checkbox(cara.getNama());
			cb.setChecked(csvCara.contains("," + cara.getId() + ","));
			bayar.appendChild(cb);
			pilihanCaraBayar.put(cb, cara);
			Comboitem item = new Comboitem(cara.getNama());
			item.setValue(cara.getId());
			caraBayarDefault.appendChild(item);
			if (cara.getId().equals(tipeAnggotaKoperasi.getCaraPembayaranDefaultId())) caraBayarDefault.setSelectedItem(item);
		}
		if (caraBayarDefault.getSelectedItem() == null) caraBayarDefault.setSelectedItem(kosong);
		bayar.appendChild(new Label("Cara bayar default"));
		bayar.appendChild(caraBayarDefault);
		kunciCaraBayar = new MyCheckboxConfig("Tidak boleh memakai cara bayar lain");
		kunciCaraBayar.setChecked(Boolean.TRUE.equals(tipeAnggotaKoperasi.getTidakBolehCaraPembayaranLain()));
		bayar.appendChild(kunciCaraBayar);
		row.appendChild(bayar);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Verifikasi Transaksi"));
		Vbox verifikasi = new Vbox();
		wajibPin = new MyCheckboxConfig("Wajib PIN numerik");
		wajibPin.setChecked(Boolean.TRUE.equals(tipeAnggotaKoperasi.getWajibPin()));
		verifikasi.appendChild(wajibPin);
		verifikasi.appendChild(new Label("PIN wajib untuk cara bayar (kosong = semua cara bayar)"));
		pilihanCaraBayarWajibPin.clear();
		String csvWajibPin = tipeAnggotaKoperasi.getDaftarCaraPembayaranWajibPin();
		for (CaraPembayaranKoperasi cara : semuaCara) {
			Checkbox cbPin = new Checkbox(cara.getNama());
			cbPin.setChecked(csvWajibPin.contains("," + cara.getId() + ","));
			verifikasi.appendChild(cbPin);
			pilihanCaraBayarWajibPin.put(cbPin, cara);
		}
		wajibWajah = new MyCheckboxConfig("Wajib biometric wajah (kamera + liveness)");
		wajibWajah.setChecked(Boolean.TRUE.equals(tipeAnggotaKoperasi.getWajibVerifikasiBiometricWajah()));
		verifikasi.appendChild(wajibWajah);
		wajibFingerprint = new MyCheckboxConfig("Wajib fingerprint (scanner USB/OTG + SDK)");
		wajibFingerprint.setChecked(Boolean.TRUE.equals(tipeAnggotaKoperasi.getWajibVerifikasiBiometricFingerprint()));
		verifikasi.appendChild(wajibFingerprint);
		row.appendChild(verifikasi);

		// Kebijakan kontak member: getter entitas sudah menerapkan default saat nilai DB
		// masih null (wajib HP mengikuti nama Pegawai/Dosen/Guru/Umum, email tidak wajib).
		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kebijakan Kontak Member"));
		org.zkoss.zul.Vbox kontakWadah = new org.zkoss.zul.Vbox();
		wajibHp = new MyCheckboxConfig("Wajib Memasukkan No. HP");
		wajibHp.setChecked(Boolean.TRUE.equals(tipeAnggotaKoperasi.getWajibHp()));
		wajibHp.setTooltiptext(
				"Bila aktif, Nomor HP wajib diisi saat menyimpan data member tipe ini. Default mengikuti nama tipe: Pegawai/Dosen/Guru/Umum aktif, tipe lain (mis. Mahasiswa/Siswa) tidak.");
		kontakWadah.appendChild(wajibHp);
		wajibEmail = new MyCheckboxConfig("Wajib Memasukkan Email");
		wajibEmail.setChecked(Boolean.TRUE.equals(tipeAnggotaKoperasi.getWajibEmail()));
		wajibEmail.setTooltiptext(
				"Bila aktif, Email wajib diisi saat menyimpan data member tipe ini. Default tidak aktif untuk semua tipe.");
		kontakWadah.appendChild(wajibEmail);
		row.appendChild(kontakWadah);

		wajibHpDiubahManual = false;
		wajibHp.addEventListener("onCheck", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				wajibHpDiubahManual = true;
			}
		});
		if (tipeAnggotaKoperasi.getId() == null) {
			// Tipe baru: selama admin belum menyentuh checkbox-nya, default wajib HP
			// mengikuti nama yang sedang diketik agar aturan per nama tetap terlihat.
			nama.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (!wajibHpDiubahManual) {
						wajibHp.setChecked(TipeAnggotaKoperasi.defaultWajibHp(nama.getValue()));
					}
				}
			});
		}

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

	private Decimalbox angka(Double nilai) {
		Decimalbox box = new Decimalbox();
		box.setValue(new java.math.BigDecimal(nilai == null ? 0.0 : Math.max(0.0, nilai.doubleValue())));
		box.setWidth("90%");
		return box;
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, nama tipe anggota koperasi belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Tipe Anggota; (2) gunakan nama yang deskriptif dan belum terpakai; (3) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaTipeAnggotaKoperasi();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, nama tipe anggota koperasi sudah ada di database. Langkah yang dapat dilakukan: (1) gunakan nama lain yang belum terdaftar; (2) cari tipe anggota yang sudah ada di daftar; (3) ulangi penyimpanan.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (tipeAnggotaKoperasi.getId() != null) {
			tipeAnggotaKoperasi = (TipeAnggotaKoperasi) session.load(TipeAnggotaKoperasi.class,
					tipeAnggotaKoperasi.getId());

		}

		tipeAnggotaKoperasi.setKode(kode.getValue());
		tipeAnggotaKoperasi.setNama(nama.getValue());
		tipeAnggotaKoperasi.setKeterangan(keterangan.getValue());
		tipeAnggotaKoperasi.setWajibHp(Boolean.valueOf(wajibHp.isChecked()));
		tipeAnggotaKoperasi.setWajibEmail(Boolean.valueOf(wajibEmail.isChecked()));
		tipeAnggotaKoperasi.setMaksimalBolehUtang(Double.valueOf(maksimalBolehUtang.getValue().doubleValue()));
		tipeAnggotaKoperasi.setMaksimalTransaksiHarian(Double.valueOf(maksimalHarian.getValue().doubleValue()));
		tipeAnggotaKoperasi.setMaksimalTransaksiMingguan(Double.valueOf(maksimalMingguan.getValue().doubleValue()));
		tipeAnggotaKoperasi.setMaksimalTransaksiBulanan(Double.valueOf(maksimalBulanan.getValue().doubleValue()));
		StringBuilder csvToko = new StringBuilder();
		for (Map.Entry<Checkbox, Toko> entry : pilihanToko.entrySet()) {
			if (entry.getKey().isChecked()) csvToko.append(",").append(entry.getValue().getId());
		}
		if (csvToko.length() > 0) csvToko.append(",");
		if (!berlakuSemuaToko.isChecked() && csvToko.length() == 0) {
			MyMessageboxConfig.show("Pilih minimal satu toko atau aktifkan Berlaku ke semua toko.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		tipeAnggotaKoperasi.setBerlakuSemuaToko(Boolean.valueOf(berlakuSemuaToko.isChecked()));
		tipeAnggotaKoperasi.setDaftarToko(berlakuSemuaToko.isChecked() ? "" : csvToko.toString());
		StringBuilder csvCara = new StringBuilder();
		for (Map.Entry<Checkbox, CaraPembayaranKoperasi> entry : pilihanCaraBayar.entrySet()) {
			if (entry.getKey().isChecked()) csvCara.append(",").append(entry.getValue().getId());
		}
		if (csvCara.length() > 0) csvCara.append(",");
		tipeAnggotaKoperasi.setDaftarCaraPembayaranYangBolehDiPilih(csvCara.toString());
		Long idDefault = caraBayarDefault.getSelectedItem() == null ? null : (Long) caraBayarDefault.getSelectedItem().getValue();
		if (idDefault != null && !csvCara.toString().contains("," + idDefault + ",")) {
			MyMessageboxConfig.show("Cara bayar default harus ikut dicentang sebagai cara bayar yang diizinkan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kunciCaraBayar.isChecked() && idDefault == null) {
			MyMessageboxConfig.show("Pilih cara bayar default sebelum mengunci cara bayar lain.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		tipeAnggotaKoperasi.setCaraPembayaranDefaultId(idDefault);
		tipeAnggotaKoperasi.setTidakBolehCaraPembayaranLain(Boolean.valueOf(kunciCaraBayar.isChecked()));
		tipeAnggotaKoperasi.setWajibPin(Boolean.valueOf(wajibPin.isChecked()));
		StringBuilder csvWajibPin = new StringBuilder();
		for (Map.Entry<Checkbox, CaraPembayaranKoperasi> entry : pilihanCaraBayarWajibPin.entrySet()) {
			if (entry.getKey().isChecked()) csvWajibPin.append(",").append(entry.getValue().getId());
		}
		if (csvWajibPin.length() > 0) csvWajibPin.append(",");
		if (csvCara.length() > 0) {
			for (Map.Entry<Checkbox, CaraPembayaranKoperasi> entry : pilihanCaraBayarWajibPin.entrySet()) {
				if (entry.getKey().isChecked() && !csvCara.toString().contains("," + entry.getValue().getId() + ",")) {
					MyMessageboxConfig.show("Cara bayar wajib PIN harus termasuk cara bayar yang diizinkan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			}
		}
		tipeAnggotaKoperasi.setDaftarCaraPembayaranWajibPin(csvWajibPin.toString());
		tipeAnggotaKoperasi.setWajibVerifikasiBiometricWajah(Boolean.valueOf(wajibWajah.isChecked()));
		tipeAnggotaKoperasi.setWajibVerifikasiBiometricFingerprint(Boolean.valueOf(wajibFingerprint.isChecked()));

		Common.refreshSaveOrUpdate(session, tipeAnggotaKoperasi);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TipeAnggotaKoperasi.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<TipeAnggotaKoperasi> tipeAnggotaKoperasi = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
				TipeAnggotaKoperasi.class);
		ListModel strset = new SimpleListModel(tipeAnggotaKoperasi);
		grid.setRowRenderer(new TipeAnggotaKoperasiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaTipeAnggotaKoperasi() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(TipeAnggotaKoperasi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.tipeAnggotaKoperasi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.tipeAnggotaKoperasi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
