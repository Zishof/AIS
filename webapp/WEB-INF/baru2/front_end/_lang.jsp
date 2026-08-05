<%@ page import="ais.database.model.Tbmuser,ais.database.model.LabelBahasa,ais.database.hibernate.HibernateUtil,org.hibernate.Session,java.util.List,org.json.JSONObject" %>
<%--
  _lang.jsp — Injeksi peta terjemahan multi-bahasa ke halaman baru2.

  Cara pakai: sertakan setelah _komponen.jsp di setiap halaman:
      <%@ include file="../_lang.jsp" %>

  Lalu gunakan AIS2.L(kunci, fallback) di JS:
      document.querySelector('.judul').textContent = AIS2.L('Mahasiswa','Mahasiswa');

  Sumber data: tabel public.label_bahasa (entitas LabelBahasa).
  Kolom dipilih berdasar bahasa aktif user (Tbmuser.getBahasa()):
    Bahasa Indonesia → kolom indonesia
    English          → kolom english
    Arabic           → kolom arab     (fallback ke indonesia jika kosong)
    Mandarin         → kolom mandarin (fallback ke indonesia jika kosong)

  Output: <script>window.AIS2_LANG = { ... };</script>
  Ukuran map dibatasi max 1000 entri untuk performa.
--%>
<%
String _curBahasa = Tbmuser.INDONESIA;
javax.servlet.http.HttpSession _langSess = request.getSession(false);
if (_langSess != null) {
    /* "login" = LogLogin setelah AJAX login; Tbmuser ada di "usersTemp" */
    Object _usr = _langSess.getAttribute("login");
    Tbmuser _tbmu = null;
    if (_usr instanceof Tbmuser) {
        _tbmu = (Tbmuser) _usr;
    } else if (_langSess.getAttribute("usersTemp") instanceof Tbmuser) {
        _tbmu = (Tbmuser) _langSess.getAttribute("usersTemp");
    }
    if (_tbmu != null) {
        String _b = _tbmu.getBahasa();
        if (_b != null && !_b.isEmpty()) _curBahasa = _b;
    }
}
final String _bahasa = _curBahasa;
boolean _isEn   = Tbmuser.ENGLISH.equals(_bahasa);
boolean _isArab = "Arabic".equals(_bahasa);
boolean _isMand = "Mandarin".equals(_bahasa);

Session _db = null;
JSONObject _map = null;
try {
    _map = new JSONObject();
    _db = HibernateUtil.openSession();
    List _rows = _db.createQuery("FROM LabelBahasa ORDER BY nama").setMaxResults(2000).list();
    for (Object _obj : _rows) {
        LabelBahasa _lb = (LabelBahasa)_obj;
        if (_lb.getNama() == null) continue;
        String _val = null;
        if (_isEn)   _val = _lb.getEnglish();
        else if (_isArab) _val = _lb.getArab();
        else if (_isMand) _val = _lb.getMandarin();
        else              _val = _lb.getIndonesia();
        if (_val == null || _val.trim().isEmpty()) _val = _lb.getIndonesia();
        if (_val == null) _val = _lb.getNama();
        _map.put(_lb.getNama(), _val);
    }
} catch (Throwable _ex) {
    /* Gagal ambil label — UI tetap menggunakan fallback hardcoded */
} finally {
    if (_db != null && _db.isOpen()) try { _db.close(); } catch (Throwable _ex2) {}
}
if (_map == null) _map = new JSONObject();
%>
<script>
/* AIS2_LANG — peta terjemahan dari tabel label_bahasa, bahasa: <%=_bahasa%> */
if (typeof window.AIS2_LANG === 'undefined') {
    window.AIS2_LANG = <%=_map.toString()%>;
} else {
    /* Merge jika sudah ada (multi-include guard) */
    var _m=<%=_map.toString()%>;
    for (var _k in _m) { if (_m.hasOwnProperty(_k)) window.AIS2_LANG[_k]=_m[_k]; }
}
window.AIS2_BAHASA = '<%=_bahasa%>';
</script>
