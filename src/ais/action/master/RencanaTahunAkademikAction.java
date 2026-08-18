package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.Calendar;
import java.util.Date;
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
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.RencanaTahunAkademik;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class RencanaTahunAkademikAction extends GenericAutowireComposer {

	private static final long serialVersionUID = -5779730267402400328L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchprogram;
	private Combobox searchStatusAwalMahasiswa;
	private Textbox searchTahunAngkatan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Combobox semester;
	private Combobox nama;
	private Textbox keterangan;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox program;
	private Combobox statusAwalMahasiswa;
	private Textbox tahunAngkatan;
	private MyDatebox tanggalMulai;
	private MyDatebox tanggalSampai;
	private MyDatebox tanggalMulaiBelajarMengajar;
	private Combobox yayasan;
	private Combobox sekolah;

	private boolean edit = false;
	private boolean delete = false;
	private boolean pt = false;
	private boolean ya = false;
	private boolean buatsekolah = false;

	private RencanaTahunAkademik rencanaTahunAkademik;
	private MyToolbarbuttonConfig add;
	private Tabpanel hariLiburNasional;

	public RencanaTahunAkademikAction() {
		this(false);
	}

	public RencanaTahunAkademikAction(boolean buatsekolah) {
		this.buatsekolah = buatsekolah;
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		initModeAplikasi();
		initKomponenAwal();
		initDataDefaultTahunAkademik();
		initPagingDanLoadData();
	        FilterLanjutHelper.setup(comp);
}

	public void onHariLibur(Event event) {
		if (hariLiburNasional == null) {
			return;
		}
		if (hariLiburNasional.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(hariLiburNasional);
			include.setSrc("/pages/master/library/hari_libur_perpustakaan.zul");
		}
	}

	private void initModeAplikasi() {
		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa != null && ptYa.length > 0 && ptYa[0];
		ya = ptYa != null && ptYa.length > 1 && ptYa[1];

		if (buatsekolah) {
			pt = false;
			ya = true;
		}
	}

	private void initKomponenAwal() {
		Common.generateTahunAjaranDanSemua(searchnama);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		program = Common.initPrograms(program);
		Common.initPrograms(searchprogram);

		Common.insertComboDanSemua(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		statusAwalMahasiswa = new Combobox();
		Common.insertComboDanSemua(statusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
	}

	private void initDataDefaultTahunAkademik() {
		Session sessionData = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		simpanDefaultTahunAkademikJikaBelumAda(sessionData, tahun - 1);
		simpanDefaultTahunAkademikJikaBelumAda(sessionData, tahun);
		simpanDefaultTahunAkademikJikaBelumAda(sessionData, tahun + 1);
	}

	private void simpanDefaultTahunAkademikJikaBelumAda(Session sessionData, int tahun) {
		if (sessionData == null) {
			return;
		}

		try {
			Number count = (Number) sessionData.createCriteria(RencanaTahunAkademik.class)
					.setProjection(Projections.rowCount()).add(Restrictions.ilike("nama", tahun + "", MatchMode.START))
					.uniqueResult();

			if (count != null && count.intValue() > 0) {
				return;
			}

			Fakultas defaultFakultas = getSelectedFakultas(fakultas);
			String tahunAkademik = tahun + "/" + (tahun + 1);

			RencanaTahunAkademik ganjil = new RencanaTahunAkademik();
			ganjil.setNama(tahunAkademik);
			ganjil.setSemester(Perkuliahan.GANJIL);
			ganjil.setFakultas(defaultFakultas);
			sessionData.save(ganjil);

			RencanaTahunAkademik genap = new RencanaTahunAkademik();
			genap.setNama(tahunAkademik);
			genap.setSemester(Perkuliahan.GENAP);
			genap.setFakultas(defaultFakultas);
			sessionData.save(genap);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initPagingDanLoadData() {
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class RencanaTahunAkademikRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			if (!(data instanceof RencanaTahunAkademik)) {
				row.setVisible(false);
				return;
			}

			final RencanaTahunAkademik rta = (RencanaTahunAkademik) data;

			RevisiHelper.createNewRevisi(RencanaTahunAkademik.class, rta, safe(rta.getNama())).setParent(row);
			new Label(safe(rta.getSemester())).setParent(row);
			new Label(formatTanggal(rta.getTanggalMulai())).setParent(row);
			new Label(formatTanggal(rta.getTanggalSampai())).setParent(row);
			new Label(formatTanggal(rta.getTanggalMulaiBelajarMengajar())).setParent(row);
			new Label(formatUnit(rta)).setParent(row);
			new Label(rta.getProgram() == null ? "Semua" : rta.getProgram()).setParent(row);
			new Label(rta.getStatusAwalMahasiswa() == null ? "Semua" : safe(rta.getStatusAwalMahasiswa().getNama()))
					.setParent(row);
			new Label(rta.getTahunAngkatan() == null ? "Semua" : rta.getTahunAngkatan() + "").setParent(row);
			new Label(safe(rta.getKeterangan())).setParent(row);

			Hbox toolbar = new Hbox();
			toolbar.setParent(row);

			MyToolbarbuttonConfig ubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			ubah.setTooltiptext("Ubah Data");
			ubah.setVisible(edit);
			ubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(rta);
					addWindow.setVisible(true);
					addWindow.onModal();
				}
			});
			ubah.setParent(toolbar);

			MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					konfirmasiHapus(rta);
				}
			});
			hapus.setParent(toolbar);
		}
	}

	private void konfirmasiHapus(final RencanaTahunAkademik data) throws Exception {
		MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						int pilihan = Integer.parseInt(event.getData().toString());
						if (pilihan != MyMessageboxConfig.OK) {
							return;
						}

						try {
							Session sessionData = HibernateUtil.currentSession();
							Common.refreshDelete(sessionData, data);
							onSearchDefault(event);
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							MyMessageboxConfig.show(
									"Data ini tidak dapat dihapus karena berelasi dengan data lainnya. Error: "
											+ safe(e.getMessage()));
						}
					}
				});
	}

	public void onAdd(Event event) throws Exception {
		init(new RencanaTahunAkademik());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(RencanaTahunAkademik data) {
		this.rencanaTahunAkademik = data == null ? new RencanaTahunAkademik() : data;

		addWindow.setTitle(data.getId() == null ? "Tambah Rencana Tahun Akademik" : "Ubah Rencana Tahun Akademik");
		Common.clear(addWindow);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid gridForm = new MyGrid();
		gridForm.setWidth("100%");
		gridForm.setHeight("100%");
		gridForm.setParent(center);

		Columns columns = new Columns();
		columns.setParent(gridForm);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(gridForm);

		initFormUmum(rows, rencanaTahunAkademik);
		initFormPerguruanTinggi(rows, rencanaTahunAkademik);
		initFormSekolah(rows, rencanaTahunAkademik);
		initFormKeterangan(rows, rencanaTahunAkademik);
		initToolbarForm(borderlayout);

		borderlayout.setParent(addWindow);
	}

	private void initFormUmum(Rows rows, RencanaTahunAkademik data) {
		Row row = createRow(rows, "Tahun Akademik");
		nama = new Combobox();
		Common.generateTahunAjaran(nama);
		Common.selectComboItem(nama, data.getNama());
		nama.setWidth("90%");
		nama.setReadonly(true);
		row.appendChild(nama);

		row = createRow(rows, "Semester");
		semester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.SP);
		comboitem.setValue(Perkuliahan.SP);
		semester.appendChild(comboitem);
		Common.selectComboItem(semester, data.getSemester());
		semester.setWidth("90%");
		semester.setReadonly(true);
		row.appendChild(semester);

		row = createRow(rows, "Tanggal Mulai *");
		tanggalMulai = new MyDatebox(data.getTanggalMulai());
		row.appendChild(tanggalMulai);

		row = createRow(rows, "Tanggal Sampai *");
		tanggalSampai = new MyDatebox(data.getTanggalSampai());
		row.appendChild(tanggalSampai);

		row = createRow(rows, "Tanggal Mulai Kegiatan Belajar Mengajar");
		tanggalMulaiBelajarMengajar = new MyDatebox(data.getTanggalMulaiBelajarMengajar());
		row.appendChild(tanggalMulaiBelajarMengajar);
	}

	private void initFormPerguruanTinggi(Rows rows, RencanaTahunAkademik data) {
		Tbmuser user = Common.getCurrentUser();

		Row row = createRow(rows, "Fakultas");
		row.setVisible(pt);
		Common.selectComboItem(fakultas, data.getFakultas() == null && user != null ? user.ambilFakultas()
				: data.getFakultas());
		fakultas.setWidth("90%");
		fakultas.setReadonly(false);
		row.appendChild(fakultas);

		if (pt) {
			Common.initKeterangan(rows, "Berlaku untuk \"" + Common.getBahasaConfig("Fakultas")
					+ "\" tertentu, kosongkan jika berlaku untuk semua.");
		}

		refreshJurusanByFakultas();

		row = createRow(rows, "Prodi");
		row.setVisible(pt);
		Common.pilihJurusan(jurusan, data.getJurusan() == null && user != null ? user.ambilJurusan()
				: data.getJurusan());
		jurusan.setWidth("90%");
		jurusan.setReadonly(false);
		row.appendChild(jurusan);

		if (pt) {
			Common.initKeterangan(rows, "Berlaku untuk \"" + Common.getBahasaConfig("Jurusan")
					+ "\" tertentu, kosongkan jika berlaku untuk semua.");
		}

		row = createRow(rows, "Program");
		row.setVisible(pt);
		Common.selectComboItem(program, data.getProgram());
		program.setWidth("90%");
		program.setReadonly(false);
		row.appendChild(program);

		if (pt) {
			Common.initKeterangan(rows, "Berlaku untuk \"Program\" tertentu, kosongkan jika berlaku untuk semua.");
		}

		row = createRow(rows, "Status Awal Mahasiswa");
		row.setVisible(pt);
		Common.selectComboItem(statusAwalMahasiswa, data.getStatusAwalMahasiswa());
		statusAwalMahasiswa.setWidth("90%");
		statusAwalMahasiswa.setReadonly(false);
		row.appendChild(statusAwalMahasiswa);

		if (pt) {
			Common.initKeterangan(rows,
					"Berlaku untuk mahasiswa dengan \"Status Awal Mahasiswa\" tertentu, kosongkan jika berlaku untuk semua.");
		}

		row = createRow(rows, "Tahun Angkatan Mahasiswa");
		row.setVisible(pt);
		tahunAngkatan = new Textbox(data.getTahunAngkatan() == null ? "" : data.getTahunAngkatan() + "");
		tahunAngkatan.setWidth("90%");
		row.appendChild(tahunAngkatan);

		if (pt) {
			Common.initKeterangan(rows,
					"Berlaku untuk mahasiswa dengan \"Tahun Angkatan\" tertentu, kosongkan jika berlaku untuk semua.");
		}
	}

	private void initFormSekolah(Rows rows, RencanaTahunAkademik data) {
		Tbmuser user = Common.getCurrentUser();
		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		Row row = createRow(rows, "Yayasan");
		row.setVisible(ya);
		Common.selectComboItem(yayasan, data.getYayasan() == null && user != null ? user.ambilYayasan()
				: data.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(false);
		row.appendChild(yayasan);

		row = createRow(rows, "Sekolah");
		row.setVisible(ya);
		Common.pilihSekolah(sekolah, data.getSekolah() == null && user != null ? user.ambilSekolah()
				: data.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(false);
		row.appendChild(sekolah);
	}

	private void initFormKeterangan(Rows rows, RencanaTahunAkademik data) {
		Row row = createRow(rows, "Keterangan");
		keterangan = new Textbox(safe(data.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		row.appendChild(keterangan);
	}

	private void initToolbarForm(Borderlayout borderlayout) {
		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.setTooltiptext("Tutup");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		batal.setParent(toolbar);

		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		simpan.setTooltiptext("Simpan");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		simpan.setParent(toolbar);
	}

	private Row createRow(Rows rows, String label) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(label));
		return row;
	}

	private void refreshJurusanByFakultas() {
		try {
			if (fakultas != null && fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
				jurusan.getItems().clear();
				Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (!validasiForm()) {
			return false;
		}

		if (checkNamaRencanaTahunAkademik()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun Akademik dan semester",
					"Tahun Akademik dan semester sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan Tahun Akademik dan semester yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session sessionData = HibernateUtil.currentSession();

		if (rencanaTahunAkademik.getId() != null) {
			rencanaTahunAkademik = (RencanaTahunAkademik) sessionData.load(RencanaTahunAkademik.class,
					rencanaTahunAkademik.getId());
		}

		isiDataDariForm(rencanaTahunAkademik);
		Common.refreshSaveOrUpdate(sessionData, rencanaTahunAkademik);
		reloadRencanaTahunAkademikAsync();

		return true;
	}

	private boolean validasiForm() throws Exception {
		if (nama == null || nama.getSelectedItem() == null || nama.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun Akademik",
					"Kolom Tahun Akademik belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tahun Akademik.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (semester == null || semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Semester",
					"Kolom Semester belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Semester.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tanggalMulai == null || tanggalMulai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal mulai",
					"Kolom Tanggal mulai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tanggal mulai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tanggalSampai == null || tanggalSampai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal sampai",
					"Kolom Tanggal sampai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tanggal sampai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tanggalMulai.getValue().after(tanggalSampai.getValue())) {
			MyMessageboxConfig.show("Tanggal mulai tidak boleh lebih besar dari tanggal sampai", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		return true;
	}

	private void isiDataDariForm(RencanaTahunAkademik data) {
		boolean sekolahScope = isSekolahScopeDipilih();

		data.setNama(selectedString(nama));
		data.setSemester(selectedString(semester));
		data.setKeterangan(keterangan == null ? null : keterangan.getValue());
		data.setTanggalMulai(tanggalMulai == null ? null : tanggalMulai.getValue());
		data.setTanggalSampai(tanggalSampai == null ? null : tanggalSampai.getValue());
		data.setTanggalMulaiBelajarMengajar(
				tanggalMulaiBelajarMengajar == null ? null : tanggalMulaiBelajarMengajar.getValue());

		if (sekolahScope) {
			data.setFakultas(null);
			data.setJurusan(null);
			data.setProgram(null);
			data.setStatusAwalMahasiswa(null);
			data.setTahunAngkatan(null);
			data.setYayasan(getSelectedYayasan(yayasan));
			data.setSekolah(getSelectedSekolah(sekolah));
		} else {
			data.setFakultas(pt ? getSelectedFakultas(fakultas) : null);
			data.setJurusan(pt ? getSelectedJurusan(jurusan) : null);
			data.setProgram(pt ? selectedString(program) : null);
			data.setStatusAwalMahasiswa(pt ? getSelectedStatusAwal(statusAwalMahasiswa) : null);
			data.setTahunAngkatan(pt ? parseInteger(tahunAngkatan == null ? null : tahunAngkatan.getValue()) : null);
			data.setYayasan(null);
			data.setSekolah(null);
		}
	}

	private boolean isSekolahScopeDipilih() {
		if (buatsekolah || (!pt && ya)) {
			return true;
		}
		if (!ya) {
			return false;
		}

		boolean adaSekolah = getSelectedYayasan(yayasan) != null || getSelectedSekolah(sekolah) != null;
		boolean adaPerguruanTinggi = getSelectedFakultas(fakultas) != null || getSelectedJurusan(jurusan) != null
				|| selectedString(program) != null || getSelectedStatusAwal(statusAwalMahasiswa) != null
				|| parseInteger(tahunAngkatan == null ? null : tahunAngkatan.getValue()) != null;

		return adaSekolah && !adaPerguruanTinggi;
	}

	private void reloadRencanaTahunAkademikAsync() {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.reloadRencanaTahunAkademik(HibernateUtil.currentSession());
			}
		});
	}

	public Criteria initCriteria(boolean order) {
		Session sessionData = HibernateUtil.currentSession();
		Criteria criteria = sessionData.createCriteria(RencanaTahunAkademik.class);

		if (order) {
			criteria.addOrder(Order.desc("nama")).addOrder(Order.desc("semester"));
		}

		boolean tampilSekolah = isSearchSekolahMode();
		if (tampilSekolah) {
			addEqIfSelected(criteria, "yayasan", getSelectedYayasan(searchyayasan));
			addEqIfSelected(criteria, "sekolah", getSelectedSekolah(searchsekolah));
		} else {
			addEqIfSelected(criteria, "fakultas", getSelectedFakultas(searchfakultas));
			addEqIfSelected(criteria, "jurusan", getSelectedJurusan(searchjurusan));
		}

		Object tahunAkademik = selectedValue(searchnama);
		if (tahunAkademik != null) {
			criteria.add(Restrictions.eq("nama", tahunAkademik));
		}

		Object status = selectedValue(searchStatusAwalMahasiswa);
		if (status != null && !tampilSekolah) {
			criteria.add(Restrictions.eq("statusAwalMahasiswa", status));
		}

		String selectedProgram = selectedString(searchprogram);
		if (selectedProgram != null && !tampilSekolah) {
			criteria.add(Restrictions.eq("program", selectedProgram));
		}

		Integer angkatan = parseInteger(searchTahunAngkatan == null ? null : searchTahunAngkatan.getValue());
		if (angkatan != null && !tampilSekolah) {
			criteria.add(Restrictions.eq("tahunAngkatan", angkatan));
		}

		return criteria;
	}

	private boolean isSearchSekolahMode() {
		if (buatsekolah || (!pt && ya)) {
			return true;
		}
		if (!ya) {
			return false;
		}
		if (!pt) {
			return true;
		}
		return getSelectedYayasan(searchyayasan) != null || getSelectedSekolah(searchsekolah) != null;
	}

	private void addEqIfSelected(Criteria criteria, String property, Object value) {
		if (criteria == null || property == null || value == null) {
			return;
		}
		// Lewati entity TRANSIENT (belum tersimpan, id == null) — mis. item placeholder "=Sekolah=" /
		// "=Yayasan=" pada combo filter yang value-nya objek baru tanpa id. Bila tetap di-eq, Hibernate
		// gagal mengikat parameter many-to-one saat list() → TransientObjectException → grid kosong &
		// filter error. Dengan dilewati: placeholder = tanpa filter (tampil semua), pilihan nyata tetap
		// memfilter seperti biasa.
		if (value instanceof ais.database.model.GeneralValueObject
				&& ((ais.database.model.GeneralValueObject) value).getId() == null) {
			return;
		}
		criteria.add(Restrictions.eq(property, value));
	}

	@SuppressWarnings({ "unchecked" })
	public void onSearchDefault(Event event) {
		try {
			Common.initPaging(initCriteria(false), paging);

			List<RencanaTahunAkademik> list = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

			ListModel model = new SimpleListModel(list);
			grid.setRowRenderer(new RencanaTahunAkademikRenderer());
			grid.setModelCheckMobile(model);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public Boolean checkNamaRencanaTahunAkademik() {
		Session sessionData = HibernateUtil.currentSession();
		Criteria criteria = sessionData.createCriteria(RencanaTahunAkademik.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", selectedString(nama)))
				.add(Restrictions.eq("semester", selectedString(semester)));

		if (rencanaTahunAkademik != null && rencanaTahunAkademik.getId() != null) {
			criteria.add(Restrictions.ne("id", rencanaTahunAkademik.getId()));
		}

		boolean sekolahScope = isSekolahScopeDipilih();
		if (sekolahScope) {
			criteria.add(Restrictions.isNull("fakultas"));
			criteria.add(Restrictions.isNull("jurusan"));
			criteria.add(Restrictions.isNull("program"));
			criteria.add(Restrictions.isNull("statusAwalMahasiswa"));
			criteria.add(Restrictions.isNull("tahunAngkatan"));
			addExactNullable(criteria, "yayasan", getSelectedYayasan(yayasan));
			addExactNullable(criteria, "sekolah", getSelectedSekolah(sekolah));
		} else {
			criteria.add(Restrictions.isNull("yayasan"));
			criteria.add(Restrictions.isNull("sekolah"));
			addExactNullable(criteria, "fakultas", pt ? getSelectedFakultas(fakultas) : null);
			addExactNullable(criteria, "jurusan", pt ? getSelectedJurusan(jurusan) : null);
			addExactNullable(criteria, "program", pt ? selectedString(program) : null);
			addExactNullable(criteria, "statusAwalMahasiswa", pt ? getSelectedStatusAwal(statusAwalMahasiswa) : null);
			addExactNullable(criteria, "tahunAngkatan",
					pt ? parseInteger(tahunAngkatan == null ? null : tahunAngkatan.getValue()) : null);
		}

		Number count = (Number) criteria.uniqueResult();
		return count != null && count.intValue() > 0;
	}

	private void addExactNullable(Criteria criteria, String property, Object value) {
		if (criteria == null || property == null) {
			return;
		}
		// Akar sama dgn addEqIfSelected: entity TRANSIENT (id null, mis. placeholder combo) dianggap
		// "tidak terpilih" (null) agar tidak memicu TransientObjectException saat bind parameter many-to-one.
		if (value instanceof ais.database.model.GeneralValueObject
				&& ((ais.database.model.GeneralValueObject) value).getId() == null) {
			value = null;
		}
		criteria.add(value == null ? Restrictions.isNull(property) : Restrictions.eq(property, value));
	}

	public static RencanaTahunAkademik getCurrentRencanaTahunAkademik(Date sekarang) {
		return getCurrentRencanaTahunAkademik(Common.getCurrentUser(), sekarang);
	}

	public static RencanaTahunAkademik getCurrentRencanaTahunAkademik(Tbmuser tbmuser, Date sekarang) {
		Fakultas fakultas = tbmuser == null ? null : tbmuser.ambilFakultas();
		Jurusan jurusan = tbmuser == null ? null : tbmuser.ambilJurusan();
		Sekolah sekolah = tbmuser == null ? null : tbmuser.ambilSekolah();
		Yayasan yayasan = tbmuser == null ? null : tbmuser.ambilYayasan();
		String program = tbmuser == null ? null
				: (tbmuser.ambilProgram() == null ? null : tbmuser.ambilProgram().getNama());

		StatusAwalMahasiswa statusAwalMahasiswa = null;
		Integer tahunAngkatan = null;

		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			fakultas = tbmuser.getMahasiswa().getJurusan() == null ? null
					: tbmuser.getMahasiswa().getJurusan().getFakultas();
			jurusan = tbmuser.getMahasiswa().getJurusan();
			program = tbmuser.getMahasiswa().getProgram();
			statusAwalMahasiswa = tbmuser.getMahasiswa().getStatusAwalMahasiswa();
			tahunAngkatan = tbmuser.getMahasiswa().getTahunangkatan();
			sekolah = null;
			yayasan = null;
		} else if (tbmuser != null && tbmuser.getDosen() != null) {
			fakultas = tbmuser.getDosen().getFakultas();
			jurusan = tbmuser.getDosen().getJurusan();
			sekolah = null;
			yayasan = null;
		} else if (tbmuser != null && tbmuser.getSiswa() != null) {
			sekolah = tbmuser.getSiswa().getSekolah();
			fakultas = null;
			jurusan = null;
		} else if (tbmuser != null && tbmuser.getGuru() != null) {
			sekolah = tbmuser.getGuru().getSekolah();
			fakultas = null;
			jurusan = null;
		}

		if (sekolah == null && fakultas == null && jurusan == null) {
			try {
				sekolah = SekolahUtil.getSekolah();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}
		if (yayasan == null && fakultas == null && jurusan == null) {
			try {
				yayasan = SekolahUtil.getYayasan();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}

		if (fakultas != null || jurusan != null) {
			sekolah = null;
			yayasan = null;
		}

		return getCurrentRencanaTahunAkademik(fakultas, jurusan, yayasan, sekolah, statusAwalMahasiswa, tahunAngkatan,
				program, sekarang, null, null);
	}

	public static RencanaTahunAkademik getCurrentRencanaTahunAkademik(Fakultas fakultas, Jurusan jurusan,
			Yayasan yayasan, Sekolah sekolah, StatusAwalMahasiswa statusAwalMahasiswa, Integer tahunAngkatan,
			String program, Date sekarangB, String ta, String smt) {

		reloadCacheRencanaTahunAkademikJikaKosong();

		if (Common.rencanaTahunAkademiks == null || Common.rencanaTahunAkademiks.isEmpty()) {
			return null;
		}

		RencanaTahunAkademik bestMatch = null;
		int highestScore = -1;

		for (RencanaTahunAkademik rta : Common.rencanaTahunAkademiks) {
			if (!matchWaktu(rta, sekarangB, ta, smt)) {
				continue;
			}

			int score = hitungScoreRencana(rta, fakultas, jurusan, yayasan, sekolah, statusAwalMahasiswa,
					tahunAngkatan, program);

			if (score < 0) {
				continue;
			}

			if (bestMatch == null || score > highestScore || (score == highestScore && lebihBaru(rta, bestMatch))) {
				bestMatch = rta;
				highestScore = score;
			}
		}

		return bestMatch;
	}

	private static void reloadCacheRencanaTahunAkademikJikaKosong() {
		if (Common.rencanaTahunAkademiks != null && !Common.rencanaTahunAkademiks.isEmpty()) {
			return;
		}

		org.hibernate.Session sessionData = null;
		try {
			sessionData = HibernateUtil.getSessionFactory().openSession();
			Common.reloadRencanaTahunAkademik(sessionData);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSession(sessionData);
		}
	}

	private static boolean matchWaktu(RencanaTahunAkademik rta, Date sekarang, String ta, String smt) {
		if (rta == null) {
			return false;
		}

		if (sekarang == null) {
			return ta != null && smt != null && ta.equalsIgnoreCase(rta.getNama())
					&& smt.equalsIgnoreCase(rta.getSemester());
		}

		return rta.getTanggalMulai() != null && rta.getTanggalSampai() != null
				&& !rta.getTanggalMulai().after(sekarang) && !rta.getTanggalSampai().before(sekarang);
	}

	private static int hitungScoreRencana(RencanaTahunAkademik rta, Fakultas fakultas, Jurusan jurusan, Yayasan yayasan,
			Sekolah sekolah, StatusAwalMahasiswa statusAwalMahasiswa, Integer tahunAngkatan, String program) {
		int score = 0;

		score = tambahScoreJikaCocok(score, rta.getSekolah(), sekolah, 1000);
		if (score < 0) {
			return -1;
		}

		score = tambahScoreJikaCocok(score, rta.getYayasan(), yayasan, 100);
		if (score < 0) {
			return -1;
		}

		score = tambahScoreJikaCocok(score, rta.getFakultas(), fakultas, 80);
		if (score < 0) {
			return -1;
		}

		score = tambahScoreJikaCocok(score, rta.getJurusan(), jurusan, 90);
		if (score < 0) {
			return -1;
		}

		if (rta.getProgram() != null) {
			if (program == null || !rta.getProgram().equals(program)) {
				return -1;
			}
			score += 40;
		}

		score = tambahScoreJikaCocok(score, rta.getStatusAwalMahasiswa(), statusAwalMahasiswa, 30);
		if (score < 0) {
			return -1;
		}

		if (rta.getTahunAngkatan() != null) {
			if (tahunAngkatan == null || !rta.getTahunAngkatan().equals(tahunAngkatan)) {
				return -1;
			}
			score += 20;
		}

		return score;
	}

	private static int tambahScoreJikaCocok(int score, Object rtaValue, Object requestValue, int nilai) {
		if (rtaValue == null) {
			return score;
		}
		if (requestValue == null) {
			return score;
		}

		try {
			Object idA = GeneralIdUtil.getId(rtaValue);
			Object idB = GeneralIdUtil.getId(requestValue);
			if (idA != null && idA.equals(idB)) {
				return score + nilai;
			}
		} catch (Exception e) {
			if (rtaValue.equals(requestValue)) {
				return score + nilai;
			}
		}

		return -1;
	}

	private static boolean lebihBaru(RencanaTahunAkademik a, RencanaTahunAkademik b) {
		if (a == null || b == null) {
			return false;
		}
		if (a.getTanggalMulai() == null) {
			return false;
		}
		if (b.getTanggalMulai() == null) {
			return true;
		}
		return a.getTanggalMulai().after(b.getTanggalMulai());
	}

	private static void closeSession(org.hibernate.Session sessionData) {
		if (sessionData == null) {
			return;
		}
		try {
			sessionData.clear();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			sessionData.disconnect();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			sessionData.close();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private static class GeneralIdUtil {
		public static Object getId(Object object) throws Exception {
			return object == null ? null : object.getClass().getMethod("getId", new Class[] {}).invoke(object,
					new Object[] {});
		}
	}

	private Object selectedValue(Combobox combo) {
		return combo == null || combo.getSelectedItem() == null ? null : combo.getSelectedItem().getValue();
	}

	private String selectedString(Combobox combo) {
		Object value = selectedValue(combo);
		if (value == null) {
			return null;
		}
		String text = value.toString();
		return text.trim().length() == 0 ? null : text.trim();
	}

	private Fakultas getSelectedFakultas(Combobox combo) {
		Object value = selectedValue(combo);
		return value instanceof Fakultas ? (Fakultas) value : null;
	}

	private Jurusan getSelectedJurusan(Combobox combo) {
		Object value = selectedValue(combo);
		return value instanceof Jurusan ? (Jurusan) value : null;
	}

	private Yayasan getSelectedYayasan(Combobox combo) {
		Object value = selectedValue(combo);
		return value instanceof Yayasan ? (Yayasan) value : null;
	}

	private Sekolah getSelectedSekolah(Combobox combo) {
		Object value = selectedValue(combo);
		return value instanceof Sekolah ? (Sekolah) value : null;
	}

	private StatusAwalMahasiswa getSelectedStatusAwal(Combobox combo) {
		Object value = selectedValue(combo);
		return value instanceof StatusAwalMahasiswa ? (StatusAwalMahasiswa) value : null;
	}

	private Integer parseInteger(String value) {
		if (value == null || value.trim().length() == 0) {
			return null;
		}
		try {
			return Integer.valueOf(value.trim());
		} catch (Exception e) {
			return null;
		}
	}

	private static String formatTanggal(Date value) {
		return value == null ? "" : Common.dateFormat4.get().format(value);
	}

	private static String safe(String value) {
		return value == null ? "" : value;
	}

	private static String formatUnit(RencanaTahunAkademik rta) {
		if (rta == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		if (rta.getFakultas() != null) {
			sb.append(safe(rta.getFakultas().getNama()));
		}
		if (rta.getJurusan() != null) {
			if (sb.length() > 0) {
				sb.append(" / ");
			}
			sb.append(safe(rta.getJurusan().getNama()));
		}
		if (rta.getYayasan() != null) {
			if (sb.length() > 0) {
				sb.append(" / ");
			}
			sb.append(safe(rta.getYayasan().getNama()));
		}
		if (rta.getSekolah() != null) {
			if (sb.length() > 0) {
				sb.append(" / ");
			}
			sb.append(safe(rta.getSekolah().getNama()));
		}
		return sb.length() == 0 ? "Semua" : sb.toString();
	}
}
