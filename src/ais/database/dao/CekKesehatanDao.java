package ais.database.dao;

import ais.database.model.CekKesehatan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.CekKesehatan} (data hasil cek kesehatan,
 * mis. untuk mahasiswa baru). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface CekKesehatanDao extends GenericDao<CekKesehatan, Long>{

}
