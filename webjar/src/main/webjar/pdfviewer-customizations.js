(function() {
   window.addEventListener('load', function() {
      setExternalLinkTarget();
      setViewRights();
   }, true);

  // PDFVIEWER-2: Open the links inside the PDF documents in a new tab
  function setExternalLinkTarget() {
    PDFViewerApplication.preferences.set('externalLinkTarget', 2);
  }
  // PDFVIEWER-13: Allow author of the macro to delegate its view right on the PDF document.
  function setViewRights() {
    var viewerOnly = window.location.search.indexOf('PDFViewerService') !== -1;
    if (viewerOnly) {
      PDFViewerApplication.appConfig.toolbar.download.hidden = true;
      PDFViewerApplication.appConfig.toolbar.print.hidden = true;
      PDFViewerApplication.appConfig.secondaryToolbar.downloadButton.hidden = true;
      PDFViewerApplication.appConfig.secondaryToolbar.printButton.hidden = true;
    }
  }
})();
