package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PembatasanNilaiIPKUntukPengambilanKRS;
import ais.database.model.Perkuliahan;
import ais.database.model.Program;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PembatasanNilaiIPKUntukPengambilanKRSAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault {
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	protected Combobox searchprogram;

	private PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS;
	private Decimalbox batasTerendahIPK;
	private Decimalbox batasMaksimumIPKYangBolehDiambil;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox program;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Textbox searchnim;
	private Textbox searchnama;
	private Textbox keterangan;
	private Intbox minimumAngkatan;
	private AmbilDataMahasiswaBanbox mahasiswa;

	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;
	private boolean berdasarIps;

	private Column colIpk;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		Common.initPrograms(searchprogram);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		berdasarIps = (Common.bolehKonfigurasi("pembatasan_maksimal_sks_pada_pegambilan_krs_berdasarkan_ip_semester_sebelum_nya"));

		String[] contents = new String[] { "id", "fakultas", "jurusan", "program", "batasTerendahIPK",
				"batasMaksimumIPKYangBolehDiambil", "minimumAngkatan", "semesterPendek", "keterangan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PembatasanNilaiIPKUntukPengambilanKRS.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		if (colIpk != null) { colIpk.setLabel((berdasarIps ? "IP Semester" : "IP Kumulatif")); }

	}

	class PembatasanIPKKRSRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS = (PembatasanNilaiIPKUntukPengambilanKRS) arg1;

			new Label(pembatasanNilaiIPKUntukPengambilanKRS.getFakultas() == null ? ""
					: pembatasanNilaiIPKUntukPengambilanKRS.getFakultas().getNama()).setParent(arg0);
			new Label(pembatasanNilaiIPKUntukPengambilanKRS.getJurusan() == null ? ""
					: pembatasanNilaiIPKUntukPengambilanKRS.getJurusan().getNama()).setParent(arg0);

			new Label(pembatasanNilaiIPKUntukPengambilanKRS.getProgram() == null ? ""
					: pembatasanNilaiIPKUntukPengambilanKRS.getProgram().getNama()).setParent(arg0);

			new Label(pembatasanNilaiIPKUntukPengambilanKRS.getBatasTerendahIPK() == null ? ""
					: pembatasanNilaiIPKUntukPengambilanKRS.getBatasTerendahIPK().toString()).setParent(arg0);
			new Label(pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil() == null ? ""
					: pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil().toString())
							.setParent(arg0);

			new Label(pembatasanNilaiIPKUntukPengambilanKRS.getMinimumAngkatan() == null ? ""
					: pembatasanNilaiIPKUntukPengambilanKRS.getMinimumAngkatan() + "").setParent(arg0);

			new Label(pembatasanNilaiIPKUntukPengambilanKRS.getMahasiswa() == null ? ""
					: pembatasanNilaiIPKUntukPengambilanKRS.getMahasiswa().getNim() + "-"
							+ pembatasanNilaiIPKUntukPengambilanKRS.getMahasiswa().getNama()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(pembatasanNilaiIPKUntukPengambilanKRS.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pembatasanNilaiIPKUntukPengambilanKRS.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(pembatasanNilaiIPKUntukPengambilanKRS);
				}
			});

			final MyCheckboxConfig semesterPendek = new MyCheckboxConfig("SP");
			semesterPendek.setDisabled(!edit);
			semesterPendek.setChecked(pembatasanNilaiIPKUntukPengambilanKRS.getSemesterPendek() != null);
			semesterPendek.setParent(arg0);
			semesterPendek.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pembatasanNilaiIPKUntukPengambilanKRS
							.setSemesterPendek(semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : null);
					Common.refreshSaveOrUpdate(pembatasanNilaiIPKUntukPengambilanKRS);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pembatasanNilaiIPKUntukPengambilanKRS);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(pembatasanNilaiIPKUntukPengambilanKRS);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show("Data ini tidak dapat dihapus");
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
		init(new PembatasanNilaiIPKUntukPengambilanKRS());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS) {
		this.pembatasanNilaiIPKUntukPengambilanKRS = pembatasanNilaiIPKUntukPengambilanKRS;
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

		Rows rows = new Rows();
		rows.setParent(grid);

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas, pembatasanNilaiIPKUntukPengambilanKRS.getFakultas() == null ? null
				: pembatasanNilaiIPKUntukPengambilanKRS.getFakultas());
		if (Common.getCurrentUser().ambilFakultas() != null) {
			Common.selectComboItem(fakultas, Common.getCurrentUser().ambilFakultas());

		}
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		Common.initKeterangan(rows, "Kosongkan " + Common.getBahasaConfig("Fakultas") + " jika untuk semua");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		Common.pilihJurusan(jurusan, pembatasanNilaiIPKUntukPengambilanKRS.getJurusan() == null ? null
				: pembatasanNilaiIPKUntukPengambilanKRS.getJurusan());
		if (Common.getCurrentUser().ambilFakultas() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", Common.getCurrentUser().ambilFakultas()));
		}
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		Common.initKeterangan(rows, "Kosongkan " + Common.getBahasaConfig("Jurusan") + " jika untuk semua");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program = new Combobox());
		Common.insertCombo(program, "namaBaru", Program.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(program, pembatasanNilaiIPKUntukPengambilanKRS.getProgram());
		program.setWidth("90%");

		Common.initKeterangan(rows, "Kosongkan program jika untuk semua");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Batas Ter-rendah " + (berdasarIps ? "IP Semester" : "IP Kumulatif") + " *"));
		row.appendChild(batasTerendahIPK = new Decimalbox(
				new BigDecimal(pembatasanNilaiIPKUntukPengambilanKRS.getBatasTerendahIPK() == null ? 0
						: pembatasanNilaiIPKUntukPengambilanKRS.getBatasTerendahIPK())));
		batasTerendahIPK.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Batas maksimum SKS *"));
		row.appendChild(batasMaksimumIPKYangBolehDiambil = new Decimalbox(
				new BigDecimal(pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil() == null ? 0
						: pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil())));
		batasMaksimumIPKYangBolehDiambil.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai berlaku mulai tahun angkatan  *"));
		row.appendChild(
				minimumAngkatan = new Intbox(pembatasanNilaiIPKUntukPengambilanKRS.getMinimumAngkatan() == null ? 2000
						: pembatasanNilaiIPKUntukPengambilanKRS.getMinimumAngkatan()));
		minimumAngkatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Khusus untuk mahasiswa"));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setWidth("90%");
		mahasiswa.setReadonly(true);
		mahasiswa.setAttribute("mahasiswa", pembatasanNilaiIPKUntukPengambilanKRS.getMahasiswa());
		mahasiswa.setValue(pembatasanNilaiIPKUntukPengambilanKRS.getMahasiswa() == null ? ""
				: pembatasanNilaiIPKUntukPengambilanKRS.getMahasiswa().getNama());
		Common.initKeterangan(rows, "Kosongkan jika bukan untuk mahasiswa tertentu");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(pembatasanNilaiIPKUntukPengambilanKRS.getKeterangan() == null ? ""
				: pembatasanNilaiIPKUntukPengambilanKRS.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(3);

		// row = new MyFormRow();
		//		// row.setParent(rows);
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

		// if (fakultas.getSelectedItem() ==
		// null||fakultas.getSelectedItem().getValue()
		// == null) {
		// MyMessageboxConfig
		// .show("Fakultas" + " harus dipilih",
		// "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// fakultas.focus();
		// return false;
		// }
		// if (jurusan.getSelectedItem() ==
		// null||jurusan.getSelectedItem().getValue()
		// == null) {
		// MyMessageboxConfig.show("Program Studi harus dipilih", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// jurusan.focus();
		// return false;
		// }
		if (batasTerendahIPK.getValue().equals(0)) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Batas ter-rendah",
					"Kolom Batas ter-rendah belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Batas ter-rendah.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			batasTerendahIPK.focus();
			return false;
		}
		if (batasMaksimumIPKYangBolehDiambil.getValue().equals(0)) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Batas maksimum SKS",
					"Kolom Batas maksimum SKS belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Batas maksimum SKS.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			batasMaksimumIPKYangBolehDiambil.focus();
			return false;
		}
		if (minimumAngkatan.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun angkatan",
					"Kolom Tahun angkatan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tahun angkatan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			jurusan.focus();
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pembatasanNilaiIPKUntukPengambilanKRS.getId() != null) {
			pembatasanNilaiIPKUntukPengambilanKRS = (PembatasanNilaiIPKUntukPengambilanKRS) session
					.load(PembatasanNilaiIPKUntukPengambilanKRS.class, pembatasanNilaiIPKUntukPengambilanKRS.getId());
		}

		pembatasanNilaiIPKUntukPengambilanKRS.setMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
		pembatasanNilaiIPKUntukPengambilanKRS.setProgram(
				(Program) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue()));

		pembatasanNilaiIPKUntukPengambilanKRS
				.setBatasMaksimumIPKYangBolehDiambil(batasMaksimumIPKYangBolehDiambil.getValue().intValue());
		pembatasanNilaiIPKUntukPengambilanKRS.setBatasTerendahIPK(batasTerendahIPK.getValue().doubleValue());
		pembatasanNilaiIPKUntukPengambilanKRS
				.setFakultas(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: (Fakultas) fakultas.getSelectedItem().getValue());
		pembatasanNilaiIPKUntukPengambilanKRS
				.setJurusan(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: (Jurusan) jurusan.getSelectedItem().getValue());
		pembatasanNilaiIPKUntukPengambilanKRS.setKeterangan(keterangan.getValue());
		pembatasanNilaiIPKUntukPengambilanKRS.setMinimumAngkatan(minimumAngkatan.getValue());

		if (pembatasanNilaiIPKUntukPengambilanKRS.getId() != null) {
			Common.refreshUpdate(session, pembatasanNilaiIPKUntukPengambilanKRS);
		} else {
			session.save(pembatasanNilaiIPKUntukPengambilanKRS);

		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PembatasanNilaiIPKUntukPengambilanKRS.class)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("program", "program", Criteria.LEFT_JOIN);

		if (order)
			criteria.addOrder(Order.asc("id"));

		criteria.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						|| searchprogram.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("program.nama", searchprogram.getSelectedItem().getValue()))

				.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("mahasiswa.nim", searchnim.getValue().trim()))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("mahasiswa.nama", searchnama.getValue().trim()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PembatasanNilaiIPKUntukPengambilanKRS> pembatasanNilaiIPKUntukPengambilanKRS = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pembatasanNilaiIPKUntukPengambilanKRS);
		grid.setRowRenderer(new PembatasanIPKKRSRenderer());
		grid.setModelCheckMobile(strset);

	}

}
