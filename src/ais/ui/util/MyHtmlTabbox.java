package ais.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.ext.AfterCompose;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Div;
import org.zkoss.zul.Style;

/**
 * {@code MyHtmlTabbox} adalah komponen tab "HTML-like" berbasis {@link org.zkoss.zul.Div}
 * (bukan turunan {@link org.zkoss.zul.Tabbox} ZK). Tujuannya: memberi kontrol penuh atas
 * scroll &amp; tinggi lewat CSS sendiri, tanpa melawan rendering internal {@code Tabbox}
 * ZK 5 (mold {@code z-tabpanel}/{@code z-tabpanel-cnt} yang kerap kolaps 0px atau
 * memotong konten).
 *
 * <h2>Tata letak (mirip Bootstrap nav-tabs)</h2>
 * <pre>
 * MyHtmlTabbox (div.myhtml-tabbox)
 *  ├─ MyHtmlTabs      (div.myhtml-tabs)      ← baris header tab
 *  │   ├─ MyHtmlTab   (div.myhtml-tab)
 *  │   └─ MyHtmlTab   ...
 *  └─ MyHtmlTabpanels (div.myhtml-tabpanels) ← kontainer isi
 *      ├─ MyHtmlTabpanel (div.myhtml-tabpanel)
 *      └─ MyHtmlTabpanel ...
 * </pre>
 *
 * <h2>Kompatibilitas ganda ZK 5 &amp; ZK 9/10</h2>
 * <p>Karena hanya memakai {@code Div}, {@code Label}, {@code Style}, event {@code onClick},
 * dan {@link AfterCompose} — semuanya API inti yang stabil lintas versi — komponen ini
 * jalan di ZK 5.0.13 sekarang DAN ZK CE 9.6/10 nanti (sejalan pola
 * {@code MyPortallayout}/{@code MyChart} yang juga {@code extends Div}).</p>
 *
 * <h2>Perilaku</h2>
 * <ul>
 *   <li>Penautan tab[i] ↔ panel[i] berdasarkan URUTAN anak (sama seperti Tabbox native).</li>
 *   <li>Klik tab → panel terkait ditampilkan, sisanya {@code display:none}; tab diberi
 *       kelas aktif. Semua murni toggle {@code sclass} sisi server (tak ada pengukuran DOM).</li>
 *   <li>Atribut {@code forward="onClick=..."} pada tab TETAP berfungsi (fitur {@code Div}),
 *       sehingga handler lazy-build di Action (mis. {@code onManajemenKelas}) tetap terpanggil
 *       tanpa perubahan kode Action.</li>
 *   <li>Guard "Exactly one selected tab is required" TIDAK diperlukan lagi (tak ada invariant
 *       seleksi ZK).</li>
 * </ul>
 *
 * <p>Wiring dilakukan di {@link #afterCompose()} — dipanggil ZK setelah seluruh anak selesai
 * dikomposisi (bottom-up), sehingga setiap tabbox (termasuk yang bersarang) hanya menautkan
 * anak LANGSUNG miliknya.</p>
 *
 * @author AIS (prototype)
 */
public class MyHtmlTabbox extends Div implements AfterCompose {

	private static final long serialVersionUID = 1L;

	/** Penanda desktop agar CSS hanya diinjeksikan sekali per halaman. */
	private static final String KUNCI_CSS = "_myhtmltab_css_injected";

	private int selectedIndex = 0;

	public MyHtmlTabbox() {
		super();
		setSclass("myhtml-tabbox");
	}

	@Override
	public void afterCompose() {
		injeksiCssSekali();

		MyHtmlTabs tabsBox = anakPertama(MyHtmlTabs.class);
		MyHtmlTabpanels panelsBox = anakPertama(MyHtmlTabpanels.class);
		if (tabsBox == null || panelsBox == null) {
			return; // struktur tak lengkap; jangan mengganggu
		}

		final List<MyHtmlTab> tabs = anakBertipe(tabsBox, MyHtmlTab.class);

		// pilih indeks awal: tab pertama yang selected=true, jika tak ada → 0
		int awal = 0;
		for (int i = 0; i < tabs.size(); i++) {
			if (tabs.get(i).isSelected()) {
				awal = i;
				break;
			}
		}

		// pasang listener klik (selain forward yang sudah ada di tab)
		for (int i = 0; i < tabs.size(); i++) {
			final int idx = i;
			tabs.get(i).addEventListener(Events.ON_CLICK, new EventListener() {
				@Override
				public void onEvent(Event evt) throws Exception {
					setSelectedIndex(idx);
				}
			});
		}

		setSelectedIndex(awal);
	}

	/** Menampilkan panel ke-{@code idx} &amp; menyembunyikan sisanya; menandai tab aktif. */
	public void setSelectedIndex(int idx) {
		MyHtmlTabs tabsBox = anakPertama(MyHtmlTabs.class);
		MyHtmlTabpanels panelsBox = anakPertama(MyHtmlTabpanels.class);
		if (tabsBox == null || panelsBox == null) {
			return;
		}
		List<MyHtmlTab> tabs = anakBertipe(tabsBox, MyHtmlTab.class);
		List<MyHtmlTabpanel> panels = anakBertipe(panelsBox, MyHtmlTabpanel.class);

		if (idx < 0) {
			idx = 0;
		}
		this.selectedIndex = idx;

		for (int i = 0; i < tabs.size(); i++) {
			tabs.get(i).tandaiAktif(i == idx);
		}
		for (int i = 0; i < panels.size(); i++) {
			panels.get(i).tampilkan(i == idx);
		}
	}

	public int getSelectedIndex() {
		return selectedIndex;
	}

	// ─────────────────────────── util internal ───────────────────────────

	@SuppressWarnings("unchecked")
	private <T> T anakPertama(Class<T> tipe) {
		for (Object o : getChildren()) {
			if (tipe.isInstance(o)) {
				return (T) o;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> anakBertipe(Component parent, Class<T> tipe) {
		List<T> hasil = new ArrayList<T>();
		for (Object o : parent.getChildren()) {
			if (tipe.isInstance(o)) {
				hasil.add((T) o);
			}
		}
		return hasil;
	}

	private void injeksiCssSekali() {
		try {
			if (getDesktop() == null || getDesktop().getAttribute(KUNCI_CSS) != null) {
				return;
			}
			getDesktop().setAttribute(KUNCI_CSS, Boolean.TRUE);
			Style st = new Style();
			st.setContent(CSS);
			appendChild(st);
		} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/ui/util/MyHtmlTabbox.java:164");
			// injeksi CSS bersifat kosmetik; jangan gagalkan render
		}
	}

	/**
	 * Gaya nav-tabs (Bootstrap-like) yang MENGIKUTI TEMA terpilih. Warna memakai token
	 * {@code --theme-*}/{@code --ais-*} yang didefinisikan di {@code css_utama.css} (di-:root)
	 * dan ditimpa file tema terpilih; token ini diinjeksikan ke SETIAP halaman lewat
	 * {@code ais.ui.util.MyThemeProvider} (theme-provider-class di zk.xml), sehingga tab
	 * otomatis berganti warna saat tema diganti — sama seperti tab ZK native yang distyle
	 * {@code base-theme.css} via {@code --theme-primary}. Fallback disediakan bila token absen.
	 * Header {@code flex-wrap:wrap} → 16+ tab membungkus, tak pernah terpotong.
	 */
	private static final String CSS = ""
			+ ".myhtml-tabbox{display:flex;flex-direction:column;width:100%;max-width:100%;box-sizing:border-box;}"
			+ ".myhtml-tabs{display:flex;flex-wrap:wrap;gap:3px;align-items:flex-end;margin:0;padding:6px 8px 0;"
			+ "border-bottom:2px solid var(--theme-primary,#007131);background:var(--ais-bg-soft,#f8fafc);box-sizing:border-box;}"
			+ ".myhtml-tab{padding:7px 15px;cursor:pointer;font-size:13px;line-height:1.2;color:#555;"
			+ "background:#e9edf2;border:1px solid var(--ais-border,#dce6ef);border-bottom:none;border-radius:9px 9px 0 0;"
			+ "white-space:nowrap;user-select:none;transition:background .12s,color .12s,border-color .12s;}"
			+ ".myhtml-tab:hover{background:var(--theme-light,rgba(0,113,49,.10));color:var(--theme-primary,#007131);}"
			+ ".myhtml-tab-active{background:var(--ais-surface,#fff);color:var(--theme-primary,#007131);font-weight:600;"
			+ "border-color:var(--theme-primary,#007131);border-top:3px solid var(--theme-primary,#007131);"
			+ "border-bottom:2px solid var(--ais-surface,#fff);margin-bottom:-2px;"
			+ "box-shadow:0 -2px 6px rgba(0,0,0,.06);}"
			+ ".myhtml-tabpanels{position:relative;width:100%;max-width:100%;box-sizing:border-box;"
			+ "background:var(--ais-surface,#fff);min-height:40px;}"
			+ ".myhtml-tabpanel{width:100%;max-width:100%;box-sizing:border-box;overflow:visible;padding:0;}"
			+ ".myhtml-tabpanel-hidden{display:none !important;}"
			// sub-tab (bersarang) sedikit lebih ringkas
			+ ".myhtml-tabbox .myhtml-tabbox .myhtml-tab{padding:5px 11px;font-size:12px;}"
			// responsif: di layar sempit header tetap wrap (bukan clip) — kelebihan utama vs Tabbox ZK
			+ "@media(max-width:768px){.myhtml-tab{padding:6px 10px;font-size:12px;}}";
}
