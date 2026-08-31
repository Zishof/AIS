package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
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
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.DetailGrupSoalHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.PenjelasanBankSoal;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk penjelasan bank soal. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code Grid grid}, {@code Textbox searchketerangan}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code Combobox searchyayasan}, {@code Combobox searchsekolah}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code initMain()}, {@code init()}, {@code initCriteria()},
 * {@code init()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi
 * domain lain ({@code onAddExternal()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
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
public class PenjelasanBankSoalAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private Grid grid;

	private Textbox searchketerangan;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private AmbilDataDosenBanbox searchdosen;
	private AmbilDataGuruBanbox searchguru;

	private Textbox nama;
	private MyCkEditor keterangan;
	private Combobox fakultas;
	private Combobox jurusan;

	private Combobox yayasan;
	private Combobox sekolah;
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	private AmbilDataDosenBanbox dosen;
	private AmbilDataGuruBanbox guru;

	private boolean edit = false;
	private boolean delete = false;

	private PenjelasanBankSoal penjelasanBankSoal;
	private MyToolbarbuttonConfig add;

	private EventListener eventListener;

	private boolean pt = false;
	private boolean ya = false;
	private Row hbFakultasLabel;
	private Row hbYayasan;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private Tbmuser tbmuser = null;
	private Combobox jenisKoreksi;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (searchguru != null) {
			searchguru.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});
		}

		if (searchparent != null) {
			searchparent.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});
		}
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);
		if (hbFakultasLabel != null)
			if (hbFakultasLabel != null) { hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1); }

		if (hbYayasan != null)
			if (hbYayasan != null) { hbYayasan.setVisible(ya); }

		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

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

		String[] contents = new String[] { "id", "nama", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PenjelasanBankSoal.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PenjelasanBankSoal.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	class PenjelasanBankSoalRenderer extends ais.ui.util.MyRowRenderer {

		private DetailGrupSoalHelper detailGrupSoalHelper = new DetailGrupSoalHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenjelasanBankSoal penjelasanBankSoal = (PenjelasanBankSoal) arg1;

			if (tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa()==null && tbmuser.getSiswa() == null
					&& tbmuser.getCalonSiswa() == null) {
				final MyDetail detail = new MyDetail();
				detail.setParent(arg0);

				detail.addEventListener("onOpen", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.clear(detail);
						if (detail.isOpen()) {

							detailGrupSoalHelper.display(penjelasanBankSoal, detail);
						}

					}
				});
			} else {
				new Label().setParent(arg0);
			}

			RevisiHelper.createNewRevisi(PenjelasanBankSoal.class, penjelasanBankSoal, penjelasanBankSoal.getNama())
					.setParent(arg0);

			new Label(penjelasanBankSoal.getJenisKoreksi()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(penjelasanBankSoal.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					penjelasanBankSoal.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(penjelasanBankSoal);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, penjelasanBankSoal, PenjelasanBankSoalAction.this)
					.setParent(arg0);
		}

	}

	public static void onAddExternal(Event event, EventListener eventListener, PenjelasanBankSoal penjelasanBankSoal)
			throws Exception {

		PenjelasanBankSoalAction penjelasanBankSoalAction = new PenjelasanBankSoalAction();
		penjelasanBankSoalAction.eventListener = eventListener;
		penjelasanBankSoalAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(penjelasanBankSoalAction.addWindow);
		penjelasanBankSoalAction.addWindow.setHeight("95%");
		penjelasanBankSoalAction.addWindow.setWidth("90%");
		penjelasanBankSoalAction.init(penjelasanBankSoal);

		penjelasanBankSoalAction.addWindow.setVisible(true);
		penjelasanBankSoalAction.addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new PenjelasanBankSoal());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private Borderlayout initMain(PenjelasanBankSoal penjelasanBankSoal) throws Exception {
		Sekolah sekolah1 = SekolahUtil.getSekolah();

		tbmuser = Common.getCurrentUser();

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];
		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, searchfakultas, searchjurusan);
		Tbmuser tbmuser = Common.getCurrentUser();

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		West west = new West();
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("75%");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// column.setWidth("90%");

		Borderlayout myborderlayout = new ais.ui.util.MyBorderlayout();
		myborderlayout.setParent(west);

		Center mycenter = new Center();
		mycenter.setParent(myborderlayout);
		ais.ui.util.ZkCompat.setFlex(mycenter, true);
		mycenter.setBorder("none");

		Borderlayout myborderlayout22 = new ais.ui.util.MyBorderlayout();
		myborderlayout22.setParent(mycenter);

		mycenter = new Center();
		mycenter.setParent(myborderlayout22);
		ais.ui.util.ZkCompat.setFlex(mycenter, true);
		mycenter.setBorder("none");

		Grid gridSoal = new MyGrid();
		gridSoal.setWidth("100%");
		gridSoal.setParent(mycenter);
		gridSoal.setWidth("100%");
		gridSoal.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(gridSoal);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		if (penjelasanBankSoal != null && penjelasanBankSoal.getId() != null) {
			RevisiHelper.createNewRevisi(PenjelasanBankSoal.class, penjelasanBankSoal, "Judul Grup Soal")
					.setParent(row);
		} else {
			row.appendChild(new MyLabelBoldConfig("Judul Grup Soal"));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(nama = new MyTextbox(penjelasanBankSoal.getNama()));
		nama.setWidth("99%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);

		if (penjelasanBankSoal != null && penjelasanBankSoal.getId() != null) {
			RevisiHelper.createNewRevisi(PenjelasanBankSoal.class, penjelasanBankSoal, "Penjelasan Soal")
					.setParent(row);
		} else {
			row.appendChild(new MyLabelBoldConfig("Penjelasan Soal"));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(keterangan = new MyCkEditor());

		keterangan.setValue(penjelasanBankSoal.getKeterangan());
		keterangan.setWidth("99%");
		keterangan.setHeight("300px");

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);

		rows = new Rows();
		rows.setParent(grid);

		jenisKoreksi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(PenjelasanBankSoal.KOREKSI_OTOMATIS);
		comboitem.setValue(PenjelasanBankSoal.KOREKSI_OTOMATIS);
		jenisKoreksi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(PenjelasanBankSoal.KOREKSI_MANUAL);
		comboitem.setValue(PenjelasanBankSoal.KOREKSI_MANUAL);
		jenisKoreksi.appendChild(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis *"));

		Common.selectComboItem(jenisKoreksi, penjelasanBankSoal.getJenisKoreksi());
		row.appendChild(jenisKoreksi);
		jenisKoreksi.setWidth("90%");
		jenisKoreksi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(
				penjelasanBankSoal.getSatuanKerja() == null ? "" : penjelasanBankSoal.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", penjelasanBankSoal.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(fakultas,
				penjelasanBankSoal.getFakultas() == null || penjelasanBankSoal.getFakultas() == null
						? Common.getCurrentUser().ambilFakultas()
						: penjelasanBankSoal.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.clear(jurusan);
		Common.pilihJurusan(jurusan, penjelasanBankSoal.getJurusan() == null ? Common.getCurrentUser().ambilJurusan()
				: penjelasanBankSoal.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(null, null, yayasan, sekolah);

		if (sekolah1 != null && sekolah1.getId() != null) {
			penjelasanBankSoal.setSekolah(sekolah1);
		}

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		Common.selectComboItem(yayasan,
				penjelasanBankSoal.getYayasan() == null || penjelasanBankSoal.getYayasan() == null
						? Common.getCurrentUser().ambilYayasan()
						: penjelasanBankSoal.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		Common.pilihSekolah(sekolah, penjelasanBankSoal.getSekolah() == null ? Common.getCurrentUser().ambilSekolah()
				: penjelasanBankSoal.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembuat"));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setValue(penjelasanBankSoal.getDosen() == null ? "" : penjelasanBankSoal.getDosen().getNama());
		dosen.setAttribute("myValue", penjelasanBankSoal.getDosen());
		dosen.setWidth("90%");

		if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen mydosen = tbmuser.ambilDosen();
			dosen.setValue(mydosen.getNama());
			dosen.setAttribute("myValue", mydosen);
			dosen.setDisabled(true);
		}

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru Pembuat"));
		row.appendChild(guru = new AmbilDataGuruBanbox());
		guru.setValue(penjelasanBankSoal.getGuru() == null ? "" : penjelasanBankSoal.getGuru().getNama());
		guru.setAttribute("myValue", penjelasanBankSoal.getGuru());
		guru.setWidth("90%");

		if (tbmuser != null && tbmuser.ambilGuru() != null) {
			Guru mydosen = tbmuser.ambilGuru();
			guru.setValue(mydosen.getNama());
			guru.setAttribute("myValue", mydosen);
			guru.setDisabled(true);
		}

		return borderlayout;
	}

	private void init(final PenjelasanBankSoal penjelasanBankSoal) throws Exception {
		this.penjelasanBankSoal = penjelasanBankSoal;
		addWindow.setTitle(penjelasanBankSoal.getId() == null ? "Tambah Grup Soal" : "Ubah Grup Soal");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		center.appendChild(initMain(penjelasanBankSoal));

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

					if (eventListener != null) {
						eventListener
								.onEvent(new Event("", addWindow, PenjelasanBankSoalAction.this.penjelasanBankSoal));
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Judul grup soal",
					"Kolom Judul grup soal belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Judul grup soal.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisKoreksi.getSelectedItem() == null || jenisKoreksi.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data JenisKoreksi grup soal",
					"Kolom JenisKoreksi grup soal belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu JenisKoreksi grup soal.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (keterangan.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Penjelasan grup soal",
					"Kolom Penjelasan grup soal belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Penjelasan grup soal.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (penjelasanBankSoal.getId() != null) {
			penjelasanBankSoal = (PenjelasanBankSoal) session.load(PenjelasanBankSoal.class,
					penjelasanBankSoal.getId());

		}

		penjelasanBankSoal.setGuru((Guru) guru.getAttribute("myValue"));
		penjelasanBankSoal.setDosen((Dosen) dosen.getAttribute("myValue"));
		penjelasanBankSoal.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		penjelasanBankSoal.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));

		penjelasanBankSoal.setKeterangan(keterangan.getValue());

		penjelasanBankSoal.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null ? null
						: yayasan.getSelectedItem().getValue()));
		penjelasanBankSoal.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null ? null
						: sekolah.getSelectedItem().getValue()));

		penjelasanBankSoal.setJenisKoreksi((String) jenisKoreksi.getSelectedItem().getValue());
		penjelasanBankSoal.setNama(nama.getValue());

		if (penjelasanBankSoal.getId() != null) {
			Common.refreshUpdate(session, penjelasanBankSoal);
		} else {
			session.save(penjelasanBankSoal);
			session.flush();
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = searchparent == null ? null : (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (satuanKerjas == null) {
			satuanKerjas = new java.util.HashSet<SatuanKerja>();
		}
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			if (satuanKerjaTreeModel != null) {
				satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
			}
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenjelasanBankSoal.class)

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas))))

		;
		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria

				.add((searchdosen == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("dosen", searchdosen.getAttribute("myValue"))))

				.add((searchguru == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchguru.getAttribute("guru") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("guru", searchguru.getAttribute("guru"))))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchdosen == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);
		List<PenjelasanBankSoal> penjelasanBankSoal = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penjelasanBankSoal);
		grid.setRowRenderer(new PenjelasanBankSoalRenderer());
		grid.setModel(strset);

	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		PenjelasanBankSoal penjelasanBankSoal = (PenjelasanBankSoal) obj;
		init(penjelasanBankSoal);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
