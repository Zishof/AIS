package ais.action.master.jurnal;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.TemplateEmailJurnal;

/** Versioned journal template resolver with a strict variable allowlist. */
public final class JurnalEmailService {
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{([A-Za-z][A-Za-z0-9_]*)\\}\\}");
    private static final Pattern UNSAFE_HTML = Pattern.compile(
            "(?is)<\\s*(script|iframe|object|embed|form|meta|base)\\b|on[a-z]+\\s*=|javascript\\s*:|data\\s*:\\s*text/html");
    private final JurnalAuthorizationService auth = new JurnalAuthorizationService();

    /**
     * Tipe implementasi bersarang {@link Rendered} milik {@link JurnalEmailService}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link JurnalEmailService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String subject}, {@code String body},
     * {@code String templateKey}, {@code String locale}, {@code int version}. Aturan bisnis bersama tetap berada
     * pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see JurnalEmailService
     */
    public static final class Rendered {
        public String subject, body, templateKey, locale;
        public int version;
    }

    public TemplateEmailJurnal save(Long journalId, String tenant, String key, String locale,
            String subject, String body, Set<String> variables, Tbmuser actor) {
        auth.requireCrud(actor, "communications", "update");
        if (!JurnalEmailTemplateCatalog.contains(key))
            throw new IllegalArgumentException("Key template tidak dikenal.");
        String normalizedLocale = locale(locale);
        validateVariables(subject, body, variables);
        Session s = HibernateUtil.currentSession();
        Transaction tx = s.getTransaction();
        boolean own = !tx.isActive();
        try {
            if (own) tx.begin();
            auth.requireJournalScope(s, actor, journalId, null, null, false, "JOURNAL");
            Number n = (Number) s.createQuery(
                    "select max(versionNumber) from TemplateEmailJurnal "
                    + "where jurnalPenelitianId=:j and templateKey=:k and locale=:l")
                    .setLong("j", journalId).setString("k", key).setString("l", normalizedLocale)
                    .uniqueResult();
            TemplateEmailJurnal t = new TemplateEmailJurnal();
            t.setTenantKey(clean(tenant).length() == 0 ? "default" : clean(tenant));
            t.setJurnalPenelitianId(journalId);
            t.setTemplateKey(key);
            t.setLocale(normalizedLocale);
            t.setSubjectTemplate(subject.trim());
            t.setBodyTemplate(body);
            JSONObject policy = new JSONObject();
            JSONArray a = new JSONArray();
            for (String v : variables) a.put(v);
            policy.put("schemaVersion", 1);
            policy.put("allowed", a);
            t.setVariablePolicyJson(policy.toString());
            t.setVersionNumber(n == null ? 1 : n.intValue() + 1);
            t.setCreatedBy(actor.getUserId());
            t.setCreatedAt(new Date());
            t.setUpdatedAt(new Date());
            t.setAktif(Boolean.TRUE);
            s.save(t);
            if (own) tx.commit();
            return t;
        } catch (RuntimeException e) {
            if (own && tx.isActive()) tx.rollback();
            throw e;
        } catch (Exception e) {
            if (own && tx.isActive()) tx.rollback();
            throw new IllegalArgumentException("Template email tidak valid.", e);
        }
    }

    /** Seeds exactly 73 keys x two locales, without overwriting configured versions. */
    @SuppressWarnings("unchecked")
    public int seedDefaults(Long journalId,String tenant,Tbmuser actor){
        auth.requireCrud(actor,"communications","create");Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();int created=0;
        try{if(own)tx.begin();auth.requireJournalScope(s,actor,journalId,null,null,false,"JOURNAL");
            Set<String> active=new HashSet<String>();for(Object[] row:(java.util.List<Object[]>)s.createQuery("select templateKey,locale from TemplateEmailJurnal where jurnalPenelitianId=:j and aktif=true").setLong("j",journalId).list())active.add(row[0]+"|"+row[1]);
            Map<String,Integer> versions=new HashMap<String,Integer>();for(Object[] row:(java.util.List<Object[]>)s.createQuery("select templateKey,locale,max(versionNumber) from TemplateEmailJurnal where jurnalPenelitianId=:j group by templateKey,locale").setLong("j",journalId).list())versions.put(row[0]+"|"+row[1],row[2]==null?Integer.valueOf(0):Integer.valueOf(((Number)row[2]).intValue()));
            JSONObject policy=new JSONObject();JSONArray allowed=new JSONArray();for(String v:JurnalEmailTemplateCatalog.STANDARD_VARIABLES)allowed.put(v);policy.put("schemaVersion",1).put("allowed",allowed);String policyJson=policy.toString();Date now=new Date();
            for(JurnalEmailTemplateCatalog.Definition d:JurnalEmailTemplateCatalog.definitions()){String composite=d.key+"|"+d.locale;if(active.contains(composite))continue;validateVariables(d.subject,d.body,JurnalEmailTemplateCatalog.STANDARD_VARIABLES);TemplateEmailJurnal t=new TemplateEmailJurnal();t.setTenantKey(clean(tenant).length()==0?"default":clean(tenant));t.setJurnalPenelitianId(journalId);t.setTemplateKey(d.key);t.setLocale(d.locale);t.setSubjectTemplate(d.subject);t.setBodyTemplate(d.body);t.setVariablePolicyJson(policyJson);t.setVersionNumber((versions.containsKey(composite)?versions.get(composite).intValue():0)+1);t.setCreatedBy(actor.getUserId());t.setCreatedAt(now);t.setUpdatedAt(now);t.setAktif(Boolean.TRUE);s.save(t);active.add(composite);created++;if(created%50==0)s.flush();}
            if(own)tx.commit();return created;}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}catch(Exception e){if(own&&tx.isActive())tx.rollback();throw new IllegalArgumentException("Seed template email gagal.",e);}
    }

    public Rendered render(Long journalId, String key, String locale, Map<String, String> values) {
        if (!JurnalEmailTemplateCatalog.contains(key))
            throw new IllegalArgumentException("Key template tidak dikenal.");
        if (values == null) throw new IllegalArgumentException("Nilai variabel wajib tersedia.");
        Session s = HibernateUtil.currentSession();
        Query q = s.createQuery("from TemplateEmailJurnal where jurnalPenelitianId=:j "
                + "and templateKey=:k and locale=:l and aktif=true order by versionNumber desc");
        q.setLong("j", journalId);
        q.setString("k", key);
        q.setString("l", locale(locale));
        q.setMaxResults(1);
        TemplateEmailJurnal t = (TemplateEmailJurnal) q.uniqueResult();
        if (t == null) throw new IllegalArgumentException("Template email belum dikonfigurasi.");
        try {
            JSONObject p = new JSONObject(t.getVariablePolicyJson());
            if (p.optInt("schemaVersion", 0) != 1)
                throw new IllegalArgumentException("Versi kontrak template email tidak didukung.");
            Set<String> allowed = new HashSet<String>();
            JSONArray a = p.getJSONArray("allowed");
            for (int i = 0; i < a.length(); i++) allowed.add(a.getString(i));
            for (String supplied : values.keySet())
                if (!allowed.contains(supplied))
                    throw new IllegalArgumentException("Variabel email tidak diizinkan: " + supplied);
            Rendered r = new Rendered();
            r.templateKey = key;
            r.locale = t.getLocale();
            r.version = t.getVersionNumber();
            r.subject = replace(t.getSubjectTemplate(), values, allowed);
            r.body = replace(t.getBodyTemplate(), values, allowed);
            if (r.subject.indexOf('\r') >= 0 || r.subject.indexOf('\n') >= 0)
                throw new IllegalArgumentException("Subject email mengandung karakter header tidak valid.");
            if (UNSAFE_HTML.matcher(r.body).find())
                throw new IllegalArgumentException("Isi email mengandung HTML tidak aman.");
            return r;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Kontrak template email rusak.", e);
        }
    }

    private static String replace(String text, Map<String, String> values, Set<String> allowed) {
        Matcher m = VARIABLE.matcher(text == null ? "" : text);
        StringBuffer b = new StringBuffer();
        while (m.find()) {
            String k = m.group(1);
            if (!allowed.contains(k) || !values.containsKey(k))
                throw new IllegalArgumentException("Variabel email belum tersedia: " + k);
            m.appendReplacement(b, Matcher.quoteReplacement(values.get(k) == null ? "" : values.get(k)));
        }
        m.appendTail(b);
        return b.toString();
    }

    private static void validateVariables(String subject, String body, Set<String> allowed) {
        if (subject == null || body == null || subject.length() > 1000 || body.length() > 262144)
            throw new IllegalArgumentException("Ukuran template tidak valid.");
        if (subject.indexOf('\r') >= 0 || subject.indexOf('\n') >= 0)
            throw new IllegalArgumentException("Subject email mengandung karakter header tidak valid.");
        if (UNSAFE_HTML.matcher(body).find())
            throw new IllegalArgumentException("Isi email mengandung HTML tidak aman.");
        if (allowed == null || allowed.size() > 100)
            throw new IllegalArgumentException("Daftar variabel template tidak valid.");
        for (String key : allowed)
            if (key == null || !key.matches("[A-Za-z][A-Za-z0-9_]{0,79}"))
                throw new IllegalArgumentException("Nama variabel email tidak valid.");
        Matcher m = VARIABLE.matcher(subject + "\n" + body);
        while (m.find())
            if (!allowed.contains(m.group(1)))
                throw new IllegalArgumentException("Variabel tidak diizinkan: " + m.group(1));
    }

    private static String locale(String value) {
        String x = clean(value);
        if (!x.matches("[a-z]{2}(?:_[A-Z]{2})?"))
            throw new IllegalArgumentException("Locale template tidak valid.");
        return x;
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
}
