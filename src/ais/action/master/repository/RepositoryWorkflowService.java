package ais.action.master.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.repository.RepoBitstream;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoItemMetadata;
import ais.database.model.repository.RepoWorkflowEvent;

/** Typed repository deposit and review state machine. */
public class RepositoryWorkflowService {
    public static final String DRAFT = "DRAFT";
    public static final String SUBMITTED = "SUBMITTED";
    public static final String IN_REVIEW = "IN_REVIEW";
    public static final String REVISION_REQUIRED = "REVISION_REQUIRED";
    public static final String REJECTED = "REJECTED";
    public static final String APPROVED = "APPROVED";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String WITHDRAWN = "WITHDRAWN";

    private static final Set<String> ACCESS_POLICIES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "OPEN_ACCESS", "METADATA_ONLY", "EMBARGOED", "INSTITUTION_ONLY", "AUTHENTICATED", "RESTRICTED")));
    private static final Map<String, Set<String> > TRANSITIONS = transitions();

    public static class DraftInput {
        public Long id;
        public Long expectedVersion;
        public Long collectionId;
        public String title;
        public String authors;
        public String authorOrcids;
        public String abstractText;
        public String subjects;
        public String publisher;
        public String language;
        public String documentType;
        public String accessPolicy;
        public String licenseUri;
        public Date embargoUntil;
        public String doi;
    }

    public static class ValidationResult {
        public boolean valid;
        public List<String> errors = new ArrayList<String>();
    }

    public static class DuplicateCandidate {
        public Long id;
        public String title;
        public String authors;
        public String workflowStatus;
    }

    public RepoItem createDraft(final DraftInput input, final Tbmuser actor, final String requestId) {
        requireLogin(actor);
        return write(new Work<RepoItem>() {
            public RepoItem run(Session session) {
                ensureCollectionAcceptsDeposit(session, input == null ? null : input.collectionId);
                RepoItem item = new RepoItem();
                item.setCollectionId(input.collectionId);
                item.setWorkflowStatus(DRAFT);
                item.setSyncStatus(DRAFT);
                item.setOwnerId(actor.getUserId());
                item.setSubmittedAt(new Date());
                item.setAktif(Boolean.TRUE);
                applyInput(item, input);
                auditFields(item, actor);
                session.save(item);
                session.flush();
                syncContributorMetadata(session, item, input, actor);
                event(session, item, null, DRAFT, "CREATE_DRAFT", "", actor, requestId);
                return item;
            }
        });
    }

    public RepoItem saveDraft(final DraftInput input, final Tbmuser actor, final String requestId) {
        requireLogin(actor);
        if (input == null || input.id == null) return createDraft(input, actor, requestId);
        return write(new Work<RepoItem>() {
            public RepoItem run(Session session) {
                RepoItem item = loadRequired(session, input.id);
                requireOwnerOrAdmin(item, actor);
                requireEditable(item);
                verifyVersion(item, input.expectedVersion);
                ensureCollectionAcceptsDeposit(session, input.collectionId);
                applyInput(item, input);
                auditFields(item, actor);
                session.update(item);
                syncContributorMetadata(session, item, input, actor);
                event(session, item, item.getWorkflowStatus(), item.getWorkflowStatus(), "AUTOSAVE", "", actor, requestId);
                return item;
            }
        });
    }

    public RepoItem submit(final Long itemId, final Long expectedVersion, final Tbmuser actor,
            final String comment, final String requestId) {
        return transition(itemId, expectedVersion, actor, comment, requestId, "SUBMIT",
                new String[] { DRAFT, REVISION_REQUIRED }, SUBMITTED, true, false);
    }

    public RepoItem resubmit(Long itemId, Long expectedVersion, Tbmuser actor, String comment, String requestId) {
        return transition(itemId, expectedVersion, actor, comment, requestId, "RESUBMIT",
                new String[] { REVISION_REQUIRED }, SUBMITTED, true, false);
    }

    public RepoItem claim(final Long itemId, final Long expectedVersion, final Tbmuser actor,
            final String comment, final String requestId) {
        requireReviewer(actor);
        return write(new Work<RepoItem>() {
            public RepoItem run(Session session) {
                RepoItem item = loadRequired(session, itemId);
                verifyVersion(item, expectedVersion);
                if (IN_REVIEW.equals(item.getWorkflowStatus()) && actor.getUserId().equals(item.getAssignedReviewerId())) return item;
                requireTransition(item.getWorkflowStatus(), IN_REVIEW);
                String from = item.getWorkflowStatus();
                item.setWorkflowStatus(IN_REVIEW);
                item.setAssignedReviewerId(actor.getUserId());
                auditFields(item, actor);
                session.update(item);
                event(session, item, from, IN_REVIEW, "CLAIM", comment, actor, requestId);
                return item;
            }
        });
    }

    public RepoItem returnForRevision(Long id, Long version, Tbmuser actor, String comment, String requestId) {
        requireNonEmpty(comment, "Komentar revisi wajib diisi.");
        return transition(id, version, actor, comment, requestId, "RETURN", new String[] { IN_REVIEW }, REVISION_REQUIRED, false, true);
    }

    public RepoItem reject(Long id, Long version, Tbmuser actor, String comment, String requestId) {
        requireNonEmpty(comment, "Alasan penolakan wajib diisi.");
        return transition(id, version, actor, comment, requestId, "REJECT", new String[] { IN_REVIEW }, REJECTED, false, true);
    }

    public RepoItem approve(Long id, Long version, Tbmuser actor, String comment, String requestId) {
        return transition(id, version, actor, comment, requestId, "APPROVE", new String[] { IN_REVIEW }, APPROVED, false, true);
    }

    public RepoItem publish(final Long id, final Long version, final Tbmuser actor, final String comment,
            final String requestId) {
        requireReviewer(actor);
        return write(new Work<RepoItem>() {
            public RepoItem run(Session session) {
                RepoItem item = loadRequired(session, id);
                verifyVersion(item, version);
                if (PUBLISHED.equals(item.getWorkflowStatus())) return item;
                requireTransition(item.getWorkflowStatus(), PUBLISHED);
                ValidationResult validation = validateForSubmit(session, item);
                if (!validation.valid) throw new IllegalArgumentException(join(validation.errors));
                String from = item.getWorkflowStatus();
                Date now = new Date();
                item.setWorkflowStatus(PUBLISHED);
                item.setSyncStatus("PUBLISHED");
                item.setPublishedAt(now);
                if (item.getIssuedAt() == null) item.setIssuedAt(now);
                item.setLastSyncAt(now);
                item.setIsWithdrawn(Boolean.FALSE);
                if (blank(item.getOaiIdentifier())) item.setOaiIdentifier("oai:ais:repository:" + item.getId());
                if (blank(item.getSlug())) item.setSlug(slug(item.getTitle(), item.getId()));
                auditFields(item, actor);
                session.update(item);
                event(session, item, from, PUBLISHED, "PUBLISH", comment, actor, requestId);
                return item;
            }
        });
    }

    public RepoItem withdraw(final Long id, final Long version, final Tbmuser actor, final String reason,
            final String requestId) {
        requireReviewer(actor);
        requireNonEmpty(reason, "Alasan penarikan wajib diisi.");
        return write(new Work<RepoItem>() {
            public RepoItem run(Session session) {
                RepoItem item = loadRequired(session, id);
                verifyVersion(item, version);
                if (WITHDRAWN.equals(item.getWorkflowStatus())) return item;
                requireTransition(item.getWorkflowStatus(), WITHDRAWN);
                String from = item.getWorkflowStatus();
                item.setWorkflowStatus(WITHDRAWN);
                item.setIsWithdrawn(Boolean.TRUE);
                item.setWithdrawnAt(new Date());
                item.setWithdrawalReason(clean(reason));
                auditFields(item, actor);
                session.update(item);
                event(session, item, from, WITHDRAWN, "WITHDRAW", reason, actor, requestId);
                return item;
            }
        });
    }

    public RepoItem restore(final Long id, final Long version, final Tbmuser actor, final String comment,
            final String requestId) {
        requireReviewer(actor);
        return write(new Work<RepoItem>() {
            public RepoItem run(Session session) {
                RepoItem item = loadRequired(session, id);
                verifyVersion(item, version);
                requireTransition(item.getWorkflowStatus(), PUBLISHED);
                item.setWorkflowStatus(PUBLISHED);
                item.setIsWithdrawn(Boolean.FALSE);
                item.setWithdrawnAt(null);
                item.setWithdrawalReason(null);
                auditFields(item, actor);
                session.update(item);
                event(session, item, WITHDRAWN, PUBLISHED, "RESTORE", comment, actor, requestId);
                return item;
            }
        });
    }

    public void comment(final Long id, final Tbmuser actor, final String comment, final String requestId) {
        requireLogin(actor);
        requireNonEmpty(comment, "Komentar wajib diisi.");
        write(new Work<Object>() {
            public Object run(Session session) {
                RepoItem item = loadRequired(session, id);
                requireOwnerReviewerOrAdmin(item, actor);
                event(session, item, item.getWorkflowStatus(), item.getWorkflowStatus(), "COMMENT", comment, actor, requestId);
                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    public List<RepoItem> myDeposits(final Tbmuser actor, final int maximum) {
        requireLogin(actor);
        return read(new Work<List<RepoItem> >() {
            public List<RepoItem> run(Session session) {
                return session.createCriteria(RepoItem.class).add(Restrictions.eq("ownerId", actor.getUserId()))
                        .add(Restrictions.eq("aktif", Boolean.TRUE)).addOrder(Order.desc("tanggal_dirubah"))
                        .setMaxResults(limit(maximum)).list();
            }
        });
    }

    @SuppressWarnings("unchecked")
    public List<RepoItem> reviewQueue(final Tbmuser actor, final int maximum) {
        requireReviewer(actor);
        return read(new Work<List<RepoItem> >() {
            public List<RepoItem> run(Session session) {
                return session.createCriteria(RepoItem.class)
                        .add(Restrictions.in("workflowStatus", new String[] { SUBMITTED, IN_REVIEW, APPROVED }))
                        .add(Restrictions.eq("aktif", Boolean.TRUE)).addOrder(Order.asc("submittedAt"))
                        .setMaxResults(limit(maximum)).list();
            }
        });
    }

    @SuppressWarnings("unchecked")
    public List<RepoWorkflowEvent> history(final Long itemId, final Tbmuser actor) {
        requireLogin(actor);
        return read(new Work<List<RepoWorkflowEvent> >() {
            public List<RepoWorkflowEvent> run(Session session) {
                RepoItem item = loadRequired(session, itemId);
                requireOwnerReviewerOrAdmin(item, actor);
                return session.createCriteria(RepoWorkflowEvent.class).add(Restrictions.eq("itemId", itemId))
                        .addOrder(Order.desc("createdAt")).addOrder(Order.desc("id")).list();
            }
        });
    }

    @SuppressWarnings("unchecked")
    public List<DuplicateCandidate> duplicates(final DraftInput input, final int maximum) {
        if (input == null || blank(input.title)) return Collections.emptyList();
        return read(new Work<List<DuplicateCandidate> >() {
            public List<DuplicateCandidate> run(Session session) {
                Criteria criteria = session.createCriteria(RepoItem.class)
                        .add(Restrictions.ilike("title", clean(input.title)))
                        .add(Restrictions.eq("aktif", Boolean.TRUE));
                if (input.id != null) criteria.add(Restrictions.ne("id", input.id));
                List<RepoItem> rows = criteria.setMaxResults(limit(maximum)).list();
                List<DuplicateCandidate> result = new ArrayList<DuplicateCandidate>();
                for (RepoItem row : rows) {
                    DuplicateCandidate d = new DuplicateCandidate();
                    d.id = row.getId(); d.title = row.getTitle(); d.authors = row.getAuthors();
                    d.workflowStatus = row.getWorkflowStatus(); result.add(d);
                }
                return result;
            }
        });
    }

    public ValidationResult validateForSubmit(final Long id, final Tbmuser actor) {
        requireLogin(actor);
        return read(new Work<ValidationResult>() {
            public ValidationResult run(Session session) {
                RepoItem item = loadRequired(session, id);
                requireOwnerOrAdmin(item, actor);
                return validateForSubmit(session, item);
            }
        });
    }

    public RepoItem workspaceItem(final Long id, final Tbmuser actor) {
        requireLogin(actor);
        return read(new Work<RepoItem>() {
            public RepoItem run(Session session) {
                RepoItem item = loadRequired(session, id);
                requireOwnerOrAdmin(item, actor);
                return item;
            }
        });
    }

    public RepoItem reviewItem(final Long id, final Tbmuser actor) {
        requireReviewer(actor);
        return read(new Work<RepoItem>() {
            public RepoItem run(Session session) { return loadRequired(session, id); }
        });
    }

    @SuppressWarnings("unchecked")
    public String authorOrcids(final Long id, final Tbmuser actor) {
        requireLogin(actor);
        return read(new Work<String>() {
            public String run(Session session) {
                RepoItem item = loadRequired(session, id); requireOwnerReviewerOrAdmin(item, actor);
                List<RepoItemMetadata> rows = session.createCriteria(RepoItemMetadata.class)
                        .add(Restrictions.eq("itemId", id)).add(Restrictions.eq("metadataField", "repository.author.orcid"))
                        .add(Restrictions.eq("aktif", Boolean.TRUE)).addOrder(Order.asc("place")).list();
                StringBuilder value = new StringBuilder();
                for (RepoItemMetadata row : rows) { if (value.length() > 0) value.append('\n'); value.append(row.getMetadataValue()); }
                return value.toString();
            }
        });
    }

    private RepoItem transition(final Long itemId, final Long expectedVersion, final Tbmuser actor,
            final String comment, final String requestId, final String action, final String[] allowedFrom,
            final String target, final boolean ownerAction, final boolean reviewerAction) {
        requireLogin(actor);
        if (reviewerAction) requireReviewer(actor);
        return write(new Work<RepoItem>() {
            public RepoItem run(Session session) {
                RepoItem item = loadRequired(session, itemId);
                if (ownerAction) requireOwnerOrAdmin(item, actor);
                if (target.equals(item.getWorkflowStatus())) return item;
                verifyVersion(item, expectedVersion);
                if (!Arrays.asList(allowedFrom).contains(item.getWorkflowStatus()))
                    throw new IllegalStateException("Aksi " + action + " tidak valid dari status " + item.getWorkflowStatus() + ".");
                requireTransition(item.getWorkflowStatus(), target);
                if (SUBMITTED.equals(target)) {
                    ValidationResult validation = validateForSubmit(session, item);
                    if (!validation.valid) throw new IllegalArgumentException(join(validation.errors));
                    item.setSubmittedAt(new Date());
                    item.setAssignedReviewerId(null);
                }
                String from = item.getWorkflowStatus();
                item.setWorkflowStatus(target);
                auditFields(item, actor);
                session.update(item);
                event(session, item, from, target, action, comment, actor, requestId);
                return item;
            }
        });
    }

    private ValidationResult validateForSubmit(Session session, RepoItem item) {
        ValidationResult result = new ValidationResult();
        if (item.getCollectionId() == null) result.errors.add("Koleksi wajib dipilih.");
        if (blank(item.getTitle())) result.errors.add("Judul wajib diisi.");
        if (blank(item.getAuthors())) result.errors.add("Minimal satu penulis wajib diisi.");
        if (blank(item.getDocumentType())) result.errors.add("Jenis dokumen wajib diisi.");
        if (!ACCESS_POLICIES.contains(item.getAccessPolicy())) result.errors.add("Kebijakan akses tidak valid.");
        if (!"METADATA_ONLY".equals(item.getAccessPolicy()) && blank(item.getLicenseUri()))
            result.errors.add("Lisensi wajib dipilih untuk berkas yang didistribusikan.");
        if ("EMBARGOED".equals(item.getAccessPolicy()) && item.getEmbargoUntil() == null)
            result.errors.add("Tanggal akhir embargo wajib diisi.");
        if (item.getEmbargoUntil() != null && item.getEmbargoUntil().before(new Date()))
            result.errors.add("Tanggal embargo harus berada di masa depan.");
        if (!"METADATA_ONLY".equals(item.getAccessPolicy())) {
            Number files = (Number) session.createCriteria(RepoBitstream.class)
                    .add(Restrictions.eq("itemId", item.getId())).add(Restrictions.eq("aktif", Boolean.TRUE))
                    .add(Restrictions.eq("primaryFile", Boolean.TRUE)).setProjection(Projections.rowCount()).uniqueResult();
            if (files == null || files.longValue() == 0L) result.errors.add("Berkas utama wajib tersedia.");
        }
        result.valid = result.errors.isEmpty();
        return result;
    }

    private void applyInput(RepoItem item, DraftInput input) {
        if (input == null) throw new IllegalArgumentException("Data deposit tidak tersedia.");
        item.setCollectionId(input.collectionId);
        item.setTitle(limit(input.title, 4000));
        item.setAuthors(limit(input.authors, 12000));
        item.setAbstractText(limit(input.abstractText, 40000));
        item.setSubjects(limit(input.subjects, 12000));
        item.setPublisher(limit(input.publisher, 255));
        item.setLanguage(blank(input.language) ? "id" : limit(input.language, 30));
        item.setDocumentType(blank(input.documentType) ? "Other" : limit(input.documentType, 80));
        String access = clean(input.accessPolicy).toUpperCase();
        if (!ACCESS_POLICIES.contains(access)) access = "METADATA_ONLY";
        item.setAccessPolicy(access);
        item.setLicenseUri(limit(input.licenseUri, 500));
        item.setEmbargoUntil(input.embargoUntil);
        item.setDoi(limit(input.doi, 255));
        validateOrcids(input.authorOrcids);
    }

    @SuppressWarnings("unchecked")
    private void syncContributorMetadata(Session session, RepoItem item, DraftInput input, Tbmuser actor) {
        List<RepoItemMetadata> existing = session.createCriteria(RepoItemMetadata.class)
                .add(Restrictions.eq("itemId", item.getId()))
                .add(Restrictions.in("metadataField", new String[] { "dc.contributor.author", "repository.author.orcid" }))
                .add(Restrictions.eq("aktif", Boolean.TRUE)).list();
        for (RepoItemMetadata old : existing) { old.setAktif(Boolean.FALSE); old.setOlehId(actor.getUserId()); old.setOleh(actor.toString()); session.update(old); }
        String[] authors = lines(input.authors);
        String[] orcids = lines(input.authorOrcids);
        for (int i = 0; i < authors.length; i++) {
            if (blank(authors[i])) continue;
            metadata(session, item.getId(), "dc.contributor.author", authors[i], i, actor);
            if (i < orcids.length && !blank(orcids[i])) metadata(session, item.getId(), "repository.author.orcid", normalizeOrcid(orcids[i]), i, actor);
        }
    }

    private void metadata(Session session, Long itemId, String field, String value, int place, Tbmuser actor) {
        RepoItemMetadata row = new RepoItemMetadata(); row.setItemId(itemId); row.setMetadataField(field);
        row.setMetadataValue(clean(value)); row.setPlace(Integer.valueOf(place)); row.setLanguage("id");
        row.setAktif(Boolean.TRUE); row.setOlehId(actor.getUserId()); row.setOleh(actor.toString()); session.save(row);
    }

    private void validateOrcids(String value) {
        String[] rows = lines(value);
        for (String row : rows) if (!blank(row) && !validOrcid(normalizeOrcid(row)))
            throw new IllegalArgumentException("ORCID tidak valid: " + clean(row));
    }

    public boolean validOrcid(String value) {
        String digits = normalizeOrcid(value).replace("-", "");
        if (!digits.matches("[0-9]{15}[0-9X]")) return false;
        int total = 0;
        for (int i = 0; i < 15; i++) total = (total + (digits.charAt(i) - '0')) * 2;
        int remainder = total % 11;
        int result = (12 - remainder) % 11;
        char check = result == 10 ? 'X' : (char) ('0' + result);
        return check == digits.charAt(15);
    }

    private static String normalizeOrcid(String value) {
        String v = clean(value).replace("https://orcid.org/", "").replace("http://orcid.org/", "").toUpperCase();
        String raw = v.replaceAll("[^0-9X]", "");
        if (raw.length() != 16) return v;
        return raw.substring(0,4)+"-"+raw.substring(4,8)+"-"+raw.substring(8,12)+"-"+raw.substring(12);
    }

    private static String[] lines(String value) {
        String v = clean(value); return v.length() == 0 ? new String[0] : v.split("\\r?\\n");
    }

    private void ensureCollectionAcceptsDeposit(Session session, Long id) {
        if (id == null) throw new IllegalArgumentException("Koleksi wajib dipilih.");
        RepoCollection collection = (RepoCollection) session.get(RepoCollection.class, id);
        if (collection == null || !Boolean.TRUE.equals(collection.getAktif()))
            throw new IllegalArgumentException("Koleksi tidak ditemukan atau tidak aktif.");
        if (!Boolean.TRUE.equals(collection.getDepositEnabled()))
            throw new IllegalStateException("Koleksi tidak menerima deposit baru.");
    }

    private void requireEditable(RepoItem item) {
        if (!DRAFT.equals(item.getWorkflowStatus()) && !REVISION_REQUIRED.equals(item.getWorkflowStatus()))
            throw new IllegalStateException("Item tidak dapat diedit pada status " + item.getWorkflowStatus() + ".");
    }

    private void verifyVersion(RepoItem item, Long expected) {
        if (expected != null && !expected.equals(item.getLockVersion()))
            throw new IllegalStateException("Record telah berubah. Muat ulang sebelum menyimpan kembali.");
    }

    private void requireOwnerOrAdmin(RepoItem item, Tbmuser actor) {
        if (!actor.getUserId().equals(item.getOwnerId()) && !isRepositoryAdmin(actor))
            throw new SecurityException("Item bukan milik pengguna aktif.");
    }

    private void requireOwnerReviewerOrAdmin(RepoItem item, Tbmuser actor) {
        if (actor.getUserId().equals(item.getOwnerId()) || actor.getUserId().equals(item.getAssignedReviewerId())
                || isRepositoryAdmin(actor)) return;
        throw new SecurityException("Pengguna tidak berhak melihat aktivitas item.");
    }

    public boolean isRepositoryAdmin(Tbmuser user) {
        if (user == null) return false;
        try {
            Tbmrole role = user.hakAkses();
            return role != null && Boolean.TRUE.equals(role.getDasborRepository());
        } catch (Exception e) {
            return false;
        }
    }

    private void requireReviewer(Tbmuser actor) {
        requireLogin(actor);
        if (!isRepositoryAdmin(actor)) throw new SecurityException("Hak reviewer repository diperlukan.");
    }

    private void requireLogin(Tbmuser actor) {
        if (actor == null || blank(actor.getUserId())) throw new SecurityException("Login diperlukan.");
    }

    private void requireTransition(String from, String to) {
        Set<String> allowed = TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to))
            throw new IllegalStateException("Transisi " + from + " ke " + to + " tidak diizinkan.");
    }

    private RepoItem loadRequired(Session session, Long id) {
        if (id == null) throw new IllegalArgumentException("ID item wajib diisi.");
        RepoItem item = (RepoItem) session.get(RepoItem.class, id);
        if (item == null || !Boolean.TRUE.equals(item.getAktif())) throw new IllegalArgumentException("Item tidak ditemukan.");
        return item;
    }

    private void event(Session session, RepoItem item, String from, String to, String action, String comment,
            Tbmuser actor, String requestId) {
        RepoWorkflowEvent event = new RepoWorkflowEvent();
        event.setItemId(item.getId()); event.setFromStatus(from); event.setToStatus(to); event.setAction(action);
        event.setCommentText(limit(comment, 10000)); event.setActorId(actor.getUserId());
        event.setActorName(limit(actor.toString(), 500)); event.setRequestId(limit(requestId, 100));
        event.setCreatedAt(new Date()); session.save(event);
    }

    private void auditFields(RepoItem item, Tbmuser actor) {
        item.setOlehId(actor.getUserId()); item.setOleh(actor.toString()); item.setTanggal_dirubah(new Date());
    }

    private interface Work<T> { T run(Session session); }

    private <T> T read(Work<T> work) {
        Session session = HibernateUtil.openSession();
        try { return work.run(session); }
        finally { HibernateUtil.closeSessionQuietly(session); }
    }

    private <T> T write(Work<T> work) {
        Session session = HibernateUtil.openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            T result = work.run(session);
            transaction.commit();
            return result;
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) try { transaction.rollback(); } catch (Exception ignored) {
                ais.common.ErrorAuditUtil.record(ignored, "RepositoryWorkflowService.rollback");
            }
            throw e;
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    private static Map<String, Set<String> > transitions() {
        Map<String, Set<String> > map = new HashMap<String, Set<String> >();
        map.put(DRAFT, set(SUBMITTED));
        map.put(SUBMITTED, set(IN_REVIEW));
        map.put(IN_REVIEW, set(REVISION_REQUIRED, REJECTED, APPROVED));
        map.put(REVISION_REQUIRED, set(SUBMITTED));
        map.put(APPROVED, set(PUBLISHED));
        map.put(PUBLISHED, set(WITHDRAWN));
        map.put(WITHDRAWN, set(PUBLISHED));
        return Collections.unmodifiableMap(map);
    }

    private static Set<String> set(String... values) { return new HashSet<String>(Arrays.asList(values)); }
    private static int limit(int value) { return value < 1 ? 100 : Math.min(value, 500); }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static boolean blank(String value) { return clean(value).length() == 0; }
    private static String limit(String value, int max) { String s = clean(value); return s.length() > max ? s.substring(0, max) : s; }
    private static void requireNonEmpty(String value, String message) { if (blank(value)) throw new IllegalArgumentException(message); }
    private static String join(List<String> values) {
        StringBuilder b = new StringBuilder();
        for (String value : values) { if (b.length() > 0) b.append(" "); b.append(value); }
        return b.toString();
    }
    private static String slug(String title, Long id) {
        String value = clean(title).toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (value.length() > 80) value = value.substring(0, 80).replaceAll("-$", "");
        return (value.length() == 0 ? "item" : value) + "-" + id;
    }
}
