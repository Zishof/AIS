package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet endpoint {@code /desktop} yang memaksa tampilan aplikasi ke mode DESKTOP (bukan mode
 * HP/mobile) untuk sesi pengguna saat ini, lalu meneruskan (forward) permintaan ke
 * {@code /WEB-INF/z/x/y/pages/main/dekstop.zul}.
 *
 * <p>
 * Mekanisme deteksi mobile ({@code CommonCurrentSessionHelper.isMobile}) membaca attribute sesi
 * {@code "is_mobile"}; dengan menyetelnya ke {@link Boolean#FALSE} di sini, {@code dekstop.zul}
 * dan {@code MainAction} merender tampilan desktop penuh (baris menu modul yang tombolnya bisa
 * diklik) walaupun perangkat sebenarnya adalah HP. Perilaku ini bersifat <i>sticky</i> selama
 * sesi berlangsung sampai pengguna memilih "Mode HP" kembali (parameter {@code ?is_mobile=true}
 * pada endpoint lain); tanpa mengunjungi {@code /desktop} terlebih dahulu, akses dari HP tetap
 * dirender sebagai tampilan mobile.
 * </p>
 */
public class Desktop extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Konstruktor baku servlet, tanpa inisialisasi tambahan. */
	public Desktop() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan GET dengan mendelegasikan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}; galat ditangani oleh
	 * {@link Common#tampilErrorJikaAdmin(Exception)} agar detail teknis hanya tampil untuk admin.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menangani permintaan POST dengan mendelegasikan ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}; galat ditangani oleh
	 * {@link Common#tampilErrorJikaAdmin(Exception)} agar detail teknis hanya tampil untuk admin.
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menyetel attribute sesi {@code "is_mobile"} menjadi {@link Boolean#FALSE} (membuat/mengambil
	 * sesi bila belum ada) untuk memaksa tampilan desktop, mengabaikan setiap galat pada langkah
	 * ini agar forward tetap berlanjut, lalu meneruskan (forward) permintaan ke
	 * {@code /WEB-INF/z/x/y/pages/main/dekstop.zul}.
	 *
	 * @param request permintaan HTTP masuk
	 * @param response respons HTTP yang akan di-forward ke halaman ZUL
	 * @throws Exception bila terjadi galat saat forward ke halaman ZUL
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		/*
		 * PAKSA tampilan DESKTOP. Mekanisme deteksi mobile (CommonCurrentSessionHelper.isMobile)
		 * membaca atribut sesi "is_mobile"; dengan menyetelnya FALSE di sini, dekstop.zul
		 * dan MainAction merender tampilan desktop PENUH (baris menu modul yang tombolnya
		 * bisa diklik), walaupun perangkatnya HP. Bersifat sticky selama sesi sampai
		 * pengguna memilih "Mode HP" kembali (?is_mobile=true).
		 * Tanpa langkah ini, /desktop di HP tetap dirender sebagai tampilan mobile.
		 */
		try {
			request.getSession(true).setAttribute("is_mobile", Boolean.FALSE);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Desktop.java:65");
			// abaikan; tetap lanjut forward
		}
		request.getRequestDispatcher("/WEB-INF/z/x/y/pages/main/dekstop.zul").forward(request, response);

	}

}
