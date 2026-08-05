package ais.ui.util;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Column;
import org.zkoss.zul.Grid;

/**
 * Pengurut kolom <b>SERVER-SIDE</b> untuk grid hasil pada popup pencarian
 * AmbilData* (Banbox/Banyak).
 *
 * <p>
 * Klik header kolom → set <code>ORDER BY</code> pada field entity kolom itu
 * (klik lagi = balik arah asc/desc) → memanggil ulang pencarian sehingga data
 * di-<b>query ulang ke database</b> dengan urutan baru (bukan sekadar mengurut
 * baris di halaman aktif). Indikator panah ▲/▼ ditambahkan pada label kolom yang
 * sedang menjadi acuan urut.
 * </p>
 *
 * <p>
 * Sengaja dibuat <b>self-contained</b> (tidak mengubah {@link AmbilDataPagingHelper})
 * supaya AMAN: popup yang belum memakai helper ini sama sekali tidak terpengaruh.
 * </p>
 *
 * <p>
 * Pemakaian per popup (3 langkah kecil):
 * </p>
 *
 * <pre>
 * // 1) field
 * private final AmbilDataSortHelper sortHelper = new AmbilDataSortHelper();
 *
 * // 2) SETELAH kolom hasil dibuat — petakan kolom ke field entity (null = tak bisa di-sort,
 * //    mis. kolom foto/checkbox atau kolom hasil gabungan/komputasi):
 * sortHelper.pasang(grid, new EventListener() {
 *     public void onEvent(Event e) throws Exception { onSearchDefault(null); }
 * }, new String[] { null, "nama", "noRegistrasi", "noUjian", null });
 *
 * // 3) pada pembangun criteria, GANTI .addOrder(Order.asc("id")) dengan urutan dinamis:
 * sortHelper.terapkan(criteria, "id", true); // "id" asc = urutan default saat belum ada pilihan
 * </pre>
 *
 * Kompatibel Java 1.7 &amp; ZK 5.5.
 */
public class AmbilDataSortHelper {

	private static final String ATTR_LABEL_ASLI = "__ais_sort_label_asli";

	private String field = null;
	private boolean asc = true;

	/** Field entity yang sedang dipakai mengurut (null bila belum dipilih). */
	public String getField() {
		return field;
	}

	/** Arah urut saat ini (true = naik / ascending). */
	public boolean isAsc() {
		return asc;
	}

	/** Field aktif, atau {@code defaultField} bila belum ada kolom yang diklik. */
	public String field(String defaultField) {
		return (field != null && field.trim().length() > 0) ? field : defaultField;
	}

	/** Arah aktif, atau {@code defaultAsc} bila belum ada kolom yang diklik. */
	public boolean asc(boolean defaultAsc) {
		return (field != null && field.trim().length() > 0) ? asc : defaultAsc;
	}

	/**
	 * Tambahkan <code>ORDER BY</code> sesuai pilihan urut aktif ke {@code criteria}.
	 * Bila belum ada kolom yang diklik, pakai {@code defaultField}/{@code defaultAsc}.
	 * Aman: bila field tak valid (mis. typo / bukan properti), tidak menjatuhkan query.
	 */
	public void terapkan(Criteria criteria, String defaultField, boolean defaultAsc) {
		if (criteria == null) {
			return;
		}
		boolean adaPilihan = field != null && field.trim().length() > 0;
		String f = adaPilihan ? field : defaultField;
		boolean a = adaPilihan ? asc : defaultAsc;
		if (f == null || f.trim().length() == 0) {
			return;
		}
		try {
			criteria.addOrder(a ? Order.asc(f) : Order.desc(f));
		} catch (Exception e) {
			// Field tak dikenal Hibernate → jangan rusak pencarian; coba urutan default.
			if (defaultField != null && !defaultField.equals(f)) {
				try {
					criteria.addOrder(defaultAsc ? Order.asc(defaultField) : Order.desc(defaultField));
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/ui/util/AmbilDataSortHelper.java:98");
				}
			}
		}
	}

	/**
	 * Pasang kemampuan urut pada kolom grid hasil.
	 *
	 * @param grid          grid hasil pencarian
	 * @param reSearch      listener yang menjalankan ulang pencarian (mis. onSearchDefault)
	 * @param propsByColumn field entity per kolom (urut sesuai kolom). null/""=kolom tak bisa di-sort.
	 */
	public void pasang(final Grid grid, final EventListener reSearch, final String[] propsByColumn) {
		try {
			if (grid == null || grid.getColumns() == null || propsByColumn == null) {
				return;
			}
			final List<?> kolom = grid.getColumns().getChildren();
			for (int i = 0; i < kolom.size() && i < propsByColumn.length; i++) {
				final String prop = propsByColumn[i];
				if (prop == null || prop.trim().length() == 0) {
					continue;
				}
				Object o = kolom.get(i);
				if (!(o instanceof Column)) {
					continue;
				}
				final Column col = (Column) o;
				col.setAttribute(ATTR_LABEL_ASLI, col.getLabel());
				col.setTooltiptext("Klik untuk mengurutkan data berdasarkan kolom ini");
				try {
					String gaya = col.getStyle();
					col.setStyle((gaya == null ? "" : gaya) + ";cursor:pointer;");
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/ui/util/AmbilDataSortHelper.java:132");
				}
				col.addEventListener("onClick", new EventListener() {
					public void onEvent(Event e) throws Exception {
						if (prop.equals(field)) {
							asc = !asc;
						} else {
							field = prop;
							asc = true;
						}
						perbaruiIndikator(kolom, propsByColumn);
						if (reSearch != null) {
							reSearch.onEvent(null);
						}
					}
				});
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/AmbilDataSortHelper.java:150");
		}
	}

	/** Perbarui panah ▲/▼ pada label kolom yang sedang menjadi acuan urut. */
	private void perbaruiIndikator(List<?> kolom, String[] propsByColumn) {
		try {
			for (int i = 0; i < kolom.size() && i < propsByColumn.length; i++) {
				Object o = kolom.get(i);
				if (!(o instanceof Column)) {
					continue;
				}
				Column col = (Column) o;
				Object asli = col.getAttribute(ATTR_LABEL_ASLI);
				if (asli == null) {
					continue;
				}
				String dasar = asli.toString();
				String prop = propsByColumn[i];
				if (prop != null && prop.equals(field)) {
					col.setLabel(dasar + (asc ? " ▲" : " ▼"));
				} else {
					col.setLabel(dasar);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/AmbilDataSortHelper.java:175");
		}
	}
}
