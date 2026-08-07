package ais.action.master.generic.v2;

import org.hibernate.metadata.ClassMetadata;

import ais.database.hibernate.HibernateUtil;

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
        return metadata;
    }
}
