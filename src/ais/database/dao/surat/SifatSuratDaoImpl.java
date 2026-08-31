package ais.database.dao.surat;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.surat.SifatSurat;

/**
 * Implementasi Hibernate {@link SifatSuratDao} untuk entitas {@link ais.database.model.surat.SifatSurat}.
 * Kelas ini sengaja kosong: seluruh logika CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public class SifatSuratDaoImpl extends
		GenericHibernateDao<SifatSurat, Long, SifatSuratDao> implements
		SifatSuratDao {

}
