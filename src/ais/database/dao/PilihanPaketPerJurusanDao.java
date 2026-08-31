package ais.database.dao;

import ais.database.model.PilihanPaketPerJurusanMhsBaru;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.PilihanPaketPerJurusanMhsBaru} (data
 * pilihan paket per jurusan untuk mahasiswa baru). Pasangan Dao/DaoImpl ini murni memakai
 * perilaku generik {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc
 * di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface PilihanPaketPerJurusanDao extends GenericDao<PilihanPaketPerJurusanMhsBaru, Long>{

}
