package ais.action.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Filter servlet global yang membungkus setiap {@link HttpServletResponse} agar header bernama
 * {@code "Authorization"} tidak pernah benar-benar ditulis ke response (dicek tanpa membedakan
 * besar-kecil huruf), sambil mencetak setiap nama+nilai header yang coba disetel ke
 * {@link System#out}.
 *
 * <p>
 * <b>Catatan keamanan (dilaporkan, tidak diperbaiki sesuai instruksi tugas):</b> perilaku ini
 * mencurigakan dan berpotensi berisiko. Pertama, method {@link System#out} mencetak
 * <i>seluruh</i> pasangan nama/nilai header response ke log server tanpa penyaringan — bila ada
 * header response lain yang membawa data sensitif (token, cookie sesi, dsb.), nilainya akan
 * tercatat polos di log aplikasi. Kedua, filter ini secara aktif memblokir header
 * {@code Authorization} agar tidak pernah terkirim pada response apa pun yang melewati filter
 * ini, tanpa penjelasan/komentar tentang alasannya di kode — perilaku ini tidak lazim untuk
 * filter response biasa dan berpotensi menyembunyikan mekanisme otorisasi/kredensial yang
 * disengaja maupun tidak. Tidak ditemukan kredensial (password/API key) tertanam langsung di
 * kelas ini, namun pola pemblokiran+pencatatan header ini layak ditinjau ulang oleh pemilik
 * modul untuk memastikan tidak ada kebocoran data atau celah keamanan yang tidak disengaja.
 * </p>
 */
public class FilterOtto implements Filter {

	/** Tidak melakukan pembersihan sumber daya khusus saat filter dihentikan (implementasi kosong). */
	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	/**
	 * Meneruskan permintaan ke {@code chain} berikutnya dengan {@code response} dibungkus agar
	 * setiap panggilan {@code setHeader} dicatat ke {@link System#out} dan header
	 * {@code "Authorization"} secara khusus tidak diteruskan ke response asli (lihat catatan
	 * keamanan pada javadoc kelas).
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		chain.doFilter(request, new HttpServletResponseWrapper((HttpServletResponse) response) {
			public void setHeader(String name, String value) {
				System.out.println("name -> " + name + ", value -> " + value);
				if (!name.equalsIgnoreCase("Authorization")) {
					super.setHeader(name, value);
				}
			}
		});
	}

	/** Tidak melakukan inisialisasi khusus saat filter dimuat (implementasi kosong). */
	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub

	}

}