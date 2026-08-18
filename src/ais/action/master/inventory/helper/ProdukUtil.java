package ais.action.master.inventory.helper;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;

import ais.database.model.file.FotoGambarProduk;
import ais.database.model.inventory.Produk;

public class ProdukUtil {
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
