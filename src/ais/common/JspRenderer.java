package ais.common;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Renderer ZK untuk jsp. Kelas ini menerjemahkan satu objek domain menjadi komponen/baris
 * antarmuka tanpa mengambil alih aturan bisnis milik action atau service pemanggil.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code renderJsp}(). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 */
public class JspRenderer {

    /**
     * Inner class untuk menangkap output respon
     */
    private static class CharResponseWrapper extends HttpServletResponseWrapper {
        private final CharArrayWriter charWriter = new CharArrayWriter();
        private final PrintWriter writer = new PrintWriter(charWriter);

        public CharResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public PrintWriter getWriter() {
            return writer;
        }

        public String toString() {
            return charWriter.toString();
        }
    }

    /**
     * Method untuk merender JSP menjadi String
     */
    public static String renderJsp(HttpServletRequest request, HttpServletResponse response, String jspPath) 
            throws ServletException, IOException {
        
        // 1. Buat wrapper respon palsu
        CharResponseWrapper responseWrapper = new CharResponseWrapper(response);
        
        // 2. Dapatkan dispatcher untuk file JSP target
        RequestDispatcher dispatcher = request.getRequestDispatcher(jspPath);
        
        // 3. Lakukan include (output akan masuk ke wrapper, bukan ke browser)
        dispatcher.include(request, responseWrapper);
        
        // 4. Kembalikan hasil sebagai string
        return responseWrapper.toString();
    }
}