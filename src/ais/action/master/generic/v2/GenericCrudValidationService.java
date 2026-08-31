package ais.action.master.generic.v2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Layanan validasi baku framework CRUD generik {@link ais.action.master.generic.v2}: memeriksa
 * bidang wajib isi ({@code required}) yang dideklarasikan pada {@link GenericCrudDefinition}
 * suatu entitas, terlepas dari validasi khusus per entitas yang ditangani masing-masing
 * {@link ais.action.master.generic.v2.adapter.GenericCrudEntityAdapter}.
 */
@SuppressWarnings("rawtypes")
public class GenericCrudValidationService {
    /**
     * Memeriksa seluruh field wajib pada {@code definition} terhadap {@code values}. Untuk
     * update ({@code create=false}), field yang tidak disertakan pada {@code values} (tidak
     * diubah) dilewati; untuk create, seluruh field wajib harus terisi nilai non-kosong.
     *
     * @param definition definisi field entitas, termasuk flag wajib-isi dan label per field
     * @param values     nilai yang akan divalidasi, berupa peta properti ke nilai
     * @param create     {@code true} bila ini operasi pembuatan baru, {@code false} bila update
     * @return daftar pesan galat berformat {@code "properti:label wajib diisi"}, kosong bila valid
     */
    public List validateRequired(GenericCrudDefinition definition, Map values, boolean create) {
        List errors = new ArrayList();
        Iterator iterator = definition.getFields().iterator();
        while (iterator.hasNext()) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) iterator.next();
            if (!field.isRequired() || (!create && !values.containsKey(field.getProperty()))) { continue; }
            Object value = values.get(field.getProperty());
            if (value == null || String.valueOf(value).trim().length() == 0) {
                errors.add(field.getProperty() + ":" + field.getLabel() + " wajib diisi");
            }
        }
        return errors;
    }
}
