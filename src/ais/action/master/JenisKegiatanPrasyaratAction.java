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
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisKegiatanPrasyarat;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JenisKegiatanPrasyaratAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchnamaprasyarat;
	private Checkbox searchaktif;

	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisKegiatanPrasyarat jenisKegiatanPrasyarat;
	private MyToolbarbuttonConfig add;
	private Combobox jenisKegiatan;
	private Combobox jenisKegiatanMenjadiPrasyarat;
	private Intbox minSmt;
	private Intbox maxSmt;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox program;
	private Textbox tahunAngkatan;
	private Row rowTa;
	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	private Row rowSmt;
	private Intbox jumlahSemesterHarusLunas;
	private MyDoublebox prosentaseLunas;
	private Row rowJumSmt;
	private MyCheckboxConfig checkJugaSmtYgSama;
	private Combobox jenisKegiatanMenjadiPrasyarat2;
	private Combobox jenisKegiatanMenjadiPrasyarat3;
	private Combobox tahunAkademikMulai;
	private Combobox jenisSemesterMulai;

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

		String[] contents = new String[] { "id", "jenisKegiatan", "jenisKegiatanPrasyarat", "jumlahSemesterHarusLunas",
				"tahunAngkatan", "fakultas", "jurusan", "program", "tahunAkademik", "jenisSemester", "prosentaseLunas",
				"minSmt", "maxSmt", "tahunAkademikMulai", "jenisSemesterMulai", "ta", "aktif", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisKegiatanPrasyarat.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisKegiatanPrasyarat.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class JenisKegiatanPrasyaratRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisKegiatanPrasyarat jenisKegiatanPrasyarat = (JenisKegiatanPrasyarat) arg1;

			RevisiHelper.createNewRevisi(JenisKegiatanPrasyarat.class, jenisKegiatanPrasyarat,
					jenisKegiatanPrasyarat.getJenisKegiatan().getNamaKegiatan()).setParent(arg0);

			String prasyarat = "";
			if (jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat() != null) {
				prasyarat += jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat().getNamaKegiatan();
			}
			if (jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat2() != null) {
				prasyarat += ", atau " + jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat2().getNamaKegiatan();
			}
			if (jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat3() != null) {
				prasyarat += ", atau " + jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat3().getNamaKegiatan();
			}

			new Label(prasyarat).setParent(arg0);

			new Label(jenisKegiatanPrasyarat.getTahunAkademik() == null ? "Semua"
					: jenisKegiatanPrasyarat.getTahunAkademik()).setParent(arg0);
			new Label(jenisKegiatanPrasyarat.getJenisSemester() == null ? "Semua"
					: jenisKegiatanPrasyarat.getJenisSemester()).setParent(arg0);
			new Label(Common.numberFormat.get().format(jenisKegiatanPrasyarat.getJumlahSemesterHarusLunas())).setParent(arg0);

			new Label(jenisKegiatanPrasyarat.getFakultas() == null ? "Semua"
					: jenisKegiatanPrasyarat.getFakultas().getNama()).setParent(arg0);
			new Label(jenisKegiatanPrasyarat.getJurusan() == null ? "Semua"
					: jenisKegiatanPrasyarat.getJurusan().getNama()).setParent(arg0);
			new Label(jenisKegiatanPrasyarat.getProgram() == null ? "Semua" : jenisKegiatanPrasyarat.getProgram())
					.setParent(arg0);
			new Label(jenisKegiatanPrasyarat.getTahunAngkatan()).setParent(arg0);
			new Label(Common.numberFormat.get().format(jenisKegiatanPrasyarat.getProsentaseLunas())).setParent(arg0);
			new Label(Common.numberFormat.get().format(jenisKegiatanPrasyarat.getMinSmt())).setParent(arg0);
			new Label(Common.numberFormat.get().format(jenisKegiatanPrasyarat.getMaxSmt())).setParent(arg0);

			new Label(jenisKegiatanPrasyarat.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisKegiatanPrasyarat.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisKegiatanPrasyarat.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisKegiatanPrasyarat);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisKegiatanPrasyarat, JenisKegiatanPrasyaratAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisKegiatanPrasyarat());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisKegiatanPrasyarat = (JenisKegiatanPrasyarat) obj;
		init(jenisKegiatanPrasyarat);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisKegiatanPrasyarat jenisKegiatanPrasyarat) throws Exception {
		this.jenisKegiatanPrasyarat = jenisKegiatanPrasyarat;
		addWindow.setTitle(jenisKegiatanPrasyarat.getId() == null ? "Tambah Prasyarat Jenis Pembayaran" : "Ubah Prasyarat Jenis Pembayaran");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran *"));
		row.appendChild(jenisKegiatan = Common.initJenisPembayaranMahasiswa(null));
		jenisKegiatan.setWidth("90%");
		jenisKegiatan.setReadonly(true);
		Common.selectComboItem(jenisKegiatan, jenisKegiatanPrasyarat.getJenisKegiatan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prasyarat Jenis Pembayaran *"));
		row.appendChild(jenisKegiatanMenjadiPrasyarat = Common.initJenisPembayaranMahasiswa(null));
		jenisKegiatanMenjadiPrasyarat.setWidth("90%");
		jenisKegiatanMenjadiPrasyarat.setReadonly(true);
		Common.selectComboItem(jenisKegiatanMenjadiPrasyarat, jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Atau Prasyarat Jenis Pembayaran ke II"));
		row.appendChild(jenisKegiatanMenjadiPrasyarat2 = Common.initJenisPembayaranMahasiswa(null));
		jenisKegiatanMenjadiPrasyarat2.setWidth("90%");
		Common.selectComboItem(jenisKegiatanMenjadiPrasyarat2, jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat2());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Atau Prasyarat Jenis Pembayaran ke III"));
		row.appendChild(jenisKegiatanMenjadiPrasyarat3 = Common.initJenisPembayaranMahasiswa(null));
		jenisKegiatanMenjadiPrasyarat3.setWidth("90%");
		Common.selectComboItem(jenisKegiatanMenjadiPrasyarat3, jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat3());

		Tbmuser tbmuser = Common.getCurrentUser();

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas, jenisKegiatanPrasyarat.getFakultas() == null ? tbmuser.ambilFakultas()
				: jenisKegiatanPrasyarat.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		Common.initKeterangan(rows, "Berlaku untuk \"" + Common.getBahasaConfig("Fakultas")
				+ "\" tertantu, kosongkan jika berlaku untuk semua");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, jenisKegiatanPrasyarat.getJurusan() == null ? tbmuser.ambilJurusan()
				: jenisKegiatanPrasyarat.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		Common.initKeterangan(rows, "Berlaku untuk \"" + Common.getBahasaConfig("Jurusan")
				+ "\" tertantu, kosongkan jika berlaku untuk semua");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		program = Common.initPrograms(null);
		Common.selectComboItem(program, jenisKegiatanPrasyarat.getProgram());
		row.appendChild(program);
		program.setWidth("90%");
		Common.initKeterangan(rows, "Berlaku untuk \"Program\" tertantu, kosongkan jika berlaku untuk semua");

		rowTa = new MyFormRow();
		rowTa.setParent(rows);
		rowTa.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		rowTa.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		Common.selectComboItem(tahunAkademik, jenisKegiatanPrasyarat.getTahunAkademik());
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		Common.initKeterangan(rows, "Berlaku untuk \"Tahun Akademik\" tertantu, kosongkan jika berlaku untuk semua");

		rowSmt = new MyFormRow();
		rowSmt.setParent(rows);
		rowSmt.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		rowSmt.appendChild(jenisSemester = new Combobox());
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		jenisSemester.appendChild(comboitem);
		Common.selectComboItem(jenisSemester, jenisKegiatanPrasyarat.getJenisSemester());
		jenisSemester.setWidth("90%");
		jenisSemester.setReadonly(true);

		Common.initKeterangan(rows, "Berlaku untuk \"Semester\" tertantu, kosongkan jika berlaku untuk semua");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(
				checkJugaSmtYgSama = new MyCheckboxConfig("Persyaratan ini juga berlaku di semester yang sama"));
		checkJugaSmtYgSama.setChecked(jenisKegiatanPrasyarat.getCheckJugaSmtYgSama());

		rowJumSmt = new MyFormRow();
		rowJumSmt.setParent(rows);
		rowJumSmt.appendChild(new ais.ui.util.MyLabelConfig("Jumlah semester ke-belakang yang harus dibayar"));
		rowJumSmt.appendChild(
				jumlahSemesterHarusLunas = new Intbox(jenisKegiatanPrasyarat.getJumlahSemesterHarusLunas()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prosentase lunas (%)"));
		row.appendChild(prosentaseLunas = new MyDoublebox(jenisKegiatanPrasyarat.getProsentaseLunas()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan Mahasiswa"));
		row.appendChild(tahunAngkatan = new Textbox(jenisKegiatanPrasyarat.getTahunAngkatan() == null ? ""
				: jenisKegiatanPrasyarat.getTahunAngkatan() + ""));
		tahunAngkatan.setWidth("90%");

		Common.initKeterangan(rows,
				"Berlaku untuk mahasiswa dengan \"Tahun Angkatan\" tertantu, kosongkan jika berlaku untuk semua");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal Semester"));
		row.appendChild(minSmt = new Intbox(jenisKegiatanPrasyarat.getMinSmt()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Maksimal Semester"));
		row.appendChild(maxSmt = new Intbox(jenisKegiatanPrasyarat.getMaxSmt()));

		tahunAkademikMulai = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		tahunAkademikMulai.appendChild(comboitem);
		tahunAkademikMulai = Common.generateTahunAjaranDanSemua(tahunAkademikMulai);

		jenisSemesterMulai = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		jenisSemesterMulai.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemesterMulai.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemesterMulai.appendChild(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berlaku Mulai Tahun Akademik"));
		row.appendChild(tahunAkademikMulai);
		tahunAkademikMulai.setWidth("90%");
		Common.selectComboItem(tahunAkademikMulai, jenisKegiatanPrasyarat.getTahunAkademikMulai());
		tahunAkademikMulai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berlaku Mulai Semester"));
		row.appendChild(jenisSemesterMulai);
		jenisSemesterMulai.setWidth("90%");
		Common.selectComboItem(jenisSemesterMulai, jenisKegiatanPrasyarat.getJenisSemesterMulai());
		jenisSemesterMulai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisKegiatanPrasyarat.getKeterangan()));
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
		if (jenisKegiatan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Pembayaran",
					"Kolom Jenis Pembayaran belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Pembayaran.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisKegiatanMenjadiPrasyarat.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Pembayaran yang menjadi prasyarat",
					"Kolom Jenis Pembayaran yang menjadi prasyarat belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Pembayaran yang menjadi prasyarat.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisKegiatanPrasyarat.getId() != null) {
			jenisKegiatanPrasyarat = (JenisKegiatanPrasyarat) session.load(JenisKegiatanPrasyarat.class,
					jenisKegiatanPrasyarat.getId());

		}

		jenisKegiatanPrasyarat.setJenisKegiatan((JenisKegiatan) jenisKegiatan.getSelectedItem().getValue());
		jenisKegiatanPrasyarat
				.setJenisKegiatanPrasyarat((JenisKegiatan) jenisKegiatanMenjadiPrasyarat.getSelectedItem().getValue());
		jenisKegiatanPrasyarat.setJenisKegiatanPrasyarat2(
				(JenisKegiatan) (jenisKegiatanMenjadiPrasyarat2.getSelectedItem() == null ? null
						: jenisKegiatanMenjadiPrasyarat2.getSelectedItem().getValue()));
		jenisKegiatanPrasyarat.setJenisKegiatanPrasyarat3(
				(JenisKegiatan) (jenisKegiatanMenjadiPrasyarat3.getSelectedItem() == null ? null
						: jenisKegiatanMenjadiPrasyarat3.getSelectedItem().getValue()));

		jenisKegiatanPrasyarat.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null ? null : fakultas.getSelectedItem().getValue()));
		jenisKegiatanPrasyarat.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null ? null : jurusan.getSelectedItem().getValue()));
		jenisKegiatanPrasyarat
				.setProgram((String) (program.getSelectedItem() == null ? null : program.getSelectedItem().getValue()));
		jenisKegiatanPrasyarat.setTahunAkademik(
				(String) (tahunAkademik.getSelectedItem() == null ? null : tahunAkademik.getSelectedItem().getValue()));
		jenisKegiatanPrasyarat.setJenisSemester(
				(String) (jenisSemester.getSelectedItem() == null ? null : jenisSemester.getSelectedItem().getValue()));
		jenisKegiatanPrasyarat.setJumlahSemesterHarusLunas(jumlahSemesterHarusLunas.getValue());
		jenisKegiatanPrasyarat.setProsentaseLunas(prosentaseLunas.getValue());
		jenisKegiatanPrasyarat.setTahunAngkatan(tahunAngkatan.getValue());
		jenisKegiatanPrasyarat.setKeterangan(keterangan.getValue());
		jenisKegiatanPrasyarat.setMinSmt(minSmt.getValue());
		jenisKegiatanPrasyarat.setMaxSmt(maxSmt.getValue());
		jenisKegiatanPrasyarat.setCheckJugaSmtYgSama(checkJugaSmtYgSama.isChecked());

		jenisKegiatanPrasyarat.setTahunAkademikMulai((String) (tahunAkademikMulai.getSelectedItem() == null
				|| tahunAkademikMulai.getSelectedItem().getValue() == null ? null
						: tahunAkademikMulai.getSelectedItem().getValue()));
		jenisKegiatanPrasyarat.setJenisSemesterMulai((String) (jenisSemesterMulai.getSelectedItem() == null ? null
				: jenisSemesterMulai.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, jenisKegiatanPrasyarat);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisKegiatanPrasyarat.class)
				.add(searchaktif == null || searchaktif.isChecked() ? Restrictions.eq("aktif", true) : Restrictions.sqlRestriction("true"))
				.createAlias("jenisKegiatan", "jenisKegiatan")
				.createAlias("jenisKegiatanPrasyarat", "jenisKegiatanPrasyarat");

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("jenisKegiatan.namaKegiatan", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchnamaprasyarat.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("jenisKegiatanPrasyarat.namaKegiatan",
								searchnamaprasyarat.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisKegiatanPrasyarat> jenisKegiatanPrasyarat = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisKegiatanPrasyarat);
		grid.setRowRenderer(new JenisKegiatanPrasyaratRenderer());
		grid.setModelCheckMobile(strset);

	}

	public static boolean checkSyarat(Mahasiswa mahasiswa, JenisKegiatan jenisKegiatan, Integer semester,
			String tahunAkademik, String jenisSemester) throws Exception {
		return checkSyarat(mahasiswa, jenisKegiatan, semester, tahunAkademik, jenisSemester, null);
	}

	@SuppressWarnings("unchecked")
	public static boolean checkSyarat(Mahasiswa mahasiswa, JenisKegiatan jenisKegiatan, Integer semester,
			String tahunAkademik, String jenisSemester, List<String> warnings) throws Exception {

		if (mahasiswa != null && jenisKegiatan != null && semester != null) {

			Session session = HibernateUtil.currentSession();
			List<JenisKegiatanPrasyarat> jenisKegiatanPrasyarats = ConstantValues.simpleList(session
					.createCriteria(JenisKegiatanPrasyarat.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("jenisKegiatan", jenisKegiatan)).add(Restrictions.le("minSmt", semester))
					.add(Restrictions.ge("maxSmt", semester)).add(Restrictions.ge("prosentaseLunas", 0.1))
					.add(Restrictions.or(Restrictions.isNull("tahunAkademik"),
							Restrictions.eq("tahunAkademik", tahunAkademik)))
					.add(Restrictions.or(Restrictions.isNull("jenisSemester"),
							Restrictions.eq("jenisSemester", jenisSemester)))

					.add(Restrictions.or(Restrictions.isNull("tahunAngkatan"),
							Restrictions.ilike("tahunAngkatan", mahasiswa.getTahunangkatan().toString(),
									MatchMode.ANYWHERE)))

					.add(Restrictions.or(Restrictions.isNull("fakultas"),
							Restrictions.eq("fakultas", mahasiswa.getJurusan().getFakultas())))
					.add(Restrictions.or(Restrictions.isNull("jurusan"),
							Restrictions.eq("jurusan", mahasiswa.getJurusan())))
					.add(Restrictions.or(Restrictions.isNull("program"),
							Restrictions.eq("program", mahasiswa.getProgram()))),
					JenisKegiatanPrasyarat.class);
			String messageTidakMemenuhiSyarat = "";
			System.out.println("mahasiswa => " + mahasiswa + ", jenisKegiatan = " + jenisKegiatan + ", semester = "
					+ semester + ", tahunAkademik = " + tahunAkademik + ", jenisKegiatanPrasyarats => "
					+ jenisKegiatanPrasyarats);
			for (JenisKegiatanPrasyarat jenisKegiatanPrasyarat : jenisKegiatanPrasyarats) {

				String id_smt = (tahunAkademik == null || tahunAkademik.trim().isEmpty() ? "0"
						: tahunAkademik.split("/")[0])
						+ (jenisSemester.trim().isEmpty() ? "0" : jenisSemester.equals(Perkuliahan.GENAP) ? "2" : "1");
				Integer ta = 0;
				try {
					ta = Integer.parseInt(id_smt.trim());
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

				if (jenisKegiatanPrasyarat.getTa() == null || jenisKegiatanPrasyarat.getTa() <= ta) {

					for (int smt = semester - (jenisKegiatanPrasyarat.getCheckJugaSmtYgSama() ? 0 : 1); smt >= (semester
							- jenisKegiatanPrasyarat.getJumlahSemesterHarusLunas()); smt--) {
						if (smt >= jenisKegiatanPrasyarat.getMinSmt()
								&& mahasiswa.getPindahKeKampusIniMasukSemester() < smt
								&& mahasiswa.getPindahKeProdiIniMasukSemester() < smt) {

							Double per = 0.0;
							Boolean memenuhiSyarat = false;

							Kegiatan kegiatan = mahasiswa.ambilKegiatans(smt,
									jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat());

							per = kegiatan == null ? 0.0 : kegiatan.getPersentaseLunas();

							memenuhiSyarat = jenisKegiatanPrasyarat.getProsentaseLunas() <= per;
							if (!memenuhiSyarat && jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat2() != null) {
								kegiatan = mahasiswa.ambilKegiatans(smt,
										jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat2());
								per = kegiatan == null ? 0.0 : kegiatan.getPersentaseLunas();
							}

							memenuhiSyarat = jenisKegiatanPrasyarat.getProsentaseLunas() <= per;
							if (!memenuhiSyarat && jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat3() != null) {
								kegiatan = mahasiswa.ambilKegiatans(smt,
										jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat3());
								per = kegiatan == null ? 0.0 : kegiatan.getPersentaseLunas();
							}

							if (!memenuhiSyarat && mahasiswa.getAlihProdiMahasiswa() != null) {
								Mahasiswa mahasiswaPindahan = mahasiswa.getAlihProdiMahasiswa();
								kegiatan = mahasiswaPindahan.ambilKegiatans(smt,
										jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat());

								per = kegiatan == null ? 0.0 : kegiatan.getPersentaseLunas();

								memenuhiSyarat = jenisKegiatanPrasyarat.getProsentaseLunas() <= per;
								if (!memenuhiSyarat && jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat2() != null) {
									kegiatan = mahasiswaPindahan.ambilKegiatans(smt,
											jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat2());
									per = kegiatan == null ? 0.0 : kegiatan.getPersentaseLunas();
								}

								memenuhiSyarat = jenisKegiatanPrasyarat.getProsentaseLunas() <= per;
								if (!memenuhiSyarat && jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat3() != null) {
									kegiatan = mahasiswaPindahan.ambilKegiatans(smt,
											jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat3());
									per = kegiatan == null ? 0.0 : kegiatan.getPersentaseLunas();
								}
							}

							System.out.println("mahasiswa => " + mahasiswa + ", prasyarat1 => "
									+ jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat() + ", prasyarat2 => "
									+ jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat2() + ", prasyarat3 => "
									+ jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat3() + ", dibayar persen => " + per
									+ ", memenuhiSyarat => " + memenuhiSyarat);

							if (!memenuhiSyarat) {

								String prasyarat = "";
								if (jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat() != null) {
									prasyarat += jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat().getNamaKegiatan();
								}
								if (jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat2() != null) {
									prasyarat += ", atau "
											+ jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat2().getNamaKegiatan();
								}
								if (jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat3() != null) {
									prasyarat += ", atau "
											+ jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat3().getNamaKegiatan();
								}

								String me = "\n\nSyarat untuk membayar biaya \"" + jenisKegiatan.getNamaKegiatan()
										+ "\" adalah harus telah membayar biaya \"" + prasyarat + "\" sebesar "
										+ Common.numberFormat.get().format(jenisKegiatanPrasyarat.getProsentaseLunas())
										+ "% di semester " + smt + ". Nilai yang telah dibayarkan mahasiswa dengan nim "
										+ mahasiswa.getNim() + " dan nama " + mahasiswa.getNama() + " adalah sebesar "
										+ Common.numberFormat.get().format(per)
										+ "%. Untuk dapat melakukan pembayaran tersebut, pilihlah semester " + smt
										+ " dan jenis pembayaran \""
										+ jenisKegiatanPrasyarat.getJenisKegiatanPrasyarat().getNamaKegiatan()
										+ "\" di menu pembayaran ini.";

								if (warnings != null) {
									warnings.add(me);
								} else {
									messageTidakMemenuhiSyarat += me;
								}
							}
						}
					}
				}
			}

			try {
				if (warnings != null && !warnings.isEmpty()) {
					return false;
				} else if (!messageTidakMemenuhiSyarat.isEmpty()) {
					MyMessageboxConfig.show(
							"Terdapat persyaratan pembayaran yang harus diselesaikan terlebih dahulu, yaitu sbb:"
									+ messageTidakMemenuhiSyarat,
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/JenisKegiatanPrasyaratAction.java:705");
				// TODO: handle exception
			}
		}

		return true;
	}

}
