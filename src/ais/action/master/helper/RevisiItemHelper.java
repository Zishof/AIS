package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.library.Item;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiItemHelper extends GenericRevisiHelper<Item> {

    private static final long serialVersionUID = 6589578552710016753L;

    public RevisiItemHelper(EventListener eventListener) throws Exception {
        super(Item.class, "Revisi Item Perpustakaan", eventListener, new String[] { "isbn", "isbn10", "issn", "kode", "nama", "pengarangs" });
    }
}
