package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.GeneralValueObject;
import ais.database.model.PenumumanWebsite;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;

/** Business-rule parity untuk PenumumanWebsiteAction tanpa merender ZUL. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class PenumumanWebsiteGenericCrudAdapter extends GenericCrudAutoEntityAdapter {
    public PenumumanWebsiteGenericCrudAdapter() {
        // Action existing hanya menyediakan tambah dan ubah; tidak ada delete.
        super(PenumumanWebsite.class, false, null, true);
    }

    public void configure(GenericCrudDefinition definition) {
        definition.setDisplayName("Pengumuman Website");
        definition.setDefaultSortProperty("tanggal");
        definition.setDefaultSortAscending(false);
        definition.setDefaultPageSize(25);
        definition.setMaxPageSize(200);
        definition.setDeleteEnabled(false);
        definition.setImportEnabled(false);
    }

    public GeneralValueObject createNew(GenericCrudRequestContext context) throws Exception {
        PenumumanWebsite value = (PenumumanWebsite) super.createNew(context);
        value.setTanggal(new Date());
        value.setKategori("Berita Kampus");
        value.setAktif(Boolean.TRUE);
        return value;
    }

    public void beforeSave(Session session, GeneralValueObject target,
            GenericCrudRequestContext context) throws Exception {
        PenumumanWebsite value = (PenumumanWebsite) target;
        if (value.getJudul() == null || value.getJudul().trim().length() == 0) {
            throw new GenericCrudException(400, "TITLE_REQUIRED", "Judul wajib diisi.");
        }
        Tbmuser user = context == null ? null : context.getUser();
        PerguruanTinggi current = user == null ? null : user.getPerguruanTinggi();
        if (current != null) value.setPerguruanTinggi(current);
        if (user != null) {
            value.setOleh(user.getUserNama());
            value.setOlehId(user.getUserId());
        }
        super.beforeSave(session, target, context);
    }

    /** Action existing tidak menerapkan deduplikasi judul pada create/import. */
    public List getNaturalKeyProperties() { return new ArrayList(); }
}
