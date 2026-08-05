package ais.action.master.asset;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Group;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.West;

import ais.action.master.TampilanPengumumanAkademisAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KategoriPengumuman;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>TampilanPengumumanVendorAction — Kontroler Tampilan Pengumuman untuk Vendor</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini berfungsi sebagai kontroler ZKoss (GenericAutowireComposer) untuk halaman
 * tampilan pengumuman yang diperuntukkan khusus bagi pihak vendor. Pengumuman yang
 * ditampilkan di sini hanya yang memiliki atribut {@code diperuntukkan} bernilai
 * {@code PengumumanAkademis.UNTUK_VENDOR}. Kontroler ini mendukung navigasi berbasis menu
 * di sisi kiri (West) untuk tampilan desktop, atau menu North lipatan untuk tampilan mobile,
 * dengan panel konten di tengah yang memuat isi pengumuman masing-masing secara dinamis
 * menggunakan tab ZKoss. Selain itu, tersedia filter tanggal rentang mulai dan sampai,
 * serta kotak pencarian judul pengumuman secara real-time.
 *
 * <b>Cara kerja:</b><br>
 * Saat halaman selesai dikomposis ({@code doAfterCompose}), kontroler melakukan pemeriksaan
 * keamanan terlebih dahulu melalui {@code Common.doCheckSecurity()} pada fase
 * {@code doBeforeCompose}. Kemudian mengambil konteks sekolah dan yayasan dari sesi aktif
 * menggunakan {@code SekolahUtil}, menginisialisasi rentang tanggal default (12 bulan ke
 * belakang dan 6 bulan ke depan dari tanggal sekarang), menentukan mode tampilan
 * (mobile atau desktop berdasarkan parameter {@code desktopWidth}), dan membangun menu
 * navigasi lateral beserta daftar pengumuman. Jika pengumuman sedikit (≤5) atau tampilan
 * bukan mobile, pengumuman pertama langsung dibuka di panel konten. Metode {@code loadData}
 * memuat ulang daftar pengumuman berdasarkan kata kunci pencarian, mengelompokkannya per
 * {@code KategoriPengumuman} menggunakan komponen {@code Group} ZKoss, dan melewatkan
 * pengumuman berkategori {@code PENGUMUMAN_UTAMA} (ditampilkan di tempat lain). Setiap
 * baris daftar menjadi tombol yang saat diklik memanggil
 * {@code TampilanPengumumanAkademisAction.prosess} untuk memuat isi ke panel tab.
 * Metode statis {@code initCriteriaStatic} membangun kriteria Hibernate dengan filter
 * multi-kondisi: sekolah, yayasan, tanggal aktif (dengan opsi "tetap tampilkan meski
 * kelewat"), status aktif, dan jenis peruntukan vendor.
 *
 * <b>Threading:</b><br>
 * Kelas ini mengikuti model threading ZKoss di mana setiap permintaan UI diproses dalam
 * thread ZK Desktop tunggal. Tidak ada akses multithreaded langsung pada field instans.
 * Akses sesi Hibernate dilakukan melalui {@code HibernateUtil.currentSession()} yang
 * memanfaatkan sesi terkelola per request. Listener timer atau latar belakang tidak
 * digunakan dalam kelas ini.
 *
 * <b>Pemeliharaan:</b><br>
 * Jika perlu menambah filter baru (misalnya filter berdasarkan satuan kerja atau jenis
 * vendor tertentu), tambahkan kondisi pada {@code initCriteria} dan perbarui
 * {@code initCriteriaStatic}. Perhatikan bahwa {@code initCriteriaStatic} digunakan pula
 * oleh kode luar sebagai helper statis, sehingga perubahan tanda tangannya dapat berdampak
 * luas. Rentang tanggal default dapat disesuaikan di {@code doAfterCompose}. Batas
 * {@code setMaxResults(500)} pada {@code loadData} harus dievaluasi jika volume pengumuman
 * vendor sangat besar.
 *
 * @author AIS Development Team
 * @version 1.0
 * @since ZKoss 5.5, Java 1.7
 */
public class TampilanPengumumanVendorAction extends GenericAutowireComposer {

	/**
	 * Versi serial untuk serialisasi objek kelas ini.
	 * Nilai ini tetap dan tidak boleh diubah sembarangan agar kompatibilitas
	 * deserialisasi terjaga antara versi deploy yang berbeda.
	 */
	private static final long serialVersionUID = -2301873239699174688L;

	/** Komponen datebox untuk tanggal mulai filter rentang pengumuman. */
	private MyDatebox searchmulai;

	/** Komponen datebox untuk tanggal akhir filter rentang pengumuman. */
	private MyDatebox searchsampai;

	/**
	 * Flag mode read-only. Jika {@code true}, operasi tambah/ubah/hapus
	 * tidak diizinkan. Nilai default {@code false}.
	 */
	private Boolean readonly = false;

	/** Komponen Rows ZKoss sebagai wadah baris daftar pengumuman di panel menu. */
	private Rows rows;

	/** Textbox pencarian judul pengumuman pada panel menu lateral. */
	private Textbox cari;

	/** Wadah panel konten tab untuk menampilkan isi pengumuman yang dipilih. */
	private Tabpanels tabpanels;

	/**
	 * Komponen menu navigasi lateral; bertipe {@code West} pada desktop
	 * atau {@code North} pada tampilan mobile.
	 */
	private Component menu;

	/** Layout utama halaman menggunakan Borderlayout ZKoss. */
	private Borderlayout layoutUtama;

	/** Komponen Tabs ZKoss yang berpasangan dengan {@code tabpanels}. */
	private Tabs tabs;

	/** Window utama halaman ini. */
	private MyWindow window;

	/**
	 * Sekolah yang aktif dalam konteks sesi pengguna saat ini.
	 * Digunakan sebagai filter pada query pengumuman.
	 */
	private Sekolah selectedSekolah;

	/**
	 * Yayasan yang aktif dalam konteks sesi pengguna saat ini.
	 * Digunakan sebagai filter pada query pengumuman.
	 */
	private Yayasan selectedYayasan;

	/**
	 * Jumlah total pengumuman vendor yang tersedia, digunakan untuk menentukan
	 * apakah menu navigasi perlu ditampilkan dan apakah pengumuman pertama
	 * langsung dimuat.
	 */
	private int size;

	/**
	 * <b>Tujuan:</b> Fase pra-komposisi ZKoss — memeriksa keamanan akses halaman
	 * sebelum komponen UI dikomposis.<br>
	 * <br>
	 * <b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} yang akan
	 * melempar pengecualian atau mengarahkan ulang pengguna yang tidak berwenang,
	 * lalu meneruskan ke implementasi induk {@code super.doBeforeCompose}.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code page} — halaman ZK tempat komponen dikomposis</li>
	 *   <li>{@code parent} — komponen induk dalam hierarki ZK</li>
	 *   <li>{@code compInfo} — informasi metadata komponen dari ZUL</li>
	 * </ul>
	 * <b>Return:</b> Objek {@code ComponentInfo} dari induk, digunakan ZK untuk
	 * melanjutkan proses komposisi.<br>
	 * <br>
	 * <b>Penanganan error:</b> Jika pengguna tidak terautentikasi atau tidak
	 * memiliki hak akses, {@code Common.doCheckSecurity()} akan menghentikan
	 * eksekusi dan mengarahkan ke halaman login.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Jangan hapus pemanggilan {@code Common.doCheckSecurity()}.
	 * Ini adalah garis pertahanan pertama terhadap akses tidak sah.
	 *
	 * @param page     halaman ZK aktif
	 * @param parent   komponen induk dalam pohon komponen ZK
	 * @param compInfo metadata komponen dari berkas ZUL
	 * @return {@code ComponentInfo} dari implementasi induk
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Fase pasca-komposisi ZKoss — menginisialisasi seluruh komponen UI
	 * dan memuat data pengumuman vendor untuk pertama kalinya.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk memastikan autowiring
	 *       komponen ZKoss berjalan.</li>
	 *   <li>Mengambil konteks sekolah dan yayasan dari sesi via {@code SekolahUtil}.</li>
	 *   <li>Menginisialisasi {@code searchmulai} ke 12 bulan yang lalu dari tanggal
	 *       sekarang (jika komponen tersedia di ZUL).</li>
	 *   <li>Menginisialisasi {@code searchsampai} ke 6 bulan ke depan dari tanggal
	 *       sekarang (jika komponen tersedia di ZUL).</li>
	 *   <li>Menentukan mode tampilan berdasarkan parameter {@code desktopWidth} atau
	 *       flag {@code Common.isMobile()}; pada mobile menggunakan komponen {@code North},
	 *       pada desktop menggunakan {@code West}.</li>
	 *   <li>Menghitung jumlah total pengumuman vendor melalui query proyeksi rowCount.</li>
	 *   <li>Menyembunyikan menu jika pengumuman hanya satu atau tidak ada.</li>
	 *   <li>Membangun panel menu lateral via {@code loadMenu()}.</li>
	 *   <li>Jika pengumuman sedikit (≤5) atau bukan mode mobile, langsung memuat
	 *       pengumuman pertama ke panel konten tab.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code comp} — komponen root yang dikomposis ZKoss (biasanya {@code MyWindow})</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Exception dilempar jika terjadi masalah inisialisasi
	 * komponen atau akses database Hibernate gagal.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Penyesuaian rentang tanggal default dilakukan di sini.
	 * Jika ZUL mengubah nama komponen ({@code searchmulai}, {@code searchsampai}),
	 * nama field di kelas ini harus disesuaikan pula.
	 *
	 * @param comp komponen root hasil komposisi ZKoss
	 * @throws Exception jika terjadi kesalahan inisialisasi komponen atau database
	 */
	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		selectedSekolah = SekolahUtil.getSekolah();
		selectedYayasan = SekolahUtil.getYayasan();
		if (searchmulai != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 12);
			searchmulai.setValue(calendar.getTime());
		}

		if (searchsampai != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 6);
			searchsampai.setValue(calendar.getTime());
		}

		String desktopWidth = execution.getParameter("desktopWidth");
		boolean mobile = Common.isMobile() || (desktopWidth != null
				&& Integer.parseInt(desktopWidth.replaceAll("px", "")) < ConstantValues.UKURAN_BATAS_MOBILE);
		if (mobile) {
			menu = new North();
			((North) menu).setHeight("100%");
			layoutUtama.appendChild(menu);
			if (desktopWidth != null) {
				window.setWidth(desktopWidth);
			}
		} else {
			menu = new West();
			((West) menu).setWidth("200px");
			layoutUtama.appendChild(menu);
		}

		size = ((Number) initCriteria(false).setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (menu != null) { menu.setVisible(size > 1); }
		loadMenu();

		if (size <= 5 || !mobile) {
			List<PengumumanAkademis> listPengumumanAkademis = ConstantValues.simpleList(
					initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_1), PengumumanAkademis.class);

			if (!listPengumumanAkademis.isEmpty()) {
				TampilanPengumumanAkademisAction.prosess(listPengumumanAkademis.get(0).getId(), tabs, tabpanels, false,
						size <= 5, cari, false);
			}
		}

	}

	/**
	 * <b>Tujuan:</b> Membangun panel menu navigasi lateral yang berisi kotak pencarian
	 * dan daftar judul pengumuman vendor yang dapat diklik.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mengembalikan lebih awal jika {@code menu}, {@code tabpanels}, atau
	 *       {@code tabs} bernilai null (ZUL tidak menyediakan komponen yang diperlukan).</li>
	 *   <li>Membangun {@code Borderlayout} bertingkat di dalam komponen {@code menu},
	 *       dengan area North untuk kotak pencarian dan tombol cari, serta area Center
	 *       untuk grid daftar pengumuman.</li>
	 *   <li>Menambahkan {@code Textbox} pencarian dengan listener {@code onOK} (tekan Enter)
	 *       dan tombol ikon search yang keduanya memanggil {@code loadData}.</li>
	 *   <li>Menginisialisasi {@code MyGrid} dengan {@code Rows} sebagai wadah daftar
	 *       dan langsung memanggil {@code loadData} untuk pengisian awal.</li>
	 * </ol>
	 * <b>Parameter:</b> tidak ada.<br>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Tidak ada exception eksplisit; jika komponen null,
	 * metode langsung kembali tanpa aksi.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Tinggi subsubNorth (28px) dan lebar West (80%) bersifat
	 * hardcoded; sesuaikan jika desain UI berubah. Batas visibilitas menu (size > 1)
	 * berarti jika hanya ada satu pengumuman, kotak pencarian ikut disembunyikan
	 * bersama border layout.
	 */
	private void loadMenu() {

		if (menu == null || tabpanels == null || tabs == null) {
			return;
		}

		Borderlayout subBorderlayout = new Borderlayout();
		subBorderlayout.setVisible(size > 1);
		subBorderlayout.setParent(menu);

		North subsubNorth = new North();
		subsubNorth.setParent(subBorderlayout);
		subsubNorth.setHeight(size > 1 ? "28px" : "0px");
		subsubNorth.setBorder("none");

		Borderlayout subSubBorderlayout = new Borderlayout();
		subSubBorderlayout.setParent(subsubNorth);

		West subSubwest = new West();
		subSubwest.setParent(subSubBorderlayout);
		subSubwest.setWidth("80%");
		subSubwest.setBorder("none");

		cari = new Textbox();
		cari.setWidth("90%");
		cari.setParent(subSubwest);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		button.setWidth("90%");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(cari.getValue().trim());
			}
		});

		Center subsubcenter = new Center();
		subsubcenter.setParent(subSubBorderlayout);
		ais.ui.util.ZkCompat.setFlex(subsubcenter, true);
		button.setParent(subsubcenter);
		subsubcenter.setBorder("none");

		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(cari.getValue().trim());
			}
		});

		Center subcenter = new Center();
		subcenter.setParent(subBorderlayout);
		ais.ui.util.ZkCompat.setFlex(subcenter, true);
		subcenter.setBorder("none");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(subcenter);
		grid.setWidth("100%");
		grid.setHeight("100%");

		rows = new Rows();
		rows.setParent(grid);
		loadData(cari.getValue());

	}

	/**
	 * <b>Tujuan:</b> Memuat ulang daftar pengumuman vendor ke panel menu lateral
	 * berdasarkan kata kunci pencarian yang diberikan.<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membersihkan konten {@code rows} sebelumnya menggunakan {@code Common.clear}.</li>
	 *   <li>Mengambil preferensi bahasa pengguna dari atribut sesi ZKoss
	 *       ({@code current_lang}); jika tidak ada atau gagal, menggunakan
	 *       {@code Tbmuser.INDONESIA} sebagai default.</li>
	 *   <li>Menjalankan query Hibernate melalui {@code initCriteria(true)} dengan filter
	 *       {@code ilike} pada kolom {@code judul} jika kata kunci tidak kosong, dibatasi
	 *       maksimal 500 hasil.</li>
	 *   <li>Mengelompokkan pengumuman berdasarkan {@code KategoriPengumuman}; setiap ganti
	 *       kategori menambahkan komponen {@code Group} sebagai pemisah visual di grid.</li>
	 *   <li>Melewati pengumuman yang masuk kategori {@code PENGUMUMAN_UTAMA}.</li>
	 *   <li>Untuk pengumuman tanpa kategori, menambahkan group "Pengumuman dan Informasi".</li>
	 *   <li>Menampilkan judul pengumuman (dipotong pada 255 karakter) sebagai
	 *       {@code Toolbarbutton}; saat diklik, memanggil
	 *       {@code TampilanPengumumanAkademisAction.prosess} untuk memuat konten ke panel
	 *       tab dan menggulir tampilan ke baris yang dipilih.</li>
	 *   <li>Pada tampilan mobile (menu bertipe North), menyesuaikan tinggi panel
	 *       berdasarkan jumlah baris + jumlah kategori × 30px.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code keyword} — kata kunci pencarian judul pengumuman; boleh null atau kosong
	 *       untuk menampilkan semua pengumuman</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Kesalahan saat mengambil {@code current_lang} dari sesi
	 * ditangkap dan ditampilkan via {@code Common.tampilErrorJikaAdmin}; eksekusi
	 * dilanjutkan dengan bahasa default Indonesia.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Batas 500 hasil hardcoded; pertimbangkan menggunakan konstanta
	 * atau konfigurasi jika volume pengumuman bertambah. Pemotong judul pada 255 karakter
	 * dapat disesuaikan. Jika menambah bahasa baru selain Indonesia dan Inggris, tambahkan
	 * cabang {@code else if} pada blok deteksi bahasa.
	 *
	 * @param keyword kata kunci pencarian judul pengumuman; null atau string kosong
	 *                berarti tampilkan semua pengumuman
	 */
	@SuppressWarnings("unchecked")
	public void loadData(String keyword) {

		Common.clear(rows);

		String currentLang = null;
		try {
			currentLang = (String) Sessions.getCurrent(true).getAttribute("current_lang");
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (currentLang == null) {
			currentLang = Tbmuser.INDONESIA;
		}

		List<PengumumanAkademis> pengumumanAkademises = ConstantValues.simpleList(
				initCriteria(true).add(keyword == null || keyword.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("judul", keyword, MatchMode.ANYWHERE)).setMaxResults(500),
				PengumumanAkademis.class);

		int jumlahkategori = 1;
		KategoriPengumuman kategoriPengumuman = new KategoriPengumuman();
		kategoriPengumuman.setId(-1L);

		for (final PengumumanAkademis pengumumanAkademis : pengumumanAkademises) {

			if (KategoriPengumuman.PENGUMUMAN_UTAMA != null && pengumumanAkademis.getKategoriPengumuman() != null
					&& KategoriPengumuman.PENGUMUMAN_UTAMA.getId()
							.equals(pengumumanAkademis.getKategoriPengumuman().getId())) {
				continue;
			}

			if (pengumumanAkademis.getKategoriPengumuman() != null && (kategoriPengumuman == null
					|| !kategoriPengumuman.getId().equals(pengumumanAkademis.getKategoriPengumuman().getId()))) {
				kategoriPengumuman = pengumumanAkademis.getKategoriPengumuman();
				if (currentLang.equals(Tbmuser.INDONESIA)) {
					Group group = new ais.ui.util.MyGroupConfig(kategoriPengumuman.getNama());
					group.setParent(rows);
				} else if (currentLang.equals(Tbmuser.ENGLISH)) {
					Group group = new ais.ui.util.MyGroupConfig(kategoriPengumuman.getNamaEn());
					group.setParent(rows);
				}
				jumlahkategori++;
			} else if (pengumumanAkademis.getKategoriPengumuman() == null && kategoriPengumuman != null) {
				kategoriPengumuman = null;
				Group group = new ais.ui.util.MyGroupConfig("Pengumuman dan Informasi");
				group.setParent(rows);
				jumlahkategori++;
			}

			final Row row = new Row();row.setValign("top");
			row.setStyle("border:0px;background: transparent;font-size: x-small;");

			row.setParent(rows);

			String text = "";
			if (currentLang.equals(Tbmuser.INDONESIA)) {
				text = pengumumanAkademis.getJudul();
			} else if (currentLang.equals(Tbmuser.ENGLISH)) {
				text = pengumumanAkademis.getJudulEn();
			}

			text = text.length() > 255 ? text.substring(0, 254) + ".." : text;

			final Toolbarbutton toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig(text);
			toolbarbutton.setStyle("font-size: x-small;");

			row.appendChild(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Clients.scrollIntoView(row);
					TampilanPengumumanAkademisAction.prosess(pengumumanAkademis.getId(), tabs, tabpanels, false,
							size <= 5, cari, false);
				}
			});

		}
		if (menu instanceof North) {
			((North) menu).setHeight(((pengumumanAkademises.size() + jumlahkategori) * 30) + "px");
		}
		pengumumanAkademises = null;
	}

	/**
	 * <b>Tujuan:</b> Membangun kriteria query Hibernate untuk mengambil daftar pengumuman
	 * vendor secara statis (dapat dipanggil tanpa instans kontroler).<br>
	 * <br>
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mengambil sesi Hibernate saat ini via {@code HibernateUtil.currentSession()}.</li>
	 *   <li>Menyusun criteria pada entitas {@code PengumumanAkademis} dengan filter:
	 *     <ul>
	 *       <li>Filter sekolah: jika sekolah atau ID-nya null, tidak memfilter; jika ada,
	 *           menggunakan {@code eq("sekolah", sekolah)}.</li>
	 *       <li>Filter yayasan: sama dengan logika filter sekolah.</li>
	 *       <li>Filter tanggal aktif: kombinasi OR antara flag
	 *           {@code tetapTampilkanPengumumanMeskipunSudahKelewat} bernilai true atau null,
	 *           dan kondisi tanggal mulai ≤ sekarang ATAU tanggal sampai ≥ sekarang.</li>
	 *       <li>Filter aktif: hanya pengumuman yang aktif (true atau null).</li>
	 *       <li>Filter peruntukan: hanya {@code PengumumanAkademis.UNTUK_VENDOR}.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Jika parameter {@code order} true, menambahkan join kiri ke
	 *       {@code KategoriPengumuman} dan mengurutkan berdasarkan
	 *       {@code nomorUrut} kategori, tanggal pengumuman descending, dan ID descending.</li>
	 * </ol>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code order} — jika {@code true}, menambahkan klausa ORDER BY untuk
	 *       penyortiran hasil; jika {@code false}, cocok untuk query COUNT</li>
	 *   <li>{@code sekolah} — entitas Sekolah sebagai filter; null berarti semua sekolah</li>
	 *   <li>{@code yayasan} — entitas Yayasan sebagai filter; null berarti semua yayasan</li>
	 * </ul>
	 * <b>Return:</b> Objek {@code Criteria} Hibernate yang siap dieksekusi dengan
	 * {@code .list()} atau {@code .uniqueResult()}.<br>
	 * <br>
	 * <b>Penanganan error:</b> Tidak ada penanganan khusus; exception Hibernate
	 * akan dipropagasi ke pemanggil.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Metode ini bersifat statis sehingga dapat digunakan oleh
	 * kelas lain sebagai utilitas filter pengumuman vendor. Perubahan logika filter
	 * di sini berdampak pada seluruh kode yang memanggil metode ini secara statis.
	 *
	 * @param order   {@code true} untuk menambahkan pengurutan hasil; {@code false}
	 *                untuk query tanpa urutan (misalnya untuk COUNT)
	 * @param sekolah entitas Sekolah sebagai filter; boleh null
	 * @param yayasan entitas Yayasan sebagai filter; boleh null
	 * @return kriteria Hibernate yang sudah dikonfigurasi untuk pengumuman vendor
	 */
	public static Criteria initCriteriaStatic(boolean order, Sekolah sekolah, Yayasan yayasan) {
		Session session = HibernateUtil.currentSession();

		Criterion r = Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_VENDOR);

		Criteria criteria = session.createCriteria(PengumumanAkademis.class)

				.add(sekolah == null || sekolah.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("sekolah", sekolah))

				.add(yayasan == null || yayasan.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("yayasan", yayasan))

				.add(Restrictions.or(
						Restrictions.or(Restrictions.eq("tetapTampilkanPengumumanMeskipunSudahKelewat", true),
								Restrictions.isNull("tetapTampilkanPengumumanMeskipunSudahKelewat")),
						Restrictions.or(Restrictions.le("tanggal", ais.ui.util.WaktuUtil.getDate()),
								Restrictions.ge("sampai", ais.ui.util.WaktuUtil.getDate()))))

				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))).add(r);
		if (order)
			criteria.createAlias("kategoriPengumuman", "kategoriPengumuman", Criteria.LEFT_JOIN)
					.addOrder(Order.asc("kategoriPengumuman.nomorUrut")).addOrder(Order.desc("tanggal"))
					.addOrder(Order.desc("id"));
		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Pembungkus instans dari metode statis {@code initCriteriaStatic},
	 * menggunakan konteks sekolah dan yayasan yang sudah terikat pada instans ini.<br>
	 * <br>
	 * <b>Cara kerja:</b> Mendelegasikan sepenuhnya ke
	 * {@code initCriteriaStatic(order, selectedSekolah, selectedYayasan)},
	 * mengisi parameter sekolah dan yayasan dari field instans yang ditetapkan
	 * pada fase {@code doAfterCompose}.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code order} — jika {@code true}, hasil query akan diurutkan sesuai
	 *       kategori, tanggal, dan ID; jika {@code false}, untuk query COUNT</li>
	 * </ul>
	 * <b>Return:</b> Objek {@code Criteria} Hibernate yang siap dieksekusi.<br>
	 * <br>
	 * <b>Penanganan error:</b> Sama dengan {@code initCriteriaStatic}.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Metode ini merupakan titik penghubung antara state instans
	 * dan logika query statis. Tidak perlu mengubah metode ini secara terpisah dari
	 * {@code initCriteriaStatic}.
	 *
	 * @param order {@code true} untuk menyertakan klausa ORDER BY; {@code false} untuk COUNT
	 * @return kriteria Hibernate yang dikonfigurasi dengan filter sekolah dan yayasan instans
	 */
	public Criteria initCriteria(boolean order) {
		return initCriteriaStatic(order, selectedSekolah, selectedYayasan);
	}

	/**
	 * <b>Tujuan:</b> Setter untuk flag mode read-only halaman pengumuman vendor.<br>
	 * <br>
	 * <b>Cara kerja:</b> Menetapkan nilai field {@code readonly} dengan nilai yang diberikan.
	 * Flag ini dapat digunakan oleh komponen lain atau ZUL untuk memeriksa apakah
	 * operasi modifikasi data diizinkan pada halaman ini.<br>
	 * <br>
	 * <b>Parameter:</b>
	 * <ul>
	 *   <li>{@code readonly} — {@code true} jika halaman dalam mode hanya-baca;
	 *       {@code false} jika operasi penuh diizinkan</li>
	 * </ul>
	 * <b>Return:</b> void.<br>
	 * <br>
	 * <b>Penanganan error:</b> Tidak ada.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Pastikan logika tampilan ZUL memeriksa nilai ini via
	 * binding atau pemanggilan eksplisit jika diperlukan.
	 *
	 * @param readonly nilai flag mode read-only; {@code true} = hanya baca
	 */
	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	/**
	 * <b>Tujuan:</b> Getter untuk flag mode read-only halaman pengumuman vendor.<br>
	 * <br>
	 * <b>Cara kerja:</b> Mengembalikan nilai field {@code readonly} saat ini.
	 * Nilai awal adalah {@code false}, dapat diubah melalui {@code setReadonly}
	 * atau oleh mekanisme binding ZKoss.<br>
	 * <br>
	 * <b>Parameter:</b> tidak ada.<br>
	 * <b>Return:</b> {@code Boolean} — {@code true} jika halaman dalam mode hanya-baca,
	 * {@code false} jika operasi modifikasi diizinkan.<br>
	 * <br>
	 * <b>Penanganan error:</b> Tidak ada; nilai tidak pernah null secara default.<br>
	 * <br>
	 * <b>Pemeliharaan:</b> Tidak perlu perubahan khusus kecuali semantik flag ini berubah.
	 *
	 * @return nilai flag read-only saat ini
	 */
	public Boolean getReadonly() {
		return readonly;
	}

}
