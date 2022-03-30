(function() {
   window.addEventListener('load', function() {
      setExternalLinkTarget();
      setViewRights();
      addPermalinkButton();
   }, true);
  // #40: Add a copyable link of the PDF file location in the macro toolbar
  function addPermalinkButton() {
	  var toolbar = document.getElementById('toolbarViewerRight');
    var secondaryToolbar = document.getElementById('secondaryToolbar');

    var linkButton = document.getElementById('print').cloneNode(true);
    var secondaryLinkButton = document.getElementById('secondaryPrint').cloneNode(true);

    linkButton.setAttribute('id', 'permalink');
    linkButton.classList.add('permalink');
    linkButton.classList.remove('print');
    linkButton.removeAttribute('data-l10n-id');

    secondaryLinkButton.setAttribute('id', 'secondaryPermalink');
 	  secondaryLinkButton.classList.add('permalink');
    secondaryLinkButton.classList.remove('print');
    secondaryLinkButton.removeAttribute('data-l10n-id');

    toolbar.prepend(linkButton);
    secondaryToolbar.prepend(secondaryLinkButton);
  }
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
