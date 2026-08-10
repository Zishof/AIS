package ais.common;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmrole;
import ais.database.model.sop.AktorSop;
import ais.database.model.sop.AlurSop;
import ais.database.model.sop.JenisSop;
import ais.database.model.sop.Sop;

/**
 * Membuat template workflow Audit Mutu Internal (AMI) pada mesin SOP existing.
 *
 * <p>Template bersumber dari dokumen WORKFLOW AMI tahun 2026 dan sengaja
 * menggunakan entity existing ({@link Sop}, {@link AlurSop}, {@link AktorSop},
 * dan {@link JenisSop}). Tidak ada tabel workflow paralel.</p>
 *
 * <p>Inisialisasi bersifat idempoten. Pengecekan SOP dilakukan sebelum master
 * pendukung dibuat. Jika kode atau nama SOP sudah ditemukan, data lama tidak
 * ditambah, dilengkapi, maupun diubah.</p>
 */
public final class AmiSopWorkflowTemplate {

    public static final String SOP_CODE = "SOP-AMI-LPM-UINMY-2026";
    public static final String SOP_NAME = "Audit Mutu Internal (AMI)";
    public static final String SOP_VERSION = "1.0/2026";
    public static final String JENIS_CODE = "SPMI-AMI";
    public static final String JENIS_NAME = "SPMI / Audit Mutu Internal";
    public static final int STEP_COUNT = 12;

    private static final String FORM_INPUT = "ais.action.master.spmi.HasilSPMIAction";
    private static final String SOURCE_DOCUMENT =
            "https://docs.google.com/document/d/1qurA97qMBD1XGJRpp0mY7Ry-7OiFz_PU/edit";
    private static final Object LOCK = new Object();

    private static final String[][] STEPS = new String[][] {
        { "001", "Pengajuan bukti audit", "Kaprodi", "Diajukan",
                "Notifikasi ke Admin LPM" },
        { "002", "Verifikasi kelengkapan", "Admin LPM", "Diverifikasi / Dikembalikan",
                "Jika lengkap, lanjut ke penjadwalan dan penugasan auditor" },
        { "003", "Penjadwalan dan penugasan auditor", "Kepala Pusat Audit Mutu Internal", "Terjadwal",
                "Sistem mengirim notifikasi kepada auditor dan auditee" },
        { "004", "Notifikasi otomatis", "Sistem", "Terkirim",
                "Auditor dan auditee memperoleh akses workflow AMI" },
        { "005", "Desk evaluation online", "Tim Auditor", "Sedang Direview",
                "Verifikasi silang data e-Campus, Litabdimas, repository, dan sistem pendukung" },
        { "006", "Audit lapangan (visitasi)", "Tim Auditor", "Visitasi Berlangsung",
                "Opening meeting, pemeriksaan, wawancara, dan klarifikasi temuan awal" },
        { "007", "Input skor dan temuan", "Tim Auditor", "Temuan Terinput",
                "Klasifikasi hasil menjadi OK, Observasi, atau KTS" },
        { "008", "Validasi temuan", "Kepala Pusat Audit Mutu Internal", "Tervalidasi",
                "Laporan Hasil Audit siap dipublikasi" },
        { "009", "Publikasi LHA ke auditee", "Sistem", "Terpublikasi",
                "Auditee menerima notifikasi temuan KTS" },
        { "010", "Pengajuan Rencana Tindak Lanjut (RTL)", "Kaprodi", "RTL Diajukan",
                "Auditor memverifikasi bukti dan realisasi RTL" },
        { "011", "Verifikasi dan closing RTL", "Auditor dan Ketua LPM", "Closed / Belum Selesai",
                "Jika closed masuk rekap; jika belum selesai kembali ke pengajuan RTL" },
        { "012", "Monitoring dan Rapat Tinjauan Manajemen (RTM)", "Rektor dan Ketua LPM",
                "Dashboard Real-time", "Menjadi dasar kebijakan dan siklus PPEPP berikutnya" }
    };

    private AmiSopWorkflowTemplate() {
    }

    /**
     * Entry point startup. Kegagalan template dicatat tetapi tidak menggagalkan
     * seluruh aplikasi; transaksi template sendiri selalu di-rollback.
     */
    public static boolean ensureCreated() {
        synchronized (LOCK) {
            Session session = null;
            Transaction transaction = null;
            List<GeneralValueObject> created = new ArrayList<GeneralValueObject>();
            try {
                session = HibernateUtil.openSession();
                transaction = session.beginTransaction();
                boolean inserted = ensureCreated(session, created);
                transaction.commit();
                if (inserted) {
                    refreshMemoryCache(created);
                    System.out.println("AMI SOP workflow template created: " + SOP_CODE
                            + " (" + STEP_COUNT + " steps)");
                } else {
                    System.out.println("AMI SOP workflow template skipped; SOP already exists: " + SOP_CODE);
                }
                return inserted;
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    try {
                        transaction.rollback();
                    } catch (Exception ignored) {
                        ErrorAuditUtil.record(ignored,
                                "auto-audit AmiSopWorkflowTemplate.ensureCreated:rollback");
                    }
                }
                ErrorAuditUtil.record(e, "auto-audit AmiSopWorkflowTemplate.ensureCreated");
                e.printStackTrace();
                return false;
            } finally {
                HibernateUtil.closeSessionQuietly(session);
            }
        }
    }

    /**
     * Variant yang menerima session untuk pengujian integrasi. Caller mengelola
     * transaksi dan lifecycle session.
     */
    public static boolean ensureCreated(Session session) {
        return ensureCreated(session, new ArrayList<GeneralValueObject>());
    }

    @SuppressWarnings("unchecked")
    private static boolean ensureCreated(Session session, List<GeneralValueObject> created) {
        if (session == null) {
            throw new IllegalArgumentException("Session untuk template workflow AMI tidak boleh null.");
        }

        // Serialisasi pengecekan+pembuatan antar node aplikasi yang memakai DB sama.
        // Tanpa lock, dua startup serentak dapat sama-sama membaca "belum ada" lalu
        // membuat SOP ganda karena tabel legacy belum memiliki unique constraint kode.
        session.createSQLQuery("LOCK TABLE public.sop IN SHARE ROW EXCLUSIVE MODE").executeUpdate();

        Sop existing = (Sop) session.createCriteria(Sop.class)
                .add(Restrictions.or(Restrictions.ilike("kode", SOP_CODE, MatchMode.EXACT),
                        Restrictions.ilike("nama", "Audit Mutu Internal", MatchMode.ANYWHERE)))
                .setMaxResults(1).uniqueResult();
        if (existing != null) {
            return false;
        }

        Map<String, AktorSop> actors = new LinkedHashMap<String, AktorSop>();
        actors.put("Kaprodi", ensureActor(session, created, "AMI-KAPRODI", "Kaprodi",
                new String[] { "kaprodi", "ketua program studi" }));
        actors.put("Admin LPM", ensureActor(session, created, "AMI-ADMIN-LPM", "Admin LPM",
                new String[] { "admin lpm", "lpm", "spmi", "penjaminan mutu" }));
        actors.put("Kepala Pusat Audit Mutu Internal", ensureActor(session, created, "AMI-KEPALA-PUSAT",
                "Kepala Pusat Audit Mutu Internal",
                new String[] { "kepala pusat audit", "lpm", "spmi", "penjaminan mutu" }));
        actors.put("Sistem", ensureActor(session, created, "AMI-SISTEM", "Sistem",
                new String[] { "administrator" }));
        actors.put("Tim Auditor", ensureActor(session, created, "AMI-AUDITOR", "Tim Auditor",
                new String[] { "auditor", "ami", "lpm", "spmi" }));
        actors.put("Auditor dan Ketua LPM", ensureActor(session, created, "AMI-AUDITOR-KETUA-LPM",
                "Auditor dan Ketua LPM", new String[] { "auditor", "ami", "ketua lpm", "lpm", "spmi" }));
        actors.put("Rektor dan Ketua LPM", ensureActor(session, created, "AMI-REKTOR-KETUA-LPM",
                "Rektor dan Ketua LPM", new String[] { "rektor", "ketua lpm", "lpm", "spmi" }));

        JenisSop jenis = ensureJenis(session, created, actors.get("Kaprodi"));

        Sop sop = new Sop();
        sop.setKode(SOP_CODE);
        sop.setNama(SOP_NAME);
        sop.setVersi(SOP_VERSION);
        sop.setTanggalTerbit(new Date());
        sop.setJenisSop(jenis);
        sop.setAktif(Boolean.TRUE);
        sop.setUntukUjiCoba(Boolean.FALSE);
        sop.setKeterangan("Workflow Audit Mutu Internal berdasarkan dokumen WORKFLOW AMI 2026. "
                + "Mencakup pengajuan bukti, verifikasi, penugasan auditor, desk evaluation, visitasi, "
                + "LHA, RTL, closing, monitoring, dan RTM. Sumber: " + SOURCE_DOCUMENT);
        session.save(sop);
        created.add(sop);

        List<AlurSop> flows = new ArrayList<AlurSop>();
        AlurSop previous = null;
        for (int i = 0; i < STEPS.length; i++) {
            String[] definition = STEPS[i];
            AlurSop flow = new AlurSop();
            flow.setKode(definition[0]);
            flow.setNama(definition[1]);
            flow.setAktorSop(actors.get(definition[2]));
            flow.setOpsi(definition[3]);
            flow.setKeterangan("Status dashboard: " + definition[3] + ". Trigger berikutnya: "
                    + definition[4] + ". Aktor: " + definition[2] + ".");
            flow.setSop(sop);
            flow.setSebelumnya(previous);
            flow.setNomor(Integer.valueOf(i + 1));
            flow.setStart(Boolean.valueOf(i == 0));
            flow.setAktif(Boolean.TRUE);
            flow.setJangkaWaktu(Integer.valueOf(i == 0 ? 14 : 1));
            flow.setFormInputan(FORM_INPUT);
            flow.setBekukanFormTampilan(Boolean.valueOf(i != 0 && i != 9));
            flow.setBekukanDokumen(Boolean.valueOf(!canAttachEvidence(i + 1)));
            flow.setBolehDiisiCatatan(Boolean.valueOf(i != 3 && i != 8));
            flow.setCatatanWajibDiisi(Boolean.valueOf(requiresNote(i + 1)));
            flow.setLampiranCatatanWajibDiisi(Boolean.valueOf(requiresEvidence(i + 1)));
            flow.setKembaliKeAktorSebelumnya(Boolean.valueOf(canReturn(i + 1)));
            flow.setAlurSetelahnyaOtomatis(Boolean.FALSE);
            flow.setAlurSetelahnyaBerupaPilihan(Boolean.FALSE);
            session.save(flow);
            created.add(flow);
            flows.add(flow);
            previous = flow;
        }

        for (int i = 0; i < flows.size() - 1; i++) {
            AlurSop current = flows.get(i);
            current.setSetelahnya(flows.get(i + 1));
            current.setOpsiSetelahnya("Lanjutkan");
            current.setPersetujuanAdaDiSini1(Boolean.FALSE);
            session.update(current);
        }

        // Closing RTL memiliki dua keluaran sesuai dokumen: selesai ke monitoring,
        // atau kembali ke tahap pengajuan RTL bila bukti belum memadai.
        AlurSop closing = flows.get(10);
        closing.setAlurSetelahnyaBerupaPilihan(Boolean.TRUE);
        closing.setOpsiSetelahnya("Closed");
        closing.setSetelahnya2(flows.get(9));
        closing.setOpsiSetelahnya2("Belum Selesai - Kembalikan RTL");
        closing.setPersetujuanAdaDiSini1(Boolean.FALSE);
        closing.setPersetujuanAdaDiSini2(Boolean.FALSE);
        session.update(closing);

        AlurSop finalStep = flows.get(11);
        finalStep.setJikaProsesDisetujuiMakaSelesai(Boolean.TRUE);
        finalStep.setPersetujuanAdaDiSini(Boolean.TRUE);
        finalStep.setAlurSetelahnyaTidakWajib(Boolean.TRUE);
        session.update(finalStep);

        session.flush();
        return true;
    }

    @SuppressWarnings("unchecked")
    private static AktorSop ensureActor(Session session, List<GeneralValueObject> created,
            String code, String name, String[] roleKeywords) {
        AktorSop actor = (AktorSop) session.createCriteria(AktorSop.class)
                .add(Restrictions.or(Restrictions.eq("kode", code),
                        Restrictions.ilike("nama", name, MatchMode.EXACT)))
                .setMaxResults(1).uniqueResult();
        if (actor != null) {
            return actor;
        }

        actor = new AktorSop();
        actor.setKode(code);
        actor.setNama(name);
        actor.setAktif(Boolean.TRUE);
        actor.setKeterangan("Aktor template workflow AMI. Hak akses awal dipetakan dari role yang namanya sesuai; "
                + "administrator tetap disertakan agar konfigurasi dapat disempurnakan per kampus.");
        actor.setJenisPengguna(findRoleIds(session, roleKeywords));
        actor.setUsernamePengguna("admin");
        session.save(actor);
        created.add(actor);
        return actor;
    }

    private static JenisSop ensureJenis(Session session, List<GeneralValueObject> created, AktorSop startActor) {
        JenisSop jenis = (JenisSop) session.createCriteria(JenisSop.class)
                .add(Restrictions.or(Restrictions.eq("kode", JENIS_CODE),
                        Restrictions.ilike("nama", JENIS_NAME, MatchMode.EXACT)))
                .setMaxResults(1).uniqueResult();
        if (jenis != null) {
            return jenis;
        }

        jenis = new JenisSop();
        jenis.setKode(JENIS_CODE);
        jenis.setNama(JENIS_NAME);
        jenis.setKeterangan("Kelompok SOP untuk pelaksanaan Audit Mutu Internal pada modul SPMI.");
        jenis.setWarna("#E8F5E9");
        jenis.setWarnatext("#1B5E20");
        jenis.setAktif(Boolean.TRUE);
        jenis.setAktorSop(startActor);
        session.save(jenis);
        created.add(jenis);
        return jenis;
    }

    @SuppressWarnings("unchecked")
    private static String findRoleIds(Session session, String[] keywords) {
        Set<String> ids = new LinkedHashSet<String>();
        ids.add(Tbmrole.ADMINISTRATOR);
        List<Tbmrole> roles = session.createCriteria(Tbmrole.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                .list();
        for (Tbmrole role : roles) {
            if (role == null || role.getRoleId() == null || role.getRoleId().trim().length() == 0) {
                continue;
            }
            String haystack = normalize(role.getRoleId() + " " + role.getRoleName());
            for (int i = 0; keywords != null && i < keywords.length; i++) {
                if (haystack.indexOf(normalize(keywords[i])) >= 0) {
                    ids.add(role.getRoleId().trim());
                    break;
                }
            }
        }
        StringBuilder result = new StringBuilder();
        for (String id : ids) {
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(id);
        }
        return result.toString();
    }

    private static String normalize(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static boolean canAttachEvidence(int step) {
        return step == 1 || step == 5 || step == 6 || step == 7 || step == 10 || step == 11;
    }

    private static boolean requiresEvidence(int step) {
        return step == 1 || step == 7 || step == 10 || step == 11;
    }

    private static boolean requiresNote(int step) {
        return step == 2 || step == 5 || step == 6 || step == 7 || step == 8 || step == 10 || step == 11;
    }

    private static boolean canReturn(int step) {
        return step == 2 || step == 5 || step == 6 || step == 8 || step == 11;
    }

    private static void refreshMemoryCache(List<GeneralValueObject> created) {
        for (GeneralValueObject value : created) {
            try {
                InitDataHelper.reInitDataBaru(value);
            } catch (Exception e) {
                ErrorAuditUtil.record(e, "auto-audit AmiSopWorkflowTemplate.refreshMemoryCache");
            }
        }
    }
}
