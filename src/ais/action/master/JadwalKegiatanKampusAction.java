package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.JadwalKegiatanKampus;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk jadwal kegiatan kampus. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchjurusan}, {@code Combobox
 * searchprogram}, {@code Combobox searchfakultas}, {@code MyCkEditor nama}; inisialisasi/lifecycle ({@code
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
public class JadwalKegiatanKampusAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchjurusan;
	private Combobox searchprogram;
	private Combobox searchfakultas;

	private MyCkEditor nama;
	private Combobox jurusan;
	private Combobox fakultas;
	private Combobox program;

	private boolean edit = false;
	private boolean delete = false;

	private MyColumnConfig editCol;

	private JadwalKegiatanKampus jadwalKegiatanKampus;
	private MyToolbarbuttonConfig add;
	private MyDatebox tanggalMulai;
	private MyDatebox tanggalSelesai;
	private MyTimebox waktuMulai;
	private MyTimebox waktuSelesai;
	private MyTextbox tempat;
	private MyTextbox pelaksana;
	private MyTextbox peserta;
	private MyTextbox narasumber;
	private MyTextbox keterangan;

	private Tab pengaturan;

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

		boolean admin = Common.getApakahAdmin();
		Tbmuser tbmuser = Common.getCurrentUser();
		boolean adminLainBoleh = false;
		String admLain = Common.getKonfigurasi("kode_role_informasi_jadwal_kegiatan_di_halaman_depan", "").getNilai();
		String[] aa = admLain.split(";");
		for (String a : aa) {
			try {
				adminLainBoleh = a.trim().equalsIgnoreCase(tbmuser.hakAkses().getRoleId());
				if (adminLainBoleh) {
					break;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);

			}
		}
		if (pengaturan != null) { pengaturan.setVisible(admin || adminLainBoleh); }
		Common.initPrograms(searchprogram);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		if (editCol != null) { editCol.setVisible(admin || adminLainBoleh); }
		if (editCol != null) { editCol.setWidth(admin || adminLainBoleh ? "10%" : "0px"); }

		if (add != null) { add.setVisible(admin || adminLainBoleh); }
		if (add != null) { add.setTooltiptext("Tambah"); }

		edit = admin || adminLainBoleh;
		delete = admin || adminLainBoleh;
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "tanggalMulai", "tanggalSelesai", "waktuMulai", "waktuSelesai",
				"pelaksana", "peserta", "narasumber", "tempat", "nama", "fakultas", "jurusan", "program", "aktif",
				"keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JadwalKegiatanKampus.class, this, contents);
		if (cetakToolbarbutton != null) { cetakToolbarbutton.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JadwalKegiatanKampus.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig buttonFormatNilai = new MyToolbarbuttonConfig("Cetak Info Kegiatan", "/img/print.png");
		if (buttonFormatNilai != null) { buttonFormatNilai.setParent(add.getParent()); }
		buttonFormatNilai.addEventListener("onClick", new EventListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event event) throws Exception {
				PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
				Map parameters = ais.common.HashMapGenerator.getRand();
				parameters.put("nama_universitas", perguruanTinggi == null ? "" : perguruanTinggi.getNama());
				List<JadwalKegiatanKampus> jadwalKegiatanKampus = initCriteria(true).list();

				List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();

				for (JadwalKegiatanKampus kegiatanKampus : jadwalKegiatanKampus) {
					Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
					map.put("hari_tanggal", (kegiatanKampus.getTanggalMulai() == null ? ""
							: Common.dateFormat41.get().format(kegiatanKampus.getTanggalMulai()))
							+ (kegiatanKampus.getTanggalSelesai() == null ? ""
									: " s.d " + Common.dateFormat41.get().format(kegiatanKampus.getTanggalSelesai())));

					map.put("nama_kegiatan", kegiatanKampus.getNama());
					map.put("waktu",
							(kegiatanKampus.getWaktuMulai() == null ? ""
									: Common.timeFormat.get().format(kegiatanKampus.getWaktuMulai()))
									+ (kegiatanKampus.getWaktuSelesai() == null ? ""
											: " s.d " + Common.timeFormat.get().format(kegiatanKampus.getWaktuSelesai())));

					map.put("pelaksana", kegiatanKampus.getPelaksana());
					map.put("peserta", kegiatanKampus.getPeserta());
					map.put("narasumber", kegiatanKampus.getNarasumber());
					map.put("tempat", kegiatanKampus.getTempat());
					map.put("keterangan", kegiatanKampus.getKeterangan());
					maps.add(map);
				}

				parameters.put("maps", maps);
				Report.generatePDFReport("pdf", parameters, "jadwal_kegiatan", ais.ui.util.WaktuUtil.getDate(),
						Common.locale, null, null);

			}

		});
	}

	class JadwalKegiatanKampusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JadwalKegiatanKampus jadwalKegiatanKampus = (JadwalKegiatanKampus) arg1;

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			if (jadwalKegiatanKampus.getTanggalMulai() != null || jadwalKegiatanKampus.getTanggalSelesai() != null) {
				Hbox hbox = new Hbox();
				hbox.setParent(vbox);
				new MyLabelBoldAja("Tanggal Kegiatan:").setParent(hbox);
				new MyLabelBoldAja(jadwalKegiatanKampus.getTanggalMulai() == null ? ""
						: Common.dateFormat2.get().format(jadwalKegiatanKampus.getTanggalMulai())).setParent(hbox);
				new MyLabelBoldAja(jadwalKegiatanKampus.getTanggalSelesai() == null ? ""
						: " s.d " + Common.dateFormat2.get().format(jadwalKegiatanKampus.getTanggalSelesai()))
						.setParent(hbox);

				new MyLabelBoldAja(", Status: " + jadwalKegiatanKampus.ambilStatus()).setParent(hbox);
			}

			if (jadwalKegiatanKampus.getWaktuMulai() != null || jadwalKegiatanKampus.getWaktuSelesai() != null) {
				Hbox hbox = new Hbox();
				hbox.setParent(vbox);
				new MyLabelBoldAja("Waktu Kegiatan:").setParent(hbox);
				new MyLabelBoldAja(jadwalKegiatanKampus.getWaktuMulai() == null ? ""
						: Common.timeFormat.get().format(jadwalKegiatanKampus.getWaktuMulai())).setParent(hbox);
				new MyLabelBoldAja(jadwalKegiatanKampus.getWaktuSelesai() == null ? ""
						: " s.d " + Common.timeFormat.get().format(jadwalKegiatanKampus.getWaktuSelesai())).setParent(hbox);

				new MyLabelBoldAja(", Status: " + jadwalKegiatanKampus.ambilStatus()).setParent(hbox);
			}

			if (jadwalKegiatanKampus.getTempat() != null && !jadwalKegiatanKampus.getTempat().trim().isEmpty()) {
				new MyLabelBoldAja("Tempat Kegiatan:" + jadwalKegiatanKampus.getTempat()).setParent(vbox);
			}

			if (jadwalKegiatanKampus.getPelaksana() != null && !jadwalKegiatanKampus.getPelaksana().trim().isEmpty()) {
				new MyLabelBoldAja("Pelaksana Kegiatan:" + jadwalKegiatanKampus.getPelaksana()).setParent(vbox);
			}

			if (jadwalKegiatanKampus.getPeserta() != null && !jadwalKegiatanKampus.getPeserta().trim().isEmpty()) {
				new MyLabelBoldAja("Peserta Kegiatan:" + jadwalKegiatanKampus.getPeserta()).setParent(vbox);
			}

			if (jadwalKegiatanKampus.getNarasumber() != null
					&& !jadwalKegiatanKampus.getNarasumber().trim().isEmpty()) {
				new MyLabelBoldAja("Narasumber Kegiatan:" + jadwalKegiatanKampus.getNarasumber()).setParent(vbox);
			}

			if (jadwalKegiatanKampus.getKeterangan() != null
					&& !jadwalKegiatanKampus.getKeterangan().trim().isEmpty()) {
				new MyLabelBoldAja("Keterangan Kegiatan:" + jadwalKegiatanKampus.getKeterangan()).setParent(vbox);
			}

			new ais.ui.util.MyHtml(jadwalKegiatanKampus.getNama()).setParent(vbox);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jadwalKegiatanKampus.getAktif());
			checkbox.setParent(vbox);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jadwalKegiatanKampus.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jadwalKegiatanKampus);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jadwalKegiatanKampus, JadwalKegiatanKampusAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JadwalKegiatanKampus());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jadwalKegiatanKampus = (JadwalKegiatanKampus) obj;
		init(jadwalKegiatanKampus);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JadwalKegiatanKampus jadwalKegiatanKampus) {
		this.jadwalKegiatanKampus = jadwalKegiatanKampus;
		addWindow.setTitle(jadwalKegiatanKampus.getId() == null ? "Tambah Jadwal Kegiatan Kampus" : "Ubah Jadwal Kegiatan Kampus");
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelBoldConfig("Tanggal Kegiatan"));

		row = new MyFormRow();
		row.setParent(rows);

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(tanggalMulai = new MyDatebox(
				jadwalKegiatanKampus.getTanggalMulai() == null ? ais.ui.util.WaktuUtil.getDate()
						: jadwalKegiatanKampus.getTanggalMulai()));
		tanggalMulai.setFormat(Common.dateFormat1.get().toPattern());
		tanggalMulai.setCols(5);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));

		hbox.appendChild(tanggalSelesai = new MyDatebox(
				jadwalKegiatanKampus.getTanggalSelesai() == null ? ais.ui.util.WaktuUtil.getDate()
						: jadwalKegiatanKampus.getTanggalSelesai()));
		tanggalSelesai.setFormat(Common.dateFormat1.get().toPattern());
		tanggalSelesai.setCols(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelBoldConfig("Waktu Kegiatan"));

		row = new MyFormRow();
		row.setParent(rows);

		hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(waktuMulai = new MyTimebox(jadwalKegiatanKampus.getWaktuMulai()));
		waktuMulai.setCols(5);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));

		hbox.appendChild(waktuSelesai = new MyTimebox(jadwalKegiatanKampus.getWaktuSelesai()));
		waktuSelesai.setCols(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelBoldConfig("Tempat Kegiatan"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(tempat = new MyTextbox(jadwalKegiatanKampus.getTempat()));
		tempat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelBoldConfig("Pelaksana Kegiatan"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(pelaksana = new MyTextbox(jadwalKegiatanKampus.getPelaksana()));
		pelaksana.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelBoldConfig("Peserta Kegiatan"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(peserta = new MyTextbox(jadwalKegiatanKampus.getPeserta()));
		peserta.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelBoldConfig("Narasumber"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(narasumber = new MyTextbox(jadwalKegiatanKampus.getNarasumber()));
		narasumber.setWidth("90%");
		narasumber.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelBoldConfig("Info Kegiatan"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(nama = new MyCkEditor());
		nama.setValue(jadwalKegiatanKampus.getNama());

		Tbmuser tbmuser = Common.getCurrentUser();

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas / Prodi / Program"));
		Common.selectComboItem(fakultas, jadwalKegiatanKampus.getFakultas() == null ? tbmuser.ambilFakultas()
				: jadwalKegiatanKampus.getFakultas());

		row = new MyFormRow();
		row.setParent(rows);

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(fakultas);
		fakultas.setCols(10);

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		Common.pilihJurusan(jurusan,
				jadwalKegiatanKampus.getJurusan() == null ? tbmuser.ambilJurusan() : jadwalKegiatanKampus.getJurusan());
		hbox.appendChild(jurusan);
		jurusan.setCols(10);

		program = Common.initPrograms(program);

		Common.selectComboItem(program, jadwalKegiatanKampus.getProgram());
		hbox.appendChild(program);
		program.setCols(10);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelBoldConfig("Keterangan"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(keterangan = new MyTextbox(jadwalKegiatanKampus.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Info Kegiatan Kampus",
					"Kolom Info Kegiatan Kampus belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Info Kegiatan Kampus.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jadwalKegiatanKampus.getId() != null) {
			jadwalKegiatanKampus = (JadwalKegiatanKampus) session.load(JadwalKegiatanKampus.class,
					jadwalKegiatanKampus.getId());

		}

		jadwalKegiatanKampus.setNama(nama.getValue());
		jadwalKegiatanKampus.setTanggalMulai(tanggalMulai.getValue());
		jadwalKegiatanKampus.setTanggalSelesai(tanggalSelesai.getValue());
		jadwalKegiatanKampus.setTempat(tempat.getValue());
		jadwalKegiatanKampus.setPelaksana(pelaksana.getValue());
		jadwalKegiatanKampus.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null ? null : fakultas.getSelectedItem().getValue()));
		jadwalKegiatanKampus.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null ? null : jurusan.getSelectedItem().getValue()));
		jadwalKegiatanKampus
				.setProgram((String) (program.getSelectedItem() == null ? null : program.getSelectedItem().getValue()));

		jadwalKegiatanKampus.setWaktuMulai(waktuMulai.getValue());
		jadwalKegiatanKampus.setWaktuSelesai(waktuSelesai.getValue());
		jadwalKegiatanKampus.setPeserta(peserta.getValue());
		jadwalKegiatanKampus.setNarasumber(narasumber.getValue());
		jadwalKegiatanKampus.setKeterangan(keterangan.getValue());
		Common.refreshSaveOrUpdate(session, jadwalKegiatanKampus);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JadwalKegiatanKampus.class);

		if (order)
			criteria.addOrder(Order.desc("tanggalMulai"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("program"),
								Restrictions.eq("program",
										searchprogram.getSelectedItem() == null ? "Reguler"
												: searchprogram.getSelectedItem().getValue())))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("fakultas"),
								CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("jurusan"),
								CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

		;
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JadwalKegiatanKampus> jadwalKegiatanKampus = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jadwalKegiatanKampus);
		grid.setRowRenderer(new JadwalKegiatanKampusRenderer());
		grid.setModelCheckMobile(strset);
		grid.setSclass("fgrid");
	}

}
