package ais.action.master.akunting;

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
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.MyGrid;

/**
 * <h3>NomorSuratAlurKeuanganAction — Konfigurasi Format Nomor Surat Alur Keuangan</h3>
 *
 * <p><b>Untuk apa:</b> Controller ZK yang mengelola halaman konfigurasi
 * "Nomor Surat Alur Keuangan" — sebuah halaman master data yang memungkinkan
 * administrator keuangan menetapkan format nomor surat yang digunakan untuk
 * setiap jenis alur transaksi keuangan (misalnya format nomor surat untuk
 * kas kecil, kas besar, uang muka, proses transfer, dana talangan, dsb.).
 * Setiap entri {@link NomorSuratAlurKeuangan} menghubungkan kode alur keuangan
 * dengan template {@link NomorSurat} yang menentukan format penomoran otomatis
 * surat/dokumen transaksi terkait.</p>
 *
 * <p><b>Cara kerja:</b> Halaman ini menampilkan grid daftar semua jenis alur
 * keuangan beserta format nomor surat yang dikonfigurasi untuk masing-masing.
 * Pengguna dapat mengubah template nomor surat via {@code AmbilDataNomorSuratBanbox}
 * (bandbox picker) langsung di baris grid tanpa membuka form terpisah. Perubahan
 * tersimpan otomatis via {@code Common.refreshUpdate} dan template nomor surat
 * di-reload ke cache static {@link NomorSuratAlurKeuangan#reloadDefault()} agar
 * efektif segera untuk transaksi berikutnya.</p>
 *
 * <p><b>Komponen kunci:</b>
 * <ul>
 *   <li>{@code paging}: ZK {@code Paging} untuk navigasi halaman grid</li>
 *   <li>{@code grid}: {@code MyGrid} tempat daftar alur keuangan ditampilkan</li>
 *   <li>{@code searchnama}: textbox pencarian berdasarkan nama alur</li>
 *   <li>Inner class {@code NomorSuratAlurKeuanganRenderer}: merender setiap baris
 *       grid dengan kolom kode, nama (editable revisi), contoh format, bandbox
 *       pemilih template, dan keterangan</li>
 * </ul>
 * </p>
 *
 * <p><b>Inisialisasi:</b> {@link #doAfterCompose} memanggil
 * {@code NomorSuratAlurKeuangan.reloadDefault()} untuk memastikan data default
 * termuat dari database, lalu menginisialisasi paging dan timer untuk pencarian
 * otomatis. Timer ZK memicu {@link #onSearchDefault} setelah komponen siap
 * sehingga grid terisi tanpa interaksi pengguna.</p>
 *
 * <p><b>Threading:</b> Semua operasi berjalan di event thread ZK. Session Hibernate
 * digunakan via {@code HibernateUtil.currentSession()} — session ini dikelola
 * oleh filter ZK/Hibernate dan tidak perlu ditutup secara manual.
 * Java 1.7, ZKoss 5.5.</p>
 */
public class NomorSuratAlurKeuanganAction extends GenericAutowireComposer {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;

	/**
	 * Memeriksa hak akses pengguna sebelum halaman dirender.
	 *
	 * <p><b>Tujuan:</b> Mencegah akses tidak sah ke halaman konfigurasi nomor surat
	 * alur keuangan. Pemeriksaan dilakukan sebelum komponen ZK diinisialisasi sehingga
	 * pengguna yang tidak berhak tidak melihat data apapun dari halaman ini.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link Common#doCheckSecurity()} yang memverifikasi
	 * role dan privilege pengguna dari sesi aktif. Jika tidak berhak, exception dilempar
	 * atau pengguna diarahkan ke halaman error. Kemudian meneruskan ke
	 * {@code super.doBeforeCompose} untuk menyelesaikan fase pra-komposisi ZK.</p>
	 *
	 * @param page     halaman ZK yang dikomposisi
	 * @param parent   komponen induk dalam hierarki ZK
	 * @param compInfo metadata komponen dari ZK framework
	 * @return info komponen dari implementasi induk
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Menginisialisasi halaman setelah semua komponen ZK tersedia via autowire.
	 *
	 * <p><b>Tujuan:</b> Hook lifecycle ZK yang dipanggil setelah komponen ZUL
	 * selesai dibuat dan di-wire ke field Java. Metode ini melakukan inisialisasi
	 * yang bergantung pada ketersediaan komponen UI, yaitu:
	 * <ol>
	 *   <li>Inisialisasi bahasa/label via {@code Common.initLaguage()}</li>
	 *   <li>Reload cache format nomor surat default via
	 *       {@code NomorSuratAlurKeuangan.reloadDefault()} — memastikan data
	 *       terbaru dari database termuat ke cache statik yang digunakan
	 *       untuk penomoran otomatis transaksi</li>
	 *   <li>Inisialisasi paging dengan listener yang memicu pencarian ulang
	 *       saat pengguna berpindah halaman</li>
	 *   <li>Membuat timer default yang memicu {@link #onSearchDefault} setelah
	 *       halaman selesai dirender, sehingga grid terisi secara otomatis
	 *       tanpa interaksi pengguna</li>
	 * </ol>
	 * </p>
	 *
	 * @param comp komponen root yang sudah selesai dikomposisi
	 * @throws Exception jika inisialisasi induk atau komponen gagal
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		NomorSuratAlurKeuangan.reloadDefault();

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
	 * <h3>NomorSuratAlurKeuanganRenderer — Renderer Baris Grid Nomor Surat Alur Keuangan</h3>
	 *
	 * <p><b>Untuk apa:</b> Inner class yang merender setiap baris grid daftar alur keuangan.
	 * Setiap baris menampilkan kode alur, nama (dengan widget revisi), contoh format nomor
	 * surat (label read-only yang diupdate saat template berubah), bandbox pemilih template
	 * nomor surat (editable inline), dan keterangan alur. Komponen bandbox memungkinkan
	 * perubahan template tanpa membuka dialog terpisah — langsung dari grid.</p>
	 */
	class NomorSuratAlurKeuanganRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris grid untuk satu entitas {@link NomorSuratAlurKeuangan}.
		 *
		 * <p><b>Tujuan:</b> Mengisi baris grid dengan komponen UI yang sesuai untuk
		 * setiap kolom data alur keuangan. Renderer ini mengimplementasikan pola
		 * "edit inline" — pengguna dapat mengubah template nomor surat langsung
		 * dari baris grid tanpa harus membuka form edit terpisah.</p>
		 *
		 * <p><b>Cara kerja (urutan kolom):</b>
		 * <ol>
		 *   <li><b>Kode</b>: Label baca-saja berisi kode unik alur (misal "KK01", "UM02").</li>
		 *   <li><b>Nama</b>: Widget revisi dari {@code RevisiHelper.createNewRevisi} yang
		 *       menampilkan nama alur dengan kemungkinan riwayat revisi.</li>
		 *   <li><b>Contoh Format</b>: Label {@code c} yang menampilkan contoh nomor surat
		 *       sesuai template yang terpilih (misal "001/KK/VI/2026"). Label ini diupdate
		 *       secara dinamis saat pengguna memilih template baru via bandbox.</li>
		 *   <li><b>Template Nomor Surat</b>: {@code AmbilDataNomorSuratBanbox} (bandbox
		 *       picker read-only) menampilkan nama template terpilih. Event listener
		 *       pada bandbox menyimpan template baru ke database via
		 *       {@code Common.refreshUpdate}, memperbarui label contoh format, dan
		 *       me-reload cache default nomor surat via timer (agar perubahan efektif
		 *       untuk transaksi baru tanpa restart server).</li>
		 *   <li><b>Keterangan</b>: Label baca-saja berisi deskripsi alur.</li>
		 * </ol>
		 * </p>
		 *
		 * <p><b>Penanganan error:</b> Exception dari proses penyimpanan akan menyebar ke
		 * event listener ZK dan ditangani oleh framework. Timer reload dijalankan
		 * setelah penyimpanan berhasil untuk memastikan konsistensi cache.</p>
		 *
		 * @param arg0 baris ZK ({@link Row}) yang akan diisi komponen
		 * @param arg1 objek data ({@link NomorSuratAlurKeuangan}) yang akan dirender
		 * @throws Exception jika proses rendering atau event gagal
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final NomorSuratAlurKeuangan nomorSuratAlurKeuangan = (NomorSuratAlurKeuangan) arg1;
			new Label(nomorSuratAlurKeuangan.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(NomorSuratAlurKeuangan.class, nomorSuratAlurKeuangan,
					nomorSuratAlurKeuangan.getNama()).setParent(arg0);

			final Label c;
			(c = new Label(nomorSuratAlurKeuangan.getNomorSurat() == null ? ""
					: nomorSuratAlurKeuangan.getNomorSurat().getContohFormat())).setParent(arg0);

			final AmbilDataNomorSuratBanbox ambilDataNomorSuratBanbox = new AmbilDataNomorSuratBanbox();
			ambilDataNomorSuratBanbox.setAttribute("nomorSurat", nomorSuratAlurKeuangan.getNomorSurat());
			ambilDataNomorSuratBanbox.setValue(nomorSuratAlurKeuangan.getNomorSurat() == null ? ""
					: nomorSuratAlurKeuangan.getNomorSurat().getNama());
			ambilDataNomorSuratBanbox.setWidth("95%");
			ambilDataNomorSuratBanbox.setReadonly(true);
			ambilDataNomorSuratBanbox.setParent(arg0);

			ambilDataNomorSuratBanbox.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					NomorSurat ns = (NomorSurat) ambilDataNomorSuratBanbox.getAttribute("nomorSurat");
					nomorSuratAlurKeuangan.setNomorSurat(ns);
					Common.refreshUpdate(nomorSuratAlurKeuangan);

					c.setValue(nomorSuratAlurKeuangan.getNomorSurat() == null ? ""
							: nomorSuratAlurKeuangan.getNomorSurat().getContohFormat());
					
					
					Common.createDefaultTimer(new EventListener() {
						
						@Override
						public void onEvent(Event arg0) throws Exception {
							NomorSuratAlurKeuangan.reloadDefault();
						}
					});
				}
			});

			new Label(nomorSuratAlurKeuangan.getKeterangan()).setParent(arg0);

		}

	}

	/**
	 * Membuat kriteria Hibernate untuk pencarian data alur keuangan dengan opsi pengurutan.
	 *
	 * <p><b>Tujuan:</b> Menyiapkan objek {@link Criteria} Hibernate yang siap dieksekusi
	 * untuk mengambil data {@link NomorSuratAlurKeuangan} dari database sesuai filter
	 * pencarian yang aktif. Dipanggil dua kali dari {@link #onSearchDefault}: sekali
	 * tanpa urutan untuk menghitung jumlah total (pagination), dan sekali dengan urutan
	 * untuk mengambil data halaman yang diperlukan.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mengambil {@code currentSession()} Hibernate — session yang dikelola
	 *       oleh filter ZK/Hibernate, tidak perlu ditutup manual.</li>
	 *   <li>Membuat {@code Criteria} untuk kelas {@code NomorSuratAlurKeuangan}.</li>
	 *   <li>Jika {@code order} bernilai true, menambahkan {@code Order.asc("kode")}
	 *       agar baris diurutkan alfabet berdasarkan kode alur.</li>
	 *   <li>Menambahkan filter nama: jika textbox {@code searchnama} kosong,
	 *       menggunakan {@code sqlRestriction("true")} (semua data); jika tidak
	 *       kosong, menggunakan {@code ilike} dengan {@code ANYWHERE} untuk
	 *       pencarian substring case-insensitive pada field {@code nama}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Untuk menambah filter baru (misalnya berdasarkan kode),
	 * tambahkan {@code Restrictions} tambahan setelah filter nama. Pastikan field
	 * textbox baru juga dideklarasikan dan di-wire ke ZUL.</p>
	 *
	 * @param order {@code true} jika hasil perlu diurutkan berdasarkan kode alur;
	 *              {@code false} jika urutan tidak diperlukan (untuk COUNT pagination)
	 * @return objek {@link Criteria} yang siap dieksekusi untuk query data atau count
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(NomorSuratAlurKeuangan.class);

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
	 * Menjalankan pencarian data dan memperbarui grid dengan hasil yang ditemukan.
	 *
	 * <p><b>Tujuan:</b> Event handler utama untuk pencarian dan refresh grid daftar
	 * nomor surat alur keuangan. Dipanggil oleh: paging listener (saat pengguna
	 * berpindah halaman), timer default (saat halaman pertama kali dimuat), dan
	 * dapat dipanggil manual dari tombol cari jika ada di ZUL.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code Common.initPaging(initCriteria(false), paging)} untuk
	 *       menghitung jumlah total baris dari database dan mengupdate komponen
	 *       paging (total halaman, navigasi).</li>
	 *   <li>Memanggil {@code initCriteria(true)} untuk mendapatkan data yang
	 *       diurutkan berdasarkan kode, dengan {@code setMaxResults} dan
	 *       {@code setFirstResult} sesuai halaman aktif pada paging.</li>
	 *   <li>Mengubah hasil list menjadi {@code SimpleListModel} untuk diumpankan
	 *       ke grid.</li>
	 *   <li>Menyetel renderer baru ({@code NomorSuratAlurKeuanganRenderer}) ke
	 *       grid dan menampilkan data via {@code grid.setModelCheckMobile}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Anotasi @SuppressWarnings:</b> Digunakan untuk menekan peringatan unchecked
	 * dari konversi list Hibernate (raw type) ke {@code List<NomorSuratAlurKeuangan>}
	 * karena framework Hibernate 3.x belum menggunakan generics.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika perlu menambah kolom tampilan, perbarui
	 * {@code NomorSuratAlurKeuanganRenderer}. Jika perlu menambah filter, perbarui
	 * {@link #initCriteria(boolean)} dan tambahkan komponen filter di ZUL.</p>
	 *
	 * @param event event ZK pemicu (bisa null jika dipanggil dari timer/paging listener)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<NomorSuratAlurKeuangan> nomorSuratAlurKeuangan = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(nomorSuratAlurKeuangan);
		grid.setRowRenderer(new NomorSuratAlurKeuanganRenderer());
		grid.setModelCheckMobile(strset);

	}

}
