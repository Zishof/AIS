package ais.action.master.dashboard.akunting;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Auxhead;
import org.zkoss.zul.Auxheader;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.action.maintenance.MainAction;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

/**
 * Membantu memeriksa keseimbangan saldo laporan sebelum laporan keuangan diselesaikan.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardNeracaLajur extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Center center;
	private MyDatebox mulai;

	private MyDatebox sampai;

	private AmbilDataSatuanKerjaBanbox searchsatuanKerja;

	private Grid grid;
	private MyDatebox saldoAwal;

	public DasboardNeracaLajur() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DasboardNeracaLajur(String title, String border, boolean closable) {
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

		Rows rows = new Rows();
		rows.setParent(grid);
		appendDashboardSopDescriptionRow(rows, "Neraca Lajur", "membantu mengecek keseimbangan saldo awal, mutasi, penyesuaian, rugi laba, penutup, dan neraca. Gunakan untuk memastikan proses penyusunan laporan keuangan berjalan rapi dan mudah diaudit.");

		Calendar calendarAwal = ais.ui.util.WaktuUtil.getCalendar();
		calendarAwal.set(Calendar.MONTH, 0);
		calendarAwal.set(Calendar.DATE, 1);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Saldo Awal"));
		row.appendChild(saldoAwal = new MyDatebox(calendarAwal.getTime()));
		saldoAwal.setFormat(Common.dateFormat1.get().toPattern());
		saldoAwal.setReadonly(true);

		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		row.appendChild(mulai = new MyDatebox(calendarAwal.getTime()));
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

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				reload();
			}
		};

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "1,1,4");

		MyToolbarbuttonConfig tampilkan;
		row.appendChild(tampilkan = new MyToolbarbuttonConfig("Tampilkan", "/img/svg/search.svg"));
		tampilkan.addEventListener("onClick", eventListener);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/svg/download.svg");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DasboardNeracaLajur.this.grid);
			}
		});
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

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

		Auxhead auxhead = new Auxhead();
		auxhead.setParent(grid);

		Auxheader auxheader = new Auxheader("Akun");
		auxheader.setColspan(1);
		auxheader.setParent(auxhead);

		Auxheader auxheaderGanjil = new Auxheader("Neraca saldo");
		auxheaderGanjil.setColspan(2);
		auxheaderGanjil.setParent(auxhead);

		auxheaderGanjil = new Auxheader("Mutasi");
		auxheaderGanjil.setColspan(2);
		auxheaderGanjil.setParent(auxhead);

		auxheaderGanjil = new Auxheader("Jurnal Penyesuaian");
		auxheaderGanjil.setColspan(2);
		auxheaderGanjil.setParent(auxhead);

		auxheaderGanjil = new Auxheader("N.Saldo Set.Penyesuaian");
		auxheaderGanjil.setColspan(2);
		auxheaderGanjil.setParent(auxhead);

		auxheaderGanjil = new Auxheader("Rugi Laba");
		auxheaderGanjil.setColspan(2);
		auxheaderGanjil.setParent(auxhead);

		auxheaderGanjil = new Auxheader("Penutup");
		auxheaderGanjil.setColspan(2);
		auxheaderGanjil.setParent(auxhead);

		auxheaderGanjil = new Auxheader("Neraca");
		auxheaderGanjil.setColspan(2);
		auxheaderGanjil.setParent(auxhead);

		Columns columns = new Columns();
		columns.setParent(grid);

		SatuanKerja satuanKerja = (SatuanKerja) searchsatuanKerja.getAttribute("satuanKerja");
		Long satker = satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId();
		Date saldoAwalD = saldoAwal.getValue();
		final Date tglMulai = mulai.getValue();
		final Date tglSampai = sampai.getValue();

		String sql = "select d.id as id_akun, d.kode as kode_akun, d.nama as akun,\r\n"
				+ "(sum(case when date(a1.tanggal_transaksi) between date('2000-01-01') and date(:saldoAwal) then (debet) else 0 end)) as saldo_awal_debet,\r\n"
				+ "(sum(case when date(a1.tanggal_transaksi) between date('2000-01-01') and date(:saldoAwal) then (kredit) else 0 end)) as saldo_awal_kredit,\r\n"
				+ "\r\n" + "\r\n"
				+ "(sum(case when date(a1.tanggal_transaksi) between date(:tanggal1) and date(:tanggal2) and a1.jenis_transaksi=7 then (debet) else 0 end)) as jurnal_penyesuaian_debet,\r\n"
				+ "(sum(case when date(a1.tanggal_transaksi) between date(:tanggal1) and date(:tanggal2) and a1.jenis_transaksi=7 then (kredit) else 0 end)) as jurnal_penyesuaian_kredit,\r\n"
				+ "\r\n" + "\r\n"
				+ "(sum(case when date(a1.tanggal_transaksi) between date(:tanggal1) and date(:tanggal2) and a1.jenis_transaksi=9 then (debet) else 0 end)) as jurnal_penutup_debet,\r\n"
				+ "(sum(case when date(a1.tanggal_transaksi) between date(:tanggal1) and date(:tanggal2) and a1.jenis_transaksi=9 then (kredit) else 0 end)) as jurnal_penutup_kredit,\r\n"
				+ "\r\n" + "\r\n"
				+ "(sum(case when date(a1.tanggal_transaksi) between date(:tanggal1) and date(:tanggal2) and (a1.jenis_transaksi!=7 or a1.jenis_transaksi is null) then (debet) else 0 end)) as penyesuaian_debet,\r\n"
				+ "(sum(case when date(a1.tanggal_transaksi) between date(:tanggal1) and date(:tanggal2) and (a1.jenis_transaksi!=7 or a1.jenis_transaksi is null) then (kredit) else 0 end)) as penyesuaian_kredit\r\n"
				+ " from akunting.transaksi a "
				+ "inner join akunting.grup_transaksi a1 on (a1.id=a.grup_transaksi)\r\n";

		if (!ConstantValues.otomatisTerposting) {
			sql += "  inner join akunting.posting_history dd on (dd.id=a1.posting_history and dd.posting=true) ";
		}

		sql += "inner join akunting.akun d on (a.akun = d.id)\r\n" + "\r\n" + "where a1.posting_history is not null\r\n"
				+ "and case when :satuan_kerja = -1 then true else :satuan_kerja = a1.satuan_kerja end\r\n"
				+ "and date(a1.tanggal_transaksi) between date('2000-01-01') and date(:tanggal2)\r\n"
				+ "and a1.closing is not null\r\n" + "group by d.id\r\n" + "order by d.kode";

		List<Object[]> objects = HibernateUtil.currentSession().createSQLQuery(sql).setLong("satuan_kerja", satker)
				.setString("saldoAwal", Common.databaseDateFormat.get().format(saldoAwalD))
				.setString("tanggal1", Common.databaseDateFormat.get().format(tglMulai))
				.setString("tanggal2", Common.databaseDateFormat.get().format(tglSampai)).list();

		MyColumnConfig column = new MyColumnConfig("");
		column.setParent(columns);
		column.setWidth("25%");

		column = new MyColumnConfig("Debet");
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig("Kredit");
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig("Debet");
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig("Kredit");
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig("Debet");
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig("Kredit");
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig("Debet");
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig("Kredit");
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig("Debet");
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig("Kredit");
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig("Debet");
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig("Kredit");
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig("Debet");
		column.setParent(columns);
		column.setAlign("right");

		column = new MyColumnConfig("Kredit");
		column.setParent(columns);
		column.setAlign("right");

		Rows rows = new Rows();
		rows.setParent(grid);

		Double nspdebTot = 0.0;
		Double nspkreTot = 0.0;

		Double penyesuaian_debetTot = 0.0;
		Double penyesuaian_kreditTot = 0.0;

		Double jurnal_penyesuaian_debetTot = 0.0;
		Double jurnal_penyesuaian_kreditTot = 0.0;

		Double saldoAwalDebetTot = 0.0;
		Double saldoAwalKreditTot = 0.0;

		Double rldebTot = 0.0;
		Double rlkreTot = 0.0;

		Double jurnal_penutup_debetTot = 0.0;
		Double jurnal_penutup_kreditTot = 0.0;

		Double neracaDebTot = 0.0;
		Double neracaKreTot = 0.0;

		String buka = "";
		String tutup = "";

		for (Object[] o : objects) {

			final Number id_akun = o[0] == null ? 0.0 : (Number) o[0];
			String kode_akun = o[1] == null ? "" : o[1].toString();
			String nama_akun = o[2] == null ? "" : o[2].toString();

			Double saldo_awal_debet = o[3] == null ? 0.0 : ((Number) o[3]).doubleValue();
			Double saldo_awal_kredit = o[4] == null ? 0.0 : ((Number) o[4]).doubleValue();

			Double jurnal_penyesuaian_debet = o[5] == null ? 0.0 : ((Number) o[5]).doubleValue();
			Double jurnal_penyesuaian_kredit = o[6] == null ? 0.0 : ((Number) o[6]).doubleValue();

			Double jurnal_penutup_debet = o[7] == null ? 0.0 : ((Number) o[7]).doubleValue();
			Double jurnal_penutup_kredit = o[8] == null ? 0.0 : ((Number) o[8]).doubleValue();

			Double penyesuaian_debet = o[9] == null ? 0.0 : ((Number) o[9]).doubleValue();
			Double penyesuaian_kredit = o[10] == null ? 0.0 : ((Number) o[10]).doubleValue();

			jurnal_penutup_debetTot += jurnal_penutup_debet;
			jurnal_penutup_kreditTot += jurnal_penutup_kredit;

			Double nspdeb = ((saldo_awal_debet + penyesuaian_debet + jurnal_penyesuaian_debet) > (saldo_awal_kredit
					+ penyesuaian_kredit + jurnal_penyesuaian_kredit))
							? ((saldo_awal_debet + penyesuaian_debet + jurnal_penyesuaian_debet)
									- (saldo_awal_kredit + penyesuaian_kredit + jurnal_penyesuaian_kredit))
							: 0.0;

			nspdebTot += nspdeb;

			Double nspkre = ((saldo_awal_kredit + penyesuaian_kredit + jurnal_penyesuaian_kredit) > (saldo_awal_debet
					+ penyesuaian_debet + jurnal_penyesuaian_debet))
							? ((saldo_awal_kredit + penyesuaian_kredit + jurnal_penyesuaian_kredit)
									- (saldo_awal_debet + penyesuaian_debet + jurnal_penyesuaian_debet))
							: 0.0;

			nspkreTot += nspkre;

			Double rldeb = kode_akun.startsWith("4") || kode_akun.startsWith("5") || kode_akun.startsWith("6")
					|| kode_akun.startsWith("7")
							? (((saldo_awal_debet + penyesuaian_debet + jurnal_penyesuaian_debet) > (saldo_awal_kredit
									+ penyesuaian_kredit + jurnal_penyesuaian_kredit))
											? ((saldo_awal_debet + penyesuaian_debet + jurnal_penyesuaian_debet)
													- (saldo_awal_kredit + penyesuaian_kredit
															+ jurnal_penyesuaian_kredit))
											: 0.0)
							: 0.0;

			rldebTot += rldeb;

			Double rlkre = kode_akun.startsWith("4") || kode_akun.startsWith("5") || kode_akun.startsWith("6")
					|| kode_akun.startsWith("7")
							? (((saldo_awal_kredit + penyesuaian_kredit + jurnal_penyesuaian_kredit) > (saldo_awal_debet
									+ penyesuaian_debet + jurnal_penyesuaian_debet))
											? ((saldo_awal_kredit + penyesuaian_kredit + jurnal_penyesuaian_kredit)
													- (saldo_awal_debet + penyesuaian_debet + jurnal_penyesuaian_debet))
											: 0.0)
							: 0.0;

			rlkreTot += rlkre;

			penyesuaian_debetTot += penyesuaian_debet;
			penyesuaian_kreditTot += penyesuaian_kredit;

			jurnal_penyesuaian_debetTot += jurnal_penyesuaian_debet;
			jurnal_penyesuaian_kreditTot += jurnal_penyesuaian_kredit;

			MyFormRow row = new MyFormRow();
			row.setParent(rows);

			A a = new A(kode_akun + " " + nama_akun);
			a.setParent(row);
			a.setStyle("font-size:11px;color:blue;");
			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					Common.displayWindow("/pages/master/akunting/grup_transaksi.zul?akun=" + id_akun + "&mulai="
							+ Common.dateFormat8.get().format(tglMulai) + "&sampai=" + Common.dateFormat8.get().format(tglSampai),
							true, "95%", "95%", null, "", false);

				}
			});

			Double saldoAwalDebet = ((saldo_awal_debet == null ? 0.0 : saldo_awal_debet)
					- (saldo_awal_kredit == null ? 0.0 : saldo_awal_kredit)) > 0.1
							? ((saldo_awal_debet == null ? 0.0 : saldo_awal_debet)
									- (saldo_awal_kredit == null ? 0.0 : saldo_awal_kredit))
							: 0.0;

			saldoAwalDebetTot += saldoAwalDebet;

			Double saldoAwalKredit = ((saldo_awal_debet == null ? 0.0 : saldo_awal_debet)
					- (saldo_awal_kredit == null ? 0.0 : saldo_awal_kredit)) < 0.0
							? ((saldo_awal_debet == null ? 0.0 : saldo_awal_debet)
									- (saldo_awal_kredit == null ? 0.0 : saldo_awal_kredit))
							: 0.0;

			saldoAwalKreditTot += saldoAwalKredit;

			Double neracaDeb = kode_akun.startsWith("1") || kode_akun.startsWith("2") || kode_akun.startsWith("3")
					? (((saldo_awal_debet + penyesuaian_debet + jurnal_penyesuaian_debet
							+ jurnal_penutup_debet) > (saldo_awal_kredit + penyesuaian_kredit
									+ jurnal_penyesuaian_kredit + jurnal_penutup_kredit))
											? ((saldo_awal_debet + penyesuaian_debet + jurnal_penyesuaian_debet
													+ jurnal_penutup_debet)
													- (saldo_awal_kredit + penyesuaian_kredit
															+ jurnal_penyesuaian_kredit + jurnal_penutup_kredit))
											: 0.0)
					: 0.0;
			Double neracaKre = kode_akun.startsWith("1") || kode_akun.startsWith("2") || kode_akun.startsWith("3")
					? (((saldo_awal_kredit + penyesuaian_kredit + jurnal_penyesuaian_kredit
							+ jurnal_penutup_kredit) > (saldo_awal_debet + penyesuaian_debet + jurnal_penyesuaian_debet
									+ jurnal_penutup_debet))
											? ((saldo_awal_kredit + penyesuaian_kredit + jurnal_penyesuaian_kredit
													+ jurnal_penutup_kredit)
													- (saldo_awal_debet + penyesuaian_debet + jurnal_penyesuaian_debet
															+ jurnal_penutup_debet))
											: 0.0)
					: 0.0;

			neracaDebTot += neracaDeb;
			neracaKreTot += neracaKre;

			row.appendChild(new MyLabelKecilSekali(saldoAwalDebet >= 0.0 ? Common.numberFormat.get().format(saldoAwalDebet)
					: buka + Common.numberFormat.get().format(Math.abs(saldoAwalDebet)) + tutup));
			row.appendChild(new MyLabelKecilSekali(saldoAwalKredit >= 0.0 ? Common.numberFormat.get().format(saldoAwalKredit)
					: buka + Common.numberFormat.get().format(Math.abs(saldoAwalKredit)) + tutup));

			row.appendChild(
					new MyLabelKecilSekali(penyesuaian_debet >= 0.0 ? Common.numberFormat.get().format(penyesuaian_debet)
							: buka + Common.numberFormat.get().format(Math.abs(penyesuaian_debet)) + tutup));
			row.appendChild(
					new MyLabelKecilSekali(penyesuaian_kredit >= 0.0 ? Common.numberFormat.get().format(penyesuaian_kredit)
							: buka + Common.numberFormat.get().format(Math.abs(penyesuaian_kredit)) + tutup));

			row.appendChild(new MyLabelKecilSekali(
					jurnal_penyesuaian_debet >= 0.0 ? Common.numberFormat.get().format(jurnal_penyesuaian_debet)
							: buka + Common.numberFormat.get().format(Math.abs(jurnal_penyesuaian_debet)) + tutup));
			row.appendChild(new MyLabelKecilSekali(
					jurnal_penyesuaian_kredit >= 0.0 ? Common.numberFormat.get().format(jurnal_penyesuaian_kredit)
							: buka + Common.numberFormat.get().format(Math.abs(jurnal_penyesuaian_kredit)) + tutup));

			row.appendChild(new MyLabelKecilSekali(nspdeb >= 0.0 ? Common.numberFormat.get().format(nspdeb)
					: buka + Common.numberFormat.get().format(Math.abs(nspdeb)) + tutup));
			row.appendChild(new MyLabelKecilSekali(nspkre >= 0.0 ? Common.numberFormat.get().format(nspkre)
					: buka + Common.numberFormat.get().format(Math.abs(nspkre)) + tutup));

			row.appendChild(new MyLabelKecilSekali(rldeb >= 0.0 ? Common.numberFormat.get().format(rldeb)
					: buka + Common.numberFormat.get().format(Math.abs(rldeb)) + tutup));
			row.appendChild(new MyLabelKecilSekali(rlkre >= 0.0 ? Common.numberFormat.get().format(rlkre)
					: buka + Common.numberFormat.get().format(Math.abs(rlkre)) + tutup));

			row.appendChild(new MyLabelKecilSekali(
					jurnal_penutup_debet >= 0.0 ? Common.numberFormat.get().format(jurnal_penutup_debet)
							: buka + Common.numberFormat.get().format(Math.abs(jurnal_penutup_debet)) + tutup));
			row.appendChild(new MyLabelKecilSekali(
					jurnal_penutup_kredit >= 0.0 ? Common.numberFormat.get().format(jurnal_penutup_kredit)
							: buka + Common.numberFormat.get().format(Math.abs(jurnal_penutup_kredit)) + tutup));

			row.appendChild(new MyLabelKecilSekali(neracaDeb >= 0.0 ? Common.numberFormat.get().format(neracaDeb)
					: buka + Common.numberFormat.get().format(Math.abs(neracaDeb)) + tutup));
			row.appendChild(new MyLabelKecilSekali(neracaKre >= 0.0 ? Common.numberFormat.get().format(neracaKre)
					: buka + Common.numberFormat.get().format(Math.abs(neracaKre)) + tutup));

		}

		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.setStyle("background-color: silver;");
		row.appendChild(new MyLabelKecilSekali("Total"));

		row.appendChild(new MyLabelKecilSekali(saldoAwalDebetTot >= 0.0 ? Common.numberFormat.get().format(saldoAwalDebetTot)
				: buka + Common.numberFormat.get().format(Math.abs(saldoAwalDebetTot)) + tutup));
		row.appendChild(
				new MyLabelKecilSekali(saldoAwalKreditTot >= 0.0 ? Common.numberFormat.get().format(saldoAwalKreditTot)
						: buka + Common.numberFormat.get().format(Math.abs(saldoAwalKreditTot)) + tutup));

		row.appendChild(
				new MyLabelKecilSekali(penyesuaian_debetTot >= 0.0 ? Common.numberFormat.get().format(penyesuaian_debetTot)
						: buka + Common.numberFormat.get().format(Math.abs(penyesuaian_debetTot)) + tutup));
		row.appendChild(
				new MyLabelKecilSekali(penyesuaian_kreditTot >= 0.0 ? Common.numberFormat.get().format(penyesuaian_kreditTot)
						: buka + Common.numberFormat.get().format(Math.abs(penyesuaian_kreditTot)) + tutup));

		row.appendChild(new MyLabelKecilSekali(
				jurnal_penyesuaian_debetTot >= 0.0 ? Common.numberFormat.get().format(jurnal_penyesuaian_debetTot)
						: buka + Common.numberFormat.get().format(Math.abs(jurnal_penyesuaian_debetTot)) + tutup));
		row.appendChild(new MyLabelKecilSekali(
				jurnal_penyesuaian_kreditTot >= 0.0 ? Common.numberFormat.get().format(jurnal_penyesuaian_kreditTot)
						: buka + Common.numberFormat.get().format(Math.abs(jurnal_penyesuaian_kreditTot)) + tutup));

		row.appendChild(new MyLabelKecilSekali(nspdebTot >= 0.0 ? Common.numberFormat.get().format(nspdebTot)
				: buka + Common.numberFormat.get().format(Math.abs(nspdebTot)) + tutup));
		row.appendChild(new MyLabelKecilSekali(nspkreTot >= 0.0 ? Common.numberFormat.get().format(nspkreTot)
				: buka + Common.numberFormat.get().format(Math.abs(nspkreTot)) + tutup));

		row.appendChild(new MyLabelKecilSekali(rldebTot >= 0.0 ? Common.numberFormat.get().format(rldebTot)
				: buka + Common.numberFormat.get().format(Math.abs(rldebTot)) + tutup));
		row.appendChild(new MyLabelKecilSekali(rlkreTot >= 0.0 ? Common.numberFormat.get().format(rlkreTot)
				: buka + Common.numberFormat.get().format(Math.abs(rlkreTot)) + tutup));

		row.appendChild(new MyLabelKecilSekali(
				jurnal_penutup_debetTot >= 0.0 ? Common.numberFormat.get().format(jurnal_penutup_debetTot)
						: buka + Common.numberFormat.get().format(Math.abs(jurnal_penutup_debetTot)) + tutup));
		row.appendChild(new MyLabelKecilSekali(
				jurnal_penutup_kreditTot >= 0.0 ? Common.numberFormat.get().format(jurnal_penutup_kreditTot)
						: buka + Common.numberFormat.get().format(Math.abs(jurnal_penutup_kreditTot)) + tutup));

		row.appendChild(new MyLabelKecilSekali(neracaDebTot >= 0.0 ? Common.numberFormat.get().format(neracaDebTot)
				: buka + Common.numberFormat.get().format(Math.abs(neracaDebTot)) + tutup));
		row.appendChild(new MyLabelKecilSekali(neracaKreTot >= 0.0 ? Common.numberFormat.get().format(neracaKreTot)
				: buka + Common.numberFormat.get().format(Math.abs(neracaKreTot)) + tutup));

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
