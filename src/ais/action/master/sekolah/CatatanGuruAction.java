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
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.ParameterTambahanCatatanGuruListener;
import ais.action.report.format1.sekolah.LaporanCatatanGuru;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CatatanGuru;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JenisCatatanGuru;
import ais.database.model.sekolah.KelompokParameterTambahanCatatanGuru;
import ais.database.model.sekolah.ParameterTambahanCatatanGuru;
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
 * Controller/action ZK untuk catatan guru. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchyayasan}, {@code Combobox
 * searchsekolah}, {@code Combobox searchta}, {@code Combobox searchsmt}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onLaporan()}, {@code onJenisCatatanGuru()}, {@code onManajemenParameter()}, {@code onDasbor()}, {@code
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
public class CatatanGuruAction extends GenericAutowireComposer
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
	private Combobox searchsmt;
	private Combobox searchjenis;

	private AmbilDataGuruBanbox searchguruPembina;

	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private CatatanGuru catatanGuru;
	private MyToolbarbuttonConfig add;
	private MyDatebox waktu;
	private Combobox jenisCatatanGuru;
	private Combobox yayasan;
	private Combobox sekolah;

	private Tabpanel tabDasbor;
	private Tabpanel tabJenisCatatanGuru;
	private Tabpanel tabManajemenParameter;

	private Tabpanel tabLaporan;

	private ArrayList<Row> parameterRows;
	private HashMap<String, LampiranLain> lampiranLains;
	private ParameterTambahanCatatanGuruListener parameterTambahanListener;
	private Combobox tahunAjaran;
	private Combobox semester;
	private AmbilDataGuruBanbox guru;

	public void onLaporan(Event event) {
		if (tabLaporan.getChildren().size() == 0) {
			LaporanCatatanGuru window = new LaporanCatatanGuru();
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabLaporan);
		}
	}

	public void onJenisCatatanGuru(Event event) {
		if (tabJenisCatatanGuru.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabJenisCatatanGuru);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/jenis_catatan_guru.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenParameter(Event event) {
		if (tabManajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabManajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/parameter_tambahan_catatan_guru.zul");
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
		if (tbmuser != null && (tbmuser.ambilGuru() != null || tbmuser.ambilGuru() != null)) {
			tabJenisCatatanGuru.setVisible(false);
			tabJenisCatatanGuru.getLinkedTab().setVisible(false);
			tabManajemenParameter.setVisible(false);
			tabManajemenParameter.getLinkedTab().setVisible(false);
		}

		searchguruPembina.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		Common.insertComboDanSemua(searchjenis, "nama", JenisCatatanGuru.class, Restrictions.eq("aktif", true));

		final EventListener jeniscatatnSiswaListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (searchsekolah.getSelectedItem() == null ? null
						: searchsekolah.getSelectedItem().getValue());

				Common.insertComboDanSemua(searchjenis, new String[] { "nama", "kode" }, "keterangan",
						JenisCatatanGuru.class,
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

		String[] contents = new String[] { "id", "guru", "waktu", "sekolah", "yayasan", "jenisCatatanGuru",
				"keterangan", "parameterTambahan", "parameterTambahanInds", "tahunAjaran", "semester" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(CatatanGuru.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

//		MyToolbarbuttonConfig upload = Common.uploadData(this, CatatanGuru.class, contents);
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
			DasbordCatatan dasbord = new DasbordCatatan(DasbordCatatan.Lingkup.GURU);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Catatan Guru",
				"Catatan kegiatan, evaluasi, dan perkembangan kerja guru.");
		}
	}

	class CatatanGuruRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CatatanGuru catatanGuru = (CatatanGuru) arg1;

			Guru guru = catatanGuru.getGuru();
			CommonMedia.tampilkanGambarKecil(guru).setParent(arg0);

			Vbox aa = new Vbox();
			aa.setParent(arg0);
			new Label(guru.getKode()).setParent(aa);
			new Label(guru.getNik()).setParent(aa);

			(RevisiHelper.createNewRevisi(CatatanGuru.class, catatanGuru, guru.getNama())).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(catatanGuru.getJenisCatatanGuru() == null ? "" : catatanGuru.getJenisCatatanGuru().getNama())
					.setParent(vbox);
			new Label(catatanGuru.getKeterangan()).setParent(vbox);
			new Label(catatanGuru.getTahunAjaran() + "/" + catatanGuru.getSemester()).setParent(vbox);

			JenisCatatanGuru j = catatanGuru.getJenisCatatanGuru();
			Session session = HibernateUtil.currentSession();
			session.refresh(j);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);

			for (KelompokParameterTambahanCatatanGuru kelompokParameterTambahanCatatanGuru : j
					.getKelompokParameterTambahanCatatanGurus()) {

				List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
						session.createCriteria(ParameterTambahanCatatanGuru.class)
								.add(Restrictions.eq("kelompokParameterTambahanCatatanGuru",
										kelompokParameterTambahanCatatanGuru))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanCatatanGuru",
										"kelompokParameterTambahanCatatanGuru")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanCatatanGuru.aktif", true))
								.setProjection(Projections.groupProperty("parameterTambahan.id")),
						ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanCatatanGuru.getId() + "->" + parameterTambahan.getId();

					String val = "";
					String[] spl = catatanGuru.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
						}
					}
					vbox2.appendChild(new MyLabelKecilBold(parameterTambahan.getLabelInputan()));
					LampiranLain lampiranLain = LampiranLain.ambil(catatanGuru.getId(), jenis);

					ParameterTambahan.tampil(vbox2, parameterTambahan, lampiranLain, val);
				}

			}

			Hbox toolbar;
			(toolbar = Common.copyEditDeleteButtons(edit, delete, catatanGuru, CatatanGuruAction.this)).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LaporanCatatanGuru.cetak(catatanGuru);
						}
					});
				}

			});
			button.setParent(toolbar);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new CatatanGuru());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		catatanGuru = (CatatanGuru) obj;
		init(catatanGuru);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(final CatatanGuru catatanGuru) {
		this.catatanGuru = catatanGuru;
		addWindow.setTitle(catatanGuru.getId() == null ? "Tambah Catatan Guru" : "Ubah Catatan Guru");
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
				catatanGuru.getTahunAjaran());
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
		Common.selectComboItem(true, semester, catatanGuru.getSemester());
		semester.setWidth("90%");
		semester.setReadonly(true);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, catatanGuru.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, catatanGuru.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu *"));
		row.appendChild(waktu = new MyDatebox(catatanGuru.getWaktu()));
		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setWidth("90%");
		waktu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Catatan *"));
		row.appendChild(jenisCatatanGuru = new Combobox());
		jenisCatatanGuru.setWidth("90%");
		jenisCatatanGuru.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru *"));
		row.appendChild(guru = new AmbilDataGuruBanbox());

		if (searchguruPembina.getAttribute("guru") != null) {
			catatanGuru.setGuru((Guru) searchguruPembina.getAttribute("guru"));
			guru.setDisabled(searchguruPembina.isDisabled());
		}

		guru.setAttribute("guru", catatanGuru.getGuru());
		guru.setAttribute("myValue", catatanGuru.getGuru());
		guru.setValue(catatanGuru.getGuru() == null ? "" : catatanGuru.getGuru().getNamaGuru());
		guru.setWidth("90%");
		guru.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
		row.appendChild(keterangan = new Textbox(catatanGuru.getKeterangan()));
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

		final EventListener eventListenerJenisCatatanGuru = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsLampiran);

				JenisCatatanGuru j = (JenisCatatanGuru) (jenisCatatanGuru.getSelectedItem() == null ? null
						: jenisCatatanGuru.getSelectedItem().getValue());

				if (j != null) {
					parameterRows = new ArrayList<Row>();
					lampiranLains = new HashMap<String, LampiranLain>();
					HibernateUtil.currentSession().refresh(j);

					Set<KelompokParameterTambahanCatatanGuru> kelompokParameterTambahanCatatanGurus = new TreeSet<KelompokParameterTambahanCatatanGuru>();
					for (KelompokParameterTambahanCatatanGuru kelompokParameterTambahanCatatanGuru : j
							.getKelompokParameterTambahanCatatanGurus()) {
						kelompokParameterTambahanCatatanGurus.add(kelompokParameterTambahanCatatanGuru);
					}

					parameterTambahanListener = new ParameterTambahanCatatanGuruListener(catatanGuru,
							kelompokParameterTambahanCatatanGurus, parameterRows, lampiranLains, rowsLampiran);

					parameterTambahanListener.onEvent(null);
				}
			}

		};

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				Common.insertCombo(jenisCatatanGuru, new String[] { "nama", "kode" }, "keterangan",
						JenisCatatanGuru.class,
						Restrictions.and(Restrictions.eq("sekolah", s), Restrictions.eq("aktif", true)));
				Common.selectComboItem(jenisCatatanGuru, catatanGuru.getJenisCatatanGuru());

				eventListenerJenisCatatanGuru.onEvent(arg0);
			}

		};

		sekolah.addEventListener("onChange", eventListener);
		jenisCatatanGuru.addEventListener("onChange", eventListenerJenisCatatanGuru);
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
		if (guru.getAttribute("guru") == null) {
			MyMessageboxConfig.show("Guru harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisCatatanGuru.getSelectedItem() == null) {
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
		if (guru.getAttribute("guru") == null) {
			MyMessageboxConfig.show("Guru harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (parameterTambahanListener != null && !parameterTambahanListener.validate()) {
			return false;
		}

		Session session = HibernateUtil.currentSession();
		try {
			if (catatanGuru.getId() != null) {
				catatanGuru = (CatatanGuru) session.load(CatatanGuru.class, catatanGuru.getId());

			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		catatanGuru.setWaktu(waktu.getValue());
		catatanGuru.setGuru((Guru) guru.getAttribute("guru"));
		catatanGuru.setJenisCatatanGuru((JenisCatatanGuru) (jenisCatatanGuru.getSelectedItem() == null ? null
				: jenisCatatanGuru.getSelectedItem().getValue()));
		catatanGuru.setKeterangan(keterangan.getValue());
		catatanGuru.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		catatanGuru.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());

		catatanGuru.setTahunAjaran(
				(String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue()));
		catatanGuru.setSemester(
				(Integer) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue()));

		parameterTambahanListener.onSave(catatanGuru);
		Common.refreshSaveOrUpdate(session, catatanGuru);

		if (!lampiranLains.isEmpty()) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.getTransaction().begin();
			for (LampiranLain lampiranLain : lampiranLains.values()) {
				streamingSession.refresh(lampiranLain);
				lampiranLain.setRef(catatanGuru.getId());
				streamingSession.update(lampiranLain);
			}
			streamingSession.getTransaction().commit();
			StreamingHibernateUtil.getInstance().closeSession();
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				LaporanCatatanGuru.cetak(catatanGuru);
			}
		});

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CatatanGuru.class)

				.add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisCatatanGuru", searchjenis.getSelectedItem().getValue()))

				.add((searchguruPembina == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchguruPembina.getAttribute("guru") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("guru", searchguruPembina.getAttribute("guru")),
								Restrictions.eq("guru", searchguruPembina.getAttribute("guru")))))

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
		criteria.createAlias("guru", "guru").add(searchnama.getValue().trim().isEmpty()
				? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("guru.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.or(
								Restrictions.ilike("guru.kode", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("guru.nuptk", searchnama.getValue().trim(), MatchMode.ANYWHERE)))

		);
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<CatatanGuru> catatanGuru = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(catatanGuru);
		grid.setRowRenderer(new CatatanGuruRenderer());
		grid.setModelCheckMobile(strset);

	}

}
