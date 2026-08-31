package ais.database.dao;

/**
 * Created by IntelliJ IDEA.
 * User:
 * Date: Jan 3, 2007
 * Time: 3:56:57 PM
 */

/*
 * Javadoc kelas lengkap ada tepat di atas deklarasi class GenericHibernateDao di bawah (setelah
 * blok import) -- header IntelliJ di atas ini adalah komentar peninggalan asli, dipertahankan
 * apa adanya sebagai jejak sejarah berkas (per aturan "jangan mengurangi dokumentasi yang ada").
 */

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * Implementasi kanonik TUNGGAL dari {@link GenericDao} — dasar bagi (hampir) setiap
 * {@code XxxDaoImpl} di {@code ais.database.dao} dan sub-paketnya. Lihat Javadoc
 * {@link GenericDao} lebih dulu untuk gambaran kenapa mayoritas subclass ({@code XxxDao}/
 * {@code XxxDaoImpl}) benar-benar kosong: SELURUH logika CRUD generik (simpan, hapus, muat,
 * cari-semua, cari-berhalaman, pencarian bebas ilike-semua-kolom-String) ada persis di sini,
 * satu kali, untuk semua entitas.
 *
 * <h2>Bagaimana tipe persisten ditentukan (reflection generic)</h2>
 * <p>
 * Konstruktor {@link #GenericHibernateDao()} membaca parameter tipe generik AKTUAL milik
 * subclass lewat reflection ({@code getClass().getGenericSuperclass()}), BUKAN lewat parameter
 * constructor eksplisit. Ini berarti setiap {@code XxxDaoImpl} WAJIB meng-extend kelas ini secara
 * langsung dengan tipe konkret tersurat di deklarasi class (mis.
 * {@code class AgamaDaoImpl extends GenericHibernateDao<Agama, Long, AgamaDao>}) — bila suatu
 * saat ada subclass abstrak PERANTARA di antara kelas ini dan implementasi konkret tanpa
 * parameter tipe tersurat sendiri, {@code getActualTypeArguments()} akan gagal/mengembalikan
 * tipe yang salah. Pola ini sudah dipakai konsisten di ribuan pasangan Dao/DaoImpl di codebase
 * ini sejak 2007 (lihat komentar header IntelliJ di atas kelas), jadi risiko ini murni teoretis
 * untuk kode baru yang mengikuti konvensi yang sama.
 * </p>
 *
 * <h2>Sesi Hibernate: per-DAO-instance, bukan benar-benar thread-local di sini</h2>
 * <p>
 * {@link #getSession()} mengembalikan {@link #session} yang di-cache di instance bila ada dan
 * masih terbuka; bila tidak, ia MENGAMBIL sesi dari
 * {@link ais.database.hibernate.HibernateUtil#currentSession()} (yang thread-local) dan
 * meng-cache-nya di field instance. Karena instance {@code XxxDaoImpl} biasanya berumur pendek
 * (dibuat baru per pemakaian, bukan singleton Spring yang dipakai ulang lintas thread), ini dalam
 * praktiknya berperilaku setara thread-local — tetapi bila suatu {@code DaoImpl} DIPAKAI ULANG
 * lintas thread/request (mis. disimpan sebagai field instance kelas lain yang berumur panjang),
 * cache sesi di sini bisa memegang sesi basi dari thread/request sebelumnya. {@link #setSession}
 * memungkinkan pemanggil memaksa sesi tertentu (mis. sesi native non-thread-local dari kode
 * batch/posting) — pola ini dipakai luas di mesin-mesin posting akunting AIS.
 * </p>
 *
 * <h2>{@link #maxRowCount} vs {@code ais.common.Common#MAX_RESULT}</h2>
 * <p>
 * Ada DUA batas hasil yang tumpang tindih di hampir setiap method {@code find*}/{@code search*}:
 * {@link #maxRowCount} (field instance, default 500, TIDAK PERNAH diubah lewat setter publik
 * mana pun di kelas ini — nilainya efektif konstan) dipasang lewat {@code crit.setMaxResults(...)}
 * di AWAL pembangunan criteria, kemudian di akhir method yang sama {@code crit.setMaxResults(
 * ais.common.Common.MAX_RESULT)} dipanggil LAGI dan MENIMPA batas pertama sebelum {@code .list()}
 * dieksekusi — sehingga nilai yang benar-benar berlaku ke database adalah
 * {@code Common.MAX_RESULT}, bukan {@link #maxRowCount}. Panggilan pertama ke
 * {@code setMaxResults(maxRowCount)} pada praktiknya tidak berpengaruh apa pun (nilai dari
 * panggilan kedua yang menang). Dicatat di sini sebagai fakta perilaku kode saat ini, bukan
 * sesuatu yang diperbaiki lewat perubahan dokumentasi ini.
 * </p>
 */
public abstract class GenericHibernateDao<T, ID extends Serializable, DaoImpl extends GenericDao<T, ID>>
		implements GenericDao<T, ID> {

	/** Kelas entitas konkret milik DAO ini, ditentukan sekali via reflection generik di {@link #GenericHibernateDao()}. */
	private Class<T> persistentClass;
	/** Sesi Hibernate yang di-cache di instance ini; lihat catatan "Sesi Hibernate" pada Javadoc kelas soal risikonya bila instance dipakai ulang lintas thread. */
	private Session session;
	/** Lihat catatan "{@link #maxRowCount} vs Common#MAX_RESULT" pada Javadoc kelas -- nilai ini efektif tidak berpengaruh karena selalu ditimpa {@code Common.MAX_RESULT} sebelum eksekusi query. */
	private Integer maxRowCount = 500;

	/**
	 * Menentukan {@link #persistentClass} lewat reflection atas parameter tipe generik AKTUAL
	 * milik subclass langsung (lihat "Bagaimana tipe persisten ditentukan" pada Javadoc kelas).
	 * Wajib dipanggil implisit oleh constructor {@code XxxDaoImpl} tanpa argumen tambahan.
	 */
	@SuppressWarnings("unchecked")
	public GenericHibernateDao() {
		this.persistentClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
				.getActualTypeArguments()[0];
	}

	/**
	 * Memaksa DAO ini memakai {@link Session} tertentu alih-alih mengambil sesi thread-local
	 * dari {@link ais.database.hibernate.HibernateUtil#currentSession()} secara otomatis lewat
	 * {@link #getSession()}. Dipakai luas oleh kode batch/posting yang membuka sesi native sendiri
	 * (di luar siklus request-per-thread web biasa) dan perlu menjalankan operasi DAO generik di
	 * atas sesi tersebut. Mengembalikan {@code this} sebagai tipe {@code DaoImpl} (bukan
	 * {@code GenericHibernateDao} mentah) agar pemanggilan dapat dirangkai
	 * ({@code new AgamaDaoImpl().setSession(s).findAll()}).
	 *
	 * @param s sesi Hibernate yang akan dipakai untuk seluruh operasi DAO ini selanjutnya
	 * @return {@code this}, di-cast ke tipe implementasi konkret {@code DaoImpl}
	 */
	@SuppressWarnings("unchecked")
	public DaoImpl setSession(Session s) {
		session = s;
		return (DaoImpl) this;
	}

	/** {@inheritDoc} Membuat {@link Criteria} baru (tanpa restriction/order apa pun) untuk {@link #persistentClass}. */
	public Criteria getCriteria() {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		return crit;
	}

	/** {@inheritDoc} Delegasi langsung ke {@link #getSession()}. */
	public Session getCurrentSession() {
		return getSession();
	}

	/**
	 * Sesi Hibernate aktif untuk DAO ini: mengembalikan {@link #session} yang di-cache bila masih
	 * terbuka, atau mengambil sesi thread-local baru dari
	 * {@link ais.database.hibernate.HibernateUtil#currentSession()} bila belum ada/sudah tertutup
	 * (lalu di-cache untuk pemanggilan berikutnya). Lihat catatan "Sesi Hibernate" pada Javadoc
	 * kelas soal risiko cache basi bila instance DAO dipakai ulang lintas thread/request.
	 *
	 * @return sesi Hibernate yang siap dipakai; tidak pernah {@code null}
	 */
	protected Session getSession() {
		if (session == null || !session.isOpen() || !session.isOpen()) {
			session = HibernateUtil.currentSession();
		}
		return session;
	}

	/** @return kelas entitas konkret {@link #persistentClass} milik DAO ini. */
	protected Class<T> getPersistentClass() {
		return persistentClass;
	}

	/**
	 * Memuat entitas via {@link Session#load} (BUKAN {@link Session#get}) — mengembalikan proxy
	 * lazy yang melempar {@code ObjectNotFoundException} saat properti pertamanya diakses bila
	 * baris dengan {@code id} tersebut tidak ada, alih-alih mengembalikan {@code null}. Pemanggil
	 * yang ingin memeriksa "ada/tidak ada" tanpa exception sebaiknya memakai {@link #isExist}
	 * lebih dulu, bukan menangkap exception dari method ini.
	 *
	 * @param id   primary key entitas
	 * @param lock bila {@code true}, memuat dengan {@link LockMode#UPGRADE} (pessimistic lock,
	 *             {@code SELECT ... FOR UPDATE} di database yang mendukungnya) — dipakai saat
	 *             pemanggil akan langsung memodifikasi+menyimpan entitas dan perlu mencegah
	 *             race condition penulisan bersamaan; bila {@code false}, memuat tanpa lock
	 *             tambahan
	 * @return proxy/entitas bertipe {@code T}; TIDAK PERNAH {@code null} secara langsung (properti
	 *         diakses baru melempar exception bila baris tidak ada — perilaku bawaan Hibernate
	 *         {@code load})
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public T findById(ID id, Boolean lock) {
		T entity;
		if (lock) {
			entity = (T) getSession().load(getPersistentClass(), id, LockMode.UPGRADE);
		} else {
			entity = (T) getSession().load(getPersistentClass(), id);
		}

		return entity;
	}

	public Boolean isExist(Serializable id) {
		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(getPersistentClass());
		String s = classMetadata.getIdentifierPropertyName();
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(1);
		crit.add(Restrictions.eq(s, id));
		@SuppressWarnings("rawtypes")
		List list = crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
		return list.size() != 0;
	}

	public Boolean isExist(Serializable id, String propertyName) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(1);
		crit.add(Restrictions.eq(propertyName, id));
		@SuppressWarnings("rawtypes")
		List list = crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
		return list.size() != 0;
	}

	public void persis(T obj) {
		getSession().persist(obj);
	}

	public List<T> findAll() {
		return findByCriteria();
	}

	@SuppressWarnings("unchecked")
	public List<T> findAll(Order order) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		crit.addOrder(order);
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> findAll(Order... order) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		for (Order odr : order) {
			crit.addOrder(odr);
		}
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	public List<T> findPageByPage(int firstResult, int maxResults) {
		return findByCriteriaPageByPage(firstResult, maxResults);
	}

	@SuppressWarnings("unchecked")
	public List<T> findByExample(T exampleInstance, String... excludeProperty) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		Example example = Example.create(exampleInstance);
		example = example.enableLike(MatchMode.ANYWHERE);
		for (String exclude : excludeProperty) {
			example.excludeProperty(exclude);
		}
		crit.add(example);
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	protected Criteria createCriteria() {
		return getSession().createCriteria(getPersistentClass());
	}

	@SuppressWarnings("unchecked")
	public List<T> search(Hashtable<String, ? extends Object> like) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		Enumeration<String> enu = like.keys();
		while (enu.hasMoreElements()) {
			String col = enu.nextElement();
			crit.add(Restrictions.like(col, String.valueOf(like.get(col)), MatchMode.ANYWHERE));
		}
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> search(String search) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(persistentClass);
		String[] property = classMetadata.getPropertyNames();

		Type[] types = classMetadata.getPropertyTypes();
		List<Criterion> criterions = new ArrayList<Criterion>();
		String indentifier = classMetadata.getIdentifierPropertyName();
		String indentifierType = classMetadata.getIdentifierType().getName();
		if (indentifierType.equalsIgnoreCase("String")) {
			criterions.add(Restrictions.ilike(indentifier, search, MatchMode.ANYWHERE));
		}
		int i = 0;
		for (String aProperty : property) {
			Type type = types[i];
			if (type.getName().equalsIgnoreCase("String")) {
				criterions.add(Restrictions.ilike(aProperty, search, MatchMode.ANYWHERE));
			}
			i++;
		}

		if (criterions.size() == 0) {
			return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
		}

		Criterion acriterion = criterions.get(0);
		for (Criterion criterion : criterions) {
			acriterion = Restrictions.or(acriterion, criterion);
		}
		crit.add(acriterion);

		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> search(String search, Order[] order, Criterion... criterionses) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(persistentClass);
		String[] property = classMetadata.getPropertyNames();

		Type[] types = classMetadata.getPropertyTypes();
		List<Criterion> criterions = new ArrayList<Criterion>();
		String indentifier = classMetadata.getIdentifierPropertyName();
		String indentifierType = classMetadata.getIdentifierType().getName();
		if (indentifierType.equalsIgnoreCase("String")) {
			criterions.add(Restrictions.ilike(indentifier, search, MatchMode.ANYWHERE));
		}
		int i = 0;
		for (String aProperty : property) {
			Type type = types[i];
			if (type.getName().equalsIgnoreCase("String")) {
				criterions.add(Restrictions.ilike(aProperty, search, MatchMode.ANYWHERE));
			}
			i++;
		}

		if (criterions.size() != 0) {
			Criterion acriterion = criterions.get(0);
			for (Criterion criterion : criterions) {
				acriterion = Restrictions.or(acriterion, criterion);
			}
			crit.add(acriterion);
		}
		for (Criterion criterion : criterionses) {
			if (criterion != null) {
				crit.add(criterion);
			}
		}
		for (Order o : order) {
			crit.addOrder(o);
		}
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> search(String search, Criterion... criterionses) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(persistentClass);
		String[] property = classMetadata.getPropertyNames();

		Type[] types = classMetadata.getPropertyTypes();
		List<Criterion> criterions = new ArrayList<Criterion>();
		String indentifier = classMetadata.getIdentifierPropertyName();
		String indentifierType = classMetadata.getIdentifierType().getName();
		if (indentifierType.equalsIgnoreCase("String")) {
			criterions.add(Restrictions.ilike(indentifier, search, MatchMode.ANYWHERE));
		}
		int i = 0;
		for (String aProperty : property) {
			Type type = types[i];
			if (type.getName().equalsIgnoreCase("String")) {
				criterions.add(Restrictions.ilike(aProperty, search, MatchMode.ANYWHERE));
			}
			i++;
		}

		if (criterions.size() != 0) {
			Criterion acriterion = criterions.get(0);
			for (Criterion criterion : criterions) {
				acriterion = Restrictions.or(acriterion, criterion);
			}
			crit.add(acriterion);
		}
		for (Criterion criterion : criterionses) {
			if (criterion != null) {
				crit.add(criterion);
			}
		}
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> search(String search, Order... order) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(persistentClass);
		String[] property = classMetadata.getPropertyNames();

		Type[] types = classMetadata.getPropertyTypes();
		List<Criterion> criterions = new ArrayList<Criterion>();
		String indentifier = classMetadata.getIdentifierPropertyName();
		String indentifierType = classMetadata.getIdentifierType().getName();
		if (indentifierType.equalsIgnoreCase("String")) {
			criterions.add(Restrictions.ilike(indentifier, search, MatchMode.ANYWHERE));
		}
		int i = 0;
		for (String aProperty : property) {
			Type type = types[i];
			if (type.getName().equalsIgnoreCase("String")) {
				criterions.add(Restrictions.ilike(aProperty, search, MatchMode.ANYWHERE));
			}
			i++;
		}

		if (criterions.size() != 0) {
			Criterion acriterion = criterions.get(0);
			for (Criterion criterion : criterions) {
				acriterion = Restrictions.or(acriterion, criterion);
			}
			crit.add(acriterion);
		}

		for (Order o : order) {
			crit.addOrder(o);
		}

		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> search(Hashtable<String, ? extends Object> like, Order... orders) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		for (Order order : orders) {
			crit.addOrder(order);
		}
		Enumeration<String> enu = like.keys();
		while (enu.hasMoreElements()) {
			String col = enu.nextElement();
			crit.add(Restrictions.like(col, String.valueOf(like.get(col)), MatchMode.ANYWHERE));
		}
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> search(List<? extends String> like, Order... orders) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		for (Order order : orders) {
			crit.addOrder(order);
		}
		for (String s : like) {
			crit.add(Restrictions.sqlRestriction(s));
		}
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	public T makePersistent(T entity) {
		getSession().saveOrUpdate(entity);
		return entity;
	}

	public void makeTransient(T entity) {
		getSession().delete(entity);
	}

	public void refresh(T entity) {
		getSession().refresh(entity);
	}

	public void refresh(T entity, LockMode lockMode) {
		getSession().refresh(entity, lockMode);
	}

	public void save(T entity) {
		getSession().save(entity);
	}

	@SuppressWarnings("unchecked")
	public T load(Serializable id) {
		return (T) getSession().load(getPersistentClass(), id);
	}

	/**
	 * Memperbarui entitas lewat {@code Common.refreshUpdate} — BUKAN {@code Session.update}
	 * Hibernate polos. {@code entity} di-cast paksa ke {@link ais.database.model.GeneralValueObject},
	 * sehingga method ini HANYA berfungsi untuk entitas yang mengimplementasikan tipe itu (di
	 * codebase ini praktis semua entitas Hibernate AIS melakukannya). Ini overload yang dipakai
	 * mayoritas kode aplikasi; bandingkan dengan {@link #update(String, Object)} di bawah yang
	 * memakai {@code Session.update} langsung dengan nama entitas eksplisit.
	 *
	 * @param entity entitas yang akan diperbarui, harus berupa {@link ais.database.model.GeneralValueObject}
	 */
	public void update(T entity) {
		Common.refreshUpdate(getSession(), (GeneralValueObject) entity);
	}

	/**
	 * Memperbarui entitas lewat {@link Session#update(String, Object)} Hibernate langsung, dengan
	 * nama entitas Hibernate diberikan eksplisit ({@code identityName}) alih-alih diturunkan dari
	 * {@link #persistentClass} — dipakai saat nama mapping entitas berbeda dari nama kelas Java
	 * (mis. entitas dengan beberapa mapping/subclass), atau saat pemanggil sudah tahu persis nama
	 * entitas tanpa perlu logika tambahan {@link #update(Object) update(T)} di atas.
	 *
	 * @param identityName nama entitas Hibernate (sesuai mapping, boleh berbeda dari {@code T.getSimpleName()})
	 * @param entity       entitas yang akan diperbarui
	 */
	public void update(String identityName, T entity) {
		getSession().update(identityName, entity);
	}

	/**
	 * Menyimpan entitas baru atau memperbarui yang sudah ada, ditentukan lewat query eksistensi
	 * eksplisit ({@link #isExist(Serializable)}) — BUKAN {@link Session#saveOrUpdate} Hibernate
	 * bawaan (yang menentukan insert/update dari nilai identifier entitas, tanpa query tambahan).
	 * Bila baris dengan {@code id} sudah ada, entitas di-{@link #merge} lebih dulu (menyatukan
	 * state dengan entitas ter-attach di sesi) baru di-{@code update}; bila belum ada, langsung
	 * {@code save}. Pola ini lebih mahal (satu query SELECT tambahan) dibanding
	 * {@code saveOrUpdate} bawaan Hibernate, tapi menghindari beberapa kasus tepi
	 * {@code NonUniqueObjectException} yang bisa muncul dari heuristik identifier bawaan.
	 *
	 * @param entity entitas yang akan disimpan/diperbarui
	 * @param id     primary key yang dipakai untuk cek eksistensi lewat {@link #isExist}
	 */
	public void saveOrUpdate(T entity, Serializable id) {
		if (isExist(id)) {
			getSession().update(merge(entity));
		} else {
			getSession().save(entity);
		}
	}

	public void saveOrUpdate(String identityName, T entity) {
		getSession().saveOrUpdate(identityName, entity);
	}

	public void delete(T entity) {
		getSession().delete((entity));
	}

	public void flush() {
		getSession().flush();
	}

	public void evict(T entity) {
		getSession().evict((entity));
	}

	@SuppressWarnings("unchecked")
	public T merge(T entity) {
		return (T) getSession().merge(entity);
	}

	@SuppressWarnings("unchecked")
	public List<T> findByCriteria(Criterion... criterion) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		for (Criterion c : criterion) {
			crit.add(c);
		}
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> findByCriteria() {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> findByCriteria(Order order) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		crit.addOrder(order);
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> findByCriteria(Order order, Criterion... criterion) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.setMaxResults(maxRowCount);
		crit.addOrder(order);
		for (Criterion c : criterion) {
			crit.add(c);
		}
		return crit.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> findByCriteriaPageByPage(int firstResult, int maxResults, Criterion... criterion) {
		Criteria criteria = getSession().createCriteria(getPersistentClass());
		criteria.setMaxResults(maxRowCount);
		for (Criterion c : criterion) {
			criteria.add(c);
		}
		criteria.setFirstResult(firstResult);
		criteria.setMaxResults(maxResults);
		return criteria.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> findByCriteriaPageByPage(int firstResult, int maxResults) {
		Criteria criteria = getSession().createCriteria(getPersistentClass());
		criteria.setMaxResults(maxRowCount);
		criteria.setFirstResult(firstResult);
		criteria.setMaxResults(maxResults);
		return criteria.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

}