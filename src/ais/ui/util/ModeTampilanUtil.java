package ais.ui.util;

import javax.servlet.http.HttpServletRequest;

import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.util.Clients;

import ais.common.Common;

/**
 * Tombol pindah tampilan <b>Mode Desktop</b> / <b>Mode HP</b> yang dapat dipakai
 * ulang di banyak halaman.
 *
 * <p>
 * Tombol di-render sebagai <b>anchor HTML NATIVE</b> (bukan komponen ZK) yang
 * mengapung di kiri-bawah, sehingga <b>selalu bisa diklik</b> meski menu/komponen
 * ZK sedang bermasalah di sebagian perangkat. Tombol hanya tampil di layar kecil
 * (≤980px) lewat CSS media query, jadi di PC tidak mengganggu.
 * </p>
 *
 * <p>
 * Mekanisme pindah memakai parameter/atribut sesi {@code is_mobile} yang sudah ada
 * ({@code CommonCurrentSessionHelper.isMobile}) dan bersifat sticky per sesi.
 * </p>
 */
public final class ModeTampilanUtil {

	private ModeTampilanUtil() {
	}

	/**
	 * Untuk halaman MANDIRI (mis. pmb.zul / psb.zul): tombol me-RELOAD halaman yang
	 * sama dengan parameter {@code is_mobile} terbalik. Label otomatis: bila tampilan
	 * sedang HP → "Mode Desktop"; bila sedang desktop → "Mode HP".
	 *
	 * @param execution execution ZK saat ini (tersedia di zscript sebagai {@code execution}).
	 * @param selfPath  path servlet halaman ini, mis. {@code "/pmb"} atau {@code "/ppdb"}.
	 */
	public static void injectReloadDiri(Execution execution, String selfPath) {
		if (execution == null || selfPath == null) {
			return;
		}
		try {
			HttpServletRequest req = (HttpServletRequest) execution.getNativeRequest();
			if (req == null) {
				return;
			}
			String ctx = req.getContextPath();
			if (ctx == null) {
				ctx = "";
			}
			boolean sedangDesktop = !Common.isMobile(req);
			String base = ctx + selfPath;
			if (sedangDesktop) {
				inject(base + "?is_mobile=true", "&#128241;", "Mode HP");
			} else {
				inject(base + "?is_mobile=false", "&#128421;", "Mode Desktop");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/ModeTampilanUtil.java:59");
			// jangan ganggu render bila injeksi gagal
		}
	}

	/**
	 * Inti injeksi: pasang anchor mengapung + CSS responsif. Idempoten — memanggil ulang
	 * akan mengganti tombol lama (id {@code aisModeTampilanBtn}). Default: tombol disembunyikan
	 * di layar lebar (≥981px) lewat media query.
	 */
	public static void inject(String href, String iconHtml, String label) {
		inject(href, iconHtml, label, false);
	}

	/**
	 * Sama seperti {@link #inject(String, String, String)} namun dengan opsi {@code paksaTampil}.
	 *
	 * @param paksaTampil bila {@code true}, tombol TIDAK disembunyikan di layar lebar (≥981px).
	 *                    Dipakai saat perangkat memang HP ASLI tetapi sedang dirender dalam mode
	 *                    desktop (viewport bisa selebar desktop), sehingga tombol "Mode Mobile"
	 *                    tetap terlihat agar pengguna bisa mencoba mode mobile.
	 */
	public static void inject(String href, String iconHtml, String label, boolean paksaTampil) {
		try {
			StringBuffer js = new StringBuffer();
			js.append("(function(){try{var id='aisModeTampilanBtn';");
			js.append("var old=document.getElementById(id);if(old&&old.parentNode){old.parentNode.removeChild(old);}");
			// CSS selalu dibuat ulang agar status paksaTampil terbaru berlaku (idempoten penuh).
			js.append("var oc=document.getElementById('aisModeTampilanCss');if(oc&&oc.parentNode){oc.parentNode.removeChild(oc);}");
			js.append("var st=document.createElement('style');");
			js.append("st.id='aisModeTampilanCss';st.innerHTML='#aisModeTampilanBtn{position:fixed;left:10px;bottom:14px;");
			js.append("z-index:2147483647;background:linear-gradient(135deg,#0f172a,#1e293b);color:#fff;");
			js.append("font:700 12px Arial,Helvetica,sans-serif;padding:8px 13px;border-radius:999px;");
			js.append("text-decoration:none;box-shadow:0 6px 18px rgba(0,0,0,.35);");
			js.append("border:1px solid rgba(255,255,255,.25);white-space:nowrap;}");
			if (!paksaTampil) {
				js.append("@media(min-width:981px){#aisModeTampilanBtn{display:none!important;}}");
			}
			js.append("';");
			js.append("document.head.appendChild(st);");
			js.append("var a=document.createElement('a');a.id=id;");
			js.append("a.href='").append(href).append("';");
			js.append("a.innerHTML='").append(iconHtml).append(" ").append(label).append("';");
			js.append("document.body.appendChild(a);}catch(e){}})();");
			Clients.evalJavaScript(js.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/ModeTampilanUtil.java:104");
		}
	}
}
