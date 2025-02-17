/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.businessproject.web;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.PaymentCondition;
import com.axelor.apps.account.db.PaymentMode;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Currency;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.PriceList;
import com.axelor.apps.base.db.Wizard;
import com.axelor.apps.businessproject.service.InvoiceServiceProjectImpl;
import com.axelor.apps.businessproject.service.SaleOrderInvoiceProjectServiceImpl;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.db.JPA;
import com.axelor.exception.AxelorException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionView.ActionViewBuilder;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.base.Joiner;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class InvoiceController {

  // Generate single invoice from several
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void mergeInvoice(ActionRequest request, ActionResponse response) {
    List<Invoice> invoiceList = new ArrayList<Invoice>();
    List<Long> invoiceIdList = new ArrayList<Long>();
    boolean fromPopup = false;

    if (request.getContext().get("invoiceToMerge") != null) {

      if (request.getContext().get("invoiceToMerge") instanceof List) {
        // No confirmation popup, invoices are content in a parameter list
        List<Map> invoiceMap = (List<Map>) request.getContext().get("invoiceToMerge");
        for (Map map : invoiceMap) {
          invoiceIdList.add(new Long((Integer) map.get("id")));
        }
      } else {
        // After confirmation popup, invoice's id are in a string separated by ","
        String invoiceIdListStr = (String) request.getContext().get("invoiceToMerge");
        for (String invoiceId : invoiceIdListStr.split(",")) {
          invoiceIdList.add(new Long(invoiceId));
        }
        fromPopup = true;
      }
    }

    // Check if company, currency and partner are the same for all selected invoices
    Company commonCompany = null;
    Currency commonCurrency = null;
    Partner commonPartner = null;
    PaymentCondition commonPaymentCondition = null;
    // Useful to determine if a difference exists between payment conditions of all invoices
    boolean existPaymentConditionDiff = false;
    Partner commonContactPartner = null;
    // Useful to determine if a difference exists between contact partners of all purchase orders
    boolean existContactPartnerDiff = false;
    PriceList commonPriceList = null;
    // Useful to determine if a difference exists between price lists of all purchase orders
    boolean existPriceListDiff = false;
    PaymentMode commonPaymentMode = null;
    // Useful to determine if a difference exists between locations of all purchase orders
    boolean existPaymentModeDiff = false;
    SaleOrder commonSaleOrder = null;
    // Useful to check if all sale orders are null (since this field is not required)
    boolean saleOrderIsNull = false;
    Project commonProject = null;
    // Useful to check if all projects are null (since this field is not required)
    boolean projectIsNull = false;

    Invoice invoiceTemp;
    int count = 1;
    for (Long invoiceId : invoiceIdList) {
      invoiceTemp = JPA.em().find(Invoice.class, invoiceId);
      invoiceList.add(invoiceTemp);
      if (count == 1) {
        commonCompany = invoiceTemp.getCompany();
        commonCurrency = invoiceTemp.getCurrency();
        commonPartner = invoiceTemp.getPartner();
        commonPaymentCondition = invoiceTemp.getPaymentCondition();
        commonContactPartner = invoiceTemp.getContactPartner();
        commonPriceList = invoiceTemp.getPriceList();
        commonPaymentMode = invoiceTemp.getPaymentMode();
        commonSaleOrder = invoiceTemp.getSaleOrder();
        commonProject = invoiceTemp.getProject();
        if (commonSaleOrder == null) {
          saleOrderIsNull = true;
        }
        if (commonProject == null) {
          projectIsNull = true;
        }
      } else {
        if (commonCompany != null && !commonCompany.equals(invoiceTemp.getCompany())) {
          commonCompany = null;
        }
        if (commonCurrency != null && !commonCurrency.equals(invoiceTemp.getCurrency())) {
          commonCurrency = null;
        }
        if (commonPartner != null && !commonPartner.equals(invoiceTemp.getPartner())) {
          commonPartner = null;
        }
        if (commonPaymentCondition != null
            && !commonPaymentCondition.equals(invoiceTemp.getPaymentCondition())) {
          commonPaymentCondition = null;
          existPaymentConditionDiff = true;
        }
        if (commonContactPartner != null
            && !commonContactPartner.equals(invoiceTemp.getContactPartner())) {
          commonContactPartner = null;
          existContactPartnerDiff = true;
        }
        if (commonPriceList != null && !commonPriceList.equals(invoiceTemp.getPriceList())) {
          commonPriceList = null;
          existPriceListDiff = true;
        }
        if (commonPaymentMode != null && !commonPaymentMode.equals(invoiceTemp.getPaymentMode())) {
          commonPaymentMode = null;
          existPaymentModeDiff = true;
        }
        if (commonSaleOrder != null && !commonSaleOrder.equals(invoiceTemp.getSaleOrder())) {
          commonSaleOrder = null;
        }
        if (commonProject != null && !commonProject.equals(invoiceTemp.getProject())) {
          commonProject = null;
        }
        if (invoiceTemp.getSaleOrder() != null) {
          saleOrderIsNull = false;
        }
        if (invoiceTemp.getProject() != null) {
          projectIsNull = false;
        }
      }
      count++;
    }

    StringBuilder fieldErrors = new StringBuilder();
    if (commonCurrency == null) {
      fieldErrors.append(I18n.get(IExceptionMessage.INVOICE_MERGE_ERROR_CURRENCY));
    }
    if (commonCompany == null) {
      if (fieldErrors.length() > 0) {
        fieldErrors.append("<br/>");
      }
      fieldErrors.append(I18n.get(IExceptionMessage.INVOICE_MERGE_ERROR_COMPANY));
    }
    if (commonPartner == null) {
      if (fieldErrors.length() > 0) {
        fieldErrors.append("<br/>");
      }
      fieldErrors.append(I18n.get(IExceptionMessage.INVOICE_MERGE_ERROR_PARTNER));
    }

    if (commonSaleOrder == null && saleOrderIsNull == false) {
      if (fieldErrors.length() > 0) {
        fieldErrors.append("<br/>");
      }
      fieldErrors.append(I18n.get(IExceptionMessage.INVOICE_MERGE_ERROR_SALEORDER));
    }

    if (commonProject == null && projectIsNull == false) {
      if (fieldErrors.length() > 0) {
        fieldErrors.append("<br/>");
      }
      fieldErrors.append(I18n.get(IExceptionMessage.INVOICE_MERGE_ERROR_PROJECT));
    }

    if (fieldErrors.length() > 0) {
      response.setFlash(fieldErrors.toString());
      return;
    }

    // Check if contactPartner or priceList or paymentMode or paymentCondition  or saleOrder are
    // content in parameters
    if (request.getContext().get("contactPartner") != null) {
      commonContactPartner =
          JPA.em()
              .find(
                  Partner.class,
                  new Long((Integer) ((Map) request.getContext().get("contactPartner")).get("id")));
    }
    if (request.getContext().get("priceList") != null) {
      commonPriceList =
          JPA.em()
              .find(
                  PriceList.class,
                  new Long((Integer) ((Map) request.getContext().get("priceList")).get("id")));
    }
    if (request.getContext().get("paymentMode") != null) {
      commonPaymentMode =
          JPA.em()
              .find(
                  PaymentMode.class,
                  new Long((Integer) ((Map) request.getContext().get("paymentMode")).get("id")));
    }
    if (request.getContext().get("paymentCondition") != null) {
      commonPaymentCondition =
          JPA.em()
              .find(
                  PaymentCondition.class,
                  new Long(
                      (Integer) ((Map) request.getContext().get("paymentCondition")).get("id")));
    }

    if (!fromPopup
        && (existPaymentConditionDiff
            || existContactPartnerDiff
            || existPriceListDiff
            || existPaymentModeDiff)) {
      // Need to display intermediate screen to select some values
      ActionViewBuilder confirmView =
          ActionView.define("Confirm merge invoice")
              .model(Wizard.class.getName())
              .add("form", "customer-invoices-merge-confirm-form")
              .param("popup", "true")
              .param("show-toolbar", "false")
              .param("show-confirm", "false")
              .param("popup-save", "false")
              .param("forceEdit", "true");

      if (existContactPartnerDiff) {
        confirmView.context("contextContactPartnerToCheck", "true");
        confirmView.context("contextPartnerId", commonPartner.getId().toString());
      }
      if (existPriceListDiff) {
        confirmView.context("contextPriceListToCheck", "true");
      }
      if (existPaymentModeDiff) {
        confirmView.context("contextPaymentModeToCheck", "true");
      }
      if (existPaymentConditionDiff) {
        confirmView.context("contextPaymentConditionToCheck", "true");
      }
      confirmView.context("invoiceToMerge", Joiner.on(",").join(invoiceIdList));

      response.setView(confirmView.map());

      return;
    }

    try {
      Invoice invoice =
          Beans.get(SaleOrderInvoiceProjectServiceImpl.class)
              .mergeInvoice(
                  invoiceList,
                  commonCompany,
                  commonCurrency,
                  commonPartner,
                  commonContactPartner,
                  commonPriceList,
                  commonPaymentMode,
                  commonPaymentCondition,
                  commonSaleOrder,
                  commonProject);
      if (invoice != null) {
        // Open the generated invoice in a new tab
        response.setView(
            ActionView.define("Invoice")
                .model(Invoice.class.getName())
                .add("grid", "invoice-grid")
                .add("form", "invoice-form")
                .param("forceEdit", "true")
                .context("_showRecord", String.valueOf(invoice.getId()))
                .map());
        response.setCanClose(true);
      }
    } catch (Exception e) {
      response.setFlash(e.getLocalizedMessage());
    }
  }

  public void exportAnnex(ActionRequest request, ActionResponse response) throws AxelorException {

    Invoice invoice =
        Beans.get(InvoiceRepository.class).find(request.getContext().asType(Invoice.class).getId());

    List<String> reportInfo =
        Beans.get(InvoiceServiceProjectImpl.class)
            .editInvoiceAnnex(invoice, invoice.getId().toString(), false);

    if (reportInfo == null || reportInfo.isEmpty()) {
      return;
    }

    response.setView(ActionView.define(reportInfo.get(0)).add("html", reportInfo.get(1)).map());
  }

  public void updateLines(ActionRequest request, ActionResponse response) throws AxelorException {
    Invoice invoice = request.getContext().asType(Invoice.class);
    invoice = Beans.get(InvoiceRepository.class).find(invoice.getId());

    for (InvoiceLine invoiceLine : invoice.getInvoiceLineList()) {
      invoiceLine.setProject(invoice.getProject());
    }
    response.setValue("invoiceLineList", invoice.getInvoiceLineList());
  }
}
