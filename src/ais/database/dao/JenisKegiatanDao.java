package ais.database.dao;

import ais.database.model.JenisKegiatan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.JenisKegiatan} (jenis kegiatan). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface JenisKegiatanDao extends GenericDao<JenisKegiatan, Long> {

}
