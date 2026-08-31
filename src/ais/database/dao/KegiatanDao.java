package ais.database.dao;

import ais.database.model.Kegiatan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Kegiatan} (data kegiatan). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface KegiatanDao extends GenericDao<Kegiatan, Long>{

}
