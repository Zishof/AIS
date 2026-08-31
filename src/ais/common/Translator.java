package ais.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Utilitas percobaan (proof-of-concept) untuk menerjemahkan teks antar-bahasa lewat pemanggilan
 * HTTP GET ke sebuah Google Apps Script eksternal yang bertindak sebagai proxy Google Translate.
 * Kelas ini BUKAN bagian dari alur aplikasi AIS yang berjalan — tidak ada kode lain di codebase
 * yang memanggil {@link #translate(String, String, String)}; satu-satunya pemicunya adalah
 * method {@link #main(String[])} miliknya sendiri, sehingga kelas ini murni contoh/latihan yang
 * ditinggalkan di source tree.
 *
 * <p>
 * Endpoint yang dipanggil ({@code urlStr} pada {@link #translate}) masih berupa nilai placeholder
 * ({@code "https://your.google.script.url"}) yang belum diisi dengan URL Apps Script sungguhan —
 * bukan kredensial nyata yang tertanam, melainkan penanda "isi URL Anda di sini" yang belum
 * dilengkapi. Tidak ada API key atau kredensial rahasia lain pada kelas ini.
 * </p>
 */
public class Translator {

    /**
     * Titik masuk demo: menerjemahkan teks contoh {@code "Hello world!"} dari bahasa Inggris
     * ({@code "en"}) ke bahasa Indonesia ({@code "id"}) lewat {@link #translate(String, String,
     * String)}, lalu mencetak hasilnya ke konsol. Murni untuk pengujian manual kelas ini.
     *
     * @param args argumen baris perintah, tidak dipakai
     * @throws IOException diteruskan dari kegagalan koneksi HTTP di {@link #translate(String,
     *                      String, String)}
     */
    public static void main(String[] args) throws IOException {
        String text = "Hello world!";
        //Translated text: Hallo Welt!
        System.out.println("Translated text: " + translate("en", "id", text));
    }

    /**
     * Menerjemahkan {@code text} dari bahasa {@code langFrom} ke bahasa {@code langTo} dengan
     * memanggil endpoint Google Apps Script eksternal via HTTP GET (parameter {@code q}/{@code
     * target}/{@code source}, teks di-encode dengan {@link URLEncoder} sebagai UTF-8) dan
     * mengembalikan seluruh isi respons sebagai satu string.
     *
     * <p>
     * <b>Catatan:</b> URL endpoint pada implementasi saat ini masih berupa placeholder yang belum
     * diisi ({@code "https://your.google.script.url"}) — method ini tidak akan berfungsi sampai
     * nilai tersebut diganti dengan URL Apps Script yang valid.
     * </p>
     *
     * @param langFrom kode bahasa asal (mis. {@code "en"})
     * @param langTo   kode bahasa tujuan (mis. {@code "id"})
     * @param text     teks yang akan diterjemahkan
     * @return teks hasil terjemahan sebagaimana dikembalikan mentah-mentah oleh endpoint
     * @throws IOException bila koneksi HTTP gagal dibuka atau gagal dibaca
     */
    private static String translate(String langFrom, String langTo, String text) throws IOException {
        // INSERT YOU URL HERE
        String urlStr = "https://your.google.script.url" +
                "?q=" + URLEncoder.encode(text, "UTF-8") +
                "&target=" + langTo +
                "&source=" + langFrom;
        URL url = new URL(urlStr);
        StringBuilder response = new StringBuilder();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

}