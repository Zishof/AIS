package ais.common;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

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