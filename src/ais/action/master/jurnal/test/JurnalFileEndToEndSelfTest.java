package ais.action.master.jurnal.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashSet;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalFileService;
import ais.common.JurnalAksesKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.RepoBitstream;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;

/** End-to-end metadata/main plus BLOB/streaming test with exact cleanup. */
public final class JurnalFileEndToEndSelfTest {
    private JurnalFileEndToEndSelfTest() {}

    public static void main(String[] args) throws Exception {
        requireClone("AIS_JURNAL_DB_NAME", "ais");
        requireClone("AIS_JURNAL_STREAMING_DB_NAME", "streaming_ais");
        System.setProperty("javax.persistence.validation.mode", "none");
        byte[] payload = "AIS journal two-database file self-test".getBytes(Charset.forName("UTF-8"));
        Tbmuser actor = admin();
        Session main = HibernateUtil.currentSession();
        Transaction tx = main.beginTransaction();
        Long bitstreamId = null;
        try {
            JurnalPenelitian journal = (JurnalPenelitian) main.createQuery(
                    "from JurnalPenelitian where aktif=true order by id").setMaxResults(1).uniqueResult();
            if (journal == null) throw new IllegalStateException("Fixture jurnal existing tidak tersedia.");
            RepoCollection collection = collection(actor);
            main.save(collection);
            main.flush();
            journal.setRepoCollectionId(collection.getId());
            journal.setTenantKey("self-test");
            main.update(journal);
            RepoItem item = item(collection, actor);
            main.save(item);
            main.flush();

            JurnalFileService service = new JurnalFileService();
            RepoBitstream stored = service.store(item.getId(), "self-test.txt", "text/plain",
                    "SUBMISSION", "MANUSCRIPT", null, new ByteArrayInputStream(payload), payload.length, actor);
            bitstreamId = stored.getId();
            if (!"LINKED".equals(stored.getStorageState()) || stored.getContentRef() == null)
                throw new IllegalStateException("File tidak mencapai state LINKED.");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            service.stream(stored.getId(), actor, output);
            if (!java.util.Arrays.equals(payload, output.toByteArray()))
                throw new IllegalStateException("Payload hasil streaming berbeda.");
            System.out.println("JurnalFileEndToEndSelfTest OK main=rollback streaming=verified+cleanup");
        } finally {
            if (tx.isActive()) tx.rollback();
            HibernateUtil.closeSession();
            if (bitstreamId != null) cleanupBlob(bitstreamId);
            Tbmuser.getUserRoleYgDipakai.remove(actor.getUserId());
        }
        System.exit(0);
    }

    private static void cleanupBlob(Long bitstreamId) throws Exception {
        StreamingHibernateUtil util = StreamingHibernateUtil.getInstance();
        Session session = util.currentSession();
        Transaction tx = session.beginTransaction();
        PreparedStatement find = null, unlink = null, delete = null;
        ResultSet result = null;
        try {
            Connection c = session.connection();
            find = c.prepareStatement("select id,file_content from public.lampiran_jurnal where repo_bitstream_id=? for update");
            find.setLong(1, bitstreamId);
            result = find.executeQuery();
            int rows = 0;
            while (result.next()) {
                rows++;
                long id = result.getLong(1), oid = result.getLong(2);
                unlink = c.prepareStatement("select lo_unlink(?)");
                unlink.setLong(1, oid);
                unlink.execute();
                unlink.close(); unlink = null;
                delete = c.prepareStatement("delete from public.lampiran_jurnal where id=?");
                delete.setLong(1, id);
                delete.executeUpdate();
                delete.close(); delete = null;
            }
            if (rows != 1) throw new IllegalStateException("Cleanup menemukan " + rows + " pasangan BLOB.");
            tx.commit();
        } finally {
            if (tx.isActive()) tx.rollback();
            if (result != null) try { result.close(); } catch (Exception ignored) {}
            if (find != null) try { find.close(); } catch (Exception ignored) {}
            if (unlink != null) try { unlink.close(); } catch (Exception ignored) {}
            if (delete != null) try { delete.close(); } catch (Exception ignored) {}
            util.closeSession();
        }
    }

    private static RepoCollection collection(Tbmuser actor) {
        RepoCollection c = new RepoCollection();
        c.setTenantKey("self-test"); c.setKode("journal-file-self-test"); c.setNama("Journal File Self Test");
        c.setTipe("JOURNAL"); c.setSourceSystem("AIS"); c.setMetadataProfileJson("{\"schemaVersion\":1}");
        c.setWorkflowProfileJson("{\"schemaVersion\":1,\"reviewForms\":[]}");
        c.setAccessPolicyJson("{\"schemaVersion\":1,\"policies\":[{\"policyKey\":\"open\",\"format\":\"OPEN\"}]}");
        c.setDepositEnabled(Boolean.TRUE); c.setAktif(Boolean.TRUE); c.setOlehId(actor.getUserId());
        return c;
    }

    private static RepoItem item(RepoCollection c, Tbmuser actor) {
        RepoItem i = new RepoItem();
        i.setCollectionId(c.getId()); i.setTenantKey(c.getTenantKey()); i.setDocumentType("JOURNAL_SUBMISSION");
        i.setWorkflowStatus("DRAFT"); i.setSyncStatus("DRAFT"); i.setTitle("File self test");
        i.setLanguage("id"); i.setOwnerId(actor.getUserId()); i.setSubmittedAt(new Date()); i.setAktif(Boolean.TRUE);
        i.setViewCount(0L); i.setDownloadCount(0L); i.setVersionNumber(1L); i.setOlehId(actor.getUserId());
        return i;
    }

    private static Tbmuser admin() throws Exception {
        Tbmrole role = new Tbmrole(); role.setRoleId(Tbmrole.ADMINISTRATOR);
        JSONObject access = JurnalAksesKatalog.modelUntukEditor(null);
        for (JurnalAksesKatalog.Entri e : JurnalAksesKatalog.DAFTAR) {
            access.getJSONObject("menu").put(e.kunci, true);
            for (String action : JurnalAksesKatalog.AKSI_CRUD)
                access.getJSONObject("crud").getJSONObject(e.kunci).put(action, true);
        }
        for (String action : JurnalAksesKatalog.AKSI_WORKFLOW)
            access.getJSONObject("workflow").put(action, true);
        role.setJurnalAksesJson(access.toString());
        HashSet<Menu> menus = new HashSet<Menu>();
        for (JurnalAksesKatalog.Entri e : JurnalAksesKatalog.DAFTAR) {
            Menu menu = new Menu(); menu.setId(Long.valueOf(2000000000L + e.child)); menus.add(menu);
        }
        role.setMenus(menus);
        Tbmuser user = new Tbmuser(); user.setUserId("JRN_FILE_SELF_TEST"); user.setUserRole(role);
        Tbmuser.getUserRoleYgDipakai.put(user.getUserId(), role);
        return user;
    }

    private static void requireClone(String key, String baseline) {
        String value = System.getenv(key);
        if (value == null || value.trim().length() == 0 || baseline.equalsIgnoreCase(value.trim()))
            throw new IllegalStateException("Self-test wajib diarahkan ke clone: " + key);
    }
}
