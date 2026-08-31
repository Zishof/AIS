package ais.database.dao.surat;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.surat.PenyampaianSurat;

/**
 * Implementasi Hibernate {@link PenyampaianSuratDao} untuk entitas
 * {@link ais.database.model.surat.PenyampaianSurat}. Kelas ini sengaja kosong: seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana
 * untuk detail perilaku method.
 */
public class PenyampaianSuratDaoImpl extends
		GenericHibernateDao<PenyampaianSurat, Long, PenyampaianSuratDao>
		implements PenyampaianSuratDao {

}
