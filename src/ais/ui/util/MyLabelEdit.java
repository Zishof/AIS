package ais.ui.util;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

/**
 * Komponen/konfigurasi ZK khusus AIS untuk my label edit. Tipe ini membakukan default dan perilaku
 * tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Vbox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code int MAKS_TAMPIL}, {@code Textbox
 * keterangan}, {@code String value}, {@code EventListener eventListener}, {@code MyLabelAgakKecil ketComp},
 * {@code Label baca}; inisialisasi/lifecycle ({@code init()}); pembacaan/pencarian ({@code addEventListener()},
 * {@code getValue()}); mutasi data ({@code setWidth()}, {@code setCols()}, {@code setRows()}, {@code
 * setValue()}); operasi domain lain ({@code terapkanRingkas()}, {@code perluRingkas()}, {@code ringkas()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Vbox
 */
public class MyLabelEdit extends Vbox {

	private static final int MAKS_TAMPIL = 25;

	private Textbox keterangan;

	/**
	 * 
	 */
	private static final long serialVersionUID = 8361456606647472897L;
	private String value;

	private EventListener eventListener;

	private MyLabelAgakKecil ketComp;
	private Label baca;

	public MyLabelEdit() {
		super();
		init();
	}

	public MyLabelEdit(String value) {
		super();
		this.value = value;
		init();
	}

	public MyLabelEdit(Component[] children) {
		super(children);
		init();
	}

	public boolean addEventListener(String event, EventListener eventListener) {
		this.eventListener = eventListener;
		return true;
	}

	private void init() {
		keterangan = new Textbox(value);
		keterangan.setWidth("90%");
		keterangan.setParent(this);
		keterangan.setVisible(false);

		Hbox ringkasBox = new Hbox();
		ringkasBox.setStyle("align-items:flex-start;gap:4px;");
		ringkasBox.setParent(this);
		ketComp = new MyLabelAgakKecil("");
		ketComp.setParent(ringkasBox);
		baca = new Label("baca");
		baca.setStyle("color:#2563eb;cursor:pointer;font-size:11px;font-weight:bold;text-decoration:underline;");
		baca.setParent(ringkasBox);
		baca.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				String lengkap = keterangan.getValue() == null ? "" : keterangan.getValue();
				MyMessageboxConfig.show(lengkap, "Teks Lengkap", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
			}
		});
		terapkanRingkas(value);
		final MyToolbarbuttonConfig buttonSelesai = new MyToolbarbuttonConfig("Selesai", "/img/save.gif");
		final MyToolbarbuttonConfig buttonUbah = new MyToolbarbuttonConfig("Ubah", "/img/edit-icon.png");

		buttonSelesai.setTooltiptext("Simpan Data");
		buttonSelesai.setVisible(false);
		buttonSelesai.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				buttonSelesai.setVisible(false);
				buttonUbah.setVisible(true);

				keterangan.setVisible(false);
				terapkanRingkas(keterangan.getValue());
				ketComp.setVisible(true);
				baca.setVisible(perluRingkas(keterangan.getValue()));

				if (eventListener != null) {
					eventListener.onEvent(event);
				}
			}

		});
		buttonSelesai.setParent(this);

		buttonUbah.setTooltiptext("Ubah Data");
		buttonUbah.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				buttonSelesai.setVisible(true);
				buttonUbah.setVisible(false);

				keterangan.setVisible(true);
				ketComp.setVisible(false);
			}

		});
		buttonUbah.setParent(this);

	}

	public void setWidth(String widht) {
		keterangan.setWidth(widht);
	}

	public void setCols(int cols) {
		keterangan.setCols(cols);
	}

	public void setRows(int rows) {
		keterangan.setRows(rows);
	}

	public String getValue() {
		return keterangan.getValue();
	}

	public void setValue(String value) {
		keterangan.setValue(value);
		terapkanRingkas(value);
	}

	private void terapkanRingkas(String teks) {
		String isi = teks == null ? "" : teks.trim();
		ketComp.setValue(ringkas(isi));
		ketComp.setTooltiptext(isi);
		baca.setVisible(perluRingkas(isi));
	}

	private boolean perluRingkas(String teks) {
		return teks != null && teks.trim().length() > MAKS_TAMPIL;
	}

	private String ringkas(String teks) {
		if (!perluRingkas(teks)) {
			return teks == null ? "" : teks;
		}
		return teks.trim().substring(0, MAKS_TAMPIL) + "...";
	}
}
