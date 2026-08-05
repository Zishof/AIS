package ais.common;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

/**
 * Helper reusable untuk filter pencarian berbasis Combobox ZKoss dan Hibernate
 * Criteria.
 *
 * Kompatibel Java 1.7.
 *
 * Catatan penting:
 * - Comboitem.getValue() sering berisi object entity hasil lookup UI.
 * - Jika object entity tersebut transient/detached dan langsung dipakai pada
 *   Restrictions.eq("sekolah", sekolah), Hibernate dapat melempar
 *   TransientObjectException saat Criteria.list().
 * - Karena itu helper ini otomatis membandingkan property.id dengan value.getId()
 *   jika value memiliki getter getId() yang valid. Contoh:
 *   eqValue("sekolah", sekolahEntity) menjadi Restrictions.eq("sekolah.id", id).
 */
public final class CommonSearchFilterHelper {

    /**
     * Default perilaku jika filter memiliki value:
     * true  = data dengan property null ikut ditampilkan: property is null OR property = value.
     * false = hanya data yang sama persis dengan value.
     */
    public static final boolean TERMASUK_NULL_DEFAULT = true;

    private static final Criterion TANPA_FILTER = Restrictions.sqlRestriction("1=1");
    private static final String ID_PROPERTY = "id";
    private static final String ID_SUFFIX = ".id";

    private CommonSearchFilterHelper() {
    }

    public static Criterion tanpaFilter() {
        return TANPA_FILTER;
    }

    public static Object selectedValue(Combobox combobox) {
        if (combobox == null) {
            return null;
        }
        Comboitem selectedItem = combobox.getSelectedItem();
        if (selectedItem == null) {
            return null;
        }
        return selectedItem.getValue();
    }

    public static boolean hasSelectedValue(Combobox combobox) {
        return selectedValue(combobox) != null;
    }

    public static boolean isNullOrIdNull(Object value) {
        if (value == null) {
            return true;
        }
        if (!hasReadableId(value)) {
            return false;
        }
        return getIdValue(value) == null;
    }

    public static boolean hasId(Object value) {
        return !isNullOrIdNull(value);
    }

    public static Object selectedValueWithId(Combobox combobox) {
        Object value = selectedValue(combobox);
        if (value == null) {
            return null;
        }
        if (hasReadableId(value) && getIdValue(value) == null) {
            return null;
        }
        return value;
    }

    public static boolean hasSelectedValueWithId(Combobox combobox) {
        return selectedValueWithId(combobox) != null;
    }

    public static Criterion eqSelected(String propertyName, Combobox combobox) {
        return eqSelected(propertyName, combobox, TERMASUK_NULL_DEFAULT);
    }

    public static Criterion eqSelected(String propertyName, Combobox combobox, boolean termasukNull) {
        return eqValue(propertyName, selectedValue(combobox), termasukNull);
    }

    public static Criterion eqSelectedWithId(String propertyName, Combobox combobox) {
        return eqSelectedWithId(propertyName, combobox, TERMASUK_NULL_DEFAULT);
    }

    public static Criterion eqSelectedWithId(String propertyName, Combobox combobox, boolean termasukNull) {
        return eqValue(propertyName, selectedValueWithId(combobox), termasukNull);
    }

    public static Criterion eqValue(String propertyName, Object value) {
        return eqValue(propertyName, value, TERMASUK_NULL_DEFAULT);
    }

    public static Criterion eqValue(String propertyName, Object value, boolean termasukNull) {
        if (!hasText(propertyName) || value == null) {
            return tanpaFilter();
        }

        if (hasReadableId(value)) {
            Object idValue = getIdValue(value);
            if (idValue == null) {
                return tanpaFilter();
            }
            return eqEntityIdValue(propertyName, idValue, termasukNull);
        }

        if (isSimpleValue(value)) {
            return eqDirectValue(propertyName, value, termasukNull);
        }

        /*
         * Fallback aman: object kompleks tanpa getId() jangan dipasang sebagai
         * parameter Criteria, karena berpotensi transient dan memicu flush error.
         */
        return tanpaFilter();
    }

    public static Criterion eqIdValue(String propertyName, Object idValue) {
        return eqIdValue(propertyName, idValue, TERMASUK_NULL_DEFAULT);
    }

    public static Criterion eqIdValue(String propertyName, Object idValue, boolean termasukNull) {
        if (!hasText(propertyName) || idValue == null) {
            return tanpaFilter();
        }
        return eqEntityIdValue(propertyName, idValue, termasukNull);
    }

    public static Criteria addEqSelected(Criteria criteria, String propertyName, Combobox combobox) {
        return addEqSelected(criteria, propertyName, combobox, TERMASUK_NULL_DEFAULT);
    }

    public static Criteria addEqSelected(Criteria criteria, String propertyName, Combobox combobox,
            boolean termasukNull) {
        if (criteria == null) {
            return null;
        }
        return criteria.add(eqSelected(propertyName, combobox, termasukNull));
    }

    public static Criteria addEqSelectedWithId(Criteria criteria, String propertyName, Combobox combobox) {
        return addEqSelectedWithId(criteria, propertyName, combobox, TERMASUK_NULL_DEFAULT);
    }

    public static Criteria addEqSelectedWithId(Criteria criteria, String propertyName, Combobox combobox,
            boolean termasukNull) {
        if (criteria == null) {
            return null;
        }
        return criteria.add(eqSelectedWithId(propertyName, combobox, termasukNull));
    }

    public static Criteria addFakultasJurusan(Criteria criteria, Combobox searchfakultas, Combobox searchjurusan) {
        return addFakultasJurusan(criteria, "fakultas", searchfakultas, "jurusan", searchjurusan,
                TERMASUK_NULL_DEFAULT);
    }

    public static Criteria addFakultasJurusan(Criteria criteria, String fakultasProperty, Combobox searchfakultas,
            String jurusanProperty, Combobox searchjurusan, boolean termasukNull) {
        if (criteria == null) {
            return null;
        }
        criteria.add(eqSelectedWithId(jurusanProperty, searchjurusan, termasukNull));
        criteria.add(eqSelectedWithId(fakultasProperty, searchfakultas, termasukNull));
        return criteria;
    }

    public static Criteria addYayasanSekolah(Criteria criteria, Combobox searchyayasan, Combobox searchsekolah) {
        return addYayasanSekolah(criteria, "yayasan", searchyayasan, "sekolah", searchsekolah,
                TERMASUK_NULL_DEFAULT);
    }

    public static Criteria addYayasanSekolah(Criteria criteria, String yayasanProperty, Combobox searchyayasan,
            String sekolahProperty, Combobox searchsekolah, boolean termasukNull) {
        if (criteria == null) {
            return null;
        }
        criteria.add(eqSelectedWithId(sekolahProperty, searchsekolah, termasukNull));
        criteria.add(eqSelectedWithId(yayasanProperty, searchyayasan, termasukNull));
        return criteria;
    }



    public static Criteria addFakultasJurusanYayasanSekolah(Criteria criteria, Combobox searchfakultas,
            Combobox searchjurusan, Combobox searchyayasan, Combobox searchsekolah) {
        return addRepositoryUnitFilters(criteria, searchfakultas, searchjurusan, searchyayasan, searchsekolah);
    }

    public static Criteria addFakultasJurusanYayasanSekolah(Criteria criteria, Combobox searchfakultas,
            Combobox searchjurusan, Combobox searchyayasan, Combobox searchsekolah, boolean termasukNull) {
        return addRepositoryUnitFilters(criteria, "fakultas", searchfakultas, "jurusan", searchjurusan, "yayasan",
                searchyayasan, "sekolah", searchsekolah, termasukNull);
    }

    public static Criteria addRepositoryUnitFilters(Criteria criteria, Combobox searchfakultas,
            Combobox searchjurusan, Combobox searchyayasan, Combobox searchsekolah) {
        return addRepositoryUnitFilters(criteria, "fakultas", searchfakultas, "jurusan", searchjurusan, "yayasan",
                searchyayasan, "sekolah", searchsekolah, TERMASUK_NULL_DEFAULT);
    }

    public static Criteria addRepositoryUnitFilters(Criteria criteria, String fakultasProperty,
            Combobox searchfakultas, String jurusanProperty, Combobox searchjurusan, String yayasanProperty,
            Combobox searchyayasan, String sekolahProperty, Combobox searchsekolah, boolean termasukNull) {
        if (criteria == null) {
            return null;
        }
        criteria.add(eqSelectedWithId(jurusanProperty, searchjurusan, termasukNull));
        criteria.add(eqSelectedWithId(fakultasProperty, searchfakultas, termasukNull));
        criteria.add(eqSelectedWithId(sekolahProperty, searchsekolah, termasukNull));
        criteria.add(eqSelectedWithId(yayasanProperty, searchyayasan, termasukNull));
        return criteria;
    }

    private static Criterion eqDirectValue(String propertyName, Object value, boolean termasukNull) {
        if (termasukNull) {
            return Restrictions.or(Restrictions.isNull(propertyName), Restrictions.eq(propertyName, value));
        }
        return Restrictions.eq(propertyName, value);
    }

    private static Criterion eqEntityIdValue(String propertyName, Object idValue, boolean termasukNull) {
        String idPropertyName = toIdPropertyName(propertyName);
        if (termasukNull) {
            return Restrictions.or(Restrictions.isNull(toNullCheckPropertyName(propertyName)),
                    Restrictions.eq(idPropertyName, idValue));
        }
        return Restrictions.eq(idPropertyName, idValue);
    }

    private static String toIdPropertyName(String propertyName) {
        String name = propertyName == null ? "" : propertyName.trim();
        if (ID_PROPERTY.equals(name) || name.endsWith(ID_SUFFIX)) {
            return name;
        }
        return name + ID_SUFFIX;
    }

    private static String toNullCheckPropertyName(String propertyName) {
        String name = propertyName == null ? "" : propertyName.trim();
        if (name.endsWith(ID_SUFFIX)) {
            return name.substring(0, name.length() - ID_SUFFIX.length());
        }
        return name;
    }

    private static boolean isSimpleValue(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Date
                || value instanceof Character || value instanceof Enum || value instanceof BigDecimal
                || value instanceof BigInteger;
    }

    private static boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private static boolean hasReadableId(Object value) {
        if (value == null || isSimpleValue(value)) {
            return false;
        }
        try {
            value.getClass().getMethod("getId", new Class[0]);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Object getIdValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            Method method = value.getClass().getMethod("getId", new Class[0]);
            return method.invoke(value, new Object[0]);
        } catch (Exception e) {
            return null;
        }
    }
}
