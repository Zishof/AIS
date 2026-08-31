package ais.database.dao;

import ais.database.model.BiodataPegawai;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.BiodataPegawai} (biodata pegawai). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface BiodataPegawaiDao extends GenericDao<BiodataPegawai, Long>{

}
