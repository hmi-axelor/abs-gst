package com.axelor.apps.gst.web;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.base.service.app.AppService;
import com.axelor.apps.gst.service.InvoiceLineService;
import com.axelor.apps.gst.service.InvoiceService;
import com.axelor.exception.AxelorException;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.List;

public class InvoiceLineController extends com.axelor.apps.account.web.InvoiceLineController {

  @Inject AppService appService;
  @Inject InvoiceLineService invoiceLineService;
  @Inject InvoiceService invoiceService;
  @Inject com.axelor.apps.account.service.invoice.InvoiceLineService invoiceLineService2;

  @Override
  public void compute(ActionRequest request, ActionResponse response) throws AxelorException {
    if (appService.isApp("gst")) {
      Context context = request.getContext();
      InvoiceLine invoiceLine = context.asType(InvoiceLine.class);
      if (context.getParent().getContextClass() == InvoiceLine.class) {
        context = request.getContext().getParent();
      }
      Invoice invoice = this.getInvoice(context);
      if (invoiceLine.getPrice() == null
          || invoiceLine.getInTaxPrice() == null
          || invoiceLine.getQty() == null) {
        return;
      }
      BigDecimal exTaxTotal;
      BigDecimal companyExTaxTotal;
      BigDecimal inTaxTotal;
      BigDecimal companyInTaxTotal;
      BigDecimal priceDiscounted =
          invoiceLineService2.computeDiscount(invoiceLine, invoice.getInAti());
      response.setValue("priceDiscounted", priceDiscounted);
      response.setAttr(
          "priceDiscounted",
          "hidden",
          priceDiscounted.compareTo(
                  invoice.getInAti() ? invoiceLine.getInTaxPrice() : invoiceLine.getPrice())
              == 0);
      BigDecimal taxRate = BigDecimal.ZERO;
      if (invoiceLine.getTaxLine() != null) {
        taxRate = invoiceLine.getTaxLine().getValue();
        response.setValue("taxRate", taxRate);
        response.setValue("taxCode", invoiceLine.getTaxLine().getTax().getCode());
      }
      invoiceLine =
          invoiceLineService.calculateInvoiceLineGst(
              invoiceLine,
              invoiceService.isStateDifferent(invoice.getCompany(), invoice.getAddress()));

      exTaxTotal = invoiceLine.getExTaxTotal();
      inTaxTotal = invoiceLine.getInTaxTotal();

      companyExTaxTotal = invoiceLineService2.getCompanyExTaxTotal(exTaxTotal, invoice);
      companyInTaxTotal = invoiceLineService2.getCompanyExTaxTotal(inTaxTotal, invoice);

      response.setValue("exTaxTotal", exTaxTotal);
      response.setValue("inTaxTotal", inTaxTotal);
      response.setValue("companyInTaxTotal", companyInTaxTotal);
      response.setValue("companyExTaxTotal", companyExTaxTotal);
      response.setValue("igst", invoiceLine.getIgst());
      response.setValue("cgst", invoiceLine.getCgst());
      response.setValue("sgst", invoiceLine.getSgst());
    } else super.compute(request, response);
  }

  public void getProductsByIds(ActionRequest request, ActionResponse response) {
    @SuppressWarnings("unchecked")
    List<Integer> ids = (List<Integer>) request.getContext().get("products");
    List<InvoiceLine> items = invoiceLineService.getInvoiceItemsById(ids);
    response.setValue("invoiceLineList", items);
  }
}
