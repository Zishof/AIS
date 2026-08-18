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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupAkun;
import ais.database.model.akunting.Transaksi;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanBukuBesarPerTanggal extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;

	private Toolbar toolbar;

	private MyDatebox mulai;

	private MyDatebox sampai;

	protected Rows rowsAkun;

	private String pencarianAkun = "";

	private AmbilDataSatuanKerjaBanbox searchsatuanKerja;

	private MyTextbox bukti;

	public LaporanBukuBesarPerTanggal() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Buku Besar Per Tanggal", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanBukuBesarPerTanggal(String title, String border, boolean closable) throws Exception {
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

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Periode Mulai"));
		row.appendChild(mulai = new MyDatebox(calendar.getTime()));
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Bukti"));
		row.appendChild(bukti = new MyTextbox());
		bukti.setWidth("90%");

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
				List<Akun> akuns = session.createCriteria(Transaksi.class).createAlias("grupTransaksi", "grupTransaksi")
						.add(bukti.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("grupTransaksi.kode", bukti.getValue().trim(), MatchMode.ANYWHERE))

						.add(Restrictions.sqlRestriction("date(this_.tanggal_transaksi) between date('"
								+ Common.databaseDateFormat.get().format(mulai.getValue()) + "') and date('"
								+ Common.databaseDateFormat.get().format(sampai.getValue()) + "')"))
						.add(Restrictions.isNotNull("akun")).add(Restrictions.isNotNull("postingHistory"))
						.setProjection(Projections.groupProperty("akun")).createAlias("akun", "akun")

						.add(grupAkunData == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("akun.grupAkun", grupAkunData))

						.add(cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : crit).list();
				Collections.sort(akuns);
				for (Akun akun : akuns) {
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rowsAkun);
					Checkbox checkbox = new Checkbox(akun.getKode() + " - " + akun.getNama());
					checkbox.setChecked(checkboxSemua.isChecked());
					checkbox.setAttribute("akun", akun);
					checkbox.setParent(row);
					row.setValign("top");
					row.setAttribute("checkbox", checkbox);
				}

			}
		};

		cariAkun.onEvent(null);
		cari.addEventListener("onOK", cariAkun);

		Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
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

			}
		});

		mulai.addEventListener("onChange", cariAkun);
		sampai.addEventListener("onChange", cariAkun);
		bukti.addEventListener("onChange", cariAkun);

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
		}, "akunting/per_tanggal_jurnal_buku_besar", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				cariAkun.onEvent(arg0);
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

		List<Long> akuns = new ArrayList<Long>();

		List<Row> myRows = rowsAkun.getChildren();
		for (Row row : myRows) {
			Checkbox checkbox = (Checkbox) row.getAttribute("checkbox");
			if (checkbox.isChecked()) {
				Akun akun = (Akun) checkbox.getAttribute("akun");
				akuns.add(akun.getId());
			}
		}
		if (akuns.isEmpty()) {
			akuns.add(-1L);
		}
		parameters.put("bukti", bukti.getValue().trim());
		parameters.put("akuns", akuns.toArray());
		SatuanKerja satuanKerja = (SatuanKerja) searchsatuanKerja.getAttribute("satuanKerja");
		parameters.put("satuan_kerja", satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId());
		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
					"akunting/per_tanggal_jurnal_buku_besar", ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Buku Besar Per Tanggal", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
