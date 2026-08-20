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

/** Typed public cover endpoint; browser never supplies a Java class or property name. */
public class LibraryItemCover extends HttpServlet {
    private static final long serialVersionUID = 1L;

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

    protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException{response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);}
    private static Long positiveLong(String value){try{long id=Long.parseLong(value==null?"":value.trim());return id>0?Long.valueOf(id):null;}catch(Exception e){return null;}}
    private static void fallback(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException{request.getRequestDispatcher("/img/book.jpg").forward(request,response);}
}
