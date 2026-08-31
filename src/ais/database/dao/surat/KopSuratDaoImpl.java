package ais.database.dao.surat;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.surat.KopSurat;

/**
 * Implementasi Hibernate {@link KopSuratDao} untuk entitas {@link ais.database.model.surat.KopSurat}.
 * Kelas ini sengaja kosong: seluruh logika CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public class KopSuratDaoImpl extends
		GenericHibernateDao<KopSurat, Long, KopSuratDao> implements
		KopSuratDao {

}
