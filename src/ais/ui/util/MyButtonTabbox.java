package ais.ui.util;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compatibility wrapper untuk pola tab tombol lama.
 *
 * Implementasi ini sengaja tidak memakai Tabbox/Tabpanel native ZK. Pada ZK 5,
 * Tabpanel yang bersarang di window/form besar beberapa kali membuat isi
 * createComponents tidak terpasang stabil atau kolaps menjadi kosong. Karena
 * pemanggil class ini hanya butuh tombol pemilih + panel konten, panel dikelola
 * manual memakai Div biasa.
 */
public class MyButtonTabbox {

	private final Map<Integer, Div> panelMap = new LinkedHashMap<Integer, Div>();
	private final Map<Integer, MyToolbarbuttonConfig> tombolMap = new LinkedHashMap<Integer, MyToolbarbuttonConfig>();
	private final Map<Integer, EventListener> pemuatLazyMap = new LinkedHashMap<Integer, EventListener>();
	private final Map<Integer, EventListener> onSetiapPilihMap = new LinkedHashMap<Integer, EventListener>();
	private final Set<Integer> sudahDimuat = new HashSet<Integer>();
	private final Div panelHost;
	private final Hbox tombolBar;
	private final Hbox actionToolbar;
	private final int[] tabAktif;

	private MyButtonTabbox(Hbox tombolBar, Div panelHost, Hbox actionToolbar, int[] tabAktif) {
		this.tombolBar = tombolBar;
		this.panelHost = panelHost;
		this.actionToolbar = actionToolbar;
		this.tabAktif = tabAktif;
	}

	public static MyButtonTabbox buat(Component parent, String tinggiCss, int[] tabAktifHolder) {
		Div outer = new Div();
		outer.setWidth("100%");
		outer.setHeight(tinggiCss);
		outer.setStyle("display:flex;flex-direction:column;min-height:0;overflow:hidden;");
		outer.setParent(parent);

		Hbox tombolBar = new Hbox();
		tombolBar.setSclass("ais-btn-group ais-button-tabbox");
		tombolBar.setWidth("100%");
		tombolBar.setStyle("flex:0 0 auto;overflow-x:auto;white-space:nowrap;");
		tombolBar.setParent(outer);

		Div panelHost = new Div();
		panelHost.setWidth("100%");
		panelHost.setHeight("100%");
		panelHost.setStyle("flex:1 1 auto;min-height:0;overflow:auto;");
		panelHost.setParent(outer);

		Hbox actionToolbar = new Hbox();
		actionToolbar.setSclass("ais-btn-group ais-button-tabbox");
		actionToolbar.setWidth("100%");
		actionToolbar.setVisible(false);
		actionToolbar.setStyle("flex:0 0 auto;");
		actionToolbar.setParent(outer);

		MyButtonTabbox tabbox = new MyButtonTabbox(tombolBar, panelHost, actionToolbar,
				tabAktifHolder == null ? new int[] { 1 } : tabAktifHolder);
		outer.setAttribute("myButtonTabbox", tabbox);
		panelHost.setAttribute("myButtonTabbox", tabbox);
		return tabbox;
	}

	public static MyButtonTabbox gantiTabboxNative(final Tabbox tabbox, int[] tabAktifHolder) {
		if (tabbox == null || tabbox.getParent() == null) {
			return null;
		}
		Object sudahAda = tabbox.getAttribute("myButtonTabboxPengganti");
		if (sudahAda instanceof MyButtonTabbox) {
			return (MyButtonTabbox) sudahAda;
		}

		int selectedIndex = tabbox.getSelectedIndex() + 1;
		if (selectedIndex <= 0) {
			selectedIndex = 1;
		}
		int[] tabAktif = tabAktifHolder == null ? new int[] { selectedIndex } : tabAktifHolder;
		if (tabAktif[0] <= 0) {
			tabAktif[0] = selectedIndex;
		}

		String tinggi = tabbox.getHeight();
		if (tinggi == null || tinggi.trim().isEmpty()) {
			tinggi = "100%";
		}
		final MyButtonTabbox pengganti = buat(tabbox.getParent(), tinggi, tabAktif);
		tabbox.setAttribute("myButtonTabboxPengganti", pengganti);

		Tabs tabs = tabbox.getTabs();
		Tabpanels tabpanels = tabbox.getTabpanels();
		if (tabs == null || tabpanels == null) {
			tabbox.setVisible(false);
			return pengganti;
		}

		List<Component> daftarTab = salinChildren(tabs);
		List<Component> daftarPanel = salinChildren(tabpanels);
		for (int i = 0; i < daftarTab.size(); i++) {
			if (!(daftarTab.get(i) instanceof Tab)) {
				continue;
			}
			final Tab tabAsli = (Tab) daftarTab.get(i);
			final int index = i + 1;
			Div panelBaru = pengganti.tambahTab(index, labelTab(tabAsli));
			if (!tabAsli.isVisible()) {
				pengganti.setVisibleTombol(index, false);
			}
			if (i < daftarPanel.size() && daftarPanel.get(i) instanceof Tabpanel) {
				Tabpanel panelAsli = (Tabpanel) daftarPanel.get(i);
				for (Component isi : salinChildren(panelAsli)) {
					isi.setParent(panelBaru);
				}
			}
			pengganti.onSetiapPilih(index, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Events.sendEvent(new Event("onClick", tabAsli));
				}
			});
		}

		tabbox.setVisible(false);
		tabbox.setStyle("display:none;height:0;width:0;overflow:hidden;");
		pengganti.pulihkanSeleksi(daftarTab.size());
		return pengganti;
	}

	private static List<Component> salinChildren(Component component) {
		List<Component> hasil = new ArrayList<Component>();
		if (component == null) {
			return hasil;
		}
		for (Object child : component.getChildren()) {
			if (child instanceof Component) {
				hasil.add((Component) child);
			}
		}
		return hasil;
	}

	private static String labelTab(Tab tab) {
		String label = tab.getLabel();
		return label == null || label.trim().isEmpty() ? "Tab" : label.trim();
	}

	public Div tambahTab(final int index, String label) {
		return buatTombolDanPanel(index, label, null);
	}

	public Div tambahTab(final int index, String label, String icon) {
		return buatTombolDanPanel(index, label, icon);
	}

	public interface PemuatTab {
		void muat(Div panel) throws Exception;
	}

	public Div tambahTabLazy(final int index, String label, final PemuatTab pemuat) {
		return tambahTabLazy(index, label, null, pemuat);
	}

	public Div tambahTabLazy(final int index, String label, String icon, final PemuatTab pemuat) {
		final Div panel = buatTombolDanPanel(index, label, icon);
		pemuatLazyMap.put(Integer.valueOf(index), new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				pemuat.muat(panel);
			}
		});
		if (tabAktif[0] == index && panel.isVisible()) {
			muatJikaBelum(index);
		}
		return panel;
	}

	public Div tambahTabZul(final int index, String label, final String src) {
		return tambahTabZul(index, label, (String) null, src);
	}

	public Div tambahTabZul(final int index, String label, String icon, final String src) {
		return tambahTabLazy(index, label, icon, new PemuatTab() {
			@Override
			public void muat(Div panel) throws Exception {
				muatZul(panel, src);
			}
		});
	}

	private int nextIndex() {
		int max = 0;
		for (Integer k : panelMap.keySet()) {
			if (k.intValue() > max) max = k.intValue();
		}
		return max + 1;
	}

	public Div tambahTab(String label, String icon) {
		return buatTombolDanPanel(nextIndex(), label, icon);
	}

	public Div tambahTabLazy(String label, String icon, final PemuatTab pemuat) {
		return tambahTabLazy(nextIndex(), label, icon, pemuat);
	}

	public void setVisiblePanel(Div panel, boolean visible) {
		for (Map.Entry<Integer, Div> entry : panelMap.entrySet()) {
			if (entry.getValue() == panel) {
				setVisibleTombol(entry.getKey().intValue(), visible);
				return;
			}
		}
	}

	public void onSetiapKlikPanel(Div panel, EventListener listener) {
		for (Map.Entry<Integer, Div> entry : panelMap.entrySet()) {
			if (entry.getValue() == panel) {
				onSetiapPilih(entry.getKey().intValue(), listener);
				return;
			}
		}
	}

	public static void muatZul(Div panel, String src) throws Exception {
		if (panel == null) {
			return;
		}
		panel.getChildren().clear();
		MyInclude include = new MyInclude(src);
		include.setParent(panel);
	}

	/**
	 * Muat fragment zul secara EAGER (langsung dibuat sebagai komponen di halaman
	 * dan ID-space yang SAMA dengan window pemanggil), bukan via Include.
	 *
	 * WAJIB dipakai (bukan muatZul) bila fragment berisi komponen ber-id yang harus
	 * di-autowire composer induk (mis. grid/paging/searchnama pada GenericCrudAction)
	 * atau memakai forward="..." ke composer induk: Include me-render kontennya
	 * TERTUNDA dan di ID-space TERPISAH, sehingga autowire mendapat null dan event
	 * forward tidak pernah sampai ke composer -- gejala khasnya grid tampil tapi
	 * datanya kosong selamanya.
	 */
	public static void muatZulEager(Div panel, String src) throws Exception {
		if (panel == null) {
			return;
		}
		panel.getChildren().clear();
		org.zkoss.zk.ui.Executions.createComponents(src, panel, null);
	}

	private Div buatTombolDanPanel(final int index, String label, String icon) {
		MyToolbarbuttonConfig tombol = (icon != null && !icon.isEmpty())
				? new MyToolbarbuttonConfig(label, icon)
				: new MyToolbarbuttonConfig(label);
		tombol.setSclass("ais-button-tabbox-button");
		tombol.setParent(tombolBar);
		tombol.setTooltiptext(label);
		tombolMap.put(Integer.valueOf(index), tombol);

		Div panel = new Div();
		panel.setParent(panelHost);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setStyle("overflow:auto;");
		panel.setVisible(false);
		panelMap.put(Integer.valueOf(index), panel);

		tombol.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				pilih(index);
			}
		});

		if (panelMap.size() == 1) {
			if (!panelMap.containsKey(Integer.valueOf(tabAktif[0]))) {
				tabAktif[0] = index;
			}
			panel.setVisible(tabAktif[0] == index);
			aturTampilanTombol(tabAktif[0]);
		}
		return panel;
	}

	public void pilih(int index) {
		if (!panelMap.containsKey(Integer.valueOf(index))) {
			index = indexPertama();
		}
		if (!panelMap.containsKey(Integer.valueOf(index))) {
			return;
		}
		tabAktif[0] = index;
		for (Map.Entry<Integer, Div> entry : panelMap.entrySet()) {
			entry.getValue().setVisible(entry.getKey().intValue() == index);
		}
		aturTampilanTombol(index);
		muatJikaBelum(index);
		EventListener onSetiapPilih = onSetiapPilihMap.get(Integer.valueOf(index));
		if (onSetiapPilih != null) {
			try {
				onSetiapPilih.onEvent(null);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}
		Div panelAktif = panelMap.get(Integer.valueOf(index));
		if (panelAktif != null) {
			aktifkanTabPertamaDiDalam(panelAktif, new HashSet<MyButtonTabbox>());
		}
	}

	public void onSetiapPilih(int index, EventListener listener) {
		onSetiapPilihMap.put(Integer.valueOf(index), listener);
	}

	private void muatJikaBelum(final int index) {
		if (sudahDimuat.contains(Integer.valueOf(index))) {
			return;
		}
		final EventListener pemuat = pemuatLazyMap.get(Integer.valueOf(index));
		if (pemuat == null) {
			return;
		}
		sudahDimuat.add(Integer.valueOf(index));
		try {
			pemuat.onEvent(null);
		} catch (Exception e) {
			Div panel = panelMap.get(Integer.valueOf(index));
			if (panel != null && panel.getChildren().isEmpty()) {
				Label label = new Label("Gagal memuat konten tab: "
						+ (e.getMessage() == null ? e.getClass().getName() : e.getMessage()));
				label.setStyle("color:#b91c1c;padding:12px;display:block;");
				label.setParent(panel);
			}
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	public void pulihkanSeleksi(int jumlahTab) {
		int idx = panelMap.containsKey(Integer.valueOf(tabAktif[0])) ? tabAktif[0] : indexPertama();
		pilih(idx);
	}

	public void pilihPertama() {
		int idx = indexPertama();
		if (idx > 0) {
			pilih(idx);
		}
	}

	public void muatUlang(int index) {
		Div panel = panelMap.get(Integer.valueOf(index));
		if (panel == null) {
			return;
		}
		panel.getChildren().clear();
		sudahDimuat.remove(Integer.valueOf(index));
		pilih(index);
	}

	public static void aktifkanTabPertamaDiDalam(Component root) {
		aktifkanTabPertamaDiDalam(root, new HashSet<MyButtonTabbox>());
	}

	private static void aktifkanTabPertamaDiDalam(Component root, Set<MyButtonTabbox> sudahDiproses) {
		if (root == null) {
			return;
		}
		Object attr = root.getAttribute("myButtonTabbox");
		if (attr instanceof MyButtonTabbox) {
			MyButtonTabbox tabbox = (MyButtonTabbox) attr;
			if (sudahDiproses.add(tabbox)) {
				tabbox.pilihPertama();
			}
		}
		for (Object child : root.getChildren()) {
			if (child instanceof Component) {
				aktifkanTabPertamaDiDalam((Component) child, sudahDiproses);
			}
		}
	}

	public void setLabelTombol(int index, String label) {
		MyToolbarbuttonConfig tombol = tombolMap.get(Integer.valueOf(index));
		if (tombol != null) {
			tombol.setLabel(label);
		}
	}

	public void setVisibleTombol(int index, boolean visible) {
		MyToolbarbuttonConfig tombol = tombolMap.get(Integer.valueOf(index));
		if (tombol != null) {
			tombol.setVisible(visible);
		}
		Div panel = panelMap.get(Integer.valueOf(index));
		if (panel != null) {
			panel.setVisible(visible && tabAktif[0] == index);
		}
	}

	public void setTooltipTombol(int index, String tooltip) {
		MyToolbarbuttonConfig tombol = tombolMap.get(Integer.valueOf(index));
		if (tombol != null) {
			tombol.setTooltiptext(tooltip);
		}
	}

	public void tambahTombolAksi(MyToolbarbuttonConfig tombol) {
		actionToolbar.setVisible(true);
		tombol.setParent(actionToolbar);
	}

	/**
	 * Sisipkan komponen header di antara baris tombol tab dan panelHost.
	 * Berguna untuk menampilkan info konteks (misalnya info pertemuan) tepat
	 * di bawah tab buttons tanpa masuk ke area scroll konten tab.
	 * Header diberi flex:0 0 auto otomatis agar mengambil tinggi alami.
	 */
	public void tambahHeaderKonten(org.zkoss.zk.ui.Component header) {
		if (header instanceof Div) {
			Div div = (Div) header;
			String existingStyle = div.getStyle() == null ? "" : div.getStyle();
			if (!existingStyle.contains("flex:")) {
				div.setStyle("flex:0 0 auto;" + existingStyle);
			}
		}
		panelHost.getParent().insertBefore(header, panelHost);
	}

	public int getTabAktif() {
		return tabAktif[0];
	}

	/**
	 * Dipertahankan hanya untuk kompatibilitas source lama. MyButtonTabbox kini
	 * tidak lagi memakai Tabbox native.
	 */
	public Tabbox getTabbox() {
		return null;
	}

	private int indexPertama() {
		if (panelMap.isEmpty()) {
			return 0;
		}
		return panelMap.keySet().iterator().next().intValue();
	}

	private void aturTampilanTombol(int indexAktif) {
		for (Map.Entry<Integer, MyToolbarbuttonConfig> entry : tombolMap.entrySet()) {
			boolean aktif = entry.getKey().intValue() == indexAktif;
			MyToolbarbuttonConfig tombol = entry.getValue();
			tombol.setSclass(aktif ? "ais-button-tabbox-button ais-button-tabbox-active"
					: "ais-button-tabbox-button");
			tombol.setStyle("");
		}
	}
}
