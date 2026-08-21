package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
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

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.DetailUjianHelper;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.UjianDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Matakuliah;
import ais.database.model.PenjelasanBankSoal;
import ais.database.model.Sertifikat;
import ais.database.model.SyaratUjian;
import ais.database.model.Tbmuser;
import ais.database.model.Ujian;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class UjianAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchjenis;
	private AmbilDataMatakuliahBanbox searchmatakuliah;
	private AmbilDataDosenBanbox searchdosen;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox keterangan;
	private MyDoublebox nilaiLulus;
	private Combobox level;
	private Combobox fakultas;
	private Combobox jurusan;

	// private MyCheckboxConfig dibatasiWaktu;
	// private MyTimebox lama;
	//
	// private MyDatebox persenUjian;
	// private MyDatebox sampaiUjian;

	private AmbilDataDosenBanbox dosen;
	private AmbilDataMatakuliahBanbox matakuliah;

	private boolean edit = false;
	private boolean delete = false;

	private Ujian ujian;
	private MyToolbarbuttonConfig add;
	private EventListener eventListener;

	private MyLabelConfig label_dosen;
	private MyLabelConfig label_fakultas;

	private Column col_dosen;
	private Column col_mat;
	private Column col_fak;
	private Column col_jurusan;

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

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilYayasan() != null) {
			if (label_dosen != null) {
				label_dosen.getParent().setVisible(false);
			}
			if (label_fakultas != null) {
				label_fakultas.setVisible(false);
			}
			if (searchfakultas != null) {
				searchfakultas.setVisible(false);
			}
			if (searchjurusan != null) {
				searchjurusan.setVisible(false);
			}
			if (col_dosen != null) {
				col_dosen.setWidth("0%");
			}
			if (col_mat != null) {
				col_mat.setWidth("0%");
			}
			if (col_fak != null) {
				col_fak.setWidth("0%");
			}
			if (col_jurusan != null) {
				col_jurusan.setWidth("0%");
			}
		}

		searchmatakuliah.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});
		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen mydosen = tbmuser.ambilDosen();
			searchdosen.setValue(mydosen.getNama());
			searchdosen.setAttribute("myValue", mydosen);
			searchdosen.setDisabled(true);
		}

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		MyComboitemConfig comboitem = new MyComboitemConfig(BankSoal.PILIHAN_GANDA);
		if (comboitem != null) { comboitem.setValue(BankSoal.PILIHAN_GANDA); }
		searchjenis.appendChild(comboitem);
		comboitem = new MyComboitemConfig(BankSoal.ESAY);
		if (comboitem != null) { comboitem.setValue(BankSoal.ESAY); }
		searchjenis.appendChild(comboitem);

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
	        FilterLanjutHelper.setup(comp);
}

	
	private Combobox sertifikatCombo;
	private Textbox kode;

	private Tabpanel sertifikat;
	public void onSertifikat(Event event) {
		if (sertifikat.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(sertifikat);
			MyInclude iframe = new MyInclude("/pages/master/sertifikat.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel syaratUjianTab;
	private Combobox syaratUjian;
	private MyCkEditor tatatertibUjian;

	public void onSyaratUjianTab(Event event) {
		if (syaratUjianTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(syaratUjianTab);
			MyInclude iframe = new MyInclude("/pages/master/syarat_ujian.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel kuotaUjianTab;
	private MyCheckboxConfig tampilanHurufDiPilihanJawaban;
	private Combobox jenisKoreksi;
	private String diperuntukkan = null;
	private boolean pt;
	private boolean ya;
	private Combobox matapelajaran;
	private AmbilDataGuruBanbox guru;
	private Combobox yayasan;
	private Combobox sekolah;

	public void onKuotaUjianTab(Event event) {
		if (kuotaUjianTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kuotaUjianTab);
			MyInclude iframe = new MyInclude("/pages/master/hasil_ujian_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	class UjianRenderer extends ais.ui.util.MyRowRenderer {

		private DetailUjianHelper detailUjianHelper = new DetailUjianHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Ujian ujian = (Ujian) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						detailUjianHelper.display(ujian, detail, true, delete);
					}

				}
			});

			new Label(ujian.getKode()).setParent(arg0);
			new Label(ujian.getNama()).setParent(arg0);
			new Label(Common.getBahasaConfig(ujian.getJenis()) + " / " + Common.getBahasaConfig(ujian.getLevel())
					+ " / " + Common.numberFormat.get().format(ujian.getNilaiLulus())).setParent(arg0);
			new Label(ujian.getSertifikat() == null ? "" : ujian.getSertifikat().getNama()).setParent(arg0);
			new Label(ujian.getSyaratUjian() == null ? "" : ujian.getSyaratUjian().getNama()).setParent(arg0);

			new Label(ujian.getDosen() == null ? "Tidak Ada" : ujian.getDosen().getNama()).setParent(arg0);
			new Label(ujian.getMatakuliah() == null ? "Tidak Ada" : ujian.getMatakuliah().getNama()).setParent(arg0);
			new Label(ujian.getFakultas() == null ? "Semua" : ujian.getFakultas().getNama()).setParent(arg0);
			new Label(ujian.getJurusan() == null ? "Semua" : ujian.getJurusan().getNama()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(ujian.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					ujian.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(ujian);
				}
			});

			new Label(ujian.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(ujian);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			/* Riwayat perubahan + pemulihan. Ujian yang terhapus atau tersunting
			 * keliru sulit disusun ulang secara manual -- satu ujian dapat memuat
			 * puluhan soal. Dialognya memakai RevisiApiHelper, mesin audit yang sama
			 * dengan POS dan JSP, sehingga aturan pemulihannya tidak mungkin berbeda
			 * antar kanal. Batas kewenangan (hanya admin) ditegakkan helper itu. */
			button = new MyToolbarbuttonConfig("", "/img/svg/history.svg");
			button.setTooltiptext("Riwayat Perubahan");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					ais.action.master.helper.RiwayatRevisiZkDialog.buka("ujian", ujian.getId(),
							ujian.getNama() == null ? "" : ujian.getNama());
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
											Common.refreshDeleteFlush(ujian);

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus karena digunakan untuk transaksi");
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
		init(new Ujian());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static void onAddExternal(Event event, EventListener eventListener, Ujian ujian, String diperuntukkan)
			throws Exception {
		UjianAction ujianAction = new UjianAction();
		ujianAction.eventListener = eventListener;
		ujianAction.diperuntukkan = diperuntukkan;
		ujianAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ujianAction.addWindow);
		ujianAction.addWindow.setHeight("95%");
		ujianAction.addWindow.setWidth("950px");

		ujianAction.init(ujian);

		ujianAction.addWindow.setVisible(true);
		ujianAction.addWindow.onModal();
	}

	private Borderlayout initMain(final Ujian ujian) throws Exception {

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

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
		column.setWidth("70%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Ujian"));
		row.appendChild(kode = new Textbox());
		kode.setValue(ujian.getKode());
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ujian *"));
		row.appendChild(nama = new Textbox());
		nama.setValue(ujian.getNama());
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Koreksi *"));
		row.appendChild(jenisKoreksi = new Combobox());

		MyComboitemConfig comboitem = new MyComboitemConfig(PenjelasanBankSoal.KOREKSI_OTOMATIS);
		comboitem.setValue(PenjelasanBankSoal.KOREKSI_OTOMATIS);
		jenisKoreksi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(PenjelasanBankSoal.KOREKSI_MANUAL);
		comboitem.setValue(PenjelasanBankSoal.KOREKSI_MANUAL);
		jenisKoreksi.appendChild(comboitem);

		Common.selectComboItem(jenisKoreksi, ujian.getJenisKoreksi());
		jenisKoreksi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(
				tampilanHurufDiPilihanJawaban = new MyCheckboxConfig("Tampilkan Huruf di pilihan jawaban soal"));
		tampilanHurufDiPilihanJawaban.setChecked(ujian.getTampilanHurufDiPilihanJawaban());

		EventListener my = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tampilanHurufDiPilihanJawaban.getParent().setVisible(jenisKoreksi.getSelectedItem() != null
						&& jenisKoreksi.getSelectedItem().getValue() != null
						&& jenisKoreksi.getSelectedItem().getValue().equals(PenjelasanBankSoal.KOREKSI_OTOMATIS));
			}
		};
		my.onEvent(null);
		jenisKoreksi.addEventListener("onChange", my);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Level"));
		row.appendChild(level = new Combobox());
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua Level");
		comboitem.setValue("Semua Level");
		level.appendChild(comboitem);

		for (int i = 1; i <= 10; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Level " + i);
			comboitem.setValue("Level " + i);
			level.appendChild(comboitem);
		}

		Common.selectComboItem(level, ujian.getLevel());
		level.setWidth("90%");
		level.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Lulus"));
		row.appendChild(nilaiLulus = new MyDoublebox(ujian.getNilaiLulus()));
		nilaiLulus.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sertifikat"));
		row.appendChild(sertifikatCombo = new Combobox());
		Common.insertComboDanSemua(sertifikatCombo, new String[] { "nama" }, "keterangan", Sertifikat.class,
				"== Tanpa Sertifikat ==");
		Common.selectComboItem(sertifikatCombo, ujian.getSertifikat());
		sertifikatCombo.setWidth("90%");
		sertifikatCombo.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Syarat Mengikuti Ujian"));
		row.appendChild(syaratUjian = new Combobox());
		Common.insertComboDanSemua(syaratUjian, new String[] { "nama" }, "keterangan", SyaratUjian.class,
				"== Tanpa Syarat Mengikuti Ujian ==",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(syaratUjian, ujian.getSyaratUjian(), true);
		syaratUjian.setWidth("90%");
		syaratUjian.setReadonly(true);

		final Row rowSyarat = Common.initKeteranganSatuKolom(rows, "Persyaratan ini hanya boleh diubah oleh admin");

		EventListener listenerSyarat = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Tbmuser tbmuser = Common.getCurrentUser();

				SyaratUjian syaratUjian = (SyaratUjian) (UjianAction.this.syaratUjian.getSelectedItem() == null ? null
						: UjianAction.this.syaratUjian.getSelectedItem().getValue());
				UjianAction.this.syaratUjian
						.setDisabled(syaratUjian != null && syaratUjian.getHanyaBolehDiubahOlehAdmin()
								&& (tbmuser == null || tbmuser.ambilDosen() != null || tbmuser.getMahasiswa() != null));
				rowSyarat.setVisible(syaratUjian != null && syaratUjian.getHanyaBolehDiubahOlehAdmin());
			}
		};
		listenerSyarat.onEvent(null);
		syaratUjian.addEventListener("onChange", listenerSyarat);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tata Tertib Ikut Ujian"));
		row.appendChild(tatatertibUjian = new MyCkEditor());
		tatatertibUjian.setWidth("90%");
		tatatertibUjian.setHeight("120px");
		tatatertibUjian.setValue(ujian.getTatatertibUjian());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(ujian.getKeterangan() == null ? "" : ujian.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setMaxlength(254);
		Tbmuser tbmuser = Common.getCurrentUser();
		boolean tampil = Common.bolehKonfigurasi("tampilkan_pilihan_prodi_dan_maakuliah_saat_buat_ujian") && tbmuser.ambilYayasan() == null;

		if (tampil && pt) {
			Common.initKeterangan(rows, "Kosongkan " + Common.getBahasaConfig("Fakultas") + ", "
					+ Common.getBahasaConfig("Jurusan")
					+ ", Dosen Pembuat, dan untuk matakuliah jika ujian ini bersifat umum dan dapat berlaku untuk semua");
		}

		row = new MyFormRow();
		row.setVisible(pt);
		row.setVisible(tampil);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas,
				ujian.getFakultas() == null ? Common.getCurrentUser().ambilFakultas() : ujian.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setVisible(pt);
		row.setVisible(tampil);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan,
				ujian.getJurusan() == null ? Common.getCurrentUser().ambilJurusan() : ujian.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setVisible(tampil);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembuat"));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setValue(ujian.getDosen() == null ? "" : ujian.getDosen().getNama());
		dosen.setAttribute("myValue", ujian.getDosen());
		dosen.setWidth("90%");

		if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen mydosen = tbmuser.ambilDosen();
			dosen.setValue(mydosen.getNama());
			dosen.setAttribute("myValue", mydosen);
			dosen.setDisabled(true);
		}

		row = new MyFormRow();
		row.setVisible(pt);
		row.setVisible(tampil);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Untuk matakuliah"));
		row.appendChild(matakuliah = new AmbilDataMatakuliahBanbox());
		matakuliah.setValue(ujian.getMatakuliah() == null ? "" : ujian.getMatakuliah().getNama());
		matakuliah.setAttribute("matakuliah", ujian.getMatakuliah());
		matakuliah.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(null, null, yayasan, sekolah);
		Sekolah sekolah1 = SekolahUtil.getSekolah();
		if (sekolah1 != null && ujian.getSekolah() == null) {
			ujian.setSekolah(sekolah1);
		}

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		Common.selectComboItem(yayasan,
				ujian.getYayasan() == null ? Common.getCurrentUser().ambilYayasan() : ujian.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		Common.pilihSekolah(sekolah,
				ujian.getSekolah() == null ? Common.getCurrentUser().ambilSekolah() : ujian.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru Pembuat"));
		row.appendChild(guru = new AmbilDataGuruBanbox());
		guru.setValue(ujian.getGuru() == null ? "" : ujian.getGuru().getNama());
		guru.setAttribute("myValue", ujian.getGuru());
		guru.setAttribute("guru", ujian.getGuru());
		guru.setWidth("90%");

		if (tbmuser != null && tbmuser.ambilGuru() != null) {
			Guru mydosen = tbmuser.ambilGuru();
			guru.setValue(mydosen.getNama());
			guru.setAttribute("myValue", mydosen);
			guru.setAttribute("guru", mydosen);
			guru.setDisabled(true);
		}

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matapelajaran terkait"));
		row.appendChild(matapelajaran = new Combobox());
		matapelajaran.setWidth("90%");

		EventListener mkListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);

				Common.insertCombo(matapelajaran, new String[] { "nama", "jenisPenilaian" }, Matapelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				matapelajaran.setReadonly(true);

				Common.selectComboItem(matapelajaran, ujian.getMatapelajaran());
			}
		};

		sekolah.addEventListener("onChange", mkListener);
		Common.createDefaultTimer(mkListener);

		return borderlayout;
	}

	private void init(Ujian ujian) throws Exception {

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		this.ujian = ujian;
		addWindow.setTitle(ujian.getId() == null ? "Tambah Ujian" : "Ubah Ujian");
		Common.clear(addWindow);
		Borderlayout borderlayout = initMain(ujian);

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
						eventListener.onEvent(new Event("", addWindow, UjianAction.this.ujian));
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data ujian",
					"Kolom Nama ujian belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama ujian.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisKoreksi.getSelectedItem() == null || jenisKoreksi.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Koreksi Soal",
					"Kolom Jenis Koreksi Soal belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Koreksi Soal.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		// Simpan dalam transaksi eksplisit yang di-commit sendiri agar perubahan PASTI tersimpan
		// (tidak bergantung pada commit/rollback sesi request). Pola sama dengan save kritis lain.
		org.hibernate.Session sessionSimpan = null;
		org.hibernate.Transaction tx = null;
		try {
			sessionSimpan = HibernateUtil.openSession();
			tx = sessionSimpan.beginTransaction();

			// Muat ulang di sesi simpan ini supaya field yang tidak ada di form tetap utuh.
			if (ujian.getId() != null) {
				Ujian managed = (Ujian) sessionSimpan.get(Ujian.class, ujian.getId());
				if (managed != null) {
					ujian = managed;
				}
			}

			ujian.setDiperuntukkan(diperuntukkan);
			ujian.setDosen((Dosen) dosen.getAttribute("myValue"));
			ujian.setMatakuliah((Matakuliah) matakuliah.getAttribute("matakuliah"));

			ujian.setJenisKoreksi((String) jenisKoreksi.getSelectedItem().getValue());
			ujian.setFakultas(
					(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
							: fakultas.getSelectedItem().getValue()));
			ujian.setJurusan(
					(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
							: jurusan.getSelectedItem().getValue()));
			ujian.setNama(nama.getValue());
			ujian.setKeterangan(keterangan.getValue());
			ujian.setLevel((String) level.getSelectedItem().getValue());
			ujian.setNilaiLulus(nilaiLulus.getValue());
			ujian.setSertifikat((Sertifikat) (sertifikatCombo.getSelectedItem() == null ? null
					: sertifikatCombo.getSelectedItem().getValue()));
			ujian.setSyaratUjian((SyaratUjian) (syaratUjian.getSelectedItem() == null ? null
					: syaratUjian.getSelectedItem().getValue()));
			ujian.setKode(kode.getValue());
			ujian.setTatatertibUjian(tatatertibUjian.getValue());
			ujian.setMatapelajaran((Matapelajaran) (matapelajaran.getSelectedItem() == null ? null
					: matapelajaran.getSelectedItem().getValue()));

			ujian.setYayasan(
					(Yayasan) (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null ? null
							: yayasan.getSelectedItem().getValue()));
			ujian.setSekolah(
					(Sekolah) (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null ? null
							: sekolah.getSelectedItem().getValue()));
			ujian.setGuru((Guru) guru.getAttribute("myValue"));

			ujian.setTampilanHurufDiPilihanJawaban(tampilanHurufDiPilihanJawaban.isChecked());

			if (ujian.getId() == null) {
				sessionSimpan.save(ujian);
			}
			tx.commit();

			// Penyimpanan dilakukan di sessionSimpan (openSession) terpisah. Sesi REQUEST
			// (currentSession) masih memegang salinan Ujian LAMA di L1 cache, sehingga
			// onSearchDefault & dialog yang dibuka ulang menampilkan nilai lama -> terkesan
			// "tidak tersimpan". Buang (evict) salinan lama itu agar reload mengambil data baru.
			try {
				Long savedId = ujian.getId();
				if (savedId != null) {
					org.hibernate.Session req = HibernateUtil.currentSession();
					Object stale = req.get(Ujian.class, savedId);
					if (stale != null) {
						req.evict(stale);
					}
				}
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/UjianAction.java:838");
			}
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/UjianAction.java:844");
				}
			}
			Common.tampilErrorJikaAdmin(e);
			MyMessageboxConfig.show(
					"Perubahan ujian gagal disimpan." + (e.getMessage() == null ? "" : " (" + e.getMessage() + ")"),
					"Gagal Menyimpan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		} finally {
			if (sessionSimpan != null && sessionSimpan.isOpen()) {
				try {
					sessionSimpan.close();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/UjianAction.java:856");
				}
			}
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Ujian.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria.add((searchdosen == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("dosen", searchdosen.getAttribute("myValue"))))
				.add((searchmatakuliah == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmatakuliah.getAttribute("matakuliah") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("matakuliah", searchmatakuliah.getAttribute("matakuliah"))))

				.add(searchjenis.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenis", searchjenis.getSelectedItem().getValue()))
				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchdosen == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);
		List<Ujian> ujian = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(ujian);
		grid.setRowRenderer(new UjianRenderer());
		grid.setModelCheckMobile(strset);

	}

}
