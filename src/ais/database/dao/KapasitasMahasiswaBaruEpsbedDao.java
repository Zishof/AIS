package ais.database.dao;

import ais.database.model.epsbed.KapasitasMahasiswaBaru;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.epsbed.KapasitasMahasiswaBaru} (data
 * kapasitas mahasiswa baru untuk pelaporan EPSBED/PDDikti) -- perhatikan nama berkas DAO ini
 * ({@code KapasitasMahasiswaBaruEpsbedDao}) tidak persis sama dengan nama kelas entitasnya.
 * Pasangan Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface KapasitasMahasiswaBaruEpsbedDao extends
		GenericDao<KapasitasMahasiswaBaru, Long> {

}
