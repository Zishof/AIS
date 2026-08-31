package ais.database.dao.surat;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.surat.LokerSurat;

/**
 * Implementasi Hibernate {@link LokerSuratDao} untuk entitas {@link ais.database.model.surat.LokerSurat}.
 * Kelas ini sengaja kosong: seluruh logika CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public class LokerSuratDaoImpl extends
		GenericHibernateDao<LokerSurat, Long, LokerSuratDao> implements
		LokerSuratDao {

}
