package ais.common;

import java.net.*;
import java.io.*;

/**
 * Kelas <b>uji coba/scratch manual</b> yang mendemonstrasikan pemanggilan SOAP (protokol
 * {@code GetToken} pada layanan web {@code WSPDDIKTI} — merujuk pada PDDIKTI, Pangkalan Data
 * Pendidikan Tinggi, layanan data resmi Kemdikbudristek yang lazim diintegrasikan modul akademik
 * perguruan tinggi) lewat <b>socket TCP mentah</b> berisi request HTTP/SOAP yang disusun manual
 * sebagai string, alih-alih memakai pustaka klien SOAP/HTTP standar. Tidak ada indikasi kelas ini
 * dipanggil dari bagian lain aplikasi — satu-satunya titik masuk adalah {@link #main(String[])}
 * yang dijalankan langsung oleh pengembang untuk menjajal endpoint {@code GetToken} secara manual.
 *
 * <p>
 * Tujuan {@code GetToken} pada layanan PDDIKTI umumnya adalah memperoleh token otentikasi yang
 * kemudian dipakai untuk memanggil layanan data akademik lain (mis. sinkronisasi data mahasiswa/
 * dosen ke pangkalan data nasional) — namun kelas ini hanya menjajal langkah permintaan token
 * itu sendiri dan mencetak balasan mentah HTTP ke konsol, tanpa memproses tokennya lebih lanjut.
 * </p>
 *
 * <h2>Peringatan keamanan — kredensial tertanam langsung di kode sumber</h2>
 * <p>
 * Method {@link #main(String[])} menanam kredensial otentikasi SOAP langsung sebagai teks polos
 * di dalam string {@code xmldata}: elemen {@code <username>fauzi</username>} dan
 * {@code <password>fauzi</password>} (nilai username dan password identik: {@code "fauzi"}).
 * Kredensial ini TIDAK dibaca dari konfigurasi runtime; nilainya tertanam permanen di riwayat
 * kode sumber. Target koneksi juga ditanam langsung ({@code hostname="localhost"},
 * {@code port=8082}, {@code path="/ws/live.php"}), sehingga kode ini pada dasarnya hanya berfungsi
 * bila dijalankan di lingkungan lokal pengembang tempat layanan {@code WSPDDIKTI} tersebut
 * disimulasikan/diproksi di {@code localhost:8082}.
 * </p>
 * <p>
 * Sesuai cakupan pekerjaan dokumentasi ini, kredensial tersebut TIDAK diubah atau dihapus dari
 * kode — lihat ringkasan hasil dokumentasi untuk detail lokasi baris lengkap. Siapa pun yang
 * menyalin pola dari kelas ini untuk kebutuhan produksi WAJIB memindahkan kredensial ke konfigurasi
 * runtime yang aman dan meninjau apakah kredensial {@code "fauzi"/"fauzi"} yang sudah terlanjur
 * ter-commit ini perlu dirotasi di sisi penyedia layanan PDDIKTI.
 * </p>
 */
public class PostXml {

	/**
	 * Titik masuk uji coba manual: menyusun amplop SOAP {@code GetToken} (dengan kredensial
	 * tertanam {@code username="fauzi"}/{@code password="fauzi"} — lihat peringatan keamanan pada
	 * javadoc kelas), membuka socket TCP mentah ke {@code localhost:8082}, menulis header dan body
	 * permintaan HTTP POST secara manual (bukan lewat pustaka HTTP), mengirim envelope SOAP sebagai
	 * body, lalu mencetak seluruh baris balasan mentah dari server ke konsol. Seluruh kegagalan
	 * (koneksi, I/O, dsb.) ditangkap generik dan hanya dilaporkan lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}, tidak dilempar ke pemanggil.
	 *
	 * @param args argumen baris perintah; tidak dipakai sama sekali oleh method ini
	 */
	public static void main(String[] args) {

		try {
			String xmldata = "<s11:Envelope xmlns:s11='http://schemas.xmlsoap.org/soap/envelope/'>"
					+ "<s11:Body>"
					+ "  <ns1:GetToken xmlns:ns1='http://localhost/soap/WSPDDIKTI'>"
					+ "    <username>fauzi</username>"
					+ "    <password>fauzi</password>"
					+ "  </ns1:GetToken>"
					+ "</s11:Body>" + "</s11:Envelope>";

			// Create socket
			String hostname = "localhost";
			int port = 8082;
			InetAddress addr = InetAddress.getByName(hostname);
			@SuppressWarnings("resource")
			Socket sock = new Socket(addr, port);

			// Send header
			String path = "/ws/live.php";
			BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(
					sock.getOutputStream(), "UTF-8"));
			// You can use "UTF8" for compatibility with the Microsoft virtual
			// machine.
			wr.write("POST " + path + " HTTP/1.1\r\n");
			wr.write("Host: " + hostname + ":" + port + "\r\n");
			wr.write("SOAPAction: http://" + hostname + path + "/GetToken\r\n");
			wr.write("Content-Length: " + xmldata.length() + "\r\n");
			wr.write("Content-Type: text/xml; charset=\"utf-8\"\r\n");
			wr.write("\r\n");

			// Send data
			wr.write(xmldata);
			wr.flush();

			// Response
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					sock.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null)
				System.out.println(line);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}
}
