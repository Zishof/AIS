<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<section class="nui-page" aria-labelledby="nuiMahasiswaHelpTitle">
  <div class="nui-breadcrumb"><a href="<%=request.getContextPath()%>/new">Beranda</a> / Mahasiswa / Bantuan</div>
  <div class="nui-page-head">
    <div><h1 id="nuiMahasiswaHelpTitle">Bantuan Mahasiswa</h1><p class="nui-page-desc">Panduan resmi yang sama dengan sumber bantuan pada modul Mahasiswa existing.</p></div>
    <a class="nui-btn" href="javascript:history.back()"><i class="fa-solid fa-arrow-left" aria-hidden="true"></i> Kembali</a>
  </div>
  <article class="nui-card nui-card-pad nui-native-help">
    <% pageContext.include("/WEB-INF/bantuan/mahasiswa.html", true); %>
  </article>
</section>
