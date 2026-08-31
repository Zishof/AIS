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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
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
import ais.database.model.akunting.GrupAkun;
import ais.database.model.akunting.Transaksi;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.EcampusUtil;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MySpreadsheet;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan buku besar. Kelas ini mengubah data domain menjadi bentuk
 * laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke lapisan
 * report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Toolbar toolbar}, {@code MyDatebox
 * mulai}, {@code MyDatebox sampai}, {@code Rows rowsAkun}, {@code String pencarianAkun}, {@code
 * AmbilDataSatuanKerjaBanbox searchsatuanKerja}, {@code MyTextbox bukti}, {@code MyTabConfig tab51};
 * inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor ({@code onReport()}); operasi domain lain ({@code
 * generateParameter()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanBukuBesar extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Toolbar toolbar;

	private MyDatebox mulai;

	private MyDatebox sampai;

	protected Rows rowsAkun;

	private String pencarianAkun = "";

	private AmbilDataSatuanKerjaBanbox searchsatuanKerja;

	private MyTextbox bukti;

	private MyTabConfig tab51;

	private MyTabConfig tab1;

	private Tabpanel tabpanel1;

	private Tabpanel tabpanel51;

	private Center centera = null;

	private Center centera1;

	private MyDatebox saldoAwal;

	public LaporanBukuBesar() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Buku Besar", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanBukuBesar(String title, String border, boolean closable) throws Exception {
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

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		tab1 = new MyTabConfig("Buku Besar Per Transaksi");
		tab1.setParent(tabs);

		tab51 = new MyTabConfig("Buku Besar Excel");
		tab51.setParent(tabs);

		tab1.addEventListener("onClick", eventListener);
		tab51.addEventListener("onClick", eventListener);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		tabpanel51 = new ais.ui.util.MyTabpanel();
		tabpanel51.setParent(tabpanels);

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
		row.setVisible(false);
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Saldo Awal"));
		row.appendChild(saldoAwal = new MyDatebox());
		saldoAwal.setFormat(Common.dateFormat1.get().toPattern());

		String saldoAwalDefault = Common.getKonfigurasi("saldo_awal_default", "").getNilai().trim();
		if (!saldoAwalDefault.isEmpty()) {
			try {
				saldoAwal.setValue(Common.dateFormat1.get().parse(saldoAwalDefault));
				row.setVisible(true);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akunting/LaporanBukuBesar.java:225");
				// TODO: handle exception
			}
		}

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
				akunsSelected.clear();
			}
		});

		mulai.addEventListener("onChange", cariAkun);
		sampai.addEventListener("onChange", cariAkun);
		bukti.addEventListener("onChange", cariAkun);

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
		}, "akunting/jurnal_buku_besar", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel1);

		centera = new Center();
		centera.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(centera, true);

		borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel51);

		centera1 = new Center();
		centera1.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(centera1, true);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				cariAkun.onEvent(arg0);
				onReport(null);
			}
		});

	}

	private List<Long> akunsSelected = new ArrayList<Long>();

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

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("mulai", Common.databaseDateFormat.get().format(tglMulai));
		parameters.put("sampai", Common.databaseDateFormat.get().format(tglSampai));
		parameters.put("mulai_1", Common.dateFormat5.get().format(tglMulai));
		parameters.put("sampai_1", Common.dateFormat5.get().format(tglSampai));

		parameters.put("saldo_awal",
				saldoAwal.getValue() == null ? "-1" : Common.databaseDateFormat.get().format(saldoAwal.getValue()));
		parameters.put("saldo_awal_1",
				saldoAwal.getValue() == null ? "" : Common.dateFormat5.get().format(saldoAwal.getValue()));

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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onReport(Event event) {

		try {

			if (tab1.isSelected()) {

				Common.clear(centera);

				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "akunting/jurnal_buku_besar",
						ais.ui.util.WaktuUtil.getDate(), toolbar);
				CommonReport.tampilkanReportPDF(centera, file);
			} else if (tab51.isSelected()) {

				SatuanKerja satuanKerja = (SatuanKerja) searchsatuanKerja.getAttribute("satuanKerja");
				Long satker = satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId();
				Date tglMulai = mulai.getValue();
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
						+ "a.debet, a.kredit, b.kode   "
						+ "from akunting.transaksi a inner join akunting.akun b on (a.akun = b.id)  left join ( "
						+ "  select sum(debet-kredit) as saldo_awal,aa.akun from akunting.transaksi aa "
						+ "  inner join akunting.grup_transaksi cc on (cc.id=aa.grup_transaksi) ";

				if (!ConstantValues.otomatisTerposting) {
					sql += "  inner join akunting.posting_history dd on (dd.id=cc.posting_history and dd.posting=true) ";
				}

				sql += "  where date(aa.tanggal_transaksi)<date(:mulai) "
						+ "  and cc.posting_history is not null and aa.akun is not null "
						+ "  and case when :bukti='' then true else cc.kode ilike :buktilike end " + "  and case when "
						+ satker + " = -1 then true else " + satker + "=cc.satuan_kerja end "
						+ (sqlAkun.isEmpty() ? " and false " : " and aa.akun in (" + sqlAkun + ") ")
						+ "  group by aa.akun ) aaa on (aaa.akun = a.akun)  "
						+ "inner join akunting.grup_transaksi c on (c.id=a.grup_transaksi)   "
						+ "where c.posting_history is not null and a.akun is not null "
						+ "and case when :bukti='' then true else c.kode ilike :buktilike end  and case when " + satker
						+ " = -1 then true else " + satker + "=c.satuan_kerja end "
						+ (sqlAkun.isEmpty() ? " and false " : " and a.akun in (" + sqlAkun + ") ")
						+ "and date(c.tanggal_transaksi) between date(:mulai) and date(:sampai) "
						+ "order by b.kode,c.tanggal_transaksi";

				System.out.println(sql);

				List<Object[]> objects = HibernateUtil.currentSession().createSQLQuery(sql)
						.setString("bukti", bukti.getValue().trim())
						.setString("buktilike", "%" + bukti.getValue().trim() + "%")
						.setString("mulai", Common.databaseDateFormat.get().format(tglMulai))
						.setString("sampai", Common.databaseDateFormat.get().format(tglSampai)).list();

				List<List> datas = new ArrayList<List>();

				ArrayList sub = new ArrayList();

				sub.add("**No. Bukti");
				sub.add("**Tanggal dan Waktu");
				sub.add("**Keterangan");
				sub.add("**Debet");
				sub.add("**Kredit");
				sub.add("**Saldo");

				datas.add(sub);

				String kodeAkun = "";
				String kodeNama = "";
				Double saldoAkhir = 0.0;
				Double totalSaldoAkhir = 0.0;
				for (Object[] o : objects) {
					String kode_transaksi = o[0] == null ? "" : o[0].toString();
					Date tanggal_transaksi = o[1] == null ? null : (Date) o[1];

					String kode_akun = o[2] == null ? "" : o[2].toString();
					String nama_akun = o[3] == null ? "" : o[3].toString();

					String keterangan_transaksi = o[4] == null ? "" : o[4].toString();
					Number saldo_awal = o[5] == null ? 0.0 : (Number) o[5];
					Number debet = o[6] == null ? 0.0 : (Number) o[6];
					Number kredit = o[7] == null ? 0.0 : (Number) o[7];

					if (!kodeAkun.equalsIgnoreCase(kode_akun)) {

						if (!kodeAkun.isEmpty()) {
							sub = new ArrayList();
							sub.add("**");
							sub.add("**Total " + kodeAkun + " " + kodeNama);
							sub.add("**");
							sub.add("**");
							sub.add("**");
							sub.add("**" + (totalSaldoAkhir >= 0.0 ? Common.numberFormat.get().format(totalSaldoAkhir)
									: "(" + Common.numberFormat.get().format(Math.abs(totalSaldoAkhir)) + ")"));

							datas.add(sub);
						}

						sub = new ArrayList();
						sub.add("**" + kode_akun);
						sub.add("**" + nama_akun);
						sub.add("**");
						sub.add("**");
						sub.add("**");
						try {
							sub.add("**" + (saldo_awal.doubleValue() >= 0.0
									? Common.numberFormat.get().format(saldo_awal.doubleValue())
									: "(" + Common.numberFormat.get().format(Math.abs(saldo_awal.doubleValue()) + ")")));
						} catch (Exception e) {
							String s = saldo_awal.toString();
							sub.add("**" + s);
						}

						datas.add(sub);

						kodeAkun = kode_akun;
						kodeNama = nama_akun;
						saldoAkhir = 0.0;
					}

					saldoAkhir += (debet.doubleValue() - kredit.doubleValue());

					try {
						totalSaldoAkhir = saldo_awal.doubleValue() + saldoAkhir;
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akunting/LaporanBukuBesar.java:613");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Buku Besar", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}

					sub = new ArrayList();

					sub.add(kode_transaksi);
					sub.add(tanggal_transaksi == null ? "" : Common.dateFormat51.get().format(tanggal_transaksi));
					sub.add(keterangan_transaksi);
					sub.add(debet.doubleValue() >= 0.0 ? Common.numberFormat.get().format(debet.doubleValue())
							: "(" + Common.numberFormat.get().format(Math.abs(debet.doubleValue())) + ")");
					sub.add(kredit.doubleValue() >= 0.0 ? Common.numberFormat.get().format(kredit.doubleValue())
							: "(" + Common.numberFormat.get().format(Math.abs(kredit.doubleValue())) + ")");
					sub.add("**" + (totalSaldoAkhir >= 0.0 ? Common.numberFormat.get().format(totalSaldoAkhir)
							: "(" + Common.numberFormat.get().format(Math.abs(totalSaldoAkhir)) + ")"));
					datas.add(sub);
				}

				if (!kodeAkun.isEmpty()) {
					sub = new ArrayList();
					sub.add("**");
					sub.add("**Total " + kodeAkun + " " + kodeNama);
					sub.add("**");
					sub.add("**");
					sub.add("**");
					sub.add("**" + (totalSaldoAkhir >= 0.0 ? Common.numberFormat.get().format(totalSaldoAkhir)
							: "(" + Common.numberFormat.get().format(Math.abs(totalSaldoAkhir)) + ")"));

					datas.add(sub);
				}

				MySpreadsheet excelku = new ais.ui.util.MySpreadsheet();
				Common.clear(centera1);
				centera1.appendChild(excelku);
				EcampusUtil.tampilkan(datas, excelku);
				// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(excelku);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Buku Besar", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
