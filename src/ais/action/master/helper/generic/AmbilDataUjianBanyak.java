package ais.action.master.helper.generic;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.BankSoalDetail;
import ais.database.model.GeneralValueObject;
import ais.database.model.Matakuliah;
import ais.database.model.Ujian;
import ais.database.model.UjianPunyaSoal;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataUjianBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private EventListener eventListener;
	private List<Ujian> ujians;
	private List<Ujian> ujiansHanyaDitampilkan;

	private MyTextbox nama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchjenis;
	private AmbilDataMatakuliahBanbox searchmatakuliah;
	private AmbilDataDosenBanbox searchdosen;

	private Set<Long> ids = new HashSet<Long>();

	private String diperuntukkan;
	private Matakuliah matakuliah;
	private Matapelajaran matapelajaran;

	private AmbilDataGuruBanbox searchguru;
	private Combobox searchsekolah;
	private Combobox searchyayasan;
	private boolean pt;
	private boolean ya;
	private Combobox mk;

	public AmbilDataUjianBanyak(List<Ujian> ujians, String diperuntukkan, Matakuliah matakuliah,
			Matapelajaran matapelajaran) {
		super();
		this.ujians = ujians;
		this.diperuntukkan = diperuntukkan;
		this.matakuliah = matakuliah;
		this.matapelajaran = matapelajaran;
		display();
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	public AmbilDataUjianBanyak(List<Ujian> ujians, List<Ujian> ujiansHanyaDitampilkan, String diperuntukkan,
			Matakuliah matakuliah, Matapelajaran matapelajaran) {
		super();
		this.ujians = ujians;
		this.ujiansHanyaDitampilkan = ujiansHanyaDitampilkan;
		this.diperuntukkan = diperuntukkan;
		this.matakuliah = matakuliah;
		this.matapelajaran = matapelajaran;
		display();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class UjianRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Ujian ujian = (Ujian) arg1;
			arg0.setAttribute("ujian", ujian);

			final Checkbox checkbox = new Checkbox(ujian.getNama());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (Ujian myUjian : ujians) {
				if (myUjian.getId().equals(ujian.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.setChecked(ids.contains(ujian.getId()));

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						ids.add(ujian.getId());
					} else {
						ids.remove(ujian.getId());
					}
				}
			});

			RevisiHelper.createNewRevisi(Ujian.class, ujian,
					Common.getBahasaConfig(ujian.getJenis()) + " / " + Common.getBahasaConfig(ujian.getJenisKoreksi())
							+ " / " + Common.getBahasaConfig(ujian.getLevel()) + " / "
							+ Common.numberFormat.get().format(ujian.getNilaiLulus()))
					.setParent(arg0);

			new Label(ujian.getDosen() == null ? (ujian.getGuru() == null ? "Tidak Ada" : ujian.getGuru().getNama())
					: ujian.getDosen().getNama()).setParent(arg0);
			new Label(ujian.getMatakuliah() == null
					? (ujian.getMatapelajaran() == null ? "Tidak Ada" : ujian.getMatapelajaran().getNama())
					: ujian.getMatakuliah().getNama()).setParent(arg0);
			new Label(ujian.getSertifikat() == null ? "" : ujian.getSertifikat().getNama()).setParent(arg0);
			new Label(ujian.getSyaratUjian() == null ? "" : ujian.getSyaratUjian().getNama()).setParent(arg0);

			new Label(ujian.getTanggal_dirubah() == null ? "" : Common.dateFormat3.get().format(ujian.getTanggal_dirubah()))
					.setParent(arg0);

			int count = ((Number) HibernateUtil.currentSession().createCriteria(UjianPunyaSoal.class)
					.add(Restrictions.eq("ujian", ujian)).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();
			new Label(Common.numberFormat.get().format(count)).setParent(arg0);
		}

	}

	public void display() {

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		searchyayasan = new Combobox();
		searchsekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Ujian");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Borderlayout myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		// Paging TUNGGAL: pagingHelper.pasangGridDanPaging(myCenter1, grid) sudah menempatkan grid +
		// kontrol paging server-side sendiri di dalam myCenter1. Pager manual di South dihapus karena
		// menimbulkan "double paging" (dua deret nomor halaman). Offset & total kini dikelola penuh
		// oleh AmbilDataPagingHelper.

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Columns columns = new Columns();
		columns.setParent(searchgrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
		row.appendChild(searchjenis = new Combobox());
		MyComboitemConfig comboitem = new MyComboitemConfig(BankSoal.PILIHAN_GANDA);
		comboitem.setValue(BankSoal.PILIHAN_GANDA);
		searchjenis.appendChild(comboitem);
		comboitem = new MyComboitemConfig(BankSoal.ESAY);
		comboitem.setValue(BankSoal.ESAY);
		searchjenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		searchjenis.appendChild(comboitem);

		searchjenis.setReadonly(true);
		searchjenis.setSelectedItem(comboitem);

		searchjenis.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		searchjenis.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		searchdosen = new AmbilDataDosenBanbox(true);
		searchguru = new AmbilDataGuruBanbox(true);

		row.setParent(rows);

		if (ya) {
			row.appendChild(new ais.ui.util.MyLabelConfig("Guru"));
			row.appendChild(searchguru);
		} else {
			row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
			row.appendChild(searchdosen);
		}

		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		searchguru.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		searchguru.setWidth("90%");
		searchdosen.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Matakuliah")));
		row.appendChild(searchmatakuliah = new AmbilDataMatakuliahBanbox());
		searchmatakuliah.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		if (matakuliah != null) {
			searchmatakuliah.setAttribute("matakuliah", matakuliah);
			searchmatakuliah.setValue(matakuliah.getKode() + "-" + matakuliah.getNama());
		}

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(searchyayasan);
		searchyayasan.setWidth("90%");
		searchyayasan.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah);
		searchsekolah.setWidth("90%");
		searchsekolah.addEventListener(Events.ON_CHANGE, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matapelajaran"));
		row.appendChild(mk = new Combobox());
		mk.setWidth("90%");

		EventListener mkListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (searchsekolah.getSelectedItem() == null ? null
						: searchsekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);

				Common.insertComboDanSemua(mk, new String[] { "nama", "jenisPenilaian" }, "kelompokMatapelajaran",
						Matapelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				mk.setReadonly(true);

				Common.selectComboItem(true, mk, matapelajaran);
			}
		};

		searchsekolah.addEventListener("onChange", mkListener);
		Common.createDefaultTimer(mkListener);

		mk.addEventListener("onChange", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Toolbar toolbar = new Toolbar();
		// Tinggi North tertutup dirapatkan (96px) agar area filter tidak menyisakan ruang kosong besar
		// di sekitar tombol "Pencarian Lebih Lanjut" (default 150px terlihat "terlalu besar").
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar, "96px");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT_100. */
		pagingHelper.pasangOnPaging(new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		pagingHelper.pasangGridDanPaging(myCenter1, grid);

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Ujian");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pembuat");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Matkul/Matpel");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sertifikat");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Syarat");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Wkt. Dibuat");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jumlah Soal");
		column.setWidth("10%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataUjianBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan dan gunakan ujian yang Sama", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<Ujian> ujians = new ArrayList<Ujian>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								Ujian myUjian = (Ujian) row.getAttribute("ujian");
								ujians.add(myUjian);
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataUjianBanyak.java:545");
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), ujians);
					eventListener.onEvent(myEvent);
				}
				AmbilDataUjianBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan dan gunakan ujian baru", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<Ujian> ujians = new ArrayList<Ujian>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								Ujian ujian = (Ujian) row.getAttribute("ujian");

								Ujian myUjian = (Ujian) ujian.clone();
								myUjian.setId(null);
								myUjian.setNama(ujian.getNama() + " (salinan "
										+ Common.dateFormat.get().format(ais.ui.util.WaktuUtil.getDate()) + ")");

								Session session = HibernateUtil.currentSession();

								session.save(myUjian);

								List<UjianPunyaSoal> punyaSoals = session.createCriteria(UjianPunyaSoal.class)
										.add(Restrictions.eq("ujian", ujian)).list();
								for (UjianPunyaSoal ujianPunyaSoal : punyaSoals) {

									UjianPunyaSoal ujianPunyaSoalBaru = new UjianPunyaSoal();
									ujianPunyaSoalBaru.setBankSoal(ujianPunyaSoal.getBankSoal());
									ujianPunyaSoalBaru.setUjian(myUjian);
									session.save(ujianPunyaSoalBaru);

								}

								ujians.add(myUjian);

							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataUjianBanyak.java:594");
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), ujians);
					eventListener.onEvent(myEvent);
				}
				AmbilDataUjianBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan dan gunakan ujian baru serta copy semua soal", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<Ujian> ujians = new ArrayList<Ujian>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
							if (checkbox.isChecked() && !checkbox.isDisabled()) {
								Ujian ujian = (Ujian) row.getAttribute("ujian");

								Ujian myUjian = (Ujian) ujian.clone();
								myUjian.setId(null);
								myUjian.setNama(ujian.getNama() + " (salinan "
										+ Common.dateFormat.get().format(ais.ui.util.WaktuUtil.getDate()) + ")");

								Session session = HibernateUtil.currentSession();

								session.save(myUjian);

								List<UjianPunyaSoal> punyaSoals = session.createCriteria(UjianPunyaSoal.class)
										.add(Restrictions.eq("ujian", ujian)).list();
								for (UjianPunyaSoal ujianPunyaSoal : punyaSoals) {
									List<Long> bankSoalDetails = ujianPunyaSoal.getBankSoal()
											.ambilBankSoalDetail(false);
									BankSoal bankSoalBaru = (BankSoal) ujianPunyaSoal.getBankSoal().clone();
									bankSoalBaru.setId(null);

									session.save(bankSoalBaru);

									for (Long bankSoalDetailid : bankSoalDetails) {

										BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject
												.ambilData(BankSoalDetail.class, bankSoalDetailid.toString());
										if (bankSoalDetail != null) {
											BankSoalDetail bankSoalDetailBaru = (BankSoalDetail) bankSoalDetail.clone();
											bankSoalDetailBaru.setBankSoal(bankSoalBaru);
											bankSoalDetailBaru.setKodeUnik(null);
											bankSoalDetailBaru.setId(null);
											session.save(bankSoalDetailBaru);
										}
									}

									UjianPunyaSoal ujianPunyaSoalBaru = new UjianPunyaSoal();
									ujianPunyaSoalBaru.setBankSoal(bankSoalBaru);
									ujianPunyaSoalBaru.setUjian(myUjian);
									session.save(ujianPunyaSoalBaru);

								}

								ujians.add(myUjian);
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataUjianBanyak.java:661");
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), ujians);
					eventListener.onEvent(myEvent);
				}
				AmbilDataUjianBanyak.this.detach();
			}
		});
		button.setParent(toolbar);

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> values = new ArrayList<Long>();
		if (ujiansHanyaDitampilkan != null) {
			for (Ujian ujian : ujiansHanyaDitampilkan) {
				values.add(ujian.getId());
			}
		}

		List<Long> notIn = new ArrayList<Long>();
		if (ujians != null) {
			for (Ujian u : ujians) {
				notIn.add(u.getId());
			}
		}

		Criteria criteria = session.createCriteria(Ujian.class)

				.add(pt || searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("sekolah"),
										CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				)

				.add(pt || searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("yayasan"),
										CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false)))

				.add(Restrictions.or(Restrictions.isNull("diperuntukkan"),
						Restrictions.eq("diperuntukkan", diperuntukkan)))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(notIn.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", notIn)))

				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.not(Restrictions.in("id", ids)))
				.add(ujiansHanyaDitampilkan == null || values.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("id", values))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(ya || searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("dosen", searchdosen.getAttribute("myValue")))

				.add(pt || searchguru.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("guru", searchguru.getAttribute("myValue")))

				.add(pt || mk.getSelectedItem() == null || mk.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("matapelajaran", mk.getSelectedItem().getValue()))

				.add(ya || searchmatakuliah.getAttribute("matakuliah") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("matakuliah", searchmatakuliah.getAttribute("matakuliah")))

				.add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenis", searchjenis.getSelectedItem().getValue()))

				.add(ya || searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(ya || searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));

		if (order) {
			criteria.addOrder(Order.asc("diperuntukkan")).addOrder(Order.desc("id"));
		}
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Ujian> ujian = ConstantValues.simpleList(
				session.createCriteria(Ujian.class).addOrder(Order.asc("nama"))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(ids.size() == 0 ? Restrictions.sqlRestriction("1!=1") : Restrictions.in("id", ids)),
				Ujian.class);

		// AmbilDataPagingHelper mengelola sendiri total (countDistinct) + firstResult/maxResults
		// berdasarkan pager server-side-nya, sehingga tidak perlu lagi setFirstResult/setMaxResults
		// manual maupun pager manual.
		List<Ujian> myUjian = pagingHelper.cariDenganCriteria(initCriteria(true), Ujian.class);

		ujian.addAll(myUjian);

		ListModel strset = new SimpleListModel(ujian);
		grid.setRowRenderer(new UjianRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
