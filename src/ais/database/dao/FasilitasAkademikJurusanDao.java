package ais.database.dao;

import ais.database.model.epsbed.FasilitasAkademikJurusan;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.epsbed.FasilitasAkademikJurusan} (data
 * fasilitas akademik milik suatu jurusan, untuk pelaporan EPSBED/PDDikti). Pasangan Dao/DaoImpl
 * ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa method tambahan --
 * lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface FasilitasAkademikJurusanDao extends
		GenericDao<FasilitasAkademikJurusan, Long> {

}
