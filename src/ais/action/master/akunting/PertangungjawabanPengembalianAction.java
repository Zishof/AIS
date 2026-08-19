package ais.action.master.akunting;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.format1.akunting.LaporanPertangungjawaban;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * <h3>PertangungjawabanPengembalianAction — Monitor Pengembalian Sisa Uang Muka LPJ</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Controller ZK berbasis {@link GenericAutowireComposer} yang menangani tampilan dan
 * pengelolaan data pengembalian sisa uang muka dari Laporan Pertanggungjawaban (LPJ).
 * Dalam alur keuangan organisasi, ketika pegawai mendapat uang muka untuk perjalanan
 * dinas atau kegiatan tertentu, mereka wajib membuat LPJ berisi rincian pengeluaran
 * aktual. Jika pengeluaran aktual lebih kecil dari uang muka yang diterima, terdapat
 * sisa yang harus dikembalikan ke kas. Halaman ini secara khusus menampilkan semua
 * {@link Pertangungjawaban} yang memiliki nilai dikembalikan lebih dari nol (0.01),
 * memungkinkan bagian keuangan memantau dan mengkonfirmasi status pengembalian tersebut.
 * Pengguna dapat menandai setiap pengembalian sebagai "Telah Dikembalikan" dengan
 * mencatat tanggal pengembalian, serta mencetak laporan pertanggungjawaban.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Inisialisasi dilakukan di {@code doAfterCompose}: mengisi combobox status
 * (Semua/Pengajuan/Disetujui/Ditolak), mengatur rentang tanggal default 6 bulan
 * ke belakang hingga hari ini, dan mendaftarkan paging serta timer refresh. Renderer
 * {@code PertangungjawabanRenderer} membangun tampilan grid yang kaya: setiap baris
 * berisi grid rincian biaya inline yang diparse dari JSON formula LPJ, menampilkan
 * nama biaya, qty, harga, PPN, PPH, total, status persetujuan, dan tanggal. Di bawah
 * grid rincian terdapat checkbox "Setuju" dan datebox tanggal yang bisa diedit
 * langsung. Logika PPH mempertimbangkan konfigurasi {@code pph_mengurangi_lpj}.
 * Filter mendukung: rentang tanggal, status, satuan kerja (hierarki), aktif, kode,
 * dan nama. Method {@code cetak} membuka laporan LPJ dalam window modal.</p>
 *
 * <p><b>Threading:</b><br>
 * Berjalan di event thread ZK. Sesi Hibernate adalah sesi terkelola. Field
 * {@code tbmuser} diinisialisasi di konstruktor dari sesi pengguna saat ini
 * dan digunakan sebagai fallback saat {@code dibuatOleh} null pada entitas.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Logika kalkulasi total di renderer membaca JSON field "formula" pada entitas
 * {@link Pertangungjawaban}. Jika format JSON berubah, perbarui blok parsing
 * di dalam metode {@code render}. Konfigurasi {@code pph_mengurangi_lpj} dapat
 * diubah melalui modul Konfigurasi tanpa mengubah kode ini. Jika ada kolom
 * baru pada grid rincian, tambahkan {@link MyColumnConfig} dan parsing JSON
 * yang sesuai di dalam renderer.</p>
 *
 * @see Pertangungjawaban
 * @see LaporanPertangungjawaban
 * @see DataCriteria
 * @see DataSearchDefault
 */
public class PertangungjawabanPengembalianAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault {

	/**
	 * Nomor versi serialisasi untuk kompatibilitas mekanisme serialisasi Java.
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	/** Komponen paging untuk navigasi halaman daftar pertanggungjawaban. */
	private Paging paging;

	/** Grid utama penampil daftar {@link Pertangungjawaban} dengan pengembalian. */
	private MyGrid grid;

	/** Field teks pencarian berdasarkan nama pertanggungjawaban. */
	private Textbox serachnama;

	/** Field teks pencarian berdasarkan kode pertanggungjawaban. */
	private Textbox serachkode;

	/** Checkbox filter untuk menampilkan hanya yang aktif atau semua. */
	private Checkbox searchaktif;

	/** Combobox filter berdasarkan status LPJ (Semua/Pengajuan/Disetujui/Ditolak). */
	private Combobox searchstatus;

	/** Datebox awal rentang tanggal filter; default 6 bulan lalu. */
	private MyDatebox start;

	/** Datebox akhir rentang tanggal filter; default besok. */
	private MyDatebox end;

	/** Apakah pengguna memiliki hak UPDATE (untuk mengaktifkan checkbox pengembalian). */
	private boolean edit;

	/**
	 * Pengguna yang sedang login. Diinisialisasi di konstruktor dan digunakan sebagai
	 * fallback saat field dibuatOleh pada entitas pertanggungjawaban null.
	 */
	private Tbmuser tbmuser;

	/** Model pohon satuan kerja untuk navigasi hierarki filter unit kerja. */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/** Banbox pemilih satuan kerja sebagai filter hierarki unit kerja. */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/**
	 * Konstruktor default yang menginisialisasi field {@code tbmuser} dengan
	 * data pengguna yang sedang login saat ini melalui {@link Common#getCurrentUser()}.
	 *
	 * <p><b>Tujuan:</b> Menyimpan referensi pengguna aktif di awal sehingga
	 * renderer dapat menggunakannya sebagai nilai fallback untuk field
	 * {@code dibuatOleh} pada entitas yang mungkin null (misalnya data lama
	 * yang dibuat sebelum field ini ada).</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link Common#getCurrentUser()} yang
	 * mengambil objek {@link Tbmuser} dari sesi ZK yang sedang aktif. Karena
	 * konstruktor dipanggil sebelum siklus hidup ZK dimulai, sesi harus sudah
	 * tersedia melalui thread-local ZK.</p>
	 *
	 * <p><b>Penanganan error:</b> Jika {@link Common#getCurrentUser()} mengembalikan
	 * null (misalnya sesi sudah kadaluarsa), field {@code tbmuser} akan null dan
	 * renderer perlu menangani null secara defensif.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika cara mendapatkan pengguna aktif berubah
	 * (misalnya menggunakan sumber lain), perbarui pemanggilan di sini.</p>
	 */
	public PertangungjawabanPengembalianAction() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Dipanggil ZK sebelum komponen halaman dibangun. Memverifikasi hak akses
	 * keamanan melalui {@link Common#doCheckSecurity()}.
	 *
	 * <p><b>Tujuan:</b> Gerbang keamanan awal yang mencegah halaman dimuat
	 * jika sesi pengguna tidak valid atau tidak memiliki akses ke modul ini.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link Common#doCheckSecurity()} lalu
	 * mendelegasikan ke super untuk melanjutkan komposisi komponen.</p>
	 *
	 * <p><b>Penanganan error:</b> {@link Common#doCheckSecurity()} akan menghentikan
	 * proses dan mengarahkan ke logoff jika akses tidak sah.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah kecuali ada kebutuhan keamanan
	 * tambahan pada tahap pre-compose.</p>
	 *
	 * @param page halaman ZK yang sedang diproses
	 * @param parent komponen induk dalam hierarki ZK
	 * @param compInfo metadata komponen dari ZUL
	 * @return ComponentInfo dari implementasi super
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Dipanggil ZK setelah seluruh komponen halaman selesai di-autowire. Menginisialisasi
	 * halaman pemantauan pengembalian LPJ: validasi sesi, bahasa, rentang tanggal default,
	 * combobox status, filter satuan kerja, paging, dan timer refresh.
	 *
	 * <p><b>Tujuan:</b> Menyiapkan halaman secara lengkap agar siap digunakan untuk
	 * memantau dan mengkonfirmasi pengembalian sisa uang muka dari LPJ.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil super dan {@link Common#initLaguage()}.</li>
	 *   <li>Validasi sesi dan hak READ; jika tidak ada, logout.</li>
	 *   <li>Mendaftarkan listener pada {@code searchparent} untuk filter satuan kerja
	 *       yang memicu pencarian ulang saat berubah.</li>
	 *   <li>Menginisialisasi {@link SatuanKerjaTreeModel}.</li>
	 *   <li>Menetapkan datebox start/end sebagai readonly (pilih via popup kalender).</li>
	 *   <li>Mengisi start: 6 bulan lalu, end: besok (tanggal besok agar data hari ini
	 *       selalu masuk rentang).</li>
	 *   <li>Mengisi combobox status dengan pilihan: Semua, Pengajuan, Disetujui, Ditolak.</li>
	 *   <li>Menyimpan flag {@code edit} berdasarkan hak UPDATE (untuk checkbox pengembalian).</li>
	 *   <li>Menginisialisasi paging dan timer refresh.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception disebarkan ke ZK. Jika sesi tidak valid,
	 * pengguna diarahkan ke logoff.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada status baru pada {@link Pertangungjawaban},
	 * tambahkan Comboitem di blok pengisian combobox status. Perubahan rentang tanggal
	 * default cukup diubah pada baris Calendar.set di sini.</p>
	 *
	 * @param comp komponen root halaman ZK
	 * @throws Exception jika terjadi kesalahan saat inisialisasi
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		Comboitem comboitemSemua = new Comboitem("Semua");
		if (comboitemSemua != null) { comboitemSemua.setValue(null); }
		searchstatus.appendChild(comboitemSemua);

		Comboitem comboitem = new Comboitem(Pertangungjawaban.PENGAJUAN);
		if (comboitem != null) { comboitem.setValue(Pertangungjawaban.PENGAJUAN); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(Pertangungjawaban.DISETUJU);
		if (comboitem != null) { comboitem.setValue(Pertangungjawaban.DISETUJU); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(Pertangungjawaban.DITOLAK);
		if (comboitem != null) { comboitem.setValue(Pertangungjawaban.DITOLAK); }
		searchstatus.appendChild(comboitem);

		if (searchstatus != null) { searchstatus.setSelectedItem(comboitemSemua); }
		if (searchstatus != null) { searchstatus.setReadonly(true); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	/**
	 * Inner class renderer yang membangun tampilan baris yang kaya untuk setiap
	 * {@link Pertangungjawaban} dalam grid, termasuk grid rincian biaya inline
	 * yang diparse dari JSON, total, status SOP, dan kontrol konfirmasi pengembalian.
	 *
	 * <p><b>Tujuan:</b> Menampilkan seluruh informasi LPJ dan pengembaliannya dalam
	 * satu baris yang ekspansif tanpa memerlukan navigasi ke halaman detail terpisah.
	 * Setiap baris berisi: header informasi uang muka, grid rincian biaya (dari JSON
	 * formula), keterangan dan status SOP, jumlah yang dikembalikan, serta kontrol
	 * untuk konfirmasi pengembalian (checkbox + tanggal) dan tombol cetak laporan.</p>
	 *
	 * <p><b>Cara kerja:</b> JSON field {@code formula} di-parse sebagai
	 * {@link JSONArray} berisi {@link JSONObject} dengan key: nama, tgl, qty, harga,
	 * ppn, jumlah, pajak, sesuai. Total dihitung dengan mempertimbangkan konfigurasi
	 * {@code pph_mengurangi_lpj}. Checkbox konfirmasi pengembalian menyimpan langsung
	 * ke database dan mengatur tanggal otomatis saat dicentang.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika format JSON formula berubah, perbarui blok parsing
	 * di dalam metode {@code render}. Total dihitung secara lokal dan disimpan ke
	 * entitas via {@code setNilai} — ini adalah nilai sementara runtime, bukan persisten.</p>
	 */
	class PertangungjawabanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Mengisi satu baris grid dengan seluruh data {@link Pertangungjawaban} secara
		 * lengkap dan interaktif, termasuk rincian biaya dari JSON dan kontrol konfirmasi.
		 *
		 * <p><b>Tujuan:</b> Menampilkan informasi LPJ yang lengkap dengan perhitungan
		 * total yang mempertimbangkan PPN dan PPH, status persetujuan per item, serta
		 * fasilitas konfirmasi pengembalian langsung dari grid tanpa popup tambahan.</p>
		 *
		 * <p><b>Cara kerja:</b>
		 * <ol>
		 *   <li>Jika {@code dibuatOleh} null, mengisi dengan {@code tbmuser} dari controller.</li>
		 *   <li>Menampilkan kode, nama, dan info uang muka menggunakan RevisiHelper dan Label.</li>
		 *   <li>Menampilkan nilai uang muka yang diformat dengan numberFormat.</li>
		 *   <li>Membangun grid rincian inline dengan kolom: Keterangan, Qty, Harga, PPN,
		 *       PPH, Jumlah, Setuju/Tanggal.</li>
		 *   <li>Mem-parse JSON {@code formula}, menghitung total per item, akumulasi nilai
		 *       keseluruhan dengan mempertimbangkan konfigurasi {@code pph_mengurangi_lpj}.</li>
		 *   <li>Menambahkan row footer dengan total akumulasi.</li>
		 *   <li>Menyimpan total ke entitas via {@code setNilai(nilai)}.</li>
		 *   <li>Menampilkan keterangan, link SOP (jika ada), status transfer via
		 *       {@link DaftarPengajuanTransfer#tampilStatus}.</li>
		 *   <li>Menampilkan nilai yang dikembalikan.</li>
		 *   <li>Checkbox "Setuju" dan datebox tanggal yang keduanya menyimpan langsung
		 *       ke database saat berubah. Checkbox otomatis mengisi tanggal hari ini
		 *       jika dicentang, atau menghapus tanggal jika dilepas.</li>
		 *   <li>Tombol Cetak untuk membuka laporan LPJ via {@code cetak(pertangungjawaban)}.</li>
		 * </ol>
		 * </p>
		 *
		 * <p><b>Penanganan error:</b> Exception dari parsing JSON atau penyimpanan
		 * disebarkan ke ZK. Data JSON yang tidak valid (field null) ditangani dengan
		 * pengecekan {@code isNull} sebelum mengambil nilai.</p>
		 *
		 * <p><b>Pemeliharaan:</b> Perubahan format JSON formula harus diselaraskan
		 * dengan perubahan pada modul input LPJ. Konfigurasi PPH dibaca dari database
		 * setiap kali render dipanggil (tidak di-cache) untuk memastikan selalu terkini.</p>
		 *
		 * @param arg0 baris grid ZK yang akan diisi
		 * @param arg1 objek data yang akan dicast ke {@link Pertangungjawaban}
		 * @throws Exception jika terjadi kesalahan rendering, parsing JSON, atau penyimpanan
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Pertangungjawaban pertangungjawaban = (Pertangungjawaban) arg1;

			if (pertangungjawaban.getDibuatOleh() == null) {
				pertangungjawaban.setDibuatOleh(tbmuser);
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(Pertangungjawaban.class, pertangungjawaban,
					pertangungjawaban.getKode() == null ? "" : pertangungjawaban.getKode().trim().toString()))
					.setParent(arg0);
			new Label(pertangungjawaban.getNama()).setParent(a);

			new Label(pertangungjawaban.getUangMuka() == null ? ""
					: pertangungjawaban.getUangMuka().getKode() + "-" + pertangungjawaban.getUangMuka().getNama())
					.setParent(a);

			arg0.appendChild(new Label(Common.numberFormat.get().format(
					pertangungjawaban.getUangMuka() == null ? 0.0 : pertangungjawaban.getUangMuka().getNilai())));

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setWidth("100%");
			grid.setParent(arg0);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig("Keterangan Biaya");
			column.setParent(columns);

			column = new MyColumnConfig("Qty");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("5%");

			column = new MyColumnConfig("Harga");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("15%");

			column = new MyColumnConfig("PPN");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("10%");

			column = new MyColumnConfig("PPH");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("10%");

			column = new MyColumnConfig("Jumlah");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("15%");

			column = new MyColumnConfig("Setuju/Tanggal");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("25%");

			Double nilai = 0.0;

			boolean pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");
			Rows rows = new Rows();
			rows.setParent(grid);
			final JSONArray array = new JSONArray(pertangungjawaban.getFormula());
			for (int i = 0; i < array.length(); i++) {

				final JSONObject jsonObject = array.getJSONObject(i);

				String nama = "";

				if (!jsonObject.isNull("nama")) {
					nama = jsonObject.get("nama") + "";
				}

				Date tgl = null;
				if (!jsonObject.isNull("tgl")) {
					tgl = Common.dateFormat1.get().parse(jsonObject.get("tgl") + "");
				}

				Double qty = 0.0;
				if (!jsonObject.isNull("qty")) {
					qty = jsonObject.getDouble("qty");
				}

				Double harga = 0.0;
				if (!jsonObject.isNull("harga")) {
					harga = jsonObject.getDouble("harga");
				}

				Double ppn = 0.0;
				if (!jsonObject.isNull("ppn")) {
					ppn = jsonObject.getDouble("ppn");
				}

				Double jumlah = 0.0;
				if (!jsonObject.isNull("jumlah")) {
					jumlah = jsonObject.getDouble("jumlah");
				}

				JenisPajakBarang barang;
				if (!jsonObject.isNull("pajak")) {
					barang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
							Long.parseLong(jsonObject.get("pajak") + ""));
				} else {
					barang = null;
				}

				Double pajak = barang == null ? 0.0 : ((barang.getPersen() / 100.0) * jumlah);

				Double tot = (jumlah + ((ppn / 100.0) * jumlah)) - (pph_mengurangi_lpj ? pajak : 0.0);

				nilai += tot;

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);

				row.appendChild(new MyLabelAgakKecil(nama));
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(qty)));
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(harga)));
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(ppn)));
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(pajak)));
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(tot)));

				Boolean sesuai = false;
				if (!jsonObject.isNull("sesuai")) {
					sesuai = jsonObject.getBoolean("sesuai");
				}

				Vbox vbox = new Vbox();
				vbox.setWidth("99%");
				vbox.setParent(row);

				vbox.appendChild(new Label("Setuju : " + (sesuai ? "Ya" : "Belum")));
				vbox.appendChild(new Label("Tgl : " + (tgl == null ? "" : Common.dateFormat6.get().format(tgl))));

			}
			Foot foot = new Foot();
			foot.setParent(grid);

			Footer footer = new Footer("Total");
			foot.appendChild(footer);

			footer = new Footer("");
			foot.appendChild(footer);

			footer = new Footer("");
			foot.appendChild(footer);

			footer = new Footer("");
			foot.appendChild(footer);

			footer = new Footer("");
			foot.appendChild(footer);

			Footer footerTotal = new Footer(Common.numberFormat.get().format(nilai));
			foot.appendChild(footerTotal);
			pertangungjawaban.setNilai(nilai);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new Label(Common.simpleString(pertangungjawaban.getKeterangan())).setParent(vbox1);
			if (pertangungjawaban.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pertangungjawaban.getDisposisiSop().getKeterangan()
						+ " (" + pertangungjawaban.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pertangungjawaban.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			DaftarPengajuanTransfer.tampilStatus(pertangungjawaban.getDaftarPengajuanTransfer(), vbox1);

			arg0.appendChild(new Label(Common.numberFormat.get().format(pertangungjawaban.getDikembalikan())));

			Vbox vbox = new Vbox();
			vbox.setWidth("99%");
			vbox.setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setuju");
			final MyDatebox tanggal = new MyDatebox(pertangungjawaban.getTanggalDikembalikan());
			checkbox.setDisabled(!edit);
			checkbox.setChecked(pertangungjawaban.getTelahDikembalikan());
			checkbox.setParent(vbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (checkbox.isChecked() && tanggal.getValue() == null) {
						tanggal.setValue(WaktuUtil.getDate());
					} else if (!checkbox.isChecked()) {
						tanggal.setValue(null);
					}

					pertangungjawaban.setTelahDikembalikan(checkbox.isChecked());
					pertangungjawaban.setTanggalDikembalikan(tanggal.getValue());

					Common.refreshSaveOrUpdate(pertangungjawaban);

					tanggal.setDisabled(!edit || !checkbox.isChecked());
				}
			});
			tanggal.setWidth("95%");
			tanggal.setReadonly(true);
			tanggal.setDisabled(!edit || !checkbox.isChecked());
			tanggal.setParent(vbox);
			tanggal.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pertangungjawaban.setTelahDikembalikan(checkbox.isChecked());
					pertangungjawaban.setTanggalDikembalikan(tanggal.getValue());

					Common.refreshSaveOrUpdate(pertangungjawaban);
				}
			});

			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(pertangungjawaban);
				}
			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	/**
	 * Membuka laporan pertanggungjawaban dalam window modal yang dapat dicetak.
	 * Metode ini bersifat static sehingga dapat dipanggil dari konteks lain jika
	 * diperlukan.
	 *
	 * <p><b>Tujuan:</b> Menyediakan akses cepat ke laporan cetak LPJ langsung dari
	 * baris grid tanpa navigasi ke halaman laporan terpisah. Laporan menampilkan
	 * rincian pertanggungjawaban yang siap dicetak untuk keperluan dokumentasi
	 * dan audit keuangan.</p>
	 *
	 * <p><b>Cara kerja:</b> Membuat instance {@link LaporanPertangungjawaban} dengan
	 * entitas pertanggungjawaban yang diberikan, mengatur properti window (judul,
	 * ukuran, closable), menambahkan ke root halaman ZK saat ini melalui
	 * {@link ExecutionsCtrl#getCurrentCtrl()}, dan membukanya dalam mode modal
	 * sehingga pengguna dapat melihat dan mencetak laporan tanpa menutup halaman
	 * daftar.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception saat membangun laporan (misalnya data
	 * entitas tidak lengkap) disebarkan ke caller. Caller bertanggung jawab menampilkan
	 * pesan error yang sesuai.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Ukuran window (900px lebar, 90% tinggi) dapat disesuaikan
	 * jika template laporan berubah ukuran. Jika perlu membatasi akses cetak berdasarkan
	 * hak, tambahkan pengecekan CommonPrivilages sebelum membuka window.</p>
	 *
	 * @param pertangungjawaban entitas {@link Pertangungjawaban} yang akan dicetak laporannya
	 * @throws Exception jika terjadi kesalahan saat membangun atau membuka window laporan
	 */
	public static void cetak(Pertangungjawaban pertangungjawaban) throws Exception {
		LaporanPertangungjawaban buktiPengeluaranKas = new LaporanPertangungjawaban(pertangungjawaban);
		buktiPengeluaranKas.setTitle("Laporan");
		buktiPengeluaranKas.setClosable(true);
		buktiPengeluaranKas.setHeight("90%");
		buktiPengeluaranKas.setWidth("900px");
		buktiPengeluaranKas.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		buktiPengeluaranKas.onModal();
	}

	/**
	 * Membangun objek Hibernate Criteria untuk query daftar {@link Pertangungjawaban}
	 * yang memiliki nilai dikembalikan positif, berdasarkan filter aktif.
	 *
	 * <p><b>Tujuan:</b> Memusatkan logika query pencarian agar dapat digunakan
	 * ganda: untuk menghitung total record (paging, tanpa order) dan untuk
	 * mengambil data halaman aktif (dengan order). Filter utama yang membedakan
	 * halaman ini dari halaman LPJ umum adalah Restrictions.gt("dikembalikan", 0.01)
	 * yang hanya menampilkan LPJ dengan sisa pengembalian.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mendapatkan dan memproses hierarki satuan kerja dari filter {@code searchparent}.</li>
	 *   <li>Membangun Criteria dengan filter:
	 *     <ul>
	 *       <li>Wajib: dikembalikan lebih dari 0.01 (sisa pengembalian positif).</li>
	 *       <li>Rentang tanggal pembuatan (jika start dan end tidak null).</li>
	 *       <li>Filter satuan kerja hierarki.</li>
	 *       <li>Filter status (Pengajuan/Disetujui/Ditolak/Semua).</li>
	 *       <li>Filter aktif (hanya aktif jika checkbox aktif).</li>
	 *       <li>Filter kode ILIKE.</li>
	 *       <li>Filter nama ILIKE.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Jika {@code order} true, menambahkan ORDER BY id DESC (terbaru di atas).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception Hibernate disebarkan ke caller.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Filter tanggal menggunakan SQL native function date() untuk
	 * perbandingan tanggal tanpa komponen waktu. Pastikan format tanggal database
	 * sesuai dengan {@link Common#databaseDateFormat}.</p>
	 *
	 * @param order jika true, tambahkan ORDER BY id DESC ke query
	 * @return objek {@link Criteria} siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Pertangungjawaban.class)

				.add(Restrictions.gt("dikembalikan", 0.01))

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(parent==null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"), Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(serachkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", serachkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(serachnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", serachnama.getValue().trim(), MatchMode.ANYWHERE));
		if (order)
			criteria.addOrder(Order.desc("id"));
		return criteria;
	}

	/**
	 * Menjalankan pencarian data pertanggungjawaban pengembalian dan memperbarui grid
	 * beserta paging dengan hasil yang ditemukan.
	 *
	 * <p><b>Tujuan:</b> Handler utama event pencarian yang mengambil halaman data
	 * saat ini dan memperbaruinya di grid. Dipanggil saat filter berubah,
	 * paging dinavigasi, atau timer refresh berdetak.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@link Common#initPaging(Criteria, Paging)} dengan Criteria
	 *       tanpa order untuk menghitung jumlah total record.</li>
	 *   <li>Mengambil data halaman aktif dengan limit {@link Common#ROWS_COUNT_ON_PAGE}
	 *       dan offset berdasarkan halaman paging aktif.</li>
	 *   <li>Membuat model list dan menyetel renderer ke grid.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception Hibernate disebarkan ke ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah kecuali ada kebutuhan sorting
	 * atau pengelompokan tambahan pada hasil query.</p>
	 *
	 * @param event event ZK yang memicu pencarian; bisa null jika dipanggil programatis
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pertangungjawaban> pertangungjawaban = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pertangungjawaban);
		grid.setRowRenderer(new PertangungjawabanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
