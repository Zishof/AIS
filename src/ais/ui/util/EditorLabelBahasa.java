package ais.ui.util;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Button;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.common.ErrorAuditUtil;
import ais.common.LabelBahasaHelper;

/**
 * {@code EditorLabelBahasa} — penyunting teks/label multi-bahasa yang dapat dibuka
 * <b>langsung dari tempat teksnya muncul</b>, khusus untuk administrator.
 *
 * <h3>Untuk apa</h3>
 * <p>Kalimat yang janggal, salah ketik, atau salah terjemah paling mudah dikenali justru
 * saat kalimat itu tampil di layar. Sebelumnya perbaikannya harus lewat menu Konfigurasi
 * terpisah — administrator perlu mengingat kalimatnya, mencarinya, lalu menebak baris mana
 * yang benar. Dengan penyunting ini, tombol perbaikan berada tepat pada dialog yang sedang
 * dibaca, dan kuncinya diturunkan otomatis sehingga tidak mungkin salah baris.</p>
 *
 * <h3>Empat bahasa</h3>
 * <p>Sesuai dukungan aplikasi saat ini: Indonesia, English, Arabic, dan Mandarin. Tersedia
 * tombol <b>Terjemahkan Otomatis</b> yang mengisi ketiga bahasa non-Indonesia dari kalimat
 * Indonesia, memakai mesin yang sama dengan penerjemah latar
 * ({@code AiTerjemah} — server AI bila siap, kamus internal bila tidak). Hasilnya
 * <i>tetap dapat disunting</i> sebelum disimpan: terjemahan mesin adalah titik awal, bukan
 * keputusan akhir.</p>
 *
 * <h3>Batas akses</h3>
 * <p>Seluruh titik masuk memeriksa {@link Common#getApakahAdmin()}. Pemeriksaan dilakukan di
 * dalam kelas ini juga — bukan hanya di pemanggil — supaya tombol yang tidak sengaja
 * ditampilkan pada layar lain tetap tidak dapat mengubah apa pun.</p>
 *
 * <p>Kompatibilitas: Java 1.6 (tanpa lambda, diamond, try-with-resources, atau Stream).</p>
 */
public final class EditorLabelBahasa {

	private EditorLabelBahasa() {
	}

	private static final String GAYA_TOMBOL_UTAMA =
			"border:1px solid #1d4ed8;background:#1d4ed8;color:#ffffff;border-radius:8px;"
			+ "padding:6px 14px;font-weight:800;";
	private static final String GAYA_TOMBOL_BIASA =
			"border:1px solid #cbd5e1;background:#ffffff;color:#334155;border-radius:8px;"
			+ "padding:6px 12px;font-weight:700;";
	private static final String GAYA_ISIAN =
			"box-sizing:border-box;font-size:13px;line-height:1.45;border:1px solid #cbd5e1;"
			+ "border-radius:6px;padding:6px;";

	/** Apakah pengguna saat ini boleh menyunting label. Gagal-aman: menolak bila ragu. */
	public static boolean bolehMenyunting() {
		try {
			return Common.getApakahAdmin();
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Buka penyunting untuk sebuah teks.
	 *
	 * @param teksAsli teks default (bahasa Indonesia) sebagaimana ditulis di kode — INI yang
	 *                 menentukan kunci kamus, bukan teks hasil terjemahan yang sedang tampil
	 *                 di layar. Mengirim teks hasil terjemahan akan membuat baris baru yang
	 *                 tidak pernah terbaca.
	 */
	public static void buka(final String teksAsli) {
		if (!bolehMenyunting()) {
			return;
		}
		if (teksAsli == null || teksAsli.trim().length() == 0) {
			return;
		}
		try {
			final String kunci = LabelBahasaHelper.kunci(teksAsli);
			final String[] nilai = LabelBahasaHelper.ambilTerjemahan(kunci);

			final Window win = new Window();
			win.setTitle("");
			win.setBorder("none");
			win.setClosable(true);
			win.setSizable(false);
			win.setWidth("560px");
			win.setStyle("border-radius:14px;overflow:hidden;background:#ffffff;"
					+ "border:1px solid #e5e7eb;box-shadow:0 24px 70px rgba(15,23,42,.28);");
			win.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

			Vbox badan = new Vbox();
			badan.setWidth("100%");
			badan.setSpacing("0");
			badan.setStyle("font-family:Arial,sans-serif;");
			badan.setParent(win);

			new Html("<div style='background:#1d4ed8;color:#fff;padding:14px 18px;'>"
					+ "<div style='font-size:15px;font-weight:800;'>Ubah Teks / Terjemahan</div>"
					+ "<div style='font-size:12px;opacity:.9;margin-top:2px;'>"
					+ "Perubahan berlaku untuk seluruh aplikasi, bukan hanya layar ini.</div></div>")
					.setParent(badan);

			Vbox isi = new Vbox();
			isi.setWidth("100%");
			isi.setSpacing("10px");
			isi.setStyle("box-sizing:border-box;padding:16px 18px 8px;");
			isi.setParent(badan);

			Label kunciLabel = new Label("Kunci kamus: " + kunci);
			kunciLabel.setStyle("font-size:11px;color:#64748b;");
			kunciLabel.setParent(isi);

			// Bila kolom Indonesia masih kosong di basis data, isi dengan teks asli dari kode
			// supaya penerjemahan otomatis punya sumber dan admin tidak perlu mengetik ulang.
			final Textbox tIndonesia = isian(isi, "Bahasa Indonesia",
					nilai[LabelBahasaHelper.INDONESIA].length() > 0
							? nilai[LabelBahasaHelper.INDONESIA] : teksAsli.trim());
			final Textbox tEnglish = isian(isi, "English", nilai[LabelBahasaHelper.ENGLISH]);
			final Textbox tArab = isian(isi, "Arabic", nilai[LabelBahasaHelper.ARAB]);
			final Textbox tMandarin = isian(isi, "Mandarin", nilai[LabelBahasaHelper.MANDARIN]);

			final Label status = new Label("");
			status.setStyle("font-size:12px;color:#166534;");
			status.setParent(isi);

			Hbox kaki = new Hbox();
			kaki.setWidth("100%");
			kaki.setSpacing("8px");
			kaki.setPack("end");
			kaki.setStyle("box-sizing:border-box;padding:12px 18px 16px;background:#f9fafb;"
					+ "border-top:1px solid #e5e7eb;");
			kaki.setParent(badan);

			final Button terjemah = new Button("Terjemahkan Otomatis");
			terjemah.setStyle(GAYA_TOMBOL_BIASA);
			terjemah.setTooltiptext("Isi English, Arabic, dan Mandarin dari kalimat Bahasa Indonesia di atas");
			terjemah.setParent(kaki);
			terjemah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					String sumber = tIndonesia.getValue();
					if (sumber == null || sumber.trim().length() == 0) {
						status.setStyle("font-size:12px;color:#b91c1c;");
						status.setValue("Isi dulu kalimat Bahasa Indonesia-nya.");
						return;
					}
					terjemah.setDisabled(true);
					terjemah.setLabel("Menerjemahkan...");
					try {
						isiBilaAda(tEnglish, LabelBahasaHelper.terjemahOtomatis(sumber, "english"));
						isiBilaAda(tArab, LabelBahasaHelper.terjemahOtomatis(sumber, "arab"));
						isiBilaAda(tMandarin, LabelBahasaHelper.terjemahOtomatis(sumber, "mandarin"));
						status.setStyle("font-size:12px;color:#166534;");
						status.setValue("Terjemahan diisikan. Silakan periksa dan perbaiki bila perlu, "
								+ "lalu tekan Simpan.");
					} finally {
						terjemah.setDisabled(false);
						terjemah.setLabel("Terjemahkan Otomatis");
					}
				}
			});

			Button batal = new Button("Batal");
			batal.setStyle(GAYA_TOMBOL_BIASA);
			batal.setParent(kaki);
			batal.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					tutup(win);
				}
			});

			Button simpan = new Button("Simpan");
			simpan.setStyle(GAYA_TOMBOL_UTAMA);
			simpan.setParent(kaki);
			simpan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					boolean ok = LabelBahasaHelper.simpan(kunci, tIndonesia.getValue(), tEnglish.getValue(),
							tArab.getValue(), tMandarin.getValue());
					if (ok) {
						tutup(win);
						MyMessageboxConfig.show("Teks berhasil diperbarui. Perubahan berlaku di seluruh "
								+ "aplikasi; halaman yang sedang terbuka mungkin perlu dimuat ulang "
								+ "untuk menampilkannya.");
					} else {
						status.setStyle("font-size:12px;color:#b91c1c;");
						status.setValue("Teks gagal disimpan. Rinciannya tercatat pada audit galat.");
					}
				}
			});

			win.doModal();
		} catch (Throwable t) {
			ErrorAuditUtil.record(t, "EditorLabelBahasa.buka teks=" + teksAsli);
		}
	}

	// =========================================================
	// Util internal
	// =========================================================

	private static Textbox isian(Vbox induk, String judul, String nilai) {
		Label l = new Label(judul);
		l.setStyle("font-size:12px;font-weight:700;color:#334155;");
		l.setParent(induk);
		Textbox t = new Textbox(nilai == null ? "" : nilai);
		t.setMultiline(true);
		t.setRows(2);
		t.setWidth("100%");
		t.setStyle(GAYA_ISIAN);
		t.setParent(induk);
		return t;
	}

	/**
	 * Isi kotak hanya bila terjemahan menghasilkan sesuatu.
	 *
	 * <p>Sengaja TIDAK mengosongkan isian yang sudah ada ketika penerjemah gagal: hasil
	 * suntingan manusia tidak boleh hilang hanya karena server AI sedang tidak siap.</p>
	 */
	private static void isiBilaAda(Textbox kotak, String hasil) {
		if (hasil != null && hasil.trim().length() > 0) {
			kotak.setValue(hasil.trim());
		}
	}

	private static void tutup(Window win) {
		try {
			win.onClose();
		} catch (Throwable t) {
			try {
				win.detach();
			} catch (Throwable abaikan) {
				ErrorAuditUtil.record(abaikan, "EditorLabelBahasa.tutup");
			}
		}
	}
}
