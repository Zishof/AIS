import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;

import ais.action.master.generic.v2.GenericCrudAutoDefinitionFactory;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudRuntimeMetadataVerifier;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * Integration smoke test seluruh model Hibernate utama.
 *
 * READ memakai Criteria sehingga seluruh kolom mapping benar-benar diparsing PostgreSQL.
 * CREATE/UPDATE/DELETE diuji sebagai zero-row SQL di dalam transaksi rollback; tidak ada
 * row, sequence, trigger row-level, atau data bisnis yang berubah.
 *
 * Password tidak boleh ditulis di source/argumen. Gunakan environment AIS_TEST_DB_PASSWORD.
 */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public final class GenericCrudPerModelIntegrationTest {
    private static final List failures = new ArrayList();

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: <hibernate.cfg.xml> <jdbc-url> <db-user>");
        }
        String password = System.getenv("AIS_TEST_DB_PASSWORD");
        if (password == null || password.length() == 0) {
            throw new IllegalStateException("AIS_TEST_DB_PASSWORD wajib diisi.");
        }
        boolean schemaUpdate = "true".equalsIgnoreCase(System.getenv("AIS_TEST_SCHEMA_UPDATE"));

        AnnotationConfiguration configuration = new AnnotationConfiguration();
        configuration.configure(new File(args[0]));
        configuration.setProperty("hibernate.connection.url", args[1]);
        configuration.setProperty("hibernate.connection.username", args[2]);
        configuration.setProperty("hibernate.connection.password", password);
        configuration.setProperty("hbm2ddl.auto", schemaUpdate ? "update" : "none");
        configuration.setProperty("hibernate.hbm2ddl.auto", schemaUpdate ? "update" : "none");
        configuration.setProperty("hibernate.show_sql", "false");
        configuration.setProperty("hibernate.cache.use_second_level_cache", "false");
        configuration.setProperty("hibernate.cache.use_query_cache", "false");
        configuration.setProperty("hibernate.c3p0.min_size", "1");
        configuration.setProperty("hibernate.c3p0.max_size", "3");
        configuration.setProperty("hibernate.c3p0.acquireRetryAttempts", "1");

        SessionFactory factory = configuration.buildSessionFactory();
        System.out.println("SCHEMA_MODE=" + (schemaUpdate ? "update" : "none"));
        installFactory(factory);
        try {
            run(factory);
        } finally {
            try { factory.close(); } catch (Exception ignored) { }
            installFactory(null);
        }
    }

    private static void run(SessionFactory factory) {
        Map all = factory.getAllClassMetadata();
        List names = new ArrayList(all.keySet());
        Collections.sort(names);
        int mappedGvo = 0;
        int fullCrud = 0;
        int readOnly = 0;
        int createEnabled = 0;
        int updateEnabled = 0;
        int deleteEnabled = 0;
        int definitionPassed = 0;
        int readPassed = 0;
        int cudPassed = 0;

        for (Iterator iterator = names.iterator(); iterator.hasNext();) {
            String entityName = String.valueOf(iterator.next());
            ClassMetadata metadata = (ClassMetadata) all.get(entityName);
            Class type;
            try { type = metadata.getMappedClass(EntityMode.POJO); }
            catch (Exception invalid) { failure(entityName, "METADATA_CLASS", invalid); continue; }
            if (type == null || Modifier.isAbstract(type.getModifiers())
                    || !GeneralValueObject.class.isAssignableFrom(type)) continue;
            mappedGvo++;

            GenericCrudDefinition definition;
            try {
                definition = GenericCrudAutoDefinitionFactory.buildAdministrative(
                        "generic", "model_crud", type.getName());
                if (definition == null) throw new IllegalStateException("Definisi Generic CRUD null");
                GenericCrudRuntimeMetadataVerifier.verify(definition);
                if (GenericCrudDefinition.FULL_CRUD.equals(definition.getLifecycleStatus())) fullCrud++;
                else readOnly++;
                if (definition.isCreateEnabled()) createEnabled++;
                if (definition.isUpdateEnabled()) updateEnabled++;
                if (definition.isDeleteEnabled()) deleteEnabled++;
                definitionPassed++;
            } catch (Throwable failure) {
                failure(type.getName(), "DEFINITION", failure);
                continue;
            }
            try { testRead(factory, type); readPassed++; }
            catch (Throwable failure) { failure(type.getName(), "READ", failure); }
            try { testZeroRowCud(factory, metadata); cudPassed++; }
            catch (Throwable failure) { failure(type.getName(), "ZERO_ROW_CUD", failure); }
            if (definitionPassed % 100 == 0) {
                System.out.println("PROGRESS definitions=" + definitionPassed + " read=" + readPassed
                        + " cud=" + cudPassed + " failures=" + failures.size());
            }
        }

        System.out.println("MAPPED_GVO=" + mappedGvo);
        System.out.println("DEFINITION_PASSED=" + definitionPassed);
        System.out.println("READ_PASSED=" + readPassed);
        System.out.println("ZERO_ROW_CUD_PASSED=" + cudPassed);
        System.out.println("FULL_CRUD_MODELS=" + fullCrud);
        System.out.println("READ_ONLY_MODELS=" + readOnly);
        System.out.println("CREATE_ENABLED_MODELS=" + createEnabled);
        System.out.println("UPDATE_ENABLED_MODELS=" + updateEnabled);
        System.out.println("SOFT_DELETE_ENABLED_MODELS=" + deleteEnabled);
        System.out.println("FAILURES=" + failures.size());
        for (int i = 0; i < failures.size(); i++) System.out.println("FAIL " + failures.get(i));
        if (!failures.isEmpty()) System.exit(1);
    }

    private static void testRead(SessionFactory factory, Class type) {
        Session session = factory.openSession();
        try { session.createCriteria(type).setMaxResults(1).list(); }
        finally { close(session); }
    }

    private static void testZeroRowCud(SessionFactory factory, ClassMetadata metadata) throws Exception {
        SessionFactoryImplementor implementor = (SessionFactoryImplementor) factory;
        AbstractEntityPersister persister = (AbstractEntityPersister)
                implementor.getEntityPersister(metadata.getEntityName());
        String table = persister.getTableName();
        String[] idColumns = persister.getIdentifierColumnNames();
        Session session = factory.openSession();
        Transaction transaction = null;
        Statement statement = null;
        try {
            transaction = session.beginTransaction();
            statement = session.connection().createStatement();
            statement.executeUpdate("insert into " + table + " select * from " + table + " where 1=0");
            if (idColumns != null && idColumns.length > 0) {
                statement.executeUpdate("update " + table + " set " + idColumns[0] + "="
                        + idColumns[0] + " where 1=0");
            }
            statement.executeUpdate("delete from " + table + " where 1=0");
        } finally {
            try { if (statement != null) statement.close(); } catch (Exception ignored) { }
            try { if (transaction != null && transaction.isActive()) transaction.rollback(); } catch (Exception ignored) { }
            close(session);
        }
    }

    private static void failure(String entity, String phase, Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        failures.add(entity + " | " + phase + " | " + root.getClass().getName() + " | " + root.getMessage());
    }

    private static void installFactory(SessionFactory factory) throws Exception {
        Field field = HibernateUtil.class.getDeclaredField("FACTORY");
        field.setAccessible(true);
        field.set(null, factory);
    }

    private static void close(Session session) {
        try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { }
    }
}
