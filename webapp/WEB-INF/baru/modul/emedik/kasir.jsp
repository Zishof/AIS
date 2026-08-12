<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<div class="mb-3 d-flex justify-content-between"><div><h4>Kasir eMedik</h4><p class="text-muted">Pembayaran layanan dan tindakan medis.</p></div><a class="btn btn-warning align-self-start" href="<%=request.getContextPath()%>/baru?p=emedik&amp;s=help&amp;menu=emedik_kasir"><i class="fas fa-question-circle"></i> Bantuan</a></div>
<jsp:include page="/WEB-INF/baru/modul/pagesmastersirspembayaranzul/index.jsp" />
