package ais.ui.render;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.helper.ProsesBayarCheckboxHelper;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DetailKegiatan;
import ais.database.model.JadwalPembayaran;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;

/**
 * Subclass khusus mode Wizard. Kelas induk
 * {@link DetailPembayaranMahasiswaRenderer} TIDAK diubah — dipakai apa adanya.
 * Subclass ini menambah pasca-proses tiap baris: aksi "Proses Bayar" disajikan
 * sebagai <b>toggle ON/OFF modern</b> pada <b>kolom paling kanan</b> (sel ke-5).
 *
 * <ul>
 * <li><b>ON</b> → memicu {@code onClick} tombol asli (logika seleksi lama dipakai).</li>
 * <li><b>OFF</b> → melepas tagihan: kosongkan baris cicilan yang cocok + buang dari
 * set terpilih {@code det} (public) + {@code hitungUlang()} (public).</li>
 * </ul>
 *
 * Bila {@code konversiCheckbox=false}, perilaku 100% sama dengan induk.
 *
 * Kompatibel Java 1.7 dan ZK 5 CE.
 */
public class ProsesBayarCheckboxRenderer extends DetailPembayaranMahasiswaRenderer {

	private final boolean konversiCheckbox;
	private final Grid gridCicilanRef;

	public ProsesBayarCheckboxRenderer(Kegiatan kegiatan, JadwalPembayaran jadwalPembayaran, Label labelFooterTagihan,
			Label labelFooterDibayar, Label labelFooterKekurangan, Label terbilang, Label terbilangTagihan,
			Label terbilangSisa, Label terbilangSisaPersen, List<MyDoubleboxMin> pengurangan,
			EventListener eventListener, Grid gridCicilan, Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			Integer semester, String tahunAkademik, Map<Long, Double> dataTagihan, Grid currentGrid,
			Collection<DetailKegiatan> detailKegiatans, EventListener refrsh, boolean bolehEditTagihan,
			boolean konversiCheckbox) {
		super(kegiatan, jadwalPembayaran, labelFooterTagihan, labelFooterDibayar, labelFooterKekurangan, terbilang,
				terbilangTagihan, terbilangSisa, terbilangSisaPersen, pengurangan, eventListener, gridCicilan, mahasiswa,
				biodataCalonMahasiswa, semester, tahunAkademik, dataTagihan, currentGrid, detailKegiatans, refrsh,
				bolehEditTagihan);
		this.konversiCheckbox = konversiCheckbox;
		this.gridCicilanRef = gridCicilan;
	}

	@Override
	public void render(Row rowPembayaran, Object arg1) throws Exception {
		super.render(rowPembayaran, arg1);
		if (!konversiCheckbox || rowPembayaran == null) {
			return;
		}
		try {
			// Sel toggle "Bayar?" diletakkan di PALING KIRI (sel ke-1) agar mahasiswa
			// langsung melihatnya tanpa harus menggulir tabel ke kanan (krusial di HP).
			// super.render() sudah menambah sel induk; sisipkan sel ini sebelum sel pertama.
			Div sel = new Div();
			sel.setStyle("display:flex;align-items:center;justify-content:center;");
			rowPembayaran.insertBefore(sel, rowPembayaran.getFirstChild());

			Object addItem = rowPembayaran.getAttribute("add_item");
			if (addItem instanceof HtmlBasedComponent) {
				final HtmlBasedComponent tombol = (HtmlBasedComponent) addItem;
				if (tombol.getParent() != null && tombol.isVisible()) {
					final Row rowRef = rowPembayaran;
					ProsesBayarCheckboxHelper.pasang(sel, tombol, new ProsesBayarCheckboxHelper.Aksi() {
						@Override
						public void jalankan() throws Exception {
							matikanItem(rowRef);
						}
					});
				}
				// Rapikan sel Item Biaya di Wizard: sembunyikan HBOX AKSI (induk tombol asli)
				// yang memuat indikator revisi "T" + tombol "Tgl, Uraian, dan Denda"/"Simpan".
				// Tombol asli tetap berfungsi via postEvent meski induknya disembunyikan.
				if (tombol.getParent() instanceof HtmlBasedComponent) {
					((HtmlBasedComponent) tombol.getParent()).setVisible(false);
				}
			}
			// Sembunyikan SISA klutter di baris: link revisi (A, mis. "T"/"P") & tombol
			// (Toolbarbutton edit/simpan) di luar hbox aksi. Label nama item & nominal (Label),
			// serta toggle (Checkbox) TIDAK terdampak → mahasiswa fokus memilih item.
			sembunyikanRevisiDanTombol(rowPembayaran);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/ProsesBayarCheckboxRenderer.java:96");
			// konversi UI bersifat opsional; jangan ganggu render induk.
		}
	}

	/** Sembunyikan secara rekursif semua link revisi ({@code A}) &amp; tombol toolbar dalam baris. */
	private void sembunyikanRevisiDanTombol(org.zkoss.zk.ui.Component c) {
		try {
			for (Object o : new java.util.ArrayList<Object>(c.getChildren())) {
				org.zkoss.zk.ui.Component child = (org.zkoss.zk.ui.Component) o;
				if ((child instanceof org.zkoss.zul.A || child instanceof org.zkoss.zul.Toolbarbutton)
						&& !Boolean.TRUE.equals(child.getAttribute("editNominalTagihan"))) {
					if (child instanceof HtmlBasedComponent) {
						((HtmlBasedComponent) child).setVisible(false);
					}
				} else {
					sembunyikanRevisiDanTombol(child);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/ProsesBayarCheckboxRenderer.java:114");
			// abaikan
		}
	}

	/**
	 * Lepas tagihan dari pembayaran (saat toggle OFF): cari baris cicilan yang
	 * item-nya cocok lalu nolkan nominalnya + picu event lama, buang dari set
	 * {@code det}, dan hitung ulang total. Memakai logika/komponen lama (aman).
	 */
	@SuppressWarnings("unchecked")
	private void matikanItem(Row rowRef) {
		try {
			Object myVal = (rowRef != null) ? rowRef.getAttribute("myValue") : null;
			ais.database.model.DetailBiaya db = (myVal instanceof ais.database.model.DetailBiaya)
					? (ais.database.model.DetailBiaya) myVal
					: null;

			if (db != null && gridCicilanRef != null && gridCicilanRef.getRows() != null) {
				for (Object ro : gridCicilanRef.getRows().getChildren()) {
					if (!(ro instanceof Row)) {
						continue;
					}
					Row r = (Row) ro;
					Object icObj = r.getAttribute("itemBiaya");
					Object jmlObj = r.getAttribute("jumlahCicilan");
					if (!(icObj instanceof Combobox) || !(jmlObj instanceof MyDoublebox)) {
						continue;
					}
					Combobox ic = (Combobox) icObj;
					if (ic.getSelectedItem() == null) {
						continue;
					}
					Object val = ic.getSelectedItem().getValue();
					if (val instanceof ais.database.model.DetailBiaya
							&& ((ais.database.model.DetailBiaya) val).getId().equals(db.getId())) {
						MyDoublebox jml = (MyDoublebox) jmlObj;
						jml.setValue(0.0);
						Object ev = jml.getAttribute("jumlahCicilanEventListener");
						if (ev instanceof EventListener) {
							((EventListener) ev).onEvent(null);
						}
					}
				}
				// det = set item terpilih (public di induk); buang item ini.
				try {
					det.remove(db.getId());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/ProsesBayarCheckboxRenderer.java:161");
				}
			}
			hitungUlang();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/render/ProsesBayarCheckboxRenderer.java:165");
			// best-effort; jangan ganggu UI.
		}
	}
}
