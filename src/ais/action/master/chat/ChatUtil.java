package ais.action.master.chat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Vbox;

import ais.common.CommonMedia;
import ais.database.model.Mahasiswa;
import ais.database.model.Pesan;
import ais.database.model.Tbmuser;

/**
 * Utilitas UI ZK untuk merender satu gelembung pesan (chat bubble) pada papan percakapan
 * ({@code msgBoard}) modul chat: menampilkan avatar pengirim, isi pesan, serta nama dan waktu
 * kirim.
 */
public class ChatUtil {

	/**
	 * Menambahkan satu {@link Pesan} sebagai baris baru pada {@code msgBoard} (komponen ZK), lalu
	 * menggulirkan tampilan agar baris baru tersebut terlihat. Deduplikasi dilakukan lewat
	 * atribut komponen {@code "pesanIds"} (set id pesan yang sudah pernah dirender pada komponen
	 * ini) sehingga pesan yang sama tidak dirender dua kali walau method dipanggil berulang (mis.
	 * dari polling/event berkala). Avatar diambil berjenjang: dari {@code mahasiswa} bila diisi,
	 * lalu dari relasi dosen/pegawai/calon mahasiswa pada {@code pengirim}, atau dari
	 * {@code pengirim} itu sendiri sebagai fallback terakhir.
	 *
	 * @param pesan     pesan yang akan ditampilkan
	 * @param msgBoard  komponen ZK tempat gelembung pesan ditambahkan sebagai anak
	 * @param pengirim  user pengirim; bila {@code mahasiswa} diisi, relasi mahasiswanya disetel ke objek ini
	 * @param mahasiswa data mahasiswa pengirim bila pengirim adalah mahasiswa, boleh {@code null}
	 * @param waktu     waktu pengiriman pesan, ditampilkan dalam format jam:menit AM/PM
	 * @param kirim     tidak dipakai pada implementasi saat ini
	 */
	public static void createPesanBox(Pesan pesan, Component msgBoard, Tbmuser pengirim, Mahasiswa mahasiswa,
			Date waktu, boolean kirim) throws Exception {

		@SuppressWarnings("unchecked")
		Set<Long> pesanIds = (Set<Long>) (msgBoard.getAttribute("pesanIds") == null ? new HashSet<Long>()
				: msgBoard.getAttribute("pesanIds"));

		if (pesanIds.contains(pesan.getId())) {
			return;
		}

		pesanIds.add(pesan.getId());
		msgBoard.setAttribute("pesanIds", pesanIds);

		Hbox hbox = new Hbox();

		if (pengirim != null) {
			pengirim.setMahasiswa(mahasiswa);
		} else {
			pengirim = new Tbmuser(mahasiswa);
		}

		if (mahasiswa != null) {
			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);
		} else if (pengirim.getDosen() != null) {
			CommonMedia.tampilkanGambarKecil(pengirim.getDosen()).setParent(hbox);
		} else if (pengirim.getPegawai() != null) {
			CommonMedia.tampilkanGambarKecil(pengirim.getPegawai()).setParent(hbox);
		} else if (pengirim.getBiodataCalonMahasiswa() != null) {
			CommonMedia.tampilkanGambarKecil(pengirim.getBiodataCalonMahasiswa()).setParent(hbox);
		} else {
			CommonMedia.tampilkanGambarKecil(pengirim).setParent(hbox);
		}

		Vbox vb = new Vbox();
		vb.setParent(hbox);
		vb.setStyle("padding-top:10px;padding-left:10px;width:100%;");
		Label contentLbl = new Label(pesan.getIsi());
		contentLbl.setStyle("font-weight:bold;");
		vb.appendChild(contentLbl);
		Hbox hb = new Hbox();
		hb.setParent(vb);
		Div div = new Div();
		div.setParent(hb);
		div.setStyle("width:100%;text-align:right;");

		Long ts = waktu.getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");
		String time = (mahasiswa != null ? (mahasiswa.getNama() + " (Mahasiswa)") : pengirim) + " @ "
				+ sdf.format(ts).toString();
		Label sendertimeLbl = new Label(time);
		div.appendChild(sendertimeLbl);

		msgBoard.appendChild(hbox);
		Clients.scrollIntoView(hbox);
	}

}
