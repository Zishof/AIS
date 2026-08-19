package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
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
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusPertemuan;
import ais.database.model.SyaratUjian;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class SyaratUjianAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;
	private Checkbox searchaktif;
	private SyaratUjian syaratUjian;
	private MyToolbarbuttonConfig add;
	private Rows subrows;
	private Textbox kodeItemBiaya;
	private MyDoublebox minimalAngkaKredit;
	private MyDoublebox minimalIpk;
	private MyIntbox minimalSks;
	private Combobox program;
	private MyTextbox kodeMatakuliahDan;
	private MyTextbox kodeMatakuliah;
	private MyCheckboxConfig tidakWajibMengambilMkTertentu;
	private Textbox kodeItemBiayaOr;
	private MyCheckboxConfig sekaliBayar;
	private MyDoublebox minimalPersenKehadiran;
	private MyIntbox minimalSmt;
	private MyIntbox maksimalSmt;

	private Combobox jurusan;
	private Combobox fakultas;
	private Intbox minimalAngkatan;
	private Intbox maksimalAngkatan;
	private Combobox statusPertemuan;
	private Combobox jenjang;
	private Combobox tahunAkademik;
	private Combobox semester;
	private Textbox alertCustom;
	private MyCheckboxConfig tampilkanHanyaAlertCustom;
	private Combobox statusAwalMahasiswa;

//	private Combobox searchfakultas;
//	private Combobox searchjurusan;

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

		String[] contents = new String[] { "id", "nama", "keterangan", "syaratPembayaran", "kodeMatakuliah",
				"kodeMatakuliahDan", "minimalPersenKehadiran", "minimalSks", "minimalIpk", "minimalAngkaKredit",
				"kodeItemBiaya", "kodeItemBiayaOr", "tidakWajibMengambilMkTertentu", "sekaliBayar", "krs", "uts", "uas",
				"nilai", "hanyaBolehDiubahOlehAdmin", "minimalSmt", "maksimalSmt", "minimalAngkatan",
				"maksimalAngkatan", "fakultas", "jurusan", "program", "tahunAkademik", "semester",
				"tampilkanHanyaAlertCustom", "alertCustom", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, SyaratUjian.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class SyaratUjianRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final SyaratUjian syaratUjian = (SyaratUjian) arg1;

			MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setOpen(true);
			detail.appendChild(initJadwal(syaratUjian, delete));

			Vbox a;
			(a = RevisiHelper.createNewRevisi(SyaratUjian.class, syaratUjian, syaratUjian.getNama())).setParent(arg0);

			if (syaratUjian.getFakultas() != null) {
				new Label(syaratUjian.getFakultas().getNama()).setParent(a);
			}
			if (syaratUjian.getJurusan() != null) {
				new Label(syaratUjian.getJurusan().getNama()).setParent(a);
			}
			if (syaratUjian.getJenjang() != null) {
				new Label(syaratUjian.getJenjang().getNama()).setParent(a);
			}
			if (syaratUjian.getProgram() != null) {
				new Label(syaratUjian.getProgram()).setParent(a);
			}
			if (syaratUjian.getStatusPertemuan() != null) {
				new Label(syaratUjian.getStatusPertemuan().getNama()).setParent(a);
			}
			if (syaratUjian.getStatusAwalMahasiswa() != null) {
				new Label(syaratUjian.getStatusAwalMahasiswa().getNama()).setParent(a);
			}

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			for (String kode : syaratUjian.getKodeMatakuliah().split(",")) {
				if (!kode.trim().isEmpty()) {

					Object[] nama = (Object[]) HibernateUtil.currentSession().createCriteria(Matakuliah.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setProjection(Projections.projectionList().add(Projections.property("kode"))
									.add(Projections.property("nama")))
							.add(Restrictions.or(Restrictions.ilike("nama", kode.trim(), MatchMode.EXACT),
									Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT)))
							.setMaxResults(1).uniqueResult();
					if (nama != null && nama.length > 1) {
						new MyLabelKecil(nama[0] + " - " + nama[1] + " (atau)").setParent(vbox);
					}
				}
			}

			for (String kode : syaratUjian.getKodeMatakuliahDan().split(",")) {
				if (!kode.trim().isEmpty()) {

					Object[] nama = (Object[]) HibernateUtil.currentSession().createCriteria(Matakuliah.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setProjection(Projections.projectionList().add(Projections.property("kode"))
									.add(Projections.property("nama")))
							.add(Restrictions.or(Restrictions.ilike("nama", kode.trim(), MatchMode.EXACT),
									Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT)))
							.setMaxResults(1).uniqueResult();
					if (nama != null && nama.length > 1) {
						new MyLabelKecil(nama[0] + " - " + nama[1] + " (dan)").setParent(vbox);
					}
				}
			}

			new Label(Common.numberFormat.get().format(syaratUjian.getMinimalSks()) + " / "
					+ Common.numberFormat.get().format(syaratUjian.getMinimalIpk()) + " / "
					+ Common.numberFormat.get().format(syaratUjian.getMinimalAngkaKredit()) + " / "
					+ Common.numberFormat.get().format(syaratUjian.getMinimalPersenKehadiran())).setParent(arg0);
			new Label(syaratUjian.getKodeItemBiaya()).setParent(arg0);
			new Label(syaratUjian.getKodeItemBiayaOr()).setParent(arg0);
			new Label(syaratUjian.getAlertCustom() + " " + syaratUjian.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(syaratUjian.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					syaratUjian.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(syaratUjian);
				}
			});

			final MyCheckboxConfig hanyaBolehDiubahOlehAdmin = new MyCheckboxConfig("Hanya Admin");
			hanyaBolehDiubahOlehAdmin.setDisabled(!edit);
			hanyaBolehDiubahOlehAdmin.setChecked(syaratUjian.getHanyaBolehDiubahOlehAdmin());
			hanyaBolehDiubahOlehAdmin.setParent(arg0);
			hanyaBolehDiubahOlehAdmin.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					syaratUjian.setHanyaBolehDiubahOlehAdmin(hanyaBolehDiubahOlehAdmin.isChecked());
					Common.refreshSaveOrUpdate(syaratUjian);
				}
			});

			final MyCheckboxConfig krs = new MyCheckboxConfig("KRS");
			krs.setDisabled(!edit);
			krs.setChecked(syaratUjian.getKrs());
			krs.setParent(arg0);
			krs.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					syaratUjian.setKrs(krs.isChecked());
					Common.refreshSaveOrUpdate(syaratUjian);
				}
			});

			final MyCheckboxConfig uts = new MyCheckboxConfig("UTS");
			uts.setDisabled(!edit);
			uts.setChecked(syaratUjian.getUts());
			uts.setParent(arg0);
			uts.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					syaratUjian.setUts(uts.isChecked());
					Common.refreshSaveOrUpdate(syaratUjian);
				}
			});

			final MyCheckboxConfig uas = new MyCheckboxConfig("UAS");
			uas.setDisabled(!edit);
			uas.setChecked(syaratUjian.getUas());
			uas.setParent(arg0);
			uas.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					syaratUjian.setUas(uas.isChecked());
					Common.refreshSaveOrUpdate(syaratUjian);
				}
			});

			final MyCheckboxConfig nilai = new MyCheckboxConfig("KHS");
			nilai.setDisabled(!edit);
			nilai.setChecked(syaratUjian.getNilai());
			nilai.setParent(arg0);
			nilai.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					syaratUjian.setNilai(nilai.isChecked());
					Common.refreshSaveOrUpdate(syaratUjian);
				}
			});

			final MyCheckboxConfig ujian = new MyCheckboxConfig("Ujian");
			ujian.setDisabled(!edit);
			ujian.setChecked(syaratUjian.getBerlakuUntukSemuaUjian());
			ujian.setParent(arg0);
			ujian.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					syaratUjian.setBerlakuUntukSemuaUjian(ujian.isChecked());
					Common.refreshSaveOrUpdate(syaratUjian);
				}
			});

			final MyCheckboxConfig tugas = new MyCheckboxConfig("Tugas");
			tugas.setDisabled(!edit);
			tugas.setChecked(syaratUjian.getBerlakuUntukSemuaTugas());
			tugas.setParent(arg0);
			tugas.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					syaratUjian.setBerlakuUntukSemuaTugas(tugas.isChecked());
					Common.refreshSaveOrUpdate(syaratUjian);
				}
			});

			// Kolom aksi dirapikan ke menu kebab (...) via UIHelper.buatBarisAksi supaya
			// konsisten dengan layar lain dan kolomnya menjadi kecil. Hbox bawaan
			// copyEditDeleteButtons diratakan satu level agar tiap tombol masuk popup.
			org.zkoss.zul.Hbox aksiHbox = Common.copyEditDeleteButtons(edit, delete, syaratUjian, SyaratUjianAction.this);
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>(aksiHbox.getChildren());
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new SyaratUjian());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		syaratUjian = (SyaratUjian) obj;
		init(syaratUjian);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "deprecation" })
	private void init(SyaratUjian syaratUjian) {
		this.syaratUjian = syaratUjian;
		addWindow.setTitle(syaratUjian.getId() == null ? "Tambah Syarat Ujian" : "Ubah Syarat Ujian");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Syarat Ujian"));
		row.appendChild(nama = new Textbox(syaratUjian.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Informasi kustom yang ditampilkan bagi yang tidak memenuhi syarat"));
		row.appendChild(alertCustom = new Textbox(syaratUjian.getAlertCustom()));
		alertCustom.setWidth("90%");
		alertCustom.setRows(3);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilkanHanyaAlertCustom = new MyCheckboxConfig(
				"Tampilkan hanya Informasi kustom yang ditampilkan bagi yang tidak memenuhi syarat"));
		tampilkanHanyaAlertCustom.setChecked(syaratUjian.getTampilkanHanyaAlertCustom());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/Nama Matakuliah (Atau)"));
		row.appendChild(kodeMatakuliah = new MyTextbox(syaratUjian.getKodeMatakuliah()));
		kodeMatakuliah.setWidth("90%");
		kodeMatakuliah.setRows(2);
		Common.initKeterangan(rows,
				"Jika terdapat banyak kode atau nama matakuliah, pisah menggunakan tanda koma (,). Misal : BSC123,DCFR45,DESW56 maka artinya adalah BSC123 atau DCFR45 atau DESW56");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/Nama Matakuliah (Dan)"));
		row.appendChild(kodeMatakuliahDan = new MyTextbox(syaratUjian.getKodeMatakuliahDan()));
		kodeMatakuliahDan.setWidth("90%");
		kodeMatakuliahDan.setRows(2);
		Common.initKeterangan(rows,
				"Jika terdapat banyak kode atau nama matakuliah, pisah menggunakan tanda koma (,). Misal : BSC123,DCFR45,DESW56 maka artinya adalah BSC123 dan DCFR45 dan DESW56");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(tidakWajibMengambilMkTertentu = new MyCheckboxConfig(
				"Mahasiswa Tidak Wajib mengambil Matakuliah Tertentu"));
		tidakWajibMengambilMkTertentu.setChecked(syaratUjian.getTidakWajibMengambilMkTertentu());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal SKS"));
		row.appendChild(minimalSks = new MyIntbox(syaratUjian.getMinimalSks()));
		minimalSks.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal IPK"));
		row.appendChild(minimalIpk = new MyDoublebox(syaratUjian.getMinimalIpk()));
		minimalIpk.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal Angka Kredit"));
		row.appendChild(minimalAngkaKredit = new MyDoublebox(syaratUjian.getMinimalAngkaKredit()));
		minimalAngkaKredit.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal Prosentase Kehadiran"));
		row.appendChild(minimalPersenKehadiran = new MyDoublebox(syaratUjian.getMinimalPersenKehadiran()));
		minimalPersenKehadiran.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(minimalSmt = new MyIntbox(syaratUjian.getMinimalSmt()));
		minimalSmt.setCols(2);
		hbox.appendChild(new ais.ui.util.MyLabelConfig("sd"));
		hbox.appendChild(maksimalSmt = new MyIntbox(syaratUjian.getMaksimalSmt()));
		maksimalSmt.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(minimalAngkatan = new Intbox(syaratUjian.getMinimalAngkatan()));
		minimalAngkatan.setCols(2);
		hbox.appendChild(new ais.ui.util.MyLabelConfig("sd"));
		hbox.appendChild(maksimalAngkatan = new Intbox(syaratUjian.getMaksimalAngkatan()));
		maksimalAngkatan.setCols(2);

		Tbmuser tbmuser = Common.getCurrentUser();
		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas,
				syaratUjian.getFakultas() == null ? tbmuser.ambilFakultas() : syaratUjian.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		Common.insertComboDanSemua(jenjang = new Combobox(), new String[] { "nama" }, "nama", Jenjang.class, "Semua",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jenjang, syaratUjian.getJenjang());
		row.appendChild(jenjang);
		jenjang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan,
				syaratUjian.getJurusan() == null ? tbmuser.ambilJurusan() : syaratUjian.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		program = new Combobox();
		Common.initPrograms(program);

		Common.selectComboItem(program, syaratUjian.getProgram());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berlaku untuk semua pertemuan"));
		Common.insertComboDanSemua(statusPertemuan = new Combobox(), new String[] { "nama" }, "nama",
				StatusPertemuan.class, "Tidak ada",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(statusPertemuan, syaratUjian.getStatusPertemuan());
		row.appendChild(statusPertemuan);
		statusPertemuan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal mahasiswa"));
		row.appendChild(statusAwalMahasiswa = new Combobox());
		Common.insertComboDanSemua(statusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(statusAwalMahasiswa, syaratUjian.getStatusAwalMahasiswa());
		statusAwalMahasiswa.setWidth("90%");
		statusAwalMahasiswa.setReadonly(true);

		tahunAkademik = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		tahunAkademik.appendChild(comboitem);
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);

		semester = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semester.appendChild(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berlaku Mulai Tahun Akademik"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		Common.selectComboItem(tahunAkademik, syaratUjian.getTahunAkademik());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berlaku Mulai Semester"));
		row.appendChild(semester);
		semester.setWidth("90%");
		Common.selectComboItem(semester, syaratUjian.getSemester());
		semester.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Item Biaya (ATAU)"));
		row.appendChild(kodeItemBiayaOr = new Textbox(syaratUjian.getKodeItemBiayaOr()));
		kodeItemBiayaOr.setWidth("90%");
		kodeItemBiayaOr.setRows(2);

		Common.initKeterangan(rows,
				"Jika syarat harus membayar biaya tertentu, masukkan kode item biaya yang harus dibayar mahasiswa. Jika item biaya lebih dari satu, pisahkan dengan tanda koma (,), contoh : 502,505,506 dan seterusnya. Dan juga pastikan kode item biaya benar.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Item Biaya (DAN)"));
		row.appendChild(kodeItemBiaya = new Textbox(syaratUjian.getKodeItemBiaya()));
		kodeItemBiaya.setWidth("90%");
		kodeItemBiaya.setRows(2);

		Common.initKeterangan(rows,
				"Jika syarat harus membayar biaya tertentu, masukkan kode item biaya yang harus dibayar mahasiswa. Jika item biaya lebih dari satu, pisahkan dengan tanda koma (,), contoh : 502,505,506 dan seterusnya. Dan juga pastikan kode item biaya benar.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(sekaliBayar = new MyCheckboxConfig(
				"Kode item biaya tersebut sekali bayar saja, jadi kalau misalnya mahasiswa membayar di semester 7, tetap bisa ikut ujian di semester 8 atau lebih tanpa membayar ulang."));
		sekaliBayar.setChecked(syaratUjian.getSekaliBayar());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(syaratUjian.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		Grid subGrid = SyaratUjianAction.initJadwal(syaratUjian, delete);
		row.appendChild(subGrid);
		subrows = subGrid.getRows();

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

	@SuppressWarnings("deprecation")
	public static Grid initJadwal(final SyaratUjian syaratUjian, final boolean delete) {

		Tbmuser tbmuser = Common.getCurrentUser();
		Grid subGrid = new Grid();

		Columns subcolumns = new Columns();
		subcolumns.setParent(subGrid);

		MyColumnConfig subcolumn = new MyColumnConfig("Item Biaya");
		subcolumn.setParent(subcolumns);
		subcolumn.setWidth("30%");

		subcolumn = new MyColumnConfig("Persen (%)");
		subcolumn.setParent(subcolumns);
		subcolumn.setWidth("10%");

		subcolumn = new MyColumnConfig("Smt sebelum-nya");
		subcolumn.setParent(subcolumns);
		subcolumn.setWidth("10%");

		subcolumn = new MyColumnConfig("Bulan (pisah dengan koma)");
		subcolumn.setParent(subcolumns);
		subcolumn.setWidth("15%");

		subcolumn = new MyColumnConfig("Keterangan");
		subcolumn.setParent(subcolumns);

		subcolumn = new MyColumnConfig("Atau");
		subcolumn.setParent(subcolumns);
		subcolumn.setWidth("10%");

		subcolumn = new MyColumnConfig("Hapus");
		subcolumn.setParent(subcolumns);
		subcolumn.setWidth("8%");

		final Rows subrows = new Rows();
		subrows.setParent(subGrid);

		List<Object[]> list = syaratUjian.daftarJadwal();

		for (Object[] o : list) {

			try {
				final String n = o[0].toString();
				String item = o.length > 5 ? o[5].toString() : "";
				final MyFormRow subrow = new MyFormRow();
				subrow.setParent(subrows);
				subrow.setValign("top");
				subrow.setAttribute("o", o);

				subrow.appendChild(new Vbox(new Component[] { new Label(n), new Label(item) }));
				subrow.appendChild(new Label(Common.numberFormat.get().format(o[1]) + "%"));
				subrow.appendChild(new Label(o.length > 6 ? Common.numberFormat.get().format(o[6]) + " smt" : ""));
				subrow.appendChild(new Label(o.length > 4 ? o[4].toString() : ""));
				subrow.appendChild(new Label(o[2].toString()));
				subrow.appendChild(new Label(o[3].equals(true) ? "Ya" : "Tidak"));

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.setVisible(
						delete && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
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
											if (syaratUjian.getId() != null) {
												syaratUjian.hapusJadwal(n);
												Common.refreshUpdate(syaratUjian);
											}
											subrow.detach();
										}

									}
								});

					}
				});
				button.setParent(subrow);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		MyFormRow subrow = new MyFormRow();
		subrow.setParent(subrows);
		ais.ui.util.ZkCompat.setSpans(subrow, "5");
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Syarat Pembayaran Mengikuti Ujian",
				"/img/add_item.png");
		button.setTooltiptext("Tambah Syarat Pembayaran Mengikuti Ujian");
		button.setVisible(delete && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				Tbmuser tbmuser = Common.getCurrentUser();
				final MyFormRow subrow = new MyFormRow();
				subrow.setParent(subrows);

				final Combobox nama = new Combobox();
				Common.insertCombo(nama, new String[] { "kode", "namaKegiatan" }, JenisKegiatan.class,
						Restrictions.eq("aktif", true));
				nama.setReadonly(true);

				final Combobox item = new Combobox();
				Common.insertComboDanSemua(item, new String[] { "kode", "nama" }, "deskripsi", ItemBiaya.class,
						Restrictions.eq("aktif", true));
				item.setReadonly(true);
				item.setVisible(false);

				final MyDoublebox persen = new MyDoublebox(99.9);
				final Intbox smt = new Intbox(0);
				final MyTextbox bulan = new MyTextbox();
				final Label keterangan = new Label();
				final MyCheckboxConfig atau = new MyCheckboxConfig("Atau");

				if (syaratUjian.getId() != null) {
					EventListener eventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							syaratUjian.populateJadwal(nama.getValue(), persen.getValue(), keterangan.getValue(),
									atau.isChecked(), bulan.getValue().trim(), item.getValue(), smt.getValue());
							Common.refreshUpdate(syaratUjian);
						}
					};
					nama.addEventListener("onChange", eventListener);
					item.addEventListener("onChange", eventListener);
					persen.addEventListener("onChange", eventListener);
					smt.addEventListener("onChange", eventListener);
					bulan.addEventListener("onChange", eventListener);
					keterangan.addEventListener("onChange", eventListener);
					atau.addEventListener("onClick", eventListener);
				}

				subrow.setValign("top");
				subrow.setAttribute("nama", nama);
				subrow.setValign("top");
				subrow.setAttribute("item", item);
				subrow.setValign("top");
				subrow.setAttribute("persen", persen);
				subrow.setValign("top");
				subrow.setAttribute("smt", smt);
				subrow.setValign("top");
				subrow.setAttribute("bulan", bulan);
				subrow.setValign("top");
				subrow.setAttribute("keterangan", keterangan);
				subrow.setValign("top");
				subrow.setAttribute("atau", atau);

				item.setWidth("90%");
				nama.setWidth("90%");
				keterangan.setWidth("90%");
				persen.setWidth("90%");
				smt.setWidth("90%");
				bulan.setWidth("90%");

				Vbox vbox = new Vbox();
				subrow.appendChild(vbox);
				vbox.setWidth("100%");

				vbox.appendChild(nama);
				vbox.appendChild(item);

				subrow.appendChild(persen);
				subrow.appendChild(smt);
				subrow.appendChild(bulan);
				subrow.appendChild(keterangan);
				subrow.appendChild(atau);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.setVisible(
						delete && tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
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
											if (syaratUjian.getId() != null) {
												syaratUjian.hapusJadwal(nama.getValue());
												Common.refreshUpdate(syaratUjian);
											}
											subrow.detach();
										}

									}
								});

					}
				});
				button.setParent(subrow);
			}
		});
		button.setParent(subrow);
		return subGrid;
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Syarat Ujian",
					"Kolom Nama Syarat Ujian belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Syarat Ujian.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaSyaratUjian();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Syarat Ujian",
					"Nama Syarat Ujian sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama syarat ujian yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (syaratUjian.getId() != null) {
			syaratUjian = (SyaratUjian) session.load(SyaratUjian.class, syaratUjian.getId());
		}

		syaratUjian.setJenjang(
				(Jenjang) (jenjang.getSelectedItem() == null ? null : jenjang.getSelectedItem().getValue()));
		syaratUjian.setStatusPertemuan((StatusPertemuan) (statusPertemuan.getSelectedItem() == null ? null
				: statusPertemuan.getSelectedItem().getValue()));
		syaratUjian.setNama(nama.getValue());
		syaratUjian.setKeterangan(keterangan.getValue());
		syaratUjian.setMinimalIpk(minimalIpk.getValue());
		syaratUjian.setMinimalSks(minimalSks.getValue());
		syaratUjian.setKodeMatakuliah(kodeMatakuliah.getValue().trim());
		syaratUjian.setKodeMatakuliahDan(kodeMatakuliahDan.getValue().trim());
		syaratUjian.setMinimalAngkaKredit(minimalAngkaKredit.getValue());
		syaratUjian.setKodeItemBiaya(kodeItemBiaya.getValue());
		syaratUjian.setKodeItemBiayaOr(kodeItemBiayaOr.getValue());
		syaratUjian.setTidakWajibMengambilMkTertentu(tidakWajibMengambilMkTertentu.isChecked());
		syaratUjian.setMinimalPersenKehadiran(minimalPersenKehadiran.getValue());
		syaratUjian.setSekaliBayar(sekaliBayar.isChecked());
		syaratUjian.setMaksimalSmt(maksimalSmt.getValue());
		syaratUjian.setMinimalSmt(minimalSmt.getValue());
		syaratUjian.setAlertCustom(alertCustom.getValue().trim());
		syaratUjian.setMaksimalAngkatan(maksimalAngkatan.getValue());
		syaratUjian.setMinimalAngkatan(minimalAngkatan.getValue());
		syaratUjian.setTampilkanHanyaAlertCustom(tampilkanHanyaAlertCustom.isChecked());
		syaratUjian.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null ? null : jurusan.getSelectedItem().getValue()));
		syaratUjian.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null ? null : fakultas.getSelectedItem().getValue()));

		syaratUjian
				.setProgram((String) (program.getSelectedItem() == null ? null : program.getSelectedItem().getValue()));

		syaratUjian.setTahunAkademik(
				(String) (tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
						? null
						: tahunAkademik.getSelectedItem().getValue()));
		syaratUjian.setSemester(
				(String) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue()));

		syaratUjian.setStatusAwalMahasiswa((StatusAwalMahasiswa) (statusAwalMahasiswa.getSelectedItem() == null ? null
				: statusAwalMahasiswa.getSelectedItem().getValue()));

		if (subrows != null) {
			syaratUjian.setSyaratPembayaran("");
			@SuppressWarnings("unchecked")
			List<Row> rows = subrows.getChildren();
			for (Row row : rows) {
				if (row.getAttribute("nama") != null) {
					Textbox nama = (Textbox) row.getAttribute("nama");
					MyDoublebox persen = (MyDoublebox) row.getAttribute("persen");
					Intbox smt = (Intbox) row.getAttribute("smt");
					Label keterangan = (Label) row.getAttribute("keterangan");
					MyCheckboxConfig atau = (MyCheckboxConfig) row.getAttribute("atau");
					MyTextbox bulan = (MyTextbox) row.getAttribute("bulan");

					Combobox item = (Combobox) row.getAttribute("item");

					syaratUjian.populateJadwal(nama.getValue(), persen.getValue(), keterangan.getValue(),
							atau.isChecked(), bulan.getValue().trim(), item.getValue(), smt.getValue());
				} else if (row.getAttribute("o") != null) {
					Object[] o = (Object[]) row.getAttribute("o");
					syaratUjian.populateJadwal((String) o[0], (Double) o[1], (String) o[2], (Boolean) o[3],
							(String) o[4], (String) o[5], (Integer) o[6]);
				}
			}
		}

		Common.refreshSaveOrUpdate(session, syaratUjian);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(SyaratUjian.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<SyaratUjian> syaratUjian = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(syaratUjian);
		grid.setRowRenderer(new SyaratUjianRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaSyaratUjian() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(SyaratUjian.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.syaratUjian.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.syaratUjian.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public static boolean checkSyaratSyaratUjian(SyaratUjian syaratUjian, VOPembelajaran voPembelajaran,
			Mahasiswa mahasiswa, Integer semester, String namaSyarat) throws Exception {
		return checkSyaratSyaratUjian(syaratUjian, voPembelajaran, mahasiswa, semester, namaSyarat, null);
	}

	public static boolean checkSyaratSyaratUjian(SyaratUjian syaratUjian, VOPembelajaran voPembelajaran,
			Mahasiswa mahasiswa, Integer semester, String namaSyarat, List<String> warnings) throws Exception {
		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);
		return checkSyaratSyaratUjian(syaratUjian, voPembelajaran, mahasiswa, semester, namaSyarat, krsMahasiswa,
				warnings);
	}

	@SuppressWarnings("unchecked")
	public static boolean checkSyaratSyaratUjian(SyaratUjian syaratUjian, VOPembelajaran voPembelajaran,
			Mahasiswa mahasiswa, Integer semester, String namaSyarat, KrsMahasiswa krsMahasiswa, List<String> warnings)
			throws Exception {

		if (syaratUjian == null || !syaratUjian.getAktif()) {
			return true;
		}

		System.out.println("syaratUjian => " + syaratUjian + ", voPembelajaran => " + voPembelajaran + ", mahasiswa "
				+ mahasiswa + ", semester " + semester + ", namaSyarat " + namaSyarat);
		if (krsMahasiswa != null && syaratUjian != null && semester >= syaratUjian.getMinimalSmt()
				&& semester <= syaratUjian.getMaksimalSmt() && mahasiswa != null

				&& mahasiswa.getTahunangkatan() >= syaratUjian.getMinimalAngkatan()
				&& mahasiswa.getTahunangkatan() <= syaratUjian.getMaksimalAngkatan()

				&& (syaratUjian.getProgram() == null || mahasiswa.getProgram().equals(syaratUjian.getProgram()))

				&& (syaratUjian.getJenjang() == null
						|| mahasiswa.getJurusan().getJenjang().getId().equals(syaratUjian.getJenjang().getId()))

				&& (syaratUjian.getJurusan() == null
						|| mahasiswa.getJurusan().getId().equals(syaratUjian.getJurusan().getId()))

				&& (syaratUjian.getStatusAwalMahasiswa() == null || mahasiswa.getStatusAwalMahasiswa().getId()
						.equals(syaratUjian.getStatusAwalMahasiswa().getId()))

				&& (syaratUjian.getFakultas() == null
						|| mahasiswa.getJurusan().getFakultas().getId().equals(syaratUjian.getFakultas().getId()))

		) {

			String tahunAkademik = krsMahasiswa.getTahunAkademik();
			String smt = mahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL)
					? (krsMahasiswa.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL)
					: (krsMahasiswa.getSemester() % 2 == 1 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
			String id_smt = (tahunAkademik == null || tahunAkademik.trim().isEmpty() ? "0"
					: tahunAkademik.split("/")[0])
					+ (smt.trim().isEmpty() ? "0" : smt.equals(Perkuliahan.GENAP) ? "2" : "1");
			Integer ta = 0;
			try {
				ta = Integer.parseInt(id_smt.trim());
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			if (syaratUjian.getTa() == null || syaratUjian.getTa() <= ta) {

				int sks = krsMahasiswa.getSksk();
				int batasSks = syaratUjian.getMinimalSks();
				System.out.println("batasSks " + batasSks + ", sks " + sks);
				if (batasSks > sks) {

					String w = !syaratUjian.getAlertCustom().trim().isEmpty()
							&& syaratUjian.getTampilkanHanyaAlertCustom()
									? syaratUjian.getAlertCustom()
									: (syaratUjian.getAlertCustom().isEmpty() ? ""
											: syaratUjian.getAlertCustom() + "\n\n") + "Mahasiswa dengan NIM "
											+ mahasiswa.getNim() + " dan nama " + mahasiswa.getNama()
											+ " belum bisa  \"" + namaSyarat + "\", karena syarat  \"" + namaSyarat
											+ "\", karena harus memiliki minimal " + batasSks
											+ " SKS. Sedangkan SKS yang telah diperoleh " + sks + " SKS di semester "
											+ semester + "";
					if (warnings != null) {
						warnings.add(w);
					} else {
						MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					}

					return false;
				}

				Double batasAngkaKredit = syaratUjian.getMinimalAngkaKredit();

				if (batasAngkaKredit > 0.1) {
					Double angkaKredit = Common.hitungAngkaKredit(mahasiswa);

					System.out.println("batasAngkaKredit " + batasAngkaKredit + ", angkaKredit " + angkaKredit);
					if (batasAngkaKredit > angkaKredit) {
						String w = !syaratUjian.getAlertCustom().trim().isEmpty()
								&& syaratUjian.getTampilkanHanyaAlertCustom()
										? syaratUjian.getAlertCustom()
										: (syaratUjian.getAlertCustom().isEmpty() ? ""
												: syaratUjian.getAlertCustom() + "\n\n") + "Mahasiswa dengan NIM "
												+ mahasiswa.getNim() + " dan nama " + mahasiswa.getNama()
												+ " belum bisa  \"" + namaSyarat + "\", karena syarat  \"" + namaSyarat
												+ "\", karena harus memiliki minimal " + batasAngkaKredit
												+ " Angka Kredit kegiatan kemahasiswaan. Sedangkan Angka Kredit yang telah diperoleh "
												+ angkaKredit + "";

						if (warnings != null) {
							warnings.add(w);
						} else {
							MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}

						return false;
					}
				}

				double ipk = krsMahasiswa.getIpk();
				double batasIpk = syaratUjian.getMinimalIpk();

				System.out.println("batasIpk " + batasIpk + ", ipk " + ipk);
				if (batasIpk > ipk) {

					String w = !syaratUjian.getAlertCustom().trim().isEmpty()
							&& syaratUjian.getTampilkanHanyaAlertCustom()
									? syaratUjian.getAlertCustom()
									: (syaratUjian.getAlertCustom().isEmpty() ? ""
											: syaratUjian.getAlertCustom() + "\n\n") + "Mahasiswa dengan NIM "
											+ mahasiswa.getNim() + " dan nama " + mahasiswa.getNama()
											+ " belum bisa  \"" + namaSyarat + "\", karena syarat  \"" + namaSyarat
											+ "\", karena harus harus memiliki minimal IPK "
											+ Common.numberFormat.get().format(batasIpk) + " di semester " + semester
											+ ". Sedangkan IPK yang telah diperoleh " + Common.numberFormat.get().format(ipk);
					if (warnings != null) {
						warnings.add(w);
					} else {
						MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					}

					return false;
				}

				double minimalPersenKehadiran = syaratUjian.getMinimalPersenKehadiran();
				if (voPembelajaran != null && minimalPersenKehadiran > 0.1) {
					List<Pertemuan> statusPertemuan = voPembelajaran.ambilPertemuanList();
					List<String> absenSemua = new ArrayList<String>();
					for (Pertemuan pertemuan : statusPertemuan) {
						absenSemua.add(pertemuan.getAbsensi());
					}

					Map<String, Integer> statuses = Perkuliahan.hitungStatus(absenSemua, mahasiswa.getId());

					int semua = statuses.get("T") == null ? 0 : statuses.get("T");
					int masuk = statuses.get("M") == null ? 0 : statuses.get("M");

					double nilaiAbsensi = semua == 0 ? 0.0 : (masuk * 100.0) / semua;

					absenSemua = null;
					statuses = null;
					statusPertemuan = null;

					System.out.println(
							"minimalPersenKehadiran " + minimalPersenKehadiran + ", nilaiAbsensi " + nilaiAbsensi);
					if (minimalPersenKehadiran > nilaiAbsensi) {

						String w = !syaratUjian.getAlertCustom().trim().isEmpty()
								&& syaratUjian.getTampilkanHanyaAlertCustom()
										? syaratUjian.getAlertCustom()
										: (syaratUjian.getAlertCustom().isEmpty() ? ""
												: syaratUjian.getAlertCustom() + "\n\n") + "Mahasiswa dengan NIM "
												+ mahasiswa.getNim() + " dan nama " + mahasiswa.getNama()
												+ " belum bisa  \"" + namaSyarat + "\", karena syarat  \"" + namaSyarat
												+ "\", karena harus harus hadir dengan minimal "
												+ Common.numberFormat.get().format(minimalPersenKehadiran) + "% di \""
												+ voPembelajaran.infoSimple()
												+ "\". Sedangkan kehadiran yang diperoleh adalah "
												+ Common.numberFormat.get().format(nilaiAbsensi) + "% ";
						if (warnings != null) {
							warnings.add(w);
						} else {
							MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}

						return false;
					}
				}

				if (!syaratUjian.getKodeMatakuliah().trim().isEmpty()) {
					Object check = Common.checkApakahSudahMengambilKrsSeminarSkripsi(mahasiswa, null,
							syaratUjian.getKodeMatakuliah().trim());
					if (check == null) {
						String mk = "";
						for (String kode : syaratUjian.getKodeMatakuliah().split(",")) {
							if (!kode.trim().isEmpty()) {
								Object[] nama = (Object[]) HibernateUtil.currentSession()
										.createCriteria(Matakuliah.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.setProjection(Projections.projectionList().add(Projections.property("kode"))
												.add(Projections.property("nama")))
										.add(Restrictions.or(Restrictions.ilike("nama", kode.trim(), MatchMode.EXACT),
												Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT)))
										.setMaxResults(1).uniqueResult();
								if (nama != null && nama.length > 1) {
									mk += mk.isEmpty() ? (nama[0] + " - " + nama[1])
											: " atau " + (nama[0] + " - " + nama[1]);
								}
							}
						}

						System.out.println("getKodeMatakuliah mk " + mk);

						String w = !syaratUjian.getAlertCustom().trim().isEmpty()
								&& syaratUjian.getTampilkanHanyaAlertCustom()
										? syaratUjian.getAlertCustom()
										: (syaratUjian.getAlertCustom().isEmpty() ? ""
												: syaratUjian.getAlertCustom() + "\n\n") + "Mahasiswa dengan NIM "
												+ mahasiswa.getNim() + " dan nama " + mahasiswa.getNama()
												+ " belum bisa  \"" + namaSyarat + "\", karena syarat  \"" + namaSyarat
												+ "\" harus mengambil salah satu matakuliah sbb :\n " + mk;
						if (warnings != null) {
							warnings.add(w);
						} else {
							MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						}
						return false;
					}
				}

				if (!syaratUjian.getKodeMatakuliahDan().trim().isEmpty()) {
					Detailperkuliahan check = Common.checkApakahSudahMengambilKrsSeminarSkripsiDan(mahasiswa,
							syaratUjian.getKodeMatakuliahDan().trim());
					if (check == null) {
						String mk = "";
						for (String kode : syaratUjian.getKodeMatakuliahDan().split(",")) {
							if (!kode.trim().isEmpty()) {
								Object[] nama = (Object[]) HibernateUtil.currentSession()
										.createCriteria(Matakuliah.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.setProjection(Projections.projectionList().add(Projections.property("kode"))
												.add(Projections.property("nama")))
										.add(Restrictions.or(Restrictions.ilike("nama", kode.trim(), MatchMode.EXACT),
												Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT)))
										.setMaxResults(1).uniqueResult();
								if (nama != null && nama.length > 1) {
									mk += mk.isEmpty() ? (nama[0] + " - " + nama[1])
											: " dan " + (nama[0] + " - " + nama[1]);
								}
							}
						}
						System.out.println("getKodeMatakuliahDan mk " + mk);
						String w = !syaratUjian.getAlertCustom().trim().isEmpty()
								&& syaratUjian.getTampilkanHanyaAlertCustom()
										? syaratUjian.getAlertCustom()
										: (syaratUjian.getAlertCustom().isEmpty() ? ""
												: syaratUjian.getAlertCustom() + "\n\n") + "Mahasiswa dengan NIM "
												+ mahasiswa.getNim() + " dan nama " + mahasiswa.getNama()
												+ " belum bisa  \"" + namaSyarat + "\", karena syarat  \"" + namaSyarat
												+ "\" harus telah mengambil matakuliah sbb :\n " + mk;
						if (warnings != null) {
							warnings.add(w);
						} else {
							MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						}
						return false;
					}
				}

				Map<JenisKegiatan, Boolean> datas = new HashMap<JenisKegiatan, Boolean>();
				String ket = "";
				List<Object[]> list = syaratUjian.daftarJadwal();
				for (Object[] o : list) {
					String biayaB = o[0].toString();
					String kodeItemBiaya = biayaB.split("-")[0].trim();
					Double persen = (Double) o[1];
					Boolean atau = (Boolean) o[3];
					String bulan = o.length > 4 ? o[4].toString() : "";

					Integer smtData = o.length > 6 ? (Integer) o[6] : 0;

					if (persen > 0.1 && !kodeItemBiaya.trim().isEmpty()) {
						System.out.println("kodeBiaya -> " + kodeItemBiaya);

						if (ais.common.CommonHelperClass.jenisKegiatansAktif == null) {
							Common.reloadJenisKegiatans();
						}
						JenisKegiatan jenisKegiatan = null;
						if (!kodeItemBiaya.trim().isEmpty()) {
							for (JenisKegiatan d : ais.common.CommonHelperClass.jenisKegiatansAktif) {
								if (d.getKode() != null && d.getKode().equalsIgnoreCase(kodeItemBiaya)) {
									jenisKegiatan = d;
									break;
								}
							}
						}

						System.out.println("kodeBiaya -> " + kodeItemBiaya + ", jenisKegiatan => " + jenisKegiatan
								+ ", atau -> " + atau);

						if (jenisKegiatan == null || Common.checkBaypassStatusPembayaranMahasiswa(semester - smtData,
								null, mahasiswa, jenisKegiatan)) {
							datas.put(jenisKegiatan, true);
							continue;
						}

						JSONObject dibayars = null;
						JSONObject tagihans = null;

						Double persenDibayar = 0.0;
						if ((ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
								&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId().equals(jenisKegiatan.getId()))
								|| (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
										&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()
												.equals(jenisKegiatan.getId()))) {
							BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) (mahasiswa
									.getBiodataCalonMahasiswa() == null ? null
											: ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(),
													mahasiswa.getBiodataCalonMahasiswa()));
							Kegiatan kegiatan = null;
							if (biodataCalonMahasiswa != null) {
								// GERBANG WAJIB DATA SEGAR (refresh=true): tanpa ini persentase dibaca
								// dari entity Kegiatan yang di-cache di memori JVM -- mahasiswa yang
								// SUDAH lunas di database tetap diblokir dengan persen basi (kasus
								// nyata: DB 100% sejak pagi, gerbang malam masih menyebut 53,398%).
								kegiatan = biodataCalonMahasiswa.ambilKegiatans(semester - smtData, jenisKegiatan,
										true);
								persenDibayar = (kegiatan == null ? 0.0 : kegiatan.getPersentaseLunas());
							}

							if (kegiatan != null) {
								dibayars = new JSONObject(kegiatan.getBulans());
								tagihans = new JSONObject(kegiatan.getTagihans());
							}

							System.out.println("biodataCalonMahasiswa -> " + biodataCalonMahasiswa + ", kegiatan -> "
									+ kegiatan + ", persen dibayar " + persenDibayar + ", persen syarat " + persen);
						} else {
							// GERBANG WAJIB DATA SEGAR (refresh=true): tanpa ini persentase dibaca dari
							// entity Kegiatan yang di-cache di memori JVM -- mahasiswa yang SUDAH lunas
							// di database tetap diblokir dengan persen basi (kasus nyata: DB 100% sejak
							// pagi, gerbang malam masih menyebut 53,398%).
							Kegiatan kegiatan = mahasiswa.ambilKegiatans(semester - smtData, jenisKegiatan, true);
							persenDibayar = (kegiatan == null ? 0.0 : kegiatan.getPersentaseLunas());

							if (kegiatan != null) {
								dibayars = new JSONObject(kegiatan.getBulans());
								tagihans = new JSONObject(kegiatan.getTagihans());
							}

							System.out.println("kegiatan -> " + kegiatan + ", persen dibayar " + persenDibayar
									+ ", persen syarat " + persen);
						}

						if (!bulan.trim().isEmpty()) {
							Set<Integer> bulansdata = new HashSet<Integer>();
							for (String b : bulan.split(",")) {
								try {
									bulansdata.add(Integer.parseInt(b));
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SyaratUjianAction.java:1367");
									// TODO: handle exception
								}
							}

							boolean adatagihan = false;
							Double tagihan = 0.0;
							if (tagihans != null) {
								Iterator<String> iterator = tagihans.keys();
								while (iterator.hasNext()) {
									try {
										String v = iterator.next();
										Object val = tagihans.get(v) + "";
										String[] vv = v.split("_");

										Integer bulanBayar = Integer.parseInt(vv[1]);

										if (bulansdata.contains(bulanBayar) && val != null
												&& !val.toString().isEmpty()) {
											tagihan += Double.parseDouble(val + "");
											adatagihan = true;
										}
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SyaratUjianAction.java:1389");
//									ais.common.Common.tampilErrorJikaAdmin(e);
									}
								}
							}

							if (adatagihan) {
								Double nilaibayar = 0.0;

								if (dibayars != null) {
									Iterator<String> iterator = dibayars.keys();
									while (iterator.hasNext()) {
										try {

											String v = iterator.next();

											Object val = dibayars.get(v) + "";
											String[] vv = v.split("_");

											if (vv.length > 2 && val != null && !val.toString().isEmpty()) {

												Integer bulanBayar = Integer.parseInt(vv[1]);

												if (val != null && !val.toString().isEmpty()
														&& bulansdata.contains(bulanBayar)) {
													Double nn = Double.parseDouble(val + "");

													nilaibayar += nn;
												}

											}
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SyaratUjianAction.java:1420");
											// TODO: handle exception
										}
									}
								}
								Double persenbayar = (tagihan.intValue() == 0 ? 0.0 : ((nilaibayar * 100.0) / tagihan));
								boolean salah = persen.intValue() > persenbayar.intValue();

								System.out.println("syarat " + biayaB + " atau " + atau + " bulansdata -> " + bulansdata
										+ " persenbayar " + persenbayar + " persen " + persen + " salah " + salah
										+ " nilaibayar " + nilaibayar + " tagihan " + tagihan);

								if (!atau) {
									if (salah) {

										String w = !syaratUjian.getAlertCustom().trim().isEmpty()
												&& syaratUjian.getTampilkanHanyaAlertCustom()
														? syaratUjian.getAlertCustom()
														: (syaratUjian.getAlertCustom().isEmpty() ? ""
																: syaratUjian.getAlertCustom() + "\n\n")
																+ "Mahasiswa dengan NIM " + mahasiswa.getNim()
																+ " dan nama " + mahasiswa.getNama() + " belum bisa  \""
																+ namaSyarat + "\", karena syarat  \"" + namaSyarat
																+ "\" harus telah membayar di bulan " + bulan
																+ "	minimal " + Common.numberFormat.get().format(persen)
																+ "%, sedangkan anda masih membayar "
																+ Common.numberFormat.get().format(persenbayar)
																+ "%, di semester " + (semester - smtData)
																+ ".\n\nHarap segera menghubungi bagian keuangan untuk informasi lebih lanjut.";

										if (warnings != null) {
											warnings.add(w);
										} else {
											MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
										}
										return false;
									}
								} else {
									if (jenisKegiatan != null) {
										String s = "blm bayar " + jenisKegiatan.getNamaKegiatan() + " bulan " + bulan
												+ "	minimal " + Common.numberFormat.get().format(persen)
												+ "%, sedangkan anda masih membayar "
												+ Common.numberFormat.get().format(persenbayar) + "%, di semester "
												+ (semester - smtData);
										ket += ket.isEmpty() ? s : ", " + s;
									}
									datas.put(jenisKegiatan, !salah);
								}
							}
						} else {
							boolean salah = persenDibayar.intValue() < persen.intValue();
							if (!atau) {
								if (salah) {

									String w = !syaratUjian.getAlertCustom().trim().isEmpty()
											&& syaratUjian.getTampilkanHanyaAlertCustom()
													? syaratUjian.getAlertCustom()
													: (syaratUjian.getAlertCustom().isEmpty() ? ""
															: syaratUjian.getAlertCustom() + "\n\n")
															+ "Mahasiswa dengan NIM " + mahasiswa.getNim()
															+ " dan nama " + mahasiswa.getNama() + " belum bisa  \""
															+ namaSyarat + "\", karena syarat  \"" + namaSyarat
															+ "\" harus telah membayar biaya \"" + biayaB
															+ "\" minimal " + Common.numberFormat.get().format(persen)
															+ "%, sedangkan pembayaran Anda masih tercatat "
															+ Common.numberFormat.get().format(persenDibayar)
															+ "%  di semester " + (semester - smtData)
															+ ".\n\nHarap segera menghubungi bagian keuangan untuk informasi lebih lanjut.";

									if (warnings != null) {
										warnings.add(w);
									} else {
										MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									}
									return false;
								}
							} else {
								if (jenisKegiatan != null) {
									String s = jenisKegiatan.getNamaKegiatan() + " sebesar "
											+ Common.numberFormat.get().format(persen)
											+ "%, sedangkan pembayaran Anda masih tercatat "
											+ Common.numberFormat.get().format(persenDibayar) + "%  di semester "
											+ (semester - smtData);
									ket += ket.isEmpty() ? s : ", " + s;
								}
								datas.put(jenisKegiatan, !salah);
							}
						}
					}
				}

				if (!datas.isEmpty()) {
					boolean semuaSalah = false;
					for (Boolean s : datas.values()) {
						semuaSalah |= s;
					}
					if (!semuaSalah) {
						String w = !syaratUjian.getAlertCustom().trim().isEmpty()
								&& syaratUjian.getTampilkanHanyaAlertCustom()
										? syaratUjian.getAlertCustom()
										: (syaratUjian.getAlertCustom().isEmpty() ? ""
												: syaratUjian.getAlertCustom() + "\n\n") + "Mahasiswa dengan NIM "
												+ mahasiswa.getNim() + " dan nama " + mahasiswa.getNama()
												+ " belum bisa  \"" + namaSyarat + "\", karena syarat  \"" + namaSyarat
												+ "\" harus telah membayar salah satu biaya \"" + ket
												+ "\".\n\nHarap segera menghubungi bagian keuangan untuk informasi lebih lanjut.";

						if (warnings != null) {
							warnings.add(w);
						} else {
							MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						}
						return false;
					}
				}

				String kodeItemBiaya = syaratUjian.getKodeItemBiaya();
				Map<String, Double> tagihans = null;
				boolean bypass = false;
				List<CicilanPembayaran> cicilanPembayaransTemp = null;
				if (!syaratUjian.getKodeItemBiaya().trim().isEmpty()
						|| !syaratUjian.getKodeItemBiayaOr().trim().isEmpty()) {
					tagihans = mahasiswa.ambilKodeTagihan(semester, false);
					cicilanPembayaransTemp = mahasiswa.ambilCicilan();
					JenisKegiatan jenisKegiatan = null;
					bypass = Common.checkBaypassStatusPembayaranMahasiswa(semester, null, mahasiswa, jenisKegiatan);
				}

				if (!kodeItemBiaya.trim().isEmpty()) {

					if (!bypass) {
						Session session = HibernateUtil.currentSession();

						for (String kode : kodeItemBiaya.trim().split(",")) {
							Double nilai = tagihans.get(kode);
							if (nilai != null && nilai > 0.1) {
								Double jumlah = mahasiswa.hitungTotalCicilanPembayaran(semester,
										syaratUjian.getSekaliBayar(), null, kode, cicilanPembayaransTemp);
								if (nilai > jumlah) {
									ItemBiaya itemBiaya = (ItemBiaya) ConstantValues.simpleObject(
											session.createCriteria(ItemBiaya.class)
													.add(Restrictions.eq("kode", kode.trim())).setMaxResults(1),
											ItemBiaya.class);
									if (itemBiaya != null) {

										System.out.println("kode biaya " + kode + ", jumlah " + jumlah);
										if (jumlah < 0.01) {

											String w = !syaratUjian.getAlertCustom().trim().isEmpty()
													&& syaratUjian.getTampilkanHanyaAlertCustom()
															? syaratUjian.getAlertCustom()
															: (syaratUjian.getAlertCustom().isEmpty() ? ""
																	: syaratUjian.getAlertCustom() + "\n\n")
																	+ "Mahasiswa dengan NIM " + mahasiswa.getNim()
																	+ " dan nama " + mahasiswa.getNama()
																	+ " belum bisa  \"" + namaSyarat
																	+ "\", karena syarat  \"" + namaSyarat
																	+ "\" harus membayar biaya " + itemBiaya.getKode()
																	+ "-" + itemBiaya.getNama() + " di semester "
																	+ semester + ". Tagihan "
																	+ Common.numberFormat.get().format(nilai) + " dibayar "
																	+ Common.numberFormat.get().format(jumlah)
																	+ ". Harap menghubungi bagian keuangan untuk melakukan pembayaran.";

											if (warnings != null) {
												warnings.add(w);
											} else {
												MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											}
											return false;
										}
									}
								}
							}
						}
					}

				}

				kodeItemBiaya = syaratUjian.getKodeItemBiayaOr();
				if (!kodeItemBiaya.trim().isEmpty()) {
					if (!bypass) {
						Session session = HibernateUtil.currentSession();

						String kodeBiaya = "";
						Double total = 0.0;

						for (String kode : kodeItemBiaya.trim().split(",")) {

							Double nilai = tagihans.get(kode);
							if (nilai != null && nilai > 0.1) {
								Double jumlah = mahasiswa.hitungTotalCicilanPembayaran(semester,
										syaratUjian.getSekaliBayar(), null, kode, cicilanPembayaransTemp);
								if (nilai > jumlah) {
									ItemBiaya itemBiaya = (ItemBiaya) ConstantValues.simpleObject(
											session.createCriteria(ItemBiaya.class)
													.add(Restrictions.eq("kode", kode.trim())).setMaxResults(1),
											ItemBiaya.class);
									if (itemBiaya != null) {

										System.out.println("kode biaya " + kode + ", jumlah " + jumlah);
										total += jumlah;
										String s = itemBiaya.getKode() + "-" + itemBiaya.getNama() + " tagihan "
												+ Common.numberFormat.get().format(nilai) + " dibayar "
												+ Common.numberFormat.get().format(jumlah);
										kodeBiaya += kodeBiaya.isEmpty() ? s : " atau " + s;
									}
								}
							}
						}

						if (total < 0.01 && !kodeBiaya.trim().isEmpty()) {

							String w = !syaratUjian.getAlertCustom().trim().isEmpty()
									&& syaratUjian.getTampilkanHanyaAlertCustom()
											? syaratUjian.getAlertCustom()
											: (syaratUjian.getAlertCustom().isEmpty() ? ""
													: syaratUjian.getAlertCustom() + "\n\n") + "Mahasiswa dengan NIM "
													+ mahasiswa.getNim() + " dan nama " + mahasiswa.getNama()
													+ " belum bisa  \"" + namaSyarat + "\", karena syarat  \""
													+ namaSyarat + "\" harus membayar biaya " + kodeBiaya
													+ " di semester " + semester
													+ ". Harap menghubungi bagian keuangan untuk melakukan pembayaran.";

							if (warnings != null) {
								warnings.add(w);
							} else {
								MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
							return false;
						}
					}
				}
			}
		}
		return true;
	}

}
