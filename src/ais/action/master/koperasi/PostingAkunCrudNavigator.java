package ais.action.master.koperasi;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Toolbarbutton;

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

	private PostingAkunCrudNavigator() {
	}

	public static void tambahkan(Hlayout wadah, String jenis) {
		if (wadah == null) {
			return;
		}
		wadah.appendChild(tombol(jenis, true));
		wadah.appendChild(tombol(jenis, false));
	}

	public static Hlayout panel(String jenis) {
		Hlayout panel = new Hlayout();
		panel.setStyle("gap:4px;flex-wrap:wrap;");
		tambahkan(panel, jenis);
		return panel;
	}

	private static Toolbarbutton tombol(final String jenis, final boolean debet) {
		String label = debet ? "Sesuaikan Akun Debet" : "Sesuaikan Akun Kredit";
		final String tujuan = tujuan(jenis, debet);
		Toolbarbutton tombol = new Toolbarbutton(label);
		tombol.setStyle("color:#1d4ed8;font-weight:600;cursor:pointer;font-size:11px;");
		tombol.setTooltiptext(petunjuk(jenis, debet));
		tombol.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Executions.getCurrent().sendRedirect(Common.ROOT + tujuan, "_blank");
			}
		});
		return tombol;
	}

	private static String tujuan(String jenis, boolean debet) {
		String j = jenis == null ? "" : jenis.trim().toLowerCase();
		if ("penjualan".equals(j)) {
			return debet ? "/pages/master/koperasi/cara_pembayaran_koperasi.zul"
					: "/pages/master/inventory/jenis_produk.zul";
		}
		if ("hpp".equals(j)) {
			return debet ? "/pages/master/inventory/jenis_produk.zul"
					: "/pages/master/asset/master_asset.zul";
		}
		if ("kulakan".equals(j)) {
			return debet ? "/pages/master/asset/master_asset.zul"
					: "/pages/master/inventory/penyedia.zul";
		}
		if ("bayar_hutang".equals(j)) {
			return debet ? "/pages/master/inventory/penyedia.zul"
					: "/pages/master/koperasi/cara_pembayaran_koperasi.zul";
		}
		if ("terima_piutang".equals(j)) {
			return debet ? "/pages/master/koperasi/cara_pembayaran_koperasi.zul"
					: "/pages/master/inventory/toko.zul";
		}
		return debet ? "/pages/master/inventory/jenis_produk.zul"
				: "/pages/master/asset/master_asset.zul";
	}

	private static String petunjuk(String jenis, boolean debet) {
		String j = jenis == null ? "" : jenis.trim().toLowerCase();
		if ("penjualan".equals(j)) {
			return debet
					? "Buka CRUD Cara Pembayaran untuk mengisi Akun Kas/Bank metode transaksi."
					: "Buka CRUD Jenis Produk untuk mengisi akun Pendapatan/PPN Keluaran.";
		}
		if ("hpp".equals(j)) {
			return debet
					? "Buka CRUD Jenis Produk untuk mengisi Akun HPP."
					: "Buka CRUD Master Aset untuk mengisi Akun Persediaan/Pembelian.";
		}
		if ("kulakan".equals(j)) {
			return debet
					? "Buka CRUD Master Aset untuk mengisi Akun Persediaan/Pembelian."
					: "Buka CRUD Penyedia untuk mengisi Akun Utang; akun Kas dapat diperbaiki di master Toko/Cara Pembayaran.";
		}
		if ("bayar_hutang".equals(j)) {
			return debet
					? "Buka CRUD Penyedia untuk mengisi Akun Utang."
					: "Buka CRUD Cara Pembayaran untuk mengisi Akun Kas/Bank.";
		}
		if ("terima_piutang".equals(j)) {
			return debet
					? "Buka CRUD Cara Pembayaran untuk mengisi Akun Kas/Bank."
					: "Buka CRUD Toko untuk mengisi Akun Piutang Usaha.";
		}
		return "Buka CRUD master sumber akun dan simpan pemetaan yang sesuai.";
	}
}

