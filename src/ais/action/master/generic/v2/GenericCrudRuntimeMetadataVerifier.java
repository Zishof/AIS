package ais.action.master.generic.v2;

import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;

import ais.database.hibernate.HibernateUtil;
import ais.action.master.generic.v2.adapter.GenericCrudQueryProvider;

/** Hibernate runtime metadata adalah sumber kebenaran terakhir sebelum entity dipakai. */
public final class GenericCrudRuntimeMetadataVerifier {
    private GenericCrudRuntimeMetadataVerifier() { }

    public static ClassMetadata verify(GenericCrudDefinition definition) throws GenericCrudException {
        if (definition == null || !definition.isEnabled()) {
            throw new GenericCrudException(404, "ENTITY_DISABLED", "Entity belum diaktifkan atau tidak terdaftar.");
        }
        if (!definition.isGeneralValueObject()) {
            throw new GenericCrudException(409, "INVALID_ENTITY", "Entity bukan GeneralValueObject.");
        }
        ClassMetadata metadata = HibernateUtil.getSessionFactory().getClassMetadata(definition.getEntityClass());
        if (metadata == null) {
            throw new GenericCrudException(409, "METADATA_NOT_FOUND", "Hibernate metadata entity tidak ditemukan.");
        }
        if (metadata.getIdentifierPropertyName() == null) {
            throw new GenericCrudException(409, "IDENTIFIER_NOT_FOUND", "Identifier Hibernate entity tidak ditemukan.");
        }
        if (!metadata.getIdentifierPropertyName().equals(definition.getIdentifierProperty())) {
            throw new GenericCrudException(409, "METADATA_DRIFT", "Konfigurasi identifier tidak sama dengan metadata runtime.");
        }
        java.util.List fields = definition.getFields();
        for (int i = 0; i < fields.size(); i++) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.get(i);
            if (definition.getIdentifierProperty().equals(field.getProperty())) continue;
            Type type;
            try { type = metadata.getPropertyType(field.getProperty()); }
            catch (Exception missing) {
                if (definition.getAdapter() instanceof GenericCrudQueryProvider) continue;
                throw new GenericCrudException(409, "FIELD_METADATA_DRIFT", "Field konfigurasi tidak ada pada metadata runtime: " + field.getProperty());
            }
            Class returned = type.getReturnedClass();
            if (type.isCollectionType() || returned == byte[].class || java.sql.Blob.class.isAssignableFrom(returned)) {
                if (field.isReadable() || field.isCreateable() || field.isUpdateable() || field.isExportable()) {
                    throw new GenericCrudException(409, "UNSAFE_FIELD_CONFIG", "Collection/blob harus excluded: " + field.getProperty());
                }
            }
        }
        return metadata;
    }
}
