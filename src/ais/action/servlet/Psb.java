package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.model.PerguruanTinggi;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Servlet titik masuk (entry point) modul PSB (Penerimaan Siswa Baru/Penerimaan Mahasiswa
 * Baru), dipetakan ke URL {@code /Psb} (lihat {@code web.xml}). Kelas ini murni router
 * tampilan: satu-satunya keputusan yang diambil adalah memilih implementasi layar PSB yang
 * dirender berdasarkan preferensi "pilihan tampilan" domain (perguruan tinggi/sekolah/yayasan
 * yang sedang diakses) — layar baru ({@code /WEB-INF/new/index.jsp}, dengan atribut
 * {@code new_context=psb}) bila {@link PerguruanTinggi#TAMPILAN_BARU} dipilih, atau layar lama
 * berbasis ZK ({@code /WEB-INF/z/x/y/psb.zul}) sebagai default/fallback. Tidak menyentuh basis
 * data untuk data PSB itu sendiri; seluruh logika pendaftaran ditangani oleh composer ZK/handler
 * pada layar yang dituju.
 */
public class Psb extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet; tidak melakukan inisialisasi
	 * khusus.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public Psb() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan ke {@link #process}; kegagalan
	 * ditangkap dan hanya ditampilkan ke administrator lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}, tidak dilempar ke kontainer.
	 *
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
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
	 * Menangani permintaan HTTP POST dengan perilaku identik {@link #doGet}.
	 *
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
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
	 * Meneruskan (forward) permintaan ke layar PSB yang sesuai: bila
	 * {@link #getPiilhanTampilanDomain(HttpServletRequest)} mengembalikan
	 * {@link PerguruanTinggi#TAMPILAN_BARU}, menyetel atribut request {@code new_context=psb}
	 * lalu forward ke {@code /WEB-INF/new/index.jsp} (antarmuka baru); selain itu forward ke
	 * ZUL lama {@code /WEB-INF/z/x/y/psb.zul}.
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String piilhan = getPiilhanTampilanDomain(request);
		if (PerguruanTinggi.TAMPILAN_BARU.equals(piilhan)) {
			request.setAttribute("new_context", "psb");
			request.getRequestDispatcher("/WEB-INF/new/index.jsp").forward(request, response);
		} else {
			request.getRequestDispatcher("/WEB-INF/z/x/y/psb.zul").forward(request, response);
		}
	}

	/**
	 * Menentukan kunci "pilihan tampilan" (mis. {@link PerguruanTinggi#TAMPILAN_BARU}) yang
	 * berlaku untuk domain/institusi yang sedang diakses, dengan urutan prioritas: (1)
	 * konfigurasi {@link PerguruanTinggi} terkait request bila bukan
	 * {@link PerguruanTinggi#TAMPILAN_DEFAULT}; (2) bila mode sekolah aktif
	 * ({@link Common#chekPtAtauSekolah()}), konfigurasi {@link Sekolah} lalu {@link Yayasan}
	 * terkait, masing-masing bila bukan nilai default. Mengembalikan
	 * {@link PerguruanTinggi#TAMPILAN_DEFAULT} bila tidak ada override yang berlaku atau
	 * terjadi exception apa pun (ditelan senyap agar servlet tetap bisa merender layar default).
	 *
	 * @param request permintaan masuk, dipakai untuk meresolusi perguruan tinggi/sekolah/yayasan
	 * @return kunci pilihan tampilan yang berlaku, tidak pernah {@code null}
	 */
	private String getPiilhanTampilanDomain(HttpServletRequest request) {
		try {
			PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
			if (pt != null && !PerguruanTinggi.TAMPILAN_DEFAULT.equals(pt.getPiilhanTampilan())) {
				return pt.getPiilhanTampilan();
			}
			boolean[] ptAtauSekolah = Common.chekPtAtauSekolah();
			boolean sekolahMode = ptAtauSekolah != null && ptAtauSekolah.length > 1 && ptAtauSekolah[1];
			if (sekolahMode) {
				Sekolah sekolah = SekolahUtil.getSekolah(request);
				if (sekolah != null && !Sekolah.TAMPILAN_DEFAULT.equals(sekolah.getPiilhanTampilan())) {
					return sekolah.getPiilhanTampilan();
				}
				Yayasan yayasan = SekolahUtil.getYayasan(request);
				if (yayasan != null && !Yayasan.TAMPILAN_DEFAULT.equals(yayasan.getPiilhanTampilan())) {
					return yayasan.getPiilhanTampilan();
				}
			}
		} catch (Exception e) {
			// ignore
		}
		return PerguruanTinggi.TAMPILAN_DEFAULT;
	}

}
