package ais.action.master.sekolah;


import ais.action.master.catatan.DasbordCatatan;
import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
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
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataKelasSiswaBanbox;
import ais.action.master.sekolah.helper.ParameterTambahanCatatanKelasSiswaListener;
import ais.action.report.format1.sekolah.LaporanCatatanKelasSiswa;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CatatanKelasSiswa;
import ais.database.model.sekolah.JenisCatatanKelasSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelompokParameterTambahanCatatanKelasSiswa;
import ais.database.model.sekolah.ParameterTambahanCatatanKelasSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk catatan kelas siswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchyayasan}, {@code Combobox
 * searchsekolah}, {@code Combobox searchta}, {@code Combobox searchjenis}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onLaporan()}, {@code onJenisCatatanKelasSiswa()}, {@code onManajemenParameter()}, {@code onDasbor()}, {@code
 * onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class CatatanKelasSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox searchta;
	private Combobox searchjenis;
	private Combobox searchsmt;

	private AmbilDataKelasSiswaBanbox searchkelasSiswaPembina;

	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private CatatanKelasSiswa catatanKelasSiswa;
	private MyToolbarbuttonConfig add;
	private MyDatebox waktu;
	private Combobox jenisCatatanKelasSiswa;
	private Combobox yayasan;
	private Combobox sekolah;

	private Tabpanel tabDasbor;
	private Tabpanel tabJenisCatatanKelasSiswa;
	private Tabpanel tabManajemenParameter;

	private Tabpanel tabLaporan;

	private ArrayList<Row> parameterRows;
	private HashMap<String, LampiranLain> lampiranLains;
	private ParameterTambahanCatatanKelasSiswaListener parameterTambahanListener;
	private Combobox tahunAjaran;
	private Combobox semester;
	private AmbilDataKelasSiswaBanbox kelasSiswa;

	public void onLaporan(Event event) {
		if (tabLaporan.getChildren().size() == 0) {
			LaporanCatatanKelasSiswa window = new LaporanCatatanKelasSiswa();
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabLaporan);
		}
	}

	public void onJenisCatatanKelasSiswa(Event event) {
		if (tabJenisCatatanKelasSiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabJenisCatatanKelasSiswa);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/jenis_catatan_kelas_siswa.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenParameter(Event event) {
		if (tabManajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabManajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/parameter_tambahan_catatan_kelas_siswa.zul");
			iframe.setParent(window);
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

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && (tbmuser.ambilGuru() != null || tbmuser.getSiswa() != null)) {
			tabJenisCatatanKelasSiswa.setVisible(false);
			tabJenisCatatanKelasSiswa.getLinkedTab().setVisible(false);
			tabManajemenParameter.setVisible(false);
			tabManajemenParameter.getLinkedTab().setVisible(false);
		}

		searchkelasSiswaPembina.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		Common.insertComboDanSemua(searchjenis, "nama", JenisCatatanKelasSiswa.class, Restrictions.eq("aktif", true));

		final EventListener jeniscatatnSiswaListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (searchsekolah.getSelectedItem() == null ? null
						: searchsekolah.getSelectedItem().getValue());

				Common.insertComboDanSemua(searchjenis, new String[] { "nama", "kode" }, "keterangan",
						JenisCatatanKelasSiswa.class,
						Restrictions.and(Restrictions.eq("sekolah", s), Restrictions.eq("aktif", true)));

			}
		};

		searchsekolah.addEventListener("onChange", jeniscatatnSiswaListener);

		Common.generateTahunAjaran(searchta);

		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(1); }
		searchsmt.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(2); }
		searchsmt.appendChild(comboitem);
		if (searchsmt != null) { searchsmt.setCols(2); }

		Common.selectComboItem(searchsmt, Common.isNowSemensterGanjil() ? 1 : 2);
		if (searchsmt != null) { searchsmt.setReadonly(true); }

		Common.insertComboDanSemua(searchjenis, "nama", JenisCatatanKelasSiswa.class, Restrictions.eq("aktif", true));

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "kelasSiswa", "waktu", "sekolah", "yayasan", "jenisCatatanKelasSiswa",
				"keterangan", "parameterTambahan", "parameterTambahanInds", "tahunAjaran", "semester" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(CatatanKelasSiswa.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

//		MyToolbarbuttonConfig upload = Common.uploadData(this, CatatanKelasSiswa.class, contents);
//		upload.setVisible(add.isVisible() && edit && delete);
//		add.getParent().appendChild(upload);

		onDasbor(null);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				jeniscatatnSiswaListener.onEvent(arg0);
				onSearchDefault(null);
			}
		});
	}

	public void onDasbor(Event event) {
		if (tabDasbor.getChildren().size() == 0) {
			DasbordCatatan dasbord = new DasbordCatatan(DasbordCatatan.Lingkup.KELAS_SISWA);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Catatan Kelas Siswa",
				"Catatan kehadiran, sikap, dan perkembangan siswa di kelas.");
		}
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link CatatanKelasSiswaAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link CatatanKelasSiswaAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see CatatanKelasSiswaAction
	 */
	class CatatanKelasSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CatatanKelasSiswa catatanKelasSiswa = (CatatanKelasSiswa) arg1;

			KelasSiswa kelasSiswa = catatanKelasSiswa.getKelasSiswa();

			Vbox aa = new Vbox();
			aa.setParent(arg0);
			new Label(kelasSiswa.getKurikulumSekolah() == null ? "" : kelasSiswa.getKurikulumSekolah().getNama())
					.setParent(aa);

			(RevisiHelper.createNewRevisi(CatatanKelasSiswa.class, catatanKelasSiswa, kelasSiswa.getNama()))
					.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(catatanKelasSiswa.getJenisCatatanKelasSiswa() == null ? ""
					: catatanKelasSiswa.getJenisCatatanKelasSiswa().getNama()).setParent(vbox);
			new Label(catatanKelasSiswa.getKeterangan()).setParent(vbox);
			new Label(catatanKelasSiswa.getTahunAjaran() + "/" + catatanKelasSiswa.getSemester()).setParent(vbox);

			JenisCatatanKelasSiswa j = catatanKelasSiswa.getJenisCatatanKelasSiswa();
			Session session = HibernateUtil.currentSession();
			session.refresh(j);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);

			for (KelompokParameterTambahanCatatanKelasSiswa kelompokParameterTambahanCatatanKelasSiswa : j
					.getKelompokParameterTambahanCatatanKelasSiswas()) {

				List<ParameterTambahan> parameterTambahans = ConstantValues
						.simpleList(
								session.createCriteria(ParameterTambahanCatatanKelasSiswa.class)
										.add(Restrictions.eq("kelompokParameterTambahanCatatanKelasSiswa",
												kelompokParameterTambahanCatatanKelasSiswa))
										.createAlias("parameterTambahan", "parameterTambahan")
										.createAlias("kelompokParameterTambahanCatatanKelasSiswa",
												"kelompokParameterTambahanCatatanKelasSiswa")
										.add(Restrictions.eq("parameterTambahan.aktif", true))
										.add(Restrictions.eq("kelompokParameterTambahanCatatanKelasSiswa.aktif", true))
										.setProjection(Projections.groupProperty("parameterTambahan.id")),
								ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(CatatanKelasSiswa.class,
							catatanKelasSiswa.getId(), kelompokParameterTambahanCatatanKelasSiswa.getId() + "->"
									+ parameterTambahan.getId());

					String val = "";
					String[] spl = catatanKelasSiswa.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
						}
					}
					vbox2.appendChild(new MyLabelKecilBold(parameterTambahan.getLabelInputan()));
					LampiranLain lampiranLain = LampiranLain.ambil(catatanKelasSiswa.getId(), jenis);

					ParameterTambahan.tampil(vbox2, parameterTambahan, lampiranLain, val);
				}

			}

			Hbox toolbar;
			(toolbar = Common.copyEditDeleteButtons(edit, delete, catatanKelasSiswa, CatatanKelasSiswaAction.this))
					.setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LaporanCatatanKelasSiswa.cetak(catatanKelasSiswa);
						}
					});
				}

			});
			button.setParent(toolbar);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new CatatanKelasSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		catatanKelasSiswa = (CatatanKelasSiswa) obj;
		init(catatanKelasSiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(final CatatanKelasSiswa catatanKelasSiswa) {
		this.catatanKelasSiswa = catatanKelasSiswa;
		addWindow.setTitle(catatanKelasSiswa.getId() == null ? "Tambah Catatan KelasSiswa" : "Ubah Catatan KelasSiswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
		Common.selectComboItem(true, tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
				catatanKelasSiswa.getTahunAjaran());
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(semester = new Combobox());
		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		comboitem.setValue(1);
		semester.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		comboitem.setValue(2);
		semester.appendChild(comboitem);
		Common.selectComboItem(true, semester, catatanKelasSiswa.getSemester());
		semester.setWidth("90%");
		semester.setReadonly(true);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, catatanKelasSiswa.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, catatanKelasSiswa.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu *"));
		row.appendChild(waktu = new MyDatebox(catatanKelasSiswa.getWaktu()));
		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setWidth("90%");
		waktu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Catatan *"));
		row.appendChild(jenisCatatanKelasSiswa = new Combobox());
		jenisCatatanKelasSiswa.setWidth("90%");
		jenisCatatanKelasSiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas Siswa *"));
		row.appendChild(kelasSiswa = new AmbilDataKelasSiswaBanbox(false, false));

		if (searchkelasSiswaPembina.getAttribute("kelasSiswa") != null) {
			catatanKelasSiswa.setKelasSiswa((KelasSiswa) searchkelasSiswaPembina.getAttribute("kelasSiswa"));
			kelasSiswa.setDisabled(searchkelasSiswaPembina.isDisabled());
		}

		kelasSiswa.setAttribute("kelasSiswa", catatanKelasSiswa.getKelasSiswa());
		kelasSiswa.setAttribute("myValue", catatanKelasSiswa.getKelasSiswa());
		kelasSiswa
				.setValue(catatanKelasSiswa.getKelasSiswa() == null ? "" : catatanKelasSiswa.getKelasSiswa().getNama());
		kelasSiswa.setWidth("90%");
		kelasSiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
		row.appendChild(keterangan = new Textbox(catatanKelasSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		MyFormRow rowLampiran = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowLampiran, "2");
		rowLampiran.setParent(rows);

		final Grid gridLampiran = new Grid();
		gridLampiran.setSclass("fgrid");
		gridLampiran.setParent(rowLampiran);

		columns = new Columns();
		columns.setParent(gridLampiran);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		final Rows rowsLampiran = new Rows();
		rowsLampiran.setParent(gridLampiran);

		final EventListener eventListenerJenisCatatanKelasSiswa = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsLampiran);

				JenisCatatanKelasSiswa j = (JenisCatatanKelasSiswa) (jenisCatatanKelasSiswa.getSelectedItem() == null
						? null
						: jenisCatatanKelasSiswa.getSelectedItem().getValue());

				if (j != null) {
					parameterRows = new ArrayList<Row>();
					lampiranLains = new HashMap<String, LampiranLain>();
					HibernateUtil.currentSession().refresh(j);

					Set<KelompokParameterTambahanCatatanKelasSiswa> kelompokParameterTambahanCatatanKelasSiswas = new TreeSet<KelompokParameterTambahanCatatanKelasSiswa>();
					for (KelompokParameterTambahanCatatanKelasSiswa kelompokParameterTambahanCatatanKelasSiswa : j
							.getKelompokParameterTambahanCatatanKelasSiswas()) {
						kelompokParameterTambahanCatatanKelasSiswas.add(kelompokParameterTambahanCatatanKelasSiswa);
					}

					parameterTambahanListener = new ParameterTambahanCatatanKelasSiswaListener(catatanKelasSiswa,
							kelompokParameterTambahanCatatanKelasSiswas, parameterRows, lampiranLains, rowsLampiran,
							false);

					parameterTambahanListener.onEvent(null);
				}
			}

		};

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				Common.insertCombo(jenisCatatanKelasSiswa, new String[] { "nama", "kode" }, "keterangan",
						JenisCatatanKelasSiswa.class,
						Restrictions.and(Restrictions.eq("sekolah", s), Restrictions.eq("aktif", true)));
				Common.selectComboItem(jenisCatatanKelasSiswa, catatanKelasSiswa.getJenisCatatanKelasSiswa());

				eventListenerJenisCatatanKelasSiswa.onEvent(arg0);
			}

		};

		sekolah.addEventListener("onChange", eventListener);
		jenisCatatanKelasSiswa.addEventListener("onChange", eventListenerJenisCatatanKelasSiswa);
		Common.createDefaultTimer(eventListener);

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
		if (kelasSiswa.getAttribute("kelasSiswa") == null) {
			MyMessageboxConfig.show("Kelas Siswa harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisCatatanKelasSiswa.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis catatan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sekolah.getSelectedItem() == null) {
			MyMessageboxConfig.show("Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (yayasan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Yayasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kelasSiswa.getAttribute("kelasSiswa") == null) {
			MyMessageboxConfig.show("KelasSiswa harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (parameterTambahanListener != null && !parameterTambahanListener.validate()) {
			return false;
		}

		Session session = HibernateUtil.currentSession();
		try {
			if (catatanKelasSiswa.getId() != null) {
				catatanKelasSiswa = (CatatanKelasSiswa) session.load(CatatanKelasSiswa.class,
						catatanKelasSiswa.getId());

			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		catatanKelasSiswa.setWaktu(waktu.getValue());
		catatanKelasSiswa.setKelasSiswa((KelasSiswa) kelasSiswa.getAttribute("kelasSiswa"));
		catatanKelasSiswa.setJenisCatatanKelasSiswa(
				(JenisCatatanKelasSiswa) (jenisCatatanKelasSiswa.getSelectedItem() == null ? null
						: jenisCatatanKelasSiswa.getSelectedItem().getValue()));
		catatanKelasSiswa.setKeterangan(keterangan.getValue());
		catatanKelasSiswa.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		catatanKelasSiswa.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());

		catatanKelasSiswa.setTahunAjaran(
				(String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue()));
		catatanKelasSiswa.setSemester(
				(Integer) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue()));

		parameterTambahanListener.onSave(catatanKelasSiswa);
		Common.refreshSaveOrUpdate(session, catatanKelasSiswa);

		if (!lampiranLains.isEmpty()) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.getTransaction().begin();
			for (LampiranLain lampiranLain : lampiranLains.values()) {
				streamingSession.refresh(lampiranLain);
				lampiranLain.setRef(catatanKelasSiswa.getId());
				streamingSession.update(lampiranLain);
			}
			streamingSession.getTransaction().commit();
			StreamingHibernateUtil.getInstance().closeSession();
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				LaporanCatatanKelasSiswa.cetak(catatanKelasSiswa);
			}
		});

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CatatanKelasSiswa.class)

				.add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisCatatanKelasSiswa", searchjenis.getSelectedItem().getValue()))

				.add((searchkelasSiswaPembina == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchkelasSiswaPembina.getAttribute("kelasSiswa") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.eq("kelasSiswa", searchkelasSiswaPembina.getAttribute("kelasSiswa")),
								Restrictions.eq("kelasSiswa", searchkelasSiswaPembina.getAttribute("kelasSiswa")))))

				.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
						|| searchsmt.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester", searchsmt.getSelectedItem().getValue()))

				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						|| searchta.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchta.getSelectedItem().getValue()))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.createAlias("kelasSiswa", "kelasSiswa")
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("kelasSiswa.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("kelasSiswa.nomorIndukNasional",
												searchnama.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("kelasSiswa.nomorInduk", searchnama.getValue().trim(),
												MatchMode.ANYWHERE)))

				);
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<CatatanKelasSiswa> catatanKelasSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(catatanKelasSiswa);
		grid.setRowRenderer(new CatatanKelasSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
