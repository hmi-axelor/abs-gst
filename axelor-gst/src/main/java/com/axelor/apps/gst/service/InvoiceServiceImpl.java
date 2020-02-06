package com.axelor.apps.gst.service;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.InvoiceLineTax;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.config.AccountConfigService;
import com.axelor.apps.account.service.invoice.factory.CancelFactory;
import com.axelor.apps.account.service.invoice.factory.ValidateFactory;
import com.axelor.apps.account.service.invoice.factory.VentilateFactory;
import com.axelor.apps.account.service.invoice.generator.tax.TaxInvoiceLine;
import com.axelor.apps.base.db.Address;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.service.PartnerService;
import com.axelor.apps.base.service.alarm.AlarmEngineService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.base.service.app.AppService;
import com.axelor.apps.businessproject.service.InvoiceServiceProjectImpl;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class InvoiceServiceImpl extends InvoiceServiceProjectImpl implements InvoiceService {

  @Inject
  public InvoiceServiceImpl(
      ValidateFactory validateFactory,
      VentilateFactory ventilateFactory,
      CancelFactory cancelFactory,
      AlarmEngineService<Invoice> alarmEngineService,
      InvoiceRepository invoiceRepo,
      AppAccountService appAccountService,
      PartnerService partnerService,
      com.axelor.apps.account.service.invoice.InvoiceLineService invoiceLineService,
      AccountConfigService accountConfigService) {
    super(
        validateFactory,
        ventilateFactory,
        cancelFactory,
        alarmEngineService,
        invoiceRepo,
        appAccountService,
        partnerService,
        invoiceLineService,
        accountConfigService);
  }

  @Inject private InvoiceLineService invoiceLineService;
  @Inject AppService appService;

  @Override
  public Invoice compute(Invoice invoice) throws AxelorException {
    if (appService.isApp("gst")) {
      invoice = calculateInvoiceGst(invoice);
      if (invoice.getPartner() != null && invoice.getInvoiceLineList() != null) {
        if (invoice.getInvoiceLineTaxList() == null)
          invoice.setInvoiceLineTaxList(new ArrayList<InvoiceLineTax>());
        else invoice.getInvoiceLineTaxList().clear();
        List<InvoiceLineTax> invoiceTaxLines =
            (new TaxInvoiceLine(invoice, invoice.getInvoiceLineList())).creates();
        invoice.getInvoiceLineTaxList().addAll(invoiceTaxLines);
        for (InvoiceLineTax inv : invoice.getInvoiceLineTaxList()) {
          inv.setExTaxBase(
              inv.getExTaxBase()
                  .setScale(AppBaseService.DEFAULT_NB_DECIMAL_DIGITS, RoundingMode.HALF_UP));
          inv.setInTaxTotal(
              inv.getInTaxTotal()
                  .setScale(AppBaseService.DEFAULT_NB_DECIMAL_DIGITS, RoundingMode.HALF_UP));
        }
      }
//      invoice.setExTaxTotal(
//          invoice
//              .getExTaxTotal()
//              .setScale(AppBaseService.DEFAULT_NB_DECIMAL_DIGITS, RoundingMode.HALF_UP));
//      invoice.setInTaxTotal(
//          invoice
//              .getInTaxTotal()
//              .setScale(AppBaseService.DEFAULT_NB_DECIMAL_DIGITS, RoundingMode.HALF_UP));
//      invoice.setTaxTotal(
//          invoice
//              .getTaxTotal()
//              .setScale(AppBaseService.DEFAULT_NB_DECIMAL_DIGITS, RoundingMode.HALF_UP));
//      invoice.setCompanyTaxTotal(
//          invoice
//              .getCompanyTaxTotal()
//              .setScale(AppBaseService.DEFAULT_NB_DECIMAL_DIGITS, RoundingMode.HALF_UP));
      invoice.setAdvancePaymentInvoiceSet(this.getDefaultAdvancePaymentInvoice(invoice));
      invoice.setAmountRemaining(invoice.getInTaxTotal());
      invoice.setHasPendingPayments(false);
      return invoice;
    } else return super.compute(invoice);
  }

  @Override
  public Invoice calculateInvoiceGst(Invoice invoice) {
    Boolean isDiff = isStateDifferent(invoice.getCompany(), invoice.getAddress());
    if (isDiff != null && invoice.getInvoiceLineList() != null && invoice.getPartner() != null) {
      BigDecimal invoiceNetAmount = BigDecimal.ZERO;
      BigDecimal invoiceNetIgst = BigDecimal.ZERO;
      BigDecimal invoiceNetCgst = BigDecimal.ZERO;
      BigDecimal invoiceNetSgst = BigDecimal.ZERO;
      BigDecimal invoiceGrossAmount = BigDecimal.ZERO;
      BigDecimal companyExTaxTotal = BigDecimal.ZERO;
      BigDecimal companyInTaxTotal = BigDecimal.ZERO;
      List<InvoiceLine> invoiceItemList = invoice.getInvoiceLineList();
      for (InvoiceLine item : invoiceItemList) {
        item = invoiceLineService.calculateInvoiceLineGst(item, isDiff);
        invoiceNetAmount = invoiceNetAmount.add(item.getExTaxTotal());
        invoiceNetIgst = invoiceNetIgst.add(item.getIgst());
        invoiceNetCgst = invoiceNetCgst.add(item.getCgst());
        invoiceNetSgst = invoiceNetSgst.add(item.getSgst());
        invoiceGrossAmount = invoiceGrossAmount.add(item.getInTaxTotal());
        companyExTaxTotal = companyExTaxTotal.add(item.getCompanyExTaxTotal());
        companyInTaxTotal = companyInTaxTotal.add(item.getCompanyInTaxTotal());
      }
      invoice.setCompanyExTaxTotal(companyExTaxTotal);
      invoice.setCompanyInTaxTotal(companyInTaxTotal);
      invoice.setTaxTotal(invoiceNetCgst.add(invoiceNetSgst).add(invoiceNetIgst));
      invoice.setExTaxTotal(invoiceNetAmount);
      invoice.setNetCgst(invoiceNetCgst);
      invoice.setNetIgst(invoiceNetIgst);
      invoice.setNetSgst(invoiceNetSgst);
      if (invoiceGrossAmount.compareTo(BigDecimal.ZERO) == 0)
        invoice.setInTaxTotal(invoiceNetAmount);
      else invoice.setInTaxTotal(invoiceGrossAmount);
      invoice.setCompanyTaxTotal(invoice.getInTaxTotal().subtract(invoice.getExTaxTotal()));
    } else {
      BigDecimal totalNetAmount = BigDecimal.ZERO;
      if (invoice.getInvoiceLineList() != null) {
        for (InvoiceLine item : invoice.getInvoiceLineList()) {
          item = invoiceLineService.calculateInvoiceLineGst(item, isDiff);
          totalNetAmount = totalNetAmount.add(item.getExTaxTotal());
        }
      }
      invoice.setExTaxTotal(totalNetAmount);
      invoice.setTaxTotal(BigDecimal.ZERO);
      invoice.setNetCgst(BigDecimal.ZERO);
      invoice.setNetIgst(BigDecimal.ZERO);
      invoice.setNetSgst(BigDecimal.ZERO);
      invoice.setInTaxTotal(totalNetAmount);
      invoice.setCompanyExTaxTotal(totalNetAmount);
      invoice.setCompanyInTaxTotal(totalNetAmount);
      invoice.setCompanyTaxTotal(BigDecimal.ZERO);
    }
    return invoice;
  }

  @Override
  public Boolean isStateDifferent(Company company, Address address) {
    if (company != null
        && address != null
        && company.getAddress() != null
        && company.getAddress().getState() != null
        && address.getState() != null) {
      if (company.getAddress().getState().equals(address.getState())) return false;
      else return true;
    }
    return null;
  }
}
