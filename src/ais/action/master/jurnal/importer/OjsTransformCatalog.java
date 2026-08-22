package ais.action.master.jurnal.importer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Executable table/field disposition catalog pinned to the 134-table matrix. */
public final class OjsTransformCatalog {
    private static final Set<String> ALTER = set(
        "announcement_settings announcement_type_settings announcement_types announcements custom_issue_orders custom_section_orders doi_settings dois edit_decisions files highlight_settings highlights issue_files issue_galley_settings issue_galleys issue_settings issues journal_settings journals library_file_settings library_files notes publication_categories publication_galley_settings publication_galleys publication_settings publications queries review_files review_round_files review_rounds submission_comments submission_file_revisions submission_file_settings submission_files submission_settings submissions");
    private static final Set<String> NEW_MODEL = set(
        "email_templates email_templates_default_data email_templates_settings institutional_subscriptions invitations query_participants review_assignment_settings review_assignments stage_assignments subeditor_submission_group subscriptions");
    private static final Set<String> DERIVED = set(
        "submission_search_keyword_list submission_search_object_keywords submission_search_objects versions");
    private static final Set<String> NOT_APPLICABLE = set(
        "failed_jobs filter_groups filter_settings filters job_batches jobs oai_resumption_tokens sessions temporary_files usage_stats_institution_temporary_records usage_stats_total_temporary_records usage_stats_unique_item_investigations_temporary_records usage_stats_unique_item_requests_temporary_records");

    public static final class Outcome {
        public final String disposition, targetType, targetField;
        Outcome(String d, String t, String f) { disposition=d; targetType=t; targetField=f; }
    }

    private OjsTransformCatalog() {}

    public static Outcome outcome(String table, String field) {
        String t=clean(table), f=clean(field);
        if (!OjsSourceCatalog.TABLE_SET.contains(t) || f.length()==0)
            throw new IllegalArgumentException("Tabel/field OJS tidak dikenal oleh transform catalog.");
        String disposition=NOT_APPLICABLE.contains(t)?"NOT_APPLICABLE_WITH_RATIONALE":DERIVED.contains(t)?"DERIVED":NEW_MODEL.contains(t)?"NEW_MODEL":ALTER.contains(t)?"ALTER_EXISTING":"MERGED";
        if (NOT_APPLICABLE.contains(t)) return new Outcome(disposition,"NONE","notApplicable."+t+"."+f);
        if (DERIVED.contains(t)) return new Outcome(disposition,"RepoSearchProjection","derived."+t+"."+f);
        return new Outcome(disposition,target(t),"ojs."+t+"."+f);
    }

    public static String disposition(String table) { return outcome(table,"_table").disposition; }
    public static int count(String disposition) { int n=0; for(String t:OjsSourceCatalog.TABLES) if(disposition.equals(disposition(t))) n++; return n; }

    private static String target(String t) {
        if (t.startsWith("email_template")) return "TemplateEmailJurnal";
        if (t.contains("subscription") || "institution_ip".equals(t)) return t.contains("_ip")?"RentangIpLanggananJurnal":"LanggananJurnal";
        if ("invitations".equals(t)) return "UndanganPeranJurnal";
        if (t.startsWith("review_assignment")) return "PenugasanReviewerJurnal";
        if (t.contains("stage_assign") || "subeditor_submission_group".equals(t) || t.startsWith("user_group") || "user_user_groups".equals(t)) return "PenugasanTahapJurnal";
        if ("query_participants".equals(t)) return "PesertaDiskusiJurnal";
        if (t.startsWith("queries") || t.startsWith("notes") || t.contains("comments")) return "Diskusi";
        if (t.startsWith("metric")) return "RepoUsageEvent/AgregatPenggunaanJurnal";
        if (t.contains("payment")) return "LogPembayaran";
        if (t.startsWith("notification") || t.startsWith("email_log")) return "Notifikasi";
        if (t.startsWith("announcement")) return "PengumumanPenelitian";
        if (t.contains("file") || t.contains("galley")) return "RepoBitstream";
        if (t.startsWith("issue")) return "RepoItem(JOURNAL_ISSUE)";
        if (t.startsWith("review_")) return "PenugasanReviewerJurnal/RepoWorkflowEvent";
        if (t.startsWith("doi")) return "RepoItem.identifier";
        if (t.startsWith("author") || t.startsWith("ror") || t.startsWith("user")) return "RepoAuthorAuthority/RepoItemContributor";
        if (t.startsWith("submission") || t.startsWith("publication") || t.startsWith("citation")) return "RepoItem/RepoItemMetadata";
        if (t.startsWith("journal") || t.startsWith("section") || t.startsWith("categor") || t.startsWith("controlled_vocab") || t.startsWith("genre") || t.startsWith("navigation") || t.startsWith("static_page") || t.startsWith("plugin") || t.startsWith("site") || t.startsWith("highlight")) return "RepoCollection.profileJson";
        if (t.startsWith("data_object_tombstone")) return "RepoItem.tombstone";
        return "ImportMappingOjs.provenance";
    }

    private static Set<String> set(String raw) { return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(raw.split(" ")))); }
    private static String clean(String v) { return v==null?"":v.trim().toLowerCase(); }
}
