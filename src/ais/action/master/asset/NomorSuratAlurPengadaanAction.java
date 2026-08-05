package ais.action.master.asset;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.surat.helper.AmbilDataNomorSuratBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.MyGrid;

/**
 * <h3>NomorSuratAlurPengadaanAction</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZK (ZKoss GenericAutowireComposer) yang mengelola
 * halaman konfigurasi <em>nomor surat alur pengadaan</em>. Setiap tahap dalam alur
 * pengadaan aset (misalnya Permintaan Pengadaan, Pemesanan, Penerimaan, BAST, dan
 * lain-lain) memerlukan format nomor surat yang unik dan terstruktur sesuai tata
 * kelola institusi. Halaman ini memungkinkan administrator untuk menetapkan
 * {@code NomorSurat} mana yang digunakan pada setiap tahap alur pengadaan
 * ({@code NomorSuratAlurPengadaan}), sehingga saat dokumen pengadaan dicetak atau
 * dikirim, sistem secara otomatis menghasilkan nomor surat dengan format yang benar.
 *
 * <b>Cara kerja:</b><br>
 * Setelah halaman ZUL dimuat oleh ZK, {@code doBeforeCompose} melakukan pemeriksaan
 * keamanan untuk memblokir akses yang tidak sah. Kemudian {@code doAfterCompose}
 * menyelesaikan inisialisasi komponen:
 * <ol>
 *   <li>Bahasa antarmuka diinisialisasi melalui {@code Common.initLaguage()}.</li>
 *   <li>Default konfigurasi nomor surat di-reload dari database via
 *       {@code NomorSuratAlurPengadaan.reloadDefault()} agar cache in-memory
 *       selalu sinkron dengan kondisi terbaru.</li>
 *   <li>Paging dikonfigurasi dengan listener yang memanggil {@code onSearchDefault}
 *       saat pengguna berganti halaman.</li>
 *   <li>Timer default dipasang untuk refresh otomatis berkala.</li>
 * </ol>
 * Grid menampilkan daftar {@code NomorSuratAlurPengadaan} yang dapat dicari berdasarkan
 * nama. Setiap baris memperlihatkan kode, nama (dengan tautan revisi), contoh format
 * nomor surat yang dipilih, dan komponen {@code AmbilDataNomorSuratBanbox} yang
 * memungkinkan pengguna langsung mengubah asosiasi nomor surat tanpa membuka form
 * terpisah. Perubahan langsung disimpan saat pengguna memilih nomor surat baru,
 * dan cache default di-reload agar perubahan segera berlaku.
 *
 * <b>Threading:</b><br>
 * Semua pemrosesan event terjadi di thread ZK Desktop. Listener pada
 * {@code AmbilDataNomorSuratBanbox} juga dieksekusi di thread ZK yang sama.
 * Timer default di-trigger oleh mekanisme timer ZK secara asinkron namun tetap
 * diproses satu per satu di thread yang sama. Sesi Hibernate diperoleh dari
 * {@code HibernateUtil.currentSession()} yang bersifat thread-local, sehingga
 * tidak ada risiko concurrent access ke sesi yang sama dari thread berbeda.
 * Instance kelas ini tidak thread-safe untuk digunakan dari beberapa thread.
 *
 * <b>Pemeliharaan:</b><br>
 * Jika alur pengadaan diperluas dengan tahap baru, tambahkan entri baru
 * {@code NomorSuratAlurPengadaan} melalui antarmuka ini atau melalui seeder
 * database. Untuk mengubah tampilan kolom grid, modifikasi renderer kelas inner
 * {@code NomorSuratAlurPengadaanRenderer} dan perbarui definisi {@code <column>}
 * di file ZUL yang berpasangan. Jika format contoh nomor surat tidak muncul
 * dengan benar, periksa implementasi {@code NomorSurat.getContohFormat()}.
 *
 * @author Tim Pengembang AIS
 * @version 1.0
 * @since ZKoss 5.5, Java 1.7
 */
public class NomorSuratAlurPengadaanAction extends GenericAutowireComposer {

	/**
	 * UID serialisasi untuk kompatibilitas versi kelas saat diserialisasi.
	 * Nilai ini tidak boleh diubah kecuali struktur kelas berubah secara
	 * tidak kompatibel dengan versi yang sudah ter-deploy.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/**
	 * Komponen paging ZK yang mengelola navigasi halaman pada grid daftar
	 * nomor surat alur pengadaan. Dikonfigurasi oleh {@code Common.initPaging}
	 * dengan listener untuk refresh data saat halaman berganti.
	 */
	private Paging paging;

	/**
	 * Komponen grid ZK yang menampilkan daftar baris {@code NomorSuratAlurPengadaan}.
	 * Dikonfigurasi menggunakan renderer kelas inner
	 * {@code NomorSuratAlurPengadaanRenderer}. Di-autowire oleh ZK berdasarkan
	 * atribut {@code id} di file ZUL.
	 */
	private MyGrid grid;

	/**
	 * Kotak teks pencarian untuk memfilter daftar nomor surat alur pengadaan
	 * berdasarkan nama. Pencarian bersifat case-insensitive dan mendukung
	 * pencocokan substring (MatchMode.ANYWHERE).
	 */
	private Textbox searchnama;
	private Textbox searchkode;

	/**
	 * <b>Tujuan:</b><br>
	 * Metode siklus hidup ZK yang dipanggil sebelum komponen ZUL dikompilasi dan
	 * diinisialisasi. Berfungsi sebagai titik pemeriksaan keamanan awal untuk
	 * memastikan hanya pengguna yang berwenang dapat mengakses halaman konfigurasi
	 * nomor surat alur pengadaan.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} yang memverifikasi sesi pengguna
	 * aktif dan hak aksesnya terhadap halaman ini. Jika pemeriksaan gagal, framework
	 * akan mengalihkan pengguna atau menampilkan pesan tidak berwenang sebelum
	 * komponen apapun dibentuk. Setelah pemeriksaan berhasil, memanggil
	 * {@code super.doBeforeCompose} untuk melanjutkan proses perakitan komponen ZK.
	 *
	 * <b>Parameter:</b><br>
	 * @param page     halaman ZK yang sedang dimuat
	 * @param parent   komponen induk tempat kontroler ini dipasang; bisa null
	 * @param compInfo metadata komponen ZK dari definisi ZUL
	 *
	 * <b>Return:</b><br>
	 * @return objek {@code ComponentInfo} dari {@code super.doBeforeCompose} yang
	 *         digunakan framework ZK untuk melanjutkan proses inisialisasi
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika pemeriksaan keamanan gagal, proses berhenti sebelum komponen terbentuk.
	 * Exception dari {@code super.doBeforeCompose} dipropagasi ke framework ZK.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Gunakan hanya untuk pemeriksaan keamanan atau konfigurasi minimal yang harus
	 * terjadi sebelum komponen dibentuk. Jangan tambahkan logika bisnis di sini.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Metode siklus hidup ZK yang dipanggil setelah seluruh komponen ZUL selesai
	 * dirakit dan di-wire ke field kontroler. Bertanggung jawab untuk inisialisasi
	 * lengkap halaman konfigurasi nomor surat alur pengadaan, termasuk pengaturan
	 * bahasa, reload data default, konfigurasi paging, dan pemasangan timer refresh.
	 *
	 * <b>Cara kerja:</b><br>
	 * Langkah inisialisasi dilakukan secara berurutan:
	 * <ol>
	 *   <li>{@code super.doAfterCompose(comp)} dipanggil agar ZK menyelesaikan
	 *       autowiring semua komponen ke field yang bersesuaian.</li>
	 *   <li>{@code Common.initLaguage()} menginisialisasi bahasa antarmuka sesuai
	 *       preferensi pengguna atau konfigurasi sistem.</li>
	 *   <li>{@code NomorSuratAlurPengadaan.reloadDefault()} me-reload cache in-memory
	 *       dari database untuk memastikan mapping nomor surat yang digunakan oleh
	 *       modul pengadaan selalu up-to-date dengan konfigurasi terbaru.</li>
	 *   <li>Paging diinisialisasi dengan {@code Common.initPaging}, mendaftarkan
	 *       listener yang memanggil {@code onSearchDefault} saat halaman berganti.</li>
	 *   <li>Timer default dipasang untuk refresh data grid secara berkala secara
	 *       otomatis tanpa interaksi pengguna.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param comp komponen root ZK yang menjadi kontainer dari seluruh komponen
	 *             yang didefinisikan di file ZUL; digunakan oleh metode induk untuk
	 *             menyelesaikan proses autowiring
	 *
	 * <b>Return:</b><br>
	 * Metode ini tidak mengembalikan nilai (void).
	 *
	 * <b>Penanganan error:</b><br>
	 * @throws Exception jika {@code super.doAfterCompose} gagal, misalnya karena
	 *                   komponen di ZUL tidak ditemukan atau id tidak cocok dengan
	 *                   nama field. Exception dipropagasi ke framework ZK.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika ada komponen tambahan yang perlu diinisialisasi saat halaman dimuat,
	 * tambahkan logikanya di sini setelah reload default. Pastikan
	 * {@code NomorSuratAlurPengadaan.reloadDefault()} selalu dipanggil di sini agar
	 * cache tidak basi jika konfigurasi diubah di sesi lain.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		NomorSuratAlurPengadaan.reloadDefault();

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
	 * <b>Tujuan:</b><br>
	 * Kelas inner yang bertindak sebagai renderer baris untuk grid daftar
	 * {@code NomorSuratAlurPengadaan}. Setiap instance renderer mengisi satu baris
	 * ZK {@code Row} dengan data dari satu entitas {@code NomorSuratAlurPengadaan},
	 * menampilkan kode, nama (dengan dukungan revisi), contoh format nomor surat,
	 * komponen pemilih nomor surat yang dapat diedit langsung di grid, dan keterangan.
	 *
	 * <b>Cara kerja:</b><br>
	 * Setiap baris di-render dengan kolom-kolom berikut:
	 * <ol>
	 *   <li><b>Kolom 1 (Kode):</b> Label berisi kode unik alur pengadaan
	 *       (misalnya "PP", "PO", "BAST").</li>
	 *   <li><b>Kolom 2 (Nama dengan revisi):</b> Widget {@code RevisiHelper} yang
	 *       menampilkan nama alur pengadaan beserta tautan riwayat revisi.</li>
	 *   <li><b>Kolom 3 (Contoh format):</b> Label {@code c} yang menampilkan
	 *       contoh format nomor surat dari {@code NomorSurat.getContohFormat()}.
	 *       Label ini diperbarui secara reaktif saat pengguna memilih nomor surat
	 *       baru melalui komponen banbox.</li>
	 *   <li><b>Kolom 4 (Banbox pemilih):</b> Komponen {@code AmbilDataNomorSuratBanbox}
	 *       yang memungkinkan pengguna memilih atau mengubah {@code NomorSurat}
	 *       yang diasosiasikan dengan alur pengadaan ini. Perubahan langsung
	 *       disimpan ke database dan cache default di-reload.</li>
	 *   <li><b>Kolom 5 (Keterangan):</b> Label berisi keterangan tambahan.</li>
	 * </ol>
	 * Listener pada banbox menangani perubahan pilihan: menyimpan asosiasi baru
	 * ke database via {@code Common.refreshUpdate}, memperbarui label contoh format,
	 * dan menjadwalkan reload cache default melalui timer.
	 *
	 * <b>Threading:</b><br>
	 * Renderer dan listener event-nya dieksekusi di thread ZK Desktop. Tidak ada
	 * akses concurrent ke sumber daya bersama dari thread lain.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika kolom baru perlu ditambahkan, tambahkan komponen ke {@code arg0} (Row)
	 * dan tambahkan definisi {@code <column>} di file ZUL. Pastikan urutan komponen
	 * yang ditambahkan ke Row sesuai dengan urutan kolom di ZUL.
	 */
	class NomorSuratAlurPengadaanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b><br>
		 * Mengisi satu baris ZK {@code Row} dengan data dari objek
		 * {@code NomorSuratAlurPengadaan} yang diberikan, menghasilkan tampilan
		 * baris lengkap pada grid konfigurasi nomor surat alur pengadaan dengan
		 * kemampuan edit inline melalui komponen banbox.
		 *
		 * <b>Cara kerja:</b><br>
		 * Parameter {@code arg1} di-cast ke {@code NomorSuratAlurPengadaan}.
		 * Kemudian komponen ZK ditambahkan ke baris {@code arg0} secara berurutan:
		 * label kode, widget revisi nama, label contoh format (disimpan di variabel
		 * {@code c} untuk diperbarui secara reaktif), banbox pemilih nomor surat
		 * (dengan nilai dan atribut awal dari nomor surat yang sudah terpilih,
		 * read-only karena edit dilakukan melalui popup banbox), dan label keterangan.
		 * Listener banbox dipasang untuk menangani perubahan: asosiasi baru disimpan
		 * ke database, label contoh format diperbarui, dan reload cache dijadwalkan
		 * melalui timer default. Alignment vertikal baris diset ke "top".
		 *
		 * <b>Parameter:</b><br>
		 * @param arg0 baris ZK ({@code Row}) yang akan diisi komponen; wadah untuk
		 *             semua sel dalam satu baris grid
		 * @param arg1 objek data baris; di-cast ke {@code NomorSuratAlurPengadaan}
		 *             yang berisi kode, nama, referensi nomor surat, dan keterangan
		 *
		 * <b>Return:</b><br>
		 * Metode ini tidak mengembalikan nilai (void); hasilnya adalah modifikasi
		 * langsung pada komponen baris {@code arg0}.
		 *
		 * <b>Penanganan error:</b><br>
		 * @throws Exception jika cast {@code arg1} gagal atau pembuatan komponen ZK
		 *                   gagal karena masalah siklus hidup komponen. Null-check
		 *                   diterapkan untuk {@code getNomorSurat()} di beberapa titik
		 *                   untuk menghindari NullPointerException pada alur pengadaan
		 *                   yang belum dikonfigurasi nomor suratnya.
		 *
		 * <b>Pemeliharaan:</b><br>
		 * Variabel {@code c} (Label contoh format) dideklarasikan final agar dapat
		 * diakses dari listener anonymous inner class. Jika perlu menambahkan
		 * komponen lain yang perlu diperbarui reaktif oleh listener, deklarasikan
		 * juga sebagai final di sini.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final NomorSuratAlurPengadaan nomorSuratAlurPengadaan = (NomorSuratAlurPengadaan) arg1;
			new Label(nomorSuratAlurPengadaan.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(NomorSuratAlurPengadaan.class, nomorSuratAlurPengadaan,
					nomorSuratAlurPengadaan.getNama()).setParent(arg0);

			final Label c;
			(c = new Label(nomorSuratAlurPengadaan.getNomorSurat() == null ? ""
					: nomorSuratAlurPengadaan.getNomorSurat().getContohFormat())).setParent(arg0);

			final AmbilDataNomorSuratBanbox ambilDataNomorSuratBanbox = new AmbilDataNomorSuratBanbox();
			ambilDataNomorSuratBanbox.setAttribute("nomorSurat", nomorSuratAlurPengadaan.getNomorSurat());
			ambilDataNomorSuratBanbox.setValue(nomorSuratAlurPengadaan.getNomorSurat() == null ? ""
					: nomorSuratAlurPengadaan.getNomorSurat().getNama());
			ambilDataNomorSuratBanbox.setWidth("95%");
			ambilDataNomorSuratBanbox.setReadonly(true);
			ambilDataNomorSuratBanbox.setParent(arg0);

			ambilDataNomorSuratBanbox.setEventListener(new EventListener() {

				/**
				 * <b>Tujuan:</b><br>
				 * Menangani event pemilihan nomor surat baru dari komponen banbox.
				 * Menyimpan asosiasi baru ke database, memperbarui label contoh format
				 * secara reaktif, dan menjadwalkan reload cache default.
				 *
				 * <b>Cara kerja:</b><br>
				 * Mengambil objek {@code NomorSurat} terpilih dari atribut banbox,
				 * menyetnya ke entitas {@code NomorSuratAlurPengadaan}, menyimpan
				 * perubahan ke database melalui {@code Common.refreshUpdate}, memperbarui
				 * teks label contoh format {@code c}, dan menjadwalkan
				 * {@code NomorSuratAlurPengadaan.reloadDefault()} melalui timer default
				 * agar cache in-memory segera mencerminkan konfigurasi baru.
				 *
				 * <b>Parameter:</b><br>
				 * @param arg0 objek event ZK yang dipicu oleh pemilihan nomor surat;
				 *             tidak digunakan secara langsung karena data diambil dari
				 *             atribut banbox
				 *
				 * <b>Return:</b><br>
				 * Metode ini tidak mengembalikan nilai (void).
				 *
				 * <b>Penanganan error:</b><br>
				 * @throws Exception jika operasi simpan ke database gagal atau jika
				 *                   timer tidak dapat dijadwalkan. Exception dipropagasi
				 *                   ke framework ZK untuk ditangani di level lebih tinggi.
				 *
				 * <b>Pemeliharaan:</b><br>
				 * {@code reloadDefault} dijadwalkan melalui timer (bukan langsung) untuk
				 * menghindari potensi masalah dengan sesi Hibernate yang masih aktif
				 * pada thread yang sama saat event ini diproses.
				 */
				@Override
				public void onEvent(Event arg0) throws Exception {
					NomorSurat ns = (NomorSurat) ambilDataNomorSuratBanbox.getAttribute("nomorSurat");
					nomorSuratAlurPengadaan.setNomorSurat(ns);
					Common.refreshUpdate(nomorSuratAlurPengadaan);

					c.setValue(nomorSuratAlurPengadaan.getNomorSurat() == null ? ""
							: nomorSuratAlurPengadaan.getNomorSurat().getContohFormat());


					Common.createDefaultTimer(new EventListener() {

						/**
						 * <b>Tujuan:</b><br>
						 * Me-reload cache default nomor surat alur pengadaan setelah
						 * timer berjalan, memastikan perubahan konfigurasi yang baru
						 * disimpan segera tercermin dalam cache in-memory yang digunakan
						 * oleh modul pengadaan lain.
						 *
						 * <b>Cara kerja:</b><br>
						 * Memanggil {@code NomorSuratAlurPengadaan.reloadDefault()} yang
						 * membaca ulang seluruh konfigurasi nomor surat alur pengadaan
						 * dari database dan memperbarui cache statis yang digunakan untuk
						 * penomoran dokumen pengadaan.
						 *
						 * <b>Parameter:</b><br>
						 * @param arg0 objek event timer ZK; tidak digunakan secara langsung
						 *
						 * <b>Return:</b><br>
						 * Metode ini tidak mengembalikan nilai (void).
						 *
						 * <b>Penanganan error:</b><br>
						 * @throws Exception jika operasi reload dari database gagal,
						 *                   misalnya karena koneksi database terputus
						 *
						 * <b>Pemeliharaan:</b><br>
						 * Pastikan {@code NomorSuratAlurPengadaan.reloadDefault()} bersifat
						 * idempoten dan aman dipanggil berulang kali tanpa efek samping.
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {
							NomorSuratAlurPengadaan.reloadDefault();
						}
					});
				}
			});

			new Label(nomorSuratAlurPengadaan.getKeterangan()).setParent(arg0);

		}

	}

	/**
	 * <b>Tujuan:</b><br>
	 * Membangun dan mengembalikan objek {@code Criteria} Hibernate untuk mengambil
	 * daftar {@code NomorSuratAlurPengadaan} dari database, dengan filter pencarian
	 * teks berdasarkan nama dan opsi pengurutan berdasarkan kode secara ascending.
	 *
	 * <b>Cara kerja:</b><br>
	 * Sesi Hibernate saat ini diperoleh dari {@code HibernateUtil.currentSession()}.
	 * Criteria dibuat untuk entitas {@code NomorSuratAlurPengadaan}. Filter pencarian
	 * diterapkan berdasarkan isi {@code searchnama}:
	 * <ul>
	 *   <li>Jika nilai pencarian kosong (setelah trim), gunakan
	 *       {@code Restrictions.sqlRestriction("true")} sehingga semua data
	 *       dikembalikan tanpa filter.</li>
	 *   <li>Jika ada nilai pencarian, gunakan {@code Restrictions.ilike("nama", ...)}
	 *       dengan {@code MatchMode.ANYWHERE} untuk pencarian substring
	 *       case-insensitive pada kolom nama.</li>
	 * </ul>
	 * Jika parameter {@code order} bernilai true, tambahkan urutan ascending
	 * berdasarkan kolom {@code kode} agar daftar tampil secara alfabetis konsisten.
	 *
	 * <b>Parameter:</b><br>
	 * @param order jika {@code true}, tambahkan ORDER BY kode ASC pada criteria;
	 *              jika {@code false}, criteria tidak memiliki urutan (digunakan
	 *              untuk operasi hitung total baris via {@code Common.initPaging})
	 *
	 * <b>Return:</b><br>
	 * @return objek {@code Criteria} Hibernate yang siap dieksekusi dengan
	 *         {@code .list()} untuk mengambil data terfilter dan terurut, atau
	 *         digunakan langsung oleh {@code Common.initPaging} untuk menghitung
	 *         total baris
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika sesi Hibernate tidak aktif, Hibernate akan melempar
	 * {@code HibernateException}. Tidak ada penanganan exception di dalam metode
	 * ini; caller bertanggung jawab menangani exception tersebut.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Untuk menambah filter baru (misalnya filter berdasarkan kode tertentu atau
	 * status aktif), tambahkan {@code Restrictions} baru ke criteria yang sudah ada.
	 * Pastikan nama properti yang digunakan sesuai dengan field yang didefinisikan
	 * di kelas entitas {@code NomorSuratAlurPengadaan} dan mapping Hibernate-nya.
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(NomorSuratAlurPengadaan.class);

		if (order)
			criteria.addOrder(Order.asc("kode"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Memuat dan menampilkan ulang daftar {@code NomorSuratAlurPengadaan} pada grid
	 * sesuai dengan kata kunci pencarian dan halaman yang sedang aktif. Metode ini
	 * merupakan inti fungsi pencarian di halaman ini dan dipanggil saat pengguna
	 * mencari, berganti halaman, atau saat timer otomatis berjalan.
	 *
	 * <b>Cara kerja:</b><br>
	 * Proses dilakukan dalam beberapa langkah:
	 * <ol>
	 *   <li>Menghitung total baris dan mengonfigurasi paging melalui
	 *       {@code Common.initPaging(initCriteria(false), paging)}, yang secara
	 *       internal menghitung total data dari criteria tanpa urutan dan memperbarui
	 *       properti paging (total size, visibilitas).</li>
	 *   <li>Mengambil data untuk halaman aktif menggunakan {@code initCriteria(true)}
	 *       dengan batasan jumlah baris ({@code Common.ROWS_COUNT_ON_PAGE}) dan
	 *       offset dihitung dari {@code paging.getActivePage()}.</li>
	 *   <li>Membuat model list dari hasil query menggunakan {@code SimpleListModel}.</li>
	 *   <li>Menerapkan renderer {@code NomorSuratAlurPengadaanRenderer} baru ke grid.</li>
	 *   <li>Memanggil {@code grid.setModelCheckMobile(strset)} untuk menampilkan
	 *       data dengan dukungan deteksi perangkat mobile.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b><br>
	 * @param event objek event ZK yang memicu pencarian; bisa null jika dipanggil
	 *              secara programatik dari timer atau inisialisasi. Tidak digunakan
	 *              secara langsung dalam metode ini.
	 *
	 * <b>Return:</b><br>
	 * Metode ini tidak mengembalikan nilai (void); hasilnya adalah pembaruan tampilan
	 * grid dan komponen paging pada halaman ZK.
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari query Hibernate tidak ditangani di sini dan dipropagasi ke
	 * framework ZK. Anotasi {@code @SuppressWarnings("unchecked")} digunakan karena
	 * {@code Criteria.list()} mengembalikan raw type pada level kompiler.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Metode ini menggunakan pola dua-langkah (hitung total dulu, lalu ambil data)
	 * yang efisien untuk paginasi. Jika perlu menambahkan logika pasca-pencarian
	 * (misalnya pesan "tidak ada data"), tambahkan setelah
	 * {@code grid.setModelCheckMobile(strset)}.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<NomorSuratAlurPengadaan> nomorSuratAlurPengadaan = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(nomorSuratAlurPengadaan);
		grid.setRowRenderer(new NomorSuratAlurPengadaanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
