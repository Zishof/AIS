package ais.action.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.file.FotoGambarItem;
import ais.database.model.library.Item;

/**
 * Servlet publik penyaji sampul buku perpustakaan lewat {@code GET
 * /LibraryItemCover?id=<id>}.
 *
 * <p><b>Endpoint bertipe kuat</b> -- berbeda dari pola lama sejenis yang menerima nama kelas
 * dan properti dari browser, di sini browser hanya mengirim {@code id} numerik; kelas entity
 * ({@link Item}, {@link FotoGambarItem}) dan nama properti Blob sudah dipatri di kode, sehingga
 * tidak ada permukaan untuk memilih kelas/refleksi sembarang dari parameter permintaan.</p>
 */
public class LibraryItemCover extends HttpServlet {
    /** ID versi serialisasi servlet ini (kontrak {@link java.io.Serializable} bawaan {@code HttpServlet}). */
    private static final long serialVersionUID = 1L;

    /**
     * Menyajikan gambar sampul item pustaka aktif, atau gambar cadangan bila item/sampul
     * tidak ditemukan.
     *
     * <p>Alur: (1) validasi {@code id} berupa bilangan bulat positif, balas 400 bila tidak;
     * (2) ambil {@link Item}, balas 404 bila tidak ada atau {@code aktif=false}; (3) ambil
     * {@link FotoGambarItem} terbaru (diurutkan {@code id} menurun) milik item tersebut; bila
     * tidak ada gambar, jatuh ke {@link #fallback}; (4) alirkan Blob gambar ke tanggapan dengan
     * tipe MIME dari kolom {@code keterangan} (divalidasi pola MIME dasar, jatuh ke
     * {@code image/jpeg} bila tidak cocok). Galat tak terduga dicatat lewat
     * {@link ais.common.ErrorAuditUtil} dan direspons dengan gambar cadangan bila tanggapan
     * belum ter-commit.</p>
     *
     * @param request permintaan HTTP; parameter {@code id} wajib berupa angka positif
     * @param response tanggapan HTTP; diisi gambar sampul, gambar cadangan, atau kode kesalahan
     * @throws ServletException bila forward ke gambar cadangan gagal
     * @throws IOException bila penulisan tanggapan gagal
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "public, max-age=3600");
        Long id = positiveLong(request.getParameter("id"));
        if (id == null) { response.sendError(HttpServletResponse.SC_BAD_REQUEST); return; }
        Session session = null; InputStream input = null;
        try {
            session = HibernateUtil.openSession();
            Item item = (Item) session.get(Item.class, id);
            if (item == null || Boolean.FALSE.equals(item.getAktif())) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
            FotoGambarItem image = (FotoGambarItem) session.createCriteria(FotoGambarItem.class)
                    .add(Restrictions.eq("item", id)).addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
            Blob blob = image == null ? null : image.getFoto();
            if (blob == null) { fallback(request, response); return; }
            String type = image.getKeterangan();
            response.setContentType(type != null && type.matches("[A-Za-z0-9.+-]+/[A-Za-z0-9.+-]+") ? type : "image/jpeg");
            long length=blob.length(); if(length>0&&length<=Integer.MAX_VALUE)response.setContentLength((int)length);
            input=blob.getBinaryStream(); OutputStream output=response.getOutputStream(); byte[]buffer=new byte[16384];int read;
            while((read=input.read(buffer))!=-1)output.write(buffer,0,read); output.flush();
        } catch(Exception e) {
            ais.common.ErrorAuditUtil.record(e,"typed library item cover");
            if(!response.isCommitted())fallback(request,response);
        } finally {
            if(input!=null)try{input.close();}catch(Exception ignored){}
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    /**
     * Menolak seluruh permintaan {@code POST}; endpoint sampul ini hanya melayani {@code GET}.
     *
     * @param request permintaan HTTP masuk (tidak dipakai selain oleh kontrak servlet)
     * @param response tanggapan HTTP; selalu diisi status 405
     * @throws IOException bila penulisan status gagal
     */
    protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException{response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);}

    /**
     * Mem-parsing {@code value} menjadi {@link Long} positif, atau {@code null} bila
     * {@code value} kosong, bukan angka, atau tidak lebih besar dari nol.
     *
     * @param value teks yang akan diparse, boleh {@code null}
     * @return ID positif hasil parse, atau {@code null} bila tidak valid
     */
    private static Long positiveLong(String value){try{long id=Long.parseLong(value==null?"":value.trim());return id>0?Long.valueOf(id):null;}catch(Exception e){return null;}}

    /**
     * Mem-forward ke gambar sampul cadangan generik ({@code /img/book.jpg}) saat item atau
     * gambarnya tidak tersedia.
     *
     * @param request permintaan HTTP yang akan diteruskan
     * @param response tanggapan HTTP yang akan diisi gambar cadangan
     * @throws ServletException bila dispatch/forward gagal
     * @throws IOException bila forward gagal menulis tanggapan
     */
    private static void fallback(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException{request.getRequestDispatcher("/img/book.jpg").forward(request,response);}
}
