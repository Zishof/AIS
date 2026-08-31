package ais.database.dao;

import ais.database.model.Jabatan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Jabatan} (data jabatan). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface JabatanDao extends GenericDao<Jabatan, Long>{

}
