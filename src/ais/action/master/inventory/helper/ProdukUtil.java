package ais.action.master.inventory.helper;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;

import ais.database.model.file.FotoGambarProduk;
import ais.database.model.inventory.Produk;

/**
 * Utilitas kecil untuk menghasilkan komponen ZK yang menampilkan foto produk pada layar
 * inventaris. Satu-satunya method, {@link #generateImage}, mengambil foto default produk
 * (jenis {@link FotoGambarProduk#DEFAULT_JENIS}) bila tersedia, atau menampilkan label placeholder
 * "Tidak ada gambar" bila tidak ada foto.
 */
public class ProdukUtil {
	/**
	 * Menghasilkan komponen gambar untuk {@code produk}: {@link Image} berisi foto default produk
	 * bila tersedia dan valid, atau {@link Label} placeholder "Tidak ada gambar" sebagai fallback
	 * (termasuk saat {@code produk} atau id-nya {@code null}, atau saat pengambilan foto gagal —
	 * galat ditampilkan hanya untuk admin lewat {@code Common.tampilErrorJikaAdmin}).
	 *
	 * @param produk entitas produk yang fotonya akan ditampilkan, boleh {@code null}
	 * @return komponen ZK ({@link Image} atau {@link Label}) siap ditambahkan ke layar
	 * @throws Exception diteruskan dari kegagalan pembuatan komponen di luar penanganan internal
	 */
	public static Component generateImage(Produk produk) throws Exception {
		try {
			FotoGambarProduk fotoGambarProduk = (FotoGambarProduk) (produk == null || produk.getId() == null ? null
					: FotoGambarProduk.ambil(produk.getId(), FotoGambarProduk.DEFAULT_JENIS, FotoGambarProduk.class));

			if (fotoGambarProduk != null && fotoGambarProduk.getId() != null) {
				Image image = new Image(fotoGambarProduk.createLinkUri());
				image.setWidth("100%");
				image.setStyle("max-height:90px;object-fit:contain;border-radius:10px;background:#f8fafc;border:1px solid #e2e8f0;");
				return image;
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		Label kosong = new Label(ais.common.Common.getBahasaConfig("Tidak ada gambar"));
		kosong.setStyle("display:block;padding:8px;border-radius:10px;background:#f8fafc;color:#94a3b8;font-size:10.5px;text-align:center;");
		return kosong;
	}
}
