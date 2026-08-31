package ais.database.dao;

import ais.database.model.PendapatanOrangTua;

/**
 * Implementasi DAO untuk entitas {@link ais.database.model.PendapatanOrangTua} (data
 * pendapatan orang tua mahasiswa). Kelas ini murni mewarisi perilaku generik dari
 * {@link ais.database.dao.GenericHibernateDao} tanpa method tambahan -- lihat javadoc di sana
 * untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public class PendapatanOrangTuaDaoImpl extends GenericHibernateDao<PendapatanOrangTua, Long, PendapatanOrangTuaDao> implements PendapatanOrangTuaDao{

}
