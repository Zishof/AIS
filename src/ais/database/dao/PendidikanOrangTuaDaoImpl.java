package ais.database.dao;

import ais.database.model.PendidikanOrangTua;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.PendidikanOrangTua} (data
 * pendidikan orang tua mahasiswa). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PendidikanOrangTuaDaoImpl extends GenericHibernateDao<PendidikanOrangTua, Long, PendidikanOrangTuaDao> implements PendidikanOrangTuaDao{

}
