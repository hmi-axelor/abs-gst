package com.axelor.apps.gst.service;

import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.Tax;
import com.axelor.apps.account.db.TaxLine;
import com.axelor.apps.account.db.repo.TaxRepository;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class InvoiceLineServiceImpl implements InvoiceLineService {

  @Inject ProductRepository productRepository;

  @Override
  public InvoiceLine calculateInvoiceLineGst(InvoiceLine invoiceLine, Boolean isStateDiff) {
    BigDecimal price = invoiceLine.getPrice();
    if (invoiceLine.getDiscountTypeSelect() != 3) price = invoiceLine.getPriceDiscounted();
    invoiceLine.setExTaxTotal(price.multiply(invoiceLine.getQty()));
    if (invoiceLine.getTaxLine() != null) {
      if (isStateDiff == null) {
        invoiceLine.setCgst(BigDecimal.ZERO);
        invoiceLine.setSgst(BigDecimal.ZERO);
        invoiceLine.setIgst(BigDecimal.ZERO);
        invoiceLine.setInTaxTotal(invoiceLine.getExTaxTotal());
      } else if (!isStateDiff) {
        invoiceLine.setIgst(BigDecimal.ZERO);
        invoiceLine.setSgst(
            (invoiceLine.getExTaxTotal().multiply(invoiceLine.getTaxLine().getValue()))
                .divide(new BigDecimal(2)));
        invoiceLine.setCgst(
            (invoiceLine.getExTaxTotal().multiply(invoiceLine.getTaxLine().getValue()))
                .divide(new BigDecimal(2)));
        invoiceLine.setInTaxTotal(
            invoiceLine.getExTaxTotal().add(invoiceLine.getCgst()).add(invoiceLine.getSgst()));
      } else if (isStateDiff) {
        invoiceLine.setCgst(BigDecimal.ZERO);
        invoiceLine.setSgst(BigDecimal.ZERO);
        invoiceLine.setIgst(
            invoiceLine.getExTaxTotal().multiply(invoiceLine.getTaxLine().getValue()));
        invoiceLine.setInTaxTotal(invoiceLine.getExTaxTotal().add(invoiceLine.getIgst()));
      }
    }
    return invoiceLine;
  }

  @Override
  public List<InvoiceLine> getInvoiceItemsById(List<Integer> products) {
    List<InvoiceLine> invoiceItemList = new ArrayList<InvoiceLine>();
    if (products != null) {
      Product product = null;
      InvoiceLine invoiceLine = null;
      for (int id : products) {
        product = productRepository.find((long) id);
        invoiceLine = new InvoiceLine();
        invoiceLine.setProduct(product);
        invoiceLine.setProductName(product.getName());
        invoiceLine.setUnit(product.getUnit());
        invoiceLine.setTypeSelect(0);
        invoiceLine.setDiscountTypeSelect(3);
        invoiceLine.setQty(BigDecimal.ONE);
        invoiceLine.setPrice(product.getSalePrice());
        invoiceLine.setExTaxTotal(invoiceLine.getQty().multiply(invoiceLine.getPrice()));
        TaxLine taxLine = null;
        if (product.getGstRate().compareTo(BigDecimal.ZERO) != 0) {
          List<Tax> taxList = Beans.get(TaxRepository.class).all().fetch();
          for (Tax t : taxList) {
            if (t.getActiveTaxLine()
                    .getValue()
                    .compareTo(product.getGstRate().divide(new BigDecimal(100)))
                == 0) taxLine = t.getActiveTaxLine();
          }
        } else if (product.getGstRate().compareTo(BigDecimal.ZERO) == 0) {
          if (product.getAccountManagementList() != null
              && product.getAccountManagementList().size() > 0) {
            if (product.getAccountManagementList().iterator().next() != null)
              taxLine =
                  product
                      .getAccountManagementList()
                      .iterator()
                      .next()
                      .getSaleTax()
                      .getActiveTaxLine();
          }
        }
        invoiceLine.setTaxLine(taxLine);
        invoiceItemList.add(invoiceLine);
      }
    }
    return invoiceItemList;
  }
}
