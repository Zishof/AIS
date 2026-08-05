<%@page import="java.util.Base64"%>
<%@page import="java.io.File"%>
<%@page import="java.io.FileOutputStream"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.StreamingHibernateUtil"%>
<%@page import="ais.database.model.file.FileFotoLain"%>
<%@page import="ais.common.Common"%>
<%
    // Set response agar berformat JSON
    response.setContentType("application/json");
    out.clearBuffer(); 
    
    try {
        // 1. Baca payload JSON Base64 dari JavaScript
        StringBuilder sb = new StringBuilder();
        BufferedReader br = request.getReader();
        String line;
        while ((line = br.readLine()) != null) { sb.append(line); }
        
        JSONObject reqJson = new JSONObject(sb.toString());
        String base64Str = reqJson.getString("base64_data");
        String refStr = reqJson.getString("ref");
        String jenis = reqJson.getString("jenis");
        String nama = reqJson.getString("nama");
        String clazzName = reqJson.getString("clazz");

        // 2. Bersihkan header Base64 bawaan browser jika ada
        if (base64Str.contains(",")) {
            base64Str = base64Str.split(",")[1];
        }

        // 3. Konversi Base64 menjadi file fisik sementara (Temp File)
        byte[] decodedBytes = Base64.getDecoder().decode(base64Str.trim());
        File tempFile = File.createTempFile("upload_lampiran_", ".tmp");
        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.write(decodedBytes);
        fos.close();

        // 4. Eksekusi fungsi Java bawaan eCampus!
        Session hibSession = StreamingHibernateUtil.getInstance().currentSession();
        Class clazz = Class.forName(clazzName);
        
        FileFotoLain.createFileFotoLain(
            Common.getCurrentUser(request), 
            hibSession, 
            clazz, 
            false, 
            Long.parseLong(refStr), 
            jenis, 
            null, 
            tempFile, 
            nama
        );
        
        // 5. Bersihkan Session dan File Sementara
        
        tempFile.delete(); 
        
        out.print("{\"status\":\"00\", \"description\":\"Berhasil\"}");
    } catch (Exception e) {
        out.print("{\"status\":\"99\", \"description\":\"" + e.getMessage() + "\"}");
    }
%>