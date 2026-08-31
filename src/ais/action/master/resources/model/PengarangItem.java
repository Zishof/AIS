package ais.action.master.resources.model;

import java.util.TreeMap;



import ais.database.model.library.Item;



/**
 * Model data sederhana (bukan entitas Hibernate) yang memetakan satu pengarang ke koleksi
 * {@link Item} pustaka miliknya beserta perannya, dipakai pada layar/laporan katalog
 * perpustakaan yang menampilkan daftar item per pengarang.
 *
 * <p>
 * Satu-satunya bidang, {@link #items}, adalah {@link TreeMap} sehingga entri item selalu
 * terurut menurut pengurutan alami {@link Item} (bukan urutan penambahan); nilai peta berupa
 * {@code String} biasanya menyimpan peran pengarang atas item tersebut (mis. penulis utama,
 * editor, penerjemah).
 * </p>
 */
public class PengarangItem {
	public TreeMap<Item, String> items = new TreeMap<Item, String>();
}