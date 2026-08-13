package ais.action.master.generic.v2.adapter;

import java.util.Map;

import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.GeneralValueObject;

/** Sanitasi row yang bergantung pada isi record, misalnya konfigurasi rahasia. */
@SuppressWarnings("rawtypes")
public interface GenericCrudRowSanitizer {
    void sanitizeRow(GeneralValueObject object, Map row,
            GenericCrudRequestContext context) throws Exception;
    boolean isSensitiveProperty(GeneralValueObject object, String property,
            GenericCrudRequestContext context) throws Exception;
}
