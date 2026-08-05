package ais.action.master;

import java.util.ArrayList;
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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
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
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.action.master.pmb.AfiliasiCalonMahasiswaDetailAction;
import ais.action.master.sop.helper.SopUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AfiliasiCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GeneralValueObject;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AfiliasiCalonMahasiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchcalon;
	private Checkbox searchaktif;
	private Combobox searchTahunAjaran;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private AfiliasiCalonMahasiswa afiliasiCalonMahasiswa;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private MyIntbox kuotaDaftar;
	private Textbox khususUsername;
	private Combobox statusAwalMahasiswa;

	private Tabpanel afiliasiPegawaiTab;

	public void onDosen(Event event) {
		if (afiliasiPegawaiTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(afiliasiPegawaiTab);
			MyInclude iframe = new MyInclude("/pages/master/afiliasi_calon_mahasiswa_pegawai.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel afiliasiMahasiswaTab;

	public void onMahasiswa(Event event) {
		if (afiliasiMahasiswaTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(afiliasiMahasiswaTab);
			MyInclude iframe = new MyInclude("/pages/master/afiliasi_calon_mahasiswa_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public static String[] contents = new String[] { "afiliasiCalonMahasiswa.nama", "noRegistrasi", "noUjian", "nama",
			"totalSkor", "alamat", "rt", "rw", "kelurahanCalon", "kecamatanCalon", "kotaCalon", "propinsiCalon",
			"namaSekolahAsal", "namaSekolahAsal.kode",

			// ----

			"pembayaranRegistrasi", "pembayaranDaftarUlang", "kodePos", "tempatLahir", "tanggalLahir", "jenisKelamin",
			"asalNegara", "kewarganegaraan", "jenisKartuIdentitas", "noIdentitas", "email", "nisn", "jenisSekolah",
			"akreditasiSekolah", "kodePosSekolah", "kecamatanSekolah", "kotaSekolah", "propinsiSekolah",
			"tahunKelulusan", "jurusanSekolah", "jurusanSekolahLain", "namaWali", "noTelpOrtu", "pendapatanOrtu",
			"pendidikanOrtu", "alamatOrtu", "rtOrtu", "rwOrtu", "kodePosOrtu", "kecamatanOrtu", "kelurahanOrtu",
			"propinsiOrtu", "kotaOrtu", "paket", "prodi1", "prodi2", "prodi3", "prodi4", "prodi5", "jenjang",
			"statusLulus", "prodiLulus", "nimGenerated", "cetakKartu", "program", "jenisSeleksi", "tanggalDaftar",
			"tahun", "semesterMulai", "tahunAkademik", "gelombangPendaftaran", "tanggalPendaftaran", "agama",
			"semesterMulai", "program", "hp", "namaAyah", "pendidikanAyah", "pekerjaanAyah", "namaIbu", "pendidikanIbu",
			"pekerjaanIbu", "namaUntukIjazah", "noIjazah", "ukuranJaket", "tinggiBadan", "pernahMenetapDiLuarNegeri",
			"beratBadan", "teleponRumah", "suratIzinMengemudi", "kendaraanKuliah", "pernahMemimpinOrganisasi",
			"namaOrganisasi", "hobi", "minatSeni", "kemampuanBahasa1", "kemampuanBahasa2", "kemampuanBahasa3",
			"asalSma", "alamatAsalSma", "asalSmp", "alamatAsalSmp", "asalSd", "alamatAsalSd", "golonganDarah",
			"statusNikah", "jenisKuliah", "statusPembayaran", "nim", "mahasiswa", "merupakanPindahan",
			"pindahanDariKampus", "pindahanDariProdi", "nimLamaSebelumPindah", "pindahDariKampusLamaDiSemester",
			"tanggalPindah", "keteranganPindah", "infoKampusDariMana", "namaTemanInfoKampusDariMana",
			"keteranganInfoKampusDariMana", "pinPassword", "parameterTambahan", "parameterTambahanInds",
			"tanggal_dirubah", "oleh", "keterangan", "telahLogin", "waktuLogin" };

	public static String[] contentsGabungan = new String[] {
			"afiliasiCalonMahasiswa.nama,afiliasiPegawai.nama,afiliasiMahasiswa.nama", "noRegistrasi", "noUjian",
			"nama", "totalSkor", "alamat", "rt", "rw", "kelurahanCalon", "kecamatanCalon", "kotaCalon", "propinsiCalon",
			"namaSekolahAsal", "namaSekolahAsal.kode",

			// ----

			"pembayaranRegistrasi", "pembayaranDaftarUlang", "kodePos", "tempatLahir", "tanggalLahir", "jenisKelamin",
			"asalNegara", "kewarganegaraan", "jenisKartuIdentitas", "noIdentitas", "email", "nisn", "jenisSekolah",
			"akreditasiSekolah", "kodePosSekolah", "kecamatanSekolah", "kotaSekolah", "propinsiSekolah",
			"tahunKelulusan", "jurusanSekolah", "jurusanSekolahLain", "namaWali", "noTelpOrtu", "pendapatanOrtu",
			"pendidikanOrtu", "alamatOrtu", "rtOrtu", "rwOrtu", "kodePosOrtu", "kecamatanOrtu", "kelurahanOrtu",
			"propinsiOrtu", "kotaOrtu", "paket", "prodi1", "prodi2", "prodi3", "prodi4", "prodi5", "jenjang",
			"statusLulus", "prodiLulus", "nimGenerated", "cetakKartu", "program", "jenisSeleksi", "tanggalDaftar",
			"tahun", "semesterMulai", "tahunAkademik", "gelombangPendaftaran", "tanggalPendaftaran", "agama",
			"semesterMulai", "program", "hp", "namaAyah", "pendidikanAyah", "pekerjaanAyah", "namaIbu", "pendidikanIbu",
			"pekerjaanIbu", "namaUntukIjazah", "noIjazah", "ukuranJaket", "tinggiBadan", "pernahMenetapDiLuarNegeri",
			"beratBadan", "teleponRumah", "suratIzinMengemudi", "kendaraanKuliah", "pernahMemimpinOrganisasi",
			"namaOrganisasi", "hobi", "minatSeni", "kemampuanBahasa1", "kemampuanBahasa2", "kemampuanBahasa3",
			"asalSma", "alamatAsalSma", "asalSmp", "alamatAsalSmp", "asalSd", "alamatAsalSd", "golonganDarah",
			"statusNikah", "jenisKuliah", "statusPembayaran", "nim", "mahasiswa", "merupakanPindahan",
			"pindahanDariKampus", "pindahanDariProdi", "nimLamaSebelumPindah", "pindahDariKampusLamaDiSemester",
			"tanggalPindah", "keteranganPindah", "infoKampusDariMana", "namaTemanInfoKampusDariMana",
			"keteranganInfoKampusDariMana", "pinPassword", "parameterTambahan", "parameterTambahanInds",
			"tanggal_dirubah", "oleh", "keterangan", "telahLogin", "waktuLogin" };

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

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "kuotaDaftar", "khususUsername", "statusAwalMahasiswa",
				"keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(AfiliasiCalonMahasiswa.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, AfiliasiCalonMahasiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		cetakToolbarbutton = Common.cetakDataCustomButton(BiodataCalonMahasiswa.class, new DataCriteria() {

			@SuppressWarnings("unchecked")
			@Override
			public Object initCriteria(boolean order) {
				List<Long> afiliasiCalonMahasiswas = AfiliasiCalonMahasiswaAction.this.initCriteria(true)
						.setProjection(Projections.property("id")).list();
				Session session = HibernateUtil.currentSession();
				return session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(searchTahunAjaran.getSelectedItem() == null
								|| searchTahunAjaran.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("tahunAkademik",
												searchTahunAjaran.getSelectedItem().getValue()))
						.createAlias("afiliasiCalonMahasiswa", "afiliasiCalonMahasiswa")
						.addOrder(Order.asc("afiliasiCalonMahasiswa.nama")).addOrder(Order.asc("noRegistrasi"))
						.add(afiliasiCalonMahasiswas.isEmpty() ? Restrictions.sqlRestriction("false")
								: Restrictions.in("afiliasiCalonMahasiswa.id", afiliasiCalonMahasiswas));
			}
		}, "Download Afiliasi Calon Mahasiswa", "/img/print.png", AfiliasiCalonMahasiswaAction.contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		Common.appendKeToolbar(AfiliasiCalonMahasiswaAction.tampilkanSemuaDownload(searchTahunAjaran), add, comp);
	}

	public static MyToolbarbuttonConfig tampilkanSemuaDownload(final Combobox searchTahunAjaran) {
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(BiodataCalonMahasiswa.class,
				new DataCriteria() {

					@Override
					public Object initCriteria(boolean order) {
						Session session = HibernateUtil.currentSession();
						return session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(searchTahunAjaran.getSelectedItem() == null
										|| searchTahunAjaran.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunAkademik",
														searchTahunAjaran.getSelectedItem().getValue()))
								.createAlias("afiliasiCalonMahasiswa", "afiliasiCalonMahasiswa", Criteria.LEFT_JOIN)
								.createAlias("afiliasiPegawai", "afiliasiPegawai", Criteria.LEFT_JOIN)
								.createAlias("afiliasiMahasiswa", "afiliasiMahasiswa", Criteria.LEFT_JOIN)
								.addOrder(Order.asc("afiliasiCalonMahasiswa.nama"))
								.addOrder(Order.asc("afiliasiPegawai.nama"))
								.addOrder(Order.asc("afiliasiMahasiswa.nama"))

								.add(Restrictions.or(Restrictions.isNotNull("afiliasiCalonMahasiswa.nama"),
										Restrictions.or(Restrictions.isNotNull("afiliasiPegawai.nama"),
												Restrictions.isNotNull("afiliasiMahasiswa.nama"))))

								.addOrder(Order.asc("noRegistrasi"));
					}
				}, "Download Afiliasi Gabungan", "/img/print.png", AfiliasiCalonMahasiswaAction.contentsGabungan);

		return cetakToolbarbutton;
	}

	class AfiliasiCalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final AfiliasiCalonMahasiswa afiliasiCalonMahasiswa = (AfiliasiCalonMahasiswa) arg1;

			(new AfiliasiCalonMahasiswaDetailAction(afiliasiCalonMahasiswa, searchTahunAjaran)).setParent(arg0);

			new Label(afiliasiCalonMahasiswa.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(AfiliasiCalonMahasiswa.class, afiliasiCalonMahasiswa,
					afiliasiCalonMahasiswa.getNama()).setParent(arg0);
			new Label(afiliasiCalonMahasiswa.getKuotaDaftar() + "").setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setWidth("100%");
			arg0.appendChild(hbox);

			SopUtil.tampilAktor(null, afiliasiCalonMahasiswa.getKhususUsername(), "", null, null, hbox);

			Session session = HibernateUtil.currentSession();
			Number d = (Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(searchTahunAjaran.getSelectedItem() == null
							|| searchTahunAjaran.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("afiliasiCalonMahasiswa", afiliasiCalonMahasiswa)).uniqueResult();

			A a;
			(a = new A(d == null ? "0" : Common.numberFormat.get().format(d.intValue()))).setParent(arg0);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					EventListener eventListener = (EventListener) Common
							.cetakDataCustomButton(BiodataCalonMahasiswa.class, new DataCriteriaWithColumn() {

								@Override
								public Object[] initCriteria(boolean order) {

									try {

										Session session = HibernateUtil.currentSession();
										Criteria criteria = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
												.add(searchTahunAjaran.getSelectedItem() == null
														|| searchTahunAjaran.getSelectedItem().getValue() == null
																? Restrictions.sqlRestriction("true")
																: Restrictions.eq("tahunAkademik",
																		searchTahunAjaran.getSelectedItem().getValue()))
												.setProjection(Projections.rowCount())
												.add(Restrictions.eq("afiliasiCalonMahasiswa", afiliasiCalonMahasiswa));

										return new Object[] { criteria, AfiliasiCalonMahasiswaAction.contents };

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
									return null;
								}

							}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
									new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" })
							.getAttribute("eventListener");

					eventListener.onEvent(null);
				}
			};
			a.addEventListener("onClick", eventListener);

			new Label(afiliasiCalonMahasiswa.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(afiliasiCalonMahasiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					afiliasiCalonMahasiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(afiliasiCalonMahasiswa);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, afiliasiCalonMahasiswa, AfiliasiCalonMahasiswaAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new AfiliasiCalonMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		afiliasiCalonMahasiswa = (AfiliasiCalonMahasiswa) obj;
		init(afiliasiCalonMahasiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(AfiliasiCalonMahasiswa afiliasiCalonMahasiswa) {
		this.afiliasiCalonMahasiswa = afiliasiCalonMahasiswa;
		addWindow.setTitle(afiliasiCalonMahasiswa.getId() == null ? "Tambah Afiliasi Calon Mahasiswa" : "Ubah Afiliasi Calon Mahasiswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Afiliasi *"));
		row.appendChild(kode = new Textbox(afiliasiCalonMahasiswa.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Afiliasi *"));
		row.appendChild(nama = new Textbox(afiliasiCalonMahasiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kuota Afiliasi"));
		row.appendChild(kuotaDaftar = new MyIntbox(afiliasiCalonMahasiswa.getKuotaDaftar()));

		final MyFormRow rowUsernameDisposisi = new MyFormRow();
		rowUsernameDisposisi.setParent(rows);
		rowUsernameDisposisi.appendChild(new ais.ui.util.MyLabelConfig("Username pengguna afiliasi"));
		rowUsernameDisposisi.appendChild(khususUsername = new Textbox(afiliasiCalonMahasiswa.getKhususUsername()));
		khususUsername.setWidth("90%");
		khususUsername.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal Calon Mahasiswa"));
		row.appendChild(statusAwalMahasiswa = new Combobox());
		Common.insertComboDanSemua(statusAwalMahasiswa, "nama", StatusAwalMahasiswa.class);
		Common.selectComboItem(statusAwalMahasiswa, afiliasiCalonMahasiswa.getStatusAwalMahasiswa());
		statusAwalMahasiswa.setWidth("90%");
		statusAwalMahasiswa.setReadonly(true);
		Common.initKeterangan(rows, "Pilih semua jika untuk semua status awal mahasiswa atau tidak digunakan");

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Username Pengguna",
				"/img/user_male_add.png");

		final MyFormRow rowAmbilPengguna = new MyFormRow();
		rowAmbilPengguna.setParent(rows);
		rowAmbilPengguna.appendChild(new ais.ui.util.MyLabelConfig(""));
		rowAmbilPengguna.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Tbmuser tbmuser : tbmusers) {
								khususUsername.setValue(khususUsername.getValue()
										+ (khususUsername.getValue().isEmpty() ? tbmuser.getUserId()
												: "," + tbmuser.getUserId()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		Common.initKeterangan(rows, "Jika lebih dari satu pengguna, pisahkan dengan tanda koma (,)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(afiliasiCalonMahasiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kode Afiliasi",
					"Kolom Kode Afiliasi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kode Afiliasi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Afiliasi",
					"Kolom Nama Afiliasi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Afiliasi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (afiliasiCalonMahasiswa.getId() != null) {
			afiliasiCalonMahasiswa = (AfiliasiCalonMahasiswa) session.load(AfiliasiCalonMahasiswa.class,
					afiliasiCalonMahasiswa.getId());
		}

		afiliasiCalonMahasiswa.setKode(kode.getValue());
		afiliasiCalonMahasiswa.setNama(nama.getValue());
		afiliasiCalonMahasiswa.setKhususUsername(khususUsername.getValue().trim());
		afiliasiCalonMahasiswa
				.setStatusAwalMahasiswa((StatusAwalMahasiswa) (statusAwalMahasiswa.getSelectedItem() == null ? null
						: statusAwalMahasiswa.getSelectedItem().getValue()));
		afiliasiCalonMahasiswa.setKuotaDaftar(kuotaDaftar.getValue());
		afiliasiCalonMahasiswa.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, afiliasiCalonMahasiswa);

		return true;
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> ids = new ArrayList<Long>();

		if (!searchcalon.getValue().trim().isEmpty()) {
			ids = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(
							Restrictions.ilike("noRegistrasi", searchcalon.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.ilike("nama", searchcalon.getValue().trim(), MatchMode.ANYWHERE)))
					.add(Restrictions.isNotNull("afiliasiCalonMahasiswa"))
					.setProjection(Projections.groupProperty("afiliasiCalonMahasiswa.id")).list();
		}
		Criteria criteria = session.createCriteria(AfiliasiCalonMahasiswa.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (!ids.isEmpty()) {
			criteria.add(Restrictions.in("id", ids));
		}
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<AfiliasiCalonMahasiswa> afiliasiCalonMahasiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(afiliasiCalonMahasiswa);
		grid.setRowRenderer(new AfiliasiCalonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
