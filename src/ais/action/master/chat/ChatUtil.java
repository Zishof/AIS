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

public class ChatUtil {

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
