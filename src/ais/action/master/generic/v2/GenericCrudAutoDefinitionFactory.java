package ais.action.master.generic.v2;

import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;

import ais.action.master.generic.v2.adapter.GenericCrudAutoEntityAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudExistingActionInvoker;
import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/** Membuat definisi dari metadata Hibernate; tidak pernah menerima nama class dari parameter HTTP. */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public final class GenericCrudAutoDefinitionFactory {
    private static final String[] BLOCKED_CLASS_TOKENS = new String[] {
        "password", "credential", "token", "secret", "oauth", "session", "login", "captcha",
        "user", "role", "privilege", "permission", "hakakses", "menu",
        "file", "lampiran", "dokumen", "blob", "audit", "revision", "log", "history",
        "queue", "job", "notification", "notifikasi", "webhook", "bank", "rekening", "payment",
        "epsbed"
    };
    private static final String[] AUTO_CREATE_BLOCKED_CLASSES = new String[] {
        "ais.database.model.PembayaranMahasiswa",
        "ais.database.model.TugasPertemuan",
        "ais.database.model.TingkatKesulitanMatakuliah",
        "ais.database.model.akunting.Pajak",
        "ais.database.model.rab.PenggunaanAnggaran"
    };
    private static final String[] BLOCKED_FIELD_TOKENS = new String[] {
        "password", "passwd", "token", "secret", "credential", "salt", "hash", "privatekey",
        "apikey", "accesskey", "refresh", "binary", "content", "isi_file", "path", "filename",
        "filelocation"
    };
    private static final String[] INTERNAL_FIELDS = new String[] {
        "oleh", "olehid", "tanggal_dirubah", "created", "createdat", "updated", "updatedat",
        "deleted", "deletedat", "version", "copydari", "initdata", "dikunci"
    };

    private GenericCrudAutoDefinitionFactory() { }

    public static GenericCrudDefinition build(String module, String page, String[] serverCandidates) throws Exception {
        Class entityClass = selectMappedClass(module, page, serverCandidates);
        return buildForClass(module, page, entityClass, false, null, null, null);
    }

    public static GenericCrudDefinition build(String module, String page, String[] serverCandidates,
            String sourceAction, String[] sourceMethods) throws Exception {
        Class entityClass = selectMappedClass(module, page, serverCandidates);
        return buildForClass(module, page, entityClass, false, null, sourceAction, sourceMethods);
    }

    public static GenericCrudDefinition build(String module, String page, String[] serverCandidates,
            String sourcePackage, String sourceAction, String[] sourceMethods) throws Exception {
        Class entityClass = selectMappedClass(module, page, serverCandidates);
        return buildForClass(module, page, entityClass, false, sourcePackage, sourceAction, sourceMethods);
    }

    /** Admin model browser: key hanya boleh cocok persis dengan mapped GVO Hibernate. */
    public static GenericCrudDefinition buildAdministrative(String module, String page, String mappedEntityKey) throws Exception {
        Class entityClass = findMappedClass(mappedEntityKey);
        Class actionClass = resolveAdministrativeAction(entityClass);
        return buildForClass(module, page, entityClass, true,
                actionClass == null ? null : actionClass.getPackage().getName(),
                actionClass == null ? null : actionClass.getSimpleName(),
                actionClass == null ? null : publicMethodNames(actionClass));
    }

    public static List listAdministrativeModels() {
        List result = new ArrayList();
        Map all = HibernateUtil.getSessionFactory().getAllClassMetadata();
        Iterator values = all.values().iterator();
        while (values.hasNext()) {
            ClassMetadata metadata = (ClassMetadata) values.next();
            Class mapped;
            try { mapped = metadata.getMappedClass(EntityMode.POJO); } catch (Exception invalid) { continue; }
            if (mapped == null || !GeneralValueObject.class.isAssignableFrom(mapped)
                    || Modifier.isAbstract(mapped.getModifiers())) continue;
            Map row = new LinkedHashMap();
            row.put("entityKey", mapped.getName()); row.put("displayName", humanize(mapped.getSimpleName()));
            row.put("packageName", mapped.getPackage().getName()); row.put("tableName", metadata.getEntityName());
            row.put("restricted", Boolean.valueOf(isBlockedClass(mapped)));
            row.put("mode", isBlockedClass(mapped) ? GenericCrudDefinition.READ_ONLY : GenericCrudDefinition.FULL_CRUD);
            result.add(row);
        }
        Collections.sort(result, new Comparator() {
            public int compare(Object one, Object two) {
                return String.valueOf(((Map) one).get("displayName")).compareToIgnoreCase(String.valueOf(((Map) two).get("displayName")));
            }
        });
        return result;
    }

    private static GenericCrudDefinition buildForClass(String module, String page, Class entityClass,
            boolean administrative, String sourcePackage, String sourceAction, String[] sourceMethods) throws Exception {
        if (entityClass == null) return null;
        ClassMetadata metadata = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
        if (metadata == null || metadata.getIdentifierPropertyName() == null) return null;
        boolean restrictedClass = isBlockedClass(entityClass);
        boolean constructable = hasDefaultConstructor(entityClass);
        boolean assignedGenerator = isAssignedIdentifier(metadata);
        boolean assignedIdentifierSupported = isSupportedScalar(metadata.getIdentifierType().getReturnedClass());
        boolean autoCreatePossible = !restrictedClass && !isAutoCreateBlocked(entityClass) && constructable
                && (!assignedGenerator || assignedIdentifierSupported);
        Class sourceActionClass = resolveSourceAction(sourcePackage, sourceAction);
        boolean actionBacked = GenericCrudExistingActionInvoker.supports(sourceActionClass, entityClass);
        boolean actionCreateBacked = GenericCrudExistingActionInvoker.supportsCreate(sourceActionClass, entityClass);

        GenericCrudDefinition definition = new GenericCrudDefinition();
        definition.setEntityClass(entityClass);
        if (administrative) definition.setEntityKey("admin:" + entityClass.getName());
        definition.setModuleKey(module);
        definition.setPageKey(page);
        definition.setDisplayName(humanize(entityClass.getSimpleName()));
        definition.setSourceActionClassName(sourceActionClass == null ? null : sourceActionClass.getName());
        definition.setExistingActionLifecycleBound(actionBacked);
        definition.setIdentifierProperty(metadata.getIdentifierPropertyName());
        // Jangan percaya daftar nama hasil scanner sebagai otorisasi mutasi.
        // Invoker di atas sudah memverifikasi class, constructor, entity init, dan
        // signature boolean onSave(Event) pada bytecode Action yang benar-benar dimuat.
        boolean actionCreate = actionCreateBacked;
        boolean actionUpdate = actionBacked;
        // Delete lama sering bergantung pada row renderer/checkbox/konfirmasi dan
        // tidak mempunyai kontrak entity tunggal. Jangan menebak soft-delete dari
        // nama metode; adapter eksplisit wajib disediakan sebelum tombol diaktifkan.
        boolean actionDelete = false;
        boolean mutable = !restrictedClass && (actionCreate || actionUpdate || actionDelete);
        definition.setLifecycleStatus(mutable ? GenericCrudDefinition.FULL_CRUD : GenericCrudDefinition.READ_ONLY);
        definition.setEnabled(true);
        definition.setCreateEnabled(autoCreatePossible && actionCreate);
        definition.setUpdateEnabled(!restrictedClass && actionUpdate);
        definition.setImportEnabled(false);
        definition.setExportPdfEnabled(true);
        definition.setExportDocxEnabled(true);
        definition.setExportPptxEnabled(true);
        definition.setAuditEnabled(false);
        definition.setRowAuditEnabled(false);
        definition.setRestoreEnabled(false);
        definition.setAdminDeleteEnabled(false);
        definition.setAutoGenerated(true);
        definition.setAdministrativeAutoCrud(administrative);

        boolean softDelete = hasBooleanProperty(metadata, "aktif");
        definition.setDeleteEnabled(!restrictedClass && softDelete && actionDelete);
        GenericCrudAutoEntityAdapter adapter = GenericCrudReviewedAdapterFactory.create(entityClass,
                softDelete, actionBacked ? sourceActionClass : null, false);
        definition.setAdapter(adapter);
        definition.setScopeAdapter(adapter);

        boolean assignedIdentifier = assignedGenerator && assignedIdentifierSupported;
        GenericCrudFieldDefinition identifier = field(metadata.getIdentifierPropertyName(), "ID",
                metadata.getIdentifierType().getReturnedClass(), !restrictedClass && assignedIdentifier, false, 0);
        identifier.setRequired(!restrictedClass && assignedIdentifier);
        identifier.setTableVisible(true);
        identifier.setSortable(true);
        definition.addField(identifier);

        String[] names = metadata.getPropertyNames();
        Type[] types = metadata.getPropertyTypes();
        int tableCount = 1;
        for (int i = 0; i < names.length; i++) {
            String name = names[i]; Type type = types[i]; Class returned = type.getReturnedClass();
            if (type.isCollectionType() || returned == byte[].class || java.sql.Blob.class.isAssignableFrom(returned)) continue;
            boolean sensitive = isBlockedField(name);
            boolean internal = isInternalField(name);
            if (sensitive || internal) continue;
            boolean nullable = isNullable(metadata, i);
            ClassMetadata relationMetadata = type.isAssociationType()
                    ? HibernateUtil.getSessionFactory().getClassMetadata(returned) : null;
            boolean relation = type.isAssociationType() && GeneralValueObject.class.isAssignableFrom(returned)
                    && relationMetadata != null;
            boolean supported = isSupportedScalar(returned) || returned.isEnum() || relation;
            if (!supported) {
                if (!isInternalField(name) && !nullable) autoCreatePossible = false;
                continue;
            }
            boolean mutableRelation = !relation || isSupportedIdentifier(relationMetadata);
            if (!mutableRelation && !nullable) autoCreatePossible = false;
            if (!nullable && (sensitive || internal) && !isAutoPopulatedInternal(name)) {
                autoCreatePossible = false;
            }
            GenericCrudFieldDefinition field = field(name, humanize(name), returned,
                    !restrictedClass && !sensitive && !internal && mutableRelation,
                    !restrictedClass && !sensitive && !internal && mutableRelation, i + 1);
            field.setSensitive(sensitive);
            field.setReadable(!sensitive);
            field.setExportable(!sensitive);
            field.setTableVisible(!sensitive && tableCount++ < 12);
            field.setSortable(!sensitive && !relation);
            field.setSearchable(!sensitive && returned == String.class);
            field.setQuickFilter(field.isSearchable() && i < 8);
            field.setRequired(!restrictedClass && !sensitive && !internal && mutableRelation && !nullable);
            if (relation) {
                field.setEditorType("relation");
                field.setRelationEntityKey(returned.getName());
                field.setRelationDisplayProperty(chooseDisplayProperty(relationMetadata));
                field.setRelationSearchProperties("kode,nama,nim");
            } else if (returned.isEnum()) {
                Object[] constants = returned.getEnumConstants();
                String[] values = new String[constants == null ? 0 : constants.length];
                for (int e = 0; e < values.length; e++) values[e] = String.valueOf(constants[e]);
                field.setEnumValues(values);
            }
            definition.addField(field);
        }
        pilihKolomTabel(definition);
        String defaultSort = chooseSort(metadata);
        definition.setDefaultSortProperty(defaultSort);
        definition.setVersionProperty(findVersionProperty(metadata));
        definition.setCreateEnabled(autoCreatePossible && actionCreate);
        // Harus paling akhir: adapter hasil review mengunci ulang definisi
        // (READ_ONLY, create/update/delete false) dan penguncian itu tidak boleh
        // tertimpa oleh flag hasil deteksi otomatis di atas.
        adapter.configure(definition);
        return definition;
    }

    /**
     * Memilih kolom tabel berdasarkan MAKNA, bukan urutan properti Hibernate.
     *
     * <p>Sebelumnya 12 properti pertama (urut alfabet) langsung ditandai
     * {@code tableVisible}, sehingga menu auto-generated menampilkan kolom
     * teknis seperti "Auto Create", "Default Prosentase Denda", atau "Denda
     * Akan Berlipat Terlambat Hari" sementara kolom yang dicari pengguna
     * (nama, kode, tanggal, nominal, status) terdorong keluar layar.</p>
     *
     * <p>Skoring memakai kata kunci domain AIS: identitas (nama/kode/nomor/
     * nim/nis/judul/label), waktu (tanggal/bulan/tahun/waktu), nilai uang
     * (nominal/jumlah/biaya/nilai/total/saldo/denda/diskon), status/keterangan,
     * lalu relasi ke entitas pokok. Kolom boolean bertele-tele dan properti
     * konfigurasi mendapat skor rendah sehingga hanya terpakai bila kolom
     * bermakna kurang dari batas. Identifier tetap kolom pertama.</p>
     */
    @SuppressWarnings("unchecked")
    private static void pilihKolomTabel(GenericCrudDefinition definition) {
        final int BATAS = 12;
        java.util.List fields = definition.getFields();
        if (fields == null || fields.isEmpty()) return;

        java.util.List<GenericCrudFieldDefinition> kandidat =
                new java.util.ArrayList<GenericCrudFieldDefinition>();
        for (Object o : fields) {
            GenericCrudFieldDefinition f = (GenericCrudFieldDefinition) o;
            if (f == null) continue;
            if (f.isSensitive()) { f.setTableVisible(false); continue; }
            String prop = f.getProperty() == null ? "" : f.getProperty();
            if (prop.equals(definition.getIdentifierProperty())) {
                f.setTableVisible(true);   // identitas baris tetap tampil
                f.setPosition(0);          // dan selalu jadi kolom pertama
                continue;
            }
            f.setTableVisible(false);
            kandidat.add(f);
        }

        java.util.Collections.sort(kandidat, new java.util.Comparator<GenericCrudFieldDefinition>() {
            public int compare(GenericCrudFieldDefinition a, GenericCrudFieldDefinition b) {
                int beda = skorKolom(b) - skorKolom(a);
                if (beda != 0) return beda;
                return a.getPosition() - b.getPosition();   // stabil: urutan asli
            }
        });

        int terpakai = 1; // identifier
        int urut = 1;
        for (GenericCrudFieldDefinition f : kandidat) {
            if (terpakai >= BATAS) break;
            f.setTableVisible(true);
            // Kolom terpilih juga ditata urutannya: makin bermakna makin kiri,
            // sehingga tabel terbaca tanpa menggulir jauh ke kanan.
            f.setPosition(urut++);
            terpakai++;
        }
        // Sisanya (hanya tampil di form) diletakkan setelah kolom tabel.
        for (GenericCrudFieldDefinition f : kandidat) {
            if (!f.isTableVisible()) f.setPosition(BATAS + urut++);
        }
    }

    /** Skor makna sebuah properti untuk ditampilkan sebagai kolom tabel. */
    private static int skorKolom(GenericCrudFieldDefinition field) {
        String p = normalize(field.getProperty());
        String tipe = field.getEditorType() == null ? "" : field.getEditorType();
        int skor = 0;
        if (p.equals("nama") || p.equals("namasiswa") || p.equals("namalengkap")) skor += 100;
        else if (p.startsWith("nama")) skor += 80;
        if (p.equals("kode") || p.startsWith("kode")) skor += 70;
        if (p.equals("nomorinduk") || p.equals("nim") || p.equals("nis")
                || p.startsWith("nomor") || p.equals("noregistrasi")) skor += 65;
        if (p.equals("judul") || p.equals("label") || p.equals("uraian")) skor += 60;
        String java = field.getJavaType() == null ? "" : field.getJavaType();
        boolean tanggalAsli = java.endsWith("Date") || java.endsWith("Timestamp")
                || java.endsWith("Calendar");
        // Hanya properti bertipe tanggal yang layak bonus waktu; nama seperti
        // tanggalTagihanMengikutiRencanaTahunAkademik justru sebuah flag Boolean.
        if ((p.startsWith("tanggal") && tanggalAsli) || p.equals("waktu") || p.equals("bulan")
                || p.equals("tahun") || p.equals("tahunajaran") || p.equals("tahunakademik")
                || p.equals("semester") || p.equals("smt")) skor += 55;
        if (p.equals("nominal") || p.equals("jumlah") || p.equals("nilai") || p.equals("total")
                || p.equals("saldo") || p.equals("biaya") || p.equals("amount")
                || p.startsWith("nilaibiaya")) skor += 50;
        if (p.equals("denda") || p.equals("diskon") || p.equals("potongan")
                || p.equals("terbayar") || p.equals("kekurangan")
                || p.equals("amountterhutang") || p.equals("pengurangan")) skor += 40;
        // "status" hanya bila memang kolom status; properti seperti
        // statusAwalMahasiswa adalah relasi turunan yang tidak layak
        // mendorong kolom utama keluar layar.
        if (p.equals("status") || p.endsWith("status") || p.equals("keterangan")
                || p.equals("validator")) skor += 35;
        // Kode internal/teknis: berguna untuk sistem, membingungkan di tabel.
        if (p.equals("kodeunik") || p.equals("kodereq") || p.equals("koderequest")
                || p.equals("refnumber") || p.equals("ref")) skor -= 25;
        // Relasi ke entitas pokok lebih informatif daripada flag konfigurasi.
        if ("relation".equals(tipe)) {
            skor += 25;
            if (p.contains("siswa") || p.contains("mahasiswa") || p.contains("sekolah")
                    || p.contains("itembiaya") || p.contains("jenis") || p.contains("akun")
                    || p.contains("kelas") || p.contains("pegawai")
                    || p.contains("kegiatan") || p.contains("pembayaran")
                    || p.contains("prodi") || p.contains("deposit")) skor += 15;
        }
        // Bendera konfigurasi/boolean cenderung tidak informatif di tabel.
        if (java.endsWith("Boolean") || java.equals("boolean")) skor -= 60;
        if (p.startsWith("boleh") || p.startsWith("auto") || p.startsWith("default")
                || p.startsWith("gunakan") || p.startsWith("tampilkan") || p.startsWith("wajib")
                || p.startsWith("harus") || p.startsWith("aktifkan")) skor -= 25;
        if (p.equals("aktif")) skor -= 10;
        return skor;
    }

    private static Class resolveSourceAction(String sourcePackage, String sourceAction) {
        String action = sourceAction == null ? "" : sourceAction.trim();
        if (action.length() == 0 || "null".equalsIgnoreCase(action)) return null;
        String className = action.indexOf('.') >= 0 ? action
                : (sourcePackage == null || sourcePackage.trim().length() == 0
                        || "null".equalsIgnoreCase(sourcePackage.trim())
                        ? action : sourcePackage.trim() + "." + action);
        try {
            Class resolved = Class.forName(className);
            return Modifier.isAbstract(resolved.getModifiers()) ? null : resolved;
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static Class resolveAdministrativeAction(Class entityClass) {
        if (entityClass == null) return null;
        String suffix = entityClass.getPackage().getName().substring("ais.database.model".length());
        String action = entityClass.getSimpleName() + "Action";
        String[] packages = new String[] { "ais.action.master" + suffix, "ais.action.master" };
        for (int i = 0; i < packages.length; i++) {
            Class candidate = resolveSourceAction(packages[i], action);
            if (GenericCrudExistingActionInvoker.supports(candidate, entityClass)) return candidate;
        }
        return null;
    }

    private static String[] publicMethodNames(Class type) {
        java.lang.reflect.Method[] methods = type.getMethods();
        String[] names = new String[methods.length];
        for (int i = 0; i < methods.length; i++) names[i] = methods[i].getName();
        return names;
    }

    public static boolean supports(String[] methods, String[] aliases) {
        if (methods == null || aliases == null) return false;
        for (int i = 0; i < methods.length; i++) {
            String method = normalize(methods[i]);
            for (int a = 0; a < aliases.length; a++) {
                String alias = normalize(aliases[a]);
                if (method.equals(alias) || method.startsWith(alias)) return true;
            }
        }
        return false;
    }

    /**
     * Melengkapi definition eksplisit dengan seluruh properti Hibernate yang aman.
     * Field yang sudah direview manual tetap menang; password, token, blob, collection,
     * dan field audit internal tidak pernah ikut menjadi input browser.
     */
    public static void appendMissingMappedFields(GenericCrudDefinition definition) throws Exception {
        if (definition == null || definition.getEntityClass() == null) return;
        ClassMetadata metadata = HibernateUtil.getSessionFactory().getClassMetadata(definition.getEntityClass());
        if (metadata == null) return;
        String[] names = metadata.getPropertyNames();
        Type[] types = metadata.getPropertyTypes();
        int basePosition = definition.getFields().size() + 100;
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            if (definition.getField(name) != null) continue;
            Type type = types[i]; Class returned = type.getReturnedClass();
            if (type.isCollectionType() || returned == byte[].class
                    || java.sql.Blob.class.isAssignableFrom(returned)) continue;
            boolean sensitive = isBlockedField(name);
            boolean internal = isInternalField(name);
            ClassMetadata relationMetadata = type.isAssociationType()
                    ? HibernateUtil.getSessionFactory().getClassMetadata(returned) : null;
            boolean relation = type.isAssociationType() && GeneralValueObject.class.isAssignableFrom(returned)
                    && relationMetadata != null;
            if (!(isSupportedScalar(returned) || returned.isEnum() || relation)) continue;
            boolean mutableRelation = !relation || isSupportedIdentifier(relationMetadata);
            boolean mutable = mutableRelation;
            GenericCrudFieldDefinition added = field(name, humanize(name), returned,
                    mutable && definition.isCreateEnabled(), mutable && definition.isUpdateEnabled(),
                    basePosition + i);
            added.setSensitive(sensitive);
            added.setReadable(!sensitive);
            added.setExportable(!sensitive);
            added.setTableVisible(false);
            added.setSortable(!sensitive && !relation);
            added.setSearchable(!sensitive && returned == String.class);
            added.setQuickFilter(false);
            added.setRequired(mutable && !isNullable(metadata, i));
            if (relation) {
                added.setEditorType("relation");
                added.setRelationEntityKey(returned.getName());
                added.setRelationDisplayProperty(chooseDisplayProperty(relationMetadata));
                added.setRelationSearchProperties("kode,nama,nim");
            } else if (returned.isEnum()) {
                Object[] constants = returned.getEnumConstants();
                String[] values = new String[constants == null ? 0 : constants.length];
                for (int e = 0; e < values.length; e++) values[e] = String.valueOf(constants[e]);
                added.setEnumValues(values);
            }
            definition.addField(added);
        }
    }

    private static Class findMappedClass(String entityKey) {
        if (entityKey == null || entityKey.length() == 0 || entityKey.indexOf("ais.database.model.") != 0) return null;
        Map all = HibernateUtil.getSessionFactory().getAllClassMetadata();
        Iterator values = all.values().iterator();
        while (values.hasNext()) {
            ClassMetadata metadata = (ClassMetadata) values.next();
            try {
                Class mapped = metadata.getMappedClass(EntityMode.POJO);
                if (mapped != null && entityKey.equals(mapped.getName())
                        && GeneralValueObject.class.isAssignableFrom(mapped)
                        && !Modifier.isAbstract(mapped.getModifiers())) return mapped;
            } catch (Exception ignored) { }
        }
        return null;
    }

    private static Class selectMappedClass(String module, String page, String[] candidates) {
        if (candidates == null || candidates.length == 0) return null;
        Map all = HibernateUtil.getSessionFactory().getAllClassMetadata();
        Class best = null; int bestScore = -1;
        Iterator values = all.values().iterator();
        while (values.hasNext()) {
            ClassMetadata metadata = (ClassMetadata) values.next();
            Class mapped;
            try { mapped = metadata.getMappedClass(EntityMode.POJO); } catch (Exception invalid) { continue; }
            if (mapped == null || !GeneralValueObject.class.isAssignableFrom(mapped) || Modifier.isAbstract(mapped.getModifiers())) continue;
            for (int i = 0; i < candidates.length; i++) {
                if (!mapped.getSimpleName().equals(candidates[i])) continue;
                int score = 10;
                String normalizedClass = normalize(mapped.getSimpleName());
                String normalizedPage = normalize(page);
                if (normalizedClass.equals(normalizedPage)) score += 100;
                else if (normalizedPage.indexOf(normalizedClass) >= 0 || normalizedClass.indexOf(normalizedPage) >= 0) score += 40;
                if (mapped.getPackage().getName().toLowerCase().indexOf("." + String.valueOf(module).toLowerCase()) >= 0) score += 20;
                score -= i;
                if (score > bestScore) { best = mapped; bestScore = score; }
            }
        }
        return best;
    }

    private static GenericCrudFieldDefinition field(String property, String label, Class type,
            boolean createable, boolean updateable, int position) {
        GenericCrudFieldDefinition result = new GenericCrudFieldDefinition(property, label, type.getName());
        result.setCreateable(createable); result.setUpdateable(updateable); result.setPosition(position);
        result.setEditorType(editor(type)); return result;
    }
    private static String editor(Class type) {
        if (type == Boolean.class || type == Boolean.TYPE) return "checkbox";
        if (Number.class.isAssignableFrom(type) || type.isPrimitive()) return "number";
        if (type == java.sql.Time.class) return "time";
        if (type == java.sql.Timestamp.class) return "datetime-local";
        if (Date.class.isAssignableFrom(type)) return "date";
        if (type.isEnum()) return "select";
        return "text";
    }
    private static boolean isSupportedScalar(Class type) {
        return type == String.class || type == Boolean.class || type == Boolean.TYPE
                || Number.class.isAssignableFrom(type) || type == Integer.TYPE || type == Long.TYPE
                || type == Short.TYPE || type == Byte.TYPE || type == Double.TYPE || type == Float.TYPE
                || type == Character.class || type == Character.TYPE || Date.class.isAssignableFrom(type)
                || java.util.UUID.class.isAssignableFrom(type);
    }
    private static boolean hasBooleanProperty(ClassMetadata metadata, String name) {
        try { Class type = metadata.getPropertyType(name).getReturnedClass(); return type == Boolean.class || type == Boolean.TYPE; }
        catch (Exception ignored) { return false; }
    }
    private static String chooseSort(ClassMetadata metadata) {
        try { if (metadata.getPropertyType("nama").getReturnedClass() == String.class) return "nama"; } catch (Exception ignored) { }
        try { if (metadata.getPropertyType("kode").getReturnedClass() == String.class) return "kode"; } catch (Exception ignored) { }
        return metadata.getIdentifierPropertyName();
    }
    private static String findVersionProperty(ClassMetadata metadata) {
        try { int index = metadata.getVersionProperty(); return index < 0 ? null : metadata.getPropertyNames()[index]; }
        catch (Exception ignored) { return null; }
    }
    private static boolean hasDefaultConstructor(Class type) {
        try { type.getDeclaredConstructor(new Class[0]); return true; }
        catch (Exception missing) { return false; }
    }
    private static boolean isSupportedIdentifier(ClassMetadata metadata) {
        return metadata != null && isSupportedScalar(metadata.getIdentifierType().getReturnedClass());
    }
    private static boolean isAssignedIdentifier(ClassMetadata metadata) {
        try {
            Object factory = HibernateUtil.getSessionFactory();
            java.lang.reflect.Method getPersister = factory.getClass().getMethod("getEntityPersister", new Class[] { String.class });
            Object persister = getPersister.invoke(factory, new Object[] { metadata.getEntityName() });
            java.lang.reflect.Method getGenerator = persister.getClass().getMethod("getIdentifierGenerator", new Class[0]);
            Object generator = getGenerator.invoke(persister, new Object[0]);
            return generator != null && generator.getClass().getName().toLowerCase().indexOf("assigned") >= 0;
        } catch (Exception unavailable) { return false; }
    }
    private static boolean isNullable(ClassMetadata metadata, int propertyIndex) {
        try {
            Object factory = HibernateUtil.getSessionFactory();
            java.lang.reflect.Method getPersister = factory.getClass().getMethod("getEntityPersister", new Class[] { String.class });
            Object persister = getPersister.invoke(factory, new Object[] { metadata.getEntityName() });
            java.lang.reflect.Method getNullability = persister.getClass().getMethod("getPropertyNullability", new Class[0]);
            boolean[] nullable = (boolean[]) getNullability.invoke(persister, new Object[0]);
            return nullable == null || propertyIndex < 0 || propertyIndex >= nullable.length || nullable[propertyIndex];
        } catch (Exception unavailable) { return true; }
    }
    private static String chooseDisplayProperty(ClassMetadata metadata) {
        if (metadata == null) return null;
        try { if (metadata.getPropertyType("nama").getReturnedClass() == String.class) return "nama"; } catch (Exception ignored) { }
        try { if (metadata.getPropertyType("userNama").getReturnedClass() == String.class) return "userNama"; } catch (Exception ignored) { }
        try { if (metadata.getPropertyType("roleName").getReturnedClass() == String.class) return "roleName"; } catch (Exception ignored) { }
        try { if (metadata.getPropertyType("kode").getReturnedClass() == String.class) return "kode"; } catch (Exception ignored) { }
        try { if (metadata.getPropertyType("nim").getReturnedClass() == String.class) return "nim"; } catch (Exception ignored) { }
        return metadata.getIdentifierPropertyName();
    }
    private static boolean isBlockedClass(Class type) { return containsToken(type.getName(), BLOCKED_CLASS_TOKENS); }
    private static boolean isAutoCreateBlocked(Class type) {
        String name = type == null ? "" : type.getName();
        for (int i = 0; i < AUTO_CREATE_BLOCKED_CLASSES.length; i++) {
            if (AUTO_CREATE_BLOCKED_CLASSES[i].equals(name)) return true;
        }
        return false;
    }
    private static boolean isBlockedField(String value) { return containsToken(value, BLOCKED_FIELD_TOKENS); }
    private static boolean isInternalField(String value) { return containsToken(normalize(value), INTERNAL_FIELDS); }
    private static boolean isAutoPopulatedInternal(String value) {
        String normalized = normalize(value);
        return "dibuatoleh".equals(normalized) || "diubaholeh".equals(normalized)
                || "validatoruser".equals(normalized) || "oleh".equals(normalized)
                || "olehid".equals(normalized) || "ditetapkanoleh".equals(normalized)
                || "tanggaldirubah".equals(normalized) || "created".equals(normalized)
                || "createdat".equals(normalized) || "updated".equals(normalized)
                || "updatedat".equals(normalized);
    }
    private static boolean containsToken(String value, String[] tokens) {
        String normalized = normalize(value);
        for (int i = 0; i < tokens.length; i++) if (normalized.indexOf(normalize(tokens[i])) >= 0) return true;
        return false;
    }
    private static String normalize(String value) { return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(); }
    private static String humanize(String value) {
        if (value == null) return "Data";
        String spaced = value.replace('_', ' ').replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        return spaced.length() == 0 ? "Data" : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
