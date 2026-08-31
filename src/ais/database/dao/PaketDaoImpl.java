package ais.database.dao;

import ais.database.model.Paket;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.Paket} (data paket
 * perkuliahan/registrasi). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PaketDaoImpl extends GenericHibernateDao<Paket, Long, PaketDao> implements PaketDao{

}
