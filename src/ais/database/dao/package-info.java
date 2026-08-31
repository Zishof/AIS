/**
 * Lapisan Data Access Object (DAO) Hibernate untuk seluruh entitas AIS — 284 file langsung di
 * paket ini plus sub-paket per modul ({@link ais.database.dao.akunting}, {@link
 * ais.database.dao.library}, {@link ais.database.dao.employ}, {@link ais.database.dao.rab},
 * {@link ais.database.dao.surat}, {@link ais.database.dao.asset}, {@link
 * ais.database.dao.kedokteran}, {@link ais.database.dao.beasiswa}).
 *
 * <h2>BACA INI DULU sebelum membuka satu pasang Dao/DaoImpl</h2>
 * <p>
 * <b>Mayoritas mutlak file di paket ini (dan sub-paketnya) HANYA berisi deklarasi kosong</b> —
 * sepasang {@code XxxDao} (interface, extends {@link ais.database.dao.GenericDao}) dan
 * {@code XxxDaoImpl} (kelas, extends {@link ais.database.dao.GenericHibernateDao}), tanpa satu
 * method pun di badannya. Ini BUKAN kode belum-selesai, placeholder, atau kode mati — seluruh
 * perilaku CRUD (simpan/hapus/muat/cari-semua/cari-berhalaman/pencarian bebas ilike-semua-kolom)
 * sudah diimplementasikan LENGKAP dan SEKALI SAJA oleh
 * {@link ais.database.dao.GenericHibernateDao}, yang didokumentasikan secara MENDALAM di sana —
 * <b>bacalah Javadoc {@link ais.database.dao.GenericDao} (kontrak) dan
 * {@link ais.database.dao.GenericHibernateDao} (implementasi) untuk memahami perilaku
 * sesungguhnya dari method apa pun yang dipanggil lewat pasangan Dao/DaoImpl mana pun di paket
 * ini</b> — javadoc pada file individual di paket ini SENGAJA tidak mengulang penjelasan yang
 * sama untuk setiap entitas, cukup menaut balik ke sini.
 * </p>
 *
 * <h2>Pola nama &amp; contoh</h2>
 * <p>
 * Konvensi baku: entitas {@code Foo} (di {@code ais.database.model}, atau sub-paket model yang
 * sepadan seperti {@code ais.database.model.sekolah}) mendapat {@code FooDao} (interface) dan
 * {@code FooDaoImpl} (kelas), keduanya di paket DAO yang sepadan dengan modul entitasnya. Contoh
 * minimal (persis seperti yang benar-benar ada di paket ini):
 * </p>
 * <pre>{@code
 * public interface AgamaDao extends GenericDao<Agama, Long> {
 * }
 *
 * public class AgamaDaoImpl extends GenericHibernateDao<Agama, Long, AgamaDao> implements AgamaDao {
 * }
 * }</pre>
 * <p>
 * Parameter tipe kedua ({@code Long} di atas) adalah tipe primary key entitas. Parameter tipe
 * ketiga pada {@code DaoImpl} ({@code AgamaDao}) adalah self-reference ke interface Dao-nya
 * sendiri — dipakai {@link ais.database.dao.GenericHibernateDao#setSession} agar method chaining
 * mengembalikan tipe konkret, bukan tipe generik mentah. Lihat Javadoc
 * {@link ais.database.dao.GenericDao} untuk penjelasan lengkap alasan desain ini.
 * </p>
 *
 * <h2>Kapan Dao/DaoImpl TIDAK kosong</h2>
 * <p>
 * Sebagian kecil pasangan menambah method spesifik-entitas di luar {@link
 * ais.database.dao.GenericDao} — biasanya query yang butuh kondisi lebih spesifik dari yang bisa
 * disediakan generik (mis. pencarian gabungan beberapa kolom dengan logika AND/OR khusus, atau
 * agregasi). File-file INI (bukan pasangan kosong) yang punya Javadoc method-level tersendiri di
 * berkasnya masing-masing — jangan berasumsi seluruh method sebuah DaoImpl otomatis berasal dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa memeriksa isi berkasnya.
 * </p>
 *
 * <h2>Cara memakai DAO dari kode aplikasi</h2>
 * <p>
 * Pola paling umum di codebase ini: {@code new FooDaoImpl().findById(id, false)},
 * {@code new FooDaoImpl().setSession(sesiNative).findAll()}, dsb — instance dibuat baru per
 * pemakaian (tidak disimpan sebagai field berumur panjang), konsisten dengan catatan "Sesi
 * Hibernate" pada Javadoc {@link ais.database.dao.GenericHibernateDao} soal risiko cache sesi
 * basi bila instance dipakai ulang lintas thread/request. Banyak kode ZK/action lama juga
 * langsung memakai {@link org.hibernate.Session}/{@link org.hibernate.Criteria} native tanpa
 * lewat DAO sama sekali untuk query yang lebih kompleks dari yang disediakan lapisan ini — kedua
 * pola hidup berdampingan di codebase ini, DAO bukan satu-satunya jalur akses data.
 * </p>
 *
 * @see ais.database.dao.GenericDao
 * @see ais.database.dao.GenericHibernateDao
 * @see ais.database.model.GeneralValueObject
 */
package ais.database.dao;
