package ais.action.master.recruitment;

import java.util.List;
import java.util.Set;

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
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.recruitment.GelombangPendaftaranPegawai;
import ais.database.model.recruitment.VerifikasiKelengkapanCalonPegawai;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk gelombang pendaftaran pegawai. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox nama}, {@code Textbox keterangan},
 * {@code boolean edit}, {@code boolean delete}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code init()}, {@code initKelengkapanBerkas()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onVerifikasiKelengkapanBerkas()}, {@code onVerifikasiTambahan()}, {@code onKonfigurasiCalonBiodataPegawai()},
 * {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class GelombangPendaftaranPegawaiAction extends GenericAutowireComposer
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

	private GelombangPendaftaranPegawai gelombangPendaftaranPegawai;
	private MyToolbarbuttonConfig add;
	private Textbox informasi;
	private MyDatebox mulai;
	private MyDatebox sampai;

	private Tabpanel verifikasi;

	public void onVerifikasiKelengkapanBerkas(Event event) {
		if (verifikasi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(verifikasi);
			MyInclude iframe = new MyInclude("/pages/master/recruitment/verifikasi_kelengkapan_calon_pegawai.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel verifikasiTambahan;

	public void onVerifikasiTambahan(Event event) {
		if (verifikasiTambahan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(verifikasiTambahan);
			MyInclude iframe = new MyInclude("/pages/master/recruitment/parameter_verifikasi_calon_pegawai.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel konfigurasiCalonBiodataPegawai;

	public void onKonfigurasiCalonBiodataPegawai(Event event) {
		if (konfigurasiCalonBiodataPegawai.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(konfigurasiCalonBiodataPegawai);
			MyInclude iframe = new MyInclude("/pages/master/konfigurasi_biodata_calon_pegawai.zul");
			iframe.setParent(window);
		}
	}

	private Set<VerifikasiKelengkapanCalonPegawai> selectedVerifikasiKelengkapanCalonPegawai;
	private Combobox jenis;
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	private MyTextbox fungsiKerja;
//	private MyTextbox jenjangKarir;
	private MyTextbox jurusan;
	private MyTextbox lulusan;
	private MyTextbox persyaratan;
	private MyTextbox tanggungJawab;
	private MyTextbox disclaimer;
	private MyTextbox fasilitas;
	private MyTextbox pengalaman;

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

		String[] contents = new String[] { "id", "nama", "jenis", "mulai", "sampai", "informasi", "keterangan",
				"fungsiKerja", "jenjangKarir", "jurusan", "lulusan", "persyaratan", "tanggungJawab", "disclaimer",
				"fasilitas", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, GelombangPendaftaranPegawai.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class GelombangPendaftaranPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final GelombangPendaftaranPegawai gelombangPendaftaranPegawai = (GelombangPendaftaranPegawai) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						Tabbox tabbox = new Tabbox();
						tabbox.setParent(detail);
						tabbox.setHeight("100%");
						tabbox.setWidth("100%");

						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						MyTabConfig tabRuangUjian = new MyTabConfig("Data Seleksi");
						tabRuangUjian.setParent(tabs);

						MyTabConfig tabRuangRuang = new MyTabConfig("Ruangan Seleksi");
						tabRuangRuang.setParent(tabs);

						MyTabConfig tabJadwal = new MyTabConfig("Agenda Seleksi");
						tabJadwal.setParent(tabs);

						MyTabConfig tabParameterTambahan = new MyTabConfig("Parameter Tambahan");
						tabParameterTambahan.setParent(tabs);

						MyTabConfig tabParameter = new MyTabConfig("Parameter Verifikasi");
						tabParameter.setParent(tabs);

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						Tabpanel ruangUjianTabpanel = new ais.ui.util.MyTabpanel();
						ruangUjianTabpanel.setParent(tabpanels);
						ruangUjianTabpanel.setHeight("890px");
						ruangUjianTabpanel.setWidth("100%");

						MyInclude iframe = new MyInclude("/pages/pegawai/ujian_pegawai.zul?gelombangPendaftaranPegawai="
								+ gelombangPendaftaranPegawai.getId());
						iframe.setHeight("890px");
						iframe.setWidth("100%");
						iframe.setParent(ruangUjianTabpanel);

						final Tabpanel ruangRuangTabpanel = new ais.ui.util.MyTabpanel();
						ruangRuangTabpanel.setParent(tabpanels);
						ruangRuangTabpanel.setHeight("890px");
						ruangRuangTabpanel.setWidth("100%");

						tabRuangRuang.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (ruangRuangTabpanel.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude(
											"/pages/pegawai/ruang_pegawai.zul?gelombangPendaftaranPegawai="
													+ gelombangPendaftaranPegawai.getId());
									iframe.setHeight("890px");
									iframe.setWidth("100%");
									iframe.setParent(ruangRuangTabpanel);

								}
							}
						});

						final Tabpanel ruangJadwalTabpanel = new ais.ui.util.MyTabpanel();
						ruangJadwalTabpanel.setParent(tabpanels);
						ruangJadwalTabpanel.setHeight("890px");
						ruangJadwalTabpanel.setWidth("100%");

						tabJadwal.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (ruangJadwalTabpanel.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude(
											"/pages/pegawai/jadwal_ujian_pegawai.zul?gelombangPendaftaranPegawai="
													+ gelombangPendaftaranPegawai.getId());
									iframe.setHeight("890px");
									iframe.setWidth("100%");
									iframe.setParent(ruangJadwalTabpanel);

								}
							}
						});

						final Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
						tabpanelUtama.setParent(tabpanels);
						tabpanelUtama.setHeight("900px");
						tabpanelUtama.setWidth("100%");
						tabParameterTambahan.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (tabpanelUtama.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude(
											"/pages/pegawai/parameter_tambahan_gelombang.zul?gelombangPendaftaranPegawai="
													+ gelombangPendaftaranPegawai.getId());
									iframe.setHeight("900px");
									iframe.setWidth("100%");
									iframe.setParent(tabpanelUtama);

								}
							}
						});

						final Tabpanel jurusanTabpanel = new ais.ui.util.MyTabpanel();
						jurusanTabpanel.setParent(tabpanels);
						jurusanTabpanel.setHeight("890px");
						jurusanTabpanel.setWidth("100%");

						tabParameter.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (jurusanTabpanel.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude(
											"/pages/pegawai/gelombang_punya_parameter_verifikasi_calon_pegawai.zul?gelombangPendaftaranPegawai="
													+ gelombangPendaftaranPegawai.getId());
									iframe.setHeight("890px");
									iframe.setWidth("100%");
									iframe.setParent(jurusanTabpanel);

								}
							}
						});

					}
				}
			});

			RevisiHelper.createNewRevisi(GelombangPendaftaranPegawai.class, gelombangPendaftaranPegawai,
					gelombangPendaftaranPegawai.getNama()).setParent(arg0);
			new Label(gelombangPendaftaranPegawai.getJenis()).setParent(arg0);
			new Label(gelombangPendaftaranPegawai.getMulai() == null ? ""
					: Common.dateFormat1.get().format(gelombangPendaftaranPegawai.getMulai())).setParent(arg0);
			new Label(gelombangPendaftaranPegawai.getSampai() == null ? ""
					: Common.dateFormat1.get().format(gelombangPendaftaranPegawai.getSampai())).setParent(arg0);

			new Label(gelombangPendaftaranPegawai.getSatuanKerja() == null ? "Semua"
					: gelombangPendaftaranPegawai.getSatuanKerja().getNama()).setParent(arg0);
			new Label(gelombangPendaftaranPegawai.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(gelombangPendaftaranPegawai.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					gelombangPendaftaranPegawai.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(gelombangPendaftaranPegawai);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, gelombangPendaftaranPegawai,
					GelombangPendaftaranPegawaiAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new GelombangPendaftaranPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		gelombangPendaftaranPegawai = (GelombangPendaftaranPegawai) obj;
		init(gelombangPendaftaranPegawai);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final GelombangPendaftaranPegawai gelombangPendaftaranPegawai) throws Exception {
		this.gelombangPendaftaranPegawai = gelombangPendaftaranPegawai;
		addWindow.setTitle(gelombangPendaftaranPegawai.getId() == null ? "Tambah Gelombang Pendaftaran" : "Ubah Gelombang Pendaftaran");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Pendaftaran *"));
		row.appendChild(nama = new Textbox(gelombangPendaftaranPegawai.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pendaftaran *"));
		row.appendChild(jenis = new Combobox());
		jenis.setWidth("90%");
		jenis.setReadonly(true);

		MyComboitemConfig combobox = new MyComboitemConfig(GelombangPendaftaranPegawai.PEGAWAI);
		combobox.setValue(GelombangPendaftaranPegawai.PEGAWAI);
		jenis.appendChild(combobox);

		combobox = new MyComboitemConfig(GelombangPendaftaranPegawai.DOSEN);
		combobox.setValue(GelombangPendaftaranPegawai.DOSEN);
		jenis.appendChild(combobox);

		combobox = new MyComboitemConfig(GelombangPendaftaranPegawai.GURU);
		combobox.setValue(GelombangPendaftaranPegawai.GURU);
		jenis.appendChild(combobox);

		Common.selectComboItem(jenis, gelombangPendaftaranPegawai.getJenis());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fungsi Kerja *"));
		row.appendChild(fungsiKerja = new MyTextbox(gelombangPendaftaranPegawai.getFungsiKerja()));
		fungsiKerja.setWidth("90%");

//		row = new MyFormRow();
////		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang Karir *"));
//		row.appendChild(jenjangKarir = new MyTextbox(gelombangPendaftaranPegawai.getJenjangKarir()));
//		jenjangKarir.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pengalaman *"));
		row.appendChild(pengalaman = new MyTextbox(gelombangPendaftaranPegawai.getPengalaman()));
		pengalaman.setWidth("90%");
		pengalaman.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jurusan *"));
		row.appendChild(jurusan = new MyTextbox(gelombangPendaftaranPegawai.getJurusan()));
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lulusan *"));
		row.appendChild(lulusan = new MyTextbox(gelombangPendaftaranPegawai.getLulusan()));
		lulusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Persyaratan *"));
		row.appendChild(persyaratan = new MyTextbox(gelombangPendaftaranPegawai.getPersyaratan()));
		persyaratan.setWidth("90%");
		persyaratan.setRows(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggung Jawab *"));
		row.appendChild(tanggungJawab = new MyTextbox(gelombangPendaftaranPegawai.getTanggungJawab()));
		tanggungJawab.setWidth("90%");
		tanggungJawab.setRows(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fasilitas"));
		row.appendChild(fasilitas = new MyTextbox(gelombangPendaftaranPegawai.getFasilitas()));
		fasilitas.setWidth("90%");
		fasilitas.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Disclaimer"));
		row.appendChild(disclaimer = new MyTextbox(gelombangPendaftaranPegawai.getDisclaimer()));
		disclaimer.setWidth("90%");
		disclaimer.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
		satuanKerja.setValue(gelombangPendaftaranPegawai.getSatuanKerja() == null ? ""
				: gelombangPendaftaranPegawai.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", gelombangPendaftaranPegawai.getSatuanKerja());
		satuanKerja.setWidth("90%");

		Common.initKeterangan(rows, "Kosongkan satuan kerja jika diperuntukkan untuk semua satuan kerja");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pendaftaran *"));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(mulai = new MyDatebox(gelombangPendaftaranPegawai.getMulai()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		hbox.appendChild(sampai = new MyDatebox(gelombangPendaftaranPegawai.getSampai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Informasi yang ditampilkan"));
		row.appendChild(informasi = new Textbox(gelombangPendaftaranPegawai.getInformasi()));
		informasi.setWidth("90%");
		informasi.setRows(3);

		initKelengkapanBerkas(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(gelombangPendaftaranPegawai.getKeterangan()));
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

	@SuppressWarnings("deprecation")
	private void initKelengkapanBerkas(Rows rows) {
		MyFormRow row = new MyFormRow();row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Formulir Verifikasi Kelengkapan Berkas"));
		final MyCheckboxConfig formulirVerifikasi;
		row.appendChild(formulirVerifikasi = new MyCheckboxConfig());
		row.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Formulir Verifikasi Kelengkapan Berkas");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		@SuppressWarnings("unchecked")
		List<VerifikasiKelengkapanCalonPegawai> verifikasiKelengkapanCalonPegawais = HibernateUtil.currentSession()
				.createCriteria(VerifikasiKelengkapanCalonPegawai.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		if (gelombangPendaftaranPegawai.getId() != null) {
			HibernateUtil.currentSession().refresh(this.gelombangPendaftaranPegawai);
		}
		selectedVerifikasiKelengkapanCalonPegawai = this.gelombangPendaftaranPegawai
				.getVerifikasiKelengkapanCalonPegawais();

		subGrid.setVisible(!selectedVerifikasiKelengkapanCalonPegawai.isEmpty());
		formulirVerifikasi.setChecked(!selectedVerifikasiKelengkapanCalonPegawai.isEmpty());

		formulirVerifikasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				subGrid.setVisible(formulirVerifikasi.isChecked());
			}
		});

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final VerifikasiKelengkapanCalonPegawai verifikasiKelengkapanCalonPegawai : verifikasiKelengkapanCalonPegawais) {
			final Checkbox checkbox = new Checkbox(verifikasiKelengkapanCalonPegawai.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(selectedVerifikasiKelengkapanCalonPegawai.contains(verifikasiKelengkapanCalonPegawai));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedVerifikasiKelengkapanCalonPegawai.add(verifikasiKelengkapanCalonPegawai);
					} else {
						selectedVerifikasiKelengkapanCalonPegawai.remove(verifikasiKelengkapanCalonPegawai);
					}
				}
			});
		}

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Gelombang harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (mulai.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Mulai harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sampai.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Sampai harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (fungsiKerja.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Fungsi Kerja harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
//		if (jenjangKarir.getValue().trim().equals("")) {
//			MyMessageboxConfig.show("Jenjang Karir harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}
		if (jurusan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Jurusan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (persyaratan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Persyaratan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tanggungJawab.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Tanggung Jawab harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (gelombangPendaftaranPegawai.getId() != null) {
			gelombangPendaftaranPegawai = (GelombangPendaftaranPegawai) session.load(GelombangPendaftaranPegawai.class,
					gelombangPendaftaranPegawai.getId());

		}

		gelombangPendaftaranPegawai.setNama(nama.getValue());
		gelombangPendaftaranPegawai.setMulai(mulai.getValue());
		gelombangPendaftaranPegawai.setSampai(sampai.getValue());
		gelombangPendaftaranPegawai.setVerifikasiKelengkapanCalonPegawais(selectedVerifikasiKelengkapanCalonPegawai);
		gelombangPendaftaranPegawai.setKeterangan(keterangan.getValue());
		gelombangPendaftaranPegawai.setInformasi(informasi.getValue());
		gelombangPendaftaranPegawai.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		gelombangPendaftaranPegawai.setJenis((String) jenis.getSelectedItem().getValue());

		gelombangPendaftaranPegawai.setFungsiKerja(fungsiKerja.getValue());
//		gelombangPendaftaranPegawai.setJenjangKarir(jenjangKarir.getValue());
		gelombangPendaftaranPegawai.setJurusan(jurusan.getValue());
		gelombangPendaftaranPegawai.setLulusan(lulusan.getValue());
		gelombangPendaftaranPegawai.setPersyaratan(persyaratan.getValue());
		gelombangPendaftaranPegawai.setTanggungJawab(tanggungJawab.getValue());
		gelombangPendaftaranPegawai.setDisclaimer(disclaimer.getValue());
		gelombangPendaftaranPegawai.setFasilitas(fasilitas.getValue());

		Common.refreshSaveOrUpdate(session, gelombangPendaftaranPegawai);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(GelombangPendaftaranPegawai.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<GelombangPendaftaranPegawai> gelombangPendaftaranPegawai = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(gelombangPendaftaranPegawai);
		grid.setRowRenderer(new GelombangPendaftaranPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
