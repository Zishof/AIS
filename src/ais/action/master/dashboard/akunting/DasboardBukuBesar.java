package ais.action.master.dashboard.akunting;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.West;

import ais.action.maintenance.MainAction;
import ais.action.master.akunting.GrupTransaksiAction;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupAkun;
import ais.database.model.akunting.Transaksi;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBoldMerah;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

/**
 * Melihat mutasi akun secara rinci agar pergerakan debet dan kredit mudah diperiksa.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardBukuBesar extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Center center;
	private MyDatebox mulai;

	private MyDatebox sampai;

	protected Rows rowsAkun;

	private String pencarianAkun = "";
	private List<Long> akunsSelected = new ArrayList<Long>();
	private AmbilDataSatuanKerjaBanbox searchsatuanKerja;

	private MyTextbox bukti;
	private Grid grid;
	private double nilaiSaldo = 0.0;

	private List<String> columnHeadersAdding = new ArrayList<String>();

	private EventListener dataAdding = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			Object[] objects = (Object[]) arg0.getData();
			Transaksi transaksi = (Transaksi) objects[0];
			XSSFRow row = (XSSFRow) objects[2];
			XSSFCellStyle hlink_style = (XSSFCellStyle) objects[7];

			XSSFCell cell = row.createCell(GrupTransaksiAction.contents.length);
			Double nilai = transaksi.getDebet() - transaksi.getKredit();
			nilaiSaldo += nilai;
			cell.setCellStyle(hlink_style);
			cell.setCellValue(nilaiSaldo < 0.0 ? "(" + Common.numberFormat.get().format(Math.abs(nilaiSaldo)) + ")"
					: Common.numberFormat.get().format(nilaiSaldo));

		}
	};

	public DasboardBukuBesar() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DasboardBukuBesar(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {
		columnHeadersAdding.add("Saldo");
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);
		borderlayout.setStyle("min-height:25000px");
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getUserId() != null) {
			Integer desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
			if (desktopHeight != null) {
				borderlayout.setStyle("min-height:" + (desktopHeight * 0.9) + "px");
			}
		}

		North north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("150px");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setWidth("150px");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setWidth("150px");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setWidth("150px");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);
		appendDashboardSopDescriptionRow(rows, "Buku Besar Akuntansi", "menampilkan mutasi debet, kredit, dan saldo berjalan per akun. Gunakan filter tanggal, satuan kerja, kode bukti, dan daftar akun di samping untuk menelusuri transaksi secara lebih terarah.");

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		row.appendChild(mulai = new MyDatebox(calendar.getTime()));
		mulai.setFormat(Common.dateFormat1.get().toPattern());
		mulai.setReadonly(true);

		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		row.appendChild(sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		sampai.setFormat(Common.dateFormat1.get().toPattern());
		sampai.setReadonly(true);

		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(searchsatuanKerja = new AmbilDataSatuanKerjaBanbox());
		searchsatuanKerja.setWidth("90%");
		searchsatuanKerja.setReadonly(true);

		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Bukti"));
		row.appendChild(bukti = new MyTextbox());
		bukti.setWidth("90%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				reload();
			}
		};

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "1,1,6");

		MyToolbarbuttonConfig tampilkan;
		row.appendChild(tampilkan = new MyToolbarbuttonConfig("Tampilkan", "/img/svg/search.svg"));
		tampilkan.addEventListener("onClick", eventListener);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/svg/download.svg");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DasboardBukuBesar.this.grid);
			}
		});
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("300px");

		MyGrid grid2 = new MyGrid();
		grid2.setParent(Common.tampilanScroll1(west));

		grid2.setMold("paging");
		grid2.setPageSize(20);

		Common.clear(grid2);
		rowsAkun = new Rows();
		rowsAkun.setParent(grid2);

		columns = new Columns();
		columns.setParent(grid2);

		column = new MyColumnConfig();
		column.setParent(columns);
		Hbox hbox = new Hbox();
		hbox.setParent(column);

		final Checkbox checkboxSemua = new Checkbox("Semua, cari :");
		checkboxSemua.setParent(hbox);
		final Textbox cari = new Textbox(pencarianAkun);
		final Combobox grupAkun = new Combobox();
		Common.insertComboDanSemua(grupAkun, "nama", GrupAkun.class);
		cari.setParent(hbox);
		cari.setCols(6);
		grupAkun.setParent(hbox);
		grupAkun.setCols(1);

		final EventListener cariAkun = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				GrupAkun grupAkunData = (GrupAkun) (grupAkun.getSelectedItem() == null ? null
						: grupAkun.getSelectedItem().getValue());

				Common.clear(rowsAkun);

				pencarianAkun = cari.getValue().trim();

				Criterion crit = Restrictions.sqlRestriction("false");

				for (String c : pencarianAkun.split(";")) {
					if (!c.trim().isEmpty()) {
						crit = Restrictions.or(crit,
								Restrictions.or(Restrictions.ilike("akun.kode", c, MatchMode.ANYWHERE),
										Restrictions.ilike("akun.nama", c, MatchMode.ANYWHERE)));
					}
				}

				Session session = HibernateUtil.currentSession();
				Criteria criteria = session.createCriteria(Transaksi.class)
						.createAlias("grupTransaksi", "grupTransaksi")
						.add(bukti.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("grupTransaksi.kode", bukti.getValue().trim(), MatchMode.ANYWHERE))

						.add(Restrictions.or(
								Restrictions.between("grupTransaksi.tanggalTransaksi", mulai.getValue(),
										sampai.getValue()),
								Restrictions.sqlRestriction("date(this_.tanggal_transaksi) between date('"
										+ Common.databaseDateFormat.get().format(mulai.getValue()) + "') and date('"
										+ Common.databaseDateFormat.get().format(sampai.getValue()) + "')")))

						.add(Restrictions.isNotNull("akun")).add(Restrictions.isNotNull("grupTransaksi.postingHistory"))
						.setProjection(Projections.groupProperty("akun.id")).createAlias("akun", "akun")

						.add(grupAkunData == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("akun.grupAkun", grupAkunData))

						.add(cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : crit);

				if (!ConstantValues.otomatisTerposting) {
					criteria.createAlias("postingHistory", "postingHistory")
							.add(Restrictions.eq("postingHistory.posting", true));
				}

				List<Akun> akuns = ConstantValues.simpleList(criteria, Akun.class, false);

				Collections.sort(akuns);
				for (Akun akun : akuns) {
					final Long id = akun.getId();
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rowsAkun);
					final Checkbox checkbox = new Checkbox(akun.getKode() + " - " + akun.getNama());
					checkbox.setChecked(checkboxSemua.isChecked() || akunsSelected.contains(id));
					checkbox.setAttribute("akun", akun);
					checkbox.setParent(row);
					row.setValign("top");
					row.setAttribute("checkbox", checkbox);

					checkbox.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (checkbox.isChecked()) {
								akunsSelected.add(id);
							} else {
								akunsSelected.remove(id);
							}

						}
					});
				}

			}
		};

		cariAkun.onEvent(null);
		cari.addEventListener("onOK", cariAkun);
		grupAkun.addEventListener("onChange", cariAkun);

		toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		toolbarbutton.setParent(hbox);
		toolbarbutton.addEventListener("onClick", cariAkun);

		checkboxSemua.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> myRows = rowsAkun.getChildren();
				for (Row row : myRows) {
					Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
					checkbox.setChecked(checkboxSemua.isChecked());
				}
				akunsSelected.clear();
			}
		});

		mulai.addEventListener("onChange", cariAkun);
		sampai.addEventListener("onChange", cariAkun);
		bukti.addEventListener("onChange", cariAkun);

		center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		Common.createDefaultTimer(eventListener);

	}

	@SuppressWarnings({ "unchecked" })
	private void reload() {
		Common.clear(center);
		grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		SatuanKerja satuanKerja = (SatuanKerja) searchsatuanKerja.getAttribute("satuanKerja");
		Long satker = satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId();
		final Date tglMulai = mulai.getValue();
		Date tglSampai = sampai.getValue();

		String sqlAkun = "";
		List<Row> myRows = rowsAkun.getChildren();
		for (Row row : myRows) {
			Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
			if (checkbox.isChecked()) {
				Akun akun = (Akun) checkbox.getAttribute("akun");
				sqlAkun += sqlAkun.isEmpty() ? akun.getId() + "" : "," + akun.getId();
			}
		}

		String sql = "select c.kode as kode_transaksi, c.tanggal_transaksi, "
				+ "b.kode as kode_akun, b.nama as nama_akun, "
				+ "a.keterangan as keterangan_transaksi, aaa.saldo_awal as saldo_awal, "
				+ "a.debet, a.kredit, b.kode, b.id as idAkun, c.id as idGrup   "
				+ "from akunting.transaksi a inner join akunting.akun b on (a.akun = b.id)  left join ( "
				+ "  select sum(debet-kredit) as saldo_awal,aa.akun from akunting.transaksi aa "
				+ "  inner join akunting.grup_transaksi cc on (cc.id=aa.grup_transaksi) ";

		if (!ConstantValues.otomatisTerposting) {
			sql += "  inner join akunting.posting_history dd on (dd.id=cc.posting_history and dd.posting=true) ";
		}

		sql += "  where date(aa.tanggal_transaksi)<date(:mulai) "
				+ "  and cc.posting_history is not null and aa.akun is not null "
				+ "  and case when :bukti='' then true else cc.kode ilike :buktilike end " + "  and case when " + satker
				+ " = -1 then true else " + satker + "=cc.satuan_kerja end "
				+ (sqlAkun.isEmpty() ? " and false " : " and aa.akun in (" + sqlAkun + ") ")
				+ "  group by aa.akun ) aaa on (aaa.akun = a.akun)  "
				+ "inner join akunting.grup_transaksi c on (c.id=a.grup_transaksi)   "
				+ "where c.posting_history is not null and a.akun is not null "
				+ "and case when :bukti='' then true else c.kode ilike :buktilike end  and case when " + satker
				+ " = -1 then true else " + satker + "=c.satuan_kerja end "
				+ (sqlAkun.isEmpty() ? " and false " : " and a.akun in (" + sqlAkun + ") ")
				+ "and date(c.tanggal_transaksi) between date(:mulai) and date(:sampai) "
				+ "order by b.kode,c.tanggal_transaksi";

		List<Object[]> objects = HibernateUtil.currentSession().createSQLQuery(sql)
				.setString("bukti", bukti.getValue().trim()).setString("buktilike", "%" + bukti.getValue().trim() + "%")
				.setString("mulai", Common.databaseDateFormat.get().format(tglMulai))
				.setString("sampai", Common.databaseDateFormat.get().format(tglSampai)).list();

		MyColumnConfig column = new MyColumnConfig("No. Bukti");
		column.setParent(columns);

		column = new MyColumnConfig("Tanggal dan Waktu");
		column.setParent(columns);

		column = new MyColumnConfig("Keterangan");
		column.setParent(columns);

		column = new MyColumnConfig("Debet");
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig("Kredit");
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig("Saldo");
		column.setParent(columns);
		column.setAlign("right");

		Rows rows = new Rows();
		rows.setParent(grid);

		Number idAkunData = -1L;
		Date tanggal_transaksiTemp = null;
		String kodeAkun = "";
		String kodeNama = "";
		Double saldoAkhir = 0.0;
		Double totalSaldoAkhir = 0.0;
		for (Object[] o : objects) {
			String kode_transaksi = o[0] == null ? "" : o[0].toString();
			final Date tanggal_transaksi = o[1] == null ? null : (Date) o[1];

			String kode_akun = o[2] == null ? "" : o[2].toString();
			String nama_akun = o[3] == null ? "" : o[3].toString();

			String keterangan_transaksi = o[4] == null ? "" : o[4].toString();
			Number saldo_awal = o[5] == null ? 0.0 : (Number) o[5];
			Number debet = o[6] == null ? 0.0 : (Number) o[6];
			Number kredit = o[7] == null ? 0.0 : (Number) o[7];

			final Number idAkun = o[9] == null ? 0.0 : (Number) o[9];
			final Number idGrup = o[10] == null ? 0.0 : (Number) o[10];

			if (!kodeAkun.equalsIgnoreCase(kode_akun)) {

				final Number idAkunDataD = idAkunData;
				final Date tanggal_transaksiTempData = tanggal_transaksiTemp;

				if (!kodeAkun.isEmpty()) {

					MyFormRow row = new MyFormRow();
					row.setParent(rows);
					row.setStyle("background-color: silver;");
					row.appendChild(new MyLabelBoldMerah("Total"));
					row.appendChild(new MyLabelBoldMerah(kodeAkun));
					row.appendChild(new MyLabelBoldMerah(kodeNama));

					row.appendChild(new Label());
					row.appendChild(new Label());

					A a = new A((totalSaldoAkhir >= 0.0 ? Common.numberFormat.get().format(totalSaldoAkhir)
							: "(" + Common.numberFormat.get().format(Math.abs(totalSaldoAkhir)) + ")"));
					a.setParent(row);
					a.setStyle("font-size:14px;color:red;");
					a.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							nilaiSaldo = 0.0;
							EventListener eventListener = (EventListener) Common
									.cetakDataCustomButton(Transaksi.class, new DataCriteriaWithColumn() {

										@Override
										public Object[] initCriteria(boolean order) {

											try {

												Criteria criteria = HibernateUtil.currentSession()
														.createCriteria(Transaksi.class)

														.add(Restrictions.or(
																Restrictions.or(Restrictions.gt("debet", 0.1),
																		Restrictions.lt("debet", -0.1)),
																Restrictions.or(Restrictions.gt("kredit", 0.1),
																		Restrictions.lt("kredit", -0.1)))

														)

														.add(Restrictions.isNotNull("postingHistory"))
														.add(Restrictions.isNotNull("grupTransaksi"))

														.add(Restrictions
																.sqlRestriction("this_.akun = " + idAkunDataD + " "))

														.add(Restrictions.sqlRestriction(
																"date(this_.tanggal_transaksi) between date('1970-01-01') and date('"
																		+ Common.databaseDateFormat.get().format(
																				tanggal_transaksiTempData)
																		+ "')"))

														.addOrder(Order.asc("tanggalTransaksi"));

												return new Object[] { criteria, GrupTransaksiAction.contents };

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
											return null;
										}

									}, null, "Download Data", "/img/svg/download.svg", columnHeadersAdding, dataAdding, false,
											null, "DATA TAMBAHAN",
											new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"" })
									.getAttribute("eventListener");

							eventListener.onEvent(null);

						}
					});

				}

				MyFormRow row = new MyFormRow();
				row.setParent(rows);
				row.setStyle("background-color: silver;");
				row.appendChild(new MyLabelBoldMerah(kode_akun));
				row.appendChild(new MyLabelBoldMerah(nama_akun));
				row.appendChild(new Label());

				row.appendChild(new Label());
				row.appendChild(new Label());

				A a = new A((saldo_awal.doubleValue() >= 0.0 ? Common.numberFormat.get().format(saldo_awal.doubleValue())
						: "(" + Common.numberFormat.get().format(Math.abs(saldo_awal.doubleValue())) + ")"));
				a.setParent(row);
				a.setStyle("font-size:14px;color:red;");
				a.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						nilaiSaldo = 0.0;
						EventListener eventListener = (EventListener) Common
								.cetakDataCustomButton(Transaksi.class, new DataCriteriaWithColumn() {

									@Override
									public Object[] initCriteria(boolean order) {

										try {

											Criteria criteria = HibernateUtil.currentSession()
													.createCriteria(Transaksi.class)

													.add(Restrictions.or(
															Restrictions.or(Restrictions.gt("debet", 0.1),
																	Restrictions.lt("debet", -0.1)),
															Restrictions.or(Restrictions.gt("kredit", 0.1),
																	Restrictions.lt("kredit", -0.1)))

													)

													.add(Restrictions.isNotNull("postingHistory"))
													.add(Restrictions.isNotNull("grupTransaksi"))

													.add(Restrictions.sqlRestriction("this_.akun = " + idAkun + " "))

													.add(Restrictions.sqlRestriction(
															"date(this_.tanggal_transaksi) between date('1970-01-01') and date('"
																	+ Common.databaseDateFormat.get().format(tglMulai)
																	+ "')"))

													.addOrder(Order.asc("tanggalTransaksi"));

											return new Object[] { criteria, GrupTransaksiAction.contents };

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}
										return null;
									}

								}, null, "Download Data", "/img/svg/download.svg", columnHeadersAdding, dataAdding, false,
										null, "DATA TAMBAHAN",
										new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "" })
								.getAttribute("eventListener");

						eventListener.onEvent(null);

					}
				});

				idAkunData = idAkun;
				tanggal_transaksiTemp = tanggal_transaksi;
				kodeAkun = kode_akun;
				kodeNama = nama_akun;
				saldoAkhir = 0.0;
			}

			saldoAkhir += (debet.doubleValue() - kredit.doubleValue());

			try {
				totalSaldoAkhir = saldo_awal.doubleValue() + saldoAkhir;
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/akunting/DasboardBukuBesar.java:676");
			}

			MyFormRow row = new MyFormRow();
			row.setParent(rows);

			A a = new A(kode_transaksi);
			a.setParent(row);
			a.setStyle("font-size:14px;color:blue;");
			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					Common.displayWindow(
							"/pages/master/akunting/grup_transaksi.zul?grup=" + idGrup + "&akun=" + idAkun + "&mulai="
									+ Common.dateFormat8.get().format(tglMulai) + "&sampai="
									+ Common.dateFormat8.get().format(tanggal_transaksi),
							true, "95%", "95%", null, "", false);

				}
			});

			row.appendChild(new Label(tanggal_transaksi == null ? "" : Common.dateFormat61.get().format(tanggal_transaksi)));
			row.appendChild(new Label(keterangan_transaksi));

			row.appendChild(new Label(debet.doubleValue() >= 0.0 ? Common.numberFormat.get().format(debet.doubleValue())
					: "(" + Common.numberFormat.get().format(Math.abs(debet.doubleValue())) + ")"));
			row.appendChild(new Label(kredit.doubleValue() >= 0.0 ? Common.numberFormat.get().format(kredit.doubleValue())
					: "(" + Common.numberFormat.get().format(Math.abs(kredit.doubleValue())) + ")"));

			a = new A((totalSaldoAkhir >= 0.0 ? Common.numberFormat.get().format(totalSaldoAkhir)
					: "(" + Common.numberFormat.get().format(Math.abs(totalSaldoAkhir)) + ")"));
			a.setParent(row);
			a.setStyle("font-size:14px;color:blue;");
			a.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					nilaiSaldo = 0.0;
					EventListener eventListener = (EventListener) Common
							.cetakDataCustomButton(Transaksi.class, new DataCriteriaWithColumn() {

								@Override
								public Object[] initCriteria(boolean order) {

									try {

										Criteria criteria = HibernateUtil.currentSession()
												.createCriteria(Transaksi.class)

												.add(Restrictions.or(
														Restrictions.or(Restrictions.gt("debet", 0.1),
																Restrictions.lt("debet", -0.1)),
														Restrictions.or(Restrictions.gt("kredit", 0.1),
																Restrictions.lt("kredit", -0.1)))

												)

												.add(Restrictions.isNotNull("postingHistory"))
												.add(Restrictions.isNotNull("grupTransaksi"))

												.add(Restrictions.sqlRestriction("this_.akun = " + idAkun + " "))

												.add(Restrictions.sqlRestriction(
														"date(this_.tanggal_transaksi) between date('1970-01-01') and date('"
																+ Common.databaseDateFormat.get().format(tanggal_transaksi)
																+ "')"))

												.addOrder(Order.asc("tanggalTransaksi"));

										return new Object[] { criteria, GrupTransaksiAction.contents };

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
									return null;
								}

							}, null, "Download Data", "/img/svg/download.svg", columnHeadersAdding, dataAdding, false, null,
									"DATA TAMBAHAN",
									new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" })
							.getAttribute("eventListener");

					eventListener.onEvent(null);

				}
			});

		}

		if (!kodeAkun.isEmpty()) {

			MyFormRow row = new MyFormRow();
			row.setParent(rows);
			row.setStyle("background-color: silver;");
			row.appendChild(new MyLabelBoldMerah("Total"));
			row.appendChild(new MyLabelBoldMerah(kodeAkun));
			row.appendChild(new MyLabelBoldMerah(kodeNama));

			row.appendChild(new Label());
			row.appendChild(new Label());

			final Number idAkunDataD = idAkunData;
			final Date tanggal_transaksiTempData = tanggal_transaksiTemp;

			A a = new A((totalSaldoAkhir >= 0.0 ? Common.numberFormat.get().format(totalSaldoAkhir)
					: "(" + Common.numberFormat.get().format(Math.abs(totalSaldoAkhir)) + ")"));
			a.setParent(row);
			a.setStyle("font-size:14px;color:red;");
			a.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					nilaiSaldo = 0.0;
					EventListener eventListener = (EventListener) Common
							.cetakDataCustomButton(Transaksi.class, new DataCriteriaWithColumn() {

								@Override
								public Object[] initCriteria(boolean order) {

									try {

										Criteria criteria = HibernateUtil.currentSession()
												.createCriteria(Transaksi.class)

												.add(Restrictions.or(
														Restrictions.or(Restrictions.gt("debet", 0.1),
																Restrictions.lt("debet", -0.1)),
														Restrictions.or(Restrictions.gt("kredit", 0.1),
																Restrictions.lt("kredit", -0.1)))

												)

												.add(Restrictions.isNotNull("postingHistory"))
												.add(Restrictions.isNotNull("grupTransaksi"))

												.add(Restrictions.sqlRestriction("this_.akun = " + idAkunDataD + " "))

												.add(Restrictions.sqlRestriction(
														"date(this_.tanggal_transaksi) between date('1970-01-01') and date('"
																+ Common.databaseDateFormat.get()
																		.format(tanggal_transaksiTempData)
																+ "')"))

												.addOrder(Order.asc("tanggalTransaksi"));

										return new Object[] { criteria, GrupTransaksiAction.contents };

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
									return null;
								}

							}, null, "Download Data", "/img/svg/download.svg", columnHeadersAdding, dataAdding, false, null,
									"DATA TAMBAHAN",
									new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" })
							.getAttribute("eventListener");

					eventListener.onEvent(null);

				}
			});
		}

	}

	private void appendDashboardSopDescriptionRow(org.zkoss.zul.Rows rows, String title, String description) {
		if (rows == null) {
			return;
		}
		org.zkoss.zul.Row row = new org.zkoss.zul.Row();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "8");
		org.zkoss.zul.Html html = new org.zkoss.zul.Html("<div style=\"margin:10px 0 12px 0; padding:14px 16px; border-radius:16px; "
				+ "background:#ffffff; border:1px solid #e2e8f0; box-shadow:0 10px 22px rgba(15,23,42,.06); color:#475569; "
				+ "font-size:11.5px; line-height:1.55;\">"
				+ "<div style=\"font-size:15px; font-weight:900; color:#0f172a; margin-bottom:5px;\">" + safeDashboardHtml(title) + "</div>"
				+ "<div><b style=\"color:#0f172a;\"></b> " + safeDashboardHtml(description) + "</div>"
				+ "<div style=\"display:grid; grid-template-columns:repeat(auto-fit,minmax(120px,1fr)); gap:8px; margin-top:12px;\">"
				+ buildMiniInfoCard("1", "Pilih Filter", "Tentukan unit dan periode laporan.")
				+ buildMiniInfoCard("2", "Baca Ringkasan", "Lihat total dan saldo utama.")
				+ buildMiniInfoCard("3", "Telusuri Detail", "Klik angka/akun untuk data rinci.")
				+ "</div></div>");
		html.setParent(row);
	}

	private String buildMiniInfoCard(String no, String title, String desc) {
		return "<div style=\"border-radius:14px; padding:10px; background:#f8fafc; border:1px solid #e2e8f0;\">"
				+ "<div style=\"width:26px; height:26px; border-radius:999px; background:#0f766e; color:#fff; display:flex; align-items:center; justify-content:center; font-weight:900;\">" + safeDashboardHtml(no) + "</div>"
				+ "<div style=\"font-size:12px; font-weight:900; color:#0f172a; margin-top:7px;\">" + safeDashboardHtml(title) + "</div>"
				+ "<div style=\"font-size:10.5px; color:#64748b; line-height:1.45; margin-top:3px;\">" + safeDashboardHtml(desc) + "</div></div>";
	}

	private String safeDashboardHtml(String value) {
		if (value == null) {
			return "";
		}
		String s = value;
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\"", "&quot;");
		s = s.replace("'", "&#39;");
		return s;
	}

}
