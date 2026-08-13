package ais.action.master.generic.v2.test;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.database.hibernate.HibernateUtil;

/** Verifikasi mapping dan tabel aktual; kredensial diberikan lewat -D. */
public final class PegawaiHistoryGenericCrudDatabaseSelfTest {
    private static final String[][] ROUTES = new String[][] {
        { "ais.database.model.employ.RiwayatTandaJasaPegawai", "riwayat_tanda_jasa_pegawai" },
        { "ais.database.model.employ.RiwayatPendidikanPegawai", "riwayat_pendidikan_pegawai" },
        { "ais.database.model.employ.RiwayatPelatihanPegawai", "riwayat_pelatihan_pegawai" },
        { "ais.database.model.employ.RiwayatOrganisasiSekolahPegawai", "riwayat_organisasi_sekolah_pegawai" },
        { "ais.database.model.employ.RiwayatOrganisasiKampusPegawai", "riwayat_organisasi_kampus_pegawai" },
        { "ais.database.model.employ.RiwayatOrganisasiLainPegawai", "riwayat_organisasi_lain_pegawai" },
        { "ais.database.model.employ.RiwayatKeteranganLainPegawai", "riwayat_keterangan_lain_pegawai" },
        { "ais.database.model.employ.RiwayatKerjaPegawai", "riwayat_kerja_pegawai" },
        { "ais.database.model.employ.RiwayatKeluarNegeriPegawai", "riwayat_keluar_negeri_pegawai" },
        { "ais.database.model.employ.RiwayatKartuIdentitasPegawai", "riwayat_kartu_identitas_pegawai" }
    };

    private PegawaiHistoryGenericCrudDatabaseSelfTest() { }

    public static void main(String[] args) throws Exception {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            for (int i = 0; i < ROUTES.length; i++) {
                GenericCrudDefinition definition = GenericCrudDefinitionRegistry.resolve(
                        ROUTES[i][0], "employ", ROUTES[i][1]);
                Criteria count = session.createCriteria(definition.getEntityClass()).setProjection(Projections.rowCount());
                Number rows = (Number) count.uniqueResult();
                if (definition.getFields().size() < 4) throw new IllegalStateException("Field metadata belum lengkap: " + ROUTES[i][1]);
                System.out.println("PASS " + ROUTES[i][1] + " fields=" + definition.getFields().size()
                        + " rows=" + (rows == null ? 0 : rows.longValue()));
            }
        } finally {
            try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { }
            try { HibernateUtil.getSessionFactory().close(); } catch (Exception ignored) { }
        }
        System.exit(0);
    }
}
