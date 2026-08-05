package ais.action.master.payroll.helper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.model.Pegawai;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.rab.Pejabat;

/**
 * Helper reusable untuk menentukan ID bawahan (pegawai &amp; dosen) dari pengguna
 * yang sedang login, lalu menerapkan filter Hibernate Criteria sesuai hak akses.
 *
 * <p><b>Tujuan:</b> Mengekstrak logika {@code CutiDanIzinAction.siapkanDataBawahan()}
 * ke class terpisah sehingga dapat dipakai ulang oleh modul lain (dashboard penggajian,
 * laporan cuti, rekap kehadiran, dsb.) tanpa duplikasi kode.</p>
 *
 * <p><b>Urutan deteksi bawahan (sama dengan CutiDanIzinAction):</b>
 * <ol>
 *   <li>Pegawai yang menempatkan tbmuser.getPegawai() sebagai
 *       {@code atasanlangsung/atasanlangsung2/atasanlangsung3}.</li>
 *   <li>Jika tbmuser juga Dosen/Guru, cek alias atasanlangsung* ke entitas dosen/guru tersebut.</li>
 *   <li>Bawahan via jabatan struktural ({@code JenisJabatan} di {@code hakAkses} atau via
 *       {@code Pejabat} untuk role tanpa jabatan).</li>
 *   <li>Jika {@code getMelihatDataPegawaiLain()==false}, tambahkan diri sendiri agar user
 *       minimal melihat datanya sendiri.</li>
 * </ol>
 * </p>
 *
 * <p><b>Cara pakai:</b>
 * <pre>
 *   BawahanSet bs = PegawaiBawahanHelper.ambilBawahan(session, Common.getCurrentUser());
 *   Criteria c = session.createCriteria(CutiDanIzin.class)
 *                       .createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN);
 *   PegawaiBawahanHelper.applyFilterCuti(c, bs, Common.getCurrentUser());
 * </pre>
 * </p>
 *
 * <p>Kompatibel Java 1.7 dan Hibernate 3 Criteria API.</p>
 */
public final class PegawaiBawahanHelper {

    private PegawaiBawahanHelper() {}

    // ────────────────────────────────────────────────────────────────────────
    // Value object
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Hasil pengumpulan ID bawahan dari satu pengguna.
     *
     * <ul>
     *   <li>{@code admin=true} → user adalah admin yang boleh lihat semua data;
     *       set ID-nya sengaja dibiarkan kosong karena filter tidak diperlukan.</li>
     *   <li>{@code pegawaiIds} → kumpulan {@code Pegawai.id} yang merupakan bawahan.</li>
     *   <li>{@code dosenIds}   → kumpulan {@code Dosen.id} bawahan (via Pegawai.dosen).</li>
     * </ul>
     */
    public static final class BawahanSet {
        /** true jika user adalah admin yang boleh melihat semua cuti/izin. */
        public final boolean admin;
        /** ID Pegawai yang merupakan bawahan (termasuk diri sendiri jika perlu). */
        public final Set<Long> pegawaiIds;
        /** ID Dosen yang merupakan bawahan. */
        public final Set<Long> dosenIds;

        BawahanSet(boolean admin, Set<Long> pegawaiIds, Set<Long> dosenIds) {
            this.admin = admin;
            this.pegawaiIds = pegawaiIds;
            this.dosenIds = dosenIds;
        }

        /** true jika ada setidaknya satu bawahan (pegawai maupun dosen). */
        public boolean hasBawahan() {
            return !pegawaiIds.isEmpty() || !dosenIds.isEmpty();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Pengumpulan bawahan
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Kumpulkan semua ID bawahan (pegawai + dosen) dari {@code tbmuser}.
     *
     * <p>Logika sama persis dengan {@code CutiDanIzinAction.siapkanDataBawahan()}.
     * Jika {@code tbmuser} adalah admin ({@code Common.getApakahAdminBolehLihatSemuaCuti()})
     * maka set ID dikembalikan kosong dan {@code admin=true} — pemanggil cukup cek flag ini
     * untuk melewati filter.</p>
     *
     * @param session Hibernate session aktif (currentSession atau openSession)
     * @param tbmuser pengguna saat ini; boleh null (hasilnya non-admin + empty)
     * @return {@link BawahanSet} berisi flag admin dan set ID bawahan
     */
    @SuppressWarnings("unchecked")
    public static BawahanSet ambilBawahan(Session session, Tbmuser tbmuser) {
        boolean admin = Common.getApakahAdminBolehLihatSemuaCuti();
        Set<Long> pegawaiIds = new HashSet<Long>();
        Set<Long> dosenIds   = new HashSet<Long>();

        if (admin || tbmuser == null) {
            return new BawahanSet(admin, pegawaiIds, dosenIds);
        }

        try {
            Tbmrole hakAkses = tbmuser.hakAkses();

            // ── 1. Bawahan via atasanlangsung (1/2/3) ──────────────────────
            if (tbmuser.getPegawai() != null) {
                org.hibernate.criterion.Criterion criterion = Restrictions.or(
                    Restrictions.eq("atasanlangsung", tbmuser.getPegawai()),
                    Restrictions.or(
                        Restrictions.eq("atasanlangsung3", tbmuser.getPegawai()),
                        Restrictions.eq("atasanlangsung2", tbmuser.getPegawai())));

                Criteria cBwh = session.createCriteria(Pegawai.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

                if (tbmuser.getDosen() != null) {
                    // Juga cari pegawai yang atasannya adalah Dosen yang sama
                    cBwh.createAlias("atasanlangsung",  "al",  Criteria.LEFT_JOIN)
                        .createAlias("atasanlangsung2", "al2", Criteria.LEFT_JOIN)
                        .createAlias("atasanlangsung3", "al3", Criteria.LEFT_JOIN);
                    criterion = Restrictions.or(criterion, Restrictions.or(
                        Restrictions.eq("al.dosen",  tbmuser.getDosen()),
                        Restrictions.or(
                            Restrictions.eq("al3.dosen", tbmuser.getDosen()),
                            Restrictions.eq("al2.dosen", tbmuser.getDosen()))));
                } else if (tbmuser.getGuru() != null) {
                    cBwh.createAlias("atasanlangsung",  "al",  Criteria.LEFT_JOIN)
                        .createAlias("atasanlangsung2", "al2", Criteria.LEFT_JOIN)
                        .createAlias("atasanlangsung3", "al3", Criteria.LEFT_JOIN);
                    criterion = Restrictions.or(criterion, Restrictions.or(
                        Restrictions.eq("al.guru",  tbmuser.getGuru()),
                        Restrictions.or(
                            Restrictions.eq("al3.guru", tbmuser.getGuru()),
                            Restrictions.eq("al2.guru", tbmuser.getGuru()))));
                }

                List<Long> bwhn = cBwh.add(criterion)
                    .setProjection(Projections.property("id")).list();
                if (bwhn != null) pegawaiIds.addAll(bwhn);

                // ── 2. Bawahan via jabatan struktural ─────────────────────
                if (hakAkses != null && hakAkses.getJenisJabatan() != null) {
                    // User punya JenisJabatan langsung di hakAkses
                    List<Long> bjab = session.createCriteria(Pegawai.class)
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                        .add(Restrictions.or(
                            Restrictions.eq("atasan.id", hakAkses.getJenisJabatan().getId()),
                            Restrictions.or(
                                Restrictions.eq("atasanPendukung.id", hakAkses.getJenisJabatan().getId()),
                                Restrictions.eq("atasanPendukungCadangan.id", hakAkses.getJenisJabatan().getId()))))
                        .setProjection(Projections.property("id")).list();
                    if (bjab != null) pegawaiIds.addAll(bjab);
                } else {
                    // Tidak ada JenisJabatan di hakAkses — cek lewat tabel Pejabat
                    List<Tbmrole> roles = tbmuser.ambilRoles();
                    boolean adaJabatan = false;
                    for (Tbmrole r : roles) {
                        if (r != null && r.getJenisJabatan() != null) { adaJabatan = true; break; }
                    }
                    if (!adaJabatan && hakAkses != null) {
                        List<Long> jenisJabatanIds = session.createCriteria(Pejabat.class)
                            .setProjection(Projections.groupProperty("jenisJabatan.id"))
                            .add(Restrictions.or(
                                Restrictions.or(
                                    Restrictions.ilike("jenisPengguna",
                                        "," + hakAkses.getRoleId() + ",", MatchMode.ANYWHERE),
                                    Restrictions.ilike("usernamePengguna",
                                        "," + tbmuser.getUserId() + ",", MatchMode.ANYWHERE)),
                                Restrictions.and(
                                    Restrictions.or(Restrictions.isNotNull("pegawai"),
                                        Restrictions.or(
                                            Restrictions.isNotNull("guru"),
                                            Restrictions.isNotNull("dosen"))),
                                    Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
                                        Restrictions.or(
                                            Restrictions.eq("dosen", tbmuser.getDosen()),
                                            Restrictions.eq("guru", tbmuser.getGuru()))))))
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                            .list();
                        if (jenisJabatanIds != null && !jenisJabatanIds.isEmpty()) {
                            List<Long> bjab = session.createCriteria(Pegawai.class)
                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                .add(Restrictions.or(
                                    Restrictions.in("atasan.id", jenisJabatanIds),
                                    Restrictions.or(
                                        Restrictions.in("atasanPendukung.id", jenisJabatanIds),
                                        Restrictions.in("atasanPendukungCadangan.id", jenisJabatanIds))))
                                .setProjection(Projections.property("id")).list();
                            if (bjab != null) pegawaiIds.addAll(bjab);
                        }
                    }
                }

                // ── 3. Diri sendiri (jika tidak boleh lihat data pegawai lain) ──
                if (hakAkses != null && !Boolean.TRUE.equals(hakAkses.getMelihatDataPegawaiLain())) {
                    pegawaiIds.add(tbmuser.getPegawai().getId());
                }
            }

            // ── 4. Bawahan dosen (via atasanlangsung Pegawai → dosen) ─────
            if (tbmuser.getDosen() != null) {
                if (tbmuser.getPegawai() != null) {
                    List<Long> bwhnDosen = session.createCriteria(Pegawai.class)
                        .createAlias("dosen", "dosen")
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                        .add(Restrictions.or(
                            Restrictions.eq("atasanlangsung",  tbmuser.getPegawai()),
                            Restrictions.or(
                                Restrictions.eq("atasanlangsung3", tbmuser.getPegawai()),
                                Restrictions.eq("atasanlangsung2", tbmuser.getPegawai()))))
                        .setProjection(Projections.groupProperty("dosen.id")).list();
                    if (bwhnDosen != null) dosenIds.addAll(bwhnDosen);
                }
                // Selalu tambahkan dosen sendiri
                dosenIds.add(tbmuser.getDosen().getId());
            }

        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }

        return new BawahanSet(admin, pegawaiIds, dosenIds);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Aplikasi filter ke Criteria
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Terapkan filter bawahan ke Criteria {@code CutiDanIzin}.
     *
     * <p><b>PRASYARAT:</b> Criteria HARUS sudah memiliki alias {@code "pegawai"}
     * sebelum memanggil method ini:
     * <pre>
     *   criteria.createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN);
     * </pre>
     * </p>
     *
     * <p>Logika filter (sama dengan blok in {@code CutiDanIzinAction.initCriteria()}):
     * <ul>
     *   <li>Admin → tidak ada filter tambahan (lihat semua).</li>
     *   <li>Punya bawahan pegawai+dosen → filter OR: pegawai.id IN / dosen.id IN /
     *       diajukanOleh.pegawai.id IN / diajukanOleh.dosen.id IN.</li>
     *   <li>Punya bawahan pegawai saja → filter OR: pegawai.id IN / diajukanOleh.pegawai.id IN.</li>
     *   <li>Punya bawahan dosen saja → filter OR: dosen.id IN / diajukanOleh.dosen.id IN.</li>
     *   <li>Tidak punya bawahan → hanya data pegawai sendiri (fail-closed: 1=0 jika bukan pegawai).</li>
     * </ul>
     * </p>
     *
     * @param c    Criteria CutiDanIzin (sudah ada alias "pegawai")
     * @param bs   hasil {@link #ambilBawahan}
     * @param user pengguna saat ini (untuk fallback "data sendiri")
     */
    public static void applyFilterCuti(Criteria c, BawahanSet bs, Tbmuser user) {
        if (bs.admin) return; // admin: lihat semua, tidak perlu filter

        boolean hasPegawai = !bs.pegawaiIds.isEmpty();
        boolean hasDosen   = !bs.dosenIds.isEmpty();

        if (hasPegawai && hasDosen) {
            c.createAlias("diajukanOleh", "diajukanOleh", Criteria.LEFT_JOIN)
             .createAlias("pegawai.dosen", "dosenbwh", Criteria.LEFT_JOIN)
             .add(Restrictions.or(
                 Restrictions.or(
                     Restrictions.in("diajukanOleh.pegawai.id", bs.pegawaiIds),
                     Restrictions.in("diajukanOleh.dosen.id",   bs.dosenIds)),
                 Restrictions.or(
                     Restrictions.in("pegawai.id",   bs.pegawaiIds),
                     Restrictions.in("dosenbwh.id",  bs.dosenIds))));

        } else if (hasPegawai) {
            c.createAlias("diajukanOleh", "diajukanOleh", Criteria.LEFT_JOIN)
             .add(Restrictions.or(
                 Restrictions.in("diajukanOleh.pegawai.id", bs.pegawaiIds),
                 Restrictions.in("pegawai.id", bs.pegawaiIds)));

        } else if (hasDosen) {
            c.createAlias("diajukanOleh", "diajukanOleh", Criteria.LEFT_JOIN)
             .createAlias("pegawai.dosen", "dosenbwh", Criteria.LEFT_JOIN)
             .add(Restrictions.or(
                 Restrictions.in("diajukanOleh.dosen.id", bs.dosenIds),
                 Restrictions.in("dosenbwh.id", bs.dosenIds)));

        } else {
            // Bukan admin, tidak punya bawahan → hanya data sendiri (fail-closed)
            if (user != null && user.getPegawai() != null) {
                c.add(Restrictions.eq("pegawai", user.getPegawai()));
            } else {
                c.add(Restrictions.sqlRestriction("1=0"));
            }
        }
    }

    /**
     * Terapkan filter bawahan ke Criteria entitas apa pun yang memiliki properti
     * langsung bernama {@code "pegawai"} (mis. {@code TransaksiPegawai},
     * {@code PerjalananDinas}, dll.).
     *
     * <p><b>PRASYARAT:</b> Criteria HARUS sudah memiliki alias {@code "pegawai"}:
     * <pre>
     *   criteria.createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN);
     * </pre>
     * </p>
     *
     * @param c    Criteria target (sudah ada alias "pegawai")
     * @param bs   hasil {@link #ambilBawahan}
     * @param user pengguna saat ini
     */
    public static void applyFilterByPegawai(Criteria c, BawahanSet bs, Tbmuser user) {
        if (bs.admin) return;

        if (!bs.pegawaiIds.isEmpty()) {
            c.add(Restrictions.in("pegawai.id", bs.pegawaiIds));
        } else if (user != null && user.getPegawai() != null) {
            c.add(Restrictions.eq("pegawai", user.getPegawai()));
        } else {
            c.add(Restrictions.sqlRestriction("1=0"));
        }
    }
}
