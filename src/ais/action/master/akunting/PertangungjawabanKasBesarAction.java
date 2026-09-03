package ais.action.master.akunting;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
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
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataKasBesarBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.format1.akunting.LaporanPertangungjawabanKasBesar;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.KasBesar;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.Pajak;
import ais.database.model.akunting.PertangungjawabanKasBesar;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.asset.JenisPajakPpn;
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
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * <h3>PertangungjawabanKasBesarAction — Pengelola Pertanggungjawaban Kas Besar</h3>
 *
 * <p><b>Untuk apa:</b> Kelas ini merupakan controller untuk modul Pertanggungjawaban Kas Besar
 * (LPJ Kas Besar) dalam sistem keuangan eCampus. Modul ini menangani proses pertanggungjawaban
 * dana talangan (kas besar / uang muka operasional) yang sebelumnya telah dicairkan melalui
 * modul {@link ais.database.model.akunting.KasBesar}. Setiap {@link ais.database.model.akunting.PertangungjawabanKasBesar}
 * merekam rincian pengeluaran aktual yang dibiayai dari kas besar tersebut, beserta perhitungan
 * PPh dan PPN, nilai yang harus dikembalikan ke kas, dan status persetujuan.</p>
 *
 * <p><b>Cara kerja:</b> Kelas mengimplementasikan empat antarmuka: {@code DataCriteria},
 * {@code DataSearchDefault}, {@code DataInitDefault}, dan {@code FormSop}. Setelah komponen
 * ZK di-wire oleh {@link #doAfterCompose(Component)}, pengguna dapat mengajukan pertanggungjawaban
 * baru dengan memilih kas besar yang bersangkutan, mengisi rincian biaya dalam format JSON
 * (disimpan di field {@code formula} sebagai JSONArray), dan mengajukan untuk disetujui.
 * Setelah disetujui, sistem otomatis membuat {@code DaftarPengajuanTransfer} untuk pengembalian
 * sisa dana.</p>
 *
 * <p><b>Struktur formula biaya:</b> Setiap item biaya disimpan sebagai JSONObject dalam
 * JSONArray dengan field: {@code key} (identifier unik), {@code nama} (keterangan biaya),
 * {@code qty}, {@code harga}, {@code jumlah} (qty*harga), {@code ppn} (persentase PPN),
 * {@code pajak} (id JenisPajakBarang/PPh), {@code ntpn}, {@code npwp}, {@code namaWp},
 * {@code tanggalStor}, {@code nama_file}, dan {@code link}. Perhitungan: total = jumlah +
 * (ppn%*jumlah) - (pph%*jumlah jika konfigurasi pph_mengurangi_lpj=aktif).</p>
 *
 * <p><b>Alur persetujuan:</b>
 * <ol>
 *   <li>Staf membuat pertanggungjawaban (status=PENGAJUAN) dan mengisi rincian biaya.</li>
 *   <li>Penyetuju mengubah status ke DISETUJU — sistem membuat DaftarPengajuanTransfer
 *       untuk pengembalian sisa dana dan memicu cetak laporan otomatis.</li>
 *   <li>Jika ada sisa (nilai dikembalikan > 0), staf wajib mengisi tanggal setor kas.</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b> Semua operasi berjalan di UI thread ZK. Operasi "Hitung Ulang"
 * massal dijalankan dalam {@code Common.createDefaultTimer} (timer ZK sekali tembak)
 * bukan thread terpisah, sehingga UI tetap responsif.</p>
 *
 * <p><b>Pemeliharaan:</b> Konfigurasi {@code pph_mengurangi_lpj} (aktif/tidak-aktif)
 * mempengaruhi rumus perhitungan total di seluruh kelas. Perubahan konfigurasi ini akan
 * langsung terlihat pada pencatatan baru, tetapi tidak mengubah data historis yang sudah
 * tersimpan. Untuk menghitung ulang data historis, gunakan tombol "Hitung Ulang".</p>
 *
 * @see ais.database.model.akunting.PertangungjawabanKasBesar
 * @see ais.database.model.akunting.KasBesar
 * @see ais.database.model.akunting.DaftarPengajuanTransfer
 * @see ais.database.model.akunting.Pajak
 */
public class PertangungjawabanKasBesarAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {

	/**
	 *
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	private MyWindow addWindow;
	private Paging paging;

	/** Wadah tab "Dasbor" (autowire dari pertangungjawaban_kas_besar.zul). */
	private org.zkoss.zul.Vbox dasborKasBesarBox;
	private MyGrid grid;

	private Textbox serachnama;
	private Textbox serachkode;
	private Checkbox searchaktif;
	private Combobox searchstatus;
	private MyDatebox start;
	private MyDatebox end;
	private Textbox nama;
	private Label kode;
	private Textbox keterangan;
	private AmbilDataKasBesarBanbox kasBesar;

	public PertangungjawabanKasBesar pertangungjawabanKasBesar;
	private MyToolbarbuttonConfig add;

	private boolean edit;
	private boolean delete;

	private Double nilai;

	private boolean persetujuan = false;

	private Tbmuser tbmuser;

	private Radiogroup status;

	private DisposisiSop disposisiSop = null;

	private JSONArray array;

	private Row rowFormula;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private boolean setujui = false;

	private AmbilDataSatuanKerjaBanbox satuanKerja;

	private Label nilaiHarusDikembalikan;

	protected double dikembalikan = 0.0;
	protected double nilaipajak = 0.0;
	private boolean pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");
	private Textbox namaSponsor;

	private MyDoublebox dariSponsor;

	private boolean viewOnly = false;

	private MyDatebox tanggalStor;

	/**
	 * Konstruktor default untuk mode daftar dan pengajuan pertanggungjawaban kas besar.
	 *
	 * <p><b>Tujuan:</b> Membuat instance dengan mode persetujuan dinonaktifkan
	 * ({@code persetujuan=false}). Mode ini digunakan saat halaman diakses oleh
	 * staf yang mengajukan pertanggungjawaban, bukan oleh penyetuju.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link Common#getCurrentUser()} untuk
	 * mengambil pengguna aktif dari sesi. Pengguna ini akan dipakai sebagai
	 * {@code dibuatOleh} pada entitas {@link ais.database.model.akunting.PertangungjawabanKasBesar}
	 * yang baru dibuat.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika nilai default {@code persetujuan} perlu diubah,
	 * ubah di sini dan di konstruktor berparameter.</p>
	 */
	public PertangungjawabanKasBesarAction() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Konstruktor dengan flag mode persetujuan eksplisit.
	 *
	 * <p><b>Tujuan:</b> Membuat instance yang berperilaku sebagai layar persetujuan
	 * ({@code persetujuan=true}) atau layar pengajuan ({@code persetujuan=false}).
	 * Perbedaan perilaku antara kedua mode:
	 * <ul>
	 *   <li><b>Mode pengajuan (false):</b> Tombol Tambah terlihat, form dapat diisi
	 *       penuh, status default PENGAJUAN.</li>
	 *   <li><b>Mode persetujuan (true):</b> Tombol Tambah tersembunyi, form
	 *       hanya-baca kecuali radio status, penyetuju dapat memilih DISETUJU/DITOLAK.</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Cara kerja:</b> Menetapkan {@code this.persetujuan} sebelum ZK melakukan
	 * wire komponen. Nilai ini juga dapat di-override oleh parameter URL
	 * {@code ?persetujuan=true} yang diproses di {@link #doAfterCompose(Component)}.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Konstruktor ini dipakai oleh modul SOP alur keuangan
	 * yang menginstansiasi controller secara programatik. Perubahan pada logika mode
	 * persetujuan harus diuji di kedua jalur instansiasi.</p>
	 *
	 * @param persetujuan {@code true} jika halaman dibuka sebagai layar persetujuan,
	 *                    {@code false} untuk layar pengajuan biasa
	 */
	public PertangungjawabanKasBesarAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Memeriksa keamanan sesi sebelum komponen ZUL mulai di-compose.
	 *
	 * <p><b>Tujuan:</b> Mencegah akses tidak sah ke halaman Pertanggungjawaban
	 * Kas Besar dengan memvalidasi token keamanan sebelum ZK memulai proses
	 * composing komponen.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link Common#doCheckSecurity()} yang
	 * memeriksa atribut sesi {@code usersTemp}. Jika tidak valid, pengguna akan
	 * di-redirect ke halaman login. Kemudian memanggil implementasi super agar
	 * proses compose ZK tetap berjalan normal.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini dipanggil oleh framework ZK sebelum
	 * {@link #doAfterCompose(Component)}. Jangan tambahkan logika bisnis di sini;
	 * gunakan {@link #doAfterCompose(Component)} untuk inisialisasi komponen.</p>
	 *
	 * @param page     halaman ZK yang sedang dicompose
	 * @param parent   komponen induk (bisa null jika halaman root)
	 * @param compInfo metadata komponen dari ZUL parser
	 * @return {@code ComponentInfo} dari super class untuk dilanjutkan ke pipeline ZK
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Menginisialisasi seluruh komponen UI setelah ZK selesai melakukan wire pada ZUL.
	 *
	 * <p><b>Tujuan:</b> Mempersiapkan halaman Pertanggungjawaban Kas Besar agar siap
	 * digunakan: mulai dari validasi sesi, inisialisasi filter tanggal, pengisian
	 * combobox status, pengecekan hak akses CRUD, hingga menambahkan tombol
	 * "Hitung Ulang" ke toolbar.</p>
	 *
	 * <p><b>Cara kerja — langkah-langkah inisialisasi:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} agar ZK melakukan auto-wire
	 *       semua field komponen dari ZUL (grid, paging, searchparent, dll).</li>
	 *   <li>Memanggil {@link Common#initLaguage()} untuk menetapkan bahasa UI.</li>
	 *   <li>Memvalidasi sesi ({@code usersTemp}) dan hak akses READ. Jika tidak valid,
	 *       memanggil {@link Common#goLogoff()} dan keluar dari metode.</li>
	 *   <li>Memasang listener pada {@code searchparent} (satuan kerja filter) agar
	 *       memicu {@link #onSearchDefault(Event)} saat nilai berubah.</li>
	 *   <li>Membuat {@link SatuanKerjaTreeModel} untuk hierarki satuan kerja.</li>
	 *   <li>Mengatur tanggal default filter: 6 bulan lalu hingga besok hari ini.</li>
	 *   <li>Mengisi combobox {@code searchstatus} dengan pilihan Semua/PENGAJUAN/
	 *       DISETUJU/DITOLAK.</li>
	 *   <li>Memeriksa parameter URL {@code ?persetujuan=true} untuk override mode
	 *       persetujuan dari konstruktor.</li>
	 *   <li>Mengatur visibilitas tombol Tambah dan hak akses edit/delete berdasarkan
	 *       {@link CommonPrivilages}.</li>
	 *   <li>Menambahkan tombol "Hitung Ulang" ke toolbar yang menjalankan kalkulasi
	 *       ulang massal untuk semua data via timer ZK.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika field ZUL baru ditambahkan (misalnya filter baru),
	 * pastikan field tersebut di-guard dengan null-check sebelum diakses di sini,
	 * karena ZUL mode persetujuan mungkin tidak memiliki komponen yang sama dengan
	 * ZUL mode pengajuan.</p>
	 *
	 * @param comp komponen root ZUL yang telah di-compose oleh ZK framework
	 * @throws Exception jika inisialisasi gagal, misalnya database tidak tersedia
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		// Tab "Dasbor": render dasbor pemantauan pertanggungjawaban kas besar.
		if (dasborKasBesarBox != null) {
			try {
				ais.action.master.akunting.helper.DasboardPertangungjawabanKasBesar.render(dasborKasBesarBox);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (searchparent == null) return;
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

		Comboitem comboitem = new Comboitem(PertangungjawabanKasBesar.PENGAJUAN);
		if (comboitem != null) { comboitem.setValue(PertangungjawabanKasBesar.PENGAJUAN); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(PertangungjawabanKasBesar.DISETUJU);
		if (comboitem != null) { comboitem.setValue(PertangungjawabanKasBesar.DISETUJU); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(PertangungjawabanKasBesar.DITOLAK);
		if (comboitem != null) { comboitem.setValue(PertangungjawabanKasBesar.DITOLAK); }
		searchstatus.appendChild(comboitem);

		if (searchstatus != null) { searchstatus.setSelectedItem(comboitemSemua); }
		if (searchstatus != null) { searchstatus.setReadonly(true); }

		if (execution.getParameter("persetujuan") != null) {
			boolean persetujuanDariUrl = Boolean.parseBoolean(execution.getParameter("persetujuan"));
			// Parameter URL TIDAK BOLEH menaikkan mode dari pengajuan ke persetujuan --
			// hanya menu Persetujuan (konstruktor super(true), lihat
			// PersetujuanPertangungjawabanKasBesarAction) atau hak APPROVE eksplisit pada
			// menu aktif yang boleh mengaktifkannya. Mencegah eskalasi via ?persetujuan=true
			// di menu Pertanggungjawaban Kas Besar biasa.
			persetujuan = persetujuanDariUrl
					? (persetujuan || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE))
					: false;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE) && !persetujuan);
		}

		if (add != null) { add.setTooltiptext("Tambah"); }
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "kasBesar", "formula", "nilai",
				"dariSponsor", "namaSponsor", "dibuatOleh", "disetujuiOleh", "tanggalPembuatan", "tanggalPersetujuan",
				"status", "disposisiSop", "daftarPengajuanTransfer.prosesTransfer.kode",
				"daftarPengajuanTransfer.prosesTransfer.nama",
				"daftarPengajuanTransfer.prosesTransfer.tanggalPembuatan",
				"daftarPengajuanTransfer.prosesTransfer.disetujuiOleh",
				"daftarPengajuanTransfer.prosesTransfer.tanggalPersetujuan",
				"daftarPengajuanTransfer.prosesTransfer.realisasikanOleh",
				"daftarPengajuanTransfer.prosesTransfer.tanggalRealisasikan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PertangungjawabanKasBesar.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PertangungjawabanKasBesar.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		// add bisa null bila pengguna tak punya hak tambah (Common.tambahData mengembalikan null).
		if (add != null) {
			if (persetujuan) {
				add.setVisible(false);
			} else {
				add.setLabel("Pengajuan Pertanggungjawaban Kas Besar");
			}
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hitung Ulang", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<PertangungjawabanKasBesar> pertangungjawabanKasBesars = initCriteria(false)
								.addOrder(Order.asc("id")).setMaxResults(5000).list();

						for (PertangungjawabanKasBesar pertangungjawabanKasBesar : pertangungjawabanKasBesars) {

							try {
								if (pertangungjawabanKasBesar.getDaftarPengajuanTransfer() == null
										&& pertangungjawabanKasBesar.getDisetujuiOleh() != null) {
									Session session = HibernateUtil.currentSession();
									DaftarPengajuanTransfer d = (DaftarPengajuanTransfer) session
											.createCriteria(DaftarPengajuanTransfer.class)
											.createAlias("disposisiSop", "disposisiSop", Criteria.LEFT_JOIN)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
													Restrictions.eq("disposisiSop.aktif", true)))
											.addOrder(Order.desc("id")).add(Restrictions.eq("pertangungjawabanKasBesar",
													pertangungjawabanKasBesar))
											.setMaxResults(1).uniqueResult();
									if (d != null) {
										pertangungjawabanKasBesar.setDaftarPengajuanTransfer(d);
										Common.refreshUpdate(session, pertangungjawabanKasBesar);
									}
								}
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}

							Double totalPajak = 0.0;
							Double nilai = 0.0;
							JSONArray array = new JSONArray(pertangungjawabanKasBesar.getFormula());
							for (int i = 0; i < array.length(); i++) {

								JSONObject jsonObject = array.getJSONObject(i);

								try {
									Pajak.buat(null, pertangungjawabanKasBesar, jsonObject, null);
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
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
								totalPajak += pajak;
								Double tot = (jumlah + ((ppn / 100.0) * jumlah)) - (pph_mengurangi_lpj ? pajak : 0.0);

								nilai += tot;

							}

							System.out.println("pajak " + totalPajak + " pertangungjawabanKasBesar "
									+ pertangungjawabanKasBesar.getPajak() + " nilai " + nilai
									+ " pertangungjawabanKasBesar " + pertangungjawabanKasBesar.getNilai());

							if (nilai.intValue() != pertangungjawabanKasBesar.getNilai().intValue()
									|| totalPajak.intValue() != pertangungjawabanKasBesar.getPajak().intValue()) {

								Double dikembalikan = ((pertangungjawabanKasBesar == null ? 0.0
										: pertangungjawabanKasBesar.getNilai())
										+ (pertangungjawabanKasBesar.getDariSponsor())) - nilai;

								pertangungjawabanKasBesar.setPajak(totalPajak);
								pertangungjawabanKasBesar.setNilai(nilai);
								pertangungjawabanKasBesar.setDikembalikan(dikembalikan);
								Session session = HibernateUtil.currentSession();
								Common.refreshUpdate(session, pertangungjawabanKasBesar);
							}

						}
						onSearchDefault(null);
					}
				});

			}

		});
		if (button != null) { button.setParent(add.getParent()); }
	}

	/**
	 * Renderer baris grid untuk daftar {@link ais.database.model.akunting.PertangungjawabanKasBesar}.
	 *
	 * <p><b>Untuk apa:</b> Kelas inner ini bertanggung jawab atas rendering setiap baris
	 * pada grid daftar pertanggungjawaban kas besar. Setiap baris menampilkan: kode/nama/kas besar
	 * yang dipertanggungjawabkan, pemohon dan tanggal, status dan penyetuju, sponsor (jika ada),
	 * tabel rincian biaya per baris JSON, nilai pengembalian, keterangan/alur SOP, status aktif,
	 * dan tombol aksi (edit/delete/cetak).</p>
	 *
	 * <p><b>Pemeliharaan:</b> Urutan penambahan komponen ke {@code arg0} (Row) harus selalu
	 * konsisten dengan jumlah kolom yang didefinisikan di ZUL. Jika kolom baru ditambahkan
	 * di ZUL, renderer ini harus diperbarui dengan menambahkan komponen di posisi yang tepat.</p>
	 */
	class PertangungjawabanKasBesarRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data pertanggungjawaban kas besar ke dalam komponen ZK.
		 *
		 * <p><b>Tujuan:</b> Mengubah entitas {@link ais.database.model.akunting.PertangungjawabanKasBesar}
		 * menjadi komponen UI ZK yang siap ditampilkan di grid, termasuk tabel rincian biaya
		 * inline, badge status, link ke proses transfer terkait, dan tombol aksi.</p>
		 *
		 * <p><b>Cara kerja:</b>
		 * <ol>
		 *   <li>Menetapkan {@code dibuatOleh} jika belum ada (untuk data lama tanpa pembuat).</li>
		 *   <li>Jika sudah disetujui tetapi {@code DaftarPengajuanTransfer} belum dibuat,
		 *       menjalankan {@code DaftarPengajuanTransfer.simpanPertangungjawabanKasBesar}
		 *       via timer ZK (lazy creation).</li>
		 *   <li>Menampilkan kode (dengan revisi history via {@code RevisiHelper}), link ke
		 *       proses transfer terkait (jika ada), nama, dan kode kas besar.</li>
		 *   <li>Menampilkan pembuat dan tanggal pembuatan.</li>
		 *   <li>Menampilkan status, penyetuju, dan tanggal persetujuan.</li>
		 *   <li>Menampilkan nama sponsor dan nilai dari sponsor (jika ada).</li>
		 *   <li>Merender tabel rincian biaya inline dari {@code formula} JSONArray:
		 *       kolom Keterangan/Qty/Harga/PPN/PPH/Jumlah dengan footer total.</li>
		 *   <li>Menampilkan nilai dikembalikan dan keterangan/link SOP.</li>
		 *   <li>Menampilkan status aktif atau checkbox aktif (khusus mode persetujuan
		 *       yang belum disetuju).</li>
		 *   <li>Menambahkan tombol Edit/Delete/Cetak via {@code Common.copyEditDeleteButtons}.</li>
		 * </ol>
		 * </p>
		 *
		 * <p><b>Pemeliharaan:</b> Tabel rincian biaya di sini bersifat read-only (hanya tampil).
		 * Untuk edit rincian biaya, gunakan {@link #reloadDataFormula(Row, JSONArray)} yang
		 * menyediakan komponen input interaktif.</p>
		 *
		 * @param arg0 baris ZK ({@code Row}) yang akan diisi komponen
		 * @param arg1 objek data {@link ais.database.model.akunting.PertangungjawabanKasBesar}
		 * @throws Exception jika terjadi error saat parsing JSON atau akses database
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PertangungjawabanKasBesar pertangungjawabanKasBesar = (PertangungjawabanKasBesar) arg1;

			if (pertangungjawabanKasBesar.getDibuatOleh() == null) {
				pertangungjawabanKasBesar.setDibuatOleh(tbmuser);
			}

			if (pertangungjawabanKasBesar.getDisetujuiOleh() != null
					&& pertangungjawabanKasBesar.getDaftarPengajuanTransfer() == null) {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (pertangungjawabanKasBesar.getStatus().equals(KasBesar.DISETUJU)) {
							DaftarPengajuanTransfer.simpanPertangungjawabanKasBesar(pertangungjawabanKasBesar);
						}

					}
				});
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PertangungjawabanKasBesar.class, pertangungjawabanKasBesar,
					pertangungjawabanKasBesar.getKode() == null ? ""
							: pertangungjawabanKasBesar.getKode().trim().toString()))
					.setParent(arg0);

			if (pertangungjawabanKasBesar.getDaftarPengajuanTransfer() != null
					&& pertangungjawabanKasBesar.getDaftarPengajuanTransfer().getProsesTransfer() != null) {

				A aaa = new A(pertangungjawabanKasBesar.getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				aaa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, pertangungjawabanKasBesar.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				aaa.setStyle("font-size:12px;");
				aaa.setParent(a);
			}

			new Label(pertangungjawabanKasBesar.getNama()).setParent(a);

			new Label(pertangungjawabanKasBesar.getKasBesar() == null ? ""
					: pertangungjawabanKasBesar.getKasBesar().getKode() + "-"
							+ pertangungjawabanKasBesar.getKasBesar().getNama())
					.setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new Label(pertangungjawabanKasBesar.getDibuatOleh() == null ? ""
					: pertangungjawabanKasBesar.getDibuatOleh().getUserNama()).setParent(a);
			new Label(pertangungjawabanKasBesar.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(pertangungjawabanKasBesar.getTanggalPembuatan())).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new Label(pertangungjawabanKasBesar.getStatus()).setParent(a);
			(new Label(pertangungjawabanKasBesar.getDisetujuiOleh() == null ? ""
					: pertangungjawabanKasBesar.getDisetujuiOleh().getUserNama())).setParent(a);
			(new Label(pertangungjawabanKasBesar.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(pertangungjawabanKasBesar.getTanggalPersetujuan()))).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new Label(pertangungjawabanKasBesar.getNamaSponsor().isEmpty() ? ""
					: pertangungjawabanKasBesar.getNamaSponsor()).setParent(a);
			(new Label(pertangungjawabanKasBesar.getNamaSponsor().isEmpty() ? ""
					: Common.numberFormat.get().format(pertangungjawabanKasBesar.getDariSponsor()))).setParent(a);

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
			column.setWidth("10%");

			column = new MyColumnConfig("Harga");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("20%");

			column = new MyColumnConfig("PPN");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("10%");

			column = new MyColumnConfig("PPH");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("20%");

			column = new MyColumnConfig("Jumlah");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("20%");

			Rows rows = new Rows();
			rows.setParent(grid);
			Double nilai = 0.0;
			JSONArray array = new JSONArray(pertangungjawabanKasBesar.getFormula());
			for (int i = 0; i < array.length(); i++) {

				JSONObject jsonObject = array.getJSONObject(i);

				String nama = "";

				if (!jsonObject.isNull("nama")) {
					nama = jsonObject.get("nama") + "";
				}

				String ntpn = "";

				if (!jsonObject.isNull("ntpn")) {
					ntpn = jsonObject.get("ntpn") + "";
				}

				String npwp = "";

				if (!jsonObject.isNull("npwp")) {
					npwp = jsonObject.get("npwp") + "";
				}

				String namaWp = "";

				if (!jsonObject.isNull("namaWp")) {
					namaWp = jsonObject.get("namaWp") + "";
				}

				String tanggalStor = "";

				if (!jsonObject.isNull("tanggalStor")) {
					tanggalStor = jsonObject.get("tanggalStor") + "";
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
				Double nilaippn = ((ppn / 100.0) * jumlah);
				Double tot = (jumlah + nilaippn) - (pph_mengurangi_lpj ? pajak : 0.0);

				nilai += tot;

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				row.appendChild(new MyLabelAgakKecil(nama));
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(qty)));
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(harga)));
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(nilaippn)));
				if (barang != null) {
					row.appendChild(new Vbox(new Component[] { new MyLabelAgakKecil(Common.numberFormat.get().format(pajak)),
							new MyLabelAgakKecil(ntpn), new MyLabelAgakKecil(npwp), new MyLabelAgakKecil(namaWp),
							new MyLabelAgakKecil(tanggalStor)

					}));
				} else {
					row.appendChild(new MyLabelAgakKecil());
				}
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(tot)));
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
			pertangungjawabanKasBesar.setNilai(nilai);
			new Label(Common.numberFormat.get().format(pertangungjawabanKasBesar.getDikembalikan())).setParent(arg0);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new Label(Common.simpleString(pertangungjawabanKasBesar.getKeterangan())).setParent(vbox1);
			if (pertangungjawabanKasBesar.getDisposisiSop() != null) {
				A aa;
				(aa = new A())
						.setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pertangungjawabanKasBesar.getDisposisiSop().getKeterangan() + " ("
						+ pertangungjawabanKasBesar.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pertangungjawabanKasBesar.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
			}

			DaftarPengajuanTransfer.tampilStatus(pertangungjawabanKasBesar.getDaftarPengajuanTransfer(), vbox1);

			if (pertangungjawabanKasBesar.getDisposisiSop() != null
					&& !pertangungjawabanKasBesar.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (persetujuan
					&& !pertangungjawabanKasBesar.getStatus().equals(PertangungjawabanKasBesar.DISETUJU)) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(pertangungjawabanKasBesar.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pertangungjawabanKasBesar.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(pertangungjawabanKasBesar);
					}
				});
			} else {
				new Label(pertangungjawabanKasBesar.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			Hbox hbx;
			(hbx = Common.copyEditDeleteButtons(edit,
					!persetujuan && !pertangungjawabanKasBesar.getStatus().equals(PertangungjawabanKasBesar.DISETUJU),
					delete && !persetujuan
							&& !pertangungjawabanKasBesar.getStatus().equals(PertangungjawabanKasBesar.DISETUJU),
					pertangungjawabanKasBesar, PertangungjawabanKasBesarAction.this)).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(pertangungjawabanKasBesar);
				}
			});
			button.setParent(hbx);
		}

	}

	/**
	 * Menghasilkan file PDF laporan pertanggungjawaban kas besar untuk keperluan ekspor/cetak batch.
	 *
	 * <p><b>Tujuan:</b> Implementasi antarmuka DataCriteria untuk keperluan ekspor data
	 * oleh {@code Common.cetakData}. Metode ini dipanggil ketika pengguna mengklik tombol
	 * cetak pada toolbar (bukan tombol cetak per-baris). Menghasilkan {@link File} PDF
	 * sementara yang kemudian akan dikirim sebagai unduhan ke browser.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Meng-cast {@code generalValueObject} ke {@link ais.database.model.akunting.PertangungjawabanKasBesar}.</li>
	 *   <li>Membuat instance {@link LaporanPertangungjawabanKasBesar} yang merupakan
	 *       generator laporan JasperReports khusus untuk entitas ini.</li>
	 *   <li>Memanggil {@link ais.action.report.Report#generateFileReport} dengan template
	 *       {@code akunting/pertangungjawabanKasBesar} untuk menghasilkan file PDF.</li>
	 *   <li>Mengembalikan file PDF sementara yang dibuat oleh JasperReports engine.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Template laporan berada di direktori resources JasperReports
	 * dengan path {@code akunting/pertangungjawabanKasBesar.jrxml}. Perubahan layout laporan
	 * dilakukan di file template tersebut, bukan di kelas ini.</p>
	 *
	 * @param generalValueObject entitas {@link ais.database.model.akunting.PertangungjawabanKasBesar}
	 *                           yang akan dicetak, dicast dari {@link GeneralValueObject}
	 * @return {@link File} PDF sementara yang berisi laporan pertanggungjawaban
	 * @throws Exception jika template JasperReports tidak ditemukan atau terjadi error compile/fill
	 */
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PertangungjawabanKasBesar pertangungjawabanKasBesar = (PertangungjawabanKasBesar) generalValueObject;
		LaporanPertangungjawabanKasBesar buktiPengeluaranKas = new LaporanPertangungjawabanKasBesar(
				pertangungjawabanKasBesar);
		buktiPengeluaranKas.setTitle("Laporan");
		buktiPengeluaranKas.setClosable(true);
		buktiPengeluaranKas.setHeight("90%");
		buktiPengeluaranKas.setWidth("900px");
		buktiPengeluaranKas.setVisible(false);
		File file = Report.generateFileReport(Report.PDF, buktiPengeluaranKas.generateParameter(),
				"akunting/pertangungjawabanKasBesar", ais.ui.util.WaktuUtil.getDate(), null, new Toolbar());
		return file;
	}

	/**
	 * Menampilkan laporan PDF pertanggungjawaban kas besar secara langsung di browser sebagai modal.
	 *
	 * <p><b>Tujuan:</b> Metode statis ini digunakan untuk mencetak dan langsung menampilkan
	 * laporan pertanggungjawaban kas besar kepada pengguna di dalam window modal ZK. Berbeda
	 * dengan {@link #cetakData(GeneralValueObject)} yang menghasilkan file unduhan, metode ini
	 * membuka window modal di halaman ZK yang sedang aktif.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuat instance {@link LaporanPertangungjawabanKasBesar} dengan entitas yang diberikan.</li>
	 *   <li>Mengkonfigurasi window laporan: judul "Laporan", dapat ditutup, tinggi 90%, lebar 900px.</li>
	 *   <li>Menambahkan window sebagai anak dari root komponen halaman aktif via
	 *       {@link org.zkoss.zk.ui.sys.ExecutionsCtrl}.</li>
	 *   <li>Memanggil {@code onModal()} untuk menampilkan sebagai dialog modal yang
	 *       memblokir interaksi background sampai ditutup.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini bersifat statis agar dapat dipanggil dari renderer
	 * baris grid maupun dari {@link #onSave(Event)} tanpa memerlukan referensi ke instance
	 * controller. Pastikan dipanggil dari UI thread ZK, bukan dari thread latar belakang.</p>
	 *
	 * @param pertangungjawabanKasBesar entitas yang akan dicetak; tidak boleh {@code null}
	 * @throws Exception jika terjadi error pada JasperReports atau komponen ZK
	 */
	public static void cetak(PertangungjawabanKasBesar pertangungjawabanKasBesar) throws Exception {
		LaporanPertangungjawabanKasBesar buktiPengeluaranKas = new LaporanPertangungjawabanKasBesar(
				pertangungjawabanKasBesar);
		buktiPengeluaranKas.setTitle("Laporan");
		buktiPengeluaranKas.setClosable(true);
		buktiPengeluaranKas.setHeight("90%");
		buktiPengeluaranKas.setWidth("900px");
		buktiPengeluaranKas.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		buktiPengeluaranKas.onModal();
	}

	/**
	 * Membuka form edit/lihat pertanggungjawaban kas besar yang ada dari external trigger.
	 *
	 * <p><b>Tujuan:</b> Implementasi antarmuka {@link DataInitDefault} yang dipanggil oleh
	 * framework SOP atau mekanisme edit baris grid. Metode ini menerima entitas yang sudah ada,
	 * menginisialisasi form dengan data entitas tersebut, lalu menampilkan window modal.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Meng-cast {@code obj} ke {@link ais.database.model.akunting.PertangungjawabanKasBesar}
	 *       dan menyimpan ke field instance.</li>
	 *   <li>Memanggil {@link #init(PertangungjawabanKasBesar)} (overload private) untuk membangun
	 *       ulang konten window berdasarkan data entitas.</li>
	 *   <li>Menampilkan dan mengaktifkan modal pada {@code addWindow}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini berbeda dari {@link #onAdd(Event)} yang membuat entitas
	 * baru. Pastikan {@code addWindow} sudah tersedia (tidak null) sebelum metode ini dipanggil.
	 * Dalam mode external (dipanggil dari modul lain), {@code addWindow} dibuat secara programatik
	 * di {@link #onAddExternal(EventListener, PertangungjawabanKasBesar)}.</p>
	 *
	 * @param obj entitas {@link ais.database.model.akunting.PertangungjawabanKasBesar} yang akan
	 *            ditampilkan/diedit; tidak boleh {@code null}
	 * @throws Exception jika terjadi error pada inisialisasi form atau komponen ZK
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pertangungjawabanKasBesar = (PertangungjawabanKasBesar) obj;
		init(pertangungjawabanKasBesar);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Membuka pertanggungjawaban kas besar tertentu dalam mode lihat-saja (view-only) dari modul lain.
	 *
	 * <p><b>Tujuan:</b> Menyediakan cara bagi modul lain (misalnya proses transfer atau alur SOP)
	 * untuk menampilkan detail pertanggungjawaban kas besar dalam window modal terpisah tanpa
	 * memerlukan halaman terpisah dan tanpa kemampuan edit.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuat instance controller baru {@code PertangungjawabanKasBesarAction} yang terisolasi
	 *       dari controller halaman utama.</li>
	 *   <li>Membuat {@link MyWindow} baru dan menetapkan mode persetujuan dan view-only.</li>
	 *   <li>Menambahkan window ke root komponen halaman aktif.</li>
	 *   <li>Mengatur dimensi window (95% tinggi, 550px lebar) dan memanggil {@code init()}
	 *       untuk mengisi konten dengan data pertanggungjawaban.</li>
	 *   <li>Menampilkan window sebagai modal yang dapat ditutup.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Parameter {@code eventListener} saat ini tidak digunakan di dalam
	 * implementasi ini (hanya ada untuk konsistensi tanda tangan metode). Jika perlu callback
	 * saat window ditutup, tambahkan listener pada event close window.</p>
	 *
	 * @param eventListener listener opsional untuk callback (saat ini tidak dipakai)
	 * @param pertangungjawabanKasBesar entitas yang akan ditampilkan; tidak boleh {@code null}
	 * @throws Exception jika terjadi error pada pembuatan komponen ZK atau inisialisasi form
	 */
	public static void onAddExternal(EventListener eventListener, PertangungjawabanKasBesar pertangungjawabanKasBesar)
			throws Exception {
		PertangungjawabanKasBesarAction pertangungjawabanKasBesarAction = new PertangungjawabanKasBesarAction();
		pertangungjawabanKasBesarAction.addWindow = new MyWindow();
		pertangungjawabanKasBesarAction.persetujuan = true;
		pertangungjawabanKasBesarAction.setujui = true;
		pertangungjawabanKasBesarAction.viewOnly = true;

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
				.appendChild(pertangungjawabanKasBesarAction.addWindow);
		pertangungjawabanKasBesarAction.addWindow.setHeight("95%");
		pertangungjawabanKasBesarAction.addWindow.setWidth("550px");

		pertangungjawabanKasBesarAction.init(pertangungjawabanKasBesar);

		pertangungjawabanKasBesarAction.addWindow.setVisible(true);
		pertangungjawabanKasBesarAction.addWindow.setClosable(true);
		pertangungjawabanKasBesarAction.addWindow.onModal();

	}

	/**
	 * Membuka form untuk membuat pertanggungjawaban kas besar baru.
	 *
	 * <p><b>Tujuan:</b> Event handler untuk tombol "Tambah"/"Pengajuan Pertanggungjawaban
	 * Kas Besar" di toolbar. Menginisialisasi form kosong dengan entitas baru dan membuka
	 * window modal untuk diisi oleh pengguna.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mereset flag {@code viewOnly} ke {@code false} agar form dapat diedit penuh.</li>
	 *   <li>Memanggil {@link #init(PertangungjawabanKasBesar)} dengan entitas baru
	 *       {@code new PertangungjawabanKasBesar()} yang belum memiliki ID.</li>
	 *   <li>Menampilkan dan mengaktifkan modal pada {@code addWindow}.</li>
	 *   <li>Menangkap exception saat {@code onModal()} dipanggil (misalnya jika window
	 *       sudah terbuka) dan menampilkan error jika pengguna adalah admin.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Tombol ini hanya terlihat jika {@code CommonPrivilages.CREATE}
	 * aktif dan mode bukan persetujuan (dicek di {@link #doAfterCompose(Component)}).
	 * Perubahan pada hak akses tombol dilakukan di sana, bukan di sini.</p>
	 *
	 * @param event event ZK dari klik tombol "Tambah"; dapat diabaikan karena tidak digunakan
	 * @throws Exception jika terjadi error pada inisialisasi entitas atau komponen ZK
	 */
	public void onAdd(Event event) throws Exception {
		viewOnly = false;
		init(new PertangungjawabanKasBesar());
		addWindow.setVisible(true);
		try {
			addWindow.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun dan mengembalikan panel form pertanggungjawaban kas besar sebagai {@link MyGrid}.
	 *
	 * <p><b>Tujuan:</b> Implementasi antarmuka {@link FormSop} yang membangun seluruh UI form
	 * pengisian pertanggungjawaban kas besar. Form ini digunakan baik untuk pengajuan baru,
	 * edit pengajuan yang ada, maupun untuk tampilan persetujuan oleh penyetuju.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menginisialisasi {@code satuanKerjaTreeModel} jika belum ada.</li>
	 *   <li>Mereset akumulator {@code dikembalikan} dan {@code nilaipajak} ke nol.</li>
	 *   <li>Menangani disposisi SOP: mempertahankan disposisi lama jika disposisi baru null/baru.</li>
	 *   <li>Membangun {@link MyGrid} dengan baris-baris form berisi:
	 *       Satuan Kerja, Kas Besar, Kode (auto-generate), Nama Pengajuan, Info Unit,
	 *       Nilai Pengajuan Kas Besar, Diajukan Oleh/Tanggal, Nama Sponsor, Nilai Sponsor,
	 *       Tabel Rincian Biaya (via {@link #reloadFormula}), Nilai Pengembalian,
	 *       Tanggal Stor (jika dikembalikan &gt; 0), Status Pengajuan (radio group),
	 *       dan Keterangan.</li>
	 *   <li>Saat satuan kerja berubah, memperbarui filter kas besar dan kode otomatis.</li>
	 *   <li>Saat kas besar terpilih, memperbarui nama, kode, dan info unit dari kas besar.</li>
	 *   <li>Menambahkan listener pada radio status yang memperbarui label tombol simpan
	 *       sesuai status yang dipilih.</li>
	 *   <li>Menetapkan attribute {@code eventListenerSetuju} pada grid untuk integrasi
	 *       alur SOP checkbox.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Mode tampilan:</b>
	 * <ul>
	 *   <li><b>Mode pengajuan (normal):</b> Semua input dapat diedit.</li>
	 *   <li><b>Mode persetujuan / setujui / viewOnly:</b> Semua input diganti Label
	 *       (hanya baca), hanya radio status yang tetap aktif untuk penyetuju.</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Konfigurasi {@code sponsor_tampil_lpj} (aktif/tidak-aktif)
	 * mengontrol visibilitas baris Nama Sponsor dan Nilai Sponsor. Perubahan field JSON
	 * formula harus diikuti dengan pembaruan di {@link #reloadDataFormula} dan
	 * {@link #reloadFormula}.</p>
	 *
	 * @param generalValueObject entitas {@link ais.database.model.akunting.PertangungjawabanKasBesar}
	 *                           yang akan diisi/ditampilkan di form
	 * @param disposisiSop       konteks alur SOP jika form dibuka dari modul SOP;
	 *                           bisa {@code null} untuk akses langsung
	 * @param save               tombol simpan yang label-nya akan diperbarui sesuai status
	 * @param setujuiData        listener SOP untuk sinkronisasi status persetujuan;
	 *                           bisa {@code null} untuk non-SOP
	 * @return {@link MyGrid} yang berisi seluruh komponen form, siap ditambahkan ke container
	 * @throws Exception jika terjadi error pada query Hibernate atau pembuatan komponen ZK
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {

		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}

		dikembalikan = 0.0;
		nilaipajak = 0.0;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		pertangungjawabanKasBesar = (PertangungjawabanKasBesar) generalValueObject;

		setujui = false;
		if (!persetujuan) {
			if (pertangungjawabanKasBesar != null
					&& pertangungjawabanKasBesar.getStatus().equals(PertangungjawabanKasBesar.DISETUJU)) {
				setujui = true;
			} else {
				setujui = false;
			}
		}

		if (pertangungjawabanKasBesar.getDisposisiSop() != null
				&& pertangungjawabanKasBesar.getDisposisiSop().getDisposisiSetuju() != null
				&& pertangungjawabanKasBesar.getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null
				&& pertangungjawabanKasBesar.getDisposisiSop().getDisposisiSetuju().getSelesai()) {
			viewOnly = true;
		}

		PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();

		SatuanKerja parent = pt == null ? null : pt.getSatuanKerja();

		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		try {
			if (pertangungjawabanKasBesar.getSatuanKerja() == null) {
				pertangungjawabanKasBesar.setSatuanKerja(Common.getSatuanKerja());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PertangungjawabanKasBesarAction.java:1187");
			// TODO: handle exception
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja *"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(pertangungjawabanKasBesar.getSatuanKerja() == null ? ""
				: pertangungjawabanKasBesar.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", pertangungjawabanKasBesar.getSatuanKerja());

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(pertangungjawabanKasBesar.getSatuanKerja() == null ? ""
					: pertangungjawabanKasBesar.getSatuanKerja().getNama()));
		} else {
			row.appendChild(satuanKerja);
		}

		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kas Besar *"));

		kasBesar = new AmbilDataKasBesarBanbox();
		kasBesar.setAttribute("kasBesar", pertangungjawabanKasBesar.getKasBesar());
		kasBesar.setValue(pertangungjawabanKasBesar.getKasBesar() == null ? ""
				: pertangungjawabanKasBesar.getKasBesar().getKode());

		kasBesar.setReadonly(true);
		kasBesar.setWidth("90%");

		satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				kasBesar.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
				if (pertangungjawabanKasBesar.getId() == null) {
					String noAgenda = generateCode(false, (SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
					pertangungjawabanKasBesar.setKode(noAgenda);
					kode.setValue(noAgenda);
				}
			}
		});

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(pertangungjawabanKasBesar.getKasBesar() == null ? ""
					: pertangungjawabanKasBesar.getKasBesar().getKode() + "-"
							+ pertangungjawabanKasBesar.getKasBesar().getNama()));
		} else {
			row.appendChild(kasBesar);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				kasBesar.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		if (pertangungjawabanKasBesar.getId() == null) {
			String noAgenda = generateCode(false, (SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
			pertangungjawabanKasBesar.setKode(noAgenda);
		}

		kode = new Label(pertangungjawabanKasBesar.getKode());
		if (persetujuan) {
			row.appendChild(new Label(pertangungjawabanKasBesar.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		nama = new Textbox(pertangungjawabanKasBesar.getNama());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Pengajuan *"));
		nama.setWidth("90%");

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(pertangungjawabanKasBesar.getNama()));
		} else {
			row.appendChild(nama);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Unit/Satuan Kerja"));
		final Label unit = new Label();
		row.appendChild(unit);

		nilaiHarusDikembalikan = new Label();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Pengajuan Kas Besar *"));
		final Label nilaiPengajuan = new Label();
		row.appendChild(nilaiPengajuan);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				KasBesar work = (KasBesar) (kasBesar.getAttribute("kasBesar"));

				if (work != null && kode.getValue().trim().isEmpty()) {
					kode.setValue(work.getKode());
				}
				if (work != null && nama.getValue().trim().isEmpty()) {
					nama.setValue(work.getNama());
				}

				unit.setValue(work == null || work.getSatuanKerja() == null ? "" : work.getSatuanKerja().getNama());

				nilaiPengajuan.setValue(work == null ? "" : Common.numberFormat.get().format(work.getNilai()));

			}
		};

		kasBesar.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan Oleh"));
		row.appendChild(new Label(pertangungjawabanKasBesar.getDibuatOleh() == null ? ""
				: pertangungjawabanKasBesar.getDibuatOleh().getUserNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan Tanggal"));
		row.appendChild(new Label(pertangungjawabanKasBesar.getTanggalPembuatan() == null ? ""
				: Common.dateFormat1.get().format(pertangungjawabanKasBesar.getTanggalPembuatan())));

		boolean sponsor_tampil_lpj = Common.bolehKonfigurasi("sponsor_tampil_lpj");

		row = new MyFormRow();
		row.setVisible(sponsor_tampil_lpj);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Sponsor"));
		namaSponsor = new Textbox(pertangungjawabanKasBesar.getNamaSponsor());

		namaSponsor.setWidth("90%");

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(pertangungjawabanKasBesar.getNamaSponsor()));
		} else {
			row.appendChild(namaSponsor);
		}

		row = new MyFormRow();
		row.setVisible(sponsor_tampil_lpj);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Sponsor"));
		dariSponsor = new MyDoublebox(pertangungjawabanKasBesar.getDariSponsor());

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(Common.numberFormat.get().format(pertangungjawabanKasBesar.getDariSponsor())));
		} else {
			row.appendChild(dariSponsor);
		}

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelBoldConfig("Rincian Laporan Pertanggung jawaban"));

		tanggalStor = new MyDatebox(pertangungjawabanKasBesar.getTanggalStor());
		tanggalStor.setReadonly(true);

		nilai = 0.0;
		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		array = new JSONArray(pertangungjawabanKasBesar.getFormula());
		rowFormula = Common.tampilanScroll1(row);
		reloadFormula(rowFormula, array);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Pengembalian *"));
		row.appendChild(nilaiHarusDikembalikan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Stor *"));
		if (persetujuan || setujui || viewOnly) {
			row.appendChild(
					new Label(tanggalStor.getValue() == null ? "" : Common.dateFormat3.get().format(tanggalStor.getValue())));
		} else {
			row.appendChild(tanggalStor);
		}
		row.setVisible(dikembalikan > 0.01);

		row = new MyFormRow();
		row.setVisible(persetujuan && !viewOnly && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
		status = new Radiogroup();
		Radio comboitem = new Radio(KasBesar.PENGAJUAN);
		comboitem.setAttribute("value", KasBesar.PENGAJUAN);
		comboitem.setValue(KasBesar.PENGAJUAN);
		comboitem.setVisible(false);
		status.appendChild(comboitem);
		comboitem = new Radio(KasBesar.DISETUJU);
		comboitem.setAttribute("value", KasBesar.DISETUJU);
		comboitem.setValue(KasBesar.DISETUJU);
		status.appendChild(comboitem);
		comboitem = new Radio(KasBesar.DITOLAK);
		comboitem.setAttribute("value", KasBesar.DITOLAK);
		comboitem.setValue(KasBesar.DITOLAK);
		status.appendChild(comboitem);
		status.setWidth("90%");
		Common.selectRadioItem(status, pertangungjawabanKasBesar.getStatus());
		row.appendChild(status);

		grid.setAttribute("eventListenerSetuju", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getTarget() instanceof Checkbox) {
					Checkbox checkbox = (Checkbox) arg0.getTarget();
					Boolean selesai = (Boolean) checkbox.getAttribute("checkbox");
					if (selesai != null && selesai) {
						Common.selectRadioItem(status, KasBesar.DISETUJU);
						Common.freeze(status, true);
					} else {
						status.setSelectedItem(null);
						Common.freeze(status, false);
					}
				}
			}
		});

		if (setujuiData != null) {
			status.addEventListener("onClick", setujuiData);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					setujuiData.onEvent(
							new Event("", null, pertangungjawabanKasBesar.getStatus().equals(KasBesar.DISETUJU)));
				}
			});
		}

		if (setujui || viewOnly) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
			row.appendChild(new ais.ui.util.MyLabelConfig(pertangungjawabanKasBesar.getStatus()));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		keterangan = new Textbox(
				pertangungjawabanKasBesar.getKeterangan() == null ? "" : pertangungjawabanKasBesar.getKeterangan());

		if (setujui) {
			row.appendChild(new Label(pertangungjawabanKasBesar.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}

		keterangan.setWidth("90%");
		keterangan.setRows(3);

		EventListener s = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				boolean setujui = status.getSelectedItem() == null ? false
						: status.getSelectedItem().getValue().equals(DanaTalangan.DISETUJU);

				if (setujui) {
					if (dikembalikan > 0.1) {
						save.setLabel("Kas Besar Selesai");
					} else {
						save.setLabel("Pertanggungjawaban Selesaikan");
					}
				} else {
					save.setLabel(!persetujuan ? "Ajukan dan Cetak" : "Ubah Status Persetujuan dan Cetak");
				}
			}
		};

		status.addEventListener("onClick", s);
		Common.createDefaultTimer(s);

		Common.createDefaultTimer(eventListener);

		return grid;
	}

	/**
	 * Membangun ulang grid rincian biaya interaktif dari JSONArray formula ke dalam Row ZK.
	 *
	 * <p><b>Tujuan:</b> Merender ulang seluruh tabel rincian biaya pertanggungjawaban dalam
	 * mode interaktif (ada input field untuk edit) atau mode baca-saja (Label). Setiap item
	 * JSON dalam array dirender sebagai satu baris grid dengan field: keterangan biaya,
	 * jenis PPh, qty, harga, DPP, PPN, potongan PPh, dan jumlah bersih.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membersihkan semua komponen anak dari {@code rowU} via {@link Common#clear(Row)}.</li>
	 *   <li>Membuat grid baru dengan kolom: Keterangan Biaya, PPh Pasal, Qty, Harga, DPP,
	 *       PPN, Pot.PPH, Jumlah, Hps (hapus), dan kolom aksi.</li>
	 *   <li>Membuat {@code hitungTotal} EventListener yang menghitung ulang total bersih,
	 *       nilai dikembalikan, dan memperbarui label {@code nilaiHarusDikembalikan}.</li>
	 *   <li>Iterasi {@code array}: untuk setiap JSONObject yang memiliki field {@code key}
	 *       (skip item kosong), membuat baris dengan komponen input yang terhubung ke
	 *       listener onChange yang memperbarui JSONObject dan memanggil {@code hitungTotal}.</li>
	 *   <li>Mendaftarkan onChange listener pada semua input (targetText, qtyBox, hargaBox,
	 *       comboboxPajak, persenPpn, ntpnText, npwpText, namaWpText, tanggalStorText).</li>
	 *   <li>Menambahkan tombol "Hapus" per baris yang mengganti JSONObject dengan
	 *       {@code new JSONObject()} (penanda hapus) dan memanggil {@link #reloadDataFormula}.</li>
	 *   <li>Menangani upload lampiran per baris: jika sudah ada {@link LampiranLain},
	 *       tampilkan link; jika ada URL eksternal, tampilkan link; jika belum ada,
	 *       sediakan widget upload.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Urutan kolom grid harus konsisten dengan header yang didefinisikan
	 * di awal metode. Jika field JSON baru ditambahkan, tambahkan juga di {@code hitungTotal}
	 * listener dan di {@link #reloadFormula} / renderer grid daftar.</p>
	 *
	 * @param rowU  baris ZK target ({@code Row}) yang akan diisi grid rincian biaya;
	 *              komponen lamanya dibersihkan sebelum diisi ulang
	 * @param array {@code JSONArray} berisi JSONObject per item biaya dari field {@code formula}
	 *              entitas pertanggungjawaban
	 * @throws Exception jika terjadi error pada query {@link org.hibernate.Session} atau
	 *                   komponen ZK saat membangun baris
	 */
	public void reloadDataFormula(final Row rowU, final JSONArray array) throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		// Lebar kolom dirapikan agar total ≤100% (sebelumnya menjumlah 113% sehingga grid meluap
		// dan kolom Hapus terpotong). Kolom Hapus dilebarkan agar tombolnya muat.
		MyColumnConfig column = new MyColumnConfig("Keterangan Biaya");
		column.setParent(columns);
		column.setWidth("22%");

		column = new MyColumnConfig("Pph Pasal");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("Qty");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("5%");

		column = new MyColumnConfig("Harga");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("DPP");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("PPN");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Pot. PPH");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("8%");

		column = new MyColumnConfig("Jumlah");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("9%");

		column = new MyColumnConfig("Hapus");
		column.setAlign("center");
		column.setParent(columns);
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);

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

		footer = new Footer("");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		final Footer footerTotal = new Footer("");
		foot.appendChild(footerTotal);

		footer = new Footer("");
		foot.appendChild(footer);

		Rows rows = new Rows();
		rows.setParent(grid);

		final EventListener hitungTotal = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				boolean pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");
				nilaipajak = 0.0;
				dikembalikan = 0.0;
				Double nilai = 0.0;
				for (int i = 0; i < array.length(); i++) {
					Double jumlah = 0.0;
					JSONObject jsonObject = array.getJSONObject(i);
					if (!jsonObject.isNull("jumlah")) {
						jumlah = jsonObject.getDouble("jumlah");
					}

					Double ppn = 0.0;
					if (!jsonObject.isNull("ppn")) {
						ppn = jsonObject.getDouble("ppn");
					}

					JenisPajakBarang barang;
					if (!jsonObject.isNull("pajak")) {
						barang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
								Long.parseLong(jsonObject.get("pajak") + ""));
					} else {
						barang = null;
					}

					Double pajak = barang == null ? 0.0 : ((barang.getPersen() / 100.0) * jumlah);
					nilaipajak += pajak;
					Double tot = (jumlah + ((ppn / 100.0) * jumlah)) - (pph_mengurangi_lpj ? pajak : 0.0);

					nilai += tot;
				}
				footerTotal.setLabel(Common.numberFormat.get().format(nilai));

				KasBesar work = (KasBesar) (kasBesar.getAttribute("kasBesar"));

				dikembalikan = ((work == null ? 0.0 : work.getNilai())
						+ (dariSponsor.getValue() == null ? 0.0 : dariSponsor.getValue())) - nilai;

				nilaiHarusDikembalikan.setValue(Common.numberFormat.get().format(dikembalikan));

				try {
					tanggalStor.getParent().setVisible(dikembalikan > 0.01);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PertangungjawabanKasBesarAction.java:1668");
					// TODO: handle exception
				}
			}

		};

		hitungTotal.onEvent(null);

		dariSponsor.addEventListener("onChange", hitungTotal);
		boolean pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");

		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);

			Long key;
			if (jsonObject.isNull("key")) {
				continue;
			} else {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
			}

			String nama = "";

			if (!jsonObject.isNull("nama")) {
				nama = jsonObject.get("nama") + "";
			}

			String ntpn = "";

			if (!jsonObject.isNull("ntpn")) {
				ntpn = jsonObject.get("ntpn") + "";
			}

			String npwp = "";

			if (!jsonObject.isNull("npwp")) {
				npwp = jsonObject.get("npwp") + "";
			}

			String namaWp = "";

			if (!jsonObject.isNull("namaWp")) {
				namaWp = jsonObject.get("namaWp") + "";
			}

			String tanggalStor = "";

			if (!jsonObject.isNull("tanggalStor")) {
				tanggalStor = jsonObject.get("tanggalStor") + "";
			}
			Date tglStor = null;
			try {
				tglStor = tanggalStor.isEmpty() ? null : Common.dateFormat1.get().parse(tanggalStor);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PertangungjawabanKasBesarAction.java:1723");
				// TODO: handle exception
			}

			final JenisPajakBarang jenisPajakBarang;
			if (!jsonObject.isNull("pajak")) {
				jenisPajakBarang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
						Long.parseLong(jsonObject.get("pajak") + ""));
			} else {
				jenisPajakBarang = null;
			}

			Double ppn = 0.0;
			if (!jsonObject.isNull("ppn")) {
				ppn = jsonObject.getDouble("ppn");
			}

			final JenisPajakPpn jenisPajakPpn;
			if (!jsonObject.isNull("pajak_ppn")) {
				jenisPajakPpn = (JenisPajakPpn) ConstantValues.ambil(JenisPajakPpn.class.getName(),
						Long.parseLong(jsonObject.get("pajak_ppn") + ""));
			} else {
				jenisPajakPpn = ppn.intValue() == 11 ? JenisPajakPpn.PPN : null;
			}

			Double qty = 0.0;
			if (!jsonObject.isNull("qty")) {
				qty = jsonObject.getDouble("qty");
			}

			Double harga = 0.0;
			if (!jsonObject.isNull("harga")) {
				harga = jsonObject.getDouble("harga");
			}

			Double jumlah = qty * harga;

			Double pajak_nilai = jenisPajakBarang == null ? 0.0 : ((jenisPajakBarang.getPersen() / 100.0) * jumlah);

			String nama_file = "";

			if (!jsonObject.isNull("nama_file")) {
				nama_file = jsonObject.get("nama_file") + "";
			}

			String link = "";

			if (!jsonObject.isNull("link")) {
				link = jsonObject.get("link") + "";
			}
			Double nilaippn = ((ppn / 100.0) * jumlah);
			Double tot = (jumlah + nilaippn) - (pph_mengurangi_lpj ? pajak_nilai : 0.0);

			MyFormRow rowData = new MyFormRow();
			rowData.setValign("top");

			rowData.setParent(rows);
			final Combobox comboboxPajak = new Combobox();
			final Label nilaiDpp = new Label(Common.numberFormat.get().format(jumlah));

			final Label total = new Label(Common.numberFormat.get().format(tot));

//			final MyDoublebox ppnData = new MyDoublebox(ppn);

			final Combobox persenPpn = new Combobox();
			Common.insertComboDanSemua(persenPpn, new String[] { "nama" }, "keterangan", JenisPajakPpn.class,
					"Tanpa PPN", Restrictions.eq("aktif", true));
			Common.selectComboItem(persenPpn, jenisPajakPpn);

			final MyTextbox targetText = new MyTextbox(nama);

			Vbox myvbox = new Vbox();
			myvbox.setParent(rowData);
			myvbox.setWidth("95%");

			final MyDoublebox qtyBox = new MyDoublebox(qty);
			final MyDoublebox hargaBox = new MyDoublebox(harga);

			targetText.setWidth("95%");
			qtyBox.setWidth("95%");
			hargaBox.setWidth("95%");
			persenPpn.setWidth("85%");

			Common.insertComboDanSemua(comboboxPajak, new String[] { "nama", "persen" }, "keterangan",
					JenisPajakBarang.class, "Tanpa Pajak", Restrictions.eq("aktif", true));
			Common.selectComboItem(comboboxPajak, jenisPajakBarang);
			comboboxPajak.setWidth("95%");

			final Label pajak_nilaiBox = new Label(Common.numberFormat.get().format(pajak_nilai));
			pajak_nilaiBox.setWidth("95%");

			final Label ppn_nilaiBox = new Label(Common.numberFormat.get().format(nilaippn));
			ppn_nilaiBox.setWidth("95%");

			final MyTextbox ntpnText = new MyTextbox(ntpn);
			ntpnText.setWidth("85%");
			final MyTextbox npwpText = new MyTextbox(npwp);
			npwpText.setWidth("85%");
			final MyTextbox namaWpText = new MyTextbox(namaWp);
			namaWpText.setWidth("85%");

			final MyDatebox tanggalStorText = new MyDatebox(tglStor);
			tanggalStorText.setWidth("85%");
			tanggalStorText.setReadonly(true);

			if (persetujuan || setujui || viewOnly) {
				myvbox.appendChild(new Label(nama));

				if (jenisPajakBarang != null) {
					Vbox aa;
					rowData.appendChild(aa = new Vbox(new Component[] {
							new MyLabelAgakKecil(jenisPajakBarang == null ? "" : jenisPajakBarang.getNama()),
							new MyLabelAgakKecil(ntpn), new MyLabelAgakKecil(npwp), new MyLabelAgakKecil(namaWp),
							new MyLabelAgakKecil(tanggalStor) }));
					aa.setWidth("100%");
				} else {
					rowData.appendChild(
							new MyLabelAgakKecil(jenisPajakBarang == null ? "" : jenisPajakBarang.getNama()));
				}

				rowData.appendChild(new Label(Common.numberFormat.get().format(qty)));
				rowData.appendChild(new Label(Common.numberFormat.get().format(harga)));
				rowData.appendChild(nilaiDpp);
				rowData.appendChild(ppn_nilaiBox);
				rowData.appendChild(new Label(Common.numberFormat.get().format(pajak_nilai)));
			} else {
				myvbox.appendChild(targetText);

				Vbox aa;
				rowData.appendChild(aa = new Vbox(new Component[] { comboboxPajak,
						new Hbox(new Component[] { new MyLabelAgakKecil("NTPN"), ntpnText }),
						new Hbox(new Component[] { new MyLabelAgakKecil("NPWP"), npwpText }),
						new Hbox(new Component[] { new MyLabelAgakKecil("Nama WP"), namaWpText }),
						new Hbox(new Component[] { new MyLabelAgakKecil("Tgl Stor"), tanggalStorText }) }));
				aa.setWidth("100%");

				rowData.appendChild(qtyBox);
				rowData.appendChild(hargaBox);
				rowData.appendChild(nilaiDpp);
				Vbox a;
				rowData.appendChild(a = new Vbox(new Component[] { persenPpn, ppn_nilaiBox }));
				a.setWidth("90%");
				rowData.appendChild(pajak_nilaiBox);
			}

			Long id_file = null;

			if (!jsonObject.isNull("id_file")) {
				id_file = Long.parseLong(jsonObject.get("id_file") + "");
			}

			final LampiranLain lampiranLain = id_file != null ? LampiranLain.ambil(true, id_file, "id")
					: LampiranLain.ambil(key, "Dokumen PertangungjawabanKasBesar");

			if (lampiranLain != null) {

				A a = new A(lampiranLain.getNama());
				a.setParent(myvbox);
				a.setWidth("95%");

				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.display(lampiranLain);
					}
				});

			}

			else if (!nama_file.isEmpty() && !link.isEmpty()) {

				A a = new A(nama_file);
				a.setParent(myvbox);
				a.setWidth("95%");
				final String url = link;
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Clients.evalJavaScript("popupCenter({url: '" + url + "', title: 'Data', w: 1200, h: 600});");
					}
				});

			} else {

				Hbox hbox = new Hbox();
				hbox.setParent(myvbox);
				LampiranLain.createDownloadUploadFileLain(hbox, key, "Dokumen PertangungjawabanKasBesar", "Bukti",
						false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								LampiranLain lampiranLain = (LampiranLain) arg0.getData();
								jsonObject.put("link", lampiranLain.createLinkUri(false));
								jsonObject.put("nama_file", lampiranLain.getNama());
								jsonObject.put("id_file", lampiranLain.getId());
							}
						}, null, false, false, false, !(persetujuan || setujui || viewOnly));
			}

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					boolean pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");
					JenisPajakBarang barang = (JenisPajakBarang) (comboboxPajak.getSelectedItem() == null ? null
							: comboboxPajak.getSelectedItem().getValue());

					ntpnText.getParent().setVisible(barang != null);
					npwpText.getParent().setVisible(barang != null);
					namaWpText.getParent().setVisible(barang != null);
					tanggalStorText.getParent().setVisible(barang != null);

					jsonObject.put("ntpn", ntpnText.getValue());
					jsonObject.put("npwp", npwpText.getValue());
					jsonObject.put("namaWp", namaWpText.getValue());
					jsonObject.put("tanggalStor", tanggalStorText.getValue() == null ? ""
							: Common.dateFormat1.get().format(tanggalStorText.getValue()));

					JenisPajakPpn pajakPpn = (JenisPajakPpn) (persenPpn.getSelectedItem() == null ? null
							: persenPpn.getSelectedItem().getValue());

					jsonObject.put("pajak_ppn", pajakPpn != null ? pajakPpn.getId() : null);

					jsonObject.put("pajak", barang != null ? barang.getId() : null);

					jsonObject.put("nama", targetText.getValue());
					jsonObject.put("qty", qtyBox.getValue());
					jsonObject.put("harga", hargaBox.getValue());

					Double ppn = pajakPpn == null ? 0.0 : pajakPpn.getPersen();
					jsonObject.put("ppn", ppn);

					Double jumlah = (qtyBox.getValue() == null ? 0.0 : qtyBox.getValue())
							* (hargaBox.getValue() == null ? 0.0 : hargaBox.getValue());
					jsonObject.put("jumlah", jumlah);

					Double nilaippn = ((ppn / 100.0) * jumlah);
					jsonObject.put("nilaippn", nilaippn);

					nilaiDpp.setValue(Common.numberFormat.get().format(jumlah));

					Double pajak = barang == null ? 0.0 : ((barang.getPersen() / 100.0) * jumlah);
					pajak_nilaiBox.setValue(Common.numberFormat.get().format(pajak));
					jsonObject.put("pajak_nilai", pajak);

					ppn_nilaiBox.setValue(Common.numberFormat.get().format(nilaippn));

					Double tot = (jumlah + nilaippn) - (pph_mengurangi_lpj ? pajak : 0.0);
					total.setValue(Common.numberFormat.get().format(tot));
					jsonObject.put("total", tot);
					hitungTotal.onEvent(null);
				}
			};

			targetText.setRows(2);

			persenPpn.addEventListener("onChange", eventListener);
			qtyBox.addEventListener("onChange", eventListener);
			targetText.addEventListener("onChange", eventListener);
			hargaBox.addEventListener("onChange", eventListener);
			comboboxPajak.addEventListener("onChange", eventListener);

			ntpnText.addEventListener("onChange", eventListener);
			npwpText.addEventListener("onChange", eventListener);
			namaWpText.addEventListener("onChange", eventListener);
			tanggalStorText.addEventListener("onChange", eventListener);

			if (ntpnText.getParent() != null)
				ntpnText.getParent().setVisible(jenisPajakBarang != null);
			if (npwpText.getParent() != null)
				npwpText.getParent().setVisible(jenisPajakBarang != null);
			if (namaWpText.getParent() != null)
				namaWpText.getParent().setVisible(jenisPajakBarang != null);
			if (tanggalStorText.getParent() != null)
				tanggalStorText.getParent().setVisible(jenisPajakBarang != null);

			rowData.appendChild(total);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											array.put(index, new JSONObject());

											reloadDataFormula(rowU, array);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			if (persetujuan || setujui || viewOnly) {
				new Label().setParent(rowData);
			} else {
				button.setParent(rowData);
			}

		}
	}

	/**
	 * Menambahkan tombol "Tambah Biaya" dan menginisialisasi tampilan rincian biaya pertama kali.
	 *
	 * <p><b>Tujuan:</b> Metode wrapper yang menyiapkan container untuk daftar item biaya dalam
	 * form. Berbeda dengan {@link #reloadDataFormula(Row, JSONArray)} yang merender ulang grid,
	 * metode ini menciptakan struktur awal berupa tombol "Tambah Biaya" di atas grid dan
	 * memanggil {@link #reloadDataFormula} untuk rendering pertama kali.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuat {@link MyFormRow} baru ({@code rowU}) sebagai wadah grid rincian biaya.
	 *       Row ini dipisah dari {@code rowFormula} agar tombol Tambah dan grid berada
	 *       di baris terpisah yang dapat di-refresh secara independen.</li>
	 *   <li>Membuat tombol "Tambah Biaya" yang saat diklik menambahkan JSONObject kosong
	 *       ke {@code array} dengan field default ({@code nama}, {@code qty=0.0},
	 *       {@code harga=0.0}, {@code jumlah=0.0}, dan {@code key} acak) lalu
	 *       memanggil {@link #reloadDataFormula} untuk refresh.</li>
	 *   <li>Tombol tidak terlihat ({@code setVisible(false)}) saat dalam mode persetujuan
	 *       atau setelah disetujui.</li>
	 *   <li>Menambahkan {@code rowU} ke parent dari {@code rowFormula} agar sejajar
	 *       dengan baris form lainnya.</li>
	 *   <li>Memanggil {@link #reloadDataFormula(Row, JSONArray)} untuk merender isi
	 *       grid dari {@code array} yang ada.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini dipanggil sekali dari {@link #form} saat form
	 * pertama kali dirender. Reload selanjutnya (saat hapus item) dilakukan langsung
	 * via {@link #reloadDataFormula}. Key item baru menggunakan {@link Common#randLong()}
	 * untuk memastikan keunikan dalam satu sesi.</p>
	 *
	 * @param rowFormula baris ZK yang berisi tombol Tambah Biaya; grid rincian
	 *                   akan ditempatkan di baris baru setelah baris ini
	 * @param array      {@code JSONArray} yang dimodifikasi secara in-place saat pengguna
	 *                   menambah atau menghapus item biaya
	 * @throws Exception jika terjadi error pada pembuatan komponen ZK atau pemanggilan
	 *                   {@link #reloadDataFormula}
	 */
	public void reloadFormula(final Row rowFormula, final JSONArray array) throws Exception {
		final MyFormRow rowU = new MyFormRow();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Biaya", "/img/svg/addthis.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(!persetujuan && !setujui);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("nama", "");
				jsonObject.put("qty", 0.0);
				jsonObject.put("harga", 0.0);
				jsonObject.put("jumlah", 0.0);
				Long key = Math.abs(Common.randLong());
				jsonObject.put("key", key);
				array.put(jsonObject);

				reloadDataFormula(rowU, array);
			}
		});
		button.setParent(rowFormula);

		rowU.setParent(rowFormula.getParent());

		reloadDataFormula(rowU, array);

	}

	/**
	 * Menyiapkan window form modal untuk pengajuan atau edit pertanggungjawaban kas besar.
	 *
	 * <p><b>Tujuan:</b> Metode private ini adalah inti pembuatan window modal. Dipanggil
	 * baik dari {@link #onAdd(Event)} (entitas baru) maupun dari {@link #init(GeneralValueObject)}
	 * (entitas yang sudah ada). Membangun layout Borderlayout dengan Center (form) dan South
	 * (toolbar tombol), lalu menambahkan logika simpan dan batal.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menetapkan {@code dibuatOleh} dan {@code tanggalPembuatan} jika entitas baru (Id null).</li>
	 *   <li>Mengatur judul window sesuai mode: "Pengajuan Pertanggungjawaban" atau
	 *       "Persetujuan Pertanggungjawaban".</li>
	 *   <li>Membersihkan konten window lama dan membuat {@link org.zkoss.zul.Borderlayout} baru.</li>
	 *   <li>Di bagian Center: memanggil {@link #form(GeneralValueObject, DisposisiSop, MyToolbarbuttonConfig, EventListener)}
	 *       untuk membangun grid form.</li>
	 *   <li>Di bagian South: menambahkan toolbar dengan tombol Batal (menutup window)
	 *       dan Simpan (memanggil {@link #onSave(Event)}, lalu refresh grid dan tutup window).</li>
	 *   <li>Jika mode non-persetujuan dan sudah disetujui ({@code setujui=true}):
	 *       menyembunyikan tombol Simpan dan mengubah label Batal menjadi "Tutup".</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Perubahan tata letak window (ukuran, judul, tombol toolbar)
	 * dilakukan di sini. Perubahan konten form (field input) dilakukan di {@link #form}.
	 * Jangan memanggil metode ini langsung dari luar kelas; gunakan {@link #onAdd(Event)}
	 * atau {@link #init(GeneralValueObject)} sebagai entry point.</p>
	 *
	 * @param pertangungjawabanKasBesar entitas yang akan ditampilkan di form; jika baru (id==null)
	 *                                   akan diisi pembuat dan tanggal otomatis
	 * @throws Exception jika terjadi error pada pembuatan komponen ZK atau form
	 */
	private void init(final PertangungjawabanKasBesar pertangungjawabanKasBesar) throws Exception {

		if (pertangungjawabanKasBesar.getDibuatOleh() == null) {
			pertangungjawabanKasBesar.setDibuatOleh(tbmuser);
			pertangungjawabanKasBesar.setTanggalPembuatan(new Date());
		}

		addWindow.setTitle((!persetujuan ? "Pengajuan" : "Persetujuan") + " Pertanggungjawaban");
		this.pertangungjawabanKasBesar = pertangungjawabanKasBesar;
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig(
				!persetujuan ? "Ajukan dan Cetak" : "Ubah Status Persetujuan dan Cetak", "/img/save.gif");

		disposisiSop=null;center.appendChild(form(pertangungjawabanKasBesar, disposisiSop, save, null));

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

		if (!persetujuan && setujui) {
			save.setVisible(false);
			cancel.setLabel("Tutup");
		}

	}

	/**
	 * Memvalidasi input, menyimpan entitas pertanggungjawaban, dan memicu cetak laporan otomatis.
	 *
	 * <p><b>Tujuan:</b> Event handler utama untuk tombol "Ajukan dan Cetak" / "Ubah Status
	 * Persetujuan dan Cetak". Melakukan serangkaian validasi, menyimpan/memperbarui entitas
	 * ke database, lalu memicu cetak laporan dan pembuatan DaftarPengajuanTransfer
	 * jika status DISETUJU.</p>
	 *
	 * <p><b>Cara kerja — validasi:</b>
	 * <ol>
	 *   <li>Memeriksa kas besar terpilih (wajib isi).</li>
	 *   <li>Memeriksa nama pengajuan tidak kosong (wajib isi).</li>
	 *   <li>Memeriksa tanggal stor diisi jika ada nilai yang harus dikembalikan
	 *       ({@code dikembalikan &gt; 0.1}).</li>
	 *   <li>Menghitung ulang total nilai dari JSONArray untuk validasi tidak melebihi
	 *       nilai kas besar asal.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Cara kerja — penyimpanan:</b>
	 * <ol>
	 *   <li>Me-load ulang entitas dari sesi Hibernate (jika sudah ada) untuk menghindari
	 *       stale data.</li>
	 *   <li>Menetapkan semua field dari komponen UI ke entitas.</li>
	 *   <li>Mengatur {@code disetujuiOleh} dan {@code tanggalPersetujuan} berdasarkan
	 *       status radio yang dipilih.</li>
	 *   <li>Menyimpan (save baru) atau memperbarui (update yang ada) via session Hibernate.</li>
	 *   <li>Memanggil {@code session.flush()} untuk memastikan perubahan tertulis ke DB.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Cara kerja — pasca simpan (via timer ZK):</b>
	 * <ol>
	 *   <li>Merefresh entitas {@link ais.database.model.akunting.KasBesar} untuk memperbarui
	 *       referensi ke pertanggungjawaban ini.</li>
	 *   <li>Memanggil {@link #cetak(PertangungjawabanKasBesar)} dengan delay 2500ms
	 *       untuk membuka laporan PDF.</li>
	 *   <li>Memanggil {@link ais.database.model.akunting.Pajak#buat} untuk setiap item biaya
	 *       (pencatatan pajak otomatis).</li>
	 *   <li>Jika status DISETUJU, memanggil
	 *       {@link ais.database.model.akunting.DaftarPengajuanTransfer#simpanPertangungjawabanKasBesar}
	 *       untuk membuat pengajuan transfer pengembalian sisa dana.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Rumus perhitungan total menggunakan konfigurasi
	 * {@code pph_mengurangi_lpj} yang dibaca ulang di setiap pemanggilan (bukan cache field)
	 * untuk memastikan menggunakan nilai konfigurasi terkini.</p>
	 *
	 * @param event event ZK dari klik tombol simpan; dapat diabaikan
	 * @return {@code true} jika penyimpanan berhasil dan window boleh ditutup;
	 *         {@code false} jika ada validasi yang gagal
	 * @throws Exception jika terjadi error pada Hibernate atau komponen ZK
	 */
	public boolean onSave(Event event) throws Exception {

		KasBesar work = (KasBesar) (kasBesar.getAttribute("kasBesar"));
		if (work == null) {
			MyMessageboxConfig.show("Mohon maaf, Kas Besar yang akan dipertanggungjawabkan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih data Kas Besar dari field pencarian yang tersedia; (2) Pastikan pengajuan kas besar sudah ada dan berstatus valid; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Judul Pengajuan belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Judul Pengajuan dengan deskripsi singkat pertanggungjawaban; (2) Pastikan judul tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (tanggalStor.getValue() == null && dikembalikan > 0.1) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Stor belum diisi. Langkah yang dapat dilakukan: (1) Isikan atau pilih Tanggal Stor menggunakan date picker; (2) Tanggal stor diperlukan karena terdapat nilai yang dikembalikan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");
		nilai = 0.0;
		for (int i = 0; i < array.length(); i++) {
			Double jumlah = 0.0;
			JSONObject jsonObject = array.getJSONObject(i);
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

			Double ppn = 0.0;
			if (!jsonObject.isNull("ppn")) {
				ppn = jsonObject.getDouble("ppn");
			}

			Double pajak = barang == null ? 0.0 : ((barang.getPersen() / 100.0) * jumlah);

			Double tot = (jumlah + ((ppn / 100.0) * jumlah)) - (pph_mengurangi_lpj ? pajak : 0.0);

			nilai += tot;
		}

		if (work.getNilai() < nilai) {
			MyMessageboxConfig.show("Mohon maaf, nilai yang dipertanggungjawabkan melebihi sisa nilai pengajuan kas besar. Langkah yang dapat dilakukan: (1) Kurangi total nilai pertanggungjawaban agar tidak melebihi sisa nilai pengajuan; (2) Periksa kembali rincian biaya yang dimasukkan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pertangungjawabanKasBesar.getId() != null) {
			pertangungjawabanKasBesar = (PertangungjawabanKasBesar) session.load(PertangungjawabanKasBesar.class,
					pertangungjawabanKasBesar.getId());
		}

		if (pertangungjawabanKasBesar.getDibuatOleh() == null) {
			pertangungjawabanKasBesar.setDibuatOleh(tbmuser);
			pertangungjawabanKasBesar.setTanggalPembuatan(new Date());
		}
		if (disposisiSop != null && disposisiSop.getId() != null) {
			pertangungjawabanKasBesar.setDisposisiSop(disposisiSop);
		}

		pertangungjawabanKasBesar.setKasBesar(work);
		pertangungjawabanKasBesar.setKode(kode.getValue());
		pertangungjawabanKasBesar.setNama(nama.getValue());
		pertangungjawabanKasBesar.setNilai(nilai);
		pertangungjawabanKasBesar.setDikembalikan(dikembalikan);
		pertangungjawabanKasBesar.setPajak(nilaipajak);
		pertangungjawabanKasBesar.setKeterangan(keterangan.getValue());

		pertangungjawabanKasBesar.setFormula(array.toString());

		pertangungjawabanKasBesar.setNamaSponsor(namaSponsor.getValue());
		pertangungjawabanKasBesar.setDariSponsor(dariSponsor.getValue());

		pertangungjawabanKasBesar.setTanggalStor(tanggalStor.getValue());

		String sts = (String) (status.getSelectedItem() == null ? null : status.getSelectedItem().getValue());
		if (sts != null && sts.equals(DanaTalangan.DISETUJU)) {
			pertangungjawabanKasBesar.setDisetujuiOleh(tbmuser);
			pertangungjawabanKasBesar.setTanggalPersetujuan(WaktuUtil.getDate());
		} else {
			pertangungjawabanKasBesar.setDisetujuiOleh(null);
			pertangungjawabanKasBesar.setTanggalPersetujuan(null);
		}

		pertangungjawabanKasBesar.setStatus(sts);

		if (pertangungjawabanKasBesar.getId() != null) {
			session.update(pertangungjawabanKasBesar);
		} else {
			pertangungjawabanKasBesar.setDibuatOleh(tbmuser);
			String noAgenda = generateCode(true, (SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
			kode.setValue(noAgenda);
			pertangungjawabanKasBesar.setKode(kode.getValue());
			session.save(pertangungjawabanKasBesar);
		}

		session.flush();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				KasBesar work = (KasBesar) (kasBesar.getAttribute("kasBesar"));
				if (work != null) {
					Session session = HibernateUtil.currentSession();
					session.refresh(work);
					work.setPertangungjawabanKasBesar(pertangungjawabanKasBesar);
					Common.refreshUpdate(session, work);
					session.flush();

				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						cetak(PertangungjawabanKasBesarAction.this.pertangungjawabanKasBesar);
					}
				}, "Proses cetak", false, 2500);

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						for (int i = 0; i < array.length(); i++) {
							JSONObject jsonObject = array.getJSONObject(i);
							try {
								Pajak.buat(null, pertangungjawabanKasBesar, jsonObject, null);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
						}

						if (pertangungjawabanKasBesar.getStatus().equals(PertangungjawabanKasBesar.DISETUJU)) {
							DaftarPengajuanTransfer.simpanPertangungjawabanKasBesar(pertangungjawabanKasBesar);
						}
					}
				});
			}
		});

		return true;
	}

	/**
	 * Membangun objek Criteria Hibernate untuk query daftar pertanggungjawaban kas besar.
	 *
	 * <p><b>Tujuan:</b> Implementasi antarmuka {@link DataCriteria} yang membangun query
	 * Hibernate berdasarkan nilai-nilai filter yang aktif di form pencarian. Dipakai oleh
	 * {@link #onSearchDefault(Event)} untuk pengisian grid dan oleh tombol ekspor data
	 * ({@code Common.cetakData}).</p>
	 *
	 * <p><b>Cara kerja — filter yang diterapkan:</b>
	 * <ol>
	 *   <li><b>Satuan Kerja:</b> Jika {@code searchparent} memiliki satuan kerja terpilih,
	 *       mengambil semua satuan kerja anak via {@code SatuanKerjaTreeModel.getChildsSet}
	 *       dan menfilter dengan {@code IN}. Jika tidak ada, tampilkan semua.</li>
	 *   <li><b>Tanggal:</b> Memfilter berdasarkan {@code date(tanggal_pembuatan)} antara
	 *       {@code start} dan {@code end} (inklusif, menggunakan SQL date()). Jika komponen
	 *       tanggal null, filter dinonaktifkan.</li>
	 *   <li><b>Status:</b> Memfilter berdasarkan status yang dipilih di combobox
	 *       (PENGAJUAN/DISETUJU/DITOLAK). Jika "Semua" dipilih, tidak ada filter.</li>
	 *   <li><b>Aktif:</b> Jika checkbox aktif dicentang, hanya tampilkan yang aktif;
	 *       jika tidak dicentang, tampilkan semua.</li>
	 *   <li><b>Kode:</b> Filter ILIKE anywhere pada field kode.</li>
	 *   <li><b>Nama:</b> Filter ILIKE anywhere pada field nama.</li>
	 *   <li><b>Urutan:</b> Jika {@code order=true}, menambahkan ORDER BY id DESC.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika {@code searchparent} adalah null (misalnya halaman
	 * dipanggil tanpa komponen filter satuan kerja), metode langsung mengembalikan null.
	 * Pemanggil harus memeriksa null sebelum menggunakan hasil Criteria ini.</p>
	 *
	 * @param order {@code true} untuk menambahkan ORDER BY id DESC pada query;
	 *              {@code false} untuk query tanpa urutan (dipakai saat hitung paging)
	 * @return {@link Criteria} Hibernate yang siap di-execute, atau {@code null} jika
	 *         {@code searchparent} belum tersedia
	 */
	public Criteria initCriteria(boolean order) {
		if (searchparent == null) return null;

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PertangungjawabanKasBesar.class)

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

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
	 * Menyegarkan grid daftar pertanggungjawaban kas besar dengan data terkini dari database.
	 *
	 * <p><b>Tujuan:</b> Implementasi {@link DataSearchDefault} yang dipanggil saat pengguna
	 * mengklik tombol cari, mengubah filter, atau setelah operasi simpan/hapus berhasil.
	 * Memperbarui paging dan mengisi ulang grid dengan data yang sesuai filter aktif.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@link Common#initPaging(Criteria, Paging)} dengan {@code initCriteria(false)}
	 *       (tanpa order) untuk menghitung total baris dan memperbarui komponen paging.</li>
	 *   <li>Mengeksekusi query dengan {@code initCriteria(true)} (dengan ORDER BY id DESC),
	 *       dibatasi {@link Common#ROWS_COUNT_ON_PAGE} baris dan dioffset sesuai halaman aktif.</li>
	 *   <li>Membuat {@link SimpleListModel} dari hasil query dan menetapkannya ke grid
	 *       bersama renderer {@link PertangungjawabanKasBesarRenderer}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini dipanggil dari banyak tempat: timer inisialisasi,
	 * listener searchparent, listener paging, dan setelah simpan/hapus. Pastikan tidak ada
	 * operasi berat di sini karena berjalan di UI thread. Jika perlu operasi berat, gunakan
	 * {@link Common#createDefaultTimer} dengan delay.</p>
	 *
	 * @param event event ZK pemicu (bisa null untuk refresh programatik);
	 *              nilai event tidak digunakan dalam implementasi ini
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PertangungjawabanKasBesar> pertangungjawabanKasBesar = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pertangungjawabanKasBesar);
		grid.setRowRenderer(new PertangungjawabanKasBesarRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Mengembalikan istilah/nama modul ini untuk keperluan alur SOP.
	 *
	 * <p><b>Tujuan:</b> Implementasi antarmuka {@link FormSop} yang menyediakan nama
	 * human-readable dari modul ini. Digunakan oleh sistem SOP untuk menampilkan nama
	 * modul di alur disposisi, notifikasi, dan log SOP.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika nama modul berubah (misalnya karena rebranding),
	 * ubah string kembalian di sini. Pastikan tidak ada logika bisnis yang bergantung
	 * pada nilai string ini (gunakan konstanta kelas jika diperlukan).</p>
	 *
	 * @return string "Pertanggungjawaban Kas Besar" sebagai nama istilah modul
	 * @throws Exception tidak akan dilempar; deklarasi ada karena kontrak antarmuka
	 */
	@Override
	public String istilah() throws Exception {
		return "Pertanggungjawaban Kas Besar";
	}

	/**
	 * Mengembalikan entitas pertanggungjawaban kas besar yang sedang aktif di form.
	 *
	 * <p><b>Tujuan:</b> Implementasi antarmuka {@link FormSop} yang memberikan akses ke entitas
	 * yang sedang diedit kepada sistem alur SOP. Digunakan untuk menyimpan referensi entitas
	 * ke disposisi SOP saat pengajuan diproses melalui alur SOP.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Field {@code pertangungjawabanKasBesar} diperbarui di
	 * {@link #form} dan {@link #init(PertangungjawabanKasBesar)}. Pastikan selalu terkini
	 * sebelum sistem SOP memanggil metode ini.</p>
	 *
	 * @return entitas {@link ais.database.model.akunting.PertangungjawabanKasBesar} yang
	 *         sedang aktif di form; bisa null jika belum diinisialisasi
	 * @throws Exception tidak akan dilempar; deklarasi ada karena kontrak antarmuka
	 */
	@Override
	public DataSop ambil() throws Exception {
		return pertangungjawabanKasBesar;
	}

	/**
	 * Mengembalikan kelas entitas yang dikelola oleh controller ini.
	 *
	 * <p><b>Tujuan:</b> Implementasi antarmuka {@link FormSop} yang memberikan informasi
	 * tipe kelas kepada framework SOP. Digunakan untuk operasi refleksi dan query generik
	 * di modul SOP dan ekspor data.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Nilai kembalian tidak boleh diubah. Jika entitas diganti
	 * dengan entitas berbeda, seluruh kelas ini perlu direview.</p>
	 *
	 * @return {@code PertangungjawabanKasBesar.class} sebagai tipe entitas yang dikelola
	 * @throws Exception tidak akan dilempar; deklarasi ada karena kontrak antarmuka
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PertangungjawabanKasBesar.class;
	}

	/**
	 * Menghasilkan kode unik untuk pertanggungjawaban kas besar baru berdasarkan nomor surat.
	 *
	 * <p><b>Tujuan:</b> Membuat kode dokumen yang terformat sesuai konfigurasi nomor surat
	 * {@link NomorSuratAlurKeuangan#PERTANGGUNGJAWABAN_KAS_BESAR_DATA}. Kode ini menggabungkan
	 * prefix, satuan kerja, tahun/bulan, dan nomor urut menjadi string seperti
	 * "LPJ-KB/SATKER/VI/2026/001".</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika konfigurasi nomor surat tidak tersedia (null), mengembalikan barcode acak
	 *       via {@link Common#getGeneratedBarCode()}.</li>
	 *   <li>Menentukan index (nomor urut) berdasarkan konfigurasi:
	 *       jika {@code gunakanIndexUrut=true}, memakai {@code nomorIndex} yang disimpan
	 *       di konfigurasi; jika tidak, memanggil {@link #getindex(NomorSurat)} yang
	 *       menghitung dari jumlah data yang ada di database.</li>
	 *   <li>Jika {@code tambah=true}, memanggil {@code NomorSurat.tambahIndexNomorSurat}
	 *       untuk menaikkan counter sequence.</li>
	 *   <li>Memformat kode via {@code NomorSurat.format(index, tanggal, satuanKerja)}.</li>
	 *   <li>Memastikan keunikan kode dengan {@link ais.action.master.KodeUnikUtil#pastikanUnik}
	 *       (menambahkan sufiks -2/-3 jika sudah dipakai).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Konfigurasi nomor surat dikelola di modul Nomor Surat Alur
	 * Keuangan. Jika pola kode perlu diubah, ubah di sana. Parameter {@code tambah=false}
	 * dipakai untuk preview kode di form (sebelum simpan), {@code tambah=true} hanya saat
	 * benar-benar menyimpan data baru.</p>
	 *
	 * @param tambah     {@code true} untuk menaikkan counter sequence (saat simpan definitif);
	 *                   {@code false} untuk preview saja tanpa mengubah counter
	 * @param satuanKerja satuan kerja yang digunakan sebagai variabel substitusi dalam format
	 *                    kode; bisa {@code null} jika tidak ada variabel satuan kerja
	 * @return string kode unik yang terformat dan dipastikan belum digunakan di database
	 */
	private String generateCode(boolean tambah, SatuanKerja satuanKerja) {
		if (NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_KAS_BESAR_DATA == null
				|| NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_KAS_BESAR_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_KAS_BESAR_DATA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_KAS_BESAR_DATA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_KAS_BESAR_DATA.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_KAS_BESAR_DATA.getNomorSurat());
		}
		String noAgenda = NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_KAS_BESAR_DATA.getNomorSurat().format(index,
				WaktuUtil.getDate(), satuanKerja);
		return ais.action.master.KodeUnikUtil.pastikanUnik(PertangungjawabanKasBesar.class, noAgenda);
	}

	/**
	 * Menghitung nomor urut (index) berikutnya dari jumlah data di database untuk kode nomor surat.
	 *
	 * <p><b>Tujuan:</b> Menghitung index numerik yang akan digunakan sebagai bagian nomor urut
	 * dalam format kode pertanggungjawaban. Berbeda dengan increment sequence murni, metode ini
	 * menghitung berdasarkan {@code rowCount} data yang ada, dengan berbagai filter berdasarkan
	 * konfigurasi reset urutan ({@code resetUrutanTiapTahun}, {@code resetUrutanTiapBulan},
	 * {@code resetTiap}).</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code nomorSurat} null, mengembalikan 0 (fallback aman).</li>
	 *   <li>Mengambil tahun dan bulan saat ini untuk filter reset periodik.</li>
	 *   <li>Membangun Criteria Hibernate pada tabel {@code PertangungjawabanKasBesar}
	 *       dengan join ke {@code nomorSuratAlurKeuangan} dan {@code nomorSurat}.</li>
	 *   <li>Filter berdasarkan konfigurasi:
	 *       <ul>
	 *         <li>{@code urutBerdasarkanNomor}: filter by nomorSurat spesifik.</li>
	 *         <li>{@code urutBerdasarkanKelompok}: filter by kelompokNomorSurat.</li>
	 *         <li>{@code resetUrutanTiapTahun}: tambahkan filter tahun = tahun sekarang.</li>
	 *         <li>{@code resetUrutanTiapBulan}: tambahkan filter tahun+bulan = sekarang.</li>
	 *         <li>{@code resetTiap}: tambahkan filter tanggalPembuatan >= resetTiap jika
	 *             sudah melewati tanggal reset.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Mengeksekusi {@code Projections.rowCount()} untuk mendapatkan jumlah data.</li>
	 *   <li>Mengembalikan {@code count + 1} sebagai index berikutnya.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini rentan terhadap race condition (dua pengguna simpan
	 * bersamaan mendapat index yang sama). Mitigasinya ada di {@link ais.action.master.KodeUnikUtil#pastikanUnik}
	 * yang dipanggil setelah generate kode. Untuk implementasi yang lebih aman, pertimbangkan
	 * menggunakan database sequence.</p>
	 *
	 * @param nomorSurat konfigurasi nomor surat yang menentukan cara penghitungan index;
	 *                   jika null, mengembalikan 0
	 * @return nomor urut berikutnya (rowCount + 1) berdasarkan data yang ada di database
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PertangungjawabanKasBesar.class)
				.createAlias("nomorSuratAlurKeuangan", "nomorSuratAlurKeuangan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurKeuangan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(nomorSurat.getUrutBerdasarkanNomor()
						? Restrictions.eq("nomorSuratAlurKeuangan.nomorSurat", nomorSurat)

						: (nomorSurat.getUrutBerdasarkanKelompok() && nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat", nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")))

				.add(nomorSurat.getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetTiap() != null && (Common.dateFormat8.get().format(nomorSurat.getResetTiap())
						.equals(Common.dateFormat8.get().format(sekarang)) || nomorSurat.getResetTiap().before(sekarang))
								? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	/**
	 * Menetapkan mode persetujuan secara programatik setelah konstruksi objek.
	 *
	 * <p><b>Tujuan:</b> Implementasi antarmuka {@link FormSop} yang memungkinkan sistem
	 * SOP mengubah mode controller dari pengajuan ke persetujuan (atau sebaliknya)
	 * setelah objek dibuat. Dipakai ketika controller diinstansiasi secara generik
	 * oleh framework SOP dan mode persetujuan baru diketahui setelah instansiasi.</p>
	 *
	 * <p><b>Cara kerja:</b> Menetapkan field {@code this.persetujuan} ke nilai yang
	 * diberikan. Perubahan ini akan memengaruhi perilaku {@link #form},
	 * {@link #onSave(Event)}, dan tampilan tombol pada pemanggilan berikutnya.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini harus dipanggil sebelum {@link #form} atau
	 * {@link #init(PertangungjawabanKasBesar)} untuk memastikan mode yang benar
	 * diterapkan saat membangun UI.</p>
	 *
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan;
	 *                    {@code false} untuk mode pengajuan biasa
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

}
