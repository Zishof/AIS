package ais.action.master;

import ais.action.master.catatan.DasbordCatatan;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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

import ais.action.master.helper.BroadcastHelper;
import ais.action.master.helper.ParameterTambahanCatatanAdministrasiListener;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanCatatanAdministrasi;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CatatanAdministrasi;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisCatatanAdministrasi;
import ais.database.model.KelompokParameterTambahanCatatanAdministrasi;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanCatatanAdministrasi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class CatatanAdministrasiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchkode;
	private Textbox searchnama;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private boolean edit = false;
	private boolean delete = false;

	private CatatanAdministrasi catatanAdministrasi;
	private MyToolbarbuttonConfig add;
	private MyDatebox waktu;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox jenisCatatanAdministrasi;

	private Tabpanel tabDasbor;
	private Tabpanel tabJenisCatatanAdministrasi;
	private Tabpanel tabManajemenParameter;
	private Tabpanel tabLaporan;

	private ArrayList<Row> parameterRows;
	private HashMap<String, LampiranLain> lampiranLains;
	private ParameterTambahanCatatanAdministrasiListener parameterTambahanListener;
	private MyTextbox nama;
	private MyCheckboxConfig broadcast;
	private Textbox keterangan;
	private DisposisiSop disposisiSop;
	private Label kode;

	public void onDasbor(Event event) {
		if (tabDasbor.getChildren().size() == 0) {
			DasbordCatatan dasbord = new DasbordCatatan(DasbordCatatan.Lingkup.ADMINISTRASI);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Catatan Administrasi",
				"Semua catatan kegiatan dan agenda administrasi kampus/sekolah.");
		}
	}

	public void onLaporan(Event event) {
		if (tabLaporan.getChildren().size() == 0) {
			LaporanCatatanAdministrasi window = new LaporanCatatanAdministrasi();
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabLaporan);
		}
	}

	public void onJenisCatatanAdministrasi(Event event) {
		if (tabJenisCatatanAdministrasi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabJenisCatatanAdministrasi);
			MyInclude iframe = new MyInclude("/pages/master/jenis_catatan_administrasi.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenParameter(Event event) {
		if (tabManajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabManajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan_catatan_administrasi.zul");
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

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

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

		String[] contents = new String[] { "id", "kode", "nama", "waktu", "satuanKerja", "jenisCatatanAdministrasi",
				"parameterTambahan", "parameterTambahanInds" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(CatatanAdministrasi.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

//		MyToolbarbuttonConfig upload = Common.uploadData(this, CatatanAdministrasi.class, contents);
//		upload.setVisible(add.isVisible() && edit && delete);
//		add.getParent().appendChild(upload);

		onDasbor(null);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class CatatanAdministrasiRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CatatanAdministrasi catatanAdministrasi = (CatatanAdministrasi) arg1;

			if ((catatanAdministrasi.getKode() == null || catatanAdministrasi.getKode().isEmpty())
					&& catatanAdministrasi.getJenisCatatanAdministrasi() != null) {
				String noAgenda = generateCode(catatanAdministrasi.getJenisCatatanAdministrasi(), true);
				catatanAdministrasi.setKode(noAgenda);
				Long currentIndex = getindex(catatanAdministrasi.getJenisCatatanAdministrasi());
				catatanAdministrasi.setIndex(++currentIndex);
				Common.refreshUpdate(catatanAdministrasi);
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(CatatanAdministrasi.class, catatanAdministrasi,
					Common.dateFormat5.get().format(catatanAdministrasi.getWaktu()))).setParent(arg0);
			a.appendChild(new Label(catatanAdministrasi.getKode()));

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(catatanAdministrasi.getJenisCatatanAdministrasi() == null ? ""
					: catatanAdministrasi.getJenisCatatanAdministrasi().getNama()).setParent(vbox);

			JenisCatatanAdministrasi j = catatanAdministrasi.getJenisCatatanAdministrasi();
			Session session = HibernateUtil.currentSession();
			session.refresh(j);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);

			for (KelompokParameterTambahanCatatanAdministrasi kelompokParameterTambahanCatatanAdministrasi : j
					.getKelompokParameterTambahanCatatanAdministrasis()) {

				List<ParameterTambahan> parameterTambahans = ConstantValues
						.simpleList(
								session.createCriteria(ParameterTambahanCatatanAdministrasi.class)
										.add(Restrictions.eq("kelompokParameterTambahanCatatanAdministrasi",
												kelompokParameterTambahanCatatanAdministrasi))
										.createAlias("parameterTambahan", "parameterTambahan")
										.createAlias("kelompokParameterTambahanCatatanAdministrasi",
												"kelompokParameterTambahanCatatanAdministrasi")
										.add(Restrictions.eq("parameterTambahan.aktif", true))
										.add(Restrictions.eq("kelompokParameterTambahanCatatanAdministrasi.aktif",
												true))
										.setProjection(Projections.groupProperty("parameterTambahan.id")),
								ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanCatatanAdministrasi.getId() + "->"
							+ parameterTambahan.getId();

					String val = "";
					String[] spl = catatanAdministrasi.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
						}
					}
					vbox2.appendChild(new MyLabelKecilBold(parameterTambahan.getLabelInputan()));
					LampiranLain lampiranLain = LampiranLain.ambil(catatanAdministrasi.getId(), jenis);

					ParameterTambahan.tampil(vbox2, parameterTambahan, lampiranLain, val);
				}

			}

			if (catatanAdministrasi.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox2);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + catatanAdministrasi.getDisposisiSop().getKeterangan() + " ("
						+ catatanAdministrasi.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(catatanAdministrasi.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			Hbox toolbar;
			(toolbar = Common.copyEditDeleteButtons(edit, delete, catatanAdministrasi, CatatanAdministrasiAction.this))
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
							LaporanCatatanAdministrasi.cetak(catatanAdministrasi);
						}
					});
				}

			});
			button.setParent(toolbar);

		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		CatatanAdministrasi catatanAdministrasi = (CatatanAdministrasi) generalValueObject;
		final JenisCatatanAdministrasi j = catatanAdministrasi.getJenisCatatanAdministrasi();
		if (j == null) {
			return null;
		}

		LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_ADMINISTRASI);

		if (lainMahaadministrasi == null) {
			return null;
		}

		Map parameters = LaporanCatatanAdministrasi.generateParameter(catatanAdministrasi.getTanggal_dirubah(),
				catatanAdministrasi.getTanggal_dirubah(), catatanAdministrasi, j);

		File file = Report.generateCompileFileReport(Report.PDF, parameters,
				lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());

		return file;
	}

	public void onAdd(Event event) throws Exception {
		init(new CatatanAdministrasi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		catatanAdministrasi = (CatatanAdministrasi) obj;
		init(catatanAdministrasi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {
		this.catatanAdministrasi = (CatatanAdministrasi) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Agenda"));
		row.appendChild(kode = new Label(catatanAdministrasi.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Catatan *"));
		row.appendChild(nama = new MyTextbox(catatanAdministrasi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu *"));
		waktu = new MyDatebox(catatanAdministrasi.getWaktu());
		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setWidth("90%");
		waktu.setReadonly(true);

		if (Common.getApakahAdmin()) {
			row.appendChild(waktu);
		} else {
			row.appendChild(new Label(Common.dateFormat3.get().format(waktu.getValue())));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(
				catatanAdministrasi.getSatuanKerja() == null ? "" : catatanAdministrasi.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", catatanAdministrasi.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Catatan *"));
		row.appendChild(jenisCatatanAdministrasi = new Combobox());
		jenisCatatanAdministrasi.setWidth("90%");
		jenisCatatanAdministrasi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		broadcast = new MyCheckboxConfig("Sebarkan / broadcast catatan ini");
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(broadcast);
		broadcast.setChecked(catatanAdministrasi.getBroadcast());

		final MyFormRow rowUsernameDisposisi = new MyFormRow();
		rowUsernameDisposisi.setParent(rows);
		rowUsernameDisposisi.appendChild(new ais.ui.util.MyLabelConfig("Disebarkan ke username pengguna"));
		rowUsernameDisposisi.appendChild(keterangan = new Textbox(catatanAdministrasi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Username Pengguna",
				"/img/user_male_add.png");

		final MyFormRow rowAmbilPengguna = new MyFormRow();
		rowAmbilPengguna.setParent(rows);
		rowAmbilPengguna.appendChild(new ais.ui.util.MyLabelConfig(""));
		rowAmbilPengguna.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Tbmuser tbmuser : tbmusers) {
								keterangan.setValue(
										keterangan.getValue() + (keterangan.getValue().isEmpty() ? tbmuser.getUserId()
												: "," + tbmuser.getUserId()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		final Row r = Common.initKeterangan(rows, "Jika lebih dari satu pengguna, pisahkan dengan tanda koma (,)");

		EventListener startEvent = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				rowUsernameDisposisi.setVisible(broadcast.isChecked());
				rowAmbilPengguna.setVisible(broadcast.isChecked());
				r.setVisible(broadcast.isChecked());
			}

		};

		broadcast.addEventListener("onClick", startEvent);
		startEvent.onEvent(null);

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

		final EventListener eventListenerJenisCatatanAdministrasi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsLampiran);

				JenisCatatanAdministrasi j = (JenisCatatanAdministrasi) (jenisCatatanAdministrasi
						.getSelectedItem() == null ? null : jenisCatatanAdministrasi.getSelectedItem().getValue());

				if (j != null) {

					if (catatanAdministrasi.getId() == null) {
						String noAgenda = generateCode(j, false);
						kode.setValue(noAgenda);
					}

					parameterRows = new ArrayList<Row>();
					lampiranLains = new HashMap<String, LampiranLain>();
					HibernateUtil.currentSession().refresh(j);

					Set<KelompokParameterTambahanCatatanAdministrasi> kelompokParameterTambahanCatatanAdministrasis = new TreeSet<KelompokParameterTambahanCatatanAdministrasi>();
					for (KelompokParameterTambahanCatatanAdministrasi kelompokParameterTambahanCatatanAdministrasi : j
							.getKelompokParameterTambahanCatatanAdministrasis()) {
						kelompokParameterTambahanCatatanAdministrasis.add(kelompokParameterTambahanCatatanAdministrasi);
					}

					parameterTambahanListener = new ParameterTambahanCatatanAdministrasiListener(catatanAdministrasi,
							kelompokParameterTambahanCatatanAdministrasis, parameterRows, lampiranLains, rowsLampiran);

					parameterTambahanListener.onEvent(null);
				}
			}

		};

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.insertCombo(jenisCatatanAdministrasi, new String[] { "nama", "kode" }, "keterangan",
						JenisCatatanAdministrasi.class, Restrictions.eq("aktif", true));
				Common.selectComboItem(jenisCatatanAdministrasi, catatanAdministrasi.getJenisCatatanAdministrasi());

				eventListenerJenisCatatanAdministrasi.onEvent(arg0);
			}

		};

		jenisCatatanAdministrasi.addEventListener("onChange", eventListenerJenisCatatanAdministrasi);
		Common.createDefaultTimer(eventListener);

		return grid;
	}

	private String generateCode(JenisCatatanAdministrasi j, boolean tambah) {

		try {
			if (j == null || j.getNomorSurat() == null) {
				return "";
			}

			Long index = j.getNomorSurat().getGunakanIndexUrut() ? j.getNomorSurat().getNomorIndex() : getindex(j);
			if (tambah) {
				NomorSurat.tambahIndexNomorSurat(j.getNomorSurat());
			}
			String noAgenda = j.getNomorSurat().format(index, waktu.getValue());
			return noAgenda;
		} catch (Exception e) {
			return "";
		}
	}

	private Long getindex(JenisCatatanAdministrasi jenisCatatanAdministrasi) {
		if (jenisCatatanAdministrasi.getNomorSurat() == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(CatatanAdministrasi.class)
				.createAlias("jenisCatatanAdministrasi", "jenisCatatanAdministrasi", Criteria.LEFT_JOIN)
				.createAlias("jenisCatatanAdministrasi.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(jenisCatatanAdministrasi.getNomorSurat().getUrutBerdasarkanNomor()
						? Restrictions.eq("jenisCatatanAdministrasi.nomorSurat",
								jenisCatatanAdministrasi.getNomorSurat())

						: (jenisCatatanAdministrasi.getNomorSurat().getUrutBerdasarkanKelompok()
								&& jenisCatatanAdministrasi.getNomorSurat().getKelompokNomorSurat() != null
										? Restrictions.eq("nomorSurat.kelompokNomorSurat",
												jenisCatatanAdministrasi.getNomorSurat().getKelompokNomorSurat())
										: Restrictions.sqlRestriction("true")))

				.add(jenisCatatanAdministrasi.getNomorSurat().getResetUrutanTiapTahun()
						? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(jenisCatatanAdministrasi.getNomorSurat().getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(jenisCatatanAdministrasi.getNomorSurat().getResetTiap() != null
						&& (Common.dateFormat8.get().format(jenisCatatanAdministrasi.getNomorSurat().getResetTiap())
								.equals(Common.dateFormat8.get().format(sekarang))
								|| jenisCatatanAdministrasi.getNomorSurat().getResetTiap().before(sekarang))
										? Restrictions.ge("waktu",
												jenisCatatanAdministrasi.getNomorSurat().getResetTiap())
										: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	private void init(final CatatanAdministrasi catatanAdministrasi) throws Exception {
		this.catatanAdministrasi = catatanAdministrasi;
		addWindow.setTitle(catatanAdministrasi.getId() == null ? "Tambah Catatan Administrasi" : "Ubah Catatan Administrasi");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop=null;center.appendChild(form(catatanAdministrasi, disposisiSop, save, null));

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

		if (jenisCatatanAdministrasi.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Catatan Administrasi",
					"Kolom Jenis Catatan Administrasi belum Bapak/Ibu pilih, padahal kolom ini wajib diisi "
							+ "sebelum data dapat disimpan.",
					new String[] {
							"Pilih terlebih dahulu Jenis Catatan Administrasi pada kolom yang tersedia.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (parameterTambahanListener != null && !parameterTambahanListener.validate()) {
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (catatanAdministrasi.getId() != null) {
			catatanAdministrasi = (CatatanAdministrasi) session.load(CatatanAdministrasi.class,
					catatanAdministrasi.getId());

		}

		catatanAdministrasi.setWaktu(waktu.getValue());
		catatanAdministrasi.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		catatanAdministrasi.setJenisCatatanAdministrasi(
				(JenisCatatanAdministrasi) (jenisCatatanAdministrasi.getSelectedItem() == null ? null
						: jenisCatatanAdministrasi.getSelectedItem().getValue()));

		catatanAdministrasi.setNama(nama.getValue());
		catatanAdministrasi.setBroadcast(broadcast.isChecked());

		if (disposisiSop != null && disposisiSop.getId() != null) {
			catatanAdministrasi.setDisposisiSop(disposisiSop);
		}

		parameterTambahanListener.onSave(catatanAdministrasi);

//		Common.refreshSaveOrUpdate(session, catatanAdministrasi);

		if (catatanAdministrasi.getId() != null) {

			if (catatanAdministrasi.getIndex() == null) {
				String noAgenda = generateCode(catatanAdministrasi.getJenisCatatanAdministrasi(), true);
				kode.setValue(noAgenda);
				catatanAdministrasi.setKode(noAgenda);
				Long currentIndex = getindex(catatanAdministrasi.getJenisCatatanAdministrasi());
				catatanAdministrasi.setIndex(++currentIndex);
			}

			Common.refreshUpdate(session, catatanAdministrasi);
		} else {
			if (catatanAdministrasi.getKode() == null) {
				String noAgenda = generateCode(catatanAdministrasi.getJenisCatatanAdministrasi(), true);
				kode.setValue(noAgenda);
				catatanAdministrasi.setKode(noAgenda);
			}

			Long currentIndex = getindex(catatanAdministrasi.getJenisCatatanAdministrasi());
			catatanAdministrasi.setIndex(++currentIndex);
			session.save(catatanAdministrasi);
		}

		if (!lampiranLains.isEmpty()) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.getTransaction().begin();
			for (LampiranLain lampiranLain : lampiranLains.values()) {
				streamingSession.refresh(lampiranLain);
				lampiranLain.setRef(catatanAdministrasi.getId());
				streamingSession.update(lampiranLain);
			}
			streamingSession.getTransaction().commit();
			StreamingHibernateUtil.getInstance().closeSession();
		}

		if (broadcast.isChecked()) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					BroadcastHelper.kirimEmailCatatanAdministrasi(catatanAdministrasi);
				}
			});
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				LaporanCatatanAdministrasi.cetak(catatanAdministrasi);
			}
		});

		return true;
	}

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CatatanAdministrasi.class);

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

		;
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<CatatanAdministrasi> catatanAdministrasi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(catatanAdministrasi);
		grid.setRowRenderer(new CatatanAdministrasiRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Catatan Administrasi";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return catatanAdministrasi;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return CatatanAdministrasi.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
//		this.persetujuan = persetujuan;
	}
}
