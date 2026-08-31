package ais.action.master.dashboard.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.AbstractTreeModel;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

import ais.action.master.akunting.GrupTransaksiAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.JenisLaporan;
import ais.database.model.akunting.Transaksi;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.UIUtil;

/**
 * Komponen dashboard khusus untuk dashboard akunting. Kelas ini memilih variasi data atau tampilan
 * dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Double nilaiSaldo}, {@code List
 * columnHeadersAdding}, {@code EventListener dataAdding}; operasi domain lain ({@code display()}, {@code
 * appendDashboardSopDescriptionRow()}, {@code buildMiniInfoCard()}, {@code safeDashboardHtml()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class DashboardAkuntingHelper {

	public static Double nilaiSaldo = 0.0;
	public static List<String> columnHeadersAdding = new ArrayList<String>();
	static {
		columnHeadersAdding.add("Saldo Akhir");
	}

	public static EventListener dataAdding = new EventListener() {

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

	@SuppressWarnings("unchecked")
	public static void display(final Component parent, final SatuanKerja sk, final int year,
			final JenisLaporan jenisLaporan) {
		Common.clear(parent);
		Row rowUtamapalingAwal = Common.tampilanScroll1(parent);
		rowUtamapalingAwal.getGrid().setSclass("dgrid");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(rowUtamapalingAwal);
		toolbar.setWidth("100%");
		appendDashboardSopDescriptionRow((org.zkoss.zul.Rows) rowUtamapalingAwal.getParent(), "Komparasi Laporan Keuangan Bulanan", "Membandingkan saldo akhir setiap bulan pada tahun terpilih. Gunakan untuk melihat bulan mana yang mengalami perubahan paling besar dan menelusuri detail akun jika diperlukan.");

		toolbar.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		final Combobox tahun;
		toolbar.appendChild(tahun = new Combobox());

		for (int i = (year + 10); i > (year - 20); i--) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
			if (i == year) {
				tahun.setSelectedItem(comboitem);
			}
		}
		tahun.setCols(3);
		tahun.setReadonly(true);
		tahun.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				DashboardAkuntingHelper.display(parent, sk, (Integer) tahun.getSelectedItem().getValue(), jenisLaporan);
			}
		});

		Long satuan_kerja = sk == null || sk.getId() == null ? -1L : sk.getId();

		Session session = HibernateUtil.currentSession();

		String sql = "select\r\n f.id as urut_laporan,\r\n c.id as kelompok,\r\n"
				+ "c.urut as urut,\r\n max(f.keterangan) as laporan,\r\n" + "max((trim(e.nama))) as jenis_laporan1,\r\n"
				+ "max((trim(e.keterangan))) as jenis_laporan2,\r\n" + "max(c.keterangan) as kelompok_laporan,\r\n"
				+ "max(c.keterangan1) as kelompok_laporan1,\r\n d.kode as kode_akun,\r\n d.nama as nama_akun,\r\n";

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		for (int bln = 0; bln < 12; bln++) {

			calendar.set(Calendar.YEAR, year);
			calendar.set(Calendar.MONTH, bln);
			calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
			sql += "(sum(case when date(a1.tanggal_transaksi) <= date('"
					+ Common.databaseDateFormat.get().format(calendar.getTime())
					+ "') then (debet-kredit) else 0 end)) as saldo_akhir_" + bln + ", ";
		}

		sql += "d.id as id_akun  from akunting.transaksi a\r\n"
				+ "inner join akunting.grup_transaksi a1 on (a1.id=a.grup_transaksi)\r\n"
				+ "inner join akunting.kelompok_laporan_punya_akun b on (a.akun = b.akun)\r\n"
				+ "inner join akunting.kelompok_laporan c on (c.id = b.kelompok_laporan)\r\n"
				+ "inner join akunting.akun d on (a.akun = d.id)\r\n"
				+ "inner join akunting.master_grup_laporan e on (c.master_grup_laporan = e.id)\r\n"
				+ "inner join akunting.jenis_laporan f on (f.id = c.jenis_laporan)\r\n \r\n"
				+ "where (c.aktif is null or c.aktif)\r\n and a1.posting_history is not null\r\n";

		if (!satuan_kerja.equals(-1L)) {
			sql += "and a1.satuan_kerja = " + satuan_kerja;
		}
		sql += " and c.jenis_laporan=" + jenisLaporan.getId() + " \r\n"
				+ "and date(a1.tanggal_transaksi) between date('1970-01-01') and date('"
				+ Common.databaseDateFormat.get().format(calendar.getTime()) + "')\r\n" + "group by f.id,e.id,c.id,d.id\r\n"
				+ "order by f.id,e.nomor_urut,e.id,c.urut,c.id,max(b.nomorurut),urut, d.kode";

		List<Object[]> dataAkunting = session.createSQLQuery(sql).list();

		final TreeMap<String, Object[]> mapData = new TreeMap<String, Object[]>();

		int indexUrut = 89;
		for (Object[] objects : dataAkunting) {
			String jenis_laporan1 = objects[4] + "";
			String jenis_laporan2 = objects[5] + "";
			String kelompok_laporan = objects[6] + "";
			String kelompok_laporan1 = objects[7] + "";

			if (jenis_laporan1 == null || jenis_laporan1.trim().isEmpty()
					|| jenis_laporan1.trim().equalsIgnoreCase("null")) {
				jenis_laporan1 = jenisLaporan.getNama();
			}

			if (jenis_laporan2 == null || jenis_laporan2.trim().isEmpty()
					|| jenis_laporan2.trim().equalsIgnoreCase("null")) {
				jenis_laporan2 = jenis_laporan1;
			}

			if (kelompok_laporan == null || kelompok_laporan.trim().isEmpty()
					|| kelompok_laporan.trim().equalsIgnoreCase("null")) {
				kelompok_laporan = jenis_laporan2;
			}

			if (kelompok_laporan1 == null || kelompok_laporan1.trim().isEmpty()
					|| kelompok_laporan1.trim().equalsIgnoreCase("null")) {
				kelompok_laporan1 = kelompok_laporan;
			}

			String kode_akun = objects[8] + "";
			String nama_akun = objects[9] + "";

			Double saldo_akhir_1 = ((Number) objects[10]).doubleValue();
			Double saldo_akhir_2 = ((Number) objects[11]).doubleValue();
			Double saldo_akhir_3 = ((Number) objects[12]).doubleValue();
			Double saldo_akhir_4 = ((Number) objects[13]).doubleValue();
			Double saldo_akhir_5 = ((Number) objects[14]).doubleValue();
			Double saldo_akhir_6 = ((Number) objects[15]).doubleValue();
			Double saldo_akhir_7 = ((Number) objects[16]).doubleValue();
			Double saldo_akhir_8 = ((Number) objects[17]).doubleValue();
			Double saldo_akhir_9 = ((Number) objects[18]).doubleValue();
			Double saldo_akhir_10 = ((Number) objects[19]).doubleValue();
			Double saldo_akhir_11 = ((Number) objects[20]).doubleValue();
			Double saldo_akhir_12 = ((Number) objects[21]).doubleValue();

			Long id_akun = ((Number) objects[22]).longValue();

			String u1 = "000000" + indexUrut;

			String urutan = u1.substring(u1.length() - 4);

			String key = urutan + "-" + jenis_laporan1 + "__" + jenis_laporan2 + "__" + kelompok_laporan + "__"
					+ kelompok_laporan1 + "__" + kode_akun + " " + nama_akun;
			key = key.trim();
			mapData.put(key,
					new Object[] { kode_akun + " " + nama_akun, saldo_akhir_1, saldo_akhir_2, saldo_akhir_3,
							saldo_akhir_4, saldo_akhir_5, saldo_akhir_6, saldo_akhir_7, saldo_akhir_8, saldo_akhir_9,
							saldo_akhir_10, saldo_akhir_11, saldo_akhir_12, key, id_akun + "" });
			indexUrut++;
		}

		Set<String> keys = new HashSet<String>();
		for (Object[] objects : dataAkunting) {
			String jenis_laporan1 = objects[4] + "";
			String jenis_laporan2 = objects[5] + "";
			String kelompok_laporan = objects[6] + "";
			String kelompok_laporan1 = objects[7] + "";

			if (jenis_laporan1 == null || jenis_laporan1.trim().isEmpty()
					|| jenis_laporan1.trim().equalsIgnoreCase("null")) {
				jenis_laporan1 = jenisLaporan.getNama();
			}

			if (jenis_laporan2 == null || jenis_laporan2.trim().isEmpty()
					|| jenis_laporan2.trim().equalsIgnoreCase("null")) {
				jenis_laporan2 = jenis_laporan1;
			}

			if (kelompok_laporan == null || kelompok_laporan.trim().isEmpty()
					|| kelompok_laporan.trim().equalsIgnoreCase("null")) {
				kelompok_laporan = jenis_laporan2;
			}

			if (kelompok_laporan1 == null || kelompok_laporan1.trim().isEmpty()
					|| kelompok_laporan1.trim().equalsIgnoreCase("null")) {
				kelompok_laporan1 = kelompok_laporan;
			}

			Double saldo_akhir_1 = ((Number) objects[10]).doubleValue();
			Double saldo_akhir_2 = ((Number) objects[11]).doubleValue();
			Double saldo_akhir_3 = ((Number) objects[12]).doubleValue();
			Double saldo_akhir_4 = ((Number) objects[13]).doubleValue();
			Double saldo_akhir_5 = ((Number) objects[14]).doubleValue();
			Double saldo_akhir_6 = ((Number) objects[15]).doubleValue();
			Double saldo_akhir_7 = ((Number) objects[16]).doubleValue();
			Double saldo_akhir_8 = ((Number) objects[17]).doubleValue();
			Double saldo_akhir_9 = ((Number) objects[18]).doubleValue();
			Double saldo_akhir_10 = ((Number) objects[19]).doubleValue();
			Double saldo_akhir_11 = ((Number) objects[20]).doubleValue();
			Double saldo_akhir_12 = ((Number) objects[21]).doubleValue();

			Long id_akun = ((Number) objects[22]).longValue();

			String key = jenis_laporan1 + "__" + jenis_laporan2 + "__" + kelompok_laporan + "__" + kelompok_laporan1;
			key = key.trim();

			keys.add(key);

			String u1 = "000000" + keys.size();

			String urutan = u1.substring(u1.length() - 4);
			key = urutan + "-" + key;

			Object[] data = mapData.get(key);
			if (data == null) {
				data = new Object[] { kelompok_laporan1, saldo_akhir_1, saldo_akhir_2, saldo_akhir_3, saldo_akhir_4,
						saldo_akhir_5, saldo_akhir_6, saldo_akhir_7, saldo_akhir_8, saldo_akhir_9, saldo_akhir_10,
						saldo_akhir_11, saldo_akhir_12, key, id_akun + "" };
			} else {
				saldo_akhir_1 += (Double) data[1];
				saldo_akhir_2 += (Double) data[2];
				saldo_akhir_3 += (Double) data[3];
				saldo_akhir_4 += (Double) data[4];
				saldo_akhir_5 += (Double) data[5];
				saldo_akhir_6 += (Double) data[6];
				saldo_akhir_7 += (Double) data[7];
				saldo_akhir_8 += (Double) data[8];
				saldo_akhir_9 += (Double) data[9];
				saldo_akhir_10 += (Double) data[10];
				saldo_akhir_11 += (Double) data[11];
				saldo_akhir_12 += (Double) data[12];

				String idAkun = data[14] + "," + id_akun;

				data = new Object[] { kelompok_laporan1, saldo_akhir_1, saldo_akhir_2, saldo_akhir_3, saldo_akhir_4,
						saldo_akhir_5, saldo_akhir_6, saldo_akhir_7, saldo_akhir_8, saldo_akhir_9, saldo_akhir_10,
						saldo_akhir_11, saldo_akhir_12, key, idAkun };
			}
			mapData.put(key, data);

		}

		keys.clear();
		for (Object[] objects : dataAkunting) {
			String jenis_laporan1 = objects[4] + "";
			String jenis_laporan2 = objects[5] + "";
			String kelompok_laporan = objects[6] + "";

			if (jenis_laporan1 == null || jenis_laporan1.trim().isEmpty()
					|| jenis_laporan1.trim().equalsIgnoreCase("null")) {
				jenis_laporan1 = jenisLaporan.getNama();
			}

			if (jenis_laporan2 == null || jenis_laporan2.trim().isEmpty()
					|| jenis_laporan2.trim().equalsIgnoreCase("null")) {
				jenis_laporan2 = jenis_laporan1;
			}

			if (kelompok_laporan == null || kelompok_laporan.trim().isEmpty()
					|| kelompok_laporan.trim().equalsIgnoreCase("null")) {
				kelompok_laporan = jenis_laporan2;
			}

			Double saldo_akhir_1 = ((Number) objects[10]).doubleValue();
			Double saldo_akhir_2 = ((Number) objects[11]).doubleValue();
			Double saldo_akhir_3 = ((Number) objects[12]).doubleValue();
			Double saldo_akhir_4 = ((Number) objects[13]).doubleValue();
			Double saldo_akhir_5 = ((Number) objects[14]).doubleValue();
			Double saldo_akhir_6 = ((Number) objects[15]).doubleValue();
			Double saldo_akhir_7 = ((Number) objects[16]).doubleValue();
			Double saldo_akhir_8 = ((Number) objects[17]).doubleValue();
			Double saldo_akhir_9 = ((Number) objects[18]).doubleValue();
			Double saldo_akhir_10 = ((Number) objects[19]).doubleValue();
			Double saldo_akhir_11 = ((Number) objects[20]).doubleValue();
			Double saldo_akhir_12 = ((Number) objects[21]).doubleValue();

			Long id_akun = ((Number) objects[22]).longValue();

			String key = jenis_laporan1 + "__" + jenis_laporan2 + "__" + kelompok_laporan;
			key = key.trim();

			keys.add(key);

			String u1 = "000000" + keys.size();

			String urutan = u1.substring(u1.length() - 4);
			key = urutan + "-" + key;

			Object[] data = mapData.get(key);
			if (data == null) {
				data = new Object[] { kelompok_laporan, saldo_akhir_1, saldo_akhir_2, saldo_akhir_3, saldo_akhir_4,
						saldo_akhir_5, saldo_akhir_6, saldo_akhir_7, saldo_akhir_8, saldo_akhir_9, saldo_akhir_10,
						saldo_akhir_11, saldo_akhir_12, key, id_akun + "" };
			} else {
				saldo_akhir_1 += (Double) data[1];
				saldo_akhir_2 += (Double) data[2];
				saldo_akhir_3 += (Double) data[3];
				saldo_akhir_4 += (Double) data[4];
				saldo_akhir_5 += (Double) data[5];
				saldo_akhir_6 += (Double) data[6];
				saldo_akhir_7 += (Double) data[7];
				saldo_akhir_8 += (Double) data[8];
				saldo_akhir_9 += (Double) data[9];
				saldo_akhir_10 += (Double) data[10];
				saldo_akhir_11 += (Double) data[11];
				saldo_akhir_12 += (Double) data[12];

				String idAkun = data[14] + "," + id_akun;

				data = new Object[] { kelompok_laporan, saldo_akhir_1, saldo_akhir_2, saldo_akhir_3, saldo_akhir_4,
						saldo_akhir_5, saldo_akhir_6, saldo_akhir_7, saldo_akhir_8, saldo_akhir_9, saldo_akhir_10,
						saldo_akhir_11, saldo_akhir_12, key, idAkun };
			}
			mapData.put(key, data);

		}

		keys.clear();
		for (Object[] objects : dataAkunting) {
			String jenis_laporan1 = objects[4] + "";
			String jenis_laporan2 = objects[5] + "";

			if (jenis_laporan1 == null || jenis_laporan1.trim().isEmpty()
					|| jenis_laporan1.trim().equalsIgnoreCase("null")) {
				jenis_laporan1 = jenisLaporan.getNama();
			}

			if (jenis_laporan2 == null || jenis_laporan2.trim().isEmpty()
					|| jenis_laporan2.trim().equalsIgnoreCase("null")) {
				jenis_laporan2 = jenis_laporan1;
			}

			Double saldo_akhir_1 = ((Number) objects[10]).doubleValue();
			Double saldo_akhir_2 = ((Number) objects[11]).doubleValue();
			Double saldo_akhir_3 = ((Number) objects[12]).doubleValue();
			Double saldo_akhir_4 = ((Number) objects[13]).doubleValue();
			Double saldo_akhir_5 = ((Number) objects[14]).doubleValue();
			Double saldo_akhir_6 = ((Number) objects[15]).doubleValue();
			Double saldo_akhir_7 = ((Number) objects[16]).doubleValue();
			Double saldo_akhir_8 = ((Number) objects[17]).doubleValue();
			Double saldo_akhir_9 = ((Number) objects[18]).doubleValue();
			Double saldo_akhir_10 = ((Number) objects[19]).doubleValue();
			Double saldo_akhir_11 = ((Number) objects[20]).doubleValue();
			Double saldo_akhir_12 = ((Number) objects[21]).doubleValue();

			Long id_akun = ((Number) objects[22]).longValue();

			String key = jenis_laporan1 + "__" + jenis_laporan2;
			key = key.trim();

			keys.add(key);

			String u1 = "000000" + keys.size();

			String urutan = u1.substring(u1.length() - 4);
			key = urutan + "-" + key;

			Object[] data = mapData.get(key);
			if (data == null) {
				data = new Object[] { jenis_laporan2, saldo_akhir_1, saldo_akhir_2, saldo_akhir_3, saldo_akhir_4,
						saldo_akhir_5, saldo_akhir_6, saldo_akhir_7, saldo_akhir_8, saldo_akhir_9, saldo_akhir_10,
						saldo_akhir_11, saldo_akhir_12, key, id_akun + "" };
			} else {
				saldo_akhir_1 += (Double) data[1];
				saldo_akhir_2 += (Double) data[2];
				saldo_akhir_3 += (Double) data[3];
				saldo_akhir_4 += (Double) data[4];
				saldo_akhir_5 += (Double) data[5];
				saldo_akhir_6 += (Double) data[6];
				saldo_akhir_7 += (Double) data[7];
				saldo_akhir_8 += (Double) data[8];
				saldo_akhir_9 += (Double) data[9];
				saldo_akhir_10 += (Double) data[10];
				saldo_akhir_11 += (Double) data[11];
				saldo_akhir_12 += (Double) data[12];

				String idAkun = data[14] + "," + id_akun;

				data = new Object[] { jenis_laporan2, saldo_akhir_1, saldo_akhir_2, saldo_akhir_3, saldo_akhir_4,
						saldo_akhir_5, saldo_akhir_6, saldo_akhir_7, saldo_akhir_8, saldo_akhir_9, saldo_akhir_10,
						saldo_akhir_11, saldo_akhir_12, key, idAkun };
			}
			mapData.put(key, data);

		}

		keys.clear();
		for (Object[] objects : dataAkunting) {
			String jenis_laporan1 = objects[4] + "";
			if (jenis_laporan1 == null || jenis_laporan1.trim().isEmpty()
					|| jenis_laporan1.trim().equalsIgnoreCase("null")) {
				jenis_laporan1 = jenisLaporan.getNama();
			}

			Double saldo_akhir_1 = ((Number) objects[10]).doubleValue();
			Double saldo_akhir_2 = ((Number) objects[11]).doubleValue();
			Double saldo_akhir_3 = ((Number) objects[12]).doubleValue();
			Double saldo_akhir_4 = ((Number) objects[13]).doubleValue();
			Double saldo_akhir_5 = ((Number) objects[14]).doubleValue();
			Double saldo_akhir_6 = ((Number) objects[15]).doubleValue();
			Double saldo_akhir_7 = ((Number) objects[16]).doubleValue();
			Double saldo_akhir_8 = ((Number) objects[17]).doubleValue();
			Double saldo_akhir_9 = ((Number) objects[18]).doubleValue();
			Double saldo_akhir_10 = ((Number) objects[19]).doubleValue();
			Double saldo_akhir_11 = ((Number) objects[20]).doubleValue();
			Double saldo_akhir_12 = ((Number) objects[21]).doubleValue();

			Long id_akun = ((Number) objects[22]).longValue();

			String key = jenis_laporan1;
			key = key.trim();

			keys.add(key);

			String u1 = "000000" + keys.size();

			String urutan = u1.substring(u1.length() - 4);
			key = urutan + "-" + key;

			Object[] data = mapData.get(key);
			if (data == null) {
				data = new Object[] { jenis_laporan1, saldo_akhir_1, saldo_akhir_2, saldo_akhir_3, saldo_akhir_4,
						saldo_akhir_5, saldo_akhir_6, saldo_akhir_7, saldo_akhir_8, saldo_akhir_9, saldo_akhir_10,
						saldo_akhir_11, saldo_akhir_12, key, id_akun + "" };
			} else {
				saldo_akhir_1 += (Double) data[1];
				saldo_akhir_2 += (Double) data[2];
				saldo_akhir_3 += (Double) data[3];
				saldo_akhir_4 += (Double) data[4];
				saldo_akhir_5 += (Double) data[5];
				saldo_akhir_6 += (Double) data[6];
				saldo_akhir_7 += (Double) data[7];
				saldo_akhir_8 += (Double) data[8];
				saldo_akhir_9 += (Double) data[9];
				saldo_akhir_10 += (Double) data[10];
				saldo_akhir_11 += (Double) data[11];
				saldo_akhir_12 += (Double) data[12];

				String idAkun = data[14] + "," + id_akun;

				data = new Object[] { jenis_laporan1, saldo_akhir_1, saldo_akhir_2, saldo_akhir_3, saldo_akhir_4,
						saldo_akhir_5, saldo_akhir_6, saldo_akhir_7, saldo_akhir_8, saldo_akhir_9, saldo_akhir_10,
						saldo_akhir_11, saldo_akhir_12, key, idAkun };
			}
			mapData.put(key, data);

		}

		Row row1 = new Row();
		row1.setParent(rowUtamapalingAwal.getParent());

		Row row2 = new Row();
		row2.setParent(rowUtamapalingAwal.getParent());

		final Tree tree = new Tree();
		tree.setZclass("z-dottree");
		tree.setParent(row2);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/svg/download.svg");
		toolbarbutton.setParent(row1);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadTree(tree);
			}
		});

		Treecols columns = new Treecols();

		columns.setParent(tree);

		Treecol column = new Treecol("Uraian");
		column.setParent(columns);

		for (String bln : Common.BULAN) {
			column = new Treecol(bln);
			column.setWidth("6%");
			column.setAlign("right");
			column.setParent(columns);
		}

		tree.setModel(new AbstractTreeModel("") {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isLeaf(Object arg0) {
				String key = "";
				try {
					Object[] objects = (Object[]) arg0;
					key = (String) objects[13];
				} catch (Exception e) {
					key = (String) arg0;
				}
				return key == null ? true : key.split("__").length == 5;
			}

			@Override
			public int getChildCount(Object arg0) {
				int count = 0;
				String key = "";
				try {
					Object[] objects = (Object[]) arg0;
					key = (String) objects[13];
				} catch (Exception e) {
					key = (String) arg0;
				}

				try {
					key = key.isEmpty() ? key : key.split("-", 2)[1];
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardAkuntingHelper.java:558");
					// TODO: handle exception
				}

				if (key.isEmpty()) {
					for (String k : mapData.keySet()) {
						if (k.split("__").length == 1) {
							count++;
						}
					}
				} else {
					for (String k : mapData.keySet()) {

						String sli = k.split("-", 2)[1];

						if ((k.split("__").length - 1) == key.split("__").length && sli.startsWith(key)) {
							count++;
						}
					}
				}


				return count;
			}

			@Override
			public Object getChild(Object arg0, int arg1) {
				String key = "";
				try {
					Object[] objects = (Object[]) arg0;
					key = (String) objects[13];
				} catch (Exception e) {
					key = (String) arg0;
				}

				try {
					key = key.isEmpty() ? key : key.split("-", 2)[1];
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardAkuntingHelper.java:595");
					// TODO: handle exception
				}

				int count = 0;
				if (key.isEmpty()) {
					for (String k : mapData.keySet()) {
						if (k.split("__").length == 1) {

							if (count == arg1) {
								return mapData.get(k);
							}

							count++;
						}
					}
				} else {
					for (String k : mapData.keySet()) {

						String sli = k.split("-", 2)[1];

						if ((k.split("__").length - 1) == key.split("__").length && sli.startsWith(key)) {

							if (count == arg1) {
								return mapData.get(k);
							}

							count++;
						}
					}
				}
				return null;
			}
		});

		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(Treeitem treeitem, Object arg1) throws Exception {
				if (arg1 == null) {
					treeitem.setVisible(false);
					return;
				}

				Object[] objects = (Object[]) arg1;

				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				Treecell arg0 = new Treecell(objects[0] + "");
				arg0.setParent(treerow);

				Double saldo_akhir_1 = ((Number) objects[1]).doubleValue();
				Double saldo_akhir_2 = ((Number) objects[2]).doubleValue();
				Double saldo_akhir_3 = ((Number) objects[3]).doubleValue();
				Double saldo_akhir_4 = ((Number) objects[4]).doubleValue();
				Double saldo_akhir_5 = ((Number) objects[5]).doubleValue();
				Double saldo_akhir_6 = ((Number) objects[6]).doubleValue();
				Double saldo_akhir_7 = ((Number) objects[7]).doubleValue();
				Double saldo_akhir_8 = ((Number) objects[8]).doubleValue();
				Double saldo_akhir_9 = ((Number) objects[9]).doubleValue();
				Double saldo_akhir_10 = ((Number) objects[10]).doubleValue();
				Double saldo_akhir_11 = ((Number) objects[11]).doubleValue();
				Double saldo_akhir_12 = ((Number) objects[12]).doubleValue();

				Double[] nilais = new Double[] { saldo_akhir_1, saldo_akhir_2, saldo_akhir_3, saldo_akhir_4,
						saldo_akhir_5, saldo_akhir_6, saldo_akhir_7, saldo_akhir_8, saldo_akhir_9, saldo_akhir_10,
						saldo_akhir_11, saldo_akhir_12 };

				final String idAkun = (String) objects[14];

				int bln = 0;
				for (Double n : nilais) {
					final int bln_data = bln;

					arg0 = new Treecell();
					arg0.setParent(treerow);
					arg0.setStyle("font-size:8px;color:blue;");

					A a = new A(n < 0.0 ? "(" + Common.numberFormat.get().format(Math.abs(n)) + ")"
							: Common.numberFormat.get().format(n));
					a.setParent(arg0);
					a.setStyle("font-size:8px;color:blue;");
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							nilaiSaldo = 0.0;

							EventListener eventListener = (EventListener) Common
									.cetakDataCustomButton(Transaksi.class, new DataCriteriaWithColumn() {

										@Override
										public Object[] initCriteria(boolean order) {

											try {
												Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
												calendar.set(Calendar.YEAR, year);
												calendar.set(Calendar.MONTH, bln_data);
												calendar.set(Calendar.DATE,
														calendar.getActualMaximum(Calendar.DAY_OF_MONTH));

												Criteria criteria = HibernateUtil.currentSession()
														.createCriteria(Transaksi.class)

														.add(Restrictions.or(
																Restrictions.or(Restrictions.gt("debet", 0.1),
																		Restrictions.lt("debet", -0.1)),
																Restrictions.or(Restrictions.gt("kredit", 0.1),
																		Restrictions.lt("kredit", -0.1))))

														.add(Restrictions.isNotNull("postingHistory"))
														.add(Restrictions.isNotNull("grupTransaksi"))

														.add(Restrictions
																.sqlRestriction("this_.akun in (" + idAkun + ")"))

														.add(Restrictions.sqlRestriction(
																"date(this_.tanggal_transaksi) between date('1970-01-01') and date('"
																		+ Common.databaseDateFormat.get()
																				.format(calendar.getTime())
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

					bln++;
				}
			}
		});

		dataAkunting = null;

	}


	private static void appendDashboardSopDescriptionRow(org.zkoss.zul.Rows rows, String title, String description) {
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

	private static String buildMiniInfoCard(String no, String title, String desc) {
		return "<div style=\"border-radius:14px; padding:10px; background:#f8fafc; border:1px solid #e2e8f0;\">"
				+ "<div style=\"width:26px; height:26px; border-radius:999px; background:#0f766e; color:#fff; display:flex; align-items:center; justify-content:center; font-weight:900;\">" + safeDashboardHtml(no) + "</div>"
				+ "<div style=\"font-size:12px; font-weight:900; color:#0f172a; margin-top:7px;\">" + safeDashboardHtml(title) + "</div>"
				+ "<div style=\"font-size:10.5px; color:#64748b; line-height:1.45; margin-top:3px;\">" + safeDashboardHtml(desc) + "</div></div>";
	}

	private static String safeDashboardHtml(String value) {
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
