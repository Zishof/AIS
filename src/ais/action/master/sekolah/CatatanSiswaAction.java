package ais.action.master.sekolah;


import ais.action.master.catatan.DasbordCatatan;
import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
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
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.master.sekolah.helper.ParameterTambahanCatatanKelasSiswaListener;
import ais.action.master.sekolah.helper.ParameterTambahanCatatanSiswaListener;
import ais.action.report.format1.sekolah.LaporanCatatanSiswa;
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
import ais.database.model.sekolah.CatatanKelasSiswa;
import ais.database.model.sekolah.CatatanSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JenisCatatanKelasSiswa;
import ais.database.model.sekolah.JenisCatatanSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelompokParameterTambahanCatatanKelasSiswa;
import ais.database.model.sekolah.KelompokParameterTambahanCatatanSiswa;
import ais.database.model.sekolah.ParameterTambahanCatatanSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
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

public class CatatanSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox searchjenis;
	private Combobox searchta;
	private Combobox searchsmt;

	private AmbilDataGuruBanbox searchguruPembina;

	private AmbilDataSiswaBanbox siswa;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private CatatanSiswa catatanSiswa;
	private MyToolbarbuttonConfig add;
	private MyDatebox waktu;
	private Combobox jenisCatatanSiswa;
	private Combobox yayasan;
	private Combobox sekolah;

	private Tabpanel tabDasbor;
	private Tabpanel tabJenisCatatanSiswa;
	private Tabpanel tabManajemenParameter;

	private Tabpanel tabLaporan;

	private ArrayList<Row> parameterRows;
	private HashMap<String, LampiranLain> lampiranLains;
	private ParameterTambahanCatatanSiswaListener parameterTambahanListener;
	private AmbilDataGuruBanbox guru;
	private Combobox tahunAjaran;
	private Combobox semester;

	public void onLaporan(Event event) {
		if (tabLaporan.getChildren().isEmpty()) {
			LaporanCatatanSiswa window = new LaporanCatatanSiswa();
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabLaporan);
		}
	}

	public void onJenisCatatanSiswa(Event event) {
		if (tabJenisCatatanSiswa.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabJenisCatatanSiswa);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/jenis_catatan_siswa.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenParameter(Event event) {
		if (tabManajemenParameter.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabManajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/parameter_tambahan_catatan_siswa.zul");
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
		super.doAfterCompose(comp);
		Common.initLaguage();

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && (tbmuser.ambilGuru() != null || tbmuser.getSiswa() != null)) {
			tabJenisCatatanSiswa.setVisible(false);
			tabJenisCatatanSiswa.getLinkedTab().setVisible(false);
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

		Common.insertComboDanSemua(searchjenis, "nama", JenisCatatanSiswa.class, Restrictions.eq("aktif", true));

		final EventListener jeniscatatnSiswaListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (searchsekolah.getSelectedItem() == null ? null
						: searchsekolah.getSelectedItem().getValue());

				if (s != null) {
					Common.insertComboDanSemua(searchjenis, new String[] { "nama", "kode" }, "keterangan",
							JenisCatatanSiswa.class,
							Restrictions.and(Restrictions.eq("sekolah", s), Restrictions.eq("aktif", true)));
				}
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

		String[] contents = new String[] { "id", "siswa", "guru", "waktu", "sekolah", "yayasan", "jenisCatatanSiswa",
				"keterangan", "parameterTambahan", "parameterTambahanInds", "tahunAjaran", "semester" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(CatatanSiswa.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

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
			DasbordCatatan dasbord = new DasbordCatatan(DasbordCatatan.Lingkup.SISWA);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Catatan Siswa",
				"Rekap catatan harian dan akademik tiap siswa.");
		}
	}

	class CatatanSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final CatatanSiswa catatanSiswa = (CatatanSiswa) arg1;

			Siswa siswa = catatanSiswa.getSiswa();
			CommonMedia.tampilkanGambarKecil(siswa).setParent(arg0);

			Vbox aa = new Vbox();
			aa.setParent(arg0);
			new Label(siswa.getNomorIndukNasional()).setParent(aa);
			new Label(siswa.getNomorInduk()).setParent(aa);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(CatatanSiswa.class, catatanSiswa, siswa.getNama())).setParent(arg0);
			KelasSiswa kelasSiswa = catatanSiswa.getKelasSiswa();
			if (kelasSiswa != null) {
				(RevisiHelper.createNewRevisi(KelasSiswa.class, kelasSiswa, kelasSiswa.getNama())).setParent(a);
			}

			Vbox rowParalel = new Vbox();
			Hbox hbox = new Hbox();
			rowParalel.appendChild(hbox);
			rowParalel.setParent(arg0);

			StringBuilder n = new StringBuilder();
			if (catatanSiswa != null && catatanSiswa.getGuru() != null) {
				try {
					CommonMedia.tampilkanGambarKecil(catatanSiswa.getGuru()).setParent(hbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				n.append(catatanSiswa.getGuru().getNama());
			}
			rowParalel.appendChild(new MyLabelKecilBold(n.toString()));

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(catatanSiswa.getJenisCatatanSiswa() == null ? "" : catatanSiswa.getJenisCatatanSiswa().getNama())
					.setParent(vbox);
			new Label(catatanSiswa.getKeterangan()).setParent(vbox);
			new Label(catatanSiswa.getTahunAjaran() + "/" + catatanSiswa.getSemester()).setParent(vbox);

			// Buat container utama di dalam kolom (Row) tersebut
			final Vbox containerParameter = new Vbox();
			containerParameter.setParent(arg0);

			// Buat tombol/link untuk men-trigger loading data
			final org.zkoss.zul.A linkLihatDetail = new org.zkoss.zul.A("Lihat Parameter Tambahan ▼");
			linkLihatDetail.setStyle("color: blue; text-decoration: underline; font-weight: bold; font-size: 11px;");
			linkLihatDetail.setParent(containerParameter);

			// Pasang EventListener untuk Lazy Loading
			linkLihatDetail.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					// 1. Hilangkan link setelah di-klik agar tidak bisa di-klik 2 kali
					linkLihatDetail.detach();

					// 2. Buat Vbox untuk menampung hasil generate UI
					Vbox vbox2 = new Vbox();
					vbox2.setParent(containerParameter);

					Session session = HibernateUtil.currentSession();

					// PENTING: Ambil ulang object JenisCatatanSiswa by ID untuk mencegah
					// LazyInitializationException (Detached Object)
					Long idJenis = catatanSiswa.getJenisCatatanSiswa().getId();
					JenisCatatanSiswa j = (JenisCatatanSiswa) session.get(JenisCatatanSiswa.class, idJenis);

					if (j == null)
						return;

					// OPTIMASI: Parsing parameter tambahan dipindah ke luar loop untuk menghemat
					// Memory dan CPU
					Map<String, String> parameterValuesMap = new HashMap<String, String>();
					String paramInds = catatanSiswa.getParameterTambahanInds();
					if (paramInds != null && !paramInds.trim().isEmpty()) {
						String[] spl = paramInds.split("\n");
						for (String d : spl) {
							String[] value = d.split("<=>");
							if (value.length > 0 && value[0] != null) {
								parameterValuesMap.put(value[0].trim().toLowerCase(),
										value.length > 1 ? value[1].trim() : "");
							}
						}
					}

					for (KelompokParameterTambahanCatatanSiswa kelompokParameterTambahanCatatanSiswa : j
							.getKelompokParameterTambahanCatatanSiswas()) {

						List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
								session.createCriteria(ParameterTambahanCatatanSiswa.class)
										.add(Restrictions.eq("kelompokParameterTambahanCatatanSiswa",
												kelompokParameterTambahanCatatanSiswa))
										.createAlias("parameterTambahan", "parameterTambahan")
										.createAlias("kelompokParameterTambahanCatatanSiswa",
												"kelompokParameterTambahanCatatanSiswa")
										.add(Restrictions.eq("parameterTambahan.aktif", true))
										.add(Restrictions.eq("kelompokParameterTambahanCatatanSiswa.aktif", true))
										.setProjection(Projections.groupProperty("parameterTambahan.id")),
								ParameterTambahan.class, false);

						Collections.sort(parameterTambahans);

						for (ParameterTambahan parameterTambahan : parameterTambahans) {
							String jenis = kelompokParameterTambahanCatatanSiswa.getId() + "->"
									+ parameterTambahan.getId();
							String searchKey = jenis.toLowerCase();

							// Mengambil langsung dari Map hasil pre-parsing
							String val = parameterValuesMap.containsKey(searchKey) ? parameterValuesMap.get(searchKey)
									: "";

							vbox2.appendChild(new MyLabelKecilBold(parameterTambahan.getLabelInputan()));
							LampiranLain lampiranLain = LampiranLain.ambil(catatanSiswa.getId(), jenis);
							ParameterTambahan.tampil(vbox2, parameterTambahan, lampiranLain, val);
						}
					}
				}
			});

			Hbox toolbar;
			(toolbar = Common.copyEditDeleteButtons(edit, delete, catatanSiswa, CatatanSiswaAction.this))
					.setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							LaporanCatatanSiswa.cetak(catatanSiswa);
						}
					});
				}
			});
			button.setParent(toolbar);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new CatatanSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		catatanSiswa = (CatatanSiswa) obj;
		init(catatanSiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(final CatatanSiswa catatanSiswa) {
		this.catatanSiswa = catatanSiswa;
		addWindow.setTitle(catatanSiswa.getId() == null ? "Tambah Catatan Siswa" : "Ubah Catatan Siswa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
		Common.selectComboItem(true, tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
				catatanSiswa.getTahunAjaran());
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
		Common.selectComboItem(true, semester, catatanSiswa.getSemester());
		semester.setWidth("90%");
		semester.setReadonly(true);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, catatanSiswa.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, catatanSiswa.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Siswa *"));
		row.appendChild(siswa = new AmbilDataSiswaBanbox(true, true));
		siswa.setAttribute("siswa", catatanSiswa.getSiswa());
		siswa.setValue(catatanSiswa.getSiswa() == null ? "" : catatanSiswa.getSiswa().getNamaSiswa());
		siswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu *"));
		row.appendChild(waktu = new MyDatebox(catatanSiswa.getWaktu()));
		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setWidth("90%");
		waktu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Catatan *"));
		row.appendChild(jenisCatatanSiswa = new Combobox());
		jenisCatatanSiswa.setWidth("90%");
		jenisCatatanSiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru *"));
		row.appendChild(guru = new AmbilDataGuruBanbox());

		if (searchguruPembina.getAttribute("guru") != null) {
			catatanSiswa.setGuru((Guru) searchguruPembina.getAttribute("guru"));
			guru.setDisabled(searchguruPembina.isDisabled());
		}

		guru.setAttribute("guru", catatanSiswa.getGuru());
		guru.setAttribute("myValue", catatanSiswa.getGuru());
		guru.setValue(catatanSiswa.getGuru() == null ? "" : catatanSiswa.getGuru().getNamaGuru());
		guru.setWidth("90%");
		guru.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
		row.appendChild(keterangan = new Textbox(catatanSiswa.getKeterangan()));
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

		final EventListener eventListenerJenisCatatanSiswa = new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsLampiran);

				JenisCatatanSiswa j = (JenisCatatanSiswa) (jenisCatatanSiswa.getSelectedItem() == null ? null
						: jenisCatatanSiswa.getSelectedItem().getValue());

				Siswa s = (Siswa) siswa.getAttribute("siswa");

				String ta = (String) (tahunAjaran.getSelectedItem() == null ? null
						: tahunAjaran.getSelectedItem().getValue());
				Integer smt = (Integer) (semester.getSelectedItem() == null ? null
						: semester.getSelectedItem().getValue());

				if (j != null) {
					parameterRows = new ArrayList<Row>();
					lampiranLains = new HashMap<String, LampiranLain>();

					if (s != null) {
						Session session = HibernateUtil.currentSession();
						JenisCatatanKelasSiswa jck = (JenisCatatanKelasSiswa) ConstantValues.simpleObject(
								session.createCriteria(JenisCatatanKelasSiswa.class)
										.add(Restrictions.ilike("nama", j.getNama(), MatchMode.EXACT))
										.add(Restrictions.eq("sekolah", j.getSekolah()))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.setMaxResults(1),
								JenisCatatanKelasSiswa.class);

						if (jck != null) {
							List<CatatanKelasSiswa> catatanKelasSiswas = session.createCriteria(CatatanKelasSiswa.class)
									.add(Restrictions.eq("tahunAjaran", ta)).add(Restrictions.eq("semester", smt))
									.add(Restrictions.eq("jenisCatatanKelasSiswa", jck))
									.add(Restrictions.eq("kelasSiswa", s.getKelas())).addOrder(Order.asc("id")).list();

							session.refresh(jck);

							Set<KelompokParameterTambahanCatatanKelasSiswa> kelompokParameterTambahanCatatanKelasSiswas = new TreeSet<KelompokParameterTambahanCatatanKelasSiswa>();
							for (KelompokParameterTambahanCatatanKelasSiswa kelompokParameterTambahanCatatanKelasSiswa : jck
									.getKelompokParameterTambahanCatatanKelasSiswas()) {
								kelompokParameterTambahanCatatanKelasSiswas
										.add(kelompokParameterTambahanCatatanKelasSiswa);
							}

							for (CatatanKelasSiswa catatanKelasSiswa : catatanKelasSiswas) {
								ParameterTambahanCatatanKelasSiswaListener parameterTambahanListenerData = new ParameterTambahanCatatanKelasSiswaListener(
										catatanKelasSiswa, kelompokParameterTambahanCatatanKelasSiswas, parameterRows,
										lampiranLains, rowsLampiran, true);
								parameterTambahanListenerData.onEvent(null);
							}
						}
					}

					HibernateUtil.currentSession().refresh(j);

					Set<KelompokParameterTambahanCatatanSiswa> kelompokParameterTambahanCatatanSiswas = new TreeSet<KelompokParameterTambahanCatatanSiswa>();
					for (KelompokParameterTambahanCatatanSiswa kelompokParameterTambahanCatatanSiswa : j
							.getKelompokParameterTambahanCatatanSiswas()) {
						kelompokParameterTambahanCatatanSiswas.add(kelompokParameterTambahanCatatanSiswa);
					}

					parameterTambahanListener = new ParameterTambahanCatatanSiswaListener(catatanSiswa,
							kelompokParameterTambahanCatatanSiswas, parameterRows, lampiranLains, rowsLampiran);

					parameterTambahanListener.onEvent(null);
				}
			}
		};

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				if (s != null) {
					Common.insertCombo(jenisCatatanSiswa, new String[] { "nama", "kode" }, "keterangan",
							JenisCatatanSiswa.class,
							Restrictions.and(Restrictions.eq("sekolah", s), Restrictions.eq("aktif", true)));
					Common.selectComboItem(jenisCatatanSiswa, catatanSiswa.getJenisCatatanSiswa());
				}
				eventListenerJenisCatatanSiswa.onEvent(arg0);
			}
		};

		sekolah.addEventListener("onChange", eventListener);
		jenisCatatanSiswa.addEventListener("onChange", eventListenerJenisCatatanSiswa);
		Common.createDefaultTimer(eventListener);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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
		if (siswa.getAttribute("siswa") == null) {
			MyMessageboxConfig.show("Siswa harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisCatatanSiswa.getSelectedItem() == null) {
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
		// Ambil ID secara aman: proxy yang sudah lepas dari sesi bisa NPE saat getId().
		java.io.Serializable idCatatan = null;
		if (catatanSiswa instanceof org.hibernate.proxy.HibernateProxy) {
			idCatatan = ((org.hibernate.proxy.HibernateProxy) catatanSiswa)
					.getHibernateLazyInitializer().getIdentifier();
		} else if (catatanSiswa != null) {
			try {
				idCatatan = catatanSiswa.getId();
			} catch (Exception e) {
				idCatatan = null;
			}
		}
		if (idCatatan != null) {
			// get() mengembalikan null bila baris sudah dihapus (tidak seperti load()
			// yang membuat proxy lalu melempar ObjectNotFoundException/NPE saat diakses).
			CatatanSiswa fromDb = (CatatanSiswa) session.get(CatatanSiswa.class, idCatatan);
			if (fromDb == null) {
				MyMessageboxConfig.show(
						"Data catatan siswa sudah tidak ada di database (mungkin telah dihapus). "
								+ "Silakan tutup form ini lalu buka kembali.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
			catatanSiswa = fromDb;
		}

		catatanSiswa.setWaktu(waktu.getValue());
		catatanSiswa.setSiswa((Siswa) siswa.getAttribute("siswa"));
		catatanSiswa.setJenisCatatanSiswa((JenisCatatanSiswa) (jenisCatatanSiswa.getSelectedItem() == null ? null
				: jenisCatatanSiswa.getSelectedItem().getValue()));
		catatanSiswa.setKeterangan(keterangan.getValue());
		catatanSiswa.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		catatanSiswa.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		catatanSiswa.setGuru((Guru) guru.getAttribute("guru"));

		catatanSiswa.setTahunAjaran(
				(String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue()));
		catatanSiswa.setSemester(
				(Integer) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue()));

		parameterTambahanListener.onSave(catatanSiswa);

		if (catatanSiswa.getId() != null) {
			Common.refreshUpdate(session, catatanSiswa);
		} else {
			session.save(catatanSiswa);
		}

		// PENAMBAHAN TRY-CATCH-FINALLY UNTUK MENCEGAH LEAK
		if (!lampiranLains.isEmpty()) {
			Session streamingSession = null;
			Transaction tx = null;
			try {
				streamingSession = StreamingHibernateUtil.getInstance().currentSession();
				tx = streamingSession.getTransaction();
				tx.begin();

				for (LampiranLain lampiranLain : lampiranLains.values()) {
					streamingSession.refresh(lampiranLain);
					lampiranLain.setRef(catatanSiswa.getId());
					streamingSession.update(lampiranLain);
				}
				tx.commit();
			} catch (Exception e) {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
				throw e; // Lemparkan kembali agar UI mengerti ada gagal simpan
			} finally {
				if (streamingSession != null) {
					StreamingHibernateUtil.getInstance().closeSession();
				}
			}
		}

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				LaporanCatatanSiswa.cetak(catatanSiswa);
			}
		});

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CatatanSiswa.class);

		Object guruPembina = searchguruPembina.getAttribute("guru");
		if (guruPembina != null) {
			criteria.createAlias("kelasSiswa", "kelasSiswa", Criteria.LEFT_JOIN).add(Restrictions
					.or(Restrictions.eq("guru", guruPembina), Restrictions.eq("kelasSiswa.guruPembina", guruPembina)));
		}

		if (searchjenis.getSelectedItem() != null && searchjenis.getSelectedItem().getValue() != null) {
			criteria.add(Restrictions.eq("jenisCatatanSiswa", searchjenis.getSelectedItem().getValue()));
		}

		if (searchsmt.getSelectedItem() != null && searchsmt.getSelectedItem().getValue() != null) {
			criteria.add(Restrictions.eq("semester", searchsmt.getSelectedItem().getValue()));
		}

		if (searchta.getSelectedItem() != null && searchta.getSelectedItem().getValue() != null) {
			criteria.add(Restrictions.eq("tahunAjaran", searchta.getSelectedItem().getValue()));
		}

		if (searchsekolah.getSelectedItem() != null && searchsekolah.getSelectedItem().getValue() != null) {
			criteria.add(CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false));
		}

		if (searchyayasan.getSelectedItem() != null && searchyayasan.getSelectedItem().getValue() != null) {
			criteria.add(CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		}

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}

		String searchKeyword = searchnama.getValue().trim();

		if (!searchKeyword.isEmpty()) {
			criteria.createAlias("siswa", "siswa")
					.add(Restrictions.or(Restrictions.ilike("siswa.nama", searchKeyword, MatchMode.ANYWHERE),
							Restrictions.or(
									Restrictions.ilike("siswa.nomorIndukNasional", searchKeyword, MatchMode.ANYWHERE),
									Restrictions.ilike("siswa.nomorInduk", searchKeyword, MatchMode.ANYWHERE))));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<CatatanSiswa> catatanSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(catatanSiswa);
		grid.setRowRenderer(new CatatanSiswaRenderer());
		grid.setModelCheckMobile(strset);
	}
}