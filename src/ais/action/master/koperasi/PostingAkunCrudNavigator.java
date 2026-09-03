package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

import ais.common.Common;

/**
 * Tombol pintas dari seluruh pratinjau posting ke CRUD master sumber akun.
 *
 * <p>Jurnal hasil pratinjau tidak diedit langsung. Pengguna diarahkan ke master
 * yang menentukan sisi Debet/Kredit, sehingga perbaikannya berlaku konsisten
 * untuk transaksi lain. Halaman tujuan tetap menjalankan pemeriksaan hak akses
 * CRUD-nya sendiri.</p>
 */
public final class PostingAkunCrudNavigator {

	private static final class Target {
		private final String id;
		private final String label;
		private final String keterangan;
		private final String path;

		private Target(String id, String label, String keterangan, String path) {
			this.id = id;
			this.label = label;
			this.keterangan = keterangan;
			this.path = path;
		}
	}

	private static final Target CARA_BAYAR = new Target("cara_bayar", "Cara Pembayaran",
			"Atur akun Kas/Bank untuk metode pembayaran.", "/pages/master/koperasi/cara_pembayaran_koperasi.zul");
	private static final Target JENIS_PRODUK = new Target("jenis_produk", "Jenis Produk",
			"Atur akun Pendapatan, PPN, HPP, retur, atau selisih stok.", "/pages/master/inventory/jenis_produk.zul");
	private static final Target SUPPLIER = new Target("supplier", "Supplier (Penyedia)",
			"Atur akun Utang Dagang supplier.", "/pages/master/inventory/penyedia.zul");
	private static final Target TOKO = new Target("toko", "Toko / Outlet",
			"Atur akun Kas/Bank atau Piutang Usaha toko.", "/pages/master/inventory/toko.zul");
	private static final Target MASTER_ASET = new Target("master_aset", "Master Aset / Persediaan",
			"Atur akun Pembelian/Persediaan pada master aset barang.", "/pages/master/asset/master_asset.zul");
	private static final Target KELOMPOK_ASET = new Target("kelompok_aset", "Kelompok Aset",
			"Atur akun Persediaan atau HPP bawaan kelompok aset.", "/pages/master/asset/kelompok_asset.zul");

	private PostingAkunCrudNavigator() {
	}

	public static void tambahkan(Hlayout wadah, String jenis) {
		if (wadah == null) {
			return;
		}
		wadah.appendChild(tombol(jenis, true, ""));
		wadah.appendChild(tombol(jenis, false, ""));
	}

	public static Hlayout panel(String jenis) {
		return panel(jenis, "");
	}

	public static Hlayout panel(String jenis, String alasan) {
		Hlayout panel = new Hlayout();
		panel.setStyle("gap:4px;flex-wrap:wrap;");
		panel.appendChild(tombol(jenis, true, alasan));
		panel.appendChild(tombol(jenis, false, alasan));
		return panel;
	}

	private static Toolbarbutton tombol(final String jenis, final boolean debet, final String alasan) {
		String label = debet ? "Sesuaikan Akun Debet" : "Sesuaikan Akun Kredit";
		final List<Target> daftar = tujuan(jenis, debet, alasan);
		Toolbarbutton tombol = new Toolbarbutton(label);
		tombol.setStyle("color:#1d4ed8;font-weight:600;cursor:pointer;font-size:11px;");
		tombol.setTooltiptext(petunjuk(daftar));
		tombol.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (daftar.size() == 1) {
					buka(daftar.get(0));
				} else {
					pilih(event, debet, daftar);
				}
			}
		});
		return tombol;
	}

	private static List<Target> tujuan(String jenis, boolean debet, String alasan) {
		String j = jenis == null ? "" : jenis.trim().toLowerCase();
		List<Target> hasil;
		if ("penjualan".equals(j)) {
			hasil = Arrays.asList(debet ? CARA_BAYAR : JENIS_PRODUK);
		} else if ("hpp".equals(j)) {
			hasil = debet ? Arrays.asList(JENIS_PRODUK, KELOMPOK_ASET)
					: Arrays.asList(MASTER_ASET, KELOMPOK_ASET);
		} else if ("kulakan".equals(j)) {
			hasil = debet ? Arrays.asList(MASTER_ASET, KELOMPOK_ASET)
					: Arrays.asList(SUPPLIER, TOKO, CARA_BAYAR);
		} else if ("bayar_hutang".equals(j)) {
			hasil = debet ? Arrays.asList(SUPPLIER) : Arrays.asList(CARA_BAYAR, TOKO);
		} else if ("terima_piutang".equals(j)) {
			hasil = debet ? Arrays.asList(CARA_BAYAR, TOKO) : Arrays.asList(TOKO, CARA_BAYAR);
		} else {
			hasil = Arrays.asList(JENIS_PRODUK, MASTER_ASET, KELOMPOK_ASET, TOKO, CARA_BAYAR);
		}

		String a = alasan == null ? "" : alasan.toLowerCase();
		if (a.length() == 0 || hasil.size() == 1) {
			return hasil;
		}
		List<Target> tersaring = new ArrayList<Target>();
		for (Target target : hasil) {
			if (cocokAlasan(target, a)) {
				tersaring.add(target);
			}
		}
		return tersaring.isEmpty() ? hasil : tersaring;
	}

	private static boolean cocokAlasan(Target target, String alasan) {
		if (alasan.contains("cara pembayaran") || alasan.contains("metode")) {
			return "cara_bayar".equals(target.id);
		}
		if (alasan.contains("utang supplier") || alasan.contains("penyedia")) {
			return "supplier".equals(target.id);
		}
		if (alasan.contains("piutang") || alasan.contains("kas/bank toko")) {
			return "toko".equals(target.id);
		}
		if (alasan.contains("jenis produk") || alasan.contains("pendapatan") || alasan.contains("ppn")
				|| (alasan.contains("hpp") && !alasan.contains("persediaan"))) {
			return "jenis_produk".equals(target.id);
		}
		if (alasan.contains("persediaan") || alasan.contains("master aset")) {
			return "master_aset".equals(target.id) || "kelompok_aset".equals(target.id);
		}
		return false;
	}

	private static String petunjuk(List<Target> daftar) {
		StringBuilder teks = new StringBuilder("Buka CRUD sumber akun: ");
		for (int i = 0; i < daftar.size(); i++) {
			if (i > 0) {
				teks.append(", ");
			}
			teks.append(daftar.get(i).label);
		}
		return teks.append('.').toString();
	}

	private static void pilih(Event event, boolean debet, List<Target> daftar) throws Exception {
		final Window dialog = new Window();
		dialog.setTitle(debet ? "Sesuaikan Akun Debet" : "Sesuaikan Akun Kredit");
		dialog.setBorder("normal");
		dialog.setClosable(true);
		dialog.setWidth("560px");
		dialog.setSizable(false);

		Vlayout isi = new Vlayout();
		isi.setStyle("padding:12px;gap:8px;");
		isi.appendChild(new Label("Pilih master sumber akun yang perlu diperbaiki. Hak akses CRUD tetap diperiksa oleh halaman tujuan."));
		for (final Target target : daftar) {
			Toolbarbutton pilihan = new Toolbarbutton(target.label + " — " + target.keterangan);
			pilihan.setStyle("display:block;padding:9px;border:1px solid #cbd5e1;border-radius:6px;color:#1d4ed8;");
			pilihan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					dialog.detach();
					buka(target);
				}
			});
			isi.appendChild(pilihan);
		}
		dialog.appendChild(isi);
		dialog.setParent(event.getTarget().getRoot());
		dialog.doModal();
	}

	private static void buka(Target target) {
		Executions.getCurrent().sendRedirect(Common.ROOT + target.path, "_blank");
	}
}
