package ais.database.dao;

import ais.database.model.PekerjaanOrangTua;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.PekerjaanOrangTua} (data pekerjaan
 * orang tua mahasiswa). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PekerjaanOrangTuaDaoImpl extends GenericHibernateDao<PekerjaanOrangTua, Long, PekerjaanOrangTuaDao> implements PekerjaanOrangTuaDao{

}
