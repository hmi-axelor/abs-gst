package com.axelor.apps.gst.module;

import com.axelor.app.AxelorModule;
import com.axelor.apps.account.service.AccountManagementServiceAccountImpl;
import com.axelor.apps.account.service.invoice.print.InvoicePrintServiceImpl;
import com.axelor.apps.account.web.InvoiceLineController;
import com.axelor.apps.businessproject.service.InvoiceServiceProjectImpl;
import com.axelor.apps.gst.service.InvoiceLineService;
import com.axelor.apps.gst.service.InvoiceLineServiceImpl;
import com.axelor.apps.gst.service.InvoiceReportPrintService;
import com.axelor.apps.gst.service.InvoiceService;
import com.axelor.apps.gst.service.InvoiceServiceImpl;
import com.axelor.apps.gst.tax.GstTaxLine;

public class GSTModule extends AxelorModule {

  @Override
  protected void configure() {
    bind(InvoiceService.class).to(InvoiceServiceImpl.class);
    bind(InvoiceLineService.class).to(InvoiceLineServiceImpl.class);
    bind(AccountManagementServiceAccountImpl.class).to(GstTaxLine.class);
    bind(InvoiceServiceProjectImpl.class).to(InvoiceServiceImpl.class);
    bind(InvoiceLineController.class).to(com.axelor.apps.gst.web.InvoiceLineController.class);
    bind(InvoicePrintServiceImpl.class).to(InvoiceReportPrintService.class);
  }
}
