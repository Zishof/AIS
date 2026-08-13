package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;

import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.GeneralValueObject;
import ais.database.model.kursus.ProdukPeserta;

/** Parity ProdukPesertaAction: laporan kepesertaan kursus bersifat read-only. */
@SuppressWarnings("rawtypes")
public class ProdukPesertaGenericCrudAdapter extends AbstractGenericCrudEntityAdapter<ProdukPeserta>
        implements GenericCrudScopeAdapter {
    public ProdukPeserta createNew(GenericCrudRequestContext context) { return new ProdukPeserta(); }
    public boolean canDelete(ProdukPeserta target, GenericCrudRequestContext context, List reasons) {
        reasons.add("Produk peserta dibentuk oleh transaksi pembelian kursus dan tidak dihapus dari laporan ini.");
        return false;
    }
    public List getNaturalKeyProperties() { List result = new ArrayList(); result.add("kode"); return result; }
    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) { }
}
