package ais.action.master.generic.v2.adapter;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.action.master.generic.v2.GenericCrudValueConverter;
import ais.database.model.GeneralValueObject;

/**
 * Implementasi dasar {@link GenericCrudEntityAdapter} (paket {@code generic/v2}, framework CRUD
 * generik action-layer) yang menyediakan perilaku default aman-secara-default (safe-by-default)
 * untuk setiap hook: tanpa filter tambahan, tanpa validasi tambahan, <b>penghapusan ditolak</b>
 * ({@link GenericCrudException} 403), dan tanpa custom action terdaftar. Adapter entitas konkret
 * cukup meng-override method yang relevan saja (mis. {@code canDelete}/{@code delete} untuk
 * mengizinkan hapus, {@code validateCreate}/{@code validateUpdate} untuk aturan bisnis tambahan).
 * Penerapan nilai form ke entitas ({@code applyCreateValues}/{@code applyUpdateValues}) sudah
 * diimplementasikan generik lewat refleksi Java Bean ({@link #apply}).
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractGenericCrudEntityAdapter<T extends GeneralValueObject>
        implements GenericCrudEntityAdapter<T> {

    /** Default no-op: tidak menambah konfigurasi apa pun pada {@link GenericCrudDefinition}. */
    public void configure(GenericCrudDefinition definition) throws Exception { }
    /** Default no-op: tidak menambah filter kriteria default apa pun. */
    public void applyDefaultFilters(Criteria criteria, GenericCrudRequestContext context) throws Exception { }
    /** Default no-op: tidak ada validasi tambahan saat pembuatan data baru. */
    public void validateCreate(Map values, GenericCrudRequestContext context, List errors) throws Exception { }
    /** Default no-op: tidak ada validasi tambahan saat pembaruan data. */
    public void validateUpdate(T current, Map values, GenericCrudRequestContext context, List errors) throws Exception { }
    /** Menerapkan {@code values} ke {@code target} baru lewat refleksi Java Bean — lihat {@link #apply}. */
    public void applyCreateValues(T target, Map values, GenericCrudRequestContext context) throws Exception { apply(target, values); }
    /** Menerapkan {@code values} ke {@code target} yang sudah ada lewat refleksi Java Bean — lihat {@link #apply}. */
    public void applyUpdateValues(T target, Map values, GenericCrudRequestContext context) throws Exception { apply(target, values); }
    /** Default no-op: tidak ada aksi tambahan sebelum penyimpanan. */
    public void beforeSave(Session session, T target, GenericCrudRequestContext context) throws Exception { }
    /** Default no-op: tidak ada aksi tambahan setelah penyimpanan. */
    public void afterSave(Session session, T target, GenericCrudRequestContext context) throws Exception { }
    /** Default: entitas tidak boleh dihapus lewat CRUD generik ({@code false}). */
    public boolean canDelete(T target, GenericCrudRequestContext context, List reasons) throws Exception { return false; }
    /** Default: selalu menolak penghapusan dengan {@link GenericCrudException} (HTTP 403, kode {@code DELETE_DISABLED}). */
    public void delete(Session session, T target, GenericCrudRequestContext context) throws Exception {
        throw new GenericCrudException(403, "DELETE_DISABLED", "Penghapusan tidak diizinkan untuk entity ini.");
    }
    /** Default: tidak ada kolom kunci alami yang didefinisikan (daftar kosong). */
    public List getNaturalKeyProperties() { return new ArrayList(); }
    /** Default: menolak seluruh custom action dengan {@link GenericCrudResult#error} kode {@code ACTION_NOT_ALLOWED}. */
    public GenericCrudResult executeCustomAction(String key, List ids, Map parameters, GenericCrudRequestContext context) throws Exception {
        return GenericCrudResult.error("ACTION_NOT_ALLOWED", "Custom action tidak terdaftar.");
    }

    /**
     * Menerapkan setiap entri {@code values} yang punya nama sama dengan properti JavaBean pada
     * {@code target} ke setter properti tersebut, dengan nilai dikonversi terlebih dahulu lewat
     * {@link GenericCrudValueConverter#convert} ke tipe properti tujuan. Properti pada
     * {@code values} yang tidak punya setter (read-only) menyebabkan
     * {@link GenericCrudException} (400, {@code READ_ONLY_FIELD}) — mencegah klien mengubah field
     * yang seharusnya tidak bisa diedit lewat form generik.
     */
    private void apply(T target, Map values) throws Exception {
        PropertyDescriptor[] descriptors = Introspector.getBeanInfo(target.getClass()).getPropertyDescriptors();
        for (int i = 0; i < descriptors.length; i++) {
            PropertyDescriptor descriptor = descriptors[i];
            if (!values.containsKey(descriptor.getName())) { continue; }
            Method writer = descriptor.getWriteMethod();
            if (writer == null) { throw new GenericCrudException(400, "READ_ONLY_FIELD", "Field tidak dapat diubah."); }
            Object converted = GenericCrudValueConverter.convert(values.get(descriptor.getName()), descriptor.getPropertyType());
            writer.invoke(target, new Object[] { converted });
        }
    }
}
