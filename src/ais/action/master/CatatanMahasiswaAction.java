package ais.action.master;

import ais.action.master.catatan.DasbordCatatan;
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

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.ParameterTambahanCatatanMahasiswaListener;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.format1.akademik.LaporanCatatanMahasiswa;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CatatanMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisCatatanMahasiswa;
import ais.database.model.KelompokParameterTambahanCatatanMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanCatatanMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
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
 * Controller/action ZK untuk catatan mahasiswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchjenis}, {@code Combobox
 * searchta}, {@code Combobox searchsmt}, {@code AmbilDataDosenBanbox searchdosenPembina}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onDasbor()}, {@code onLaporan()}, {@code onJenisCatatanMahasiswa()}, {@code onManajemenParameter()}, {@code
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
public class CatatanMahasiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchjenis;
	private Combobox searchta;
	private Combobox searchsmt;

	private AmbilDataDosenBanbox searchdosenPembina;

	private AmbilDataMahasiswaBanbox mahasiswa;
	private Textbox keterangan;

	private boolean edit = false; 
	private boolean delete = false;

	private CatatanMahasiswa catatanMahasiswa;
	private MyToolbarbuttonConfig add;
	private MyDatebox waktu;
	private Combobox jenisCatatanMahasiswa;

	private Tabpanel tabDasbor;
	private Tabpanel tabJenisCatatanMahasiswa;
	private Tabpanel tabManajemenParameter;
	private Tabpanel tabLaporan;

	private ArrayList<Row> parameterRows;
	private HashMap<String, LampiranLain> lampiranLains;
	private ParameterTambahanCatatanMahasiswaListener parameterTambahanListener;
	private AmbilDataDosenBanbox dosen;
	private Combobox tahunAjaran;
	private Combobox semester;
	private Mahasiswa mahasiswaData = null;

	public void onDasbor(Event event) {
		if (tabDasbor.getChildren().size() == 0) {
			DasbordCatatan dasbord = new DasbordCatatan(DasbordCatatan.Lingkup.MAHASISWA);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Catatan Mahasiswa",
				"Semua catatan harian dan akademik yang dicatat untuk mahasiswa.");
		}
	}

	public void onLaporan(Event event) {
		if (tabLaporan.getChildren().size() == 0) {
			LaporanCatatanMahasiswa window = new LaporanCatatanMahasiswa();
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabLaporan);
		}
	}

	public void onJenisCatatanMahasiswa(Event event) {
		if (tabJenisCatatanMahasiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabJenisCatatanMahasiswa);
			MyInclude iframe = new MyInclude("/pages/master/jenis_catatan_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenParameter(Event event) {
		if (tabManajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabManajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan_catatan_mahasiswa.zul");
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

		mahasiswaData = (Mahasiswa) (execution.getParameter("mahasiswa") == null ? null
				: ConstantValues.ambil(Mahasiswa.class.getName(),
						Long.parseLong(execution.getParameter("mahasiswa").trim())));

		if (mahasiswaData != null) {
			searchnama.setValue(mahasiswaData.getNim());
			searchnama.setDisabled(true);
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && (tbmuser.ambilDosen() != null || tbmuser.getMahasiswa() != null)) {
			tabJenisCatatanMahasiswa.setVisible(false);
			tabJenisCatatanMahasiswa.getLinkedTab().setVisible(false);
			tabManajemenParameter.setVisible(false);
			tabManajemenParameter.getLinkedTab().setVisible(false);
		}

		searchdosenPembina.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.insertComboDanSemua(searchjenis, "nama", JenisCatatanMahasiswa.class, Restrictions.eq("aktif", true));

		final EventListener jeniscatatnMahasiswaListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.insertComboDanSemua(searchjenis, new String[] { "nama", "kode" }, "keterangan",
						JenisCatatanMahasiswa.class, Restrictions.eq("aktif", true));

			}
		};

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

		String[] contents = new String[] { "id", "mahasiswa", "dosen", "waktu", "jenisCatatanMahasiswa", "keterangan",
				"parameterTambahan", "parameterTambahanInds", "tahunAjaran", "semester" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(CatatanMahasiswa.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

//		MyToolbarbuttonConfig upload = Common.uploadData(this, CatatanMahasiswa.class, contents);
//		upload.setVisible(add.isVisible() && edit && delete);
//		add.getParent().appendChild(upload);

		onDasbor(null);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jeniscatatnMahasiswaListener.onEvent(arg0);
				onSearchDefault(null);
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link CatatanMahasiswaAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link CatatanMahasiswaAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see CatatanMahasiswaAction
	 */
	class CatatanMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CatatanMahasiswa catatanMahasiswa = (CatatanMahasiswa) arg1;

			Mahasiswa mahasiswa = catatanMahasiswa.getMahasiswa();
			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(arg0);

			Vbox aa = new Vbox();
			aa.setParent(arg0);
			new Label(mahasiswa.getNim()).setParent(aa);

			(RevisiHelper.createNewRevisi(CatatanMahasiswa.class, catatanMahasiswa, mahasiswa.getNama()))
					.setParent(arg0);
//			TbmuserAction.tampilkanSocialMediaProfile(a, mahasiswa.getSocialMediaProfile());

			Vbox rowParalel = new Vbox();
			Hbox hbox = new Hbox();
			rowParalel.appendChild(hbox);
			rowParalel.setParent(arg0);
			String n = "";
			if (catatanMahasiswa != null && catatanMahasiswa.getDosen() != null) {
				try {
					CommonMedia.tampilkanGambarKecil(catatanMahasiswa.getDosen()).setParent(hbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				n += n.isEmpty() ? catatanMahasiswa.getDosen().getNama() : ", " + catatanMahasiswa.getDosen().getNama();
			}
			rowParalel.appendChild(new MyLabelKecilBold(n));

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(catatanMahasiswa.getJenisCatatanMahasiswa() == null ? ""
					: catatanMahasiswa.getJenisCatatanMahasiswa().getNama()).setParent(vbox);
			new Label(catatanMahasiswa.getKeterangan()).setParent(vbox);
			new Label(catatanMahasiswa.getTahunAjaran() + "/" + catatanMahasiswa.getSemester()).setParent(vbox);

			JenisCatatanMahasiswa j = catatanMahasiswa.getJenisCatatanMahasiswa();
			Session session = HibernateUtil.currentSession();
			session.refresh(j);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);

			for (KelompokParameterTambahanCatatanMahasiswa kelompokParameterTambahanCatatanMahasiswa : j
					.getKelompokParameterTambahanCatatanMahasiswas()) {

				List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
						session.createCriteria(ParameterTambahanCatatanMahasiswa.class)
								.add(Restrictions.eq("kelompokParameterTambahanCatatanMahasiswa",
										kelompokParameterTambahanCatatanMahasiswa))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanCatatanMahasiswa",
										"kelompokParameterTambahanCatatanMahasiswa")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanCatatanMahasiswa.aktif", true))
								.setProjection(Projections.groupProperty("parameterTambahan.id")),
						ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanCatatanMahasiswa.getId() + "->" + parameterTambahan.getId();

					String val = "";
					String[] spl = catatanMahasiswa.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
						}
					}
					vbox2.appendChild(new MyLabelKecilBold(parameterTambahan.getLabelInputan()));
					LampiranLain lampiranLain = LampiranLain.ambil(catatanMahasiswa.getId(), jenis);
					ParameterTambahan.tampil(vbox2, parameterTambahan, lampiranLain, val);
				}

			}

			Hbox toolbar;
			(toolbar = Common.copyEditDeleteButtons(edit, delete, catatanMahasiswa, CatatanMahasiswaAction.this))
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
							LaporanCatatanMahasiswa.cetak(catatanMahasiswa);
						}
					});
				}

			});
			button.setParent(toolbar);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new CatatanMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		catatanMahasiswa = (CatatanMahasiswa) obj;
		init(catatanMahasiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(final CatatanMahasiswa catatanMahasiswa) {
		this.catatanMahasiswa = catatanMahasiswa;
		addWindow.setTitle(catatanMahasiswa.getId() == null ? "Tambah Catatan Mahasiswa" : "Ubah Catatan Mahasiswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		Common.selectComboItem(true, tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
				catatanMahasiswa.getTahunAjaran());
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
		Common.selectComboItem(true, semester, catatanMahasiswa.getSemester());
		semester.setWidth("90%");
		semester.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Mahasiswa *"));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setAttribute("mahasiswa", catatanMahasiswa.getMahasiswa());
		mahasiswa.setValue(catatanMahasiswa.getMahasiswa() == null ? "" : catatanMahasiswa.getMahasiswa().getNama());
		mahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu *"));
		row.appendChild(waktu = new MyDatebox(catatanMahasiswa.getWaktu()));
		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setWidth("90%");
		waktu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Catatan *"));
		row.appendChild(jenisCatatanMahasiswa = new Combobox());
		jenisCatatanMahasiswa.setWidth("90%");
		jenisCatatanMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembina"));
		row.appendChild(dosen = new AmbilDataDosenBanbox());

		if (searchdosenPembina.getAttribute("dosen") != null) {
			catatanMahasiswa.setDosen((Dosen) searchdosenPembina.getAttribute("dosen"));
			dosen.setDisabled(searchdosenPembina.isDisabled());
		}

		dosen.setAttribute("dosen", catatanMahasiswa.getDosen());
		dosen.setAttribute("myValue", catatanMahasiswa.getDosen());
		dosen.setValue(catatanMahasiswa.getDosen() == null ? "" : catatanMahasiswa.getDosen().getNama());
		dosen.setWidth("90%");
		dosen.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
		row.appendChild(keterangan = new Textbox(catatanMahasiswa.getKeterangan()));
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

		final EventListener eventListenerJenisCatatanMahasiswa = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsLampiran);

				JenisCatatanMahasiswa j = (JenisCatatanMahasiswa) (jenisCatatanMahasiswa.getSelectedItem() == null
						? null
						: jenisCatatanMahasiswa.getSelectedItem().getValue());

				Mahasiswa s = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");

				if (s != null) {

					parameterRows = new ArrayList<Row>();
					lampiranLains = new HashMap<String, LampiranLain>();

					if (j == null) {
						return;
					}
					HibernateUtil.currentSession().refresh(j);

					Set<KelompokParameterTambahanCatatanMahasiswa> kelompokParameterTambahanCatatanMahasiswas = new TreeSet<KelompokParameterTambahanCatatanMahasiswa>();
					for (KelompokParameterTambahanCatatanMahasiswa kelompokParameterTambahanCatatanMahasiswa : j
							.getKelompokParameterTambahanCatatanMahasiswas()) {
						kelompokParameterTambahanCatatanMahasiswas.add(kelompokParameterTambahanCatatanMahasiswa);
					}

					parameterTambahanListener = new ParameterTambahanCatatanMahasiswaListener(catatanMahasiswa,
							kelompokParameterTambahanCatatanMahasiswas, parameterRows, lampiranLains, rowsLampiran);

					parameterTambahanListener.onEvent(null);
				}
			}

		};

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.insertCombo(jenisCatatanMahasiswa, new String[] { "nama", "kode" }, "keterangan",
						JenisCatatanMahasiswa.class, Restrictions.eq("aktif", true));
				Common.selectComboItem(jenisCatatanMahasiswa, catatanMahasiswa.getJenisCatatanMahasiswa());

				eventListenerJenisCatatanMahasiswa.onEvent(arg0);
			}

		};

		jenisCatatanMahasiswa.addEventListener("onChange", eventListenerJenisCatatanMahasiswa);
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
		if (mahasiswa.getAttribute("mahasiswa") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Mahasiswa",
					"Kolom Mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisCatatanMahasiswa.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis catatan",
					"Kolom Jenis catatan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis catatan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (dosen.getAttribute("dosen") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Dosen",
					"Kolom Dosen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Dosen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (parameterTambahanListener != null && !parameterTambahanListener.validate()) {
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (catatanMahasiswa.getId() != null) {
			catatanMahasiswa = (CatatanMahasiswa) session.load(CatatanMahasiswa.class, catatanMahasiswa.getId());

		}

		catatanMahasiswa.setWaktu(waktu.getValue());
		catatanMahasiswa.setMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
		catatanMahasiswa.setJenisCatatanMahasiswa(
				(JenisCatatanMahasiswa) (jenisCatatanMahasiswa.getSelectedItem() == null ? null
						: jenisCatatanMahasiswa.getSelectedItem().getValue()));
		catatanMahasiswa.setKeterangan(keterangan.getValue());
		catatanMahasiswa.setDosen((Dosen) dosen.getAttribute("dosen"));

		catatanMahasiswa.setTahunAjaran(
				(String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue()));
		catatanMahasiswa.setSemester(
				(Integer) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue()));

		parameterTambahanListener.onSave(catatanMahasiswa);

		if (catatanMahasiswa.getId() != null) {
			Common.refreshUpdate(session, catatanMahasiswa);
		} else {
			session.save(catatanMahasiswa);
		}

		if (!lampiranLains.isEmpty()) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.getTransaction().begin();
			for (LampiranLain lampiranLain : lampiranLains.values()) {
				streamingSession.refresh(lampiranLain);
				lampiranLain.setRef(catatanMahasiswa.getId());
				streamingSession.update(lampiranLain);
			}
			streamingSession.getTransaction().commit();
			StreamingHibernateUtil.getInstance().closeSession();
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				LaporanCatatanMahasiswa.cetak(catatanMahasiswa);
			}
		});

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CatatanMahasiswa.class)

				.add(mahasiswaData != null ? Restrictions.eq("mahasiswa", mahasiswaData)
						: Restrictions.sqlRestriction("1=1"))

				.add((searchdosenPembina == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchdosenPembina.getAttribute("dosen") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("dosen", searchdosenPembina.getAttribute("dosen"))))

				.add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisCatatanMahasiswa", searchjenis.getSelectedItem().getValue()))

				.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
						|| searchsmt.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester", searchsmt.getSelectedItem().getValue()))

				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						|| searchta.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchta.getSelectedItem().getValue()))

		;

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.createAlias("mahasiswa", "mahasiswa")
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("mahasiswa.nim", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				);
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<CatatanMahasiswa> catatanMahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(catatanMahasiswa);
		grid.setRowRenderer(new CatatanMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
