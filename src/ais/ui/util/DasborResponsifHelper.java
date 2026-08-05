package ais.ui.util;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Div;

/**
 * Pembungkus pola dasbor lama "Borderlayout (North=saringan, Center=isi)" menjadi
 * portal responsif yang menumpuk otomatis di layar HP — TANPA mengubah logika data.
 *
 * <p>Alih-alih ZK {@link org.zkoss.zul.Borderlayout} (yang tinggi region-nya
 * digerakkan JavaScript dan tidak bisa di-reflow lewat CSS), pola ini memakai
 * {@link MyPortallayout} + {@link MyPortalchildren} + kartu Panel dari
 * {@link PortalUiHelper}. Kartu Saringan tampil penuh di atas, kartu Isi penuh di
 * bawah; di layar sempit keduanya tetap menumpuk rapi (CSS blok "KOMPAT
 * PORTALLAYOUT CE" di css_utama.css). Tinggi mengikuti isi (natural-height),
 * jadi tidak pernah kolaps seperti Borderlayout.</p>
 *
 * <p>Tujuan: satu sumber pola supaya migrasi tiap dasbor cukup memanggil method ini
 * dan menempelkan komponen yang sudah ada ke host yang dikembalikan — seragam,
 * mudah dipelihara, dan reuse maksimum.</p>
 *
 * <pre>
 *   // OLD: Borderlayout + North(grid saringan) + Center(isi)
 *   // NEW:
 *   Component[] host = DasborResponsifHelper.saringanDanIsi(this,
 *           "Saringan Data", "Pilih tahun & semester untuk menyaring.",
 *           "Statistik Mahasiswa", "Jumlah mahasiswa per program studi.");
 *   Component saringan = host[0];          // tempel grid filter ke sini
 *   center = (Div) host[1];                // field isi (dulu Center) → Div
 * </pre>
 *
 * Kompatibel Java 1.6/1.7 (tanpa lambda/diamond/stream) dan ZK 5 CE maupun 9/10 CE.
 */
public final class DasborResponsifHelper {

	private DasborResponsifHelper() {
	}

	/**
	 * Portal + 1 kartu penuh; kembalikan host isi (Div). Untuk dasbor yang hanya
	 * punya satu area isi (tanpa saringan).
	 */
	public static Component isiTunggal(Component parent, String judul, String deskripsi) {
		MyPortallayout portal = PortalUiHelper.portal(parent);
		MyPortalchildren kolom = PortalUiHelper.kolom(portal, "100%");
		return PortalUiHelper.panel(kolom, judul, deskripsi);
	}

	/**
	 * Portal + kartu Saringan (atas, penuh) + kartu Isi (bawah, penuh).
	 *
	 * @return array 2 elemen: [0] host saringan, [1] host isi. Keduanya {@link Div}
	 *         sehingga aman dipakai {@code Common.clear()}, {@code appendChild()},
	 *         dan {@code setStyle()} seperti Center/North lama.
	 */
	public static Component[] saringanDanIsi(Component parent, String judulSaringan, String deskripsiSaringan,
			String judulIsi, String deskripsiIsi) {
		MyPortallayout portal = PortalUiHelper.portal(parent);
		Component hostSaringan = PortalUiHelper.panel(PortalUiHelper.kolom(portal, "100%"), judulSaringan,
				deskripsiSaringan);
		Component hostIsi = PortalUiHelper.panel(PortalUiHelper.kolom(portal, "100%"), judulIsi, deskripsiIsi);
		return new Component[] { hostSaringan, hostIsi };
	}

	/**
	 * Varian dua kolom berdampingan di layar lebar (mis. {@code "40%"} saringan /
	 * {@code "60%"} isi) yang otomatis menumpuk jadi satu kolom di HP. Dipakai bila
	 * saringan ringkas dan isi lebar.
	 */
	public static Component[] saringanDanIsi(Component parent, String lebarSaringan, String lebarIsi,
			String judulSaringan, String deskripsiSaringan, String judulIsi, String deskripsiIsi) {
		MyPortallayout portal = PortalUiHelper.portal(parent);
		Component hostSaringan = PortalUiHelper.panel(PortalUiHelper.kolom(portal, lebarSaringan), judulSaringan,
				deskripsiSaringan);
		Component hostIsi = PortalUiHelper.panel(PortalUiHelper.kolom(portal, lebarIsi), judulIsi, deskripsiIsi);
		return new Component[] { hostSaringan, hostIsi };
	}

	/**
	 * Membuat sebuah {@code Tabpanel} <b>mengikuti tinggi kontennya</b> (dasbor bertinggi natural),
	 * bukan terkunci setinggi viewport dengan <i>scroll dalam</i> yang terbatas.
	 *
	 * <p><b>Masalah.</b> Tabbox ber-{@code height:100%} membuat tiap Tabpanel dibatasi setinggi
	 * viewport; konten dasbor yang lebih tinggi jadi tergulung di dalam panel (scroll dalam bertinggi
	 * terbatas), tidak "mengikuti tinggi parent". Pola yang sudah terbukti di halaman ini (tab "Data")
	 * memakai tinggi definit besar ({@code setHeight("30330px")}) sehingga panel memanjang dan scroll
	 * ditangani wadah luar ({@code tampilanScrollTabbox}) — tetapi tinggi tetap itu menyisakan ruang
	 * kosong bila konten lebih pendek.</p>
	 *
	 * <p><b>Solusi.</b> Memasang skrip klien {@code onBind} (di-<i>scope</i> HANYA ke tabpanel ini,
	 * BUKAN global — agar tidak mengulang masalah auto-tinggi global yang pernah di-<i>revert</i>):
	 * mengukur tinggi konten sebenarnya lalu menyetel tinggi tabpanel = tinggi konten
	 * ({@code overflow:visible}), sehingga panel <b>pas</b> mengikuti konten — tanpa scroll dalam DAN
	 * tanpa ruang kosong. Diukur ulang beberapa kali (konten dasbor dimuat asinkron via timer) dan
	 * lewat {@code ResizeObserver} bila tersedia. Semua dibungkus {@code try/catch} sehingga aman.</p>
	 *
	 * @param tabpanel tabpanel host dasbor yang akan dibuat mengikuti tinggi konten (boleh null)
	 */
	public static void fitTabpanelKeKonten(org.zkoss.zul.Tabpanel tabpanel) {
		if (tabpanel == null) {
			return;
		}
		tabpanel.setStyle("overflow:visible");
		String js = "var self=this;setTimeout(function(){try{"
				+ "var n=self.$n();if(!n){return;}n.style.overflow='visible';"
				+ "var fit=function(){try{n.style.height='auto';var h=n.scrollHeight;if(h>80){n.style.height=h+'px';}}catch(e){}};"
				+ "fit();"
				+ "var c=n.firstElementChild;"
				+ "if(window.ResizeObserver&&c){try{new ResizeObserver(fit).observe(c);}catch(e){}}"
				+ "var k=0,iv=setInterval(function(){fit();if(++k>25){clearInterval(iv);}},400);"
				+ "}catch(e){}},250);";
		tabpanel.setWidgetListener("onBind", js);
	}

	/**
	 * Membuat tab "Dasbor" (natural-height) <b>mengikuti tinggi parent sebenarnya</b> — memakai scroll
	 * halaman utama, BUKAN scroll dalam bertinggi terbatas dari wadah {@code ais-scroll-tabbox-host}.
	 *
	 * <p><b>Latar.</b> {@code tampilanScrollTabbox} membungkus tabbox dalam sebuah {@code Center}
	 * ({@code ais-scroll-tabbox-host}) ber-{@code overflow:auto} dan tinggi terukur setinggi viewport
	 * (mis. 743px). Itu memang DIBUTUHKAN tab lain (Data/KRS/dst. memakai {@code setHeight("100%")}
	 * mengisi tinggi definit tsb). Karena itu wadah TIDAK boleh dilepas global; ia hanya di-<i>lepas
	 * klip</i>-nya SAAT tab Dasbor aktif, lalu <b>dipulihkan</b> saat pindah ke tab lain.</p>
	 *
	 * <p><b>Cara kerja (skrip klien scoped di wadah).</b> Saat tab Dasbor terlihat: rantai
	 * [host, center-body, tabbox, tabpanels, tabpanel-dasbor] disetel {@code height:auto;
	 * overflow:visible} sehingga konten mengalir penuh dan scroll ditangani halaman luar. Saat tab lain
	 * aktif: rantai dikembalikan ke nilai semula (disimpan di atribut {@code data-aish/data-aiso} sekali,
	 * setelah ZK selesai mengukur) sehingga tab tsb kembali menggulir di dalam wadah seperti semula.
	 * Disinkronkan pada klik tab + beberapa kali di awal (konten dasbor dimuat asinkron). Semua
	 * dibungkus {@code try/catch}. Tidak mengubah perilaku global — hanya wadah dasbor ini.</p>
	 *
	 * @param host           {@code Center} pembungkus dari {@code tampilanScrollTabbox} (boleh null)
	 * @param tabpanelDasbor tabpanel tab "Dasbor" yang isinya harus mengalir penuh (boleh null)
	 */
	public static void dasborTabIkutiParent(org.zkoss.zul.Center host, org.zkoss.zul.Tabpanel tabpanelDasbor) {
		if (host == null || tabpanelDasbor == null) {
			return;
		}
		final String tpId = tabpanelDasbor.getUuid();
		String js = "var self=this;var tpId='" + tpId + "';setTimeout(function(){try{"
				+ "var host=self.$n();if(!host){return;}"
				+ "var q=function(s){return host.querySelector(s);};"
				+ "var cave=q('.z-center-body');var tabbox=q('.z-tabbox');var tabpanels=q('.z-tabpanels');"
				+ "var tp=document.getElementById(tpId);"
				+ "var chain=[host,cave,tabbox,tabpanels];"
				+ "for(var i=0;i<chain.length;i++){var el=chain[i];if(el&&el.getAttribute('data-aish')===null){el.setAttribute('data-aish',el.style.height||'');el.setAttribute('data-aiso',el.style.overflow||'');}}"
				+ "if(tp&&tp.getAttribute('data-aish')===null){tp.setAttribute('data-aish',tp.style.height||'');tp.setAttribute('data-aiso',tp.style.overflow||'');}"
				+ "var relax=function(){for(var i=0;i<chain.length;i++){var el=chain[i];if(el){el.style.height='auto';el.style.overflow='visible';}}if(tp){tp.style.height='auto';tp.style.overflow='visible';}};"
				+ "var restore=function(){for(var i=0;i<chain.length;i++){var el=chain[i];if(el){el.style.height=el.getAttribute('data-aish');el.style.overflow=el.getAttribute('data-aiso')||'auto';}}if(tp){tp.style.height=tp.getAttribute('data-aish');tp.style.overflow=tp.getAttribute('data-aiso');}};"
				+ "var sync=function(){try{var aktif=tp&&tp.style.display!=='none'&&tp.offsetParent!==null;if(aktif){relax();}else{restore();}}catch(e){}};"
				+ "sync();"
				+ "if(tabbox){tabbox.addEventListener('click',function(){setTimeout(sync,60);setTimeout(sync,260);},true);}"
				+ "var k=0,iv=setInterval(function(){sync();if(++k>20){clearInterval(iv);}},400);"
				+ "}catch(e){}},300);";
		host.setWidgetListener("onBind", js);
	}
}
