package ais.action.master.resources;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;

import com.sun.jersey.api.NotFoundException;

/**
 * Kelas dasar generik untuk seluruh endpoint REST (JAX-RS/Jersey) baca-saja keluarga
 * {@code *Resource} di paket ini: menyediakan pengambilan satu baris berdasarkan id serta
 * pencarian daftar (tanpa filter, atau dengan 1-2 kata kunci yang dicocokkan terhadap SEMUA
 * properti bertipe {@link String} pada entitas {@code T} lewat refleksi metadata Hibernate).
 * Subclass hanya perlu memetakan path REST dan meneruskan parameter ke method di sini.
 *
 * <p>
 * <b>Catatan keamanan (berlaku untuk seluruh subclass)</b> — setiap method publik di sini
 * mewajibkan {@code username}/{@code password} sebagai parameter biasa yang di-passing langsung
 * oleh subclass dari segmen <i>path URL</i> (lewat {@link Common#checkLogin(String, String)}),
 * bukan lewat header Authorization/body. Ini menyebabkan kredensial pengguna tersimpan dalam
 * bentuk teks-jelas pada log akses server dan kemungkinan cache/log perantara. Selain itu,
 * pencarian kata kunci ({@code getAllData(..., search[, search1])}) membangun kriteria pencarian
 * dari SELURUH properti String entitas secara otomatis lewat refleksi metadata — subclass tidak
 * dapat membatasi kolom mana yang boleh dicari, sehingga kolom sensitif bertipe String pada
 * entitas manapun yang memakai {@code DataResource} berpotensi ikut ter-expose lewat pencarian
 * teks. Ini adalah keputusan desain lama, bukan sesuatu yang diperbaiki di sini sesuai batasan
 * tugas dokumentasi ini.
 * </p>
 *
 * @param <T> tipe entitas yang dilayani resource ini
 */
public abstract class DataResource<T> {

	@SuppressWarnings("rawtypes")
	private Class persistentClass;

	/** @param class1 kelas entitas Hibernate yang dilayani resource ini */
	@SuppressWarnings("rawtypes")
	public DataResource(Class class1) {
		this.persistentClass = class1;
	}

	/** @return waktu sistem server saat ini (epoch milidetik), dipakai klien untuk sinkronisasi jam. */
	public long getSystemTime() {
		return System.currentTimeMillis();
	}

	/**
	 * Mengambil satu baris entitas berdasarkan id, setelah memverifikasi kredensial (lihat
	 * catatan keamanan pada javadoc kelas).
	 *
	 * @param username kredensial user
	 * @param password kredensial user
	 * @param id       id baris yang dicari (harus berupa angka)
	 * @return entitas yang ditemukan
	 * @throws NotFoundException bila login gagal, data tidak ditemukan, atau terjadi kesalahan internal
	 */
	@SuppressWarnings("unchecked")
	public T getData(String username, String password, String id) {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		try {
			Session session = HibernateUtil.currentNativeSession();
			T generalValueObject = (T) session.createCriteria(persistentClass)
					.add(Restrictions.idEq(Long.parseLong(id.trim())))
					.uniqueResult();
			
			HibernateUtil.closeSession();
			return generalValueObject;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	/**
	 * @param username kredensial user (lihat catatan keamanan di javadoc kelas)
	 * @param password kredensial user (lihat catatan keamanan di javadoc kelas)
	 * @return hingga {@link Common#MAX_RESULT} baris entitas terbaru (diurutkan menurun berdasarkan id)
	 * @throws NotFoundException bila login gagal atau terjadi kesalahan internal
	 */
	@SuppressWarnings({ "unchecked" })
	public List<T> getAllData(String username, String password) {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		try {
			Session session = HibernateUtil.currentNativeSession();
			List<T> generalValueObjects = (List<T>) session
					.createCriteria(persistentClass).addOrder(Order.desc("id"))
					.setMaxResults(Common.MAX_RESULT).list();
			
			HibernateUtil.closeSession();
			return generalValueObjects;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	/**
	 * Mencari entitas yang mengandung {@code search} pada SALAH SATU properti bertipe
	 * {@link String} (ditentukan otomatis lewat metadata Hibernate, bukan daftar kolom eksplisit).
	 *
	 * @param username kredensial user (lihat catatan keamanan di javadoc kelas)
	 * @param password kredensial user (lihat catatan keamanan di javadoc kelas)
	 * @param search   kata kunci pencarian
	 * @return hingga {@link Common#MAX_RESULT} baris yang cocok, diurutkan menurun berdasarkan id
	 * @throws NotFoundException bila login gagal atau terjadi kesalahan internal
	 */
	@SuppressWarnings({ "unchecked" })
	public List<T> getAllData(String username, String password, String search) {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		try {

			ClassMetadata classMetadata = HibernateUtil
					.getClassMetadata(persistentClass);
			String[] strings = classMetadata.getPropertyNames();
			Type[] types = classMetadata.getPropertyTypes();

			Criterion criterion = Restrictions.sqlRestriction("1!=1");
			int i = 0;
			for (String s : strings) {
				Type type = types[i++];

				if (type.getReturnedClass().getName()
						.equals(String.class.getName())) {
					criterion = Restrictions.or(criterion, Restrictions.ilike(
							s, search.trim(), MatchMode.ANYWHERE));
				}

			}

			Session session = HibernateUtil.currentNativeSession();
			List<T> generalValueObjects = (List<T>) session
					.createCriteria(persistentClass).add(criterion)
					.addOrder(Order.desc("id"))
					.setMaxResults(Common.MAX_RESULT).list();
			
			HibernateUtil.closeSession();
			return generalValueObjects;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	/**
	 * Seperti {@link #getAllData(String, String, String)}, dengan kriteria kedua
	 * ({@code search1}) yang harus SAMA-SAMA cocok (kombinasi AND dari dua kondisi OR-per-kolom).
	 *
	 * @param username kredensial user (lihat catatan keamanan di javadoc kelas)
	 * @param password kredensial user (lihat catatan keamanan di javadoc kelas)
	 * @param search   kata kunci pencarian pertama
	 * @param search1  kata kunci pencarian kedua
	 * @return hingga {@link Common#MAX_RESULT} baris yang cocok dengan kedua kata kunci, diurutkan menurun berdasarkan id
	 * @throws NotFoundException bila login gagal atau terjadi kesalahan internal
	 */
	@SuppressWarnings({ "unchecked" })
	public List<T> getAllData(String username, String password, String search,
			String search1) {

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		try {

			ClassMetadata classMetadata = HibernateUtil
					.getClassMetadata(persistentClass);
			String[] strings = classMetadata.getPropertyNames();
			Type[] types = classMetadata.getPropertyTypes();

			Criterion criterion = Restrictions.sqlRestriction("1!=1");
			Criterion criterion1 = Restrictions.sqlRestriction("1!=1");
			int i = 0;
			for (String s : strings) {
				Type type = types[i++];

				if (type.getReturnedClass().getName()
						.equals(String.class.getName())) {
					criterion = Restrictions.or(criterion, Restrictions.ilike(
							s, search.trim(), MatchMode.ANYWHERE));
					criterion1 = Restrictions.or(criterion1, Restrictions
							.ilike(s, search1.trim(), MatchMode.ANYWHERE));
				}

			}

			Session session = HibernateUtil.currentNativeSession();
			List<T> generalValueObjects = (List<T>) session
					.createCriteria(persistentClass).add(criterion)
					.add(criterion1).addOrder(Order.desc("id"))
					.setMaxResults(Common.MAX_RESULT).list();
			
			HibernateUtil.closeSession();
			return generalValueObjects;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}
}
