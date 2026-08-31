package ais.action.master;

import java.util.List;
import java.util.TreeMap;

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
import org.zkoss.zul.Intbox;
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
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.UjianPMBDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.PerguruanTinggi;
import ais.database.model.UjianPMB;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk ujian pmb. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchTahunAjaran}, {@code Textbox
 * nama}, {@code Textbox lokasi}, {@code Intbox jumlahHariUjian}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}, {@code init()});
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
public class UjianPMBAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267902900328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchTahunAjaran;

	private Textbox nama;
	private Textbox lokasi;
	private Intbox jumlahHariUjian;
	private MyDatebox tanggalUjian1;
	private MyDatebox tanggalUjian2;
	private MyDatebox tanggalUjian3;
	private MyDatebox tanggalUjian4;
	private MyDatebox tanggalUjian5;
	private MyDatebox tanggalUjian6;
	private MyDatebox tanggalUjian7;
	private MyDatebox tanggalUjian8;
	private MyDatebox tanggalUjian9;
	private MyDatebox tanggalUjian10;
	private MyCheckboxConfig tampilkanJadwalUjianDiKartuUjian;
	private PerguruanTinggi selectedPerguruanTinggi;
//	private Combobox tahunAkademik;
	private Textbox keterangan;
	private Textbox keteranganSetelahBayar;

	// private Textbox keteranganHeader;
	// private Textbox keteranganSetelahBayarHeader;

	private boolean edit = false;
	private boolean delete = false;

	private UjianPMB ujianPMB;
	private MyToolbarbuttonConfig add;
	private Row row2;
	private Row row3;
	private Row row9;
	private Row row5;
	private Row row6;
	private Row row7;
	private Row row8;
	private Row row4;
	private Row row10;
	private Combobox gelombangPendaftaran;

	private Checkbox searchaktif;

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
		selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		if (searchTahunAjaran != null) { searchTahunAjaran.setReadonly(true); }
		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());
		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);

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
	}

	class UjianPMBRenderer extends ais.ui.util.MyRowRenderer {

		private Label buatInformasiRingkas(String nilai) {
			String informasi = nilai == null ? "" : nilai.trim();
			MyLabelKecil label = new MyLabelKecil(informasi);
			label.setMaxlength(140);
			label.setTooltiptext(informasi.length() == 0 ? "Tidak ada informasi" : informasi);
			label.setStyle("font-size:10px;display:block;white-space:normal;line-height:16px;"
					+ "max-height:64px;overflow:hidden;word-wrap:break-word;cursor:help;");
			return label;
		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			arg0.setValign("top");
			final UjianPMB ujianPMB = (UjianPMB) arg1;

			RevisiHelper.createNewRevisi(UjianPMB.class, ujianPMB, ujianPMB.getNama()).setParent(arg0);

			new Label(ujianPMB.getTahunAkademik()).setParent(arg0);
			new MyLabelAgakKecil(
					ujianPMB.getGelombangPendaftaran() == null ? "" : ujianPMB.getGelombangPendaftaran().toString())
					.setParent(arg0);
			new Label(ujianPMB.getJumlahHariUjian().toString()).setParent(arg0);

			String dosenPengampu = "<font style='font-size:9px;'>" + "<ol>";
			if (ujianPMB.getTanggalUjian1() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPMB.getTanggalUjian1()) + "</li>";
			}
			if (ujianPMB.getTanggalUjian2() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPMB.getTanggalUjian2()) + "</li>";
			}
			if (ujianPMB.getTanggalUjian3() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPMB.getTanggalUjian3()) + "</li>";
			}
			if (ujianPMB.getTanggalUjian4() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPMB.getTanggalUjian4()) + "</li>";
			}
			if (ujianPMB.getTanggalUjian5() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPMB.getTanggalUjian5()) + "</li>";
			}
			if (ujianPMB.getTanggalUjian6() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPMB.getTanggalUjian6()) + "</li>";
			}
			if (ujianPMB.getTanggalUjian7() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPMB.getTanggalUjian7()) + "</li>";
			}
			if (ujianPMB.getTanggalUjian8() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPMB.getTanggalUjian8()) + "</li>";
			}
			if (ujianPMB.getTanggalUjian9() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPMB.getTanggalUjian9()) + "</li>";
			}
			if (ujianPMB.getTanggalUjian10() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPMB.getTanggalUjian10()) + "</li>";
			}
			dosenPengampu += "</ol>" + "</font>";

			new ais.ui.util.MyHtml(dosenPengampu).setParent(arg0);

			new Label(ujianPMB.getLokasi()).setParent(arg0);
			new Label(ujianPMB.getTampilkanJadwalUjianDiKartuUjian() ? "Ya" : "Tidak").setParent(arg0);
			buatInformasiRingkas(ujianPMB.getKeterangan()).setParent(arg0);
			buatInformasiRingkas(ujianPMB.getKeteranganSetelahBayar()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(ujianPMB.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					ujianPMB.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(ujianPMB);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, ujianPMB, UjianPMBAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new UjianPMB());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(UjianPMB ujianPMB) throws Exception {
		this.ujianPMB = ujianPMB;
		addWindow.setTitle(ujianPMB.getId() == null ? "Tambah Ujian PMB" : "Ubah Ujian PMB");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ujian PMB *"));
		row.appendChild(nama = new Textbox(ujianPMB.getNama() == null ? "" : ujianPMB.getNama()));
		nama.setWidth("90%");

		Common.initKeterangan(rows, "Jika tidak ada ujian, masukkan nama Tanpa Ujian");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi Ujian PMB"));
		row.appendChild(lokasi = new Textbox(ujianPMB.getLokasi()));
		lokasi.setWidth("90%");

//		row = new MyFormRow();
////		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
//		row.appendChild(tahunAkademik = new Combobox());
//		Common.generateTahunAjaranDanSemua(tahunAkademik);
//		Common.selectComboItem(tahunAkademik, ujianPMB.getTahunAkademik());
//		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang Pendaftaran *"));
		row.appendChild(gelombangPendaftaran = new Combobox());
		gelombangPendaftaran.setWidth("90%");

		EventListener gelombangEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.insertCombo(gelombangPendaftaran, new String[] { "nama", "mulai", "sampai", "jenisSeleksi" },
						"tahunAkademik", GelombangPendaftaran.class,
						selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
										Restrictions.isNull("perguruanTinggi")),
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								searchTahunAjaran.getSelectedItem() == null
										|| searchTahunAjaran.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunAkademik",
														searchTahunAjaran.getSelectedItem().getValue())));
			}
		};

		gelombangEventListener.onEvent(null);
//		tahunAkademik.addEventListener("onChange", gelombangEventListener);

		Common.selectComboItem(gelombangPendaftaran, ujianPMB.getGelombangPendaftaran());
		gelombangPendaftaran.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Hari Ujian"));
		row.appendChild(jumlahHariUjian = new Intbox(ujianPMB.getJumlahHariUjian()));
		jumlahHariUjian.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampilkan Info Ujian Di Kartu Peserta"));
		row.appendChild(tampilkanJadwalUjianDiKartuUjian = new MyCheckboxConfig());
		tampilkanJadwalUjianDiKartuUjian.setChecked(ujianPMB.getTampilkanJadwalUjianDiKartuUjian());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Ujian"));
		row.appendChild(tanggalUjian1 = new MyDatebox(ujianPMB.getTanggalUjian1()));
		tanggalUjian1.setWidth("90%");

		row2 = new MyFormRow();
		row2.setStyle("border:0px;background: transparent;");
		row2.setParent(rows);
		row2.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 2")));
		row2.appendChild(tanggalUjian2 = new MyDatebox(ujianPMB.getTanggalUjian2()));
		tanggalUjian2.setWidth("90%");

		row3 = new MyFormRow();
		row3.setStyle("border:0px;background: transparent;");
		row3.setParent(rows);
		row3.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 3")));
		row3.appendChild(tanggalUjian3 = new MyDatebox(ujianPMB.getTanggalUjian3()));
		tanggalUjian3.setWidth("90%");

		row4 = new MyFormRow();
		row4.setStyle("border:0px;background: transparent;");
		row4.setParent(rows);
		row4.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 4")));
		row4.appendChild(tanggalUjian4 = new MyDatebox(ujianPMB.getTanggalUjian4()));
		tanggalUjian4.setWidth("90%");

		row5 = new MyFormRow();
		row5.setStyle("border:0px;background: transparent;");
		row5.setParent(rows);
		row5.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 5")));
		row5.appendChild(tanggalUjian5 = new MyDatebox(ujianPMB.getTanggalUjian5()));
		tanggalUjian5.setWidth("90%");

		row6 = new MyFormRow();
		row6.setStyle("border:0px;background: transparent;");
		row6.setParent(rows);
		row6.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 6")));
		row6.appendChild(tanggalUjian6 = new MyDatebox(ujianPMB.getTanggalUjian6()));
		tanggalUjian6.setWidth("90%");

		row7 = new MyFormRow();
		row7.setStyle("border:0px;background: transparent;");
		row7.setParent(rows);
		row7.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 7")));
		row7.appendChild(tanggalUjian7 = new MyDatebox(ujianPMB.getTanggalUjian7()));
		tanggalUjian7.setWidth("90%");

		row8 = new MyFormRow();
		row8.setStyle("border:0px;background: transparent;");
		row8.setParent(rows);
		row8.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 8")));
		row8.appendChild(tanggalUjian8 = new MyDatebox(ujianPMB.getTanggalUjian8()));
		tanggalUjian8.setWidth("90%");

		row9 = new MyFormRow();
		row9.setStyle("border:0px;background: transparent;");
		row9.setParent(rows);
		row9.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 9")));
		row9.appendChild(tanggalUjian9 = new MyDatebox(ujianPMB.getTanggalUjian9()));
		tanggalUjian9.setWidth("90%");

		row10 = new MyFormRow();
		row10.setStyle("border:0px;background: transparent;");
		row10.setParent(rows);
		row10.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 10")));
		row10.appendChild(tanggalUjian10 = new MyDatebox(ujianPMB.getTanggalUjian10()));
		tanggalUjian10.setWidth("90%");

		EventListener rowEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				int jumlahUjian = jumlahHariUjian.getValue() == null ? 1 : jumlahHariUjian.getValue();
				jumlahHariUjian.setValue(jumlahUjian);
				row2.setVisible(jumlahUjian > 1);
				row3.setVisible(jumlahUjian > 2);
				row4.setVisible(jumlahUjian > 3);
				row5.setVisible(jumlahUjian > 4);
				row6.setVisible(jumlahUjian > 5);
				row7.setVisible(jumlahUjian > 6);
				row8.setVisible(jumlahUjian > 7);
				row9.setVisible(jumlahUjian > 8);
				row10.setVisible(jumlahUjian > 9);
			}
		};

		jumlahHariUjian.addEventListener("onChange", rowEventListener);
		rowEventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Informasi ke peserta ujian pada kartu pembayaran"));
		row.appendChild(keterangan = new Textbox(ujianPMB.getKeterangan() == null ? "" : ujianPMB.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(6);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Informasi ke peserta ujian pada kartu Ujian"));
		row.appendChild(keteranganSetelahBayar = new Textbox(ujianPMB.getKeteranganSetelahBayar()));
		keteranganSetelahBayar.setWidth("90%");
		keteranganSetelahBayar.setRows(6);

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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Ujian PMB",
					"Kolom Nama Ujian PMB belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Ujian PMB.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
//		if (lokasi.getValue().trim().equals("")) {
//			MyMessageboxConfig.show("Lokasi Ujian PMB harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}

//		if (tahunAkademik.getSelectedItem() == null) {
//			MyMessageboxConfig.show("Tahun Akademik Ujian PMB harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}

		if (gelombangPendaftaran.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Gelombang Pendaftaran Ujian PMB",
					"Kolom Gelombang Pendaftaran Ujian PMB belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Gelombang Pendaftaran Ujian PMB.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
//
//		boolean i = checkNamaUjianPMB();
//		if (i) {
//			MyMessageboxConfig.show("Nama Ujian PMB sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}

		UjianPMBDao ujianPMBDao = DaoFactory.getInstance().getUjianPMBDao();
		if (ujianPMB.getId() != null) {
			ujianPMB = ujianPMBDao.load(ujianPMB.getId());

		}

		ujianPMB.setKeteranganSetelahBayar(keteranganSetelahBayar.getValue());
		ujianPMB.setTampilkanJadwalUjianDiKartuUjian(tampilkanJadwalUjianDiKartuUjian.isChecked());
		ujianPMB.setNama(nama.getValue());
		ujianPMB.setLokasi(lokasi.getValue());
		ujianPMB.setKeterangan(keterangan.getValue());

		ujianPMB.setJumlahHariUjian(jumlahHariUjian.getValue());
//		ujianPMB.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		ujianPMB.setTanggalUjian1(tanggalUjian1.getValue());
		ujianPMB.setTanggalUjian2(tanggalUjian2.getValue());
		ujianPMB.setTanggalUjian3(tanggalUjian3.getValue());
		ujianPMB.setTanggalUjian4(tanggalUjian4.getValue());
		ujianPMB.setTanggalUjian5(tanggalUjian5.getValue());
		ujianPMB.setTanggalUjian6(tanggalUjian6.getValue());
		ujianPMB.setTanggalUjian7(tanggalUjian7.getValue());
		ujianPMB.setTanggalUjian8(tanggalUjian8.getValue());
		ujianPMB.setTanggalUjian9(tanggalUjian9.getValue());
		ujianPMB.setTanggalUjian10(tanggalUjian10.getValue());
		ujianPMB.setGelombangPendaftaran((GelombangPendaftaran) gelombangPendaftaran.getSelectedItem().getValue());

		if (ujianPMB.getId() != null) {
			ujianPMBDao.update(ujianPMB);
		} else {
			ujianPMBDao.save(ujianPMB);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(UjianPMB.class)

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.createAlias("gelombangPendaftaran", "gelombangPendaftaran")
				.add(selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.eq("gelombangPendaftaran.perguruanTinggi", selectedPerguruanTinggi),
								Restrictions.isNull("gelombangPendaftaran.perguruanTinggi")));

		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("gelombangPendaftaran.tahunAkademik",
										searchTahunAjaran.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<UjianPMB> ujianPMBs = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		TreeMap<Long, UjianPMB> a = new TreeMap<Long, UjianPMB>();
		for (UjianPMB ujianPMB : ujianPMBs) {
			a.put(ujianPMB.getId(), ujianPMB);
		}

		ListModel strset = new SimpleListModel(a.values().toArray(new UjianPMB[] {}));
		grid.setRowRenderer(new UjianPMBRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		ujianPMB = (UjianPMB) obj;
		init(ujianPMB);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
