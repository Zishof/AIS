package ais.database.dao.asset;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.asset.Lokasi;

/**
 * Implementasi Hibernate {@link LokasiDao} untuk entitas {@link ais.database.model.asset.Lokasi}.
 * Kelas ini sengaja kosong: seluruh logika CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public class LokasiDaoImpl extends GenericHibernateDao<Lokasi, Long, LokasiDao>
		implements LokasiDao {

}
