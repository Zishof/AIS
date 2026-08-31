package ais.database.dao;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;

/**
 * Kontrak DAO generik yang menjadi fondasi <b>seluruh</b> lapisan akses data AIS berbasis
 * Hibernate — hampir setiap entitas di {@code ais.database.model} punya sepasang
 * {@code <Entitas>Dao} (interface ini) + {@code <Entitas>DaoImpl} (implementasi, lihat
 * {@link GenericHibernateDao}) di paket {@code ais.database.dao} (dan sub-paketnya per modul:
 * {@code akunting}, {@code library}, {@code employ}, {@code sekolah}, dst). Berdasarkan pola
 * publik "Generic Data Access Objects" (Hibernate community wiki, sekitar era Hibernate 3), namun
 * sudah lama menyimpang jauh dari sumber aslinya lewat penambahan method {@code search}/varian
 * paginasi yang tidak ada di artikel rujukan.
 *
 * <h2>Kenapa hampir semua Dao/DaoImpl kosong</h2>
 * <p>
 * Bila Anda membaca satu file {@code XxxDao.java}/{@code XxxDaoImpl.java} dan isinya HANYA
 * deklarasi {@code extends}/{@code implements} tanpa method apa pun — itu BUKAN berkas
 * belum-selesai atau kode mati; itu memang seharusnya kosong. Seluruh perilaku CRUD (simpan,
 * hapus, cari-by-id, cari-semua, cari-berhalaman, pencarian teks bebas ilike di semua kolom
 * String, dsb) sudah diimplementasikan SEKALI untuk semua entitas oleh {@link GenericHibernateDao},
 * dan interface generik ini (yang dideklarasikan lewat parameter tipe {@code <T, ID>}) yang
 * memberi setiap pasangan Dao/DaoImpl per-entitas kombinasi tipe yang benar (mis.
 * {@code AgamaDao extends GenericDao<Agama, Long>}). Pasangan kosong ini murni ada untuk
 * penamaan/pengetikan kuat dan untuk titik injeksi Spring (setiap {@code XxxDaoImpl} biasanya
 * didaftarkan sebagai bean di konfigurasi Spring XML dengan nama bean = nama kelas), BUKAN untuk
 * menambah perilaku baru. Menambah method spesifik-entitas (mis. {@code findByNis(String nis)})
 * ke salah satu Dao HANYA dilakukan bila perilaku generik di sini benar-benar tidak cukup — pola
 * yang lebih umum di codebase ini justru menulis query spesifik langsung di kelas
 * {@code ais.action.*} atau helper terkait lewat {@link org.hibernate.Session} native, bukan
 * menambah method baru ke Dao.
 * </p>
 *
 * <h2>Konvensi penamaan &amp; parameter tipe</h2>
 * <p>
 * {@code T} adalah kelas entitas Hibernate (biasanya turunan
 * {@link ais.database.model.GeneralValueObject}), {@code ID extends Serializable} adalah tipe
 * primary key-nya (hampir selalu {@code Long}, kadang {@code String} untuk entitas berkode alami).
 * Implementasi konkret {@code XxxDaoImpl} menambahkan parameter tipe ketiga
 * {@code DaoImpl extends GenericDao<T, ID>} (self-referencing, lihat {@link GenericHibernateDao})
 * agar method {@link GenericHibernateDao#setSession(Session)} dapat mengembalikan tipe implementasi
 * konkret untuk method chaining, bukan tipe generik {@code GenericHibernateDao} mentah.
 * </p>
 *
 * <h2>Catatan penting tiap kelompok method</h2>
 * <ul>
 * <li><b>{@code findById}/{@code load}</b>: memakai {@link Session#load} (lazy proxy Hibernate,
 * melempar {@code ObjectNotFoundException} bila diakses tapi baris tidak ada), BUKAN
 * {@link Session#get} (yang mengembalikan {@code null} bila tidak ada). Pemanggil yang
 * mengharapkan {@code null} untuk "tidak ditemukan" akan salah asumsi di sini.</li>
 * <li><b>{@code search(String)}</b> dan variannya: membangun kondisi {@code ILIKE '%kata%'} pada
 * SEMUA kolom bertipe {@code String} milik entitas secara otomatis (via reflection metadata
 * Hibernate {@code ClassMetadata}), digabung dengan {@code OR}. Praktis untuk kotak pencarian
 * generik di layar ZK, tapi berarti performanya menurun linear terhadap jumlah kolom String
 * entitas dan TIDAK memakai index parsial/full-text — tidak cocok untuk tabel besar dengan banyak
 * kolom teks.</li>
 * <li><b>{@code findPageByPage}/{@code findByCriteriaPageByPage}</b>: paginasi native Hibernate
 * ({@code setFirstResult}/{@code setMaxResults}) — OFFSET-based, bukan keyset/cursor, sehingga
 * halaman besar pada tabel besar tetap mahal (database tetap men-scan+skip baris sebelum offset).</li>
 * <li><b>{@code update(T)}</b> vs <b>{@code update(String, T)}</b>: overload pertama (tanpa nama
 * entitas Hibernate) didelegasikan ke {@code Common.refreshUpdate} di implementasi — BUKAN
 * {@code Session.update} polos, ada logika tambahan di sana (lihat {@link GenericHibernateDao}).</li>
 * <li><b>{@code saveOrUpdate(T, Serializable id)}</b>: cek eksistensi dulu lewat {@link #isExist}
 * sebelum memilih save vs merge+update — BUKAN memakai {@code Session.saveOrUpdate} Hibernate
 * bawaan (yang menentukan insert/update dari nilai identifier, bukan query eksistensi terpisah).</li>
 * </ul>
 *
 * @see <a href='http://www.hibernate.org/328.html'>Generic Data Access
 *      Objects</a> (artikel rujukan asli, sudah tidak aktif di web.archive.org)
 * @see GenericHibernateDao
 */
public interface GenericDao<T, ID extends Serializable> {

	/** Sesi Hibernate yang sedang dipakai DAO ini — dibuka lazy dari {@link ais.database.hibernate.HibernateUtil#currentSession()} bila belum ada, lihat {@link GenericHibernateDao#getSession()}. */
	public Session getCurrentSession();

	/** {@link Criteria} kosong (belum ada restriction/order) untuk kelas entitas {@code T} milik DAO ini — titik awal umum untuk query kustom di luar method generik di interface ini. */
	public Criteria getCriteria();

	/**
	 * @param id
	 * @return Boolean
	 */
	public Boolean isExist(Serializable id);

	/**
	 * @param id
	 * @param propertyName
	 * @return kembaliannya
	 */
	public Boolean isExist(Serializable id, String propertyName);

	/**
	 * @param id
	 * @param lock
	 * @return kembaliannya
	 */
	public T findById(ID id, Boolean lock);

	public void persis(T obj);

	/**
	 * @return kembaliannya
	 */
	public List<T> findAll();

	/**
	 * @param order
	 * @return kembaliannya
	 */
	public List<T> findAll(Order order);

	/**
	 * @param order
	 * @return kembaliannya
	 */
	public List<T> findAll(Order... order);

	/**
	 * @param firstResult
	 * @param maxResults
	 * @return kembaliannya
	 */
	public List<T> findPageByPage(int firstResult, int maxResults);

	/**
	 * @param exampleInstance
	 * @param excludeProperty
	 * @return kembaliannya
	 */
	public List<T> findByExample(T exampleInstance, String... excludeProperty);

	/**
	 * @param entity
	 * @return kembaliannya
	 */
	public T makePersistent(T entity);

	/**
	 * @param entity
	 */
	public void makeTransient(T entity);

	/**
	 * @param entity
	 */
	public void refresh(T entity);

	/**
	 * @param entity
	 * @param lockMode
	 */
	public void refresh(T entity, LockMode lockMode);

	/**
	 * @param like
	 * @return kembaliannya
	 */
	public List<T> search(Hashtable<String, ? extends Object> like);

	/**
	 * @param search
	 * @return kembaliannya
	 */
	public List<T> search(String search);

	public List<T> search(String search, Order[] order,
			Criterion... criterionses);

	public List<T> search(String search, Criterion... criterionses);

	public List<T> search(String search, Order... order);

	/**
	 * @param like
	 * @param orders
	 * @return kembaliannya
	 */
	public List<T> search(Hashtable<String, ? extends Object> like,
			Order... orders);

	/**
	 * @param like
	 * @param orders
	 * @return kembaliannya
	 */
	public List<T> search(List<? extends String> like, Order... orders);

	/**
	 * @param entity
	 */
	public void save(T entity);

	/**
     *
     */
	// public void beginTransaction();
	//
	//
	// public void rollbackTransaction();
	//
	// /**
	// *
	// */
	// public void commitTransaction();

	/**
	 * @param id
	 * @return kembaliannya
	 */
	public T load(Serializable id);

	/**
	 * @param entity
	 */
	public void update(T entity);

	/**
	 * @param entity
	 * @param id
	 */
	public void saveOrUpdate(T entity, Serializable id);

	public void flush();

	public void evict(T entity);

	/**
	 * @param entity
	 */
	public void delete(T entity);

	/**
	 * @param identityName
	 * @param entity
	 */
	public void saveOrUpdate(String identityName, T entity);

	/**
	 * @param identityName
	 * @param entity
	 */
	public void update(String identityName, T entity);

	/**
	 * @param entity
	 * @return kembaliannya
	 */
	public T merge(T entity);

	/**
	 * @param criterion
	 * @return kembaliannya
	 */
	public List<T> findByCriteria(Criterion... criterion);

	public List<T> findByCriteria(Order order, Criterion... criterion);

	/**
	 * @param firstResult
	 * @param maxResults
	 * @param criterion
	 * @return kembaliannya
	 */
	public List<T> findByCriteriaPageByPage(int firstResult, int maxResults,
			Criterion... criterion);

}
