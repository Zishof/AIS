package ais.action.report.format1.akunting;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.West;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.Transaksi;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanJurnalHarian extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private MyDatebox mulai;
	private MyDatebox sampai;
	private AmbilDataSatuanKerjaBanbox searchsatuanKerja;
	private Center center;

	private Toolbar toolbar;

	protected Rows rowsAkun;

	private String pencarianAkun = "";

	public LaporanJurnalHarian() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Jurnal Harian", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanJurnalHarian(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onReport(event);

			}
		};

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("320px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("40%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Periode Mulai"));
		row.appendChild(mulai = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		mulai.setFormat(Common.dateFormat1.get().toPattern());
		mulai.setReadonly(true);
		// mulai.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Periode Sampai"));
		row.appendChild(sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		sampai.setFormat(Common.dateFormat1.get().toPattern());
		sampai.setReadonly(true);
		// sampai.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(searchsatuanKerja = new AmbilDataSatuanKerjaBanbox());
		searchsatuanKerja.setWidth("90%");
		searchsatuanKerja.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Button tampilkan;
		row.appendChild(tampilkan = new Button("Tampilkan"));
		tampilkan.addEventListener("onClick", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid grid2 = new MyGrid();
		row.appendChild(grid2);

		grid2.setMold("paging");
		grid2.setPageSize(15);

		final EventListener eventListenerAkun = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(grid2);
				rowsAkun = new Rows();
				rowsAkun.setParent(grid2);

				Columns columns = new Columns();
				columns.setParent(grid2);

				Column column = new Column();
				column.setParent(columns);
				Hbox hbox = new Hbox();
				hbox.setParent(column);
				final Checkbox checkboxSemua = new Checkbox("Semua, cari :");
				checkboxSemua.setParent(hbox);
				final Textbox cari = new Textbox(pencarianAkun);
				cari.setParent(hbox);
				cari.setCols(10);

				EventListener cariAkun = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						pencarianAkun = cari.getValue().trim();

						Common.clear(rowsAkun);

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
								.add(Restrictions.or(
										Restrictions.between("grupTransaksi.tanggalTransaksi", mulai.getValue(),
												sampai.getValue()),
										Restrictions.sqlRestriction("date(this_.tanggal_transaksi) between date('"
												+ Common.databaseDateFormat.get().format(mulai.getValue()) + "') and date('"
												+ Common.databaseDateFormat.get().format(sampai.getValue()) + "')")))
								.add(Restrictions.isNotNull("akun")).add(Restrictions.isNotNull("postingHistory"))
								.setProjection(Projections.groupProperty("akun.id")).createAlias("akun", "akun")
								.add(pencarianAkun.isEmpty() ? Restrictions.sqlRestriction("true") : crit);

						List<Akun> akuns = ConstantValues.simpleList(criteria, Akun.class, false);

						Collections.sort(akuns);
						for (Akun akun : akuns) {
							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rowsAkun);
							final Checkbox checkbox = new Checkbox(akun.getKode() + " - " + akun.getNama());
							checkbox.setChecked(checkboxSemua.isChecked());
							checkbox.setAttribute("akun", akun);
							checkbox.setParent(row);
							row.setValign("top");
							row.setAttribute("checkbox", checkbox);

							checkbox.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									List<Row> myRows = rowsAkun.getChildren();

									Akun akun = (Akun) checkbox.getAttribute("akun");
									Session session = HibernateUtil.currentSession();

									Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
									calendar.setTime(mulai.getValue());
									calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);

									Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
									calendar1.setTime(sampai.getValue());
									calendar1.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);

									List<Long> groupUp = session.createCriteria(Transaksi.class)
											.add(Restrictions.eq("akun", akun))
											.createAlias("grupTransaksi", "grupTransaksi")
											.add(Restrictions.between("grupTransaksi.tanggalTransaksi",
													calendar.getTime(), calendar1.getTime()))
											.add(Restrictions.isNotNull("akun"))
											.add(Restrictions.isNotNull("postingHistory"))
											.setProjection(Projections.groupProperty("grupTransaksi.id")).list();

									List<Akun> akuns = session.createCriteria(Transaksi.class)
											.add(groupUp.isEmpty() ? Restrictions.sqlRestriction("false")
													: Restrictions.in("grupTransaksi.id", groupUp))
											.add(Restrictions.ne("akun", akun)).add(Restrictions.isNotNull("akun"))
											.add(Restrictions.isNotNull("postingHistory"))
											.setProjection(Projections.groupProperty("akun"))

											.list();
									Collections.sort(akuns);
									if (checkbox.isChecked()) {

										for (Akun a : akuns) {
											boolean ada = false;
											for (Row row : myRows) {
												Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
												Akun akn = (Akun) checkbox.getAttribute("akun");
												if (akn.getId().equals(a.getId())) {
													checkbox.setChecked(true);
													checkbox.setDisabled(true);
													ada = true;
												}
											}
											if (!ada) {
												MyFormRow row = new MyFormRow();
												row.setValign("top");
												row.setParent(rowsAkun);
												final Checkbox checkbox = new Checkbox(
														a.getKode() + " - " + a.getNama());
												checkbox.setAttribute("akun", a);
												checkbox.setParent(row);
												row.setValign("top");
												row.setAttribute("checkbox", checkbox);
												checkbox.setChecked(true);
												checkbox.setDisabled(true);
											}
										}
									} else {
										for (Akun a : akuns) {
											for (Row row : myRows) {
												Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
												Akun akn = (Akun) checkbox.getAttribute("akun");
												if (akn.getId().equals(a.getId())) {
													checkbox.setChecked(false);
													checkbox.setDisabled(false);
												}
											}
										}
									}
								}
							});
						}
					}
				};

				cariAkun.onEvent(null);
				cari.addEventListener("onOK", cariAkun);
				Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
				toolbarbutton.setParent(hbox);
				toolbarbutton.addEventListener("onClick", cariAkun);

				checkboxSemua.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Row> myRows = rowsAkun.getChildren();
						for (Row row : myRows) {
							Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
							checkbox.setChecked(checkboxSemua.isChecked());
							checkbox.setDisabled(false);
						}

					}
				});

			}
		};

		mulai.addEventListener("onChange", eventListenerAkun);
		sampai.addEventListener("onChange", eventListenerAkun);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				if (mulai.getValue() == null) {
					MyMessageboxConfig.show("Pilih tanggal mulai", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (sampai.getValue() == null) {
					MyMessageboxConfig.show("Pilih tanggal sampai", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "akunting/jurnal_harian_ket", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListenerAkun.onEvent(arg0);
				onReport(null);
			}
		});

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (mulai.getValue() == null) {
			MyMessageboxConfig.show("Pilih tanggal mulai", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (sampai.getValue() == null) {
			MyMessageboxConfig.show("Pilih tanggal sampai", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}

		Date tglMulai = mulai.getValue();
		Date tglSampai = sampai.getValue();

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("mulai", Common.databaseDateFormat.get().format(tglMulai));
		parameters.put("sampai", Common.databaseDateFormat.get().format(tglSampai));
		parameters.put("mulai_1", Common.dateFormat5.get().format(tglMulai));
		parameters.put("sampai_1", Common.dateFormat5.get().format(tglSampai));

		SatuanKerja satuanKerja = (SatuanKerja) searchsatuanKerja.getAttribute("satuanKerja");
		parameters.put("satuan_kerja", satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId());

		List<Long> akuns = new ArrayList<Long>();

		List<Row> myRows = rowsAkun.getChildren();
		for (Row row : myRows) {
			Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
			if (checkbox.isChecked() && !checkbox.isDisabled()) {
				Akun akun = (Akun) checkbox.getAttribute("akun");
				akuns.add(akun.getId());
			}
		}
		if (akuns.isEmpty()) {
			akuns.add(-1L);
		}
		parameters.put("akuns", akuns.toArray());

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(mulai.getValue());
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);

		Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
		calendar1.setTime(sampai.getValue());
		calendar1.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);

		Session session = HibernateUtil.currentSession();
		List<Long> groupUp = session.createCriteria(Transaksi.class)
				.add(akuns.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("akun.id", akuns))
				.createAlias("grupTransaksi", "grupTransaksi")
				.add(Restrictions.between("grupTransaksi.tanggalTransaksi", calendar.getTime(), calendar1.getTime()))
				.add(Restrictions.isNotNull("akun")).add(Restrictions.isNotNull("postingHistory"))
				.setProjection(Projections.groupProperty("grupTransaksi.id")).list();
		if (groupUp.isEmpty()) {
			groupUp.add(-1L);
		}
		parameters.put("groupUp", groupUp.toArray());

		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			// akunting/jurnal_harian_ket

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "akunting/jurnal_harian_ket",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Jurnal Harian", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
