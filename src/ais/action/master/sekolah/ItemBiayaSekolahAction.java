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

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.helper.AmbilDataParameterTambahanBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.ParameterTambahan;
import ais.database.model.akunting.Akun;
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
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk item biaya sekolah. Tipe ini merupakan titik masuk UI yang
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
public class ItemBiayaSekolahAction extends GenericAutowireComposer
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

	private ItemBiayaSekolah itemBiayaSekolah;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Textbox kode;
	private AmbilDataAkunBanbox akun;
	private AmbilDataAkunBanbox akunPiutang;
	private AmbilDataAkunBanbox akunDibayarDimuka;
	private AmbilDataAkunBanbox akunDenda;
	private AmbilDataAkunBanbox akunPiutangDenda;
	private AmbilDataAkunBanbox akunDiskon;
	private Decimalbox khususBulan;
	private Combobox harusBayar;
	private Combobox induk;
	private AmbilDataAkunBanbox akunUtangDiskon;
	private Combobox kelamin;

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
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "nama", "akun", "akunPiutang", "akunDibayarDimuka", "akunDenda",
				"akunDiskon", "nilaiBiayaBisaDiubahSaatPembayaran", "sekolah", "khususBulan", "harusBayar", "induk",
				"keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ItemBiayaSekolah.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link ItemBiayaSekolahAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link ItemBiayaSekolahAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see ItemBiayaSekolahAction
	 */
	class ItemBiayaSekolahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ItemBiayaSekolah itemBiayaSekolah = (ItemBiayaSekolah) arg1;

			new Label(itemBiayaSekolah.getKode()).setParent(arg0);
			Vbox a;
			(a = RevisiHelper.createNewRevisi(ItemBiayaSekolah.class, itemBiayaSekolah, itemBiayaSekolah.getNama()))
					.setParent(arg0);

			a.appendChild(new MyLabelKecil(
					itemBiayaSekolah.getInduk() == null ? "" : "Induk: " + itemBiayaSekolah.getInduk().getNama()));

			a.appendChild(new MyLabelKecil(itemBiayaSekolah.getKelamin() == null ? "" : itemBiayaSekolah.getKelamin()));

			a.appendChild(new MyLabelKecil(
					itemBiayaSekolah.getSekolah() == null ? "" : itemBiayaSekolah.getSekolah().getNama()));

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelKecil(itemBiayaSekolah.getAkun() == null ? ""
					: itemBiayaSekolah.getAkun().getKode() + "-" + itemBiayaSekolah.getAkun().getNama())
					.setParent(vbox);

			new MyLabelKecil(itemBiayaSekolah.getAkunPiutang() == null ? ""
					: itemBiayaSekolah.getAkunPiutang().getKode() + "-" + itemBiayaSekolah.getAkunPiutang().getNama())
					.setParent(vbox);

			new MyLabelKecil(itemBiayaSekolah.getAkunDibayarDimuka() == null ? ""
					: itemBiayaSekolah.getAkunDibayarDimuka().getKode() + "-"
							+ itemBiayaSekolah.getAkunDibayarDimuka().getNama())
					.setParent(vbox);

			new MyLabelKecil(itemBiayaSekolah.getAkunDenda() == null ? ""
					: itemBiayaSekolah.getAkunDenda().getKode() + "-" + itemBiayaSekolah.getAkunDenda().getNama())
					.setParent(vbox);

			new MyLabelKecil(itemBiayaSekolah.getAkunPiutangDenda() == null ? ""
					: itemBiayaSekolah.getAkunPiutangDenda().getKode() + "-"
							+ itemBiayaSekolah.getAkunPiutangDenda().getNama())
					.setParent(vbox);

			new MyLabelKecil(itemBiayaSekolah.getAkunDiskon() == null ? ""
					: itemBiayaSekolah.getAkunDiskon().getKode() + "-" + itemBiayaSekolah.getAkunDiskon().getNama())
					.setParent(vbox);

			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(Tagihan.class)
					.add(Restrictions.isNotNull("pembayaranSiswaDetail")).createAlias("nominalBiaya", "nominalBiaya")
					.add(Restrictions.eq("nominalBiaya.itemBiayaSekolah", itemBiayaSekolah))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			boolean buttonDelete = delete;
			if (Common.bolehKonfigurasi("pembayaran_siswa_yang_sudah_dibayar_tidak_bisa_dihapus")) {
				buttonDelete = delete && count == 0;
			}

			new Label(itemBiayaSekolah.getKeterangan() + "(jml pemb. " + Common.numberFormat.get().format(count) + ")")
					.setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(itemBiayaSekolah.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					itemBiayaSekolah.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(itemBiayaSekolah);
				}
			});

			final MyCheckboxConfig nilaiBiayaBisaDiubahSaatPembayaran = new MyCheckboxConfig("Nilai Diubah");
			nilaiBiayaBisaDiubahSaatPembayaran.setDisabled(!edit);
			nilaiBiayaBisaDiubahSaatPembayaran.setChecked(itemBiayaSekolah.getNilaiBiayaBisaDiubahSaatPembayaran());
			nilaiBiayaBisaDiubahSaatPembayaran.setParent(arg0);
			nilaiBiayaBisaDiubahSaatPembayaran.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					itemBiayaSekolah
							.setNilaiBiayaBisaDiubahSaatPembayaran(nilaiBiayaBisaDiubahSaatPembayaran.isChecked());
					Common.refreshSaveOrUpdate(itemBiayaSekolah);
				}
			});

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);

			final MyCheckboxConfig bolehDiangsur = new MyCheckboxConfig("Boleh Diangsur");
			bolehDiangsur.setDisabled(!edit);
			bolehDiangsur.setChecked(itemBiayaSekolah.getBolehDiangsur());
			bolehDiangsur.setParent(vbox2);
			bolehDiangsur.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					itemBiayaSekolah.setBolehDiangsur(bolehDiangsur.isChecked());
					Common.refreshSaveOrUpdate(itemBiayaSekolah);
				}
			});

			final MyCheckboxConfig angsuranSeragam = new MyCheckboxConfig("Diangsur Seragam");
			angsuranSeragam.setDisabled(!edit);
			angsuranSeragam.setChecked(itemBiayaSekolah.getAngsuranSeragam());
			angsuranSeragam.setParent(vbox2);
			angsuranSeragam.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					itemBiayaSekolah.setAngsuranSeragam(angsuranSeragam.isChecked());
					Common.refreshSaveOrUpdate(itemBiayaSekolah);
				}
			});

			Vbox vb = new Vbox();
			vb.setParent(arg0);

			final AmbilDataParameterTambahanBanbox parameterTambahan = new AmbilDataParameterTambahanBanbox();
			parameterTambahan.setWidth("100px");
			parameterTambahan.setReadonly(true);
			parameterTambahan.setAttribute("parameterTambahan", itemBiayaSekolah.getParameterTambahan());
			parameterTambahan.setValue(itemBiayaSekolah.getParameterTambahan() == null ? ""
					: itemBiayaSekolah.getParameterTambahan().getNama());

			final MyCheckboxConfig terhubungKeNilaiTambahan = new MyCheckboxConfig("");
			terhubungKeNilaiTambahan.setChecked(itemBiayaSekolah.getTerhubungKeNilaiTambahan());
			terhubungKeNilaiTambahan.setParent(vb);
			parameterTambahan.setParent(vb);
			terhubungKeNilaiTambahan.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					itemBiayaSekolah.setTerhubungKeNilaiTambahan(terhubungKeNilaiTambahan.isChecked());
					Common.refreshSaveOrUpdate(itemBiayaSekolah);
					parameterTambahan.setVisible(itemBiayaSekolah.getTerhubungKeNilaiTambahan());
				}
			});
			parameterTambahan.setVisible(itemBiayaSekolah.getTerhubungKeNilaiTambahan());
			parameterTambahan.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					itemBiayaSekolah.setParameterTambahan(
							(ParameterTambahan) (parameterTambahan.getAttribute("parameterTambahan")));
					Common.refreshSaveOrUpdate(itemBiayaSekolah);
				}
			});

			vb = new Vbox();
			vb.setParent(arg0);
			final MyCheckboxConfig wajibPilih = new MyCheckboxConfig("Wajib Dipilih");
			wajibPilih.setDisabled(!edit);
			wajibPilih.setChecked(itemBiayaSekolah.getWajibPilih());
			wajibPilih.setParent(vb);
			wajibPilih.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					itemBiayaSekolah.setWajibPilih(wajibPilih.isChecked());
					Common.refreshSaveOrUpdate(itemBiayaSekolah);
				}
			});

			final MyCheckboxConfig wajibPilihJikaBulanDipilih = new MyCheckboxConfig("Wajib Berdasarkan Bulan");
			wajibPilihJikaBulanDipilih.setDisabled(!edit);
			wajibPilihJikaBulanDipilih.setChecked(itemBiayaSekolah.getWajibPilihJikaBulanDipilih());
			wajibPilihJikaBulanDipilih.setParent(vb);
			wajibPilihJikaBulanDipilih.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					itemBiayaSekolah.setWajibPilihJikaBulanDipilih(wajibPilihJikaBulanDipilih.isChecked());
					Common.refreshSaveOrUpdate(itemBiayaSekolah);
				}
			});

			Common.copyEditDeleteButtons(edit, buttonDelete, itemBiayaSekolah, ItemBiayaSekolahAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new ItemBiayaSekolah());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		itemBiayaSekolah = (ItemBiayaSekolah) obj;
		init(itemBiayaSekolah);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final ItemBiayaSekolah itemBiayaSekolah) {
		this.itemBiayaSekolah = itemBiayaSekolah;
		addWindow.setTitle(itemBiayaSekolah.getId() == null ? "Tambah Item Biaya" : "Ubah Item Biaya");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Item Biaya *"));
		row.appendChild(kode = new Textbox(itemBiayaSekolah.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Item Biaya *"));
		row.appendChild(nama = new Textbox(itemBiayaSekolah.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Pendapatan"));
		row.appendChild(akun = new AmbilDataAkunBanbox());
		akun.setValue(itemBiayaSekolah.getAkun() == null ? "" : itemBiayaSekolah.getAkun().getNama());
		akun.setAttribute("akun", itemBiayaSekolah.getAkun());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Piutang"));
		row.appendChild(akunPiutang = new AmbilDataAkunBanbox());
		akunPiutang.setValue(
				itemBiayaSekolah.getAkunPiutang() == null ? "" : itemBiayaSekolah.getAkunPiutang().toString());
		akunPiutang.setAttribute("akun", itemBiayaSekolah.getAkunPiutang());
		akunPiutang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Biaya Dibayar Dimuka"));
		row.appendChild(akunDibayarDimuka = new AmbilDataAkunBanbox());
		akunDibayarDimuka.setValue(itemBiayaSekolah.getAkunDibayarDimuka() == null ? ""
				: itemBiayaSekolah.getAkunDibayarDimuka().toString());
		akunDibayarDimuka.setAttribute("akun", itemBiayaSekolah.getAkunDibayarDimuka());
		akunDibayarDimuka.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Denda"));
		row.appendChild(akunDenda = new AmbilDataAkunBanbox());
		akunDenda.setValue(itemBiayaSekolah.getAkunDenda() == null ? "" : itemBiayaSekolah.getAkunDenda().toString());
		akunDenda.setAttribute("akun", itemBiayaSekolah.getAkunDenda());
		akunDenda.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Piutang Denda"));
		row.appendChild(akunPiutangDenda = new AmbilDataAkunBanbox());
		akunPiutangDenda.setValue(itemBiayaSekolah.getAkunPiutangDenda() == null ? ""
				: itemBiayaSekolah.getAkunPiutangDenda().toString());
		akunPiutangDenda.setAttribute("akun", itemBiayaSekolah.getAkunPiutangDenda());
		akunPiutangDenda.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Diskon"));
		row.appendChild(akunDiskon = new AmbilDataAkunBanbox());
		akunDiskon
				.setValue(itemBiayaSekolah.getAkunDiskon() == null ? "" : itemBiayaSekolah.getAkunDiskon().toString());
		akunDiskon.setAttribute("akun", itemBiayaSekolah.getAkunDiskon());
		akunDiskon.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Utang Diskon"));
		row.appendChild(akunUtangDiskon = new AmbilDataAkunBanbox());
		akunUtangDiskon.setValue(
				itemBiayaSekolah.getAkunUtangDiskon() == null ? "" : itemBiayaSekolah.getAkunUtangDiskon().toString());
		akunUtangDiskon.setAttribute("akun", itemBiayaSekolah.getAkunUtangDiskon());
		akunUtangDiskon.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, itemBiayaSekolah.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, itemBiayaSekolah.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		kelamin = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig();
		comboitem.setLabel("Laki-laki");
		comboitem.setValue("Laki-laki");
		kelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Perempuan");
		comboitem.setValue("Perempuan");
		kelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		kelamin.appendChild(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kelamin"));
		row.appendChild(kelamin);
		Common.selectComboItem(kelamin, itemBiayaSekolah.getKelamin());
		kelamin.setReadonly(true);
		kelamin.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Item Induk"));
		row.appendChild(induk = new Combobox());
		induk.setWidth("90%");
		induk.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Item yang wajib dibayar sebelum-nya"));
		row.appendChild(harusBayar = new Combobox());
		harusBayar.setWidth("90%");
		harusBayar.setReadonly(true);

		EventListener eventListenerSyarat = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				Common.insertComboDanSemua(harusBayar, new String[] { "kode", "nama" }, "keterangan",
						ItemBiayaSekolah.class, "Tanpa item lain",
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(harusBayar, itemBiayaSekolah.getHarusBayar());

				Common.insertComboDanSemua(induk, new String[] { "kode", "nama" }, "keterangan", ItemBiayaSekolah.class,
						"Tanpa Induk",
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(induk, itemBiayaSekolah.getInduk());
			}
		};
		sekolah.addEventListener("onChange", eventListenerSyarat);
		Common.createDefaultTimer(eventListenerSyarat);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Khusus Bulan"));
		row.appendChild(khususBulan = new Decimalbox(
				itemBiayaSekolah.getKhususBulan() == null ? null : new BigDecimal(itemBiayaSekolah.getKhususBulan())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(itemBiayaSekolah.getKeterangan()));
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
			MyMessageboxConfig.show("Nama Jenis Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
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
		if (itemBiayaSekolah.getId() != null) {
			itemBiayaSekolah = (ItemBiayaSekolah) session.load(ItemBiayaSekolah.class, itemBiayaSekolah.getId());
		}

		itemBiayaSekolah.setKode(kode.getValue());
		itemBiayaSekolah.setAkun((Akun) akun.getAttribute("akun"));
		itemBiayaSekolah.setAkunPiutang((Akun) akunPiutang.getAttribute("akun"));
		itemBiayaSekolah.setAkunDibayarDimuka((Akun) akunDibayarDimuka.getAttribute("akun"));
		itemBiayaSekolah.setAkunDenda((Akun) akunDenda.getAttribute("akun"));
		itemBiayaSekolah.setAkunPiutangDenda((Akun) akunPiutangDenda.getAttribute("akun"));
		itemBiayaSekolah.setAkunUtangDiskon((Akun) akunUtangDiskon.getAttribute("akun"));
		itemBiayaSekolah
				.setKelamin((String) (kelamin.getSelectedItem() == null ? null : kelamin.getSelectedItem().getValue()));
		itemBiayaSekolah.setAkunDiskon((Akun) akunDiskon.getAttribute("akun"));

		itemBiayaSekolah.setNama(nama.getValue());
		itemBiayaSekolah.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		itemBiayaSekolah.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		itemBiayaSekolah.setKeterangan(keterangan.getValue());

		itemBiayaSekolah.setHarusBayar((ItemBiayaSekolah) (harusBayar.getSelectedItem() == null ? null
				: harusBayar.getSelectedItem().getValue()));

		itemBiayaSekolah.setInduk(
				(ItemBiayaSekolah) (induk.getSelectedItem() == null ? null : induk.getSelectedItem().getValue()));

		itemBiayaSekolah.setKhususBulan(khususBulan.getValue() == null ? null : khususBulan.getValue().intValue());

		Common.refreshSaveOrUpdate(session, itemBiayaSekolah);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ItemBiayaSekolah.class)
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

		List<ItemBiayaSekolah> itemBiayaSekolah = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(itemBiayaSekolah);
		grid.setRowRenderer(new ItemBiayaSekolahRenderer());
		grid.setModelCheckMobile(strset);

	}

}
