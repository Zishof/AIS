package ais.action.master.jurnal.test;

import ais.action.master.jurnal.JurnalGalleyViewerService;

public final class JurnalGalleyViewerSelfTest {
    public static void main(String[]args){String dirty="<article><h1>Aman</h1><script>alert(1)</script><img src='https://tracker/x'><a href='javascript:alert(2)'>x</a><a href='https://example.test/a'>ok</a></article>";String safe=JurnalGalleyViewerService.sanitizeHtml(dirty);check(safe.indexOf("<h1>Aman</h1>")>=0,"heading hilang");check(safe.indexOf("script")<0&&safe.indexOf("img")<0&&safe.indexOf("javascript")<0,"active content lolos");check(safe.indexOf("noopener noreferrer")>=0,"rel aman hilang");String jats="<article><front><article-meta><title-group><article-title>Judul &amp; Aman</article-title></title-group><abstract><p>Ringkas</p></abstract></article-meta></front><body><sec><title>Metode</title><p>Isi &lt;uji&gt;</p></sec></body></article>";String rendered=JurnalGalleyViewerService.jatsToAccessibleHtml(jats);check(rendered.indexOf("<h1>Judul &amp; Aman</h1>")>=0&&rendered.indexOf("<h2>Metode</h2>")>=0&&rendered.indexOf("&lt;uji&gt;")>=0,"render JATS salah");boolean xxe=false;try{JurnalGalleyViewerService.jatsToAccessibleHtml("<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><article>&e;</article>");}catch(IllegalArgumentException expected){xxe=true;}check(xxe,"DTD/entity tidak ditolak");System.out.println("JurnalGalleyViewerSelfTest OK html-allowlist no-active-content JATS-accessible XXE-denied");}
    private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
}
