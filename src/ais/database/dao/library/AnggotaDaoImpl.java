package ais.database.dao.library;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.library.Anggota;

/**
 * Implementasi Hibernate untuk entitas {@link ais.database.model.library.Anggota} pada modul
 * perpustakaan — anggota perpustakaan (peminjam terdaftar). Kelas ini sengaja kosong: seluruh
 * perilaku CRUD generik diwarisi langsung dari {@link ais.database.dao.GenericHibernateDao}, lihat
 * javadoc di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericHibernateDao
 */
public class AnggotaDaoImpl extends GenericHibernateDao<Anggota, Long, AnggotaDao> implements AnggotaDao{

}
