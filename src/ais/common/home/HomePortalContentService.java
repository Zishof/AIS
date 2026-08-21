package ais.common.home;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.Jurusan;
import ais.database.model.PengumumanAkademis;
import ais.database.model.sekolah.GelombangPendaftaranPsb;

/** Read-only, bounded public content queries. Failures hide a section instead of breaking home. */
public class HomePortalContentService {
    private final HomePortalLinkResolver links;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", new Locale("id", "ID"));

    public HomePortalContentService(HomePortalLinkResolver links) {
        this.links = links;
    }

    @SuppressWarnings("unchecked")
    public void loadPrograms(HomePortalViewModel vm, HomePortalSectionResolver config) {
        if (!vm.institution.college || vm.institution.id == null) return;
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            List<Jurusan> rows = session.createQuery("select distinct j from Jurusan j left join fetch j.jenjang left join fetch j.fakultas where j.aktif = true and j.fakultas.perguruanTinggi.id = :pt order by j.nama")
                    .setParameter("pt", vm.institution.id).setMaxResults(6).list();
            String detailUrl = config.value("home_v3_programs_url", "#program");
            for (Jurusan row : rows) {
                HomePortalViewModel.ProgramItem item = new HomePortalViewModel.ProgramItem();
                item.label = clean(row.getNama());
                item.level = row.getJenjang() == null ? "" : clean(row.getJenjang().getNama());
                item.unit = row.getFakultas() == null ? "" : clean(row.getFakultas().getNama());
                item.accreditation = clean(row.getPeringkatAkreditasi());
                item.description = compact(first(row.getDeskripsi(), row.getProfil()), 180);
                copyLink(item, links.resolve(detailUrl, "#program"));
                if (item.label.length() > 0) vm.programs.add(item);
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "HomePortalContentService.loadPrograms");
        } finally {
            close(session);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadNews(HomePortalViewModel vm, HomePortalSectionResolver config) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            StringBuilder hql = new StringBuilder("from PengumumanAkademis p where p.aktif = true and p.diperuntukkan = :audience");
            if (vm.institution.college && vm.institution.id != null) hql.append(" and p.perguruanTinggi.id = :tenant");
            else if (vm.institution.schoolId != null) hql.append(" and p.sekolah.id = :tenant");
            else if (vm.institution.foundationId != null) hql.append(" and p.yayasan.id = :tenant");
            else return;
            org.hibernate.Query query = session.createQuery(hql.append(" order by p.tanggal desc, p.id desc").toString())
                    .setParameter("audience", PengumumanAkademis.UNTUK_UMUM)
                    .setParameter("tenant", vm.institution.college ? vm.institution.id : (vm.institution.schoolId != null ? vm.institution.schoolId : vm.institution.foundationId))
                    .setMaxResults(3);
            List<PengumumanAkademis> rows = query.list();
            String url = config.value("home_v3_news_url", "#informasi");
            for (PengumumanAkademis row : rows) {
                HomePortalViewModel.NewsItem item = new HomePortalViewModel.NewsItem();
                item.label = clean(row.getJudul());
                item.summary = compact(row.getCatatan(), 220);
                item.date = format(row.getTanggal());
                item.category = "Pengumuman";
                copyLink(item, links.resolve(url, "#informasi"));
                if (item.label.length() > 0) vm.news.add(item);
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "HomePortalContentService.loadNews");
        } finally {
            close(session);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadAdmission(HomePortalViewModel vm, HomePortalSectionResolver config) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Date now = new Date();
            Object row;
            if (vm.institution.college) {
                List<GelombangPendaftaran> rows = session.createQuery("from GelombangPendaftaran g where g.aktif = true and (g.sampai is null or g.sampai >= :now) order by g.mulai asc")
                        .setParameter("now", now).setMaxResults(1).list();
                row = rows.isEmpty() ? null : rows.get(0);
            } else if (vm.institution.schoolId != null) {
                List<GelombangPendaftaranPsb> rows = session.createQuery("from GelombangPendaftaranPsb g where g.aktif = true and g.sekolah.id = :school and (g.sampai is null or g.sampai >= :now) order by g.mulai asc")
                        .setParameter("school", vm.institution.schoolId).setParameter("now", now).setMaxResults(1).list();
                row = rows.isEmpty() ? null : rows.get(0);
            } else return;
            if (row != null) {
                HomePortalViewModel.Admission a = new HomePortalViewModel.Admission();
                Date start;
                Date end;
                if (row instanceof GelombangPendaftaran) {
                    GelombangPendaftaran g = (GelombangPendaftaran) row;
                    a.period = clean(g.getNama()) + (g.getTahunAkademik() == null ? "" : " · " + clean(g.getTahunAkademik()));
                    start = g.getMulai(); end = g.getSampai();
                } else {
                    GelombangPendaftaranPsb g = (GelombangPendaftaranPsb) row;
                    a.period = clean(g.getNama()); start = g.getMulai(); end = g.getSampai();
                }
                a.startDate = format(start); a.endDate = format(end); a.open = end == null || !end.before(now);
                a.label = config.value("home_v3_primary_cta_label", "Daftar Sekarang");
                a.description = config.value("home_v3_admission_description", "Temukan jadwal, persyaratan, dan proses penerimaan melalui portal resmi.");
                copyLink(a, links.resolve(config.value("home_v3_primary_cta_url", vm.institution.college ? "/pmb" : "/ppdb"), vm.institution.college ? "/pmb" : "/ppdb"));
                vm.admission = a;
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "HomePortalContentService.loadAdmission");
        } finally {
            close(session);
        }
    }

    public void loadConfiguredContent(HomePortalViewModel vm, HomePortalSectionResolver config) {
        for (int n = 1; n <= 6; n++) {
            String value = config.value("home_v3_stat_" + n + "_value", "");
            String label = config.value("home_v3_stat_" + n + "_label", "");
            if (value.length() > 0 && label.length() > 0) {
                HomePortalViewModel.StatItem item = new HomePortalViewModel.StatItem(); item.value = value; item.label = label; vm.statistics.add(item);
            }
        }
        if (vm.programs.isEmpty()) {
            for (int n = 1; n <= 6; n++) {
                String name = config.value("home_v3_program_" + n + "_name", "");
                if (name.length() == 0) continue;
                HomePortalViewModel.ProgramItem item = new HomePortalViewModel.ProgramItem();
                item.label = name; item.level = config.value("home_v3_program_" + n + "_level", "");
                item.accreditation = config.value("home_v3_program_" + n + "_accreditation", "");
                item.description = config.value("home_v3_program_" + n + "_description", "");
                copyLink(item, links.resolve(config.value("home_v3_program_" + n + "_url", "#program"), "#program"));
                vm.programs.add(item);
            }
        }
    }

    private String first(String a, String b) { return a != null && a.trim().length() > 0 ? a : b; }
    private String clean(String value) { return value == null ? "" : value.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim(); }
    private String compact(String value, int max) { String v = clean(value); return v.length() > max ? v.substring(0, max - 1).trim() + "…" : v; }
    private String format(Date value) { return value == null ? "" : dateFormat.format(value); }
    private void close(Session session) { if (session != null && session.isOpen()) try { session.close(); } catch (Exception ignored) { } }
    private void copyLink(HomePortalViewModel.LinkItem to, HomePortalViewModel.LinkItem from) { to.url = from.url; to.target = from.target; to.rel = from.rel; }
}
