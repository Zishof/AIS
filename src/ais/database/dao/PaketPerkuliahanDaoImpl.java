package ais.database.dao;

import ais.database.model.PaketPerkuliahan;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.PaketPerkuliahan} (data paket
 * perkuliahan). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PaketPerkuliahanDaoImpl extends
		GenericHibernateDao<PaketPerkuliahan, Long, PaketPerkuliahanDao>
		implements PaketPerkuliahanDao {

}
