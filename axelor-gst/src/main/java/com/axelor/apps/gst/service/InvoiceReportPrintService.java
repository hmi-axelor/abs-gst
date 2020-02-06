package com.axelor.apps.gst.service;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.apps.account.service.invoice.print.InvoicePrintServiceImpl;
import com.axelor.apps.base.service.app.AppService;
import com.axelor.apps.gst.report.IReport;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;

public class InvoiceReportPrintService extends InvoicePrintServiceImpl {

  @Inject AppService appService;

  @Override
  public ReportSettings prepareReportSettings(Invoice invoice, Integer reportType, String format)
      throws AxelorException {
    if (appService.isApp("gst")) {
      if (invoice.getPrintingSettings() == null) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_MISSING_FIELD,
            String.format(
                I18n.get(IExceptionMessage.INVOICE_MISSING_PRINTING_SETTINGS),
                invoice.getInvoiceId()),
            invoice);
      }
      String locale = ReportSettings.getPrintingLocale(invoice.getPartner());

      String title = I18n.get("Invoice");
      if (invoice.getInvoiceId() != null) {
        title += " " + invoice.getInvoiceId();
      }

      ReportSettings reportSetting =
          ReportFactory.createReport(IReport.INVOICE_GST, title + " - ${date}");

      return reportSetting
          .addParam("InvoiceId", invoice.getId())
          .addParam("Locale", locale)
          .addParam("ReportType", reportType == null ? 0 : reportType)
          .addFormat(format);
    } else return super.prepareReportSettings(invoice, reportType, format);
  }
}
