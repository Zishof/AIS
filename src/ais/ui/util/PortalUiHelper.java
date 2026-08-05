package ais.ui.util;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;

/**
 * Pembantu membangun "portal" tampilan yang seragam & rapi dengan sedikit kode.
 * Membungkus pola yang sudah dipakai halaman e-Learning ({@link MyPortallayout} +
 * {@link MyPortalchildren} + Panel CE + {@link PanelToolHelper}) menjadi 3 method
 * ringkas, plus DESKRIPSI SEDERHANA di tiap panel untuk pengguna awam.
 *
 * <p>Tujuan: satu sumber pola portal supaya halaman lain cukup memanggil method ini
 * (mudah dipelihara, tampilan konsisten, responsif otomatis — kolom menumpuk di HP
 * lewat CSS blok "KOMPAT PORTALLAYOUT CE" di css_utama.css). Kolom ber-lebar 100%
 * otomatis pindah ke baris sendiri (membentang penuh).</p>
 *
 * <pre>
 *   MyPortallayout portal = PortalUiHelper.portal(host);
 *   MyPortalchildren k1 = PortalUiHelper.kolom(portal, "50%");
 *   MyPortalchildren k2 = PortalUiHelper.kolom(portal, "50%");
 *   MyPortalchildren k3 = PortalUiHelper.kolom(portal, "100%");   // membentang
 *   Panelchildren bodyMhs   = PortalUiHelper.panel(k1, "Data Mahasiswa", "Identitas mahasiswa & rincian biaya.");
 *   Panelchildren bodyBayar = PortalUiHelper.panel(k2, "Pembayaran", "Catatan pembayaran & tombol bayar.");
 *   Panelchildren bodyDasbor= PortalUiHelper.panel(k3, "Analisis", "Ringkasan & grafik pembayaran.");
 * </pre>
 *
 * Kompatibel Java 1.6/1.7 (tanpa lambda/diamond/stream).
 */
public final class PortalUiHelper {

	private PortalUiHelper() {
	}

	/** Portal responsif (deretan kolom flex; menumpuk jadi 1 kolom di layar sempit). */
	public static MyPortallayout portal(Component parent) {
		MyPortallayout portal = new MyPortallayout();
		portal.setWidth("100%");
		if (parent != null) {
			portal.setParent(parent);
		}
		return portal;
	}

	/**
	 * Satu kolom portal. {@code lebar} contoh "50%", "100%". Kolom 100% otomatis
	 * pindah ke barisnya sendiri (membentang penuh).
	 */
	public static MyPortalchildren kolom(MyPortallayout portal, String lebar) {
		MyPortalchildren kolom = new MyPortalchildren();
		if (lebar != null && lebar.trim().length() > 0) {
			kolom.setWidth(lebar.trim());
		}
		kolom.setSclass("ais-portal-col");
		if (portal != null) {
			kolom.setParent(portal);
		}
		return kolom;
	}

	/** Panel portal + deskripsi sederhana. Tombol Bantuan memakai deskripsi yang sama. */
	public static Component panel(MyPortalchildren kolom, String judul, String deskripsiSederhana) {
		return panel(kolom, judul, deskripsiSederhana, null);
	}

	/**
	 * Buat portal + kolom 100% + panel sekaligus — shortcut satu baris untuk
	 * kebutuhan panel tunggal. Berguna di action class yang tidak butuh multi-kolom.
	 *
	 * @return host Div untuk diisi konten
	 */
	public static Component panelInParent(Component parent, String judul, String deskripsi) {
		return panelInParent(parent, "100%", judul, deskripsi, null);
	}

	/** Seperti {@link #panelInParent(Component, String, String)} dengan bantuan HTML kustom. */
	public static Component panelInParent(Component parent, String lebarKolom,
			String judul, String deskripsi, String bantuanHtml) {
		MyPortallayout p = portal(parent);
		MyPortalchildren k = kolom(p, lebarKolom);
		return panel(k, judul, deskripsi, bantuanHtml);
	}

	/**
	 * Panel portal seragam: header (judul + tombol Layar Penuh + tombol Bantuan) +
	 * baris DESKRIPSI SEDERHANA + area isi. Mengembalikan HOST (Div) untuk diisi
	 * komponen apa pun. Host adalah saudara dari deskripsi, jadi memanggil
	 * {@code Common.clear(host)} TIDAK menghapus deskripsi (deskripsi tetap tampil
	 * walau isi panel dibangun ulang berkali-kali).
	 *
	 * @param deskripsiSederhana 1 kalimat bahasa awam (tanpa istilah teknis).
	 * @param bantuanHtml        isi popup Bantuan; bila null/kosong dipakai deskripsi.
	 */
	public static Component panel(MyPortalchildren kolom, String judul, String deskripsiSederhana,
			String bantuanHtml) {
		Panel panel = new MyPanelConfig();
		panel.setBorder("none");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMinimizable(false);
		panel.setSclass("ais-portal-panel");
		if (kolom != null) {
			panel.setParent(kolom);
		}

		String desc = deskripsiSederhana == null ? "" : deskripsiSederhana.trim();
		String bantuan = bantuanHtml != null && bantuanHtml.trim().length() > 0
				? bantuanHtml
				: PanelBantuanRegistry.getBantuan(judul, desc);

		// Header seragam (judul + Layar Penuh + Bantuan). Caption wajib anak pertama Panel.
		try {
			PanelToolHelper.pasangBantuanDanLayarPenuh(panel, judul, bantuan);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		Panelchildren isi = new Panelchildren();
		isi.setSclass("ais-portal-panel-body");
		isi.setParent(panel);

		// Deskripsi sederhana, terlihat langsung (bukan hanya di tombol Bantuan).
		if (desc.length() > 0) {
			Html descHtml = new Html();
			descHtml.setContent("<div class='ais-portal-desc'>" + DashboardUiKit.esc(desc) + "</div>");
			descHtml.setParent(isi);
		}

		// Host konten — aman di-Common.clear() berulang tanpa menghapus deskripsi.
		Div host = new Div();
		host.setWidth("100%");
		host.setSclass("ais-portal-panel-host");
		host.setParent(isi);
		return host;
	}
}
