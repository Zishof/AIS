package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

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
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.dashboard.admin.DashboardKknMahasiswa;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.kkn.KknUntukMahasiswaAction;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.KknDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisAktfitasMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.Kkn;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKkn;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.kkn.KknPunyaKomponenPenilaianKkn;
import ais.database.model.kkn.KknPunyaPersyaratan;
import ais.database.model.kkn.KomponenPenilaianKkn;
import ais.database.model.kkn.PersyaratanKkn;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk kkn. Tipe ini merupakan titik masuk UI yang menghubungkan event layar
 * dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchketerangan}, {@code Combobox
 * searchfakultas}, {@code Combobox searchjurusan}, {@code AmbilDataMahasiswaBanbox searchmahasiswa};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi
 * domain lain ({@code onStatistik()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk
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
public class KknAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchnama;
	private Textbox searchketerangan;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private AmbilDataMahasiswaBanbox searchmahasiswa;

	private Textbox nama_kelompok;
	private MyDatebox tanggal_mulai;
	private MyDatebox tanggal_selesai;
	private Textbox keterangan;
	private Intbox minimalSksBolehIkutKkn;
	private MyDoublebox minimalIpkBolehIkutKkn;

	private MyCheckboxConfig aktifkanSyaratLain;
	private Intbox minimalSksBolehIkutKkn2;
	private MyDoublebox minimalIpkBolehIkutKkn2;

	private Combobox fakultas;
	private Combobox jurusan;
	private Textbox kodeItemBiaya;

	private Kkn kkn;

	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;
	private MyCheckboxConfig harusBayar;
	private Combobox tahunAkademik;
	private Combobox semester;

	private List<KomponenPenilaianKkn> selectedKomponenPenilaianKkn;
	private List<PersyaratanKkn> selectedPersyaratanKkn;
	private Combobox program;

	protected Tabpanel statistik;
	private Textbox nimMhsTanpaBiaya;
	private Combobox jenisAktfitasMahasiswa;
	private MyCheckboxConfig mahasiswaBolehMerubahAgenda;
	private MyCheckboxConfig dosenBolehMerubahAgenda;

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DashboardKknMahasiswa include = new DashboardKknMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(include, statistik,
				"Statistik KKN", "Gambaran sebaran lokasi, kelompok, dan capaian KKN mahasiswa.");
		}
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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class KknRenderer extends ais.ui.util.MyRowRenderer {

		// private KknHelper kknHelper = new KknHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Kkn kkn = (Kkn) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(Kkn.class, kkn, kkn.getNama_kelompok())).setParent(arg0);
			new Label(kkn.getJenisAktfitasMahasiswa() == null ? "" : kkn.getJenisAktfitasMahasiswa().getNama())
					.setParent(a);
			new Label(kkn.getJenisAktfitasMahasiswa() == null ? ""
					: kkn.getJenisAktfitasMahasiswa().getKampusMerderka() ? "Kampus Merdeka:YA"
							: "Kampus Merdeka:TIDAK")
					.setParent(a);

			new Label(kkn.getFakultas() == null ? "Semua" : kkn.getFakultas().getNama()).setParent(arg0);
			new Label(kkn.getJurusan() == null ? "Semua" : kkn.getJurusan().getNama()).setParent(arg0);
			new Label(kkn.getProgram() == null || kkn.getProgram().trim().isEmpty() ? "Semua" : kkn.getProgram())
					.setParent(arg0);
			new Label((kkn.getTanggal_mulai() == null ? "" : Common.dateFormat4.get().format(kkn.getTanggal_mulai()))
					+ (kkn.getTanggal_selesai() == null ? ""
							: " s.d " + Common.dateFormat4.get().format(kkn.getTanggal_selesai())))
					.setParent(arg0);
			new Label(kkn.getTahunAkademik() + " / " + kkn.getSemester()).setParent(arg0);
			new Label(kkn.getMinimalSksBolehIkutKkn() + " SKS, IPK min " + kkn.getMinimalIpkBolehIkutKkn())
					.setParent(arg0);

			new Label(kkn.getAktifkanSyaratLain()
					? (kkn.getMinimalSksBolehIkutKkn2() + " SKS, IPK min " + kkn.getMinimalIpkBolehIkutKkn2())
					: "").setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			Session session = HibernateUtil.currentSession();
			if (!kkn.getKodeItemBiaya().trim().isEmpty()) {

				for (String kode : kkn.getKodeItemBiaya().trim().split(",")) {
					ItemBiaya itemBiaya = (ItemBiaya) ConstantValues.simpleObject(session
							.createCriteria(ItemBiaya.class).add(Restrictions.eq("kode", kode.trim())).setMaxResults(1),
							ItemBiaya.class);
					if (itemBiaya != null) {
						new Label(itemBiaya.getKode() + "-" + itemBiaya.getNama()).setParent(hbox);
					}
				}
			} else {
				new Label(ais.common.Common.getBahasaConfig("Tidak ada item biaya")).setParent(hbox);
			}

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kkn);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
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

											Common.refreshDelete(kkn);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Kkn());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "unchecked" })
	private void init(Kkn kkn) throws Exception {
		this.kkn = kkn;
		Common.clear(addWindow);
		addWindow.setTitle("Kkn");
		addWindow.setWidth("98%");
		addWindow.setHeight("98%");
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		West west = new West();
		west.setTitle("Pendataan");
		west.setParent(borderlayout);
		west.setWidth("30%");

		Center center = new Center();
		center.setTitle("Komponen Penilaian");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		East east = new East();
		east.setTitle("Form dan Persyaratan");
		east.setParent(borderlayout);
		east.setWidth("40%");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama_kelompok = new Textbox(kkn.getNama_kelompok() == null ? "" : kkn.getNama_kelompok()));
		nama_kelompok.setWidth("90%");
		// nama_kelompok.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis / Kampus Merdeka *"));
		row.appendChild(jenisAktfitasMahasiswa = new Combobox());
		Common.insertCombo(jenisAktfitasMahasiswa, "nama", "merupakanKampusMerdeka", JenisAktfitasMahasiswa.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisAktfitasMahasiswa, kkn.getJenisAktfitasMahasiswa());
		jenisAktfitasMahasiswa.setWidth("90%");
		jenisAktfitasMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pendaftaran Dimulai"));
		row.appendChild(tanggal_mulai = new MyDatebox(
				kkn.getTanggal_mulai() == null ? ais.ui.util.WaktuUtil.getDate() : kkn.getTanggal_mulai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pendaftaran Ditutup"));
		row.appendChild(tanggal_selesai = new MyDatebox(
				kkn.getTanggal_selesai() == null ? ais.ui.util.WaktuUtil.getDate() : kkn.getTanggal_selesai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik (*)"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		Common.selectComboItem(tahunAkademik, kkn.getTahunAkademik());
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		semester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semester.appendChild(comboitem);

		Common.selectComboItem(semester, kkn.getSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester (*)"));
		row.appendChild(semester);
		semester.setReadonly(true);

		tanggal_mulai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tanggal_mulai.getValue() != null) {
					Common.selectComboItem(tahunAkademik, Common.getCurrentTahunAkademik(tanggal_mulai.getValue()));
					Common.selectComboItem(semester,
							Common.isNowSemensterGanjil(tanggal_mulai.getValue()) ? Perkuliahan.GANJIL
									: Perkuliahan.GENAP);
				}
			}
		});

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		Tbmuser tbmuser = Common.getCurrentUser();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas, kkn.getFakultas() == null ? tbmuser.ambilFakultas() : kkn.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		// fakultas.setDisabled(false);

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, kkn.getJurusan() == null ? tbmuser.ambilJurusan() : kkn.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program = Common.initPrograms(program));
		Common.selectComboItem(program, kkn.getProgram());
		program.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(mahasiswaBolehMerubahAgenda = new MyCheckboxConfig("Mahasiswa boleh mengubah agenda"));
		mahasiswaBolehMerubahAgenda.setChecked(kkn.getMahasiswaBolehMerubahAgenda());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(dosenBolehMerubahAgenda = new MyCheckboxConfig("Dosen boleh mengubah agenda"));
		dosenBolehMerubahAgenda.setChecked(kkn.getDosenBolehMerubahAgenda());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(harusBayar = new MyCheckboxConfig("Harus telah membayar"));
		harusBayar.setChecked(kkn.getHarusBayar());

		Common.initKeterangan(rows, "* Mahasiswa harus telah membayar biaya perkuliahan sebelum bisa ikut mendaftar");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal SKS dan IPK"));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(minimalSksBolehIkutKkn = new Intbox(kkn.getMinimalSksBolehIkutKkn()));
		hbox.appendChild(minimalIpkBolehIkutKkn = new MyDoublebox(kkn.getMinimalIpkBolehIkutKkn()));

		minimalSksBolehIkutKkn.setCols(2);
		minimalIpkBolehIkutKkn.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktifkan Syarat Lain"));
		row.appendChild(aktifkanSyaratLain = new MyCheckboxConfig());
		aktifkanSyaratLain.setChecked(kkn.getAktifkanSyaratLain());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Atau juga minimal SKS dan IPK"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(minimalSksBolehIkutKkn2 = new Intbox(kkn.getMinimalSksBolehIkutKkn2()));
		hbox.appendChild(minimalIpkBolehIkutKkn2 = new MyDoublebox(kkn.getMinimalIpkBolehIkutKkn2()));

		minimalSksBolehIkutKkn2.setCols(2);
		minimalIpkBolehIkutKkn2.setCols(2);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				minimalSksBolehIkutKkn2.setDisabled(!aktifkanSyaratLain.isChecked());
				minimalIpkBolehIkutKkn2.setDisabled(!aktifkanSyaratLain.isChecked());
			}
		};
		eventListener.onEvent(null);
		aktifkanSyaratLain.addEventListener("onClick", eventListener);

		MyGrid subGrid = new MyGrid();
		subGrid.setWidth("100%");
		subGrid.setParent(center);
		subGrid.setHeight("100%");

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		Session session = HibernateUtil.currentSession();
		List<KomponenPenilaianKkn> komponenPenilaianKkns = session.createCriteria(KomponenPenilaianKkn.class)
				.createAlias("parent", "parent", Criteria.LEFT_JOIN).addOrder(Order.asc("parent.nomorUrut"))
				.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		TreeMap<KomponenPenilaianKkn, List<KomponenPenilaianKkn>> dataKomponenPenilaian = new TreeMap<KomponenPenilaianKkn, List<KomponenPenilaianKkn>>();
		for (KomponenPenilaianKkn komponenPenilaianKkn : komponenPenilaianKkns) {
			if (komponenPenilaianKkn.getParent() != null) {
				if (!dataKomponenPenilaian.keySet().contains(komponenPenilaianKkn.getParent())) {
					List<KomponenPenilaianKkn> datas = new ArrayList<KomponenPenilaianKkn>();
					datas.add(komponenPenilaianKkn);
					dataKomponenPenilaian.put(komponenPenilaianKkn.getParent(), datas);
				} else {
					dataKomponenPenilaian.get(komponenPenilaianKkn.getParent()).add(komponenPenilaianKkn);
				}
			}
		}

		for (KomponenPenilaianKkn komponenPenilaianKkn : komponenPenilaianKkns) {
			if (komponenPenilaianKkn.getParent() == null && !dataKomponenPenilaian.containsKey(komponenPenilaianKkn)) {
				List<KomponenPenilaianKkn> datas = new ArrayList<KomponenPenilaianKkn>();
				dataKomponenPenilaian.put(komponenPenilaianKkn, datas);
			}
		}

		if (kkn.getId() != null) {
			HibernateUtil.currentSession().refresh(this.kkn);
		}

		if (kkn.getId() != null) {

			selectedKomponenPenilaianKkn = session.createCriteria(KknPunyaKomponenPenilaianKkn.class)
					.setProjection(Projections.groupProperty("komponenPenilaianKkn"))
					.createAlias("komponenPenilaianKkn", "komponenPenilaianKkn")
					.add(Restrictions.or(Restrictions.isNull("komponenPenilaianKkn.aktif"),
							Restrictions.eq("komponenPenilaianKkn.aktif", true)))
					.add(Restrictions.eq("kkn", kkn)).list();

		} else {
			selectedKomponenPenilaianKkn = new ArrayList<KomponenPenilaianKkn>();
		}

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);

		for (final KomponenPenilaianKkn parent : dataKomponenPenilaian.keySet()) {

			final List<KomponenPenilaianKkn> datas = dataKomponenPenilaian.get(parent);
			if (datas.isEmpty()) {
				final Checkbox checkbox = new Checkbox(parent.getNama());
				checkbox.setParent(vboxSkala);
				checkbox.setChecked(selectedKomponenPenilaianKkn.contains(parent));
				checkbox.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (checkbox.isChecked()) {
							selectedKomponenPenilaianKkn.add(parent);
						} else {
							selectedKomponenPenilaianKkn.remove(parent);
						}
					}
				});
			} else {

				final Checkbox checkboxAll = new Checkbox(parent.getNama());
				checkboxAll.setParent(vboxSkala);
				checkboxAll.setChecked(selectedKomponenPenilaianKkn.contains(parent));

				Hbox myHb = new Hbox();
				myHb.setParent(vboxSkala);

				myHb.appendChild(new Space());

				Vbox vboxSkalaSub = new Vbox();
				vboxSkalaSub.setPack("top");
				vboxSkalaSub.setParent(myHb);

				final List<Checkbox> checkboxs = new ArrayList<Checkbox>();
				for (final KomponenPenilaianKkn komponenPenilaianKkn : datas) {

					final Checkbox checkbox = new Checkbox(komponenPenilaianKkn.getNama());
					checkboxs.add(checkbox);
					checkbox.setParent(vboxSkalaSub);
					checkbox.setChecked(selectedKomponenPenilaianKkn.contains(komponenPenilaianKkn));
					checkbox.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (checkbox.isChecked()) {
								selectedKomponenPenilaianKkn.add(komponenPenilaianKkn);
							} else {
								selectedKomponenPenilaianKkn.remove(komponenPenilaianKkn);
							}
						}
					});

				}

				checkboxAll.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (checkboxAll.isChecked()) {
							selectedKomponenPenilaianKkn.add(parent);
						} else {
							selectedKomponenPenilaianKkn.remove(parent);
						}

						for (Checkbox checkbox : checkboxs) {
							checkbox.setChecked(checkboxAll.isChecked());
						}
						for (final KomponenPenilaianKkn komponenPenilaianKkn : datas) {
							if (checkboxAll.isChecked()) {
								selectedKomponenPenilaianKkn.add(komponenPenilaianKkn);
							} else {
								selectedKomponenPenilaianKkn.remove(komponenPenilaianKkn);
							}
						}
					}
				});

			}
		}

		subGrid = new MyGrid();
		subGrid.setWidth("100%");
		subGrid.setParent(east);
		subGrid.setHeight("100%");

		subRows = new Rows();
		subRows.setParent(subGrid);

		List<PersyaratanKkn> persyaratanKkns = session.createCriteria(PersyaratanKkn.class).addOrder(Order.asc("nama"))
				.addOrder(Order.asc("labelInputan"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		if (kkn.getId() != null) {
			HibernateUtil.currentSession().refresh(this.kkn);
		}

		if (kkn.getId() != null) {

			selectedPersyaratanKkn = session.createCriteria(KknPunyaPersyaratan.class)
					.setProjection(Projections.groupProperty("persyaratanKkn"))
					.createAlias("persyaratanKkn", "persyaratanKkn")
					.add(Restrictions.or(Restrictions.isNull("persyaratanKkn.aktif"),
							Restrictions.eq("persyaratanKkn.aktif", true)))
					.add(Restrictions.eq("kkn", kkn)).list();

		} else {
			selectedPersyaratanKkn = new ArrayList<PersyaratanKkn>();
		}
		final List<PersyaratanKkn> persyaratanKknsKomponens = new ArrayList<PersyaratanKkn>();
		Label labelLama = new Label("");
		List<Component> components = new ArrayList<Component>();
		for (final PersyaratanKkn persyaratanKkn : persyaratanKkns) {

			Row myrow = KknUntukMahasiswaAction.tampilkanPersyaratan(persyaratanKkn, null, labelLama, subRows,
					components, false, persyaratanKknsKomponens);

			final Checkbox checkbox = new Checkbox();
			checkbox.setParent(myrow);
			checkbox.setChecked(selectedPersyaratanKkn.contains(persyaratanKkn));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedPersyaratanKkn.add(persyaratanKkn);
					} else {
						selectedPersyaratanKkn.remove(persyaratanKkn);
					}
				}
			});
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Item Biaya"));
		row.appendChild(kodeItemBiaya = new Textbox(kkn.getKodeItemBiaya()));
		kodeItemBiaya.setWidth("90%");
		kodeItemBiaya.setRows(2);

		Common.initKeterangan(rows,
				"Jika syarat mengikuti kkn harus membayar biaya tertentu, masukkan kode item biaya yang harus dibayar mahasiswa yang mengikuti kkn. Jika item biaya lebih dari satu, pisahkan dengan tanda koma (,), contoh : 502,505,506 dan seterusnya. Dan juga pastikan kode item biaya benar.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM yg tidak mengikuti syarat pembayaran"));
		row.appendChild(nimMhsTanpaBiaya = new Textbox(kkn.getNimMhsTanpaBiaya()));
		nimMhsTanpaBiaya.setWidth("90%");
		nimMhsTanpaBiaya.setRows(3);

		Common.initKeterangan(rows,
				"Jika NIM mahasiswa lebih dari satu, pisahkan dengan tanda koma (,), contoh : 12345678,12345679,123456710 dan seterusnya.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ketarangan"));
		row.appendChild(keterangan = new Textbox(kkn.getKeterangan() == null ? "" : kkn.getKeterangan()));
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
					// loadKurikulum();
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {

		if (nama_kelompok.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data KKN",
					"Kolom Nama KKN belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama KKN.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisAktfitasMahasiswa.getSelectedItem() == null
				|| jenisAktfitasMahasiswa.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis aktiftas",
					"Kolom Jenis aktiftas belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis aktiftas.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		KknDao kknDao = DaoFactory.getInstance().getKknDao();
		if (kkn.getId() != null) {
			kkn = kknDao.load(kkn.getId());
		}

		kkn.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		kkn.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		kkn.setProgram((String) (program.getSelectedItem() == null ? null : program.getSelectedItem().getValue()));
		kkn.setKeterangan(keterangan.getValue());
		kkn.setNama_kelompok(nama_kelompok.getValue());
		kkn.setTanggal_mulai(tanggal_mulai.getValue());
		kkn.setTanggal_selesai(tanggal_selesai.getValue());
		kkn.setMinimalSksBolehIkutKkn(minimalSksBolehIkutKkn.getValue());
		kkn.setMinimalIpkBolehIkutKkn(minimalIpkBolehIkutKkn.getValue());
		kkn.setMinimalIpkBolehIkutKkn2(minimalIpkBolehIkutKkn2.getValue());
		kkn.setMinimalSksBolehIkutKkn2(minimalSksBolehIkutKkn2.getValue());
		kkn.setAktifkanSyaratLain(aktifkanSyaratLain.isChecked());
		kkn.setHarusBayar(harusBayar.isChecked());
		kkn.setKodeItemBiaya(kodeItemBiaya.getValue().trim());
		kkn.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		kkn.setSemester((String) semester.getSelectedItem().getValue());
		kkn.setNimMhsTanpaBiaya(nimMhsTanpaBiaya.getValue().trim());
		kkn.setJenisAktfitasMahasiswa((JenisAktfitasMahasiswa) jenisAktfitasMahasiswa.getSelectedItem().getValue());
		kkn.setMahasiswaBolehMerubahAgenda(mahasiswaBolehMerubahAgenda.isChecked());
		kkn.setDosenBolehMerubahAgenda(dosenBolehMerubahAgenda.isChecked());
		// kknDao.beginTransaction();
		if (kkn.getId() != null) {
			kknDao.update(kkn);
		} else {
			kknDao.save(kkn);
		}

		Session session = HibernateUtil.currentSession();
		session.createSQLQuery("delete from kkn_punya_komponen_penilaian_kkn where kkn=" + kkn.getId()).executeUpdate();
		for (KomponenPenilaianKkn komponenPenilaianKkn : selectedKomponenPenilaianKkn) {
			KknPunyaKomponenPenilaianKkn kknPunyaKomponenPenilaianKkn = new KknPunyaKomponenPenilaianKkn();
			kknPunyaKomponenPenilaianKkn.setKomponenPenilaianKkn(komponenPenilaianKkn);
			kknPunyaKomponenPenilaianKkn.setNama(komponenPenilaianKkn.getNama());
			kknPunyaKomponenPenilaianKkn.setKkn(kkn);
			session.save(kknPunyaKomponenPenilaianKkn);
		}

		session.createSQLQuery("delete from kkn_punya_persyaratan where kkn=" + kkn.getId()).executeUpdate();
		for (PersyaratanKkn persyaratanKkn : selectedPersyaratanKkn) {
			KknPunyaPersyaratan kknPunyaPersyaratanKkn = new KknPunyaPersyaratan();
			kknPunyaPersyaratanKkn.setPersyaratanKkn(persyaratanKkn);
			kknPunyaPersyaratanKkn.setNama(persyaratanKkn.getNama());
			kknPunyaPersyaratanKkn.setKkn(kkn);
			session.save(kknPunyaPersyaratanKkn);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Mahasiswa mahasiswa = (Mahasiswa) searchmahasiswa.getAttribute("mahasiswa");

		if (mahasiswa != null) {
			Criteria criteria = session.createCriteria(MahasiswaDapatKkn.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa)).setProjection(Projections.property("kkn"))
					.createCriteria("kkn")

					.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
							|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
					.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
							|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			if (order)
				criteria.addOrder(Order.desc("tanggal_mulai"));
			criteria.add(Restrictions.ilike("nama_kelompok", searchnama.getValue(), MatchMode.ANYWHERE))
					.add(Restrictions.ilike("keterangan", searchketerangan.getValue(), MatchMode.ANYWHERE));
			return criteria;
		} else {
			Criteria criteria = session.createCriteria(Kkn.class)

					.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
							|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
					.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
							|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
									: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			if (order)
				criteria.addOrder(Order.desc("tanggal_mulai"));
			criteria.add(Restrictions.ilike("nama_kelompok", searchnama.getValue(), MatchMode.ANYWHERE))
					.add(Restrictions.ilike("keterangan", searchketerangan.getValue(), MatchMode.ANYWHERE));
			return criteria;
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Kkn> kkn = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(kkn);
		grid.setRowRenderer(new KknRenderer());
		grid.setModelCheckMobile(strset);

	}

}
