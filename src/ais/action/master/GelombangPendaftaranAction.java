package ais.action.master;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
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
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AktifitasPerkuliahanHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisDiskonMahasiswa;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jenjang;
import ais.database.model.KelompokParameterTambahanCalonMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Paket;
import ais.database.model.PaketPunyaGelombangPendaftaran;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.VerifikasiKelengkapanCalonMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.database.model.sekolah.KelompokGelombang;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class GelombangPendaftaranAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchTahunAjaran;
	private Checkbox searchaktif;

	private Textbox kode;
	private Textbox nama;
	private Textbox keterangan;
	private Combobox tahunAkademik;
	private Combobox jenisSeleksi;
	private Combobox jenisDiskonMahasiswa;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private MyCheckboxConfig bisaDipilihPendaftarOnline;
	private MyCheckboxConfig tampilFormTambahanSaatRegistrasi;
	private MyCheckboxConfig tampilFormTambahanSaatLoginCalonMhs;
	private Combobox jenjang;
	private Combobox jenisSemester;
	private MyDatebox tanggalLoginCalonMahasiswaBerakhir;
	private MyDatebox tanggalDaftarUlangBerakhir;
	private Combobox statusAwalMahasiswa;

	private MyCheckboxConfig dibatasiUmur;
	private Intbox umurmaksimal;

	private Intbox tahunAngkatanMinimal;
	private Intbox tahunAngkatanMaksimal;

	private boolean edit = false;
	private boolean delete = false;

	private GelombangPendaftaran gelombangPendaftaran;
	private MyToolbarbuttonConfig add;

	private Label lblTahunAkaemik;
	private MyColumnConfig colTahunAkademik;
	private MyColumnConfig colJenisSmt;
	private MyColumnConfig colBisaDipilih;
	private MyColumnConfig coljenisSeleksi;
	private MyColumnConfig coljenisJenjang;
	private MyColumnConfig colLoginTerakhir;

	private Tabpanel kelompok;

	public void onKelompok(Event event) {
		bukaHalamanTab(kelompok, "/pages/master/sekolah/kelompok_gelombang.zul");
	}

	private Tabpanel verifikasi;

	private HashMap<Long, PaketPunyaGelombangPendaftaran> selectedPaketPunyaGelombangPendaftaran;

	public void onVerifikasiKelengkapanBerkas(Event event) {
		bukaHalamanTab(verifikasi, "/pages/master/verifikasi_kelengkapan_calon_mahasiswa.zul");
	}

	private Tabpanel pembagianKelas;

	public void onPembagianKelas(Event event) {
		bukaHalamanTab(pembagianKelas, "/pages/master/kelas_pmb.zul");
	}

	private void bukaHalamanTab(Tabpanel tabpanel, String zulPath) {
		if (tabpanel == null || zulPath == null || zulPath.trim().length() == 0) {
			return;
		}
		if (tabpanel.getChildren() != null && tabpanel.getChildren().size() > 0) {
			return;
		}
		MyWindow window = new MyWindow("", "none", false);
		window.setHeight("100%");
		window.setWidth("100%");
		window.setParent(tabpanel);
		MyInclude iframe = new MyInclude(zulPath);
		iframe.setParent(window);
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private boolean tampilSederhana = false;
	private String tahunAkademikPenerimaanMahasiswaBaru;
	private Set<VerifikasiKelengkapanCalonMahasiswa> selectedVerifikasiKelengkapanCalonMahasiswa;
	private Set<JenisSeleksi> selectedJenisSeleksi;
	private Combobox program;
	private MyCheckboxConfig tidakBolehMemilihProgramLain;
	private MyCheckboxConfig mahasiswaPindahanBolehMendaftar;
	private Intbox umurminimal;
	private PerguruanTinggi selectedPerguruanTinggi;
	private MyCheckboxConfig harusIkutStatusAwalDefault;
	private MyCheckboxConfig dokumenHarusDiverivikasiSebelumBisaCetakKartuUjian;
	private MyCheckboxConfig dokumenHarusDiverivikasiSebelumBisaIkutUjian;
	private Combobox onlineMenggunakan;
	private Row rowMeetKeterangan;
	private Row rowMeet;
	private Row rowLinkZoomKeterangan;
	private Row rowLinkZoomLink;
	private Row rowLinkZoom;
	private Textbox zoomLink;
	private Row rowLinkZoomButton;
	private Row rowLinkBbbKeterangan;
	private Row rowLinkBbbLink;
	private Row rowLinkBbb;
	private Textbox bbbLink;
	private Row rowLinkBbbButton;
	private Row rowLinkSkypeKeterangan;
	private Row rowLinkSkypeLink;
	private Row rowLinkSkype;
	private Textbox skypeLink;
	private Row rowLinkSkypeButton;
	private Row rowLinkWa;
	private Textbox waLink;
	private Row rowLinkWaButton;
	private Row rowLinkWaKeterangan;
	private Set<KelompokParameterTambahanCalonMahasiswa> selectedKelompokParameterTambahanCalonMahasiswa;
	private Row rowLinkLainKeterangan;
	private Row rowLinkLain;
	private Textbox linkLain;
	private MyCheckboxConfig dokumenHarusDiverivikasiSebelumBisaSimpan;
	private Row rowJp;
	private MyCheckboxConfig hanyapaket;
	private MyCheckboxConfig tidakBolehMendaftarMhsYgSama;
	private MyCheckboxConfig tidakBolehNikSama;
	private MyCheckboxConfig harusBayarSebelumBisaLogin;
	private MyCheckboxConfig jenisSeleksiDipilihDiFormPendaftaran;
	private MyCheckboxConfig tampilkanMasaPendaftaranKeCalonMahasiswa;
	private Textbox info;
	protected LampiranLain kop;
	private MyCkEditor infoSetelahUjianOnline;
	private MyCkEditor infoSaatInterview;
	private MyCheckboxConfig terdapatUjianOnline;
	private MyCheckboxConfig terdapatInterview;
	private MyCheckboxConfig fotoWajibDiuplad;
	private MyCheckboxConfig tampilkanUploadFoto;
	protected LampiranLain lainMahasiswa;
	private MyCheckboxConfig otomatisLoginSetelahDaftar;
	private MyDatebox tanggalTagihanRegistrasi;
	private MyDatebox tanggalTagihanDaftarUlang;
	protected LampiranLain bg_ppdb;
	private MyCheckboxConfig harusSebagaiAlumni;
	private MyCheckboxConfig ujianOnlineOtomatisDiterima;
	private MyDoublebox nilaiMinimalUjianOnlineOtomatisDiterima;
	private MyCheckboxConfig otomatisDiterimaSaatDaftar;
	protected LampiranLain icon;
	private MyCheckboxConfig tampilkanQrCodeMahasiswaSetelahDapatNim;
	private Combobox kelompokGelombang;

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
		tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);

		tampilSederhana = Common.bolehKonfigurasi("tampil_gelombang_sederhana", Konfigurasi.TIDAK_AKTIF);

		if (tampilSederhana) {
			lblTahunAkaemik.setVisible(false);
			searchTahunAjaran.setVisible(false);
			searchTahunAjaran.setSelectedItem(null);

			colTahunAkademik.setVisible(false);
			colJenisSmt.setVisible(false);
			colBisaDipilih.setVisible(false);
			coljenisSeleksi.setVisible(false);
			coljenisJenjang.setVisible(false);
			colLoginTerakhir.setVisible(false);
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		jenisSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		jenisSemester.appendChild(comboitem);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

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

	class GelombangPendaftaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			arg0.setValign("top");
			final GelombangPendaftaran gelombangPendaftaran = (GelombangPendaftaran) arg1;
			gelombangPendaftaran.chekKuotaPendaftar();
			MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			HibernateUtil.currentSession().refresh(gelombangPendaftaran);
			List<VerifikasiKelengkapanCalonMahasiswa> berkas = new ArrayList<VerifikasiKelengkapanCalonMahasiswa>(
					gelombangPendaftaran.getVerifikasiKelengkapanCalonMahasiswas());

			try {
				Collections.sort(berkas);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/GelombangPendaftaranAction.java:334");
				// TODO: handle exception
			}

			Vbox vbox = new Vbox();
			vbox.setWidth("100%");
			vbox.setParent(detail);
			if (!berkas.isEmpty()) {

				vbox.appendChild(new MyLabelBoldAja("Kelengkapan Berkas :"));
				int i = 1;
				for (VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa : berkas) {
					vbox.appendChild(new MyLabelKecil(i + ". " + verifikasiKelengkapanCalonMahasiswa.getNama()));
					i++;
				}
				vbox.appendChild(new Html("<hr>"));
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(GelombangPendaftaran.class, gelombangPendaftaran,
					gelombangPendaftaran.getKode() + " " + gelombangPendaftaran.getNama())).setParent(arg0);

			if (gelombangPendaftaran.getKelompokGelombang() != null) {
				new Label(gelombangPendaftaran.getKelompokGelombang().getNama()).setParent(a);
			}

			new Label(gelombangPendaftaran.getTahunAkademik()).setParent(arg0);
			new Label(gelombangPendaftaran.getJenisSemester()).setParent(arg0);
			new Label(gelombangPendaftaran.getBisaDipilihPendaftarOnline() ? "Ya" : "Tidak").setParent(arg0);

			new Label("Default: " + (gelombangPendaftaran.getJenisSeleksi() == null ? ""
					: gelombangPendaftaran.getJenisSeleksi().getNama())).setParent(arg0);

			if (!gelombangPendaftaran.getJenisSeleksiLain().isEmpty()) {
				vbox.appendChild(new MyLabelBoldAja("Jenis Seleksi :"));
				int i = 1;

				for (JenisSeleksi jenisSeleksi : gelombangPendaftaran.ambilJenisSeleksi()) {
					new MyLabelKecil(i + ". " + jenisSeleksi.getNama()).setParent(vbox);
					i++;
				}

				vbox.appendChild(new Html("<hr>"));
			}

			new Label(gelombangPendaftaran.getJenjang() == null ? "Semua" : gelombangPendaftaran.getJenjang().getNama())
					.setParent(arg0);
			new Label(gelombangPendaftaran.getJenisDiskonMahasiswa() == null ? ""
					: gelombangPendaftaran.getJenisDiskonMahasiswa().getNama()).setParent(arg0);
			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			new Label(Common.dateFormat11.get().format(gelombangPendaftaran.getMulai())).setParent(hbox);
			new Label(Common.dateFormat11.get().format(gelombangPendaftaran.getSampai())).setParent(hbox);

			new Label(gelombangPendaftaran.getTanggalLoginCalonMahasiswaBerakhir() == null ? ""
					: Common.dateFormat2.get().format(gelombangPendaftaran.getTanggalLoginCalonMahasiswaBerakhir()))
					.setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(gelombangPendaftaran.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					gelombangPendaftaran.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(gelombangPendaftaran);
				}
			});

			new Label(gelombangPendaftaran.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			(toolbar = Common.copyEditDeleteButtons(edit, delete, gelombangPendaftaran,
					GelombangPendaftaranAction.this)).setParent(arg0);

			if (!gelombangPendaftaran.getOnlineMenggunakan().equals(GelombangPendaftaran.TIDAK_AKTIF)) {
				GelombangPendaftaran.createVideoConrefrence(gelombangPendaftaran, toolbar, true, true,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								// TODO Auto-generated method stub

							}
						});
			}

		}

	}

	public void onAdd(Event event) throws Exception {
		GelombangPendaftaran gelombangPendaftaran = new GelombangPendaftaran();
		gelombangPendaftaran.setTahunAkademik(tahunAkademikPenerimaanMahasiswaBaru);
		init(gelombangPendaftaran);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(final GelombangPendaftaran gelombangPendaftaran) throws Exception {
		this.gelombangPendaftaran = gelombangPendaftaran;
		addWindow.setTitle(gelombangPendaftaran.getId() == null ? "Tambah Gelombang Pendaftaran" : "Ubah Gelombang Pendaftaran");
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

		int countCalon = 0;
		if (!Common.getApakahAdmin() && gelombangPendaftaran != null && gelombangPendaftaran.getId() != null) {
			countCalon = ((Number) HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran)).uniqueResult()).intValue();
		}

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Gelombang (*)"));
		row.appendChild(
				nama = new Textbox(gelombangPendaftaran.getNama() == null ? "" : gelombangPendaftaran.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox(gelombangPendaftaran.getKode()));
		kode.setWidth("90%");

		kop = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KOP Formulir Pendaftaran "));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, gelombangPendaftaran.getId(), LampiranLain.KOP_GELOMBANG_PMB,
				"KOP", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kop = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		bg_ppdb = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Background Gelombang (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, gelombangPendaftaran.getId(), LampiranLain.BG_PMB_GELOMBANG,
				"Background", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						bg_ppdb = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		icon = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Icon Gelombang (PNG)"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, gelombangPendaftaran.getId(), LampiranLain.ICON_GELOMBANG_PMB,
				"Icon", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						icon = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik (*)"));
		tahunAkademik = new Combobox();
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
		Common.selectComboItem(tahunAkademik, gelombangPendaftaran.getTahunAkademik());
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setDisabled(countCalon > 0);
		tahunAkademik.setReadonly(true);

		if (countCalon > 0) {
			Common.initKeterangan(rows,
					"Tahun akademik tidak boleh diubah, karena sudah ada mahasiswa yang mengambil gelombang ini");
		}

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester (*)"));
		Common.selectComboItem(jenisSemester, gelombangPendaftaran.getJenisSemester());
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setDisabled(countCalon > 0);
		jenisSemester.setReadonly(true);

		if (countCalon > 0) {
			Common.initKeterangan(rows,
					"Jenis Semester tidak boleh diubah, karena sudah ada mahasiswa yang mengambil gelombang ini");
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok Gelombang"));
		row.appendChild(kelompokGelombang = new Combobox());
		Common.insertComboDanSemua(kelompokGelombang, new String[] { "nama" }, "kode", KelompokGelombang.class,
				"Tanpa Kelompok", Restrictions.eq("aktif", true));
		Common.selectComboItem(kelompokGelombang, gelombangPendaftaran.getKelompokGelombang());
		kelompokGelombang.setWidth("90%");
		kelompokGelombang.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Seleksi Default"));
		jenisSeleksi = new Combobox();
		Common.insertComboDanSemua(jenisSeleksi, new String[] { "nama" }, "deskripsi", JenisSeleksi.class,
				"=tidak ada default=", Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jenisSeleksi, gelombangPendaftaran.getJenisSeleksi());
		row.appendChild(jenisSeleksi);
		jenisSeleksi.setWidth("90%");
		jenisSeleksi.setReadonly(true);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		final MyCheckboxConfig jenisSeleksiForm;
		row.appendChild(jenisSeleksiForm = new MyCheckboxConfig("Terdapat jenis seleksi lain"));
		row.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid subGridJenisSeleksi = new MyGrid();
		row.appendChild(subGridJenisSeleksi);

		Columns subColumns = new Columns();
		subColumns.setParent(subGridJenisSeleksi);
		Column c = new Column("Jenis Seleksi");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGridJenisSeleksi);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		Session session = HibernateUtil.currentSession();

		@SuppressWarnings("unchecked")
		List<JenisSeleksi> jenisSeleksis = ConstantValues.simpleList(
				session.createCriteria(JenisSeleksi.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				JenisSeleksi.class);

		if (gelombangPendaftaran.getCopyDari() != null && gelombangPendaftaran.getCopyDari().getId() != null) {

			GelombangPendaftaran gelombangPendaftaranCopy = (GelombangPendaftaran) gelombangPendaftaran.getCopyDari();
			session.refresh(gelombangPendaftaranCopy);
			selectedJenisSeleksi = new HashSet<JenisSeleksi>();

			for (JenisSeleksi jenisSeleksi : gelombangPendaftaranCopy.ambilJenisSeleksi()) {
				selectedJenisSeleksi.add(jenisSeleksi);
			}

		} else {

			if (gelombangPendaftaran.getId() != null) {
				session.refresh(this.gelombangPendaftaran);
			}
			selectedJenisSeleksi = new HashSet<JenisSeleksi>();

			for (JenisSeleksi jenisSeleksi : gelombangPendaftaran.ambilJenisSeleksi()) {
				selectedJenisSeleksi.add(jenisSeleksi);
			}

		}

		Set<Long> ids = new HashSet<Long>();
		for (JenisSeleksi v : selectedJenisSeleksi) {
			ids.add(v.getId());
		}

		System.out.println("ids ->" + ids);

		subGridJenisSeleksi.setVisible(!selectedJenisSeleksi.isEmpty());
		jenisSeleksiForm.setChecked(!selectedJenisSeleksi.isEmpty());

		jenisSeleksiForm.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				subGridJenisSeleksi.setVisible(jenisSeleksiForm.isChecked());
			}
		});

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final JenisSeleksi jenisSeleksi : jenisSeleksis) {
			final Checkbox checkbox = new Checkbox(jenisSeleksi.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(ids.contains(jenisSeleksi.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedJenisSeleksi.add(jenisSeleksi);
					} else {
						for (JenisSeleksi a : selectedJenisSeleksi) {
							if (a.getId().equals(jenisSeleksi.getId())) {
								selectedJenisSeleksi.remove(a);
								break;
							}
						}
					}

					System.out.println("selectedJenisSeleksi => " + selectedJenisSeleksi);
				}
			});
		}

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(jenisSeleksiDipilihDiFormPendaftaran = new MyCheckboxConfig(
				"Jenis Seleksi Dipilih Di Form Pendaftaran"));
		jenisSeleksiDipilihDiFormPendaftaran.setChecked(gelombangPendaftaran.getJenisSeleksiDipilihDiFormPendaftaran());
		row.setParent(rows);

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Diskon Default"));
		jenisDiskonMahasiswa = new Combobox();
		Common.insertComboDanSemua(jenisDiskonMahasiswa, new String[] { "nama" }, "keterangan", JenisDiskonMahasiswa.class,
				"=tidak ada diskon=", Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jenisDiskonMahasiswa, gelombangPendaftaran.getJenisDiskonMahasiswa());
		row.appendChild(jenisDiskonMahasiswa);
		jenisDiskonMahasiswa.setWidth("90%");
		jenisDiskonMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program Default"));
		program = Common.initPrograms(program);
		row.appendChild(program);
		program.setWidth("90%");
		Common.selectComboItem(program, gelombangPendaftaran.getProgram());
		program.setReadonly(true);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tidakBolehMemilihProgramLain = new MyCheckboxConfig("Tidak boleh memilih program lain"));
		tidakBolehMemilihProgramLain.setChecked(gelombangPendaftaran.getTidakBolehMemilihProgramLain());
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(mahasiswaPindahanBolehMendaftar = new MyCheckboxConfig("Mahasiswa Pindahan Boleh Mendaftar"));
		mahasiswaPindahanBolehMendaftar.setChecked(gelombangPendaftaran.getMahasiswaPindahanBolehMendaftar());
		row.setParent(rows);

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal Mahasiswa Default"));
		statusAwalMahasiswa = new Combobox();
		Common.insertCombo(statusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		row.appendChild(statusAwalMahasiswa);
		Common.selectComboItem(statusAwalMahasiswa, gelombangPendaftaran.getStatusAwalMahasiswaDefault());
		statusAwalMahasiswa.setWidth("90%");
		statusAwalMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(harusIkutStatusAwalDefault = new MyCheckboxConfig(
				"Calon mahasiswa harus mengikuti status awal default"));
		harusIkutStatusAwalDefault.setChecked(gelombangPendaftaran.getHarusIkutStatusAwalDefault());
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tidakBolehMendaftarMhsYgSama = new MyCheckboxConfig(
				"Hanya satu calon mahasiswa yang boleh mendaftar di satu tahun akadmeik yang sama"));
		tidakBolehMendaftarMhsYgSama.setChecked(gelombangPendaftaran.getTidakBolehMendaftarMhsYgSama());
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tidakBolehNikSama = new MyCheckboxConfig(
				"NIK (Nomor Identitas) tidak boleh sama antar pendaftar di gelombang ini"));
		tidakBolehNikSama.setChecked(gelombangPendaftaran.getTidakBolehNikSama());
		row.setParent(rows);

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang (*)"));
		jenjang = new Combobox();
		Common.insertCombo(jenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		MyComboitemConfig comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		jenjang.appendChild(comboitem);
		Common.selectComboItem(jenjang, gelombangPendaftaran.getJenjang());
		row.appendChild(jenjang);
		jenjang.setWidth("90%");

		if (jenjang.getSelectedItem() == null) {
			jenjang.setSelectedItem(comboitem);
		}

		jenjang.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(bisaDipilihPendaftarOnline = new MyCheckboxConfig("Bisa Dipilih Pendaftar Secara Online"));
		bisaDipilihPendaftarOnline.setChecked(gelombangPendaftaran.getBisaDipilihPendaftarOnline());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Media Online"));
		onlineMenggunakan = new Combobox();

		Comboitem mediaOnline = new Comboitem("Jitsi", "/img/jitsi.png");
		mediaOnline.setValue(GelombangPendaftaran.JITSI);
		onlineMenggunakan.appendChild(mediaOnline);

		mediaOnline = new Comboitem("Google Meet", "/img/meet-google.png");
		mediaOnline.setValue(GelombangPendaftaran.GOOGLE_MEET);
		onlineMenggunakan.appendChild(mediaOnline);

		mediaOnline = new Comboitem("Zoom", "/img/zoom.png");
		mediaOnline.setValue(GelombangPendaftaran.ZOOM);
		onlineMenggunakan.appendChild(mediaOnline);

		mediaOnline = new Comboitem("Big Blue Button", "/img/bbb.png");
		mediaOnline.setValue(GelombangPendaftaran.BBB);
		onlineMenggunakan.appendChild(mediaOnline);

		mediaOnline = new Comboitem("Skype", "/img/Skype-icon.png");
		mediaOnline.setValue(GelombangPendaftaran.SKYPE);
		onlineMenggunakan.appendChild(mediaOnline);

		mediaOnline = new Comboitem("Grup Whatsapp", "/img/svg/whats.svg");
		mediaOnline.setValue(Pertemuan.WA);
		onlineMenggunakan.appendChild(mediaOnline);

		mediaOnline = new Comboitem("Lain-Lain", "/img/online-red-icon.png");
		mediaOnline.setValue(Pertemuan.LAIN);
		onlineMenggunakan.appendChild(mediaOnline);

		mediaOnline = new Comboitem("Tidak Ada Tatap Muka Online", "/img/svg/trash.svg");
		mediaOnline.setValue(GelombangPendaftaran.TIDAK_AKTIF);
		onlineMenggunakan.appendChild(mediaOnline);

		Common.selectComboItem(onlineMenggunakan, gelombangPendaftaran.getOnlineMenggunakan());
		onlineMenggunakan.setCols(7);

		Hbox myonlineMenggunakan = new Hbox();
		row.appendChild(myonlineMenggunakan);
		myonlineMenggunakan.appendChild(onlineMenggunakan);

		final MyToolbarbuttonConfig testButton = new MyToolbarbuttonConfig("Tes Online Sekarang");
		myonlineMenggunakan.appendChild(testButton);
		testButton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Integer ol = (Integer) onlineMenggunakan.getSelectedItem().getValue();
				String url = "";
				if (ol.equals(GelombangPendaftaran.GOOGLE_MEET)) {
					String l = gelombangPendaftaran.retreive("hangoutLink");
					if (l == null || l.trim().isEmpty()) {
						MyMessageboxConfig.show(
								"Untuk tatap muka online menggunakan Google Meet, harap singkronkan dulu ke Google Calendar dengan cara klik terlebih dahulu tombol Kalendar.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
					url = l + "?hs=122&ijlm=1588886137268";
				} else if (ol.equals(GelombangPendaftaran.JITSI)) {
					url = gelombangPendaftaran.generateJitsiLink();
				} else if (ol.equals(GelombangPendaftaran.ZOOM)) {
					url = gelombangPendaftaran.getZoomLink();
				} else if (ol.equals(GelombangPendaftaran.BBB)) {
					url = gelombangPendaftaran.getBbbLink();
				} else if (ol.equals(GelombangPendaftaran.SKYPE)) {
					url = gelombangPendaftaran.getSkypeLink();
				} else if (ol.equals(GelombangPendaftaran.WA)) {
					url = gelombangPendaftaran.getWaLink();
				} else if (ol.equals(GelombangPendaftaran.LAIN)) {
					url = gelombangPendaftaran.getLainLink();
				}
				if (url == null || url.trim().isEmpty()) {
					MyMessageboxConfig.show(
							"Untuk tatap muka online menggunakan Zoom, Big Blue Button, Skype, atau WA, harap masukkan link online secara benar.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}
				if (Common.isMobile()) {
					ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
				} else {
					Clients.evalJavaScript(
							"popupCenter({url: '" + url + "', title: 'Video Conference', w: 1200, h: 600});");

				}
			}
		});

		onlineMenggunakan.setReadonly(true);

		Common.initKeterangan(rows,
				"Jika terdapat wawancara atau kegiatan tatap muka secara online, pilihlah salah satu media online.");

		rowMeetKeterangan = Common.initKeterangan(rows,
				"Untuk tatap muka online menggunakan Google Meet, harap singkronkan dulu ke Google Calendar di bawah ini.");

		rowMeet = new MyFormRow();
		rowMeet.setValign("top");
		rowMeet.setParent(rows);
		rowMeet.appendChild(new Label());
		rowMeet.appendChild(AktifitasPerkuliahanHelper.createCalendarButton(gelombangPendaftaran,
				Common.getCurrentUser(), true, new DataLoader() {

					@Override
					public void loadData(Object value) {

					}
				}));

		rowLinkZoomKeterangan = Common.initKeterangan(rows,
				"Untuk tatap muka online menggunakan Zoom, harap memasukkan link zoom di bawah ini. Contoh link zoom : https://us04web.zoom.us/j/4445712881?pwd=ZnNReHRJYXVRem8zRkc5OFpPd3I3QT09");

		rowLinkZoomLink = new MyFormRow();
		rowLinkZoomLink.setValign("top");
		rowLinkZoomLink.setParent(rows);
		rowLinkZoomLink.appendChild(new ais.ui.util.MyLabelConfig(""));
		A linkZoomSignup;
		rowLinkZoomLink.appendChild(linkZoomSignup = new A(
				"Klik disini dan login untuk mendapatkan link zoom yang baru, https://zoom.us/signin"));
		linkZoomSignup.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String server = "https://zoom.us/signin";

				if (Common.isMobile()) {
					ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
				} else {
					Clients.evalJavaScript(
							"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");

				}
			}
		});

		rowLinkZoom = new MyFormRow();
		rowLinkZoom.setValign("top");
		rowLinkZoom.setParent(rows);
		rowLinkZoom.appendChild(new ais.ui.util.MyLabelConfig("Link Zoom *"));
		rowLinkZoom.appendChild(zoomLink = new Textbox(gelombangPendaftaran.getZoomLink()));
		zoomLink.setWidth("90%");
		zoomLink.setRows(2);

		rowLinkZoomButton = Common.initKeterangan(rows,
				"Secara default, link zoom akan menggunakan link zoom dari gelombangPendaftaran sebelumnya..");

		rowLinkBbbKeterangan = Common.initKeterangan(rows,
				"Untuk tatap muka online menggunakan Big Blue Button, harap memasukkan link Big Blue Button di bawah ini. Contoh link bbb : https://demo.bigbluebutton.org/gl/muh-jjn-72p");

		rowLinkBbbLink = new MyFormRow();
		rowLinkBbbLink.setValign("top");
		rowLinkBbbLink.setParent(rows);
		rowLinkBbbLink.appendChild(new ais.ui.util.MyLabelConfig(""));
		A linkBbbSignup;
		rowLinkBbbLink.appendChild(linkBbbSignup = new A(
				"Klik disini dan login untuk mendapatkan link Big Blue Button yang baru, https://demo.bigbluebutton.org/gl/signin"));

		linkBbbSignup.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String server = "https://demo.bigbluebutton.org/gl/signin";

				if (Common.isMobile()) {
					ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
				} else {
					Clients.evalJavaScript(
							"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");

				}
			}
		});

		rowLinkBbb = new MyFormRow();
		rowLinkBbb.setValign("top");
		rowLinkBbb.setParent(rows);
		rowLinkBbb.appendChild(new ais.ui.util.MyLabelConfig("Link Big Blue Button *"));
		rowLinkBbb.appendChild(bbbLink = new Textbox(gelombangPendaftaran.getBbbLink()));
		bbbLink.setWidth("90%");
		bbbLink.setRows(2);

		rowLinkBbbButton = Common.initKeterangan(rows,
				"Secara default, link Big Blue Button akan menggunakan link Big Blue Button dari gelombangPendaftaran sebelumnya..");

		rowLinkSkypeKeterangan = Common.initKeterangan(rows,
				"Untuk tatap muka online menggunakan Skype, harap memasukkan link Skype di bawah ini. Contoh link skype : https://join.skype.com/Ut2b1onFnJnD");

		rowLinkSkypeLink = new MyFormRow();
		rowLinkSkypeLink.setValign("top");
		rowLinkSkypeLink.setParent(rows);
		rowLinkSkypeLink.appendChild(new ais.ui.util.MyLabelConfig(""));
		A linkSkypeSignup;
		rowLinkSkypeLink.appendChild(linkSkypeSignup = new A(
				"Klik disini dan login untuk mendapatkan link Skype yang baru, https://web.skype.com"));

		linkSkypeSignup.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String server = "https://web.skype.com";

				if (Common.isMobile()) {
					ExecutionsCtrl.getCurrent().sendRedirect(server, "_blank");
				} else {
					Clients.evalJavaScript(
							"popupCenter({url: '" + server + "', title: 'Video Conference', w: 1200, h: 600});");

				}
			}
		});

		rowLinkSkype = new MyFormRow();
		rowLinkSkype.setValign("top");
		rowLinkSkype.setParent(rows);
		rowLinkSkype.appendChild(new ais.ui.util.MyLabelConfig("Link Skype *"));
		rowLinkSkype.appendChild(skypeLink = new Textbox(gelombangPendaftaran.getSkypeLink()));
		skypeLink.setWidth("90%");
		skypeLink.setRows(2);

		rowLinkSkypeButton = Common.initKeterangan(rows,
				"Secara default, link Skype akan menggunakan link Skype dari gelombangPendaftaran sebelumnya..");

		rowLinkWa = new MyFormRow();
		rowLinkWa.setValign("top");
		rowLinkWa.setParent(rows);
		rowLinkWa.appendChild(new ais.ui.util.MyLabelConfig("Link Grup Whatsapp *"));
		rowLinkWa.appendChild(waLink = new Textbox(gelombangPendaftaran.getWaLink()));
		waLink.setWidth("90%");
		waLink.setRows(2);

		rowLinkWaButton = Common.initKeterangan(rows,
				"Secara default, link Grup Whatsapp akan menggunakan link Grup Whatsapp dari gelombangPendaftaran sebelumnya..");

		rowLinkWaKeterangan = Common.initKeterangan(rows,
				"Untuk tatap muka online menggunakan Grup WA, harap memasukkan link WA di atas. Untuk membuat link Grup WA, buka aplikasi WA Grup Anda (harus sebagai admin) atau buat grup WA baru, pilih Grup Info, dan pilih undang via link.. Contoh link : https://chat.whatsapp.com/Djx0r98Z30YTmFmEZGJ3");

		rowLinkLain = new MyFormRow();
		rowLinkLain.setValign("top");
		rowLinkLain.setParent(rows);
		rowLinkLain.appendChild(new ais.ui.util.MyLabelConfig("Link Media Online *"));
		rowLinkLain.appendChild(linkLain = new Textbox(gelombangPendaftaran.getWaLink()));
		linkLain.setWidth("90%");
		linkLain.setRows(2);

		rowLinkLainKeterangan = Common.initKeterangan(rows,
				"Untuk tatap muka online menggunakan media onlien lain, harap memasukkan link media tersebut di bawah ini.");

		EventListener eventListenerOl = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Integer ol = (Integer) onlineMenggunakan.getSelectedItem().getValue();
				rowMeetKeterangan.setVisible(ol.equals(GelombangPendaftaran.GOOGLE_MEET));
				rowMeet.setVisible(ol.equals(GelombangPendaftaran.GOOGLE_MEET));

				rowLinkZoomKeterangan.setVisible(ol.equals(GelombangPendaftaran.ZOOM));
				rowLinkZoom.setVisible(ol.equals(GelombangPendaftaran.ZOOM));
				rowLinkZoomButton.setVisible(ol.equals(GelombangPendaftaran.ZOOM));
				rowLinkZoomLink.setVisible(ol.equals(GelombangPendaftaran.ZOOM));

				rowLinkBbbKeterangan.setVisible(ol.equals(GelombangPendaftaran.BBB));
				rowLinkBbb.setVisible(ol.equals(GelombangPendaftaran.BBB));
				rowLinkBbbButton.setVisible(ol.equals(GelombangPendaftaran.BBB));
				rowLinkBbbLink.setVisible(ol.equals(GelombangPendaftaran.BBB));

				rowLinkSkypeKeterangan.setVisible(ol.equals(GelombangPendaftaran.SKYPE));
				rowLinkSkype.setVisible(ol.equals(GelombangPendaftaran.SKYPE));
				rowLinkSkypeButton.setVisible(ol.equals(GelombangPendaftaran.SKYPE));
				rowLinkSkypeLink.setVisible(ol.equals(GelombangPendaftaran.SKYPE));

				rowLinkWa.setVisible(ol.equals(GelombangPendaftaran.WA));
				rowLinkWaButton.setVisible(ol.equals(GelombangPendaftaran.WA));
				waLink.setVisible(ol.equals(GelombangPendaftaran.WA));
				rowLinkWaKeterangan.setVisible(ol.equals(GelombangPendaftaran.WA));

				rowLinkLain.setVisible(ol.equals(GelombangPendaftaran.LAIN));
				linkLain.setVisible(ol.equals(GelombangPendaftaran.LAIN));
				rowLinkLainKeterangan.setVisible(ol.equals(GelombangPendaftaran.LAIN));

				testButton.setVisible(true);
				if (ol.equals(GelombangPendaftaran.GOOGLE_MEET)) {
					testButton.setImage("/img/meet-google.png");
				} else if (ol.equals(GelombangPendaftaran.JITSI)) {
					testButton.setImage("/img/jitsi.png");
				} else if (ol.equals(GelombangPendaftaran.ZOOM)) {
					testButton.setImage("/img/zoom.png");
				} else if (ol.equals(GelombangPendaftaran.BBB)) {
					testButton.setImage("/img/bbb.png");
				} else if (ol.equals(GelombangPendaftaran.SKYPE)) {
					testButton.setImage("/img/Skype-icon.png");
				} else if (ol.equals(GelombangPendaftaran.WA)) {
					testButton.setImage("/img/svg/whats.svg");
				} else if (ol.equals(GelombangPendaftaran.LAIN)) {
					testButton.setImage("/img/online-red-icon.png");
				} else {
					testButton.setVisible(false);
				}

			}
		};

		onlineMenggunakan.addEventListener("onChange", eventListenerOl);
		eventListenerOl.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa pendaftaran (*)"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(mulai = new MyDatebox(gelombangPendaftaran.getMulai()));
		hbox.appendChild(sampai = new MyDatebox(gelombangPendaftaran.getSampai()));

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilkanMasaPendaftaranKeCalonMahasiswa = new MyCheckboxConfig(
				"Masa pendaftaran tampil di menu pilihan gelombang"));
		tampilkanMasaPendaftaranKeCalonMahasiswa
				.setChecked(gelombangPendaftaran.getTampilkanMasaPendaftaranKeCalonMahasiswa());
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilkanUploadFoto = new MyCheckboxConfig("Tampilkan upload foto calon mahasiswa"));
		tampilkanUploadFoto.setChecked(gelombangPendaftaran.getTampilkanUploadFoto());
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(fotoWajibDiuplad = new MyCheckboxConfig("Foto calon mahasiswa wajib diupload"));
		fotoWajibDiuplad.setChecked(gelombangPendaftaran.getFotoWajibDiuplad());
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(terdapatUjianOnline = new MyCheckboxConfig("Terdapat ujian online pada gelombang ini"));
		terdapatUjianOnline.setChecked(gelombangPendaftaran.getTerdapatUjianOnline());
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(ujianOnlineOtomatisDiterima = new MyCheckboxConfig(
				"Ujian online menentukan otomatis dinyatakan diterima / lulus pada pilihan prodi utama"));
		ujianOnlineOtomatisDiterima.setChecked(gelombangPendaftaran.getUjianOnlineOtomatisDiterima());
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai minimal diterima setelah ikut ujian online"));
		row.appendChild(nilaiMinimalUjianOnlineOtomatisDiterima = new MyDoublebox(
				gelombangPendaftaran.getNilaiMinimalUjianOnlineOtomatisDiterima()));
		row.setParent(rows);

		EventListener ujianOnlineOtomatisDiterimaEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				nilaiMinimalUjianOnlineOtomatisDiterima.getParent().setVisible(ujianOnlineOtomatisDiterima.isChecked());
			}
		};

		ujianOnlineOtomatisDiterima.addEventListener("onClick", ujianOnlineOtomatisDiterimaEventListener);
		ujianOnlineOtomatisDiterimaEventListener.onEvent(null);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(otomatisDiterimaSaatDaftar = new MyCheckboxConfig(
				"Saat pertama kali daftar, calon mahasiswa otomatis dinyatakan diterima / lulus pada pilihan prodi utama"));
		otomatisDiterimaSaatDaftar.setChecked(gelombangPendaftaran.getOtomatisDiterimaSaatDaftar());
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(terdapatInterview = new MyCheckboxConfig("Terdapat interview pada gelombang ini"));
		terdapatInterview.setChecked(gelombangPendaftaran.getTerdapatInterview());
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilkanQrCodeMahasiswaSetelahDapatNim = new MyCheckboxConfig(
				"Tampilkan QR-Code Mahasiswa Setelah mendapat NIM"));
		tampilkanQrCodeMahasiswaSetelahDapatNim
				.setChecked(gelombangPendaftaran.getTampilkanQrCodeMahasiswaSetelahDapatNim());
		row.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Info yang ditampilkan ke calon mahasiswa"));
		row.appendChild(info = new Textbox(gelombangPendaftaran.getInfo()));
		info.setWidth("90%");
		info.setRows(2);

		lainMahasiswa = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File / lampiran informasi yang ditampilkan ke calon mahasiswa"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, gelombangPendaftaran.getId(), "INFO_PMB", "Informasi PMB",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows,
				"Jika file lampiran informasi yang ditampilkan ke calon mahasiswa lebih dari satu file, zip dulu semua file tersebut");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(otomatisLoginSetelahDaftar = new MyCheckboxConfig(
				"Saat pertama kali daftar, otomatis login setelah calon mahasiswa menyelesaikan pendaftaran"));
		otomatisLoginSetelahDaftar.setChecked(gelombangPendaftaran.getOtomatisLoginSetelahDaftar());

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				tampilFormTambahanSaatRegistrasi = new MyCheckboxConfig("Tampil form tambahan saat registrasi"));
		tampilFormTambahanSaatRegistrasi.setChecked(gelombangPendaftaran.getTampilFormTambahanSaatRegistrasi());

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilFormTambahanSaatLoginCalonMhs = new MyCheckboxConfig(
				"Tampil form tambahan saat login calon mhs"));
		tampilFormTambahanSaatLoginCalonMhs.setChecked(gelombangPendaftaran.getTampilFormTambahanSaatLoginCalonMhs());

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Login Calon Mahasiswa"));
		row.appendChild(tanggalLoginCalonMahasiswaBerakhir = new MyDatebox(
				gelombangPendaftaran.getTanggalLoginCalonMahasiswaBerakhir()));
		tanggalLoginCalonMahasiswaBerakhir.setWidth("90%");

		if (!tampilSederhana) {
			Common.initKeterangan(rows,
					"Kosongkan \"Tanggal Login Calon Mahasiswa Berakhir\" jika tidak ada tanggal berakhir");
		} else {
			addWindow.setHeight("300px");
		}

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Upload Berkas Berakhir *"));
		row.appendChild(
				tanggalDaftarUlangBerakhir = new MyDatebox(gelombangPendaftaran.getTanggalDaftarUlangBerakhir()));
		tanggalDaftarUlangBerakhir.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan Pembayaran Pendaftaran"));
		row.appendChild(tanggalTagihanRegistrasi = new MyDatebox(gelombangPendaftaran.getTanggalTagihanRegistrasi()));
		tanggalTagihanRegistrasi.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Tagihan Pembayaran Daftar Ulang"));
		row.appendChild(tanggalTagihanDaftarUlang = new MyDatebox(gelombangPendaftaran.getTanggalTagihanDaftarUlang()));
		tanggalTagihanDaftarUlang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(harusSebagaiAlumni = new MyCheckboxConfig("Calon mahasiswa harus sebagai alumni"));
		harusSebagaiAlumni.setChecked(gelombangPendaftaran.getHarusSebagaiAlumni());

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(dibatasiUmur = new MyCheckboxConfig("Dibatasi Umur"));
		if (gelombangPendaftaran.getDibatasiUmur() == null) {
			gelombangPendaftaran
					.setDibatasiUmur(Common.bolehKonfigurasi("umur_calon_mahasiswa_dibatasi", Konfigurasi.TIDAK_AKTIF));
		}
		dibatasiUmur.setChecked(gelombangPendaftaran.getDibatasiUmur());

		int umur = 27;
		try {
			umur = Integer
					.parseInt(Common.getKonfigurasi("nilai_umur_calon_mahasiswa_dibatasi", "27").getNilai().trim());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		if (gelombangPendaftaran.getUmurmaksimal() == null) {
			gelombangPendaftaran.setUmurmaksimal(umur);
		}

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Umur Minimal"));
		row.appendChild(umurminimal = new Intbox(gelombangPendaftaran.getUmurminimal()));
		umurminimal.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Umur Maksimal"));
		row.appendChild(umurmaksimal = new Intbox(gelombangPendaftaran.getUmurmaksimal()));
		umurmaksimal.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Kelulusan Minimal"));
		row.appendChild(tahunAngkatanMinimal = new Intbox(gelombangPendaftaran.getTahunAngkatanMinimal()));
		tahunAngkatanMinimal.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Kelulusan Maksimal"));
		row.appendChild(tahunAngkatanMaksimal = new Intbox(gelombangPendaftaran.getTahunAngkatanMaksimal()));
		tahunAngkatanMaksimal.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(harusBayarSebelumBisaLogin = new MyCheckboxConfig(
				"Calon mahasiswa harus mem-bayar sebelum bisa melengkapi biodata dan berkas"));
		harusBayarSebelumBisaLogin.setChecked(gelombangPendaftaran.getHarusBayarSebelumBisaLogin());
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(dokumenHarusDiverivikasiSebelumBisaSimpan = new MyCheckboxConfig(
				"Kelengkapan biodata harus dilakukan bisa \"Simpan Biodata\""));
		dokumenHarusDiverivikasiSebelumBisaSimpan
				.setChecked(gelombangPendaftaran.getDokumenHarusDiverivikasiSebelumBisaSimpan());
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(dokumenHarusDiverivikasiSebelumBisaCetakKartuUjian = new MyCheckboxConfig(
				"Kelengkapan biodata dan berkas harus diupload atau diverifikasi sebelum bisa \"Cetak Kartu Ujian\""));
		dokumenHarusDiverivikasiSebelumBisaCetakKartuUjian
				.setChecked(gelombangPendaftaran.getDokumenHarusDiverivikasiSebelumBisaCetakKartuUjian());
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(dokumenHarusDiverivikasiSebelumBisaIkutUjian = new MyCheckboxConfig(
				"Kelengkapan biodata dan berkas harus diupload atau diverifikasi sebelum bisa \"Akses materi / ikut ujian online\""));
		dokumenHarusDiverivikasiSebelumBisaIkutUjian
				.setChecked(gelombangPendaftaran.getDokumenHarusDiverivikasiSebelumBisaIkutUjian());
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				hanyapaket = new MyCheckboxConfig("Gelombang pendaftaran ini hanya berlaku untuk paket tertentu"));
		row.setParent(rows);

		selectedPaketPunyaGelombangPendaftaran = new HashMap<Long, PaketPunyaGelombangPendaftaran>();

		rowJp = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowJp, "2");
		rowJp.setParent(rows);

		EventListener ubahJenisPenialain = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

				List<Paket> pakets = ConstantValues.simpleList(
						session.createCriteria(Paket.class)
								.add(Restrictions.or(Restrictions.isNull("perguruanTinggi"),
										Restrictions.eq("perguruanTinggi", perguruanTinggi)))
								.addOrder(Order.asc("nama"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						Paket.class);

				if (gelombangPendaftaran.getCopyDari() != null && gelombangPendaftaran.getCopyDari().getId() != null) {

					GelombangPendaftaran gelombangPendaftaranCopy = (GelombangPendaftaran) gelombangPendaftaran
							.getCopyDari();
					session.refresh(gelombangPendaftaranCopy);

					List<PaketPunyaGelombangPendaftaran> paketPunyaGelombangPendaftarans = ConstantValues.simpleList(
							session.createCriteria(PaketPunyaGelombangPendaftaran.class)
									.add(Restrictions.eq("gelombangPendaftaran", gelombangPendaftaranCopy)),
							PaketPunyaGelombangPendaftaran.class);

					selectedPaketPunyaGelombangPendaftaran.clear();
					for (PaketPunyaGelombangPendaftaran paketPunyaGelombangPendaftaran : paketPunyaGelombangPendaftarans) {
						if (!selectedPaketPunyaGelombangPendaftaran
								.containsKey(paketPunyaGelombangPendaftaran.getPaket().getId())) {
							selectedPaketPunyaGelombangPendaftaran.put(
									paketPunyaGelombangPendaftaran.getPaket().getId(), paketPunyaGelombangPendaftaran);
						}
					}

				} else {

					if (gelombangPendaftaran.getId() != null) {
						session.refresh(gelombangPendaftaran);
					}

					if (gelombangPendaftaran.getId() != null) {

						List<PaketPunyaGelombangPendaftaran> paketPunyaGelombangPendaftarans = ConstantValues
								.simpleList(
										session.createCriteria(PaketPunyaGelombangPendaftaran.class)
												.add(Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran)),
										PaketPunyaGelombangPendaftaran.class);

						selectedPaketPunyaGelombangPendaftaran.clear();
						for (PaketPunyaGelombangPendaftaran paketPunyaGelombangPendaftaran : paketPunyaGelombangPendaftarans) {
							if (!selectedPaketPunyaGelombangPendaftaran
									.containsKey(paketPunyaGelombangPendaftaran.getPaket().getId())) {
								selectedPaketPunyaGelombangPendaftaran.put(
										paketPunyaGelombangPendaftaran.getPaket().getId(),
										paketPunyaGelombangPendaftaran);
							}
						}

					} else {
						selectedPaketPunyaGelombangPendaftaran.clear();
					}

				}

				rowJp.setVisible(hanyapaket.isChecked());
				Common.clear(rowJp);
				MyGrid vboxSkala = new MyGrid();
				vboxSkala.setParent(rowJp);

				Columns columns = new Columns();
				columns.setParent(vboxSkala);

				MyColumnConfig column = new MyColumnConfig("Pilih Paket");
				column.setParent(columns);

				Rows rowsSkala = new Rows();
				rowsSkala.setParent(vboxSkala);

				KategoriItemPenilaianSiswa kategoriItemPenilaianSiswa = new KategoriItemPenilaianSiswa();
				kategoriItemPenilaianSiswa.setId(-1L);

				for (final Paket paket : pakets) {

					MyFormRow rowSkala = new MyFormRow();
					rowSkala.setStyle("border:0px;background: transparent;");
					rowSkala.setParent(rowsSkala);

					PaketPunyaGelombangPendaftaran paketPunyaGelombangPendaftaranTemp = selectedPaketPunyaGelombangPendaftaran
							.get(paket.getId());
					if (paketPunyaGelombangPendaftaranTemp == null) {
						paketPunyaGelombangPendaftaranTemp = new PaketPunyaGelombangPendaftaran();
					}
					paketPunyaGelombangPendaftaranTemp.setPaket(paket);
					final PaketPunyaGelombangPendaftaran paketPunyaGelombangPendaftaran = paketPunyaGelombangPendaftaranTemp;

					final Checkbox checkbox = new Checkbox(paket.getNama());
					checkbox.setAttribute("paket", paket);
					checkbox.setParent(rowSkala);
					checkbox.setChecked(selectedPaketPunyaGelombangPendaftaran.containsKey(paket.getId()));
					checkbox.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							if (checkbox.isChecked()) {
								selectedPaketPunyaGelombangPendaftaran.put(paket.getId(),
										paketPunyaGelombangPendaftaran);
							} else {
								selectedPaketPunyaGelombangPendaftaran.remove(paket.getId());
							}

						}
					});

				}
			}
		};

		hanyapaket.addEventListener("onClick", ubahJenisPenialain);

		ubahJenisPenialain.onEvent(null);
		hanyapaket.setChecked(!selectedPaketPunyaGelombangPendaftaran.isEmpty());
		rowJp.setVisible(hanyapaket.isChecked());

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		final MyCheckboxConfig formulirVerifikasi;
		row.appendChild(formulirVerifikasi = new MyCheckboxConfig("Formulir Verifikasi Kelengkapan Berkas"));
		row.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		subColumns = new Columns();
		subColumns.setParent(subGrid);
		c = new Column("Formulir Verifikasi Kelengkapan Berkas");
		subColumns.appendChild(c);

		subRows = new Rows();
		subRows.setParent(subGrid);

		subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		@SuppressWarnings("unchecked")
		List<VerifikasiKelengkapanCalonMahasiswa> verifikasiKelengkapanCalonMahasiswas = ConstantValues.simpleList(
				session.createCriteria(VerifikasiKelengkapanCalonMahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				VerifikasiKelengkapanCalonMahasiswa.class);

		if (gelombangPendaftaran.getCopyDari() != null && gelombangPendaftaran.getCopyDari().getId() != null) {

			GelombangPendaftaran gelombangPendaftaranCopy = (GelombangPendaftaran) gelombangPendaftaran.getCopyDari();
			session.refresh(gelombangPendaftaranCopy);
			selectedVerifikasiKelengkapanCalonMahasiswa = new HashSet<VerifikasiKelengkapanCalonMahasiswa>();
			for (VerifikasiKelengkapanCalonMahasiswa kelengkapanCalonMahasiswa : gelombangPendaftaranCopy
					.getVerifikasiKelengkapanCalonMahasiswas()) {
				selectedVerifikasiKelengkapanCalonMahasiswa.add(kelengkapanCalonMahasiswa);
			}

		} else {
			if (gelombangPendaftaran.getId() != null) {
				session.refresh(this.gelombangPendaftaran);
			}
			selectedVerifikasiKelengkapanCalonMahasiswa = new HashSet<VerifikasiKelengkapanCalonMahasiswa>();
			if (this.gelombangPendaftaran != null
					&& this.gelombangPendaftaran.getVerifikasiKelengkapanCalonMahasiswas() != null) {
				selectedVerifikasiKelengkapanCalonMahasiswa
						.addAll(this.gelombangPendaftaran.getVerifikasiKelengkapanCalonMahasiswas());
			}

		}

		ids = new HashSet<Long>();
		for (VerifikasiKelengkapanCalonMahasiswa v : selectedVerifikasiKelengkapanCalonMahasiswa) {
			ids.add(v.getId());
		}

		System.out.println("ids ->" + ids);

		subGrid.setVisible(!selectedVerifikasiKelengkapanCalonMahasiswa.isEmpty());
		formulirVerifikasi.setChecked(!selectedVerifikasiKelengkapanCalonMahasiswa.isEmpty());

		formulirVerifikasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				subGrid.setVisible(formulirVerifikasi.isChecked());
			}
		});

		vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa : verifikasiKelengkapanCalonMahasiswas) {
			final Checkbox checkbox = new Checkbox(verifikasiKelengkapanCalonMahasiswa.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(ids.contains(verifikasiKelengkapanCalonMahasiswa.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedVerifikasiKelengkapanCalonMahasiswa.add(verifikasiKelengkapanCalonMahasiswa);
					} else {
						for (VerifikasiKelengkapanCalonMahasiswa a : selectedVerifikasiKelengkapanCalonMahasiswa) {
							if (a.getId().equals(verifikasiKelengkapanCalonMahasiswa.getId())) {
								selectedVerifikasiKelengkapanCalonMahasiswa.remove(a);
								break;
							}
						}
					}

					System.out.println("selectedVerifikasiKelengkapanCalonMahasiswa => "
							+ selectedVerifikasiKelengkapanCalonMahasiswa);
				}
			});
		}

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		final MyCheckboxConfig parameterTambahan;
		row.appendChild(parameterTambahan = new MyCheckboxConfig("Terdapat Pilihan Kelompok Parameter Tambahan"));
		row.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid mysubGrid = new MyGrid();
		row.appendChild(mysubGrid);

		subColumns = new Columns();
		subColumns.setParent(mysubGrid);
		c = new Column("Kelompok Parameter Tambahan");
		subColumns.appendChild(c);

		subRows = new Rows();
		subRows.setParent(mysubGrid);

		subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		@SuppressWarnings("unchecked")
		List<KelompokParameterTambahanCalonMahasiswa> kelompokParameterTambahanCalonMahasiswas = session
				.createCriteria(KelompokParameterTambahanCalonMahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		if (gelombangPendaftaran.getCopyDari() != null && gelombangPendaftaran.getCopyDari().getId() != null) {
			GelombangPendaftaran gelombangPendaftaranCopy = (GelombangPendaftaran) gelombangPendaftaran.getCopyDari();
			session.refresh(gelombangPendaftaranCopy);

			selectedKelompokParameterTambahanCalonMahasiswa = new HashSet<KelompokParameterTambahanCalonMahasiswa>();
			for (KelompokParameterTambahanCalonMahasiswa tambahanCalonMahasiswa : gelombangPendaftaranCopy
					.getKelompokParameterTambahanCalonMahasiswas()) {
				selectedKelompokParameterTambahanCalonMahasiswa.add(tambahanCalonMahasiswa);
			}

		} else {
			if (gelombangPendaftaran.getId() != null) {
				session.refresh(this.gelombangPendaftaran);
			}
			selectedKelompokParameterTambahanCalonMahasiswa = new HashSet<KelompokParameterTambahanCalonMahasiswa>();
			if (this.gelombangPendaftaran != null
					&& this.gelombangPendaftaran.getKelompokParameterTambahanCalonMahasiswas() != null) {
				selectedKelompokParameterTambahanCalonMahasiswa
						.addAll(this.gelombangPendaftaran.getKelompokParameterTambahanCalonMahasiswas());
			}
		}

		mysubGrid.setVisible(!selectedKelompokParameterTambahanCalonMahasiswa.isEmpty());
		parameterTambahan.setChecked(!selectedKelompokParameterTambahanCalonMahasiswa.isEmpty());

		parameterTambahan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				mysubGrid.setVisible(parameterTambahan.isChecked());
			}
		});

		Vbox myvboxSkala = new Vbox();
		myvboxSkala.setPack("top");
		myvboxSkala.setParent(subRow);
		for (final KelompokParameterTambahanCalonMahasiswa kelompokParameterTambahanCalonMahasiswa : kelompokParameterTambahanCalonMahasiswas) {
			final Checkbox checkbox = new Checkbox(kelompokParameterTambahanCalonMahasiswa.getNama());
			checkbox.setParent(myvboxSkala);
			checkbox.setChecked(
					selectedKelompokParameterTambahanCalonMahasiswa.contains(kelompokParameterTambahanCalonMahasiswa));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedKelompokParameterTambahanCalonMahasiswa.add(kelompokParameterTambahanCalonMahasiswa);
					} else {
						for (KelompokParameterTambahanCalonMahasiswa a : selectedKelompokParameterTambahanCalonMahasiswa) {
							if (a.getId().equals(kelompokParameterTambahanCalonMahasiswa.getId())) {
								selectedKelompokParameterTambahanCalonMahasiswa.remove(a);
								break;
							}
						}
					}

					System.out.println("selectedKelompokParameterTambahanCalonMahasiswa => "
							+ selectedKelompokParameterTambahanCalonMahasiswa);
				}
			});
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Info yang ditampilkan saat ikut ujian online"));
		row.appendChild(infoSetelahUjianOnline = new MyCkEditor());
		infoSetelahUjianOnline.setWidth("90%");
		infoSetelahUjianOnline.setHeight("120px");
		infoSetelahUjianOnline.setValue(gelombangPendaftaran.getInfoSetelahUjianOnline());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Info yang ditampilkan saat interview"));
		row.appendChild(infoSaatInterview = new MyCkEditor());
		infoSaatInterview.setWidth("90%");
		infoSaatInterview.setHeight("120px");
		infoSaatInterview.setValue(gelombangPendaftaran.getInfoSaatInterview());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				gelombangPendaftaran.getKeterangan() == null ? "" : gelombangPendaftaran.getKeterangan()));
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
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Gelombang Pendaftaran",
					"Kolom Nama Gelombang Pendaftaran belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Gelombang Pendaftaran.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tahunAkademik.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun akademik",
					"Kolom Tahun akademik belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tahun akademik.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			tahunAkademik.focus();
			return false;
		}
		// if (jenisSeleksi.getSelectedItem() == null) {
		// MyMessageboxConfig.show("Jenis Seleksi harus diisi", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// tahunAkademik.focus();
		// return false;
		// }

		if (mulai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal mulai",
					"Kolom Tanggal mulai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tanggal mulai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			mulai.focus();
			return false;
		}
		if (sampai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal sampai",
					"Kolom Tanggal sampai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tanggal sampai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			sampai.focus();
			return false;
		}
		if (tanggalDaftarUlangBerakhir.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal Daftar Ulang Berakhir",
					"Kolom Tanggal Daftar Ulang Berakhir belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tanggal Daftar Ulang Berakhir.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			tanggalDaftarUlangBerakhir.focus();
			return false;
		}
		Session sessionData = HibernateUtil.currentSession();
		if (gelombangPendaftaran.getId() != null) {
			gelombangPendaftaran = (GelombangPendaftaran) sessionData.load(GelombangPendaftaran.class,
					gelombangPendaftaran.getId());

		}

		gelombangPendaftaran
				.setTampilkanQrCodeMahasiswaSetelahDapatNim(tampilkanQrCodeMahasiswaSetelahDapatNim.isChecked());
		gelombangPendaftaran.setOtomatisDiterimaSaatDaftar(otomatisDiterimaSaatDaftar.isChecked());
		gelombangPendaftaran
				.setProgram((String) (program.getSelectedItem() == null ? null : program.getSelectedItem().getValue()));
		gelombangPendaftaran.setTidakBolehMemilihProgramLain(tidakBolehMemilihProgramLain.isChecked());
		gelombangPendaftaran.setMahasiswaPindahanBolehMendaftar(mahasiswaPindahanBolehMendaftar.isChecked());
		gelombangPendaftaran.setTanggalDaftarUlangBerakhir(tanggalDaftarUlangBerakhir.getValue());
		if (selectedVerifikasiKelengkapanCalonMahasiswa == null) {
			selectedVerifikasiKelengkapanCalonMahasiswa = new HashSet<VerifikasiKelengkapanCalonMahasiswa>();
		}
		gelombangPendaftaran.setVerifikasiKelengkapanCalonMahasiswas(selectedVerifikasiKelengkapanCalonMahasiswa);

		String jenisS = "";
		for (JenisSeleksi jenisSeleksi : this.selectedJenisSeleksi) {
			jenisS += jenisS.isEmpty() ? jenisSeleksi.getId().toString() : "," + jenisSeleksi.getId();
		}
		gelombangPendaftaran.setJenisSeleksiLain(jenisS);

		if (selectedKelompokParameterTambahanCalonMahasiswa == null) {
			selectedKelompokParameterTambahanCalonMahasiswa = new HashSet<KelompokParameterTambahanCalonMahasiswa>();
		}
		gelombangPendaftaran
				.setKelompokParameterTambahanCalonMahasiswas(selectedKelompokParameterTambahanCalonMahasiswa);
		gelombangPendaftaran.setStatusAwalMahasiswaDefault(
				(StatusAwalMahasiswa) (statusAwalMahasiswa.getSelectedItem() == null ? null
						: statusAwalMahasiswa.getSelectedItem().getValue()));
		gelombangPendaftaran.setTahunAngkatanMaksimal(tahunAngkatanMaksimal.getValue());
		gelombangPendaftaran.setTahunAngkatanMinimal(tahunAngkatanMinimal.getValue());
		gelombangPendaftaran.setUmurmaksimal(umurmaksimal.getValue());
		gelombangPendaftaran.setDibatasiUmur(dibatasiUmur.isChecked());
		gelombangPendaftaran.setJenjang((Jenjang) jenjang.getSelectedItem().getValue());
		gelombangPendaftaran.setKode(kode.getValue().trim());
		gelombangPendaftaran.setBisaDipilihPendaftarOnline(bisaDipilihPendaftarOnline.isChecked());
		gelombangPendaftaran.setJenisSeleksi((JenisSeleksi) (jenisSeleksi.getSelectedItem() == null ? null
				: jenisSeleksi.getSelectedItem().getValue()));
		gelombangPendaftaran.setJenisDiskonMahasiswa((JenisDiskonMahasiswa) (jenisDiskonMahasiswa.getSelectedItem() == null ? null
				: jenisDiskonMahasiswa.getSelectedItem().getValue()));
		gelombangPendaftaran.setNama(nama.getValue());
		gelombangPendaftaran.setKeterangan(keterangan.getValue());
		gelombangPendaftaran.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		gelombangPendaftaran.setMulai(mulai.getValue());
		gelombangPendaftaran.setSampai(sampai.getValue());
		gelombangPendaftaran.setJenisSemester((String) (jenisSemester.getSelectedItem() == null ? Perkuliahan.GANJIL
				: jenisSemester.getSelectedItem().getValue()));
		gelombangPendaftaran.setUmurminimal(umurminimal.getValue());

		gelombangPendaftaran.setTampilFormTambahanSaatLoginCalonMhs(tampilFormTambahanSaatLoginCalonMhs.isChecked());
		gelombangPendaftaran.setTampilFormTambahanSaatRegistrasi(tampilFormTambahanSaatRegistrasi.isChecked());
		gelombangPendaftaran.setTanggalLoginCalonMahasiswaBerakhir(tanggalLoginCalonMahasiswaBerakhir.getValue());
		gelombangPendaftaran.setPerguruanTinggi(selectedPerguruanTinggi);
		gelombangPendaftaran.setHarusIkutStatusAwalDefault(harusIkutStatusAwalDefault.isChecked());
		gelombangPendaftaran.setDokumenHarusDiverivikasiSebelumBisaCetakKartuUjian(
				dokumenHarusDiverivikasiSebelumBisaCetakKartuUjian.isChecked());
		gelombangPendaftaran.setDokumenHarusDiverivikasiSebelumBisaIkutUjian(
				dokumenHarusDiverivikasiSebelumBisaIkutUjian.isChecked());
		gelombangPendaftaran
				.setDokumenHarusDiverivikasiSebelumBisaSimpan(dokumenHarusDiverivikasiSebelumBisaSimpan.isChecked());
		gelombangPendaftaran.setHarusBayarSebelumBisaLogin(harusBayarSebelumBisaLogin.isChecked());

		gelombangPendaftaran.setOnlineMenggunakan(
				(Integer) (onlineMenggunakan == null || onlineMenggunakan.getValue() == null ? null
						: onlineMenggunakan.getSelectedItem().getValue()));
		gelombangPendaftaran.setZoomLink(zoomLink.getValue().trim());
		gelombangPendaftaran.setBbbLink(bbbLink.getValue().trim());
		gelombangPendaftaran.setSkypeLink(skypeLink.getValue().trim());
		gelombangPendaftaran.setWaLink(waLink.getValue().trim());
		gelombangPendaftaran.setLainLink(linkLain.getValue().trim());

		gelombangPendaftaran.setTidakBolehMendaftarMhsYgSama(tidakBolehMendaftarMhsYgSama.isChecked());

		gelombangPendaftaran.setTidakBolehNikSama(tidakBolehNikSama.isChecked());

		gelombangPendaftaran.setJenisSeleksiDipilihDiFormPendaftaran(jenisSeleksiDipilihDiFormPendaftaran.isChecked());

		gelombangPendaftaran
				.setTampilkanMasaPendaftaranKeCalonMahasiswa(tampilkanMasaPendaftaranKeCalonMahasiswa.isChecked());

		gelombangPendaftaran.setInfo(info.getValue().trim());
		gelombangPendaftaran.setInfoSetelahUjianOnline(infoSetelahUjianOnline.getValue().trim());
		gelombangPendaftaran.setInfoSaatInterview(infoSaatInterview.getValue().trim());

		gelombangPendaftaran.setTerdapatInterview(terdapatInterview.isChecked());
		gelombangPendaftaran.setTerdapatUjianOnline(terdapatUjianOnline.isChecked());
		gelombangPendaftaran.setFotoWajibDiuplad(fotoWajibDiuplad.isChecked());
		gelombangPendaftaran.setTampilkanUploadFoto(tampilkanUploadFoto.isChecked());

		gelombangPendaftaran.setTanggalTagihanDaftarUlang(tanggalTagihanDaftarUlang.getValue());
		gelombangPendaftaran.setTanggalTagihanRegistrasi(tanggalTagihanRegistrasi.getValue());

		gelombangPendaftaran.setOtomatisLoginSetelahDaftar(otomatisLoginSetelahDaftar.isChecked());
		gelombangPendaftaran.setHarusSebagaiAlumni(harusSebagaiAlumni.isChecked());

		gelombangPendaftaran
				.setNilaiMinimalUjianOnlineOtomatisDiterima(nilaiMinimalUjianOnlineOtomatisDiterima.getValue());
		gelombangPendaftaran.setUjianOnlineOtomatisDiterima(ujianOnlineOtomatisDiterima.isChecked());

		gelombangPendaftaran
				.setKelompokGelombang((KelompokGelombang) (kelompokGelombang.getSelectedItem() == null ? null
						: kelompokGelombang.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(sessionData, gelombangPendaftaran);

//		if (gelombangPendaftaran.getCopyDari() != null && gelombangPendaftaran.getCopyDari().getId() != null) {
//			GelombangPendaftaran gelombangPendaftaranData = (GelombangPendaftaran) gelombangPendaftaran.getCopyDari();
//			sessionData.refresh(gelombangPendaftaranData);
//
////			List<JenisSeleksi> jenisSeleksis
//
//			for (Menu menu : menus) {
//				RolePrivilage rolePrivilage = (RolePrivilage) ConstantValues.simpleObject(
//						session.createCriteria(RolePrivilage.class).add(Restrictions.eq("role", tbmrole.getCopyDari()))
//								.add(Restrictions.eq("menu", menu)).setMaxResults(1),
//						RolePrivilage.class);
//
//				if (rolePrivilage != null) {
//					RolePrivilage rolePrivilageBaru = new RolePrivilage();
//
//					try {
//						BeanUtilsBean.getInstance().copyProperties(rolePrivilageBaru, rolePrivilage);
//					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/GelombangPendaftaranAction.java:1880");
//						ais.common.Common.tampilErrorJikaAdmin(e);
//					}
//
//					rolePrivilageBaru.setId(null);
//					rolePrivilageBaru.setMenu(menu);
//					rolePrivilageBaru.setRole(tbmrole);
//					session.save(rolePrivilageBaru);
//					session.flush();
//				}
//			}
//		}

		try {

			sessionData.createSQLQuery("delete from paket_punya_gelombang_pendaftaran where gelombang_pendaftaran="
					+ gelombangPendaftaran.getId()).executeUpdate();

			if (selectedPaketPunyaGelombangPendaftaran != null) {
				for (PaketPunyaGelombangPendaftaran paketPunyaGelombangPendaftaran : selectedPaketPunyaGelombangPendaftaran
						.values()) {
					paketPunyaGelombangPendaftaran.setGelombangPendaftaran(gelombangPendaftaran);
					sessionData.save(paketPunyaGelombangPendaftaran);
					sessionData.flush();
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (kop != null && kop.getId() != null) {
			try {
				Session session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(kop);
				kop.setRef(gelombangPendaftaran.getId());

				session.getTransaction().begin();
				session.update(kop);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (bg_ppdb != null && bg_ppdb.getId() != null) {
			try {
				Session session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(bg_ppdb);
				bg_ppdb.setRef(gelombangPendaftaran.getId());

				session.getTransaction().begin();
				session.update(bg_ppdb);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (icon != null && icon.getId() != null) {
			try {
				Session session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(icon);
				icon.setRef(gelombangPendaftaran.getId());

				session.getTransaction().begin();
				session.update(icon);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				Session session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(gelombangPendaftaran.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(GelombangPendaftaran.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
								Restrictions.isNull("perguruanTinggi")))
				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<GelombangPendaftaran> gelombangPendaftarans = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		TreeMap<Long, GelombangPendaftaran> a = new TreeMap<Long, GelombangPendaftaran>();
		for (GelombangPendaftaran gelombangPendaftaran : gelombangPendaftarans) {
			a.put(gelombangPendaftaran.getId(), gelombangPendaftaran);
		}

		ListModel strset = new SimpleListModel(a.values().toArray(new GelombangPendaftaran[] {}));
		grid.setRowRenderer(new GelombangPendaftaranRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		gelombangPendaftaran = (GelombangPendaftaran) obj;
		init(gelombangPendaftaran);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
