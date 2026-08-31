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

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.ParameterTambahanCatatanPegawaiListener;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.format1.employ.LaporanCatatanPegawai;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CatatanPegawai;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisCatatanPegawai;
import ais.database.model.KelompokParameterTambahanCatatanPegawai;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanCatatanPegawai;
import ais.database.model.Pegawai;
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
 * Controller/action ZK untuk catatan pegawai. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchta}, {@code Combobox
 * searchsmt}, {@code AmbilDataPegawaiBanbox searchpegawaiPembina}, {@code Textbox keterangan};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()});
 * operasi domain lain ({@code onLaporan()}, {@code onJenisCatatanPegawai()}, {@code onManajemenParameter()},
 * {@code onDasbor()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
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
public class CatatanPegawaiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchta;
	private Combobox searchsmt;

	private AmbilDataPegawaiBanbox searchpegawaiPembina;

	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private CatatanPegawai catatanPegawai;
	private MyToolbarbuttonConfig add;
	private MyDatebox waktu;
	private Combobox jenisCatatanPegawai;

	private Tabpanel tabDasbor;
	private Tabpanel tabJenisCatatanPegawai;
	private Tabpanel tabManajemenParameter;
	private Tabpanel tabLaporan;

	private ArrayList<Row> parameterRows;
	private HashMap<String, LampiranLain> lampiranLains;
	private ParameterTambahanCatatanPegawaiListener parameterTambahanListener;
	private Combobox tahunAjaran;
	private Combobox semester;
	private AmbilDataPegawaiBanbox pegawai;
	private Pegawai currPegawai;

	public void onLaporan(Event event) {
		if (tabLaporan.getChildren().size() == 0) {
			LaporanCatatanPegawai window = new LaporanCatatanPegawai();
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabLaporan);
		}
	}

	public void onJenisCatatanPegawai(Event event) {
		if (tabJenisCatatanPegawai.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabJenisCatatanPegawai);
			MyInclude iframe = new MyInclude("/pages/master/jenis_catatan_pegawai.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenParameter(Event event) {
		if (tabManajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabManajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan_catatan_pegawai.zul");
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

		currPegawai = tbmuser.ambilPegawai();

		if (execution.getParameter("currPegawai") != null) {
			currPegawai = (Pegawai) ConstantValues.ambil(Pegawai.class.getName(),
					Long.parseLong(execution.getParameter("currPegawai")));
		}

		if (tbmuser != null && (currPegawai != null)) {
			tabJenisCatatanPegawai.setVisible(false);
			tabJenisCatatanPegawai.getLinkedTab().setVisible(false);
			tabManajemenParameter.setVisible(false);
			tabManajemenParameter.getLinkedTab().setVisible(false);
		}

		if (currPegawai != null) {
			searchpegawaiPembina.setAttribute("pegawai", currPegawai);
			searchpegawaiPembina.setValue(currPegawai.getNama());
			searchpegawaiPembina.setDisabled(true);
		}

		searchpegawaiPembina.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

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

		String[] contents = new String[] { "id", "pegawai", "waktu", "jenisCatatanPegawai", "keterangan",
				"parameterTambahan", "parameterTambahanInds", "tahunAjaran", "semester" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(CatatanPegawai.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

//		MyToolbarbuttonConfig upload = Common.uploadData(this, CatatanPegawai.class, contents);
//		upload.setVisible(add.isVisible() && edit && delete);
//		add.getParent().appendChild(upload);

		onDasbor(null);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (currPegawai != null) {
					searchpegawaiPembina.setAttribute("pegawai", currPegawai);
					searchpegawaiPembina.setValue(currPegawai.getNama());
					searchpegawaiPembina.setDisabled(true);
				}

				onSearchDefault(null);
			}
		});
	}

	public void onDasbor(Event event) {
		if (tabDasbor.getChildren().size() == 0) {
			DasbordCatatan dasbord = new DasbordCatatan(DasbordCatatan.Lingkup.PEGAWAI);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Catatan Pegawai",
				"Catatan kegiatan dan perkembangan kerja pegawai.");
		}
	}

	class CatatanPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CatatanPegawai catatanPegawai = (CatatanPegawai) arg1;

			Pegawai pegawai = catatanPegawai.getPegawai();
			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);

			Vbox aa = new Vbox();
			aa.setParent(arg0);
			new Label(pegawai.getCode()).setParent(aa);
			new Label(pegawai.getMycode()).setParent(aa);

			(RevisiHelper.createNewRevisi(CatatanPegawai.class, catatanPegawai, pegawai.getNama())).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(catatanPegawai.getJenisCatatanPegawai() == null ? ""
					: catatanPegawai.getJenisCatatanPegawai().getNama()).setParent(vbox);
			new Label(catatanPegawai.getKeterangan()).setParent(vbox);
			new Label(catatanPegawai.getTahunAjaran() + "/" + catatanPegawai.getSemester()).setParent(vbox);

			JenisCatatanPegawai j = catatanPegawai.getJenisCatatanPegawai();
			Session session = HibernateUtil.currentSession();
			session.refresh(j);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);

			for (KelompokParameterTambahanCatatanPegawai kelompokParameterTambahanCatatanPegawai : j
					.getKelompokParameterTambahanCatatanPegawais()) {

				List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
						session.createCriteria(ParameterTambahanCatatanPegawai.class)
								.add(Restrictions.eq("kelompokParameterTambahanCatatanPegawai",
										kelompokParameterTambahanCatatanPegawai))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanCatatanPegawai",
										"kelompokParameterTambahanCatatanPegawai")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanCatatanPegawai.aktif", true))
								.setProjection(Projections.groupProperty("parameterTambahan.id")),
						ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanCatatanPegawai.getId() + "->" + parameterTambahan.getId();

					String val = "";
					String[] spl = catatanPegawai.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
						}
					}
					vbox2.appendChild(new MyLabelKecilBold(parameterTambahan.getLabelInputan()));
					LampiranLain lampiranLain = LampiranLain.ambil(catatanPegawai.getId(), jenis);

					ParameterTambahan.tampil(vbox2, parameterTambahan, lampiranLain, val);
				}

			}

			Hbox toolbar;
			(toolbar = Common.copyEditDeleteButtons(edit, delete, catatanPegawai, CatatanPegawaiAction.this))
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
							LaporanCatatanPegawai.cetak(catatanPegawai);
						}
					});
				}

			});
			button.setParent(toolbar);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new CatatanPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		catatanPegawai = (CatatanPegawai) obj;
		init(catatanPegawai);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(final CatatanPegawai catatanPegawai) {
		this.catatanPegawai = catatanPegawai;
		addWindow.setTitle(catatanPegawai.getId() == null ? "Tambah Catatan Pegawai" : "Ubah Catatan Pegawai");
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
				catatanPegawai.getTahunAjaran());
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
		Common.selectComboItem(true, semester, catatanPegawai.getSemester());
		semester.setWidth("90%");
		semester.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu *"));
		row.appendChild(waktu = new MyDatebox(catatanPegawai.getWaktu()));
		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setWidth("90%");
		waktu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Catatan *"));
		row.appendChild(jenisCatatanPegawai = new Combobox());
		jenisCatatanPegawai.setWidth("90%");
		jenisCatatanPegawai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox());

		if (searchpegawaiPembina.getAttribute("pegawai") != null) {
			catatanPegawai.setPegawai((Pegawai) searchpegawaiPembina.getAttribute("pegawai"));
			pegawai.setDisabled(searchpegawaiPembina.isDisabled());
		}

		pegawai.setAttribute("pegawai", catatanPegawai.getPegawai());
		pegawai.setAttribute("myValue", catatanPegawai.getPegawai());
		pegawai.setValue(catatanPegawai.getPegawai() == null ? "" : catatanPegawai.getPegawai().getNama());
		pegawai.setWidth("90%");
		pegawai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
		row.appendChild(keterangan = new Textbox(catatanPegawai.getKeterangan()));
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

		final EventListener eventListenerJenisCatatanPegawai = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsLampiran);

				JenisCatatanPegawai j = (JenisCatatanPegawai) (jenisCatatanPegawai.getSelectedItem() == null ? null
						: jenisCatatanPegawai.getSelectedItem().getValue());

				if (j != null) {
					parameterRows = new ArrayList<Row>();
					lampiranLains = new HashMap<String, LampiranLain>();
					HibernateUtil.currentSession().refresh(j);

					Set<KelompokParameterTambahanCatatanPegawai> kelompokParameterTambahanCatatanPegawais = new TreeSet<KelompokParameterTambahanCatatanPegawai>();
					for (KelompokParameterTambahanCatatanPegawai kelompokParameterTambahanCatatanPegawai : j
							.getKelompokParameterTambahanCatatanPegawais()) {
						kelompokParameterTambahanCatatanPegawais.add(kelompokParameterTambahanCatatanPegawai);
					}

					parameterTambahanListener = new ParameterTambahanCatatanPegawaiListener(catatanPegawai,
							kelompokParameterTambahanCatatanPegawais, parameterRows, lampiranLains, rowsLampiran);

					parameterTambahanListener.onEvent(null);
				}
			}

		};

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.insertCombo(jenisCatatanPegawai, new String[] { "nama", "kode" }, "keterangan",
						JenisCatatanPegawai.class, Restrictions.eq("aktif", true));
				Common.selectComboItem(jenisCatatanPegawai, catatanPegawai.getJenisCatatanPegawai());

				eventListenerJenisCatatanPegawai.onEvent(arg0);
			}

		};

		jenisCatatanPegawai.addEventListener("onChange", eventListenerJenisCatatanPegawai);
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
		if (pegawai.getAttribute("pegawai") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Pegawai",
					"Kolom Pegawai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Pegawai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisCatatanPegawai.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis catatan",
					"Kolom Jenis catatan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis catatan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (pegawai.getAttribute("pegawai") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Pegawai",
					"Kolom Pegawai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Pegawai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (parameterTambahanListener != null && !parameterTambahanListener.validate()) {
			return false;
		}

		Session session = HibernateUtil.currentSession();
		try {
			if (catatanPegawai.getId() != null) {
				catatanPegawai = (CatatanPegawai) session.load(CatatanPegawai.class, catatanPegawai.getId());

			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		catatanPegawai.setWaktu(waktu.getValue());
		catatanPegawai.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		catatanPegawai
				.setJenisCatatanPegawai((JenisCatatanPegawai) (jenisCatatanPegawai.getSelectedItem() == null ? null
						: jenisCatatanPegawai.getSelectedItem().getValue()));
		catatanPegawai.setKeterangan(keterangan.getValue());

		catatanPegawai.setTahunAjaran(
				(String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue()));
		catatanPegawai.setSemester(
				(Integer) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue()));

		parameterTambahanListener.onSave(catatanPegawai);
		Common.refreshSaveOrUpdate(session, catatanPegawai);

		if (!lampiranLains.isEmpty()) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.getTransaction().begin();
			for (LampiranLain lampiranLain : lampiranLains.values()) {
				streamingSession.refresh(lampiranLain);
				lampiranLain.setRef(catatanPegawai.getId());
				streamingSession.update(lampiranLain);
			}
			streamingSession.getTransaction().commit();
			StreamingHibernateUtil.getInstance().closeSession();
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				LaporanCatatanPegawai.cetak(catatanPegawai);
			}
		});

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CatatanPegawai.class)

				.add((searchpegawaiPembina == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchpegawaiPembina.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("pegawai", searchpegawaiPembina.getAttribute("pegawai")),
								Restrictions.eq("pegawai", searchpegawaiPembina.getAttribute("pegawai")))))

				.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
						|| searchsmt.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester", searchsmt.getSelectedItem().getValue()))

				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						|| searchta.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchta.getSelectedItem().getValue()));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.createAlias("pegawai", "pegawai").add(searchnama.getValue().trim().isEmpty()
				? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("pegawai.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.or(
								Restrictions.ilike("pegawai.code", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("pegawai.mycode", searchnama.getValue().trim(), MatchMode.ANYWHERE)))

		);
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<CatatanPegawai> catatanPegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(catatanPegawai);
		grid.setRowRenderer(new CatatanPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
