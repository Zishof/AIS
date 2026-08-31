package ais.action.report.format1.library;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.library.helper.AmbilDataAnggotaBanbox;
import ais.action.master.library.helper.AmbilDataItemBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.PesananAnggota;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

/**
 * Penyusun/penyaji laporan untuk laporan pesanan item. Kelas ini mengubah data domain menjadi
 * bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke
 * lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code AmbilDataItemBanbox
 * ambilDataItemBanbox}, {@code AmbilDataAnggotaBanbox ambilDataAnggotaBanbox}, {@code MyDatebox mulai}, {@code
 * MyDatebox sampai}, {@code Combobox status}, {@code Center center}, {@code Toolbar toolbar};
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
public class LaporanPesananItem extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private AmbilDataItemBanbox ambilDataItemBanbox;
	private AmbilDataAnggotaBanbox ambilDataAnggotaBanbox;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private Combobox status;

	private Center center;
	private Toolbar toolbar;

	public LaporanPesananItem() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pesanan Item", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

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
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Item"));
		row.appendChild(ambilDataItemBanbox = new AmbilDataItemBanbox());
		ambilDataItemBanbox.setWidth("90%");
		ambilDataItemBanbox.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Anggota"));
		row.appendChild(ambilDataAnggotaBanbox = new AmbilDataAnggotaBanbox());
		ambilDataAnggotaBanbox.setWidth("90%");
		ambilDataAnggotaBanbox.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		row.appendChild(mulai = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		mulai.setWidth("90%");
		mulai.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai"));
		row.appendChild(sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		sampai.setWidth("90%");
		sampai.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(status = new Combobox());
		status.setWidth("90%");
		status.addEventListener("onChange", eventListener);

		MyComboitemConfig comboitem = new MyComboitemConfig(PesananAnggota.PESAN);
		status.appendChild(comboitem);
		comboitem = new MyComboitemConfig(PesananAnggota.KADALUARSA);
		status.appendChild(comboitem);
		comboitem = new MyComboitemConfig(PesananAnggota.PESAN);
		status.appendChild(comboitem);
		comboitem = new MyComboitemConfig(PesananAnggota.PINJAM);
		status.appendChild(comboitem);
		comboitem = new MyComboitemConfig(PesananAnggota.DIKEMBALIKAN);
		status.appendChild(comboitem);
		comboitem = new MyComboitemConfig(PesananAnggota.BATAL);
		status.appendChild(comboitem);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				Map parameters = generateParameter();
				return parameters;
			}
		}, "library/pesanan", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
	private Map generateParameter() throws Exception {

		final Date mulai = this.mulai.getValue();
		final Date sampai = this.sampai.getValue();
		sampai.setDate(sampai.getDate() + 1);
		final Map parameters = ais.common.HashMapGenerator.getRand();

		Session session = HibernateUtil.currentSession();
		List<PesananAnggota> pesananAnggotas = session.createCriteria(PesananAnggota.class)
				.add(status.getSelectedItem() == null || status.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("status", status.getSelectedItem().getLabel()))

				.add(ambilDataItemBanbox.getAttribute("item") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("item", ambilDataItemBanbox.getAttribute("item")))

				.add(ambilDataAnggotaBanbox.getAttribute("anggota") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("anggota", ambilDataAnggotaBanbox.getAttribute("anggota")))

				.add(mulai == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction(
								"date(tanggal) >= date('" + Common.databaseDateFormat.get().format(mulai) + "')"))
				.add(sampai == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction(
								"date(tanggal) <= date('" + Common.databaseDateFormat.get().format(sampai) + "')"))
				.setMaxResults(Common.MAX_RESULT_100).addOrder(Order.desc("id")).list();

		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		for (PesananAnggota pesananAnggota : pesananAnggotas) {
			Map<String, Object> map = new java.util.HashMap<String, Object>();
			String code = pesananAnggota.getAnggota().toString()
					+ (pesananAnggota.getPerpustakaan() == null ? ""
							: "\n" + pesananAnggota.getPerpustakaan().getNama())
					+ "\n" + pesananAnggota.getItem().getIsbn() + " - " + pesananAnggota.getItem().getNama()
					+ "\nStatus : " + pesananAnggota.getStatus() + " - "
					+ Common.dateFormat5.get().format(pesananAnggota.getTanggal());
			map.put("code", code);

			final File myfile = new File(Common.ambilREAL_PATH_REPORT() + "/barcode_" + pesananAnggota.getKode() + ".png");

			if (!myfile.exists()) {
				Barcode mybarcode = BarcodeFactory.createCode128(pesananAnggota.getKode());
				BarcodeImageHandler.savePNG(mybarcode, myfile);
			}

			map.put("barcode", myfile.getAbsolutePath());
			map.put("c_code", pesananAnggota.getKode());
			maps.add(map);
		}
		parameters.put("maps", maps);
		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "library/pesanan",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Pesanan Item", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
