package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.Calendar;
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
import ais.ui.util.MyDetail;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Longbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.beasiswa.BeasiswaUntukMahasiswaAction;
import ais.action.master.dashboard.admin.DashboardBeasiswaMahasiswa;
import ais.action.master.epsbed.TransaksiBeasiswaBidikmisi;
import ais.action.master.helper.AmbilDataItemBiayaHelper;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.BeasiswaHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Beasiswa;
import ais.database.model.BeasiswaPunyaItemBiayaTambahan;
import ais.database.model.Fakultas;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisPenerimaBeasiswa;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatBeasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.beasiswa.BeasiswaPunyaPersyaratan;
import ais.database.model.beasiswa.PersyaratanBeasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk beasiswa. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code MyGrid itemGrid}, {@code Textbox searchnama}, {@code Textbox
 * searchketerangan}, {@code AmbilDataMahasiswaBanbox searchmahasiswa}, {@code Textbox nama};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initItemBiaya()}, {@code
 * init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}, {@code loadData()}); mutasi
 * data ({@code onSave()}); operasi domain lain ({@code onPenerima()}, {@code onStatistik()}, {@code onAdd()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class BeasiswaAction extends GenericAutowireComposer implements DataLoader {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private MyGrid itemGrid;
	private Textbox searchnama;
	private Textbox searchketerangan;
	private AmbilDataMahasiswaBanbox searchmahasiswa;

	private Textbox nama;
	private MyDatebox date;
	private Textbox instansi;
	private Textbox keterangan;
	private MyDatebox tanggalBuka;
	private MyDatebox tanggalTutup;
	private Combobox tahun;
	private Combobox searchtahun;
	private MyCheckboxConfig dibukaUntukMahasiswa;
	private Doublebox batasanIP;
	private Doublebox batasanSks;
	private Doublebox batasanSkkp;
	private MyCheckboxConfig bolehGanda;
	private Longbox penghasilanOrangTua;

	private Combobox fakultas;
	private Combobox jurusan;

	private Combobox tahunAkademik;
	private Combobox semester;

	private Beasiswa beasiswa;

	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;
	private MyCheckboxConfig harusBayar;
	private Combobox jenisPenerimaBeasiswa;

	private Tabpanel laporanPenerima;

	public void onPenerima(Event event) {

		if (laporanPenerima.getChildren().size() == 0) {
			TransaksiBeasiswaBidikmisi laporan = new TransaksiBeasiswaBidikmisi();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(laporanPenerima);
		}
	}

	protected Tabpanel statistik;

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DashboardBeasiswaMahasiswa include = new DashboardBeasiswaMahasiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(include, statistik,
				"Statistik Beasiswa", "Gambaran sebaran penerima beasiswa per jenis, jurusan, dan tahun.");
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

		tahun = new Combobox();
		MyComboitemConfig comboitem;
		Integer tahunCurrent = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (Integer i = tahunCurrent - 10; i <= tahunCurrent; i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
		}
		for (Integer i = tahunCurrent - 10; i <= tahunCurrent; i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			searchtahun.appendChild(comboitem);
		}

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class BeasiswaRenderer extends ais.ui.util.MyRowRenderer {

		private BeasiswaHelper beasiswaHelper = new BeasiswaHelper();

		// private Session session = HibernateUtil.currentSession();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Beasiswa beasiswa = (Beasiswa) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen())
						beasiswaHelper.displayPrasyaratBeasiswa(beasiswa, detail, addWindow);
				}

			});

			new Label(beasiswa.getTahun() == null ? "" : beasiswa.getTahun().toString()).setParent(arg0);
			Vbox v = RevisiHelper.createNewRevisi(Beasiswa.class, beasiswa, beasiswa.getNama());
			v.setParent(arg0);
			v.appendChild(new Label(beasiswa.getTahunAkademik() + "/" + beasiswa.getSemester()));
			new Label(beasiswa.getJenisPenerimaBeasiswa() == null ? "" : beasiswa.getJenisPenerimaBeasiswa().getNama())
					.setParent(arg0);
			new Label(beasiswa.getTanggalBuka() == null ? "" : Common.dateFormat2.get().format(beasiswa.getTanggalBuka()))
					.setParent(arg0);
			new Label(beasiswa.getTanggalTutup() == null ? "" : Common.dateFormat2.get().format(beasiswa.getTanggalTutup()))
					.setParent(arg0);
			new Label(beasiswa.getJenjang() == null ? "Semua" : beasiswa.getJenjang().getNama()).setParent(arg0);
			new Label(beasiswa.getFakultas() == null ? "Semua" : beasiswa.getFakultas().getNama()).setParent(arg0);
			new Label(beasiswa.getJurusan() == null ? "Semua" : beasiswa.getJurusan().getNama()).setParent(arg0);
			new Label(beasiswa.getInstansi()).setParent(arg0);

			new Label(beasiswa.getDibukaUtkMahasiswa().equals(1) ? "Ya" : "Tidak").setParent(arg0);

			// new Label(beasiswa.getDate() == null ? "" :
			// Common.dateFormat.get().format(beasiswa.getDate())).setParent(arg0);

			new Label(Common.numberFormat.get().format(beasiswa.getBatasanIP()) + " / "
					+ Common.numberFormat.get().format(beasiswa.getBatasanSks()) + " / "
					+ Common.numberFormat.get().format(beasiswa.getBatasanSkkp())).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig

			// button = new MyToolbarbuttonConfig("", "/img/absensi_pmb.png");
			// button.setOrient("vertical");
			// button.setTooltiptext("Form dan Persyaratan Beasiswa");
			// button.setVisible(edit);
			// final AmbilDataSyaratBeasiswaHelper ambilDataSyaratBeasiswaHelper
			// = new AmbilDataSyaratBeasiswaHelper(
			// beasiswa);
			// button.addEventListener("onClick", new EventListener() {
			// @Override
			// public void onEvent(Event event) throws Exception {
			// ambilDataSyaratBeasiswaHelper.display(addWindow, new
			// EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// onSearchDefault(arg0);
			// }
			// });
			// addWindow.setVisible(true);
			// addWindow.onModal();
			//
			// }
			//
			// });
			// button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(beasiswa);
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

											Common.refreshDelete(beasiswa);

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
			toolbar.setParent(arg0);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new Beasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("unused")
	private void initItemBiaya(final Beasiswa beasiswa) throws Exception {
		this.beasiswa = beasiswa;
		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		Common.clear(addWindow);
		addWindow.setTitle("Item Biaya Tambahan");
		addWindow.setWidth("550px");
		addWindow.setHeight("500px");
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Item Biaya", "/img/new.gif");
		toolbarbutton.setParent(toolbar);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataItemBiayaHelper ambilDataItemBiayaHelper = new AmbilDataItemBiayaHelper();
				ambilDataItemBiayaHelper.display(beasiswa, BeasiswaAction.this);

			}
		});

		itemGrid = new MyGrid();
		itemGrid.setParent(center);
		Columns columns = new Columns();
		columns.setParent(itemGrid);
		MyColumnConfig column = new MyColumnConfig("Kode");
		column.setParent(columns);
		column = new MyColumnConfig("Nama");
		column.setParent(columns);
		column = new MyColumnConfig("Deskripsi");
		column.setParent(columns);
		column = new MyColumnConfig("Jumlah");
		column.setParent(columns);
		column = new MyColumnConfig("");
		column.setParent(columns);
		column.setWidth("10%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
				onSearchDefault(event);
			}
		});
		cancel.setParent(toolbar);

		loadData(null);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private List<PersyaratanBeasiswa> selectedPersyaratanBeasiswa;
	private Combobox jenjang;

	@SuppressWarnings("unchecked")
	private void init(final Beasiswa beasiswa) {
		this.beasiswa = beasiswa;
		Common.clear(addWindow);
		addWindow.setTitle("Beasiswa");
		addWindow.setWidth("800px");
		addWindow.setHeight("95%");
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		East east = new East();
		east.setTitle("Form dan Persyaratan");
		east.setParent(borderlayout);
		east.setWidth("50%");

		MyGrid subGrid = new MyGrid();
		subGrid.setWidth("100%");
		subGrid.setParent(east);
		subGrid.setHeight("100%");

		Rows subRows = new Rows();
		subRows.setParent(subGrid);
		Session session = HibernateUtil.currentSession();
		List<PersyaratanBeasiswa> persyaratanBeasiswas = session.createCriteria(PersyaratanBeasiswa.class)
				.addOrder(Order.asc("nama")).addOrder(Order.asc("labelInputan"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		if (beasiswa.getId() != null) {
			HibernateUtil.currentSession().refresh(this.beasiswa);
		}

		if (beasiswa.getId() != null) {
			selectedPersyaratanBeasiswa = session.createCriteria(BeasiswaPunyaPersyaratan.class)
					.setProjection(Projections.groupProperty("persyaratanBeasiswa"))
					.createAlias("persyaratanBeasiswa", "persyaratanBeasiswa")
					.add(Restrictions.or(Restrictions.isNull("persyaratanBeasiswa.aktif"),
							Restrictions.eq("persyaratanBeasiswa.aktif", true)))
					.add(Restrictions.eq("beasiswa", beasiswa)).list();

		} else {
			selectedPersyaratanBeasiswa = new ArrayList<PersyaratanBeasiswa>();
		}

		Label labelLama = new Label("");
		List<Component> components = new ArrayList<Component>();
		for (final PersyaratanBeasiswa persyaratanBeasiswa : persyaratanBeasiswas) {

			Row myrow = BeasiswaUntukMahasiswaAction.tampilkanPersyaratan(persyaratanBeasiswa, null, null, labelLama,
					components, false);
			subRows.appendChild(myrow);

			final Checkbox checkbox = new Checkbox();
			checkbox.setParent(myrow);
			checkbox.setChecked(selectedPersyaratanBeasiswa.contains(persyaratanBeasiswa));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedPersyaratanBeasiswa.add(persyaratanBeasiswa);
					} else {
						selectedPersyaratanBeasiswa.remove(persyaratanBeasiswa);
					}
				}
			});
		}

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Program Beasiswa *"));
		row.appendChild(nama = new Textbox(beasiswa.getNama() == null ? "" : beasiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		row.appendChild(tahun);
		Common.selectComboItem(tahun, beasiswa.getTahun() == null ? null : beasiswa.getTahun());
		tahun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		jenjang = new Combobox();
		Common.insertComboDanSemua(jenjang, "nama", "keterangan", Jenjang.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(jenjang, beasiswa.getJenjang());
		row.appendChild(jenjang);
		jenjang.setWidth("90%");

		fakultas = new Combobox();
		jurusan = new Combobox();

		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		Tbmuser tbmuser = Common.getCurrentUser();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas,
				beasiswa.getFakultas() == null ? tbmuser.ambilFakultas() : beasiswa.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		// fakultas.setDisabled(false);

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, beasiswa.getJurusan() == null ? tbmuser.ambilJurusan() : beasiswa.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Beasiswa *"));
		row.appendChild(jenisPenerimaBeasiswa = new Combobox());
		Common.insertCombo(jenisPenerimaBeasiswa, "nama", "jenis", JenisPenerimaBeasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		jenisPenerimaBeasiswa.setWidth("90%");
		Common.selectComboItem(jenisPenerimaBeasiswa, beasiswa.getJenisPenerimaBeasiswa());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dibuka Untuk Mahasiswa"));
		row.appendChild(dibukaUntukMahasiswa = new MyCheckboxConfig());
		dibukaUntukMahasiswa
				.setChecked(beasiswa.getDibukaUtkMahasiswa() != null && beasiswa.getDibukaUtkMahasiswa() == 1);
		dibukaUntukMahasiswa.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				if (dibukaUntukMahasiswa.isChecked()) {
					tanggalBuka.setDisabled(false);
					tanggalTutup.setDisabled(false);
					batasanIP.setDisabled(false);
					batasanSks.setDisabled(false);
					batasanSkkp.setDisabled(false);
					bolehGanda.setDisabled(false);
					penghasilanOrangTua.setDisabled(false);
				} else {
					tanggalBuka.setDisabled(true);
					tanggalTutup.setDisabled(true);
					batasanIP.setDisabled(true);
					batasanSks.setDisabled(true);
					batasanSkkp.setDisabled(true);
					bolehGanda.setDisabled(true);
					penghasilanOrangTua.setDisabled(true);
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Buka"));
		row.appendChild(
				tanggalBuka = new MyDatebox(beasiswa.getTanggalBuka() == null ? null : beasiswa.getTanggalBuka()));
		tanggalBuka.setDisabled(beasiswa.getDibukaUtkMahasiswa() == null || beasiswa.getDibukaUtkMahasiswa() != 1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tutup"));
		row.appendChild(
				tanggalTutup = new MyDatebox(beasiswa.getTanggalTutup() == null ? null : beasiswa.getTanggalTutup()));
		tanggalTutup.setDisabled(beasiswa.getDibukaUtkMahasiswa() == null || beasiswa.getDibukaUtkMahasiswa() != 1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("IPK > = "));
		row.appendChild(batasanIP = new MyDoublebox(beasiswa.getBatasanIP() == null ? 0.0 : beasiswa.getBatasanIP()));
		batasanIP.setDisabled(beasiswa.getDibukaUtkMahasiswa() == null || beasiswa.getDibukaUtkMahasiswa() != 1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SKS Total > = "));
		row.appendChild(batasanSks = new MyDoublebox(beasiswa.getBatasanSks()));
		batasanSks.setDisabled(beasiswa.getDibukaUtkMahasiswa() == null || beasiswa.getDibukaUtkMahasiswa() != 1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angka Kredit Kegiatan Kemahasiswaan > = "));
		row.appendChild(batasanSkkp = new MyDoublebox(beasiswa.getBatasanSkkp()));
		batasanSkkp.setDisabled(beasiswa.getDibukaUtkMahasiswa() == null || beasiswa.getDibukaUtkMahasiswa() != 1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tidak sedang menerima beasiswa dari instansi lain"));
		row.appendChild(bolehGanda = new MyCheckboxConfig());
		bolehGanda.setChecked(beasiswa.getBolehGanda() != null && beasiswa.getBolehGanda() == true);
		bolehGanda.setDisabled(beasiswa.getDibukaUtkMahasiswa() == null || beasiswa.getDibukaUtkMahasiswa() != 1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Batas maksimal penghasilan orang tua"));
		row.appendChild(penghasilanOrangTua = new Longbox(
				beasiswa.getPenghasilanOrangTua() == null ? 0 : beasiswa.getPenghasilanOrangTua()));
		// Common.selectComboItem(
		// penghasilanOrangTua,
		// beasiswa.getPenghasilanOrangTua() == null ? null : beasiswa
		// .getPenghasilanOrangTua());
		penghasilanOrangTua
				.setDisabled(beasiswa.getDibukaUtkMahasiswa() == null || beasiswa.getDibukaUtkMahasiswa() != 1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Instansi/Sponsor/Perusahaan"));
		row.appendChild(instansi = new Textbox(beasiswa.getInstansi() == null ? "" : beasiswa.getInstansi()));
		instansi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Harus telah membayar *"));
		row.appendChild(harusBayar = new MyCheckboxConfig());
		harusBayar.setChecked(beasiswa.getHarusBayar());

		Common.initKeterangan(rows, "* Mahasiswa harus telah membayar biaya perkuliahan sebelum bisa ikut mendaftar");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik (*)"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		Common.selectComboItem(tahunAkademik, beasiswa.getTahunAkademik());
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

		Common.selectComboItem(semester, beasiswa.getSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester (*)"));
		row.appendChild(semester);
		semester.setReadonly(true);

		tanggalBuka.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tanggalBuka.getValue() != null) {
					Common.selectComboItem(tahunAkademik, Common.getCurrentTahunAkademik(tanggalBuka.getValue()));
					Common.selectComboItem(semester,
							Common.isNowSemensterGanjil(tanggalBuka.getValue()) ? Perkuliahan.GANJIL
									: Perkuliahan.GENAP);
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(beasiswa.getKeterangan() == null ? "" : beasiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		row.appendChild(date = new MyDatebox(
				beasiswa.getDate() == null ? ais.ui.util.WaktuUtil.getDate() : beasiswa.getDate()));

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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisPenerimaBeasiswa.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Beasiswa",
					"Kolom Jenis Beasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Beasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		// if (tahun.getSelectedItem() == null) {
		// MyMessageboxConfig.show("Tahun harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }

		Session session = HibernateUtil.currentSession();
		if (beasiswa.getId() != null) {
			beasiswa = (Beasiswa) session.load(Beasiswa.class, beasiswa.getId());
		}

		beasiswa.setBatasanSks(batasanSks.getValue());
		beasiswa.setJenisPenerimaBeasiswa((JenisPenerimaBeasiswa) jenisPenerimaBeasiswa.getSelectedItem().getValue());
		beasiswa.setNama(nama.getValue());
		beasiswa.setDate(date.getValue());
		beasiswa.setInstansi(instansi.getValue());
		beasiswa.setKeterangan(keterangan.getValue());
		beasiswa.setTahun((Integer) tahun.getSelectedItem().getValue());
		beasiswa.setTanggalBuka(tanggalBuka.getValue());
		beasiswa.setTanggalTutup(tanggalTutup.getValue());
		beasiswa.setDibukaUtkMahasiswa(dibukaUntukMahasiswa.isChecked() ? 1 : 0);
		beasiswa.setBatasanIP(batasanIP.getValue() == null ? 0.0 : batasanIP.getValue());
		beasiswa.setBatasanSkkp(batasanSkkp.getValue());
		beasiswa.setBolehGanda(bolehGanda.isChecked() ? true : false);
		// beasiswa.setPenghasilanOrangTua((Long) (penghasilanOrangTua
		// .getSelectedItem().getValue() == null ? null
		// : penghasilanOrangTua.getSelectedItem().getValue()));
		beasiswa.setPenghasilanOrangTua(penghasilanOrangTua.getValue());
		beasiswa.setHarusBayar(harusBayar.isChecked());
		beasiswa.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		beasiswa.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));

		beasiswa.setJenjang(
				(Jenjang) (jenjang.getSelectedItem() == null || jenjang.getSelectedItem().getValue() == null ? null
						: jenjang.getSelectedItem().getValue()));

		beasiswa.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		beasiswa.setSemester((String) semester.getSelectedItem().getValue());
		if (beasiswa.getId() != null) {
			Common.refreshUpdate(session, beasiswa);
		} else {
			session.save(beasiswa);
		}

		session.createSQLQuery("delete from beasiswa_punya_persyaratan where beasiswa=" + beasiswa.getId())
				.executeUpdate();
		for (PersyaratanBeasiswa persyaratanBeasiswa : selectedPersyaratanBeasiswa) {
			BeasiswaPunyaPersyaratan beasiswaPunyaPersyaratanBeasiswa = new BeasiswaPunyaPersyaratan();
			beasiswaPunyaPersyaratanBeasiswa.setPersyaratanBeasiswa(persyaratanBeasiswa);
			beasiswaPunyaPersyaratanBeasiswa.setNama(persyaratanBeasiswa.getNama());
			beasiswaPunyaPersyaratanBeasiswa.setBeasiswa(beasiswa);
			session.save(beasiswaPunyaPersyaratanBeasiswa);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Mahasiswa mahasiswa = (Mahasiswa) searchmahasiswa.getAttribute("mahasiswa");

		Session session = HibernateUtil.currentSession();
		if (mahasiswa != null) {
			Criteria criteria = session.createCriteria(MahasiswaDapatBeasiswa.class)
					.setProjection(Projections.property("beasiswa")).add(Restrictions.eq("mahasiswa", mahasiswa))
					.createCriteria("beasiswa");
			if (order)
				criteria.addOrder(Order.desc("date"));
			criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
					.add(Restrictions.ilike("keterangan", searchketerangan.getValue(), MatchMode.ANYWHERE));
			return criteria;
		} else {
			Criteria criteria = session.createCriteria(Beasiswa.class);
			if (order)
				criteria.addOrder(Order.desc("date"));
			criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
					.add(Restrictions.ilike("keterangan", searchketerangan.getValue(), MatchMode.ANYWHERE));
			return criteria;
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Beasiswa> beasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(beasiswa);
		grid.setRowRenderer(new BeasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	class BeasiswaPunyaItemBiayaTambahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BeasiswaPunyaItemBiayaTambahan beasiswaPunyaItemBiayaTambahan = (BeasiswaPunyaItemBiayaTambahan) arg1;
			ItemBiaya itemBiaya = beasiswaPunyaItemBiayaTambahan.getItemBiaya();
			RevisiHelper.createNewRevisi(BeasiswaPunyaItemBiayaTambahan.class, beasiswaPunyaItemBiayaTambahan,
					itemBiaya.getKode()).setParent(arg0);
			new Label(itemBiaya.getNama()).setParent(arg0);
			new Label(itemBiaya.getDeskripsi()).setParent(arg0);
			final MyDoublebox doublebox = new MyDoublebox(beasiswaPunyaItemBiayaTambahan.getJumlah() == null ? 0
					: beasiswaPunyaItemBiayaTambahan.getJumlah());
			doublebox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					beasiswaPunyaItemBiayaTambahan.setJumlah(doublebox.getValue());
					Common.refreshUpdate(session, (beasiswaPunyaItemBiayaTambahan));
				}
			});
			doublebox.setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
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

											Common.refreshDelete(beasiswaPunyaItemBiayaTambahan);

											loadData(null);
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
			toolbar.setParent(arg0);
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<BeasiswaPunyaItemBiayaTambahan> itemBiaya = session.createCriteria(BeasiswaPunyaItemBiayaTambahan.class)
				.add(Restrictions.eq("beasiswa", beasiswa)).addOrder(Order.asc("id"))
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(itemBiaya);
		itemGrid.setRowRenderer(new BeasiswaPunyaItemBiayaTambahanRenderer());
		itemGrid.setModelCheckMobile(strset);

		itemGrid.renderAll();
		onSearchDefault(null);
	}

}
